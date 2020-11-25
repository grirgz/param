
////////////////////// Basic building blocks

// misc stuff for param GUI
// define the default palette
ParamViewToolBox {

	classvar <>color_dark;
	classvar <>color_ligth;

	*initClass {
		//color_ligth = Color.newHex("63AFD0");
		//color_dark = Color.newHex("0772A1");
		color_ligth = Color.new255(101, 166, 62);
		color_dark = Color.new255(130, 173, 105);

		//color_ligth = Color.newHex("5CCCCC");
		//color_dark = Color.newHex("009999");
	}

	*horizontal_slider { arg param, label_mode;
		var view = View.new;
		var slider, val, label;
		view.layout_(
			HLayout (
				label = StaticText.new,
				slider = param.asSlider.orientation_(\horizontal),
				val = param.asTextField,
			)
		);
		view.addUniqueMethod(\label, { label });
		view.addUniqueMethod(\slider, { slider });
		view.addUniqueMethod(\textfield, { val });
		view.addUniqueMethod(\mapParam, { arg view, param;
			if(label_mode == \full) {
				label.string  = param.asLabel;
			} {
				label.string = param.property
			};
			val.mapParam(param);
			slider.mapParam(param);
		});
		view.addUniqueMethod(\unmapParam, { arg view, param;
			label.string = "";
			val.unmapParam;
			slider.unmapParam;
		});
		if(param.notNil) { view.mapParam(param) };
		^view
	}

	*label_knob { arg name;
		var view;
		var val, label, control;
		label = StaticText.new.string_(name).align_(\center);
		control = Knob.new;
		val = TextField.new;
		view = View.new;
		view.layout = VLayout(
			label,
			control,
			val,
		);
		view.layout.margins_(0);
		view.addUniqueMethod(\mapParam, { arg view, param;
			param.debug("ParamViewToolBox.knob.mapParam");
			control.mapParam(param);
			val.mapParam(param);
			view;
		});
		view.addUniqueMethod(\unmapParam, { arg view, param;
			control.unmapParam;
			val.unmapParam;
			view;
		});
		^view;
	}

	*knob { arg param;
		var view;
		var val, label, control;
		label = StaticText.new;
		control = Knob.new;
		val = TextField.new;
		view = View.new;
		view.layout = VLayout(
			label,
			control,
			val
		);
		view.addUniqueMethod(\mapParam, { arg view, param;
			param.debug("ParamViewToolBox.knob.mapParam");
			label.mapParamLabel(param).align_(\center);
			control.mapParam(param);
			val.mapParam(param).align_(\right);
			view;
		});
		view.addUniqueMethod(\unmapParam, { arg view, param;
			label.unmapParam;
			control.unmapParam;
			val.unmapParam;
			view;
		});
		if(param.notNil) { view.mapParam(param) };
		^view;
	}

	*attachContextMenu { arg param, view;
		view.mouseDownAction = {  arg vie, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseUpAction");
			if(buttonNumber == 1) {
				if(WindowDef(\ParamGenericOverlayMenu).notNil) {
					WindowDef(\ParamGenericOverlayMenu).front(vie, x, y, param)
				}
			}
		};
	}
}


