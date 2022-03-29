
+ TestDependant {
	// the base class is not usefull because it don't post whole argument list
	update { arg obj, msg ...args;
		("TestDependant receive signal % from object % with args %".format(msg.asCompileString, obj, args)).postln;
	}
}
