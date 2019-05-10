(

Ndef(\target_ndef, { arg freq=200, pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(\array_param.kr(400 ! 8) + \scalar_param.kr(5)).mean;
	sig = sig * EnvGen.kr(\env_param.kr(Env([0,1,0.5,0],[0.1,0.8,0.1])), Impulse.kr(1/2));
	sig = Pan2.ar(sig, pan, amp);
});

Ndef(\target_ndef).addSpec(\env_param, XEnvSpec(\unipolar ! 4));

SynthDef(\target_synthdef, { arg freq=200, pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(\array_param.kr(500 ! 8) + \scalar_param.kr(5)).mean;
	sig = sig * EnvGen.kr(\env_param.kr(Env([0,1,0.5,0],[0.1,0.8,0.1])), \gate.kr(1), doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(\out.kr(0), sig);
}, metadata:(specs:(
		env_param: XEnvSpec(\unipolar ! 4),
		scalar_param: ControlSpec(1,10,\lin),
))).add;


Pdef(\target_pdef, Pbind(
	\instrument, \target_synthdef
));

TrackDef(\target_object, (
	play: { arg self;
		// TODO
		
	},
));

~all_players = [
	PlayerWrapper(Pdef(\target_pdef)),
	PlayerWrapper(Ndef(\target_ndef)),
	PlayerWrapper(NodeProxy.new.source_({ SinOsc.ar(200)!2 })),
	PlayerWrapper(EventPatternProxy.new.source_(Pbind(\instrument, \default))),
	PlayerWrapper(Param(Ndef(\target_ndef), \scalar_param)),
];

WindowDef(\testplayergui, {
	VLayout(*
		~all_players.collect({ arg player;
			HLayout(
				StaticText.new.string_(player.label),
				StaticText.new.string_(player.targetClass),
				StaticText.new.string_(player.key),
				StaticText.new.string_(player.asCompileString),
				player.asView,
			)
		})
		++ [
			Param(Ndef(\target_ndef), \scalar_param).asTextField
		]
	)
}).front;

)

// TODO: test
// - play now
// - play with latency
// - play with quant
// - find other methods of playerwrapper and test them




/////////// play time
/// Pdef

(
Pdef(\pdeftester, 
	Pbind(
		\instrument, \default,
		\degree, Pseq([10],inf),
		\dur, 1,
		\amp, 0.1,
	)
);
);
Pdef(\pdeftester).quant = 4;
PlayerWrapper(Pdef(\pdeftester)).play; // play with quant
PlayerWrapper(Pdef(\pdeftester)).stop; // stop with quant

// PASS
(
	
PlayerWrapper(Pdef(\pdeftester)).playNow; // play with no quant (latency is added because it's a pattern)
~compare = Pbind().play;
)
(
PlayerWrapper(Pdef(\pdeftester)).stopNow; // stop with no quant (but don't cut playing events)
~compare.stop;
)
(
PlayerWrapper(Pdef(\pdeftester)).stop; // stop with quant (and don't cut playing events)
~compare.stop;
)


/// Ndef

	(
	Ndef(\ndeftester, { arg freq=200, pan=0, amp=0.1;
		var sig;
		sig = SinOsc.ar(freq);
		sig = Pan2.ar(sig, pan, amp);
	});
	);
Ndef(\ndeftester).quant = 4;
PlayerWrapper(Ndef(\ndeftester)).play; // play with quant
PlayerWrapper(Ndef(\ndeftester)).stop; // stop with quant

// FAIL: often late messages (why ??), Ndef seems always later than pattern
(
	
PlayerWrapper(Ndef(\ndeftester)).playNow; // play with no quant (latency is added because it's a pattern)
~compare = Pbind().play;
)

(
Ndef(\ndeftester).quant = 0;	
Ndef(\ndeftester).play; // play with no quant (latency is added because it's a pattern)
~compare = Pbind().play;
)
Ndef(\ndeftester).quant = 4;	
(
PlayerWrapper(Ndef(\ndeftester)).stopNow; // stop with no quant (but don't cut playing events)
~compare.stop;
)
(
PlayerWrapper(Ndef(\ndeftester)).stop; // stop with quant (and don't cut playing events)
~compare.stop;
)

