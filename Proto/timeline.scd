
// "~/code/sc/seco/vlive/demo/param/lib/timeline.scd".standardizePath.load;


SynthDef(\env_segment, { arg out=0, level=0.1, time=0.1, curve=1, gate=1;
	var sig;
	var in = In.kr(out);
	sig = EnvGen.kr(Env([in,level], [time], curve),gate,doneAction:2);
	sig.poll(label:"env segment");
	Out.kr(out, sig);
}).add;

Event.addEventType(\player, {
	var method = ~method ? \play;
	var args = ~arguments ? [];
	var receiver = ~receiver.value;
	var quant;
	[method, args, receiver].debug("player EventType: method, args, receiver");
	if(~disableQuant != false) {
		quant = receiver.tryPerform(\quant);
		receiver.tryPerform(\quant_, 0);
	};
	receiver.perform(method, *args);
	if(~disableQuant != false) {
		receiver.tryPerform(\quant_, quant);
	};
	if(method == \play) {
		TempoClock.default.sched(~sustain.value(currentEnvironment), {
			if(~disableQuant != false) {
				quant = receiver.tryPerform(\quant);
				receiver.tryPerform(\quant_, 0);
			};
			receiver.perform(\stop, *args);
			if(~disableQuant != false) {
				receiver.tryPerform(\quant_, quant);
			};
			
		}.inEnvir);
	};

});

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

///////////////////////////////////////////////////////////////////// timeline objects


~eventTimeline = (

	prAdd: { arg self, name;
		self.all[name] = self;
		self.key = name;
		self;
	},

	makeDefaultEventList: { arg self;
		var el = XEventList.new;
		el.start;
		el.finish(1);
	},

	asPatternAbs: { arg self, startPosition, endPosition;
		var relStartTime, totalDur;
		var eventlist = self.eventList;
		// currentlty, startPosition and endPosition are in absolute time in the GUI, but will be relative to start event in futur
		if(startPosition.isNil) {
			relStartTime = eventlist.relStartTime;
		} {
			relStartTime = ( startPosition - eventlist.firstTime ).clip(0,inf);
			// now relStartTime is independent of start event
			relStartTime = eventlist.relStartTime max: relStartTime;
			// now relStartTime is always after start event
		};

		if(endPosition.isNil) {
			totalDur = self.eventloop.list.totalDur - (startPosition - eventlist.startTime ).clip(0,inf);
		} {
			totalDur = endPosition - (startPosition ? eventlist.startTime);
		};
		[startPosition, endPosition, relStartTime, totalDur].debug("asPatternAbs: start, end, relstart, totdur");
		self.xasPattern(relStartTime, totalDur);
	},

	eventList: { arg self;
		self.eventloop.list
	},

	embedInTimeline: { arg self, drop_dur, sustain;
		var relStartTime;
		relStartTime = self.eventList.relStartTime + drop_dur;
		self.xasPattern(relStartTime, self.eventList.totalDur min: sustain);
	},

	xembedInStream: { arg self, inval;
		self.xasPattern
	},

	eventPlay: { arg self;
		[ self.cursor.startPosition, self.cursor.endPosition ].debug("eventPlay: start, end");
		self.asPatternAbs(self.cursor.startPosition, self.cursor.endPosition).play;
	},

	eventStop: { arg self;
		self.proxy.stop;
	},

	eventIsPlaying: { arg self;
		self.proxy.isPlaying;
	},

	label: { arg self;
		self.key;
	},


	repeatCount: inf,

);

if(~clipTimeline_all.isNil) {
	~clipTimeline_all = IdentityDictionary.new;
};
~clipTimeline = (
	parent: ~eventTimeline,
	all: { ~clipTimeline_all },
	eventType: \clipTimeline,

	new: { arg self, name;

		if(self.all[name].notNil) {
			self = self.all[name]
		} {
			self = self.deepCopy;
			self.prAdd(name).init;
		};
	
		self;
	},

	init: { arg self;
		var el = self.makeDefaultEventList;
		self.areasize = 10@4;
		self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;
		"=============clipTimeline: end init".debug;

		self;
	},

	xasPattern: { arg self, relStartTime, totalDur;
		self.proxy.source = Plazy({
			var eventlist = self.eventList;

			relStartTime = relStartTime ? eventlist.relStartTime;
			totalDur = totalDur ? eventlist.totalDur;

			[relStartTime, totalDur].debug("clip xasPattern: plazy: relstart, totdur");

			self.eventloop.list.changed(\cursor, \play);
			Pfindur(totalDur, ~timeline_pattern.(
				Pn(eventlist, 1),
				relStartTime
			));
		}).repeat(self.repeatCount);
		self.proxy;
	},

	edit: { arg self;
		~clipTimelinePanelView.new(self);
	},

);


