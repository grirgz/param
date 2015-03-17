
Param {
	var <wrapper;

	*new  { arg ...args;
		^super.new.init(args)
	}

	*newWrapper { arg wrap;
		^super.new.newWrapper([wrap])
	}

	init { arg args;
		if (args.size > 1) {
			this.newWrapper(args)
		} {
			this.newWrapper(args[0])
		};
	}

	newWrapper { arg args;
		var target = args[0];
		var property = args[1];
		var spec = args[2];
		switch(target.class,
			Ndef, {
				wrapper = NdefParam(*args);
			},
			Pdef, {
				switch(property.class,
					Association, {
						var idx;
						var asso = property;
						args[1] = asso.key;
						idx = asso.value;
						wrapper = PdefParamSlot(*args++[idx]);
					},
					Symbol, {
						switch(PdefParam.toSpec(spec, target, property).class.debug("deja, WTF, spec"),
							XEnvSpec, {
								//wrapper = PdefEnvParam(*args);
								wrapper = PdefParam(*args);
							}, 
							// else
							{
								wrapper = PdefParam(*args);
							}
						);
					}
				)
			//Volume, {
			//	wrapper = VolumefParam(args);
			//},
			}, {
				// ParamValue goes here
				wrapper = target;
			}
		);
	}

	== { arg param;
		( this.wrapper.property == param.wrapper.property)
		and: { this.wrapper.target == param.wrapper.target } 
	}

	asParam {
		^this
	}

	asLabel {
		^wrapper.asLabel;
	}

	type {
		^switch(this.spec.class,
			XEnvSpec, \env,
			XArraySpec, \array,
			\scalar,
		)
	}

	setBusMode { arg enable=true, free=true;
		wrapper.setBusMode(enable, free);
	}

	numChannels {
		^this.spec.numChannels;
	}

	key {
		^wrapper.key;
	}

	target {
		^wrapper.target;
	}

	property {
		^wrapper.property;
	}

	spec {
		^wrapper.spec;
	}

	at { arg idx;
		^wrapper.at(idx);
	}

	get {
		^wrapper.get;
	}

	set { arg val;
		wrapper.set(val);
	}

	normGet {
		^wrapper.normGet;
	}

	normSet { arg val;
		wrapper.normSet(val);
	}

	map { arg msgNum, chan, msgType=\control, srcID, blockmode;
		MIDIMap(this, msgNum, chan, msgType, srcID, blockmode);
	}

	unmap { arg msgNum, chan, msgType, srcID, blockmode;
		MIDIMap.free(msgNum, chan, msgType, srcID, blockmode);
	}

	mapSlider { arg slider, action;
		var controller;
		var param = this;
		controller = slider.getHalo(\simpleController);
		controller.debug("11");
		if(controller.notNil) {
			slider.addHalo(\simpleController, nil);
			debug("notnil:remove simpleController!!");
			controller.remove;
		};
		debug("11");
		if(param.notNil) {
			debug("11x");
			param = param.asParam;
			debug("11x");
			slider.action = { arg self;
				action.value(self);
				param.normSet(self.value);
				debug("action!");
			};
			debug("11x ========== CREATING!!!!!!!!!!!!");
			controller = SimpleController(param.target);
			controller.debug("11x");
			slider.addHalo(\simpleController, controller);
			controller.debug("11x");
			controller.put(\set, { arg ...args; slider.value = param.normGet.debug("controolll"); args.debug("args"); });
			slider.value = param.normGet;
			controller.debug("11x");
			slider.onClose = slider.onClose.addFunc({ controller.remove; debug("remove simpleController!!"); });
			controller.debug("11x");
		}
	}

	makeSimpleController { arg slider, action, updateAction, initAction, customAction;
		var controller;
		var param = this;
		controller = slider.getHalo(\simpleController);
		controller.debug("11");
		if(controller.notNil) {
			slider.addHalo(\simpleController, nil);
			debug("notnil:remove simpleController!!");
			controller.remove;
		};
		if(action.isNil) {
			action = { arg self;
				param.normSet(self.value);
			}
		};
		if(updateAction.isNil) {
			updateAction = { arg self;
				self.value = param.normGet.debug("controolll")
			}
		};
		if(initAction.isNil) {
			initAction = updateAction;
		};
		if(param.notNil) {
			param = param.asParam;
			slider.action = { arg self;
				action.value(self, this);
				customAction.value(self, this);
				debug("action!");
			};
			controller = SimpleController(param.target);
			slider.addHalo(\simpleController, controller);
			controller.put(\set, { arg ...args; updateAction.(slider, param) });
			initAction.(slider, param);
			slider.onClose = slider.onClose.addFunc({ controller.remove; debug("remove simpleController!!"); });
		}
	}

	mapStaticText { arg view, precision=6;
		this.makeSimpleController(view, {}, { arg view, param;
			switch(param.type,
				\scalar, {
					view.string = param.get.asFloat.asStringPrec(precision);
				},
				\array, {
					view.string = param.get.collect({ arg x; x.asFloat.asStringPrec(precision) });
				},
				\env, {
					view.string = param.get.asCompileString;
				}
			);
		}, nil, nil)
	}

	mapStaticTextLabel { arg view;
		this.makeSimpleController(view, {}, {}, { arg view, param;
			view.string = param.asLabel;
		}, nil)
	}

	mapTextField { arg view, action;
		this.makeSimpleController(view, { arg view, param;
			param.set(view.value.asFloat.debug("set tfif"));
		}, { arg view, param;
			view.value = param.get;
		}, nil, action)
	}

	mapButton { arg view, action;
		this.makeSimpleController(view, { arg view, param;
			var size;
			size = view.states.size;
			param.normSet(view.value.linlin(0,size-1,0,1));
		}, { arg view, param;
			var size;
			size = view.states.size;
			view.value = param.normGet.linlin(0,1,0,size-1);
		}, nil, action)
	}

	*unmapView { arg view;
		var controller;
		controller = view.getHalo(\simpleController);
		view.action = nil;
		if(controller.notNil) {
			view.addHalo(\simpleController, nil);
			controller.remove;
		};
	}

	*unmapSlider { arg slider;
		Param.unmapView(slider)
	}

	asSlider {
		^Slider.new.mapParam(this);
	}

	asKnob {
		^Knob.new.mapParam(this);
	}

	asMultiSlider {
		^MultiSliderView.new.elasticMode_(1).size_(this.numChannels).mapParam(this);
	}

	asStaticText {
		^StaticText.new.mapParam(this);
	}

	asStaticTextLabel {
		^StaticText.new.mapParamLabel(this);
	}

	asEnvelopeView {
		var view;
		view = XEnvelopeView.new(nil, Rect(0, 0, 230, 80))
			.drawLines_(true)
			.selectionColor_(Color.red)
			.drawRects_(true)
			.step_(0)
			.thumbSize_(10)
			.elasticSelection_(true)
			.keepHorizontalOrder_(true)
			.mapParam(this)
			.grid_(Point(this.spec.times[0].unmap(1/8),1/8))
			.totalDur_(this.spec.times[0].unmap(2))
			.gridOn_(true);

		^view;
	}

	asView {
		case(
			{ this.spec.class == XEnvSpec }, {
				^this.asEnvelopeView;
			},
			{ this.spec.class == XArraySpec }, {
				^this.asMultiSlider;
			}, {
				^this.asKnob;
			}
		)

	}

	edit {
		var win;
		var label, widget, val;
		win = Window.new;

		label = this.asStaticTextLabel;
		label.align = \center;

		widget = this.asView;

		val = this.asStaticText;
		//val.background = Color.red;
		val.align = \center;

		win.layout = VLayout.new(
			label, 
			widget, 
			val
		);
		win.alwaysOnTop = true;
		win.front;
	}

	*toSpec { arg spec, argName, default_spec=\widefreq;
		if(spec.isNil, {
			if( argName.asSpec.notNil, {
				spec = argName.asSpec;
			}, {
				spec = default_spec.asSpec;
			});
		});
		^spec
	}

	*toSynthDefSpec { arg spec, argName, defname=nil, default_spec=\widefreq;
		if(spec.isNil) {
			var val;
			var rval;
			val = SynthDescLib.global.synthDescs[defname];
			if(val.notNil) {
				val = val.metadata;
				if(val.notNil) {
					val = val.specs;
					if(val.notNil) {
						rval = val[argName];
					}
				} {
					// no metadata but maybe a default value
					var def = Param.getSynthDefDefaultValue(argName, defname);
					if(def.class == Float) {
						rval = default_spec;
					} {
						if(def.class.isSequenceableCollection) {
							rval = XArraySpec(default_spec!def.size);
						}
					};
				};
			};
			rval = rval.asSpec;
			spec = this.toSpec(spec, argName, default_spec);
		};
		^spec;
	}

	*getSynthDefDefaultValue { arg argName, defname;
		var desc;
		var val;
		"getSynthDefDefaultValue 1".debug;
		desc = SynthDescLib.global.synthDescs[defname];
		desc.debug("getSynthDefDefaultValue 2");
		val = if(desc.notNil) {
			var con = desc.controlDict[argName];
		con.debug("getSynthDefDefaultValue 4");
			if(con.notNil) {
		"getSynthDefDefaultValue 5".debug;
				con.defaultValue;
			}
		};
		^val;
	}

}


