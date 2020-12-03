
// menu type
// - asPopUpMenu
// - asBusPopUpMenu
// - asValuePopUpMenu
// - asIndexPopUpMenu

// value type
// - scalar
// - array

// target type
// - pdef
// - ndef

// spec
// as keys
// - symbol list
// - string list
// - TagSpec
// as spec
// - TagSpec

// TODO: test ar and kr and numchannel

(
SynthDef(\test_popup, { arg out=0, amp=0.1, gate=1, pan=0, spread=0.8, freq=200,  doneAction=2;
	var sig, sig1, sig2, sig3;
	freq = freq * \freqratiotab.kr([1.01,1,0.99]) * \popup_busmap.kr(1) * \popup_bustab.kr(1!3);
	sig = SelectX.ar(\popup_index.kr(0) + \popup_indextab.kr(0!3) , [
		SinOsc.ar(freq),
		LFSaw.ar(freq),
		Pulse.ar(freq),
	]);
	sig = RLPF.ar(sig, \popup_valuetab.kr(1.1!3) * \popup_value.kr(1.1) * freq, \rq.kr(0.5));
	sig.debug("sig");
	sig = sig * EnvGen.ar(\adsr.kr(Env.adsr(0.01,0.1,0.8,0.1)),gate,doneAction:doneAction);
	sig = Splay.ar(sig, spread, amp, pan);
	Out.ar(out, sig);
}, metadata:(specs:(
	popup_bustab: ParamArraySpec( ControlSpec(0,5,\lin)!3 ),
	popup_indextab: ParamArraySpec(ControlSpec(0,3,\lin)!3),
	popup_valuetab: ParamArraySpec(ControlSpec(0,5,\lin)!3),
))).add;

SynthDef(\test_fx, { arg out=0, gate=1;
	var sig;
	var in = InFeedback.ar(\inbus.kr(BusDef(\zero, \audio)), 2); 
	sig = in;
	sig = FreqShift.ar(sig, SinOsc.kr(0.6) * 60);
	sig = SelectX.ar(\mix.kr(0.5), [in, sig]);
	sig = sig * EnvGen.kr(\adsr.kr(Env.adsr(0.1,0.1,0.8,0.1)), gate, doneAction:2);
	sig = sig * \gain.kr(1);
	Out.ar(out, sig);
}).add;

Ndef(\target_ndef).source = \test_popup;
Ndef(\lfo_ar1, { arg lofreq=1.1; SinOsc.ar(lofreq) *0.3 + 1 });
Ndef(\lfo_ar2, { arg lofreq=1.2; SinOsc.ar(lofreq * [1.1,1]) *0.3 + 1 });
Ndef(\lfo_kr1, { arg lofreq=1.3; SinOsc.kr(lofreq)*0.3  });
Ndef(\lfo_kr2, { arg lofreq=1.4; SinOsc.kr(lofreq * [1.1,1])*0.3 + 1 });

Pdef(\target_pdef, 
	Pbind(
		\instrument, \test_popup,
		\degree, Pseq([Pn(0,7),Pwhite(1,3,1),Pseg(Pwhite(0,10,1)!3,Pwhite(1/8,2))],inf),
		\dur, 1/4,
		\amp, 0.1,
	),
);

Pdef(\testtools, 
	Ppar([
		Pbind(
			\instrument, \test_fx,
			\inbus, BusDef(\test_fx, \audio, 2),
			\freq, 200,
			\dur, 1,
			\amp, 0.1,
		),
		Pbind(
			\instrument, \default,
			\out, BusDef(\test_arbus, \audio, 2),
			\freq, 200,
			\dur, 1,
			\amp, 0.1,
		),
		Pbind(
			\type, \bus,
			\out, BusDef(\testbus, \control),
			\array, Ptuple(Pwhite(1/2,2,inf)!1),
			\dur, 1,
		),
	]),
);


TagSpecDef(\test_popup_bus, [
	\testbus -> BusDef(\testbus, \control),
	\lfo_ar1 -> Ndef(\lfo_ar1),
	\lfo_ar2 -> Ndef(\lfo_ar2),
	\lfo_kr1 -> Ndef(\lfo_kr1),
	\lfo_kr2 -> Ndef(\lfo_kr2),
	\zerobus -> BusDef(\zerobus, \control),
	\test_fx -> BusDef(\test_fx, \audio, 2),
	\one -> 1,
	\zero -> 0,
	\nile -> nil,
]);
TagSpecDef(\test_popup_busmap, [
	\testbus -> BusDef(\testbus, \control).asMap,
	\zerobus -> BusDef(\zerobus, \control).asMap,
	\lfo_ar1 -> Ndef(\lfo_ar1).asMap,
	\lfo_ar2 -> Ndef(\lfo_ar2).asMap,
	\lfo_kr1 -> Ndef(\lfo_kr1).asMap,
	\lfo_kr2 -> Ndef(\lfo_kr2).asMap,
	\one -> 1,
	\zero -> 0,
	\nile -> nil,
]);
TagSpecDef(\test_popup_value, [
	\low -> 0.4,
	\mid -> 1.2,
	\high -> 2.3,
	\one -> 1,
	\zero -> 0,
	\nile -> nil,
]);
TagSpecDef(\test_popup_index, [
	\sin, \saw, \pulse,
]);

~makerow = { arg target;
	HLayout (
		Param(target, \out).asBusPopUpMenu(TagSpecDef(\test_popup_bus)),

		Param(target, \popup_index).asPopUpMenu(TagSpecDef(\test_popup_index)), // default is index
		Param(target, \popup_busmap).asBusPopUpMenu(TagSpecDef(\test_popup_busmap)),
		Param(target, \popup_value).asValuePopUpMenu(TagSpecDef(\test_popup_value)),
		Param(target, \popup_index).asIndexPopUpMenu(TagSpecDef(\test_popup_index)),


		Param(target, \popup_indextab -> 1).asPopUpMenu(TagSpecDef(\test_popup_index)), // default is index
		Param(target, \popup_bustab -> 1).asBusPopUpMenu(TagSpecDef(\test_popup_busmap)),
		Param(target, \popup_valuetab -> 1).asValuePopUpMenu(TagSpecDef(\test_popup_value)),
		Param(target, \popup_indextab -> 1).asIndexPopUpMenu(TagSpecDef(\test_popup_index)),
	)

};
WindowDef(\test_popup, {
	VLayout (
		HLayout(
			PlayerWrapper(Pdef(\testtools)),
			PlayerWrapper(Pdef(\target_pdef)),
			PlayerWrapper(Ndef(\target_ndef)),
		),
		~makerow.(Pdef(\target_pdef)),
		~makerow.(Ndef(\target_ndef)),
	)
}).front;
)


