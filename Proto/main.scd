
/////////////////////////////// main


(
	[

		"default_specs.scd",
		"misc/selector.scd",
		"misc/overlayMenu.scd",
		"misc/SoundFileView.scd",
		//"experimental/eventFactory.scd", // deprecated by player_group
		"player_groups.scd",
		"GlobalLibrary.scd",

		//// editors

		"editors/base_editors.scd",
		"editors/FileDialog.scd",
		"editors/ParamSelectDialog.scd",
		"editors/PatKitDefEditor.scd",
		"editors/PbindefEditor.scd",

		//// timelines

		"timeline/timeline_utils.scd",
		"timeline/timeline.scd",
		"timeline/timeline_gui.scd",
		"timeline/timeline_sample.scd",
		"timeline/timeline_recorders.scd",

		//// tracks
		"tracks/PlayerGrid.scd",
		"tracks/TrackList.scd",
		"tracks/TrackList_base_tracks.scd",
		"tracks/TrackList_gui.scd",
		"tracks/AutomationRack.scd",

		//// others
		//"experimental/drumrack.scd",
		//"experimental/sequencer_score_manager.scd",
		//"experimental/StepEventView.scd",
		"misc/PresetSelectorView.scd",
		//"experimental/PresetSelectorView_compat.scd",
		//"experimental/steptimeline.scd", // fail with ProxySpace
		////"experimental/old_track.scd", // deprecated
		"TrackMixerDef.scd",

		//"experimental/drumrack_synth.scd", // wip and maybe deprecated
	].do({ arg file, idx;
		var path = (ParamProto.base_path +/+ file);
		"-------- loading % (%)".format(path, idx).debug;
		FileSystemProject.load(path);
		//RandDelayNetwork(\deldefault).define_specs[\posthpf].debug(">>> ");
	});

);