BaseParam {

}

NdefParam : BaseParam {
	var <target, <property, <spec, <key;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	asLabel {
		^"Ndef % %".format(target.key, property)
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		sp.debug("sp1");
		spec = this.toSpec(sp);
		key = obj.key;
	}

	// retrieve default spec if no default spec given
	toSpec { arg sp;
		sp.debug("sp2");
		sp = sp ? target.getSpec(property);
		sp.debug("sp3");
		sp = Param.toSpec(sp, property);
		sp.debug("sp4");
		^sp.asSpec;
	}

	get {
		var val;
		val = target.get(property);
		if(spec.class == XEnvSpec) {
			val = val.asEnv;
		};
		^val;
	}

	set { arg val;
		target.set(property, val);
		//target.changed(\set, property); // already exists in Ndef
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}
}

PdefParam : BaseParam {
	var <target, <property, <spec, <key;
	var <multiParam = false;
	*new { arg obj, meth, sp;
		^super.new.init(obj, meth, sp);
	}

	init { arg obj, meth, sp;
		target = obj;
		property = meth;
		spec = this.toSpec(sp);
		key = obj.key;
		if(spec.isKindOf(XArraySpec)) {
			multiParam = true;
		};
	}

	asLabel {
		^"Pdef % %".format(target.key, property)
	}

	*instrument { arg target;
		var val;
		val = target.getHalo(\instrument) ?? { 
			var inval = target.source;
			if(inval.notNil) {
				inval = inval.patternpairs.clump(2).detect { arg pair;
					pair[0] == \instrument
				};
				if(inval.notNil) {
					inval = inval[1];
					if(inval.class == Symbol) {
						inval;
					} {
						nil
					}
				} {
					nil
				}
			} {
				nil
			};
		};
		^val;
	}

	instrument { 
		^PdefParam.instrument(target)
	}

	// retrieve default spec if no default spec given
	*toSpec { arg xspec, xtarget, xproperty;
		var instr = PdefParam.instrument(xtarget);
		xspec = Param.toSynthDefSpec(xspec, xproperty, instr);
		^xspec.asSpec;
	}

	toSpec { arg spec;
		^this.class.toSpec(spec, target, property)
	}
	
	at { arg idx;
		^Param(target, property -> idx, spec)
	}

	setBusMode { arg enable=true, free=true;
		target.setBusMode(property, enable, free);
	}

	inBusMode {
		^target.inBusMode(property)
	}

	get {
		var val = target.getVal(property);
		if(val.isNil) {
			var instr = this.instrument;
			val = Param.getSynthDefDefaultValue(property, instr) ?? { spec.default };
			if(spec.class == XEnvSpec) {
				val = val.asEnv;
			};
		};
		^val;
	}

	getRaw {
		^target.get(property)
	}

	set { arg val;
		target.setVal(property, val);
	}

	setRaw { arg val;
		target.set(property, val);
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}
}

