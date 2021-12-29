
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
			prefx: 2000,
			sends: 2100,
			postfx: 3000,
			postfader: 4000,
			recbus: 5000,
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

	inBus: { arg self;
		self.inbus;
	},

	inBus_: { arg self, val;
		self.inbus = val;
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
		self.getParam(( "send"++idx ).asSymbol);
	},

	getParam: { arg self, key;
		if(key.asString.beginsWith("send")) {
			var idx = key.asString.drop(4).asInteger;
			self.build_sender(idx);
			Param(self.proxy, key);
		} {
			Param(self.proxy, key);
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
		( options ?? { [] } ).clump(2).do { arg item, idx;
			windef.proto.perform(item.first.asSetter, item.last)
		};
		windef.asView(self);
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

ProtoTemplateDef(\TrackMixer_InsertFx, (
	// ndef style fx

	new: { arg self, proxy, index, synthName, mixerKey;
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
		self.offset + self.index;
	},

	synthName: \default,
	synthName_: { arg self, val;
		self[\synthName] = val;
		self.updateFx;
		self.changed(\synthName);
	},

	updateFx: { arg self;
		self.synthName.asCompileString.debug("updateFx");
		self[\edit].asCompileString.debug("edit function");
		if(self.synthName == \equi) {
			self.proto = ProtoTemplateDef(\TrackMixer_InsertFx_equi);
			self[\edit].asCompileString.debug("edit function XXX");
		} {
			self.proto = ProtoTemplateDef(\TrackMixer_InsertFx_SynthDef);
		};
		self[\edit].asCompileString.debug("edit function");
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
			sdc // return whole sd clone to be collect-ed
		} {
			Log(\Param).debug("InsertFx.cloneSynthDefWithIndexedArguments: no synthDef: %", self.synthName);
			nil
		}
	},


));

ProtoTemplateDef(\TrackMixer_InsertFx_SynthDef, (
	// ndef style fx

	excludedArgs: [\inbus, \in, \out, \gate],
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
			SynthDesc(self.synthName).controlNames
				.reject({ arg x; self.excludedArgs.includes(x) })
				.collect({ arg item;
					self.getParam(item)
				})
		);
	},

	getParam: { arg self, val;
		var oldval = val;
		var pa;
		if(self.excludedArgs.includes(val).not) {
			val = (val ++ self.index).asSymbol;
		};
		pa = Param(self.proxy, val, Param.toSynthDefSpec(nil, oldval, self.synthName));
		pa.default = Param.getSynthDefDefaultValue(oldval, self.synthName);
		pa;
	},


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
			var equi, equiparams;
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

			equi = EQui(nil,nil,model.proxy,nil,model.prefix);
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
WindowDef(\TrackMixerDef_fxlist, { arg me, mixer, slotCount;
	var layout;
	var num = slotCount ? 4;
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
});

);