if(~noteTimeline_all.isNil) {
	~noteTimeline_all = IdentityDictionary.new;
};
~noteTimeline = (
	parent: ~eventTimeline,
	all: { ~noteTimeline_all },

	eventType: \noteTimeline,
	composeParamPattern: true,

	new: { arg self, name, param;

		if(self.all[name].notNil) {
			self = self.all[name]
		} {
			self = self.deepCopy;
			self.prAdd(name).init(param);
		};
	
		self;
	},

	init: { arg self, param;

		var el = self.makeDefaultEventList;
		self.param = param;
		self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment
		self.areasize = 9@128;
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;

		// note: this is grid quant, not playing quant
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code
		
	},

	xasPattern: { arg self, relStartTime, totalDur;
		// there is 3 kinds of call to asPattern:
		// - the gui: currently relative startPosition to absTime_0 : asPatternAbs
		// - the live code: no startPosition given, defaulting to start event : asPattern
		// - when embedded in bigger timeline: startPosition is relative to start event : embedInTimeline

		// timeline_pattern take a start time relative to firstEvent
		// XEventList.relStartTime is relative to firstEvent
		self.proxy.source = Plazy({
			var eventlist = self.eventloop.list;
			var pat;

			relStartTime = relStartTime ? eventlist.relStartTime;
			totalDur = totalDur ? eventlist.totalDur;

			self.eventloop.list.changed(\cursor, \play);
			pat = Pfindur(totalDur, ~timeline_pattern.(
				Pn(self.eventloop.list, 1),
				relStartTime
			));

			if(self.composeParamPattern == true) {
				pat = pat <> self.param.target;
			};

			pat;
		}).repeat(self.repeatCount);
		self.proxy;
	},

	edit: { arg self;
		~noteTimelinePanelView.new(self)
	},

	asView: { arg self;
		~noteTimelinePanelView.newView(self).makeLayout;
	},

);

