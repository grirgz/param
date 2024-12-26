
/////////////////////////////// main


(
	[

		"default_specs.scd",
		"misc/selector.scd",
		"misc/overlayMenu.scd",
		//"experimental/eventFactory.scd", // deprecated by player_group
		"player_groups.scd",
		"GlobalLibrary.scd",
		"editors.scd",
		"misc/FileDialog.scd",
		"misc/ParamSelectDialog.scd",
		"timeline/timeline_utils.scd",
		"timeline/timeline.scd",
		"timeline/timeline_gui.scd",
		"timeline/timeline_sample.scd",
		"timeline/timeline_recorders.scd",
		"timeline/AutomationRack.scd",
		//"experimental/drumrack.scd",
		//"experimental/sequencer_score_manager.scd",
		//"experimental/StepEventView.scd",
		"misc/PresetSelectorView.scd",
		"experimental/PresetSelectorView_compat.scd",
		//"experimental/steptimeline.scd", // fail with ProxySpace
		////"experimental/old_track.scd", // deprecated
		"TrackMixerDef.scd",

		//"experimental/drumrack_synth.scd", // wip and maybe deprecated
	].do({ arg file;
		var path = (ParamProto.base_path +/+ file);
		("loading " ++ path).debug;
		FileSystemProject.load(path);
		//RandDelayNetwork(\deldefault).define_specs[\posthpf].debug(">>> ");
	});

);

