
////////////////////// Basic building blocks

// misc stuff for param GUI
// define the default palette
ParamViewToolBox {

	classvar <>color_dark;
	classvar <>color_light;
	classvar <>color_pale;
	classvar <>color_playing;
	classvar <>color_userPlayed;
	classvar <>color_userStopped;
	classvar <>color_stopped;

	classvar <>color_startedRecording;
	classvar <>color_userStartedRecording;
	classvar <>color_userStoppedRecording;
	classvar <>color_stoppedRecording;

	classvar <>color_TextField_enabled;
	classvar <>color_TextField_disabled;

	classvar <>clipboard;

	*initClass {
		//color_ligth = Color.newHex("63AFD0");
		//color_dark = Color.newHex("0772A1");
		color_dark = Color(0.07843137254902, 0.13725490196078, 0.18039215686275); // Color.newHex("14232E");
		color_light = Color(0.30588235294118, 0.50196078431373, 0.52549019607843); // Color.newHex("4E8086");
		color_pale = Color(0.30588235294118, 0.50196078431373, 0.52549019607843).add(Color.grey,0.4); // Color.newHex("4E8086").add(Color.grey,0.4);

		//color_pale = Color.newHex("B9FFFC");
		color_playing = color_light;
		color_userPlayed = color_pale;
		color_userStopped = color_pale;
		color_stopped = Color.white;

		color_startedRecording = Color.red;
		color_userStartedRecording = Color.red.alpha_(0.2);
		color_userStoppedRecording = Color.red.alpha_(0.2);
		color_stoppedRecording = Color.white;

        color_TextField_enabled = Color.black;
        color_TextField_disabled = Color.grey(0.5);
		//color_ligth = Color.newHex("5CCCCC");
		//color_dark = Color.newHex("009999");

		clipboard = ();
	}


	*color_ligth{ ^color_light } // backward compat for wrong spelling :/

	*horizontal_slider { arg param, label_mode;
		var view = View.new;
		var slider, val, label;
		view.layout_(
			HLayout (
				label = StaticText.new,
				slider = param.asSlider.orientation_(\horizontal),
				val = param.asTextField(6),
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
			//param.debug("ParamViewToolBox.knob.mapParam");
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
			//param.debug("ParamViewToolBox.knob.mapParam");
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
				if(WindowDef(\ParamGenericContextMenu).notNil) {
					WindowDef(\ParamGenericContextMenu).sourceValue(param, vie).front;
				} {
					if(WindowDef(\ParamGenericOverlayMenu).notNil) {
						WindowDef(\ParamGenericOverlayMenu).front(vie, x, y, param)
					}
				}
			}
		};
        ^view;
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
		^view.followChange(param.target, \cursor, { arg view ...args;
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
				//[args[2], x].debug("bbb");
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
				//args.debug("cursor!!");
			})
			.states_([
				["", Color.black, Color.white],
				["", Color.black, ParamViewToolBox.color_light],
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

	*entryGroup { arg pgroup;
		^VLayout (
			*pgroup.collect { arg item, idx;
				ParamGroupLayout.formEntry(item)
			};
		)
	}

	*formEntry { arg item, label_mode;
		// if player, show a button
		// if param scalar, show horizontally a label, horizontal slider and textField
		// if param env or array, show vertically a label, an env/array, a textField

		// do not call .asView here because it call recursively formEntry !
		var minHeight = 70;
		var minWidth_main = 150;
		var minWidth_label = 80;
		var minWidth_right = 70;
   
		var scalar_entry = { arg param;
			var lay;
			lay = if(param.isNil) {
				nil;
			} {
              var control; 
				HLayout(
					if(label_mode == \full) {
						var st = param.asStaticTextLabel(\full);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					} {
						var st = StaticText.new.string_(param.propertyLabel);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					}.fixedWidth_(minWidth_label),
					control = param.asSlider.orientation_(\horizontal).minWidth_(minWidth_main),
					param.asTextField(6).fixedWidth_(minWidth_right),
				).addUniqueMethod(\slider, { control })
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
						param.asStaticTextLabel(\full).attachContextMenu;
					} {
						var st;
						st = StaticText.new.string_(param.propertyLabel);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					},
					param.asEnvelopeView.minHeight_(minHeight),
					param.asTextField,
				)
			};
			lay;
		};
		var array_entry = { arg param;
			var lay;
			lay = if(param.isNil) {
				nil;
			} {
				var control;
				VLayout(
					if(label_mode == \full) {
						param.asStaticTextLabel(\full).attachContextMenu;
					} {
						var st;
						st = StaticText.new.string_(param.propertyLabel);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					},
					control = param.asMultiSlider.minHeight_(minHeight).attachContextMenu,
					param.asTextField,
				).addUniqueMethod(\slider, { control })
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
						param.asStaticTextLabel(\full).attachContextMenu;
					} {
						var st;
						st = StaticText.new.string_(param.propertyLabel);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					}.fixedWidth_(minWidth_label),
					param.asPopUpMenu.minWidth_(minWidth_main),
					//param.asTextField.minWidth_(70),
				)
			};
			lay;
		};
		var popup_buffer_entry = { arg param;
			var lay;
			lay = if(param.isNil) {
				nil;
			} {
				HLayout(
					if(label_mode == \full) {
						param.asStaticTextLabel(\full).attachContextMenu;
					} {
						var st;
						st = StaticText.new.string_(param.propertyLabel);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					}.fixedWidth_(minWidth_label),
					param.asPopUpMenu.minWidth_(minWidth_main).mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
						//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
						if(buttonNumber == 1) {
							WindowDef(\GlobalLibrary_select).front(nil, { arg val; 
								//val.debug("selected");
								// val: [ Class, (key -> bufnum) ]
								param.set(val.last.value)
							}, [\AudioBuffer])
						}
					}),

					BasicButton.new.string_("Load").fixedWidth_(minWidth_right).action_({
						WindowDef(\filedialog_sample).front(nil, { arg path;
							switch(param.spec.numChannels,
								1, { param.set(BufDef.mono(path).bufnum) },
								2, { param.set(BufDef.stereo(path).bufnum) },
								{ param.set(BufDef(path).bufnum) }
							);
							 {
								param.spec.tagSpec.changed(\list);
								param.sendChanged;
							}.defer(0.1);
				
						})
					})
				)
			};
			lay;
		};
		var popup_busmap_entry = { arg param;
			var lay;
			lay = if(param.isNil) {
				nil;
			} {
				HLayout(
					if(label_mode == \full) {
						param.asStaticTextLabel(\full).attachContextMenu;
					} {
						var st;
						st = StaticText.new.string_(param.propertyLabel);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					}.fixedWidth_(minWidth_label),
					param.asBusPopUpMenu.minWidth_(minWidth_main),
					//param.asTextField.minWidth_(70),
				)
			};
			lay;
		};
		var failback_entry = { arg param;
			var lay;
			lay = if(param.isNil) {
				nil;
			} {
				HLayout(
					if(label_mode == \full) {
						param.asStaticTextLabel(\full).attachContextMenu;
					} {
						var st;
						st = StaticText.new.string_(param.propertyLabel);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					}.fixedWidth_(minWidth_label),
					param.asStaticText.minWidth_(minWidth_main),
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
					case(
						{ item.type == \env }, {
							env_entry.(item)
						},
						{ item.type == \array }, {
							array_entry.(item)
						},
						{ item.spec.isKindOf(ParamAudioBufferSpec) }, {
							popup_buffer_entry.(item)
						},
						{ item.spec.isKindOf(TagSpec) }, {
							popup_entry.(item)
						},
						{ item.spec.isKindOf(ParamBusSpec) }, {
							popup_entry.(item)
						},
						{ item.spec.isKindOf(ParamMappedBusSpec) }, {
							popup_busmap_entry.(item)
						}, {
							failback_entry.(item)
						}
					);
				}
			},
			{ item.isKindOf(PlayerWrapper) }, {
				item.asView.rightClickEditorEnabled_(true)
			}, {
				PlayerWrapper(item).asView.rightClickEditorEnabled_(true)
			}
		)
	}

	*buffer_load_button { arg param;
		^BasicButton.new.string_("Load").action_({
			WindowDef(\filedialog_sample).front(nil, { arg path;
				switch(param.spec.numChannels,
					1, { param.set(BufDef.mono(path)) },
					2, { param.set(BufDef.stereo(path)) },
					{ param.set(BufDef(path)) }
				);
				{
					param.spec.tagSpec.changed(\list);
					param.sendChanged;
				}.defer(0.1);

			})
		}).mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(buttonNumber == 1) {
				Menu(
					Menu(
						* TagSpecDef(\RandomBufferLibrary).list.collect { arg asso, idx;
							var key = asso.key;
							var val = asso.value;

							MenuAction(key, {
								var path = val[val.size.rand].value;
								switch(param.spec.numChannels,
									1, { param.set(BufDef.mono(path)) },
									2, { param.set(BufDef.stereo(path)) },
									{ param.set(BufDef(path)) }
								);
								{
									param.spec.tagSpec.changed(\list);
									param.sendChanged;
								}.defer(0.1);
							})
						}
					).title_("Random")
				).front;
			};
		})

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

                    //param.asPopupView;
                    // FIXME: why special for TagSpec ? 
                    // maybe i broke everything by commenting this code
                    if(param.spec.isKindOf(TagSpec) or: param.spec.isKindOf(ParamBufferSpec) ) {
                        param.asValuePopUpMenu;
                    } {
                        param.asBusPopUpMenu;
                    },
					if(param.spec.isKindOf(ParamBufferSpec)) {
						this.buffer_load_button(param)
					} {
						nil
					};
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
					param.asTextField(6),
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
				param.asView(label_mode);
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
					//param.debug("what param");
					if(param.isNil) {
						nil ! 3;
						//{ Button.new } ! 3
					} {
						[
							if(label_mode == \full) {
								param.asStaticTextLabel.attachContextMenu;
							} {
								var st;
								st = StaticText.new.string_(param.property);
								ParamViewToolBox.attachContextMenu(param, st);
								st;
							},
							param.asSlider.orientation_(\horizontal).minWidth_(150),
							param.asTextField(6).minWidth_(70),
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
						param.asStaticTextLabel.attachContextMenu;
					} {
						var st;
						st = StaticText.new.string_(param.property);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
					},
					param.asSlider.orientation_(\horizontal),
					param.asTextField(6),
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
						param.asStaticTextLabel.attachContextMenu;
					} {
						var st;
						st = StaticText.new.string_(param.property).maxHeight_(10);
						ParamViewToolBox.attachContextMenu(param, st);
						st;
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
			HLayout(*scalarlist.clump(8).collect({ arg gr; 
				//gr.debug("gr"); 
				vertical_slider_group.(gr)
		   	})),
			biglayout
		);
		layout;
		scrollview = ScrollView.new.canvas_(View.new.layout_(layout));
		^VLayout(scrollview)
	}

	*knobView { arg param, showValue=false;
		var pa = param;
		var lay;
		var label = pa.asStaticTextLabel;
		ParamViewToolBox.attachContextMenu(pa, label);
		lay = VLayout (
			[label, align:\center],
			pa.asKnob.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				if(buttonNumber == 2) {
					Param.lastTweaked = pa;
					Param.changed(\lastTweaked);
					false;
				} {
					if(buttonNumber == 1) {
						view.mode = \horiz;
					};
				}
			})
			.mouseMoveAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				if(buttonNumber == 2) {
					false;
				}
			})
			.mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				if(buttonNumber == 2) {
					false;
				} {
					if(buttonNumber == 1) {
						view.mode = \round;
					}
				}
			})
			.centered_(pa.spec.minval == pa.spec.maxval.neg),
		);
		if(showValue == true) {
			lay.add([param.asTextField, align:\center])
		};
		^lay
	}

	*cursorRow { arg param;
	}
}
