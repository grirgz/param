
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

//SimpleParamViewToolBox {
	//
	//			new {
	//				var font;
	//				self = self.deepCopy;
	//			
	//				font = Font.default;
	//				font.size = 11;
	//				self.layout = VLayout.new;
	//				//self.label = StaticText.new.font_(font).minWidth_(150);
	//				self.label = StaticText.new.font_(font);
	//				self.knob = Knob.new;
	//				//self.val = TextField.new.font_(font).minWidth_(150);
	//				self.val = TextField.new.font_(font);
	//				self.layout.add(self.label, stretch:1);
	//				self.layout.add(self.knob);
	//				self.layout.add(self.val);
	//				self.layout.margins = 1;
	//				self.layout.spacing = 10;
	//			
	//				self;
	//			}
	//
	//			mapMidi { arg key;
	//				MIDIMap.mapStaticTextLabel(key, self.label);
	//				MIDIMap.mapView(key, self.knob);
	//				MIDIMap.mapView(key, self.val);
	//			}
//}

//TrackDef {
//	classvar <>all;
//	var <>key;
//	//var <>source;
//	var <>wrapper;
//
//	*initClass {
//		all = PresetDictionary(\TrackDef)
//	}
//
//	*new { arg key, val;
//		if(all[key].isNil) {
//			if(val.notNil) {
//				^super.new.init(val).prAdd(key)
//			} {
//				^nil
//			}
//		} {
//			var ret = all[key];
//			if(val.notNil) {
//				//ret.source = val
//			};
//			^ret;
//		}
//	}
//
//	*newInstance { arg val;
//		^super.new.init(val).prAdd(\instance)
//	}
//
//	prAdd { arg xkey;
//		key = xkey;
//		all[key] = this;
//	}
//
//	init { arg val;
//		wrapper = val;
//		wrapper.me = { this };
//	}
//
//	at { arg x;
//		x.debug("at");
//		^wrapper.atChild(x)
//	}
//
//	put { arg x, val;
//		wrapper.putChild(x, val);
//	}
//
//	clear {
//		this.destructor;
//		all[this.key] = nil;
//		^nil;
//	}
//
//	collect { arg fun;
//		^this.collectChildren(fun)
//	}
//
//	do { arg fun;
//		^this.doChildren(fun)
//	}
//
//	source { arg ... args;
//		^this.doesNotUnderstand(\source, * args)
//	}
//
//	isPlaying { arg ... args;
//		^this.doesNotUnderstand(\isPlaying, * args)
//	}
//
//	play { arg ... args;
//		this.doesNotUnderstand(\play, * args)
//	}
//
//	stop { arg ... args;
//		this.doesNotUnderstand(\stop, * args)
//	}
//
//    doesNotUnderstand { arg selector...args;
//		if(wrapper.isKindOf(ProtoClass) and: {
//				wrapper[selector].notNil
//			}
//			or: {
//				wrapper.class.findRespondingMethodFor(selector).notNil
//			}
//		) {
//			//"% perform: %, %".format(this.class, selector, args).debug;
//			^wrapper.perform(selector, * args);
//		} {
//			"% perform: %, %".format(this.class, selector, args).debug;
//			"soft doesNotUnderstand".debug;
//			DoesNotUnderstandError.new(this, selector, args).throw
//		};
//	}
//}

//TrackGroupDef : TrackDef {
//
//	*initClass {
//		all = PresetDictionary(\TrackGroupDef)
//	}
//	
//	init { arg src;
//		if(src.isKindOf(SequenceableCollection)) {
//			wrapper = ~trackGroupType_PlayerWrapper.new(src);
//		} {
//			wrapper = src
//		};
//		wrapper.me = { this };
//	}
//}

//+Document {
//	*load {
//		switch(Platform.ideName,
//			\scide, {
//				var curdir = ""
//
//			}, 
//			\scvim, {
//
//			}
//	}
//
//	currentDir
//	
//}


/////////////////////



////////////////////////////////////////




/////////////////////////////////



/////////////////////////////////

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
