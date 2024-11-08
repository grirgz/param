
// test weither Bus.asMap should return array in case of multichannel bus
(
SynthDef(\zed, { arg out=0, gate=1, amp=0.1, pan=0, freq=200, modbus=#[0,0], modbus2=0;
	var sig;
	var mod;
	mod = Select.ar( LFPulse.kr(1).unipolar, [modbus, modbus1] );
	sig = SinOsc.ar(freq * modbus);
	sig = sig * EnvGen.kr(\adsr.kr(Env.adsr(0.1,0.1,0.8,0.1)), gate, doneAction:2);
	sig = Pan2.ar(sig, pan, amp);
	Out.ar(out, sig);
}).add;
);
