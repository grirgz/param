////// Main editors

(
WindowDef(\GenericEditor, { arg me, model;
	case(
		{ model.isKindOf(Ndef) }, {
			WindowDef(\NdefEditor).asView(model);
		},
		{ model.isKindOf(Pdef) }, {
			WindowDef(\PdefEditor).asView(model);
		}, 
		{ model.isKindOf(EventPatternProxy) }, {
			WindowDef(\PdefEditor).asView(model);
		}, {
			BasicButton.new("Edit").action_({
				model.edit
			})
		}
	)
});
);

(
WindowDef(\EventPatternProxyEditor, { arg me, target;
	VLayout(
		PlayerWrapper(target).asView,
		ParamGroupLayout.two_panes(target.asParamGroup),
	);
});


WindowDef(\PdefEditor, { arg me, key=\default;
	var target;
	if(key.isKindOf(Pdef) or: key.isKindOf(Ndef)) {
		target = key;
	} {
		target = Pdef(key);
	};
	VLayout(
		HLayout (
			PlayerWrapper(target).asView,

			BasicButton.new.string_("...").maxWidth_(20).action_({ arg view;
				WindowDef(\PdefEditorOverlayMenu, { arg def, model, view, x=0, y=0;
					var inview, layout;
					def.windowDo { arg win;
						var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
						win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 200,100);
						win.endFrontAction = { win.close };
					};
					inview = View.new;


					layout = VLayout (
						* [
							if(target.getHalo(\edit).notNil) {
								BasicButton.new.string_("Halo edit").action_({
									target.getHalo(\edit).value(target);
								})
							},
							if(WindowDef(\PbindefEditor).notNil) {
								BasicButton.new.string_("Convert to Pbindef").action_({
									target.convertToPbindef;
									WindowDef(\PbindefEditor).front(target);
									me.closeWindow;
								})
							},
						].reject(_.isNil) ++
						[ nil ]
					);
					inview.layout_(layout);

				}).border_(false).front(target, view);
			}),
		),
		ParamGroupLayout.two_panes(target.asParamGroup),
	);
});





WindowDef(\NodeProxyEditor, { arg me, target;
	var pg;
	pg = target.asParamGroup;
	VLayout(
		HLayout(
			PlayerWrapper(target).asView,
			//EventClass.presetSelectorView.(pg), // can't save without name so is useless
			Param(Message(target), \quant, ParamNonFloatSpec() ).asTextField.maxWidth_(80),
		),
		ParamGroupLayout.two_panes(target.asParamGroup)
	);
});

WindowDef(\NdefEditor, { arg me, key=\default;
	var target;
	var pg;
	if(key.isKindOf(Pdef) or: key.isKindOf(Ndef)) {
		target = key;
	} {
		target = Ndef(key);
	};
	pg = ParamGroupDef.update(\NdefParamGroup_ +++ target.key, target.asParamGroup);
	VLayout(
		HLayout(
			PlayerWrapper(target).asView,
			EventClass.presetSelectorView.(pg),
			Param(Message(target), \quant, ParamNonFloatSpec()).asTextField.maxWidth_(80),
			BasicButton.new.string_("...").maxWidth_(20).action_({
				Menu(
					MenuAction("Refresh GUI", {
						me.front(key)
					}),
					MenuAction("Send Ndef", {
						target.send;
					}),
				).front
			})
		),
		ParamGroupLayout.two_panes(target.asParamGroup)
	);
});
);

