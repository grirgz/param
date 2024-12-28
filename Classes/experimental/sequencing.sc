// kind of deprecated or never really used
// but still lot of reference in code so can't remove now

//////////// sequencer models

StepList : List {
	var <>bypass = 0;
	var <>coinMode = false;

	// TODO: send changed message when .put or other

	embedInStream { arg ev;
		^this.asPattern.embedInStream(ev);
	}


	asPattern {
		if(bypass == 1) {
			^nil
		} {
			var ret;
			ret = PstepSeq(this);
			if(coinMode == true) {
				ret.coin.not;
			} {
				ret;
			};
			^ret;
		}
	}

	asParam {
		^Param(this, \list)
	}

	edit {
		Param(this, \list).edit
	}

	prest { 
		// TODO: deprecate this
		// TODO: find better name
		^this.asPattern.coin.not
	}

	stepCount_ { arg val;
		this.array = this.array.wrapExtend(val.asInteger);
		this.changed(\refresh);
		this.changed(\set, [\stepCount]);
	}

	stepCount {
		^this.size;
	}

	spec {
		^this.getSpec(\list)
	}

	spec_ { arg sp;
		this.addSpec(\list, sp);
	}

	clone {
		var new = this.deepCopy;
		//Halo.put(new, Halo.at(this).deepCopy);
		new.addHalo(\seqstyle, this.getHalo(\seqstyle));
		new.addSpec(\list, this.getSpec(\list));
		^new;
	}

	isNaN { ^true } // TODO: document why this method

}

StepListDef {
	// TODO
	var steplist;
	*new { arg key, val; 
		Pdefn(key, StepList.new)
	}
}

StepEvent : Event {
	var <>repeats = 1;
	var <player;

	//initPbindProxy

	asParamGroup {
		var instr;
		var specgroup;
		instr = this.getHalo(\instrument) ? this.instrument;
		if(instr.notNil and: {SynthDesc(instr).notNil}) {
			^SynthDesc(instr).asParamGroup(this);
		} {
			"error: no instrument found: %".format(instr).postln;
			^nil;
		}
	}

	embedInStream { arg ev;
		repeats.do {
			ev = this.embedInStreamOnce(ev);
		};
		^ev;
	}

	embedInStreamOnce { arg ev;
		var pairs = List.new;
		var pbind;
		this.keysValuesDo { arg key, val;
			if(key == \isRest) {
				// FIXME: to much assumption on how rest are handled
				// currently, if a key is isRest, it's a coin pattern with button style
				// could be \amp == 0 too!
				// maybe isRest could be a function looking if \step != 0 is defined and if \amp != 0
				// need to add a filter on values, excluding functions in the view
				// and not call asPattern on it
				pairs.add(key);
				//pairs.add(val.addHalo(\seqstyle, \button).prest);
				pairs.add(val.prest);
			} {
				var pat;
				pat = Prout({ arg inevent;
					var rep = true;
					while { rep = true  } {

						case
							{ val.isKindOf(StepList) } {
								val.embedInStream(inevent);
								rep = false;
							}
							{ val.isKindOf(Event) and: { val[\eventType] == \paramTimeline } } {
								val.outBus.asMap.yield;
								rep = true;
							}
							{ val.isKindOf(Number) or: { val.isKindOf(Symbol) } } {
								this[key].yield;
								rep = true;
							}
							// not a good idea, how to set \out bus then ?
							//{ val.isKindOf(Bus) } {
							//	val.asMap.yield;
							//}
							{ val.isKindOf(Pattern) } {
								val.embedInStream(inevent);
								rep = false;
							}
							{
								val.yield;
								rep = true;
							}
						;
					}
				});
				if(pat.notNil) {
					pairs.add(key);
					pairs.add(pat); // FIXME: what if already a pattern ?
				}
			}
		};
		pbind = Pbind(
			*pairs
			//++ [\what, Pfunc({ arg ev; ev.debug("WTF!"); pairs.clump(2).do { arg x; 
			//	x.debug("list") };
			//	1
			//})]
		);
		if(pairs.size == 0) { // avoid infinite loop when empty because not infinite when not empty
			pbind = pbind.keep(1);
		};
		ev = pbind.embedInStream(ev);
		//ev.debug("ev: end");
		^ev;
	}

	asPattern {
		^Prout({ arg ev; 
			this.embedInStream(ev)
		})
	}

	asSidePattern {
		^Plazy({
			var list = List.new;
			this.keys.asArray.do({ arg key;
				var val = this[key];
				if(val.isKindOf(Event) and: { val.eventType == \paramTimeline }) {
					list.add(val.xasPattern);
				};
			});
			if(list.size == 0) {
				nil // can be inifinite zero loop if looped infinitely
				// need nilsafe by james harkins
			} {
				Ppar(list);
			};
		});
		//}).repeat(repeats)
	}

	setSize { arg val;
	}

	stepCount_ { arg val;
		this.keysValuesDo { arg k, v;
			if(v.isKindOf(StepList)) {
				v.stepCount = val;
			}
		};
		this.changed(\set, [\stepCount]);
	}

	clone { 
		^this.collect({ arg x;
			if(x.respondsTo(\clone)) {
				x.clone;
			} {
				x.deepCopy;
			}
		})
	}

	stepCount {
		this.keysValuesDo { arg k, v;
			if(v.isKindOf(StepList)) {
				^v.stepCount;
			}
		};
		^1
	}

	play {
		player = this.asPattern.play;
	}

	stop {
		if(player.notNil) {
			player.stop;
			player = nil;
		}
	}

	asCompileString { 
		^"StepEvent.newFrom((%))".format(
			this.keys.as(Array).collect({ arg key, x;
				"%: %".format(key, this[key].asCompileString)
			}).join(", ")
		)
	}

	storeOn { arg stream;
		stream << this.asCompileString
	}

	printOn { arg stream;
		this.storeOn(stream);
	}
}

