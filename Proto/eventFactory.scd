
~class_event_factory = ProtoClass((
	// === options:
	// - mode: event or whole pattern ?
	// - playmode: toggle or gate ?
	// - silentmode: bool: should record play the generated pattern or be silent because it's handled elsewhere
	// - fullrecord: bool: should record put every key of input pattern or just midinote and time because composition is done at playing time in pattern ?
	new: { arg self, pat;
		var me= self;
		self.thisClass = { me };
		self = self.deepCopy;

		self.pattern = pat;
		self.children = SparseArray.new;
		self.in_event = Event.default;
		self.silentmode = false;
		self.fullrecord = false;
		self.mode = \note; // add some default, might change

		self;
	},

	newChild: { arg parent, inevent;
		var self = parent.thisClass.deepCopy;
		self.thisClass = { parent.thisClass };

		self.parent_factory = { parent };
		//self.children = List.new;
		self.pattern = { arg self; self.parent_factory.pattern };
		self.mode = { arg self; self.parent_factory.mode };
		self.playmode = { arg self; self.parent_factory.playmode };
		self.silentmode = { arg self; self.parent_factory.silentmode };
		self.fullrecord = { arg self; self.parent_factory.fullrecord };
		self[\stream] = { arg self; self.parent_factory.stream };
		self[\stream_] = { arg self, val; self.parent_factory.stream = val;  };
		self.in_event = Event.default.copy.putAll(inevent ? ());
		self.eventLoop = { arg self; self.parent_factory.eventLoop };

		self;
	},

	toggle: { arg self, but;
		[self.player.notNil, self.player.isPlaying ].debug("toggle: player, playing");
		if(self.player.notNil and: { self.player.isPlaying == true }) {
			"toggle.stop".debug;
			self.stop;
			if(but.notNil) {
				but.value = 0;
			};
		} {
			self.play;
			if(but.notNil) {
				but.value = 1;
			};
		}
	},

	child_is_playing: { arg self, index;
		var child = self.children[index];
		child.notNil and: { child.player.notNil and: { child.player.isPlaying == true } }
	},

	child: { arg self, event;
		self.newChild(event);
	},

	childfun: { arg self, midinote;
		self.newChild((midinote:midinote));
	},

	chainfun: { arg self, pat1, pat2;
		Pchain(pat1, pat2);
	},

	childAt: { arg self, index;
		if(self.children[index].isNil) {
			self.children[index] = self.childfun(index);
		};
		self.children[index];
	},

	midinote: { arg self, midinote;
		self.newChild((midinote:midinote));
	},

	get_playing_children_indexes: { arg self;
		var list = List.new;
		self.children.do({ arg child, idx; 
			if(self.child_is_playing(idx)) {
				list.add(idx);
			}
		});
		list
	},

	startRecording: { arg self;
		if(self.eventLoop.notNil) {
			self.eventLoop.isRecording.debug("startRecording: before: isRecording");
			self.eventLoop.startRec;
			self.eventLoop.isRecording.debug("startRecording: after: isRecording");
			self.changed(\isRecording);
		}
	},

	stopRecording: { arg self;
		if(self.eventLoop.notNil) {
			self.eventLoop.isRecording.debug("stopRecording: before: isRecording");
			self.eventLoop.stopRec;
			self.eventLoop.isRecording.debug("stopRecording: after: isRecording");
			self.changed(\isRecording);
		}
	},

	isRecording: { arg self;
		if(self.eventLoop.isNil) {
			false
		} {
			self.eventLoop.isRecording;
		}
	},

	isRecording_: { arg self, val;
		if(val == true or: { val == 1 }) { self.startRecording } { self.stopRecording };
	},

	recordEvent: { arg self, ev;
		if(self.isRecording and: {self.eventLoop.notNil}) {
			ev.debug("eventFactory: recordEvent");
			self.eventLoop.isRecording.debug("recordEvent: before: isRecording");
			ev[\latency] = nil; // recorded in live with no latency, but should be played with latency
			// FIXME: so should adjust timing too i guess, no ?
			self.current_event = self.eventLoop.recordEvent(ev); // eventloop add timing info
			self.eventLoop.isRecording.debug("isRecording");
			self.eventLoop.debug("evloop");
		}
	},

	recordEventStop: { arg self;
		if(self.eventLoop.notNil and: self.current_event.notNil) {
			//self.current_event.sustain = self.eventLoop.getAbsTime - self.current_event.absTime; // always in seconds, not good for changing tempo
			self.current_event.sustain = (self.eventLoop.getAbsTime - self.current_event.absTime)/self.eventLoop.clock.tempo;
			self.current_event.changed(\refresh);
			self.current_event.debug("recordEventStop");
			self.current_event = nil;
		}
	},

	play_OLD: { arg self, inEvent;
		var mode;
		var playmode;
		if([nil, \proxy].includes(self.mode)) {
			mode = \pattern;
		} {
			mode = self.mode;
		};
		switch(mode,
			\pattern, {
				if(self.player.notNil) {
					self.player.stop;
				};
				self.player = ( self.pattern <> ( inEvent ? self.in_event ) ).play;
			},
			\trigger, {
				self.player = ( self.pattern <> ( inEvent ? self.in_event ) ).play;
			},
			\arp, {
				var arp;
				if(self.player.notNil) {
					self.player.stop;
				};
				arp = Prout({ arg in;
					block { arg break;
						loop {
							in = in.copy.putAll(midinotes: self.get_playing_children_indexes).yield;
							if(in.isNil) {
								break.value;
							}
						}
					}
				});
				self.player = ( self.pattern <> arp <> ( inEvent ? self.in_event ) ).play;
			},
			\event, {
				var ev;
				if(self.stream.isNil) {
					self.stream = self.pattern.loop.asStream;
				};
				ev = self.stream.next(inEvent ? self.in_event);
				if(ev.isNil) {
					self.stream = nil;
				} {
					if(self.player.notNil) {
						self.player.release;
					};
					ev = ev.copy;
					ev[\sustain] = inf;
					ev[\latency] = \;
					self.player = ev.play;
					ev;
				};
			},
			\note, {
				var ev;
				if(self.stream.isNil) {
					self.stream = self.pattern.loop.asStream;
				};
				ev = self.stream.next(Event.default);
				if(ev.isNil) {
					self.stream = nil;
				} {
					if(self.player.notNil) {
						self.player.release;
					};
					ev = ev.copy;
					ev[\sustain] = inf;
					ev[\latency] = \;
					ev.putAll(inEvent ? self.in_event);
					ev.debug("EV play");
					self.recordEvent(ev);
					self.player = ev.play;
					ev;
				};
			},
		
		)
		
	},

	immediateEvent: { arg self, ev;
		ev = ev.copy;
		ev[\sustain] = inf;
		ev[\latency] = '';
		ev;
	},

	play: { arg self, inEvent;
		var pos_ev, stream_ev, ev;
		var evs;
		evs = self.makeEvent(inEvent);
		evs.debug("play!");
		if(evs.notNil) {
			#pos_ev, stream_ev, ev = evs;
			if(self.silentmode != true) {
				self.player = self.immediateEvent(ev).play;
			};
			if(self.isRecording) {
				if(self.fullrecord) {
					self.recordEvent(ev);
				} {
					self.recordEvent(pos_ev);
				}
			}
		}
	},

	getEvent: { arg self, inEvent;
		var pos_ev, stream_ev, ev;
		var evs;
		evs = self.makeEvent(inEvent);
		if(evs.notNil) {
			#pos_ev, stream_ev, ev = evs;
			if(self.fullrecord) {
				ev;
			} {
				pos_ev;
			}
		}
	},

	get: { arg self ...args;
		self.getEvent(*args)
	},

	makeEvent: { arg self, inEvent;
		var mode;
		var playmode;
		var stream_ev, pos_ev, ev;
		var res;
		if([nil, \proxy].includes(self.mode)) {
			mode = \pattern;
		} {
			mode = self.mode;
		};
		switch(mode,
			// generate events of type pattern
				// can be embeded or out of band
				// out of band is not sensible to composition
				// this also stop with note off
			\pattern, {
				if(self.player.notNil) {
					self.player.stop;
				};
				//self.player = ( self.pattern <> ( inEvent ? self.in_event ) ).play;
				ev = (
					type: \pattern,
					pattern: self.pattern <> ( inEvent ? self.in_event ),
				);
				// debug
				//(
				//	type: \pattern,
				//	pattern: Pbind(
				//		\instrument, \default,
				//		\freq, 200,
				//		\dur, 1,
				//		\amp, 0.1
				//	),
				//).play;
				pos_ev = inEvent ? self.in_event;
				stream_ev = (play: { ( self.pattern ).play });
				res = [pos_ev, stream_ev, ev];
			},
			// generate event of type pattern but don't listen to noteOff
			\trigger, {
				//self.player = ( self.pattern <> ( inEvent ? self.in_event ) ).play;
				ev = (play: { ( self.pattern <> ( inEvent ? self.in_event ) ).play });
				pos_ev = inEvent ? self.in_event;
				stream_ev = (play: { ( self.pattern ).play });
				res = [pos_ev, stream_ev, ev];
			},
			// not sure if working
			\arp, {
				var arp;
				if(self.player.notNil) {
					self.player.stop;
				};
				arp = Prout({ arg in;
					block { arg break;
						loop {
							in = in.copy.putAll(midinotes: self.get_playing_children_indexes).yield;
							if(in.isNil) {
								break.value;
							}
						}
					}
				});
				// TODO
				self.player = ( self.pattern <> arp <> ( inEvent ? self.in_event ) ).play;
			},
			// take the event from the stream, the key pushed is composed in the pattern
			// useful when using pattern that process input notes into a new form
			// note that timeline.asPattern doesnt handle this case (because composing is in the other way),
			// so recording will not match replay
			\event, {
				if(self.stream.isNil) {
					self.stream = self.pattern.loop.asStream;
				};
				pos_ev = inEvent ? self.in_event;
				ev = self.stream.next(pos_ev);
				// TODO
				if(ev.isNil) {
					self.stream = nil;
					res = nil;
				} {
					if(self.player.notNil) {
						self.player.release;
						self.player = nil;
					};
					ev = ev.copy;
					ev;
					res = [pos_ev, ev, ev];
				};
				res;
			},
			// take the event from the stream, the key pushed replace the key in the pattern
			// useful for overriding the current melody score in input pattern
			\note, {
				if(self.stream.isNil) {
					self.stream = self.pattern.loop.asStream;
				};
				stream_ev = self.stream.next(Event.default);
				if(stream_ev.isNil) {
					// if stream end, stop everything
					// maybe add repeat argument ?
					// note: it's looped at top of function
					self.stream = nil;
					res = nil;
				} {
					if(self.player.notNil) {
						self.player.release;
					};
					pos_ev = inEvent ? self.in_event;
					pos_ev = pos_ev.copy;
					ev = stream_ev.copy;
					ev.putAll(pos_ev);
					[pos_ev, stream_ev, ev].debug("EV make");
					res = [pos_ev, stream_ev, ev];
				};
			},
		
		);
		res;
	},

	get_eventFactory_function: { arg self;
		{ arg pos, nodesize;
			var ev = self.makeEvent;
			ev.absTime = pos.x;
			ev[\midinote] = pos.y;
			ev[\latency] = nil;
			ev[\sustain] = nodesize;
		}
	},

	stop: { arg self;
		var mode;
		"self.stop".debug;
		if([nil, \proxy].includes(self.mode)) {
			mode = \pattern;
		} {
			mode = self.mode;
		};
		switch(mode,
			\pattern, {
				if(self.player.notNil) {
					"self.stop: pattern".debug;
					self.player.stop;
					self.player = nil;
				};
			},
			\note, {
				self.recordEventStop;
				if(self.player.notNil) {
					if(self.player.hasGate) {
						self.player.release;
					};
					self.player = nil;
				};
			},
			\event, {
				self.recordEventStop;
				if(self.player.notNil) {
					if(self.player.hasGate) {
						self.player.release;
					};
					self.player = nil;
				};
			}
		);
		
	},

));

