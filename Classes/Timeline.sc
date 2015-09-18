
TimelineRulerView : TimelineView {
	// this is X ruler actually
	var <>mygrid; // debug
	var <>cursor;

	mapCursor { arg curs;
		cursor = curs;
		this.view.mouseDownAction = { arg me, px, py, mod, buttonNumber, clickCount, chosennode;
			px.debug("TimelineLocatorBarView: mousedown set start");
			//if(chosennode.isNil) 
			cursor.startPosition_(this.pixelPointToGridPoint(Point(px, 0)).x.round(quant.value.x));
			mouseDownAction.(me, px, py, mod, buttonNumber, clickCount);
		}
	}


	//*new { arg w, bounds; 
	//	^super.new.initTimelineRulerView(w, bounds);
	//}

	//mimicTimeline { arg timeline;
	//	if(timeline_controller.notNil) {timeline_controller.remove};
	//	timeline_controller = SimpleController(timeline).put(\viewport, {
	//		viewport = timeline.viewport;
	//		this.view.refresh;
	//	});
	//}


	//*newFromEventList { arg eventlist, w, bounds;
	//	^super.new.initParaSpace(w, bounds).mapEventList(eventlist);
	//}


	specialInit { 
		this.view.mouseDownAction = nil;
		this.view.mouseMoveAction = nil;
		this.view.mouseUpAction = nil;
		this.view.drawFunc = { this.drawFunc };
	}

	//bounds {
	//	^this.view.bounds;
	//}

	drawFunc {
		var grid;
		var bounds = this.bounds;
		if(mygrid.notNil) {
			mygrid.(this)
		} {
			DrawGrid(
				//Rect(0 - (viewport.origin.x * bounds.width / viewport.width),0 - (viewport.origin.y * bounds.height / viewport.height), bounds.width / viewport.width, bounds.height / viewport.height),

				// x only
				Rect(0 - (viewport.origin.x * bounds.width / viewport.width),0, bounds.width / viewport.width, bounds.height),
				DenseGridLines(ControlSpec(
						0,
						areasize.x,
						\lin,
						0,
						0
				)).density_(1),
				nil
			).draw;
		};
	}
}

TimelineLocatorBarView : TimelineView {

	specialInit {
		"SPECIAL INIT".debug;
		this.view.focusLostAction = {
			"FOCUS LOST".debug;
			this.deselectAllNodes;
			this.refresh;
		};
	}

	nodeClass {
		^TimelineViewLocatorNode
	}

	eventFactory { arg pos;
		if(eventFactory.isNil) {
			var nodesize = Point(1,1);
			nodesize = this.gridPointToNormPoint(nodesize);
			^(absTime: pos.x, label: "unnamed", type:\locator);
		} {
			^eventFactory.(pos);
		}
	}


	addEvent { arg event;
		var node;
		switch(event[\type],
			\locator, {
				^this.addEventRaw(event);
			},
			\start, {
				^this.addEventRaw(event);
			},
			\end, {
				^this.addEventRaw(event);
			},
			// else
			{
				// ignore the rest
				^nil
			}
		)
	}

	keyDownActionBase { |me, key, modifiers, unicode, keycode |

		key.debug("TimelineLocatorBarView: keyDownActionBase");
		// edit
		if(key == $e) {
			if(chosennode.notNil){
				TimelineLocatorPropertiesView.new(chosennode.model);
			}
		};

		super.keyDownActionBase(me, key, modifiers, unicode, keycode);
	}

	drawFunc {
		//var bounds = this.view.bounds;
		var pen = Pen;
		var bounds = this.virtualBounds;
		var pstartSelPoint, pendSelPoint;
		var grid;

		pen.width = 1;
		pen.color = background; // background color
		backgrDrawFunc.value; // background draw function


		this.drawNodes;
		

	}
}

