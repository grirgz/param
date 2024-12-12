
///////////////////////////////////////////////////////////////////// timeline panel
//////////////////////////////// Timeline Panel Views

/////// base timeline panel view

ProtoTemplateDef(\TimelinePanelView, (
	new: { arg self, eventloop;
		//"timelinePanelView".debug("init");
		self = self.deepCopy;

		self.eventloop = eventloop;
		self.areasize = 10@1;
		self.quant_ref = Ref(Point(1/8,1));

		//self.definePattern;

		//"timelinePanelView".debug("end init");
		self;
	},


	timelineInstrumentParamGroup: { arg self;
		var target;
		if(self.param.isNil) {
			ParamGroup()
		} {
			target = self.param.target;
			target.asParamGroup
				.add( Param(target, \velocity, ControlSpec(0,127,\lin,0,64)) )
				.add( Param(target, \sustain) )
				.reject({ arg x; x.property == \legato })
			;
		}
	},

	asPattern: { arg self;
		// TODO
		Pfunc({})
	},

	////// properties

	save_timeline_properties: { arg self;
		var tl = self;
		// tl is view ProtoDef, tl.timeline is View subclass, tl.timeline_model is the object programmer interact with
		if(tl.timeline.notNil) {
			tl.timeline_model.addHalo(\viewport, tl.timeline.viewport);
			tl.timeline_model.addHalo(\areasize, tl.timeline.areasize);
			tl.timeline_model.addHalo(\enablePreview, tl.timeline.enablePreview);
			// quant_ref is now member of timeline model
			//tl.timeline_model.addHalo(\gridquant, tl.timeline.quant_ref.value); // this line seems wrong
			//tl.quant_ref.value.debug("quant_ref saving!!!");
			//tl.quant_ref.value ?? { tl.timeline_model.addHalo(\gridquant, tl.quant_ref.value); }
		}
	},

	load_timeline_properties: { arg self;
		var tl = self;
		var cur;
		cur = tl.timeline_model.getHalo(\viewport);
		if(cur.notNil) {
			tl.timeline.viewport = cur;
		};
		cur = tl.timeline_model.getHalo(\areasize) ? self.pr_areasize;
		if(cur.notNil) {
			tl.timeline.areasize = cur;
		};
		tl.timeline_model.getHalo(\enablePreview) !? { arg x;
			tl.timeline.enablePreview = x;
		};
		//tl.timeline_model.getHalo(\gridquant) ?? { arg x; self.timeline.quant_ref.value = x; }; // this line seems wrong
		//tl.timeline_model.getHalo(\gridquant) ?? { arg x; self.quant_ref.value = x; };
	},

	areasize: { arg self;
		if(self.timeline.notNil) {
			self.timeline.areasize 
		} {
			self.pr_areasize;
		}
	},

	areasize_: { arg self, val;
		if(self.timeline.notNil) {
			self.timeline.areasize = val;
		};
		self.pr_areasize = val;
	},


	node_move_multiplier_x: 8,
	node_move_multiplier_y: 8,

	set_xquant: { arg self, quant;
		self.quant_ref.value = Point(quant, self.quant_ref.value.y);
	},

	get_xquant: { arg self;
		self.quant_ref.value.x;
	},

	set_yquant: { arg self, quant;
		self.quant_ref.value = Point(self.quant_ref.value.x, quant);
	},

	get_yquant: { arg self;
		self.quant_ref.value.y;
	},

	eventLoop: { arg self;
		self.timeline_model.eventLoop;
	},

	eventLoop_: { arg self, val;
		self.timeline_model.eventLoop = val;
		self;
	},

	eventList: { arg self;
		self.timeline_model.eventList;
	},

	eventList_: { arg self, val;
		self.timeline_model.eventList = val;
		self;
	},


	////// saving system

	hasSavePath: { arg self;
		self.timeline_model.hasSavePath
	},


	////// gui components

	// update

	refresh: { arg self;
		// event list has changed, refresh the mappings
		self.timeline.mapEventList(self.eventloop.list);

		if(self.cursor_timeline.notNil) {
			self.cursor_timeline.mapEventList(self.eventloop.list);
		};
		if(self.locatorbar.notNil) {
			self.locatorbar.mapEventList(self.eventloop.list);
		};
		if(self.ruler_x_beat.notNil) {
			self.ruler_x_beat.mapEventList(self.eventloop.list);
		};
		if(self.velocityTimeline.notNil) {
			self.velocityTimeline.mapEventList(self.eventloop.list);
		}
	},

	update_track_header: { arg self;
		var track_count = self.timeline_model.track_count;
		var track_header_block = { arg x;
			var idx = track_count - 1 - x; // reverse index to have first track at bottom
			var res = View.new.layout_(
				VLayout(
					HLayout(
						//XSimpleButton.new.label_("track %".format(x)).
						self.track_selector.views[idx],
						Param(Message(self.timeline_model.tracks[idx]), \muted, XBoolSpec()).asButton("M").maxWidth_(20).addAction({
							self.timeline_model.computeSoloMute;
						}),
						Param(Message(self.timeline_model.tracks[idx]), \solo, XBoolSpec()).asButton("S").maxWidth_(20).addAction({
							self.timeline_model.computeSoloMute;
						}),
					).spacing_(0).margins_(2),
					HLayout (
						Param(Message(self.timeline_model.tracks[idx]), \recordBus, TagSpecDef(\BusDef_object)).asPopUpMenu.maxWidth_(100),
						Param(Message(self.timeline_model.tracks[idx]), \recordArmed, XBoolSpec()).asButton("R").maxWidth_(20),
						nil,
					).spacing_(0).margins_(2),
				).spacing_(0).margins_(0)
			);
			//res.background_(Color.blue);
			res;
		};
		self.track_selector = ProtoTemplateDef(\ViewSelector).new(track_count, {}, { arg view, x; 
			//x = track_count - x;
			if(self.timeline_model.is_track_bypassed(x)) {
				view.label_("[track %]".format(x));
			} {
				view.label_("track %".format(x));
			}
		});
		self.track_header.canvas_(
			self.track_header_view = View.new.layout_(
				VLayout(*
					track_count.collect { arg x;
						[
							track_header_block.(x),
							align: \top,
						]
					}
				).spacing_(0).margins_(0)
			);
			self.track_header_view
		).maxWidth_(130);
		self.update_track_header_size;
		self.track_header.onResize = { self.update_track_header_size };
		if(self.track_header_controller.notNil) {
			self.track_header_controller.remove;
		};
		self.track_header_controller = SimpleController(self.timeline)
		.put(\viewport, {
			//"update_track_header_size: viewport".debug;
			//TODO: remove controller
			if(self.track_header.isNil or: {self.track_header.isClosed}) {
				self.track_header_controller.remove;
			} {
				self.update_track_header_size;
			}

		})
		.put(\areasize, {
			//"update_track_header_size: areasize".debug;
			if(self.track_header.isNil or: {self.track_header.isClosed}) {
				self.track_header_controller.remove;
			} {
				self.update_track_header_size;
			}
			//self.update_track_header;
		})
		;
	},

	update_track_header_size: { arg self;
		//self.verticalScroller.view.doAction;
		if(self.verticalScroller.notNil) {
			self.verticalScroller_track_header_action(self.verticalScroller.view)
		} {
			if(self.timeline.notNil) {
				//"update_track_header_size: real updating now!".debug;
				//[self.timeline.bounds.height, self.timeline.viewport.height].debug("bh, vh");
				self.track_header_view.fixedHeight = self.timeline.bounds.height / self.timeline.viewport.height;
				self.track_header.visibleOrigin = Point(0,(1-self.timeline.viewport.bottom) * self.track_header.bounds.height);
				//self.timeline.viewport.top.debug("top");
				//[self.timeline.viewport, self.timeline.viewport.bottom, self.track_header.bounds.height, (1-self.timeline.viewport.bottom) * self.track_header.bounds.height].debug("[self.timeline.viewport, self.timeline.viewport.bottom, self.track_header.bounds.height, (1-self.timeline.viewport.bottom) * self.track_header.bounds.height]");
				//self.track_header.visibleOrigin.debug("update_track_header_size: visibleOrigin");
			}
		}
	},

	getTimelineKeyDownAction: { arg self;
		//Window.keyDownActionTest
		({ arg me, key, modifiers, unicode, keycode;
			//[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction copypaste");
			self.getTimelineKeyDownActionForNodeMove.(me, key, modifiers, unicode, keycode);
			if(modifiers.isCtrl) {
				//if(key == $n) { // ^n // don't work anymore
				if(keycode == $n.ascii) { // ^n
					"new!".debug;
					self.newScoreAction;
				};
				if(keycode == $k.ascii) { // ^k
					"clone!".debug;
					self.cloneScoreAction;
				};
				if(keycode == $d.ascii) { // ^d
					"double!".debug;
					self.doubleScoreAction;
				};
				if(keycode == $c.ascii) { // ^c
					"copy!".debug;
					self.copyAction;
				};
				if(keycode == $v.ascii) { // ^v
					"paste!".debug;
					self.pasteAction;
				};
				if(keycode == $s.ascii) { // ^s
					"split!".debug;
					self.splitAction;
				};
				if(keycode == $ .ascii) { // ^s
					"stop!".debug;
					self.timeline_model.stopNow;
				}
			} {
				// no modifier

				if(keycode == $ .ascii) { // ^s
					"play/stop now!".debug;
					if(self.timeline_model.selection_cursor.notNil) {
						self.timeline_model.cursor.setPositions(
							self.timeline_model.selection_cursor.startPosition ? 0,
							self.timeline_model.selection_cursor.endPosition
						);
					} {
						Log(\Param).debug("no selection cursor");
					};
					self.timeline_model.playNow(1);
				};
				// quantize

				if(key == $q) {
					self.quantizeAction;
				};
				if(keycode == 65481) { // F12
					"Panic!".debug;
					CmdPeriod.run;
				};
				if(keycode == 65474) { // F5
					"play!".debug;
					PlayerWrapper(self.timeline_model).togglePlay;
				}
			}
		});
	},

	getTimelineKeyDownActionForNodeMove: { arg self;
		var reorderButKeepSelection = {
			// FIXME: this is ugly, should integrate in TimelineView with a clean algo
			var selmodels = IdentitySet.new;
			var selnodes = List.new;
			self.timeline.selNodes.do { arg node;
				selmodels.add(node.model);
			};
			self.timeline.model.reorder;
			self.timeline.model.changed(\refresh);
			self.timeline.nodes.do { arg node;
				if(selmodels.matchItem(node.model)) {
					selnodes.add(node)
				}
			};
			self.timeline.selectNodes(selnodes);
		};
		var handleMove = { arg distance, keycode;
			if(keycode == 65364) { // down key
				self.timeline.selNodes.do{ arg node;
					node.model[node.posyKey] = node.model[node.posyKey]-distance.y;
					node.model.changed(\refresh);
				}
			};
			if(keycode == 65362) { // up key
				self.timeline.selNodes.do{ arg node;
					node.model[node.posyKey] = node.model[node.posyKey]+distance.y;
					node.model.changed(\refresh);
				}
			};
			if(keycode == 65361) { // left key
				self.timeline.selNodes.do{ arg node;
					node.model[node.timeKey] = node.model[node.timeKey]-distance.x;
				};
				reorderButKeepSelection.();
			};
			if(keycode == 65363) { // right key
				self.timeline.selNodes.do{ arg node;
					node.model[node.timeKey] = node.model[node.timeKey]+distance.x;
				};
				reorderButKeepSelection.();
			};
		};
		({ arg me, key, modifiers, unicode, keycode;
			//[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction copypaste");
			var quant = self.quant_ref.value.copy;
			if(quant.x == 0) { // can't move when quant is zero, use pixel instead
				quant.x = self.timeline.pixelExtentToGridExtent(Point(1,1)).x;
			};
			if(quant.y == 0) { // can't move when quant is zero, use pixel instead
				quant.y = self.timeline.pixelExtentToGridExtent(Point(1,1)).y;
			};
			if(modifiers.isCtrl) {
				handleMove.(
					Point(
						quant.x * self.node_move_multiplier_x,
						quant.y * self.node_move_multiplier_y,
					),
					keycode,
				)
			} {
				// no modifier
				handleMove.(quant, keycode)
			}
		});
	},

	// make

	make_track_header: { arg self;
		// should return the track header view
		var track_header;
		track_header = ScrollView.new;
		track_header.keyDownAction = self.getTimelineKeyDownAction;
		track_header.hasBorder = false;
		track_header.hasVerticalScroller = false;
		track_header.hasHorizontalScroller = false;
		self.track_header = track_header;

		self.verticalScroller_track_header_action = { arg self, slider;
			if(slider.isNil) {
				//Log(\Param).debug("dont know why there is two actions with one nil: % %", self.key, self.verticalScroller.view.action.array);
			} {

				self.track_header_view.fixedHeight = 1/slider.range * track_header.bounds.height;
				//( 1/slider.range * track_header.bounds.height ).debug("vrange.view.action: fixedHeight");
				track_header.visibleOrigin = Point(0, ( 1-slider.hi ) / slider.range) * track_header.bounds.height;
				//( Point(0, ( 1-slider.hi ) / slider.range) * track_header.bounds.height ).debug("vrange.view.action: visibleOrigin");
			};
		};

		self.verticalScroller.view.addAction({ arg slider;
			self.verticalScroller_track_header_action.(slider)
		});

		self.make_tracks_listeners;
		self.update_track_header;

		//self.verticalScroller.view.activeLo = self.verticalScroller.view.lo; // trigger action

		self.track_header;
	},

	make_undo_system_buttons: { arg self;
		var el = self.timeline_model.eventLoop;
		var undobut, redobut;

		undobut = BasicButton.new.string_("<Undo").action_({
			self.undoAction;
		}).enabled_(el.historyCanUndo).fixedWidth_(50).toolTip_("Undo");

		redobut = BasicButton.new.string_("->").action_({ arg but;
			self.redoAction;
		}).enabled_(el.historyCanRedo).fixedWidth_(30).toolTip_("Redo");

		redobut.followChange(el, \inHistoryMode, {
			var el = self.timeline_model.eventLoop;
			undobut.enabled_(el.historyCanUndo);
			redobut.enabled_(el.historyCanRedo);
		});

		HLayout (
			undobut,
			redobut,
		).spacing_(1)
	},

	makeTimelineView: { arg self;
		self.timelineClass.new
	},

	makeWindow: { arg self;
		//var window = Window.new;
		WindowDef("%_%".format(self.timeline_model.eventType, self.timeline_model.key).asSymbol, { arg me;
			var layout;
			var etype;
			etype = self.timeline_model.eventType.asString;
			etype = "%%".format(etype[0].toUpper, etype[1..]);
			me.windowName = "%: %".format(etype, self.timeline_model.key);
			me.window.onClose_({
				self.save_timeline_properties;
			});
			layout = self.makeLayout;
			//window.layout = layout;
			//window.alwaysOnTop = true;
			//window.front;	
			layout
		}).front
	},

	make_tracks_listeners: { arg self;
		if(self.tracks_controller.notNil) {
			self.tracks_controller.remove;
		};
		self.tracks_controller = SimpleController(self.timeline_model)
		.put(\tracks, {
			//"update_track_header_size: areasize".debug;
			if(self.track_header.isNil or: {self.track_header.isClosed}) {
				self.tracks_controller.remove;
			} {
				self.update_track_header;
				self.areasize = Point(self.areasize.x, self.timeline_model.track_count);
			}
			//self.update_track_header;
		})
		;
	},

	make_saveas_button: { arg self;
		BasicButton.new
			.action_({ arg me;
				self.saveAction(me)
			})
			.mouseUpAction_({ arg me, x, y, mod, but;
				//[a,b,c,but].debug("but");
				if(but == 1) { // right click
					self.saveAsAction(me)
				}
			})
			.refreshChangeAction_({ arg me;
				if(self.timeline_model.presetCompileStringSavePath.notNil) {
					me.string = "Save"
				} {
					me.string = "Save as"
				};
			})
			.refreshChange
		;
	},

	make_header: { arg self;
		//self.header_layout = HLayout(
		//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
		//	self.make_stop_button,
		//	XSimpleButton.new.label_("Add track").action_({
		//		self.add_track;
		//	}),
		//	self.make_quant_popup,
		//);
		//
		//self.header_layout;
		View.new;
	},

	makeRulerY: { arg self, timeline;
		//self.ruler_y = MidinoteTimelineRulerView.new;
		//self.ruler_y.mimicTimeline(timeline);
		//self.ruler_y;
		nil
	},

	make_stop_button: { arg self;
		BasicButton.new.label_("⏹").action_({ // doesnt work with my font but show a square anyway
			self.cursor.startPosition = nil;
			self.cursor.endPosition = nil;
			PlayerWrapper(self.timeline_model).stop;
			//self.cursor_timeline.stop; // FIXME: should be automatic when stoping Pdef
		}).fixedWidth_(30).maxHeight_(25).font_(Font.default.size_(16));
	},

	make_repeatCount_box: { arg self;
		Param(Message(self.timeline_model), \repeatCount, ControlSpec(1,inf,\lin)).asNumberBox.maxWidth_(40)
	},

	make_quant_spec: { arg self;
		TagSpec([
			"0",
			"1/32",
			"1/16",
			"1/8",
			"1/4",
			"1/2",
			"1",
			"2",
			"4",
			"8",
			"16",
		].collect({ arg x; x -> x.interpret }));
	},

	make_quant_popup: { arg self;
		var quant_popup;
		var quant_spec;
		quant_spec = self.make_quant_spec;

		self.quant_param = Param({ self.get_xquant }, { arg val; self.set_xquant(val) }, quant_spec);
		quant_popup = self.quant_param.asPopUpMenu.toolTip_("Quant");
		self.quant_popup = quant_popup;
		self.quant_popup;
	}, 

	makeCursorTimeline: { arg self;
		var cursor_timeline;
		
		cursor_timeline = CursorTimelineView.new;

		//~cursor_timeline = cursor_timeline;
		self.cursor_timeline = cursor_timeline;
		cursor_timeline.mapCursor(self.cursor);
		cursor_timeline.quant = self.quant_ref;
		cursor_timeline.mimicTimeline(self.timeline);
		if(self.timeline_model.isPlaying) {
			self.cursor_timeline.play;
		};
	},

	makeXRuler: { arg self;
		var locatorbar;
		var ruler_x_beat;

		self.makeCursorTimeline;

		ruler_x_beat = TimelineRulerView.new;
		ruler_x_beat.mapCursor(self.cursor);
		ruler_x_beat.quant = self.quant_ref;
		self.ruler_x_beat = ruler_x_beat;
		locatorbar = TimelineLocatorBarView.new;
		self.locatorbar = locatorbar;
		self.locatorbar.mouseDownAction = {|me, px, py, mod, buttonNumber, clickCount, chosennode|
			if(clickCount == 2) {
				if(chosennode.notNil) {
					TimelineLocatorPropertiesView.new(chosennode.model);
				};
			}
		};

		locatorbar.quant = self.quant_ref;

		ruler_x_beat.mimicTimeline(self.timeline);
		locatorbar.mimicTimeline(self.timeline);

		self.locatorbar.view.minSize = Point(10,15);
		self.ruler_x_beat.view.minSize = Point(10,10);

		self.locatorbar.keyDownAction = self.getTimelineKeyDownAction;
		self.ruler_x_beat.keyDownAction = self.getTimelineKeyDownAction;
		
		VLayout(
			locatorbar.view,
			self.ruler_x_beat.view,
		);
	},

	initTimelineView: { arg self, timeline;
		if(timeline.notNil) {
			self.timeline = timeline;
		};
		self.timeline.mouseDownAction = {|me, px, py, mod, buttonNumber, clickCount, chosennode|
			if(clickCount == 2) {
				if(chosennode.notNil) {
					self.showClipEditor(chosennode.model, self.eventloop.list)
				};
			};
			if(buttonNumber == 1 and: { mod.isCtrl.not }) {
				self.showTimelineContextMenu(chosennode)
				
			};
		};
		self.timeline.preCreateNodeHook = {
			self.timeline_model.addHistorySnapshot;
		};

		self.timeline.quant = self.quant_ref;
		self.timeline.mapSelectionCursor(self.selection_cursor);
		self.timeline.keyDownAction = self.getTimelineKeyDownAction;
		self.timeline.deleteSelectedNodesHook = self.deleteSelectedNodesHook;
	},

	makeSizerButton: { arg self;
		var sizer = HLayout(
			BasicButton.new.label_("<").action_({
				var point = Point(2,1);
				//self.areasize = self.areasize - point;
				self.areasize = self.areasize / point;
				self.timeline.refresh;
			}).fixedSize_(Point(10,20)),
			BasicButton.new.label_(">").action_({
				var point = Point(2,1);
				self.areasize = self.areasize * point;
				self.timeline.refresh;
			}).fixedSize_(Point(10,20)),
		).spacing_(0) ;
		self.sizerButton = sizer;
		sizer;
	},

	makeHorizontalScroller: { arg self;
		self.horizontalScroller = TimelineScroller.new;
		self.horizontalScroller.orientation = \horizontal;
		self.horizontalScroller.mapTimeline(self.timeline);
	},

	makeVerticalScroller: { arg self;
		self.verticalScroller = TimelineScroller.new;
		self.verticalScroller.orientation = \vertical;
		self.verticalScroller.mapTimeline(self.timeline);
	},

	makeLayout: { arg self;
		var layout;
		var tl_layout;
		var vlayout, timeline, vrange, hrange, size;
		var eventloop = self.eventloop;
		var ruler_y, sizer;
		var sizer_action;
		var track_header;
		var track_header_view;
		var quant_popup;
		layout = HLayout.new;
		self.layout = layout;
		vlayout = VLayout.new;
		layout.add(vlayout);

		self.save_timeline_properties;
		timeline = self.makeTimelineView;
		self.timeline = timeline;
		self.load_timeline_properties;
		self.initTimelineView;

		//self.makeVelocityView;
		self.makeBottomPanelView;

		self.ruler_x = self.makeXRuler;

		self.makeVerticalScroller;
		self.makeHorizontalScroller;
		self.makeSizerButton;


		// FIXME: created even if disabled, sometime not what we want
		track_header = self.make_track_header(timeline);


		tl_layout = GridLayout.rows(*
			[
				[
					nil,
					self.ruler_x,
					BasicButton.new.label_("z")
						.maxWidth_(10)
						.action_({
							self.zoom_fit;
						})
				],
				[
					if(self.disable_track_header != true) {track_header},
					StackLayout(
						self.cursor_timeline.view,
						self.timeline.selectionView,
						self.timeline.view,
					).mode_(1),
					self.verticalScroller.view,
				],
			] 
			++ if(self.bottomPanelView.notNil) {
				[
					[
						self.bottomPanelHeaderView,
						self.bottomPanelView,
						self.bottomPanelVerticalScroller,
						//self.velocityTimeline.view,
						//self.velocityVerticalRange.view,
					],
				]
			} { [] }
			++ if(self.disable_hrange != true) {
				[
					[
						nil,
						self.horizontalScroller.view,
						self.sizerButton,
					]
				]
			} { [] }  
		);
		tl_layout.spacing = 0;
		self.timelineGridLayout = tl_layout;

		layout = VLayout(*
			[
				self.make_header,
				tl_layout,
			] ++ 
			if(self.bottomPanelToolbar.notNil) { // hide if velocityView is disabled
				[
					self.bottomPanelToolbar,
					//HLayout(
						//self.velocityShowButton,
						//self.velocityPopUp,
						//self.automationButton,
						//self.velocityValue,
						//nil,
					//)
				]
			} {
				[]
			}
		);

		self.refresh; // map eventlist to timelines

		self.layout = layout;
		//self.zoom_fit;

		layout;
	},

	showTimelineContextMenu: { arg self, chosennode;
		var selected;
		selected = self.timeline.selectedNodes;
		Menu(
			* [
				if(chosennode.notNil) {
					[
						MenuAction("Edit", {
							self.showClipEditor(chosennode.model);
						}),
						MenuAction("Delete node", {
							self.deleteAction;
						}),
						MenuAction("Copy", {
							self.copyAction;
						}),
					]
				} {
					if(selected.notNil and: { selected.size > 0 }) {
						[
							MenuAction("Delete selected nodes", {
								self.deleteAction;
							}),
							MenuAction("Copy selected nodes", {
								self.copyAction;
							}),
						]
					} {
						[
							MenuAction.separator,
							MenuAction("Create node", {
								self.createNodeAction;
							}),
						]
					};
				},
				if(self.clipboard.notNil) {
					MenuAction("Paste", {
						self.pasteAction;
					})
				},
				MenuAction("Split", {
					self.splitAction;
				}),

				MenuAction.separator,
				MenuAction("Set start", {
					self.setStartAction;
				}),
				MenuAction("Set end", {
					self.setEndAction;
				}),

				MenuAction.separator,
				MenuAction("New score", {
					self.newScoreAction
				}),
				MenuAction("Clone score", {
					self.cloneScoreAction
				}),
				MenuAction("Duplicate score", {
					self.doubleScoreAction
				}),

				MenuAction.separator,
				MenuAction("Zoom fit", {
					self.zoomFitAction;
				}),

				MenuAction("Zoom full", {
					self.zoomFullAction;
				}),
				MenuAction.separator,
				MenuAction("Undo", {
					self.undoAction;
				}).enabled_(self.timeline_model.eventLoop.historyCanUndo),
				MenuAction("Redo", {
					self.redoAction;
				}).enabled_(self.timeline_model.eventLoop.historyCanRedo),

				MenuAction.separator,
				Param({ self.get_xquant }, { arg val; self.set_xquant(val) }, self.make_quant_spec).asMenu("Quant"),

			].flat.select(_.notNil)
		).front
	},

	////// gui actions

	saveAction: { arg self, view;
		var me = view ?? { () };
		if(self.hasSavePath) {
			var dialog;
			if(PathName(self.timeline_model.presetCompileStringSavePath).isFolder) {
				self.timeline_model.presetCompileStringSavePath = self.timeline_model.presetCompileStringSavePath +/+ self.timeline_model.key.asString ++ ".scd"
			};
			dialog = Window.new("Save").layout_(
				VLayout(
					StaticText.new.string_("Save this object to the file % ?".format(self.timeline_model.presetCompileStringSavePath.asCompileString)),
					HLayout(
						nil,
						BasicButton.new.string_("OK").action_({ 
							self.timeline_model.savePresetCompileString;
							me.refreshChange;
							dialog.close;
						}),
						BasicButton.new.string_("Choose").action_({ 
							self.timeline_model.savePresetCompileString(nil, {
								me.refreshChange;
							}, true);
							dialog.close;
						}),
						BasicButton.new.string_("Cancel").action_({
							dialog.close;
						}),
					)
				)
			).setInnerExtent(600,140).front;
		} {
			self.saveAsAction(me);
		};
	},

	saveAsAction: { arg self, view;
		var me = view ?? { () };
		self.timeline_model.savePresetCompileString(nil, {
			me.refreshChange;
		}, true)
	},

	undoAction: { arg self;
		var el = self.timeline_model.eventLoop;
		el.historyUndo;
		//self.timeline.refreshSelectionView;
	},

	redoAction: { arg self;
		var el = self.timeline_model.eventLoop;
		el.historyRedo;
		//self.timeline.refreshSelectionView;
	},
	

	add_track: { arg self;
		self.timeline_model.add_track;
		self.timeline.refreshEventList;
		self.timeline.refresh;
		self.update_track_header;
	},

	zoomFitAction: { arg self;
		// TODO: use timeline.spec to get initial maxy and miny
		var timeline = self.timeline;
		var maxy=0, miny=127;
		var range;

		if(timeline.model.size < 3) { // if empty
			// NOOP
		} {

			timeline.model.do { arg ev, x;
				var midi = ev.use { ev.midinote };
				//[x, maxy, miny, midi, ev].debug("zoom_fit: midi");
				if(midi.notNil) {
					//[x, midi].debug("zoom_fit, notnil");
					if(midi > maxy) {
						maxy = midi;
					};
					if(midi < miny) {
						miny = midi;
					};
				}
			};

			if(( maxy + 1 ) > timeline.areasize.y) {
				timeline.areasize.y = maxy + 1;
			};

			// add margin top and bottom
			miny = miny - 1;
			maxy = maxy + 1;

			//[timeline.viewport, timeline.viewport.top, maxy/timeline.areasize.y, timeline.viewport.height, (maxy-miny)/timeline.areasize.y].debug("ZOOM ZOOOOOOOOOOOOOOOOOOOOM viewport before");
			//[timeline.areasize].debug("areasize");
			range = (maxy-miny+1)/timeline.areasize.y;
			timeline.viewport.top = (miny/timeline.areasize.y);
			timeline.viewport.height = range.clip(0.1,1);
			//[timeline.viewport.top, timeline.viewport.height, maxy, miny, timeline.areasize.y].debug("top, height, maxy, miny, areasize");

			//timeline.viewport.top = 42/128;
			//timeline.viewport.height = ((54-42)/128);

			//timeline.viewport.debug("ZOOM ZOOOOOOOOOOOOOOOOOOOOM viewport");


			timeline.changed(\viewport);
			timeline.refresh;
		};


		//[self.areasize, self.timeline.areasize].debug("2");
		self.areasize = self.timeline.areasize;
		//[self.areasize, self.timeline.areasize].debug("4");
	},

	zoom_fit: { arg self;
		// deprecated name
		self.zoomFitAction;
	},

	copyAction_old: { arg self;
		var selnodes;
		var cursor = self.timeline_model.cursor;
		var startpos, endpos;
		selnodes = self.timeline.selNodes.as(Array).collect({ arg x; x.model });
		if(selnodes.size == 0) {
			startpos = cursor.startPosition ? self.eventList.startTime;
			endpos = cursor.endPosition ? self.eventList.endTime;
			self.timeline.nodes.collect({ arg node;
				var at = node.model[\absTime];
				if(at >= startpos and: { at < endpos and: { node.selectable }} ) {
					self.timeline.selectNode(node)
				}
			});
			selnodes = self.timeline.selNodes.as(Array).collect({ arg x; x.model });
		};
		if(selnodes.size > 0) {
			self.clipboard = selnodes;
			self.selection_rect = self.selection_cursor.rect;
		};
	},

	copyAction: { arg self;
		var selevents;
		var cursor = self.timeline_model.cursor;
		var startpos, endpos;
		selevents = self.timeline.copyAtSelectionEdges;
		Log(\Param).debug("clipTimeline.copyAction: selevents: %", selevents);
		if(selevents.size > 0) {
			self.clipboard = selevents;
			self.selection_rect = self.selection_cursor.rect;
		};
	},

	pasteAction: { arg self;
		if(self.clipboard.notNil) {
			var delta;
			var newnodes;
			var first = self.clipboard.as(Array).inject(nil, { arg a, b; 
				if(a.isNil) {
					b
				} {
					if(a[\absTime] < b[\absTime]) {
						a
					} {
						b
					}
				};
			});
			//first.debug("first");
			//delta = self.timeline.lastGridPos.x.round(self.timeline.quant.value.x) - first[\absTime];
			delta = self.timeline.lastGridPos.round(self.timeline.quant.value) - self.selection_rect.origin;
			//self.clipboard.debug("clipboard");
			self.timeline_model.addHistorySnapshot;
			self.clipboard.do({ arg event;
				var new;
				new = event.copy;
				//[new, delta].debug("new, delta");
				new[\absTime] = new[\absTime] + delta.x;
				new[self.timeline.valueKey] = new[self.timeline.valueKey] + delta.y;
				self.timeline.model.addEvent(new);
			});
			//self.timeline.model.changed(\refresh);

			self.selection_cursor.rect = self.selection_rect.moveBy(delta.x, delta.y);
			self.timeline.updatePreviousNormSelRect; // needed to move selection as nodes moves
			self.timeline.deselectAllNodes;
			//Log(\Param).debug("deselectNodes %".format(self.timeline.selectedNodes));
			newnodes = self.timeline.findNodes(self.selection_cursor.rect);
			//Log(\Param).debug("new nodes %".format(newnodes));
			self.timeline.selectNodes(newnodes);
			//self.timeline.model.changed(\refresh);
			self.timeline.model.reorder;
		}
	},


	deleteSelectedNodesHook: { arg self;
		{ arg tl;
			self.timeline_model.addHistorySnapshot;
			//self.timeline_model.eventLoop.cloneAndPushList;
			//Log(\Param).debug("hook qui foire");
			self.timeline.deleteSelectedNodes;
			//self.timeline_model.eventLoop.pushList;
		}
	},

	quantizeAction: { arg self;
		var selNodes;
		var snapshot;
		// TODO: should be implemented by adding a new property to the event, understood by XEventList
		//			to be able to undo it
		//quant.value.debug("quantize!!");
		//"quantize ouais c'est ici".debug;
		//snapshot = self.timeline_model.eventList.clone;
		//self.timeline_model.pushList;


		self.timeline_model.addHistorySnapshot;
		selNodes = self.timeline.selNodes;
		selNodes.do { arg node;
			//node.nodeloc.debug("before");
			node.nodeloc = node.nodeloc.round(self.quant_ref.value.x);
			//node.nodeloc.debug("after");
		};


		////// this works
		//snapshot = self.timeline_model.eventList.clone;
		//selNodes = self.timeline.selNodes;
		//selNodes.do { arg node;
		////node.nodeloc.debug("before");
		//node.nodeloc = node.nodeloc.round(self.quant_ref.value.x);
		////node.nodeloc.debug("after");
		//};
		//self.timeline_model.eventLoop.cloneAndPushList(snapshot);
		//self.timeline_model.eventLoop.addList;
	},

	setStartAction: { arg self;
		self.timeline.setStartPosition(self.timeline.lastGridPos.x.trunc(self.quant_ref.value.x));
		self.refresh;
	},

	setEndAction: { arg self;
		self.timeline.setEndPosition(self.timeline.lastGridPos.x.trunc(self.quant_ref.value.x));
		self.refresh;
	},

	newScoreAction: { arg self;
		self.eventLoop.pushList;
		self.timeline_model.addHistorySnapshot;
		self.eventList = self.timeline_model.makeDefaultEventList;
		self.eventLoop.pushList;
	},

	cloneScoreAction: { arg self;
		self.eventLoop.pushList;
		self.timeline_model.addHistorySnapshot;
		self.eventList = self.eventList.clone;
		self.eventLoop.pushList;
	},

	doubleScoreAction: { arg self;
		self.timeline_model.addHistorySnapshot;
		self.eventList = self.eventList.clone.double;
	},

	deleteAction: { arg self;
		self.deleteSelectedNodesHook.value;
	},

	createNodeAction: { arg self;
		self.timeline.createNode
	},

	zoomFullAction: { arg self;
		self.timeline.viewport = Rect(0,0,1,1);
		self.refresh;
	},


	////// bottom panel

	// automation 

	showAutomation: { arg self, prop;
		//var prop = self.automationPopup.items[self.automationPopup.value];
		var selectedParam = Param(self.timeline_model.param.target, prop);
		var ptline;
		var ptview;
		var rack = self.timeline_model.automationRack;
		self.selectedAutomationParam = selectedParam;
		//if(rack.getParamTimeline(selectedParam).isNil) {
			//// automatically enable automation if new automation is created
			//self.timeline_model.automationEnabled = true;
		//};
		self.timeline_model.automationRack.addParam(selectedParam);
		self.bottomPanelView.removeAll;
		ptline = rack.getParamTimeline(selectedParam);
		self.automationRackView = self.timeline_model.automationRack.getParamTimeline(selectedParam);

		ptview = ProtoTemplateDef(\ParamTimelinePanelView).newView(ptline);
		ptview.makeAutomationLayout;
		ptview.timeline.mimicTimeline(self.timeline, \horizontal);
		self.bottomPanelVerticalScroller.mapTimeline(ptview.timeline);
		self.bottomPanelHeaderView.removeAll;
		ptview.cursor_timeline.followChange(self.eventList, \cursor, { arg obj, msg, arg1;
			if(arg1 == \play) {
				//ptview.cursor_timeline.play; // already sent by loopMaster
			} {
				// this event is sent by timeline_model.stop function
				ptline.eventList.changed(\cursor, \stop);
				//ptview.cursor_timeline.stop;
			}
		});
		//if(self.timeline_model.isPlaying) {
		//ptview.cursor_timeline.play;
		//};
		self.bottomPanelSelectedTimelineView = ptview.timeline;
		self.bottomPanelHeaderView.layout = HLayout(
			ptview.track_header
		).margins_(0);

		self.bottomPanelView.layout = HLayout(ptview.timelineStack).margins_(0);
		self.isBottomPanelVisible = true;
		self.bottomPanelDescription.string = "Automation: %".format(prop);
		self.updateBottomPanel;
	},

	showAllAutomations: { arg self;
		var rack = self.timeline_model.automationRack;
		self.bottomPanelView.removeAll;
		self.bottomPanelView.layout = VLayout (
			rack.asStackPreview({ arg view;
				view.timeline.mimicTimeline(self.timeline, \horizontal)
		   	})
		).margins_(0);
		//self.bottomPanelView.fixedHeight_(40);
		self.isBottomPanelVisible = true;
		//self.timelineGridLayout.setRowStretch(1, 100);
		//self.timelineGridLayout.setRowStretch(2, 0);
		self.bottomPanelVerticalScroller.visible = false;
		self.bottomPanelHeaderView.removeAll;
	},

	makeAutomationMenu: { arg self;
		var rack = self.timeline_model.automationRack;
		Menu(
			*[
				if(rack.isEmpty.not) {
					[
						Param(Message(self), \isBottomPanelVisible, ParamBoolSpec()).asMenuAction("Show automation panel"),
						Param(Message(self.timeline_model), \automationEnabled, ParamBoolSpec()).asMenuAction("Enable automation"),
						MenuAction("Preview all automations", {
							self.showAllAutomations;
						}),
					]
				},

				if(self.selectedAutomationParam.notNil) {
					[
						MenuAction.separator,
						Menu(
							MenuAction("Edit", {
								rack.getParamTimeline(self.selectedAutomationParam).edit;
							}),
							MenuAction("Remove", {
								rack.removeParam(self.selectedAutomationParam)
							}),
						).title_("Current: %".format(self.selectedAutomationParam.propertyRoot)),
					]
				},

				MenuAction.separator,
				self.timelineInstrumentParamGroup.rejectByKey([\dur, \legato, \sustain]).selectByType(\scalar).asArray.collect { arg param, idx;
					var ptline = self.timeline_model.automationRack.getParamTimeline(param);

					MenuAction(param.propertyRoot, {
						self.showAutomation(param.property);
					}).checkable_(true).checked_(ptline.notNil)
				}
			].flat.reject(_.isNil)
		).front;
	},

	// velocity 

	showVelocityView: { arg self, prop;
		var keyspec;
		var height = 150;
		var param = Param(self.timeline_model.param.target, prop);
		self.velocityTimeline = VelocityTimelineView.new;
		//self.velocityTimeline.fixedHeight = height;
		self.velocityTimeline.mimicTimeline(self.timeline, \horizontal);
		self.velocityTimeline.mimicNodeSelection(self.timeline);
		//self.velocityTimeline.mapParam(Param(self.param.target, \velocity, ControlSpec(0,127,\lin,0,64)));
		self.velocityTimeline.quant = Point(0,0);
		self.velocityTimeline.forbidHorizontalNodeMove = true;

		self.bottomPanelVerticalScroller.orientation = \vertical;
		//self.bottomPanelVerticalScroller.fixedHeight = height;
		self.bottomPanelVerticalScroller.mapTimeline(self.velocityTimeline);

		self.bottomPanelSelectedTimelineView = self.velocityTimeline;
		self.isBottomPanelVisible = true;
		self.bottomPanelView.layout = HLayout(self.velocityTimeline).margins_(0);
		self.velocityTimeline.mapParam(param);
		self.updateBottomPanel;
		self.bottomPanelDescription.string = "Property: %".format(prop);
		self.refresh; // required for mapping EventList to velocityTimeline
	},

	makeVelocityMenu: { arg self;
		Menu(
			* self.timelineInstrumentParamGroup.collect({ arg x; 
				MenuAction(x.propertyRoot, {
					self.showVelocityView(x.propertyRoot)
				});
			})
		).front
	},

	makeVelocityView: { arg self; // deprecated by showVelocityView
		var keyspec;
		var height = 150;
		self.velocityTimeline = VelocityTimelineView.new;
		self.velocityTimeline.fixedHeight = height;
		self.velocityTimeline.mimicTimeline(self.timeline, \horizontal);
		self.velocityTimeline.mimicNodeSelection(self.timeline);
		//self.velocityTimeline.mapParam(Param(self.param.target, \velocity, ControlSpec(0,127,\lin,0,64)));
		self.velocityTimeline.quant = Point(0,0);
		self.velocityTimeline.forbidHorizontalNodeMove = true;

		self.velocityVerticalRange = TimelineScroller.new;
		self.velocityVerticalRange.orientation = \vertical;
		self.velocityVerticalRange.fixedHeight = height;
		self.velocityVerticalRange.mapTimeline(self.velocityTimeline);

		self.velocityValue = StaticText.new;
		self.velocityValue.fixedWidth = 50;
		self.velocityValue.font = Font.default.size_(10);
		//self.velocityValue.string = "XXX";

		self.refreshPosyValue = {
			var node;
			//"---------------heyyyyyyyyyyyyyyy".debug;
			//"heyyyyyyyyyyyyyyy".debug;
			//"heyyyyyyyyyyyyyyy".debug;
			node = self.velocityTimeline.chosennode;
			if(node.notNil and: { node.model.notNil }) {
				var val =   node.model[node.posyKey];
				if(val.notNil) {
					val = val.round(0.001)
				};
				self.velocityValue.string = val;
			}

		};
		self.velocityValue.followChange(self.velocityTimeline, \nodeMoved, {
			self.refreshPosyValue;
		});
		self.velocityValue.followChange(self.velocityTimeline, \selectedNodes, {
			self.refreshPosyValue;
		});

		self.velocityTimeline.visible = false; // not visible by default, FIXME: should be remembered
		self.velocityVerticalRange.visible = false;
		self.velocityShowButton = BasicButton.new.string_(">")
			.fixedWidth_(20)
			.action_({ arg me;
				if(self.velocityTimeline.visible) {
					me.string = ">";
					self.velocityTimeline.visible = false;
					self.velocityVerticalRange.visible = false;
				} {
					me.string = "^";
					self.velocityTimeline.visible = true;
					self.velocityVerticalRange.visible = true;
				}
			})
		;
		
		self.velocityPopUp = PopUpMenu.new;
		self.velocityPopUp.action_({ arg me;
			//"velocityPopUp.action".debug;
			//"velocityPopUp.actio3n".postln;
			self.velocityTimeline.mapParam(self.timelineInstrumentParamGroup[me.value]);
			//self.velocityTimeline.param.debug("new param!!!");
		}).items_(
			self.timelineInstrumentParamGroup.collect({ arg x; 
				x.property;
			})
		);
		if(self.timelineInstrumentParamGroup.size>0) {
			self.velocityPopUp.valueAction = 0;
		};
		//self.velocityPopUp.mapParam(Param(Message(self.velocityTimeline), \valueKey, keyspec));
	},
	// bottom panel 

	makeBottomPanelValueText: { arg self;
		self.bottomPanelValueText = StaticText.new;
		self.bottomPanelValueText.fixedWidth = 50;
		self.bottomPanelValueText.font = Font.default.size_(10);

		self.refreshBottomPanelValueText = {
			var node;
			//"---------------heyyyyyyyyyyyyyyy".debug;
			//"heyyyyyyyyyyyyyyy".debug;
			//"heyyyyyyyyyyyyyyy".debug;
			node = self.bottomPanelSelectedTimelineView.chosennode;
			if(node.notNil and: { node.model.notNil }) {
				var val =   node.model[node.posyKey];
				if(val.notNil) {
					val = val.round(0.001)
				};
				self.bottomPanelValueText.string = val;
			}
		};
	},

	updateBottomPanel: { arg self;
		var con;
		con = self.bottomPanelValueText.getHalo(\followChangeController);
		if(con.notNil) {
			con.remove;
		};
		self.bottomPanelValueText.followChange(self.bottomPanelSelectedTimelineView, \nodeMoved, {
			self.refreshBottomPanelValueText;
		});
		con = self.bottomPanelValueText.getHalo(\followChangeController);
		con.put(\selectedNodes, {
			self.refreshBottomPanelValueText;
		});
		self.bottomPanelValueText.string = "-";
		
	},

	makeBottomPanelToolbar: { arg self;
		self.bottomPanelToolbar = View.new;
		self.bottomPanelShowButton = BasicButton.new.string_(">")
			.fixedWidth_(20)
			.action_({ arg me;
				if(self.isBottomPanelVisible) {
					me.string = ">";
					self.isBottomPanelVisible = false;
				} {
					me.string = "^";
					self.isBottomPanelVisible = true;
				}
			})
		;
		self.bottomPanelDescription = StaticText.new;
		self.velocityButton = BasicButton.new.string_("Notes prop").fixedWidth_(80).action_({
			self.makeVelocityMenu;
		});
		self.automationButton = BasicButton.new.string_("Automate").fixedWidth_(80).action_({
			self.makeAutomationMenu;
		});
		self.makeBottomPanelValueText;
		self.bottomPanelToolbar.layout = HLayout (
			self.bottomPanelShowButton,
			//self.automationPopup,
			self.velocityButton,
			self.automationButton,
			self.bottomPanelDescription,
			self.bottomPanelValueText,
			nil,
		).margins_(0);
	},

	makeBottomPanelView: { arg self;
		self.bottomPanelView = View.new;
		self.makeAutomationView;
		self.bottomPanelHeaderView = View.new;
		self.bottomPanelVerticalScroller = TimelineScroller.new;
		self.bottomPanelVerticalScroller.orientation = \vertical;
		self.makeBottomPanelToolbar;
		self.isBottomPanelVisible = false;
	},

	isBottomPanelVisible_: { arg self, val;
		if(val == true) {
			self.bottomPanelView.visible = true;
			self.bottomPanelVerticalScroller.visible = true;
			self.bottomPanelHeaderView.visible = true;
		} {
			self.bottomPanelView.visible = false;
			self.bottomPanelVerticalScroller.visible = false;
			self.bottomPanelHeaderView.visible = false;
		};
	},
	isBottomPanelVisible: { arg self;
		self.bottomPanelView.visible;
	},

));

