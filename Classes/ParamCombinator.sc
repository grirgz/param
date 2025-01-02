// GUI is in proto/editors.scd: WindowDef(\ParamCombinatorEditor)r)

ParamCombinator : Pattern {
	// replace the value of the target param with itself

	// because each ParamCombinator instance has a different Ndef name
	// ndef is used only in bus mode
	// FIXME: maybe use NodeProxy instead, but I don't know if Param support NodeProxy yet
    

    // resultParam = inputParam * rangeParam + baseParam
    // targetParam: this point to the param to be combinated, first argument of ParamCombinator
	// baseParam: the param to be controlled by GUI that should replace the old one
	var <ranges, <inputs, <base, <result;
	var <baseParam, <rangeParam, <inputParam, <targetParam, <resultParam;
	var <controllers; 
	var <rangeSize;
	var <key;
	var <>proxy;
	var <halokey;
	var <>busMode, <>rate = \kr;
	var <>size;
    var <>inputObjects; // to store pointers to modulators objects for editing and such

	*new { arg param, size=3;
		var halokey = ( \ParamCombinator_++param.property ).asSymbol;
		//Class.initClassTree(ParamGroupLayout); // FIXME: why ?
		//if(param.getRaw.isKindOf(ParamCombinator).not) {
		if(param.hasCombinator.not) {
			var inst = super.new.init(param, size);
			param.target.addHalo(halokey, inst);
			param.target.changed(\combinator, param.propertyRoot);
			^inst;
		} {
			param.debug("ParamCombinator already exist, use it");
			//^param.target.getHalo(halokey)
			^param.getCombinator;
			//^param.getRaw
		}
	}

	init { arg param, xsize=3;
		halokey = ( \ParamCombinator_++param.property ).asSymbol;
		key = ( \ParamCombinator_++1000000.rand ).asSymbol;
		busMode = false;
		size = xsize;

		rangeSize = size;
		ranges = List.newFrom( 0!size );
		inputs = List.newFrom( 0!size );
		inputObjects = List.newFrom( nil!size );

		targetParam = param;

		///// value is stored in targetParam because we can't .set a Pattern
		///// value is stored in resultParam when targetParam contains the combinator

		base = ParamValue(targetParam.spec); 
		baseParam = base.asParam;
		result = ParamValue(targetParam.spec); 
		resultParam = result.asParam; 
		//resultParam = targetParam; // this make infinite loop when targetParam is a combinator

		baseParam.set(targetParam.get); // if already redirected, return baseParam.get

		baseParam.label = targetParam.asLabel; 
		baseParam.shortLabel = targetParam.shortLabel;
		baseParam.property = targetParam.property;
		baseParam.combinator = this;

		rangeParam = Param(ranges, \list, \bipolar);
		inputParam = Param(inputs, \list, \unipolar);

		controllers = [
			SimpleController(ranges).put(\set, {
				this.computeAndSetTargetValue;
			}),

			SimpleController(inputs).put(\set, {
				//Log(\Param).debug("ParamCombinator followChange set input");
				this.computeAndSetTargetValue;
			}),

			SimpleController(base).put(\set, {
				this.computeAndSetTargetValue;
			}),

		];

		TagSpecDef(\ParamCombinator).add(this);

        if(param.target.isKindOf(Pdef) and: { param.target.source.isKindOf(PbindProxy) }) {
			// can only redirect with Pbindef
			// todo: test with Ndef
			if(param.propertyArray.last == \source) {
				param.set(this);
			} {
				param.at(\source).set(this);
			};
		} {
			this.inBusMode = true;
			param.set(this); // in envir, Pdef will call .asControlInput
		};
		

	}
	//*force { arg param, size=3;

	//}
	//setCombinator { arg param;
		//param.set(ParamCombinator(param))

	//}

	*bus { arg ...args;
		^this.kr(*args)
	}

	// not a ugen, is this a good idea ?
	// use Ndef with control rate busses
	*kr { arg  param, size=3;
		var inst = this.new(param, size);
		if(inst.busMode.not) {
			inst.rate = \kr;
			inst.setBusMode(true); 
		} {
			inst.proxy.wakeUp; // when CmdPeriod, proxy is freed, need to wake up to work again
		};
		^inst;
	}

	// use Ndef with audio rate busses
	// i don't really know if its useful
	*ar { arg  param, size=3;
		var inst = this.new(param, size);
		if(inst.busMode.not) {
			inst.rate = \ar;
			inst.setBusMode(true);
		} {
			inst.proxy.wakeUp;
		};
		^inst;
	}

	clear { arg fullyRemove=true;
		var oldval = targetParam.get;
		oldval.debug("ParamCombinator.clear");
		this.size.do { arg idx;
			this.clearInput(idx)
		};
		if(fullyRemove == true) {
			Ndef(key).clear;
			targetParam.target.addHalo(halokey, nil);
			if(targetParam.target.source.isKindOf(PbindProxy)) {
				targetParam.at(\source).getRaw.debug("ParamCombinator.clear set source");
				targetParam.at(\source).unset;
				targetParam.setRaw(oldval);
				targetParam.at(\source).getRaw.debug("ParamCombinator.clear set source after");
			} {
				targetParam.debug("ParamCombinator.clear set");
				targetParam.setRaw(oldval);
			};
			targetParam.target.changed(\combinator, targetParam.propertyRoot);
			targetParam.target.changed(\set, [ targetParam.propertyRoot ]);
			^nil;
		};
	}


	freeAllSimpleControllers {
		controllers.do { arg x; x.remove; };
	}

	setBusMode { arg bool=true, name;
		// TODO: disable, anyone ?
		var bus;
		var rawVal;
		var val, inputtab, rangetab;
		if(busMode == bool) {
			^this
		};
		busMode = bool;
		if(bool == true) {
			name = name ? key;
			//bus = BusDef(name, \control);
			proxy = Ndef(name);
			this.freeAllSimpleControllers;
			//[targetParam, targetParam.getRaw, targetParam.get].debug("MAIS QUOI");
			val = targetParam.get;
			ranges = rangeParam.get;
			inputtab = inputParam.getBus; // contains mapped bus, should not assign to 'inputs'
			Ndef(name).clear;
			Ndef(name, {
				var inputtab, rangetab;
				var fval;
				var sig;
				//Log(\Param).debug("ParamCombinator: rate %", rate);
				fval = \base.perform(rate, 0);
				fval = targetParam.spec.unmap(fval);
				inputtab = \inputs.perform(rate,0!rangeSize);
				rangetab = \ranges.perform(rate,0!rangeSize);

				fval = this.computeTargetValue(fval, inputtab, rangetab);
				sig = targetParam.spec.map(fval);
				//sig.poll(label:"final sig");

			});
			baseParam = Param(Ndef(name), \base, targetParam.spec);
			baseParam.set(val); // if already redirected, return baseParam.get

			baseParam.label = targetParam.asLabel; 
			baseParam.shortLabel = targetParam.shortLabel;
			//targetParam.target.set(targetParam.property, Ndef(name).asMap.asSymbol); // Ndef.asMap is String!!!
			//targetParam.setRaw(this);   // already done in non bus mode
			//targetParam.setRaw(proxy.asMap.asSymbol); 
			rangeParam = Param(Ndef(name), \ranges, ParamArraySpec(\bipolar ! rangeSize));
			rangeParam.set(ranges.asArray); // whyyyy list doesnt do anything ????
			inputParam = Param(Ndef(name), \inputs, ParamArraySpec(ParamMappedBusSpec() ! rangeSize));
			inputParam.set(inputtab.asArray);


			baseParam.label = targetParam.asLabel;
			baseParam.shortLabel = targetParam.shortLabel.asString + "(m)";
			baseParam.combinator = this;
		} {
			
			baseParam = base.asParam;
		}
	}

	computeTargetValue { arg fval, inputtab, rangetab;
		//fval.poll(label: "fval start");
		//inputtab.poll(label:"inputtab in cstv");
		//rangetab.poll(label:"rangetab in cstv");
		inputtab.do { arg in, x;
			fval = fval + (in * rangetab[x])
		};
		//fval.poll(label:"fval end");
		^fval;
	}

	computeAndSetTargetValue {
		var fval;
		//Log(\Param).debug("computeAndSetTargetValue");
		fval = baseParam.normGet;
		//Log(\Param).debug("computeAndSetTargetValue1");
		fval = this.computeTargetValue(fval, inputs, ranges);
		//Log(\Param).debug("computeAndSetTargetValue2");
		resultParam.normSet(fval);
		//Log(\Param).debug("computeAndSetTargetValue end");
	}

	mapModKnob { arg modknob;
		modknob
			.onChange(rangeParam.target, \set, { arg view;
				//debug("ranges changz");
				rangeParam.do { arg param, x;
					modknob.set_range(x, param.get);
				};
				modknob.refresh;
			})
			.onChange(baseParam.target, \set, { arg view;
				//debug("base changz");
				modknob.value = baseParam.normGet;
			})
			.onChange(targetParam.target, \set, { arg view;
				var nval;
				var gval;
				//debug("target changz");
				gval = targetParam.get;
				if(gval.isKindOf(Number)) {
					modknob.midi_value = targetParam.normGet;
					modknob.refresh;
				}
			})
			.action_({
				//debug("action changz");
				baseParam.normSet(modknob.value)
			})
		;
		baseParam.target.changed(\set);
		rangeParam.target.changed(\set);
		targetParam.target.changed(\set);

	}

	edit { 
		WindowDef(\ParamCombinatorEditor).front(this);
		//var window = Window.new;
		//var layout;
		//var modknob;
		//modknob = ModKnob.new
			//.onChange(ranges, \set, { arg view;
				//ranges.do { arg val, x;
					//modknob.set_range(x, val);
				//};
				//modknob.refresh;
			//})
			//.onChange(base, \set, { arg view;
				//modknob.value = baseParam.normGet;
			//})
			//.onChange(targetParam.target, \set, { arg view;
				//modknob.midi_value = targetParam.normGet;
				//modknob.refresh;
			//})
			//.action_({
				//baseParam.normSet(modknob.value)
			//})
			//;
		//modknob.view.minSize = 100@100;
		//layout = HLayout(
			//modknob.view,
			//ParamGroupLayout.two_panes(ParamGroup([
				//baseParam,
				//rangeParam,
				//inputParam,
				//targetParam,
			//].flatten)),
		//);
		//window.layout = layout;
		//window.alwaysOnTop = true;
		//window.front;
	}

	asControlInput {
		// called for example by Pdef when in envir
		this.playAll; 
		^proxy.asMap.asSymbol;
	}

	inBusMode {
		^busMode
	}

	inBusMode_ { arg val;
		this.setBusMode(val)
	}

	embedInStream { arg in;
		^this.asPattern.embedInStream(in);
	}

	asStream { arg in;
		^this.asPattern.asStream(in);
	}

	streamArg {
		^this.asStream;
	}

	mapPatternKeyToInput { arg patkey, idx, spec, range;
		var obj;
		if(idx.isNil) {
			idx = this.nextFreeInput;
		};
		if(idx.isNil) {
			"ParamCombinator: No more slot available for %".format(this.targetParam).error;
			^nil;
		};
		if(this.inputObjects.select(_.notNil).any { arg obj;
			obj.patKey.notNil and: {
				obj.patKey == patkey;
			}
		}) {
			Log(\Param).info("Already connected");
			^nil;
		};
		obj = ProtoTemplateDef(\PatternCombinatorSlot).new(this, idx, patkey, spec);
		this.inputObjects[idx] = obj;
		if(range.notNil) {
			this.rangeParam.at(idx).set(range)
		};
		targetParam.target.changed(\combinator, targetParam.propertyRoot);
		^idx
	}

	mapNodeProxyToInput { arg proxy, idx, spec, range;
		if(idx.isNil) {
			idx = this.nextFreeInput;
		};
		if(idx.isNil) {
			"ParamCombinator: No more slot available for %".format(this.targetParam).error;
			^nil;
		};
		if(this.inputObjects.select(_.notNil).any { arg obj;
			obj.proxy.notNil and: {
				obj.proxy == proxy;
			}
		}) {
			Log(\Param).info("Already connected");
			^nil;
		};
		this.inBusMode = true;
		//[proxy, idx, spec].debug("mapNodeProxyToInput: proxy, idx, spec");
		this.inputParam.at(idx).setBus(proxy.asMap);
		this.inputObjects[idx] = ProtoTemplateDef(\NodeProxyCombinatorSlot).new(this, idx, proxy, spec);
		if(range.notNil) {
			this.rangeParam.at(idx).set(range)
		};
		targetParam.target.changed(\combinator, targetParam.propertyRoot);
		^idx
	}

	mapProtoClassToInput { arg proto, idx, spec, range;
		if(idx.isNil) {
			idx = this.nextFreeInput;
		};
		if(idx.isNil) {
			"ParamCombinator: No more slot available for %".format(this.targetParam).error;
			^nil;
		};
		if(this.inputObjects.select(_.notNil).any { arg obj;
			obj.proxy == proto;
		}) {
			Log(\Param).info("Already connected");
			^nil;
		};
		this.inBusMode = true;
		//[proxy, idx, spec].debug("mapNodeProxyToInput: proxy, idx, spec");
		this.inputParam.at(idx).setBus(proto.outBus.asMap);
		this.inputObjects[idx] = ProtoTemplateDef(\ProtoClassCombinatorSlot).new(this, idx, proto, spec);
		if(range.notNil) {
			this.rangeParam.at(idx).set(range)
		};
		targetParam.target.changed(\combinator, targetParam.propertyRoot);
		^idx
	}

	mapObjectToInput { arg obj, idx, spec, range;
		case(
			{ obj.isNil }, {
				this.clearInput(idx);
			},
			{ obj.isKindOf(Symbol) }, {
				this.mapPatternKeyToInput(obj, idx, spec, range);
			},
			{ obj.isKindOf(ProtoClass) }, {
				this.mapProtoClassToInput(obj, idx, spec, range);
			},
			{ obj.isKindOf(NodeProxy) }, {
				this.mapNodeProxyToInput(obj, idx, spec, range);
			}, {
				"ParamCombinator: don't know what to do with %".format(obj).error;
			}
		)
	}

	asPattern { 
		//Log(\Param).debug("asPattern");
		^if(this.inBusMode) {
			//Log(\Param).debug("asPattern busmode");
			Pfunc({ arg ev;
				this.playAll(ev);
				proxy.asMap.asSymbol
			});
		} {
			//Log(\Param).debug("asPattern value");
			Pfunc({ arg ev;
				this.playAll(ev);
				this.resultParam.get 
			});
		}
	}

	clearInput { arg idx;
		this.rangeParam.at(idx).set(0);
		this.inputParam.at(idx).setBus(0);
		this.inputObjects[idx] = nil;
		targetParam.target.changed(\combinator, targetParam.propertyRoot);
	}

	playAll { arg ev;
		//Log(\Param).debug("ParamCombinator: play all");
		if(this.inBusMode) {
			proxy.wakeUp;
		};
		//this.inputParam.do { arg param, idx;
			//var val = param.get;
			////[val.asCompileString, idx, param, proxy].debug("playAll: val");
			//if(val.isKindOf(Symbol) or: { val.isKindOf(String) }) {
				//var nkey = TagSpecDef(\ParamCombinatorInput_asMap).unmapKey(val);
				////Log(\Param).debug("ParamCombinator.playAll: nkey %", nkey);
				//if(nkey.notNil) {
					//Ndef(nkey).wakeUp;
				//} {
					//Log(\Param).debug("ParamCombinator.playAll: key not found for %", val);
				//}
			//}
		//};
        this.inputObjects.do { arg obj, idx;
			if(obj.notNil and: { obj.isPlaying != true }) {
				obj.play(ev);
			}
        };
	}

    nextFreeInput {
		^this.inputParam.as(Array).detectIndex { arg p, pidx; 
			( p.getBus != 0 or: { this.inputObjects[pidx].notNil } ).not 
		}
    }

	existingInputRanges {
		var res = List.new;
		this.inputParam.as(Array).do { arg p, pidx; 
			if(
				p.getBus != 0 or: { this.inputObjects[pidx].notNil }
			) {
				res.add(this.rangeParam.at(pidx))
			};
		};
		^res;
	}

	existingInputObjects {
		var res = List.new;
		this.inputParam.as(Array).do { arg p, pidx; 
			if(
				p.getBus != 0 or: { this.inputObjects[pidx].notNil }
			) {
				res.add(this.inputObjects.at(pidx))
			};
		};
		^res;
	}

	asMap {
		^proxy.asMap.asSymbol;
	}

	/// interface of CachedBus

	asCachedBus {
		^this
	}

	getCached {
		^this.baseParam.get;
	}

	get {
		^this.baseParam.get;
	}

	set { arg val;
		Log(\Param).debug("ParamCombinator set val %".format(val));
		this.baseParam.set(val);
	}

	mapAllInputs { arg objlist, rangelist;
		this.size.do { arg idx;
			this.mapObjectToInput(objlist[idx], idx, range:rangelist[idx]);
		};
	}

	presetCompileString {
		var inlist = "[%]".format(this.inputObjects.collect({ arg obj, idx;
			if(obj.isNil) {
				nil
			} {
				obj.presetCompileString;
			};	
		}).join(", "));
		var rlist = "[%]".format(this.rangeParam.collect({ arg subp, idx;
			if(subp.isNil) {
				0
			} {
				subp.get.asCompileString;
			};	
		}).join(", "));
		^"ParamCombinator(%).mapAllInputs(%, %);\n".format(this.targetParam.asCompileString, inlist, rlist);
	}
}

