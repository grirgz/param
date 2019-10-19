
(
	~test = UnitTest.new;
	~t = 135/4/60; TempoClock.default.tempo = ~t;
	Pdef(\test_pdef, Pbind(
		\instrument, \default,
		\degree, Pseq([0],inf),
		\dur, 1,
		\amp, 0.1
	));
	ParamProto.init;
);
NoteTimeline(\test_notetimeline, Param(Pdef(\test_pdef), \midinote)).edit
NoteTimeline(\test_notetimeline).repeatCount = 1

NoteTimeline(\test_notetimeline).presetCompileString.editorInsert
(
NoteTimeline('test_notetimeline').eventList = XEventList.newFrom([
	(\absTime: 0, \dur: 0.0, \label: 'start', \playDur: 0.0, \relDur: 0.0, \sustain: 0, \type: 'start', ),
	(\absTime: 0.0, \dur: 0.125, \midinote: 53.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.125, \dur: 0.125, \midinote: 56.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.25, \dur: 0.125, \midinote: 65.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.375, \dur: 0.125, \midinote: 66.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.5, \dur: 0.125, \midinote: 81.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.625, \dur: 0.125, \midinote: 83.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.75, \dur: 0.125, \midinote: 99.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.875, \dur: 0.125, \midinote: 103.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 1, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);
);

(
NoteTimeline('test_notetimeline2', Param(Pdef(\test_pdef), \midinote)).eventList = XEventList.newFrom([
	(\absTime: 0, \dur: 0.0, \label: 'start', \playDur: 0.0, \relDur: 0.0, \sustain: 0, \type: 'start', ),
	(\absTime: 0.0, \dur: 0.125, \midinote: 53.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.125, \dur: 0.125, \midinote: 56.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.25, \dur: 0.125, \midinote: 65.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.375, \dur: 0.125, \midinote: 66.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.5, \dur: 0.125, \midinote: 81.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.625, \dur: 0.125, \midinote: 83.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.75, \dur: 0.125, \midinote: 99.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 0.875, \dur: 0.125, \midinote: 103.0, \playDur: 0.125, \relDur: 0.125, \sustain: 0.1, ),
	(\absTime: 1, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);
);

NoteTimeline(\test_notetimeline).play
NoteTimeline(\test_notetimeline).stop
NoteTimeline(\test_notetimeline).asPattern.asStream.nextN(50,()).asCompileString.editorInsert
(
~expected = [ ( 'instrument': 'default', 'playDur': 0.0, 'degree': 0, 'dur': 0.0, 
  'amp': 0.1, 'sustain': 0, 'absTime': 0, 'delta': 0.0, 'relDur': 0.0, 
  'type': 'rest', 'label': 'start' ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 53.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.0 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 56.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.125 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 65.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.25 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 66.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.375 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 81.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.5 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 83.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.625 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 99.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.75 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 103.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.875 ), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil 
];

~test.assertEquals(NoteTimeline(\test_notetimeline).asPattern.asStream.nextN(50,()), ~expected)
);


//
ClipTimeline(\test_time_clip).edit
ClipTimeline(\test_time_clip).play
ClipTimeline(\test_time_clip).presetCompileString.editorInsert


ClipTimeline(\test_time_clip).repeatCount = 1
ClipTimeline(\test_time_clip).asPattern.asStream.nextN(50,()).asCompileString.editorInsert



/////////// TEST 1: play two NoteTimeline sequentially


(
ClipTimeline('test_time_clip').eventList = XEventList.newFrom([
	(\absTime: 0, \dur: 0.125, \label: 'start', \playDur: 0.125, \relDur: 0.125, \sustain: 0, \type: 'start', ),
	PatternEvent((\absTime: 0.125, \dur: 0.25, \midinote: 0.0, \playDur: 0.25, \relDur: 0.25, \sustain: 0.25, \timeline: { NoteTimeline('test_notetimeline') }, \type: 'pattern', )),
	PatternEvent((\absTime: 0.375, \dur: 1.125, \midinote: 1.0, \playDur: 1.125, \relDur: 1.125, \sustain: 0.25, \timeline: { NoteTimeline('test_notetimeline2') }, \type: 'pattern', )),
	(\absTime: 1.5, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);
);


(
~expected = [ ( 'playDur': 0.125, 'dur': 0.125, 'sustain': 0, 'delta': 0.125, 
  'relDur': 0.125, 'absTime': 0, 'type': 'rest', 'label': 'start' ), ( 'delta': 0.0, 'dur': Rest(0.25) ), ( 'instrument': 'default', 'playDur': 0.0, 'degree': 0, 'dur': 0.0, 
  'amp': 0.1, 'sustain': 0, 'absTime': 0, 'delta': 0.0, 'relDur': 0.0, 
  'type': 'rest', 'label': 'start' ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 53.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.0 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 56.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.125 ), ( 'delta': 0.0, 'dur': Rest(0.0) ), ( 'delta': 0.0, 'dur': Rest(1.125) ), ( 'instrument': 'default', 'playDur': 0.0, 'degree': 0, 'dur': 0.0, 
  'amp': 0.1, 'sustain': 0, 'absTime': 0, 'delta': 0.0, 'relDur': 0.0, 
  'type': 'rest', 'label': 'start' ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 53.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.0 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 56.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.125 ), ( 'delta': 0.875, 'dur': Rest(0.875) ), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil ];

~test1_expected = ~expected;
  
~test.assertEquals(ClipTimeline(\test_time_clip).asPattern.asStream.nextN(50,()), ~expected);
);


/////////// TEST 2: play two NoteTimeline in parallel

(
ClipTimeline('test_time_clip').eventList = XEventList.newFrom([
	(\absTime: 0, \dur: 0.125, \label: 'start', \playDur: 0.125, \relDur: 0.125, \sustain: 0, \type: 'start', ),
	PatternEvent((\absTime: 0.125, \dur: 0.0, \midinote: 0.0, \playDur: 0.0, \relDur: 0.0, \sustain: 0.25, \timeline: { NoteTimeline('test_notetimeline') }, \type: 'pattern', )),
	PatternEvent((\absTime: 0.125, \dur: 1.375, \midinote: 1.0, \playDur: 1.375, \relDur: 1.375, \sustain: 0.25, \timeline: { NoteTimeline('test_notetimeline2') }, \type: 'pattern', )),
	(\absTime: 1.5, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);
);


///// FAIL: expected is wrong, sound wrong, 
(
~expected = [ ( 'playDur': 0.125, 'dur': 0.125, 'sustain': 0, 'delta': 0.125, 
  'relDur': 0.125, 'absTime': 0, 'type': 'rest', 'label': 'start' ), ( 'delta': 0.0, 'dur': Rest(0.0) ), ( 'instrument': 'default', 'playDur': 0.0, 'degree': 0, 'dur': 0.0, 
  'amp': 0.1, 'sustain': 0, 'absTime': 0, 'delta': 0.0, 'relDur': 0.0, 
  'type': 'rest', 'label': 'start' ), ( 'delta': 0.0, 'dur': Rest(0.0) ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 53.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.0 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 56.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.125 ), ( 'delta': 0.0, 'dur': Rest(1.375) ), ( 'instrument': 'default', 'playDur': 0.0, 'degree': 0, 'dur': 0.0, 
  'amp': 0.1, 'sustain': 0, 'absTime': 0, 'delta': 0.0, 'relDur': 0.0, 
  'type': 'rest', 'label': 'start' ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 53.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.0 ), ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 56.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.125 ), ( 'delta': 0.875, 'dur': Rest(1.125) ), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil ];
~test.assertEquals(ClipTimeline(\test_time_clip).asPattern.asStream.nextN(50,()), ~expected);
~cur = ClipTimeline(\test_time_clip).asPattern.asStream.nextN(50,());
~test.assert(~cur != ~test1_expected);
);



/// debug
(
~expected = [ ( 'playDur': 0.125, 'dur': 0.125, 'sustain': 0, 'delta': 0.125, 
  'relDur': 0.125, 'absTime': 0, 'type': 'rest', 'label': 'start' ),  

  ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 53.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.0 ),

  ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 56.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.125 ),
  ( 'delta': 0.0, 'dur': Rest(0.375) ),
  ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 53.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.0 ),
  ( 'instrument': 'default', 'playDur': 0.125, 'degree': 0, 'dur': 0.125, 
  'amp': 0.1, 'sustain': 0.1, 'midinote': 56.0, 'delta': 0.125, 'relDur': 0.125, 
  'absTime': 0.125 ), 
  ( 'delta': 0.875, 'dur': Rest(1.125) ), nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil, nil ];
  Pseq(~expected).play
)


////////////////// TEST3 : pdef instead of notetimeline

(
Pdef(\test_scale, 
	Pbind(
		\instrument, \default,
		\degree, Pseq((0..8),inf),
		\dur, 1/8,
		\amp, 0.1,
	)
);
Pdef(\test_scale2, 
	Pbind(
		\instrument, \default,
		\degree, Pseq((0..8),inf)+3,
		\dur, 1/8,
		\amp, 0.1,
	)
);
Ndef(\test_ndef1, { SinOsc.ar(600)*0.1!2 });
Ndef(\test_ndef2, { SinOsc.ar(720)*0.1!2 });
);



/// paralel Pdef
// PASS
(
ClipTimeline('test_time_clip').eventList = XEventList.newFrom([
	(\absTime: 0, \dur: 0.125, \label: 'start', \playDur: 0.125, \relDur: 0.125, \sustain: 0, \type: 'start', ),
	PatternEvent((\absTime: 0.125, \dur: 0.0, \midinote: 1.0, \pattern: Pdef('test_scale'), \playDur: 0.0, \relDur: 0.0, \sustain: 0.5, \type: 'pattern', )),
	PatternEvent((\absTime: 0.125, \dur: 1.375, \midinote: 2.0, \pattern: Pdef('test_scale2'), \playDur: 1.375, \relDur: 1.375, \sustain: 0.5, \type: 'pattern', )),
	(\absTime: 1.5, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);
);



/// paralel Ndef
// PASS: but maybe stop too fast ?
(
ClipTimeline('test_time_clip').eventList = XEventList.newFrom([
	(\absTime: 0, \dur: 0.125, \label: 'start', \playDur: 0.125, \relDur: 0.125, \sustain: 0, \type: 'start', ),
	PlayerEvent((\absTime: 0.125, \dur: 0.0, \midinote: 1.0, \playDur: 0.0, \receiver: PlayerWrapper(Ndef('test_ndef2')), \relDur: 0.0, \sustain: 0.875, \type: 'player', )),
	PlayerEvent((\absTime: 0.125, \dur: 1.375, \midinote: 2.0, \playDur: 1.375, \receiver: PlayerWrapper(Ndef('test_ndef1')), \relDur: 1.375, \sustain: 0.875, \type: 'player', )),
	(\absTime: 1.5, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);
);


///////////////////////// paralel events


