
///////////////////////////////// GUI Test


(
Ndef(\paramrectest, { arg freq=200, pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(freq);
	sig = Pan2.ar(sig, pan, amp);
}).play;
);

(
SynthDef(\paramrectest, { arg out=0, amp=0.1, gate=1, pan=0, freq=200;
	var sig;
	sig = SinOsc.ar(\xfreq.kr(300));
	sig = sig * EnvGen.ar(Env.adsr(0.01,0.1,0.8,0.1),gate,doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(out, sig);
}).add;
);

(
Pdef(\pdefparamrectest, Pbind(
	\instrument, \paramrectest,
	\degree, Pseq([0],inf),
	\dur, 1,
	\amp, 0.1
)).play;
);

ParamProto.init(true)
ParamTimeline(\pdefparamrectest).clear

ParamTimeline(\pdefparamrectest, Param(Pdef(\pdefparamrectest), \xfreq)).edit
ParamTimeline(\pdefparamrectest).asPattern.asStream.nextN(10,()).do(_.postln)
ParamTimeline(\pdefparamrectest).eventList.print
ParamTimeline(\pdefparamrectest).eventList.asPattern.nextN(10,()).do(_.postln)

///////////////////////////////// Recording

~recorder = ~paramRecorder.new(~param)
~recorder.eventLoop = ParamTimeline(\pdefparamrectest).eventLoop
~recorder.startRecording
~recorder.stopRecording
~recorder.eventLoop.list.print

////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////// asPattern algo

(
SynthDef(\paramrectest, { arg out=0, amp=0.1, gate=1, pan=0, freq=200;
	var sig;
	sig = SinOsc.ar(\xfreq.kr(300));
	sig = sig * EnvGen.ar(Env.adsr(0.01,0.1,0.8,0.1),gate,doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(out, sig);
}).add;

Pdef(\pdefparamrectest, Pbind(
	\instrument, \paramrectest,
	\degree, Pseq([0],inf),
	\dur, 1,
	\amp, 0.1
));

	
ParamTimeline('pdefparamrectest', Param(Pdef(\pdefparamrectest), \xfreq)).eventList = XEventList.newFrom([
        (\absTime: 0, \dur: 0.0, \label: 'start', \playDur: 0.0, \relDur: 0.0, \sustain: 0, \type: 'start', ),
        (\absTime: 0.0, \dur: 0.25, \legato: 1, \level: 214.489288885121, \playDur: 0.25, \relDur: 0.25, ),
        (\absTime: 0.25, \dur: 0.375, \legato: 1, \level: 351.899324476949, \playDur: 0.375, \relDur: 0.375, ),
        (\absTime: 0.625, \dur: 0.375, \legato: 1, \level: 777.9884080813402, \playDur: 0.375, \relDur: 0.375, ),
        (\absTime: 1.0, \dur: 0.0, \legato: 1, \level: 372.933256834288, \playDur: 0.0, \relDur: 0.0, ),
        (\absTime: 1, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);

///// step 0: set param

~param = Param(Pdef(\pdefparamrectest), \xfreq);
~param.get.postln;
ParamTimeline('pdefparamrectest').outBus.postln;
Pdef(\pdefparamrectest).get(\xfreq).postln;
Pdef(\pdefparamrectest).set(\xfreq, ParamTimeline('pdefparamrectest').outBus.asMap);
Pdef(\pdefparamrectest).get(\xfreq).postln;
~param.get.postln;

///// step1: eventlist
~eventlist = ParamTimeline(\pdefparamrectest).eventList;
~eventlist.print;

///// step2: remove_envlist_duplicate (should not do anything because there are no duplicate)
~remove_envlist_duplicate.(~eventlist);
~eventlist.size.postln;

///// step3: compute_eventenv_sustain
~compute_eventenv_sustain.(~eventlist);
~eventlist.print;

///// step4: wrap in Pembed
~pat1 = Pembed( Pn(~eventlist.clone, 1), 0);
~pat1.asStream.nextN(10,()).do(_.postln);

///// step5: cut end
~pat2 = Pfindur(10, ~pat1);
~pat2.asStream.nextN(10,()).do(_.postln);

///// step6: chain with special pattern
(
~pat3 = Pbind(
	\instrument, \env_segment,
	//\level, Pfunc({ arg ev; ev[self.param.property] ? default }),
	\curve, 0,
	\out, ParamTimeline('pdefparamrectest').outBus,
	\time, Pfunc({ arg ev; ev.use { 
			// absTime is in beats (but maybe should be in seconds ?)
			// need to convert to seconds
			//(ev.next_absTime ? ev.absTime) - ev.absTime / thisThread.clock.tempo
			// use dur instead
			( ev[\sustain] ? 1 ) / thisThread.clock.tempo
	} }),
	\type, Pfunc({  arg ev;

		if(ev[\type].isNil or: { ev[\type] == \note }) {
			if(ev[\next_level].isNil) { // last one should be rest
				\rest
			} {
				if(ev[\time].notNil and: {ev[\time] < 0.051}, {
					ev[\array] = [ ev[\level] ];
					//ev.debug("envTimeline: asPattern: busmode!");
					\bus;
				}, { 
					\note;
				});
			}

		} {
			ev[\type]
		}
	})
).trace(prefix:"MAIIIIIIIIIIIIIIIIIIIIIIII") <> ~pat2;
);
~pat3.asStream.nextN(10,()).do(_.postln);

///// step7: wrap in pattern_to_envpattern
~pat4 = ~pattern_to_envpattern.(~pat4);
~pat4.asStream.nextN(10,()).do(_.postln);

//// test
Pdef(\pdefparamrectest).play;
~param.set(300);
~pat4.play;


)
