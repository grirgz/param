
KitTimeline {
	*new { arg ... args;
		ParamProto.init;
		^topEnvironment[\kitTimeline].new(*args); // FIXME: should maybe use ProtoDef(\kitTimeline)
	}
}

NoteTimeline {
	*new { arg ... args;
		ParamProto.init;
		^topEnvironment[\noteTimeline].new(*args)
	}
}

ParamTimeline {
	*new { arg ... args;
		ParamProto.init;
		^topEnvironment[\envTimeline].new(*args)
	}
}

ClipTimeline {
	*new { arg ... args;
		ParamProto.init;
		^topEnvironment[\clipTimeline].new(*args)
	}
}

TrackTimeline {
	*new { arg ... args;
		ParamProto.init;
		^topEnvironment[\trackTimeline].new(*args)
	}
}
