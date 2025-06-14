
////////////////////
///// custom tracks


//////// PlayerWrapper

(
TrackTemplateDef(\PlayerWrapperTrack, (
	parent: TrackTemplateDef(\CustomTrack),
	childClass: { TrackTemplateDef(\PlayerWrapperTrackPart) },
	editInstrument: { arg self;
		Log(\Param).debug("No GUI for PlayerWrapperTrack");
	},
	makeScore: { arg self;
		//self.isEmpty = false;
	},
));
TrackTemplateDef(\PlayerWrapperTrackPart, (
	parent: TrackTemplateDef(\CustomTrackPart),
	selectSourceDialog: { arg self;
		TrackTemplateDef(\PlayerTrackPart)[\selectSourceDialog].value(self, {
			self.parentTrack.parentTrack.changed(\tracklist);
		});
	},

	asPreview: { arg self;
		StaticText.new.string_(self.proxy.target.asCompileString).mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(buttonNumber == 1) {
				var res;
				var grid = self.parentTrack.parentTrack;
				if(grid.resources.notNil) {
					res = grid.resources.includes(self.proxy.target);
				} {
					res = false;
				};
				Menu(
					*[
						MenuAction("Select player...", {
							self.selectSourceDialog;
						}),
						if(self.isEmpty.not) {
							[
								MenuAction("Edit", {
									self.edit;
								}),
								MenuAction("Add to resources", {
									grid.addResource(self.proxy.target);
								}).checkable_(true).checked_(res),
							]
						},
					].flat.select(_.notNil)
				).front;
			} {
				self.edit;
			};
		})
		
	},

	makeScore: { arg self;
		self.selectSourceDialog;
		
	},

	scorePresetCompileString: { arg self;
		// should not save Player presetCompileString, add it to resources instead
		var str;
		str = List.new;
		str.add( "%.source = %;".format(self.refCompileString, self.source.asCompileString));
		str.join($\n);
	},

));

TagSpecDef(\CustomTrackTemplate).addUnique(\PlayerWrapperTrack -> TrackTemplateDef(\PlayerWrapperTrack));
);



//////// Pbindef
(
TrackTemplateDef(\PbindefTrack, (
	parent: TrackTemplateDef(\CustomTrack),
	childClass: { TrackTemplateDef(\PbindefTrackPart) },
	trackEditor: { arg self;
		WindowDef(\PbindefEditor);
	},
	editInstrument: { arg self;
		WindowDef(\PbindefEditor).front(self.score);
	},
	scorePresetCompileString: { arg self;
		var str;
		str = List.new;
		str.add( "%.score = %".format(self.refCompileString, ParamGroup.getPbindefCompileString(self.score)) );
		str.join($\n);
	},
	outBus_: { arg self, outBus;
		self[\outBus] = outBus;
		Param(self.score, \out -> \source).set(outBus);
		self.changed(\outBus);
	},
	makeScore: { arg self;
		self[\score] = Pdef(self.trackKey); // first create Pdef to avoid asCompileString showing all keys
		Pbindef(self.trackKey, \instrument, \default);
		//self.isEmpty = false;
	},
));
TrackTemplateDef(\PbindefTrackPart, (
	parent: TrackTemplateDef(\CustomTrackPart),
	scorePresetCompileString: { arg self;
		var str;
		str = List.new;
		str.add( "%.score = %".format(self.refCompileString, ParamGroup.getPbindefCompileString(self.score)) );
		str.join($\n);
	},
	edit: { arg self;
		WindowDef(\PbindefEditor).front(self.score);
	},
	score_: { arg self, val;
		if(val.notNil) {
			self[\score] = val;
			self[\pchain] = Pdef( (self.trackKey++\_chain).asSymbol, self.score <> self.parentTrack.score );
			self[\proxy] = PlayerWrapper(self.pchain);
		} {
			self[\score] = nil;
			self[\pchain] = nil;
			self[\proxy] = nil;
		};
	},

	asPreview: { arg self;
		var tkeys, val, label;
		//View.new.layout_()
		//StaticText.new.string_(self.score.source.get(\note))
		tkeys = [\note, \degree, \freq];
		tkeys.do { arg key, idx;
			val = val ?? {
				label = key;
				self.score.source.at(key);
			}
		};
		StaticText.new.string_("%: %".format(label, val.source.asCompileString)).mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			//if(buttonNumber == 1) {
				self.edit;
			//};
		});
	},

	cloneScore: { arg self;
		var newscore;
		newscore = Pbindef(self.trackKey).source_(PbindProxy());
		self.score.source.pairs.clump(2).collect { arg pair, idx;
			//[ pair, pair[1].source.copy ].debug("clone");
			newscore.source.set(pair[0], pair[1].source.copy);
		};
		//~newscore = newscore;
		newscore.envir = self.score.envir.copy;
		newscore;
	},

	makeScore: { arg self;
		self.score = Pdef(self.trackKey);
		Pbindef(self.trackKey).source_(PbindProxy());
		//self.proxy.debug("makeScore proxy end");
	},
	selectSourceDialog: { arg self;
		TrackTemplateDef(\playerwrapper)[\selectSourceDialog].value(self);
	},

));

