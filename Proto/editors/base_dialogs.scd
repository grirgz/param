
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
	// okHook args: name string
	var lay;
	var savefun;
	var listview = ListView.new;
	var textfield = TextField.new;
	var selectAction = { arg view, x, y, modifiers, buttonNumber, clickCount;
		//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

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
		//[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
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
	// okHook args: name string, list association, index
	var listview = ListView.new;
	var textfield;
	var selectedView = StaticText.new;
	var selectAction = { arg view, x, y, modifiers, buttonNumber, clickCount;
		//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

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
				//[me, key.asCompileString, modifiers, unicode, keycode].debug("keyUpAction");
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
					okHook.(name, filteredList[listview.value], listview.value);
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
