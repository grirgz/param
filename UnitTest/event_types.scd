///// sound test objects

(
ParamProto.init;
Ndef(\test, { SinOsc.ar(500)*0.1!2 });
Ndef(\test).quant = 2;
Pdef(\test_long).quant = 2;
Pdef(\test_short).quant = 2;
Pdef(\part).quant = 2;
~t = 135/4/60; TempoClock.default.tempo = ~t;

Pdef(\test_long, Pbind(
	\instrument, \default,
	\freq, 700,
	\dur, 1,
	\amp, 0.1,
));

Pdef(\test_short, Pbind(
	\instrument, \default,
	\freq, 700 * Pseq([1,1.4],inf),
	\dur, 1/8,
	\amp, 0.1,
));
Pdef(\test_short2, Pbind(
	\instrument, \default,
	\freq, 550 * Pseq([1,1.4],inf),
	\dur, 1/8,
	\amp, 0.1,
));

Pdef(\test_long_mono, Pmono( \default,
	\freq, 700 * Pseq([1,1.4],inf),
	\dur, 1,
	\amp, 0.1,
));
)

////////////

// "target" or "player" (instead of "receiver") doesnt work
// also should be wrapped in PlayerWrapper because playNow is used

// Pdef
// FAIL with legato of long note (because EventStreamPlayer can't stop in middle of a note)
(
~p = PlayerEvent( 
	(
		\receiver: PlayerWrapper(Pdef(\test_long)),
		\dur: 1,
		\legato: 0.2, // legato doesnt work

	)
);
~p.play;
)

// Pdef with short note
// SUCCESS 
(
~p = PlayerEvent( 
	(
		\receiver: PlayerWrapper(Pdef(\test_short)),
		\dur: 1,
		\legato: 0.2,

	)
);
~p.play;
)

// Pdef with mono long note
// SUCCESS 
(
~p = PlayerEvent( 
	(
		\receiver: PlayerWrapper(Pdef(\test_long_mono)),
		\dur: 1,
		\legato: 0.2,

	)
);
~p.play;
)

// Ndef
// SUCCESS 
(
~p = PlayerEvent( 
	(
		\receiver: PlayerWrapper(Ndef(\test)),
		\dur: 1,
		\legato: 0.2,

	)
);
~p.play;
)



// Ndef without PlayerWrapper
// SUCCESS only with no quant
Ndef(\test).quant= 0;
(
~p = PlayerEvent( 
	(
		\receiver: Ndef(\test),
		\method: \play,
		\stopMethod: \stop,
		\dur: 1,
		\legato: 0.2,

	)
);
~p.play;
)
Ndef(\test).quant = 2; // put back to test if quand is bypassed by PlayerWrapper

// Pdef without PlayerWrapper
// FAIL with legato of long note
Pdef(\test).quant = 0; // work only with no quant, that's the reason of PlayerWrapper
(
~p = PlayerEvent( 
	(
		\receiver: Pdef(\test_long),
		\method: \play,
		\stopMethod: \stop,
		\dur: 1,
		\legato: 0.2,

	)
);
~p.play;
)
Pdef(\test).quant = 2; // put back to test if quand is bypassed by PlayerWrapper

// Pdef without PlayerWrapper
// SUCCESS
Pdef(\test2).quant = 0; // work only with no quant, that's the reason of PlayerWrapper
(
~p = PlayerEvent( 
	(
		\receiver: Pdef(\test_short),
		\method: \play,
		\stopMethod: \stop,
		\dur: 1,
		\legato: 0.2,

	)
);
~p.play;
)
Pdef(\test2).quant = 2; // put back to test if quand is bypassed by PlayerWrapper


// embed \player events
// SUCCESS
(
	Pdef(\part, Pdef(\zed, 
		Pembed(
			Pseq([
				(
					\type: \player,
					\receiver: PlayerWrapper(Ndef(\test)),
					\dur: 1,
					\legato: 0.2,
				),
				(
					\type: \player,
					\receiver: PlayerWrapper(Ndef(\test)),
					\dur: 1,
					\legato: 0.2,
				),
			])
		)
	)).play;
);