/////// derivatives

ProtoTemplateDef(\ClipTimelinePanelView, (
	parent: ProtoTemplateDef(\TimelinePanelView),

	new: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		self.timeline_model = { timeline };
		self.areasize = self.timeline_model.areasize;
		self.quant_ref = { arg self; self.timeline_model.quant_ref };
		
		self.makeWindow;
	
		self;
	},

	newView: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		self.timeline_model = {timeline};
		self.areasize = self.timeline_model.areasize;
		
		//self.makeWindow;
	
		self;
	},

	cursor: { arg self;
		self.timeline_model.cursor;
	},

	selection_cursor: { arg self;
		self.timeline_model.selection_cursor;
	},

	key: { arg self;
		self.timeline_model.key;
	},

	param: { arg self;
		self.timeline_model.param;
	},

	makeBottomPanelView: { },

	make_header: { arg self;
		//var mute_button = Button.new.states_([
			//[ "Mute", Color.black, Color.white ],
			//[ "Unmute", Color.black, ParamViewToolBox.color_ligth ],
		//]).followChange(self.track_selector, \selected_index, { arg view;
			//view.value = if(self.timeline_model.is_track_bypassed(self.track_selector.selected_index)) {
				//1
			//} {
				//0
			//}
		//}).action_({
			//self.timeline_model.toggle_bypass_track(self.track_selector.selected_index);
			//self.track_selector.viewInit.(self.track_selector.selected_view, self.track_selector.selected_index);
		//});

		Log(\Param).debug("recorder %", self.timeline_model.recorder);
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			//self.make_saveas_button,
			BasicButton.new.label_("Add track").action_({
				self.add_track;
			}),
			//mute_button, // useless with "M" button
			self.make_quant_popup,
			self.make_undo_system_buttons,
			//NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			//Param(Message(self.timeline_model), \isRecording, XBoolSpec()).asButton("Rec"),
			RecordButton(self.timeline_model.recorder, "Rec"),
			//Param(Message(self.timeline_model), \bindPlayWithRecord, XBoolSpec()).asButton("PlayRec"),
			//BasicButton.new.label_("Samples").action_({
				//WindowDef("%_SampleManager".format(self.timeline_model.key).asSymbol, WindowDef(\ClipTimeline_SampleManager)).front(self.timeline_model);
			//}),
			BasicButton.new.string_("...").mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				//WindowDef(\ClipTimelineOverlayMenu).front(self, view, x, y)
				WindowDef(\ClipTimelineContextMenu).sourceValue(self, view, x, y).front
			}).maxWidth_(20),
		);
		
		self.header_layout;
	},


	timelineClass: { arg self;
		ClipTimelineView
		//PdefTimelineView
	},

	showClipEditor: { arg self, ev;
		//ProtoTemplateDef(\ClipEditor).new(model ? self.model, self.timeline_model.eventList);
		WindowDef(\ClipTimeline_ClipEditor).front(ev, self.timeline_model.eventList);
	},

	makeLayout: { arg self;
		var clipEditor;
		//"clipTimelinePanelView.makeLayout: start init".debug;

		self[\makeVelocityView] = {}; // disable velocity view
		self.layout = VLayout(
			HLayout (
				*[
					self.makeLeftPaneLayout,
					VLayout (
						[ProtoTemplateDef(\TimelinePanelView)[\makeLayout].(self), stretch:1],
					),
					self.makeRightPaneLayout,
				].select(_.notNil)
			),
		);
		//"clipTimelinePanelView.makeLayout: part 2".debug;
		self.timeline.followChange(self.timeline_model, \eventFactory, {
			Log(\Param).debug("eventFactory change!!!!");
			self.timeline.eventFactory = self.timeline_model.eventFactory;
		});
		self.timeline.mouseDownAction = { arg me, px, py, mod, buttonNumber, clickCount, chosennode;
			if(clickCount == 2) {
				if(chosennode.notNil) {
					self.showClipEditor(chosennode.model);
				}
			} {
				if(buttonNumber == 1 and: { mod.isCtrl.not }) {
					self.showTimelineContextMenu(chosennode);
				};
			}
		};
		//"clipTimelinePanelView.makeLayout: part 3".debug;
		self.timeline.view.followChange(self.eventloop, \list, { 
			{
				self.refresh;
			}.defer
		});
		//"clipTimelinePanelView.makeLayout: end init".debug;
		self.timeline.keyDownAction = self.getTimelineKeyDownAction;
		self.timeline.deleteSelectedNodesHook = self.deleteSelectedNodesHook;

		self.layout;
	},

	copyAction: { arg self;
		var selevents;
		var cursor = self.timeline_model.cursor;
		var startpos, endpos;
		selevents = self.timeline.copyAtSelectionEdges;
		Log(\Param).debug("clipTimeline.copyAction: selevents: %", selevents);
		if(selevents.size > 0) {
			self.clipboard = selevents;
			self.selection_rect = self.selection_cursor.rect;
		};
	},

	clipboard: { arg self;
		ParamViewToolBox.clipboard.clipTimeline
	},

	clipboard_: { arg self, val;
		ParamViewToolBox.clipboard.clipTimeline = val;
	},

	selection_rect: { arg self;
		//ParamViewToolBox.clipboard.clipTimeline_selectionRect
		ParamViewToolBox.clipboard.clipTimeline_selectionRect ?? { Rect(0,0,1,1) };
	},

	selection_rect_: { arg self, val;
		ParamViewToolBox.clipboard.clipTimeline_selectionRect = val;
	},

	pasteAction: { arg self;
		if(self.clipboard.notNil) {
			var delta;
			var newnodes;
			var first = self.clipboard.as(Array).inject(nil, { arg a, b; 
				if(a.isNil) {
					b
				} {
					if(a[\absTime] < b[\absTime]) {
						a
					} {
						b
					}
				};
			});
			//first.debug("first");
			//delta = self.timeline.lastGridPos.x.round(self.timeline.quant.value.x) - first[\absTime];
			delta = self.timeline.lastGridPos.round(self.timeline.quant.value) - self.selection_rect.origin;
			//~clipTimeline_clipboard.debug("clipboard");
			self.timeline_model.addHistorySnapshot;
			self.clipboard.do({ arg event;
				var new;
				new = event.copy;
				//[new, delta].debug("new, delta");
				new[\absTime] = new[\absTime] + delta.x;
				new[self.timeline.valueKey] = new[self.timeline.valueKey] + delta.y;
				self.timeline.model.addEvent(new);
			});
			self.selection_cursor.rect = self.selection_rect.moveBy(delta.x, delta.y);
			self.timeline.updatePreviousNormSelRect; // needed to move selection as nodes moves
			self.timeline.deselectAllNodes;
			//Log(\Param).debug("deselectNodes %".format(self.timeline.selectedNodes));
			newnodes = self.timeline.findNodes(self.selection_cursor.rect);
			//Log(\Param).debug("new nodes %".format(newnodes));
			self.timeline.selectNodes(newnodes);
			//self.timeline.model.changed(\refresh);
			self.timeline.model.reorder;
		}
	},

	splitAction: { arg self;
		self.timeline_model.addHistorySnapshot;
		self.timeline.splitAtSelectionEdges;
	},

	makeRightPaneLayout: { arg self;
		self.leftPaneView = View.new.layout_(WindowDef(\ClipTimelineClipList).asView(self.timeline_model.clipList)).maxWidth_(200);
		self.leftPaneView;
	},

	makeRightPaneToggle: { arg self;
		BasicButton.new.string_(">")
		.fixedWidth_(20)
		.action_({ arg me;
			if(self.leftPaneView.visible) {
				me.string = ">";
				self.leftPaneView.visible = false;
			} {
				me.string = "^";
				self.leftPaneView.visible = true;
			}
		})
		;
	};
));

