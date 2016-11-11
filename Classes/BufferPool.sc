BufferPool {

	classvar <counts,<annotations,<paths, <mono_paths, <wavetable_paths;
	classvar bufferclass;

	*initClass {
		//bufferclass = BufDef;
		bufferclass = Buffer;
		this.reset;
	}

	*alloc { |client,name,numFrames,numChannels=1,server=nil|
		var prev,buf;
		server = server ?? Server.default;
		buf = bufferclass.alloc(server, numFrames, numChannels);
		this.retain(buf,client,name);
		^buf
	}

	*read { |client,name,path, action=nil, server=nil|
		var prev,buf;
		server = server ?? Server.default;
		buf = bufferclass.read(server, path, 0, -1, action);
		this.retain(buf,client,name);
		^buf
	}

	*read_mono { |client,name,path, action=nil, server=nil|
		var prev,buf;
		server = server ?? Server.default;
		buf = bufferclass.readChannel(server, path, 0, -1, [0], action); //FIXME: can't choose the channel
		this.retain(buf,client,name);
		^buf
	}

	*read_wavetable { |client,name,path, action=nil, server=nil|
		var prev,buf;
		var soundfile;
		var signal;
		var size;
		server = server ?? Server.default;

		// load sample as signal

		soundfile = SoundFile.openRead(path);
		size = soundfile.numFrames;
		signal = Signal.newClear(size);
		soundfile.readData(signal);
		soundfile.close; // close the file

		// load signal in wavetable buffer 

		// FIXME: action ?
		buf = bufferclass.alloc(server, size*2, 1);
		signal = signal.asWavetable;
		buf.loadCollection(signal);

		this.retain(buf,client,name);
		^buf
	}

	*read_forced_stereo { |client, name, path, action=nil, server=nil|
		var prev,buf;
		var numchan;
		server = server ?? Server.default;
		numchan = SoundFile.use(path, { arg f;
			f.numChannels;
		});
		if(numchan == 1) {
			buf = bufferclass.readChannel(server, path, 0, -1, [0,0], action);
		} {
			buf = bufferclass.read(server, path, 0, -1, action); 
		};
		this.retain(buf,client,name);
		^buf
	}

	*get_sample { |client,path, action=nil, server=nil|
		var buf = paths.at(path); 
		//paths.debug("paths");
		if(buf.notNil, {
			this.retain(buf,client,\void);
		}, {
			buf = this.read(client,\void,path,action,server);
			paths[path] = buf;
		});
		^buf
	}

	*get_forced_stereo_sample { |client,path, action=nil, server=nil|
		^this.get_stereo_sample(client, path, action, server);
	}

	*get_stereo_sample { |client,path, action=nil, server=nil|
		var buf = paths.at(path); 
		//paths.debug("paths");
		if(buf.notNil, {
			this.retain(buf,client,\void);
		}, {
			buf = this.read_forced_stereo(client,\void,path,action,server);
			paths[path] = buf;
		});
		^buf
	}

	*get_mono_sample { |client,path, action=nil, server=nil|
		var buf = mono_paths.at(path); 
		//mono_paths.debug("mono_paths");
		if(buf.notNil, {
			this.retain(buf,client,\void);
		}, {
			buf = this.read_mono(client,\void,path,action,server);
			mono_paths[path] = buf;
		});
		^buf
	}

	*get_wavetable_sample { |client,path, action=nil, server=nil|
		var buf = wavetable_paths.at(path); 
		//mono_paths.debug("mono_paths");
		if(buf.notNil, {
			this.retain(buf,client,\void);
		}, {
			buf = this.read_wavetable(client,\void,path,action,server);
			wavetable_paths[path] = buf;
		});
		^buf
	}

	*retain { |buf,client,name|
		//if(annotations.at(buf,client).notNil,{
		//	//(client.asString++" already retained buffer "++buf.path).warn;
		//}, {
			counts.add(buf);
			annotations.put(buf,client,name);
			this.watchServer(buf.server);
		//});
	}
	*release { |buf,client|
		// seems broken ?
		var dict,key;
		if(annotations.at(buf,client).isNil,{
			(client++" already released buffer "++buf.path).warn;
		}, {
			counts.remove(buf);
			annotations.removeAt(buf,client);
			if(counts.itemCount(buf) == 0,{
				if(buf.numChannels == 1) {
					mono_paths[buf.path] = nil;
				} {
					paths[buf.path] = nil;
				};
				if(wavetable_paths[buf.path].notNil) {
					// FIXME: strange to test for channel number for mono and stereo but not for wavetable
					wavetable_paths[buf.path] = nil;
				};
				buf.free;
			})
		});
	}

	*is_freed { arg buf;
		counts.includes(buf)
	}

	*release_client { arg client;
		annotations.leafDo({ arg x, y;
			[x,y].debug("BufferPool.release_client: buf, client");
			if( x[1] == client ) { 
				x[0].debug("to free");
				this.release(x[0], x[1]);
			}
		})
	}

	*reset {
		if(counts.notNil,{
			counts.contents.keysValuesDo({ |buf,count| buf.free });
		});
		counts = Bag.new;
		annotations = MultiLevelIdentityDictionary.new;
		paths = Dictionary.new;
		mono_paths = Dictionary.new;
		wavetable_paths = Dictionary.new;
	}
	*watchServer { |server|
		if(NotificationCenter.registrationExists(server,\newAllocators,this).not,{
			NotificationCenter.register(server,\newAllocators,this,{
				this.reset;
			});
		});
	}
	*itemCount { |buf| ^counts.itemCount(buf) }
	*buffers { ^counts.contents.keys.as(Array) }
}
