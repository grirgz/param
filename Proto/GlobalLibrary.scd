
(
// libId structure:
// [TypeSymbol, (NameSymbol -> Object)]
ProtoDef(\GlobalLibrary, (

	parent:(
		// init code in parent, property is replaced by init func
		clipFactories: { arg self;
			self.init_default_clip_factories;
		},
		clipFactories_keys: { arg self;
			self.init_default_clip_factories;
			self[\clipFactories_keys];
		},
	),

	edit: { arg self;
		WindowDef(\GlobalLibrary).front;
	},

	///// libId management

	libIdToTarget: { arg self, libid;
		var wr = self.libIdToWrapper(libid);
		if(wr.notNil) {
			wr.getTarget;
		} {
			Log(\Param).error("libIdToTarget: no wrapper for libid %", libid);
			nil
		};
	},

	targetToLibId: { arg self, target;
		var libId;
		var fac;
		if(target.isKindOf(ProtoClass)) {
			var ttype;
			ttype = target.eventType ?? { target.protoClass };
			if(ttype.notNil) {
				if(self.clipFactories.isNil) {
					self.init_default_clip_factories;
				};
				if(self.clipFactories_keys.includes(ttype)) {
					fac = self.clipFactories[ttype];
					//Log(\Param).debug("fac.libIdKey: %", fac.libIdKey);
					//libId = [fac.libIdKey, target.key -> target]
					// TODO: find a way to address anonymous track children
					//	for the moment, i use track.trackKey but no addressing is done
					libId = fac.targetToLibId(target)
				};
			};
		};
		if(target.isKindOf(Pdef)) {
			fac = self.clipFactories.pdef;
			libId = [fac.libIdKey, target.key -> target]
		};
		if(target.isKindOf(Ndef)) {
			fac = self.clipFactories.ndef;
			libId = [fac.libIdKey, target.key -> target]
		};
		libId;
	},

	libIdToStrings: { arg self, libid;
		//Log(\Param).debug("libIdToStrings: %", libid);
		//Log(\Param).debug("libIdToStrings: res %", [libid.first.asString, libid.last.key.asString]);
		[libid.last.key.asString, libid.first.asString]
	},

	libIdToLabel: { arg self, libid;
		var wrapper = self.libIdToWrapper(libid);
		"% (%)".format(wrapper.libIdTargetKey, wrapper.libIdKey);
	},

	libIdToFactory: { arg self, libid;
		var fac;
		if(self.clipFactories.isNil) {
			self.init_default_clip_factories;
		};
		fac = self.clipFactories.detect({ arg fac; fac.libIdKey == libid.first });
		if(fac.isNil) {
			Log(\Param).debug("GlobalLibrary: libIdToFactory: no fac for libid: %", libid);
		};
		fac;
	},

	libIdToWrapper: { arg self, libid;
		var wrapper;
		//Log(\Param).debug("libIdToWrapper: libid:%", libid);
		wrapper = self.libIdToFactory(libid).copy;
		//Log(\Param).debug("libIdToWrapper: wrapper:%", wrapper);
		if(wrapper.notNil) {
			wrapper.libId =  libid;
			wrapper;
		} {
			Log(\Param).error("libIdToWrapper: no wrapper for libid %", libid);
			nil;
		};
	},

	eventToLibId: { arg self, model;
		var libId;
		// 'Pdef',
		// 'Ndef',
		// 'ClipTimeline',
		// 'SampleTimeline',
		// 'NoteTimeline',
		// 'EnvTimeline',
		// 'ModStepTimeline',
		// 'Tracks',

		switch(model.type, 
			\pattern, {
				if(model.timeline.notNil) {
					//Log(\Param).debug("eventToLibId: eventType %, %", model.timeline.eventType, self.clipFactories_keys);
					if(self.clipFactories_keys.includes(model.timeline.eventType)) {
						//Log(\Param).debug("ok");
						libId = self.clipFactories[model.timeline.eventType].eventToLibId(model);
					};
				} {
					var tar = model.pattern;
					if(tar.isKindOf(Pdef)) {
						libId = self.clipFactories.pdef.eventToLibId(model);
					} {
						//model.debug("event_to_clipFactory_key: pattern: unknown");
					}
				}
			},
			\player, {
				var branch = { arg obj;
					switch( obj.class,
						PlayerWrapper, {
							branch.(obj.target)
						},
						Ndef, {
							'ndef'
						},
						Pdef, {
							'pdef'
						},
						Tdef, {
							'tdef'
						},
						Fdef, {
							'fdef'
						},
						TrackDef, {
							'trackdef'
						},
						ProtoClass, {
							obj.eventType ?? { obj.protoClass }
						},
						{
							if(obj.isKindOf(ProtoClass)) {
								obj.eventType ?? { obj.protoClass }
							} {
								//model.debug("event_to_clipFactory_key: player: unknown");
								nil
							};
						}
					);
				};
				var clipkey = branch.(model[\receiver].value);
				var fac;
				if(clipkey.notNil) {
					fac = self.clipFactories[clipkey];
					if(fac.notNil) {
						//Log(\Param).debug("GlobalLibrary.eventToLibId: found fac: fackey: %, ev:%, fac:%", clipkey, model, fac);
						//Log(\Param).debug("fac.eventToLibId %: %", fac.key, fac[\eventToLibId].asCompileString);
						libId = fac.eventToLibId(model);
						//Log(\Param).debug("GlobalLibrary.eventToLibId: found libId: %", libId);
					} {
						Log(\Param).debug("GlobalLibrary.eventToLibId: no factory for %, key:%", model, clipkey);
					}
				} {
					Log(\Param).debug("GlobalLibrary.eventToLibId: no clipkey found for % (%)", model, model.class);
				}
			},
			{
				//model.debug("event_to_clipFactory_key: unknown");
				nil
			}
		);
		//clipkey.debug("event_to_clipFactory_key: result");
		libId;
		
	},

	//// tracklist deprecated

	libIdToTrackInstrument: { arg self, libid, name;
		ProtoDef(\trackInstrument).new(libid, name)
	},

	libIdToScore: { arg self, libid, name;
		self.libIdToWrapper(libid).makeScore(name)
	},

	setTrackInstrument: { arg self, target, trackinstr;
		self.libIdToWrapper(self.targetToLibId(target)).setTrackInstrument(trackinstr)
	},


	//// clip factories

	init_default_clip_factories: { arg superself;
		var tl;
		var parentFactory;
		superself.clipFactories = ();

		parentFactory = (
			libIdTargetKey: { arg self;
				self.libId.last.key; // work only when label is a key
			},

			targetToLibId: { arg self, target;
				[self.libIdKey, target.key -> target];
			},

			getTarget: { arg self;
				self.libId.last.value
			},

			eventToLibId: { arg self, ev;
				var tar;
				//Log(\Param).debug("clipFactories.%.eventToLibId: getEventTarget: %", self.key, self[\getEventTarget].asCompileString);
				tar = self.getEventTarget(ev);
				//Log(\Param).debug("clipFactories.%.eventToLibId: target: %", self.key, tar);
				self.targetToLibId(tar)
			},

			getEvent: { arg self;
				var ev = ();
				self.redefineEvent(ev, self.getTarget);
				ev;
			},

			tagSpec: { arg self;
				TagSpecDef(self.libIdKey)
			},

			keylist: { arg self;
				self.tagSpec.keyList
			},

			label: { arg self;
				PlayerWrapper(self.getTarget).label.asString;
			},

			className: { arg self; // deprecated
				self.classObject
			},

			redefineEvent: { arg self, ev, target;
				self.redefineEventAsPlayerEvent(ev, target)
			},

			redefineEventAsPlayerEvent: { arg self, ev, target;
				// unlike redefineClipAsPlayerEvent, it use target
				PlayerEvent.redefine(ev);
				ev[\receiver] = PlayerWrapper(target);
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},

			redefineEventAsPatternEvent: { arg self, ev, target;
				PatternEvent.redefine(ev);
				// need a closed function to use eventList.asCompileString as preset
				//ev[\timeline] = "{ % }".format(( target ?? {self.getTarget} ).asCompileString).interpret; 
				ev[\pattern] = target;
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},


			/// static methods (not using libId)

			getEventTarget: { arg self, ev;
				// depends pattern event, player event
				if(ev.type == \pattern) {
					// pattern type
					if(ev.timeline.notNil) {
						ev.timeline
					} {
						ev.pattern;
					}
				} {
					// player type
					if(ev.type == \player) {
						if(ev.timeline.notNil) {
							ev.timeline
						} {
							var tar;
							tar = ev.receiver.value;
							//tar.key; // PlayerWrapper has key wrapper
							if(tar.isKindOf(PlayerWrapper)) {
								tar = tar.target;
							};
							if(tar.notNil) {
								tar;
							} {
								Log(\Param).error("clipFactories.parent: ERROR: getEventTarget: receiver is nil");
								nil;
							}; 
						};
					} {
						Log.error("ERROR: can't find target of unknown type event %", ev);
						nil;
					};
				}
			},

			getTargetKeyFromEvent: { arg self, ev;
				PlayerWrapper(self.getEventTarget(ev)).key;
			},

			getName: { arg self, ev; // deprecated
				self.getTargetKeyFromEvent(ev)
			},

			editEventTarget: { arg self, ev;
				PlayerWrapper(self.getEventTarget(ev)).edit;
			},

			editSelected: { arg self, model; // deprecated
				self.editEventTarget(model);
			},

		);
		
		superself.clipFactories_parent = parentFactory;

		superself.clipFactories.ndef = (
			parent: superself.clipFactories_parent,
			key: 'ndef',
			libIdKey: 'Ndef',

			classObject: {
				Ndef
			},
		);

		superself.clipFactories.pdef = (
			parent: superself.clipFactories_parent,
			key: 'pdef',
			libIdKey: 'Pdef',

			redefineEvent: { arg self, ev, target;
				self.redefineEventAsPatternEvent(ev, target)
			},

			classObject: {
				Pdef
			},

		);

		superself.clipFactories.pbindef = (
			parent: superself.clipFactories_parent,
			key: 'pbindef',
			libIdKey: 'Pbindef',
			label: { arg self; self.key.asString },

			keylist: {
				Pbindef.all.keys.asArray.sort; 
			},

			redefineEvent: { arg self, ev, target;
				self.redefineEventAsPatternEvent(ev, target)
			},

			classObject: {
				Pbindef
			},

		);

		superself.clipFactories.tdef = (
			parent: superself.clipFactories_parent,
			key: 'tdef',
			libIdKey: 'Tdef',

			redefineEvent: { arg self, ev, target;
				self.redefineEventAsPlayerEvent(ev, target)
			},

			classObject: {
				Tdef
			},

		);

		superself.clipFactories.fdef = (
			parent: superself.clipFactories_parent,
			key: 'fdef',
			libIdKey: 'Fdef',

			redefineEvent: { arg self, ev, target;
				self.redefineEventAsPlayerEvent(ev, target)
			},

			classObject: {
				Fdef
			},

		);

		//// tracks

		superself.clipFactories.trackdef = (
			parent: superself.clipFactories_parent,
			key: 'trackdef',
			libIdKey: 'TrackDef',

			keylist: {
				TrackDef.all.keys.asArray.sort; 
			},

		);

		superself.clipFactories.tracklist = (
			parent: superself.clipFactories.trackdef,
			key: 'tracklist',
			libIdKey: 'TrackList',
			keylist: {
				TrackDef.all.keys.asArray.sort; 
			},
		);

		superself.clipFactories.tracklistpart = (
			parent: superself.clipFactories.trackdef,
			key: 'tracklistpart',
			libIdKey: 'TrackListPart',
			tagSpec: {
				TagSpecDef(\TrackListPart)
			}
		);

		superself.clipFactories.playergrid = (
			parent: superself.clipFactories.trackdef,
			key: 'playergrid',
			libIdKey: 'PlayerGrid',
			keylist: {
				TrackDef.all.keys.asArray.sort; 
			},
		);

		// generic timeline factory
		tl = (
			parent: superself.clipFactories_parent,
			key: 'generictimeline',

			classObject: {
				ProtoClassDef(\NoteTimeline)
			},

			keylist: { arg self;
				self.className.all.keys.asArray.sort;
			},

			redefineEvent: { arg self, ev, target;
				self.redefineEventAsPatternEvent(ev, target)
			},

			redefineEventAsPlayerEvent: { arg self, ev, target;
				// unlike redefineClipAsPlayerEvent, it use target
				ev.putAll(target.asPlayerEvent);
				ev.parent = PlayerEvent.defaultParent;
				superself.parent_model.changed(\refresh);
			},

			redefineEventAsPatternEvent: { arg self, ev, target;
				ev.putAll(target.asPatternEvent);
				ev.parent = PatternEvent.defaultParent;
				superself.parent_model.changed(\refresh);
			},

		);

		superself.clipFactories_timeline_parent = tl;

		[\noteTimeline, \clipTimeline, \modStepTimeline, \kitTimeline].do { arg key;
			var uckey = (key.asString[0].toUpper ++ key.asString[1..]).asSymbol;
			superself.clipFactories[key] = (
				key: key,
				libIdKey: uckey,
				parent: tl,
				classObject: "{ ProtoClassDef(%) }".format(uckey.asCompileString).interpret,
			);
		};
		[\paramTimeline, \sampleTimeline].do { arg key;
			var uckey = (key.asString[0].toUpper ++ key.asString[1..]);
			superself.clipFactories[key] = (
				key: key,
				libIdKey: uckey.asSymbol,
				parent: tl,
				classObject: "{ ProtoClassDef(%) }".format(uckey.asCompileString).interpret,
			);
		};

		superself.clipFactories.clip = (
			key: \clip,
			libIdKey: \Clip,
			parent: parentFactory,
			getEvent: { arg self;
				self.getTarget.copy;
			},
		);

		//superself.clipFactories.sampleTimeline = (
			//key: 'sampleTimeline',
			//libIdKey: \SampleTimeline,
			//parent: tl,
			//classObject: { SampleTimeline.proto },
		//);

		superself.init_other_factories;

		superself.clipFactories_keys = superself.clipFactories.keys.asArray.sort;
		
		superself.clipFactories
	},

	init_other_factories: { arg superself;
		superself.clipFactories.patkitdef = ().putAll(superself.clipFactories_parent).putAll((
			key: 'patkitdef',
			libIdKey: 'PatKitDef',
			label: { arg self; self.key.asString },
			keylist: {
				PatKitDef.all.keys.asArray.sort; 
			},

			getEventTarget: { arg self, ev;
				// depends pattern event, player event
				if(ev.pattern.isKindOf(Pdef)) {
					// pattern type
					ev.pattern;
				} {
					// player type
					var tar = ev.receiver.value;
					tar.key; // PlayerWrapper has key wrapper
					if(tar.isKindOf(PlayerWrapper)) {
						tar = tar.target;
					};
					if(tar.isKindOf(Pdef)) {
						tar;
					} {
						Log(\Param).error("clipFactories.patkitdef: ERROR: getEventTarget: don't know what type!!!");
						nil;
					}; 
				}
				
			},

			getTarget: { arg self;
				PatKitDef(self.libIdTargetKey)
			},

			classObject: {
				PatKitDef
			},

			getEventTargetKey: { arg self, ev;
				self.getTarget(ev).key;
			},

			editSelected: { arg self, model;
				var player;
				//player = model.receiver.value;
				//player.asParamGroup.debug("Edit: pattern");
				//if(player.class == PlayerWrapper) {
				//	player = player.target;
				//};
				//player.debug("clipeditor: player");
				//player.getHalo(\edit).debug("clipeditor: player, edit");
				player = self.getTarget(model);
				if(player.isKindOf(Pdef)) {
					if(player.getHalo(\edit).notNil) {
						//player.getHalo(\edit).debug("clipeditor: player, halo edit");
						player.getHalo(\edit).value;
					} {
						//player.getHalo(\edit).debug("clipeditor: normal edit");
						player.asParamGroup.edit;
					}
				} {
					Log(\Param).error("clipFactories.patkitdef: ERROR: edit: don't know what to do with type of %", player)
				}
			},

			redefineClip: { arg self, curname, model;
				PatternEvent.redefine(model);
				model[\pattern] = Pdef(curname);
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey;
				(
					timeline: KitTimeline(scorekey, self.getTarget)
				)
			},
		));

		superself.clipFactories.instrumenttrack = (
			parent: superself.clipFactories.clipFactories_parent,
			key: 'instrumenttrack',
			libIdKey: 'InstrumentTrack',
			label: { arg self; self.key.asString },

			tagSpec: { arg self;
				TagSpecDef(\InstrumentTrack)
			},

			targetToLibId: { arg self, target;
				[self.libIdKey, target.trackKey -> target];
			},

			libIdTargetKey: { arg self;
				self.libId.last.key; // work only when label is a key
			},


			getEventTarget: { arg self, ev;
				// depends pattern event, player event
				if(ev.pattern.isKindOf(Pdef)) {
					// pattern type
					ev.pattern;
				} {
					// player type
					var tar = ev.receiver.value;
					tar.key; // PlayerWrapper has key wrapper
					if(tar.isKindOf(PlayerWrapper)) {
						tar = tar.target;
					};
					if(tar.isKindOf(Pdef)) {
						tar;
					} {
						Log(\Param).error("clipFactories.instrumenttrack: ERROR: getEventTarget: don't know what type!!!");
						nil;
					}; 
				}
				
			},

			getTarget: { arg self;
				// libid: [type, label -> linePlayer]
				// NOTE: not by key reference, directly use object
				self.libId.last.value;
			},

			classObject: {
				// TODO
				TrackDef
			},

			getEventTargetKey: { arg self, ev;
				self.getTarget(ev).key;
			},

			editSelected: { arg self, model;
				var player;
				//player = model.receiver.value;
				//player.asParamGroup.debug("Edit: pattern");
				//if(player.class == PlayerWrapper) {
				//	player = player.target;
				//};
				//player.debug("clipeditor: player");
				//player.getHalo(\edit).debug("clipeditor: player, edit");
				player = self.getTarget(model);
				if(player.isKindOf(Pdef)) {
					if(player.getHalo(\edit).notNil) {
						//player.getHalo(\edit).debug("clipeditor: player, halo edit");
						player.getHalo(\edit).value;
					} {
						//player.getHalo(\edit).debug("clipeditor: normal edit");
						player.asParamGroup.edit;
					}
				} {
					Log(\Param).error("clipFactories.instrumenttrack: ERROR: edit: don't know what to do with type of %", player)
				}
			},

			redefineClip: { arg self, curname, model;
				self.redefineClipAsPatternEvent(curname, model);
			},

			redefineClipAsPatternEvent: { arg self, curname, model;
				PatternEvent.redefine(model);
				// need a closed function to use eventList.asCompileString as preset
				model[\timeline] = "{ % }".format(self.getTarget.refCompileString).interpret; 
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},


			redefineClipAsPlayerEvent: { arg self, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = PlayerWrapper(self.getTarget);
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey;
				(
					timeline: KitTimeline(scorekey, self.getTarget)
				)
			},
		);

		superself.clipFactories.instrumenttrackpart = ().putAll(superself.clipFactories_parent).putAll((
			key: 'instrumenttrackpart',
			libIdKey: 'InstrumentTrackPart',
			label: { arg self; self.key.asString },
			keylist: {
				// TODO (done in TagSpecDef)
				[]
			},

			libIdTargetKey: { arg self;
				self.libId.last.key; // work only when label is a key
			},


			targetToLibId: { arg self, target;
				[self.libIdKey, target.trackKey -> target];
			},

			getEventTarget: { arg self, ev;
				// depends pattern event, player event
				if(ev.pattern.isKindOf(Pdef)) {
					// pattern type
					ev.pattern;
				} {
					// player type
					var tar = ev.receiver.value;
					Log(\Param).debug("clipFactories.instrumenttrackpart.getEventTarget %, %, key:", ev, ev.receiver.value, ev.receiver.value.key);
					if(tar.isKindOf(PlayerWrapper)) {
						tar = tar.target;
					};
					if(tar.notNil) {
						tar;
					} {
						Log(\Param).error("clipFactories.instrumenttrackpart: ERROR: getEventTarget: don't know what type!!!");
						nil;
					}; 
				}
				
			},

			getTarget: { arg self;
				// libid: [type, label -> linePlayer]
				// NOTE: not by key reference, directly use object
				self.libId.last.value;
			},

			classObject: {
				// TODO
				TrackDef
			},

			getEventTargetKey: { arg self, ev;
				self.getTarget(ev).key;
			},

			editSelected: { arg self, model;
				var player;
				//player = model.receiver.value;
				//player.asParamGroup.debug("Edit: pattern");
				//if(player.class == PlayerWrapper) {
				//	player = player.target;
				//};
				//player.debug("clipeditor: player");
				//player.getHalo(\edit).debug("clipeditor: player, edit");
				player = self.getTarget(model);
				if(player.isKindOf(Pdef)) {
					if(player.getHalo(\edit).notNil) {
						//player.getHalo(\edit).debug("clipeditor: player, halo edit");
						player.getHalo(\edit).value;
					} {
						//player.getHalo(\edit).debug("clipeditor: normal edit");
						player.asParamGroup.edit;
					}
				} {
					Log(\Param).error("clipFactories.instrumenttrackpart: ERROR: edit: don't know what to do with type of %", player)
				}
			},

			redefineClip: { arg self, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = PlayerWrapper(self.getTarget);
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey;
				(
					timeline: KitTimeline(scorekey, self.getTarget)
				)
			},
		));

		superself.clipFactories.bufdef = ().putAll(superself.clipFactories_parent).putAll((
			key: 'bufdef',
			libIdKey: 'BufDef',
			label: { arg self; self.key.asString },
			keylist: {
				BufDef.all
			},

			libIdTargetKey: { arg self;
				self.libId.last.key; // work only when label is a key
			},


			getEventTarget: { arg self, ev;
				// TODO
				// depends pattern event, player event
				if(ev.pattern.isKindOf(Pdef)) {
					// pattern type
					ev.pattern;
				} {
					// player type
					var tar = ev.receiver.value;
					tar.key; // PlayerWrapper has key wrapper
					if(tar.isKindOf(PlayerWrapper)) {
						tar = tar.target;
					};
					if(tar.isKindOf(Pdef)) {
						tar;
					} {
						Log(\Param).error("clipFactories.bufdef: ERROR: getEventTarget: don't know what type!!!");
						nil;
					}; 
				}
				
			},

			getTarget: { arg self;
				// libid: [type, label -> linePlayer]
				// NOTE: not by key reference, directly use object
				self.libId.last.value;
			},

			classObject: {
				// TODO
				BufDef
			},

			getEventTargetKey: { arg self, ev;
				self.getTarget(ev).key;
			},

			editSelected: { arg self, model;
				var player;
				//player = model.receiver.value;
				//player.asParamGroup.debug("Edit: pattern");
				//if(player.class == PlayerWrapper) {
				//	player = player.target;
				//};
				//player.debug("clipeditor: player");
				//player.getHalo(\edit).debug("clipeditor: player, edit");
				player = self.getTarget(model);
				if(player.isKindOf(Pdef)) {
					if(player.getHalo(\edit).notNil) {
						//player.getHalo(\edit).debug("clipeditor: player, halo edit");
						player.getHalo(\edit).value;
					} {
						//player.getHalo(\edit).debug("clipeditor: normal edit");
						player.asParamGroup.edit;
					}
				} {
					Log(\Param).error("clipFactories.bufdef: ERROR: edit: don't know what to do with type of %", player)
				}
			},

			redefineClip: { arg self, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = PlayerWrapper(self.getTarget);
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey;
				(
					timeline: KitTimeline(scorekey, self.getTarget)
				)
			},
		));

		// instrument of InstrumentTrack, used to create new timelines
		// maybe deprecated
		superself.clipFactories.trackinstrument = ().putAll(superself.clipFactories_parent).putAll((
			key: 'trackinstrument',
			libIdKey: 'TrackInstrument',
			label: { arg self; self.key.asString },
			keylist: {
				// TODO (done in TagSpecDef)
				[]
			},

			targetToLibId: { arg self, target;
				[self.libIdKey, target.trackKey -> target];
			},

			libIdTargetKey: { arg self;
				self.libId.last.key; // work only when label is a key
			},


			getEventTarget: { arg self, ev;
				// depends pattern event, player event
				if(ev.pattern.isKindOf(Pdef)) {
					// pattern type
					ev.pattern;
				} {
					// player type
					var tar = ev.receiver.value;
					tar.key; // PlayerWrapper has key wrapper
					if(tar.isKindOf(PlayerWrapper)) {
						tar = tar.target;
					};
					if(tar.isKindOf(Pdef)) {
						tar;
					} {
						Log(\Param).error("clipFactories.trackinstrument: ERROR: getEventTarget: don't know what type!!!");
						nil;
					}; 
				}
				
			},

			getTarget: { arg self;
				// libid: [type, label -> linePlayer]
				// NOTE: not by key reference, directly use object
				self.libId.last.value;
			},

			classObject: {
				// TODO
				ProtoClass
			},

			getEventTargetKey: { arg self, ev;
				self.getTarget(ev).key;
			},

			editSelected: { arg self, model;
				var player;
				//player = model.receiver.value;
				//player.asParamGroup.debug("Edit: pattern");
				//if(player.class == PlayerWrapper) {
				//	player = player.target;
				//};
				//player.debug("clipeditor: player");
				//player.getHalo(\edit).debug("clipeditor: player, edit");
				player = self.getTarget(model);
				if(player.isKindOf(Pdef)) {
					if(player.getHalo(\edit).notNil) {
						//player.getHalo(\edit).debug("clipeditor: player, halo edit");
						player.getHalo(\edit).value;
					} {
						//player.getHalo(\edit).debug("clipeditor: normal edit");
						player.asParamGroup.edit;
					}
				} {
					Log(\Param).error("clipFactories.trackinstrument: ERROR: edit: don't know what to do with type of %", player)
				}
			},

			redefineClip: { arg self, curname, model;
				self.redefineClipAsPatternEvent(curname, model);
			},

			redefineClipAsPatternEvent: { arg self, curname, model;
				PatternEvent.redefine(model);
				// need a closed function to use eventList.asCompileString as preset
				model[\timeline] = "{ % }".format(self.getTarget.refCompileString).interpret; 
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},


			redefineClipAsPlayerEvent: { arg self, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = PlayerWrapper(self.getTarget);
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey;
				(
					timeline: KitTimeline(scorekey, self.getTarget)
				)
			},
		));

	},
));

//TagSpecDef(\SynthDef).clear; // to debug;

/////////////////////////////
///// Tag definitions
// there is raw tags like Pdef or Ndef in the form \name -> object
// all theses tags are registered in the GlobalLibraryType tag
// Since there is a lot of types, we want to group them in categories, 
//		the list is stored in GlobalLibraryCategory tag
// Each category is defined in the tag GlobalLibrary.type.CATEGORYNAME with the list of raw tags

TagSpecDef(\SynthDef_all, {
	SynthDescLib.all.values.asList.collect({arg lib; lib.synthDescs.asAssociations}).flat.sort;
});

TagSpecDef(\SynthDef_keys, {
	var blocklist = TagSpecDef(\SynthDef_blocklist).valueList.collect({ arg x; Constraint(x) });
	blocklist = blocklist.reduce('or');
	SynthDescLib.all.values.asList.collect({arg lib; lib.synthDescs.keys.as(Array) }).flat.reject({ arg item;
		blocklist.value(item)
	}).sort;
}).default_(\default);

TagSpecDef(\SynthDef, {
	var blocklist = TagSpecDef(\SynthDef_blocklist).valueList.collect({ arg x; Constraint(x) });
	blocklist = blocklist.reduce('or');
	SynthDescLib.all.values.asList.collect({arg lib; lib.synthDescs.asAssociations}).flat.reject({ arg item;
		blocklist.value(item.key)
	});
});

TagSpecDef(\SynthDef_blocklist, 
	[
		\system_, 
		\CTKMeta_, 
		\Param_buffer_player, 
		\PmodEnv_,
		\PnoteEnv_,
		\ParamTimeline_segment,
		\Pmod_constant,
	].collect { arg item, idx;
		var itemstr = item.asString;
		item -> { arg key; key.asString.beginsWith(itemstr) };
	}
);

//// jitlib

TagSpecDef(\Pdef, {
	Pdef.all.asAssociations;
});

TagSpecDef(\Ndef, {
	if(Ndef.all[\localhost].notNil) {
		Ndef.all[\localhost].envir.asAssociations
	} {
		[];
	}
});

TagSpecDef(\Tdef, {
	Tdef.all.asAssociations;
});

TagSpecDef(\Fdef, {
	Fdef.all.asAssociations;
});

//// timelines

TagSpecDef(\SampleTimeline, {
	SampleTimeline.proto.all.asAssociations;
});

TagSpecDef(\KitTimeline, {
	ProtoClassDef(\KitTimeline).all.asAssociations;
});

TagSpecDef(\ClipTimeline, {
	ProtoClassDef(\ClipTimeline).all.asAssociations;
});

TagSpecDef(\NoteTimeline, {
	ProtoClassDef(\NoteTimeline).all.asAssociations;
});

TagSpecDef(\ParamTimeline, {
	ProtoClassDef(\ParamTimeline).all.asAssociations;
});


TagSpecDef(\PatKitDef, {
	PatKitDef.all.asAssociations;
});

TagSpecDef(\TrackMixerDef, {
	ProtoClassDef(\TrackMixerDef).all
});

TagSpecDef(\TrackMixerDef_insertFx, {
	TagSpecDef(\TrackMixerDef).valueList.collect { arg mixer, idx;
		mixer.insertFxList.select { arg fx, idx;
			fx.notNil and: {  fx.isEmpty == false }
		}.collect { arg fx, idx;
			"%_insertFx%".format(mixer.key, fx.relIndex).asSymbol -> fx
		};
	}.flatten;
});


/////

TagSpecDef(\Modulators, {
	TagSpecDef(\ParamCombinatorInput).list 
	++ TrackDef(\ModulatorRack).existingChildren.collect({ arg child, idx;
		child.trackKey -> child
	})
});

///// BusDef

TagSpecDef(\BusDef, {
	TagSpecDef(\BusDef_object).list
});

TagSpecDef(\BusDef_index, {
	[\0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k).index }) 
});

