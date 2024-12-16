(
ProtoTemplateDef(\filemanager_node, (
	playable_extensions: [
		"wav",
		"mp3",
		"flac",
		"ogg",
	],
	new: { arg self, name;
		self = self.copy;
		//[name, name.class].debug("new name");
		if(name.isKindOf(String)) {
			name = PathName(name);
		};
		self.pathName = name;
		self;
	},

	extension: { arg self;
		self.pathName.extension
	},
	className: { arg self;
		ProtoTemplateDef(\filemanager_node);
	},
	name: { arg self;
		var name = self.pathName.fullPath;
		if(File.type(name) == \directory) {
			if(name.endsWith("/")) {
				self.pathName.folderName ++ "/"
			} {
				self.pathName.fileName ++ "/"
			}
		} {
			if(name.endsWith("/")) {
				self.pathName.folderName
			} {
				self.pathName.fileName
			}
		}
	},
	label: { arg self;
		self.name;
	},
	fullPath: { arg self;
		var path = self.pathName.fullPath;
		if(path.beginsWith("//")) {
			path.keep(1 - path.size)
		} {
			path
		}
	},

	play: { arg self;
		if(self.isFile and: { self.playable_extensions.includesEqual(self.pathName.extension) }) {
			// FIXME: need to write custom code to load file into buffer and play it on a proxy
			// then free buffer
			SoundFile(self.fullPath).play;
		}
	},
	typeLabel: { arg self;
		File.type(self.pathName.fullPath).asString;
	},
	splitPath: { arg self;
		var parts = [ "/" ] ++ self.fullPath.split($/).select(_ != "");
		parts.collect { arg p, idx;
			self.new(parts.keep(idx+1).join("/"))
		}
	},
	isFile: { arg self;
		File.type(self.pathName.fullPath) != \directory
	},
	isFolder: { arg self;
		File.type(self.pathName.fullPath) == \directory
	},
	isChoosable: { arg self;
		self.isFile
	},
	parentNode: { arg self;
		self.new(self.pathName.parentPath)
	},
	selectedIndex: 0,
	children: { arg self;
		self.pathName.entries.collect({ arg p; self.new(p) });
	},

	asString: { arg self;
		self.pathName.fullPath;
	},

));

WindowDef(\filedialog_generic, { arg def, path, callback, nodeClass, isSaving=false;
	// callback(file_full_path_to_open)

	var fileview, changepath, select_file;
	var pathview = View.new;
	var quicklist = ListView.new;
	var node;
	var update_pathview;
	var preview = View.new;
	var proto = ProtoDef(\filemanager_player);
	var name_field;
	var nodelist;
	var defaultFilename;
	nodeClass = nodeClass ?? { ProtoTemplateDef(\filemanager_node) };

	name_field = TextField.new;

	preview.onClose_({ if(proto.player.notNil) { proto.player.stop } });

	if(path.isNil) {
		path = proto.cwd ?? { try { PathName(Document.current.path).pathOnly } ? "~" }
	} {
		name_field.value = PathName(path).fileName;
		defaultFilename = PathName(path).fileName;
		if(File.exists(PathName(path).pathOnly)) {
			path = PathName(path).pathOnly;
		} {
			path = proto.cwd ?? { try { PathName(Document.current.path).pathOnly } ? "~" }
		};
	};

	if(def.window.notNil) {
		def.window.view.keyDownAction = { arg view, char, modifiers, u, k; 
			//[char, modifiers, u, k].debug("KEYBOARD INPUT");
			if( u == 27 ) { // Esc
				def.closeWindow
			};
		};
	};

	fileview = ListView.new;
	changepath = { arg view, path; // called with double click
		var newnode;
		//[path, nodeClass.keys].debug("changepath: ");
		newnode = nodeClass.new(path);
		if(newnode.isFolder) {
			node = newnode;
			proto.disable_play = true;
			nodelist = node.children;
			view.items = nodelist.collect(_.label);
			//view.items.debug("changepath items");
			{
				// hack because selectionAction is triggered even without valueAction_
				// and with a delay
				proto.disable_play = false;
			}.defer(1/4);
			//debug("end disable play");

			proto.cwd = node.fullPath;
			update_pathview.()
		} {
			proto.chooseNode(newnode, true);
		}
	};

	select_file = { arg view, path;
		var filenode;
		//path.debug("select_file: path:");
		if(path.notNil) {
			var selected;
			filenode = nodeClass.new(path);

			proto.disable_play = true;
			selected = view.items.indexOfEqual(filenode.label);
			if(selected.notNil) {
				view.value = selected;
			};
			proto.filenameview.string = filenode.fullPath.asString;
			{
				// hack because selectionAction is triggered even without valueAction_
				// and with a delay
				proto.disable_play = false;
			}.defer(1/4);
			Log(\Param).debug("end disable play file");
		}
	};

	fileview.enterKeyAction = { arg view;
		//[view.value, view.items].debug("select: ");
		if(view.items.size > 0) {
			changepath.(view, node.fullPath ++ view.items[view.value])
		}
	};
	fileview.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
		//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
		
		//[view.value, view.items].debug("double click select: ");
		if(clickCount == 2 and: {view.items.size > 0}) {
			changepath.(view, nodelist[view.value].fullPath)
		}
	});
	fileview.selectionAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
		var path;
		var newnode;
		//[view, x, y, modifiers, buttonNumber, clickCount].debug("selectionAction");
		if(view.value.notNil) {
			path = nodelist[view.value].fullPath;
			newnode = nodeClass.new(path);
			//newnode.extension.dump;
			//proto.playable_extensions.debug("ext");
			//[proto.auto_mode, proto.auto_mode == 1, newnode.isFile, proto.playable_extensions.includesEqual(newnode.extension.debug("ext"))].debug("sel");
			if(newnode.isChoosable) {
				proto.chooseNode(nodelist[view.value], proto.auto_mode == true);
			};
		}
	});

	update_pathview = {
		pathview.removeAll;
		pathview.layout = HLayout.new.spacing_(0);
		node.splitPath.do({ arg p, idx;
			//p.name.debug("update_pathview p");
			pathview.layout.add(
				BasicButton.new.minSize_(10).string_(p.label).action_({
					changepath.(fileview, node.splitPath[idx].fullPath)

				})
			)
		});
		pathview.layout.add(nil)
	};

	quicklist.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
		var item;
		//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
		
		//[view.value, view.items].debug("double click select: ");
		if(clickCount == 2 and: {view.items.size > 0}) {
			item = view.items[view.value];
			if(item == "Recent files") {
				proto.show_recent_files
			} {
				changepath.(fileview, item)
			};
		}
	});

	proto.update_quicklistview = { arg self;
		quicklist.items = proto.quicklist.select(_.notNil);	
	};
	proto.save_quicklist = { arg self;
		File.use(self.config_file, "w", { arg file;
			file.write(
				"ProtoDef(%).quicklist = List[\n%];\n".format(
					self.key.asCompileString,
					self.quicklist.collect(_.asCompileString).join(",\n")
				)
			);
			file.write(
				"ProtoDef(%).recent_files_list = List[\n%];\n".format(
					self.key.asCompileString,
					self.recent_files_list.collect(_.asCompileString).join(",\n")
				)
			)
		});
	};
	proto.load_quicklist = { arg self;
		if(self.config_file.notNil) {
			self.config_file.load;
		};
	};

	proto.check_name_field = { arg self, callback;
		var node = nodeClass.new(self.choosedNode.parentNode.fullPath +/+ name_field.string);
		if(node.isChoosable) {
			self.chooseNode(node);
			callback.();
		} {
			"Can't open: file doesn't exist: %".format(node.fullPath).error;
		}
	};

	proto.chooseNode = { arg self, node, alsoPlay=false;
		self.current_file = node.fullPath;
		self.choosedNode = node;
		name_field.string = node.name;
		self.filenameview.string = "[%] %".format(node.typeLabel, node.fullPath.asString);
		if(alsoPlay == true) {
			if(self.disable_play != true) {
				proto.playNode(node);
			};
		}
	};

	proto.playNode = { arg self, node;
		Buffer.read(Server.default, node.fullPath, action: { arg buf;
			self.player.stop;
			self.player.source = Pmono( \playersec,
				\bufnum, buf,
				\dur, 1/8,
			);
			self.player.play;
		})
		
	};

	proto.show_recent_files = { arg self;
		var newnode;
		//[path, nodeClass.keys].debug("changepath: ");
		proto.disable_play = true;
		fileview.items = proto.recent_files_list.as(Array);
		//view.items.debug("changepath items");
		{
			// hack because selectionAction is triggered even without valueAction_
			// and with a delay
			proto.disable_play = false;
		}.defer(1/4);
		//debug("end disable play");

		//proto.cwd = node.fullPath;
		//update_pathview.()
	};

	proto.recent_files_list = List[];

	proto.add_to_recent_files = { arg self, cwd, name;
		var filename;
		if(name.notNil) {
			filename = cwd +/+ name
		} {
			filename = cwd;
		};
		proto.recent_files_list.add(filename);
		proto.save_quicklist;
	};

	//proto.play_file = { arg self, file, node;
		//self.current_file = file;
		//if(self.disable_play != true) {

			//file.debug("new source");
			//self.filenameview.string = "[%] %".format(node.typeLabel, file.asString);
			//nodeClass.new(file).play;
			//self.player.source_(
			////Pmono( \playersec,
			////\bufnum, BufDef(file.asSymbol),
			////\dur, 1/8,
			////\gain, 1,
			////\amp, 0.1,
			////),
			//Pbind(
			//\instrument, \playersec,
			//\bufnum, BufDef(file.asSymbol),
			//\dur, (BufDef(file.asSymbol).duration ? 1) * ~t * Pseq([1],1),
			//\amp, 0.1,
			//),
			//);
			//self.player.play;
		//}
	//};

	proto.init = { arg self;
		//"wtf".debug;
		proto.config_file = Platform.userConfigDir +/+ "filemanager_quicklist.sc";
		proto.load_quicklist;
		proto.quicklist = proto.quicklist ? [
			"Recent files",
			path, 
			"/", 
			"~", 
			"~/Musique/",
			"~/Musique/sc/samplekit/",
			"~/Musique/sc/reckit/",
			"~/Musique/sc/samplekit/com/",
			"~/code/sc/seco/vlive/",
			"~/code/sc/seco/vlive/repeat/",
			"~/code/sc/seco/vlive/repeat/capsule/",
		].select(_.notNil);
		proto.update_quicklistview;
		self.player = self.player ?? { Pdef(\FileDialogPreview) };
		self.player.quant = 0;
		proto.playerwrapper = PlayerWrapper(proto.player);
		proto.auto_mode = proto.auto_mode ?? 0;
		//proto.auto_mode.debug("auto_modexx");
		changepath.(fileview, path);
		//"wut".debug;
		select_file.(fileview, proto.current_file);
		//defaultFilename.debug("+++++++++++++++++++++++++++++++++++ defaultFilename");
		if(defaultFilename.notNil) {
			//name_field.value.debug("+++++++++++++++++++++++++++++++++++ name_field");
			// FIXME: why i have to defer ?
			{
				name_field.value = defaultFilename;
			}.defer(1)
		};
	};
	/////////////////////////////////////////////

	proto.filenameview = StaticText.new.string_("...");
	proto.init;

	preview.layout = HLayout (
		Param(proto.player, \gain).asKnob,
		proto.playerwrapper.asView,
		Param(Message(proto), \auto_mode, XBoolSpec()).asButton("Auto"),
		[proto.filenameview, stretch:4],
			BasicButton.new.string_("OK").action_({
				if(isSaving) {
					var fpath = proto.cwd+/+name_field.value;
					if(File.exists(fpath)) { // FIXME: should use .exists from node proto to not break the API
						WindowDef(\ConfirmDialog).front("File already exists, are you sure you want to overwrite ?\n\n%".format(fpath), {
							proto.add_to_recent_files(proto.cwd, name_field.value);
							callback.(proto.cwd, name_field.value);
							def.closeWindow;
						})
					} {
						proto.add_to_recent_files(proto.cwd, name_field.value);
						callback.(proto.cwd, name_field.value);
						def.closeWindow;
					};
				} {
					proto.check_name_field {
						proto.add_to_recent_files(proto.current_file);
						callback.(proto.current_file);
						def.closeWindow;
					};
				}
			}).maxWidth_(50),
			BasicButton.new.string_("Cancel").action_({
				def.closeWindow;
			}).maxWidth_(50),
	);

	VLayout (
		HLayout (
			BasicButton.new.string_("+").action_({
				if(proto.quicklist.includesEqual(node.fullPath).not) {
					proto.quicklist = proto.quicklist.add(node.fullPath);
					proto.update_quicklistview;
					proto.save_quicklist;
					PathName("//bin").fullPath
				}
			}).maxWidth_(30),
			BasicButton.new.string_("-").action_({
				proto.quicklist.removeAt(quicklist.value ? 0);
				proto.update_quicklistview;
				proto.save_quicklist;
			}).maxWidth_(30),
			BasicButton.new.string_("Up").action_({
				changepath.(fileview, node.parentNode.fullPath)
			}).maxWidth_(50),
			BasicButton.new.string_("Mkdir").action_({
				WindowDef(def.key+++\MkdirDialog, WindowDef(\RenameDialog)).front("What is the new folder name ?", "", { arg name;
					if(name != "") {
						(node.fullPath +/+ name).mkdir;
						(node.fullPath +/+ name).debug("folder created");
						changepath.(fileview, node.fullPath); // refresh
					} {
						name.debug("invalid name");
					}
				})
			}).maxWidth_(50),
			pathview,

		),
		name_field,
		HLayout (
			quicklist,
			fileview,
		),
		preview,
	);

});

