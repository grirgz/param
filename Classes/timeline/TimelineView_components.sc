// Timeline helper widgets

////// x rulers
// the ruler is a small bar, the grid under the timeline is drawn by TimelineView.drawGridX

TimelineRulerView : TimelineView {
	// this is a X ruler with graduated bars to measure time in beats
	// you can define start and stop of preview loop interactively
	// also draw the red triangle for current position (useful for pasting)
	//var <>mygrid; // debug, already in parentclass
	var <>cursor;

	specialInit { 
		this.view.mouseDownAction = nil;
		this.view.mouseMoveAction = nil;
		this.view.mouseUpAction = nil;
		this.view.drawFunc = { this.drawFunc };
		this.virtualBoundsOffsetY = 0;
	}

	refreshEventList {
		// this view draw no nodes
	}

	mapCursor { arg curs;
		cursor = curs;
		this.view.mouseDownAction = { arg me, px, py, mod, buttonNumber, clickCount, chosennode;
			//px.debug("TimelineLocatorBarView: mousedown set start");
			//if(chosennode.isNil) 
			switch(buttonNumber,
				0, {
					cursor.startPosition_(this.pixelPointToGridPoint(Point(px, 0)).x.round(quant.value.x));
				},
				1, {
					cursor.endPosition_(this.pixelPointToGridPoint(Point(px, 0)).x.round(quant.value.x));
				},
				2, {
					cursor.startPosition_(nil);
					cursor.endPosition_(nil);
				}
			);
			this.refresh;
			mouseDownAction.(me, px, py, mod, buttonNumber, clickCount);
		}
	}

	drawCursor {
		var start_ppos = 0;
		var end_ppos;
		//Pen.color = Color.red;
		//cpos = this.gridPointToPixelPoint(Point(cursorPos, 0)).x;
		//Pen.line(Point(cpos,0), Point(cpos, this.virtualBounds.height));
		//Pen.stroke;

		if(cursor.notNil) {
			if(cursor.startPosition.notNil) {
				Pen.color = Color.blue;
				Pen.width = 2;
				start_ppos = this.gridPointToPixelPoint(Point(cursor.startPosition, 0)).x;
			};
			//[cursor.startPosition, spos].debug("CursorTimelineView: start, spos");
			//Pen.line(Point(spos,0), Point(spos, this.virtualBounds.height));
			//Pen.stroke;

			if(cursor.endPosition.notNil) {
				//var 
				Pen.color = Color.blue.alpha_(0.4);
				Pen.width = 2;
				end_ppos = this.gridPointToPixelPoint(Point(cursor.endPosition, 0)).x;
				//[cursor.endPosition, spos].debug("CursorTimelineView: end, spos");
				//Pen.line(Point(spos,0), Point(spos, this.virtualBounds.height));
				//Pen.fillRect(Rect(start_ppos, 0, end_ppos - start_ppos, this.virtualBounds.height));

			};

			//if(cursor.startPosition.notNil or: { cursor.endPosition.notNil }) {
			// TODO: find a way to retrieve end event time position
			if(end_ppos.notNil) {

				Pen.fillRect(Rect(
					start_ppos, 
					//0,
					this.virtualBounds.height * 3/4, 
					end_ppos - start_ppos, 
					this.virtualBounds.height * 1/4
				));
				Pen.stroke;
			}
		} {
			//"cursorisnil:::!!!!".debug;
		};
	}

	drawGraduations { arg factor, x, oidx, idx;
		// factor: the zoom factor, a big factor means big zoom and grid marking very small times
		// x is x position in pixel where to draw a bar
		// oidx is the index of the bar to alternate bold bars
		// idx is not used
		var yoffset;
		var prec = 4;
		//x = x + this.gridPointToPixelPoint(gridRulerOffset,0).x;

		if( oidx % 4 == 0 ) { 
			yoffset = 0;
			Pen.color = Color.black;
			Pen.alpha = 1;
		} { 
			yoffset = 4;
			Pen.color = Color.black;
			Pen.alpha = 0.5;
		};
		//Pen.line(Point(x,yoffset), Point(x,this.virtualBounds.height));
		x = x - 2; // x offset to center text
		if(oidx % 2 == 0) {
			var fontsize = 8;
			if(oidx % 16 == 0) {
				if(oidx % 32 == 0) {
					fontsize = 10;
					Pen.stringAtPoint("" ++ ( oidx/factor ).asStringPrec(prec), Point(x,-2), Font.new.size_(fontsize).bold_(true));
				} {
					fontsize = 10;
					Pen.stringAtPoint("" ++ ( oidx/factor ).asStringPrec(prec), Point(x,-2), Font.new.size_(fontsize));
				}
			} {
				Pen.stringAtPoint("" ++ ( oidx/factor ).asStringPrec(prec), Point(x,0), Font.new.size_(fontsize));
			};
		};
		//Pen.stroke;
	}

	drawGraduationsWithLines { arg factor, x, oidx, idx;
		// deprecated
		// write vertical line and the number is at the left
		// factor: the zoom factor, a big factor means big zoom and grid marking very small times
		// x is x position in pixel where to draw a bar
		// oidx is the index of the bar to alternate bold bars
		// idx is not used
		var yoffset;
		var prec = 4;
		//x = x + this.gridPointToPixelPoint(gridRulerOffset,0).x;

		if( oidx % 4 == 0 ) { 
			yoffset = 0;
			Pen.color = Color.black;
			Pen.alpha = 1;
		} { 
			yoffset = 4;
			Pen.color = Color.black;
			Pen.alpha = 0.5;
		};
		Pen.line(Point(x,yoffset), Point(x,this.virtualBounds.height));
		if(oidx % 2 == 0) {
			var fontsize = 8;
			if(oidx % 16 == 0) {
				if(oidx % 32 == 0) {
					fontsize = 10;
					Pen.stringAtPoint(" " ++ ( oidx/factor ).asStringPrec(prec), Point(x,-2), Font.new.size_(fontsize).bold_(true));
				} {
					fontsize = 10;
					Pen.stringAtPoint(" " ++ ( oidx/factor ).asStringPrec(prec), Point(x,-2), Font.new.size_(fontsize));
				}
			} {
				Pen.stringAtPoint(" " ++ ( oidx/factor ).asStringPrec(prec), Point(x,0), Font.new.size_(fontsize));
			};
		};
		Pen.stroke;
	}

	*vertical_grid_do { arg view, fun, gridRulerOffset=0;
		// dynamic time grid generation
		var unitRect = view.gridRectToPixelRect(Rect(0,0,1,1));

		var minsize = 20;
		var bounds = view.virtualBounds;
		var areasize = view.areasize;
		var viewport = view.viewport;
		var xlen = unitRect.width; // number of pixel for one beat
		var offset = unitRect.left;
		var factor = 1;
		var lineCount;
		factor = 2**( ( xlen/minsize ).log2.asInteger );
		// plus on zoom et plus unitRect donc ylen augmente sa valeur en pixel, ce qui rend factor plus grand
		//factor = ( xlen/minsize ).asInteger;
		//xlen.debug("xlen");

		//[ (areasize.x * viewport.origin.x).asInteger, (areasize.x * factor * viewport.width + 1).asInteger ].debug("start, end XXXXXX");
		// on prend la largeur visible en beats (area*viewport) qu'on multiplie par le factor
		// par exemple s'il y a moyen de caser 5 graduations dans un beat, le factor est de 5
		// donc si 3 beats sont visibles a l'ecran, alors il y aura 3*5=15 graduations

		//lineCount = (factor * (areasize.x * viewport.width + gridRulerOffset) + 0).asInteger;
		lineCount = (factor * (areasize.x * viewport.width) + 0).asInteger;
		lineCount.do { arg idx;
			var oidx, x;
			//var orx;

			//oidx = (idx + (factor * (areasize.x * viewport.origin.x - gridRulerOffset)).asInteger + 1);
			//x = oidx * xlen / factor + offset + (gridRulerOffset/areasize.x * bounds.width);

			oidx = (idx + (factor * (areasize.x * viewport.origin.x)).asInteger + 1);
			x = oidx * xlen / factor + offset;

			//orx = (idx) * xlen / factor + offset;
			//x = (idx + (areasize.x * viewport.origin.x * factor).asInteger + 1) * xlen / factor + offset;
			//[idx, x, xlen, bounds.height, bounds, offset, factor].debug("grid drawer: x");
			fun.(factor, x, oidx, idx, lineCount);
		}
	}

	*horizontal_grid_do { arg view, fun;
		// dynamic pitch grid generation
		var unitRect = view.normRectToPixelRect(Rect(0,0,1,1));
		var minsize = 30;
		var bounds = view.bounds;
		var areasize = view.areasize;
		var ylen = unitRect.height; // number of pixel of the virtual height, the more we zoom, the bigger
		var factor = 1;
		var lineCount; 
		var startidx, endidx;
		var vlineCount;
		// we want a graduation each minsize pixels, we take the virtual height and count how many graduations
		factor = 2**( ( ylen/minsize ).log2.asInteger );
		lineCount = factor.asInteger;
		//[factor, lineCount].debug("factor");
		endidx = ( view.pixelPointToNormPoint(Point(0,0)).y*lineCount ).asInteger + 2;
		startidx = ( view.pixelPointToNormPoint(Point(0,bounds.height)).y*lineCount ).asInteger;
		vlineCount = endidx - startidx;
		vlineCount.do { arg idx;
			var y;
			var vidx = idx + startidx;
			y = view.normPointToPixelPoint(Point(0,vidx/lineCount)).y;
			// we use vidx and lineCount together because the vertue of vidx is it behave like it was the real index, but skip the off-screen ones, so we want the real lineCount too
			fun.(y, vidx, lineCount, idx, vlineCount);
		}
	}

	drawFunc {
		var grid;
		var bounds = this.bounds;
		var fontsize = 8;
		if(mygrid.notNil) {
			mygrid.(this)
		} {
			this.class.vertical_grid_do(this, { arg ... args;
				this.perform(\drawGraduations, *args)
			}, gridRulerOffset);
		};
		//this.drawCursor; // now drawn by CursorTimelineView

		// current pos
		if(this.lastGridPos.notNil) {
			var ppos = this.gridPointToPixelPoint( this.lastGridPos );
			var sweep = 1/2;
			Pen.color = Color.red(alpha:0.8);
			Pen.addWedge(Point( ppos.x, 10 ), 10, 3pi/2 - (sweep/2), sweep );
			Pen.fill;
		};

		Pen.color = Color.black;
		Pen.alpha = 0.5;
		Pen.stringAtPoint("beats", Point(bounds.right-35,0), Font.new.size_(fontsize).bold_(true));

	}
}

