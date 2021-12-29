
SimpleServerMeterView : SCViewHolder {
	// TODO: add player interface (isPlaying)

	classvar serverMeterViews; 
	classvar serverCleanupFuncs;

	var inresp, outresp, synthFunc, responderFunc, server, numIns, numOuts, inmeters, outmeters, startResponderFunc;
	var <bus;
	var <>updateFreq = 10, <>dBLow = -80, <>meterWidth = 15, <>gapWidth = 4, <>meterHeight = 180;

	*new { |aserver, numIns=0, numOuts=2, bus|
		^super.new.initSimpleServerMeterView(aserver, numIns, numOuts, bus)
	}

	//*getWidth { arg numIns, numOuts, server;
		//^20+((numIns + numOuts + 2) * (meterWidth + gapWidth))
	//}

	bus_ { arg val;
		bus = val;
		this.stop;
		this.setSynthFunc(inmeters, outmeters);
		this.start;
	}

	makeLayout {
		var levelIndic;

		// dB scale

		if(numIns > 0) {
			// ins
			inmeters = Array.fill( numIns, { arg i;
				levelIndic = LevelIndicator( nil, Rect(0, 0, meterWidth, meterHeight) ).warning_(0.9).critical_(1.0)
				.drawsPeak_(true)
				.numTicks_(9)
				.numMajorTicks_(3);
			});
		};

		// outs
		if(numOuts > 0) {
			outmeters = Array.fill( numOuts, { arg i;
				levelIndic = LevelIndicator( nil, Rect(0, 0, meterWidth, meterHeight) ).warning_(0.9).critical_(1.0)
				.drawsPeak_(true)
				.numTicks_(9)
				.numMajorTicks_(3);
			});
		};
		this.view = View.new;
		this.view.layout = HLayout(
			HLayout(* inmeters).margins_(0),
			HLayout(* outmeters).margins_(0),
		).margins_(5);
	}

	initSimpleServerMeterView { arg aserver, anumIns, anumOuts, abus;
		var innerView, viewWidth, palette;

		server = aserver ?? { Server.default };

		if(abus.isKindOf(Bus)) {
			abus = abus.index;
		};
		bus = abus ? 0;

		numIns = anumIns ?? { server.options.numInputBusChannels };
		numOuts = anumOuts ?? { server.options.numOutputBusChannels };

		this.makeLayout;

		this.setSynthFunc(inmeters, outmeters);
		startResponderFunc = {this.startResponders};
		this.start;
	}

	setSynthFunc {
		var numRMSSamps, numRMSSampsRecip;

		synthFunc = {
			//responders and synths are started only once per server
			var numIns = server.options.numInputBusChannels;
			var numOuts = server.options.numOutputBusChannels;
			numRMSSamps = server.sampleRate / updateFreq;
			numRMSSampsRecip = 1 / numRMSSamps;

			server.bind( {
				var insynth, outsynth;
				if(numIns > 0, {
					insynth = SynthDef(server.name ++ "InputLevels", {
						var in = In.ar(NumOutputBuses.ir, numIns);
						SendPeakRMS.kr(in, updateFreq, 3, "/" ++ server.name ++ "InLevels")
					}).play(RootNode(server), nil, \addToHead);
				});
				if(numOuts > 0, {
					outsynth = SynthDef(server.name ++ bus ++ "OutputLevels", {
						var in = In.ar(bus, numOuts);
						SendPeakRMS.kr(in, updateFreq, 3, ( "/" ++ server.name ++ bus ++ "OutLevels" ))
					}).play(RootNode(server), nil, \addToTail);
				});

				if (serverCleanupFuncs.isNil) {
					serverCleanupFuncs = MultiLevelIdentityDictionary.new;
				};
				serverCleanupFuncs.put(server, bus, {
					insynth.free;
					outsynth.free;
					ServerTree.remove(synthFunc, server);
				});
			});
		};
	}

	startResponders {
		var numRMSSamps, numRMSSampsRecip;

		//responders and synths are started only once per server
		numRMSSamps = server.sampleRate / updateFreq;
		numRMSSampsRecip = 1 / numRMSSamps;
		if(numIns > 0) {
			inresp = OSCFunc( {|msg|
				{
					try {
						var channelCount = min(msg.size - 3 / 2, numIns);

						channelCount.do {|channel|
							var baseIndex = 3 + (2*channel);
							var peakLevel = msg.at(baseIndex);
							var rmsValue = msg.at(baseIndex + 1);
							var meter = inmeters.at(channel);
							if (meter.notNil) {
								if (meter.isClosed.not) {
									meter.peakLevel = peakLevel.ampdb.linlin(dBLow, 0, 0, 1, \min);
									meter.value = rmsValue.ampdb.linlin(dBLow, 0, 0, 1);
								}
							}
						}
					} { |error|
						if(error.isKindOf(PrimitiveFailedError).not) { error.throw }
					};
				}.defer;
			}, ("/" ++ server.name ++ "InLevels").asSymbol, server.addr).fix;
		};
		if(numOuts > 0) {
			outresp = OSCFunc( {|msg|
				{
					try {
						var channelCount = min(msg.size - 3 / 2, numOuts);

						channelCount.do {|channel|
							var baseIndex = 3 + (2*channel);
							var peakLevel = msg.at(baseIndex);
							var rmsValue = msg.at(baseIndex + 1);
							var meter = outmeters.at(channel);
							if (meter.notNil) {
								if (meter.isClosed.not) {
									meter.peakLevel = peakLevel.ampdb.linlin(dBLow, 0, 0, 1, \min);
									meter.value = rmsValue.ampdb.linlin(dBLow, 0, 0, 1);
								}
							}
						}
					} { |error|
						if(error.isKindOf(PrimitiveFailedError).not) { error.throw }
					};
				}.defer;
			}, ("/" ++ server.name ++ bus ++ "OutLevels").asSymbol, server.addr).fix;
		};
	}

	start {
		if(serverMeterViews.isNil) {
			serverMeterViews = MultiLevelIdentityDictionary.new;
		};
		if(serverMeterViews.at(server, bus).isNil) {
			serverMeterViews.put(server, bus, List());
		};
		if(serverMeterViews[server][bus].size == 0) {
			ServerTree.add(synthFunc, server);
			if(server.serverRunning, synthFunc); // otherwise starts when booted
		};
		serverMeterViews[server][bus].add(this);
		if (server.serverRunning) {
			this.startResponders
		} {
			ServerBoot.add (startResponderFunc, server)
		}
	}

	stop {
		serverMeterViews[server][bus].remove(this);
		if(serverMeterViews[server][bus].size == 0 and: (serverCleanupFuncs.notNil)) {
			serverCleanupFuncs[server][bus].value;
			serverCleanupFuncs.removeAt(server, bus);
		};

		(numIns > 0).if( { inresp.free; });
		(numOuts > 0).if( { outresp.free; });

		ServerBoot.remove(startResponderFunc, server)
	}

	remove {
		view.remove
	}
}