////// Param editors
(
WindowDef(\ParamGenericContextMenu, { arg def, param, view;
	Menu(*
		WindowDef(\ParamGenericContextMenuEntries).sourceValue(param, view)
	)
});

WindowDef(\ParamGenericContextMenuEntries, { arg def, param, view;
	var newmodmenu;
	newmodmenu = Menu(*
		TagSpecDef(\ModulatorTemplate).list.collect { arg asso, idx;
			MenuAction(asso.key, {
				var combi, nextidx;
				var child = TrackDef(\ModulatorRack).addModulator(asso.value);
				var pa = param;
				if(param.property.value == \source) {
					pa = param.parent;
				};
				if(param.hasCombinator.not) {
					ParamCombinator(pa).setBusMode(true);
				};
				combi = param.getCombinator;
				nextidx = combi.nextFreeInput;
				if(nextidx.notNil) {
					combi.inputParam.at(nextidx).setBus(PlayerWrapper(child).outBus.asMap);
					combi.inputObjects[nextidx] = child;
					//pa.at(\source).set(combi);
					child.play;
					param.target.changed(\keyForm, param); // for PbindefEditor
				} {
					Log(\Param).error("No free input for param % combi %".format(param, combi));
				};
			})
		}
	).title_("New modulator");

	[
		MenuAction(param.fullLabel).enabled_(false),
		MenuAction.separator,
		Param(Message(param), \inBusMode, ParamBoolSpec()).asMenuAction("Bus mode"),
		MenuAction("Override pattern", {
			if(param.target.isKindOf(EventPatternProxy)) {
				Pbindef(param.target.key, param.propertyRoot, Pkey(param.propertyRoot));
			} {
				Log(\Param).info("Can't override, not an EventPatternProxy");
			}
		}),
		MenuAction("Set default", {
			param.set(param.default)
		}),
		MenuAction("Unset", {
			param.unset;
		}),
		Menu(
			CustomViewAction(
				param.asTextField.minWidth_(500);
			)
		).title_("Edit text"),
		MenuAction.separator,

		MenuAction("MIDI CC learn and map", {
			var midi = param.target.getHalo(\MIDIFunc, param.property);
			if(midi.notNil) {
				midi.free;
				param.target.addHalo(\MIDIFunc, param.property, nil);
			};
			midi = MIDIFunc.cc({ arg ... args;
				//args.debug("MENU MIDI Func");
				param.normSet(args[0]/127)
			}).fix.learn;
			param.target.addHalo(\MIDIFunc, param.property, midi);
		}),
		MenuAction(
			if(param.target.getHalo(\MIDIFunc, param.property).notNil) {
				"Clear MIDI (%)".format(param.target.getHalo(\MIDIFunc, param.property).msgNum);
			} {
				"Clear MIDI (not set)";
			},
		   	{
				var midi = param.target.getHalo(\MIDIFunc, param.property);
				if(midi.notNil) {
					midi.free;
					param.target.addHalo(\MIDIFunc, param.property, nil);
				};
			}
		),
		// view is only the label, can't work, must find another way
		//if(param.propertyArray.last == \source) {
			//MenuAction("Convert to bare param")
		//} {
			//MenuAction("Convert to source param", {
			//})
		//},

		MenuAction.separator,
		//if(param.hasCombinator) {
		if(TagSpecDef(\Modulators).list.size > 0) {
			var checkedfun = false; // checked if modulator already added
			if(param.hasCombinator) {
				var combi = param.getCombinator;
				checkedfun = { arg x; combi.inputObjects.includes(x) }
			};
			Menu(*
				TagSpecDef(\Modulators).list.collect { arg asso, idx;
					var child = asso.value;
					MenuAction(asso.key, {
						var combi;
						var pa = param;
						var nextidx;
						if(checkedfun.(child)) { // if already added
							var idx;
							combi = param.getCombinator;
							idx = combi.inputObjects.indexOf(child);
							combi.inputParam.at(idx).set(0);
							combi.inputObjects[idx] = nil;
							param.target.changed(\keyForm, param); // for PbindefEditor
						} {
							if(param.hasCombinator.not) {
								if(param.property.value == \source) {
									pa = param.parent;
								};
								ParamCombinator(pa).setBusMode(true);
							};
							combi = param.getCombinator;
							nextidx = combi.nextFreeInput;
							if(nextidx.notNil) {
								combi.inputParam.at(nextidx).setBus(PlayerWrapper(child).outBus.asMap);
								combi.inputObjects[nextidx] = child;
								//pa.at(\source).set(combi);
								child.play;
								param.target.changed(\keyForm, param); // for PbindefEditor
							} {
								Log(\Param).error("No free input for param % combi %".format(param, combi));
							};
						};
					}).checked_(checkedfun.(child))
				} ++ [
					Menu(*
						TagSpecDef(\BusDef).list.collect { arg asso, idx;
							var child = asso.value;
							MenuAction(asso.key, {
								var combi;
								var pa = param;
								var nextidx;
								if(checkedfun.(child)) { // if already added
									var idx;
									combi = param.getCombinator;
									idx = combi.inputObjects.indexOf(child);
									combi.inputParam.at(idx).set(0);
									combi.inputObjects[idx] = nil;
									param.target.changed(\keyForm, param); // for PbindefEditor
								} {
									if(param.hasCombinator.not) {
										if(param.property.value == \source) {
											pa = param.parent;
										};
										ParamCombinator(pa).setBusMode(true);
									};
									combi = param.getCombinator;
									nextidx = combi.nextFreeInput;
									if(nextidx.notNil) {
										combi.inputParam.at(nextidx).setBus(child.asMap);
										combi.inputObjects[nextidx] = child;
										param.target.changed(\keyForm, param); // for PbindefEditor
									} {
										Log(\Param).error("No free input for param % combi %".format(param, combi));
									};
								};
							})
						};
					).title_("Modulate by bus"),
					newmodmenu,
				];
			).title_("Add modulator")
		} {
			newmodmenu
			//MenuAction("Add modulator", {
				//var combi;
				//var pa = param;
				//if(param.property.value == \source) {
					//pa = param.parent;
				//};
				//combi = ParamCombinator(pa).setBusMode(true);
				//if(TrackDef(\ModulatorRack).parent.notNil) { // if ModulatorRack is loaded
					//// automatically create and connect a modulator
					//var mod;

					//if( TrackDef(\ModulatorRack).existingChildren.size == 0) {
						//TrackDef(\ModulatorRack).addModulator;
					//};
					//mod = TrackDef(\ModulatorRack).existingChildren.first;
					//combi.inputParam.at(0).setBus(mod.outBus.asMap);
					//combi.inputObjects[0] = mod;
					////pa.at(\source).set(combi);
					//mod.play;
					//param.target.changed(\keyForm, param); // for PbindefEditor
				//} {
					//WindowDef(\ParamCombinatorModulatorEditor).front(pa)

				//};
			//})
		},
		if(param.hasCombinator) {
			Menu(
				CustomViewAction(
					View.new.layout_(WindowDef(\ParamCombinatorModulatorEditor).asView(param))
				)
			).title_("Modulator panel")
		},
		MenuAction("Combinator editor", {
			var combi;
			var pa = param;
			if(param.property.value == \source) {
				pa = param.parent;
			};
			combi = ParamCombinator(pa).setBusMode(true);
			WindowDef(( \ParamCombinator_++pa.fullLabel ).asSymbol, WindowDef(\ParamCombinatorEditor)).front(combi);
			//param.set(param.default)
		}),

		MenuAction.separator,
		MenuAction("Param Debug View", { WindowDef(\ParamDebugView).front(param) }),


		MenuAction.separator,
		MenuAction("Edit target", {
			PlayerWrapper(param.target).edit;
		}),
		if(param.spec.isKindOf(ParamBusSpec)) {
			MenuAction("Scope bus", {
				var bus = param.get;
				if(bus.notNil) {
					WindowDef(\ScopeView).front(bus);
				} {
					Log(\Param).debug("bus is nil");
				}
			})
		},

		MenuAction.separator,
		MenuAction("Spec editor", {
			WindowDef(\ParamSpecEditor).front(param)
		})
	].select(_.notNil)
});

WindowDef(\ParamEditor, { arg def, param;
	var midiClearButton;
	VLayout (
		StaticText.new.string_(param.fullLabel),
		Param(Message(param), \inBusMode, ParamBoolSpec()).asButton("Bus mode"),
		BasicButton.new.string_("Override pattern").action_({
			if(param.target.isKindOf(EventPatternProxy)) {
				Pbindef(param.target.key, param.propertyRoot, Pkey(param.propertyRoot));
			} {
				Log(\Param).info("Can't override, not an EventPatternProxy");
			}
		}),
		BasicButton.new.string_("Set Combinator mode").action_({
			var combi;
			combi = ParamCombinator(param).setBusMode(true);
			WindowDef(( \ParamCombinator_++param.fullLabel ).asSymbol, WindowDef(\ParamCombinatorEditor)).front(combi);
			//param.set(param.default)
		}),
		BasicButton.new.string_("Set default").action_({
			param.set(param.default)
		}),
		BasicButton.new.string_("Unset").action_({
			param.unset;
		}),
		HLayout (
			BasicButton.new.string_("MIDI CC learn and map").action_({
				var midi = param.target.getHalo(\MIDIFunc, param.property);
				if(midi.notNil) {
					midi.free;
					param.target.addHalo(\MIDIFunc, param.property, nil);
				};
				midi = MIDIFunc.cc({ arg ... args;
					//args.debug("MENU MIDI Func");
					param.normSet(args[0]/127)
				}).fix.learn;
				param.target.addHalo(\MIDIFunc, param.property, midi);
			}),
			midiClearButton = BasicButton.new.string_(
				if(param.target.getHalo(\MIDIFunc, param.property).notNil) {
					"Clear MIDI (%)".format(param.target.getHalo(\MIDIFunc, param.property).msgNum);
				} {
					"Clear MIDI (not set)";
				}
			).action_({
					var midi = param.target.getHalo(\MIDIFunc, param.property);
					if(midi.notNil) {
						midi.free;
						param.target.addHalo(\MIDIFunc, param.property, nil);
					};
			}),
		),
		StaticText.new.string_(""),
		StaticText.new.string_("Spec editor"),
		WindowDef(\ParamSpecEditor).embedView(def, param),
		nil
	)
});

WindowDef(\ParamEditorSimple, { arg def, param;
	var midiClearButton;
	VLayout (
		ParamGroupLayout.formEntry(param, \full),
		StaticText.new,
		Param(Message(param), \inBusMode, ParamBoolSpec()).asButton("Bus mode"),
		BasicButton.new.string_("Override pattern").action_({
			if(param.target.isKindOf(EventPatternProxy)) {
				Pbindef(param.target.key, param.propertyRoot, Pkey(param.propertyRoot));
			} {
				Log(\Param).info("Can't override, not an EventPatternProxy");
			}
		}),
		BasicButton.new.string_("Set default").action_({
			param.set(param.default)
		}),
		BasicButton.new.string_("Unset").action_({
			param.unset;
		}),
		HLayout (
			BasicButton.new.string_("MIDI CC learn and map").action_({
				var midi = param.target.getHalo(\MIDIFunc, param.property);
				if(midi.notNil) {
					midi.free;
					param.target.addHalo(\MIDIFunc, param.property, nil);
				};
				midi = MIDIFunc.cc({ arg ... args;
					//args.debug("MENU MIDI Func");
					param.normSet(args[0]/127)
				}).fix.learn;
				param.target.addHalo(\MIDIFunc, param.property, midi);
			}),
			midiClearButton = BasicButton.new.string_(
				if(param.target.getHalo(\MIDIFunc, param.property).notNil) {
					"Clear MIDI (%)".format(param.target.getHalo(\MIDIFunc, param.property).msgNum);
				} {
					"Clear MIDI (not set)";
				}
			).action_({
					var midi = param.target.getHalo(\MIDIFunc, param.property);
					if(midi.notNil) {
						midi.free;
						param.target.addHalo(\MIDIFunc, param.property, nil);
					};
			}),
		),
		nil
	)
});

////// Param Spec editors

WindowDef(\ParamControlSpecEditor, { arg def, spec;
	var setspec;
	var speclist;
	setspec = { arg key;
		var newspec;
		newspec = Spec.specs[key];
		Log(\Param).debug("setspec newspec %", newspec);
		if(newspec.notNil) {
			spec.minval = newspec.minval;
			spec.maxval = newspec.maxval;
			spec.default = newspec.default;
			spec.step = newspec.step;
			spec.warp = newspec.warp;
			spec.changed(\set, \minval);
			spec.changed(\set, \maxval);
			spec.changed(\set, \default);
			spec.changed(\set, \step);
			spec.changed(\set, \warp);
		};
		Log(\Param).debug("done setting spec %", newspec);
	};
	speclist = Spec.specs.select({ arg spec; spec.isKindOf(ControlSpec) }).keys.as(Array).sort;
	VLayout(
		HLayout (
			StaticText.new.string_("Load from spec: "),
			PopUpMenu.new.items_(speclist).action_({ arg view;
				setspec.(view.item.asSymbol);
			}).mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				if(buttonNumber == 1) {
					WindowDef(\ListSelectDialog).front(speclist, { arg selected, asso;
						[view, selected, asso].debug("ListSelectDialog");
						defer {
							setspec.(asso.value);
						};
						//view.valueAction = 1;
					}, view.item)
				};
			})
		),
		ParamGroupLayout.formEntry( Param(Message(spec), \minval, \widefreq)),
		ParamGroupLayout.formEntry( Param(Message(spec), \maxval, \widefreq)),
		ParamGroupLayout.formEntry( Param(Message(spec), \default, \widefreq)),
		ParamGroupLayout.formEntry( Param(Message(spec), \step, \widefreq)),
		ParamGroupLayout.formEntry( Param(Message(spec), \warp, TagSpec(
			[
				\lin -> LinearWarp(),
				\exp -> ExponentialWarp()
			]
		))),
	)
});

