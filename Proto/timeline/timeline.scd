
///////////////////////////////////////////////////////////////////// timeline objects


ProtoClassDef(\EventTimeline, (
	proxyQuantIsDefault: true, // the only way to preserve default proxy quant

	controller_: { arg self, val;
		self[\controller] = val;
		self.startControl;
		self.changed(\controller);
	},

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
		self.cursor.startPosition = nil; // no preview loop cursor
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

	cutPatternFunction: { arg self, parentEvent;
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
		self.loopMaster.getPreviewLoopPattern(self.cutPatternFunction, absStart, absEnd, repeatCount ? self.repeatCount, self.automationPattern);
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
		PlayerWrapper(self.proxy).stop;
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
		// note that RecordButton call directly recorder.startRecording
		if(self.recorder.notNil) {
			//self.startControl; // in hook now
			self.recorder.isRecording = val;
		}
	},

	asPlayerEvent: { arg self;
		PlayerEvent((
			receiver: ( "{ " ++ self.refCompileString ++ " }" ).interpret, // to have closed compile string
			timeline: ( "{ " ++ self.refCompileString ++ " }" ).interpret,
			label: { arg self; self.timeline.label },
		))
	},

	asPatternEvent: { arg self;
		PatternEvent((
			//eventType: \timeline, // i want to get ride of this type system // done! 
			timeline: ( "{ " ++ self.refCompileString ++ " }" ).interpret, // to have closed compile string
		))
		
	},

	refCompileString: { arg self, name;
		"ProtoClassDef(\\EventTimeline)"
	},

	presetCompileString: { arg self;
		var str = List.new;
		str = str.add("%.param = %;".format(self.refCompileString(self.key.asCompileString), self.param.asCompileString));
		str = str.add("%.eventList = %".format(self.refCompileString(self.key.asCompileString), self.eventList.presetCompileString));
		if(self.automationRack.notNil and: { self.automationRack.isEmpty != true }) {
			str = str.add(self.automationRack.presetCompileString("%.automationRack".format(self.refCompileString)))
		};
		str.join($\n);
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


	hasSavePath: { arg self;
		self.presetCompileStringSavePath.notNil
	},

	presetCompileStringSaveFilename: { arg self;
		var etype;
		etype = self.eventType.asString;
		etype = "%%".format(etype[0].toUpper, etype[1..]);
		"%_%.scd".format(etype, self.key)
	},

	presetCompileStringSaveFile: { arg self;
		if(self.presetCompileStringSavePath.notNil) {
			if( self.presetCompileStringSavePath.isFolder) {
				self.presetCompileStringSavePath +/+ self.presetCompileStringSaveFilename
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
				mypath = myfolderpathname.fullPath +/+ PathName(mypath).fileName;
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
						PathName(Document.current.path).pathOnly +/+ self.presetCompileStringSaveFilename,
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
			"%.savePresetCompileString: no file to load: %".format(
				self.refCompileString, 
				self.presetCompileStringSavePath
			).error;
		}
	},

	asPreview: { arg self;
		TimelinePreview.new
			.mapEventList(self.eventList)
			.areasize_(self.areasize)
			.view.mouseDownAction_({ 
				self.edit;
			}).fixedHeight_(40);
	},

	addHistorySnapshot: { arg self;
		self.eventLoop.historyAddSnapshot;
	},

	automationRack: { arg self;
		var arkey = "AutomationRack_%_%".format(self.eventType, self.key);
		self[\automationRack] = TrackDef(arkey, \ParamTimelineRack);
		self[\automationRack];
	},

	automationEnabled: true,

	automationPattern: { arg self;
		if(self.automationEnabled == true) {
			self.automationRack.asPattern
		} {
			nil
		};
	},
));

ProtoClassDef(\ClipTimeline, (
	parent: ProtoClassDef(\EventTimeline),
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
		self.recorder = ProtoTemplateDef(\SampleClipTimelineMultiTrackRecorder).new(self) !? { arg x; {x} };

		self.clipList = ProtoClassDef(\ClipTimelineClipList).new(self) !? { arg x; {x} };
		//"=============clipTimeline: end init".debug;

		self;
	},

	bindPlayWithRecord: false,

	track_count: { arg self;
		self.tracks.size;
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
		// used by mutingPattern, SampleTimeline and steptimeline
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
		var x = ProtoTemplateDef(\ClipTimelinePanelView).new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		ProtoTemplateDef(\ClipTimelinePanelView).newView(self).makeLayout;
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
			//Log(\Param).debug("recorded ev: %".format(ev));
			tl = ev.timeline;
			if(self.eventList.any{ arg xev; 
				//Log(\Param).debug("eventList ev: %".format(xev));
				xev.timeline.value === tl 
		   	}) {
				//Log(\Param).debug("yep i keep: %".format(tl));
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
//~clipTimeline = ClipTimeline.proto;

ProtoClassDef(\TrackTimeline, (
	parent: ProtoClassDef(\ClipTimeline),
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
		// FIXME: i removed support for PlayerWrapperGroup, think to add it back
		self.basicInit;
		//Log(\Param).debug("TrackTimeline init 1");
		//if(tracks.isKindOf(TrackDef)) {
			//var trackdef = tracks;
			////tracks = PlayerWrapperGroup(tracks.children);
			////tracks.playerEventWrapper = { arg ev;
				////ev.receiver = "{ arg self; TrackDef(%).childAt(%) }".format(trackdef.key)

			////}
		//};
		//self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment
		//Log(\Param).debug("TrackTimeline init 2");
		//eventfac = ~class_track_event_factory.new(tracks);
		//self.eventFactory = { eventfac };
		//self.eventFactory.eventLoop = self.eventloop;
		//self.eventFactory.mode = \note;
		//self.eventFactory.playmode = \gate;
		//self.eventFactory.fullrecord = false;

		self.tracks = tracks;

		self.tracks_bypassed = Set.new;
		//self.wrapper = ProtoTemplateDef(\ParPlayerGroup).new(tracks) !? { arg x; {x} };
		//Log(\Param).debug("TrackTimeline init 3");

		self.clipList = ProtoClassDef(\ClipTimelineClipList).new(self) !? { arg x; {x} };

		self.updateClipList;

		//"=============trackTimeline: end init".debug;
		//Log(\Param).debug("TrackTimeline init 4");

		self;
	},

	tracks_: { arg self, tracks;
		// FIXME: i removed support for PlayerWrapperGroup, think to add it back
		self[\tracks] = { tracks.children };
		self.mainTrack = tracks;
		//self.tracks = { tracks.children };
		self.areasize = Point(self.areasize.x, self.tracks.size );
		if(self.recorder.notNil) {
			self.recorder.stopRecording;
			self.recorder.target = self.mainTrack;
		} {
			self.recorder = ProtoTemplateDef(\PlayerGroupRecorder).new(self.mainTrack) !? { arg x; {x} };
			self.recorder.inQuantMode = true; // listen to \playing instead of \play
			self.recorder.eventLoop = self.eventLoop;
		};
		self.changed(\tracks);
	},


	updateClipList: { arg self;
		var clips = self.tracks.collect { arg track, idx;
			track.children.collect { arg clip, idx;
				var id;
				if(clip.isEmpty.not) {
					id = ProtoDef(\GlobalLibrary).targetToLibId(clip);
					//Log(\Param).debug("id %", id);
					self.clipList.addClipIdToClipList(id);
					id;
				}
			};
		};
	},


	edit: { arg self;
		var x = ProtoTemplateDef(\TrackTimelinePanelView).new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		ProtoTemplateDef(\TrackTimelinePanelView).newView(self).makeLayout;
	},


	refCompileString: { arg self, name;
		if(name.isNil) {
			name = self.key.asCompileString
		};
		"TrackTimeline(%)".format(name)
	},
));


ProtoClassDef(\NoteTimeline, (
	parent: ProtoClassDef(\EventTimeline),
	all: { PresetDictionary.new(\noteTimeline) },

	eventType: \noteTimeline,
	composeParamPattern: true,
	controller: { ControllerDef.getTagSpec(\piano).list.asDict[\piano_generic] },

	new: { arg self, name, param;

		if(self.all[name].notNil) {
			self = self.all[name];
			if(param.notNil) { self.param = param };
		} {
			//self = self.deepCopy;
			self = NoteTimeline.newFromProto(self.deepCopy);
			self.prAdd(name).init(param);
		};
	
		self;
	},

	init: { arg self, param;

		self.basicInit;
		self.param = param; // depends on eventloop

	},

	trackInstrument_: { arg self, instr;
		//Log(\Param).debug("NoteTimeline % trackInstrument_: %", self.key, [instr.hash, instr.target, instr.libId]);
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
		self.wrapper = ProtoTemplateDef(\SeqPlayerGroup).new(param.target) !? { arg x; {x} };
		self.recorder = ProtoTemplateDef(\PlayerGroupRecorder).new(self.wrapper) !? { arg x; {x} };
		self.recorder.eventLoop = self.eventLoop;
		self.recorder.startRecordingHook = { arg iself; self.startControl };
		self.changed(\param);
		self.changed(\recorder);

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


	cutPatternFunction: { arg self, parentEvent, parentTimeline;
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


			//pat = Ppar([pat, self.automationRack.asPattern]);
			//pat = Pbind(\addAutomation, Prout({ arg ev;
				//var cleanup = EventStreamCleanup();
				//cleanup.addFunction(ev, {
					//"cleanup save me!!!".debug;
					//self.automationRack.stop;
				//});
				//ev[\finish] = ev[\finish].addFunc({
					//if(self.automationRack.isEmpty.not) {
						//"playing automation".debug;
						//self.automationRack.playNow;
					//};
				//});
				//inf.do { arg idx;
					//ev = 1.yield;
					//cleanup.update(ev);
				//};
				
			//})) <> pat;

			if(parentTimeline.notNil) {
				pat = parentTimeline.mutingPattern(parentEvent) <> pat;
			};

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
		var x = ProtoTemplateDef(\NoteTimelinePanelView).new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		ProtoTemplateDef(\NoteTimelinePanelView).newView(self).makeLayout;
	},

	refCompileString: { arg self, name;
		if(name.isNil) {
			name = self.key.asCompileString
		};
		"NoteTimeline(%)".format(name)
	},
));

ProtoClassDef(\ParamTimeline, (
	parent: ProtoClassDef(\EventTimeline),
	all: { PresetDictionary.new(\paramTimeline) }, 
	eventType: \paramTimeline,

	new: { arg self, name, param;
	
		if(self.all[name].notNil) {
			self = self.all[name]
		} {
			self = ProtoClass(( parent:ProtoClassDef(\ParamTimeline) ));
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
		// destined to TimelineEnvView because all events in EventList have \level key
		if(self.param.notNil) {
			Param(self.param.target, \level, self.param.spec);
		} {
			// FIXME: this is maybe wrong
			Param(Message(self), \levelValue);
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
		self.recorder = ProtoTemplateDef(\ParamRecorder).new(self.param) !? { arg x; { x } };
		self.recorder.eventLoop = self.eventLoop;

		if(self.customBus.isNil) {
			if(param.target.isKindOf(Bus)) {
				self[\outBus] = param.target;
			} {
				// FIXME: should always provide a bus or customBus when calling newInstance
				// else everything write to the same bus since .key is the same
				self[\outBus] = BusDef("ParamTimeline_%".format(self.key).asSymbol, \control);
			};
		} {
			self[\outBus] = self.customBus;
		};
		self.changed(\param);
	},

	inBusMode: { arg self;
		self.param.inBusMode;
	},

	inBusMode_: { arg self, val;
		self.param.inBusMode = val;
		self.changed(\inBusMode);
	},

	init: { arg self, param, bus;
		self.basicInit;
		self.customBus = bus;

		self.param = param ?? { Param(Message((empty:1)), \empty, \unipolar) };

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

	cutPatternFunction: { arg self;
		{ arg sloop, offset, dur;
			var postpat, prepat;
			var pat;
			// FIXME: this should be an event
			if(self.param.notNil and:{ self.param.target.isKindOf(Bus).not }) {
				// FIXME: bus mode should be generic
				//if([EventPatternProxy, NodeProxy].any({ arg x; self.param.target.isKindOf(x) })) {
					//self.param.target.set(self.param.property, self.outBus.asMap);
					//self.param.changed(\inBusMode);
				//} {
					//self.param.set(self.outBus.asMap);
				//};
				self.param.setBus(self.outBus.asMap);
				{
					self.changed(\inBusMode);
					//self.changed(\param);
				}.defer;
				//default = self.param.default;
			};

			self.remove_envlist_duplicate(self.eventList);
			self.compute_eventenv_sustain(self.eventList);

			postpat = Pbind(
				\out, self.outBus,
				//\level, Pfunc({ arg ev; ev[self.param.property] ? default }),
				\time, Pfunc({ arg ev; 
					//Log(\Param).debug("cutPatternFunction ParamTimeline postpat time");
					ev.use { 
						// absTime is in beats (but maybe should be in seconds ?)
						// need to convert to seconds
						//(ev.next_absTime ? ev.absTime) - ev.absTime / thisThread.clock.tempo
						// use dur instead
						//( ev[\sustain] ? 1 ) / thisThread.clock.tempo
						( ~sustain.value ? 1 ) / thisThread.clock.tempo
					} 
				}),
				\type, Pfunc({  arg ev;

					//Log(\Param).debug("postpat type %", ev[\type]);
					//Log(\Param).debug("postpat ev %", ev);
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
			//pat = postpat.trace(prefix:"postpat: ") <> pat.trace(prefix:"pat: ") <> prepat.trace(prefix:"prepat: ");
			pat = postpat <> pat <> prepat;

			~pat = pat;
			//~pattern_to_envpattern.(pat).embedInStream(inval ? ()); 
			self.pattern_to_envpattern(pat).embedInStream;
		}
	},

	asInlinePattern: { arg self;
		Prout({ arg ev;
			ev[\finish] = ev[\finish].addFunc({
				var ev = self.asPlayerEvent;
				ev.sustain = \inf;
				ev.play;
			});
			loop{
				ev = self.outBus.asMap.yield;
			};
			ev;
		})
	},

	asControlInput: { arg self;
		self.outBus.asMap
	},

	asStream: { arg self;
		if(self.inlineModeEnabled == true) {
			self.asInlinePattern.asStream;
		} {
			self.asPattern.asStream;
		}
	},

	streamArg: { arg self;
		self.asStream;
	},


	edit: { arg self;
		var x = ProtoTemplateDef(\ParamTimelinePanelView).new(self);
		self.editor = { x };
		x;
	},

	asPreview: { arg self;
		TimelinePreview_Env.new
			.mapEventList(self.eventList)
			.mapParam(self.levelParam)
			.areasize_(self.areasize)
			.view.mouseDownAction_({ 
				self.edit;
			})
			.fixedHeight_(40)
		;
	},

	asView: { arg self;
		ProtoTemplateDef(\ParamTimelinePanelView).newView(self).makeLayout;
	},

	asAutomationView: { arg self;
		ProtoTemplateDef(\ParamTimelinePanelView).newView(self).makeAutomationLayout;
	},


	refCompileString: { arg self, name;
		if(name.isNil) {
			name = self.key.asCompileString
		};
		"ParamTimeline(%)".format(name)
	},

	// useful methods for EventList containing curve data

	remove_envlist_duplicate: { arg self, eventlist;
		// in-place remove
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
		eventlist
	},

	compute_eventenv_sustain: { arg self, eventlist;
		// in-place
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

	},

	eventlist_to_envpattern: { arg self, eventlist, start;
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
	},

	pattern_to_envpattern: { arg self, pattern;
		Prout({ arg inval;
			var str = pattern.asStream;
			var val;
			var first = true;
			var previous_val;
			var nextval; // not used
			var is_rest = { arg val;
				val.type == \rest or: { val.type == \start or: { val.type == \end } };
			};

			//val.debug("pattern_to_envpattern start");

			block { arg break;
				while({
					if(nextval.notNil) {
						val = nextval;
					} {
						val = str.next(Event.default);
					};
					val.notNil;
				}, {
					//val.debug("pattern_to_envpattern val");
					if(first == true) { // first not rest
						if(is_rest.(val)) {
							//val.type.debug("pattern_to_envpattern: start/end");
							previous_val = val;
							previous_val.yield;
						} {
							// need a bus.set event because env_segment start from bus value
							var level = val.use { val.level };
							var busev = val.copy;
							var next_level, next_absTime;
							//level.debug("pattern_to_envpattern: first");
							if(busev[\event_dropdur].notNil) {
								next_level = val[\next_level];
								next_absTime = val[\next_absTime];
								//[ next_level, next_absTime, busev ].debug("nlevel, nabstime, busev");
								level = 
								( next_level - busev.level ) * busev.event_dropdur 
								/ (next_absTime - busev.absTime) 
								+ busev.level
								;
							};
							//level.debug("pattern_to_envpattern: first: altered level");
							busev.putAll(
								(
									type: \bus,
									array: [level],
									delta: 0,
									dur: 0,
								)
							);
							busev.yield;
							previous_val = val;
							first = false;
						}
					} {
						if(previous_val.use { previous_val.time } <= 0) {
							previous_val[\array] = [previous_val[\next_level]];
							//previous_val.debug("pattern_to_envpattern: yield previous time=0");
							previous_val[\type] = \bus;
						} {
							previous_val[\level] = previous_val[\next_level];
							//previous_val.debug("pattern_to_envpattern: yield previous");
							previous_val;
						};
						previous_val.yield;
						previous_val = val;
					};
				});
			};
			//previous_val[\type] = \rest; // this seems to be the cause to the last segment is rest
			if(previous_val.type != \start and: { first != true }) {
				// if eventList has no content (contains only start event and end event is ommited)
				// 		previous_val is type start and should not be repeated here
				// if eventList has only one point (and start event)
				// 		the last point is always rest, so "first" never became false
				//		should not be repeated here

				if(previous_val.use { previous_val.time } <= 0) {
					previous_val[\array] = [previous_val[\next_level]];
					previous_val[\type] = \bus;
				} {
					previous_val[\level] = previous_val[\next_level];
					//previous_val.debug("pattern_to_envpattern: yield last previous");
					previous_val;
				};
				previous_val.yield;
			}
		});

	},
));

ProtoClassDef(\KitTimeline, (
	parent: ProtoClassDef(\EventTimeline),
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
			self[\wrapper] = ProtoTemplateDef(\ParPlayerGroup).new(self.kit) !? { arg x; {x} };
		} {
			self[\wrapper] = kit;
			self[\kit] = { arg self; self.wrapper.getPatternList };
		};
		self.recorder = ProtoTemplateDef(\PlayerGroupRecorder).new(self.wrapper) !? { arg x; {x} }; 
		self.recorder.eventLoop = self.eventLoop;
		// FIXME: this polymorphism look ugly
		self.changed(\kit);
		self.changed(\wrapper);
	},

	wrapper_: { arg self, kit;
		self.kit = kit;
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

	//startControl: { arg self;
		//if(self.controller.notNil) {
			//self.controller.startControl(self.wrapper);
		//}
	//},


	asPreview: { arg self;
		TimelinePreview.new(\kitIndex)
			.mapEventList(self.eventList)
			.viewport_(Rect(0,0,1,1/4))
			.areasize_(self.areasize)
			.view.mouseDownAction_({ 
				self.edit;
			}).fixedHeight_(40);
	},

	cutPatternFunction: { arg self;
		{ arg sloop, offset, dur;
			var pat;
			pat = sloop.cutPattern(
				Pn(self.eventList.clone, 1), // clone avoid lose of sync when adding note but require to wait the end of loop
				// TODO: schedule added notes on clock to have a virtual real-time refresh
				offset, dur
			);


			//Log(\Param).debug("KitTimeline: cutPatternFunction 1");
			if(self.kit.notNil and: {self.composeParamPattern == true}) {
				//Log(\Param).debug("KitTimeline: cutPatternFunction 2");
				pat = PdrumStep(self.kit, pat, 1, key: \kitIndex);
				//~pat = PdrumStep(self.kit, pat, 1, key: \kitIndex);
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
		var x = ProtoTemplateDef(\KitTimelinePanelView).new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		ProtoTemplateDef(\KitTimelinePanelView).newView(self).makeLayout;
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
//ProtoClassDef(\NoteTimeline).all.loadIfNotInitialized;
//ProtoClassDef(\ParamTimeline).all.loadIfNotInitialized;
//ProtoClassDef(\ClipTimeline).all.loadIfNotInitialized;

/////////////////////// cliplist side panel


ProtoClassDef(\ClipTimelineClipList, (
	new: { arg self, timeline;
		self = self.deepCopy;
	
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
		//Log(\Param).debug("eventToClipId: ev: %".format(ev));
		if([\rest, \start,\end].includes(ev.type)) {
			nil
		} {
			libId = self.lib.eventToLibId(ev); 
			if(libId.notNil) {
				libId
			} {
				//Log(\Param).debug("eventToClipId: no libId: ev:%", ev);
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

	clearClipList: { arg self;
		self.clipList.clear;
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
		};
		self.gatherClipsFromTrackList;
	},

	gatherClipsFromTrackList: { arg self;
		if(self.timeline.mainTrack.notNil) {
			self.timeline.mainTrack.existingChildren.collect { arg track, idx;
				if(track.notNil and: { track.isEmpty.not }) {
					track.existingChildren.collect { arg child, idx;
						var clipId = self.lib.targetToLibId(child);
						self.addClipIdToClipList(clipId);
					};
				}
			};
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
			type: \rest,
			dur: 0,
			startCursorEvent: true,
			play: {
				self.cursor.startTime = thisThread.beats;
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

	cutPatternFadeOut: { arg self, pat, firstEventOffset, dur, fadeOut=0;
		// do not embedInStream to allow composing it
		if(fadeOut.isNil or: { fadeOut <= 0}) {
			self.cutPattern(pat, firstEventOffset, dur)
		} {
			Log(\Param).debug("fadeout %, %", fadeOut, dur - fadeOut);
			if(firstEventOffset < 0) {
				Pseq([
					Pfindur(dur - fadeOut, 
						Pseq([
							Event.silent(firstEventOffset.abs),
							Pembed(pat.value, 0, self.timeline)
						],1),
					),
					Event.silent(fadeOut),
				],1)
			} {
				Pseq([
					Pfindur(dur - fadeOut, 
						Pembed(pat.value, firstEventOffset, self.timeline)
					),
					Event.silent(fadeOut),
				],1)
			}
		}
	},

	cutPattern: { arg self, pat, firstEventOffset, dur;
		// do not embedInStream to allow composing it
		// timeline.cutPatternFunction is responsible for calling .embedInStream
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
		// customTotalDur is not the duration of the final pattern. totalDur = endEvent - startEvent. A customTotalDur is just like changing the endEvent position. Then customStartEventOffset is applied
		// repeat pattern when outside clip loop
		// we want to give time since start event
		// cutFunction is rather a SliceGenerator that take offset and dur and return a slice of the pattern
		// 		it is provided by the timeline because the timeline know better how to cut the pattern
		//		it generally call sloop.cutPattern but can do other things
		//		for example this allow to start inside a sample without having to fast forward events manually
		Prout({
			var sloop = self;
			var ret;
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
				ret = self.embedPattern(cutFunction, firstEventOffset, dur)
			};
			// Prout return value is important, if we do not return ret, repeatCount is returned and this cause bug when using Pseq([ thisPat, Event.silent(1) ]); 
			ret; 

		})
	},

	enablePreviewLoopToClipStart: true,

	getPreviewLoopPattern: { arg self, cutFunction, absStart, absEnd, customRepeatCount, parallelPat;
		var pat;
		// read outside notes when outside clip loop
		pat = Prout({
			var sloop = self;
			var ret;
			var repeatCount = customRepeatCount ? sloop.repeatCount;
			repeatCount.do { arg count;
				var previewStartAbsTime;
				var firstEventOffset;
				var previewEndAbsTime;
				var finalStartAbsTime;
				var dur;

				if(self.timeline.isRecording) {
					Log(\Param).debug("timeline is recording, playing is muted");
					ret = Event.silent(1).yield;
				} {
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
					ret = self.embedPattern(cutFunction, firstEventOffset, dur);
				};
			};
			ret;

		});
		if(parallelPat.notNil) {
			//parallelPat.debug("parallelPat");
			pat = Ppar([
				pat,
				parallelPat
			])
		};
		pat;
		
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

if(currentEnvironment.isKindOf(ProxySpace).not) {
~kitTimeline = ProtoClassDef(\KitTimeline);
~envTimeline = ProtoClassDef(\ParamTimeline);
~noteTimeline = ProtoClassDef(\NoteTimeline);
~trackTimeline = ProtoClassDef(\TrackTimeline);
~clipTimeline = ProtoClassDef(\ClipTimeline);
~eventTimeline = ProtoClassDef(\EventTimeline);

};
