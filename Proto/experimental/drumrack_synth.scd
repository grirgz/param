


///////////////// some example synths

s.boot;

SynthDef(\plop, { arg out=0, amp=0.1, gate=1, pan=0, freq=200;
	var sig;
	sig = LFSaw.ar(freq * [1,1.01,1.001, 1+0.1.rand]);
	sig = RLPF.ar(sig, \lpf.kr(400), \rq.kr(0.1));
	sig = sig * EnvGen.ar(Env.adsr(0.01,0.1,0.8,0.4),gate,doneAction:2);
	sig = Pan2.ar(sig, pan, amp).flop;
	Out.ar(out, sig);
}).add;

Pdef(\plop1, Pbind(
	\instrument, \plop,
	\degree, Pseq([0,4,5,8],inf),
	\dur, 1/8,
	\amp, 0.1
));

Pdef(\plop2, Pbind(
	\instrument, \default,
	\degree, Pseq([0,2],inf),
	\octave, 3,
	\dur, 1/2,
	\amp, 0.1
));

Pdef(\plop3, Pbind(
	\instrument, \default,
	\degree, Pseq([0,7,7,7,7,2],inf),
	\octave, 5,
	\dur, 1/4,
	\amp, 0.1
));


/////////////////////////// drumrack base instrs

EventPrototype.initPrototypes(true);

SynthDef(\dr_kick, { |out=0, amp=0.1, pan=0, fratio=1.5, freqpan=0.01, spread=0.08, timeScale=1|
	var env0, env1, env1m, son;

	//freqpan = [-1,0,1] * freqpan;
	freqpan = [1,0,-1] * freqpan;
	env0 = \env0.kr(Env.new([0, 1, 0.5, 0], [0.105, 0.16, 00.2], [-4, -2, -4]));
	env1 = \env1.kr(Env.new([110, 32, 29], [0.105, 0.29], [-4, -5]));
	
	env0 =  EnvGen.ar(env0, doneAction:2, timeScale:timeScale);
	env1 = EnvGen.ar(env1, timeScale:timeScale);
	env1m = env1.midicps;
	
	son = LFPulse.ar(env1m * freqpan, 0, 0.5, 1, -0.5);
	son = son + WhiteNoise.ar(1);
	son = LPF.ar(son, env1m*fratio, env0);
	son = son + SinOsc.ar(env1m, 0.5, env0);
	
	//son = son * 1.2;
	//son = son.clip2(1);

	son = son * amp;
	son = Splay.ar(son, spread, amp, pan);
	
	OffsetOut.ar(out, son);
}, metadata:(specs:(
	// FIXME: disabled because error ^^ The preceding error dump is for ERROR: Message 'setFrom' not understood.

	//env0: ParamEnvSpec( ControlSpec(0,9,\lin)!4, ControlSpec(0,2,\lin)!4 ),
	//env1: ParamEnvSpec( ControlSpec(0,127,\lin)!3, ControlSpec(0,2,\lin)!3 ),
	timeScale: ControlSpec(0.001,4,\lin, 0, 1),
))).store;

