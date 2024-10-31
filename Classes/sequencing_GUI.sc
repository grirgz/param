
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
							var newcolor = ParamViewToolBox.color_light;
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
			Color(0.83529411764706, 0.97254901960784, 0.97254901960784), Color(0.83529411764706, 0.97254901960784, 0.97254901960784), Color(0.62745098039216, 0.90196078431373, 0.90196078431373), Color(0.62745098039216, 0.90196078431373, 0.90196078431373)  
			//Color.newHex("D5F8F8"),
			//Color.newHex("D5F8F8"),
			//Color.newHex("A0E6E6"),
			//Color.newHex("A0E6E6"),
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