StepEventDef : StepEvent {
	classvar <>all;
	var <>key;
	var <>source;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { arg key;
		if(all[key].isNil) {
			^super.new.prAdd(key)
		} {
			var ret = all[key];
			^ret;
		}
	}

	prAdd { arg xkey;
		key = xkey;
		all[key] = this;
	}

	clear {
		if(key.notNil) {
			all[key] = nil
		};
		^nil
	}
}


PresetEvent : StepEvent {
	var >bypass;

	embedInStream { arg ev;
		var pairs = List.new;
		var pbind;
		this.keysValuesDo { arg k, v;
			if(bypass.notNil and: { bypass[k] != true }) {
				pairs.add(k);
				pairs.add(v);
			}
		};
		pbind = Pbind(
			*pairs
			//++ [\what, Pfunc({ arg ev; ev.debug("WTF!"); pairs.clump(2).do { arg x; 
			//	x.debug("list") };
			//	1
			//})]
		);
		pbind = pbind.keep(1); // force reread of event keys for each event
		ev = pbind.embedInStream(ev);
		//ev.debug("ev: end");
		^ev;
	}

	bypassKey { arg key, val=true;
		bypass = bypass ?? { IdentityDictionary.new };
		bypass[key] = val;
	}

	bypass { 
		bypass = bypass ?? { IdentityDictionary.new };
		^bypass;
	}

	asView {
		^ParamGroupLayout.two_panes(this.asParamGroup, \property)
	}

	edit {
		var window = Window.new;
		var layout;
		layout = VLayout(
			this.asView
		);
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
	}
}

BankList : List {
	var <index=0;

	index_ { arg val;
		index = val.clip(0,this.size-1);
		this.changed(\index);
	}

	asPattern {
		^Plazy({
			this[index].asPattern;
		})
	}

	current {
		^this[this.index]
	}

	current_ { arg val;
		this[this.index] = val;
		this.changed(\current);
	}

	keys {
		^(0..this.size-1)
	}

	embedInStream { arg ev;
		^this.asPattern.embedInStream(ev);
	}
}

ParBankList : List {

	index_ { arg val;
		this.do({ arg bank;
			bank.index = val;
		});
		this.changed(\index);
	}

	index { 
		^this.first.index;
	}

	asPattern {
		^Plazy({
			if(this.size > 0) {
				Ppar([
					this.collect({ arg bank;
						bank.current.asPattern;
					})
				]);
			} {
				Event.silent(1); // is it good ??
			}
		})
	}

	current {
		^this.collect({ arg bank; bank.current })
	}

	current_ { arg vals;
		this.do { arg bank, i; 
			bank[bank.index] = vals.wrapAt(i);
		};
	}

	keys {
		^(0..this.size-1)
	}

	embedInStream { arg ev;
		^this.asPattern.embedInStream(ev);
	}
}

ParBankDictionary : IdentityDictionary {

	index_ { arg val;
		this.do({ arg bank;
			bank.index = val;
		});
		this.changed(\index);
	}

	index { 
		^this[this.keys.asArray.first].index;
	}

	asPattern {
		^Plazy({
			if(this.size > 0) {
				Ppar([
					this.collect({ arg bank;
						bank.current.asPattern;
					})
				]);
			} {
				Event.silent(1); // is it good ??
			}
		})
	}

	//current {
	//	^this.collect({ arg bank; bank.current })
	//}

	//current_ { arg val_dict;
	//	this.do { arg bank, i; 
	//		bank[bank.index] = vals.wrapAt(i);
	//	};
	//}

	pageKeys {
		^this[this.keys.asArray.first].keys;
	}

	pageCount {
		^this[this.keys.asArray.first].size;
	}

	addPage { arg fun, select_it=true;
		this.keysValuesDo({ arg k, v;
			this[k].add(fun.value(k,v))
		});
		if(select_it == true) {
			this.index = this.pageCount-1;
		};
		this.changed(\pageCount);
	}

	removePage { arg idx;
		this.keysValuesDo({ arg k, v;
			this[k].removeAt(idx)
		});
		this.index = this.index.clip(0, this.pageCount-1);
		this.changed(\pageCount);
	}

	embedInStream { arg ev;
		^this.asPattern.embedInStream(ev);
	}

}

DictStepList : StepList {
	var >dict;

	dict {
		if(dict.isNil) {
			dict = BankList.new;
		};
		^dict;
	}

	asPattern {
		^Plazy({
			Pdict(this.dict,super.asPattern);
		})
	}
}

ParDictStepList : StepList {
	var >dicts;

	dicts {
		if(dicts.isNil) {
			dicts = List[BankList.new];
		};
		^dicts;
	}

	asValuePattern {
		^super.asPattern;
	}

	asPattern {
		^this.dicts.collect { arg dict;
			Plazy({
				Pdict(dict,super.asPattern);
			})
		};
	}
}