ProtoTemplateDef(\TrackTimelinePanelView, (
	parent: ProtoTemplateDef(\ClipTimelinePanelView),

	new: { arg self, timeline;
		//"trackTimelinePanelView: start init".debug;
		self = ProtoTemplateDef(\ClipTimelinePanelView)[\new].(self, timeline);
		//"trackTimelinePanelView: init part1".debug;
	
		self.timeline_model = { timeline };
		//"trackTimelinePanelView: init part2".debug;
		self.areasize = self.timeline_model.areasize;
		//"trackTimelinePanelView: init part3".debug;
		
		self.makeWindow;
	
		//"trackTimelinePanelView: end init";
		self;
	},


	newView: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		self.timeline_model = {timeline};
		self.areasize = self.timeline_model.areasize;
		
		//self.makeWindow;
	
		self;
	},

	clipboard: { arg self;
		ParamViewToolBox.clipboard.trackTimeline
	},

	clipboard_: { arg self, val;
		ParamViewToolBox.clipboard.trackTimeline = val;
	},

	selection_rect: { arg self;
		//ParamViewToolBox.clipboard.trackTimeline_selectionRect
		ParamViewToolBox.clipboard.trackTimeline_selectionRect ?? { Rect(0,0,1,1) };
	},

	selection_rect_: { arg self, val;
		ParamViewToolBox.clipboard.trackTimeline_selectionRect = val;
	},

	makeBottomPanelView: { },


	update_track_header: { arg self;
		var track_count;
		var track_header_block;
		//"trackTimelinePanelView.update_track_header: init".debug;
		self.areasize.y = self.timeline_model.tracks.size;
		track_count = self.areasize.y.asInteger;
		track_header_block = { arg x;
			var res = View.new.layout_(
				HLayout(
					//XSimpleButton.new.label_("track %".format(x)).
					self.track_selector.views[track_count - 1 - x]
				)
			);
			//res.background_(Color.blue);
			res;
		};
		//"trackTimelinePanelView.update_track_header: part 1".debug;
		self.track_selector = ProtoTemplateDef(\ViewSelector).new(track_count, {}, { arg view, x; 
			//x = track_count - x;
			if(self.timeline_model.is_track_bypassed(x)) {
				view.label_("[%]".format(self.timeline_model.tracks[x].label));
			} {
				view.label_("%".format(self.timeline_model.tracks[x].label));
			}
		});
		//"trackTimelinePanelView.update_track_header: part 2".debug;
		self.track_header.canvas_(
			self.track_header_view = View.new.layout_(
				VLayout(*
					track_count.collect { arg x;
						[
							track_header_block.(x),
							align: \top,
						]
					}
				).spacing_(0).margins_(0)
			);
			self.track_header_view
		).maxWidth_(100);
		//"trackTimelinePanelView.update_track_header: part 3".debug;
		self.update_track_header_size;
		self.track_header.onResize = { self.update_track_header_size };
		if(self.track_header_controller.notNil) {
			self.track_header_controller.remove;
		};
		self.track_header_controller = SimpleController(self.timeline)
		.put(\viewport, {
			//"update_track_header_size: viewport".debug;
			//TODO: remove controller
			if(self.track_header.isNil or: {self.track_header.isClosed}) {
				self.track_header_controller.remove;
			} {
				self.update_track_header_size;
			}

		})
		.put(\areasize, {
			//"update_track_header_size: areasize".debug;
			if(self.track_header.isNil or: {self.track_header.isClosed}) {
				self.track_header_controller.remove;
			} {
				self.update_track_header_size;
			}
			//self.update_track_header;
		})
		;
		//"trackTimelinePanelView.update_track_header: part 4".debug;
	},
));