CompactServerMeterView : SimpleServerMeterView {
	var <orientation = \vertical;
	var <numTicks = 9;
	var <numMajorTicks = 3;
	var <minViewWidth = 3; // TODO: write setter (makeLayout happen before setting it)
	var <hideTicks = false;
	var <>ticksView;
	makeLayout {

		var ticks = LevelIndicator.new.numTicks_(numTicks).numMajorTicks_(numMajorTicks);
		var layout;
		var fixedSetter, minSetter;
		ticksView = ticks;
		if(orientation == \vertical) {
			ticks.fixedWidth_(minViewWidth);
			fixedSetter = \fixedWidth_;
			minSetter = \minWidth_;
			layout = HLayout;
		} {
			fixedSetter = \fixedHeight_;
			minSetter = \minHeight_;
			layout = VLayout;
		};
		ticks.perform(fixedSetter, minViewWidth);
		inmeters = numIns.collect { arg idx;
			LevelIndicator.new
				.perform(minSetter, minViewWidth)
		   		.warning_(0.9)
				.critical_(1.0)
				.drawsPeak_(true)
			;
		};
		outmeters = numOuts.collect { arg idx;
			LevelIndicator.new
				.perform(minSetter, minViewWidth)
		   		.warning_(0.9)
				.critical_(1.0)
				.drawsPeak_(true)
			;
		};
		if(this.view.notNil) {
			this.view.remove;
		};
		this.view = View.new;
		this.view.layout = layout.new (* 
			inmeters ++ outmeters ++ if(hideTicks == true) {
				[]
			} {
				[ticks] 
			};
		).spacing_(0).margins_(0);
	}

	hideTicks_ { arg val;
		hideTicks = val;
		ticksView.visible = val.not;
	}

	numTicks_ { arg val;
		numTicks = val;
		ticksView.numTicks_(val)
	}

	numMajorTicks_ { arg val;
		numMajorTicks = val;
		ticksView.numMajorTicks_(val)
	}

	orientation_ { arg val;
		orientation = val;
		this.makeLayout;
	}

	minViewWidth_ { arg val;
		minViewWidth = val;
		this.makeLayout;
	}
}
