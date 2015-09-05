PseqCursor : Prout {
	// TODO: find a way to cleanup when stopped
	*new { arg list;
		^super.new({
			var previous;
			list.size.do { arg x;
				list.changed(\cursor, x, 0);
			};
			loop {
				list.size.do { arg x;
					list.changed(\cursor, previous, 0);
					list.changed(\cursor, x, 1);
					previous = x;
					x.embedInStream;
				};
			}
		})
	}
}

//PeventListCursor : Pattern {
//	var <>pattern;
//	var <>model;
//
//	*new { arg pattern, model;
//		^super.new.pattern_(pattern).model_(model)
//	}
//
//	embedInStream { | event |
//
//		model.changed(\cursor, \play); // FIXME: this will be called even if the pattern is not played, not really cool
//		cleanup = EventStreamCleanup.new;
//		cleanup.addFunction(event, { 
//			model.changed(\cursor, \stop)
//		});
//		^event;
//	}
//
//}

PbindSeqDef : Pdef {
	var <>repeat;
	*new { arg key, xrepeat;
		var ins;
		var prout;
		//[key, Pdef(key).source].debug("souu");
		if(all[key].source.isNil or: { all[key].class == Pdef }) {
			Pdef(key).clear;
			all[key] = nil;
		//if(false) {
			ins = super.new(key);
			ins.repeat = (xrepeat ? 1);
			ins.repeat.debug("repeat!!");
			if(ins.envir.isNil) { ins.envir = ins.class.event };
			ins.repeat.debug("repeoat!!");
			prout = Prout({
				arg ev;
				var ret;
				var bind = List.new;
				var str;
			ins.repeat.debug("repeiat!!");
				ins.envir.keysValuesDo { arg key, val;
					[key, val].debug("kv PbindSeqDef");
					if(val.isSequenceableCollection) {
						//bind.add(Pseq(val[0].debug("what?")));
						bind.add(key);
						bind.add(Pseq(val[0],ins.repeat.debug("reeeppet")));
					} {
						//bind.add(Pseq([val],1));
					}
				};
				//ev.debug("ev");
				//bind.debug("bind");
				//str = Pbind(*bind).asStream;

				//while({ev.notNil}) {
				//	ev = str.next(ev);
				//	ev.yield;
				//};
				ev = Pbind(*bind).embedInStream(ev)
			});
			ins.source = prout;
			//Pdef(key).source.debug("souu2");
			//ins.source.debug("souu3");
			//ins.class.debug("class");
			all[key] = ins;
			^ins;
		} {
			"kk".debug;
			ins = super.new(key);
			if(xrepeat.notNil) {
				ins.repeat = xrepeat.debug("reeeprprperp");
			};
			^ins;
			//Pdef(key)
		};
	}

}

/////////////////////

PstepSeq : ListPattern {
	var <>offset;
	*new { arg list, repeats=1, offset=0;
		^super.new(list, repeats).offset_(offset)
	}

	embedInStream {  arg ev;
		var item, offsetValue;
		var i = 0;
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
		i = 0;
		this.changed(\cursor, nil, 0); // turn off all cells
		this.list.changed(\cursor, nil, 0); // turn off all cells
	}
	//storeArgs { ^[ list, repeats, offset ] }
}

//PstepSeq : Prout {
//	var <>list;
//	new { arg seq;
//		var ins;
//		ins = super.new({
//			Prout({ arg ev;
//				var i = 0;
//				block { arg break;
//					this.changed(\cursor, nil, 0); // turn off all cells
//					this.list.changed(\cursor, nil, 0); // turn off all cells
//					loop {
//
//
//						if( this.list[i].notNil ) {
//							// cursor following 
//							this.changed(\cursor, (i-1).wrap(0,this.list.size-1), 0);
//							this.list.changed(\cursor, (i-1).wrap(0,this.list.size-1), 0);
//							this.changed(\cursor, i, 1, this.list[i]);
//							this.list.changed(\cursor, i, 1, this.list[i]);
//
//							ev = this.list[i].yield(ev);
//						} {
//							i = 0;
//							break.value;
//						};
//						i = i + 1;
//					};
//				}
//			})
//
//		});
//		ins.list = seq;
//		^ins;
//	}
//
//    doesNotUnderstand { arg selector...args;
//        if(this.list.class.findRespondingMethodFor(selector).notNil) {
//			^this.list.perform(selector, * args);
//		};
//	}
//
//}

