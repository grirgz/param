
(

SynthDef(\SampleTimeline_player, { arg out=0, gate=1, amp=0.1, pan=0, freq=200, speed=1, trigger=1, loop=0, bufnum=0, startSeconds=0, gain=1, muteGain=1, parentGain=1;
	var sig;
	sig = PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum) * speed, trigger, startPos: (startSeconds*BufSampleRate.kr(bufnum)), doneAction:2, loop: loop);
	sig = sig * EnvGen.kr(\adsr.kr(Env.adsr(0.01,0.1,1,0.01)), gate, doneAction:2);
	sig = sig * gain;
	sig = sig * muteGain * parentGain;
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
	parent: ProtoClassDef(\EventTimeline),
	all: { PresetDictionary.new(\sampleTimeline) },
	eventType: \sampleTimeline,
	fixedResampleCount: 500,
	enableFixedResampling: true,
	enableWaveformView: true,
	resampleRate: 10,

	recordFolder: { arg self;
		(
			FileSystemProject.current !? _.recordFolder ?? {FileSystemProject.recordFolder ?? { "~/Musique/"  }};
		).standardizePath;
	},

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
		self.recorder = ProtoTemplateDef(\SampleTimelineRecorder).new(self) !? { arg x; {x} };
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
			var newfilename = self.recordFolder +/+ PathName(fileName).fileName;
			File.mkdir(self.recordFolder);
			File.copy(fileName, newfilename);
			self.fileName = newfilename;
			self.buffer = BufDef(newfilename.asSymbol);
		} {
			Log(\Param).info("Keep: Already kept or no file name: %", self.fileName);
		}
		
	},

	isRecordedFileKept: { arg self;
		(self.fileName ? "").beginsWith(self.recordFolder)
	},

	recordedFileIsKept: { arg self;
		self.isRecordedFileKept;
	},

	postProxy: { arg self;
		self[\postProxy] = EventPatternProxy.new.source_(Pbind());
		self[\postProxy].addHalo(\instrument, \SampleTimeline_player);
		self[\postProxy];
	},

	postPatternPreset: { arg self;
		if(self.postProxy.envir.notNil) {
			self.postProxy.envir.keys.asList.collect({ arg key;
				key -> self.postProxy.getVal(key)
			}).asEvent
		} {
			()
		}
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

	getEmptyBuffer: { arg self;
		BufDef(\empty, Server.default.sampleRate, 2);
	},

	samplePattern: { arg self, startBeats=0, parentEvent, parentTimeline;
		var pat;
		[startBeats, startBeats / self.tempoClock.tempo].debug("samplePattern: startBeats, startSeconds");
		pat = self.postProxy <> Pmono(self.playerSynthDef,
			\bufnum, self.buffer ?? { self.getEmptyBuffer },
			\startSeconds, startBeats / self.tempoClock.tempo,
			\legato, 1,
			\dur, 1,
			\parentGain, Pfunc({ 
				if(parentEvent.notNil) {
					parentEvent.gain ? 1 
				} {
					1
				};
			}),
			\muteGain, Pfunc({ 
				if(parentTimeline.notNil) {
					parentTimeline.is_track_bypassed(parentEvent.use({ ~midinote.value.asInteger })).not.asInteger 
				} {
					1
				};
			}),
			\out, Pfunc({ parentEvent !? _.outBus ? self.outBus ? 0 }), // since \out is never nil in an event because default to 0, i must use another key
		);
	},

	cutPatternFunction: { arg self, parentEvent, parentTimeline;
		{ arg sloop, offset, dur;
			sloop.cutPatternFadeOut(
				Pn(self.samplePattern(offset+self.eventList.startTime, parentEvent, parentTimeline), 1),
				0, //offset,
				dur,
				self.fadeOut
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

			//[xrelStartTime, xtotalDur, xstartTime,xrelStartTime+xstartTime ].debug("sampleTimeline.asPattern: plazy: relstart, totdur");
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

	asPreview: { arg self;
		TimelinePreview_Sample.new.mapData(self).mapEventList(self.eventList).view.mouseDownAction_({ 
			self.edit;
		}).fixedHeight_(40);
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
		var str;

		str = "%.eventList = %\n".format(self.refCompileString(self.key.asCompileString), self.eventList.presetCompileString)
		++
		"%.buffer = %;\n".format(self.refCompileString(self.key.asCompileString), BufDef.bufferCompileString(self.buffer)) 
		++
		"%.postPatternPreset = %;\n".format(self.refCompileString(self.key.asCompileString), self.postPatternPreset.asCompileString);
		if(self.fadeOut.notNil) {
			str = str ++
			"%.fadeOut = %;\n".format(self.refCompileString(self.key.asCompileString), self.fadeOut.asCompileString) 
		};
		str;
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
		if(buffer.notNil) {
			if(self.bufferInfo.isNil) {
				self.bufferInfo = ProtoClass(());
			};
			if(self.bufferInfo.buffer != buffer) {
				self.bufferInfo = ProtoClass(());
				self.bufferInfo.buffer = buffer;
				self.bufferInfo.fileName = buffer.path;
				// NOTE: setting end position when setting buffer break previously set position
				//		like when loading a saved SampleTimeline
				//{
					//try {
						//self.eventList.setEndPosition(buffer.duration * self.tempoClock.tempo);
					//} { arg error;
						//Log(\Param).debug("Warning: Can't get duration of buffer %", buffer);
					//};
				//}.defer(1);
				if(self.buffer.notNil) {
					// do not refresh at setting because when no gui, no need to compute the data
					// data should be computed when gui ask for it
					// when data is ready, a signal is sent to gui to refresh it
					//self.refreshBufferData;
					self.changed(\data);
				}
			}
		} {
			self.bufferInfo = ProtoClass(());
		}
	},

	setDurationToBufferDuration: { arg self;
		try {
			self.eventList.setStartPosition(0);
			self.eventList.setEndPosition(self.buffer.duration * self.tempoClock.tempo);
		} { arg error;
			Log(\Param).debug("Warning: Can't get duration of buffer %, error: %", self.buffer, error);
		};
	},

	resizeBufferInFrames: { arg self, newsize;
		if(self.bufferInfo.notNil) {
			if(self.buffer.numFrames != newsize) {
				var newbuf;
				newbuf = Buffer.alloc(Server.default, newsize, self.buffer.numChannels);
				self.buffer.debug("now getting slice");
				self.buffer.loadToFloatArray(0, newsize, { arg col;
					col.size.debug("got colsize");
					newbuf.debug("now loading in newbuf");
					newbuf.loadCollection(col, action: { arg xbuf;
						newbuf.debug("newbuf loaded slice");
						xbuf.debug("xbuf loaded slice");
						{
							newbuf.debug("newbuf loaded slice2");
							xbuf.debug("xbuf loaded slice2");
							//~newbuf = newbuf;
							self.bufferInfo.bufferHistory = self.bufferInfo.bufferHistory.add(self.buffer);
							self.buffer = newbuf;
							self.changed(\bufferNumFrames);
						}.defer
					})
				});
			}
		}
	},

	bufferNumFrames_: { arg self, val;
		self.resizeBufferInFrames(val);
	},

	bufferNumFrames: { arg self;
		if(self.buffer.notNil) {
			self.buffer.numFrames;
		}
	},

	bufferNumBeats: { arg self;
		if(self.buffer.notNil) {
			self.buffer.numFrames / self.buffer.sampleRate * TempoClock.default.tempo;
		}
	},

	bufferNumBeats_: { arg self, val;
		if(self.buffer.notNil) {
			self.resizeBufferInFrames(
				val / TempoClock.default.tempo * self.buffer.sampleRate;
			);
			self.changed(\bufferNumBeats);
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

	enableWaveformView_: { arg self, val;
		self[\enableWaveformView] = val;
		self.changed(\data);
	},

	askBufferData: { arg self;
		if(self.buffer.notNil and: { self.enableWaveformView == true }) {
			if(self.bufferInfo.waveformImage.isNil) {
				self.refreshBufferData;
			} {
				self.changed(\areasize);
				self.changed(\data);
			}
		}
	},

	computeWaveformImage: { arg self;
		var buf = self.buffer;
		//var size = buf.numFrames/1200@800; // waveform resolution
		var size = Point(( buf.numFrames/Server.default.sampleRate*300 ).asInteger, buf.numChannels * 100);
		buf.loadToFloatArray(0,-1,{ arg data;
			Task({
				if(data.size > 0) {
					var img = Image.new(size);
					var yzoom = 1;
					var chanspacefactor = 2;
					var chunksize = 16;
					var lastpoint;
					var drawChannel = { arg chandata, frameOffset, yoffset;
						var dsize = chandata.size;
						var totalFrames = buf.numFrames;
						var chanysize = size.y/buf.numChannels;
						//[chandata.size, frameOffset, yoffset].debug("chandata");
						Pen.moveTo(Point(( frameOffset )/totalFrames * size.x, (yoffset + 1) *chanysize  /2  ));
						chandata.do { arg item, idx;
							var point;
							//[idx, frameOffset, totalFrames, size.x, ( idx+frameOffset ),( idx+frameOffset )/totalFrames, ( idx+frameOffset )/totalFrames * size.x ].debug("chandata point");
							point = Point(( idx+frameOffset )/totalFrames * size.x, (item * yzoom + yoffset + 1) *chanysize  /2);
							Pen.lineTo(point);
						};
						Pen.stroke;
					};
					Log(\Param).debug("SampleTimelineView: computeWaveformImage: start drawing waveform for %", buf);
					//[buf.numFrames, buf.numFrames/chunksize].debug("numFrames");
					( buf.numFrames/chunksize ).asInteger.do { arg chunkidx;
						// draw the image by chunks of 16 values
						// chunkdata has 32 values for a 2 channels buffer with chunksize 16
						var chunkdata = data[chunkidx*chunksize*buf.numChannels..( chunkidx+1 )*chunksize*buf.numChannels -1];
						//[chunkdata.size, chunkdata ].debug("chunkdata");

						img.draw({
							chunkdata.clump(buf.numChannels).flop.do { arg chandata, idx;
								drawChannel.(chandata, chunkidx*chunksize, idx * chanspacefactor)
							};
						});
						0.0000000001.wait;

					};
					if(self.bufferInfo.waveformImage.notNil) {
						self.bufferInfo.waveformImage.free;
					};
					self.bufferInfo.waveformImage = img;
					self.bufferInfo.bufferData = data; 
					self.bufferInfo.numChannels = buf.numChannels;
					self.areasize = Point(self.areasize.x, self.bufferInfo.numChannels ? 2);
					self.changed(\areasize);
					self.changed(\data)
				} {
					// FIXME: buffer seems to be loaded two times and the second time it is empty
					// this is why i check size
					Log(\Param).debug("Error: buffer is empty %", buf);
				};
			}).play(AppClock)
		})
		
	},

	computeWaveformImage_old: { arg self;
		var buf = self.buffer;
		//var size = buf.numFrames/1200@800; // waveform resolution
		var size = Point(( buf.numFrames/Server.default.sampleRate*300 ).asInteger, buf.numChannels * 100);
		buf.loadToFloatArray(0,-1,{ arg data;
			{
				var img;
				if(data.size > 0) {
					img = Image.new(size);
					img.draw({
						var yzoom = 1.0;
						var chanspacefactor = 2.0;
						var drawChannel = { arg chandata, yoffset;
							var dsize = chandata.size;
							var chanysize = size.y/buf.numChannels;
							Log(\Param).debug("drawChannel: chandata %", chandata[0..10]);
							chandata.do { arg item, idx;
								var point = Point(idx/dsize * size.x, (item * yzoom + yoffset + 1) *chanysize  /2);
								Pen.lineTo(point);
							};
							Pen.stroke;
						};
						data.clump(buf.numChannels).flop.do { arg chandata, idx;
							drawChannel.(chandata, idx * chanspacefactor)
						};
					});
					//~img = img; // debug
					if(self.bufferInfo.waveformImage.notNil) {
						self.bufferInfo.waveformImage.free;
					};
					self.bufferInfo.waveformImage = img;
					self.bufferInfo.bufferData = data; 
					self.bufferInfo.numChannels = buf.numChannels;
					self.areasize = Point(self.areasize.x, self.bufferInfo.numChannels ? 2);
					self.changed(\areasize);
					self.changed(\data)
				} {
					// FIXME: buffer seems to be loaded two times and the second time it is empty
					// this is why i check size
					Log(\Param).debug("Error: buffer is empty %", buf);
				};
				//img.plot;
			}.defer;
		})
		
	},

	computeResampledData: { arg self;
		// not used anymore, computeWaveformImage use raw data
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
				0.05.wait;
				chan.resamp1(resampleCount);
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
			Log(\Param).debug("refreshBufferData: buffer loaded");
		});
	},

	refreshBufferData: { arg self;
		{
			block { arg break;
				10.do {
					if(self.buffer.numFrames.isNil) {
						Log(\Param).debug("waiting for buffer %", self.buffer);
						1.wait;
					} {
						//self.computeResampledData;
						self.computeWaveformImage;
						break.value;
					}
				};
				Log(\Param).debug("abort waiting for buffer %", self.buffer)
			}

		}.fork;

	},
));
BufDef.addPath(SampleTimeline.proto.recordFolder);


//////// GUI

ProtoTemplateDef(\SampleTimelineView, (
	parent: ProtoTemplateDef(\TimelinePanelView),

	new: { arg self, timeline;
		//"so strange".debug;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		self.timeline_model = { timeline };
		self.areasize = self.timeline_model.areasize;
		self.quant_ref = { arg self; self.timeline_model.quant_ref };
		
		self.makeWindow;
	
		self;
	},

	newView: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
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

	makeBottomPanelView: { },

	make_main_context_menu: { arg self;
		var model = self.timeline_model;
		var viewmodel = self;
		Menu(
			MenuAction("Edit buffer", { 
				WindowDef(\BufferEditor).front(model.bufferInfo.buffer);
			}),
			MenuAction("Edit with audacity", { 
				{
					var tmpname = "/tmp/SampleTimeline_audacity.wav";
					Log(\Param).debug("Writing tmp file %...", tmpname);
					model.buffer.write(tmpname);
					1.wait;
					Log(\Param).debug("Launching audacity...");
					"audacity %".format(tmpname).unixCmd { arg res, pid;
						Buffer.read(Server.default, tmpname, action: { arg buf;
							model.buffer = buf;
						});
						Log(\Param).info("edit done");
					};
				}.fork;
			}),
			MenuAction("Export selection to buffer", { 
				viewmodel.exportSelectionToBufferEditor;
			}),

			MenuAction.separator,
			MenuAction("Load audio file", { 
				WindowDef(\filedialog_sample).front(nil, { arg path;
					viewmodel.timeline_model.buffer = BufDef(path);
				})
			}),

			MenuAction.separator,
			MenuAction("Clipboard refString", { 
				model.refCompileString.pbcopy;
			}),

			MenuAction.separator,
			Param(
				Message(model),
				\outBus, 
				TagSpecDef(\BusDef),
				//MenuSpec([\default -> nil, \0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k) }) )
			).asMenu("outBus"),
			Param(Message(model), \enableWaveformView, ParamBoolSpec()).asMenuAction("Display waveform"),
			CustomViewAction(View.new.layout_(HLayout (
				StaticText.new.string_("Buf length (beats):"),
				Param(Message(model), \bufferNumBeats, ControlSpec(1/8,512,\lin)).asTextField,
			))),
			CustomViewAction(
				View.new.layout_(HLayout(StaticText.new.string_(
					if(model.buffer.notNil) {
						"Path: %".format(model.buffer.path)
					} {
						"No buffer"
					};
				))),
			)
		)
	},

	make_header: { arg self;
		self.counterView = StaticText.new.fixedWidth_(50).font_(Font.default.copy.size_(9));
		//self.counterView = StaticText.new;
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			self.make_quant_popup,
			if(self.timeline_model.recordedFileIsKept.not) {
				BasicButton.new.label_("Keep").action_({
					self.timeline_model.keepRecordedFile;
				});
			},
			RecordButton(self.timeline_model.recorder, "Rec"),
			Param(Message(self.timeline_model.recorder), \recBus, TagSpecDef(\BusDef)).asPopUpMenu,
			BasicButton.new.label_("Edit").action_({
				WindowDef(\EventPatternProxyEditor_ +++ self.timeline_model.key, { arg me, target, label, group;
					VLayout(
						HLayout (
							PlayerWrapper(target).label_(label).asView,
							Param(Message(self.timeline_model), \playerSynthDef, TagSpecDef(\SampleTimeline_player)).asPopUpMenu,
						),
						Param(Message(self.timeline_model), \fadeOut, ControlSpec(0,1,\lin)).asView,
						ParamGroupLayout.two_panes(group),
					);
				}).front(self.timeline_model.postProxy, self.timeline_model.label, self.timeline_model.postProxy.asParamGroup(nil, false, [\gate, \doneAction, \bufnum]));
			}),
			self.counterView,

			BasicButton.new.string_("...").mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				self.make_main_context_menu.front;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				//WindowDef(\SampleTimelineOverlayMenu).front(self, view, x, y)
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

	makeXRuler: { arg self;
		var cursor_timeline;
		var locatorbar;
		var ruler_x_beat;
		var ruler_x_second;
		cursor_timeline = CursorTimelineView.new;
		//~cursor_timeline = cursor_timeline;
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
			[ProtoTemplateDef(\TimelinePanelView)[\makeLayout].(self), stretch:1],
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
		self.timeline.eventFactory = { nil }; // disable event creation
		self.timeline.keyDownAction = self.getTimelineKeyDownAction;
		self.timeline.mouseMoveAction = {|me, px, py, mod|
			var scursor = self.timeline_model.selection_cursor;
			var cursor = self.timeline_model.cursor;
			var lenbeat = 0, lensec;
			var prec = 3;
			var str;

			if(scursor.endPosition.notNil) {
				lenbeat = scursor.endPosition - (scursor.startPosition ? 0 );
			};

			// NOTE: make no sense to use cursor in mouseMoveAction
			//if(lenbeat == 0) {
				//if(cursor.endPosition.isNil) {
					//lenbeat = 0;
				//} {
					//lenbeat = ( cursor.endPosition ? 0 ) - ( cursor.startPosition ? 0 );
				//}
			//};
			lensec = lenbeat/self.timeline_model.tempoClock.tempo;
		   	str = "%b\n%:%s".format(lenbeat.asStringPrec(prec), ( lensec / 60 ).asInteger, ( lensec % 60 ).asStringPrec(prec));
			self.counterView.string_(str)
		};
		self.layout;
	},

	copyAction: { arg self;
		var selnodes;
		var cursor = self.timeline_model.selection_cursor;
		var startpos, endpos;
		var buf = self.timeline_model.buffer;
		var model = self.timeline_model;
		//[ cursor.startPosition, cursor.endPosition ].debug("copy start end");

		buf.loadToFloatArray(action:{ arg arr;
			startpos = cursor.startPosition/model.tempoClock.tempo * buf.sampleRate * buf.numChannels;
			startpos = startpos.round.asInteger;
			endpos = cursor.endPosition/model.tempoClock.tempo * buf.sampleRate * buf.numChannels;
			endpos = endpos.round.asInteger;
			//"ém1".debug;
			ParamViewToolBox.clipboard.sampleTimeline = arr[startpos..endpos];
		})

	},

	deleteSelectedNodesHook: { arg self;
		{ arg tl;
			var cursor = self.timeline_model.selection_cursor;
			var startpos, endpos;
			var buf = self.timeline_model.buffer;
			var model = self.timeline_model;

			buf.loadToFloatArray(action:{ arg arr;
				startpos = cursor.startPosition/model.tempoClock.tempo * buf.sampleRate * buf.numChannels;
				startpos = startpos.round.asInteger;
				endpos = cursor.endPosition/model.tempoClock.tempo * buf.sampleRate * buf.numChannels;
				endpos = endpos.round.asInteger;
				arr[startpos..endpos] = 0;
				//"ém1".debug;
				buf.loadCollection(arr, action:{
					//model.bufferTempPath = nil;
					//model.changed(\buffer);
					//model.isModified = true;
					model.refreshBufferData;
					Log(\Param).info("erasing done");
				})
			})
		}
	},

	splitAction: { arg self;
		self.reverseAction;
	},

	reverseAction: { arg self;
		var cursor = self.timeline_model.selection_cursor;
		var startpos, endpos;
		var buf = self.timeline_model.buffer;
		var model = self.timeline_model;
		var slice;

		buf.loadToFloatArray(action:{ arg arr;
			startpos = cursor.startPosition/model.tempoClock.tempo * buf.sampleRate * buf.numChannels;
			startpos = startpos.round.asInteger;
			endpos = cursor.endPosition/model.tempoClock.tempo * buf.sampleRate * buf.numChannels;
			endpos = endpos.round.asInteger;
			slice = arr[startpos..endpos];
			slice = slice.reverse;
			arr.overWrite(slice, startpos);
			//"ém1".debug;
			buf.loadCollection(arr, action:{
				//model.bufferTempPath = nil;
				//model.changed(\buffer);
				//model.isModified = true;
				model.refreshBufferData;
				Log(\Param).info("reverse done");
			})
		})
	},

	pasteAction: { arg self;
		if(ParamViewToolBox.clipboard.sampleTimeline.notNil) {
			var cursor = self.timeline_model.selection_cursor;
			var startpos, endpos;
			var buf = self.timeline_model.buffer;
			var model = self.timeline_model;

			buf.loadToFloatArray(action:{ arg arr;
				startpos = cursor.startPosition/model.tempoClock.tempo * buf.sampleRate * buf.numChannels;
				startpos = startpos.round.asInteger;
				endpos = cursor.endPosition/model.tempoClock.tempo * buf.sampleRate * buf.numChannels;
				endpos = endpos.round.asInteger;
				arr.overWrite(ParamViewToolBox.clipboard.sampleTimeline, startpos);
				//"ém1".debug;
				buf.loadCollection(arr, action:{
					//model.bufferTempPath = nil;
					//model.changed(\buffer);
					//model.isModified = true;
					model.refreshBufferData;
					Log(\Param).info("pasting done");
				})
			})
		}
	},

	exportSelectionToBufferEditor: { arg self;
		var model = self.timeline_model;
		var scursor = model.selection_cursor;
		var cursor = model.cursor;
		var startpos, endpos;
		var exportedFrameCount;
		var newbuf;
		var buf = model.buffer;
		if(scursor.startPosition == scursor.endPosition) { // when no selection
			scursor = cursor; // use preview cursor instead
		};
		startpos = scursor.startPosition ? cursor.startPosition;
		endpos = scursor.endPosition ? cursor.endPosition;
		if(startpos.notNil) {

			startpos = startpos/model.tempoClock.tempo * buf.sampleRate;
			startpos = startpos.round.asInteger;
			endpos = endpos/model.tempoClock.tempo * buf.sampleRate;
			endpos = endpos.round.asInteger;
			exportedFrameCount = endpos - startpos;

			//exportedFrameCount.debug("exportedFrameCount");
			newbuf = Buffer.alloc(Server.default, exportedFrameCount+1, model.buffer.numChannels);
			//model.buffer.debug("now getting slice");
			model.buffer.loadToFloatArray(startpos, exportedFrameCount, { arg col;
				//col.size.debug("got colsize");
				//newbuf.debug("now loading in newbuf");
				newbuf.loadCollection(col, action: { arg xbuf;
					//newbuf.debug("newbuf loaded slice");
					//xbuf.debug("xbuf loaded slice");
					{
						//newbuf.debug("newbuf loaded slice2");
						//xbuf.debug("xbuf loaded slice2");
						//~newbuf = newbuf;
						WindowDef("%_%".format("BufferEditor", 100000000.rand).asSymbol, WindowDef(\BufferEditor)).front(newbuf);
					}.defer
				})
			});
		} {
			Log(\Param).info("No selection");
		}
	},

));
);

WindowDef(\SampleTimelineOverlayMenu, { arg def, viewmodel, view, x=600, y=600;
	var inview, layout;
	var model = viewmodel.timeline_model;
	//[x, y].debug("xy");
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
		BasicButton.new.string_("Load audio file").action_({ 
			WindowDef(\filedialog_sample).front(nil, { arg path;
				viewmodel.timeline_model.buffer = BufDef(path);
			})
		}),
		Param(Message(model), \enableWaveformView, ParamBoolSpec()).asButton("Display waveform"),
		HLayout (
			StaticText.new.string_("Size in beats:"),
			Param(Message(model), \bufferNumBeats, ControlSpec(1/8,512,\lin)).asTextField,
		),
		BasicButton.new.string_("Clipboard refString").action_({ 
			model.refCompileString.pbcopy;
		}),
		BasicButton.new.string_("Edit buffer").action_({ 
			WindowDef(\BufferEditor).front(model.bufferInfo.buffer);
		}),
		BasicButton.new.string_("Edit with audacity").action_({ 
			{
				var tmpname = "/tmp/SampleTimeline_audacity.wav";
				Log(\Param).debug("Writing tmp file %...", tmpname);
				model.buffer.write(tmpname);
				1.wait;
				Log(\Param).debug("Launching audacity...");
				"audacity %".format(tmpname).unixCmd { arg res, pid;
					Buffer.read(Server.default, tmpname, action: { arg buf;
						model.buffer = buf;
					});
					Log(\Param).info("edit done");
				};
			}.fork;
		}),
		BasicButton.new.string_("Export selection to buffer").action_({ 
			viewmodel.exportSelectionToBufferEditor;
		}),
		StaticText.new.string_(
			if(model.buffer.notNil) {
				"Path: %".format(model.buffer.path)
			} {
				"No buffer"
			};
		),
		nil
	);
	//def.windowDo { arg win;
		//win.setInnerExtent(view.bounds.extent.min(20));
	//};
	inview.layout_(layout);
	
}).border_(false);



if(currentEnvironment.isKindOf(ProxySpace).not) {

	~sampleTimeline = SampleTimeline.proto;
}
