
//PlayerWrapperView : ObjectGui {
// FIXME: maybe use BasicButton to simplify implementation
PlayerWrapperView {
	var <>states;
	var player;
	var <>button;
	var skipjack;
	var <>pollRate = 1;
	var <>label;
	var <model, >view;
	var follower;
	*new { arg model;
		^super.new.init(model);
	}

	init { arg xmodel;
		this.model = xmodel;
	}

	view { 
		if(view.isNil) {
			this.makeLayout;
		};
		^view;
	}

	layout {
		^this.view;
	}

	asView {
		^this.view;
	}

	// FIXME: this is dirty, a layout is not a view
	// but require to remember if class is a layout or a view
	// maybe better would be for view to return a view and layout return a layout

	makeLayout { arg parent;
		var lay = HLayout(
			button = Button.new.action_({ arg view;
				// value first increment then action is called
				switch(view.value,
					0+1, {
						[view.value, model].debug("was stopped: play");
						model.play;
					},
					1+1, {
						[view.value, model].debug("user played: stop");
						model.stop;
					},
					2+1, {
						[view.value, model].debug("was playing: stop");
						model.stop;
					},
					0, {
						[view.value, model].debug("user stopped: play");
						model.play;
					}
				);
			})
		);
		lay = button; // backward compat
		lay.addUniqueMethod(\maxWidth_, { arg v, x; 
			Log(\Param).debug("maxWidth_ %",[v,x].asCompileString);
			button.maxWidth_(x); 
			v;
		});
		lay.addUniqueMethod(\button, { button }); // FIXME: why is button wrapped in a layout ?
		lay.addUniqueMethod(\parentView, { this }); 
		lay.addUniqueMethod(\rightClickEditorEnabled_, { arg self, val=true;
			//Log(\Param).debug("enableRightClickEditor_ %", val);
			if(val == true) {
				button.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
					//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
					if(buttonNumber == 1) {
						this.model.edit;
					} 
				})
			} {
				button.mouseDownAction_({})
			};
			self;
		});
		this.makeUpdater;
		this.update;
		//view = lay;
		view = button;
		^view;
	}


	makeUpdater {
		skipjack = SkipJack({
			this.update;
		}, pollRate + (pollRate/2).rand, { 
			//button.isClosed.debug("SkipJack: button isClosed?")
			button.isClosed;
		});
		this.makeDependentListener;
	}

	makeDependentListener {
		var target = model;
		// support for ProtoClass
		if(model.isKindOf(PlayerWrapper)) {
			target = model.target;
		}; 
		target.removeDependant(follower);
		follower = { arg obj, changed, status;
			//[obj, changed, status].debug("follower: args");
			if(changed == \PlayerWrapper) {
				defer {
					var butval = button.value;
					button.states = this.getStates(model.label);
					button.value = switch(status,
						\stopped, { 0 },
						\playing, { 2 },
						\userPlayed, { 
							if(butval == 2) { 2 } { 1 }
						},
						\userStopped, { 
							if(butval == 0) { 0 } { 3 }
						},
						{ 0 },
					);
				}
			};
		};
		target.addDependant(follower);
		button.onClose({target.removeDependant(follower)});
		
	}


	model_ { arg val;
		if(val.isKindOf(PlayerWrapper).not) {
			val = PlayerWrapper(val)
		};
		if(val.notNil) {
			model = val;
		} {
			skipjack.stop;
		};
		if(button.notNil) { // else fail when model is set before the layout in init
			this.makeDependentListener;
		};
	}

	getStates { arg str="";
		// FIXME: uncomment debug lines to see that the SkipJack continue to run
		//[ str ++ " ▶" ].debug("getStates");
		if(states.notNil) {
			^states.(str);
		} {
			^[
				[ str ++ " ▶", Color.black, ParamViewToolBox.color_stopped ],
				[ str ++ " ▶", Color.black, ParamViewToolBox.color_userPlayed ],
				[ str ++ " ||", Color.black, ParamViewToolBox.color_playing ],
				[ str ++ " ||", Color.black, ParamViewToolBox.color_userStopped ],
			];
		}
	}

	update { arg changed, changer ... args;
		var xlabel;
		var playing;

		//[changed, changer, args].debug("changed, changer");

        if(changer !== this) {  
			//model.isPlaying.debug("isPlaying?");
			if(model.isKindOf(PlayerWrapper)) {
				//label.asCompileString.debug("PlayerWrapperView: getlabel");
				if([0,2].includes(button.value)) {
					xlabel = label ? model.label;
					button.states = this.getStates(xlabel);
					playing = model.isPlaying;
					if(playing.notNil and: {playing} ) {
						button.value = 2
					} {
						button.value = 0
					};
				}
			} {
				//xlabel = label;
				//button.states = this.getStates(xlabel);
				//button.value = 0;
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
		color_empty = Color.gray;
		color_deselected = ParamViewToolBox.color_userPlayed;
		color_active = ParamViewToolBox.color_playing;
		color_selected = Color.yellow;
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
				// value first increment then action is called
				switch(view.value,
					0+1, {
						[view.value, model].debug("was stopped: play");
						model.play;
					},
					1+1, {
						[view.value, model].debug("user played: stop");
						model.stop;
					},
					2+1, {
						[view.value, model].debug("was playing: stop");
						model.stop;
					},
					0, {
						[view.value, model].debug("user stopped: play");
						model.play;
					}
				);
			}).maxWidth_(50),
			labelView,
		);
		view = View.new.layout_(lay.margins_(0)).background_(ParamViewToolBox.color_light).maxSize_(200@30).mouseDownAction_({ 
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
		this.makeDependentListener;
	}

	model_ { arg val;
		if(val.isKindOf(PlayerWrapper).not or: { val.isKindOf(ProtoClass).not }) {
			//"c fini".debug(val);
			//val = PlayerWrapper(val)
		};
		if(val.notNil) {
			model = val;
		} {
			skipjack.stop;
		};
		if(button.notNil) { // else fail when model is set before the layout in init
			this.makeDependentListener;
		};
	}

	getStates { arg str="";
		// FIXME: uncomment debug lines to see that the SkipJack continue to run
		//[ str ++ " ▶" ].debug("getStates");
		if(states.notNil) {
			^states.(str);
		} {
			^[
				[ str ++ " ▶", Color.black, ParamViewToolBox.color_stopped ],
				[ str ++ " ▶", Color.black, ParamViewToolBox.color_userPlayed ],
				[ str ++ " ||", Color.black, ParamViewToolBox.color_playing ],
				[ str ++ " ||", Color.black, ParamViewToolBox.color_userStopped ],
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
				//xlabel = label ? model.label;
				//label.asCompileString.debug("PlayerWrapperView: getlabel");
				//button.states = this.getStates;
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
				//model.isPlaying.debug("isPlaying?");
				if([0,2].includes(button.value)) {
					xlabel = label ? model.label;
					button.states = this.getStates;
					playing = model.isPlaying;
					if(playing.notNil and: {playing} ) {
						button.value = 2
					} {
						button.value = 0
					};
					labelView.string = xlabel ++ " ";
				};
			} {
				xlabel = label;
				button.states = this.getStates;
				labelView.string = xlabel ++ " ";
				button.value = 0;
			}
		}
	}
}

/////////

PlayerWrapperSelectorView : PlayerWrapperView {
	// FIXME: this is outdated relative to PlayerWrapperView 4 state
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
		color_deselected = ParamViewToolBox.color_light;
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
		view = View.new.layout_(lay.margins_(0)).background_(ParamViewToolBox.color_light).maxSize_(200@30).mouseDownAction_({ 
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
		//[ str ++ " ▶" ].debug("getStates");
		str = ""; // small button
		if(states.notNil) {
			^states.(str);
		} {
			^[
				[ str ++ " ▶", Color.black, Color.white ],
				[ str ++ " ||", Color.black, ParamViewToolBox.color_light ],
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

