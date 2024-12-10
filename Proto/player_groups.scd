/////////// individuals

// produce and play an event from a pattern for each .play call 
// designed to be a child of some parent collection class where targets are stored
ProtoTemplateDef(\StreamPlayer, (
	new: { arg self, parent, x;
		self = self.deepCopy;
	
		
		self.parent_self = {parent};
		self.index = x;
	
		self;
	},

	target: { arg self;
		self.parent_self.getTarget(self.index);
	},

	target_: { arg self, val;
		self.parent_self.setTarget(self.index, val);
	},

	defaultTarget: { arg self;
		self.parent_self.defaultTarget
	},

	pattern: { arg self;
		self.target.pattern;
	},

	playMode: { arg self;
		self.parent_self.playMode
	},

	isEmpty: { arg self;
		self.target.isNil or: { self.target == self.defaultTarget }
	},

	preChain: { arg self;
		self.parent_self.preChain
	},

	postChain: { arg self;
		self.parent_self.postChain
	},

	play: { arg self;
		if(self.isEmpty.not) {

			if(self.stream.isNil) {
				var pat = self.pattern;
				if(self.preChain.notNil) {
					pat = pat <> self.preChain;
				};
				if(self.postChain.notNil) {
					pat = self.postChain <> pat
				};
				self.stream = pat.asStream;
				//[self.stream, pat, pat.source].debug("set stream from pat")
			};
			if(self.cur_ev.notNil) { // required for not overwriting player if already playing (too fast pushing)
				//self.ev.debug("self.cur_ev not nil, release");
				self.cur_ev.release;
				self.cur_ev = nil;
			};
			if(self.ev.isNil) {
				//self.ev.debug("self.ev is nil, setting it to Event.default");
				self.ev = Event.default;
			};
			//self.ev.debug("play: before next: ev");
			// FIXME: is self.ev used as parent ? so each event is parent of the next ?
			// something is wrong
			self.ev = self.stream.next(Event.default);
			//self.ev.debug("self.ev is the next event to be played");
			//self.ev = self.stream.next(self.ev);
			if(self.ev.notNil) {
				//self.ev.debug("play: ev");
				self.raw_ev = self.ev.copy; // for recording, find better property name
				self.raw_ev[\kitIndex] = self.index;
				self.play_ev = self.ev.copy;
				self.play_ev[\isRest] = false;
				self.play_ev[\sustain] = inf;
				//self.play_ev[\latency] = \; // not supported in 3.9.1
				self.play_ev[\latency] = \no; // TODO: find standard way
				self.cur_ev = self.play_ev.play;
				self.changed(\play);
				self.parent_self.changed(\play, self.index);
			}
		} 
	},

	asPlayerEvent: { arg self; 
		// PlayerEvent == does not embed in stream
		// but asPlayerEvent is taken by PlayerWrapper
		//self.raw_ev.debug("asPlayerEvent: raw_ev");
		self.raw_ev;
	},

	reboot: { arg self;
		self.stream = nil;
	},

	isPlaying: { arg self;
		if(self.cur_ev.isNil) {
			false;
		} {
			self.cur_ev.isPlaying;
		};
	},

	stop: { arg self;
		self.changed(\stop);
		self.parent_self.changed(\stop, self.index);
		self.cur_ev.release;
		self.cur_ev = nil;
	},
));

