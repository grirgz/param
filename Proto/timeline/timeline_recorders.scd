
////////// recorders

// can record seqPlayerGroup (NoteTimeline) and parPlayerGroup (KitTimeline)
ProtoTemplateDef(\PlayerGroupRecorder, (
	inQuantMode: false,
	new: { arg self, group;
		self = self.deepCopy;
	
		self.init(group);
	
		self;
	},

	init: { arg self, target;
		self.target = target;
		self.tempoClock = TempoClock.default;
	},

	eventLoop: { arg self;
		if(self[\priv_eventLoop].isNil) {
			self[\priv_eventLoop] = XEventLoop.newInstance; // will be overwritten by timeline eventloop
		};
		self[\priv_eventLoop];
	},

	eventLoop_: { arg self, el;
		self[\priv_eventLoop] = el;
	},

	eventList: { arg self;
		self.eventLoop.list
	},

	eventList_: { arg self, val;
		self.eventLoop.list = val;
	},

	startRecording: { arg self;
		var startRecordingTime = self.tempoClock.beats;
		self.controller.remove;
		self.controller = self.target.makeListener({ arg player, status, status2;
			var playStatus, stopStatus;
			if(status == \PlayerWrapper) {
				status = status2;
			};
			if(self.inQuantMode) {
				playStatus = \playing;
				stopStatus = \stopped;
			} {
				playStatus = \play;
				stopStatus = \stop;
			};
			Log(\Param).debug("startRecording: listener: status: %", status);
			if(status == playStatus) {
				if(player.recordedEvent.notNil and: {
					player.recordedEvent.clockTime > startRecordingTime
				}) {
					// this player is already recording an event, ignore duplicate signals
					// FIXME: how to prevent breaking recording by skipping to nil the recordedEvent ?
					Log(\Param).debug("listener: playStatus: already recording event for player %", player);
				} {
					//Log(\Param).debug("listener xxx");
					//Log(\Param).debug("listener xx : %", player);
					//Log(\Param).debug("listener : %", player.asPlayerEvent);
					player.recordedEvent = self.eventLoop.recordEvent(player.asPlayerEvent);
					//Log(\Param).debug("listener xxx2");
					player.recordedEvent[\clockTime] = self.tempoClock.beats;
					//Log(\Param).debug("listener: start: player:%, recordedEvent:%", player, player.recordedEvent);
				}
			} {
				//Log(\Param).debug("listener xxx3");
				if(status == stopStatus and: {player.recordedEvent.notNil}) {
					//Log(\Param).debug("listener xxx4");
					player.recordedEvent[\sustain] = self.tempoClock.beats - player.recordedEvent[\clockTime];
					//Log(\Param).debug("listener xxx4.1");
					//Log(\Param).debug("listener: stop: recordedEvent:%", player.recordedEvent);
					player.recordedEvent.changed(\refresh);
					//Log(\Param).debug("listener xxx5");
					player.recordedEvent = nil; // prevent setting sustain again at end of record
				} {
					Log(\Param).debug("listener: don't know what to do with %: recordedEvent:%", status.asCompileString, player.recordedEvent);

				}
			}
		});
		self.eventLoop.startRec(false, self.quant ? Pdef.defaultQuant, if(self.inQuantMode ) { 0 });
		self.changed(\startedRecording);
		self.changed(\isRecording);
		self.startRecordingHook;
	},

	isRecording: { arg self;
		self.eventLoop.isRecording;
	},

	isRecording_: { arg self, val;
		if(val == true) {
			self.startRecording;
		} {
			self.stopRecording;
		}
	},

	stopRecording: { arg self;
		self.controller.remove;
		self.eventLoop.stopRec;
		self.changed(\stoppedRecording);
		self.changed(\isRecording);
		self.stopRecordingHook;
	},

	presetCompileStringPbind: { arg self;
		var ret = "";
		var lists = (
		);

		// envent list to Pbind containing Pseq's
		// TODO: need to strip event list from all specials event (like locations) because no way to represent them within Pseq
		// 		but it's a good thing to have them, maybe includes them in a separate Pbind wihtin Ppar

		//self.eventLoop.list.do ({ arg ev;
		//	if(ev.type != \start or: { ev.type != \end }) {
		//	};
		//	ev.keys.do { arg key;
		//		if(ev[key].isNil and: { [\start, \end, \rest].includes(ev[\type]).not and: { ev[\isRest] != false } }) {
		//			ev[key] = \none;
		//		};
		//		if(lists[key] != \none) {
		//			lists[key] = lists[key].add(ev[key]);
		//		};
		//	};
		//});

		self.eventLoop.list.do ({ arg ev;
			//ev.debug("recorder: process ev");
			if(ev.type == \start or: { ev.type == \end }) {
			} {
				[\kitIndex, \midinote, \dur, \sustain].do { arg key;
					lists[key] = lists[key].add(ev[key] ? 1);
				}
			};
		});

		//lists.debug("recorder: getPresetCompileString: lists");

		ret = lists.keys.as(Array).collect({ arg key;
			if(lists[key] != \none) {
				"\t%, Pseq(%),\n".format(key.asCompileString, lists[key].asCompileString)
			} { 
				""
			};
		}).join;

		ret = "Pbind(\n%)".format(ret);

		if(self.eventLoop.list[0].dur != 0) {
			ret = "Pseq([ Event.silent(%), % ])".format(self.eventLoop.list[0].dur, ret)
		};
		ret = "TimelineEventList.newFrom(%);".format(ret);
		ret;
	},

	presetCompileString: { arg self;
		var ret = "";
		ret = self.eventList.presetCompileString;
		ret;
	},
));