ProtoTemplateDef(\ParamTimelinePanelView, (
	parent: ProtoTemplateDef(\TimelinePanelView),

	new: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		// self.timeline is timelineView actually
		self.timeline_model = timeline;

		self.makeWindow;
		
		self;
	},

	newView: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		// self.timeline is timelineView actually
		self.timeline_model = timeline;

		self;
	},

	cursor: { arg self;
		self.timeline_model.cursor;
	},

	selection_cursor: { arg self;
		self.timeline_model.selection_cursor;
	},

	key: { arg self;
		self.timeline_model.key;
	},

	param: { arg self;
		self.timeline_model.param;
	},

	levelParam: { arg self;
		self.timeline_model.levelParam;
	},

	timelineClass: { arg self;
		TimelineEnvView;
	},

	make_track_header: { arg self, timeline;
		//self.track_header = View.new;
		self.ruler_y = ParamTimelineRulerView.new(timeline);
		self.track_header = self.ruler_y;
		self.ruler_y.mimicTimeline(timeline);
		self.ruler_y.view.minWidth_(50);
		self.track_header;
	},

	clipboard: { arg self;
		ParamViewToolBox.clipboard.paramTimeline
	},

	clipboard_: { arg self, val;
		ParamViewToolBox.clipboard.paramTimeline = val;
	},

	selection_rect: { arg self;
		//ParamViewToolBox.clipboard.paramTimeline_selectionRect
		ParamViewToolBox.clipboard.paramTimeline_selectionRect ?? { Rect(0,0,1,1) };
	},

	selection_rect_: { arg self, val;
		ParamViewToolBox.clipboard.paramTimeline_selectionRect = val;
	},

	makeBottomPanelView: { },

	make_header: { arg self;
		self.paramKnob = Knob.new.maxHeight_(25);
		self.paramBusModeButton = BoolButton.new.string_("Bus mode");
		self.targetPlayerView = PlayerWrapperView.new;
		HLayout(
			PlayerWrapperView(self.timeline_model).view,
			//if(self.param.target.isKindOf(Bus) or: { self.param.target.isKindOf(Message) }) {
				//nil;
			//} {
				//PlayerWrapperView(PlayerWrapper(self.param.target)).view;
			//},
			self.targetPlayerView,
			self.param.asStaticTextLabel.attachContextMenu,
			self.paramKnob,
			//Param(Message(self.param), \inBusMode, XBoolSpec()).asButton("Bus mode old"),
			self.paramBusModeButton,
			self.make_repeatCount_box,
			self.make_quant_popup,
			self.make_undo_system_buttons,
			//NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			RecordButton(self.timeline_model.recorder, "Rec"),
			PlayerWrapper(Pdef(\metronome)).asView, // TODO: make a good one and define it a init
			//Button.new.mapParam(Param(Message(self.timeline_model), \isRecording, XBoolSpec())).states_([
				//["Rec", Color.black, Color.white],
				//["Rec", Color.black, Color.red],
			//]),
			BasicButton.new.string_("...").mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				var model = self.timeline_model;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				Menu(
					* [
						MenuAction("Edit param", { 
							WindowDef(\ParamEditor).front(self.param)
						}),
						MenuAction("Select param", {
							WindowDef(\ParamSelectDialog).front(nil, { arg param;
								self.timeline_model.param = param;
							})
						}),
						MenuAction.separator,
						if(model.hasSavePath) {
							MenuAction("Save", {
								self.saveAction
							})
						},
						MenuAction("Save as...", {
							self.saveAsAction;
						}),
						MenuAction("Copy refString to clipboard", { 
							model.refCompileString.pbcopy;
						}),
					].select(_.notNil)
				).front;

				//WindowDef(\TimelineOverlayMenu).front(self.timeline_model, view, x, y)
			}).maxWidth_(20)
		)
	},

	makeLayout: { arg self;
		self[\makeVelocityView] = {}; // disable velocity view
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
			self.parent[\makeLayout].(self),
			//self.parent.makeLayout,
		);
		self.timeline.eventFactory = { arg pos, nodesize;
			(absTime: pos.x, level: pos.y, legato:1);
		};
		//self.timeline.mapParam(self.levelParam);
		self.timeline.view.followChange(self.timeline_model, \param, { 
			self.timeline.mapParam(self.levelParam);
			self.ruler_y.refresh;
			self.paramKnob.mapParam(self.param);
			self.paramBusModeButton.mapParam(Param(Message(self.param), \inBusMode, XBoolSpec()));
			if(self.param.target.isKindOf(Bus) or: { self.param.target.isKindOf(Message) }) {
				self.targetPlayerView.model = nil;
				self.targetPlayerView.view.visible = false;
			} {
				self.targetPlayerView.view.visible = true;
				self.targetPlayerView.model = PlayerWrapper(self.param.target);
				self.targetPlayerView.rightClickEditorEnabled = true;
			};
		});
		self.set_yquant(self.param.spec.step);
		// dopn
		//self.timeline.mouseUpAction = {|me, px, py, mod, buttonNumber, clickCount, chosennode|
			//self.timeline.refreshEventList;
		//};
		self.timeline.view.followChange(self.eventloop, \list, { 
			self.refresh;
		});
		self.timeline.keyDownAction = self.getTimelineKeyDownAction;
		self.timeline.deleteSelectedNodesHook = self.deleteSelectedNodesHook;

		self.layout;
	},

	makeAutomationLayout: { arg self;
		var timeline;
		self.save_timeline_properties;
		timeline = self.makeTimelineView;
		self.timeline = timeline;
		self.load_timeline_properties;
		self.initTimelineView;
		self.makeVerticalScroller;
		self.timeline.eventFactory = { arg pos, nodesize;
			(absTime: pos.x, level: pos.y, legato:1);
		};
		self.make_track_header(self.timeline);
		self.timeline.view.followChange(self.timeline_model, \param, { 
			self.timeline.mapParam(self.levelParam);
			self.ruler_y.refresh;
		});
		self.set_yquant(self.param.spec.step);

		self.makeCursorTimeline; // should be before refresh to mapEventList

		self.timeline.view.followChange(self.eventloop, \list, { 
			self.refresh;
		});


		self.timelineStack = StackLayout(
			self.cursor_timeline.view,
			self.timeline.selectionView,
			self.timeline.view,
		).mode_(1)
	},
));
ProtoTemplateDef(\EnvTimelinePanelView, ProtoTemplateDef(\ParamTimelinePanelView)); // compat