StepList : List {

	// TODO: send changed message when .put or other

	embedInStream { arg ev;
		^this.asPattern.embedInStream(ev);
	}

	asPattern {
		^PstepSeq(this)
	}

	asParam {
		^Param(this, \list)
	}

	edit {
		Param(this, \list).edit
	}

	prest { // TODO: find better name
		^this.asPattern.coin.not
	}

	stepCount_ { arg val;
		this.array = this.array.wrapExtend(val.asInteger);
		this.changed(\refresh);
	}

	stepCount {
		^this.size;
	}

	isNaN { ^true }

}

StepListDef {
	// TODO
	*new { arg key; 
		Pdefn(key, StepList.new)

	}
}

StepEvent : Event {

	embedInStream { arg ev;
		var pairs = List.new;
		var pbind;
		this.keysValuesDo { arg key, val;
			pairs.add(key);
			if(key == \isRest) {
				// FIXME: to much assumption on how rest are handled
				// currently, if a key is isRest, it's a coin pattern with button style
				// could be \amp == 0 too!
				// maybe isRest could be a function looking if \step != 0 is defined and if \amp != 0
				// need to add a filter on values, excluding functions in the view
				// and not call asPattern on it
				pairs.add(val.addHalo(\seqstyle, \button).prest);
			} {
				pairs.add(val.asPattern);
			}
		};
		pbind = Pbind(
			*pairs.debug("pairs")
			//++ [\what, Pfunc({ arg ev; ev.debug("WTF!"); pairs.clump(2).do { arg x; 
			//	x.debug("list") };
			//	1
			//})]
		);
		ev = pbind.embedInStream(ev);
		ev.debug("ev: end");
		^ev;
	}

	asPattern {
		^Prout({ arg ev; this.embedInStream(ev) });
	}


	setSize { arg val;
	}

	stepCount_ { arg val;
		this.keysValuesDo { arg k, v;
			if(v.isKindOf(StepList)) {
				v.stepCount = val;
			}
		};
	}

	stepCount {
		this.keysValuesDo { arg k, v;
			if(v.isKindOf(StepList)) {
				^v.stepCount;
			}
		};
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
	}

	keys {
		^(0..this.size-1)
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



//What : Pdef {
//	*new { arg key;
//		^super.new(key)
//	}
//}

//Ppredef : Pdef {
//	var <>preenvir;
//
//	source_ { arg obj;
//		if(obj.isKindOf(Function)) // allow functions to be passed in
//		{ pattern = PlazyEnvirN(obj) }
//		{ if (obj.isNil)
//			{ pattern = this.class.default }
//			{ pattern = obj }
//		};
//
//		if(preenvir.isNil) { preenvir = () };
//
//		envir !? { pattern = Pseq([preenvir],inf) <> pattern <> envir };
//		this.wakeUp;
//		source = obj;
//		this.changed(\source, obj);
//	}
//
//	set { arg ... args;
//		if(envir.isNil) { this.envir = this.class.event };
//		args.pairsDo { arg key, val; preenvir.put(key, val) };
//		this.changed(\set, args);
//	}
//
//	unset { arg ... args;
//		if(envir.notNil) {
//			args.do { arg key; preenvir.removeAt(key) };
//			this.changed(\unset, args);
//		}
//	}
//
//	get { arg key;
//		^if(preenvir.notNil) { preenvir[key] } { nil };
//	}
//
//
//}

Ppredef : Pdef {
	var initialPattern;

	source_ { arg obj;
		if(this.class == Pdef) {
			// FIXME: fuck this, there is no way to allow Subclasses of Pdef in the same dictionnary
			var env = this.envir;
			this.class.all[this.key] = Ppredef(this.key, obj);
			this.class.all[this.key].envir = env;
			^this.class.all[this.key];
		} {
			if(obj.isKindOf(Function)) // allow functions to be passed in
			{ pattern = PlazyEnvirN(obj) }
			{ if (obj.isNil)
				{ pattern = this.class.default }
				{ pattern = obj }
			};

			initialPattern = pattern;

			if(envir.isNil) { envir = () };

			envir !? { pattern = Pseq([envir],inf) <> pattern };
			this.wakeUp;
			source = obj;
			this.changed(\source, obj);
		};
	}

	default {
		if(initialPattern.isKindOf(Pbind)) {
			^initialPattern.patternpairs.asDict
		} {
			^()
		}
	}
}

PdrumStep : Pattern {
	var <dict, <>score, <>repeats, <>default, <>key, <>isRestFunction;
	var streamDict;
	var silentEvent;
	*new { arg dict, score, repeats=inf, default, key=\midinote, isRestFun;
		^super.newCopyArgs(dict, score, repeats, default, key).initPdrumStep(isRestFun);
	}

	initPdrumStep { arg isRestFun;
		isRestFunction = isRestFun ? { arg x; x.class == Symbol };
		silentEvent = (isRest:true, type: \rest, foudmagueul: \ouais);
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
		ev = ev ? ();
		^this.dictStream(idx).next(ev)
	}

	embedInStream { arg inval;
		var scoreStream, note;
		var ev = inval; // i don't know what i'm doing..
		repeats.value(inval).do({
			scoreStream = score.asStream;
			scoreStream.do( { arg scoreev;
				var pat;
				if(scoreev.isNil) { "RETRUN".debug; ^inval };
				if(scoreev[key].notNil) {
					if(isRestFunction.(scoreev[key])) {
						//ev = silentEvent.composeEvents(scoreev).debug("yieldrest").yield(ev);
						ev = silentEvent.composeEvents(scoreev).yield(ev);
					} {
						var padevs;
						var xscoreev = scoreev.copy;
						//scoreev[key].debug("midinote");
						xscoreev[key] = nil;
						padevs = this.dictStream(scoreev[key]).next(inval);
						//padevs.debug("padevs");
						if(padevs.isSequenceableCollection.not) {
							padevs = [padevs]
						};
						padevs.collect{ arg padev, x;
							if(x == ( padevs.size-1 )) {
								//ev = padev.composeEvents(xscoreev).debug("yield1").yield(ev);
								ev = padev.composeEvents(xscoreev).yield(ev);
							} {
								var sc = xscoreev.copy;
								sc[\delta] = 0;
								//ev = padev.composeEvents(sc).debug("yield2").yield(ev);
								ev = padev.composeEvents(sc).yield(ev);
							};
							ev;
						};
					}
				}
			}, inval);
		});
		^inval
	}
}

Pkeyd : Pattern {
	var	<>key, <>default;
	var <>repeats;
	*new { |key, default|
		^super.newCopyArgs(key, default)
	}
	storeArgs { ^[key] }
		// avoid creating a routine
	asStream {
		var	keystream = key.asStream;
		var	defaultstream = default.asStream;
		^FuncStream({ |inevent|
			inevent !? { inevent[keystream.next(inevent)] ? default.next(inevent) }
		});
	}
}

////////////////////////////////////////


DrumRack {
	// A drumrack proxy actually
	var <>key;
	var <drumrack;
	var <>pattern;
	var <>scoreproxy;
	var <>drumrackName;
	classvar <>lib_drumrack;
	classvar <>lib_drumpad;
	classvar <>lib_score;
	classvar <>lib_instr;
	classvar <>all;
	//var <>lib_drumrack; // in ~drumrack instance directly, maybe add a redirection
	//var <>lib_drumpad;
	//var <>lib_score;
	//var <>lib_instr;

	*initClass {
		Class.initClassTree(IdentityDictionary);
		Class.initClassTree(PresetDictionary);
		Class.initClassTree(XSamplePlaceholder);
		all = PresetDictionary.new(\DrumRack);
		// TODO: auto save/load presetdictionary
		lib_drumrack = PresetDictionary.new(\lib_drumrack);
		lib_drumpad = PresetDictionary.new(\lib_drumpad);
		lib_score = PresetDictionary.new(\lib_score);
		lib_instr = IdentityDictionary.new; // storing Pdef only for the moment

	}

	*new { arg name, val;
		var inst;
		this.initForEventClass;
		if(all[name].notNil) {
			inst = all[name];
			if(inst.drumrack.isNil and: { inst.drumrackName.notNil }) {
				inst.loadDrumrack(inst.drumrackName);
			}
		} {
			inst = super.new.init(name);
			all[name] = inst;
		};
		if(val.notNil) {
			inst.source = val;
		};
		^inst;
	}

	//*newFromName { arg name, drumrack_name; // to be used in load but I don't know if it's a good idea
	//	var inst;
	//	inst = super.new.initFromName(name, drumrack_name);
	//	all[name] = inst;
	//	if(val.notNil) {
	//		inst.source = val;
	//	};
	//	^inst;
	//
	//}

	*initForEventClass { // temporary
		//lib_instr.loadIfNotInitialized; 
		EventPrototype.initPrototypes; // used by drumpad

		lib_drumpad.loadIfNotInitialized;
		lib_drumrack.loadIfNotInitialized;
		lib_score.loadIfNotInitialized;
		all.loadIfNotInitialized;
	}

	init { arg name;
		key = name;
		scoreproxy = EventPatternProxy.new;
		pattern = PdrumStep([], scoreproxy);
		if(this.class.lib_drumrack[\default].notNil) { // this use class lib, there is no ~drumrack yet !
			// NOOP
		} {
			this.class.lib_drumrack[\default] = DrumRackManager.new;
		};
		this.loadDrumrack(\default);
	}

	//initFromName { arg name, drumrack_name;
	//	key = name;
	//	scoreproxy = PatternProxy.new;
	//	pattern = PdrumStep([], scoreproxy);
	//	this.initForEventClass;
	//	if(this.class.lib_drumrack[\default].notNil) { // this use class lib, there is no ~drumrack yet !
	//		// NOOP
	//	} {
	//		this.class.lib_drumrack[\default] = DrumRackManager.new;
	//	};
	//	this.loadDrumrack(\default);
	//}

	source_ { arg val;
		scoreproxy.source = val;
	}

	source {
		^scoreproxy.source
	}

	drumrack_ { arg val;
		drumrack = val;
		pattern.dict = val.pads;
	}

	//drumrack {
	//	this.class.lib_drumrack[drumrackName];
	//}

	//set_drumrack { arg val;
	//	// this is for back compat with event prototype
	//	this.drumrack = val;
	//}

	loadDrumrack { arg name;
		var dr = this.class.lib_drumrack[name];
		if(dr.isNil) {
			[key, name].debug("loadDrumrack: drumrack is nil!");
		} {
			[key, name].debug("loadDrumrack: Ok");
			this.drumrackName = name;
			this.drumrack = dr;
		}
	}

	*addInstr { arg instr, params;
		var name;
		switch(instr.class,
			Pdef, {
				name = instr.key;
				lib_instr[name] = instr;
				if(params.notNil) {
					Pdef(name).addHalo(\params, params);
				};
				if(Pdef(name).getHalo(\params).isNil and: { Pdef(name).getHalo(\instrument).notNil }) {
					var par = par ?? { 
						var ins;
						ins = Pdef(name).getHalo(\instrument);
						if(ins.notNil) {
							if(SynthDesc(ins).notNil) {
								SynthDesc(ins).params
							} 
						}
					};
					Pdef(name).addHalo(\params, par);


				};
			},
			SynthDesc, { // SynthDef
				name = instr.name.asSymbol;
				Pbindef(name, 
					\instrument, name
				);
				Pdef(name).addHalo(\instrument, name);
				if(params.notNil) {
					Pdef(name).addHalo(\params, params);
				} {
					Pdef(name).addHalo(\params, instr.params.select({ arg x; 
						if(x.isSequenceableCollection) {
							x = x[0];
						};
						[\out, \gate, \doneAction].includes(x).not; // should be filtered later with Specs
					}));
				};
				lib_instr[name] = Pdef(name);
			}
		);
		lib_drumpad[name] = ~class_presetgroup.new;  // DrumPad.new(name);
		lib_drumpad[name].add_preset( ~class_preset.new(name) );  // DrumPad.new(name);
	}


	asArchiveData {
		^(
			load: { arg self; 
				var inst;
				"1".debug;
				inst = DrumRack(self.name);
				"2".debug;
				inst.loadDrumrack(self.drumrack_name);
				"3".debug;
				inst;
			},
			name: key,
			drumrack_name: drumrackName,

		)
	}

	clear {
		all[key] = nil;
	}

	embedInStream {
		^pattern.embedInStream;
	}

	edit {
		^~class_drumrack_view.new(this);
	}

    doesNotUnderstand { arg selector...args;
		if(this.drumrack.class == Event) {
			if(this.drumrack[selector].notNil) {
				^this.drumrack.perform(selector, * args);
			}
		} {
			if(this.drumrack.respondsTo(selector)) {
				^this.drumrack.perform(selector, * args);
			}
		};
		DoesNotUnderstandError.new(this, selector, args).throw
	}
}

EventPrototype {
	classvar <>allPrototypes;
	var <>prototypeInstance;
	classvar <>initialized = false;

	*initClass {
		Class.initClassTree(Event);
		Class.initClassTree(Environment);
		Class.initClassTree(List);
		allPrototypes = List.new;
	}

	*initPrototypes { arg force=false;
		if(initialized.not or: force) {
			allPrototypes.do { arg protodata;
				protodata.file.load;
			};
			initialized = true;
		}
	}

	*eventPrototypeInitClass { arg protodata;
		allPrototypes.add(protodata);
	}

	*new { arg ... args;
		this.initPrototypes;
		^super.new;
	}

	init { arg instance;
		prototypeInstance = instance;
	}

    doesNotUnderstand { arg selector...args;
        if(prototypeInstance[selector].notNil) {
			^prototypeInstance.perform(selector, * args);
		};
	}

}

StepSeqManager : EventPrototype {
	*initClass {
		this.eventPrototypeInitClass((
			file: "/home/ggz/code/sc/seco/vlive/demo/param/lib/stepeditor.scd",
			name: \class_score_manager,
		));
	}

	*new { arg ... args;
		^super.new.init(~class_score_manager.new(*args))
	}
}

DrumRackManager : EventPrototype {
	*initClass {
		this.eventPrototypeInitClass((
			file: "/home/ggz/code/sc/seco/vlive/demo/param/lib/drumrack.scd",
			name: \class_drumrack,
		));
	}

	*new { arg ... args;
		^super.new.init(~class_drumrack.new(*args))
	}
}


StepSeq {
	classvar <>all;
	var <>stepseq;
	var <>key;

	*initClass {
		all = PresetDictionary.new(\StepSeq);
	}

	*new { arg name;
		all.loadIfNotInitialized;
		if(all[name].isNil) {
			all[name] = super.new.init(name, StepSeqManager.new);
		};
		^all[name];
	}

	init { arg name, score;
		key = name;
		stepseq = score;
	}

	edit {
		^stepseq.make_window;
	}

	getSpec { arg ... args;
		^stepseq.getSpec(*args)
	}

	addSpec { arg ... args;
		^stepseq.addSpec(*args)
	}

	setStepSpec { arg spec;
		stepseq.setStepSpec(spec)
	}

	asArchiveData { arg x;
		// return saved state
		^(
			load: { arg self; 
				var inst = StepSeq.new(self.name);
				inst.stepseq = self.stepseq.load;
				inst;
			},
			name: key,
			stepseq: stepseq.asArchiveData,
		);
	}

	*save {
		// save in Archive
		all.save;
	}

	patterns { ^this.stepseq.as_pattern }

	asCoinStep { arg ekey=\midinote, repeat=inf; // good name ?
		^Ppar(
			this.stepseq.as_pattern.collect({ arg pat, x;
				Pbind(
					\isRest, pat.coin.not,
					ekey, x,
				).loop;
			})
		).repeat(repeat);
	}

    doesNotUnderstand { arg selector...args;
		if(this.stepseq.notNil) {

			if(this.stepseq.class == Event) {
				if(this.stepseq[selector].notNil) {
					^this.stepseq.perform(selector, * args);
				}
			} {
				if(this.stepseq.respondsTo(selector)) {
					^this.stepseq.perform(selector, * args);
				}
			};
		};
		DoesNotUnderstandError.new(this, selector, args).throw
	}

}



PtimeGate : Pattern {
	// output continuously values from a pattern
	// independently of the chained pattern asking 10 or 100 values
	// until the specified time end
	var <>pattern, repeats;

	*new { arg pattern, repeats = 1;
		^super.newCopyArgs(pattern, repeats).init
	}

	init { 
		
	}

	embedInStream { arg subval;
		var stream, val,sustain;
		var subpat;
		var substr;
		var restdur;
		var dur;
		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
		[thisThread.endBeat, thisThread.beats].debug("beats");

		repeats.value(subval).do { | i |
			stream = pattern.asStream;
			while (
				{ val = stream.next(());
					val.notNil;
				},
				{
					//val.debug("val");
					sustain = val.use { val.sustain } ? 1;
					dur = val.use { val.dur } ? 1;
					restdur = dur - sustain; // max: 0;
					[subval, val, sustain, dur, restdur].debug("subval, val, sustain, dur, restdur");
					thisThread.endBeat = thisThread.endBeat + sustain;
					subpat = val.use { val.pattern ? (val.key !? { Pdef(val.key) }) };
					substr = subpat.asStream;
					while(
						{ 
							thisThread.endBeat > thisThread.beats and: {
								subval = substr.next(subval);
								subval.debug("while cond subval");
								subval.notNil;
							}
						},
						{ 
							//subval = subval.yield;
							subval = subval.debug("subval").yield;
						}
					);
					thisThread.endBeat = thisThread.endBeat + restdur;
					//subpat = Event.silent(restdur).loop;
					subpat = (isRest: true, dur:restdur).loop;
					substr = subpat.asStream;
					while(
						{ 
							thisThread.endBeat > thisThread.beats and: {
								subval = substr.next(subval);
								subval.notNil;
							}
						},
						{ 
							//subval = subval.yield;
							subval = subval.debug("subval: rest").yield;
						}
					)
				});
		};
		^subval;
	}

	storeArgs {
		^[pattern, repeats]
	}
}

PtimeGatePunch_old : Pattern {
	// output continuously values from a pattern
	// independently of the chained pattern asking 10 or 100 values
	// until the specified time end
	// accept a punchIn and punchOut point
	// TODO: punchOut
	var <>pattern, punchIn, punchOut, repeats;

	*new { arg pattern, punchIn, punchOut, repeats = 1;
		^super.newCopyArgs(pattern, punchIn, punchOut, repeats).init
	}

	init { 
		
	}

	embedInStream { arg subval;
		var stream, val,sustain;
		var subpat;
		var substr;
		var restdur;
		var dur;
		var drop_time;
		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
		[thisThread.endBeat, thisThread.beats].debug("beats");

		repeats.value(subval).do { | i |
			var current_offset = 0;
			var previous_offset = 0;
			stream = pattern.asStream;

			if(punchIn.notNil) {

				while (
					{
						current_offset <= punchIn and: {
							val = stream.next(());
							val.notNil;
						}
					},
					{
						previous_offset = current_offset;
						current_offset = current_offset + val.use( { val.dur });
						[current_offset, previous_offset, val].debug("mangling");
					}
				);

				val.use {
					if(val.notNil) {
						var suboffset = punchIn - previous_offset;
						if(suboffset == 0) {
							// we are on a border, do nothing;
							val.debug("we are on a border, do nothing; ");
						} {
							if( suboffset > val.sustain ) {
								// we are on a rest
								val[\dur] = val.dur - suboffset;
								val[\sustain] = val.sustain - suboffset;
								val.debug("we are on a rest");
							} {
								// we are on a note
								val[\dur] = val.dur - suboffset;
								val[\sustain] = val.sustain + suboffset;
								val[\PtimeGatePunch_drop] = suboffset;
								val.debug("we are on a note");

							};
						}
					};
				};
			} {
				val = stream.next(());
			};

			while (
				{ 
					val.notNil;
				},
				{
					val.debug("============== first super val");
					sustain = val.use { val.sustain } ? 1;
					dur = val.use { val.dur } ? 1;
					restdur = dur - sustain; // max: 0;
					[subval, val, sustain, dur, restdur].debug("subval, val, sustain, dur, restdur");
					thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
					[thisThread.endBeat, thisThread.beats].debug("start endbeat, beats");
					thisThread.endBeat = thisThread.endBeat + sustain;
					[thisThread.endBeat, thisThread.beats].debug("second endbeat, beats");
					subpat = val.use { val.pattern ? (val.key !? { Pdef(val.key) }) };
					if(subpat.notNil) {

						substr = subpat.asStream;

						current_offset = 0;
						previous_offset = 0;
						drop_time = val[\PtimeGatePunch_drop];

						if(drop_time.notNil) {

							while (
								{
									current_offset <= drop_time and: {
										subval = substr.next(subval);
										subval.notNil
									}
								},
								{
									previous_offset = current_offset;
									current_offset = current_offset + subval.use( { subval.dur });
									[current_offset, previous_offset, subval].debug("sub mangling");
								}
							);


							subval.use {
								if(subval.notNil) {
									var suboffset = drop_time - previous_offset;
									[drop_time, previous_offset, suboffset, subval.sustain].debug( "[drop_time, previous_offset, suboffset, subval.sustain]" );
									if(suboffset == 0) {
										// we are on a border: do nothing
										subval.debug("sub we are on a border, do nothing; ");
									} {
										if( suboffset > subval.sustain ) {
											// we are on a rest: cut it in two
											subval[\dur] = subval.dur - suboffset;
											subval[\sustain] = subval.sustain - suboffset;
											subval[\isRest] = true; 
											subval.debug("sub we are on a rest");
										} {
											// we are on a note: transform it in rest
											subval[\dur] = subval.dur - suboffset;
											subval[\sustain] = subval.sustain - suboffset;
											subval.debug("sub we are on a note");

										};
									}
								};
							};
						};

						while(
							{ 
								[thisThread.endBeat, thisThread.beats].debug("sub endbeat, beats");
								thisThread.endBeat > thisThread.beats and: {
									subval = substr.next(subval);
									subval.debug("while cond subval");
									subval.notNil;
								}
							},
							{ 
								//subval = subval.yield;
								subval = subval.debug("subval").yield;
							}
						);
						thisThread.endBeat = thisThread.endBeat + restdur;
						//subpat = Event.silent(restdur).loop;
						subpat = (isRest: true, dur:restdur).loop; // dur is replaced by chained pat
						//subpat = (isRest: true).loop;
						substr = subpat.asStream;
						while(
							{ 
								[thisThread.endBeat, thisThread.beats].debug("sub rest endbeat, beats");
								thisThread.endBeat > thisThread.beats and: {
									subval = substr.next(subval);
									subval.notNil;
								}
							},
							{ 
								//subval = subval.yield;
								subval = subval.debug("subval: rest").yield;
							}
						);
					} {
						// not a subpattern, maybe a ndef event
						val.yield;
					};
					val = stream.next(());
				});
		};
		subval.debug("end subval");
		^subval;
	}

	storeArgs {
		^[pattern, repeats]
	}
}

PtimeGatePunch : Pattern {
	// output continuously values from a pattern
	// independently of the chained pattern asking 10 or 100 values
	// until the specified time end
	// accept a punchIn and punchOut point
	// TODO: punchOut
	var <>pattern, punchIn, punchOut, repeats;

	*new { arg pattern, punchIn, punchOut, repeats = 1;
		^super.newCopyArgs(pattern, punchIn, punchOut, repeats).init
	}

	init { 
		
	}

	embedInStream { arg subval;
		var stream, val,sustain;
		var subpat;
		var substr;
		var restdur;
		var dur;
		var drop_time;

		var stream_dropdur;
		
		stream_dropdur = { arg drop_time, stream;
			var current_offset = 0;
			var previous_offset = 0;
			var val;
			if(drop_time.notNil) {

				while (
					{
						current_offset <= punchIn and: {
							val = stream.next(());
							val.notNil;
						}
					},
					{
						previous_offset = current_offset;
						current_offset = current_offset + val.use( { val.dur });
						[current_offset, previous_offset, val].debug("mangling");
					}
				);

				val.use {
					if(val.notNil) {
						var suboffset = drop_time - previous_offset;
						if(suboffset == 0) {
							// we are on a border, do nothing;
							val.debug("we are on a border, do nothing; ");
						} {
							if( suboffset > val.sustain ) {
								// we are on a rest
								val[\dur] = val.dur - suboffset;
								val[\sustain] = val.sustain - suboffset;
								val.debug("we are on a rest");
							} {
								// we are on a note
								val[\dur] = val.dur - suboffset;
								val[\sustain] = val.sustain - suboffset;
								val[\PtimeGatePunch_drop] = suboffset;
								val.debug("we are on a note");
							};
						}
					};
				};
			} {
				val = stream.next(());
			};

			val;
		};

		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
		[thisThread.endBeat, thisThread.beats].debug("beats");

		repeats.value(subval).do { | i |
			stream = pattern.asStream;

			val = stream_dropdur.(punchIn, stream);

			Pspawner({ arg spawner;

				while (
					{ 
						val.notNil;
					},
					{
						val.debug("============== first super val");
						sustain = val.use { val.sustain } ? 1;
						dur = val.use { val.dur } ? 1;
						restdur = dur - sustain; // max: 0;
						[subval, val, sustain, dur, restdur].debug("subval, val, sustain, dur, restdur");
						thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
						[thisThread.endBeat, thisThread.beats].debug("start endbeat, beats");
						thisThread.endBeat = thisThread.endBeat + sustain;
						[thisThread.endBeat, thisThread.beats].debug("second endbeat, beats");
						subpat = val.use { val.pattern ? (val.key !? { Pdef(val.key) }) };

						if(subpat.notNil) {


							spawner.par(
								Prout({

									substr = subpat.asStream;
									if(val[\PtimeGatePunch_drop].notNil) {
										stream_dropdur.( val[\PtimeGatePunch_drop], substr);
									};

									while(
										{ 
											[thisThread.endBeat, thisThread.beats].debug("sub endbeat, beats");
											thisThread.endBeat > thisThread.beats and: {
												subval = substr.next(subval);
												subval.debug("while cond subval");
												subval.notNil;
											}
										},
										{ 
											//subval = subval.yield;
											subval = subval.debug("subval").yield;
										}
									);
									thisThread.endBeat = thisThread.endBeat + restdur;
									//subpat = Event.silent(restdur).loop;
									subpat = (isRest: true, dur:restdur).loop; // dur is replaced by chained pat
									//subpat = (isRest: true).loop;
									substr = subpat.asStream;
									while(
										{ 
											[thisThread.endBeat, thisThread.beats].debug("sub rest endbeat, beats");
											thisThread.endBeat > thisThread.beats and: {
												subval = substr.next(subval);
												subval.notNil;
											}
										},
										{ 
											//subval = subval.yield;
											subval = subval.debug("subval: rest").yield;
										}
									);
								})
							)
						} {
							// not a subpattern, maybe a ndef event
							val.yield;
						};
						val = stream.next(());
					}
				);
			}).embedInStream
		};
		subval.debug("end subval");
		^subval;
	}

	storeArgs {
		^[pattern, repeats]
	}
}

///////////////////////////// Builder - Not really a pattern..

Builder {
	var <source;
	var >envir;
	var <>key;
	classvar <all;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { arg key, fun;
		if(all[key].isNil) {
			all[key] = this.make(fun).key_(key);
			^all[key];
		} {
			var ins;
			ins = all[key];
			if(fun.notNil and: { ins.notNil }) {
				ins.source = fun;
			};
			^ins;
		}
	}

	*make { arg fun;
		var ins = super.new;
		ins.source = fun;
		ins.envir[\builder] = ins;
		^ins;
	}

	source_ { arg fun;
		if( fun.isNil ) {
			fun = {}
		};
		source = fun;
		this.class.functionArgsToEnvir(fun).keysValuesDo { arg k, v;
			if( this.envir[k].isNil ) {
				this.envir[k] = v;
			};
		};
	}
	 
	envir {
		if(envir.isNil) { 
			envir = IdentityDictionary.new;
			envir[\builder] = this;
		};
		^envir
	}

	*functionArgsToEnvir { arg fun;
		var env = ();
		fun.def.argNames.do { arg name, x;
			env[name] = fun.def.prototypeFrame[x]
		};
		^env;
	}

	build {
		^source.valueWithEnvir(this.envir);
	}

	set { arg ...args;
		var hasChanged = false;
		args.pairsDo { arg key, val; 
			if(this.envir.at(key) != val) {
				this.envir.put(key, val);
				hasChanged = true;
			}
		};
		if(hasChanged) {
			this.build;
			this.changed(\set, args);
		}
	}

	unset { arg ... args;
		args.do { arg key; this.envir.removeAt(key) };
		this.changed(\unset, args);
	}

	get { arg key;
		^this.envir[key];
	}
	
}




PlayerWrapper  {
	var <>target;
	*new { arg target;
		^super.new.init(target);
	}

	init { arg tar;
		target = tar;
	}

	play {
		if(target.notNil) {
			target.play;
		}
	}

	stop {
		if(target.notNil) {
			target.stop;
		}
	}

	edit {
		^WindowLayout({ PlayerWrapperView.new(this).layout });
	}

	asView {
		^PlayerWrapperView.new(this).layout;
	}
}



// not needed, just use Pdefn

//+Pfindur {
//	embedInStream { arg event;
//		var item, delta, elapsed = 0.0, nextElapsed, inevent;
//		var localdur = dur.value(event);
//		var stream = pattern.asStream;
//		var cleanup = EventStreamCleanup.new;
//		loop {
//
//			inevent = stream.next(event);
//			if(inevent.isSequenceableCollection) {
//				var inevent0;
//				inevent0 = inevent[0].asEvent ?? { ^event };
//				cleanup.update(inevent);
//				delta = inevent0.delta;
//				nextElapsed = elapsed + delta;
//				if (nextElapsed.roundUp(tolerance) >= localdur) {
//					// must always copy an event before altering it.
//					// fix delta time and yield to play the event.
//					inevent = inevent.collect({ arg x; x.copy.put(\delta, localdur - elapsed) }).yield;
//					^cleanup.exit(inevent);
//				};
//
//				elapsed = nextElapsed;
//				event = inevent.yield;
//
//			} {
//				inevent = inevent.asEvent ?? { ^event };
//				cleanup.update(inevent);
//				delta = inevent.delta;
//				nextElapsed = elapsed + delta;
//				if (nextElapsed.roundUp(tolerance) >= localdur) {
//					// must always copy an event before altering it.
//					// fix delta time and yield to play the event.
//					inevent = inevent.copy.put(\delta, localdur - elapsed).yield;
//					^cleanup.exit(inevent);
//				};
//
//				elapsed = nextElapsed;
//				event = inevent.yield;
//			}
//
//		}
//	}
//
//}
