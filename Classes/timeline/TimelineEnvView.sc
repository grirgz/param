
//////////////////////////////// Env and Env node

// should be named TimelineParamView, no ?
TimelineEnvView : TimelineView {
	var valueKey = \midinote;
	var <param;

	gridPointToNormPoint { arg point;
		if(param.notNil) {
			var ypos;
			ypos = param.spec.unmap(point.y);
			^Point(point.x / areasize.x, 1-ypos);
		} {
			^(point / areasize)
		}
	}

	normPointToGridPoint { arg point;
		if(param.notNil) {
			var ypos;
			ypos = param.spec.map(1-point.y);
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
}

TimelineEnvViewNode : TimelineViewEventNode {
	var <>radius = 3;

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

		Pen.color = ParamViewToolBox.color_ligth;
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

