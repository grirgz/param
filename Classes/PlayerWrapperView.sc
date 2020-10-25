
//PlayerWrapperView : ObjectGui {
// FIXME: maybe use BasicButton to simplify implementationjgTgT
PlayerWrapperView {
	var <>states;
	var player;
	var <>button;
	var skipjack;
	var <>pollRate = 1;
	var <>label;
	var <model, >view;
	*new { arg model;
		^super.new.init(model);
	}

	init { arg xmodel;
		this.model = xmodel;
	}

	// FIXME: this is dirty, a layout is not a view
	// but require to remember if class is a layout or a view
	// maybe better would be for view to return a view and layout return a layout
	view { ^this.layout }

	layout { arg parent;
		var lay = HLayout(
			button = Button.new.action_({ arg view;
				switch(view.value,
					1, {
						//[view.value, model].debug("play");
						model.play;
					},
					0, {
						//[view.value, model].debug("stop");
						model.stop;
					}
				);
			})
		);
		lay.addUniqueMethod(\button, { button }); // FIXME: why is button wrapped in a layout ?
		lay.addUniqueMethod(\parentView, { this }); // FIXME: why is button wrapped in a layout ?
		this.makeUpdater;
		this.update;
		^lay;
	}

	makeUpdater {
		skipjack = SkipJack({
			this.update;
		}, pollRate + (pollRate/2).rand, { 
			//button.isClosed.debug("SkipJack: button isClosed?")
			button.isClosed;
		});
	}

	model_ { arg val;
		if(val.isKindOf(PlayerWrapper).not) {
			val = PlayerWrapper(val)
		};
		if(val.notNil) {
			model = val;
		} {
			skipjack.stop;
		}
	}

	getStates { arg str="";
		// FIXME: uncomment debug lines to see that the SkipJack continue to run
		//[ str ++ " ▶" ].debug("getStates");
		if(states.notNil) {
			^states.(str);
		} {
			^[
				[ str ++ " ▶", Color.black, Color.white ],
				[ str ++ " ||", Color.black, ParamViewToolBox.color_ligth ],
			];
		}
	}

	update { arg changed, changer ... args;

		//[changed, changer, args].debug("changed, changer");

        if(changer !== this) {  
			var xlabel;
			//model.isPlaying.debug("isPlaying?");
			if(model.isKindOf(PlayerWrapper)) {
				var playing;
				xlabel = label ? model.label;
				//label.asCompileString.debug("PlayerWrapperView: getlabel");
				button.states = this.getStates(xlabel);
				playing = model.isPlaying;
				if(playing.notNil and: {playing} ) {
					button.value = 1
				} {
					button.value = 0
				};
			} {
				xlabel = label;
				button.states = this.getStates(xlabel);
				button.value = 0;
			}
		}
	}
}

