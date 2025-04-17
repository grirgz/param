
////////////////// nice storages for resources

BufDef {
	// FIXME: need to decide if file path is stored as absolute or relative
	//		need to make clear where it is symbol and where it is string
	classvar <>root; // deprecated
	classvar <>paths;

	*all {
		// the value can be a buffer or a symbol path or a string path
		^PresetDictionary.new(\BufDef);
	}

	*bufferChannelCache {
		^PresetDictionary.new(\BufDef_bufferChannelCache);
	}

	// FIXME: maybe child classes of BufDef should have independent path
	//*paths {
		//^PresetDictionary.new(\BufDef_classvar)[\paths];
	//}

	//*paths_ { arg val
		//^PresetDictionary.new(\BufDef_classvar)[\paths] = val;
	//}

	*initClass {
		//root = "~/Musique/sc/samplekit".standardizePath;
		//paths = List[root,"~/Musique/sc/reckit".standardizePath];
		paths = List.new;
	}

	*addPath { arg path;
		//var rpath = FileSystemProject.resolve(path);
		var rpath = PathName(path).normalizedPath;
		if(rpath.notNil) {
			if(paths.includesEqual(rpath).not) {
				paths.add(rpath);
			}
		} {
			Log(\Param).error("Path not found: " ++ path );
		}
	}


	*new { arg name, path, channels;
		name = name.asSymbol;
		if(path.isNil) {
			// getter
			if(this.all.at(name).isNil) {
				if(name.asString.contains("/")) {
					// special constructor with file path as name
					name = this.abspath_to_relpath(name.asString);
					//Log(\Param).debug("BufName: special cons: %", name);
					^BufDef(name.asSymbol, name.asString, channels)
				} {
					if(name == \0) {
						// special case for buffer named \0 corresponding to bufnum 0
						// do not add it to .all because TagSpecDef(\BufDef) already have it
						//this.all.put(\0, Buffer.cachedBufferAt(Server.default, 0) );
						//^this.all.at(\0);
						^Buffer.cachedBufferAt(Server.default, 0);
					} {
						^nil
					}
				}
			} {
				var path = this.all.at(name);
				if(path.isKindOf(Buffer)) {
					^path;
				} {
					path = this.relpath_to_abspath(path.asString);
					^this.getBufferForPath(path, channels);
					//^BufferPool.get_stereo_sample(client, path);
				}
			}
		} {
			// setter
			if(path.isKindOf(Number)) {
				//// buffer in memory only
				// path is in fact the frame count
				if(this.all.at(name).isNil) {
					var buf = Buffer.alloc(this.server, path, channels ? 2);
					buf.key = name;
					this.all.put(name, buf);
					^this.all.at(name);
				} {
					// already defined
					^this.all.at(name);
				}

			} {
				if(path.isKindOf(Buffer)) {
					var buf = path;
					buf.key = name;
					this.all.put(name, buf);
					^this.all.at(name)
				} {
					//// file buffer
					if(this.all.at(name).isNil) {
						var inst, relpath;
						relpath = path.asString;
						// doesn't exists, define it
						path = this.relpath_to_abspath(relpath);
						inst = this.getBufferForPath(path, channels).key_(name);
						this.all.put(name, relpath);
						^inst;
						//^BufferPool.get_stereo_sample(client, path).key_(name);
					} {
						// already defined
						var path = this.all.at(name);
						if(path.isKindOf(Buffer)) {
							^path;
						} {
							path = this.relpath_to_abspath(path);
							^this.getBufferForPath(path, channels);
							//^BufferPool.get_stereo_sample(client, path);
						}
					}
				}
			}
		};
	}

    *newFrom { arg buf;
		if(buf.key.notNil) {
			// kind of useless because BufDef just return the buffer
			^BufDef(buf.key);
		} {
			if(buf.path.notNil) {
				^BufDef(buf.path);
			};
		};
		Log(\Param).debug("BufDef.newFrom: no BufDef for %", buf);
		^nil
    }

	*server {
		^Server.default;
	}

	*getBufferForPath { arg path, channels, action;
		this.watchServer(this.server);
		path = path.asSymbol;
		this.bufferChannelCache[path] = this.bufferChannelCache[path] ?? { Dictionary.new }; // channels can be an array so no IdentityDictionary
		if(this.bufferChannelCache[path][\numChannels].isNil) {
			var numchan = SoundFile.use(path.asString, { arg f;
				f.numChannels;
			});
			//numchan.debug("numchan!!!!!!!!");
			this.bufferChannelCache[path][\numChannels] = numchan
		};
		if(this.bufferChannelCache[path][\wantedChannels].isNil) {
			this.bufferChannelCache[path][\wantedChannels] = channels ? this.bufferChannelCache[path][\numChannels];
		};
		if(channels.isNil) {
			channels = this.bufferChannelCache[path][\numChannels]
		};
		//channels.debug("channels");
		if(this.bufferChannelCache[path][channels].isNil) {
			//~f.(1,2) == [0];
			//~f.(2,1) == [0,0];
			//~f.(3,1) == [0,0,0];
			//~f.(1,3) == [0];
			//~f.(2,3) == [0,1];
			//~f.(3,2) == [0,1,0];
			//~f.(5,3) == [0,1,2,0,1];
			//~f.(3,5) == [0,1,2];
			//~f.(5,5) == nil == [0,1,2,3,4]
			var chanArray = { arg want, have;
				want = (want-1).clip(0,inf);
				have = (have-1).clip(0,inf);
				(0..have).wrapAt((0..want)).collect(_.asInteger); // fail silently if float
			};

			var have = this.bufferChannelCache[path][\numChannels];
			var want = channels;
			if(want == have) {
				this.bufferChannelCache[path][channels] = Buffer.read(this.server, path.asString, 0, -1, action);
			} {
				var chan;
				chan = if(want.isSequenceableCollection) {
					want.collect(_.asInteger);
				} {
					chanArray.(want, have);
				};
				//[channels, path, chan].collect(_.asCompileString).debug("asked chan!!!!");
				this.bufferChannelCache[path][channels] = Buffer.readChannel(this.server, path.asString, 0, -1, chan, action);
			};
		}; 
		^this.bufferChannelCache[path][channels]
	}

	*bufferList { arg numChannels;
		^BufDef.all.keys.as(Array).sort
		.collect({ arg k; 
			var path;
			var buf;
			var cache;
			//k.asCompileString.debug("TagSpecDef BufDef_stereo key");
			path = BufDef.relpath_to_abspath(BufDef.all[k]).asSymbol;
			//path.asCompileString.debug("TagSpecDef BufDef_stereo path");
			cache = BufDef.bufferChannelCache[path];
			//cache.debug("TagSpecDef BufDef_stereo cache");
			if(cache.notNil) {
				numChannels = numChannels ?? { cache[\numChannels] };
				buf = cache[numChannels];
				if(buf.notNil) {
					k -> buf
				};
			};
		}).select(_.notNil)
	}

	*watchServer { |server|
		if(NotificationCenter.registrationExists(server,\newAllocators,this).not,{
			NotificationCenter.register(server,\newAllocators,this,{
				this.freeAll;
			});
		});
	}

	*loadDialog { arg name, callback;
		if(WindowDef(\filedialog_sample).notNil) {
			WindowDef(\filedialog_sample).front(nil,
				{ arg path, file;
					file = path +/+ file;
					if(name.notNil) {
						callback.(
							BufDef(name, file).asCompileString.postln;
						)
					} {
						callback.(
							BufDef(file).asCompileString.postln;
						)
					}
				};
			)
		} {
			Dialog.openPanel({ arg file;
				if(name.notNil) {
					callback.(
						BufDef(name, file).asCompileString.postln;
					)
				} {
					callback.(
						BufDef(file).asCompileString.postln;
					)
				}
			});
		};
		^nil;
	}


	*mono { arg name, path;
		^this.new(name, path, 1);
	}

	*stereo { arg name, path;
		^this.new(name, path, 2);
	}

	*my_new { arg name, path, channels;
		// FIXME: this store the thing without knowing if it exists
		if(path.isNil) {
			if(this.all.at(name).isNil) {
				if(name.asString.contains("/")) {
					// special constructor with file path as name
					path = this.abspath_to_relpath(name.asString);
					this.all.put(name.asSymbol, path.asString);
					path = this.relpath_to_abspath(path);
				} {
					^nil
				}
			} {
				path = this.all.at(name);
				path = this.relpath_to_abspath(path);
			}
		} {
			this.all.put(name, path);
			path = this.relpath_to_abspath(path);
		};
		^path;
	}

	*relpath_to_abspath { arg path;
		path = path.standardizePath;
		if(PathName(path).isRelativePath) {
			this.paths.do { arg folder;
				var abspath = folder +/+ path;
				if(PathName(abspath).isFile) {
					^abspath;
				}
			}
		};
		^path
	}

	*addTrailingSlash { arg path;
		^if(path.endsWith("/")) {
			path
		} {
			path++"/"
		}
	}

	*removeTrailingSlash { arg path;
		^if(path.endsWith("/")) {
			path.drop(1)
		} {
			path
		}
	}

	*abspath_to_relpath { arg path;
		path = PathName(path.asString).normalizedPath;
		FileSystemProject.relativeToAbsolutePath(path) !? { arg abspath;
			^abspath;
	   	};
		
		this.paths.do { arg folder;
			folder = this.addTrailingSlash(folder);
			if(path.beginsWith(folder)) {
				var newpath;
				newpath = path.drop(folder.size);
				if(newpath.contains("/").not) {
					// magic path constructor doesnt recognize it when no slash
					newpath = "./"++newpath;
				};
				^newpath;
			};
		};
		^path;
	}

	*freeAll {
		//BufferPool.reset;	
		this.bufferChannelCache.do({ arg path; path.do(_.free) });
		this.all.do({ arg x; 
			if(x.isKindOf(Buffer)) { 
				x.free 
			}
	   	});
		this.bufferChannelCache.clear;
		this.all.clear;
	}

	*free { arg name, channels;
		var buf;
		name = name.asSymbol;
		buf = this.all[name];
		if(buf.isKindOf(Buffer)) {
			buf.free;
			this.all[name] = nil;
		} {
			if(buf.isKindOf(String) or: { buf.isKindOf(Symbol) }) {
				try {
					var path = this.relpath_to_abspath(buf.asString);
					this.bufferChannelCache[path.asSymbol].do({ arg buf;
						if(buf.isKindOf(Buffer)) {
							buf.free;
						};
					});
					this.bufferChannelCache[path.asSymbol] = nil;
				} { arg error;
					"In: %.free:".format(this).err;
					error.reportError;
					//error.throw;
				};
				this.all[name] = nil;
			} {
				"BufDef: %: can't free: doesn't exists".format(name).warn;
			}
		}
	}

	*reload { arg name, channels;
		var buf;
		name = name.asSymbol;
		buf = this.all[name];
		if(buf.isKindOf(Buffer)) {
			var numFrames = buf.numFrames, numChannels = buf.numChannels;
			buf.free;
			this.all[name] = nil;
			^BufDef(name, numFrames, numChannels)
		} {
			if(buf.isKindOf(String) or: { buf.isKindOf(Symbol) }) {
				var path = this.relpath_to_abspath(buf);
				this.bufferChannelCache[path.asSymbol].do({ arg buf;
					if(buf.isKindOf(Buffer)) {
						buf.free;
					};
			   	});
				this.bufferChannelCache[path.asSymbol] = nil;
				this.all[name] = nil;
				^BufDef(name, buf, channels)
			} {
				"BufDef: %: can't free: doesn't exists".format(name).warn;
			}
		};
	}

	*clear { arg name, channels;
		//this.all[name] = nil;
		^this.free(name, channels);
	}

	*presetCompileString { arg name;
		// this method define the BufDef while bufferCompileString is just a reference
		// but for path as key buffer, this is the same
		// FIXME: should support numChannels and .mono/.stereo
		var buf = this.all[name];
		name.debug("BufDef.presetCompileString");
		if(buf.isKindOf(Buffer)) {
			var path = this.abspath_to_relpath(this.all.at(name).path);
			^"BufDef(%, %)".format(name.asCompileString, path.asCompileString);
		} {
			if(buf.isKindOf(String) or: { buf.isKindOf(Symbol) }) {
				^"BufDef(%, %)".format(name.asCompileString, this.all.at(name).asCompileString);
			} {
				"BufDef: %: can't free: doesn't exists".format(name).warn;
			}
		};
	}

	*bufferCompileString { arg buffer;
		// Buffer.asCompileString is added in Param_extensions.sc
		if(buffer.notNil) {
			if(buffer.key.notNil) {
				var fileNumChan;
				var rpath = this.all[buffer.key];
				var path = this.relpath_to_abspath(rpath).asSymbol;
				if( BufDef.bufferChannelCache[path].notNil ) {
					fileNumChan = BufDef.bufferChannelCache[path][\numChannels];
				};
				[ buffer.key.asCompileString, rpath.asCompileString, path.asCompileString, fileNumChan, buffer.numChannels ].debug("bufferCompileString key");
				if(fileNumChan == buffer.numChannels) {
					^"BufDef(%)".format(buffer.key.asCompileString)
				} {
					var meth;
					meth = switch(buffer.numChannels,
						1, { ".mono" },
						2, { ".stereo" },
						{ "" },
					);
					^"BufDef%(%)".format(meth, buffer.key.asCompileString)
				};
			} {
				if(buffer.path.notNil) {
					var fileNumChan;
					var rpath = this.all[buffer.key];
					var path = this.relpath_to_abspath(rpath).asSymbol;
					if( BufDef.bufferChannelCache[path].notNil ) {
						fileNumChan = BufDef.bufferChannelCache[path][\numChannels];
					};
					[ buffer.path.asCompileString, rpath.asCompileString, path.asCompileString, fileNumChan, buffer.numChannels ].debug("bufferCompileString path");
					if(fileNumChan == buffer.numChannels) {
						^"BufDef(%)".format(buffer.path.asCompileString)
					} {
						var meth;
						meth = switch(buffer.numChannels,
							1, { ".mono" },
							2, { ".stereo" },
							{ "" },
						);
						//^"BufDef%(%)".format(meth, buffer.key.asCompileString)
						^"BufDef%(%)".format(meth, buffer.path.asCompileString)
					};
				} {
					Log(\Param).warning("WARNING: BufDef.bufferCompileString: Can't get compile string of buffer %".format(buffer));
					^buffer.asCompileString;
				};
			}
		} {
			^"nil"
		};
	}

}

