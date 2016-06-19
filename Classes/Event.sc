
PlayerEvent {
	classvar <>event; // debug: replace without rebooting
	*new { arg ev;
		^(
			event ?
			(
				type: \player,
				parent: (
					parent: Event.default,
					label: { arg self;
						self.player.label;
					}
				)
			).putAll(ev)
		)
	}
}

PatternEvent {
	classvar <>event; // debug: replace without rebooting
	*new { arg ev;
		^(
			event ?
			(
				type: \pattern,
				parent: (
					parent: Event.default,
					label: { arg self;
						if(self.timeline.notNil) {
							self.timeline.label;
						} {
							// TODO: if Pdef, give Pdef key
							"No Name"
						}
					},
					eventLoop: { arg self;
						self.timeline.eventLoop
					},
					eventList: { arg self;
						self.eventLoop.list
					},
					pattern: { arg self;
						self.eventList.asPattern;
					},
					embedPattern: { arg self;
						Pfin(self.sustain, Pembed(self.pattern, self.startOffset))
					},
				)
			).putAll(ev)
		)
	}
}

Pembed : Pattern {
	*new { arg ...args;
		^~pembed.new(*args);
	}
}
