
~stepseq_page = (
	new: { arg self, parent;
		self = self.deepCopy;
	
		
		self.parent_model = { parent };
		self.lines = List.newFrom(
			16.collect({
				var se = StepEvent.new;
				se[\isRest] = StepList[0];
				se[\isRest].addSpec(\list, \unipolar.asSpec);
				se[\isRest].addHalo(\seqstyle, \gridButton);
				//se[\isRest].bypass = 1;
				se;
			})
		);
		self.proxy = EventPatternProxy.new;
		self.proxy.source = self.xasPattern;
		self.label = "No name";
	
		self;
	},

	drumrack: { arg self;
		if(self.parent_model.notNil) {
			self.parent_model.drumrack
		} {
			nil
		}
	},

	bypass_track: { arg self, idx, val=true;
		self.lines[idx][\type] = if(val) { \rest } { \note };
	},

	toggle_bypass_track: { arg self, idx;
		if(self.is_track_bypassed(idx)) {
			self.bypass_track(idx, false)
		} {
			self.bypass_track(idx, true)
		}
	},

	is_track_bypassed: { arg self, idx;
		self.lines[idx][\type] == \rest
	},

	///////////////////// Pattern

	asRawPattern: { arg self;
		Ppar(
			self.lines.collect({ arg line, x;
				Plazy({ self.lines[x].asPattern }).loop
			});
		);
	},

	xasPattern: { arg self, event;
		Ppar(
			self.lines.size.collect({ arg i;
				~pdemul.(
					PchainT(
						Plazy({ self.lines[i].asPattern }).loop, 
						Plazy({
							var drumrack;
							if(event.notNil and: { event.drumrack.notNil }) {
								drumrack = event.drumrack;
							} {
								drumrack = self.drumrack;
							};
							//[i,drumrack].debug("event drumrack ?");
							if(drumrack.notNil and: {drumrack.get_pad(i).patternList.size > 0}) {
								//[i,j,drumrack.get_pad(i)].debug("drum i");
								Ptuple(
									drumrack.get_pad(i).patternList.collect({ arg spat, j;
										if(event.notNil and: {event[\modulators].notNil}) {
											spat = event[\modulators].value.get_pad_modulator_pattern(i,j) <> spat;
										};
										spat;
									});
								).keep(1);
							} {
								Ptuple([Pbind().keep(1)])
							}
						}).loop
					)
				)
			})
		);
	},

	asPatternList: { arg self;
		self.lines.collect({ arg line;
			line.asPattern.loop
		})
	},

	///////////////////// Archiving

	clone: { arg self;
		var new = ~stepseq_page.new(self.parent_model);
		//new.lines = self.lines.deepCopy;
		new.lines = self.lines.collect { arg line, i;
			var newline = line.clone;
			// FIXME: halo should be already copied by clone
			//newline[\isRest].addSpec(\list, \unipolar.asSpec);
			//newline[\isRest].addHalo(\seqstyle, line.getHalo(\seqstyle));
			newline;
		};
		new;
	},

	new_load: { arg self, data, parent;
		self = self.deepCopy;
		
		self.parent_model = { parent }; // need to set parent later

		// TODO: temporary, should be implemented in StepList.new_load
		self.lines = data.lines.collect({ arg line, i;
			line[\isRest].addSpec(\list, \unipolar.asSpec);
			line;
			//newline[\isRest].addHalo(\seqstyle, \gridButtonline.getHalo(\seqstyle));
		});

		self.proxy = EventPatternProxy.new;
		self.proxy.source = self.xasPattern; // this will update in real time after parent is set
		self.label = data.label;
	
		self;
	},

	asArchiveData: { arg self;
		// TODO: implement asArchiveData on StepList
		(
			lines: self.lines,
			label: self.label,
			load: { arg me;
				~stepseq_page.new_load(me)
			},
		)
	},

	///////////////////// GUI

	makeLayout: { arg self;
		var drumrack = self.drumrack;
		var drkey = drumrack !? { drumrack.key } ? "";
		var mute_button;
		var refresh_mute_button = { arg x;
			mute_button
				.action_({
					x.debug("selector Action!");
					self.toggle_bypass_track(x);
					self.track_selector.refresh_view(x);
					refresh_mute_button.(x);
				})
			;
			if(self.is_track_bypassed(x)) {
				mute_button.label_(
					"Unmute"
				)
				.background_(ParamViewToolBox.color_ligth)
			} {
				mute_button.label_(
					"Mute"
				)
				.background_(Color.white)
			}
		};
		mute_button = XSimpleButton.new.label_("xMute");
		self.track_selector = ~view_selector.new(self.lines.size, { arg x;
			refresh_mute_button.(x);
		}, { arg view, i;
			var lab = i;
			if(self.is_track_bypassed(i)) {
				i = "[%]".format(i)
			};
			view.label_(i)
		});
		self.lines_view = View.new;
		refresh_mute_button.(self.track_selector.selected_index);
		self.layout = VLayout (
			HLayout (
				PlayerWrapper(self.proxy).asView,
				TextField.new.string_(self.label).action_({ arg view;
					self.label = view.string;
				}),
				XSimpleButton.new.label_("Edit DrumRack " ++ drkey).action_({
					if(self.drumrack.notNil) {
						self.drumrack.edit;
					}
				}),
				mute_button
			),
			ScrollView.new.canvas_(self.lines_view)
		);
		self.updateLines;
		self.layout;
	},

	updateLines: { arg self;
		self.lines_view.removeAll;
		self.lines_view.layout_(
				VLayout (
					* self.lines.collect({ arg stepevent, i;
						HLayout (
							self.track_selector.views[i].fixedWidth_(30),
							~stepEventCoinView.new(stepevent).hasCursor_(true).asView
						)
					})
				).spacing_(0).margins_(0)
			);
	},

	makeWindow: { arg self;
		//var window = Window.new;
		//var layout;
		//layout = self.makeLayout;
		//window.layout = layout;
		////window.alwaysOnTop = true;
		//window.front;

		WindowDef(\stepseq, { arg win;
			win.view.keyDownAction_({ arg me, key, modifiers, unicode, keycode;
				[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
				if(keycode == 54) { // ^c
					~stepseq_page_clipboard = self.lines[self.track_selector.selected_index];
				};
				if(keycode == 55) { // ^v
					if(~stepseq_page_clipboard.notNil) {
						self.lines[self.track_selector.selected_index] = ~stepseq_page_clipboard.clone;
						self.updateLines;
					}
				}
			});
			self.makeLayout;
		}).alwaysRecreate_(true).front;
		
	},

	edit: { arg self, drumrack;
		//self.drumrack = drumrack; // use parent_model instead
		self.makeWindow;
	},
);

/////

~stepTimeline = (
	parent: ~eventTimeline,
	all: { PresetDictionary(\stepTimeline) },
	eventType: \stepTimeline,

	new: { arg self, name;

		//if(self.all[name].notNil) {
		if(false) {
			self = self.all[name]
		} {
			self = self.deepCopy;
			self.prAdd(name).init;
		};
	
		self;
	},

	newInstance: { arg self, name;
		self = self.deepCopy;
		self.prAdd(name).init;
		self;
	},


	init: { arg self;
		var el = self.makeDefaultEventList;
		self.mod_eventlists = List.new;
		self.areasize = 10@4;
		self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;
		"=============stepTimeline: end init".debug;

		self;
	},

	new_load: { arg self, data;
		var el;
		self = self.deepCopy;
		self.prAdd(data.name).init;
		el = ~eventlist_loadArchiveData.(data.eventlist);
		el.do { arg ev;
			if(ev[\stepseq_page].notNil) {
				ev[\stepseq_page].parent_model = { self };
			}
		};
		self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment
		self.eventList.reorder;
		self.areasize = data.areasize;
		self.cursor.startPosition = self.eventList.startTime;
		self.quant_ref = data.quant_ref;
		self.tracks_bypassed = data.tracks_bypassed;
		self;
	},

	asArchiveData: { arg self;
		(
			name: self.key,
			param: self.param,
			eventlist: ~eventlist_asArchiveData.(self.eventList),
			areasize: self.areasize,
			quant_ref: self.quant_ref,
			tracks_bypassed: self.tracks_bypassed,
			load: { arg me; ~stepTimeline.new_load(me) },
		)
	},

	asRefArchiveData: { arg self;
		(
			key: self.key,
			load: { arg me; ~stepTimeline.new(me.key) },
		)
	},


	xasPattern: { arg self, relStartTime, totalDur;
		self.proxy.source = Plazy({
			var eventlist = self.eventList;
			var xtotalDur; 
			var xrelStartTime;

			xrelStartTime = relStartTime ? eventlist.relStartTime;
			xtotalDur = totalDur ? eventlist.totalDur;

			[xrelStartTime, xtotalDur].debug("clip xasPattern: plazy: relstart, totdur");

			self.eventloop.list.changed(\cursor, \play);
			Pfindur(xtotalDur, ~timeline_pattern.(
				Pn(eventlist, 1) <> (drumrack: self.drumrack, modulators: self[\modulators]),
				//Pn(eventlist, 1),
				xrelStartTime
			));
		}).repeat(self.repeatCount); // FIXME: repeat will not update in real time
		self.proxy;
	},

	edit: { arg self;
		var editor = ~stepTimelinePanelView.new(self); 
		self.editor = {editor};
	},

	initStepEvent: { arg self, ev;
		ev.putAll(~event_type_pattern);
		ev[\stepseq_page] = ~stepseq_page.new(self);
		ev[\label] = { arg me; me.stepseq_page.label };
		ev[\edit] = { arg me, drumrack; me.stepseq_page.edit(drumrack) };
		ev[\asArchiveData] = { arg me;
			var data = me.copy;
			data[\stepseq_page] = data.stepseq_page.asArchiveData;
			data[\load] = { arg datame;
				datame[\stepseq_page] = datame.stepseq_page.load;
			};
		};
		ev[\pattern] = { arg me; 
			me.stepseq_page.xasPattern(me);
			//Ppar(
			//	me.stepseq_page.asPatternList.collect({ arg pat,i;
			//		~pdemul.(
			//			PchainT(
			//				pat, 
			//				Plazy({
			//					//me[\drumrack].debug("event drumrack ?");
			//					if(me[\drumrack].notNil and: {me[\drumrack].get_pad(i).patternList.size > 0}) {
			//						//[i,me[\drumrack].get_pad(i)].debug("drum i");
			//						Ptuple(
			//							me[\drumrack].get_pad(i).patternList.collect({ arg pat, j;
			//								if(me[\modulators].notNil) {
			//									pat = me[\modulators].value.get_pad_modulator_pattern(i,j) <> pat;
			//								};
			//								pat;
			//							});
			//						).keep(1);
			//					} {
			//						Ptuple([Pbind().keep(1)])
			//					}
			//				}).loop
			//			)
			//		)
			//	})
			//)
			//Pbind(
			//	\instrument, \default,
			//	\freq, 200,
			//	\dur, 1/8,
			//	\amp, 0.1
			//);
		};
		
	},
);

~stepTimelinePanelView = (
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
	
		self.timeline_model = timeline;
		self.areasize = self.timeline_model.areasize;
		
		self;
		
	},

	cursor: { arg self;
		self.timeline_model.cursor;
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
			XSimpleButton.new.label_("Add track").action_({
				self.add_track;
			}),
			self.make_quant_popup,
		);
		
		self.header_layout;
	},

	timelineClass: { arg self;
		PdefTimelineView
	},

	drumrack: { arg self;
		self.timeline_model.drumrack;
	},

	makeLayout: { arg self;
		var edit_button;
		var drumrack_button;
		var layout_list;

		drumrack_button = XSimpleButton.new.label_("Edit DrumRack").action_({
			if(self.drumrack.notNil) {
				self.drumrack.edit;
			}
		});
		edit_button = XSimpleButton.new.label_("edit").action_({
			var ev = self.selected_event;
			if(ev.notNil) {
				if(ev[\stepseq_page].notNil) {
					ev.edit(self.drumrack);
				} {
					self.timeline_model.initStepEvent(ev);
					ev.edit(self.drumrack);
					self.timeline_model.eventList.changed(\refresh);
				}
			}
		});

		self.edit_button = edit_button;
		self.drumrack_button = drumrack_button;


		layout_list = [
			[self.parent[\makeLayout].(self), stretch:1],
		];
		if(self.disable_footer != true) {
			layout_list.add(
				HLayout (
					drumrack_button,
					edit_button,
				),
			)
		};

		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
			*layout_list
			
			//self.clipEditorLayout,
			//self.parent.makeLayout,
		);

		self.timeline.mouseDownAction = { arg me, px, py, mod, buttonNumber, clickCount, chosennode;
			self.last_pixelpos = Point(px,py);
			if(chosennode.notNil) {
				self.selected_event = chosennode.model;
				edit_button.background_(ParamViewToolBox.color_ligth);
				if(clickCount == 2) {
					var ev = self.selected_event;
					if(ev.notNil) {
						if(ev[\stepseq_page].notNil) {
							ev.edit(self.drumrack);
						} {
							self.timeline_model.initStepEvent(ev);
							ev.edit(self.drumrack);
							self.timeline_model.eventList.changed(\refresh);
						}
					}
				};
			} {
				self.selected_event = nil;
				edit_button.background_(Color.white)
			}
		};
		self.timeline.keyDownAction = ({ arg me, key, modifiers, unicode, keycode;
			[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
			if(modifiers.isCtrl) {
				if(keycode == 54) {
					if(self.timeline.chosennode.notNil) {
						~stepTimeline_clipboard = self.timeline.chosennode.model;
					}
				};
				if(keycode == 55) {
					if(~stepTimeline_clipboard.notNil) {
						var gpos = self.timeline.pixelPointToGridPoint(self.last_pixelpos);
						var new;
						new = ~stepTimeline_clipboard.copy;
						new.stepseq_page = new.stepseq_page.clone;
						new[\absTime] = gpos.x;
						self.timeline.model.addEvent(new);
						//self.timeline.model.changed(\refresh);
						self.timeline.model.reorder;
					}
				}
			}
		});
		self.layout;
	},
);