// can record Param
ProtoTemplateDef(\ParamRecorder, (
	parent: ProtoTemplateDef(\PlayerGroupRecorder),

	new: { arg self, param;
		self = self.deepCopy;
	
		self.init(param);
	
		self;
	},

	init: { arg self, target;
		self.target = target;
		self.tempoClock = TempoClock.default;
	},

	startRecording: { arg self;
		// should i notify the record only start at first input or not ?
		// say no
		if(self.eventLoop.notNil) {
			if(self.controller.notNil) {
				self.controller.remove;
			};
			self.controller = self.target.makeListener({ arg obj, param;
				var ev = (level:param.get);
				Log(\Param).debug("LEVEL EV RECORDING !! %", ev);
				self.eventLoop.recordEvent(ev);
			});
			self.eventLoop.startRec(false, 0);
			Log(\Param).info("recording started");
			self.changed(\isRecording);
			self.changed(\startedRecording);
		}
	},

	stopRecording: { arg self;
		if(self.eventLoop.notNil) {
			if(self.controller.notNil) {
				self.controller.remove;
			};
			self.eventLoop.stopRec;
			Log(\Param).info("recording stopped");
			self.changed(\isRecording);
			self.changed(\stoppedRecording);
		}
	},

));

// used in SampleTimeline
ProtoTemplateDef(\SampleTimelineRecorder, (
	isRecording: false,
	new: { arg self, timeline;
		self = self.deepCopy;
	
		self.init(timeline);
		
	
		self;
	},
	tempoClock: { arg self; self.timeline.tempoClock ? TempoClock.default },
	quant: { arg self; self.timeline.quant },
	init: { arg self, timeline;
		//"ini".debug;
		self.recorder = Recorder(Server.default);
		self.drawTask = TaskProxy.new;
		self.tempoClock = TempoClock.default;
		//self.tempoClock.debug("clock");
		self.timeline = { timeline };

		self;
	},
	isRecording_: { arg self, val;
		if(val == true) {
			self.startRecording;
		} {
			self.stopRecording;
		}
	},


	recBus: 0,

	trackIndex: { arg self;
		var tl = self.timeline;
		(tl.selection_cursor.startPoint !? _.y ? 0).trunc(tl.quant_ref.value.y)
	},

	trackAbsTime: { arg self;
		var tl = self.timeline;
		tl.selection_cursor.startPosition ? 0
	},

	addEvent: { arg self, ev;
		var tl = self.timeline;
		tl.eventList.addEvent(ev);
		tl.eventList.reorder;
	},

	temporaryRecordFolder: { arg self;
		(
			FileSystemProject.current !? _.temporaryRecordFolder ?? {FileSystemProject.temporaryRecordFolder ?? { "/tmp/"  }};
		).standardizePath
	},

	startRecording: { arg self, numChannels=2;
		var name = "rec_%_%".format(self.timeline.key, Date.localtime.stamp);
		var clip;
		var ev;
		var tl = self.timeline;
		if(self.isRecording == false) {
			self[\isRecording] = true;
			Log(\Param).debug("prepare Start recording %", name);
			self.changed(\userStartedRecording);

			self.recordName = name;
			self.filename = self.temporaryRecordFolder +/+ "%.wav".format(name);
			if(self.recordOnClipTimelineEnabled == true) {
				clip = SampleTimeline(name.asSymbol);
				ev = clip.asPatternEvent;
				self.recordedEvent = ev;
				self.recordedClip = { clip };
				ev[\absTime] = tl.selection_cursor.startPosition ? 0;
				ev[\sustain] = 1/2;
				ev[\midinote] = (tl.selection_cursor.startPoint !? _.y ? 0).trunc(tl.quant_ref.value.y);
				tl.eventList.addEvent(ev);
				tl.eventList.reorder;
				self.drawTask.source = {
					inf.do { arg idx;
						1.wait;
						ev[\sustain] = idx + 1;
						ev.changed(\refresh);
					}
				};
				self.drawTask.quant = self.timeline.quant;
				self.drawTask.play;
			} {
				// recordedClip is already the model
				self.recordedClip = { self.timeline };
			};
			self.recordedClip.fileName = self.filename;
			self.recorder.prepareForRecord(path:self.filename, numChannels:numChannels);
			//self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant)-( s.latency*~t ), {
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				Server.default.bind {
					self.recorder.record(path:self.filename, bus:self.recBus, numChannels:numChannels);
				};
				Log(\Param).debug("Start recording: %: bus:%, chan:%, file:%, beats:%, quant:%, tempo:%", name, self.recBus, numChannels, self.filename, self.tempoClock.beats, self.quant, self.tempoClock.tempo);
				self.startRecordingTime = self.tempoClock.beats; // NOTE: doesnt account for latency but is used to compute duration so not a problem for the moment
				self.changed(\isRecording);
				self.changed(\startedRecording);
			});
		} {
			Log(\Param).debug("Already recording");
		}

	},
	stopRecording: { arg self;
		if(self.isRecording == true) {
			Log(\Param).debug("prepare Stop recording %", self.filename);
			self.changed(\userStoppedRecording);
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				Server.default.bind {
					self.recorder.stopRecording;
				};
				Log(\Param).debug("Stop recording %", self.filename);
				self[\isRecording] = false;
				self.changed(\isRecording);
				self.changed(\stoppedRecording);

				self.recordedDuration = self.tempoClock.beats - self.startRecordingTime;

				if(self.recordOnClipTimelineEnabled == true) {
					self.drawTask.stop;
					self.recordedEvent[\sustain] = self.recordedDuration;
					self.recordedEvent.changed(\refresh);
				};
				{
					var buf = BufDef(self.filename.asSymbol);
					Log(\Param).debug("recorded filename %, buf %, bufdef", self.filename.asSymbol.asCompileString, buf, BufDef(self.filename.asSymbol));
					self.recordedClip.buffer = buf;
					Log(\Param).debug("recorded clip %, clip buffer %", self.recordedClip, self.recordedClip.buffer);
					self.recordedClip.eventList.setEndPosition(self.recordedDuration);
				}.defer(2);
			});
		} {
			Log(\Param).info("Was not recording");
		}
	},

));