TagSpecDef(\CustomTrackTemplate).addUnique(\PbindefTrack -> TrackTemplateDef(\PbindefTrack));
);


//////// NoteTimeline

(
TrackTemplateDef(\NoteTimelineTrack, (
	parent: TrackTemplateDef(\CustomTrack),
	childClass: { TrackTemplateDef(\NoteTimelineTrackPart) },

	asParamGroup: { arg self;
		self.source.asParamGroup;
	},
	trackEditor: { arg self;
		WindowDef(\PdefInstrumentEditor);
	},


	recorderView: { arg self;
		HLayout (
			Param(Message(self), \recordArmed, ParamBoolSpec()).asButton("●").maxWidth_(20),
			Param(Message(self), \inputController, ControllerDef.getTagSpec(\piano)).asPopUpMenu.minWidth_(5),
		).margins_(0).spacing_(0)
	},

	scorePresetCompileString: { arg self;
		var str;
		str = List.new;
		str.add( "%.source = %;".format(self.refCompileString, self.source.asCompileString));
		//str.add( "%.playerSynthDef = %;".format(self.refCompileString, self.playerSynthDef.asCompileString));
		str.add( PlayerWrapper(self.source).presetCompileString );
		str.join($\n);
	},

	cloneScore: { arg self;
		// WARN: CustomTrack cloneScore should assign score while CustomTrackPart cloneScore should return score
		self[\source] = ProtoTemplateDef(\SynthDefPresetBank).clonePdef("%.instr".format(self.trackKey).asSymbol, self.source);
	},


	playerSynthDef_: { arg self, instr;
		// not used anymore, editor use PbindefEditor to change instrument
		// TagSpecDef(\SynthDef) values are SynthDesc, not SynthDef key
		//Log(\Param).debug("TrackInstrument_NoteTimelineTrack set playerSynthDef %", instr);
		self[\playerSynthDef] = instr;
		Pbindef(self.source.key, \instrument, instr);
		//self.source.addHalo(\instrument, instr);
		self.changed(\playerSynthDef);
	},
	makeScore: { arg self;
		self[\source] = Pdef("%.instr".format(self.trackKey).asSymbol, Pbind());
		self.playerSynthDef = \default;
		//self.isEmpty = false;
	},
));
TrackTemplateDef(\NoteTimelineTrackPart, (
	parent: TrackTemplateDef(\CustomTrackPart),
	edit: { arg self;
		self.score.edit;
	},

	startRecordingNewScore: { arg self;
		self.makeScore;
		self.score.startRecording;
	},

	stopRecording: { arg self;
		if(self.score.notNil) {
			self.score.stopRecording;
		}
	},

	cloneScore: { arg self;
		var newscore;
		newscore = NoteTimeline(self.name+++\score, self.parentTrack.source);
		newscore.eventList = self.score.eventList.clone;
		self.score.automationRack.cloneTo(newscore.automationRack);
		newscore.controller = self.parentTrack.inputController;
		//[newscore.outBus, self.score.outBus].debug("cloneScore: new old");
		newscore.outBus = self.score.outBus;
		//[newscore.outBus, self.score.outBus].debug("cloneScore: new old2");
		newscore;
	},

	outBus_: { arg self, val;
		if(self.score.notNil) {
			self.score.outBus = val;
		};
	},

	outBus: { arg self;
		if(self.score.notNil) {
			self.score.outBus;
		} {
			nil
		}
	},

	asPreview: { arg self;
		var model = self.parentTrack.parentTrack;
		var tlpreview = self.score.asPreview(model.areasizex, true);
		//tlpreview.setViewportToClipBounds(model.areasizex);
		//tlpreview.timeline.areasize.x = model.areasizex ?? { tlpreview.timeline.areasize.x };
		tlpreview.timeline.changed(\areasize);
		tlpreview;
	},

	scorePresetCompileString: { arg self;
		var str;
		str = List.new;
		str.add( "%.score = %;".format(self.refCompileString, self.score.refCompileString));
		str.add( self.score.presetCompileString );
		str.join($\n);
	},

	score_: { arg self, score;
		self[\score] = score;
		self.score.controller = self.parentTrack.inputController;
		self.proxy = PlayerWrapper(self.score);
		// do not assign outBus to allow outBus copy when copying sections
		//self.proxy.debug("makeScore proxy end");
	},

	makeScore: { arg self;
		//self.proxy.debug("makeScore proxy start");
		self.score = NoteTimeline(self.name+++\score, self.parentTrack.source);
		self.score.outBus = self.parentTrack.outBus;
	}
));

TagSpecDef(\CustomTrackTemplate).addUnique(\NoteTimelineTrack -> TrackTemplateDef(\NoteTimelineTrack));
);

