
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
