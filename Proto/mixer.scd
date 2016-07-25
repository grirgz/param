
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
		self.outbus = outbus;
		self.numchannels = numchannels;
		self.define_mixer;
		self.asParam(\inbus).set(self.inbus)
	},

	numChannels: { arg self;
		self.numchannels
	},

	inbus: { arg self;
		BusDef(\trackMixerDef_+++self.key, \audio, self.numchannels)
	},

	numchannels: 2,

	define_mixer: { arg self;
		//self.proxy = NodeProxy.new;
		self.proxy = Ndef(\trackMixerDef_+++self.key);
		self.proxy[0] = {
			InFeedback.ar(\inbus.kr(self.inbus), self.numchannels);
		};
		self.proxy[1000] = \filter -> { arg in;
			in * \prefader.kr(0).dbamp;
		};
		self.proxy[2000] = \filter -> { arg in;
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
		self.proxy.play(outbus, self.numchannels);
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
		// if proxy not playing, adding a source will create it on server, so set its group
		self.proxy.addSpec(sendkey, \unipolar);
		self.proxy.put(idx+1100, \filter -> { arg in;
			Out.ar(( \sendbus+++idx ).kr(self.zerobus), in * sendkey.kr(0));
			in;
		}, now:false);
		//self.proxy.parentGroup = GroupDef(\mixer, nil, \addAfter); 
	},

	stop: { arg self;
		self.proxy.stop;
	},

	isPlaying: { arg self;
		self.proxy.isPlaying;
	},

));