WindowDef(\PdefInstrumentEditor, { arg def, instr;

	//instr.debug("instr");
	//instr.source.debug("source");
	WindowDef(\PbindefEditor).asView(instr.source);

	//var target = instr.source;
	//var pwrapper = PlayerWrapper(target);
	//var paramView = View.new;
	//paramView.followChange(instr, \source, {
		//pwrapper.target = target;
	//});
	//paramView.followChange(instr, \playerSynthDef, {
		////Log(\Param).debug("paramView: followChange playerSynthDef");
		//paramView.removeAll;
		//paramView.layout = ParamGroupLayout.two_panes(target.asParamGroup(nil, false, [\gate, \doneAction]));
	//});
	//VLayout(
		//HLayout (
			//pwrapper.asView,
			//Param(Message(instr), \playerSynthDef, TagSpecDef(\SynthDef_keys)).asPopUpMenu,
			//if(target.getHalo(\edit).notNil) {
				//BasicButton.new.string_("Special edit").action_({
					//target.getHalo(\edit).value(target);
				//})
			//}
		//),
		//paramView,
	//);
});

//////// KitTimeline
(
TrackTemplateDef(\KitTimelineTrack, (
	parent: TrackTemplateDef(\NoteTimelineTrack),
	childClass: { TrackTemplateDef(\KitTimelineTrackPart) },

	asParamGroup: { arg self;
		self.source.asParamGroup;
	},

	editInstrument: { arg self;
		self.kit.edit;
	},

	trackEditor: { arg self;
		//WindowDef(\PdefInstrumentEditor);
		WindowDef(\KitTimelineTrackEditor, { arg def;
			VLayout (
				//HLayout (
					//Param(Message(self), \kit, ParamNonFloatSpec()).asTextField,
					//BasicButton.new.string_("Select kit"),
				//),
				//BasicButton.new.string_("gtg kit"),
				nil
			);
		});
	},

	recorderView: { arg self;
		HLayout (
			Param(Message(self), \recordArmed, ParamBoolSpec()).asButton("●").maxWidth_(20),
			Param(Message(self), \inputController, ControllerDef.getTagSpec(\drumpad)).asPopUpMenu.minWidth_(5),
		).margins_(0).spacing_(0)
	},

	scorePresetCompileString: { arg self;
		var str;
		str = List.new;
		str.add( "%.kit = %;".format(self.refCompileString, self.kit.asCompileString));
		if(self.kit.key == self.defaultKitKey) {
			// if not a custom kit, save it
			str.add( "%".format(self.kit.presetCompileString));
		};
		str.join($\n);
	},

	defaultKitKey: { arg self;
		( self.trackKey++\_kit ).asSymbol;
	},

	cloneScore: { arg self;
		var newkit;
		newkit = PatKitDef(self.defaultKitKey);
		newkit.loadFrom(self.kit);
		self.kit = newkit;
	},

	makeScore: { arg self;
		self.kit = PatKitDef(self.defaultKitKey);
		//self[\source] = Pdef("%.%.instr".format(self.key, self.index).asSymbol, Pbind());
	},
));

TrackTemplateDef(\KitTimelineTrackPart, (
	parent: TrackTemplateDef(\NoteTimelineTrackPart),

	cloneScore: { arg self;
		var newscore;
		newscore = KitTimeline(self.name+++\score, self.parentTrack.kit);
		newscore.eventList = self.score.eventList.clone;
		newscore.buffer = self.score.buffer;
		newscore.outBus = self.parentTrack.outBus;
		newscore.recorder.recBus = self[\score].recorder.recBus;
		newscore;
	},

	score_: { arg self, score;
		self[\score] = score;
		self.proxy = PlayerWrapper(self.score);
		//self.proxy.debug("makeScore proxy end");
	},


	makeScore: { arg self;
		self.score = KitTimeline(self.name+++\score, self.parentTrack.kit);
		self.score.outBus = self.parentTrack.outBus;
		self.score.recorder.recBus = self.parentTrack.recBus ?? { self[\score].recorder.recBus  };
	},

	scorePresetCompileString: { arg self;
		var str;
		str = List.new;
		str.add( "%.score = %;".format(self.refCompileString, self.score.refCompileString));
		str.add( self.score.presetCompileString );
		str.join($\n);
	},
));

TagSpecDef(\CustomTrackTemplate).addUnique(\KitTimelineTrack -> TrackTemplateDef(\KitTimelineTrack));
);

