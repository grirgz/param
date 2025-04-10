
//FileSystemProject.load("libdef/ParamSelectDialog.scd");
//FileSystemProject.load("libdef/CustomTrackList.scd");
TrackTemplateDef(\AutomationRackTrack, (
	parent: TrackTemplateDef(\CustomTrack),
	childClass: { TrackTemplateDef(\AutomationRack) },

));
ProtoTemplateDef(\AutomationItem, (
	new: { arg self, parent, param;
		self = ProtoClass((parent: self));
		// FIXME: if two param have same proprerty, names will clash
		self.paramTimeline = ParamTimeline("%_%".format(parent.automationKey, param.propertyRoot).asSymbol, param);
		self.param = param;
		self.parentRack = parent;
		self;
	},

	disabled_: { arg self, val;
		self[\disabled] = val;
		self.changed(\disabled);
	},

	clone: { arg oldself, newparent;
		var self;
		self = oldself.copy;
		self.paramTimeline = oldself.paramTimeline.clone("%_%".format(newparent.automationKey, self.param.propertyRoot).asSymbol);
		self.parentRack = newparent;
		
	},

));
TrackTemplateDef(\AutomationRack, (
	parent: TrackTemplateDef(\CustomTrackPart),
	//paramList
	edit: { arg self;
		WindowDef(\AutomationRack).edit(self)
	},

	//playHook: { arg self;
		//self.paramList.debug("playHook");
		//self.paramList.do { arg asso, idx;
			//asso.value.play;
		//};
	//},
	//stopHook: { arg self;
		//self.paramList.do { arg asso, idx;
			//asso.value.stop;
		//};
	//},

	index: { arg self;
		// when standalone, use 0
		0
	},

	proxy: { arg self;
		PlayerWrapperGroup(self.automationList.collect(_.paramTimeline))
	},

	isEmpty: { arg self;
		self.automationList.size == 0
	},

	automationKey: { arg self;
		self.trackKey ?? { self.key };
	},

	hasAnyTrackEnabled: { arg self;
		self.automationList.select({ arg x; x.disabled != true }).size > 0
	},

	isPlaying: { arg self;
		self.automationList.size > 0 and: {
			self.automationList.any({ arg item; item.paramTimeline.isPlaying.not }).not
		}
	},
	
	addAutomation: { arg self, param;
		self.automationList = self.automationList ?? { List.new };
		if(self.getParamTimeline(param).isNil) {
			self.automationList.add( ProtoTemplateDef(\AutomationItem).new(self, param));
			self.changed(\automationList);
		};
	},

	removeAutomation: { arg self, param;
		var idx = self.automationList.detectIndex { arg x; x.param == param };
		if(idx.notNil) {
			self.automationList.removeAt(idx);
			self.changed(\automationList);
		};
	},

	addParamDialog: { arg self;
		WindowDef(\ParamSelectDialog).front(nil, { arg param;
			param.debug("ParamSelectDialog param");
			self.addAutomation(param);

		})
	},

	getParamTimeline: { arg self, param;
		self.automationList.detect { arg x; x.param == param } !? _.paramTimeline
	},

	getAutomation: { arg self, param;
		self.automationList.detect { arg x; x.param == param }
	},

	asPreview: { arg self, param;
		self.getParamTimeline(param).asPreview
	},

	asStackPreview: { arg self, initfun;
		StackLayout(
			*self.automationList.collect { arg item, idx;
				var preview;
				preview = item.paramTimeline.asPreview;
				preview.background = Color.grey(1,0);
				initfun.(preview);
				preview;
			};
		).mode_(1)
	},

	asView: { arg self, param;
		self.getParamTimeline(param).asView
	},

	makeScore: { arg self;
		self.addParamDialog
	},

	clone: { arg oldself, parent, index;
		// WIP
		var self;
		self = oldself.copy;

		self.parentTrack = {parent};
		self.index = index.asInteger;
		//self.score = self.cloneScore;
		self.automationList = oldself.cloneAutomationList(self);
		self;
	},

	cloneTo: { arg self, part;
		part.automationList = self.cloneAutomationList(part);
	},

	cloneAutomationList: { arg self, newparent;
		self.automationList.collect { arg item, idx;
			item.clone(newparent);
		};
	},

	asPattern: { arg self, repeatCount;
		if(self.automationList.size > 0) {
			Ppar(
				self.automationList.select({ arg x; x.disabled != true }).collect { arg item, idx;
					var pt = item.paramTimeline;
					pt.asSelectedPattern(repeatCount);
				};
			)
		} {
			nil
		};
	},

	paramList: { arg self;
		self.automationList = self.automationList ?? { List.new };
		self.automationList.collect(_.param);
	},

	paramList_: { arg self, plist;
		self.automationList = List.new;
		plist.do { arg param, idx;
			self.addAutomation(param)
		};
	},

	mimicCursor: { arg self, cursorModel, view;
		//[cursor, othercur].debug("mimicCursor");
		if(self.mimicCursorController.notNil) {
			self.mimicCursorController.remove;
		};
		self.mimicCursorController = SimpleController(cursorModel).put(\refresh, {
			//"mimicCursorController".debug;
			if(view.notNil) {
				if(view.isClosed) {
					self.mimicCursorController.remove;
				} {
					self.automationList.do { arg auto, idx;
						var curdest = auto.paramTimeline.cursor;
						curdest.startPosition = cursorModel.startPosition;
						curdest.endPosition = cursorModel.endPosition;
					};
				};
			}
		})
	},

	presetCompileString: { arg self, refCompileString;
		var str = "";
		refCompileString = refCompileString ?? { self.refCompileString };
		str = str ++ "%.paramList = %;\n".format(refCompileString, self.paramList.asCompileString);
		self.automationList.collect { arg item, idx;
			str = str ++ item.paramTimeline.presetCompileString;
		};
		str;
	},

	recorder: { arg self;
		if(self[\recorder_obj].isNil) {
			self[\recorder_obj] = ProtoTemplateDef(\AutomationRackRecorder).new(self);
		};
		self[\recorder_obj];
	},

));


