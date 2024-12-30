
(

ProtoTemplateDef(\CombinatorSlot, (
	eventType: \CombinatorSlot,
	label: "",
	play: { arg self, ev; },
	stop: {},
	isPlaying: false,

));

ProtoTemplateDef(\PatternCombinatorSlot, (
	parent: ProtoTemplateDef(\CombinatorSlot),

	new: { arg self, combinator, idx, patKey, spec;
		
		self.combinator = combinator;
		self.patKey = patKey;
		self.spec = spec;
		self.index = idx;
	},

	label: { arg self; "Mod: %".format(self.patKey) },

	play: { arg self, ev;
		var normval;
		var patkey = self.patKey;
		var combi = self.combinator;
		var spec = self.spec;
		var index = self.index;
		var val = ev.use { currentEnvironment[patkey].value };
		//[combi, index, ev].debug("run combi pattern");
		if(spec.isNil) {
			spec = patkey.asSpec;
			if(spec.isNil) {
				spec = \unipolar.asSpec;
			};
		};
		if(val.isNil) {
			val = spec.default ?? { 1 };
		};
		normval = spec.unmap(val);
		//[val, normval, spec].debug("val normval spec");
		this.inputParam.at(index).set(normval);
		this.computeAndSetTargetValue;
	}
));

ProtoTemplateDef(\NodeProxyCombinatorSlot, (
	parent: ProtoTemplateDef(\CombinatorSlot),

	new: { arg self, combinator, idx, proxy, spec;
		
		self.combinator = combinator;
		self.proxy = proxy;
		if(proxy.isKindOf(Ndef)) {
			self.label = proxy.key;
		} {
			self.label = proxy.identityHash.asString;
		};
		self.spec = spec;
		self.index = idx;
	},

	play: { arg self, ev;
		self.proxy.wakeUp;
	}

	stop: { arg self;
		self.proxy.stop;
	},

	isPlaying: { arg self;
		PlayerWrapper(self.proxy).isPlaying;
	},

));

ProtoTemplateDef(\ProtoClassCombinatorSlot, (
	parent: ProtoTemplateDef(\CombinatorSlot),

	new: { arg self, combinator, idx, target, spec;
		
		self.combinator = combinator;
		self.proxy = target;
		self.spec = spec;
		self.index = idx;
	},

	label: { arg self;
		self.proxy.label;
	},

	play: { arg self, ev;
		self.proxy.play;
	}

	stop: { arg self;
		self.proxy.stop;
	},

	isPlaying: { arg self;
		PlayerWrapper(self.proxy).isPlaying;
	},

));
)
