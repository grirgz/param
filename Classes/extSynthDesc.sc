
// the goal is to extend SynthDesc to be able to access info about a SynthDef by key: SynthDesc(\sampler) for the \sampler SynthDef
// this class make use of experimental class SpecGroup

+SynthDesc {
	*new { arg name;
		if(name.isNil) {
			^super.new
		} {
			^SynthDescLib.global.at(name)
		}
	}

	params { 
		^this.controls.collect { arg control;
			var ret;
			var spec;
			if(control.name == '?') {
				ret = nil;
			} {
				spec = this.getSpec(control.name.asSymbol);
				if(spec.isNil) {
					ret = control.name.asSymbol;
				} {
					ret = [control.name.asSymbol, spec];
				};
			};
			ret
		}.select(_.notNil);
	}

	asParamGroup { arg target;
		var sgroup = SpecGroup.newFrom(this.params);
		if(sgroup.notNil) {
			^sgroup.asParamGroup(target)
		} {
			Log(\Param).info("SynthDesc: synth definition not found: %", this.name);
			^ParamGroup.new
		}
	}

	specs {
		var val;
		val = this.metadata;
		if(val.notNil) {
			val = val.specs;
			if(val.notNil) {
				^val.composeEvent(this.getHalo(\specs))
			} {
				^this.getHalo(\specs)
			}
		} {
			^this.getHalo(\specs)
		}
	}

	defaultValue { arg argname;
		var val;
		var con = this.controlDict[argname];
		if(con.notNil) {
			val = con.defaultValue;
		}
		^val;
	}

	getSpec { arg name;
		var val;
		var rval;
		if(super.getSpec(name).notNil) {
			rval = super.getSpec(name)
		} {
			val = this.metadata;
			if(val.notNil) {
				val = val.specs;
				if(val.notNil) {
					rval = val[name];
				}
			};

		};
		^rval;
	}
	
}


