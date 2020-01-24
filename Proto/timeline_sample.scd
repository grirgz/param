
(

SynthDef(\SampleTimeline_player, { arg out=0, gate=1, amp=0.1, pan=0, freq=200, speed=1, trigger=1, loop=0, bufnum=0, startSeconds=0, gain=1;
	var sig;
	sig = PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum) * speed, trigger, startPos: (startSeconds*BufSampleRate.kr(bufnum)), doneAction:2, loop: loop);
	sig = sig * EnvGen.kr(\adsr.kr(Env.adsr(0.01,0.1,1,0.01)), gate, doneAction:2);
	sig = sig * gain;
	Out.ar(out, sig);
}, metadata:(specs:(
	startSeconds: ControlSpec(0,8,\lin, 0, 0),
))).add;

TagSpecDef(\SampleTimeline_player).addUnique(\SampleTimeline_player);

SynthDef(\SampleTimeline_player_rhpf, { arg out=0, gate=1, amp=0.1, pan=0, freq=200, speed=1, trigger=1, loop=0, bufnum=0, startSeconds=0, gain=1;
	var sig;
	sig = PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum) * speed, trigger, startPos: (startSeconds*BufSampleRate.kr(bufnum)), doneAction:2, loop: loop);
	sig = sig * EnvGen.kr(\adsr.kr(Env.adsr(0.01,0.1,1,0.01)), gate, doneAction:2);
	sig = RHPF.ar(sig, \hpf.kr(10), \rq.kr(0.5));
	sig = sig * gain;
	Out.ar(out, sig);
}, metadata:(specs:(
	startSeconds: ControlSpec(0,8,\lin, 0, 0),
))).add;

TagSpecDef(\SampleTimeline_player).addUnique(\SampleTimeline_player_rhpf);


