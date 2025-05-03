
///////////////////////////// PlayerWrapper


PlayerWrapper  {
	classvar <>capturePlayerHook;

	var <>wrapper;
	var >label;
	var init_args;
	var <>recordedEvent; // playerGroupRecorder need this placeholder to store current event being recorded
	var <>playerEventWrapper;
	var <>recorderMode = false; // in recorderMode, .play call .isRecording_(true)
	

	*new { arg target;
		^super.new.initWrapper(target);
	}

	initWrapper { arg target;
		// FIXME: handle when not a kind of wrapper in list, and handle GUI when wrapper is nil
		init_args = [target];
		wrapper = case 
			{ target.isNil } {
				//"WARNING: PlayerWrapper: target is nil".debug;
				PlayerWrapper_Nil(target, this)
				//^nil;
			}
			{ target.isKindOf(PlayerWrapper) } {
				^target
			}
			{ target.isKindOf(PlayerWrapperGroup) } {
				PlayerWrapper_PlayerWrapperGroup(target, this)
			}
			{ target.isKindOf(ProtoClass) } {
				PlayerWrapper_ProtoClass(target, this)
			}
			{ target.isKindOf(StepEvent) } {
				PlayerWrapper_StepEvent(target, this)
			}
			{ target.isKindOf(Event) } {
				PlayerWrapper_Event(target, this)
			}
			{ target.isKindOf(NodeProxy) } {
				PlayerWrapper_NodeProxy(target, this)
			}
			{ target.isKindOf(EventPatternProxy) } {
				PlayerWrapper_EventPatternProxy(target, this)
			}
			{ target.isKindOf(TaskProxy) } {
				PlayerWrapper_TaskProxy(target, this)
			}
			{ target.isKindOf(Fdef) } {
				PlayerWrapper_Fdef(target, this)
			}
			{ target.isKindOf(Node) } {
				PlayerWrapper_Node(target, this)
			}
			{ target.isKindOf(Builder) } {
				PlayerWrapper_Builder(target, this)
			}
			{ target.isKindOf(Param) } {
				PlayerWrapper_Param.new(target, this)
			}
			{ target.isKindOf(Bus) } {
				PlayerWrapper_Bus(target, this)
			}
			{
				// assume target respond to wrapper interface
				// FIXME: errors are hard to find, deprecate this feature
				Log(\Param).error("ERROR: PlayerWrapper target class not recognized: %", target.class);
				PlayerWrapper_Nil(nil, this)
			}
		;
		
	}

	*capturePlayer { arg player;
		^capturePlayerHook.(player)
	}

	*doWithQuant { arg quant, fun, clock;
		if(quant.isNil) {
			fun.()
		} {
			// should return nil, else if fun return 0 it create an infinite loop
			clock = clock ?? { TempoClock.default };
			clock.schedAbs(clock.nextTimeOnGrid(quant), { fun.value; nil })
		}
	}

	mapPlayer { arg  val;
		this.target = val;
	}

	target {
		if(wrapper.notNil) {
			^wrapper.target
		} {
			^nil
		};
	}

	target_ { arg target;
		this.initWrapper(target);
		this.changed(\player);
	}

	///////// API
	// play
	// stop
	// isPlaying
	// label and key
	// quant

	// *could be added
	// pause
	// record

	///////////////

    doesNotUnderstand { arg selector...args;
		//Log(\Param).debug("PlayerWrapper: doesNotUnderstand %",[selector, args]);
        if(wrapper.class.findRespondingMethodFor(selector).notNil) {
			//Log(\Param).debug("PlayerWrapper: perform on wrapper");
			^wrapper.perform(selector, * args);
		} {
			if(wrapper.target.class.findRespondingMethodFor(selector).notNil) {
				//Log(\Param).debug("PlayerWrapper: perform on target %", selector);
				^wrapper.target.perform(selector, * args);
			} {
				Log(\Param).error("PlayerWrapper: class, wrapper and target does not understand %, %, %"
					.format(selector, wrapper.class, wrapper.target)
				);
				DoesNotUnderstandError.throw;
			}
		};
	}

	/////////////// overRide object methods

	isPlaying {
		^wrapper.isPlaying
	}

	isPlaying_ { arg val;
		if(val == true) {
			wrapper.play
		} {
			wrapper.stop
		}
	}

	isActive {
		// used by PlayerWrapperGridCellView
		^wrapper.isActive
	}

	isActive_ { arg val;
		wrapper.isActive = val;
	}

	stop {
		wrapper.stop;
	}

	play {
		wrapper.play;
	}

	stopNow {
		wrapper.stopNow;
	}

	playNow {
		wrapper.playNow;
	}

	label {
		if(label.notNil) {
			^label;
		} {
			^wrapper.label;
		}
	}

	isEmpty {
		^wrapper.isEmpty
	}

	== { arg playerwrapper;
		^playerwrapper.notNil and: {this.target == playerwrapper.target}
	}

	///////////// gui

	edit {
		^wrapper.edit;
		//^WindowLayout({ PlayerWrapperView.new(this).layout });
	}

	asView {
		if(this.recorderMode != true) {
			^PlayerWrapperView.new(this).view;
		} {
			^RecordButton.new(this.target, nil, this.label).view;
		}
	}

	asPlayerEvent {
		^wrapper.asPlayerEvent;
	}

	makeListener { arg fun;
		var controller;
		var listenfun = { arg target ... args;
			fun.(this, *args);
		};
		//this.target.debug("makeListener");
		controller = SimpleController(this.target)
			.put(\play, listenfun)
			.put(\stop, listenfun)
			.put(\userPlayed, listenfun)
			.put(\userStopped, listenfun)
			.put(\playing, listenfun)
			.put(\stopped, listenfun)
			.put(\PlayerWrapper, listenfun)
		;
		^controller
	}
	//TODO: asPatternEvent which detect if it's a pattern

	printOn { arg stream;
		this.storeOn(stream); // storeOn call storeArgs
	}

	storeArgs { arg stream;
		^init_args
		//stream << ("Param.new(" ++ init_args.asCompileString ++ ")");
	}

	*savePresetCompileStringHelper { arg ...args;
		PlayerWrapper_Base.savePresetCompileStringHelper(*args);
	}
	
}

