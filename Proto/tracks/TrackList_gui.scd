
WindowDef(\TrackGrid, (
	trackCount: 9,
	rowCount: 8,
	masterTrackIndex: 8,
	maxColumnWidth: 200,
	showToolBar: true,
	color_header: Color.gray.lighten(ParamViewToolBox.color_dark, 0.7),

	makeMixerViewList: { arg self;
		
	},

	clipboard: { arg self;
		ParamViewToolBox.clipboard.trackDef
	},
	clipboard_: { arg self, val;
		ParamViewToolBox.clipboard.trackDef = val;
	},
	
	asView: { arg self, me, maintrack;
		var viewgrid;
		var mainview;
		var make_view;
		var playerviews;
		var map_players;
		var mixerviews;
		var specialviews;
		maintrack = maintrack ?? { TrackDef(\main) };

		if(me.key == \customgrid) {
			me.windowName = "customgrid: %".format(maintrack.key);
		};

		self.maintrack = maintrack;
		self.windef = me;
		// commented because at caller level
		//if(me.window.notNil) {
		//	me.window.bounds = Rect(144, 217, 1090, 438);
		//};

		mainview = View.new;
		self.mainview = mainview;
		mainview.onChange(maintrack, \gridOffset, { {map_players.()}.defer });

		playerviews = { { PlayerWrapperGridCellView.new } ! self.rowCount } ! self.trackCount;
		playerviews.last.do { arg item, idx;
			item.color_empty = self.color_header;
		};
		//mixerviews = { WindowDef(\mixerView).asView } ! 9;
		~playerviews = playerviews;

		specialviews = self.trackCount.collect({ arg x;
			View.new.addUniqueMethod(\model_, { arg view, track;
				var recorderView;
				view.removeAll;
				//Log(\Param).debug("specialviews model_: %", track);
				if(track.notNil) {
					recorderView = track.recorderView;
					if(recorderView.notNil) {
						//Log(\Param).debug("specialviews recorderView: %", recorderView);
						view.layout = recorderView;
					}
				}
			}).maxWidth_(self.maxColumnWidth).fixedHeight_(30).background_(Color.gray);
		});

		mixerviews = self.trackCount.collect({ arg x;
			var boxview;
			var delayview;
			var child;
			var mixer;
			var mixerview;
			boxview = View.new;
			boxview.addUniqueMethod(\model_, { arg view, track;
		   
				// TODO: make listen to \mixer changed and update model
				// 
				if(track.notNil) {
					if(track.isKindOf(Volume).not) {
						//x.debug("set trackHeaders model");
						self.trackHeaders[x].model = track;
						//[ self.trackFaders[x], track, track.mixer, x ].debug("set trackFaders model: view, track, mixer, x");
						self.trackFaders[x].model = track.mixer;
						//debug("set mixer follower");
						view.getHalo(\followChangeController) !? _.remove;
						view.followChange(track, \mixer, {

							// when mixer is created, need to assign it
							//[ self.trackFaders[x], track, track.mixer, track.isMixerEnabled, x ].debug("set trackFaders model: view, track, mixer, x");
							self.trackFaders[x].model = track.mixer;
							//self.trackHeaders[x].visible = (track.parentTrack.showTrackHeaders == true);
							//self.trackFaders[x].visible = ( track.isMixerEnabled == true );
						}, false); // if true, show ghost mixer model bug
					} {
						//x.debug("set trackHeaders model volume!!!!!");
						self.trackHeaders[x].model = PlayerWrapper(self.maintrack);
						self.trackFaders[x].model = ProtoClassDef(\TrackMixerDef_volume);
						//self.trackFaders[x].slider.mapParam(Param(track, \volume));
						//self.trackFaders[x].textfield.mapParam(Param(track, \volume));
						//Param(track, \volume).debug("volume");
						self.trackFaders[x].proto.showSends = false;
						self.trackFaders[x].proto.showMuteSolo = false;
						//self.trackFaders[x].model = track;
						
					}
				} {

				}
			});


			boxview.layout = VLayout(
				self.makeTrackHeader(x),
				specialviews[x],
				[self.makeTrackFader(x), stretch:10],
				nil,
			).spacing_(0).margins_(0);

			boxview.maxWidth = self.maxColumnWidth;
			boxview;

		});

		if(me.window.notNil) {
			me.window.name = "playergrid: " ++ maintrack.key;
		};

		map_players = {
			//debug("map players");
			playerviews.do({ arg track, x;
				track.do { arg child, y;
					//[track, x, child, y].debug("set playerview model: track child");
					if(x == 8) {
						child.model = maintrack.linePlayerAt(y+maintrack.gridOffset.y);
					} {
						child.model = maintrack.childAtOffset(Point(x,y));
					}
				};
			});
			specialviews.do({ arg child, x;
				//[child, x].debug("set specialviews model: track idx");
				if(x == 8) {
					child.model = nil
				} {
					child.model = maintrack.childAtOffset(x);
				}
			});
			mixerviews.do({ arg child, x;
				//[child, x].debug("set mixerview model: child");
				if(child.notNil) {
					if(x == 8) {
						child.model = Server.default.volume;
					} {
						child.model = maintrack.childAtOffset(x);
					}
				}
			});

			if(maintrack.showMixerView == true) {
				//Log(\Param).debug("showMixerView enable: %", maintrack.showMixerView);
				mixerviews.do({ arg th; th.visible = true })
			} {
				//Log(\Param).debug("showMixerView disable: %, %", maintrack.showMixerView, mixerviews);
				mixerviews.do({ arg th; th.visible = false })
			};

			if(maintrack.showTrackHeaders == true) {
				self.trackHeaders.do({ arg th; th.visible = true })
			} {
				self.trackHeaders.do({ arg th; th.visible = false })
			};

			if(maintrack.showRecorderView == true) {
				specialviews.do({ arg th; th.visible = true })
			} {
				specialviews.do({ arg th; th.visible = false })
			};

		};

		make_view = {
			mainview.removeAll;
			mainview.layout = VLayout(
				GridLayout.columns(*
					playerviews.collect({ arg track, idx;

						//[track.first.view, track.first.model, track.first.model.parentTrack].debug("make_view: follower");
						track.first.view.followChange(track.first.model.parentTrack, \children, { {map_players.()}.defer });
						track.collect({ arg child;
							var view = child.asView;
							view.button.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
								//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
								if(buttonNumber == 1) {
									if(child.model.isKindOf(PlayerWrapper)) {
										child.model.target.edit;
									} {
										child.model.edit;
									}
								};
							});
							view.layout.margins_(1);
							view.button.fixedWidth_(20);
							//view.labelView.fixedWidth_(49);
							//view.fixedSize_(Point(300,20));
							view.labelView.font_(Font.default.size_(10));
							view.labelView.minWidth_(10);
							//child.model.target.debug("childmodeltarget");
							//if(child.model.target.isEmpty == true) {
							//child.color_deselected = Color.gray;
							//};
							child.selected = false;
							view.labelView.mouseDownAction_({ arg label, x, y, modifiers, buttonNumber, clickCount;
								//"DOWN".debug;
								//view.selected = true
								if(buttonNumber == 1) {
									var model = child.model;
									self.makeOverlayMenu(label, x, y, model);
								};
								if(clickCount == 2) {
									child.model.selectSourceDialog;
								};
							});
							view;
						})
						++ [ 
							[ mixerviews[idx] ],
							//[ specialviews[idx] ],
						]
					})
				).vSpacing_(1).hSpacing_(1),
				nil
			).margins_(0);
			if(self.showToolBar == true) {
				VLayout(
					self.makeToolBar,
					mainview,
				).margins_(5)
			} {
				mainview
			}
		};
		map_players.();
		self.addKeybordBindings;
		make_view.();
	},

	addKeybordBindings: { arg self;
		var anchor = self.windef.windowDo { arg win; win.view } ?? { self.mainview };
		anchor.keyDownAction_({ arg me, key, modifiers, unicode, keycode;
			var matrix;
			var idx, posx, posy;
			var maintrack = self.maintrack;
			//[me, key.asCompileString, modifiers, unicode, keycode].debug("grid keyDownAction");
			matrix = [
				"&" ++ -23.asAscii ++ "\"'(-" ++ -24.asAscii ++ "_" ++ -25.asAscii,
				"azertyuio",
				"qsdfghjkl",
				"wxcvbn,;:",
			].join;
			//~matrix
			//~matrix.indexOf($a);
			idx = matrix.indexOf(key);
			if(idx.notNil) {
				posx = idx%9;
				posy = ( idx/9 ).floor;
				//Log(\Param).debug("grid keydown: idx %, x %, y %", idx, posx, posy);
				if(posx == 8) {
					PlayerWrapper(maintrack.linePlayerAt(posy+maintrack.gridOffset.y)).togglePlay;
				} {
					PlayerWrapper(maintrack.childAtOffset(Point(posx,posy))).togglePlay;
				}
			}
		});
		
	},

	showGeneralOverlayMenu: { arg self, view, x, y;
		Menu(
			MenuAction("Save", {
				self.maintrack.savePresetCompileStringDialog;
			}),
			MenuAction("Load", {
				self.maintrack.loadPresetCompileStringDialog;
			}),
			MenuAction.separator,
			Param(Message(self.maintrack), \showMixerView, ParamBoolSpec()).asMenuAction,
			Param(Message(self.maintrack), \enableAutoSetOutbus, ParamBoolSpec()).asMenuAction,
			Menu(
				* WindowDef(\TrackMixerDef_advanced).proto.displayOptions.collect { arg item, idx;
					var curval;
					if(self.maintrack.displayConfig.isNil) {
						self.maintrack.displayConfig = ();
					};
					curval = self.maintrack.displayConfig.perform(item);
					curval = curval ?? { self.trackFaders.first.proto.perform(item) };
					self.maintrack.displayConfig.perform(item.asSetter, curval.not);
					MenuAction(item, {
						self.trackFaders.do { arg mixerview, idx;
							mixerview.proto.perform(item.asSetter, curval.not)
						};
					}).checked_(curval)
				}
				++ [
					Param(Message(self.maintrack), \showTrackHeaders, ParamBoolSpec()).asMenuAction(),
					Param(Message(self.maintrack), \showRecorderView, ParamBoolSpec()).asMenuAction(),
				]
			).title_("Mixer layout"),
			Param(
				Message(self.maintrack),
				\controller, 
				ControllerDef.getTagSpec(\launchpad)
			).asMenu( "Grid controller" ),
		).front
		
	},

	showGeneralOverlayMenu_old: { arg self, view, x, y;
		WindowDef(\OverlayMenu).front(view, x, y, { arg def;
			VLayout (
				HLayout(
					Param(Message(self.maintrack), \showMixerView, ParamBoolSpec()).asButton.addAction({ self.maintrack.changed(\children) }),
				),
				HLayout(
					BasicButton.new.string_("Save").action_({
						self.maintrack.savePresetCompileStringDialog;
					}),
					BasicButton.new.string_("Load").action_({
						self.maintrack.loadPresetCompileStringDialog;
					}),
				),
				HLayout(
					StaticText.new.string_("controller:"),
					Param(
						Message(self.maintrack),
						\controller, 
						ControllerDef.getTagSpec(\launchpad)
					).asPopUpMenu.allowsReselection_(true),
				),
			)
		})
		
	},

	makeToolBar: { arg self;
		HLayout (
			BasicButton.new.string_("^").mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				var mult = 1;
				if(modifiers.isShift or: {buttonNumber == 1}) {
					mult = 8;
				};
				self.maintrack.gridOffset = self.maintrack.gridOffset + Point(0,-1*mult);
			}),
			BasicButton.new.string_("v").mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				var mult = 1;
				if(modifiers.isShift or: {buttonNumber == 1}) {
					mult = 8;
				};
				self.maintrack.gridOffset = self.maintrack.gridOffset + Point(0,1*mult);
			}),
			BasicButton.new.string_("<").mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				var mult = 1;
				if(modifiers.isShift or: {buttonNumber == 1}) {
					mult = 8;
				};
				self.maintrack.gridOffset = self.maintrack.gridOffset + Point(-1*mult,0);
			}),
			BasicButton.new.string_(">").mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				var mult = 1;
				if(modifiers.isShift or: {buttonNumber == 1}) {
					mult = 8;
				};
				self.maintrack.gridOffset = self.maintrack.gridOffset + Point(1*mult,0);
			}),
			nil,
			BasicButton.new.string_("...").mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				self.showGeneralOverlayMenu(view, x, y);
			}).maxWidth_(20),
		)
	},

	makeTrackFader: { arg self, trackidx;
		var child, boxview, mixer, mixerview;
		child = self.maintrack.childAtOffset(trackidx);
		if(child.notNil) {
			var win = WindowDef(\customgrid_mixer+++trackidx, WindowDef(\TrackMixerDef_advanced));
			var mproto;
			mixer = child.mixer;
			//mixerview = win.asView(mixer);
			mixerview = win.asView;
			mproto = mixerview.proto;
			mproto.neverShowPlay = true;
			if(self.maintrack.displayConfig.notNil) {
				self.maintrack.displayConfig.keysValuesDo { arg key,val;
					mproto[key] = val;
				};
			};
			mproto.refreshView;
			// model_ is already defined in mixer view
			//mixerview.debug("created mixerview");
			mixerview.maxWidth_(200);
			self.trackFaders = self.trackFaders ?? { nil ! self.trackCount };
			//if(trackidx == 0) { // debug
				//~dbfirstmix = mixerview;
			//};
			self.trackFaders[trackidx] = mixerview;
			mixerview;
		};
	},

	makeTrackHeader: { arg self, trackidx;
		var player, playerview;
		if(trackidx != ( self.trackCount - 1 )) { // if not last column
			var child;
			var maintrack = self.maintrack;
			child = maintrack.childAtOffset(trackidx);
			//[child, child.source, trackidx].debug("showTrackHeaders: child");
			player = PlayerWrapperGridCellView(child);
			playerview = player.asView;
			player.rightClickEditorEnabled = true;
			playerview.labelView.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				if(buttonNumber == 1) {
					child.editTrackTemplate;
				};
			});

			playerview.parentView.color_empty = self.color_header;
			playerview.layout.margins_(1);
			playerview.button.fixedWidth_(20);
			playerview.labelView.minWidth_(10);
			playerview.labelView.font_(Font.default.size_(10));
			self.trackHeaders = self.trackHeaders ?? { nil ! self.trackCount };
			self.trackHeaders[trackidx] = playerview;
			playerview;
		} {
			// last column for line players
			playerview = PlayerWrapperGridCellView(nil).asView;
			playerview.parentView.color_empty = self.color_header;
			playerview.layout.margins_(1);
			playerview.button.fixedWidth_(20);
			playerview.labelView.minWidth_(10);
			playerview.labelView.font_(Font.default.size_(10));
			self.trackHeaders = self.trackHeaders ?? { nil ! self.trackCount };
			self.trackHeaders[trackidx] = playerview;
			//"volume!!!".debug;
			playerview;
			//nil;
		}
	},

	makeOverlayMenu: { arg self, view, x, y, amodel;
		var model = amodel;
		var track = model.parentTrack;
		Menu(
			* [
				if(model.isLinePlayer == true) {
					Menu(
						CustomViewAction(
							Param( Message(model), \label, ParamStringSpec()).asTextField.minWidth_(200),
						)
					).title_("Section label")
				} {
					[
						if(track.isEmpty.not) {
							[
								Menu(
									CustomViewAction(
										Param( Message(model), \label, ParamStringSpec()).asTextField.minWidth_(200),
									)
								).title_("Cell label"),
								if(model.isEmpty) {
									[
										MenuAction("Create cell", {
											[model, model[\makeScore].asCompileString].debug("model");
											model.makeScore;
										}),
										MenuAction("Create and edit cell", {
											//[model].debug("model");
											model.makeScore;
											model.edit;
										}),
										MenuAction("Paste cell", {
											if(self.clipboard.notNil) {
												model.parentTrack.children[model.index] = self.clipboard.clone(model.parentTrack, model.index);
												model.parentTrack.changed(\children);
											};
										}),
									]
								} {
									[
										MenuAction("Edit cell", {
											model.edit;
										}),
										MenuAction("Copy cell", {
											self.clipboard = model;
										}),
										MenuAction.separator,
										MenuAction("Erase cell", {
											model.clearTrack;
										}),
									]
								},
								MenuAction.separator,
								Menu(
									CustomViewAction(
										Param( Message(track), \label, ParamStringSpec()).asTextField.minWidth_(200),
									)
								).title_("Track label"),
								MenuAction("Edit track instrument", {
									model.parentTrack.editInstrument;
								}),
								MenuAction("Erase track", {
									model.parentTrack.clearTrack;
								}),
							]
						},
						Param( Message(track), \trackTemplate, TagSpecDef(\CustomTrackTemplate) ).asMenu("Choose track template"),
						MenuAction.separator,
						Param(Message(model.parentTrack), \isMixerEnabled, ParamBoolSpec()).asMenuAction("Enable mixer"),
					]
				}
			].flat.select(_.notNil) 
		).front;
	},

	makeOverlayMenu_old: { arg self, view, x, y, amodel;
		WindowDef(\OverlayMenu).front(view, x, y, { arg def;
			var model = amodel;
			VLayout (
				* [
					if(model.isLinePlayer == true) {
						HLayout (
							StaticText.new.string_("label:"),
							Param( Message(model), \label, ).asTextField,
						) 
					} {
						[
							if(model.parentTrack.isEmpty.not) {
								[
									HLayout (
										StaticText.new.string_("label:"),
										Param( Message(model), \label, ).asTextField,
									) 
								] ++
								if(model.isEmpty) {
									[
										BasicButton.new.string_("Create cell").action_({
											[model, model[\makeScore].asCompileString].debug("model");
											model.makeScore;
											def.closeWindow;
										}),
										BasicButton.new.string_("Create and edit cell").action_({
											//[model].debug("model");
											model.makeScore;
											model.edit;
											def.closeWindow;
										}),

										BasicButton.new.string_("Paste cell").action_({
											if(self.clipboard.notNil) {
												model.parentTrack.children[model.index] = self.clipboard.clone(model.parentTrack, model.index);
												model.parentTrack.changed(\children);
												def.closeWindow;
											};
										}),
									]
								} {
									[
										BasicButton.new.string_("Edit cell").action_({
											model.edit;
											def.closeWindow;
										}),

										BasicButton.new.string_("Copy cell").action_({
											self.clipboard = model;
											def.closeWindow;
										}),

										StaticText.new,

										BasicButton.new.string_("Erase cell").action_({
											model.clearTrack;
											def.closeWindow;
										}),
									]
								} ++ [
									StaticText.new,
									BasicButton.new.string_("Edit track instrument").action_({
										model.parentTrack.editInstrument;
										def.closeWindow;
									}),
									BasicButton.new.string_("Erase track").action_({
										model.parentTrack.clearTrack;
										def.closeWindow;
									}),
								]
							},
							BasicButton.new.string_("Choose track template").action_({
								//[model].debug("model");
								model.parentTrack.editTrackTemplate;
								def.closeWindow;
							}),
							Param(Message(model.parentTrack.parentTrack), \showTrackHeaders, ParamBoolSpec()).asButton("Show track headers"),
							Param(Message(model.parentTrack.parentTrack), \showRecorderView, ParamBoolSpec()).asButton("Show track recorder views"),
							Param(Message(model.parentTrack), \isMixerEnabled, ParamBoolSpec()).asButton("Enable mixer"),
						].flatten
					}
				].flatten.select(_.notNil) ++ [nil]
			)
		})
	},
));

