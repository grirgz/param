
// test all wrappers
//	- Ndef
//	- Pdef
//	- EventPatternProxy
//	- NodeProxy
//	- StepEvent
//	- StepList
//	- ProtoClass
//	- IdentityDictionary
//	- Volume
//	- Bus
//	- TempoClock
//	- setter/getter
// test all types
//	- array
//	- env
//	- scalar
//	- bool
//	- menu
//	- bus
//	- buffer
//	- busmap // TODO (both mapBusPopUpMenu and mapValuePopUpMenu)
//	- busarraymap // TODO
//  - stepseq // TODO
// test all gui
//	- button
//	- knob
//	- slider
//	- popup
//	- envview
//	- multislider
//	- paramlayout
// test all path for spec
// test all path for default
// test all subparam
// test all busmode
// TODO: test if target value is nil
(
s.boot;

/////////////////////// define target objects

Ndef(\target_object, { arg freq=200, pan=0, amp=0.1;
	var sig;
	var fm;
	fm = \mapped_audio_bus.ar * \mapped_audio_bus_array.ar(0!3).mean * \modfreq_scale.kr(1) + InFeedback.ar(\audio_bus.kr, 1) + 1;
	sig = SinOsc.ar(\array_param.kr(400 ! 8) + \scalar_param.kr(5) + \scalar_bus_param.kr(5) * fm).mean;
	sig = sig * EnvGen.kr(\env_param.kr(Env([0,1,0.5,0],[0.1,0.8,0.1])), Impulse.kr(1/2));
	sig = Pan2.ar(sig, pan, amp);
});

Ndef(\target_object).addSpec(\env_param, ParamEnvSpec(\unipolar ! 4));
Ndef(\target_object).addSpec(\mapped_audio_bus, ParamMappedBusSpec());
Ndef(\target_object).addSpec(\mapped_audio_bus_array, ParamArraySpec(ParamMappedBusSpec()));
Ndef(\target_object).addSpec(\audio_bus, ParamBusSpec());

SynthDef(\target_synthdef, { arg freq=200, pan=0, amp=0.1;
	var sig;
	var fm;
	fm = \mapped_audio_bus.ar * \mapped_audio_bus_array.ar(0!3).mean * \modfreq_scale.kr(1) + InFeedback.ar(\audio_bus.kr, 1) + 1;
	sig = LFSaw.ar(\array_param.kr(400 ! 8) + \scalar_param.kr(5) + \scalar_bus_param.kr(5) * fm).mean;
	sig = RLPF.ar(sig, \lpfr.kr(1.1) * freq , \rq.kr(0.5));
	sig = sig * EnvGen.kr(\env_param.kr(Env([0,1,0.5,0],[0.1,0.8,0.1])), \gate.kr(1), doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(\out.kr(0), sig);
}, metadata:(specs:(
	audio_bus: ParamBusSpec(),
	mapped_audio_bus: ParamMappedBusSpec(),
	mapped_audio_bus_array: ParamArraySpec(ParamMappedBusSpec()),
	env_param: ParamEnvSpec(\unipolar ! 4),
	lpfr: ControlSpec(0.1,6,\lin),
	//array_param: ParamArraySpec(), // FIXME: this should be commented, Param should detect spec from default value, but bug in 3.11
))).add;

~envspec = ParamEnvSpec(\unipolar ! 4);

Pdefn(\lpfr_pdefn, PstepSeq([1,2,2.3,0.4],inf));
Pdefn(\rq_pdefn, 0.3);

Pdef(\target_object, Pbind(
	\instrument, \target_synthdef,
	\lpfr, Pdefn(\lpfr_pdefn),
	\rq, Pdefn(\rq_pdefn),
));

Pbindef(\target_pbindef, 
	\instrument, \target_synthdef,
	\lpfr, PstepSeq([1,0,3,6],inf),
	\scalar_param, Pseq([0,100,200,0],inf),
	\rq, 0.1,
);

~eventPatternProxy = EventPatternProxy.new.source_(
	Pbind(
		\instrument, \target_synthdef,
		\dur, 1,
		\amp, 0.1,
	),
);

~nodeProxy = NodeProxy.new.source_(Ndef(\target_object).source);
~nodeProxy.addSpec(\mapped_audio_bus, ParamMappedBusSpec());

(
Ndef(\modulator, { arg freq=200, pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(2);
}).play(BusDef(\modbus_audio_mono, \audio, 1));
);

StepEventDef(\target_object);
StepEventDef(\target_object)[\instrument] = \target_synthdef;

~steplist = StepList[10,20,30,50];

~bus = BusDef(\target_object, \control);

~dict = IdentityDictionary.new;
~dict[\scalar_param] = 10;
~dict[\array_param] = [10,20,30,10];
~dict[\env_param] = Env([0,1,0.5,0],[0.1,0.8,0.1]);
~dict.addSpec(\env_param, ParamEnvSpec(\unipolar!4));

~list = List[12,43,45,12];

Builder(\target_object, { arg def, scalar_param=10; "bla" });

~variable = 10;

///////////////////////// define list of params to test

~three = { arg target;
	[\scalar_param, \array_param, \env_param].collect({ arg prop;
		Param(target, prop);
	})
};

~env_individual_params = { arg target;
	[
		Param(target, \env_param -> \times -> 0),
		Param(target, \env_param).at(\times, 0),
		Param(target, \env_param -> \times -> 1),
		Param(target, \env_param -> \times -> 2),
		Param(target, \env_param -> \levels -> 0),
		Param(target, \env_param).at(\levels, 0),
		Param(target, \env_param -> \levels -> 1),
		Param(target, \env_param -> \levels -> 2),
		Param(target, \env_param -> \levels -> 3),
	]
};

~array_individual_params = { arg target;
	[
		Param(target, \array_param -> 0),
		Param(target, \array_param).at(0),
		Param(target, \array_param -> 1),
	]
};

Param.trace = true;
//Param.trace = false;

~all_targets = [
	// Ndef
	~three.(Ndef(\target_object)),
	~env_individual_params.(Ndef(\target_object)),
	~array_individual_params.(Ndef(\target_object)),
	Param(Ndef(\target_object), "vol"), // no gui update
	Param(Ndef(\target_object), \scalar_bus_param).setBusMode, // bus mode for Pdef
	Param(Ndef(\target_object), \inexistant),
	Param(Ndef(\target_object), \mapped_audio_bus),
	Param(Ndef(\target_object), \audio_bus),
	Param(Ndef(\target_object), \mapped_audio_bus_array).at(0),

	// NodeProxy
	Param(~nodeProxy, \mapped_audio_bus),
	Param(~nodeProxy, \audio_bus),

	// Pdef
	~three.(Pdef(\target_object)),
	~env_individual_params.(Pdef(\target_object)),
	~array_individual_params.(Pdef(\target_object)),
	Param(Pdef(\target_object), \scalar_bus_param).setBusMode, // bus mode for Pdef
	Param(Pdef(\target_object), \mapped_audio_bus),
	Param(Pdef(\target_object), \inexistant),
	Param(Pdef(\target_object), \audio_bus),

	// Pbindef
	Param(Pbindef(\target_pbindef), \lpfr -> \stepseq),
	Param(Pbindef(\target_pbindef), \scalar_param -> \stepseq),
	Param(Pbindef(\target_pbindef), \array_param -> \source),
	Param(Pbindef(\target_pbindef), \env_param -> \source),
	Param(Pbindef(\target_pbindef), \rq),
	Param(Pbindef(\target_pbindef), \rq -> \source).label_("rq source"),
	Param(Pbindef(\target_pbindef).source.at(\rq)).label_("rq proxy"), // no update
	Param(Pbindef(\target_pbindef), \mapped_audio_bus -> \source),

	// EventPatternProxy
	Param(~eventPatternProxy, \mapped_audio_bus),
	Param(~eventPatternProxy, \audio_bus),

	// Pdefn
	Param(Pdefn(\lpfr_pdefn), \stepseq), // no update
	Param(Pdefn(\rq_pdefn)),


	// StepEventDef
	~three.(StepEventDef(\target_object)),

	// IdentityDictionary
	~three.(~dict),

	// StepList
	Param(~steplist, \list), // this bug preset
	Param(Message(StepList[1,2,3]), \stepCount),

	// List
	Param(~list, \list), // this bug preset
	//Param(~list, 0), // this don't work

	// TempoClock
	Param(TempoClock.default, \tempo),

	// BusDef / Bus
	//Param(~bus, \value), // this is not working yet


	// Event
	Param(Message((arr:[3,2,1,2])), \arr),
	Param(Message((arr:[3,2,1,2])), \arr -> 1),
	Param(Message(()), \inexistant, ControlSpec(0,1,\lin)),
	Param(Message(()), \inexistant, XBoolSpec()),
	Param(Message(()), \inexistant),

	// Builder
	// 	TODO: add test for each spec !
	Param(Builder(\target_object), \scalar_param),

	// get/set
	Param({ ~variable }, { arg x; ~variable = x }), // no gui update

	// Volume
	Param(s.volume, \volume),

].flatten;

ParamGroupDef(\all_targets, ~all_targets);
~pg = ParamGroup(~all_targets);

ParamProto.init; // for preset menu

/////////////////////// display every param

WindowDef(\testgui, {
	var layout;
	var layoutAs;
	var layoutMap;
	var viewList = List.new;

	layoutAs = VLayout (
		* ~all_targets.collect { arg param;
			var playout;
			param.debug("=============================================================== TESTING param");
			playout = switch(param.type, 
				\scalar, {
					HLayout(*
						[\asStaticTextLabel, \asStaticText, \asTextField, \asNumberBox, \asKnob, \asSlider, \asButton, \asView].collect({ arg method;
							param.perform(method);
						})
					)

				},
				\array, {
					HLayout(*
						[\asStaticTextLabel, \asStaticText, \asTextField, \asView].collect({ arg method;
							param.perform(method);
						})
					)
				},
				\env, {
					HLayout(*
						[\asStaticTextLabel, \asStaticText, \asTextField, \asView].collect({ arg method;
							param.perform(method);
						})
					)

				},
				\other, {
					HLayout(*
						[ param.asStaticTextLabel ] ++
						if(param.spec.isKindOf(ParamBusSpec) or: {param.spec.isKindOf(TagSpec)}) {
							[\asPopUpMenu, \asBusPopUpMenu, \asValuePopUpMenu].collect({ arg method;
								param.perform(method);
							})
						} {
							[\asButton].collect({ arg method;
								param.perform(method);
							})
						};
					)

				},
			);
			HLayout(
				StaticText.new.string_(param.target.class),
				playout
			)
		}
	);
	/*
	layoutMap = VLayout (
		PlayerWrapper(Ndef(\target_object)).asView,
		EventClass.presetSelectorView.(ParamGroupDef(\all_targets)),
		EventClass.presetSelectorView.(~pg),
		* ~all_targets.collect { arg param;
			var playout;
			param.debug("=============================================================== TESTING param");
			playout = switch(param.type, 
				\scalar, {
					HLayout(*
						viewList.addAll([\StaticText, \TextField, \NumberBox, \Knob, \Slider, \sButton].collect({ arg gui;
							gui.new.mapParam(param);
						})
						++ [
							StaticText.new.mapParamLabel(param)
						])
					)

				},
				\array, {
					HLayout(*
						[\StaticText, \TextField, \MultiSliderView].collect({ arg method;
							gui.new.mapParam(param);
						})
						++ [
							StaticText.new.mapParamLabel(param)
						]
					)
				},
				\env, {
					HLayout(*
						[\StaticText, \TextField, \EnvelopeView, \XEnvelopeView].collect({ arg method;
							gui.new.mapParam(param);
						})
						++ [
							StaticText.new.mapParamLabel(param)
						]
					)

				},
			);
			HLayout(
				StaticText.new.string_(param.target.class),
				playout
			)
		}
	);
	*/
	layout = VLayout(
		layoutAs,
		//layoutMap, // TODO
	);
	VLayout(
		//HLayout (
			//BasicButton.new.string_("Map param").action_({
			
			//}),
			//BasicButton.new.string_("Unmap param").action_({
			
			//}),
		//),
		HLayout (
			StaticText.new.string_("Ndef"), PlayerWrapper(Ndef(\target_object)).asView,
			StaticText.new.string_("Pdef"), PlayerWrapper(Pdef(\target_object)).asView,
			StaticText.new.string_("EventPat"), PlayerWrapper(~eventPatternProxy).asView,
			StaticText.new.string_("NodeProxy"), PlayerWrapper(~nodeProxy).asView,
			PlayerWrapper(Pdef(\target_pbindef)).asView,
			EventClass.presetSelectorView.(ParamGroupDef(\all_targets)),
			EventClass.presetSelectorView.(~pg),
		),
		ScrollView.new.canvas_(View.new.layout_(layout)),
	)
}) .front
;

);

(
	ParamGroupDef(\all_targets).group.presetCompileString
)


(
	// static methods
	~test = UnitTest.new;
	~test.assertEquals(Param.getSynthDefSpec(\scalar_param, \target_synthdef), nil);
	~test.assertEquals(Param.getSynthDefSpec(\array_param, \target_synthdef), nil);
	~test.assertEquals(Param.getSynthDefSpec(\env_param, \target_synthdef), ~envspec);

	~test.assertEquals(Param.getSynthDefDefaultValue(\scalar_param, \target_synthdef), 5);
	~test.assertEquals(Param.getSynthDefDefaultValue(\array_param, \target_synthdef), 400 ! 8);
	~test.assertEquals(Param.getSynthDefDefaultValue(\env_param, \target_synthdef).asEnv.asCompileString, "Env([ 0.0, 1.0, 0.5, 0.0 ], [ 0.10000000149012, 0.80000001192093, 0.10000000149012 ], [ 0.0, 0.0, 0.0 ])");

	~test.assertEquals(Param.specFromDefaultValue(\scalar_param, \target_synthdef), Param.defaultSpec);
	~test.assertEquals(Param.specFromDefaultValue(\array_param, \target_synthdef), ParamArraySpec(Param.defaultSpec!8));
	~test.assertEquals(Param.specFromDefaultValue(\env_param, \target_synthdef), ParamEnvSpec(Param.defaultSpec!8)); // Fail because no way to know it's an env

	~test.assertEquals(Param.valueToSpec(10), Param.defaultSpec);
	~test.assertEquals(Param.valueToSpec([10,10]), ParamArraySpec(Param.defaultSpec!2));
	~test.assertEquals(Param.valueToSpec(Env([10,10],[10])), ParamEnvSpec(Param.defaultSpec!2));

)


////////////////////////////// debug

Param(Pdef('target_object'), 'array_param').edit
Param(Pdef('target_object'), 'array_param').spec
Param(Pdef('target_object'), 'array_param').default
PdefParam.toSpec(nil, Pdef(\target_object), \array_param)
Pdef('target_object').getSpec('array_param')
PdefParam.instrument(Pdef('target_object'))
Param.getSynthDefSpec(\array_param, \target_synthdef)
		SynthDescLib.global.synthDescs[\target_synthdef].metadata.specs[\array_param];
		SynthDescLib.global.synthDescs[\target_synthdef].metadata.specs[\out];
		SynthDescLib.global.synthDescs[\target_synthdef].metadata.specs.keys;
'array_param'.asSpec
Param.specFromDefaultValue(\array_param, \target_synthdef)
Param.getSynthDefDefaultValue(\array_param, \target_synthdef)
Param.defaultSpec


Param(Pdef('target_object'), 'env_param').get.asCompileString
Param(Pdef('target_object'), 'env_param').edit
Param(StepEventDef('target_object'), 'env_param').spec
Param(Pdef('target_object'), 'env_param').spec
Pdef(\target_object).get(\env_param)
Pdef(\target_object).set(\env_param, nil)
Pdef(\target_object).set(\env_param, nil)
Pdef(\target_object).get(\array_param)
Ndef(\target_object).get(\array_param)
Ndef(\target_object).set(\array_param)
Param(Ndef('target_object'), 'array_param').spec
Param(Ndef('target_object'), 'array_param').get
Param(Ndef('target_object'), 'array_param' -> 0).get
Param(Ndef('target_object'), 'array_param' -> 0).spec
Param(Ndef('target_object'), 'array_param').at(0).wrapper
Param(Ndef('target_object'), 'array_param').wrapper
Param(Pdef('target_object'), 'array_param').wrapper
Param(Pdef('target_object'), 'array_param').get
Param(Pdef('target_object'), 'array_param').spec
Param(Pdef('target_object'), 'scalar_param').asPopUpMenu
Param(Pdef('target_object'), 'out', XBusSpec()).type
Param(BusDef(\target_object, \control), 'value').get
Env([10,10,12],[10,1]).size
Param({ ~variable }, { arg x; ~variable = x })
~variable
Param(~bus, \value).type
bus
~bb = Bus.control
~bb.type

	Param(Message(()), \inexistant, XBoolSpec()).type

	Param()~three.(Pdef(\target_object)),

	Param(Pdef(\target_object), \mapped_audio_bus).edit
	Param(Pdef(\target_object), \mapped_audio_bus).setBusMode = false
	Param(Pdef(\target_object), \mapped_audio_bus).wrapper
	Param(Pdef(\target_object), \mapped_audio_bus).accessor
	Param(Pdef(\target_object), \mapped_audio_bus).accessor.getval
	Param(Pdef(\target_object), \mapped_audio_bus).get
	Param(Pdef(\target_object), \mapped_audio_bus).spec.tagSpec
	Param(~eventPatternProxy, \mapped_audio_bus).get
	Param(~eventPatternProxy, \mapped_audio_bus).wrapper
	Param(~eventPatternProxy, \mapped_audio_bus).inBusMode

	Param(Ndef(\target_object), \mapped_audio_bus).spec
	Param(Ndef(\target_object), \audio_bus).spec
	Param(Ndef(\target_object), \audio_bus).type



Param(Ndef(\target_object), \mapped_audio_bus_array).at(0).accessor
Param(Ndef(\target_object), \mapped_audio_bus_array).get
Param(Ndef(\target_object), \mapped_audio_bus_array).inRawMode
Param(Ndef(\target_object), \mapped_audio_bus_array).spec
Param(Ndef(\target_object), \mapped_audio_bus_array).at(0).spec
Param(Ndef(\target_object), \mapped_audio_bus_array).at(0).parent.get
Param(Ndef(\target_object), \mapped_audio_bus_array).at(0).inRawMode

(
WindowDef(\win, { arg def;
	
Param(Ndef(\target_object), \mapped_audio_bus_array).at(0).asPopUpMenu
}).front;
);

~list
Param(~list).get
Param(~list).set([1,2])
Param(~list, 0, \lofreq).get
Param(~list, 0, \lofreq).setBus(0)