PlayerWrapper_Base {
	var <>target;
	var <>parent;
	*new { arg target, parent;
		
		^super.new.parent_(parent).init(target);

	}

	init { arg xtarget;
		target = xtarget
	}

	key { 
		^this.label.asSymbol;
	}

	asPlayerEvent {
		var fun = this.playerEventWrapper ? { arg x; x };
		^fun.(PlayerEvent((
			receiver: "{ % }".format(this.asCompileString).interpret
		)))
	}

    doesNotUnderstand { arg selector...args;
		//Log(\Param).debug("PlayerWrapper_Base: doesNotUnderstand %", [selector, args]);
		if(target.class.findRespondingMethodFor(selector).notNil) {
			//Log(\Param).debug("perform on target");
			^target.perform(selector, * args);
		} {
			Log(\Param).error("PlayerWrapper_Base: wrapper and target doesn't respond to %", selector);
			DoesNotUnderstandError.throw;
		}
	}

	/////////////// overRide object methods

	isPlaying {
		^target.isPlaying;
	}

	isActive {
		^false
	}

	isActive_ {
		// NOOP
	}

	stop {
		target.changed(\PlayerWrapper, \userStopped);
		this.doWithQuant {
			target.stop; // in Pdef, stop is not quantied but play yes
			target.changed(\PlayerWrapper, \stopped);
		};
	}

	play {
		target.play;
		target.changed(\PlayerWrapper, \userPlayed);
		this.doWithQuant {
			target.changed(\PlayerWrapper, \playing);
		};
	}

	stopNow {
		target.stop;
		//target.changed(\PlayerWrapper, \userStopped);
		target.changed(\PlayerWrapper, \stopped);
	}

	playNow {
		target.play;
		target.changed(\PlayerWrapper, \playing);
	}

	togglePlay {
		if(this.isPlaying) {
			this.stop;
		} {
			this.play;
		}
	}

	outBus_ { arg val;
		if(this.target.notNil) {
			Param(this.target, \out, ParamBusSpec()).set(val);
		}
	}

	outBus { arg val;
		if(this.target.notNil) {
			^Param(this.target, \out, ParamBusSpec()).get;
		} {
			^nil
		};
	}

	quant {
		^if(this.target.respondsTo(\quant)) {
			this.target.quant;
		}
	}

	quant_ { arg val;
		if(this.target.respondsTo(\quant)) {
			this.target.quant = val;
		}
	}

	doWithQuant { arg fun;
		PlayerWrapper.doWithQuant(this.quant, fun, this.clock);
	}

	clock {
		^this.target.tryPerform(\clock) ?? { TempoClock.default };
	}

	targetClass {
		^this.target.class.asSymbol;
	}

	//savePresetCompileString { arg ...args;
	//	"ERROR: %.savePresetCompileString: not implemented for %".format(this, this.targetClass).postln;
	//}

	//loadPresetCompileString { arg ...args;
	//	"ERROR: %.loadPresetCompileString: not implemented for %".format(this, this.targetClass).postln;
	//}

	*savePresetCompileStringHelper { arg mypath, onDoneAction, refCompileString, presetCompileString, writePath=true;
		if(mypath.notNil) {
			var myfolderpath = PathName(mypath).pathOnly;
			var myfolderpathname;
			myfolderpathname = FileSystemProject.resolve(myfolderpath);
			if(myfolderpathname.notNil) {
				mypath = myfolderpathname.fullPath +/+ PathName(mypath).fileName;
				"Trying to write preset to file %".format(mypath.asCompileString).postln;
				File.use(mypath, "w", { arg file;
					var relpath = FileSystemProject.unresolve(mypath);
					var preset;
					//refCompileString.interpret.presetCompileStringSavePath = relpath; // commented because moved higher in call chain
					if(writePath==true) {
						file.write("%.presetCompileStringSavePath = %;\n\n".format(refCompileString, relpath.asCompileString));
					};

					preset = presetCompileString;
					if(preset.isNil) {
						"ERROR: PlayerWrapper.savePresetCompileStringHelper: no preset found for this object".postln;
					} {
						file.write(preset);
					};
				});
				onDoneAction.()
			} {
				"ERROR: PlayerWrapper.savePresetCompileStringHelper: Can't resolve file %".format(mypath).postln;
			};
		} {
			"ERROR: PlayerWrapper.savePresetCompileStringHelper: no path to save to".postln;
		}
	}

	savePresetCompileString { arg path, onDoneAction;
		this.class.savePresetCompileStringHelper(path ? this.presetCompileStringSavePath, onDoneAction, this.asCompileString, this.presetCompileString)
	}

	asCompileString {
		^this.parent.asCompileString;
	}

	loadPresetCompileString { arg ...args;
		//"loadPresetCompileString: TODO".debug;
		//NotYetImplementedError.throw
		//"ERROR: %.loadPresetCompileString: not implemented for %".format(this, this.targetClass).error;
		if(this.presetCompileStringSavePath.notNil) {
			FileSystemProject.load(this.presetCompileStringSavePath);
		} {
			"ERROR: no presetCompileStringSavePath defined for %".format(this).postln;
		}
	}

	savePresetCompileStringDialog { arg path, action, force_dialog=false, dialogKey;
		dialogKey = dialogKey ? \filedialog_save;
		if(path.notNil) {
			this.savePresetCompileString(path, action);
			this.presetCompileStringSavePath = path;
		} {
			if(this.presetCompileStringSavePath.notNil and: { force_dialog==false }) {
				this.savePresetCompileString(this.presetCompileStringSavePath, action);
			} {
				if(WindowDef(dialogKey).notNil) {
					WindowDef(dialogKey).front(nil, { arg mypath, name;
						[mypath, name].debug("filedialog_save ok callback");
						this.savePresetCompileString(mypath +/+ name, action);
						this.presetCompileStringSavePath = mypath +/+ name;
					})
				} {
					Dialog.savePanel({ arg mypath;
						//mypath.debug("save panel: path");
						this.savePresetCompileString(mypath, action);
						this.presetCompileStringSavePath = mypath;
					},{
						//"cancelled".postln;
					});
				};
			}

		};
	}

	loadPresetCompileStringDialog { arg path, action, force_dialog=false;
		// WIP
		if(path.notNil) {
			this.loadPresetCompileString(path, action);
			this.presetCompileStringSavePath = path;
		} {
			if(this.presetCompileStringSavePath.notNil and: { force_dialog==false }) {
				this.loadPresetCompileString(this.presetCompileStringSavePath, action);
			} {
				if(WindowDef(\filedialog).notNil) {
					WindowDef(\filedialog).front(nil, { arg mypath, name;
						[mypath, name].debug("filedialog open ok callback");
						this.presetCompileStringSavePath = mypath +/+ name;
						this.loadPresetCompileString(mypath +/+ name, action);
					})
				} {
					Dialog.savePanel({ arg mypath;
						//mypath.debug("save panel: path");
						this.presetCompileStringSavePath = mypath;
						this.loadPresetCompileString(mypath, action);
					},{
						//"cancelled".postln;
					});
				};
			}

		};
	}

	presetCompileStringSavePath {
		^this.target.getHalo(\presetCompileStringSavePath);
	}

	presetCompileStringSavePath_ { arg path;
		this.target.addHalo(\presetCompileStringSavePath, path)
	}

	isEmpty { 
		^this.target.isNil
	}

	edit {
		^WindowLayout({ PlayerWrapperView.new(this).layout });
	}
}

