////////////////////// Basic building blocks

ParamView {

	classvar <>color_dark;
	classvar <>color_ligth;

	*initClass {
		//color_ligth = Color.newHex("63AFD0");
		//color_dark = Color.newHex("0772A1");

		color_ligth = Color.newHex("5CCCCC");
		color_dark = Color.newHex("009999");
	}

	*label_knob { arg name;
		var view;
		var val, label, control;
		label = StaticText.new.string_(name).align_(\center);
		control = Knob.new;
		val = TextField.new;
		view = View.new;
		view.layout = VLayout(
			label,
			control,
			val,
		);
		view.layout.margins_(0);
		view.addUniqueMethod(\mapParam, { arg view, param;
			param.debug("ParamView.knob.mapParam");
			control.mapParam(param);
			val.mapParam(param);
			view;
		});
		view.addUniqueMethod(\unmapParam, { arg view, param;
			control.unmapParam;
			val.unmapParam;
			view;
		});
		^view;
	}

	*knob { 
		var view;
		var val, label, control;
		label = StaticText.new;
		control = Knob.new;
		val = TextField.new;
		view = View.new;
		view.layout = VLayout(
			label,
			control,
			val
		);
		view.addUniqueMethod(\mapParam, { arg view, param;
			param.debug("ParamView.knob.mapParam");
			label.mapParamLabel(param).align_(\center);
			control.mapParam(param);
			val.mapParam(param).align_(\right);
			view;
		});
		view.addUniqueMethod(\unmapParam, { arg view, param;
			label.unmapParam;
			control.unmapParam;
			val.unmapParam;
			view;
		});
		^view;
	}
}


ListParamLayout {

	*new { arg param, makecell;
		super.new.init(param, makecell)
	}

	*button { arg param;
		^super.new.init(param, { arg param;
			param.asButton;
		});
	}

	*slider { arg param;
		^super.new.init(param, { arg param;
			param.asSlider;
		});
	}

	*knob { arg param;
		^super.new.init(param, { arg subparam;
			subparam.asKnob;
		});
	}

	*valuePopup { arg param, keys;
		^super.new.init(param, { arg subparam;
			var pm = PopUpMenu.new;
			pm.items = keys;
			pm.action = {
				subparam.set(pm.value)
			};
			pm.onChange(subparam.target, \set, {
				// TODO: do not change the whole row when just one value is updated!
				var val;
				//"there is change! my lord!".debug;
				pm.value = pm.items.detectIndex({ arg x; x == subparam.get })
			});
			pm.value = pm.items.detectIndex({ arg x; x == subparam.get });
			pm;
		});
	}

	*indexPopup { arg param, keys;
		^super.new.init(param, { arg subparam;
			var pm = PopUpMenu.new;
			pm.items = keys;
			pm.action = {
				subparam.set(pm.value)
			};
			pm.onChange(subparam.target, \set, {
				pm.value = subparam.get;
			});
			pm;
		});
	}

	*addCursor { arg x, view, param, on, off;
		^view.onChange(param.target, \cursor, { arg view ...args;
			//[args[2], x].debug("bbb");
			if(args[2] == x or: { args[2].isNil }) {
				// AppClock doesnt have the same tempo of the pattern
				//		but s.latency is in seconds and appclock has tempo 1, so this works!!!
				// FIXME: how to specify another server ?
				Task{
					Server.default.latency.wait;
					if(view.isClosed.not) {
						if(args[3] == 1) {
							on.value(view, x, param, args[4]);
						} {
							off.value(view, x, param, args[4]);
						};
					};
					nil
				}.play(AppClock);
			};
			//args.debug("cursor!!");
		})
	}

	*cursor { arg param;
		^super.new.init(param, { arg param, x;
			Button.new
			.enabled_(false)
			.maxHeight_(10)
			.onChange(param.target, \cursor, { arg view ...args;
				[args[2], x].debug("bbb");
				if(args[2] == x or: { args[2].isNil }) {
					// FIXME: AppClock doesnt have the same tempo of the pattern :/
					// FIXME: how to specify another server ?
					Task{
						Server.default.latency.wait;
						if(args[3] == 1) {
							view.value = 1;
						} {
							view.value = 0;
						};
						nil
					}.play(AppClock);
				};
				args.debug("cursor!!");
			})
			.states_([
				["", Color.black, Color.clear],
				["", Color.black, ParamView.color_ligth],
			]);
		})
	}

	init { arg param, makecell;
		var viewlist;
		^HLayout(*
			viewlist = param.collect(makecell)
		)
		.add(nil)
		.addUniqueMethod(\mapParam, { arg param;
			viewlist.do { arg view, n; 
				view.mapParam(param.at(n));
			}
		})
		.addUniqueMethod(\viewlist, {
			viewlist
		})
	}
}

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

	*cursorRow { arg param;
	}
}