// follow a pattern but note is given by the caller
ProtoTemplateDef(\NotePlayer, (
	new: { arg self, parent, x, stream, pre_event, post_event;
		self = self.deepCopy;
	
		
		self.parent_self = {parent};
		self.stream = stream;
		self.post_event = post_event;
		self.pre_event = pre_event;
		self.index = x;
	
		self;
	},

	play: { arg self, local_pre_event, local_post_event;

		// event generating
		if(self.cur_ev.notNil) { // required for not overwriting player if already playing (too fast pushing)
			self.cur_ev.release;
			self.cur_ev = nil;
		};
		if(self.ev.isNil) {
			self.ev = Event.default;
		};
		//Log(\Param).debug("NotePlayer: play: ev 1: %", self.ev);
		self.ev = self.ev.copy.putAll(self.pre_event);
		self.ev = self.ev.copy.putAll(local_pre_event ?? {()});
		//self.ev.debug("play: before next: ev");
		//Log(\Param).debug("NotePlayer: play: ev 2: %", self.ev);
		// FIXME: next should take Event.default
		self.ev = self.stream.next(self.ev);
		//self.ev.debug("play: ev");

		// processing
		//Log(\Param).debug("NotePlayer: play: ev 3: %", self.ev);
		self.ev.putAll(self.post_event);
		self.ev.putAll(local_post_event ?? { () });
		//Log(\Param).debug("NotePlayer: play: ev 4: %", self.ev);

		// recording
		self.raw_ev = self.ev.copy; // for recording, find better property name
		self.raw_ev[\kitIndex] = self.index;
		//Log(\Param).debug("NotePlayer: play: ev 5: %", self.ev);

		// playing
		self.play_ev = self.ev.copy;
		//Log(\Param).debug("NotePlayer: play: ev 6: %", self.ev);
		self.play_ev[\isRest] = false;
		self.play_ev[\sustain] = inf;
		self.play_ev[\latency] = Ref(0);
		//Log(\Param).debug("NotePlayer: play: ev 7: %", self.ev);
		self.play_ev.releaseServerNode = #{ |releaseTime|
			var sendGate, msg;
			~latency = 0;
			if(~id.notNil) {

				releaseTime = if(releaseTime.isNil) { 0.0 } { -1.0 - releaseTime };
				sendGate = ~sendGate ? ~hasGate;

				if(sendGate) {
					~server.sendBundle(~latency, *["/n_set", ~id, "gate", releaseTime].flop);
				} {
					~server.sendBundle(~latency, ["/n_free"] ++ ~id);
				};
				~isPlaying = false;
			};
		};
		//Log(\Param).debug("NotePlayer: play: ev 8: %", self.ev);
		self.cur_ev = self.play_ev.play;
		//Log(\Param).debug("NotePlayer: play: ev 9: %", self.ev);
		self.changed(\play);
		//Log(\Param).debug("play: raw_ev:%, idx:%, h:%", self.raw_ev, self.index, self.hash);
		//Log(\Param).debug("NotePlayer: play: ev 10: %", [ self.play_ev, self.cur_ev ]);
		self.parent_self.changed(\play, self.index);
	},

	asPlayerEvent: { arg self; 
		// PlayerEvent == does not embed in stream
		// but asPlayerEvent is taken by PlayerWrapper
		//Log(\Param).debug("asPlayerEvent: raw_ev:%, idx:%, h:%", self.raw_ev, self.index, self.hash);
		self.raw_ev;
	},

	isPlaying: { arg self;
		if(self.cur_ev.isNil) {
			false;
		} {
			self.cur_ev.isPlaying;
		};
	},

	stop: { arg self;
		self.changed(\stop);
		self.parent_self.changed(\stop, self.index);
		self.cur_ev.release;
		self.cur_ev = nil;
	},
));


////////// collectives