if(~envTimeline_all.isNil) {
	~envTimeline_all = IdentityDictionary.new;
};
~envTimeline = (
	parent: ~eventTimeline,
	all: { ~envTimeline_all },
	eventType: \envTimeline,

	new: { arg self, name, param;
	
		if(self.all[name].notNil) {
			self = self.all[name]
		} {
			self = self.deepCopy;
			self.prAdd(name).init(param);
		};
	
		self;
	},

	newInstance: { arg self, param, bus;
		// param target should be a bus
		self = self.deepCopy;
		self.key = \instance;
		param.debug("envTimeline: newInstance: param");
		self[\outBus] = param.target;
		"xx".debug;
		self.init(param)
	},

	levelParam: { arg self;
		// this is a fake param,
		// target is not used, property is the key where data is stored (\level) and his spec
		// destined to TimelineEnvView
		if(self.param.notNil) {
			Param(self.param.target, \level, self.param.spec);
		} {
			// FIXME: this is wrong
			Param((), \level);
		}
	},

	outBus: { arg self;
		BusDef(self.key, \control);
	},

	// when clicking play in GUI, it call to asPatternAbs
	//listenToCursor: { arg self;
	//	self.cursor_controller = SimpleController(self.cursor).put(\refresh, {
	//		self.asPatternAbs;
	//	})
	//},

	init: { arg self, param;

		var el = self.makeDefaultEventList;
		self.param = param;
		self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment
		self.areasize = 9@128;
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;

		// note: this is grid quant, not playing quant
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code

		self.xasPattern; // init proxy

		self;
		
	},


	xasPattern: { arg self, relStartTime, totalDur;
		[relStartTime, totalDur].debug("xasPattern: relstart, totdur");
		self.proxy.source = Prout({ arg inval;
			var eventlist = self.eventloop.list;
			var pat;
			var str;
			var first;
			var first_level;

			relStartTime = relStartTime ? eventlist.relStartTime;
			totalDur = totalDur ? eventlist.totalDur;

			[relStartTime, totalDur].debug("xasPattern: prout: relstart, totdur");

			// FIXME: this should be an event
			if(self.param.notNil and:{ self.param.target.isKindOf(Bus).not }) {
				self.param.set(self.outBus.asMap);
				//default = self.param.default;
			};

			~compute_eventenv_sustain.(self.eventloop.list);

			pat = 
				Pbind(
					\instrument, \env_segment,
					//\level, Pfunc({ arg ev; ev[self.param.property] ? default }),
					\curve, 0,
					\out, self.outBus,
					\time, Pfunc({ arg ev; ev.debug("ev!!!!!!!!!!!!!!!!!!"); ev.use { ev.sustain } }),
				).trace(prefix:"MAIIIIIIIIIIIIIIIIIIIIIIII")
				<>
				Pfindur(totalDur, ~timeline_pattern.(
					Pn(self.eventloop.list, 1),
					relStartTime
				))
			;

			self.eventloop.list.changed(\cursor, \play);
			~pattern_to_envpattern.(pat).embedInStream(inval);

		}).repeat(self.repeatCount);
		self.proxy;
	},

	edit: { arg self;
		~envTimelinePanelView.new(self)
	},

);


///////////////////////////////////////////////////////////////////// timeline panel


