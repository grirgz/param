
///////////////////// NoteTimeline
(
	Pdef(\test_notetimeline, Pbind(
		\instrument, \default,
		\degree, Pseq([0],inf),
		\dur, 1,
		\amp, 0.1
	));
);
NoteTimeline(\test_notetimeline, Param(Pdef(\test_notetimeline), \midinote)).edit

///////////////////// KitTimeline
(
PatKitDef(\test_patkitdef).targets = [
	StepEvent.newFrom((pos: 0.04, instrument: 'playersec', bufnum: BufDef('fix3/kick1.flac'))),
	StepEvent.newFrom((instrument: 'playersec', adsr: Env([ 0, 1, 1, 0 ], [ 0.004, 0, 0.01 ], [ -4, -4, -4 ], 2), bufnum: BufDef('/home/ggz/Musique/sc/reckit/fix3/kick1.flac'), pos: 0.08)),
	nil,
	nil,
	StepEvent.newFrom((pos: 0.04, instrument: 'playersec', bufnum: BufDef('fix3/snare1.flac'))),
	nil,
	nil,
	nil,
	StepEvent.newFrom((instrument: 'playersec', bufnum: BufDef('my3/snare2.flac'), pos: 0.031065759637188, sustain: 1)),
	nil,
	nil,
	nil,
	StepEvent.newFrom((pos: 0.04, instrument: 'playersec', bufnum: BufDef('fix1/hat.flac'))),
	StepEvent.newFrom((adsr: Env([ 0, 1, 1, 0 ], [ 0.01, 0, 0.1 ], [ -4, -4, -4 ], 2), speed: 1.000875, bufnum: BufDef('fix3/hurl.flac'), instrument: 'playersec', gain: 0.70786947024746, sustain: 1, pos: 0.25433106575964)),
];
);

KitTimeline(\test_kittimeline, PatKitDef(\test_patkitdef));
KitTimeline(\test_kittimeline).edit;

///////////////////// ClipTimeline

ClipTimeline(\test_cliptimeline).edit;

///////////////////// TrackTimeline

TrackTimeline(\test_tracktimeline).edit;

///////////////////// ParamTimeline

(
	Pdef(\test_envtimeline, Pbind(
		\instrument, \default,
		\degree, Pseq([0],inf),
		\dur, 1,
		\amp, 0.1
	)).play;
);
ParamTimeline(\test_envtimeline, Param(Pdef(\test_envtimeline), \freq));
ParamTimeline(\test_envtimeline).edit;
ParamTimeline(\test_envtimeline).clear;
ParamProto.init(true)


///////////////////////////////////////////////////////////////
// Features to test:
// - instanciation
// - playing
// 		- playing from GUI
// 		- playing from code
//		- embed in pattern
//		- start and stop event
//		- start and stop selection
// - recording
//		- GUI
//		- MIDI
// - outBus
// - nodes
//		- create node
//		- delete node
//		- move node
//		- select node
//		- select nodes by rect
// - saving
//		- presetCompileString
//		- presetCompileStringSavePath
//		- save As button
// - ClipTimeline
//		- play with no quant:
//			- Pdef
//			- Ndef
//			- NoteTimeline
//			- ClipTimeline
//			- KitTimeline
//			- ParamTimeline
