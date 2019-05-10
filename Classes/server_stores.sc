
////////////////// nice storages for resources

BufDef {
	classvar <>client = \BufDef;
	classvar <>all;
	classvar <>root;
	classvar <>paths;

	*initClass {
		all = IdentityDictionary.new;
		root = "~/Musique/sc/samplekit".standardizePath;
		paths = List[root,"~/Musique/sc/reckit".standardizePath];
	}

	*new { arg name, path, channels=2;
		if(path.isNil) {
			// getter
			if(all.at(name).isNil) {
				if(name.asString.contains("/")) {
					// special constructor with file path as name
					name = this.abspath_to_relpath(name);
					^BufDef(name.asSymbol, name.asString)
				} {
					^nil
				}
			} {
				var path = all.at(name);
				if(path.isKindOf(Buffer)) {
					^path;
				} {
					path = this.relpath_to_abspath(path);
					^BufferPool.get_stereo_sample(client, path);
				}
			}
		} {
			// setter
			if(path.isKindOf(Number)) {
				//// buffer in memory only
				// path is in fact the frame count
				if(all.at(name).isNil) {
					var buf = Buffer.alloc(Server.default, path, channels);
					buf.key = name;
					all.put(name, buf);
					^all.at(name);
				} {
					// already defined
					^all.at(name);
				}

			} {
				//// file buffer
				if(all.at(name).isNil) {
					// doesn't exists, define it
					all.put(name, path);
					path = this.relpath_to_abspath(path);
					^BufferPool.get_stereo_sample(client, path).key_(name);
				} {
					// already defined
					var path = all.at(name);
					if(path.isKindOf(Buffer)) {
						^path;
					} {
						path = this.relpath_to_abspath(path);
						^BufferPool.get_stereo_sample(client, path);
					}
				}
			}
		};
	}

	*loadDialog { arg name;
		Dialog.openPanel({ arg file;
			if(name.notNil) {
				BufDef(name, file)
			} {
				BufDef(file)
			}
		});
		^nil;
	}

	*mono { arg name, path;
		// FIXME: majority of other method doesnt take in account the possibility of mono buffers
		path = this.my_new(name, path);
		if(path.notNil) {
			^BufferPool.get_mono_sample(client, path);
		} {
			"%: Path not found: %, %".format(this.name, name, path).error;
			^nil
		}
	}

	*my_new { arg name, path, channels;
		// FIXME: this store the thing without knowing if it exists
		if(path.isNil) {
			if(all.at(name).isNil) {
				if(name.asString.contains("/")) {
					// special constructor with file path as name
					path = this.abspath_to_relpath(name.asString);
					all.put(name.asSymbol, path.asString);
					path = this.relpath_to_abspath(path);
				} {
					^nil
				}
			} {
				path = all.at(name);
				path = this.relpath_to_abspath(path);
			}
		} {
			all.put(name, path);
			path = this.relpath_to_abspath(path);
		};
		^path;
	}

	*relpath_to_abspath { arg path;
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

	*abspath_to_relpath { arg path;
		path = path.asString.standardizePath;
		this.paths.do { arg folder;
			if(path.beginsWith(folder)) {
				^path.drop(folder.size+1);
			};
		};
		^path;
	}

	*freeClient {
		BufferPool.release_client(client)
	}

	*freeAll {
		BufferPool.reset;	
	}

	*reload { arg name;
		if(all.at(name).isNil) {
			if(name.asString.contains("/")) {
				// special constructor with file path as name
				name = this.abspath_to_relpath(name);
				^BufDef(name.asSymbol, name.asString)
			} {
				^nil
			}
		} {
			var path = all.at(name);
			if(path.isKindOf(Buffer)) {
				^path;
			} {
				var buf;
				path = this.relpath_to_abspath(path);
				buf = BufferPool.get_stereo_sample(client, path);
				BufferPool.release(buf, client);
				^BufferPool.get_stereo_sample(client, path);
			}
		}
	}

	*clear { arg name;
		if(all.at(name).isNil) {
			^nil
		} {
			var path = all.at(name);
			if(path.isKindOf(Buffer)) {
				^path
			} {
				var buf;
				path = this.relpath_to_abspath(path);
				buf = BufferPool.get_stereo_sample(client, path);
				BufferPool.release(buf, client);
				^nil;
			}
		}
	}

}

WavetableDef : BufDef {

	classvar <>client = \veco;
	classvar <>all;
	classvar <>root;

	*initClass {
		all = IdentityDictionary.new;
		root = "~/Musique/sc/samplekit/wavetable".standardizePath;
	}
	
	*new { arg name, path;
		path = this.my_new(name, path);
		path.debug("WavetableDef.new: path");
		^BufferPool.get_wavetable_sample(client, path);
	}


}

BusDef : Bus {
	
	classvar <>client = \veco;
	classvar <>all;
	classvar <>root;
	var <>key;

	*initClass {
		all = IdentityDictionary.new;
		root = "~/Musique/sc/samplekit".standardizePath;
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

	printOn { arg stream;
		this.storeOn(stream)
	}

	storeOn { arg stream;
		//stream << "BusDef(%%)".format("\\", this.key)
		stream << "BusDef(%%)".format("\\", this.key)
	}

	*new { arg name, rate, channels;
		var bus;

		//if(~veco_project_path.notNil) {
		//	client = ~veco_project_path;
		//}

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

	*newGroup { arg target, addaction;
		^Group.new(target, addaction)
	}

	*new { arg name, target, addaction='addToHead';
		var group;
		target = target ? Server.default;
		if(groupdict[name].isNil or: { groupdict[name].isPlaying.not }) {
			groupdict[name] = this.newGroup(target, addaction);
			groupdict[name].register(true);
		};
		^groupdict[name]
	}
}

ParGroupDef : GroupDef {
	*newGroup { arg target, addaction;
		^ParGroup.new(target, addaction)
	}
}
