
SimpleServerMeterView : SCViewHolder {
	// TODO: add player interface (isPlaying)

	classvar serverMeterViews; 
	classvar serverCleanupFuncs;

	var inresp, outresp, synthFunc, responderFunc, server, numIns, numOuts, inmeters, outmeters, startResponderFunc;
	var outputSynthFunc, inputSynthFunc;
	var <bus, busLabel;
	var <>updateFreq = 10, <>dBLow = -80, <>meterWidth = 15, <>gapWidth = 4, <>meterHeight = 180;

	*new { |aserver, numIns=0, numOuts=2, bus|
		^super.new.initSimpleServerMeterView(aserver, numIns, numOuts, bus)
	}

	initSimpleServerMeterView { arg aserver, anumIns, anumOuts, abus;
		var innerView, viewWidth, palette;

		server = aserver ?? { Server.default };

		if(abus.isKindOf(Bus)) {
			abus = abus.index;
		};
		abus = abus ? 0;
		if(abus.isKindOf(Bus)) {
			busLabel = abus.key ?? { abus.index };
			bus = abus.index;
		} {
			busLabel = abus;
			bus = abus;
		};

		numIns = anumIns ?? { server.options.numInputBusChannels };
		numOuts = anumOuts ?? { server.options.numOutputBusChannels };

		this.makeLayout;
		this.view.onClose = { this.stop }; // cleanup

		//this.setSynthFunc(inmeters, outmeters);
		//startResponderFunc = {this.startResponders};
		this.start;
	}

	//*getWidth { arg numIns, numOuts, server;
		//^20+((numIns + numOuts + 2) * (meterWidth + gapWidth))
	//}

	bus_ { arg abus;
		//Log(\Param).debug("SimpleServerMeterView: set bus %".format(abus));
		if(abus.isKindOf(Bus)) {
			busLabel = abus.key ?? { abus.index };
			bus = abus.index;
		} {
			busLabel = abus;
			bus = abus;
		};
		//this.stop;
		//this.setSynthFunc(inmeters, outmeters);
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

	///////
	
	// input

	startInputMeter { 
		// 3 states:
		// - stopped: no serverMeterViews and no serverCleanupFuncs
		// - starting: serverMeterViews but no serverCleanupFuncs
		// - running: serverMeterViews and serverCleanupFuncs
		//var starting = serverMeterViews serverCleanupFuncs.at(server, bus).notNil;
		//
		// The problem is CmdPeriod does not set serverCleanupFuncs to nil, we don't know synth is freed
		//	  solved by reseting it in ServerTree
		// The problem is stop should only stop if there is no more instance playing

		if(numIns < 1) {
			^\abort
		};

		if(serverMeterViews.at(server, \input).isNil) {
			serverMeterViews.put(server, \input, Set());
		};
		if(serverMeterViews.at(server, \input).includes(this).not) {
			serverMeterViews.at(server, \input).add(this);
			this.startInputResponder;
		};

		if (serverCleanupFuncs.isNil) {
			serverCleanupFuncs = MultiLevelIdentityDictionary.new;
		};

		if(serverCleanupFuncs.at(server, \input).isNil) {
			var initFun;
			var insynth;
			serverCleanupFuncs.put(server, \input, {
				// when stopped
				//Log(\Param).debug(">-< ServerMeterView: serverCleanupFuncs run: input freed %", insynth);
				serverCleanupFuncs.removeAt(server, \input);
				ServerTree.remove(initFun, server);
				{ // if freed too early, synth has not launched yet
					insynth.free;
				}.defer(1);
			});
			initFun = {
				// when CmdPeriod
				serverCleanupFuncs.removeAt(server, \input);
				ServerTree.remove(initFun, server);
				this.startInputMeter;
			};
			ServerTree.add(initFun, server);


			insynth = SynthDef(server.name ++ "InputLevels", {
				var in = In.ar(NumOutputBuses.ir, server.options.numInputBusChannels);
				SendPeakRMS.kr(in, updateFreq, 3, "/" ++ server.name ++ "InLevels")
			}).play(RootNode(server), nil, \addToHead);
			//Log(\Param).debug("--- startInputMeter: launch synth %", insynth);


		}
	}

	stopInputMeter {
		serverMeterViews[server][\input].remove(this);
		this.stopInputResponder;
		if(serverMeterViews[server][\input].size == 0 and: (serverCleanupFuncs.notNil)) {
			serverCleanupFuncs[server][\input].value;
			//Log(\Param).debug("ServerMeterView.stop: cleanup: bus %, server %, data %".format(bus, server, serverMeterViews.at(server, bus)));
		};
	}

	startInputResponder {
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
	}

	stopInputResponder {
		inresp.free;
	}

	// output

	startOutputMeter { 
		// 3 states:
		// - stopped: no serverMeterViews and no serverCleanupFuncs
		// - starting: serverMeterViews but no serverCleanupFuncs
		// - running: serverMeterViews and serverCleanupFuncs
		//var starting = serverMeterViews serverCleanupFuncs.at(server, bus).notNil;
		//
		// The problem is CmdPeriod does not set serverCleanupFuncs to nil, we don't know synth is freed
		//	  solved by reseting it in ServerTree
		// The problem is stop should only stop if there is no more instance playing
		
		//Log(\Param).debug("startOutputMeter %".format(numOuts));

		if(numOuts < 1) {
			^\abort
		};

		if(serverMeterViews.at(server, bus).isNil) {
			serverMeterViews.put(server, bus, Set());
		};
		if(serverMeterViews.at(server, bus).includes(this).not) {
			serverMeterViews.at(server, bus).add(this);
			this.startOutputResponder;
		};

		if (serverCleanupFuncs.isNil) {
			serverCleanupFuncs = MultiLevelIdentityDictionary.new;
		};

		if(serverCleanupFuncs.at(server, bus).isNil) {
			var initFun;
			var outsynth;
			var name;
			name = "%_%_%".format(server.name, bus, "OutputLevels");
			serverCleanupFuncs.put(server, bus, {
				// when stopped
				//Log(\Param).debug(">------< ServerMeterView: serverCleanupFuncs run: output freed %", outsynth);
				serverCleanupFuncs.removeAt(server, bus);
				ServerTree.remove(initFun, server);
				{ // if freed too early, synth has not launched yet
					outsynth.free;
				}.defer(1);
			});
			initFun = {
				// when CmdPeriod
				serverCleanupFuncs.removeAt(server, bus);
				ServerTree.remove(initFun, server);
				this.startOutputMeter;
			};
			ServerTree.add(initFun, server);

			//Log(\Param).debug("ServerMeterView: outputSynthFunc run. numOuts %".format(numOuts));

			outsynth = SynthDef(name, {
				var in = In.ar(bus, numOuts);
				SendPeakRMS.kr(in, updateFreq, 3, ( "/" ++ server.name ++ bus ++ "OutLevels" ))
			}).play(RootNode(server), nil, \addToTail);
			//Log(\Param).debug("-------- outputSynthFunc: launch synth %", outsynth);


		}
	}

	stopOutputMeter {
		serverMeterViews[server][bus].remove(this);
		this.stopOutputResponder;
		if(serverMeterViews[server][bus].size == 0 and: (serverCleanupFuncs.notNil)) {
			serverCleanupFuncs[server][bus].value;
			//Log(\Param).debug("ServerMeterView.stop: cleanup: bus %, server %, data %".format(bus, server, serverMeterViews.at(server, bus)));
		};
	}

	startOutputResponder {
		var numRMSSamps, numRMSSampsRecip;

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

	stopOutputResponder {
		outresp.free;
	}

	//

	start {
		if(serverMeterViews.isNil) {
			serverMeterViews = MultiLevelIdentityDictionary.new;
		};
		this.startOutputMeter;
		this.startInputMeter;

		//Log(\Param).debug("ServerMeterView.start: bus %, server %, data %".format(bus, server, serverMeterViews.at(server, bus)));

		//if(serverMeterViews.at(server, bus).isNil) {
			//serverMeterViews.put(server, bus, List());
		//};
		//if(serverMeterViews.at(server, \input).isNil) {
			//serverMeterViews.put(server, \input, List());
		//};

		//if(serverMeterViews[server][\input].size == 0) {
			//serverMeterViews[server][\input].add(this);
			//Log(\Param).debug("ServerMeterView.start: before run input %, server %, data %".format(bus, server, serverMeterViews.at(server, \input)));
			//ServerTree.add(inputSynthFunc, server);
			//if(server.serverRunning, inputSynthFunc); // otherwise starts when booted
		//} {
			//serverMeterViews[server][\input].add(this);
		//};

		//if(serverMeterViews[server][bus].size == 0) {
			//serverMeterViews[server][bus].add(this);
			//Log(\Param).debug("ServerMeterView.start: before run bus %, server %, data %".format(bus, server, serverMeterViews.at(server, bus)));
			//this.startOutputMeter;
		//} {
			//serverMeterViews[server][bus].add(this);
		//};
		//Log(\Param).debug("ServerMeterView.start: after run bus %, server %, data %".format(bus, server, serverMeterViews.at(server, bus)));
		//if (server.serverRunning) {
			//this.startResponders
		//} {
			//ServerBoot.add (startResponderFunc, server)
		//}
	}

	stop {

		if(serverMeterViews.isNil) {
			serverMeterViews = MultiLevelIdentityDictionary.new;
		};
		this.stopOutputMeter;
		this.stopInputMeter;



		//Log(\Param).debug("ServerMeterView.stop: bus %, server %, data %".format(bus, server, serverMeterViews.at(server, bus)));
		//serverMeterViews[server][bus].remove(this);
		//serverMeterViews[server][\input].remove(this);
		////Log(\Param).debug("ServerMeterView.stop: after: bus %, server %, data %".format(bus, server, serverMeterViews.at(server, bus)));
		//if(serverMeterViews[server][\input].size == 0 and: (serverCleanupFuncs.notNil)) {
			//serverCleanupFuncs[server][\input].value;
			//serverCleanupFuncs.removeAt(server, \input);
			//Log(\Param).debug("ServerMeterView.stop: cleanup: bus %, server %, data %".format(bus, server, serverMeterViews.at(server, bus)));
		//};
		//if(serverMeterViews[server][bus].size == 0 and: (serverCleanupFuncs.notNil)) {
			//serverCleanupFuncs[server][bus].value;
			//serverCleanupFuncs.removeAt(server, bus);
			//Log(\Param).debug("ServerMeterView.stop: cleanup: bus %, server %, data %".format(bus, server, serverMeterViews.at(server, bus)));
		//};

		//outmeters.do { arg item, idx;
			//item.peakLevel = -inf;
			//item.value = -inf;
		//};
		//inmeters.do { arg item, idx;
			//item.peakLevel = -inf;
			//item.value = -inf;
		//};

		//(numIns > 0).if( { inresp.free; });
		//(numOuts > 0).if( { outresp.free; });

		//ServerBoot.remove(startResponderFunc, server)
	}

	remove {
		view.remove
	}

	//// deprecated

	//setSynthFunc {
		//var numRMSSamps, numRMSSampsRecip;

		//inputSynthFunc = {
			////responders and synths are started only once per server
			//numRMSSamps = server.sampleRate / updateFreq;
			//numRMSSampsRecip = 1 / numRMSSamps;

			//Log(\Param).debug("ServerMeterView: inputSynthFunc run. numIns %".format(numIns));
			//server.bind({
				//var insynth, outsynth;
				//if (serverCleanupFuncs.isNil) {
					//serverCleanupFuncs = MultiLevelIdentityDictionary.new;
				//};
				//serverCleanupFuncs.at(server, \input).value;
				//if(numIns > 0, {
					//insynth = SynthDef(server.name ++ "InputLevels", {
						//var in = In.ar(NumOutputBuses.ir, numIns);
						//SendPeakRMS.kr(in, updateFreq, 3, "/" ++ server.name ++ "InLevels")
					//}).play(RootNode(server), nil, \addToHead);
					//Log(\Param).debug("outputSynthFunc: launch synth %", insynth);
				//});

				//serverCleanupFuncs.put(server, \input, {
					//Log(\Param).debug("ServerMeterView: serverCleanupFuncs run: input freed %", insynth);
					//insynth.free;
					//ServerTree.remove(inputSynthFunc, server);
				//});
			//});
		//};

		//outputSynthFunc	= {
			////responders and synths are started only once per server
			////var numOuts = server.options.numOutputBusChannels;
			//numRMSSamps = server.sampleRate / updateFreq;
			//numRMSSampsRecip = 1 / numRMSSamps;

			//Log(\Param).debug("ServerMeterView: outputSynthFunc run. numOuts %".format(numOuts));
			//server.bind( {
				//var insynth, outsynth;
				//var name;
				//name = "%_%_%".format(server.name, bus, "OutputLevels");
				//if (serverCleanupFuncs.isNil) {
					//serverCleanupFuncs = MultiLevelIdentityDictionary.new;
				//};
				//serverCleanupFuncs.debug("serverCleanupFuncs");
				//serverCleanupFuncs.at(server, bus).value;
				//if(numOuts > 0, {
					//outsynth = SynthDef(name, {
						//var in = In.ar(bus, numOuts);
						//SendPeakRMS.kr(in, updateFreq, 3, ( "/" ++ server.name ++ bus ++ "OutLevels" ))
					//}).play(RootNode(server), nil, \addToTail);
					//Log(\Param).debug("-------- outputSynthFunc: launch synth %", outsynth);
				//});

				//serverCleanupFuncs.put(server, bus, {
					//Log(\Param).debug(">------< ServerMeterView: serverCleanupFuncs run: output freed %", outsynth);
					//{ // if freed too early, synth has not launched yet
						//outsynth.free;
						//ServerTree.remove(outputSynthFunc, server);
					//}.defer(1);
				//});
			//});
		//};

		//synthFunc = { inputSynthFunc.value; outputSynthFunc.value };
	//}

	//startResponders {
		//var numRMSSamps, numRMSSampsRecip;

		////responders and synths are started only once per server
		//numRMSSamps = server.sampleRate / updateFreq;
		//numRMSSampsRecip = 1 / numRMSSamps;
		//if(numIns > 0) {
			//inresp = OSCFunc( {|msg|
				//{
					//try {
						//var channelCount = min(msg.size - 3 / 2, numIns);

						//channelCount.do {|channel|
							//var baseIndex = 3 + (2*channel);
							//var peakLevel = msg.at(baseIndex);
							//var rmsValue = msg.at(baseIndex + 1);
							//var meter = inmeters.at(channel);
							//if (meter.notNil) {
								//if (meter.isClosed.not) {
									//meter.peakLevel = peakLevel.ampdb.linlin(dBLow, 0, 0, 1, \min);
									//meter.value = rmsValue.ampdb.linlin(dBLow, 0, 0, 1);
								//}
							//}
						//}
					//} { |error|
						//if(error.isKindOf(PrimitiveFailedError).not) { error.throw }
					//};
				//}.defer;
			//}, ("/" ++ server.name ++ "InLevels").asSymbol, server.addr).fix;
		//};
		//if(numOuts > 0) {
			//outresp = OSCFunc( {|msg|
				//{
					//try {
						//var channelCount = min(msg.size - 3 / 2, numOuts);

						//channelCount.do {|channel|
							//var baseIndex = 3 + (2*channel);
							//var peakLevel = msg.at(baseIndex);
							//var rmsValue = msg.at(baseIndex + 1);
							//var meter = outmeters.at(channel);
							//if (meter.notNil) {
								//if (meter.isClosed.not) {
									//meter.peakLevel = peakLevel.ampdb.linlin(dBLow, 0, 0, 1, \min);
									//meter.value = rmsValue.ampdb.linlin(dBLow, 0, 0, 1);
								//}
							//}
						//}
					//} { |error|
						//if(error.isKindOf(PrimitiveFailedError).not) { error.throw }
					//};
				//}.defer;
			//}, ("/" ++ server.name ++ bus ++ "OutLevels").asSymbol, server.addr).fix;
		//};
	//}
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
		//orientation.debug("CompactServerMeterView.makeLayout orientation");
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
			this.view.onClose = nil; // prevent stopping
			this.view.removeAll;
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
		//layout.debug("CompactServerMeterView: layout");
		//this.view.layout = layout.new (* 
			//outmeters 
		//).spacing_(0).margins_(0);
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
		//orientation.debug("CompactServerMeterView set orientation");
		this.makeLayout;
		this.view.onClose = { this.stop }; // cleanup

		//this.setSynthFunc(inmeters, outmeters);
		//startResponderFunc = {this.startResponders};
		this.start;
	}

	minViewWidth_ { arg val;
		minViewWidth = val;
		this.makeLayout;
		this.view.onClose = { this.stop }; // cleanup

		//this.setSynthFunc(inmeters, outmeters);
		//startResponderFunc = {this.startResponders};
		this.start;
	}
}
