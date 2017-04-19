

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

SynthDef(\env_segment, { arg out=0, level=0.1, time=0.1, curve=1;
	var sig;
	var in = In.kr(out);
	sig = EnvGen.kr(Env([in,level], [time], curve),1,doneAction:2);
	sig.poll(label:"env segment");
	Out.kr(out, sig);
}).add;

~pembed = ProtoClass((
	// replace all following function by one class: Pembed, this is the prototype

	new: { arg self, pat, start_offset, stop_offset;
		// FIXME: stop_offset not used (user should use Pfindur instead)
		if(pat.isKindOf(Pattern)) {
			self.embed_pattern(pat, start_offset);
		} {
			Pspawner({ arg spawner;
				self.embed_event(spawner, pat);
			})
		};
		
	},

	embed_pattern: { arg self, pat, start_offset;
		Pspawner({ arg spawner;
			var stream = pat.asStream;
			[pat, start_offset].debug("timeline_pattern: start");

			stream = self.cut_stream(start_offset, stream);

			if(stream.notNil) {
				stream.do({ arg ev;
					self.embed_event(spawner, ev);
				}, Event.default)
			};

		})
		
	},

	embed_event: { arg self, spawner, ev;
		//ev.debug("EMBED event");
		if(ev.type == \pattern) {
			ev = PatternEvent(ev); // because normal Event have embedInStream which replace parent by Event.default
			//ev.parent.keys.debug("EMBED event parent keys (pattern)");
			spawner.par(ev.embedPattern);
			spawner.wait(ev.use {ev.dur}); // FIXME: use delta ?
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
					val.debug("we are on a border, do nothing; ");
				} {
					if( suboffset > val.sustain ) {
						// we are on a rest
						val[\sustain] = val.sustain - suboffset;
						val[\dur] = val.dur - suboffset;
						val[\isRest] = true;
						val.debug("we are on a rest");
					} {
						// we are on a note
						val[\sustain] = val.sustain - suboffset;
						val[\dur] = val.dur - suboffset;
						//val[\event_dropdur] = suboffset;
						val.debug("we are on a note");
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
		[drop_time, stream].debug("entering ~stream_dropdur");
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
						val.debug("stream_dropdur: val");
						[val.dur, val.sustain].debug("stream_dropdur: val: dur, sustain");
						previous_offset = current_offset;
						current_offset = current_offset + val.dur; 
						if(
							// if last event cross the end line
							current_offset <= drop_time and: {
								( previous_offset + val.sustain ) > drop_time
							}
						) {
							val[\event_dropdur] = drop_time - previous_offset + (val[\event_dropdur] ? 0);
							val.debug("stream_dropdur: added to crossing_line");
							crossing_line.add(val);
						};
						[current_offset, previous_offset, val].debug("mangling");
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
					ev.debug("stream_dropdur: transformed crossing_line");
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
						ev.debug("stream_dropdur: transformed crossing_line");
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
			"~stream_dropdur: return original stream untouched".debug;
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
		[eventlist].debug("entering ~eventlist_aspattern");

		eventlist.do { arg ev;
			if(ev[\type] == \start) {
				drop_time = ev[\absTime]
			};
			if(ev[\playDur].notNil) {
				ev[\dur] = ev[\playDur];
			}
		};

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
					val.debug("eventlist_aspattern: val");
					[val.dur, val.sustain].debug("eventlist_aspattern: val: dur, sustain");
					previous_offset = current_offset;
					current_offset = current_offset + val.dur; 
					if(
						val[\type] != \start and: {
							val.sustain.notNil and: {
								( previous_offset + val.sustain ) > drop_time
							}
						}
					) {
						val[\event_dropdur] = drop_time - previous_offset;
						val.debug("eventlist_aspattern: added to crossing_line");
						crossing_line.add(val);
					};
					[current_offset, previous_offset, val].debug("mangling");
				}
			}
		);
		// now current_offset point to the end of current event, ie: past the drop_time line
		// now val is the start event


		if(val.notNil) {

			crossing_line = crossing_line.collect({ arg ev; 
				ev = self.split_event(ev.copy);
				ev[\dur] = 0; // every event start at the same time, \start event hold the delta to next event
				ev.debug("eventlist_aspattern: transformed crossing_line");
			});

			val.use {
				val = val.copy;
				val[\type] = \rest;
			};

			Prout({ arg inval;
				original_eventlist.changed(\cursor, \play);
				"============================= Pembed.embed_eventlist starting routine".debug;
				crossing_line.collect({ arg x; 
					x.debug("crossing_line list");
					x.proto !? { x.proto.keys.debug("-proto") }
				});
				[val].collect({ arg x; 
					x.debug("val list");
					x.proto !? { x.proto.keys.debug("-proto") }
				});
				eventlist.collect({ arg x; 
					x.debug("eventlist list");
					x.proto !? { x.proto.keys.debug("-proto") }
				});
				( original_eventlist.endTime - original_eventlist.startTime ).debug("handling end event in pfindur");
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



//// BEGIN deprecated section
~dereference_event = { arg spawner, ev; 
	[spawner, ev].debug("entering ~dereference_event");
	[ev[\eventType], ev[\nodeType], ev[\type]].debug("eventype, nodetype, type");
	switch(ev[\eventType] ? ev[\nodeType] ? ev[\type],
		\pattern, {
			var pat;
			debug("dereference_event: sub pattern type");
			pat = ev.pattern ?? (ev[\key] !? { Pdef(ev[\key]) } );
			if(ev[\pattern].isNil) {
				pat.debug("pat!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
			};
			spawner.par(Pfindur(ev[\sustain], ~timeline_pattern.(pat, ev[\event_dropdur])));
			spawner.wait(ev[\dur]);
		},
		\timeline, {
			var sustain, timeline, event_dropdur, dur;
			ev.use {
				sustain = ev.sustain;
				timeline = ev.timeline;
				event_dropdur = ev.event_dropdur;
				dur = ev.dur;
			};
			ev.debug("dereference_event: timeline!!!!");
			spawner.par(Pfindur( sustain, timeline.xasPattern(event_dropdur) ));
			spawner.wait(dur);
		},
		\eventlist, {
			var sustain, eventlist, event_dropdur, dur;
			ev.use {
				sustain = ev.sustain;
				eventlist = ev.eventlist;
				event_dropdur = ev.event_dropdur;
				dur = ev.dur;
			};
			ev.debug("dereference_event: eventlist!!!!");
			//ev.sustain.debug("dereference_event: eventlist!!!! sustain");
			//ev.eventlist.debug("dereference_event: eventlist!!!! eventlist");
			//ev.event_dropdur.debug("dereference_event: eventlist!!!! event_dropdur");
			//		~timeline_pattern.(
			//			~eventlist_aspattern.(ev.eventlist),
			//		).play;
			//		"________".debug;
			spawner.par(Pfindur(
				sustain, 
				~timeline_pattern.(
					~eventlist_aspattern.(eventlist),
					event_dropdur,
				)
			));
			spawner.wait(dur);
		},
		\eventenv, {
			var sustain, eventlist, event_dropdur, dur;
			ev.use {
				sustain = ev.sustain;
				eventlist = ev.eventlist;
				event_dropdur = ev.event_dropdur;
				dur = ev.dur;
			};
			ev.debug("dereference_event: eventenv!!!!");
			spawner.par(Pfindur(
				sustain, 
				~eventenv_aspattern.(eventlist, event_dropdur),
			));
			spawner.wait(dur);
		},
		//\player, {
		//	spawner.seq(Pseq([
		//		ev
		//	]))
		//},
		{
			debug("dereference_event: simple event type");
			spawner.seq(Pseq([
				ev
			]))
		}
	);

};

~split_event = { arg val;
	[val].debug("entering ~split_event");
	val.use {
		if(val.notNil) {
			var suboffset = val[\event_dropdur] - (val[\previous_event_dropdur] ? 0);
			if(suboffset == 0) {
				// we are on a border, do nothing;
				val.debug("we are on a border, do nothing; ");
			} {
				if( suboffset > val.sustain ) {
					// we are on a rest
					val[\sustain] = val.sustain - suboffset;
					val[\dur] = val.dur - suboffset;
					val[\isRest] = true;
					val.debug("we are on a rest");
				} {
					// we are on a note
					val[\sustain] = val.sustain - suboffset;
					val[\dur] = val.dur - suboffset;
					//val[\event_dropdur] = suboffset;
					val.debug("we are on a note");
				};
			}
		};
	};
};

~stream_dropdur = { arg drop_time, stream;
	var current_offset = 0;
	var previous_offset = 0;
	var val;
	var crossing_line = List.new;
	[drop_time, stream].debug("entering ~stream_dropdur");
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
					val.debug("stream_dropdur: val");
					[val.dur, val.sustain].debug("stream_dropdur: val: dur, sustain");
					previous_offset = current_offset;
					current_offset = current_offset + val.dur; 
					if(
						current_offset <= drop_time and: {
							( previous_offset + val.sustain ) > drop_time
						}
					) {
						val[\event_dropdur] = drop_time - previous_offset + (val[\event_dropdur] ? 0);
						val.debug("stream_dropdur: added to crossing_line");
						crossing_line.add(val);
					};
					[current_offset, previous_offset, val].debug("mangling");
				}
			}
		);
		// now current_offset point to the end of current event, ie: past the drop_time line


		if(val.notNil) {
			val[\event_dropdur] = drop_time - previous_offset + (val[\event_dropdur] ? 0);
			val = ~split_event.(val.copy);

			crossing_line = crossing_line.collect({ arg ev; 
				ev = ~split_event.(ev.copy);
				ev[\dur] = 0;
				ev.debug("stream_dropdur: transformed crossing_line");
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
				val = ~split_event.(val.copy);
				crossing_line = crossing_line.collect({ arg ev; 
					ev = ~split_event.(ev.copy);
					ev[\dur] = 0;
					ev.debug("stream_dropdur: transformed crossing_line");
				});
				Pseq(
					crossing_line 
					++
					[
						val;
					]
				).asStream;
			} {
				nil
			}
		}

		// first yield all the splitted event concurrent with current val, then val, then remaining stream

	} {
		"~stream_dropdur: return original stream untouched".debug;
		stream;
	};
};

~eventlist_aspattern = { arg eventlist;
	// used only by events of type \eventlist (so not timelines)
	// to enable reading before start event or after end event, add a flag to ignore start and end event
	// will be set by toplevel timeline view
	var current_offset = 0;
	var previous_offset = 0;
	var val;
	var crossing_line = List.new;
	var drop_time;
	var original_eventlist = eventlist;
	eventlist = eventlist.copy;
	[eventlist].debug("entering ~eventlist_aspattern");

	eventlist.do { arg ev;
		if(ev[\type] == \start) {
			drop_time = ev[\absTime]
		};
		if(ev[\playDur].notNil) {
			ev[\dur] = ev[\playDur];
		}
	};

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
				val.debug("eventlist_aspattern: val");
				[val.dur, val.sustain].debug("eventlist_aspattern: val: dur, sustain");
				previous_offset = current_offset;
				current_offset = current_offset + val.dur; 
				if(
					val[\type] != \start and: {
						val.sustain.notNil and: {
							( previous_offset + val.sustain ) > drop_time
						}
					}
				) {
					val[\event_dropdur] = drop_time - previous_offset;
					val.debug("eventlist_aspattern: added to crossing_line");
					crossing_line.add(val);
				};
				[current_offset, previous_offset, val].debug("mangling");
			}
		}
	);
	// now current_offset point to the end of current event, ie: past the drop_time line
	// now val is the start event


	if(val.notNil) {

		crossing_line = crossing_line.collect({ arg ev; 
			ev = ~split_event.(ev.copy);
			ev[\dur] = 0; // every event start at the same time, \start event hold the delta to next event
			ev.debug("eventlist_aspattern: transformed crossing_line");
		});

		val.use {
			val = val.copy;
			val[\type] = \rest;
		};

		Prout({ arg inval;
			original_eventlist.changed(\cursor, \play);
			"============================= FUCKING OALAPLAY !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!".debug;
			"============================= FUCKING OALAPLAY !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!".debug;
			"============================= FUCKING OALAPLAY !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!".debug;
			"============================= FUCKING OALAPLAY !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!".debug;
			crossing_line.collect({ arg x; 
				x.debug("crossing_line list");
				x.proto !? { x.proto.keys.debug("-proto") }
			});
			[val].collect({ arg x; 
				x.debug("val list");
				x.proto !? { x.proto.keys.debug("-proto") }
			});
			eventlist.collect({ arg x; 
				x.debug("eventlist list");
				x.proto !? { x.proto.keys.debug("-proto") }
			});
			( original_eventlist.endTime - original_eventlist.startTime ).debug("handling end event in pfindur");
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
};

~eventenv_aspattern = { arg eventenv, relStartTime, property;
	///////////// Abandoned construction site
	"abandoned".throw;

	//// drop_time is relative to start event
	//// currently, doesnt support embedding others timelines or different event types
	//var str;
	//var first;
	//var key;
	////key = eventenv.param.target.class, eventenv.param.property
	//property = property ? \midinote;
	//relStartTime = relStartTime ? eventenv.relStartTime;
	//str = ~stream_dropdur.(relStartTime, Pn(eventenv));
	//first = str.next(Event.default);
	//str = Prout({
	//			Pbind(
	//				\type, \bus,
	//				\array, [1],
	//				\dur, Pseq([0],1),
	//				\out, BusDef(self.key, \control),
	//			).embedInStream(inval);

	//			pat = Pbind(
	//				\instrument, \env_segment,
	//				\time, Pfunc({ arg ev; ev.debug("ev!!!!!!!!!!!!!!!!!!"); ev[\dur] }),
	//				\level, Pfunc({ arg ev; ev[property] ? default }),
	//				\curve, 0,
	//				\out, BusDef(self.key, \control),
	//			).trace(prefix:"MAIIIIIIIIIIIIIIIIIIIIIIII") 
	//			<> str;
	//			pat.embedInStream(inval)
	//			

	//});
	//Pfindur(eventenv.endTime, str);
};


~timeline_pattern = { arg pat, drop_time, model, is_toplevel=false;
	// take a pattern in so no access to startTime
	// cut and dereference events
	// when dereferencing, the crossings patterns are cut again recursively
	Pspawner({ arg spawner;
		var stream = pat.asStream;
		[pat, drop_time].debug("timeline_pattern: start");

		stream = ~stream_dropdur.(drop_time, stream);

		if(stream.notNil) {

			//model.changed(\cursor, \play);
			stream.do({ arg ev;
				ev.debug("timeline_pattern");
				~dereference_event.(spawner, ev);
				//spawner.wait(ev[\dur]);
			}, Event.default)
		};

	})
};



/////////////////////////////////////// /////////////////////////////////////// event types

~event_get_label = { arg ev;
	var label;
	var receiver = ev[\receiver].value;
	label = if(receiver.notNil) {
		if(receiver.isKindOf(PlayerWrapper)) {
			receiver = receiver.target;
		};
		if(
			receiver.isKindOf(Ndef) or: {
				receiver.isKindOf(Pdef)
			}
		) {
			"% %".format(receiver.class, receiver.key);
		} {
			receiver.asCompileString; // FIXME: could be very long
		}
	} {
		var type;
		//type =  ev[\nodeType] ? ev[\type] ? ev[\eventType] ?
		if(ev[\eventType] == \pattern) {
			if(ev[\pattern].isKindOf(Pdef)) {
				"% %".format(ev[\pattern].class, ev[\pattern].key);
			} {
				if(ev[\key].notNil) {
					"Pdef %".format(ev[\key])
				}
			}
		};
		if(ev[\type] == \timeline) {
			ev.timeline.label
		}
	};
	label = label ?? { "No Name" };
	label
};

~event_type_player = (
	parent: Event.default,
	label: ~event_get_label,
	type: \player,
);

~event_type_timeline = (
	parent: Event.default,
	label: ~event_get_label,
	type: \timeline,
	eventlist: { arg self;
		self.timeline.eventList
	},
);

~event_type_pattern = (
	parent: Event.default,
	label: ~event_get_label,
	eventType: \pattern,
	type: \pattern,

	key: { arg self;
		if(self.receiver.notNil) {
			self.receiver.value.key
		}
	},

	pattern: { arg self;
		if(self.key.notNil) {
			Pdef(self.key);
		};
	},

	isEmbeddable: true,

	embedEvent: { arg self, inval;
		self.pattern.embedInStream(inval);
	}
);

~clear_event_type = { arg ev;
	ev[\isEmbeddable] = nil;
	ev[\embedEvent] = nil;
	ev[\pattern] = nil;
	ev[\key] = nil;
	ev[\type] = nil;
	ev[\eventType] = nil;
	ev[\label] = nil;
	ev[\eventlist] = nil;
	ev;
};

~event_type_eventlist = (
	parent: Event.default,
	label: ~event_get_label,
	type: \eventlist,
	nodeType: \eventlist,

	eventlist: { arg self;
		self.eventloop.list;
	},

	pattern: { arg self;
		~eventlist_aspattern.(self.eventlist);
	},

	isEmbeddable: true,

	embedEvent: { arg self, inval;
		self.pattern.embedInStream(inval);
	}
);

~event_type_eventenv = ~event_type_eventlist.copy.putAll((
	nodeType: \eventenv,
	pattern: { arg self;
		~eventenv_aspattern.(self.eventlist);
	},
));

//~event_type_eventenv = (
//	// not used, ~event_type_eventlist used currently
//	parent: Event.default,
//	label: ~event_get_label,
//	type: \bus,
//	nodeType: \eventlist,
//
//	eventlist: { arg self;
//		self.eventloop.list;
//	},
//
//	pattern: { arg self;
//		~eventlist_aspattern.(self.eventlist);
//	},
//
//	isEmbeddable: true,
//
//	embedEvent: { arg self, inval;
//		self.pattern.embedInStream(inval);
//	}
//);

//Event.partialEvents.keys


~mkevent = { arg event;
	var proto = event.proto;
	if(proto.isNil) {
		event
	} {
		event = event.copy;
		event[\proto] = nil;
		proto = proto.copy;
		proto.putAll(event);
		proto;
	}
};

///// END deprecated section


/////////////////////////////////////// /////////////////////////////////////// timeline utilities

~zoom_fit = { arg timeline;
	// TODO: use timeline.spec to get initial maxy and miny
	var maxy=0, miny=127;
	var range;
	if(timeline.model.size < 3) { // if empty
		// NOOP
	} {

		timeline.model.do { arg ev, x;
			var midi = ev.use { ev.midinote };
			[x, maxy, miny, midi, ev].debug("zoom_fit: midi");
			if(midi.notNil) {
				[x, midi].debug("zoom_fit, notnil");
				if(midi > maxy) {
					maxy = midi;
				};
				if(midi < miny) {
					miny = midi;
				};
			}
		};

		if(( maxy + 1 ) > timeline.areasize.y) {
			timeline.areasize.y = maxy + 1;
		};

		[timeline.viewport, timeline.viewport.top, maxy/timeline.areasize.y, timeline.viewport.height, (maxy-miny)/timeline.areasize.y].debug("ZOOM ZOOOOOOOOOOOOOOOOOOOOM viewport before");
		[timeline.areasize].debug("areasize");
		range = (maxy-miny+1)/timeline.areasize.y;
		timeline.viewport.top = (miny/timeline.areasize.y);
		timeline.viewport.height = range.clip(0.1,1);
		[timeline.viewport.top, timeline.viewport.height, maxy, miny, timeline.areasize.y].debug("top, height, maxy, miny, areasize");

		//timeline.viewport.top = 42/128;
		//timeline.viewport.height = ((54-42)/128);

		timeline.viewport.debug("ZOOM ZOOOOOOOOOOOOOOOOOOOOM viewport");

		timeline.changed(\viewport);
		timeline.refresh;
	}
};


/////////////////////////////////////////////////////////////////////


~remove_envlist_duplicate = { arg eventlist;
	var prev;
	var to_remove = List.new;
	eventlist.do { arg ev;
		if(prev.notNil) {
			if (prev[\level].notNil and: { prev[\level] == ev[\level] and: { prev[\dur] == 0 } }) {
				to_remove.add(prev)
			}
		};
		prev = ev;
	};
	to_remove.do { arg ev;
		eventlist.remove(ev)
	};
};

~compute_eventenv_sustain = { arg eventlist;
	var previous;
	var el = eventlist.copy;
	eventlist.do { arg ev;
		if([\start, \end].includes(ev.type)) {
			// noop
		} {
			if(previous.notNil) {
				if(previous[\dur] != 0) {
					// why ?? legato should always be one, no ?
					// but legato is not used, env_segment use time to know the length
					//previous[\legato] = ( ev[\absTime] - previous[\absTime] ) / previous[\dur];  
					//previous[]
					// sustain is in beats
				};
				previous[\sustain] = ( ev[\absTime] - previous[\absTime] ); // abstime is in beats, sustain is in beats
				//previous[\legato] = 1; // 
				previous[\next_level] = ev[\level];  
				previous[\next_absTime] = ev[\absTime]; // this is a bit useless, time can be computed from dur / tempo 
			};
			previous = ev;

		}
	};
	
};

~eventlist_to_envpattern = { arg eventlist, start;
	//var el = eventlist.copy;
	////var startev = eventlist.
	//var endlist = List.new;
	//var previous;
	//var start_level;
	//var bus_event;
	//~compute_eventenv_sustain.(eventlist);
	//block { arg break;
	//	eventlist.do { arg ev;
	//		if(ev[\type] == \start) {
	//			if(ev.dur == 0 or: { ev.delta == 0 }) {
	//				el.removeAt(0);
	//			};
	//			break.value;
	//		} {
	//			previous = el.removeAt(0);
	//		}
	//	};
	//};
	//if(previous.notNil) {
	//	var level = val.use { val.level };
	//	var busev = val.copy;
	//	var next_level, next_absTime;
	//	next_level = val[\next_level];
	//	next_absTime = val[\next_absTime];
	//	level = 
	//		( next_level - busev.level ) * busev.event_dropdur 
	//		/ (next_absTime - busev.absTime) 
	//		+ busev.level
	//	;
	//	bus_event = (
	//		type: \bus,
	//		array: [level]
	//	)

	//};
};

~pattern_to_envpattern = { arg pattern;
	Prout({ arg inval;
		var str = pattern.asStream;
		var val;
		var first = true;
		var previous_val;
		var nextval; // ugly hack (FIXME: for what ?)
		
		block { arg break;
			while({
				if(nextval.notNil) {
					val = nextval;
				} {
					val = str.next(Event.default);
				};
				val.notNil;
			}, {
				var is_rest = { arg val;
					val.type == \rest or: { val.type == \start or: { val.type == \end } };
				};
				val.debug("pattern_to_envpattern");
				if(first == true) {
					if(is_rest.(val)) {
						val.type.debug("pattern_to_envpattern: start/end");
						previous_val = val;
						previous_val.yield;
					} {
						var level = val.use { val.level };
						var busev = val.copy;
						var next_level, next_absTime;
						level.debug("pattern_to_envpattern: first");
						if(busev[\event_dropdur].notNil) {
							next_level = val[\next_level];
							next_absTime = val[\next_absTime];
							[ next_level, next_absTime, busev ].debug("nlevel, nabstime, busev");
							level = 
								( next_level - busev.level ) * busev.event_dropdur 
								/ (next_absTime - busev.absTime) 
								+ busev.level
							;
						};
						level.debug("pattern_to_envpattern: first: altered level");
						busev.putAll(
							(
								type: \bus,
								array: [level],
								delta: 0,
							)
						);
						busev.yield;
						previous_val = val;
						first = false;
					}
				} {
					if(previous_val.use { previous_val.time } <= 0) {
						previous_val[\array] = [previous_val[\next_level]];
						previous_val[\type] = \bus;
					} {
						previous_val[\level] = previous_val[\next_level];
						previous_val.debug("pattern_to_envpattern: yield previous");
					};
					previous_val.yield;
					previous_val = val;
				};
			});
		};
		//previous_val[\type] = \rest; // this seems to be the cause to the last segment is rest
		if(previous_val.use { previous_val.time } <= 0) {
			previous_val[\array] = [previous_val[\next_level]];
			previous_val[\type] = \bus;
		} {
			previous_val[\level] = previous_val[\next_level];
			previous_val.debug("pattern_to_envpattern: yield previous");
		};
		previous_val.yield;
	})

};


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
	"eventloop_loadArchiveData".debug;
	loop = XEventLoop.newInstance;
	"eventloop_loadArchiveData y".debug;

	loop.list = if(eventloop_data.notNil) { // FIXME: don't know why could be nil
		~eventlist_loadArchiveData.(eventloop_data.list)
	} {
		XEventList.new.start.finish(1);
	};
	//loop.addList(list);
	"eventloop_loadArchiveData x".debug;
	//loop.setList(0);
	"eventloop_loadArchiveData END".debug;
	loop;
};

/////////////////////////////////////////////////////////////////////////////////////




// To be converted in class in Gui.sc
~fxblock = { arg name, player, mixparam, pg;
	VLayout(
		View.new.layout_(

			HLayout(
				//player.asView, //.background_(ParamViewToolBox.color_dark);
				~onoff.(player),
				//StaticText.new.string_(name).background_(ParamViewToolBox.color_dark),
				StaticText.new.string_(name),
				//mixparam.asSlider.orientation_(\horizontal).background_(ParamViewToolBox.color_dark),
				mixparam.asSlider.orientation_(\horizontal),
			).margins_(2),
		).background_(ParamViewToolBox.color_dark),
		ParamGroupLayout.two_panes(pg, \property)
	);
};

~onoff = { arg player;
	var p = PlayerWrapperView(player);
	var view = p.view;
	p.states = { [
		["Off", Color.gray, Color.white],
		["On", Color.black, Color.white], 
	] };
	p.button.fixedSize = 30@20;
	p.button.background_(ParamViewToolBox.color_dark);
	view;
};

if( ~iProxyChain_all.isNil) { 
	~iProxyChain_all = IdentityDictionary.new; 
};
~iProxyChain = (
	all: { ~iProxyChain_all },
	new: { arg self, name, slots, inKey=\inBus;

		if(self.all[name].notNil) {
			self = self.all[name];
		} {
			self = self.deepCopy;
			self.init(slots, inKey).prAdd(name);
		};
	
		self;
	},

	label: { arg self;
		self.name;	
	},

	prAdd: { arg self, name;
		self.name = name;
		self.all[name] = self;
	},

	init: { arg self, slots, inKey;
		self.slots = slots;
		self.inKey = inKey;
		self.isEnabled = 1 ! self.slots.size;
		self.monitor = NodeProxy.new.source_({ arg inBus; InFeedback.ar(inBus, 2); });

		self.resetOrder;
		//self.updateRouting;
		//self.busses = { Bus.audio(s,2) } ! self.slots.size;

		self;
		
	},

	proxyGroup: { arg self;
		GroupDef(self.name);
	},

	updateRouting: { arg self;
		var in = self.inBus;
		var out;
		if(in.notNil) {
			self.proxyOrder.do { arg slot_idx, i;
				var slot;
				slot_idx.debug("slot_idx");
				slot = self.slots[slot_idx];
				if(slot.notNil and: { self.isEnabled[slot_idx] != 0 }) {
					[slot_idx, in, i].debug("in!!!");
					slot.xset(self.inKey, in.asControlInput);
					//slot.parentGroup = self.proxyGroup;
					//self.proxyGroup.moveNodeToTail(slot.group);
					//slot.play;
					slot.send;
					in = slot.bus;
				}
			};
			self.lastIn = in;
			//self.proxyOrder.asArray.reverse.do { arg slot_idx;
			//	var slot;
			//	slot_idx.debug("slot_idx2");
			//	slot = self.slots[slot_idx];
			//	if(slot.notNil and: { self.isEnabled[slot_idx] != 0 }) {
			//		slot.play(self.inBus.asControlInput, 2);
			//		slot.set(self.inKey, self.inBus.asControlInput);
			//	}
			//};

			[self.monitor, in, self.monitor.get(\inBus)].debug("monitor!!!");
			self.monitor.xset(\inBus, in);
			[self.monitor, in, self.monitor.get(\inBus)].debug("monitor!!! after");
			//self.proxyGroup.moveNodeToHead(self.monitor.group);
			//self.monitor.group.moveNodeToTail(Group(s));
			//self.slots[self.proxyOrder.asArray.last].play(0)
		};
	},

	eventPlay: { arg self;
		self.updateRouting;
		self.monitor.play;
	},

	eventStop: { arg self;
		self.monitor.xset(\inBus, self.inBus);
		self.monitor.play;
	},

	eventIsPlaying: { arg self;
		self.monitor.monitor.isPlaying and: { 
			self.monitor.get(\inBus) != self.inBus
		};
	},

	enableLink: { arg self, x, onezero=1;
		self.isEnabled[x] = onezero;
		self.updateRouting;
	},

	enabledLinkParam: { arg self, x;
		Param({ self.isEnabled[x] }, { arg val; self.enableLink(x, val) }, ControlSpec(0,1,\lin,1));
	},

	resetOrder: { arg self;
		self.proxyOrder = (0..self.slots.size-1);
	},

	proxyOrder_: { arg self, seq;
		self[\proxyOrder] = OrderedIdentitySet.newFrom(seq.asArray.keep(self.slots.size));
		self.updateRouting;
	},

	xadd: { arg self, proxy;
		self.slots.add(proxy);
		self.isEnabled.add(1);
		self.proxyOder.add(self.slots.size-1);
		self.updateRouting;
	},

	xput: { arg self, i, proxy;
		self.slots.put(i, proxy);
		self.isEnabled.put(i, 1);
		self.updateRouting;
		//self.order.pu(self.slots.size-1);
	},

	xinsert: { arg self, i, proxy;
		self.slots.insert(i, proxy);
		self.isEnabled.insert(i, 1);
		self.updateRouting;
		//self.order.pu(self.slots.size-1);
	},

	removeAt: { arg self, idx;
		self.slots.removeAt(idx);
		self.isEnabled.removeAt(idx);
		self.proxyOrder.remove(idx);
		self.updateRouting;
	}

);