PdefParamSlot : PdefParam {
	var <index;

	*new { arg obj, meth, sp, index;
		var inst;
		obj.debug("obj");
		inst = super.new(obj, meth, sp);
		inst.pdefParamSlotInit(index);
		^inst;
	}

	asLabel {
		^"Pdef % % %".format(target.key, this.property, index)
	}

	pdefParamSlotInit { arg idx;
		index = idx;
	}

	spec {
		spec.at(index);
	}

	set { arg val;
		var vals = target.getVal(property);
		vals[index] = val;
		target.setVal(property, vals);
	}

	get {
		var vals = target.getVal(property);
		^vals[index];
	}
}

PdefEnvParam : PdefParam {
	// not used currently
	var <target, <property, <spec, <key;
	var <multiParam = false;
	*new { arg obj, meth, sp;
		^super.new(obj, meth, sp).pdefEnvParamInit(obj, meth, sp);
	}

	pdefEnvParamInit { arg obj, meth, sp;
		target = obj;
		property = meth;
		spec = this.toSpec(sp);
		key = obj.key;
		multiParam = true;
	}

	// retrieve default spec if no default spec given
	toSpec { arg spec;
		var instr = target.getHalo(\instrument);
		spec = Param.toSynthDefSpec(spec, property, instr);
		^spec.asSpec;
	}

	*toSpec { arg xspec, xtarget, xproperty;
		var instr = xtarget.getHalo(\instrument);
		xspec = Param.toSynthDefSpec(xspec, xproperty, instr);
		^xspec.asSpec;
	}
	
	at { arg idx;
		^Param(target, property -> idx, spec)
	}

	setBusMode { arg enable=true, free=true;
		target.setBusMode(property, enable, free);
	}

	inBusMode {
		^target.inBusMode(property)
	}

	get {
		^target.getVal(property)
	}

	getRaw {
		^target.get(property)
	}

	set { arg val;
		target.setVal(property, val);
	}

	setRaw { arg val;
		target.set(property, val);
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}

}

