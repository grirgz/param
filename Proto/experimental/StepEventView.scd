
"init StepEventView.scd".postln;

~stepEventView = (
	// FIXME: find a better name, this is a special kind of stepEventView
	new: { arg self, stepEvent, param_group;
		self = self.deepCopy;
	
		self.stepevent = stepEvent;
		self.param_group = param_group ? self.stepevent.asParamGroup;
		//self.stepevent.debug("new: stepevent");
		//self.stepevent.addHalo(\instrument, self.base.getHalo(\instrument));
		//self.stepevent.addHalo(\spec, self.base.getHalo(\spec));
		//self.playerWrapper = PlayerWrapper(self.base);
		
	
		self;
	},

	add_modulator: { arg self, param;
		self.stepevent[param.property] = StepList[param.get].stepCount_(self.stepevent.stepCount);
		self.refresh_mod_list_view;
	},

	remove_modulator: { arg self, param;
		self.stepevent[param.property] = nil;
		self.refresh_mod_list_view;
	},

	bypass_modulator: { arg self, modu;
		modu.bypass = ( modu.bypass + 1 % 2 );
		self.refresh_mod_list_view;
	},

	refresh_mod_list_view: { arg self;
		//self.stepevent.debug("refresh_mod_list_view: stepevent");
		if(self.mod_list_view.notNil) {
			var keys;
			self.mod_list_view.removeAll;
			self.param_group_dict = Dictionary.new;
			self.param_group.do ({ arg param;
				self.param_group_dict[param.property] = param 
			});
			keys = self.stepevent.keys.asList;
			keys.sortLike(self.param_group.collect(_.property));
			self.mod_list_view.layout = VLayout(
				GridLayout.rows(
					//StaticText.new.string_("fuck"),
					* keys.collect({ arg key;
						var editor;
						var steplist;
						//key.debug("what??");
						steplist = self.stepevent[key];
						editor = if(steplist.isKindOf(StepList)) {
							var param;

							// propagate spec in the steplist
							param = self.param_group_dict[key];
							if(param.notNil) {
								steplist.addSpec(\list, param.spec);
							};

							//if(steplist.bypass == 1) {
							//	StaticText.new.string_("disabled");
							//} {
							//	StepListView(steplist).view;
							//}
							StepListColorView(steplist).setCursor.view;
						} {
							StaticText.new.string_(steplist.asCompileString);
						};

						[
							if(steplist.isKindOf(StepList) and: {steplist.bypass == 1}) {
								StaticText.new.string_("[%]".format(key));
							} {
								StaticText.new.string_(key);
							},
							editor,
						]

					}) 
				),
				nil,
			);
		}
	},

	makeLayout: { arg self;
		var param_view;
		var mod_view;
		var mod_header_view, mod_list_view;
		var param_selector;

		//param_view = GridLayout.rows(
		//	*self.param_group.collect({ arg param;
		//		[
		//			param.asStaticTextLabel,
		//			param.asSlider.orientation_(\horizontal),
		//			param.asTextField,
		//		]
		//	})
		//);

		mod_header_view = HLayout(
			param_selector = PopUpMenu.new.items_(self.param_group.collect({ arg param;
				param.property;
			})),
			XSimpleButton.new.label_("+").action_({
				var modulator = self.stepevent[param_selector.items[param_selector.value]];
				if(modulator.isNil) {
					self.add_modulator(self.param_group[param_selector.value]);
				}
			}),
			XSimpleButton.new.label_("-").action_({
				var modulator = self.stepevent[param_selector.items[param_selector.value]];
				if(modulator.notNil) {
					self.remove_modulator(self.param_group[param_selector.value]);
				}
			}),
			XSimpleButton.new.label_("bypass").action_({
				var modulator = self.stepevent[param_selector.items[param_selector.value]];
				if(modulator.notNil) {
					self.bypass_modulator(modulator);
				}
			}),
			Param(Message(self.stepevent), \stepCount).asNumberBox,
		);

		mod_list_view = View.new;
		self.mod_list_view = mod_list_view;
		self.refresh_mod_list_view;

		mod_view = VLayout(
			mod_header_view,
			ScrollView.new.canvas_(mod_list_view),
		);

		self.layout = VLayout(
			//self.playerWrapper.asView,
			//param_view,
			mod_view,
		);
		self.layout;
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
		
	},

	as_pattern: { arg self;
		self.stepevent.asPattern;
	},

);

