
Param {
	var <wrapper;
	var init_args;
	classvar <>defaultSpec;
	classvar <>simpleControllers;
	classvar <>defaultUpdateMode = \dependants;
	classvar <>defaultPollRate = 0.2;
	classvar <>editFunction;

	*initClass {
		Class.initClassTree(List);
		Class.initClassTree(Spec);
		Class.initClassTree(ControlSpec);
		defaultSpec = \widefreq.asSpec;
		simpleControllers = List.new;
	}

	*new  { arg ...args;
		"hehehe".debug;
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

		//class_dispatcher = IdentityDictionary.new;
		//"hello gangenr".debug;

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
							"Ndef: env named segment".debug;
							"NOT IMPLEMENTED YET!".debug;
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
							"Error: don't know what to do with string property (%) of Ndef, did you mean to use a symbol ?"
								.format(property).postln;
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
			Array: {
				//target.class.debug("mais what ??");
				"ERROR: not implemented for Array, use List instead".postln;
				^nil;
			},
			Message: {
				wrapper = MessageParam(*args);
			},
			Function: {
				wrapper = FunctionParam(*args);
			},
			Builder: {
				Builder.debug("newWrapper");
				wrapper = BuilderParam(*args);
			}
		);
		class_dispatcher['Ppredef'] = class_dispatcher['Pdef'];
		class_dispatcher['StepList'] = class_dispatcher['List'];
		class_dispatcher['DictStepList'] = class_dispatcher['List'];
		class_dispatcher['ParDictStepList'] = class_dispatcher['List'];
		class_dispatcher['Event'] = class_dispatcher['Dictionary'];
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
		( this.wrapper.property == param.wrapper.property)
		and: { this.wrapper.target == param.wrapper.target } 
	}

	asParam {
		^this
	}

	asLabel {
		^wrapper.asLabel;
	}

	type {
		^wrapper.type
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
			controller = SimpleController(param.target);
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

	*freeAllSimpleControllers {
		// used to free all controllers when something break in the GUI and you can't access it to remove the controller
		simpleControllers.do { arg con;
			con.remove
		};
		simpleControllers = List.new;
	}

	stringGet { arg precision=6;
		var val;
		val = this.get;
		if(val.class == Ndef or: {val.class == Symbol or: {val.class == String}}) {
			// the parameter is mapped to a Ndef
			^val.asCompileString;
		} {
			switch(this.type,
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
		this.mapIndexPopUpMenu(view)
	}

	mapIndexPopUpMenu { arg view, keys;
		var pm = view;
		if(keys.notNil) {
			pm.items = keys;
		};
		pm.action = {
			this.set(pm.value)
		};
		pm.onChange(this.target, \set, { arg me;
			me.value = this.get;
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
				[""],
				["", Color.white, Color.black],
			]);
		but.mapParam(this);
		^but;
	}

	asView {
		case(
			{ this.spec.class == XEnvSpec }, {
				^this.asEnvelopeView;
			},
			{ this.spec.class == XArraySpec }, {
				^this.asMultiSlider;
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
		default_spec = default_spec ? defaultSpec;
		if(val.isSequenceableCollection) {
			^XArraySpec.new(default_spec.asSpec!val.size)
		} {
			^default_spec.asSpec
		}
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
			if(def.class.isSequenceableCollection) {
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
			wrapper.perform(selector, * args);
		};
	}

}


BaseParam {

	at { arg idx;
		^Param(this.target, this.property -> idx, this.spec)
	}

	asParamList {
		^this.spec.size.collect { arg x;
			this.at(x)
		}
	}

	do { arg fun;
		this.spec.size.do { arg x;
			fun.(this.at(x), x)
		}
	}

	collect { arg fun;
		^this.spec.size.collect { arg x;
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
		^switch(this.spec.class,
			XEnvSpec, \env,
			XArraySpec, \array,
			\scalar,
		)
	}
}

StandardConstructorParam : BaseParam {
	var <target, <property, <spec, <key;

	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	asLabel {
		^"% %".format(target.class, property)
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
			if(args[2].any({ arg x; x == param.property })) {
				action.(view, param);
			}
		});
	}

}

////////////////// Ndef

NdefParam : BaseParam {
	var <target, <property, <spec, <key;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	asLabel {
		^"N % %".format(target.key, property)
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
			^this.spec.unmap(this.get)
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
	var <target, <property, <spec, <key;
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

	asLabel {
		^"P % %".format(target.key, property)
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
			//args.debug("args");

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
	var <target, <property, <spec, <key;
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
	var <target, <property, <spec, <key;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	asLabel {
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
	var <target, <property, <spec, <key;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	asLabel {
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
	var <target, <property, spec, <key;
	*new { arg obj, meth, sp;
		// meth/property is useless
		^super.new.init(obj, meth, sp);
	}

	asLabel {
		^"L % %".format(target.identityHash, property)
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
	var <target, <property, <spec, <key;
	*new { arg obj, meth, sp;
		// meth/property is index
		// spec should be scalar
		^super.new.init(obj, meth, sp);
	}

	asLabel {
		^"L % %".format(target.identityHash, property)
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
	
	var <target, <property, <spec, <key;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	asLabel {
		^"D % %".format(target.identityHash, property)
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
					Param.defaultSpec;
				};
			};
		^sp.asSpec;
	}

	get {
		var val;
		val = target[property];
		^val;
	}

	set { arg val;
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
	var <target, <property, spec, <key, <index;
	*new { arg obj, meth, sp, index;
		// meth/property is index
		// spec should be scalar
		^super.new(obj, meth, sp).initDictionaryParamSlot(index);
	}

	initDictionaryParamSlot { arg idx;
		index = idx;
	}

	asLabel {
		^"D % %".format(target.identityHash, property)
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

////////////////// Object property (message)
MessageParam : StandardConstructorParam {

	asLabel {
		^"% %".format(target.class, property)
	}

	set { arg val;
		target.receiver.perform((property++"_").asSymbol, val);	
	}

	get { 
		^target.receiver.perform(property);	
	}
}

FunctionParam : StandardConstructorParam {
	var <getFunc, <setFunc;

	asLabel {
		^"F % %".format(target.identityHash)
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;

		getFunc = obj;
		setFunc = meth;

		spec = this.toSpec(sp);
		//key = obj.identityHash.asSymbol;
	}

	set { arg val;
		setFunc.(val, this);
	}

	get { 
		^getFunc.(this);
	}
}

BuilderParam : StandardConstructorParam {

	asLabel {
		^"B % %".format(target.key ? "", property)
	}

	init { arg obj, meth, sp;
		this.class.debug("init");
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



////////////////////////////////////////



MIDIMap {
	classvar <responders;
	classvar responders_param;
	classvar <mapped_views; // debug: added getter
	classvar midivalues;
	classvar <controls;
	classvar <>permanent = true;
	classvar <>defaultBlockmode = false;

	// old path type: [srcID, msgType, chan, msgNum]
	// current path type: [msgNum, chan, msgType, srcID]
	
	*initClass {
		Class.initClassTree(MultiLevelIdentityDictionary);
		responders = MultiLevelIdentityDictionary.new;
		responders_param = MultiLevelIdentityDictionary.new;
		mapped_views = MultiLevelIdentityDictionary.new;
		midivalues = MultiLevelIdentityDictionary.new;
		controls = IdentityDictionary.new;
		//params = Dictionary.new;
	}

	*new { arg key, param, blockmode;
		var func;
		var path = this.keyToPath(key);
		var nilpath;
		if(path.isNil) {
			^nil
		};
		if(param.class != Function) {
			param = param.asParam;
		};
		nilpath = path.collect({ arg x; if(x == \all) { nil } { x } }); // can't have nil has dict key
		//[key, path, nilpath, param].debug("key, path, nilpath, param");

		func = { arg val, num, chan, src;
			var setfun = {
				//Task({
					param.normSet(val);
				//	nil;
				//}).play(AppClock);
			};
			//[key, path, nilpath, param].debug("key, path, nilpath, param");
			val = val/127;
			//[val, num, chan, src].debug("key, path, nilpath, param");
			if(param.class == Function) {
				param.value;
			} {
				var myblockmode = if(blockmode.isNil) {
					defaultBlockmode;
				} {
					blockmode
				};
				//myblockmode.debug("BLOCKMODE");
				if(myblockmode != true) {
					setfun.();
					midivalues.put(*path++[val]);
				} {
					var midival = midivalues.at(*path) ? 0;
					var normval = param.normGet;
					if(normval.class == BinaryOpFunction) {
						setfun.();
					} {
						//[midival, normval, val, (normval - midival).abs, (normval - midival).abs < ( 1/126 ), ( normval - midival ).abs < ( 1/110 ) ].debug("- midi, norm, val");
						//if((normval - midival).abs < ( 1/126 )) {
						if((normval - midival).abs < ( (param.spec.unmap(param.spec.step)) + ( 1/126 ) )) {
							//"NOT BLOCKED".debug;
							setfun.();
						} {
							if(( normval - midival ).abs < ( 1/110 ) ) {
								//"UNBLOCK".debug;
								setfun.();
							} {
								// do nothing because it's blocked
								//"BLOCKED".debug;
							}
						};
					};
					midivalues.put(*path++[val]);
					//midivalues.at(*path).debug("new stored midival at the end");
				};
			}
		};

		if(responders.at(*path).notNil) {
			responders.at(*path).free
		};
		responders.put(*path ++ [
			MIDIFunc(func, nilpath[0], nilpath[1], nilpath[2], nilpath[3]).permanent_(permanent)
			//params[param] =	params[param].add( path );
		]);
		responders_param.put(*path ++ [ param ]);
		this.changed(\midimap, path, param);
		this.updateViews(path, param);
	}

	*define { arg channel, defs;
		var source_uid = nil;
		if(channel.isSequenceableCollection) {
			source_uid = channel[1];
			channel = channel[0]
		};
		defs.pairsDo { arg key, val;
			var kind=\control, keychannel;
			if(val.class == Association) {
				kind = val.key;
				val = val.value;
			};
			if(val.isSequenceableCollection) {
				keychannel = val[1];
				val = val[0]
			} {
				keychannel = channel;
			};
			//key.debug("kkKKey");
			//val.debug("kkKKeyVVVVVVVVVVVVV");
			//kind.debug("kkKKeykinddddddddddd");
			//controls[key].changed(\free_map);
			if(kind == \note) {
				kind = \noteOn
			};
			controls[key] = [val, keychannel, kind, source_uid];
		};
	}

	*keyToPath { arg key;
		if(key.class == Symbol) {
			var path = controls[key];
			if(path.isNil) {
				"Error: no key named % in MIDIMap".format(key).postln;
				^nil
			} {
				^this.normalizePath(path)
			}
		} {
			^this.normalizePath(key)
		}
	}

	*normalizePath { arg path;
		path = path.extend(4,nil);
		if(path[2] == nil or: {path[2] == \all}) { // default msgType is \control
			path[2] = \control;
		};
		path = path.collect({ arg x; if(x.isNil) { \all } { x } });
		^path;
	}


	*unmap { arg param;
		// TODO
		//params[param]
	}

	*free { arg key;
		var path = this.keyToPath(key);
		responders.at(*path).free;
		responders_param.put(*path ++ [nil]);
		this.changed(\midimap, path, nil);
		this.updateViews(path);
	}

	*get { arg key;
		var path = this.keyToPath(key);
		responders.at(*path)
	}

	*freeAll {
		responders.leafDo { arg path, resp;
			this.free(path);
		};
	}

	*learn {
		arg key, param, blockmode;
	}

	*updateViews { arg path, param;
		var to_remove = List.new;
		mapped_views.at(*path).do { arg view, x;
			var kind = view[1];
			view = view[0];
			if(view.isClosed) {
				to_remove.add(x)
			} {
				if(param.notNil) {
					if(kind == \label) {
						view.mapParamLabel(param)
					} {
						view.mapParam(param)
					}
				} {
					view.unmapParam;
				}
			}
		};
		to_remove.reverse.do { arg x;
			mapped_views.at(*path).removeAt(x)
		};
	}

	*mapView { arg key, view;
		var path = this.keyToPath(key);
		if(path.isNil) {
			^nil
		};
		if(mapped_views.at(*path).isNil) {
			mapped_views.put(*path ++ [ List.new ])
		};
		mapped_views.at(*path).add([view]);
		this.updateViews(path, responders_param.at(*path));
	}

	*mapSlider { arg key, slider;
		^mapView(key, slider);
	}

	*mapStaticTextLabel { arg key, view;
		var path = this.keyToPath(key);
		if(path.isNil) {
			^nil
		};
		if(view.isNil) {
			"MIDIMap.mapStaticTextLabel: Error: view is nil".postln;
			^nil
		};
		if(mapped_views.at(*path).isNil) {
			mapped_views.put(*path ++ [ List.new ])
		};
		mapped_views.at(*path).add([view, \label]);
		this.updateViews(path, responders_param.at(*path));
	}

	*unmapView { arg key, view;
		// TODO: add code to handle key=nil: search in all paths
		var list;
		var path = this.keyToPath(key);
		if(path.isNil) {
			^nil
		};
		if(mapped_views.at(*path).isNil) {
			mapped_views.put(*path ++ [ List.new ])
		};
		list = mapped_views.at(*path);
		list.reverse.do { arg vi, x;
			vi = vi[0]; // [1] is view type
			if(view === vi) {
				list.removeAt(list.size - 1 - x)
			}
		}
	}

}

////////////////////////////////////////

ParamGroup : List {
	var <presets;
	var <>morphers;
	classvar <>editFunction;

	// morphers format : Dict[ \name -> (val: 0.5, presets:[\preset1, \preset2]) ]
	// - morphTo(\name, 0.3)
	// - addMorphing(\name)
	// - morphTo([\preset1, \preset2], 0.4)
	*new { arg anArray;
		var inst;
		Class.initClassTree(ParamGroupLayout);
		inst = super.new.setCollection( anArray.collect(_.asParam) );
		inst.initParamGroup;
		^inst;
	}

	initParamGroup {
		presets = IdentityDictionary.new;
	}

	save { arg key=\default; 
		presets[key] = super.array.collect { arg param;
			param.get;
		}
	}

	presets_ { arg val;
		presets = val.deepCopy;
	}

	getPreset { arg key=\default;
		^presets[key]
	}

	getPresetCompileString { arg key=\default;
		// TODO: write asPresetCompileStringNdef and Pdef and each other Param type

	}

	valueList {
		^this.collect { arg param;
			param.get;
		}
	}

	erase { arg key=\default;
		presets[key] = nil;
	}

	load { arg key=\default; 
		if(presets[key].notNil) {
			presets[key].do { arg val, x;
				super.array[x].set(val)
			}
		}
	}

	edit {
		var fun = editFunction ? { arg pg;
			ParamGroupLayout.new(pg);
		};
		fun.(this);
	}

}

ParamGroupDef {
	classvar <lib;
	var <key;
	var <group;

	*initClass {
		Class.initClassTree(IdentityDictionary);
		lib = IdentityDictionary.new
	}

	*new { arg defkey, group;
		var inst;
		if(group.isNil) {
			^lib[defkey]
		} {
			if(lib[defkey].isNil) {
				inst = super.new.init(defkey, group);
				lib[defkey] = inst;
				^inst
			} {
				"Warning: ParamGroupDef(%) already defined, use .clear before redefine it".format(defkey).postln;
				^lib[defkey]
			}
		}
	}

	init { arg defkey, xgroup;
		//xgroup.debug("hhhhhhhhhh");
		key = defkey;
		group = ParamGroup(xgroup);
		if(Archive.global.at(\ParamGroupDef, key).isNil) {
			this.saveArchive;
		} {
			this.loadArchive;
		};
	}

	presets {
		^group.presets
	}

	presets_ { arg val;
		group.presets = val.deepCopy;
		this.saveArchive;
	}

	getPreset { arg name=\default;
		^group.getPreset(name);
	}

	valueList {
		^group.valueList;
	}

	saveArchive {
		var archive = IdentityDictionary.new;
		archive[\presets] = group.presets;
		archive[\morphers] = group.morphers;
		Archive.global.put(\ParamGroupDef, key, archive);
	}

	loadArchive {
		var archive;
		var presets;
		"load Archive".debug;
		archive = this.getArchive;
		presets = archive[\presets];
		presets.keysValuesDo { arg k,v;
			// this loop is to workaround a bug in Ndef/Archive which load the Ndef with a nil server
			v = v.collect { arg val;
				//val.class.debug("val class");
				if(val.class == Ndef) {
					"val is a Ndef".debug;
					val.server = Server.default;
					val = val.asCompileString.interpret;
				};
				val;
			};
			presets[k] = v;
		};
		group.presets = presets;
		group.morphers = archive[\presets];
		"end load Archive".debug;
	}

	getArchive {
		^Archive.global.at(\ParamGroupDef, key);
	}


	clear {
		Archive.global.put(\ParamGroupDef, key, nil);
		lib[key] = nil;
	}

	save { arg name;
		group.save(name);
		this.saveArchive;
	}

	load { arg name;
		group.presets[name] = this.getArchive[\presets][name];
		group.load(name);
	}

	erase { arg name;
		group.erase(name);
		this.saveArchive;
	}

	do { arg fun;
		group.do(fun)
	}

	collect { arg fun;
		^group.collect(fun)
	}

	at { arg x;
		^group[x]
	}

	size {
		^group.size;
	}

	edit {
		group.edit;
	}

	asList {
		^group
	}

}

// act also as a Param wrapper
ParamValue : BaseParam {
	var <>value=0;
	var <>spec, <>property=\value, <target;
	var <>label;

	*new { arg spec;
		^super.new.initParamValue(spec);
	}

	initParamValue { arg xspec;
		target = this;
		property = \value;
		label = "ParamValue";
		spec = xspec.asSpec ? Param.defaultSpec;
	}

	get {
		^value;
	}

	asParam {
		^Param(this)
	}

	set { arg val;
		value = val;
		this.changed(\set, [\value, val]);
	}

	asLabel {
		^label;
	}

	map { arg val;
		^this.spec.map(val)
	}

	unmap { arg val;
		^this.spec.unmap(val)
	}

	// normGet and normSet are not used by morpher because of additional code on setter/getter
	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}

	putListener { arg param, view, controller, action;
		controller.put(\set, { arg ...args; 
			// args: object, \set, keyval_list
			//args.debug("args");

			// update only if concerned key is set
			// FIXME: may break if property is an association :(
			// FIXME: if a value is equal the key, this fire too, but it's a corner case bug
			if(args[2].any({ arg x; x == param.property })) {
				action.(view, param);
			}
		});
	}
}

// act also as a Param
ParamMorpher : Param {
	var <group, <>presets;
	var <>key;
	var <>optimizedPresets;
	*new { arg group, presets;
		var pval = ParamValue.new;
		var inst;
		pval.spec = ControlSpec(0,presets.size-1,\lin,0,0);
		inst = super.newWrapper(pval, presets);
		inst.initParamMorpher(group, presets);
		^inst;
	}

	initParamMorpher { arg arggroup, argpresets;
		if(arggroup.class == Symbol) {
			group = ParamGroupDef(arggroup)
		} {
			group = arggroup;
		};
		presets = argpresets;
		//[group, presets].debug("initParamMorpher");
	}

	morph { arg list, morph;
		^list.blendAt(morph)
	}

	asLabel {
		// TODO
		^(key ?? {
			if(group.class == ParamGroupDef) {
				group.key
			} {
				nil
			}
		} ?? {presets.asString})
	}

	optimizeMorphing {
		var first;
		var presetgrid;
		if(presets.size > 0) {
			// a group preset contains a snapshot of the value of each param of the group
			// a param preset contains each value a param can take in presets
			optimizedPresets = List.new;
			first = group.getPreset(presets[0]);
			presetgrid = presets.collect{ arg x; group.getPreset(x) }; // list of group presets
			presetgrid.flop.do { arg parampreset, x; // list of param presets
				if(parampreset.any({ arg x; x != parampreset[0] })) {
					// different values, so do the morphing
					optimizedPresets.add(x)
				} {
					// same values, dont add it for morphing
				};
			};
		}

	}

	disableOptimize {
		optimizedPresets = nil;
	}

	set { arg val;
		var presets_vals;
		var iter;
		//val.debug("ParamMorpher: set");
		this.wrapper.set(val);
		presets_vals = presets.collect({ arg x; 
			var res = group.getPreset(x);
			if(res.isNil) {
				"Error: preset % is not defined".format(x.asCompileString).postln;
				^nil
			};
			res;
		});
		//[presets_vals, presets].debug("presets");
		presets_vals = presets_vals.flop;
		if(group.size != presets_vals.size) {
			"Error: preset size (%) don't match group size (%)".format(presets_vals.size, group.size).postln;
			^nil;
		};

		if(optimizedPresets.notNil) {
			iter = optimizedPresets;
		} {
			iter = group.size;
		};
		iter.do({ arg x;
			var resval;
			var param = group.at(x);
			//[x, presets_vals[x], val].debug("morph.set: groupdo");
			resval = this.morph(presets_vals[x], val);
			//[param.asLabel, val].debug("ParamMorpher: param set");
			param.set(resval);
		})
	}

	get {
		^this.wrapper.get;
	}

	normGet {
		^this.spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(this.spec.map(val))
	}
}

ParamMorpherDef : ParamMorpher {
	classvar lib;

	*initClass {
		Class.initClassTree(IdentityDictionary);
		lib = IdentityDictionary.new;
	}

	*new { arg defkey, group, presets;
		var inst;
		if(group.isNil) {
			^lib[defkey]
		} {
			if(lib[defkey].isNil) {
				inst = super.new(group, presets);
				inst.key = defkey;
				lib[defkey] = inst;
				^inst
			} {
				"Warning: already defined, use .clear before redefine it".postln;
				^lib[defkey]
			}
		}
	}

	clear {
		lib[this.key] = nil
	}
}

PresetListMorpher : ParamMorpher {
	var <disabledPresets;
	var size;
	*new { arg group, size, prefix=\preset;
		var pval = ParamValue.new;
		var inst;
		pval.spec = ControlSpec(0,size,\lin,0,0);
		inst = super.newWrapper(pval);
		inst.initPresetListMorpher(group, size, prefix);
		^inst;
	}

	initPresetListMorpher { arg group, xsize, prefix;
		var prelist;
		size = xsize;
		disabledPresets = Set.new;
		prelist = size.collect { arg x; ( prefix++x ).asSymbol };
		this.initParamMorpher(group, prelist);
	}

	set { arg val;
		var presets_vals;
		var xsize;
		var iter;
		//val.debug("ParamMorpher: set");
		this.wrapper.set(val);
		presets_vals = presets.collect({ arg x; 
			var res = group.getPreset(x);
			res;
		});
		presets_vals = presets_vals.reject({ arg x; x.isNil }).reject(disabledPresets.includes(_));
		xsize = presets_vals.size;
		//[presets_vals, presets].debug("presets");
		if(xsize == 0) {
			"PresetListMorpher.set: WARNING: not preset in final list, do nothing".postln;
		} {
			val = val.linlin(0, size-1, 0, xsize-1 );
			presets_vals = presets_vals.flop;
			if(group.size != presets_vals.size) {
				"Error: preset size (%) don't match group size (%)".format(presets_vals.size, group.size).postln;
				^nil;
			};
			if(optimizedPresets.notNil) {
				iter = optimizedPresets;
			} {
				iter = group.size;
			};
			iter.do({ arg x;
				var resval;
				var param = group.at(x);
				//[x, presets_vals[x], val].debug("morph.set: groupdo");
				resval = this.morph(presets_vals[x], val);
				//[param.asLabel, val].debug("ParamMorpher: param set");
				param.set(resval);
			})
		};
	}

	getPreset { arg preset_index;
		^group.getPreset(presets[preset_index]);
	}

	save { arg preset_index;
		group.save(presets[preset_index]);
		//this.updateOptimizer;
	}

	load { arg preset_index;
		group.load(presets[preset_index]);
	}

	erase { arg preset_index;
		group.erase(presets[preset_index]);
	}

	enablePreset { arg preset_index;
		disabledPresets = disabledPresets.remove(presets[preset_index]);
	}

	isEnabled { arg preset_index; 
		^disabledPresets.includes(presets[preset_index]).not;
	}

	disablePreset { arg preset_index;
		disabledPresets = disabledPresets.add(presets[preset_index]);
	}
}

PresetListMorpherDef : PresetListMorpher {
	classvar <>all;
	*initClass {
		Class.initClassTree(IdentityDictionary);
		all = IdentityDictionary.new;
	}

	*new { arg key, group, size, prefix=\preset;
		var inst;
		if(all[key].isNil) {
			inst = super.new(group, size, prefix);
			inst.key = key;
			all[key] = inst;
		} {
			if(group.notNil) {
				// TODO: provide a way to disable this message, and put key name in message
				"Warning: PresetListMorpherDef: already defined, use .clear before redefine it".postln;
			};
			inst = all[key];
		};
		^inst
	}

	clear {
		all[key] = nil;
		^nil
	}
}

///////////////////////


ParamCombinator {
	var <ranges, <inputs, <base;
	var <baseParam, <rangeParam, <inputParam, <targetParam;
	var <controllers; 
	var <rangeSize;

	*new { arg param, size=3;
		Class.initClassTree(ParamGroupLayout);
		^super.new.init(param, size);
	}

	init { arg param, size=3;

		rangeSize = size;
		ranges = List.newFrom( 0!size );
		inputs = List.newFrom( 0!size );
		targetParam = param;
		base = ParamValue(param.spec).set(param.get);
		baseParam = base.asParam;

		rangeParam = Param(ranges, \list, \bipolar);
		inputParam = Param(inputs, \list, \unipolar);

		controllers = [
			SimpleController(ranges).put(\set, {
				this.computeTargetValue;
			}),

			SimpleController(inputs).put(\set, {
				this.computeTargetValue;
			}),

			SimpleController(base).put(\set, {
				this.computeTargetValue;
			}),

		];

	}

	freeAllSimpleControllers {
		controllers.do { arg x; x.remove; };
	}

	setBusMode { arg bool=true, name=\default;
		// TODO: disable, anyone ?
		var bus = BusDef(name, \control);
		Ndef(name).clear;
		Ndef(name, {
			var inputs, ranges;
			var fval;
			fval = \base.kr(0);
			fval = targetParam.spec.unmap(fval);
			inputs = \inputs.kr(0!rangeSize);
			ranges = \ranges.kr(0!rangeSize);

			inputs.do { arg in, x;
				fval = fval + (in * ranges[x])
			};
			targetParam.spec.map(fval).poll;
		});
		this.freeAllSimpleControllers;
		baseParam = Param(Ndef(name), \base, targetParam.spec);
		targetParam.target.set(targetParam.property, Ndef(name).asMap);
		rangeParam = Param(Ndef(name), \ranges, XArraySpec(\bipolar ! rangeSize));
		rangeParam.set(ranges.asArray); // whyyyy list doesnt do anything ????
		inputParam = Param(Ndef(name), \inputs, XArraySpec(\unipolar ! rangeSize));
		inputParam.set(inputs.asArray);
	}

	computeTargetValue {
		var fval;
		fval = baseParam.normGet;
		inputs.do { arg in, x;
			fval = fval + (in * ranges[x])
		};
		targetParam.normSet(fval);
	}

	mapModKnob { arg modknob;
		modknob
			.onChange(rangeParam.target, \set, { arg view;
				debug("ranges changz");
				rangeParam.do { arg param, x;
					modknob.set_range(x, param.get.debug("range change X"+x));
				};
				modknob.refresh;
			})
			.onChange(baseParam.target, \set, { arg view;
				debug("base changz");
				modknob.value = baseParam.normGet;
			})
			.onChange(targetParam.target, \set, { arg view;
				var nval;
				var gval;
				debug("target changz");
				gval = targetParam.get;
				if(gval.isKindOf(Number)) {
					modknob.midi_value = targetParam.normGet;
					modknob.refresh;
				}
			})
			.action_({
				debug("action changz");
				baseParam.normSet(modknob.value)
			})
		;
		baseParam.target.changed(\set);
		rangeParam.target.changed(\set);
		targetParam.target.changed(\set);

	}

	edit { arg self;
		var window = Window.new;
		var layout;
		var modknob;
		modknob = ModKnob.new
			.onChange(ranges, \set, { arg view;
				ranges.do { arg val, x;
					modknob.set_range(x, val);
				};
				modknob.refresh;
			})
			.onChange(base, \set, { arg view;
				modknob.value = baseParam.normGet;
			})
			.onChange(targetParam.target, \set, { arg view;
				modknob.midi_value = targetParam.normGet;
				modknob.refresh;
			})
			.action_({
				baseParam.normSet(modknob.value)
			})
			;
		modknob.view.minSize = 100@100;
		layout = HLayout(
			modknob.view,
			ParamGroupLayout.two_panes(ParamGroup([
				baseParam,
				rangeParam,
				inputParam,
				targetParam,
			].flatten)),
		);
		window.layout = layout;
		window.alwaysOnTop = true;
		window.front;
	}
}


////////////////////////////////////////

CachedBus : Bus {
	classvar cache;

	*initClass {
		Class.initClassTree(IdentityDictionary);
		cache = IdentityDictionary.new;
		cache[\audio] = IdentityDictionary.new;
		cache[\control] = IdentityDictionary.new;
	}

	set { arg ... values;
		var val;
		if(values.size == 1) {
			val = values[0];
			super.set(val);
		} {
			val = values;
			super.setn(val);
		};
		cache[this.rate][this.index] = val;
	}

	setn { arg values;
		this.set(*values)
	}

	getCached {
		if(cache[this.rate][this.index].isNil) {
			this.get({ arg x; cache[this.rate][this.index] = x });
			^0
		} {
			^cache[this.rate][this.index]
		}
	}

}

//SimpleParamView {
	//
	//			new {
	//				var font;
	//				self = self.deepCopy;
	//			
	//				font = Font.default;
	//				font.size = 11;
	//				self.layout = VLayout.new;
	//				//self.label = StaticText.new.font_(font).minWidth_(150);
	//				self.label = StaticText.new.font_(font);
	//				self.knob = Knob.new;
	//				//self.val = TextField.new.font_(font).minWidth_(150);
	//				self.val = TextField.new.font_(font);
	//				self.layout.add(self.label, stretch:1);
	//				self.layout.add(self.knob);
	//				self.layout.add(self.val);
	//				self.layout.margins = 1;
	//				self.layout.spacing = 10;
	//			
	//				self;
	//			}
	//
	//			mapMidi { arg key;
	//				MIDIMap.mapStaticTextLabel(key, self.label);
	//				MIDIMap.mapView(key, self.knob);
	//				MIDIMap.mapView(key, self.val);
	//			}
//}


//////////////////////////////////

+Symbol {
	asBus { arg numChannels=1, busclass;
		var rate;
		var index;
		var map;
		busclass = busclass ? Bus;
		map = this;
		map = map.asString;
		switch(map[0],
			$c, {
				rate = \control;
			},
			$a, {
				rate = \audio;
			}, {
				"get_bus_from_map: error, not a bus: %".format(map).postln;
			}
		);
		index = map[1..].asInteger;
		^busclass.new(rate, index, numChannels, Server.default);
	}

	asCachedBus { arg numChannels=1;
		^this.asBus(numChannels, CachedBus);
	}

}

+Pdef {
	nestOn { arg val;
		// see also .bubble and .unbubble
		if(val.isSequenceableCollection) {
			if(val[0].isSequenceableCollection) {
				// NOOP
			} {
				val = [val]
			}
		};
		^val
	}

	nestOff { arg val;
		if(val.isSequenceableCollection) {
			if(val[0].isSequenceableCollection) {
				val = val[0];
			} {
				// NOOP
			}
		};
		^val
	}

	setBusMode { arg key, enable=true, free=true;
		//"whatktktj".debug;
		if(enable) {
			//"1whatktktj".debug;
			if(this.inBusMode(key)) {
				// NOOP
				//"2whatktktj".debug;
			} {
				var val = this.getVal(key);
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
					// hardcoded rate, but can't convert audio buffer to a number, so it's ok
				//bus.debug("setBusMode: bus");
				if(val.isSequenceableCollection) {
					bus.setn(val);
				} {
					bus.set(val);
				};
				//val.debug("setBusMode: val");
				this.set(key, this.nestOn(bus.asMap));
				//bus.asMap.debug("setBusMode: busmap");
			}
		} {
			if(this.inBusMode(key).not) {
				// NOOP
			} {
				var val = this.getVal(key);
				var map = this.get(key);
				var numChannels = 1;
				var bus;
				map = this.nestOff(map);
				bus = map.asCachedBus;
				this.set(key, this.nestOn(val));
				if(free) {
					bus.free;
				};
			}

		}

	}

	inBusMode { arg key;
		var val = this.get(key);
		if(val.isSequenceableCollection) {
			// multichannel
			if(val[0].isSequenceableCollection) {
				// nested
				^(val[0][0].class == Symbol)
			} {
				^(val[0].class == Symbol)
			}
		} {
			^(val.class == Symbol)
		}
	}

	getVal { arg key;
		var curval;
		if(this.envir.isNil) { this.envir = this.class.event };
		curval = this.get(key);
		curval = this.nestOff(curval);
		if(this.inBusMode(key)) {
			var bus = curval.asCachedBus;
			^bus.getCached;
		} {
			if(curval.class == Function) {
				curval = this.envir.use({ curval.value });
			};
			^curval;
		};
	}

	setVal { arg key, val;
		if(val.isKindOf(Env)) {
			this.set(key, this.nestOn(val))
		} {
			if(this.inBusMode(key)) {
				var bus;
				var curval;
				curval = this.get(key);
				curval = this.nestOff(curval);
				bus = curval.asCachedBus;
				if(curval.isSequenceableCollection) {
					bus.setn(val);
				} {
					bus.set(val);
				};
				this.changed(\set, [key, val]);
			} {
				this.set(key, this.nestOn(val))
			};
		}
	}

	asParamGroup { arg instrument, notes=true, exclude;
		// TODO: find a better name for 'notes' argument (this is for adding \dur and \legato)
		var list;

		instrument = instrument ?? { this.getHalo(\instrument) };
		if(instrument.isNil) {
			"ERROR: Pdef:asParamGroup: Can't create paramGroup: no instrument is defined".postln;
			^nil
		} {
			exclude = exclude ? [\out, \gate, \doneAction, \bufnum];

			list = SynthDescLib.global.synthDescs[instrument].controls.reject({ arg con; 
				con.name == '?' or: {
					exclude.includes(con.name)
				}
			}).collect({ arg con;
				Param( this, con.name );
			});

			if(notes) {
				list = [
					Param(this, \dur),
					Param(this, \legato),
				] ++ list;
			};
			^ParamGroup(list)
		}
	}

	asEnvirCompileString {
		^this.envir.collect({ arg x; x.asCompileString })
	}

	asPatternCompileString {
		var res;
		res = "Pbind(\n".format(this.key);
		this.envir.keysValuesDo({ arg k,v;
			res = res ++ "\t%, %,\n".format(k.asCompileString, v.asCompileString);
		});
		res = res ++ ");\n";
		^res;
	}
}

+Ndef {
	asParamGroup { 
		// TODO: add method argument (find a better name) for adding volume
		^ParamGroup(
			this.controlNames.collect{ arg con;
				Param(this, con.name)
			}
		)
	}

	isNaN {
		// used to avoid NumberBox and EZ* GUI to throw an error
		^true
	}

}

+String {
	isNaN {
		// used to avoid NumberBox and EZ* GUI to throw an error
		^true
	}
}

/////////////////////////// 3.6.7

+Slider {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this);
	}
}

+Knob {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this);
	}
}

+MultiSliderView {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapMultiSlider(this);
	}
}

+StaticText {
	unmapParam {
		Param.unmapSlider(this);
		{
			this.string = "-";
		}.defer;
	}

	mapParam { arg param;
		param.mapStaticText(this);
	}

	mapParamLabel { arg param;
		param.mapStaticTextLabel(this);
	}
}

+TextField {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapTextField(this);
	}
}

+NumberBox {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapNumberBox(this);
	}
}

+Button {
	unmapParam {
		Param.unmapView(this);
	}

	mapParam { arg param;
		param.mapButton(this);
	}
}

+EZKnob {
	unmapParam { arg mapLabel=true;
		Param.unmapEZKnob(this, mapLabel);
	}

	mapParam { arg param, mapLabel=true;
		param.mapEZKnob(this, mapLabel);
	}
}

+EZSlider {
	unmapParam { arg mapLabel=true;
		Param.unmapEZSlider(this, mapLabel);
	}

	mapParam { arg param, mapLabel=true;
		param.mapEZSlider(this, mapLabel);
	}
}

+PopUpMenu {
	// map index of popupmenu to param 
	mapParam { arg param;
		this.mapIndexParam(param)
	}

	mapIndexParam { arg param;
		param.mapIndexPopUpMenu(this)
	}
}

/////////////

+ View {
	onChange { arg model, key, fun;
		var con = SimpleController.new(model).put(key, { arg ...args;
			if(this.isClosed) {
				con.remove;
			} {
				fun.(* [this] ++ args);
			};
		});
	}
}


/////////////////////////// 3.6.6
+QKnob {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this);
	}
}

+QSlider {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this);
	}
}

