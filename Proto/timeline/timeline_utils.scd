

// "~/code/sc/seco/vlive/demo/param/lib/timeline.scd".standardizePath.load;


// why has a gate ? it's time the important thing
// time should be in seconds
// maybe i used gate to be able to release early when pattern is stopped ?
// gate is used because when event is cut by dropdur, the sustain is cut, but the slope should be the same
	// ^ to verify
//SynthDef(\env_segment, { arg out=0, level=0.1, time=0.1, curve=1, gate=1;
//	var sig;
//	var in = In.kr(out);
//	sig = EnvGen.kr(Env([in,level], [time], curve),gate,doneAction:2);
//	sig.poll(label:"env segment");
//	Out.kr(out, sig);
//}).add;
SynthDef(\metronome, { arg out=0, amp=0.1, gate=1, pan=0, freq=200;
	var sig;
	sig = SinOsc.ar(freq, mul:1/2);
	sig = sig + Impulse.kr(1/8, mul:4);
	sig = sig * EnvGen.ar(Env.adsr(0.001,0.1,0.8,0.001),gate,doneAction:2);
	sig = sig / 1;
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(out, sig);
}).add;

Pdef(\metronome, Pbind(
	\instrument, \metronome,
	\degree, Pseq([2,0,0,0,1,0,0,0],inf),
	\sustain, 0.1,
	\dur, 1/2,
));

SynthDef(\env_segment, { arg out=0, level=0.1, time=0.1, curve=1;
	var sig;
	var in = In.kr(out);
	sig = EnvGen.kr(Env([in,level], [time], curve),1,doneAction:2);
	//sig.poll(label:"env segment");
	Out.kr(out, sig);
}).add;


