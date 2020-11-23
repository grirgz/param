
///////////////////////////////////////////////////////////////////// timeline objects


~eventTimeline = ProtoClass((
	proxyQuantIsDefault: true, // the only way to preserve default proxy quant

	prAdd: { arg self, name;
		self.all[name] = self;
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

	basicInit: { arg self;
		self.eventloop = self.makeDefaultEventLoop;
		self.areasize = 10@128;
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.loopMaster = ProtoClassDef(\LoopMaster).new(self);
		self.cursor.loopMaster = self.loopMaster;
		self.cursor.startPosition = self.eventList.startTime;
		self.postChain = EventPatternProxy.new.source_(Pbind());
		self.selection_cursor = CursorTimeline.new;

		// note: this is grid quant, not playing quant
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code
		
	},

	target: { arg self;
		if(self.param.notNil) {
			self.param.target
		}
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

	makeDefaultEventList: { arg self;
		var el = TimelineEventList.new;
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

	outBus: { arg self;
		self.postChain.get(\out)
	},
	
	outBus_: { arg self, val;
		self.postChain.set(\out, val)
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

	tempoClock: { arg self;
		TempoClock.default
	},

	repeatCount: inf,


	asPatternAbs: { arg self ...args;
		self.previewPattern(*args)

		//var sloop = self.loopmaster.get
		//self.xasPattern(relStartTime, totalDur);
	},

	cutPatternFunction: { arg self;
		// this function should be overwritten by subclasses to provide a cutting function to their pattern
		// all other patterns methods will depend on it
		{ arg sloop, offset, dur;
			sloop.cutPattern(
				//self.bypass_pattern( Pn(self.eventList.clone, 1) ), // bypassing in upstream is cpu friendlier but prevent unmuting while already in pattern
				Pn(self.eventList.clone, 1),
				offset,
				dur
			).embedInStream
		}
	},

	asPattern: { arg self, startEventOffset, totalDur, repeatCount, parentEvent, parentTimeline;
		self.loopMaster.getClipLoopPattern(self.cutPatternFunction(parentEvent, parentTimeline), startEventOffset, totalDur, repeatCount ? self.repeatCount);
	},

	previewPattern: { arg self, absStart, absEnd, repeatCount;
		self.loopMaster.getPreviewLoopPattern(self.cutPatternFunction, absStart, absEnd, repeatCount ? self.repeatCount);
	},

	asPatternSlice: { arg self, startLocator, endLocator, repeatCount=1;
		self.loopMaster.getPatternSlice(self.cutPatternFunction, startLocator, endLocator, repeatCount ? self.repeatCount);
	},

	asPatternSlices: { arg self, repeatCount=1;
		self.loopMaster.getPatternSlices(self.cutPatternFunction, repeatCount ? self.repeatCount);
	},

	asSelectedPattern: { arg self, repeatCount;
		self.asPatternAbs({self.cursor.startPosition}, {self.cursor.endPosition}, repeatCount)
	},

	embedInTimeline: { arg self, drop_dur, sustain;
		var relStartTime;
		relStartTime = self.eventList.relStartTime + drop_dur;
		self.xasPattern(relStartTime, self.eventList.totalDur min: sustain);
	},

	embedInStream: { arg self, inval;
		self.asPattern
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

	stop: { arg self;
		self.proxy.stop;
		PlayerWrapper(self.proxy).doWithQuant({
			self.eventloop.list.changed(\cursor, \stop);
		})
	},

	isPlaying: { arg self;
		self.proxy.isPlaying;
	},

	/////////////// backward compat (old way before ProtoClass)

	asPatternAbs_old: { arg self, startPosition, endPosition, repeatCount;
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
				Log(\Param).debug("timeline.asPattern: loop too short, setting it to 2: xx:%", xx);
				2;
			} {
				xx
			}
		};

		//[startPosition, endPosition, relStartTime, totalDur].collect(_.value).debug("asPatternAbs: start, end, relstart, totdur");
		self.xasPattern(relStartTime, totalDur, repeatCount);
	},


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

	xembedInStream: { arg self, inval;
		self.embedInStream(inval);
	},

	///////// control and recording

	startControl: { arg self;
		if(self.controller.notNil) {
			self.controller.startControl(self);
		}
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
			self.startControl;
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
		ret = "TimelineEventList.newFrom([\n%]);".format(ret);
		ret;
	},

	presetCompileStringSaveFile: { arg self;
		if(self.presetCompileStringSavePath.notNil) {
			if( self.presetCompileStringSavePath.isFolder) {
				self.presetCompileStringSavePath +/+ "ClipTimeline_%.scd".format(self.key)
			} {
				self.presetCompileStringSavePath
			}
		}
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
			if(self.presetCompileStringSaveFile.notNil and: { force_dialog==false }) {
				save_it.(self.presetCompileStringSaveFile)
			} {
				if(WindowDef(\filedialog_save).notNil) {
					WindowDef(\filedialog_save).front(
						PathName(Document.current.path).pathOnly +/+ "ClipTimeline_%.scd".format(self.key),
						{ arg dir, filename;
							save_it.(dir+/+filename)
						}
					)
				} {
					Log(\Param).debug("filedialog_save not defined, use old dialog");
					Dialog.savePanel({ arg mypath;
						//path.postln;
						save_it.(mypath)
					},{
						//"cancelled".postln;
					});
				}
			}

		};
	},

	loadPresetCompileString: { arg self;
		if(self.presetCompileStringSavePath.notNil) {
			FileSystemProject.load(self.presetCompileStringSavePath)
		} {
			"%.savePresetCompileString: no file to load: %".format(self.refCompileString, self.presetCompileStringSavePath).error;
		}
	},

	asPreview: { arg self;
		TimelinePreview.new.mapEventList(self.eventList).view.mouseDownAction_({ 
			self.edit;
		}).fixedHeight_(40);
	},

	addHistorySnapshot: { arg self;
		self.eventLoop.pushList;
		self.eventList = self.eventList.clone;
		self.eventLoop.pushList;
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
		self.basicInit;
		self.areasize = 10@4;

		//self.eventFactory = ~class_player_event_factory.new;
		//self.eventFactory.eventLoop = self.eventloop;
		//self.eventFactory.mode = \note;
		//self.eventFactory.playmode = \gate;
		//self.eventFactory.fullrecord = false;

		self.tracks = self.areasize.y.collect({ arg idx; (
			index: idx,
			recordBus: 0,
			recordArmed: false,
			muted: false,
		)});
		self.tracks_bypassed = Set.new;
		self.recorder = ProtoTemplateDef(\SampleClipTimelineMultiTrackRecorder).new(self);

		self.clipList = ProtoClassDef(\ClipTimelineClipList).new(self);
		//"=============clipTimeline: end init".debug;

		self;
	},

	bindPlayWithRecord: true,

	track_count: { arg self;
		self.tracks.size;
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

	add_track: { arg self;
		
		self.tracks = self.tracks.add((
			index: self.tracks.size,
			recordBus: 0,
			recordArmed: false,
			muted: false,
			solo: false,
		));
		self.areasize = Point(self.areasize.x, self.tracks.size ); // areasize is deprecated, use track_count
	},

	tracks_: { arg self, tracks;
		self[\tracks] = tracks;
		self.areasize = Point(self.areasize.x, self.tracks.size );
		if(self.recorder.notNil) {
			self.recorder.updateTrackRecorders;
		};
		self.changed(\tracks);
	},

	//muteTrack: { arg self, index, val=true;
		//self.tracks[index].muted = val;
	//},

	//soloTrack: { arg self, index, val=true;
		//self.tracks[index].solo = val;
	//},

	bypass_track: { arg self, track_index, val=true;
		// used by steptimeline, maybe deprecated
		if(val) {
			self.tracks_bypassed.add(track_index);
		} {
			self.tracks_bypassed.remove(track_index);
		}
	},

	toggle_bypass_track: { arg self, track_index;
		// used by steptimeline, maybe deprecated
		if(self.is_track_bypassed(track_index)) {
			self.bypass_track(track_index, false)
		} {
			self.bypass_track(track_index, true)
		}
	},

	is_track_bypassed: { arg self, track_index;
		// used by steptimeline, maybe deprecated
		//self.tracks[track_index] !? _.muted ? false or: {self.tracks_bypassed.includes(track_index)};
		if(self.soloTracks.notNil) {
			self.soloTracks.includes(track_index).not
		} {
			false
		};
	},

	computeSoloMute: { arg self;
		var trackAssos = self.tracks.collect({ arg track, idx; idx -> track });
		var soloTracks = trackAssos.select({ arg trackasso; trackasso.value.solo == true });
		if(soloTracks.size == 0) {
			soloTracks = trackAssos.select({ arg trackasso; trackasso.value.muted != true })
		};
		self.soloTracks = soloTracks.collect(_.key).asSet;
	},

	mutingPattern: { arg self, parentEvent;
		Pbind(
			\type, Pfunc({ arg ev;
				if(self.is_track_bypassed(parentEvent.use({ ~midinote.value.asInteger }))) {
					\rest;
				} {
					ev[\type];
				}
			})
		)
	},

	bypass_pattern: { arg self, pat;
		Pcollect({ arg ev;
			//"entering pcollect".debug;
			ev = ev.copy;
			if(self.is_track_bypassed(ev.use({ev.midinote.asInteger}))) {
				ev[\type] = \rest;
				ev[\eventType] = nil;
				ev[\nodeType] = nil;
			};
			//"ending pcollect".debug;
			ev;
		}, pat)	
	},

	//asPattern_old: { arg self, relStartTime, totalDur, repeatCount;
		//self.proxy.source = Plazy({
			//var eventlist = self.eventList;
			//var xrelStartTime, xtotalDur;

			//xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			//xtotalDur = totalDur.value ? eventlist.totalDur;

			////[xrelStartTime, xtotalDur].debug("clip xasPattern: plazy: relstart, totdur");
			//if(xtotalDur <= 0) {
				//"ERROR: totalDur is negative or zero".postln;
				//xtotalDur = 2
			//};

			//self.eventloop.list.changed(\cursor, \play); // FIXME: should be an event
			//Pfindur(xtotalDur, Pembed(
				//self.bypass_pattern( Pn(eventlist.clone, 1) ),
				//xrelStartTime
			//));
		//}).repeat(repeatCount ? self.repeatCount);
		//self.proxy;
	//},

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

	keepSamplesInUse: { arg self;
		self.recordedEvents.do({ arg ev;
			var tl;
			Log(\Param).debug("recorded ev: %".format(ev));
			tl = ev.timeline;
			if(self.eventList.any{ arg xev; 
				Log(\Param).debug("eventList ev: %".format(xev));
				xev.timeline.value === tl 
		   	}) {
				Log(\Param).debug("yep i keep: %".format(tl));
				tl.keepRecordedFile;
			}
		})
	},

	samplesPresetCompileString: { arg self;
		var samples = self.eventList.select({ arg ev;
			ev.timeline.notNil and: {
				ev.timeline.eventType == \sampleTimeline
			}
		});
		var str = samples.collect({ arg ev; 
			ev.timeline.key -> ev.timeline.presetCompileString 
		}).asDict.values.join("\n");
		"%\n".format(str);
	},

	presetCompileString: { arg self;
		var str = "";
		if(self.label != self.key) {
			str = str ++ "%.label = %;\n".format(self.refCompileString(self.key.asCompileString), self.label.asCompileString);
		};
		str = str ++ "%.tracks = %;\n".format(self.refCompileString(self.key.asCompileString), self.tracks.asCompileString);
		// enclose in function to workaround the selector table size limit
		str = str ++ "{\n%.eventList = %\n }.value;\n".format(self.refCompileString(self.key.asCompileString), self.eventList.presetCompileString);
		if(self.recordedEvents.notNil) {
			str = str ++ "%.recordedEvents = %;\n".format(self.refCompileString(self.key.asCompileString), self.recordedEvents.asCompileString);
		};
		self.keepSamplesInUse;
		str = str ++ "\n%\n".format(self.samplesPresetCompileString);
		str;
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
	controller: { TagSpecDef(\NoteTimeline_controller).list.asDict[\generic_piano] },

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

		self.basicInit;
		self.param = param; // depends on eventloop

		// TODO: make a recorder and deprecate eventFactory
	},

	trackInstrument_: { arg self, instr;
		Log(\Param).debug("NoteTimeline % trackInstrument_: %", self.key, [instr.hash, instr.target, instr.libId]);
		self[\trackInstrument] = instr;
		self.param = instr.target;
	},

	param_: { arg self, param;
		if(param.isNil) {
			param = Param(Pdef(\NoteTimeline_default, Pbind()), \midinote);
		};
		if(param.isKindOf(Pdef)) {
			param = Param(param, \midinote);
		};
		self[\param] = param;
		self.wrapper = ~seqPlayerGroup.new(self.param.target);
		self.recorder = ~playerGroupRecorder.new(self.wrapper);
		self.recorder.eventLoop = self.eventLoop;

		//self.eventFactory = ~class_event_factory.new(self.param.target);
		//self.eventFactory.eventLoop = self.eventloop;
		//self.eventFactory.mode = \note;
		//self.eventFactory.playmode = \gate;
		//self.eventFactory.fullrecord = false;

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

	cutPatternFunction: { arg self;
		{ arg sloop, offset, dur;
			var inpat;
			var pat;
			inpat = Pn(self.eventList.clone, 1);
			//Log(\Param).debug("now in cutPatternFunction of NoteTimeline, off:%, dur:%", offset, dur);

			if(self.param.notNil and: {self.composeParamPattern == true}) {
				inpat = self.param.target <> inpat;
				//if(self.eventFactory.notNil and: {self.eventFactory.mode == \event}) {
					////"Event mode!".debug;
					//pat = self.param.target <> pat;
				//} {
					////"Normal mode!".debug;
					//pat = pat <> self.param.target;
				//}
			};

			pat = self.postChain <> sloop.cutPattern(
				inpat,
				offset, dur,
			);

			pat.embedInStream;
			//Log(\Param).debug("now in cutPatternFunction after embed");
		}
	},

	//asPattern_old: { arg self, relStartTime, totalDur, repeatCount;
		////[relStartTime.value, totalDur.value].debug("relstart, totaldur");
		//// there is 3 kinds of call to asPattern:
		//// - the gui: currently relative startPosition to absTime_0 : asPatternAbs
		//// - the live code: no startPosition given, defaulting to start event : asPattern
		//// - when embedded in bigger timeline: startPosition is relative to start event : embedInTimeline
		////		if startOffset, relStartTime is adjusted

		//// timeline_pattern take a start time relative to firstEvent
		//// XEventList.relStartTime is relative to firstEvent
		//// FIXME: replacing source in proxy at call time means that a playing proxy will play immediately the new source when asked its pattern
		////		could have nasty side effects
		////self.proxy.source = Plazy({
		//var pat = Plazy({
			//var eventlist = self.eventloop.list;
			//var pat;
			//var xrelStartTime, xtotalDur;

			//xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			//xtotalDur = totalDur.value ? eventlist.totalDur;

			//self.eventloop.list.changed(\cursor, \play);
			////pat = Pfindur(xtotalDur, ~timeline_pattern.(
			////	Pn(self.eventloop.list, 1),
			////	xrelStartTime
			////));

			////[xrelStartTime, relStartTime.value, eventlist.relStartTime].debug("xrel, rel, evrel");
			////[xtotalDur, totalDur.value, eventlist.totalDur].debug("xrel, rel, evrel");

			//if(xtotalDur <= 0) {
				//"ERROR: totalDur is negative or zero".postln;
				//xtotalDur = 2
			//};

			//pat = self.postChain <> Pfindur(xtotalDur, Pembed(
				//Pn(self.eventloop.list.clone, 1),
				//xrelStartTime
			//));

			//if(self.param.notNil and: {self.composeParamPattern == true}) {
				//if(self.eventFactory.notNil and: {self.eventFactory.mode == \event}) {
					////"Event mode!".debug;
					//pat = self.param.target <> pat;
				//} {
					////"Normal mode!".debug;
					//pat = pat <> self.param.target;
				//}
			//};

			//pat;
		//}).repeat(repeatCount ? self.repeatCount);
		////self.proxy;
		//pat;
	//},


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
				// FIXME: should always call newInstance with a bus param or custom bus
				// else everything write to the same bus
				self[\outBus] = BusDef("ParamTimeline_%".format(self.key), \control);
			};
		} {
			self[\outBus] = self.customBus;
		};
	},

	init: { arg self, param, bus;
		self.basicInit;
		self.customBus = bus;

		self.param = param ?? { Param((empty:1), \empty, \unipolar) };

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

	cutPatternFunction: { arg self;
		{ arg sloop, offset, dur;
			var postpat, prepat;
			var pat;
			// FIXME: this should be an event
			if(self.param.notNil and:{ self.param.target.isKindOf(Bus).not }) {
				// FIXME: bus mode should be generic
				if([EventPatternProxy, NodeProxy].any({ arg x; self.param.target.isKindOf(x) })) {
					self.param.target.set(self.param.property, self.outBus.asMap);
					self.param.changed(\inBusMode);
				} {
					self.param.set(self.outBus.asMap);
				};
				//default = self.param.default;
			};

			~remove_envlist_duplicate.(self.eventList);
			~compute_eventenv_sustain.(self.eventList);

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

			pat = sloop.cutPattern(
				Pn(self.eventList.clone, 1), 
				offset, dur,
			);
			pat = postpat <> pat <> prepat;

			//~pattern_to_envpattern.(pat).embedInStream(inval ? ()); 
			~pattern_to_envpattern.(pat).embedInStream;
		}
	},

	//asPattern_old: { arg self, relStartTime, totalDur;
		////[relStartTime, totalDur].debug("xasPattern: relstart, totdur");
		//self.proxy.source = Plazy({ arg inval;
			//var eventlist = self.eventloop.list;
			//var pat;
			//var str;
			//var first;
			//var first_level;
			//var xrelStartTime, xtotalDur;	
			//var postpat, prepat;

			//xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			//xtotalDur = totalDur.value ? eventlist.totalDur;

			////[xrelStartTime, xtotalDur].debug("xasPattern: prout: relstart, totdur");
			//self.eventloop.list.changed(\cursor, \play);

			//if(xtotalDur <= 0) {
				//"ERROR: totalDur is negative or zero".postln;
				//xtotalDur = 2
			//};


			//// FIXME: this should be an event
			//if(self.param.notNil and:{ self.param.target.isKindOf(Bus).not }) {
				//// FIXME: bus mode should be generic
				//if(self.param.target.class == Pdef) {
					//self.param.target.set(self.param.property, self.outBus.asMap)
				//} {
					//self.param.set(self.outBus.asMap);
				//};
				////default = self.param.default;
			//};

			//~remove_envlist_duplicate.(eventlist);
			//~compute_eventenv_sustain.(eventlist);

			//postpat = Pbind(
				//\out, self.outBus,
				////\level, Pfunc({ arg ev; ev[self.param.property] ? default }),
				//\time, Pfunc({ arg ev; ev.use { 
					//// absTime is in beats (but maybe should be in seconds ?)
					//// need to convert to seconds
					////(ev.next_absTime ? ev.absTime) - ev.absTime / thisThread.clock.tempo
					//// use dur instead
					//( ev[\sustain] ? 1 ) / thisThread.clock.tempo
				//} }),
				//\type, Pfunc({  arg ev;

					//if(ev[\type].isNil or: { ev[\type] == \note }) {
						//if(ev[\next_level].isNil) { // last one should be rest
							//\rest
						//} {
							//if(ev[\time].notNil and: {ev[\time] < 0.051}, {
								//ev[\array] = [ ev[\level] ];
								////ev.debug("envTimeline: asPattern: busmode!");
								//\bus;
							//}, { 
								//\note;
							//});
						//}

					//} {
						//ev[\type]
					//}
				//})
			//);
			////postpat = postpat.trace(prefix:"MAIIIIIIIIIIIIIIIIIIIIIIII"); // debug
			//prepat = Pbind(
				//\instrument, \env_segment,
				//\curve, 0,
			//);

			//pat = Pfindur(xtotalDur, Pembed(
				//Pn(self.eventloop.list.clone, 1), 
				//xrelStartTime
			//));
			//pat = postpat <> pat <> prepat;

			////~pattern_to_envpattern.(pat).embedInStream(inval ? ()); 
			//~pattern_to_envpattern.(pat);

		//}).repeat(self.repeatCount);
		//self.proxy;
	//},

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

	init: { arg self, kit;

		self.basicInit;
		self.kit = kit ?? { PatKitDef(\PatKitDef_default) };
		
	},

	trackInstrument_: { arg self, instr;
		Log(\Param).debug("KitTimeline % trackInstrument_: %", self.key, [instr.hash, instr.target, instr.libId]);
		self[\trackInstrument] = instr;
		self.kit = instr.target;
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

	cutPatternFunction: { arg self;
		{ arg sloop, offset, dur;
			var pat;
			pat = sloop.cutPattern(
				Pn(self.eventList.clone, 1), // clone avoid lose of sync when adding note but require to wait the end of loop
				// TODO: schedule added notes on clock to have a virtual real-time refresh
				offset, dur
			);


			if(self.kit.notNil and: {self.composeParamPattern == true}) {
				pat = PdrumStep(self.kit, pat, 1, key: \kitIndex).trace;
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

			pat.embedInStream;
		}
	},


	//asPattern_old: { arg self, relStartTime, totalDur, repeatCount;
		//// there is 3 kinds of call to asPattern:
		//// - the gui: currently relative startPosition to absTime_0 : asPatternAbs
		//// - the live code: no startPosition given, defaulting to start event : asPattern
		//// - when embedded in bigger timeline: startPosition is relative to start event : embedInTimeline

		//// timeline_pattern take a start time relative to firstEvent
		//// XEventList.relStartTime is relative to firstEvent
		////self.proxy.source = Plazy({
		//var pat = Plazy({
			//var eventlist = self.eventloop.list;
			//var pat;
			//var xrelStartTime, xtotalDur;

			//xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			//xtotalDur = totalDur.value ? eventlist.totalDur;

			//self.eventloop.list.changed(\cursor, \play);
			////pat = Pfindur(xtotalDur, ~timeline_pattern.(
			////	Pn(self.eventloop.list, 1),
			////	xrelStartTime
			////));
			//if(xtotalDur <= 0) {
				//"ERROR: totalDur is negative or zero".postln;
				//xtotalDur = 2
			//};


			//pat = Pfindur(xtotalDur, Pembed(
				//Pn(self.eventloop.list.clone, 1), // clone avoid lose of sync when adding note but require to wait the end of loop
				//xrelStartTime
			//));


			//if(self.kit.notNil and: {self.composeParamPattern == true}) {
				//pat = PdrumStep(self.kit, pat, 1, key: \kitIndex);
				//if(self.wrapper.postChain.notNil) {
					////"post!".debug;
					//pat = self.wrapper.postChain <> pat;
				//};
				//if(self.postChain.notNil) {
					////"post!".debug;
					//pat = self.postChain <> pat;
				//};
				//if(self.wrapper.preChain.notNil) {
					////"pre!".debug;
					//pat = pat <> self.wrapper.preChain;
				//};
				//if(self.preChain.notNil) {
					////"pre!".debug;
					//pat = pat <> self.preChain;
				//};
			//};

			////if(self.param.notNil and: {self.composeParamPattern == true}) {
			////	if(self.eventFactory.mode == \event) {
			////		"Event mode!".debug;
			////		pat = self.param.target <> pat;
			////	} {
			////		"Normal mode!".debug;
			////		pat = pat <> self.param.target;
			////	}
			////};

			//pat;
		//}).repeat(repeatCount ? self.repeatCount);
		////self.proxy;
		//pat
	//},

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

/////////////////////// cliplist side panel


ProtoClassDef(\ClipTimelineClipList, (
	parent: ~clipEditor,
	new: { arg self, timeline;
		self = self.deepCopy;
	
		self.init_default_clip_factories;
		self.lib = ProtoDef(\GlobalLibrary);
		self.clipList = List.new;
		self.timeline = timeline;
		self.defineEventFactory;
		self.gatherClips;
		if(self.clipList.size > 0) {
			self.setBrushToClipId(self.clipList.first)
		};
		
		self;
	},

	eventToClipId: { arg self, ev;
		var libId;
		Log(\Param).debug("eventToClipId: ev: %".format(ev));
		if([\rest, \start,\end].includes(ev.type)) {
			nil
		} {
			libId = self.lib.eventToLibId(ev); 
			if(libId.notNil) {
				libId
			} {
				Log(\Param).debug("eventToClipId: no libId: ev:%", ev);
				nil;
			};
		}
	},

	clipIdToEvent: { arg self, clipId;
		var ev = self.lib.libIdToWrapper(clipId).getEvent;
		//[ev, ev[\label]].debug("clipIdToEvent ev");
		ev
	},

	addClipIdToClipList: { arg self, clipId;
		if(clipId.notNil) {
			if(self.clipList.includesEqual(clipId).not) {
				self.clipList.add(clipId);
				if(self.clipList.size == 1) {
					self.setBrushToClipId(self.clipList.first)
				};
			} {
				Log(\Param).debug("already added");
			}
		} {
			Log(\Param).debug("addClipIdToClipList: no clipId %", clipId);
		};
		self.changed(\clipList);
	},

	addAndSelectClipId: { arg self, clipId;
		self.addClipIdToClipList(clipId);
		self.selectClipId(clipId);
	},

	addToClipList: { arg self, val;
		var clipId = self.eventToClipId(val);
		self.addClipIdToClipList(clipId);
	},

	setBrushToClipId: { arg self, clipId;
		self.brushClipId = clipId;
		self.changed(\brushClipId);
	},

	selectClipId: { arg self, clipId;
		self.setBrushToClipId(clipId);
	},

	clipIdToTarget: { arg self, clipId;
		self.lib.libIdToWrapper(clipId).getTarget;
	},

	gatherClips: { arg self;
		//"gat".debug;
		self.timeline.eventList.do { arg clip;
			self.addToClipList(clip)
		}
	},
	
	edit: { arg self;
		WindowDef(\ClipList).front(self);
	},

	clipIdLabel: { arg self, clipId;
		if(clipId.isNil) {
			"nil"
		} {
			self.lib.libIdToLabel(clipId);
		}
	},

	defineEventFactory: { arg self;
		self.timeline.eventFactory = { 
			{ arg pos, nodesize;
				var ev = (absTime: pos.x, midinote: pos.y, legato:1, sustain: nodesize);
				var clipev;
				Log(\Param).debug("ev fac: brush:%, clipev:%", self.brushClipId, clipev);
				if(self.brushClipId.isNil) {
					Log(\Param).debug("No brush selected");
					clipev = ev;
				} {
					clipev = self.clipIdToEvent(self.brushClipId);
					clipev.putAll(ev);
				};
				Log(\Param).debug("ev fac: brush:%, clipev:%", self.brushClipId, clipev);
				clipev;
			}
		};
		self.timeline.changed(\eventFactory);
		
	},
));




ProtoClassDef(\LoopMaster, (
	new: { arg self, timeline;
		self = self.deepCopy;
		
		self.timeline = { timeline };
	
		self;
	},

	new_as_parent: { arg self, timeline;

		(
			parent: self,
		   	new:nil,
			timeline: { timeline },
		);
	},

	cursor: { arg self;
		self.timeline.cursor;
	},

	eventList: { arg self;
		self.timeline.eventList;
	},

	///////////////////////// tool functions

	firstEventAbsTime: { arg self;
		self.eventList.firstTime;
	},

	repeatCount: { arg self;
		self.timeline.repeatCount
	},

	totalDur: { arg self;
		self.eventList.totalDur;
	},

	startEventAbsTime: { arg self;
		self.eventList.startTime;
	},

	endEventAbsTime: { arg self;
		self.eventList.endTime;
	},

	firstEventAbsTime: { arg self;
		self.eventList.firstTime;
	},

	endOfLastEventAbsTime: { arg self;
		//self.eventList.last.absTime + self.eventList.last.use { ~dur.value };
		self.eventList.last.absTime + ( self.eventList.last.use { ~sustain.value } ? 0 );
	},

	previewStartAbsTime: { arg self;
		self.cursor.startPosition ? self.startEventAbsTime;
	},

	previewEndAbsTime: { arg self;
		self.cursor.endPosition ? self.endEventAbsTime;
	},

	hasPreviewEnd: { arg self;
		self.cursor.endPosition.isNil.not;
	},

	//////////////////////////////

	startCursorEvent: { arg self;
		(
			\type: \rest,
			\dur: 0,
			\play: {
				self.eventList.changed(\cursor, \play);
			}
		)
	},

	cutAndEmbedPattern: { arg self, pat, firstEventOffset, dur;
		// deprecated. Now I use embedPattern with a cutFunction as argument
		if(dur < 0.01) {
			dur = 1;
			Log(\Param).debug("prevent too short loop");
		};
		self.currentLoopDur = dur;
		self.currentLoopStartAbsTime = firstEventOffset + self.firstEventAbsTime;
		self.startCursorEvent.yield;
		Pfindur(dur, Pembed(pat.value, firstEventOffset, self.timeline)).embedInStream;
	},

	cutPattern: { arg self, pat, firstEventOffset, dur;
		// do not embedInStream to allow composing it
		if(firstEventOffset < 0) {
			Pfindur(dur, 
				Pseq([
					Event.silent(firstEventOffset.abs),
					Pembed(pat.value, 0, self.timeline)
				],1),
			);
		} {
			Pfindur(dur, 
				Pembed(pat.value, firstEventOffset, self.timeline)
			);
		}
	},

	embedPattern: { arg self, cutFunction, firstEventOffset, dur;
		if(dur < 0.01) {
			dur = 1;
			Log(\Param).debug("prevent too short loop");
		};
		self.currentLoopDur = dur;
		self.currentLoopStartAbsTime = firstEventOffset + self.firstEventAbsTime;
		self.startCursorEvent.yield;
		//Log(\Param).debug("embedPattern: now call cutFunction, off:% dur:%", firstEventOffset, dur);
		cutFunction.(self, firstEventOffset, dur);
	},

	getClipLoopPattern: { arg self, cutFunction, customStartEventOffset, customTotalDur, customRepeatCount;
		//"hello 2".debug;
		// repeat pattern when outside clip loop
		// we want to give time since start event
		// cutFunction is rather a SliceGenerator that take offset and dur and return a slice of the pattern
		// 		it is provided by the timeline because the timeline know better how to cut the pattern
		//		it generally call sloop.cutPattern but can do other things
		//		for example this allow to start inside a sample without having to fast forward events manually
		Prout({
			var sloop = self;
			var repeatCount = customRepeatCount ? sloop.repeatCount;
			repeatCount.do { arg count;
				var firstEventOffset;
				var startEventOffset;
				var totalDur = customTotalDur ? sloop.totalDur;
				var dur;
				// if the event if longer than the eventList, repeat it, but the offset is only for the first repetition
				if(count == 0) {
					startEventOffset = (customStartEventOffset ? 0) % totalDur; // will never exceed the clip loop
				} {
					startEventOffset = 0;
				};
				dur = totalDur - startEventOffset;
				firstEventOffset = startEventOffset + sloop.startEventAbsTime - sloop.firstEventAbsTime;
				//self.cutAndEmbedPattern(pat, firstEventOffset, dur);
				self.embedPattern(cutFunction, firstEventOffset, dur)
			};

		})
	},

	enablePreviewLoopToClipStart: true,

	getPreviewLoopPattern: { arg self, cutFunction, absStart, absEnd, customRepeatCount;
		// read outside notes when outside clip loop
		Prout({
			var sloop = self;
			var repeatCount = customRepeatCount ? sloop.repeatCount;
			repeatCount.do { arg count;
				var previewStartAbsTime;
				var firstEventOffset;
				var previewEndAbsTime;
				var finalStartAbsTime;
				var dur;
				previewStartAbsTime = absStart.value ? sloop.previewStartAbsTime;
				previewEndAbsTime = absEnd.value ? sloop.previewEndAbsTime;

				// if null loop, set end to end of list
				if(previewStartAbsTime >= previewEndAbsTime) {
					previewEndAbsTime = sloop.endOfLastEventAbsTime
				};

				if(count == 0) {
					finalStartAbsTime = previewStartAbsTime;
				} {
					if(sloop.hasPreviewEnd.not and:{ absEnd.value.isNil and: {sloop.enablePreviewLoopToClipStart == true}}) {
						finalStartAbsTime = sloop.startEventAbsTime;
					} {
						finalStartAbsTime = previewStartAbsTime;
					};
				};
				// Pembed need startOffset to be relative to first event, not absTime
				//		so need to substract firstEventAbsTime
				firstEventOffset = finalStartAbsTime - sloop.firstEventAbsTime;
				dur = previewEndAbsTime - finalStartAbsTime;
				//self.cutAndEmbedPattern(pat, firstEventOffset, dur);
				self.embedPattern(cutFunction, firstEventOffset, dur);
			};

		})
		
	},

	getLocatorAbsTime: { arg self, label;
		var locatype;
		var res;
		if(label == \start) {
			res = self.startEventAbsTime;
		} {
			if(label == \end) {
				res = self.endEventAbsTime;
			} {
				if(label.isNumber) {
					var count = 0;
					self.eventList.detect({ arg x, idx; 
						if(self.isLocator(x)) {
							count = count +1;
						};
						( count-1 ) == label
				   	}).absTime
				} {
					self.eventList.detect({ arg x; x.type == \locator and: { x.label == label } }).absTime
				}
			};
		};
	},

	isLocator: { arg self, ev, label;
		if(label.isNil) {
			[\start, \end, \locator].includes(ev.type)
		} {
			if(label == \start) {
				ev.type == \start;
			} {
				if(label == \end) {
					ev.type == \end
				} {
					if(label.isNumber) {
						Log(\Param).debug("this can't work with number");
						nil
					} {
						ev.type == \locator and: { ev.label == label } 
					}
				};
			};
		}
	},

	getNextLocatorAbsTime: { arg self, label;
		var locatype;
		var res;
		var next;
		var curIdx;
		var locators = self.eventList.select({arg x; self.isLocator(x)});
		if(label.isNumber) {
			if(label < (self.eventList.size-1)) {
				locators[label+1].absTime
			} {
				locators[label+1].absTime

			}
		} {
			curIdx = self.eventList.detectIndex({ arg x; self.isLocator(x, label) });
			if(curIdx.isNil) {
				Log(\Param).error("locator not found: %", label);
				nil
			} {
				if(curIdx < ( self.eventList.size-1 )) {
					next = self.eventList[curIdx+1..].detect({ arg x; self.isLocator(x) });
					if(next.isNil) {
						Log(\Param).error("locator next to % not found", label);
						nil
					} {
						next.absTime;
					}
				} {
					Log(\Param).error("locator next to % not found", label);
				}
			}
		}
	},

	findLocator: { arg self, loclist, loc;
		if(loc.isNumber) {
			loclist[loc]
		} {
			loclist.detect({ arg x; self.isLocator(x, loc) })
		}
	},

	locatorLabelToIndex: { arg self, loclist, label;
		if(label.isNumber) {
			label
		} {
			loclist.detectIndex({ arg x; self.isLocator(x, label) })
		}
	},

	getPatternSlice: { arg self, cutFunction, marker_start, marker_end, repeatCount;
		var start, end;
		var loclist = self.eventList.select({arg x; self.isLocator(x)});
		marker_start = self.locatorLabelToIndex(loclist, marker_start);
		marker_start = marker_start % ( loclist.size - 1 ); // -1 to avoid start to be end

		marker_end = marker_end ? ( marker_start+1 );
		marker_end = self.locatorLabelToIndex(loclist, marker_end);
		marker_end = marker_end % ( loclist.size );

		start = loclist[marker_start].absTime;
		end = loclist[marker_end].absTime;
		self.getPreviewLoopPattern(cutFunction, start, end, repeatCount)
	},

	getPatternSlices: { arg self, cutFunction, repeatCount=1;
		var loclist = self.eventList.select({arg x; self.isLocator(x)});
		loclist.drop(-1).collect({ arg loc, idx;
			self.getPreviewLoopPattern(cutFunction, loclist[idx].absTime, loclist[idx+1].absTime, repeatCount)
		});
	},

));

