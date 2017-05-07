
///////////////////////////// PlayerWrapper


PlayerWrapper  {
	var <>wrapper;
	var >label;
	var init_args;

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
			{ target.isKindOf(ProtoClass) } {
				PlayerWrapper_ProtoClass(target, this)
			}
			{ target.isKindOf(StepEvent) } {
				PlayerWrapper_ProtoClass(target, this)
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
			{ target.isKindOf(Param) } {
				PlayerWrapper_Param.new(target, this)
			}
			{
				// assume target respond to wrapper interface
				target
			}
		;
		
	}

	mapPlayer { arg  val;
		this.initWrapper(val);
		this.changed(\player);
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
		[selector, args].debug("PlayerWrapper: doesNotUnderstand");
        if(wrapper.class.findRespondingMethodFor(selector).notNil) {
			"PlayerWrapper: perform on wrapper".debug;
			^wrapper.perform(selector, * args);
		} {
			if(wrapper.target.class.findRespondingMethodFor(selector).notNil) {
				"PlayerWrapper: perform on target".debug;
				^wrapper.target.perform(selector, * args);
			} {
				"PlayerWrapper: class, wrapper and target does not understand %".format(selector).debug;
				DoesNotUnderstandError.throw;
			}
		};
	}

	/////////////// overRide object methods

	isPlaying {
		^wrapper.isPlaying
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

	///////////// gui

	edit {
		^WindowLayout({ PlayerWrapperView.new(this).layout });
	}

	asView {
		^PlayerWrapperView.new(this).layout;
	}

	asPlayerEvent {
		^PlayerEvent((
			receiver: {this}
		))
	}

	makeListener { arg fun;
		var controller;
		this.target.debug("makeListener");
		controller = SimpleController(this.target)
			.put(\play, fun)
			.put(\stop, fun)
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


    doesNotUnderstand { arg selector...args;
		[selector, args].debug("PlayerWrapper_Base: doesNotUnderstand");
		if(target.class.findRespondingMethodFor(selector).notNil) {
			"perform on target".debug;
			^target.perform(selector, * args);
		} {
			"PlayerWrapper_Base: wrapper and target doesn't respond to %".format(selector).debug;
			DoesNotUnderstandError.throw;
		}
	}

	/////////////// overRide object methods

	isPlaying {
		^target.isPlaying;
	}

	stop {
		this.doWithQuant {
			target.stop;
		}
	}

	play {
		target.play;
	}

	stopNow {
		target.stop
	}

	playNow {
		target.play
	}

	togglePlay {
		if(this.isPlaying) {
			this.stop;
		} {
			this.play;
		}
	}

	outBus_ { arg val;
		Param(this.target, \out, XBusSpec()).set(val);
	}

	outBus { arg val;
		^Param(this.target, \out, XBusSpec()).get;
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
		if(this.quant.isNil) {
			fun.()
		} {
			this.clock.schedAbs(this.clock.nextTimeOnGrid(this.quant), fun)
		}
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

	*savePresetCompileStringHelper { arg mypath, onDoneAction, refCompileString, presetCompileString;
		if(mypath.notNil) {
			var myfolderpath = PathName(mypath).pathOnly;
			var myfolderpathname;
			myfolderpathname = FileSystemProject.resolve(myfolderpath);
			if(myfolderpathname.notNil) {
				mypath = myfolderpathname.fullPath ++ PathName(mypath).fileName;
				"Trying to write preset to file %".format(mypath.asCompileString).postln;
				File.use(mypath, "w", { arg file;
					var relpath = FileSystemProject.unresolve(mypath);
					var preset;
					//refCompileString.interpret.presetCompileStringSavePath = relpath; // commented because moved higher in call chain
					file.write("%.presetCompileStringSavePath = %;\n\n".format(refCompileString, relpath.asCompileString));

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
		"loadPresetCompileString: TODO".debug;
		//"ERROR: %.loadPresetCompileString: not implemented for %".format(this, this.targetClass).postln;
	}

	savePresetCompileStringDialog { arg path, action, force_dialog=false;
		if(path.notNil) {
			this.savePresetCompileString(path, action);
			this.presetCompileStringSavePath = path;
		} {
			if(this.presetCompileStringSavePath.notNil and: { force_dialog==false }) {
				this.savePresetCompileString(this.presetCompileStringSavePath, action);
			} {
				Dialog.savePanel({ arg mypath;
					mypath.debug("save panel: path");
					this.savePresetCompileString(mypath, action);
					this.presetCompileStringSavePath = mypath;
				},{
					//"cancelled".postln;
				});
			}

		};
	}

	presetCompileStringSavePath {
		^this.target.getHalo(\presetCompileStringSavePath);
	}

	presetCompileStringSavePath_ { arg path;
		this.target.addHalo(\presetCompileStringSavePath, path)
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
		if(target.isKindOf(Ndef)) {
			^target.key
		} {
			^""
		}
	}

	outBus_ { arg val;
		if(val.isKindOf(Bus)) {
			val = val.index;
		};
		this.target.initMonitor.out = val;
	}

	outBus { arg val;
		this.target.initMonitor.out ? 0;
	}

	play {
		// hack: Ndef now have same latency than Pdef
		//{ // defer implemented in dereference_event
		target.play;
		//}.defer(Server.default.latency)
	}

	stop {
		// hack: Ndef now have same latency than Pdef
		//{ // defer implemented in dereference_event
		this.doWithQuant {
			if(target.getHalo(\stopIsMute) != false) {
				target.stop(target.fadeTime); // FIXME: how to configure ?
			} {
				target.free;
			}
		};
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
	}

	stopNow {
		if(target.getHalo(\stopIsMute) != false) {
			target.stop(target.fadeTime); // FIXME: how to configure ?
		} {
			target.free;
		}
	}

	isPlaying {
		^target.monitor.isPlaying;
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
			"ERROR: can't save a NodeProxy preset".debug;
			^nil;
		}
	}

}

PlayerWrapper_EventPatternProxy : PlayerWrapper_Base {
	label {
		if(target.isKindOf(Pdef)) {
			^target.key
		} {
			^target.getHalo(\label) ? ""
		}
	}

}

EventPlayerWrapper : PlayerWrapper_Event { } // compat, to be deleted

PlayerWrapper_Event : PlayerWrapper_Base {
	// allow an event to act as a PlayerWrapper

	play {
		target.eventPlay;
	}

	label {
		^target.label ?? {target.class}
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
		Param(Message(this.target), \outBus, XBusSpec()).set(val);
	}

	outBus { arg val;
		^Param(Message(this.target), \outBus, XBusSpec()).get;
	}

	play {
		target.play;
	}

	label {
		^target.label ?? "-"
	}

	isPlaying {
		^target.isPlaying
	}

	quant {
		^this.target.quant;
	}

	quant_ { arg val;
		this.target.quant = val;
	}

	stop {
		this.doWithQuant {
			target.stop;
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
			[path, this.presetCompileStringSavePath, args].debug("PlayerWrapper_ProtoClass.savePresetCompileString");
			this.class.savePresetCompileStringHelper(path ? this.presetCompileStringSavePath, onDoneAction, this.asCompileString, this.target.presetCompileString)
		}
	}

	loadPresetCompileString { arg ...args;
		if(this.presetCompileStringSavePath.notNil) {
			FileSystemProject.load(this.presetCompileStringSavePath);
		} {
			"ERROR: no presetCompileStringSavePath defined for %".format(this).postln;
		}
	}

	presetCompileStringSavePath {
		^this.target.presetCompileStringSavePath
	}

	presetCompileStringSavePath_ { arg path;
		^this.target.presetCompileStringSavePath = path
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

}


///////////////////////////////////////:

PlayerWrapperGroup : List {
	var <>mode;
	var <>label;
	*new { arg anArray;
		var inst;
		inst = super.new.setCollection( anArray.collect({ arg item;
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
	}

	quant {
		^this.first.quant;
	}

	quant_ { arg val;
		this.do({ arg x; x.quant = val });
	}

	play { 
		this.collect(_.play);
	}

	stop {
		this.collect(_.stop);
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
	
}

//////////////////////////////

PatKitDef {
	*new { arg name, val;
		ParamProto.init;
		^~patKitDef.new(name, val);
	}
}