ProtoTemplateDef(\TimelineEmbeder, (
	// replace all following function by one class: Pembed, this is the prototype
	// Pembed is defined in Param/custom_events.sc and redirect to here

	new: { arg self, pat, start_offset, parent;
		// start_offset is relative to first event which is time=0, 
		// 		not \absTime used only in TimelineEventList
		// FIXME: stop_offset not used (user should use Pfindur instead)
		if(pat.isKindOf(Pattern)) {
			self.embed_pattern(pat, start_offset, parent);
		} {
			Pspawner({ arg spawner;
				self.embed_event(spawner, pat, parent);
			})
		};
		
	},

	embed_pattern: { arg self, pat, start_offset, parent;
		Pspawner({ arg spawner;
			var stream = pat.asStream;
			//[pat, start_offset].debug("timeline_pattern: start");

			stream = self.cut_stream(start_offset, stream);

			if(stream.notNil) {
				stream.do({ arg ev;
					self.embed_event(spawner, ev, parent);
				}, Event.default)
			};

		})
		
	},

	embed_event: { arg self, spawner, ev, parent;
		//Log(\Param).debug("EMBED event %", ev);
		if(ev.type == \pattern) {
			ev = PatternEvent(ev); // because normal Event have embedInStream which replace parent by Event.default
			//Log(\Param).debug("EMBED event parent keys (pattern) %", ev.parent.keys);
			//ev.debug("embed_event: ev");
			spawner.par(ev.embedPattern(parent));
			spawner.wait(ev.use{ev.delta} ? ev.use {ev.dur}); // FIXME: use delta ?
			//spawner.seq(ev.embedPattern);
		} {
			if(ev.type == \player) {
				ev = PlayerEvent(ev);
				//ev.parent.keys.debug("EMBED event parent keys (player)");
			} {
				//ev.parent.keys.debug("EMBED event parent keys (normal)");
			};
			spawner.seq(Pseq([
				ev
			]))
		};
	},

	split_event: { arg self, val;
		//[val].debug("entering ~split_event");
		if(val.notNil) {
			val.use {
				var suboffset = val[\event_dropdur] - (val[\previous_event_dropdur] ? 0);
				if(suboffset == 0) {
					// we are on a border, do nothing;
					//val.debug("we are on a border, do nothing; ");
					val;
				} {
					if( suboffset > val.sustain ) {
						// we are on a rest
						val[\sustain] = val.sustain - suboffset;
						val[\dur] = val.dur - suboffset;
						val[\isRest] = true;
						//val.debug("we are on a rest");
						val;
					} {
						// we are on a note
						val[\sustain] = val.sustain - suboffset;
						val[\dur] = val.dur - suboffset;
						//val[\event_dropdur] = suboffset;
						//val.debug("we are on a note");
						val;
					};
				}
			};
		};
	},

	cut_stream: { arg self, drop_time, stream;
		var current_offset = 0;
		var previous_offset = 0;
		var val;
		var crossing_line = List.new;
		//[drop_time, stream].debug("entering ~stream_dropdur");
		if(drop_time.notNil and: { drop_time != 0}) {

			while (
				{
					current_offset <= drop_time and: {
						val = stream.next(Event.default);
						val.notNil;
					}
				},
				{
					val.use {
						val[\previous_event_dropdur] = val[\event_dropdur]; // used by ~split_event to not substract sustain two times
						//val.debug("stream_dropdur: val");
						//[val.dur, val.sustain].debug("stream_dropdur: val: dur, sustain");
						previous_offset = current_offset;
						current_offset = current_offset + val.dur; 
						if(
							// if last event cross the end line
							current_offset <= drop_time and: {
								( previous_offset + val.sustain ) > drop_time
							}
						) {
							val[\event_dropdur] = drop_time - previous_offset + (val[\event_dropdur] ? 0);
							//val.debug("stream_dropdur: added to crossing_line");
							crossing_line.add(val);
						};
						[current_offset, previous_offset, val];
					}
				}
			);
			// now current_offset point to the end of current event, ie: past the drop_time line


			if(val.notNil) {
				val[\event_dropdur] = drop_time - previous_offset + (val[\event_dropdur] ? 0);
				val = self.split_event(val.copy);

				crossing_line = crossing_line.collect({ arg ev; 
					ev = self.split_event(ev.copy);
					ev[\dur] = 0;
					//ev.debug("stream_dropdur: transformed crossing_line");
					ev;
				});

				Pseq(
					crossing_line ++
					[
						val,
						stream
					]
				).asStream;
			} {
				// if there is only one event crossing the line but it's not the last to start 
				// (meaning his dur < sustain), val will be nil, but not crossing_line
				if(crossing_line.size > 0) {
					val = crossing_line.pop;
					val = self.split_event(val.copy);
					crossing_line = crossing_line.collect({ arg ev; 
						ev = self.split_event(ev.copy);
						ev[\dur] = 0;
						//ev.debug("stream_dropdur: transformed crossing_line");
						ev;
					});
					Pseq(
						crossing_line 
						++
						[
							val;
						]
					).asStream;
				} {
					//nil
					"ERROR: Pembed: droptime % is longer than the stream".format(drop_time).postln;
					Pseq([Event.silent(2)]);
				}
			}

			// first yield all the splitted event concurrent with current val, then val, then remaining stream

		} {
			//"~stream_dropdur: return original stream untouched".debug;
			stream;
		};
	},

	embed_eventlist: { arg self, eventlist;
		// to enable reading before start event or after end event, add a flag to ignore start and end event
		// will be set by toplevel timeline view
		var current_offset = 0;
		var previous_offset = 0;
		var val;
		var crossing_line = List.new;
		var drop_time;
		var original_eventlist = eventlist;
		eventlist = eventlist.copy;
		//[eventlist].debug("entering ~eventlist_aspattern");

		eventlist.do { arg ev;
			if(ev[\type] == \start) {
				drop_time = ev[\absTime]
			};
			if(ev[\playDur].notNil) {
				ev[\dur] = ev[\playDur];
			}
		};

		// advance to start event
		while (
			{
				val = eventlist.removeAt(0);
				val = val.copy;
				val.notNil and: {
					val[\type] != \start;
				}
			},
			{
				val.use {
					//val.debug("eventlist_aspattern: val");
					//[val.dur, val.sustain].debug("eventlist_aspattern: val: dur, sustain");
					previous_offset = current_offset;
					current_offset = current_offset + val.dur; 
					// if some event started before the start event but continue after, cut them
					if(
						val[\type] != \start and: {
							val.sustain.notNil and: {
								( previous_offset + val.sustain ) > drop_time
							}
						}
					) {
						val[\event_dropdur] = drop_time - previous_offset;
						//val.debug("eventlist_aspattern: added to crossing_line");
						crossing_line.add(val);
					};
					[current_offset, previous_offset, val];
				}
			}
		);
		// now current_offset point to the end of current event, ie: past the drop_time line
		// now val is the start event


		if(val.notNil) {

			crossing_line = crossing_line.collect({ arg ev; 
				ev = self.split_event(ev.copy);
				ev[\dur] = 0; // every event start at the same time, \start event hold the delta to next event
				//ev.debug("eventlist_aspattern: transformed crossing_line");
				ev;
			});

			val.use {
				val = val.copy;
				val[\type] = \rest;
			};

			Prout({ arg inval;
				original_eventlist.changed(\cursor, \play);
				//"============================= Pembed.embed_eventlist starting routine".debug;
				crossing_line.collect({ arg x; 
					//x.debug("crossing_line list");
					x.proto !? { x.proto.keys }
				});
				[val].collect({ arg x; 
					//x.debug("val list");
					x.proto !? { x.proto.keys }
				});
				eventlist.collect({ arg x; 
					//x.debug("eventlist list");
					x.proto !? { x.proto.keys }
				});
				//( original_eventlist.endTime - original_eventlist.startTime ).debug("handling end event in pfindur");
				Pfindur(original_eventlist.endTime - original_eventlist.startTime, Pseq(
					( crossing_line ++ [val] ++ eventlist ).collect({ arg x; Pev(x) })
				)).embedInStream(inval);
				//original_eventlist.changed(\cursor, \stop);
			})
		} {
			"should never get there".throw;
			// if there is only one event crossing the line but it's not the last to start 
			// (meaning his dur < sustain), val will be nil, but not crossing_line
			//if(crossing_line.size > 0) {
			//	val = crossing_line.pop;
			//	val = ~split_event.(val.copy);
			//	crossing_line = crossing_line.collect({ arg ev; 
			//		ev = ~split_event.(ev.copy);
			//		ev[\dur] = 0;
			//		ev.debug("eventlist_aspattern: transformed crossing_line");
			//	});
			//	Pseq(
			//		crossing_line ++ [ val, ];
			//	);
			//} {
			//	nil
			//}
		}
	},
));
~pembed = ProtoTemplateDef(\TimelineEmbeder);


