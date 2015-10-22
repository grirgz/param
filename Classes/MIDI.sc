
MIDIMap {
	classvar <responders;
	classvar responders_param;
	classvar <mapped_views; // debug: added getter
	classvar midivalues;
	classvar <controls;
	classvar <>permanent = true;
	classvar <>defaultBlockmode = false;

	// old path type: [srcID, msgType, chan, msgNum]
	// current path type: [msgNum, chan, msgType, srcID]
	// MIDIdef args : [arg val, num, chan, src]
	
	*initClass {
		Class.initClassTree(MultiLevelIdentityDictionary);
		responders = MultiLevelIdentityDictionary.new;
		responders_param = MultiLevelIdentityDictionary.new;
		mapped_views = MultiLevelIdentityDictionary.new;
		midivalues = MultiLevelIdentityDictionary.new;
		controls = IdentityDictionary.new;
		//params = Dictionary.new;
	}

	*new { arg key, param, blockmode;
		var func;
		var path = this.keyToPath(key);
		var nilpath;
		if(path.isNil) {
			^nil
		};

		if(param.isNil) {
			^this.free(key);
		};

		param.class.debug("param class");
		if(param.isKindOf(PlayerWrapper)) {
			var pw = param;
			param.debug("param1 PlayerWrapper");
			param = { pw.togglePlay };
			param.debug("param PlayerWrapper");
			pw.debug("param P2layerWrapper");
		};

		if(param.class != Function) {
			param = param.asParam;
		};


		nilpath = path.collect({ arg x; if(x == \all) { nil } { x } }); // can't have nil has dict key
		//[key, path, nilpath, param].debug("key, path, nilpath, param");

		func = { arg val, num, chan, src;
			var setfun = {
				//Task({
					param.normSet(val);
				//	nil;
				//}).play(AppClock);
			};
			[key, path, nilpath, param].debug("key, path, nilpath, param");
			val = val/127;
			[val, num, chan, src].debug("val, num, chan, src");
			if(param.class == Function) {
				param.value(val, num, chan, src);
			} {
				var myblockmode = if(blockmode.isNil) {
					defaultBlockmode;
				} {
					blockmode
				};
				//myblockmode.debug("BLOCKMODE");
				if(myblockmode != true) {
					setfun.();
					midivalues.put(*path++[val]);
				} {
					var midival = midivalues.at(*path) ? 0;
					var normval = param.normGet;
					if(normval.class == BinaryOpFunction) {
						setfun.();
					} {
						//[midival, normval, val, (normval - midival).abs, (normval - midival).abs < ( 1/126 ), ( normval - midival ).abs < ( 1/110 ) ].debug("- midi, norm, val");
						//if((normval - midival).abs < ( 1/126 )) {
						if((normval - midival).abs < ( (param.spec.unmap(param.spec.step)) + ( 1/126 ) )) {
							//"NOT BLOCKED".debug;
							setfun.();
						} {
							if(( normval - midival ).abs < ( 1/110 ) ) {
								//"UNBLOCK".debug;
								setfun.();
							} {
								// do nothing because it's blocked
								//"BLOCKED".debug;
							}
						};
					};
					midivalues.put(*path++[val]);
					//midivalues.at(*path).debug("new stored midival at the end");
				};
			}
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
			//key.debug("kkKKey");
			//val.debug("kkKKeyVVVVVVVVVVVVV");
			//kind.debug("kkKKeykinddddddddddd");
			//controls[key].changed(\free_map);
			if(kind == \note) {
				kind = \noteOn
			};
			controls[key] = [val, keychannel, kind, source_uid];
		};
	}

	*pathToKey { arg path;
		var type, val, num, chan, src;
		var found;
		
		// current path type: [msgNum, chan, msgType, srcID]
		#num, chan, type, src = path;

		block { arg break;
			this.controls.keysValuesDo { arg k, path;
				var pnum, pchan, ptype, puid;
				#pnum, pchan, ptype, puid = path;
				if(type == ptype or: { ptype.isNil or: { type.isNil }}) {
					if(num == pnum or: { pnum.isNil }) {
						if(chan == pchan or: { pchan.isNil }) {
							if(puid == src or: { puid.isNil }) {
								found = k;
								break.value
							}
						}
					}
				}
			}
		};
		^found
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

	*learn { arg param, blockmode;
		var resp;
		// currently \cc only
		// MIDIFunc args : [arg val, num, chan, src]
		// current path type: [msgNum, chan, msgType, srcID]
		// FIXME: the problem is to retrieve the key name from the MIDI message
		if(param.isNil) {
			param = Param.lastTweaked;
		};

		resp = MIDIFunc.cc({ arg val, num, chan, src; 
			var key = this.pathToKey([num, chan, \control, src]);
			if(key.notNil) {
				MIDIMap(key, param, blockmode)
			};
			resp.free;
		});
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
		if(path.isNil) {
			^nil
		};
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
		if(path.isNil) {
			^nil
		};
		if(view.isNil) {
			"MIDIMap.mapStaticTextLabel: Error: view is nil".postln;
			^nil
		};
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
		if(path.isNil) {
			^nil
		};
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
