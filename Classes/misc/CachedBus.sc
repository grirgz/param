
CachedBus : Bus {
	classvar <cache;

	*initClass {
		Class.initClassTree(IdentityDictionary);
		cache = IdentityDictionary.new;
		cache[\audio] = IdentityDictionary.new;
		cache[\control] = IdentityDictionary.new;
	}

	set { arg ... values;
		var val;
		if(values.size == 1) {
			val = values[0];
			super.set(val);
		} {
			val = values;
			super.setn(val);
		};
		cache[this.rate][this.index] = val;
	}

	setn { arg values;
		this.set(*values)
	}

	getCached {
		if(cache[this.rate][this.index].isNil) {
			this.get({ arg x; cache[this.rate][this.index] = x });
			^0
		} {
			^cache[this.rate][this.index]
		}
	}

}