ProtoTemplateDef(\NoteTimelinePanelView, (
	parent: ProtoTemplateDef(\TimelinePanelView),

	new: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
		self.quant_ref = { arg self; self.timeline_model.quant_ref };
		self.makeWindow;
	
		self;
	},

	newView: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
	
		self;
	},

	node_move_multiplier_y: 12,

	cursor: { arg self;
		self.timeline_model.cursor;
	},

	selection_cursor: { arg self;
		self.timeline_model.selection_cursor;
	},


	key: { arg self;
		self.timeline_model.key;
	},

	param: { arg self;
		self.timeline_model.param;
	},

	clipboard: { arg self;
		ParamViewToolBox.clipboard.noteTimeline
	},

	clipboard_: { arg self, val;
		ParamViewToolBox.clipboard.noteTimeline = val;
	},

	selection_rect: { arg self;
		//ParamViewToolBox.clipboard.noteTimeline_selectionRect
		ParamViewToolBox.clipboard.noteTimeline_selectionRect ?? { Rect(0,0,1,1) };
	},

	selection_rect_: { arg self, val;
		ParamViewToolBox.clipboard.noteTimeline_selectionRect = val;
	},


	make_header: { arg self;
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			//self.make_saveas_button,
			self.make_repeatCount_box,
			self.make_quant_popup,
			self.make_undo_system_buttons,
			//NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			RecordButton( self.timeline_model.recorder, "Rec" ),
			PlayerWrapper(Pdef(\metronome)).asView, // TODO: make a good one and define it a init
			BasicButton.new.string_("...").mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				var model = self.timeline_model;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				Menu(
					* [
						MenuAction("Edit", { 
							PlayerWrapper(model.param.target).edit;
						}),
						MenuAction("Select source", { 
							WindowDef(\GlobalLibrary_select).front(model.param.target, { arg libid;
								var pdef = libid.last.value;
								model.param = Param(pdef, \midinote);
							}, \Pdef)
						}),
						Param(
							Message(model),
							\outBus, 
							TagSpecDef(\BusDef),
							//MenuSpec([\default -> nil, \0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k) }) )
						).asMenu("outBus"),
						MenuAction("Piano keyboard", { 
							//model.debug("m");
							//model.target.debug("t");
							WindowDef(\TimelinePianoKeyboard).front(model.wrapper)
						}),
						Param(
							Message(model),
							\controller, 
							ControllerDef.getTagSpec(\piano),
							//TagSpecDef(\NoteTimeline_controller),
						).asMenu("Controller"),
						MenuAction.separator,
						if(model.hasSavePath) {
							MenuAction("Save", {
								self.saveAction
							})
						},
						MenuAction("Save as...", {
							self.saveAsAction;
						}),
						MenuAction("Copy refString to clipboard", { 
							model.refCompileString.pbcopy;
						}),
					].select(_.notNil)
				).front;

				//WindowDef(\TimelineOverlayMenu).front(self.timeline_model, view, x, y)
			}).maxWidth_(20)
		);
		
		self.header_layout;
	},


	timelineClass: { arg self;
		MidinoteTimelineView;
	},

	make_track_header: { arg self, timeline;
		//self.track_header = View.new;
		var oldnote;
		self.ruler_y = MidinoteTimelineRulerView.new;
		self.track_header = self.ruler_y;
		self.ruler_y.mimicTimeline(timeline);
		self.ruler_y.view.minWidth_(50);
		self.ruler_y.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			var noteidx = self.ruler_y.pixelPointToGridPoint(Point(x,y)).y.asInteger;
			[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

			Log(\Param).debug("noteidx %", noteidx);
			if(oldnote.notNil) {
				self.timeline_model.wrapper.elAt(oldnote).stop;
			};
			oldnote = noteidx;
			self.timeline_model.wrapper.elAt(noteidx).play;
		});
		self.ruler_y.mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			var noteidx = self.ruler_y.pixelPointToGridPoint(Point(x,y)).y.asInteger;
			[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseUpAction");
			self.timeline_model.wrapper.elAt(oldnote ? noteidx).stop;
		});
	},

	makeLayout: { arg self;
		//"noteTimelinePanelView.makeLayout".debug;
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(EventPlayerWrapper(self.timeline_model)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
		//"noteTimelinePanelView.makeLayout bef".debug;
			self.parent[\makeLayout].(self),
			//self.parent.makeLayout,
		);
		//"noteTimelinePanelView.makeLayout af".debug;
		self.timeline.view.followChange(self.eventloop, \list, { 
			self.refresh;
		});
		// not needed because of default event factory of TimelineView
		//if(self.timeline_model.eventFactory.notNil) {
		//	self.timeline.eventFactory = { arg pos, nodesize;
		//		self.timeline_model.eventFactory.get(
		//			(absTime: pos.x, midinote: pos.y, sustain:nodesize)
		//		)
		//	};
		//}
		self.layout;
	},
));