PlayerWrapper_Param : PlayerWrapper_Base {
	play {
		target.normSet(1)
	}

	label {
		var res;
		res = target.property;
		if(res.isKindOf(Function)) {
			^""
		} {
			^res
		}
	}

	stop {
		target.normSet(0)
	}

	isPlaying {
		^target.normGet == 1
	}
}

PlayerWrapper_NodeProxy : PlayerWrapper_Base {
	label {
		^target.key ?? { "NodeProxy<%>".format(target.index) }
	}

	outBus_ { arg val;
		if(val.isKindOf(Bus)) {
			val = val.index;
		};
		if(val.isNil) {
			// prevent assigning nil because it raise exception
			this.target.initMonitor.clear;
		} {
			this.target.initMonitor.out = val;
		}
	}

	outBus { arg val;
		if(this.target.rate == \control) {
			^this.target.bus;
		} {
			^this.target.initMonitor.out ? 0;
		};
	}

	play {
		// hack: Ndef now have same latency than Pdef
		//{ // defer implemented in dereference_event
		if(target.rate == \control) {
			target.wakeUp; 
		} {
			target.play;
		};
		target.changed(\PlayerWrapper, \userPlayed);
		this.doWithQuant {
			target.changed(\PlayerWrapper, \playing);
		};
		//}.defer(Server.default.latency)
	}

	stop {
		// hack: Ndef now have same latency than Pdef
		//{ // defer implemented in dereference_event
		this.doWithQuant {
			if(target.rate == \control) {
				target.free;
			} {
				if(target.getHalo(\stopIsMute) != false) {
					target.stop(target.fadeTime); // FIXME: how to configure ?
				} {
					target.free;
				}
			};
			target.changed(\PlayerWrapper, \stopped);
		};
		target.changed(\PlayerWrapper, \userStopped);
		//}.defer(Server.default.latency)
	}

	playNow {
		var bundle = MixedBundle.new;
		if(target.homeServer.serverRunning.not) {
			("server not running:" + this.homeServer).warn;
			^this
		};
		if(target.bus.rate == \control) { "Can't monitor a control rate bus.".warn; target.monitor.stop; ^this };
		target.playToBundle(bundle, nil, nil, nil, false, nil, 0, nil);
		// homeServer: multi client support: monitor only locally
		bundle.schedSend(target.homeServer, target.clock ? TempoClock.default, 0);
		target.changed(\play);
		target.send;
		target.changed(\PlayerWrapper, \playing);
	}

	stopNow {
		if(target.getHalo(\stopIsMute) != false) {
			target.stop(target.fadeTime); // FIXME: how to configure ?
		} {
			{
				//Server.default.latency.wait;
				//target.free;
				target.end;
			}.fork;
		};
		target.changed(\PlayerWrapper, \stopped);
	}

	isPlaying {
		if(target.rate == \control) {
			^target.isPlaying
		} {
			^target.monitor.isPlaying;
		}
	}

	presetCompileString {
		var ret;
		if(this.key.isKindOf(Symbol)) {
			if(this.target.getHalo(\ParamGroup).isNil) {
				this.target.addHalo(\ParamGroup, this.target.asParamGroup)
			};
			ret = "%.addHalo(\\ParamGroup, \n%\n);\n".format(this.target.asCompileString, this.target.getHalo(\ParamGroup).presetCompileString);
			^ret;
		} {
			Log(\Param).error("ERROR: can't save a NodeProxy preset");
			^nil;
		}
	}

	isEmpty { 
		^this.target.source.isNil
	}

	edit {
		ParamProto.init;
		if(this.target.isKindOf(Ndef)) {
			^WindowDef(\NdefEditor).front(this.target);
		} {
			^WindowDef(\NodeProxyEditor).front(this.target);
		}
	}

}

