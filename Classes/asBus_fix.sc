
+Symbol {
	asBus { arg numChannels=1, busclass;
		var rate;
		var index;
		var map;
		busclass = busclass ? Bus;
		map = this;
		map = map.asString;
		switch(map[0],
			$c, {
				rate = \control;
			},
			$a, {
				rate = \audio;
			}, {
				"get_bus_from_map: error, not a bus: %".format(map).postln;
			}
		);
		index = map[1..].asInteger;
		^busclass.new(rate, index, numChannels, Server.default);
	}

	asCachedBus { arg numChannels=1;
		^this.asBus(numChannels, CachedBus);
	}

}

+String {
	asBus { arg numChannels=1, busclass;
		var rate;
		var index;
		var map;
		busclass = busclass ? Bus;
		map = this;
		map = map.asString;
		switch(map[0],
			$c, {
				rate = \control;
			},
			$a, {
				rate = \audio;
			}, {
				"get_bus_from_map: error, not a bus: %".format(map).postln;
			}
		);
		index = map[1..].asInteger;
		^busclass.new(rate, index, numChannels, Server.default);
	}

	asCachedBus { arg numChannels=1;
		^this.asBus(numChannels, CachedBus);
	}

}

