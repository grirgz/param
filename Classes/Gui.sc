ParamGroupLayout {
	*new { arg group;
		^this.windowize(this.two_panes(group));
	}

	*windowize { arg layout, alwaysOnTop=true;
		var win = Window.new;
		win.layout = layout;
		win.alwaysOnTop = alwaysOnTop;
		win.front;
		^win;
	}

	*singleWindowize {
		// TODO: the window close before recreating if open
	}

	*two_panes { arg pg;

		var layout;
		var gridlayout;
		var biglayout;
		var scalarlist, biglist;
		var layout_type;

		scalarlist = pg.select({ arg param; 
			param.type == \scalar;
		});
		biglist = pg.select({ arg param;
			param.type != \scalar;
		});

		gridlayout = GridLayout.rows(*
			scalarlist.collect({ arg param;
				[
					param.asStaticTextLabel,
					param.asSlider.orientation_(\horizontal),
					param.asTextField,
				]
			})
		);
		gridlayout.setColumnStretch(1,1);

		// chipotage
		if(biglist.size < 5 and: { scalarlist.size < 6 } ) {
			layout_type = VLayout;
		} {
			layout_type = HLayout;
		};

		biglayout = VLayout(*
			biglist.collect({ arg param;
				VLayout(
					param.asStaticTextLabel,
					param.asView,
					param.asTextField,
				)
			})
		);

		layout = layout_type.new(
			gridlayout,
			biglayout
		);
		^layout;
	}
}

WindowLayout {
	*new { arg fun;
		var window = Window.new;
		var layout;
		layout = fun.value(window);
		window.layout = fun;
		//window.alwaysOnTop = true;
		window.front;
	}
}