PlayerWrapper_EventPatternProxy : PlayerWrapper_Base {
	playNow {
		target.play(quant:0);
	}

	label {
		if(target.isKindOf(Pdef)) {
			^target.key
		} {
			^target.getHalo(\label) ? ""
		}
	}

	presetCompileString {
		var ret;
		if(this.key.isKindOf(Symbol)) {
			^this.asParamGroup.getPbindefCompileString;
		} {
			Log(\Param).error("ERROR: can't save a EventPlayerWrapper");
			^nil;
		}
	}

	isEmpty { 
		^this.target.source.isNil
	}

	edit {
		^WindowDef(\PdefEditor).front(this.target);
	}
}

PlayerWrapper_TaskProxy : PlayerWrapper_Base {
	playNow {
		target.play(quant:0);
	}

	label {
		if(target.isKindOf(Tdef)) {
			^target.key
		} {
			^target.getHalo(\label) ? ""
		}
	}

	isEmpty { 
		^this.target.source.isNil
	}

	edit {
		^WindowDef(\TdefEditor).front(this.target);
	}
}

PlayerWrapper_Fdef : PlayerWrapper_Base {
	playNow {
		target.value;
	}

	play {
		^target.value;
	}

	stop {
		// NOOP
	}

	isPlaying { ^false }

	label {
		^currentEnvironment.findKeyForValue(target) ?? { 
			Fdef.all.findKeyForValue(target) ?? {
				target.getHalo(\label) ?? {""}
			}
		}
	}

	isEmpty { 
		^this.target.source.isNil
	}

	edit {
		^WindowDef(\TdefEditor).front(this.target);
	}
}