~timelinePanelView = (
	new: { arg self, eventloop;
		self = self.deepCopy;

		self.eventloop = eventloop;
		self.areasize = 9@1;
		self.quant_ref = Ref(Point(1/8,1));

		self.definePattern;

		self;
	},

	areasize: { arg self;
		if(self.timeline.notNil) {
			self.timeline.areasize 
		} {
			self.pr_areasize;
		}
	},

	areasize_: { arg self, val;
		if(self.timeline.notNil) {
			self.timeline.areasize = val;
		};
		self.pr_areasize = val;
	},

	makeRulerY: { arg self, timeline;
		//self.ruler_y = MidinoteTimelineRulerView.new;
		//self.ruler_y.mimicTimeline(timeline);
		//self.ruler_y;
		nil
	},

	make_stop_button: { arg self;
		XSimpleButton.new.label_("[]").action_({
			self.cursor.startPosition = self.eventloop.list.startTime;
			PlayerWrapper(self.timeline_model).stop;
			self.cursor_timeline.stop; // FIXME: should be automatic when stoping Pdef
		});
	},

	make_quant_popup: { arg self;
		var quant_popup;
		var quant_spec;
		quant_spec = MenuSpec([
			"0",
			"1/32",
			"1/16",
			"1/8",
			"1/4",
			"1/2",
			"1",
			"2",
			"4",
			"8",
			"16",
		].collect({ arg x; x -> x.interpret }));

		self.quant_param = Param({ self.get_xquant }, { arg val; self.set_xquant(val) }, quant_spec);
		quant_popup = self.quant_param.asPopUpMenu;
		self.quant_popup = quant_popup;
		self.quant_popup;
	}, 

	make_header: { arg self;

		//self.header_layout = HLayout(
		//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
		//	self.make_stop_button,
		//	XSimpleButton.new.label_("Add track").action_({
		//		self.add_track;
		//	}),
		//	self.make_quant_popup,
		//);
		//
		//self.header_layout;
		View.new;
	},

	definePattern: { arg self;
		Pdef(self.eventloop.key, Plazy({
			//PtimeGatePunch(self.eventloop.list.asPattern.trace(prefix:"xxxxxxxxxxxxxxxxxxxxx"))
			var start, end;
			var eventlist = self.eventloop.list;
			if(self.cursor.notNil) {
				start = self.cursor.startPosition - eventlist.startTime;
				if(self.cursor.endPosition.notNil) {
					end = self.cursor.endPosition - eventlist.endTime;
				}
			};
			~timeline_pattern.(
				~eventlist_aspattern.(
					self.eventloop.list
				).trace(prefix:"xxxxxxxxxxxxxxxxxxxxx"),
				start, end
			);
			//self.eventloop.list.asPattern.trace(prefix:"xxxxxxxxxxxxxxxxxxxxx")
		}));
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		window.alwaysOnTop = true;
		window.front;	
	},

	add_track: { arg self;
		self.areasize = Point(self.areasize.x, self.areasize.y+1 );
		self.timeline.refreshEventList;
		self.timeline.refresh;
		self.update_track_header;
	},

	zoom_fit: { arg self;
		[self.areasize, self.timeline.areasize].debug("1");
		~zoom_fit.(self.timeline);
		[self.areasize, self.timeline.areasize].debug("2");
		self.areasize = self.timeline.areasize;
		[self.areasize, self.timeline.areasize].debug("4");
	},

	update_track_header: { arg self;
		var track_header_block = { arg x;
			var res = View.new.layout_(
				HLayout(
					XSimpleButton.new.label_("track %".format(x))
				)
			);
			//res.background_(Color.blue);
			res;
		};
		self.track_header.canvas_(
			self.track_header_view = View.new.layout_(
				VLayout(*
					self.areasize.y.asInteger.collect { arg x;
						[
							track_header_block.(x),
							align: \top,
						]
					}
				).spacing_(0).margins_(0)
			);
			self.track_header_view
		).maxWidth_(100);
		self.update_track_header_size;
		self.track_header.onResize = { self.update_track_header_size };
		if(self.track_header_controller.notNil) {
			self.track_header_controller.remove;
		};
		self.track_header_controller = SimpleController(self.timeline)
		.put(\viewport, {
			"update_track_header_size: viewport".debug;
			//TODO: remove controller
			self.update_track_header_size;
		})
		.put(\areasize, {
			"update_track_header_size: areasize".debug;
			self.update_track_header_size;
			//self.update_track_header;
		})
		;
	},

	update_track_header_size: { arg self;
		//self.vrange.view.doAction;
		if(self.timeline.notNil) {
			"update_track_header_size: real updating now!".debug;
			[self.timeline.bounds.height, self.timeline.viewport.height].debug("bh, vh");
			self.track_header_view.fixedHeight = self.timeline.bounds.height / self.timeline.viewport.height;
			self.track_header.visibleOrigin = Point(0,(1-self.timeline.viewport.bottom) * self.track_header.bounds.height);
			self.timeline.viewport.top.debug("top");
			[self.timeline.viewport, self.timeline.viewport.bottom, self.track_header.bounds.height, (1-self.timeline.viewport.bottom) * self.track_header.bounds.height].debug("[self.timeline.viewport, self.timeline.viewport.bottom, self.track_header.bounds.height, (1-self.timeline.viewport.bottom) * self.track_header.bounds.height]");
			self.track_header.visibleOrigin.debug("update_track_header_size: visibleOrigin");
		}
	},

	set_xquant: { arg self, quant;
		self.quant_ref.value = Point(quant, self.quant_ref.value.y);
	},

	get_xquant: { arg self;
		self.quant_ref.value.x;
	},

	set_yquant: { arg self, quant;
		self.quant_ref.value = Point(self.quant_ref.value.x, quant);
	},

	get_yquant: { arg self;
		self.quant_ref.value.y;
	},

	make_track_header: { arg self;
		var track_header;
		track_header = ScrollView.new;
		track_header.hasBorder = false;
		track_header.hasVerticalScroller = false;
		track_header.hasHorizontalScroller = false;
		self.track_header = track_header;

		self.vrange.view.addAction({ arg slider;
			self.track_header_view.fixedHeight = 1/slider.range * track_header.bounds.height;
			( 1/slider.range * track_header.bounds.height ).debug("vrange.view.action: fixedHeight");
			track_header.visibleOrigin = Point(0, ( 1-slider.hi ) / slider.range) * track_header.bounds.height;
			( Point(0, ( 1-slider.hi ) / slider.range) * track_header.bounds.height ).debug("vrange.view.action: visibleOrigin");
		});

		self.update_track_header;
		self.track_header;
	},

	makeLayout: { arg self;
		var layout;
		var tl_layout;
		var vlayout, timeline, vrange, hrange, size;
		var eventloop = self.eventloop;
		var ruler_x, ruler_y, sizer;
		var locatorbar;
		var sizer_action;
		var cursor_timeline;
		var track_header;
		var track_header_view;
		var quant_popup;
		"AZE".debug;
		layout = HLayout.new;
		self.layout = layout;
		vlayout = VLayout.new;
		layout.add(vlayout);

		timeline = self.timelineClass.new;
		timeline.areasize = self.pr_areasize.debug("==================seting areasize");
		~mytl = timeline;
		timeline.mouseDownAction = {|me, px, py, mod, buttonNumber, clickCount, chosennode|
			if(clickCount == 2) {
				if(chosennode.notNil) {
					~clipEditor.new(chosennode.model, self.eventloop.list)
				};
			}
		};

		timeline.quant = self.quant_ref;

		//self.cursor = CursorTimeline.new;
		//self.cursor.startPosition = self.eventloop.list.startTime;

		cursor_timeline = CursorTimelineView.new;
		~cursor_timeline = cursor_timeline;
		self.cursor_timeline = cursor_timeline;
		cursor_timeline.mapCursor(self.cursor);
		cursor_timeline.quant = self.quant_ref;


		ruler_x = TimelineRulerView.new;
		ruler_x.mapCursor(self.cursor);
		locatorbar = TimelineLocatorBarView.new;

		locatorbar.quant = self.quant_ref;

		ruler_x.mimicTimeline(timeline);
		locatorbar.mimicTimeline(timeline);
		cursor_timeline.mimicTimeline(timeline);

		hrange = TimelineScroller.new;
		hrange.mapTimeline(timeline);
		hrange.orientation = \horizontal;
		vrange = TimelineScroller.new;
		vrange.mapTimeline(timeline);
		vrange.orientation = \vertical;
		self.vrange = vrange;

		sizer = 
		HLayout(
			XSimpleButton.new.label_("<").action_({
				var point = Point(4,0);
				self.areasize = self.areasize - point;
				timeline.refresh;
			}).fixedSize_(Point(10,20)),
			XSimpleButton.new.label_(">").action_({
				var point = Point(4,0);
				self.areasize = self.areasize + point;
				timeline.refresh;
			}).fixedSize_(Point(10,20)),
		).spacing_(0)
		;


		track_header = self.make_track_header(timeline);


		tl_layout = GridLayout.rows(
			[
				nil,
				VLayout(
					locatorbar.view,
					ruler_x.view,
				),
				XSimpleButton.new.label_("z")
					.maxWidth_(10)
					.action_({
						self.zoom_fit;
					})
			],
			[
				track_header,
				StackLayout(
					cursor_timeline.view,
					timeline.view,
				).mode_(1),
				vrange.view,
			],
			[
				nil,
				hrange.view,
				sizer,
			]
		);
		tl_layout.spacing = 0;

		layout = VLayout(
			self.make_header,
			tl_layout,
		);

		size = self.areasize;
		size.debug("size!!!");

		timeline.areasize = size;

		ruler_x.view.minSize = Point(10,10);
		//ruler_y.view.minSize = Point(40,10);
		locatorbar.view.minSize = Point(10,15);

		timeline.mapEventList(eventloop.list);
		cursor_timeline.mapEventList(eventloop.list);
		locatorbar.mapEventList(eventloop.list);

		self.timeline = timeline;
		self.layout = layout;
		"iiAZE".debug;


		self.zoom_fit;

		layout;
	},

	asPattern: { arg self;
		Pfunc({})

	},

	refresh: { arg self;
		self.timeline.mapEventList(self.eventloop.list);
	},
);