//////// SampleTimeline
(
TrackTemplateDef(\SampleTimelineTrack, (
	parent: TrackTemplateDef(\NoteTimelineTrack),
	childClass: { TrackTemplateDef(\SampleTimelineTrackPart) },

	asParamGroup: { arg self;
		self.source.asParamGroup;
	},

	keepSamplesInUse: { arg self;
		self.existingChildren.do { arg clip, idx;
			if(clip.isEmpty.not and: { clip.source.notNil }) {
				clip.source.keepRecordedFile;
			}
		};
	},

	playerSynthDef: \SampleTimeline_player,

	scorePresetCompileString: { arg self;
		if(self.recBus.notNil) {
			"%.recBus = %;".format(self.refCompileString, self.recBus.asCompileString);
		} {
			""
		}
	},

	trackEditor: { arg self;
		//WindowDef(\PdefInstrumentEditor);
		WindowDef(\SampleTimelineTrackEditor, { arg def;
			VLayout (
				Param(Message(self),\recBus,ParamAudioBusSpec()).asView,
				nil
			);
		});
	},

	recorderView: { arg self;
		HLayout (
			Param(Message(self), \recordArmed, ParamBoolSpec()).asButton("●").maxWidth_(20),
			Param(Message(self), \recBus, ParamAudioBusSpec()).asPopUpMenu.minWidth_(5),
		).margins_(0).spacing_(0)
	},

	cloneScore: { arg self;
		// NOOP
	},

	makeScore: { arg self;
		//self[\source] = Pdef("%.%.instr".format(self.key, self.index).asSymbol, Pbind());
		//self.isEmpty = false;
	},
));

TrackTemplateDef(\SampleTimelineTrackPart, (
	parent: TrackTemplateDef(\NoteTimelineTrackPart),

	cloneScore: { arg self;
		var newscore;
		newscore = SampleTimeline(self.name+++\score);
		newscore.eventList = self.score.eventList.clone;
		newscore.buffer = self.score.buffer;
		newscore.outBus = self.parentTrack.outBus;
		newscore.recorder.recBus = self[\score].recorder.recBus;
		newscore;
	},

	score_: { arg self, score;
		self[\score] = score;
		self.score.recorder.recBus = self.parentTrack.recBus ?? { self[\score].recorder.recBus  };
		self.proxy = PlayerWrapper(self.score);
		//self.proxy.debug("makeScore proxy end");
	},


	makeScore: { arg self;
		self.score = SampleTimeline(self.name+++\score);
		self.score.outBus = self.parentTrack.outBus;
	},

	scorePresetCompileString: { arg self;
		var str;
		str = List.new;
		str.add( "%.score = %;".format(self.refCompileString, self.score.refCompileString));
		str.add( self.score.presetCompileString );
		str.join($\n);
	},
));

TagSpecDef(\CustomTrackTemplate).addUnique(\SampleTimelineTrack -> TrackTemplateDef(\SampleTimelineTrack));
);