////////////////////////////////////////



MIDIMap {
	classvar responders;
	classvar responders_param;
	classvar <mapped_views; // debug: added getter
	classvar midivalues;
	classvar <controls;
	classvar <>permanent = true;

	// path type: [srcID, msgType, chan, msgNum]
	
	*initClass {
		responders = MultiLevelIdentityDictionary.new;
		responders_param = MultiLevelIdentityDictionary.new;
		mapped_views = MultiLevelIdentityDictionary.new;
		midivalues = MultiLevelIdentityDictionary.new;
		controls = IdentityDictionary.new;
		//params = Dictionary.new;
	}

	*define { arg channel, defs;
		var source_uid = nil;
		if(channel.isSequenceableCollection) {
			source_uid = channel[1];
			channel = channel[0]
		};
		defs.pairsDo { arg key, val;
			var kind=\control, keychannel;
			if(val.class == Association) {
				kind = val.key;
				val = val.value;
			};
			if(val.isSequenceableCollection) {
				keychannel = val[1];
				val = val[0]
			} {
				keychannel = channel;
			};
			key.debug("kkKKey");
			val.debug("kkKKeyVVVVVVVVVVVVV");
			kind.debug("kkKKeykinddddddddddd");
			//controls[key].changed(\free_map);
			if(kind == \note) {
				kind = \noteOn
			};
			controls[key] = [val, channel, kind, source_uid];
		};
	}

	*keyToPath { arg key;
		if(key.class == Symbol) {
			var path = controls[key];
			if(path.isNil) {
				"Error: no key named % in MIDIMap".format(key).postln;
				^nil
			} {
				^this.normalizePath(path)
			}
		} {
			^this.normalizePath(key)
		}
	}

	*normalizePath { arg path;
		path = path.extend(4,nil);
		if(path[2] == nil or: {path[2] == \all}) { // default msgType is \control
			path[2] = \control;
		};
		path = path.collect({ arg x; if(x.isNil) { \all } { x } });
		^path;
	}

	*new { arg key, param, blockmode;
		var func;
		var path = this.keyToPath(key);
		var nilpath;
		nilpath = path.collect({ arg x; if(x == \all) { nil } { x } }); // can't have nil has dict key
		[key, path, nilpath, param].debug("key, path, nilpath, param");

		func = { arg val, num, chan, src;
			[key, path, nilpath, param].debug("key, path, nilpath, param");
			val = val/127;
			[val, num, chan, src].debug("key, path, nilpath, param");
			if(blockmode.isNil) {
				Task({
					param.normSet(val);
					nil;
				}).play(AppClock);
				midivalues.put(*path++[val]);
			};
		};

		if(responders.at(*path).notNil) {
			responders.at(*path).free
		};
		responders.put(*path ++ [
			MIDIFunc(func, nilpath[0], nilpath[1], nilpath[2], nilpath[3]).permanent_(permanent)
			//params[param] =	params[param].add( path );
		]);
		responders_param.put(*path ++ [ param ]);
		this.changed(\midimap, path, param);
		this.updateViews(path, param);
	}

	*unmap { arg param;
		// TODO
		//params[param]
	}
	
	*free { arg key;
		var path = this.keyToPath(key);
		responders.at(*path).free;
		responders_param.put(*path ++ [nil]);
		this.changed(\midimap, path, nil);
		this.updateViews(path);
	}

	*get { arg key;
		var path = this.keyToPath(key);
		responders.at(*path)
	}
	
	*freeAll {
		responders.leafDo { arg path, resp;
			this.free(path);
		};
	}

	*learn {
		arg key, param, blockmode;
	}

	*updateViews { arg path, param;
		var to_remove = List.new;
		mapped_views.at(*path).do { arg view, x;
			var kind = view[1];
			view = view[0];
			if(view.isClosed) {
				to_remove.add(x)
			} {
				if(param.notNil) {
					if(kind == \label) {
						view.mapParamLabel(param)
					} {
						view.mapParam(param)
					}
				} {
					view.unmapParam;
				}
			}
		};
		to_remove.reverse.do { arg x;
			mapped_views.at(*path).removeAt(x)
		};
	}

	*mapView { arg key, view;
		var path = this.keyToPath(key);
		if(mapped_views.at(*path).isNil) {
			mapped_views.put(*path ++ [ List.new ])
		};
		mapped_views.at(*path).add([view]);
		this.updateViews(path, responders_param.at(*path));
	}

	*mapSlider { arg key, slider;
		^mapView(key, slider);
	}

	*mapStaticTextLabel { arg key, view;
		var path = this.keyToPath(key);
		if(mapped_views.at(*path).isNil) {
			mapped_views.put(*path ++ [ List.new ])
		};
		mapped_views.at(*path).add([view, \label]);
		this.updateViews(path, responders_param.at(*path));
	}

	*unmapView { arg key, view;
		// TODO: add code to handle key=nil: search in all paths
		var list;
		var path = this.keyToPath(key);
		if(mapped_views.at(*path).isNil) {
			mapped_views.put(*path ++ [ List.new ])
		};
		list = mapped_views.at(*path);
		list.reverse.do { arg vi, x;
			vi = vi[0]; // [1] is view type
			if(view === vi) {
				list.removeAt(list.size - 1 - x)
			}
		}
	}

}

