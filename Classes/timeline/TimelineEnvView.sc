
//////////////////////////////// Env and Env node

// should be named ParamTimelineView, no ?
TimelineEnvView : TimelineView {
	var valueKey = \midinote;
	var <param;

	changeCurveMode {
		^true;
	}

	gridPointToNormPoint { arg point;
		if(param.notNil and: { useSpecInConversions != false }) {
			var ypos;
			ypos = param.spec.unmap(point.y);
			//^Point(point.x / areasize.x, 1-ypos);
			^Point(point.x / areasize.x, ypos);
		} {
			^(point / areasize)
		}
	}

	normPointToGridPoint { arg point;
		if(param.notNil and: { useSpecInConversions != false  }) {
			var ypos;
			//ypos = param.spec.map(1-point.y);
			ypos = param.spec.map(point.y);
			^Point(point.x * areasize.x, ypos);
		} {
			^(point * areasize)
		}
	}

	clipGridPoint { arg point;
		// FIXME: this prevent negative y values
		var x, y;
		if(param.notNil and: { useSpecInConversions != false }) {
			x = point.x.clip(0,this.areasize.x-quant.value.x); // FIXME: not sure if -1 should be scaled to something
			y = point.y.clip(param.spec.clipLo,param.spec.clipHi-quant.value.y);
		} {
			x = point.x.clip(0,this.areasize.x-quant.value.x); // FIXME: not sure if -1 should be scaled to something
			y = point.y.clip(0,this.areasize.y-quant.value.y);
		};
		^Point(x,y);
	}

	nodeClass {
		^TimelineEnvViewNode
	}

	initNode { arg node;
		node.posyKey = valueKey;
		node.refresh;
		//node.posyKey.debug("initNode: posyKey");
	}

	mapParam { arg param_object;
		param = param_object;
		this.valueKey = param.property;
		this.areasize.y = param.spec.range;
	}

	valueKey {
		^valueKey;
	}

	valueKey_ { arg val;
		valueKey = val;
		//paraNodes.do { arg node;
		//	if(node.isKindOf(TimelineEnvViewNode)) {
		//		node.posyKey = val;
		//		node.refresh;
		//	}
		//};
		this.refreshEventList;
	}

	eventFactory { arg pos;
		var nodesize = Point(1,1);
		// why nodesize is in normalized form ???
		nodesize = this.gridPointToNormPoint(nodesize);
		if(eventFactory.isNil) {
			var ev;
			ev = (absTime: pos.x, sustain:nodesize.x);
			if(param.isNil) {
				//"=========eventFactory: param is nil".debug;
				ev[\midinote] = pos.y;
			} {
				//param.property.debug("=========eventFactory: param property");
				ev[param.property] = pos.y;
			};
			^ev;
		} {
			^eventFactory.(pos, nodesize.x);
		}
	}

	drawGridY {
		TimelineDrawer.draw_param_horizontal_lines(this, this.param);
	}

	drawCurve {
		this.drawCurveFill;
	}

	drawCurveStroke {
		var first = true;
		var nodes = paraNodes.copy.sort({ arg a, b; a.nodeloc.x < b.nodeloc.x });
		var prevNode;
		//debug("<<<<<<<<<<<< start drawing nodes");
		//[this.bounds, this.virtualBounds].debug("bounds, virtualBounds");

		//[this.viewport, this.bounds, this.virtualBounds, this.areasize].debug("drawNodes:bounds");
		nodes.do({arg node;
			//[this.class, node, node.spritenum, node.origin, node.extent, node.rect, node.model].debug("drawing node");
			//[node.rect, this.gridRectToNormRect(node.rect), this.gridRectToPixelRect(node.rect)].debug("drawNodes:rect, norm, pixel");

			// is Set.contains quick enough with big selection ?
			if(node.visible) {
				if(node.class != TimelineViewLocatorLineNode) {
					if(first) { // for env timelines
						Pen.moveTo(this.gridPointToPixelPoint(node.origin));
						first = false;
					};
					node.drawCurve(prevNode);
					prevNode = node;
				}
			}
		});

		//debug(";;;;;;;;;;;;;;;;;; stop drawing nodes");
		Pen.stroke;		
	}

	drawCurveFill {
		var first = true;
		var nodes = paraNodes.copy.sort({ arg a, b; a.nodeloc.x < b.nodeloc.x });
		var prevNode;
		var firstVisibleNode;
		//debug("<<<<<<<<<<<< start drawing nodes");
		//[this.bounds, this.virtualBounds].debug("bounds, virtualBounds");

		firstVisibleNode = nodes.detect { arg node; node.visible == true and: {node.class != TimelineViewLocatorLineNode} };
		//[this.viewport, this.bounds, this.virtualBounds, this.areasize].debug("drawNodes:bounds");
		//Pen.moveTo(this.gridPointToPixelPoint(Point(0, 0)));
		//Log(\Param).debug("firstVisibleNode %", firstVisibleNode);
		if(firstVisibleNode.notNil) {
			Pen.moveTo(this.gridPointToPixelPoint(Point(firstVisibleNode.origin.x, 0)));
		} {

		};
		nodes.do({arg node;
			//[this.class, node, node.spritenum, node.origin, node.extent, node.rect, node.model].debug("drawing node");
			//[node.rect, this.gridRectToNormRect(node.rect), this.gridRectToPixelRect(node.rect)].debug("drawNodes:rect, norm, pixel");

			// is Set.contains quick enough with big selection ?
			if(node.visible) {
				if(node.class != TimelineViewLocatorLineNode) {
					//if(first) { // for env timelines
						//Pen.moveTo(this.gridPointToPixelPoint(node.origin));
						//first = false;
					//};
					node.drawCurve(prevNode);
					prevNode = node;
				}
			}
		});
		if(prevNode.notNil) {
			Pen.lineTo(this.gridPointToPixelPoint(Point(prevNode.origin.x, 0)));
			Pen.lineTo(this.gridPointToPixelPoint(Point(firstVisibleNode.origin.x, 0)));
			Pen.color = ParamViewToolBox.color_dark;
			//Pen.stroke;
			Pen.fillColor = ParamViewToolBox.color_ligth.copy.alpha_(0.5);
			Pen.fillStroke;
		}

		//debug(";;;;;;;;;;;;;;;;;; stop drawing nodes");
		//Pen.stroke;		
	}
}