//////

~modStepTimeline = (
	parent: ~eventTimeline,
	eventType: \modStepTimeline,
	all: { PresetDictionary(\modStepTimeline) },
	new: { arg self, name;
	
		if(self.all[name].notNil) {
		//if(false) {
			self = self.all[name]
		} {
			self = self.deepCopy;
			self.prAdd(name).init;
		};
		
	
		self;
	},

	newInstance: { arg self;
		self = self.deepCopy;
		self.key = \instance;
		self.init;
		self;
	},

	init: { arg self;

		self.proxy = EventPatternProxy.new;
		self.eventloop = { arg me; me.stepTimeline.eventloop };
		self.stepTimeline = ~stepTimeline.newInstance(self.key);
		self.stepTimeline.modulators = { self };
		//self.stepTimeline.modulators = { (get_pad_modulator_pattern: I)nil };
		self.mod_timelines = List.new;
		self.pad_modulators = Array.fill(16, { Array.fill(16, PresetEvent.new) });
		
	},

	new_load: { arg self, data;
		self = self.deepCopy;

		self.proxy = EventPatternProxy.new;
		self.stepTimeline = data[\stepTimeline].load;
		self.eventloop = { arg me; me.stepTimeline.eventloop };
		self.stepTimeline.modulators = { self };
		//self.stepTimeline.modulators = { (get_pad_modulator_pattern: I)nil };
		self.mod_timelines = List.new;
		self.pad_modulators = Array.fill(16, { Array.fill(16, PresetEvent.new) });

		self;
	},

	asArchiveData: { arg self;
		(
			name: self.key,
			stepTimeline: self.stepTimeline.asArchiveData,
			load: { arg me; ~modStepTimeline.new_load(me) },
		)
	},

	asRefArchiveData: { arg self;
		(
			key: self.key,
			load: { arg me; ~modStepTimeline.new(me.key) },
		)
	},

	add_modulator: { arg self, param;
		//var param;
		var timeline;
		var bus_key = "%_%".format(self.drumrack.key, param.getHalo(\label)).asSymbol;
		//param = Param(BusDef(\plop, \control), \fratio, ControlSpec(0.1,5,\lin));
		timeline = ~envTimeline.newInstance(param, BusDef(bus_key, \control));
		self.mod_timelines.add( timeline );
		self.xasPattern; // update proxy
	},

	remove_modulator: { arg self, idx;
		// TODO: changed signal
		if(idx >= 0) {
			self.mod_timelines.removeAt(idx);
			self.xasPattern; // update proxy
		}
	},

	toggle_bypass_modulator: { arg self, idx;
		var param = self.mod_timelines[idx].param;
		if(idx >= 0) {
			if(param.target.bypass[param.property] == true) {
				param.target.bypassKey(param.property, false);
			} {
				param.target.bypassKey(param.property, true);
			}
		}
	},

	get_pad_modulator_pattern: { arg self, idx, subidx;
		self.pad_modulators[idx][subidx].asPattern
	},

	drumrack: { arg self;
		self.stepTimeline.drumrack;
	},

	get_params: { arg self;
		//self.drumrack.pads.collect({ arg pad, pad_idx;
		//	pad.list.collect({ arg preset, p_idx;
		//		preset.paramlist.select({ arg p; p.type == \scalar }).collect({ arg param;
		//			param.addHalo(\label, "pad %_% -- %".format(pad_idx.asString.padLeft(2), p_idx, param.property))
		//		});
		//	})
		//}).flat;
		self.update_pad_modulators;
		self.mod_params;
	},

	update_pad_modulators: { arg self;
		self.mod_params = self.drumrack.pads.collect({ arg pad, pad_idx;
			pad.list.collect({ arg preset, p_idx;
				var preset_modulator = self.pad_modulators[pad_idx][p_idx];
				preset.paramlist.select({ arg p; p.type == \scalar }).collect({ arg param;
					var modparam = Param(preset_modulator, param.property, param.spec);
					modparam.addHalo(\label, "pad %_% -- %".format(pad_idx.asString.padLeft(2), p_idx, param.property));
					modparam.addHalo(\track_label, "pad %_%\n%".format(pad_idx.asString.padLeft(2), p_idx, param.property));
					modparam;
				});
			})
		}).flat;
	},

	drumrack_: { arg self, drumrack;
		self.stepTimeline.drumrack = drumrack;
	},

	drumrack: { arg self;
		self.stepTimeline.drumrack
	},

	xasPattern: { arg self;
		self.proxy.source = Ppar(
			[ 
			
				self.stepTimeline.xasPattern
			]
			++
			self.mod_timelines.collect({ arg tl;
				tl.xasPattern
			})
		);
		self.proxy;
	},

	edit: { arg self;
		self.editor = ~modStepTimelinePanelView.new(self);
		self.editor.makeWindow;
	},

	asView: { arg self;
		self.editor = ~modStepTimelinePanelView.new(self);
		self.editor.makeLayout;
	},
);

