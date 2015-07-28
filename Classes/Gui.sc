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
		layout = fun.value(window);
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
	}
}


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


XEnvelopeView : QEnvelopeView {
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
	value {
		this.string;
	}

	value_ { arg val;
		this.string = val
	}
}

XSimpleButton : QButton {
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
			[args[2], x].debug("bbb");
			if(args[2] == x or: { args[2].isNil }) {
				// AppClock doesnt have the same tempo of the pattern
				//		but s.latency is in seconds and appclock has tempo 1, so this works!!!
				// FIXME: how to specify another server ?
				Task{
					Server.default.latency.wait;
					if(args[3] == 1) {
						on.value(view, x, param, args[4]);
					} {
						off.value(view, x, param, args[4]);
					};
					nil
				}.play(AppClock);
			};
			args.debug("cursor!!");
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
				["", Color.black, Color.white],
				["", Color.black, Color.yellow],
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

StepListView : SCViewHolder {
	var <>stepseq;
	var <>controller;
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

	addCursor { arg select, deselect;
		this.viewlist.do { arg view, x;
			ListParamLayout.addCursor(x, view, stepseq.asParam.at(x), { 
				var color = view.color;
				color[0] = Color.yellow;
				view.color = color;
			}, {
				var color = view.color;
				color[0] = Color.white;
				view.color = color;
			}) 
		}
	}

	makeLayout { arg seq, style;
		^ListParamLayout.perform(style, stepseq.asParam)
	}

	makeUpdater {
		if(controller.notNil) { controller.remove; };
		controller = SimpleController.new(this.stepseq).put(\refresh, { arg ...args;
			if(this.view.isClosed) { controller.remove };
			this.mapStepList(this.stepseq); // refresh
		});
	}

	mapStepList { arg seq, style;
		stepseq = seq;
		this.view.removeAll;
		if(seq.notNil) {
			this.makeUpdater;
			style = style ?? { seq.getHalo(\seqstyle) ? \knob }; 
			this.view.layout_(this.makeLayout(seq, style));
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
	var stepseqview;
	var popupview;
	var eventseq;

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

	mapStepEvent { arg seq;
		eventseq = seq;
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
