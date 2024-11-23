
// Pdef was replaced by EventPatternProxy as a hope to generalize it easily
+EventPatternProxy {
	*nestOn { arg val;
		// see also .bubble and .unbubble
		if(val.isSequenceableCollection or: { val.isKindOf(Env) }) {
            // in Pdef, envs are arrays, but in Pbindef they are Env
			if(val[0].isSequenceableCollection or: { val[0].isKindOf(Env) }) {
				// NOOP
			} {
				val = [val]
			}
		};
		^val
	}

	*nestOff { arg val;
		if(val.isSequenceableCollection) {
			if(val[0].isSequenceableCollection or: { val[0].isKindOf(Env) }) {
				val = val[0];
			} {
				// NOOP
			}
		};
		^val
	}

	setBusMode { arg key, enable=true, free=true;
		//"whatktktj".debug;
		if(enable) {
			//"1whatktktj".debug;
			if(this.inBusMode(key)) {
				// NOOP
				//"2whatktktj".debug;
			} {
				var val = this.getVal(key);
				var numChannels = 1;
				var bus;
				//"3whatktktj".debug;
				//val.debug("setBusMode: val");
				if(val.isSequenceableCollection) {
					numChannels = val.size;
				};
				//numChannels.debug("setBusMode: nc");
				bus = CachedBus.control(Server.default,numChannels );
					// FIXME: hardcoded server
					// hardcoded rate, but can't convert audio data to a number, so it's ok
				//bus.debug("setBusMode: bus");
				if(val.isSequenceableCollection) {
					bus.setn(val);
				} {
					bus.set(val);
				};
				//val.debug("setBusMode: val");
				this.set(key, this.class.nestOn(bus.asMap));
				//bus.asMap.debug("setBusMode: busmap");
			}
		} {
			if(this.inBusMode(key).not) {
				// NOOP
			} {
				var val = this.getVal(key);
				var map = this.get(key);
				var numChannels = 1;
				var bus;
				map = this.class.nestOff(map);
				bus = map.asCachedBus;
				this.set(key, this.class.nestOn(val));
				if(free) {
					bus.free;
				};
			}

		}

	}

	*symbolIsBus { arg sym;
		var num = #[ $1, $2, $3, $4, $5, $6, $7, $8, $9, $0  ];
		sym = sym.asString;
		^[$c, $a].includes(sym[0]) and: { sym.drop(1).every({ arg x; num.includes(x) }) };
	}

	inBusMode { arg key;
		var val = this.get(key);
		if(val.isSequenceableCollection) {
			// multichannel
			if(val[0].isSequenceableCollection) {
				// nested
				^(val[0][0].class == Symbol) and: { this.class.symbolIsBus(val[0][0]) }
			} {
				^(val[0].class == Symbol) and: { this.class.symbolIsBus(val[0]) }
			}
		} {
			^((val.class == Symbol and: { this.class.symbolIsBus(val) }) or: { val.isKindOf(ParamCombinator) or: { this.getHalo(( \ParamCombinator_++key ).asSymbol) !? (_.inBusMode) == true } })
		}
	}

	//hasCombinator {
		//this.getHalo(( \ParamCombinator_++key ).asSymbol).notNil
	//}

	//combinator {

	//}

	getVal { arg key;
		var curval;
		var combi;
		var bus;
		if(this.envir.isNil) { this.envir = this.class.event };
		curval = this.get(key);
		curval = this.class.nestOff(curval);
		if(this.inBusMode(key)) {
			combi = this.getHalo(( \ParamCombinator_++key ).asSymbol);
			if( combi.notNil ) {
				^combi.get;
			} {
				bus = curval.asCachedBus;
				^bus.getCached;
			}
		} {
			if(curval.class == Function) {
				// this use default value of event instead of using default value of param/spec
				curval = this.envir.use({ curval.value });
			};
			^curval;
		};
	}

	setVal { arg key, val;
		var bus;
		var curval;
		var combi;
		if(val.isKindOf(Env)) {
			this.set(key, this.class.nestOn(val))
		} {
			if(val.isKindOf(Symbol) or: { val.isKindOf(SequenceableCollection) and: { val[0].isKindOf(Symbol) } }) { 
				// If the val is a mapped bus, replace the mapped bus
				// Used to map modulator bus to parameters
                // This should be handled in another layer, we can do it here because we see the value to set is a symbol,
                // but it is not possible in getVal
				this.set(key, val)
			} {
				if(this.inBusMode(key)) {
					combi = this.getHalo(( \ParamCombinator_++key ).asSymbol);
					if( combi.notNil ) {
						combi.set(val);
					} {
						curval = this.get(key);
						curval = this.class.nestOff(curval);
						bus = curval.asCachedBus;
						if(curval.isSequenceableCollection) {
							bus.setn(val);
						} {
							bus.set(val);
						};
					};
					this.changed(\set, [key, val]);
				} {
					this.set(key, this.class.nestOn(val))
				};
			}
		}
	}

	asParamGroup { arg instrument, notes=true, exclude;
		// TODO: find a better name for 'notes' argument (this is for adding \dur and \legato)
		var list;

		instrument = instrument ?? { PdefParam.instrument(this) };
		if(instrument.isNil) {
			//"ERROR: Pdef:asParamGroup: Can't create paramGroup: no instrument is defined".postln;
			//^nil
			list = List.new;
		} {
			var synthdesc;
			exclude = exclude ? [\gate, \doneAction];

			synthdesc = SynthDescLib.global.synthDescs[instrument];
			if(synthdesc.isNil) {
				Log(\Param).error("ERROR: Pdef:asParamGroup: Can't create paramGroup: no synthdesc for this instrument: %".format(instrument));
				list = List.new;
			} {
				list = synthdesc.controls.reject({ arg con; 
					con.name == '?' or: {
						exclude.includes(con.name)
					}
				}).collect({ arg con;
					Param( this, con.name );
				});
			}

		};
		if(notes) {
			list = [
				Param(this, \dur),
				Param(this, \legato),
			] ++ list;
		};
		^ParamGroup(list)
	}

	asEnvirCompileString {
		^this.envir.collect({ arg x; x.asCompileString })
	}

	presetCompileString {
		// FIXME: need to test if Pbindef.presetCompileString is not broken
		^this.asParamGroup.selectByKey(( this.envir ?? { () } ).keys.asArray).getSetCompileString;
	}

	asPatternCompileString {
		var res;
		res = "Pbind(\n".format(this.key);
		this.envir.keysValuesDo({ arg k,v;
			res = res ++ "\t%, %,\n".format(k.asCompileString, v.asCompileString);
		});
		res = res ++ ");\n";
		^res;
	}
}

