
(
ProtoTemplateDef(\filemanager_node_project,(
	parent: ProtoTemplateDef(\filemanager_node),
	className: { arg self;
		ProtoTemplateDef(\filemanager_node_project);
	},

	label: { arg self;
		//"16".debug;
		if(self.isProject) {
			var name = self.name;
			"[ % ]".format(name.keep(name.size-1));
		} {
			self.name;
		}
	},

	isProject: { arg self;
		var isp = try {
			var typ;
			//self.pathName.fullPath.debug("type");
			typ = File.type(self.pathName.fullPath +/+ "data/project.scd");
			//typ = \regular;
			typ == \regular
		} { arg err;
			err.debug("ERR");
			false
		};
		//isp.debug("isp");
		isp;
	},
	isChoosable: { arg self;
		self.isProject
	},

	typeLabel: { arg self;
		if(self.isProject) {
			"project"
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

WindowDef(\filedialog_project, { arg def, path, callback;
	WindowDef(\filedialog_generic).embedView(def, path, callback, ProtoTemplateDef(\filemanager_node_project))
});
WindowDef(\filedialog_project_save, { arg def, path, callback;
	WindowDef(\filedialog_generic).embedView(def, path, callback, ProtoTemplateDef(\filemanager_node_project), true)
});
//WindowDef(\filedialog_project).front("~/", { arg ...args; args.debug("args") });
//WindowDef(\filedialog_project_save).front("~/", { arg ...args; args.debug("args") });
)
