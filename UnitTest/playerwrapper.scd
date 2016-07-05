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


Pdef(\target_object, Pbind(
	\instrument, \target_synthdef
));

TrackDef(\target_object, (
	play: { arg self;
		// TODO
		
	},
))

~all_players = [
	PlayerWrapper(Pdef(\target_object)),
	PlayerWrapper(Ndef(\target_object)),
	PlayerWrapper(NodeProxy.new.source_({ SinOsc.ar(200)!2 })),
	PlayerWrapper(EventPatternProxy.new.source_({ Pbind(\instrument, \default) })),
	PlayerWrapper(Param(Ndef(\target_object), \scalar_param)),
];

WindowDef(\testplayergui, {
	VLayout(*
		~all_players.collect({ arg player;
			player.asView;
		})
	)
}).front;

)

