(
~makegui = { arg fun;
	var win;
	var widget;
	win = Window.new;
	widget = fun.();
	win.layout = HLayout.new(widget);
	win.alwaysOnTop = true;
	win.front;
};
)

//////////////////// Ndef

// scalar

(
Ndef(\ndef_scalar, { arg freq=200, pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(freq);
	sig = Pan2.ar(sig, pan, amp);
}).play;
);

Platform.userExtensionDir

~p = Param(Ndef(\ndef_scalar), \freq, \freq);

~p.get;
~p.get == 200;
~p.set(400);
~p.get == 400;
~p.normGet;
~p.normSet(0.4);

~makegui.({ ~p.asView; });

// array

(
Ndef(\ndef_array, { arg pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar(\freq.kr(100!3));
	sig = Pan2.ar(sig, pan, amp).mean;
}).play;
);

~p = Param(Ndef(\ndef_array), \freq, XArraySpec(\freq!3));

~p.get;
~p.get == ( 100!3 );
~p.set([120,150,780]); // must reload the ndef to take effect :( ndef bug
~p.get == [120,150,780];
~p.normGet;
~p.normSet([0.5,0.2,0.4]);
~p.normSet([0.5,0.2,0.4]); 
//Ndef(\ndef_scalar).spawn

~makegui.({ ~p.asView; });

// env

(
Ndef(\ndef_env, { arg pan=0, amp=0.1;
	var sig;
	sig = SinOsc.ar( 
		EnvGen.ar( \freqenv.kr(Env([150,550,250,1000],[0.1,0.8,0.3])),Impulse.kr(1/2),doneAction:0)
	);
	sig = Pan2.ar(sig, pan, amp);
}).play;
);

~p = Param(Ndef(\ndef_env), \freqenv, XEnvSpec(\freq!4, ControlSpec(0,2,\lin,0,0.1)));

~p.get;
~p.get.asCompileString;
~p.get.asEnv.asCompileString;
~p.get == Env([150,550,250,1000],[0.1,0.8,0.3]); // give false, but it's true (float precision error ?)
~p.set( Env([1000,200,501,3200],[0.2,0.4,0.6]) ); // must reload the ndef to take effect :( ndef bug
~p.get == Env([1000,200,501,3200],[0.2,0.4,0.6]);
~p.normGet;
~p.normGet.asCompileString
~p.normSet(Env([0.4,0.200,0.7,0.5],[0.2,0.4,0.6]));

~makegui.({ ~p.asView; });


//////////////////// Pdef

// scalar

(
SynthDef(\synthdef_scalar, { arg out=0, amp=0.1, gate=1, pan=0, freq=200;
	var sig;
	sig = SinOsc.ar(freq);
	sig = sig * EnvGen.ar(Env.adsr(0.01,0.1,0.8,0.1),gate,doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(out, sig);
}).add;
);

(
Pdef(\pdef_scalar, Pbind(
	\instrument, \synthdef_scalar,
	\degree, Pseq([0],inf),
	\dur, 1,
	\amp, 0.1
)).play;
);

~p = Param(Pdef(\pdef_scalar), \freq, \freq);

~p.wrapper.instrument // need instrument to get spec and default value

Pdef(\pdef_scalar).addHalo(\instrument, \synthdef_scalar); // if no instrument in found in the pattern, you can set it

~p.get;
~p.get == 200; 
~p.set(400);
~p.get == 400;
~p.normGet;
~p.normSet(0.4);

~p.setBusMode // bus mode
~p.set(400);
~p.set(600);
~p.setBusMode(false)

~makegui.({ ~p.asView; });

// array

(
SynthDef(\synthdef_array, { arg out=0, amp=0.1, gate=1, pan=0;
	var sig;
	sig = SinOsc.ar(\freq.kr(100!3));
	sig = sig * EnvGen.ar(Env.adsr(0.01,0.1,0.8,0.1),gate,doneAction:2);
	sig = Pan2.ar(sig, pan, amp).mean;
	Out.ar(out, sig);
}).add;
);

(
Pdef(\pdef_array, Pbind(
	\instrument, \synthdef_array,
	\degree, Pseq([0],inf),
	\dur, 1,
	\amp, 0.1
)).play;
);

~p = Param(Pdef(\pdef_array), \freq, XArraySpec(\freq!3));

~p.get;
~p.get == ( 100!3 ); 
~p.set([120,1050,780]); 
~p.get == [120,1050,780];
~p.normGet;
~p.normSet([0.5,0.2,0.4]);

~p.setBusMode // bus mode
~p.set([120,1050,780]);
~p.set([520,5050,1780]);
~p.setBusMode(false)

~makegui.({ ~p.asView; });

// env

(
SynthDef(\synthdef_env, { arg out=0, amp=0.1, gate=1, pan=0;
	var sig;
	sig = SinOsc.ar( 
		EnvGen.ar( \freqenv.kr(Env([150,550,250,1000],[0.1,0.8,0.3])),gate,doneAction:0)
	);
	sig = sig * EnvGen.ar(Env.adsr(0.01,0.1,0.8,0.1),gate,doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(out, sig);
}).add;
);

(
Pdef(\pdef_env, Pbind(
	\instrument, \synthdef_env,
	\degree, Pseq([0],inf),
	\dur, 1,
	\amp, 0.1
)).play;
);


~p = Param(Pdef(\pdef_env), \freqenv, XEnvSpec(\freq!4, ControlSpec(0,2,\lin,0,0.1)));

~p.get; // default value
~p.get.asCompileString;
~p.get.asEnv.asCompileString;
~p.get == Env([150,550,250,1000],[0.1,0.8,0.3]); // give false, but it's true (float precision error ?)
~p.set( Env([1000,200,501,3200],[0.2,0.4,0.6]) ); // must reload the ndef to take effect :( ndef bug
~p.get == Env([1000,200,501,3200],[0.2,0.4,0.6]);
~p.normGet;
~p.normGet.asCompileString
~p.normSet(Env([0.4,0.200,0.7,0.5],[0.2,0.4,0.6]));

~makegui.({ ~p.asView; });
~p.spec

// default spec (not working)
~p = Param(Pdef(\pdef_env), \freqenv);
~p.spec
GUI.qt
Knob.new