SampleTimeline.proto = ProtoClass(( // can't use ProtoTemplateDef, clash with key and prAdd
	parent: ~eventTimeline,
	all: { PresetDictionary.new(\sampleTimeline) },
	eventType: \sampleTimeline,
	recordDirectory: "~/Musique/sc/cliprec/".standardizePath, // FIXME: don't hardcode
	fixedResampleCount: 500,
	enableFixedResampling: true,
	resampleRate: 10,

	new: { arg self, name, path;
		//"so many strange".debug;

		if(self.all[name].notNil) {
			self = self.all[name]
		} {
			self = self.deepCopy;
			self.addInstance(name).init(path);
		};
	
		self;
	},

	init: { arg self, path;
		// TODO: make accept a PlayerWrapperGroup as optional argument to handle recording
		self.basicInit;
		self.areasize = 10@4;
		//self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment

		//self.eventFactory = ~class_player_event_factory.new;
		//self.eventFactory.eventLoop = self.eventloop;
		//self.eventFactory.mode = \note;
		//self.eventFactory.playmode = \gate;
		//self.eventFactory.fullrecord = false;

		self.tracks_bypassed = Set.new;
		self.recorder = ProtoTemplateDef(\SampleTimelineRecorder).new(self);
		//"=============clipTimeline: end init".debug;

		if(path.notNil) {
			if(path.isKindOf(String) or: {path.isKindOf(Symbol)})  {
				self.buffer = BufDef(self.key.asSymbol, path.asString);
			};
			if(path.isKindOf(Buffer))  {
				self.buffer = path;
			};
		};

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

	postProxy: { arg self;
		self[\postProxy] = EventPatternProxy.new.source_(Pbind());
		self[\postProxy].addHalo(\instrument, \SampleTimeline_player);
		self[\postProxy];
	},

	postPatternPreset: { arg self;
		self.postProxy.envir.keys.asList.collect({ arg key;
			key -> self.postProxy.getVal(key)
		}).asEvent
	},

	postPatternPreset_: { arg self, ev;
		ev.keys.collect({ arg key;
			self.postProxy.setVal(key, ev[key])
		})
	},

	playerSynthDef: \SampleTimeline_player,

	playerSynthDef_: { arg self, instr;
		self[\playerSynthDef] = instr;
		self.postProxy.addHalo(\instrument, instr);
	},

	samplePattern: { arg self, startSeconds=0;
		var pat;
		startSeconds.debug("samplePattern");
		pat = self.postProxy <> Pmono(self.playerSynthDef,
			\bufnum, self.buffer ? 0,
			\startSeconds, startSeconds / self.tempoClock.tempo,
			\legato, 1,
			\dur, 1,
			\out, Pfunc({ self.outBus ? 0 }),
			\amp, 0.1,
		).trace;
	},

	cutPatternFunction: { arg self;
		{ arg sloop, offset, dur;
			sloop.cutPattern(
				Pn(self.samplePattern(offset+self.eventList.startTime), 1),
				offset,
				dur
			).embedInStream
		}
	},


	asPattern_old: { arg self, relStartTime, totalDur;
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
			self.frontProxy <> Pfindur(xtotalDur, Pembed(
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

	presetCompileString: { arg self;

		"%.eventList = %\n".format(self.refCompileString(self.key.asCompileString), self.eventList.presetCompileString)
		++
		"%.buffer = %;".format(self.refCompileString(self.key.asCompileString), self.buffer.asCompileString) 
	},

	bufferInfo: { arg self;
		self.eventList.extraData
	},

	bufferInfo_: { arg self, val;
		self.eventList.extraData = val;
	},

	buffer: { arg self;
		if(self.bufferInfo.notNil) {
			self.bufferInfo.buffer
		}
	},

	buffer_: { arg self, buffer;
		if(self.bufferInfo.isNil) {
			self.bufferInfo = ProtoClass(());
		};
		if(self.bufferInfo.buffer != buffer) {
			self.bufferInfo.buffer = buffer;
			self.bufferInfo.fileName = buffer.path;
			if(self.buffer.notNil) {
				//self.refreshBufferData;
			}
		}
	},

	fileName: { arg self;
		if(self.bufferInfo.notNil) {
			self.bufferInfo.fileName;
		}
	},

	fileName_: { arg self, fileName;
		if(self.bufferInfo.isNil) {
			self.bufferInfo = ProtoClass(()); // because numChanenls is reserved in EVent
		};
		self.bufferInfo.fileName = fileName;
	},

	askBufferData: { arg self;
		if(self.buffer.notNil) {
			if(self.bufferInfo.bufferData.isNil) {
				self.refreshBufferData;
			} {
				self.changed(\areasize);
				self.changed(\data);
			}
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
							var resamplePerSecond = self.resampleRate;
							var resampleCount;
							var numChannels = buffer.numChannels;
							var resample;
							if(self.enableFixedResampling) {
								resamplePerSecond = self.fixedResampleCount/numFrames*sampleRate
							};
							resampleCount = (numFrames/sampleRate*resamplePerSecond).asInteger;
							resample = bufferData.clump(numChannels).flop;
							0.01.wait;
							resample = resample.collect({ arg chan;
							   	chan.resamp1(resampleCount);
								0.05.wait;
						   	}).flop; 
							self.bufferInfo.enableFixedResampling = self.enableFixedResampling; 
							self.bufferInfo.fixedResampleCount = self.fixedResampleCount; 
							self.bufferInfo.bufferData = bufferData; 
							self.bufferInfo.resampledData = resample;
							self.bufferInfo.resampleRate = resamplePerSecond;
							self.bufferInfo.numChannels = numChannels;
							self.areasize = Point(self.areasize.x, self.bufferInfo.numChannels ? 2);
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
BufDef.paths = (BufDef.paths ++ [ SampleTimeline.proto.recordDirectory ]).as(Set).as(List);



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
			},
			Param(Message(self.timeline_model.recorder), \isRecording, XBoolSpec()).asButton("Rec"),
			Param(Message(self.timeline_model.recorder), \recBus, TagSpecDef(\BusDef)).asPopUpMenu,
			BasicButton.new.label_("Edit").action_({
				WindowDef(\EventPatternProxyEditor_ +++ self.timeline_model.key, { arg me, target, label, group;
					VLayout(
						HLayout (
							PlayerWrapper(target).label_(label).asView,
							Param(Message(self.timeline_model), \playerSynthDef, TagSpecDef(\SampleTimeline_player)).asPopUpMenu,
						),
						ParamGroupLayout.two_panes(group),
					);
				}).front(self.timeline_model.postProxy, self.timeline_model.label, self.timeline_model.postProxy.asParamGroup(nil, false, [\gate, \doneAction, \bufnum]));
			}),

			BasicButton.new.string_("...").mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				WindowDef(\SampleTimelineOverlayMenu).front(self.timeline_model, view, x, y)
			}).maxWidth_(20),

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
		self.locatorbar.keyDownAction = self.getTimelineKeyDownAction;
		self.ruler_x_beat.keyDownAction = self.getTimelineKeyDownAction;
		self.ruler_x_second.keyDownAction = self.getTimelineKeyDownAction;
		
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
);

WindowDef(\SampleTimelineOverlayMenu, { arg def, model, view, x=600, y=600;
	var inview, layout;
	[x, y].debug("xy");
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 200,100);
		win.endFrontAction = { win.close };
	};
	inview = View.new;


	layout = VLayout (
		HLayout(
			StaticText.new.string_("outBus:"),
			Param(
				Message(model),
				\outBus, 
				TagSpecDef(\BusDef),
				//MenuSpec([\default -> nil, \0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k) }) )
			).asPopUpMenu,
		),
		BasicButton.new.string_("Clipboard refString").action_({ 
			model.refCompileString.pbcopy;
		}),
		nil
	);
	//def.windowDo { arg win;
		//win.setInnerExtent(view.bounds.extent.min(20));
	//};
	inview.layout_(layout);
	
}).border_(false);