////////////////////////////////////////

ParamGroup : List {
	var <>presets;
	var <>morphers;

	// morphers format : Dict[ \name -> (val: 0.5, presets:[\preset1, \preset2]) ]
	// - morphTo(\name, 0.3)
	// - addMorphing(\name)
	// - morphTo([\preset1, \preset2], 0.4)
	*new { arg anArray;
		var inst;
		inst = super.new.setCollection( anArray.collect(_.asParam) );
		inst.initParamGroup;
		^inst;
	}

	initParamGroup {
		presets = IdentityDictionary.new;
	}

	save { arg key=\default; 
		presets[key] = super.array.collect { arg param;
			param.get;
		}
	}

	getPreset { arg key=\default;
		^presets[key]
	}

	valueList {
		^this.collect { arg param;
			param.get;
		}
	}

	erase { arg key=\default;
		presets[key] = nil;
	}

	load { arg key=\default; 
		if(presets[key].notNil) {
			presets[key].do { arg val, x;
				super.array[x].set(val)
			}
		}
	}

	edit {
		var win;
		var hlayout = HLayout.new;
		win = Window.new;

		this.collect({ arg param;
			var label, widget, val;

			label = param.asStaticTextLabel;
			label.align = \center;

			widget = param.asView;

			val = param.asStaticText;
			//val.background = Color.red;
			val.align = \center;

			hlayout.add(VLayout.new(
				label, 
				widget, 
				val
			), stretch:1);
		});

		win.layout = hlayout;
		win.alwaysOnTop = true;
		win.front;
	}

}