~clipEditor = (
	new: { arg self, model, parent;
		self = self.deepCopy;
	
		self.model = model;
		self.parent_model = parent;
		//self.makeWindow;
		
	
		self;
	},

	clearEditor: { arg self;
		self.type_popup.items = [""];
		self.name_popup.items = [""];
	},

	makeUpdater: { arg self;
		self.controller.remove;
		self.controller = SimpleController(self.model).put(\refresh, {
			var ev = self.model;
			[self.type_popup, ev].debug("entering clipEditor updater");
			if(self.type_popup.isNil or: {self.type_popup.isClosed}) {
				self.controller.remove;
			} {
				self.refreshPopups;
				switch(ev[\eventType] ? ev[\nodeType] ? ev[\type],
					\player, {
						ev.debug("clipEditor: updater: player");
						self.type_popup.value = self.type_popup.items.detectIndex({ arg x; x == \Ndef });
						self.name_popup.items = Ndef.all[\localhost].envir.keys.asArray;
						self.name_popup.value = self.name_popup.items.detectIndex({ arg x; 
							x == ( ev.key ?? { ev[\receiver].value.key } )
						});
						[self.name_popup.value, self.name_popup.items, ev[\key], ev[\receiver], ev[\receiver].value.key].debug("name, items, key, rec, reckey");
					},
					\pattern, {
						ev.debug("clipEditor: updater: pattern");
						self.type_popup.value = self.type_popup.items.detectIndex({ arg x; x == \Pdef });
						self.name_popup.items = Pdef.all.keys.asArray;
						self.name_popup.value = self.name_popup.items.detectIndex({ arg x; 
							x == ( ev.key ?? { ev[\receiver].value.key } ) 
						});
						[self.name_popup.value, self.name_popup.items, ev[\key], ev[\receiver], ev[\receiver].value.key].debug("name, items, key, rec, reckey");
					},
					\timeline, {
						var type;
						ev.debug("clipEditor: updater: timeline");
						if(ev.timeline.notNil) {
							switch(ev.timeline.eventType,
								\envTimeline, {
									type = 'EnvTimeline'
								},
								\clipTimeline, {
									type = 'ClipTimeline'
								},
								\noteTimeline, {
									type = 'noteTimeline'
								},
							)
						};
						self.type_popup.value = self.type_popup.items.detectIndex({ arg x; x == type });

						self.name_popup.items = ev.timeline.all.keys.asArray;
						self.name_popup.value = self.name_popup.items.detectIndex({ arg x; 
							x == ( ev.key ?? { ev.timeline.key } ) 
						});
						[self.name_popup.value, self.name_popup.items, ev.key].debug("name, items, key, rec, reckey");
					}, {

						ev.debug("clipEditor: updater: other");
					}
				)
			}
		});
	},

	refreshPopups: { arg self;
		var name_popup = self.name_popup;
		self.type_popup.items = [
			'Pdef',
			'Ndef',
			'ClipTimeline',
			'NoteTimeline',
			'EnvTimeline',
		];
		self.type_popup.action = {
			switch( self.type_popup.items[self.type_popup.value],
				\Ndef, {
					if(Ndef.all[\localhost].notNil) {
						name_popup.items = Ndef.all[\localhost].envir.keys.asArray;
					} {
						name_popup.items = [];
					}
				},
				\Pdef, {
					name_popup.items = Pdef.all.keys.asArray;
				},
				\NoteTimeline, {
					name_popup.items = ~noteTimeline.all.keys.asArray;
				},
				\ClipTimeline, {
					name_popup.items = ~clipTimeline.all.keys.asArray;
				},
				\EnvTimeline, {
					name_popup.items = ~envTimeline.all.keys.asArray;
				}
			);
		};
		self.type_popup.valueAction = 0;
	},

	makeLayout: { arg self;
		var layout;
		var header;
		var editor;
		var type_popup, name_popup;
		var model = self.model;
		type_popup = PopUpMenu.new;
		name_popup = PopUpMenu.new;
		self.name_popup = name_popup;
		self.type_popup = type_popup;
		name_popup.allowsReselection = true;
		//type_popup.allowsReselection = true; // lose selection
		//name_popup.items = type_popup.items[type_popup.value].asClass.all.keys.asArray;
		name_popup.action = {
			var curname = name_popup.items[name_popup.value];
			var model = self.model;
			switch( type_popup.items[type_popup.value],
				\Ndef, {
					model.putAll(~event_type_player);
					model[\receiver] = Ref(PlayerWrapper(Ndef(curname)));
					//model.changed(\refresh);
					self.parent_model.changed(\refresh);
				},
				\Pdef, {
					model.putAll(~event_type_pattern);
					model.receiver = Ref(Pdef(curname));
					//model.changed(\refresh);
					self.parent_model.changed(\refresh);
				},
				\NoteTimeline, {
					model.putAll(~event_type_timeline);
					model[\timeline] = ~noteTimeline.new(curname);
					self.parent_model.changed(\refresh);
				},
				\ClipTimeline, {
					model.putAll(~event_type_timeline);
					model[\timeline] = ~clipTimeline.new(curname);
					self.parent_model.changed(\refresh);
				},
				\EnvTimeline, {
					model.putAll(~event_type_timeline);
					model[\timeline] = ~envTimeline.new(curname);
					self.parent_model.changed(\refresh);
				},
			);
		};

		self.refreshPopups;

		header = HLayout(
			type_popup,
			name_popup,
			XSimpleButton.new.label_("Edit").action_({
				var model = self.model;
				[model.type, model].debug("Edit: type");
				switch(model.type,
					\player, {
						var player = model.receiver.value;
						if(player.class == PlayerWrapper) {
							player = player.target;
						};
						if(player.getHalo(\edit).notNil) {
							player.getHalo(\edit).value;
						} {
							player.edit;
						}
					},
					\pattern, {
						var player = model.receiver.value;
						player.asParamGroup.debug("Edit: pattern");
						if(player.class == PlayerWrapper) {
							player = player.target;
						};
						player.debug("clipeditor: player");
						player.getHalo(\edit).debug("clipeditor: player, edit");
						if(player.getHalo(\edit).notNil) {
							player.getHalo(\edit).debug("clipeditor: player, halo edit");
							player.getHalo(\edit).value;
						} {
							player.getHalo(\edit).debug("clipeditor: normal edit");
							player.asParamGroup.edit;
						}
					},
					\timeline, {
						model.timeline.edit;
					}
				);
			})
		);
		layout = VLayout(
			header,
			editor,
		);

		self.layout = layout;
		self.layout;
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		window.alwaysOnTop = true;
		window.front;

		self.makeUpdater;
		self.model.changed(\refresh);
	},

);

