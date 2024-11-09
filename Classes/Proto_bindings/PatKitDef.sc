
//////////////////////////////

PatKitDef {
	*new { arg name, val;
		ParamProto.init;
		^ProtoClassDef(\PatKitDef).new(name, val);
	}

	*all {
		ParamProto.init;
		^ProtoClassDef(\PatKitDef).all;
	}
}
