
///////////////////////////// PlayerWrapper


PlayerWrapper  {
	var <>wrapper;
	var >label;

	*new { arg target;
		^super.new.initWrapper(target);
	}

	initWrapper { arg target;
		// FIXME: handle when not a kind of wrapper in list, and handle GUI when wrapper is nil
		wrapper = case 
			{ target.isNil } {
				//"WARNING: PlayerWrapper: target is nil".debug;
				PlayerWrapper_Nil(target)
				//^nil;
			}
			{ target.isKindOf(PlayerWrapper) } {
				^target
			}
			{ target.isKindOf(ProtoClass) } {
				PlayerWrapper_ProtoClass(target)
			}
			{ target.isKindOf(StepEvent) } {
				PlayerWrapper_ProtoClass(target)
			}
			{ target.isKindOf(Event) } {
				PlayerWrapper_Event(target)
			}
			{ target.isKindOf(NodeProxy) } {
				PlayerWrapper_NodeProxy(target)
			}
			{ target.isKindOf(EventPatternProxy) } {
				PlayerWrapper_EventPatternProxy(target)
			}
			{ target.isKindOf(Param) } {
				PlayerWrapper_Param.new(target)
			}
			{
				// assume target respond to wrapper interface
				target
			}
		;
		
	}

	mapPlayer { arg self, val;
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
			"PlayerWrapper: perform".debug;
			^wrapper.perform(selector, * args);
		} {
			if(wrapper.target.class.findRespondingMethodFor(selector).notNil) {
				"PlayerWrapper: sub perform".debug;
				^wrapper.target.perform(selector, * args);
			} {
				"PlayerWrapper: doesnot".debug;
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
			receiver: Ref(this)
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
}

PlayerWrapper_Base {
	var <>target;
	*new { arg target;
		^super.new.init(target)
	}

	init { arg xtarget;
		target = xtarget
	}

	key { arg self;
		^this.label.asSymbol;
	}


    doesNotUnderstand { arg selector...args;
		[selector, args].debug("PlayerWrapper_Base: doesNotUnderstand");
		if(target.class.findRespondingMethodFor(selector).notNil) {
			"perform".debug;
			^target.perform(selector, * args);
		} {
			"PlayerWrapper_Base: doesnot".debug;
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

	isPlaying {
		^target.monitor.isPlaying;
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
		^target.label ?? "-"
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