~class_param_event_factory = ProtoClass((
	parent: ~class_event_factory,

	new: { arg self, param;
		var me = self;
		self.thisClass = { me };
		self = self.deepCopy;

		self.param = param;
		self.children = SparseArray.new;
		self.in_event = Event.default;
		self.silentmode = false;
		self.fullrecord = false;
		self.mode = \note; // add some default, might change

		self;
	},

	startRecording: { arg self;
		if(self.eventLoop.notNil) {
			if(self.controller.notNil) {
				self.controller.remove;
			};
			self.controller = self.param.makeListener({ arg obj, param;
				var ev = (level:param.get);
				ev.debug("LEVEL EV RECORDING !!");
				self.recordEvent(ev);
			});
			self.eventLoop.isRecording.debug("startRecording: before: isRecording");
			self.eventLoop.startRec;
			self.eventLoop.isRecording.debug("startRecording: after: isRecording");
			self.changed(\isRecording);
		}
	},

	makeEvent: { arg self, inEvent;
		// not used ?
		().putAll(inEvent)
	},

	stopRecording: { arg self;
		if(self.eventLoop.notNil) {
			if(self.controller.notNil) {
				self.controller.remove;
			};
			self.eventLoop.isRecording.debug("stopRecording: before: isRecording");
			self.eventLoop.stopRec;
			self.eventLoop.isRecording.debug("stopRecording: after: isRecording");
			self.changed(\isRecording);
		}
	},


));