WindowDef(\filedialog, { arg def, path, callback, nodeClass;
	WindowDef(\filedialog_generic).embedView(def, path, callback)
});

WindowDef(\filedialog_save, { arg def, path, callback, nodeClass;
	WindowDef(\filedialog_generic).embedView(def, path, callback,nil,true)
});


ProtoTemplateDef(\filemanager_node_sample, ProtoTemplateDef(\filemanager_node)).putAll((
	className: { arg self;
		ProtoTemplateDef(\filemanager_node_sample);
	},

	isChoosable: { arg self;
		self.playable_extensions.includesEqual(self.extension);
	},

	children: { arg self;
		//"12".debug;
		self.pathName.entries.collect({ arg p; 
			var entry = self.new(p);
			if(entry.isChoosable or: {entry.isFolder}) {
				entry;
			} {
				nil
			}
		}).select(_.notNil);
	},
));
WindowDef(\filedialog_sample, { arg def, path, callback;
	WindowDef(\filedialog_generic).embedView(def, path, callback, ProtoTemplateDef(\filemanager_node_sample))
});
WindowDef(\filedialog_insert_bufdef, { arg def, path;
	// FIXME: not recursive ?!
	//WindowDef(\filedialog_sample).embedView(def, nil,{ arg path ...args; 
		//BufDef(path).asCompileString.editorInsert(false)
	//});
	WindowDef(\filedialog_generic).embedView(def, path, { arg path ...args;
		BufDef(path).asCompileString.editorInsert(false)
	}, ProtoTemplateDef(\filemanager_node_sample))
});

);

