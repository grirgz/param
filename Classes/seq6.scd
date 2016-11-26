// add env timelines
(

ParamProto.init;

~modStepTimeline = (
	parent: ~eventTimeline,
	all: { PresetDictionary(\modStepTimeline) },
	new: { arg self, name;
	
		self = self.deepCopy;
		self.prAdd(name).init;
		
	
		self;
	},

	init: { arg self;

		self.stepTimeline = ~stepTimeline.newInstance(self.key);
		self.mod_timelines = List.new;
		
	},

	add_modulator: { arg self;
		var param;
		var timeline;
		param = Param(BusDef(\plop, \control), \fratio, ControlSpec(0.1,5,\lin));
		timeline = ~envTimeline.newInstance(param);
		self.mod_timelines.add( timeline );
	},

	drumrack_: { arg self, drumrack;
		self.stepTimeline.drumrack = drumrack;
	},

	drumrack: { arg self;
		self.stepTimeline.drumrack
	},

	edit: { arg self;
		~modStepTimelinePanelView.new(self).makeWindow;
	},

);

~modStepTimelinePanelView = (
	new: { arg self, model;
		self = self.deepCopy;
	
		self.model = model;
		
	
		self;
	},

	makeLayout: { arg self;
		self.timeline_views = View.new;
		self.layout = VLayout(
			HLayout(
				XSimpleButton.new.label_("add mod").action_({
					//var param = Param()
					self.model.add_modulator;
					self.refresh_timelines;
				})
			),
			ScrollView.new.canvas_(self.timeline_views),
		);

		self.refresh_timelines;

		self.layout;
	},

	refresh_timelines: { arg self;
		self.timeline_views.removeAll;
		self.timeline_views.layout = VLayout(
			* [
				~stepTimelinePanelView.newView(self.model.stepTimeline).make_header_(nil).makeLayout,
			] ++
			self.model.mod_timelines.collect({ arg timeline;
				~envTimelinePanelView.newView(timeline).make_header_(nil).makeLayout
			})
		)
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
		
	},

);


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
		self.areasize = 10@1;
		self.eventloop = (list: el, key: { self.key }); // fake eventloop for the moment
		self.quant_ref = Ref(Point(1/8,1)); // TODO: replace by Quant class and change TimelineView code
		self.proxy = EventPatternProxy.new;
		self.cursor = CursorTimeline.new;
		self.cursor.startPosition = self.eventList.startTime;
		"=============clipTimeline: end init".debug;

		self;
	},

	xasPattern: { arg self, relStartTime, totalDur;
		self.proxy.source = Plazy({
			var eventlist = self.eventList;

			relStartTime = relStartTime ? eventlist.relStartTime;
			totalDur = totalDur ? eventlist.totalDur;

			[relStartTime, totalDur].debug("clip xasPattern: plazy: relstart, totdur");

			self.eventloop.list.changed(\cursor, \play);
			Pfindur(totalDur, ~timeline_pattern.(
				Pn(eventlist, 1) <> (drumrack: self.drumrack),
				//Pn(eventlist, 1),
				relStartTime
			));
		}).repeat(self.repeatCount);
		self.proxy;
	},

	edit: { arg self;
		~stepTimelinePanelView.new(self);
	},

	initStepEvent: { arg self, ev;
		ev.putAll(~event_type_pattern);
		ev[\stepseq_page] = ~stepseq_page.new;
		ev[\label] = { arg me; me.stepseq_page.label };
		ev[\edit] = { arg me, drumrack; me.stepseq_page.edit(drumrack) };
		ev[\pattern] = { arg me; 
			Ppar(
				me.stepseq_page.asPatternList.collect({ arg pat,i;
					~pdemul.(
						PchainT(
							pat, 
							Plazy({
								me[\drumrack].debug("event drumrack ?");
								if(me[\drumrack].notNil) {
									[i,me[\drumrack].get_pad(i)].debug("drum i");
									me[\drumrack].get_pad(i).proxy.keep(1);
									//Pbind(
									//	\instrument, \default,
									//	\freq, 400,
									//).keep(1);
								} {
									Ptuple([Pbind().keep(1)])
								}
							}).loop
						)
					)
				})
			)
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

		self.layout = VLayout(
			//HLayout(
			//	PlayerWrapperView(Pdef(self.eventloop.key)).view,
			//	XSimpleButton.new.label_("yep"),
			//),
			[self.parent[\makeLayout].(self), stretch:1],
			HLayout (
				drumrack_button,
				edit_button,
			),
			
			//self.clipEditorLayout,
			//self.parent.makeLayout,
		);

		self.timeline.mouseDownAction = { arg me, px, py, mod, buttonNumber, clickCount, chosennode;
			self.last_pixelpos = Point(px,py);
			if(chosennode.notNil) {
				self.selected_event = chosennode.model;
				edit_button.background_(ParamViewToolBox.color_ligth)
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

~stepseq_page = (
	new: { arg self;
		self = self.deepCopy;
	
		
		self.lines = List.newFrom(
			16.collect({
				var se = StepEvent.new;
				se[\isRest] = StepList[0];
				se[\isRest].addSpec(\list, \unipolar.asSpec);
				//se[\isRest].bypass = 1;
				se;
			})
		);
		self.label = "No name";
	
		self;
	},

	clone: { arg self;
		var new = ~stepseq_page.new;
		new.lines = self.lines.deepCopy;
		new.lines.do { arg line;
			line[\isRest].addSpec(\list, \unipolar.asSpec);
		};
		new;
	},

	makeLayout: { arg self;
		var drumrack = self.drumrack;
		var drkey = drumrack !? { drumrack.key } ? "";
		VLayout (
			HLayout (
				TextField.new.string_(self.label).action_({ arg view;
					self.label = view.string;
				}),
				XSimpleButton.new.label_("Edit DrumRack " ++ drkey).action_({
					if(self.drumrack.notNil) {
						self.drumrack.edit;
					}
				}),
			),
			ScrollView.new.canvas_(View.new.layout_(
				VLayout (
					* self.lines.collect({ arg stepevent, i;
						HLayout (
							StaticText.new.string_(i).minWidth_(20),
							~stepEventCoinView.new(stepevent).asView
						)
					})
				)
			
			))
		)
	},

	makeWindow: { arg self;
		var window = Window.new;
		var layout;
		layout = self.makeLayout;
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
		
	},

	asPatternList: { arg self;
		self.lines.collect({ arg line;
			line.asPattern.loop
		})
	},

	edit: { arg self, drumrack;
		self.drumrack = drumrack;
		self.makeWindow;
	},
);

	~pdemul = { arg pat;

		Prout({ arg proutinval;
			var str;
			str = pat.asStream;

			"what".debug;
			pat.debug;
			inf.do{ 
				var inval = str.next(proutinval);
				inval.debug("inval");
				inval.drop(-1).do { arg in;
					var in1 = in.copy;
					"hhh".debug;
					in1[\delta] = 0;
					in1.yield;
				};
				inval.last.debug("what").yield(Event.default);
			}
		}).trace
	};

//~st = ~stepTimeline.new(\bop);
//~st.drumrack = DrumRack(\yep);
//~st.edit;

~st = ~modStepTimeline.new(\wap);
~st.drumrack = DrumRack(\yep);
~st.edit;

)
