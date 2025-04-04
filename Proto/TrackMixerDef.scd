
(
SynthDef(\ifx_default, { arg out=0, gate=1;
	var sig;
	var in = In.ar(out, 2); 
	sig = in;
	sig = sig * EnvGen.kr(\adsr.kr(Env.adsr(0.1,0.1,0.8,0.1)), gate, doneAction:2);
	XOut.ar(out, \wet.kr(0), sig* \gain.kr(1));
}, metadata:(specs:(
	gain: \unipolar.asSpec,
))).add;


ProtoClassDef(\TrackMixerDef, ( 
	all: {PresetDictionary(\trackMixerDef)},
	new: { arg self, key, numChannels=2, outbus=0;
	
		if(self.all[key].isNil) {
			//self = self.deepCopy;
			self = ProtoClass((parent:self));
			self.key = key;
			self.init(key, numChannels, outbus);
			self.all[key] = self;
		} {
			self = self.all[key];
			//self.val = val;
		};
	
		self;
	},

	storeOn: { arg self, str; 
		str << self.refCompileString
	},

	refCompileString: { arg self;
		"TrackMixerDef(%)".format(self.key.asCompileString);
	},

	clear: { arg self;
		self.all[self.key] = nil;
	},

	init: { arg self, key, numChannels, outbus;
		self.stages = (
			in: 0,
			prefader: 1000,
			prefx: 2000,
			sends: 2100,
			postfx: 3000,
			postfader: 4000,
			recbus: 5000,
		);

		self.outbus = outbus;
		self.numChannels = numChannels;
		self.define_mixer;
		self.asParam(\inbus).set(self.inbus);

	},

	inbus: { arg self;
		BusDef(\trackMixerDef_+++self.key, \audio, self.numChannels)
	},

	inbus_: { arg self, val;
		self[\inbus] = val;
		self.proxy.set(\inbus, val);
	},

	inBus: { arg self;
		self.inbus;
	},

	inBus_: { arg self, val;
		self.inbus = val;
	},

	// output to this bus so we can record mixer output, without having to change outbus and add a master
	recbus: { arg self;
		BusDef(\trackMixerDef_record_+++self.key, \audio, self.numChannels)
	},

	recbus_: { arg self, val;
		self[\recbus] = val;
		self.proxy.set(\recbus, val);
	},

	numChannels: 2,

	numchannels: { arg self; self.numChannels }, // compat

	define_mixer: { arg self;
		//self.proxy = NodeProxy.new;
		self.proxy = Ndef(\trackMixerDef_+++self.key);
		self.proxy[self.stages.in] = {
			InFeedback.ar(\inbus.kr(self.inbus), self.numChannels);
		};

		// mute + solo
		self.proxy[self.stages.in + 1] = \filter -> { arg in;
			//Select.ar(( \mute.kr(0) - \solo.kr(0) ).clip(0,1), [in, DC.ar(0)]);
			in * ( 1 - ( \mute.kr(0) - \solo.kr(0) ).clip(0,1) );
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
		self.defineMixerHook;
	},

	muteMode_: { arg self, val;
		self.proxy.set(\mute, val.asInteger);
		self.changed(\muteMode);
	},

	muteMode: { arg self;
		self.proxy.get(\mute) == 1;
	},

	soloMode_: { arg self, valbool;
		var valint;
		valint = valbool.asInteger;
		self.proxy.set(\solo, valint);
		self.soloModeHook(valbool); // this should call .muteBySolo on all other tracks
		self.changed(\soloMode, valbool);
	},

	muteBySolo: { arg self, val;
		if(val == true) {
			// another track is soloed, should mute this one except if soloed too
			if(self.proxy.get(\solo) != 1) {
				self.proxy.set(\solo, -1);
			}
		} {
			// another track is no longer soloed, should unmute this one except if soloed too
			if(self.proxy.get(\solo) != 1) {
				self.proxy.set(\solo, 0);
			}
		}
	},

	soloMode: { arg self;
		self.proxy.get(\solo) == 1;
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
		self.getParam(( "send"++idx ).asSymbol);
	},

	getParam: { arg self, key;
		if(key.asString.beginsWith("send")) {
			var idx = key.asString.drop(4).asInteger;
			self.build_sender(idx);
			Param(self.proxy, key);
		} {
			if([\soloMode, \muteMode].includes(key)) {
				Param(Message(self), key, ParamBoolSpec());
			} {
				Param(self.proxy, key);
			}
		}
	},

	asParam: { arg self, key; // compat
		self.getParam(key)
	},

	set: { arg self, key, val;
		self.asParam(key).set(val);
	},

	get: { arg self, key;
		self.asParam(key).get;
	},

	setVal: { arg self, key, val; 
		// compatibility method for getSetCompileString
		// if the target val is a bus, should set the value of the bus
		// but in this case, the proxy is a Ndef so no bus mode needed
		self.set(key, val)
	},

	getVal: { arg self, key; 
		// compatibility method for getSetCompileString
		// if the target val is a bus, should get the value of the bus
		// but in this case, the proxy is a Ndef so no bus mode needed
		self.get(key)
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
				Out.ar(( \sendbus+++idx ).kr(BusDef(\sendbus+++idx, \audio, self.numChannels)), in * sendkey.kr(0));
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
		WindowDef("TrackMixerDef_%".format(self.key).asSymbol, WindowDef(\TrackMixerDef_advanced)).front(self)
	},

	asView: { arg self, options;
		var windef = WindowDef("TrackMixerDef_%".format(self.key).asSymbol, WindowDef(\TrackMixerDef_advanced));
		//( options ?? { [] } ).clump(2).do { arg item, idx;
			//windef.proto.perform(item.first.asSetter, item.last)
		//};
		windef.asView(self, options);
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
			self.insertFxList[idx] = ProtoTemplateDef(\TrackMixer_InsertFx).new(self.proxy, idx, self.key);
		};
		self.insertFxList[idx]
		
	},

	playIfNotPlaying: { arg self;
		if(self.isPlaying.not) {
			self.play;
		}
	},

	asStream: { arg self;
		Pfunc({ self.playIfNotPlaying; self.inbus }).asStream; // don't know if playing at each event is a problem
	},

	streamArg: { arg self; self.asStream }, // needed by Pbindef
	next: { arg self;
		self.playIfNotPlaying; self.inbus;
		
	},

	embedInStream: { arg self;
		Pfunc({ self.playIfNotPlaying; self.inbus }).embedInStream;
	},

	asUgenInput: { arg self;
		self.inbus
	},

	presetCompileString: { arg self;
		self.asParamGroup.getSetCompileString(self.refCompileString)
		++
		"%.outBus = %;".format(self.refCompileString, self.outbus.asCompileString);
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

//////////////// InsertFx

ProtoTemplateDef(\TrackMixer_InsertFx, (
	// ndef style fx

	new: { arg self, proxy, index, mixerKey, synthName;
		self = self.deepCopy;
	
		self.index = index;
		self.proxy = proxy;
		self.mixerKey = mixerKey; // used to have unique names
		self.synthName = synthName;
		self.proto = ProtoTemplateDef(\TrackMixer_InsertFx_SynthDef);
	
		self;
	},
	
	offset: 0,
	index: 100,
	absIndex: { arg self;
		// in current state, this is not used and self.index is already absolute
		self.offset + self.index;
	},
	relIndex: { arg self;
		self.index - TrackMixerDef(self.mixerKey).stages.postfx;
	},

	synthNameDefault: \ifx_default,
	synthName: \ifx_default, // overriden by constructor
	synthName_: { arg self, val;
		self[\synthName] = val;
		self.updateFx;
		self.changed(\synthName);
	},

	isEmpty: { arg self;
		self.proxy.isNil or: { self.synthName.isNil } or: { self.synthName == self.synthNameDefault }
	},

	updateFx: { arg self;
		//self.synthName.asCompileString.debug("updateFx");
		//self[\edit].asCompileString.debug("edit function");
		if(self.synthName == \equi) {
			self.proto = ProtoTemplateDef(\TrackMixer_InsertFx_equi);
			//self[\edit].asCompileString.debug("edit function XXX");
		} {
			self.proto = ProtoTemplateDef(\TrackMixer_InsertFx_SynthDef);
		};
		//self[\edit].asCompileString.debug("edit function");
		if(self.proxy[self.index].notNil) {
			self.proxy[self.index] = self.fxValue
		}
	},

	cloneSynthDefWithIndexedArguments: { arg self;
		var sdc;
		var synthDef;
		var suffix = self.index;
		var synthDesc;
		synthDesc = SynthDesc(self.synthName);
		if(synthDesc.notNil) {
			synthDef = synthDesc.def;
			sdc = synthDef.deepCopy;
			sdc.name = synthDef.name.asString ++ suffix; // no eff without asString!
			sdc.allControlNames.do { arg cno;
				if(not(self.excludedArgs.includes(cno.name))) {
					cno.name = (cno.name.asString ++ suffix);
				} {
					// ("Skipped renaming" + cno.name + "in" + sdc).postln
				}
			};
			sdc.add; // to prevent Ndef bug
			//sdc; // return whole sd clone to be collect-ed
			sdc.name.asSymbol;
		} {
			Log(\Param).debug("InsertFx.cloneSynthDefWithIndexedArguments: no synthDef: %", self.synthName);
			nil
		}
	},
));
//TagSpecDef(\SynthDef_InsertFx).addUnique(nil);
TagSpecDef(\SynthDef_InsertFx).addUnique(\ifx_default);

ProtoTemplateDef(\TrackMixer_InsertFx_SynthDef, (
	// ndef style fx

	excludedArgs: [\inbus, \in, \out, \gate, \fadeTime],
	index: 100,

	fxValue: { arg self;
		if(self.synthName.notNil) {
			//\filter -> { arg in; SynthDef.wrap(SynthDesc(self.synthName).def.func, nil, [in]) };
			//\filter -> SynthDesc(self.synthName).def.func
			self.cloneSynthDefWithIndexedArguments;
			//self.wrapFunctionWithIndexedArguments;
		}
	},


	asParamGroup: { arg self;
		ParamGroup(
			* [
				if(self.synthName.notNil) {

					SynthDesc(self.synthName).controlNames
					.reject({ arg x; self.excludedArgs.includes(x) })
					.collect({ arg item;
						self.getParam(item)
					})
				}
			].select(_.notNil)
		);
	},

	getParam: { arg self, val;
		var oldval = val;
		var pa;
		if(self.excludedArgs.includes(val).not) {
			val = (val ++ self.index).asSymbol;
		};
		pa = Param(self.proxy, val, Param.toSynthDefSpec(nil, oldval, self.synthName));
		pa.label_(oldval);
		pa.default = Param.getSynthDefDefaultValue(oldval, self.synthName ? \ifx_default);
		pa;
	},


	play: { arg self;
		// Ndef bug: when setting the synthdef by using SynthDef object instead of symbol
		//		if slot is nil, this work, if slot is already full, error synthdef not found
		// solution: use .add to add the modified synthdef
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

	asParamView: { arg self;
		self.asParamGroup.asView
	},

	edit: { arg self;
		WindowDef("%_%%".format(\TrackMixer_InsertFxEditor, self.mixerKey, self.index).asSymbol, WindowDef(\TrackMixer_InsertFxEditor)).front(self);
	},


	// deprecated
	wrapFunctionWithIndexedArguments: { arg self;
		var newArgs = List.new;
		var index = self.index;
		var synthName = self.synthName;
		var code = SynthDescLib.global.at(synthName).def.func.asCompileString;
		var regexp =  "[^[:w:]\.](%)[^[:w:]]";
		var excluded = self.excludedArgs;
		SynthDesc(synthName).controlNames.do({ arg item;
			var newname;
			if(excluded.includes(item).not and: {item.asString.endsWith(index).not}) {
				newname = ( item ++ index ).asSymbol;
				code = ~replaceAllRegexp.(code, regexp.format(item), newname);
			}
		});
		// replace in by out for ndef filters
		code = ~replaceAllRegexp.(code, regexp.format("inbus"), "out");
		code.interpret;
	},

	// deprecated
	replaceAllRegexp: { arg self, string, regexp, replaceString;
		var strar = string.as(Array);
		var res = string.findRegexp(regexp);
		res = res.clump(2).collect({ arg x; x.last }).reverse; // retrieve only the grouped expr
		//res.debug("res");
		res.do { arg item, idx;
			//strar[item.first..replaceString.size-1]
			( item.first .. item.first + item.last.size-1 ).reverseDo { arg remidx;
				//[remidx, strar[remidx]].debug("remidx");
				strar.removeAt(remidx);
			};
			replaceString.as(Array).do { arg char, repidx;
				//[char, repidx, repidx + item.first].debug("char repidx");
				strar = strar.insert(repidx + item.first, char);
				//strar.join.debug("strar");
			};
			
		};
		strar.join;
	},
));

ProtoTemplateDef(\TrackMixer_InsertFx_equi, (
	parent: ProtoTemplateDef(\TrackMixer_InsertFx_SynthDef),
	// ndef style fx

	prefix: { arg self; "i" ++ self.index  },

	monitorBus: { arg self;
		BusDef("InsertFx_equi_monitor_%_%".format(self.mixerKey, self.index).asSymbol, \audio, 2);
	},
	
	argsArray: [ 
		'loShelfFreq', 'loShelfGain', 'loShelfRs', 'loPeakFreq', 'loPeakGain', 'loPeakRq', 
		'midPeakFreq', 'midPeakGain', 'midPeakRq', 'hiPeakFreq', 'hiPeakGain', 'hiPeakRq', 
		'hiShelfFreq', 'hiShelfGain', 'hiShelfRs' 
	],

	fxValue: { arg self;
		if(self.synthName.notNil) {
			//\filter -> { arg in; SynthDef.wrap(SynthDesc(self.synthName).def.func, nil, [in]) };
			//\filter -> SynthDesc(self.synthName).def.func
			//self.wrapFunctionWithIndexedArguments;
			\filter -> { arg in; 
				var sig;
				sig = in.equi(nil, self.prefix);
				sig = sig * ( "gain" ++ self.index ).asSymbol.kr(1);
				Out.ar(self.monitorBus, sig);
				sig;
			}
		}
	},

	asParamGroup: { arg self;
		ParamGroup(self.argsArray.collect({ arg key;
			self.getParam(key)
		}));
	},

	getParam: { arg self, val, spec;
		var oldval = val;
		var pa;
		if(self.argsArray.includes(val)) {
			if(self.excludedArgs.includes(val).not) {
				val = ( self.prefix ++ val ).asSymbol;
			};
		} {
			if(self.excludedArgs.includes(val).not) {
				val = ( val ++ self.index ).asSymbol
			};
		};
		pa = Param(self.proxy, val, spec ? Param.toSynthDefSpec(nil, oldval, self.synthName))
	},

	asParamView: { arg self;
		WindowDef(\InsertFx_Equi, { arg def, model, inbus, paramdict;
			var equi, equiparams, equiparent;
			var scope = FreqScopeView(nil, Rect(0,0,100,100))
			.freqMode_(1)
			.active_(true);
			if(paramdict.isNil) {
				paramdict = IdentityDictionary.newFrom([
					wet: model.getParam(\wet),
					gain: model.getParam(\gain),
				]);
			};
			scope.inBus = inbus ?? { self.monitorBus };
			scope.onClose = {
				scope.kill;
			};

			equiparent = View.new;
			equi = EQui(equiparent,nil,model.proxy,nil,model.prefix);
			equi.sync;
			~equi = equi;
			VLayout (
				View.new.layout_(
					VLayout(
						//HLayout (
							//PlayerWrapper(model).asView,
							//BasicButton.new.string_("edit").action_({ 
								//WindowDef(\NdefEditor).front(model.proxy)
							//}),
							//BasicButton.new.string_("reset").action_({ 
								//equi.value = EQuiParams.new;
							//}),
						//),
						ParamGroupLayout.two_panes(
							ParamGroup ([
								paramdict[\wet],
								paramdict[\gain],
								//paramdict[\inbus],
								//paramdict[\out],
							])
						)
					)
				).maxHeight_(150),
				View.new.layout_(
					VLayout (
						equi
					)
				).minHeight_(300),
				scope,
			)
		}).asView(self);
	},
));
TagSpecDef(\SynthDef_InsertFx).addUnique(\equi);


////////////////////////////////////////////////////
/////////////////// GUI

WindowDef(\TrackMixerDef, { arg me, mixer; // not used much, TrackMixerDef_advanced is much better
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
WindowDef(\TrackMixerDef_fxlist, { arg me, mixer, slotCount;
	var layout;
	var num = slotCount ? 4;
	layout = VLayout(* num.collect { arg idx;
		me.waitIfNeeded;
		HLayout (
			PlayerWrapper(mixer.insertFxAt(idx)).label_(idx.asString).asView,
			Param(Message(mixer.insertFxAt(idx)), \synthName, TagSpecDef(\SynthDef_InsertFx)).asPopUpMenu,
			BasicButton.new.string_("Edit").action_({
				mixer.insertFxAt(idx).edit;
			}).fixedWidth_(40),
			mixer.insertFxAt(idx).getParam(\wet).asSlider.orientation_(\horizontal).minWidth_(100),
			mixer.insertFxAt(idx).getParam(\gain).asSlider.orientation_(\horizontal).minWidth_(100),
		)
	} ++ [nil]);
	layout;
});

);

/////////////////////////////// advanced mixer view
(
WindowDef(\TrackMixerDef_advanced, (
	showDelay: false,
	showOutbus: false,
	showMeter: true,
	showMuteSolo: true,
	showPlay: true,
	neverShowPlay: false,
	showFx: false,
	showSends: true,
	showFaderTextField: true,

	delayMode: \postdelay,
	faderMode: \postfader,
	meterMode: \recbus,

	sendCount: 8,
	fxCount: 4,

	maxWidth: 100,


	displayOptions: [\showSends, \showOutbus, \showPlay, \showFx, \showFaderTextField, \showDelay, \showMeter, \showMuteSolo],

	makeFader: { arg self, mixer;
		var stack;
		var textfield;
		var slider = Slider.new.orientation_(\vertical).background_(Color.clear);
		var param = mixer.getParam(self.faderMode);

		//[mixer, param].debug("TrackMixerDef_advanced: makeFader: mixer param");

		slider.mapParam(param);
		slider.minWidth_(10);
		slider.thumbSize_(10).knobColor_(Color.clear.alpha_(1));
		self.slider = slider;

		if(self.showMeter) {
			var bus, meterout;
			if(mixer[self.meterMode].notNil) {
				bus = mixer[self.meterMode].value(mixer);
			} {
				bus = BusDef(\zero, \audio);
			};
			if(self.meterOut.notNil) {
				self.meterOut.stop;
			};
			Log(\Param).debug("makeFader: mixer %, param %, bus %".format(mixer, param, bus));
			meterout = CompactServerMeterView(nil, 0,2, bus);
			//meterout = ();
			//meterout.stop;
			self.meterOut = meterout;
			//slider.onClose = {
				//"mixer fader close!".debug;
				//self.meterOut.stop;
			//};
			stack = StackLayout(
				slider, 
				meterout.view,
			).mode_(\stackAll);
		} {
			stack = slider;
		};


		if(self.showFaderTextField) {
			textfield = TextField.new;
			textfield.font_(Font.default.copy.size_(9));
			textfield.mapParam(param, 5);
			self.textfield = textfield;
			VLayout (
				stack,
				textfield,
			)
		} {
			self.textfield = nil;
			stack
		};
	},

	makeSendKnobs: { arg self, mixer;
		var lay;
		var knobs;
		self.showSends.debug("makeSendKnobs: showSends");
		if(self.showSends) {
			lay = VLayout(*
				knobs = self.sendCount.collect({ 
					Knob.new.minSize_(Point(25,25));
				})
			);
			knobs.do { arg item, idx;
				item.mapParam( mixer.sendParam(idx) );
				//item.toolTip = "%: %".format(send.key, mixerTemplate[\sendbus +++ send.key].value(mixerTemplate));
			};
			self.knobs = knobs;
			lay;
		} {
			nil
		};
	},

	makeMuteSoloButtons: { arg self, mixer;
		self.soloButton = Param(Message(mixer), \soloMode, ParamBoolSpec()).asButton.string_("S").minSize_(Point(5,15));
		self.muteButton = Param(Message(mixer), \muteMode, ParamBoolSpec()).asButton.string_("M").minSize_(Point(5,15));
		HLayout (
			self.muteButton,
			self.soloButton,
		)
	},

	makeMixerView: { arg self, mixer;
		var delayview;
		var mixerview;
		var boxview = self.boxview;
		var playbut;
		if(mixer.isNil) {
			mixer = () // ugly trick to not have to test if model is nil everywhere
		};
		boxview.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(buttonNumber == 1) {
				//WindowDef(\SimpleMixerOverlayMenu).front(simpleMixer, boxview, x, y);
				[self.model, mixer].debug("model");
				if(self.model.notNil) {
					WindowDef(\TrackMixerDef_advanced_OverlayMenu).front(self, view, x, y)
				} {
					if(self.track.notNil) {
						Menu(
							MenuAction("Enable mixer", { 
								self.track.isMixerEnabled = true;
							})
						).front
					};
				}
			};
		});
		if(self.showDelay) {
			delayview = TextField.new;
			self.delayView = delayview;
			if(mixer.proxy.notNil) {
				delayview.mapParam( Param(mixer.proxy, \postdelay, ControlSpec(0,0.1,\lin)) );
			};
		};
		boxview.layout = VLayout ( 
			*[
				if(self.showPlay and: { self.neverShowPlay.not }) {
					playbut = PlayerWrapper(mixer.proxy)
						.label_("%".format(mixer.name ?? mixer.key))
						.asView
						.minSize_(Point(10,18))
						.font_(Font.default.copy.size_(9))
					;
					~playbut = playbut;
					playbut;
				},
				mixerview = HLayout(*[
					self.makeFader(mixer),
					self.makeSendKnobs(mixer),
				].select(_.notNil)),
				if(self.showDelay) {
					HLayout(StaticText.new.string_("postdelay:"), delayview);
				},
				if(self.showMuteSolo) {
					self.makeMuteSoloButtons(mixer);
				},
				if(self.showOutbus) {
					self.outMenu = PopUpMenu.new.minWidth_(60);
					//Param(Message(mixer), \outbus,TagSpecDef(\BusDef)).asPopUpMenu.addAction({ 
						//mixer.play;
						//"kjkj".postln
					//});
					self.outMenu;
				},
				if(self.showFx) {
					self.makeTinyFxView(mixer)
				},
			].select(_.notNil)
		).spacing_(5).margins_(5);
		self.playerButton = playbut;
		boxview.background = Color.gray;
		boxview.maxWidth_(self.maxWidth - if(self.showSends) { 0 } { 30 });
		boxview.addUniqueMethod(\model_, { arg me, val;
			//val.debug("TrackMixerDef_advanced: model_");
			//self.model = val;
			self.setModel(val);
			//self.refreshView;
		});
		boxview.addUniqueMethod(\proto, { self });
		boxview.addUniqueMethod(\slider, { self.slider });
		boxview.addUniqueMethod(\textfield, { self.textfield });
		self.setModel(mixer);
		boxview;
	},

	setModel: { arg self, model;
		var mixer, param;
		if(model == ()) {
			model = nil;
		};
		self[\model] = model;
		mixer = model;
		
		if(mixer.notNil) {
			if(self.playerButton.notNil) {
				self.playerButton.model = PlayerWrapper(mixer);
			};

			self.knobs.do { arg item, idx;
				item.mapParam( mixer.sendParam(idx) );
				//item.toolTip = "%: %".format(send.key, mixerTemplate[\sendbus +++ send.key].value(mixerTemplate));
			};
			param = mixer.getParam(self.faderMode);

			//[mixer, param].debug("TrackMixerDef_advanced: setModel: mixer fader param");

			self.slider.enabled = true;
			self.slider.mapParam(param);
			self.slider.background = Color.clear;

			if(self.meterOut.notNil) {
				var bus;
				if(mixer[self.meterMode].notNil) {
					bus = mixer[self.meterMode].value(mixer);
					self.meterOut.bus = bus; // debug comment test
				} {
					self.meterOut.stop;
					//bus = BusDef(\zero, \audio);
				};
			};

			if(self.textfield.notNil) {
				self.textfield.mapParam(param, 5);
			};


			if(self.soloButton.notNil) {
				self.soloButton.mapParam(Param(Message(mixer), \soloMode, ParamBoolSpec()));
			};
			if(self.muteButton.notNil) {
				self.muteButton.mapParam(Param(Message(mixer), \muteMode, ParamBoolSpec()));
			};

			if(self.delayView.notNil and: {mixer.proxy.notNil}) {
				self.delayView.mapParam(Param(mixer.proxy, \postdelay, ControlSpec(0,0.1,\lin)));
			};


			if(self.outMenu.notNil) {
				self.outMenu.mapParam(Param(Message(mixer), \outbus,TagSpecDef(\BusDef))).addAction({ 
					mixer.play;
					//Log(\Param).debug("mixer play from outbus menu: ", mixer);
				});
			};

			if(self.fxButtons.notNil) {
				self.fxButtons.do { arg fxbut, idx;
					fxbut.model = PlayerWrapper(mixer.insertFxAt(idx)).label_(idx.asString);
				};
			}
		} {
			if(self.playerButton.notNil) {
				self.playerButton.model = nil;
			};
			
			self.knobs.do { arg item, idx;
				item.mapParam(nil);
			};

			self.slider.mapParam(nil);
			self.slider.enabled = false;
			self.slider.background = Color.grey;

			if(self.meterOut.notNil) {
				var bus;
				//bus = BusDef(\zero, \audio);
				//self.meterOut.bus = bus;
				self.meterOut.stop;
			};

			if(self.soloButton.notNil) {
				self.soloButton.mapParam(nil);
			};
			if(self.muteButton.notNil) {
				self.muteButton.mapParam(nil);
			};

			if(self.textfield.notNil) {
				self.textfield.mapParam(nil);
			};
			if(self.delayView.notNil) {
				self.delayView.mapParam(nil);
			};
			if(self.outMenu.notNil) {
				self.outMenu.mapParam(nil);
			};
			self.fxButtons.do { arg fxbut, idx;
				fxbut.model = nil
			};
			//self.boxview.removeAll;
		}
	},

	track_: { arg self, track;
		// track from TrackList support
		self[\track] = { track };
		self.boxview.followChange(track, \mixer, {
			if(track.parentTrack.notNil) {
				if(track.parentTrack.displayConfig.notNil) {
					track.parentTrack.displayConfig.asCompileString.debug("setting initial options");
					track.parentTrack.displayConfig.keysValuesDo { arg key,val;
						[key,val].debug("set");
						self[key] = val; // do not trigger refreshView
					};
				};
			};
			self.boxview.model = track.mixer;
			self.refreshView;
			if(self.playerButton.notNil) {
				self.playerButton.label = "%: %".format(track.index, track.label);
				self.playerButton.toolTip = "%: %".format(track.index, track.label);
			};
		});
	},

	refreshView: { arg self;
		self.boxview.removeAll;
		self.makeMixerView(self.model);
		if(self.track.notNil) {
			if(self.playerButton.notNil) {
				self.playerButton.label = "%: %".format(self.track.index, self.track.label);
				self.playerButton.toolTip = "%: %".format(self.track.index, self.track.label);
			};
		};
	},

	makeTinyFxView: { arg self, mixer;
		var layout;
		var num = self.fxCount ? 4;
		var fxButtons;
		layout = HLayout(* 
			fxButtons = num.collect { arg idx;
				PlayerWrapper(mixer.insertFxAt(idx)).label_(idx.asString).asView
				.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
					//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

					if(buttonNumber == 1) {
						//WindowDef(\SimpleMixerOverlayMenu).front(simpleMixer, boxview, x, y);
						//Log(\Param).debug("makeTinyFxView: model %", self.model);
						if(self.model.notNil) {
							WindowDef(\TrackMixerDef_advanced_fx_OverlayMenu).front(self, idx, view, x, y);
						};
						true; // do not trigger the general right click menu of Fx view
					};
				})
				.minSize_(Point(8,15))
				.font_(Font.default.size_(8))
				;
			} ++ [nil]
		).spacing_(1);
		self.fxButtons = fxButtons.select(_.notNil); // remove nil from layout
		layout;
	},

	makeFullFxView: { arg self, mixer;
		var layout;
		var num = self.fxCount ? 4;
		layout = VLayout(* num.collect { arg idx;
			HLayout (
				PlayerWrapper(mixer.insertFxAt(idx)).label_(idx.asString).asView,
				Param(Message(mixer.insertFxAt(idx)), \synthName, TagSpecDef(\SynthDef_InsertFx)).asPopUpMenu,
				BasicButton.new.string_("Edit").action_({
					mixer.insertFxAt(idx).edit;
				}).fixedWidth_(40),
				mixer.insertFxAt(idx).getParam(\wet).asSlider.orientation_(\horizontal).minWidth_(100),
				mixer.insertFxAt(idx).getParam(\gain).asSlider.orientation_(\horizontal).minWidth_(100),
			)
		} ++ [nil]);
		layout;
	},

	makePropertySetters: { arg pself;
		pself.displayOptions.do { arg item, idx;
			pself[item.asSetter] = { arg self, val;
				self[item] = val;
				self.refreshView;
			}
		};
	},

	asView: { arg self, def, mixer, options;
		//Log(\Param).debug("TrackMixerDef_advanced.asView: mixer: %", mixer);
		( options ?? { () } ).keysValuesDo { arg key, val;
			//Log(\Param).debug("set option % %", key, val);
			self[key] = val;
		};
		self.makePropertySetters;
		self.model = mixer;
		self.boxview = View.new;
		self.makeMixerView(mixer);
	},

));

WindowDef(\TrackMixerDef_advanced_OverlayMenu, { arg def, mixerview, view, x=600, y=600;
	var inview, layout;
	var sends;
	var mixer = mixerview.model;
	var sendView;
	var optionView;
	//[x, y].debug("xy");
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 300,100);
		win.endFrontAction = { win.close };
	};
	inview = View.new;
	sendView = View.new;
	optionView = View.new;


	layout = VLayout (
		//PlayerWrapper(mixerTemplate).asView,
		//HLayout(
			//StaticText.new.string_("Track count:"),
			//Param(Message(mixerTemplate), \trackCount, ControlSpec(1,16,\lin,1)).asNumberBox,
		//),
		WindowDef(\TrackMixerDef_fxlist).asView(mixer),
		[BasicButton.new.string_("> Sends        ").background_(Color.clear).action_({
			// spaces in label to align to left
			sendView.removeAll;
			sends = mixerview.sendCount.collect({ arg idx;
				HLayout(
					StaticText.new.string_("send %".format(idx)),
					Param(mixer.proxy, "%%".format(\sendbus, idx).asSymbol, TagSpecDef(\BusDef_object)).asPopUpMenu,
				)
			});
			sendView.layout = VLayout(
				*sends
			);
		}), align:\left],
		sendView,
		[BasicButton.new.string_("> Display options").background_(Color.clear).action_({
			optionView.removeAll;
			optionView.layout = HLayout (
				* mixerview.displayOptions.collect { arg item, idx;
					def.waitIfNeeded;
					Param(Message(mixerview), item, ParamBoolSpec()).asButton;
				};
			).margins_(0);
		}), align:\left],
		optionView,
		if(mixerview.track.notNil) {
			BasicButton.new.string_("Disable mixer").action_({
				mixerview.track.isMixerEnabled = false;
			})
		},
		nil
	);
	//def.windowDo { arg win;
		//win.setInnerExtent(view.bounds.extent.min(20));
	//};
	inview.layout_(layout);
	
}).border_(false);

WindowDef(\TrackMixerDef_advanced_fx_OverlayMenu, { arg def, mixerview, idx, view, x=600, y=600;
	var inview, layout;
	var sends;
	var mixer = mixerview.model;
	//[x, y].debug("xy");
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 300,100);
		win.endFrontAction = { win.close };
	};
	inview = View.new;

	layout = HLayout (
		PlayerWrapper(mixer.insertFxAt(idx)).label_(idx.asString).asView,
		Param(Message(mixer.insertFxAt(idx)), \synthName, TagSpecDef(\SynthDef_InsertFx)).asPopUpMenu,
		BasicButton.new.string_("Edit").action_({
			mixer.insertFxAt(idx).edit;
		}).fixedWidth_(40),
		mixer.insertFxAt(idx).getParam(\wet).asSlider.orientation_(\horizontal).minWidth_(100),
		mixer.insertFxAt(idx).getParam(\gain).asSlider.orientation_(\horizontal).minWidth_(100),
	);

	layout = VLayout(
		layout,
		nil
	);

	inview.layout_(layout);
	
}).border_(false);