~modStepTimelinePanelView = (
	new: { arg self, model;
		self = self.deepCopy;
	
		self.model = { model };
		
	
		self;
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;	
	},

	make_vrange: { arg self;
		self.ruler_y_view = RangeSlider.new;
		self.ruler_y_view.action = { arg slider;
			self.timelines_view.fixedHeight = 1/slider.range * self.timelines_scrollview.bounds.height;
			self.timelines_scrollview.visibleOrigin = Point(0, ( 1-slider.hi ) / slider.range) * self.timelines_scrollview.bounds.height;
		
		};
		
		self.ruler_y_view;
	},

	get_xquant: { arg self;
		self.stepTimelineView.get_xquant;
	},

	set_xquant: { arg self, val;
		self.timelines.do { arg tl;
			tl.set_xquant(val);
		}
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
		].collect({ arg x; ( "Quant: " ++ x ) -> x.interpret }));

		self.quant_param = Param({ self.get_xquant }, { arg val; self.set_xquant(val) }, quant_spec);
		quant_popup = self.quant_param.asPopUpMenu;
		self.quant_popup = quant_popup;
		self.quant_popup;
	}, 

	save_timelines_properties: { arg self;
		self.timelines.do { arg tl;
			tl.timeline_model.addHalo(\viewport, tl.timeline.viewport);
			tl.timeline_model.addHalo(\areasize, tl.timeline.areasize);
		};
	},

	save_timeline_properties: { arg self;
		self.save_timelines_properties;
	},
	
	load_timeline_properties: { arg self;
		self.load_timelines_properties
	},

	load_timelines_properties: { arg self;
		self.timelines.do { arg tl;
			var cur;
			cur = tl.timeline_model.getHalo(\viewport);
			if(cur.notNil) {
				tl.timeline.viewport = cur;
			};
			cur = tl.timeline_model.getHalo(\areasize);
			if(cur.notNil) {
				tl.timeline.areasize = cur;
			};
		};
	},

	refresh_timelines_view: { arg self;
		var stepTimelineView;
		var timeline;
		var selector;

		selector = ~view_selector.new(self.model.mod_timelines.size+1, {

		});
		if(self.track_selector.notNil) {
			selector.select_item(self.track_selector.selected_index);
		};
		self.track_selector = selector;

		self.save_timelines_properties;

		self.timelines = 
			[
				~stepTimelinePanelView.newView(self.model.stepTimeline)
					.make_header_(nil)
					.make_track_header_({})
					.update_track_header_({})
					.disable_footer_(true)
					.disable_hrange_(true) 
					.disable_track_header_(true)
			]
			++
			self.model.mod_timelines.collect({ arg x, i; 
				~envTimelinePanelView.newView(x)
					.make_header_(nil) 
					.disable_hrange_(true)
			})
		;

		self.timelines.do({ arg x, i;
			x.track_header_({ arg self; 
				var label;
				if(i == 0) {
					label = "steps"
				} {
					label = x.param.getHalo(\track_label);
					if(self.param.target.bypass[self.param.property] == true) {
						label = "[%]".format(label)
					}
				};
				VLayout(
					[selector.views[i].label_(label), align:\top]
				).margins_(10)
			
			})
		});

		stepTimelineView = self.timelines.first;
		self.stepTimelineView = stepTimelineView;

		self.timelines_layout = GridLayout.rows(*
			self.timelines.collect({ arg x; 
				x.makeLayout;
				[
					x.track_header,
					x.layout,
				]
			})
		).spacing_(0).margins_(0);
		self.timelines_layout.setColumnStretch(0,0);
		self.timelines_layout.setColumnStretch(1,1);

		self.load_timelines_properties;

		timeline = stepTimelineView.timeline;

		self.stepTimelineView_header.removeAll;
		self.stepTimelineView_header.layout = HLayout(
			stepTimelineView.edit_button,
			stepTimelineView.drumrack_button,
		);


		self.timelines_view.removeAll;
		self.timelines_view.layout = self.timelines_layout;

		self.timelines.reduce({ arg a, b; b.timeline.mimicTimeline(a.timeline, \horizontal); a });
		self.ruler_x.mimicTimeline(timeline);
		self.locatorbar.mimicTimeline(timeline);
		self.hrange.mapTimeline(timeline);
		
	},

	makeLayout: { arg self;
		var layout;
		var header_layout;
		var vrange, hrange;
		var eventlist;
		var ruler_x, ruler_y, sizer;
		var locatorbar;
		var timelines_scrollview;

		ruler_x = TimelineRulerView.new;
		self.ruler_x = ruler_x;
		ruler_x.view.minSize = Point(10,10);

		locatorbar = TimelineLocatorBarView.new;
		self.locatorbar = locatorbar;
		locatorbar.view.minSize = Point(10,15);

		hrange = TimelineScroller.new;
		self.hrange = hrange;
		hrange.orientation = \horizontal;

		self.timelines_view = View.new;
		self.stepTimelineView_header = View.new;

		self.refresh_timelines_view;


		timelines_scrollview = ScrollView.new.canvas_(self.timelines_view);
		timelines_scrollview.hasBorder = false;
		timelines_scrollview.hasVerticalScroller = false;
		self.timelines_scrollview = timelines_scrollview;

		vrange = self.make_vrange;

		eventlist = self.stepTimelineView.eventList;

		self.stepTimelineView.mapEventList(eventlist);
		locatorbar.mapEventList(eventlist);


		sizer = 
			HLayout(
				XSimpleButton.new.label_("<").action_({
					var point = 0 - Point(4,0);
					var timeline = self.stepTimelineView.timeline;
					timeline.areasize = timeline.areasize + point;
					timeline.refresh;
				}).fixedSize_(Point(10,20)),
				XSimpleButton.new.label_(">").action_({
					var point = Point(4,0);
					var timeline = self.stepTimelineView.timeline;
					timeline.areasize = timeline.areasize + point;
					timeline.refresh;
				}).fixedSize_(Point(10,20)),
			).spacing_(0)
		;


		header_layout = HLayout (
			PlayerWrapper(self.model.xasPattern).asView,
			XSimpleButton.new.label_("Add track").action_({
				WindowLayout({ arg win;
					var params = self.model.get_params;
					var popupmenu;
					params = params.reject({ arg p; 
						// reject already included
						self.model.mod_timelines.any({ arg tl; tl.param == p})
					});
					popupmenu = PopUpMenu.new.items_(params.collect(_.getHalo(\label)));
					VLayout(
						popupmenu,
						XSimpleButton.new.label_("OK").action_({
							win.close;
							self.model.add_modulator(params[popupmenu.value]);
							self.refresh_timelines_view;
						}),
						nil,
					)
				})

			}),
			XSimpleButton.new.label_("Remove track").action_({
				(self.track_selector.selected_index-1).debug("remove track");
				self.model.remove_modulator(self.track_selector.selected_index-1);
				self.refresh_timelines_view;
			}),
			XSimpleButton.new.label_("Bypass track").action_({
				self.model.toggle_bypass_modulator(self.track_selector.selected_index-1);
				self.refresh_timelines_view;
			}),
			self.stepTimelineView_header,
			self.make_quant_popup,
		);


		layout = VLayout (
			header_layout,
			GridLayout.rows(
				[
					timelines_scrollview,
					vrange,
				],
				[
					hrange.view,
					sizer,
				]
			).spacing_(0);
		);



		self.layout = layout;
		layout;
	},
);