// act as a drum kit, play the player in the group according to midinote
// this use streamPlayer as children
// used by KitTimeline when pattern list is provided
ProtoTemplateDef(\ParPlayerGroup, (
	new: { arg self, list;
		self = self.deepCopy;
	
		self.init(list);
	
		self;
	},

	playMode: \gate, // play on key down and stop on key up

	init: { arg self, list;
		self.targets = list;
	},

	children: { arg self;
		self[\children] = SparseArray.new;
		self[\children];
	},

	source_: { arg self, val;
		self.targets = val;
	},

	source: { arg self;
		self.targets;
	},

	convert: { arg self, val;
		val
	},

	targets_: { arg self, val;
		self[\targets] = val.as(SparseArray);
		self.children.do({ arg x; x.reboot });
	},

	getTarget: { arg self, idx;
		self[\targets][idx]
	},

	setTarget: { arg self, idx, val;
		if(self[\targets].size <= idx) {
			self[\targets].tryPerform(\extend, idx+1)
		};
		self[\targets][idx] = val;
	},

	reboot: { arg self;
		self.children.reject(_.isNil).do(_.reboot)
	},

	preChain_: { arg self, val;
		self[\preChain] = val;
		self.reboot;
	},

	startControl: { arg self;
		if(self.controller.notNil) {
			self.controller.startControl((wrapper:{self}));
		}
	},

	controller_: { arg self, val;
		self[\controller] = val;
		self.startControl;
	},

	postChain_: { arg self, val;
		self[\postChain] = val;
		self.reboot;
	},

	makeListener: { arg self, fun;
		// FIXME: not the good way, need a omni controller
		var listenfun = { arg target, msg ... args;
			//Log(\Param).debug("playergroup: listenfun %", [target, msg, args]);
			fun.(self.elAt(args[0]), msg, *args);
		};
		SimpleController.new(self).put(\play, listenfun).put(\stop, listenfun)
	},

	newChild: { arg parent_self, x;
	//	PlayerWrapper(
			ProtoTemplateDef(\StreamPlayer).new(parent_self, x)
	//	)
	},

	elDo: { arg self, fun;
		self.targets.do ({ arg target, idx;
			fun.(self.elAt(idx), idx)
		})
	},

	elCollect: { arg self, fun;
		self.targets.collect ({ arg target, idx;
			fun.(self.elAt(idx), idx)
		})
	},


	elAt: { arg self, x;
		var child;
		child = self.children[x];
		if(child.isNil) {
			child = self.newChild(x);
			self.children[x] = child;
		};
		child;
	},

	childAt: { arg self, x;
		self.elAt(x)
	},
	
	presetCompileString: { arg self, varname="~ppg";
		"%.targets = [\n%];".format(
			varname,
			self.targets.collect({ arg target, idx;
				var res;
				res = "\t%,\n".format(target.asCompileString);
				res;
			}).join
		)
	},

	asPattern: { arg self, score;
		var pat = PdrumStep(self.targets, score, 1, key:\kitIndex);
		pat
	},
));



// seq because a single pattern is read instead of one pattern per note index in parPlayerGroup
// this use notePlayer as children
// used by NoteTimeline
ProtoTemplateDef(\SeqPlayerGroup, (
	eventType: \SeqPlayerGroup,
	new: { arg self, pat;
		var res;

		if(pat.isKindOf(ProtoClass)) {
			res = switch(pat.eventType,
				\SeqPlayerGroup, { pat },
				\noteTimeline, { pat.wrapper },
			);
		};

		if(res.notNil) {
			res;
		} {
			self = self.deepCopy;

			self.init(pat);

			self;
		}
	},

	init: { arg self, pat;
		self.target = pat;
		self.children = SparseArray.new;
	},

	makeListener: { arg self, fun;
		var listenfun = { arg target, msg ... args;
			//Log(\Param).debug("playergroup: listenfun %", [msg, args]);
			fun.(self.elAt(args[0]), msg, *args);
		};
		SimpleController.new(self).put(\play, listenfun).put(\stop, listenfun)
	},

	chainingMode: \pre, 

	stream: { arg self;
		var targ;
		if(self[\prStream].isNil) {
			if(self.target.isKindOf(Pattern)) {
				self[\prStream] = self.target.asStream;
			} {
				self[\prStream] = self.target.asPattern.asStream;
			}
		};
		self[\prStream]
	},

	newChild: { arg parent_self, x;
		var post_event = (), pre_event = ();
		if(parent_self.chainingMode == \pre) {
			pre_event = (midinote:x);
		} {
			post_event = (midinote:x);
		};
	//	PlayerWrapper(
			ProtoTemplateDef(\NotePlayer).new(parent_self, x, parent_self.stream, pre_event, post_event);
	//	)
	},

	elDo: { arg self, fun;
		self.targets.do ({ arg target, idx;
			fun.(self.elAt(idx), idx)
		})
	},

	elAt: { arg self, x;
		var child;
		child = self.children[x];
		if(child.isNil) {
			child = self.newChild(x);
			self.children[x] = child;
		};
		child;
	},

	childAt: { arg self, x;
		self.elAt(x)
	},

	stop: { arg self;
		self.children.do(_.stop)
	},
));