//////////////////////////////// Views

~clipTimelinePanelView = (
	parent: ~timelinePanelView,

	new: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
		
		self.makeWindow;
	
		self;
	},

	cursor: { arg self;
		self.timeline_model.cursor;
	},

	key: { arg self;
		self.timeline_model.key;
	},

	param: { arg self;
		self.timeline_model.param;
	},


	make_header: { arg self;
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			XSimpleButton.new.label_("Add track").action_({
				self.add_track;
			}),
			self.make_quant_popup,
		);
		
		self.header_layout;
	},

	timelineClass: { arg self;
		PdefTimelineView
	},

	makeLayout: { arg self;
		self.clipEditor = ~clipEditor.new(nil, self.timeline_model.eventList);
		self.clipEditorLayout = self.clipEditor.makeLayout;
		self.clipEditor.clearEditor;
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
			[self.parent[\makeLayout].(self), stretch:1],
			self.clipEditorLayout,
			//self.parent.makeLayout,
		);
		self.timeline.mouseDownAction = { arg me, px, py, mod, buttonNumber, clickCount, chosennode;
			if(chosennode.notNil) {
				self.clipEditor.model = chosennode.model;
				self.clipEditor.makeUpdater;
				chosennode.model.changed(\refresh);
			} {
				self.clipEditor.model = nil;
				self.clipEditor.clearEditor;
			}
		};
		self.layout;
	},
);

