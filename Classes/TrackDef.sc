
TrackDef : ProtoDef {
	// just another placeholder to manage players and mixers

	*defaultTemplateDictionary { ^\TrackTemplateDef }

	*all {
		^PresetDictionary.new(\TrackDef);
	}
}

TrackTemplateDef : TrackDef {
	// just another placeholder to distinguish tracks proto-classes and proto-instances
	*all {
		^PresetDictionary.new(\TrackTemplateDef);
	}
}

TrackMixerDef : ProtoDef {
	// FIXME: redirect to defined protoclass or be a placeholder ?
	//		or defined protoclass could be the default ? but constructor signature will be different than TrackDef

	*new { arg key ...args;
		ParamProto.init;
		^ProtoInst(\TrackMixerDef, key, *args)
	}

	*all {
		^PresetDictionary.new(\TrackMixerDef);
	}
}

