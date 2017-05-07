////////////////////// Basic building blocks

ParamViewToolBox {

	classvar <>color_dark;
	classvar <>color_ligth;

	*initClass {
		//color_ligth = Color.newHex("63AFD0");
		//color_dark = Color.newHex("0772A1");
		color_ligth = Color.new255(101, 166, 62);
		color_dark = Color.new255(130, 173, 105);

		//color_ligth = Color.newHex("5CCCCC");
		//color_dark = Color.newHex("009999");
	}

	*horizontal_slider { arg param, label_mode;
		var view = View.new;
		var slider, val, label;
		view.layout_(
			HLayout (
				label = StaticText.new,
				slider = param.asSlider.orientation_(\horizontal),
				val = param.asTextField,
			)
		);
		view.addUniqueMethod(\mapParam, { arg view, param;
			if(label_mode == \full) {
				label.string  = param.asLabel;
			} {
				label.string = param.property
			};
			val.mapParam(param);
			slider.mapParam(param);
		});
		view.addUniqueMethod(\unmapParam, { arg view, param;
			label.string = "";
			val.unmapParam;
			slider.unmapParam;
		});
		if(param.notNil) { view.mapParam(param) };
		^view
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
			param.debug("ParamViewToolBox.knob.mapParam");
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

	*knob { arg param;
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
			param.debug("ParamViewToolBox.knob.mapParam");
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
		if(param.notNil) { view.mapParam(param) };
		^view;
	}
}


ListParamLayout {

	*new { arg param, makecell;
		^super.new.init(param, makecell)
	}

	*button { arg param, width=40;
		^super.new.init(param, { arg param;
			param.asButton.fixedWidth_(width);
		});
	}

	*gridButton { arg xparam, width=40;
		// bug in button size, gridButton is a workaround,
		// the drawback is you need to access to the button inside the view:
		^super.new.init(xparam, { arg param;
			var button = param.asButton;
			View.new.layout_(HLayout(button).margins_(0))
				.addHalo(\isGridButton, true)
				.addUniqueMethod(\button, { button } )
			;
		});
	}

	*slider { arg param, width=40;
		^super.new.init(param, { arg param;
			param.asSlider.fixedWidth_(width);
		});
	}

	*knob { arg param, width=40;
		^super.new.init(param, { arg subparam;
			subparam.asKnob.fixedWidth_(width);
		});
	}

	*valuePopup { arg param, keys, width=40;
		keys = keys ? []; // TODO: use spec and Param.asPopupView
		^super.new.init(param, { arg subparam;
			var pm = PopUpMenu.new;
			pm.fixedWidth_(width);
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

	*indexPopup { arg param, keys, width=40;
		keys = keys ? []; // TODO: use spec and Param.asPopupView
		^super.new.init(param, { arg subparam;
			var pm = PopUpMenu.new;
			pm.fixedWidth_(width);
			pm.items = keys;
			pm.action = {
				subparam.set(pm.value)
			};
			pm.onChange(subparam.target, \set, {
				pm.value = subparam.get;
			});
			pm.value = subparam.get;
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

	*cursor { arg param, width=40;
		^super.new.init(param, { arg param, x;
			Button.new
			.enabled_(false)
			.fixedWidth_(width)
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
				["", Color.black, Color.white],
				["", Color.black, ParamViewToolBox.color_ligth],
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
		// replaced by WindowLayout and WindowDef
		var win = Window.new;
		win.layout = layout;
		win.alwaysOnTop = alwaysOnTop;
		win.front;
		^win;
	}

	*singleWindowize {
		// TODO: the window close before recreating if open
	}

	*block { arg label, pg;
		^VLayout(
			[StaticText.new.string_(label).background_(ParamViewToolBox.color_dark), stretch:0],
			[this.two_panes(pg, \property), stretch:1],
		)
	}

	*player_block { arg player, pg, views;
		^VLayout(
			[View.new.layout_(
				HLayout(
					PlayerWrapper(player).asView,
					views,
					nil
				).margins_(0)
			).background_(ParamViewToolBox.color_dark), stretch:0],
			[this.two_panes(pg, \property), stretch:1],
		);
	}

	*player_preset_block { arg player, pg, views;
		^VLayout (
			HLayout (
				PlayerWrapper(player).asView,
				views,
				EventClass.presetSelectorView.(pg),
			),
			ParamGroupLayout.two_panes(pg, \property),
		)

	}

	*two_panes { arg pg, label_mode;

		var layout;
		var gridlayout;
		var biglayout;
		var scalarlist, biglist;
		var layout_type;

		label_mode = label_mode ? \full; // \full, \property

		scalarlist = pg.select({ arg param; 
			param.type == \scalar;
		});
		biglist = pg.select({ arg param;
			param.type != \scalar and: { 
				param.spec.isKindOf(AudioSpec).not
				and: { 
					// FIXME: find a better way to handle this
					param.type != \other 
				}
			}
		});

		gridlayout = GridLayout.rows(*
			scalarlist.collect({ arg param;
				[
					if(label_mode == \full) {
						param.asStaticTextLabel;
					} {
						StaticText.new.string_(param.property)
					},
					param.asSlider.orientation_(\horizontal),
					param.asTextField,
				]
			}) 
		);
		gridlayout.setColumnStretch(0,2);
		gridlayout.setColumnStretch(1,6);
		gridlayout.setColumnStretch(2,2);
		gridlayout = VLayout(gridlayout, [nil, stretch:2]);

		// chipotage
		if(biglist.size < 5 and: { scalarlist.size < 6 } ) {
			layout_type = VLayout;
		} {
			layout_type = HLayout;
		};

		biglayout = VLayout(*
			biglist.collect({ arg param;
				VLayout(
					if(label_mode == \full) {
						param.asStaticTextLabel;
					} {
						StaticText.new.string_(param.property)
					},
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

	*inline_groups { arg pg, label_mode;

		var layout;
		var gridlayout;
		var biglayout;
		var scalarlist, biglist;
		var layout_type;
		var scrollview;

		var vertical_slider_group = { arg params, size=8;
			var lay;
			lay = GridLayout.rows(
				* size.collect({ arg idx;
					var param = params[idx];
					param.debug("what param");
					if(param.isNil) {
						nil ! 3;
						//{ Button.new } ! 3
					} {
						[
							if(label_mode == \full) {
								param.asStaticTextLabel;
							} {
								StaticText.new.string_(param.property)
							},
							param.asSlider.orientation_(\horizontal).minWidth_(150),
							param.asTextField.minWidth_(70),
						]
					}
				}) ++ [{ View.new }!3]
			).vSpacing_(2);

			lay;
		};

		label_mode = label_mode ? \property; // \full, \property

		scalarlist = pg.select({ arg param; 
			param.type == \scalar;
		});
		biglist = pg.select({ arg param;
			param.type != \scalar and: { 
				param.spec.isKindOf(AudioSpec).not
				and: { 
					// FIXME: find a better way to handle this
					param.type != \other 
				}
			}
		});

		// FIXME: not used ?!
		gridlayout = GridLayout.rows(*
			scalarlist.collect({ arg param;
				[
					if(label_mode == \full) {
						param.asStaticTextLabel;
					} {
						StaticText.new.string_(param.property)
					},
					param.asSlider.orientation_(\horizontal),
					param.asTextField,
				]
			})
		);
		gridlayout.setColumnStretch(0,2);
		gridlayout.setColumnStretch(1,6);
		gridlayout.setColumnStretch(2,2);

		// chipotage
		//if(biglist.size < 5 and: { scalarlist.size < 6 } ) {
		//	layout_type = VLayout;
		//} {
		//	layout_type = HLayout;
		//};
		layout_type = HLayout;

		biglayout = HLayout(*
			biglist.collect({ arg param;

				[
				View.new.layout_(VLayout(
					if(label_mode == \full) {
						param.asStaticTextLabel;
					} {
						StaticText.new.string_(param.property).maxHeight_(10)
					},
					param.asView,
					param.asTextField,
					View.new,
					nil,
				).margins_(0)).minWidth_(300).maxHeight_(200),
				align: \top,
				]
			})
		);

		layout = layout_type.new(
			HLayout(*scalarlist.clump(8).collect({ arg gr; gr.debug("gr"); vertical_slider_group.(gr) })),
			biglayout
		);
		layout;
		scrollview = ScrollView.new.canvas_(View.new.layout_(layout));
		^VLayout(scrollview)
	}

	*cursorRow { arg param;
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

BasicButton : QButton {
	// need to access to properties individually and setting them independently of state index (.value)
	var <color, <label, <background, myValue=0;

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

	string_ { arg val;
		this.label = val;
	}

	string { ^this.label }

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

XSimpleButton : BasicButton {} // backward compat

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
						this.buffer = SampleProxy(path);
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
		if(buf.notNil and: { buf.respondsTo(\bufnum) and: {buffer != buf} }) {
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
	var <>cellWidth = 40;
	var <>style;


	*new { arg stepseq, style;
		^super.new.init(stepseq, style);
	}

	stepList { // realname, should avoid stepseq
		^stepseq
	}

	viewlist { 
		if(this.view.notNil and: { this.view.layout.notNil }) {
			^this.view.layout.viewlist;
		} {
			^nil
		}
	}

	init { arg seq, astyle;
		this.view = View.new;
		style = astyle;
		if(seq.notNil) {
			this.mapStepList(seq, astyle);
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
		if(this.viewlist.notNil) {
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
						if(view.getHalo(\isGridButton) == true) {
							var states, val;
							var myview = view.button;
							states = myview.states;
							val = myview.value;
							states.do { arg x; x[0] = "O" };
							myview.states = states;
							myview.value = val;
						} {
							var color = view.color;
							var newcolor = ParamViewToolBox.color_ligth;
							if(color.isSequenceableCollection) {
								color[0] = newcolor;
							} {
								color = newcolor;
							};
							view.color = color;
						}
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
						if(view.getHalo(\isGridButton) == true) {
							var states, val;
							var myview = view.button;
							states = myview.states;
							val = myview.value;
							states.do { arg x; x[0] = " " };
							myview.states = states;
							myview.value = val;
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
					}
				}) 
			};
		}
	}

	makeLayout { arg seq, astyle;
		var lpl;
		if(style.isKindOf(Function)) {
			lpl = ListParamLayout.new(seq ? stepseq.asParam, astyle ? style);
		} {
			lpl = ListParamLayout.perform(astyle ? style, seq ? stepseq.asParam);
			lpl.viewlist.collect({ arg x; x.fixedWidth = cellWidth});
		};
		^lpl
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
		if(this.view.notNil and: {this.view.isClosed.not}) {
			this.view.removeAll;
			if(seq.notNil) {
				this.makeUpdater;
				style = style ?? { seq.getHalo(\seqstyle) ? \knob }; 
				this.view.layout_(this.makeLayout(seq, style));
				if(hasCursor) {
					this.addCursor;
				}
			} {
				this.view.removeAll;
			}
		};
	}
}

StepListColorView : StepListView {
	classvar <>colorRing;

	makeLayout { arg seq, style;
		^this.makeColorLayout(seq, style);
	}

	makeColorLayout { arg seq, astyle;
		var lpl;
		var color;
		var color_ring = colorRing ?? { [
			Color.newHex("D5F8F8"),
			Color.newHex("D5F8F8"),
			Color.newHex("A0E6E6"),
			Color.newHex("A0E6E6"),
		]};
		astyle = astyle ? style;
		if(astyle.isKindOf(Function)) {
			lpl = ListParamLayout(stepseq.asParam, astyle);
		} {
			lpl = ListParamLayout.perform(astyle, stepseq.asParam);
			lpl.viewlist.do { arg x;
				x.fixedWidth = 30;
				x.minHeight_(30+3);
			};
		};
		color_ring = color_ring.copy;
		^HLayout(*
			lpl.viewlist.clump(4).collect({ arg group4;
				color = color_ring[0]; 
				color_ring = color_ring.rotate(-1);
				View.new.layout_(
					HLayout (
						* group4.collect({ arg view;
							view;
						})
					).spacing_(5).margins_([5,5])
				).background_(color);
			}) 
			++ [nil];
		).spacing_(0).margins_(0)
			.addUniqueMethod(\viewlist, { lpl.viewlist })
			.addUniqueMethod(\mapParam, { arg x; lpl.mapParam(x) })
		;
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
	var <>sizer_view;

	*new { arg seq;
		^super.new.init(seq);
	}

	init { arg seq;
		this.view = View.new;
		stepseqview = StepListView.new;
		this.view.layout = HLayout(
			[stepseqview.view, stretch:1],
			VLayout(
				popupview = PopUpMenu.new,
				sizer_view = NumberBox.new; [sizer_view, stretch:0],
			)
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
			if(stepseq.isKindOf(StepList)) {
				stepseqview.mapStepList(stepseq);
				sizer_view.mapParam(Param(Message(stepseq), \stepCount));
			} {
				// TODO: implement simple knob if scalar
				stepseqview.mapStepList(nil);
				sizer_view.unmapParam;
			}
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
		} {
			this.view.removeAll;
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
		lay.addUniqueMethod(\button, { button }); // FIXME: why is button wrapped in a layout ?
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

	layout { arg parent;
		var lay;
		labelView = StaticText.new.string_("bla");
		lay = HLayout(
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
		this.makeUpdater;
		this.update;
		^view;
	}

	view { 
		if(view.isNil) {
			this.layout;
		};
		^view;
	}

	asView {
		^this.layout;
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

///////////////////////////////

ContextMenuWindow {
	var <>list;
	var <>action;
	classvar <>window;
	
	*new { arg list;
		^super.new.init(list);
	}

	init { arg xlist;
		list = xlist
	}

	close {
		if(window.notNil and: { window.isClosed.not }) {
			window.close;
		};
		window = nil;
	}
	
	front { arg view, x, y, mouseButton;
		var bo = view.absoluteBounds;
		this.close;
		if(mouseButton.notNil and: { mouseButton != 1 }) {
			^this
		};
		window = Window("kkk",Rect(x+bo.origin.x,Window.screenBounds.height - view.absoluteBounds.top - y,1,1), border:false);
		//[x,y, view.absoluteBounds, view.bounds, Window.screenBounds].debug("BOUDS");
		window.endFrontAction = {
			this.close;
		};
		window.layout_(
			VLayout(
				ListView.new.items_(list).mouseDownAction_({

					//{ win.close }.defer(1);
				}).selectionAction_({ arg me;
					me.selection.debug("selection!!");
					if(me.selection.size > 0) {
						action.(this, me.selection[0]);
						this.close;
					}
				}).selection_(nil).selectionMode_(\single)
			).margins_(0).spacing_(0)
		);
		window.front;
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




////////////////////////////////////////

