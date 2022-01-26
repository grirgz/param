// code from scztt

Pmod : Pattern {
	classvar defHashLRU, <defCache, <defNames, <defNamesFree, defCount=0, maxDefNames=100;
	var <>synthName, <>patternPairs, <rate, <>channels, asValues=false;

	*new {
		|synthName ... pairs|
		^super.newCopyArgs(synthName, pairs)
	}

	*kr {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\control)
	}

	*kr1 {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\control).channels_(1)
	}

	*kr2 {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\control).channels_(2)
	}

	*kr3 {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\control).channels_(3)
	}

	*kr4 {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\control).channels_(4)
	}

	*ar {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\audio)
	}

	*ar1 {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\audio).channels_(1)
	}

	*ar2 {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\audio).channels_(2)
	}

	*ar3 {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\audio).channels_(3)
	}

	*ar4 {
		|synthName ... pairs|
		^this.new(synthName, *pairs).rate_(\audio).channels_(4)
	}

	*initClass {
		defCache = ();
		defNames = ();
		defHashLRU = LinkedList();
		defNamesFree = IdentitySet();
		(1..16).do {
			|n|
			[\kr, \ar].do {
				|rate|
				this.wrapSynth(
					rate: rate,
					func: { \value.perform(rate, (0 ! n)) },
					channels: n,
					defName: "Pmod_constant_%_%".format(n, rate).asSymbol,
				);
			}
		}
	}

	// Wrap a func in fade envelope / provide XOut
	*wrapSynth {
		|rate, func, channels, defName|
		var hash, def, args;

		defName = defName ?? {
			hash = [func, rate].hash;
			defHashLRU.remove(hash);
			defHashLRU.addFirst(hash);
			defNames[hash] ?? {
				defNames[hash] = this.getDefName();
				defNames[hash]
			};
		};

		if (defCache[defName].isNil) {

			def = SynthDef(defName, {
				var fadeTime, paramLag, fade, sig;

				fadeTime = \fadeTime.kr(0);
				paramLag = \paramLag.ir(0);

				fade = Env([1, 1, 0], [0, fadeTime], releaseNode:1).kr(gate:\gate.kr(1), doneAction:2);
				sig = SynthDef.wrap(func, paramLag ! func.def.argNames.size);
				sig = sig.asArray.flatten;

				if (channels.isNil) {
					channels = sig.size;
				};

				if (rate.isNil) {
					rate = sig.rate.switch(\audio, \ar, \control, \kr);
				};

				\channels.ir(channels); // Unused, but helpful to see channelization for debugging

				sig = sig.collect {
					|channel|
					if ((channel.rate == \scalar) && (rate == \ar)) {
						channel = DC.ar(channel);
					};

					if ((channel.rate == \audio) && (rate == \kr)) {
						channel = A2K.kr(channel);
						"Pmod output is \audio, \control rate expected".warn;
					} {
						if ((channel.rate == \control) && (rate == \ar)) {
							channel = K2A.ar(channel);
							"Pmod output is \control, \audio rate expected".warn;
						}
					};
					channel;
				};

				if (sig.shape != [channels]) {
					sig.reshape(channels);
				};

				XOut.perform(rate, \out.kr(0), fade, sig);
			});
			args = def.asSynthDesc.controlNames.flatten.asArray;
			defCache[defName] = [rate, channels, def, args];
		} {
			#rate, channels, def, args = defCache[defName];
		};

		def.add;

		^(
			instrument: defName,
			args: [\value, \fadeTime, \paramLag, \out] ++ args,
			pr_rate: rate,
			pr_channels: channels,
			pr_instrumentHash: hash ?? { [func, rate].hash },
			hasGate: true
		)
	}

	rate_{
		|r|
		rate = (
			control: \kr,
			audio: \ar,
			kr: \kr,
			ar: \kr
		)[r]
	}

	embedInStream {
		|inEvent|
		var server, synthStream, streamPairs, endVal, cleanup,
		synthGroup, newSynthGroup, modGroup, newModGroup,
		buses, currentArgs, currentBuses, currentSize, currentEvent, fadeTime,
		nextEvent, nextSynth, streamAsValues, currentChannels, currentRate, cleanupFunc;

		// CAVEAT: Server comes from initial inEvent and cannot be changed later on.
		server = inEvent[\server] ?? { Server.default };
		server = server.value;

		streamAsValues = asValues;

		// Setup pattern pairs
		streamPairs = patternPairs.copy;
		endVal = streamPairs.size - 1;
		forBy (1, endVal, 2) { |i| streamPairs[i] = streamPairs[i].asStream };
		synthStream = synthName.asStream;

		// Prepare busses
		buses = List();

		// Cleanup
		cleanupFunc = Thunk({
			currentEvent !? {
				if (currentEvent[\isPlaying].asBoolean) {
					currentEvent.release(currentEvent[\fadeTime])
				};

				this.recycleDefName(currentEvent);
				{
					newModGroup !? _.free;
					buses.do(_.free);
				}.defer(currentEvent[\fadeTime] ? 10)
				{
					newSynthGroup !? _.free;
				}.defer(5);
			}
		});
		cleanup = EventStreamCleanup();
		cleanup.addFunction(inEvent, cleanupFunc);

		loop {
			// Prepare groups, reusing input group if possible.
			// This is the group that the outer event - the one whose parameters
			// we're modulating - is playing to.
			//
			// If newSynthGroup.notNil, then we allocated and we must clean up.
			if (inEvent.keys.includes(\group)) {
				synthGroup = inEvent.use({ ~group.value });
			} {
				inEvent[\group] = synthGroup = newSynthGroup ?? {
					newSynthGroup = Group(server.asTarget);
				};
			};

			// Prepare modGroup, which is our modulation group and lives before
			// synthGroup.
			// If newModGroup.notNil, then we allocated and we must clean up
			if (inEvent.keys.includes(\modGroup)) {
				modGroup = inEvent[\modGroup];
			} {
				inEvent[\modGroup] = modGroup = newModGroup ?? {
					newModGroup = Group(synthGroup.asTarget, \addBefore);
				};
			};

			// We must set group/addAction early, so they are passed to the .next()
			// of child streams.
			nextEvent = ();
			nextEvent[\synthDesc] 	= nil;
			nextEvent[\msgFunc] 	= nil;
			nextEvent[\group] 		= modGroup;
			nextEvent[\addAction] 	= \addToHead;
			nextEvent[\resend] 		= false;

			// Get nexts
			nextSynth = synthStream.next(nextEvent.copy);
			nextSynth = this.prepareSynth(nextSynth);
			nextEvent = this.prNext(streamPairs, nextEvent);

			if (inEvent.isNil || nextEvent.isNil || nextSynth.isNil) {
				^cleanup.exit(inEvent);
			} {
				cleanup.update(inEvent);

				nextEvent.putAll(nextSynth);

				// 1. We need argument names in order to use (\type, \set).
				// 2. We need size to determine if we need to allocate more busses for e.g.
				//    an event like (freq: [100, 200]).
				currentArgs = nextEvent[\instrument].asArray.collect(_.asSynthDesc).collect(_.controlNames).flatten.asSet.asArray;
				currentSize = nextEvent.atAll(currentArgs).maxValue({ |v| v.isArray.if(v.size, 1)  }).max(1);

				currentChannels = nextSynth[\pr_channels];
				currentRate = nextSynth[\pr_rate];

				buses.first !? {
					|bus|
					var busRate = switch(bus.rate, \audio, \ar, \control, \kr, bus.rate);
					if (busRate != currentRate) {
						Error("Cannot use Synths of different rates in a single Pmod (% vs %)".format(
							bus.rate, currentRate
						)).throw;
					}
				};

				(currentSize - buses.size).do {
					if (currentRate == \ar) {
						buses = buses.add(Bus.audio(server, currentChannels))
					} {
						buses = buses.add(Bus.control(server, currentChannels))
					};
				};
				currentBuses = buses.collect(_.index).extend(currentSize);
				if (currentBuses.size == 1) { currentBuses = currentBuses[0] };

				// If we've got a different instrument than last time, send a new one,
				// else just set the parameters of the existing.
				if (nextEvent[\resend]
						or: {nextEvent[\pr_instrumentHash] != currentEvent.tryPerform(\at, \pr_instrumentHash)})
				{
					nextEvent[\parentType]  = \note;
					nextEvent[\type] 		= \note;
					nextEvent[\sustain] 	= nil;
					nextEvent[\sendGate] 	= false;
					nextEvent[\fadeTime] 	= fadeTime = nextEvent[\fadeTime] ?? 0;
					nextEvent[\out] 		= currentBuses;
					nextEvent[\group] 		= modGroup;
					nextEvent[\addAction] 	= \addToHead; // SUBTLE: new synths before old, so OLD synth is responsible for fade-out

					// Free existing synth
					currentEvent !? {
						|e|
						// Assumption: If \hasGate -> false, then synth will free itself.
						if (e[\isPlaying].asBoolean && e[\hasGate]) {
							e[\sendGate] = true;
							e.release(nextEvent[\fadeTime]);
							e[\isPlaying] = false;
						}
					};
				} {
					nextEvent[\parentType]  = \set;
					nextEvent[\type] 		= \set;
					nextEvent[\id] 			= currentEvent[\id];
					nextEvent[\args] 		= currentEvent[\args];
					nextEvent[\out] 		= currentEvent[\out];
				};

				nextEvent.parent ?? { nextEvent.parent = Event.parentEvents.default };

				// SUBTLE: If our inEvent didn't have a group, we set its group here.
				//         We do this late so previous uses of inEvent aren't disrupted.
				if (newSynthGroup.notNil) {
					inEvent[\group] = newSynthGroup;
				};

				// Yield our buses via .asMap
				inEvent = currentSize.collect({
					|i|
					var group;
					{
						if (i == 0) {
							cleanup.addFunction(currentEnvironment, cleanupFunc)
						};
						// In this context, ~group refers to the event being modulated,
						// not the Pmod event.

						~group = ~group.value;
						if (~group.notNil and: { ~group != synthGroup }) {
							modGroup.moveBefore(~group.asGroup)
						};

						if (nextEvent[\isPlaying].asBoolean.not) {
							currentEvent = nextEvent;
							nextEvent[\isPlaying] = true;
							nextEvent.playAndDelta(cleanup, false);
						};

						if (streamAsValues) {
							buses[i].getSynchronous;
						} {
							buses[i].asMap;
						}
					}
				});
				if (currentSize == 1) {
					inEvent = inEvent[0].yield;
				} {
					inEvent = inEvent.yield;
				}
			};
		}

		^cleanup.exit(inEvent);
	}

	// This roughly follows the logic of Pbind
	prNext {
		|streamPairs, inEvent|
		var event, endVal;

		event = this.prScrubEvent(inEvent);
		endVal = streamPairs.size - 1;

		forBy (0, endVal, 2) { arg i;
			var name = streamPairs[i];
			var stream = streamPairs[i+1];
			var streamout = stream.next(event);
			if (streamout.isNil) { ^inEvent };

			if (name.isSequenceableCollection) {
				if (name.size > streamout.size) {
					("the pattern is not providing enough values to assign to the key set:" + name).warn;
					^inEvent
				};
				name.do { arg key, i;
					event.put(key, streamout[i]);
				};
			}{
				event.put(name, streamout);
			};
		};

		^event;
	}

	recycleDefName {
		|event|
		var hash, name;
		if (defHashLRU.size > maxDefNames) {
			hash = defHashLRU.pop();
			name = defNames[hash];
			defNames[hash] = nil;
			defCache[name] = nil;
			defNamesFree.add(name);
		}
	}

	*getDefName {
		if (defNamesFree.notEmpty) {
			^defNamesFree.pop()
		} {
			defCount = defCount + 1;
			^"Pmod_unique_%".format(defCount).asSymbol;
		}
	}

	// Scrub parent event of Pmod-specific values like group - these will disrupt
	// the way we set up our groups and heirarchy.
	prScrubEvent {
		|event|
		event[\modGroup] = nil;
		^event;
	}

	// Convert an item from our instrument stream into a SynthDef name.
	// This can possible add a new SynthDef if supplied with e.g. a function.
	prepareSynth {
		|synthVal|
		var synthDesc, synthOutput;
		^case
		{ synthVal.isKindOf(Array) } {
			synthVal.collect(this.prepareSynth(_)).reduce({
				|a, b|
				a.merge(b, {
					|a, b|
					a.asArray.add(b)
				})
			})
		}
		{ synthVal.isKindOf(SimpleNumber) } {
			var constRate = rate ?? { \ar }; // default to \ar, because this works for both ar and kr mappings;
			var constChannels = channels ?? { 1 };

			this.class.wrapSynth(
				channels: constChannels, rate: constRate,
				defName: "Pmod_constant_%_%".format(constChannels, constRate).asSymbol
			).putAll((
				value: synthVal
			))
		}
		{ synthVal.isKindOf(Symbol) } {
			synthDesc = synthVal.asSynthDesc;
			synthOutput = synthDesc.outputs.detect({ |o| o.startingChannel == \out });

			if (synthOutput.isNil) {
				Error("Synth '%' needs at least one output, connected to an \out synth parameter".format(synthVal)).throw;
			};

			(
				instrument: synthVal,
				args: synthDesc.controlNames.flatten.asSet.asArray,
				pr_instrumentHash: synthVal.identityHash,
				pr_rate: synthOutput.rate.switch(\audio, \ar, \control, \kr),
				pr_channels: synthOutput.numberOfChannels
			)
		}
		{ synthVal.isKindOf(AbstractFunction) } {
			this.class.wrapSynth(rate, synthVal, channels)
		}
		{ synthVal.isNil } {
			nil
		}
		{
			synthVal.putAll(this.prepareSynth(synthVal[\instrument]));
		}
	}

	asValues {
		asValues = true;
	}

	expand {
		^(
			Pfunc({
				|in|
				var thunk;

				if (in.isArray) { in = in[0] };
				thunk = Thunk({
					in.value
				});

				this.channels.collect {
					|i|
					{
						thunk.value.asArray[i]
					}
				}
			}) <> this
		)
	}
}


+Symbol {
    asSynthDesc {
        ^SynthDescLib.default[this]
    }
}
