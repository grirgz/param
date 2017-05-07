
Ptask {
	*new { arg fun, time=0;
		^Prout({
			Task(fun).play;
			Event.silent(time).yield;
		})
	}
}