PlayerWrapperSelectorView : PlayerWrapperView {
	var <>states;
	var player;
	var <>button;
	var skipjack;
	var pollRate = 1;
	var <>label;
	var <>labelView;
	var <>selectAction;
	var selected;
	var <>color_selected, <>color_deselected;


	*initClass {
	}

	*new { arg model;
		^super.new(model).initPlayerWrapperSelectorView;
	}

	initPlayerWrapperSelectorView {
		color_selected = ParamViewToolBox.color_dark;
		color_deselected = ParamViewToolBox.color_ligth;
	}

	// FIXME: this is dirty, a layout is not a view
	// but require to remember if class is a layout or a view
	// maybe better would be for view to return a view and layout return a layout

	layout {
		^this.makeLayout;
	}

	makeLayout { arg parent;
		var lay;
		labelView = StaticText.new.string_("bla");
		lay = VLayout(
			button = Button.new.action_({ arg view;
				switch(view.value,
					1, {
						//[view.value, model].debug("play");
						model.play;
					},
					0, {
						//[view.value, model].debug("stop");
						model.stop;
					}
				);
			}).maxWidth_(50),
			labelView,
		);
		view = View.new.layout_(lay.margins_(0)).background_(ParamViewToolBox.color_ligth).maxSize_(200@30).mouseDownAction_({ 
			arg xview, x, y, modifiers, buttonNumber, clickCount;
			[xview, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			this.selectAction.(this, view);
		});
		view.addUniqueMethod(\button, { button });
		view.addUniqueMethod(\labelView, { labelView });
		view.addUniqueMethod(\selected, { arg me, x; this.selected });
		view.addUniqueMethod(\selected_, { arg me, x; this.selected = x });
		view.addUniqueMethod(\parentView, { this });
		this.makeUpdater;
		this.update;
		^view;
	}

	view { 
		if(view.isNil) {
			this.makeLayout;
		};
		^view;
	}

	asView {
		^this.view;
	}

	selected_ { arg val;
		selected = val;
		[selected, val].debug( "PlayerWrapperSelectorView.selected" );
		if(val == true) {
			this.view.debug("seltrue");
			this.view.background_(color_selected);
		} {
			this.view.debug("selfalse");
			this.view.background_(color_deselected);
		};
	}

	selected {
		^selected;
	}

	makeUpdater {
		skipjack = SkipJack({
			this.update;
		}, pollRate + (pollRate/2).rand, { 
			//button.isClosed.debug("SkipJack: button isClosed?")
			button.isClosed;
		});
	}

	model_ { arg val;
		if(val.isKindOf(PlayerWrapper).not) {
			val = PlayerWrapper(val)
		};
		if(val.notNil) {
			model = val;
		} {
			skipjack.stop;
		}
	}

	getStates { arg str="";
		// FIXME: uncomment debug lines to see that the SkipJack continue to run
		//[ str ++ " ▶" ].debug("getStates");
		if(states.notNil) {
			^states.(str);
		} {
			^[
				[ str ++ " ▶", Color.black, Color.white ],
				[ str ++ " ||", Color.black, ParamViewToolBox.color_ligth ],
			];
		}
	}

	update { arg changed, changer ... args;

		//[changed, changer, args].debug("changed, changer");

        if(changer !== this) {  
			var xlabel;
			//model.isPlaying.debug("isPlaying?");
			if(model.isKindOf(PlayerWrapper)) {
				var playing;
				xlabel = label ? model.label;
				//label.asCompileString.debug("PlayerWrapperView: getlabel");
				button.states = this.getStates;
				playing = model.isPlaying;
				if(playing.notNil and: {playing} ) {
					button.value = 1
				} {
					button.value = 0
				};
				labelView.string = xlabel ++ " ";
			} {
				xlabel = label;
				button.states = this.getStates;
				labelView.string = xlabel ++ " ";
				button.value = 0;
			}
		}
	}
}

PlayerWrapperGridCellView : PlayerWrapperView {
	var <>states;
	var player;
	var <>button;
	var skipjack;
	var pollRate = 1;
	var <>label;
	var <>labelView;
	var <>selectAction; // hook to outer selector system which organize deselection of siblings
	var selected;
	var <>color_selected, <>color_deselected;
	var <>color_empty;
	var <>color_active;


	*initClass {
	}

	*new { arg model;
		^super.new(model).initPlayerWrapperSelectorView;
	}

	initPlayerWrapperSelectorView {
		color_selected = Color.yellow;
		color_deselected = ParamViewToolBox.color_dark;
		color_empty = Color.gray;
		color_active = ParamViewToolBox.color_ligth;
	}

	// FIXME: this is dirty, a layout is not a view
	// but require to remember if class is a layout or a view
	// maybe better would be for view to return a view and layout return a layout

	layout {
		^this.makeLayout;
	}

	makeLayout { arg parent;
		var lay;
		labelView = StaticText.new.string_("bla");
		lay = HLayout(
			button = Button.new.action_({ arg view;
				switch(view.value,
					1, {
						//[view.value, model].debug("play");
						model.play;
					},
					0, {
						//[view.value, model].debug("stop");
						model.stop;
					}
				);
			}).maxWidth_(50),
			labelView,
		);
		view = View.new.layout_(lay.margins_(0)).background_(ParamViewToolBox.color_ligth).maxSize_(200@30).mouseDownAction_({ 
			arg xview, x, y, modifiers, buttonNumber, clickCount;
			[xview, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			this.selectAction.(this, view);
		});
		view.addUniqueMethod(\button, { button });
		view.addUniqueMethod(\labelView, { labelView });
		view.addUniqueMethod(\selected, { arg me, x; this.selected });
		view.addUniqueMethod(\selected_, { arg me, x; this.selected = x });
		view.addUniqueMethod(\parentView, { this });
		this.makeUpdater;
		this.update;
		^view;
	}

	view { 
		if(view.isNil) {
			this.makeLayout;
		};
		^view;
	}

	asView {
		^this.view;
	}

	selected_ { arg val;
		selected = val;
		[selected, val].debug( "PlayerWrapperSelectorView.selected" );
		this.update;
		//if(val == true) {
			//this.view.debug("seltrue");
			//this.view.background_(color_selected);
		//} {
			//this.view.debug("selfalse");
			//this.view.background_(color_deselected);
		//};
	}

	selected {
		^selected;
	}

	makeUpdater {
		skipjack = SkipJack({
			this.update;
		}, pollRate + (pollRate/2).rand, { 
			//button.isClosed.debug("SkipJack: button isClosed?")
			button.isClosed;
		});
	}

	model_ { arg val;
		if(val.isKindOf(PlayerWrapper).not or: { val.isKindOf(ProtoClass).not }) {
			"c fini".debug(val);
			//val = PlayerWrapper(val)
		};
		if(val.notNil) {
			model = val;
		} {
			skipjack.stop;
		}
	}

	getStates { arg str="";
		// FIXME: uncomment debug lines to see that the SkipJack continue to run
		//[ str ++ " ▶" ].debug("getStates");
		if(states.notNil) {
			^states.(str);
		} {
			^[
				[ str ++ " ▶", Color.black, Color.white ],
				[ str ++ " ||", Color.black, ParamViewToolBox.color_ligth ],
			];
		}
	}

	update { arg changed, changer ... args;

		//[changed, changer, args].debug("changed, changer");

        if(changer !== this) {  
			var xlabel;
			//model.isPlaying.debug("isPlaying?");
			if(model.isKindOf(PlayerWrapper) or: { model.isKindOf(ProtoClass) }) {
				var playing;
				xlabel = label ? model.label;
				//label.asCompileString.debug("PlayerWrapperView: getlabel");
				button.states = this.getStates;
				if(selected == true) {
					this.view.background_(color_selected);
				} {
					if(model.isEmpty == true) {
						this.view.background_(color_empty);
					} {
						if(model.isActive == true) {
							this.view.background_(color_active);
						} {
							this.view.background_(color_deselected);
						};				
					};
				};
				playing = model.isPlaying;
				//model.isPlaying.debug("isPlaying?");
				if(playing.notNil and: {playing} ) {
					button.value = 1
				} {
					button.value = 0
				};
				labelView.string = xlabel ++ " ";
			} {
				xlabel = label;
				button.states = this.getStates;
				labelView.string = xlabel ++ " ";
				button.value = 0;
			}
		}
	}
}
