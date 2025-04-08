

(
WindowDef(\PbindefEditor, (
	isPstepSeqParam: { arg self, param;
		param.get.isKindOf(PstepSeq)
	},

	showParamOverlayMenu: { arg self, param, editorView, textView, view;
		Log(\Param).debug("PbindefEditor: param %".format(param));
		Menu(*
			WindowDef(\ParamGenericContextMenuEntries).sourceValue(param) ++
			[
				MenuAction.separator, /// Pbindef menu

				if(param.parent.type == \scalar) {
					// can change only scalar parameter
					var curform = self.getParamForm(param);
					//"hello1.1".debug;
					Menu(*
						self.paramForms.collect { arg form, idx;
							MenuAction(form, {
								// why unmapping params ?
								if(textView.notNil) {
									textView.unmapParam;
								};
								//Log(\Param).debug("editorView.layout.innerView %", editorView.layout.innerView);
								if(editorView.layout.innerView.notNil) {
									editorView.layout.innerView.unmapParam;
								};
								self.setParamForm(param, self.paramForms[idx], editorView);

							}).checked_(curform == form)
						}
					).title_("Param form")
				},
				if(param.spec.isKindOf(ParamBoolSpec)) {
					// can change only scalar parameter
					var curform = self.getParamForm(param);
					var forms = self.paramForms.reject({ arg x;
						[].includes(x)
					});
					//"hello1.1".debug;
					Menu(*
						self.paramForms.collect { arg form, idx;
							MenuAction(form, {
								// why unmapping params ?
								if(textView.notNil) {
									textView.unmapParam;
								};
								//Log(\Param).debug("editorView.layout.innerView %", editorView.layout.innerView);
								if(editorView.layout.innerView.notNil) {
									editorView.layout.innerView.unmapParam;
								};
								self.setParamForm(param, self.paramForms[idx], editorView);

							}).checked_(curform == form)
						}
					).title_("Param form")
				},
				if(self.sustainKeys.includes(param.propertyRoot)) {
					Param(Message(self), \legatoSustainMode, TagSpec(self.sustainKeys)).asMenu("Legeto mode")
				},
				if(self.noteKeys.includes(param.propertyRoot)) {
					Param(Message(self), \noteMode, TagSpec(self.noteKeys)).asMenu("Note mode")
				},


				MenuAction.separator,

				if(param.type == \array or: { param.get.isSequenceableCollection }) {
					var model;
					self.arrayControllerDict = self.arrayControllerDict ?? { IdentityDictionary.new };
					model = self.arrayControllerDict[param.propertyRoot] ?? { ProtoClass((
						arrayController_: { arg me, con;
							me[\arrayController] = con;
							self.setArrayController(param, con)
						},
					)) };
					self.arrayControllerDict[param.propertyRoot] = model;

					Param(
						Message(model),
						\arrayController, 
						ControllerDef.getTagSpec(\knobline)
					).asMenu.title_("Array controller")
				},
			].select(_.notNil)
		).front
	},

	showParamOverlayMenu_oldmenu: { arg self, param, editorView, textView, view;
		Menu(*
			[
				MenuAction(param.fullLabel).enabled_(false),
				MenuAction.separator,
				Param(Message(param), \inBusMode, ParamBoolSpec()).asMenuAction("Bus mode"),
				MenuAction("Override pattern", {
					if(param.target.isKindOf(EventPatternProxy)) {
						Pbindef(param.target.key, param.propertyRoot, Pkey(param.propertyRoot));
					} {
						Log(\Param).info("Can't override, not an EventPatternProxy");
					}
				}),
				MenuAction("Set default", {
					param.set(param.default)
				}),
				MenuAction("Unset", {
					param.unset;
				}),
				Menu(
					CustomViewAction(
						param.asTextField.minWidth_(500);
					)
				).title_("Edit text"),

				MenuAction.separator, /// Pbindef menu

				if(param.parent.type == \scalar) {
					// can change only scalar parameter
					var curform = self.getParamForm(param);
					//"hello1.1".debug;
					Menu(*
						self.paramForms.collect { arg form, idx;
							MenuAction(form, {
								// why unmapping params ?
								if(textView.notNil) {
									textView.unmapParam;
								};
								//Log(\Param).debug("editorView.layout.innerView %", editorView.layout.innerView);
								if(editorView.layout.innerView.notNil) {
									editorView.layout.innerView.unmapParam;
								};
								self.setParamForm(param, self.paramForms[idx], editorView);

							}).checked_(curform == form)
						}
					).title_("Param form")
				},
				if(param.spec.isKindOf(ParamBoolSpec)) {
					// can change only scalar parameter
					var curform = self.getParamForm(param);
					var forms = self.paramForms.reject({ arg x;
						[].includes(x)
					});
					//"hello1.1".debug;
					Menu(*
						self.paramForms.collect { arg form, idx;
							MenuAction(form, {
								// why unmapping params ?
								if(textView.notNil) {
									textView.unmapParam;
								};
								//Log(\Param).debug("editorView.layout.innerView %", editorView.layout.innerView);
								if(editorView.layout.innerView.notNil) {
									editorView.layout.innerView.unmapParam;
								};
								self.setParamForm(param, self.paramForms[idx], editorView);

							}).checked_(curform == form)
						}
					).title_("Param form")
				},
				if(self.sustainKeys.includes(param.propertyRoot)) {
					Param(Message(self), \legatoSustainMode, TagSpec(self.sustainKeys)).asMenu("Legeto mode")
				},
				if(self.noteKeys.includes(param.propertyRoot)) {
					Param(Message(self), \noteMode, TagSpec(self.noteKeys)).asMenu("Note mode")
				},


				MenuAction.separator,

				if(param.type == \array or: { param.get.isSequenceableCollection }) {
					var model;
					self.arrayControllerDict = self.arrayControllerDict ?? { IdentityDictionary.new };
					model = self.arrayControllerDict[param.propertyRoot] ?? { ProtoClass((
						arrayController_: { arg me, con;
							me[\arrayController] = con;
							self.setArrayController(param, con)
						},
					)) };
					self.arrayControllerDict[param.propertyRoot] = model;

					Param(
						Message(model),
						\arrayController, 
						ControllerDef.getTagSpec(\knobline)
					).asMenu.title_("Array controller")
				},
				MenuAction("MIDI CC learn and map", {
					var midi = param.target.getHalo(\MIDIFunc, param.property);
					if(midi.notNil) {
						midi.free;
						param.target.addHalo(\MIDIFunc, param.property, nil);
					};
					midi = MIDIFunc.cc({ arg ... args;
						//args.debug("MENU MIDI Func");
						param.normSet(args[0]/127)
					}).fix.learn;
					param.target.addHalo(\MIDIFunc, param.property, midi);
				}),
				MenuAction(
					if(param.target.getHalo(\MIDIFunc, param.property).notNil) {
						"Clear MIDI (%)".format(param.target.getHalo(\MIDIFunc, param.property).msgNum);
					} {
						"Clear MIDI (not set)";
					},
					{
						var midi = param.target.getHalo(\MIDIFunc, param.property);
						if(midi.notNil) {
							midi.free;
							param.target.addHalo(\MIDIFunc, param.property, nil);
						};
					}
				),

				MenuAction.separator,
				if(param.hasCombinator) {
					Menu(
						CustomViewAction(
							View.new.layout_(WindowDef(\ParamCombinatorModulatorEditor).asView(param))
						)
					).title_("Modulators")
				} {
					MenuAction("Add modulator", {
						var combi = ParamCombinator(param.parent).setBusMode(true);
						WindowDef(\ParamCombinatorModulatorEditor).front(param)
					})
				},
				MenuAction("Combinator editor", {
					var combi;
					combi = ParamCombinator(param.parent).setBusMode(true);
					WindowDef(( \ParamCombinator_++param.fullLabel ).asSymbol, WindowDef(\ParamCombinatorEditor)).front(combi);
					//param.set(param.default)
				}),

				MenuAction.separator,
				MenuAction("Edit target", {
					PlayerWrapper(param.target).edit;
				}),
				MenuAction("Scope bus", {
					var bus = param.get;
					if(bus.notNil) {
						WindowDef(\ScopeView).front(bus);
					} {
						Log(\Param).debug("bus is nil");
					}
				}),

				MenuAction.separator,
				MenuAction("Spec editor", {
					WindowDef(\ParamSpecEditor).front(param)
				})
			].select(_.notNil)
		).front
	},

	showParamOverlayMenu_old: { arg self, param, editorView, textView, view, x, y;
		WindowDef(\OverlayMenu).front(view, x, y, { arg def;
			var midiClearButton;
			var typePopup;
			var specEditorView = View.new;
			//Log(\Param).debug("showParamOverlayMenu: param % % %", param, param.type, param.parent.type);
			VLayout (
				* [
					//if(param.type == \array) {
						//HLayout (
							//StaticText.new.string_("List size"),
							//Param(Message(param), \size, ControlSpec(1,32,\lin,1,4)).asNumberBox.maxWidth_(100)
						//)
					//},
					TextField.new.mapParam(param),
					//"hello1".debug;
					if(param.parent.type == \scalar) {
						// can change only scalar parameter
					//"hello1.1".debug;
						typePopup = PopUpMenu.new.items_(self.paramForms).action_({ arg view;
							// why unmapping params ?
							if(textView.notNil) {
								textView.unmapParam;
							};
							//Log(\Param).debug("editorView.layout.innerView %", editorView.layout.innerView);
							if(editorView.layout.innerView.notNil) {
								editorView.layout.innerView.unmapParam;
							};
							self.setParamForm(param, self.paramForms[view.value], editorView);
							def.closeWindow;
						}).value_(self.paramForms.indexOf(self.getParamForm(param)));
					//"hello1.2".debug;
						HLayout (
							StaticText.new.string_("Param form"),
							typePopup,
						);
					},
					//"hello2".debug;
					if(self.sustainKeys.includes(param.propertyRoot)) {
						HLayout (
							StaticText.new.string_("Mode:"),
							Param(Message(self), \legatoSustainMode, TagSpec(self.sustainKeys)).asPopUpMenu,
						)
					},
					if(self.noteKeys.includes(param.propertyRoot)) {
						HLayout (
							StaticText.new.string_("Mode:"),
							Param(Message(self), \noteMode, TagSpec(self.noteKeys)).asPopUpMenu,
						)
					},
					StaticText.new.string_(""),
					Param(Message(param), \inBusMode, ParamBoolSpec()).asButton("Bus mode"),
					HLayout (
						BasicButton.new.string_("Add modulator").action_({
							WindowDef(\ListSelectDialog).front(TagSpecDef(\ParamCombinatorInput).list, {
								arg selected, asso;
								var combi;
								var inputidx; 
								//"1".debug;
								combi = ParamCombinator(param.parent,6);
								//"2".debug;
								//WindowDef(( \ParamCombinator_++param.fullLabel ).asSymbol, WindowDef(\ParamCombinatorEditor)).front(combi);
								//param.set(param.default)
								//[param, param.wrapper, combi].debug("param");
								param.set(combi);
								//"3".debug;
								inputidx = self.combinatorNextInputIndex(combi);
								//"4".debug;
								if(inputidx.notNil) {
								//"5".debug;
									combi.inBusMode = true;
								//"6".debug;
									combi.inputParam.at(inputidx).set(asso.value.asMap);
								//"7".debug;
									//self.addModulatorLine(asso.key); // done by keyForm
									self.proxy.changed(\keyForm, param);
								//"8".debug;
									self.updateModulatorView;
								//"9".debug;
								} {
									Log(\Param).error("No combinator slot available");
								}
							});
						}),
						BasicButton.new.string_("Combinator").action_({
							var combi;
							combi = ParamCombinator(param.parent).setBusMode(true);
							param.set(combi);
							WindowDef(( \ParamCombinator_++param.fullLabel ).asSymbol, WindowDef(\ParamCombinatorEditor)).front(combi);
							//param.set(param.default)
							self.proxy.changed(\keyForm, param);
						}),
					),
					HLayout (
						BasicButton.new.string_("Default").action_({
							param.set(param.default)
						}),
						BasicButton.new.string_("Unset").action_({
							param.unset;
						}),
						BasicButton.new.string_("Remove").action_({
							param.unset;
							self.updateParamGroupView;
							def.closeWindow;
							//self.proxy.changed(\keyForm, param);
						}),
						BasicButton.new.string_("Refresh").action_({
							self.proxy.changed(\keyForm, param);
							def.closeWindow;
						}),
					),
					//BasicButton.new.string_("Make PstepSeq").action_({

						//param.set( self.makePstepSeq );
						//self.proxy.changed(\keyForm, param);
					//}),
					if(param.type == \scalar) {
						HLayout (
							BasicButton.new.string_("MIDI CC learn").action_({
								var midi = param.target.getHalo(\MIDIFunc, param.property);
								if(midi.notNil) {
									midi.free;
									param.target.addHalo(\MIDIFunc, param.property, nil);
								};
								midi = MIDIFunc.cc({ arg ... args;
									//args.debug("MENU MIDI Func");
									param.normSet(args[0]/127)
								}).fix.learn;
								param.target.addHalo(\MIDIFunc, param.property, midi);
							}),
							midiClearButton = BasicButton.new.string_(
								if(param.target.getHalo(\MIDIFunc, param.property).notNil) {
									"Clear MIDI (%)".format(param.target.getHalo(\MIDIFunc, param.property).msgNum);
								} {
									"Clear MIDI (not set)";
								}
							).action_({
								var midi = param.target.getHalo(\MIDIFunc, param.property);
								if(midi.notNil) {
									midi.free;
									param.target.addHalo(\MIDIFunc, param.property, nil);
								};
							}),
						);
					},
					if(param.type == \array or: { param.get.isSequenceableCollection }) {
						var model;
						self.arrayControllerDict = self.arrayControllerDict ?? { IdentityDictionary.new };
						model = self.arrayControllerDict[param.propertyRoot] ?? { ProtoClass((
							arrayController_: { arg me, con;
								me[\arrayController] = con;
								self.setArrayController(param, con)
							},
						)) };
						self.arrayControllerDict[param.propertyRoot] = model;
						HLayout(
							StaticText.new.string_("controller:"),
							Param(
								Message(model),
								\arrayController, 
								ControllerDef.getTagSpec(\knobline)
							).asPopUpMenu.allowsReselection_(true),
						);
					},
					StaticText.new.string_(""),
					BasicButton.new.string_("> Spec editor").maxWidth_(100).background_(Color.clear).action_({
						specEditorView.layout = HLayout(WindowDef(\ParamSpecEditor).embedView(def, param)).margins_(0).spacing_(5);
						//specEditorView;
						//WindowDef(\ParamSpecEditor).front(param)
					}),
					specEditorView,
				].select(_.notNil) ++ [nil]
			)
		});
		
	},

	showCombinatorOverlayMenu: { arg self, param, combi, idx, view, x, y;
		WindowDef(\OverlayMenu).front(view, x, y, { arg def;
			//var label = PlayerWrapper(combi.inpu)
			VLayout (
				* [
					combi.rangeParam.at(idx).asView,
					//combi.inputParam.at(idx).spec_(TagSpecDef(\ParamCombinatorInput_asMap)).asPopUpMenu,
					HLayout (
						Param(Message(combi), \inputObjects -> idx, TagSpecDef(\Modulators)).asPopUpMenu.addAction({
							combi.inputParam.at(idx).setBus(PlayerWrapper(combi.inputObjects[idx]).outBus.asMap);
						}),
						combi.inputParam.at(idx).copy.spec_(ParamMappedBusSpec()).asPopUpMenu,
					),
					//combi.inputParam.at(1).spec_(TagSpecDef(\ParamCombinatorInput_asMap)).asPopUpMenu,
					//combi.inputParam.at(2).spec_(TagSpecDef(\ParamCombinatorInput_asMap)).asPopUpMenu,
					BasicButton.new.string_("Remove").action_({
						combi.inputParam.at(idx).set(0);
						combi.inputObjects[idx] = nil;
						//param.set(combi.baseParam.get);
						self.proxy.changed(\keyForm, param);
						def.closeWindow;
					}).onClose_({
						self.proxy.changed(\keyForm, param);
						self.updateModulatorView;
					}),
				].select(_.notNil) ++ [nil]
			)
		});
		
	},


	showGeneralOverlayMenu: { arg self, view;
		var addParamAction = { arg idx;
			if(idx == ( self.optionalEventKeys.size - 1 )) {
				// last value is 'Add custom param'
				WindowDef(\ListSaveDialog).front(self.optionalEventKeys.drop(-1), { arg selected;
					self.addParam(selected);
					self.updateParamGroupView;
				})
			} {
				self.addParam(self.optionalEventKeys[idx]);
				self.updateParamGroupView;
			};
		};
		Menu(
			Menu(*
				self.optionalEventKeys.collect { arg item, idx;
					MenuAction(item, {
						addParamAction.(idx)
					})
				};
			).title_("Add param"),
			Param(Message(self), \showSynthDefParams, ParamBoolSpec()).asMenuAction("Show SynthDef params"),
			Param(Message(self), \isNoteTimelineModeEnabled, ParamBoolSpec()).asMenuAction("NoteTimeline mode"),
			MenuAction.separator,
			MenuAction("Piano keyboard", { 
				//model.debug("m");
				//model.target.debug("t");
				WindowDef(\PdefPostPianoKeyboard).front(
					//Pbind(\type, \note, \NoteTimeline, 1, \dur, 1) 
					self.strippedPattern
				)
			}),
			Param(
				Message(self),
				\pianoController, 
				ControllerDef.getTagSpec(\piano)
			).asMenu("Piano controller"),
			Param(
				Message(self),
				\sliderController, 
				ControllerDef.getTagSpec(\knobline)
			).asMenu("Slider controller"),

			MenuAction.separator,
			MenuAction("Load from Pdef", {
				WindowDef(\GlobalLibrary_select).front(nil, { arg libid;
					var pdef = libid.last.value;
					self.proxy.source = pdef.source.copy;
					self.proxy.envir = pdef.envir.copy;
					self.proxy.convertToPbindef;
					self.updateParamGroupView;
					self.updateModulatorView;
					self.updateFxView;
				}, \Pdef)
			}),

			MenuAction.separator,
			MenuAction("Copy compile string", { 
				self.presetCompileString.pbcopy.postln;
			}),
			MenuAction("Refresh", {
				self.modulators = List.new;
				self.updateParamGroupView;
				self.updateModulatorView;
				self.updateFxView;
			}),
			MenuAction("Clear", {
				WindowDef(\ConfirmDialog).front("Are you sure ? This will erase the Pbindef.", {
					self.proxy.clear;
					self.proxy.source = PbindProxy.new;
					self.updateParamGroupView;
					self.updateModulatorView;
					self.updateFxView;
				})
			}),
		).front;

	},

	showGeneralOverlayMenu_old: { arg self, view, x, y;
		WindowDef(\OverlayMenu).front(view, x, y, { arg def;
			var addParamPopup = PopUpMenu.new;
			def.windowDo { arg win;
				win.setInnerExtent(300,100);
			};
			VLayout (
				//Param(Message(self), \legatoSustainMode, TagSpec([\legato, \sustain])).asPopUpMenu,
				//Param(Message(self), \noteMode, TagSpec([\freq, \midinote, \note, \degree])).asPopUpMenu,

				HLayout (
					addParamPopup.items_(self.optionalEventKeys),
					BasicButton.new.string_("Add param").action_({
						if(addParamPopup.value == ( self.optionalEventKeys.size - 1 )) {
							// last value is 'Add custom param'
							WindowDef(\ListSaveDialog).front(self.optionalEventKeys.drop(-1), { arg selected;
								self.addParam(selected);
								self.updateParamGroupView;
							})
						} {
							self.addParam(self.optionalEventKeys[addParamPopup.value]);
							self.updateParamGroupView;
						};
					}),
				),
				Param(Message(self), \showSynthDefParams, ParamBoolSpec()).asButton("Show SynthDef params"),
				Param(Message(self), \isNoteTimelineModeEnabled, ParamBoolSpec()).asButton.string_("NoteTimeline mode"),
				BasicButton.new.string_("Piano keyboard").action_({ 
					//model.debug("m");
					//model.target.debug("t");
					WindowDef(\PdefPostPianoKeyboard).front(
						//Pbind(\type, \note, \NoteTimeline, 1, \dur, 1) 
						self.strippedPattern
					)
				}),
				HLayout(
					StaticText.new.string_("piano controller:"),
					Param(
						Message(self),
						\pianoController, 
						ControllerDef.getTagSpec(\piano)
					).asPopUpMenu.allowsReselection_(true),
				),
				HLayout(
					StaticText.new.string_("slider controller:"),
					Param(
						Message(self),
						\sliderController, 
						ControllerDef.getTagSpec(\knobline)
					).asPopUpMenu.allowsReselection_(true),
				),
				BasicButton.new.string_("Copy compile string").action_({ 
					self.presetCompileString.pbcopy.postln;
				}),
				BasicButton.new.string_("Refresh").action_({
					self.updateParamGroupView;
					self.updateModulatorView;
					self.updateFxView;
				}),
				BasicButton.new.string_("Clear").action_({
					WindowDef(\ConfirmDialog).front("Are you sure ? This will erase the Pbindef.", {
						self.proxy.clear;
						self.proxy.source = PbindProxy.new;
						self.updateParamGroupView;
						self.updateModulatorView;
						self.updateFxView;
					})
				}),
			)
		});
		
	},

	showMixerOverlayMenu: { arg self, view, x, y;
		Menu(
				Param(Message(self), \isMixerEnabled, ParamBoolSpec()).asMenuAction("Enable mixer"),
				MenuAction("Show mixer", {
					if(self.isMixerEnabled) {
						self.mixer.edit;
					}
				}).enabled_(self.isMixerEnabled),
				MenuAction("Add FX", {
					self.addFx;
				}).enabled_(self.isMixerEnabled),
		).front;
	},

	hiddenParamList: { arg self;
		if(self.isNoteTimelineModeEnabled) {
			self.noteKeys ++ self.sustainKeys ++ (self.customHiddenParamList ?? { [] })
		} {
			self.customHiddenParamList ?? { [] }
		}
	},

	makeParamView: { arg self, param;
		var labelView, editorView, textView;
		var minWidth_right = 60;
		textView = TextField.new.mapParam(param, 5).maxWidth_(minWidth_right);
		labelView = StaticText.new.string_(param.propertyRoot).minWidth_(70);
		labelView.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(buttonNumber == 1) {
				self.showParamOverlayMenu(param, editorView, textView, view, x, y);
			};
			true;
		});
		//Log(\Param).debug("makeParamView: %", param);
		editorView = View.new;
		editorView.followChange(self.proxy, \keyForm, { arg view, model, msg, cparam ...args;
			var val;
			var bigheight = 80;
			var innerView;
			//if(true) {
			//[view, model, msg, args].debug("args");
			[param, cparam].debug("followChange test");
			if(param === cparam) {
				//Log(\Param).debug("editorView: followChange: %, %", param.property, param.spec);
				editorView.removeAll;
				editorView.maxHeight_(bigheight);
				val = param.getRaw;
				//val.debug("val");
				case(
					//{ val.isNil }, {
						//// param removed, do nothing
					//},
					{ param.propertyRoot == \dur and: {self.isNoteTimelineModeEnabled} }, {
						//val.debug("makeParamView: dur NoteTimeline");
						editorView.layout = HLayout([ innerView = self.noteTimeline.asPreview, stretch:10 ]).margins_(0).spacing_(0);
						textView.visible = false;
					},
					//{ [\freq, \midinote, \note, \degree].includes(param.propertyRoot) and: {self.isNoteTimelineModeEnabled} }, {
					//editorView.layout = HLayout([ self.noteTimeline.asPreview, stretch:10 ]).margins_(0).spacing_(0);
					//textView.visible = false;
					//},
					//{ val.isKindOf(PstepSeq) }, {
					//editorView.layout = HLayout(param.parent.at(\stepseq).asMultiSlider).margins_(0).spacing_(0);
					//textView.visible = false;
					//},
					{ param.spec.isKindOf(ParamBufferSpec) }, {
						var load = BasicButton.new.string_("Load").fixedWidth_(minWidth_right).action_({
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
						});
						//val.debug("makeParamView: buffer");
						~param = param;
						if(param.spec.isKindOf(ParamAudioBufferSpec)) {
							if(param.spec.startParamName.notNil) {
								editorView.layout = VLayout(
									self.paramAsSoundFileView(param),
									HLayout (
										[innerView = param.asPopUpMenu, stretch: 10],
										load,
									)
								).margins_(0);
							} {
								editorView.layout = HLayout(
									//[innerView = WindowDef(\ParamAsSoundFileView).asView(param), stretch:10],
									[innerView = param.asPopUpMenu, stretch: 10],
									load,
								).margins_(0);
							}
						} {
							editorView.layout = HLayout(
								//[innerView = WindowDef(\ParamAsSoundFileView).asView(param), stretch:10],
								[innerView = param.asPopUpMenu, stretch: 10],
								load,
							).margins_(0);
						};
						//editorView.layout = HLayout(
							//[innerView = BasicButton.new.string_("kjkjkj"), stretch:10],
						//).margins_(0);
						innerView.addUniqueMethod(\unmapParam, {}); // no param to unmap
						textView.visible = false;
					},
					{ param.spec.isKindOf(TagSpec) }, {
						//val.debug("makeParamView: bus");
						editorView.layout = HLayout(
							[param.asPopUpMenu, stretch:10],
						).margins_(0);
						textView.visible = false;
					},
					{ param.spec.isKindOf(ParamBusSpec) }, {
						//val.debug("makeParamView: bus");
						if(param.propertyRoot == \out) {
							editorView.layout = HLayout(
								if(self.isMixerEnabled) {
									var slider;
									var stack;
									var meterout = CompactServerMeterView(nil, 0,2, self.mixer.recbus);
									meterout.ticksView.minWidth_(6);
									meterout.orientation_(\horizontal);
									slider = Slider.new.orientation_(\horizontal).background_(Color.clear);
									slider.mapParam(self.mixer.getParam(\postfader));
									stack = StackLayout(
										slider, 
										meterout.view,
									).mode_(\stackAll);
									[stack, stretch:10];
								} {
									[param.asPopUpMenu, stretch:10];
								},
								BasicButton.new.string_("...").maxWidth_(60).mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
									//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
									self.showMixerOverlayMenu(view, x, y);

								})
							).margins_(0);
						} {
							innerView = param.asPopUpMenu;
							editorView.layout = HLayout([ innerView, stretch:10 ]).margins_(0).spacing_(0);
						};
						textView.visible = false;
					},
					{ val.value.isKindOf(ProtoClass) and: { val.value.eventType == \inlineEnv } }, {
						//[val, val.value].debug("makeParamView: inlineEnv");
						innerView = val.value.asView;
						//Log(\Param).debug("innerView %", innerView);
						editorView.layout = HLayout([ innerView, stretch:10 ]).margins_(0).spacing_(0);
						//Log(\Param).debug("xxx");
						innerView.addUniqueMethod(\unmapParam, {}); // no param to unmap
						textView.visible = false;
					},
					{ val.value.isKindOf(ProtoClass) and: { val.value.eventType == \paramTimeline } }, {
						//[val, val.value].debug("makeParamView: ParamTimeline");
						editorView.layout = HLayout([ innerView = val.value.asPreview, stretch:10 ]).margins_(0).spacing_(0);
						innerView.addUniqueMethod(\unmapParam, {}); // no param to unmap
						textView.visible = false;
					},
					{ param.hasCombinator }, {
						var mod;
						var knobs;
						var layoutList = List.new;
						var xtextView;
						var combi = param.getCombinator;
						//[combi, val].debug("makeParamView: ParamCombinator");
						self.modulators = self.modulators ?? { List.new };
						//[self.combinatorInputCount(combi)].debug("makeParamView: ParamCombinator: count");
						knobs = combi.size.collect { arg idx;
							var combiObject;
							if(combi.inputParam.at(idx).get != 0) {

								combiObject = combi.inputObjects[idx];

								//self.modulators.debug("WTF modulators");
								if(combiObject.isKindOf(ProtoClass) and: { combiObject.eventType == \paramTimeline }) {
									layoutList.add(
										//innerView = View.new.layout_(VLayout(combiObject.asPreview)).minHeight_(100);
										combiObject.asPreview
									);
								} {

									// commented for debug
									//mod = Ndef( TagSpecDef(\ParamCombinatorInput_asMap).unmapKey(
									//val.inputParam.at(idx).get 
									//));
									//mod.debug("mod");
									if(self.modulators.includes(combiObject).not) {
										self.modulators.add(combiObject);
									};
								};

								//maxSize_(20)
								combi.rangeParam.at(idx).asKnob.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
									//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
									if(buttonNumber == 1) {
										self.showCombinatorOverlayMenu(param, combi, idx, view, x, y);
										false;
									}
								})
							}
						}.select(_.notNil);
						//xtextView = textView;
						//textView = nil;
						knobs.do { arg knob, idx;
							// if not set, slider is smaller for unknown reason
							// stretch doesnt do nothing
							knob.maxSize_(25)
						};
						layoutList.add(
							HLayout(
								innerView = combi.baseParam.asSlider.orientation_(\horizontal),
								*knobs
							).margins_(0)
						);
						editorView.layout = VLayout(*layoutList).margins_(0);
						//editorView.layout = VLayout(layoutList[0]).margins_(0).spacing_(0);
						textView.mapParam(combi.baseParam, 5);
						//textView.visible = false;

					}, 
					{ val.isKindOf(Pattern) }, {
						//val.debug("makeParamView: pattern");
						editorView.layout = HLayout(innerView = param.asTextField).margins_(0).spacing_(0);
						textView.visible = false;

					}, 
					{ param.type == \array }, {
						innerView = param.asMultiSlider.maxHeight_(bigheight).attachContextMenu;
						editorView.layout = HLayout(innerView).margins_(0).spacing_(0);
						textView.visible = false;
					},
					{ param.type == \env }, {
						editorView.layout = HLayout(innerView = param.asEnvelopeView.maxHeight_(bigheight)).margins_(0).spacing_(0);
						textView.visible = false;
					},
					{ val.isKindOf(SequenceableCollection) }, {
						// Pattern multichannel expansion
						//		Param is scalar but should be treated as array according to Pbindef value
						//		Note that PstepSeq also fall in this switch case
						// should be placed after param.type==\env else envs will be treated as chords
						innerView = param.asMultiSlider.attachContextMenu;
						//val.debug("makeParamView: chord");
						editorView.layout = HLayout(innerView).margins_(0).spacing_(0);
						textView.visible = false;

					}, 
					{ param.type == \scalar }, {
						//val.debug("makeParamView: scalar");
						editorView.layout = HLayout(innerView = param.asSlider.orientation_(\horizontal)).margins_(0).spacing_(0);
						//textView.visible = true;

					}, 
					{
						//val.debug("makeParamView: default");
						editorView.layout = HLayout(innerView = param.asSlider.orientation_(\horizontal)).margins_(0).spacing_(0);
						//textView.visible = true;

					}
				);
				if(editorView.layout.notNil) {
					editorView.layout.addUniqueMethod(\innerView, { innerView });
				};
				//Log(\Param).debug("editorView: END followChange: %, %", param.property, param.spec);
			}
		}, false);
		self.proxy.changed(\keyForm, param);
		if(self.hiddenParamList.includes(param.propertyRoot)) {
			//"lay1".debug;
			HLayout()
		} {
			//"lay2".debug;
			if(textView.notNil) {
				HLayout (
					labelView,
					editorView,
					textView,
				)	
			} {

				HLayout (
					labelView,
					editorView,
				)	
			};
		}
	},

	paramAsSoundFileView: { arg self, param;
		var spec = param.spec;
		var target = param.target;
		//var buf = param.get;
		WindowDef(\soundfileview).asView(
			//BufDef(spec.tagSpec.unmapKey(param.get)),
			param,
			spec.startParamName !? { Param(target, spec.startParamName -> \source) },
			spec.sustainParamName !? { Param(target, spec.sustainParamName -> \source) },
			spec.endParamName !? { Param(target, spec.endParamName -> \source) },
			spec.speedParamName !? { Param(target, spec.speedParamName -> \source) },
			spec.startType ? \unipolar,
			spec.sustainType ? \beats,
			spec.endType ? \unipolar,
		);
	},

	makeFxView: { arg self;
		self.fxView = View.new;
		self.updateFxView;
		self.fxView;
	},

	updateFxView: { arg self;
		self.fxView.removeAll;
		if(self.isMixerEnabled) {
			self.fxView.layout = WindowDef(\TrackMixerDef_fxlist).asView(self.mixer, self.mixer.fxCount ? 0).margins_(0)
		};
	},


	makeModulatorView: { arg self;
		self.modulatorView = View.new;
		self.modulatorView.background = Color.grey(0.7);
		self.updateModulatorView;
		self.modulatorView;
	},

	updateModulatorView: { arg self;
		if(self.modulatorView.notNil) {
			self.modulatorView.removeAll;
			self.modulatorView.layout = VLayout(
				* self.modulators.reject({ arg mod; mod.isKindOf(Bus) }).collect { arg mod, idx;
					var pg;
					var fidx;
					var mparam;
					pg = mod.asParamGroup;
					fidx = pg.detectIndex( { arg x; x.propertyRoot == \freq } );
					if(fidx.notNil) {
						mparam = pg[fidx];
					} {
						pg = pg.rejectPbindParams.rejectByKey([\out, \gain, \amp]);
						mparam = pg.first;
					};

					HLayout (
						PlayerWrapperView(mod).view.fixedWidth_(120),
						BasicButton.new.string_("Edit").action_({
							PlayerWrapper(mod).edit;
							//WindowDef(\NdefEditor).front(mod);
						}).maxWidth_(30),
						//mod.asParamGroup.first.asSlider.orientation_(\horizontal),
						mparam.asView,
					)
				}
			).margins_(5)
		}
	},

	makeParamGroupView: { arg self, pgroup;
		var lay;
		//pgroup.do { arg param, idx;
			//param.debug("makeParamGroupView: pgroup item: %");
		//};
		lay	= VLayout(
			* pgroup.collect { arg param, idx;
				var res;
				//Log(\Param).debug("makeParamGroupView % %", param, idx);
				res = self.makeParamView(param);
				//Log(\Param).debug("makeParamGroupView END % %", param, idx);
				res;
			} ++ [nil];
		);
		//Log(\Param).debug("makeParamGroupView2");
		self.updateModulatorView;
		//Log(\Param).debug("makeParamGroupView3");
		lay;
	},

	updateParamGroupView: { arg self, view, proxy;
		view = view ? self.paramlistView;
		proxy = proxy ? self.proxy;
		view.removeAll;
		view.layout = self.makeParamGroupView(self.paramList);
		~plist = self.paramList;
	},

	makeInstrView: { arg self, proxy;
		var instrparam;
		var paramlistView = View.new;
		instrparam = Param( proxy, \instrument -> \source, TagSpecDef(\SynthDef_keys));
		instrparam.onChange({ arg view, param;
			self.updateParamGroupView(view, proxy);
		}, paramlistView);
		self.instrumentParam = instrparam;
		self.paramlistView = paramlistView;
		self.updateParamGroupView(paramlistView, proxy);
		//paramlistView.layout = self.makeParamGroupView(proxy.asParamGroup);
		VLayout (
			HLayout (
				PlayerWrapper(proxy),
				instrparam.asValuePopUpMenu.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
					//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");

					if(buttonNumber == 1) {
						WindowDef(\GlobalLibrary_select).front(nil, { arg synth;
							//synth.debug("synth");
							instrparam.set(synth.last.key);

						}, [\SynthDef])
					}
				}),
				BasicButton.new.string_("...").maxWidth_(30).mouseUpAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
					//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
					
					self.showGeneralOverlayMenu(view, x, y);
				}),
			),
			ScrollView.new.canvas_(
				paramlistView
			).hasBorder_(false),
		)
	},

	/// data

	makePstepSeq: { arg self, default=0;
		PstepSeq(default!8,inf);
	},

	defaultMixer: { arg self;
		TrackMixerDef("Pbindef_%".format(self.proxy.key).asSymbol)
	},

	mixer: { arg self;
		var val = Param(self.proxy, \out -> \source).get;
		if(val.isKindOf(ProtoClass) and: { val.inbus.notNil }) {
			val;
		} {
			self.defaultMixer;
		};
	},

	noteTimeline: { arg self;
		NoteTimeline("Pbindef_%".format(self.proxy.key).asSymbol)
	},

	strippedPattern: { arg self;
		// remove NoteTimeline keys (\midinote, \type, \play, etc) to be able to use the pattern in a keyboard

		//var exkeys = [\midinote, \play, \dur, \NoteTimeline];

		self.noteTimelineProxy = self.noteTimelineProxy ?? {
			EventPatternProxy.new.source_( 
				self.proxy <>
				Pbind(\disableNoteTimeline, true)
			);
		};
		self.noteTimelineProxy;


		//EventPatternProxy.new.source_( // because Param can't take Prout as target

			//Prout({ arg ev;
				//ev[\midinote] = ev[\save_midinote];
				//ev[\type] = \note;
				//ev[\NoteTimeline] = nil;
				//ev[\play] = nil;
				//ev.yield;
			//}).loop <>
			//self.proxy
			//<>
			//Prout({ arg ev;
				//ev[\save_midinote] = ev[\midinote];
				//ev.yield;
			//}).loop;
		//)
		
	},

	paramList: { arg self;
		var plist = List.new;
		var cust = self.noteParamList ++ self.customParamList;
		var pbkeys;
		if(self.showSynthDefParams == true) {
			plist = plist.addAll(self.proxy.asParamGroup(nil, false, [\freq, \gate, \doneAction, \instrument]).as(Array));
		};
		if(self.showOnlySetParams == true) {
			plist = plist.select({ arg p; p.isSet });
		};
		//[ cust ++ plist ].debug("pbjkeys");
		pbkeys = self.pbindefKeysParamList(cust ++ plist);
		plist = plist.addAll(pbkeys);
		plist = plist.addAll(cust);
		plist = plist.collect({ arg p; 
			if([\source, \stepseq].includes(p.propertyArray.last).not) {
				if(p.at(\source).get.isKindOf(PstepSeq)) {
					p.at(\stepseq)
				} {
					p.at(\source);
				}
			} {
				p
			};
		});
		//plist.do(_.debug("PbindefEditor: plist")); // debug
		plist;
	},

	pbindefKeysParamList: { arg self, exclude, excludeBaseKeys=true;
		var exkeys;
		if(excludeBaseKeys==true) {
			exkeys = Set.newFrom(exclude.collect(_.propertyRoot)) 
			++ [\instrument, \NoteTimeline]
			++ self.noteKeys
			++ self.sustainKeys;
		} {
			exkeys = [];
		};
		self.proxy.source.pairs.clump(2).flop.first.reject({ arg k; exkeys.includes(k) }).collect { arg key, idx;
			Param(self.proxy, key)
		};
	},

	noteParamList: { arg self;
		[
			Param(self.proxy, self.noteMode),
			Param(self.proxy, self.legatoSustainMode),
		]
	},

	addFx: { arg self;
		if(self.isMixerEnabled) {
			self.mixer.fxCount = self.mixer.fxCount ? 0;
			self.mixer.insertFxAt(self.mixer.fxCount);
			self.mixer.fxCount = self.mixer.fxCount + 1;
		};
		self.updateFxView;
	},

	addModulatorLine: { arg self, modkey;
		self.modulators = self.modulators ?? { List.new };
		self.modulators.add( Ndef(modkey)  );
		self.updateModulatorView;
	},

	noteKeys: [\freq, \midinote, \note, \degree],
	sustainKeys: [\legato, \sustain],
	optionalEventKeys: [
		\octave, 
		\root,
		\scale,
		\strum,
		\detune,
		\stretch, 
		\lag, 
		\mtranspose, 
		\ctranspose, 
		'* Add custom param...'
	],
	paramForms: [\scalar, \stepseq, \chord, \text, \timeline, \modTimeline, \inlineEnv],

	getParamForm: { arg self, param;
		//Log(\Param).debug("getParamForm %", param);
		case(
			{ param.parent.at(\source).get.isKindOf(PstepSeq) }, {
				\stepseq
			},
			{ 
				var val = param.parent.at(\source).getRaw; 
				val.isKindOf(ParamCombinator) 
				and: { val.inputObjects[0].notNil }
				and: { val.inputObjects[0].isKindOf(ProtoClass) }
				and: { val.inputObjects[0].eventType == \paramTimeline } 
			}, {
				\modTimeline
			},
			{ var val = param.parent.at(\source).getRaw; val.isKindOf(ProtoClass) and: { val.eventType == \paramTimeline } }, {
				\timeline
			},
			{ var val = param.parent.at(\source).getRaw; val.isKindOf(ProtoClass) and: { val.eventType == \inlineEnv } }, {
				\inlineEnv
			},
			{ param.parent.at(\source).get.isKindOf(Pattern) }, {
				\text
			},
			{ param.parent.at(\source).get.isKindOf(SequenceableCollection) }, {
				\chord
			}, {
				\scalar
			}
		)
	},

	setParamForm: { arg self, param, form, editorView;
		var default;
		switch(form,
			\scalar, { 
				editorView.removeAll;
				param.parent.at(\source).set( param.default );
				self.updateParamGroupView;
				   // can't use changed(\keyForm) because param need to change property by .paramList
			},
			\stepseq, { 
				editorView.removeAll;
				//editorView.layout.slider.unmapParam;
				default = param.default;
				if(default.isSequenceableCollection) {
					default = default.first;
				};
				param.set( self.makePstepSeq(default) );
				self.updateParamGroupView;
			},
			\chord, { 
				//editorView.layout.slider.unmapParam;
				editorView.removeAll;
				default = param.default;
				if(default.isSequenceableCollection) {
					default = default.first;
				};
				param.parent.at(\source).set( default!3 );
				self.updateParamGroupView; 
			},
			\text, { 
				//editorView.layout.slider.unmapParam;
				editorView.removeAll;
				//default = param.default;
				//if(default.isSequenceableCollection) {
					//default = default.first;
				//};
				param.parent.at(\source).set( "Pn( % )".format(self.proxy.source.at(param.propertyRoot).source.asCompileString).interpret );
				self.updateParamGroupView; 
			},
			\timeline, {
				var pt, busparam;
				editorView.removeAll;
				busparam = Param(BusDef("Pbindef_%_%".format(self.proxy.key, param.propertyRoot).asSymbol, \control), \value, param.spec);
				//busparam.set(param.get); // to test
				pt = ParamTimeline("Pbindef_%_%".format(self.proxy.key, param.propertyRoot).asSymbol, busparam);
				pt.inlineModeEnabled = true;
				param.parent.at(\source).setRaw( pt );
				self.updateParamGroupView; 
				
			},
			\modTimeline, {
				var pt, busparam;
				var modkey;
				var combi;
				combi = ParamCombinator(param.parent);
				combi.setBusMode(true);
				modkey = "Pbindef_mod_%_%".format(self.proxy.key, param.propertyRoot).asSymbol;
				editorView.removeAll;
				busparam = Param(BusDef(modkey, \control, 1), \value, \bipolar);
				//busparam.set(param.get); // to test
				pt = ParamTimeline(modkey, busparam);
				combi.inputObjects[0] = pt;
				combi.inputParam.at(0).set(pt.outBus.asMap);
				pt.inlineModeEnabled = true;
				param.parent.at(\source).setRaw( combi );
				self.updateParamGroupView; 
				
			},
			\inlineEnv, {
				var pt, busparam;
				editorView.removeAll;
				//Log(\Param).debug("create inlineEnv %", param);
				pt = ProtoTemplateDef(\PdefEditor_InlineEnv).new("Pbindef_%_%".format(self.proxy.key, param.propertyRoot).asSymbol, param.spec);
				param.parent.at(\source).setRaw( pt );
				self.updateParamGroupView; 
				
			}
		);
		
	},

	combinatorNextInputIndex: { arg self, combi;
		combi.nextFreeInput
	},

	/// controllers

	pianoController_: { arg self, con;
		self[\pianoController] = con;
		con.startControl(
			self.strippedPattern;
		)
	},

	sliderController_: { arg self, con;
		self[\sliderController] = con;
		con.startControl(self.paramList.select({ arg p; p.type == \scalar }));
	},

	setArrayController: { arg self, param, con;
		con.startControl(param)
	},

	/// modes

	showSynthDefParams_: { arg self, val;
		//self[\showSynthDefParams] = val;
		//~pbindefEditor_showSynthDefParams = val;
		Pbindef.addHalo(\PbindefEditor_showSynthDefParams, val);
		self.updateParamGroupView;
	},
	showSynthDefParams: {
		//~pbindefEditor_showSynthDefParams ? true
		Pbindef.getHalo(\PbindefEditor_showSynthDefParams) ? true;
	},

	isNoteTimelineModeEnabled_: { arg self, val;
		if(val == true) {
			self.removeParam(\dur);
			self.noteTimeline.param = Param(self.strippedPattern, \midinote);
			self.proxy.source.set(\NoteTimeline, { self.noteTimeline });
			// TODO: move Prout to NoteTimeline().asDurPattern
			self.proxy.source.set(\dur, Prout({ arg ev;
				//ev.debug("PbindefEditor NoteTimeline dur Prout");
				if(ev[\disableNoteTimeline] == true) {
					loop {
						ev = ev[\dur].yield;
					}
				} {
					var pat;
					//ev.debug("PbindefEditor NoteTimeline dur now in pattern mode");
					pat = ev[\NoteTimeline].value.asPattern;
					pat.do { arg val;
						ev.putAll(val);
						//ev[\midinote] = val[\midinote];
						//ev[\type] = val[\type];
						//ev[\sustain] = val[\sustain];
						ev = val.dur.yield;
					};
				};
				ev;
			}));
		} {
			self.removeParam(\NoteTimeline);
			self.proxy.source.set(\dur, 1);
		};
		self.updateParamGroupView
	},

	isNoteTimelineModeEnabled: { arg self;
		self.proxy.source.at(\dur).source.isKindOf(Prout) and: {
			var val;
			val = self.proxy.source.at(\NoteTimeline).source.value;
			val.isKindOf(ProtoClass) and: { val.eventType == \noteTimeline }
		}
	},

	isMixerEnabled: { arg self;
		var val = Param(self.proxy, \out -> \source).get;
		val.isKindOf(ProtoClass) and: { val.inbus.notNil }
	},

	isMixerEnabled_: { arg self, val;
		if(val == true) {
			Param(self.proxy, \out -> \source).set(self.defaultMixer);
		} {
			Param(self.proxy, \out -> \source).set(0);
		};
		self.updateParamGroupView;
	},

	legatoSustainMode_: { arg self, mode;
		switch(mode,
			\legato, { 
				self.addParam(\legato, Param(self.proxy, \sustain -> \source).get / Param(self.proxy, \dur -> \source).get );
				self.removeParam(\sustain);
			   },
			\sustain, { 
				self.addParam(\sustain, Param(self.proxy, \legato -> \source).get * Param(self.proxy, \dur -> \source).get );
				//self.removeParam(\legato); // not required, \sustain take precedence
				self.updateParamGroupView
		   
			},
		);
		self.updateParamGroupView

	},

	legatoSustainMode: { arg self;
		if(self.proxy.source.pairs.clump(2).any { arg pair, idx;
			pair.first == \sustain
		}) {
			\sustain
		} {
			\legato
		};
	},

	noteMode: { arg self;
		var keys = self.proxy.source.pairs.clump(2).flop.first;
		case(
			{ keys.includes(\freq) }, {
				\freq
			},
			{ keys.includes(\midinote) }, {
				\midinote
			},
			{ keys.includes(\note) }, {
				\note
			},
			{ keys.includes(\degree) }, {
				\degree
			}, {
				\freq
			}
		);
	},

	noteMode_: { arg self, val;
		switch(val,
			\freq, { 
				self.addParam(\freq, 200);
			},
			\midinote, {  
				self.removeParam(\freq);
				self.addParam(\midinote, 60);
			},
			\note, {  
				self.removeParam(\freq);
				self.removeParam(\midinote);
				self.addParam(\note, 0);
			},
			\degree, {  
				self.removeParam(\freq);
				self.removeParam(\midinote);
				self.removeParam(\note);
				self.addParam(\degree, 0);
			},
		);
		self.updateParamGroupView;
	},

	removeParam: { arg self, key;
		if(self.proxy.source.at(key).notNil) { // if no key defined, Pbindef add it back!
			self.proxy.source.set(key, nil);
		}
	},

	addParam: { arg self, key, val;
		if(self.proxy.source.at(key).isNil) { // if already defined, do not change it
			self.proxy.source.set(key, val ?? { Param(self.proxy, key).default });
		}
	},

	presetCompileString: { arg self;
		var params;
		params = self.pbindefKeysParamList([], false).collect { arg param, idx;
			// needed because instrumentParam is already a source subparam
			// and double source in propertyPath return wrong value
			param = if(
				PdefParam.associationToArray(param.propertyPath).last != \source
			) {
				param.at(\source)
			} {
				param;
			};
			if(param.spec.isKindOf(ParamBufferSpec)) {
				"\t%, %,\n".format(
					param.propertyRoot.asCompileString,
					"BufDef(%)".format(param.spec.tagSpec.unmapKey(param.get).asCompileString)
				)
			} {
				"\t%, %,\n".format(
					param.propertyRoot.asCompileString,
					param.getRaw.asCompileString, // getRaw to keep double brackets for arrays
				)
			}
		}.join;
		"Pbindef(%,\n%);".format(self.proxy.key.asCompileString, params)
	},

	asView: { arg self, def, proxy;
		var modview = self.makeModulatorView;
		~self = self;
		self.proxy = proxy;
		self.customParamList = [
			Param(self.proxy, \dur),
		];
		self.customHiddenParamList = [ ];
		VLayout (
			[self.makeInstrView(proxy), stretch:10],
			self.modulatorView,
			self.makeFxView,
		)

	},
));


