# param

Param Quark for SuperCollider

The goal of this quark is to ease the controlling of sounds objects parameters (Ndef, Pdef, etc) using GUI, MIDI, and OSC.

```
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

// now the MIDI knob number 16 controls the Ndef parameter \freq
MIDIMap([16], ~p); 
```

The benefit is you have the same API for controlling Ndef, Pdef, Synth, Bus, ... and you can easily control arrays and envelopes in the same way. Setting .action and updating of GUI, and freeing resources like MIDIFunc and buses are done for you in the background.

Using this library, you can quickly write custom GUI, but more importantly, you can build complex GUI which are completely independent of the sounds objects you control, just pass to your GUI a list a Param you want to control. Mapping any of theses parameters with MIDI is easy too.

```
// if you have a GUI with 8 sliders, you can simply send it the list of parameters you want to control:
~mygui.set_params( [
	[Pdef(\plop), \freq],
	[Pdef(\plop), \lpfreq],
	[Pdef(\plop), \rq],
	[Ndef(\echo), \shift1],
	[Ndef(\echo), \shift2],
] )
```

Dependencies : please install the following quark before trying Param:
- JITLibExtensions

Current features:
- map any Ndef of Pdef parameter to a GUI object, including arrays and envelopes parameters
- map any Ndef of Pdef parameter to a MIDI control
- write easily a GUI showing current parameters mapped to your MIDI controls
- save and load presets, persistent across SC reboot
- morph between selected presets
- switch quickly between normal mode and bus mode in patterns (bus mode is the way to continuously control a parameter)
- block the MIDI until the MIDI value match the Param value to avoid sudden value jump
- GUI can be updated in synchronous or polling mode
- replace default GUI used by .edit

Controlled objects
- Ndef
- Pdef
- Ndef volume
- Volume (eg: s.volume)
- TempoClock


Planned features:
- control others objects:
	- Bus
	- Synth
	- Instr
	- list (to have an array of slider sequencing the sound in a pattern for example)
- map Param to others GUI objects like PopupMenu
- integration with Modality toolkit
- easy step sequencer creation with visual feedback
- OSC mapping

