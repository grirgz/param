

// tests
// drumkit recording
// 

/////////////////////// drumkit
(
ParamProto.init;

~drumkit = [
	Pbind(
		\instrument, \default,
		\detune, Pseq([200,201,212]*1.4/200,inf),
		\midinote, 60,
		\dur, 1,
		\amp, 0.1
	),
	Pbind(
		\instrument, \default,
		\detune, Pseq([200,201,212]*1.4/200,inf),
		\midinote, Pseq([62],inf),
		\dur, 1,
		\amp, 0.1
	),
	Pbind(
		\instrument, \default,
		\detune, Pseq([200,201,212]*1.4/200,inf),
		\midinote, Pseq([64],inf),
		\dur, 1,
		\amp, 0.1
	),
];

)

//////// map controller

(
~pg = nil;
~pg = ~pg ?? { ~parPlayerGroup.new(~drumkit) };
// recording with DrumPlayerWrapperGroup

MKtl(\lp, "novation-launchpad-s");
~pg.elDo({ arg player, idx;
	var pad = MKtl(\lp).elAt(\pad);
	player.debug("player");
	pad.elAt(idx).elAt(0).action_({
		"on".debug;
		player.play;
	});
	pad.elAt(idx).elAt(1).action_({
		"off".debug;
		player.stop;
	});
});

)

//////// map gui
(
// on off
//~pg = nil;
~pg = ~pg ?? { ~parPlayerGroup.new(~drumkit) };
WindowDef(\piano, {

	VLayout (*
		~pg.elCollect({ arg player, idx;
			BasicButton.new.mouseDownAction_({
				player.play;
			}).action_({
				player.stop;
			}).label_(idx);
		})
	)

}).front;
)

//////// recording
(
// now record

//~PlayerWrapperRecorder( XEventLoop(\drum), DrumPlayerGroup(~drumkit) );
~pr = ~playerGroupRecorder.new(~pg);

~pr.stopRecording;
~pr.startRecording;
)

~pr.eventLoop.play

//////// saving


~pr.getPresetCompileString.editorInsert

//////// editing

~kitTimeline.clear
~kitTimeline
(
~kitTimeline.new(\kitx, ~drumkit).edit;
)
(
~pg = ~kitTimeline.new(\kit).wrapper;

MKtl(\lp, "novation-launchpad-s");
~pg.elDo({ arg player, idx;
	var pad = MKtl(\lp).elAt(\pad);
	player.debug("player");
	pad.elAt(idx).elAt(0).action_({
		"on".debug;
		player.play;
	});
	pad.elAt(idx).elAt(1).action_({
		"off".debug;
		player.stop;
	});
});

)

~kitTimeline.new(\kit).eventLoop.list.print
~kitTimeline.new(\kit).composeParamPattern = false

~str = ~kitTimeline.new(\kit).asPattern.asStream
~str.next(Event.default)

~str = ~kitTimeline.new(\kit).eventLoop.list.asPattern.asStream
~str.next(Event.default)

~pg.recorder.startRecording
~kitTimeline.new(\kit).startRecording
~kitTimeline.new(\kit).stopRecording
~kitTimeline.new(\kit).isRecording = true
~kitTimeline.new(\kit).isRecording = false
~kitTimeline.new(\kit).recorder.startRecording
~kitTimeline.new(\kit).recorder.stopRecording
~kitTimeline.new(\kit).recorder.eventLoop.hash
~kitTimeline.new(\kit).eventLoop.hash
~kitTimeline.new(\kit).edit;

~kitTimeline.new(\kit, ~pg).startRecording;
~kitTimeline.new(\kit, ~pg).stopRecording;
~kitTimeline.new(\kit, ~pg).getPresetCompileString.editorInsert;

//////// playing

~pg.elAt(0).play;
~pg.elAt(0).stop;
~kitTimeline.new(\kit).play;

(
Pdef(\main, 
	Pbind(\amp, 0.2)
	<>
	~kitTimeline.new(\kit).asPattern,
).play;
)

///////// PdrumStep

(
PdrumStep(~drumkit, Pbind(
	\midinote, Pseq([0,1, 1,2,3],4),
	\dur, 1/4,
	\amp, 0.1
)).play;
)

///////////////////////////////////////////////////////////////// debug

ParamProto.init;
ParamProto.init(true);


(
PdrumStep(~drumkit, Pbind(
	\midinote, Pseq([0,1, 1,2,3],4),
	\kitIndex, Pseq([0,1, 1,2,3],4),
	\dur, 1/4,
	\amp, 0.1
), key: \kitIndex).play;
)