WindowDef(\PdefPostPianoKeyboard, { arg def, pdef;
	var kb = MIDIKeyboardView.new;
	kb.mapPattern(pdef, \post);
	def.windowDo { arg win;
		win.setInnerExtent(800,120)
	};
	kb.asView;
});

WindowDef(\PbindefEditorParams, (
	parent: WindowDef(\PbindefEditor).proto,

	makeInstrView: { arg self, proxy;
		var instrparam;
		var paramlistView = View.new;
		instrparam = Param( proxy, \instrument -> \source, TagSpecDef(\SynthDef_keys));
		instrparam.onChange({ arg view, param;
			self.updateParamGroupView(view, proxy);
		}, paramlistView);
		self.instrumentParam = instrparam;
		self.paramlistView = paramlistView;
		self.updateParamGroupView(paramlistView, proxy);
		//paramlistView.layout = self.makeParamGroupView(proxy.asParamGroup);
		paramlistView;
	},
	asView: { arg self, def, proxy;
		var view;
		self.proxy = proxy;
		self.customParamList = [
			Param(self.proxy, \dur),
		];
		view = ScrollView.new.canvas_(
			View.new.layout_(VLayout (
				[self.makeInstrView(proxy), stretch:1],
				[self.makeModulatorView, stretch:0],
				[self.makeFxView, stretch:10],
				nil,
			).margins_(0))
		).hasBorder_(false);

		view.addUniqueMethod(\showGeneralOverlayMenu, { arg mainview, button, x, y;
			self.showGeneralOverlayMenu(button, x, y);
		});
		view.addUniqueMethod(\proto, { 
			self
		});
		view;
	},
));