// Build GUI from a param pointing to a list (sequencer style)
ListParamLayout {

	*new { arg param, makecell;
		^super.new.init(param, makecell)
	}

	*button { arg param, width=40;
		^super.new.init(param, { arg param;
			param.asButton.fixedWidth_(width);
		});
	}

	*gridButton { arg xparam, width=40;
		// bug in button size, gridButton is a workaround,
		// the drawback is you need to access to the button inside the view:
		^super.new.init(xparam, { arg param;
			var button = param.asButton;
			View.new.layout_(HLayout(button).margins_(0))
				.addHalo(\isGridButton, true)
				.addUniqueMethod(\button, { button } )
			;
		});
	}

	*slider { arg param, width=40;
		^super.new.init(param, { arg param;
			param.asSlider.fixedWidth_(width);
		});
	}

	*knob { arg param, width=40;
		^super.new.init(param, { arg subparam;
			subparam.asKnob.fixedWidth_(width);
		});
	}

	*valuePopup { arg param, keys, width=40;
		keys = keys ? []; // TODO: use spec and Param.asPopupView
		^super.new.init(param, { arg subparam;
			var pm = PopUpMenu.new;
			pm.fixedWidth_(width);
			pm.items = keys;
			pm.action = {
				subparam.set(pm.value)
			};
			pm.onChange(subparam.target, \set, {
				// TODO: do not change the whole row when just one value is updated!
				var val;
				//"there is change! my lord!".debug;
				pm.value = pm.items.detectIndex({ arg x; x == subparam.get })
			});
			pm.value = pm.items.detectIndex({ arg x; x == subparam.get });
			pm;
		});
	}

	*indexPopup { arg param, keys, width=40;
		keys = keys ? []; // TODO: use spec and Param.asPopupView
		^super.new.init(param, { arg subparam;
			var pm = PopUpMenu.new;
			pm.fixedWidth_(width);
			pm.items = keys;
			pm.action = {
				subparam.set(pm.value)
			};
			pm.onChange(subparam.target, \set, {
				pm.value = subparam.get;
			});
			pm.value = subparam.get;
			pm;
		});
	}

	*addCursor { arg x, view, param, on, off;
		^view.onChange(param.target, \cursor, { arg view ...args;
			//[args[2], x].debug("bbb");
			if(args[2] == x or: { args[2].isNil }) {
				// AppClock doesnt have the same tempo of the pattern
				//		but s.latency is in seconds and appclock has tempo 1, so this works!!!
				// FIXME: how to specify another server ?
				Task{
					Server.default.latency.wait;
					if(view.isClosed.not) {
						if(args[3] == 1) {
							on.value(view, x, param, args[4]);
						} {
							off.value(view, x, param, args[4]);
						};
					};
					nil
				}.play(AppClock);
			};
			//args.debug("cursor!!");
		})
	}

	*cursor { arg param, width=40;
		^super.new.init(param, { arg param, x;
			Button.new
			.enabled_(false)
			.fixedWidth_(width)
			.onChange(param.target, \cursor, { arg view ...args;
				[args[2], x].debug("bbb");
				if(args[2] == x or: { args[2].isNil }) {
					// FIXME: AppClock doesnt have the same tempo of the pattern :/
					// FIXME: how to specify another server ?
					Task{
						Server.default.latency.wait;
						if(args[3] == 1) {
							view.value = 1;
						} {
							view.value = 0;
						};
						nil
					}.play(AppClock);
				};
				args.debug("cursor!!");
			})
			.states_([
				["", Color.black, Color.white],
				["", Color.black, ParamViewToolBox.color_ligth],
			]);
		})
	}

	init { arg param, makecell;
		var viewlist;
		^HLayout(*
			viewlist = param.collect(makecell)
		)
		.add(nil)
		.addUniqueMethod(\mapParam, { arg param;
			viewlist.do { arg view, n; 
				view.mapParam(param.at(n));
			}
		})
		.addUniqueMethod(\viewlist, {
			viewlist
		})
	}
}


