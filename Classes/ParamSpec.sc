
// A proposal for standard specs for synthdefs: array, env, bus, buffer, trig, gate, doneAction, wavetable
// Add also useful spec for list of labels associated with a value: MenuSpec, MenuSpecDef
// X is because name is already taken in cruciallib, should find a better name, but it's core API of Param quark :/

ParamBaseSpec : Spec {
	setFrom {
		// since 3.11 this is needed

	}

	step {
		// used by MultiSliderView
		^0
	}

	range {
		^1
	}

}

///////////////////////////// Base

ParamArraySpec : ParamBaseSpec {
	var array, default;
	var size, <>isMonoSpec;
	var isDynamic;

	*new { arg array, default=nil;
		var inst;
		inst = super.new;
		inst.array = array;
		inst.default = default;
		^inst;
	}

	default {
		if(this.isDynamic) {
			^[default]
		} {
			^default;
		}

	}

	size {
		if(this.isDynamic) {
			^size
		} {
			^array.size;
		};
	}
	
	size_ { arg val;
		if(this.isDynamic) {
			size = val;
		} {
			this.array = this.array.extend(val)
		};
	}

	default_ { arg val;
		if(val.isNil) {
			if(this.isDynamic) {
				default = array.default;
			} {
				default = array.collect(_.default);
			}
		} {
			default = val;
		};

		//if(default.size != array.size and: { isDynamic.not }) {
			//"Warning: default value size does not match the spec array size".postln;
		//};
		
	}

	dynamicSpec {
		if(this.isDynamic) {
			^array
		} {
			^array[0]
		};
	}

    array {
		if(this.isDynamic) {
			^[array]
		} {
			^array;
		}
    }
    
    isDynamic {
		^array.isSequenceableCollection.not;
    }

    array_ { arg val;
		//val.debug("array_ val");
		if(val.isSequenceableCollection.not) {
			if(val.isNil) {
				^Error("ParamArraySpec: spec is nil or not in Spec library: "++array.asCompileString).throw
			} {
				array = val.asSpec;
				size = 1;
			};
			isMonoSpec = true;
		} {
			val.do({ arg sp;
				if(sp.isNil) {
					^Error("ParamArraySpec: spec is nil or not in Spec library: "++array.asCompileString).throw
				}
			});
			array = val.collect(_.asSpec);
			size = array.size; // not really used when not dynamic
			if(
				array.any { arg val;
					val != array[0]
				};
			) {
				isMonoSpec = false
			} {
				isMonoSpec = true
			};
		};
    }

	numChannels {
		^this.size
	}

	storeArgs {
		^[array, default]
	}

	at { arg idx;
		^this.array.wrapAt(idx);
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

	step {
		^this.dynamicSpec.step
	}
	range {
		^this.dynamicSpec.range
	}
}


StepListSpec : ParamArraySpec {
	default {
		^StepList.newFrom(array.collect(_.default))
	}
}

ParamEnvSpec : ParamBaseSpec {
	var <levels, <times, <curves;
	var <>default;
	var <size;
	var <isMonoSpec;
	var <isDynamic=false;
	var type=\default;
	classvar zerospec;
	classvar defaultLevelSpec;

	*classInit {
		zerospec = ControlSpec(0,0.000000001,\lin);
		defaultLevelSpec = ControlSpec(0,2,\lin,0,0.1);
	}

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

		//curves.debug("ParamEnvSpec:curves");

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

    levels_ { arg val;
		if(val.isSequenceableCollection.not) {
			isDynamic = true;
			levels = val.asSpec ! 2;
		};
		levels = levels.collect(_.asSpec);
		size = levels.size;
    }

	times_ { arg val;

		if(times.isSequenceableCollection.not) {
			times = times.asSpec ! levels.size;
		};
		times = times.asArray.wrapExtend(size - 1);
		times = times.collect(_.asSpec);
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
		inst.default = Env.adsr(0.1,0.1,0.8,0.1);
		^inst
	}


	*dadsr { arg delayTime, attackTime, decayTime, sustainLevel, releaseTime, peakLevel;
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

	*triangle { arg dur, level;
		var inst;
		dur = dur ? defaultLevelSpec;
		level = level ? dur;
		inst = this.new([ zerospec, level, zerospec], [dur, dur]);
		^inst;
	}

	*sine { arg dur, level;
		var inst;
		dur = dur ? defaultLevelSpec;
		level = level ? defaultLevelSpec;
		inst = this.new([level, level, level], dur, \sine);
		^inst;
	}

	*linen { arg attackTime, sustainTime, releaseTime, level, curve;
		var inst;
		level = level ? defaultLevelSpec;
		inst = this.new(level, [attackTime, sustainTime, releaseTime], curve);
		^inst;
	}

	*cutoff { arg releaseTime, level, curve;
		var inst;
		level = level ? defaultLevelSpec;
		inst = this.new([level, level], releaseTime, curve);
		^inst;
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

ParamAudioSpec : ParamBaseSpec {
}


///////////////////////////// Non-float

ParamNonFloatSpec : ParamBaseSpec { // maybe a parent for all others special spec to exclude them when making a gui ?
	var <>default;
	constrain { arg val;
		^val;
	}
	map { arg val;
		^val;
	}
	unmap { arg val;
		^val;
	}
}

ParamGateSpec : ParamNonFloatSpec {
	var <default;
	*new {
		^super.new(0,1,\lin,1,0);
	}

	default_ { arg val;
		default = val;
	}
}

ParamTrigSpec : ParamNonFloatSpec {
	var <default;
	*new {
		^super.new(0,1,\lin,1,0);
	}

	default_ { arg val;
		default = val;
	}
}

ParamBufferSpec : ParamNonFloatSpec {
	var >tagSpec;
	var <>numChannels;

	*new { arg numChannels;
		^super.new.initParamBufferSpec(numChannels);
	}

	initParamBufferSpec { arg innumChannels;
		numChannels = innumChannels;
	}

	tagSpec { arg self;
		^tagSpec ?? { 
			switch(numChannels,
				1, { TagSpecDef(\BufDef_mono) },
				2, { TagSpecDef(\BufDef_stereo) },
				{ TagSpecDef(\BufDef) }
			);
		}
	}

	default { ^0 } // maybe return an empty buffer

}

ParamAudioBufferSpec : ParamBufferSpec {
	// WIP
	// FIXME: how to specify sampler parameters and list of available buffers/samples ?
	var <>startParamName, <>sustainParamName, <>endParamName, <>speedParamName, <>startType, <>sustainType, <>endType;
	new { arg self, numChannels, startParamName, sustainParamName, endParamName, speedParamName, startType, sustainType, endType;
		^this.newCopyArgs(numChannels, startParamName, sustainParamName, endParamName, speedParamName, startType, sustainType, endType);
	}
}

ParamDoneActionSpec : ParamNonFloatSpec {

}

ParamWavetableSpec : ParamBufferSpec {

	tagSpec { arg self;
		^tagSpec ?? { 
			TagSpecDef(\WavetableDef_object)
		}
	}
}

///////////////////////////// Busses

ParamBusSpec : ParamNonFloatSpec {
	var <>numChannels;
	var >tagSpec;
	*new { arg spec, numChannels;
		var inst = super.new;
		if(spec.notNil) {
			inst.tagSpec = spec;
		};
		if(numChannels.notNil) {
			inst.numChannels = numChannels;
		};
		^inst;
	}

	tagSpec {
		^tagSpec ?? { TagSpecDef(\BusDef) }
	}

}

ParamAudioBusSpec : ParamBusSpec {
	tagSpec { 
		^tagSpec ?? { TagSpecDef(\BusDef_audio) }
	}
}

ParamControlBusSpec : ParamBusSpec {
	tagSpec { 
		^tagSpec ?? { TagSpecDef(\BusDef_control) }
	}
}

ParamMappedBusSpec : ParamBusSpec {
	tagSpec {
		^tagSpec ?? { TagSpecDef(\BusDef_asMap) }
	}
}


ParamMappedControlBusSpec : ParamMappedBusSpec {
	tagSpec { 
		^tagSpec ?? { TagSpecDef(\BusDef_control_asMap) }
	}
}

ParamMappedAudioBusSpec : ParamMappedBusSpec {
	tagSpec { 
		^tagSpec ?? { TagSpecDef(\BusDef_control_asMap) }
	}
}

ParamInBusSpec : ParamBusSpec {

}

ParamOutBusSpec : ParamBusSpec {

}

///////////////////////////// Misc

ParamBoolControlSpec : ControlSpec {

}

ParamBoolSpec : ParamNonFloatSpec {
	var <>reverse = false;
	*new { arg reverse=false;
		^super.new.reverse_(reverse).default_(false);
	}

	range {
		^1
	}

	step {
		^1
	}

	map { arg val;
		if(reverse) {
			^(val <= 0)
		} {
			^(val > 0)
		}
	}

	unmap { arg val;
		if(reverse) {
			if(val.isSequenceableCollection) {
				^val.collect({ arg x; x.not.asInteger })
			} {
				^val.not.asInteger;
			}
		} {
			^val.asInteger
		}
	}

}

ParamStringSpec : ParamNonFloatSpec {
	// only used by TextField views to not display quotes
  
	*new { 
		^super.new.default_("");
	}

	range {
		^1
	}

	step {
		^1
	}

	map { arg val;
		^val
	}

	unmap { arg val;
		^val
	}
}

///////////////////////////// Menu

TagSpec : ParamNonFloatSpec {
	var list; // forced to List
	var <>dynamicLists;
	//var dirty = true; // cache not really implemented

	*new { arg list;
		^super.new.menuSpecInit(list);
	}

	*index { arg list;
		^super.new.indexMenuSpecInit(list);
	}

	addDynamicList { arg dlist;
		// symbol -> { function returning a list }
		// dynamicList is also an association
		// key is unused currently but could serve to remove
		if(dlist.isKindOf(Association)) {
			dynamicLists.add(dlist);
		} {
			"TagSpec: dynamic list should be an association: %".format(dlist).error
		};
		this.changed(\list);
		//dirty = true;
	}

	addUniqueDynamicList { arg newdlist;
		if(dynamicLists.every({ arg dlist; dlist.key != newdlist.key })) { 
			this.addDynamicList(newdlist)
		};
	}

	replaceDynamicList { arg newdlist;
		var idx = dynamicLists.detectIndex({ arg dlist; dlist.key == newdlist.key });
		if(idx.notNil) {
			this.dynamicLists[idx] = newdlist;
			this.changed(\list);
		} {
			this.addDynamicList(newdlist)
		};
	}

	add { arg key, val;
		if(key.class == Association) {
			list.add(key)
		} {
			list.add(key -> ( val ? key ))
		};
		this.changed(\list);
	}

	valueList {
		^this.associationList.collect(_.value)
	}

	labelList {
		^this.associationList.collect(_.key)
	}

	keyList {
		^this.labelList
	}

	associationList {
		var ret = list.copy;
		// Note: if there is already an item in the list the dynamic list will put it in double
		dynamicLists.do({ arg dlist;
			try {
				dlist.value.value.do { arg x; // asso.value then execute
					if(x.isKindOf(Association)) {
						ret.add(x);
					} {
						ret.add(x -> x);
					}
				}
			} { arg err;
				"In TagSpec: dynamic list: %".format(dlist).error;
				err.reportError;
			}
		});
		//dirty = false;
		^ret;
	}

	list {
		^this.associationList
	}

	staticList {
		^list
	}

	addUnique { arg key, val;
		if(key.class == Association) {
			if(this.labelList.includes(key.key).not) {
				list.add(key.key -> key.value);
				//dirty = true;
			} {
				// replace existing non dynamic value, important if previous value is nil somehow
				var idx = list.detectIndex({ arg it; it.key == key.key });
				if(idx.notNil) {
					list[idx] = key.key -> key.value;
				}
			}
		} {
			var xval = val ? key;
			if(this.labelList.includes(key).not) {
				list.add(key -> xval);
				//dirty = true;
			}
		}
	}

	addUniq { arg ...args;
		^this.addUnique(*args);
	}

	indexAdd { arg val;
		this.add(val, list.size);
	}

	menuSpecInit { arg xlist;
		dynamicLists = dynamicLists ?? { List.new }; // don't erase dynlist by default
		if(xlist.isKindOf(Function)) { // can init with a dynamic list
			this.replaceDynamicList(\list -> xlist);
			xlist = [];
		};
		if(xlist.isKindOf(Association)) { // can init with a dynamic list
			this.replaceDynamicList(xlist);
			xlist = [];
		};
		list = List.new;
		xlist.do({ arg item;
			this.add(item)
		});
	}

	indexMenuSpecInit { arg xlist;
		if(xlist.isKindOf(Function)) { // can init with a dynamic list
			this.addDynamicList(\list -> xlist);
			xlist = [];
		};
		if(xlist.isKindOf(Association)) { // can init with a dynamic list
			this.addDynamicList(xlist);
			xlist = [];
		};
		list = List.new;
		dynamicLists = dynamicLists ? List.new; // don't erase dynlist by default
		xlist.do({ arg item;
			this.indexAdd(item)
		});
	}

	default { 
		if(default.notNil) {
			^default
		} {
			^this.valueList[0]
		};
	}

	mapIndex { arg val;
		^this.valueList[val.round.asInteger]
	}

	unmapIndex { arg val;
		^this.valueList.detectIndex({ arg x; x == val })
	}

	mapKey { arg val;
		^this.associationList.asDict[val];
	}

	asDict { arg val;
		^this.associationList.asDict;
	}

	unmapKey { arg val;
		^this.associationList.detect({ arg x; x.value == val }) !? _.key
	}

	map { arg val;
		^this.mapIndex(val * ( this.valueList.size - 1))
	}

	unmap { arg val;
		^this.unmapIndex(val) ? 0 / ( this.valueList.size - 1);
	}
}

TagSpecDef : TagSpec {
	classvar <>all;
	var <>key;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { arg key, val;
		ParamProto.init; // to load GlobalLibrary
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

    printOn { arg stream;
      this.storeOn(stream); 
    }

	storeOn { arg stream;
        stream << "TagSpecDef(%)".format(this.key.asCompileString);
	}
}



ListIndexSpec : ParamNonFloatSpec {
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


//////////////////////////// Deprecated
XBoolSpec : ParamBoolSpec {}
XOutBusSpec : ParamOutBusSpec {}
XInBusSpec : ParamInBusSpec {}
XControlBusSpec : ParamControlBusSpec {}
XAudioBusSpec : ParamAudioBusSpec {}
XWavetableSpec : ParamWavetableSpec {}
XSampleSpec : ParamAudioBufferSpec {} // compat
XTrigSpec : ParamTrigSpec {}
XGateSpec : ParamGateSpec {}
XAudioSpec : ParamAudioSpec {}
XArraySpec : ParamArraySpec {}
XNonFloatSpec : ParamNonFloatSpec {}
XDoneActionSpec : ParamDoneActionSpec {}
XBusSpec : ParamBusSpec {}
XEnvSpec : ParamEnvSpec {}
XBufferSpec : ParamBufferSpec {}

MenuSpec : TagSpec { } // compat
MenuSpecDef : TagSpecDef { } // compat

MenuSpecFuncDef : TagSpecDef { } // deprecated
ParamSampleSpec : ParamAudioBufferSpec {} // compat