TimelineViewLocatorNode : TimelineViewEventNode {

	var label;
	var labelKey = \label;
	var width = 8;
	var height = 10;

	init { arg xparent, nodeidx, event;
		parent = xparent;
		spritenum = nodeidx;
		model = event;

		[spritenum, model].debug(this.class.debug("CREATE EVENT NODE !"));

		action = {
			//[model, origin].debug("node action before");
			model[timeKey] = origin.x;
			model[labelKey] = label;
			//model[lenKey] = extent.x;
		};

		refresh = {
			"TimelineViewLocatorLineNode: refresh: 1".debug;
			origin = Point(model[timeKey], 0);
			"TimelineViewLocatorLineNode: refresh: 2".debug;
			color = Color.black;
			"TimelineViewLocatorLineNode: refresh: 3".debug;
			label = model[labelKey] ? (model[\type] ? "unnamed");
			"TimelineViewLocatorLineNode: refresh: 4".debug;
			[parent.viewport, parent.areasize, Point(width,height)].debug("parent vi, are, size");
			extent = parent.pixelPointToGridPoint(Point(width,height)); //FIXME: why /2 ???
			"TimelineViewLocatorLineNode: refresh: 5".debug;
			//extent.debug("---------extent");
			//extent = Point(model.use { currentEnvironment[lenKey].value(model) }, 1); // * tempo ?
			parent.model.changed(\redraw);
			"TimelineViewLocatorLineNode: refresh: 6".debug;
			//[this.class, spritenum, origin, extent, color].debug("refresh");
		};

		this.makeUpdater;
		this.refresh;
		this.action;
	}

	draw {
		var point;
		var pos;
		var len=width;
		var box = {
			var middle = height*3/4;
			Pen.moveTo(point);
			Pen.lineTo(point + Point(len,0));
			Pen.lineTo(point + Point(len,middle));
			Pen.lineTo(point + Point(len/2,height));
			Pen.lineTo(point + Point(0,middle));
			Pen.lineTo(point + Point(0,0));

			//Pen.lineTo(point + Point(len,0));
			//Pen.lineTo(point + Point(len,middle));
			//Pen.lineTo(point + Point(len/2+1,middle));
			//Pen.lineTo(point + Point(len/2,height));
			//Pen.lineTo(point + Point(len/2-1,middle));
			//Pen.lineTo(point + Point(0,middle));
			//Pen.lineTo(point + Point(0,0));
		};

		// FIXME: this lines are here because I need to freeze size and ypos
		extent = parent.pixelPointToGridPoint(Point(width,height)); 
		origin = Point(model[timeKey], 0);

		pos = this.origin;
		point = Point(parent.gridPointToPixelPoint(pos).x, 1);
		point = point - Point(len/2, 0);
		[spritenum, point, this.rect, parent.gridRectToPixelRect(this.rect)].debug("draw");


		box.();
		Pen.color = Color.green;
		Pen.fill;

		box.();
		Pen.color = outlineColor;
		Pen.stroke;
		Pen.stringAtPoint(" "+label, Point(point.x+len,point.y+1),Font('sans', 7));

		// debug collision rect
		//Pen.color = Color.red;
		//Pen.fillRect(parent.gridRectToPixelRect(this.rect));

	}

	rect {
		var rect;
		var point = this.origin;
		extent = parent.pixelExtentToGridExtent(Point(width,height)); 
		rect = Rect(point.x-(extent.x/2), 0, extent.x, extent.y*4);
		^rect;
	}

	selectNode {
		this.refloc = this.nodeloc;
		outlineColor = Color.red;
	}

	deselectNode {
		outlineColor = Color.black;
	}

}

// TODO: replace hardcoded line with node
// FIXME: how to be never selected by findNode ?
TimelineViewLocatorLineNode : TimelineViewEventNode {
	var <>alpha = 0.5;


	init { arg xparent, nodeidx, event;
		parent = xparent;
		spritenum = nodeidx;
		model = event;

		selectable = false;
		color = Color.red;

		//[spritenum, model].debug(this.class.debug("CREATE EVENT NODE !"));

		action = {
			//[model, origin].debug("node action before");
			model[timeKey] = origin.x;
			//model[lenKey] = extent.x;
		};

		refresh = {
			origin = Point(model[timeKey], 0);
			extent = Point(1,1);
			//[this.class, spritenum, origin, extent, color].debug("refresh");
		};

		this.makeUpdater;
		this.refresh;
		this.action;
	}

	draw {
		var point;
		Pen.use {

			Pen.color = Color.black;
			Pen.alpha = alpha;
			point = parent.gridPointToPixelPoint(this.origin);
			// now in screen coordinates
			Pen.line(Point(point.x, this.parent.virtualBounds.origin.y), Point(point.x, parent.virtualBounds.bottom));
			Pen.stroke;
			Pen.alpha = 1;
		};

		//Pen.color = Color.red;
		//Pen.addRect(this.parent.virtualBounds);
		//Pen.stroke;
	}

	selectNode {
		this.refloc = this.nodeloc;
		color = Color.red;
	}

	deselectNode {
		color = Color.black;
	}

}