// parallel
~class_player_event_factory = ProtoClass((
	parent: ~class_event_factory,

	new: { arg self, players;
		self.thisClass = { self };
		self = self.deepCopy;

		self.set_players(players);
		self.children = SparseArray.new;
		self.in_event = Event.default;
		self.silentmode = false;
		self.fullrecord = false;
		self.controllers = SparseArray.new;
		self.mode = \note; // add some default, might change
		self.recordingQuant = 1;

		self;
	},

	set_players: { arg self, players;
		self.rawplayers = players;
		players = players.collect({ arg p; 
			if(p.isKindOf(PlayerWrapper), { 
				p
			}, { 
				PlayerWrapper(p);
			});
		});
		self.players = players;
	},

	startListening: { arg self, player;
		self.stopListening;
		player.debug("start listening");
		self.controller = player.makeListener({ arg target, msg, quant;
			var ev = player.asPlayerEvent;
			msg.debug("received event");
			if(msg == \play) {
				ev.putAll(self.in_event);
				ev.debug("LEVEL EV RECORDING !!");
				self.clock.schedAbs(self.clock.nextTimeOnGrid(quant), {
					self.recordEvent(ev);
				});
				//self.current_event.absTime = self.eventLoop.clock.nextTimeOnGrid(quant);
			} {
				if(msg == \stop) {
					self.recordEventStop;
				}
			}
		});
	},

	stopListening: { arg self;
		if(self.controller.notNil) {
			self.controller.remove
		}
	},

	clock: { arg self;
		self.eventLoop.clock
	},

	startRecording: { arg self;
		if(self.eventLoop.notNil) {
			self.clock.schedAbs(self.clock.nextTimeOnGrid(self.recordingQuant), {
				self.players.do { arg player, idx;
					player.debug("startRecording: player");
					self.childAt(idx).startListening(player);
				};
				self.eventLoop.isRecording.debug("startRecording: before: isRecording");
				self.eventLoop.startRec(true);
				self.eventLoop.isRecording.debug("startRecording: after: isRecording");
				self.changed(\isRecording);
			});
		}
	},

	makeEvent: { arg self, inEvent;
		// not used ?
		().putAll(inEvent)
	},

	stopRecording: { arg self;
		if(self.eventLoop.notNil) {
			self.clock.schedAbs(self.clock.nextTimeOnGrid(self.recordingQuant), {
				self.players.do { arg player, idx;
					self.childAt(idx).stopListening;
				};
				self.eventLoop.isRecording.debug("stopRecording: before: isRecording");
				self.eventLoop.stopRec;
				self.eventLoop.isRecording.debug("stopRecording: after: isRecording");
				self.changed(\isRecording);
			});
		}
	},
));