// harder to get a presetCompileString when the bus is numeric
TagSpecDef(\BusDef_object, {
	[\0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k) }) 
});

TagSpecDef(\BusDef_asMap, {
	[\nil->nil] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k).asMap }) 
});

[\audio, \control].do { arg rate;

	TagSpecDef(( \BusDef_++rate ).asSymbol, {
		[\nil -> nil] ++ TagSpecDef(\BusDef_object).list.select { arg asso, idx;
			if(asso.value.isKindOf(Bus)) {
				asso.value.rate == rate
			} {
				true
			}
		};
	});
	
	TagSpecDef(( \BusDef_++rate++\_asMap ).asSymbol, {
		TagSpecDef(( \BusDef_++rate ).asSymbol).list.collect { arg asso, idx;
			if(asso.value.isKindOf(Bus)) {
				asso.key -> asso.value.asMap 
			} {
				asso
			}
		};
	});

	[\mono, \stereo].do { arg chantype, numchan;
		numchan = numchan + 1;

		TagSpecDef(( \BusDef_++rate++\_++chantype ).asSymbol, {
			[\nil -> nil] ++ TagSpecDef(\BusDef_object).list.select { arg asso, idx;
				if(asso.value.isKindOf(Bus)) {
					asso.value.rate == rate and: { asso.value.numChannels == numchan }
				} {
					true
				}
			}
		});

		TagSpecDef(( \BusDef_++rate++\_++chantype++\_asMap ).asSymbol, {
			TagSpecDef(( \BusDef_++rate++\_++chantype ).asSymbol).list.collect { arg asso, idx;
				if(asso.value.isKindOf(Bus)) {
					asso.key -> asso.value.asMap 
				} {
					asso
				}
			};
		});
	};
};


