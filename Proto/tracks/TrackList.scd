

//ParamProto.init;
//FileSystemProject.loadOnce("libdef/tracks.scd");
//FileSystemProject.loadOnce("libdef/drumkit.scd");
//FileSystemProject.loadOnce("libdef/synthplayrec.scd");
//FileSystemProject.loadOnce("libdef/MainProject.scd");

//FileSystemProject.loadOnce("libdef/soundfileview.scd");
//FileSystemProject.loadOnce("libdef/tracklist_buildertrack.scd");
//FileSystemProject.loadOnce("libdef/default_specs.scd");

// cell API
// - proxy: object that is played, generic name for Pdef, Pbindef, Ndef, etc
// - source: the raw content, user set object, which will be wrapped under the hood and put in .proxy
// - score: same as source, but for CustomInstrumentTrackPart, the content according to track instrument
// - pchain
// - base



/// base class

(
// TODO: should be named TrackList
TrackTemplateDef(\TrackList, (
	parent: TrackTemplateDef(\PlayerGrid),

	eventType: \tracklist,
	childClass: { TrackTemplateDef(\CustomTrack) },
	sectionClass: { TrackTemplateDef(\TrackListSection) },
	showTrackHeaders: true,
	showRecorderView: true,
	showMixerView: true,

	showRecorderView_: { arg self, val;
		self[\showRecorderView] = val;
		self.changed(\children);
	},

	showTrackHeaders_: { arg self, val;
		self[\showTrackHeaders] = val;
		self.changed(\children);
	},

	showMixerView_: { arg self, val;
		self[\showMixerView] = val;
		self.changed(\children);
	},

	addTrack: { arg self, trackTemplate, label;
		var lastidx = -1;
		var trackidx;

		// find empty track
		block  { arg break;
			self.children.do { arg child, childidx;
				lastidx = childidx;
				if(self.childAt(childidx).isEmpty) {
					//Log(\Param).debug("found empty track: %", childidx);
					trackidx = childidx;
					break.value;
				}
			};
			//Log(\Param).debug("grid.addTrack: no empty track found, use %", lastidx+1);
			trackidx = lastidx+1;
		};

		// define it
		self.defineTrack(trackidx, trackTemplate, label)
	},

	defineTrack: { arg self, trackidx, trackTemplate, label;
		var trackkey;
		var newtrack = self.childAt(trackidx);
		TagSpecDef(\TrackList).addUnique(self.key -> self);
		newtrack.trackTemplate = trackTemplate;
		if(label.notNil) {
			newtrack.label = label
		};
		self.changed(\tracklist);
		
	},

	removeTrack: { arg self, index;
		self.childAt(index).clearTrack;
		self.children[index] = nil;
		self.changed(\tracklist);
	},

	////// others

	keepSamplesInUse: { arg self;
		self.existingChildren.do { arg track, idx;
			track.keepSamplesInUse;
		};

	},


	////// properties

	trackKey: { arg self;
		self.key;
	},

	selectedPart: 0,
	selectedPart_: { arg self, val;
		if(val != self[\selectedPart]) {
			var isPlaying = self.linePlayerAt(self.selectedPart).isPlaying;
			self[\selectedPart] = val;
			self.changed(\selectedPart);
			self.changed(\tracklist);
			//self.isPlaying.debug("test playing");
			if(isPlaying) {
				//self.selectedPart.debug("test playing yes!");
				self.linePlayerAt(self.selectedPart).play;
			};
		}
	},

	areasizex_: { arg self, val;
		self[\areasizex] = val;
		self.changed(\tracklist);
	},

	////// editor

	edit: { arg self;
		WindowDef(( \TrackList_++self.key ).asSymbol, WindowDef(\TrackList)).front(self);
	},

	editGrid: { arg self;
		WindowDef(( \TrackGrid_++self.key ).asSymbol, WindowDef(\TrackGrid)).front(self);
	},

	////// save/load

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
			path = path ? self.presetCompileStringSavePath;
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
					PlayerWrapper(res).savePresetCompileString;
				}; 
				Log(\Param).info("TrackList % saved.", self.key);
			} {
				"savePresetCompileString: can't save on existing project. (loadedFromPath is different)".error;
			}
		}
	},

	refCompileString: { arg self;
		"TrackDef(%)".format(self.key.asCompileString);
	},

	presetCompileString: { arg self;
		"%\n".format([
			"TrackDef(%, %);".format(self.key.asCompileString, "TrackTemplateDef(\\TrackList)"),
			"TrackDef(%).presetCompileStringSavePath = PathName(thisProcess.nowExecutingPath).pathOnly;".format(self.key.asCompileString),
			"",
			self.linePlayers.collect { arg lplayer, idx;
				lplayer.presetCompileString
			}.select(_.notNil).join("\n"),
			"\nTrackDef(%).loadPresetCompileString;".format(self.key.asCompileString),
		].join("\n"))
	},

	loadPresetCompileString: { arg self;
		// this function should not load the main file because it is the main file that call this function
		if(self.isEmpty) {
			self.resources.do { arg res;
				PlayerWrapper(res).loadPresetCompileString;
			}; 
			// FIXME: find a not hardcoded way
			16.do { arg idx;
				self.childAt(idx).loadPresetCompileString;
			};
			if(self.isEmpty.not) {
				self.loadedFromPath = self.presetCompileStringSavePath;
			};
			self.changed(\tracklist);
		} {
			"Can only load on empty project to prevent overriding".error;
		}
	},

	dataPath: { arg self;
		if(self.presetCompileStringSavePath.notNil) {
			self.presetCompileStringSavePath +/+ "TrackDef_%/".format(self.key);
		}
	},

	presetCompileStringSavePath_: { arg self, val;
		// FIXME: path should either be asbsolute or relative
		// savePresetCompileString does not resolve the path
		self[\presetCompileStringSavePath] = val;
		//val.debug("===== setting resources save path. path:");
		self.resources.do { arg track;
			PlayerWrapper(track).presetCompileStringSavePath = "%%_%.scd".format(val, PlayerWrapper(track).targetClass, track.key);
		};
	},

	loadedFromPath: nil,
));