WavetableDef : BufDef {

	*all {
		^PresetDictionary.new(\WavetableDef);
	}

	*bufferChannelCache {
		^PresetDictionary.new(\WavetableDef_bufferChannelCache);
	}

	*bufferMultiCache {
		^PresetDictionary.new(\WavetableDef_bufferMultiCache);
	}

	*new { arg name, path, channels;
		var multipath, keypath;
		keypath = path;
		//Log(\Param).debug("BufDef.new: % % %", name, path, channels);
		if(path.isKindOf(Array)) {
			multipath = path;
			// this is support for loading different files in same buffer, not well tested
			keypath = multipath.join(":"); 
			path = path.first;
		};
		if(path.isNil) {
			// getter
			if(this.all.at(name).isNil) {
				if(name.asString.contains("/")) {
					// special constructor with file path as name
					name = this.abspath_to_relpath(name);
					//Log(\Param).debug("BufName: special cons: %", name);
					^BufDef(name.asSymbol, name.asString, channels)
				} {
					^nil
				}
			} {
				var path = this.all.at(name);
				if(path.isKindOf(Buffer)) { 
					// buffer in memory only
					^path;
				} {
					// buffer is at some path
					if(this.bufferMultiCache[path].notNil) {
						// this is a multi wavetable. In this.all[name], path is a :-separated path
						^this.bufferMultiCache[path].first;  
					} {
						// standard wavetable
						path = this.relpath_to_abspath(path);
						^this.getBufferForPath(path, channels, nil, multipath).addHalo(\asCompileString, "WavetableDef(%)".format(name.asCompileString));
					};
				}
			}
		} {
			// setter
			if(path.isKindOf(Number)) {
				//// buffer in memory only
				// path is in fact the frame count
				if(this.all.at(name).isNil) {
					var buf = Buffer.alloc(this.server, path, channels ? 2);
					buf.key = name;
					this.all.put(name, buf);
					^this.all.at(name);
				} {
					// already defined
					^this.all.at(name);
				}

			} {
				// Wavetable class support
				if(path.isKindOf(Wavetable)) {
					if(this.all.at(name).isNil) {
						// first set
						var buf = Buffer.alloc(this.server, path.size, path.numChannels ? channels ? 2);
						buf.loadCollection(path);
						buf.key = name;
						this.all.put(name, buf);
						^this.all.at(name);
					} {
						// already existing
						if(path.size != this.all.at(name).numFrames) {
							Log(\Param).error("WavetableDef %: wavetable already exists and is different size: current: %, new: %", name, this.all.at(name).numFrames, path.size);
							^this.all.at(name);
						} {
							var buf = this.all.at(name);
							buf.loadCollection(path);
							^buf;
						}

					}
				} {

					//// file buffer
					if(this.all.at(name).isNil) {
						var inst;
						// doesn't exists, define it
						path = this.relpath_to_abspath(keypath.asString);
						inst = this.getBufferForPath(path, channels, nil, multipath).key_(name).addHalo(\asCompileString, "WavetableDef(%)".format(name.asCompileString));
						this.all.put(name, keypath.asString);
						^inst;
						//^BufferPool.get_stereo_sample(client, path).key_(name);
					} {
						// already defined
						var path = this.all.at(name);
						if(path.isKindOf(Buffer)) {
							^path;
						} {
							path = this.relpath_to_abspath(path);
							^this.getBufferForPath(path, channels, nil, multipath).addHalo(\asCompileString, "WavetableDef(%)".format(name.asCompileString));
							//^BufferPool.get_stereo_sample(client, path);
						}
					}
				}
			}
		};
	}

	*getBufferForPath { arg path, channels, action, multipath;
		var keypath = path.asSymbol;
		this.watchServer(this.server);
		if(multipath.notNil) {
			keypath = multipath.join(":").asSymbol;
			path = multipath.first.asSymbol;
		};
		this.bufferChannelCache[keypath] = this.bufferChannelCache[keypath] ? Dictionary.new; // channels can be an array so no Identity
		if(this.bufferChannelCache[keypath][\numChannels].isNil) {
			var numchan = SoundFile.use(path.asString, { arg f;
				f.numChannels;
			});
			//numchan.debug("numchan!!!!!!!!");
			this.bufferChannelCache[keypath][\numChannels] = numchan
		};
		if(this.bufferChannelCache[keypath][\wantedChannels].isNil) {
			this.bufferChannelCache[keypath][\wantedChannels] = channels ? this.bufferChannelCache[keypath][\numChannels];
		};
		if(channels.isNil) {
			channels = this.bufferChannelCache[keypath][\wantedChannels]
		};
		//channels.debug("channels");
		if(this.bufferChannelCache[keypath][channels].isNil) {
			//~f.(1,2) == [0];
			//~f.(2,1) == [0,0];
			//~f.(3,1) == [0,0,0];
			//~f.(1,3) == [0];
			//~f.(2,3) == [0,1];
			//~f.(3,2) == [0,1,0];
			//~f.(5,3) == [0,1,2,0,1];
			//~f.(3,5) == [0,1,2];
			//~f.(5,5) == nil == [0,1,2,3,4]
			var chanArray = { arg want, have;
				want = (want-1).clip(0,inf);
				have = (have-1).clip(0,inf);
				(0..have).wrapAt((0..want)).collect(_.asInteger); // fail silently if float
			};

			var have = this.bufferChannelCache[keypath][\numChannels];
			var want = channels;
			if(want == have) {
				if(multipath.notNil) {
					this.bufferMultiCache[keypath] = multipath.collect({ arg ipath;
						this.readWavetableFromPath(ipath);
					});
					this.bufferChannelCache[keypath][channels] = this.bufferMultiCache[keypath].first;
					this.bufferChannelCache[keypath][channels].consecutive = this.bufferMultiCache[keypath];
				} {
					this.bufferChannelCache[keypath][channels] = this.readWavetableFromPath(path);
				};
			} {
				var chan;
				"not implemented: wavetable custom channel".throw;
				chan = if(want.isSequenceableCollection) {
					want.collect(_.asInteger);
				} {
					chanArray.(want, have);
				};
				//[channels, keypath, chan].collect(_.asCompileString).debug("asked chan!!!!");
				this.bufferChannelCache[keypath][channels] = Buffer.readChannel(this.server, keypath.asString, 0, -1, chan, action);
			};
		}; 
		^this.bufferChannelCache[keypath][channels]
	}

	*readWavetableFromPath { arg path;
		var buf, table, sf;
		sf = SoundFile.openRead(path.asString);
		if(sf.notNil) {
			table = FloatArray.newClear(sf.numFrames);
			sf.readData(table);
			sf.close; // close the file
			table = table.as(Signal);
			table = table.asWavetable;
			if(log2(table.size) % 1 != 0) {
				Log(\Param).warning("WavetableDef: Buffer size not power of 2: %".format(table.size, path));
			};
			buf = Buffer.loadCollection(this.server, table);
			^buf;
		} {
			Log(\Param).error("WavetableDef: File not found: %", path);
			"WavetableDef: File not found: %".format(path).throw;
			^nil
		}
	}

}