// individual track for SampleClipTimelineMultiTrackRecorder
ProtoTemplateDef(\SampleClipTimelineTrackRecorder, (
	recordOnClipTimelineEnabled: true,
	isRecording: false,
	new: { arg self, timeline, trackGetter;
		self = self.deepCopy;
	
		self.init(timeline, trackGetter);
		
	
		self;
	},
	tempoClock: { arg self; self.timeline.tempoClock ? TempoClock.default },
	quant: { arg self; self.timeline.quant },
	init: { arg self, timeline, trackGetter;
		//"ini".debug;
		self.timeline = { timeline };
		self.track = trackGetter;
		self.recorder = Recorder(Server.default);
		self.drawTask = TaskProxy.new;
		self.tempoClock = TempoClock.default;
		//self.tempoClock.debug("clock");

		self;
	},
	isRecording_: { arg self, val;
		if(val == true) {
			self.startRecording;
		} {
			self.stopRecording;
		}
	},


	recBus: { arg self;
		self.track.recordBus
	},

	trackIndex: { arg self;
		self.track.index
	},

	trackAbsTime: { arg self;
		var tl = self.timeline;
		tl.selection_cursor.startPosition ? 0
	},

	addEvent: { arg self, ev;
		var tl = self.timeline;
		tl.eventList.addEvent(ev);
		tl.eventList.reorder;
	},

	addSample: { arg self, event;
		self.timeline.recordedEvents = self.timeline.recordedEvents.add(event);
	},

	startRecording: { arg self, numChannels=2;
		var name = "rec_%_t%_%".format(self.timeline.key, self.trackIndex, Date.localtime.stamp);
		var clip;
		var ev;
		var tl = self.timeline;
		if(self.isRecording == false) {
			self[\isRecording] = true;
			"prepare Start recording".debug(name);

			self.recordName = name;
			self.filename = "/tmp/%.wav".format(name);
			if(self.recordOnClipTimelineEnabled == true) {
				clip = SampleTimeline(name.asSymbol);
				ev = clip.asPatternEvent;
				self.recordedEvent = ev;
				self.recordedClip = { clip };
				ev[\absTime] = self.trackAbsTime;
				ev[\sustain] = 1/2;
				ev[\midinote] = self.trackIndex;
				self.addEvent(ev);
				self.addSample(ev);
				self.drawTask.source = {
					inf.do { arg idx;
						1.wait;
						ev[\sustain] = idx + 1;
						ev.changed(\refresh);
					}
				};
				self.drawTask.quant = self.timeline.quant;
				self.drawTask.play;
			} {
				// recordedClip is already the model
				self.recordedClip = { self.timeline };
			};
			self.recordedClip.fileName = self.filename;
			self.recorder.prepareForRecord(path:self.filename, numChannels:numChannels);
			//self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant)-( s.latency*~t ), {
			if(tl.bindPlayWithRecord == true) {
				tl.play;
			};
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				Server.default.bind {
					self.recorder.record(path:self.filename, bus:self.recBus, numChannels:numChannels);
				};
				self.changed(\isRecording);
				Log(\Param).debug("Start recording: %: bus:%, chan:%, file:%, beats:%, quant:%, tempo:%", name, self.recBus, numChannels, self.filename, self.tempoClock.beats, self.quant, self.tempoClock.tempo);
				self.startRecordingTime = self.tempoClock.beats; // NOTE: doesnt account for latency but is used to compute duration so not a problem for the moment
			});
		} {
			"Already recording".debug;
		}

	},
	stopRecording: { arg self;
		if(self.isRecording == true) {
			"prepare Stop recording".debug(self.filename);
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				Server.default.bind {
					self.recorder.stopRecording;
				};
				"Stop recording".debug(self.filename);
				self[\isRecording] = false;
				self.changed(\isRecording);


				self.recordedDuration = self.tempoClock.beats - self.startRecordingTime;

				if(self.recordOnClipTimelineEnabled == true) {
					self.drawTask.stop;
					self.recordedEvent[\sustain] = self.recordedDuration;
					self.recordedEvent.changed(\refresh);
				};
				{
					self.recordedClip.buffer = BufDef(self.filename.asSymbol);
					self.recordedClip.debug("recorded clip");
					self.recordedClip.buffer.debug("recorded clip buffer");
					self.recordedClip.eventList.setEndPosition(self.recordedDuration);
				}.defer(2);
			});
		} {
			"Was not recording".debug;
		}
	},

));

