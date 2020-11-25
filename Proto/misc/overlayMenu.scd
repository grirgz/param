
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
		//win.alwaysOnTop = true; // would be nice but this make error don't know why
		win.endFrontAction = { win.close };
	};
	fun.value(def, view, x, y);
	
}).border_(false); // border should be specified at window creation, so too late in WindowDef code body
);


(
WindowDef(\ParamGenericOverlayMenu, { arg def, view, x, y, param;
	WindowDef(\OverlayMenu).embedView(def, view, x, y, {
		WindowDef(\ParamEditor).embedView(def, param)
	})
}).border_(false)
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
