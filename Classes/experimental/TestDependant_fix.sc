
+ TestDependant {
	// the base class is not usefull because it don't post whole argument list
	update { arg ...things;
		(things.asString ++ " was changed.\n").post;
	}
}
