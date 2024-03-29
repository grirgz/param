
(
ProtoDef(\GlobalLibrary, (
	edit: { arg self;
		WindowDef(\GlobalLibrary).front;
	},

	libIdToTarget: { arg self, libid;
		self.libIdToWrapper(libid).getTarget;
	},

	parent:(
		clipFactories: { arg self;
			self.init_default_clip_factories;
		},
		clipFactories_keys: { arg self;
			self.init_default_clip_factories;
		},
	),

	targetToLibId: { arg self, target;
		var libId;
		var fac;
		if(target.isKindOf(ProtoClass)) {
			if(target.eventType.notNil) {
				if(self.clipFactories.isNil) {
					self.init_default_clip_factories;
				};
				if(self.clipFactories_keys.includes(target.eventType)) {
					fac = self.clipFactories[target.eventType];
					Log(\Param).debug("fac.libIdKey: %", fac.libIdKey);
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
		Log(\Param).debug("libIdToWrapper: libid:%", libid);
		wrapper = self.libIdToFactory(libid).copy;
		Log(\Param).debug("libIdToWrapper: wrapper:%", wrapper);
		wrapper.libId =  libid;
		wrapper;
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
					if(self.clipFactories_keys.includes(model.timeline.eventType)) {
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
						TrackDef, {
							'trackdef'
						},
						ProtoClass, {
							obj.eventType
						},
						{
							//model.debug("event_to_clipFactory_key: player: unknown");
							nil
						}
					);
				};
				var clipkey = branch.(model[\receiver].value);
				var fac;
				if(clipkey.notNil) {
					fac = self.clipFactories[clipkey];
					if(fac.notNil) {
						Log(\Param).debug("GlobalLibrary.eventToLibId: found fac: fackey: %, ev:%, fac:%", clipkey, model, fac);
						Log(\Param).debug("fac.eventToLibId %: %", fac.key, fac[\eventToLibId].asCompileString);
						libId = fac.eventToLibId(model);
						Log(\Param).debug("GlobalLibrary.eventToLibId: found libId: %", libId);
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


	libIdToTrackInstrument: { arg self, libid, name;
		ProtoDef(\trackInstrument).new(libid, name)
	},

	libIdToScore: { arg self, libid, name;
		self.libIdToWrapper(libid).makeScore(name)
	},

	setTrackInstrument: { arg self, target, trackinstr;
		self.libIdToWrapper(self.targetToLibId(target)).setTrackInstrument(trackinstr)
	},

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

			eventToLibId: { arg self, ev;
				var tar;
				Log(\Param).debug("clipFactories.%.eventToLibId: getEventTarget: %", self.key, self[\getEventTarget].asCompileString);
				tar = self.getEventTarget(ev);
				Log(\Param).debug("clipFactories.%.eventToLibId: target: %", self.key, tar);
				self.targetToLibId(tar)
			},

			getEvent: { arg self;
				var ev = ();
				self.redefineClip(self.libIdTargetKey, ev);
				ev;
			},
		);
		
		superself.clipFactories_parent = parentFactory;

		superself.clipFactories.ndef = (
			key: 'ndef',
			libIdKey: 'Ndef',
			label: { arg self; self.key.asString },
			keylist: {
				if(Ndef.all[\localhost].notNil) {
					Ndef.all[\localhost].envir.keys.asArray.sort;
				} {
					[];
				}
			},

			getName: { arg self, ev;
				ev.receiver.value.key;
			},

			className: {
				Ndef
			},

			getEventTarget: { arg self, ev;
				// depends pattern event, player event
				if(ev.receiver.isKindOf(Ndef)) {
					// pattern type
					ev.receiver;
				} {
					// player type
					var tar = ev.receiver.value;
					//tar.key; // PlayerWrapper has key wrapper
					if(tar.isKindOf(PlayerWrapper)) {
						tar = tar.target;
					};
					if(tar.isKindOf(Ndef)) {
						tar;
					} {
						Log(\Param).error("clipFactories.ndef: ERROR: getEventTarget: don't know what type!!!");
						nil;
					}; 
				}
				
			},

			editSelected: { arg self, model;
				var player = model.receiver.value;
				if(player.class == PlayerWrapper) {
					player = player.target;
				};
				if(player.getHalo(\edit).notNil) {
					player.getHalo(\edit).value;
				} {
					player.edit;
				}
			},

			redefineClip: { arg self, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = PlayerWrapper(Ndef(curname));
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey, target;
				(
					timeline: ClipTimeline(scorekey, target)
				)
			},


		).putAll(parentFactory); // TODO: put this at begining else it overwrite

		superself.clipFactories.pdef = (
			key: 'pdef',
			libIdKey: 'Pdef',
			label: { arg self; self.key.asString },
			keylist: {
				Pdef.all.keys.asArray.sort; 
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
						Log(\Param).error("clipFactories.pdef: ERROR: getEventTarget: don't know what type!!!");
						nil;
					}; 
				}
			},

			getTarget: { arg self;
				Pdef(self.libIdTargetKey)
			},

			classObject: {
				Pdef
			},

			getEventTargetKey: { arg self, ev; // NOT USED FOR THE MOMENT
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
					Log(\Param).error("clipFactories.pdef: ERROR: edit: don't know what to do with type of %", player)
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
					timeline: NoteTimeline(scorekey, self.getTarget)
				)
			},
		).putAll(parentFactory);

		superself.clipFactories.trackdef = (
			key: 'trackdef',
			libIdKey: 'TrackDef',
			label: { arg self; self.key.asString },
			keylist: {
				TrackDef.all.keys.asArray.sort; 
			},

			getName: { arg self, ev;
				ev.receiver.value.key;
			},

			editSelected: { arg self, model;
				var player = model.receiver.value;
				if(player.class == PlayerWrapper) {
					player = player.target;
				};
				if(player.getHalo(\edit).notNil) {
					player.getHalo(\edit).value;
				} {
					if(player[\edit].notNil) {
						player.edit;
					} {
						player.editor.front;
					}
				}
			},

			redefineClip: { arg self, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = "{ PlayerWrapper(TrackDef(%)) }".format(curname).interpret;
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey, target;
				TrackTimeline(scorekey, target)
			},
		).putAll(parentFactory);

		// generic timeline factory
		tl = (
			key: 'generictimeline',
			label: { arg self; self.key.asString },

			className: { arg self; // deprecated
				self.classObject
			},

			classObject: {
				~noteTimeline
			},

			getEventTarget: { arg self, ev;
				ev.timeline
			},

			keylist: { arg self;
				self.className.all.keys.asArray.sort;
			},

			getName: { arg self, model;
				model.timeline.key;
			},

			editSelected: { arg self, model;
				model.timeline.edit;
			},

			getTarget: { arg self;
				self.classObject.new(self.libIdTargetKey)
			},

			setTrackInstrument: { arg self, trackinstr;
				self.getTarget.trackInstrument = trackinstr;
			},

			redefineClip: { arg self, curname, model;
				var tlobj;
				Log(\Param).debug("redefineClip timeline before ev:% %", model, model.class);
				PatternEvent.redefine(model);
				Log(\Param).debug("redefineClip timeline ev:% %", model, model.class);

				tlobj = ( "{ " ++ self.className.new(curname).refCompileString ++ " }" ).interpret;
				model[\timeline] = tlobj;
				//model[\timeline] = { 
				//	me.keys.debug("redefineClip3:me.new!!!!!");
				//	me.className.new(curname)
				//};
				//"redefineClip3".debug;
				//model.changed(\refresh);
				superself.parent_model.changed(\refresh);
				//"redefineClip4".debug;
			},

			makeScore: { arg self, scorekey, target;
				ClipTimeline(scorekey, target)
			},
		).putAll(parentFactory);

		superself.clipFactories_timeline_parent = tl;

		[\noteTimeline, \clipTimeline, \modStepTimeline, \kitTimeline].do { arg key;
			superself.clipFactories[key] = (
				key: key,
				libIdKey: (key.asString[0].toUpper ++ key.asString[1..]).asSymbol,
				parent: tl,
				classObject: "{ topEnvironment[%] }".format(key.asCompileString).interpret,
			);
		};
		[\paramTimeline, \sampleTimeline].do { arg key;
			var uckey = (key.asString[0].toUpper ++ key.asString[1..]);
			superself.clipFactories[key] = (
				key: key,
				libIdKey: uckey.asSymbol,
				parent: tl,
				classObject: "{ % }".format(uckey).interpret,
			);
		};

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

		superself.clipFactories.instrumenttrack = ().putAll(superself.clipFactories_parent).putAll((
			key: 'instrumenttrack',
			libIdKey: 'InstrumentTrack',
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
		));

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
	SynthDescLib.all.values.asList.collect({arg lib; lib.synthDescs.asAssociations}).flat;
});

TagSpecDef(\SynthDef_keys, {
	SynthDescLib.all.values.asList.collect({arg lib; lib.synthDescs.keys.as(Array) }).flat;
});

TagSpecDef(\SynthDef, {
	SynthDescLib.all.values.asList.collect({arg lib; lib.synthDescs.asAssociations}).flat.reject({ arg item;
		item.key.asString.beginsWith("system_")
   
	});
});

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

TagSpecDef(\BusDef, {
	TagSpecDef(\BusDef_object).list
});

TagSpecDef(\BusDef_asMap, {
	[\nil->nil] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k).asMap }) 
});