TimelineSecondRulerView : TimelineRulerView {
	// this is a X ruler with graduated bars to measure time in *seconds*
	// you can define start and stop of preview loop interactively
	// also draw the red triangle for current position (useful for pasting)
	//var <>mygrid; // debug, already in parentclass
	var <>cursor;

	mapCursor { arg curs;
		cursor = curs;
		this.view.mouseDownAction = { arg me, px, py, mod, buttonNumber, clickCount, chosennode;
			//px.debug("TimelineLocatorBarView: mousedown set start");
			//if(chosennode.isNil) 
			switch(buttonNumber,
				0, {
					cursor.startPosition_(this.pixelPointToGridPoint(Point(px, 0)).x.round(quant.value.x));
				},
				1, {
					cursor.endPosition_(this.pixelPointToGridPoint(Point(px, 0)).x.round(quant.value.x));
				},
				2, {
					cursor.startPosition_(nil);
					cursor.endPosition_(nil);
				}
			);
			this.refresh;
			mouseDownAction.(me, px, py, mod, buttonNumber, clickCount);
		}
	}

	specialInit { 
		this.view.mouseDownAction = nil;
		this.view.mouseMoveAction = nil;
		this.view.mouseUpAction = nil;
		this.view.drawFunc = { this.drawFunc };
	}

	drawCursor {
		var start_ppos = 0;
		var end_ppos;
		//Pen.color = Color.red;
		//cpos = this.gridPointToPixelPoint(Point(cursorPos, 0)).x;
		//Pen.line(Point(cpos,0), Point(cpos, this.virtualBounds.height));
		//Pen.stroke;

		if(cursor.notNil) {
			if(cursor.startPosition.notNil) {
				Pen.color = Color.blue;
				Pen.width = 2;
				start_ppos = this.gridPointToPixelPoint(Point(cursor.startPosition, 0)).x;
			};
			//[cursor.startPosition, spos].debug("CursorTimelineView: start, spos");
			//Pen.line(Point(spos,0), Point(spos, this.virtualBounds.height));
			//Pen.stroke;

			if(cursor.endPosition.notNil) {
				//var 
				Pen.color = Color.blue.alpha_(0.4);
				Pen.width = 2;
				end_ppos = this.gridPointToPixelPoint(Point(cursor.endPosition, 0)).x;
				//[cursor.endPosition, spos].debug("CursorTimelineView: end, spos");
				//Pen.line(Point(spos,0), Point(spos, this.virtualBounds.height));
				//Pen.fillRect(Rect(start_ppos, 0, end_ppos - start_ppos, this.virtualBounds.height));

			};

			//if(cursor.startPosition.notNil or: { cursor.endPosition.notNil }) {
			// TODO: find a way to retrieve end event time position
			if(end_ppos.notNil) {

				Pen.fillRect(Rect(
					start_ppos, 
					//0,
					this.virtualBounds.height * 3/4, 
					end_ppos - start_ppos, 
					this.virtualBounds.height * 1/4
				));
				Pen.stroke;
			}
		} {
			//"cursorisnil:::!!!!".debug;
		};
	}


	drawGraduations { arg factor, x, oidx, idx;
		var yoffset;
		if( oidx % 4 == 0 ) { 
			yoffset = 0;
			Pen.color = Color.black;
			Pen.alpha = 1;
		} { 
			yoffset = 4;
			Pen.color = Color.black;
			Pen.alpha = 0.5;
		};
		Pen.line(Point(x,yoffset), Point(x,this.virtualBounds.height));
		if(oidx % 2 == 0) {
			var fontsize = 8;
			if(oidx % 16 == 0) {
				if(oidx % 32 == 0) {
					fontsize = 10;
					Pen.stringAtPoint(" " ++ ( oidx/factor ).asString, Point(x,-2), Font.new.size_(fontsize).bold_(true));
				} {
					fontsize = 10;
					Pen.stringAtPoint(" " ++ ( oidx/factor ).asString, Point(x,-2), Font.new.size_(fontsize));
				}
			} {
				Pen.stringAtPoint(" " ++ ( oidx/factor ).asString, Point(x,0), Font.new.size_(fontsize));
			};
		};
		Pen.stroke;
	}

	*vertical_grid_do { arg view, fun;
		// dynamic grid generation
		var unitRect = view.secondRectToPixelRect(Rect(0,0,1,1));

		var minsize = 20;
		var bounds = view.bounds;
		var areasize = view.areasize;
		var second_areasize = view.areasize / view.clock.tempo;
		var viewport = view.viewport;
		var xlen = unitRect.width; // number of pixel for one beat
		var offset = unitRect.left;
		var factor = 1;
		factor = 2**( xlen/minsize ).log2.asInteger;
		//factor = (factor / view.clock.tempo).asInteger;
		//xlen.debug("xlen");

		//[ (areasize.x * viewport.origin.x).asInteger, (areasize.x * factor * viewport.width + 1).asInteger ].debug("start, end XXXXXX");
		(second_areasize.x  * factor * viewport.width + 1).asInteger.do { arg idx;
			var oidx, x;
			//var orx;
			oidx = (idx + (second_areasize.x * viewport.origin.x * factor).asInteger + 1);
			x = oidx * xlen / factor + offset;
			//orx = (idx) * xlen / factor + offset;
			//x = (idx + (areasize.x * viewport.origin.x * factor).asInteger + 1) * xlen / factor + offset;
			//[idx, x, xlen, bounds.height, bounds, offset, factor].debug("grid drawer: x");
			fun.(factor, x, oidx, idx);
		}
	}

	drawFunc {
		var grid;
		var bounds = this.bounds;
		var fontsize = 8;
		if(mygrid.notNil) {
			mygrid.(this)
		} {
			this.class.vertical_grid_do(this, { arg ... args;
				this.perform(\drawGraduations, *args)
			});
		};
		this.drawCursor;

		// current pos
		if(this.lastGridPos.notNil) {
			var ppos = this.gridPointToPixelPoint( this.lastGridPos );
			var sweep = 1/2;
			Pen.color = Color.red(alpha:0.8);
			Pen.addWedge(Point( ppos.x, 10 ), 10, 3pi/2 - (sweep/2), sweep );
			Pen.fill;
		};

		Pen.color = Color.black;
		Pen.alpha = 0.5;
		Pen.stringAtPoint("secs.", Point(bounds.right-35,0), Font.new.size_(fontsize).bold_(true));

	}
}