//////// ParamTimeline
(
TrackTemplateDef(\ParamTimelineTrack, (
	parent: TrackTemplateDef(\NoteTimelineTrack),
	childClass: { TrackTemplateDef(\ParamTimelineTrackPart) },

	asParamGroup: { arg self;
		self.source.asParamGroup;
	},

	scorePresetCompileString: { arg self;
		if(self.targetParam.notNil) {
			"%.targetParam = %;".format(self.refCompileString, self.targetParam.asCompileString)
		} {
			""
		}
	},

	trackEditor: { arg self;
		//WindowDef(\PdefInstrumentEditor);
		WindowDef(\ParamTimelineTrackEditor, { arg def;
			HLayout (
				Param(Message(self),\targetParam,ParamNonFloatSpec()).asView,
				BasicButton.new.string_("Select param").action_({
					WindowDef(\ParamSelectDialog).front(nil, { arg param; 
						//param.debug("selected param");
						self.targetParam = param;
					});
				}),
			);
		});
	},

	recorderView: { arg self;
		HLayout (
			Param(Message(self), \recordArmed, ParamBoolSpec()).asButton("●").maxWidth_(20),
			Param(Message(self), \targetParam, ParamNonFloatSpec()).asPopUpMenu.minWidth_(5),
		).margins_(0).spacing_(0)
	},

	cloneScore: { arg self;
		// NOOP
	},

	makeScore: { arg self;
		//self[\source] = Pdef("%.%.instr".format(self.key, self.index).asSymbol, Pbind());
		//self.isEmpty = false;
	},
));

TrackTemplateDef(\ParamTimelineTrackPart, (
	parent: TrackTemplateDef(\NoteTimelineTrackPart),

	cloneScore: { arg self;
		var newscore;
		newscore = ParamTimeline(self.name+++\score, self.score.param);
		newscore.eventList = self.score.eventList.clone;
		newscore.buffer = self.score.buffer;
		newscore.outBus = self.parentTrack.outBus;
		newscore.recorder.recBus = self[\score].recorder.recBus;
		newscore;
	},

	score_: { arg self, score;
		self[\score] = score;
		self.proxy = PlayerWrapper(self.score);
		//self.proxy.debug("makeScore proxy end");
	},


	makeScore: { arg self;
		self.score = ParamTimeline(self.name+++\score, self.parentTrack.targetParam);
	},

	scorePresetCompileString: { arg self;
		var str;
		str = List.new;
		str.add( "%.score = %;".format(self.refCompileString, self.score.refCompileString));
		str.add( self.score.presetCompileString );
		str.join($\n);
	},
));

TagSpecDef(\CustomTrackTemplate).addUnique(\ParamTimelineTrack -> TrackTemplateDef(\ParamTimelineTrack));
);

//////// ClipTimeline
(
TrackTemplateDef(\ClipTimelineTrack, (
	parent: TrackTemplateDef(\NoteTimelineTrack),
	childClass: { TrackTemplateDef(\ClipTimelineTrackPart) },

	asParamGroup: { arg self;
		self.source.asParamGroup;
	},
	trackEditor: { arg self;
		//WindowDef(\PdefInstrumentEditor);
	},


	recorderView: { arg self;
		HLayout (
			Param(Message(self), \recordArmed, ParamBoolSpec()).asButton("●").maxWidth_(20),
			Param(Message(self), \inputController, ControllerDef.getTagSpec(\piano)).asPopUpMenu.minWidth_(5),
		).margins_(0).spacing_(0)
	},

	scorePresetCompileString: { arg self;
		var str;
		str = List.new;
		//str.add( "%.source = %;".format(self.refCompileString, self.source.asCompileString));
		//str.add( PlayerWrapper(self.source).presetCompileString );
		str.join($\n);
	},

	cloneScore: { arg self;
		// NOOP
	},

	makeScore: { arg self;
		//self[\source] = Pdef("%.%.instr".format(self.key, self.index).asSymbol, Pbind());
		//self.isEmpty = false;
	},
));
TrackTemplateDef(\ClipTimelineTrackPart, (
	parent: TrackTemplateDef(\NoteTimelineTrackPart),
	edit: { arg self;
		self.score.edit;
	},

	startRecordingNewScore: { arg self;
		self.makeScore;
		self.score.startRecording;
	},

	stopRecording: { arg self;
		if(self.score.notNil) {
			self.score.stopRecording;
		}
	},

	cloneScore: { arg self;
		var newscore;
		newscore = ClipTimeline(self.name+++\score);
		newscore.eventList = self.score.eventList.clone;
		newscore.controller = self.parentTrack.inputController;
		newscore.outBus = self.parentTrack.outBus;
		newscore;
	},

	outBus_: { arg self, val;
		if(self.score.notNil) {
			self.score.outBus = val;
		}
	},

	outBus: { arg self;
		if(self.score.notNil) {
			self.score.outBus
		}
	},

	asPreview: { arg self;
		var model = self.parentTrack.parentTrack;
		var tlpreview = self.score.asPreview(model.areasizex);
		//tlpreview.timeline.areasize.x = model.areasizex ?? { tlpreview.timeline.areasize.x };
		tlpreview.timeline.changed(\areasize);
		tlpreview;
	},

	score_: { arg self, val;
		self[\score] = val;
		self.score.controller = self.parentTrack.inputController;
		self.proxy = PlayerWrapper(self.score);
	},

	makeScore: { arg self;
		//self.proxy.debug("makeScore proxy start");
		self.score = ClipTimeline(self.name+++\score);
	}
));

TagSpecDef(\CustomTrackTemplate).addUnique(\ClipTimelineTrack -> TrackTemplateDef(\ClipTimelineTrack));
);