+Pdef {
	edit { 
		ParamProto.init;
		if(this.getHalo(\model).notNil) {
			this.getHalo(\model).edit;
		} {
			if(this.class == Pbindef and: { WindowDef(\PbindefEditor).notNil }) {
				^WindowDef("PbindefEditor_%".format(this.key).asSymbol, WindowDef(\PbindefEditor)).front(this)
			} {
				if(WindowDef(\PdefEditor).notNil) {
					^WindowDef("PdefEditor_%".format(this.key).asSymbol, WindowDef(\PdefEditor)).front(this)
				} {
					Log(\Param).info("no editor found: WindowDef(\\PdefEditor) is not defined");
					^nil
				}
			}
		}
	}

	convertToPbindef {
		Pbindef.convertToPbindef(this);
		^Pbindef(this.key);
	}
}

+Pbindef {
	*convertToPbindef { arg proxy;
		var src = proxy.source;
		var pairs = [];
		if(src.class === PbindProxy) {
			// OK
		} {
			if(src.isKindOf(Pbind))
			{
				src.patternpairs.pairsDo { |key, pat|
					if(pairs.includes(key).not) {
						pairs = pairs.add(key);
						pairs = pairs.add(pat);
					}
				}
			};

			src = PbindProxy.new(*pairs).quant_(proxy.quant);
			proxy.source = src
		};

	}
}