ProtoTemplateDef(\KitTimelinePanelView, (
	parent: ProtoTemplateDef(\TimelinePanelView),

	new: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
		self.quant_ref = { arg self; self.timeline_model.quant_ref };
		self.makeWindow;
	
		self;
	},

	newView: { arg self, timeline;
		self = ProtoTemplateDef(\TimelinePanelView)[\new].(self, timeline.eventloop);
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
	
		self;
	},

	cursor: { arg self;
		self.timeline_model.cursor;
	},

	selection_cursor: { arg self;
		self.timeline_model.selection_cursor;
	},

	key: { arg self;
		self.timeline_model.key;
	},

	param: { arg self;
		self.timeline_model.param;
	},

	kit: { arg self;
		self.timeline_model.kit;
	},

	makeTimelineView: { arg self;
		self.timelineClass.new(\kitIndex).viewport_(Rect(0,0,1,1/4))
	},

	clipboard: { arg self;
		ParamViewToolBox.clipboard.kitTimeline
	},

	clipboard_: { arg self, val;
		ParamViewToolBox.clipboard.kitTimeline = val;
	},

	selection_rect: { arg self;
		//ParamViewToolBox.clipboard.kitTimeline_selectionRect
		ParamViewToolBox.clipboard.kitTimeline_selectionRect ?? { Rect(0,0,1,1) };
	},

	selection_rect_: { arg self, val;
		ParamViewToolBox.clipboard.kitTimeline_selectionRect = val;
	},

	make_header: { arg self;
		self.recordButton = RecordButton(self.timeline_model.recorder, "Rec");
		self.recordButton.followChange(self.timeline_model, \kit, {
			self.recordButton.model = self.timeline_model.recorder;
		});
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			//self.make_saveas_button,
			self.make_repeatCount_box,
			self.make_quant_popup,
			self.make_undo_system_buttons,
			//NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			self.recordButton,
			PlayerWrapper(Pdef(\metronome)).asView, // TODO: make a good one and define it a init
			//Button.new.mapParam(Param(Message(self.timeline_model), \isRecording, XBoolSpec())).states_([
				//["Rec", Color.black, Color.white],
				//["Rec", Color.black, Color.red],
			//]),
			BasicButton.new.string_("Edit kit").action_({ 
				WindowDef(\PatKitDefEditor).front(self.timeline_model.wrapper)
		   	}),
			BasicButton.new.string_("...").fixedWidth_(20).action_({ 
				var model = self.timeline_model;
				Menu(
					*[
						Param(
							Message(model),
							\outBus, 
							TagSpecDef(\BusDef),
							//MenuSpec([\default -> nil, \0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k) }) )
						).asMenu("outBus"),
						MenuAction("Select kit", {
							WindowDef(\GlobalLibrary_select).front(self.wrapper, { arg libid;
								model.wrapper = libid.last.value;
							}, \Kit)
						}),
						MenuAction("Use new kit", {
							WindowDef(\RenameDialog).front("Create new kit and use it. Choose a symbol name", model.key, { 
								arg val;
								var pkd= PatKitDef(val);
								model.kit = pkd;
								pkd.edit;
							})
						}),
						Param(
							Message(model),
							\controller, 
							ControllerDef.getTagSpec(\drumpad),
							//TagSpecDef(\NoteTimeline_controller),
						).asMenu("Controller"),
						MenuAction.separator,
						if(model.hasSavePath) {
							MenuAction("Save", {
								self.saveAction
							})
						},
						MenuAction("Save as...", {
							self.saveAsAction;
						}),
						MenuAction("Copy refString to clipboard", { 
							model.refCompileString.pbcopy;
						}),
					].select(_.notNil)
				).front;
				//WindowDef(\PatKitDefEditor).front(self.wrapper)
		   	}),
		);
		
		self.header_layout;
	},


	timelineClass: { arg self;
		KitTimelineView;
	},

	make_track_header: { arg self, timeline;
		var oldnote;
		//self.track_header = View.new;
		self.ruler_y = KitTimelineRulerView.new;
		self.track_header = self.ruler_y;
		self.ruler_y.mimicTimeline(timeline);
		self.ruler_y.view.minWidth_(50);
		self.ruler_y.mapWrapper(self.timeline_model.wrapper);
		self.ruler_y.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			var noteidx = self.ruler_y.pixelPointToGridPoint(Point(x,y)).y.asInteger;
			[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

			Log(\Param).debug("noteidx %", noteidx);
			if(oldnote.notNil) {
				self.timeline_model.wrapper.elAt(oldnote).stop;
			};
			oldnote = noteidx;
			self.timeline_model.wrapper.elAt(noteidx).play;
		});
		self.ruler_y.mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			var noteidx = self.ruler_y.pixelPointToGridPoint(Point(x,y)).y.asInteger;
			[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseUpAction");
			self.timeline_model.wrapper.elAt(oldnote ? noteidx).stop;
		});
		self.ruler_y.followChange(self.timeline_model, \kit, {
			self.ruler_y.mapWrapper(self.timeline_model.wrapper);
			self.ruler_y.refresh;
		});
	},

	timelineInstrumentParamGroup: { arg self;
		var target;
		if(self.kit.isNil) {
			ParamGroup()
		} {
			var set, group;
			var kit = self.kit;
			set = Set.new;
			if(kit.notNil) {

				kit.reject(_.isNil).do({ arg pat; 
					pat.asParamGroup.do({ arg param;
						if(param.type == \scalar) {
							Log(\Param).debug("param %", param);
							set.add(param.property)
						}
					})
				});
				target = kit.reject(_.isNil).first;	
				Log(\Param).debug("target %", target);
			} {
				Log(\Param).error("kit is nil for this timeline %", self.timeline_model);
			};
			group = set.as(List).collect({ arg key;
				Param(target, key)
			});
			ParamGroup(group);
		}
	},


	makeLayout: { arg self;
		//"noteTimelinePanelView.makeLayout".debug;
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(EventPlayerWrapper(self.timeline_model)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
		//"noteTimelinePanelView.makeLayout bef".debug;
			self.parent[\makeLayout].(self),
			//self.parent.makeLayout,
		);
		//"noteTimelinePanelView.makeLayout af".debug;
		self.timeline.view.followChange(self.eventloop, \list, { 
			self.refresh;
		});

		if(self.timeline_model.wrapper.notNil) {
			self.timeline.eventFactory = { arg pos, nodesize;
				(absTime: pos.x, kitIndex: pos.y, sustain:nodesize)
			};
		};
		self.layout;
	},
));