~envTimelinePanelView = (
	parent: ~timelinePanelView,

	new: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		// self.timeline is timelineView actually
		self.timeline_model = timeline;

		self.makeWindow;
		
		self;
	},

	newView: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		// self.timeline is timelineView actually
		self.timeline_model = timeline;

		self;
	},

	cursor: { arg self;
		self.timeline_model.cursor;
	},

	key: { arg self;
		self.timeline_model.key;
	},

	param: { arg self;
		self.timeline_model.param;
	},

	levelParam: { arg self;
		self.timeline_model.levelParam;
	},

	timelineClass: { arg self;
		TimelineEnvView;
	},

	make_track_header: { arg self, timeline;
		//self.track_header = View.new;
		//self.ruler_y = MidinoteTimelineRulerView.new;
		//self.track_header = self.ruler_y;
		//self.ruler_y.mimicTimeline(timeline);
		//self.ruler_y.view.minWidth_(50);
		nil
	},

	make_header: { arg self;
		HLayout(
			PlayerWrapperView(self.timeline_model).view,
			if(self.param.target.isKindOf(Bus)) {
				nil;
			} {
				PlayerWrapperView(PlayerWrapper(self.param.target)).view;
			},
			self.make_quant_popup,
		)
	},

	makeLayout: { arg self;
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
			self.parent[\makeLayout].(self),
			//self.parent.makeLayout,
		);
		self.timeline.eventFactory = { arg pos, nodesize;
			(absTime: pos.x, level: pos.y, legato:1);
		};
		self.timeline.mapParam(self.levelParam);
		self.set_yquant(self.param.spec.step);
		self.timeline.mouseUpAction = {|me, px, py, mod, buttonNumber, clickCount, chosennode|
			self.timeline.refreshEventList;
		};
		self.layout;
	},
);


