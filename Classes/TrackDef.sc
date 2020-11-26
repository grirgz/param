
TrackDef : ProtoDef {
	// just another placeholder to manage players and mixers
	*all {
		^PresetDictionary.new(\TrackDef);
	}
}

TrackTemplateDef : TrackDef {
	// just another placeholder to distinguish tracks proto-classes and proto-instances
	*all {
		^PresetDictionary.new(\TrackTemplateDef);
	}
}

TrackMixerDef {
	// FIXME: redirect to defined protoclass or be a placeholder ?
	//		or defined protoclass could be the default ? but constructor signature will be different than TrackDef

	*all {
		^PresetDictionary.new(\TrackMixerDef);
	}
}

FileSystemProject : TrackDef {
	classvar <>defaultProject;
	classvar <>paths;
	classvar <cwd;
	classvar <>loadingFiles;
	classvar <>loadedFiles;
	classvar <>current;
	classvar <>temporaryRecordFolder = "/tmp/";
	classvar <>recordFolder;

	*all {
		^PresetDictionary.new(\FileSystemProject);
	}

	*initClass {
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

			loadProject: { arg self, path;
				var projectfile;
				var proj;
				if(path.endsWith("data/project.scd")) {
					projectfile = path;
				} {
					projectfile = path +/+ "data/project.scd";
				};
				proj = FileSystemProject.load(projectfile);
				FileSystemProject.current = proj;
				PathName(proj.dataPath).files.do { arg file;
					if(file.extension == "scd" and: { file.fileName != "project.scd" }) {
						FileSystemProject.load(file.fullPath)
					}
				};
				// load first level of folders in data/ (for grids)
				PathName(proj.dataPath).folders.do { arg folder;
					folder.files.do { arg file;
						if(file.extension == "scd" and: { file.fileName != "project.scd" }) {
							FileSystemProject.load(file.fullPath)
						}
					};
				};
				proj;
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

	*new { arg key, val;
		var rkey = this.resolve(key.asString);
		if(rkey.notNil) {
			if(this.all[rkey].isNil) {
				^super.new(rkey.fullPath.asSymbol, val ? this.defaultProject);
			} {
				^super.new(rkey.fullPath.asSymbol, val);
			}
		} {
			"FileSystemProject: can't resolve this project".error;  
		};
		^nil
	}

	*clearLoadingFiles {
		loadingFiles = List.new;
	}

	*loadFileTillEnd { arg path, silent=false;
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
				loadingFiles.removeAt(loadingFiles.detectIndex({ arg x; x == path }));
				loadedFiles = loadedFiles ?? { Set.new };
				loadedFiles.add(path);
			} {
				if(silent == false) {
					Log(\Param).error("ERROR: FileSystemProject: File don't exists: %", path);
				};
				^nil
			};
		};
		^ret;
	}

	*load { arg path, silent=false;
		var rpath = this.resolve(path);
		if(rpath.notNil and: { rpath.isFile }) {
			^this.loadFileTillEnd(rpath.fullPath, silent);
		} {
			if(silent == false) {
				Log(\Param).error("FileSystemProject.load: file doesnt exists or is a directory: " ++ path );
			};
			^nil
		};
	}

	*loadOnce { arg path, silent=false;
		var rpath = this.resolve(path);
		loadedFiles = loadedFiles ?? { Set.new };
		if(loadedFiles.includes(rpath)) {
			if(silent == false) {
				Log(\Param).debug("FileSystemProject.load: file already loaded: " ++ path );
			};
		} {
			if(rpath.notNil and: { rpath.isFile }) {
				^this.loadFileTillEnd(rpath.fullPath, silent);
			} {
				if(silent == false) {
					Log(\Param).error("FileSystemProject.load: file doesnt exists or is a directory: " ++ path );
				};
				^nil
			}
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
			if(paths.includesEqual(rpath.fullPath).not) {
				paths.add(rpath.fullPath);
			}
		} {
			Log(\Param).error("Path not found: " ++ path );
		}
	}

	*resolve { arg val;
		var abspath;
		// need to decide if it should return only existing files
		// currently do not check for existance if path starts with ./ or is absolute, only if it need to find it in this.paths
		// note: return a pathname
		// FIXME: why a pathname ???
		val = val.standardizePath;
		if(val.isKindOf(PathName)) {
			val = val.fullPath;
		};
		if(PathName(val).isAbsolutePath) {
			^PathName(val)
		};
		abspath = this.relativeToAbsolutePath(val);
		if(abspath.notNil) {
			^PathName(abspath)
		};
		( [ this.cwd ] ++ paths ).do({ arg path;
			var pn;
			//Log(\Param).debug("try resolve %", [path, val]);
			if(val == "") {
				// FIXME: why one would pass an empty string ?
				// why return the first path ??
				pn = PathName(path);
			} {
				pn = PathName(path +/+ val);
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

	*relativeToAbsolutePath { arg val;
		var curpath;
		if(val.beginsWith("./")) {
			curpath = Document.current.path;
			if(curpath.notNil) {
				^PathName(Document.current.path).pathOnly +/+ val.drop(2)
			} {
				^val;
			};
		} {
			^nil
		};
	}

}

FSProject {
	*new { arg ... args;
		^FileSystemProject(*args)
	}
	// TODO: redirect to FileSystemProjectDictionary or switch the name
}




