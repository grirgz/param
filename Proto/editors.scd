
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
			Param(Message(target), \quant).asTextField.maxWidth_(80),
		),
		ParamGroupLayout.two_panes(target.asParamGroup)
	);
});
);

(
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

WindowDef(\ParamSpecEditor, { arg def, param;
	var spec = param.spec.copy; // copy to avoid modifying standard specs
	param.spec = spec;
	case(
		{ spec.isKindOf(ControlSpec) }, {
			VLayout (
				ParamGroupLayout.formEntry( param),
				HLayout (
					StaticText.new.string_("Load from spec: "),
					PopUpMenu.new.items_(Spec.specs.keys.as(Array)).action_({
						arg view;
						var key = view.item.asSymbol;
						var newspec = Spec.specs[key];
						if(newspec.notNil) {
							spec.minval = newspec.minval;
							spec.maxval = newspec.maxval;
							spec.default = newspec.default;
							spec.step = newspec.step;
							spec.warp = newspec.warp;
						};
						Log(\Param).debug("done setting spec %", newspec);
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
				BasicButton.new.string_("Save in Spec library").action_({
					Spec.add(param.propertyRoot, spec.copy);
				}),
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
			button.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseUpAction");
				pwrapper.edit;
			});
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
					var tagspec_map = TagSpecDef(\ParamCombinatorInput_asMap);
					var tagspec_player = TagSpecDef(\ParamCombinatorInput);
					//TagSpec(
						//["-"->0] ++ [\lfo1,\lfo2,\lfo3].collect({ arg x; x -> Ndef(x).asMap })
					//);
					var menu = input.asBusPopUpMenu(tagspec_map);
					menu.followChange(combi.inputParam.target, \set, {
						//"combi input change!".debug;
						Log(\Param).debug("input.get %", input.getBus);
						if(input.getBus != 0) {
							pwrapper.mapPlayer(
								tagspec_map.unmapKey(input.getBus) !? { arg key;
									Log(\Param).debug("combi editor: key: %, ts%", key, tagspec_player.asDict[ key ]);
									tagspec_player.asDict[ key ]
								} ?? {
									nil
								}
							)
						} {
							pwrapper.mapPlayer(nil)
						}
					});
					HLayout (
						//menu,
						//pwrapper.asView,
						HLayout(*WindowDef(\ParamCombinatorInputPlayerButton).asView(combi, idx).collect(_.asView)),
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