+Ndef {
	asParamGroup { arg exclude;
		// TODO: add method argument (find a better name) for adding volume
		exclude = exclude ?? {[]};
		^ParamGroup(
			this.controlNames.reject({ arg con; 
				con.name == '?' or: {
					exclude.includes(con.name)
				}
			}).collect{ arg con;
				Param(this, con.name)
			}
		)
	}

	isNaN {
		// used to avoid NumberBox and EZ* GUI to throw an error
		^true
	}

	setBusMode { arg key, enable=true, free=true;
		//"whatktktj".debug;
		if(enable) {
			//"1whatktktj".debug;
			if(this.inBusMode(key)) {
				// NOOP
				//"2whatktktj".debug;
			} {
				var val = this.getVal(key);
				var numChannels = 1;
				var bus;
				//"3whatktktj".debug;
				//val.debug("setBusMode: val");
				if(val.isSequenceableCollection) {
					numChannels = val.size;
				};
				//numChannels.debug("setBusMode: nc");
				bus = CachedBus.control(Server.default,numChannels );
					// FIXME: hardcoded server
					// hardcoded rate, but can't convert audio buffer to a number, so it's ok
				//bus.debug("setBusMode: bus");
				if(val.isSequenceableCollection) {
					bus.setn(val);
				} {
					bus.set(val);
				};
				//val.debug("setBusMode: val");
				this.set(key, bus.asMap);
				//bus.asMap.debug("setBusMode: busmap");
			}
		} {
			if(this.inBusMode(key).not) {
				// NOOP
			} {
				var val = this.getVal(key);
				var map = this.get(key);
				var numChannels = 1;
				var bus;
				bus = map.asCachedBus;
				this.set(key, val);
				if(free) {
					bus.free;
				};
			}

		}

	}

	inBusMode { arg key;
		var val = this.get(key);
		if(val.isSequenceableCollection) {
			// multichannel
            ^(val[0].class == Symbol)
			// no nested in Ndef, only in Pdef
			//if(val[0].isSequenceableCollection) {
				//^(val[0][0].class == Symbol)
			//} {
				//^(val[0].class == Symbol)
			//}
		} {
			^(val.class == Symbol or: { val.isKindOf(ParamCombinator) })
		}
	}

	getVal { arg key;
		var curval;
		//if(this.envir.isNil) { this.envir = this.class.event };
		curval = this.get(key);
		if(this.inBusMode(key)) {
			var bus = curval.asCachedBus;
			^bus.getCached;
		} {
			^curval;
		};
	}

	setVal { arg key, val;
		if(val.isKindOf(Env)) {
			// if value is an Env, can't be set on a bus
			this.set(key, val)
		} {
			if(this.inBusMode(key)) {
				var bus;
				var curval;
				curval = this.get(key);
				bus = curval.asCachedBus;
				if(curval.isSequenceableCollection) {
					bus.setn(val);
				} {
					bus.set(val);
				};
				//this.changed(\set, [key, val]);
			} {
				this.set(key, val)
			};
		}
	}
}

+NodeProxy {
	inBusMode { arg key;
		var val = this.get(key);
		if(val.isSequenceableCollection) {
			// multichannel
			if(val[0].isSequenceableCollection) {
				// nested
				^(val[0][0].class == Symbol)
			} {
				^(val[0].class == Symbol)
			}
		} {
			^(val.class == Symbol or: { val.isKindOf(ParamCombinator) })
		}
	}

	asParamGroup { arg exclude;
		// TODO: add method argument (find a better name) for adding volume
		exclude = exclude ?? {[]};
		^ParamGroup(
			this.controlNames.reject({ arg con; 
				con.name == '?' or: {
					exclude.includes(con.name)
				}
			}).collect{ arg con;
				Param(this, con.name)
			}
		)
	}
}

+String {
	isNaN {
		// used to avoid NumberBox and EZ* GUI to throw an error
		^true
	}
}

/////////////////////////// 3.6.7

+Slider {
	unmapParam {
		Param.unmapView(this);
		this.value = 0;
	}

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapSlider(this);
		};
	}
}

+Knob {
	unmapParam {
		Param.unmapView(this);
		this.value = 0;
	}

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapKnob(this);
		}
	}
}

+MultiSliderView {
	unmapParam {
		Param.unmapView(this);
	}

	mapParam { arg param, trackCursor=true;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapMultiSlider(this, trackCursor:trackCursor);
		}
	}
}

+StaticText {
	unmapParam {
		Param.unmapView(this);
		{
			this.string = "-";
		}.defer;
	}

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapStaticText(this);
		}
	}

	mapParamLabel { arg param, labelmode;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapStaticTextLabel(this, labelmode);
		}
	}
}

+TextField {
	unmapParam {
		Param.unmapView(this);
		{
			this.string = "";
		}.defer;
	}

	mapParam { arg param, precision, action;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapTextField(this, precision, action);
		}
	}
}