(
//////////////////////////////////// test spec update
TagSpecDef(\test_popup_bus).add(\newone -> BusDef(\newone, \control));
TagSpecDef(\test_popup_busmap).add(\newone -> BusDef(\newone, \control).asMap);
TagSpecDef(\test_popup_value).add(\newone -> 3);
TagSpecDef(\test_popup_index).add(\newone -> \cub);
)


(
///////////////////////////////////// test get set
~test_getset = { 
	var target = Pdef(\target_pdef);
	var param = Param(target, \popup_bustab );
	var param1 = Param(target, \popup_bustab -> 1 );
	var test = { arg a, b, message;
		if(a == b) {
			"OK: % (%)".format(message, a).postln;
		} {
			"ERROR: %: not equal: result: %, expected:%".format(message, a, b).postln;
		}
	};

	"================================ test_getset".postln;

	target.set(\popup_bustab, [[1,1,1]]);

	test.(param.get, [1,1,1], "get");
	test.(param.getRaw, [[1,1,1]], "getRaw");
	test.(param.getBus, [[1,1,1]], "getBus");

	test.(param1.get, 1, "slot get");
	test.(param1.getRaw, nil, "slot getRaw (can't work)");
	test.(param1.getBus, 1, "slot getBus");

	param.set([4,3,2]);
	test.(param.get, [4,3,2], "set (check set)");
	test.(param.getRaw, [[4,3,2]], "set (check raw)");
	test.(param.getBus, [4,3,2], "set (check bus)");
	param.setRaw([ [2,5,4] ]);
	test.(param.get, [2,5,4], "setRaw (check set)");
	test.(param.getRaw, [[2,5,4]], "setRaw (check raw)");
	test.(param.getBus, [2,5,4], "setRaw (check bus)");
	param.setBus([7,0,1]);
	test.(param.get, [7,0,1], "setBus (check set)");
	test.(param.getRaw, [[7,0,1]], "setBus (check raw)");
	test.(param.getBus, [7,0,1], "setBus (check bus)");

	param1.set(2);
	test.(param1.get, 2, "slot set");
	param1.setRaw(9);
	test.(param1.getRaw, nil, "slot setRaw (can't work)");
	test.(param.getRaw, [[7,2,1]], "slot setRaw (check full raw) (can't work)");
	param1.setBus(8);
	test.(param1.getBus, 8, "slot setBus");
	test.(param.getBus, [[7,8,1]], "slot setBus (check full bus)");

	target.set(\popup_bustab, nil);

	test.(param.get, [1,1,1], "nil get");
	test.(param.getRaw, nil, "nil getRaw");
	test.(param.getBus, nil, "nil getBus");

	test.(param1.get, 1, "nil slot get");
	test.(param1.getRaw, nil, "nil slot getRaw (can't work)");
	test.(param1.getBus, nil, "nil slot getBus");

	"================================ test_getset ndef".postln;
	target = Ndef(\target_ndef);
	param = Param(target, \popup_bustab );
	param1 = Param(target, \popup_bustab -> 1 );

	target.set(\popup_bustab, [1,1,1]);

	test.(param.get, [1,1,1], "get");
	test.(param.getRaw, [1,1,1], "getRaw");
	test.(param.getBus, [1,1,1], "getBus");

	test.(param1.get, 1, "slot get");
	test.(param1.getRaw, nil, "slot getRaw (can't work)");
	test.(param1.getBus, 1, "slot getBus");

	param.set([4,3,2]);
	test.(param.get, [4,3,2], "set (check set)");
	test.(param.getRaw, [4,3,2], "set (check raw)");
	test.(param.getBus, [4,3,2], "set (check bus)");
	param.setRaw([2,5,4]);
	test.(param.get, [2,5,4], "setRaw (check set)");
	test.(param.getRaw, [2,5,4], "setRaw (check raw)");
	test.(param.getBus, [2,5,4], "setRaw (check bus)");
	param.setBus([7,0,1]);
	test.(param.get, [7,0,1], "setBus (check set)");
	test.(param.getRaw, [7,0,1], "setBus (check raw)");
	test.(param.getBus, [7,0,1], "setBus (check bus)");

	param1.set(2);
	test.(param1.get, 2, "slot set");
	param1.setRaw(9);
	test.(param1.getRaw, nil, "slot setRaw (can't work)");
	test.(param.getRaw, [7,2,1], "slot setRaw (check full raw) (can't work)");
	param1.setBus(8);
	test.(param1.getBus, 8, "slot setBus");
	test.(param.getBus, [7,8,1], "slot setBus (check full bus)");

	target.set(\popup_bustab, nil);

	test.(param.get, [1,1,1], "nil get");
	test.(param.getRaw, [1,1,1], "nil getRaw (can't nil)");
	test.(param.getBus, [1,1,1], "nil getBus (can't nil)");

	test.(param1.get, 1, "nil slot get");
	test.(param1.getRaw, 1, "nil slot getRaw (can't work)");
	test.(param1.getBus, 1, "nil slot getBus");
	
};
~test_getset.()
)


