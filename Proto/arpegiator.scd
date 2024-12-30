
ProtoTemplateDef(\Arpegiator, (

	arper: { arg self, pattern, arp, engine;
		( engine ?? { self.engine } ) <> self.arpize(pattern, arp);
	},

	arperlive: { arg self, notefun, pattern, arp, engine;
		( engine ?? { self.engine } ) <> Pbind(\currentNotes, Pfunc(notefun)) <> arp <> pattern
	},

	arpize: { arg self, pattern, arp;
		Prout({ arg ev;
			var inpstr = pattern.asStream;
			var arpstr = arp.asStream;
			var arpval;
			var inpval;
			var arptime; // arp pattern absolute time cursor
			var inptime; // input pattern absolute time cursor
			var nextarptime; // next time to yield currentNotes from arp.delta
			var notes;
			var delta;

			inptime = 0;
			inpval = inpstr.next(Event.default);
			inpval[\absTime] = inptime;

			arptime = 0;
			arpval = arpstr.next(ev);
			arpval[\absTime] = arptime;

			if(arpval.isNil or: inpval.isNil) {
				nil.yield;
			};
			notes = List[];

			//patval.debug("patval");
			while { 
				inptime == 0;
			} {
				//[arptime, inptime, arpval, inpval].debug("input loop 0");
				notes = notes.add( inpval );
				inptime = inptime + inpval.delta;
				inpval = inpstr.next(Event.default);
				inpval[\absTime] = inptime;
			};
			arpval.currentNotes = notes;

			notes = notes.select { arg note;
				note.notNil and: { [\rest, \start, \end].includes(note[\type]).not } and: {note[\midinote].notNil}
			};

			//[arptime, inptime].debug("yield state 0");
			//notes.collect({ arg ev; ev.use { ~midinote.value } }).debug("currentNotes yield 0");
			ev = arpval.yield; // yield 0


			loop { // unroll arp stream
				// inpval contains note with abstime 2
				//[arptime, inptime, arpval, inpval].debug("arp loop 0");
				arptime = arptime + arpval.delta;
				arpval = arpstr.next(ev);
				arpval[\absTime] = arptime;
				//arpval.debug("arpval");
				if(arpval.isNil or: inpval.isNil) {
					nil.yield;
				};

				//patval.debug("patval");
				while { // unroll input stream until it catch up arp stream time
					inptime <= arptime; // 2 < 1/8
				} {
					//[arptime, inptime, arpval, inpval].debug("input loop 1");
					notes = notes.add( inpval );
					//notes.class.debug("notes in input loop 1");
					inptime = inptime + inpval.delta;
					inpval = inpstr.next(Event.default);
					inpval[\absTime] = inptime;
				};

				notes = notes.select { arg note;
					note.notNil and: { [\rest, \start, \end].includes(note[\type]).not } and: {note[\midinote].notNil}
				};

				notes = notes.select({ arg note;
					var endtime = note[\absTime] + note.use { ~sustain.value };
					//[endtime, arptime, endtime > arptime, note].debug("select note ? ");
					endtime > arptime; // 2 > 1/8
				});

				arpval.currentNotes = notes.copy;
				//[arptime, inptime].debug("yield state 1");
				//notes.collect({ arg ev; ev.use { ~midinote.value } }).debug("currentNotes yield 1");
				ev = arpval.yield; // yield 1 (arptime == 1/8)

			}

		});

	},

	engine_old: { arg self;
		
		Pbind(
			\midinote, Prout({ arg ev;
				//ev[\degree].choose.yield
				//ev = ev[\degree].mirror1.wrapAt(ev[\idx]).yield;
				var note, notes;
				notes = ev[\currentNotes].select { arg note;
					note.notNil and: { [\rest, \start, \end].includes(note[\type]).not } and: {note[\midinote].notNil}
				};
				notes = notes.sort({ arg a, b; a.midinote < b.midinote });
				notes.debug("notes");
				note = notes.wrapAt(ev[\idx]);
				if(note.notNil) {
					ev = ( note[\midinote] + ( ev[\octaveshift] ? 0 * 12 ) ).yield;
				} {
					ev = \rest.yield;
				};
				ev;
			}).loop,
		);
	},

	engine: { arg self;
		// support arrayed idx
		// support rest in arp
		
		Pbind(
			\midinote, Prout({ arg ev;
				//ev[\degree].choose.yield
				//ev = ev[\degree].mirror1.wrapAt(ev[\idx]).yield;
				var note, notes;
				var resnotes;
				var eidx;
				var compose;
				compose = { arg eva, evb;
					eva.merge(evb, { arg a, b; a });
				};
				notes = ev[\currentNotes].select { arg note;
					note.notNil and: { [\rest, \start, \end].includes(note[\type]).not } and: {note[\midinote].notNil}
				};
				notes = notes.sort({ arg a, b; a.midinote < b.midinote });
				//notes.debug("notes");
				eidx = ev[\idx];
				resnotes = notes.wrapAt(eidx);
				if(resnotes.isSequenceableCollection) {
					note = resnotes.last.copy;
					note[\sustain] = nil;
					note[\legato] = nil;
					note[\dur] = nil;
					note[\delta] = nil;
					ev.putAll( compose.(ev, note) );
					ev = resnotes.collect { arg rnote, idx;
						( rnote[\midinote] + ( ev[\octaveshift] ? 0 * 12 ) )
					}.yield;
					
				} {
					if(resnotes.notNil) {
						note = resnotes.copy;
						note[\sustain] = nil;
						note[\legato] = nil;
						note[\dur] = nil;
						note[\delta] = nil;
						//ev.putAll(note);
						ev.putAll( compose.(ev, note) );
						//note.putAll(ev);
						//ev = note;
						ev = ( resnotes[\midinote] + ( ev[\octaveshift] ? 0 * 12 ) ).yield;
					} {
						ev = \rest.yield;
					};
				};
				ev;
			}).loop,
		);
	},

	filterEngine: { arg self;
		// if we want real control, engine should take the pattern as argument
	},

	demux: { arg self, pat; // not used
		Prout({ arg ev;
			pat.do ({ arg item, idx;
				if(item.isSequenceableCollection) {
					//item.drop(-1).do { arg subitem, idx;
						//subitem[\delta] = subitem.delta;
						//subitem[\dur] = 0;
					//};
					item.do { arg subitem, idx;
						ev = subitem.yield;
					};
				} {
					ev = item.yield;
				};
				ev;
			});
		})
		
	},

	/// instance mode

	new: { arg self, playerGroup;
		self = ProtoClass((parent: self));
		self.playerGroup = playerGroup;
	},

	inArpMode: { arg self;
		self.arpPreset != \disabled
	},

	arpProxy: { arg self;
		if(self[\arpProxy_obj].isNil) {
			self[\arpProxy_obj] = EventPatternProxy.new;
		};
		self[\arpProxy_obj]
	},

	arpPreset: { arg self;
		TagSpecDef(\ArpPreset).addUnique(\disabled);
		\disabled
	},

	addArpListener: { arg self;
		~currentNotes = nil!128;
		if(self.arpListener.notNil) {
			self.arpListener.remove;
		};
		self.arpListener = self.playerGroup.makeListener({ arg child, msg, idx;
			//args.debug("args");
			//[msg, idx].debug("arp note");
			if(msg == \play) {
				~currentNotes[idx] = (midinote:idx);
			} {
				~currentNotes[idx] = nil;
			}
		});
		self.playerGroup.muted = true;
	},

	removeArpListener: { arg self;
		if(self.arpListener.notNil) {
			self.arpListener.remove;
		};
		self.playerGroup.muted = false;
	},

	arpPreset_: { arg self, val;
		self[\arpPreset] = val;
		if(val != \disabled) { // arp enabled
			var pat = self.arperlive({ ~currentNotes }, self.playerGroup.target, Pdef(val));
			if(self.recorder.notNil) {
				self.arpProxy.source = self.recorder.emitterPattern <> pat;
				self.recorder.inArpMode = true;
			} {
				self.arpProxy.source = pat;
			};
			self.addArpListener;
			self.arpProxy.play;
			self.arpPresetHook(val);

		} { // arp disabled
			self.removeArpListener;
			self.arpProxy.stop;
			self.arpPresetHook(val);
			if(self.recorder.notNil) {
				self.recorder.inArpMode = false;
			};

		};
		self.startControl;
		self.changed(\arpPreset);
	},

	asArpButton: { arg self;
		HLayout (
			Param(
				Message(self),
				\arpPreset, 
				TagSpecDef(\ArpPreset),
			).asPopUpMenu,
			PlayerWrapper(self.arpProxy).asView.fixedWidth_(20).label_(""),
		).margins_(0).spacing_(0)
	},

));