+NumberBox {
	unmapParam {
		Param.unmapView(this);
	}

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapNumberBox(this);
		}
	}
}

+Button {
	unmapParam {
		Param.unmapView(this);
		this.value = 0;
	}

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapButton(this);
		}
	}
}

+EZKnob {
	unmapParam { arg mapLabel=true;
		Param.unmapEZKnob(this, mapLabel);
	}

	mapParam { arg param, mapLabel=true;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapEZKnob(this, mapLabel);
		}
	}
}

+EZSlider {
	unmapParam { arg mapLabel=true;
		Param.unmapEZSlider(this, mapLabel);
	}

	mapParam { arg param, mapLabel=true;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapEZSlider(this, mapLabel);
		}
	}
}

+PopUpMenu {
	unmapParam {
		Param.unmapView(this);
		this.items = [];
	}

	// map index of popupmenu to param 
	mapParam { arg param, keys;
		if(param.isNil) {
			this.unmapParam;
		} {
			param.mapPopUpMenu(this, keys);
		}
	}

	mapValueParam { arg param, keys;
		if(param.isNil) {
			this.unmapParam;
		} {
			param.mapValuePopUpMenu(this, keys);
		}
	}

	mapBusParam { arg param, keys;
		//Log(\Param).debug("mapBusParam %", keys);
		if(param.isNil) {
			this.unmapParam;
		} {
			param.mapBusPopUpMenu(this, keys);
		}
	}

	mapIndexParam { arg param, keys;
		if(param.isNil) {
			this.unmapParam;
		} {
			param.mapIndexPopUpMenu(this, keys)
		}
	}
}

+MenuAction {
	unmapParam {
		Param.unmapView(this);
		this.value = 0;
	}

	mapParam { arg param, label, action;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapMenuAction(this, label, action);
		}
	}
}

/////////////

+ View {
	// FIXME: why doesnt init ? 
	onChange { arg ...args; // deprecated name
		^this.followChange(*args)
	}

	followChange { arg model, key, fun, init=true;
		// user should free followChangeController before calling it again
		var con;
		//[model, key, fun, init].debug("model, key, fun, init");
	   	con = SimpleController.new(model).put(key, { arg ...args;
			//[model, key, fun, init].debug("update followChange");
			if(this.isClosed) {
				//[model, key, fun, init].debug("update followChange2");
				con.remove;
				Halo.lib.removeAt(this);
			} {
				//[model, key, fun, init].debug("update followChange3");
				try {
					fun.(* [this] ++ args);
				//[model, key, fun, init].debug("update followChange4");
				} { arg err;
				//[model, key, fun, init].debug("update followChange5");
					"In View.followChange: key:%".format(key).error;
					err.reportError;
				}
			};
		});
		this.addHalo(\followChangeController, con);
		this.onClose = this.onClose.addFunc({
			con.remove;
			Halo.lib.removeAt(this);
		});
				//[model, key, fun, init].debug("update followChange6");
		if(init==true) { 
				//[model, key, fun, init].debug("update followChange7");
			model.changed(key);	
		};
				//[model, key, fun, init].debug("update followChange8");
	}

	refreshChangeAction_ { arg fun;
		this.addHalo(\refreshChangeAction, fun)
	}

	refreshChangeAction {
		^this.getHalo(\refreshChangeAction)
	}

	refreshChange {
		this.getHalo(\refreshChangeAction).(this)
	}
}


/////////////////////////// 3.6.6
+QKnob {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this);
	}
}

+QSlider {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this);
	}
}

+QMultiSliderView {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapMultiSlider(this);
	}
}

+QStaticText {
	unmapParam {
		Param.unmapSlider(this);
		{
			this.string = "-";
		}.defer;
	}

	mapParam { arg param;
		param.mapStaticText(this);
	}

	mapParamLabel { arg param, labelmode;
		param.mapStaticTextLabel(this, labelmode);
	}
}

+QTextField {
	unmapParam {
		Param.unmapSlider(this);
		{
			this.value = "";
		}.defer;
	}

	mapParam { arg param;
		param.mapTextField(this);
	}
}

+QButton {
	unmapParam {
		Param.unmapView(this);
	}

	mapParam { arg param;
		param.mapButton(this);
	}
}

//XGridLayout : QGridLayout {
//	var <myStretch;
//
//	*rows { arg ...rows ;
//
//	}
//
//	stretch_ { arg val;
//		myStretch = val;
//	}
//}

