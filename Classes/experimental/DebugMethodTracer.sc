
DebugMethodTracer : ProtoClass {
	classvar <>all;
	*initClass {
		all = IdentityDictionary.new;
	}

	asControlInput { arg ... args;
		^this.doesNotUnderstand(\asControlInput, *args)
	}

    *doesNotUnderstand { arg selector...args;
		[selector, args].debug("DebugMethodTracer: class selector and args");
		if(selector.asString.endsWith("_")) {
			all[selector.asGetter] = args[0];
			^all[selector.asGetter];
		} {
			^all[selector]
		}
	}
    doesNotUnderstand { arg selector...args;
		[selector, args].debug("DebugMethodTracer: selector and args");
		if(selector.asString.endsWith("_")) {
			all[selector.asGetter] = args[0];
			^all[selector.asGetter];
		} {
			^all[selector]
		}
	}
}
