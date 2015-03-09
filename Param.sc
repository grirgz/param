
Param {
	var <wrapper;

	*new  { arg ...args;
		^super.new.init(args)
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

	init { arg args;
		if (args.size > 1) {
			this.newWrapper(args)
		} {
			this.newWrapper(args[0])
		};
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


	newWrapper { arg args;
		var target = args[0];
		var property = args[1];
		var spec = args[2];
		switch(target.class,
			Ndef, {
				wrapper = NdefParam(*args);
			},
			Pdef, {
				switch(property.class,
					Association, {
						var idx;
						var asso = property;
						args[1] = asso.key;
						idx = asso.value;
						wrapper = PdefParamSlot(*args++[idx]);
					},
					Symbol, {
						switch(PdefParam.toSpec(spec, target, property).class.debug("deja, WTF, spec"),
							XEnvSpec, {
								//wrapper = PdefEnvParam(*args);
								wrapper = PdefParam(*args);
							}, 
							// else
							{
								wrapper = PdefParam(*args);
							}
						);
					}
				)
			},
			//Volume, {
			//	wrapper = VolumefParam(args);
			//},
		);
		^wrapper;
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

	map { arg msgNum, chan, msgType=\control, srcID, blockmode;
		MIDIMap(this, msgNum, chan, msgType, srcID, blockmode);
	}

	unmap { arg msgNum, chan, msgType, srcID, blockmode;
		MIDIMap.free(msgNum, chan, msgType, srcID, blockmode);
	}

	mapSlider { arg slider, action;
		var controller;
		var param = this;
		controller = slider.getHalo(\simpleController, controller);
		controller.debug("11");
		if(controller.notNil) {
			slider.addHalo(\simpleController, nil);
			debug("notnil:remove simpleController!!");
			controller.remove;
		};
		debug("11");
		if(param.notNil) {
			debug("11x");
			param = param.asParam;
			debug("11x");
			slider.action = { arg self;
				action.value(self);
				param.normSet(self.value);
				debug("action!");
			};
			debug("11x ========== CREATING!!!!!!!!!!!!");
			controller = SimpleController(param.target);
			controller.debug("11x");
			slider.addHalo(\simpleController, controller);
			controller.debug("11x");
			controller.put(\set, { arg ...args; slider.value = param.normGet.debug("controolll"); args.debug("args"); });
			slider.value = param.normGet;
			controller.debug("11x");
			slider.onClose = slider.onClose.addFunc({ controller.remove; debug("remove simpleController!!"); });
			controller.debug("11x");
		}
	}

	makeSimpleController { arg slider, action, updateAction, customAction;
		var controller;
		var param = this;
		controller = slider.getHalo(\simpleController, controller);
		controller.debug("11");
		if(controller.notNil) {
			slider.addHalo(\simpleController, nil);
			debug("notnil:remove simpleController!!");
			controller.remove;
		};
		if(action.isNil) {
			action = { arg self;
				param.normSet(self.value);
			}
		};
		if(updateAction.isNil) {
			updateAction = { arg self;
				self.value = param.normGet.debug("controolll")
			}
		};
		if(param.notNil) {
			param = param.asParam;
			slider.action = { arg self;
				action.value(self);
				customAction.value(self);
				param.normSet(self.value);
				debug("action!");
			};
			controller = SimpleController(param.target);
			slider.addHalo(\simpleController, controller);
			controller.put(\set, { arg ...args; updateAction.(slider, param) });
			updateAction.(slider, param);
			slider.onClose = slider.onClose.addFunc({ controller.remove; debug("remove simpleController!!"); });
		}
	}

	mapStaticTextValue { arg view, action, precision=6;
		this.makeSimpleController(view, nil, { arg view, param;
			view.value = param.get.asFloat.asStringPrec(precision);
		}, action)
	}

	mapStaticTextLabel { arg view, action, precision=6;
		this.makeSimpleController(view, nil, { arg view, param;
			view.value = param.asLabel;
		}, action)
	}


	*unmapSlider { arg slider;
		var controller;
		controller = slider.getHalo(\simpleController);
		slider.action = nil;
		if(controller.notNil) {
			slider.addHalo(\simpleController, nil);
			controller.remove;
		};
	}

	asSlider {
		^Slider.new.mapParam(this);
	}

	asKnob {
		^Knob.new.mapParam(this);
	}

	asMultiSlider {
		^MultiSliderView.new.elasticMode_(1).size_(this.numChannels).mapParam(this);
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
		var widget;
		win = Window.new;
		widget = this.asView;
		win.layout = HLayout.new(widget);
		win.alwaysOnTop = true;
		win.front;
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
		"getSynthDefDefaultValue 1".debug;
		desc = SynthDescLib.global.synthDescs[defname];
		desc.debug("getSynthDefDefaultValue 2");
		val = if(desc.notNil) {
			var con = desc.controlDict[argName];
		con.debug("getSynthDefDefaultValue 4");
			if(con.notNil) {
		"getSynthDefDefaultValue 5".debug;
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
		sp.debug("sp1");
		spec = this.toSpec(sp);
		key = obj.key;
	}

	// retrieve default spec if no default spec given
	toSpec { arg sp;
		sp.debug("sp2");
		sp = sp ? target.getSpec(property);
		sp.debug("sp3");
		sp = Param.toSpec(sp, property);
		sp.debug("sp4");
		^sp.asSpec;
	}

	get {
		var val;
		val = target.get(property);
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
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
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
		};
		^val;
	}

	instrument { 
		^PdefParam.instrument(target)
	}

	// retrieve default spec if no default spec given
	*toSpec { arg xspec, xtarget, xproperty;
		var instr = PdefParam.instrument(xtarget);
		xspec = Param.toSynthDefSpec(xspec, xproperty, instr);
		^xspec.asSpec;
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
		obj.debug("obj");
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
		spec.at(index);
	}

	set { arg val;
		var vals = target.getVal(property);
		vals[index] = val;
		target.setVal(property, vals);
	}

	get {
		var vals = target.getVal(property);
		^vals[index];
	}
}

PdefEnvParam : PdefParam {
	// not used currently
	var <target, <property, <spec, <key;
	var <multiParam = false;
	*new { arg obj, meth, sp;
		^super.new(obj, meth, sp).pdefEnvParamInit(obj, meth, sp);
	}

	pdefEnvParamInit { arg obj, meth, sp;
		target = obj;
		property = meth;
		spec = this.toSpec(sp);
		key = obj.key;
		multiParam = true;
	}

	// retrieve default spec if no default spec given
	toSpec { arg spec;
		var instr = target.getHalo(\instrument);
		spec = Param.toSynthDefSpec(spec, property, instr);
		^spec.asSpec;
	}

	*toSpec { arg xspec, xtarget, xproperty;
		var instr = xtarget.getHalo(\instrument);
		xspec = Param.toSynthDefSpec(xspec, xproperty, instr);
		^xspec.asSpec;
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
		^target.getVal(property)
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

////////////////////////////////////////

ParamGroup : List {
	*new { arg anArray;
		^super.new.setCollection( anArray.collect(_.asParam) )
	}
}


MIDIMap {
	classvar responders;
	classvar midivalues;
	
	*initClass {
		responders = MultiLevelIdentityDictionary.new;
		midivalues = MultiLevelIdentityDictionary.new;
		//params = Dictionary.new;
	}

	*new { arg param, msgNum=nil, chan=nil, msgType=\control, srcID=nil, blockmode;
		var func;
		var path;
		path = [srcID, msgType, chan, msgNum];
		path = path.collect({ arg x; if(x.isNil) { \all } { x } }); // can't have nil has dict key

		func = { arg val, num, chan, src;
			val = val/127;
			if(blockmode.isNil) {
				param.normSet(val);
				midivalues.put(*path++[val]);
			};
		};

		if(responders.at(*path).notNil) {
			responders.at(*path).free
		};
		responders.put(*path ++ [
			MIDIFunc(func, msgNum, chan, msgType, srcID).permanent_(true)
			//params[param] =	params[param].add( path );
		]);
	}


	*unmap { arg param;
		// TODO
		//params[param]
	}
	
	*free { arg msgNum, chan, msgType=\control, srcID;
		var path = [srcID, msgType, chan, msgNum];
		path = path.collect({ arg x; if(x.isNil) { \all } { x } });
		responders.at(*path).free
	}

	*get { arg msgNum, chan, msgType=\control, srcID;
		var path = [srcID, msgType, chan, msgNum];
		path = path.collect({ arg x; if(x.isNil) { \all } { x } });
		responders.at(*path)
	}
	
	*freeAll {
		responders.do { arg resp;
			resp.free;
		};
		responders = MultiLevelIdentityDictionary.new;
	}

}

////////////////////////////////////////

CachedBus : Bus {
	classvar cache;

	*initClass {
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

	curves {
		^curves
	}

	curves_ { arg xcurves;
		curves.debug("curves::");
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
		super.grid = Point( rawGrid.x / totalDur,rawGrid.y).debug("grid")
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
		env = Env.new(levels, times, curves);
		^env
	}

	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this, { arg self;
			var val = self.valueXY;
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
				map.debug("get_bus_from_map: error, not a bus");
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
		"whatktktj".debug;
		if(enable) {
			"1whatktktj".debug;
			if(this.inBusMode(key)) {
				// NOOP
				"2whatktktj".debug;
			} {
				var val = this.getVal(key);
				var numChannels = 1;
				var bus;
				"3whatktktj".debug;
				val.debug("setBusMode: val");
				if(val.isSequenceableCollection) {
					numChannels = val.size;
				};
				numChannels.debug("setBusMode: nc");
				bus = CachedBus.control(Server.default,numChannels );
					// FIXME: hardcoded server
					// hardcoded rate, but can't convert audio buffer to a number, so it's ok
				bus.debug("setBusMode: bus");
				if(val.isSequenceableCollection) {
					bus.setn(val);
				} {
					bus.set(val);
				};
				val.debug("setBusMode: val");
				this.set(key, this.nestOn(bus.asMap));
				bus.asMap.debug("setBusMode: busmap");
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
		curval = this.get(key);
		curval = this.nestOff(curval);
		if(this.inBusMode(key)) {
			var bus = curval.asCachedBus;
			^bus.getCached;
		} {
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
				}
			} {
				this.set(key, this.nestOn(val))
			};
		}
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
		val.debug("val");
		val = val.clump(4);
		val.debug("val");
		first = val.removeAt(0);
		val.debug("val");
		levels.add(first[0]);
		releaseNode = if(first[2] == -99) { nil } { first[2] };
		loopNode = if(first[3] == -99) { nil } { first[3] };
		levels.debug("levels");
		
		val.do { arg point, x;
			levels.add( point[0] );
			times.add( point[1] );
			//FIXME: dont know how to do with shape names ???
			curves.add( point[3] );
		};
		levels.debug("levels");
		times.debug("times");
		curves.debug("curves");
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
