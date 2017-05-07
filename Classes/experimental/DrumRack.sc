// the DrumRack system is kind of deprecated by parPlayerGroup which is simpler (less features, mut more idiomatic)

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
	var <>playerWrapper;
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
	// bad name: not really the manager, should be the reverse
	// but it's just a prototype now, should be translated in real class code
	*initClass {
		// since param/Proto/main.scd, this is not needed anymore

		//this.eventPrototypeInitClass((
		//	file: "/home/ggz/code/sc/seco/vlive/demo/param/lib/stepeditor.scd",
		//	name: \class_score_manager,
		//));
	}

	*new { arg ... args;
		^super.new.init(~class_score_manager.new(*args))
	}
}

DrumRackManager : EventPrototype {
	*initClass {
		// since param/Proto/main.scd, this is not needed anymore

		//this.eventPrototypeInitClass((
		//	file: "/home/ggz/code/sc/seco/vlive/demo/param/lib/drumrack.scd",
		//	name: \class_drumrack,
		//));
	}

	*new { arg ... args;
		^super.new.init(~class_drumrack.new(*args))
	}
}

