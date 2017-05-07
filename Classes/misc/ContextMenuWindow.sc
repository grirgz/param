
ContextMenuWindow {
	var <>list;
	var <>action;
	classvar <>window;
	
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
	
	front { arg view, x, y, mouseButton;
		var bo = view.absoluteBounds;
		this.close;
		if(mouseButton.notNil and: { mouseButton != 1 }) {
			^this
		};
		window = Window("kkk",Rect(x+bo.origin.x,Window.screenBounds.height - view.absoluteBounds.top - y,1,1), border:false);
		//[x,y, view.absoluteBounds, view.bounds, Window.screenBounds].debug("BOUDS");
		window.endFrontAction = {
			this.close;
		};
		window.layout_(
			VLayout(
				ListView.new.items_(list).mouseDownAction_({

					//{ win.close }.defer(1);
				}).selectionAction_({ arg me;
					me.selection.debug("selection!!");
					if(me.selection.size > 0) {
						action.(this, me.selection[0]);
						this.close;
					}
				}).selection_(nil).selectionMode_(\single)
			).margins_(0).spacing_(0)
		);
		window.front;
	}
}

