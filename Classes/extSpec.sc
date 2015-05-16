
XArraySpec : Spec {
	var <array, <default;
	var <size, <>isMonoSpec;

	*new { arg array, default=nil;
		var size = array.size;
		var isMonoSpec;
		array = array.collect(_.asSpec);

		if(
			array.any { arg val;
				val != array[0]
			};
		) {
			isMonoSpec = false
		} {
			isMonoSpec = true
		};

		if(default.isNil) {
			if(isMonoSpec) {
				default = array[0].default ! size;
			} {
				default = array.collect(_.default);
			}
		};

		if(default.size != array.size) {
			"Warning: default value size does not match the spec array size".postln;
		};

		^super.newCopyArgs(array, default, size, isMonoSpec);
	}

	numChannels {
		^size
	}

	storeArgs {
		^[array, default]
	}

	at { arg idx;
		^array[idx];
	}

	map { arg val;
		var nested = false;
		var res;
		if(val[0].isSequenceableCollection) {
			// pattern array are nested in []
			val = val[0];
			nested = true;
		};
		res = val.collect({ arg subval, x;
			this.array[x].map(subval)
		});

		if(nested) {
			^[res]
		} {
			^res
		}

	}

	unmap { arg val;
		var nested = false;
		var res;
		if(val[0].isSequenceableCollection) {
			// pattern arrays are nested in []
			val = val[0];
			nested = true;
		};
		res = val.collect({ arg subval, x;
			this.array[x].unmap(subval)
		});

		if(nested) {
			^[res]
		} {
			^res
		}
	}
}

XEnvSpec : Spec {
	var <levels, <times, <curves;
	var <default;
	var <size;
	var <isMonoSpec;
	var <type=\default;

	*new { arg levels, times, curves, default;
		var size;
		var isMonoSpec;

		if(curves.isNil) {
			// FIXME: provide better default spec
			curves = ControlSpec(-9,9,\lin,0,0);
		};
		size = levels.size;
		times = times.asArray.wrapExtend(size - 1);
		curves = curves.asArray.wrapExtend(size - 1);

		times = times.collect(_.asSpec);
		levels = levels.collect(_.asSpec);
		curves = curves.collect(_.asSpec);

		curves.debug("XEnvSpec:curves");

		if(
			levels.any { arg val;
				val != levels[0]
			} or: {
				times.any { arg val;
					val != times[0]
				} or: {
					curves.any { arg val;
						val != curves[0]
					}
				}
			};
		) {
			isMonoSpec = false
		} {
			isMonoSpec = true
		};

		if(default.isNil) {
			default = Env(levels.collect(_.default), times.collect(_.default), curves.collect(_.default))
		};

		if(default.levels.size != levels.size) {
			"Warning: default value size does not match the spec env size".postln;
		};

		^super.newCopyArgs(levels, times, curves, default, size, isMonoSpec)
	}

	storeArgs {
		^[levels, times, curves, default]
	}

	*adsr { arg attack, decay, sustain, release, peak;
		var inst;
		var zerospec = ControlSpec(0,0.000000001,\lin);
		attack = attack ? ControlSpec(0,2,\lin,0,0.1);
		decay = decay ? attack;
		sustain = sustain ? attack;
		release = release ? attack;
		peak = peak ? sustain;
		inst = this.new([ zerospec, peak, sustain, zerospec], [attack, decay, release]);
		^inst
	}

	*dadsr { arg delay, attack, decay, sustain, release;

	}

	*asr {

	}

	*perc {

	}

	*triangle {

	}

	*sine {

	}

	*linen {

	}

	*cutoff {

	}

	map { arg val, ignoreCurves=true;
		var levels, times, curves;
		var nested = false, res;
		if(val.isSequenceableCollection) {
			// pattern arrays are nested in []
			val = val[0];
			nested = true;
		};
		levels = val.levels.collect({ arg subval, x;
			this.levels[x].map(subval);
		});
		times = val.times.collect({ arg subval, x;
			this.times[x].map(subval);
		});

		if(ignoreCurves) {
			curves = val.curves;
		} {
			switch(val.curves.class,
				Symbol, {
					curves = val.curves;
				},
				Float, {
					curves = [val.curves];
					curves = curves.collect({ arg subval, x;
						this.curves[x].map(subval);
					});
				}, 
				{
					curves = val.curves.collect({ arg subval, x;
						this.curves[x].map(subval);
					});
				}
			
			);
		};

		res = Env(levels, times, curves, val.releaseNode, val.loopNode);

		if(nested) {
			^[res]
		} {
			^res
		}
		
	}

	unmap { arg val, ignoreCurves=true;
		var levels, times, curves;
		var nested = false;
		var res;
		if(val.isSequenceableCollection) {
			// pattern arrays are nested in []
			val = val[0];
			nested = true;
		};
		levels = val.levels.collect({ arg subval, x;
			this.levels[x].unmap(subval);
		});
		times = val.times.collect({ arg subval, x;
			this.times[x].unmap(subval);
		});

		if(ignoreCurves) {
			curves = val.curves;
		} {
			switch(val.curves.class,
				Symbol, {
					curves = val.curves;
				},
				Float, {
					curves = [val.curves];
					curves = curves.collect({ arg subval, x;
						this.curves[x].unmap(subval);
					});
				}, 
				{
					curves = val.curves.collect({ arg subval, x;
						this.curves[x].unmap(subval);
					});
				}
			
			);
		};

		res = Env(levels, times, curves, val.releaseNode, val.loopNode);

		if(nested) {
			^[res]
		} {
			^res
		}
		
	}
	
}

XGateSpec : Spec {
	*new {
		^super.new(0,1,\lin,1,0);
	}
}

XTrigSpec : Spec {
	*new {
		^super.new(0,1,\lin,1,0);
	}
}

XBufferSpec : Spec {

}

XSampleSpec : XBusSpec {

}

XWavetableSpec : XBusSpec {

}

XBusSpec : Spec {

}

XInBusSpec : Spec {

}

XOutBusSpec : Spec {

}


// ~a = XArraySpec( \freq.asSpec ! 15  ) // array of size 15
// ~a = XArraySpec( [\freq, \unipolar]  ) // array of size two
// ~a = XArraySpec( [\freq, \unipolar], [1,2]  ) // default value
// ~a.size;
// ~a.default;
// ~a.array;
// ~a.isMonoSpec;
// 
// 
// ~b = XEnvSpec( \freq.asSpec ! 5, \dur.asSpec, \bipolar.asSpec) // env with 5 segments (times have 4 values)
// ~b = XEnvSpec( [\freq.asSpec, \lofreq.asSpec] , [\dur.asSpec, \unipolar.asSpec], \bipolar.asSpec) // 2 segment, different spec for each segment
// ~b = XEnvSpec( \freq.asSpec ! 5, \dur.asSpec, \bipolar.asSpec, Env([10,30,10,420],[1,2,1,2])) // default value
// ~b.default.asCompileString
// ~b.levels;
// ~b.curves;
// ~b.times;
// ~b.isMonoSpec;