// Build GUI from a ParamGroup
ParamGroupLayout {
	*new { arg group;
		^this.windowize(this.two_panes(group));
	}

	*windowize { arg layout, alwaysOnTop=true;
		// replaced by WindowLayout and WindowDef
		var win = Window.new;
		win.layout = layout;
		win.alwaysOnTop = alwaysOnTop;
		win.front;
		^win;
	}

	*singleWindowize {
		// TODO: the window close before recreating if open
	}

	*block { arg label, pg;
		^VLayout(
			[StaticText.new.string_(label).background_(ParamViewToolBox.color_dark), stretch:0],
			[this.two_panes(pg, \property), stretch:1],
		)
	}

	*player_block { arg player, pg, views;
		^VLayout(
			[View.new.layout_(
				HLayout(
					PlayerWrapper(player).asView,
					views,
					nil
				).margins_(0)
			).background_(ParamViewToolBox.color_dark), stretch:0],
			[this.two_panes(pg, \property), stretch:1],
		);
	}

	*player_preset_block { arg player, pg, views;
		^VLayout (
			HLayout (
				PlayerWrapper(player).asView,
				views,
				EventClass.presetSelectorView.(pg),
			),
			ParamGroupLayout.two_panes(pg, \property),
		)

	}

	*formEntry { arg item, label_mode;
		// if player, show a button
		// if param scalar, show horizontally a label, horizontal slider and textField
		// if param env or array, show vertically a label, an env/array, a textField

		// do not call .asView here because it call recursively formEntry !
   
		var scalar_entry = { arg param;
			var lay;
			lay = if(param.isNil) {
				nil;
			} {
				HLayout(
					if(label_mode == \full) {
						var st = param.asStaticTextLabel(\full);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					} {
						var st = StaticText.new.string_(param.property);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					}.fixedWidth_(80),
					param.asSlider.orientation_(\horizontal).minWidth_(150),
					param.asTextField.fixedWidth_(80),
				)
			};
			lay;
		};
		var env_entry = { arg param;
			var lay;
			lay = if(param.isNil) {
				nil;
			} {
				VLayout(
					if(label_mode == \full) {
						param.asStaticTextLabel(\full);
					} {
						StaticText.new.string_(param.property)
					},
					param.asEnvelopeView.minHeight_(150),
					param.asTextField,
				)
			};
			lay;
		};
		var popup_entry = { arg param;
			var lay;
			lay = if(param.isNil) {
				nil;
			} {
				HLayout(
					if(label_mode == \full) {
						param.asStaticTextLabel(\full);
					} {
						StaticText.new.string_(param.property)
					}.fixedWidth_(80),
					param.asPopUpMenu.minWidth_(150),
					//param.asTextField.minWidth_(70),
				)
			};
			lay;
		};
		
		^case(
			{ item.isKindOf(Param) }, {
				if(item.type == \scalar) {
					scalar_entry.(item)
				} {
					if([\array, \env].includes(item.type)) {
						env_entry.(item)
					} {
						if(item.spec.isKindOf(TagSpec) or: { item.spec.isKindOf(ParamBusSpec) }) {
							popup_entry.(item)
						}
					}
				}
			},
			{ item.isKindOf(PlayerWrapper) }, {
				item.asView.enableRightClickEditor_(true)
			}, {
				PlayerWrapper(item).asView.enableRightClickEditor_(true)

			}
		)
	}

	*two_panes { arg pg, label_mode;

		var layout;
		var gridlayout;
		var biglayout;
		var scalarlist, biglist, busbuflist;
		var layout_type;

		label_mode = label_mode ? \full; // \full, \property

		scalarlist = pg.select({ arg param; 
			param.type == \scalar;
		});
		biglist = pg.select({ arg param;
			param.type != \scalar and: { 
				param.spec.isKindOf(ParamAudioSpec).not
				and: { 
					// FIXME: find a better way to handle this
					param.type != \other 
				}
			}
		});
		busbuflist = pg.select( { arg param;
			param.spec.isKindOf(ParamBusSpec) or: {
				param.spec.isKindOf(ParamBufferSpec) or: {
					param.spec.isKindOf(TagSpec)
				}
			}
		});

		gridlayout = GridLayout.rows(*
			busbuflist.collect({ arg param;

				var statictext = if(label_mode == \full) {
				param.asStaticTextLabel;
				} {
					StaticText.new.string_(param.property)
				};
				ParamViewToolBox.attachContextMenu(param, statictext);
				[
					statictext,
					param.asPopUpMenu,
					//param.asTextField,
				]
			}) ++
			scalarlist.collect({ arg param;

				var statictext = if(label_mode == \full) {
				param.asStaticTextLabel;
				} {
					StaticText.new.string_(param.property)
				};
				ParamViewToolBox.attachContextMenu(param, statictext);
				[
					statictext,
					param.asSlider.orientation_(\horizontal),
					param.asTextField,
				]
			}) 

		);
		gridlayout.setColumnStretch(0,2);
		gridlayout.setColumnStretch(1,6);
		gridlayout.setColumnStretch(2,2);
		gridlayout = VLayout(gridlayout, [nil, stretch:2]);

		// chipotage
		if(biglist.size < 5 and: { scalarlist.size < 6 } ) {
			layout_type = VLayout;
		} {
			layout_type = HLayout;
		};

		biglayout = VLayout(*
			biglist.collect({ arg param;
				var statictext = if(label_mode == \full) {
					param.asStaticTextLabel;
				} {
					StaticText.new.string_(param.property)
				};
				VLayout(
					statictext,
					param.asView,
					param.asTextField,
				)
			})
		);

		layout = layout_type.new(
			gridlayout,
			biglayout
		);
		^layout;
	}

	*inline_groups { arg pg, label_mode;

		var layout;
		var gridlayout;
		var biglayout;
		var scalarlist, biglist;
		var layout_type;
		var scrollview;

		var vertical_slider_group = { arg params, size=8;
			var lay;
			lay = GridLayout.rows(
				* size.collect({ arg idx;
					var param = params[idx];
					param.debug("what param");
					if(param.isNil) {
						nil ! 3;
						//{ Button.new } ! 3
					} {
						[
							if(label_mode == \full) {
								param.asStaticTextLabel;
							} {
								StaticText.new.string_(param.property)
							},
							param.asSlider.orientation_(\horizontal).minWidth_(150),
							param.asTextField.minWidth_(70),
						]
					}
				}) ++ [{ View.new }!3]
			).vSpacing_(2);

			lay;
		};

		label_mode = label_mode ? \property; // \full, \property

		scalarlist = pg.select({ arg param; 
			param.type == \scalar;
		});
		biglist = pg.select({ arg param;
			param.type != \scalar and: { 
				param.spec.isKindOf(AudioSpec).not
				and: { 
					// FIXME: find a better way to handle this
					param.type != \other 
				}
			}
		});

		// FIXME: not used ?!
		gridlayout = GridLayout.rows(*
			scalarlist.collect({ arg param;
				[
					if(label_mode == \full) {
						param.asStaticTextLabel;
					} {
						StaticText.new.string_(param.property)
					},
					param.asSlider.orientation_(\horizontal),
					param.asTextField,
				]
			})
		);
		gridlayout.setColumnStretch(0,2);
		gridlayout.setColumnStretch(1,6);
		gridlayout.setColumnStretch(2,2);

		// chipotage
		//if(biglist.size < 5 and: { scalarlist.size < 6 } ) {
		//	layout_type = VLayout;
		//} {
		//	layout_type = HLayout;
		//};
		layout_type = HLayout;

		biglayout = HLayout(*
			biglist.collect({ arg param;

				[
				View.new.layout_(VLayout(
					if(label_mode == \full) {
						param.asStaticTextLabel;
					} {
						StaticText.new.string_(param.property).maxHeight_(10)
					},
					param.asView,
					param.asTextField,
					View.new,
					nil,
				).margins_(0)).minWidth_(300).maxHeight_(200),
				align: \top,
				]
			})
		);

		layout = layout_type.new(
			HLayout(*scalarlist.clump(8).collect({ arg gr; gr.debug("gr"); vertical_slider_group.(gr) })),
			biglayout
		);
		layout;
		scrollview = ScrollView.new.canvas_(View.new.layout_(layout));
		^VLayout(scrollview)
	}

	*cursorRow { arg param;
	}
}
