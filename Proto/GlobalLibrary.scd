
(
ProtoDef(\GlobalLibrary, (
	edit: { arg self;
		WindowDef(\GlobalLibrary).front;
	},

	libIdToTarget: { arg self, libid;
		self.libIdToWrapper(libid).getTarget;
	},

	libIdToStrings: { arg self, libid;
		Log(\Param).debug("libIdToStrings: %", libid);
		Log(\Param).debug("libIdToStrings: res %", [libid.first.asString, libid.last.key.asString]);
		[libid.first.asString, libid.last.key.asString]
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
		var wrapper = self.libIdToFactory(libid).copy;
		wrapper.libIdTargetKey = libid.last.key;
		wrapper;
	},

	libIdToTrackInstrument: { arg self, libid, name;
		ProtoDef(\trackInstrument).new(libid, name)
	},

	libIdToScore: { arg self, libid, name;
		self.libIdToWrapper(libid).makeScore(name)
	},

	init_default_clip_factories: { arg self;
		var tl;
		self.clipFactories = ();

		self.clipFactories.ndef = (
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

			redefineClip: { arg me, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = PlayerWrapper(Ndef(curname));
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey, target;
				(
					timeline: ClipTimeline(scorekey, target)
				)
			},


		);

		self.clipFactories.pdef = (
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
						Log(\Param).error("clipFactories.pdef: ERROR: getTarget: don't know what type!!!");
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
					"clipFactories.pdef: ERROR: edit: don't know what type!!!".debug;
				}
			},

			redefineClip: { arg me, curname, model;
				PatternEvent.redefine(model);
				model[\pattern] = Pdef(curname);
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey;
				(
					timeline: NoteTimeline(scorekey, self.getTarget)
				)
			},
		);

		self.clipFactories.trackdef = (
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

			redefineClip: { arg me, curname, model;
				PlayerEvent.redefine(model);
				model[\receiver] = "{ PlayerWrapper(TrackDef(%)) }".format(curname).interpret;
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey, target;
				TrackTimeline(scorekey, target)
			},
		);

		// generic timeline factory
		tl = (
			key: 'generictimeline',
			label: { arg self; self.key.asString },

			className: {
				~noteTimeline
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

			redefineClip: { arg me, curname, model;
				var tlobj;
				Log(\Param).debug("redefineClip timeline before ev:% %", model, model.class);
				PatternEvent.redefine(model);
				Log(\Param).debug("redefineClip timeline ev:% %", model, model.class);

				tlobj = ( "{ " ++ me.className.new(curname).refCompileString ++ " }" ).interpret;
				model[\timeline] = tlobj;
				//model[\timeline] = { 
				//	me.keys.debug("redefineClip3:me.new!!!!!");
				//	me.className.new(curname)
				//};
				//"redefineClip3".debug;
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
				//"redefineClip4".debug;
			},

			makeScore: { arg self, scorekey, target;
				ClipTimeline(scorekey, target)
			},
		);
		self.clipFactories_timeline_parent = tl;

		[\noteTimeline, \clipTimeline, \envTimeline, \modStepTimeline, \kitTimeline].do { arg key;
			self.clipFactories[key] = (
				key: key,
				libIdKey: (key.asString[0].toUpper ++ key.asString[1..]).asSymbol,
				parent: tl,
				className: "{ topEnvironment[%] }".format(key.asCompileString).interpret,
			);
		};

		self.clipFactories.sampleTimeline = (
			key: 'sampleTimeline',
			libIdKey: \SampleTimeline,
			parent: tl,
			className: { SampleTimeline.proto },
		);

		self.init_other_factories;

		self.clipFactories_keys = self.clipFactories.keys.asArray.sort;
		
	},

	init_other_factories: { arg self;
		self.clipFactories.patkitdef = (
			key: 'pdef',
			libIdKey: 'PatKitDef',
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
						Log(\Param).error("clipFactories.pdef: ERROR: getTarget: don't know what type!!!");
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
					"clipFactories.pdef: ERROR: edit: don't know what type!!!".debug;
				}
			},

			redefineClip: { arg me, curname, model;
				PatternEvent.redefine(model);
				model[\pattern] = Pdef(curname);
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey;
				(
					timeline: KitTimeline(scorekey, self.getTarget)
				)
			},
		);

		self.clipFactories.patkitdef = (
			key: 'pdef',
			libIdKey: 'PatKitDef',
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
						Log(\Param).error("clipFactories.pdef: ERROR: getTarget: don't know what type!!!");
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
					"clipFactories.pdef: ERROR: edit: don't know what type!!!".debug;
				}
			},

			redefineClip: { arg me, curname, model;
				PatternEvent.redefine(model);
				model[\pattern] = Pdef(curname);
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
			},

			makeScore: { arg self, scorekey;
				(
					timeline: KitTimeline(scorekey, self.getTarget)
				)
			},
		);
		
	},
));

//TagSpecDef(\SynthDef).clear; // to debug;

TagSpecDef(\SynthDef_all, {
	SynthDescLib.all.values.asList.collect({arg lib; lib.synthDescs.asAssociations}).flat;
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
	Pdef.all.asAssociations;
});

TagSpecDef(\SampleTimeline, {
	SampleTimeline.proto.all.asAssociations;
});

TagSpecDef(\PatKitDef, {
	PatKitDef.all.asAssociations;
});

TagSpecDef(\BusDef, {
	[\0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k).index }) 
});

