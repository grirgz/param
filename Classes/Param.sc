
Param {
	var <wrapper;
	var init_args;
	var >label;  // why not in BaseParam ?
	classvar <>defaultSpec;
	classvar <>simpleControllers;
	classvar <>userSimpleControllers;
	classvar <>defaultUpdateMode = \dependants;
	classvar <>defaultPollRate = 0.2;
	classvar <>editFunction;
	classvar <>lastTweaked;

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

		envdict = (
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
					wrapper = envclass.new(*args++[subpro, idx]);
				},
				{
					//"Ndef: a simple asso".debug;
					switch(property.value, 
						Symbol, { // env named segment: (\adsr -> \sustain) 
							// need to get spec, but spec is determined after wrapper creation :(
							Log(\Param).error("Ndef: env named segment");
							Log(\Param).error("NOT IMPLEMENTED YET!");
						},
						// else: an index into an array: (\delaytab -> 0)
						{ 
							var idx;
							//"Ndef: an index into an array".debug;
							args[1] = property.key;
							idx = property.value;
							//(args++[idx]).debug("NdefParamSlot args");
							wrapper = arrayclass.new(*args++[idx]);
						}

					)
				}
			);
		};

		//"hello3 gangenr".debug;

		class_dispatcher = (
			Ndef: {
				switch(property.class,
					Association, {
						//"Ndef: an asso".debug;
						property_dispatcher.(property, NdefParamSlot, NdefParamEnvSlot);
						//property_dispatcher.(property, NdefParam);
						//wrapper = NdefParam(*args);
						//switch(property.key.class,
						//	Association, { // index of ((\adsr -> \levels) -> 0)
						//		var subpro = property.key.value;
						//		var idx = property.value;
						//		//"Ndef: a double asso".debug;
						//		args[1] = property.key.key;
						//		//(args++[subpro, idx]).debug("NdefParamEnvSlot args");
						//		wrapper = NdefParamEnvSlot(*args++[subpro, idx]);
						//	},
						//	{
						//		//"Ndef: a simple asso".debug;
						//		switch(property.value, 
						//			Symbol, { // env named segment: (\adsr -> \sustain) 
						//				// need to get spec, but spec is determined after wrapper creation :(
						//				"Ndef: env named segment".debug;
						//				"NOT IMPLEMENTED YET!".debug;
						//			},
						//			// else: an index into an array: (\delaytab -> 0)
						//			{ 
						//				var idx;
						//				//"Ndef: an index into an array".debug;
						//				args[1] = property.key;
						//				idx = property.value;
						//				//(args++[idx]).debug("NdefParamSlot args");
						//				wrapper = NdefParamSlot(*args++[idx]);
						//			}

						//		)
						//	}
						//);
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
				switch(property.class,
					Association, {
						property_dispatcher.(property, PdefParamSlot, PdefParamEnvSlot);
						//switch(property.key.class,
						//	Association, {
						//		var subpro = property.key.value;
						//		var idx = property.value;
						//		args[1] = property.key.key;
						//		//(args++[subpro, idx]).debug("PdefParamEnvSlot args");
						//		wrapper = PdefParamEnvSlot(*args++[subpro, idx]);
						//	},
						//	// else: an index
						//	{
						//		var idx;
						//		args[1] = property.key;
						//		idx = property.value;
						//		//(args++[idx]).debug("PdefParamSlot args");
						//		wrapper = PdefParamSlot(*args++[idx]);
						//	}
						//);
					},
					Symbol, {
						wrapper = PdefParam(*args);
					}
				);
			}, 
			PbindSeqDef: {
				switch(property.class,
					Association, {
						property_dispatcher.(property, PdefParamSlot, PdefParamEnvSlot);

						//switch(property.key.class,
						//	Association, {
						//		var subpro = property.key.value;
						//		var idx = property.value;
						//		args[1] = property.key.key;
						//		//(args++[subpro, idx]).debug("PdefParamEnvSlot args");
						//		wrapper = PdefParamEnvSlot(*args++[subpro, idx]);
						//	},
						//	// else: an index
						//	{
						//		var idx;
						//		args[1] = property.key;
						//		idx = property.value;
						//		//(args++[idx]).debug("PdefParamSlot args");
						//		wrapper = PdefParamSlot(*args++[idx]);
						//	}
						//);
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

						//switch(property.key.class,
						//	Association, {
						//		var subpro = property.key.value;
						//		var idx = property.value;
						//		args[1] = property.key.key;
						//		//(args++[subpro, idx]).debug("PdefParamEnvSlot args");
						//		wrapper = PdefParamEnvSlot(*args++[subpro, idx]);
						//	},
						//	// else: an index
						//	{
						//		var idx;
						//		args[1] = property.key;
						//		idx = property.value;
						//		//(args++[idx]).debug("PdefParamSlot args");
						//		wrapper = PdefParamSlot(*args++[idx]);
						//	}
						//);
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
		class_dispatcher['Ppredef'] = class_dispatcher['Pdef'];
		class_dispatcher['Pbindef'] = class_dispatcher['Pdef'];

		class_dispatcher['StepList'] = class_dispatcher['List'];
		class_dispatcher['DictStepList'] = class_dispatcher['List'];
		class_dispatcher['ParDictStepList'] = class_dispatcher['List'];

		class_dispatcher['Event'] = class_dispatcher['Dictionary'];
		class_dispatcher['PresetEvent'] = class_dispatcher['Dictionary'];
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
		} {
			// ParamValue goes here
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

	asLabel {
		^this.label
	}

	label {
		^(label ?? { wrapper.label })
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

	///////// list behavior

	at { arg idx;
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

	/////////////////// MIDI mapping

	// FIXME: ambigous name, maybe rename to midiMap. Also map mean something different for the wrapper class

	map { arg msgNum, chan, msgType=\control, srcID, blockmode;
		MIDIMap(this, msgNum, chan, msgType, srcID, blockmode);
	}

	unmap { arg msgNum, chan, msgType, srcID, blockmode;
		MIDIMap.free(msgNum, chan, msgType, srcID, blockmode);
	}

	/////////////////// GUI mapping

	makeSimpleController { arg slider, action, updateAction, initAction, customAction;
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
				// is it garenteed that the erasing of action in a defer in freeUpdater below execute before this defer ?
				slider.action = { arg self;
					action.value(self, this);
					customAction.value(self, this);
					Param.lastTweaked = this;
					Param.changed(\lastTweaked);
					//debug("action!");
				};
			}.defer;

			this.makeUpdater(slider, updateAction);

			initAction.(slider, param);
		};
	}

	makeUpdater { arg view, action, updateMode;
		var param = this;
		updateMode = updateMode ? defaultUpdateMode;


		if(updateMode == \dependants) {
			// dependants mode
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
			switch(this.valueType,
				\scalar, {
					^val.asFloat.asStringPrec(precision);
				},
				\array, {
					//param.debug("mapStaticText param");
					//param.get.debug("param get");
					^val.collect({ arg x; x.asFloat.asStringPrec(precision) });
				},
				\env, {
					^val.asStringPrec(precision);
				}, {
					^val.asCompileString
				}
			);
		};
	}

	////// widgets

	mapMultiSlider { arg slider, action;
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
			customAction:action
		);
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
			updateAction: { arg self;
				var val = this.normGet;
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

	mapStaticTextLabel { arg view;
		this.makeSimpleController(view, {}, {}, { arg view, param;
			{
				view.string = param.asLabel;
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
				// refresh action
				{
					//[param, param.stringGet(precision)].debug("Param.mapTextField:get");
					//[param, view.hasFocus].debug("Param.mapTextField: hasfocus");
					if(view.hasFocus.not) {
						view.value = param.stringGet(precision);
					};
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
				view.value = param.get;
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

	mapPopUpMenu { arg view;
		if(this.type == \scalar) {
			this.mapIndexPopUpMenu(view)
		} {
			this.mapValuePopUpMenu(view)
		};
	}

	mapIndexPopUpMenu { arg view, keys;
		// FIXME: mapIndexPopUpMenu does not use updater
		var pm = view;
		//[keys, this.spec, this.spec.labelList].debug("mapIndexPopUpMenu: whatXXX");
		view.refreshChangeAction = { arg me;
			if(keys.isNil and: {this.spec.isKindOf(MenuSpec)}) {
				//[keys, this.spec, this.spec.labelList].debug("whatXXX");
				keys = this.spec.labelList;
			};
			if(keys.notNil) {
				pm.items = keys.asArray; // because PopUpMenu doesn't accept List
			};
			me.value = this.get;
		};
		pm.refreshChange;
		pm.action = {
			this.set(pm.value);
		};
		pm.onChange(this.controllerTarget, \set, { arg me;
			me.refreshChange;
		});
	}

	mapValuePopUpMenu { arg view;
		// FIXME: mapIndexPopUpMenu does not use updater
		// TODO: define a listener when the list change
		var pm = view;
		//debug("mapValuePopUpMenu:1");
		view.refreshChangeAction = {
			//[ this.spec.labelList.asArray, this.get, this.spec.unmapIndex(this.get)].debug("spec, get, unmap");
			view.items = this.spec.labelList.asArray;
			view.value = this.spec.unmapIndex(this.get);
		};
		view.refreshChange;
		//[this.spec, this.get].debug("mapValuePopUpMenu:2");
		//view.value.debug("mapValuePopUpMenu:3");
		pm.action = {
			//view.value.debug("mapValuePopUpMenu:4 (action)");
			this.set(this.spec.mapIndex(view.value));
			//this.get.debug("mapValuePopUpMenu:5 (action)");
		};
		pm.onChange(this.controllerTarget, \set, { arg me;
			// TODO: do not change the whole row when just one value is updated!
			//view.value.debug("mapValuePopUpMenu:6 (onchange)");
			view.refreshChange;
			//view.value.debug("mapValuePopUpMenu:7 (onchange)");
		});
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
			size = view.states.size;
			{
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

	asMultiSlider {
		^MultiSliderView.new
			.elasticMode_(1)
			.indexThumbSize_(100)
			.isFilled_(true)
			.fillColor_(Color.gray)
			.strokeColor_(Color.black)
			.size_(this.numChannels)
			.mapParam(this);
	}

	asStaticText {
		^StaticText.new.mapParam(this);
	}

	asStaticTextLabel {
		^StaticText.new.mapParamLabel(this);
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
		view = XEnvelopeView.new(nil, Rect(0, 0, 230, 80))
			.drawLines_(true)
			.selectionColor_(Color.red)
			.drawRects_(true)
			.step_(0)
			.thumbSize_(10)
			.elasticSelection_(true)
			.keepHorizontalOrder_(true)
			.mapParam(this)
			.grid_(Point(this.spec.times[0].unmap(1/8),1/8))
			.totalDur_(this.spec.times[0].unmap(2))
			.gridOn_(true);

		^view;
	}

	asButton {
		var but = Button.new
			.states_([
				["", Color.black, Color.white],
				["", Color.black, ParamViewToolBox.color_ligth],
			]);
		but.mapParam(this);
		^but;
	}

	asPopUpMenu {
		^PopUpMenu.new.mapParam(this)
	}

	asIndexPopUpMenu {
		^PopUpMenu.new.mapIndexParam(this)
	}

	asView {
		case(
			{ this.spec.isKindOf(XEnvSpec) }, {
				^this.asEnvelopeView;
			},
			{ this.spec.isKindOf(XArraySpec) }, {
				^this.asMultiSlider;
			},
			{ this.spec.isKindOf(MenuSpec) }, {
				^this.asPopUpMenu;
			},
			{ this.spec.isKindOf(XBufferSpec) }, {
				var scv = SampleChooserView.new;
				^scv.mapParam(this).view.addHalo(\ViewHolder, scv);
			}, {
				^this.asKnob;
			}
		)

	}

	edit {
		var fun = editFunction ? { arg param;
			var win;
			var label, widget, val;
			win = Window.new;

			label = param.asStaticTextLabel;
			label.align = \center;

			widget = param.asView;

			val = param.asStaticText;
			//val.background = Color.red;
			val.align = \center;

			win.layout = VLayout.new(
				label, 
				widget, 
				val
			);
			win.alwaysOnTop = true;
			win.front;
		};
		fun.(this);
	}

	/////////// Spec

	*valueToSpec { arg val, default_spec;
		var def = default_spec ? defaultSpec;
		^if(val.isKindOf(Array) or: val.isKindOf(List)) {
			XArraySpec( def ! val.size )
		} {
			if(val.isKindOf(Env)) {
				XEnvSpec( def ! val.levels.size )
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
				rval = XArraySpec(default_spec!def.size);
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
							rval = XArraySpec(default_spec!def.size);
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
	var <>combinator;
	var <>labelmode;
	var >default;

	/////// labels

	shortLabel { // deprecated
		^( shortLabel ? property );
	}

	typeLabel {
		^""
	}

	propertyLabel {
		^property.asString
	}

	targetLabel {
		^target.asString
	}

	fullLabel {
		^"% % %".format(this.typeLabel, this.targetLabel, this.propertyLabel)
	}

	asLabel { // backward compat
		^this.label
	}
	
	label {
		if(labelmode == \full) {
			^this.fullLabel
		} {
			^this.propertyLabel
		}
	}

	/////////////////

	at { arg idx;
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
		// to be used with Pseq because Pseq load the array only once
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
		if(this.get.notNil) {
			this.do { arg subparam, x;
				subparam.normSet(list[x])
			}
		} {
			this.normSet(list);
		}
	}

	type {
		// type according to spec
		var res = [
			[XEnvSpec, \env],
			[XArraySpec, \array],
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
		//	XEnvSpec, \env,
		//	XArraySpec, \array,
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
		//	XEnvSpec, \env,
		//	XArraySpec, \array,
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
			val = Param.getSynthDefDefaultValue(property, instr) ?? { spec.default };
			if(spec.class == XEnvSpec) {
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
		^this.spec.unmap(this.get)
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

////////////////// Ndef

NdefParam : BaseParam {
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
		spec = this.toSpec(sp);
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
		if(spec.class == XEnvSpec) {
			val = val.asEnv;
		};
		^val;
	}

	set { arg val;
		target.set(property, val);
		//target.changed(\set, property); // already exists in Ndef
	}

	normGet {
		var val = this.get;
		if(val.class == String) {
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
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug

			// debug variables
			var property = param.property;
			var target = param.target;
			var spec = param.spec;

			// action
			if(args[2].any({ arg x; x == param.property })) {
				action.(view, param);
			}
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
		if(spec.class == XArraySpec) {
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
		^"N % % %%".format(target.key, this.property, subproperty.asString[0], index)
	}

	ndefParamEnvSlotInit { arg subpro, idx;
		index = idx;
		subproperty = subpro;
	}

	spec {
		if(spec.class == XEnvSpec) {
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

////////////////// Pdef

PdefParam : BaseParam {
	var <multiParam = false;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		spec = this.toSpec(sp);
		key = obj.key;
		if(spec.isKindOf(XArraySpec)) {
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
			if(inval.notNil) {
				if(inval.class == Pbind) {
					inval = inval.patternpairs.clump(2).detect { arg pair;
						pair[0] == \instrument
					};
					if(inval.notNil) {
						inval = inval[1];
						if(inval.class == Symbol) {
							inval;
						} {
							nil
						}
					} {
						nil
					}
				} {
					nil
				};
			} {
				nil
			};
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
				//debug("1");
				xtarget.getSpec(xproperty) ?? {
					var mysp;
				//debug("2");
					// instrument metadata spec
					instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
				//debug("3");
						mysp = Param.getSynthDefSpec(xproperty, instr);
						// arg name in Spec
						mysp ?? {
				//debug("4");
							// arg name in Spec
							xproperty.asSpec ?? {
				//debug("5");
								// default value in SynthDef
								Param.specFromDefaultValue(xproperty, instr) ?? {
									Param.defaultSpec
								}
							}
						}
					} {
						// arg name in Spec
						xproperty.asSpec ?? {
							// default value in Pdef
							var myval = xtarget.getVal(xproperty);
				//debug("6");
							if(myval.notNil) {
				//debug("7");
								Param.valueToSpec(myval);
							} {
								// default spec
				//debug("8");
								Param.defaultSpec
							}
						}
					}
				}

			};
		^sp.asSpec;
	}

	toSpec { arg spec;
		^this.class.toSpec(spec, target, property)
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
			if(spec.class == XEnvSpec) {
				val = val.asEnv;
			};
			val = val.copy;
		};
		^val;
	}

	getRaw {
		^target.get(property)
	}

	set { arg val;
		target.setVal(property, val);
	}

	setRaw { arg val;
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
			if(args[2].any({ arg x; x == param.property })) {
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
		if(spec.class == XArraySpec) {
			// FIXME: fail if imbricated XArraySpec, but it's not supported anyway
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
		^"P % % %%".format(target.key, this.property, subproperty.asString[0], index)
	}

	pdefParamEnvSlotInit { arg subpro, idx;
		index = idx;
		subproperty = subpro;
	}

	spec {
		if(spec.class == XEnvSpec) {
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
		^XArraySpec(spec ! target.size);
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
		if(spec.class == XEnvSpec) {
			val = val.asEnv;
		};
		^val;
	}

	set { arg val;
		target[property] = val;
		target.changed(\set, property);
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

///////////

PbindSeqDefParam : PdefParam {
	spec {
		var si = this.get.size;
		if(si == 0) {
			^spec;
		} {
			^XArraySpec(spec ! si);
		}
	}

}

PbindSeqDefParamSlot : PdefParamSlot {
	//spec {
	//	^XArraySpec(spec ! this.get.size);
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
		if(spec.class == XArraySpec) {
			// FIXME: fail if imbricated XArraySpec, but it's not supported anyway
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
		if(spec.class == XEnvSpec) {
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

ParamAccessor {

	*neutral { 
		^(
			setval: { arg self, val;
				self.obj.setRaw(val);
			},

			getval: { arg self;
				self.obj.getRaw;
			},

			toSpec: { arg self, sp;
				sp
			},

			property: { arg self, prop;
				prop
			},

			path: { arg self, prop;
				prop
			},
		)

	}

	*array { arg idx;
		^(
			setval: { arg self, val;
				var oldval;
				oldval = self.obj.parent.get;

				oldval[idx] = val; 
				self.obj.parent.set(oldval);
			},

			getval: { arg self;
				var val;
				val = self.obj.parent.get;
				val[idx]
			},

			toSpec: { arg self, sp;
				if(sp.isKindOf(XArraySpec)) {
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
				if(sp.isKindOf(XEnvSpec)) {
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

	*envitem { arg selector, idx;
		^(
			setval: { arg self, val;
				var oldval;
				oldval = self.obj.parent.get;

				oldval.perform(selector)[idx] = val;
				self.obj.parent.set(oldval);
			},

			getval: { arg self;
				var oldval;
				oldval = self.obj.parent.get;
				oldval.perform(selector)[idx]
			},

			toSpec: { arg self, sp;
				if(sp.isKindOf(XEnvSpec)) {
					sp.perform(selector)[idx];
				} {
					sp
				}
			},

			property: { arg self, prop;
				idx
			},

			path: { arg self, prop;
				prop -> selector -> idx;
			},
		);
	}
}

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

	getRaw {
		var val;
		val = target[property] ?? { 
			//this.default.debug("dddefault: %, %, %;".format(this.target, this.property, this.spec));
			this.default
		};
		if(target.getHalo(\nestMode) == true) { // FIXME: what about more granularity ?
			val = Pdef.nestOff(val); 
		};
		^val;
	}

	setRaw { arg val;
		if(target.getHalo(\nestMode) == true) { // FIXME: what about more granularity ?
			val = Pdef.nestOn(val); 
		};
		target[property] = val;
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

