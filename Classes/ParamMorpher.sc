
// act also as a Param wrapper
ParamValue : BaseParam {
	var <>value=0;

	*new { arg spec;
		^super.new.initParamValue(spec);
	}

	initParamValue { arg xspec;
		Log(\Param).debug("initParamValue %", xspec);
		target = this;
		property = \value;
		label = "ParamValue";
		spec = xspec.asSpec ? Param.defaultSpec;
		Log(\Param).debug("initParamValue2 %", xspec);
		Log(\Param).debug("initParamValue2 %", spec);
	}

	property_ { arg val;
		property = val;
	}

	get {
		^value;
	}

	asParam {
		^Param.fromWrapper(this)
	}

	set { arg val;
		value = val;
		this.changed(\set, [\value, val]);
	}

	label {
		^label
	}

	map { arg val;
		^this.spec.map(val)
	}

	unmap { arg val;
		^this.spec.unmap(val)
	}

	// normGet and normSet are not used by morpher because of additional code on setter/getter
	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\set, { arg ...args; 
			// args: object, \set, keyval_list
			//args.debug("args");

			// update only if concerned key is set
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug
			if(args[2].notNil) {
				if(args[2].any({ arg x; x == param.property })) {
					action.(view, param);
				}
			} {
				action.(view, param);
			}
		});
	}
}

// act also as a Param
ParamMorpher : Param {
	var <group, <>presets;
	var <>key;
	var <>optimizedPresets;
	*new { arg group, presets;
		// presets is the number of presets to take
		var pval = ParamValue.new;
		var inst;
		pval.spec = ControlSpec(0,presets.size-1,\lin,0,0);
		inst = super.newWrapper(pval, presets);
		inst.initParamMorpher(group, presets);
		^inst;
	}

	initParamMorpher { arg arggroup, argpresets;
		if(arggroup.class == Symbol) {
			group = ParamGroupDef(arggroup)
		} {
			group = arggroup;
		};
		presets = argpresets;
		//[group, presets].debug("initParamMorpher");
	}

	morph { arg list, morph;
		^list.blendAt(morph)
	}

	asLabel {
		// TODO
		^(key ?? {
			if(group.class == ParamGroupDef) {
				group.key
			} {
				nil
			}
		} ?? {presets.asString})
	}

	optimizeMorphing {
		var first;
		var presetgrid;
		if(presets.size > 0) {
			// a group preset contains a snapshot of the value of each param of the group
			// a param preset contains each value a param can take in presets
			optimizedPresets = List.new;
			first = group.getPreset(presets[0]);
			presetgrid = presets.collect{ arg x; group.getPreset(x) }; // list of group presets
			presetgrid.flop.do { arg parampreset, x; // list of param presets
				if(parampreset.any({ arg x; x != parampreset[0] })) {
					// different values, so do the morphing
					optimizedPresets.add(x)
				} {
					// same values, dont add it for morphing
				};
			};
		}

	}

	disableOptimize {
		optimizedPresets = nil;
	}

	set { arg val;
		var presets_vals;
		var iter;
		//val.debug("ParamMorpher: set");
		this.wrapper.set(val);
		presets_vals = presets.collect({ arg x; 
			var res = group.getPreset(x);
			if(res.isNil) {
				"Error: preset % is not defined".format(x.asCompileString).postln;
				^nil
			};
			res;
		});
		//[presets_vals, presets].debug("presets");
		presets_vals = presets_vals.flop;
		if(group.size != presets_vals.size) {
			"Error: preset size (%) don't match group size (%)".format(presets_vals.size, group.size).postln;
			^nil;
		};

		if(optimizedPresets.notNil) {
			iter = optimizedPresets;
		} {
			iter = group.size;
		};
		iter.do({ arg x;
			var resval;
			var param = group.at(x);
			//[x, presets_vals[x], val].debug("morph.set: groupdo");
			resval = this.morph(presets_vals[x], val);
			//[param.asLabel, val].debug("ParamMorpher: param set");
			param.set(resval);
		})
	}

	get {
		^this.wrapper.get;
	}

	normGet {
		^this.spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(this.spec.map(val))
	}
}

