
ClipTimeline(\bla).edit
ClipTimeline(\bla).presetCompileString.editorInsert
(
ClipTimeline('bla').tracks = [ ( 'index': 0, 'recordArmed': false, 'recordBus': 0, 'muted': false ), ( 'index': 1, 'recordArmed': false, 'recordBus': 0, 'muted': false ), ( 'index': 2, 'recordArmed': false, 'recordBus': 0, 'muted': false ), ( 'index': 3, 'recordArmed': false, 'recordBus': 0, 'muted': false ), ( 'index': 4, 'recordArmed': true, 'recordBus': 0, 'muted': false ) ];
ClipTimeline('bla').eventList = XEventList.newFrom([
	(\absTime: 0, \dur: 0.75, \label: 'start', \playDur: 0.75, \relDur: 0.75, \sustain: 0, \type: 'start', ),
	PatternEvent((\absTime: 0.75, \dur: 7.25, \midinote: 4, \playDur: 7.25, \relDur: 7.25, \sustain: 4.0, \timeline: { SampleTimeline('rec_bla_t4_191019_003818') }, \type: 'pattern', )),
	(\absTime: 8, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);
);

ClipTimeline(\bla).tracks.size
ClipTimeline(\bla).track_count


ClipTimeline(\rah).edit
ClipTimeline(\rah).tracks.size
ClipTimeline(\rah).areasize
ClipTimeline(\rah).track_count

ParamProto.init(true)

(
Pdef(\part, Pdef(\zed, 
	Ppar([
		Pbind(
			\instrument, \default,
			\freq, 200,
			\dur, 1,
			\amp, 0.1,
		),
		
	])
)).play;
);


	~proxy = EventPatternProxy.new.source_(Pbind());
	~proxy.get(\freq)
	~proxy.set(\freq, 500)
	PlayerWrapper(~proxy).asParamGroup
	SynthDesc(\default).asParamGroup
(
WindowDef(\bla, {
	~proxy.set(\instrument, \default);
	~proxy.play;
	VLayout (
		PlayerWrapper(~proxy).label_("proxy").asView,
		ParamGroupLayout.two_panes(SynthDesc(\default).asParamGroup(~proxy))
	)
}).front;
)
~proxy = EventPatternProxy.new.source_(Pbind());
~proxy.set(\instrument, \SampleTimeline_player)
~proxy.set(\instrument, nil)
~proxy.set(\xxx, nil)
~proxy.addHalo(\instrument, \SampleTimeline_player)
~proxy.envir
~proxy.asParamGroup
~proxy.play
~proxy.asParamGroup.getPresetCompileString
~proxy.asParamGroup.presetCompileString
Pbindef

~proxy.get(\freq)
Param(~proxy, \freq).wrapper
Param(~proxy, \freq).instrument
~target = ~proxy
~target.get(\instrument)
		~target.get(\instrument) ?? { 
			~target.getHalo(\instrument) ?? {
				(~target !? { BaseParam.getInstrumentFromPbind(~target.source) }) ? \default;
			};
		}
Param(~proxy, \freq).target.getHalo(\instrument)
Param(~proxy, \freq).get
Param(~proxy, \gain).default
Param(~proxy, \adsr).default.asCompileString
Param(~proxy, \adsr).normGet.asCompileString
Param(~proxy, \adsr).get.asCompileString
Param(~proxy, \adsr).wrapper.getRaw.asCompileString
~proxy.getVal(\adsr)

Param(~proxy, \gain).spec
Param(~proxy, \inbus).type
Spec.specs[\gain]
Spec.specs[\adsr].default.asCompileString
,
~proxy.getVal(\freq)
~proxy.unset(\freq)

SampleTimeline(\bla).edit
SampleTimeline(\bla).postPatternPreset
SampleTimeline(\bla).postPatternPreset = (freq: 300)
SampleTimeline(\bla).postProxy.getHalo(\instrument)
SampleTimeline(\bla).postProxy.get(\instrument)
SampleTimeline(\bla).postProxy.envir[\instrument]
Param(SampleTimeline(\bla).postProxy, \adsr).default.asCompileString
Param(SampleTimeline(\bla).postProxy, \adsr).instrument
Param(SampleTimeline(\bla).postProxy, \adsr).wrapper.instrument
TagSpec(\BusDef).list

~proxy = EventPatternProxy.new.source_(Pbind());

BusDef(\bla, \audio, 2)

Param.getSynthDefDefaultValue(\adsr, \SampleTimeline_player).asEnv.asCompileString
