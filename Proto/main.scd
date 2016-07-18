
/////////////////////////////// main


(
	[
		"base.scd",
		"selector.scd",
		"eventFactory.scd",
		"timeline.scd",
		"drumrack.scd",
		"stepeditor.scd",
		"event.scd",
		"preset.scd",
		"steptimeline.scd",
		//"track.scd", // deprecated
		"mixer.scd",

		"synth.scd",
	].do({ arg file;
		var path = (ParamProto.base_path +/+ file);
		("loading " ++ path).debug;
		path.load
	});

);

