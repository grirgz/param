
TrackDef : ProtoDef {
	// just another placeholder
}

TrackTemplateDef : TrackDef {
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
		if(val.isKindOf(PathName)) {
			val = val.fullPath;
		};
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