ParamCustomCombinator : ParamCombinator {
	var computeFunction, inputParamList;

	*new { arg param, computefun, inputlist;
		var halokey = \ParamCombinator_+++param.property;
		if(param.target.getHalo(halokey).isNil) {
			var inst = super.new(param).initParamCustomCombinator(computefun, inputlist);
			param.target.addHalo(halokey, inst);
			param.target.changed(\combinator, param.property);
			^inst;
		} {
			^param.target.getHalo(halokey)
		}
	}

	init { arg param, xsize=3;
		halokey = ( \ParamCombinator_++param.property ).asSymbol;
		key = ( \ParamCombinator_++1000000.rand ).asSymbol;
		busMode = false;

		targetParam = param;

		///// value is stored in targetParam because we can't .set a Pattern
		///// value is stored in resultParam when targetParam contains the combinator

		base = ParamValue(targetParam.spec); 
		baseParam = base.asParam;
		resultParam = targetParam; // for compat when targetParam is a Combinator

		baseParam.set(targetParam.get); // if already redirected, return baseParam.get

		baseParam.label = targetParam.asLabel; 
		baseParam.shortLabel = targetParam.shortLabel;
		baseParam.property = targetParam.property;
		baseParam.combinator = this;

		controllers = [
			SimpleController(base).put(\set, {
				this.computeAndSetTargetValue;
			}),
		];

		TagSpecDef(\ParamCustomCombinator).add(this);

	}

	initParamCustomCombinator { arg computefun, inputlist;
		inputParamList = inputlist ?? { [] };
		computeFunction = computefun ?? { {0} };

		inputlist.do { arg inputparam, idx;
			controllers = controllers.add(
				SimpleController(inputparam).put(\set, { // FIXME: does param signal \set itself ?
					this.computeAndSetTargetValue;
				}),
			);
		};
	}

	computeTargetValue { arg fval, inputlist;
		^computeFunction.(fval, inputlist)
	}

	computeAndSetTargetValue {
		var fval;
		fval = baseParam.normGet;
		fval = this.computeTargetValue(fval, inputParamList);
		resultParam.normSet(fval);
	}

}

