
//////////////// PlayerWrapperGroup

(
Pdef(\testrec1, Pbind(
	\instrument, \default,
	\degree, Pseq([0,1,3],inf),
	\octave, 5,
	\dur, 1/8,
	\amp, 0.1
));

Pdef(\testrec2, Pbind(
	\instrument, \default,
	\degree, Pseq([5,4,6,4],inf),
	\octave, 4,
	\dur, 1/4,
	\amp, 0.2
));

~pwg = PlayerWrapperGroup([
	Pdef(\testrec1),
	Pdef(\testrec2),
]);

ParamProto.init;
~pgr = ~playerGroupRecorder.new(~pwg);
~el = XEventLoop.newInstance;
~pgr.eventLoop = ~el;

WindowDef(\testrec, {
	HLayout (
		VLayout (*
			~pwg.collect({ arg player;
				player.asView;
			})
			++ [nil] 
		),
		VLayout (
			BasicButton.new.string_("start recording").action_({
				~pgr.startRecording;
			}),
			BasicButton.new.string_("stop recording").action_({
				~pgr.stopRecording;
			}),
			nil
		)
	)
});

)



// record some players
WindowDef(\testrec).front;
// verify recording
~pgr.eventLoop.list;
// view recording
ClipTimeline(\testrec).eventList_(~pgr.eventLoop.list);
ClipTimeline(\testrec).edit;


////////////////////////////////////////////////////////// debug


PlayerWrapper(Pdef(\testrec1)).asPattern
~pwg[0].asPlayerEvent

~pgr.startRecording;

~pgr.startRecording;

ParamProto.init(true);
ClipTimeline(\testrec).eventList
Pdef.defaultQuant = 4
Pdef(\testrec1).quant
Pdef(\testrec1).play(quant:0)
PlayerWrapper(Pdef(\testrec1)).playNow
PlayerWrapper(Pdef(\testrec1)).play
PlayerWrapper(Pdef(\testrec1)).wrapper.playNow
PlayerWrapper(Pdef(\testrec1)).wrapper.target.play(quant:0)
