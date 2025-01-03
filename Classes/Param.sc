
Param {
	var <>wrapper; // should not it be read only ?
	var <>baseWrapper; // should not it be read only ?
	var init_args;
	classvar <>defaultSpec;
	classvar <>simpleControllers;
	classvar <>userSimpleControllers;
	classvar <>defaultUpdateMode = \dependants;
	classvar <>defaultPollRate = 0.2;
	classvar <>editFunction;
	classvar <>lastTweaked;
	classvar <>trace = false;
	classvar <>midiFuncList;

	*initClass {
		Class.initClassTree(List);
		Class.initClassTree(Spec);
		Class.initClassTree(ControlSpec);
		defaultSpec = \widefreq.asSpec;
		simpleControllers = List.new;
	}

	*new  { arg ...args;
		//"hehehe".debug;
		^super.new.init(args)
	}

	*newWrapper { arg wrap;
		^super.new.newWrapper([wrap])
	}

	*fromWrapper { arg wrap;
		^super.new.initFromWrapper(wrap)
	}

	initFromWrapper { arg wrap;
		wrapper = wrap;
		// WARNING: wrap.spec could be a computed spec and not the arg given
		// no spec given because initFromWrapper is used by at to get a slotparam, so no spec is given
		init_args = [wrap.target, wrap.property];
	}

	init { arg args;
		//"init xxxxxxxxxxx".debug;
		// FIXME: why this test on size ? if ommit parameters, this break! like Param(s.volume)
		this.newWrapper(args);
		//if (args.size > 1) {
			//this.newWrapper(args)
		//} {
			//// TODO: test without this code to see if something break
			//if(args[0].isSequenceableCollection) {
				//// This is for support Param([Ndef(\plop), \freq, \freq])
				//// this support is stupid, why not Param(*[Ndef(\plop), \freq, \freq]) ???
				//this.newWrapper(args[0])
			//} {
				//// This is for support Param(ParamValue.new)
				//// This break API for why ? to not use an extra wrapper
				//// should be removed because there is enough wrapper to access data anywhere
				//this.newWrapper(args)
			//}
		//};
	}

	newWrapper { arg args;
		var target, property, spec, envdict;
		var class_dispatcher;
		var property_dispatcher;
		//"iiiwhattt".debug;
		target = args[0];
		property = args[1];
		spec = args[2];
		envdict;
		//"whattt".debug;
		//[target, property, spec].debug("newWrapper");

		envdict = ( // not used
			adsr: (
				attack: \times -> 0,
				decay: \times -> 1,
				sustain: \levels -> 2,
				peak: \levels -> 1,
				release: \times -> 2,
			),
			asr: (
				attack: \times -> 0,
				sustain: \levels -> 1,
				release: \times -> 1,
			),
		);

		property_dispatcher = { arg property, arrayclass, envclass;
			// handle Array and Env indexing
			switch(property.key.class,
				Association, { // index of ((\adsr -> \levels) -> 0)
					var subpro = property.key.value;
					var idx = property.value;
					//"Ndef: a double asso".debug;
					args[1] = property.key.key;
					//(args++[subpro, idx]).debug("NdefParamEnvSlot args");
					//Log(\Param).debug("property_dispatcher: env % %", envclass, args++[subpro, idx]);
					wrapper = envclass.new(*args++[subpro, idx]);
				},
				{
					//"Ndef: a simple asso".debug;
					switch(property.value.class, 
						Symbol, { // env named segment: (\adsr -> \sustain) 
							// need to get spec, but spec is determined after wrapper creation :(
							//Log(\Param).error("Ndef: env named segment");
							//Log(\Param).error("NOT IMPLEMENTED YET!");
							args[1] = property.key;
							//Log(\Param).debug("property_dispatcher: simple env % %", envclass, args++[property.value]);
							wrapper = envclass.new(*args++[property.value])
						},
						// else: an index into an array: (\delaytab -> 0)
						{ 
							var idx;
							//"Ndef: an index into an array".debug;
							args[1] = property.key;
							idx = property.value;
							//(args++[idx]).debug("NdefParamSlot args");
							//Log(\Param).debug("property_dispatcher: array % %", arrayclass, args++[idx]);
							wrapper = arrayclass.new(*args++[idx]);
						}

					)
				}
			);
		};

		//"hello3 gangenr".debug;

		class_dispatcher = (
			Node: {
				wrapper = NodeParam(*args);
			},
			Ndef: {
				switch(property.class,
					Association, {
						//"Ndef: an asso".debug;
						//property_dispatcher.(property, NdefParamSlot, NdefParamEnvSlot);
						wrapper = NdefParam(*args);
					},
					Symbol, { // a simple param : \freq
						//"Param.newWrapper: Ndef: a symbol".debug;
						wrapper = NdefParam(*args);
					},
					String, { // volume of the Ndef (Ndef(\x).vol)
						//"Param.newWrapper: Ndef: a string".debug;
						if(property == "vol" or: { property == "volume" }) {
							wrapper = NdefVolParam(*args);
						} {
							Log(\Param).critical("Error: don't know what to do with string property (%) of Ndef, did you mean to use a symbol ?"
								.format(property));
							^nil
						}
					}
				);
			},
			NodeProxy: {
				switch(property.class,
					Association, {
						//"Ndef: an asso".debug;
						//property_dispatcher.(property, NdefParamSlot, NdefParamEnvSlot);
						wrapper = NodeProxyParam(*args);
					},
					Symbol, { // a simple param : \freq
						//"Param.newWrapper: Ndef: a symbol".debug;
						wrapper = NodeProxyParam(*args);
					},
					String, { // volume of the Ndef (Ndef(\x).vol)
						//"Param.newWrapper: Ndef: a string".debug;
						if(property == "vol" or: { property == "volume" }) {
							//wrapper = NodeProxyVolParam(*args);
                            "notimplemented".throw;
						} {
							Log(\Param).critical("Error: don't know what to do with string property (%) of Ndef, did you mean to use a symbol ?"
								.format(property));
							^nil
						}
					}
				);
			},

			Pdef: {
				wrapper = PdefParam(*args);
				//switch(property.class,
					//Association, {
						////property_dispatcher.(property, PdefParamSlot, PdefParamEnvSlot);
						//property_dispatcher.(property, PdefParam, PdefParam);
					//},
					//Symbol, {
						//wrapper = PdefParam(*args);
					//}
				//);
			}, 
			EventPatternProxy: {
				wrapper = EventPatternProxyParam(*args);
			},

			Pdefn: {
				wrapper = PdefnParam(*args);
			}, 
			PatternProxy: {
				wrapper = PatternProxyParam(*args);
			}, 

			PbindSeqDef: {
				switch(property.class,
					Association, {
						property_dispatcher.(property, PdefParamSlot, PdefParamEnvSlot);
					},
					Symbol, {
						wrapper = PbindSeqDefParam(*args);
					}
				);
			},
			Volume: {
				//"wrapper: VolumeParam".debug;
				wrapper = VolumeParam(*args);
			},
			TempoClock: {
				//"wrapper: VolumeParam".debug;
				wrapper = TempoClockParam(*args);
			},
			PstepSeq: {
				wrapper = PstepSeq(*args);
			},
			List: {
				if(property.isKindOf(Integer)) {
					wrapper = ListParamSlot(*args);
				} {
					wrapper = ListParam(*args);
				}
			},
			Dictionary: {
				switch(property.class,
					Association, {
						property_dispatcher.(property, DictionaryParamSlot, DictionaryParamEnvSlot);
					},
					Symbol, {
						wrapper = DictionaryParam(*args);
					}
				);
			},
			StepEvent: { 
				wrapper = StepEventParam(*args);
			},
			Array: {
				//target.class.debug("mais what ??");
				//Log(\Param).error("ERROR: not implemented for Array, use List instead");
				//^nil;
				if(property.isKindOf(Integer)) {
					wrapper = ListParamSlot(*args);
				} {
					wrapper = ListParam(*args);
				}
			},
			Message: {
				wrapper = MessageParam(*args);
			},
			Function: {
				wrapper = FunctionParam(*args);
			},
			Builder: {
				//Builder.debug("newWrapper");
				wrapper = BuilderParam(*args);
			},
			Bus: {
				wrapper = BusParam(*args);
			}
		);
		// FIXME: should not have to add the whole class hierarchy
		class_dispatcher['Synth'] = class_dispatcher['Node'];
		class_dispatcher['Group'] = class_dispatcher['Node'];

		class_dispatcher['Ppredef'] = class_dispatcher['Pdef'];
		class_dispatcher['Pbindef'] = class_dispatcher['Pdef'];

		class_dispatcher['Pseq'] = class_dispatcher['PstepSeq'];
		class_dispatcher['Prand'] = class_dispatcher['PstepSeq'];

		class_dispatcher['StepList'] = class_dispatcher['List'];
		class_dispatcher['DictStepList'] = class_dispatcher['List'];
		class_dispatcher['ParDictStepList'] = class_dispatcher['List'];

		class_dispatcher['Event'] = class_dispatcher['Dictionary'];
		class_dispatcher['PresetEvent'] = class_dispatcher['Dictionary'];
		class_dispatcher['PlayerEvent'] = class_dispatcher['Dictionary'];
		class_dispatcher['PatternEvent'] = class_dispatcher['Dictionary'];
		//class_dispatcher['StepEvent'] = class_dispatcher['Dictionary'];
		class_dispatcher['StepEventDef'] = class_dispatcher['StepEvent'];
		class_dispatcher['IdentityDictionary'] = class_dispatcher['Dictionary'];
		class_dispatcher['Environment'] = class_dispatcher['Dictionary'];

		class_dispatcher['BusDef'] = class_dispatcher['Bus'];


		if(args.size == 2) {
			// no spec given, normalize args size to 3
			args.add(nil)
		};
		//"1".debug;
		init_args = args;
		//"2".debug;
		//target.class.debug("newWrapper: class");

		if(class_dispatcher[target.class.asSymbol].notNil) {
			class_dispatcher[target.class.asSymbol].value;
			//if(this.combinator.notNil and: { baseWrapper.isNil }) {
				//baseWrapper = wrapper;
				//wrapper = this.combinator.baseParam.wrapper;
			//}
		} {
			// ParamValue goes here
			// FIXME: this is error prone when target is not recognized
            this.dumpBackTrace;
			Log(\Param).debug("WARNING: target class not supported, using it as the wrapper");
			Log(\Param).debug("FIXME: should not create Param with a wrapper as arg %, %", target.class, target);
			wrapper = target;
		}
	}

	printOn { arg stream;
		this.storeOn(stream); // storeOn call storeArgs
	}

	storeArgs { arg stream;
		^init_args
		//stream << ("Param.new(" ++ init_args.asCompileString ++ ")");
	}

	== { arg param;
		if(param.isKindOf(Param)) {
			^( this.wrapper.property == param.wrapper.property)
			and: { this.wrapper.target == param.wrapper.target } 
		} {
			^false;
		};
	}

	asParam {
		^this
	}

	asLabel { arg labelmode;
		^this.label(labelmode)
	}

	label { arg labelmode;
		//^(label ?? { wrapper.label(labelmode) })
		^wrapper.label(labelmode);
	}

	fullLabel {
		^wrapper.fullLabel
	}

	type {
		^wrapper.type
	}

	default {
		^wrapper.default;
	}

	default_ { arg val;
		wrapper.default = val;
	}

	setBusMode { arg enable=true, free=true;
		wrapper.setBusMode(enable, free);
	}

	inBusMode {
		^wrapper.inBusMode
	}

	inBusMode_ { arg ...args;
		wrapper.inBusMode_(*args);
		this.changed(\inBusMode);
	}

	numChannels {
		^this.spec.numChannels;
	}

	key {
		^wrapper.key;
	}

	target {
		^wrapper.target;
	}

	property {
		^wrapper.property;
	}

	propertyRoot {
		^wrapper.propertyRoot;
	}

    propertyArray {
		^BaseAccessorParam.associationToArray(this.property)
    }

	spec {
		^wrapper.spec;
	}

	spec_ { arg val; // not sure if safe
		wrapper.spec = val;
	}

	///// combinator
    // need refactoring

	combinator {
		^wrapper.combinator;
	}

	combinator_ { arg val;
		wrapper.combinator = val;
	}

	combinatorEnabled_ { arg val;
   		wrapper.combinatorEnabled = val;
	}

	combinatorEnabled {
   		^wrapper.combinatorEnabled
	}

	asUncombinatedParam {
		//var wr = this.baseWrapper ?? { this.wrapper };
		^this.class.new(this.target, this.property).combinatorEnabled_(false);
	}

    // halo combinator getter

	hasCombinator {
		^wrapper.hasCombinator
	}

	getCombinator {
		^wrapper.getCombinator
	}

	clearCombinator {
		^wrapper.clearCombinator
	}

    //

	//inCombinatorMode_ { arg val;
		//if(val == true) {
			//if(this.combinator.notNil and: { baseWrapper.isNil }) {
				//baseWrapper = wrapper;
				//wrapper = this.combinator.baseParam.wrapper;
			//}
		//} {
			//if(baseWrapper.notNil) {
				//wrapper = baseWrapper;
				//wrapper
			//}
			//if(this.combinator.notNil and: { baseWrapper.isNil }) {
				//baseWrapper = wrapper;
				//wrapper = this.combinator.baseParam.wrapper;
			//}

		//}
	//}


	//////

	clone {
		^this.class.new(this.target, this.property);
	}

	///////// list behavior

	size { 
		^wrapper.size;
	}

	at { arg idx;
		// current implementation is: wrapper.at should return a Param, not a wrapper
		^wrapper.at(idx);
	}

	parent { 
		if(wrapper.parent.notNil) {
			^Param.fromWrapper(wrapper.parent);
		} {
			^nil
		};
	}

	asParamList {
		^wrapper.asParamList;
	}

	do { arg fun;
		wrapper.do(fun);
	}

	collect { arg fun;
		^wrapper.collect(fun);
	}

	/////////// sub param behavior
	
	subIndex {
		^wrapper.subIndex;
	}

	///////////

	get {
		^wrapper.get;
	}

	set { arg val;
		wrapper.set(val);
	}

	normGet {
		^wrapper.normGet;
	}

	normSet { arg val;
		wrapper.normSet(val);
	}

	getRaw {
		^wrapper.getRaw
	}

	setRaw { arg val;
		wrapper.setRaw(val)
	}

	getBus {
		^wrapper.getBus
	}

	setBus { arg val;
		wrapper.setBus(val)
	}


	/////////////////// MIDI mapping

	// old name was .map and .unmap

	midiMap { arg msgNum, chan, msgType=\control, srcID, blockmode;
		var mf;
		mf = MIDIFunc({ arg val, noteNum, channel, deviceId;
			//[ velocity, noteNum, channel, deviceId ].debug;
			this.normSet(val/127);
			
		}, msgNum, chan, msgType, srcID).fix;
		this.class.midiFuncList = this.class.midiFuncList.add(this -> mf);
		^mf;
		//MIDIMap(this, msgNum, chan, msgType, srcID, blockmode);
	}

	*midiUnmap { arg msgNum, chan, msgType, srcID;
		//MIDIMap.free(msgNum, chan, msgType, srcID, blockmode);
		var lists;
		lists = this.midiFuncList.inject([List(),List()], { arg lists, asso;
			var resp = asso.value;
			var list = if(
				(msgNum.isNil or: { resp.msgNum == msgNum })
				and: { chan.isNil or: { resp.chan == chan } }
				and: { msgType.isNil or: { resp.msgType == msgType } }
				and: { srcID.isNil or: { resp.srcID == srcID } }
			) {
				lists.first;
			} {
				lists.last;
			};
			list.add(asso);
			lists;
		});
		lists.first.collect(_.value.free);
		this.midiFuncList = lists.last;
	}

	midiUnmap { arg msgNum, chan, msgType, srcID;
		var lists;
		lists = this.class.midiFuncList.inject([List(),List()], { arg lists, asso;
			var resp = asso.value;
			var list = if(
				asso.key == this and: {
					(msgNum.isNil or: { resp.msgNum == msgNum })
					and: { chan.isNil or: { resp.chan == chan } }
					and: { msgType.isNil or: { resp.msgType == msgType } }
					and: { srcID.isNil or: { resp.srcID == srcID } }
				}
			) {
				lists.first;
			} {
				lists.last;
			};
			list.add(asso);
			lists;
		});
		lists.first.collect(_.value.free);
		this.class.midiFuncList = lists.last;
	}

	midiLearn { arg blockmode, msgType=\control;
		//MIDIMap.learn(this, blockmode);
		var mf;
		mf = MIDIFunc({ arg val, noteNum, channel, deviceId;
			//[ velocity, noteNum, channel, deviceId ].debug;
			this.normSet(val/127);
			
		}, msgType:msgType).learn.fix;
		this.class.midiFuncList = this.class.midiFuncList.add(this -> mf);
		^mf
	}
	
	*getMidiMappedParams { arg msgNum, chan, msgType, srcID;
		// what params are mapped to this midi knob ?
		^this.midiFuncList.select({ arg asso;
			var resp = asso.value;
			(msgNum.isNil or: { resp.msgNum == msgNum })
			and: { chan.isNil or: { resp.chan == chan } }
			and: { msgType.isNil or: { resp.msgType == msgType } }
			and: { srcID.isNil or: { resp.srcID == srcID } }
		})
	}

	midiMapList {
		// to what knobs this param is mapped ?
		^this.class.midiFuncList.select({ arg asso;
			asso.key == this
		})
	}


	/////////////////// GUI mapping

	makeSimpleController { arg slider, action, updateAction, initAction, customAction, cursorAction;
		// define view action to update model and make a controller to update the view
		var param = this;
		{
			slider.toolTip = this.fullLabel; // FIXME: not really a good place, but so it can quicly apply to every view
		}.defer;

		if(action.isNil) {
			action = { arg self;
				param.normSet(self.value);
			}
		};
		if(updateAction.isNil) {
			updateAction = { arg self;
				{
					self.value = param.normGet;
				}.defer;
			}
		};
		if(initAction.isNil) {
			initAction = updateAction;
		};

		if(param.notNil) {
			param = param.asParam;

			this.class.freeUpdater(slider);

			{
				// is it guaranted that the erasing of action in a defer in freeUpdater below execute before this defer ?
				slider.action = { arg self;
					if(Param.trace == true) {
						//"%: view.action: input: %, param value: %, param norm value: %".format(this, self.value, this.get, this.normGet).postln;
						"%: view.action: input: %".format(this, self.value).postln;
					};
					action.value(self, this);
					customAction.value(self, this);
					Param.lastTweaked = this; // FIXME: only work when there is a GUI, should add in MIDI too
					Param.changed(\lastTweaked);
				};
			}.defer;

			this.makeUpdater(slider, updateAction);
			if(cursorAction.notNil) {
				this.makeCursorUpdater(slider, cursorAction)
			};

			{
				// long defer is needed else very strange bug occurs 
				// ^^ The preceding error dump is for ERROR: Message 'new' not understood
				// RECEIVER: an ExponentialWarp
				initAction.(slider, param);
			}.defer(1/100); 
			nil;
		};
	}

	makeCursorUpdater { arg view, action;
		var con = view.getHalo(\simpleControllerCursor) ?? { view.getHalo(\simpleController) };
		con.put(\cursor, { arg obj, message, idx, idx2;
			action.(view, idx, idx2)
		});
	}

	refreshUpdater { arg view, action, updateMode;
		// FIXME: updateMode can't be retrieved by putListener
		this.class.freeUpdater(view);
		this.makeUpdater(view, action, updateMode);
	}

	makeUpdater { arg view, action, updateMode;
		var param = this;
		updateMode = updateMode ? defaultUpdateMode;


		if(updateMode == \dependants) {
			// dependants mode
			// FIXME: should i free it ? should i reuse it ?
			//		refreshUpdater free it before calling makeUpdater
			var controller, controllerCursor;
			//Log(\Param).debug("makeUpdater: controllerTarget:%", this.controllerTarget);
			controller = SimpleController(param.controllerTarget);
			{
				view.onClose = view.onClose.addFunc({
				   	this.class.freeUpdater(view);
					Halo.lib.removeAt(view);
			   	});
			}.defer;
			view.addHalo(\simpleController, controller);
			simpleControllers.add(controller);

			//Log(\Param).debug("makeUpdater controllerTargetCursor %", param.controllerTargetCursor);
			if(param.controllerTargetCursor.notNil) {
				controllerCursor = SimpleController(param.controllerTargetCursor);
				simpleControllers.add(controllerCursor);
				view.addHalo(\simpleControllerCursor, controllerCursor);
			};

			this.putListener(view, controller, action);
		} {
			// polling mode
			var skipjack;
			var pollRate = defaultPollRate + (defaultPollRate/1.7).rand;
			skipjack = SkipJack({
				//param.debug("skipjack:action");
				action.(view, param);
				//debug("skipjack:action: done");
			}, pollRate, { 
				// isClosed always exists ?
				if(view.isClosed) {
					// is it ok to remove thing on a closed view ? is even necessary if the view is already closed ?
	 				//this.class.freeUpdater(view);
					true;
				} {
					false;
				}
			});
			view.addHalo(\skipJack, skipjack);
		}
	}

	putListener { arg view, controller, action;
		//Log(\Param).debug("Param.putListener view:%, wrapper:%", view, wrapper );
		this.wrapper.putListener(this, view, controller, action);
	}

	makeListener { arg action, obj; 
		// helper method for external to listen to param value changes
		// WARNING: caller is responsible for freeing the controller !
		// obj is a view
		// action.(obj, param)
		var cont;
		cont = SimpleController.new(this.controllerTarget);
		this.class.userSimpleControllers = this.class.userSimpleControllers.add(cont);
		this.putListener(obj, cont, action);
		^cont
	}

	attachListener { arg view, action;
		// this should be the top level method, used to easily adapt a GUI to Param
		var con;
		this.removeListener(view);
		view.onClose = view.onClose.addFunc({ this.removeListener(view) });
		con = this.makeListener({ arg lview, pa;
			if(lview.isClosed) {
				this.removeListener(lview);
			} {
				action.(lview, pa);
			}
		}, view);
		view.addHalo(\paramListener, con);
	}

	removeListener { arg view;
		view.getHalo(\paramListener) !? { arg x; x.remove; };
		if(view.isClosed) {
			Halo.lib.removeAt(view);
		}
	}

	// FIXME: too many methods for the same thing, with fuzzy semantics
	onChange { arg action, view;
		// if view is defined, automatically free the controller when closed
		var cont;
		cont = SimpleController.new(this.controllerTarget);
		this.class.userSimpleControllers = this.class.userSimpleControllers.add(cont);
		this.putListener(view, cont, { arg view, param;
			if(view.isClosed) {
				cont.remove;
			} {
				action.(view, param);
			}
		});
		^cont
	}

	sendChanged {
		this.set(this.get); // ugly, but works
	}

	*freeAllSimpleControllers {
		// used to free all controllers when something break in the GUI and you can't access the controller anymore to remove it
		simpleControllers.do { arg con;
			con.remove
		};
		simpleControllers = List.new;

		userSimpleControllers.do { arg con;
			con.remove
		};
		userSimpleControllers = List.new;
	}

	stringGet { arg precision=6;
		var val;
		val = this.get;
		if(val.class == Ndef or: {val.class == Symbol or: {val.class == String}}) {
			// the parameter is mapped to a Ndef
			^val.asCompileString;
		} {
			switch(this.wrapper.valueType,
				\scalar, {
					^val.asFloat.asStringPrec(precision);
				},
				\array, {
					//param.debug("mapStaticText param");
					//param.get.debug("param get");
					if(val.first.isKindOf(Boolean)) {
						^val.collect({ arg x; x.asCompileString });
					} {
						^val.collect({ arg x; x.asFloat.asStringPrec(precision) });
					}
				},
				\env, {
					^val.asStringPrec(precision);
				}, {
					^val.asCompileString
				}
			);
		};
	}

	controllerTarget {
		^this.wrapper.controllerTarget;
	}

	controllerTargetCursor {
		^this.wrapper.controllerTargetCursor;
	}

	////// widgets

	mapMultiSlider { arg slider, action, trackCursor=false;
		var cursorAction;
		slider.step_(this.spec.step/this.spec.range);
		if(trackCursor) {
			cursorAction = { arg self, index;
				{
					self.index = index;
				}.defer(Server.default.latency)
			};
			slider.showIndex = true;
		};
		this.makeSimpleController(slider, 
			action: { arg self;
				// to be used in Pseq
				this.normSetList( self.value );
			},
			updateAction: { arg self;
				var val = this.normGet;
				//[ val.class, val ].debug("mapMultiSlider: updateAction");
				if(val.isKindOf(SequenceableCollection) and: { val[0].isKindOf(Number) }) {
					{
						self.value = val;
					}.defer;
				}
			},
			customAction:action,
			cursorAction: cursorAction
		);
		slider.addUniqueMethod(\attachContextMenu, {
			slider.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				if(buttonNumber == 1) {
					ParamProto.init;
					WindowDef(\OverlayMenu).front(slider, x, y, { arg def;
						HLayout (
							StaticText.new.string_("List size"),
							Param(Message(slider), \size, ControlSpec(1,32,\lin,1,4)).asNumberBox.maxWidth_(100)
						)
					} );
					false
				};
			});
		})
	}

	mapEnvelopeView { arg slider, action;
		this.makeSimpleController(slider, 
			updateAction: { arg self;
				var val = this.normGet;
				//[ val.class, val, val.asCompileString ].debug("mapMultiSlider: updateAction");
				if((val.isKindOf(Env) or: {val.isKindOf(SequenceableCollection)}) and: { val[0].isKindOf(Number) }) {
					{
						self.value = val;
					}.defer;
				}
			},
			customAction:action
		);
	}

	mapSlider { arg slider, action;
		this.makeSimpleController(slider, 
			updateAction: { arg self, param;
				var val;
				var color;
				//"start executing".debug;
				try {
					if(param.get.isKindOf(Number)) {
						val = param.normGet;
					} {
						val = nil
					}
				} { arg error;
					"In: %.mapSlider:updateAction".format(param).error;
					error.reportError;
					if(error.errorString.contains("Message 'round'")) {
						"ERROR: Param spec (%) expected a number but received %".format(this.spec, try{ this.getRaw.asCompileString }{ arg error; error.errorString }).postln;
					};
					//error.throw;
				};
                color = if(param.isSet) {
					Color.black;
                } {
					Color.grey(0.7);
                };
				if(val.isKindOf(Number)) {
					{
						self.value = val;
						self.knobColor = color;
					}.defer;
				}
			},
			customAction:action
		);
	}

	mapKnob { arg slider, action;
		this.makeSimpleController(slider, 
			updateAction: { arg self, param;
				var val;
				var color;
				//"start executing".debug;
				try {
					if(param.get.isKindOf(Number)) {
						val = param.normGet;
					} {
						val = nil
					}
				} { arg error;
					"In: %.mapKnob:updateAction".format(param).error;
					error.reportError;
					if(error.errorString.contains("Message 'round'")) {
						"ERROR: Param spec (%) expected a number but received %".format(this.spec, try{ this.getRaw.asCompileString }{ arg error; error.errorString }).postln;
					};
					//error.throw;
				};
                color = if(param.isSet) {
					Color.white; // default color
                } {
					Color.clear;
                };
				if(val.isKindOf(Number)) {
					{
						if(self.isClosed.not) {

							self.value = val;
							self.background = color;
						};
					}.defer;
				}
			},
			customAction:action
		);
	}

	mapStaticText { arg view, precision=6;
		this.makeSimpleController(view, {}, { arg view, param;
			{
				//param.type.debug("mapStaticText: update!");
				view.string = param.stringGet(precision);
			}.defer;
		}, nil, nil)
	}

	mapStaticTextLabel { arg view, labelmode;
		this.makeSimpleController(view, {}, {}, { arg view, param;
			{
				view.string = param.asLabel(labelmode);
			}.defer;
		}, nil)
	}

	mapTextField { arg view, precision=6, action;
		var updateAction = { arg force=false; 
			{ arg view, param;
				var val = "";
                var color;
				//Log(\Param).debug("mapTextField start: " ++ this.fullLabel);
                if(param.spec.isKindOf(ParamStringSpec)) {
					val = param.get;
				} {
					try {
						//[param, param.stringGet(precision)].debug("Param.mapTextField:get");
						val = param.stringGet(precision);
						//val = param.get.asCompileString;
					} { arg error;
						//Log(\Param).debug("param.get %", param.get);
						val = param.get.asCompileString;
					};
				};
				//Log(\Param).debug("mapTextField: val: %", val);
				// refresh action
                color = if(param.isSet) {
					ParamViewToolBox.color_TextField_enabled;
                } {
					ParamViewToolBox.color_TextField_disabled;
                };
				{
					//[val.asCompileString, view.hasFocus].debug("Param.mapTextField: hasfocus");
					// hasFocus is nil in some unidentified cases
					// i think the goal is to prevent continuously updating widgets in minimized windows
					// i think the real goal is to prevent updating the field while we are editing it
					if(view.isClosed.not) {
						// with ParamSelectDialog i get an error when view is already closed
						// should not happen since updater check if view is closed
						// maybe caused by defer delay
						if(force or: {view.hasFocus.notNil and: {view.hasFocus.not}}) {

							view.value = val;
							//view.debug("TextField updateAction view");
							//view.isClosed.debug("TextField updateAction closed");
							view.stringColor = color;
						} {
							Log(\Param).debug("mapTextField: focus, no set, view %, val %", view, val);
						};
					};
					//Log(\Param).debug("mapTextField: updateAction: end");
					//"done".debug;
				}.defer;
			};
		};
		this.makeSimpleController(view, 
			action: { arg view, param;
				var color;
				if(param.spec.isKindOf(ParamStringSpec)) { 
					param.set(view.value)
				} {
					param.set(view.value.interpret);
				};
				// updating the textfield value could change the string
				// so i only update isSet
                color = if(param.isSet) {
					ParamViewToolBox.color_TextField_enabled;
                } {
					ParamViewToolBox.color_TextField_disabled;
                };
				view.stringColor = color;
				//"done".debug;
			}, 
			updateAction: updateAction.value(false),
			initAction: updateAction.value(true),
			customAction: action
		)
	}

	mapNumberBox { arg view, action;
		this.makeSimpleController(view, { arg view, param;
			param.set(view.value.asFloat);
		}, { arg view, param;
			{
				view.value = param.get ? 0;
			}.defer;
		}, nil, action)
	}

	mapEZKnob { arg view, mapLabel=true, action;
		var param = this;
		view.controlSpec = param.spec;
		view.knobView.mapParam(param);
		view.numberView.mapParam(param);
		if(view.labelView.notNil and: {mapLabel == true}) {
			view.labelView.mapParamLabel(param);
		}
	}

	*unmapEZKnob { arg view, mapLabel = true;
		var param = this;
		view.knobView.unmapParam;
		view.numberView.unmapParam;
		if(view.labelView.notNil and: {mapLabel == true}) {
			view.labelView.unmapParam;
		}
	}

	mapEZSlider { arg view, mapLabel=true, action;
		var param = this;
		//view.debug("wTF");
		view.controlSpec = param.spec;
		view.sliderView.mapParam(param);
		view.numberView.mapParam(param);
		//view.debug("wTF");
		if(view.labelView.notNil and: {mapLabel == true}) {
			view.labelView.mapParamLabel(param);
		}
	}

	*unmapEZSlider {arg view, mapLabel = true;
		var param = this;
		view.sliderView.unmapParam;
		view.numberView.unmapParam;
		if(view.labelView.notNil and: {mapLabel == true}) {
			view.labelView.unmapParam;
		}
	}

	mapPopUpMenu { arg view, keys;
		if(this.type == \scalar) {
			this.mapIndexPopUpMenu(view, keys)
		} {
			this.mapValuePopUpMenu(view, keys)
		};
	}

	mapPopUpMenuHelper { arg view, keys, set, get, unmap, map, refreshChangeAction, action, onChange;
		// TODO: for refactoring menu method, not used for the moment
		var pm = view;
		//debug("mapValuePopUpMenu:1");

		if(keys.notNil and: { keys.isKindOf(TagSpec).not }) {
			keys = TagSpec(keys);
		};

		view.refreshChangeAction = refreshChangeAction ? {
			var spec;
			var val;
			//var isMapped = false;
			//[ this.spec.labelList.asArray, this.get, this.spec.unmapIndex(this.get)].debug("spec, get, unmap");
			if(keys.notNil) {
				spec = keys;
				val = get.();
			} {
				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					if(this.spec.isKindOf(ParamMappedBusSpec)) {
						val = this.getBus;
					} {
						val = get.();
					};
					spec = this.spec.tagSpec;
				} {
					val = get.();
					spec = this.spec;
				};
			};
			view.items = spec.labelList.asArray;
			view.value = spec.perform(unmap, val);
			//view.value.debug("mapValuePopUpMenu:1.5");
		};
		view.refreshChange;
		//[this.spec, this.get].debug("mapValuePopUpMenu:2");
		//view.value.debug("mapValuePopUpMenu:3");
		view.action = action ? {
			var spec;
			var isMapped = false;
			//view.value.debug("mapValuePopUpMenu:4 (action)");
			if(keys.notNil) {
				spec = keys;
			} {
				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					if(this.spec.isKindOf(ParamMappedBusSpec)) {
						isMapped = true
					};
					spec = this.spec.tagSpec;
				} {
					spec = this.spec;
				};
			};
			if(isMapped) {
				this.setBus(spec.perform(map, view.value));
			} {
				this.set(spec.perform(map, view.value));
			};
			//this.get.debug("mapValuePopUpMenu:5 (action)");
		};
		//[view, this.controllerTarget].value.debug("mapValuePopUpMenu:3.5");
		view.followChange(this.controllerTarget, \set, { arg aview, model, message, arg1;
			// TODO: do not change the whole row when just one value is updated!
			//[view, me, arg1, arg2, arg3].value.debug("mapValuePopUpMenu:6 (onchange)");
			if(arg1 == this.propertyRoot or: { arg1.isKindOf(SequenceableCollection) and: {
				arg1.includes(this.propertyRoot)
			} }) {
				aview.refreshChange;
			};
			//view.value.debug("mapValuePopUpMenu:7 (onchange)");
		});
		keys = keys ?? {this.spec};
		if( keys.isKindOf(TagSpec)) {
			view.followChange(keys, \list, onChange ? { arg aview, model, message, arg1;
				//var idx = view.value;
				view.items = keys.labelList.asArray;
				aview.refreshChange;
			});
		};
		//view.value.debug("mapValuePopUpMenu:8");
	}

	mapIndexPopUpMenu { arg view, keys;
		// this method i used when the target parameter contains an integer used as an index into the TagSpec's list
		// FIXME: mapIndexPopUpMenu does not use updater
		var pm = view;
		var mykeys;
		//[keys, this.spec, this.spec.labelList].debug("mapIndexPopUpMenu: whatXXX");
		if(keys.notNil and: { keys.isKindOf(TagSpec).not }) {
			keys = TagSpec(keys);
		};

		view.refreshChangeAction = { arg me;
			var spec;
			if(keys.notNil) {
				spec = keys;
			} {
				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					spec = this.spec.tagSpec;
				} {
					spec = this.spec;
				};

				//if(spec.isKindOf(TagSpec)) {
					////[keys, this.spec, this.spec.labelList].debug("whatXXX");
					//keys = spec.labelList;
				//};
			};
			if(spec.notNil) {
				try {
					if(spec.labelList.notNil) {
						pm.items = spec.labelList.asArray; // because PopUpMenu doesn't accept List
					}
				} { arg error;
					"In %.mapIndexPopUpMenu:refreshChangeAction".format(this).error;
					error.reportError;
				}
			};
			me.value = this.get;
		};
		pm.refreshChange;
		pm.action = {
			this.set(pm.value);
		};

		this.makeUpdater(view, { view.refreshChange });
		//view.onChange(this.controllerTarget, \set, { arg aview, model, message, arg1;
			//// TODO: do not change the whole row when just one value is updated!
			////[view, me, arg1, arg2, arg3].value.debug("mapValuePopUpMenu:6 (onchange)");
			//if(arg1 == this.propertyRoot or: { arg1.isKindOf(SequenceableCollection) and: {
				//arg1.includes(this.propertyRoot)
			//} }) {
				//aview.refreshChange;
			//};
			////view.value.debug("mapValuePopUpMenu:7 (onchange)");
		//});
		mykeys = keys ?? {this.spec};
		if( mykeys.isKindOf(TagSpec)) {
			view.onChange(mykeys, \list, { arg aview, model, message, arg1;
				if(arg1 == this.propertyRoot or: { arg1.isKindOf(SequenceableCollection) and: {
					arg1.includes(this.propertyRoot)
				} }) {
					//Log(\Param).debug("mapBusPopUpMenu:onChange: going to refresh");
					aview.refreshChange;
				};
				//aview.refreshChange;
			});
		}
	}

	mapValuePopUpMenu { arg view, keys;
		// this method is used when the target parameter contains a value, TagSpec find the key symbol associated to it in its list
		// FIXME: mapIndexPopUpMenu does not use updater
		// TODO: define a listener when the list change
		var mykeys;
		var pm = view;
		//debug("mapValuePopUpMenu:1");

		if(keys.notNil and: { keys.isKindOf(TagSpec).not }) {
			keys = TagSpec(keys);
		};

		view.toolTip = this.fullLabel;

		view.refreshChangeAction = {
			var vspec;
			var val;
			//var isMapped = false;
			//[ this.spec.labelList.asArray, this.get, this.spec.unmapIndex(this.get)].debug("spec, get, unmap");
			//Log(\Param).debug("refreshChangeAction %", this);
			//Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:1 spec %, keys %", this.spec, keys);
			if(keys.notNil) {
				vspec = keys;
				val = this.get;
			} {
				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					//if(this.spec.isKindOf(ParamMappedBusSpec)) {
					if(false) {
						val = this.getBus;
					} {
						val = this.get;
					};
					vspec = this.spec.tagSpec;
				} {
					val = this.get;
					vspec = this.spec;
				};
			};
			{
				try {
					//Log(\Param).debug("mapValuePopUpMenu refreshChangeAction labelList %", vspec.labelList);
					if(vspec.labelList.notNil) {
						//vspec.labelList.do(_.postln); // for debug
						view.items = vspec.labelList.asArray;
					};
					view.value = vspec.unmapIndex(val);
				} { arg error;
					"In %.mapValuePopUpMenu:refreshChangeAction".format(this).error;
					error.reportError;
				};
			}.defer;
			//view.value.debug("mapValuePopUpMenu:1.5");
		};
		view.refreshChange;
		//[this.spec, this.get].debug("mapValuePopUpMenu:2");
		//view.value.debug("mapValuePopUpMenu:3");
		view.action = {
			var spec;
			var isMapped = false;
			//view.value.debug("mapValuePopUpMenu:4 (action)");
			if(keys.notNil) {
				spec = keys;
			} {
				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					if(this.spec.isKindOf(ParamMappedBusSpec)) {
						isMapped = true
					};
					spec = this.spec.tagSpec;
				} {
					spec = this.spec;
				};
			};
			if(isMapped) {
				this.setBus(spec.mapIndex(view.value));
			} {
				this.set(spec.mapIndex(view.value));
			};
			//this.get.debug("mapValuePopUpMenu:5 (action)");
		};

		view.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(buttonNumber == 1) {
				var speclist;
				if(keys.notNil) {
					speclist = keys;
				} {
					if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec)}) {
						speclist = this.spec.tagSpec.list;
					} {
						if(this.spec.isKindOf(TagSpec)) {
							speclist = this.spec.list;
						};
					};
				};
				if(speclist.notNil) {
					var listlabel = "% %".format(this.fullLabel, speclist.tryPerform(\key) ?? { "" });
					ParamProto.init;
					//speclist.debug("speclist");
					WindowDef(\ListSelectDialog).front(speclist, { arg selected, asso, idx;
						//selected.asCompileString.debug("selected");
						//this.set(asso.value)
						view.valueAction = idx; // trigger user custom addAction on PopUp
					}, this.get, listlabel);
				}
			}
		});
		//[view, this.controllerTarget].value.debug("mapValuePopUpMenu:3.5");
		this.makeUpdater(view, { view.refreshChange });
		//view.onChange(this.controllerTarget, \set, { arg aview, model, message, arg1;
			//// TODO: do not change the whole row when just one value is updated!
			////[view, me, arg1, arg2, arg3].value.debug("mapValuePopUpMenu:6 (onchange)");
			//if(arg1 == this.propertyRoot or: { arg1.isKindOf(SequenceableCollection) and: {
				//arg1.includes(this.propertyRoot)
			//} }) {
				//aview.refreshChange;
			//};
			////view.value.debug("mapValuePopUpMenu:7 (onchange)");
		//});
		mykeys = keys ?? {this.spec};
		if( mykeys.isKindOf(TagSpec)) {
			view.followChange(mykeys, \list, { arg aview, model, message, arg1;
				aview.refreshChange;
			});
		};
		if( mykeys.isKindOf(ParamBufferSpec)) {
			//Log(\Param).debug("Enable PopUpMenu listener %", this);
			view.followChange(mykeys.tagSpec, \list, { arg aview, model, message, arg1;
				//Log(\Param).debug("PopUpMenu listener %", this);
				aview.refreshChange;
			});
		};
		//view.value.debug("mapValuePopUpMenu:8");
	}

	mapBusPopUpMenu { arg view, keys;
		// this method is used when the target parameter contains a mapped bus, use getRaw instead of get to avoid bus mode to return the bus value instead of the bus
		// FIXME: mapIndexPopUpMenu does not use updater
		// TODO: define a listener when the list change
		var pm = view;
		var mykeys;
		//debug("mapValuePopUpMenu:1");
		//Log(\Param).debug("mapBusPopUpMenu %", keys);
		if(keys.notNil and: { keys.isKindOf(TagSpec).not }) {
			keys = TagSpec(keys);
		};
		//Log(\Param).debug("mapBusPopUpMenu 0.1 %", keys);

		view.refreshChangeAction = {
			var xspec;
			//[ this.spec.labelList.asArray, this.get, this.spec.unmapIndex(this.get)].debug("spec, get, unmap");
			//Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:1 spec %, keys %", this.spec, keys);
			if(keys.notNil) {
				xspec = keys;
				//Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:2 spec %, keys %", xspec, keys);
			} {

				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					xspec = this.spec.tagSpec;
					//Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:3 spec %, keys %", xspec, keys);
				} {
					xspec = this.spec;
					//Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:4 spec %, keys %", xspec, keys);
				};
			};
			{
				try {
					if(xspec.labelList.notNil) {
						//Log(\Param).debug("PopUpMenu.refreshChangeAction: %", xspec.labelList.asArray);
						view.items = xspec.labelList.asArray;
					};
					view.value = xspec.unmapIndex(this.getBus);
				} { arg error;
					"In %.mapBusPopUpMenu:refreshChangeAction".format(this).error;
					error.reportError;
					//error.throw;
				};
			//Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:5 spec %, keys %", xspec, keys);
			//Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:6 spec %, keys %", xspec, keys);
			}.defer;
			//view.value.debug("mapValuePopUpMenu:1.5");
		};
		view.refreshChange;
		//[this.spec, this.get].debug("mapValuePopUpMenu:2");
		//view.value.debug("mapValuePopUpMenu:3");
		view.action = {
			var xspec;
			//Log(\Param).debug("mapBusPopUpMenu:action:1: view.value %", view.value);
			//Log(\Param).debug("mapBusPopUpMenu:action:2 spec %, keys %", this.spec, keys);
			if(keys.notNil) {
				xspec = keys;
				//Log(\Param).debug("mapBusPopUpMenu:action:3 spec %, keys %", xspec, keys);
			} {
				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					xspec = this.spec.tagSpec;
					//Log(\Param).debug("mapBusPopUpMenu:action:4 spec %, keys %", xspec, keys);
				} {
					xspec = this.spec;
					//Log(\Param).debug("mapBusPopUpMenu:action:5 spec %, keys %", xspec, keys);
				};
			};
			//Log(\Param).debug("mapBusPopUpMenu:action:6 spec %, keys %", xspec, keys);
			this.setBus(xspec.mapIndex(view.value));
			//Log(\Param).debug("mapBusPopUpMenu:action: end");
		};

		view.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(buttonNumber == 1) {
				var speclist;
				if(keys.notNil) {
					speclist = keys;
				} {
					if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec)}) {
						speclist = this.spec.tagSpec.list;
					} {
						if(this.spec.isKindOf(TagSpec)) {
							speclist = this.spec.list;
						};
					};
				};
				if(speclist.notNil) {
					ParamProto.init;
					//speclist.debug("speclist");
					WindowDef(\ListSelectDialog).front(speclist, { arg selected, asso;
						//selected.asCompileString.debug("selected");
						this.setRaw(asso.value)
					}, this.getRaw)
				}
			}
		});

		//[view, this.controllerTarget].value.debug("mapValuePopUpMenu:3.5");
		view.onChange(this.controllerTarget, \set, { arg aview, model, message, arg1;
			// TODO: do not change the whole row when just one value is updated!
			//Log(\Param).debug("mapBusPopUpMenu:onChange: prop:% view:% model:% msg:% arg:%", this.propertyRoot, aview, model, message, arg1);
			if(arg1 == this.propertyRoot or: { arg1.isKindOf(SequenceableCollection) and: {
				arg1.includes(this.propertyRoot)
			} }) {
				//Log(\Param).debug("mapBusPopUpMenu:onChange: going to refresh");
				aview.refreshChange;
			};
			{
				//Log(\Param).debug("mapBusPopUpMenu:onchange: end", view.value);
			}.defer;
		});
		mykeys = keys ?? {this.spec};
		if( mykeys.isKindOf(TagSpec)) {
			view.onChange(mykeys, \list, { arg aview, model, message, arg1;
				if(arg1 == this.propertyRoot or: { arg1.isKindOf(SequenceableCollection) and: {
					arg1.includes(this.propertyRoot)
				} }) {
					//Log(\Param).debug("mapBusPopUpMenu:onChange: going to refresh");
					aview.refreshChange;
				};
			});
		}
		//view.value.debug("mapValuePopUpMenu:8");
	}

	unmapPopUpMenu { arg view;
		// FIXME: mapIndexPopUpMenu does not use updater
		this.freeUpdater(view);
	}

	mapButton { arg view, action;
		var cursorAction;
		var update = { arg view, param;
			var size;
			{
				var val;
				size = view.states.size;
				// FIXME: not sure why default is not used (in stepseqitem)
				// setting it in GUI for the moment
				if(param.get.isNil) {
					val = 0;
				} {
					val = param.normGet;
				};
				view.value = val.linlin(0,1,0,size-1);
			}.defer
		};
		if(this.subIndex.notNil) {
			cursorAction = { arg vie, idx, val;
				if(idx.notNil) {
					{
						if(idx == this.subIndex) {
							vie.label = "O"
						} {
							vie.label = " "
						}
					}.defer(Server.default.latency)
				}
			};
		};
		this.makeSimpleController(view, { arg view, param;
			var size;
			size = view.states.size;
			param.normSet(view.value.linlin(0,size-1,0,1));
		}, update, nil, action, cursorAction: cursorAction)
	}

	mapLED { arg view, action;
		var color_on = Color.green;
		var color_off = Color.black;
		var update = { arg view, param;
			var size;
			{
				var val;
				// FIXME: not sure why default is not used (in stepseqitem)
				// setting it in GUI for the moment
				if(param.get.isNil) {
					val = 0;
				} {
					val = param.normGet;
				};
				view.stringColor = if(val.round == 1) {
					color_on;
				} {
					color_off;
				};
			}.defer
		};
		this.makeSimpleController(view, { arg view, param;
			// no action
		}, update, nil, action)
	}

	mapCheckBox { arg view, action;
		var update = { arg view, param;
			var size;
			{
				var val;
				// FIXME: not sure why default is not used (in stepseqitem)
				// setting it in GUI for the moment
				if(param.get.isNil) {
					val = false;
				} {
					val = param.normGet;
				};
				view.value = val.asBoolean;
			}.defer
		};
		this.makeSimpleController(view, { arg view, param;
			var size;
			param.normSet(view.value.asInteger);
		}, update, nil, action)
	}

	mapMenuAction { arg view, label, action;
		// support only boolean for the moment
		// no need for update, menus are temporary (and there is no .onClose method)
		if(label.notNil) {
			view.string = label;
		};
		view.checkable = true;
		view.checked = this.get;
		view.action = { arg view;
			this.set(view.checked);
			action.();
		};
	}

	*freeUpdater { arg view;
		var controller;
		var skipjack;
		var free_controller = {
			controller.remove; 
			simpleControllers.remove(controller) 
		};
		var free_skipjack = {
			skipjack.stop;
		};
		controller = view.getHalo(\simpleController);
		skipjack = view.getHalo(\skipJack);
		{
			view.action = nil;
		}.defer;
		if(controller.notNil) {
			view.addHalo(\simpleController, nil);
			free_controller.();
		};
		if(skipjack.notNil) {
			view.addHalo(\skipJack, nil);
			free_skipjack.();
		};
	}

	*unmapView { arg view;
		this.freeUpdater(view);
	}

	*unmapSlider { arg slider;
		Param.unmapView(slider)
	}

	//////// GUI shortcuts

	asSlider {
		^Slider.new.mapParam(this);
	}

	asKnob {
		^Knob.new.mode_(\vert).mapParam(this);
	}

	//asEZKnob {
	//	^EZKnob.new.mapParam(this);
	//}

	asMultiSlider { arg trackCursor=true;
		^MultiSliderView.new
			.elasticMode_(1)
			.indexThumbSize_(100)
			.isFilled_(false)
			.fillColor_(ParamViewToolBox.color_ligth)
			.strokeColor_(Color.black)
			.size_(this.numChannels)
			.mapParam(this, trackCursor:trackCursor);
	}

	asStaticText {
		^StaticText.new.mapParam(this);
	}

	asStaticTextLabel { arg labelmode;
		^StaticText.new.mapParamLabel(this, labelmode).addUniqueMethod(\attachContextMenu, { arg view;
			ParamViewToolBox.attachContextMenu(this, view)
        });
	}

	asTextField { arg precision=6, action;
		^TextField.new.mapParam(this, precision, action);
	}

	asNumberBox {
		^NumberBox.new
			.clipLo_(this.spec.clipLo)
			.clipHi_(this.spec.clipHi)
			.step_(this.spec.step)
			.mapParam(this);
	}

	asEnvelopeView {
		var view;

		view = FixedEnvelopeView.new(nil, Rect(0, 0, 230, 80))
			.drawLines_(true)
			.selectionColor_(Color.red)
			.drawRects_(true)
			.step_(0)
			.thumbSize_(10)
			.elasticSelection_(false)
			.keepHorizontalOrder_(true)
			.rightClickZoomEnabled_(true);

		try {
			var step = this.spec.times[0].unmap(1/8);
			var point = Point(step,1/8);
			var dur = this.spec.times[0].unmap(2);
			view.grid_(point);
			view.totalDur_(dur);
			view.gridOn_(true);
			view.mapParam(this); 	// should be after spec access which can fail

		} { arg error;
			"In: %.asEnvelopeView".format(this).error;
			error.reportError;
			if(this.spec.isKindOf(ParamEnvSpec).not) {
				"%.asEnvelopeView: spec is probably not compatible with EnvelopeView: %".format(this, this.spec).error;
			};
		};
		^view;
	}

	asButton { arg label;
		var but;
		label = label ?? { this.propertyLabel ?? { "" }};
		but = BoolButton.new.string_(label).minWidth_(10);
		but.mapParam(this);
		^but;
	}

	asLED { arg label;
		var but;
		label = label ?? { "◉" };
		but = StaticText.new.string_(label).fixedSize_(10@10).font_(Font.default.size_(9));
		this.mapLED(but);
		^but;
	}

	asCheckBox { arg label;
		var but;
		label = label ?? { this.propertyLabel ?? { "" }};
		but = CheckBox.new.string_(label).minWidth_(10);
		but.mapParam(this);
		^but;
	}

	asMenu { arg label, keys;
		var but;
		var xspec;
		label = label ?? { this.propertyLabel ?? { "" }};
		if(keys.notNil) {
			xspec = keys.collect({ arg k; k -> k });
		} {
			if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
				xspec = this.spec.tagSpec.list;
			} {
				if(this.spec.isKindOf(TagSpec)) {
					xspec = this.spec.list;
				}
			};
		};
		if(xspec.isKindOf(SequenceableCollection)) {
			^Menu(*
				xspec.collect { arg asso, idx;
					MenuAction(asso.key, {
						this.set(asso.value)
					}).checked_(this.get == asso.value)
				};
			).title_(label);
		} {
			^Menu( MenuAction("Spec not implemented"), { this.spec.asCompileString.debug("spec") } )
		};
	}

	asMenuAction { arg label, action;
		var but;
		label = label ?? { this.propertyLabel ?? { "" }};
		but = MenuAction.new;
		but.mapParam(this, label, action);
		^but;
	}

	asPopUpMenu { arg keys;
		^PopUpMenu.new.mapParam(this, keys) // is Param.mapPopUpMenu
	}

	asValuePopUpMenu { arg keys;
		^PopUpMenu.new.mapValueParam(this, keys) // is Param.mapValuePopUpMenu
	}

	asBusPopUpMenu { arg keys;
		//Log(\Param).debug("asBusPopUpMenu %", keys);
		^PopUpMenu.new.mapBusParam(this, keys) // is Param.mapBusPopUpMenu
	}

	asIndexPopUpMenu { arg keys;
		^PopUpMenu.new.mapIndexParam(this, keys) // is Param.mapIndexPopUpMenu
	}

	asView { arg labelmode;
		^ParamGroupLayout.formEntry(this, labelmode)
		//case(
			//{ this.spec.isKindOf(ParamEnvSpec) }, {
				//^this.asEnvelopeView;
			//},
			//{ this.spec.isKindOf(ParamArraySpec) }, {
				//^this.asMultiSlider;
			//},
			//{ this.spec.isKindOf(TagSpec) }, {
				//^this.asPopUpMenu;
			//},
			//{ this.spec.isKindOf(ParamBufferSpec) }, {
				//var scv = SampleChooserView.new;
				//^scv.mapParam(this).view.addHalo(\ViewHolder, scv);
			//}, {
				//^this.asKnob;
			//}
		//)
	}

	edit {
		var fun = editFunction ? { arg param;
			var win;
			var label, widget, val;
			win = Window.new;

			win.layout = VLayout.new(
				if(WindowDef(\ParamEditorSimple).notNil) {
					WindowDef(\ParamEditorSimple).asView(param)
				} {
					param.asView(\full)
				},
				nil,
			);
			//win.alwaysOnTop = true;
			win.front;
		};
		fun.(this);
	}

	/////////// Spec

	*valueToSpec { arg val, default_spec;
		var def = default_spec ? defaultSpec;
		^if(val.isKindOf(Array) or: val.isKindOf(List)) {
			ParamArraySpec( def ! val.size )
		} {
			if(val.isKindOf(Env)) {
				ParamEnvSpec( def ! val.levels.size )
			} {
				def.asSpec
			}
		};
	}

	*toSpec { arg spec, argName, default_spec=\widefreq;
		if(spec.isNil, {
			if( argName.notNil and: {argName.asSpec.notNil}, {
				spec = argName.asSpec;
			}, {
				spec = default_spec.asSpec;
			});
		});
		^spec
	}

	*toNdefSpec { arg spec, argName, default_spec=\widefreq;
		if(spec.isNil, {
			if( argName.asSpec.notNil, {
				spec = argName.asSpec;
			}, {
				spec = default_spec.asSpec;
			});
		});
		^spec
	}

	*specFromDefaultValue { arg argname, defname, default_spec;
		var def = Param.getSynthDefDefaultValue(argname, defname);
		var rval;
		default_spec = default_spec ? Param.defaultSpec;
		if(def.class == Float) {
			rval = default_spec;
		} {
			if(def.isSequenceableCollection) {
				rval = ParamArraySpec(default_spec!def.size);
			}
		};
		^rval;
	}

	*getSynthDefSpec { arg argName, defname=nil;
		//var val;
		//var rval;
		//var desc;
		//val = SynthDescLib.global.synthDescs[defname];
		//desc = val;
		////Log(\Param).debug("synthdesc % % %", argName, defname, val);
		//if(val.notNil) {
			//val = val.metadata;
			//if(val.notNil) {
				//val = val.specs;
				//if(val.notNil) {
					//rval = val[argName];
				//}
			//} {
				//rval = desc.getSpec[argName]
			//}
		//};
		//^rval;
		if(SynthDesc(defname).notNil) {
			^SynthDesc(defname).allSpecs[argName]
		} {
			^nil
		}
	}

	*toSynthDefSpec { arg spec, argName, defname=nil, default_spec=\widefreq;
		// try to deduce spec from default value if no spec defined for the synthdef
		if(spec.isNil) {
			var val;
			var rval;
			val = SynthDescLib.global.synthDescs[defname];
			if(val.notNil) {
				// if there is a SynthDesc, check metadata, then Spec.specs, then deduce from default value
				val = val.metadata;
				if(val.notNil) {
					val = val.specs;
					if(val.notNil) {
						rval = val[argName];
					}
				};
				if(rval.isNil and: {argName.notNil and: { argName.asSpec.notNil }}) {
					rval = argName.asSpec;
				};
			   	if(rval.isNil) {
					// no metadata but maybe a default value
					var def = Param.getSynthDefDefaultValue(argName, defname);
					if(def.class == Float) {
						rval = default_spec.asSpec;
					} {
						if(def.isSequenceableCollection) {
							rval = ParamArraySpec(default_spec!def.size);
						}
					};
				};
			};
			//rval = rval.asSpec; // if rval is nil, asSpec return \unipolar
			spec = this.toSpec(rval, argName, default_spec);
		};
		^spec;
	}

	*getSynthDefDefaultValue { arg argName, defname;
		var desc;
		var val;
		//"getSynthDefDefaultValue 1".debug;
		desc = SynthDescLib.global.synthDescs[defname];
		//desc.debug("getSynthDefDefaultValue 2");
		val = if(desc.notNil) {
			var con = desc.controlDict[argName];
		//con.debug("getSynthDefDefaultValue 4");
			if(con.notNil) {
		//"getSynthDefDefaultValue 5".debug;
				con.defaultValue;
			}
		};
		^val;
	}

    *getInstrumentFromPbind { arg inval;
		^BaseParam.getInstrumentFromPbind(inval);
    }

	/////////////
	
	*isSynthDefParameter { arg argName, defname, else_return=false;
		// else_return: return value if not found
		var desc;
		var val;
		desc = SynthDescLib.global.synthDescs[defname];
		if(desc.notNil) {
			var con = desc.controlDict[argName];
			^con.notNil
		};
		^else_return; // no synthdesc found
	}
	
	isSynthDefParameter { arg else_return=false;
		// TODO: for Ndef check if part of definition
		^this.class.isSynthDefParameter(wrapper.propertyRoot, wrapper.instrument, else_return)
	}

	//////////////////////

    doesNotUnderstand { arg selector...args;
        if(wrapper.class.findRespondingMethodFor(selector).notNil) {
			if(selector.asString.endsWith("_")) {
				wrapper.perform(selector, * args);
			} {
				^wrapper.perform(selector, * args);
			}
		} {
			DoesNotUnderstandError(this, selector, args).throw
		};
	}

}