///////////////////////// test with no key and no spec
// should produce errors but not abort the window

(
SynthDef(\test_popup_spec, { arg out=0, amp=0.1, gate=1, pan=0, spread=0.8, freq=200,  doneAction=2;
	var sig, sig1, sig2, sig3;
	freq = freq * \freqratiotab.kr([1.01,1,0.99]) * \popup_busmap.kr(1) * \popup_bustab.kr(1!3);
	sig = SelectX.ar(\popup_index.kr(0) + \popup_indextab.kr(0!3) , [
		SinOsc.ar(freq),
		LFSaw.ar(freq),
		Pulse.ar(freq),
	]);
	sig = RLPF.ar(sig, \popup_valuetab.kr(1.1!3) * \popup_value.kr(1.1) * freq, \rq.kr(0.5));
	sig.debug("sig");
	sig = sig * EnvGen.ar(\adsr.kr(Env.adsr(0.01,0.1,0.8,0.1)),gate,doneAction:doneAction);
	sig = Splay.ar(sig, spread, amp, pan);
	Out.ar(out, sig);
}, metadata:(specs:(
	//popup_value: TagSpecDef(\test_popup_value),
	popup_bustab: ParamArraySpec( ControlSpec(0,5,\lin)!3 ),
	popup_indextab: ParamArraySpec(ControlSpec(0,3,\lin)!3),
	popup_valuetab: ParamArraySpec(ControlSpec(0,5,\lin)!3),
))).add;

SynthDef(\test_fx, { arg out=0, gate=1;
	var sig;
	var in = InFeedback.ar(\inbus.kr(BusDef(\zero, \audio)), 2); 
	sig = in;
	sig = FreqShift.ar(sig, SinOsc.kr(0.6) * 60);
	sig = SelectX.ar(\mix.kr(0.5), [in, sig]);
	sig = sig * EnvGen.kr(\adsr.kr(Env.adsr(0.1,0.1,0.8,0.1)), gate, doneAction:2);
	sig = sig * \gain.kr(1);
	Out.ar(out, sig);
}).add;

Ndef(\target_ndef_spec).source = \test_popup_spec;
Ndef(\lfo_ar1, { arg lofreq=1.1; SinOsc.ar(lofreq) *0.3 + 1 });
Ndef(\lfo_ar2, { arg lofreq=1.2; SinOsc.ar(lofreq * [1.1,1]) *0.3 + 1 });
Ndef(\lfo_kr1, { arg lofreq=1.3; SinOsc.kr(lofreq)*0.3  });
Ndef(\lfo_kr2, { arg lofreq=1.4; SinOsc.kr(lofreq * [1.1,1])*0.3 + 1 });

Pdef(\target_pdef_spec, 
	Pbind(
		\instrument, \test_popup_spec,
		\degree, Pseq([Pn(0,7),Pwhite(1,3,1),Pseg(Pwhite(0,10,1)!3,Pwhite(1/8,2))],inf),
		\dur, 1/4,
		\amp, 0.1,
	),
);

Pdef(\testtools, 
	Ppar([
		Pbind(
			\instrument, \test_fx,
			\inbus, BusDef(\test_fx, \audio, 2),
			\freq, 200,
			\dur, 1,
			\amp, 0.1,
		),
		Pbind(
			\instrument, \default,
			\out, BusDef(\test_arbus, \audio, 2),
			\freq, 200,
			\dur, 1,
			\amp, 0.1,
		),
		Pbind(
			\type, \bus,
			\out, BusDef(\testbus, \control),
			\array, Ptuple(Pwhite(1/2,2,inf)!1),
			\dur, 1,
		),
	]),
);


TagSpecDef(\test_popup_bus, [
	\testbus -> BusDef(\testbus, \control),
	\lfo_ar1 -> Ndef(\lfo_ar1),
	\lfo_ar2 -> Ndef(\lfo_ar2),
	\lfo_kr1 -> Ndef(\lfo_kr1),
	\lfo_kr2 -> Ndef(\lfo_kr2),
	\zerobus -> BusDef(\zerobus, \control),
	\test_fx -> BusDef(\test_fx, \audio, 2),
	\one -> 1,
	\zero -> 0,
	\nile -> nil,
]);
TagSpecDef(\test_popup_busmap, [
	\testbus -> BusDef(\testbus, \control).asMap,
	\zerobus -> BusDef(\zerobus, \control).asMap,
	\lfo_ar1 -> Ndef(\lfo_ar1).asMap,
	\lfo_ar2 -> Ndef(\lfo_ar2).asMap,
	\lfo_kr1 -> Ndef(\lfo_kr1).asMap,
	\lfo_kr2 -> Ndef(\lfo_kr2).asMap,
	\one -> 1,
	\zero -> 0,
	\nile -> nil,
]);
TagSpecDef(\test_popup_value, [
	\low -> 0.4,
	\mid -> 1.2,
	\high -> 2.3,
	\one -> 1,
	\zero -> 0,
	\nile -> nil,
]);
TagSpecDef(\test_popup_index, [
	\sin, \saw, \pulse,
]);

~makerow = { arg target;
	HLayout (
		//Param(target, \out).asBusPopUpMenu,

		Param(target, \popup_index).asPopUpMenu, // default is index
		Param(target, \popup_busmap).asBusPopUpMenu,
		Param(target, \popup_value).asValuePopUpMenu,
		Param(target, \popup_index).asIndexPopUpMenu,


		Param(target, \popup_indextab -> 1).asPopUpMenu, // default is index
		Param(target, \popup_bustab -> 1).asBusPopUpMenu,
		Param(target, \popup_valuetab -> 1).asValuePopUpMenu,
		Param(target, \popup_indextab -> 1).asIndexPopUpMenu,
	)

};
WindowDef(\test_popup, {
	VLayout (
		HLayout(
			PlayerWrapper(Pdef(\testtools)),
			PlayerWrapper(Pdef(\target_pdef_spec)),
			PlayerWrapper(Ndef(\target_ndef_spec)),
		),
		~makerow.(Pdef(\target_pdef_spec)),
		//~makerow.(Ndef(\target_ndef_spec)),
	)
}).front;
)

