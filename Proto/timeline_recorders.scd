
////////// recorders

// can record seqPlayerGroup and parPlayerGroup
~playerGroupRecorder = ProtoClass((
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
		self.controller.remove;
		self.controller = self.target.makeListener({ arg player, status, index;
			Log(\Param).debug("listener: status: %", status);
			if(status == \play) {
				player.recordedEvent = self.eventLoop.recordEvent(player.asPlayerEvent);
				player.recordedEvent[\clockTime] = self.tempoClock.beats;
				Log(\Param).debug("listener: start: recordedEvent:%", player.recordedEvent);
			} {
				if(status == \stop and: {player.recordedEvent.notNil}) {
					player.recordedEvent[\sustain] = self.tempoClock.beats - player.recordedEvent[\clockTime];
					Log(\Param).debug("listener: stop: recordedEvent:%", player.recordedEvent);
					player.recordedEvent.changed(\refresh);
				} {
					Log(\Param).debug("listener: don't know what to do with %: recordedEvent:%", status.asCompileString, player.recordedEvent);

				}
			}
		});
		self.eventLoop.startRec(false, self.quant ? Pdef.defaultQuant);
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
			ev.debug("recorder: process ev");
			if(ev.type == \start or: { ev.type == \end }) {
			} {
				[\kitIndex, \midinote, \dur, \sustain].do { arg key;
					lists[key] = lists[key].add(ev[key] ? 1);
				}
			};
		});

		lists.debug("recorder: getPresetCompileString: lists");

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
		ret = "XEventList.newFrom(%);".format(ret);
		ret;
	},

	presetCompileString: { arg self;
		var ret = "";
		ret = self.eventList.presetCompileString;
		ret;
	},
));

// can record Param
~paramRecorder = ProtoClass((
	parent: ~playerGroupRecorder,

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
		}
	},

));

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
		"ini".debug;
		self.recorder = Recorder(Server.default);
		self.drawTask = TaskProxy.new;
		self.tempoClock = TempoClock.default;
		self.tempoClock.debug("clock");
		self.timeline = timeline;

		self;
	},
	isRecording_: { arg self, val;
		if(val == true) {
			self.startRecording;
		} {
			self.stopRecording;
		}
	},



	startRecording: { arg self, numChannels=2;
		var name = "rec_%_%".format(self.timeline.key, Date.localtime.stamp);
		var clip;
		var ev;
		var tl = self.timeline;
		if(self.isRecording == false) {
			self[\isRecording] = true;
			"prepare Start recording".debug(name);

			self.recordName = name;
			self.startRecordingTime = self.tempoClock.beats;
			self.filename = "/tmp/%.wav".format(name);
			if(self.recordOnClipTimelineEnabled == true) {
				clip = SampleTimeline(name.asSymbol);
				ev = clip.asPatternEvent;
				self.recordedEvent = ev;
				self.recordedClip = clip;
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
				self.drawTask.play;
			} {
				// recordedClip is already the model
				self.recordedClip = self.timeline;
			};
			self.recordedClip.fileName = self.filename;
			self.recorder.prepareForRecord(path:self.filename, numChannels:numChannels);
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				"Start recording".debug(name);
				self.recorder.record(path:self.filename, numChannels:numChannels);
			});
		} {
			"Already recording".debug;
		}

	},
	stopRecording: { arg self;
		if(self.isRecording == true) {
			"prepare Stop recording".debug(self.filename);
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				"Stop recording".debug(self.filename);
				self[\isRecording] = false;

				self.recorder.stopRecording;
				self.recordedDuration = self.tempoClock.beats - self.startRecordingTime;

				if(self.recordOnClipTimelineEnabled == true) {
					self.drawTask.stop;
					self.recordedEvent[\sustain] = self.recordedDuration;
					self.recordedEvent.changed(\refresh);
				};
				{
					self.recordedClip.buffer = BufDef(self.filename.asSymbol);
					self.recordedClip.eventList.setEndPosition(self.recordedDuration);
				}.defer(2);
			});
		} {
			"Was not recording".debug;
		}
	},

));

// record in ClipTimeline directly
ProtoTemplateDef(\SampleClipTimelineRecorder, (
	parent: ProtoTemplateDef(\SampleTimelineRecorder),
	recordOnClipTimelineEnabled: true,
));
