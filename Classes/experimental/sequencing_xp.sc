
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