////////////////// clip editor 


WindowDef(\ClipTimeline_ClipEditor, { arg def, clip, eventlist;
	var lib = ProtoDef(\GlobalLibrary);
	var libid = lib.eventToLibId(clip);
	var target = lib.libIdToTarget(libid);
	var wrapper = lib.libIdToWrapper(libid);
	var params;
	var refresh;
	params = [
		Param(Message(clip), \label, ParamStringSpec()),
		Param(clip, Pembed.startOffsetKey, ControlSpec(0,100,\lin)),
		Param(clip, \gain, \gain.asSpec ?? { ControlSpec(0,1,\lin, 0,1)}),
		Param(clip, \outBus, TagSpecDef(\BusDefDefault)),
		Param(clip, \absTime),
		Param(clip, Pembed.sustainKey),
	];
	refresh = {
		debug("refresh from ClipTimeline_ClipEditor");
		eventlist.changed(\refresh);
	};
	VLayout (
		HLayout (
			StaticText.new.string_("type:"),
			TextField.new.string_(clip.type),
		),
		HLayout (
			StaticText.new.string_("libid:"),
			TextField.new.string_(libid.asCompileString),
		),
		HLayout (
			StaticText.new.string_("Target:"),
			TextField.new.string_(target.asCompileString),
			//TextField.new.string_(PlayerWrapper(target).refCompileString),
			BasicButton.new.string_("Select target").action_({
				WindowDef(\GlobalLibrary_select).front(target, { arg playerid;
					//playerid.debug("playerid");
					wrapper.redefineEvent(clip, playerid.last.value);
					def.front(clip, eventlist);
					//clip.debug("new clip");
				}, \Player)
			}),
		),
		BasicButton.new.string_("Edit target").action_({
			PlayerWrapper(target).edit
		}),
		* params.collect({ arg param;
			//param.asView
			HLayout(
				StaticText.new.string_(param.property).minWidth_(110),
				if(param.spec.isKindOf(TagSpec)) {
					param.asPopUpMenu;
				} {
					TextField.new.mapParam(param).addAction({
						refresh.();
					});
				},
			);
		}) ++ [nil]
	)
});


/////////////////////// cliplist side panel

WindowDef(\ClipTimelineClipList, { arg def, model;
	var addButton, newButton;
	addButton = BasicButton.new.string_("Add").action_({ arg view;
		WindowDef(\ClipList_AddClip).front({ arg idef, libId, label, clickCount;
			if(clickCount == \double) {
				Log(\Param).debug("doubleclick! libid:%, label:%", libId, label);
				
				if(libId.notNil) {
					model.addAndSelectClipId(libId);
				} {
					"no name".debug;
				}
			}

		}, { arg idef, libId, label;
			Log(\Param).debug("OK! libid:%, label:%", libId, label);

			if(libId.notNil) {
				model.addAndSelectClipId(libId);
				idef.closeWindow;
			} {
				"no name".debug;
			}
		});
		//model.gatherClips; // now in menu
	});
	newButton = BasicButton.new.string_("New").action_({ arg view;
		WindowDef(\ClipList_NewClip).front({}, { arg idef, factory, label;
			Log(\Param).debug("OK! fac:%, label:%", factory, label);

			if(factory.notNil) {
				//var score = ProtoTemplateDef(\TrackInstrument).new(libId, label).makeIndependentScore;
				//var scorelibid = ProtoDef(\GlobalLibrary).targetToLibId(score);
				var score = factory.value.value(label);
				var scorelibid = [factory.key, (label -> score)];
				Log(\Param).debug("************** addClip id %", scorelibid);
				model.addClipIdToClipList(scorelibid);
				idef.closeWindow;
			} {
				"no name".debug;
			}
		});
		//model.gatherClips; // commented for debug
	});
	VLayout (
		HLayout (
			addButton,
			newButton,
		),
		ListView.new.action_({arg view;
			Log(\Param).debug("action! %", view.value);
			model.setBrushToClipId(model.clipList[view.value]);
		}).enterKeyAction_({arg view;
			Log(\Param).debug("action! %", view.value);
			model.setBrushToClipId(model.clipList[view.value]);
		}).followChange(model, \clipList, { arg view;
			view.items_(model.clipList.collect({ arg clipId;
				Log(\Param).debug("clipList: add to listview: %", clipId);
				model.clipIdLabel(clipId)
			}));
			// since selected item is deleted, need to reselect
			if(view.items.size > 0) {
				model.setBrushToClipId(model.clipList[view.value]);
			};
		}).followChange(model, \brushClipId, { arg view;
			view.value = model.clipList.detectIndex({ arg clipId;
				Log(\Param).debug("clipList: change brushClipId %", model.brushClipId);
				clipId == model.brushClipId;
			})
		}).followChange(FileSystemProject.current, \tracklist_selected_track, { 
			//"ClipTimelineClipList: change!!!".debug;
			FileSystemProject.current.notNil and: {
				FileSystemProject.current.tracklist_selected_track.notNil and: {
					model.addAndSelectClipId(FileSystemProject.current.tracklist_selected_track)
				}
			}
		}).mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[x, y, modifiers, buttonNumber, clickCount].debug("xyyyyy");
			if(buttonNumber == 1) { // right click
				WindowDef(\ClipList_ContextMenu).sourceValue(model, view, model.clipList[view.value ? 0]).front;
			};
		}),
		HLayout (
			StaticText.new.followChange(model, \brushClipId, { arg view;
				var clipId = model.brushClipId;
				view.string_(
					model.clipIdLabel(clipId)
				)
			}),
			BasicButton.new.string_("Edit").action_({
				PlayerWrapper(model.clipIdToTarget(model.brushClipId)).edit;
			})
		)
	)
});

