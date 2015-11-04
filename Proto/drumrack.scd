
"Init drumrack.scd".postln;


~class_preset = (
	eventClass: \preset,
	new: { arg self, instr;
	
		if(instr.tryPerform(\eventClass) == \preset) {
			instr.clone;
		} {
			self = self.deepCopy;

			if(instr.class == Symbol) {
				instr = Pdef(instr);
			};
			self.instr = instr;
			self.params = ();

			self
		}
	},

	new_load: { arg eventclass, data;
		var self = eventclass.new(data.instr);
		self.params = data.params.collect({ arg x;
			// TODO: hack: find a better way to save and load buffer
			//if(x.class == SamplePlaceholder) {
			//	x.load;
			//} {
			//	x
			//}
			x;
		});
		self;
	},

	asArchiveData: { arg self;
		(
			instr: self.instr.key,
			params: self.params.collect({ arg x; 
				// TODO: hack: find a better way to save and load buffer
				if(x.class == Buffer) {
					SampleProxy.new(x.path, x.numChannels)
				} {
					x
				} 
			}),
			load: { arg x; ~class_preset.new_load(x) },
		)
	},

	clone: { arg self;
		var preset;
		preset = ~class_preset.new(self.instr);
		preset.params = self.params.deepCopy;
		preset;
	},

	isSpecialSpec: { arg self, obj;
		// disable when new control GUIs are written
		[
			//XBufferSpec,
			XTrigSpec,
			XGateSpec,
			XBusSpec,
			XWavetableSpec,
			XDoneActionSpec,
		].any({ arg x; obj.isKindOf(x) })
	},

	paramlist: { arg self;
		var res;
		res = self.instr.getHalo(\params).collect { arg param;
			var retparam;
			if(param.isSequenceableCollection.not) {
				param = [ param ];
			};
			if(param[1].isNil) {
				var spec;
				//spec = self.instr.getSpec(param[0]);
				//if(spec.isNil) {
				//	spec = self.instr.getHalo(\instrument);

				//};
				spec = Param(self.instr, param[0]).spec; // TODO: maybe avoid creating a whole Param object
				param.add(spec);
			};
			if(self.isSpecialSpec(param[1])) {
				// also filtered by .type in ~class_drumrack
				// disable when new control GUIs are written
				retparam = nil;
			} {
				self.params.addHalo(\instrument, self.instr.getHalo(\instrument));
				retparam = Param(self.params.addHalo(\nestMode, true), *param);
				//if(self.params[param[0]].isNil) {
				//	if(self.instr.getHalo(\instrument).notNil) {
				//		var defval = Param.getSynthDefDefaultValue(param[0], self.instr.getHalo(\instrument));
				//		if(retparam.type == \env) {
				//			defval = defval.asEnv;
				//		};
				//		retparam.set(defval); // should be retparam.default but not yep implemented
				//	} {
				//		retparam.set(retparam.spec.default); // should be retparam.default but not yet implemented
				//	}
				//};
			};
			retparam;
		}.select(_.notNil);
		res = res ? [];
		//[
		//	Param(self.params, \amp, \amp),
		//	Param(self.params, \pan, \pan),
		//]
	},

	as_pattern: { arg self;
		//Prout({ arg ev;
		//	var istr = self.instr.asStream;
		//	var iev;

		//	inf.do { arg x;
		//		iev = istr.next(ev.composeEvents(self.params));
		//		iev
		//		self.instr.a
		//	
		//	};
		//	self.instr.do({ arg iev;
		//		iev

		//	
		//	}, );
		//	ev.composeEvents()
		//});
		self.instr <> Pn(self.params)
	}
);

