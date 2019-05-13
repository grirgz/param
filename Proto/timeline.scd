
///////////////////////////////////////////////////////////////////// timeline objects


~eventTimeline = ProtoClass((
	proxyQuantIsDefault: true, // the only way to preserve default proxy quant

	prAdd: { arg self, name;
		[self.all, name].debug("prAdd");
		self.all[name] = self;
		[self.all, name].debug("prAdd2");
		self.key = name;
		self;
	},

	addInstance: { arg self, name; // workaround to not clash with ProtoDef prAdd
		self[\prAdd].(self, name)
	},

	clear: { arg self;
		self.all[self.key] = nil;
		nil
	},

	makeDefaultEventList: { arg self;
		var el = XEventList.new;
		el.start;
		el.finish(8);
		el;
	},

	makeDefaultEventLoop: { arg self;
		var loop = XEventLoop.newInstance;
		loop.list = self.makeDefaultEventList;
		//loop.setList(0);
		loop;
	},

	asPatternAbs_futur: { arg self, startPosition, endPosition;

		//var sloop = self.loopmaster.get
		//self.xasPattern(relStartTime, totalDur);
	},

	asPatternAbs: { arg self, startPosition, endPosition, repeatCount;
		var relStartTime, totalDur;
		var eventlist = self.eventList;
		// currentlty, startPosition and endPosition are in absolute time in the GUI, but will be relative to start event in futur
		// this code translate absolute in relative (to start event)

		// this is the time between first event absTime and asked start time
		// because silence before first event can't be counted in Pembed
		// if startPosition is before first event, relStartTime is negative, not handled by Pembed for the moment
		// relStartTime need to be relative to the first event, because Pembed only see the relative time to the first event and doesn't read \absTime
		
		relStartTime = {
			var sp = startPosition.value;
			if(sp.isNil) {
				self.eventList.relStartTime; // start time relative to first event
			} {
				var xx;
				xx = ( sp - self.eventList.firstTime ).clip(0,inf); // now relStartTime is independent of start event
				self.eventList.firstTime max: xx; // now relStartTime is always after first event
			};

		};

		// total dur is absEnd - absStart (eventlist.totalDur is already that)
		totalDur = {
			var absStart = relStartTime.value + self.eventList.firstTime;
			var ep = endPosition.value;
			var xx;
			if(ep.notNil and:{ep - absStart < 0.01}) { // prevent too short cursor loop
				ep = eventlist.endTime;
			};
			//xx = if(ep.isNil) {
				////totalDur = self.eventloop.list.totalDur - (startPosition - eventlist.startTime ).clip(0,inf);
				//self.eventlist.totalDur;
			//} {
				////totalDur = endPosition - (startPosition ? eventlist.startTime);
				//(( ep ? eventlist.endTime ) - absStart);
			//};
			xx = (( ep ? eventlist.endTime ) - absStart);
			// prevent empty (infinite loop)
			if(xx < 0.01) {
				Log(\Param).debug("timeline.asPattern: loop too short, setting it to 2: xx", xx);
				2;
			} {
				xx
			}
		};

		//[startPosition, endPosition, relStartTime, totalDur].collect(_.value).debug("asPatternAbs: start, end, relstart, totdur");
		self.xasPattern(relStartTime, totalDur, repeatCount);
	},

	eventList: { arg self;
		self.eventloop.list
	},

	eventList_: { arg self, val;
		self.eventloop.list = val;
		self;
	},

	eventLoop: { arg self;
		self.eventloop;
	},

	eventLoop_: { arg self, val;
		self.eventloop = val;
		self;
	},

	embedInTimeline: { arg self, drop_dur, sustain;
		var relStartTime;
		relStartTime = self.eventList.relStartTime + drop_dur;
		self.xasPattern(relStartTime, self.eventList.totalDur min: sustain);
	},

	embedInStream: { arg self, inval;
		self.asPattern
	},

	xembedInStream: { arg self, inval;
		self.embedInStream(inval);
	},

	play: { arg self;
		//[ self.cursor.startPosition, self.cursor.endPosition ].debug("eventPlay: start, end");
		var pat = self.asSelectedPattern;
		if(self.proxy != pat) {
			self.proxy.source = pat;
		};
		self.proxy.play;
	},

	playNow: { arg self, repeatCount;
		//[ self.cursor.startPosition, self.cursor.endPosition ].debug("eventPlay: start, end");
		var pat = self.asSelectedPattern(repeatCount);
		if(self.proxy != pat) {
			// FIXME: this destroy use of defaultQuant, with little workaround, ugly
			var oldquant = self.proxy.quant; 
			self.proxy.quant = 0;
			self.proxy.setSource(pat);
			self.proxy.stop;// Pmono doesnt supprot doReset
			self.proxy.play(quant:0);
			//self.proxy.play(quant:0, doReset:true);
			//PlayerWrapper(self.proxy).playNow;
			if(self.proxyQuantIsDefault) {
				self.proxy.quant = nil
			} {
				self.proxy.quant = oldquant;
			}
		} {
			//self.proxy.play(quant:0, doReset:true);
			self.proxy.stop;// Pmono doesnt supprot doReset
			self.proxy.play(quant:0);
			//PlayerWrapper(self.proxy).playNow;
		};
	},

	stopNow: { arg self;
		//[ self.cursor.startPosition, self.cursor.endPosition ].debug("eventPlay: start, end");
		PlayerWrapper(self.proxy).stopNow;
		self.eventloop.list.changed(\cursor, \stop);
	},

	asSelectedPattern: { arg self, repeatCount;
		self.asPatternAbs({self.cursor.startPosition}, {self.cursor.endPosition}, repeatCount)
	},

	stop: { arg self;
		self.proxy.stop;
		PlayerWrapper(self.proxy).doWithQuant({
			self.eventloop.list.changed(\cursor, \stop);
		})
	},

	isPlaying: { arg self;
		self.proxy.isPlaying;
	},

	label: { arg self;
		self.key;
	},

	quant: { arg self;
		self.proxy.quant;
	},

	quant_: { arg self, val;
		self.proxyQuantIsDefault = val.isNil;
		self.proxy.quant = val;  // so PlayerEvent can set quant of timelines
	},


	repeatCount: inf,

	/////////////// backward compat (old way before ProtoClass)

	eventIsPlaying: { arg self;
		self.isPlaying;
	},

	eventStop: { arg self;
		self.stop;
	},

	eventPlay: { arg self; 
		self.play;
	},

	xclear: { arg self;
		self.clear;
	},

	xasPattern: { arg self ...args;
		self.asPattern(*args);
	},

	startRecording: { arg self;
		self.isRecording = true;
	},

	stopRecording: { arg self;
		self.isRecording = false;
	},

	isRecording: { arg self;
		if(self.recorder.notNil) {
			self.recorder.isRecording;
		} {
			false
		}
	},

	isRecording_: { arg self, val;
		// deprecated
		//if(self.eventFactory.notNil) {
		//	self.eventFactory.isRecording = val;
		//}

		if(self.recorder.notNil) {
			self.recorder.isRecording = val;
		}
	},

	asPlayerEvent: { arg self;
		PlayerEvent((
			receiver: ( "{ " ++ self.refCompileString ++ " }" ).interpret; // to have closed compile string
		))
	},

	asPatternEvent: { arg self;
		PatternEvent((
			//eventType: \timeline, // i want to get ride of this type system // done! 
			timeline: ( "{ " ++ self.refCompileString ++ " }" ).interpret; // to have closed compile string
		))
		
	},

	refCompileString: { arg self, name;
		"~eventTimeline(..)"	
	},

	presetCompileString: { arg self;
		"%.eventList = %".format(self.refCompileString(self.key.asCompileString), self.eventList.presetCompileString)
	},

	eventList_presetCompileString: { arg self, eventList;
		var ret = "";
		ret = eventList.collect({ arg ev;
			"\t(%),\n".format(
				// ev.asCompileString return stuff from parent event, so looping manually
				ev.keys.as(Array).sort.collect({ arg key;
					"%%: %, ".format("\\",key, ev[key].asCompileString)
				}).join
			)
		}).join;
		ret = "XEventList.newFrom([\n%]);".format(ret);
		ret;
	},

	savePresetCompileString: { arg self, path, action, force_dialog=false;
		var save_it = { arg mypath;
			var myfolderpath = PathName(mypath).pathOnly;
			var myfolderpathname;
			myfolderpathname = FileSystemProject.resolve(myfolderpath);
			if(myfolderpathname.notNil) {
				mypath = myfolderpathname.fullPath ++ PathName(mypath).fileName;
				"Trying to write preset to file %".format(mypath.asCompileString).postln;
				File.use(mypath, "w", { arg file;
					var relpath = FileSystemProject.unresolve(mypath);
					var preset;
					self.presetCompileStringSavePath = relpath;
					file.write("%.presetCompileStringSavePath = %;\n\n".format(self.refCompileString, relpath.asCompileString));
					
					preset = self.presetCompileString;
					if(preset.isNil) {
						"ERROR: no preset found for this object".postln;
					} {
						file.write(preset);
						"Preset written: %".format(mypath).postln;
					};
				});
				action.()
			} {
				"ERROR: savePresetCompileString: Can't resolve file %".format(mypath).postln;
			};
		};
		if(path.notNil) {
			save_it.(path)
		} {
			if(self.presetCompileStringSavePath.notNil and: { force_dialog==false }) {
				save_it.(self.presetCompileStringSavePath)
			} {
				Dialog.savePanel({ arg mypath;
					//path.postln;
					save_it.(mypath)
				},{
					//"cancelled".postln;
				});
			}

		};
	},
));

