

PtimeGate : Pattern {
	// FIXME: seems broken
	// output continuously values from a pattern
	// independently of the chained pattern asking 10 or 100 values
	// until the specified time end
	var <>pattern, repeats;

	*new { arg pattern, repeats = 1;
		^super.newCopyArgs(pattern, repeats).init
	}

	init { 
		
	}

	embedInStream { arg subval;
		var stream, val,sustain;
		var subpat;
		var substr;
		var restdur;
		var dur;
		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
		[thisThread.endBeat, thisThread.beats].debug("beats");

		repeats.value(subval).do { | i |
			stream = pattern.asStream;
			while (
				{ val = stream.next(());
					val.notNil;
				},
				{
					//val.debug("val");
					sustain = val.use { val.sustain } ? 1;
					dur = val.use { val.dur } ? 1;
					restdur = dur - sustain; // max: 0;
					[subval, val, sustain, dur, restdur].debug("subval, val, sustain, dur, restdur");
					thisThread.endBeat = thisThread.endBeat + sustain;
					subpat = val.use { val.pattern ? (val.key !? { Pdef(val.key) }) };
					substr = subpat.asStream;
					while(
						{ 
							thisThread.endBeat > thisThread.beats and: {
								subval = substr.next(subval);
								subval.debug("while cond subval");
								subval.notNil;
							}
						},
						{ 
							//subval = subval.yield;
							subval = subval.debug("subval").yield;
						}
					);
					thisThread.endBeat = thisThread.endBeat + restdur;
					//subpat = Event.silent(restdur).loop;
					subpat = (isRest: true, dur:restdur).loop;
					substr = subpat.asStream;
					while(
						{ 
							thisThread.endBeat > thisThread.beats and: {
								subval = substr.next(subval);
								subval.notNil;
							}
						},
						{ 
							//subval = subval.yield;
							subval = subval.debug("subval: rest").yield;
						}
					)
				});
		};
		^subval;
	}

	storeArgs {
		^[pattern, repeats]
	}
}

PtimeGatePunch_old : Pattern {
	// output continuously values from a pattern
	// independently of the chained pattern asking 10 or 100 values
	// until the specified time end
	// accept a punchIn and punchOut point
	// TODO: punchOut
	var <>pattern, punchIn, punchOut, repeats;

	*new { arg pattern, punchIn, punchOut, repeats = 1;
		^super.newCopyArgs(pattern, punchIn, punchOut, repeats).init
	}

	init { 
		
	}

	embedInStream { arg subval;
		var stream, val,sustain;
		var subpat;
		var substr;
		var restdur;
		var dur;
		var drop_time;
		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
		[thisThread.endBeat, thisThread.beats].debug("beats");

		repeats.value(subval).do { | i |
			var current_offset = 0;
			var previous_offset = 0;
			stream = pattern.asStream;

			if(punchIn.notNil) {

				while (
					{
						current_offset <= punchIn and: {
							val = stream.next(());
							val.notNil;
						}
					},
					{
						previous_offset = current_offset;
						current_offset = current_offset + val.use( { val.dur });
						[current_offset, previous_offset, val].debug("mangling");
					}
				);

				val.use {
					if(val.notNil) {
						var suboffset = punchIn - previous_offset;
						if(suboffset == 0) {
							// we are on a border, do nothing;
							val.debug("we are on a border, do nothing; ");
						} {
							if( suboffset > val.sustain ) {
								// we are on a rest
								val[\dur] = val.dur - suboffset;
								val[\sustain] = val.sustain - suboffset;
								val.debug("we are on a rest");
							} {
								// we are on a note
								val[\dur] = val.dur - suboffset;
								val[\sustain] = val.sustain + suboffset;
								val[\PtimeGatePunch_drop] = suboffset;
								val.debug("we are on a note");

							};
						}
					};
				};
			} {
				val = stream.next(());
			};

			while (
				{ 
					val.notNil;
				},
				{
					val.debug("============== first super val");
					sustain = val.use { val.sustain } ? 1;
					dur = val.use { val.dur } ? 1;
					restdur = dur - sustain; // max: 0;
					[subval, val, sustain, dur, restdur].debug("subval, val, sustain, dur, restdur");
					thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
					[thisThread.endBeat, thisThread.beats].debug("start endbeat, beats");
					thisThread.endBeat = thisThread.endBeat + sustain;
					[thisThread.endBeat, thisThread.beats].debug("second endbeat, beats");
					subpat = val.use { val.pattern ? (val.key !? { Pdef(val.key) }) };
					if(subpat.notNil) {

						substr = subpat.asStream;

						current_offset = 0;
						previous_offset = 0;
						drop_time = val[\PtimeGatePunch_drop];

						if(drop_time.notNil) {

							while (
								{
									current_offset <= drop_time and: {
										subval = substr.next(subval);
										subval.notNil
									}
								},
								{
									previous_offset = current_offset;
									current_offset = current_offset + subval.use( { subval.dur });
									[current_offset, previous_offset, subval].debug("sub mangling");
								}
							);


							subval.use {
								if(subval.notNil) {
									var suboffset = drop_time - previous_offset;
									[drop_time, previous_offset, suboffset, subval.sustain].debug( "[drop_time, previous_offset, suboffset, subval.sustain]" );
									if(suboffset == 0) {
										// we are on a border: do nothing
										subval.debug("sub we are on a border, do nothing; ");
									} {
										if( suboffset > subval.sustain ) {
											// we are on a rest: cut it in two
											subval[\dur] = subval.dur - suboffset;
											subval[\sustain] = subval.sustain - suboffset;
											subval[\isRest] = true; 
											subval.debug("sub we are on a rest");
										} {
											// we are on a note: transform it in rest
											subval[\dur] = subval.dur - suboffset;
											subval[\sustain] = subval.sustain - suboffset;
											subval.debug("sub we are on a note");

										};
									}
								};
							};
						};

						while(
							{ 
								[thisThread.endBeat, thisThread.beats].debug("sub endbeat, beats");
								thisThread.endBeat > thisThread.beats and: {
									subval = substr.next(subval);
									subval.debug("while cond subval");
									subval.notNil;
								}
							},
							{ 
								//subval = subval.yield;
								subval = subval.debug("subval").yield;
							}
						);
						thisThread.endBeat = thisThread.endBeat + restdur;
						//subpat = Event.silent(restdur).loop;
						subpat = (isRest: true, dur:restdur).loop; // dur is replaced by chained pat
						//subpat = (isRest: true).loop;
						substr = subpat.asStream;
						while(
							{ 
								[thisThread.endBeat, thisThread.beats].debug("sub rest endbeat, beats");
								thisThread.endBeat > thisThread.beats and: {
									subval = substr.next(subval);
									subval.notNil;
								}
							},
							{ 
								//subval = subval.yield;
								subval = subval.debug("subval: rest").yield;
							}
						);
					} {
						// not a subpattern, maybe a ndef event
						val.yield;
					};
					val = stream.next(());
				});
		};
		subval.debug("end subval");
		^subval;
	}

	storeArgs {
		^[pattern, repeats]
	}
}