// specialisation of parPlayerGroup for PatKitDef: compile string and Def design
// can't use ProtoTemplateDef because 'all' variable is already used
// used by KitTimeline through PatKitDef
ProtoClassDef(\PatKitDef, (
	protoClass: \PatKitDef,
	parent: ProtoTemplateDef(\ParPlayerGroup),
	all: { PresetDictionary.new(\patKitDef) },
	new: { arg self, key, instrs;

		if(self.all[key].isNil) {
			self = ProtoClass((parent: ProtoClassDef(\PatKitDef)));
			if(instrs.notNil) {
				if(instrs.isSequenceableCollection) {
					self.source = instrs;
				} {
					// allow for copy by using PatKitDef(\xx2, PatKitDef(\xx))
					try {
						if(instrs.protoClass == \PatKitDef) {
							self.source = instrs.source.deepCopy;
						}
					}
				}
			};
			self.key = key;
			self.all[key] = self;
		} {
			self = self.all[key];
			if(instrs.notNil) {
				self.source = instrs;
			};
		};

		// make default source for gui editing
		if(self.source.isNil) {
			self.source = SparseArray.new;
		};
		
		self;
	},

	edit: { arg self;
		WindowDef("PatKitDefEditor_%".format(self.key).asSymbol, WindowDef(\PatKitDefEditor)).front(self);
	},

	convertOldStepEventToEvent: { arg self, stepevent;
		if(stepevent.isNil) {
			nil
		} {
			stepevent.as(Event)
		};
	},


	newChild: { arg parent_self, x;
		ProtoTemplateDef(\PatKitDefChild).new(parent_self, x)
	},

	/////// target lists

	targets_: { arg self, targetlist;
		if(targetlist.first.isKindOf(StepEvent)) {
			Log(\Param).debug("Converting old StepEvent to Event");
			targetlist = targetlist.collect({ arg x; self.convertOldStepEventToEvent(x) });
			Log(\Param).debug("Converting old StepEvent to Event2");
			self.targetsData = targetlist;
		} {
			targetlist.do{ arg target, idx; 
				self.setTarget(idx, target);
				self.getTargetDataProxy(idx).set(\instrument, nil);
				self.getPattern(idx); // just for update
			};
		};
	},

	targets: { arg self;
		self[\targetProxyList].collect { arg item, idx;
			self.getTarget(idx)
		};
	},

	targetsData_: { arg self, datalist;
		datalist.do { arg data, idx;
			self.setTargetData(idx, data)
		};
		//self.targets.do { arg target, idx;
			//var data;
			//if(target.isKindOf(StepEvent)) {
				//// no data for StepEvent
			//} {
				//data = self.targetsData[idx];
				//if(data.notNil) {
					//data.keys.do { arg key;
						//target.proxy.set(key, data[key])
					//};
				//};
			//};
			
		//};
	},

	targetsData: { arg self;
		self[\targetDataProxyList].size.collect { arg idx;
			self.getTargetData(idx);
		};
	},

	/////// target

	getTargetProxy: { arg self, idx; // default value
		// return the EventPatternProxy
		if(self[\targetProxyList].isNil) {
			self[\targetProxyList] = SparseArray();
		};
		//if(self[\targets].size <= idx) {
			//self[\targets].tryPerform(\extend, idx+1)
		//};
		if(self[\targetProxyList][idx].isNil) {
			self[\targetProxyList][idx] = self.defaultTargetProxy;
		};
		self[\targetProxyList][idx]
	},

	getTarget: { arg self, idx; 
		self.getTargetProxy(idx).source;
	},

	setTarget: { arg self, idx, val; 
		// accept the source, not the proxy itself, not symetrical with getTarget
		//[idx,val].debug("setTarget");
		self.getTargetProxy(idx).source = val;
	},

	defaultTargetProxy: { arg self;
		EventPatternProxy(Pbind());
	},

	/////// target data

	getTargetDataProxy: { arg self, idx; 
		// return the EventPatternProxy
		//Log(\Param).debug("=== %.getTargetDataProxy: %, %".format(self.key, idx, self[\targetDataProxyList]));
		if(self[\targetDataProxyList].isNil) {
			self[\targetDataProxyList] = SparseArray();
		};
		//self[\targetDataProxyList].debug("before targetDataProxyList");
		//self[\targetDataProxyList].dump;
		if(self[\targetDataProxyList][idx].isNil) {
			self[\targetDataProxyList][idx] = self.defaultTargetDataProxy;
			//self[\targetDataProxyList].debug("after targetDataProxyList");
			//self[\targetDataProxyList].dump;
			//self[\targetDataProxyList][idx].debug("getTargetDataProxy: new! idx=%".format(idx));
			//self[\targetDataProxyList][idx].envir.debug("getTargetDataProxy: envir");
		};
		self[\targetDataProxyList][idx];
	},

	setTargetData: { arg self, idx, val; 
		// accept only Event object, not symetrical with getTargetData
		// used in presetCompileString
		var proxy;
		proxy = self.getTargetDataProxy(idx);
		//[idx,val, proxy.envir].debug("setTargetData");
		proxy.envir.clear;
		if(val.isNil) {
			Log(\Param).debug("setTargetData %: val is nil, clearing child".format(idx));
			val = self.defaultTargetDataProxy.envir;
		};
		val.keys.do { arg key, idx;
			proxy.setVal(key, val[key]);
		};
	},

	getTargetData: { arg self, idx;
		self.getTargetDataProxy(idx).envir
	},

	defaultTargetDataProxy: { arg self;
		EventPatternProxy(Pbind()).set(\instrument, \saw);
	},

	/////// pattern

	getPattern: { arg self, idx; // child pattern
		if(self.patterns.isNil) {
			self.patterns = SparseArray();
		};
		if(self.patterns[idx].isNil) {
			self.patterns[idx] = EventPatternProxy(self.getTargetDataProxy(idx) <> self.getTargetProxy(idx));
			//self.patterns[idx].debug("CREATED PATTERN %".format(idx));
			//self.patterns[idx].source.patterns.first.dump;
		};
		self.patterns[idx];
	},

	getPatternList: { arg self;
		self.lastChildIndex.collect { arg idx;
			self.getPattern(idx)
		};
	},

	updatePattern: { arg self, idx;
		// useful only for debug because proxies should be updated automatically
		var pat;
		var data;
		var chain;
		pat = self.getPattern(idx);
		chain = pat.source;
		//chain.debug("before chain");
		//chain.dump;
		data = pat.source.patterns.first;
		//data.debug("before targetData");
		//data.dump;
		self.getPattern(idx).source = self.getTargetDataProxy(idx) <> self.getTargetProxy(idx);
		pat = self.getPattern(idx);
		chain = pat.source;
		//chain.debug("after chain");
		//chain.dump;
		data = pat.source.patterns.first;
		//data.debug("after targetData");
		//data.dump;
	},

	setPattern: { arg self;
		
	},

	asPattern: { arg self, score; // PatKitDef pattern
		var pat = PdrumStep(self.patterns, score, 1, key:\kitIndex);
		pat
	},

	/////// general children methods

	clear: { arg self;
		self.all[self.key] = nil;
		nil;
	},

	clearChildren: { arg self;
		// TODO
		self[\patterns] = nil;
		self[\targets] = nil;
		self[\targetsData] = nil;
	},

	lastChildIndex: { arg self;
		var datalist;
		datalist = self[\targetDataProxyList].copy;
		while { datalist.size > 0 and: { datalist.last == nil or: { self.childAt(datalist.size-1).isEmpty } } } { 
			datalist.pop 
		}; 
		datalist.size;
	},

	clearChild: { arg self, idx;
		//idx.debug("clearChild");
		self.setTarget(idx, self.defaultTargetProxy.source);
		self.setTargetData(idx, self.defaultTargetDataProxy.envir);
		self.getPattern(idx);
	},

	pasteChild: { arg self, idx, child;
		//idx.debug("pasteChild");
		//self[\targetDataProxyList].debug("pasteChild targetDataProxyList 1");
		self.setTarget(idx, child.target.copy);
		//self[\targetDataProxyList].debug("pasteChild targetDataProxyList 2");
		self.setTargetData(idx, child.targetData.copy);
		//self[\targetDataProxyList].debug("pasteChild targetDataProxyList 3");
		// no copy of targetData source for the moment
		self.getPattern(idx);
		//self[\targetDataProxyList].debug("pasteChild targetDataProxyList 4");
	},

	loadFrom: { arg self, kit;
		var count;
		//self[\targetDataProxyList].debug("loadFrom targetDataProxyList 1");
		count = max(kit.lastChildIndex, self.lastChildIndex);
		//self[\targetDataProxyList].debug("loadFrom targetDataProxyList 2");
		//count.debug("loadFrom");
		count.do { arg idx;
			//self[\targetDataProxyList].debug("loadFrom targetDataProxyList 3");
			if(kit.childAt(idx).isEmpty) {
				self.clearChild(idx);
			} {
				self.pasteChild(idx, kit.childAt(idx));
			};
			//self[\targetDataProxyList].debug("loadFrom targetDataProxyList 4");
		};
	},

	///////

	getInstrument: { arg self, idx;
		Param.getInstrumentFromPbind(self.getTarget(idx)) ?? { 
			var proxy = self.getTargetDataProxy(idx);
			if(proxy.envir.notNil and: { proxy.envir.keys.includes(\instrument) }) {
				proxy.get(\instrument) ? \playersec
			} {
				\playersec
			}
		}
	},

	setInstrument: { arg self, idx, instr;
		if(Param.getInstrumentFromPbind(self.getTarget(idx)).isNil) {
			self.getTargetDataProxy(idx).set(\instrument, instr)
		} {
			Log(\Param).debug("Abort setting the instrument because target already have instrument");
		};
	},


	refCompileString: { arg self, name;
		"PatKitDef(%)".format(name ? self.key.asCompileString)
	},

	presetCompileString: { arg self, varname;
		var str;
		varname = varname ?? { self.refCompileString };
		str = "%.targetsData = [\n%];".format(
			varname ?? {self.refCompileString},
			{
				var datalist = self.targetsData.as(Array);

				// remove trailing empty children
				while { datalist.size > 0 and: { datalist.last == nil or: { self.childAt(datalist.size-1).isEmpty } } } { 
					datalist.pop 
				}; 

				datalist = datalist.collect({ arg data, dataidx;
					var res;
					if(self.childAt(dataidx).isEmpty.not) {
						data = data.keys.as(Array).collect { arg key, idx;
							"%: %".format(key.asCompileString, self.getTargetDataProxy(dataidx).getVal(key).asCompileString)
						}.join(", ");

						res = "\t(%),\n".format(data);
					} {
						res = "\t%,\n".format(nil.asCompileString);
					};
					res;
				});
				datalist = datalist.join;
				datalist;
			}.value
		);
		str;
	},

	savePresetCompileString: { arg self, path, action, force_dialog=false;
		PlayerWrapper.savePresetCompileStringHelper(path ? self.presetCompileStringSavePath, action, self.refCompileString, self.presetCompileString);
	},

	savePresetCompileStringDialog: { arg self, path, action, force_dialog=false;
		PlayerWrapper(self).savePresetCompileStringDialog(path ? self.presetCompileStringSavePath, action, force_dialog);
	},

	loadPresetCompileString: { arg self;
		if(self.presetCompileStringSavePath.notNil) {
			FileSystemProject.load(self.presetCompileStringSavePath);
		} {
			"ERROR: no presetCompileStringSavePath defined for %".format(self).postln;
		}
	},
));



