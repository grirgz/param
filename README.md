# param

Param Quark for SuperCollider

The goal of this quark is to ease the controlling of sounds objects parameters (Ndef, Pdef, etc) using GUI, MIDI, and OSC.

(
Ndef(\ndef_scalar, { arg freq=200, pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(freq);
	sig = Pan2.ar(sig, pan, amp);
}).play;
);

// create a reference to a parameter of a Ndef
~p = Param(Ndef(\ndef_scalar), \freq, \freq.asSpec);

// now the slider control the Ndef parameter \freq
Slider.new.mapParam(~p);

// now the midi control number 16 controls the Ndef parameter \freq
MIDIMap([16], ~p); 

// if you have a GUI with 8 sliders, you can simply send it the list of parameters you want to control:
~mygui.set_params( [
	[Pdef(\plop), \freq],
	[Pdef(\plop), \lpfreq],
	[Pdef(\plop), \rq],
	[Ndef(\echo), \shift1],
	[Ndef(\echo), \shift2],
] )

The benefit is that is the same API for controlling Ndef, Pdef, Synth, Bus, ... and you can easily control arrays and enveloppes in the same way. Setting .action and updating of GUI, and freeing resources like MIDIFunc and buses are done for you in the background.

This is the core idead of this quark, but it (will) have some other nices features, like presets and preset morphing.
