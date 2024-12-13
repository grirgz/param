(
	~compare_value = { arg item, expected;
		var keycmp = true;
		case(
			{ item.isKindOf(SequenceableCollection) }, {
				if(expected.notNil) {
					item.do { arg slot, slotidx;
						keycmp = ~compare_value.(slot, expected[slotidx]) and: keycmp;
					};
				} {
					keycmp = false;
				}
			},
			{ item.isKindOf(Float) }, {
				if(expected.isNil) {
					keycmp = false;
				} {
					keycmp = ( item.equalWithPrecision(expected, 0.1) );
				};
			}, {
				keycmp = ( item == expected );
			}
		);
		keycmp;
	};
	~compare_unrolled = { arg unrolled, expected;
		var passed = true;
		unrolled.do { arg item, idx;
			var expitem = expected[idx];
			var cmp = ( item == expitem );
			//cmp.debug("--Event comparison %".format(idx));
			if(cmp.not) {
				item.keys.do { arg key;
					var keycmp = ~compare_value.(item[key], expitem[key]);
					if(keycmp.not and:{ [\play].includes(key).not }) {
						keycmp.debug("entry % comparison %: % == %".format(idx, key, item[key], expitem.obtain(idx).obtain(key)));
						passed = false;
					};
				};
				
			};
		};
		if(passed) {
			"====== TEST: OK".debug;
		} {
			"====== TEST: FAILED".debug;
		};
		passed
	};

ParamTimeline(\empty).eventList = TimelineEventList.newFrom([
	(\absTime: 0, \dur: 3.375, \label: 'start', \playDur: 3.375, \relDur: 3.375, \sustain: 0, \type: 'start', ),
	(\absTime: 8, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);

ParamTimeline(\one).eventList = TimelineEventList.newFrom([
	(\absTime: 0, \dur: 3.375, \label: 'start', \playDur: 3.375, \relDur: 3.375, \sustain: 0, \type: 'start', ),
	(\absTime: 3.375, \dur: 4.625, \legato: 1, \level: 0.69178082191781, \midinote: 0, \playDur: 4.625, \relDur: 4.625, \sustain: 1, ),
	(\absTime: 8, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);

ParamTimeline('two').eventList = TimelineEventList.newFrom([
	(\absTime: 0, \dur: 2.625, \label: 'start', \playDur: 2.625, \relDur: 2.625, \sustain: 0, \type: 'start', ),
	(\absTime: 2.625, \dur: 2.125, \legato: 1, \level: 0.66095890410959, \midinote: 0, \playDur: 2.125, \relDur: 2.125, \sustain: 1, ),
	(\absTime: 4.75, \dur: 3.25, \legato: 1, \level: 0.71575342465753, \midinote: 0, \playDur: 3.25, \relDur: 3.25, \sustain: 1, ),
	(\absTime: 8, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);

ParamTimeline('three').eventList = TimelineEventList.newFrom([
	(\absTime: 0, \dur: 1.375, \label: 'start', \playDur: 1.375, \relDur: 1.375, \sustain: 0, \type: 'start', ),
	(\absTime: 1.375, \dur: 2.0, \legato: 1, \level: 0.50684931506849, \midinote: 0, \playDur: 2.0, \relDur: 2.0, \sustain: 1, ),
	(\absTime: 3.375, \dur: 2.125, \legato: 1, \level: 0.58561643835616, \midinote: 0, \playDur: 2.125, \relDur: 2.125, \sustain: 1, ),
	(\absTime: 5.5, \dur: 2.5, \legato: 1, \level: 0.46232876712329, \midinote: 0, \playDur: 2.5, \relDur: 2.5, \sustain: 1, ),
	(\absTime: 8, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);

~expected = [ 
	// empty
[ 
	( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), 
	( 'instrument': 'env_segment', 'playDur': 8, 'dur': 8, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_empty'), 'curve': 0, 'delta': 8.0, 
  'relDur': 8, 'type': 'start', 'label': 'start' ), 

  	( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), 
  	( 'instrument': 'env_segment', 'playDur': 8, 'dur': 8, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_empty'), 'curve': 0, 'delta': 8.0, 
  'relDur': 8, 'type': 'start', 'label': 'start' ), 
  
  ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 8, 'dur': 8, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_empty'), 'curve': 0, 'delta': 8.0, 
  'relDur': 8, 'type': 'start', 'label': 'start' ), ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 8, 'dur': 8, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_empty'), 'curve': 0, 'delta': 8.0, 
  'relDur': 8, 'type': 'start', 'label': 'start' ), ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 8, 'dur': 8, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_empty'), 'curve': 0, 'delta': 8.0, 
  'relDur': 8, 'type': 'start', 'label': 'start' ) 
],
  
	// one
[ 
	( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), 
	( 'instrument': 'env_segment', 'playDur': 3.375, 'dur': 3.375, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_one'), 'curve': 0, 'delta': 3.375, 
  'relDur': 3.375, 'type': 'start', 'label': 'start' ), 
  	( 'instrument': 'env_segment', 'playDur': 4.625, 'dur': 4.625, 'time': 1.0, 
  'midinote': 0, 'sustain': 1, 'absTime': 3.375, 'out': BusDef('ParamTimeline_one'), 'legato': 1, 
  'curve': 0, 'level': 0.69178082191781, 'delta': 4.625, 'relDur': 4.625, 'type': 'rest' ), 
  
  ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 3.375, 'dur': 3.375, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_one'), 'curve': 0, 'delta': 3.375, 
  'relDur': 3.375, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 4.625, 'dur': 4.625, 'time': 1.0, 
  'midinote': 0, 'sustain': 1, 'absTime': 3.375, 'out': BusDef('ParamTimeline_one'), 'legato': 1, 
  'curve': 0, 'level': 0.69178082191781, 'delta': 4.625, 'relDur': 4.625, 'type': 'rest' ), ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 3.375, 'dur': 3.375, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_one'), 'curve': 0, 'delta': 3.375, 
  'relDur': 3.375, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 4.625, 'dur': 4.625, 'time': 1.0, 
  'midinote': 0, 'sustain': 1, 'absTime': 3.375, 'out': BusDef('ParamTimeline_one'), 'legato': 1, 
  'curve': 0, 'level': 0.69178082191781, 'delta': 4.625, 'relDur': 4.625, 'type': 'rest' ), ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ) 
],
  
	// two
  [ 
	( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), 
	( 'instrument': 'env_segment', 'playDur': 2.625, 'dur': 2.625, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_two'), 'curve': 0, 'delta': 2.625, 
  'relDur': 2.625, 'type': 'start', 'label': 'start' ), 
	( 'instrument': 'env_segment', 'playDur': 2.125, 'dur': 0, 'time': 2.125, 
  'midinote': 0, 'sustain': 2.125, 'array': [ 0.66095890410959 ], 'absTime': 2.625, 'next_absTime': 4.75, 
  'out': BusDef('ParamTimeline_two'), 'legato': 1, 'next_level': 0.71575342465753, 'curve': 0, 'level': 0.66095890410959, 
  'delta': 0, 'relDur': 2.125, 'type': 'bus' ), 
  	( 'instrument': 'env_segment', 'playDur': 2.125, 'dur': 2.125, 'time': 2.125, 
  'midinote': 0, 'sustain': 2.125, 'absTime': 2.625, 'next_absTime': 4.75, 'out': BusDef('ParamTimeline_two'), 
  'legato': 1, 'next_level': 0.71575342465753, 'curve': 0, 'level': 0.71575342465753, 'delta': 2.125, 
  'relDur': 2.125, 'type': 'note' ), 
  	( 'instrument': 'env_segment', 'playDur': 3.25, 'dur': 3.25, 'time': 1.0, 
  'midinote': 0, 'sustain': 1, 'absTime': 4.75, 'out': BusDef('ParamTimeline_two'), 'legato': 1, 
  'curve': 0, 'delta': 3.25, 'relDur': 3.25, 'type': 'rest' ), 
  
  	( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 2.625, 'dur': 2.625, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_two'), 'curve': 0, 'delta': 2.625, 
  'relDur': 2.625, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 2.125, 'dur': 0, 'time': 2.125, 
  'midinote': 0, 'sustain': 2.125, 'array': [ 0.66095890410959 ], 'absTime': 2.625, 'next_absTime': 4.75, 
  'out': BusDef('ParamTimeline_two'), 'legato': 1, 'next_level': 0.71575342465753, 'curve': 0, 'level': 0.66095890410959, 
  'delta': 0, 'relDur': 2.125, 'type': 'bus' ), ( 'instrument': 'env_segment', 'playDur': 2.125, 'dur': 2.125, 'time': 2.125, 
  'midinote': 0, 'sustain': 2.125, 'absTime': 2.625, 'next_absTime': 4.75, 'out': BusDef('ParamTimeline_two'), 
  'legato': 1, 'next_level': 0.71575342465753, 'curve': 0, 'level': 0.71575342465753, 'delta': 2.125, 
  'relDur': 2.125, 'type': 'note' ), ( 'instrument': 'env_segment', 'playDur': 3.25, 'dur': 3.25, 'time': 1.0, 
  'midinote': 0, 'sustain': 1, 'absTime': 4.75, 'out': BusDef('ParamTimeline_two'), 'legato': 1, 
  'curve': 0, 'delta': 3.25, 'relDur': 3.25, 'type': 'rest' ) ] ,


  // three
[ 
	( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), 
	( 'instrument': 'env_segment', 'playDur': 1.375, 'dur': 1.375, 'time': 0.0, 
	'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_three'), 'curve': 0, 'delta': 1.375, 
	'relDur': 1.375, 'type': 'start', 'label': 'start' ), 
	( 'instrument': 'env_segment', 'playDur': 2.0, 'dur': 0, 'time': 2.0, 
	'midinote': 0, 'sustain': 2.0, 'array': [ 0.50684931506849 ], 'absTime': 1.375, 'next_absTime': 3.375, 
	'out': BusDef('ParamTimeline_three'), 'legato': 1, 'next_level': 0.58561643835616, 'curve': 0, 'level': 0.50684931506849, 
	'delta': 0, 'relDur': 2.0, 'type': 'bus' ), 
	( 'instrument': 'env_segment', 'playDur': 2.0, 'dur': 2.0, 'time': 2.0, 
	'midinote': 0, 'sustain': 2.0, 'absTime': 1.375, 'next_absTime': 3.375, 'out': BusDef('ParamTimeline_three'), 
	'legato': 1, 'next_level': 0.58561643835616, 'curve': 0, 'level': 0.58561643835616, 'delta': 2.0, 
	'relDur': 2.0, 'type': 'note' ), 
	( 'instrument': 'env_segment', 'playDur': 2.125, 'dur': 2.125, 'time': 2.125, 
	'midinote': 0, 'sustain': 2.125, 'absTime': 3.375, 'next_absTime': 5.5, 'out': BusDef('ParamTimeline_three'), 
	'legato': 1, 'next_level': 0.46232876712329, 'curve': 0, 'level': 0.46232876712329, 'delta': 2.125, 
	'relDur': 2.125, 'type': 'note' ), 
	( 'instrument': 'env_segment', 'playDur': 2.5, 'dur': 2.5, 'time': 1.0, 
	'midinote': 0, 'sustain': 1, 'absTime': 5.5, 'out': BusDef('ParamTimeline_three'), 'legato': 1, 
	'curve': 0, 'delta': 2.5, 'relDur': 2.5, 'type': 'rest' ), 

  
  ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 1.375, 'dur': 1.375, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_three'), 'curve': 0, 'delta': 1.375, 
  'relDur': 1.375, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 2.0, 'dur': 0, 'time': 2.0, 
  'midinote': 0, 'sustain': 2.0, 'array': [ 0.50684931506849 ], 'absTime': 1.375, 'next_absTime': 3.375, 
  'out': BusDef('ParamTimeline_three'), 'legato': 1, 'next_level': 0.58561643835616, 'curve': 0, 'level': 0.50684931506849, 
  'delta': 0, 'relDur': 2.0, 'type': 'bus' ), ( 'instrument': 'env_segment', 'playDur': 2.0, 'dur': 2.0, 'time': 2.0, 
  'midinote': 0, 'sustain': 2.0, 'absTime': 1.375, 'next_absTime': 3.375, 'out': BusDef('ParamTimeline_three'), 
  'legato': 1, 'next_level': 0.58561643835616, 'curve': 0, 'level': 0.58561643835616, 'delta': 2.0, 
  'relDur': 2.0, 'type': 'note' ) ]

];

~unrolled = [\empty, \one, \two, \three].collect { arg key, idx;
	ParamTimeline(key).asPattern.asStream.nextN(10,Event.default)
};

~unrolled.do { arg item, idx;
	~compare_unrolled.(item, ~expected[idx])
};
);


