
/////////////////////////////////////////////////////////////////////////////////////


// TODO: this old code should be removed


// To be converted in class in Gui.sc
~fxblock = { arg name, player, mixparam, pg;
	VLayout(
		View.new.layout_(

			HLayout(
				//player.asView, //.background_(ParamViewToolBox.color_dark);
				~onoff.(player),
				//StaticText.new.string_(name).background_(ParamViewToolBox.color_dark),
				StaticText.new.string_(name),
				//mixparam.asSlider.orientation_(\horizontal).background_(ParamViewToolBox.color_dark),
				mixparam.asSlider.orientation_(\horizontal),
			).margins_(2),
		).background_(ParamViewToolBox.color_dark),
		ParamGroupLayout.two_panes(pg, \property)
	);
};

~onoff = { arg player;
	var p = PlayerWrapperView(player);
	var view = p.view;
	p.states = { [
		["Off", Color.gray, Color.white],
		["On", Color.black, Color.white], 
	] };
	p.button.fixedSize = 30@20;
	p.button.background_(ParamViewToolBox.color_dark);
	view;
};

