"init PresetSelectorView_compat.scd".postln;


EventClass.presetSelectorView = { arg pg;
	WindowDef(\PresetSelectorView).asView(pg);
};

EventClass.presetListMorpherView = { arg pg, size=8, features;
	WindowDef(\PresetListMorpherView).asView(pg, size, features)
};
