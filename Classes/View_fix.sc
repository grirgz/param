//////////////////////// Enhanceds SCLib views (X prefix because I don't know how to handle this)

FixedEnvelopeView : QEnvelopeView {
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