/////////////////////////////////////// /////////////////////////////////////// timeline utilities





// archive system deprecated


~eventlist_asArchiveData = { arg eventlist;
	var el = eventlist.collect({ arg ev;
		if(ev[\asArchiveData].notNil) {
			ev = ev.asArchiveData;
		} {
			ev = ev.copy;
			if(ev.use({ev.type}) == \timeline) {
				var tl = ev.use({ev.timeline});
				ev[\timeline]  = tl.asRefArchiveData;
			};
		};
		ev;
	});
	(
		eventlist: el,
		load: { arg self;
			~eventlist_loadArchiveData.(self)
		},
	);
};

~eventlist_loadArchiveData = { arg eventlist_data;
	var el = eventlist_data[\eventlist].collect({ arg ev;
		if(ev[\load].notNil) {
			ev = ev.load;
		} {
			ev = ev.copy;
			if(ev.use({ev.type}) == \timeline) {
				var tl = ev.use({ev.timeline});
				ev[\timeline]  = tl.load;
			};
		};
		ev;
	});
	XEventList.newFrom(el);
};

~eventloop_asArchiveData = { arg eventloop;
	(list: ~eventlist_asArchiveData.(eventloop.list), key: eventloop.key)
};

~eventloop_loadArchiveData = { arg eventloop_data;
	var loop;
	var list;
	//"eventloop_loadArchiveData".debug;
	loop = XEventLoop.newInstance;
	//"eventloop_loadArchiveData y".debug;

	loop.list = if(eventloop_data.notNil) { // FIXME: don't know why could be nil
		~eventlist_loadArchiveData.(eventloop_data.list)
	} {
		XEventList.new.start.finish(1);
	};
	//loop.addList(list);
	//"eventloop_loadArchiveData x".debug;
	//loop.setList(0);
	//"eventloop_loadArchiveData END".debug;
	loop;
};