ParamMorpherDef : ParamMorpher {
	classvar lib;

	*initClass {
		Class.initClassTree(IdentityDictionary);
		lib = IdentityDictionary.new;
	}

	*new { arg defkey, group, presets;
		var inst;
		if(group.isNil) {
			^lib[defkey]
		} {
			if(lib[defkey].isNil) {
				inst = super.new(group, presets);
				inst.key = defkey;
				lib[defkey] = inst;
				^inst
			} {
				"Warning: already defined, use .clear before redefine it".postln;
				^lib[defkey]
			}
		}
	}

	clear {
		lib[this.key] = nil
	}
}

PresetListMorpher : ParamMorpher {
	var <disabledPresets;
	var <>size;
	*new { arg group, size, prefix=\preset;
		var pval = ParamValue.new;
		var inst;
		pval.spec = ControlSpec(0,size,\lin,0,0);
		inst = super.newWrapper(pval);
		inst.initPresetListMorpher(group, size, prefix);
		^inst;
	}

	initPresetListMorpher { arg group, xsize, prefix;
		var prelist;
		size = xsize;
		disabledPresets = Set.new;
		prelist = size.collect { arg x; ( prefix++x ).asSymbol };
		this.initParamMorpher(group, prelist);
	}

	set { arg val;
		var presets_vals;
		var xsize;
		var iter;
		//val.debug("ParamMorpher: set");
		this.wrapper.set(val);
		presets_vals = presets.collect({ arg x; 
			var res = group.getPreset(x);
			if(disabledPresets.includes(x).not) {
				res;
			} {
				nil;
			}
		});
		presets_vals = presets_vals.reject({ arg x; x.isNil });
		xsize = presets_vals.size;
		//[presets_vals, presets].debug("presets");
		if(xsize == 0) {
			Log(\Param).warning("PresetListMorpher.set: WARNING: not preset in final list, do nothing");
		} {
			val = val.linlin(0, size-1, 0, xsize-1 );
			presets_vals = presets_vals.flop;
			if(group.size != presets_vals.size) {
				Log(\Param).error("Error: preset size (%) don't match group size (%)".format(presets_vals.size, group.size));
				^nil;
			};
			if(optimizedPresets.notNil) {
				iter = optimizedPresets;
			} {
				iter = group.size;
			};
			iter.do({ arg x;
				var resval;
				var param = group.at(x);
				//[x, presets_vals[x], val].debug("morph.set: groupdo");
				resval = this.morph(presets_vals[x], val);
				//[param.asLabel, val].debug("ParamMorpher: param set");
				param.set(resval);
			})
		};
	}

	getPreset { arg preset_index;
		^group.getPreset(presets[preset_index]);
	}

	save { arg preset_index;
		group.save(presets[preset_index]);
		//this.updateOptimizer;
	}

	load { arg preset_index;
		group.load(presets[preset_index]);
	}

	erase { arg preset_index;
		group.erase(presets[preset_index]);
	}

	enablePreset { arg preset_index;
		disabledPresets = disabledPresets.remove(presets[preset_index]);
	}

	isEnabled { arg preset_index; 
		^disabledPresets.includes(presets[preset_index]).not;
	}

	disablePreset { arg preset_index;
		disabledPresets = disabledPresets.add(presets[preset_index]);
	}

	toggleEnablePreset { arg preset_index;
		if(this.isEnabled(preset_index)) {
			this.disablePreset(preset_index)
		} {
			this.enablePreset(preset_index)
		}
	}
}

PresetListMorpherDef : PresetListMorpher {
	classvar <>all;
	*initClass {
		Class.initClassTree(IdentityDictionary);
		all = IdentityDictionary.new;
	}

	*new { arg key, group, size, prefix=\preset;
		var inst;
		if(all[key].isNil) {
			inst = super.new(group, size, prefix);
			inst.key = key;
			all[key] = inst;
		} {
			if(group.notNil) {
				// TODO: provide a way to disable this message, and put key name in message
				"Warning: PresetListMorpherDef: already defined, use .clear before redefine it".postln;
			};
			inst = all[key];
		};
		^inst
	}

	clear {
		all[key] = nil;
		^nil
	}
}
