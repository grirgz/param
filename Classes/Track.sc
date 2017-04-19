
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
				ret[\key] = key;
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

	putAll { arg ... args;
		var k = this.key;
		// preserve key
		super.putAll(*args);
		this[\key] = k;
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

TrackTemplateDef : TrackDef {
	// just another placeholder
}

ProtoDef : TrackDef {
	// just another placeholder
}

ProtoTemplateDef : ProtoDef {
	// just another placeholder
}

FileSystemProject : TrackDef {
	classvar <>defaultProject;
	classvar <>paths;
	classvar <cwd;
	classvar <>loadingFiles;
	classvar <>current;

	*initClass {
		all = IdentityDictionary.new;
		loadingFiles = List.new;
		defaultProject = (
			path: { arg self;
				self.key.asString
			},

			open: { arg self;
				if(self.isOpening != true) {
					self.server.waitForBoot {
						self.isOpening = true;
						FileSystemProject.cwd = self.path;
						FileSystemProject.current = self;
						FileSystemProject.load("init.scd");
						self.isOpening = false;
					}
				}
			},

			load: { arg self, val;
				FileSystemProject.load(self.path +/+ val);
			},

			defaultQuant_: { arg self, quant;
				Pdef.defaultQuant = quant;
				Ndef.defaultQuant = quant;
				self[\defaultQuant] = quant;
			},

			clock: { arg self;
				TempoClock.default;
			},

			server: { arg self;
				Server.default;
			},

			tempo_: { arg self, tempo;
				~t = tempo;
				self.clock.tempo = tempo;
			},

			tempo: { arg self;
				self.clock.tempo;
			},
		);
		paths = List.new;
		cwd = Platform.userAppSupportDir +/+ "FileSystemProjectDictionary";
		if(PathName(cwd).isFolder.not) {
			File.mkdir(cwd);
		};
		this.addPath(cwd);
	}

	*clearLoadingFiles {
		loadingFiles = List.new;
	}

	*loadFileTillEnd { arg path;
		var res = path;
		var ret;
		if(loadingFiles.includesEqual(path)) {
			path.debug("Already loading this file, do nothing\nFileSystemProject.clearLoadingFiles; // to reset");
			^nil;
		} {
			if(File.exists(res)) {
				var file;
				var code;
				var end;
				var oldpath;
				loadingFiles.add(path);
				File.use(res, "r", { arg file;
					code = file.readAllString;
				});
				end = code.find("\n// END");
				if(end.notNil) {
					code = code.keep(end+1);
				};

				res.debug("Loading file");
				try {
					oldpath = thisProcess.nowExecutingPath;
					thisProcess.nowExecutingPath = path;
					ret = code.interpret;
					thisProcess.nowExecutingPath = oldpath;
				} { arg e;
					thisProcess.nowExecutingPath = oldpath;
					res.debug("Error when loading file");
					e.debug("ExC");
					loadingFiles.removeAt(loadingFiles.detectIndex({ arg x; x == path }));
					e.throw;
				};
				loadingFiles.removeAt(loadingFiles.detectIndex({ arg x; x == path }))
			} {
				path.debug("FileSystemProject: File don't exists");
				^nil
			};
		};
		^ret;
	}

	*load { arg path;
		var rpath = this.resolve(path);
		if(rpath.notNil and: { rpath.isFile }) {
			^this.loadFileTillEnd(rpath.fullPath);
		} {
			( "FileSystemProject.load: file doesnt exists or is a directory: " ++ path ).debug;
			^nil
		};
	}

	*cwd_ { arg path;
		var rp = this.resolve(path);
		if(rp.notNil) {
			cwd = rp.fullPath;
		} {
			( "FileSystemProject.cwd: file not found: " ++ path ).debug;
		}
	}

	*addPath { arg path;
		var rpath = this.resolve(path);
		if(rpath.notNil) {
			if(paths.includesEqual(rpath).not) {
				paths.add(rpath.fullPath);
			}
		} {
			debug( "Path not found: " ++ path )
		}
	}

	*resolve { arg val;
		// note: return a pathname
		// FIXME: why a pathname ???
		val = val.standardizePath;
		if(PathName(val).isAbsolutePath) {
			^PathName(val)
		};
		( [ this.cwd ] ++ paths ).do({ arg path;
			var pn;
			[path, val].debug("try resolve");
			if(val == "") {
				pn = PathName(path);
			} {
				pn = PathName(val);
				pn.fullPath.asCompileString.debug("fp");
				if(pn.isAbsolutePath.not) {
					pn = PathName(path +/+ val);
				};
			};
			pn.fullPath.asCompileString.debug("fp2");
			if(pn.isFile or: { pn.isFolder }) {
				^pn
			}
		});
		^nil;
	}

	*unresolve { arg val;
		// asRelativePath
		// FIXME: what to do if two candidates ?
		paths.do({ arg path;
			var pn;
			[path, val].debug("try unresolve");
			if(val.beginsWith(path)) {
				^val[( path.size )..]
			}
		});
		^val;
	}

	*new { arg key, val;
		var rkey = this.resolve(key.asString);
		if(rkey.notNil) {
			if(all[rkey].isNil) {
				^super.new(rkey.fullPath.asSymbol, val ? this.defaultProject);
			} {
				^super.new(rkey.fullPath.asSymbol, val);
			}
		};
		^nil
	}

}

FSProject {
	*new { arg ... args;
		^FileSystemProject(*args)
	}
	// TODO: redirect to FileSystemProjectDictionary or switch the name
}


FileSystemProjectDictionary : IdentityDictionary {
	classvar <>all;
	var <>key;
	var <>source;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { arg key, val;
		if(all[key].isNil) {
			^super.new.initFileSystemProjectDictionary(val).prAdd(key)
		} {
			var ret = all[key];
			^ret;
		}
	}

	prAdd { arg xkey;
		key = xkey;
		all[key] = this;
	}

	initFileSystemProjectDictionary { arg val;

	}

	at  { arg atkey;
		var satkey, instanceKey, path, fullpath, curval;
		var abspath, abskey;
		satkey = atkey.asString;
		instanceKey = PathName(satkey).fileName.asSymbol;
		path = PathName(satkey).pathOnly;
		abspath = FileSystemProject.resolve(path);
		if(abspath.isNil) {
			[key, atkey, abspath, abskey].debug("FileSystemProjectDictionary: Error: no project here");
			^nil
		} {
			abspath = abspath.fullPath;
			abskey = abspath +/+ instanceKey;
			curval = super.at(abskey.asSymbol);
			if(curval.isNil) {
				var inst = this.loadFromFileSystam(abspath, instanceKey);
				if(inst.isNil) {
					[key, atkey, abspath, abskey].debug("FileSystemProjectDictionary: no archived data");
					^nil
				} {
					super.put(abskey.asSymbol, inst);
					^inst;
				};
			} {
				^curval
			};
		}
	}

	put { arg atkey, val;
		var satkey, instanceKey, path, fullpath, curval;
		var abspath, abskey;
		satkey = atkey.asString;
		instanceKey = PathName(satkey).fileName.asSymbol;
		path = PathName(satkey).pathOnly;
		abspath = FileSystemProject.resolve(path);
		if(abspath.isNil) {
			[key, atkey, abspath, abskey].debug("FileSystemProjectDictionary: Error: no project here");
			^nil
		} {
			abspath = abspath.fullPath;
			abskey = abspath +/+ instanceKey;
			super.put(abskey.asSymbol, val);
		}
	}

	saveAt { arg atkey;
		var satkey, instanceKey, path, fullpath, curval;
		var abspath, abskey;
		satkey = atkey.asString;
		instanceKey = PathName(satkey).fileName.asSymbol;
		path = PathName(satkey).pathOnly;
		abspath = FileSystemProject.resolve(path);
		if(abspath.isNil) {
			[key, atkey, abspath, abskey].debug("FileSystemProjectDictionary: Error: no project here");
			^nil
		} {
			abspath = abspath.fullPath;
			abskey = abspath +/+ instanceKey;
			curval = super.at(abskey.asSymbol);
			if(curval.notNil) {
				this.saveToFileSystam(abspath, instanceKey, curval);
			} {
				[key, atkey, abspath, abskey].debug("FileSystemProjectDictionary: no data to save");
				^nil
			};
		}
	}

	loadAt { arg atkey;
		var satkey, instanceKey, path, fullpath, curval;
		var abspath, abskey;
		satkey = atkey.asString;
		instanceKey = PathName(satkey).fileName.asSymbol;
		path = PathName(satkey).pathOnly;
		abspath = FileSystemProject.resolve(path);
		if(abspath.isNil) {
			[key, atkey, abspath, abskey].debug("FileSystemProjectDictionary: Error: no project here");
			^nil
		} {
			var inst;
			abspath = abspath.fullPath;
			abskey = abspath +/+ instanceKey;
			inst = this.loadFromFileSystam(abspath, instanceKey);
			if(inst.isNil) {
				[key, atkey, abspath, abskey].debug("FileSystemProjectDictionary: no archived data");
				^nil
			} {
				super.put(abskey.asSymbol, inst);
				^inst;
			};
		}
	}

	saveToFileSystam { arg abspath, instanceKey, obj;
		var datapath = abspath +/+ "data";
		var filepath = datapath +/+ key +/+ ( instanceKey ++ ".arch.scd" );
		var fh, archdata;
		if(PathName(datapath).isFolder.not) {
			File.mkdir(datapath);
		};
		if(PathName(datapath +/+ key).isFolder.not) {
			File.mkdir(datapath +/+ key);
		};
		fh = File(filepath, "w");
		archdata = obj.tryPerform(\asArchiveData);
		if(archdata.notNil) {
			fh.write(archdata.asCompileString)
		} {
			fh.write(obj.asCompileString)
		};
		fh.close;
	}

	loadFromFileSystam { arg abspath, instanceKey;
		var datapath = abspath +/+ "data";
		var filepath = datapath +/+ key +/+ ( instanceKey ++ ".arch.scd" );
		//if(PathName(datapath).isFolder.not) {
		//	File.mkdir(datapath);
		//};
		//if(PathName(datapath +/+ key).isFolder.not) {
		//	File.mkdir(datapath +/+ key);
		//};
		[abspath, instanceKey, datapath, filepath].debug("filepath!");
		if(PathName(filepath).isFile) {
			^FileSystemProject.load(filepath);
		} {
			filepath.debug("No file!");
			^nil;
		}
	}

	clear {
		if(key.notNil) {
			all[key] = nil
		};
		^nil
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

//+Document {
//	*load {
//		switch(Platform.ideName,
//			\scide, {
//				var curdir = ""
//
//			}, 
//			\scvim, {
//
//			}
//	}
//
//	currentDir
//	
//}