TagSpecDef(\BufDef, {
	[\0->0] ++ BufDef.all.keys.as(Array).sort.collect({ arg k; k -> BufDef(k).bufnum }) 
});


TagSpecDef(\BusDefDefault, {
	[\default -> nil] ++ TagSpecDef(\BusDef).list
});

TagSpecDef(\TrackListPart, {
	var tracklists = TagSpecDef(\TrackList).valueList;
	tracklists.collect({ arg tracklist;
		var sizes = tracklist.children.select{|x|x.isEmpty.not}.collect({ arg channel;
			channel.children.select{|x|x.isEmpty.not}.size;
		});
		var channelIdxWithMostParts = sizes.maxIndex;
		if(size) // TODO: finish this
		//(0..sizes[channelIdxWithMostParts]).collect({ arg idx;
			//"%.part%".format(tracklist.key, idx) -> ( tracklist -> idx );
		//})
	}).flatten
}).list;

TagSpecDef(\TrackListChannelPart, {
	TagSpecDef(\TrackList).valueList.collect({ arg tracklist;
		tracklist.children.select{|x|x.isEmpty.not}.collect { arg channel;
			channel.children.select({|x|x.isEmpty.not}).collect({ arg part, partidx;
				channel.label -> (channel -> partidx)
			}).asArray;
		}
	}).flatten(2);
}).list;

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
	\PatKitDef,
	\Sample,
	\TrackListPart,
	\TrackListChannelPart,
]);

TagSpecDef(\GlobalLibraryCategory, [
	\TrackInstrument,
	\Effect,
	\Score,
	\Preset,
]);

TagSpecDef('GlobalLibrary.type.TrackInstrument', [
	\SynthDef,
	\Pdef,
	\Ndef,
	\PatKitDef,
	\BufDef,
	\Sample,
]);

TagSpecDef('GlobalLibrary.type.Clip', [
	\SynthDef,
	\BufDef,
	\Sample,
	\Pdef,
	\Ndef,
	\NoteTimeline,
	\TrackTimeline,
	\KitTimeline,
	\SampleTimeline,
	\ClipTimeline,
]);

TagSpecDef('GlobalLibrary.type.TrackListPart', [
	\TrackListPart,
]);

TagSpecDef('GlobalLibrary.type.TrackListChannelPart', [
	\TrackListChannelPart,
]);

WindowDef(\GlobalLibrary, { arg def, lib, selectedCallback, categoryWhitelist;


	var resultView;
	var searchBox;
	var typeView;
	var categoryView;
	var lay;
	var itemIds = List.new;
	searchBox = TextField.new.action_({
		"action!".debug;
			def.changed(\search);
			def.changed(\category);
	}).keyUpAction_({ arg me, key, modifiers, unicode, keycode;
		[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
		def.changed(\results);
	});

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
			view.selection.debug("cat action");
			def.changed(\results);
		})
		.selectionMode_(\multi)
		.mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			view.selection.debug("cat mouse up");
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
			view.selection.debug("cat action");
			def.changed(\results);
		})
		.selectionMode_(\multi)
		.mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			view.selection.debug("cat mouse up");
			def.changed(\results);
			
		})
	;


	resultView = TreeView.new
		.columns_(["Name", "Type", "Category", "Tags"])
		.followChange(def, \results, { arg view, model, msg, arg1;
			var superitem;
			var selectedTypes = typeView.selection.collect({ arg idx; typeView.items[idx] });
			var selectedCategories = categoryView.selection.collect({ arg idx; categoryView.items[idx] });
			var filteredTypes;
			if(selectedTypes.size == 0) {
				selectedTypes = MenuSpecDef(\GlobalLibraryType).keyList;
			};
			if(categoryWhitelist.notNil and: {categoryWhitelist.size>0}) {
				selectedCategories = categoryWhitelist;
			};
			filteredTypes = selectedTypes;
			if(selectedCategories.size > 0) {
				filteredTypes = selectedTypes.asSet.sect(
					selectedCategories.collect({ arg superkey;
						MenuSpecDef("GlobalLibrary.type.%".format(superkey).asSymbol).keyList;
					}).flat.asSet
				).asList;
			};
			view.clear;
			//filteredTypes = [\Sample];

			[model, view, msg, selectedTypes, selectedCategories, filteredTypes].debug("category change");
			itemIds = filteredTypes.collect({ arg typeitem;
				Log(\Param).debug("typeitem %", typeitem);
				TagSpecDef(typeitem.value).list.collect ({ arg item;
					//Log(\Param).debug("item %", item);
					[typeitem, item];
				})
			}).flatten;
			Log(\Param).debug("itemIds %", itemIds);

			itemIds.collect({ arg id;
				lib.libIdToStrings(id);
			}).select({ arg strings;
				Log(\Param).debug("search strings to add %", strings);
				if(searchBox.string.trim != "") {
					strings.join(" ").toLower.contains(searchBox.string.toLower)
				} {
					true
				}
			}).do { arg strings;
				Log(\Param).debug("strings to add %", strings);
				view.addItem(strings)
			}
		})
		.itemPressedAction_({ arg view;
			selectedCallback.(itemIds[view.currentItem.index], view.currentItem.strings)
		})
	;
	lay = VLayout (
		searchBox,
		[resultView, stretch:3],
		[HLayout (
			typeView,
			categoryView,
			ListView.new,
		), stretch:1]
	);
	lay;


});

)
