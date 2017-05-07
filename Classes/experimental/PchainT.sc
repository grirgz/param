PchainT : Pattern {
        var <>eventPattern, <>eventListPattern;
        *new { |eventPattern, eventListPattern|
                ^super.newCopyArgs(eventPattern, eventListPattern);
        }
        embedInStream { arg inval;
                var eventStream, eventListStream, inevent, nextEvent, nextEventList,
                        outEventList, cleanup = EventStreamCleanup.new;

                eventStream = eventPattern.asStream;
                eventListStream = eventListPattern.asStream;
                loop {
                        inevent = inval.copy;
                        nextEventList = eventListStream.next(());
                        nextEvent = eventStream.next(());
                        outEventList = nextEventList.collect { |ev, i|
                                if(inevent.isNil) { ^cleanup.exit(inval) };
                                inevent = inevent.composeEvents(ev);
                                if(inevent.isNil) { ^cleanup.exit(inval) };
                                inevent = inevent.composeEvents(nextEvent);
                                if(inevent.isNil) { ^cleanup.exit(inval) };
                                inevent
                        };
                        cleanup.update(inevent);
                        inval = yield(outEventList);
                };
        }
}

// we need playAndDelta for event lists
// important assumption: dur is equal for all events of a list !!

+SequenceableCollection {
        playAndDelta { | cleanup, mute |
                if (mute) { this.put(\type, \rest) };
              cleanup.update(this);
                this.do { |x| x.play };
                ^this.first.delta;
        }

		//asEvent {
		//	var val;
		//	val = this.first.copy;
		//	if(val.delta.isNil) {
		//		val.delta = 1;
		//	};
		//	^val.debug("asEvent=========")
		//}
		//asEvent {
		//	^this.first;
		//}
}


