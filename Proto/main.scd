
/////////////////////////////// main


(
	[
		"base.scd",
		"timeline.scd",
		"drumrack.scd",
		"stepeditor.scd",
		"event.scd",
		"preset.scd",

		"synth.scd",
	].do({ arg file;
		(ParamProto.base_path +/+ file).load
	});

);

