
(
ProtoClassDef(\TrackMixerDef, (
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
			recbus: 4000,
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

	// output to this bus so we can record mixer output, without having to change outbus and add a master
	recbus: { arg self;
		BusDef(\trackMixerDef_record_+++self.key, \audio, self.numchannels)
	},

	recbus_: { arg self, val;
		self[\recbus] = val;
		self.proxy.set(\recbus, val);
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
		self.proxy[self.stages.recbus] = \filter -> { arg in;
			Out.ar(\recbus.kr(self.recbus ? BusDef(\null,\audio)), in);
			in;
		};
		self.proxy.addSpec(\inbus, ParamBusSpec());
		self.proxy.addSpec(\recbus, ParamBusSpec());
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

	build_sender: { arg self, idx, force=false;
		if(force or: {self.proxy[idx+self.stages.sends].isNil}) {
			var sendkey = \send+++idx;
			self.proxy.addSpec(sendkey, \unipolar);
			self.proxy.put(idx+self.stages.sends, \filter -> { arg in;
				Out.ar(( \sendbus+++idx ).kr(self.zerobus), in * sendkey.kr(0));
				in;
			}, now:self.proxy.isPlaying);
		}
		// if proxy not playing, adding a source will create it on server, so set its group
		//self.proxy.parentGroup = GroupDef(\mixer, nil, \addAfter); 
	},

	stop: { arg self;
		PlayerWrapper(self.proxy).stop;
	},

	isPlaying: { arg self;
		PlayerWrapper(self.proxy).isPlaying;
	},

	edit: { arg self;
		WindowDef("TrackMixerDef_%".format(self.key).asSymbol, WindowDef(\TrackMixerDef)).front(self)
	},

	insertFxList: { arg self;
		if(self[\obj_insertFxList].isNil) {
			self[\obj_insertFxList] = SparseArray.new;
			//self[\obj_insertFxList] = TagSpecDef(\SynthDef_InsertFx).list.collect { arg item, idx;
				//\filter -> { arg in; SynthDef.wrap(SynthDesc(item.key).def.func, nil, [in]) };
			//};
		};
		self[\obj_insertFxList];
	},

	insertFxAt: { arg self, idx, offset;
		if(offset.isNil) {
			offset = self.stages.postfx;
		};
		idx = idx + offset;
		if(self.insertFxList[idx].isNil) {
			self.insertFxList[idx] = ProtoTemplateDef(\TrackMixer_InserFx).new(self.proxy, idx)
		};
		self.insertFxList[idx]
		
	},

	//enableInsertFx: { arg self, idx, val = true, offset;
		//if(offset.isNil) {
			//offset = self.stages.postfx;
		//};
		//idx = idx + offset;
		//if(val == true) {
			//if(self.insertFxList[idx].notNil) {
				//self.proxy[idx] = self.insertFxList[idx];
			//} {
				//Log(\Param).debug("Can't enable, No fx defined for index %", idx);
			//}
		//} {
			//self.proxy[idx] = nil
		//}
	//},

	//isInsertFxEnabled: { arg self, idx, offset;
		//if(offset.isNil) {
			//offset = self.stages.postfx;
		//};
		//idx = idx + offset;
		//self.proxy[idx].isNil;
	//},

));
~trackMixerDef = ProtoClassDef(\TrackMixerDef);

ProtoTemplateDef(\TrackMixer_InserFx, (
	// ndef style fx

	new: { arg self, proxy, index, synthName;
		self = self.deepCopy;
	
		self.index = index;
		self.proxy = proxy;
		self.synthName = synthName;
	
		self;
	},
	
	offset: 0,
	absIndex: { arg self;
		self.offset + self.index;
	},

	synthName: \default,
	synthName_: { arg self, val;
		self[\synthName] = val;
		self.updateFx;
	},

	updateFx: { arg self;
		if(self.proxy[self.index].notNil) {
			self.proxy[self.index] = self.fxValue
		}
	},

	wrapFunctionWithIndexedArguments: { arg self;
		var newArgs = List.new;
		var index = self.index;
		var synthName = self.synthName;
		Log(\Param).debug("synthName %", synthName);
		newArgs = SynthDesc(synthName).controlNames.collect({ arg item;
			if(item != \in and: {item.asString.endsWith(index).not}) {
				( item ++ index ).asSymbol
			} {
				item;
			};
		});
		"{ arg %; SynthDef.wrap(SynthDesc(%).def.func, nil, [%]); }".format(newArgs.join(", "), synthName.asCompileString, newArgs.join(", ")).interpret;
	},

	controlNames: { arg self;
		var index = self.index;
		SynthDesc(self.synthName).controlNames.collect({ arg item;
			if(item != \in and: {item.asString.endsWith(index).not}) {
				( item ++ index ).asSymbol
			} {
				item;
			};
		});
	},

	fxValue: { arg self;
		if(self.synthName.notNil) {
			//\filter -> { arg in; SynthDef.wrap(SynthDesc(self.synthName).def.func, nil, [in]) };
			//\filter -> SynthDesc(self.synthName).def.func
			\filter -> self.wrapFunctionWithIndexedArguments;
		}
	},

	asParamGroup: { arg self;
		var stdNames = SynthDesc(self.synthName).controlNames;
		//ParamGroup(self.controlNames.collect({ arg x, idx; 
			//Param(self.proxy, x, Param(self.proxy, stdNames[idx].asSymbol).spec);
		//}))
		ParamGroup(self.proxy.objects[self.index].controlNames.collect({ arg x, idx; 
			Param(self.proxy, x);
		}))

	},

	index: 100,

	play: { arg self;
		self.proxy[self.index] = self.fxValue
	},

	stop: { arg self;
		self.proxy[self.index] = nil
	},
	
	isPlaying: { arg self;
		self.proxy[self.index].notNil;
	},

	isPlaying_: { arg self, val;
		if(val == true) {
			self.play;
		} {
			self.stop;
		}
	},

));

WindowDef(\TrackMixerDef, { arg me, mixer;
	var param;
	var sends = [];
	var layout;
	var slider, knobs = List.new, textfield;
	layout = VLayout(
		HLayout(
			VLayout (
				slider = Slider.new.orientation_(\vertical),
				textfield = TextField.new,
			),
			VLayout(*
				knobs = 4.collect({ 
					Knob.new;
				})
			)
		)
	);
	layout.addUniqueMethod(\model_, { arg me, mixer;
		if(mixer.isKindOf(Volume)) {
			//"volume".debug;
			param = Param(mixer, \volume);
			sends = 4.collect({ arg x; nil });
		} {
			//"track".debug;
			param = mixer.asParam(\postfader);
			sends = 4.collect({ arg x; mixer.asParam(\send+++x) });
		};
		//[slider, param].debug("slider, param: what ?");
		slider.mapParam(param);
		textfield.mapParam(param);
		knobs.do({ arg x, idx; x.mapParam(sends[idx]) })
	});
	if(mixer.notNil) {
		layout.model = mixer;
	};
	layout.addUniqueMethod(\slider, { slider });
	layout.addUniqueMethod(\textfield, { textfield });
	layout.addUniqueMethod(\knobs, { knobs });
	layout;
});
);

(
	// FIXME: why in double ? seems not used
ProtoTemplateDef(\NdefTrackMixer, (

	clear: { arg self;
		self.all[self.key] = nil;
	},

	stages: { arg self;
		self[\stages] = (
			in: 0,
			prefader: 1000,
			prefx: 3000,
			sends: 1100,
			postfx: 2000,
			postfader: 3000,
		);
		self[\stages];
	},

	numChannels: { arg self;
		self[\numChannels] = 2;
		self[\numChannels];
	},

	numChannels_: { arg self, val;
		self[\numChannels] = val;
		self.define_mixer;
	},

	inbus: { arg self;
		BusDef(\trackMixerDef_+++self.key, \audio, self.numChannels)
	},

	inbus_: { arg self, val;
		self[\inbus] = val;
		self.proxy.set(\inbus, val);
	},

	proxy: { arg self;
		self.define_mixer; // init proxy
		self[\proxy]
	},

	define_mixer: { arg self;
		//self.proxy = NodeProxy.new;
		self.proxy = Ndef(\trackMixerDef_+++self.key);
		self.proxy[self.stages.in] = {
			InFeedback.ar(\inbus.kr(self.inbus), self.numChannels);
		};
		self.proxy[self.stages.prefader] = \filter -> { arg in;
			in * \prefader.kr(0).dbamp;
		};
		self.proxy[self.stages.postfader] = \filter -> { arg in;
			in * \postfader.kr(0).dbamp;
		};
		self.proxy.addSpec(\inbus, ParamBusSpec());
		self.proxy.addSpec(\prefader, \db);
		self.proxy.addSpec(\postfader, \db);
		self.proxy.parentGroup = GroupDef(\mixer, nil, \addAfter);
		self.asParam(\inbus).set(self.inbus);
	},

	play: { arg self;
		var outbus = self.outbus;
		if(outbus.isKindOf(Bus)) {
			outbus = outbus.index;
		};
		self.proxy.parentGroup = GroupDef(\mixer, nil, \addAfter);
		self.proxy.play(outbus, self.numChannels, GroupDef(\mixer, nil, \addAfter));
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
		BusDef(\null+++self.numChannels, \audio, self.numChannels)
	},

	zerobus: { arg self;
		BusDef(\zero+++self.numChannels, \audio, self.numChannels)
	},

	build_sender: { arg self, idx, force=false;
		if(force or: {self.proxy[idx+self.stages.sends].isNil}) {
			var sendkey = \send+++idx;
			self.proxy.addSpec(sendkey, \unipolar);
			self.proxy.put(idx+self.stages.sends, \filter -> { arg in;
				Out.ar(( \sendbus+++idx ).kr(self.zerobus), in * sendkey.kr(0));
				in;
			}, now:self.proxy.isPlaying);
		}
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

);
