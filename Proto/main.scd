
/////////////////////////////// main


(
	[

		"default_specs.scd",
		"misc/pdemul.scd",
		"misc/selector.scd",
		"misc/overlayMenu.scd",
		"experimental/eventFactory.scd",
		"player_groups.scd",
		"GlobalLibrary.scd",
		"timeline_utils.scd",
		"timeline.scd",
		"timeline_sample.scd",
		"timeline_recorders.scd",
		"experimental/drumrack.scd",
		"experimental/sequencer_score_manager.scd",
		"StepEventView.scd",
		"PresetSelectorView.scd",
		"experimental/PresetSelectorView_compat.scd",
		"experimental/steptimeline.scd",
		//"experimental/old_track.scd", // deprecated
		"TrackMixerDef.scd",

		"experimental/drumrack_synth.scd",
	].do({ arg file;
		var path = (ParamProto.base_path +/+ file);
		("loading " ++ path).debug;
		FileSystemProject.load(path)
	});

);

