
// A proposal for standard specs for synthdefs: array, env, bus, buffer, trig, gate, doneAction, wavetable
// Add also useful spec for list of labels associated with a value: MenuSpec, MenuSpecDef
// X is because name is already taken in cruciallib, should find a better name, but it's core API of Param quark :/


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
			//val.debug("nested!");
			val = val[0];
			nested = true;
		};
		res = val.collect({ arg subval, x;
			//[ this.array, this.array.at(x), this.at(x), this.array.wrapAt(x), subval, x ].debug("collect!");
			this.array.wrapAt(x).unmap(subval);
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
	var zerospec;
	var defaultLevelSpec

	*new { arg levels, times, curves, default;
		var size;
		var isMonoSpec;
		var isDynamic = false;

		zerospec = ControlSpec(0,0.000000001,\lin);
		defaultLevelSpec = ControlSpec(0,2,\lin,0,0.1);

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

	*adsr { arg attackTime, decayTime, sustainLevel, releaseTime, peakLevel;
		var inst;
		attackTime = attackTime ? defaultLevelSpec;
		decayTime = decayTime ? attackTime;
		sustainLevel = sustainLevel? attackTime;
		releaseTime = releaseTime ? attackTime;
		peakLevel = peakLevel ? sustainLevel;
		inst = this.new([ zerospec, peakLevel, sustainLevel, zerospec], [attackTime, decayTime, releaseTime]);
		^inst
	}


	*dadsr { arg delayTime, attackTime, decayTime, sustainLevel, releaseTime;
		var inst;
		delayTime = delayTime ? ControlSpec(0,2,\lin,0,0);
		attackTime = attackTime ? defaultLevelSpec;
		decayTime = decayTime ? attackTime;
		sustainLevel = sustainLevel? attackTime;
		releaseTime = releaseTime ? attackTime;
		peakLevel = peakLevel ? sustainLevel;
		inst = this.new([ zerospec, zerospec, peakLevel, sustainLevel, zerospec], [delayTime, attackTime, decayTime, releaseTime]);
		^inst
	}

	*asr { arg attackTime, sustainLevel, releaseTime;
		var inst;
		attackTime = attackTime ? defaultLevelSpec;
		sustainLevel = sustainLevel ? attackTime;
		releaseTime = releaseTime ? attackTime;
		inst = this.new([ zerospec, sustainLevel, zerospec], [attackTime, releaseTime]);
		^inst
	}

	*perc { arg attackTime, releaseTime, level;
		var inst;
		attackTime = attackTime ? defaultLevelSpec;
		level = level ? attackTime;
		releaseTime = releaseTime ? attackTime;
		inst = this.new([ zerospec, level, zerospec], [attackTime, releaseTime]);
		^inst
	}

	*triangle { dur, level;
		dur = dur ? defaultLevelSpec;
		level = level ? dur;
		inst = this.new([ zerospec, level, zerospec], [dur, dur]);
	}

	// TODO: others env

	*sine {
		"NOT IMPLEMENTED".throw;
	}

	*linen {
		"NOT IMPLEMENTED".throw;
	}

	*cutoff {
		"NOT IMPLEMENTED".throw;
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


XNonFloatSpec : Spec { // maybe a parent for all others special spec to exclude them when making a gui ?
	var <>default;

}

XGateSpec : XNonFloatSpec {
	var <default;
	*new {
		^super.new(0,1,\lin,1,0);
	}

	default_ { arg val;
		default = val;
	}
}

XTrigSpec : XNonFloatSpec {
	var <default;
	*new {
		^super.new(0,1,\lin,1,0);
	}

	default_ { arg val;
		default = val;
	}
}

XBufferSpec : XNonFloatSpec {
	// arg: channel count

	default { ^0 } // maybe return an empty buffer

}

XSampleSpec : XBufferSpec {
	// WIP
	// FIXME: how to specify sampler parameters and list of available buffers/samples ?
	var <>numChannels, <>startParamName, sustainParamName, endParamName, speedParamName, startType, sustainType, endType;
	new { arg self, numChannels, startParamName, sustainParamName, endParamName, speedParamName, startType, sustainType, endType;
		^this.newCopyArgs(numChannels, startParamName, sustainParamName, endParamName, speedParamName, startType, sustainType, endType);
	}
}

XDoneActionSpec : XNonFloatSpec {

}

XWavetableSpec : XBufferSpec {

}

XBusSpec : XNonFloatSpec {
	// arg: channel count

}

XInBusSpec : XBusSpec {

}

XOutBusSpec : XBusSpec {

}

XBoolSpec : XNonFloatSpec {
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

///////////////////////////// Menu

MenuSpec : XNonFloatSpec {
	var <>labelList, <>valueList;
	var <>list;

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

	addUnique { arg key, val;
		if(key.class == Association) {
			if(labelList.includes(key.value).not) {
				list.add(key.key -> key.value);
				labelList.add(key.key);
				valueList.add(key.value);
			}
		} {
			var xval = val ? key;
			if(labelList.includes(xval).not) {
				list.add(key -> xval);
				labelList.add(key);
				valueList.add(xval);
			}
		}
	}

	indexAdd { arg val;
		labelList.add(val);
		valueList.add(valueList.size);
	}

	menuSpecInit { arg xlist;
		list = xlist;
		labelList = List.new;
		valueList = List.new;
		xlist.value.do { arg x;
			if(x.class == Association) {
				labelList.add(x.key);
				valueList.add(x.value);
			} {
				labelList.add(x);
				valueList.add(x);
			}
		}
	}

	indexMenuSpecInit { arg xlist;
		list = xlist;
		labelList = List.new;
		valueList = List.new;
		xlist.value.do { arg x, idx;
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

MenuSpecDef : MenuSpec {
	classvar <>all;
	var <>key;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { arg key, val;
		if(all[key].isNil) {
			if(val.notNil) {
				^super.new.init(val).prAdd(key)
			} {
				^super.new.init([]).prAdd(key)
			}
		} {
			var ret = all[key];
			if(val.notNil) {
				ret.source = val
			};
			^ret;
		}
	}

	source { 
		^this.list;
	}

	source_ { arg val;
		this.menuSpecInit(val)
	}

	prAdd { arg xkey;
		key = xkey;
		all[key] = this;
	}

	init { arg val;
		this.menuSpecInit(val);
	}

	clear {
		if(key.notNil) {
			all[key] = nil
		};
		^nil
	}
}

MenuSpecFuncDef : MenuSpecDef {
	*initClass {
		all = IdentityDictionary.new;
	}

	labelList {
		var ret = List.new;
		list.value.do { arg x;
			if(x.class == Association) {
				ret.add(x.key);
			} {
				ret.add(x);
			}
		};
		^ret;
	}

	valueList {
		var ret = List.new;
		list.value.do { arg x;
			if(x.class == Association) {
				ret.add(x.value);
			} {
				ret.add(x);
			}
		};
		^ret;
	}
}

ListIndexSpec : XNonFloatSpec {
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

