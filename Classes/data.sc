EnvInit {
	*new { arg key, val;
		if(currentEnvironment[key].isNil) {
			currentEnvironment[key] = val;
		};
		^currentEnvironment[key];
	}
}


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

EventClass : ClassMethodDictionary {}

//ProtoClass {
ProtoClass : Event {
	//var <>protoclass_event;
	// FIXME: does not work!
		// yes it work, forgot the * !

	*new { arg xevent;
		var inst;
		"bla".debug;
		xevent.debug("xevent");
		inst = super.new;
		//inst.protoclass_event = ();
		//inst.protoclass_event.putAll(xevent);
		inst.putAll(xevent);
		^inst;
	}

	storeOn { arg stream;
		//if(protoclass_event.)
		//^init_args
		stream << "Param.new(";
		super.storeOn(stream);
		stream << ")";
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

	embedInStream { arg ... args;
		^this[\embedInStream].(this, * args)
	}
}


ArchiveDictionary {
	classvar <>loaderStack;
	classvar <>lockedFiles;

	*initClass {
		loaderStack = List.new;
		lockedFiles = Set.new;
	}

	*fileExclusiveUse { arg path, mode, fun;
		// not really what I wanted but I keep it
		var res;
		path = File.realpath(path);
		if(lockedFiles.includes(path)) {
			"WARNING: already using this file: %, %".format(path, mode);
		} {
			lockedFiles.add(path);
			res = File.use(path, mode, fun);
			lockedFiles.remove(path);
			^res;
		};
	}

	*fileExclusiveDo { arg path, fun;
		var res;
		path = File.realpath(path);
		if(lockedFiles.includes(path)) {
			"WARNING: already using this file: %".format(path);
		} {
			lockedFiles.add(path);
			res = fun.();
			lockedFiles.remove(path);
			^res;
		};
	}

	*write { arg dict, file;
		if(dict.size > 0) {
			if(this.isValidDestination(file)) {
				file.debug("ArchiveDictionary: writing file");
				File.use(file, "w", { arg file;
					dict.keysValuesDo { arg k, v;
						var str;
						file << "\nArchiveDictionary.load( %,

							%

						);\n".format(k.asCompileString, v.asCompileString);
					};
				});
			};
		}
	}

	*loaderDo { arg str, loader;
		var res;
		loaderStack.add(loader);
		res = str.interpret;
		loaderStack.pop;
		^res;
	}

	*load { arg ...args;
		loaderStack.last.(*args);
	}

	*isValidDestination { arg file;
		if(File.exists(file).not) {
			^true
		} {
			var dict = IdentityDictionary.new;
			var str;
			try {
				File.use(file, "r", { arg file;
					str = file.readAllString;
				});
			} { arg error;
				^false
			};
			this.loaderDo(str, { arg key, val;
				dict[key] = val; // do not load, there is maybe side effects
			});
			if(dict.size > 0) {
				^true
			};
		};
		^false;
	}

	*read { arg file;
		if(File.exists(file).not) {
			// no archive yet
			^nil
		} {
			var dict = IdentityDictionary.new;
			var str;
			try {
				File.use(file, "r", { arg file;
					str = file.readAllString;
				})
			} { arg error;
				"ERROR: can't read archive!".postln;
				error.throw;
			};

			try {
				this.loaderDo(str, { arg key, val;
					dict[key] = val.load;
				});
			} { arg error;
				"ERROR: can't load archive!".postln;
				error.throw;
			};

			if(dict.size > 0) {
				^dict
			} {
				"ERROR: empty archive, there is maybe a problem: %".format(file).throw;
			}; 
		};
	}

}