WindowDef(\ParamSpecEditor, { arg def, param;
	var setspec;
	var speclist;
	var spec = param.spec.copy; // copy to avoid modifying standard specs
	param.spec = spec;
	case(
		{ spec.isKindOf(ControlSpec) }, {
			VLayout (
				ParamGroupLayout.formEntry( param),
				WindowDef(\ParamControlSpecEditor).asView(spec),
				HLayout (
					BasicButton.new.string_("Save spec in object Halo").action_({
						param.target.addSpec(param.propertyRoot, spec.copy)
					}),
					BasicButton.new.string_("Save in Spec library").action_({
						Spec.add(param.propertyRoot, spec.copy);
					}),
				),
				nil
			)
		}, { spec.isKindOf(ParamArraySpec) }, {
			VLayout (
				ParamGroupLayout.formEntry( param),
				VLayout (
					StaticText.new.string_("Levels:").align_(\center),
					WindowDef(\ParamControlSpecEditor).asView(spec.dynamicSpec),
				),
				HLayout (
					BasicButton.new.string_("Set spec to all channels").action_({
						spec.array = spec.dynamicSpec;
					}),
					BasicButton.new.string_("Change spec to ParamEnvSpec").action_({
						param.spec = ParamEnvSpec(spec.dynamicSpec);
						def.front(param);
					}),
					BasicButton.new.string_("Save spec in object Halo").action_({
						param.target.addSpec(param.propertyRoot, spec.copy)
					}),
					BasicButton.new.string_("Save in Spec library").action_({
						Spec.add(param.propertyRoot, spec.copy);
					}),
				),
				nil
			)
		}, { spec.isKindOf(ParamEnvSpec) }, {
			VLayout (
				ParamGroupLayout.formEntry( param),
				HLayout (
					VLayout (
						StaticText.new.string_("Levels:").align_(\center),
						WindowDef(\ParamControlSpecEditor).asView(spec.levels.first),
					),
					VLayout (
						StaticText.new.string_("Times:").align_(\center),
						WindowDef(\ParamControlSpecEditor).asView(spec.times.first),
					)
				),
				HLayout (
					BasicButton.new.string_("Set spec to all channels").action_({
						spec.levels = spec.levels.first;
						spec.times = spec.times.first;
					}),
					BasicButton.new.string_("Change spec to ParamArraySpec").action_({
						param.spec = ParamArraySpec(spec.levels.first);
						def.front(param);
					}),
					BasicButton.new.string_("Save spec in object Halo").action_({
						param.target.addSpec(param.propertyRoot, spec.copy)
					}),
					BasicButton.new.string_("Save in Spec library").action_({
						Spec.add(param.propertyRoot, spec.copy);
					}),
				),
				nil
			)

		}, {
			TextField.new.string_(spec.asCompileString).action_({ arg view;
				param.spec = view.string.interpret;
			})
		}
	);
});
);


