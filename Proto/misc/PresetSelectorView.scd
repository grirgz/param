
WindowDef(\PresetSelectorView, { arg def, pg;
		var layout;
		var left_button, popup_menu, right_button;
		var preset_list;
		var refresh_menu;
		var menu_current_index, menu_old_index;
		var menu_previous_index;
		var menu_walk;
		var arrow_width=25;

		var make_save_preset_layout = { arg pg, save_callback;
			var layout;
			var preset_name_field, preset_list_view;
			var save_button;
			preset_name_field = TextField.new;
			save_button = BasicButton.new
				.label_("Save preset")
				.action_({
					if(preset_name_field.string != "") {
						pg.savePreset(preset_name_field.string.asSymbol);
						save_callback.(preset_name_field.string.asSymbol);
					};
				});
			preset_list_view = ListView.new;
			preset_list_view.items = pg.presetDict.keys.asArray.sort;
			preset_list_view.action = { arg view;
				preset_name_field.string_(view.items[view.value])
			};
			preset_list_view.value = nil;
			layout = VLayout.new(
				preset_name_field,
				save_button,
				preset_list_view,
			);
			layout;
		};

		var make_save_preset_window = { arg pg, ok_callback, cancel_callback;
			var win = Window.new;
			var pglayout;
			var layout = make_save_preset_layout.(pg, { arg name; 
				win.onClose = {ok_callback.(name)}; 
				win.close;
			});
			win.onClose = cancel_callback;
			win.layout = layout;
			win.front;
		};

		refresh_menu = {
			var index = popup_menu.value ? 1;
			preset_list =  pg.presetDict.keys.asArray.sort;
			popup_menu.items = [
				"Save preset",
				"-----------",
			] ++ preset_list;
			popup_menu.value = index;
		};

		left_button = BasicButton.new
			.action_({
				if( menu_previous_index <= 2) {
					popup_menu.value = popup_menu.items.size - 1;
				} {
					popup_menu.value = popup_menu.value - 1;
				};
				menu_previous_index = popup_menu.value;
				pg.loadPreset(preset_list[popup_menu.value-2]);
			})
			.label_("<");

		popup_menu = PopUpMenu.new;

		popup_menu.onChange(pg, \presets, {
			refresh_menu.();
		});

		right_button = BasicButton.new
			.action_({
				if( menu_previous_index == ( popup_menu.items.size - 1 )) {
					popup_menu.value = 2;
				} {
					popup_menu.value = popup_menu.value + 1;
				};
				menu_previous_index = popup_menu.value;
				pg.loadPreset(preset_list[popup_menu.value-2]);
			})
			.label_(">");

		refresh_menu.();

		//menu_current_index = popup_menu.value.debug("cur first");
		menu_previous_index = popup_menu.value;
		popup_menu.action = { arg menu;
			if(menu.value < 2) {
				if(menu.value == 0) {
					make_save_preset_window.(pg, { arg name;
						refresh_menu.();
						popup_menu.value = popup_menu.items.detectIndex({ arg x; x == name});
						menu_previous_index = menu.value;
					}, {

					});
					popup_menu.value = menu_previous_index;
				};
			} {
				pg.loadPreset(preset_list[menu.value-2]);
			};
			menu_previous_index = menu.value;
		};
		
		layout = HLayout(
			left_button.maxWidth_(arrow_width),
			[popup_menu, stretch:1],
			right_button.maxWidth_(arrow_width),
		).spacing_(1);
		layout;

});

WindowDef(\PresetListMorpherView, { arg def, pg, size=8, features;
		var morpher = PresetListMorpher(pg, size);
		var save_buttons, load_buttons, disable_buttons, delete_buttons;
		var features_gui;

		// FIXME: use \presets signal of pg to update gui
		var refresh_button = { arg but, i;
			if(morpher.getPreset(i).notNil) {
				but.background = ParamViewToolBox.color_dark;
			} {
				but.background = Color.white;
			}
		};

		var refresh_disable_button = { arg but, i;
			if(morpher.isEnabled(i)) {
				but.background = Color.white;
			} {
				but.background = ParamViewToolBox.color_ligth;
			}
		};

		features = features ? [\load, \disable, \save, \erase, \morph];

		save_buttons = morpher.size.collect { arg i;
			var but = BasicButton.new.label_(i).minWidth_(15).action_({ arg but;
				morpher.save(i);
				refresh_button.(load_buttons[i], i);
			});
			but;
		};
		load_buttons = morpher.size.collect { arg i;
			var but = BasicButton.new.label_(i).minWidth_(15).action_({ arg but;
				morpher.load(i);
				refresh_button.(load_buttons[i], i);
			});
			refresh_button.(but, i);
			but;
		};
		disable_buttons = morpher.size.collect { arg i;
			var but = BasicButton.new.label_(i).minWidth_(15).action_({ arg but;
				morpher.toggleEnablePreset(i);
				refresh_disable_button.(disable_buttons[i], i);
			});
			refresh_disable_button.(but, i);
			but;
		};
		delete_buttons = morpher.size.collect { arg i;
			var but = BasicButton.new.label_(i).minWidth_(15).action_({ arg but;
				morpher.erase(i);
				refresh_button.(load_buttons[i], i);
			});
			but;
		};

		features_gui = features.collect({ arg feat;
			switch(feat, 
				\save, {
					[ StaticText.new.string_("Save") ] ++ save_buttons;
				},
				\load, {
					[ StaticText.new.string_("Load") ] ++ load_buttons;
				},
				\disable, {
					[ StaticText.new.string_("Disable") ] ++ disable_buttons;
				},
				\erase, {
					[ StaticText.new.string_("Erase") ] ++ delete_buttons;
				},
				\morph, {
					[
						StaticText.new.string_("Morph"),
						[morpher.asSlider.orientation_(\horizontal), columns:8],
					]
				}
			)
		});

		GridLayout.rows (
			* features_gui
		)

});