(
// TODO: add escape key shortcut

WindowDef(\OkDialog, { arg me, question, okfun;
	me.windowName = "Info: " ++ question;
	VLayout (
		StaticText.new.string_(question),
		HLayout(
			nil,
			BasicButton.new.string_("Ok").action_({ 
				okfun.();
				me.closeWindow;
			}),
		)
	)
});

WindowDef(\ConfirmDialog, { arg me, question, savefun, cancelfun;
	me.windowName = "Confirm: " ++ question;
	VLayout (
		StaticText.new.string_(question),
		HLayout(
			nil,
			BasicButton.new.string_("Yes").action_({ 
				savefun.();
				me.closeWindow;
			}),
			BasicButton.new.string_("Cancel").action_({
				cancelfun.();
				me.closeWindow;
			}),
		)
	)
});

WindowDef(\RenameDialog, { arg me, question, defaultValue="", savefun, cancelfun;
	var textfield;
	me.windowName = question;
	VLayout (
		StaticText.new.string_(question),
		textfield = TextField.new.string_(defaultValue),
		HLayout(
			nil,
			BasicButton.new.string_("Confirm").action_({ 
				savefun.(textfield.value);
				me.closeWindow;
			}),
			BasicButton.new.string_("Cancel").action_({
				cancelfun.(textfield.value);
				me.closeWindow;
			}),
		)
	)
});
);

