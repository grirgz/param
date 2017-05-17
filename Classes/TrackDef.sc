
TrackDef : ProtoDef {
	// just another placeholder to manage players and mixers
}

TrackTemplateDef : TrackDef {
	// just another placeholder to distinguish tracks proto-classes and proto-instances
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
			Log(\Param).info("Already loading this file, do nothing: %\nFileSystemProject.clearLoadingFiles; // use this to reset", path);
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

				Log(\Param).info("Loading file %", res);
				try {
					oldpath = thisProcess.nowExecutingPath;
					thisProcess.nowExecutingPath = path;
					ret = code.interpret;
					thisProcess.nowExecutingPath = oldpath;
				} { arg e;
					thisProcess.nowExecutingPath = oldpath;
					Log(\Param).error("Error when loading file %", res);
					Log(\Param).error("Exception %", e);
					loadingFiles.removeAt(loadingFiles.detectIndex({ arg x; x == path }));
					e.throw;
				};
				loadingFiles.removeAt(loadingFiles.detectIndex({ arg x; x == path }))
			} {
				Log(\Param).error("FileSystemProject: File don't exists: %", path);
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
			Log(\Param).error("FileSystemProject.load: file doesnt exists or is a directory: " ++ path );
			^nil
		};
	}

	*cwd_ { arg path;
		var rp = this.resolve(path);
		if(rp.notNil) {
			cwd = rp.fullPath;
		} {
			Log(\Param).error("FileSystemProject.cwd: file not found: " ++ path );
		}
	}

	*addPath { arg path;
		var rpath = this.resolve(path);
		if(rpath.notNil) {
			if(paths.includesEqual(rpath).not) {
				paths.add(rpath.fullPath);
			}
		} {
			Log(\Param).error("Path not found: " ++ path );
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
			//Log(\Param).debug("try resolve %", [path, val]);
			if(val == "") {
				pn = PathName(path);
			} {
				pn = PathName(val);
				//Log(\Param).debug("fp:%", pn.fullPath.asCompileString);
				if(pn.isAbsolutePath.not) {
					pn = PathName(path +/+ val);
				};
			};
			//Log(\Param).debug("fp2:%", pn.fullPath.asCompileString);
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
			//[path, val].debug("try unresolve");
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




