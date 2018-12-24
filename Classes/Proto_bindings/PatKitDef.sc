
//////////////////////////////

PatKitDef {
	*new { arg name, val;
		ParamProto.init;
		^~patKitDef.new(name, val);
	}

	*all {
		^~patKitDef.all;
	}
}