///////////////// parent classes

BaseParam {
	var <target, <property, <>spec, <key;
	var >shortLabel; // deprecated
	var >combinator;
	var <>combinatorEnabled = true;
	var <>labelmode;
	var >default;
	var >label; 
	var <>subIndex;

	/////// labels

	shortLabel { // deprecated
		^( shortLabel ? property );
	}

	typeLabel {
		^""
	}

	combinator {
		^combinator;
		//^if(combinator.isNil and: { combinatorEnabled == true }) {
			//target.getHalo(( \ParamCombinator_++property ).asSymbol)
		//} {
			//combinator;
		//}
	}

	hasCombinator {
		^target.getHalo(( \ParamCombinator_++this.propertyRoot ).asSymbol).notNil
	}

	getCombinator {
		^target.getHalo(( \ParamCombinator_++this.propertyRoot ).asSymbol)
	}

	clearCombinator { arg fullyRemove=false;
		if(this.hasCombinator) {
			this.getCombinator.clear(fullyRemove);
		};
	}

	propertyLabel {
		^property.asString
	}

	propertyRoot {
		^property
	}

    propertyArray {
		^BaseAccessorParam.associationToArray(this.property)
    }

	targetLabel {
		^target.asString
	}

	fullLabel {
		^"% % %".format(this.typeLabel, this.targetLabel, this.propertyLabel)
	}

	asLabel { arg labelmode; // backward compat
		^this.label(labelmode)
	}
	
	label { arg alabelmode;
		if(label.notNil) {
			^label
		} {
			if(( alabelmode ? labelmode ) == \full) {
				^this.fullLabel
			} {
				^this.propertyLabel
			}
		}
	}

	/////////////////

	at { arg idx;
		//^this.wrapper.at(idx, idx2);
		^Param(this.target, this.property -> idx, this.spec)
	}

	size {
		if(this.spec.tryPerform(\isDynamic) == true) {
			^this.get.size
		} {
			^this.spec.size
		}
	}

	asParamList {
		^this.size.collect { arg x;
			this.at(x)
		}
	}

	do { arg fun;
		this.size.do { arg x;
			fun.(this.at(x), x)
		}
	}

	collect { arg fun;
		^this.size.collect { arg x;
			fun.(this.at(x), x)
		}
	}

	setList { arg list;
		// this method replace individuals values in the array
		// to be used with Pseq because Pseq keep reference to the first array received
		if(this.get.notNil) {
			this.do { arg subparam, x;
				subparam.set(list[x])
			}
		} {
			this.set(list);
		}
	}

	normSetList { arg list;
		// this method replace individuals values in the array
		// to be used with Pseq because Pseq load the array only once
		//if(this.get.notNil) {
			//this.do { arg subparam, x;
				//subparam.normSet(list[x])
			//}
		//} {
			//this.normSet(list);
		//}

		// FIXME: should determine when to do sub and when not
		// when controlling a PstepSeq list with MultiSliderView, using sub, changes are not picked
		this.normSet(list);
	}

	type {
		// type according to spec
		var res = [
			[ParamEnvSpec, \env],
			[ParamArraySpec, \array],
			[ControlSpec, \scalar],
		].detect({ arg x;
			this.spec.isKindOf(x[0])
		});
		if(res.notNil) {
			^res[1];
		} {
			^\other
		};
		//^switch(this.spec.class,
		//	ParamEnvSpec, \env,
		//	ParamArraySpec, \array,
		//	\scalar,
		//)
	}

	valueType {
		// type according to value
		var res = [
			[Env, \env],
			[SequenceableCollection, \array],
			[Number, \scalar],
		].detect({ arg x;
			this.get.isKindOf(x[0])
		});
		if(res.notNil) {
			^res[1];
		} {
			^\other
		};
		//^switch(this.spec.class,
		//	ParamEnvSpec, \env,
		//	ParamArraySpec, \array,
		//	\scalar,
		//)
	}

	controllerTarget {
		^this.target
	}

	controllerTargetCursor {
		^nil
	}

	instrument { nil }

	default {
		var instr;
		var val;
		if(default.notNil) {
			^default;
		};
		instr = this.instrument;
		if(instr.notNil) {
			val = Param.getSynthDefDefaultValue(this.propertyRoot, instr) ?? { spec !? _.default ? 0 };
			if(spec.isKindOf(ParamEnvSpec)) {
				val = val.asEnv;
			};
		} {
			if(spec.isNil) {
				val = 0;
			} {
				val = spec.default
			}
		};
		^val.copy;
	}

	inBusMode {
		^false
	}

	inBusMode_ {
		// do nothing, to be subclassed
	}

	inRawMode {
		// WIP
		// should not try to read or write bus, return the bus or set a new bus instead
		var sp;
		sp = this.spec;
		^sp.isKindOf(ParamMappedBusSpec) or: {
			sp.isKindOf(ParamArraySpec) and: { sp.array.any(_.isKindOf(ParamMappedBusSpec)) }
		}
	}

	*getInstrumentFromPbind { arg inval;
		^if(inval.notNil) {
			case(
				{ inval.isKindOf(Pbind) }, {
					inval = inval.patternpairs.clump(2).detect { arg pair;
						pair[0] == \instrument
					};
					if(inval.notNil) {
						inval = inval[1];
						if(inval.class == Symbol or:{ inval.class == String }) {
							inval;
						} {
							nil
						}
					} {
						nil
					}

				},
				{ inval.isKindOf(PbindProxy) }, {
					inval = inval.pairs.clump(2).detect { arg pair;
						pair[0] == \instrument
					};
					if(inval.notNil) {
						inval = inval[1];
						if(inval.isKindOf(PatternProxy)) {
							inval = inval.source;
						};
						if(inval.class == Symbol or:{ inval.class == String }) {
							inval;
						} {
							nil
						}
					} {
						nil
					}
				},
				{ inval.isKindOf(Pmono) }, {
					inval.synthName
				}, {
					nil
				}
			)
		}
	}

	getBus {
		^this.get
	}

	setBus { arg val;
		this.set(val)
	}

	getRaw {
		^target.get(property)
	}

	setRaw { arg val;
		^target.set(property, val)
	}

	isSet {
		^true
	}
}