~class_serial_player_event_factory = ProtoClass((
	parent: ~class_player_event_factory,

	new: { arg self, players, midinote;
		self.thisClass = { self };
		self = self.deepCopy;

		self.current_midinote = midinote; // midinote is already a method
		self.set_players(players);
		self.children = SparseArray.new;
		self.in_event = Event.default;
		self.silentmode = false;
		self.fullrecord = false;
		self.controllers = SparseArray.new;
		self.mode = \note; // add some default, might change
		self.recordingQuant = 1;

		self;
	},

	newParent: { arg self, parent, players, midinote;
		var inevent;
		self.thisClass = { self };
		self = self.deepCopy;

		inevent = (midinote:midinote);

		self.current_midinote = midinote; // midinote is already a method
		self.set_players(players);
		self.children = SparseArray.new;
		self.parent_factory = { parent };
		//self.children = List.new;
		self.pattern = { arg self; self.parent_factory.pattern };
		self.mode = { arg self; self.parent_factory.mode };
		self.playmode = { arg self; self.parent_factory.playmode };
		self.silentmode = { arg self; self.parent_factory.silentmode };
		self.fullrecord = { arg self; self.parent_factory.fullrecord };
		self[\stream] = { arg self; self.parent_factory.stream };
		self[\stream_] = { arg self, val; self.parent_factory.stream = val;  };
		self.in_event = Event.default.copy.putAll(inevent ? ());
		self.eventLoop = { arg self; self.parent_factory.eventLoop };
		self.recordingQuant = { arg self; self.parent_factory.recordingQuant };

		self;
	},

	childfun: { arg self, idx;
		self.newChild((part: idx, midinote:self.current_midinote));
	},

	set_players: { arg self, players;
		if(players.isKindOf(PlayerWrapperGroup)) {
			self.players = players;
			self.rawplayers = players; // TODO
		} {
			self.rawplayers = players;
			players = players.collect({ arg p; 
				if(p.isKindOf(PlayerWrapper), { 
					p
				}, { 
					PlayerWrapper(p);
				});
			});
			self.players = players;
		}
	},

	startListeningChildren: { arg self;
		self.players.do { arg player, idx;
			player.debug("startRecording: player");
			self.childAt(idx).startListening(player);
		};
	},

	startListening: { arg self, player;
		self.stopListening;
		player.debug("start listening");
		self.controller = player.makeListener({ arg target, msg, quant;
			var ev = player.asPlayerEvent;
			msg.debug("received event");
			if(msg == \play) {
				ev.putAll(self.in_event);
				ev.debug("LEVEL EV RECORDING !!");
				self.clock.schedAbs(self.clock.nextTimeOnGrid(quant), {
					self.parent_factory.children.do{ arg child;
						// stop currently playing events
						if(child.current_event.notNil) {
							child.recordEventStop;
						}
					};
					self.recordEvent(ev);
				});
				//self.current_event.absTime = self.eventLoop.clock.nextTimeOnGrid(quant);
			} {
				if(msg == \stop) {
					self.recordEventStop;
				}
			}
		});
	},

));