PtimeGatePunch : Pattern {
	// output continuously values from a pattern
	// independently of the chained pattern asking 10 or 100 values
	// until the specified time end
	// accept a punchIn and punchOut point
	// TODO: punchOut
	var <>pattern, punchIn, punchOut, repeats;

	*new { arg pattern, punchIn, punchOut, repeats = 1;
		^super.newCopyArgs(pattern, punchIn, punchOut, repeats).init
	}

	init { 
		
	}

	embedInStream { arg subval;
		var stream, val,sustain;
		var subpat;
		var substr;
		var restdur;
		var dur;
		var drop_time;

		var stream_dropdur;
		
		stream_dropdur = { arg drop_time, stream;
			var current_offset = 0;
			var previous_offset = 0;
			var val;
			if(drop_time.notNil) {

				while (
					{
						current_offset <= punchIn and: {
							val = stream.next(());
							val.notNil;
						}
					},
					{
						previous_offset = current_offset;
						current_offset = current_offset + val.use( { val.dur });
						[current_offset, previous_offset, val].debug("mangling");
					}
				);

				val.use {
					if(val.notNil) {
						var suboffset = drop_time - previous_offset;
						if(suboffset == 0) {
							// we are on a border, do nothing;
							val.debug("we are on a border, do nothing; ");
						} {
							if( suboffset > val.sustain ) {
								// we are on a rest
								val[\dur] = val.dur - suboffset;
								val[\sustain] = val.sustain - suboffset;
								val.debug("we are on a rest");
							} {
								// we are on a note
								val[\dur] = val.dur - suboffset;
								val[\sustain] = val.sustain - suboffset;
								val[\PtimeGatePunch_drop] = suboffset;
								val.debug("we are on a note");
							};
						}
					};
				};
			} {
				val = stream.next(());
			};

			val;
		};

		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
		[thisThread.endBeat, thisThread.beats].debug("beats");

		repeats.value(subval).do { | i |
			stream = pattern.asStream;

			val = stream_dropdur.(punchIn, stream);

			Pspawner({ arg spawner;

				while (
					{ 
						val.notNil;
					},
					{
						val.debug("============== first super val");
						sustain = val.use { val.sustain } ? 1;
						dur = val.use { val.dur } ? 1;
						restdur = dur - sustain; // max: 0;
						[subval, val, sustain, dur, restdur].debug("subval, val, sustain, dur, restdur");
						thisThread.endBeat = thisThread.endBeat min: thisThread.beats;
						[thisThread.endBeat, thisThread.beats].debug("start endbeat, beats");
						thisThread.endBeat = thisThread.endBeat + sustain;
						[thisThread.endBeat, thisThread.beats].debug("second endbeat, beats");
						subpat = val.use { val.pattern ? (val.key !? { Pdef(val.key) }) };

						if(subpat.notNil) {


							spawner.par(
								Prout({

									substr = subpat.asStream;
									if(val[\PtimeGatePunch_drop].notNil) {
										stream_dropdur.( val[\PtimeGatePunch_drop], substr);
									};

									while(
										{ 
											[thisThread.endBeat, thisThread.beats].debug("sub endbeat, beats");
											thisThread.endBeat > thisThread.beats and: {
												subval = substr.next(subval);
												subval.debug("while cond subval");
												subval.notNil;
											}
										},
										{ 
											//subval = subval.yield;
											subval = subval.debug("subval").yield;
										}
									);
									thisThread.endBeat = thisThread.endBeat + restdur;
									//subpat = Event.silent(restdur).loop;
									subpat = (isRest: true, dur:restdur).loop; // dur is replaced by chained pat
									//subpat = (isRest: true).loop;
									substr = subpat.asStream;
									while(
										{ 
											[thisThread.endBeat, thisThread.beats].debug("sub rest endbeat, beats");
											thisThread.endBeat > thisThread.beats and: {
												subval = substr.next(subval);
												subval.notNil;
											}
										},
										{ 
											//subval = subval.yield;
											subval = subval.debug("subval: rest").yield;
										}
									);
								})
							)
						} {
							// not a subpattern, maybe a ndef event
							val.yield;
						};
						val = stream.next(());
					}
				);
			}).embedInStream
		};
		subval.debug("end subval");
		^subval;
	}

	storeArgs {
		^[pattern, repeats]
	}
}

