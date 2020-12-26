
(
Ndef(\zed, { arg pan=0, amp=0.1, freqratio=1, ratio=0.9, rel=0.1, impfreq=1;
	var sig;
	var fun = { arg x; 
		SinOsc.ar(rrand(0.1,1000.0) * freqratio * ( x * ratio + 1 ) ) 
		* EnvGen.kr(Env([0,1,0],[0.1,rel]), Impulse.kr(rrand(1/8,8) * impfreq));
	};
	sig = DC.ar(0);
	16.do {
		sig = fun.(sig);
	};
	sig = Pan2.ar(sig, pan, amp);
}).play;
Ndef(\zed).addSpec(\rel, ControlSpec(0.01,2,\lin), \ratio, ControlSpec(0.01,20,\lin))
);
Ndef(\zed).edit
Ndef(\zed).send

Ndef(\zed2, Ndef(\zed))
(
WindowDef(\multi, {
	arg def, player;
	HLayout(
		* 4.collect { arg idx;
			VLayout(
				* 4.collect {
					arg idx2;
					WindowDef(\meter).asView(Ndef(\player+++idx+++idx2).source_(player.source) )
				}
			)
		}
	)
}).front(Ndef(\zed))
)

(
WindowDef(\meter, { arg def, player = Ndef(\zed);
	VLayout (
		HLayout (
			PlayerWrapper(player),
			Button.new.string_("Rebuild").action_({
				player.source = player.source
			})
		),
		HLayout (
			player.asParamGroup.asView,
			SimpleServerMeterView(nil,0,2,player.bus).asView
		)
	)
}).front;
);

(
WindowDef(\MeterFader, { arg def, bus;
	var meterin = SimpleServerMeterView(nil, 0,2, bus ? 0);
	var slider = Slider.new.orientation_(\vertical).background_(Color.clear);
	StackLayout(
		slider, 
		meterin.view,
	).mode_(\stackAll)
		.addUniqueMethod(\slider, { slider })
		.addUniqueMethod(\meter, { meterin });
}).front;
)