(
WindowDef(\ListSaveDialog, { arg def, list, okHook, defaultValue, label;
	var lay;
	var savefun;
	var listview = ListView.new;
	var textfield = TextField.new;
	var selectAction = { arg view, x, y, modifiers, buttonNumber, clickCount;
		[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

		//[view.value, view.items].debug("double click select: ");
		//if(clickCount == 1 and: {view.items.size > 0}) {
			textfield.value = listitems[view.value]
		//}
	};
	var listitems = list.collect { arg item, idx;
		if(item.isKindOf(Association)) {
			item.key;
		} {
			item;
		};
	};
	savefun = {
		var name = textfield.value.trim();
		if(name != "") {
			name = name.asSymbol;
			if(listitems.includes(name)) {
				WindowDef(\ConfirmDialog).front("This preset already exists, are you sure ?", {
					okHook.(name);
					def.closeWindow;
				})
			} {
				okHook.(name);
				def.closeWindow;
			}
		} {
			"invalid name".debug;
		};
	};
	listview.items = listitems;
	if(defaultValue.notNil) {
		var defidx = listitems.detectIndex { arg x; x == defaultValue };
		if(defidx.notNil) {
			listview.value = defidx;
			selectAction.(listview);
		} {
			textfield.value = defaultValue;
		};
	};
	textfield.keyDownAction_({ arg me, key, modifiers, unicode, keycode;
		[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
		if(key == $\r) {
			savefun.();
		};
	});
	lay = VLayout (
		textfield,
		listview.selectionAction_(selectAction),
		HLayout (
			BasicButton.new.string_("Save").action_({
				savefun.();
			}),
			BasicButton.new.string_("Cancel").action_({
				def.closeWindow;
			}),
		)
	);
	textfield.focus(true);
	lay;
});

WindowDef(\ListSelectDialog, { arg def, list, okHook, defaultValue, label;
	var listview = ListView.new;
	var textfield;
	var selectedView = StaticText.new;
	var selectAction = { arg view, x, y, modifiers, buttonNumber, clickCount;
		[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

		//[view.value, view.items].debug("double click select: ");
		if(view.items.size > 0 and: {view.value.notNil}) {
			selectedView.string = filteredList[view.value].key.asString;
		}
	};
	var listitems;
	var filteredList;
	def.windowName = label ?? {"ListSelectDialog"};
	listitems = list.collect { arg item, idx;
		if(item.isKindOf(Association)) {
			item;
		} {
			item -> item;
		};
	};
	filteredList = listitems;
	listview.items = listitems.collect(_.key);
	if(defaultValue.notNil) {
		listview.value = listitems.detectIndex { arg x; x == defaultValue }
	};
	selectAction.(listview);
	VLayout (
		HLayout (
			StaticText.new.string_("Selected: "),
			selectedView,
		),
		HLayout (
			StaticText.new.string_("Search: "),
			textfield = TextField.new.keyUpAction_({ arg me, key, modifiers, unicode, keycode;
				[me, key.asCompileString, modifiers, unicode, keycode].debug("keyUpAction");
				if(me.string.trim != "") {
					filteredList = listitems.select({ arg item, idx;
						item.key.asString.toLower.contains(me.string.toLower)
					});
					listview.items = filteredList.collect(_.key);
				} {
					listview.items = listitems.collect(_.key);
				};
				selectAction.(listview);
			}).focus(true),
		),
		listview.selectionAction_(selectAction),
		HLayout (
			BasicButton.new.string_("Ok").action_({
				var name = selectedView.string;
				if(name != "") {
					name = name.asSymbol;
					okHook.(name, filteredList[listview.value]);
					def.closeWindow;
				} {
					"invalid name".debug;
				};
			}),
			BasicButton.new.string_("Cancel").action_({
				def.closeWindow;
			}),
		)
	)
});
	  
);

// END

WindowDef(\ConfirmDialog).front("this will destroy everything, are you shure????")
WindowDef(\RenameDialog).front("what name will it be ?")
WindowDef(\filedialog_sample).front
WindowDef(\ListSelectDialog).front([\bla, \rah], { arg ...args; args.debug("res") }, \rah -> \rah);
WindowDef(\ListSaveDialog).front([\bla, \rah -> \hjj], { arg x; x.postln; },\rah)