TrackTemplateDef(\CustomTrack, (
	parent: TrackTemplateDef(\PlayerTrack),
	eventType: \instrumenttrack,
	childClass: { TrackTemplateDef(\CustomTrackPart) },
	makeScoreOnInitTrack: true, // WIP


	new: { arg self, parent, index;
		self = ProtoClass(( parent: self ));

		self.selectChild(0);
		self.index = index.asInteger;
		self.parentTrack = {parent};
		self[\storeOn] = { arg self, str; str << self.refCompileString };
		self;
	},

	initTrack: { arg self;
		if(self.makeScoreOnInitTrack) {
			self.makeScore;
		}
	},


	childAt: { arg self, idx;
		var ret;
		//idx.debug("childAt!!!!x");
		//this.dumpBackTrace;	
		self.children = self.children ?? { SparseArray.new };
		ret = self.children[idx] ?? { 
			self.children[idx] = self.childClass.new(self, idx);
			//idx.debug("childAt init!!!!");
			self.children[idx].initTrack;
			self.children[idx];
		};
		ret;
	},

	clearTrack: { arg self;
		self.clearAllTrackParts;
		//self.trackTemplate = nil;
		if(self != TrackTemplateDef(\CustomTrack)) {
			self.parent = TrackTemplateDef(\CustomTrack);
		};
		self.hasTrackTemplate = false;
	},

	clearAllTrackParts: { arg self;
		self.children.do { arg child;
			if(child.notNil) {
				child.clearPart;
			}
		}
	},


	///// properties

	isEmpty: { arg self;
		self.hasTrackTemplate.not;
		// in tracklist view, even tracks without children should appear
		//and: {
			//self.children.every({ arg x; x.isNil or: {x.isEmpty} });
		//}
	},

	trackKey: { arg self;
		if(self.parentTrack.notNil) {
			"%.%".format( self.parentTrack.key , self.index.asInteger).asSymbol;
		} {
			"%".format(self.key).asSymbol;
		}
	},

	name: { arg self;
		self.trackKey;
	},

	label: { arg self;
		if(self.trackTemplate.key == \CustomTrack) {
			"T%".format(self.index); 
		} {
			self.trackTemplate.key
		}
	},

	outBus_: { arg self, outBus;
		self[\outBus] = outBus;
		self.existingChildren.do({ arg child;
			child.outBus = outBus
		});
		self.changed(\outBus);
	},


	///// editor

	editTrackTemplate: { arg self;
		WindowDef(\CustomInstrumentTrack_TemplateEditor).front(self)
	},
	editInstrument: { arg self;
		var te = self.trackEditor;
		if(te.notNil) {
			te.front(self);
		} {
			Log(\Param).info("No GUI (self.trackEditor) for %".format(self));
		};
	},


	/////// save/load

	presetCompileString: { arg self;
		if(self.isEmpty) {
			nil
		} {
			var str = List[];

			str.add("%.trackTemplate = %;".format(self.refCompileString, self.trackTemplate.asCompileString));
			str.add("%.label = %;".format(self.refCompileString, self.label.asCompileString));
			if(self.mixer.notNil) {
				str.add("\n\n");
				str.add("%.isMixerEnabled = %;".format(self.refCompileString, self.isMixerEnabled.asCompileString));
				str.add(( self.mixer.presetCompileString ?? { "" } ));
			};
			str.add("\n\n" ++ ( self.scorePresetCompileString ?? { "" } ));

			str.join($\n);
		}
	},

	refCompileString: { arg self;
		if(self.parentTrack.notNil) {
			"%.childAt(%)".format(self.parentTrack.refCompileString, self.index.asCompileString);
		} {
			"%(%)".format(self.class.asString, self.key.asCompileString);
		};
	},


	savePresetCompileString: { arg self, path, onDoneAction;
		// path should be a file
		if(self.isEmpty.not) {
			var file;
			//[path, self.presetCompileStringSavePath].debug("InstrumentTrack.savePresetCompileString");
			path = path ? self.presetCompileStringSavePath;
			//path.debug("save path");
			PlayerWrapper_Base.savePresetCompileStringHelper(path, onDoneAction, self.refCompileString, self.presetCompileString, false);

			self.existingChildren.do({ arg track;
				track.savePresetCompileString;
			})
		}
	},

	presetCompileStringSavePath: { arg self;
		if(self.parentTrack.dataPath.notNil) {
			self.parentTrack.dataPath +/+ "TrackDef_%.%.scd".format(self.parentTrack.key, self.index);
		}
	},

	loadPresetCompileString: { arg self;
		FileSystemProject.load(self.presetCompileStringSavePath, silent:true);
		32.do({ arg idx;
			self.childAt(idx).loadPresetCompileString;
		});
	},

	/////// mixer

	isMixerEnabled: false,

	isMixerEnabled_: { arg self, val;
		self[\isMixerEnabled] = val;
		if(val == true) {
			if(self.parentTrack.mixer.notNil) {
				self.parentTrack.mixer.tracks[self.index]
			} {
				if(self.mixer_obj.isNil) {
					var mix;
					ParamProto.init;
					self.mixer_obj = ProtoClassDef(\TrackMixerDef).new(self.parentTrack.key+++"."+++self.index);
					self.mixer_obj[\soloModeHook] = { arg mself, val;
						// track is soloed, should mute all other tracks
						// val is true if current track is soloed, else false
						var someSolo;
						//self.parentTrack.existingChildren.debug("soloModeHook");
						someSolo = self.parentTrack.existingChildren.any({ arg child, idx;
							child.mixer.notNil and: { child.mixer.soloMode }
						});
						if(val == false) {
							// if current track is unsoloed, should still solo it if someSolo
							self.mixer.muteBySolo(someSolo);
						};
						self.parentTrack.existingChildren.do { arg child, idx;
							//[ child, child.mixer, val, child !== self, someSolo ].debug("muteBySolo");
							if(child !== self and: { child.mixer.notNil}) {
								child.mixer.muteBySolo(someSolo);
							}
						};
					};

					self.mixer_obj.define_mixer;

					mix = self.mixer_obj;
					mix.stages[\postdelay] = mix.stages.postfx+1;
					mix.proxy.put(mix.stages.postdelay, \filter -> { arg in, postdelay=0;
						DelayC.ar(in, 0.1, postdelay);
					});

					self.init_mixer_busses;
				};
				self.outBus = self.mixer_obj.inbus;
			}
		} {
			self.outBus = 0;
		};
		self.changed(\mixer);
	},


	mixer: { arg self;
		if(self.isMixerEnabled) {
			self.mixer_obj;
		} {
			nil
		}
	},
));

