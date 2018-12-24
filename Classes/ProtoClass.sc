

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
}


ProtoDef : ProtoClass {
	classvar <>all;
	//var <>key;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { arg key, val;
		if(all[key].isNil) {
			if(val.notNil) {
				^super.new(val).prAdd(key)
			} {
				^super.new(()).prAdd(key)
			}
		} {
			var ret = all[key];
			if(val.notNil) {
				ret.putAll(val);
				ret[\key] = key;
				ret[\parent] = val[\parent];
			};
			^ret;
		}
	}

	prAdd { arg xkey;
		this[\key] = xkey;
		all[this.key] = this;
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
			all[this.key] = nil
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
	// just another placeholder
}