EventPlayerWrapper : PlayerWrapper_Event { } // compat, to be deleted

PlayerWrapper_Event : PlayerWrapper_Base {
	// allow an event to act as a PlayerWrapper

	play {
		target.eventPlay;
	}

	label {
		^target.label ?? {target.key ?? { "-" }}
	}

	isPlaying {
		^target.eventIsPlaying
	}

	stop {
		target.eventStop;
	}

}

PlayerWrapper_ProtoClass : PlayerWrapper_Base {
	// allow a protoclass to act as a PlayerWrapper

	outBus_ { arg val;
		Param(Message(this.target), \outBus, ParamBusSpec()).set(val);
	}

	outBus { arg val;
		^Param(Message(this.target), \outBus, ParamBusSpec()).get;
	}

	asPlayerEvent {
		if(target[\asPlayerEvent].isNil) {
			var fun = this.playerEventWrapper ? { arg x; x };
			^fun.(PlayerEvent((
				receiver: "{ % }".format(this.asCompileString).interpret
			)))
		} {
			^target.asPlayerEvent
		}
	}

	play {
		if(parent.recorderMode != true) {
			target.play;
			target.changed(\PlayerWrapper, \userPlayed);
			this.doWithQuant {
				target.changed(\PlayerWrapper, \playing);
			}
		} {
			target.isRecording_(true)
		}
	}

	playNow {
		target.playNow;
		target.changed(\PlayerWrapper, \playing);
	}

	stopNow {
		target.stopNow;
		target.changed(\PlayerWrapper, \stopped);
	}

	label {
		^target.label ?? { target.key ?? { "-" } };
	}

	isPlaying {
		if(parent.recorderMode != true) {
			^target.isPlaying
		} {
			^target.isRecording
		}
	}

	isActive {
		^target.isActive
	}

	isActive_ { arg val;
		target.isActive = val;
	}

	quant {
		^this.target.quant;
	}

	quant_ { arg val;
		this.target.quant = val;
	}

	stop {
		if(parent.recorderMode != true) {
			// if protoclass already have a quant, the wrapper sould not add it again
			// the protoclass is responsible to add a quant
			target.stop;
			target.changed(\PlayerWrapper, \userStopped);
			this.doWithQuant {
				target.changed(\PlayerWrapper, \stopped);
			}
		} {
			target.isRecording_(false);
		}
	}

	// generic way to classify players : ("%_%".format(player.targetClass, player.key))
	targetClass {
		var targetclass;
		var player;
		try {
			targetclass = this.target.class.asSymbol;
			player = this.target;

			if(targetclass == \ProtoClass) {
				if( player.protoClass.notNil ) {
					targetclass = player.protoClass;
				} {
					if(player.all.notNil) {
						targetclass = player.all.key;
					}
				}
			};
		}
		^targetclass;
	}

	savePresetCompileString { arg ...args;
		if(this.target[\savePresetCompileString].notNil) {
			^this.target.savePresetCompileString(*args)
		} {
			var path = args[0];
			var onDoneAction = args[1];
			//[path, this.presetCompileStringSavePath, args].debug("PlayerWrapper_ProtoClass.savePresetCompileString");
			this.class.savePresetCompileStringHelper(path ? this.presetCompileStringSavePath, onDoneAction, this.asCompileString, this.target.presetCompileString)
		}
	}

	loadPresetCompileString { arg ...args;
		if(this.target[\loadPresetCompileString].notNil) {
			^this.target.loadPresetCompileString(*args);
		} {
			if(this.presetCompileStringSavePath.notNil) {
				FileSystemProject.load(this.presetCompileStringSavePath);
			} {
				"ERROR: no presetCompileStringSavePath defined for %".format(this).postln;
			}
		};
	}

	presetCompileStringSavePath {
		^this.target.presetCompileStringSavePath
	}

	presetCompileStringSavePath_ { arg path;
		^this.target.presetCompileStringSavePath = path
	}

	isEmpty { 
		^this.target.isEmpty
	}

	edit {
		^this.target.edit;
	}
}