PresetDictionary : IdentityDictionary {
	classvar allInitialized = false; // temporary
	classvar <>all;
	var <>key;
	classvar allIsLoading = false;
	var isLoading = false;
	var initialized = false;
	classvar <archiveFolder;

	//put { arg k, val;
	//	super.put(k, val);
	//	this.save;
	//}

	*initClass {
		Class.initClassTree(File);
		Class.initClassTree(Platform);
		Class.initClassTree(IdentityDictionary);
		super.initClass;
		all = IdentityDictionary.new;
		this.archiveFolder = Platform.userAppSupportDir +/+ "PresetDictionary";
	}

	*archiveFolder_ { arg path;
		if(File.exists(path)) {
			if(PathName(path).isFolder) {
				archiveFolder = path;
			} {
				"ERROR: PresetDictionary.archiveFolder is not a folder".throw;
			}
		} {
			File.mkdir(path);
		}
	}

	*new { arg name;
		if(name.isNil) {
			^super.new;
		} {
			if(all[name].isNil) {
				var inst = super.new.initPresetDictionary(name);
				all[name] = inst;
				^inst;
			} {
				^all[name];
			}
		}
	}

	initPresetDictionary { arg name;
		key = name;
		//this.load;
	}

	*loadIfNotInitialized { // temporary method to work with eventclass
		if(allInitialized.not) {
			allInitialized = true;
			this.loadAll;
		}
	}

	loadIfNotInitialized {
		if(initialized.not) {
			initialized = true;
			this.load;
		}
	}
	
	*loadAll {
		if(allIsLoading.not) {
			//var keys_order;
			allIsLoading = true;

			"/home/ggz/code/sc/seco/vlive/demo/param/lib/drumrack.scd".load;
			"================================== loadAll".debug;
			Archive.global.at(\PresetDictionary).keys.debug("loadAll: archive keys");
			//keys_order = Archive.global.at(\PresetDictionary).keys.asArray;
			this.all.keys.debug("loadAll: all keys first");
			Archive.global.at(\PresetDictionary).keys.do { arg pkey;
				var inst;
				pkey.debug("-------------- loadAll");
				//[pkey, this.all.keys].debug("loadAll: all keys2");
				inst = this.new(pkey);
				//[pkey, this.all.keys].debug("loadAll: all keys3");
				[pkey ,Archive.global.at(\PresetDictionary, pkey)].debug("Loading library ==");
				if( Archive.global.at(\PresetDictionary, pkey).notNil ) {
					//[pkey, this.all.keys].debug("loadAll: all keys4");
					Archive.global.at(\PresetDictionary, pkey).keys.do { arg subkey, n;
						var data;
						[n, pkey, subkey, this.all.keys].debug("3x loadAll: in your PresetDictionary, loading its content");
						data = Archive.global.at(\PresetDictionary, pkey)[subkey];
						inst[subkey] = data.load;
					};
					[pkey, this.all.keys].debug("loadAll: all keys5");
				}
			};
			this.all.keys.debug("loadAll: all end");
		} {
			"================================== loadAll: already loading, skipping".debug;
		};
		allIsLoading = false;
	}

	save_preset { arg name, preset;
		this[name] = preset;
		this.save;
	}

	save {
		// need a isSaving in case of asArchiveData saving something ? (which would be stupid)
		var data = this.collect({ arg x; x.asArchiveData });
		[key, data.asCompileString].debug( "Saving library ===========================================" );
		ArchiveDictionary.write(data, this.class.archiveFolder +/+ key);
		^data
	}

	load {
		if(isLoading.not) {
			var dict;
			isLoading = true;
			dict = ArchiveDictionary.read(this.class.archiveFolder +/+ key);
			[key , dict].debug("Loading library ======================");
			if(dict.notNil) {
				dict.keysValuesDo { arg k, v;
					this[k] = v;
				}
			};
			isLoading = false;
		} {
			[key].debug("PresetDictionary: already loading");
		}
	}



	lib { 
		// this is for back compat with event prototype
		^this
	}

	get_list { 
		^this.keys.asArray.sort;
	}

}

XSamplePlaceholder {
	// used to store the path and numchannel when storing to disk
	// TODO: look at crucial Sample

	var <>path, <>numChannels;

	*new { arg path, numChannels=2;
		^super.newCopyArgs(path, numChannels)
	}

	//init { arg xpath, xnumChannels; 
	//	path = xpath;
	//	numChannels = xnumChannels;
	//}

	storeArgs { arg stream; 
		^[path,numChannels]     
	}


	printOn { arg stream;
		this.storeOn(stream); // storeOn call storeArgs
	}

	//storeOn { arg stream; 
	//	stream << "MyTestPoint.new(" << path << ", " << numChannels << ")";    
	//}

	load {
		if(path.notNil) {
			^BufferPool.get_stereo_sample(\param, path); // TODO: use numChannels
		} {
			debug("XSamplePlaceholder: error, path is nil");
			^\error_path_is_nil
		}
	}
}

SampleProxy  {
	var <>samplePath, <>sampleNumChannels;
	*new { arg path, numChannels;
		^super.new.initSampleProxy(path, numChannels);
	}

	initSampleProxy { arg xpath, xnumChannels;
		samplePath = xpath;
		sampleNumChannels = xnumChannels;
	}

	storeArgs { arg stream; 
		^[samplePath,sampleNumChannels]     
	}

	printOn { arg stream;
		this.storeOn(stream); // storeOn call storeArgs
	}

	source {
		// FIXME: semi harcoded numChannels
		if(sampleNumChannels == 1) {
			^BufferPool.get_mono_sample(\SampleProxy, samplePath);
		} {
			^BufferPool.get_stereo_sample(\SampleProxy, samplePath);
		}
	}

	numChannels {
		^this.source.numChannels ?? { sampleNumChannels }
	}

	isFreed {
		^this.source.numChannels.isNil;
	}

	path {
		^this.source.path ?? { samplePath }
	}

	bufnum {
		^this.source.bufnum;
	}

	asControlInput {
		^this.bufnum
	}

}


