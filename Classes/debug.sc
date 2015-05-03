
//////////////////////// Debug
// why TestDependant does not show any argument ? :(

TestDependant2 {
	update { arg ... args;
			(args.asCompileString ++ " was changed.\n").post;
	}
}