//if(~clipTimeline_all.isNil) {
//	~clipTimeline_all = IdentityDictionary.new;
//};
ClipTimeline.proto = ProtoClass((
	parent: ~eventTimeline,
	all: { PresetDictionary.new(\clipTimeline) },
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
		// TODO: make accept a PlayerWrapperGroup as optional argument to handle recording
		self.areasize = 10@4;
		//self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment
		self.eventloop = self.makeDefaultEventLoop;

		//self.eventFactory = ~class_player_event_factory.new;
		//self.eventFactory.eventLoop = self.eventloop;
		//self.eventFactory.mode = \note;
		//self.eventFactory.playmode = \gate;
		//self.eventFactory.fullrecord = false;

		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;
		self.selection_cursor = CursorTimeline.new;
		self.tracks_bypassed = Set.new;
		self.recorder = ProtoTemplateDef(\SampleClipTimelineRecorder).new(self);
		//"=============clipTimeline: end init".debug;

		self;
	},

	new_load: { arg self, data;
		self = self.deepCopy;
		self.prAdd(data.name).init(data.param);
		self.eventloop = ~eventloop_loadArchiveData.(data.eventloop);
		self.eventList.reorder;
		self.areasize = data.areasize;
		self.cursor.startPosition = self.eventList.startTime;
		self.quant_ref = data.quant_ref;
		self.tracks_bypassed = data.tracks_bypassed;
		self;
	},

	asArchiveData: { arg self;
		(
			name: self.key,
			param: self.param,
			eventloop: ~eventloop_asArchiveData.(self.eventList),
			areasize: self.areasize,
			quant_ref: self.quant_ref,
			tracks_bypassed: self.tracks_bypassed,
			load: { arg me; ~clipTimeline.new_load(me) },
		)
	},

	asRefArchiveData: { arg self;
		(
			key: self.key,
			load: { arg me; ~clipTimeline.new(me.key) },
		)
	},

	bypass_track: { arg self, track_index, val=true;
		if(val) {
			self.tracks_bypassed.add(track_index);
		} {
			self.tracks_bypassed.remove(track_index);
		}
	},

	toggle_bypass_track: { arg self, track_index;
		if(self.is_track_bypassed(track_index)) {
			self.bypass_track(track_index, false)
		} {
			self.bypass_track(track_index, true)
		}
	},

	is_track_bypassed: { arg self, track_index;
		self.tracks_bypassed.includes(track_index);
	},

	bypass_pattern: { arg self, pat;
		Pcollect({ arg ev;
			//"entering pcollect".debug;
			ev = ev.copy;
			if(self.tracks_bypassed.includes(ev.use({ev.midinote}))) {
				ev[\type] = \rest;
				ev[\eventType] = nil;
				ev[\nodeType] = nil;
			};
			//"ending pcollect".debug;
			ev;
		}, pat)	
	},

	asPattern_futur: { arg self, relStartTime, totalDur;
		self.proxy.source = Plazy({
			var eventlist = self.eventList;
			var sloop = self.loopmaster.getStandardLoop(relStartTime, totalDur);

			self.eventloop.list.changed(\cursor, \play);
			Pfindur(sloop.totalDur, Pembed(
				self.bypass_pattern( Pn(eventlist.clone, 1) ),
				sloop.relStartTime
			));
		}).repeat(self.repeatCount);
		self.proxy;
	},

	asPattern: { arg self, relStartTime, totalDur, repeatCount;
		self.proxy.source = Plazy({
			var eventlist = self.eventList;
			var xrelStartTime, xtotalDur;

			xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			xtotalDur = totalDur.value ? eventlist.totalDur;

			//[xrelStartTime, xtotalDur].debug("clip xasPattern: plazy: relstart, totdur");
			if(xtotalDur <= 0) {
				"ERROR: totalDur is negative or zero".postln;
				xtotalDur = 2
			};

			self.eventloop.list.changed(\cursor, \play); // FIXME: should be an event
			Pfindur(xtotalDur, Pembed(
				self.bypass_pattern( Pn(eventlist.clone, 1) ),
				xrelStartTime
			));
		}).repeat(repeatCount ? self.repeatCount);
		self.proxy;
	},

	edit: { arg self;
		var x = ~clipTimelinePanelView.new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		~clipTimelinePanelView.newView(self).makeLayout;
	},

	refCompileString: { arg self, name;
		if(name.isNil) {
			name = self.key.asCompileString
		};
		"ClipTimeline(%)".format(name)
	},
));
~clipTimeline = ClipTimeline.proto;

~trackTimeline = ProtoClass((
	parent: ~clipTimeline,
	all: { PresetDictionary.new(\trackTimeline) },
	new: { arg self, name, tracks;

		if(self.all[name].notNil) {
			self = self.all[name]
		} {
			self = self.deepCopy;
			self.prAdd(name).init(tracks);
		};
	
		self;
	},

	init: { arg self, tracks;
		var eventfac;
		if(tracks.isKindOf(TrackDef)) {
			tracks = PlayerWrapperGroup(tracks.children);
		};
		self.areasize = 10@4;
		//self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment
		self.eventloop = self.makeDefaultEventLoop;
		eventfac = ~class_track_event_factory.new(tracks);
		self.eventFactory = { eventfac };
		self.eventFactory.eventLoop = self.eventloop;
		self.eventFactory.mode = \note;
		self.eventFactory.playmode = \gate;
		self.eventFactory.fullrecord = false;
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;
		self.selection_cursor = CursorTimeline.new;
		self.tracks_bypassed = Set.new;

		self.tracks = { tracks };

		//"=============trackTimeline: end init".debug;

		self;
	},


	asArchiveData: { arg self;
		(
			name: self.key,
			param: self.param,
			eventloop: ~eventloop_asArchiveData.(self.eventList),
			areasize: self.areasize,
			quant_ref: self.quant_ref,
			tracks_bypassed: self.tracks_bypassed,
			load: { arg me; ~trackTimeline.new_load(me) },
		)
	},

	asRefArchiveData: { arg self;
		(
			key: self.key,
			load: { arg me; ~trackTimeline.new(me.key) },
		)
	},


	edit: { arg self;
		var x = ~trackTimelinePanelView.new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		~trackTimelinePanelView.newView(self).makeLayout;
	},


	refCompileString: { arg self, name;
		if(name.isNil) {
			name = self.key.asCompileString
		};
		"TrackTimeline(%)".format(name)
	},
));


//if(~noteTimeline_all.isNil) {
//	~noteTimeline_all = IdentityDictionary.new;
//};
~noteTimeline = ProtoClass((
	parent: ~eventTimeline,
	all: { PresetDictionary.new(\noteTimeline) },

	eventType: \noteTimeline,
	composeParamPattern: true,

	new: { arg self, name, param;

		if(self.all[name].notNil) {
			self = self.all[name];
			if(param.notNil) { self.param = param };
		} {
			self = self.deepCopy;
			self.prAdd(name).init(param);
		};
	
		self;
	},

	init: { arg self, param;

		self.eventloop = self.makeDefaultEventLoop;
		self.param = param; // depends on eventloop
		self.areasize = 10@128;
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;
		self.selection_cursor = CursorTimeline.new;

		// TODO: make a recorder and deprecate eventFactory
		//self.recorder = ~playerGroupRecorder.new(self.wrapper);
		//self.recorder.eventLoop = self.eventLoop;

		// note: this is grid quant, not playing quant
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code
		
	},

	param_: { arg self, param;
		if(param.isNil) {
			param = Param(Pdef(\NoteTimeline_default, Pbind()), \midinote);
		};
		if(param.isKindOf(Pdef)) {
			param = Param(param, \note);
		};
		self[\param] = param;
		self.eventFactory = ~class_event_factory.new(self.param.target);
		self.eventFactory.eventLoop = self.eventloop;
		self.eventFactory.mode = \note;
		self.eventFactory.playmode = \gate;
		self.eventFactory.fullrecord = false;

		//if(kit.isSequenceableCollection) {
			//self[\kit] = kit;
			//self.wrapper = ~parPlayerGroup.new(self.kit);
		//} {
			//self.wrapper = kit;
			//self[\kit] = { arg self; self.wrapper.targets };
		//};
		//self.recorder = ~playerGroupRecorder.new(self.wrapper);
		//self.recorder.eventLoop = self.eventLoop;
	},

	new_load: { arg self, data;
		self = self.deepCopy;
		self.prAdd(data.name).init(data.param);
		self.eventloop = ~eventloop_loadArchiveData.(data.eventloop);
		self.eventList.reorder;
		self.areasize = data.areasize;
		self.cursor.startPosition = self.eventList.startTime;
		self.quant_ref = data.quant_ref;
		self;
	},

	asArchiveData: { arg self;
		(
			name: self.key,
			param: self.param,
			eventloop: ~eventloop_asArchiveData.(self.eventloop),
			areasize: self.areasize,
			quant_ref: self.quant_ref,
			load: { arg me; ~noteTimeline.new_load(me) },
		)
	},

	asRefArchiveData: { arg self;
		(
			key: self.key,
			load: { arg me; ~noteTimeline.new(me.key) },
		)
	},

	asPattern: { arg self, relStartTime, totalDur, repeatCount;
		//[relStartTime.value, totalDur.value].debug("relstart, totaldur");
		// there is 3 kinds of call to asPattern:
		// - the gui: currently relative startPosition to absTime_0 : asPatternAbs
		// - the live code: no startPosition given, defaulting to start event : asPattern
		// - when embedded in bigger timeline: startPosition is relative to start event : embedInTimeline
		//		if startOffset, relStartTime is adjusted

		// timeline_pattern take a start time relative to firstEvent
		// XEventList.relStartTime is relative to firstEvent
		// FIXME: replacing source in proxy at call time means that a playing proxy will play immediately the new source when asked its pattern
		//		could have nasty side effects
		//self.proxy.source = Plazy({
		var pat = Plazy({
			var eventlist = self.eventloop.list;
			var pat;
			var xrelStartTime, xtotalDur;

			xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			xtotalDur = totalDur.value ? eventlist.totalDur;

			self.eventloop.list.changed(\cursor, \play);
			//pat = Pfindur(xtotalDur, ~timeline_pattern.(
			//	Pn(self.eventloop.list, 1),
			//	xrelStartTime
			//));

			//[xrelStartTime, relStartTime.value, eventlist.relStartTime].debug("xrel, rel, evrel");
			//[xtotalDur, totalDur.value, eventlist.totalDur].debug("xrel, rel, evrel");

			if(xtotalDur <= 0) {
				"ERROR: totalDur is negative or zero".postln;
				xtotalDur = 2
			};

			pat = Pfindur(xtotalDur, Pembed(
				Pn(self.eventloop.list.clone, 1),
				xrelStartTime
			));

			if(self.param.notNil and: {self.composeParamPattern == true}) {
				if(self.eventFactory.notNil and: {self.eventFactory.mode == \event}) {
					//"Event mode!".debug;
					pat = self.param.target <> pat;
				} {
					//"Normal mode!".debug;
					pat = pat <> self.param.target;
				}
			};

			pat;
		}).repeat(repeatCount ? self.repeatCount);
		//self.proxy;
		pat;
	},

	outBus: { arg self;
		self.proxy.get(\out)
	},
	
	outBus_: { arg self, val;
		self.proxy.set(\out, val)
	},


	edit: { arg self;
		var x = ~noteTimelinePanelView.new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		~noteTimelinePanelView.newView(self).makeLayout;
	},

	refCompileString: { arg self, name;
		if(name.isNil) {
			name = self.key.asCompileString
		};
		"NoteTimeline(%)".format(name)
	},

));