///// BufDef

TagSpecDef(\BufDef, {
	[\0->0] ++ BufDef.all.keys.as(Array).sort.collect({ arg k; k -> BufDef(k).bufnum }) 
});

TagSpecDef(\BufDef_nozero, {
	BufDef.all.keys.as(Array).sort.collect({ arg k; k -> BufDef(k).bufnum }) 
});

TagSpecDef(\BufDef_mono, {
	BufDef.bufferList(1)
		.collect({ arg kv; kv.key -> kv.value.bufnum })
});

TagSpecDef(\BufDef_stereo, {
	BufDef.bufferList(2)
		.collect({ arg kv; kv.key -> kv.value.bufnum })
});

TagSpecDef(\BusDefDefault, {
	[\default -> nil] ++ TagSpecDef(\BusDef).list
});

TagSpecDef(\WavetableDef, {
	WavetableDef.all.keys.as(Array).sort.collect({ arg k; k -> WavetableDef(k).bufnum }) 
});

TagSpecDef(\WavetableDef_object, {
	WavetableDef.all.keys.as(Array).sort.collect({ arg k; k -> WavetableDef(k) }) 
});

TagSpecDef(\Spec, {
	Spec.specs
});

///////////////////

//TagSpecDef(\TrackList, { }); // updated by TrackTemplateDef(\TrackList) when adding a track