WindowLayout {
	*new { arg fun;
		var window = Window.new;
		var layout;
		var val;
		val = fun.value(window);
		if(val.isKindOf(Layout)) {
			layout = val;
		} {
			layout = HLayout(val)
		};
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
	}
}

//////////////////////// Enhanceds SCLib views (X prefix because I don't know how to handle this)

XEnvelopeView : QEnvelopeView {
	// standard EnvelopeView doesn't provide a symetric getEnv/setEnv
	var curves;
	var <timeScale = 1;
	var duration;
	var envDur;
	var rawValue;
	var rawGrid;
	var autoTimeScale = true;
	var <totalDur = 8;
	var <loopNode, <releaseNode;

	curves {
		^curves
	}

	curves_ { arg xcurves;
		//curves.debug("curves::");
		curves = xcurves;
		super.curves = xcurves;
	}

	valueXY_ { arg val;
		val = val.deepCopy;
		envDur = val[0].last;
		if(envDur > totalDur) {
			totalDur = envDur;
		};
		val[0] = val[0] / totalDur;
		super.value = val;
		this.updateGrid;
	}

	valueXY { 
		var val = super.value.deepCopy; // deepCopy to avoid messing with internal value
		val[0] = val[0] * totalDur;
		^val;
	}

	value {
		^this.getEnv;
	}

	value_ { arg val;
		this.setEnv(val);
	}

	zoomFit {
		var val = this.valueXY;
		envDur = val[0].last;
		this.totalDur = envDur;
	}

	totalDur_ { arg newdur;
		// FIXME: if valueXY is nil, crash
		var curval = this.valueXY.deepCopy;
		totalDur = newdur;
		this.valueXY = curval;
	}

	setEnv { arg env;
		var times = [0] ++ env.times.integrate;
		this.valueXY = [times, env.levels];
		this.curves = env.curves;
		loopNode = env.loopNode;
		releaseNode = env.releaseNode;
	}

	grid_ { arg val;
		rawGrid = val;
		this.updateGrid;
	}

	grid {
		^rawGrid;
	}

	updateGrid {
		rawGrid = rawGrid ? Point(1/8,1/8);
		super.grid = Point( rawGrid.x / totalDur,rawGrid.y)
	}

	getEnv { arg val;
		var curves;
		var times;
		var levels;
		var env;
		if(val.isNil) {
			val = this.valueXY;
		};
		times = val[0];
		times = times.drop(1);
		times = times.differentiate;
		levels = val[1];
		curves = this.curves;
		env = Env.new(levels, times, curves, releaseNode, loopNode);
		^env
	}

	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapEnvelopeView(this, { arg self;
			var val = self.valueXY;
			// prevent the first node from moving
			if( val[0][0] != 0) {
				val[0][0] = 0;
				self.valueXY = val;
			};
		});
	}
}

XStaticText : QStaticText {
	// no value method in StaticText make it less generic when mixed with others views
	value {
		this.string;
	}

	value_ { arg val;
		this.string = val
	}
}

XSimpleButton : QButton {
	// need to access to properties individually and setting them independently of state index (.value)
	var <color, <label, <background, myValue;

	color_ { arg val;
		color = val;
		this.updateStates;
	}

	background_ { arg val;
		background = val;
		this.updateStates;
	}

	label_ { arg val;
		label = val;
		this.updateStates;
	}

	value_ { arg val;
		myValue = val;
	}

	value { arg val;
		^myValue;
	}

	updateStates {
		this.states = [[label, color, background]];
	}

}

