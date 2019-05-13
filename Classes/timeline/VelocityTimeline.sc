
//////////////////////////////// Velocity Timeline

VelocityTimelineView : TimelineEnvView {
	var valueKey = \velocity;

	nodeClass {
		^VelocityTimelineViewNode
	}

	clipGridPoint { arg point;
		var x, y;
		x = point.x.clip(0,this.areasize.x-quant.value.x); // FIXME: not sure if -1 should be scaled to something
		y = point.y.clip(param.spec.clipLo,param.spec.clipHi-quant.value.y);
		^Point(x,y);
	}

	//drawGridY {
	//	TimelineDrawer.draw_quad_lines_factor(this, 1/4);
	//}

	deleteNode { arg node, refresh=true;
		var del;
		var nodenr = node.spritenum;
		if(node.deletable.not) { ^this };
		node.model.removeAt(node.posyKey);
		deleteNodeHook.(node, nodenr);
		if(refresh == true, {this.refreshEventList; this.refresh});
	}
	

}

VelocityTimelineViewNode : TimelineEnvViewNode {
	var <>radius = 8;

	//*new { arg parent, nodeidx, event;
	//	switch(event[\type],
	//		\start, {
	//			^TimelineViewLocatorLineNode.new(parent, nodeidx, event);
	//		},
	//		\end, {
	//			^TimelineViewLocatorLineNode.new(parent, nodeidx, event);
	//		},
	//		{
	//			^super.new(parent, nodeidx, event).baseInit;
	//		}
	//	);
	//}

	drawTriangle {

	}

	//visible {
	//	^[\rest, \start, \end].includes(this.model[\type]).not
	//}

	//defaultPosyValue {
	//	if(parent.param.notNil) {
	//		^parent.param.default
	//	} {
	//		^64
	//	}
	//}

	//defaultHeight {
	//	//^parent.pixelPointToGridPoint(Point(1000,1000)).x; // bug with areasize initialisation order
	//	^4
	//}

	//draw {
	//	var rect;
	//	var pos;
	//	Pen.color = this.color;
	//	pos = this.origin;
	//	rect = parent.gridRectToPixelRect(this.rect);
	//	//[spritenum, rect].debug("draw");
	//	Pen.fillRect(rect);
	//	Pen.color = this.outlineColor;
	//	Pen.strokeRect(rect);
	//	//Pen.stroke;
	//}

	draw {
		var point;
		var pos;
		pos = this.origin;
		point = parent.gridPointToPixelPoint(this.origin);
		//[spritenum, point, this.class].debug("draw");

		Pen.color = this.color;
		Pen.moveTo(Point(point.x, parent.virtualBounds.height));
		Pen.lineTo(point);
		Pen.stroke;

		Pen.color = this.outlineColor;
		Pen.addArc(point, radius, 0, 2*pi);
		//Pen.strokeRect(this.pixelRect);
		Pen.stroke;

		Pen.color = Color(0.8,0.8,0.8);
		Pen.addArc(point, radius-1, 0, 2*pi);
		//Pen.strokeRect(this.pixelRect);
		Pen.stroke;

		Pen.color = this.color;
		Pen.moveTo(point);

	}

}

