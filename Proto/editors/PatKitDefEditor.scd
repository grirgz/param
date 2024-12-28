
(

WindowDef(\PatKitDefEditor, (
	asView: { arg self, def, group, selectidx=0;
		var size = self.kitsize;
		var layout;
		var selector = List.new;
		var select, deselect;

		//"3".debug;
		self.def = def;
		self.pgroup = group;
		self.selector = List.new;
		//group.defaultTarget = self.default_stepevent;
		self.editlayout = View.new;
		self.makeKitLayout;
		//"4".debug;
		self.editCell(selectidx);
		self.defineBindings;

		def.windowName = "%: %".format("PatKitDefEditor", self.pgroup.key);

		//"6".debug;
		layout = HLayout (
			[VLayout (
				HLayout(
					self.make_saveas_button,
					self.make_loadfrom_button,
					self.make_main_menu_button,
				),
				self.kitlayout,
			), stretch:0],
			[self.editlayout, stretch:10],
			nil,
		);

		layout;
	},

	make_saveas_button: { arg self, def;
		var model = self.pgroup;
		BasicButton.new.fixedWidth_(100)
		.action_({ arg me;
			if(model.presetCompileStringSavePath.notNil) {
				var dialog;
				dialog = Window.new("Save").layout_(
					VLayout(
						StaticText.new.string_("Save this object to the file % ?".format(model.presetCompileStringSavePath.asCompileString)),
						HLayout(
							nil,
							BasicButton.new.string_("OK").action_({ 
								model.savePresetCompileString;
								me.refreshChange;
								dialog.close;
							}),
							BasicButton.new.string_("Cancel").action_({
								dialog.close;
							}),
						)
					)
				).setInnerExtent(600,140).front;
			} {
				model.savePresetCompileStringDialog(nil, {
					me.refreshChange;
				}, false);
			};
		})
		.mouseUpAction_({ arg me, x, y, mod, but;
			//[a,b,c,but].debug("but");
			if(but == 1) { // right click to force save as
				model.savePresetCompileStringDialog(nil, {
					me.refreshChange;
				}, true)
			}
		})
		.refreshChangeAction_({ arg me;
			if(model.presetCompileStringSavePath.notNil) {
				me.string = "Save"
			} {
				me.string = "Save as"
			};
		})
		.refreshChange
		;
	},

	make_loadfrom_button: { arg self;
		BasicButton.new.string_("Load preset").action_({
			WindowDef(\GlobalLibrary_select).front(nil, { arg libid;
				self.pgroup.loadFrom(libid.last.value);
				self.refreshCells;
			}, \Kit)
		})
		
	},

	make_main_menu_button: { arg self;
		BasicButton.new.string_("...").fixedWidth_(20).action_({
			Menu(
				MenuAction("Clear all cells", {
					self.pgroup.clearChildren;
				}),
				//MenuAction("Basic Recorder", {
					//FileSystemProject.loadOnce("libdef/basicrecorder.scd");
					//ProtoDef(\basicrecorder).edit;
				//}),
				//MenuAction("Record factory", {
					//FileSystemProject.loadOnce("libdef/wip/RecordFactory.scd");
					//ProtoDef(\RecordFactory).edit;
				//}),
				Param(
					Message(self.pgroup),
					\controller, 
					ControllerDef.getTagSpec(\drumpad),
					//TagSpecDef(\NoteTimeline_controller),
				).asMenu("Controller"),
				//MenuAction("Recorder", {
					//ProtoDef(\recorder).edit // seems broken, in ex2/recordgui.scd
				//}),
			).front
		})
		
	},

	defineBindings: { arg self;
		var padkeys;
		var keyDownAction, keyUpAction;
		var def = self.def;
		padkeys = "&X\"'azerqsdfwxcv(-X_tyuighjkbn,;".as(Array);
		padkeys[1] = -23.asAscii;
		padkeys[1+16] = -24.asAscii;

		keyDownAction = { arg me, key, modifiers, unicode, keycode;
			var group = self.pgroup;
			var selected = self.selected;
			[me, key.asCompileString, modifiers, unicode, keycode].debug("samplekit keyDownAction");
			case
			{ modifiers.isCtrl and: { keycode == 99 } } { // ^c
				"copy!".debug;
				ParamViewToolBox.clipboard.windowdef_samplekit = group.elAt(selected).copy;
			}
			{ modifiers.isCtrl and: { keycode == 118 } } { // ^v
				"paste!".debug;
				if(ParamViewToolBox.clipboard.windowdef_samplekit.notNil) {
					group.pasteChild(selected, ParamViewToolBox.clipboard.windowdef_samplekit);
					//group.reboot;
					self.editCell(selected);
				};
			}
			{ modifiers == 0 and:{ padkeys.includes(key) }} {
				var idx = padkeys.indexOf(key);
				"play!".debug(idx);
				group.elAt(idx).play;
			}
			;
		};
		keyUpAction = { arg me, key, modifiers, unicode, keycode;
			var group = self.pgroup;
			[me, key.asCompileString, modifiers, unicode, keycode].debug("samplekit keyUpAction");
			case
			{ modifiers == 0 and:{ padkeys.includes(key) }} {
				var idx = padkeys.indexOf(key);
				"stop!".debug(idx);
				group.elAt(idx).stop;
			}
			;

		};

		if(self.def.window.notNil) {
			def.window.view.keyDownAction = keyDownAction;
			def.window.view.keyUpAction = keyUpAction;
		};
		
	},

	editCell: { arg self, idx;
		var drum = self.pgroup.elAt(idx);
		"ed1".debug;
		self.selectCell(idx);
		"ed2".debug;
		self.editlayout.removeAll;
		self.editlayout.layout = VLayout(
			//WindowDef(\vertical_paramgroup).asView(drum.target.asParamGroup)
			WindowDef(\PatKitDefChildEditor).asView(drum)
		);

	},

	refreshCells: { arg self;
		self.selector.do { arg cell, idx;
			if(self.selected == idx) {
				self.selectCell(idx)
			} {
				self.deselectCell(idx)
			};
		};
		
	},

	selectCell: { arg self, x;
		self.deselectCell(self.selected);
		self.selected = x;
		self.selector[x].background_(ParamViewToolBox.color_dark);
		//self.pgroup.reboot;
	},

	deselectCell: { arg self, x;
		var tar;
		//"x1".debug;
		//tar = group.elAt(x).target;
		//"x2".debug;
		self.selector[x].background = if(self.pgroup.elAt(x).isEmpty) {
			Color.white
		} {
			ParamViewToolBox.color_ligth;
		};
		//"x3".debug;
	},

	//default_stepevent: { StepEvent.newFrom((instrument: \playersec)) },
	selected: 0,
	kitsize: Point(4,8),

	makeKitLayout: { arg self;
		self.kitlayout = VLayout (*
			self.kitsize.y.collect({ arg y;
				HLayout(*
					self.kitsize.x.collect({ arg x;
						var idx = y*self.kitsize.x+x;
						var button;
						//"2.3".debug;
						button = BasicButton.new.string_(idx).fixedSize_(50@50)
						.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
							[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
							//"3".debug;
							if(buttonNumber == 0) {
								self.pgroup.elAt(idx).play;
							};
							if(buttonNumber == 1) {
								self.editCell(idx);
							};
						})
						//.keyDownAction_(keyDownAction)
						.action_({
							self.pgroup.elAt(idx).stop;
						});
						//"2.5".debug;
						self.selector.add(button);
						self.deselectCell(idx);
						//"2.6".debug;
						button;
					})
					++ [ nil ]
				)
			})
			++ [ nil ]
		);
		
	},
));

