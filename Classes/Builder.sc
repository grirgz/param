
///////////////////////////// Builder 

Builder {
	var <source;
	var >envir;
	var <>key;
	var <>proxy;
	var <>editor;
	var >proto;
	var >label;
	classvar <all;

	*initClass {
		all = IdentityDictionary.new;
	}

	label {
		^label ?? { this.key }
	}

	proto {
		if(proto.isNil) {
			proto = ProtoClass(());
		};
		^proto;
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
		// can be used to make a nameless builder
		var ins = super.new;
		ins.source = fun;
		//ins.envir[\builder] = ins;
		^ins;
	}

	source_ { arg fun;
		if( fun.isNil ) {
			fun = {}
		};
		if(fun.isKindOf(Builder)) {
			Halo.put(this, Halo.at(fun).copy); // copy Halo specs
			fun = fun.source;
		};
		source = fun;
		this.class.functionArgsToEnvir(fun, this).keysValuesDo { arg k, v;
			if( this.envir[k].isNil ) {
				this.envir[k] = v;
			};
		};
	}

	asParamGroup {
		^ParamGroup(this.source.def.argNames.drop(1).collect { arg k;
			Param(this, k);
		});
	}
	 
	envir {
		if(envir.isNil) { 
			envir = IdentityDictionary.new;
			//envir[\builder] = this;
		};
		^envir
	}

	*functionArgsToEnvir { arg fun, inst;
		var env = ();
		fun.def.argNames.do { arg name, x;
			if(x == 0) {
				env[name] = inst;
			} {
				env[name] = fun.def.prototypeFrame[x]
			};
		};
		^env;
	}

	build {
		^source.valueWithEnvir(this.envir);
	}

	buildInit {
		source.valueWithEnvir(this.envir);
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
		this.changed(\unset, args); // FIXME: nobody listen on \unset
	}

	get { arg key;
		^this.envir[key];
	}
	
	printOn { arg stream;
		this.storeOn(stream)
	}

	storeOn { arg stream;
		stream << "%(%)".format(this.class.asString, this.key.asCompileString);
	}

	///// player support

	play {
		proxy.play;
	}

	stop {
		proxy.stop;
	}

	isPlaying {
		^PlayerWrapper(proxy).isPlaying;
	}

	isPlaying_ { arg val;
		PlayerWrapper(proxy).isPlaying = val;
	}

	edit {
		editor.front;
	}

	asView {
		editor.asView;
	}

}
