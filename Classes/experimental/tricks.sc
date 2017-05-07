
// EnvInit(\yep, {21}) is equivalent to ~yep = ~yep ?? { 21 }
EnvInit {
	*new { arg key, val;
		if(currentEnvironment[key].isNil) {
			currentEnvironment[key] = val;
		};
		^currentEnvironment[key];
	}
}

