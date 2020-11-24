
(
WindowDef(\OverlayMenu, { arg def, view, x, y, fun;
	var inview, layout;
	x = x ? 600;
	y = y ? 600;
	Log(\Param).debug("xy: %, %", x, y);
	[def, def.key, def.window].debug("def");
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
		"heeee".debug;
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 200,100);
		win.endFrontAction = { win.close };
	};
	fun.value(def, view, x, y);
	
}).border_(false); // border should be specified at window creation, so too late in WindowDef code body
);

(
WindowDef(\ParamGenericOverlayMenu, { arg def, view, x, y, param;
	WindowDef(\OverlayMenu).embedView(def, view, x, y, {
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
			BasicButton.new.string_("Set default").action_({
				param.set(param.default)
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
	})
}).border_(false)
);

(
	WindowDef(\ParamSpecEditor, { arg def, param;
		var spec = param.spec.copy; // copy to avoid modifying standard specs
		param.spec = spec;
		case(
			{ spec.isKindOf(ControlSpec) }, {
				VLayout (
					ParamGroupLayout.formEntry( param),
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
						Spec.add(param.property, spec.copy);
					}),
					nil
				)
			}, {
				StaticText.new.string_(spec.asCompileString)
			}
		);
		
	});
);

// END

// test menu
(
WindowDef(\OverlayMenu).front(nil,nil,nil,{ arg def;
	BasicButton.new.string_("kjkj")

})
)

// usage
(
	WindowDef(\overlaydemo, { arg def;

		BasicButton.new.string_("open menu").mouseDownAction_({ arg but, x, y, modifiers, buttonNumber, clickCount;
			[but, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

			if(buttonNumber == 1) { // if right-click

				WindowDef(\OverlayMenu).front(but, x, y, { arg def;
					BasicButton.new.string_("entry 1").action_({
						def.closeWindow;
					})
				} )

			}
		})

	}).front;
)


// define custom menu
(
WindowDef(\MyOverlayMenu, { arg def, view, x, y, fun;
	WindowDef(\OverlayMenu).embedView(def, view, x, y, {
		BasicButton.new.string_("kjkj")
	})
}).border_(false);
)

// use it
(
	WindowDef(\overlaydemo, { arg def;

		BasicButton.new.string_("open menu").mouseDownAction_({ arg but, x, y, modifiers, buttonNumber, clickCount;
			[but, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

			if(buttonNumber == 1) { // if right-click

				WindowDef(\MyOverlayMenu).front(but, x, y)

			}
		})

	}).front;
)


///////////

WindowDef(\ParamGenericOverlayMenu).front(nil,nil,nil, Param(Pdef(\saw), \rlpf))
