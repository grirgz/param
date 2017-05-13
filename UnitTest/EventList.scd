// loading from a pattern
Log(\Param).debug("kkjkj")
Log(\Param).levels
(
~el = XEventList.newFrom(
	Pbind(
		\kitIndex, Pseq([0,1],8),
		\dur, 1/8
	),
	40000
)
)



/////////// KitTimeline event list loading

ParamProto.init(true)
ParamProto.init
FileSystemProject.load("libdef/soundfileview.scd")
FileSystemProject.load("libdef/synthplayrec.scd")
FileSystemProject.load("libdef/drumkit.scd")

///////////// PatKit
PatKitDef(\param_test).edit
//PatKitDef(\param_test).presetCompileString.editorInsert
(
PatKitDef('param_test').targets = [
	StepEvent.newFrom((instrument: 'playersec', bufnum: BufDef('amen'), pos: 3.5209750566893, sustain: 1)),
	StepEvent.newFrom((instrument: 'playersec', bufnum: BufDef('amen'), pos: 0.44151927437642, sustain: 1)),
	StepEvent.newFrom((instrument: 'playersec', bufnum: BufDef('amen'), pos: 0.84151927437642, sustain: 1)),
	StepEvent.newFrom((instrument: 'playersec', bufnum: BufDef('amen'), pos: 4.8298185941043, sustain: 1)),
];
);

KitTimeline(\param_test, PatKitDef(\param_test)).edit
KitTimeline(\param_test, PatKitDef(\param_test)).eventList
KitTimeline(\param_test, PatKitDef(\param_test)).eventList

/// simple pattern
(
KitTimeline(\param_test, PatKitDef(\param_test)).eventList = XEventList.newFrom(
	Pbind(
		\kitIndex, Pseq([0,1],8),
		\dur, 1/8
	),
	40000
)
)

/// simple pattern with rest
(
KitTimeline(\param_test, PatKitDef(\param_test)).eventList = XEventList.newFrom(
	Pbind(
		\kitIndex, Pseq([0,1],8),
		\isRest, Pseq([
			1,0,0,0, 1,1,1,0,
		],inf).coin.not,
		\dur, 1/8
	),
	40000
)
)

/// par pattern with rest
(
KitTimeline(\param_test, PatKitDef(\param_test)).eventList = XEventList.newFrom(
	Ppar(4.collect { arg x;
		Pbind(
			\kitIndex, Pseq([x],8),
			\isRest, Pseq([
				1,0,0,0, 1,1,1,0,
			],inf,x).coin.not,
			\dur, 1/8
		);

	}),
	40000
)
)

	(
	Ppar(4.collect { arg x;
		Pbind(
			\kitIndex, Pseq([x],8),
			\isRest, Pseq([
				1,0,0,0, 1,1,1,0,
			],inf,x).coin.not,
			\dur, 1/8
		);

	}).asStream.nextN(10,()).do(_.postln);
	)

	(
	Ppar(4.collect { arg x;
		Pbind(
			\kitIndex, Pseq([x],8),
			\isRest, Pseq([
				1,0,0,0, 1,1,1,0,
			],inf,x).coin.not,
			\dur, 1/8
		);

	}).asStream.nextN(10,()).do({ arg x; x.postln; x[\sustain].postln; x[\delta].postln });
	)

	(
		Pbind(
			\kitIndex, Pseq([0,1],8),
			\isRest, Pseq([
				1,0,0,0, 1,1,1,0,
			],inf).coin.not,
			\dur, 1/8
		).asStream.nextN(10,()).do({ arg x; x[\delta].postln });
	)


KitTimeline(\param_test).edit
KitTimeline(\param_test).eventList.print

//////////////////////////////// debug algo
(
KitTimeline(\param_test_algo, PatKitDef(\algokit)).eventList = XEventList.newFrom( Pfindur(8,Pdef(\algo1_inner1)));
)
KitTimeline(\param_test_algo).edit
KitTimeline(\param_test_algo).eventList.print
Pdef(\algo1_inner1).asStream.nextN(13,()).do(_.postln)
(
	Pdef(\algo1_inner1, 
	Pseq([
		Plazy({

			var pat1 = Ppar(
				12.collect({ arg x;
					var randcode = [
						0.1+rrand(0,0.4),
						rrand(1/2,2),
						2**rrand(1,8)/32,
						rrand(1,32)/8,
					];
					"randcode %: %".format(x, randcode).postln;
					Pseq([
						Pfindur(6,
							Pbind(
								\kitIndex, Pseq([x],inf),
								\isRest, Pseq( [
									[ 1,1,0,0,  ],
									[ 0,0,1,1,  ],
									[ 1,0,0,0,  ],
									[ 0,0,1,0,  ],
									[ 1,0,0,0, 0,0,0,0,  ],
									[ 1,0,1,0, 0,0,0,0,  ],
									[ 1,0,0,0, 1,0,1,0,  ],
									[ 1,0.2,0,0, 0.2,0,1,0,  ],
									[ 1,1,1,1, 0,0,0,0,  ],
									[ 0,0,0,0, 1,1,1,1,  ],
									[ 
										1,0,1,0, 1,0,1,0, 
										0,0,0,0, 0,0,0,0,
									],
									[ 
										0,0,0,0, 0,0,0,0,
										1,0,1,0, 1,0,1,0, 
									],
									[ 
										1,1,1,1, 0,0,0,0,
										0,0,0,0, 0,0,0,0,
									],
									[ 
										0,0,0,0, 0,0,0,0,
										1,1,1,1, 0,0,0,0,
									],
								].choose,inf).coin.not,
								\sustain, randcode[0],
								//\speed, randcode[1],
								\dur, randcode[3],
								\amp, 0.1
							)
						),
						Pfindur(2,
							Pbind(
								\kitIndex, Pseq([x],inf),
								\sustain, randcode[0],
								\isRest, Pseq([
									1,0,
								],inf).coin.not,
								\speed, randcode[1],
								\dur, randcode[2],
								\amp, 0.1
							)
						)
					])

				})
			);

			Pseq([
				pat1,
				Ppar([
					pat1,
					Pfindur(8, {
						var randcode = [
							0.1+rrand(0,0.4),
							rrand(1/2,2),
							rrand(1,32)/8,
							2**rrand(1,14)/32,
						];
						var x = 12.rand;
						Pbind(
							\kitIndex, Pseq([x],inf),
							\isRest, Pseq( [
								[ 1,1,0,0,  ],
								[ 0,0,1,1,  ],
								[ 1,0,0,0,  ],
								[ 0,0,1,0,  ],
							].choose ,inf).coin.not,
							\sustain, randcode[0],
							//\speed, randcode[1],
							\dur, randcode[3],
							\amp, 0.1
						)
					}.value),
					Pfindur(8, {
						var randcode = [
							0.1+rrand(0,0.4),
							rrand(1/2,2),
							rrand(1,32)/8,
							2**rrand(1,14)/32,
						];
						var x = 12.rand;
						Pbind(
							\kitIndex, Pseq([x],inf),
							\isRest, Pseq( [
								[ 1,1,0,0,  ],
								[ 0,0,1,1,  ],
								[ 1,0,0,0,  ],
								[ 0,0,1,0,  ],
							].choose ,inf).coin.not,
							\sustain, randcode[0],
							//\speed, randcode[1],
							\dur, randcode[3],
							\amp, 0.1
						)
					}.value),
				])
			])
		})
	],inf)
);
);