ParamAccessor {
	// self.obj is the param, set by BaseAccessorParam.init
	*neutral { 
		^(
			key: \neutral,
			setval: { arg self, val;
				self.obj.setVal(val);
			},

			getval: { arg self;
				//Log(\Param).debug("neutral.getval obj %", self.obj);
				self.obj.getVal;
			},

			setbus: { arg self, val;
				// set the bus (or map) and not its value
                // multichannel mapped bus for arrayed synthdef parameters should be nested
                val = Pdef.nestOn(val);
				self.obj.setRaw(val);
			},


			getbus: { arg self;
				var val = self.obj.getRaw;
                // multichannel mapped bus for arrayed synthdef parameters should be nested
                val = Pdef.nestOff(val);
				val;
			},

			toSpec: { arg self, sp;
				sp.asSpec;
			},

			property: { arg self, prop;
				prop
			},

			propertyLabel: { arg self;
				self.obj.property.asString;
			},

			path: { arg self, prop;
				prop
			},
		)

	}

	// selectors

	*array { arg idx;
		^(
			key: \array,
			setval: { arg self, val;
				// set the value or the value of the bus
				var oldval;
				oldval = self.obj.parent.get;

				oldval[idx] = val; 
				self.obj.parent.set(oldval);
			},

			getval: { arg self;
				var val;
				//Log(\Param).debug("neutral.getval obj %", self.obj);
				val = self.obj.parent.get;
				val[idx]
			},

			setbus: { arg self, val;
				// set the bus and not its value
				var oldval;
				oldval = self.obj.parent.getNest;
				oldval = oldval ?? { self.obj.parent.default };
				oldval[idx] = val; 
				self.obj.parent.setNest(oldval);
			},


			getbus: { arg self;
				var val;
				//Log(\Param).debug("euhh1");
				val = self.obj.parent.getNest;
				//Log(\Param).debug("euhh10 %", val);
				val !? {
				//Log(\Param).debug("euhh11 %", val);
					val[idx];
				}
			},


			toSpec: { arg self, sp;
				if(sp.isKindOf(ParamArraySpec)) {
					sp[idx]
				} {
					sp.asSpec;
				}
			},

			subIndex: { arg self;
				idx;
			},

			property: { arg self, prop;
				idx
			},

			propertyLabel: { arg self;
				"% %".format(self.obj.parent.property, idx)
			},

			path: { arg self;
				self.obj.parent.property -> idx;
			},
		);
	}

	*envitem { arg selector, idx;
		^(
			key: \envitem,
			setval: { arg self, val;
				var oldval;
				var lvals;
				oldval = self.obj.parent.get;

				// need to assign selector (Env.levels for example) for the update to be detected by Ndef
				lvals = oldval.perform(selector);
				lvals[idx] = val;
				oldval.perform(selector.asSetter, lvals);
				self.obj.parent.set(oldval);
			},

			getval: { arg self;
				var oldval;
				oldval = self.obj.parent.get;
				oldval.perform(selector)[idx]
			},

			toSpec: { arg self, sp;
				if(sp.isKindOf(ParamEnvSpec)) {
					sp.perform(selector)[idx];
				} {
					sp.asSpec;
				}
			},
			
			subIndex: { arg self;
				idx;
			},

			property: { arg self, prop;
				idx
			},

			propertyLabel: { arg self;
				"% %%".format(self.obj.parent.property, selector.asString[0], idx)
			},

			path: { arg self, prop;
				prop -> selector -> idx;
			},
		);
	}

	// pbindef and stepseq

	*stepseq { arg selector;
		// Pbindef key with PstepSeq inside
		// note: work also with Pseq
		// build with Param( Pbindef(\bla), \rq -> \stepseq)
		^(
			key: \stepseq,
			setval: { arg self, val;
				var res;
				//Log(\Param).debug("stepseq.setval % %", self.obj, val);
				res = self.obj.target.source.at(selector);
				if(res.source.isKindOf(ListPattern)) {
					res.source.list = val;
				} {
					res.source = val;
				};
				self.obj.target.changed(\set, self.obj.parent.property); // we don't call Pdef.set so no changed message
			},

			getval: { arg self;
				var res;
				//Log(\Param).debug("stepseq.getval %", self.obj);
				res = self.obj.target.source.at(selector);
				if(res.source.isKindOf(ListPattern)) {
					res.source.list
				} {
					res.source
				}
			},

			setbus: { arg self, val;
				// TODO
				// set the bus (or map) and not its value
				self.obj.setRaw(val);
			},


			getbus: { arg self;
				// TODO
				self.obj.getRaw;
			},

			toSpec: { arg self, sp;
				if(sp.isKindOf(ParamArraySpec).not) {
					ParamArraySpec(sp.asSpec);
				} {
					sp;
				}
			},

			property: { arg self, prop;
				prop
			},

			propertyLabel: { arg self;
				"% seq".format(selector)
			},

			path: { arg self, prop;
				prop
			},

			controllerTargetCursor: { arg self;
				self.obj.target.source.at(selector).source
			},
		)
	}

	*stepseqitem { arg selector, index;
		// Pbindef key with PstepSeq inside
		// build with Param( Pbindef(\bla), \rq -> \stepseq -> 0 )
		^(
			key: \stepseqitem,
			parent: this.array(index),
			//setval: { arg self, val;
				//var res;
				//res = self.obj.target.source.at(selector);
				//res.source.list.put(index, val);
				//self.obj.target.changed(\set, self.obj.parent.property, index); // we don't call Pdef.set so no changed message
			//},

			//getval: { arg self;
				//var res;
				//res = self.obj.target.source.at(selector);
				//res.source.list.at(index)
			//},

			//setbus: { arg self, val;
				//// TODO
				//// set the bus (or map) and not its value
				//self.obj.setRaw(val);
			//},


			//getbus: { arg self;
				//// TODO
				//self.obj.getRaw;
			//},

			//toSpec: { arg self, sp;
				//sp.asSpec;
			//},

			//subIndex: { arg self;
				//index;
			//},

			//property: { arg self, prop;
				//prop
			//},

			//propertyLabel: { arg self;
				//"% %".format(self.obj.parent.property, index)
			//},

			//path: { arg self, prop;
				//prop
			//},

			controllerTargetCursor: { arg self;
				self.obj.target.source.at(selector).source
			},
		)
	}

	*pdefn_stepseq { 
		// Pbindef key with PstepSeq inside
		// note: work also with Pseq
		// build with Param( Pbindef(\bla), \rq -> \stepseq)
		^(
			key: \stepseq, // used by controllerTargetCursor not sure if i need to change it
			setval: { arg self, val;
				var res;
				//Log(\Param).debug("stepseq.setval % %", self.obj, val);
				res = self.obj.target;
				res.source.list = val;
				self.obj.target.changed(\set); // we don't call Pdef.set so no changed message
			},

			getval: { arg self;
				var res;
				//Log(\Param).debug("stepseq.getval %", self.obj);
				res = self.obj.target;
				res.source.list
			},

			setbus: { arg self, val;
				// TODO
				// set the bus (or map) and not its value
				self.obj.setRaw(val);
			},


			getbus: { arg self;
				// TODO
				self.obj.getRaw;
			},

			toSpec: { arg self, sp;
				ParamArraySpec(sp.asSpec);
			},

			property: { arg self, prop;
				prop
			},

			propertyLabel: { arg self;
				"seq"
			},

			path: { arg self, prop;
				prop
			},

			controllerTargetCursor: { arg self;
				self.obj.target.source
			},
		)
	}

	*pdefn_stepseqitem { arg index;
		// Pbindef key with PstepSeq inside
		// build with Param( Pbindef(\bla), \rq -> \stepseq -> 0 )
		^(
			key: \stepseqitem,
			setval: { arg self, val;
				var res;
				res = self.obj.target;
				res.source.list.put(index, val);
				self.obj.target.changed(\set, index); // we don't call Pdef.set so no changed message
			},

			getval: { arg self;
				var res;
				res = self.obj.target;
				res.source.list.at(index)
			},

			setbus: { arg self, val;
				// TODO
				// set the bus (or map) and not its value
				self.obj.setRaw(val);
			},


			getbus: { arg self;
				// TODO
				self.obj.getRaw;
			},

			toSpec: { arg self, sp;
				if(sp.isKindOf(ParamArraySpec)) {
					sp[index]
				} {
					sp.asSpec;
				}
			},

			subIndex: { arg self;
				index;
			},

			property: { arg self, prop;
				prop
			},

			propertyLabel: { arg self;
				"seq %".format(index)
			},

			path: { arg self, prop;
				prop -> index
			},
		)
	}

	// WIP

	*pbindef_source { arg selector;
		// access Pbindef key source directly
		// build with Param( Pbindef(\bla), \rq -> \source)
		// selector is propertyRoot
		// TODO: not updated when set from code with Pbindef, should listen to \source
		// TODO: default value is not from the SynthDef
		^(
			key: \pbindef_source,
			setval: { arg self, val;
				var res;
				//Log(\Param).debug("stepseq.setval % %", self.obj, val);
				res = self.obj.target.source;
				if(res.isNil) {
					self.obj.target.source = PbindProxy.new;
					res = self.obj.target.source;
				};
				res = res.at(selector);
				//if(val.isKindOf(SequenceableCollection) or: { val.isKindOf(Env) } ) {
                //Log(\Param).debug("shoud be nested % : %", val, self.obj.shouldBeNested(val));
				if(self.obj.shouldBeNested(val)) {
					val = Pdef.nestOn(val);
				};
				if(self.obj.hasCombinator) {
					self.obj.getCombinator.set(val);
				} {
					if(res.isNil) {
						self.obj.target.source.set(selector, val);
					} {
						if(self.obj.spec.isKindOf(ParamMappedBusSpec).not and:{ self.inBusMode }) {
							var curval = res.source;
							var bus;
							curval = Pdef.nestOff(curval);
							bus = curval.asCachedBus;
							if(val.isKindOf(ParamCombinator)) {
								// if the value we set is a ParamCombinator, set it as source
								res.source = val;
							} {
								if(curval.isKindOf(ParamCombinator)) {
									// if the param has a combinator, set its base param
									curval.baseParam.set(val)
								} {
									if(curval.isSequenceableCollection) {
										bus.setn(val);
									} {
										bus.set(val);
									};
								}
							}
						} {
							res.source = val;
						}
					};
				};
				self.obj.target.changed(\set, self.obj.parent.property); // we don't call Pdef.set so no changed message
			},

			getval: { arg self;
				var res;
				//Log(\Param).debug("stepseq.getval %", self.obj);
				res = self.obj.target.source;
				if(res.notNil) {
					res = res.at(selector);
					if(self.obj.hasCombinator) {
						self.obj.getCombinator.get; // halo combinator
					} {
						res.source !? { arg x; 
							if(self.obj.spec.isKindOf(ParamMappedBusSpec).not and: {self.inBusMode}) {
								x.asCachedBus.getCached; // value ParamCombinator respond to .asCachedBus
							} {
								Pdef.nestOff(x) 
							}
						}
					} ?? { self.obj.default }
				} {
					Log(\Param).debug("Param.get: no source defined for Pbindef %", self.obj.target);
					self.obj.default
				}
			},

			getRaw: { arg self;
				var res;
				//Log(\Param).debug("stepseq.getval %", self.obj);
				res = self.obj.target.source;
				if(res.notNil) {
					res = res.at(selector);
					res.source 
				};
			},

			setRaw: { arg self, val;
				var res;
				//Log(\Param).debug("stepseq.setval % %", self.obj, val);
				res = self.obj.target.source;
				if(res.isNil) {
					self.obj.target.source = PbindProxy.new;
					res = self.obj.target.source;
				};
				res = res.at(selector);
				//if(val.isKindOf(SequenceableCollection) or: { val.isKindOf(Env) } ) {
				if(res.isNil) {
					self.obj.target.source.set(selector, val);
				} {
					res.source = val;
				};
				self.obj.target.changed(\set, self.obj.parent.property); // we don't call Pdef.set so no changed message
			},

			unset: { arg self;
				if(self.obj.target.source.at(selector).notNil) { // Pbindef recreate key if not defined
					self.obj.target.source.set(selector, nil);
					self.obj.target.changed(\set, [selector]); // no changed msg when setting PbindProxy
				}
			},

			inBusMode_: { arg self, enable;
				if(enable == true) {
					var val = self.getval;
					var numChannels = 1;
					var bus;
					//"3whatktktj".debug;
					//val.debug("setBusMode: val");
					if(val.isSequenceableCollection) {
						numChannels = val.size;
					};
					//numChannels.debug("setBusMode: nc");
					bus = CachedBus.control(Server.default,numChannels );
						// FIXME: hardcoded server
						// hardcoded rate, but can't convert audio data to a number, so it's ok
					//bus.debug("setBusMode: bus");
					if(val.isSequenceableCollection) {
						bus.setn(val);
					} {
						bus.set(val);
					};
					//val.debug("setBusMode: val");
					//this.set(key, this.class.nestOn(bus.asMap));

					self.obj.target.source.set(selector, bus.asMap);
					self.obj.target.changed(\set, self.obj.parent.property); // we don't call Pdef.set so no changed message
				} {
					self.obj.target.source.set(selector, self.getval);
				}
			},

			inBusMode: { arg self;
				var val = self.obj.target.source.at(selector).source;
				var key = selector;
				if(val.isSequenceableCollection) {
					// multichannel
					if(val[0].isSequenceableCollection) {
						// nested
						(val[0][0].class == Symbol) and: { EventPatternProxy.symbolIsBus(val[0][0]) }
					} {
						(val[0].class == Symbol) and: { EventPatternProxy.symbolIsBus(val[0]) }
					}
				} {
					((val.class == Symbol and: { EventPatternProxy.symbolIsBus(val) }) or: { val.isKindOf(ParamCombinator) or: { this.getHalo(( \ParamCombinator_++key ).asSymbol) !? (_.inBusMode) == true } })
				}
			},

			setbus: { arg self, val;
				// TODO
				// set the bus (or map) and not its value
				self.obj.setRaw(val);
			},


			getbus: { arg self;
				// TODO
				self.obj.getRaw;
			},

			toSpec: { arg self, sp;
				sp;
			},

			property: { arg self, prop;
				prop
			},

			propertyLabel: { arg self;
				"% src".format(selector)
			},

			path: { arg self, prop;
				prop
			},

			controllerTargetCursor: { arg self;
				self.obj.target.source.at(selector).source
			},
		)
	}

	*busmode { 
		^(
			key: \busmode,
			setval: { arg self, val;
				self.obj.setVal(val);
			},

			getval: { arg self;
				self.obj.getVal;
			},

			toSpec: { arg self, sp;
				sp.asSpec;
			},

			property: { arg self, prop;
				prop
			},

			propertyLabel: { arg self;
				self.obj.property.asString;
			},

			path: { arg self, prop;
				prop
			},
		)

	}

	*nestedArray { arg idx;
		^(
			key: \nestedArray,
			setval: { arg self, val;
				var oldval;
				oldval = self.obj.parent.get;
				if(oldval.isSequenceableCollection) {
					oldval[0][idx] = val; 
					self.obj.parent.set(oldval.bubble);
				} {
					oldval[idx] = val; 
					self.obj.parent.set(oldval);
				};
			},

			getval: { arg self;
				var val;
				val = self.obj.parent.get;
				val.unbubble[idx]
			},

			toSpec: { arg self, sp;
				if(sp.isKindOf(ParamArraySpec)) {
					sp[idx]
				} {
					sp.asSpec;
				}
			},

			property: { arg self, prop;
				idx
			},

			path: { arg self;
				self.obj.parent.property -> idx;
			},
		);
	}

	*env { arg selector;
		^(
			key: \env,
			setval: { arg self, val;
				var oldval;
				oldval = self.obj.parent.get;

				oldval.perform(selector.asSetter, val);
				self.obj.parent.set(oldval);
			},

			getval: { arg self;
				var oldval;
				oldval = self.obj.parent.get;
				oldval.perform(selector)
			},

			toSpec: { arg self, sp;
				if(sp.isKindOf(ParamEnvSpec)) {
					sp.perform(selector);
				} {
					sp.asSpec;
				}
			},

			property: { arg self, prop;
				selector
			},

			path: { arg self, prop;
				prop -> selector;
			},
		);
	}
}

