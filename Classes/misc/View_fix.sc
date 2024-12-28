//////////////////////// Enhanceds SCLib views

FixedEnvelopeView : EnvelopeView {
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
	var <rightClickZoomEnabled = false;

	curves {
		^curves
	}

	curves_ { arg xcurves;
		//curves.debug("curves::");
		curves = xcurves;
		super.curves = xcurves;
	}

	size {
		^this.getEnv.size;
	}

	valueXY_ { arg val;
		val = val.deepCopy;
		envDur = val[0].last;
		if(envDur.notNil and:{ envDur > totalDur }) {
			totalDur = envDur;
		};
		val[0] = val[0] / totalDur;
		super.value = val;
		this.updateGrid;
	}

	envDur {
		^this.valueXY[0].last;
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

	nodeIndexFromPixelPoint { arg point; 
		var beatx = point.x/this.bounds.width * this.totalDur;
		^this.valueXY.first.detectIndex { arg i; i > beatx } ?? { this.size  } - 1;
	}

	setMouseHooks {
		this.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

			if(buttonNumber == 1) {
				view.addHalo(\mouseDownPosition, Point(x, y));
				view.addHalo(\mouseDownTotalDur, view.totalDur);
				view.addHalo(\elasticSelection, this.elasticSelection); 
				this.elasticSelection = true;
			};
			if(buttonNumber == 0) {
				var selidx;
				var xy;
				if(view.index < 0) {
					selidx = this.nodeIndexFromPixelPoint(Point(x,y));
					//view.index = idx;
				} {
					selidx = view.index;
				};
				//sel = view.index;
				//selidx.debug("sel");
				view.addHalo(\mouseDownIndex, selidx);
				view.addHalo(\mouseDownPosition, Point(x, y));
				xy = this.valueXY;
				if(selidx != ( this.size - 1 )) {
					view.addHalo(\mouseDownInverse, xy[1][selidx] > xy[1][selidx + 1] );
				};
				if(modifiers.isShift) {
					if(view.curves.isSequenceableCollection.not) {
						view.addHalo(\mouseDownCurve, view.curves);
					} {
						view.addHalo(\mouseDownCurve, view.curves[selidx]);
					};
				}

			};
		}).mouseMoveAction_({ arg view, x, y, modifiers;
			var delta;
			//[view, x, y, modifiers].debug("mouseMoveAction");
			if(view.getHalo(\mouseDownPosition).notNil and: { view.getHalo(\mouseDownTotalDur).notNil }) {
				delta = x - view.getHalo(\mouseDownPosition).x / 500;
				view.totalDur = view.getHalo(\mouseDownTotalDur) + delta;
			};
			if(modifiers.isShift) {
				if(view.getHalo(\mouseDownPosition).notNil and: {view.getHalo(\mouseDownCurve).notNil  }) {
					var selidx = view.getHalo(\mouseDownIndex) ?? { view.index };
					var inv = if(view.getHalo(\mouseDownInverse) == true) { -1 } { 1 };
					delta = y - view.getHalo(\mouseDownPosition).y / 10;
					if(view.curves.isSequenceableCollection.not) {
						view.curves = view.curves ! view.value.size;
					};
					view.curves[selidx] = view.getHalo(\mouseDownCurve) + ( delta * inv );
					//view.curves[selidx].debug("added curve delta %".format(delta));
					view.curves = view.curves; // update GUI
					this.doAction;
					//view.totalDur = view.getHalo(\mouseDownTotalDur) + delta;
				}

			}

		}).mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(buttonNumber == 1) {
				this.elasticSelection = view.getHalo(\elasticSelection); 
			};
			view.addHalo(\mouseDownCurve, nil);
			view.addHalo(\mouseDownIndex, nil);
			view.addHalo(\mouseDownInverse, nil);
			view.addHalo(\mouseDownPosition, nil);
			view.addHalo(\mouseDownTotalDur, nil);
			view.addHalo(\elasticSelection, nil);
			if(Halo.lib[view].isEmpty) {
				Halo.lib.removeAt(view)
			};
		});

		// only zoom, old code
		//this.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			////[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			//if(buttonNumber == 1) {
				//view.addHalo(\mouseDownPosition, Point(x, y));
				//view.addHalo(\mouseDownTotalDur, view.totalDur);
				//view.addHalo(\elasticSelection, this.elasticSelection); 
				//this.elasticSelection = true;
			//};
		//}).mouseMoveAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//var delta;
			////[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			//if(view.getHalo(\mouseDownPosition).notNil) {
				//delta = x - view.getHalo(\mouseDownPosition).x / 500;
				//view.totalDur = view.getHalo(\mouseDownTotalDur) + delta;
			//}
		//}).mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			////[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			//if(buttonNumber == 1) {
				//view.addHalo(\mouseDownPosition, nil);
				//this.elasticSelection = view.getHalo(\elasticSelection); 
			//};
		//});
	}

	rightClickZoomEnabled_ { arg val;
		rightClickZoomEnabled = val;
		if(val == true) {
			this.setMouseHooks;
		} {
			this.mouseDownAction_({});
			this.mouseMoveAction_({});
			this.mouseUpAction_({});
		}
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
		if(val.isInteger) { 
			val = this.valueXY;
		};
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
XEnvelopeView : FixedEnvelopeView {}

ParamStaticText : QStaticText {
	// no value method in StaticText make it less generic when mixed with others views
	value {
		this.string;
	}

	value_ { arg val;
		this.string = val
	}
}

XStaticText : ParamStaticText { }
