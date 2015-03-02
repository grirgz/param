
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
		//object = args[0];
		//property = args[1];
		//spec = args[2];
		switch(args[0].class,
			Ndef, {
				wrapper = NdefParam(*args);
			},
			Pdef, {
				switch(args[1].class,
					Association, {
						var idx;
						var asso = args[1];
						args[1] = asso.key;
						idx = asso.value;
						wrapper = PdefParamSlot(*args++[idx]);
					},
					Symbol, {
						wrapper = PdefParam(*args);
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

	mapSlider { arg slider;
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
		^MultiSliderView.new.size_(this.numChannels).mapParam(this);
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
			try { 
				spec = if( SynthDescLib.global.synthDescs[defname].metadata.specs[argName].notNil, {
					var sp;
					sp = SynthDescLib.global.synthDescs[defname].metadata.specs[argName];
					sp.asSpec;
				})
			};
			spec = this.toSpec(spec, argName, default_spec);
		};
		^spec;
	}

}



BaseParam {

}

NdefParam : BaseParam {
	var <target, <property, <spec, <key;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
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
		^target.get(property)
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

	// retrieve default spec if no default spec given
	toSpec { arg spec;
		var instr = target.getHalo(\instrument);
		spec = Param.toSynthDefSpec(spec, property, instr);
		^spec.asSpec;
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

PdefParamSlot : PdefParam {
	var <index;

	*new { arg obj, meth, sp, index;
		var inst;
		obj.debug("obj");
		inst = super.new(obj, meth, sp);
		inst.pdefParamSlotInit(index);
		^inst;
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