// compat
WindowDef(\customgrid, WindowDef(\TrackGrid));


WindowDef(\TrackList, (
	asView: { arg self, wdef, model;
		self.mainView = View.new;
		self.model = model;
		self.mainView.layout = VLayout(
			self.makeToolbar,
			self.makeTrackList,
		);
		
		self.mainView;
	},

	clipboard: { arg self;
		ParamViewToolBox.clipboard.trackDef
	},
	clipboard_: { arg self, val;
		ParamViewToolBox.clipboard.trackDef = val;
	},

	clipboardSection: { arg self;
		ParamViewToolBox.clipboard.trackDef_section
	},
	clipboardSection_: { arg self, val;
		ParamViewToolBox.clipboard.trackDef_section = val;
	},

	previewLen_: { arg self, len;
		self.model
	},

	makeToolbar: { arg self;
		var model = self.model;
		var playButton;
		playButton = PlayerWrapperView.new;
		self.mainView.followChange(model, \selectedPart, {
			playButton.model = model.linePlayerAt(model.selectedPart);
			//if(model.isPlaying) {
				//model.linePlayerAt(model.selectedPart).play;
			//};
		});

		HLayout (
			playButton.view,
			BasicButton.new.string_("Add").action_({
				Menu(
					* TagSpecDef(\CustomTrackTemplate).list.collect { arg asso, idx;
						MenuAction(asso.key, {
							model.addTrack(asso.value);
						})
					};
				).front;
			}),
			HLayout (
				StaticText.new.string_("Section:"),
				Param(Message(model), \selectedPart, ControlSpec(0,16,\lin,1)).asNumberBox,
			),
			HLayout (
				StaticText.new.string_("Len:"),
				Param(Message(model), \areasizex, ControlSpec(1,128,\lin)).asTextField,
			),
			BasicButton.new.string_("Mixer").action_({
				WindowDef(( \TrackListMixer_++model.key ).asSymbol, WindowDef(\TrackListMixer)).front(model);
			}),
			BasicButton.new.string_("Timeline").action_({
				TrackTimeline(model.key, model).edit;
			}),
			BasicButton.new.string_("Grid").action_({
				model.editGrid;
			}),
			BasicButton.new.string_("...").fixedWidth_(20).action_({
				self.showGeneralOverlayMenu;
			}),
		)
	},

	duplicateSection: { arg self, from, to;
		from = from ?? {self.model.selectedPart};
		to = to ?? { from+1 };
		[from, to].debug("duplicateSection");
		if(self.model.sectionAt(to).existingChildren.every(_.isEmpty)) {
			self.model.sectionAt(from).existingChildren.do { arg fromchild, idx;
				var tochild = self.model.sectionAt(to).childAt(fromchild.parentTrack.index);
				[fromchild, tochild].debug("duplicateSection pasteScore");
				self.pasteScore(fromchild, tochild);
			};
		} {
			Log(\Param).debug("can't duplicate, next section is not empty");
		}
		
	},

	copySection: { arg self, from;
		from = from ?? {self.model.selectedPart};
		self.clipboardSection = self.model.sectionAt(from);
	},

	pasteSection: { arg self, to;
		to = to ?? {self.model.selectedPart};
		if(self.clipboardSection.notNil) {
			if(self.model.sectionAt(to).existingChildren.every(_.isEmpty)) {
				self.clipboardSection.existingChildren.do { arg fromchild, idx;
					var tochild = self.model.sectionAt(to).childAt(fromchild.parentTrack.index);
					[fromchild, tochild].debug("pasteSection pasteScore");
					self.pasteScore(fromchild, tochild);
				};
			};
			
		};
	},

	showGeneralOverlayMenu: { arg self, view, x, y;
		var sectionEmpty = self.model.sectionAt(self.model.selectedPart).isEmpty;
		Menu(
			if(sectionEmpty) {
				MenuAction("Paste section", {
					self.pasteSection;
				}).enabled_(self.clipboardSection.notNil)
			} {
				MenuAction("Copy section", {
					self.copySection;
				})
			},
			MenuAction("Duplicate section", {
				self.duplicateSection
			}).enabled_(sectionEmpty.not),

			MenuAction.separator,
			MenuAction("Save", {
				self.model.savePresetCompileStringDialog;
			}),
			MenuAction("Load", {
				self.model.loadPresetCompileStringDialog;
			}),
			MenuAction("Copy refString to clipboard", { 
				self.model.refCompileString.pbcopy;
			}),
			MenuAction.separator,
			//Param(Message(self.model), \showMixerView, ParamBoolSpec()).asMenuAction,
			//Param(Message(self.model), \enableAutoSetOutbus, ParamBoolSpec()).asMenuAction,
			MenuAction("Edit properties", {
				self.model.editProperties;
			}).enabled_(self.model[\editProperties].notNil),
			Param(
				Message(self.model),
				\controller, 
				ControllerDef.getTagSpec(\launchpad)
			).asMenu( "Grid controller" ),
		).front
		
	},

	makeTrackList: { arg self;
		var view = View.new;
		var viewSelector = ProtoTemplateDef(\ViewSelector).new(0);
		var oldselidx;
		viewSelector.selected_color = ParamViewToolBox.color_pale;
		viewSelector.deselected_color = Color.clear;
		self.trackButtonSelector = { viewSelector };
		view.followChange(self.model, \tracklist, {
			view.removeAll;
			oldselidx = viewSelector.selected_index;
			viewSelector.views = [];
			view.layout = VLayout (*
				self.model.existingChildren.collect { arg child, idx;
					try {
						self.makeTrackView(child)
					} { arg error;
						Log(\Param).error("ERROR: in WindowDef(\TrackList).makeTrackView idx=%".format(idx));
						error.reportError;
						//error.throw;
						BasicButton.new.string_("error");
					}
				}.as(Array) ++ [nil];
			);
			viewSelector.select_item(oldselidx)
		});
		view;
	},

	pasteScore: { arg self, fromcell, tocell;
		var cell = tocell;
		if(self.clipboard.notNil) {
			cell.parentTrack.children[cell.index] = fromcell.clone(cell.parentTrack, cell.index);
			cell.parentTrack.parentTrack.changed(\tracklist);
		};
	},

	makeTrackMenu: { arg self, child, cell;
		
		Menu( *[
			CustomViewAction(
				View.new.layout_(HLayout (
					StaticText.new.string_("label:"),
					Param( Message(child), \label, ParamStringSpec()).asTextField(action:{ arg view;
						self.model.changed(\tracklist);

					}),
				)),
			),
			if(cell.isEmpty) {
				[
					MenuAction("Create and edit score", {
						cell.makeScore;
						self.model.changed(\tracklist); // overkill refresh
						cell.edit;
					}),
					MenuAction("Paste score", {
						self.pasteScore(self.clipboard, cell);
					}).enabled_(self.clipboard.notNil);
				]
			} {
				[
					MenuAction("Edit score", {
						cell.edit;
					}),
					MenuAction("Copy score", {
						self.clipboard = cell;
					})
				]
			},
			MenuAction("Edit instrument", {
				child.editInstrument;
			}),
			Param(Message(child), \isMixerEnabled, ParamBoolSpec()).asMenuAction("Enable mixer"),
			MenuAction.separator,
			MenuAction("Clear score", {
				cell.clearTrack;
				self.model.changed(\tracklist);
			}).enabled_(cell.isEmpty.not),
			MenuAction("Remove track", {
				self.model.removeTrack(child.index);
			}).enabled_(child.isEmpty.not)
		].flat).front;

		//WindowDef(\OverlayMenu).front(but, x, y, { arg def;
		//VLayout (
		//HLayout (
		//StaticText.new.string_("label:"),
		//Param( Message(child), \label, ParamStringSpec()).asTextField,
		//),
		//BasicButton.new.string_("Edit instrument").action_({
		//child.editInstrument;
		//def.closeWindow;
		//}),
		//BasicButton.new.string_("Edit track part").action_({
		//child.edit;
		//def.closeWindow;
		//}),
		//BasicButton.new.string_("Remove track").action_({
		//model.removeTrack(childidx);
		//def.closeWindow;
		//}),
		//Param(Message(child), \isMixerEnabled, ParamBoolSpec()).asButton("Enable mixer"),
		//)
		//})
	},

	makeTrackView_old: { arg self, child;
		var rowHeight = 80;
		var rowHeight_inset = rowHeight - 5;
		var model = self.model;
		var idx = model.selectedPart.asInteger;
		var cell = child.childAt(idx);
		var linename = "%.%".format(model.key, idx).asSymbol;
		var name = "%.%.%".format(model.key, childidx, idx).asSymbol;
		var playbut = PlayerWrapper(cell).label_("").asView;
		var lay;
		var chanview = View.new;
		var variableView;
		var viewSelector = self.trackButtonSelector;
		var childidx = child.index;
		var tracknamebut; 

		tracknamebut = BasicButton.new
			.string_("%: %".format(child.index, child.label))
			.action_({
				//Log(\Param).debug("edit: idx%, instr %", childidx, child.instrument.asCompileString);
				child.editInstrument;
			})
			.maxWidth_(200)
			.mouseDownAction_({ arg but, x, y, modifiers, buttonNumber, clickCount;
				//[but, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
		
				if(buttonNumber == 1) { // if right-click
					self.makeTrackMenu(child, cell);
				}
			})
		;
  
		variableView = View.new.followChange(child, \mixer, { arg view;
			view.removeAll;
			//Log(\Param).debug("mixer variableView: %, %, %", child.label, child.isMixerEnabled, child.mixer);
			if(child.isMixerEnabled) {
				view.layout = HLayout (
					Param(Message(child.mixer), \muteMode, ParamBoolSpec()).asButton.string_("M").fixedSize_(Point(15,15)),
					Param(Message(child.mixer), \soloMode, ParamBoolSpec()).asButton.string_("S").fixedSize_(Point(15,15)),
					StackLayout(
						child.mixer.asParam(\postfader).asSlider.orientation_(\horizontal).background_(Color.clear),
						CompactServerMeterView(nil, 0,2, child.mixer.recbus).orientation_(\horizontal),
					).mode_(\stackAll);
				).spacing_(0).margins_(0)
			}
		});

		//playbut.debug("i");
		playbut.button.fixedWidth_(30);
		//playbut.parentView.states = [
		//[""],
		//["", Color.black,ParamViewToolBox.color_ligth],
		//];
		//playbut.parentView.update;
		lay = HLayout (
			VLayout (
				HLayout (
					VLayout (
						playbut,
						Param(Message(child), \recordArmed, ParamBoolSpec()).asButton("●").fixedWidth_(30),
						nil,
					),
					VLayout (
						tracknamebut,
						HLayout (
							Param(
								Message(child),
								\outBus, 
								TagSpec({[\default -> nil, \0->0] ++ TagSpecDef(\BusDef_object).list})
							).asPopUpMenu.maxWidth_(100),
							Param(
								Message(cell),
								\outBus, 
								TagSpec({[\default -> nil, \0->0] ++ TagSpecDef(\BusDef_object).list})
							).asPopUpMenu.maxWidth_(100),
							nil,
						),
						nil
					),
				),
				variableView,
			),
			[{
				if(cell.isEmpty) {
					BasicButton.new.string_("make score").action_({
						cell.makeScore;
						model.changed(\tracklist);
					}).fixedHeight_(rowHeight_inset)
				} {
					var preview;
					//var preview = cell.score.timeline.asPreview.fixedHeight_(rowHeight_inset);
					//Log(\Param).debug("InstrumentTrackPart: cell %", cell);
					preview = ( cell.asPreview ?? { View.new } ).fixedHeight_(rowHeight_inset);
					//timelinePreviewList.add(preview);
					//preview.timeline.areasize.x = model.areasizex ?? { preview.timeline.areasize.x };
					//preview.timeline.changed(\areasize);
					preview;
				}

			}.value, stretch:100],
		);
		chanview.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount, childidx].debug("mouseDownAction");
			//view.background = ParamViewToolBox.color_dark;
			//view.background = Color.grey;
			viewSelector.select_item(childidx);
			if(FileSystemProject.current.notNil) {

				FileSystemProject.current.tracklist_selected_track = [\TrackListPart, linename -> model.linePlayerAt(idx)];
				FileSystemProject.current.changed(\tracklist_selected_track);
				FileSystemProject.current.tracklist_selected_track = [\InstrumentTrackPart, name -> cell];
				FileSystemProject.current.changed(\tracklist_selected_track);
				//FileSystemProject.current.tracklist_selected_track.debug("sel track");
			}

		});
		lay.spacing_(3);
		lay.margins_(0);
		chanview.fixedHeight_(rowHeight);
		chanview.layout_(lay);
		viewSelector.add_view(chanview);
		chanview;
	},

	makeTrackView: { arg self, child;
		var rowHeight = 30;
		var rowHeight_inset = rowHeight - 5;
		var model = self.model;
		var idx = model.selectedPart.asInteger;
		var cell = child.childAt(idx);
		var linename = "%.%".format(model.key, idx).asSymbol;
		var name = "%.%.%".format(model.key, childidx, idx).asSymbol;
		var playbut = PlayerWrapper(cell).label_("").asView;
		var lay;
		var chanview = View.new;
		var variableView;
		var viewSelector = self.trackButtonSelector;
		var childidx = child.index;
		var tracknamebut; 

		tracknamebut = BasicButton.new
			.string_("%: %".format(child.index, child.label))
			.action_({
				//Log(\Param).debug("edit: idx%, instr %", childidx, child.instrument.asCompileString);
				child.editInstrument;
			})
			.fixedWidth_(200)
			.mouseDownAction_({ arg but, x, y, modifiers, buttonNumber, clickCount;
				//[but, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
		
				if(buttonNumber == 1) { // if right-click
					self.makeTrackMenu(child, cell);
				}
			})
		;
  
		//variableView = View.new.followChange(child, \mixer, { arg view;
			//view.removeAll;
			////Log(\Param).debug("mixer variableView: %, %, %", child.label, child.isMixerEnabled, child.mixer);
			//if(child.isMixerEnabled) {
				//view.layout = HLayout (
					//Param(Message(child.mixer), \muteMode, ParamBoolSpec()).asButton.string_("M").fixedSize_(Point(15,15)),
					//Param(Message(child.mixer), \soloMode, ParamBoolSpec()).asButton.string_("S").fixedSize_(Point(15,15)),
					//StackLayout(
						//child.mixer.asParam(\postfader).asSlider.orientation_(\horizontal).background_(Color.clear),
						//CompactServerMeterView(nil, 0,2, child.mixer.recbus).orientation_(\horizontal),
					//).mode_(\stackAll);
				//).spacing_(0).margins_(0)
			//}
		//});

		//playbut.debug("i");
		playbut.button.fixedWidth_(30);
		//playbut.parentView.states = [
		//[""],
		//["", Color.black,ParamViewToolBox.color_ligth],
		//];
		//playbut.parentView.update;
		lay = HLayout (
			HLayout (
				playbut,
				tracknamebut,
			),
			[{
				if(cell.isEmpty) {
					BasicButton.new.string_("make score").action_({
						cell.makeScore;
						model.changed(\tracklist);
					}).fixedHeight_(rowHeight_inset)
				} {
					var preview;
					//var preview = cell.score.timeline.asPreview.fixedHeight_(rowHeight_inset);
					//Log(\Param).debug("InstrumentTrackPart: cell %", cell);
					preview = ( cell.asPreview ?? { View.new } ).fixedHeight_(rowHeight_inset);
					//timelinePreviewList.add(preview);
					//preview.timeline.areasize.x = model.areasizex ?? { preview.timeline.areasize.x };
					//preview.timeline.changed(\areasize);
					preview;
				}

			}.value, stretch:100],
		);
		chanview.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount, childidx].debug("mouseDownAction");
			//view.background = ParamViewToolBox.color_dark;
			//view.background = Color.grey;
			viewSelector.select_item(childidx);
			if(FileSystemProject.current.notNil) {

				FileSystemProject.current.tracklist_selected_track = [\TrackListPart, linename -> model.linePlayerAt(idx)];
				FileSystemProject.current.changed(\tracklist_selected_track);
				FileSystemProject.current.tracklist_selected_track = [\InstrumentTrackPart, name -> cell];
				FileSystemProject.current.changed(\tracklist_selected_track);
				//FileSystemProject.current.tracklist_selected_track.debug("sel track");
			}

		});
		lay.spacing_(3);
		lay.margins_(0);
		chanview.fixedHeight_(rowHeight);
		chanview.layout_(lay);
		viewSelector.add_view(chanview);
		chanview;
	},
));



