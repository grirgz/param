
(
TrackTemplateDef(\PlayerTrackPart, (
	// if modeTrigger is true, GUI does not track playing state
	// not implemented here, see mixerPreset
	// should emit changed(\PlayerWrapper, \stopped) in play method to prevent PlayerWrapper from
	// believing something is currently playing
	modeTrigger: false, 

	new: { arg self, parent, index;
		self = ProtoClass((parent: self));

		self.parentTrack = {parent};
		self.index = index.asInteger;
		self[\storeOn] = { arg self, str; str << self.refCompileString };
		self;
	},

	label: { arg self; 
		"%: %".format(self.index, if(self.proxy.notNil, { self.proxy.label }, { "-" }))
	},
	label_: { arg self, val;
		if(val == "") {
			self.removeAt(\label)
		} {
			self[\label] = val;
		};
	},
	fullLabel: { arg self;
		// TODO
		//"s%: % %".format(self.index.asInteger, self.parentTrack.label, self.label)
		self.label;
	},

	defaultSource: nil,

	trackTemplate: { arg self;
		self.parent;
	},

	trackTemplate_: { arg self, val;
		self[\parent] = val;
		self.hasTrackTemplate = true;
		self.updateChildren;
	},

	makeListener: { arg self, fun;
		var controller;
		var listenfun = { arg target ... args;
			fun.(self, *args);
		};
		//this.target.debug("makeListener");
		controller = SimpleController(self)
			.put(\play, listenfun)
			.put(\stop, listenfun)
			.put(\userPlayed, listenfun)
			.put(\userStopped, listenfun)
			.put(\playing, listenfun)
			.put(\stopped, listenfun)
			.put(\PlayerWrapper, listenfun)
		;
		self.controller = controller;
		controller
	},

	asPlayerEvent: { arg self;
		PlayerEvent((
			midinote: self.parentIndex,
			receiver: "{ % }".format(self.refCompileString).interpret
		))
	},

	isEmpty: { arg self;
		self.proxy.isNil or: { self.proxy.target == nil };
	},

	clearTrack: { arg self;
		self.proxy = nil;
	},

	source_: { arg self, val;
		self.proxy = PlayerWrapper(val);
		if(self.mixer.notNil and: { self.enableAutoSetOutbus == true }) {
			self.proxy.outBus = self.mixer.inbus;
		}
	},

	source: { arg self;
		self.proxy.target;
	},

	refCompileString: { arg self;
		if(self.parentTrack.notNil) {
			"%.childAt(%)".format(self.parentTrack.refCompileString, self.index);
		} {
			"TrackDef(%)".format(self.trackKey)
		}
	},

	proxy: { arg self;
		if(self[\obj_proxy].isNil) {
			self[\obj_proxy] = PlayerWrapper(self.defaultSource);
		};
		self[\obj_proxy];
	},

	asParamGroup: { arg self;
		self.source.asParamGroup;
	},

	edit: { arg self;
		var target = self.source;
		if(target.notNil) {

			if(target.getHalo(\edit).notNil) {
				target.getHalo(\edit).value(target);
			} {
				if(target.isKindOf(Pdef)) {
					WindowDef(\PdefEditor).front(target)
				} {
					if(target.isKindOf(Ndef)) {
						WindowDef(\NdefEditor).front(target)
					} {
						target.edit
					}
				};
			};
		} {
			self.selectSourceDialog;
		};
		//WindowDef(\playerwrapper, {
			//var track = self;
			//VLayout(
				//PlayerWrapper(track).asView,
				//ParamGroupLayout.two_panes(track.asParamGroup)
			//)
		//}).front;
	},

	selectSourceDialog: { arg self, callback;
		var source;
		var default;
		if(self.source.isKindOf(PlayerWrapper)) {
			source = self.source.target;
		} {
			source = self.source;
		};
	   	default = ProtoDef(\GlobalLibrary).targetToLibId(source);
		WindowDef(\GlobalLibrary_select).front(default, { arg player;
			var val = player.last.value;
			//~child = child;
			self.source = val;
			callback.value(player)
		}, [\Player])
	},

	enableOnePlayerPerTrack: { arg self;
		if(self.parentTrack.notNil and: { self.parentTrack.parentTrack.notNil }) {
			self.parentTrack.parentTrack.enableOnePlayerPerTrack
		} {
			true
		};
	},

	stopInternal: { arg self;
		// this is called when another child start playing
		// can be overridden when parentTrack.proxy should not be stopped, like in FileCodeTrack
		if(self.enableOnePlayerPerTrack != false) {
			self.stop;
		};
	},

	play: { arg self;
		var stopchild;
		if(self.isEmpty == true) {
			if(self.recordArmed == true) {
				self.startRecordingNewScore;
			} {
				self.parentStop;
			}
		} {
			//"xx1".debug;
			if(self.parentActiveIndex != self.index) {
				stopchild = self.parentActiveChild;
			};
			//"xx2".debug;
			self.parentActiveIndex = self.index;
			//"xx3".debug;
			if(stopchild.notNil) { stopchild.stopInternal }; // changed message but arrive after deselect
			//"xx4".debug;
			self.proxy.play;
			//"xx5".debug;
			self.playHook;
			//"xx6".debug;
			self.parentChildPlayed;
			//"xx9".debug;
			self.changed(\play, self.quant);
			//"xx10".debug;
			self.changed(\PlayerWrapper, \userPlayed); // the view put directly the ProtoDef so need to simulate PlayerWrapper
			//"xx11".debug;
			self.proxy.doWithQuant {
				self.changed(\PlayerWrapper, \playing); // the view put directly the ProtoDef so need to simulate PlayerWrapper
				self.parentChildPlayed({ arg me;
					me.changed(\PlayerWrapper, \playing); 
				});

			};
			//"xx12".debug;
			// it's not a good design this mean i should do it on every ProtoDef!
		}
	},

	resetPlay: { arg self;
		var stopchild;
		if(self.isEmpty == true) {
			self.parentStop;
		} {
			if(self.parentActiveIndex != self.index) {
				stopchild = self.parentActiveChild;
			};
			self.parentActiveIndex = self.index;
			if(stopchild.notNil) { stopchild.stop }; // changed message but arrive after deselect
			// TODO: make it in PlayerWrapper
			if(self.proxy.target.isKindOf(EventPatternProxy)) {
				Log(\Param).debug("playerwrapper track: reset play");
				self.proxy.target.reset;
				self.proxy.play;
			} {
				self.proxy.play;
			};
			self.resetPlayHook;
			self.parentChildPlayed;
			self.changed(\play, self.quant);
			self.changed(\PlayerWrapper, \playing); 
		}
		
	},

	playNow: { arg self;
		var stopchild;
		if(self.isEmpty == true) {
			self.parentStop;
		} {
			self.parentActiveIndex = self.index;
			self.proxy.playNow;
			self.playNowHook;
			self.parentChildPlayed;
			self.changed(\play, self.quant);
			self.changed(\PlayerWrapper, \playing); 
		}
	},

	stop: { arg self;
		if(self.isEmpty.not) {
			self.proxy.stop;
			self.stopHook;
			if(self.recordArmed == true) {
				self.stopRecording;
			};
			self.parentChildStopped;
			self.changed(\stop, self.quant);
			self.changed(\PlayerWrapper, \userStopped); 
			self.proxy.doWithQuant {
				self.changed(\PlayerWrapper, \stopped); 
				self.parentChildStopped({ arg me; me.changed(\PlayerWrapper, \stopped) });
			}
		} 
	},

	stopNow: { arg self;
		self.proxy.stopNow;
		self.stopNowHook;
		self.parentChildStopped;
		self.changed(\stop, self.quant);
		self.changed(\PlayerWrapper, \stopped); 
	},

	quant: { arg self;
		self.proxy.quant;
	},

	quant_: { arg self, val;
		self.proxy.quant = val;
	},

	isPlaying: { arg self;
		(self.proxy.notNil and: { self.proxy.isPlaying == true }) and: {
			self.mixer.isNil or: { self.mixer.isPlaying == true }
		};
	},

	isActive: { arg self;
		if(self.parentTrack.notNil) {
			self.parentTrack.activeIndex == self.index;
		} {
			true
		};
	},

	mixer: { arg self;
		if(self.parentTrack.notNil) {
			self.parentTrack.mixer;
		} {
			nil
		}
	},

	savePresetCompileString: { arg self;
		if(self.isEmpty.not and: { self.proxy.presetCompileStringSavePath.notNil }) {
			self.proxy.savePresetCompileString;
		}
	},

	loadPresetCompileString: { arg self;
		if(self.isEmpty.not and: { self.proxy.presetCompileStringSavePath.notNil }) {
			self.proxy.loadPresetCompileString;
		}
	},

	presetCompileStringSavePath: { arg self;
		if(self.proxy.notNil) {
			self.proxy.presetCompileStringSavePath;
		}
	},

	presetCompileStringSavePath_: { arg track, val;
		var playerwrapper;
		var player_prefix;
		var savepath;
		var player;
		//"--------- try setting save path of %".format(track).debug;
		try {
			playerwrapper = track.proxy;
			player = playerwrapper.target;
			player_prefix = playerwrapper.targetClass;
			if(player.notNil) {

				savepath = val +/+ "%_%.scd".format(player_prefix, player.key);
				savepath.debug("savepath");

				// problem: 
				// - if i put a TrackList in the PlayerWrapper, it will save it inside PlayerGrid, we do not want it
				// - if i test if presetCompileStringSavePath is not, when saving to multiple folders, some players will save at some place and some other at some other place
				//		- i can prevent this if i store the presetCompileStringSavePath in cell instead of target
				// - if i test type, what to do with NoteTimeline ?
				if(playerwrapper.presetCompileStringSavePath.isNil) {
					// prevent setting save path 

					playerwrapper.presetCompileStringSavePath = savepath;
				};
			}
		} { arg ex;
			ex.reportError;
			"Above error means track % can't infer saving path of player from name: %".format(track.key, player).postln;
		}
	
	},

	//// parent methods
	// allow standalone
	parentIndex: { arg self;
		if(self.parentTrack.notNil) {
			self.parentTrack.index;
		} {
			0
		}
	},

	enableAutoSetOutbus: { arg self;
		if(self.parentTrack.notNil) {
			self.parentTrack.enableAutoSetOutbus;
		} {
			false
		}
	},

	recordArmed: { arg self;
		if(self.parentTrack.notNil) {
			self.parentTrack.recordArmed;
		} {
			false
		}
	},

	parentActiveIndex: { arg self;
		if(self.parentTrack.notNil) {
			self.parentTrack.activeIndex;
		} {
			0
		}
	},

	parentActiveIndex_: { arg self, val;
		if(self.parentTrack.notNil) {
			self.parentTrack.activeIndex = val;
		} {
			// NOOP
		}
	},

	parentActiveChild: { arg self;
		if(self.parentTrack.notNil) {
			self.parentTrack.activeChild;
		} {
			self
		}
	},

	parentStop: { arg self;
		if(self.parentTrack.notNil) {
			self.parentTrack.stop;
		} {
			// NOOP
		}
	},

	parentChildPlayed: { arg self, fun;
		if(self.parentTrack.notNil) {
			self.parentTrack.childPlayed(fun);
		} {
			// NOOP
		}
	},

	parentChildStopped: { arg self, fun;
		if(self.parentTrack.notNil) {
			self.parentTrack.childStopped(fun);
		} {
			// NOOP
		}
	},

));
TrackTemplateDef(\PlayerTrack, (
	childClass: { TrackTemplateDef(\playerwrapper) },
	label: { arg self; 
		"%: playertrack".format(self.index)
	},
	fullLabel: { arg self;
		// TODO
		//"s%: % %".format(self.index.asInteger, self.parentTrack.label, self.label)
		self.label;
	},

	new: { arg self, parent, index;
		self = ProtoClass((parent: self));

		self.index = index.asInteger;
		self.parentTrack = {parent};
		self[\storeOn] = { arg self, str; str << self.refCompileString };
		self.selectChild(0);
		self;
	},

	//defaultTrackTemplate: { TrackTemplateDef(\NoteTimelineTrack) },

	trackTemplate: { arg self;
		self.parent;
	},
	trackTemplate_: { arg self, val;
		self[\parent] = val;
		self.hasTrackTemplate = true;
		self.updateChildren;
	},
	hasTrackTemplate: false,

	updateChildren: { arg self;
		//"updateChildren x".debug;
		self.initTrack;
		//"updateChildren2".debug;
		self.children.do { arg child, idx;
			if(child.notNil) {
				if(child.isEmpty) {
					self.children[idx] = nil
				}
			}
		};	
		self.changed(\children);
	},


	activateChild: { arg self, idx;
		self.childAt(idx).play;
	},

	activeChild: { arg self;
		self.childAt(self.activeIndex ? 0);
	},

	makeListener: { arg self, fun;
		if(self.controllerList.notNil) {
			self.controllerList.remove;
		};
		self.controllerList = ProtoClass((
			list: List.new, 
			remove: { arg iself;
				iself.list.do(_.remove)
			})
		);
		self.children.do { arg child;
			var controller = child.makeListener(fun);
			self.controllerList.list.add(controller);
		};
		self.controllerList;
	},
	
	asPlayerEvent: { arg self;
		PlayerEvent((
			midinote: self.index,
			receiver: "{ % }".format(self.activeChild.asCompileString).interpret
		))
	},

	isEmpty: { arg self;
		self.children.every({ arg x; x.isNil or: {x.isEmpty} });
	},

	clearTrack: { arg self;
		self.children.do({ arg child, idx; 
			if(child.notNil) {
				child.clearTrack;
				self.children[idx] = nil;
			}
		});
	},


	source_: { arg self, array;
		array.collect({ arg val, idx;
			self.childAt(idx).source = val;
		})
	},

	source: { arg self;
		self.children.collect({ arg x; x.source });
	},

	savePresetCompileString: { arg self;
		self.existingChildren.do({ arg player;
			player.savePresetCompileString;
		});
	},

	refCompileString: { arg self;
		if(self.parentTrack.notNil) {
			"%.childAt(%)".format(self.parentTrack.refCompileString, self.index);
		} {
			"%(%)".format(self.class.asString, self.key);
		}
	},

	play: { arg self;
		self.activeChild.play;
		self.playHook;
	},

	playNow: { arg self;
		self.activeChild.playNow;
		self.playNowHook;
	},

	stopNow: { arg self;
		self.activeChild.stopNow;
		self.stopNowHook;
	},

	childPlayed: { arg self, fun;
		if(self.mixer.notNil) {
			self.mixer.play;
		};
		self.childPlayedHook(fun);
		if(fun.notNil) {
			fun.value(self);
		} {
			self.changed(\play, self.quant);
		}
	},

	quant: { arg self;
		self.childAt(0).quant;
	},

	quant_: { arg self, val;
		self.children.do({ arg x; x.quant = val });
	},

	isPlaying: { arg self;
		self.activeChild.isPlaying and: {
			self.mixer.isNil or: { self.mixer.isPlaying }
		};
	},

	stop: { arg self;
		self.activeChild.stop;
		self.stopHook;
		if(self.mixer.notNil) {
			//self.mixer.stop; // never stop mixers!
		};
		self.changed(\stop, self.quant);
	},

	childStopped: { arg self, fun;
		self.childStoppedHook(fun);
		if(fun.notNil) {
			fun.value(self);
		} {
			self.changed(\stop, self.quant);
		}
	},


	childAt: { arg self, idx;
		var ret;
		self.children = self.children ?? { SparseArray.new };
		ret = self.children[idx] ?? { 
			self.children[idx] = self.childClass.new(self, idx);
			self.children[idx];
		};
		ret;
	},

	existingChildren: { arg self;
		self.children.select { arg child, idx;
			child.notNil and: { child.isEmpty != true }
		}
	},

	firstChildren: { arg self;
		// return all children until the last existing one
		var ch = self.children.asArray;
		var last = ch.reverse.detectIndex { arg x; x.isEmpty.not };
		if(last.notNil) {
			ch[0..ch.size - last - 1]
		} {
			[]
		};
	},

	// mimic modality
	elAt: { arg self ...args;
		self.childAt(*args)
	},

	gridPosToAbsPos: { arg self, pos;
		pos + self.parentTrack.gridOffset.y;
	},

	childAtOffset: { arg self, idx;
		self.childAt(self.gridPosToAbsPos(idx));
	},

	selectChild: { arg self, idx;
		self.selectedChild = idx; // to be deprecated, should be an object instead an index
		self.selectedIndex = idx;
		self.activeIndex = idx; // this one is the good one

		// childProxy is not defined, can be used to have only one proxy per track
		// FIXME: should i keep it ?
		// source can be called on nil so there is no error
		self.childProxy.source = self.childAt(idx).asPattern; 
	},

	init_mixer_busses: { arg self;
		8.do { arg x;
			self.mixer.proxy.set(\sendbus+++x, BusDef(\sendbus+++x, \audio, self.mixer.numchannels))
		}
	},

	enableMixer: { arg self; // compat
		self.isMixerEnabled;
	},

	isMixerEnabled: { arg self;
		self.parentTrack.isMixerEnabled;
	},

	isMixerEnabled_: { arg self, val;
		self[\isMixerEnabled] = val;
		self.changed(\mixer);
	},

	enableAutoSetOutbus: { arg self;
		self.parentTrack.enableAutoSetOutbus;
	},

	isActive: { arg self;
		self.parentTrack.activeIndex == self.index;
	},

	mixer: { arg self;
		if(self.isMixerEnabled == true) {
			if(self.parentTrack.mixer.notNil) {
				self.parentTrack.mixer.tracks[self.index]
			} {
				if(self.mixer_obj.isNil and: {self.isEmpty.not}) {
					ParamProto.init;
					self.mixer_obj = ProtoClassDef(\TrackMixerDef).new(self.parentTrack.key+++"."+++self.index);
					self.mixer_obj[\soloModeHook] = { arg mself, val;
						// track is soloed, should mute all other tracks
						// val is true or false
						var someSolo;
						someSolo = self.parentTrack.existingChildren.any({ arg child, idx;
							child.mixer.notNil and: { child.mixer.soloMode }
						});
						if(val == false) {
							// if current track is unsoloed, should still solo it if someSolo
							self.mixer.muteBySolo(someSolo);
						};
						self.parentTrack.existingChildren.collect { arg child, idx;
							if(child !== self and: { child.mixer.notNil}) {
								child.mixer.muteBySolo(val);
							}
						};
					};
					self.mixer_obj.define_mixer;
					self.init_mixer_busses;
				};
				self.mixer_obj;
			}
		} {
			nil
		}
	},
));
TrackTemplateDef(\PlayerGrid, (
	label: { arg self; "%".format(self.key) },
	protoClass: \playergrid,
	childClass: { TrackTemplateDef(\playertrack) },
	sectionClass: { TrackTemplateDef(\PlayerGridSection) },
	lineClass: { arg self; self.sectionClass },

	enableAutoSetOutbus: true,
	enableOnePlayerPerTrack: true,
	isMixerEnabled: true,
	//mixer:nil, // TODO: make a container for child mixers here with handy methods

	trackKey: { arg self;
		self.key;
	},

	source_: { arg self, array;
		array.collect({ arg val, idx;
			self.childAt(idx).source = val;
		})
	},

	initProto: { arg self;
		TagSpecDef(\playergrid).addUnique(self.key -> self)
	},

	isActive: true, // compat when grid used as a cell in another grid

	gridOffset: { arg self;
		if(self.obj_gridOffset.isNil) {
			self.obj_gridOffset = Point(0,0);
		};
		self.obj_gridOffset
	},

	gridOffset_: { arg self, pos;
		//[pos, Point(pos.x.clip(0,inf), pos.y.clip(0,inf))].debug("expliquez moi comment c'est possible!");
		self.obj_gridOffset = Point(pos.x.clip(0,inf), pos.y.clip(0,inf));
		self.changed(\gridOffset);
	},

	makeListener: { arg self, fun;
		if(self.controllerList.notNil) {
			self.controllerList.remove;
		};
		self.controllerList = ProtoClass((
			list: List.new, 
			remove: { arg iself;
				iself.list.do(_.remove)
			})
		);
		self.children.do { arg child;
			var controller = child.makeListener(fun);
			self.controllerList.list.add(controller);
		};
		self.controllerList;
	},

	addResource: { arg self ...args;
		if(self.presetCompileStringSavePath.notNil) {
			args.do { arg item, idx;
				PlayerWrapper(item).presetCompileStringSavePath = self.presetCompileStringSavePath;
			};
		};
		self.resources = IdentitySet.newFrom(self.resources ++ args);
	},

	///////// presetCompileString saving system

	savePresetCompileString_old: { arg self;
		self.existingChildren.do({ arg column;
			column.savePresetCompileString;
			column.existingChildren.do({ arg track;
				track.savePresetCompileString;
			})
		});
		self.resources.do { arg res;
			PlayerWrapper(res).savePresetCompileString;
		}; 
	},

	savePresetCompileString: { arg self, path, onDoneAction;
		// currently, this require to give an absolute path to a directory that does not 
		//		exist and that will contains all the save files


		// path should be an absolute folder path
		// FIXME: giving path as argument is broken: self.dataPath is not defined
		var savefile;
		if(path.isNil and: {self.presetCompileStringSavePath.isNil}) {
			"savePresetCompileString: presetCompileStringSavePath is nil, can't save".error;
		} {
			var pathexists;
			//path = path ?? { (self.presetCompileStringSavePath +/+ "TrackDef_%.scd".format(self.key)) };
			path = path ?? { self.presetCompileStringSavePath };
			//path.debug("TrackList.savePresetCompileString");
			savefile = path +/+ "TrackDef_%.scd".format(self.key);
			pathexists = File.exists(path);
			if(pathexists.not or: {self.loadedFromPath == path}) {
				if(pathexists.not) {
					File.mkdir(path);
				};
				self.loadedFromPath = path;
				self.presetCompileStringSavePath = path;
				File.mkdir(self.dataPath);
				PlayerWrapper_Base.savePresetCompileStringHelper(savefile, onDoneAction, self.refCompileString, self.presetCompileString, false);
				self.existingChildren.do({ arg column;
					column.savePresetCompileString;
				});
				self.resources.do { arg res;
					[res, PlayerWrapper(res).presetCompileStringSavePath].debug("trying to save resource");
					PlayerWrapper(res).savePresetCompileString;
				}; 
				Log(\Param).info("TrackList % saved.", self.key);
			} {
				"savePresetCompileString: can't save on existing project. (loadedFromPath is different)".error;
			}
		}
	},

	presetCompileString: { arg self;
		"%\n".format([
			"TrackDef(%, %);".format(self.key.asCompileString, "TrackTemplateDef(\\PlayerGrid)"),
			"%.presetCompileStringSavePath = FileSystemProject.nowExecutingFolder;".format(self.refCompileString),
			"",
			self.existingSections.collect { arg lplayer, idx;
				lplayer.presetCompileString
			}.select(_.notNil).join("\n"),
			"",
			"%.source = %;".format(self.refCompileString, self.source.asCompileString),
			if(self.resources.notNil and: { self.resources.size > 0 }) {
				"%.addResource(*%);".format(self.refCompileString, self.resources.asCompileString)
			},
			"",
			"\n%.loadPresetCompileString;".format(self.refCompileString),
		].select(_.notNil).join("\n"))
	},

	dataPath: { arg self;
		if(self.presetCompileStringSavePath.notNil) {
			self.presetCompileStringSavePath +/+ "TrackDef_%/".format(self.key);
		}
	},

	loadPresetCompileString: { arg self;
		if(self.presetCompileStringSavePath.notNil) {
			self.resources.do { arg res;
				PlayerWrapper(res).loadPresetCompileString;
			}; 
			PathName(self.dataPath).files.do { arg file, idx;
				FileSystemProject.load(file.fullPath);
			};
			self.children.do({ arg column;
				column.children.do({ arg track;
					track.loadPresetCompileString;
				})
			});
		};
		self; // should return self for save file to work
	},

	presetCompileStringSavePath_: { arg self, val;
		// this is a directory since it need several files to save/load
		self[\presetCompileStringSavePath] = val;
		// NOTE: maybe retrieve it from parent in children instead of setting it in parent to all children
		self.children.do({ arg column;
			column.children.do({ arg track;
				track.presetCompileStringSavePath = self.dataPath;
			})
		});
		"===== setting path of resources".debug;
		self.resources.do { arg track;
			PlayerWrapper(track).presetCompileStringSavePath = val +/+ "%_%.scd".format(PlayerWrapper(track).targetClass, track.key);
		};
	},

	savePresetCompileStringDialog: { arg self, path, action, force_dialog=false;
		PlayerWrapper(self).savePresetCompileStringDialog(path, action, force_dialog, \filedialog_tracklist_save);
	},

	loadPresetCompileStringDialog: { arg self;
		//PlayerWrapper(self).loadPresetCompileStringDialog;
	},

	openPresetCompileStringDialog: { arg self;
		WindowDef(\filedialog_tracklist).front(nil, { arg mypath, name;
			[mypath, name].debug("filedialog open ok callback");
			if(name.notNil) {
				var grid = FileSystemProject.load(mypath+/+name);
				if(grid.isKindOf(String)) {
					grid.error;
				} {
					if(grid.isKindOf(TrackDef)) {
						grid.edit;
					} {
						grid.debug("openPresetCompileStringDialog: don't know what to do with");
					};
				};
			};
			//self.presetCompileStringSavePath = mypath +/+ name;
			//this.loadPresetCompileString(mypath +/+ name, action);
		})
		
	},


	/////////

	source: { arg self;
		self.children.collect({ arg x; x.source });
	},

	proxy: { arg self;
		PlayerWrapperGroup(self.children);
	},

	children: { arg self;
		self[\obj_children] ?? { 
			self[\obj_children] = SparseArray.new;
			self[\obj_children];
		};
	},

	existingChildren: { arg self;
		self.children.select { arg child, idx;
			child.notNil and: { child.isEmpty.not }
		}
	},

	firstChildren: { arg self;
		// return all children until the last existing one
		var ch = self.children.asArray;
		var last = ch.reverse.detectIndex { arg x; x.isEmpty.not };
		if(last.notNil) {
			ch[0..ch.size - last - 1]
		} {
			[]
		};
	},


	childAt: { arg self, idx, idx2;
		var ret;
		if(idx.isSequenceableCollection) {
			idx.collect({ arg x; self.childAt(x) })
		} {
			if(idx.isKindOf(Point)) {
				idx2 = idx.y;
				idx = idx.x;
			};
			ret = self.children[idx] ?? { 
				self.children[idx] = self.childClass.new(self, idx);
				self.children[idx];
			};
			if(idx2.notNil) {
				ret.childAt(idx2)
			} {
				ret;
			}
		}
	},

	// mimic modality
	elAt: { arg self ...args;
		self.childAt(*args)
	},


	gridPosToAbsPos: { arg self, pos;
		pos + self.gridOffset;
	},

	childAtOffset: { arg self, pos;
		if(pos.isKindOf(Point)) {
			self.childAt(self.gridPosToAbsPos(pos));
		} {
			self.childAt(self.gridPosToAbsPos(Point(pos,0)).x);
		}
	},

	play: { arg self;
		self.children.do(_.play);
	},

	stop: { arg self;
		self.children.do(_.stop);
	},

	isPlaying: { arg self;
		self.children.any({ arg x; x.notNil and: {x.isPlaying} });
	},

	quant: { arg self;
		self.childAt(0).quant;
	},

	quant_: { arg self, val;
		// disable for the moment
		// when PlayerEvent set quant to 0 just before playing, all various quant are lost
		//self.children.do({ arg x; x.quant = val });
	},

	edit: { arg self;
		var wkey = \Grid_ +++ self.key;
		var editor;
		if(WindowDef(\TrackGrid).notNil) {
			editor = \TrackGrid;
		} {
			editor = \playergrid;
		};
		if(WindowDef(wkey).isNil) {
			WindowDef(wkey, WindowDef(editor)).front(self);
			{
				WindowDef(wkey).window.bounds =  Rect(144, 217, 1090, 438);
			}.defer(1)
		} {
			WindowDef(wkey, WindowDef(editor)).front(self);
		}
	},

	refCompileString: { arg self;
		self.asCompileString;
	},

	activate: { arg self;
		~launchpad_playergrid.new(self).activate
	},

	linePlayerAt: { arg self, idx;
		self.sectionAt(idx);
	},

	existingSections: { arg self;
		self.linePlayers.collect { arg item, idx;
			if(item.notNil and: { item.isEmpty.not }) {
				item;
			};
		}.select(_.notNil);
	},

	linePlayers: { arg self;
		self.sections;
	},

	sectionAt: { arg self, idx;
		if(self.linePlayers[idx].isNil) {
			self.linePlayers[idx] = self.sectionClass.new(self, idx);
		}; 
		self.linePlayers[idx];
	},

	sections: { arg self;
		self[\obj_sections] ?? { 
			self[\obj_sections] = SparseArray.new;
			self[\obj_sections];
		};
	},

	sectionAtOffset: { arg self, idx;
		self.sectionAt(idx + self.gridOffset.y);
	},

	linePlayerAtOffset: { arg self, idx;
		self.linePlayerAt(idx + self.gridOffset.y);
	},

	initFromPresetDir: { arg self, path, force=false;
		if(self.presetCompileStringSavePath.isNil or: { force == true }) {
			self.presetCompileStringSavePath = path;
			self.loadPresetCompileString;
		} {
			"Load Aborted! %.presetCompileStringSavePath is already defined, might override current presets".format(self).postln;
		}
	},


	enableMixer: { arg self; // compat
		self.isMixerEnabled;
	},

	selectChild: { arg self, idx;
		self.activeIndex = idx; // this one is the good one
	},

	activeIndex_: { arg self, val;
		self[\activeIndex] = val;
		self.changed(\activeIndex);
	},

	isEmpty: { arg self;
		// if any children is existing, return false
		self.children.any({ arg x; x.notNil and: {x.isEmpty.not } }).not
	},


	//// controller

	controller_: { arg self, con;
		self[\controller] = con;
		con.startControl(self);
	},

));

TrackTemplateDef(\PlayerGridSection, (
	new: { arg self, parent, idx; 
		self = ProtoClass((parent: self));
	
		
		self.parentTrack = { parent };
		self.index = idx.asInteger; // line index
	
		self;
	},
	label: { arg self;
		"sec %".format(self.index.asInteger, self.parentTrack.label, self.index.asInteger)
	},
	fullLabel: { arg self;
		"s%: % %".format(self.index.asInteger, self.parentTrack.label, self.label)
	},

	trackKey: { arg self;
		"%.sec%".format(self.parentTrack.trackKey, self.index).asSymbol;
	},

	isLinePlayer: true,

	quant: { arg self;
		Pdef.defaultQuant;
	},

	play: { arg self;
		["play"].postln;
		self.children.do({ arg x, ii; 
			Log(\Param).debug("lineplayer play % %", ii, x);
			x.play;
		});
		self.changed(\play, self.quant);
		self.changed(\PlayerWrapper, \userPlayed); // the view put directly the ProtoDef so need to simulate PlayerWrapper
		PlayerWrapper.doWithQuant(self.quant, {
			self.changed(\PlayerWrapper, \playing); // the view put directly the ProtoDef so need to simulate PlayerWrapper
		});
		//self.play;
	},

	playNow: { arg self;
		["play now"].postln;
		self.children.do({ arg x, ii; 
			Log(\Param).debug("lineplayer playNow % %", ii, x);
			x.playNow;
		});
		self.changed(\PlayerWrapper, \playing); // the view put directly the ProtoDef so need to simulate PlayerWrapper
	},

	stop: { arg self;
		["stop"].postln;
		self.children.do({ arg x, ii; 
			Log(\Param).debug("lineplayer stop % %", ii, x);
			x.stop;
		});
		self.changed(\stop);
		self.changed(\PlayerWrapper, \userStopped); // the view put directly the ProtoDef so need to simulate PlayerWrapper
		PlayerWrapper.doWithQuant(self.quant, {
			self.changed(\PlayerWrapper, \stopped); // the view put directly the ProtoDef so need to simulate PlayerWrapper
		});
		//parent.stop;
	},

	stopNow: { arg self;
		["stop now"].postln;
		self.children.do({ arg x, ii; 
			Log(\Param).debug("lineplayer stopNow % %", ii, x);
			x.stopNow;
		});
		self.changed(\stop);
		self.changed(\PlayerWrapper, \stopped); // the view put directly the ProtoDef so need to simulate PlayerWrapper
	},

	trackingMethod: \any,

	existingChildren: { arg self;
		self.children.select { arg child, idx;
			child.notNil and: { child.isEmpty.not }
		}
	},

	isEmpty: { arg self;
		self.existingChildren.size == 0
	},

	isPlaying: { arg self;
		// should be:
		// - at least one cell is playing
		//		- allow to stop the cells playing
		// or
		// - every cell that is not empty is playing
		//		- allow to play the cells not playing
		if(self.trackingMethod == \any) {
			// any
			self.children.any({ arg child, idx;
				child.isPlaying;
			})
		} {
			// every
			self.children.every({ arg child, idx;
				child.isNil or: {child.isEmpty or: child.isPlaying};
			})
		}

		//["lineplayer: isPlaying",parent.isPlaying, self.children.every({ arg x; x.activeIndex == idx })].postln;
		//self.parentTrack.isPlaying and: {
			//var atLeastOneIsPlaying = false;
			//self.children.every({ arg x, ii; 
				////["child:", x,ii, x.activeIndex, idx].postln;
				//x.notNil and: {x.parentTrack.isEmpty or: {
					//if(x.parentTrack.activeIndex == self.index) {
						//atLeastOneIsPlaying = true;
						//true;
					//} {
						//false;
					//}
				//}}
			//}) and: { atLeastOneIsPlaying };
		//};
	},

	asPatternAbs: { arg self, startPosition, endPosition, repeatCount;
		var relStartTime, totalDur;
		var eventlist = self.eventList;
		// currentlty, startPosition and endPosition are in absolute time in the GUI, but will be relative to start event in futur
		// this code translate absolute in relative (to start event)

		// this is the time between first event absTime and asked start time
		// because silence before first event can't be counted in Pembed
		// if startPosition is before first event, relStartTime is negative, not handled by Pembed for the moment
		// relStartTime need to be relative to the first event, because Pembed only see the relative time to the first event and doesn't read \absTime
		
		relStartTime = {
			var sp = startPosition.value;
			if(sp.isNil) {
				self.eventList.relStartTime; // start time relative to first event
			} {
				var xx;
				xx = ( sp - self.eventList.firstTime ).clip(0,inf); // now relStartTime is independent of start event
				self.eventList.firstTime max: xx; // now relStartTime is always after first event
			};

		};

		// total dur is absEnd - absStart (eventlist.totalDur is already that)
		totalDur = {
			var absStart = relStartTime.value + self.eventList.firstTime;
			var ep = endPosition.value;
			var xx;
			if(ep.notNil and:{ep - absStart < 0.01}) { // prevent too short cursor loop
				ep = eventlist.endTime;
			};
			//xx = if(ep.isNil) {
				////totalDur = self.eventloop.list.totalDur - (startPosition - eventlist.startTime ).clip(0,inf);
				//self.eventlist.totalDur;
			//} {
				////totalDur = endPosition - (startPosition ? eventlist.startTime);
				//(( ep ? eventlist.endTime ) - absStart);
			//};
			xx = (( ep ? eventlist.endTime ) - absStart);
			// prevent empty (infinite loop)
			if(xx < 0.01) {
				Log(\Param).debug("timeline.asPattern: loop too short, setting it to 2: xx", xx);
				2;
			} {
				xx
			}
		};

		//[startPosition, endPosition, relStartTime, totalDur].collect(_.value).debug("asPatternAbs: start, end, relstart, totdur");
		self.asPattern(relStartTime, totalDur, repeatCount);
	},

	bypass_pattern: { arg self, pat;
		Pcollect({ arg ev;
			//"entering pcollect".debug;
			ev = ev.copy;
			if(self.tracks_bypassed.notNil and: {self.tracks_bypassed.includes(ev.use({ev.midinote}))}) {
				ev[\type] = \rest;
				ev[\eventType] = nil;
				ev[\nodeType] = nil;
			};
			//"ending pcollect".debug;
			ev;
		}, pat)	
	},

	makeEventList: { arg self, totalDur = 1;
		var el = TimelineEventList.new;
		el.start(0);
		self.children.select(_.isEmpty.not).do({ arg child, idx; 
			var ev = child.asPatternEvent;
			ev[\absTime] = 0;
			ev[\sustain] = totalDur;
			el.addEvent(ev)
		});
		el.finish(totalDur);
		el;
	},

	repeatCount: inf,

	refCompileString: { arg self;
		"TrackDef(%).sectionAt(%)".format(self.parentTrack.key.asCompileString, self.index.asInteger.asCompileString);
	},

	presetCompileString: { arg self;
		if(self[\label].isKindOf(Function).not) {
			"%.label = %;".format(self.refCompileString, self.label.asCompileString)
		} {
			nil
		}
	},

	asPattern: { arg self, relStartTime, totalDur=1000, repeatCount;
		var pat;
		pat = Plazy({
			var eventlist = self.makeEventList(totalDur);
			var xrelStartTime, xtotalDur;

			xrelStartTime = relStartTime.value ? eventlist.relStartTime;
			xtotalDur = totalDur.value ? eventlist.totalDur;

			//[xrelStartTime, xtotalDur].debug("clip xasPattern: plazy: relstart, totdur");
			if(xtotalDur <= 0) {
				"ERROR: totalDur is negative or zero".postln;
				xtotalDur = 2
			};

			eventlist.changed(\cursor, \play); // FIXME: should be an event
			Pfindur(xtotalDur, Pembed(
				self.bypass_pattern( Pn(eventlist.clone, 1) ),
				xrelStartTime
			));
		}).repeat(repeatCount ? self.repeatCount);
		pat;
	},

	childAt: { arg self, idx;
		self.parentTrack.childAt(idx, self.index)
	},

	children: { arg self;
		// take also empty children, to stop the track
		self.parentTrack.children.asArray.collect({ arg x; 
			if(x.notNil) {
				x.childAt(self.index) 
			};
		});
	},

	source: { arg self;
		self.children.collect { arg item, idx;
			if(item.notNil) {
				item.source
			};
		};
	},

	source_: { arg self, val;
		val.do { arg item, idx;
			self.childAt(idx).source = item;
		};
	},
));
);


// backward compat 
TrackTemplateDef(\playerwrapper, TrackTemplateDef(\PlayerTrackPart));
TrackTemplateDef(\playertrack, TrackTemplateDef(\PlayerTrack));
TrackTemplateDef(\playergrid, TrackTemplateDef(\PlayerGrid));
TrackTemplateDef(\lineplayer, TrackTemplateDef(\PlayerGridSection));
TrackDef(\playerwrapper, TrackTemplateDef(\playerwrapper));
TrackDef(\playertrack, TrackTemplateDef(\playertrack));
TrackDef(\playergrid, TrackTemplateDef(\playergrid));