~modStepTimeline.all.loadIfNotInitialized;

/////

~modStepTimelineBank = ProtoClass((
	all: { PresetDictionary(\modStepTimelineBank) },
	new: { arg self, key;
	
		//if(self.all[key].isNil) {
		if(true) {
			self = self.deepCopy;
			self.prAdd(key).init;
		} {
			self = self.all[key]
		};
	
		self;
	},

	prAdd: { arg self, key;
		self.all[key] = self;
		self.key = key;
	},

	init: { arg self;
		self.drumrack = DrumRack(self.key);
		self.bank = BankList.new;
		self.bank.add(self.new_item);
	},

	drumrack_: { arg self, dr;
		self.bank.do({ arg x; x.drumrack = dr });
	},

	add_item: { arg self;
		self.bank.add(self.new_item);
	},

	new_item: { arg self;
		var item = ~modStepTimeline.newInstance;
		item.drumrack = self.drumrack;
		item;
	},

	asPattern: { arg self;
		Plazy({ self.bank.current.xasPattern });
	},

	edit: { arg self;
		WindowDef(\modStepTimelineBank_+++self.key, {
			var bankselector = PopUpMenu.new;
			var itemView = View.new;
			var updateItemView = {
				if(self.bank.current.editor.notNil) {
					self.bank.current.editor.save_timeline_properties;
				};
				itemView.removeAll;
				itemView.layout = self.bank.current.asView;
				self.bank.current.editor.load_timeline_properties;
				bankselector.mapParam(Param(Message(self.bank), \index, MenuSpec((0..self.bank.size-1))));
			};
			bankselector.onChange(self.bank, \index, {
				updateItemView.()
			});
			updateItemView.();
			VLayout(
				HLayout(
					bankselector,
					XSimpleButton.new.label_("+").action_({ 
						self.add_item;
						updateItemView.();
					}),
					XSimpleButton.new.label_("-"),
				),
				itemView,
			)
		}).front;
	},
));