BaseAccessorParam : BaseParam {
	var <>accessor;
	var <>parent;
	

	// to be overriden
	//*new { arg obj, meth, sp;
		//^this.prNew(obj, meth, sp)
	//}

	*rawNew {
		^super.new;
	}

	*prNew { arg obj, meth, sp;
		var inst;
		var propertyArray = this.associationToArray(meth);
		if(propertyArray.size == 1) {
			inst = super.new.init(obj, meth, sp);
			inst.spec = inst.toSpec(sp);	// need rootspec -> rootspec
			^inst;
		} {
			var rootparam = super.new.init(obj, propertyArray[0]);
			var slotparam, acc;
			// init rootparam spec from default spec
			rootparam.spec = rootparam.toSpec(nil); // need rootspec -> rootspec
			acc = this.propertyToAccessor(propertyArray, rootparam);
			//Log(\Param).debug("BaseAccessorParam.prNew: acc %, obj %", acc, obj);
			slotparam = super.new.init(obj, meth, sp, acc);
			slotparam.spec = sp !? { acc.toSpec(sp) } ?? { acc.toSpec(rootparam.spec) }; // need slotspec -> slotspec
			slotparam.parent = rootparam;
			^slotparam;
		}
	}

	init { arg obj, meth, sp, acc;
		target = obj;
		property = meth;
		//Log(\Param).debug("BaseAccessorParam.init: target %, property %, sp %, acc %", obj, meth, sp, acc);
		accessor = acc ?? { ParamAccessor.neutral };
		accessor.obj = this;
		//Log(\Param).debug("init accessor % %".format(this, accessor));
		key = meth ? \volume;
	}

	at { arg idx, idx2;
		var acc, slotparam;
		var meth = property -> idx;
		if(idx.isNil) {
			^nil
		} {
			if(idx2.notNil) {
				meth = meth -> idx2;
			};
			acc = this.class.propertyToAccessor(this.class.associationToArray(meth), this);
			//Log(\Param).debug("now %", super);
			//this.dumpBackTrace;
			slotparam = this.class.rawNew.init(target, meth, nil, acc);
			slotparam.spec = acc.toSpec(this.spec); // need rootspec -> slotspec
			slotparam.parent = this;
			^Param.fromWrapper(slotparam);
		};
	}

	subIndex {
		^accessor.subIndex;
	}

	//*newWithAccessor { arg obj, meth, sp, acc;
		//^super.new.init(obj, meth, sp, acc)
	//}

	//accessWith { arg acc, slotspec;
		//// do not use current instance spec because it is not scalar and the slotparam spec should be scalar
		//// FIXME: with this.propertyRoot the slotparam have always Symbol property !
		//// slotparam can use accessor.path to get the full property
		//var child = this.class.newWithAccessor(target, this.propertyRoot, slotspec, acc);
		//child.parent = this;
		//^child;
	//}

	*associationToArray { arg asso;
		^if(asso.isKindOf(Association)) {
			switch(asso.key.class,
				Association, { // index of ((\adsr -> \levels) -> 0)
					[asso.key.key, asso.key.value, asso.value]
				},
				{
					[asso.key, asso.value]
				}
			);
		} {
			[asso]
		}
	}

	*propertyToAccessor { arg propertyArray, rootparam;
		//Log(\Param).debug("propertyToAccessor: propertyArray %, rootparam %", propertyArray, rootparam);
		^switch(propertyArray.size,
			3, {
				if(propertyArray[1] == \stepseq) { 
					// special property for Pbindef Pstepseq with index
					//Log(\Param).debug("propertyToAccessor: stepseq");
					ParamAccessor.stepseqitem(propertyArray[0], propertyArray[2]);
				} {
					ParamAccessor.envitem(propertyArray[1], propertyArray[2])
				}
			},
			2, {
				switch( propertyArray[1].class, 
					Symbol, { 
						var res;
						if(propertyArray[1] == \stepseq) {
							// special property for Pbindef Pstepseq
							//Log(\Param).debug("123324");
							ParamAccessor.stepseq(propertyArray[0]);
						} {
							if(propertyArray[1] == \source) {
								// special property for Pbindef source
								ParamAccessor.pbindef_source(propertyArray[0]);

							} {
								// env named segment: (\adsr -> \sustain) 
								res = switch(propertyArray[1],
									\attack, { [\times, 0] },
									\decay, { [\times, 1] },
									\sustain, { [\levels, rootparam.get.releaseNode] },
									\release, { [\times, rootparam.get.releaseNode] },
									\peak, { [\levels, 1] },
								);
								ParamAccessor.envitem(res[0], res[1]);
							}
						};
					},
					// else: an index into an array: (\delaytab -> 0)
					{ 
						ParamAccessor.array(propertyArray[1]);
					}
				)

			},
			{
				Log(\Param).error("propertyToAccessor: propertyArray should have at least 2 items");
			}
		)
	}

	//*propertyDispatcher { arg rootparam, obj, meth, rootspec, slotspec;
		//// FIXME: coeur du probleme: meth = \arr -> 0 et pourtant sp est sensé etre scalar
		//var inst;
		//if(meth.isKindOf(Association)) {
			//// handle Array and Env indexing
			//switch(meth.key.class,
				//Association, { // index of ((\adsr -> \levels) -> 0)
					//var subpro = meth.key.value;
					//var idx = meth.value;
					//var pro = meth.key.key;
					//var rootparam, slotparam;
					////"Ndef: a double asso".debug;
					////(args++[subpro, idx]).debug("NdefParamEnvSlot args");


					//// no spec given to base object because spec is for slot param
					//// but if there is no spec, this is an indication this param is envtype
					//// can be a nice to have to use this hint
					////inst = super.new.init(obj, pro).accessWith(ParamAccessor.envitem(subpro, idx), sp);
					//rootparam = rootparam ?? { super.new.init(obj, pro, rootspec) };
					//slotparam = super.new.init(obj, meth, slotspec, ParamAccessor.envitem(subpro, idx))
					//slotparam.parent = rootparam;
					////inst = rootparam.accessWith(ParamAccessor.envitem(subpro, idx), slotspec);
					////inst = rootparam.accessWith(ParamAccessor.envitem(subpro, idx), slotspec);
					//^inst
				//},
				//{
					////"Ndef: a simple asso".debug;
					//switch( meth.value.class, 
						//Symbol, { 
							//// env named segment: (\adsr -> \sustain) 
							//var res;
							//var pro = meth.key;
							//// no spec given to base object because spec is for slot param
							//inst = super.new.init(obj, pro);
							//res = switch(meth.value,
								//\attack, { [\times, 0] },
								//\decay, { [\times, 1] },
								//\sustain, { [\levels, inst.get.releaseNode] },
								//\release, { [\times, inst.get.releaseNode] },
								//\peak, { [\levels, 1] },
							//);
							//inst = inst.accessWith(ParamAccessor.envitem(res[0], res[1]), sp);
							//^inst
						//},
						//// else: an index into an array: (\delaytab -> 0)
						//{ 
							//var idx;
							//var pro;
							////"Ndef: an index into an array".debug;
							//pro = meth.key;
							//idx = meth.value;
							//Log(\Param).debug("BaseAccessorParam propertyDispatcher % % % %", obj, meth, pro, idx);
							//inst = super.new.init(obj, pro).accessWith(ParamAccessor.array(idx), sp);
						//}
					//)
				//}
			//);
		//} {
			//inst = super.new.init(obj, meth, sp);
		//};
		//^inst;
	//}

	typeLabel {
		^"P"
	}

	targetLabel {
		^target.key.asString;
	}

	// FIXME: need to decide what property should return
	// - always the root symbol, use propertyPath to get full path
	//		allow code to not break when expecting a symbol property
	// - always the local property, use propertyPath to get full path
	// - always the full path, use another method to get the local property
	property {
		//^accessor.path(this.propertyRoot);
		^this.propertyPath;
	}

	propertyRoot {
		^this.class.associationToArray(property)[0];
	}

	propertyPath {
		^accessor.path(property)
	}

	propertyLabel {
		^accessor.propertyLabel
	}

	*toSpec { arg xspec, xtarget, xproperty;
		var instr;
		var sp;
		sp =
			// Param arg
			xspec ?? {
				// halo
				//Log(\Param).debug("to Spec: 1");
				xtarget.getSpec(xproperty) ?? {
					var mysp;
				//Log(\Param).debug("2");
					// instrument metadata spec
					instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
				//Log(\Param).debug("3 % % %", xproperty, instr, Param.getSynthDefSpec(xproperty, instr));
						mysp = Param.getSynthDefSpec(xproperty, instr);
						
						// arg name in Spec
						mysp ?? {
				//Log(\Param).debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				//Log(\Param).debug("5");
								// default value in SynthDef
				//Log(\Param).debug("what %", Param.specFromDefaultValue(xproperty, instr));
								Param.specFromDefaultValue(xproperty, instr) ?? {
				//Log(\Param).debug("5.1");
									Param.defaultSpec
								}
							}
						}
					} {
						// arg name in Spec
						xproperty.asSpec ?? {
							// default value in Pdef
							var myval = xtarget.getVal(xproperty);
				//Log(\Param).debug("6");
							if(myval.notNil) {
				//Log(\Param).debug("7");
								Param.valueToSpec(myval);
							} {
								// default spec
				//Log(\Param).debug("8");
								Param.defaultSpec
							}
						}
					}
				}

			};
		^sp.asSpec;
	}

	toSpec { arg xspec;
		// accessing a slot of an array should return a param with a scalar spec
		^this.class.toSpec(xspec, target, property)
	}

	instrument {
		^target[\instrument] ?? { target.getHalo(\instrument) };
	}


	setDefaultIfNil {
		this.set(this.get); // this doesn't work if the default of target is different from Param.default
		//if(target[property].isNil) {
			//this.set(this.default);
		//};
	}

	rawDefault {

	}

	getVal {
		// this is not called by accessor, accessor always use parent.getVal
		// this means this method is always called by the top Param on the top value
		var val;
		val = target[property] ?? { 
			//this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			this.default
		};
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOff(val); 
			//Log(\Param).debug("Val unNested! %", val);
		};
		//Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		// this is not called by accessor, accessor always use parent.setVal
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOn(val); 
			//Log(\Param).debug("Val Nested! %", val);
		};
		target[property] = val;
		if(Param.trace == true) {
			"%: setVal: %".format(this, val).postln;
		};
		target.changed(\set, property, val);
	}

	get {
		^accessor.getval;
	}

	set { arg val;
		^accessor.setval(val);
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}

	setBus { arg val;
		// set the bus (or map) instead of the value of the bus
		accessor.setbus(val);
	}

	getBus {
		^accessor.getbus
	}


	getRaw {
		^target.get(property)
	}

	setRaw { arg val;
		^target.set(property, val)
	}

	getNest {
		// not sure if should return default or nil
		//^this.getRaw ?? { this.default };
		^this.getRaw;
	}

	setNest { arg val;
		this.setRaw(val);
	}

	//putListener { arg param, view, controller, action;
		//controller.put(\set, { arg ...args; 
			//action.(view, param);
		//});
	//}

	putListener { arg param, view, controller, action;
		//Log(\Param).debug("BaseAccessorParam.putListener %", param);
		controller.put(\set, { arg ...args; 
			// args: object, \set, keyval_list
			//args.debug("args");

			// update only if concerned key is set
			// do not update if no key is specified
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug

			// debug variables
			var property = param.property;
			var target = param.target;
			var spec = param.spec;

			if(Param.trace == true) {
				"%: % received update message: %".format(this, view, args).postln;
			};

			// action
			if(args[2].notNil and: {
				args[2].asSequenceableCollection.any({ arg x; x == param.propertyRoot })
			}) {
				if(Param.trace == true) {
					"%: % received update message: matching property! do update: %".format(this, view, args).postln;
				};
				action.(view, param);
			};
		});
		//controller.put(\target, { arg ...args;
			//param.refreshUpdater(view, action)
		//});
		//controller.put(\combinator, { arg ...args;
			//if(param.baseWrapper.isNil) {
				//var target = param.target;
				//param.wrapper = param.combinator.baseParam.wrapper;
				//param.baseWrapper = param.wrapper;
				//target.changed(\target);
			//} {
				////param.wrapper = param.combinator.baseParam.wrapper;

			//}
		//});
	}
}

