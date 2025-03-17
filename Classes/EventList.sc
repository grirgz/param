
// edited version of EventList and EventLoop




/* EventList is a list of recorded events.
   It follows a few conventions:
*  recorded events always have an abstime,
*  plus any other key/value combinations that help
     to store the events in semantically rich form.
TimeLoop
* reserved keys - overwrite at your own risk:
   relDur: is used for storing delta-time between events,
   dur: is used to calculate actual logical duration,
        e.g. when soft-quantizing an EventList to a time grid.

* recording a List is terminated by .finish(absTime),
  which puts an end event at the end of the list.

e = EventLoop(\x);
e.startRec;
e.recordEvent((absTime: 2));


(meta: (absTime: x, relDur: y), lx: 0.24, ly: 0.56);
protected keyNames!

*

a = EventList[];
a.addEvent((absTime: 0));// events should begin with time 0;
a.addEvent((absTime: 0.3));
a.addEvent((absTime: 0.52));
a.addEvent((absTime: 0.72));
a.addEvent((absTime: 0.93));
a.finish(1.88);

a.print;
a.print([\dur]);
a.print([\dur], false);
a.print([\absTime, \dur], false);

a.quantizeDurs(0.25, 2).printAll;"";
a.totalDur;
a.playingDur;


a.collect(_.absTime);
a.collect(_.type);

? also put a startEvent before all others?
    esp. if one wants to record silence first?

*/

// playDur is used by the playing task in EventLoop, used to quantize and restore back to value of relDur
// in embedInStream, playDur is copied to dur
// relDur is equal to dur and both are set by calcRelDurs by diff'ing absTime
// totalDur is set by finish


