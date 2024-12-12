

ClipTimeline {
	//classvar <>proto;

	//*all { // needed by clipEditor if no env var defined
		//^proto.all
	//}

	*new { arg ... args;
		ParamProto.init;
		//^proto.new(*args);
		^ProtoClassDef(\ClipTimeline).new(*args)
	}
}

//NoteTimeline {
	//*new { arg ... args;
		//ParamProto.init;
		//^ProtoClassDef(\NoteTimeline).new(*args)
	//}
//}

// this design allow proto classes to answer correctly .isKindOf(NoteTimeline) instead of using .eventType
// maybe also make them herits Timeline or BaseTimeline (because Timeline is a too generic name) to test if this is a kind of timeline
//	this allow to know that some methods are shared accross all timelines
// FIXME: this create a new instance each time and break IdentitySet
NoteTimeline : ProtoClass {
	*new { arg ... args;
		//var inst, proto;
		//ParamProto.init;
		//inst = super.new;
		//proto = ProtoClassDef(\NoteTimeline).new(*args);
		//inst.putAll(proto);
		//inst.parent = proto.parent;
		//^inst;
		ParamProto.init;
		^ProtoClassDef(\NoteTimeline).new(*args);
	}

	*newFromProto { arg proto;
		var inst;
		ParamProto.init;
		inst = super.new;
		inst.putAll(proto);
		inst.parent = proto.parent;
		^inst;
	}
}

KitTimeline {
	*new { arg ... args;
		ParamProto.init;
		^ProtoClassDef(\KitTimeline).new(*args);
	}
}

ParamTimeline {
	*new { arg ... args;
		ParamProto.init;
		^ProtoClassDef(\ParamTimeline).new(*args)
	}
}

SampleTimeline {
	classvar <>proto;

	*all { // needed by clipEditor if no env var defined
		^proto.all
	}

	*new { arg ... args;
		ParamProto.init;
		^proto.new(*args);
		//^topEnvironment[\sampleTimeline].new(*args)
	}
}

TrackTimeline {
	*new { arg ... args;
		ParamProto.init;
		^ProtoClassDef(\TrackTimeline).new(*args)
	}
}

CursorTimeline {
	// represent a slice of time with start and end position
	var startPosition; // scalar in grid units (beats)
	var endPosition;
	var <startPoint; // point in grid units (for selection)
	var <endPoint;
	var <>model; // eventlist or object who send \cursor signals and has startTime endTime and firstTime
	var <>clock;
	var <>controller;
	var <>enableSwapIfNegative = true;
	var <>loopMaster;
	var <>startTime; // set by loopMaster cursor start event

	startPosition {
		^(startPoint !? _.x);
	}

	endPosition {
		^(endPoint !? _.x);
	}

	startPoint_ { arg point;
		startPoint = point;
		this.swapIfNegative;
		this.changed(\startPosition, startPosition);
		this.changed(\refresh);
	}

	endPoint_ { arg point;
		endPoint = point;
		this.swapIfNegative;
		this.changed(\startPosition, startPosition);
		this.changed(\refresh);
	}

	rect {
		^Rect.fromPoints(this.startPoint, this.endPoint)
	}

	rect_ { arg rect;
		startPoint = rect.origin;
		endPoint = rect.rightBottom;
		this.swapIfNegative;
		this.changed(\startPosition, startPosition);
		this.changed(\refresh);
	}

	setPositions { arg startPos, endPos;
		// to avoid swapping start and end in the middle of setting them
		if(startPos.isNil) {
			startPoint = nil;
		} {
			if(startPoint.isNil) {
				startPoint = Point(0,0);
			};
			startPoint.x = startPos;
		};
		if(endPos.isNil) {
			endPoint = nil;
		} {
			if(endPoint.isNil) {
				endPoint = Point(0,0);
			};
			endPoint.x = endPos;
		};
		this.swapIfNegative;
		this.changed(\startPosition, startPosition);
		this.changed(\endPosition_, startPosition);
		this.changed(\refresh);

	}

	startPosition_ { arg startPos;
		if(startPos.isNil) {
			startPoint = nil;
		} {
			if(startPoint.isNil) {
				startPoint = Point(0,0);
			};
			startPoint.x = startPos;
		};
		this.swapIfNegative;
		this.changed(\startPosition, startPosition);
		this.changed(\refresh);
	}

	endPosition_ { arg endPos;
		if(endPos.isNil) {
			endPoint = nil;
		} {
			if(endPoint.isNil) {
				endPoint = Point(0,0);
			};
			endPoint.x = endPos;
		};
		this.swapIfNegative;
		this.changed(\endPosition, startPosition);
		this.changed(\refresh);
	}

	swapIfNegative {
		if(enableSwapIfNegative == true) {
			if(this.endPosition.notNil and: { this.startPosition.notNil }) {
				if(this.endPosition < this.startPosition) {
					var x;
					x = this.startPosition;
					this.startPosition = this.endPosition;
					this.endPosition = x;
				}
			}
		}
	}

	mapEventList { arg eventlist;
		// TODO: use it
		model = eventlist;
		if(controller.notNil) {controller.remove};
		controller = SimpleController(model).put(\cursor, { arg obj, msg, arg1;
			if(arg1 == \play) {
				this.changed(\play);
			} {
				this.changed(\stop);
			}
		});
		controller.put(\redraw, {
			this.changed(\redraw);
		});
	}

	computeLoop {
		/// TODO: use this wonderful code in timeline.asPattern and cursor view
		var absLoopStart, absLoopEnd, absStart, absEnd;
		absStart = this.model.startTime;
		absEnd = this.model.endTime;
		absLoopStart = this.startPosition ? absStart;

		// prevent too short loop
		if(this.endPosition.notNil and: {  
			this.startPosition.notNil
			and: {
				this.endPosition - this.startPosition < 0.1
			}
		}) {
			absLoopEnd = absEnd;
		} {
			absLoopEnd = this.endPosition ? absEnd; // no real time modification of end time
		};

		// prevent too short loop with endtime
		if(absLoopEnd - absLoopStart < 0.1) {
			Log(\Param).debug("CursorTimelineView: too short loop, start % end %", absLoopStart, absLoopEnd);
			absLoopEnd = absLoopStart + 1;
		};

		if(absEnd - absStart < 0.1) {
			Log(\Param).debug("CursorTimelineView: too short clip, start % end %", absStart, absEnd);
			absEnd = absStart + 1;
		};
		^(
			absLoopStart: absLoopStart,
			absLoopEnd: absLoopEnd,
			totalLoopDur: absLoopEnd - absLoopStart,
			startLoopOffsetFromFirstEvent: absLoopStart - this.model.firstTime,

			absStart: absStart,
			absEnd: absEnd,
			totalDur: absEnd - absStart,
			startLoopOffsetFromFirstEvent: absStart - this.model.firstTime,
		)
	}
}