~noteTimelinePanelView = (
	parent: ~timelinePanelView,

	new: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
		self.makeWindow;
	
		self;
	},

	newView: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
	
		self;
	},

	cursor: { arg self;
		self.timeline_model.cursor;
	},

	key: { arg self;
		self.timeline_model.key;
	},

	param: { arg self;
		self.timeline_model.param;
	},


	make_header: { arg self;
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			Param(Message(self.timeline_model), \repeatCount, ControlSpec(1,inf,\lin)).asNumberBox,
			self.make_quant_popup,
		);
		
		self.header_layout;
	},


	timelineClass: { arg self;
		MidinoteTimelineView;
	},

	make_track_header: { arg self, timeline;
		//self.track_header = View.new;
		self.ruler_y = MidinoteTimelineRulerView.new;
		self.track_header = self.ruler_y;
		self.ruler_y.mimicTimeline(timeline);
		self.ruler_y.view.minWidth_(50);
	},

	makeLayout: { arg self;
		"noteTimelinePanelView.makeLayout".debug;
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(EventPlayerWrapper(self.timeline_model)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
		"noteTimelinePanelView.makeLayout bef".debug;
			self.parent[\makeLayout].(self),
			//self.parent.makeLayout,
		);
		"noteTimelinePanelView.makeLayout af".debug;
		self.layout;
	},

);


/////////////////////////////////////////////////////////////////////



~compute_eventenv_sustain = { arg eventlist;
	var previous;
	var el = eventlist.copy;
	eventlist.do { arg ev;
		if([\start, \end].includes(ev.type)) {
			// noop
		} {
			if(previous.notNil) {
				if(previous[\dur] != 0) {
					previous[\legato] = ( ev[\absTime] - previous[\absTime] ) / previous[\dur];  
				};
				previous[\next_level] = ev[\level];  
				previous[\next_absTime] = ev[\absTime];  
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
		var nextval; // ugly hack
		
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
					if(previous_val.use { previous_val.sustain} <= 0) {
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
		previous_val[\type] = \rest;
		previous_val.yield;
	})

};


/////////////////////////////////////////////////////////////////////////////////////




// To be converted in class in Gui.sc
	~fxblock = { arg name, player, mixparam, pg;
		VLayout(
			View.new.layout_(

				HLayout(
					//player.asView, //.background_(ParamView.color_dark);
					~onoff.(player),
					//StaticText.new.string_(name).background_(ParamView.color_dark),
					StaticText.new.string_(name),
					//mixparam.asSlider.orientation_(\horizontal).background_(ParamView.color_dark),
					mixparam.asSlider.orientation_(\horizontal),
				).margins_(2),
			).background_(ParamView.color_dark),
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
		p.button.background_(ParamView.color_dark);
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

