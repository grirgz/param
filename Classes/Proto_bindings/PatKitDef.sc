
//////////////////////////////

PatKitDef {
	*new { arg name, val;
		ParamProto.init;
		^~patKitDef.new(name, val);
	}

	*all {
		ParamProto.init;
		^~patKitDef.all;
	}
}
