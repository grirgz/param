
Param {
	var key, object, method, spec;
	var wrapper;

	*new  { arg ...args;
		^super.new.init(args)
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

	map { arg msgNum=\all, chan=\all, msgType=\cc, srcID=\all, blockmode;
		MIDIMap(this, srcID, msgType, chan, msgNum, blockmode);
	}

}



BaseParam {

}

NdefParam : BaseParam {
	var target_object, method, spec, key;
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
		target_object.set(method, val)
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
	}

	*new { arg param, msgNum=\all, chan=\all, msgType=\cc, srcID=\all, blockmode;
		var func;
		var path;
		path = [srcID, msgType, chan, msgNum];

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
		]);
	}

	free { arg msgNum=\all, chan=\all, msgType=\cc, srcID=\all;
		var path = [srcID, msgType, chan, msgNum];
		responders.at(*path).free
	}

	get { arg msgNum=\all, chan=\all, msgType=\cc, srcID=\all;
		var path = [srcID, msgType, chan, msgNum];
		responders.at(*path)
	}
	
	*freeAll {
		responders.do { arg resp;
			resp.free;
		};
		responders = MultiLevelIdentityDictionary.new;
	}

	init { arg param, msgNum, chan, msgType, srcID;
		
	}
}


+Slider {
	mapParam { arg param;
		param = param.asParam;
		this.action = { arg self;
			param.set(self.value)
		};
		//param.register(this)
	}
}

+SequenceableCollection {
	asParam { arg self;
		^Param(this)
	}
}