ParamPreset {
	classvar <lib;
	var <libkey;
	var <group;

	*initClass {
		lib = IdentityDictionary.new
	}

	*new { arg defkey, group;
		var inst;
		if(group.isNil) {
			^lib[defkey]
		} {
			if(lib[defkey].isNil) {
				inst = super.new.init(defkey, group);
				lib[defkey] = inst;
				^inst
			} {
				"Warning: already defined, use .clear before redefine it".postln;
				^lib[defkey]
			}
		}
	}

	init { arg defkey, xgroup;
		//xgroup.debug("hhhhhhhhhh");
		libkey = defkey;
		group = ParamGroup(xgroup);
		if(Archive.global.at(\ParamPreset, libkey).isNil) {
			this.saveArchive;
		} {
			this.loadArchive;
		};
	}

	getPreset { arg key=\default;
		^group.getPreset(key);
	}

	valueList {
		^group.valueList;
	}

	saveArchive {
		var archive = IdentityDictionary.new;
		archive[\presets] = group.presets;
		archive[\morphers] = group.morphers;
		Archive.global.put(\ParamPreset, libkey, archive);
	}

	loadArchive {
		var archive;
		archive = this.getArchive;
		group.presets = archive[\presets];
		group.morphers = archive[\presets];
	}

	getArchive {
		^Archive.global.at(\ParamPreset, libkey);
	}


	clear {
		Archive.global.put(\ParamPreset, libkey, nil);
		lib[libkey] = nil;
	}

	save { arg key;
		group.save(key);
		this.saveArchive;
	}

	load { arg key;
		group.presets[key] = this.getArchive[\presets][key];
		group.load(key);
	}

	erase { arg key;
		group.erase(key);
		this.saveArchive;
	}

	do { arg fun;
		group.do(fun)
	}

	collect { arg fun;
		^group.collect(fun)
	}

	at { arg x;
		^group[x]
	}

	edit {
		group.edit;
	}

}

// act also as a Param wrapper
ParamValue {
	var <>value=0;
	var <>spec, <property=\value, <target;

	*new { arg spec;
		^super.new.init(spec);
	}

	init { arg spec;
		target = this;
		property = \value;
		spec = spec;
	}

	get {
		^value;
	}

	set { arg val;
		value = val;
	}

	normGet {
		^spec.unmap(this.get)
	}

	normSet { arg val;
		this.set(spec.map(val))
	}
}

// act also as a Param
ParamMorpher : Param {
	var group, presets;
	var <>key;
	*new { arg group, presets;
		var pval = ParamValue.new;
		var inst;
		pval.spec = ControlSpec(0,presets.size-1,\lin,0,0);
		inst = super.newWrapper(pval, presets);
		inst.initParamMorpher(group, presets);
		^inst;
	}

	initParamMorpher { arg arggroup, argpresets;
		group = arggroup;
		presets = argpresets;
		[group, presets].debug("initParamMorpher");
	}

	morph { arg list, morph;
		^list.blendAt(morph)
	}

	asLabel {
		// TODO
		"morph"
	}

	set { arg val;
		var presets_vals;
		val.debug("ParamMorpher: set");
		this.wrapper.set(val);
		presets_vals = presets.collect({ arg x; 
			group.getPreset(x)
		});
		[presets_vals, presets].debug("presets");
		presets_vals = presets_vals.flop;
		group.do({ arg param, x;
			var resval;
			resval = this.morph(presets_vals[x], val);
			[param.asLabel, val].debug("ParamMorpher: param set");
			param.set(resval);
		})
	}

	get {
		^this.wrapper.get;
	}
}

ParamMorpherDef : ParamMorpher {
	classvar lib;
	*new { arg defkey, group, presets;
		var inst;
		if(group.isNil) {
			^lib[defkey]
		} {
			if(lib[defkey].isNil) {
				inst = super.new.init(group, presets);
				inst.key = defkey;
				lib[defkey] = inst;
				^inst
			} {
				"Warning: already defined, use .clear before redefine it".postln;
				^lib[defkey]
			}
		}
	}

	clear {
		lib[this.key] = nil
	}
}

////////////////////////////////////////

