
PlayerEvent : Event {
	classvar <>myevent; // for debug: replace without rebooting
	classvar <>defaultParent;
    classvar <>playFunction;


	*initClass {
		//Class.initClassTree(Event);
		var playfun = {
			var method = ~method.value ? \playNow;
			var stopmethod = ~stopMethod.value ? \stopNow;
			var args = ~arguments ? [];
			var receiver = ~receiver.value(currentEnvironment); // should decide if envir style or proto style!
			var quant;
			//[method, args, receiver, ~disableQuant].debug("player EventType: method, args, receiver");
			TempoClock.default.sched(0, {
				// apparently, latency is not needed, but there is a little latency quand meme 
				//TempoClock.default.sched(Server.default.latency, {
				//if(receiver.isKindOf(NodeProxy)) {
				//	// problems with timelines, so stop only with Ndef which else doesn't restart from begining
				//	receiver.perform(stopmethod, *args);
				//};
				//quant = receiver.tryPerform(\quant);
				//receiver.tryPerform(\quant_, 0);
				//receiver.quant.debug("receiver quant");
				receiver.perform(method, *args);
				//receiver.tryPerform(\quant_, quant);
				//quant.debug("in zero quant: old quant");
				if(method == \playNow or: { method == \play }) {
					TempoClock.default.sched(~sustain.value(currentEnvironment), {
						receiver.perform(stopmethod, *args);
						nil;
					}.inEnvir);
				};
				nil;
			}.inEnvir);
			//~miam = (haha: "bla" );
		};
        playFunction = playfun;


		// make start and stop event silent
		// FIXME: would be better to have only one type of event instead of polluting namespace
		//		but this would break so much things and definitely break compat with original EventList
		Event.addEventType(\start, {});
		Event.addEventType(\end, {});

		defaultParent = (
			type: \player,
			//parent: Event.default, // cause infinite loop
			target: { arg self; // should decide if named receiver (old) or target (like PlayerWrapper)
				self.receiver;
			},
			label: { arg self;
				var tar;
				tar = self.target.value;
				if(tar.notNil) {
					tar.label;
				} {
					"No name"
				}
			}
		);

		Event.addEventType(\player, playfun, defaultParent);

		Event.addEventType(\start, {});
		Event.addEventType(\end, {});
		Event.addEventType(\locator, {});
		//Event.addEventType(\player, playfun);
	}

	*new { arg ev;
		var inst;
		inst = super.new;
		inst.parent = myevent ? defaultParent;
		inst.putAll(ev ?? { () });
		inst[\type] = \player;
		^inst;
	}

	*redefine { arg ev;
		ev = ev ?? { () };
		this.resetEvent(ev);
		ev.parent = defaultParent;
		ev[\type] = \player; // because when used on normal event, doesnt have special embedInStream
		^ev;
	}

	embedInStream { arg inevent;
		var ev = (parent: this.parent);
		if(inevent.notNil) {
			ev.putAll(inevent)
		};
		^super.embedInStream(ev)
		//^super.embedInStream((parent: this.parent))
	}

	*resetEvent { arg ev;
		// used to get ride of old keys when redefining an Event (in clipEditor for example)
		// while keeping position keys and keeping the same reference
		// this is generic to PatternEvent and PlayerEvent 
		// why not an instance method ? I don't know, I don't want to clutter
		//ev[\isEmbeddable] = nil;
		ev[\embedEvent] = nil;
		ev[\pattern] = nil;
		ev[\key] = nil;
		ev[\type] = nil;
		ev[\eventType] = nil;
		ev[\label] = nil;
		ev[\eventlist] = nil;
		^ev;
	}


	printOn { arg stream;
		this.storeOn(stream)
	}

	storeOn { arg stream;
		stream << "PlayerEvent((";
		stream << this.keys.as(Array).collect { arg key;
			"%: %".format(key, this[key].asCompileString)
		}.join(", ");
		stream << "))";
	}
}

PlayerPattern {
	//Pevent

}

