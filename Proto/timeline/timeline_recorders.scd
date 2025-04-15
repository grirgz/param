
////////// recorders

// can record seqPlayerGroup (NoteTimeline) and parPlayerGroup (KitTimeline)
ProtoTemplateDef(\PlayerGroupRecorder, (
	inQuantMode: false,
	recordOnlyIndex: true,
	new: { arg self, group;
		self = self.deepCopy;
	
		self.init(group);
	
		self;
	},

	init: { arg self, target;
		self.target = target;
		self.tempoClock = TempoClock.default;
	},

	quant: { arg self; self.target.quant ?? { Pdef.defaultQuant } },

	eventLoop: { arg self;
		if(self[\priv_eventLoop].isNil) {
			self[\priv_eventLoop] = TimelineEventLoop.newInstance; // will be overwritten by timeline eventloop
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

	arpTarget: { arg self;
		self.target.arpTarget;
	},

	emitterPattern: { arg self;
		Pbind(
			// does it override finish function ?
			\finish, { arg ev;
				if(ev.type != \rest and: {ev.midinote != \rest}) {

					//thisThread.clock.beats.debug("emitterPattern: catched ev beats");
					self.arpTarget.asPlayerEvent = ev;
				}
			}
		);
	},

	startRecording: { arg self;
		self.startRecordingTime = self.tempoClock.beats;
		self.installlListener;
		self.eventLoop.startRec(false, self.quant ? Pdef.defaultQuant, if(self.inQuantMode ) { 0 });
		self.changed(\startedRecording);
		self.changed(\isRecording);
		self.startRecordingHook;
	},

	inArpMode: false,
	inArpMode_: { arg self, val;
		self[\inArpMode] = val;
		if(self.isRecording == true) {
			if(self.inArpMode) {
				self.installArpListener;
			} {
				self.installNormalListener;
			};
		};
	},

	installlListener: { arg self;
		
		if(self.inArpMode) {
			self.installArpListener
		} {
			self.installNormalListener;
		};
		
	},

	installNormalListener: { arg self;
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
			//Log(\Param).debug("startRecording: listener: status: %", status);
			if(status == playStatus) {
				if(player.recordedEvent.notNil and: {
					//player.recordedEvent.debug("rec ev");
					player.recordedEvent.clockTime > self.startRecordingTime;
				}) {
					// this player is already recording an event, ignore duplicate signals
					// FIXME: how to prevent breaking recording by skipping to nil the recordedEvent ?
					//Log(\Param).debug("listener: playStatus: already recording event for player %", player);
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
		
	},

	installArpListener: { arg self;
		self.controller.remove;
		self.controller = self.arpTarget.makeListener({ arg player, status, status2;
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
			//Log(\Param).debug("startRecording: listener: status: %", status);
			if(status == playStatus) {
				var recev;
				//Log(\Param).debug("listener xxx");
				//Log(\Param).debug("listener xx : %, clock:%", player, self.tempoClock.beats);
				//Log(\Param).debug("listener : %", player.asPlayerEvent);
				recev = self.eventLoop.recordEvent(player.asPlayerEvent);
				//recev.debug("recorded this event");
				//Log(\Param).debug("listener xxx2");
				//Log(\Param).debug("listener: start: player:%, recordedEvent:%", player, player.recordedEvent);
			};
		});
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
		self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant) + Server.default.latency, {
			self.eventLoop.stopRec;
			self.changed(\stoppedRecording);
			self.changed(\isRecording);
			self.stopRecordingHook;
		});
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

	new: { arg self, param, timeline;
		self = self.deepCopy;
	
		self.init(param, timeline);
	
		self;
	},

	init: { arg self, target, timeline;
		self.target = target;
		self.timeline = timeline; // optional
		self.tempoClock = TempoClock.default;
	},

	quant: { arg self;
		if(self.timeline.notNil) {
			self.timeline.quant;
		} {
			if(self.target.target.isKindOf(Pdef)) {
				self.target.target.quant;
			} {
				Pdef.defaultQuant;
			}
		}
	},

	startRecording: { arg self;
		// should i notify the record only start at first input or not ?
		// say no
		if(self.eventLoop.notNil) {
			if(self.controller.notNil) {
				self.controller.remove;
			};
			self.eventLoop.startRec(false, self.quant ? Pdef.defaultQuant, if(self.inQuantMode ) { 0 });
			self.controller = self.target.makeListener({ arg obj, param;
				var ev = (level:param.get);
				//Log(\Param).debug("LEVEL EV RECORDING !! % : %", ev, Main.elapsedTime);
				self.eventLoop.recordEvent(ev);
			});
			Log(\Param).info("recording started");
			self.changed(\isRecording);
			self.changed(\startedRecording);
		}
	},

	stopRecording: { arg self;
		// since recording is in real time and playing have latency
		// when we record events in the last 0.2 seconds, in reality the processing of the next event has already started
		// the next event is the event we are recording because we are in a loop
		// either we do not record the last 0.2 seconds, or we prevent playing the just recorded event in a loop

		if(self.eventLoop.notNil) {
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant) + Server.default.latency, {
				// adding latency to schedAbs, ClipTimelineTrackRecorder_clip should prevent playing the recorded event until ready
				if(self.controller.notNil) {
					self.controller.remove;
				};
				self.eventLoop.stopRec;
				Log(\Param).info("recording stopped");
				self.changed(\isRecording);
				self.changed(\stoppedRecording);
			});
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
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				Server.default.bind {
					self.recorder.record(path:self.filename, bus:self.recBus, numChannels:numChannels);
				};
				//Log(\Param).debug("Start recording: %: bus:%, chan:%, file:%, beats:%, quant:%, tempo:%", name, self.recBus, numChannels, self.filename, self.tempoClock.beats, self.quant, self.tempoClock.tempo);
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
			//Log(\Param).debug("prepare Stop recording %", self.filename);
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
					//Log(\Param).debug("recorded filename %, buf %, bufdef", self.filename.asSymbol.asCompileString, buf, BufDef(self.filename.asSymbol));
					self.recordedClip.buffer = buf;
					//Log(\Param).debug("recorded clip %, clip buffer %", self.recordedClip, self.recordedClip.buffer);
					self.recordedClip.eventList.setEndPosition(self.recordedDuration);
				}.defer(2);
			});
		} {
			Log(\Param).info("Was not recording");
		}
	},

));

