
"Init base.scd".postln;


~pdemul = { arg pat;
	Prout({ arg proutinval;
		var str;
		str = pat.asStream;

		"what".debug;
		pat.debug;
		inf.do{ 
			var inval = str.next(proutinval);
			inval.debug("inval");
			inval.drop(-1).do { arg in;
				var in1 = in.copy;
				"hhh".debug;
				in1[\delta] = 0;
				in1.yield;
			};
			inval.last.debug("what").yield(Event.default);
		}
	}).trace
};