WindowDef(\TimelineOverlayMenu, { arg def, model, view, x=600, y=600; // deprecated
	var inview, layout;
	Log(\Param).debug("xy: %, %", x, y);
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 200,100);
		win.endFrontAction = { win.close };
	};
	inview = View.new;


	layout = VLayout (
		BasicButton.new.string_("Edit").action_({ 
			PlayerWrapper(model.param.target).edit;
		}),
		HLayout(
			StaticText.new.string_("outBus:"),
			Param(
				Message(model),
				\outBus, 
				TagSpecDef(\BusDef),
				//MenuSpec([\default -> nil, \0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k) }) )
			).asPopUpMenu,
		),
		BasicButton.new.string_("Piano keyboard").action_({ 
			//model.debug("m");
			//model.target.debug("t");
			WindowDef(\TimelinePianoKeyboard).front(model.wrapper)
		}),
		HLayout(
			StaticText.new.string_("controller:"),
			Param(
				Message(model),
				\controller, 
				ControllerDef.getTagSpec(\piano),
				//TagSpecDef(\NoteTimeline_controller),
			).asPopUpMenu.allowsReselection_(true),
			//BasicButton.new.string("Go").fixedWidth_(15).action_({
				//model.startControl;
			//})
		),
		nil
	);
	//def.windowDo { arg win;
		//win.setInnerExtent(view.bounds.extent.min(20));
	//};
	inview.layout_(layout);
}).border_(false);

WindowDef(\ClipList_ContextMenu, { arg def, model, view, selected;
	Menu(
		* 
		if(selected.notNil) {
			[
				MenuAction("Edit", { 
					PlayerWrapper(selected.last.value).edit;
				}),
				MenuAction("Remove " ++ selected.last.key, { 
					model.clipList.remove(selected);
					model.changed(\clipList);
					def.closeWindow;
				}),
			]
		} {
			[]
		} ++ 
		[
			MenuAction.separator,
			MenuAction("Gather clips from timeline", { 
				model.gatherClips;
			}),
			MenuAction("Clear list", { 
				model.clearClipList;
				def.closeWindow;
			}),
		] 
	);
});

WindowDef(\ClipList_OverlayMenu, { arg def, model, view, x=600, y=600, selected; // deprecated
	// deprecated for Menu
	var inview, layout;
	Log(\Param).debug("xy: %, %", x, y);
	Log(\Param).debug("selected: %", selected);
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 200,100);
		win.endFrontAction = { win.close };
	};
	inview = View.new;


	layout = VLayout (
		* [
			BasicButton.new.string_("Gather clips from timeline").action_({ 
				model.gatherClips;
			}),
			BasicButton.new.string_("Clear list").action_({ 
				model.clearClipList;
				def.closeWindow;
			}),
		] ++
		if(selected.notNil) {
			[
				BasicButton.new.string_("Remove " ++ selected.last.key).action_({ 
					model.clipList.remove(selected);
					model.changed(\clipList);
					def.closeWindow;
				}),
				BasicButton.new.string_("Edit").action_({ 
					PlayerWrapper(selected.last.value).edit;
				}),
			]
		} 
		++ [nil]
	);
	//def.windowDo { arg win;
		//win.setInnerExtent(view.bounds.extent.min(20));
	//};
	inview.layout_(layout);
}).border_(false);

WindowDef(\ClipList_AddClip, { arg def, selectedHook, okHook;
	var namefield = TextField.new;
	var trackInstrumentLabel = StaticText.new;
	var lib = ProtoDef(\GlobalLibrary);
	var selectedLibId;
	var selectedCallback = { arg libid, strings, clickCount;
		Log(\Param).debug("selectedCallback itemId:%, str:%", libid, strings);
		trackInstrumentLabel.string = lib.libIdToLabel(libid);
		selectedLibId = libid;
		selectedHook.(def, libid, strings, clickCount)
	};
	VLayout(
		View.new.layout_(
			HLayout(
				StaticText.new.string_("Clip name:"),
				namefield,
			),
		).background_(Color.grey),
		VLayout (
			HLayout (
				StaticText.new.string_("Clip source:"),
				trackInstrumentLabel,
			),
			WindowDef(\GlobalLibrary).asView(lib, selectedCallback, [
				\Clip, \AllTracks, \Timeline,
			]),
		),
		HLayout(
			BasicButton.new.string_("Cancel").action_({
				def.closeWindow;
			}),
			BasicButton.new.string_("OK").action_({
				var label;
				def.closeWindow;
				if(namefield.string.trim != "") {
					label = namefield.name;
				};
				Log(\Param).debug("okhook itemId:%, str:%", selectedLibId, label);
				okHook.(def, selectedLibId, label);
			})
		)
	)

	
});

WindowDef(\TimelinePianoKeyboard, { arg def, player;
	var kb = MIDIKeyboardView.new;
	if(player.notNil) {
		kb.keyDownAction = { arg midinote, old;
			if(old.notNil) {
				player.elAt(old).stop;
			};
			player.elAt(midinote).play;
		};
		kb.keyTrackAction = { arg midinote, old;
			player.elAt(old).stop;
			player.elAt(midinote).play;
		};
		kb.keyUpAction = { arg midinote;
			player.elAt(midinote).stop;
		};
	};
	def.windowDo { arg win;
		win.setInnerExtent(800,120)
   
	};
	kb.asView;
});


/////////////////////////////////

WindowDef(\ClipList_NewClip, { arg def, selectedHook, okHook, tagSpec;
	var namefield = TextField.new;
	var trackInstrumentLabel = StaticText.new;
	var lib = ProtoDef(\GlobalLibrary);
	var selectedFactory;
	var selectedCallback = { arg item;
		trackInstrumentLabel.string = item.key;
		selectedFactory = item;
	};
	tagSpec = tagSpec ?? {TagSpecDef(\ClipTimeline_newPlayer)  };
	VLayout(
		View.new.layout_(
			HLayout(
				StaticText.new.string_("Clip key:"),
				namefield,
			),
		).background_(Color.grey),
		VLayout (
			HLayout (
				StaticText.new.string_("Clip source:"),
				trackInstrumentLabel,
			),
			ListView.new.items_(
				tagSpec.keyList
			).action_({ arg view;
				selectedCallback.(tagSpec.list[view.selection.first])
			}).valueAction_(0),
			//WindowDef(\GlobalLibrary).asView(lib, selectedCallback, [
				//\TrackInstrument
			//]),
		),
		HLayout(
			BasicButton.new.string_("Cancel").action_({
				def.closeWindow;
			}),
			BasicButton.new.string_("OK").action_({
				var label;
				if(namefield.string.trim != "") {
					def.closeWindow;
					label = namefield.string.asSymbol;
					Log(\Param).debug("okhook fac:%, str:%", selectedFactory.asCompileString, label);
					okHook.(def, selectedFactory, label);
				} {
					Log(\Param).error("Please provide a name");
					WindowDef(\OkDialog).front("Please provide a name, it will be used a the key to store the object");
				};
			})
		)
	)
});

///////////////////////////////


WindowDef(\ClipTimeline_SampleManager, { arg def, timeline;
	var list, refresh;
	var timelines;
	list = ListView.new;
	refresh = {
		var sel = list.selection;
		list.items = timeline.recordedEvents.collect({ arg x; 
			var flags = "";
			var tl;
			tl = x.timeline.value;
			if(timeline.eventList.includes(x).not and: {
				timeline.eventList.every({ arg ev; ev.timeline.value !== tl  })
			}) {
				flags = "[Masked]"
			};
			if(tl.recordedFileIsKept) {
				flags = "% (kept)".format(flags)
			};
			"% %".format(tl.label, flags);
		});
		timelines = timeline.recordedEvents.collect({ arg x; x.timeline.value });
		list.selection = sel;
	};
	list.selectionMode = \extended;
	refresh.();
	VLayout(
		HLayout (
			BasicButton.new.string_("play").action_({
				list.selection.do { arg i;
					timelines[i].play;
				};
			}),
			BasicButton.new.string_("stop").action_({
				list.selection.do { arg i;
					timelines[i].stop;
				};
			}),
			BasicButton.new.string_("label").action_({
				list.selection.first !? { arg x; 
					var tl = timelines[x];
					WindowDef(\SampleTimelineManager_RenameDialog, WindowDef(\RenameDialog).front(
						"Change timeline label ?\n\nCurrent label: %\nKey: %\nBuffer: %".format(tl.label, tl.key, tl.buffer.asCompileString),
						tl.label,
						{ arg newlabel;
							tl.label = newlabel;
							refresh.();
						}
					))
				}
			}),
			BasicButton.new.string_("keep").action_({
				list.selection.do { arg i;
					timelines[i].keepRecordedFile;
				};
				refresh.();
			}),
			BasicButton.new.string_("clipboard preset").action_({
				"\n(\n%\n)\n".format(list.selection.collect { arg i;
					timelines[i].presetCompileString;
				}.join("\n\n")).pbcopy;
			}),
			BasicButton.new.string_("remove").action_({
				var flist = list.selection.collect { arg i;
					timelines[i].key
				};
				WindowDef(\SampleTimelineManager_ConfirmDialog, WindowDef(\ConfirmDialog)).front(
					"Are you sure you want to delete theses timelines ?\n\n%".format(flist.collect(_.asString).join("\n")),
					{
						list.selection.do { arg i;
							SampleTimeline.proto.all[timelines[i].key] = nil; // should remove .proto after recompile
						};
						refresh.();
					}
				)
			}),
		),
		list
	)
});

//////////////////////

WindowDef(\ClipTimelineContextMenu, { arg def, viewmodel, view;
	var model = viewmodel.timeline_model;
	Menu(*
		[
			Param(Message(viewmodel.timeline), \enablePreview, ParamBoolSpec()).asMenuAction("Display preview"),
			Param(Message(viewmodel.leftPaneView), \visible, ParamBoolSpec()).asMenuAction("Display right panel"),
			Param(Message(viewmodel.timeline_model), \bindPlayWithRecord, ParamBoolSpec()).asMenuAction("Bind play with record button"),
			MenuAction.separator,
			MenuAction("Add track", {
				viewmodel.add_track;
			}),
			MenuAction("Sample manager", {
				WindowDef("%_SampleManager".format(viewmodel.timeline_model.key).asSymbol, WindowDef(\ClipTimeline_SampleManager)).front(viewmodel.timeline_model);
			}),
			MenuAction.separator,
			if(viewmodel.hasSavePath) {
				MenuAction("Save", {
					viewmodel.saveAction;
				})
			},
			MenuAction("Save as", {
				viewmodel.saveAsAction;
			}),
			MenuAction("Copy refString to clipboard", { 
				viewmodel.timeline_model.refCompileString.pbcopy;
			}),
		].select(_.notNil)
	)
	
});

WindowDef(\ClipTimelineOverlayMenu, { arg def, viewmodel, view, x=600, y=600;
	// deprecated for Menu
	var inview, layout;
	var model = viewmodel.timeline_model;
	//[x, y].debug("xy");
	def.windowDo { arg win;
		var ab = view !? {view.absoluteBounds} ? Rect(0,0,600,600);
		win.bounds = Rect(x+ab.origin.x, Window.screenBounds.height - ab.top - y, 200,100);
		win.endFrontAction = { win.close };
	};
	inview = View.new;


	layout = VLayout (
		Param(Message(viewmodel.timeline), \enablePreview, ParamBoolSpec()).asButton("Display preview"),
		BasicButton.new.string_("Clipboard refString").action_({ 
			model.refCompileString.pbcopy;
		}),
		nil
	);
	//def.windowDo { arg win;
		//win.setInnerExtent(view.bounds.extent.min(20));
	//};
	inview.layout_(layout);
	
}).border_(false);

if(currentEnvironment.isKindOf(ProxySpace).not) {

	~timelinePanelView = ProtoTemplateDef(\TimelinePanelView);
	~clipTimelinePanelView = ProtoTemplateDef(\ClipTimelinePanelView);
	~trackTimelinePanelView = ProtoTemplateDef(\TrackTimelinePanelView);
	~envTimelinePanelView = ProtoTemplateDef(\ParamTimelinePanelView);
	~noteTimelinePanelView = ProtoTemplateDef(\NoteTimelinePanelView);
	~kitTimelinePanelView = ProtoTemplateDef(\KitTimelinePanelView);
	~clipEditor = ProtoTemplateDef(\ClipEditor);
	~trackEditor = ProtoTemplateDef(\TrackEditor);
}
