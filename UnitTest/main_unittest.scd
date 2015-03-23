
//{ arg test;
~test = UnitTest.new;

	(
	Ndef(\ndef_array, { arg pan=0, amp=0.1;
		var sig;
		sig = SinOsc.ar(\freq.kr(100!3));
		sig = Pan2.ar(sig, pan, amp).mean;
	}).play;
	);

	~p = Param(Ndef(\ndef_array), \freq, XArraySpec(\freq!3));

	~p.get;
	~test.assertEquals(~p.get, ( 101!3 ));
	~p.set([100,200,300]);
	~test.assertEquals(~p.get, [100,200,300]);
	Ndef(\ndef_array).stop;
//}
Window.closeAll
