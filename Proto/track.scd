
"loading track.scd".debug;

~clipType_pat = ProtoClass((
	new: { arg self, parent;
		self = self.deepCopy;
	
		self.myparent = { parent };
		
	
		self;
	},

	play: { arg self;
		if(self.source.notNil) {
			self.source.play;
		}
	},
	
));

~trackType_pdef = ProtoClass((
	new: { arg self, prefix;
		self = self.deepCopy;
	
		
		self.prefix = prefix;
		self.list = SparseArray.new;
		self.proxy = EventPatternProxy.new;
		self.selected = 0;
	
		self;
	},

	destructor: {},

	initialize: { arg self, inst;
		inst.list = SparseArray.new;
	},

	atChild: { arg self, idx;
		if(self.list[idx].isNil) {
			self.list[idx] = ~cliptype_pat.new;
		};
		self.list[idx]
	},

	play: { arg self;
		if(self.list.size > 0) {
			self.list[self.selected].play;
		}
	},

	stop: { arg self;
		if(self.list.size > 0) {
			self.list[self.selected].stop;
		}
	},

	putChild: { arg self, idx, clip;
		if(self.list[idx].isNil) {
			self.list[idx] = ~cliptype_pat.new;
		};
		self.list[idx].source = clip;
	},
));


///////////// TrackType

~trackType_base = ProtoClass((
	label: { arg self;
		""
	},

	asPlayerView: { arg self;
		//VLayout

		
	},

	collectChildren: { arg self, fun;
		self.list.collect(fun);
	},

	hasChildren: { arg self;
		self.list.notNil and: { self.list.size > 0 }
	},

	doChildren: { arg self, fun;
		self.list.do(fun);
	},
));

~trackType_PlayerWrapper = ProtoClass((
	parent: ~trackType_base,
	protoclass: \trackType_PlayerWrapper,
	new: { arg self, player;
		self = self.deepCopy;
	
		
		self.parent = ~trackType_base;
		self.parent.debug("parent of trackType_PlayerWrapper");
		self.list = SparseArray.new;
		self.player = PlayerWrapper(nil);
		self.source = nil;
		self.selected = 0;
	
		self;
	},

	destructor: {},

	newChild: { arg self;
		//TrackDef.newInstance(~trackType_PlayerWrapper.new);
		~cliptype_pat.new;
	},

	initialize: { arg self, inst;
		inst.list = SparseArray.new;
	},

	source_: { arg self, src;
		self.player.target = src;
		//self.player = PlayerWrapper(src);
	},

	source: { arg self, src;
		if(self.player.notNil) {
			self.player.target;
		} {
			nil;
		}
	},

	atChild: { arg self, idx;
		idx.debug("atChild: fuck");
		if(self.list[idx].isNil) {
			idx.debug("atChild: fuck nil");
			self.list[idx] = self.newChild;
		};
		self.list[idx]
	},

	play: { arg self;
		self.player.play;
	},

	stop: { arg self;
		self.player.stop;
	},

	label: { arg self, key;
		self.player.label
	},

	isPlaying: { arg self;
		self.player.isPlaying;
	},

	putChild: { arg self, idx, clip;
		if(self.list[idx].isNil) {
			self.list[idx] = self.newChild;
		};
		self.list[idx].source = clip;
	},

	asView: { arg self;
		var x = self.source;
		var vi;
		vi = case
			{ x.isKindOf(Pdef) } {
				//ParamGroupLayout.two_panes(x.asParamGroup, \property)
				ParamGroupLayout.inline_groups(x.asParamGroup, \property)
			}
			{ x.isKindOf(Ndef) } {
				//ParamGroupLayout.two_panes(x.asParamGroup, \property)
				ParamGroupLayout.inline_groups(x.asParamGroup, \property)
			}
			{
				HLayout(
					XSimpleButton.new.label_("xx");
				)
			}
		;
		vi
	},
));

///////////// TrackGroupType

~trackGroupType_PlayerWrapper = ProtoClass((
	protoclass: \trackGroupType_PlayerWrapper,

	new: { arg self, plist;
		self = self.deepCopy;
	
		self.parent = ~trackType_base;
		self.parent.debug("parent of trackGroupType_PlayerWrapper");
		self.list = SparseArray.new;
		self.source = plist;
	
		self;
	},

	source_: { arg self, src;
		src.do { arg x, idx; 
			self.putChild(idx, x)
		};
	},

	source: { arg self;
		self.list;
	},

	newChild: { arg self;
		TrackDef.newInstance(~trackType_PlayerWrapper.new);
	},

	atChild: { arg self, idx;
		idx.debug("atChild: fuck");
		if(self.list[idx].isNil) {
			idx.debug("atChild: fuck nil");
			self.list[idx] = self.newChild;
		};
		self.list[idx]
	},

	play: { arg self;
		self.list.do{ arg x; x.play  };
	},

	stop: { arg self;
		self.list.do{ arg x; x.stop  };
	},

	isPlaying: { arg self;
		self.list.any{ arg x; x.isPlaying  };
	},

	putChild: { arg self, idx, clip;
		if(self.list[idx].isNil) {
			self.list[idx] = self.newChild;
		};
		self.list[idx].source = clip;
	},

	destructor: {},

	edit: { arg self;
		~player_grid_view.(self.me, 8@8, \TrackGroupDef_+++self.me.key);
	},
));

