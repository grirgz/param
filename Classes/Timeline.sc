
TimelineRulerView : SCViewHolder {
	var canvas;
	var <>viewport, <>areasize;
	var <>mygrid; // debug

	*new { arg w, bounds; 
		^super.new.initTimelineRulerView(w, bounds);
	}

	//*newFromEventList { arg eventlist, w, bounds;
	//	^super.new.initParaSpace(w, bounds).mapEventList(eventlist);
	//}

	initTimelineRulerView { arg w, argbounds;
		var a, b, rect, relX, relY, pen;
		//bounds = argbounds ? Rect(20, 20, 400, 200);
		//bounds = Rect(bounds.left + 0.5, bounds.top + 0.5, bounds.width, bounds.height);

		//if((win= w).isNil, {
		//	win = GUI.window.new("ParaSpace",
		//		Rect(10, 250, bounds.left + bounds.width + 40, bounds.top + bounds.height+30));
		//	win.front;
		//});
		viewport = viewport ?? Rect(0,0,1,1);
		areasize = areasize ?? Point(2,128);
		canvas = UserView.new;
		this.view = canvas;
		
 		//bounds = mouseTracker.bounds; // thanks ron!
 		
		this.view.background = Color.white(0.9);
		//canvas.drawFunc = Message(this, \drawFunc);
		canvas.drawFunc = { this.drawFunc };
	}

	bounds {
		^this.view.bounds;
	}

	drawFunc {
		var grid;
		var bounds = this.bounds;
		if(mygrid.notNil) {
			mygrid.(this)
		} {
			DrawGrid(
				Rect(0 - (viewport.origin.x * bounds.width),0 - (viewport.origin.y * bounds.height), bounds.width / viewport.width, bounds.height / viewport.height),
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

		[spritenum, model].debug("CREATE EVENT NODE !");

		action = {
			[model, origin].debug("node action before");
			model[timeKey] = origin.x;
			model[labelKey] = label;
			//model[lenKey] = extent.x;
		};

		refresh = {
			origin = Point(model[timeKey], 0);
			color = Color.black;
			outlineColor = Color.green;
			label = model[labelKey] ? (model[\type] ? "unnamed");
			extent = parent.pixelPointToGridPoint(Point(width,height)); //FIXME: why /2 ???
			//extent = Point(model.use { currentEnvironment[lenKey].value(model) }, 1); // * tempo ?
			[spritenum, origin, extent, color].debug("refresh");
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

		// FIXME: this calls are here because I need to freeze size and ypos
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
		Pen.color = Color.black;
		Pen.stroke;
		Pen.stringAtPoint(" "+label, Point(point.x+len,point.y+1),Font('sans', 7));

		// debug collision rect
		Pen.color = Color.red;
		Pen.fillRect(parent.gridRectToPixelRect(this.rect));

	}

	rect {
		var rect;
		var point = this.origin;
		extent = parent.pixelPointToGridPoint(Point(width,height)); 
		rect = Rect(point.x-(extent.x/2), 0, extent.x, extent.y*4);
		^rect;
	}

	deselectNode {
		outlineColor = Color.green;
	}

}

// TODO: replace hardcoded line with node
// FIXME: how to be never selected by findNode ?
TimelineViewLocatorLineNode : TimelineViewEventNode {

	init { arg xparent, nodeidx, event;
		parent = xparent;
		spritenum = nodeidx;
		model = event;

		color = Color.red;

		[spritenum, model].debug("CREATE EVENT NODE !");

		action = {
			[model, origin].debug("node action before");
			model[timeKey] = origin.x;
			//model[lenKey] = extent.x;
		};

		refresh = {
			origin = Point(model[timeKey], 0);
			[spritenum, origin, extent, color].debug("refresh");
		};

		this.makeUpdater;
		this.refresh;
		this.action;
	}

	draw {
		var point;
		Pen.color = Color.red;
		point = parent.gridPointToPixelPoint(this.origin);
		Pen.line(Point(point.x, 0), Point(point.x, parent.bounds.height));
		Pen.stroke;
	}

	selectNode {
		color = Color.red;
	}

	deselectNode {
		color = Color.black;
	}

}

MidinoteTimelineRulerView : SCViewHolder {
	var canvas;
	var <>viewport, <>areasize;
	var <>mygrid; // debug
	var >virtualBounds;

	*new { arg w, bounds; 
		^super.new.initTimelineRulerView(w, bounds);
	}

	///////////////// coordinates conversion
	// FIXME: move it to a common class instead of copying

	normRectToPixelRect { arg rect;
		var bounds = this.virtualBounds;
		^Rect(
			rect.origin.x * bounds.extent.x / viewport.extent.x - ( viewport.origin.x * bounds.extent.x ) + bounds.origin.x, 
			( 1-rect.origin.y ) * bounds.extent.y / viewport.extent.y - ( viewport.origin.y * bounds.extent.y ) + bounds.origin.y,
			rect.width * bounds.extent.x/ viewport.extent.x,
			(0 - (rect.height * bounds.extent.y) ) / viewport.extent.y,
		);
	}

	pixelRectToNormRect { arg rect;
		var bounds = this.virtualBounds;
		^Rect(
			rect.origin.x + ( viewport.origin.x * bounds.extent.x ) - bounds.origin.x / bounds.extent.x * viewport.extent.x,
			1-(rect.origin.y + ( viewport.origin.y * bounds.extent.y ) - bounds.origin.y / bounds.extent.y * viewport.extent.y),
			rect.width / bounds.extent.x * viewport.extent.x,
			rect.height / bounds.extent.y * viewport.extent.y,
		);
	}

	gridRectToNormRect { arg rect;
		^Rect.fromPoints(
			this.gridPointToNormPoint(rect.origin),
			this.gridPointToNormPoint(rect.rightBottom),
		);
	}

	normRectToGridRect { arg rect;
		^Rect.fromPoints(
			this.normPointToGridPoint(rect.origin),
			this.normPointToGridPoint(rect.rightBottom),
		);
	}

	gridRectToPixelRect { arg rect;
		^this.normRectToPixelRect(this.gridRectToNormRect(rect));
	}

	pixelRectToGridRect { arg rect;
		^this.normRectToGridRect(this.pixelRectToNormRect(rect));
	}

	normPointToPixelPoint { arg point;
		^this.normRectToPixelRect(Rect.fromPoints(point, point+Point(0,0))).origin;
	}

	pixelPointToNormPoint { arg point;
		^this.pixelRectToNormRect(Rect.fromPoints(point, point+Point(0,0))).origin;
	}

	gridPointToNormPoint { arg point;
		^(point / areasize)
	}

	normPointToGridPoint { arg point;
		^(point * areasize)
	}

	pixelPointToGridPoint { arg point;
		^this.normPointToGridPoint(this.pixelPointToNormPoint(point))
	}

	gridPointToPixelPoint { arg point;
		^this.normPointToPixelPoint(this.gridPointToNormPoint(point))
	}

	virtualBounds {
		^(virtualBounds ? Rect(0,0,this.bounds.width, this.bounds.height));
	}

	////////////////////////////////////////////////////////////////


	//*newFromEventList { arg eventlist, w, bounds;
	//	^super.new.initParaSpace(w, bounds).mapEventList(eventlist);
	//}

	initTimelineRulerView { arg w, argbounds;
		var a, b, rect, relX, relY, pen;
		//bounds = argbounds ? Rect(20, 20, 400, 200);
		//bounds = Rect(bounds.left + 0.5, bounds.top + 0.5, bounds.width, bounds.height);

		//if((win= w).isNil, {
		//	win = GUI.window.new("ParaSpace",
		//		Rect(10, 250, bounds.left + bounds.width + 40, bounds.top + bounds.height+30));
		//	win.front;
		//});
		viewport = viewport ?? Rect(0,0,1,1);
		areasize = areasize ?? Point(2,128);
		canvas = UserView.new;
		this.view = canvas;
		
 		//bounds = mouseTracker.bounds; // thanks ron!
 		
		this.view.background = Color.white(0.9);
		//canvas.drawFunc = Message(this, \drawFunc);
		canvas.drawFunc = { this.drawFunc };
	}

	bounds {
		^this.view.bounds;
	}

	drawFunc {
		var grid;
		var bounds = this.bounds;
		Pen.alpha = 0.2;

		areasize.y.do { arg py;
			Pen.line(this.gridPointToPixelPoint(Point(0,py)),this.gridPointToPixelPoint(Point(areasize.x, py)));
		};
	}

}