~class_track_event_factory = ProtoClass((
	parent: ~class_player_event_factory,

	new: { arg self, tracks;
		self.thisClass = { self };
		self = self.deepCopy;

		self.set_players(tracks);
		self.children = SparseArray.new;
		self.in_event = Event.default;
		self.silentmode = false;
		self.fullrecord = false;
		self.controllers = SparseArray.new;
		self.mode = \note; // add some default, might change
		self.recordingQuant = 1;

		self;
	},

	set_players: { arg self, players;
		if(players.isKindOf(PlayerWrapperGroup)) {
			self.players = players;
			self.rawplayers = players; // TODO
		} {
			self.rawplayers = players;
			players = players.collect({ arg p; 
				if(p.isKindOf(PlayerWrapper), { 
					p
				}, { 
					PlayerWrapper(p);
				});
			});
			self.players = players;
		}
	},

	childfun: { arg self, idx;
		var child = ~class_serial_player_event_factory.newParent(self, self.players[idx].target.children, idx);
		child;
	},

	startRecording: { arg self;
		if(self.eventLoop.notNil) {
			self.clock.schedAbs(self.clock.nextTimeOnGrid(self.recordingQuant), {
				self.players.do { arg player, idx;
					player.debug("startRecording: player");
					self.childAt(idx).startListeningChildren;
				};
				self.eventLoop.isRecording.debug("startRecording: before: isRecording");
				self.eventLoop.startRec(true);
				self.eventLoop.isRecording.debug("startRecording: after: isRecording");
				self.changed(\isRecording);
			});
		}
	},

));

~map_button = { arg but, ef;
	but.mouseDownAction = {
		if(ef.playmode == \toggle or: { ef.playmode == nil }) {
		} {
			but.value = 1;
			ef.play;
		}
	};
	but.action = {
		if(ef.playmode == \toggle or: { ef.playmode == nil }) {
			ef.toggle(but);
		} {
			but.value = 0;
			ef.stop;
		}
	};
};
~map_key = { arg action, ef;
	switch(action,
		\down,{
			if(ef.playmode == \toggle or: { ef.playmode == nil }) {
				"down map_key: do nothing".debug;
			} {
				"down map_key: play".debug;
				ef.play;
			}
		},

		\up,{
			if(ef.playmode == \toggle or: { ef.playmode == nil }) {
				"up map_key: toggle".debug;
				ef.toggle;
			} {
				"up map_key: stop".debug;
				ef.stop;
			}
		},
	);
};