MidinoteTimelineRulerView : TimelineView {
	var <>mygrid; // debug
	//var >virtualBounds;

	*new { arg w, bounds; 
		^super.new.specialInit;
	}

	//*newFromEventList { arg eventlist, w, bounds;
	//	^super.new.initParaSpace(w, bounds).mapEventList(eventlist);
	//}

	specialInit { arg w, argbounds;
		this.view.drawFunc = { TimelineDrawer.draw_piano_bar(this, 2/3) };
	}

	//bounds {
	//	^this.view.bounds;
	//}

	drawFuncSimple {
		var grid;
		var bounds = this.bounds;
		Pen.alpha = 0.5;
		Pen.color = Color.black;

		areasize.debug("drawFunc: areasize");
		areasize.y.do { arg py;
			//[this.gridPointToPixelPoint(Point(0,py)),this.gridPointToPixelPoint(Point(areasize.x, py))].debug("line");
			Pen.line(this.gridPointToPixelPoint(Point(0,py)),this.gridPointToPixelPoint(Point(areasize.x, py)));
		};
		Pen.stroke;
	}

	drawFuncPiano {
		var grid;
		var bounds = this.bounds;
		var piano_pattern = [0,1,0,1,0, 0,1,0,1,0,1,0];
		Pen.alpha = 0.5;
		Pen.color = Color.black;


		areasize.debug("drawFunc: areasize");
		areasize.y.do { arg py;
			var start;
			var next;
			//[this.gridPointToPixelPoint(Point(0,py)),this.gridPointToPixelPoint(Point(areasize.x, py))].debug("line");
			start = this.gridPointToPixelPoint(Point(0,py));
			next = this.gridPointToPixelPoint(Point(0,py+1));
			Pen.line(start,this.gridPointToPixelPoint(Point(areasize.x/4, py)));
			Pen.fillRect( Rect(start.x, start.y, areasize.x/4, next.y-start.y  ) )
		};
		Pen.stroke;
	}

}


