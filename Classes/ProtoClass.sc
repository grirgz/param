

// this class is just a dictionary stored as a class name, with the syntax of Event
// to be subclassed to easily create Class dictionaries
// usefull as a namespace
ClassMethodDictionary {
	classvar <>all;
	*initClass {
		all = IdentityDictionary.new;
	}

    *doesNotUnderstand { arg selector...args;
		if(selector.asString.endsWith("_")) {
			all[selector.asGetter] = args[0];
			^all[selector.asGetter];
		} {
			^all[selector]
		}
	}
}

// Maybe name it ClassStorage
EventClass : ClassMethodDictionary {}

//ProtoClass {
// FIXME: should be better named EventClass (but now is used everywhere)
ProtoClass : Event {
	//var <>protoclass_event;
	// FIXME: parent does not work!


	*new { arg xevent;
		var inst;
		//"bla".debug;
		//xevent.debug("xevent");
		inst = super.new;
		inst.putAll(xevent);
		inst.parent = xevent.parent; // is not copied with putAll
		^inst;
	}

	printOn { arg stream;
		this.storeOn(stream)
	}

	storeOn { arg stream;
		if(this[\refCompileString].notNil) {
			stream << this[\refCompileString].(this);
		} {
			stream << "ProtoClass(";
			super.storeOn(stream);
			stream << ")";
		}
	}

	asPattern { arg ... args;
		^this[\asPattern].(this, * args)
		//^this[\asPattern]
	}

	//at { arg ... args;
	//	^this[\asPattern].(this, * args)
	//}

	play { arg ... args;
		^this[\play].(this, * args)
	}

	stop { arg ... args;
		^this[\stop].(this, * args)
	}

	isPlaying { arg ... args;
		^this[\isPlaying].(this, * args)
	}

	source { arg ... args;
		^this[\source].(this, * args)
	}

	first { arg ... args;
		^this[\first].(this, * args)
	}

	next { arg ... args;
		^this[\next].(this, * args)
	}

	make { arg ... args;
		^this[\make].(this, * args)
	}

	quant {  arg ...args;
		// so can use tryPerform on quant
		^this[\quant].(this, * args)
	}

	quant_ { arg ...args;
		// so can use tryPerform on quant
		^this[\quant_].(this, * args)
	}

	source_ { arg ... args;
		if(this[\source_].notNil) {
			this[\source_].(this, *args)
		} {
			this[\source] = args[0]
		}
	}

	embedInStream { arg ... args;
		^this[\embedInStream].(this, * args)
	}

	clear { arg ... args;
		^this[\clear].(this, * args)
	}

	isEmpty { arg ... args;
		^this[\isEmpty].(this, * args)
	}

	isEmpty_ { arg ... args;
		if(this[\isEmpty_].notNil) {
			this[\isEmpty_].(this, * args)
		} {
			this[\isEmpty] = args[0]
		}
	}

	render { arg ... args;
		^this[\render].(this, * args)
	}

	remove { arg ... args;
		if(this[\remove].notNil) {
			^this[\remove].(this, * args)
		} {
			^this.remove(*args)
		}
	}

	numChannels { arg ... args;
		^this[\numChannels].(this, * args)
	}

	numChannels_ { arg ... args;
		if(this[\numChannels_].notNil) {
			this[\numChannels_].(this, * args)
		} {
			this[\numChannels] = args[0]
		}
	}

	set { arg ... args;
		^this[\set].(this, * args)
	}
}


ProtoDef : ProtoClass {
	//var <>key;

	*all {
		^PresetDictionary.new(\ProtoDef);
	}

	*new { arg key, val;
		if(this.all[key].isNil) {
			if(val.notNil) {
				^super.new(val).protoDef_prAdd(key)
			} {
				^super.new(()).protoDef_prAdd(key)
			}
		} {
			var ret = this.all[key];
			if(val.notNil) {
				ret.putAll(val);
				ret[\key] = key;
				ret[\parent] = val[\parent];
			};
			^ret;
		}
	}

	protoDef_prAdd { arg xkey;
		this[\key] = xkey;
		this.class.all[this.key] = this;
	}

	key {
		^this[\key]
	}

	putAll { arg ... args;
		var k = this.key;
		// preserve key
		super.putAll(*args);
		this[\key] = k;
	}

	clear {
		if(this.key.notNil) {
			this.class.all[this.key] = nil
		};
		^nil
	}

	// TODO: should be in parent class
	collect { arg ...args;
		^this[\collect].(this, *args)
	}

	do { arg ...args;
		this[\do].(this, *args)
	}

	printOn { arg stream;
		this.storeOn(stream)
	}

	storeOn { arg stream;
		stream << "%(%)".format(this.class.asString, this.key.asCompileString);
	}

}

ProtoTemplateDef : ProtoDef {
	*all {
		^PresetDictionary.new(\ProtoTemplateDef);
	}
}

ProtoClassDef {
	classvar <>protoClassDef_all;
	//classvar <>protoClassDef_key = \ProtoClassDefKey;

	*initClass {
		Class.initClassTree(PresetDictionary);
		protoClassDef_all = PresetDictionary.new(\ProtoClassDef);
	}

	*new { arg key, val;
		if(val.notNil) {
			if(val.isKindOf(ProtoClass).not) {
				val = ProtoClass(val);
			};
			protoClassDef_all[key] = val;
			^val
		} {
			^protoClassDef_all[key];
		}
	}

	*protoClassDef_clear { arg key;
		if(key.notNil) {
			protoClassDef_all[key] = nil
		};
		^nil
	}

	// TODO: should be in parent class
	collect { arg ...args;
		^this[\collect].(this, *args)
	}

	do { arg ...args;
		this[\do].(this, *args)
	}

	printOn { arg stream;
		this.storeOn(stream)
	}

	storeOn { arg stream;
		stream << "%(%)".format(this.class.asString, this.key.asCompileString);
	}

}

ProtoInst {
	*new { arg templateKey ...args;
		^ProtoClassDef(templateKey).new(*args)
	}
}