// embed PlayerEvent
// SUCCESS
(
	Pdef(\part, Pdef(\zed, 
		Pembed(
			Pseq([
				PlayerEvent((
					\type: \player,
					\receiver: PlayerWrapper(Ndef(\test)),
					\dur: 1,
					\legato: 0.2,
				)),
				PlayerEvent((
					\type: \player,
					\receiver: PlayerWrapper(Ndef(\test)),
					\dur: 1,
					\legato: 0.2,
				)),
			])
		)
	)).play;
);

// embed PlayerEvent without Pembed
// SUCCESS
(
	Pdef(\part, Pdef(\zed, 
			Pseq([
				PlayerEvent((
					\type: \player,
					\receiver: PlayerWrapper(Ndef(\test)),
					\dur: 1,
					\legato: 0.2,
				)),
				PlayerEvent((
					\type: \player,
					\receiver: PlayerWrapper(Pdef(\test_long)),
					\dur: 1,
					\legato: 0.2,
				)),
				PlayerEvent((
					\type: \player,
					\receiver: PlayerWrapper(Pdef(\test_long_mono)),
					\dur: 1,
					\legato: 0.2,
				)),
				PlayerEvent((
					\type: \player,
					\receiver: PlayerWrapper(Pdef(\test_short)),
					\dur: 1,
					\legato: 0.2,
				)),
			])
	)).play;
);

// embed \player events without Pembed
// this work because the only thing needed for player events are a custom play function, already defined in the main library event system with:
	//Event.addEventType(\player, playfun);
	//Event.addEventType(\pattern, playfun); // just for compat
// SUCCESS
(
	Pdef(\part, Pdef(\zed, 
		Pseq([
			(
				\type: \player,
				\receiver: PlayerWrapper(Pdef(\test_short)),
				\dur: 1,
				\legato: 0.2,
			),
			(
				\type: \player,
				\receiver: PlayerWrapper(Ndef(\test)),
				\dur: 1,
				\legato: 0.2,
			),
		])
	)).play;
);

/////////////// 
// implementation assume PlayerEvent(PlayerEvent(ev)) == PlayerEvent(ev)
(
	~ev= (
		\receiver: Ndef(\test),
		\method: \play,
		\stopMethod: \stop,
		\dur: 1,
		\legato: 0.2,
	);
	PlayerEvent(PlayerEvent(~ev)) == PlayerEvent(~ev);
	PatternEvent(PatternEvent(~ev)) == PatternEvent(~ev);

	// strange tests
	PatternEvent(PlayerEvent(~ev)) != PatternEvent(~ev); // why ?
	PlayerEvent(PatternEvent(~ev)) == PlayerEvent(~ev); // why ?
)

// play the same
	PlayerEvent(PlayerEvent(~ev)).play;
	PlayerEvent(~ev).play;


/////////////////////////// asPatternEvent asPlayerEvent

Pdef(\test).asPatternEvent // should be implemened ?
Ndef(\test).asPatternEvent // should be implemened ?
PlayerWrapper(Pdef(\test)).asPatternEvent // should be implemened ?


// embed player events in a pattern
// SUCCESS (and FAIL)
// as always legato on Pdef is not respected because long note
(
	Pdef(\part, Pdef(\zed, 
		Pseq([
			PlayerWrapper(Pdef(\test_long)).asPlayerEvent,
			PlayerWrapper(Ndef(\test)).asPlayerEvent.putAll((
				\legato: 0.2,
			)),
			PlayerWrapper(Pdef(\test_long)).asPlayerEvent.putAll(( // doesnt work
				\legato: 0.2,
			)),
			PlayerWrapper(Pdef(\test_long_mono)).asPlayerEvent.putAll((
				\legato: 0.1,
			)),
			PlayerWrapper(Pdef(\test_short)).asPlayerEvent.putAll((
				\legato: 0.2,
			)),
			PlayerWrapper(Ndef(\test)).asPlayerEvent,
		])
	)).play;
);




