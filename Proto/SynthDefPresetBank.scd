
(
ProtoTemplateDef(\SynthDefPresetBank, (
	new: { arg self, target;
		self = ProtoClass((parent:self));

		//if(target.isKindOf(Pdef)) {
		self.target = target;
		//};
		if(Archive.global.at(\Param, \SynthDefPresetBank, self.instrument).notNil) {
			self.loadArchive;
		};

		self;
	},

	instrument: { arg self;
		var instr = PdefParam.instrument(self.target);
		var target = self.target;
		if(instr.isNil) {
			case(
				{ target.isKindOf(Pdef) }, {
					instr = "Pdef_%".format(target.key).asSymbol;
				},
				{ target.isKindOf(Ndef) }, {
					instr = "Ndef_%".format(target.key).asSymbol;
				}, {
					"Error: chan't find instrument name for %".format(target).error;
					\error
				}
			)
		};
		instr;
	},

	presetDict: IdentityDictionary.new,

	buildPreset: { arg self;
		var preset = IdentityDictionary.new;
		var data = IdentityDictionary.new;
		preset[\data] = data;
		self.target.asParamGroup.do { arg param, idx;
			if(param.isSet) {
				data[param.propertyRoot] = param.get;
			} {
				preset[\unset] = preset[\unset].add(param.propertyRoot)
			};
		};
		preset;
	},

	hasSavedPresets: { arg self;
		self.presetDict.size > 0
	},

	savePreset: { arg self, key;
		self.presetDict[key] = self.buildPreset;
		self.saveArchive;
		self.changed(\bank);
	},

	loadPreset: { arg self, key;
		var preset;
		preset = self.presetDict[key];
		if(preset.isNil) {
			"no preset named %".format(key).error;
		} {
			self.loadPresetFromObject(preset);
		}
	},

	loadPresetFromObject: { arg self, preset;
		var pg = self.target.asParamGroup.asPropertyDict;
		pg.keys.do { arg pkey, idx;
			if(preset[\data][pkey].notNil) {
				pg[pkey].set(preset[\data][pkey])
			} {
				pg[pkey].unset;
			};
		};
		//preset[\data].keys.do { arg pkey, idx;
			//if(pg[pkey].notNil) {
				//pg[pkey].set(preset[\data][pkey])
			//};
		//};
		//preset[\unset].do { arg pkey, idx;
			//if(pg[pkey].notNil) {
				//pg[pkey].unset;
			//};
		//};
	},

	saveArchive: { arg self;
		var archive = IdentityDictionary.new;
		archive[\presetDict] = self.presetDict;
		Archive.global.put(\Param, \SynthDefPresetBank, self.instrument, archive);
	},

	loadArchive: { arg self;
		var archive;
		var presets;
		//"load Archive".debug;
		archive = self.getArchive;
		self.presetDict = archive[\presetDict];
		self.presetDict.keysValuesDo { arg k,v;
			// this loop is to workaround a bug in Ndef/Archive which load the Ndef with a nil server
			v[\data] = v[\data].collect { arg val;
				//val.class.debug("val class");
				if(val.class == Ndef) {
					//"val is a Ndef".debug;
					val.server = Server.default;
					val = val.asCompileString.interpret;
				};
				val;
			};
			self.presetDict[k][\data] = v[\data];
		};
		this.changed(\bank);
		//"end load Archive".debug;
	},

	getArchive: { arg self;
		Archive.global.at(\Param, \SynthDefPresetBank, self.instrument);
	},


	clearBank: { arg self;
		Archive.global.put(\Param, \SynthDefPresetBank, self.instrument, nil);
		self.presetDict[self.instrument] = nil;
		self.changed(\bank);
	}

));

ProtoTemplateDef(\PresetGraphBank, (
	// presetScheme format:
	// (
	//	main: a function returning the main ParamGroup,
	//	items: an optional function returning a list of presetScheme
	// )
	// the constraint for paramgroup in a presetScheme is no duplicate property name
	// if we remove one key from the paramgroup, preset loading will use the target of the 
	//		first param of the group to set the preset value

	shouldUnsetParams: true,

	new: { arg self, presetScheme, loadPresetHook;
		self = ProtoClass((parent:self));

		self.presetScheme = presetScheme;
		self.loadPresetHook = loadPresetHook;
		if(Archive.global.at(\Param, \PresetGraphBank, presetScheme.name).notNil) {
			self.loadArchive;
		};

		self;
	},

	groupDict:{ arg self;
		self.presetScheme;
	},

	presetBank: { arg self;
		if(self[\presetBank_obj].isNil) {
			self[\presetBank_obj] = IdentityDictionary.new;
		};
		self[\presetBank_obj];
	},

	presetDict: { arg self;
		self.presetBank
	},
	////

	getCombinatorPresetEvent: { arg self, pg;
		var ev = ();
		pg.getCombinatorList.do { arg asso, idx;
			var combi = asso.value;
			ev[asso.key.propertyRoot] = (
				mapAllInputs: combi.inputObjects.collect { arg item, idx;
					if(item.notNil) {
						item.presetCompileString
					};
				},
				ranges: combi.rangeParam.get.copy,
			)
		};
		if(ev.keys.size == 0) {
			nil
		} {
			ev;
		};
	},

	loadCombinatorPresetEvent: { arg self, pg, preset;
		if(preset.notNil) {
			pg.do { arg param, idx;
				var key = param.propertyRoot;
				if(preset[key].isNil) {
					param.clearCombinator;
				} {
					ParamCombinator(param).mapAllInputs(preset[key][\mapAllInputs].collect { arg item, idx;
						item.interpret;
					}, preset[key][\ranges].copy);
				};
			};
		};
	},

	////

	getParamDict: { arg self, pg;
		var ev = ();
		[pg, pg.asPropertyDict].debug("PresetGraphBank.getParamDict");
		pg.asPropertyDict.keysValuesDo { arg key, val;
			if(val.isSet) {
				ev[key] = val.get.copy;
			};
		};
		ev;
	},

	getPresetTreeFromScheme: { arg self, pgEvent;
		var preset = ();
		var mpg = pgEvent[\main].value;
		preset[\main] = self.getParamDict(mpg);
		preset[\combinators] = self.getCombinatorPresetEvent(mpg);
		preset[\name] = pgEvent.name;
		preset[\items] = pgEvent[\items].value.collect { arg pg;
			self.getPresetTreeFromScheme(pg);
		};
		preset;
	},

	loadParamDict: { arg self, pg, preset;
		var pgd = pg.asPropertyDict;
		var keys = pgd.keys ++ preset.keys;
		keys.do { arg key, idx;
			if(pgd[key].isNil) {
				pgd[key] = Param(pgd[pgd.keys.first].target, key)
			};
			if(preset[key].notNil) {
				pgd[key].set(preset[key].copy);
			} {
				pgd[key].unset;
			};
		};
	},

	loadPresetTreeToScheme: { arg self, pgEvent, presetEvent;
		var mpg = pgEvent[\main].value;
		self.loadParamDict(mpg, presetEvent[\main]);
		self.loadCombinatorPresetEvent(mpg, presetEvent[\combinators]);
		presetEvent[\items].value.do { arg subpreset, idx;
			self.loadPresetTreeToScheme(pgEvent[\items].value[idx], subpreset);
		};
	},

	savePreset: { arg self, key;
		var preset = ();
		self.presetScheme.debug("savePreset: schema:");
		preset = self.getPresetTreeFromScheme(self.presetScheme);
		preset.debug("savePreset: data:");
		self.presetBank[key] = preset;
	},

	loadPreset: { arg self, key;
		var preset = self.presetBank[key];
		if(preset.isNil) {
			"No preset named %".format(key).error;
		} {
			self.presetScheme.debug("loadPreset: schema:");
			preset.debug("loadPreset: data:");
			self.loadPresetTreeToScheme(self.presetScheme, preset);
		};
		self.loadPresetHook(key);
	},

	loadPresetFromObject: { arg self, preset;
		self.loadPresetTreeToScheme(self.presetScheme, preset);
	},

	getCurrentPreset: { arg self;
		var preset;
		preset = self.getPresetTreeFromScheme(self.presetScheme);
		preset;
	},

	getTreeString: { arg self, tree, indent=0;
		var pg, items = "", itemstr = "";
		var indstr = "\t".dup(indent).join;
		var mainstr = "";
		var namestr = "";
		var combistr = "";
		tree = tree ?? { self.presetScheme };
		if(tree[\main].notNil) {
			var val = tree[\main].value;
			if(val.isKindOf(IdentityDictionary)) {
				pg = val.asCompileString;
			} {
				pg = "[\n%%\t]".format(val.collect { arg item, idx;
					"\t%%,\n".format("\t".dup(indent+1).join, item.asCompileString);
				}.join, indstr);
			};
			mainstr = "\t%main: %,\n".format(indstr, pg);
		};
		if(tree[\combinators].notNil) {
			var val = tree[\combinators].value;
			val = val.asCompileString;
			combistr = "\t%combinators: %,\n".format(indstr, val);
		};
		if(tree[\items].notNil) {
			var val = tree[\items].value;
			if(val.notNil) {
				items = val.collect { arg item, idx;
					"%,\n".format(self.getTreeString(item, indent+2))
				}.join;
				itemstr = "\t%items: [\n%\t%]\n".format(indstr, items, indstr);
			} {
				itemstr = "\t%items: {}, // ERROR\n".format(indstr, items, indstr);
			};
		};
		if(tree[\name].notNil) {
			namestr = "\t%name: %,\n".format(indstr, tree[\name].value.asCompileString);
		};

		"%(\n%%%%%)".format(indstr, namestr, mainstr, combistr, itemstr, indstr)
	},

	printTree: { arg self, val;
		self.getTreeString(val).postln;
		nil
	},

	printCurrentPreset: { arg self;
		self.printTree(self.getCurrentPreset)
	},

));
)