TagSpecDef(\BusDef_control_asMap, {
	[\nil->nil] ++ BusDef.all.keys.as(Array).sort.select { arg k;
		BusDef(k).rate == \control
	}.collect({ arg k; 
		k -> BusDef(k).asMap 
	}) 
});

TagSpecDef(\BusDef_index, {
	[\0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k).index }) 
});

// harder to get a presetCompileString when the bus is numeric
TagSpecDef(\BusDef_object, {
	[\0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k) }) 
});

TagSpecDef(\BufDef, {
	[\0->0] ++ BufDef.all.keys.as(Array).sort.collect({ arg k; k -> BufDef(k).bufnum }) 
});

TagSpecDef(\BufDef_mono, {
	[\0->0] ++ BufDef.all.keys.as(Array).sort
		.collect({ arg k; k -> BufDef(k) })
		.select({ arg kv; kv.value.numChannels == 1 }) 
		.collect({ arg kv; kv.key -> kv.value.bufnum })
});

TagSpecDef(\BufDef_stereo, {
	[\0->0] ++ BufDef.all.keys.as(Array).sort
		.collect({ arg k; k -> BufDef(k) })
		.select({ arg kv; kv.value.numChannels == 2 }) 
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
		var sizes = tracklist.children.select{|x|x.isEmpty.not}.collect({ arg channel;
			// for each channel, count the number of part
			channel.children.select{|x|x.isEmpty.not}.size; 
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
}).list;

TagSpecDef(\InstrumentTrackPart, {
	TagSpecDef(\TrackList).valueList.collect({ arg tracklist;
		tracklist.children.select{|x|x.isEmpty.not}.collect { arg channel;
			channel.children.select({|x|x.isEmpty.not}).collect({ arg part, partidx;
				// format: (libLabel -> libObj)
				// type is added by TagSpecDef key
				//("%.%.part%".format(tracklist.key, channel.label, part.index) -> part)
				part.trackKey -> part
			}).asArray;
		}
	}).flatten(2);
}).list;

// maybe should rename to TrackInstrument_ClipTimeline
TagSpecDef('TrackInstrument', [
	\ClipTimelineTrack,
	\KitTimelineTrack,
	\NoteTimelineTrack,
	\SampleTimelineTrack,
	\ParamTimelineTrack,
]);


//////////////////////////////////////
////////////////////////// Categories

// FIXME: rename to GlobalLibrary.type
TagSpecDef(\GlobalLibraryType, [
	\BusDef,
	\BufDef,
	\SynthDef,
	\Pdef,
	\Ndef,
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
	\TrackPart,
	\TrackGrid,
	\LineTrack,
	\Track,
	\AllTracks,
	\Clip,
	\Timeline,
	\Editable,
	\Players,
	\AudioBuffer,
	\SynthDef,
]);

//TagSpecDef('GlobalLibrary.type.TrackInstrument', [
	//\ClipTimelineTrackX,
//]);


// FIXME: rename to GlobalLibrary.category.TrackInstrument
TagSpecDef('GlobalLibrary.type.TrackInstrument', [
	\TrackInstrument,
]);

TagSpecDef('GlobalLibrary.type.SynthDef', [
	\SynthDef,
]);


TagSpecDef('GlobalLibrary.type.TrackInstrumentTarget', [
	\SynthDef,
	\Pdef,
	\Ndef,
	\PatKitDef,
	\BufDef,
	\Sample,
]);

TagSpecDef('GlobalLibrary.type.Clip', [
	//\SynthDef, // FIXME: not implemented yet
	//\BufDef, // FIXME: not implemented yet
	\Sample,
	\Pdef,
	\Ndef,
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

TagSpecDef('GlobalLibrary.type.Players', {
	[
		\Pdef,
		\Ndef,
		\NoteTimeline,
		\TrackTimeline,
		\KitTimeline,
		\SampleTimeline,
		\ClipTimeline,
		\ParamTimeline,
		\CustomTrackList,
		\ProtoPlayer,
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
	var updateItems;
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
		def.changed(\results);
	}).focus(true);

	updateItems = {
		var view = resultView;
		var oldTagSelection;
		var model = def;

		var superitem;
		var selectedTypes = typeView.selection.collect({ arg idx; typeView.items[idx] });
		var selectedCategories = categoryView.selection.collect({ arg idx; categoryView.items[idx] });
		var filteredTypes;
		if(selectedTypes.size == 0) {
			selectedTypes = MenuSpecDef(\GlobalLibraryType).keyList;
		};
		if(selectedCategories.size == 0) {
			selectedCategories = categoryView.items;
		};
		filteredTypes = selectedTypes;
		if(selectedCategories.size > 0) {
			Log(\Param).debug("selected categories: %", selectedCategories);
			filteredTypes = selectedTypes.asSet.sect(
				selectedCategories.collect({ arg superkey;
					MenuSpecDef("GlobalLibrary.type.%".format(superkey).asSymbol).keyList;
				}).flat.asSet
			).asList;
		};
		view.clear;
		//filteredTypes = [\Sample];

		//[model, view, msg, selectedTypes, selectedCategories, filteredTypes].debug("category change");
		itemIds = filteredTypes.collect({ arg typeitem;
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

	typeView = ListView.new
		.items_(
			TagSpecDef(\GlobalLibraryType).list.collect ({ arg superitem;
				//Log(\Param).debug("superitem %", superitem);
				//TagSpecDef(superitem.value).list.collect { arg item;
				//Log(\Param).debug("item %", item);
				superitem.key;
				//}

			}).flat.asArray
		)
		.selection_([])
		.action_({ arg view;
			//view.selection.debug("cat action");
			def.changed(\results);
		})
		.selectionMode_(\multi)
		.mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			//view.selection.debug("cat mouse up");
			def.changed(\results);
			
		})
	;

	categoryView = ListView.new
		.items_(
			TagSpecDef(\GlobalLibraryCategory).list.collect ({ arg superitem;
				superitem.key;
			}).select({ arg superitem;
				if(categoryWhitelist.notNil) {
					categoryWhitelist.includes(superitem)
				} {
					true
				};
			}).flat.asArray
		)
		.selection_([])
		.action_({ arg view;
			//view.selection.debug("cat action");
			def.changed(\results);
		})
		.selectionMode_(\multi)
		.mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			//view.selection.debug("cat mouse up");
			def.changed(\results);
			
		})
	;

	tagView = ListView.new
		.selection_([])
		.action_({ arg view;
			//view.selection.debug("cat action");
			def.changed(\results);
		})
		.selectionMode_(\multi)
		.mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			//view.selection.debug("cat mouse up");
			def.changed(\results);
			
		})
	;

	resultView = TreeView.new
		.columns_(["Name", "Type", "Tags"])
		.setColumnWidth(0,300)
		.itemPressedAction_({ arg view;
			var itemId = treeViewItemToLibId.(view.currentItem);
			Log(\Param).debug("itemPressedAction_ itemIds:%, idx:%", itemIds.size, view.currentItem.index);
			//Log(\Param).debug("itemPressedAction_ itemId:%, str:%", itemIds[view.currentItem.index], view.currentItem.strings);
			Log(\Param).debug("itemPressedAction_ real itemId:%, str:%", treeViewItemToLibId.(view.currentItem), view.currentItem.strings);
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

	resultView.followChange(def, \results, { arg view, model, msg, arg1;
		updateItems.();
	});
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
		searchBox,
		[resultView, stretch:3],
		[HLayout (
			typeView,
			categoryView,
			tagView,
		), stretch:1]
	);
	lay;


});

WindowDef(\GlobalLibrary_select, { arg def, defaultValue, okHook, categories;
	var selected;
	//categories = [ \AudioBuffer ];
	VLayout (
		WindowDef(\GlobalLibrary).asView(nil, { arg val; 
			selected = val;
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
