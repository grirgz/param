
////////////////////////////////////////

ParamGroup : List {
	var <presets;
	var <>morphers;
	classvar <>editFunction;

	// morphers format : Dict[ \name -> (val: 0.5, presets:[\preset1, \preset2]) ]
	// - morphTo(\name, 0.3)
	// - addMorphing(\name)
	// - morphTo([\preset1, \preset2], 0.4)

	*new { arg anArray;
		var inst;
		Class.initClassTree(ParamGroupLayout); // FIXME: should be in *initClass, no ?
		inst = super.new.setCollection( anArray.collect(_.asParam) );
		inst.initParamGroup;
		^inst;
	}

	initParamGroup {
		presets = IdentityDictionary.new;
	}

	save { arg key=\default; 
		presets[key] = super.array.collect { arg param;
			var val = param.get;
			if(val.isKindOf(Buffer)) {
				val; // should not copy buffer else WavetableDef and BufDef.asCompileString does not work
			} {
				val.copy;// need to copy to avoid reference sharing with array
			};
		};
		this.changed(\presets);
	}

	presets_ { arg val;
		presets = val.deepCopy;
		this.changed(\presets);
	}

	getPreset { arg key=\default;
		^presets[key]
	}

	select { arg fun;
		// FIXME: don't know why it loose the class
		^ParamGroup(super.select(fun))
	}

	reject { arg fun;
		^ParamGroup(super.reject(fun))
	}

    rejectUnsetParams {
		this.reject({ arg p; p.isSet.not })
    }

	rejectByKey { arg keylist;
		if(keylist.isSequenceableCollection.not) {
			keylist = [keylist]
		};
		^this.reject({ arg x; keylist.includes(x.propertyRoot) })
	}

	selectByKey { arg keylist;
		if(keylist.isSequenceableCollection.not) {
			keylist = [keylist]
		};
		^this.select({ arg x; keylist.includes(x.propertyRoot) })
	}

	rejectSystemParams {
		^this.rejectByKey([ \gate, \doneAction, \trig ])
	}

	rejectPbindParams {
		^this.rejectByKey([ \legato, \dur, \stretch, \instrument ])
	}

	selectCustomParams {
		^this.rejectByKey([ \out, \gate, \doneAction, \amp, \out, \freq, \trig ])
	}

	selectByType { arg type;
		^this.select({ arg p; p.type == type });
	}

	selectSynthDefParams {
		^this.select({ arg x; x.isSynthDefParameter })
	}

	valueList {
		^this.collect { arg param;
			param.get;
		}
	}

	erase { arg key=\default;
		presets[key] = nil;
		this.changed(\presets);
	}

	load { arg key=\default; 
		if(presets[key].notNil) {
			presets[key].do { arg val, x;
				super.array[x].set(val)
			}
		}
	}

	edit {
		var fun = editFunction ? { arg pg;
			ParamGroupLayout.new(pg);
		};
		fun.(this);
	}

	editorView { 
		// FIXME: should decide terminology..
		^ParamGroupLayout.two_panes(this);
	}

	asView {
		^this.editorView
	}

	asParam {
		^this; // to act like a Param with subparams
	}

	getPresetCompileString { arg key=\default;
		^this.presetCompileString
	}

	getPbindCompileString {
		// TODO: write asPresetCompileStringNdef and Pdef and each other Param type
		^"\nPbind(\n\t%\n)\n".format(
			this.collect({ arg p; 
				"%, %,".format(p.property.asCompileString, p.get.asCompileString)
			}).join("\n\t")
		)
	}

	getBufferCompileString { arg param;
		// if val is a buffer, try to build BufDef compile string, else return val.asCompileString
		var val = param.get;
		case(
			{ param.spec.isKindOf(ParamBufferSpec) }, {
				var path = TagSpecDef(\BufDef).unmapKey(val);
				if(path.notNil) {
					val = "BufDef(%)".format(path.asCompileString)
				} {
					val.asCompileString;
				}
				
			},
			{ val.isKindOf(Buffer) }, {
				if(val.path.notNil) {
					val = "BufDef(%)".format(val.path.asCompileString)
				} {
					val.asCompileString;
				}
			}, {
				val = val.asCompileString;
			}
		);
		^val
	}

	getSetCompileString { arg targetCompileString;
		// return only params that are set in the envir
		^"%\n".format(
			this.collect({ arg p; 
				if(p.target.class == Message) {
					"%.% = %;".format(targetCompileString ?? { p.target.receiver.asCompileString }, p.property, p.getRaw.asCompileString)
				} {
					if(p.isSet) {
						"%.setVal(%, %);".format(targetCompileString ?? { p.target.asCompileString }, p.property.asCompileString, this.getBufferCompileString(p))
					} {
						nil
					};
				};
			}).reject(_.isNil).join("\n")
		)
	}

	getParamCompileString { arg targetCompileString;
		^"%\n".format(
			this.collect({ arg p; 
				//if(p.target.class == Message) {
					//"%.% = %;".format(targetCompileString ?? { p.target.receiver.asCompileString }, p.property, p.get.asCompileString)
				//} {
					"%.set(%);".format(targetCompileString ?? { p.asCompileString }, p.get.asCompileString)
				//};
			}).join("\n")
		)
		
	}

    getEventCompileString { arg onlySet=true;
		var ev = ();
		this.do { arg p, idx;
			if(p.isSet != false) {
				ev[p.property] = p.get
			} 
		};
		^"%\n".format(ev.asCompileString)
    }

	presetCompileString {
		var ret;
		var params, presets;
		var dictAsCompileString = { arg dict; // not used
			"IdentityDictionary[ % ]".format(dict.keys.collect { arg key, idx;
				"(% -> %)".format(key.asCompileString, ~d[key].asCompileString)
			}.join(", "));
		};
		var arrayAsCompileString = { arg array; // should call .asCompileString for WavetableDef and BufDef
			"[ % ]".format(array.collect(_.asCompileString).join(", "))
		};
		this.save(\current);
		params = this.collect({ arg param;
			"\t" ++ param.asCompileString
		}).join(",\n");
		presets = this.presets.keys.as(Array).collect({ arg pkey;
			"\t% -> %".format(pkey.asCompileString, arrayAsCompileString.(this.presets[pkey]))
		}).join(",\n");
		ret = "ParamGroup([\n%\n]).presets_(IdentityDictionary[\n%\n]);\n".format(params, presets);
		^ret;
	}
}