TagSpecDef(\TrackListPart, {
	var tracklists = TagSpecDef(\TrackList).valueList;
	tracklists.collect({ arg tracklist;
		var sizes = tracklist.existingChildren.collect({ arg channel;
			// for each channel, count the number of part
			channel.existingChildren.size; 
		});
		var channelIdxWithMostParts = sizes.maxIndex;
		// now the max number of part is the number of part of the tracklist
		if(sizes.size > 0) {

			(0..sizes[channelIdxWithMostParts]-1).collect({ arg idx;
				// format: (libLabel -> libObj)
				// type is added by TagSpecDef key
				var line = tracklist.linePlayerAt(idx);
				//(("%.part%".format(tracklist.key, idx)) -> ( tracklist.linePlayerAt(idx) ));
				line.trackKey -> line;
			})
		} {
			nil
		}
	}).flatten.select(_.notNil)
});

TagSpecDef(\InstrumentTrack, {
	TagSpecDef(\TrackList).valueList.collect({ arg tracklist;
		tracklist.existingChildren.collect { arg track;
			track.trackKey -> track
		}
	}).flatten;
});


// maybe should rename to TrackInstrument_ClipTimeline
TagSpecDef('TrackInstrument', [ // maybe deprecated
	\ClipTimelineTrack,
	\KitTimelineTrack,
	\NoteTimelineTrack,
	\SampleTimelineTrack,
	\ParamTimelineTrack,
]);