///////////////////////// special control view

SampleChooserView : SCViewHolder {
	var <>controller;
	var <>waveview, <>pathfield;
	var <buffer;
	var <>layout;

	*new { arg buf;
		^super.new.init(buf);
	}

	mapParam { arg param;
		param.makeSimpleController(this.view, 
			action: { arg view, param;
				"this is action!!!!!!!".debug;
				param.set(this.buffer);
			}, 
			updateAction: { arg view, param;
				{
					this.buffer = param.get;
				}.defer;
			},
			initAction: nil,
			//customAction: action
		)

	}

	unmapParam {
		this.getHalo(\simpleController).remove;
	}

	init { arg buf;
		this.makeView;
		if(buf.notNil) {
			this.buffer = buf;
		};
	}

	makeView { arg self;
		this.view = View.new;
		pathfield = TextField.new;
		waveview = SoundFileView.new;
		layout = VLayout(
			waveview,
			HLayout(
				pathfield,
				Button.new.action_( {
					Dialog.openPanel({ arg path;
						path.postln;
						path.debug( "filedialog: set buf" );
						this.buffer = BufferPool.get_stereo_sample(\param, path);
						"bientot action".debug;
						this.view.action.(this);
					},{
						"cancelled".postln; 
					});
				})
			)
		);
		this.view.layout = layout;
		this.view.addUniqueMethod(\action_, { arg view, val; val.debug("fuckset"); this.action = val; }); // compat with makeSimpleController
		this.view.addUniqueMethod(\action, { arg view, val;"fuckget".debug; this.action });
	}

	buffer_ { arg buf;
		buf.debug("set buffer");
		if(buf.notNil and: { buf.isKindOf(Buffer) and: {buffer != buf} }) {
			if(buf.path.notNil) {
				var soundfile;
				buffer = buf;
				pathfield.string = buf.path;
				soundfile = SoundFile(buf.path);
				waveview.readFile(soundfile);
				soundfile.close;
			} {
				// TODO
				pathfield.string = buf.asCompileString
			}
		}
	}

	value_ { arg val;
		this.buffer = val;
	}

	value {
		^this.buffer;
	}

}

///////////////////// Sequencer views

StepListView : SCViewHolder {
	var <>stepseq;
	var <>controller;
	var <>selectAction;
	var <>deselectAction;
	var <>hasCursor = false;
	*new { arg stepseq;
		^super.new.init(stepseq);
	}

	viewlist { 
		^this.view.layout.viewlist;
	}

	init { arg seq;
		this.view = View.new;
		if(seq.notNil) {
			this.mapStepList(seq);
		}
	}

	setCursor { arg active=true, select, deselect;
		deselectAction = deselect;
		selectAction = select;
		hasCursor = active;
		this.addCursor;
	}

	addCursor { arg select, deselect;
		select = select ? selectAction;
		deselect = deselect ? deselectAction;
		this.viewlist.do { arg view, x;
			ListParamLayout.addCursor(x, view, stepseq.asParam.at(x), select ? { 
				if(view.isKindOf(Button)) {
					var states, val;
					states = view.states;
					val = view.value;
					states.do { arg x; x[0] = "O" };
					view.states = states;
					view.value = val;
				} {
					var color = view.color;
					var newcolor = ParamView.color_ligth;
					if(color.isSequenceableCollection) {
						color[0] = newcolor;
					} {
						color = newcolor;
					};
					view.color = color;
				}
			}, deselect ? {
				if(view.isKindOf(Button)) {
					var states, val;
					states = view.states;
					val = view.value;
					states.do { arg x; x[0] = " " };
					view.states = states;
					view.value = val;
				} {
					var color = view.color;
					var newcolor = Color.white;
					if(color.isSequenceableCollection) {
						color[0] = newcolor;
					} {
						color = newcolor;
					};
					view.color = color;
				}
			}) 
		}
	}

	makeLayout { arg seq, style;
		^ListParamLayout.perform(style, stepseq.asParam)
	}

	makeUpdater {
		if(controller.notNil) { controller.remove; };
		controller = SimpleController.new(this.stepseq).put(\refresh, { arg ...args;
			if(this.view.isNil or: {this.view.isClosed}) { controller.remove } {
				this.mapStepList(this.stepseq); // refresh
			};
		});
	}

	mapStepList { arg seq, style;
		stepseq = seq;
		this.view.removeAll;
		if(seq.notNil) {
			this.makeUpdater;
			style = style ?? { seq.getHalo(\seqstyle) ? \knob }; 
			this.view.layout_(this.makeLayout(seq, style));
			if(hasCursor) {
				this.addCursor;
			}
		}
	}
}