ProtoTemplateDef(\PatKitDefChild, (
	parent: ProtoTemplateDef(\StreamPlayer),

	targetProxy: { arg self;
		self.parent_self.getTargetProxy(self.index)
	},

	targetData: { arg self;
		self.parent_self.getTargetData(self.index);
	},

	targetDataProxy: { arg self;
		self.parent_self.getTargetDataProxy(self.index);
	},

	pattern: { arg self;
		self.parent_self.getPattern(self.index);
	},

	presetCompileString: { arg self;
		self.targetData.asParamGroup.getEventCompileString
	},

	defaultTargetDataProxy: { arg self;
		self.parent_self.defaultTargetDataProxy;
	},

	defaultTargetProxy: { arg self;
		self.parent_self.defaultTargetProxy;
	},

	instrument: { arg self;
		self.parent_self.getInstrument(self.index)
	},

	instrument_: { arg self, instr;
		self.parent_self.setInstrument(self.index, instr)
	},

	isEmpty: { arg self;
		//Log(\Param).debug("IS EMPTY!!!!! %", self.index);
		(self.targetProxy.isNil or: { self.targetProxy.source.patternpairs == self.defaultTargetProxy.source.patternpairs }) and: {
			self.targetDataProxy.isNil or: {
				self.targetDataProxy.source.patternpairs == self.defaultTargetDataProxy.source.patternpairs
				and: { self.targetDataProxy.envir.size == 0 or: { 
					self.targetDataProxy.envir == self.defaultTargetDataProxy.envir 
				} }
			}
		}
	},

	asParamGroup: { arg self;
		self.targetDataProxy.asParamGroup(self.instrument)
	},
));