ParamGroupDef {
	// FIXME: this should be a subclass or superclass of ParamGroup because changed signals don't propagate
	classvar <lib;
	var <key;
	var <group;

	*initClass {
		Class.initClassTree(IdentityDictionary);
		lib = IdentityDictionary.new
	}

	*new { arg defkey, group;
		var inst;
		if(group.isNil) {
			^lib[defkey]
		} {
			if(lib[defkey].isNil) {
				inst = super.new.init(defkey, group);
				lib[defkey] = inst;
				^inst
			} {
				Log(\Param).warning("Warning: ParamGroupDef(%) already defined, use .clear before redefine it".format(defkey));
				^lib[defkey]
			}
		}
	}

	*force { arg ...args;
		^this.update(*args)
	}

	*update { arg defkey, group;
		if(group.notNil and: { lib[defkey].notNil }) {
			var inst = lib[defkey];
			var news = List.new;
			var olds = Set.new;
			var losts = List.new;
			var matching = IdentityDictionary.new;

			// find new params:
			group.collect({ arg param, idx;
				var found = false;
				block { arg break;
					inst.group.do { arg oldparam, oldidx;
						if(param == oldparam ) { 
							found = true;
							matching[idx] = oldidx;
							//olds.add(oldidx);
							break.value;
						}
					};
				};
				//if(found.not) {
				//	news.add(param);
				//};
			});
			//inst.group.do({ arg oldparam, x; 
			//	if(olds.includes(x).not) {
			//		losts.add(oldparam);
			//	}
			//});

			/////

			inst.presets.keys.do { arg key;
				inst.presets[key] = group.collect({ arg param, idx;
					var oldidx = matching[idx];
					if(oldidx.isNil) {
						param.get;
					} {
						inst.presets[key][oldidx]
					}
				})
			};
			inst.group.array = group.asArray;
			^inst;
		} {
			if(group.notNil) {
				^this.new(defkey, group)
			} {
				^nil
			}
		}
	}

	init { arg defkey, xgroup;
		//xgroup.debug("hhhhhhhhhh");
		key = defkey;
		group = ParamGroup(xgroup);
		if(Archive.global.at(\ParamGroupDef, key).isNil) {
			this.saveArchive;
		} {
			this.loadArchive;
		};
	}

	prGroup_ { arg val;
		// private, don't use
		group = val;
	}

	presets {
		^group.presets
	}

	presets_ { arg val;
		group.presets = val.deepCopy;
		this.saveArchive;
	}

	getPreset { arg name=\default;
		^group.getPreset(name);
	}

	valueList {
		^group.valueList;
	}

	saveArchive {
		var archive = IdentityDictionary.new;
		archive[\presets] = group.presets;
		archive[\morphers] = group.morphers;
		Archive.global.put(\ParamGroupDef, key, archive);
	}

	loadArchive {
		var archive;
		var presets;
		//"load Archive".debug;
		archive = this.getArchive;
		presets = archive[\presets];
		presets.keysValuesDo { arg k,v;
			// this loop is to workaround a bug in Ndef/Archive which load the Ndef with a nil server
			v = v.collect { arg val;
				//val.class.debug("val class");
				if(val.class == Ndef) {
					//"val is a Ndef".debug;
					val.server = Server.default;
					val = val.asCompileString.interpret;
				};
				val;
			};
			presets[k] = v;
		};
		group.presets = presets;
		group.morphers = archive[\presets];
		this.changed(\presets);
		//"end load Archive".debug;
	}

	getArchive {
		^Archive.global.at(\ParamGroupDef, key);
	}


	clear {
		Archive.global.put(\ParamGroupDef, key, nil);
		lib[key] = nil;
	}

	save { arg name=\default;
		group.save(name);
		this.saveArchive;
		this.changed(\presets);
	}

	load { arg name;
		group.presets[name] = this.getArchive[\presets][name];
		group.load(name);
	}

	erase { arg name;
		group.erase(name);
		this.saveArchive;
		this.changed(\presets);
	}

	do { arg fun;
		group.do(fun)
	}

	collect { arg fun;
		^group.collect(fun)
	}

	select { arg fun;
		^group.select(fun)
	}

	at { arg x;
		^group[x]
	}

	size {
		^group.size;
	}

	edit {
		group.edit;
	}

	asView {
		^group.asView;
	}

	asList {
		^group
	}

	asParam {
		^this; // to act like a Param with subparams
	}

	presetCompileString {
		var ret;
		var params, presets;
		this.save(\current);
		params = this.collect({ arg param;
			"\t" ++ param.asCompileString
		}).join(",\n");
		presets = this.presets.keys.as(Array).collect({ arg pkey;
			"\t% -> %".format(pkey.asCompileString, this.presets[pkey].asCompileString)
		}).join(",\n");
		ret = "ParamGroupDef(%, [\n%\n]).presets_(IdentityDictionary[\n%\n]);\n".format(this.key, params, presets);
		^ret;
	}
}


///////////////////////