WindowDef(\PbindefEditorParamView, (
	parent: WindowDef(\PbindefEditor).proto,

	makeParamGroupView: { arg self;
		var param = self.argParam;
		param = if(param.at(\source).get.isKindOf(PstepSeq)) {
			param.at(\stepseq)
		} {
			param.at(\source);
		};
		self.makeParamView(param);
	},

	asView: { arg self, def, param;
		self.paramlistView = View.new;
		self.argParam = param;
		self.proxy = param.target;
		self.updateParamGroupView;
		self.paramlistView;
	},
));

);

(
SynthDef(\inlineEnv_adsr, { arg out=0, gate=1, levelScale=1, levelBias=0;
	var sig;
	sig = EnvGen.kr(\adsr.kr(Env.adsr(0.4,0.1,0.8,0.1)), gate, levelScale: levelScale, levelBias: levelBias, doneAction:2);
	Out.kr(out, sig);
}).add;
);

(
ProtoTemplateDef(\PdefEditor_InlineEnv, (
	eventType: \inlineEnv,
	new: { arg self, uniqkey, spec;
		self = ProtoClass((parent: self));
		self.spec = spec ?? { \unipolar.asSpec };
		self.busKey = uniqkey;
		self.baseEvent = (levelBias: self.spec.minval, levelScale: 1 );
		self.baseEvent.addSpec(\levelScale, ControlSpec(0.1,4,\exp));
		self.baseEvent.addSpec(\levelBias, self.spec);
	
		
	
		self;
	},

	synthName: { arg self;
		\inlineEnv_adsr;
	},

	getParam: { arg self, key;
		Param(self.baseEvent, key);
	},

	bus: { arg self;
		BusDef(self.busKey, \control);
	},

	asParamGroup: { arg self;
		SynthDesc(self.synthName).asParamGroup(self.baseEvent)
	},

	asPattern: { arg self;
		Prout({ arg ev;
			var envev;
			var spec = self.spec;

			loop {
				envev = self.baseEvent.copy;
				[\sustain, \legato, \dur].do { arg key, idx;
					envev[key] = ev[key];
				};
				envev[\instrument] = self.synthName;
				envev[\out] = self.bus;
				envev[\levelScale] = spec.range * ( self.baseEvent.levelScale ? 1 );
				envev[\levelBias] = spec.minval + ( self.baseEvent.levelBias ? 0 );
				//envev.debug("envev");
				ev[\finish] = ev[\finish].addFunc({
					envev.play;
				});
				ev = envev[\out].asMap.yield;
			};
			ev
		});
	},

	editor: { arg self;
		WindowDef(\PdefEditor_InlineEnv, { arg def, pgroup;
			var envparam;
			var lay;
			var fpgroup;
			var exkeys = [\out, \gate];
			//Log(\Param).debug("PdefEditor_InlineEnv in, pgroup: %", pgroup);
			// can't use block{} because interpreter bug
			[\adsr, \asr, \env].collect { arg ekey;
				pgroup.detect { arg param; param.propertyRoot == ekey  } !? { arg sel;
					envparam = sel
				}
			};
			//envparam.debug("envparam");
			if(envparam.notNil) {
				exkeys = exkeys ++ [ envparam.propertyRoot ];
			};
			fpgroup = pgroup.reject({ arg x; exkeys.includes(x.propertyRoot) });
			//fpgroup.debug("fpgroup");
			//fpgroup.debug("fpgroup2");
			lay = VLayout (
				//VLayout(BasicButton.new.string_("Tu me soule"))
				if(envparam.notNil) {
					envparam.asEnvelopeView;
				},
				HLayout (
					* fpgroup.collect { arg param, idx;
						HLayout (
							param.asStaticTextLabel.align_(\right).maxWidth_(70),
							param.asKnob.maxSize_(Point(20,20)),
						).margins_(0).spacing_(10)
					};
				).spacing_(0).margins_(0)
			);
			//lay = BasicButton.new.string_("soouuule");
			//View.new.layout_(lay)
			//View.new;
			lay;

		});
	},

	asView: { arg self;
		self.editor.asView(self.asParamGroup);
		//VLayout(BasicButton.new.string_("Tu me soule"))
	},

	edit: { arg self;
		self.editor.front(self.asParamGroup);
	},

	asStream: { arg self;
		self.asPattern.asStream;
	},

	streamArg: { arg self;
		self.asStream;
	},
));
);

