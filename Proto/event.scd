
"init event.scd".postln;

	~stepEventView = (
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
			self.stepevent.debug("refresh_mod_list_view: stepevent");
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
							key.debug("what??");
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
								StaticText.new.string_("empty");
							};

							[
								if(steplist.bypass == 1) {
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
