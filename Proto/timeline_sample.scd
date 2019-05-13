
(

SynthDef(\SampleTimeline_player, { arg out=0, gate=1, amp=0.1, pan=0, freq=200, speed=1, trigger=1, loop=0, bufnum=0, startSeconds=0, gain=1;
	var sig;
	sig = PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum) * speed, trigger, startPos: (startSeconds*BufSampleRate.kr(bufnum)), doneAction:2, loop: loop);
	sig = sig * EnvGen.kr(\adsr.kr(Env.adsr(0.01,0.1,1,0.01)), gate, doneAction:2);
	sig = sig * gain;
	Out.ar(out, sig);
}).add;

ProtoTemplateDef(\SampleClipTimelineRecorder, (
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
	startRecording: { arg self;
		var name = "rec_%_%".format(self.timeline.key, Date.localtime.stamp);
		var clip;
		var ev;
		var tl = self.timeline;
		if(self.isRecording == false) {
			self[\isRecording] = true;
			"prepare Start recording".debug(name);

			clip = SampleTimeline(name.asSymbol);
			ev = clip.asPatternEvent;
			self.recordedEvent = ev;
			self.recordedClip = clip;
			self.recordName = name;
			ev[\absTime] = tl.selection_cursor.startPosition ? 0;
			ev[\sustain] = 1/2;
			ev[\midinote] = (tl.selection_cursor.startPoint !? _.y ? 0).trunc(tl.quant_ref.value.y);
			tl.eventList.addEvent(ev);
			tl.eventList.reorder;
			self.startRecordingTime = self.tempoClock.beats;
			self.drawTask.source = {
				inf.do { arg idx;
					1.wait;
					ev[\sustain] = idx + 1;
					ev.changed(\refresh);
				}
			};
			self.drawTask.play;
			self.filename = "/tmp/%.wav".format(name);
			self.recordedClip.fileName = self.filename;
			self.recorder.prepareForRecord(path:self.filename, numChannels:2);
			self.tempoClock.schedAbs(self.tempoClock.nextTimeOnGrid(self.quant), {
				"Start recording".debug(name);
				self.recorder.record(path:self.filename, numChannels:2);
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
				self.drawTask.stop;
				self.recordedDuration = self.tempoClock.beats - self.startRecordingTime;
				self.recordedEvent[\sustain] = self.recordedDuration;
				self.recordedEvent.changed(\refresh);
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

SampleTimeline.proto = ProtoClass(( // can't use ProtoTemplateDef, clash with key and prAdd
	parent: ~eventTimeline,
	all: { PresetDictionary.new(\sampleTimeline) },
	eventType: \sampleTimeline,
	recordDirectory: "~/Musique/sc/cliprec/".standardizePath, // FIXME: don't hardcode

	new: { arg self, name;
		//"so many strange".debug;

		if(self.all[name].notNil) {
			self = self.all[name]
		} {
			self = self.deepCopy;
			self.addInstance(name).init;
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
		self.selection_cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;
		self.tracks_bypassed = Set.new;
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

	keepRecordedFile: { arg self;
		var fileName = self.fileName;
		if(fileName.notNil && self.recordedFileIsKept.not) { // can't use ProtoTemplateDef, clash with key and prAdd
			var newfilename = self.recordDirectory +/+ PathName(fileName).fileName;
			File.mkdir(self.recordDirectory);
			File.copy(fileName, newfilename);
			self.fileName = newfilename;
			self.buffer = BufDef(newfilename.asSymbol);
		} {
			"Keep: Already kept or no file name".debug(self.fileName);
		}
		
	},

	recordedFileIsKept: { arg self;
		(self.fileName ? "").beginsWith(self.recordDirectory)
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

	samplePattern: { arg self, startSeconds=0;
		Pmono(\SampleTimeline_player,
			\bufnum, self.buffer ? 0,
			\startSeconds, startSeconds,
			\dur, 1,
			\amp, 0.1,
		);
	},

	asPattern: { arg self, relStartTime, totalDur;
		var pat;
		pat = Plazy({
			var eventlist = self.eventList;
			var xrelStartTime, xtotalDur;
			var xstartTime;

			xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			xtotalDur = totalDur.value ? eventlist.totalDur;
			xstartTime = eventlist.startTime;

			[xrelStartTime, xtotalDur, xstartTime,xrelStartTime+xstartTime ].debug("sampleTimeline.asPattern: plazy: relstart, totdur");
			if(xtotalDur <= 0) {
				"ERROR: totalDur is negative or zero".postln;
				xtotalDur = 2
			};

			self.eventloop.list.changed(\cursor, \play);
			Pfindur(xtotalDur, Pembed(
				Pn(self.samplePattern(xrelStartTime+xstartTime), 1),
				xrelStartTime
			));
		}).repeat(self.repeatCount);
		//self.proxy.source = pat;
		//self.proxy;
		pat
	},

	edit: { arg self;
		var x = ProtoTemplateDef(\SampleTimelineView).new(self);
		self.editor = { x };
		x;
	},

	asView: { arg self;
		ProtoTemplateDef(\SampleTimelineView).newView(self).makeLayout;
	},

	refCompileString: { arg self, name;
		if(name.isNil) {
			name = self.key.asCompileString
		};
		"SampleTimeline(%)".format(name)
	},

	buffer_: { arg self, buffer;
		
		self[\buffer] = buffer;
		if(self.buffer.notNil) {
			self.refreshBufferData;
		}
	},

	refreshBufferData: { arg self;
		{
			block { arg break;
				10.do {
					if(self.buffer.numFrames.isNil) {
						self.buffer.debug("waiting for buffer");
						1.wait;
					} {
						self.buffer.loadToFloatArray(0, -1, {|data| 
							var buffer = self.buffer;
							var sampleRate = buffer.sampleRate;
							var numFrames = buffer.numFrames;
							var bufferData = data;
							var resamplePerSecond = 30;
							var resampleCount = (numFrames/sampleRate*resamplePerSecond).asInteger;
							var numChannels = buffer.numChannels;
							var resample =bufferData.clump(numChannels).flop.collect(_.resamp1(resampleCount)).flop; 
							self.bufferData = bufferData; 
							self.resampledData = resample;
							self.resampleRate = resamplePerSecond;
							self.numChannels = numChannels;
							self.areasize = Point(self.areasize.x, self.numChannels ? 2);
							self.changed(\areasize);
							self.changed(\data);
							"loaded".debug;
						});
						break.value;
					}
				};
				self.buffer.debug("abort waiting for buffer")
			}

		}.fork;

	},
));
~sampleTimeline = SampleTimeline.proto;




ProtoTemplateDef(\SampleTimelineView, (
	parent: ~timelinePanelView,

	new: { arg self, timeline;
		//"so strange".debug;
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
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			self.make_quant_popup,
			if(self.timeline_model.recordedFileIsKept.not) {
				BasicButton.new.label_("Keep").action_({
					self.timeline_model.keepRecordedFile;
				});
			}
		);
		
		self.header_layout;

	},

	make_track_header: { arg self, timeline;
		//self.track_header = View.new;
		//self.ruler_y = MidinoteTimelineRulerView.new;
		//self.track_header = self.ruler_y;
		//self.ruler_y.mimicTimeline(timeline);
		//self.ruler_y.view.minWidth_(50);
		nil
	},

	timelineClass: { arg self;
		SampleTimelineView
	},

	clipEditorNew: { arg self, model;
		~clipEditor.new(model ? self.model, self.timeline_model.eventList);
	},

	makeXRuler: { arg self;
		var cursor_timeline;
		var locatorbar;
		var ruler_x_beat;
		var ruler_x_second;
		cursor_timeline = CursorTimelineView.new;
		~cursor_timeline = cursor_timeline;
		self.cursor_timeline = cursor_timeline;
		cursor_timeline.mapCursor(self.cursor);
		cursor_timeline.quant = self.quant_ref;



		ruler_x_beat = TimelineRulerView.new;
		ruler_x_beat.mapCursor(self.cursor);
		ruler_x_beat.quant = self.quant_ref;
		self.ruler_x_beat = ruler_x_beat;

		ruler_x_second = TimelineSecondRulerView.new;
		ruler_x_second.mapCursor(self.cursor);
		ruler_x_second.quant = self.quant_ref;
		self.ruler_x_second = ruler_x_second;

		locatorbar = TimelineLocatorBarView.new;
		self.locatorbar = locatorbar;

		locatorbar.quant = self.quant_ref;

		ruler_x_beat.mimicTimeline(self.timeline);
		ruler_x_second.mimicTimeline(self.timeline);
		locatorbar.mimicTimeline(self.timeline);
		cursor_timeline.mimicTimeline(self.timeline);

		self.locatorbar.view.minSize = Point(10,15);
		self.ruler_x_beat.view.minSize = Point(10,10);
		self.ruler_x_second.view.minSize = Point(10,10);

		//ruler_x_second.mygrid = { arg thi;
			//14.do { arg x;
				//Pen.line(
					//thi.secondPointToPixelPoint(Point(x,0)).debug("dd"++x),
					//thi.secondPointToPixelPoint(Point(x,20))
				//);
				//Pen.stroke;
			//}

		//};
		
		VLayout(
			locatorbar.view,
			self.ruler_x_second.view,
			self.ruler_x_beat.view,
		);
	},

	makeLayout: { arg self;
		var clipEditor;
		//"clipTimelinePanelView.makeLayout: start init".debug;

		self[\makeVelocityView] = {}; // disable velocity view
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
			[~timelinePanelView[\makeLayout].(self), stretch:1],
		);
		//"clipTimelinePanelView.makeLayout: part 2".debug;
		//"clipTimelinePanelView.makeLayout: part 3".debug;
		self.timeline.view.onChange(self.eventloop, \list, { 
			{
				self.refresh;
			}.defer
		});
		self.timeline.mapData(self.timeline_model);
		self.timeline_model.changed(\data);
		//"clipTimelinePanelView.makeLayout: end init".debug;
		self.timeline.keyDownAction = self.getTimelineKeyDownAction;
		self.layout;
	},
));
)