SynthDef(\dr_oscsampler, { arg out=0, amp=0.1, gate=1, pan=0, freq=200, bufnum, pos=0, finepos=0, range=0,
						lpmix=0.5, ffreq=500, rq=0.1, spread=0.8, fshift=0;
	// env longer
	var ou, ou2;
	var osc;
	var bufsig;
	var phase;
	var ffreqtab;
	osc = SinOsc.ar(freq*[1.001,0.999,1]);
	//osc = osc.sum;
	pos = EnvGen.kr(\posenv.kr(Env([0.5,0.6,0.5,0.2, 0.2], [0.2,0.1,0.1,0.1])),gate, levelScale:\levelScale.kr(1), timeScale:\timeScale.kr(1)) + pos;
	range = (range + (\framerange.kr(1)/BufFrames.ir(bufnum)));
	phase = osc * range + pos + finepos.lag(\fineposlag.kr(0));

	bufsig = BufRd.ar(2, bufnum, phase*BufFrames.ir(bufnum), 1);
	bufsig = bufsig.sum;
	ou = bufsig;
	ffreqtab = \ffreqtab.kr(1!8);
	ou = FreqShift.ar(ou, fshift);
	ou2 = RLPF.ar(ou, (ffreq * ffreqtab).clip(10,18000), rq);
	ou = SelectX.ar(lpmix, [ou, ou2]);
	ou2 = HPF.ar(ou, (\hpfreq.kr(180) * ffreqtab).clip(10,18000));
	ou = SelectX.ar(\hpmix.kr(0), [ou, ou2]);

	ou2 = (ou * \distpreamp.kr(18)).tanh * \distamp.kr(1);
	ou = SelectX.ar(\distmix.kr(0), [ou, ou2]);
	ou = Splay.ar(ou, spread, amp, pan);

	ou = ou * EnvGen.ar(\adsr.kr(Env.adsr(0.01,0.1,0.8,0.1)),gate,doneAction:2);
	
	Out.ar(out, ou);
}, metadata:(specs:(
	finepos: ControlSpec(-0.001,0.001,\lin, 0, 1),
	range: ControlSpec(-0.01,0.01,\lin, 0, 1),
	adsr: ParamEnvSpec.adsr( ControlSpec(0,2,\lin) ),
	posenv: ParamEnvSpec(\unipolar!5, ControlSpec(0,1,\lin)),
	fshift: ControlSpec(-2000,2000,\lin,0,0),
	spread: \unipolar.asSpec,
	bufnum: ParamSampleSpec.new,
	pos: \unipolar.asSpec,
	lpmix: \unipolar.asSpec,
	ffreq: \freq.asSpec,
	ffreqtab: ParamArraySpec( ControlSpec(1/8,8,\exp)!8 ),
	hpmix: \unipolar.asSpec,
	hpfreq: \freq.asSpec,
	distamp: \amp.asSpec,
	distpreamp: ControlSpec(0,100,\lin),
	distmix: \unipolar.asSpec,
))).store;

//SynthDesc(\dr_kick).specs
//SynthDesc(\dr_kick).params.do(_.postln)
//SynthDesc(\dr_kick).controls[8].name.dump
//SynthDesc(\dr_kick).defaultValue(\timeScale)
//SynthDesc(\dr_kick).test



	////////////////
	DrumRack(\init); // FIXME: should not need this
	DrumRack.addInstr(SynthDesc(\dr_kick), SynthDesc(\dr_kick).params ++ [[ \sustain, ControlSpec(0.05,16,\exp) ]] );

	DrumRack.addInstr(SynthDesc(\dr_oscsampler), SynthDesc(\dr_oscsampler).params ++ [[ \sustain, ControlSpec(0.05,16,\exp) ]] );

	Pdef(\dr_oscsampler_table_old, Pbind(
		\instrument, \dr_oscsampler,
		//\bufnum, BufDef(\table, "field/table.flac"),
		\bufnum, BufDef(\amen, "vipere/amenbreak.wav");
		//\bufnum, BufDef(\metal, "vipere/metal.flac");
	)).addHalo(\instrument, \dr_oscsampler);

	Pdef(\dr_oscsampler_table, Pbind(
		\instrument, \dr_oscsampler,
		//\bufnum, BufDef(\table, "field/table.flac"),
		\bufnum, BufDef(\amen, "vipere/amenbreak.wav");
		//\bufnum, BufDef(\metal, "vipere/metal.flac");
	))
	.addHalo(\instrument, \dr_oscsampler)
	.addHalo(\params, SynthDesc(\dr_oscsampler).params ++ [[ \sustain, ControlSpec(0.05,16,\exp) ]] );
	DrumRack.addInstr(Pdef(\dr_oscsampler_table));
