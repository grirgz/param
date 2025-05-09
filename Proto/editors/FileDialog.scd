(
	8.do { arg numChannels;
		numChannels = numChannels+1;
		SynthDef("Param_buffer_player_%".format(numChannels).asSymbol, { arg out=0, amp=0.1, gate=1, pan=0, freq=200, bufnum, speed=1, pos=0, doneAction=2, loop=0, trigger=1, velamp=1;
			// pos in seconds
			var sig;
			var sig1;
			sig = PlayBuf.ar(numChannels, bufnum, BufRateScale.kr(bufnum) * speed, trigger, startPos: (pos*BufSampleRate.kr(bufnum)), doneAction:doneAction, loop: loop);
			sig = sig * EnvGen.ar(\adsr.kr(Env.adsr(0.001,0,1,0.01)),gate,doneAction:doneAction);
			//sig = Pan2.ar(sig, pan, 1).sum;
			sig = Splay.ar(sig, \spread.kr(1), amp, pan);
			Out.ar(out, sig * \gain.kr(1) * velamp);
		}, metadata:(specs:(
			bufnum: ParamAudioBufferSpec()
			.startParamName_(\pos)
			.numChannels_(numChannels)
			.startType_(\seconds)
			.sustainType_(\beats)
			.sustainParamName_(\sustain)
			.speedParamName_(\speed),
			pos: ControlSpec(0,20,\lin),
			\velamp: \unipolar.asSpec,
		))).add;
	};
);

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
		if(name == "recent://") {
			ProtoTemplateDef(\filemanager_recent_files).new(name);
		} {
			if(name.isKindOf(String)) {
				name = PathName(name);
			};
			self.pathName = name;
			self;
		};
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
	icon_:{ arg self, val;
		self[\icon] = val;
		self;
	},
	icon: { arg self;
		if(self.isFolder) {
			self.iconPath +/+ "places/32/folder.png"
		} {
			self.iconPath +/+ "mimes/32/gtk-file.png"
		};
	},
	iconPath: { arg self;
		// FIXME: hardcoded
		"/usr/share/icons/elementary-xfce/";
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

	player: { arg self;
		self.player = Pdef(\FileDialogPreview).quant_(0);
		self.player;
	},

	freeOldBuffers: { arg self;
		ProtoTemplateDef(\filemanager_node).bufferToFree.do { arg item, idx;
			item.free;
		};
		ProtoTemplateDef(\filemanager_node).bufferToFree = [];
	},

	play: { arg self;
		if(self.isFile and: { self.playable_extensions.includesEqual(self.pathName.extension) }) {
			// FIXME: need to write custom code to load file into buffer and play it on a proxy
			// then free buffer
			//SoundFile(self.fullPath).play;

			self.freeOldBuffers;
			Buffer.read(Server.default, self.fullPath, action: { arg buf;
				ProtoTemplateDef(\filemanager_node).bufferToFree = ProtoTemplateDef(\filemanager_node).bufferToFree.add(buf);
				self.player.stop;
				self.player.source = Pfindur(buf.duration * thisThread.clock.tempo, Pmono(
					"Param_buffer_player_%".format(buf.numChannels).asSymbol,
					\bufnum, buf,
					\dur, 1/8,
				));
				self.player.play;
			})
		}

	},


	numChannels: { arg self;
		if(self.isFile and: { self.playable_extensions.includesEqual(self.pathName.extension) }) {
			SoundFile(self.fullPath).info.numChannels;
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
ProtoTemplateDef(\filemanager_special_node, (
	parent: ProtoTemplateDef(\filemanager_node),

	play: { arg self; },

	typeLabel: { arg self;
		\special
	},

	isFile: { arg self;
		false
	},

	isFolder: { arg self;
		true
	},

	isChoosable: { arg self;
		false
	},

	splitPath: { arg self;
		[ self  ]
	},

	asString: { arg self;
		self.fullPath;
	},
));
ProtoTemplateDef(\filemanager_quicklist, (
	parent: ProtoTemplateDef(\filemanager_special_node),

	config_file: Platform.userConfigDir +/+ "filemanager_quicklist.sc",

	className: { arg self;
		ProtoTemplateDef(\filemanager_quicklist);
	},

	new: { arg self, name;
		self = self.copy;
		//[name, name.class].debug("new name");
		self;
	},

	currentWorkingDir: { arg self;
		try { PathName(Document.current.path).pathOnly } ? "~"
	},

	children: { arg self;
		var iconpath = "/usr/share/icons/elementary-xfce/";
		var iconsize = "64";
		[
			ProtoTemplateDef(\filemanager_node).new("/").name_("Root /").icon_(iconpath+/+"devices/48/drive-harddisk-root.png"),
			ProtoTemplateDef(\filemanager_node).new("~").name_("Home ~").icon_(iconpath+/+"/places/24/folder-home.png"),
			ProtoTemplateDef(\filemanager_recent_files).new.icon_(iconpath+/+"places/32/folder-recent.png"),
			\separator,
			ProtoTemplateDef(\filemanager_node).new(self.currentWorkingDir).name_("Cwd %".format(self.currentWorkingDir)),
			\separator,

		] ++ self.quicklist.collect { arg path, idx;
			ProtoTemplateDef(\filemanager_node).new(path)
		};
	},

	quicklist: { arg self;
		self[\quicklist] = List.new;
		self[\quicklist];
	},

	addRecentFile: { arg self, path;
		if(self.recent_files_list.includesEqual(path).not) {
			self.recent_files_list = self.recent_files_list.add(path);
			self.save_quicklist;
		}
	},

	addQuickPath: { arg self, path;
		if(self.quicklist.includesEqual(path).not) {
			self.quicklist = self.quicklist.add(path);
			self.save_quicklist;
		}
	},

	removeQuickPath: { arg self, path;
		var idx = self.quicklist.detectIndex{ arg x; x == path };
		if(idx.notNil) {
			self.quicklist.removeAt(idx);
			self.save_quicklist;
		};
	},

	save_quicklist: { arg self;
		File.use(self.config_file, "w", { arg file;
			file.write("// FileDialog configuration from Param quark\n");
			file.write(
				"ProtoTemplateDef(%).quicklist = List[\n%];\n".format(
					self.key.asCompileString,
					self.quicklist.collect({ arg x; "\t%,\n".format(x.asCompileString)}).join
				)
			);
			file.write(
				"ProtoTemplateDef(%).recent_files_list = List[\n%];\n".format(
					self.key.asCompileString,
					self.recent_files_list.collect({ arg x; "\t%,\n".format(x.asCompileString)}).join
				)
			)
		});
	},

	load_quicklist: { arg self;
		if(self.config_file.notNil) {
			self.config_file.load;
		};
	},

	name: { arg self;
		"Quicklist";
	},

	fullPath: { arg self;
		"quicklist://"
	},

	parentNode: { arg self;
		self
	},

));
ProtoTemplateDef(\filemanager_recent_files, (
	parent: ProtoTemplateDef(\filemanager_special_node),

	new: { arg self, name;
		self = self.copy;
		//[name, name.class].debug("new name");
		self;
	},

	children: { arg self;
		ProtoTemplateDef(\filemanager_quicklist).recent_files_list.collect { arg path, idx;
			ProtoTemplateDef(\filemanager_node).new(path);
		};
	},

	className: { arg self;
		ProtoTemplateDef(\filemanager_recent_files);
	},

	name: { arg self;
		"Recent files"
	},

	fullPath: { arg self;
		"recent://"
	},

	parentNode: { arg self;
		ProtoTemplateDef(\filemanager_quicklist).new;
	},
));
		ProtoTemplateDef(\filemanager_quicklist)[\parent][\parent];

WindowDef(\filedialog_generic, { arg def, path, callback, nodeClass, isSaving=false;
	// callback(file_full_path_to_open)

	var fileview, changepath, select_file;
	var pathview = View.new;
	//var quicklist = ListView.new;
	var quicklist = Menu();
	var node;
	var update_pathview;
	var preview = View.new;
	var proto = ProtoDef(\filemanager_player);
	var name_field;
	var nodelist;
	var quicknodelist;
	var defaultFilename;
	var okLabel;
	var baseProto = ProtoTemplateDef(\filemanager_node);
	var quicklistProto = ProtoTemplateDef(\filemanager_quicklist);
	nodeClass = nodeClass ?? { ProtoTemplateDef(\filemanager_node) };

	name_field = TextField.new;

	preview.onClose_({ 
		var p = baseProto.player;
		if(p.notNil) { p.stop };
	});

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
			//Log(\Param).debug("end disable play file");
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

	//quicklist.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
		//var item;
		////[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
		
		////[view.value, view.items].debug("double click select: ");
		//if(clickCount == 2 and: {view.items.size > 0}) {
			//item = quicknodelist[view.value];
			//changepath.(fileview, item.fullPath);
			////if(item == "Recent files") {
				////changepath.(fileview, "recents://")
			////} {
				////changepath.(fileview, item)
			////};
		//}
	//});

	proto.update_quicklistview = { arg self;
		quicknodelist = quicklistProto.new.children;
		//quicklist.items = quicknodelist.collect(_.label).asArray;
		quicklist.clear;
		quicknodelist.asArray.do { arg item, idx;
			var action;
			if(item == \separator) {
				action = MenuAction.separator;
			} {
				action = MenuAction(item.label, {
					changepath.(fileview, item.fullPath);
				});
				if(item.icon.notNil) {
					if(File.exists(item.icon)) {
						action.icon = Image(item.icon);
					} {

					}
				};
			};
			quicklist.addAction(action)
		};
		//[ quicknodelist, quicklist.items ].debug("gni ?");
	};
	proto.save_quicklist = { arg self;
		quicklistProto.save_quicklist;
		//File.use(self.config_file, "w", { arg file;
			//file.write(
				//"ProtoDef(%).quicklist = List[\n%];\n".format(
					//self.key.asCompileString,
					//self.quicklist.collect(_.asCompileString).join(",\n")
				//)
			//);
			//file.write(
				//"ProtoDef(%).recent_files_list = List[\n%];\n".format(
					//self.key.asCompileString,
					//self.recent_files_list.collect(_.asCompileString).join(",\n")
				//)
			//)
		//});
	};
	proto.load_quicklist = { arg self;
		quicklistProto.load_quicklist;
		//if(self.config_file.notNil) {
			//self.config_file.load;
		//};
	};

	proto.check_name_field = { arg self, callback;
		var node;
		if(self.choosedNode.notNil) {
			node = nodeClass.new(self.choosedNode.parentNode.fullPath +/+ name_field.string);
			if(node.isChoosable) {
				self.chooseNode(node);
				callback.();
			} {
				"Can't open: file doesn't exist: %".format(node.fullPath).error;
			}
		} {
			"Can't open: no chosen file: %".format(self.choosedNode).error;
		};
	};

	proto.chooseNode = { arg self, node, alsoPlay=false;
		var numChannels;
		var typeinfo;
		self.current_file = node.fullPath;
		self.choosedNode = node;
		name_field.string = node.name;
		numChannels = node.numChannels;
		if(numChannels.notNil) {
			typeinfo = "numChan: %".format(numChannels)
		} {
			typeinfo = node.typeLabel
		};
		self.filenameview.string = "[%] %".format(typeinfo, node.fullPath.asString);
		if(alsoPlay == true) {
			if(self.disable_play != true) {
				proto.playNode(node);
			};
		}
	};

	proto.playNode = { arg self, node;
		node.play;
		//Buffer.read(Server.default, node.fullPath, action: { arg buf;
			//self.player.stop;
			//self.player.source = Pmono( \playersec,
				//\bufnum, buf,
				//\dur, 1/8,
			//);
			//self.player.play;
		//})
		
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
		quicklistProto.addRecentFile(filename);
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
		//proto.quicklist = proto.quicklist ? [
			//"Recent files",
			//path, 
			//"/", 
			//"~", 
			//"~/Musique/",
			//"~/Musique/sc/samplekit/",
			//"~/Musique/sc/reckit/",
			//"~/Musique/sc/samplekit/com/",
			//"~/code/sc/seco/vlive/",
			//"~/code/sc/seco/vlive/repeat/",
			//"~/code/sc/seco/vlive/repeat/capsule/",
		//].select(_.notNil);
		proto.update_quicklistview;
		//self.player = self.player ?? { Pdef(\FileDialogPreview) };
		//self.player.quant = 0;
		//proto.playerwrapper = PlayerWrapper(proto.player);
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

	okLabel = if(isSaving) { "Save" } { "Open" };

	preview.layout = HLayout (
		Param(baseProto.player, \gain).asKnob.fixedHeight_(25),
		PlayerWrapper(ProtoTemplateDef(\filemanager_node).player).asView.label_("Preview"),
		Param(Message(proto), \auto_mode, ParamBoolSpec()).asCheckBox("Auto play"),
		[proto.filenameview.fixedHeight_(25), stretch:4],
			BasicButton.new.string_(okLabel).action_({
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
						// specialFileName is used to load specific file in a project dir
						proto.add_to_recent_files(proto.current_file);
						callback.(proto.current_file, proto.choosedNode.specialFileName);
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
			HLayout (
				BasicButton.new.string_("+").action_({
					quicklistProto.addQuickPath(node.fullPath);
					proto.update_quicklistview;
					//proto.save_quicklist;
					//PathName("//bin").fullPath
				}).maxWidth_(30),
				BasicButton.new.string_("-").action_({
					quicklistProto.removeQuickPath(node.fullPath);
					proto.update_quicklistview;
				}).maxWidth_(30),
			).spacing_(0),
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


// END

WindowDef(\ConfirmDialog).front("this will destroy everything, are you shure????")
WindowDef(\RenameDialog).front("what name will it be ?")
WindowDef(\filedialog_sample).front
WindowDef(\ListSelectDialog).front([\bla, \rah], { arg ...args; args.debug("res") }, \rah -> \rah);
WindowDef(\ListSaveDialog).front([\bla, \rah -> \hjj], { arg x; x.postln; },\rah)