////////////////////////////////


SpecGroup : List {

	asParamGroup { arg target;
		var group = this.collect({ arg param_spec;
			if(param_spec.isSequenceableCollection.not) {
				param_spec = [param_spec]
			};
			if(param_spec.size > 1) {
				Param(target, param_spec[0], param_spec[1]);
			} {
				Param(target, param_spec[0]);
			}
		});
		^ParamGroup(group);
	}
	
}



///////////////////////////// Builder 

Builder {
	var <source;
	var >envir;
	var <>key;
	classvar <all;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { arg key, fun;
		if(all[key].isNil) {
			all[key] = this.make(fun).key_(key);
			^all[key];
		} {
			var ins;
			ins = all[key];
			if(fun.notNil and: { ins.notNil }) {
				ins.source = fun;
			};
			^ins;
		}
	}

	*make { arg fun;
		var ins = super.new;
		ins.source = fun;
		ins.envir[\builder] = ins;
		^ins;
	}

	source_ { arg fun;
		if( fun.isNil ) {
			fun = {}
		};
		source = fun;
		this.class.functionArgsToEnvir(fun).keysValuesDo { arg k, v;
			if( this.envir[k].isNil ) {
				this.envir[k] = v;
			};
		};
	}
	 
	envir {
		if(envir.isNil) { 
			envir = IdentityDictionary.new;
			envir[\builder] = this;
		};
		^envir
	}

	*functionArgsToEnvir { arg fun;
		var env = ();
		fun.def.argNames.do { arg name, x;
			env[name] = fun.def.prototypeFrame[x]
		};
		^env;
	}

	build {
		^source.valueWithEnvir(this.envir);
	}

	set { arg ...args;
		var hasChanged = false;
		args.pairsDo { arg key, val; 
			if(this.envir.at(key) != val) {
				this.envir.put(key, val);
				hasChanged = true;
			}
		};
		if(hasChanged) {
			this.build;
			this.changed(\set, args);
		}
	}

	unset { arg ... args;
		args.do { arg key; this.envir.removeAt(key) };
		this.changed(\unset, args);
	}

	get { arg key;
		^this.envir[key];
	}
	
}


///////////////////////////// PlayerWrapper


PlayerWrapper  {
	var <>wrapper;
	var >label;

	*new { arg target;
		^super.new.initWrapper(target);
	}

	initWrapper { arg target;
		// FIXME: handle when not a kind of wrapper in list, and handle GUI when wrapper is nil
		wrapper = case 
			{ target.isNil } {
				"WARNING: PlayerWrapper: target is nil".debug;
				^nil;
			}
			{ target.isKindOf(Event) } {
				PlayerWrapper_Event(target)
			}
			{ target.isKindOf(NodeProxy) } {
				PlayerWrapper_NodeProxy(target)
			}
			{ target.isKindOf(EventPatternProxy) } {
				PlayerWrapper_EventPatternProxy(target)
			}
			{ target.isKindOf(Param) } {
				PlayerWrapper_Param.new(target)
			}
		;
		
	}

	target {
		if(wrapper.notNil) {
			^wrapper.target
		} {
			^nil
		};
	}

	///////// API
	// play
	// stop
	// isPlaying
	// label and key
	// quant

	quant {
		^if(this.target.respondsTo(\quant)) {
			this.target.quant;
		}
	}

	quant_ { arg val;
		if(this.target.respondsTo(\quant)) {
			this.target.quant = val;
		}
	}

	// *could be added
	// pause
	// record

    doesNotUnderstand { arg selector...args;
		[selector, args].debug("PlayerWrapper: doesNotUnderstand");
        if(wrapper.class.findRespondingMethodFor(selector).notNil) {
			"PlayerWrapper: perform".debug;
			^wrapper.perform(selector, * args);
		} {
			if(wrapper.target.class.findRespondingMethodFor(selector).notNil) {
				"PlayerWrapper: sub perform".debug;
				^wrapper.target.perform(selector, * args);
			} {
				"PlayerWrapper: doesnot".debug;
				DoesNotUnderstandError.throw;
			}
		};
	}

	/////////////// overRide object methods

	isPlaying {
		^wrapper.isPlaying
	}

	stop {
		wrapper.stop;
	}

	play {
		wrapper.play;
	}

	label {
		if(label.notNil) {
			^label;
		} {
			^wrapper.label;
		}
	}

	///////////// gui

	edit {
		^WindowLayout({ PlayerWrapperView.new(this).layout });
	}

	asView {
		^PlayerWrapperView.new(this).layout;
	}
}

