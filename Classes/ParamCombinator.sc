
ParamCombinator {
	// attach to param target so it can reuse the existing ParamCombinator
	// because each ParamCombinator instance has a different Ndef name
	// ndef is used only in bus mode
	// FIXME: maybe use NodeProxy instead, but I don't know if Param support NodeProxy yet
	var <ranges, <inputs, <base;
	var <baseParam, <rangeParam, <inputParam, <targetParam;
	var <controllers; 
	var <rangeSize;
	var <key;
	var <>proxy;
	var <halokey;
	var <>busMode, <>rate;

	*new { arg param, size=3;
		var halokey = \ParamCombinator_+++param.property;
		Class.initClassTree(ParamGroupLayout);
		if(param.target.getHalo(halokey).isNil) {
			var inst = super.new.init(param, size);
			param.target.addHalo(halokey, inst);
			^inst;
		} {
			^param.target.getHalo(halokey)
		}
	}

	//*force { arg param, size=3;

	//}

	*bus { arg ...args;
		^this.kr(*args)
	}

	// not a ugen, is this a good idea ?
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

	clear {
		Ndef(key).clear;
		targetParam.target.addHalo(halokey, nil);
		^nil;
	}

	init { arg param, size=3;

		halokey = \ParamCombinator_+++param.property;
		key = \ParamCombinator_+++1000000.rand.asSymbol;
		busMode = false;

		rangeSize = size;
		ranges = List.newFrom( 0!size );
		inputs = List.newFrom( 0!size );
		targetParam = param;
		base = ParamValue(param.spec).set(param.get);
		baseParam = base.asParam;
		baseParam.label = targetParam.asLabel; 
		baseParam.shortLabel = targetParam.shortLabel;
		baseParam.property = targetParam.property;
		baseParam.combinator = this;

		rangeParam = Param(ranges, \list, \bipolar);
		inputParam = Param(inputs, \list, \unipolar);

		controllers = [
			SimpleController(ranges).put(\set, {
				this.computeTargetValue;
			}),

			SimpleController(inputs).put(\set, {
				this.computeTargetValue;
			}),

			SimpleController(base).put(\set, {
				this.computeTargetValue;
			}),

		];

	}

	freeAllSimpleControllers {
		controllers.do { arg x; x.remove; };
	}

	setBusMode { arg bool=true, name;
		// TODO: disable, anyone ?
		var bus;
		busMode = bool;
		name = name ? key;
		bus = BusDef(name, \control);
		proxy = Ndef(name);
		Ndef(name).clear;
		Ndef(name, {
			var inputs, ranges;
			var fval;
			var sig;
			fval = \base.perform(rate, 0);
			fval = targetParam.spec.unmap(fval);
			inputs = \inputs.perform(rate,0!rangeSize);
			ranges = \ranges.perform(rate,0!rangeSize);

			inputs.do { arg in, x;
				fval = fval + (in * ranges[x])
			};
			sig = targetParam.spec.map(fval);
			//sig.poll;
			sig;
		});
		this.freeAllSimpleControllers;
		baseParam = Param(Ndef(name), \base, targetParam.spec);
		baseParam.set(targetParam.get);
		targetParam.target.set(targetParam.property, Ndef(name).asMap);
		rangeParam = Param(Ndef(name), \ranges, ParamArraySpec(\bipolar ! rangeSize));
		rangeParam.set(ranges.asArray); // whyyyy list doesnt do anything ????
		inputParam = Param(Ndef(name), \inputs, ParamArraySpec(\unipolar ! rangeSize));
		inputParam.set(inputs.asArray);


		baseParam.label = targetParam.asLabel;
		baseParam.shortLabel = targetParam.shortLabel.asString + "(m)";
		baseParam.combinator = this;
	}

	computeTargetValue {
		var fval;
		fval = baseParam.normGet;
		inputs.do { arg in, x;
			fval = fval + (in * ranges[x])
		};
		targetParam.normSet(fval);
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

	edit { arg self;
		var window = Window.new;
		var layout;
		var modknob;
		modknob = ModKnob.new
			.onChange(ranges, \set, { arg view;
				ranges.do { arg val, x;
					modknob.set_range(x, val);
				};
				modknob.refresh;
			})
			.onChange(base, \set, { arg view;
				modknob.value = baseParam.normGet;
			})
			.onChange(targetParam.target, \set, { arg view;
				modknob.midi_value = targetParam.normGet;
				modknob.refresh;
			})
			.action_({
				baseParam.normSet(modknob.value)
			})
			;
		modknob.view.minSize = 100@100;
		layout = HLayout(
			modknob.view,
			ParamGroupLayout.two_panes(ParamGroup([
				baseParam,
				rangeParam,
				inputParam,
				targetParam,
			].flatten)),
		);
		window.layout = layout;
		window.alwaysOnTop = true;
		window.front;
	}
}