// used in ClipTimeline to record a SampleTimeline per armed track
// by creating an array of SampleClipTimelineTrackRecorder
ProtoTemplateDef(\SampleClipTimelineMultiTrackRecorder, (
	isRecording: false,
	new: { arg self, timeline;
		self = self.deepCopy;
	
		self.init(timeline);
	
		self;
	},
	quant: { arg self; self.timeline.quant },
	init: { arg self, timeline;
		//"ini".debug;
		self.timeline = timeline;
		self.updateTrackRecorders;

		self;
	},

	updateTrackRecorders: { arg self;
		self.recorders = self.recorders ?? { List.new };
		self.recorders = self.timeline.tracks.collect({ arg track, idx;
			self.recorders[idx] ?? { 
				var trackGetter = { self.timeline.tracks[idx] };
				ProtoTemplateDef(\SampleClipTimelineTrackRecorder).new(self.timeline, trackGetter) 
			}
		})
	},

	isRecording_: { arg self, val;
		if(val == true) {
			self.startRecording;
		} {
			self.stopRecording;
		}
	},

	startRecording: { arg self, numChannels=2;
		if(self.isRecording) {
			"Already recording".debug;
		} {
			var toRec = self.recorders.select({ arg r; r.track.recordArmed });
			if(toRec.size == 0) {
				"No track is armed for record".debug;
				self.changed(\stoppedRecording);
			} {
				toRec.do(_.startRecording);
				self[\isRecording] = true;
				self.changed(\userStartedRecording);
				PlayerWrapper(toRec.first).doWithQuant({
					self.changed(\startedRecording);
				})
			};
		}
	},
	stopRecording: { arg self;
		self[\isRecording] = false;
		self.changed(\userStoppedRecording);
		self.recorders.do(_.stopRecording);
		PlayerWrapper(self.recorders.first).doWithQuant({
			self.changed(\stoppedRecording);
		})
	},

));


// i think it's deprecated by SampleClipTimelineMultiTrackRecorder
ProtoTemplateDef(\SampleClipTimelineRecorder, (
	parent: ProtoTemplateDef(\SampleTimelineRecorder),
	recordOnClipTimelineEnabled: true,
));

if(currentEnvironment.isKindOf(ProxySpace).not) {

	~playerGroupRecorder = ProtoTemplateDef(\PlayerGroupRecorder);
	~paramRecorder = ProtoTemplateDef(\ParamRecorder);
}