(
WindowDef(\TrackListMixer, { arg def, tracklist;
	var mixers;
	var menuButton;
	var mainview = View.new;

	menuButton = BasicButton.new.string_("...").action_({
		Menu(
			//MenuAction("Save preset", {
				//tracklist.savePresetCompileStringDialog;
			//}),
			//MenuAction("Load preset", {
				//tracklist.loadPresetCompileStringDialog;
			//}),
			//Menu(
				* WindowDef(\TrackMixerDef_advanced).proto.displayOptions.collect { arg item, idx;
					var curval;
					if(tracklist.displayConfig.isNil) {
						tracklist.displayConfig = ();
					};
					curval = tracklist.displayConfig.perform(item);
					curval = curval ?? { mixers.first.proto.perform(item) };
					MenuAction(item, {
						tracklist.displayConfig.perform(item.asSetter, curval.not);
						mixers.do { arg mixerview, idx;
							mixerview.proto.perform(item.asSetter, curval.not)
						};
					}).checked_(curval)
				}
				//++ [
					//Param(Message(self.maintrack), \showTrackHeaders, ParamBoolSpec()).asMenuAction(),
					//Param(Message(self.maintrack), \showRecorderView, ParamBoolSpec()).asMenuAction(),
				//]
			//).title_("Mixer layout"),
		).front
	});
	menuButton.fixedWidth_(20);


	//HLayout(* mixers ++ [menuButton]);
	mainview.followChange(tracklist, \trackCount, {
		mainview.removeAll;

		mixers = tracklist.existingChildren.collect { arg track, idx;
			var mixer = track.mixer;
			var mproto;
			var boxview;

			boxview = WindowDef(\TrackList_mixer+++idx, WindowDef(\TrackMixerDef_advanced)).asView;
			mproto = boxview.proto;
			mproto.track = track;
			boxview;
		}.as(Array);
		mainview.layout = HLayout(* mixers ++ [[ menuButton, align:\top ], nil]);
	});
	mainview;
});


WindowDef(\TrackListMixerStrip, { arg def, mixer, mixerTemplate;
	// not used anymore
		var sendKnobCount = 8;
		var param;
		var sends = [], allsends = [];
		var layout;
		var slider, knobs = List.new, textfield;
		var stack;
		var meterin = SimpleServerMeterView(nil, 0,2, mixer.inbus);
		var meterout = CompactServerMeterView(nil, 0,2, mixer.recbus);
		meterout.ticksView.minWidth_(6);
		slider = Slider.new.orientation_(\vertical).background_(Color.clear);


		stack = StackLayout(
			slider, 
			meterout.view,
			// FIXME: no way to make ticks less wide, so with both in and out displayed, meter is too small
			//View.new.layout_(HLayout (
				//meterin.view,
				//meterout.view,
			//).spacing_(0).margins_(0))
		).mode_(\stackAll);
		layout = VLayout(
			HLayout(
				VLayout (
					//slider,
					stack,
					textfield = TextField.new,
				),
				VLayout(*
					knobs = sendKnobCount.collect({ 
						Knob.new;
					})
				)
			)
		);
		layout.addUniqueMethod(\model_, { arg me, mixer;
			if(mixer.notNil) {

				if(mixer.isKindOf(Volume)) {
					//"volume".debug;
					param = Param(mixer, \volume);
					sends = mixerTemplate.sendCount.collect({ arg x; x -> nil });
				} {
					//"mixer".debug;
					param = mixer.asParam(\postfader);
					allsends = mixerTemplate.sendCount.collect({ arg x; x -> mixer.asParam(\send+++x) });
					sends = allsends.select{ arg assoidx;
						var idx = assoidx.key;
						var bus = mixerTemplate[\sendbus +++ idx].value(mixerTemplate);
						bus != mixerTemplate.nullbus
					};
				};
				//[slider, param].debug("slider, param: what ?");
				slider.mapParam(param);
				textfield.mapParam(param);
				knobs.do({ arg k, idx; 
					var send = sends[idx] ?? { allsends[idx] }; 
					k.mapParam(send.value); 
					k.toolTip = "%: %".format(send.key, mixerTemplate[\sendbus +++ send.key].value(mixerTemplate));
				});
				slider.followChange(mixerTemplate)
			} {
				slider.mapParam(nil);
				textfield.mapParam(nil);
				knobs.do({ arg k, idx; 
					k.mapParam(nil);
					k.toolTip = "";
				});
			};
		});
		if(mixer.notNil) {
			layout.model = mixer;
		};
		layout.addUniqueMethod(\slider, { slider });
		layout.addUniqueMethod(\textfield, { textfield });
		layout.addUniqueMethod(\knobs, { knobs });
		layout;
});

WindowDef(\TrackListMixerStrip_OverlayMenu, { arg def, mixer, mixerTemplate, view, x=600, y=600;
	var inview, layout;
	var sends;
	//[x, y].debug("xy");
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 300,100);
		win.endFrontAction = { win.close };
	};
	inview = View.new;

	sends = mixerTemplate.sendCount.collect({ arg idx;
		HLayout(
			StaticText.new.string_("send %".format(idx)),
			Param(mixer.proxy, "%%".format(\sendbus, idx).asSymbol, TagSpecDef(\BusDef_object)).asPopUpMenu,
		)
	});

	layout = VLayout (
		//PlayerWrapper(mixerTemplate).asView,
		//HLayout(
			//StaticText.new.string_("Track count:"),
			//Param(Message(mixerTemplate), \trackCount, ControlSpec(1,16,\lin,1)).asNumberBox,
		//),
		WindowDef(\TrackMixerDef_fxlist).asView(mixer),
		VLayout(
			*sends
		),
		nil
	);
	//def.windowDo { arg win;
		//win.setInnerExtent(view.bounds.extent.min(20));
	//};
	inview.layout_(layout);
	
}).border_(false);
);