TimelineEventList : List {
	var <totalDur = 0, <playingDur = 0;
	var startTime, <endTime;
	var <>extraData; // for storing buffer data or other things sync with his eventlist

	print { |keys, postRest = true|
		var postKeys;
		if (postRest.not) {
			postKeys = keys;
		} {
			postKeys = this[1].keys.asArray.sort;
			if (keys.notNil) {
				postKeys = (keys ++ postKeys.removeAll(keys));
			};
		};
		this.do { |ev|
			var ev2 = ev.copy;
			postKeys.do { |key|
				"%: %, ".postf(key, ev2.removeAt(key));
			};
			if (ev2.size > 0) {
				".. %\n".postf(ev2);
			} {
				"".postln;
			};
		}
	}

	start { |absTime = 0|
		this.add((absTime: absTime, type: \start, relDur: 0, sustain:0));
	}

	setStartPosition { arg time;
		var tev;
		block { arg break;
			this.do { arg ev;
				if(ev[\type] == \start) {
					ev[\absTime] = time;
					tev = ev;
					break.value;
				}
			}
		};
		this.reorder;
		if(tev.notNil) {
			tev.changed(\refresh);
		};
	}

	setEndPosition { arg time;
		var tev;
		block { arg break;
			this.do { arg ev;
				if(ev[\type] == \end) {
					ev[\absTime] = time;
					tev = ev;
					break.value;
				}
				// FIXME: create end if not existing ?
			}
		};
		this.reorder;
		if(tev.notNil) {
			tev.changed(\refresh);
		};
	}

	startTime {
		// this time is absolute
		^startTime;
		//if(this.size > 0) {
		//	^this[0].absTime
		//} {
		//	^0
		//};
	}

	startTime_ { arg val;
		this.setStartPosition(val)
	}

	endTime_ { arg val;
		this.setEndPosition(val)
	}

	relStartTime {
		// this time is relative to the first event
		^this.startTime - this.firstTime;
	}

	firstTime {
		^this[0][\absTime];
	}

	clone {
		var inst = this.class.newFrom(this.collect(_.copy));
		inst.extraData = this.extraData.copy;
		^inst;
	}

	double {
		var oldendidx;
		var elcopy = this.copy;
		elcopy.do( { arg ev, idx;
			if(ev.type == \start) {
				// NOOP
			} {
				ev = ev.copy;
				ev[\absTime] = ev[\absTime] + elcopy.last[\absTime];
				this.add(ev);
			};
			if(ev.type == \end) {
				oldendidx = idx;
			};
		} );
		this.removeAt(oldendidx);
		this.reorder;
		this.changed(\refresh);
	}

	addEvent { |ev|
		if (array.size == 0) { this.start(ev[\absTime] ? 0) };
		super.add(ev);
		this.setRelDurInPrev(ev, this.lastIndex);
		this.changed(\refresh); // added by ggz
	}

	calcRelDurs {
		if(this.size > 2) {
			this.doAdjacentPairs { |prev, next|
				var newRelDur = next[\absTime] - prev[\absTime];
				prev.put(\relDur, newRelDur);
				prev.put(\dur, newRelDur);
			};
			this.last.put(\relDur, 0).put(\dur, 0);
		};
	}

	calcAbsTimes {
		var absTime = 0;
		this.do { arg ev, idx;
			var newAbsTime = ( ev[\dur] ?? ev[\relDur] ?? ev[\playDur] );
			ev[\absTime] = absTime;
			absTime = absTime + newAbsTime;
		};
	}

	finish { |absTime|
		this.addEvent((absTime: absTime, type: \end, relDur: 0));
		//totalDur = absTime - this.first[\absTime]; // set in .reorder
		//playingDur = totalDur; // set in .reorder
		//this.setPlayDursToRelDur; // called in .reorder
		this.reorder;
	}

	setRelDurInPrev { |newEvent, newIndex|
		var prevEvent;
		newIndex = newIndex ?? { array.indexOf(newEvent) };
		prevEvent = array[newIndex - 1];

		if (prevEvent.notNil) {
			prevEvent[\relDur] = newEvent[\absTime] - prevEvent[\absTime];
		};
	}

	setPlayDurs { |func| this.do { |ev| ev.put(\playDur, func.value(ev)) } }

	setPlayDursToRelDur { 
		// playDur is deprecated, was used to play at different tempo or for quantization
		this.setPlayDurs({ |ev| ev[\relDur] }); 
	}

	quantizeDurs { |quant = 0.25, fullDur|
		var durScaler = 1;
		fullDur !? {
			playingDur = fullDur;
			durScaler = fullDur / totalDur;
		};

		this.doAdjacentPairs({ |ev1, ev2|
			var absNow = (ev2[\absTime] * durScaler).round(quant);
			var absPrev = (ev1[\absTime] * durScaler).round(quant);
			ev1.put(\playDur, (absNow - absPrev));
		});
		// leaves end event untouched.
	}

	restoreDurs {
		this.setPlayDursToRelDur;
	}

	//////////// added by ggz

	removeEvent { arg event, refresh=true;
		this.remove(event);
		if(refresh == true) {
			this.calcRelDurs;
			this.setPlayDursToRelDur;
			this.changed(\refresh);
		}
	}

	splitEvent { arg event, durFromEventStart;
		var ev1 = event;
		var ev2 = event.copy;
		var osus = event.use{ ~sustain.value } ? 0;
		if(durFromEventStart > 0 and: {durFromEventStart < osus}) {
			ev1[\sustain] = durFromEventStart;
			ev2[\sustain] = osus-durFromEventStart;
			ev2[\absTime] = ev1[\absTime] + durFromEventStart;
			ev2[\event_dropdur] = durFromEventStart + ( ev2[\event_dropdur] ? 0 );
			this.add(ev2);
			this.reorder;
			this.changed(\refresh);
			^ev2;
		} {
			^ev1
		};
	}

	reorder {
		var startEv, endEv;
		//"eventlist reordering".debug;
		this.sort({ arg a,b; 
			switch(a[\type],
				// if a note and start has equal absTime, \start come first
				\start, {
					( a[\absTime] == b[\absTime] ) or: { a[\absTime] < b[\absTime] }
				},
				// if a note and end has equal absTime, \end come last
				\end, {
					( a[\absTime] == b[\absTime] ).not and: { a[\absTime] < b[\absTime] }
				},
				{
					a[\absTime] <= b[\absTime] 
				}
			)
			//a[\absTime] < b[\absTime] 
		});
		this.do { arg ev;
			if(ev[\type] == \start) {
				startEv = ev;
				startTime = ev[\absTime];
				if(endEv.notNil) {
					// end before start, swap them
					var tmp = endEv.copy;
					//endEv.debug("swap!");
					endEv[\type] = startEv[\type];
					endEv[\label] = startEv[\label];
					startEv[\type] = tmp[\type];
					startEv[\label] = tmp[\label];
					startTime = endEv[\absTime];
					endTime = startEv[\absTime];
					//[ev, startEv, endEv, this].debug("end swap");
				};
			};
			if(ev[\type] == \end) {
				//ev.debug("type end");
				endEv = ev;
				endTime = ev[\absTime];
			};
		};

		if(startTime.isNil) { 
			// if empty list, set default value
			// hope this doesn't break anything
			startTime = 0;
		};
		if(endTime.isNil) {
			endTime = 0;
		};
		this.calcRelDurs;
		this.setPlayDursToRelDur;
		totalDur = endTime - startTime;
		playingDur = totalDur;
		//this.changed(\refresh);
	}

	embedInStream { arg in;
		var res;
		var seq = this.copy;
		// FIXME: this assert that first event is the start event, but it's not true, 
		// may be a bug
		// and why not change the type of end event too ?
		//seq[0] = seq[0].copy;
		//seq[0][\type] = \rest;
		//seq[0][\sustain] = 0;
		in = in ? Event.default;
		seq = seq.collect({ arg ev; ev[\dur] = ev[\playDur] });

		res = Pseq(
			seq
		);
		^res.embedInStream(in)
	}

	asPattern { arg in;
		^Prout({ arg inpat;
			this.changed(\cursor, \play); // FIXME: should be an event
			// but if it's an event, it would be cut by outer Pembed
			// only work because cutting the pattern is really fast
			inpat = this.embedInStream(inpat)
		})
	}

	*newFrom { arg pat, size=2000, inval;
		//startTime = 0;
		if(pat.isKindOf(Pattern)) {
			var ins = super.new;
			var endtime;
			var str;
			var absTime = 0;
			var ev, prev;
			var first = true;
			//Log(\Param).debug("XEventList.newfrom start");
			str = pat.asStream;
			ins.start(absTime);
			inval = inval ? Event.default;
			block { arg break;
				( size ).do {
					prev = ev;
					ev = str.next(inval);  // no need to change inval because we are not chaining a pattern
					if(first) {
						// Number: handle Rest(x) which return x instead of an event
						// TODO: but only at the start, should also implement the other case!
						// maybe there is a better way ?
						//ins[0].absTime = ev;
						if(ev.isKindOf(Number)) {
							absTime = ev;
							//Log(\Param).debug("starting rest");
						} {
							if( ev.isRest == true or: { ev.use { ~midinote.value } == \rest }) {
								absTime = ev.use{~delta.value} ? ev.use{~dur.value};
							}
						}
					};
					if(ev.notNil) {
						//Log(\Param).debug("ev %", ev);
						ev[\absTime] = absTime;
						//Log(\Param).debug("XEventList.newFrom: absTime: %, evdur: %", absTime, ev[\dur]);
						absTime = absTime + ( ev.use{~delta.value} ? ev.use{~dur.value} );
						// FIXME: hardcode sustain because it's in function of \dur which is overwritten in calcRelDurs
						// 		should maybe use \delta in calcRelDurs, but this can break everything!
						ev[\sustain] = ev.use{~sustain.value};
						ev[\midinote] = ev.use{~midinote.value}; // FIXME: midinote is hardcoded because NoteTimeline does not know how to display \degree and never compute \midinote function
						// now that absTime is calculated, should get rid of it because not handled everywhere but used instead of \dur in pattern.play
						ev[\delta] = nil; 
						ins.addEvent(ev);
						//ev.debug("endev");
						//Log(\Param).debug("endev:%", ev);
					} {
						break.value;
					};
					first = false;
				}
			};
			//endtime = absTime + prev.use({ ~sustain.value(prev) }); // this is wrong
			//Log(\Param).debug("XEventList.newfrom end 1");
			endtime = absTime;

			// remove rests because not handled by timeline (should display them transparent)
			ins.reverse.do { arg ev;
				if(ev.isRest == true or: { ev.use { ~midinote.value } == \rest }) {
					if([\start, \stop,\locator].includes(ev[\type]).not) {
						ins.remove(ev);
					}
				}
			};

			ins.finish(endtime);
			^ins
		} {
			//Log(\Param).debug("XEventList.newfrom: not pattern, create normal event list");
			if(pat.isKindOf(SequenceableCollection)) {
				var ret = super.newFrom(pat.asArray);
				ret.reorder;
				^ret;
			}
		}
		
	}

	presetCompileString {
		var ret;
		ret = this.collect({ arg ev;
			var evstring;
			evstring = case
				{ ev.type == \pattern } {
					"\tPatternEvent((%)),\n"
				}
				{ ev.type == \player } {
					"\tPlayerEvent((%)),\n"
				}
				{
					"\t(%),\n"
				}
			;
			evstring.format(
				// ev.asCompileString return stuff from parent event, so looping manually
				ev.keys.as(Array).sort.collect({ arg key;
					"%%: %, ".format("\\",key, ev[key].asCompileString)
				}).join
			)
		}).join;
		ret = "%.newFrom([\n%]);".format(this.class.asString, ret);
		^ret
	}

	hasContent {
		// isEmpty is already implemented in List
		^this.any { arg item;
			[\start, \end].includes(item.type).not
		};
	}
}