PlayerWrapper_StepEvent : PlayerWrapper_ProtoClass {
	outBus { 
		^this.out
	}

	outBus_ { arg bus;
		this.out = bus;
	}

}

PlayerWrapper_Nil : PlayerWrapper_Base {

	play {
		
	}

	label {
		^"-"
	}

	isPlaying {
		^false
	}

	stop {
	}

	isEmpty { 
		^true
	}

}

PlayerWrapper_PlayerWrapperGroup : PlayerWrapper_Base {
	
}

PlayerWrapper_Builder : PlayerWrapper_Base {

	outBus_ { arg val;
		Param(this.target, \outBus, ParamBusSpec()).set(val);
		^this.parent
	}

	outBus { arg val;
		^Param(this.target, \outBus, ParamBusSpec()).get;
	}

	
}

PlayerWrapper_Node : PlayerWrapper_Base {

	play {
		// NOOP
	}

	stop {
		this.target.release;
	}

	label {
		^"S" + this.target.nodeID.asString
	}

	outBus_ { arg val;
		//Log(\Param).debug("WTF %", this.target);
		Param(this.target, \outBus, ParamBusSpec()).set(val);
		^this.parent
	}

	outBus { arg val;
		^Param(this.target, \outBus, ParamBusSpec()).get;
	}

	
}