//WindowDef(\TrackMixerDef_advanced).front(ProtoInst(\TrackMixerDef, \tr1))

/////////////////////////////// InsertFx GUI

WindowDef(\TrackMixer_InsertFxEditor, { arg def, insertFx;
	var paramView = View.new;
	paramView.followChange(insertFx, \synthName, {
		paramView.removeAll;
		paramView.layout = insertFx.asParamView;
	});
	VLayout (
		HLayout (
			PlayerWrapper(insertFx).asView,
			//Param({ mixer.isInsertFxEnabled(idx) }, { mixer.enableInsertFx(idx) }, ParamBoolSpec()).asButton("Fx"++idx),
			Param(Message(insertFx), \synthName, TagSpecDef(\SynthDef_InsertFx)).asPopUpMenu,
		),
		paramView
	)
});
);


///////////////////////////////
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

// END
/////////////

TrackMixerDef(\mixer)

(
// TrackMixerDef_volume can be used to mimic a mixer that control Server.volume instead
// but you can also simply do:
// trackMixerView.slider.mapParam(Param(Server.default.volume, \volume));
// trackMixerView.textfield.mapParam(Param(Server.default.volume, \volume));
// trackMixerView.proto.showSends = false;
// trackMixerView.proto.showMuteSolo = false;
ProtoClassDef(\TrackMixerDef_volume, (
	parent: ProtoClassDef(\TrackMixerDef),
	play:{},
	stop:{},
	isPlaying: true,
	key: {Server.default.name},
	refCompileString: { arg self;
		"ProtoClassDef(\\TrackMixerDef_volume)"
	},
	new: { arg self, server;
		self = ProtoClass((parent: self));
		self.server = server ? Server.default;
		self.key = self.server.name;
		self;
	},

	server: { arg self;
		Server.default;
	},

	recbus: { arg self;
		0
	},

	getParam: { arg self, key;
		case(
			{ key == \postfader or: { key == \prefader } }, {
				Param(self.server.volume, \volume, \db);
			}, {
				Param(Message(()), key); // dummy param
			}
		)
		
	},

));
);