///// new objects

TagSpecDef(\ClipTimeline_newPlayer, [
	'ClipTimeline' -> { arg name; ClipTimeline(name) },
	'NoteTimeline' -> { arg name; NoteTimeline(name) },
	'KitTimeline' -> { arg name; KitTimeline(name) },
	'SampleTimeline' -> { arg name; SampleTimeline(name) },
	'ParamTimeline' -> { arg name; ParamTimeline(name) },
	'TrackList' -> { arg name; TrackDef(name, \CustomTrackList) },
	'PlayerGrid' -> { arg name; TrackDef(name, \playergrid) },
	//'TrackTimeline' -> { arg name; TrackTimeline(name) },
	//'Pdef' -> { arg name; Pdef(name) },
	'Pbindef' -> { arg name; Pbindef(name) },
	'Ndef' -> { arg name; Ndef(name) },
	'Tdef' -> { arg name; Tdef(name) },
	'Fdef' -> { arg name; Fdef(name) },
]);

TagSpecDef(\MainProject_newObject, {
	TagSpecDef(\ClipTimeline_newPlayer).list ++ [
		\PatKitDef -> { arg name; PatKitDef(name) },
		\TrackMixerDef -> { arg name; TrackMixerDef(name) },
	]
});


//////////////////////////////////////
////////////////////////// Categories

