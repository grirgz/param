
TrackDef : ProtoClass {
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


}



//TrackDef {
//	classvar <>all;
//	var <>key;
//	//var <>source;
//	var <>wrapper;
//
//	*initClass {
//		all = PresetDictionary(\TrackDef)
//	}
//
//	*new { arg key, val;
//		if(all[key].isNil) {
//			if(val.notNil) {
//				^super.new.init(val).prAdd(key)
//			} {
//				^nil
//			}
//		} {
//			var ret = all[key];
//			if(val.notNil) {
//				//ret.source = val
//			};
//			^ret;
//		}
//	}
//
//	*newInstance { arg val;
//		^super.new.init(val).prAdd(\instance)
//	}
//
//	prAdd { arg xkey;
//		key = xkey;
//		all[key] = this;
//	}
//
//	init { arg val;
//		wrapper = val;
//		wrapper.me = { this };
//	}
//
//	at { arg x;
//		x.debug("at");
//		^wrapper.atChild(x)
//	}
//
//	put { arg x, val;
//		wrapper.putChild(x, val);
//	}
//
//	clear {
//		this.destructor;
//		all[this.key] = nil;
//		^nil;
//	}
//
//	collect { arg fun;
//		^this.collectChildren(fun)
//	}
//
//	do { arg fun;
//		^this.doChildren(fun)
//	}
//
//	source { arg ... args;
//		^this.doesNotUnderstand(\source, * args)
//	}
//
//	isPlaying { arg ... args;
//		^this.doesNotUnderstand(\isPlaying, * args)
//	}
//
//	play { arg ... args;
//		this.doesNotUnderstand(\play, * args)
//	}
//
//	stop { arg ... args;
//		this.doesNotUnderstand(\stop, * args)
//	}
//
//    doesNotUnderstand { arg selector...args;
//		if(wrapper.isKindOf(ProtoClass) and: {
//				wrapper[selector].notNil
//			}
//			or: {
//				wrapper.class.findRespondingMethodFor(selector).notNil
//			}
//		) {
//			//"% perform: %, %".format(this.class, selector, args).debug;
//			^wrapper.perform(selector, * args);
//		} {
//			"% perform: %, %".format(this.class, selector, args).debug;
//			"soft doesNotUnderstand".debug;
//			DoesNotUnderstandError.new(this, selector, args).throw
//		};
//	}
//}

//TrackGroupDef : TrackDef {
//
//	*initClass {
//		all = PresetDictionary(\TrackGroupDef)
//	}
//	
//	init { arg src;
//		if(src.isKindOf(SequenceableCollection)) {
//			wrapper = ~trackGroupType_PlayerWrapper.new(src);
//		} {
//			wrapper = src
//		};
//		wrapper.me = { this };
//	}
//}