~class_presetgroup = (
	new: { arg self;
		self = self.deepCopy;
	
		self.list = List.new;
		self.proxy = PatternProxy.new;
		self.update_proxy;
	
		self;
	},

	groupsize: { arg self;
		self.list.size;
	},

	clone: { arg self;
		var pg = ~class_presetgroup.new;
		pg.list = self.list.collect({ arg x; x.clone });
		pg.update_proxy;
		pg;
	},

	new_load: { arg eventclass, data;
		var self = eventclass.new;
		self.list = List.newFrom(data.list.collect({ arg x; x.load; }));
		self.update_proxy;
		self;
	},

	asArchiveData: { arg self;
		(
			list: self.list.collect({ arg x; x.asArchiveData }),
			load: { arg self; ~class_presetgroup.new_load(self); }
		)
	},

	add_preset: { arg self, preset;
		self.list.add(preset);
		self.update_proxy;
	},

	add_from_lib: { arg self, lib, key;
		[lib.key, key].debug("add_from_lib");
		lib.lib[key].list.debug("list");
		self.list.collect(_.keys).debug("before");
		lib.lib[key].list.collect({ arg x; self.list.add(x.clone.debug("clone")) });
		self.list.collect(_.keys).debug("after");
		self.update_proxy;
	},

	remove_preset_at: { arg self, idx;
		self.list.removeAt(idx);
		self.update_proxy;
	},

	update_proxy: { arg self;
		self.proxy.source = if(self.list.size == 0) {
			Ptuple([(isRest: true, type: \rest)])
		} {
			Ptuple(self.list.collect({ arg item;
				item.as_pattern;
			}))
		};

		self.patternList = if(self.list.size == 0) {
			[(isRest: true, type: \rest)]
		} {
			self.list.collect({ arg item;
				item.as_pattern;
			})
		};

	},

	as_stream: { arg self;
		self.proxy.asStream;
	},

	remove_all_preset: { arg self;
		self.list = List.new;
		self.update_proxy;
	},

	play_preset: { arg self, ev=(), no_latency=false;
		if(no_latency != false) {
			var nola, note;
			nola = Pn((latency: Ref(nil)),inf).asStream.next(ev);
			note = self.proxy.asStream.next(ev);
			[nola, note].debug("nola, note");
			nola.composeEvents( note ).debug("WAAAA play_preset");
			note.collect({ arg no; no.composeEvents(nola) }).debug("WAAAA play_preset").play;
		} {
			self.as_stream.next(ev).play;
		}
	},
);


~class_drumrack = (
	new: { arg self;
		self = self.deepCopy;

		//self.drumrack_size = 128;
		self.drumrack_size = 16;
	
		self.pads = List.newClear(self.drumrack_size).collect({
			~class_presetgroup.new;
		});
		self.padstreams = List.newClear(self.drumrack_size);
		self.lib_drumpad = {DrumRack.lib_drumpad};
		self.lib_drumrack = { DrumRack.lib_drumrack };
		self.lib_instr = { DrumRack.lib_instr };
		self.lib_score = { DrumRack.lib_score };
	
		self;
	},

	clone: { arg self;
		var res = ~class_drumrack.new;
		res.lib_drumrack = self[\lib_drumrack];
		res.lib_drumpad = self[ \lib_drumpad ];
		res.lib_score = self[ \lib_score ];
		res.lib_instr = self[ \lib_instr ];
		res.pads = self.pads.collect({ arg x; x.clone });
		res;
	},

	new_load: { arg eventclass, data;
		var self = eventclass.new(data.pads.size);
		self.pads = List.newFrom(data.pads.collect({ arg x; x.load; }))
	},

	asArchiveData: { arg self;
		(
			pads: self.pads.collect({ arg x; x.asArchiveData }),
			load: { arg self; ~class_drumrack.new_load(self); }
		)
	},

	//////////////////

	add_to_pad: { arg self, padidx, instr;
		self.pads[padidx] = self.pads[padidx].add(~class_presetgroup.new(instr));
	},

	set_pad: { arg self, padidx, presetgroup;
		self.pads[padidx] = presetgroup;
	},

	get_pad: { arg self, idx;
		self.pads[idx];
	},

	pad_is_empty: { arg self, idx;
		self.pads[idx].isNil or: { self.pads[idx].list.size == 0 }
	},

	play_pad: { arg self, idx, ev;
		ev = ev ?? { () };
		idx.debug("play_pad!!");
		Ppar(
			self.pdrumstep.dictNext(idx, ev).collect({ arg x; Pn((latency: Ref(nil))) <> Pn(x,1) }),
		).trace.play
	},
	

	padstream: { arg self, idx;
		if(self.padstreams[idx].isNil) {
			if(self.pads[idx].notNil and: { self.pads[idx].groupsize > 0 }) {
				self.padstreams[idx] = self.pads[idx].as_stream;
			} {
				self.padstreams[idx] = [(isRest: true).asStream];
			};
			self.padstreams[idx];
		} {
			self.padstreams[idx];
		}
	},

	pdrumstep: { arg self;
		if(self.pr_pdrumstep.isNil) {
			self.pr_pdrumstep = PdrumStep(self.pads, Pbind());
			self.pr_pdrumstep;
		} {
			self.pr_pdrumstep;
		}
	},

	as_pattern: { arg self, score;
		if(score.notNil) {
			PdrumStep(self.pads, score);
		} {
			nil
		};
	},

);

