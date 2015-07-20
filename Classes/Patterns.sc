PseqCursor : Prout {
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
		if(initialPattern.class.isKindOf(Pbind)) {
			^initialPattern.patternpairs.asDict
		} {
			^()
		}
	}
}
