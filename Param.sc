
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


	newWrapper { arg args;
		//object = args[0];
		//property = args[1];
		//spec = args[2];
		switch(args[0].class,
			Ndef, {
				^wrapper = NdefParam(*args);
			},
			Pdef, {
				^wrapper = PdefParam(*args);
			},
			//Volume, {
			//	wrapper = VolumefParam(args);
			//},
		);
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
		Slider.new.mapParam(this);
	}

	asKnob {
		Knob.new.mapParam(this);
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

	*getSynthDefSpec { arg argName, defname=nil, default_spec=\widefreq;
		var spec = nil;
		try { 
			spec = if( SynthDescLib.global.synthDescs[defname].metadata.specs[argName].notNil, {
				var sp;
				sp = SynthDescLib.global.synthDescs[defname].metadata.specs[argName];
				sp.asSpec;
			})
		};
		spec = this.toSpec(spec, argName, default_spec);
		^spec;
	}

}



BaseParam {

}

NdefParam : BaseParam {
	var <target, <property, >spec, <key;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}
	
	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		spec = sp.asSpec;
		key = obj.key;
	}

	// retrieve default spec if no default spec given
	toSpec { arg spec;
		if(spec.isNil) {
			spec = self.target.getSpec(property);
			spec = Param.toSpec(spec, property);
		};
		^spec;
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
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}
	
	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		spec = sp.asSpec;
		key = obj.key;
	}

	setBusMode { arg enable=true, free=true;
		if(enable) {
			if(this.inBusMode) {
				// NOOP
			} {
				var val = this.getRaw;
				var numChannels = 1;
				var bus;
				val.debug("setBusMode: val");
				if(val.isSequenceableCollection) {
					numChannels = val.size;
				};
				numChannels.debug("setBusMode: nc");
				bus = CachedBus.control(Server.default,numChannels );
					// FIXME: hardcoded server
					// hardcoded rate, but can't convert audio buffer to a number, so it's ok
				bus.debug("setBusMode: bus");
				bus.set(val);
				val.debug("setBusMode: val");
				this.setRaw(bus.asMap);
				bus.asMap.debug("setBusMode: busmap");
			}
		} {
			if(this.inBusMode.not) {
				// NOOP
			} {
				var map = this.getRaw;
				var numChannels = 1;
				var bus;
				bus = map.asBus;
				this.setRaw(bus.getCached);
				if(free) {
					bus.free;
				};
			}

		}

	}

	inBusMode {
		^( this.getRaw.class == Symbol)
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
		} {
			val = values;
		};
		cache[this.rate][this.index] = val;
		super.set(*values);
	}

	getCached {
		if(cache[this.rate][this.index].isNil) {
			this.get({ arg x; cache = x });
			^0
		} {
			^cache[this.rate][this.index]
		}
	}

}


+Symbol {
	asBus { arg busclass;
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
		//FIXME: hardcoded numchannels
		^busclass.new(rate, index, 1, Server.default);
	}

	asCachedBus {
		^this.asBus(CachedBus);
	}

}

+Pdef {

	getVal { arg key;
		var curval;
		curval = this.get(key);
		if(curval.class == Symbol) {
			var bus = curval.asCachedBus;
			^bus.getCached;
		} {
			^this.get(key);
		};

	}

	setVal { arg key, val;
		var curval;
		curval = this.get(key);
		if(curval.class == Symbol) {
			var bus = curval.asCachedBus;
			bus.set(val);
		} {
			this.set(key, val)
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

+SequenceableCollection {
	asParam { arg self;
		^Param(this)
	}
}