/////////////////////////// END 3.6.6


+SequenceableCollection {
	asParam { arg self;
		^Param(this)
	}

	asCachedBus {
		^this.at(0).asCachedBus(this.size)
	}

	asBus {
		^this.at(0).asBus(this.size)
	}

	asBusDef {
		^this.at(0).asBusDef(this.size)
	}

	asEnv {
		var val = this.value.deepCopy;
		var first;
		var levels = List.new, times = List.new, curves = List.new;
		var releaseNode, loopNode;
		//val.debug("val");
		val = val.clump(4);
		//val.debug("val");
		first = val.removeAt(0);
		//val.debug("val");
		levels.add(first[0]);
		releaseNode = if(first[2] == -99) { nil } { first[2] };
		loopNode = if(first[3] == -99) { nil } { first[3] };
		//levels.debug("levels");
		
		val.do { arg point, x;
			levels.add( point[0] );
			times.add( point[1] );
			//FIXME: dont know how to do with shape names ???
			curves.add( point[3] );
		};
		//levels.debug("levels");
		//times.debug("times");
		//curves.debug("curves");
		^Env(levels.asArray, times.asArray, curves.asArray, releaseNode, loopNode)
	}
}

+Env {
	asEnv {
		^this
	}

	asStringPrec { arg prec;
		var args;
		args = this.storeArgs;
		args = args.collect({ arg seq; 
			if(seq.isSequenceableCollection) {
				seq.collect({ arg val; val.asFloat.asStringPrec(prec) }) 
			} {
				if(seq.notNil) {
					seq.asFloat.asStringPrec(prec)
				} {
					seq;
				}
			}
		});
		^"%(%)".format(this.class.name, args.join(", "));
	}

	size {
		// FIXME: or times because first level is init level ???
		^this.levels.size;
	}

	printOn { arg stream;
		this.storeOn(stream); // storeOn call storeArgs
	}

	// Moved to SystemOverwrites/ folder
	//storeArgs { arg stream;
		//^[levels, times, curves, releaseNode, loopNode, offset]
		////stream << ("Param.new(" ++ init_args.asCompileString ++ ")");
	//}



}



///////////////////////// dont know where to put that

+ Buffer {
	saveDialog { arg numFrames=( -1 ), startFrame=0, fun;
		Dialog.savePanel({ arg file;
			var format = PathName(file).extension;
			if(format == "") { 
				file = file ++ ".flac";
				format = "FLAC"
			};
			format = format.toUpper;
			this.write(file, format, numFrames:numFrames, startFrame:startFrame);
			fun.(file, format);
		});
	}

	asCompileString {
		if(this.getHalo(\asCompileString).notNil) { // WavetableDef support
			^this.getHalo(\asCompileString)
		} {
			if(this.key.notNil) {
				^"BufDef(%)".format(this.key.asCompileString)
			} {
				^super.asCompileString;
			}
		}
	}

	clear {
		if(this.key.notNil) {
			BufDef.clear(this.key);
		}
	}

	key {
		// used to get back the key of BufDef from a Buffer
		^this.getHalo(\key);
	}

	key_ { arg val;
		this.addHalo(\key, val)
	}

	consecutive_ { arg val; 
		// when allocating consecutives buffer in WavetableDef, this property contains the count
		this.addHalo(\consecutive, val)
	}

	consecutive { 
		^this.getHalo(\consecutive)
	}
}

+ Bus {
	key {
		// used to get back the key of BufDef from a Buffer
		^this.getHalo(\key);
	}

	key_ { arg val;
		this.addHalo(\key, val)
	}

	asCompileString {
		if(this.key.notNil) {
			^"BusDef(%)".format(this.key.asCompileString)
		} {
			^super.asCompileString;
		}
	}

}

+ List {
	sortLike { arg model;
		var ar = Array.new(this.size);
		model.do({ arg key;
			if(array.includes(key)) {
				ar = ar.add( array.remove(key) );
			}
		});
		//array.do { arg val;
		//	if(val.notNil) {
		//		ar = ar.add(val)
		//	}
		//}
		ar = ar ++ array;
		array = ar
	}
}

+ Boolean {
	blend { arg that, blendfact;
		if(blendfact < 0.5) {
			^this
		} {
			^that
		}
	}
}