// FIXME: rename to GlobalLibrary.type
TagSpecDef(\GlobalLibraryType, [
	\BusDef,
	\BufDef,
	\SynthDef,
	\Pdef,
	\Ndef,
	\Tdef,
	\Fdef,
	\NoteTimeline,
	\TrackTimeline,
	\KitTimeline,
	\SampleTimeline,
	\ClipTimeline,
	\ParamTimeline,
	\PatKitDef,
	\Sample,
	\TrackListPart,
	\InstrumentTrackPart,
	\TrackInstrument,
	\ProtoPlayer,
]);

// TODO: add GlobalLibrary.tag which should be the list of tags displayed by browser

// FIXME: rename to GlobalLibrary.category
TagSpecDef(\GlobalLibraryCategory, [
	\TrackInstrument,
	\Effect,
	\Score,
	\Preset,
	\Kit,
	\TrackPart,
	\TrackGrid,
	\LineTrack,
	\Track,
	\AllTracks,
	\Clip,
	\Timeline,
	\Editable,
	\Player,
	\AudioBuffer,
	\SynthDef,
]);

//TagSpecDef('GlobalLibrary.type.TrackInstrument', [
	//\ClipTimelineTrackX,
//]);


// FIXME: rename to GlobalLibrary.category.TrackInstrument
TagSpecDef('GlobalLibrary.type.TrackInstrument', [ // maybe deprecated
	\TrackInstrument,
]);

TagSpecDef('GlobalLibrary.type.SynthDef', [
	\SynthDef,
]);

TagSpecDef('GlobalLibrary.type.Preset', [
	\SynthDefPreset,
]);

