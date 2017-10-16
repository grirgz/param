WindowLayout {
	*new { arg fun;
		var window = Window.new;
		var layout;
		var val;
		val = fun.value(window);
		if(val.isKindOf(Layout)) {
			layout = val;
		} {
			layout = HLayout(val)
		};
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
	}

}

WindowDef {
	classvar <>all;
	var <>key;
	var <>source;
	var <>window;
	var <>windowProperties;
	var <>alwaysRecreate = true;
	var <>windowName;
	var <>simpleController;
	var <>startRenderingTime;


	// FIXME: window is not restored at the exact same position but is shifted downward, maybe a ubuntu unity bug
	// FIXME: if maximized then restored, the bound is lost

	*initClass {
		Class.initClassTree(PresetDictionary);
		all = PresetDictionary(\WindowDef)
	}

	*new { arg key, val;
		if(val.isKindOf(WindowDef)) {
			val = val.source;
		};
		if(key.isNil or: {all[key].isNil}) {
			if(val.notNil) {
				^super.new.init(val).prAdd(key)
			} {
				^nil
			}
		} {
			var ret = all[key];
			if(val.notNil) {
				ret.source = val
			};
			^ret;
		}
	}

	prAdd { arg xkey;
		key = xkey;
		if(xkey.notNil) {
			all[key] = this;
		}
	}

	init { arg val;
		source = val;
		windowProperties = IdentityDictionary.new;
	}

	front { arg ...args;
		this.windowize(*args);
		window.front;
	}

	frontTop { arg ...args;
		this.windowize(*args);
		window.alwaysOnTop = true;
		window.front;
	}

	saveWindowProperties {
		[\alwaysOnTop].do { arg k;
			windowProperties[k] = window.perform(k);
		}
	}

	loadWindowProperties {
		[\alwaysOnTop, \bounds].do { arg k;
			window.perform(k.asSetter, windowProperties[k]);
		};
	}
	
	isFullScreen {
		if(window.notNil) {
			^Window.availableBounds.extent == window.bounds.extent;
		} {
			^false
		}
	}

	saveBounds {
		if(window.notNil and: { this.isFullScreen.not }) {
			//FIXME: full screen protection dont work: bounds are saved wide
			windowProperties[\bounds] = window.bounds;
		}
	}

	asView { arg ...args;
		var res;
		res = source.value(this, *args);
		res.addUniqueMethod(\windowName, { this.windowName });
		^res;
	}

	updateView { arg ... args;
		var val, layout;
		if(window.notNil) {
			Task({ // defering because sometime GUI need to wait to allow processing of sound while loading
				startRenderingTime = thisThread.seconds;
				window.view.removeAll;
				val = source.value(this, *args);
				window.name = this.windowName ? key ? "";
				if(val.isKindOf(Layout)) {
					layout = val;
				} {
					layout = HLayout(val)
				};
				window.layout = layout;
			}).play(AppClock);
		}
	}

	waitIfNeeded { arg self;
		if(thisThread.clock === AppClock) {
			if(Process.elapsedTime > ( startRenderingTime + 0.01 )) {
				"WindowsDef rendering: WAITING !!!!!!".debug([ startRenderingTime, Process.elapsedTime ]);
				0.001.wait;
				startRenderingTime = Process.elapsedTime; 
			} {
				"WindowsDef rendering: no wait needed".debug([ startRenderingTime, Process.elapsedTime, Process.elapsedTime - startRenderingTime ]);
			}
		} {
			"WindowsDef rendering: no wait because not on AppClock".debug([ startRenderingTime, Process.elapsedTime, thisThread.clock ]);
		}
	}

	windowize { arg ...args;
		var layout;
		var val;
		if(alwaysRecreate == true) {
			if(window.notNil and: { window.isClosed.not }) {
				window.close;
			}
		};
		if(window.isNil or: { window.isClosed }) {
			window = Window.new;
			this.loadWindowProperties;
			this.updateView(*args);
			window.onClose = window.onClose.addFunc({
				this.saveWindowProperties;
			});
			window.view.onResize = {
				this.saveBounds;
			};
			window.view.onMove = {
				this.saveBounds;
			};
		};
		//window.alwaysOnTop = true;
	}

	onChange { arg model, key, func;
		simpleController = simpleController ?? { SimpleController(model) };
		simpleController.put(key, {
			if(this.window.isNil or: { this.window.isClosed }) {
				simpleController.remove;
			} {
				func.value;
			};
		})
	}

}


ViewProxy : SCViewHolder {
	var <>func;

	init { arg fun;
		func = fun
	}

	source_ { arg widget;
		var layout;
		// test if view or layout
		this.view.removeAll;
		if(widget.isKindOf(Layout)) {
			layout = widget;
		} {
			layout = HLayout(widget)
		};
		this.view.layout = layout;
	}

	updateView { arg ...args;
		//this.source = this.windowDef.asView(*args)
		this.source = this.func.value(*args)
	}
}
 