////// ParamCombinator
(
TagSpecDef(\ParamCombinatorInput_asMap, { 
	["-" -> 0] ++ TagSpecDef(\ParamCombinatorInput).list.collect({ arg x; x.key -> x.value.asMap }).sort({ arg a, b; a.key < b.key })
	++ TagSpecDef(\BusDef_asMap).list
});

WindowDef(\ParamCombinatorInputPlayerButton, { arg def, combi, idx, tagspecplayer, tagspecmap;
	var tagspec_map = tagspecmap ? TagSpecDef(\ParamCombinatorInput_asMap);
	var tagspec_player = tagspecplayer ? TagSpecDef(\ParamCombinatorInput);
	var pwrapper = PlayerWrapper();
	var input = combi.inputParam.at(idx);
	var menu = input.asBusPopUpMenu(tagspec_map);
	var button = pwrapper.asView;
	//button.rightClickEditorEnabled_(true);
	//button.debug("button");
	// button click events doesnt work for mysterious reasons
	button.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
		//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseUpAction");
		pwrapper.edit;
	});
	menu.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
		//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseUpAction");
		if(buttonNumber == 1) {
			pwrapper.edit;
		}
	});
	menu.followChange(combi.inputParam.target, \set, {
		//"combi input change!".debug;
		//Log(\Param).debug("input.get %", input.getBus);
		if(input.getBus != 0) {
			pwrapper.mapPlayer(
				tagspec_map.unmapKey(input.getBus) !? { arg key;
					Log(\Param).debug("combi editor: key: %, ts%", key, tagspec_player.asDict[ key ]);
					tagspec_player.asDict[ key ]
				} ?? {
					nil
				}
			);
			defer {
				button.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
					//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseUpAction");
					pwrapper.edit;
				});
			};
		} {
			pwrapper.mapPlayer(nil)
		}
	});
	[menu, pwrapper];
});