TagSpecDef('GlobalLibrary.type.TrackInstrumentTarget', [ // maybe deprecated
	\SynthDef,
	\Pdef,
	\Ndef,
	\PatKitDef,
	\BufDef,
	\Sample,
]);

TagSpecDef('GlobalLibrary.type.Clip', [ // deprecated, use Player instead
	//\SynthDef, // FIXME: not implemented yet
	//\BufDef, // FIXME: not implemented yet
	\Sample,
	\Pdef,
	\Ndef,
	\Tdef,
	\Fdef,
	\NoteTimeline,
	\TrackTimeline,
	\KitTimeline,
	\SampleTimeline,
	\ClipTimeline,
	\ParamTimeline,
	\ProtoPlayer,
]);

TagSpecDef('GlobalLibrary.type.Timeline', [
	\NoteTimeline,
	\TrackTimeline,
	\KitTimeline,
	\SampleTimeline,
	\ClipTimeline,
	\ParamTimeline,
]);

TagSpecDef('GlobalLibrary.type.Kit', [
	\PatKitDef,
]);

TagSpecDef('GlobalLibrary.type.Editable', [
	\NoteTimeline,
	\TrackTimeline,
	\KitTimeline,
	\SampleTimeline,
	\ClipTimeline,
	\ParamTimeline,
	\PatKitDef,
	\Pdef,
	\Ndef,
]);

TagSpecDef('GlobalLibrary.type.TrackGrid', [
	\playergrid,
	\TrackList,
	\CustomTrackList,
]);

TagSpecDef('GlobalLibrary.type.LineTrack', [
	\lineplayer,
	\TrackListPart,
]);

TagSpecDef('GlobalLibrary.type.TrackPart', [
	\InstrumentTrackPart,
	\playerwrapper,
]);


TagSpecDef('GlobalLibrary.type.Track', [
	\InstrumentTrack,
	\playertrack,
]);

TagSpecDef('GlobalLibrary.type.AudioBuffer', [
	\BufDef,
]);

// group track players of all kind and level
TagSpecDef('GlobalLibrary.type.AllTracks', {
	[\TrackGrid, \LineTrack, \TrackPart, \Track].collect { arg item, idx;
		TagSpecDef("GlobalLibrary.type.%".format(item).asSymbol).list
	}.flat;
});

TagSpecDef('GlobalLibrary.type.Player', {
	[
		\Pdef,
		\Ndef,
		\Tdef,
		\Fdef,
		\NoteTimeline,
		\TrackTimeline,
		\KitTimeline,
		\SampleTimeline,
		\ClipTimeline,
		\ParamTimeline,
		\CustomTrackList,
		\ProtoPlayer,
		\TrackMixerDef,
		\TrackMixerDef_insertFx,
	] ++
	[\TrackGrid, \LineTrack, \TrackPart, \Track].collect { arg item, idx;
		TagSpecDef("GlobalLibrary.type.%".format(item).asSymbol).list
	}.flat;
});

///////// Synthdef Tags
// WIP


//TagSpecDef(\SynthDefTags).addUnique(\default -> [\nice]);
////TagSpecDef(\SynthDefTags).list

//TagSpecDef(\SynthDefTags_inversed, {
	//var res = IdentityDictionary.new;
	//TagSpecDef(\SynthDefTags).list.do { arg asso;
		//asso.value.collect({ arg tag;
			//res[tag] = res[tag] ?? { IdentitySet() };
			//res[tag] = res[tag].add(asso.key);
		//})
	//};
	//res.asAssociations
//});
//TagSpecDef(\SynthDefTags_inversed).list

/////////////////////////////////////////////////////////////////
///////// Browser GUI
////////////////////////////////////////////////////////////////