PatternEvent : Event {
	classvar <>myevent; // debug: replace without rebooting
	classvar <>defaultParent;

	*initClass {
		Class.initClassTree(PlayerEvent);

		defaultParent = (
			type: \pattern,
			//parent: Event.default,
			label: { arg self;
				if(self.timeline.notNil) {
					self.timeline.label;
				} {
					// TODO: if Pdef, give Pdef key
					if(self.pattern.isKindOf(Pdef)) {
						self.pattern.key;
					} {
						"No Name"
					}
				}
			},
			eventLoop: { arg self;
				self.timeline.eventLoop
			},
			eventList: { arg self;
				self.eventLoop.list
			},
			pattern: { arg self;
				if(self.timeline.notNil) {
					self.timeline.asPattern;
				} {
					self.eventList.asPattern;
				};
			},
			embedPattern: { arg self, parent;

				var sus = self.use { self.sustain };
				// NOTE: dur is not used because only Pembed at event list level can embed pattern which have dur < sustain
				if(self.timeline.notNil) {
					// FIXME: Pfindur is useless because in theory every timeline use sloop.cutPattern, in practice this fail
					// DO NOT use "sus" as argument of asPattern because it is used to change the eventList size, you just want to repeat the eventList without changing its size, but you can use offset
					Pfindur(sus, self.timeline.asPattern(self.startOffset ? self.event_dropdur, nil, nil, self, parent));
				} {
					var pat = Pembed(self.pattern, self.startOffset ? self.event_dropdur);
					var mp;
					if(parent.notNil) {
						mp = parent.mutingPattern(self);
					};
					if(mp.notNil) {
						pat = Pfindur(sus, mp <> pat );
					} {
						pat = Pfindur(sus, pat );
					};
					// event_dropdur is the old key, should replace by startOffset
					pat;
					//Pspawner({ arg sp;
						//sp.par(
							//Pfindur(sus, Pembed(self.pattern, self.startOffset ? self.event_dropdur))
						//);
						//sp.wait(self.use{self.dur}) // use delta ?
					//})
				}
			},
			receiver: { arg self; // can be used like a \type:\player thanks to this method
				if(self.timeline.notNil) {
					self.timeline
				} {
					if(self.proxy.isNil) {
						self.proxy = EventPatternProxy.new;
						self.proxy.source = self.pattern
					};
					self.proxy
				}
			},
		);

		Event.addEventType(\pattern, PlayerEvent.playFunction, defaultParent);
		//Event.addEventType(\pattern, PlayerEvent.playFunction);
	}

	*new { arg ev;
		var inst;
		inst = super.new;
		inst.parent = myevent ? defaultParent;
		inst[\type] = \pattern; // don't know why it's not taken from parent event, need enforce
		inst.putAll(ev);
		^inst;
	}

	*redefine { arg ev;
		PlayerEvent.resetEvent(ev);
		ev.parent = defaultParent;
		ev[\type] = \pattern; // because when used on normal event, doesnt have special embedInStream
		^ev;
	}

	embedInStream { arg inevent;
		// NOTE: should not embed pattern immediatly, else PatternEvents in Pseq cannot be parallel
		^super.embedInStream((parent: this.parent).putAll(inevent))
		//^this.embedPattern.debug("embedPattern").embedInStream((parent: this.parent).putAll(inevent))
		//^this.embedPattern.embedInStream(inevent)
		//^super.embedInStream((parent: this.parent))
	}

	embedPatternInStream { arg inevent;
		^this.embedPattern.embedInStream(inevent)
	}

	printOn { arg stream;
		this.storeOn(stream)
	}

	storeOn { arg stream;
		stream << "PatternEvent((";
		stream << this.keys.as(Array).collect { arg key;
			"%: %".format(key, this[key].asCompileString)
		}.join(", ");
		stream << "))";
	}
}


Pembed : Pattern {
	// A pattern return events, but some events are PatternEvent, they point to a pattern
	// Pembed is used to process a pattern and replace PatternEvent by its events
	classvar <>startOffsetKey = \event_dropdur;
	classvar <>sustainKey = \sustain;
	*new { arg ...args;
		ParamProto.init;
		^ProtoTemplateDef(\TimelineEmbeder).new(*args);
		//^topEnvironment[\pembed].new(*args);
	}
}