//////////////////////// Pattern event


(
	// work only with no quant
	// use the same play function as PlayerEvent so need \method and \stopMethod
	// \method should be renamed in \playMethod
~p = PatternEvent( 
	(
		\pattern: Pdef(\test_long),
		\method: \play,
		\stopMethod: \stop,
		\dur: 1,
		\legato: 0.8,
	)
);
~p.play;
)



// embed two pattern event
// Pembed transform events of type \pattern into real patterns and cut and ambed them according to \legato and \startOffset
// SUCCESS
(
	Pdef(\part, Pdef(\zed, 
		Pembed(
			Pseq([
				(
					\type: \pattern,
					\pattern: Pdef(\test_long),
					\dur: 1,
					\legato: 0.2,
				),
				(
					\type: \pattern,
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				),
				(
					\type: \pattern,
					\pattern: Pdef(\test_long_mono),
					\dur: 1,
					\legato: 0.2,
				)
			])
		)
	)).play;
);


// embed two pattern event without Pembed
// this mean this will use ~play method of Event of type \pattern, which is currently the same as PlayerEvent
// FAIL: no sound, don't know if it's supposed to work, shoud use either Pembed or PatternEvent
(
	Pdef(\part, Pdef(\zed, 
			Pseq([
				(
					\type: \pattern,
					\pattern: Pdef(\test_long_mono),
					\method: \play,
					\stopMethod: \stop,
					\dur: 1,
					\legato: 0.2,
				),
				(
					\type: \pattern,
					\pattern: Pdef(\test_short),
					\method: \play,
					\stopMethod: \stop,
					\dur: 1,
					\legato: 0.2,
				)
			])
	)).play;
);

// embed two pattern event as classes
// SUCCESS
(
	Pdef(\part, Pdef(\zed, 
		Pembed(
			Pseq([
				PatternEvent((
					\pattern: Pdef(\test_long_mono),
					\dur: 1,
					\legato: 0.2,
				)),
				PatternEvent((
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				)),
			])
		)
	)).play;
);

// without Pembed
// when played, PatternEvent.embedInStream is called
	// which call 
// expected FAIL because need Pembed since PatternEvent should be embeded at list level
(
	Pdef(\part, Pdef(\zed, 
			Pseq([
				PatternEvent((
					\type: \pattern,
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				)),
				PatternEvent((
					\pattern: Pdef(\test_long_mono),
					\dur: 1,
					\legato: 0.2,
				)),
				PatternEvent((
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				)),
			])
	)).play;
);

////////////// debug



/////////////// cutting!
///////////
/// this section need more analysis and might be wrong



// cut a pattern event (need to be embeded before))
// SUCCESS
(
	Pdef(\part, Pdef(\zed, 
			Pseq([
				Pfindur(1/4, Pembed(PatternEvent((
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 1,
				)))),
				PatternEvent((
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				)),
			])
	)).play;
);


// cut a player event (need to be embeded before))
// SUCCESS: expected to fail because Pfindur can't cut a PlayerEvent (no one can ?)
(
	Pdef(\part, Pdef(\zed, 
			Pseq([
				Pfindur(1/4, Pembed(PlayerEvent((
					\receiver: PlayerWrapper(Pdef(\test_short)),
					\dur: 1,
					\legato: 1,
				)))),
				PatternEvent((
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				)),
			])
	)).play;
);

// cut a pattern event (need to be embeded before))
// SUCCESS: expected to fail because Pfindur can't cut a PlayerPattern (no one can)
(
	Pdef(\part, Pdef(\zed, 
			Pseq([
				Pfindur(1/4, Pembed(PlayerEvent((
					\receiver: PlayerWrapper(Pdef(\test_short)),
					\dur: 1,
					\legato: 1,
				)))),
				PatternEvent((
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				)),
			])
	)).play;
);


////////////////////// paralel events