BusDef : Bus {
	
	classvar <>all;
	var <>key;

	*initClass {
		all = IdentityDictionary.new;
	}

	*control { arg server, numChannels=1;
		var alloc;
		server = server ? Server.default;
		alloc = server.controlBusAllocator.alloc(numChannels);
		if(alloc.isNil, {
			error("Meta_Bus:control: failed to get a control bus allocated."
				+ "numChannels:" + numChannels + "server:" + server.name);
			^nil
		});
		^super.new(\control, alloc, numChannels, server)
	}

	*audio { arg server, numChannels=1;
		var alloc;
		server = server ? Server.default;
		alloc = server.audioBusAllocator.alloc(numChannels);
		if(alloc.isNil, {
			error("Meta_Bus:audio: failed to get an audio bus allocated."
			+ "numChannels:" + numChannels + "server:" + server.name);
			^nil
		});
		^super.new(\audio, alloc, numChannels, server)
	}

	*newFromIndex { arg arate, idx, numchan;
		var foundbus;
		foundbus = this.all.detect({ arg busdef;
			busdef.index == idx and: {
				// not sure chan check is needed
				// you can't allocate 2 bus with same index and different number of chan
				busdef.numChannels == numchan 
			}
		});
		^foundbus
	}

	printOn { arg stream;
		this.storeOn(stream)
	}

	storeOn { arg stream;
		//stream << "BusDef(%%)".format("\\", this.key)
		stream << "BusDef(%)".format(this.key.asCompileString)
	}

	*new { arg name, rate, channels;
		var bus;

		//if(~veco_project_path.notNil) {
		//	client = ~veco_project_path;
		//}
		name = name.asSymbol; // force all symbol else this will bite you

		if(all.at(name).notNil or: {rate.isNil}) {
			bus = all.at(name)
		} {
			if(channels.isNil) {
				if(rate == \audio) {
					channels = 2
				} {
					channels = 1
				};
			};
			//rate.debug("BusDef.new:rate");
			bus = super.alloc(rate, Server.default, channels);
			//bus.debug("BusDef.new:bus");
			bus.key = name;
			this.watchServer(Server.default);
			all.put(name, bus);
		};
		^bus;
	}

	//asControlInput {
		//if(mapMode)
	//}

	*free { arg name;
		all.at(name).free;
		all.put(name, nil);
	}

	*freeClient {
		this.freeAll;
	}

	*freeAll {
		all.do { _.free };	
		all = IdentityDictionary.new;
	}

	*watchServer { |server|
		if(NotificationCenter.registrationExists(server,\newAllocators,this).not,{
			NotificationCenter.register(server,\newAllocators,this,{
				this.freeAll;
			});
		});
	}

}

