
KitTimeline {
	*new { arg ... args;
		ParamProto.init;
		^~kitTimeline.new(*args)
	}
}

NoteTimeline {
	*new { arg ... args;
		ParamProto.init;
		^~noteTimeline.new(*args)
	}
}

ClipTimeline {
	*new { arg ... args;
		ParamProto.init;
		^~clipTimeline.new(*args)
	}
}

TrackTimeline {
	*new { arg ... args;
		ParamProto.init;
		^~trackTimeline.new(*args)
	}
}