CachedBus : Bus {
	classvar cache;

	*initClass {
		cache = IdentityDictionary.new;
		cache[\audio] = IdentityDictionary.new;
		cache[\control] = IdentityDictionary.new;
	}

	set { arg ... values;
		var val;
		if(values.size == 1) {
			val = values[0];
			super.set(val);
		} {
			val = values;
			super.setn(val);
		};
		cache[this.rate][this.index] = val;
	}

	setn { arg values;
		this.set(*values)
	}

	getCached {
		if(cache[this.rate][this.index].isNil) {
			this.get({ arg x; cache[this.rate][this.index] = x });
			^0
		} {
			^cache[this.rate][this.index]
		}
	}

}

XEnvelopeView : QEnvelopeView {
	var curves;
	var <timeScale = 1;
	var duration;
	var envDur;
	var rawValue;
	var rawGrid;
	var autoTimeScale = true;
	var <totalDur = 8;

	curves {
		^curves
	}

	curves_ { arg xcurves;
		curves.debug("curves::");
		curves = xcurves;
		super.curves = xcurves;
	}

	valueXY_ { arg val;
		val = val.deepCopy;
		envDur = val[0].last;
		if(envDur > totalDur) {
			totalDur = envDur;
		};
		val[0] = val[0] / totalDur;
		super.value = val;
		this.updateGrid;
	}

	valueXY { 
		var val = super.value.deepCopy; // deepCopy to avoid messing with internal value
		val[0] = val[0] * totalDur;
		^val;
	}

	value {
		^this.getEnv;
	}

	value_ { arg val;
		this.setEnv(val);
	}

	zoomFit {
		var val = this.valueXY;
		envDur = val[0].last;
		this.totalDur = envDur;
	}

	totalDur_ { arg newdur;
		// FIXME: if valueXY is nil, crash
		var curval = this.valueXY.deepCopy;
		totalDur = newdur;
		this.valueXY = curval;
	}

	setEnv { arg env;
		var times = [0] ++ env.times.integrate;
		this.valueXY = [times, env.levels];
		this.curves = env.curves;
	}

	grid_ { arg val;
		rawGrid = val;
		this.updateGrid;
	}

	grid {
		^rawGrid;
	}

	updateGrid {
		rawGrid = rawGrid ? Point(1/8,1/8);
		super.grid = Point( rawGrid.x / totalDur,rawGrid.y).debug("grid")
	}

	getEnv { arg val;
		var curves;
		var times;
		var levels;
		var env;
		if(val.isNil) {
			val = this.valueXY;
		};
		times = val[0];
		times = times.drop(1);
		times = times.differentiate;
		levels = val[1];
		curves = this.curves;
		env = Env.new(levels, times, curves);
		^env
	}

	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this, { arg self;
			var val = self.valueXY;
			if( val[0][0] != 0) {
				val[0][0] = 0;
				self.valueXY = val;
			};
		});
	}
}

XStaticText : QStaticText {
	value {
		this.string;
	}

	value_ { arg val;
		this.string = val
	}
}

XSimpleButton : QButton {
	var <color, <label, <background, myValue;

	color_ { arg val;
		color = val;
		this.updateStates;
	}

	background_ { arg val;
		background = val;
		this.updateStates;
	}

	label_ { arg val;
		label = val;
		this.updateStates;
	}

	value_ { arg val;
		myValue = val;
	}

	value { arg val;
		^myValue;
	}

	updateStates {
		this.states = [[label, color, background]];
	}

}

//////////////////////////////////

+Symbol {
	asBus { arg numChannels=1, busclass;
		var rate;
		var index;
		var map;
		busclass = busclass ? Bus;
		map = this;
		map = map.asString;
		switch(map[0],
			$c, {
				rate = \control;
			},
			$a, {
				rate = \audio;
			}, {
				map.debug("get_bus_from_map: error, not a bus");
			}
		);
		index = map[1..].asInteger;
		^busclass.new(rate, index, numChannels, Server.default);
	}

	asCachedBus { arg numChannels=1;
		^this.asBus(numChannels, CachedBus);
	}

}