/////////////////////////////// advanced mixer view
(
WindowDef(\TrackMixerDef_advanced, (
	showDelay: false,
	showOutbus: false,
	showMeter: true,
	showPlay: true,
	showFx: false,
	showSends: true,
	showFaderTextField: true,

	delayMode: \postdelay,
	faderMode: \postfader,
	meterMode: \recbus,

	sendCount: 8,
	fxCount: 4,

	maxWidth: 100,


	displayOptions: [\showSends, \showOutbus, \showPlay, \showFx, \showFaderTextField, \showDelay, \showMeter],

	makeFader: { arg self, mixer;
		var stack;
		var textfield;
		var slider = Slider.new.orientation_(\vertical).background_(Color.clear);
		var meterout = CompactServerMeterView(nil, 0,2, mixer[self.meterMode].value(mixer).debug("out"));
		var param = mixer.getParam(self.faderMode);

		slider.mapParam(param);
		slider.minWidth_(10);
		slider.thumbSize_(10).knobColor_(Color.clear.alpha_(1));

		if(self.showMeter) {
			stack = StackLayout(
				slider, 
				meterout.view,
			).mode_(\stackAll);
		} {
			stack = slider;
		};


		if(self.showFaderTextField) {
			textfield = TextField.new;
			textfield.mapParam(param, 5);
			VLayout (
				stack,
				textfield,
			)
		} {
			stack
		};
	},

	makeSendKnobs: { arg self, mixer;
		var lay;
		var knobs;
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
			lay;
		} {
			nil
		};
	},

	makeMixerView: { arg self, mixer;
		var delayview;
		var mixerview;
		var boxview = self.boxview;
		boxview.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(buttonNumber == 1) {
				//WindowDef(\SimpleMixerOverlayMenu).front(simpleMixer, boxview, x, y);
				WindowDef(\TrackMixerDef_advanced_OverlayMenu).front(self, view, x, y)
			};
		});
		delayview = TextField.new;
		delayview.mapParam( Param(mixer.proxy, \postdelay, ControlSpec(0,0.1,\lin)) );
		boxview.layout = VLayout ( 
			*[
				if(self.showPlay) {
					PlayerWrapper(mixer.proxy)
						.label_("%".format(mixer.name ?? mixer.key))
						.asView
						.minSize_(Point(10,18))
						.font_(Font.default.copy.size_(9))
					;
				},
				mixerview = HLayout(*[
					self.makeFader(mixer),
					self.makeSendKnobs(mixer),
				].select(_.notNil)),
				if(self.showDelay) {
					HLayout(StaticText.new.string_("postdelay:"), delayview);
				},
				if(self.showOutbus) {
					Param(Message(mixer), \outbus,TagSpecDef(\BusDef)).asPopUpMenu.minWidth_(60).addAction({ 
						mixer.play;
						"kjkj".postln
					})
				},
				if(self.showFx) {
					self.makeTinyFxView(mixer)
				},
			].select(_.notNil)
		).spacing_(5).margins_(5);
		boxview.background = Color.gray;
		boxview.maxWidth_(self.maxWidth - if(self.showSends) { 0 } { 30 });
		boxview.addUniqueMethod(\model_, { arg me, val;
			mixerview.model = val;
		});
		boxview;
	},

	refreshView: { arg self;
		self.boxview.removeAll;
		self.makeMixerView(self.model);
	},

	makeTinyFxView: { arg self, mixer;
		var layout;
		var num = self.fxCount ? 4;
		layout = HLayout(* num.collect { arg idx;
				PlayerWrapper(mixer.insertFxAt(idx)).label_(idx.asString).asView
					.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
						[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
						
						if(buttonNumber == 1) {
							//WindowDef(\SimpleMixerOverlayMenu).front(simpleMixer, boxview, x, y);
							WindowDef(\TrackMixerDef_advanced_fx_OverlayMenu).front(self, idx, view, x, y);
							true;
						};
					})
					.minSize_(Point(8,15))
					.font_(Font.default.size_(8))
				;
		} ++ [nil]).spacing_(1);
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

	asView: { arg self, def, mixer;
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
	[x, y].debug("xy");
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 300,100);
		win.endFrontAction = { win.close };
	};
	inview = View.new;

	sends = mixerview.sendCount.collect({ arg idx;
		HLayout(
			StaticText.new.string_("send %".format(idx)),
			Param(mixer.proxy, "%%".format(\sendbus, idx).asSymbol, TagSpecDef(\BusDef_object)).asPopUpMenu,
		)
	});

	layout = VLayout (
		//PlayerWrapper(mixerTemplate).asView,
		//HLayout(
			//StaticText.new.string_("Track count:"),
			//Param(Message(mixerTemplate), \trackCount, ControlSpec(1,16,\lin,1)).asNumberBox,
		//),
		WindowDef(\TrackMixerDef_fxlist).asView(mixer),
		HLayout (
			* mixerview.displayOptions.collect { arg item, idx;
				Param(Message(mixerview), item, ParamBoolSpec()).asButton;
			};
		),
		VLayout(
			*sends
		),
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
	[x, y].debug("xy");
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

/////////////
// END

TrackMixerDef(\mixer)