WindowDef(\PatKitDefChildEditor, { arg def, child, show_buttons=true, player;
	// FIXME: no support for \mbufnum
	var pkey = \sampler;
	var anchor;
	var sndfileview;
	var layout;
	var updatebuffer;
	var buffer_popup = PopUpMenu.new;
	var buffer;
	var oldbuf;
	var paramgroup;
	var proxy;
	var isBufferTarget = true;
	var pgview;
	var refresh_pgview;
	var instrumentParam;
	var loadbutton;
	var bufcontroller;

	child.targetData.debug("targetData!!!!!");

	if(child.isEmpty) {
		//child.targetData.addHalo(\instrument, \playersec);
	};

	proxy = child.targetDataProxy;

	player = player ?? { 
		Pdef(\PatKitDef_test_player, child.pattern)
	};

	anchor = buffer_popup;

	sndfileview = View.new;
	sndfileview.fixedHeight = 100;

	// param group view
	pgview = View.new;
	refresh_pgview = {
		var bufnumname;
		pgview.removeAll;
		paramgroup  = ( child.asParamGroup ?? { ParamGroup([]) } )
		.add(Param(proxy, \sustain))
		.add(Param(proxy, \lag))
		;

		paramgroup.do { arg param, idx;
			if(param.property == \bufnum) {
				bufnumname = \bufnum;
			};
			if(param.property == \mbufnum) {
				bufnumname = \mbufnum;
			};
		};
		paramgroup = paramgroup.rejectByKey([\out, \bufnum]);
		pgview.layout = paramgroup.asView;

		if(bufnumname.isNil) {
			sndfileview.removeAll;
			buffer_popup.enabled = false;
			loadbutton.enabled = false;
		} {
			buffer = Param(proxy, bufnumname, TagSpec(
				BufDef.all.keys.as(Array).sort.collect({ arg x; x -> BufDef(x) })
			));
			oldbuf = nil;
			updatebuffer.();
			buffer_popup.enabled = true;
			loadbutton.enabled = true;
			buffer_popup.mapParam(buffer);
			bufcontroller.remove;
			bufcontroller = buffer.onChange(updatebuffer, anchor);
		};
	};
	instrumentParam = Param(Message(child), \instrument, TagSpecDef(\SynthDef_keys));

	//

	layout = VLayout (*
		if(show_buttons == true) {
			[
				HLayout (
					PlayerWrapper(player).asView,
					Param(proxy, \out, TagSpecDef(\BusDef_object)).asPopUpMenu,
				),
			]
		} {
			[]
		}
		++
		[
			sndfileview,
			HLayout (
				//Param(target, \instrument, MenuSpec(
				//	~sampler_synthdef_list ? [\playersec]
				//)).asPopUpMenu,
				//Param(proxy, \instrument, TagSpecDef(\patkitdef_synthdefs)).asPopUpMenu,
				instrumentParam.asPopUpMenu,
				buffer_popup.addAction({
					// workaround because proxy doesnt update for buffer
					player.reset;
				}),
				loadbutton = BasicButton.new.string_("load").fixedWidth_(50).action_({ arg view;
					//Dialog.openPanel({ arg file;})
					WindowDef(\filedialog).front("./", { arg file;
						BufDef.stereo(file.asSymbol); // autoload it
						buffer.spec = MenuSpec(
							BufDef.all.keys.as(Array).sort.collect({ arg x; x -> BufDef(x) })
						);
						//buffer.spec.labelList.debug("labellist");
						//buffer_popup.items.debug("poplabellist");
						buffer.set(BufDef.stereo(file.asSymbol));
						player.reset;
						//buffer_popup.refreshChange;
					});
				})
			),
			pgview,
		]
	);
	//anchor.debug("anchor");
	updatebuffer = {
		var buf;
		if(buffer.isNil) {
			buf = nil
		} {
			buf = buffer.get;
		};
		if(buf.isKindOf(Integer)) {
			var path;
			path = TagSpecDef(\BufDef).unmapKey(buf);
			if(path.notNil) {
				buf = BufDef(path)
			}
		};
		buf.debug("buf");
		if(buf != 0 and: buf.notNil and: {oldbuf != buf}) {
			sndfileview.removeAll;
			sndfileview.layout = VLayout(WindowDef(\SoundFileView).asView(
				buf,
				Param(proxy, \pos),
				Param(proxy, \sustain),
				nil,
				nil,
				\seconds,
				\seconds,
				\seconds,
			));
			oldbuf = buf;
		}
	};
	updatebuffer.();
	instrumentParam.onChange({
		refresh_pgview.();
	}, pgview);
	refresh_pgview.();
	layout;
});

// compat
WindowDef(\samplekit, WindowDef(\PatKitDefEditor));
WindowDef(\sampler, WindowDef(\PatKitDefChildEditor));

);
