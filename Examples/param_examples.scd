
/// make some controllable sound
(
SynthDef(\plop, { arg out=0, amp=0.1, gate=1, pan=0, freq=200;
	var sig;
	sig = LFSaw.ar(freq);
	sig = RLPF.ar(sig, \lpfreq.kr(100), \rq.kr(0.3));
	sig = sig * EnvGen.ar(Env.adsr(0.01,0.1,0.8,0.1),gate,doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(out, sig);
}, metadata:(specs:(
	rq: ControlSpec(0.001,4,\lin, 0, 1),
	lpfreq: \freq,
	freq: \freq,
))).add;

BusDef(\fx1, \audio);

Pdef(\plop, Pbind(
	\instrument, \plop,
	\out, BusDef(\fx1),
	\legato, 0.1,
	\dur, 1,
	\amp, 0.6
));

Ndef(\echo, { arg freq=200, pan=0, amp=0.1;
	var sig;
	var sig1, sig2, sig3, sig4, sig5, sig6, sig7, sig8;
	var del = 0.2;
	sig = InFeedback.ar(BusDef(\fx1),2);
	sig1 = DelayL.ar(sig, 1,del);
	sig1 = FreqShift.ar(sig1, \shift1.kr(100));
	sig2 = DelayL.ar(sig1, 1,del);
	sig2 = FreqShift.ar(sig2, \shift2.kr(300));
	sig = [sig, sig1, sig2].mean;
});

Spec.add(\shift, ControlSpec(-800,800,\lin,0,1));
Ndef(\echo).addSpec(\shift1, \shift.asSpec);
Ndef(\echo).addSpec(\shift2, \shift.asSpec);

// make list of params
~list = [
	[Pdef(\plop), \freq],
	[Pdef(\plop), \lpfreq],
	[Pdef(\plop), \rq],
	[Ndef(\echo), \shift1],
	[Ndef(\echo), \shift2],
];

);

////////////////////////////////////////////////////////////
/// Example 1
/// Simple gui creation

(
~makegui = { arg fun;
	var win;
	var widget;
	win = Window.new;
	win.layout = VLayout.new(*ParamGroup(~list).collect(_.asView)); // automatic view creation
	win.alwaysOnTop = true;
	win.front;
};
~makegui.();
Pdef(\plop).play;
Ndef(\echo).play;
)


////////////////////////////////////////////////////////////
/// Example 2
/// More complete example with labels, values and preset choosing



(
~makegui = { arg list;
	var win, playout, mlayout;
	win = Window.new;
	win.alwaysOnTop = true;

	playout = HLayout(*list.collect { arg param;
		var lay;
		param = param.asParam;
		lay = VLayout(
			// create a label, a knob and a value label from the param
			param.asStaticTextLabel.align_(\center),
			param.asKnob,
			param.asStaticText.minWidth_(80).align_(\center),
		); 
		[lay, \stretch:1];
	});

	mlayout = VLayout(
		playout,
		HLayout(
			VLayout(
				XSimpleButton.new.label_("load preset1").action_({ list.loadPreset(\preset1) }),
				XSimpleButton.new.label_("save preset1").action_({ list.savePreset(\preset1) }),
			),
			VLayout(
				XSimpleButton.new.label_("load preset2").action_({ list.loadPreset(\preset2) }),
				XSimpleButton.new.label_("save preset2").action_({ list.savePreset(\preset2) }),
			),
		)
	);

	win.layout = mlayout;
	win.front;
	
};
ParamGroupDef(\mypreset, ~list);
~makegui.(ParamGroupDef(\mypreset));
Pdef(\plop).play;
Ndef(\echo).play;

);


////////////////////////////////////////////////////////////
/// Exemple 3
/// Hot swapping params

(
~makegui = {
	var win, playout, mlayout;
	var size = 8;
	var list = List.new; // keep a reference to views to be able to map them
	win = Window.new;
	win.alwaysOnTop = true;

	playout = HLayout(*8.collect {
		var lay;
		var label, slider, val;
		lay = VLayout(
			// here the view is created in the usual way
			[label = StaticText.new.fixedHeight_(40), align:\center],
			slider = Slider.new,
			[val = StaticText.new.minWidth_(80), align:\center],
		); 
		list.add([label, slider, val]);
		[lay, \stretch:1];
	});

	~set_params = { arg params;
		params = ParamGroup(params); // transform a list of list in a list of params
		8.do { arg x;
			var pa = params[x];
			if(pa.notNil) {
				// map the views to a param
				list[x][0].mapParamLabel(pa); // label
				list[x][1].mapParam(pa); // slider
				list[x][2].mapParam(pa); // value
			} {
				list[x].do { arg view;
					view.unmapParam; // no param, clear this slider
				}
			}
		}
	};

	win.layout = playout;
	win.front;
	
};
~makegui.();
Pdef(\plop).play;
Ndef(\echo).play;

);

// set some params
(
~set_params.([
	[Pdef(\plop), \freq],
	[Pdef(\plop), \lpfreq],
	[Pdef(\plop), \rq],
	[Ndef(\echo), \shift1],
	[Ndef(\echo), \shift2],
]);
)

// replace by others
(
~set_params.([
	[Ndef(\echo), \shift1],
	[Ndef(\echo), \shift2],
]);
)


////////////////////////////////////////////////////////////
/// Exemple 4
/// Each slider in the view match a slider on your midi controller and show the current mapped param

(
~makegui = { arg list;
	// list is now a list of midi controls
	var win, playout, mlayout;
	var size = 8;
	win = Window.new;
	win.alwaysOnTop = true;

	playout = HLayout(*list.collect { arg midipath;
		var lay;
		var label, slider, val;
		lay = VLayout(
			// here the view is created in the usual way
			[label = StaticText.new.fixedHeight_(40), align:\center],
			slider = Slider.new,
			[val = StaticText.new.minWidth_(80), align:\center],
		); 
		// now map the views on the param controlled by midi controller with ccnum midipath
		MIDIMap.mapStaticTextLabel(midipath, label);
		MIDIMap.mapView(midipath, slider);
		MIDIMap.mapView(midipath, val);
		[lay, \stretch:1];
	});

	win.layout = playout;
	win.front;
	
};
Pdef(\plop).play;
Ndef(\echo).play;

);

MIDIdef.cc(\arf).learn; // find some midi cc
MIDIdef.cc(\arf).free;

(
// put the midi cc here
~makegui.([
	//[19, 0, \noteOn, 1451154], // format: [msgNum, channel, msgType=\control, uid]
	[16], // ccnum 16, every channel and every uid
	[17], 
	[18],
]);
)

(
// if you have 4 knobs (\k1, \k2, ..) and 4 sliders (\s1, \s2, ..) defined in MIDIMap
~makegui.(
	//[\k, \s].collect { arg pre; 4.collect{ arg x; ( pre++(x+1) ).asSymbol }}.flat
	[ 'k1', 'k2', 'k3', 'k4', 's1', 's2', 's3', 's4'  ]
);
)


// map some midi control to params 
MIDIMap([16], Param(Pdef(\plop), \freq));
MIDIMap.free([16]); // unmap it
MIDIMap([17], Param(Pdef(\plop), \lpfreq));
MIDIMap([16], Param(Ndef(\echo), \shift1)); // replace param with another


(
// the gui know what is already mapped
~makegui.([
	[16], // ccnum 16, every channel and uid
	[17], 
	[18],
	//[19, 0, \noteOn, 1451154], // [msgNum, channel, msgType, uid]
]);
)

MIDIMap.freeAll // free all mappings // FIXME: don't work

(
// define convenient names for your midi controls
MIDIMap.define(
	nil, // channel (or [channel, uid])
	[
		\k1, 16, // my first knob is now named k1
		\k2, 17,
		\k3, 18,
	]
)
)

MIDIMap(\k1, Param([Pdef(\plop), \freq])); // now mapping can be done by name
MIDIMap.free(\k1);

////////////////////////////////////////////////////////////

