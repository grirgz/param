
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

CursorTimeline {
	// represent a slice of time with start and end position
	var <startPosition;
	var <endPosition;
	startPosition_ { arg startPos;
		startPosition = startPos;
		this.swapIfNegative;
		this.changed(\startPosition, startPosition);
		this.changed(\refresh);
	}

	swapIfNegative {
		if(endPosition.notNil and: { startPosition.notNil }) {
			if(endPosition < startPosition) {
				var x;
				x = startPosition;
				startPosition = endPosition;
				endPosition = x;
			}
		}
	}

	endPosition_ { arg endPos;
		endPosition = endPos;
		this.swapIfNegative;
		this.changed(\endPosition, startPosition);
		this.changed(\refresh);
	}
}

