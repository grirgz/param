
(
WindowDef(\OverlayMenu, { arg def, menuView, view, x=0, y=0;
	var inview, layout;
	[x, y].debug("xy");
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(600,600,600,600);
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y - ( ab.height/2 ), 200,100);
		win.endFrontAction = { win.close };
	};
	menuView.(def, view, x, y);
	
}).border_(false);
)

// END

(
WindowDef(\OverlayMenu).front({ arg def;
	BasicButton.new.string_("kjkj")

})
)

(
	WindowDef(\overlaydemo, {

		BasicButton.new.string_("open menu").mouseDownAction_({ arg but, x, y, modifiers, buttonNumber, clickCount;
			[but, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

			if(buttonNumber == 1) { // if right-click

				WindowDef(\OverlayMenu).front({ arg def;
					BasicButton.new.string_("entry 1").action_({
						def.closeWindow;
					})
				}, but, x, y )

			}
		})

	}).front;
)