GroupDef {
	classvar <>groupdict;

	*initClass {
		groupdict = Dictionary.new;
	}

	*newGroup { arg target, addaction, nodeID;
		if(nodeID.notNil) {
			^Group.basicNew(Server.default, nodeID)
		} {
			^Group.new(target, addaction)
		}
	}

	*new { arg name, target, addaction='addToHead', nodeID;
		var group;
		target = target ? Server.default;
		if(groupdict[name].isNil or: { groupdict[name].isPlaying.not }) {
			groupdict[name] = this.newGroup(target, addaction, nodeID);
			groupdict[name].register(true);
		};
		^groupdict[name]
	}

	*makePayload { arg event, fun;
		// re-use payload code from PmodEnv quark to avoid dependency
		if(event[\hasPmodEnvPayload] != true) {
			//debug("attach payload");
			event[\hasPmodEnvPayload] = true;
			event[\finish] = event[\finish].addFunc({ arg ev;
				//"ss".debug("run finish");
				ev.keys.do { arg key;
					if(ev[key].isKindOf(Event)) {
						if([\PmodEnv_payload].includes(ev[key][\type])) {
							//ev[key].payload.debug("run finish");
							ev[key] = ev[key].payload.(ev, key);
						};
					};

				};

			});
		} {
			//Log(\Param).debug("Pgroup has already a payload");
		};
		^(type: \PmodEnv_payload, payload: { fun }).yield;
	}


	*pattern { arg name, target, addaction='addToHead';
		var unref = { arg group;
			var ret;
			if(group.isKindOf(Pattern)) {
				// since you can't nest makePayload, i store args in pattern object
				var args = group.args;
				//"parent group is pattern %, %, %".format(*args).debug;
				if(args.notNil) {
					//"parent group pattern unref".debug;
					args[1] = unref.(args[1]);
					ret = GroupDef(*args)
				};
			};
			if(ret.isNil) {
				//group.debug("passing group as is");
				group
			} {
				ret
			};
		};
		^Prout({ arg ev;
			this.makePayload(ev, {
				try {
					var parentgroup;
					parentgroup = unref.(target);
					//"create group %, %, %".format(name, parentgroup ? target, addaction).debug;
					GroupDef(name, parentgroup ? target, addaction);
				} { arg error;
					error.reportError;
					error.throw;
				}
			})
		}).loop.addUniqueMethod(\args, { [ name, target, addaction ] })
	}
}

ParGroupDef : GroupDef {
	*newGroup { arg target, addaction;
		^ParGroup.new(target, addaction)
	}
}
