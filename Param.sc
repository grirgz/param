
Param {
	var wrapper;

	*new  { arg ...args;
		^super.new.init(args)
	}

	== { arg param;
		( this.wrapper.method == param.wrapper.method)
		and: { this.wrapper.target_object == param.wrapper.target_object } 
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

	target_object {
		^wrapper.target_object;
	}

	method {
		^wrapper.method;
	}

	spec {
		^wrapper.spec;
	}


	newWrapper { arg args;
		//object = args[0];
		//method = args[1];
		//spec = args[2];
		switch(args[0].class,
			Ndef, {
				wrapper = NdefParam(*args);
			},
			//Pdef, {
			//	wrapper = PdefParam(args);
			//},
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

}



BaseParam {

}

NdefParam : BaseParam {
	var <target_object, <method, <spec, <key;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}
	
	init { arg obj, meth, sp;
		target_object = obj;
		method = meth;
		spec = sp.asSpec;
		key = obj.key;
	}

	get {
		^target_object.get(method)
	}

	set { arg val;
		target_object.set(method, val);
		//target_object.changed(\set, method); // already exists in Ndef
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


+Slider {
	unmapParam {
		var controller;
		controller = this.getHalo(\simpleController, controller);
		if(controller.notNil) {
			this.addHalo(\simpleController, nil);
			controller.remove;
		};
	}

	mapParam { arg param;
		var controller;
		controller = this.getHalo(\simpleController, controller);
		controller.debug("11");
		if(controller.notNil) {
			this.addHalo(\simpleController, nil);
			debug("notnil:remove simpleController!!");
			controller.remove;
		};
		debug("11");
		if(param.notNil) {
			debug("11x");
			param = param.asParam;
			debug("11x");
			this.action = { arg self;
				param.normSet(self.value);
				debug("action!");
			};
			debug("11x");
			controller = SimpleController(param.target_object);
			controller.debug("11x");
			this.addHalo(\simpleController, controller);
			controller.debug("11x");
			controller.put(\set, { arg ...args; this.value = param.normGet.debug("controolll") });
			controller.debug("11x");
			this.parent.debug("parent");
			if(this.parent.isNil)
			//this.parent.onClose = this.parent.onClose.add({ controller.remove; debug("remove simpleController!!"); });
			controller.debug("11x");
		}
	}
}

+SequenceableCollection {
	asParam { arg self;
		^Param(this)
	}
}