////// locator bar

TimelineLocatorBarView : TimelineView {
	// this is the horizontal bar where are displayed the start and stop events and also the locators 
	// and the grey band outside play loop

	specialInit {
		//"SPECIAL INIT".debug;
		this.view.focusLostAction = {
			//"FOCUS LOST".debug;
			this.deselectAllNodes;
			this.refresh;
		};
		this.virtualBoundsOffsetY = 0;
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

		//key.debug("TimelineLocatorBarView: keyDownActionBase");
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
		var nstart, nend;

		//this.class.debug("drawFunc start");

		pen.width = 1;
		pen.color = background; // background color
		backgrDrawFunc.value; // background draw function


		// darken outside of playing region
		paraNodes.do { arg item, idx;
			if(item.model.type == \start) {
				nstart = this.gridPointToPixelPoint(item.origin).x;
				Pen.addRect(Rect(bounds.left,bounds.origin.y, nstart - bounds.left, bounds.height ));
				Pen.color = Color.black.alpha_(0.2);
				Pen.fill;
			};
			if(item.model.type == \end) {
				//[item.origin, item.model].debug("end node");
				nstart = this.gridPointToPixelPoint(item.origin).x;
				nend = this.gridPointToPixelPoint(item.origin).x;
				Pen.addRect(Rect(nend,bounds.origin.y, bounds.right - nstart, bounds.height ));
				Pen.color = Color.black.alpha_(0.2);
				Pen.fill;
			};
			
		};

		//this.class.debug("drawFunc nodes");

		this.drawNodes;
		//this.class.debug("drawFunc end");
		

	}
}