StepCursorView : StepListView {
	makeLayout { arg seq, style;
		^ListParamLayout.cursor(stepseq.asParam)
	}
}

StepEventView : SCViewHolder {
	var <viewlist;
	var <>stepseqview;
	var <>popupview;
	var <>eventseq;
	var <>controller;

	*new { arg seq;
		^super.new.init(seq);
	}

	init { arg seq;
		this.view = View.new;
		stepseqview = StepListView.new;
		this.view.layout = HLayout(
			stepseqview.view,
			popupview = PopUpMenu.new,
		);
		if(seq.notNil) {
			this.mapStepEvent(seq);
		}
	}

	setCursor { arg active=true, select, deselect;
		stepseqview.setCursor(active, select, deselect);
	}

	hasCursor {
		^stepseqview.hasCursor;
	}

	makeUpdater {
		if(controller.notNil) { controller.remove; };
		controller = SimpleController.new(this.eventseq).put(\refresh, { arg ...args;
			if(this.view.isNil or: {this.view.isClosed}) { controller.remove } {
				this.mapStepEvent(this.eventseq); // refresh
			};
		});
	}

	mapStepEvent { arg seq;
		eventseq = seq;
		this.makeUpdater;
		popupview.items = eventseq.keys.asArray.sort;
		popupview.action = { arg view;
			var stepseq = eventseq[view.items[view.value].asSymbol];
			stepseqview.mapStepList(stepseq)
		};
		if(eventseq.size > 0) {
			if(eventseq[\isRest].notNil) {
				popupview.valueAction = popupview.items.detectIndex({ arg x; x == \isRest });
			} {
				popupview.valueAction = 0
			};
		} {
			stepseqview.removeAll;
		}
	}
}

DictStepListView : StepListView {
	*new { arg stepseq;
		^super.new.init(stepseq);
	}

	init { arg seq;
		this.view = View.new;
		if(seq.notNil) {
			this.mapStepList(seq);
		}
	}

	makeLayout { arg seq, style;
		^ListParamLayout.perform(style, stepseq.asParam, stepseq.dict.keys)
	}

	mapStepList { arg seq, style;
		stepseq = seq;
		this.view.removeAll;
		if(seq.notNil) {
			this.makeUpdater;
			style = style ?? { seq.getHalo(\seqstyle) ? \valuePopup }; 
			//[style, seq.dict].debug("mapDictStepList: style, dict");
			this.view.layout_(this.makeLayout(seq, style));
		}
	}
}

ParDictStepListView : DictStepListView {
	makeLayout { arg seq, style;
		^ListParamLayout.perform(style, stepseq.asParam, stepseq.dicts[0].keys)
	}
}

//BankListView : SCViewHolder {
//
//	*new { arg banklist;
//		^super.new.init(banklist)
//	}
//
//	init { arg banklist;
//	
//	}
//}

//////////////////////////////////////////// DRAFT

XVLayout : VLayout {
	// does not work :( not recognized as a Layout )
	var <>list;
	*new { arg ...items;
		var serializedItems = items.collect( { |x| this.parse(x) } );
		var li;
		var ins;
		li = serializedItems.collect({ arg x; x[0] });
		ins = super.new( [serializedItems] );
		ins.list = li;
		^ins;
	}

	//add { arg item, stretch = 0, align;
	//	this.invokeMethod( \addItem, [[item, stretch, QAlignment(align)]], true );
	//}

	//insert { arg item, index=0, stretch = 0, align;
	//	this.invokeMethod( \insertItem, [[item, index, stretch, QAlignment(align)]], true );
	//}

}