+QMultiSliderView {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapMultiSlider(this);
	}
}

+QStaticText {
	unmapParam {
		Param.unmapSlider(this);
		{
			this.string = "-";
		}.defer;
	}

	mapParam { arg param;
		param.mapStaticText(this);
	}

	mapParamLabel { arg param;
		param.mapStaticTextLabel(this);
	}
}

+QTextField {
	unmapParam {
		Param.unmapSlider(this);
		{
			this.value = "";
		}.defer;
	}

	mapParam { arg param;
		param.mapTextField(this);
	}
}

+QButton {
	unmapParam {
		Param.unmapView(this);
	}

	mapParam { arg param;
		param.mapButton(this);
	}
}

//XGridLayout : QGridLayout {
//	var <myStretch;
//
//	*rows { arg ...rows ;
//
//	}
//
//	stretch_ { arg val;
//		myStretch = val;
//	}
//}

/////////////////////////// END 3.6.6


+SequenceableCollection {
	asParam { arg self;
		^Param(this)
	}

	asCachedBus {
		^this.at(0).asCachedBus(this.size)
	}

	asBus {
		^this.at(0).asBus(this.size)
	}

	asEnv {
		var val = this.value.deepCopy;
		var first;
		var levels = List.new, times = List.new, curves = List.new;
		var releaseNode, loopNode;
		//val.debug("val");
		val = val.clump(4);
		//val.debug("val");
		first = val.removeAt(0);
		//val.debug("val");
		levels.add(first[0]);
		releaseNode = if(first[2] == -99) { nil } { first[2] };
		loopNode = if(first[3] == -99) { nil } { first[3] };
		//levels.debug("levels");
		
		val.do { arg point, x;
			levels.add( point[0] );
			times.add( point[1] );
			//FIXME: dont know how to do with shape names ???
			curves.add( point[3] );
		};
		//levels.debug("levels");
		//times.debug("times");
		//curves.debug("curves");
		^Env(levels.asArray, times.asArray, curves.asArray, releaseNode, loopNode)
	}
}