// why not name it ParamTimeline ????
~envTimeline = ProtoClass((
	parent: ~eventTimeline,
	all: { PresetDictionary.new(\envTimeline) },
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
		//param.debug("envTimeline: newInstance: param");
		//"xx".debug;
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
		// overriden in init
		BusDef(self.key, \control);
	},


	// when clicking play in GUI, it call to asPatternAbs
	//listenToCursor: { arg self;
	//	self.cursor_controller = SimpleController(self.cursor).put(\refresh, {
	//		self.asPatternAbs;
	//	})
	//},

	param_: { arg self, param;
		self[\param] = param;
		if(self.recorder.notNil) {
			self.recorder.stopRecording;
		};
		self.recorder = ~paramRecorder.new(self.param);
		self.recorder.eventLoop = self.eventLoop;

		if(self.customBus.isNil) {
			if(param.target.isKindOf(Bus)) {
				self[\outBus] = param.target;
			} {
				// FIXME: should always call newInstance with a bus param
				// else everything write to the same bus
				self[\outBus] = BusDef(self.key, \control);
			};
		} {
			self[\outBus] = self.customBus;
		};
	},

	init: { arg self, param, bus;
		self.eventloop = self.makeDefaultEventLoop;
		self.areasize = 10@128;
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;
		self.selection_cursor = CursorTimeline.new;
		self.customBus = bus;

		self.param = param;

		//eventFactory system is deprecated
		//self.eventFactory = ~class_param_event_factory.new(self.param);
		//self.eventFactory.eventLoop = self.eventloop;
		//self.eventFactory.mode = \note;
		//self.eventFactory.playmode = \gate;
		//self.eventFactory.fullrecord = false;


		// note: this is grid quant, not playing quant
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code

		self.xasPattern; // init proxy

		self;
		
	},

	new_load: { arg self, data;
		self = self.deepCopy;
		self.prAdd(data.name).init(data.param);
		self.eventloop = ~eventloop_loadArchiveData.(data.eventloop);
		self.eventList.reorder;
		self.areasize = data.areasize;
		self.cursor.startPosition = self.eventList.startTime;
		self.quant_ref = data.quant_ref;
		self.xasPattern;
		self;
	},

	asArchiveData: { arg self;
		(
			name: self.key,
			param: self.param,
			eventloop: ~eventloop_asArchiveData.(self.eventloop),
			areasize: self.areasize,
			quant_ref: self.quant_ref,
			load: { arg me; ~envTimeline.new_load(me) },
		)
	},

	asRefArchiveData: { arg self;
		// TODO: what if it's an instance, not a def ?
		// maybe no need for this "if", just call asArchiveData when needed ?
		if(self.key == \instance) {
			self.asArchiveData
		} {
			(
				key: self.key,
				load: { arg me; ~envTimeline.new(me.key) },
			)
		}
	},


	asPattern: { arg self, relStartTime, totalDur;
		//[relStartTime, totalDur].debug("xasPattern: relstart, totdur");
		self.proxy.source = Plazy({ arg inval;
			var eventlist = self.eventloop.list;
			var pat;
			var str;
			var first;
			var first_level;
			var xrelStartTime, xtotalDur;	
			var postpat, prepat;

			xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			xtotalDur = totalDur.value ? eventlist.totalDur;

			//[xrelStartTime, xtotalDur].debug("xasPattern: prout: relstart, totdur");
			self.eventloop.list.changed(\cursor, \play);

			if(xtotalDur <= 0) {
				"ERROR: totalDur is negative or zero".postln;
				xtotalDur = 2
			};


			// FIXME: this should be an event
			if(self.param.notNil and:{ self.param.target.isKindOf(Bus).not }) {
				// FIXME: bus mode should be generic
				if(self.param.target.class == Pdef) {
					self.param.target.set(self.param.property, self.outBus.asMap)
				} {
					self.param.set(self.outBus.asMap);
				};
				//default = self.param.default;
			};

			~remove_envlist_duplicate.(eventlist);
			~compute_eventenv_sustain.(eventlist);

			postpat = Pbind(
				\out, self.outBus,
				//\level, Pfunc({ arg ev; ev[self.param.property] ? default }),
				\time, Pfunc({ arg ev; ev.use { 
					// absTime is in beats (but maybe should be in seconds ?)
					// need to convert to seconds
					//(ev.next_absTime ? ev.absTime) - ev.absTime / thisThread.clock.tempo
					// use dur instead
					( ev[\sustain] ? 1 ) / thisThread.clock.tempo
				} }),
				\type, Pfunc({  arg ev;

					if(ev[\type].isNil or: { ev[\type] == \note }) {
						if(ev[\next_level].isNil) { // last one should be rest
							\rest
						} {
							if(ev[\time].notNil and: {ev[\time] < 0.051}, {
								ev[\array] = [ ev[\level] ];
								//ev.debug("envTimeline: asPattern: busmode!");
								\bus;
							}, { 
								\note;
							});
						}

					} {
						ev[\type]
					}
				})
			);
			//postpat = postpat.trace(prefix:"MAIIIIIIIIIIIIIIIIIIIIIIII"); // debug
			prepat = Pbind(
				\instrument, \env_segment,
				\curve, 0,
			);

			pat = Pfindur(xtotalDur, Pembed(
				Pn(self.eventloop.list.clone, 1), 
				xrelStartTime
			));
			pat = postpat <> pat <> prepat;

			//~pattern_to_envpattern.(pat).embedInStream(inval ? ()); 
			~pattern_to_envpattern.(pat);

		}).repeat(self.repeatCount);
		self.proxy;
	},

	edit: { arg self;
		var x = ~envTimelinePanelView.new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		~envTimelinePanelView.newView(self).makeLayout;
	},

	refCompileString: { arg self, name;
		if(name.isNil) {
			name = self.key.asCompileString
		};
		"ParamTimeline(%)".format(name)
	},
));

~kitTimeline = ProtoClass((
	parent: ~eventTimeline,
	all: { PresetDictionary.new(\kitTimeline) },

	eventType: \kitTimeline,
	composeParamPattern: true,

	new: { arg self, name, kit;

		if(self.all[name].notNil) {
			self = self.all[name];
			if(kit.notNil) { self.kit = kit };
		} {
			self = self.deepCopy;
			self.prAdd(name).init(kit);
		};
	
		self;
	},

	outBus: { arg self;
		self.proxy.get(\out)
	},
	
	outBus_: { arg self, val;
		self.proxy.set(\out, val)
	},

	init: { arg self, kit;

		self.eventloop = self.makeDefaultEventLoop;
		self.kit = kit;
		self.areasize = 10@128;
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;
		self.selection_cursor = CursorTimeline.new;

		// note: this is grid quant, not playing quant
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code
		
	},

	kit_: { arg self, kit;
		if(kit.isSequenceableCollection) {
			self[\kit] = kit;
			self.wrapper = ~parPlayerGroup.new(self.kit);
		} {
			self.wrapper = kit;
			self[\kit] = { arg self; self.wrapper.targets };
		};
		self.recorder = ~playerGroupRecorder.new(self.wrapper);
		self.recorder.eventLoop = self.eventLoop;
	},

	new_load: { arg self, data;
		self = self.deepCopy;
		self.prAdd(data.name).init(data.param);
		self.eventloop = ~eventloop_loadArchiveData.(data.eventloop);
		self.eventList.reorder;
		self.areasize = data.areasize;
		self.cursor.startPosition = self.eventList.startTime;
		self.quant_ref = data.quant_ref;
		self;
	},

	isRecording: { arg self;
		self.recorder.isRecording;
	},

	startRecording: { arg self;
		self.recorder.startRecording;
	},

	stopRecording: { arg self;
		self.recorder.stopRecording;
	},

	isRecording_: { arg self, val;
		self.recorder.isRecording = val;
	},

	asArchiveData: { arg self;
		(
			name: self.key,
			param: self.param,
			eventloop: ~eventloop_asArchiveData.(self.eventloop),
			areasize: self.areasize,
			quant_ref: self.quant_ref,
			load: { arg me; ~noteTimeline.new_load(me) },
		)
	},

	asRefArchiveData: { arg self;
		(
			key: self.key,
			load: { arg me; ~noteTimeline.new(me.key) },
		)
	},

	asPattern: { arg self, relStartTime, totalDur;
		// there is 3 kinds of call to asPattern:
		// - the gui: currently relative startPosition to absTime_0 : asPatternAbs
		// - the live code: no startPosition given, defaulting to start event : asPattern
		// - when embedded in bigger timeline: startPosition is relative to start event : embedInTimeline

		// timeline_pattern take a start time relative to firstEvent
		// XEventList.relStartTime is relative to firstEvent
		self.proxy.source = Plazy({
			var eventlist = self.eventloop.list;
			var pat;
			var xrelStartTime, xtotalDur;

			xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			xtotalDur = totalDur.value ? eventlist.totalDur;

			self.eventloop.list.changed(\cursor, \play);
			//pat = Pfindur(xtotalDur, ~timeline_pattern.(
			//	Pn(self.eventloop.list, 1),
			//	xrelStartTime
			//));
			if(xtotalDur <= 0) {
				"ERROR: totalDur is negative or zero".postln;
				xtotalDur = 2
			};


			pat = Pfindur(xtotalDur, Pembed(
				Pn(self.eventloop.list.clone, 1), // clone avoid lose of sync when adding note but require to wait the end of loop
				xrelStartTime
			));


			if(self.kit.notNil and: {self.composeParamPattern == true}) {
				pat = PdrumStep(self.kit, pat, 1, key: \kitIndex);
				if(self.wrapper.postChain.notNil) {
					//"post!".debug;
					pat = self.wrapper.postChain <> pat;
				};
				if(self.postChain.notNil) {
					//"post!".debug;
					pat = self.postChain <> pat;
				};
				if(self.wrapper.preChain.notNil) {
					//"pre!".debug;
					pat = pat <> self.wrapper.preChain;
				};
				if(self.preChain.notNil) {
					//"pre!".debug;
					pat = pat <> self.preChain;
				};
			};

			//if(self.param.notNil and: {self.composeParamPattern == true}) {
			//	if(self.eventFactory.mode == \event) {
			//		"Event mode!".debug;
			//		pat = self.param.target <> pat;
			//	} {
			//		"Normal mode!".debug;
			//		pat = pat <> self.param.target;
			//	}
			//};

			pat;
		}).repeat(self.repeatCount);
		self.proxy;
	},

	edit: { arg self;
		var x = ~kitTimelinePanelView.new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		~kitTimelinePanelView.newView(self).makeLayout;
	},

	presetCompileString: { arg self;
		"%.eventList = %".format(self.refCompileString(self.key.asCompileString), self.recorder.presetCompileString)
	},


	refCompileString: { arg self, name;
		if(name.isNil) {
			name = self.key.asCompileString
		};
		"KitTimeline(%)".format(name)
	},

));