~class_drumrack_proxy = (
	// replaced by DrumRack
	new: { arg self, drumrack;
		self = self.deepCopy;
	
		self.drumrack = drumrack;
	
		self;
	},

	set_drumrack: { arg self, drumrack;
		self.drumrack = drumrack;
		if(self.current_pdrumstep.notNil) {
			self.current_pdrumstep.dict = drumrack.pads;
		};
	},

	as_pattern: { arg self, score;
		if(score.notNil) {
			self.current_pdrumstep = PdrumStep(self.drumrack.pads, score);
			self.current_pdrumstep;
		} {
			nil
		};
	},
);

~class_drumrack_view = (
	new: { arg self, drumrackproxy;
		self = self.deepCopy;

		self.drumrackproxy = drumrackproxy;
	
		self.pad_size = Point(4,4);
		self.make_window;
		self.select_drumpad(0);
	
		self;
	},

	select_drumpad: { arg self, idx;
		self.set_drumpad(self.drumrack.get_pad(idx));
		self.selected_drumpad_index = idx;
	},

	drumrack: { arg self;
		self.drumrackproxy.drumrack;
	},

	set_drumpad: { arg self, drumpad;
		// currently edited drumpad
		if(drumpad.notNil) {
			self.drumpad = drumpad;
			self.drumpad_row_selector = ~selector.new(self.drumpad.list.size, { self.refresh_env_panel }, { arg x; x.maxWidth_(15) });
			self.refresh_drumpad;
		} 
	},

	make_drumpreset_layout: { arg self, drumpreset, rowidx;
		HLayout(*
			[
				StaticText.new.string_(drumpreset.instr.key + rowidx),
				self.drumpad_row_selector.views[rowidx],
			]
			++
			drumpreset.paramlist.select({ arg x; x.type == \scalar }).keep(32)
				.collect({ arg param, x;
					[x, param].debug("Param");
					self.make_knob(param.property).mapParam(param);
				})
			++
			[ nil ]
		)
	},

	refresh_env_panel: { arg self;
		if( self.drumpad.list[self.drumpad_row_selector.selected_index].notNil) {

			self.drumpad.list[self.drumpad_row_selector.selected_index].paramlist.debug("refresh_env_panel");
			self.env_panel.removeAll;
			self.env_panel.layout = HLayout(*
				self.drumpad.list[self.drumpad_row_selector.selected_index].paramlist
					.select({ arg x;
						x.type != \scalar and: { x.type != \other or: { x.spec.isKindOf(XBufferSpec) } };
					}).do({ arg x; x.property.debug("refresh_env_panel: selected params") })
					.collect({ arg param;
						if(param.spec.isKindOf(XBufferSpec)) {
							VLayout(
								StaticText.new.string_(param.property),
								param.asView,
							)
						} {
							VLayout(
								StaticText.new.string_(param.property),
								param.asView,
								param.asTextField,
							)
						}
					})
			)
		} {
			self.env_panel.removeAll;
			self.drumpad.list.debug("refresh_env_panel : no selected");
		}
	},

	refresh_drumpad: { arg self;
		if(self.drumpad_view.notNil) {
			if(self.drumpad.notNil) {
				self.drumpad_view.removeAll;
				[self.drumpad.list.size, self.drumpad.list.collect(_.keys)].debug("class_drumrack_view.refresh_drumpad: drumpads");
				self.drumpad_row_selector.resize(self.drumpad.list.size);
				self.drumpad_view.layout = VLayout(*
					self.drumpad.list.collect({ arg preset, x;
						self.make_drumpreset_layout(preset, x)
					})
					++
					[ nil ]
				)
			} {
				self.drumpad_view.removeAll;
			}
		};
		self.refresh_env_panel;

	},

	make_window: { arg self;
		var window = Window.new;
		var layout;
		layout = self.make_layout;
		window.layout = layout;
		//window.addToOnClose({
		window.view.keyDownAction = { arg me, key, modifiers, unicode, keycode;
			[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");
			if(key == $\r) {
				self.drumpad.play_preset;
			}
		};
		window.onClose_({
			debug( "closing!");
			self.drumrack.lib_drumrack.debug("lib_drumrack").save;
			PresetDictionary(\DrumRack).save;
		});
		//window.alwaysOnTop = true;
		window.front;
		
	},

	make_knob: { arg self, label;
		ParamView.label_knob(label).maxWidth_(100);
	},

	refresh_pads: { arg self;
		self.pad_views.do { arg view, x;
			if(self.drumrack.pad_is_empty(x)) {
				view.background = Color.clear;
			} {
				view.background = ParamView.color_ligth;
			}
		}
	},

	refresh_drumpreset_popup: { arg self;
		if(self.drumpreset_popup.notNil) {
			var idx = self.drumpreset_popup.value;
			self.drumpreset_popup.items_(self.drumrack.lib_drumpad.get_list);
			self.drumpreset_popup.value = idx;
			self.selected_drumpreset_to_add = self.drumpreset_popup.items[self.drumpreset_popup.value];
		}
	},

	refresh_rackpreset_popup: { arg self;
		if(self.rackpreset_popup.notNil) {
			var idx = self.rackpreset_popup.value;
			self.rackpreset_popup.items_(self.drumrack.lib_drumrack.get_list);
			//if(self.drumrack.notNil)
			//self.rackpreset_popup.value = idx;
			self.rackpreset_popup.value = self.rackpreset_popup.items.detectIndex({ arg x;
				self.drumrack.lib_drumrack[x] === self.drumrack; // identity because in-place preset system
			})
		}
	},

	make_save_drumpreset_window: { arg self, drumpad;
		var window = Window.new;
		var layout;
		var preset_name_view;
		var lib = self.drumrack.lib_drumpad;
		layout = VLayout(
			preset_name_view = TextField.new,
			ListView.new.items_( lib.get_list ).value_(nil).action_({ arg view;
				preset_name_view.value = view.items[view.value];
			}),
			HLayout(
				XSimpleButton.new.label_("Cancel").action_({
					window.close;
				}),
				XSimpleButton.new.label_("Save").action_({
					var name = preset_name_view.value;
					if(name != "" and: { drumpad.notNil }) {
						lib.save_preset(name.asSymbol, drumpad.clone);
						window.close;
						self.refresh_drumpreset_popup;
					};
				}),
			)
			
		);
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
		
	},

	make_load_drumpreset_window: { arg self, drumpad;
		var window = Window.new;
		var layout;
		var preset_name_view;
		var lib = self.drumrack.lib_drumpad;
		layout = VLayout(
			preset_name_view = TextField.new,
			ListView.new.items_( lib.get_list ).value_(nil).action_({ arg view;
				preset_name_view.value = view.items[view.value];
				self.drumrack.lib_drumpad.lib[preset_name_view.value.asSymbol].play_preset((), true);
			}),
			HLayout(
				XSimpleButton.new.label_("Cancel").action_({
					window.close;
				}),
				XSimpleButton.new.label_("Load").action_({
					var name = preset_name_view.value;
					if(name != "" and: { drumpad.notNil }) {
						self.drumpad.remove_all_preset;
						self.drumrack.get_pad(self.selected_drumpad_index).add_from_lib(
							self.drumrack.lib_drumpad,
							name.asSymbol
						);
						self.refresh_drumpad;
						self.refresh_pads;
						window.close;
					};
				}),
			)
			
		);
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
		
	},

	make_save_rackpreset_window: { arg self;
		var window = Window.new;
		var layout;
		var preset_name_view;
		var drumrack = self.drumrack;
		var lib = drumrack.lib_drumrack;
		layout = VLayout(
			preset_name_view = TextField.new,
			ListView.new.items_( lib.get_list ).value_(nil).action_({ arg view;
				preset_name_view.value = view.items[view.value];
			}),
			HLayout(
				XSimpleButton.new.label_("Cancel").action_({
					window.close;
				}),
				XSimpleButton.new.label_("Save").action_({
					var name = preset_name_view.value;
					if(name != "" and: { drumrack.notNil }) {
						lib.save_preset(name.asSymbol, drumrack.clone);
						window.close;
						self.refresh_rackpreset_popup;
					};
				}),
			)
			
		);
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
		
	},

	make_new_rackpreset_window: { arg self;
		var window = Window.new;
		var layout;
		var preset_name_view;
		var drumrack = self.drumrack;
		var lib = drumrack.lib_drumrack;
		var based_on;
		layout = VLayout(
			preset_name_view = TextField.new,
			StaticText.new.string_("Based on:"),
			ListView.new.items_( ["--- None --- "] ++ lib.get_list ).value_(0).action_({ arg view;
				if(view.value == 0) {
					based_on = nil;
				} {
					based_on = view.items[view.value].asSymbol;
				}
			}),
			HLayout(
				XSimpleButton.new.label_("Cancel").action_({
					window.close;
				}),
				XSimpleButton.new.label_("New").action_({
					var name = preset_name_view.value;
					if(name != "" and: { drumrack.notNil }) {
						lib.save_preset(name.asSymbol, self.make_new_rack(based_on));
						self.refresh_rackpreset_popup;
						self.load_rackpreset(name.asSymbol);
						window.close;
					};
				}),
			)
			
		);
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
		
	},

	load_rackpreset: { arg self, rackpreset;
		var rack;
		"load_rackpreset: begin".debug;
		self.current_rackpreset_name = rackpreset; 
		//rack = rack.clone; // loaded racks are independent of the saved rack
		self.drumrackproxy.loadDrumrack(rackpreset);
		//[self.keys, self.lib_drumpad.lib.keys, self.lib_drumrack.lib.keys].debug("make_new_rack: self rack");
		//[rack.keys, rack.lib_drumpad.lib.keys, rack.lib_drumrack.lib.keys].debug("make_new_rack: self rack");
		self.selected_drumpad_index.debug("load_rackpreset:selected_drumpad_index");
		self.set_drumpad(self.drumrack.get_pad(self.selected_drumpad_index));
		self.refresh_drumpad;
		self.refresh_pads;
		self.refresh_rackpreset_popup;
	},

	make_new_rack: { arg self, basename;
		var rack;
		if(basename.notNil) {
			rack = self.drumrack.lib_drumrack.lib[basename];
			rack = rack.clone; // loaded racks are independent of the saved rack
			rack;
		} {
			rack = ~class_drumrack.new;
			//rack.lib_drumpad = self.drumrack.lib_drumpad;
			//rack.lib_drumrack = self.drumrack.lib_drumrack;
			//[self.keys, self.lib_drumpad.lib.keys, self.lib_drumrack.lib.keys].debug("make_new_rack: self rack");
			//[rack.keys, rack.lib_drumpad.lib.keys, rack.lib_drumrack.lib.keys].debug("make_new_rack: self rack");
			rack;
		}
		
	},

	make_layout: { arg self;
		var padlayout, paramlayout;
		var padheader, paramheader;
		var envpanel;
		var pm = PopUpMenu.new;
		self.drumpreset_popup = pm;
		self.rackpreset_popup = PopUpMenu.new;

		self.drumpad_selector = ~selector.new(self.pad_size.x * self.pad_size.y, { arg idx, idx2;
			[idx, idx2].debug("selecting pad !!!");
			self.select_drumpad(idx)
		});

		self.pad_views = List.new;

		padlayout = GridLayout.rows(*
			self.pad_size.x.collect({ arg x;
				self.pad_size.y.collect({ arg y;
					var idx = (x*self.pad_size.x+y);
					VLayout(
						self.pad_views.add( 
							XSimpleButton.new.label_((idx).asString).fixedSize_(Point(64,64-20))
							.action_({
								self.drumrack.play_pad(idx)
							}));
							self.pad_views.last,
						self.drumpad_selector.views.collect({ arg view, vidx;
							view.fixedSize_(Point(64,20));
							view.label_("edit");
						}).at(idx)
					).spacing_(0)
				})
			})
		);

		self.drumpad_view = View.new;
		paramlayout = ScrollView.new.canvas_(self.drumpad_view);

		self.refresh_rackpreset_popup;
		self.rackpreset_popup.action = { arg view;
			view.items[view.value].debug("load rackpreset!!!");
			self.load_rackpreset(view.items[view.value]);
		};
		padheader = HLayout(
			if(self.drumrackproxy.playerWrapper.notNil) {
				PlayerWrapper(self.drumrackproxy.playerWrapper).asView
			} { 
				nil
			},
			self.rackpreset_popup,
			//{
			//	var rpp = self.rackpreset_popup;
			//	rpp.items = self.drumrack.lib_drumrack.get_list;
			//	rpp.action = {
			//		rpp.items[rpp.value].debug("load rackpreset!!!");
			//		self.load_rackpreset(rpp.items[rpp.value]);
			//	};
			//	rpp;
			//}.value,
			XSimpleButton.new.label_("New Rack").action_({
				//self.make_save_rackpreset_window;
				self.make_new_rackpreset_window;
			})
		);

		paramheader = HLayout(
			{
				"1".debug;
				pm.items = self.drumrack.lib_drumpad.get_list;
				pm.action = {
					self.selected_drumpreset_to_add = pm.items[pm.value];
				};
				"1".debug;
				"2".debug;
				[ pm.value, pm.items ].debug("val, item");
				if(pm.items.size > 0) {
					self.selected_drumpreset_to_add = pm.items[pm.value];
				};
				"3".debug;
				//pm.onChange(subparam.target, \set, {
				//	var val;
				//	//"there is change! my lord!".debug;
				//	pm.value = pm.items.detectIndex({ arg x; x == subparam.get })
				//});
				//pm.value = pm.items.detectIndex({ arg x; x == subparam.get });
				pm;
			}.value,
			"1".debug;
			XSimpleButton.new.label_("+").action_({ 
				if(self.selected_drumpreset_to_add.notNil) {
					self.drumrack.get_pad(self.selected_drumpad_index).add_from_lib(
						self.drumrack.lib_drumpad,
						self.selected_drumpreset_to_add
					);
					self.refresh_drumpad;
					self.refresh_pads;
				}
			}),
				"1".debug;
			XSimpleButton.new.label_("-").action_({ 
				self.drumrack.get_pad(self.selected_drumpad_index).remove_preset_at(
					self.drumpad_row_selector.selected_index
				);
				self.refresh_drumpad;
				self.refresh_pads;
			}),
				"1".debug;
			XSimpleButton.new.label_("Save preset").action_({ 
				self.make_save_drumpreset_window(self.drumpad);
			}),
			XSimpleButton.new.label_("Load preset").action_({ 
				self.make_load_drumpreset_window(self.drumpad);
			}),
		);
		"xxx1".debug;

		//self.env_panel = View.new.minSize_(Point(300,300)).background_(Color.red);
		self.env_panel = View.new.fixedHeight_(150);
		envpanel = self.env_panel;

		self.layout = GridLayout.columns(
			[
				padheader,
				VLayout(
					[View.new.layout_(padlayout).background_(Color.clear), stretch:0],
					nil
				),
			],
			[
				paramheader,
				VLayout(
					paramlayout,
					envpanel,
				)
			],
		);

		self.refresh_pads;

		self.layout;
	},

);