+Bus {
	// now a multi-channel bus can be mapped as an array of symbol like patterns expect it
	asMap {
		^mapSymbol ?? {
			if(index.isNil) { MethodError("bus not allocated.", this).throw };
			mapSymbol = if(rate == \control) { "c" } { "a" };
			if(this.numChannels > 1) {
				mapSymbol = numChannels.collect({ arg x;
					(mapSymbol ++ ( index + x)).asSymbol;
				})
			} {
				mapSymbol = (mapSymbol ++ index).asSymbol;
			}
		}
	}
}

+Env {
	asEnv {
		^this
	}

	asStringPrec { arg prec;
		var args;
		args = this.storeArgs;
		args = args.collect({ arg seq; 
			if(seq.isSequenceableCollection) {
				seq.collect({ arg val; val.asFloat.asStringPrec(prec) }) 
			} {
				if(seq.notNil) {
					seq.asFloat.asStringPrec(prec)
				} {
					seq;
				}
			}
		});
		^"%(%)".format(this.class.name, args.join(", "));
	}

}


////////////////////////////////


+SynthDesc {
	*new { arg name;
		if(name.isNil) {
			^super.new
		} {
			^SynthDescLib.global.at(name)
		}
	}

	params { 
		^this.controls.collect { arg control;
			var ret;
			var spec;
			if(control.name == '?') {
				ret = nil;
			} {
				spec = this.getSpec(control.name.asSymbol);
				if(spec.isNil) {
					ret = control.name.asSymbol;
				} {
					ret = [control.name.asSymbol, spec];
				};
			};
			ret
		}.select(_.notNil);
	}

	specs {
		var val;
		val = this.metadata;
		if(val.notNil) {
			val = val.specs;
			if(val.notNil) {
				^val.composeEvent(this.getHalo(\specs))
			} {
				^this.getHalo(\specs)
			}
		} {
			^this.getHalo(\specs)
		}
	}

	defaultValue { arg argname;
		var val;
		var con = this.controlDict[argname];
		if(con.notNil) {
			val = con.defaultValue;
		}
		^val;
	}

	getSpec { arg name;
		var val;
		var rval;
		if(super.getSpec(name).notNil) {
			rval = super.getSpec(name)
		} {
			val = this.metadata;
			if(val.notNil) {
				val = val.specs;
				if(val.notNil) {
					rval = val[name];
				}
			};

		};
		^rval;
	}
	
}