// PASS
// NOTE: need to transform PatternEvent in pattern with .embedPattern (because only Pseq accept list of events, Ppar need patterns)
(
	Pdef(\part, Pdef(\zed, 
		Ppar([
			PatternEvent((
				\type: \pattern,
				\pattern: Pdef(\test_short),
				\dur: 1,
				\legato: 0.2,
			)).embedPattern,
			PatternEvent((
				\type: \pattern,
				\pattern: Pdef(\test_short2),
				\dur: 1,
				\legato: 0.2,
			)).embedPattern,
		]),
	)).play;
);

//// Pembed should be transparent
// PASS
(
	Pdef(\part, Pdef(\zed, 
		Pembed(Ppar([
			Pembed(Pseq([
				PatternEvent((
					\type: \pattern,
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				)),
			])),
			Pembed(Pseq([
				PatternEvent((
					\type: \pattern,
					\pattern: Pdef(\test_short2),
					\dur: 1,
					\legato: 0.2,
				)),
			])),
		])),
	)).play;
);
// PASS
(
	Pdef(\part, Pdef(\zed, 
		Pembed(Ppar([
			Pseq([
				PatternEvent((
					\type: \pattern,
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				)),
			]),
			Pembed(Pseq([
				PatternEvent((
					\type: \pattern,
					\pattern: Pdef(\test_short2),
					\dur: 1,
					\legato: 0.2,
				)),
			])),
		])),
	)).play;
);

///////// paralel event of type \pattern
// PASS 
(
	Pdef(\part, Pdef(\zed, 
		Pembed(Ppar([
			Pseq([
				(
					\type: \pattern,
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				),
			]),
			Pseq([
				(
					\type: \pattern,
					\pattern: Pdef(\test_short2),
					\dur: 1,
					\legato: 0.2,
				),
			]),
		])),
	)).play;
);


/// individual Pembed
// PASS
(
	Pdef(\part, Pdef(\zed, 
		Ppar([
			Pembed(Pseq([
				(
					\type: \pattern,
					\pattern: Pdef(\test_short),
					\dur: 1,
					\legato: 0.2,
				),
			])),
			Pembed(Pseq([
				(
					\type: \pattern,
					\pattern: Pdef(\test_short2),
					\dur: 1,
					\legato: 0.2,
				),
			])),
		])),
	)).play;
);


///// events in paralel in list
// PASS
(
	Pdef(\part, Pdef(\zed, 
			Pembed(Pseq([
				(
					\type: \pattern,
					\pattern: Pdef(\test_short),
					\dur: 0.3,
					\sustain: 1,
				),
				(
					\type: \pattern,
					\pattern: Pdef(\test_short2),
					\dur: 2,
					\sustain: 1,
				),
			]))
	)).play;
);


///// PatternEvent in paralel in list
// PASS
(
	Pdef(\part, Pdef(\zed, 
			Pembed(Pseq([
				PatternEvent((
					\type: \pattern,
					\pattern: Pdef(\test_short),
					\dur: 0.3,
					\sustain: 4,
				)),
				PatternEvent((
					\type: \pattern,
					\pattern: Pdef(\test_short2),
					\dur: 2,
					\sustain: 1,
				)),
			]))
	)).play;
);



///// PlayerEvent in paralel in list
// PASS
(
	Pdef(\part, Pdef(\zed, 
			Pembed(Pseq([
				PlayerEvent((
					\receiver: PlayerWrapper(Pdef(\test_short)),
					\dur: 0.3,
					\sustain: 1,
				)),
				PlayerEvent((
					\receiver: PlayerWrapper(Pdef(\test_short2)),
					\dur: 2,
					\sustain: 1,
				)),
			]))
	)).play;
);

///// Pembed is not needed
// PASS
(
	Pdef(\part, Pdef(\zed, 
			Pseq([
				PlayerEvent((
					\receiver: PlayerWrapper(Pdef(\test_short)),
					\dur: 0.3,
					\sustain: 1,
				)),
				PlayerEvent((
					\receiver: PlayerWrapper(Pdef(\test_short2)),
					\dur: 2,
					\sustain: 1,
				)),
			])
	)).play;
);