////////////////////

PlayerWrapper_Bus : PlayerWrapper_Base {

	label {
		^target.asCompileString
	}

	play {
		// TODO: monitor if audio
	}

	stop {
		// stop monitor
	}

	outBus {
		^target
	}

	edit {
		// TODO: make gui:
		// - bus properties
		// - slider to write to control bus
		// - button to monitor audio bus
		// - scope
		// - level
		// - record
		WindowDef(\ScopeView).front(target)
	}

}

///////////////////////////////////////:

PlayerWrapperGroup : List {
	var <>mode;
	var <>label;
	var <>controllerList;
	var <>playerEventWrapper;

	// TODO: integrate with PlayerTracker

	*new { arg anArray;
		var inst;
		inst = super.new.setCollection( anArray.collect({ arg item, idx;
			if(item.isKindOf(PlayerWrapper)) {
				item;
			} {
				PlayerWrapper(item)
			}
		}) );
		inst.initPlayerWrapperGroup;
		^inst;
	}

	initPlayerWrapperGroup {
		mode = \any;
		label = this.collect(_.label).inject("", { arg a, b; a.asString + b.asString });
		this.do({ arg wrapper, idx; 
			var eventWrapper = { arg ev;
				if(this.playerEventWrapper.notNil) {
					this.playerEventWrapper.(ev, idx);
				} {
					ev[\midinote] = idx;
					ev;
				};
			};
			wrapper.playerEventWrapper_(eventWrapper)
		});
	}

	makeListener { arg fun;
		if(controllerList.notNil) {
			controllerList.remove;
		};
		controllerList = ProtoClass((
			list: List.new, 
			remove: { arg self;
				self.list.do(_.remove)
			})
		);
		this.do { arg child;
			var controller = child.makeListener(fun);
			controllerList.list.add(controller);
		};
		^controllerList;
	}

	quant {
		^this.first.quant;
	}

	quant_ { arg val;
		this.do({ arg x; x.quant = val });
	}

	doWithQuant { arg fun;
		PlayerWrapper.doWithQuant(this.quant, fun)
	}

	play { 
		this.collect(_.play);
	}

	stop {
		this.collect(_.stop);
	}

	playNow {
		this.collect(_.playNow);
	}

	stopNow {
		this.collect(_.stopNow);
	}

	isPlaying {
		if(mode == \any) {
			^this.any({ arg x;
				x.isPlaying ? false;
			});
		} {
			^this.every({ arg x; x.isPlaying ? true });
		}
	}
	
	isPlaying_ { arg val;
		if(val == true) {
			this.do(_.play);
		} {
			this.do(_.stop);
		}
	}

	asView {
		^this.editor.asView;
	}

	editor {
		^WindowDef("%_%".format(\PlayerWrapperGroup, this.identityHash).asSymbol, { arg def;
			VLayout (
				PlayerWrapperView(this).asView,
				StaticText.new,
				VLayout(
					*this.collect({ arg player;
						var view = player.asView.rightClickEditorEnabled_(true);
						//var follower = { arg ...args;
				
						//};
						//player.target.addDependant(follower);
						//view.onClose({ plaer.target.removeDependant(follower) });

						//view.button.followChange(player.target, \PlayerWrapper, { arg but, pw, changed, status;
							////if(a)
				
						//});
						view;
					}) ++ [nil]
				)
			)
		});
	}

	edit { 
		this.editor.front;
	}
}