StandardConstructorParam : BaseParam {
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		spec = this.toSpec(sp);
		key = obj.tryPerform(\key);
	}

	toSpec { arg sp;
		sp = sp ?? { target.getSpec(property) ?? { Param.defaultSpec } };
		^sp.asSpec;
	}

	normGet {
		//var val = this.get;
		^this.spec.unmap(this.get ?? {this.spec.default})
	}

	normSet { arg val;
		this.set(this.spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\set, { arg ...args; 
			// args: object, \set, keyval_list
			//args.debug("args");

			// update only if concerned key is set
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug
			if(args[2].notNil and: { args[2].isSequenceableCollection }) {
				if(args[2].any({ arg x; x == param.property })) {
					action.(view, param);
				}
			} {
				action.(view, param);
			}
		});
	}
}

////////////////// Pdef

PdefParam : BaseAccessorParam {
	var <multiParam = false;

	*new { arg obj, meth, sp;
		^this.prNew(obj, meth, sp);
	}

	//*newWithAccessor { arg obj, meth, sp, acc;
		//^super.new.init(obj, meth, sp, acc)
	//}
	
	typeLabel {
		^"P"
	}

	targetLabel {
		^target.key.asString;
	}

	*toSpec { arg xspec, xtarget, xproperty;
		var instr;
		var sp;
        //Log(\Param).debug("PdefParam.toSpec: %, %, %", xspec, xtarget, xproperty);
		sp =
			// Param arg
			xspec ?? {
				// halo
				//Log(\Param).debug("PdefParam: toSpec: 1");
				// halo.getSpec return the spec in Spec.specs if not found in halo but we want to check instrument spec before
				if(xtarget.getSpec !? { arg sp; sp.keys.includes(xproperty) } ? false) {
					xtarget.getSpec(xproperty)
				} {
					var mysp;
				//Log(\Param).debug("2");
					// instrument metadata spec
					instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
				//Log(\Param).debug("3 % % % %", xproperty, instr, Param.getSynthDefSpec(xproperty, instr));
						mysp = Param.getSynthDefSpec(xproperty, instr);
						
						// arg name in Spec
						mysp ?? {
				//Log(\Param).debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				//Log(\Param).debug("5");
								// default value in SynthDef
				//Log(\Param).debug("sfdv: %", Param.specFromDefaultValue(xproperty, instr));
								Param.specFromDefaultValue(xproperty, instr) ?? {
				//Log(\Param).debug("5.1");
									Param.defaultSpec
								}
							}
						}
					} {
						// arg name in Spec
						xproperty.asSpec ?? {
							// default value in Pdef
							var myval = xtarget.getVal(xproperty);
				//Log(\Param).debug("6");
							if(myval.notNil) {
				//Log(\Param).debug("7");
								Param.valueToSpec(myval);
							} {
								// default spec
				//Log(\Param).debug("8");
								Param.defaultSpec
							}
						}
					}
				}

			};
		^sp.asSpec;
	}

	toSpec { arg xspec;
		^this.class.toSpec(xspec, target, property);
	}

	size {
		^this.get.size; // use real value size else multichannel pbind on scalar synthparam is not seen as array
		//if(this.spec.tryPerform(\isDynamic) == true) {
			//^this.get.size
		//} {
			//^this.spec.size
		//}
	}

	setBusMode { arg enable=true, free=true;
		if(accessor[\inBusMode_].notNil) {
			accessor.inBusMode = enable;
			this.changed(\inBusMode); // FIXME: not sure if useful because on wrapper
		} {
			target.setBusMode(property, enable, free);
			this.changed(\inBusMode); // FIXME: not sure if useful because on wrapper
		}
	}

	inBusMode {
		if(accessor[\inBusMode].notNil) {
			^accessor.inBusMode;
		} {
			^target.inBusMode(property)
		}
	}

	inBusMode_ { arg val;
		if(val == true) {
			this.setBusMode(true)
		} {
			this.setBusMode(false)
		}
	}


	*instrument { arg target;
		var val;
		val = target.getHalo(\instrument) ?? { 
			var inval = target.source;
			this.getInstrumentFromPbind(inval) ?? {
				target.get(\instrument)
			};
		};
		^val;
	}

	instrument { 
		^PdefParam.instrument(target)
	}

	unset { 
		if(accessor[\unset].notNil) {
			^accessor.unset
		} {
			target.unset(property);
			target.changed(\set, [property]);
		};
	}

	isSet {
		// FIXME: should test with accessors
		// should implement it everywhere
		var propertyArray = this.class.associationToArray(this.property);
		if(propertyArray[1] == \source) {
			// Pbindef
			^this.target.source.at(this.propertyRoot).notNil
		} {
			// Pdef
			if(this.hasCombinator) {
				// we can't unset a param with a combinator
				// upstream pattern value will always be replaced by ParamCombinator
				^true
			} {
				var envir = this.target.envir;
				if(envir.isNil) {
					^false
				} {
					^envir.keys.includes(this.propertyRoot)
				}
			};
		}
	}

	getNest {
		// not sure if should return default or nil
		// nil can signal the popup that there is nothing assigned
		//^this.getRaw !? { arg v; 
			//EventPatternProxy.nestOff(v)
		//} ?? { this.default };
		^EventPatternProxy.nestOff(this.getRaw)
	}

	setNest { arg val;
		this.setRaw(EventPatternProxy.nestOn(val));
	}

	shouldBeNested { arg val;
		var instr, mysp;
		instr = PdefParam.instrument(this.target);
		if(instr.notNil) {
			//Log(\Param).debug("3 % % % %", xproperty, instr, Param.getSynthDefSpec(xproperty, instr));
			mysp = Param.toSynthDefSpec(nil, this.propertyRoot, instr);
			if(mysp.isKindOf(ParamArraySpec) or: { mysp.isKindOf(ParamEnvSpec) }) {
				^true
			}
		};
		^false
	}

	getRaw {
		if(accessor[\getRaw].notNil) {
			^accessor.getRaw;
		} {
			^target.get(property)
		}
	}

	setRaw { arg val;
		if(accessor[\setRaw].notNil) {
			^accessor.setRaw(val);
		} {
			^target.set(property, val)
		}
	}


	getVal {
		// FIXME: the bus mode is managed inside Pdef.getVal. Is it possible and desirable to use accessor for that ?
		var val;

		// no need to check for TagSpec anymore, Pdef.inBusMode check the bus format
        // this allow popupmenu with TagSpec to set a scalar param in BusMode
        // but need to check for ParamMappedBusSpec
        val = if(this.spec.isKindOf(ParamMappedBusSpec)) {
            Pdef.nestOff(target.get(property))
        } {
            target.getVal(property)
        };

		val = val ?? { 
            //this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
            this.default
        };

		//val = target.getVal(property) ?? { 
			////this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			//this.default
		//};
        
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOff(val); 
			//Log(\Param).debug("Val unNested! %", val);
		};
		//this.dumpBackTrace;
		//Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOn(val); 
			//Log(\Param).debug("Val Nested! %", val);
		};
        if(this.spec.isKindOf(ParamMappedBusSpec)) {
          //Log(\Param).debug("param.setVal nest on %", Pdef.nestOn(val));
			target.set(property, Pdef.nestOn(val));
		} {
			target.setVal(property, val);
		};

		//Log(\Param).debug("set:final Val %", val);
		if(Param.trace == true) {
			"%: setVal: %".format(this, val).postln;
		};
		//target.changed(\set, property, val); // Pdef already send a changed message
	}

	controllerTargetCursor {
		if(accessor.key == \stepseq or: { accessor.key == \stepseqitem}) {
			^accessor.controllerTargetCursor
		} {
			^nil
		}
	}

	initPstepSeq { arg size=8, forcePbindef=true;
		var getpstepseq = {
			var val;
			var def = this.default;
			if(this.propertyArray.last == \stepseq) {
				val = PstepSeq(def.extend(size, def.first))
			} {
				val = PstepSeq(def!size)
			};
			val;
		};
		if(this.target.source.isKindOf(PbindProxy)) {
			if(this.target.source.at(this.propertyRoot).source.isKindOf(PstepSeq).not) {
				this.target.source.set(this.propertyRoot, getpstepseq.())
			};
		} {
			if(forcePbindef and: { this.target.isKindOf(Pdef) }) {
				Pbindef(this.target.key, this.propertyRoot, getpstepseq.())
			};
		};
	}
}