PlayerWrapper_Base {
	var <>target;
	*new { arg target;
		^super.new.init(target)
	}

	init { arg xtarget;
		target = xtarget
	}

	key { arg self;
		^this.label.asSymbol;
	}


    doesNotUnderstand { arg selector...args;
		[selector, args].debug("PlayerWrapper_Base: doesNotUnderstand");
		if(target.class.findRespondingMethodFor(selector).notNil) {
			"perform".debug;
			^target.perform(selector, * args);
		} {
			"PlayerWrapper_Base: doesnot".debug;
			DoesNotUnderstandError.throw;
		}
	}

	/////////////// overRide object methods

	isPlaying {
		^target.isPlaying;
	}

	stop {
		target.stop;
	}

	play {
		target.play;
	}

	togglePlay {
		if(this.isPlaying) {
			this.stop;
		} {
			this.play;
		}
	}

}

PlayerWrapper_Param : PlayerWrapper_Base {
	play {
		target.normSet(1)
	}

	label {
		var res;
		res = target.property;
		if(res.isKindOf(Function)) {
			^""
		} {
			^res
		}
	}

	stop {
		target.normSet(0)
	}

	isPlaying {
		^target.normGet == 1
	}
}

PlayerWrapper_NodeProxy : PlayerWrapper_Base {
	label {
		if(target.isKindOf(Ndef)) {
			^target.key
		} {
			^""
		}
	}

	play {
		// hack: Ndef now have same latency than Pdef
		//{ // defer implemented in dereference_event
			target.play;
		//}.defer(Server.default.latency)
	}

	stop {
		// hack: Ndef now have same latency than Pdef
		//{ // defer implemented in dereference_event
			if(target.getHalo(\stopIsMute) == true) {
				target.stop;
			} {
				target.free;
			}
		//}.defer(Server.default.latency)
	}

	isPlaying {
		^target.monitor.isPlaying;
	}

}

PlayerWrapper_EventPatternProxy : PlayerWrapper_Base {
	label {
		if(target.isKindOf(Pdef)) {
			^target.key
		} {
			^target.getHalo(\label) ? ""
		}
	}

}

EventPlayerWrapper : PlayerWrapper_Event { } // compat, to be deleted

PlayerWrapper_Event : PlayerWrapper_Base {
	// allow an event to act as a PlayerWrapper

	play {
		target.eventPlay;
	}

	label {
		^target.label ?? "-"
	}

	isPlaying {
		^target.eventIsPlaying
	}

	stop {
		target.eventStop;
	}

}

///////////////////////// dont know where to put that


+ List {
	sortLike { arg model;
		var ar = Array.new(this.size);
		model.do({ arg key;
			if(array.includes(key)) {
				ar = ar.add( array.remove(key) );
			}
		});
		//array.do { arg val;
		//	if(val.notNil) {
		//		ar = ar.add(val)
		//	}
		//}
		ar = ar ++ array;
		array = ar
	}
}


////////////////////////////////


+SynthDesc {
	*new { arg name;
		if(name.isNil) {
			^super.new
		} {
			^SynthDescLib.global.at(name)
		}
	}

	params { 
		^this.controls.collect { arg control;
			var ret;
			var spec;
			if(control.name == '?') {
				ret = nil;
			} {
				spec = this.getSpec(control.name.asSymbol);
				if(spec.isNil) {
					ret = control.name.asSymbol;
				} {
					ret = [control.name.asSymbol, spec];
				};
			};
			ret
		}.select(_.notNil);
	}

	asParamGroup { arg target;
		var sgroup = SpecGroup.newFrom(this.params);
		^sgroup.asParamGroup(target)
	}

	specs {
		var val;
		val = this.metadata;
		if(val.notNil) {
			val = val.specs;
			if(val.notNil) {
				^val.composeEvent(this.getHalo(\specs))
			} {
				^this.getHalo(\specs)
			}
		} {
			^this.getHalo(\specs)
		}
	}

	defaultValue { arg argname;
		var val;
		var con = this.controlDict[argname];
		if(con.notNil) {
			val = con.defaultValue;
		}
		^val;
	}

	getSpec { arg name;
		var val;
		var rval;
		if(super.getSpec(name).notNil) {
			rval = super.getSpec(name)
		} {
			val = this.metadata;
			if(val.notNil) {
				val = val.specs;
				if(val.notNil) {
					rval = val[name];
				}
			};

		};
		^rval;
	}
	
}
