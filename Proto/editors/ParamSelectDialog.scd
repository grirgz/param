
(

TagSpecDef(\GlobalLibraryCategory).addUnique(\ParamGroup);


TagSpecDef('GlobalLibrary.type.ParamGroup', [
	\Ndef,
	\Pdef,
	\ParamGroupDef,
]);

WindowDef(\ParamSelectDialog,{ arg def, selectedHook, okHook, defaultValue;
	var namefield = TextField.new;
	var trackInstrumentLabel = StaticText.new;
	var lib = ProtoDef(\GlobalLibrary);
	var selectedLibId;
	var paramTargetField, paramPropertyField, paramSpecText, paramCompileStringField;
	var paramEditView;
	var selectedParam;
	var selectedParamGroup;
	var paramList;
	var defaultValueLibId;
	var localOkHook;
	var lay;
	var selectedCallback = { arg libid, strings;
		//libid.asCompileString.debug("selectedCallback");
		paramTargetField.string = libid[1].value.asCompileString;
		selectedParamGroup = libid[1].value.asParamGroup;
		paramList.items = selectedParamGroup
			.collect { arg item, idx;
				//item.debug("item");
				item.property;
			}
		;
	};

	localOkHook = {
		var label;
		//"okHook".debug;
		def.closeWindow;
		if(namefield.string.trim != "") {
			label = namefield.name;
		};
		okHook.(paramCompileStringField.string.interpret, selectedParam);
	};

	paramTargetField = TextField.new;
	paramPropertyField = TextField.new;
	paramSpecText = StaticText.new.fixedSize_(300@20);
	paramCompileStringField = TextField.new;
	paramEditView = View.new.layout_(HLayout(StaticText.new));

	paramList = ListView.new;
	paramList.selectionAction = { arg view;
		selectedParam = selectedParamGroup[view.selection.first];
		paramPropertyField.string = selectedParam.property.asCompileString;
		paramSpecText.string = selectedParam.spec.asCompileString;
		paramCompileStringField.string = selectedParam.asCompileString;
		paramEditView.removeAll;
		paramEditView.layout = selectedParam.asView;
	};

	paramCompileStringField.keyUpAction_({ arg me, key, modifiers, unicode, keycode;
		//[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
		if(key == $\r) {
			localOkHook.()
		};
	});

	if(defaultValue.notNil) {
		ProtoDef(\GlobalLibrary).targetToLibId(defaultValue.target).debug("targetToLibId");
		defaultValueLibId = ProtoDef(\GlobalLibrary).targetToLibId(defaultValue.target);
	};


	lay = VLayout(
		View.new.layout_(
			VLayout (
				HLayout(
					StaticText.new.string_("Selected param:"),
					//paramTargetField, paramPropertyField, 
				),
				HLayout (
					paramCompileStringField, paramSpecText,
				),
				paramEditView,
				nil
			)
		).background_(Color.grey).minHeight_(100).maxHeight_(300),
		VLayout (
			HLayout (
				StaticText.new.string_("Search by name:"),
				trackInstrumentLabel,
			),
			HLayout (
				WindowDef(\GlobalLibrary).asView(lib, selectedCallback, [\ParamGroup], defaultValueLibId),
				VLayout (
					BasicButton.new.string_("Select last tweaked param").action_({
						var libId;
						var param = Param.lastTweaked;
						if(param.notNil) {
							Log(\Param).debug("param %" , param);
							libId = ProtoDef(\GlobalLibrary).targetToLibId(param.target);
							if(libId.notNil) {
								selectedCallback.(libId);
								paramList.value = paramList.items.indexOf(param.property);
							} {
								Log(\Param).info("Param not found in lib: %", param.asCompileString);
							}
						} {
							Log(\Param).info("No last tweaked param.");
						}
					}),
					paramList,
				)
			)
		),
		HLayout(
			BasicButton.new.string_("OK").action_({
				localOkHook.()
			}),
			BasicButton.new.string_("Cancel").action_({
				def.closeWindow;
			}),
		)
	);

	if(defaultValue.notNil and: {defaultValueLibId.notNil}) {
		selectedCallback.(defaultValueLibId);
		paramList.value = paramList.items.indexOf(defaultValue.property)
	};

	lay;
});
)
