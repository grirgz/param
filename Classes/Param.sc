
Param {
	var <wrapper;
	var init_args;
	classvar <>defaultSpec;
	classvar <>simpleControllers;

	*initClass {
		List.initClass;
		Spec.initClass;
		ControlSpec.initClass;
		defaultSpec = \widefreq.asSpec;
		simpleControllers = List.new;
	}

	*new  { arg ...args;
		^super.new.init(args)
	}

	*newWrapper { arg wrap;
		^super.new.newWrapper([wrap])
	}

	init { arg args;
		if (args.size > 1) {
			this.newWrapper(args)
		} {
			this.newWrapper(args[0])
		};
	}

	newWrapper { arg args;
		var target = args[0];
		var property = args[1];
		var spec = args[2];
		var envdict;

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


		if(args.size == 2) {
			// no spec given, normalize args size to 3
			args.add(nil)
		};
		//"1".debug;
		init_args = args;
		//"2".debug;
		switch(target.class,
			Ndef, {
				switch(property.class,
					Association, {
						//"Ndef: an asso".debug;
						switch(property.key.class,
							Association, { // index of ((\adsr -> \levels) -> 0)
								var subpro = property.key.value;
								var idx = property.value;
								//"Ndef: a double asso".debug;
								args[1] = property.key.key;
								//(args++[subpro, idx]).debug("NdefParamEnvSlot args");
								wrapper = NdefParamEnvSlot(*args++[subpro, idx]);
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
										wrapper = NdefParamSlot(*args++[idx]);
									}

								)
							}
						);
					},
					Symbol, { // a simple param : \freq
						wrapper = NdefParam(*args);
					}
				);
			},
			Pdef, {
				switch(property.class,
					Association, {
						switch(property.key.class,
							Association, {
								var subpro = property.key.value;
								var idx = property.value;
								args[1] = property.key.key;
								//(args++[subpro, idx]).debug("PdefParamEnvSlot args");
								wrapper = PdefParamEnvSlot(*args++[subpro, idx]);
							},
							// else: an index
							{
								var idx;
								args[1] = property.key;
								idx = property.value;
								//(args++[idx]).debug("PdefParamSlot args");
								wrapper = PdefParamSlot(*args++[idx]);
							}
						);
					},
					Symbol, {
						wrapper = PdefParam(*args);
					}
				);
			}, 
			//Volume, {
			//	wrapper = VolumefParam(args);
			//},
			// else
			{
				// ParamValue goes here
				wrapper = target;
			}
		);
		//"3".debug;
	}

	// why Post << Param doesnt use this method ?
	storeOn { arg stream;
		stream << ("Param.new(\"" ++ this.asLabel ++ "\")");
	}

	asString {
		^this.asLabel
	}

	//storeArgs { arg stream;
	//	//^init_args
	//	stream << ("Param.new(" ++ init_args.asCompileString ++ ")");
	//}
	

	asCompileString {
		^( "Param.new(" ++ init_args.asCompileString ++ ")" );
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
		^switch(this.spec.class,
			XEnvSpec, \env,
			XArraySpec, \array,
			\scalar,
		)
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

	at { arg idx;
		^wrapper.at(idx);
	}

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

	//////// MIDI mapping

	map { arg msgNum, chan, msgType=\control, srcID, blockmode;
		MIDIMap(this, msgNum, chan, msgType, srcID, blockmode);
	}

	unmap { arg msgNum, chan, msgType, srcID, blockmode;
		MIDIMap.free(msgNum, chan, msgType, srcID, blockmode);
	}

	//////// GUI mapping

	//mapSlider { arg slider, action;
	//	var controller;
	//	var param = this;
	//	controller = slider.getHalo(\simpleController);
	//	//controller.debug("11");
	//	if(controller.notNil) {
	//		slider.addHalo(\simpleController, nil);
	//		//debug("notnil:remove simpleController!!");
	//		controller.remove;
	//	};
	//	//debug("11");
	//	if(param.notNil) {
	//		//debug("11x");
	//		param = param.asParam;
	//		//debug("11x");
	//		slider.action = { arg self;
	//			action.value(self);
	//			param.normSet(self.value);
	//			//debug("action!");
	//		};
	//		//debug("11x ========== CREATING!!!!!!!!!!!!");
	//		controller = SimpleController(param.target);
	//		//controller.debug("11x");
	//		slider.addHalo(\simpleController, controller);
	//		//controller.debug("11x");
	//		//controller.put(\set, { arg ...args; slider.value = param.normGet.debug("controolll"); args.debug("args"); });
	//		controller.put(\set, { arg ...args; slider.value = param.normGet; });
	//		slider.value = param.normGet;
	//		//controller.debug("11x");
	//		//slider.onClose = slider.onClose.addFunc({ controller.remove; debug("remove simpleController!!"); });
	//		slider.onClose = slider.onClose.addFunc({ controller.remove; });
	//		//controller.debug("11x");
	//	}
	//}

	makeSimpleController { arg slider, action, updateAction, initAction, customAction;
		var controller;
		var param = this;
		var free_controller = {
			controller.remove; 
			simpleControllers.remove(controller) 
		};
		controller = slider.getHalo(\simpleController);
		//controller.debug("11");
		if(controller.notNil) {
			slider.addHalo(\simpleController, nil);
			//debug("notnil:remove simpleController!!");
			free_controller.();
		};
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
			slider.action = { arg self;
				action.value(self, this);
				customAction.value(self, this);
				//debug("action!");
			};
			controller = SimpleController(param.target);
			slider.onClose = slider.onClose.addFunc( free_controller );
			slider.addHalo(\simpleController, controller);
			simpleControllers.add(controller);

			controller.put(\set, { arg ...args; 
				// args: object, \set, keyval_list
				//args.debug("args");

				// update only if concerned key is set
				// FIXME: may break if property is an association :(
				// FIXME: if a value is equal the key, this fire too, but it's a corner case bug
				// TODO: this is linked to the way the target signals the change, maybe move it to wrapper class
				if(args[2].any({ arg x; x == param.property })) {
					updateAction.(slider, param);
				}
			});
			initAction.(slider, param);
		}
	}

	*freeAllSimpleControllers {
		// used to free all controllers when something break in the GUI and you can't access it to remove the controller
		simpleControllers.do { arg con;
			con.remove
		};
		simpleControllers = List.new;
	}

	mapSlider { arg slider, action;
		this.makeSimpleController(slider, customAction:action);
	}

	mapStaticText { arg view, precision=6;
		this.makeSimpleController(view, {}, { arg view, param;
			var val;
					//param.asLabel.debug("mapStaticText param");
					//param.asCompileString.debug("mapStaticText param");
					//param.type.debug("mapStaticText type");
					//param.get.debug("param get");
			val = param.get;
			if(val.class == Ndef or: {val.class == Symbol or: {val.class == String}}) {
				// the parameter is mapped to a Ndef
				{
					view.string = val;
				}.defer;
			} {
				switch(param.type,
					\scalar, {
						{
							view.string = val.asFloat.asStringPrec(precision);
						}.defer;
					},
					\array, {
						//param.debug("mapStaticText param");
						//param.get.debug("param get");
						{
							view.string = val.collect({ arg x; x.asFloat.asStringPrec(precision) });
						}.defer;
					},
					\env, {
						{
							view.string = val.asCompileString;
						}.defer;
					}
				);
			};
		}, nil, nil)
	}

	mapStaticTextLabel { arg view;
		this.makeSimpleController(view, {}, {}, { arg view, param;
			{
				view.string = param.asLabel;
			}.defer;
		}, nil)
	}

	mapTextField { arg view, action;
		this.makeSimpleController(view, { arg view, param;
			param.set(view.value.asFloat);
		}, { arg view, param;
			{
				view.value = param.get;
			}.defer;
		}, nil, action)
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
		if(mapLabel == true) {
			view.labelView.mapParamLabel(param);
		}
	}

	*unmapEZKnob { arg view, mapLabel = true;
		var param = this;
		view.knobView.unmapParam;
		view.numberView.unmapParam;
		if(mapLabel == true) {
			view.labelView.unmapParam;
		}
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

	*unmapView { arg view;
		var controller;
		var free_controller = {
			controller.remove; 
			simpleControllers.remove(controller) 
		};
		controller = view.getHalo(\simpleController);
		view.action = nil;
		if(controller.notNil) {
			view.addHalo(\simpleController, nil);
			free_controller.();
		};
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

	asMultiSlider {
		^MultiSliderView.new.elasticMode_(1).size_(this.numChannels).mapParam(this);
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
		var win;
		var label, widget, val;
		win = Window.new;

		label = this.asStaticTextLabel;
		label.align = \center;

		widget = this.asView;

		val = this.asStaticText;
		//val.background = Color.red;
		val.align = \center;

		win.layout = VLayout.new(
			label, 
			widget, 
			val
		);
		win.alwaysOnTop = true;
		win.front;
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

}


BaseParam {

}

NdefParam : BaseParam {
	var <target, <property, <spec, <key;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	asLabel {
		^"Ndef % %".format(target.key, property)
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
			^spec.unmap(this.get)
		}
	}

	normSet { arg val;
		this.set(spec.map(val))
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
		^"Ndef % % %".format(target.key, this.property, index)
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
		var vals = super.get;
		vals[index] = val;
		super.set(vals);
	}

	get {
		var vals = super.get;
		^vals[index];
	}
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
		^"Ndef % % %%".format(target.key, this.property, subproperty.asString[0], index)
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
		^"Pdef % %".format(target.key, property)
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
				xtarget.getSpec(xproperty) ?? {
					var mysp;
					// instrument metadata spec
					instr = PdefParam.instrument(xtarget);
					if(instr.notNil) {
						mysp = Param.getSynthDefSpec(xproperty, instr);
						// arg name in Spec
						mysp ?? {
							// arg name in Spec
							xproperty.asSpec ?? {
								// default value in SynthDef
								Param.getSynthDefDefaultValue(xproperty, instr) ?? {
									Param.defaultSpec
								}
							}
						}
					} {
						// arg name in Spec
						xproperty.asSpec ?? {
							// default value in Pdef
							var myval = xtarget.getVal(xproperty);
							if(myval.notNil) {
								Param.valueToSpec(myval);
							} {
								// default spec
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
	
	at { arg idx;
		^Param(target, property -> idx, spec)
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
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
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
		^"Pdef % % %".format(target.key, this.property, index)
	}

	pdefParamSlotInit { arg idx;
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
		^"Pdef % % %%".format(target.key, this.property, subproperty.asString[0], index)
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


////////////////////////////////////////



MIDIMap {
	classvar <responders;
	classvar responders_param;
	classvar <mapped_views; // debug: added getter
	classvar midivalues;
	classvar <controls;
	classvar <>permanent = true;
	classvar <>defaultBlockmode = false;

	// NO THIS IS FALSE: path type: [srcID, msgType, chan, msgNum]
	// path type: [msgNum, chan, msgType, srcID]
	
	*initClass {
		MultiLevelIdentityDictionary.initClass;
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
				myblockmode.debug("BLOCKMODE");
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
						if((normval - midival).abs < ( 1/126 )) {
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
			controls[key] = [val, channel, kind, source_uid];
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

	// morphers format : Dict[ \name -> (val: 0.5, presets:[\preset1, \preset2]) ]
	// - morphTo(\name, 0.3)
	// - addMorphing(\name)
	// - morphTo([\preset1, \preset2], 0.4)
	*new { arg anArray;
		var inst;
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
		var win;
		var hlayout = HLayout.new;
		win = Window.new;

		this.collect({ arg param;
			var label, widget, val;

			label = param.asStaticTextLabel;
			label.align = \center;

			widget = param.asView;

			val = param.asStaticText;
			//val.background = Color.red;
			val.align = \center;

			hlayout.add(VLayout.new(
				label, 
				widget, 
				val
			), stretch:1);
		});

		win.layout = hlayout;
		win.alwaysOnTop = true;
		win.front;
	}

}

ParamGroupDef {
	classvar <lib;
	var <key;
	var <group;

	*initClass {
		IdentityDictionary.initClass;
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
				val.class.debug("val class");
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

}

// act also as a Param wrapper
ParamValue {
	var <>value=0;
	var <>spec, <property=\value, <target;

	*new { arg spec;
		^super.new.init(spec);
	}

	init { arg spec;
		target = this;
		property = \value;
		spec = spec;
	}

	get {
		^value;
	}

	set { arg val;
		value = val;
		this.changed(\set, [\value, val]);
	}

	// normGet and normSet are not used by morpher because of additional code on setter/getter
	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}
}

// act also as a Param
ParamMorpher : Param {
	var group, presets;
	var <>key;
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

	set { arg val;
		var presets_vals;
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
		group.do({ arg param, x;
			var resval;
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
		IdentityDictionary.initClass;
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

////////////////////////////////////////

CachedBus : Bus {
	classvar cache;

	*initClass {
		IdentityDictionary.initClass;
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

XEnvelopeView : QEnvelopeView {
	var curves;
	var <timeScale = 1;
	var duration;
	var envDur;
	var rawValue;
	var rawGrid;
	var autoTimeScale = true;
	var <totalDur = 8;
	var <loopNode, <releaseNode;

	curves {
		^curves
	}

	curves_ { arg xcurves;
		//curves.debug("curves::");
		curves = xcurves;
		super.curves = xcurves;
	}

	valueXY_ { arg val;
		val = val.deepCopy;
		envDur = val[0].last;
		if(envDur > totalDur) {
			totalDur = envDur;
		};
		val[0] = val[0] / totalDur;
		super.value = val;
		this.updateGrid;
	}

	valueXY { 
		var val = super.value.deepCopy; // deepCopy to avoid messing with internal value
		val[0] = val[0] * totalDur;
		^val;
	}

	value {
		^this.getEnv;
	}

	value_ { arg val;
		this.setEnv(val);
	}

	zoomFit {
		var val = this.valueXY;
		envDur = val[0].last;
		this.totalDur = envDur;
	}

	totalDur_ { arg newdur;
		// FIXME: if valueXY is nil, crash
		var curval = this.valueXY.deepCopy;
		totalDur = newdur;
		this.valueXY = curval;
	}

	setEnv { arg env;
		var times = [0] ++ env.times.integrate;
		this.valueXY = [times, env.levels];
		this.curves = env.curves;
		loopNode = env.loopNode;
		releaseNode = env.releaseNode;
	}

	grid_ { arg val;
		rawGrid = val;
		this.updateGrid;
	}

	grid {
		^rawGrid;
	}

	updateGrid {
		rawGrid = rawGrid ? Point(1/8,1/8);
		super.grid = Point( rawGrid.x / totalDur,rawGrid.y)
	}

	getEnv { arg val;
		var curves;
		var times;
		var levels;
		var env;
		if(val.isNil) {
			val = this.valueXY;
		};
		times = val[0];
		times = times.drop(1);
		times = times.differentiate;
		levels = val[1];
		curves = this.curves;
		env = Env.new(levels, times, curves, releaseNode, loopNode);
		^env
	}

	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this, { arg self;
			var val = self.valueXY;
			// prevent the first node from moving
			if( val[0][0] != 0) {
				val[0][0] = 0;
				self.valueXY = val;
			};
		});
	}
}

XStaticText : QStaticText {
	value {
		this.string;
	}

	value_ { arg val;
		this.string = val
	}
}

XSimpleButton : QButton {
	var <color, <label, <background, myValue;

	color_ { arg val;
		color = val;
		this.updateStates;
	}

	background_ { arg val;
		background = val;
		this.updateStates;
	}

	label_ { arg val;
		label = val;
		this.updateStates;
	}

	value_ { arg val;
		myValue = val;
	}

	value { arg val;
		^myValue;
	}

	updateStates {
		this.states = [[label, color, background]];
	}

}

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

	asParamGroup { arg instrument, notes=true;
		// TODO: get synthdef parameters and generate a ParamGroup with all parameters
		// the second parameter (find a better name) is for adding \dur and \legato
	
	}
}

+Ndef {
	asParamGroup { arg instrument, notes=true;
		// TODO: get Ndef parameters and generate a ParamGroup with all parameters
		// the second parameter (find a better name) is for adding volume
	
	}

	isNaN {
		// used to avoid NumberBox and EZ* GUI to throwing an error
		^true
	}

}

+String {
	isNaN {
		// used to avoid NumberBox and EZ* GUI to throwing an error
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
		param.mapSlider(this);
	}
}

+StaticText {
	unmapParam {
		Param.unmapSlider(this);
		this.string = "-";
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
		param.mapSlider(this);
	}
}

+QStaticText {
	unmapParam {
		Param.unmapSlider(this);
		this.string = "-";
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
		this.value = "";
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
		^Env(levels, times, curves, releaseNode, loopNode)
	}
}

+Bus {
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
}