/////// ClipTimeline

(
ProtoTemplateDef(\ClipTimelineTrackRecorder_main, (
	isRecording: false,
	new: { arg self, timeline;
		self = self.deepCopy;
	
		self.init(timeline);
	
		self;
	},
	quant: { arg self; self.timeline.quant },
	init: { arg self, timeline;
		//"ini".debug;
		self.tempoClock = TempoClock.default;
		self.timeline = { timeline };
		//self.updateTrackRecorders;

		self;
	},

	updateTrackRecorders: { arg self;
		self.recorders = self.recorders ?? { List.new };
		self.recorders = self.timeline.tracks.collect({ arg track, idx;
			var trackGetter = { self.timeline.tracks[idx] };
			var src = track.recordSource;
			var rec;
			if(self.recorders[idx].notNil) {
				self.recorders[idx].free;
			};
			//[track, idx, src].debug("updateTrackRecorders");
			if(track.recordArmed == true) {
				if(src.isNil) {
					Log(\Param).debug("Error: can't record with no source: track %".format(idx));
					nil;
				} {
					rec = if(src == \lastTweaked) {
						ProtoTemplateDef(\ClipTimelineTrackRecorder_lastTweaked).new(self.timeline, trackGetter) 
					} {
						ProtoTemplateDef(\ClipTimelineTrackRecorder_clip).new(self.timeline, trackGetter) 
					};
					rec.parentRecorder = self;
					rec;
				};
			};
		}).select(_.notNil);
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
			var toRec;
			self.updateTrackRecorders;
			toRec = self.recorders;
			if(toRec.size == 0) {
				"No track is armed for record".debug;
				self.changed(\stoppedRecording);
			} {
				//"prepare Start recording (main) ===============================".debug;
				self.startRecordingTime = self.tempoClock.nextTimeOnGrid(self.quant) - (self.quant*1); // NOTE: doesnt account for latency but is used to compute starting time of ParamTimeline lastTweaked
				//[self.startRecordingTime, self.tempoClock.beats].debug("main startRecordingTime, beats");
				toRec.do(_.startRecording);
				self[\isRecording] = true;
				self.changed(\userStartedRecording);
				if(self.timeline.bindPlayWithRecord == true) {
					self.startRecordingTime = self.tempoClock.nextTimeOnGrid(self.quant); // NOTE: doesnt account for latency but is used to compute starting time of ParamTimeline lastTweaked
					// TODO: find current playing cursor position when it's already playging
					self.timeline.play;
				};
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
		if(self.timeline.bindPlayWithRecord == true) {
			self.timeline.stop;
		};
		PlayerWrapper(self.recorders.first).doWithQuant({
			self.changed(\stoppedRecording);
		})
	},
));

ProtoTemplateDef(\ClipTimelineTrackRecorder_clip, (
	recordOnClipTimelineEnabled: true,
	isRecording: false,
	new: { arg self, timeline, trackGetter, lastTweaked;
		self = self.deepCopy;
	
		self.init(timeline, trackGetter, lastTweaked);
		
	
		self;
	},
	tempoClock: { arg self; self.timeline.tempoClock ? TempoClock.default },
	quant: { arg self; self.timeline.quant },
	init: { arg self, timeline, trackGetter, lastTweaked;
		//"ini".debug;
		self.timeline = { timeline };
		self.track = trackGetter;
		self.lastTweaked = lastTweaked; // used only for ParamTimeline
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

	free: { arg self;
		if(self.isRecording) {
			self.stopRecording;
		};
	},


	trackIndex: { arg self;
		self.track.index
	},

	trackAbsTime: { arg self;
		var tl = self.timeline;
		var res;
		res = tl.selection_cursor.startPosition ? 0;
		if(self.timeline.bindPlayWithRecord == true) {
			var currentRecBlockDur = self.tempoClock.nextTimeOnGrid(self.quant) - self.parentRecorder.startRecordingTime;
			// since ParamRecorder is quantized (omit first empty quant blocks), we need to start the clip on the previous quant block and not the start of the record session
			//res = tl.eventList.startTime + self.tempoClock.nextTimeOnGrid(self.quant) - self.parentRecorder.startRecordingTime - self.quant;
			res = tl.eventList.startTime + currentRecBlockDur - self.quant;
			//if(self.track.recordSource == \lastTweaked) {
				//res = tl.eventList.startTime + self.tempoClock.nextTimeOnGrid(self.quant) - self.parentRecorder.startRecordingTime - (self.quant*1);
				
			//};
		};
		res;
	},

	addEvent: { arg self, ev;
		var tl = self.timeline;
		tl.eventList.addEvent(ev);
		tl.eventList.reorder;
	},

	addRecordedEvent: { arg self, event;
		self.timeline.recordedEvents = self.timeline.recordedEvents.add(event);
	},

	getTimelineFromRecordSource: { arg self;
		var name = "rec_%_t%_%".format(self.timeline.key, self.trackIndex, Date.localtime.stamp).asSymbol;
		var src = self.track.recordSource;
		case(
			{ src.isKindOf(Pdef)  }, {
				NoteTimeline(name, src);
			},
			{ src.isKindOf(Param)  }, {
				ParamTimeline(name, src);
			},
			{ src.isKindOf(PatKitDef)  }, {
				KitTimeline(name, src);
			},
			{ src == \lastTweaked }, {
				ParamTimeline(name, self.lastTweaked);
			},
			{ src.isKindOf(Bus) or: { src.isKindOf(Number) } }, {
				SampleTimeline(name).recordBus = src;
			}, {
				Log(\Param).debug("getTimelineFromRecordSource: source is not recognized: %", src);
				nil;
			}
		)
	},

	startRecording: { arg self;
		var name = "rec_%_t%_%".format(self.timeline.key, self.trackIndex, Date.localtime.stamp);
		var clip;
		var ev;
		var tl = self.timeline;
		if(self.isRecording == false) {
			// at this point, this code is executed instantly when clicked on record button
			// sub timeline add rest for first recorded event to match trackAbsTime
			// in lastTweaked mode, this time is when param is changed
			//"prepare Start recording (clip)".debug(name);
			self[\isRecording] = true;
			self.startRecordingTime = self.tempoClock.nextTimeOnGrid(self.quant); 
			//[self.tempoClock.beats, self.startRecordingTime].debug("clip.startRecording: startRecordingTime");

			self.recordName = name;
			clip = self.getTimelineFromRecordSource;
			ev = clip.asPatternEvent;
			self.recordedEvent = ev;
			self.recordedClip = { clip };
			ev[\absTime] = self.trackAbsTime; // set event start at previous time on quant grid
			ev[\sustain] = 1/2;
			ev[\midinote] = self.trackIndex;
			self.recordedEventType = ev[\type];
			ev[\type] = \rest; // prevent playing while recording
			self.addEvent(ev);
			self.addRecordedEvent(ev);
			self.drawTask.source = {
				inf.do { arg idx;
					ev[\sustain] = self.tempoClock.beats - self.startRecordingTime;
					if(self.track.recordSource == \lastTweaked) {
						ev[\sustain] = self.tempoClock.beats - self.startRecordingTime + self.quant;
						//self.startRecordingTime = self.tempoClock.nextTimeOnGrid(self.quant) - self.quant; 
					};
					if(clip.eventLoop.recStartTime.notNil) {
						//Log(\Param).debug("changing start time %", ev[\absTime]);
						ev[\absTime] = self.timeline.eventList.startTime + clip.eventLoop.recStartTime - self.startRecordingTime - Server.default.latency;
						ev[\sustain] = self.tempoClock.beats - clip.eventLoop.recStartTime;
						if(self.track.recordSource == \lastTweaked) {
							ev[\sustain] = self.tempoClock.beats - self.startRecordingTime + self.quant;
							ev[\absTime] = self.timeline.eventList.startTime + clip.eventLoop.recStartTime - self.parentRecorder.startRecordingTime - Server.default.latency;
							//self.startRecordingTime = self.tempoClock.nextTimeOnGrid(self.quant) - self.quant; 
						};
					};
					//ev[\sustain].debug("recorded event sustain");
					ev.changed(\refresh);
					1.wait;
				}
			};
			//self.drawTask.quant = self.timeline.quant;
			self.drawTask.quant = 1;
			self.drawTask.play;

			clip.startRecording;

			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				self.changed(\isRecording);
				//Log(\Param).debug("Start recording: %: beats:%, quant:%, tempo:%", clip.label, self.tempoClock.beats, self.quant, self.tempoClock.tempo);
			});
		} {
			"Already recording".debug;
		}

	},
	stopRecording: { arg self;
		if(self.isRecording == true) {
			//"prepare Stop recording".debug(self.filename);
			self.recordedClip.stopRecording;
			self.changed(\userStoppedRecording);
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				//"Stop recording".debug(self.filename);
				self[\isRecording] = false;
				self.drawTask.stop;


				//[self.tempoClock.beats, self.startRecordingTime, self.tempoClock.beats - self.startRecordingTime].debug("clip.stopRecording: beats, startrectime, rec dur");
				self.recordedDuration = self.tempoClock.beats - self.startRecordingTime;
				if(self.track.recordSource == \lastTweaked) {
					self.recordedDuration = self.tempoClock.beats - self.startRecordingTime + self.quant;
					//self.startRecordingTime = self.tempoClock.nextTimeOnGrid(self.quant) - self.quant; 
				};

				self.recordedEvent[\sustain] = self.recordedDuration;
				//self.recordedEventType.debug("finalize recordedEvent");
				//self.recordedEventType.debug("end finalize recordedEvent");
				{
					// wait for end event to be added in recorded sub timeline before setting back type
					self.recordedEvent[\type] = self.recordedEventType;
				}.defer(Server.default.latency);
				self.changed(\stoppedRecording);
				self.changed(\isRecording);
				self.recordedEvent.changed(\refresh);
			});
		} {
			"Was not recording".debug;
		}
	},
));

