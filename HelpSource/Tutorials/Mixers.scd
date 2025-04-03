title:: Using and creating mixers
summary:: Mixers
categories:: GUI

You can create a mixer just by providing a name code::TrackMixerDef(\some_name)::. It allow you to assign a mixer bus to any pattern.

code::
// create two pattern each playing in its own mixer bus
(
Pdef(\block1,
	Ppar([
		Pbind(
			\instrument, \default,
			\octave, 3,
			\note, Pseq([
				0,6,4,-3,
			],inf),
			\dur, 1/4,
			\amp, 0.1,
			\out, TrackMixerDef(\bass),
		),
		Pbind(
			\instrument, \default,
			\octave, 5,
			\note, Pseq([
				6,0,0,4,
			],inf),
			\dur, 1/2,
			\amp, 0.1,
			\out, TrackMixerDef(\hi),
		),
		
	])
)).play;
);

TrackMixerDef(\bass).edit; // edit mixer \bass

(
// build a GUI with both mixers
WindowDef(\mixer, { arg def;
	HLayout (
		TrackMixerDef(\bass),
		TrackMixerDef(\hi),
		nil
	)
}).front;
)
code:: 

SECTION:: TODO

There is a simple predefined mixer:

code::
ProtoDef(\simpleMixer).edit;
::

Internally it create an array of ten ProtoInst(\TrackMixer, mixer_key) where mixer_key start with \mixer0 and end at \mixer10. 

There is also an additionnal simple mixer for more tracks

code::
ProtoDef(\fxMixer).edit
::



TODO: doc


TrackMixerDef(\track1, ProtoTemplateDef(\NdefTrackMixer)).asParamGroup.edit
TrackMixerDef(\track1, ProtoTemplateDef(\NdefTrackMixer)).edit
TrackMixerDef(\track1, ProtoTemplateDef(\NdefTrackMixer)).outbus


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

(
WindowDef(\TrackMixer, { arg def, track;
	var mixerView = { arg mixer;
		var sendKnobCount = 8;
		var param;
		var sends = [], allsends = [];
		var layout;
		var slider, knobs = List.new, textfield;
		var stack;
		var meterin = SimpleServerMeterView(nil, 0,2, mixer.inbus);
		var meterout = SimpleServerMeterView(nil, 0,2, mixer.outbus);
		slider = Slider.new.orientation_(\vertical).background_(Color.clear);


		stack = StackLayout(
			slider, 
			meterin.view,
			// FIXME: no way to make ticks less wide, so with both in and out displayed, meter is too small
			//View.new.layout_(HLayout (
				//meterin.view,
				//meterout.view,
			//).spacing_(0).margins_(0))
		).mode_(\stackAll);
		layout = VLayout(
			HLayout(
				VLayout (
					//slider,
					stack,
					textfield = TextField.new,
				),
				VLayout(*
					knobs = sendKnobCount.collect({ 
						Knob.new;
					})
				)
			)
		);
		layout.addUniqueMethod(\model_, { arg me, mixer;
			if(mixer.isKindOf(Volume)) {
				//"volume".debug;
				param = Param(mixer, \volume);
				sends = mixer.sendCount.collect({ arg x; x -> nil });
			} {
				//"mixer".debug;
				param = mixer.asParam(\postfader);
				allsends = mixer.sendCount.collect({ arg x; x -> mixer.asParam(\send+++x) });
				//sends = allsends.select{ arg assoidx;
					//var idx = assoidx.key;
					//var bus = mixer[\sendbus +++ idx].value(simpleMixer);
					//bus != simpleMixer.nullbus
				//};
			};
			//[slider, param].debug("slider, param: what ?");
			slider.mapParam(param);
			textfield.mapParam(param);
			//knobs.do({ arg k, idx; 
				//var send = sends[idx] ?? { allsends[idx] }; 
				//k.mapParam(send.value); 
				//k.toolTip = "%: %".format(send.key, simpleMixer[\sendbus +++ send.key].value(simpleMixer));
			//})
		});
		if(mixer.notNil) {
			layout.model = mixer;
		};
		layout.addUniqueMethod(\slider, { slider });
		layout.addUniqueMethod(\textfield, { textfield });
		layout.addUniqueMethod(\knobs, { knobs });
		layout;

	};


	//def.windowName = simpleMixer.key ? "simpleMixer";

	mixerView.(track)
}).front(TrackMixerDef(\track1));
)
