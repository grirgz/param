
ControllerDef {
	classvar <>all;
	var <>key;
	var <>source;
	var <tags;

	*initClass {
		Class.initClassTree(PresetDictionary);
		all = PresetDictionary(\ControllerDef)
	}

	*new { arg key, val, tags;
		if(val.isKindOf(ControllerDef)) {
			val = val.source;
		};
		if(key.isNil or: {all[key].isNil}) {
			if(val.notNil) {
				^super.new.init(val, tags).prAdd(key)
			} {
				^nil
			}
		} {
			var ret = all[key];
			if(val.notNil) {
				ret.source = val
			};
			if(tags.notNil) {
				ret.tags = tags
			};
			^ret;
		}
	}

	clear {
		all[key] = nil;
		^key;
	}

	prAdd { arg xkey;
		key = xkey;
		if(xkey.notNil) {
			all[key] = this;
		}
	}

	tags_ { arg symlist;
		// FIXME: tags are not replaced, they are added forever
		tags = symlist;
		symlist.do { arg sym;
			var tt = TagSpecDef("ControllerDef_capabilities_%".format(sym).asSymbol).addUnique( this.key -> this );
			TagSpecDef("ControllerDef_capabilities".asSymbol).addUnique(sym -> tt);
		};
	}

	*getByTag { arg symlist;
		var dict = IdentityDictionary.new;
		symlist.collect { arg sym;
			dict.putAll(TagSpecDef("ControllerDef_capabilities_%".format(sym).asSymbol).associationList)
		};
		TagSpecDef(\bla).asDict
		^dict;
	}

	init { arg val, tags;
		source = val;
		tags = tags;
	}

	startControl { arg ... args;
		source.value(this, *args)
	}

}