ProtoTemplateDef(\ClipTimelineTrackRecorder_lastTweaked, (
	// created by ClipTimelineTrackRecorder_main as a replacement of ClipTimelineTrackRecorder_clip and under the hood, create a ClipTimelineTrackRecorder_clip for each tweaked param
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

	free: { arg self;
		if(self.isRecording) {
			self.stopRecording;
		};
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

	addRecordedEvent: { arg self, event;
		self.timeline.recordedEvents = self.timeline.recordedEvents.add(event);
	},

	startRecording: { arg self;
		var clip;
		var ev;
		var tl = self.timeline;
		var con;
		if(self.isRecording == false) {
			//"prepare Start recording (lastTweaked)".debug;
			//self.tempoClock.beats.debug("lastTweaked record started at");
			self[\isRecording] = true;

			if(self.clipList.isNil) {
				self.clipList = List.new;
			};
			con = SimpleController(Param).put(\lastTweaked, {
				var param = Param.lastTweaked;
				var prec;
				if(self.clipList.any({ arg tl; tl.lastTweaked == param }).not) {
					prec = ProtoTemplateDef(\ClipTimelineTrackRecorder_clip).new(tl,self.track, param);
					prec.parentRecorder = self.parentRecorder;
					//param.debug("ClipTimelineTrackRecorder_lastTweaked got signal");
					self.clipList.add(prec);
					prec.startRecording;
				};
			});
			self.lastTweakedController = con;
			self.changed(\isRecording);
			self.changed(\startedRecording);

			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				self.changed(\isRecording);
				//Log(\Param).debug("Start recording (lastTweaked): beats:%, quant:%, tempo:%", self.tempoClock.beats, self.quant, self.tempoClock.tempo);
				self.startRecordingTime = self.tempoClock.beats; // NOTE: doesnt account for latency but is used to compute duration so not a problem for the moment
			});
		} {
			"Already recording".debug;
		}

	},
	stopRecording: { arg self;
		if(self.isRecording == true) {
			//"prepare Stop recording".debug(self.filename);
			self.clipList.do { arg clip, idx;
				clip.stopRecording;
			};
			self.lastTweakedController.remove;
			self[\isRecording] = false;

			self.changed(\isRecording);
			self.changed(\stoppedRecording);
		} {
			"Was not recording".debug;
		}
	},
));
);

//////////////////// deprecated
//// rewrite ClipTimeline recorder to record several kind of tracks

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



////////////////////

if(currentEnvironment.isKindOf(ProxySpace).not) {

	~playerGroupRecorder = ProtoTemplateDef(\PlayerGroupRecorder);
	~paramRecorder = ProtoTemplateDef(\ParamRecorder);
}