EventPatternProxyParam : PdefParam {

	targetLabel {
		^target.hash.asString;
	}

}

PbindefParam : PdefParam {
	

}

////////////////// Pdefn

PdefnParam : PdefParam {

	*toSpec { arg xspec, xtarget, xproperty;
		var instr;
		var sp;
		sp =
			// Param arg
			xspec ?? {
				// halo
				//Log(\Param).debug("to Spec: 1");
				xtarget.getSpec(xproperty) ?? {
					var mysp;
				//Log(\Param).debug("2");
					// instrument metadata spec
					instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
				//Log(\Param).debug("3 % % %", xproperty, instr, Param.getSynthDefSpec(xproperty, instr));
						mysp = Param.getSynthDefSpec(xproperty, instr);
						
						// arg name in Spec
						mysp ?? {
				//Log(\Param).debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				//Log(\Param).debug("5");
								// default value in SynthDef
				//Log(\Param).debug("what %", Param.specFromDefaultValue(xproperty, instr));
								Param.specFromDefaultValue(xproperty, instr) ?? {
				//Log(\Param).debug("5.1");
									Param.defaultSpec
								}
							}
						}
					} {
						// arg name in Spec
						xproperty.asSpec ?? {
							// default value in Pdef
							var myval = xtarget.source;
				//Log(\Param).debug("6");
							if(myval.notNil) {
				//Log(\Param).debug("7");
								Param.valueToSpec(myval);
							} {
								// default spec
				//Log(\Param).debug("8");
								Param.defaultSpec
							}
						}
					}
				}

			};
		^sp.asSpec;
	}

	getVal {
		// FIXME: the bus mode is managed inside Pdef.getVal. Is it possible and desirable to use accessor for that ?
		var val;
		val = target.source ?? { 
			//this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			this.default
		};
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOff(val); 
			//Log(\Param).debug("Val unNested! %", val);
		};
		//this.dumpBackTrace;
		//Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOn(val); 
			//Log(\Param).debug("Val Nested! %", val);
		};
		target.source = val;
		//Log(\Param).debug("set:final Val %", val);
		if(Param.trace == true) {
			"%: setVal: %".format(this, val).postln;
		};
		//target.changed(\set, property, val); // Pdef already send a changed message
	}

	getRaw {
		^target.source
	}

	setRaw { arg val;
		^target.source = val
	}

	inBusMode {
		// TODO
		^false
	}

	inBusMode_ {
		// TODO
	}


	putListener { arg param, view, controller, action;
		var updateAction;
		//Log(\Param).debug("PdefnParam.putListener %", param);
		updateAction = { arg ...args;
			// args: object, \set, keyval_list
			//args.debug("args");

			// update only if concerned key is set
			// do not update if no key is specified
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug

			// debug variables
			var property = param.property;
			var target = param.target;
			var spec = param.spec;

			if(Param.trace == true) {
				"%: % received update message: %".format(this, view, args).postln;
			};

			// action
			if(args[2].notNil and: {
				args[2].asSequenceableCollection.any({ arg x; x == param.propertyRoot })
			}) {
				if(Param.trace == true) {
					"%: % received update message: matching property! do update: %".format(this, view, args).postln;
				};
				action.(view, param);
			};
			//param.refreshUpdater(view, action);
		};
		controller.put(\source, { arg ...args; 
			updateAction.(*args);
			if(view.isKindOf(MultiSliderView)) {
				// we just want to listen to the new source for cursor message
				// if we remap other views, StaticTextLabel become a value StaticText
				view.unmapParam;
				view.mapParam(param);
			}
		});
		controller.put(\set, { arg ...args; 
			// this is called by accessor when changing the stepseq value
			// avoid to unmap remap Param
			updateAction.(*args);
		});
		//controller.put(\target, { arg ...args;
			//param.refreshUpdater(view, action)
		//});
		//controller.put(\combinator, { arg ...args;
			//if(param.baseWrapper.isNil) {
				//var target = param.target;
				//param.wrapper = param.combinator.baseParam.wrapper;
				//param.baseWrapper = param.wrapper;
				//target.changed(\target);
			//} {
				////param.wrapper = param.combinator.baseParam.wrapper;

			//}
		//});
	}

	*associationToArray { arg asso;
		// this is a hack to trigger accessor construction by having more than one property
		if(asso == \stepseq) {
			asso = \stepseq -> nil
		};
		^if(asso.isKindOf(Association)) {
			switch(asso.key.class,
				Association, { // index of ((\adsr -> \levels) -> 0)
					[asso.key.key, asso.key.value, asso.value]
				},
				{
					[asso.key, asso.value]
				}
			);
		} {
			[asso]
		}
	}

	*propertyToAccessor { arg propertyArray, rootparam;
		// no property needed for Pdefn, \stepseq is a special property
		//Log(\Param).debug("propertyToAccessor: propertyArray %, rootparam %", propertyArray, rootparam);
		^switch(propertyArray.size,
			1, {
				if(propertyArray.first == \stepseq) {
					ParamAccessor.pdefn_stepseq
				} {
					if(propertyArray.first.isKindOf(Number)) {
						ParamAccessor.array(propertyArray.first)
					}
				}
			},
			2, {
				if(propertyArray.first == \stepseq) {
					if(propertyArray.last.isKindOf(Number)) {
						ParamAccessor.pdefn_stepseqitem(propertyArray.last)
					} {
						if(propertyArray.last.isNil) {
							ParamAccessor.pdefn_stepseq
						}
					};
				}
			},
			{
				Log(\Param).error("propertyToAccessor: propertyArray should have at least 2 items");
			}
		)
	}

	controllerTargetCursor {
		if(accessor.key == \stepseq) {
			^accessor.controllerTargetCursor
		} {
			^nil
		}
	}
}

PatternProxyParam : PdefnParam {
	
	targetLabel {
		^target.hash.asString;
	}

}


////////////////// Ndef

NdefParam : BaseAccessorParam {
	var <multiParam = false;

	*new { arg obj, meth, sp;
		^this.prNew(obj, meth, sp);
	}

	//*newWithAccessor { arg obj, meth, sp, acc;
		//^super.new.init(obj, meth, sp, acc)
	//}
	
	typeLabel {
		^"N"
	}

	unset { 
		target.unset(property);
	}

	isSet {
		// FIXME: should test with accessors
		// should implement it everywhere
		var envir = this.target.nodeMap;
		if(envir.isNil) {
			^false
		} {
			^envir.keys.includes(this.propertyRoot)
		}
	}

	targetLabel {
		^target.key.asString;
	}

	toSpec { arg sp;
		//sp.debug("sp2");
		sp = 
			// param arg
			sp ?? {
				// halo
				target.getSpec(property) ?? {
					// arg name in Spec
					property.asSpec ?? {
						// default value
						var defval = target.get(property);
						if(defval.notNil) {
							Param.valueToSpec(defval, Param.defaultSpec)
						} {
							// default spec
							Param.defaultSpec
						}
					};
				};
			};
		^sp.asSpec;
	}

	setBusMode { arg enable=true, free=true;
		target.setBusMode(property, enable, free);
		this.changed(\inBusMode);
	}

	inBusMode {
		^target.inBusMode(property)
	}

	inBusMode_ { arg val;
		if(val == true) {
			this.setBusMode(true)
		} {
			this.setBusMode(false)
		}
	}

	getVal {
		var val;
        val = if(this.inRawMode) {
			target.get(property)
		} {
			target.getVal(property)
		};
		val = val ?? { 
			//this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			this.default
		};
		if(spec.isKindOf(ParamEnvSpec)) {
			val = val.asEnv;
		};
		//this.dumpBackTrace;
		//Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		if(Param.trace == true) {
			"%: setVal: %".format(this, val.asCompileString).postln;
		};
        if(spec.isKindOf(ParamMappedBusSpec)) {
			target.set(property, val);
		} {
			target.setVal(property, val);
		};
		//Log(\Param).debug("set:final Val %, prop %", val, property);
		//target.changed(\set, property, val); // Ndef already send a set message
	}
}

NodeProxyParam : BaseAccessorParam {
	var <multiParam = false;

	*new { arg obj, meth, sp;
		^this.prNew(obj, meth, sp);
	}

	//*newWithAccessor { arg obj, meth, sp, acc;
		//^super.new.init(obj, meth, sp, acc)
	//}
	
	typeLabel {
		^"N"
	}

	unset { 
		target.unset(property);
	}

	targetLabel {
		^target.index.asString;
	}

	toSpec { arg sp;
		//sp.debug("sp2");
		sp = 
			// param arg
			sp ?? {
				// halo
				target.getSpec(property) ?? {
					// arg name in Spec
					property.asSpec ?? {
						// default value
						var defval = target.get(property);
						if(defval.notNil) {
							Param.valueToSpec(defval, Param.defaultSpec)
						} {
							// default spec
							Param.defaultSpec
						}
					};
				};
			};
		^sp.asSpec;
	}

	setBusMode { arg enable=true, free=true;
		Log(\Param).warning("setBusMode not implemented for NodeProxy");
		//target.setBusMode(property, enable, free);
		//this.changed(\inBusMode);
	}

	inBusMode {
		^target.inBusMode(property)
	}

	inBusMode_ { arg val;
		if(val == true) {
			this.setBusMode(true)
		} {
			this.setBusMode(false)
		}
	}

	getVal {
		var val;
        // FIXME: this do not use getVal and setVal, so no bus mode
        //    this make ParamMappedBusSpec works by default
		val = target.get(property) ?? { 
			//this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			this.default
		};
		if(spec.isKindOf(ParamEnvSpec)) {
			val = val.asEnv;
		};
		//this.dumpBackTrace;
		//Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		if(Param.trace == true) {
			"%: setVal: %".format(this, val.asCompileString).postln;
		};
		target.set(property, val);
		//Log(\Param).debug("set:final Val %, prop %", val, property);
		//target.changed(\set, property, val); // Ndef already send a set message
	}
}

/// Ndef vol

NdefVolParam : NdefParam {
	*new { arg obj, meth, sp;
		^super.new(obj, meth, sp);
	}

	//init { arg obj, meth, sp;
	//	target = obj;
	//	property = meth;
	//	//sp.debug("sp1");
	//	spec = this.toSpec(sp);
	//	key = obj.key;
	//}

	// retrieve default spec if no default spec given
	toSpec { arg sp;
		// FIXME: maybe add a way to .addSpec for Ndef:vol
		//ControlSpec(0,1,\lin)
		if(sp.isNil) {
			^\amp.asSpec
		} {
			^sp.asSpec;
		}
	}

	get {
		var val;
		val = target.vol;
		^val;
	}

	set { arg val;
		target.vol = val;
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}

	//putListener { arg param, view, controller, action;
	//	controller.put(\vol, { arg obj, name, volume; 
	//		// args: object, \amp, volume
	//		//args.debug("args");
	//		action.(view, param);
	//	});
	//}
}

