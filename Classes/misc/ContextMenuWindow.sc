
ContextMenuWindow {
	var <>list;
	var <>action;
	classvar <>window;

	// new(list)
	// list: list of string/symbols as the menu
	// action type: action(contextMenuWindow, selected string)
	
	*new { arg list;
		^super.new.init(list);
	}

	init { arg xlist;
		list = xlist
	}

	close {
		if(window.notNil and: { window.isClosed.not }) {
			window.close;
		};
		window = nil;
	}

	attach { arg view, initAction;
		view.mouseUpAction = {  arg vie, x, y, modifiers, buttonNumber, clickCount;
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseUpAction");
			initAction.value(this, view);
			this.front(vie, x, y, buttonNumber)
		};
	}
	
	front { arg view, x, y, mouseButton;
		// FIXME: window width is hardcoded
		var bo = view.absoluteBounds;
		this.close;
		if(mouseButton.notNil and: { mouseButton != 1 }) {
			^this
		};
		window = Window("Context menu",Rect(x+bo.origin.x,Window.screenBounds.height - view.absoluteBounds.top - y,200,200), border:false);
		//[x,y, view.absoluteBounds, view.bounds, Window.screenBounds].debug("BOUDS");
		window.endFrontAction = {
			this.close;
		};
		window.layout_(
			VLayout(
				ListView.new.items_(list).mouseDownAction_({

					//{ win.close }.defer(1);
				}).selectionAction_({ arg me;
					//me.selection.debug("context menu selected item");
					if(me.selection.size > 0) {
						try {
							action.(this, me.selection[0]);
						} { arg ex;
							ex.reportError;
							"Exception in context menu action.".postln;
						};
						this.close;
					}
				}).selection_(nil).selectionMode_(\single)
			).margins_(0).spacing_(0)
		);
		window.front;
	}
}


/////////////////:
/*
// example


WindowDef(\popup, {
		VLayout(
			Knob.new.mouseDownAction_({ arg view, x, y, mod, mouseButton;
				var menu = MenuSpec(
					[
						"Setmode" -> { "k".debug },
						"remove" -> { "xxxk".debug },
					]
				);
				ContextMenuWindow(menu.labelList.asArray).front(view, x, y, mouseButton).action_({ arg me, idx;
					menu.valueList[idx].value

				})
			})
		)

}).front;

*/