TrackTemplateDef(\CustomTrackPart, (
	parent: TrackTemplateDef(\playerwrapper),
	eventType: \instrumenttrackpart,

	new: { arg self, parent, index;
		self = ProtoClass(( parent: self ));

		self.parentTrack = {parent};
		self.index = index.asInteger;
		self[\storeOn] = { arg iself, str; str << iself.refCompileString };
		self;
	},

	initTrack: { arg self; 
		self.parentTrack.initChild(self);
	},

	//////// properties

	grid: { arg self;
		self.parentTrack.parentTrack;
	},

	name: { arg self;
		self.trackKey;
	},

	isEmpty: { arg self;
		self.score.isNil and: {
			self.proxy.isNil or: { self.proxy.target == nil };
		}
	},

	trackKey: { arg self;
		if(self.parentTrack.notNil) {
			"%.%".format(self.parentTrack.trackKey, self.index.asInteger).asSymbol;
		} {
			"%".format(self.key).asSymbol;
		};
	},

	outBus: { arg self;
		if(self.proxy.notNil) {
			self.proxy.outBus;
		} {
			self.parentTrack.outBus
		}
	},

	outBus_: { arg self, val;
		if(val.isNil) {
			val = self.parentTrack.outBus ? 0;
		};
		if(self.proxy.notNil) {
			self.proxy.outBus = val;
		}
	},

	//////// editor

	edit: { arg self;
		var target = self.source;
		if(self.editor.notNil) {
			self.editor.front(self);
		} {
			if(target.getHalo(\edit).notNil) {
				target.getHalo(\edit).value(target);
			} {
				if(target.isKindOf(Pdef)) {
					target.edit;
					//WindowDef(\PdefEditor).front(target)
				} {
					if(target.isKindOf(Ndef)) {
						WindowDef(\NdefEditor).front(target)
					} {
						target.edit
					}
				};
			};
		};
	},

	editor: { arg self;
		self.parentTrack.editor
	},

	asPreview: { arg self;
		BasicButton.new.string_("No preview for %".format(self.parentTrack.trackTemplate.key)).action_({
			self.edit;
		})
	},

	asView: { arg self ...args;
		self.parentTrack.editor.asView(self, *args);
	},

	selectSourceDialog: { arg self;
		// in playergrid, this open a dialog to select the player 
		// edit is used to edit the player
		// in tracklist, it is used to edit the instrument
		self.parentTrack.editInstrument;
	},

	//////// score

	makeScore: { arg self;
		Log(\Param).debug("no makeScore method implemented in %", self);
		nil;
	},

	score_: { arg self, val;
		self[\score] = val;
		Log(\Param).debug("setting score to: %", self.score.asCompileString);
		self.grid.changed(\tracklist);
	},

	// source is the player in playerwrapper track, but in tracklist we use score
	source_: { arg self, val;
		self.proxy = PlayerWrapper(val);
		self.outBus = nil; // set to default
	},

	clearTrack: { arg self;
		self.clearPart;
		self.parentTrack.children[self.index] = nil; // erase from parent, force recreate with good template
		self.parentTrack.changed(\children);
		Log(\Param).debug("clearTrack: erased %", self.index);
	},

	clearPart: { arg self;
		self.score = nil;
		self.proxy = nil;
	},


	clone: { arg self, parent, index;
		self = self.copy;

		self.parentTrack = {parent};
		self.index = index.asInteger;
		self.score = self.cloneScore;
		self;
	},

	cloneScore: { arg self;
		var newscore = self.score.copy;
		newscore;
	},

	//////// others

	asPatternEvent: { arg self;
		if(self.score !? self.score.timeline.notNil) {
			self.score.timeline.asPatternEvent;
		} {
			Event.silent(1) // TODO
		}
	},

	//////// save/load

	savePresetCompileString: { arg self, path, onDoneAction;
		if(self.isEmpty.not) {
			var file;
			path = path ? self.presetCompileStringSavePath;
			PlayerWrapper_Base.savePresetCompileStringHelper(path, onDoneAction, self.refCompileString, self.presetCompileString, false);
		}
	},
	
	loadPresetCompileString: { arg self;
		if(self.presetCompileStringSavePath.notNil) {
			FileSystemProject.load(self.presetCompileStringSavePath, true);
		}
	},

	presetCompileString: { arg self;
		// FIXME: need to clarify the difference between source and score
		if(self.isEmpty) {
			nil
		} {
			var str = List[];

			//str.add("%.score = %;".format(self.refCompileString, self.score.asCompileString));
			str.add("\n\n%".format(self.scorePresetCompileString ?? { "" }));
			str.join($\n)
		}
	},
	refCompileString: { arg self;
		if(self.parentTrack.notNil) {
			if( self.parentTrack.parentTrack.notNil ) {
				"%.childAt(%, %)".format(self.parentTrack.parentTrack.refCompileString, self.parentTrack.index.asCompileString, self.index.asCompileString);
			} {
				"%.childAt(%)".format(self.parentTrack.refCompileString, self.index.asCompileString);
			}
		} {
			"%(%)".format(self.class.asString, self.key.asCompileString);
		}
	},

	scorePresetCompileString: { arg self;
		if(self.score.notNil) {

			"%.score = %;".format(self.refCompileString, self.score.presetCompileString)
		} {
			""
		}
	},

	scorePresetCompileStringSavePath: { arg self, scoretype="NoteTimeline";
		var grid = self.grid;
		if(grid.dataPath.notNil) {
			grid.dataPath +/+ "%_%.%.%.scd".format(scoretype, grid.key, self.parentTrack.index, self.index);
		}
	},

	presetCompileStringSavePath: { arg self;
		self.scorePresetCompileStringSavePath("TrackDef");
	},


	presetCompileStringSavePath_: { arg track, val;
		track[\presetCompileStringSavePath] = val;
	},
));

TrackTemplateDef(\TrackListSection, (
	parent: TrackTemplateDef(\lineplayer),
	eventType: \tracklistpart,
	
	new: { arg self, parent, idx; 
		self = ProtoClass((parent: self));
	
		self.parentTrack = { parent };
		self.index = idx; // line index
	
		self;
	},
	trackKey: { arg self;
		"%.part%".format(self.parentTrack.key, self.index.asInteger).asSymbol;
	},
));
);

// backward compat
TrackTemplateDef(\CustomTrackList, TrackTemplateDef(\TrackList));
TrackTemplateDef(\CustomInstrumentTrack, TrackTemplateDef(\CustomTrack));
TrackTemplateDef(\CustomInstrumentTrackPart, TrackTemplateDef(\CustomTrackPart));
TrackTemplateDef(\CustomTrackListPart, TrackTemplateDef(\TrackListSection));
