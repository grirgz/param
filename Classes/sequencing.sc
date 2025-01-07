////////// sequencer patterns

PseqCursor : Prout {
	// TODO: find a way to cleanup when stopped
	*new { arg list;
		^super.new({
			var previous;
			list.size.do { arg x;
				list.changed(\cursor, x, 0);
			};
			loop {
				//list.size.do { arg x;
				list.do { arg elm, idx;
					list.changed(\cursor, previous, 0);
					list.changed(\cursor, idx, 1);
					previous = idx;
					elm.embedInStream;
				};
			}
		})
	}
}

PstepSeq : ListPattern {
	var <>offset;
	*new { arg list, repeats=1, offset=0;
		^super.new(list, repeats).offset_(offset)
	}

	list_ { arg val;
		list = val;
		this.changed(\list); // for update in gui
	}

	embedInStream {  arg ev;
		var item, offsetValue;
		var i;
		repeats.do {
			i = 0;
			block { arg break;
				this.changed(\cursor, nil, 0); // turn off all cells
				this.list.changed(\cursor, nil, 0); // turn off all cells
				loop {
					if( this.list[i].notNil ) {
						// cursor following 
						this.changed(\cursor, (i-1).wrap(0,this.list.size-1), 0);
						this.list.changed(\cursor, (i-1).wrap(0,this.list.size-1), 0);
						this.changed(\cursor, i, 1, this.list[i]);
						this.list.changed(\cursor, i, 1, this.list[i]);

						ev = this.list[i].yield(ev);
					} {
						break.value;
					};
					i = i + 1;
				};
			};
		};
		i = 0;
		this.changed(\cursor, nil, 0); // turn off all cells
		this.list.changed(\cursor, nil, 0); // turn off all cells
	}

    asParam { 
		^Param(Message(this), \list)
    }
	//storeArgs { ^[ list, repeats, offset ] }
}

PdrumStep : Pattern {
	var <dict, <>score, <>repeats, <>default, <>key, <>isRestFunction;
	var <>streamDict;
	var silentEvent;
	*new { arg dict, score, repeats=inf, default, key=\midinote, isRestFun;
		^super.newCopyArgs(dict, score, repeats, default, key).initPdrumStep(isRestFun);
	}

	initPdrumStep { arg isRestFun;
		isRestFunction = isRestFun ? { arg x; x.class == Symbol };
		silentEvent = (isRest:true, type: \rest);
		streamDict = IdentityDictionary.new;
	}

	storeArgs { ^[dict,score,repeats,default, key ] }

	dict_ { arg val;
		dict = val;
		streamDict = IdentityDictionary.new;
	}

	dictStream { arg idx;
		if(streamDict[idx].isNil) {
			var item = dict[idx];
			[item, dict].debug("dictStream");
			if(item.notNil) {
				if(item.class == Event) { // debug only
					streamDict[idx] = item.as_stream;
				} {
					streamDict[idx] = item.asStream;
				}
			} {
				// FIXME: if the score says it's not a rest, this will not be a rest :(
				streamDict[idx] = ( default ?? {  silentEvent } ).asStream;
			};
		};
		^streamDict[idx];
	}

	dictNext { arg idx, ev;
		ev = ev ?? { () };
		^this.dictStream(idx).next(ev)
	}

	embedInStream { arg inval;
		var scoreStream, note;
		var ev = inval; // i don't know what i'm doing..
		repeats.value(ev).do({
			scoreStream = score.asStream;
			scoreStream.do( { arg scoreev;
				var pat;
				var kitIndex;
				if(scoreev.isNil) { 
					//"RETRUN".debug; 
					^ev
				};
				if(scoreev.isNumber) {
					scoreev = (isRest:true, dur:scoreev)
				};
				kitIndex = scoreev.use {  currentEnvironment[key].value };
				if(kitIndex.isNil or: {isRestFunction.(kitIndex)}) {
					ev = silentEvent.composeEvents(scoreev).yield(ev);
					//ev = silentEvent.composeEvents(scoreev).yield(ev);
				} {
					var padevs;
					var xscoreev = scoreev.copy;
					kitIndex = kitIndex.asInteger;
					//[scoreev, key, kitIndex, ev].debug("midinote (or specified key)");
					xscoreev[key] = nil;
					padevs = this.dictStream(kitIndex).next(ev);
					//~padevs = padevs;
					//padevs.debug("padevs");
					if(padevs.isSequenceableCollection.not) {
						padevs = [padevs]
					};
					//padevs = padevs.collect({ arg x; Event.newFrom(x) }); // FIXME: convert StepEvent to Event as a workaround because when embedInStream the StepEvent is repeated instead of one shot (see PatKitDef doc)
					//padevs.debug("padevs as event"); 
					padevs.do{ arg padev, x;
						if(x == ( padevs.size-1 )) {
							ev = padev.composeEvents(xscoreev).yield(ev);
							//ev = padev.composeEvents(xscoreev).yield(ev);
						} {
							var sc = xscoreev.copy;
							sc[\delta] = 0;
							ev = padev.composeEvents(sc).yield(ev);
							//ev = padev.composeEvents(sc).yield(ev);
						};
					};
				}
			}, ev);
		});
		^ev
	}
}