TimelineViewLocatorNode : TimelineViewEventNode {
	// a locator node used in TimelineLocatorBarView
	// draw a little triangle and allow to drag

	var label;
	var labelKey = \label;
	var width = 8;
	var height = 10;

	init { arg xparent, nodeidx, event;
		parent = xparent;
		spritenum = nodeidx;
		model = event;

		//[spritenum, model].debug(this.class.debug("CREATE EVENT NODE !"));

		action = {
			//[this.class, model, origin].debug("node action before");
			model[timeKey] = origin.x;
			model[labelKey] = label;
			//model[lenKey] = extent.x;
		};

		refresh = {
			//[this.class, model, origin].debug("node refresh before");
			origin = Point(model[timeKey], 0);
			//"TimelineViewLocatorLineNode: refresh: 2".debug;
			color = Color.black;
			//"TimelineViewLocatorLineNode: refresh: 3".debug;
			label = model[labelKey] ? (model[\type] ? "unnamed");
			//"TimelineViewLocatorLineNode: refresh: 4".debug;
			//[parent.viewport, parent.areasize, Point(width,height)].debug("parent vi, are, size");
			extent = parent.pixelExtentToGridExtent(Point(width,height)); //FIXME: why /2 ???
			//Log(\Param).debug("TimelineViewLocatorNode refresh: extent: %, width %", extent, Point(width,height));
			//"TimelineViewLocatorLineNode: refresh: 5".debug;
			//extent.debug("---------extent");
			//extent = Point(model.use { currentEnvironment[lenKey].value(model) }, 1); // * tempo ?
			this.refreshEnabledDo {
				parent.model.changed(\redraw);
			};
			//"TimelineViewLocatorLineNode: refresh: 6".debug;
			//[this.class, spritenum, origin, extent, color].debug("refresh");
		};

		this.makeUpdater;
		this.refresh;
		this.action;
	}

	draw {
		var point;
		var dbrect; // debug
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
		extent = parent.pixelExtentToGridExtent(Point(width,height)); 
		//origin = Point(model[timeKey], 0);

		pos = this.origin;
		point = Point(parent.gridPointToPixelPoint(pos).x, 1);
		point = point - Point(len/2, 0);
		//[spritenum, point, this.rect, parent.gridRectToPixelRect(this.rect)].debug("draw");


		box.();
		Pen.color = ParamViewToolBox.color_ligth;
		Pen.fill;

		box.();
		Pen.color = outlineColor;
		Pen.stroke;
		Pen.stringAtPoint(" "+label, Point(point.x+len,point.y+1),Font('sans', 7));

		// debug collision rect
		//Pen.color = Color.red;
		//Pen.fillRect(parent.gridRectToPixelRect(this.rect));

		//point = this.origin;
		//extent = parent.pixelPointToGridPoint(Point(width,height)); 
		//dbrect = Rect(point.x-(extent.x/2), 0, extent.x, extent.y*4);
		//Pen.color = Color.blue;
		//Pen.strokeRect(parent.gridRectToPixelRect(dbrect));
	}

	rect {
		var rect;
		var point = this.origin;
		//extent = parent.pixelExtentToGridExtent(Point(width,height));  // not used because should not be influenced by viewport
		extent = parent.pixelExtentToGridExtent(Point(width,height)); 
		rect = Rect(point.x-(extent.x/2), 0, extent.x, extent.y*4);
		//Log(\Param).debug("TimelineViewLocatorNode rect: extent: %, width %, rect %", extent, Point(width,height), rect);
		^rect;
	}

	selectable {
		^true
	}

	selectNode {
		this.refloc = this.nodeloc;
		outlineColor = Color.red;
	}

	deselectNode {
		outlineColor = Color.black;
	}
}

TimelineLocatorPropertiesView {
	// editor for the name of the locator
	
	*initClass {
		Class.initClassTree(TextField)
	}

	*new { arg model;
		super.new.init(model);
	}

	init { arg model;
		var window = Window.new("Locator edit", Rect(550,550,200,60));
		var layout;
		var field;
		var ok = {
			model[\label] = field.value;
			model.changed(\refresh);
		};
		layout = VLayout(
			field = TextField.new
			.string_(model[\label] ? "unnamed")
			.keyDownAction_({ arg me, key, modifiers, unicode, keycode;
				//[me, key.asCompileString, modifiers, unicode, keycode].debug("keyDownAction");

				if(key == $\r) {
					// defering because Enter trigger action after keyDownAction so view is already closed
					{
						window.close;
					}.defer(0.1)
				};
				
			})
			.action_({ arg view;
				ok.();
			}),
			HLayout (
				BasicButton.new.string_("Ok").action_({
					ok.();
					window.close;
				}),
				BasicButton.new.string_("Cancel").action_({
					window.close;
				}),
			)
		);
		window.layout = layout;
		//window.alwaysOnTop = true;
		window.front;
	}
}


