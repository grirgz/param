
Pkeyd : Pattern {
	var	<>key, <>default;
	var <>repeats;
	*new { |key, default|
		^super.newCopyArgs(key, default)
	}
	storeArgs { ^[key] }
		// avoid creating a routine
	asStream {
		var	keystream = key.asStream;
		var	defaultstream = default.asStream;
		^FuncStream({ |inevent|
			inevent !? { inevent[keystream.next(inevent)] ? default.next(inevent) }
		});
	}
}

