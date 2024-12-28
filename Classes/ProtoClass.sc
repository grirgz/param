

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


	*new { arg xevent;
		var inst;
		//"bla".debug;
		//xevent.debug("xevent");
		inst = super.new;
		if(xevent.notNil) {
			inst.putAll(xevent);
			inst.parent = xevent.parent; // is not copied with putAll
		}
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

	isPlaying_ { arg ... args;
		if(this[\isPlaying_].notNil) {
			^this[\isPlaying_].(this, * args)
		} {
			this[\isPlaying] = args[0];
		}
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


	asStream { arg ... args;
		if(this[\asStream].notNil) {
			^this[\asStream].(this, * args)
		} {
			^super.asStream;
		}
	}

	streamArg { arg ... args;
		if(this[\streamArg].notNil) {
			^this[\streamArg].(this, * args)
		} {
			^super.streamArg;
		}
	}

	embedInStream { arg ... args;
		^this[\embedInStream].(this, * args)
	}

	removeAll { arg ... args;
		^this[\removeAll].(this, * args)
	}

	clear { arg ... args;
		^this[\clear].(this, * args)
	}

	update { arg ... args;
		^this[\update].(this, * args)
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

	off { arg ... args;
		^this[\off].(this, * args)
	}

	off_ { arg ... args;
		if(this[\off_].notNil) {
			this[\off_].(this, * args)
		} {
			this[\off] = args[0]
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

	asControlInput { arg ... args;
		^this[\asControlInput].(this, * args)
	}

	asControlInput_ { arg ... args;
		if(this[\asControlInput_].notNil) {
			this[\asControlInput_].(this, * args)
		} {
			this[\asControlInput] = args[0]
		}
	}

}


ProtoDef : ProtoClass {
	classvar <>instanceClasses = #[\ProtoDef, \TrackDef, \FileSystemProject];
	classvar <>templateClasses = #[\ProtoTemplateDef, \TrackTemplateDef, \FileSystemProjectTemplate];
	//var <>key;

	*all {
		^PresetDictionary.new(\ProtoDef);
	}

	*defaultTemplateDictionary { ^\ProtoTemplateDef }

	*new { arg key, val;
		var inst;
		if(val.isKindOf(Symbol)) {
			// Note: parent not existing is not a problem because it can be defined after
			val = this.defaultTemplateDictionary.asClass.new(val)
		};
		if(this.all[key].isNil) {
			if(val.notNil) {
				// if value is a template class
				//Log(\Param).debug("ProtoDef % %", this.class.name.asCompileString, val.class.name.asCompileString);
				if(templateClasses.includes(val.class.name) and: { instanceClasses.includes(this.name) } ) {
					//Log(\Param).debug("use parent!!! %", val);
					inst = super.new(()).protoDef_prAdd(key);
					inst[\parent] = val;
					inst.initProto; // constructor
					^inst
				} {
					^super.new(val).protoDef_prAdd(key)
				}
			} {
				^super.new(()).protoDef_prAdd(key)
			}
		} {
			var ret = this.all[key];
			if(val.notNil) {
				if(templateClasses.includes(val.class.name) and: { instanceClasses.includes(this.name) } ) {
					//Log(\Param).debug("replace parent!!! %", val);
					ret[\parent] = val;
				} {
					ret.putAll(val);
					ret[\key] = key;
					ret[\parent] = val[\parent];
				}
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

	clear { arg ...args;
		this[\clear].value(this, *args);
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
		// FIXME: not the same mecanism as ProtoClass
		if(this[\storeOn].notNil) {
			this[\storeOn].(this, stream);
		} {
			stream << "%(%)".format(this.class.asString, this.key.asCompileString);
		}
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
			if(protoClassDef_all[key].notNil) {
				// don't know if its a good idea to update event instead of replacing, now it look a lot like ProtoDef 
				// but now i can update the protoclass and all instances are updated
				protoClassDef_all[key].putAll(val);
				protoClassDef_all[key].parent = val.parent;
			} {
				if(val.isKindOf(ProtoClass).not) {
					val = ProtoClass(val);
				};
				protoClassDef_all[key] = val;
			};
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