XEventList : TimelineEventList {}

TimelineEventLoop {

	// for now, let all EventLoops live in one dict?
	// subclasses can redirect to their own this.all
	classvar <allEls;

	var <key, <func;
	var <list, <task, <isRecording = false;
	var recStartTime, then;
	var <>keysToRecord;
	var >clock;

	var <>recordQuant, <>recordLatency;

	var <verbosity = 1;

	var <lists, <currIndex = 0, <numLists = 0;

	var <>inHistoryMode = false, <>historyList, <>historyBackup, <>historyIndex = -1;

	*initClass { allEls = () }

	*all { ^allEls }

	*at { |key| ^this.all[key] }

	*newInstance { arg func; // added by ggz
		^super.newCopyArgs(\instance, func).init;
	}

	*new { arg key, func;
		var res = this.at(key);
		if(res.isNil) {
			res = super.newCopyArgs(key, func).init.prAdd(key);
		} {
			// do we want to use the interface
			// EventLoop(\x, {}) to change the playback func?
			if (func.notNil) { res.func_(func) };
		}
		^res
	}

	prAdd { arg argKey;
		key = argKey;
		this.class.all.put(argKey, this);
	}

	// backwards compat in KeyPlayer
	isOn { ^isRecording }

	storeArgs { ^[key] }

	printOn { |stream| ^this.storeOn(stream) }

	init { |argFunc|
		func = argFunc ?? { this.defaultFunc };
		lists = List.new; // ggz: should not be XEventList
		historyList = List.new;

		this.initTask;
		this.prepRec;
	}

	func_ { |inFunc|
		func = inFunc;
		task.set(\func, func);
	}

	defaultFunc { ^{ |ev| ev.postln.copy.play; } }

	// check that it is an EventList?
	list_ { |inList|
		this.historyAddSnapshot;
		list = inList;
		this.changed(\list); // added by ggz
		task.set(\list, list);
	}

	prSetList { arg inList;
		// do not add history snapshot, used internally
		list = inList;
		this.changed(\list); // added by ggz
		task.set(\list, list);
	}

	verbosity_ { |num|
		verbosity = num;
		task.set(\verbosity, num);
	}

	looped { ^task.get(\looped) }
	looped_ { |val| task.set(\looped, val) }
	toggleLooped { this.looped_(this.looped.not) }

	tempo { ^task.get(\tempo) }
	tempo_ { |val| task.set(\tempo, val) }

	step { ^task.get(\step) }
	step_ { |val| task.set(\step, val) }

	jitter { ^task.get(\jitter) }
	jitter_ { |val| task.set(\jitter, val) }

	lpStart { ^task.get(\lpStart) }
	lpStart_ { |val| task.set(\lpStart, val) }

	range { ^task.get(\range) }
	range_ { |val| task.set(\range, val) }

	initTask {

		task = TaskProxy({ |envir|
			var event, absTime, relDur;
			var index = 0, indexOffset = 0, indexPlusOff;
			var maxIndex, minIndex, indexRange, indexInRange = true;

			var calcRange = {
				var lastIndex =(envir[\list].lastIndex ? -1);
				minIndex = (envir[\lpStart] * lastIndex).round.asInteger;
				indexRange = (envir[\range] * lastIndex).round.asInteger;
				maxIndex = minIndex + indexRange;
				// [minIndex, maxIndex, indexRange].postln;
			};
			var calcIndexInRange = {
				indexInRange = (index >= minIndex) and: { index <= maxIndex };
			};

			if (envir.verbosity > 0) {
				(envir[\postname] + "plays list of % events and % secs.")
				.format(list.size, list.totalDur.round(0.01)).postln;
			};

			calcRange.value;
			index = if (envir[\step] > 0, minIndex, maxIndex);
			calcIndexInRange.value;

			while { envir[\looped] or: indexInRange } {

				indexOffset = (envir[\jitter].bilinrand * indexRange).round.asInteger;
				indexPlusOff = (index + indexOffset).round.asInteger.wrap(minIndex, maxIndex);
				// [index, indexOffset, indexPlusOff].postln;

				event = envir[\list].wrapAt(indexPlusOff);
				if (event.isNil) {
					0.1.wait;
					// early exit here? e.g. set loop false?
				}{
					event[\type].switch(
						\start, { "startfunc?"; },
						\end, { "endfunc?"; },
						{ envir[\func].value(event); }
					);

					if (envir.verbosity > 1) {
						String.fill(indexPlusOff, $-).post;
						"i: % - ev: %".format(indexPlusOff, event).postln;
					};

					(event[\playDur] / envir[\tempo]).wait;

					index = (index + envir[\step]);
					calcRange.value;
					calcIndexInRange.value;
					if (envir[\looped] and: { indexInRange.not }) {
						index = index.wrap(minIndex, maxIndex);
					};
				};
			};

			if (envir.verbosity > 0) { (envir[\postname] + "ends.").postln; };

		});

		task.set(\postname, this.asString);
		task.set(\verbosity, 1);
		task.set(\looped, false, \step, 1, \tempo, 1);
		task.set(\lpStart, 0, \range, 1, \jitter, 0);

		task.addSpec(\tempo, [0.1, 10, \exp, 0.001]);
		task.addSpec(\lpStart, [0, 1]);
		task.addSpec(\range, [0, 1]);
		task.addSpec(\jitter, [0, 1, \amp]);
		task.addSpec(\step, [-1, 1, \lin, 1]);
		task.addHalo(\orderedNames, [\tempo, \lpStart, \range, \jitter]);

		task.set(\func, func);
	}

	// recording events:

	startRec { |instant = false, quant, latency|
		if(quant.notNil) {
			this.recordQuant = quant ? 0;
		};

		if (isRecording) { ^this };

		isRecording = true;
		this.prepRec;
		task.stop;
		if (verbosity > 0) {
			"  %.startRec; // recording list[%].\n".postf(this, list.size);
		};
		recordLatency = latency ?? { Server.default.latency };
		if (instant == true) { 
			list.start(this.getAbsTime(this.recordQuant));
			//this.getAbsTime.debug("instant start");
		};
	}

	recordEvent { |event|
		var recEvent;
		//isRecording.debug("XEventLoop: recordEvent: isRecording");
		//verbosity.debug("XEventLoop: recordEvent: isRecording");
		if (isRecording) {
			// autostart at 0
			if (list.size == 0) { list.start(this.getAbsTime(this.recordQuant)); };
			//recEvent = this.getTimes;
			recEvent = event.class.new; // added by ggz to keep event subclass
			recEvent.putAll(this.getTimes);
			event.keysValuesDo { |key, val|
				if (key === \absTime) {
					warn("" + thisMethod ++ ": can't use 'absTime' as key in event: %!"
						.format(event));
				} {
					recEvent.put(key, val);
				};
			};
			//[event, recEvent].debug("XEventLoop: recordEvent: event, recEvent");
			list.addEvent(recEvent);
			if (verbosity > 1) {
				("//" + this.asString + "rec: ").post; recEvent.postln;
			};
		}
		^recEvent; // added by ggz, needed to set noteOff/sustain
	}

	stopRec {
		if (isRecording.not) { ^this };

		isRecording = false;
		list.finish(this.getAbsTime);
		this.addList;
		recStartTime = nil;

		if (verbosity > 0) {
			// ggz FIXED: the added list is not the last but the first
			"// % stopRec; // recorded list[0] with % events.\n".postf(
				this, lists.first.size)
		};
	}

	toggleRec { |instant=false|
		if (isRecording, { this.stopRec }, { this.startRec(instant) });
	}

	getAbsTime { arg quant = 0, latency;
		//var now = thisThread.seconds;
		var now = this.clock.beats;
		latency = latency ?? { this.recordLatency };
		//[now, recStartTime].debug("recStartTime: debug before");
		if(recStartTime.isNil and: { quant != 0 }) {
			recStartTime = this.clock.nextTimeOnGrid(quant) - quant + latency;
			now = recStartTime;
		} {
			recStartTime = recStartTime ? now;
		};
		//[now, recStartTime].debug("recStartTime: debug after");
		^( now - recStartTime );
	}

	getTimes {
		var absTime, relDur;
		var now = this.clock.beats;
		var nowsec = thisThread.seconds;
		if (then.isNil) {
			//then = now;
			//recStartTime = now;
			then = recStartTime; // ggz: was a bug: instant not working because set recStartTime but "then" is not defined
		};
		relDur = now - then;
		absTime = now - recStartTime;
		then = now;
		^(absTime: absTime, relDur: relDur);
	}

	prepRec {
		this.addList;
		this.historyAddSnapshot;
		this.list_(TimelineEventList[]);
		then = recStartTime = nil;
		this.resetLoop;
	}
	// support simple pattern recording
	next { |inval|
		this.recordEvent(inval);
		^this.isRecording.binaryValue
	}

	// taskproxy for playback interface

	play {
		this.stopRec;
		if (verbosity > 0) { "  %.play;\n".postf(this) };
		task.stop.play;
	}

	togglePlay { if (task.isPlaying, { this.stop }, { this.play }); }

	stop {
		if (verbosity > 0) { "  %.stop;\n".postf(this) };
		task.stop;
	}

	pause { task.pause; }
	resume { task.resume; }
	isPlaying { ^task.isPlaying; }

	// could be more flexible
	playOnce {
		task.fork(event: (task.envir.copy).put(\looped, false));
	}

	resetLoop { task.set(\lpStart, 0, \range, 1, \step, 1, \tempo, 1, \jitter, 0) }

	isReversed { ^this.step == -1 }
	reverse { this.step_(-1) }
	forward { this.step_(1) }
	flip { this.step_(this.step.neg) }


		// handling the lists

	addList {
		if (list.notNil and: { list.notEmpty and: { lists.first !== list } }) {
			lists.addFirst(list);
			numLists = lists.size;
		}
	}

	pushList { // beter name
		this.addList
	}

	cloneAndPushList {
		if (list.notNil and: { list.notEmpty }) {
			lists.addFirst(list.clone);
			numLists = lists.size;
		}
	}

	listInfo { ^lists.collect { |l, i| [i, l.size] } }

	printLists {|round = 0.01|
		this.post; this.asString + "- lists: ".postln;
		this.listInfo.postln;
		lists.do (_.print);
	}

	setList { |index = 0|
		var newList = lists[index];
		if (newList.isNil) {
			this.post; ": no list at index %.\n".postf(index); // added \n
			^this
		};
		this.list_(newList);
		currIndex = index;
		this.changed(\currIndex);
	}

	currIndex_ { arg index = 0;
		this.setList(index);
	}

	listDur { ^list.last.keep(2).sum; }

	quantize { |quant = 0.25, fullDur|
		list.quantizeDurs(quant, fullDur);
	}

	unquantize { list.restoreDurs; }

	///////// added by ggz

	clear {
		list = List.new;
		lists = List.new;
	}

	clock { arg self;
		^(clock ? TempoClock.default);
	}

	startRecording {
		^this.startRec
	}

	stopRecording {
		^this.stopRec
	}

	isRecording_ { arg val;
		if(val) {
			this.startRecording;
		} {
			this.stopRecording;
		}
	
	}

	/// undo system
	
	historyAddSnapshot {
		//~debug = this;
		if(list.notNil) {
			if(inHistoryMode == true) {
				inHistoryMode = false;
				historyIndex = -1;
			};
			historyList.addFirst(list.clone);
			this.changed(\inHistoryMode); // signal undo button can be enabled
		};
	}

	historyUndo {
		//~debug = this;
		if(this.historyCanUndo) {
			if(inHistoryMode == false) {
				inHistoryMode = true;
				historyBackup = list;
			};
			historyIndex = historyIndex + 1;
			this.prSetList(historyList[historyIndex].clone);
		}; 
		this.changed(\inHistoryMode); // signal undo button can be enabled
	}

	historyRedo {
		//~debug = this;
		if(this.historyCanRedo) {
			historyIndex = historyIndex - 1;
			if(historyIndex == -1) {
				inHistoryMode = false;
				this.prSetList(historyBackup);
			} {
				this.prSetList(historyList[historyIndex].clone);
			};
		}; 
		this.changed(\inHistoryMode); // signal undo button can be enabled
	}

	historyCanUndo {
		//~debug = this;
		^historyIndex < ( historyList.size-1 )
	}

	historyCanRedo {
		//~debug = this;
		^historyIndex > -1
	}

}

XEventLoop : TimelineEventLoop {}

// not sure if needed
XEventEnv : XEventList {
	var <>param;

	
}

Pev {
	*new { arg event;
		^Pevent(
			Pbind(),
			event
		).keep(1)
	}
}

