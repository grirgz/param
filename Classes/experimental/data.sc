
ArchiveDictionary {
	// this class take a dictionary and a file name and write it on disk
	// FIXME: bad name, not a dictionary, but can archive Dictionaries. Maybe should be DictionaryArchiver
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
	
	// FIXME: seems to be deprecated in favor of .load
	// don't use Archive anymore as it's too easy to corrupt, and when corrupt, everything get erased
	// now store as Compile Strings on text files
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
				//[pkey ,Archive.global.at(\PresetDictionary, pkey)].debug("Loading library ==");
				[pkey].debug("Loading library ==");
				if( Archive.global.at(\PresetDictionary, pkey).notNil ) {
					//[pkey, this.all.keys].debug("loadAll: all keys4");
					Archive.global.at(\PresetDictionary, pkey).keys.do { arg subkey, n;
						var data;
						//[n, pkey, subkey, this.all.keys].debug("3x loadAll: in your PresetDictionary, loading its content");
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
			//[key , dict].debug("Loading library ======================");
			[key].debug("Loading library ======================");
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


// NOTE: experimental, don't know if useful yet
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




///////////////// BusGroup : dictionary of bus

BusGroup {


}





////////////////////////////////









//////////////////////////////////////////////////////////////////////