///////////// views

~track_player_view = (
	// deprecated by PlayerWrapperSelectorView
	new: { arg self, track;
		self = self.deepCopy;
		
		self.track = track;
	
		self;
	},

	asView: { arg self;
		var view;
		var p = PlayerWrapper(self.track);
		var pv = PlayerWrapperView(p);
		pv.label = "";
		view = View.new.layout_(HLayout (
			pv.view.button.maxWidth_(30),
			StaticText.new.string_(p.label).onChange(p, \changed, {
				view.string_(p.label)
			}),
		).margins_(0)).background_(ParamView.color_ligth).maxSize_(200@30).mouseDownAction_({ 
			arg xview, x, y, modifiers, buttonNumber, clickCount;
			[xview, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			self.select_hook;
		});
		self.view = view;
		view.addUniqueMethod(\isSelected, { arg x; self.selected });
		view.addUniqueMethod(\isSelected_, { arg x; self.selected = x });
		view
	},

	selected: { arg self;
		self.pr_selected;
	},

	selected_: { arg self, val;
		self.pr_selected = val;
		if(val == true) {
			self.view.background_(ParamView.color_dark)
		} {
			self.view.background_(ParamView.color_ligth)
		}
	},


	select_hook: { arg self;
		self.view.background_(ParamView.color_dark);
	},
);

~trackgroup_view = { arg trackgroup;
	WindowLayout({

		GridLayout.rows(
			*trackgroup.collect { arg track;
				track.debug("track");
				[
					~track_player_view.new(track).asView,
				] ++ 
				track.collect { arg clip;
					clip.debug("clip");
					~track_player_view.new(clip).asView;
				}
			}
		)
	})
};

~player_grid_view = { arg trackgroup, size = (8@8), name;
	WindowDef(name, { arg win;

		var grid;
		var listview, clipview;
		var selector;
		var selector_views = List.new;
		var select_item_action, track_to_key;
		var listviewdict;
		var selected_track, selected_index;
		var trackstream;

		var refresh_menu_list = {
			if(listviewdict.notNil) {
				listviewdict.values.do { arg val; listview.removeItem(val) }
			};
			listviewdict = Dictionary.new;
			Pdef.all.keys.asArray.collect({ arg x; x.asString }).sort.do { arg lab;
				var key;
				lab = lab.asSymbol;
				key = [lab];
				listviewdict[key] = listview.addItem(key);
			};

		};

		win.view.focusGainedAction = {
			"refresh_menu_list".debug;
			refresh_menu_list.();
		};

		trackgroup.debug("first trackgroup");
		trackstream = Routine({
			trackgroup.debug("trackgroup");
			trackgroup.do { arg track;
				track.debug("routine track");
				track.yield;
			};
		}).asStream;


		track_to_key = { arg track;
			[track.label.asSymbol];
		};
		select_item_action = { arg track, idx;
			selected_track = track;
			selected_index = idx;
			if(track.isNil) {
				listview.currentItem = nil;
			} {
				listview.currentItem = listviewdict[track_to_key.(track)];
			}
		};

		selector = ~view_selector.new(size.x*size.y);
		selector.select_action = { arg self, view;
			"select_action".debug("!!!");
			view.selected = true;
		};
		selector.deselect_action = { arg self, view;
			"deselect_action".debug("!!!");
			view.selected = false;
		};

		grid = GridLayout.rows(
			*size.y.collect { arg i;
				size.x.collect { arg j;
					var tpv;
					var tpvv;
					var track;
					var idx = i*size.x + j;
					//track = trackstream.next;
					track = trackgroup[idx];
					track.debug("track");
					tpv = PlayerWrapperSelectorView(PlayerWrapper(track));
					tpv.pollRate = 4;
					
					tpv.selectAction = { arg self;
						clipview.removeAll;
						if(track.notNil) {
							clipview.layout = track.asView;
						};
						selector.select_item(idx);
						select_item_action.(track, idx);
					};
					tpvv = tpv.asView;
					tpvv.debug("TPVV");
					selector_views.add(tpvv);
					tpvv;
				}
			}
		);
		selector.views = selector_views;

		listview = TreeView.new;
		listview.maxWidth = 300;
		refresh_menu_list.();
		listview.beginDragAction = { arg x, y;
			//listview.drag??? = Pdef(listview.currentItem.strings[0].asSymbol);
			"plop";
		};
		listview.focusGainedAction = {
			"listview refresh_menu_list".debug;
			refresh_menu_list.();
		};
		listview.itemPressedAction = { arg ... args;
			//self.selected_track.source = 
			args.debug("onItemPressedAction");
			if(selected_track.isNil) {
				selected_track = trackgroup[selector.selected_index]
			};
			selected_track.source = Pdef(args[0].currentItem.strings[0].asSymbol);
		};

		clipview = View.new;

		HLayout(
			[listview, stretch:0],
			[VLayout(
				grid,
				clipview,
			), stretch:2]
		)
	}).front
};