////////////////// Node
// to control Synth (and Group ? to test)

NodeParam : BaseAccessorParam {
	var <multiParam = false; // deprecated

	*new { arg obj, meth, sp;
		^this.prNew(obj, meth, sp);
	}

	//*newWithAccessor { arg obj, meth, sp, acc;
		//^super.new.init(obj, meth, sp, acc)
	//}
	
	typeLabel {
		^"S"
	}

	unset { 
		target.unset(property);
	}

	targetLabel {
		^target.nodeID.asString;
	}

	instrument {
		target.defName;
	}

	toSpec { arg sp;
		// FIXME: use property or propertyRoot ?
		//sp.debug("sp2");
		sp = 
			// param arg
			sp ?? {
				var instr = target.tryPerform(\defName);
				var mysp;
				if(instr.notNil) {
					mysp = Param.getSynthDefSpec(property, instr);
				};
				mysp ?? {
					// halo
					target.getSpec(property) ?? {
						// arg name in Spec
						property.asSpec ?? {
							// default value
							var defval = target.get(property);
							if(defval.notNil) {
								Param.valueToSpec(defval, Param.defaultSpec)
							} {
								// default spec
								Param.defaultSpec
							}
						};
					};
				}
			};
		^sp.asSpec;
	}

	setBusMode { arg enable=true, free=true;
		// TODO
		//target.setBusMode(property, enable, free);
	}

	inBusMode {
		// TODO
		//^target.inBusMode(property)
		^false
	}


	cachedValue {
		^this.target.getHalo(\cachedValue, property);
	}

	cachedValue_ { arg val;
		this.target.addHalo(\cachedValue, property, val);
	}

	inBusMode_ { arg val;
		// TODO
		//if(val == true) {
			//this.setBusMode(true)
		//} {
			//this.setBusMode(false)
		//}
	}

	getVal {
		var val;
		if(target.isKindOf(Synth)) {
			target.get(property, { arg value;
				this.cachedValue = value;
				//target.changed(\set, property); // Synth do not use changed messages
			});
		};
		val = this.cachedValue ?? { this.default };
		if(spec.isKindOf(ParamEnvSpec)) {
			val = val.asEnv;
		};
		//this.dumpBackTrace;
		//Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		if(Param.trace == true) {
			"%: setVal: %".format(this, val.asCompileString).postln;
		};
		this.cachedValue = val;
		target.set(property, val);
		target.changed(\set, property); // Synth do not use changed messages
		//Log(\Param).debug("set:final Val %, prop %", val, property);
		//target.changed(\set, property, val); // Ndef already send a set message
	}
}

////////////////// ListPattern (PstepSeq, Pseq, Prand)

ListPatternParam : BaseAccessorParam {

	*toSpec { arg xspec, xtarget, xproperty;
		var instr;
		var sp;
		sp =
			// Param arg
			xspec ?? {
				// halo
				//Log(\Param).debug("to Spec: 1");
				xtarget.getSpec(xproperty) ?? {
					var mysp;
				//Log(\Param).debug("2");
					// instrument metadata spec
					//instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
				//Log(\Param).debug("3 % % %", xproperty, instr, Param.getSynthDefSpec(xproperty, instr));
						mysp = Param.getSynthDefSpec(xproperty, instr);
						
						// arg name in Spec
						mysp ?? {
				//Log(\Param).debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				//Log(\Param).debug("5");
								// default value in SynthDef
				//Log(\Param).debug("what %", Param.specFromDefaultValue(xproperty, instr));
								Param.specFromDefaultValue(xproperty, instr) ?? {
				//Log(\Param).debug("5.1");
									Param.defaultSpec
								}
							}
						}
					} {
						// arg name in Spec
						xproperty.asSpec ?? {
							// default value in Pdef
							var myval = xtarget.list;
				//Log(\Param).debug("6");
							if(myval.notNil) {
				//Log(\Param).debug("7");
								Param.valueToSpec(myval);
							} {
								// default spec
				//Log(\Param).debug("8");
								Param.defaultSpec
							}
						}
					}
				}

			};
		^sp.asSpec;
	}

	getVal {
		// FIXME: the bus mode is managed inside Pdef.getVal. Is it possible and desirable to use accessor for that ?
		var val;
		val = target.list ?? { 
			//this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			this.default
		};
		//this.dumpBackTrace;
		//Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		target.list = val;
		//Log(\Param).debug("set:final Val %", val);
		if(Param.trace == true) {
			"%: setVal: %".format(this, val).postln;
		};
		//target.changed(\set, property, val); // Pdef already send a changed message
	}

	getRaw {
		^target.list
	}

	setRaw { arg val;
		^target.list = val
	}

	inBusMode {
		// TODO
		^false
	}

	inBusMode_ {
		// TODO
	}


	putListener { arg param, view, controller, action;
		var updateAction;
		//Log(\Param).debug("PdefnParam.putListener %", param);
		updateAction = { arg ...args;
			// args: object, \set, keyval_list
			//args.debug("args");

			// update only if concerned key is set
			// do not update if no key is specified
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug

			// debug variables
			var property = param.property;
			var target = param.target;
			var spec = param.spec;

			if(Param.trace == true) {
				"%: % received update message: %".format(this, view, args).postln;
			};

			// action
			//if(args[2].notNil and: {
				//args[2].asSequenceableCollection.any({ arg x; x == param.propertyRoot })
			//}) {
				if(Param.trace == true) {
					"%: % received update message: matching property! do update: %".format(this, view, args).postln;
				};
				action.(view, param);
			//};
			//param.refreshUpdater(view, action);
		};
		//controller.put(\source, { arg ...args; 
			//updateAction.();
			//if(view.isKindOf(MultiSliderView)) {
				//// we just want to listen to the new source for cursor message
				//// if we remap other views, StaticTextLabel become a value StaticText
				//view.unmapParam;
				//view.mapParam(param);
			//}
		//});
		controller.put(\set, { arg ...args; 
			// this is called by accessor when changing the stepseq value
			// avoid to unmap remap Param
			updateAction.();
		});
		//controller.put(\target, { arg ...args;
			//param.refreshUpdater(view, action)
		//});
		//controller.put(\combinator, { arg ...args;
			//if(param.baseWrapper.isNil) {
				//var target = param.target;
				//param.wrapper = param.combinator.baseParam.wrapper;
				//param.baseWrapper = param.wrapper;
				//target.changed(\target);
			//} {
				////param.wrapper = param.combinator.baseParam.wrapper;

			//}
		//});
	}

	*associationToArray { arg asso;
		// this is a hack to trigger accessor construction by having more than one property
		if(asso == \stepseq) {
			asso = \stepseq -> nil
		};
		^if(asso.isKindOf(Association)) {
			switch(asso.key.class,
				Association, { // index of ((\adsr -> \levels) -> 0)
					[asso.key.key, asso.key.value, asso.value]
				},
				{
					[asso.key, asso.value]
				}
			);
		} {
			[asso]
		}
	}

	*propertyToAccessor { arg propertyArray, rootparam;
		// no property needed for Pdefn, \stepseq is a special property
		//Log(\Param).debug("propertyToAccessor: propertyArray %, rootparam %", propertyArray, rootparam);
		^switch(propertyArray.size,
			1, {
				if(propertyArray.first == \stepseq) {
					ParamAccessor.pdefn_stepseq
				} {
					if(propertyArray.first.isKindOf(Number)) {
						ParamAccessor.array(propertyArray.first)
					}
				}
			},
			2, {
				if(propertyArray.first == \stepseq) {
					if(propertyArray.last.isKindOf(Number)) {
						ParamAccessor.pdefn_stepseqitem(propertyArray.last)
					} {
						if(propertyArray.last.isNil) {
							ParamAccessor.pdefn_stepseq
						}
					};
				}
			},
			{
				Log(\Param).error("propertyToAccessor: propertyArray should have at least 2 items");
			}
		)
	}

	controllerTargetCursor {
		if(accessor.key == \stepseq) {
			^accessor.controllerTargetCursor
		} {
			^nil
		}
	}
}

////////////////// List

ListParam : BaseParam {
	*new { arg obj, meth, sp;
		// meth/property is useless
		^super.new.init(obj, meth, sp);
	}

	typeLabel {
		^"L"
	}

	targetLabel {
		^target.identityHash
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		//sp.debug("sp1");
		spec = this.toSpec(sp);
		key = obj.identityHash.asSymbol;
	}

	spec {
		^ParamArraySpec(spec ! target.size);
	}

	at { arg idx;
		^Param(target, idx, spec) // not this.spec: spec is scalar 
	}

	collect { arg fun;
		^target.collect({ arg val, x;
			fun.(this.at(x), x)
		});
	}

	do { arg fun;
		target.do({ arg val, x;
			fun.(this.at(x), x)
		});
	}


	// retrieve default spec if no default spec given
	toSpec { arg sp;
		//sp.debug("sp2");
		sp = 
			// param arg
			sp ?? {
				// halo
				target.getSpec(property) ?? {
					Param.defaultSpec;
				};
			};
		^sp.asSpec;
	}

	get {
		^target;
	}

	set { arg val;
		target.array = val;
		target.changed(\set, property, val);
	}

	normGet {
		var val = this.get;
		^this.spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(this.spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\set, { arg ...args; 
			Log(\Param).debug("ListParam: putListener: set %".format(args));
			action.(view, param);
		});
	}
}

ListParamSlot : BaseParam {
	*new { arg obj, meth, sp;
		// meth/property is index
		// spec should be scalar
		^super.new.init(obj, meth, sp);
	}

	typeLabel {
		^"L"
	}

	targetLabel {
		^target.identityHash
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		//sp.debug("sp1");
		spec = this.toSpec(sp);
		key = obj.identityHash.asSymbol;
	}

	// retrieve default spec if no default spec given
	toSpec { arg sp;
		//sp.debug("sp2");
		sp = 
			// param arg
			sp ?? {
				// halo
				target.getSpec(property) ?? {
					Param.defaultSpec;
				};
			};
		^sp.asSpec;
	}

	get {
		var val;
		val = target[property];
		if(val.isNil) {
			val = spec.default;
		};
		if(spec.isKindOf(ParamEnvSpec)) {
			val = val.asEnv;
		};
		^val;
	}

	set { arg val;
		target[property] = val;
		//Log(\Param).debug("ParamListSlot");
		//this.dumpBackTrace;
		target.changed(\set, property);
	}

	getRaw { 
		^this.get
	}

	setRaw { arg val;
		this.set(val)
	}

	normGet {
		var val = this.get;
		if(val.class == Symbol) {
			// workaround when a Bus (\c0) is mapped to the parameter
			^0
		} {
			^this.spec.unmap(this.get)
		}
	}

	normSet { arg val;
		this.set(this.spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\set, { arg ...args; 
			// TODO: update only the slot changed ?
			//Log(\Param).debug("ListParamSlot: putListener: set %".format(args));
			action.(view, param);
		});
	}
}



////////////////// Dictionary

DictionaryParam : BaseParam {
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	typeLabel {
		^"D"
	}

	targetLabel {
		^target.identityHash
	}


	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		//sp.debug("sp1");
		spec = this.toSpec(sp);
		key = meth ? \volume;
	}

	// retrieve default spec if no default spec given
	toSpec { arg sp;
		//sp.debug("sp2");
		sp = 
			// param arg
			sp ?? {
				// halo
				target.getSpec(property) ?? {
					Param.valueToSpec(this.get)
				};
			};
		^sp.asSpec;
	}

	instrument {
		^target[\instrument] ? target.getHalo(\instrument);
	}


	setDefaultIfNil {
		if(target[property].isNil) {
			this.set(this.default);
		};
	}

    isSet {
		^target[property].notNil
    }

	get {
		var val;
		val = target[property] ?? { this.default };
		if(target.getHalo(\nestMode) == true) { // FIXME: what about more granularity ?
			val = Pdef.nestOff(val); 
		};
		^val;
	}

	set { arg val;
		if(target.getHalo(\nestMode) == true) { // FIXME: what about more granularity ?
			val = Pdef.nestOn(val); 
		};
		target[property] = val;
		target.changed(\set, property, val);
	}

	at { arg idx;
		^Param(target, property -> idx, spec)
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\set, { arg ...args; 
			action.(view, param);
		});
	}
}

DictionaryParamSlot : DictionaryParam {
	var <>index;
	*new { arg obj, meth, sp, index;
		// meth/property is index
		// spec should be scalar
		^super.new(obj, meth, sp).initDictionaryParamSlot(index);
	}

	initDictionaryParamSlot { arg idx;
		index = idx;
	}

	spec {
		if(spec.isKindOf(ParamArraySpec)) {
			// FIXME: fail if imbricated ParamArraySpec, but it's not supported anyway
			^spec.at(index);
		} {
			^spec
		}
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		//sp.debug("sp1");
		spec = this.toSpec(sp);
		key = obj.identityHash.asSymbol;
	}

	get {
		var val;
		val = super.get[index];
		if(val.isNil) {
			val = spec.default;
		};
		if(spec.isKindOf(ParamEnvSpec)) {
			val = val.asEnv;
		};
		^val;
	}

	set { arg val;
		var prev = super.get;
		prev[index] = val;
		super.set(prev);
	}

	normGet {
		var val = this.get;
		if(val.class == String) {
			// workaround when a Bus ("c0") is mapped to the parameter
			^0
		} {
			^this.spec.unmap(this.get)
		}
	}

	normSet { arg val;
		this.set(this.spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\set, { arg ...args; 
			// TODO: update only the slot changed ?
			action.(view, param);
		});
	}
}

DictionaryParamEnvSlot : DictionaryParamSlot {
	// TODO
	
}

////////////////// Object property (Message)

MessageParam : BaseAccessorParam {
	*new { arg obj, meth, sp;
		^this.prNew(obj, meth, sp);
	}

	controllerTarget {
		^this.target.receiver;
	}

	targetLabel {
		^target.receiver.class
	}

	getVal {
		// this is not called by accessor, accessor always use parent.getVal
		// this means this method is always called by the top Param on the top value
		var val;
		val = this.getRaw ?? { 
			//this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			this.default
		};
		//if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			//val = Pdef.nestOff(val); 
			////Log(\Param).debug("Val unNested! %", val);
		//};
		//Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		// this is not called by accessor, accessor always use parent.setVal
		//if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			//val = Pdef.nestOn(val); 
			////Log(\Param).debug("Val Nested! %", val);
		//};
		this.setRaw(val);
		if(Param.trace == true) {
			"%: setVal: %".format(this, val).postln;
		};
		target.changed(\set, property, val);
	}

	setRaw { arg val;
		target.receiver.perform((property++"_").asSymbol, val);	
		this.controllerTarget.changed(\set, property); // FIXME: may update two times when pointed object already send changed signal
	}

	getRaw { 
		^target.receiver.perform(property);	
	}

	//putListener { arg param, view, controller, action;
		//controller.put(this.property, { arg ...args; 
			//action.(view, param);
		//});
	//}
	
	putListener { arg param, view, controller, action;
		//Log(\Param).debug("BaseAccessorParam.putListener %", param);
		controller.put(this.property, { arg ...args; 
			// args: object, \set, keyval_list
			//args.debug("args");

			// update only if concerned key is set
			// do not update if no key is specified
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug

			// debug variables
			var property = param.property;
			var target = param.target;
			var spec = param.spec;

			if(Param.trace == true) {
				"%: % received update message: %, do update".format(this, view, args).postln;
			};
			// action
			action.(view, param);

		});
	}

	instrument {
		^nil
	}

	*toSpec { arg xspec, xtarget, xproperty;
		var instr;
		var sp;
		sp =
			// Param arg
			xspec ?? {
				// halo
				//Log(\Param).debug("to Spec: 1");
				xtarget.receiver.getSpec(xproperty) ?? {
					var mysp;
				//Log(\Param).debug("2");
					// instrument metadata spec
					//instr = PdefParam.instrument(xtarget); // commented: no instr in MessageParam
					if(instr.notNil) { 
				//Log(\Param).debug("3 % % %", xproperty, instr, Param.getSynthDefSpec(xproperty, instr));
						mysp = Param.getSynthDefSpec(xproperty, instr);
						
						// arg name in Spec
						mysp ?? {
				//Log(\Param).debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				//Log(\Param).debug("5");
								// default value in SynthDef
				//Log(\Param).debug("what %", Param.specFromDefaultValue(xproperty, instr));
								Param.specFromDefaultValue(xproperty, instr) ?? {
				//Log(\Param).debug("5.1");
									Param.defaultSpec
								}
							}
						}
					} {
						// arg name in Spec
						xproperty.asSpec ?? {
							// default value in Pdef
							var myval = xtarget.receiver.perform(xproperty);
				//Log(\Param).debug("6");
							if(myval.notNil) {
				//Log(\Param).debug("7");
								Param.valueToSpec(myval);
							} {
								// default spec
				//Log(\Param).debug("8");
								Param.defaultSpec
							}
						}
					}
				}

			};
		^sp.asSpec;
	}
}

////////////////// Custom set/get

FunctionParam : StandardConstructorParam {
	var <getFunc, <setFunc;

	typeLabel {
		^"F"
	}

	targetLabel {
		^target.identityHash
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;

		getFunc = obj;
		setFunc = meth;

		spec = this.toSpec(sp);
		//key = obj.identityHash.asSymbol;
	}

	toSpec { arg sp;
		sp = sp ?? { Param.defaultSpec };
		^sp.asSpec;
	}

	set { arg val;
		setFunc.(val, this);
	}

	get { 
		^getFunc.(this);
	}
}

////////////////// Builder

BuilderParam : StandardConstructorParam {

	typeLabel {
		^"B"
	}

	targetLabel {
		^( target.key ? "" );
	}

	init { arg obj, meth, sp;
		//this.class.debug("init");
		target = obj;
		property = meth;
		spec = this.toSpec(sp);
	}

	set { arg val;
		target.set(property, val);
	}

	get { 
		^target.get(property);
	}
}

////////////////// Bus

BusParam : StandardConstructorParam {
	controllerTarget {
		^this.target;
	}

	targetLabel {
		^target.class
	}

	set { arg val;
		//target.receiver.perform((property++"_").asSymbol, val);	
		this.target.set(val);
		this.controllerTarget.changed(\set); // FIXME: may update two times when pointed object already send changed signal
	}

	get { 
		^target.getSynchronous; // could use cached bus
	}
}

//////////////////////// Various objects

////////////////// Volume

VolumeParam : BaseParam {
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	targetLabel {
		if(key == \volume or: { key == \amp }) {
			^"Vol";
		} {
			^"Vol %".format(key)
		}
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		//sp.debug("sp1");
		spec = this.toSpec(sp);
		key = meth ? \volume;
	}

	// retrieve default spec if no default spec given
	toSpec { arg sp;
		//ControlSpec(0,1,\lin)
		if(sp.isNil) {
			^\db.asSpec
		} {
			^sp.asSpec;
		}
	}

	get {
		var val;
		val = target.volume;
		^val;
	}

	set { arg val;
		target.volume = val;
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\amp, { arg obj, name, volume; 
			// args: object, \amp, volume
			//args.debug("args");
			action.(view, param);
		});
	}
}

