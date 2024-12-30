
ControllerDef {
	classvar <>all;
	var <>key;
	var <>source;
	var <tags;
	var <>lastUse;

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
				^super.new.prAdd(key).init(val, tags)
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
		if(symlist.isSequenceableCollection.not) {
			symlist = [symlist]
		};
		tags = symlist;
		symlist.do { arg sym;
			var tt = this.class.getTagSpec(sym).addUnique( this.key -> this );
			TagSpecDef("ControllerDef_capabilities".asSymbol).addUnique(sym -> tt);
		};
	}

	*getByTag { arg symlist;
		var dict = IdentityDictionary.new;
		symlist.asSequenceableCollection.collect { arg sym;
			dict.putAll(this.getTagSpec(sym).associationList)
		};
		^dict;
	}

	*getTagSpec { arg sym;
		^TagSpecDef("ControllerDef_capabilities_%".format(sym).asSymbol);
	}

	init { arg val, intags;
		source = val;
		this.tags = intags;
	}

	startControl { arg ... args;
		var oldLastUse = lastUse;
		lastUse = args;
		//[oldLastUse, lastUse].debug("startControl: old new");
		oldLastUse.first.changed(\hasControl); // if lastUse is SeqPlayerGroup, this update hasControl views
		this.changed(\hasControl);
		source.value(this, *args)
	}

	hasControl { arg ... args;
		^lastUse == args;
	}

	// make printOn return the same than storeOn
	printOn { arg stream;
		this.storeOn(stream);
	}

	storeOn { arg stream;
		stream << "ControllerDef(";
		stream << key;
		stream << ")";
	} 

}