WindowDef(\ParamCombinatorEditor, { arg def, combi;
	var targetParam = combi.targetParam;
	VLayout (
		PlayerWrapper(combi.proxy).asView,
		targetParam.asView,
		combi.baseParam.asView,
		combi.rangeParam.asView,
		//VLayout(*combi.inputParam.collect(_.asPopUpMenu(TagSpecDef(\BusDef_asMap)))),
		//VLayout(*combi.inputParam.collect(_.asPopUpMenu(TagSpecDef(\BusDef_control_asMap)))),
		if(combi.inBusMode) {
			VLayout(
				*combi.inputParam.collect( { arg input, idx;
					var pwrapper = PlayerWrapper();
					//var tagspec_map = TagSpecDef(\ParamCombinatorInput_asMap);
					//var tagspec_player = TagSpecDef(\ParamCombinatorInput);
					//TagSpec(
						//["-"->0] ++ [\lfo1,\lfo2,\lfo3].collect({ arg x; x -> Ndef(x).asMap })
					//);
					//var menu = input.asBusPopUpMenu(tagspec_map);
					var menu;
					//menu.followChange(combi.inputParam.target, \set, {
						////"combi input change!".debug;
						//Log(\Param).debug("input.get %", input.getBus);
						//if(input.getBus != 0) {
							//pwrapper.mapPlayer(
								//tagspec_map.unmapKey(input.getBus) !? { arg key;
									//Log(\Param).debug("combi editor: key: %, ts%", key, tagspec_player.asDict[ key ]);
									//tagspec_player.asDict[ key ]
								//} ?? {
									//nil
								//}
							//)
						//} {
							//pwrapper.mapPlayer(nil)
						//}
					//});
					#menu, pwrapper = WindowDef(\ParamCombinatorInputPlayerButton).asView(combi, idx);
					HLayout (
						//menu,
						//pwrapper.asView,
						HLayout(
							menu, 
							pwrapper
						),
						Button.new.string_("Edit").action_({
							if(pwrapper.target.notNil) {
								pwrapper.edit;
							}
							//WindowDef(\NdefEditor).front(Ndef(tagspec.list[ tagspec.unmapIndex(input.get) ].key))
						})
					)
				})
			);
		} {
			combi.inputParam.asView;
		},
		nil
		//Param(Pdef(\param), \xlpfr).asPopUpMenu([\bla,\rah]),
	)
});
);


