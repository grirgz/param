
// place here some other classes related to EventList and EventLoop
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

XEventList : List {
	var <totalDur = 0, <playingDur = 0;

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
		this.add((absTime: absTime, type: \start, relDur: 0));
	}

	startTime {
		if(this.size > 0) {
			^this[0].absTime
		} {
			^0
		};
	}

	addEvent { |ev|
		if (array.size == 0) { this.start(ev[\absTime]) };
		super.add(ev);
		this.setRelDurInPrev(ev, this.lastIndex);
		this.changed(\refresh); // added by ggz
	}

	calcRelDurs {
		this.doAdjacentPairs { |prev, next|
			var newRelDur = next[\absTime] - prev[\absTime];
			prev.put(\relDur, newRelDur);
			prev.put(\dur, newRelDur);
		};
		this.last.put(\relDur, 0).put(\dur, 0);
	}

	finish { |absTime|
		this.addEvent((absTime: absTime, type: \end, relDur: 0));
		totalDur = absTime - this.first[\absTime];
		playingDur = totalDur;
		this.setPlayDursToRelDur;
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

	setPlayDursToRelDur { this.setPlayDurs({ |ev| ev[\relDur] }); }

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

	removeEvent { arg event;
		this.remove(event);
		this.calcRelDurs;
		this.setPlayDursToRelDur;
		this.changed(\refresh);
	}

	reorder {
		"eventlist reordering".debug;
		this.sort({ arg a,b; 
			switch(a[\type],
				\start, {
					true
				},
				// dont put end at the end because we maybe want to shorten the clip
				//\end, {
				//	false
				//},
				{
					a[\absTime] < b[\absTime] 
				}
			)
		});
		this.calcRelDurs;
		this.setPlayDursToRelDur;
		//this.changed(\refresh);
	}

	embedInStream { arg in;
		var res;
		var seq = this.copy;
		seq[0] = seq[0].copy;
		seq[0][\type] = \rest;
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
			this.changed(\cursor, \play);
			inpat = this.embedInStream(inpat)
		})
	}

	*newFrom { arg pat, size=20;
		if(pat.isKindOf(Pattern)) {
			var ins = super.new;
			var endtime;
			var str;
			var absTime = 0;
			var ev, prev;
			str = pat.asStream;
			ins.start(absTime);
			block { arg break;
				size.do {
					prev = ev;
					ev = str.next(Event.default);
					if(ev.notNil) {
						ev.debug("ev");
						ev[\absTime] = absTime;
						absTime = absTime + ev[\dur];
						ins.addEvent(ev);
						ev.debug("endev");
					} {
						break.value;
					};
				}
			};
			//endtime = absTime + prev.use({ ~sustain.value(prev) }); // this is wrong
			endtime = absTime;
			ins.finish(endtime);
			^ins
		}
		
	}

	setEndPosition { arg time;
		block { arg break;
			this.do { arg ev;
				if(ev[\type] == \end) {
					ev[\absTime] = time;
					break.value;
				}
			}
		};
		// FIXME: changed signal ?
		this.reorder;
	}

}


XEventLoop {

	// for now, let all EventLoops live in one dict?
	// subclasses can redirect to their own this.all
	classvar <allEls;

	var <key, <func;
	var <list, <task, <isRecording = false;
	var recStartTime, then;
	var <>keysToRecord;

	var <verbosity = 1;

	var <lists, <currIndex = 0, <numLists = 0;

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
		func = func ?? { "defaultFunc!".postln; this.defaultFunc };
		lists = XEventList[];

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
		list = inList;
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

	startRec { |instant = false|

		if (isRecording) { ^this };

		isRecording = true;
		this.prepRec;
		task.stop;
		if (verbosity > 0) {
			"  %.startRec; // recording list[%].\n".postf(this, list.size);
		};
		if (instant) { list.start(this.getAbsTime); };
	}

	recordEvent { |event|
		var recEvent;
		if (isRecording) {
			// autostart at 0
			if (list.size == 0) { list.start(this.getAbsTime); };
			recEvent = this.getTimes;
			event.keysValuesDo { |key, val|
				if (key === \absTime) {
					warn("" + thisMethod ++ ": can't use 'absTime' as key in event: %!"
						.format(event));
				} {
					recEvent.put(key, val);
				};
			};
			list.addEvent(recEvent);
			if (verbosity > 1) {
				("//" + this.asString + "rec: ").post; recEvent.postln;
			};
		}
	}

	stopRec {
		if (isRecording.not) { ^this };

		isRecording = false;
		list.finish(this.getAbsTime);
		this.addList;
		recStartTime = nil;

		if (verbosity > 0) {
			"// % stopRec; // recorded list[%] with % events.\n".postf(
				this, lists.lastIndex, lists.last.size)
		};
	}

	toggleRec { |instant=false|
		if (isRecording, { this.stopRec }, { this.startRec(instant) });
	}

	getAbsTime {
		var now = thisThread.seconds;
		recStartTime = recStartTime ? now;
		^now - recStartTime;
	}

	getTimes {
		var absTime, relDur;
		var now = thisThread.seconds;
		if (then.isNil) {
			then = now;
			recStartTime = now;
		};
		relDur = now - then;
		absTime = now - recStartTime;
		then = now;
		^(absTime: absTime, relDur: relDur);
	}

	prepRec {
		this.addList;
		this.list_(XEventList[]);
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

	listInfo { ^lists.collect { |l, i| [i, l.size] } }

	printLists {|round = 0.01|
		this.post; this.asString + "- lists: ".postln;
		this.listInfo.postln;
		lists.do (_.print);
	}

	setList { |index = 0|
		var newList = lists[index];
		if (newList.isNil) {
			this.post; ": no list at index %.".postf(index);
			^this
		};
		this.list_(newList);
		currIndex = index;
	}

	listDur { ^list.last.keep(2).sum; }

	quantize { |quant = 0.25, fullDur|
		list.quantizeDurs(quant, fullDur);
	}

	unquantize { list.restoreDurs; }

	///////// added by ggz

	clear {
		list = List.new;
	}

}