//////////////////////////////////////////// loading archived data
~noteTimeline.all.loadIfNotInitialized;
~envTimeline.all.loadIfNotInitialized;
~clipTimeline.all.loadIfNotInitialized;

///////////////////////////////////////////////////////////////////// timeline panel

////////////////// clip editor (really: clip chooser/selector)

~clipEditor = (
	new: { arg self, model, parent;
		self = self.deepCopy;
	
		self.model = model;
		self.parent_model = {parent};
		//self.makeWindow;
		
		self.init_default_clip_factories;
	
		self;
	},

	init_default_clip_factories: { arg self;
		var tl;
		self.clipFactories = ();

		self.clipFactories.ndef = (
			key: 'ndef',
			label: { arg self; self.key.asString },
			keylist: {
				if(Ndef.all[\localhost].notNil) {
					Ndef.all[\localhost].envir.keys.asArray.sort;
				} {
					[];
				}
			},

			getName: { arg self, ev;
				ev.receiver.value.key;
			},

			editSelected: { arg self, model;
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

			redefineClip: { arg me, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = PlayerWrapper(Ndef(curname));
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
			},


		);

		self.clipFactories.pdef = (
			key: 'pdef',
			label: { arg self; self.key.asString },
			keylist: {
				Pdef.all.keys.asArray.sort; 
			},

			getTarget: { arg self, ev;
				// depends pattern event, player event
				if(ev.pattern.isKindOf(Pdef)) {
					// pattern type
					ev.pattern;
				} {
					// player type
					var tar = ev.receiver.value;
					tar.key; // PlayerWrapper has key wrapper
					if(tar.isKindOf(PlayerWrapper)) {
						tar = tar.target;
					};
					if(tar.isKindOf(Pdef)) {
						tar;
					} {
						Log(\Param).error("clipFactories.pdef: ERROR: getTarget: don't know what type!!!");
						nil;
					}; 
				}
				
			},

			getName: { arg self, ev;
				self.getTarget(ev).key;
			},

			editSelected: { arg self, model;
				var player;
				//player = model.receiver.value;
				//player.asParamGroup.debug("Edit: pattern");
				//if(player.class == PlayerWrapper) {
				//	player = player.target;
				//};
				//player.debug("clipeditor: player");
				//player.getHalo(\edit).debug("clipeditor: player, edit");
				player = self.getTarget(model);
				if(player.isKindOf(Pdef)) {
					if(player.getHalo(\edit).notNil) {
						//player.getHalo(\edit).debug("clipeditor: player, halo edit");
						player.getHalo(\edit).value;
					} {
						//player.getHalo(\edit).debug("clipeditor: normal edit");
						player.asParamGroup.edit;
					}
				} {
					"clipFactories.pdef: ERROR: edit: don't know what type!!!".debug;
				}
			},

			redefineClip: { arg me, curname, model;
				PatternEvent.redefine(model);
				model[\pattern] = Pdef(curname);
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
			},
		);

		self.clipFactories.trackdef = (
			key: 'trackdef',
			label: { arg self; self.key.asString },
			keylist: {
				TrackDef.all.keys.asArray.sort; 
			},

			getName: { arg self, ev;
				ev.receiver.value.key;
			},

			editSelected: { arg self, model;
				var player = model.receiver.value;
				if(player.class == PlayerWrapper) {
					player = player.target;
				};
				if(player.getHalo(\edit).notNil) {
					player.getHalo(\edit).value;
				} {
					if(player[\edit].notNil) {
						player.edit;
					} {
						player.editor.front;
					}
				}
			},

			redefineClip: { arg me, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = "{ PlayerWrapper(TrackDef(%)) }".format(curname).interpret;
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
			},

		);

		// generic timeline factory
		tl = (
			key: 'generictimeline',
			label: { arg self; self.key.asString },

			className: {
				~noteTimeline
			},

			keylist: { arg self;
				self.className.all.keys.asArray.sort;
			},

			getName: { arg self, model;
				model.timeline.key;
			},

			editSelected: { arg self, model;
				model.timeline.edit;
			},

			redefineClip: { arg me, curname, model;
				var tlobj;
				Log(\Param).debug("redefineClip timeline before ev:% %", model, model.class);
				PatternEvent.redefine(model);
				Log(\Param).debug("redefineClip timeline ev:% %", model, model.class);

				tlobj = ( "{ " ++ me.className.new(curname).refCompileString ++ " }" ).interpret;
				model[\timeline] = tlobj;
				//model[\timeline] = { 
				//	me.keys.debug("redefineClip3:me.new!!!!!");
				//	me.className.new(curname)
				//};
				//"redefineClip3".debug;
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
				//"redefineClip4".debug;
			},
		);
		self.clipFactories_timeline_parent = tl;

		[\noteTimeline, \clipTimeline, \envTimeline, \modStepTimeline, \kitTimeline].do { arg key;
			self.clipFactories[key] = (
				key: key,
				parent: tl,
				className: "{ topEnvironment[%] }".format(key.asCompileString).interpret,
			);
		};

		self.clipFactories.sampleTimeline = (
			key: 'sampleTimeline',
			parent: tl,
			className: { SampleTimeline.proto },
		);

		self.clipFactories_keys = self.clipFactories.keys.asArray.sort;
		
	},

	event_to_clipFactory_key: { arg self, model;
		var clipkey;

		// 'Pdef',
		// 'Ndef',
		// 'ClipTimeline',
		// 'SampleTimeline',
		// 'NoteTimeline',
		// 'EnvTimeline',
		// 'ModStepTimeline',
		// 'Tracks',

		switch(model.type, 
			\pattern, {
				if(model.timeline.notNil) {
					if(self.clipFactories_keys.includes(model.timeline.eventType)) {
						clipkey = model.timeline.eventType
					};
					//switch(model.timeline.eventType,
						//\envTimeline, {
							//clipkey = 'envTimeline'
						//},
						//\clipTimeline, {
							//clipkey = 'clipTimeline'
						//},
						//\sampleTimeline, {
							//clipkey = 'sampleTimeline'
						//},
						//\noteTimeline, {
							//clipkey = 'noteTimeline'
						//},
						//\kitTimeline, {
							//clipkey = 'kitTimeline'
						//},
						//\modStepTimeline, {
							//clipkey = 'modStepTimeline'
						//},
					//)
				} {
					//var tar = model.receiver.value;
					//if(tar.isKindOf(PlayerWrapper)) {
					//	tar = tar.target;
					//};
					var tar = model.pattern;
					if(tar.isKindOf(Pdef)) {
						clipkey = 'pdef'
					} {
						//model.debug("event_to_clipFactory_key: pattern: unknown");
					}
				}
			},
			\player, {
				var branch = { arg obj;
					switch( obj.class,
						PlayerWrapper, {
							branch.(obj.target)
						},
						Ndef, {
							'ndef'
						},
						Pdef, {
							'pdef'
						},
						TrackDef, {
							'trackdef'
						},
						{
							//model.debug("event_to_clipFactory_key: player: unknown");
						}
					);
				};
				clipkey = branch.(model[\receiver].value);
			},
			{
				//model.debug("event_to_clipFactory_key: unknown");
				nil
			}
		);
		//clipkey.debug("event_to_clipFactory_key: result");
		clipkey;
	},

	clearEditor: { arg self;
		// when no event selected

		self.type_popup.items = [""];
		self.name_popup.items = [""];
		self.clipLabelField.mapParam(nil);
		self.startOffsetField.mapParam(nil);
		self.sustainField.mapParam(nil);
	},

	makeUpdater: { arg self;
		self.controller.remove;
		self.controller = SimpleController(self.model).put(\refresh, {
			var ev = self.model;
			[self.type_popup, ev].debug("entering clipEditor updater");
			if(self.type_popup.isNil or: {self.type_popup.isClosed}) {
				self.controller.remove;
			} {
				var clipkey;
				var clipfac;
				self.refreshPopups;
				clipkey = self.event_to_clipFactory_key(ev);
				clipkey.debug("makeUpdater: clipkey");
				self.startOffsetField.mapParam(Param(self.model, Pembed.startOffsetKey, ControlSpec(0,100,\lin)));
				self.clipLabelField.mapParam(Param(Message(self.model), \label));
				self.sustainField.mapParam(Param(self.model, Pembed.sustainKey));
				if(clipkey.notNil) {
					clipkey.debug("makeUpdater: clipkey not nil");
					clipfac = self.clipFactories[clipkey];
					self.type_popup.value = self.clipFactories_keys.detectIndex({ arg x; x == clipkey });
					self.name_popup.items = clipfac.keylist;
					self.name_popup.value = self.name_popup.items.detectIndex({ arg x; 
						x == clipfac.getName(ev)
					});

					// FIXME: should use parent_model startOffsetKey to be flexible
				} {
					// selected event not recognized

					self.type_popup.value = nil;
					self.name_popup.value = nil;
				}
			}
		});
	},

	refreshPopups: { arg self;
		var name_popup = self.name_popup;
		self.type_popup.items = self.clipFactories_keys.collect({ arg x; self.clipFactories[x].label });
		self.type_popup.action = {
			name_popup.items = self.clipFactories[ self.clipFactories_keys[self.type_popup.value] ].keylist;
		};
		self.type_popup.valueAction = 0;
	},

	edit_selected: { arg self, model;
		var clipkey, clipfac;
		model = model ? self.model;
		//[model.type, model].debug("Edit: type");
		clipkey = self.event_to_clipFactory_key(model);
		clipfac = self.clipFactories[clipkey];
		clipfac.editSelected(model);
	},

	makeLayout: { arg self;
		var layout;
		var header;
		var editor;
		var type_popup, name_popup;
		var model = self.model;
		//"clipEditor.makeLayout: init".debug;
		type_popup = PopUpMenu.new;
		name_popup = PopUpMenu.new;
		self.name_popup = name_popup;
		self.type_popup = type_popup;
		name_popup.allowsReselection = true;
		//"clipEditor.makeLayout: part 1".debug;
		//type_popup.allowsReselection = true; // lose selection
		//name_popup.items = type_popup.items[type_popup.value].asClass.all.keys.asArray;
		name_popup.action = {
			var curname = name_popup.items[name_popup.value];
			var model = self.model;
			self.clipFactories[self.clipFactories_keys[type_popup.value]].redefineClip(curname, model);
		};

		self.refreshPopups;
		//"clipEditor.makeLayout: part 2".debug;
		self.clipLabelField = TextField.new.fixedWidth_(60);
		self.startOffsetField = TextField.new.fixedWidth_(60);
		self.sustainField = TextField.new.fixedWidth_(60);

		header = HLayout(
			[type_popup, stretch:2],
			[name_popup, stretch:2],
			XSimpleButton.new.label_("Edit").action_({
				self.edit_selected;
			}),
			[HLayout(
				StaticText.new.string_("offset:"),
				self.startOffsetField,
			), stretch:0],
			[HLayout(
				StaticText.new.string_("sustain:"),
				self.sustainField,
			), stretch:0],
			[HLayout(
				StaticText.new.string_("name:"),
				self.clipLabelField,
			), stretch:0],

		);
		layout = VLayout(
			header,
			editor,
		);

		//"clipEditor.makeLayout: part 3".debug;
		self.layout = layout;
		self.layout;
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;

		self.makeUpdater;
		self.model.changed(\refresh);
	},

);

~trackEditor = ProtoClass((
	new: { arg self, model, parent, trackgroup;
		self = self.deepCopy;
	
		self.model = model;
		self.parent_model = {parent};
		self.trackgroup = { trackgroup };
		~clipEditor[\init_default_clip_factories].(self);
		
		self;
	},
	
	mapModel: { arg self, model, parent, trackgroup;
		self.model = model;
		self.parent_model = { parent };
		self.trackgroup = { trackgroup };
		self.makeUpdater;
		self.model.changed(\refresh);
	},

	makeUpdater: { arg self;
		self.controller.remove;
		self.controller = SimpleController(self.model).put(\refresh, {
			var ev = self.model;
			//[self.type_popup, ev].debug("entering clipEditor updater");
			if(self.type_popup.isNil or: {self.type_popup.isClosed}) {
				self.controller.remove;
			} {
				var clipkey;
				var clipfac;
				self.refreshPopups;
			}
		});
	},

	model_to_type_index: { arg self;
		var track;
		if(self.model.notNil and: { self.model.receiver.notNil }) {
			track = self.model.receiver.value.target.parentTrack;
			self.trackgroup.detectIndex({ arg x; x.target === track  });
		}
	},

	model_to_name_index: { arg self;
		if(self.model.notNil and: { self.model.receiver.notNil }) {
			self.model.receiver.value.target.index;
		}
	},

	redefineSelected: { arg self, model;
		var view = self.name_popup;
		var track = self.trackgroup[self.type_popup.value].target;
		var idx = view.value;
		model = model ? self.model;
		PlayerEvent.redefine(model);
		model[\receiver] = { PlayerWrapper(track.childAt(idx)) };
		self.parent_model.changed(\refresh);
	},

	refreshPopups: { arg self;
		var name_popup = self.name_popup;
		//"refreshPopups".debug;
		self.type_popup.items = self.trackgroup.asArray.collect(_.label);
		self.type_popup.action = {
			//"type_popup.action".debug;
			if(self.type_popup.value.notNil) {
				name_popup.items = self.trackgroup[self.type_popup.value ].target.children.asArray.collect(_.label);
			}
		};
		self.name_popup.action = { arg view;
			//"name_popup.action".debug;
			if(view.value.notNil and: {self.model.notNil and: {self.type_popup.value.notNil}}) {
				self.redefineSelected;
			}
		};
		self.type_popup.valueAction = self.model_to_type_index;
		self.name_popup.value = self.model_to_name_index;
	},

	edit_selected: { arg self, model;
		model = model ? self.model;
		//[model.type, model].debug("Edit: type");
		self.model.receiver.value.target.edit;
	},

	makeLayout: { arg self;
		var layout;
		var header;
		var editor;
		var type_popup, name_popup;
		var model = self.model;
		//"clipEditor.makeLayout: init".debug;
		type_popup = PopUpMenu.new;
		name_popup = PopUpMenu.new;
		self.name_popup = name_popup;
		self.type_popup = type_popup;
		name_popup.allowsReselection = true;
		//"clipEditor.makeLayout: part 1".debug;
		//type_popup.allowsReselection = true; // lose selection
		self.refreshPopups;

		header = HLayout(
			type_popup,
			name_popup,
			BasicButton.new.label_("Edit").action_({
				self.edit_selected;
			})
		);
		layout = VLayout(
			header,
			editor,
		);

		//"clipEditor.makeLayout: part 3".debug;
		self.layout = layout;
		self.layout;
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;

		self.makeUpdater;
		self.model.changed(\refresh);
	},

	asView: { arg self;
		self.makeLayout;
	},
));

//////////////////////////////// Timeline Panel Views

/////// base timeline panel view

~timelinePanelView = (
	new: { arg self, eventloop;
		//"timelinePanelView".debug("init");
		self = self.deepCopy;

		self.eventloop = eventloop;
		self.areasize = 10@1;
		self.quant_ref = Ref(Point(1/8,1));

		//self.definePattern;

		//"timelinePanelView".debug("end init");
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
			self.cursor.startPosition = nil;
			self.cursor.endPosition = nil;
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

	make_saveas_button: { arg self;
		BasicButton.new
			.action_({ arg me;
				if(self.timeline_model.presetCompileStringSavePath.notNil) {
					var dialog;
					dialog = Window.new("Save").layout_(
						VLayout(
							StaticText.new.string_("Save this object to the file % ?".format(self.timeline_model.presetCompileStringSavePath.asCompileString)),
							HLayout(
								nil,
								BasicButton.new.string_("OK").action_({ 
									self.timeline_model.savePresetCompileString;
									me.refreshChange;
									dialog.close;
								}),
								BasicButton.new.string_("Choose").action_({ 
									self.timeline_model.savePresetCompileString(nil, {
										me.refreshChange;
									}, false);
									dialog.close;
								}),
								BasicButton.new.string_("Cancel").action_({
									dialog.close;
								}),
							)
						)
					).setInnerExtent(600,140).front;
				} {
					self.timeline_model.savePresetCompileString(nil, {
						me.refreshChange;
					}, false);
				};
			})
			.mouseUpAction_({ arg me, x, y, mod, but;
				//[a,b,c,but].debug("but");
				if(but == 1) { // right click
					self.timeline_model.savePresetCompileString(nil, {
						me.refreshChange;
					}, true)
				}
			})
			.refreshChangeAction_({ arg me;
				if(self.timeline_model.presetCompileStringSavePath.notNil) {
					me.string = "Save"
				} {
					me.string = "Save as"
				};
			})
			.refreshChange
		;
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

	//definePattern: { arg self;
	//	// FIXME: why this is in a view class ??
	//	Pdef(self.eventloop.key, Plazy({
	//		//PtimeGatePunch(self.eventloop.list.asPattern.trace(prefix:"xxxxxxxxxxxxxxxxxxxxx"))
	//		var start, end;
	//		var eventlist = self.eventloop.list;
	//		if(self.cursor.notNil) {
	//			start = self.cursor.startPosition - eventlist.startTime;
	//			if(self.cursor.endPosition.notNil) {
	//				end = self.cursor.endPosition - eventlist.endTime;
	//			}
	//		};
	//		~timeline_pattern.(
	//			~eventlist_aspattern.(
	//				self.eventloop.list
	//			).trace(prefix:"xxxxxxxxxxxxxxxxxxxxx"),
	//			start, end
	//		);
	//		//self.eventloop.list.asPattern.trace(prefix:"xxxxxxxxxxxxxxxxxxxxx")
	//	}));
	//},

	makeWindow: { arg self;
		//var window = Window.new;
		WindowDef("%_%".format(self.timeline_model.eventType, self.key).asSymbol, { arg me;
			var layout;
			me.window.onClose_({
				self.save_timeline_properties;
			});
			layout = self.makeLayout;
			//window.layout = layout;
			//window.alwaysOnTop = true;
			//window.front;	
			layout
		}).front
	},

	add_track: { arg self;
		self.areasize = Point(self.areasize.x, self.areasize.y+1 );
		self.timeline.refreshEventList;
		self.timeline.refresh;
		self.update_track_header;
	},

	zoom_fit: { arg self;
		//[self.areasize, self.timeline.areasize].debug("1");
		~zoom_fit.(self.timeline);
		//[self.areasize, self.timeline.areasize].debug("2");
		self.areasize = self.timeline.areasize;
		//[self.areasize, self.timeline.areasize].debug("4");
	},

	update_track_header: { arg self;
		var track_count = self.areasize.y.asInteger;
		var track_header_block = { arg x;
			var res = View.new.layout_(
				HLayout(
					//XSimpleButton.new.label_("track %".format(x)).
					self.track_selector.views[track_count - 1 - x]
				)
			);
			//res.background_(Color.blue);
			res;
		};
		self.track_selector = ~view_selector.new(track_count, {}, { arg view, x; 
			//x = track_count - x;
			if(self.timeline_model.is_track_bypassed(x)) {
				view.label_("[track %]".format(x));
			} {
				view.label_("track %".format(x));
			}
		});
		self.track_header.canvas_(
			self.track_header_view = View.new.layout_(
				VLayout(*
					track_count.collect { arg x;
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
			//"update_track_header_size: viewport".debug;
			//TODO: remove controller
			self.update_track_header_size;
		})
		.put(\areasize, {
			//"update_track_header_size: areasize".debug;
			self.update_track_header_size;
			//self.update_track_header;
		})
		;
	},

	update_track_header_size: { arg self;
		//self.vrange.view.doAction;
		if(self.vrange.notNil) {
			self.vrange_track_header_action(self.vrange.view)
		} {
			if(self.timeline.notNil) {
				//"update_track_header_size: real updating now!".debug;
				//[self.timeline.bounds.height, self.timeline.viewport.height].debug("bh, vh");
				self.track_header_view.fixedHeight = self.timeline.bounds.height / self.timeline.viewport.height;
				self.track_header.visibleOrigin = Point(0,(1-self.timeline.viewport.bottom) * self.track_header.bounds.height);
				//self.timeline.viewport.top.debug("top");
				//[self.timeline.viewport, self.timeline.viewport.bottom, self.track_header.bounds.height, (1-self.timeline.viewport.bottom) * self.track_header.bounds.height].debug("[self.timeline.viewport, self.timeline.viewport.bottom, self.track_header.bounds.height, (1-self.timeline.viewport.bottom) * self.track_header.bounds.height]");
				//self.track_header.visibleOrigin.debug("update_track_header_size: visibleOrigin");
			}
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
		track_header.keyDownAction = self.getTimelineKeyDownAction;
		track_header.hasBorder = false;
		track_header.hasVerticalScroller = false;
		track_header.hasHorizontalScroller = false;
		self.track_header = track_header;

		self.vrange_track_header_action = { arg self, slider;
			self.track_header_view.fixedHeight = 1/slider.range * track_header.bounds.height;
			//( 1/slider.range * track_header.bounds.height ).debug("vrange.view.action: fixedHeight");
			track_header.visibleOrigin = Point(0, ( 1-slider.hi ) / slider.range) * track_header.bounds.height;
			//( Point(0, ( 1-slider.hi ) / slider.range) * track_header.bounds.height ).debug("vrange.view.action: visibleOrigin");
		};

		self.vrange.view.addAction({ arg slider;
			self.vrange_track_header_action.(slider)
		});

		self.update_track_header;

		//self.vrange.view.activeLo = self.vrange.view.lo; // trigger action

		self.track_header;
	},

	makeTimelineView: { arg self;
		self.timelineClass.new
	},

	eventLoop: { arg self;
		self.timeline_model.eventLoop;
	},

	eventLoop_: { arg self, val;
		self.timeline_model.eventLoop = val;
		self;
	},

	eventList: { arg self;
		self.timeline_model.eventList;
	},

	eventList_: { arg self, val;
		self.timeline_model.eventList = val;
		self;
	},

	getTimelineKeyDownAction: { arg self;
		//Window.keyDownActionTest
		({ arg me, key, modifiers, unicode, keycode;
			[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction copypaste");
			self.getTimelineKeyDownActionForNodeMove.(me, key, modifiers, unicode, keycode);
			if(modifiers.isCtrl) {
				//if(key == $n) { // ^n // don't work anymore
				if(keycode == $n.ascii) { // ^n
					"new!".debug;
					self.eventLoop.pushList;
					self.eventList = self.timeline_model.makeDefaultEventList;
					self.eventLoop.pushList;
				};
				if(keycode == $k.ascii) { // ^k
					"clone!".debug;
					self.eventLoop.pushList;
					self.eventList = self.eventLoop.lists.first.clone;
					self.eventLoop.pushList;
				};
				if(keycode == $d.ascii) { // ^d
					"double!".debug;
					self.eventLoop.pushList;
					self.eventList = self.eventLoop.lists.first.clone.double;
					self.eventLoop.pushList;
				};
				if(keycode == $c.ascii) { // ^c
					var selnodes;
					var cursor = self.timeline_model.cursor;
					var startpos, endpos;
					"copy!".debug;
					selnodes = self.timeline.selNodes.as(Array).collect({ arg x; x.model });
					if(selnodes.size == 0) {
						startpos = cursor.startPosition ? self.eventList.startTime;
						endpos = cursor.endPosition ? self.eventList.endTime;
						self.timeline.nodes.collect({ arg node;
							var at = node.model[\absTime];
							if(at >= startpos and: { at < endpos and: { node.selectable }} ) {
								self.timeline.selectNode(node)
							}
						});
						selnodes = self.timeline.selNodes.as(Array).collect({ arg x; x.model });
					};
					if(selnodes.size > 0) {
						~clipTimeline_clipboard = selnodes;
					};
				};
				if(keycode == $v.ascii) { // ^v
					"paste!".debug;
					if(~clipTimeline_clipboard.notNil) {
						var delta;
						var first = ~clipTimeline_clipboard.as(Array).inject(nil, { arg a, b; 
							if(a.isNil) {
								b
							} {
								if(a[\absTime] < b[\absTime]) {
									a
								} {
									b
								}
							};
						});
						//first.debug("first");
						delta = self.timeline.lastGridPos.x.round(self.timeline.quant.value.x) - first[\absTime];
						//~clipTimeline_clipboard.debug("clipboard");
						~clipTimeline_clipboard.do({ arg event;
							var new;
							new = event.copy;
							//[new, delta].debug("new, delta");
							new[\absTime] = new[\absTime] + delta;
							self.timeline.model.addEvent(new);
						});
						//self.timeline.model.changed(\refresh);
						self.timeline.model.reorder;
					}
				};
				if(keycode == $s.ascii) { // ^s
					"split!".debug;
					self.splitAction;
				};
				if(keycode == $ .ascii) { // ^s
					"stop!".debug;
					self.timeline_model.stopNow;
				}
			} {
				// no modifier

				if(keycode == $ .ascii) { // ^s
					"play/stop now!".debug;
					if(self.timeline_model.selection_cursor.notNil) {
						self.timeline_model.cursor.setPositions(
							self.timeline_model.selection_cursor.startPosition ? 0,
							self.timeline_model.selection_cursor.endPosition
						);
					} {
						Log(\Param).debug("no selection cursor");
					};
					self.timeline_model.playNow(1);
				};
				if(keycode == 65481) { // F12
					"Panic!".debug;
					CmdPeriod.run;
				};
				if(keycode == 65474) { // F5
					"play!".debug;
					self.timeline_model.play;
				}
			}
		});
	},

	getTimelineKeyDownActionForNodeMove: { arg self;
		var reorderButKeepSelection = {
			// FIXME: this is ugly, should integrate in TimelineView with a clean algo
			var selmodels = IdentitySet.new;
			var selnodes = List.new;
			self.timeline.selNodes.do { arg node;
				selmodels.add(node.model);
			};
			self.timeline.model.reorder;
			self.timeline.model.changed(\refresh);
			self.timeline.nodes.do { arg node;
				if(selmodels.matchItem(node.model)) {
					selnodes.add(node)
				}
			};
			self.timeline.selectNodes(selnodes);
		};
		var handleMove = { arg distance, keycode;
			if(keycode == 65364) { // down key
				self.timeline.selNodes.do{ arg node;
					node.model[node.posyKey] = node.model[node.posyKey]-distance.y;
					node.model.changed(\refresh);
				}
			};
			if(keycode == 65362) { // up key
				self.timeline.selNodes.do{ arg node;
					node.model[node.posyKey] = node.model[node.posyKey]+distance.y;
					node.model.changed(\refresh);
				}
			};
			if(keycode == 65361) { // left key
				self.timeline.selNodes.do{ arg node;
					node.model[node.timeKey] = node.model[node.timeKey]-distance.x;
				};
				reorderButKeepSelection.();
			};
			if(keycode == 65363) { // right key
				self.timeline.selNodes.do{ arg node;
					node.model[node.timeKey] = node.model[node.timeKey]+distance.x;
				};
				reorderButKeepSelection.();
			};
		};
		({ arg me, key, modifiers, unicode, keycode;
			//[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction copypaste");
			if(modifiers.isCtrl) {
				handleMove.(
					Point(
						self.quant_ref.value.x * self.node_move_multiplier_x,
						self.quant_ref.value.y * self.node_move_multiplier_y,
					),
					keycode,
				)
			} {
				// no modifier
				handleMove.(self.quant_ref.value, keycode)
			}
		});

	},

	node_move_multiplier_x: 8,
	node_move_multiplier_y: 8,

	timelineInstrumentParamGroup: { arg self;
		var target;
		if(self.param.isNil) {
			ParamGroup()
		} {
			target = self.param.target;
			target.asParamGroup
				.add( Param(target, \velocity, ControlSpec(0,127,\lin,0,64)) )
				.add( Param(target, \sustain) )
				.reject({ arg x; x.property == \legato })
			;
		}
	},

	makeVelocityView: { arg self;
		var keyspec;
		var height = 150;
		self.velocityTimeline = VelocityTimelineView.new;
		self.velocityTimeline.fixedHeight = height;
		self.velocityTimeline.mimicTimeline(self.timeline, \horizontal);
		self.velocityTimeline.mimicNodeSelection(self.timeline);
		//self.velocityTimeline.mapParam(Param(self.param.target, \velocity, ControlSpec(0,127,\lin,0,64)));
		self.velocityTimeline.quant = Point(0,0);
		self.velocityTimeline.forbidHorizontalNodeMove = true;

		self.velocityVerticalRange = TimelineScroller.new;
		self.velocityVerticalRange.orientation = \vertical;
		self.velocityVerticalRange.fixedHeight = height;
		self.velocityVerticalRange.mapTimeline(self.velocityTimeline);

		self.velocityValue = StaticText.new;
		self.velocityValue.fixedWidth = 50;
		self.velocityValue.font = Font.default.size_(10);
		//self.velocityValue.string = "XXX";

		self.refreshPosyValue = {
			var node;
			//"---------------heyyyyyyyyyyyyyyy".debug;
			//"heyyyyyyyyyyyyyyy".debug;
			//"heyyyyyyyyyyyyyyy".debug;
			node = self.velocityTimeline.chosennode;
			if(node.notNil and: { node.model.notNil }) {
				var val =   node.model[node.posyKey];
				if(val.notNil) {
					val = val.round(0.001)
				};
				self.velocityValue.string = val;
			}

		};
		self.velocityValue.onChange(self.velocityTimeline, \nodeMoved, {
			self.refreshPosyValue;
		});
		self.velocityValue.onChange(self.velocityTimeline, \selectedNodes, {
			self.refreshPosyValue;
		});

		self.velocityTimeline.visible = false; // not visible by default, FIXME: should be remembered
		self.velocityVerticalRange.visible = false;
		self.velocityShowButton = BasicButton.new.string_(">")
			.fixedWidth_(20)
			.action_({ arg me;
				if(self.velocityTimeline.visible) {
					me.string = ">";
					self.velocityTimeline.visible = false;
					self.velocityVerticalRange.visible = false;
				} {
					me.string = "^";
					self.velocityTimeline.visible = true;
					self.velocityVerticalRange.visible = true;
				}
			})
		;
		
		self.velocityPopUp = PopUpMenu.new;
		self.velocityPopUp.action_({ arg me;
			//"velocityPopUp.action".debug;
			//"velocityPopUp.actio3n".postln;
			self.velocityTimeline.mapParam(self.timelineInstrumentParamGroup[me.value]);
			//self.velocityTimeline.param.debug("new param!!!");
		}).items_(
			self.timelineInstrumentParamGroup.collect({ arg x; 
				x.property;
			})
		);
		if(self.timelineInstrumentParamGroup.size>0) {
			self.velocityPopUp.valueAction = 0;
		};
		//self.velocityPopUp.mapParam(Param(Message(self.velocityTimeline), \valueKey, keyspec));
	},

	makeXRuler: { arg self;
		var cursor_timeline;
		var locatorbar;
		var ruler_x_beat;
		cursor_timeline = CursorTimelineView.new;
		~cursor_timeline = cursor_timeline;
		self.cursor_timeline = cursor_timeline;
		cursor_timeline.mapCursor(self.cursor);
		cursor_timeline.quant = self.quant_ref;


		ruler_x_beat = TimelineRulerView.new;
		ruler_x_beat.mapCursor(self.cursor);
		ruler_x_beat.quant = self.quant_ref;
		self.ruler_x_beat = ruler_x_beat;
		locatorbar = TimelineLocatorBarView.new;
		self.locatorbar = locatorbar;

		locatorbar.quant = self.quant_ref;

		ruler_x_beat.mimicTimeline(self.timeline);
		locatorbar.mimicTimeline(self.timeline);
		cursor_timeline.mimicTimeline(self.timeline);

		self.locatorbar.view.minSize = Point(10,15);
		self.ruler_x_beat.view.minSize = Point(10,10);
		
		VLayout(
			locatorbar.view,
			self.ruler_x_beat.view,
		);
	},

	makeLayout: { arg self;
		var layout;
		var tl_layout;
		var vlayout, timeline, vrange, hrange, size;
		var eventloop = self.eventloop;
		var ruler_y, sizer;
		var sizer_action;
		var track_header;
		var track_header_view;
		var quant_popup;
		//"AZE".debug;
		layout = HLayout.new;
		self.layout = layout;
		vlayout = VLayout.new;
		layout.add(vlayout);

		self.save_timeline_properties;
		timeline = self.makeTimelineView;
		self.timeline = timeline;
		self.load_timeline_properties;
		//timeline.areasize = self.pr_areasize.debug("==================seting areasize");
		~mytl = timeline;
		timeline.mouseDownAction = {|me, px, py, mod, buttonNumber, clickCount, chosennode|
			if(clickCount == 2) {
				if(chosennode.notNil) {
					self.clipEditorNew(chosennode.model, self.eventloop.list)
				};
			}
		};

		timeline.quant = self.quant_ref;
		timeline.mapSelectionCursor(self.selection_cursor);

		self.makeVelocityView;

		//self.cursor = CursorTimeline.new;
		//self.cursor.startPosition = self.eventloop.list.startTime;
		self.ruler_x = self.makeXRuler;


		hrange = TimelineScroller.new;
		hrange.orientation = \horizontal;
		hrange.mapTimeline(timeline);
		vrange = TimelineScroller.new;
		vrange.orientation = \vertical;
		vrange.mapTimeline(timeline);
		self.vrange = vrange;

		sizer = 
		HLayout(
			XSimpleButton.new.label_("<").action_({
				var point = Point(2,1);
				//self.areasize = self.areasize - point;
				self.areasize = self.areasize / point;
				timeline.refresh;
			}).fixedSize_(Point(10,20)),
			XSimpleButton.new.label_(">").action_({
				var point = Point(2,1);
				self.areasize = self.areasize * point;
				timeline.refresh;
			}).fixedSize_(Point(10,20)),
		).spacing_(0)
		;


		// FIXME: created even if disabled, sometime not what we want
		track_header = self.make_track_header(timeline);


		tl_layout = GridLayout.rows(*
			[
				[
					nil,
					self.ruler_x,
					XSimpleButton.new.label_("z")
						.maxWidth_(10)
						.action_({
							self.zoom_fit;
						})
				],
				[
					if(self.disable_track_header != true) {track_header},
					StackLayout(
						self.cursor_timeline.view,
						timeline.selectionView,
						timeline.view,
					).mode_(1),
					vrange.view,
				],
			] 
			++ if(self.velocityTimeline.notNil) {
				[
					[
						nil,
						self.velocityTimeline.view,
						self.velocityVerticalRange.view,
					],
				]
			} { [] }
			++ if(self.disable_hrange != true) {
				[
					[
						nil,
						hrange.view,
						sizer,
					]
				]
			} { [] }  
		);
		tl_layout.spacing = 0;

		layout = VLayout(*
			[
				self.make_header,
				tl_layout,
			] ++ 
			if(self.velocityTimeline.notNil) { // hide if velocityView is disabled
				[
					HLayout(
						self.velocityShowButton,
						self.velocityPopUp,
						//StaticText.new.string_("Velocity: ")
						//	.font_(Font.default.size_(10))
						//,
						self.velocityValue,
						nil,
					)
				]
			} {
				[]
			}
		);

		size = self.areasize;
		//size.debug("size!!!");

		timeline.areasize = size;

		//ruler_y.view.minSize = Point(40,10);

		self.refresh; // map eventlist to timelines

		self.timeline = timeline;
		self.layout = layout;
		//"iiAZE".debug;


		//self.zoom_fit;

		self.timeline.keyDownAction = self.getTimelineKeyDownAction;

		layout;
	},

	asPattern: { arg self;
		// TODO
		Pfunc({})
	},

	refresh: { arg self;
		// event list has changed, refresh the mappings
		self.timeline.mapEventList(self.eventloop.list);
		self.cursor_timeline.mapEventList(self.eventloop.list);
		self.locatorbar.mapEventList(self.eventloop.list);
		if(self.velocityTimeline.notNil) {
			self.velocityTimeline.mapEventList(self.eventloop.list);
		}
	},

	save_timeline_properties: { arg self;
		var tl = self;
		if(tl.timeline.notNil) {
			tl.timeline_model.addHalo(\viewport, tl.timeline.viewport);
			tl.timeline_model.addHalo(\areasize, tl.timeline.areasize);
		}
	},

	load_timeline_properties: { arg self;
		var tl = self;
		var cur;
		cur = tl.timeline_model.getHalo(\viewport);
		if(cur.notNil) {
			tl.timeline.viewport = cur;
		};
		cur = tl.timeline_model.getHalo(\areasize) ? self.pr_areasize;
		if(cur.notNil) {
			tl.timeline.areasize = cur;
		};
	},
);

/////// derivatives

~clipTimelinePanelView = (
	parent: ~timelinePanelView,

	new: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = { timeline };
		self.areasize = self.timeline_model.areasize;
		
		self.makeWindow;
	
		self;
	},

	newView: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = {timeline};
		self.areasize = self.timeline_model.areasize;
		
		//self.makeWindow;
	
		self;
	},

	cursor: { arg self;
		self.timeline_model.cursor;
	},

	selection_cursor: { arg self;
		self.timeline_model.selection_cursor;
	},

	key: { arg self;
		self.timeline_model.key;
	},

	param: { arg self;
		self.timeline_model.param;
	},


	make_header: { arg self;
		var mute_button = Button.new.states_([
			[ "Mute", Color.black, Color.white ],
			[ "Unmute", Color.black, ParamViewToolBox.color_ligth ],
		]).onChange(self.track_selector, \selected_index, { arg view;
			view.value = if(self.timeline_model.is_track_bypassed(self.track_selector.selected_index)) {
				1
			} {
				0
			}
		}).action_({
			self.timeline_model.toggle_bypass_track(self.track_selector.selected_index);
			self.track_selector.viewInit.(self.track_selector.selected_view, self.track_selector.selected_index);
		});

		Log(\Param).debug("recorder %", self.timeline_model.recorder);
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			self.make_saveas_button,
			XSimpleButton.new.label_("Add track").action_({
				self.add_track;
			}),
			mute_button,
			self.make_quant_popup,
			NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			Param(Message(self.timeline_model.recorder), \isRecording, XBoolSpec()).asButton("Rec"),
		);
		
		self.header_layout;
	},

	timelineClass: { arg self;
		PdefTimelineView
	},

	clipEditorNew: { arg self, model;
		~clipEditor.new(model ? self.model, self.timeline_model.eventList);
	},

	makeLayout: { arg self;
		var clipEditor;
		//"clipTimelinePanelView.makeLayout: start init".debug;

		clipEditor = self.clipEditorNew;
		self.clipEditor = { clipEditor };
		//"clipTimelinePanelView.makeLayout: part 0.1".debug;
		self.clipEditorLayout = self.clipEditor.makeLayout;
		//"clipTimelinePanelView.makeLayout: part 0.2".debug;
		self.clipEditor.clearEditor;
		//"clipTimelinePanelView.makeLayout: part 1".debug;
		self[\makeVelocityView] = {}; // disable velocity view
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
			HLayout (
				self.makeLeftPaneLayout,
				VLayout (
					[~timelinePanelView[\makeLayout].(self), stretch:1],
					HLayout (
						self.makeLeftPaneToggle,
						self.clipEditorLayout,
					)
				)
			),
			//self.parent.makeLayout,
		);
		//"clipTimelinePanelView.makeLayout: part 2".debug;
		self.timeline.followChange(self.timeline_model, \eventFactory, {
			Log(\Param).debug("eventFactory change!!!!");
			self.timeline.eventFactory = self.timeline_model.eventFactory;
		});
		self.timeline.mouseDownAction = { arg me, px, py, mod, buttonNumber, clickCount, chosennode;
			if(clickCount == 2) {
				if(chosennode.notNil) {
					self.clipEditor.edit_selected(chosennode.model);
				}
			} {
				if(chosennode.notNil) {
					self.clipEditor.model = chosennode.model;
					self.clipEditor.makeUpdater;
					chosennode.model.changed(\refresh);
				} {
					self.clipEditor.model = nil;
					self.clipEditor.clearEditor;
				}
			}
		};
		//"clipTimelinePanelView.makeLayout: part 3".debug;
		self.timeline.view.onChange(self.eventloop, \list, { 
			{
				self.refresh;
			}.defer
		});
		//"clipTimelinePanelView.makeLayout: end init".debug;
		self.timeline.keyDownAction = self.getTimelineKeyDownAction;
		self.layout;
	},

	splitAction: { arg self;
		self.timeline.splitAtSelectionEdges;
	},
);

~trackTimelinePanelView = (
	parent: ~clipTimelinePanelView,

	new: { arg self, timeline;
		//"trackTimelinePanelView: start init".debug;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
		//"trackTimelinePanelView: init part1".debug;
	
		self.timeline_model = { timeline };
		//"trackTimelinePanelView: init part2".debug;
		self.areasize = self.timeline_model.areasize;
		//"trackTimelinePanelView: init part3".debug;
		
		self.makeWindow;
	
		//"trackTimelinePanelView: end init";
		self;
	},


	newView: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = {timeline};
		self.areasize = self.timeline_model.areasize;
		
		//self.makeWindow;
	
		self;
	},

	clipEditorNew: { arg self, model;
		//"clipEditorNew".debug;
		~trackEditor.new(model ? self.model, self.timeline_model.eventList, self.timeline_model.tracks);
	},

	update_track_header: { arg self;
		var track_count;
		var track_header_block;
		//"trackTimelinePanelView.update_track_header: init".debug;
		self.areasize.y = self.timeline_model.tracks.size;
		track_count = self.areasize.y.asInteger;
		track_header_block = { arg x;
			var res = View.new.layout_(
				HLayout(
					//XSimpleButton.new.label_("track %".format(x)).
					self.track_selector.views[track_count - 1 - x]
				)
			);
			//res.background_(Color.blue);
			res;
		};
		//"trackTimelinePanelView.update_track_header: part 1".debug;
		self.track_selector = ~view_selector.new(track_count, {}, { arg view, x; 
			//x = track_count - x;
			if(self.timeline_model.is_track_bypassed(x)) {
				view.label_("[%]".format(self.timeline_model.tracks[x].label));
			} {
				view.label_("%".format(self.timeline_model.tracks[x].label));
			}
		});
		//"trackTimelinePanelView.update_track_header: part 2".debug;
		self.track_header.canvas_(
			self.track_header_view = View.new.layout_(
				VLayout(*
					track_count.collect { arg x;
						[
							track_header_block.(x),
							align: \top,
						]
					}
				).spacing_(0).margins_(0)
			);
			self.track_header_view
		).maxWidth_(100);
		//"trackTimelinePanelView.update_track_header: part 3".debug;
		self.update_track_header_size;
		self.track_header.onResize = { self.update_track_header_size };
		if(self.track_header_controller.notNil) {
			self.track_header_controller.remove;
		};
		self.track_header_controller = SimpleController(self.timeline)
		.put(\viewport, {
			//"update_track_header_size: viewport".debug;
			//TODO: remove controller
			self.update_track_header_size;
		})
		.put(\areasize, {
			//"update_track_header_size: areasize".debug;
			self.update_track_header_size;
			//self.update_track_header;
		})
		;
		//"trackTimelinePanelView.update_track_header: part 4".debug;
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

	selection_cursor: { arg self;
		self.timeline_model.selection_cursor;
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
			self.param.asKnob,
			Param(Message(self.timeline_model), \repeatCount, ControlSpec(1,inf,\lin)).asNumberBox,
			self.make_quant_popup,
			PlayerWrapper(Pdef(\metronome)).asView, // TODO: make a good one and define it a init
			NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			Button.new.mapParam(Param(Message(self.timeline_model), \isRecording, XBoolSpec())).states_([
				["Rec", Color.black, Color.white],
				["Rec", Color.black, Color.red],
			]),
		)
	},

	makeLayout: { arg self;
		self[\makeVelocityView] = {}; // disable velocity view
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
		self.timeline.view.onChange(self.eventloop, \list, { 
			self.refresh;
		});
		self.timeline.keyDownAction = self.getTimelineKeyDownAction;
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

	node_move_multiplier_y: 12,

	cursor: { arg self;
		self.timeline_model.cursor;
	},

	selection_cursor: { arg self;
		self.timeline_model.selection_cursor;
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
			self.make_saveas_button,
			Param(Message(self.timeline_model), \repeatCount, ControlSpec(1,inf,\lin)).asNumberBox,
			self.make_quant_popup,
			PlayerWrapper(Pdef(\metronome)).asView, // TODO: make a good one and define it a init
			NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			Button.new.mapParam(Param(Message(self.timeline_model), \isRecording, XBoolSpec())).states_([
				["Rec", Color.black, Color.white],
				["Rec", Color.black, Color.red],
			]),
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
		//"noteTimelinePanelView.makeLayout".debug;
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(EventPlayerWrapper(self.timeline_model)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
		//"noteTimelinePanelView.makeLayout bef".debug;
			self.parent[\makeLayout].(self),
			//self.parent.makeLayout,
		);
		//"noteTimelinePanelView.makeLayout af".debug;
		self.timeline.view.onChange(self.eventloop, \list, { 
			self.refresh;
		});
		// not needed because of default event factory of TimelineView
		//if(self.timeline_model.eventFactory.notNil) {
		//	self.timeline.eventFactory = { arg pos, nodesize;
		//		self.timeline_model.eventFactory.get(
		//			(absTime: pos.x, midinote: pos.y, sustain:nodesize)
		//		)
		//	};
		//}
		self.layout;
	},

);

~kitTimelinePanelView = (
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

	selection_cursor: { arg self;
		self.timeline_model.selection_cursor;
	},

	key: { arg self;
		self.timeline_model.key;
	},

	param: { arg self;
		self.timeline_model.param;
	},

	kit: { arg self;
		self.timeline_model.kit;
	},

	makeTimelineView: { arg self;
		self.timelineClass.new(\kitIndex).viewport_(Rect(0,0,1,1/4))
	},

	make_header: { arg self;
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			self.make_saveas_button,
			Param(Message(self.timeline_model), \repeatCount, ControlSpec(1,inf,\lin)).asNumberBox,
			self.make_quant_popup,
			PlayerWrapper(Pdef(\metronome)).asView, // TODO: make a good one and define it a init
			NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			Button.new.mapParam(Param(Message(self.timeline_model), \isRecording, XBoolSpec())).states_([
				["Rec", Color.black, Color.white],
				["Rec", Color.black, Color.red],
			]),
			BasicButton.new.string_("Edit kit").action_({ self.timeline_model.wrapper.edit }),
		);
		
		self.header_layout;
	},


	timelineClass: { arg self;
		KitTimelineView;
	},

	make_track_header: { arg self, timeline;
		//self.track_header = View.new;
		self.ruler_y = KitTimelineRulerView.new;
		self.track_header = self.ruler_y;
		self.ruler_y.mimicTimeline(timeline);
		self.ruler_y.view.minWidth_(50);
	},

	timelineInstrumentParamGroup: { arg self;
		var target;
		if(self.kit.isNil) {
			ParamGroup()
		} {
			var set, group;
			var kit = self.kit;
			set = Set.new;
			if(kit.notNil) {

				kit.reject(_.isNil).do({ arg pat; 
					pat.asParamGroup.do({ arg param;
						if(param.type == \scalar) {
							Log(\Param).debug("param %", param);
							set.add(param.property)
						}
					})
				});
				target = kit.reject(_.isNil).debug("xxx").first;	
				Log(\Param).debug("target %", target);
			} {
				Log(\Param).error("kit is nil for this timeline %", self.timeline_model);
			};
			group = set.as(List).collect({ arg key;
				Param(target, key)
			});
			ParamGroup(group);
		}
	},


	makeLayout: { arg self;
		//"noteTimelinePanelView.makeLayout".debug;
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(EventPlayerWrapper(self.timeline_model)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
		//"noteTimelinePanelView.makeLayout bef".debug;
			self.parent[\makeLayout].(self),
			//self.parent.makeLayout,
		);
		//"noteTimelinePanelView.makeLayout af".debug;
		self.timeline.view.onChange(self.eventloop, \list, { 
			self.refresh;
		});

		if(self.timeline_model.wrapper.notNil) {
			self.timeline.eventFactory = { arg pos, nodesize;
				(absTime: pos.x, kitIndex: pos.y, sustain:nodesize)
			};
		};
		self.layout;
	},

);