(
ProtoClassDef(\PchainProxy, (
	protoClass: \PchainProxy,
	new: { arg self, pattern;
		self = self.deepCopy;
	
		//pattern.debug("hoo");
		
		self[\proxy] = EventPatternProxy.new.source_(Pbind());
		self[\pattern] = EventPatternProxy.new.source_(Pbind());
		self.source = pattern;
	
		self;
	},

	proxy_: { arg self, val;
		self[\proxy] = val;
		self.pattern.source = self.proxy <> self.source;
	},

	source_: { arg self, val;
		self[\source] = val;
		self.instrument = BaseParam.getInstrumentFromPbind( self.source );
		self.proxy.set(\instrument, self.instrument);
		self.pattern.source = self.proxy <> self.source;
	},

	asPattern: { arg self;
		self.pattern;
	},

	asStream: { arg self;
		self.asPattern.asStream
	},

	embedInStream: { arg self, ev;
		self.asPattern.embedInStream(ev)
	},
	
	asParamGroup: { arg self, instrument;
		self.proxy.asParamGroup;
		//self.proxy.asParamGroup( instrument ?? { BaseParam.getInstrumentFromPbind( self.source ) } )
	},

	presetCompileString: { arg self;
		self.proxy.asParamGroup.getEventCompileString
		
	},
))
);

if(currentEnvironment.isKindOf(ProxySpace).not) {

	// backward compat
	~streamPlayer = ProtoTemplateDef(\StreamPlayer); 
	~patKitDef = ProtoClassDef(\PatKitDef);
	~seqPlayerGroup = ProtoTemplateDef(\SeqPlayerGroup);
	~parPlayerGroup = ProtoTemplateDef(\ParPlayerGroup);
	~notePlayer = ProtoTemplateDef(\NotePlayer);
}
