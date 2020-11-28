
Psched {
	*new { arg time, pat;
		var start, end;

		^if(time.isSequenceableCollection and: { time.size == 2 }) {
			start = time[0];
			end = time[1];
			if(end.sign == -1) {
				end = start + time[1].neg
			};
			Pseq([
				Event.silent(start),
				Pfindur(end - start, pat),
			]);
		} {
			start = time;
			Pseq([
				Event.silent(start),
				pat
			]);
		};
	}
}

PschedMute {
	// FIXME: maybe time should be absolute, not relative
	*new { arg time, pat;
		var start, end;
		var muter = Pbind(
			\type, Pstep(
				Pseq([
					\rest, 
					Pkey(\type).keep(1)
				],inf),
				time
			)
		);
		if(pat.notNil) {
			muter = muter <> pat;
		};
		^muter

		//^if(time.isSequenceableCollection and: { time.size == 2 }) {
			//start = time[0];
			//end = time[1];
			//if(end.sign == -1) {
				//end = start + time[1].neg
			//};
			//Pseq([
				//Event.silent(start),
				//Pfindur(end - start, pat),
			//]);
		//} {
			//start = time;
			//Pseq([
				//Event.silent(start),
				//pat
			//]);
		//};
	}
}
