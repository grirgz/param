(
SynthDef(\plop, { arg out=0, amp=0.1, gate=1, pan=0, freq=200;
	var sig;
	sig = LFSaw.ar(freq);
	sig = RLPF.ar(sig, \lpfreq.kr(100), \rq.kr(0.3));
	sig = Pan2.ar(sig, pan, amp);
	sig = sig * EnvGen.kr(\adsr.kr(Env.adsr(0.01,0.1,0.8,0.1)),gate,doneAction:2);
	Out.ar(out, sig);
}, metadata:(specs:(
	rq: ControlSpec(0.001,4,\lin, 0, 1),
	lpfreq: \freq,
	freq: \freq,
))).add;

BusDef(\fx1, \audio);

Pdef(\plop, 
	Pdef(\macro, Pbind()) <> // use another Pdef to control keys present in the pattern
	Pbind(
	\instrument, \plop,
	\out, BusDef(\fx1),
	//\legato, 0.1, // key must be absent to be controlled
	\dur, 1,
	\amp, 0.2
));

Ndef(\echo, { arg freq=200, pan=0, amp=0.1;
	var sig;
	var sig1, sig2, sig3, sig4, sig5, sig6, sig7, sig8;
	var del = 0.2;
	sig = InFeedback.ar(BusDef(\fx1),2);
	sig = CombL.ar(sig!8, 1,( \delay.kr(0.01) * \delaytab.kr(1!8) ).lag(0.1),\decay.kr(1)) + sig;
	sig = sig.sum;
	//sig = EnvGen.ar(Env([1,2,1],[0.1,0.8]), Onsets.kr(),doneAction:2);
	sig.debug("sig");
	sig = sig.tanh;
});

Spec.add(\shift, ControlSpec(-800,800,\lin,0,1));
Spec.add(\dur, ControlSpec(1/16,4,\lin,0,1));
Spec.add(\legato, ControlSpec(0.01,4,\lin,0,1));
Spec.add(\adsr, XEnvSpec(ControlSpec(0.00,3,\lin)!4));
Ndef(\echo).addSpec(\delaytab, XArraySpec(ControlSpec(0.01,2,\lin)!8));
Ndef(\echo).addSpec(\delay, ControlSpec(0.0001,1,\exp,0,1));
Ndef(\echo).addSpec(\decay, ControlSpec(0,8,\lin,0,1));
Pdef(\macro).addHalo(\instrument, \plop);
Pdef(\plop).addHalo(\instrument, \plop);

// make list of params
~list = [
	[Pdef(\macro), \amp],
	[Pdef(\macro), \dur],
	[Pdef(\plop), \legato],
	[Pdef(\plop), \freq],
	[Pdef(\plop), \lpfreq],
	[Pdef(\plop), \rq],
	[Ndef(\echo), \delay],
	[Ndef(\echo), \decay],
	[Ndef(\echo), \delaytab],
	[Pdef(\plop), \adsr],
];

Pdef(\plop).play;
Ndef(\echo).play;
)

////////////////////////////////////////////////////////////
/// Example 2.1
/// Preset morphing



(
~makegui = { arg list, presets;
	var win, playout, mlayout;
	var morpher;
	win = Window.new;
	win.alwaysOnTop = true;

	playout = HLayout(*list.collect { arg param;
		var lay;
		param = param.asParam;
		lay = VLayout(
			// create a label, a knob and a value label from the param
			param.asStaticTextLabel.align_(\center),
			param.asView,
			//param.asStaticText.minWidth_(80).align_(\center),
			param.asStaticText.align_(\center),
		); 
		[lay, \stretch:1];
	});

	morpher = ParamMorpher(list, presets);

	mlayout = VLayout(
		[playout, stretch:1],
		HLayout(*
			presets.collect({ arg preset, x;
				VLayout(
					XSimpleButton.new.label_("load " ++ preset).action_({ morpher.set(x) }),
					XSimpleButton.new.label_("save " ++ preset).action_({ list.save(preset) }),
				);
			})
		),
		Slider.new.orientation_(\horizontal).mapParam(morpher)
	);

	win.layout = mlayout;
	win.front;
	
};
ParamGroupDef(\mypreset2, ~list);
~makegui.(ParamGroupDef(\mypreset2), [\preset1, \preset2, \preset4]);
Pdef(\plop).play;
Ndef(\echo).play;

);



///////////////////// Debug

ParamGroupDef(\mypreset2).clear
ParamGroupDef(\mypreset2).presets
ParamGroupDef(\mypreset2).loadArchive

(
Ndef(\plopx, { arg freq, bla, pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(freq);
	sig = Pan2.ar(sig, pan, amp);
}).play;
);

	Param([Ndef(\plopx), \bla]).get
	Param([Ndef(\plopx), \bla]).edit
	Param([Ndef(\plopx2), \bla]).edit
	Param([Ndef(\plopx2), \bla]).get

	Param([Ndef(\echo), \delay]).edit
	Param([Ndef(\echo), \delay]).get
	Param([Ndef(\echo), \delay]).set(0.2)

	Param([Pdef(\plop), \freq]).get.asCompileString
	ParamGroup(~list).edit
	ParamGroup(~list).valueList
	Pdef(\plop).get(\freq)
	Pdef(\plop).getVal(\freq)
	Pdef(\macro).set(\dur, 0.4)
	Pdef(\plop).set(\legato, 0.4)
	Pdef(\plop).envir.parent
	Pdef(\plop).set(\degree, 4)
	Pdef(\plop).envir.use({ Pdef(\plop).get(\freq).value })


	(
		~win = Window.new;
		~win.layout = HLayout(* ~list.collect { arg p; Param(p).asSlider });
		~win.front;

	)

Archive.read
Archive.global
~e = Env.adsr(0.01,0.1,0.8,0.1)
~e.curves