// this is the library browser or select dialog
WindowDef(\GlobalLibrary, { arg def, lib, selectedCallback, categoryWhitelist, defaultValue;


	var resultView;
	var searchBox;
	var typeView;
	var categoryView;
	var tagView;
	var lay;
	var itemIds = List.new;
	var updateItems, updateTypes;
	var treeViewItemToLibId;
	var tagList;


	treeViewItemToLibId = { arg item;
		//itemIds[item.index];
		itemIds.detect({ arg id; id.strings == item.strings })
	};

	lib = lib ?? { ProtoDef(\GlobalLibrary) };

	searchBox = TextField.new.action_({
		//"action!".debug;
			def.changed(\search);
			def.changed(\category);
	}).keyUpAction_({ arg me, key, modifiers, unicode, keycode;
		//[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
		//def.changed(\results);
		updateItems.()
	}).focus(true);

	updateItems = {
		var view = resultView;
		var oldTagSelection;
		var model = def;

		var superitem;
		var selectedTypes;
		var selectedCategories;
		var filteredTypes;

		//Log(\Param).debug("updateItems");

		//// type filtering
		selectedTypes = typeView.selection.collect({ arg idx; typeView.items[idx] });

		if(selectedTypes.size == 0) {
			selectedTypes = typeView.items;
		};

		view.clear;

		//[model, view, msg, selectedTypes, selectedCategories, filteredTypes].debug("category change");
		itemIds = selectedTypes.collect({ arg typeitem;
			//Log(\Param).debug("typeitem %", typeitem);
			TagSpecDef(typeitem.value).list.collect ({ arg item;
				//Log(\Param).debug("item %", item);
				[typeitem, item];
			})
		}).flatten;
		//Log(\Param).debug("itemIds %", itemIds);

		tagList = Set.new;
		itemIds = itemIds.collect({ arg id;
			//// build models and collect tags
			var tags = Halo.at(id.last.value, \tag) !? _.keys ?? {[]};
			var moretags;
			//var tags = [];
			moretags = TagSpecDef(\SynthDefTags).asDict[id.last.key];
			if(moretags.notNil) {
				tags = tags ++ moretags;
			};
			tagList.addAll(tags);
			(
				strings: lib.libIdToStrings(id) ++ [tags.asArray.join(", ")],
				//strings: lib.libIdToStrings(id),
				tags: tags,
				libId: id,
			);
		}).select({ arg obj;
			//// searchbox filtering
			//Log(\Param).debug("search strings to add %", obj.strings);
			if(searchBox.string.trim != "") {
				obj.strings.join(" ").toLower.contains(searchBox.string.toLower)
			} {
				true
			}
		}).select({ arg obj;
			//// tag filtering
			if(
				obj.tags.includes(\system) and: { 
					tagView.selection.any({ arg idx; tagView.items[idx] == \system }).not }
			) {
				// hide system objects
				false
			} {
				if( tagView.selection.size == 0 ) {
					true
				} {
					tagView.selection.any({ arg idx;
						obj.tags.includes(tagView.items[idx])
					})
				}
			}
		});
		
		itemIds.do { arg obj;
			//Log(\Param).debug("strings to add %", obj.strings);
			view.addItem(obj.strings)
		};

		//tagList.debug("tagList");
		oldTagSelection = tagView.selection;
		tagView.items = tagList.asArray;
		tagView.selection = oldTagSelection;

	};

	updateTypes = {
		var selectedTypes;
		var selectedCategories;
		var filteredTypes;
		var allTypes;
		var tmpAction;
		//Log(\Param).debug("updateTypes");

		selectedTypes = typeView.selection.collect({ arg idx; typeView.items[idx] });
		selectedCategories = categoryView.selection.collect({ arg idx; categoryView.items[idx] });

		if(selectedCategories.size == 0) {
			selectedCategories = categoryView.items;
		};
		filteredTypes = selectedCategories.collect({ arg superkey;
			var rlist;
			rlist = TagSpecDef("GlobalLibrary.type.%".format(superkey).asSymbol).keyList;

			// allow types in categoryWhitelist
			//rlist.debug("rlist");
			if(rlist.size == 0) {
				if(TagSpecDef(\GlobalLibraryType).keyList.includes(superkey)) {
					rlist = [ superkey ];
				};
			};

			rlist;
		}).flat.asSet.asArray;

		tmpAction = typeView.selectionAction; // avoid calling typeView.selectionAction
		typeView.selectionAction = nil;
		typeView.items = filteredTypes.sort; // call selectionAction_
		typeView.selection = []; // call selectionAction_
		typeView.selectionAction = tmpAction;

		updateItems.();
		
		
	};

	typeView = ListView.new
		.selectionMode_(\multi)
		.selection_([])
		.selectionAction_({
			// can use selectionAction because updateTypes skip selectionAction calls
			// content set by categoryView via updateTypes
			//["typeView selected"].debug;
			updateItems.();
			//def.changed(\results);
		})
	;

	categoryView = ListView.new
		.items_(
			{
				var catlist = TagSpecDef(\GlobalLibraryCategory).keyList;
				var typelist = TagSpecDef(\GlobalLibraryType).keyList;
				var rlist;

				rlist = catlist.select({ arg superitem;
					if(categoryWhitelist.notNil) {
						categoryWhitelist.includes(superitem)
					} {
						true
					};
				}).flat.asArray;

				// allow types in categoryWhitelist
				categoryWhitelist.do({ arg item;
					//item.debug("item");
					//[typelist.includes(item), typelist].debug("typelist");
					if(catlist.includes(item).not and: { typelist.includes(item) }) {
						rlist = rlist.add(item);
					};
				});
				//rlist.debug("rlist");

				rlist;
				
			}.value
		)
		.selectionMode_(\multi)
		.selection_([])
		.selectionAction_({ arg view;
			// can use selectionAction because its content is static
			//view.selection.debug("cat action");
			updateTypes.();
		})
	;

	tagView = ListView.new
		.selection_([])
		.selectionAction_({
			// if set, this will be fired inside updateItems function
			//updateItems.();
		})
		.selectionMode_(\multi)
		.mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			//view.selection.debug("cat mouse up");
			//def.changed(\results);
			updateItems.();
			
		})
	;

	resultView = TreeView.new
		.columns_(["Name", "Type", "Tags"])
		.setColumnWidth(0,300)
		.itemPressedAction_({ arg view;
			var itemId = treeViewItemToLibId.(view.currentItem);
			//Log(\Param).debug("itemPressedAction_ itemIds:%, idx:%", itemIds.size, view.currentItem.index);
			//Log(\Param).debug("itemPressedAction_ itemId:%, str:%", itemIds[view.currentItem.index], view.currentItem.strings);
			//Log(\Param).debug("itemPressedAction_ real itemId:%, str:%", treeViewItemToLibId.(view.currentItem), view.currentItem.strings);
			selectedCallback.(itemId.libId, view.currentItem.strings)
		})
		.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(clickCount == 2) {
				var itemId = treeViewItemToLibId.(view.currentItem);
				selectedCallback.(itemId.libId, view.currentItem.strings, \double)
			};
		})
		.canSort_(true) // imply to find item by its strings, ugly but works (treeViewItemToLibId)
	;

	updateTypes.();
	//resultView.followChange(def, \results, { arg view, model, msg, arg1;
		//updateItems.();
	//}, false);
	resultView.currentItem_(
		if(defaultValue.notNil) {
			var libIds;
			var idx;
			updateItems.();
			libIds = itemIds.collect(_.libId);
			//[defaultValue, libIds.indexOf(defaultValue), libIds.indexOfEqual(defaultValue), libIds].debug("default value");
			idx = libIds.indexOfEqual(defaultValue);
			if(idx.notNil) {
				resultView.itemAt(idx);
			} {
				nil
			};
		} {
			nil
		}
	);
	if(resultView.currentItem.notNil) {
		selectedCallback.(itemIds[resultView.currentItem.index].libId, resultView.currentItem.strings);
	};

	lay = VLayout (
		HLayout (
			StaticText.new.string_("Search:"),
			searchBox,
		),
		[resultView, stretch:3],
		[HLayout (
			typeView,
			categoryView,
			tagView,
		), stretch:1]
	);
	lay;


});

WindowDef(\GlobalLibrary_select, { arg def, defaultValue, okHook, categories, doubleClickHook;
	var selected;
	//categories = [ \AudioBuffer ];
	if(categories.isKindOf(Symbol)) {
		categories = [categories]
	};
	VLayout (
		WindowDef(\GlobalLibrary).asView(nil, { arg val, str, double; 
			selected = val;
			//[selected, val, str, double].debug("GlobalLibrary_select");
			if(double == \double) {
				var fun = doubleClickHook ?? { okHook };
				fun = okHook;
				fun.(selected);
			};
		}, categories, defaultValue),
		HLayout (
			BasicButton.new.string_("Ok").action_({
				okHook.(selected);
				def.closeWindow;
			}),
			BasicButton.new.string_("Cancel").action_({
				def.closeWindow;
			}),
		)
	);
});

)

// END

ProtoDef(\GlobalLibrary).edit