// nodes that draw start and end events vertical lines
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
			extent = Point(1,parent.areasize.y);
			//[this.class, spritenum, origin, extent, color].debug("refresh");
		};

		this.makeUpdater;
		this.refresh;
		this.action;
	}

	draw {
		var point;
		Pen.use {

			Pen.lineDash = FloatArray[1];
			if(model.type == \start) {
				Pen.color = ParamViewToolBox.color_ligth;
				Pen.width = 2;
			} {
				if(model.type == \end) {
					Pen.color = ParamViewToolBox.color_ligth;
					Pen.lineDash = FloatArray[4,1];
					Pen.width = 2;
				} {
					Pen.color = ParamViewToolBox.color_ligth;
					//Pen.color = Color.black;
					Pen.width = 2;
					Pen.lineDash = FloatArray[2,2];
				};
			};
			Pen.alpha = alpha;
			point = parent.gridPointToPixelPoint(this.origin);
			// point is now in screen coordinates
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

////// y rulers
// the ruler is a small bar, the grid under the timeline is drawn by TimelineView.drawGridY

MidinoteTimelineRulerView : TimelineRulerView {
	// the piano roll!
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

	// old draw function
	drawFuncSimple {
		var grid;
		var bounds = this.bounds;
		Pen.alpha = 0.5;
		Pen.color = Color.black;

		//areasize.debug("drawFunc: areasize");
		areasize.y.do { arg py;
			//[this.gridPointToPixelPoint(Point(0,py)),this.gridPointToPixelPoint(Point(areasize.x, py))].debug("line");
			Pen.line(this.gridPointToPixelPoint(Point(0,py)),this.gridPointToPixelPoint(Point(areasize.x, py)));
		};
		Pen.stroke;
	}

	// deprecated by draw_piano_bar
	drawFuncPiano {
		var grid;
		var bounds = this.bounds;
		var piano_pattern = [0,1,0,1,0, 0,1,0,1,0,1,0];
		Pen.alpha = 0.5;
		Pen.color = Color.black;


		//areasize.debug("drawFunc: areasize");
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

KitTimelineRulerView : TimelineRulerView {
	// simple y ruler for KitTimeline
	var <>mygrid; // debug
	var <>wrapper;

	*new { arg w, bounds; 
		^super.new.specialInit;
	}

	mapWrapper { arg awrapper;
		wrapper = awrapper;
	}

	specialInit { arg w, argbounds;
		this.view.drawFunc = { TimelineDrawer.draw_quad_lines(this, wrapper) };
	}
}

ParamTimelineRulerView : TimelineRulerView {
	// simple y ruler for ParamTimeline
	var <>mygrid; // debug
	var <>paramTimeline;
	var <>targetParam; // function returning param

	*new { arg paramTimeline, param; 
		^super.new.initParamTimelineRulerView(paramTimeline, param);
	}

	initParamTimelineRulerView { arg ptimeline, param;
		//Log(\Param).debug("ParamTimelineRulerView init %", [ptimeline, param]);
		paramTimeline = ptimeline;
		targetParam = if(param.notNil) { { param } } ?? { { 
			//[ this, this.paramTimeline ].debug("targetParam run");
			this.paramTimeline.param;
		} };
		//[ ptimeline, paramTimeline, this.paramTimeline, this.paramTimeline.param, targetParam, param ].debug("targetParam");
		this.view.drawFunc = { TimelineDrawer.draw_param_values(this, targetParam.()) };
	}
}




////////////////////////// cursor

CursorTimelineView : TimelineView {
	// an userview to put on top of timeline to have lines indicating the start and end of cursor
	// also the line indicating the current playing position
	// TimelineRulerView handle the interactivity
	var <>cursorPos = 0;
	var <>playtask;
	var <>cursor;
	var <>cursorController;
	var <>mimicCursorController;
	var <>isPlaying = false;
	var <>refreshRate = 16;
	var <>bandWidth = 5;

	drawFunc {
		var cpos;
		var spos, espos;
		Pen.color = Color.red;
		cpos = this.gridPointToPixelPoint(Point(cursorPos, 0)).x;
		Pen.line(Point(cpos,0), Point(cpos, this.virtualBounds.height));
		Pen.stroke;

		if(cursor.notNil) {
			if(cursor.startPosition.notNil) {
				Pen.color = Color.blue;
				Pen.width = 2;
				spos = this.gridPointToPixelPoint(Point(cursor.startPosition, 0)).x;
				//[cursor.startPosition, spos].debug("CursorTimelineView: start, spos");
				Pen.line(Point(spos,0), Point(spos, this.virtualBounds.height));
				Pen.stroke;
			};

			if(cursor.endPosition.notNil) {
				Pen.color = Color.blue;
				Pen.width = 2;
				espos = this.gridPointToPixelPoint(Point(cursor.endPosition, 0)).x;
				//[cursor.endPosition, spos].debug("CursorTimelineView: end, spos");
				Pen.line(Point(espos,0), Point(espos, this.virtualBounds.height));
				Pen.stroke;

			};

			if(cursor.startPosition.notNil and: cursor.endPosition.notNil) {
				Pen.color = Color.blue.alpha_(0.2);
				Pen.width = 2;
				Pen.fillRect(Rect(
					spos, 
					0,
					espos - spos, 
					bandWidth,
				));
				Pen.stroke;
				
			}
		} {
			//"cursorisnil:::!!!!".debug;
		};

	}

	refreshEventList {
		// this view draw no nodes
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

	mimicCursor { arg othercur;
		//[cursor, othercur].debug("mimicCursor");
		if(mimicCursorController.notNil) {
			mimicCursorController.remove;
		};
		mimicCursorController = SimpleController(othercur).put(\refresh, {
			//"mimicCursorController".debug;
			if(this.view.notNil) {
				if(this.view.isClosed) {
					mimicCursorController.remove;
				} {
					//[cursor.startPosition, cursor.endPosition, othercur.startPosition, othercur.endPosition].debug("mimicCursorController");
					cursor.startPosition = othercur.startPosition;
					cursor.endPosition = othercur.endPosition;
				};
			}
		})
	}

	specialInit {
		this.view.background = Color.clear;
		this.view.acceptsMouse_(false);
		this.virtualBoundsOffsetY = 0;
	}

	makeUpdater {
		if(controller.notNil) {controller.remove};
		controller = SimpleController(model).put(\cursor, { arg obj, msg, arg1;
			if(this.view.isNil) {
				//"COOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOSLCLOCLOCLOSED".debug;
				controller.remove;
			} {
				//"CursorTimelineView get a refresh signal!".debug;
				//{
					if(arg1 == \play) {
						this.play;
					} {
						if(arg1 == \stop) {
							this.stop;
						};
					}
				//}.defer(Server.default.latency)

			};
		});
		controller.put(\redraw, {
			this.refresh;
		});
	}

	play {
		//"==================************-----------(##############)".debug("cursor PLAY");
		if(playtask.isNil) {
			playtask = TaskProxy.new;
			playtask.quant = 0;
		};

		fork {
			Server.default.latency.wait; // compense for pattern latency

			playtask.source = {
				var sloop = this.cursor.loopMaster;
				var startAbsTime = sloop.currentLoopStartAbsTime;
				var finalDur = sloop.currentLoopDur;
				var start_beat;
				var currentDur = 0;

				if(this.cursor.startTime.notNil) {
					start_beat = this.cursor.startTime + Server.default.latency;
				} {
					start_beat =  TempoClock.default.beats;
				};

				while({
					currentDur < finalDur;
					//true;
				}, {
					currentDur = TempoClock.default.beats - start_beat;
					cursorPos = startAbsTime + currentDur;
					//sloop.timeline.refCompileString.debug("cursor of timeline");
					//cursorPos.debug("cursorPos");
					//currentDur.debug("currentDur");
					{
						if(this.view.isNil or: { this.view.isClosed }) {
							this.stop;
						} {
							this.view.refresh;
						};
					}.defer;
					( this.refreshRate.reciprocal * this.clock.tempo).wait; 
				});
				cursorPos = startAbsTime + finalDur;
				{ this.view.refresh; }.defer;
				nil;
			};
			playtask.play(TempoClock.default)
		}

	}

	play_old {
		//"==================************-----------(##############)".debug("cursor PLAY");
		//if(playtask.notNil) {
		//	{
		//	//	Server.default.latency.wait;
		//		playtask.stop;
		//	}.defer(Server.default.latency.wait);
		//};
		//playtask = playtask ?? {TaskProxy.new};
		//if(playtask.isPlaying.not) {
			//{
				if(playtask.isNil) {
					playtask = TaskProxy.new;
					playtask.quant = 0;
				};
			{
				Server.default.latency.wait; // compense for pattern latency

				playtask.source = {
					var start_beat;
					var start_offset;
					var endTime;
					var privcursor;
					//Server.default.latency.wait; // compense for pattern latency
					//loop{
					//"at begining of loop:%".format(cursorPos, endTime).postln;
					//if(this.isPlaying) {
					start_beat = TempoClock.default.beats;
					start_offset = cursor.startPosition ? this.model.startTime;

					// prevent too short loop
					if(cursor.endPosition.notNil 
						and: {  
							cursor.startPosition.notNil
							and: {
								cursor.endPosition - cursor.startPosition < 0.1
							}
						}) {
							endTime = this.model.endTime;
						} {
						endTime = cursor.endPosition ? this.model.endTime; // no real time modification of end time
					};

					// prevent too short loop with endtime
					if(endTime - start_offset < 0.1) {
						Log(\Param).debug("CursorTimelineView: too short loop, start % end %", start_offset, endTime);
						endTime = start_offset + 1;
					};

					cursorPos =  start_offset;
					if(cursor.notNil and: { cursor.startPosition.notNil }) {
						start_offset = start_offset max: cursor.startPosition;
					};

					privcursor = cursorPos;
					//Log(\Param).debug("start_beat %, start_offset %, endTime:%", start_beat, start_offset, endTime);

					while({
						privcursor < endTime;
						//true;
					}, {
						cursorPos = TempoClock.default.beats - start_beat + start_offset;
						privcursor = TempoClock.default.beats - start_beat + start_offset;
						//cursorPos.debug("cursorPos");
						{
							if(this.view.isNil or: { this.view.isClosed }) {
								this.stop;
							} {
								this.view.refresh;
							};
						}.defer;
						( 1/16 ).wait;
						//{
						//	this.view.refresh;
						//}.defer;

					});
					//this.isPlaying = false;
					//} {
					//	(1/16).wait;
					//}
					//}
				};
				playtask.play(TempoClock.default)
			}.fork

			//}.defer(Server.default.latency)
		//} {
		//	this.isPlaying = true;
		//}
	}

	stop {
		//"==================************-----------(##############)".debug("cursor STOP");
		if(playtask.notNil) {
			//this.isPlaying = false;
			{
				playtask.stop;
			}.defer(Server.default.latency)
		};
		
	}
}


//////////////////////////////// utilities

TimelineDrawer {
	// Toolbox class for drawing

	*draw_dynamic_vertical_lines {
		// see TimelineView.drawGridY
	}
	
	*draw_piano_bar { arg timeline, roll_start_x_fac=(2/3), alpha=0.7;
		// if roll_start_x_fac == 0, do not draw octave labels
		var grid;
		var bounds = timeline.bounds;
		var piano_pattern = Pseq([0,1,0,1,0, 0,1,0,1,0,1,0],inf).asStream;
		var areasize = timeline.areasize;
		var roll_start_x = timeline.bounds.width * roll_start_x_fac;
		var labels;
		Pen.alpha = alpha;
		Pen.color = Color.black;


		//areasize.debug("drawFunc: areasize");
		areasize.y.do { arg py;
			var start;
			var smallstart;
			var next;
			var end;
			var pkey;
			py = py+1; // else there is an incorrect offset of one semitone
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


			if(roll_start_x_fac > 0) {
				if((py)%12==0) {
					var font;
					Pen.color = Color.black;
					font = Font('sans', 8);

					// C4 should be midinote 60 in scientific pitch notation
					Pen.stringInRect("C%   %".format((py/12).trunc.asInteger - 1, py.asInteger), Rect(next.x+5, next.y-10, timeline.bounds.width, 10), font);
					Pen.line( start, end );
					Pen.stroke;
				} {
					Pen.line(smallstart, end);
					Pen.color = Color.black;
					Pen.stroke;
				};
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

	*draw_quad_lines { arg me, wrapper;
		var areasize = me.areasize;
		var color_on = ParamViewToolBox.color_ligth.copy.alpha_(0.3);
		var color_on_dark = ParamViewToolBox.color_ligth.copy.alpha_(0.6);
		//~drawme.(this, areasize);
		//areasize.debug("drawme: drawFunc: areasize");
		Pen.use {

			areasize.y.do { arg py;
				var cellrect = Rect.fromPoints(
					me.gridPointToPixelPoint(Point(0,py)),
					me.gridPointToPixelPoint(Point(areasize.x, py+1))
				);
				//[this.gridPointToPixelPoint(Point(0,py)),this.gridPointToPixelPoint(Point(areasize.x, py))].debug("line");
				if(py%32 >= 16) {

					Pen.width = 1;
					Pen.color = Color.gray(alpha:0.3);
					if(wrapper.notNil) {
						if(wrapper.elAt(py.asInteger).isEmpty.not) {
							Pen.color = color_on_dark;
							Pen.fillRect(cellrect);
							Pen.color = Color.black;
							Pen.stringInRect(wrapper.elAt(py.asInteger).label, cellrect);
						} {
							Pen.fillRect(cellrect);
						};
					} {
						Pen.fillRect(cellrect);
					};
				} {
					if(wrapper.notNil) {
						if(wrapper.elAt(py.asInteger).isEmpty.not) {
							Pen.color = color_on;
							Pen.fillRect(cellrect);
							Pen.color = Color.black;
							Pen.stringInRect(wrapper.elAt(py.asInteger).label, cellrect);
						};
					};
				};
				if(py % 4 == 0) {
					Pen.width = 1;
					Pen.color = Color.black;
				} {
					Pen.width = 1;
					Pen.color = Color.gray;
				};
				Pen.line(me.gridPointToPixelPoint(Point(0,py)),me.gridPointToPixelPoint(Point(areasize.x, py)));
				Pen.stroke;
			};
		}
	}

	*draw_quad_lines_factor { arg me, factor=1;
		var areasize = me.areasize;
		//~drawme.(this, areasize);
		//areasize.debug("drawme: drawFunc: areasize");
		Pen.use {

			areasize.y.do { arg py;
				//[this.gridPointToPixelPoint(Point(0,py)),this.gridPointToPixelPoint(Point(areasize.x, py))].debug("line");
				if(py % ( 1/factor ) == 0) {

					if(py * factor %32 >= 16) {

						Pen.width = 1;
						Pen.color = Color.gray(alpha:0.3);
						Pen.fillRect(
							Rect.fromPoints(
								me.gridPointToPixelPoint(Point(0,py)),
								me.gridPointToPixelPoint(Point(areasize.x, py+1))
							)
						);
					};
					if(py * factor % 4 == 0) {
						Pen.width = 1;
						Pen.color = Color.black;
					} {
						Pen.width = 1;
						Pen.color = Color.gray;
					};
					Pen.line(me.gridPointToPixelPoint(Point(0,py)),me.gridPointToPixelPoint(Point(areasize.x, py)));
					Pen.stroke;
				}
			};
		}
	}

	*draw_param_horizontal_lines { arg me, param;
		//~draw_param_values.(me, param);

		var areasize = me.areasize;
		//~drawme.(this, areasize);
		if(param.notNil) {
			var font = Font.default.copy.size_(9);
			Pen.use {
				Pen.width = 1;
				Pen.color = Color.black;
				TimelineRulerView.horizontal_grid_do(me, { arg y, idx, lineCount;
					if(idx % 8 == 0) {
						Pen.color = Color.black;
					} {
						if(idx % 4 == 0) {
							Pen.color = Color.gray;
						} {
							Pen.color = Color.gray(alpha:0.3);
						}
					};
					Pen.line(Point(me.virtualBounds.origin.x,y),Point(me.virtualBounds.origin.x + me.virtualBounds.width, y));
					Pen.stroke;
				});
			}
		}
	}

	*draw_param_values { arg me, param;
		//~draw_param_values.(me, param);

		var areasize = me.areasize;
		//~drawme.(this, areasize);
		if(param.notNil) {
			var font = Font.default.copy.size_(9);
			Pen.use {
				Pen.width = 1;
				Pen.color = Color.black;
				TimelineRulerView.horizontal_grid_do(me, { arg y, idx, lineCount;
					var normVal = me.pixelPointToNormPoint(Point(1,y)).y;
					var val;
					val = param.spec.map(normVal).asStringPrec(5); 
					//[normVal, val].debug("val");
					Pen.stringAtPoint(val, Point(10,y), font);
				});
			}
		}
	}

	*draw_param_values_old2 { arg me, param;
		var areasize = me.areasize;
		//~drawme.(this, areasize);
		//areasize.debug("draw_param_values: areasize");
		if(param.notNil) {
			var font = Font.default.copy.size_(9);
			var pixelPerLine = 20;
			var gridPerLine = me.pixelPointToGridPoint(Point(10,pixelPerLine)).y;
			var lineCount = me.areasize.y/gridPerLine;
			Pen.use {
				Pen.width = 1;
				Pen.color = Color.black;
				lineCount.do { arg idx;
					var pixpos = me.gridPointToPixelPoint(Point(1,idx * gridPerLine));
					var val = param.spec.map(idx/lineCount).asStringPrec(5); // reverse
					Pen.stringAtPoint(val, pixpos, font);

				};
			}
		}
	}

	*draw_param_values_old { arg me, param;
		var areasize = me.areasize;
		//~drawme.(this, areasize);
		//areasize.debug("draw_param_values: areasize");
		if(param.notNil) {
			var font = Font.default.copy.size_(9);
			Pen.use {
				var pixelPerLine = 20; 
				var count = (me.bounds.height/pixelPerLine).asInteger;

				Pen.width = 1;
				Pen.color = Color.black;
				count.do { arg idx;
					var val = param.spec.map(( count-idx )/count).asStringPrec(5); // reverse
					Pen.stringAtPoint(val, Point(1,idx*pixelPerLine), font);

				};
			}
		}
	}
}


TimelineScroller : SCViewHolder {
	// Horizontal and vertical range sliders to allow moving and zooming

	var myOrientation;

	*new {
		var ins = super.new;
		ins.view = RangeSlider.new;
		^ins;
	}

	orientation_ { arg val;
		myOrientation = val;
		this.view.orientation = val;
	}

	orientation {
		^myOrientation;
	}

	mapTimeline { arg timeline;
		this.view.action = { arg slider;
			var range;
			range = slider.range.clip(0.01,1); // prevent division by 0
			if(this.orientation == \horizontal) {
				// prevent unneeded updates
				if(timeline.viewport.left != slider.lo or: { timeline.viewport.width != range }) {
					timeline.viewport.left = slider.lo;
					timeline.viewport.width = range;
					//[timeline.viewport, slider.hi, slider.lo, slider.range].debug("hrange action");
					timeline.lazyRefresh {
						timeline.refresh;
						timeline.changed(\viewport);
						timeline.refreshDeferred = false;
					};
				};
			} {
				if(timeline.viewport.top != slider.lo or: { timeline.viewport.height != range }) {
					timeline.viewport.top = slider.lo;
					timeline.viewport.height = range;
					//[timeline.viewport, slider.hi, slider.lo, slider.range].debug("vrange action");
					timeline.lazyRefresh {
						timeline.refresh;
						timeline.changed(\viewport);
						timeline.refreshDeferred = false;
					};
				};
			}

		};
		this.view.mouseWheelAction = { arg view, x, y, modifiers, xDelta, yDelta;
			var newport;
			var oldport;
			var top;
			var minport = ( 1/timeline.virtualBounds.extent ).clip(0,0.5);
			//[ view, x, y, modifiers, xDelta, yDelta ].debug("mouseWheelAction");
			if(modifiers.isCtrl) { // zoom horizontally
				oldport = timeline.viewport;
				newport = oldport.insetBy( ( oldport.extent.x * ( yDelta.clip2(150)/300 + 1 ) ) - oldport.extent.x, 0).sect(Rect(0,0,1,1));
				if(newport.extent.x >= minport.x) {

					timeline.viewport = newport;
					//timeline.viewport.debug("end viewport");
					timeline.refresh;
				};
				//newport.extent = Point(newport.extent.x.clip(minport.x,1), newport.extent.y.clip(minport.y,1));
			};
			if(modifiers.isShift) { // zoom horizontally
				oldport = timeline.viewport;
				newport = oldport.insetBy(0, ( oldport.extent.y * ( yDelta.clip2(150)/300 + 1 ) ) - oldport.extent.y).sect(Rect(0,0,1,1));
				if(newport.extent.y >= minport.y) {

					timeline.viewport = newport;
					//timeline.viewport.debug("end viewport");
					timeline.refresh;
				};

			};
			if(modifiers.isCtrl.not and: { modifiers.isShift.not }) {
				if(this.orientation == \horizontal) {
					var left;
					oldport = timeline.viewport;
					left = ( oldport.left + ( yDelta/timeline.virtualBounds.width ) ).clip(0,1-oldport.width);
					newport = Rect(left, oldport.top, oldport.width, oldport.height);
					//[oldport, newport, oldport.height, oldport.top, oldport.bottom].debug("oldport, newport");
					timeline.viewport = newport;
					timeline.refresh;
				} {
					oldport = timeline.viewport;
					top = ( oldport.top + ( yDelta/timeline.virtualBounds.height ) ).clip(0,1-oldport.height);
					newport = Rect(oldport.left, top, oldport.width, oldport.height);
					//[oldport, newport, oldport.height, oldport.top, oldport.bottom].debug("oldport, newport");
					timeline.viewport = newport;
					timeline.refresh;
				};
			};
		};

		this.view.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			var os = 0.04; // offset 
			// without offset, clicking on resize handle activate scrolling
			//[view, x, y, modifiers, buttonNumber, clickCount].debug("mouseDownAction");
			if(this.orientation == \horizontal) {
				var npos = x/view.bounds.width;
				//[npos, view.lo, view.hi].debug("npos");
				if(npos < ( view.lo-os ) or: { npos > ( view.hi+( os*2 ) ) }) {
					var range = view.range;
					var lo;
					lo = ( npos - ( view.range/2 ) ).clip(0,1-range);
					view.setSpanActive(lo, lo+range);
					//view.activeLo = lo;
					//dd
					//view.setSpan(npos - ( view.range/2 ), npos);
					//view.action.(view);
				};
			} {
				var npos = 1- ( y/view.bounds.height );
				//[npos, view.lo, view.hi].debug("npos");
				if(npos < ( view.lo-(os*2) ) or: { npos > ( view.hi+os ) }) {
					var range = view.range;
					var lo;
					lo = ( npos - ( view.range/2 ) ).clip(0,1-range);
					view.setSpanActive(lo, lo+range);
					//view.activeLo = lo;
					//dd
					//view.setSpan(npos - ( view.range/2 ), npos);
					//view.action.(view);
				};

			};
		});

		// make updater
		this.view.followChange(timeline, 'viewport', { arg caller, receiver, morearg;
			//[caller, receiver, morearg].debug("onChange args");
			this.refresh(timeline);
		});

		this.refresh(timeline);
	
	}

	refresh { arg timeline;
		{
			var slider = this;
			if(this.orientation == \horizontal) {
				slider.hi = timeline.viewport.left+timeline.viewport.width;
				slider.lo = timeline.viewport.left;
				//[timeline, timeline.viewport, timeline.viewport.width, slider.range, slider.hi].debug("=====+++======= ScrollView.refresh: range, hi");
			} {
				slider.hi = timeline.viewport.top+timeline.viewport.height;
				slider.lo = timeline.viewport.top;
			}
		}.defer;
	}
}

///////////////////////////////////////

// not used anymore, use TimelineDrawer 
DenseGridLines : GridLines {
	var <>density = 1;
	var <>labelDensity = 1;
	
	getParams { |valueMin,valueMax,pixelMin,pixelMax,numTicks|
		var lines,p,pixRange;
		var nfrac,d,graphmin,graphmax,range;
		pixRange = pixelMax - pixelMin;
		if(numTicks.isNil,{
			numTicks = (pixRange / 64 * density);
			numTicks = numTicks.max(3).round(1);
		});
		# graphmin,graphmax,nfrac,d = this.ideals(valueMin,valueMax,numTicks);
		lines = [];
		if(d != inf,{
			forBy(graphmin,graphmax + (0.5*d),d,{ arg tick;
				if(tick.inclusivelyBetween(valueMin,valueMax),{
					lines = lines.add( tick );
				})
			});
		});
		p = ();
		p['lines'] = lines;
		if(pixRange / numTicks > (9 / labelDensity)) {
			p['labels'] = lines.collect({ arg val; [val, this.formatLabel(val,nfrac) ] });
		};
		^p
	}
}

// not used anymore, use TimelineDrawer 
MidinoteGridLines : GridLines {
	
	var <>density = 1;
	var <>labelDensity = 1;
	
	//getParams { |valueMin,valueMax,pixelMin,pixelMax,numTicks|
	//	var lines,p,pixRange;
	//	var nfrac,d,graphmin,graphmax,range;

	//	var count = valueMax - valueMin;
	//	var pixelCount = pixelMax - pixelMin;

	//	if(pixelCount < 200) {
	//		count = count / 2;
	//	};

	//	count.collect {
	//		pixelMin

	//	}

	//	pixRange = pixelMax - pixelMin;
	//	if(numTicks.isNil,{
	//		numTicks = (pixRange / 64 * density);
	//		numTicks = numTicks.max(3).round(1);
	//	});
	//	# graphmin,graphmax,nfrac,d = this.ideals(valueMin,valueMax,numTicks);
	//	lines = [];
	//	if(d != inf,{
	//		forBy(graphmin,graphmax + (0.5*d),d,{ arg tick;
	//			if(tick.inclusivelyBetween(valueMin,valueMax),{
	//				lines = lines.add( tick );
	//			})
	//		});
	//	});
	//	p = ();
	//	p['lines'] = lines;
	//	if(pixRange / numTicks > (9 / labelDensity)) {
	//		p['labels'] = lines.collect({ arg val; [val, this.formatLabel(val,nfrac) ] });
	//	};
	//	^p
	//}

	getParams { |valueMin,valueMax,pixelMin,pixelMax,numTicks|
		var lines,p,pixRange;
		var nfrac,d,graphmin,graphmax,range;
		var count = 127;
		var pixelCount = pixelMax - pixelMin;
		pixRange = pixelMax - pixelMin;
		if(numTicks.isNil,{
			numTicks = 127;
			//if(pixelCount < 200) {
			//	numTicks = ( numTicks / 2 ).round(1);
			//};
		});
		# graphmin,graphmax,nfrac,d = this.ideals(valueMin,valueMax,numTicks);
		d= 1;
		if(pixelCount < 200) {
			d = 2
		};
		lines = [];
		if(d != inf,{
			forBy(graphmin,graphmax + (0.5*d),d,{ arg tick;
				if(tick.inclusivelyBetween(valueMin,valueMax),{
					lines = lines.add( tick );
				})
			});
		});
		p = ();
		p['lines'] = lines;
		if(pixRange / numTicks > 9) {
			p['labels'] = lines.collect({ arg val; [val, this.formatLabel(val,nfrac) ] });
		};
		^p
	}
}


