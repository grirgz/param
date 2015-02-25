
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

}

BaseParam {

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
	asParam: { arg self;
		Param(this)
	}
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
	*new { arg param, msgNum, chan, msgType, srcID;
		^super.new.init(param, msgNum, chan, msgType, srcID)
	}

	init { arg param, msgNum, chan, msgType, srcID;
		
	}
}
