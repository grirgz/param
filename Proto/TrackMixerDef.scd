
~trackMixerDef = ProtoClass((
	all: {PresetDictionary(\trackMixerDef)},
	new: { arg self, key, numchannels=2, outbus=0;
	
		if(self.all[key].isNil) {
			self = self.deepCopy;
			self.key = key;
			self.init(key, numchannels, outbus);
			self.all[key] = self;
		} {
			self = self.all[key];
			//self.val = val;
		};
	
		self;
	},

	clear: { arg self;
		self.all[self.key] = nil;
	},

	init: { arg self, key, numchannels, outbus;
		self.stages = (
			in: 0,
			prefader: 1000,
			prefx: 3000,
			sends: 1100,
			postfx: 2000,
			postfader: 3000,
		);

		self.outbus = outbus;
		self.numchannels = numchannels;
		self.define_mixer;
		self.asParam(\inbus).set(self.inbus);

	},

	numChannels: { arg self;
		self.numchannels
	},

	inbus: { arg self;
		BusDef(\trackMixerDef_+++self.key, \audio, self.numchannels)
	},

	inbus_: { arg self, val;
		self[\inbus] = val;
		self.proxy.set(\inbus, val);
	},

	numchannels: 2,

	define_mixer: { arg self;
		//self.proxy = NodeProxy.new;
		self.proxy = Ndef(\trackMixerDef_+++self.key);
		self.proxy[self.stages.in] = {
			InFeedback.ar(\inbus.kr(self.inbus), self.numchannels);
		};
		self.proxy[self.stages.prefader] = \filter -> { arg in;
			in * \prefader.kr(0).dbamp;
		};
		self.proxy[self.stages.postfader] = \filter -> { arg in;
			in * \postfader.kr(0).dbamp;
		};
		self.proxy.addSpec(\inbus, XBusSpec());
		self.proxy.addSpec(\prefader, \db);
		self.proxy.addSpec(\postfader, \db);
		self.proxy.parentGroup = GroupDef(\mixer, nil, \addAfter);
	},

	play: { arg self;
		var outbus = self.outbus;
		if(outbus.isKindOf(Bus)) {
			outbus = outbus.index;
		};
		self.proxy.parentGroup = GroupDef(\mixer, nil, \addAfter);
		self.proxy.play(outbus, self.numchannels, GroupDef(\mixer, nil, \addAfter));
	},

	setOut: { arg self, outbus;
		self.outbus = outbus;
		self.play;
	},

	sendParam: { arg self, idx;
		self.asParam(( "send"++idx ).asSymbol);
	},

	asParam: { arg self, key;
		if(key.asString.beginsWith("send")) {
			var idx = key.asString.drop(4).asInteger;
			self.build_sender(idx);
			Param(self.proxy, key);
		} {
			Param(self.proxy, key);
		}
	},

	set: { arg self, key, val;
		self.asParam(key).set(val);
	},

	get: { arg self, key;
		self.asParam(key).get;
	},

	asParamGroup: { arg self;
		self.proxy.asParamGroup
	},

	nullbus: { arg self;
		BusDef(\null+++self.numchannels, \audio, self.numchannels)
	},

	zerobus: { arg self;
		BusDef(\zero+++self.numchannels, \audio, self.numchannels)
	},

	build_sender: { arg self, idx;
		var sendkey = \send+++idx;
		self.proxy.addSpec(sendkey, \unipolar);
		self.proxy.put(idx+self.stages.sends, \filter -> { arg in;
			Out.ar(( \sendbus+++idx ).kr(self.zerobus), in * sendkey.kr(0));
			in;
		}, now:self.proxy.isPlaying);
		// if proxy not playing, adding a source will create it on server, so set its group
		//self.proxy.parentGroup = GroupDef(\mixer, nil, \addAfter); 
	},

	stop: { arg self;
		self.proxy.stop;
	},

	isPlaying: { arg self;
		self.proxy.isPlaying;
	},

));