~envEventView = (

	new: { arg self, stepEvent, param_group;
		self = self.deepCopy;
	
		self.stepevent = stepEvent;
		self.param_group = param_group ? self.stepevent.asParamGroup;
		//self.stepevent.debug("new: stepevent");
		//self.stepevent.addHalo(\instrument, self.base.getHalo(\instrument));
		//self.stepevent.addHalo(\spec, self.base.getHalo(\spec));
		//self.playerWrapper = PlayerWrapper(self.base);
		
	
		self;
	},

	add_modulator: { arg self, param;
		self.stepevent[param.property] = ~envTimeline.newInstance(
			Param(BusDef(param.property, \control), param.property, param.spec)
		);
		//"eeeeeeeeeeeeee".debug;
		self.refresh_mod_list_view;
	},

	remove_modulator: { arg self, param;
		self.stepevent[param.property] = nil;
		self.refresh_mod_list_view;
	},

	bypass_modulator: { arg self, modu;
		//modu.bypass = ( modu.bypass + 1 % 2 );
		self.refresh_mod_list_view;
	},

	refresh_mod_list_view: { arg self;
		//self.stepevent.debug("refresh_mod_list_view: stepevent");
		if(self.mod_list_view.notNil) {
			var keys;
			self.mod_list_view.removeAll;
			self.param_group_dict = Dictionary.new;
			self.param_group.do ({ arg param;
				self.param_group_dict[param.property] = param 
			});
			keys = self.stepevent.keys.asList;
			keys.sortLike(self.param_group.collect(_.property));
			self.mod_list_view.layout = VLayout(
				GridLayout.rows(
					//StaticText.new.string_("fuck"),
					* keys.collect({ arg key;
						var editor;
						var steplist;
						//key.debug("what??");
						steplist = self.stepevent[key];
						editor = if(steplist.isKindOf(StepList)) {
							var param;

							// propagate spec in the steplist
							param = self.param_group_dict[key];
							if(param.notNil) {
								steplist.addSpec(\list, param.spec);
							};

							//if(steplist.bypass == 1) {
							//	StaticText.new.string_("disabled");
							//} {
							//	StepListView(steplist).view;
							//}
							StepListView(steplist).setCursor.view;
						} {
							if(steplist.isKindOf(Event) and: { steplist.eventType == \paramTimeline }) {
								//~envTimelinePanelView.newView(steplist).debug("panel!!!!!!!!!").makeLayout;
								~envTimelinePanelView.newView(steplist).make_header_(nil).makeLayout;
							} {
								StaticText.new.string_("empty");
							}
						};

						[
							if(steplist.isKindOf(StepList) and: {steplist.bypass == 1}) {
								StaticText.new.string_("[%]".format(key));
							} {
								StaticText.new.string_(key);
							},
							editor,
						]

					}) 
				),
				nil,
			);
		}
	},

	makeLayout: { arg self;
		var param_view;
		var mod_view;
		var mod_header_view, mod_list_view;
		var param_selector;


		mod_header_view = HLayout(
			param_selector = PopUpMenu.new.items_(self.param_group.collect({ arg param;
				param.property;
			})),
			XSimpleButton.new.label_("+").action_({
				var modulator = self.stepevent[param_selector.items[param_selector.value]];
				if(modulator.isNil) {
					self.add_modulator(self.param_group[param_selector.value]);
				}
			}),
			XSimpleButton.new.label_("-").action_({
				var modulator = self.stepevent[param_selector.items[param_selector.value]];
				if(modulator.notNil) {
					self.remove_modulator(self.param_group[param_selector.value]);
				}
			}),
			XSimpleButton.new.label_("bypass").action_({
				var modulator = self.stepevent[param_selector.items[param_selector.value]];
				if(modulator.notNil) {
					self.bypass_modulator(modulator);
				}
			}),
		);

		mod_list_view = View.new;
		self.mod_list_view = mod_list_view;
		self.refresh_mod_list_view;

		mod_view = VLayout(
			mod_header_view,
			ScrollView.new.canvas_(mod_list_view),
		);

		self.layout = VLayout(
			mod_view,
		);
		self.layout;
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
		
	},

	asEventPattern: { arg self;
		(Plazy({ self.stepevent.asPattern })  <> Plazy({ self.base })).loop;
	},

	xasPattern: { arg self;
		Ppar([
			self.asEventPattern,
			self.asEnvPattern,
		]).loop
	},

	asEnvPattern: { arg self;
		Plazy({
			var list = List.new;
			self.stepevent.keys.asArray.do({ arg key;
				var val = self.stepevent[key];
				if(val.isKindOf(Event) and: { val.eventType == \paramTimeline }) {
					list.add(val.xasPattern);
				};
			});
			if(list.size == 0) {
				Event.silent(1); // FIXME: this will delay base if shorter, don't know what to do
			} {
				Ppar(list);
			};
		}).loop

	}

);


~stepEventCoinView = (
	new: { arg self, stepEvent;
		self = self.deepCopy;
	
		self.stepEvent = stepEvent;
		self.hasCursor = false;

	
		self;
	},

	mapStepEvent: { arg self, stepevent;
		self.stepEvent = stepevent;
		if(self.stepEvent.notNil) {
			self.stepEvent[\dur] = self.stepEvent[\dur] ? 1;
			self.stepEvent[\isRest] = self.stepEvent[\isRest] ?? { 
				StepList[1].addSpec(\list, \unipolar).addHalo(\seqstyle, \gridButton)
			};
		};
		if(self.durField.notNil) {
			if(self.stepEvent.isNil) {
				"TODO stepEvent.isNil".debug;
			} {
				self.stepListView.mapStepList(self.stepEvent[\isRest]);
				self.durField.mapParam(Param(self.stepEvent, \dur, ControlSpec(0,100,\lin)));
				self.stepField.mapParam(Param(Message(self.stepEvent[\isRest]), \stepCount, ControlSpec(1,128,\lin)));
			}
		}
	},

	asView: { arg self;
		self.stepListView = StepListColorView.new;
		self.durField = TextField.new.maxWidth_(40);
		self.stepField = NumberBox.new.maxWidth_(40);
		self.layout = HLayout (
			PopUpMenu.new.items_(["step", "coin"]).action_({ arg view;
				switch(view.value,
					0, {
						self.stepEvent[\isRest].addHalo(\seqstyle, \gridButton);
						self.stepListView.mapStepList(self.stepEvent[\isRest])
					},
					1, {
						self.stepEvent[\isRest].addHalo(\seqstyle, \knob);
						self.stepListView.mapStepList(self.stepEvent[\isRest])
					}
				)
			}).value_(
				if(self.stepEvent[\isRest].getHalo(\seqstyle) == \gridButton) {
					0
				} {
					1
				}
			),
			self.durField,
			self.stepField,
			self.stepListView.asView,
		);
		//self.stepListView.setCursor(self.hasCursor);
		self.stepListView.hasCursor = self.hasCursor;
		self.mapStepEvent(self.stepEvent);
		self.layout;
	},

	edit: { arg self;
		var window = Window.new;
		var layout;
		layout = self.asView;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
	},

);