ProtoTemplateDef(\AutomationRackRecorder,(
	isRecording: false,
	new: { arg self, rack;
		self = ProtoClass((parent:self));
	
		self.rack = { rack };
	
		self;
	},
	isRecording_: { arg self, val;
		if(val == true) {
			self.startRecording;
		} {
			self.stopRecording;
		};
	},

	startRecording: { arg self;
		var con;
		"AutomationRack.startRecording".debug;
		if(self.isRecording == false) {
			var rack = self.rack;
			self[\isRecording] = true;

			con = SimpleController(Param).put(\lastTweaked, {
				var param = Param.lastTweaked;
				param.debug("AutomationRack.lastTweaked");
				rack.addAutomation(param);
				rack.getParamTimeline(param).startRecording;
			});
			self.controller = con;
			self.changed(\isRecording);
			self.changed(\startedRecording);
		};
	},

	stopRecording: { arg self;
		"AutomationRack.stopRecording".debug;
		if(self.isRecording == true) {
			self[\isRecording] = false;
			self.controller.remove;
			self.rack.automationList.do { arg item, idx;
				item.paramTimeline.stopRecording;
			};

			self.changed(\isRecording);
			self.changed(\stoppedRecording);

		};

	},
));


WindowDef(\AutomationRack, { arg def, model;
	var childview = View.new;
	childview.followChange(model, \automationList, {
		childview.removeAll;
		childview.layout = VLayout (
			*model.automationList.collect { arg item, idx;
				item.debug("view item");
				HLayout (
					Param(Message(item.paramTimeline), \bypassed, ParamBoolSpec()).asCheckBox.string_("").fixedWidth_(20),
					PlayerWrapperView(PlayerWrapper(item.paramTimeline)).label_("").asView.fixedSize_(30),
					item.param.asStaticTextLabel(\full).fixedWidth_(100).attachContextMenu,
					item.paramTimeline.asPreview;
				)
			} ++ [nil]
		)
	});
	VLayout (
		HLayout (
			PlayerWrapper(model).asView,
			BasicButton.new.string_("Add ParamTimeline").action_({
				model.addParamDialog;
			}),
			RecordButton.new(model.recorder),
			BasicButton.new.string_("Refresh").action_({
				model.changed(\automationList);
			}),
			//BasicButton.new.string_("Rec").action_({
				//model.recorder.startRecording;
			//})
		),
		childview
	)
	
});

TagSpecDef(\CustomTrackTemplate).addUnique(\ParamTimelineRackTrack -> TrackTemplateDef(\ParamTimelineRackTrack));
