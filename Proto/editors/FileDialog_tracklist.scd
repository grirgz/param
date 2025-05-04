
(
ProtoTemplateDef(\filemanager_node_tracklist,(
	parent: ProtoTemplateDef(\filemanager_node),
	className: { arg self;
		ProtoTemplateDef(\filemanager_node_tracklist);
	},

	label: { arg self;
		//"16".debug;
		if(self.isTrackListDirectory) {
			var name = self.name;
			"[ % ]".format(name.keep(name.size-1));
		} {
			self.name;
		}
	},

	isTrackListDirectory: { arg self;
		// NOTE: if there is several TrackDef in the directory, no way to choose which one we want
		self.specialFileName.notNil
	},

	specialFileName: { arg self;
		var isp = try {
			var typ;
			var fp = self.pathName.fullPath;
			var ret = nil;
			//fp.debug("fp");
			if(self.pathName.isFolder) {
				var files = self.pathName.files;
				var fidx;
				//"isfolder true".debug;
				fidx = files.detectIndex { arg f; 
					//f.fileName.debug("test file");
					f.fileName.beginsWith("TrackDef_") and: {
						f.fileName.endsWith(".scd")
					}
				};
				//fidx.debug("fidx");
				if(fidx.notNil) {
					var didx;
					var dirs = self.pathName.folders;
					var dirname = files[fidx].fileNameWithoutExtension;
					//dirname.debug("dirname");
					if(PathName(self.pathName.fullPath +/+ dirname).isFolder) {
						ret = files[fidx].fileName;
					};
				};
			};
			ret;
		} { arg err;
			err.debug("ERR");
			nil
		};
		//isp.debug("specialFileName");
		isp;
	},

	isChoosable: { arg self;
		self.isTrackListDirectory
	},

	typeLabel: { arg self;
		if(self.isTrackListDirectory) {
			"TrackList"
		} {
			File.type(self.pathName.fullPath).asString;
		}
	},

	children: { arg self;
		//"12".debug;
		self.pathName.entries.collect({ arg p; 
			var entry = self.new(p);
			if(entry.isFile) {
				nil;
			} {
				entry;
			}
		}).select(_.notNil);
	},

));

WindowDef(\filedialog_tracklist, { arg def, path, callback;
	WindowDef(\filedialog_generic).embedView(def, path, callback, ProtoTemplateDef(\filemanager_node_tracklist))
});
WindowDef(\filedialog_tracklist_save, { arg def, path, callback;
	WindowDef(\filedialog_generic).embedView(def, path, callback, ProtoTemplateDef(\filemanager_node_tracklist), true)
});
//WindowDef(\filedialog_project).front("~/", { arg ...args; args.debug("args") });
//WindowDef(\filedialog_project_save).front("~/", { arg ...args; args.debug("args") });
)