(
ParamTimeline('start').eventList = TimelineEventList.newFrom([
	(\absTime: 0, \dur: 0.0, \label: 'start', \playDur: 0.0, \relDur: 0.0, \sustain: 0, \type: 'start', ),
	(\absTime: 0.0, \dur: 2.625, \legato: 1, \level: 0.56506849315068, \midinote: 0, \playDur: 2.625, \relDur: 2.625, \sustain: 1, ),
	(\absTime: 2.625, \dur: 5.375, \legato: 1, \level: 0.58219178082192, \midinote: 0, \playDur: 5.375, \relDur: 5.375, \sustain: 1, ),
	(\absTime: 8, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);


ParamTimeline('end').eventList = TimelineEventList.newFrom([
	(\absTime: 0, \dur: 4.5, \label: 'start', \playDur: 4.5, \relDur: 4.5, \sustain: 0, \type: 'start', ),
	(\absTime: 4.5, \dur: 3.5, \legato: 1, \level: 0.58561643835616, \midinote: 0, \playDur: 3.5, \relDur: 3.5, \sustain: 1, ),
	(\absTime: 8.0, \curve: 0, \dur: 0.0, \legato: 1, \level: 0.62328767123288, \midinote: 0, \playDur: 0.0, \relDur: 0.0, \sustain: 1, ),
	(\absTime: 8, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);

ParamTimeline('startend').eventList = TimelineEventList.newFrom([
	(\absTime: 0, \dur: 0.0, \label: 'start', \playDur: 0.0, \relDur: 0.0, \sustain: 0, \type: 'start', ),
	(\absTime: 0.0, \dur: 8.0, \legato: 1, \level: 0.43150684931507, \midinote: 0, \playDur: 8.0, \relDur: 8.0, \sustain: 1, ),
	(\absTime: 8.0, \dur: 0.0, \legato: 1, \level: 0.55479452054795, \midinote: 0, \playDur: 0.0, \relDur: 0.0, \sustain: 1, ),
	(\absTime: 8, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);

ParamTimeline('startendlong').eventList = TimelineEventList.newFrom([
	(\absTime: 0, \dur: 0.0, \label: 'start', \playDur: 0.0, \relDur: 0.0, \sustain: 0, \type: 'start', ),
	(\absTime: 0.0, \dur: 2.625, \legato: 1, \level: 0.51369863013699, \midinote: 0, \playDur: 2.625, \relDur: 2.625, \sustain: 1, ),
	(\absTime: 2.625, \dur: 2.5, \legato: 1, \level: 0.33904109589041, \midinote: 0, \playDur: 2.5, \relDur: 2.5, \sustain: 1, ),
	(\absTime: 5.125, \dur: 1.75, \legato: 1, \level: 0.6472602739726, \midinote: 0, \playDur: 1.75, \relDur: 1.75, \sustain: 1, ),
	(\absTime: 6.875, \dur: 1.125, \legato: 1, \level: 0.37671232876712, \midinote: 0, \playDur: 1.125, \relDur: 1.125, \sustain: 1, ),
	(\absTime: 8.0, \curve: 0, \dur: 0.0, \legato: 1, \level: 0.48972602739726, \midinote: 0, \playDur: 0.0, \relDur: 0.0, \sustain: 1, ),
	(\absTime: 8, \dur: 0, \label: 'end', \playDur: 0, \relDur: 0, \type: 'end', ),
]);

~expected = [ 
	[ 
		// start
		( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), 
		( 'instrument': 'env_segment', 'playDur': 0.0, 'dur': 0.0, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_start'), 'curve': 0, 'delta': 0.0, 
  'relDur': 0.0, 'type': 'start', 'label': 'start' ), 
  		( 'instrument': 'env_segment', 'playDur': 2.625, 'dur': 0, 'time': 2.625, 
  'midinote': 0, 'sustain': 2.625, 'array': [ 0.56506849315068 ], 'absTime': 0.0, 'next_absTime': 2.625, 
  'out': BusDef('ParamTimeline_start'), 'legato': 1, 'next_level': 0.58219178082192, 'curve': 0, 'level': 0.56506849315068, 
  'delta': 0, 'relDur': 2.625, 'type': 'bus' ), 
  		( 'instrument': 'env_segment', 'playDur': 2.625, 'dur': 2.625, 'time': 2.625, 
  'midinote': 0, 'sustain': 2.625, 'absTime': 0.0, 'next_absTime': 2.625, 'out': BusDef('ParamTimeline_start'), 
  'legato': 1, 'next_level': 0.58219178082192, 'curve': 0, 'level': 0.58219178082192, 'delta': 2.625, 
  'relDur': 2.625, 'type': 'note' ), 
  		( 'instrument': 'env_segment', 'playDur': 5.375, 'dur': 5.375, 'time': 1.0, 
  'midinote': 0, 'sustain': 1, 'absTime': 2.625, 'out': BusDef('ParamTimeline_start'), 'legato': 1, 
  'curve': 0, 'delta': 5.375, 'relDur': 5.375, 'type': 'rest' ), 
  		
  		( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), 
		( 'instrument': 'env_segment', 'playDur': 0.0, 'dur': 0.0, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_start'), 'curve': 0, 'delta': 0.0, 
  'relDur': 0.0, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 2.625, 'dur': 0, 'time': 2.625, 
  'midinote': 0, 'sustain': 2.625, 'array': [ 0.56506849315068 ], 'absTime': 0.0, 'next_absTime': 2.625, 
  'out': BusDef('ParamTimeline_start'), 'legato': 1, 'next_level': 0.58219178082192, 'curve': 0, 'level': 0.56506849315068, 
  'delta': 0, 'relDur': 2.625, 'type': 'bus' ), ( 'instrument': 'env_segment', 'playDur': 2.625, 'dur': 2.625, 'time': 2.625, 
  'midinote': 0, 'sustain': 2.625, 'absTime': 0.0, 'next_absTime': 2.625, 'out': BusDef('ParamTimeline_start'), 
  'legato': 1, 'next_level': 0.58219178082192, 'curve': 0, 'level': 0.58219178082192, 'delta': 2.625, 
  'relDur': 2.625, 'type': 'note' ), ( 'instrument': 'env_segment', 'playDur': 5.375, 'dur': 5.375, 'time': 1.0, 
  'midinote': 0, 'sustain': 1, 'absTime': 2.625, 'out': BusDef('ParamTimeline_start'), 'legato': 1, 
  'curve': 0, 'delta': 5.375, 'relDur': 5.375, 'type': 'rest' ) ], 
  
  [ 
	  ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 4.5, 'dur': 4.5, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_end'), 'curve': 0, 'delta': 4.5, 
  'relDur': 4.5, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 3.5, 'dur': 0, 'time': 3.5, 
  'midinote': 0, 'sustain': 3.5, 'array': [ 0.58561643835616 ], 'absTime': 4.5, 'next_absTime': 8.0, 
  'out': BusDef('ParamTimeline_end'), 'legato': 1, 'next_level': 0.62328767123288, 'curve': 0, 'level': 0.58561643835616, 
  'delta': 0, 'relDur': 3.5, 'type': 'bus' ), ( 'instrument': 'env_segment', 'playDur': 3.5, 'dur': 3.5, 'time': 3.5, 
  'midinote': 0, 'sustain': 3.5, 'absTime': 4.5, 'next_absTime': 8.0, 'out': BusDef('ParamTimeline_end'), 
  'legato': 1, 'next_level': 0.62328767123288, 'curve': 0, 'level': 0.62328767123288, 'delta': 3.5, 
  'relDur': 3.5, 'type': 'note' ), ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 4.5, 'dur': 4.5, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_end'), 'curve': 0, 'delta': 4.5, 
  'relDur': 4.5, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 3.5, 'dur': 0, 'time': 3.5, 
  'midinote': 0, 'sustain': 3.5, 'array': [ 0.58561643835616 ], 'absTime': 4.5, 'next_absTime': 8.0, 
  'out': BusDef('ParamTimeline_end'), 'legato': 1, 'next_level': 0.62328767123288, 'curve': 0, 'level': 0.58561643835616, 
  'delta': 0, 'relDur': 3.5, 'type': 'bus' ), ( 'instrument': 'env_segment', 'playDur': 3.5, 'dur': 3.5, 'time': 3.5, 
  'midinote': 0, 'sustain': 3.5, 'absTime': 4.5, 'next_absTime': 8.0, 'out': BusDef('ParamTimeline_end'), 
  'legato': 1, 'next_level': 0.62328767123288, 'curve': 0, 'level': 0.62328767123288, 'delta': 3.5, 
  'relDur': 3.5, 'type': 'note' ), ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 4.5, 'dur': 4.5, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_end'), 'curve': 0, 'delta': 4.5, 
  'relDur': 4.5, 'type': 'start', 'label': 'start' ) ], 
  
  [ 
	  ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 0.0, 'dur': 0.0, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_startend'), 'curve': 0, 'delta': 0.0, 
  'relDur': 0.0, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 8.0, 'dur': 0, 'time': 8.0, 
  'midinote': 0, 'sustain': 8.0, 'array': [ 0.43150684931507 ], 'absTime': 0.0, 'next_absTime': 8.0, 
  'out': BusDef('ParamTimeline_startend'), 'legato': 1, 'next_level': 0.55479452054795, 'curve': 0, 'level': 0.43150684931507, 
  'delta': 0, 'relDur': 8.0, 'type': 'bus' ), ( 'instrument': 'env_segment', 'playDur': 8.0, 'dur': 8.0, 'time': 8.0, 
  'midinote': 0, 'sustain': 8.0, 'absTime': 0.0, 'next_absTime': 8.0, 'out': BusDef('ParamTimeline_startend'), 
  'legato': 1, 'next_level': 0.55479452054795, 'curve': 0, 'level': 0.55479452054795, 'delta': 8.0, 
  'relDur': 8.0, 'type': 'note' ), ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 0.0, 'dur': 0.0, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_startend'), 'curve': 0, 'delta': 0.0, 
  'relDur': 0.0, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 8.0, 'dur': 0, 'time': 8.0, 
  'midinote': 0, 'sustain': 8.0, 'array': [ 0.43150684931507 ], 'absTime': 0.0, 'next_absTime': 8.0, 
  'out': BusDef('ParamTimeline_startend'), 'legato': 1, 'next_level': 0.55479452054795, 'curve': 0, 'level': 0.43150684931507, 
  'delta': 0, 'relDur': 8.0, 'type': 'bus' ), ( 'instrument': 'env_segment', 'playDur': 8.0, 'dur': 8.0, 'time': 8.0, 
  'midinote': 0, 'sustain': 8.0, 'absTime': 0.0, 'next_absTime': 8.0, 'out': BusDef('ParamTimeline_startend'), 
  'legato': 1, 'next_level': 0.55479452054795, 'curve': 0, 'level': 0.55479452054795, 'delta': 8.0, 
  'relDur': 8.0, 'type': 'note' ), ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 0.0, 'dur': 0.0, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_startend'), 'curve': 0, 'delta': 0.0, 
  'relDur': 0.0, 'type': 'start', 'label': 'start' ) ], 
  
  [ 
	  ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 0.0, 'dur': 0.0, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_startendlong'), 'curve': 0, 'delta': 0.0, 
  'relDur': 0.0, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 2.625, 'dur': 0, 'time': 2.625, 
  'midinote': 0, 'sustain': 2.625, 'array': [ 0.51369863013699 ], 'absTime': 0.0, 'next_absTime': 2.625, 
  'out': BusDef('ParamTimeline_startendlong'), 'legato': 1, 'next_level': 0.33904109589041, 'curve': 0, 'level': 0.51369863013699, 
  'delta': 0, 'relDur': 2.625, 'type': 'bus' ), ( 'instrument': 'env_segment', 'playDur': 2.625, 'dur': 2.625, 'time': 2.625, 
  'midinote': 0, 'sustain': 2.625, 'absTime': 0.0, 'next_absTime': 2.625, 'out': BusDef('ParamTimeline_startendlong'), 
  'legato': 1, 'next_level': 0.33904109589041, 'curve': 0, 'level': 0.33904109589041, 'delta': 2.625, 
  'relDur': 2.625, 'type': 'note' ), ( 'instrument': 'env_segment', 'playDur': 2.5, 'dur': 2.5, 'time': 2.5, 
  'midinote': 0, 'sustain': 2.5, 'absTime': 2.625, 'next_absTime': 5.125, 'out': BusDef('ParamTimeline_startendlong'), 
  'legato': 1, 'next_level': 0.6472602739726, 'curve': 0, 'level': 0.6472602739726, 'delta': 2.5, 
  'relDur': 2.5, 'type': 'note' ), ( 'instrument': 'env_segment', 'playDur': 1.75, 'dur': 1.75, 'time': 1.75, 
  'midinote': 0, 'sustain': 1.75, 'absTime': 5.125, 'next_absTime': 6.875, 'out': BusDef('ParamTimeline_startendlong'), 
  'legato': 1, 'next_level': 0.37671232876712, 'curve': 0, 'level': 0.37671232876712, 'delta': 1.75, 
  'relDur': 1.75, 'type': 'note' ), ( 'instrument': 'env_segment', 'playDur': 1.125, 'dur': 1.125, 'time': 1.125, 
  'midinote': 0, 'sustain': 1.125, 'absTime': 6.875, 'next_absTime': 8.0, 'out': BusDef('ParamTimeline_startendlong'), 
  'legato': 1, 'next_level': 0.48972602739726, 'curve': 0, 'level': 0.48972602739726, 'delta': 1.125, 
  'relDur': 1.125, 'type': 'note' ), ( 'startCursorEvent': true, 'dur': 0, 'type': 'rest', 'play': { "open Function" } ), ( 'instrument': 'env_segment', 'playDur': 0.0, 'dur': 0.0, 'time': 0.0, 
  'sustain': 0, 'absTime': 0, 'out': BusDef('ParamTimeline_startendlong'), 'curve': 0, 'delta': 0.0, 
  'relDur': 0.0, 'type': 'start', 'label': 'start' ), ( 'instrument': 'env_segment', 'playDur': 2.625, 'dur': 0, 'time': 2.625, 
  'midinote': 0, 'sustain': 2.625, 'array': [ 0.51369863013699 ], 'absTime': 0.0, 'next_absTime': 2.625, 
  'out': BusDef('ParamTimeline_startendlong'), 'legato': 1, 'next_level': 0.33904109589041, 'curve': 0, 'level': 0.51369863013699, 
  'delta': 0, 'relDur': 2.625, 'type': 'bus' ) 
] 
];

~unrolled = [\start, \end, \startend, \startendlong].collect { arg key, idx;
	ParamTimeline(key).asPattern.asStream.nextN(10,Event.default)
};

~unrolled.do { arg item, idx;
	~compare_unrolled.(item, ~expected[idx])
};

)