TimelineEnvViewNode : TimelineViewEventNode {
	var <>radius = 4;
	var <>curve = 0;

	*new { arg parent, nodeidx, event;
		switch(event[\type],
			\start, {
				^TimelineViewLocatorLineNode.new(parent, nodeidx, event);
			},
			\end, {
				^TimelineViewLocatorLineNode.new(parent, nodeidx, event);
			},
			{
				^super.new(parent, nodeidx, event).baseInit;
			}
		);
	}


	init { arg xparent, nodeidx, event;
		parent = xparent;
		spritenum = nodeidx;
		model = event;
		extent = Point(0,0);

		//[spritenum, model].debug(this.class.debug("CREATE EVENT NODE !"));

		action = {
			//[model, origin].debug("node action before");
			model[timeKey] = origin.x;
			model[this.posyKey] = origin.y;
			model[\curve] = curve;
			//model[lenKey] = extent.x;
		};

		refresh = {
			//var posy = model[this.posyKey] ? (
			//	if(parent.param.notNil) {
			//		parent.param.default.debug("dd====================");
			//	} {
			//		0
			//	}
			//);
			var posy = model.use { model[this.posyKey].value(model) } ? (
				if(parent.param.notNil) {
					parent.param.default;
				} {
					0
				}
			);
			curve = model[\curve] ? 0;
			origin = Point(model[timeKey] ? 0, posy);
			color = ParamViewToolBox.color_ligth;
			outlineColor = Color.black;
			//extent = Point(model.use { currentEnvironment[lenKey].value(model) }, 1); // * tempo ?
			//[this.class, spritenum, origin, extent, color].debug("refresh");
		};

		this.makeUpdater;
		this.refresh;
		//this.action;
	}

	draw {
		var point;
		var pos;
		pos = this.origin;
		point = parent.gridPointToPixelPoint(this.origin);
		//[spritenum, point, this.class].debug("draw");

		//Pen.color = ParamViewToolBox.color_ligth;
		//Pen.lineTo(point);
		//Pen.stroke;

		Pen.color = this.outlineColor;

		if(this.model[\releaseNode] == true or: { this.model[\loopNode] == true }) {
			Pen.addRect(Rect.fromPoints(point-( radius ), point+( radius )))
		} {
			Pen.addArc(point, radius, 0, 2*pi);
		};

		if(selected) {
			Pen.color = this.colorSelected;
		} {
			Pen.color = this.color;
		};
		//Pen.strokeRect(this.pixelRect);
		//Pen.fill;

		//Pen.color = Color(0.8,0.8,0.8);
		//Pen.color = ParamViewToolBox.color_ligth;
		case(
			{ this.model[\releaseNode] == true }, {
				Pen.color = Color.blue;
				Pen.addRect(Rect.fromPoints(point-( radius-1 ), point+( radius-1 )))
			},
			{ this.model[\loopNode] == true }, {
				Pen.color = Color.green;
				Pen.addRect(Rect.fromPoints(point-( radius-1 ), point+( radius-1 )))
			}, {
				Pen.addArc(point, radius-1, 0, 2*pi);
			}
		);
		//Pen.strokeRect(this.pixelRect);
		Pen.fill;

		Pen.color = this.color;
		Pen.moveTo(point);

	}

	drawCurve { arg prevNode;
		var point, prevPoint;
		var pos;
		pos = this.origin;
		point = parent.gridPointToPixelPoint(this.origin);
		//[spritenum, point, this.class].debug("draw");

		Pen.color = ParamViewToolBox.color_ligth;
		//Pen.arcTo(point, ( this.curve ? 0 ).clip(1,3));
		if(prevNode.notNil) {
			var env;
			var pointCount;
			var samplePeriod = 5; // in pixels
			// the algo sample the curve and get curve coordinate from an Env
			//Log(\Param).debug("draw curve from % to %: %", prevNode.origin, this.origin, this.model);
			prevPoint = parent.gridPointToPixelPoint(prevNode.origin);
			pointCount = ( (point.x - prevPoint.x)/samplePeriod ).asInteger;
			//Pen.quadCurveTo(point, prevPoint + point / 2 + Point(0,( this.curve*200 ? 0 ).debug("curve").clip2(100)));
			env = Env.xyc([ [prevPoint.x, prevPoint.y, prevNode.curve], [point.x,point.y] ]);
			//Log(\Param).debug("pointcount %", pointCount);
			( pointCount+1 ).do { arg idx;
				var posx;
				if(pointCount == 0) {
					// point are less than samplePeriod pixel apart, no need to sample the curve
					posx = (point.x - prevPoint.x) + prevPoint.x;
				} {
					posx = idx/pointCount * (point.x - prevPoint.x) + prevPoint.x;
				};
				//Log(\Param).debug("lineTo %", Point(posx, env.at(posx)));
				Pen.lineTo(Point(posx, env.at(posx)));
			};
		} {
			//Log(\Param).debug("draw line to %: %", this.origin, this.model);
			Pen.lineTo(point);
		};
		//Pen.stroke;
		//Pen.fill;

		//Pen.color = this.outlineColor;
		//Pen.addArc(point, radius, 0, 2*pi);
		////Pen.strokeRect(this.pixelRect);
		//Pen.fill;

		//Pen.color = Color(0.8,0.8,0.8);
		//Pen.addArc(point, radius-1, 0, 2*pi);
		////Pen.strokeRect(this.pixelRect);
		//Pen.fill;

		Pen.color = this.color;
		//Pen.moveTo(point);

	}

	//deselectNode {
	//	outlineColor = Color.green;
	//}

	pixelRect {
		var point, rect;
		point = parent.gridPointToPixelPoint(this.origin);
		rect = Rect(point.x-radius, point.y-radius, radius*2, radius*2)
		^rect;
	}

	handleRect {
		var point;
		var rect;
		rect = parent.pixelRectToGridRect(this.pixelRect);
		^rect;
	}
	
}