//ParamCombinator_halo {

	//// attach to param target so it can reuse the existing ParamCombinator
	//// because each ParamCombinator instance has a different Ndef name
	//// ndef is used only in bus mode
	//// FIXME: maybe use NodeProxy instead, but I don't know if Param support NodeProxy yet
	//var <ranges, <inputs, <base;
	//var <baseParam, <rangeParam, <inputParam, <targetParam;
	//var <controllers; 
	//var <rangeSize;
	//var <key;
	//var <>proxy;
	//var <halokey;
	//var <>busMode, <>rate = \kr;

	//*new { arg param, size=3;
		//// FIXME: if use new without *kr, there is no rate
		//var halokey = \ParamCombinator_+++param.property;
		//Class.initClassTree(ParamGroupLayout);
		//if(param.target.getHalo(halokey).isNil) {
			//var inst = super.new.init(param, size);
			//param.target.addHalo(halokey, inst);
			//param.target.changed(\combinator, param.property);
			//^inst;
		//} {
			//^param.target.getHalo(halokey)
		//}
	//}

	///[>force { arg param, size=3;

	////}

	//*bus { arg ...args;
		//^this.kr(*args)
	//}

	//// not a ugen, is this a good idea ?
	//*kr { arg  param, size=3;
		//var inst = this.new(param, size);
		//if(inst.busMode.not) {
			//inst.rate = \kr;
			//inst.setBusMode(true);
		//} {
			//inst.proxy.wakeUp; // when CmdPeriod, proxy is freed, need to wake up to work again
		//};
		//^inst;
	//}

	//*ar { arg  param, size=3;
		//var inst = this.new(param, size);
		//if(inst.busMode.not) {
			//inst.rate = \ar;
			//inst.setBusMode(true);
		//} {
			//inst.proxy.wakeUp;
		//};
		//^inst;
	//}

	//clear {
		//Ndef(key).clear;
		//targetParam.target.addHalo(halokey, nil);
		//^nil;
	//}

	//init { arg param, size=3;

		//halokey = \ParamCombinator_+++param.property;
		//key = \ParamCombinator_+++1000000.rand.asSymbol;
		//busMode = false;

		//rangeSize = size;
		//ranges = List.newFrom( 0!size );
		//inputs = List.newFrom( 0!size );
		//targetParam = param.asUncombinatedParam;
		//base = ParamValue(param.spec).set(param.get);
		//baseParam = base.asParam;
		//baseParam.label = targetParam.asLabel; 
		//baseParam.shortLabel = targetParam.shortLabel;
		//baseParam.property = targetParam.property;
		//baseParam.combinator = this;

		//rangeParam = Param(ranges, \list, \bipolar);
		//inputParam = Param(inputs, \list, \unipolar);

		//controllers = [
			//SimpleController(ranges).put(\set, {
				//this.computeTargetValue;
			//}),

			//SimpleController(inputs).put(\set, {
				//this.computeTargetValue;
			//}),

			//SimpleController(base).put(\set, {
				//this.computeTargetValue;
			//}),

		//];

	//}

	//freeAllSimpleControllers {
		//controllers.do { arg x; x.remove; };
	//}

	//setBusMode { arg bool=true, name;
		//// TODO: disable, anyone ?
		//var bus;
		//var rawVal;
		//busMode = bool;
		//name = name ? key;
		//bus = BusDef(name, \control);
		//proxy = Ndef(name);
		//Ndef(name).clear;
		//Ndef(name, {
			//var inputs, ranges;
			//var fval;
			//var sig;
			//Log(\Param).debug("ParamCombinator: rate %", rate);
			//fval = \base.perform(rate, 0);
			//fval = targetParam.spec.unmap(fval);
			//inputs = \inputs.perform(rate,0!rangeSize);
			//ranges = \ranges.perform(rate,0!rangeSize);

			//inputs.do { arg in, x;
				//fval = fval + (in * ranges[x])
			//};
			//sig = targetParam.spec.map(fval);
			////sig.poll;
			//sig;
		//});
		//this.freeAllSimpleControllers;
		//baseParam = Param(Ndef(name), \base, targetParam.spec);
		//baseParam.set(targetParam.get);
		//targetParam.target.set(targetParam.property, Ndef(name).asMap.asSymbol); // Ndef.asMap is String!!!
		//rangeParam = Param(Ndef(name), \ranges, ParamArraySpec(\bipolar ! rangeSize));
		//rangeParam.set(ranges.asArray); // whyyyy list doesnt do anything ????
		//inputParam = Param(Ndef(name), \inputs, ParamArraySpec(\unipolar ! rangeSize));
		//inputParam.set(inputs.asArray);


		//baseParam.label = targetParam.asLabel;
		//baseParam.shortLabel = targetParam.shortLabel.asString + "(m)";
		//baseParam.combinator = this;
	//}

	//computeTargetValue {
		//var fval;
		//fval = baseParam.normGet;
		//inputs.do { arg in, x;
			//fval = fval + (in * ranges[x])
		//};
		//targetParam.normSet(fval);
	//}

	//mapModKnob { arg modknob;
		//modknob
			//.onChange(rangeParam.target, \set, { arg view;
				////debug("ranges changz");
				//rangeParam.do { arg param, x;
					//modknob.set_range(x, param.get);
				//};
				//modknob.refresh;
			//})
			//.onChange(baseParam.target, \set, { arg view;
				////debug("base changz");
				//modknob.value = baseParam.normGet;
			//})
			//.onChange(targetParam.target, \set, { arg view;
				//var nval;
				//var gval;
				////debug("target changz");
				//gval = targetParam.get;
				//if(gval.isKindOf(Number)) {
					//modknob.midi_value = targetParam.normGet;
					//modknob.refresh;
				//}
			//})
			//.action_({
				////debug("action changz");
				//baseParam.normSet(modknob.value)
			//})
		//;
		//baseParam.target.changed(\set);
		//rangeParam.target.changed(\set);
		//targetParam.target.changed(\set);

	//}

	//edit { arg self;
		//var window = Window.new;
		//var layout;
		//var modknob;
		//modknob = ModKnob.new
			//.onChange(ranges, \set, { arg view;
				//ranges.do { arg val, x;
					//modknob.set_range(x, val);
				//};
				//modknob.refresh;
			//})
			//.onChange(base, \set, { arg view;
				//modknob.value = baseParam.normGet;
			//})
			//.onChange(targetParam.target, \set, { arg view;
				//modknob.midi_value = targetParam.normGet;
				//modknob.refresh;
			//})
			//.action_({
				//baseParam.normSet(modknob.value)
			//})
			//;
		//modknob.view.minSize = 100@100;
		//layout = HLayout(
			//modknob.view,
			//ParamGroupLayout.two_panes(ParamGroup([
				//baseParam,
				//rangeParam,
				//inputParam,
				//targetParam,
			//].flatten)),
		//);
		//window.layout = layout;
		//window.alwaysOnTop = true;
		//window.front;
	//}

	//asControlInput {
		//baseParam.getRaw.asMap;
	//}
//}
