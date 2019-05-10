/////////// individuals

// produce and play an event from a pattern for each .play call 
// designed to be a child of some parent collection class where targets are stored
~streamPlayer = ProtoClass((
	new: { arg self, parent, x;
		self = self.deepCopy;
	
		
		self.parent_self = {parent};
		self.index = x;
	
		self;
	},

	target: { arg self;
		self.parent_self.targets[self.index];
	},

	target_: { arg self, val;
		self.parent_self.setTarget(self.index, val);
	},

	defaultTarget: { arg self;
		self.parent_self.defaultTarget
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
		if(self.target.notNil) {

			if(self.stream.isNil) {
				var pat = self.target.asPattern;
				if(self.preChain.notNil) {
					pat = self.target.asPattern <> self.preChain;
				};
				if(self.postChain.notNil) {
					pat = self.postChain <> self.target.asPattern;
				};
				self.stream = pat.asStream;
			};
			if(self.cur_ev.notNil) { // required for not overwriting player if already playing (too fast pushing)
				self.cur_ev.release;
				self.cur_ev = nil;
			};
			if(self.ev.isNil) {
				self.ev = Event.default;
			};
			self.ev.debug("play: before next: ev");
			self.ev = self.stream.next(self.ev);
			if(self.ev.notNil) {
				self.ev.debug("play: ev");
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
		self.raw_ev.debug("asPlayerEvent: raw_ev");
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
~notePlayer = ProtoClass((
	new: { arg self, parent, x, stream, pre_event, post_event;
		self = self.deepCopy;
	
		
		self.parent_self = {parent};
		self.stream = stream;
		self.post_event = post_event;
		self.pre_event = pre_event;
		self.index = x;
	
		self;
	},

	play: { arg self;

		// event generating
		if(self.cur_ev.notNil) { // required for not overwriting player if already playing (too fast pushing)
			self.cur_ev.release;
			self.cur_ev = nil;
		};
		if(self.ev.isNil) {
			self.ev = Event.default;
		};
		self.ev = self.ev.copy.putAll(self.pre_event);
		self.ev.debug("play: before next: ev");
		self.ev = self.stream.next(self.ev);
		self.ev.debug("play: ev");

		// processing
		self.ev.putAll(self.post_event);

		// recording
		self.raw_ev = self.ev.copy; // for recording, find better property name
		self.raw_ev[\kitIndex] = self.index;

		// playing
		self.play_ev = self.ev.copy;
		self.play_ev[\isRest] = false;
		self.play_ev[\sustain] = inf;
		self.play_ev[\latency] = Ref(0);
		self.cur_ev = self.play_ev.play;
		self.changed(\play);
		self.parent_self.changed(\play, self.index);
	},

	asPlayerEvent: { arg self; 
		// PlayerEvent == does not embed in stream
		// but asPlayerEvent is taken by PlayerWrapper
		self.raw_ev.debug("asPlayerEvent: raw_ev");
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
~parPlayerGroup = ProtoClass((
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

	targets_: { arg self, val;
		self[\targets] = val.as(SparseArray);
		self.children.do({ arg x; x.reboot });
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

	postChain_: { arg self, val;
		self[\postChain] = val;
		self.reboot;
	},

	makeListener: { arg self, fun;
		// FIXME: not the good way, need a omni controller
		var listenfun = { arg target, msg ... args;
			fun.(self.elAt(args[0]), msg, *args);
		};
		SimpleController.new(self).put(\play, listenfun).put(\stop, listenfun)
	},

	newChild: { arg parent_self, x;
	//	PlayerWrapper(
			~streamPlayer.new(parent_self, x)
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
~seqPlayerGroup = ProtoClass((
	new: { arg self, pat;
		self = self.deepCopy;
	
		self.init(pat);
	
		self;
	},

	init: { arg self, pat;
		self.target = pat;
		self.children = SparseArray.new;
	},

	makeListener: { arg self, fun;
		var listenfun = { arg target ... args;
			fun.(self.elAt(args[0]), *args);
		};
		SimpleController.new(self).put(\play, listenfun).put(\stop, listenfun)
	},

	stream: { arg self;
		if(self[\prStream].isNil) {
			self[\prStream] = self.target.asPattern.asStream;
		};
		self[\prStream]
	},

	newChild: { arg parent_self, x;
		var post_event = ();
	//	PlayerWrapper(
			~notePlayer.new(parent_self, x, parent_self.stream, (midinote:x), post_event);
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

	stop: { arg self;
		self.children.do(_.stop)
	},

));



// specialisation of parPlayerGroup for PatKitDef: compile string and Def design
~patKitDef = ProtoClass((
	protoClass: \PatKitDef,
	parent: ~parPlayerGroup,
	all: { PresetDictionary.new(\patKitDef) },
	new: { arg self, key, instrs;

		if(self.all[key].isNil) {
			self = self.deepCopy;
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

	clear: { arg self;
		self.all[self.key] = nil;
		nil;
	},

	refCompileString: { arg self, name;
		"PatKitDef(%)".format(name ? self.key.asCompileString)
	},

	presetCompileString: { arg self, varname="~ppg";
		self.parent[\presetCompileString].(self, self.refCompileString)
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

////////// recorders

// can record seqPlayerGroup and parPlayerGroup
~playerGroupRecorder = ProtoClass((
	new: { arg self, group;
		self = self.deepCopy;
	
		self.init(group);
	
		self;
	},

	init: { arg self, target;
		self.target = target;
		self.tempoClock = TempoClock.default;
	},

	eventLoop: { arg self;
		if(self[\priv_eventLoop].isNil) {
			self[\priv_eventLoop] = XEventLoop.newInstance; // will be overwritten by timeline eventloop
		};
		self[\priv_eventLoop];
	},

	eventLoop_: { arg self, el;
		self[\priv_eventLoop] = el;
	},

	eventList: { arg self;
		self.eventLoop.list
	},

	eventList_: { arg self, val;
		self.eventLoop.list = val;
	},

	startRecording: { arg self;
		self.controller.remove;
		self.controller = self.target.makeListener({ arg player, status;
			Log(\Param).debug("listener: status: %", status);
			if(status == \play) {
				player.recordedEvent = self.eventLoop.recordEvent(player.asPlayerEvent);
				player.recordedEvent[\clockTime] = self.tempoClock.beats;
				Log(\Param).debug("listener: start: recordedEvent:%", player.recordedEvent);
			} {
				if(status == \stop and: {player.recordedEvent.notNil}) {
					player.recordedEvent[\sustain] = self.tempoClock.beats - player.recordedEvent[\clockTime];
					Log(\Param).debug("listener: stop: recordedEvent:%", player.recordedEvent);
					player.recordedEvent.changed(\refresh);
				}
			}
		});
		self.eventLoop.startRec(false, self.quant ? Pdef.defaultQuant);
	},

	isRecording: { arg self;
		self.eventLoop.isRecording;
	},

	isRecording_: { arg self, val;
		if(val == true) {
			self.startRecording;
		} {
			self.stopRecording;
		}
	},

	stopRecording: { arg self;
		self.controller.remove;
		self.eventLoop.stopRec;
	},

	presetCompileStringPbind: { arg self;
		var ret = "";
		var lists = (
		);

		// envent list to Pbind containing Pseq's
		// TODO: need to strip event list from all specials event (like locations) because no way to represent them within Pseq
		// 		but it's a good thing to have them, maybe includes them in a separate Pbind wihtin Ppar

		//self.eventLoop.list.do ({ arg ev;
		//	if(ev.type != \start or: { ev.type != \end }) {
		//	};
		//	ev.keys.do { arg key;
		//		if(ev[key].isNil and: { [\start, \end, \rest].includes(ev[\type]).not and: { ev[\isRest] != false } }) {
		//			ev[key] = \none;
		//		};
		//		if(lists[key] != \none) {
		//			lists[key] = lists[key].add(ev[key]);
		//		};
		//	};
		//});

		self.eventLoop.list.do ({ arg ev;
			ev.debug("recorder: process ev");
			if(ev.type == \start or: { ev.type == \end }) {
			} {
				[\kitIndex, \midinote, \dur, \sustain].do { arg key;
					lists[key] = lists[key].add(ev[key] ? 1);
				}
			};
		});

		lists.debug("recorder: getPresetCompileString: lists");

		ret = lists.keys.as(Array).collect({ arg key;
			if(lists[key] != \none) {
				"\t%, Pseq(%),\n".format(key.asCompileString, lists[key].asCompileString)
			} { 
				""
			};
		}).join;

		ret = "Pbind(\n%)".format(ret);

		if(self.eventLoop.list[0].dur != 0) {
			ret = "Pseq([ Event.silent(%), % ])".format(self.eventLoop.list[0].dur, ret)
		};
		ret = "XEventList.newFrom(%);".format(ret);
		ret;
	},

	presetCompileString: { arg self;
		var ret = "";
		ret = self.eventList.presetCompileString;
		ret;
	},
));

// can record Param
~paramRecorder = ProtoClass((
	parent: ~playerGroupRecorder,

	new: { arg self, param;
		self = self.deepCopy;
	
		self.init(param);
	
		self;
	},

	init: { arg self, target;
		self.target = target;
		self.tempoClock = TempoClock.default;
	},

	startRecording: { arg self;
		if(self.eventLoop.notNil) {
			if(self.controller.notNil) {
				self.controller.remove;
			};
			self.controller = self.target.makeListener({ arg obj, param;
				var ev = (level:param.get);
				Log(\Param).debug("LEVEL EV RECORDING !! %", ev);
				self.eventLoop.recordEvent(ev);
			});
			self.eventLoop.startRec(false, 0);
			Log(\Param).info("recording started");
			self.changed(\isRecording);
		}
	},

	stopRecording: { arg self;
		if(self.eventLoop.notNil) {
			if(self.controller.notNil) {
				self.controller.remove;
			};
			self.eventLoop.stopRec;
			Log(\Param).info("recording stopped");
			self.changed(\isRecording);
		}
	},

));
