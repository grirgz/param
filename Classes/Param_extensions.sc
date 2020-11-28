
// PDef was replaced by EventPatternProxy as a hope to generalize it easily
+EventPatternProxy {
	*nestOn { arg val;
		// see also .bubble and .unbubble
		if(val.isSequenceableCollection) {
			if(val[0].isSequenceableCollection) {
				// NOOP
			} {
				val = [val]
			}
		};
		^val
	}

	*nestOff { arg val;
		if(val.isSequenceableCollection) {
			if(val[0].isSequenceableCollection) {
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
			^(val.class == Symbol or: { val.isKindOf(ParamCombinator) or: { this.getHalo(( \ParamCombinator_++key ).asSymbol) !? (_.inBusMode) == true } })
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
			if(val.isKindOf(Symbol)) { 
				// if the val is a bus, replace the bus
				// used to map modulator bus to parameters
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
		^this.asParamGroup.getSetCompileString;
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
		if(WindowDef(\PdefEditor).notNil) {
			^WindowDef("PdefEditor_%".format(this.key).asSymbol, WindowDef(\PdefEditor)).front(this)
		} {
			Log(\Param).info("no editor found: WindowDef(\\PdefEditor) is not defined");
			^nil
		}
	}
}

+Ndef {
	asParamGroup { 
		// TODO: add method argument (find a better name) for adding volume
		^ParamGroup(
			this.controlNames.collect{ arg con;
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
	}

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapSlider(this);
		}
	}
}

+MultiSliderView {
	unmapParam {
		Param.unmapView(this);
	}

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapMultiSlider(this);
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

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			param.mapTextField(this);
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
	// map index of popupmenu to param 
	mapParam { arg param, keys;
		param.mapPopUpMenu(this, keys);
	}

	mapValueParam { arg param, keys;
		param.mapValuePopUpMenu(this, keys);
	}

	mapBusParam { arg param, keys;
		param.mapBusPopUpMenu(this, keys)
	}

	mapIndexParam { arg param, keys;
		param.mapIndexPopUpMenu(this, keys)
	}
}

/////////////

+ View {
	// FIXME: why doesnt init ? 
	onChange { arg ...args; // deprecated name
		^this.followChange(*args)
	}

	followChange { arg model, key, fun, init=true;
		var con;
		//[model, key, fun, init].debug("model, key, fun, init");
	   	con = SimpleController.new(model).put(key, { arg ...args;
			//[model, key, fun, init].debug("update followChange");
			if(this.isClosed) {
				//[model, key, fun, init].debug("update followChange2");
				con.remove;
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
		if(this.key.notNil) {
			^"BufDef(%)".format(this.key.asCompileString)
		} {
			^super.asCompileString;
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

