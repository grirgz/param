
////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////
/////////////////////////////// tests init



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
NoteTimeline(\test_notetimeline).clear;
NoteTimeline(\test_notetimeline, Param(Pdef(\test_pdef), \midinote));
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

NoteTimeline(\test_notetimeline).edit;
ClipTimeline(\test_outer).edit; // make it always on top

///////////////// test 1
/// everything normal

(
ClipTimeline('test_outer').eventList = XEventList.newFrom([
	(\absTime: 0.0, \dur: 0.5, \label: 'start', \playDur: 0.5, \relDur: 0.5, \sustain: 0, \type: 'start', ),
	PatternEvent((\absTime: 0.5, \dur: 3.5, \legato: 1, \midinote: 3.0, \playDur: 3.5, \relDur: 3.5, \sustain: 2.75, \timeline: { NoteTimeline('test_notetimeline') }, \type: 'pattern', )),
	(\absTime: 4.0, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);
);

//////////// test 1.1: normal play
// PASS
ClipTimeline('test_outer').asPattern.play;

//////////// test 1.2: default preview play
// PASS
ClipTimeline('test_outer').asSelectedPattern(1).play;

//////////// test 1.2.1: play should behave as asSelectedPattern with inf repeat and a proxy
// PASS
ClipTimeline('test_outer').play;

//////////// test 1.2: cut middle preview
/// should loop on previewStart
// PASS
(
ClipTimeline('test_outer').cursor.startPosition = 1;
ClipTimeline('test_outer').cursor.endPosition = 2;
ClipTimeline('test_outer').asSelectedPattern(2).play;
)


//////////// test 1.3: only start 
/// should loop on clipStart
// PASS
(
ClipTimeline('test_outer').cursor.startPosition = 1;
ClipTimeline('test_outer').cursor.endPosition = nil;
ClipTimeline('test_outer').asSelectedPattern(2).play;
)


///////////////// test 2
/// event is cut by clip loop
(
ClipTimeline('test_outer').eventList = XEventList.newFrom([
	PatternEvent((\absTime: 0.5, \dur: 1.0, \legato: 1, \midinote: 3.0, \playDur: 1.0, \relDur: 1.0, \sustain: 3.5, \timeline: { NoteTimeline('test_notetimeline') }, \type: 'pattern', )),
	(\absTime: 1.5, \dur: 1.5, \label: 'start', \playDur: 1.5, \relDur: 1.5, \sustain: 0, \type: 'start', ),
	(\absTime: 3.0, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);
ClipTimeline('test_outer').cursor.startPosition = nil;
ClipTimeline('test_outer').cursor.endPosition = nil;
);

//////////// test 2.1: normal play
// should begin pattern in the middle and stop in the middle
// PASS
ClipTimeline('test_outer').asPattern.play;

//////////// test 2.2: default preview play
// should be exactly like normal play
// PASS
(
ClipTimeline('test_outer').cursor.startPosition = nil;
ClipTimeline('test_outer').cursor.endPosition = nil;
ClipTimeline('test_outer').asSelectedPattern(1).play;
)

//////////// test 2.3: normal play should ignore preview loop
// PASS
(
ClipTimeline('test_outer').cursor.startPosition = 1;
ClipTimeline('test_outer').cursor.endPosition = 4;
ClipTimeline('test_outer').asPattern.play;
)

//////////// test 2.4: outer preview play
// should start play before startEvent and stop after endEvent
// PASS
(
ClipTimeline('test_outer').cursor.startPosition = 1;
ClipTimeline('test_outer').cursor.endPosition = 3.5;
ClipTimeline('test_outer').asSelectedPattern(1).play;
)

//////////// test 2.5: preview start before first event
// should add a rest to compensate
// PASS 
(
ClipTimeline('test_outer').cursor.startPosition = 0.25;
ClipTimeline('test_outer').cursor.endPosition = 3.5;
ClipTimeline('test_outer').asSelectedPattern(1).play;
)

///////////////// test 3
/// there is an event after end event

(
ClipTimeline('test_outer').eventList = XEventList.newFrom([
	PatternEvent((\absTime: 0.5, \dur: 1.0, \legato: 1, \midinote: 3.0, \playDur: 1.0, \relDur: 1.0, \sustain: 3.5, \timeline: { NoteTimeline('test_notetimeline') }, \type: 'pattern', )),
	(\absTime: 1.5, \dur: 1.5, \label: 'start', \playDur: 1.5, \relDur: 1.5, \sustain: 0, \type: 'start', ),
	(\absTime: 3.0, \dur: 1.75, \label: 'end', \playDur: 1.75, \relDur: 1.75, \type: 'end', ),
	(\absTime: 4.75, \dur: 0, \legato: 1, \midinote: 1.0, \playDur: 0, \relDur: 0, \sustain: 3.0, ),
]);
ClipTimeline('test_outer').cursor.startPosition = nil;
ClipTimeline('test_outer').cursor.endPosition = nil;
);

//////////// test 3.1: normal play
// should begin pattern in the middle and stop in the middle
// PASS
ClipTimeline('test_outer').asPattern.play;

//////////// test 2.2: default preview play
// should be exactly like normal play
// PASS
(
ClipTimeline('test_outer').cursor.startPosition = nil;
ClipTimeline('test_outer').cursor.endPosition = nil;
ClipTimeline('test_outer').asSelectedPattern(1).play;
)


//////////// test 3.3 only start preview inside clip loop, no end
// should stop at end event
// PASS
(
ClipTimeline('test_outer').cursor.startPosition = 2;
ClipTimeline('test_outer').cursor.endPosition = nil;
ClipTimeline('test_outer').asSelectedPattern(1).play;
)

//////////// test 3.4 only start preview after end event, no end
// should stop at end of last event
// PASS
(
ClipTimeline('test_outer').cursor.startPosition = 3.5;
ClipTimeline('test_outer').cursor.endPosition = nil;
ClipTimeline('test_outer').asSelectedPattern(1).play;
)

//////////// test 3.4 previewStart in clip loop, previewEnd after clipEnd
// should stop at previewEnd
// PASS
(
ClipTimeline('test_outer').cursor.startPosition = 2;
ClipTimeline('test_outer').cursor.endPosition = 5;
ClipTimeline('test_outer').asSelectedPattern(1).play;
)