PlayerWrapperView : ObjectGui {
	var player;
	var button;
	var skipjack;
	var pollRate = 1;
	new { arg model;
		super.new(model);
	}

	// FIXME: this is dirty, a layout is not a view
	// but require to remember if class is a layout or a view
	view { ^this.layout }

	layout { arg parent;
		var lay = HLayout(
			button = Button.new.action_({ arg view;
				switch(view.value,
					1, {
						[view.value, model].debug("mais play bordel!");
						model.play;
					},
					0, {
						[view.value, model].debug("mais stop bordel!");
						model.stop;
					}
				);
			})
		);
		button.states = this.getStates;
		this.makeUpdater;
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
		if(val.notNil) {
			model = val;
		} {
			skipjack.stop;
		}
	}

	getStates { arg str="";
		^[
			[ str + "▶", Color.black, Color.white ],
			[ str + "||", Color.black, ParamView.color_ligth ],
		];
	}

	update { arg changed, changer ... args;

		//[changed, changer, args].debug("changed, changer");

        if(changer !== this) {  
			var label;
			//model.isPlaying.debug("isPlaying?");
			if(model.isKindOf(PlayerWrapper).not or: { model.target.notNil }) {
				if(model.class == PlayerWrapper) {
					label = model.label;
				} {
					label = model.tryPerform(\name) ? model.tryPerform(\label) ? model.tryPerform(\key);
					label = model.class.asString[0].asString + label;
				};

				button.states = this.getStates(label);
				if(model.isPlaying) {
					button.value = 1
				} {
					button.value = 0
				};
			} {
				button.states = this.getStates;
				button.value = 0;
			}
		}
	}
}

// more complex
YourGuiClass : ObjectGui {
    
    var numberEditor;
    
    //for example
    guiBody { arg layout;
        var r;
        // the object you are making a gui for is referred to as the model
        
        // display some param on screen.
        // here we assume that someParam is something that
        //  has a suitable gui class
        // implemented, or that the default ObjectGui is sufficient.
        model.someParam.gui(layout);
        
        // using non 'gui' objects
        //r = layout.layRight(300,300); // allocate yourself some space
        Button(layout.win)
            .action_({ arg butt;
                model.goApeShit;
            });
        
        // note: NumberEditor is a cruciallib class
        // which is itself a model (its an editor of a value)
        // and has its own gui class that creates and manages the NumberBox view
        numberEditor = NumberEditor(model.howFast,[0,100])
            .action_({ arg val; 
                model.howFast = val; 
                model.changed(this); 
                // tell the model that this gui changed it
            });
        numberEditor.gui(layout);
    }
    
    // your gui object will have update called any time the .changed message
    // is sent to your model
    update { arg changed,changer;
    
        if(changer !== this,{ 
            /* if it is this gui object that changed the value
                using the numberEditor, then we already have a correct
                display and don't need to waste cpu to update it.
                if anyone else changed anything about the model,
                we will update ourselves here.
            */
            numberEditor.value = model.howFast;
            /*
                note that 
                    numberEditor.value = model.howFast;
                is passive, and does not fire the numberEditor's action.    

                numberEditor.activeValue = model.howFast
                would fire the action as well, resulting in a loop that would
                probably crash your machine.
            */
        })
    }

}

MyModel {
	
	var <>howFast = 10;
	var <>someParam = 50;
	
	*new { 
		^super.new.init;
	}

	goApeShit {
		"plxxop".debug;
	}

	guiClass { ^YourGuiClass }

	init {
	}
}


////////////////////////////////

+Window {
	*keyDownActionTest {
		var window = Window.new;
		var layout;
		var field = TextField.new;
		window.view.keyDownAction = { arg me, key, modifiers, unicode, keycode;
			[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
			field.string = [me, key, modifiers, unicode, keycode].asCompileString;
			true;
		};
		field.keyDownAction = window.view.keyDownAction;
		layout = VLayout(
			field
		);
		window.layout = layout;
		window.alwaysOnTop = true;
		window.front;

	}
}
