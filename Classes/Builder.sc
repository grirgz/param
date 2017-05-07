
///////////////////////////// Builder 

Builder {
	var <source;
	var >envir;
	var <>key;
	classvar <all;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { arg key, fun;
		if(all[key].isNil) {
			all[key] = this.make(fun).key_(key);
			^all[key];
		} {
			var ins;
			ins = all[key];
			if(fun.notNil and: { ins.notNil }) {
				ins.source = fun;
			};
			^ins;
		}
	}

	*make { arg fun;
		var ins = super.new;
		ins.source = fun;
		ins.envir[\builder] = ins;
		^ins;
	}

	source_ { arg fun;
		if( fun.isNil ) {
			fun = {}
		};
		source = fun;
		this.class.functionArgsToEnvir(fun).keysValuesDo { arg k, v;
			if( this.envir[k].isNil ) {
				this.envir[k] = v;
			};
		};
	}
	 
	envir {
		if(envir.isNil) { 
			envir = IdentityDictionary.new;
			envir[\builder] = this;
		};
		^envir
	}

	*functionArgsToEnvir { arg fun;
		var env = ();
		fun.def.argNames.do { arg name, x;
			env[name] = fun.def.prototypeFrame[x]
		};
		^env;
	}

	build {
		^source.valueWithEnvir(this.envir);
	}

	set { arg ...args;
		var hasChanged = false;
		args.pairsDo { arg key, val; 
			if(this.envir.at(key) != val) {
				this.envir.put(key, val);
				hasChanged = true;
			}
		};
		if(hasChanged) {
			this.build;
			this.changed(\set, args);
		}
	}

	unset { arg ... args;
		args.do { arg key; this.envir.removeAt(key) };
		this.changed(\unset, args);
	}

	get { arg key;
		^this.envir[key];
	}
	
}
