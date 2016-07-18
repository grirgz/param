
XArraySpec : Spec {
	var <array, <default;
	var <>size, <>isMonoSpec;
	var <>isDynamic;

	*new { arg array, default=nil;
		var size = array.size;
		var isMonoSpec;
		var isDynamic = false;
		if(array.isSequenceableCollection.not) {
			isDynamic = true;
			array = [array.asSpec];
		};
		array = array.collect(_.asSpec);

		array.do({ arg sp;
			if(sp.isNil) {
				Error("XArraySpec: spec is nil or not in Spec library: "++array.asCompileString).throw
			}
		});

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

		if(default.size != array.size and: { isDynamic.not }) {
			"Warning: default value size does not match the spec array size".postln;
		};

		^super.newCopyArgs(array, default, size, isMonoSpec, isDynamic);
	}

	numChannels {
		^size
	}

	storeArgs {
		^[array, default]
	}

	at { arg idx;
		^array.wrapAt(idx);
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
			this.array.wrapAt(x).map(subval)
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
			this.array.wrapAt(x).unmap(subval)
		});

		if(nested) {
			^[res]
		} {
			^res
		}
	}
}

StepListSpec : XArraySpec {
	default {
		^StepList.newFrom(array.collect(_.default))
	}
}

XEnvSpec : Spec {
	var <levels, <times, <curves;
	var <default;
	var <size;
	var <isMonoSpec;
	var <isDynamic=false;
	var <type=\default;

	*new { arg levels, times, curves, default;
		var size;
		var isMonoSpec;
		var isDynamic = false;

		if(levels.isSequenceableCollection.not) {
			isDynamic = true;
			levels = levels.asSpec ! 2;
		};
		if(times.isSequenceableCollection.not) {
			times = times.asSpec ! levels.size;
		};
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

		if(default.levels.size != levels.size and: { isDynamic.not }) {
			"Warning: default value size does not match the spec env size".postln;
		};

		^super.newCopyArgs(levels, times, curves, default, size, isMonoSpec, isDynamic)
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

	// TODO: others env

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
			this.levels.wrapAt(x).map(subval);
		});
		times = val.times.collect({ arg subval, x;
			this.times.wrapAt(x).map(subval);
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
						this.curves.wrapAt(x).map(subval);
					});
				}, 
				{
					curves = val.curves.collect({ arg subval, x;
						this.curves.wrapAt(x).map(subval);
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
			this.levels.wrapAt(x).unmap(subval);
		});
		times = val.times.collect({ arg subval, x;
			this.times.wrapAt(x).unmap(subval);
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
						this.curves.wrapAt(x).unmap(subval);
					});
				}, 
				{
					curves = val.curves.collect({ arg subval, x;
						this.curves.wrapAt(x).unmap(subval);
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


XNonFloatSpec { // maybe a parent for all others special spec to exclude them when making a gui ?

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
	// arg: channel count

	default { ^0 } // maybe return an empty buffer

}

XDoneActionSpec : Spec {

}

XSampleSpec : XBufferSpec {

}

XWavetableSpec : XBufferSpec {

}

XBusSpec : Spec {
	// arg: channel count

}

XInBusSpec : XBusSpec {

}

XOutBusSpec : XBusSpec {

}

XBoolSpec : Spec {
	*new { 
		^super.new;
	}

	map { arg val;
		^(val > 0)
	}

	unmap { arg val;
		^val.asInteger
	}

}

MenuSpec : Spec {
	var <>labelList, <>valueList;

	*new { arg list;
		^super.new.menuSpecInit(list);
	}

	*index { arg list;
		^super.new.indexMenuSpecInit(list);
	}

	add { arg key, val;
		if(key.class == Association) {
			labelList.add(key.key);
			valueList.add(key.value);
		} {
			labelList.add(key);
			valueList.add(val ? key);
		}
	}

	indexAdd { arg val;
		labelList.add(val);
		valueList.add(valueList.size);
	}

	menuSpecInit { arg list;
		labelList = List.new;
		valueList = List.new;
		list.do { arg x;
			if(x.class == Association) {
				labelList.add(x.key);
				valueList.add(x.value);
			} {
				labelList.add(x);
				valueList.add(x);
			}
		}
	}

	indexMenuSpecInit { arg list;
		labelList = List.new;
		valueList = List.new;
		list.do { arg x, idx;
			if(x.class == Association) {
				labelList.add(x.key);
				valueList.add(x.value);
			} {
				labelList.add(x);
				valueList.add(idx);
			}
		}
	}

	default { 
		^valueList[0]
	}

	mapIndex { arg val;
		^valueList[val.round.asInteger]
	}

	unmapIndex { arg val;
		^valueList.detectIndex({ arg x; x == val })
	}

	map { arg val;
		^this.mapIndex(val * ( valueList.size - 1))
	}

	unmap { arg val;
		^this.unmapIndex(val) / ( valueList.size - 1);
	}

}

ListIndexSpec : Spec {
	var <>fun;
	*new { arg fun;
		^super.new.fun_(fun);
	}

	map { arg val;
		^(val * ( fun.value.size - 1))
	}

	unmap { arg val;
		^((val) / ( fun.value.size - 1));
	}

	maxval { 
		^fun.value.size;
	}

	valueList {
		^(0..fun.value.size);
	}

	minval { ^0 }

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
