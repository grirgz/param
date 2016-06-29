
PlayerEvent : Event {
	classvar <>myevent; // debug: replace without rebooting
	classvar <>defaultParent;


	*initClass {
		//Class.initClassTree(Event);
		var playfun = {
			var method = ~method.value ? \play;
			var stopmethod = ~stopMethod.value ? \stop;
			var args = ~arguments ? [];
			var receiver = ~receiver.value(currentEnvironment); // should decide if envir style or proto style!
			var quant;
			[method, args, receiver, ~disableQuant].debug("player EventType: method, args, receiver");
			if(~disableQuant != false) {
				TempoClock.default.sched(0, {
					// apparently, latency is not needed, but there is a little latency quand meme 
					//TempoClock.default.sched(Server.default.latency, {
					if(receiver.isKindOf(NodeProxy)) {
						// problems with timelines, so stop only with Ndef which else doesn't restart from begining
						receiver.perform(stopmethod, *args);
					};
					quant = receiver.tryPerform(\quant);
					receiver.tryPerform(\quant_, 0);
					receiver.quant.debug("receiver quant");
					receiver.perform(method, *args);
					receiver.tryPerform(\quant_, quant);
					quant.debug("in zero quant: old quant");
					if(method == \play) {
						TempoClock.default.sched(~sustain.value(currentEnvironment), {
							receiver.perform(stopmethod, *args);
						}.inEnvir);
					};
				}.inEnvir);
			} {
				receiver.perform(method, *args);
				if(method == \play) {
					TempoClock.default.sched(~sustain.value(currentEnvironment), {
						receiver.perform(stopmethod, *args);
					}.inEnvir);
				};
			};

		};

		Event.addEventType(\player, playfun);
		Event.addEventType(\pattern, playfun); // just for compat

		defaultParent = (
			type: \player,
			parent: Event.default,
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
		^super.embedInStream((parent: this.parent).putAll(inevent))
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

}

PlayerPattern {
	//Pevent

}

PatternEvent : Event {
	classvar <>myevent; // debug: replace without rebooting
	classvar <>defaultParent;

	*initClass {
		defaultParent = (
			type: \pattern,
			parent: Event.default,
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
			embedPattern: { arg self;
				if(self.timeline.notNil) {
					self.timeline.asPattern;
				} {
					// event_dropdur is the old key, should replace by startOffset
					Pfindur(self.use {self.sustain}, Pembed(self.pattern, self.startOffset ? self.event_dropdur))
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

	}

	*new { arg ev;
		var inst;
		inst = super.new;
		inst.parent = myevent ? defaultParent;
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
		//^super.embedInStream((parent: this.parent).putAll(inevent))
		//^this.embedPattern.debug("embedPattern").embedInStream((parent: this.parent).putAll(inevent))
		^this.embedPattern.debug("embedPattern").embedInStream(inevent)
		//^super.embedInStream((parent: this.parent))
	}
}


Pembed : Pattern {
	*new { arg ...args;
		^topEnvironment[\pembed].new(*args);
	}
}
