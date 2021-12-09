
//////////////////////////////// Env and Env node

// should be named ParamTimelineView, no ?
TimelineEnvView : TimelineView {
	var valueKey = \midinote;
	var <param;

	changeCurveMode {
		^true;
	}

	gridPointToNormPoint { arg point;
		if(param.notNil) {
			var ypos;
			ypos = param.spec.unmap(point.y);
			//^Point(point.x / areasize.x, 1-ypos);
			^Point(point.x / areasize.x, ypos);
		} {
			^(point / areasize)
		}
	}

	normPointToGridPoint { arg point;
		if(param.notNil) {
			var ypos;
			//ypos = param.spec.map(1-point.y);
			ypos = param.spec.map(point.y);
			^Point(point.x * areasize.x, ypos);
		} {
			^(point * areasize)
		}
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
}

TimelineEnvViewNode : TimelineViewEventNode {
	var <>radius = 3;
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
		extent = Point(1/4,1);

		//[spritenum, model].debug(this.class.debug("CREATE EVENT NODE !"));

		action = {
			//[model, origin].debug("node action before");
			model[timeKey] = origin.x;
			model[this.posyKey] = origin.y;
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
			color = Color.black;
			outlineColor = Color.grey;
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
		Pen.addArc(point, radius, 0, 2*pi);
		//Pen.strokeRect(this.pixelRect);
		Pen.fill;

		//Pen.color = Color(0.8,0.8,0.8);
		Pen.color = ParamViewToolBox.color_ligth;
		Pen.addArc(point, radius-1, 0, 2*pi);
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
			Log(\Param).debug("draw curve from % to %: %", prevNode.origin, this.origin, this.model);
			prevPoint = parent.gridPointToPixelPoint(prevNode.origin);
			pointCount = ( (point.x - prevPoint.x)/10 ).asInteger;
			//Pen.quadCurveTo(point, prevPoint + point / 2 + Point(0,( this.curve*200 ? 0 ).debug("curve").clip2(100)));
			env = Env.xyc([ [prevPoint.x, prevPoint.y, prevNode.curve], [point.x,point.y] ]);
			( pointCount+1 ).do { arg idx;
				var posx = idx/pointCount * (point.x - prevPoint.x) + prevPoint.x;
				Pen.lineTo(Point(posx, env.at(posx)));
			};
		} {
			Log(\Param).debug("draw line to %: %", this.origin, this.model);
			Pen.lineTo(point);
		};
		Pen.stroke;

		//Pen.color = this.outlineColor;
		//Pen.addArc(point, radius, 0, 2*pi);
		////Pen.strokeRect(this.pixelRect);
		//Pen.fill;

		//Pen.color = Color(0.8,0.8,0.8);
		//Pen.addArc(point, radius-1, 0, 2*pi);
		////Pen.strokeRect(this.pixelRect);
		//Pen.fill;

		Pen.color = this.color;
		Pen.moveTo(point);

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

	rect {
		var point;
		var rect;
		rect = parent.pixelRectToGridRect(this.pixelRect);
		^rect;
	}
	
}