////////////////// TempoClock
// maybe could be implemented with MessageParam

TempoClockParam : BaseParam {
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	targetLabel {
		if(key == \tempo ) {
			^"Tempo";
		} {
			^"Tempo %".format(key)
		}
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		//sp.debug("sp1");
		spec = this.toSpec(sp);
		key = meth ? \tempo;
	}

	// retrieve default spec if no default spec given
	toSpec { arg sp;
		if(sp.isNil) {
			^ControlSpec(10/60,300/60,\lin,0,1)
		} {
			^sp.asSpec;
		}
	}

	get {
		var val;
		val = target.tempo;
		^val;
	}

	set { arg val;
		target.tempo = val;
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\tempo, { arg obj, name, volume; 
			// args: object, \amp, volume
			//args.debug("args");
			action.(view, param);
		});
	}
}

////////////////////// experimental

//////////// StepEvent

StepEventParam : BaseParam {
	var <>accessor;
	var <>parent;
	
	*new { arg obj, meth, sp;
		^this.propertyDispatcher(obj, meth, sp);
	}

	*newWithAccessor { arg obj, meth, sp, acc;
		^super.new.init(obj, meth, sp, acc)
	}

	*propertyDispatcher { arg obj, meth, sp;
		var inst;
		if(meth.isKindOf(Association)) {
			// handle Array and Env indexing
			switch(meth.key.class,
				Association, { // index of ((\adsr -> \levels) -> 0)
				var subpro = meth.key.value;
				var idx = meth.value;
				var pro = meth.key.key;
				//"Ndef: a double asso".debug;
				//(args++[subpro, idx]).debug("NdefParamEnvSlot args");
				inst = super.new.init(obj, pro, sp).accessWith(ParamAccessor.envitem(subpro, idx));
				^inst
			},
			{
				//"Ndef: a simple asso".debug;
				switch(meth.value, 
					Symbol, { // env named segment: (\adsr -> \sustain) 
					// need to get spec, but spec is determined after wrapper creation :(
					Log(\Param).error("Ndef: env named segment");
					Log(\Param).error("NOT IMPLEMENTED YET!");
				},
				// else: an index into an array: (\delaytab -> 0)
				{ 
					var idx;
					var pro;
					//"Ndef: an index into an array".debug;
					pro = meth.key;
					idx = meth.value;
					//(args++[idx]).debug("NdefParamSlot args");
					inst = super.new.init(obj, pro, sp).accessWith(ParamAccessor.array(idx));
				})
			});
		} {
			inst = super.new.init(obj, meth, sp);
		};
		^inst;
	}

	accessWith { arg acc;
		var child = this.class.newWithAccessor(target, property, spec, acc);
		child.parent = this;
		^child;
	}

	typeLabel {
		^"D"
	}

	targetLabel {
		^target.identityHash
	}

	property {
		^accessor.property(property);
	}

	propertyPath {
		^accessor.path(property)
	}

	init { arg obj, meth, sp, acc;
		target = obj;
		property = meth;
		accessor = acc ?? { ParamAccessor.neutral };
		accessor.obj = this;
		//sp.debug("sp1");
		spec = accessor.toSpec(this.toSpec(sp));
		key = meth ? \volume;
	}

	// retrieve default spec if no default spec given
	toSpec { arg sp;
		//sp.debug("sp2");
		sp = sp ?? {
			// halo
			target.getSpec(property) ?? {
				var instr = this.instrument;
				var xproperty = property;
				if(instr.notNil) {
					var mysp;
					mysp = Param.getSynthDefSpec(xproperty, instr);
					mysp ?? {
						// arg name in Spec
						xproperty.asSpec ?? {
							// default value in SynthDef
							Param.specFromDefaultValue(xproperty, instr) ?? {
								Param.defaultSpec
							}
						}
					}
				} {
					var val = this.get;
					Param.valueToSpec(val)
				}
			};
		};
		^sp.asSpec;
	}

	instrument {
		^target[\instrument] ?? { target.getHalo(\instrument) };
	}


	setDefaultIfNil {
		if(target[property].isNil) {
			this.set(this.default);
		};
	}

	rawDefault {

	}

	getVal {
		var val;
		val = target[property] ?? { 
			//this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			this.default
		};
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOff(val); 
			//Log(\Param).debug("Val unNested! %", val);
		};
		//Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOn(val); 
			//Log(\Param).debug("Val Nested! %", val);
		};
		target[property] = val;
		//Log(\Param).debug("set:final Val %", val);
		if(Param.trace == true) {
			"%: setVal: %".format(this, val).postln;
		};
		target.changed(\set, property, val);
	}

	get {
		^accessor.getval;
	}

	set { arg val;
		^accessor.setval(val);
	}

	at { arg idx, idx2;
		if(idx2.notNil) {
			// at(\levels, 1)
			^Param(target, property -> idx -> idx2, spec);
		} {
			^Param(target, property -> idx, spec);
		}
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}

	putListener { arg param, view, controller, action;
		//Log(\Param).debug("BaseAccessorParam.putListener %", param);
		controller.put(\set, { arg ...args; 
			// args: object, \set, keyval_list
			//args.debug("args");

			// update only if concerned key is set
			// do not update if no key is specified
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug

			// debug variables
			var property = param.property;
			var target = param.target;
			var spec = param.spec;

			if(Param.trace == true) {
				"%: % received update message: %".format(this, view, args).postln;
			};

			// action
			if(args[2].notNil and: {
				args[2].asSequenceableCollection.any({ arg x; x == param.propertyRoot })
			}) {
				if(Param.trace == true) {
					"%: % received update message: matching property! do update: %".format(this, view, args).postln;
				};
				action.(view, param);
			};
		});
		//controller.put(\target, { arg ...args;
			//param.refreshUpdater(view, action)
		//});
		//controller.put(\combinator, { arg ...args;
			//if(param.baseWrapper.isNil) {
				//var target = param.target;
				//param.wrapper = param.combinator.baseParam.wrapper;
				//param.baseWrapper = param.wrapper;
				//target.changed(\target);
			//} {
				////param.wrapper = param.combinator.baseParam.wrapper;

			//}
		//});
	}
}

////////// seq

PbindSeqDefParam : PdefParam {
	spec {
		var si = this.get.size;
		if(si == 0) {
			^spec;
		} {
			^ParamArraySpec(spec ! si);
		}
	}
}

PbindSeqDefParamSlot : PdefParamSlot {
	//spec {
	//	^ParamArraySpec(spec ! this.get.size);
	//}

}

////////////////////// deprecated

MessageParam_old : StandardConstructorParam {

	controllerTarget {
		^this.target.receiver;
	}

	targetLabel {
		^target.receiver.class
	}

	set { arg val;
		target.receiver.perform((property++"_").asSymbol, val);	
		this.controllerTarget.changed(\set); // FIXME: may update two times when pointed object already send changed signal
	}

	get { 
		^target.receiver.perform(property);	
	}

	putListener { arg param, view, controller, action;
		controller.put(this.property, { arg ...args; 
			action.(view, param);
		});
	}
}

PdefParam_old : BaseParam {
	var <multiParam = false;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		spec = this.toSpec(sp);
		key = obj.key;
		if(spec.isKindOf(ParamArraySpec)) {
			multiParam = true;
		};
	}

	typeLabel {
		^"P"
	}

	targetLabel {
		^target.key
	}

	*instrument { arg target;
		var val;
		val = target.getHalo(\instrument) ?? { 
			var inval = target.source;
			this.getInstrumentFromPbind(inval);
		};
		^val;
	}

	instrument { 
		^PdefParam.instrument(target)
	}

	// retrieve default spec if no default spec given
	*toSpec { arg xspec, xtarget, xproperty;
		var instr;
		var sp;
		sp =
			// Param arg
			xspec ?? {
				// halo
				//Log(\Param).debug("1");
				xtarget.getSpec(xproperty) ?? {
					var mysp;
				//Log(\Param).debug("2");
					// instrument metadata spec
					instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
						mysp = Param.getSynthDefSpec(xproperty, instr);
						
						// arg name in Spec
						mysp ?? {
				//Log(\Param).debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				//Log(\Param).debug("5");
								// default value in SynthDef
								Param.specFromDefaultValue(xproperty, instr) ?? {
				//Log(\Param).debug("5.1");
									Param.defaultSpec
								}
							}
						}
					} {
						// arg name in Spec
						xproperty.asSpec ?? {
							// default value in Pdef
							var myval = xtarget.getVal(xproperty);
				//Log(\Param).debug("6");
							if(myval.notNil) {
				//Log(\Param).debug("7");
								Param.valueToSpec(myval);
							} {
								// default spec
				//Log(\Param).debug("8");
								Param.defaultSpec
							}
						}
					}
				}

			};
		^sp.asSpec;
	}

	toSpec { arg xspec;
		^this.class.toSpec(xspec, target, property)
	}
	
	setBusMode { arg enable=true, free=true;
		target.setBusMode(property, enable, free);
	}

	inBusMode {
		^target.inBusMode(property)
	}

	get {
		var val = target.getVal(property);
		// FIXME: why this seems redondant with toSpec ?
		if(val.isNil) {
			var instr = this.instrument;
			val = Param.getSynthDefDefaultValue(property, instr) ?? { spec.default };
			if(spec.isKindOf(ParamEnvSpec)) {
				val = val.asEnv;
			};
			val = val.copy;
		};
		^val;
	}

	getVal {
		^target.get(property)
	}

	set { arg val;
		target.setVal(property, val);
	}

	unset { 
		target.unset(property);
	}

	setVal { arg val;
		target.set(property, val);
	}

	normGet {
		^this.spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(this.spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\set, { arg ...args; 
			// args: object, \set, keyval_list
			//args.debug("PdefParam putListener args");

			// update only if concerned key is set
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug
			if(args[2].notNil and: {
				args[2].any({ arg x; x == param.property })
			}) {
				action.(view, param);
			}
		});
	}
}

PdefParamSlot : PdefParam { // deprecated by accessor
	var <index;

	*new { arg obj, meth, sp, index;
		var inst;
		//obj.debug("obj");
		inst = super.new(obj, meth, sp);
		inst.pdefParamSlotInit(index);
		^inst;
	}

	asLabel {
		^"P % % %".format(target.key, this.property, index)
	}

	pdefParamSlotInit { arg idx;
		index = idx;
	}

	spec {
		if(spec.isKindOf( ParamArraySpec )) {
			// FIXME: fail if imbricated ParamArraySpec, but it's not supported anyway
			^spec.at(index);
		} {
			^spec
		}
	}

	set { arg val;
		var vals = super.get;
		vals[index] = val;
		super.set(vals);
	}

	get {
		var vals = super.get;
		^vals[index];
	}
}

PdefParamEnvSlot : PdefParam {
	var <index;
	var <subproperty;

	*new { arg obj, meth, sp, subproperty, index;
		var inst;
		//obj.debug("obj");
		inst = super.new(obj, meth, sp);
		inst.pdefParamEnvSlotInit(subproperty, index);
		^inst;
	}

	asLabel {
		// masked
		^"P % % %%".format(target.key, this.property, subproperty.asString[0], index)
	}

	propertyLabel {
		^"% %%".format(this.property, subproperty.asString[0], index)
	}

	pdefParamEnvSlotInit { arg subpro, idx;
		var res;
		res = switch(subpro,
			\attack, { [\times, 0] },
			\decay, { [\times, 1] },
			\sustain, { [\levels, super.get.asEnv.releaseNode] },
			\release, { [\times, super.get.asEnv.releaseNode] },
			\peak, { [\levels, 1] },
			\times, { [\times, idx] },
			\levels, { [\levels, idx] },
			{ [subpro, idx] }
		);
		index = res[1];
		subproperty = res[0];
	}

	spec {
		if(spec.isKindOf(ParamEnvSpec)) {
			^spec.perform(subproperty).at(index);
		} {
			^spec
		}
	}

	set { arg val;
		var vals = super.get.asEnv;
		var lvals = vals.perform(subproperty);
		lvals[index] = val;
		vals.perform(subproperty.asSetter, lvals);
		super.set(vals);
	}

	get {
		var vals = super.get;
		^vals.asEnv.perform(subproperty)[index];
	}
}

NdefParam_old : BaseParam {
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	typeLabel {
		^"N"
	}

	targetLabel {
		^target.key
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		//sp.debug("sp1");
		spec = this.oSpec(sp);
		key = obj.key;
	}

	// retrieve default spec if no default spec given
	toSpec { arg sp;
		//sp.debug("sp2");
		sp = 
			// param arg
			sp ?? {
				// halo
				target.getSpec(property) ?? {
					// arg name in Spec
					property.asSpec ?? {
						// default value
						var defval = target.get(property);
						if(defval.notNil) {
							Param.valueToSpec(defval, Param.defaultSpec)
						} {
							// default spec
							Param.defaultSpec
						}
					};
				};
			};
		^sp.asSpec;
	}

	get {
		var val;
		val = target.get(property);
		if(val.isNil) {
			val = spec.default;
		};
		//if(val.class == Ndef) {
		//	// mapped to a Ndef
		//	val = 0
		//};
		if(spec.isKindOf(ParamEnvSpec)) {
			val = val.asEnv;
		};
		^val;
	}

	set { arg val;
		target.set(property, val);
		//target.changed(\set, property); // already exists in Ndef
	}

	unset { 
		target.unset(property);
	}

	normGet {
		var val = this.get;
		if(val.class == Symbol) {
			// workaround when a Bus ("c0") is mapped to the parameter
			^0
		} {
			^this.spec.unmap(val)
		}
	}

	normSet { arg val;
		this.set(this.spec.map(val))
	}

	at { arg idx;
		^Param(target, property -> idx, spec)
	}

	putListener { arg param, view, controller, action;
		controller.put(\set, { arg ...args; 
			// args: object, \set, keyval_list
			//args.debug("args");

			// update only if concerned key is set
			// do not update if no key is specified
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug

			// debug variables
			var property = param.property;
			var target = param.target;
			var spec = param.spec;

			// action
			if(args[2].notNil and: {
				args[2].any({ arg x; x == param.property })
			}) {
				action.(view, param);
			};
		});
	}
}

NdefParamSlot : NdefParam {
	var <index;

	*new { arg obj, meth, sp, index;
		var inst;
		//obj.debug("obj");
		inst = super.new(obj, meth, sp);
		inst.ndefParamSlotInit(index);
		^inst;
	}

	asLabel {
		^"N % % %".format(target.key, this.property, index)
	}

	ndefParamSlotInit { arg idx;
		index = idx;
	}

	spec {
		if(spec.isKindOf(ParamArraySpec)) {
			^spec.at(index);
		} {
			^spec
		}
	}

	set { arg val;
		var vals = super.get.copy;
		vals[index] = val;
		super.set(vals);
	}

	get {
		var vals = super.get;
		^vals[index];
	}

	//normGet {
	//	var val = this.get;
	//	if(val.class == String) {
	//		// workaround when a Bus ("c0") is mapped to the parameter
	//		^0
	//	} {
	//		^this.spec.unmap(this.get)
	//	}
	//}

	//normSet { arg val;
	//	this.set(this.spec.map(val))
	//}
}

NdefParamEnvSlot : NdefParam {
	var <index;
	var <subproperty;

	*new { arg obj, meth, sp, subproperty, index;
		var inst;
		//obj.debug("obj");
		inst = super.new(obj, meth, sp);
		inst.ndefParamEnvSlotInit(subproperty, index);
		^inst;
	}

	asLabel {
		// not used, Param asLabel call fullLabel which call propertyLabel
		^"N % % %%".format(target.key, this.property, subproperty.asString[0], index)
	}

	propertyLabel {
		^"% %%".format(this.property, subproperty.asString[0], index)
	}


	ndefParamEnvSlotInit { arg subpro, idx;
		var res;
		res = switch(subpro,
			\attack, { [\times, 0] },
			\decay, { [\times, 1] },
			\sustain, { [\levels, super.get.asEnv.releaseNode] },
			\release, { [\times, super.get.asEnv.releaseNode] },
			\peak, { [\levels, 1] },
			\times, { [\times, idx] },
			\levels, { [\levels, idx] },
			{ [subpro, idx] }
		);
		index = res[1];
		subproperty = res[0];
	}

	spec {
		if(spec.isKindOf(ParamEnvSpec)) {
			^spec.perform(subproperty).at(index);
		} {
			^spec
		}
	}

	set { arg val;
		var vals = super.get.asEnv;
		var lvals = vals.perform(subproperty);
		lvals[index] = val;
		vals.perform(subproperty.asSetter, lvals);
		super.set(vals);
	}

	get {
		var vals = super.get.asEnv;
		^vals.perform(subproperty)[index];
	}
}

//EventPatternProxyParam : StepEventParam {

	//unset {
		//target.set(property, nil)
	//}

	//isSet {
		//^target.envir.notNil and: {
			//target.envir.includes(property)
		//}
	//}

	//setBusMode { arg enable=true, free=true;
		//target.setBusMode(property, enable, free);
	//}

	//inBusMode {
		//^target.inBusMode(property)
	//}

	//inBusMode_ { arg val;
		//if(val == true) {
			//this.setBusMode(true)
		//} {
			//this.setBusMode(false)
		//}
	//}


	//instrument {
		//// before setting any key, the .envir is nil and .get(\instrument) return nil
		//// after, it return \default even if \instrument is not defined (default event)
		//// so if result is \default, i still check halo instrument to be sure
		//var res = target.get(\instrument);
		//if(res == \default) {
			//if(target.getHalo(\instrument).notNil) {
				//res = target.getHalo(\instrument)
			//}
		//};
		//^res ?? { 
			//target.getHalo(\instrument) ?? {
				//(target !? { this.class.getInstrumentFromPbind(target.source) }) ? \default;
			//};
		//}
	//}

	//getVal {
		//var val;
		//val = target.getVal(property) ?? { 
			////this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			//this.default
		//};
		//if(target.getHalo(\nestMode) == true) { // FIXME: what about more granularity ?
			//val = Pdef.nestOff(val); 
			//Log(\Param).debug("Val unNested! %", val);
		//};
		//// FIXME: this function is called four times, why ? maybe one for each widget so it's normal
		////Log(\Param).debug("get:final Val %", val); 
		//^val;
	//}

	//setVal { arg val;
		//if(target.getHalo(\nestMode) == true) { // FIXME: what about more granularity ?
			//val = Pdef.nestOn(val); 
			//Log(\Param).debug("Val Nested! %", val);
		//};
		//if(Param.trace == true) {
			//"%: setVal: %".format(this, val).postln;
		//};
		//target.setVal(property, val);
		////Log(\Param).debug("set:final Val %", val);
		//target.changed(\set, property, val);
	//}
//}