///////////////////////////////////////////// draft

(
{
var window = Window.new;
var layout;
layout = VLayout(
		Param(Pdef(\target_pdef_spec), \popup_busmap).asBusPopUpMenu
);
window.layout = layout;
//window.alwaysOnTop = true;
window.front;
}.value
)



(
{
var window = Window.new;
var layout;
var pop = PopUpMenu.new;
var xx;
~pop = pop;
	//xx = pop.mapBusParam (
//Param(Pdef(\target_pdef_spec), \popup_busmap)
	//);
layout = VLayout(
	//pop
	//xx.dump;
	//pop.mapIndexParam  // work
	//pop.mapValueParam  // error new
	pop.mapBusParam ( // error new
Param(Pdef(\target_pdef_spec), \popup_busmap)
	);
//Param(Pdef(\target_pdef_spec), \popup_busmap).asBusPopUpMenu
	
);
	//	Param(Pdef(\target_pdef_spec), \popup_busmap).mapBusPopUpMenu(~pop);
window.layout = layout;
//window.alwaysOnTop = true;
window.front;
}.value
)

~x = 3
~x = PopUpMenu.new

Pdef(\target_pdef_spec).get(\popup_value)
Pdef(\target_pdef_spec).set(\popup_value, 1)

Param(Pdef(\target_pdef), \popup_bus).getRaw
Param(Pdef(\target_pdef), \popup_valuetab).getRaw
Param(Pdef(\target_pdef), \popup_valuetab).getBus
Param(Pdef(\target_pdef), \popup_valuetab).get
Param(Pdef(\target_pdef), \popup_bustab).get
Param(Pdef(\target_pdef), \popup_valuetab).spec
Pdef(\target_pdef).get(\popup_valuetab)
Pdef(\target_pdef).get(\popup_bus)
Pdef(\target_pdef).get(\out)
Ndef(\target_ndef).edit
Ndef(\target_ndef).play
PlayerWrapper(Ndef(\target_ndef)).play
Ndef(\target_ndef).rate
BusDef(\testbus).set(1)
BusDef(\testbus).get
Param(Pdef(\target_pdef), \popup_bustab ).getRaw
Param(Pdef(\target_pdef), \popup_bustab ).getBus
Param(Pdef(\target_pdef), \popup_bustab ).get
Param(Pdef(\target_pdef), \popup_bustab -> 1).getRaw
Param(Pdef(\target_pdef), \popup_bustab -> 1).getBus
Param(Pdef(\target_pdef), \popup_bustab -> 1).get
Param(Pdef(\target_pdef), \popup_bustab -> 1).set(2)
Param(Pdef(\target_pdef), \popup_bustab -> 1).setBus(1.32)
Param(Pdef(\target_pdef), \popup_bustab -> 1).setRaw(1.2)
Ndef(\target_ndef).stop
Ndef(\target_ndef).monitor.out
Ndef(\target_ndef).edit
NdefGui