TimelineLocatorPropertiesView {
	
	*initClass {
		Class.initClassTree(TextField)
	}

	*new { arg model;
		super.new.init(model);
	}

	init { arg model;
		var window = Window.new("Locator edit", Rect(550,550,200,60));
		var layout;
		layout = VLayout(
			TextField.new
			.string_(model[\label] ? "unnamed")
			.keyDownAction_({ arg me, key, modifiers, unicode, keycode;
				[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");

				if(key == $\r) {
					// defering because Enter trigger action after keyDownAction so view is already closed
					{
						window.close;
					}.defer(0.1)
				};
				
			})
			.action_({ arg view;
				model[\label] = view.value;
				model.changed(\refresh);
			})
		);
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
	}
	
}


TimelineDrawer {
	
	*draw_piano_bar { arg timeline, roll_start_x_fac=(2/3), alpha=0.7;
		var grid;
		var bounds = timeline.bounds;
		var piano_pattern = Pseq([0,1,0,1,0, 0,1,0,1,0,1,0],inf).asStream;
		var areasize = timeline.areasize;
		var roll_start_x = timeline.bounds.width * roll_start_x_fac;
		var labels;
		Pen.alpha = alpha;
		Pen.color = Color.black;


		areasize.debug("drawFunc: areasize");
		areasize.y.do { arg py;
			var start;
			var smallstart;
			var next;
			var end;
			var pkey;
			//[timelines.gridPointToPixelPoint(Point(0,py)),timelines.gridPointToPixelPoint(Point(areasize.x, py))].debug("line");
			start = timeline.gridPointToPixelPoint(Point(0,py));
			start = Point(0, start.y);
			smallstart = timeline.gridPointToPixelPoint(Point(0,py));
			smallstart = Point(roll_start_x, start.y);
			end = timeline.gridPointToPixelPoint(Point(0, py));
			end = Point(timeline.bounds.width, end.y);
			next = timeline.gridPointToPixelPoint(Point(0,py+1));
			next = Point(0, next.y);
			pkey = Rect(roll_start_x, start.y,  timeline.bounds.width, next.y-start.y  ).flipY;


			Pen.color = if(piano_pattern.next == 0) {
				Color.white;
			} {
				Color.black
			};
			Pen.fillRect( pkey );


			if((py+1)%12==0) {
				var font;
				Pen.color = Color.black;
				font = Font('sans', 8);

				Pen.stringInRect("C" ++ (py/12).trunc ++" "++py, Rect(next.x+5, next.y-10, timeline.bounds.width, 10), font);
				Pen.line( start, end );
				Pen.stroke;
			} {
				Pen.line(smallstart, end);
				Pen.color = Color.black;
				Pen.stroke;
			};
			//[start, end, pkey].debug("pianooooooooooooo");
			//Pen.fill;
		};
		Pen.color = Color.black;
		Pen.line(Point(roll_start_x, 0), Point(roll_start_x, timeline.bounds.height));
		//Pen.line(Point(20, 0), Point(20, timeline.bounds.height));
		Pen.stroke;
		Pen.alpha = 1;
	}

}

MidinoteTimelineView : TimelineView {
	drawGridY {
		TimelineDrawer.draw_piano_bar(this, 0, 0.2);
	}
	
}

PdefTimelineView : TimelineView {

}

////////////////////////// cursor

CursorTimelineView : TimelineView {
	var <>cursorPos = 0;
	var <>playtask;
	var <>cursor;
	var <>cursorController;
	drawFunc {
		var cpos;
		var spos;
		Pen.color = Color.red;
		cpos = this.gridPointToPixelPoint(Point(cursorPos, 0)).x;
		Pen.line(Point(cpos,0), Point(cpos, this.virtualBounds.height));
		Pen.stroke;

		if(cursor.notNil) {
			Pen.color = Color.blue;
			spos = this.gridPointToPixelPoint(Point(cursor.startPosition, 0)).x;
			//[cursor.startPosition, spos].debug("CursorTimelineView: start, spos");
			Pen.line(Point(spos,0), Point(spos, this.virtualBounds.height));
			Pen.stroke;

			if(cursor.endPosition.notNil) {
				Pen.color = Color.blue;
				spos = this.gridPointToPixelPoint(Point(cursor.endPosition, 0)).x;
				//[cursor.endPosition, spos].debug("CursorTimelineView: end, spos");
				Pen.line(Point(spos,0), Point(spos, this.virtualBounds.height));
				Pen.stroke;

			};
		} {
			"cursorisnil:::!!!!".debug;
		};

	}

	mapCursor { arg cur;
		cursor = cur;
		if(cursorController.notNil) {
			cursorController.remove;
		};
		cursorController = SimpleController(cursor).put(\refresh, {
			if(this.view.notNil) {
				if(this.view.isClosed) {
					cursorController.remove;
				} {
					this.view.refresh;
				};
			}
		})
	}

	specialInit {
		this.view.background = Color.clear;
		this.view.acceptsMouse_(false);
	}

	makeUpdater {
		if(controller.notNil) {controller.remove};
		controller = SimpleController(model).put(\cursor, { arg obj, msg, arg1;
			if(this.view.isNil) {
				//"COOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOSLCLOCLOCLOSED".debug;
				controller.remove;
			} {
				"CursorTimelineView get a refresh signal!".debug;
				if(arg1 == \play) {
					this.play;
				} {
					this.stop;
				}

			};
		});
		controller.put(\redraw, {
			this.refresh;
		});
	}

	play {
		if(playtask.notNil) {
			playtask.stop;
		};

		playtask = Task({
			var start_beat;
			var start_offset;
			var endTime;
			Server.default.latency.wait; // compense for pattern latency
			start_beat = TempoClock.default.beats;
			start_offset = this.model.startTime;
			endTime = this.model.endTime; // no real time modification of end time
			cursorPos =  start_offset;
			if(cursor.notNil and: { cursor.startPosition.notNil }) {
				start_offset = start_offset max: cursor.startPosition;
			};
			while({
				cursorPos < endTime;
				//true;
			}, {
				cursorPos = TempoClock.default.beats - start_beat + start_offset;
				//cursorPos.debug("cursorPos");
				{
					if(this.view.isNil or: { this.view.isClosed }) {
						//this.stop;
					} {
						this.view.refresh;
					};
				}.defer;
				( 1/16 ).wait;
				
			});
		});
		playtask.play(TempoClock.default)
	}

	stop {
		if(playtask.notNil) {
			playtask.stop;
		};
		
	}
	
}

CursorTimeline {
	var <startPosition;
	var <endPosition;
	startPosition_ { arg startPos;
		startPosition = startPos;
		this.changed(\startPosition, startPosition);
		this.changed(\refresh);
	}

	endPosition_ { arg endPos;
		endPosition = endPos;
		this.changed(\endPosition, startPosition);
		this.changed(\refresh);
	}
}