+Pdef {
	nestOn { arg val;
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

	nestOff { arg val;
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
		"whatktktj".debug;
		if(enable) {
			"1whatktktj".debug;
			if(this.inBusMode(key)) {
				// NOOP
				"2whatktktj".debug;
			} {
				var val = this.getVal(key);
				var numChannels = 1;
				var bus;
				"3whatktktj".debug;
				val.debug("setBusMode: val");
				if(val.isSequenceableCollection) {
					numChannels = val.size;
				};
				numChannels.debug("setBusMode: nc");
				bus = CachedBus.control(Server.default,numChannels );
					// FIXME: hardcoded server
					// hardcoded rate, but can't convert audio buffer to a number, so it's ok
				bus.debug("setBusMode: bus");
				if(val.isSequenceableCollection) {
					bus.setn(val);
				} {
					bus.set(val);
				};
				val.debug("setBusMode: val");
				this.set(key, this.nestOn(bus.asMap));
				bus.asMap.debug("setBusMode: busmap");
			}
		} {
			if(this.inBusMode(key).not) {
				// NOOP
			} {
				var val = this.getVal(key);
				var map = this.get(key);
				var numChannels = 1;
				var bus;
				map = this.nestOff(map);
				bus = map.asCachedBus;
				this.set(key, this.nestOn(val));
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
			^(val.class == Symbol)
		}
	}

	getVal { arg key;
		var curval;
		curval = this.get(key);
		curval = this.nestOff(curval);
		if(this.inBusMode(key)) {
			var bus = curval.asCachedBus;
			^bus.getCached;
		} {
			^curval;
		};
	}

	setVal { arg key, val;
		if(val.isKindOf(Env)) {
			this.set(key, this.nestOn(val))
		} {
			if(this.inBusMode(key)) {
				var bus;
				var curval;
				curval = this.get(key);
				curval = this.nestOff(curval);
				bus = curval.asCachedBus;
				if(curval.isSequenceableCollection) {
					bus.setn(val);
				} {
					bus.set(val);
				}
			} {
				this.set(key, this.nestOn(val))
			};
		}
	}
}

/////////////////////////// 3.6.7

+Slider {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this);
	}
}

+Knob {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this);
	}
}

+MultiSliderView {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapSlider(this);
	}
}

+StaticText {
	unmapParam {
		Param.unmapSlider(this);
		this.string = "-";
	}

	mapParam { arg param;
		param.mapStaticText(this);
	}

	mapParamLabel { arg param;
		param.mapStaticTextLabel(this);
	}
}

+TextField {
	unmapParam {
		Param.unmapSlider(this);
	}

	mapParam { arg param;
		param.mapTextField(this);
	}
}

+Button {
	unmapParam {
		Param.unmapView(this);
	}

	mapParam { arg param;
		param.mapButton(this);
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
		param.mapSlider(this);
	}
}

+QStaticText {
	unmapParam {
		Param.unmapSlider(this);
		this.string = "-";
	}

	mapParam { arg param;
		param.mapStaticText(this);
	}

	mapParamLabel { arg param;
		param.mapStaticTextLabel(this);
	}
}

+QTextField {
	unmapParam {
		Param.unmapSlider(this);
		this.value = "";
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

	asEnv {
		var val = this.value.deepCopy;
		var first;
		var levels = List.new, times = List.new, curves = List.new;
		var releaseNode, loopNode;
		val.debug("val");
		val = val.clump(4);
		val.debug("val");
		first = val.removeAt(0);
		val.debug("val");
		levels.add(first[0]);
		releaseNode = if(first[2] == -99) { nil } { first[2] };
		loopNode = if(first[3] == -99) { nil } { first[3] };
		levels.debug("levels");
		
		val.do { arg point, x;
			levels.add( point[0] );
			times.add( point[1] );
			//FIXME: dont know how to do with shape names ???
			curves.add( point[3] );
		};
		levels.debug("levels");
		times.debug("times");
		curves.debug("curves");
		^Env(levels, times, curves, releaseNode, loopNode)
	}
}

+Bus {
	asMap {
		^mapSymbol ?? {
			if(index.isNil) { MethodError("bus not allocated.", this).throw };
			mapSymbol = if(rate == \control) { "c" } { "a" };
			if(this.numChannels > 1) {
				mapSymbol = numChannels.collect({ arg x;
					(mapSymbol ++ ( index + x)).asSymbol;
				})
			} {
				mapSymbol = (mapSymbol ++ index).asSymbol;
			}
		}
	}
}

+Env {
	asEnv {
		^this
	}
}
