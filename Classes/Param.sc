
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
		if (args.size > 1) {
			this.newWrapper(args)
		} {
			// TODO: test without this code to see if something break
			if(args[0].isSequenceableCollection) {
				// This is for support Param([Ndef(\plop), \freq, \freq])
				// this support is stupid, why not Param(*[Ndef(\plop), \freq, \freq]) ???
				this.newWrapper(args[0])
			} {
				// This is for support Param(ParamValue.new)
				// This break API for why ? to not use an extra wrapper
				this.newWrapper(args)
			}
		};
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
					Log(\Param).debug("property_dispatcher: env % %", envclass, args++[subpro, idx]);
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
							Log(\Param).debug("property_dispatcher: simple env % %", envclass, args++[property.value]);
							wrapper = envclass.new(*args++[property.value])
						},
						// else: an index into an array: (\delaytab -> 0)
						{ 
							var idx;
							//"Ndef: an index into an array".debug;
							args[1] = property.key;
							idx = property.value;
							//(args++[idx]).debug("NdefParamSlot args");
							Log(\Param).debug("property_dispatcher: array % %", arrayclass, args++[idx]);
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
				Log(\Param).error("ERROR: not implemented for Array, use List instead");
				^nil;
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
		^( this.wrapper.property == param.wrapper.property)
		and: { this.wrapper.target == param.wrapper.target } 
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
		wrapper.inBusMode_(*args)
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

	spec {
		^wrapper.spec;
	}

	spec_ { arg val; // not sure if safe
		wrapper.spec = val;
	}

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

	clone {
		^this.class.new(this.target, this.property);
	}

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

	///////// list behavior

	size { 
		^wrapper.size;
	}

	at { arg idx;
		// current implementation is: wrapper.at should return a Param, not a wrapper
		^wrapper.at(idx);
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

	// FIXME: ambigous name, maybe rename to midiMap. Also map mean something different for the wrapper class

	map { arg msgNum, chan, msgType=\control, srcID, blockmode;
		MIDIMap(this, msgNum, chan, msgType, srcID, blockmode);
	}

	unmap { arg msgNum, chan, msgType, srcID, blockmode;
		MIDIMap.free(msgNum, chan, msgType, srcID, blockmode);
	}

	/////////////////// GUI mapping

	makeSimpleController { arg slider, action, updateAction, initAction, customAction, cursorAction;
		var param = this;
		slider.toolTip = this.fullLabel; // FIXME: not really a good place, but so it can quicly apply to every view

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
		var con = view.getHalo(\simpleController);
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
			var controller;
			controller = SimpleController(param.controllerTarget);
			{
				view.onClose = view.onClose.addFunc( { this.class.freeUpdater(view) } );
			}.defer;
			view.addHalo(\simpleController, controller);
			simpleControllers.add(controller);

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
		this.wrapper.putListener(this, view, controller, action);
	}

	makeListener { arg action, obj; 
		// helper method for external to listen to param value changes
		// WARNING: caller is responsible for freeing the controller !
		// action.(obj, param)
		var cont;
		cont = SimpleController.new(this.controllerTarget);
		this.class.userSimpleControllers = this.class.userSimpleControllers.add(cont);
		this.putListener(obj, cont, action);
		^cont
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


	////// widgets

	mapMultiSlider { arg slider, action, trackCursor=false;
		var cursorAction;
		if(trackCursor) {
			cursorAction = { arg self, index;
				defer {
					self.index = index;
				}
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
		slider.addUniqueMethod(\attachOverlayMenu, {
			slider.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
				//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
				if(buttonNumber == 1) {
					ParamProto.init;
					WindowDef(\OverlayMenu).front(slider, x, y, { arg def;
						Param(Message(slider), \size, ControlSpec(1,32,\lin,1,4)).asNumberBox.maxWidth_(100)
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
				//"start executing".debug;
				try {
					val = param.normGet;
				} { arg error;
					"In: %.mapSlider:updateAction".format(param).error;
					error.reportError;
					if(error.errorString.contains("Message 'round'")) {
						"ERROR: Param spec (%) expected a number but received %".format(this.spec, try{ this.getRaw.asCompileString }{ arg error; error.errorString }).postln;
					};
					//error.throw;
				};
				if(val.isKindOf(Number)) {
					{
						self.value = val;
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
		this.makeSimpleController(view, 
			action: { arg view, param;
				param.set(view.value.interpret);
				//"done".debug;
			}, 
			updateAction: { arg view, param;
				var val = "";
				Log(\Param).debug("mapTextField start");
				try {
					//[param, param.stringGet(precision)].debug("Param.mapTextField:get");
					val = param.stringGet(precision);
					//val = param.get.asCompileString;
				} { arg error;
					Log(\Param).debug("param.get %", param.get);
					val = param.get.asCompileString;
				};
				Log(\Param).debug("mapTextField: val: %", val);
				// refresh action
				{
					//[val.asCompileString, view.hasFocus].debug("Param.mapTextField: hasfocus");
					if(view.hasFocus.not) {
						// TODO: handle other types than float
							view.value = val;
					};
					Log(\Param).debug("mapTextField: updateAction: end");
					//"done".debug;
				}.defer;
			},
			initAction: nil,
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
		// TODO: for refactoring menu method
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
					if(this.spec.isKindOf(ParamMappedControlBusSpec)) {
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
		view.onChange(this.controllerTarget, \set, { arg aview, model, message, arg1;
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
		if( keys.isKindOf(TagSpecDef)) {
			view.onChange(keys, \list, onChange ? { arg aview, model, message, arg1;
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
		view.onChange(this.controllerTarget, \set, { arg aview, model, message, arg1;
			// TODO: do not change the whole row when just one value is updated!
			//[view, me, arg1, arg2, arg3].value.debug("mapValuePopUpMenu:6 (onchange)");
			if(arg1 == this.propertyRoot or: { arg1.isKindOf(SequenceableCollection) and: {
				arg1.includes(this.propertyRoot)
			} }) {
				aview.refreshChange;
			};
			//view.value.debug("mapValuePopUpMenu:7 (onchange)");
		});
		mykeys = keys ?? {this.spec};
		if( mykeys.isKindOf(TagSpecDef)) {
			view.onChange(mykeys, \list, { arg aview, model, message, arg1;
				aview.refreshChange;
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

		view.refreshChangeAction = {
			var spec;
			var val;
			//var isMapped = false;
			//[ this.spec.labelList.asArray, this.get, this.spec.unmapIndex(this.get)].debug("spec, get, unmap");
			if(keys.notNil) {
				spec = keys;
				val = this.get;
			} {
				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					if(this.spec.isKindOf(ParamMappedControlBusSpec)) {
						val = this.getBus;
					} {
						val = this.get;
					};
					spec = this.spec.tagSpec;
				} {
					val = this.get;
					spec = this.spec;
				};
			};
			try {
				if(spec.labelList.notNil) {
					view.items = spec.labelList.asArray;
				};
				view.value = spec.unmapIndex(val);
			} { arg error;
				"In %.mapValuePopUpMenu:refreshChangeAction".format(this).error;
				error.reportError;
			};
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
					if(this.spec.isKindOf(ParamMappedControlBusSpec)) {
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
		//[view, this.controllerTarget].value.debug("mapValuePopUpMenu:3.5");
		view.onChange(this.controllerTarget, \set, { arg aview, model, message, arg1;
			// TODO: do not change the whole row when just one value is updated!
			//[view, me, arg1, arg2, arg3].value.debug("mapValuePopUpMenu:6 (onchange)");
			if(arg1 == this.propertyRoot or: { arg1.isKindOf(SequenceableCollection) and: {
				arg1.includes(this.propertyRoot)
			} }) {
				aview.refreshChange;
			};
			//view.value.debug("mapValuePopUpMenu:7 (onchange)");
		});
		mykeys = keys ?? {this.spec};
		if( mykeys.isKindOf(TagSpecDef)) {
			view.onChange(mykeys, \list, { arg aview, model, message, arg1;
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
		Log(\Param).debug("mapBusPopUpMenu %", keys);
		if(keys.notNil and: { keys.isKindOf(TagSpec).not }) {
			keys = TagSpec(keys);
		};
		Log(\Param).debug("mapBusPopUpMenu 0.1 %", keys);

		view.refreshChangeAction = {
			var xspec;
			//[ this.spec.labelList.asArray, this.get, this.spec.unmapIndex(this.get)].debug("spec, get, unmap");
			Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:1 spec %, keys %", this.spec, keys);
			if(keys.notNil) {
				xspec = keys;
				Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:2 spec %, keys %", xspec, keys);
			} {

				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					xspec = this.spec.tagSpec;
					Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:3 spec %, keys %", xspec, keys);
				} {
					xspec = this.spec;
					Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:4 spec %, keys %", xspec, keys);
				};
			};
			{
				try {
					if(xspec.labelList.notNil) {
						view.items = xspec.labelList.asArray;
					};
					view.value = xspec.unmapIndex(this.getBus);
				} { arg error;
					"In %.mapBusPopUpMenu:refreshChangeAction".format(this).error;
					error.reportError;
					//error.throw;
				};
			Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:5 spec %, keys %", xspec, keys);
			Log(\Param).debug("mapBusPopUpMenu:refreshChangeAction:6 spec %, keys %", xspec, keys);
			}.defer;
			//view.value.debug("mapValuePopUpMenu:1.5");
		};
		view.refreshChange;
		//[this.spec, this.get].debug("mapValuePopUpMenu:2");
		//view.value.debug("mapValuePopUpMenu:3");
		view.action = {
			var xspec;
			Log(\Param).debug("mapBusPopUpMenu:action:1: view.value %", view.value);
			Log(\Param).debug("mapBusPopUpMenu:action:2 spec %, keys %", this.spec, keys);
			if(keys.notNil) {
				xspec = keys;
				Log(\Param).debug("mapBusPopUpMenu:action:3 spec %, keys %", xspec, keys);
			} {
				if(this.spec.isKindOf(ParamBusSpec) or: { this.spec.isKindOf(ParamBufferSpec) }) {
					xspec = this.spec.tagSpec;
					Log(\Param).debug("mapBusPopUpMenu:action:4 spec %, keys %", xspec, keys);
				} {
					xspec = this.spec;
					Log(\Param).debug("mapBusPopUpMenu:action:5 spec %, keys %", xspec, keys);
				};
			};
			Log(\Param).debug("mapBusPopUpMenu:action:6 spec %, keys %", xspec, keys);
			this.setBus(xspec.mapIndex(view.value));
			Log(\Param).debug("mapBusPopUpMenu:action: end");
		};
		//[view, this.controllerTarget].value.debug("mapValuePopUpMenu:3.5");
		view.onChange(this.controllerTarget, \set, { arg aview, model, message, arg1;
			// TODO: do not change the whole row when just one value is updated!
			Log(\Param).debug("mapBusPopUpMenu:onChange: prop:% view:% model:% msg:% arg:%", this.propertyRoot, aview, model, message, arg1);
			if(arg1 == this.propertyRoot or: { arg1.isKindOf(SequenceableCollection) and: {
				arg1.includes(this.propertyRoot)
			} }) {
				Log(\Param).debug("mapBusPopUpMenu:onChange: going to refresh");
				aview.refreshChange;
			};
			{
				Log(\Param).debug("mapBusPopUpMenu:onchange: end", view.value);
			}.defer;
		});
		mykeys = keys ?? {this.spec};
		if( mykeys.isKindOf(TagSpecDef)) {
			view.onChange(mykeys, \list, { arg aview, model, message, arg1;
				aview.refreshChange;
			});
		}
		//view.value.debug("mapValuePopUpMenu:8");
	}

	unmapPopUpMenu { arg view;
		// FIXME: mapIndexPopUpMenu does not use updater
		this.freeUpdater(view);
	}

	mapButton { arg view, action;
		this.makeSimpleController(view, { arg view, param;
			var size;
			size = view.states.size;
			param.normSet(view.value.linlin(0,size-1,0,1));
		}, { arg view, param;
			var size;
			{
				size = view.states.size;
				view.value = param.normGet.linlin(0,1,0,size-1);
			}.defer
		}, nil, action)
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
		^Knob.new.mapParam(this);
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
		^StaticText.new.mapParamLabel(this, labelmode);
	}

	asTextField {
		^TextField.new.mapParam(this);
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

	asButton { arg label = "";
		var but = Button.new
			.states_([
				[label, Color.black, Color.white],
				[label, Color.black, ParamViewToolBox.color_ligth],
			]);
		but.mapParam(this);
		^but;
	}

	asPopUpMenu { arg keys;
		^PopUpMenu.new.mapParam(this, keys) // is Param.mapPopUpMenu
	}

	asValuePopUpMenu { arg keys;
		^PopUpMenu.new.mapValueParam(this, keys) // is Param.mapValuePopUpMenu
	}

	asBusPopUpMenu { arg keys;
		Log(\Param).debug("asBusPopUpMenu %", keys);
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
			if( argName.asSpec.notNil, {
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

	*getSynthDefSpec { arg argName, defname=nil;
		var val;
		var rval;
		val = SynthDescLib.global.synthDescs[defname];
		//Log(\Param).debug("synthdesc % % %", argName, defname, val);
		if(val.notNil) {
			val = val.metadata;
			if(val.notNil) {
				val = val.specs;
				if(val.notNil) {
					rval = val[argName];
				}
			}
		};
		^rval;
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

	*toSynthDefSpec { arg spec, argName, defname=nil, default_spec=\widefreq;
		if(spec.isNil) {
			var val;
			var rval;
			val = SynthDescLib.global.synthDescs[defname];
			if(val.notNil) {
				val = val.metadata;
				if(val.notNil) {
					val = val.specs;
					if(val.notNil) {
						rval = val[argName];
					}
				} {
					// no metadata but maybe a default value
					var def = Param.getSynthDefDefaultValue(argName, defname);
					if(def.class == Float) {
						rval = default_spec;
					} {
						if(def.class.isSequenceableCollection) {
							rval = ParamArraySpec(default_spec!def.size);
						}
					};
				};
			};
			rval = rval.asSpec;
			spec = this.toSpec(spec, argName, default_spec);
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


BaseParam {
	var <target, <property, <>spec, <key;
	var >shortLabel; // deprecated
	var >combinator;
	var <>combinatorEnabled = true;
	var <>labelmode;
	var >default;
	var >label;  // why not in BaseParam ?

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

	propertyLabel {
		^property.asString
	}

	propertyRoot {
		^property
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
		if(( alabelmode ? labelmode ) == \full) {
			^this.fullLabel
		} {
			^this.propertyLabel
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

	instrument { nil }

	default {
		var instr;
		var val;
		if(default.notNil) {
			^default;
		};
		instr = this.instrument;
		if(instr.notNil) {
			val = Param.getSynthDefDefaultValue(property, instr) ?? { spec !? _.default ? 0 };
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
}

ParamAccessor {
	*neutral { 
		^(
			key: \neutral,
			setval: { arg self, val;
				self.obj.setVal(val);
			},

			getval: { arg self;
				self.obj.getVal;
			},

			setbus: { arg self, val;
				// set the bus (or map) and not its value
				self.obj.setRaw(val);
			},


			getbus: { arg self;
				self.obj.getRaw;
			},

			toSpec: { arg self, sp;
				sp
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
				Log(\Param).debug("euhh1");
				val = self.obj.parent.getNest;
				Log(\Param).debug("euhh10 %", val);
				val !? {
				Log(\Param).debug("euhh11 %", val);
					val[idx];
				}
			},

			getval: { arg self;
				var val;
				val = self.obj.parent.get;
				val[idx]
			},

			toSpec: { arg self, sp;
				if(sp.isKindOf(ParamArraySpec)) {
					sp[idx]
				} {
					sp
				}
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
					sp
				}
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

	// WIP

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
				sp
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
					sp
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
					sp
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
			rootparam.spec = rootparam.toSpec(nil); // need rootspec -> rootspec
			acc = this.propertyToAccessor(propertyArray, rootparam);
			slotparam = super.new.init(obj, meth, sp, acc);
			slotparam.spec = sp ?? { acc.toSpec(rootparam.spec) }; // need slotspec -> slotspec
			slotparam.parent = rootparam;
			^slotparam;
		}
	}

	init { arg obj, meth, sp, acc;
		target = obj;
		property = meth;
		accessor = acc ?? { ParamAccessor.neutral };
		accessor.obj = this;
		//Log(\Param).debug("init accessor % %".format(this, accessor));
		key = meth ? \volume;
	}

	at { arg idx, idx2;
		var acc, slotparam;
		var meth = property -> idx;
		if(idx2.notNil) {
			meth = meth -> idx2;
		};
		acc = this.class.propertyToAccessor([property, idx, idx2].select(_.notNil), this);
		//Log(\Param).debug("now %", super);
		//this.dumpBackTrace;
		slotparam = this.class.rawNew.init(target, meth, nil, acc);
		slotparam.spec = acc.toSpec(this.spec); // need rootspec -> slotspec
		slotparam.parent = this;
		^Param.fromWrapper(slotparam);
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
		^switch(propertyArray.size,
			3, {
				ParamAccessor.envitem(propertyArray[1], propertyArray[2])
			},
			2, {
				switch( propertyArray[1].class, 
					Symbol, { 
						// env named segment: (\adsr -> \sustain) 
						var res;
						res = switch(propertyArray[1],
							\attack, { [\times, 0] },
							\decay, { [\times, 1] },
							\sustain, { [\levels, rootparam.get.releaseNode] },
							\release, { [\times, rootparam.get.releaseNode] },
							\peak, { [\levels, 1] },
						);
						ParamAccessor.envitem(res[0], res[1]);
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
				Log(\Param).debug("to Spec: 1");
				xtarget.getSpec(xproperty) ?? {
					var mysp;
				Log(\Param).debug("2");
					// instrument metadata spec
					instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
				Log(\Param).debug("3 % % %", xproperty, instr, Param.getSynthDefSpec(xproperty, instr));
						mysp = Param.getSynthDefSpec(xproperty, instr);
						
						// arg name in Spec
						mysp ?? {
				Log(\Param).debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				Log(\Param).debug("5");
								// default value in SynthDef
				Log(\Param).debug("what %", Param.specFromDefaultValue(xproperty, instr));
								Param.specFromDefaultValue(xproperty, instr) ?? {
				Log(\Param).debug("5.1");
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
		var val;
		val = target[property] ?? { 
			//this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			this.default
		};
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOff(val); 
			Log(\Param).debug("Val unNested! %", val);
		};
		Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		// this is not called by accessor, accessor always use parent.setVal
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOn(val); 
			Log(\Param).debug("Val Nested! %", val);
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
		var val = this.get;
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
		sp =
			// Param arg
			xspec ?? {
				// halo
				Log(\Param).debug("PdefParam: toSpec: 1");
				xtarget.getSpec(xproperty) ?? {
					var mysp;
				Log(\Param).debug("2");
					// instrument metadata spec
					instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
				Log(\Param).debug("3 % % % %", xproperty, instr, Param.getSynthDefSpec(xproperty, instr));
						mysp = Param.getSynthDefSpec(xproperty, instr);
						
						// arg name in Spec
						mysp ?? {
				Log(\Param).debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				Log(\Param).debug("5");
								// default value in SynthDef
				Log(\Param).debug("sfdv: %", Param.specFromDefaultValue(xproperty, instr));
								Param.specFromDefaultValue(xproperty, instr) ?? {
				Log(\Param).debug("5.1");
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

	setBusMode { arg enable=true, free=true;
		target.setBusMode(property, enable, free);
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

	unset { 
		target.unset(property);
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


	getVal {
		// FIXME: the bus mode is managed inside Pdef.getVal. Is it possible and desirable to use accessor for that ?
		var val;
		val = target.getVal(property) ?? { 
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
		target.setVal(property, val);
		//Log(\Param).debug("set:final Val %", val);
		if(Param.trace == true) {
			"%: setVal: %".format(this, val).postln;
		};
		//target.changed(\set, property, val); // Pdef already send a changed message
	}
}

EventPatternProxyParam : PdefParam {

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
		val = target.getVal(property) ?? { 
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
		target.setVal(property, val);
		//Log(\Param).debug("set:final Val %, prop %", val, property);
		//target.changed(\set, property, val); // Ndef already send a set message
	}
}

////////////////// Ndef vol

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

NodeParam : BaseAccessorParam {
	var <multiParam = false;

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
		val = this.cachedValue;
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
		var val;
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
			action.(view, param);
		});
	}
}

///////////

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

//////////////////// StepEvent

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
			Log(\Param).debug("Val unNested! %", val);
		};
		Log(\Param).debug("get:final Val %", val);
		^val;
	}

	setVal { arg val;
		if(target.getHalo(\nestMode) != false) { // FIXME: what about more granularity ?
			val = Pdef.nestOn(val); 
			Log(\Param).debug("Val Nested! %", val);
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
		controller.put(\set, { arg ...args; 
			action.(view, param);
		});
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

////////////////// Object property (message)
MessageParam : StandardConstructorParam {

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
}

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

////////////////////// deprecated


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
				Log(\Param).debug("1");
				xtarget.getSpec(xproperty) ?? {
					var mysp;
				Log(\Param).debug("2");
					// instrument metadata spec
					instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
						mysp = Param.getSynthDefSpec(xproperty, instr);
						
						// arg name in Spec
						mysp ?? {
				Log(\Param).debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				Log(\Param).debug("5");
								// default value in SynthDef
								Param.specFromDefaultValue(xproperty, instr) ?? {
				Log(\Param).debug("5.1");
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

PdefParamSlot : PdefParam {
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