(
WindowDef(\ParamCombinatorModulatorEditor, { arg def, param;
	if(param.hasCombinator) {
		var combi = param.getCombinator;
		VLayout (*
			[
				HLayout (
					param.asView,
					BasicButton.new.string_("Modulator Rack").action_({
						TrackDef(\ModulatorRack).edit;
					}),
					PlayerWrapper(combi.proxy),
				)
			] ++
			combi.inputObjects.collect { arg obj, idx;
				var player = PlayerWrapper(obj);
				var inputp = Param(Message(combi), \inputObjects -> idx, TagSpecDef(\Modulators));
				HLayout(
					player.asView,
					inputp.asPopUpMenu.addAction({
						obj = combi.inputObjects[idx];
						[ obj].debug("obj");
						[ PlayerWrapper(obj).outBus.asMap ].debug("bus");
						player.mapPlayer(obj);
						combi.inputParam.at(idx).setBus(PlayerWrapper(obj).outBus.asMap);
						param.target.changed(\keyForm, param); // for PbindefEditor
					}),
					combi.inputParam.at(idx).copy.spec_(ParamMappedBusSpec()).asPopUpMenu,
					combi.rangeParam.at(idx).asSlider.orientation_(\horizontal).fixedWidth_(200),
					BasicButton.new.string_("Edit").maxWidth_(30).action_({
						player.edit;
					}),
				)
			
			
			} ++ [ nil ];
		)
	} {
		BasicButton.new.string_("Add Combinator").action_({
			var combi = ParamCombinator(param).setBusMode(true);
			"add it now!".debug;
			def.front(param);
		})
	}
});
);

