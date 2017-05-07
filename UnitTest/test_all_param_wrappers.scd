
// test all wrappers
//	- Ndef
//	- Pdef
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
//	- busmap // TODO
//	- busarraymap // TODO
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
(

Ndef(\target_object, { arg freq=200, pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(\array_param.kr(400 ! 8) + \scalar_param.kr(5)).mean;
	sig = sig * EnvGen.kr(\env_param.kr(Env([0,1,0.5,0],[0.1,0.8,0.1])), Impulse.kr(1/2));
	sig = Pan2.ar(sig, pan, amp);
});

Ndef(\target_object).addSpec(\env_param, XEnvSpec(\unipolar ! 4));

SynthDef(\target_synthdef, { arg freq=200, pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(\array_param.kr(400 ! 8) + \scalar_param.kr(5)).mean;
	sig = sig * EnvGen.kr(\env_param.kr(Env([0,1,0.5,0],[0.1,0.8,0.1])), \gate.kr(1), doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(\out.kr(0), sig);
}, metadata:(specs:(
		env_param: XEnvSpec(\unipolar ! 4),
))).add;

~envspec = XEnvSpec(\unipolar ! 4);

Pdef(\target_object, Pbind(
	\instrument, \target_synthdef
));

StepEventDef(\target_object);
StepEventDef(\target_object)[\instrument] = \target_synthdef;

~steplist = StepList[10,20,30,50];

~bus = BusDef(\target_object, \control);

~dict = IdentityDictionary.new;
~dict[\scalar_param] = 10;
~dict[\array_param] = [10,20,30,10];
~dict[\env_param] = Env([0,1,0.5,0],[0.1,0.8,0.1]);
~dict.addSpec(\env_param, XEnvSpec(\unipolar!4));

~list = List[12,43,45,12];

Builder(\target_object, { arg scalar_param=10; "bla" });

~variable = 10;

////////////////

~three = { arg target;
	[\scalar_param, \array_param, \env_param].collect({ arg prop;
		Param(target, prop);
	})
};

~all_targets = [
	~three.(Ndef(\target_object)),
	~three.(Pdef(\target_object)),
	~three.(StepEventDef(\target_object)),
	~three.(~dict),
	Param(~steplist, \list),
	Param(~list, \list),
	Param(TempoClock.default, \tempo),
	Param(Ndef(\target_object), "vol"),
	//Param(~bus, \value),

	Param(Message(StepList[1,2,3]), \stepCount),
	Param(Builder(\target_object), \scalar_param),
	// TODO: add test for each spec !

	Param({ ~variable }, { arg x; ~variable = x }),
	Param(s.volume, \volume),
].flatten;

ParamGroupDef(\all_targets, ~all_targets);
~pg = ParamGroup(~all_targets);

ParamProto.init; // for preset menu

WindowDef(\testgui, {
	var layout;

	layout = VLayout (
		EventClass.presetSelectorView.(ParamGroupDef(\all_targets)),
		EventClass.presetSelectorView.(~pg),
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
			);
			HLayout(
				StaticText.new.string_(param.target.class),
				playout
			)
		}
	);
	ScrollView.new.canvas_(View.new.layout_(layout));
}).front;

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
	~test.assertEquals(Param.specFromDefaultValue(\array_param, \target_synthdef), XArraySpec(Param.defaultSpec!8));
	~test.assertEquals(Param.specFromDefaultValue(\env_param, \target_synthdef), XEnvSpec(Param.defaultSpec!8)); // Fail because no way to know it's an env

	~test.assertEquals(Param.valueToSpec(10), Param.defaultSpec);
	~test.assertEquals(Param.valueToSpec([10,10]), XArraySpec(Param.defaultSpec!2));
	~test.assertEquals(Param.valueToSpec(Env([10,10],[10])), XEnvSpec(Param.defaultSpec!2));

)


////////////////////////////// debug

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
