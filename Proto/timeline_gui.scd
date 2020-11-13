
///////////////////////////////////////////////////////////////////// timeline panel
//////////////////////////////// Timeline Panel Views

/////// base timeline panel view

~timelinePanelView = (
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
			self.cursor_timeline.stop; // FIXME: should be automatic when stoping Pdef
		}).maxWidth_(30);
	},

	make_quant_popup: { arg self;
		var quant_popup;
		var quant_spec;
		quant_spec = MenuSpec([
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

		self.quant_param = Param({ self.get_xquant }, { arg val; self.set_xquant(val) }, quant_spec);
		quant_popup = self.quant_param.asPopUpMenu;
		self.quant_popup = quant_popup;
		self.quant_popup;
	}, 

	make_saveas_button: { arg self;
		BasicButton.new
			.action_({ arg me;
				if(self.timeline_model.presetCompileStringSavePath.notNil) {
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
					self.timeline_model.savePresetCompileString(nil, {
						me.refreshChange;
					}, false);
				};
			})
			.mouseUpAction_({ arg me, x, y, mod, but;
				//[a,b,c,but].debug("but");
				if(but == 1) { // right click
					self.timeline_model.savePresetCompileString(nil, {
						me.refreshChange;
					}, true)
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

	//definePattern: { arg self;
	//	// FIXME: why this is in a view class ??
	//	Pdef(self.eventloop.key, Plazy({
	//		//PtimeGatePunch(self.eventloop.list.asPattern.trace(prefix:"xxxxxxxxxxxxxxxxxxxxx"))
	//		var start, end;
	//		var eventlist = self.eventloop.list;
	//		if(self.cursor.notNil) {
	//			start = self.cursor.startPosition - eventlist.startTime;
	//			if(self.cursor.endPosition.notNil) {
	//				end = self.cursor.endPosition - eventlist.endTime;
	//			}
	//		};
	//		~timeline_pattern.(
	//			~eventlist_aspattern.(
	//				self.eventloop.list
	//			).trace(prefix:"xxxxxxxxxxxxxxxxxxxxx"),
	//			start, end
	//		);
	//		//self.eventloop.list.asPattern.trace(prefix:"xxxxxxxxxxxxxxxxxxxxx")
	//	}));
	//},

	makeWindow: { arg self;
		//var window = Window.new;
		WindowDef("%_%".format(self.timeline_model.eventType, self.key).asSymbol, { arg me;
			var layout;
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

	add_track: { arg self;
		self.timeline_model.add_track;
		self.timeline.refreshEventList;
		self.timeline.refresh;
		self.update_track_header;
	},

	zoom_fit: { arg self;
		//[self.areasize, self.timeline.areasize].debug("1");
		~zoom_fit.(self.timeline);
		//[self.areasize, self.timeline.areasize].debug("2");
		self.areasize = self.timeline.areasize;
		//[self.areasize, self.timeline.areasize].debug("4");
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
		self.track_selector = ~view_selector.new(track_count, {}, { arg view, x; 
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
		//self.vrange.view.doAction;
		if(self.vrange.notNil) {
			self.vrange_track_header_action(self.vrange.view)
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

	make_track_header: { arg self;
		var track_header;
		track_header = ScrollView.new;
		track_header.keyDownAction = self.getTimelineKeyDownAction;
		track_header.hasBorder = false;
		track_header.hasVerticalScroller = false;
		track_header.hasHorizontalScroller = false;
		self.track_header = track_header;

		self.vrange_track_header_action = { arg self, slider;
			if(slider.isNil) {
				//Log(\Param).debug("dont know why there is two actions with one nil: % %", self.key, self.vrange.view.action.array);
			} {

				self.track_header_view.fixedHeight = 1/slider.range * track_header.bounds.height;
				//( 1/slider.range * track_header.bounds.height ).debug("vrange.view.action: fixedHeight");
				track_header.visibleOrigin = Point(0, ( 1-slider.hi ) / slider.range) * track_header.bounds.height;
				//( Point(0, ( 1-slider.hi ) / slider.range) * track_header.bounds.height ).debug("vrange.view.action: visibleOrigin");
			};
		};

		self.vrange.view.addAction({ arg slider;
			self.vrange_track_header_action.(slider)
		});

		self.make_tracks_listeners;
		self.update_track_header;

		//self.vrange.view.activeLo = self.vrange.view.lo; // trigger action

		self.track_header;

	},

	makeTimelineView: { arg self;
		self.timelineClass.new
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

	copyAction: { arg self;
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
			~clipTimeline_clipboard = selnodes;
			~clipTimeline_selection_rect = self.selection_cursor.rect;
		};
	},

	pasteAction: { arg self;
		if(~clipTimeline_clipboard.notNil) {
			var delta;
			var newnodes;
			var first = ~clipTimeline_clipboard.as(Array).inject(nil, { arg a, b; 
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
			delta = self.timeline.lastGridPos.round(self.timeline.quant.value) - ~clipTimeline_selection_rect.origin;
			//~clipTimeline_clipboard.debug("clipboard");
			self.timeline_model.addHistorySnapshot;
			~clipTimeline_clipboard.do({ arg event;
				var new;
				new = event.copy;
				//[new, delta].debug("new, delta");
				new[\absTime] = new[\absTime] + delta.x;
				new[self.timeline.valueKey] = new[self.timeline.valueKey] + delta.y;
				self.timeline.model.addEvent(new);
			});
			//self.timeline.model.changed(\refresh);

			self.selection_cursor.rect = ~clipTimeline_selection_rect.moveBy(delta.x, delta.y);
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
			self.timeline_model.eventLoop.cloneAndPushList;
			//Log(\Param).debug("hook qui foire");
			self.timeline.deleteSelectedNodes;
			self.timeline_model.eventLoop.pushList;
		}
	},

	getTimelineKeyDownAction: { arg self;
		//Window.keyDownActionTest
		({ arg me, key, modifiers, unicode, keycode;
			[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction copypaste");
			self.getTimelineKeyDownActionForNodeMove.(me, key, modifiers, unicode, keycode);
			if(modifiers.isCtrl) {
				//if(key == $n) { // ^n // don't work anymore
				if(keycode == $n.ascii) { // ^n
					"new!".debug;
					self.eventLoop.pushList;
					self.eventList = self.timeline_model.makeDefaultEventList;
					self.eventLoop.pushList;
				};
				if(keycode == $k.ascii) { // ^k
					"clone!".debug;
					self.eventLoop.pushList;
					self.eventList = self.eventLoop.lists.first.clone;
					self.eventLoop.pushList;
				};
				if(keycode == $d.ascii) { // ^d
					"double!".debug;
					self.eventLoop.pushList;
					self.eventList = self.eventLoop.lists.first.clone.double;
					self.eventLoop.pushList;
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
				if(keycode == 65481) { // F12
					"Panic!".debug;
					CmdPeriod.run;
				};
				if(keycode == 65474) { // F5
					"play!".debug;
					self.timeline_model.play;
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
			if(modifiers.isCtrl) {
				handleMove.(
					Point(
						self.quant_ref.value.x * self.node_move_multiplier_x,
						self.quant_ref.value.y * self.node_move_multiplier_y,
					),
					keycode,
				)
			} {
				// no modifier
				handleMove.(self.quant_ref.value, keycode)
			}
		});

	},

	node_move_multiplier_x: 8,
	node_move_multiplier_y: 8,

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

	makeVelocityView: { arg self;
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
		self.velocityValue.onChange(self.velocityTimeline, \nodeMoved, {
			self.refreshPosyValue;
		});
		self.velocityValue.onChange(self.velocityTimeline, \selectedNodes, {
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

	makeXRuler: { arg self;
		var cursor_timeline;
		var locatorbar;
		var ruler_x_beat;
		cursor_timeline = CursorTimelineView.new;
		~cursor_timeline = cursor_timeline;
		self.cursor_timeline = cursor_timeline;
		cursor_timeline.mapCursor(self.cursor);
		cursor_timeline.quant = self.quant_ref;


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
		cursor_timeline.mimicTimeline(self.timeline);

		self.locatorbar.view.minSize = Point(10,15);
		self.ruler_x_beat.view.minSize = Point(10,10);

		self.locatorbar.keyDownAction = self.getTimelineKeyDownAction;
		self.ruler_x_beat.keyDownAction = self.getTimelineKeyDownAction;
		
		VLayout(
			locatorbar.view,
			self.ruler_x_beat.view,
		);
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
		//"AZE".debug;
		layout = HLayout.new;
		self.layout = layout;
		vlayout = VLayout.new;
		layout.add(vlayout);

		self.save_timeline_properties;
		timeline = self.makeTimelineView;
		self.timeline = timeline;
		self.load_timeline_properties;
		//timeline.areasize = self.pr_areasize.debug("==================seting areasize");
		~mytl = timeline;
		timeline.mouseDownAction = {|me, px, py, mod, buttonNumber, clickCount, chosennode|
			if(clickCount == 2) {
				if(chosennode.notNil) {
					self.clipEditorNew(chosennode.model, self.eventloop.list)
				};
			}
		};

		timeline.quant = self.quant_ref;
		timeline.mapSelectionCursor(self.selection_cursor);

		self.makeVelocityView;

		//self.cursor = CursorTimeline.new;
		//self.cursor.startPosition = self.eventloop.list.startTime;
		self.ruler_x = self.makeXRuler;


		hrange = TimelineScroller.new;
		hrange.orientation = \horizontal;
		hrange.mapTimeline(timeline);
		vrange = TimelineScroller.new;
		vrange.orientation = \vertical;
		vrange.mapTimeline(timeline);
		self.vrange = vrange;

		sizer = 
		HLayout(
			XSimpleButton.new.label_("<").action_({
				var point = Point(2,1);
				//self.areasize = self.areasize - point;
				self.areasize = self.areasize / point;
				timeline.refresh;
			}).fixedSize_(Point(10,20)),
			XSimpleButton.new.label_(">").action_({
				var point = Point(2,1);
				self.areasize = self.areasize * point;
				timeline.refresh;
			}).fixedSize_(Point(10,20)),
		).spacing_(0)
		;


		// FIXME: created even if disabled, sometime not what we want
		track_header = self.make_track_header(timeline);


		tl_layout = GridLayout.rows(*
			[
				[
					nil,
					self.ruler_x,
					XSimpleButton.new.label_("z")
						.maxWidth_(10)
						.action_({
							self.zoom_fit;
						})
				],
				[
					if(self.disable_track_header != true) {track_header},
					StackLayout(
						self.cursor_timeline.view,
						timeline.selectionView,
						timeline.view,
					).mode_(1),
					vrange.view,
				],
			] 
			++ if(self.velocityTimeline.notNil) {
				[
					[
						nil,
						self.velocityTimeline.view,
						self.velocityVerticalRange.view,
					],
				]
			} { [] }
			++ if(self.disable_hrange != true) {
				[
					[
						nil,
						hrange.view,
						sizer,
					]
				]
			} { [] }  
		);
		tl_layout.spacing = 0;

		layout = VLayout(*
			[
				self.make_header,
				tl_layout,
			] ++ 
			if(self.velocityTimeline.notNil) { // hide if velocityView is disabled
				[
					HLayout(
						self.velocityShowButton,
						self.velocityPopUp,
						//StaticText.new.string_("Velocity: ")
						//	.font_(Font.default.size_(10))
						//,
						self.velocityValue,
						nil,
					)
				]
			} {
				[]
			}
		);

		size = self.areasize;
		//size.debug("size!!!");

		timeline.areasize = size;

		//ruler_y.view.minSize = Point(40,10);

		self.refresh; // map eventlist to timelines

		self.timeline = timeline;
		self.layout = layout;
		//"iiAZE".debug;


		//self.zoom_fit;

		self.timeline.keyDownAction = self.getTimelineKeyDownAction;
		self.timeline.deleteSelectedNodesHook = self.deleteSelectedNodesHook;

		layout;
	},

	asPattern: { arg self;
		// TODO
		Pfunc({})
	},

	refresh: { arg self;
		// event list has changed, refresh the mappings
		self.timeline.mapEventList(self.eventloop.list);
		self.cursor_timeline.mapEventList(self.eventloop.list);
		self.locatorbar.mapEventList(self.eventloop.list);
		if(self.velocityTimeline.notNil) {
			self.velocityTimeline.mapEventList(self.eventloop.list);
		}
	},

	save_timeline_properties: { arg self;
		var tl = self;
		if(tl.timeline.notNil) {
			tl.timeline_model.addHalo(\viewport, tl.timeline.viewport);
			tl.timeline_model.addHalo(\areasize, tl.timeline.areasize);
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
	},
);

/////// derivatives

~clipTimelinePanelView = (
	parent: ~timelinePanelView,

	new: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = { timeline };
		self.areasize = self.timeline_model.areasize;
		
		self.makeWindow;
	
		self;
	},

	newView: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
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


	make_header: { arg self;
		var mute_button = Button.new.states_([
			[ "Mute", Color.black, Color.white ],
			[ "Unmute", Color.black, ParamViewToolBox.color_ligth ],
		]).onChange(self.track_selector, \selected_index, { arg view;
			view.value = if(self.timeline_model.is_track_bypassed(self.track_selector.selected_index)) {
				1
			} {
				0
			}
		}).action_({
			self.timeline_model.toggle_bypass_track(self.track_selector.selected_index);
			self.track_selector.viewInit.(self.track_selector.selected_view, self.track_selector.selected_index);
		});

		Log(\Param).debug("recorder %", self.timeline_model.recorder);
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			self.make_saveas_button,
			XSimpleButton.new.label_("Add track").action_({
				self.add_track;
			}),
			mute_button,
			self.make_quant_popup,
			NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			Param(Message(self.timeline_model.recorder), \isRecording, XBoolSpec()).asButton("Rec"),
			Param(Message(self.timeline_model), \bindPlayWithRecord, XBoolSpec()).asButton("PlayRec"),
			XSimpleButton.new.label_("Samples").action_({
				WindowDef("%_SampleManager".format(self.timeline_model.key).asSymbol, WindowDef(\ClipTimeline_SampleManager)).front(self.timeline_model);
			}),
		);
		
		self.header_layout;
	},

	timelineClass: { arg self;
		ClipTimelineView
		//PdefTimelineView
	},

	clipEditorNew: { arg self, model;
		~clipEditor.new(model ? self.model, self.timeline_model.eventList);
	},

	makeLayout: { arg self;
		var clipEditor;
		//"clipTimelinePanelView.makeLayout: start init".debug;

		clipEditor = self.clipEditorNew;
		self.clipEditor = { clipEditor };
		//"clipTimelinePanelView.makeLayout: part 0.1".debug;
		self.clipEditorLayout = self.clipEditor.makeLayout;
		//"clipTimelinePanelView.makeLayout: part 0.2".debug;
		self.clipEditor.clearEditor;
		//"clipTimelinePanelView.makeLayout: part 1".debug;
		self[\makeVelocityView] = {}; // disable velocity view
		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
			HLayout (
				*[
					self.makeLeftPaneLayout,
					VLayout (
						[~timelinePanelView[\makeLayout].(self), stretch:1],
						HLayout (
							*[
								self.makeLeftPaneToggle,
								self.clipEditorLayout,
								self.makeRightPaneToggle,
							].select(_.notNil)
						)
					),
					self.makeRightPaneLayout,
				].select(_.notNil)
			),
			//self.parent.makeLayout,
		);
		//"clipTimelinePanelView.makeLayout: part 2".debug;
		self.timeline.followChange(self.timeline_model, \eventFactory, {
			Log(\Param).debug("eventFactory change!!!!");
			self.timeline.eventFactory = self.timeline_model.eventFactory;
		});
		self.timeline.mouseDownAction = { arg me, px, py, mod, buttonNumber, clickCount, chosennode;
			if(clickCount == 2) {
				if(chosennode.notNil) {
					self.clipEditor.edit_selected(chosennode.model);
				}
			} {
				if(chosennode.notNil) {
					self.clipEditor.model = chosennode.model;
					self.clipEditor.makeUpdater;
					chosennode.model.changed(\refresh);
				} {
					self.clipEditor.model = nil;
					self.clipEditor.clearEditor;
				}
			}
		};
		//"clipTimelinePanelView.makeLayout: part 3".debug;
		self.timeline.view.onChange(self.eventloop, \list, { 
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
			~clipTimeline_clipboard = selevents;
			~clipTimeline_selection_rect = self.selection_cursor.rect;
		};
	},

	pasteAction: { arg self;
		if(~clipTimeline_clipboard.notNil) {
			var delta;
			var newnodes;
			var first = ~clipTimeline_clipboard.as(Array).inject(nil, { arg a, b; 
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
			delta = self.timeline.lastGridPos.round(self.timeline.quant.value) - ~clipTimeline_selection_rect.origin;
			//~clipTimeline_clipboard.debug("clipboard");
			self.timeline_model.addHistorySnapshot;
			~clipTimeline_clipboard.do({ arg event;
				var new;
				new = event.copy;
				//[new, delta].debug("new, delta");
				new[\absTime] = new[\absTime] + delta.x;
				new[self.timeline.valueKey] = new[self.timeline.valueKey] + delta.y;
				self.timeline.model.addEvent(new);
			});
			self.selection_cursor.rect = ~clipTimeline_selection_rect.moveBy(delta.x, delta.y);
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
);

~trackTimelinePanelView = (
	parent: ~clipTimelinePanelView,

	new: { arg self, timeline;
		//"trackTimelinePanelView: start init".debug;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
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
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = {timeline};
		self.areasize = self.timeline_model.areasize;
		
		//self.makeWindow;
	
		self;
	},

	clipEditorNew: { arg self, model;
		//"clipEditorNew".debug;
		~trackEditor.new(model ? self.model, self.timeline_model.eventList, self.timeline_model.tracks);
	},

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
		self.track_selector = ~view_selector.new(track_count, {}, { arg view, x; 
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


);

~envTimelinePanelView = (
	parent: ~timelinePanelView,

	new: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		// self.timeline is timelineView actually
		self.timeline_model = timeline;

		self.makeWindow;
		
		self;
	},

	newView: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
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
		//self.ruler_y = MidinoteTimelineRulerView.new;
		//self.track_header = self.ruler_y;
		//self.ruler_y.mimicTimeline(timeline);
		//self.ruler_y.view.minWidth_(50);
		nil
	},

	make_header: { arg self;
		HLayout(
			PlayerWrapperView(self.timeline_model).view,
			if(self.param.target.isKindOf(Bus)) {
				nil;
			} {
				PlayerWrapperView(PlayerWrapper(self.param.target)).view;
			},
			self.param.asKnob,
			Param(Message(self.timeline_model), \repeatCount, ControlSpec(1,inf,\lin)).asNumberBox,
			self.make_quant_popup,
			PlayerWrapper(Pdef(\metronome)).asView, // TODO: make a good one and define it a init
			NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			Button.new.mapParam(Param(Message(self.timeline_model), \isRecording, XBoolSpec())).states_([
				["Rec", Color.black, Color.white],
				["Rec", Color.black, Color.red],
			]),
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
		self.timeline.mapParam(self.levelParam);
		self.set_yquant(self.param.spec.step);
		self.timeline.mouseUpAction = {|me, px, py, mod, buttonNumber, clickCount, chosennode|
			self.timeline.refreshEventList;
		};
		self.timeline.view.onChange(self.eventloop, \list, { 
			self.refresh;
		});
		self.timeline.keyDownAction = self.getTimelineKeyDownAction;
		self.timeline.deleteSelectedNodesHook = self.deleteSelectedNodesHook;

		self.layout;
	},
);


~noteTimelinePanelView = (
	parent: ~timelinePanelView,

	new: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
		self.makeWindow;
	
		self;
	},

	newView: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
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


	make_header: { arg self;
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			self.make_saveas_button,
			Param(Message(self.timeline_model), \repeatCount, ControlSpec(1,inf,\lin)).asNumberBox,
			self.make_quant_popup,
			PlayerWrapper(Pdef(\metronome)).asView, // TODO: make a good one and define it a init
			NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			Button.new.mapParam(Param(Message(self.timeline_model), \isRecording, XBoolSpec())).states_([
				["Rec", Color.black, Color.white],
				["Rec", Color.black, Color.red],
			]),
			BasicButton.new.string_("...").mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				WindowDef(\TimelineOverlayMenu).front(self.timeline_model, view, x, y)
			}).maxWidth_(20)
		);
		
		self.header_layout;
	},


	timelineClass: { arg self;
		MidinoteTimelineView;
	},

	make_track_header: { arg self, timeline;
		//self.track_header = View.new;
		self.ruler_y = MidinoteTimelineRulerView.new;
		self.track_header = self.ruler_y;
		self.ruler_y.mimicTimeline(timeline);
		self.ruler_y.view.minWidth_(50);
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
		self.timeline.view.onChange(self.eventloop, \list, { 
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
);

~kitTimelinePanelView = (
	parent: ~timelinePanelView,

	new: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
		self.makeWindow;
	
		self;
	},

	newView: { arg self, timeline;
		self = ~timelinePanelView[\new].(self, timeline.eventloop);
	
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

	make_header: { arg self;
		self.header_layout = HLayout(
			PlayerWrapperView(self.timeline_model).view,
			self.make_stop_button,
			self.make_saveas_button,
			Param(Message(self.timeline_model), \repeatCount, ControlSpec(1,inf,\lin)).asNumberBox,
			self.make_quant_popup,
			PlayerWrapper(Pdef(\metronome)).asView, // TODO: make a good one and define it a init
			NumberBox.new.mapParam(Param(Message(self.timeline_model.eventLoop), \currIndex, ControlSpec(0,32,\lin,1))),
			Button.new.mapParam(Param(Message(self.timeline_model), \isRecording, XBoolSpec())).states_([
				["Rec", Color.black, Color.white],
				["Rec", Color.black, Color.red],
			]),
			Param(
				Message(self.timeline_model),
				\outBus, 
				TagSpecDef(\BusDef),
				//MenuSpec([\default -> nil, \0->0] ++ BusDef.all.keys.as(Array).sort.collect({ arg k; k -> BusDef(k) }) )
			).asPopUpMenu,
			BasicButton.new.string_("Edit kit").action_({ self.timeline_model.wrapper.edit }),
		);
		
		self.header_layout;
	},


	timelineClass: { arg self;
		KitTimelineView;
	},

	make_track_header: { arg self, timeline;
		//self.track_header = View.new;
		self.ruler_y = KitTimelineRulerView.new;
		self.track_header = self.ruler_y;
		self.ruler_y.mimicTimeline(timeline);
		self.ruler_y.view.minWidth_(50);
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
		self.timeline.view.onChange(self.eventloop, \list, { 
			self.refresh;
		});

		if(self.timeline_model.wrapper.notNil) {
			self.timeline.eventFactory = { arg pos, nodesize;
				(absTime: pos.x, kitIndex: pos.y, sustain:nodesize)
			};
		};
		self.layout;
	},

);


////////////////// clip editor (really: clip chooser/selector)

~clipEditor = (
	new: { arg self, model, parent;
		self = self.deepCopy;
	
		self.model = model;
		self.parent_model = {parent};
		//self.makeWindow;
		
		self.init_default_clip_factories;
	
		self;
	},

	init_default_clip_factories: { arg self;
		var tl;
		self.clipFactories = ();

		self.clipFactories.ndef = (
			key: 'ndef',
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


		);

		self.clipFactories.pdef = (
			key: 'pdef',
			label: { arg self; self.key.asString },
			keylist: {
				Pdef.all.keys.asArray.sort; 
			},

			getTarget: { arg self, ev;
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

			className: {
				Pdef
			},

			getName: { arg self, ev;
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
					"clipFactories.pdef: ERROR: edit: don't know what type!!!".inform;
				}
			},

			redefineClip: { arg me, curname, model;
				PatternEvent.redefine(model);
				model[\pattern] = Pdef(curname);
				//model.changed(\refresh);
				self.parent_model.changed(\refresh);
			},
		);

		self.clipFactories.trackdef = (
			key: 'trackdef',
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
		);
		self.clipFactories_timeline_parent = tl;

		[\noteTimeline, \clipTimeline, \envTimeline, \modStepTimeline, \kitTimeline].do { arg key;
			self.clipFactories[key] = (
				key: key,
				parent: tl,
				className: "{ topEnvironment[%] }".format(key.asCompileString).interpret,
			);
		};

		self.clipFactories.sampleTimeline = (
			key: 'sampleTimeline',
			parent: tl,
			className: { SampleTimeline.proto },
		);

		self.clipFactories_keys = self.clipFactories.keys.asArray.sort;
		
	},

	event_to_clipFactory_key: { arg self, model;
		var clipkey;

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
						clipkey = model.timeline.eventType
					};
					//switch(model.timeline.eventType,
						//\envTimeline, {
							//clipkey = 'envTimeline'
						//},
						//\clipTimeline, {
							//clipkey = 'clipTimeline'
						//},
						//\sampleTimeline, {
							//clipkey = 'sampleTimeline'
						//},
						//\noteTimeline, {
							//clipkey = 'noteTimeline'
						//},
						//\kitTimeline, {
							//clipkey = 'kitTimeline'
						//},
						//\modStepTimeline, {
							//clipkey = 'modStepTimeline'
						//},
					//)
				} {
					//var tar = model.receiver.value;
					//if(tar.isKindOf(PlayerWrapper)) {
					//	tar = tar.target;
					//};
					var tar = model.pattern;
					if(tar.isKindOf(Pdef)) {
						clipkey = 'pdef'
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
						{
							//model.debug("event_to_clipFactory_key: player: unknown");
						}
					);
				};
				clipkey = branch.(model[\receiver].value);
			},
			{
				//model.debug("event_to_clipFactory_key: unknown");
				nil
			}
		);
		//clipkey.debug("event_to_clipFactory_key: result");
		clipkey;
	},

	clearEditor: { arg self;
		// when no event selected

		self.type_popup.items = [""];
		self.name_popup.items = [""];
		self.clipLabelField.mapParam(nil);
		self.startOffsetField.mapParam(nil);
		self.sustainField.mapParam(nil);
	},

	makeUpdater: { arg self;
		self.controller.remove;
		self.controller = SimpleController(self.model).put(\refresh, {
			var ev = self.model;
			Log(\Param).debug("entering clipEditor updater: %, %", self.type_popup, ev);
			if(self.type_popup.isNil or: {self.type_popup.isClosed}) {
				self.controller.remove;
			} {
				var clipkey;
				var clipfac;
				self.refreshPopups;
				clipkey = self.event_to_clipFactory_key(ev);
				Log(\Param).debug("makeUpdater: clipkey", clipkey);
				self.startOffsetField.mapParam(Param(self.model, Pembed.startOffsetKey, ControlSpec(0,100,\lin)));
				self.clipLabelField.mapParam(Param(Message(self.model), \label));
				self.sustainField.mapParam(Param(self.model, Pembed.sustainKey));
				if(clipkey.notNil) {
					//clipkey.debug("makeUpdater: clipkey not nil");
					clipfac = self.clipFactories[clipkey];
					self.type_popup.value = self.clipFactories_keys.detectIndex({ arg x; x == clipkey });
					self.name_popup.items = clipfac.keylist;
					self.name_popup.value = self.name_popup.items.detectIndex({ arg x; 
						x == clipfac.getName(ev)
					});

					// FIXME: should use parent_model startOffsetKey to be flexible
				} {
					// selected event not recognized

					self.type_popup.value = nil;
					self.name_popup.value = nil;
				}
			}
		});
	},

	refreshPopups: { arg self;
		var name_popup = self.name_popup;
		self.type_popup.items = self.clipFactories_keys.collect({ arg x; self.clipFactories[x].label });
		self.type_popup.action = {
			name_popup.items = self.clipFactories[ self.clipFactories_keys[self.type_popup.value] ].keylist;
		};
		self.type_popup.valueAction = 0;
	},

	edit_selected: { arg self, xmodel;
		// edit event
		if((xmodel ? self.model).notNil) {
			WindowDef(\EventEditor, { arg def, model;
				var type_popup, name_popup;
				//var clipLabelField, startOffsetField, sustainField;
				var params;
				var clipkey, clipfac;
				type_popup = PopUpMenu.new;
				name_popup = PopUpMenu.new;
				type_popup.items = self.clipFactories_keys.collect({ arg x; self.clipFactories[x].label });
				type_popup.action = {
					name_popup.items = self.clipFactories[ self.clipFactories_keys[type_popup.value] ].keylist;
				};
				//type_popup.valueAction = 0;

				clipkey = self.event_to_clipFactory_key(model);
				//clipkey.debug("makeUpdater: clipkey");
				if(clipkey.notNil) {
					//clipkey.debug("makeUpdater: clipkey not nil");
					clipfac = self.clipFactories[clipkey];
					type_popup.value = self.clipFactories_keys.detectIndex({ arg x; x == clipkey });
					name_popup.items = clipfac.keylist;
					name_popup.value = name_popup.items.detectIndex({ arg x; 
						x == clipfac.getName(model)
					});
				};

				params = [
					Param(Message(self.model), \label),
					Param(self.model, Pembed.startOffsetKey, ControlSpec(0,100,\lin)),
					Param(self.model, \gain, \gain.asSpec),
					Param(self.model, \outBus, TagSpecDef(\BusDefDefault)),
					Param(self.model, \absTime),
					Param(self.model, Pembed.sustainKey),
				];
				name_popup.action = {
					var curname = name_popup.items[name_popup.value];
					var model = self.model;
					self.clipFactories[self.clipFactories_keys[type_popup.value]].redefineClip(curname, model);
				};
				VLayout (
					[type_popup, stretch:2],
					[name_popup, stretch:2],
					Button.new.string_("Edit target").action_({
						self.edit_selected_target;
					}),
					* params.collect({ arg param;
						HLayout(
							StaticText.new.string_(param.property).minWidth_(110),
							if(param.spec.isKindOf(TagSpec)) {
								param.asPopUpMenu;
							} {
								TextField.new.mapParam(param);
							},
						);
					}) ++ [nil]
				);
			}).front(xmodel ? self.model);
		}
	},

	edit_selected_target: { arg self, model;
		var clipkey, clipfac;
		model = model ? self.model;
		//[model.type, model].debug("Edit: type");
		clipkey = self.event_to_clipFactory_key(model);
		clipfac = self.clipFactories[clipkey];
		clipfac.editSelected(model);
	},

	makeLayout: { arg self;
		var layout;
		var header;
		var editor;
		var type_popup, name_popup;
		var model = self.model;
		//"clipEditor.makeLayout: init".debug;
		type_popup = PopUpMenu.new;
		name_popup = PopUpMenu.new;
		self.name_popup = name_popup;
		self.type_popup = type_popup;
		name_popup.allowsReselection = true;
		//"clipEditor.makeLayout: part 1".debug;
		//type_popup.allowsReselection = true; // lose selection
		//name_popup.items = type_popup.items[type_popup.value].asClass.all.keys.asArray;
		name_popup.action = {
			var curname = name_popup.items[name_popup.value];
			var model = self.model;
			self.clipFactories[self.clipFactories_keys[type_popup.value]].redefineClip(curname, model);
		};

		self.refreshPopups;
		//"clipEditor.makeLayout: part 2".debug;
		self.clipLabelField = TextField.new.fixedWidth_(60);
		self.startOffsetField = TextField.new
					.keyDownAction_({ arg me, key, modifiers, unicode, keycode;
						// FIXME: this doesn't work because parent_model is the EventList and not the timeline. refactor: Timeline GUI should provide the keyDownAction function directly
						[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
						
						if(modifiers.isCtrl and: {keycode == $ .ascii}) { // ^s
							"play/stop now!".debug;
							if(self.parent_model.selection_cursor.notNil) {
								self.parent_model.cursor.setPositions(
									self.parent_model.selection_cursor.startPosition ? 0,
									self.parent_model.selection_cursor.endPosition
								);
							} {
								Log(\Param).debug("no selection cursor");
							};
							self.parent_model.playNow(1);
						};
						if(keycode == 65307) { // esc
							"Focus!".inform;
							me.focus(false;) // FIXME: this outfocus but do not focus on the timeline
						};
						if(keycode == 65481) { // F12
							"Panic!".inform;
							CmdPeriod.run;
						};
						if(keycode == 65474) { // F5
							"play!".inform;
							self.timeline_model.play;
						}
					})
			.fixedWidth_(60);
		self.sustainField = TextField.new.fixedWidth_(60);

		header = HLayout(
			[type_popup, stretch:2],
			[name_popup, stretch:2],
			XSimpleButton.new.label_("Edit").action_({
				self.edit_selected;
			}),
			[HLayout(
				StaticText.new.string_("offset:"),
				self.startOffsetField,
			), stretch:0],
			[HLayout(
				StaticText.new.string_("sustain:"),
				self.sustainField,
			), stretch:0],
			[HLayout(
				StaticText.new.string_("name:"),
				self.clipLabelField,
			), stretch:0],

		);
		layout = VLayout(
			header,
			editor,
		);

		//"clipEditor.makeLayout: part 3".debug;
		self.layout = layout;
		self.layout;
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;

		self.makeUpdater;
		self.model.changed(\refresh);
	},

);

~trackEditor = ProtoClass((
	new: { arg self, model, parent, trackgroup;
		self = self.deepCopy;
	
		self.model = model;
		self.parent_model = {parent};
		self.trackgroup = { trackgroup };
		~clipEditor[\init_default_clip_factories].(self);
		
		self;
	},
	
	mapModel: { arg self, model, parent, trackgroup;
		self.model = model;
		self.parent_model = { parent };
		self.trackgroup = { trackgroup };
		self.makeUpdater;
		self.model.changed(\refresh);
	},

	makeUpdater: { arg self;
		self.controller.remove;
		self.controller = SimpleController(self.model).put(\refresh, {
			var ev = self.model;
			//[self.type_popup, ev].debug("entering clipEditor updater");
			if(self.type_popup.isNil or: {self.type_popup.isClosed}) {
				self.controller.remove;
			} {
				var clipkey;
				var clipfac;
				self.refreshPopups;
			}
		});
	},

	model_to_type_index: { arg self;
		var track;
		if(self.model.notNil and: { self.model.receiver.notNil }) {
			track = self.model.receiver.value.target.parentTrack;
			self.trackgroup.detectIndex({ arg x; x.target === track  });
		}
	},

	model_to_name_index: { arg self;
		if(self.model.notNil and: { self.model.receiver.notNil }) {
			self.model.receiver.value.target.index;
		}
	},

	redefineSelected: { arg self, model;
		var view = self.name_popup;
		var track = self.trackgroup[self.type_popup.value].target;
		var idx = view.value;
		model = model ? self.model;
		PlayerEvent.redefine(model);
		model[\receiver] = { PlayerWrapper(track.childAt(idx)) };
		self.parent_model.changed(\refresh);
	},

	refreshPopups: { arg self;
		var name_popup = self.name_popup;
		//"refreshPopups".debug;
		self.type_popup.items = self.trackgroup.asArray.collect(_.label);
		self.type_popup.action = {
			//"type_popup.action".debug;
			if(self.type_popup.value.notNil) {
				name_popup.items = self.trackgroup[self.type_popup.value ].target.children.asArray.collect(_.label);
			}
		};
		self.name_popup.action = { arg view;
			//"name_popup.action".debug;
			if(view.value.notNil and: {self.model.notNil and: {self.type_popup.value.notNil}}) {
				self.redefineSelected;
			}
		};
		self.type_popup.valueAction = self.model_to_type_index;
		self.name_popup.value = self.model_to_name_index;
	},

	edit_selected: { arg self, model;
		model = model ? self.model;
		//[model.type, model].debug("Edit: type");
		self.model.receiver.value.target.edit;
	},

	makeLayout: { arg self;
		var layout;
		var header;
		var editor;
		var type_popup, name_popup;
		var model = self.model;
		//"clipEditor.makeLayout: init".debug;
		type_popup = PopUpMenu.new;
		name_popup = PopUpMenu.new;
		self.name_popup = name_popup;
		self.type_popup = type_popup;
		name_popup.allowsReselection = true;
		//"clipEditor.makeLayout: part 1".debug;
		//type_popup.allowsReselection = true; // lose selection
		self.refreshPopups;

		header = HLayout(
			type_popup,
			name_popup,
			BasicButton.new.label_("Edit").action_({
				self.edit_selected;
			})
		);
		layout = VLayout(
			header,
			editor,
		);

		//"clipEditor.makeLayout: part 3".debug;
		self.layout = layout;
		self.layout;
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;

		self.makeUpdater;
		self.model.changed(\refresh);
	},

	asView: { arg self;
		self.makeLayout;
	},
));

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
		model.gatherClips;
	});
	newButton = BasicButton.new.string_("New").action_({ arg view;
		WindowDef(\ClipList_NewClip).front({}, { arg idef, libId, label;
			Log(\Param).debug("OK! libid:%, label:%", libId, label);

			if(libId.notNil) {
				model.addClipIdToClipList(libId);
				idef.closeWindow;
			} {
				"no name".debug;
			}
		});
		model.gatherClips;
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
			}))
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
		}),
		HLayout (
			StaticText.new.followChange(model, \brushClipId, { arg view;
				var clipId = model.brushClipId;
				view.string_(
					model.clipIdLabel(clipId)
				)
			}),
			BasicButton.new.string_("Edit").action_({
				model.clipIdToTarget(model.brushClipId).edit;
			})
		)
	)
});

WindowDef(\TimelineOverlayMenu, { arg def, model, view, x=600, y=600;
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
				TagSpecDef(\NoteTimeline_controller),
			).asPopUpMenu,
		),
		nil
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
				\Clip, \TrackListPart, \TrackListChannelPart, \Timeline,
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

WindowDef(\ClipList_NewClip, { arg def, selectedHook, okHook; // TODO: should create new clip
	/////////////////////////
	// TODO	 TODO TODO
	//////////////////////:
	var namefield = TextField.new;
	var trackInstrumentLabel = StaticText.new;
	var lib = ProtoDef(\GlobalLibrary);
	var selectedLibId;
	var selectedCallback = { arg libid, strings, clickCount;
		Log(\Param).debug("selectedCallback itemId:%, str:%", libid, strings);
		trackInstrumentLabel.string = lib.libIdToLabel(libid);
		selectedLibId = libid;
		selectedHook.(def, libid, strings, clickCount);
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
				\Clip, \TrackListPart, \TrackListChannelPart, \Timeline,
			]),
		),
	/////////////////////////
	// TODO	 TODO TODO
	//////////////////////:
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


WindowDef(\ClipTimelineNewClip, { arg def, model, fac; // deprecated
	var namefield = TextField.new;
	VLayout (
		HLayout(
			StaticText.new.string_("name:"),
			namefield,
		),
		ListView.new.items_(fac.keylist ++ [""]).action_({ arg view;
			namefield.string = view.items[view.value];
		}),
		HLayout(
			BasicButton.new.string_("Cancel").action_({ arg view;
				def.closeWindow;
			}),
			BasicButton.new.string_("OK").action_({ arg view;
				var name = namefield.string;
				if(name.notNil and: {name.trim != ""}) {
					var inst = fac.className.new(name.asSymbol);
					model.addClipIdToClipList([fac.key, inst.key]);
					def.closeWindow;
				} {
					"no name".debug;
				}
			}),
		)
	)
});


///////////////////////////////


(
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
);