////// Server
(
WindowDef(\ServerVolumeEditor, { arg def, server;
	var slider;
	var stack, meterout;
	server = server ?? { Server.default };
	def.windowDo { arg win;
		win.bounds = Rect(100,100,70,440);
	};
	{
		slider.focus(true);
	}.defer(1/100);
	meterout = CompactServerMeterView(server, 0,2, 0);
	slider = Slider.new.orientation_(\vertical);
	slider.mapParam( Param(server.volume, \volume, \db) );
	slider.background_(Color.clear);
	stack = StackLayout(
		slider, 
		meterout.view,
	).mode_(\stackAll);
	VLayout (
		Param(server.volume, \volume, \db).asTextField(4),
		stack,
	)
});
);

////// Debug

(
WindowDef(\ParamDebugView, { arg def, param;
	var makeDebugView;
	var lay;
	makeDebugView = { arg param;
		GridLayout.rows(
			[
				StaticText.new.string_("fullLabel"),
				TextField.new.string_(param.fullLabel),
			],
			[
				StaticText.new.string_("target"),
				TextField.new.string_(param.target.asCompileString),
			],
			[
				StaticText.new.string_("property"),
				TextField.new.string_(param.property.asCompileString),
			],
			[
				StaticText.new.string_("spec"),
				TextField.new.string_(param.spec.asCompileString),
			],
			[
				StaticText.new.string_("wrapper"),
				TextField.new.string_(param.wrapper.asCompileString),
			],
			[
				StaticText.new.string_("accessor"),
				TextField.new.string_(param.accessor.key),
			],
			[
				StaticText.new.string_("get"),
				TextField.new.string_(param.get.asCompileString),
			],
			[
				StaticText.new.string_("getBus"),
				TextField.new.string_(param.getBus.asCompileString),
			],
			[
				StaticText.new.string_("getRaw"),
				TextField.new.string_(param.getRaw.asCompileString),
			],
			[
				StaticText.new.string_("getVal"),
				TextField.new.string_(param.getVal.asCompileString),
			],
			[
				StaticText.new.string_("hasCombinator"),
				TextField.new.string_(param.hasCombinator),
			],
			[
				StaticText.new.string_("inBusMode"),
				TextField.new.string_(param.inBusMode.asCompileString),
			],
			[
				StaticText.new.string_("isSet"),
				TextField.new.string_(param.isSet),
			],
			[
				StaticText.new.string_("source"),
				if(param.target.isKindOf(Pbindef)) {
					TextField.new.string_(param.target.source.at(param.propertyRoot).source)
				} 
			],
			[
				nil
			],
		)
	};
	if(param.isNil) {
		Button.new.string_("param is nil")
	} {

		lay = if(param.parent.notNil) {
			HLayout (
				makeDebugView.(param),
				makeDebugView.(param.parent),
			)
		} {
			makeDebugView.(param)
		};
		VLayout (
			HLayout (
				BasicButton.new.string_("copy param in ~param").action_({
					~param = param;
				}),
				BasicButton.new.string_("refresh").action_({
					def.front(param);
				}),
			),
			lay,
		)
	};
});
);
