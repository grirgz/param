
TrackDef {
	classvar <>all;
	var <>key;
	//var <>source;
	var <>wrapper;

	*initClass {
		all = PresetDictionary(\TrackDef)
	}

	*new { arg key, val;
		if(all[key].isNil) {
			if(val.notNil) {
				^super.new.init(val).prAdd(key)
			} {
				^nil
			}
		} {
			var ret = all[key];
			if(val.notNil) {
				//ret.source = val
			};
			^ret;
		}
	}

	*newInstance { arg val;
		^super.new.init(val).prAdd(\instance)
	}

	prAdd { arg xkey;
		key = xkey;
		all[key] = this;
	}

	init { arg val;
		wrapper = val;
	}

	at { arg x;
		x.debug("at");
		^wrapper.atClip(x)
	}

	put { arg x, val;
		wrapper.putClip(x, val);
	}

	clear {
		this.destructor;
		all[this.key] = nil;
		^nil;
	}

	collect { arg fun;
		^this.collectClip(fun)
	}

	do { arg fun;
		^this.doClip(fun)
	}

	source { arg ... args;
		^this.doesNotUnderstand(\source, * args)
	}

	isPlaying { arg ... args;
		^this.doesNotUnderstand(\isPlaying, * args)
	}

	play { arg ... args;
		this.doesNotUnderstand(\play, * args)
	}

	stop { arg ... args;
		this.doesNotUnderstand(\stop, * args)
	}

    doesNotUnderstand { arg selector...args;
		if(wrapper.isKindOf(ProtoClass) and: {
				wrapper[selector].notNil
			}
			or: {
				wrapper.class.findRespondingMethodFor(selector).notNil
			}
		) {
			"% perform: %, %".format(this.class, selector, args).debug;
			^wrapper.perform(selector, * args);
		} {
			"soft doesNotUnderstand".debug;
			DoesNotUnderstandError.new(this, selector, args).throw
		};
	}
}

TrackGroupDef : TrackDef {

	*initClass {
		all = PresetDictionary(\TrackGroupDef)
	}
	
	init { arg src;
		if(src.isKindOf(SequenceableCollection)) {
			wrapper = ~trackGroupType_PlayerWrapper.new(src);
		} {
			wrapper = src
		}
	}
}
