
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

FileSystemProject : TrackDef {
	classvar <>defaultProject;
	classvar <>paths;
	classvar <cwd;
	classvar <>loadingFiles;

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
		cwd = "~/".standardizePath;
	}

	*loadFileTillEnd { arg path;
		var res = path;
		if(loadingFiles.includesEqual(path)) {
			path.debug("Already loading this file, do nothing");
		} {
			if(File.exists(res)) {
				var file;
				var code;
				var end;
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
					code.interpret;
				} { arg e;
					e.debug("ExC");
					e.throw;
					res.debug("Error when loading file");
				};
				loadingFiles.removeAt(loadingFiles.detectIndex({ arg x; x == path }))
			} {
				path.debug("FileSystemProject: File don't exists");
			}
		}
	}

	*load { arg path;
		var rpath = this.resolve(path);
		if(rpath.notNil and: { rpath.isFile }) {
			this.loadFileTillEnd(rpath.fullPath);
		} {
			( "FileSystemProject.load: file doesnt exists or is a directory: " ++ path ).debug;
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
		val = val.standardizePath;
		( [ this.cwd ] ++ paths ).do({ arg path;
			var pn;
			pn = PathName(val);
			if(pn.isAbsolutePath.not) {
				pn = PathName(path +/+ val);
			};
			if(pn.isFile or: { pn.isFolder }) {
				^pn
			}
		});
		^nil;
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
