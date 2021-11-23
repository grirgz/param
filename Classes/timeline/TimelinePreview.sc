
//////////////////////////////// preview

TimelinePreview : TimelineView {
	drawFunc {
		Log(\Param).debug("preview drawFunc");
		this.drawNodes;
		//this.drawNodesDebug;
		this.drawEndLine;
	}

	drawNodesDebug {

		var defered_nodes = List.new;
		var first = true;
		Log(\Param).debug("x==x==x==x==x((( start drawing nodes DEBUG");
		//debug("start drawing nodes");
		//[this.bounds, this.virtualBounds].debug("bounds, virtualBounds");

		//[this.viewport, this.bounds, this.virtualBounds, this.areasize].debug("drawNodes:bounds");
		Log(\Param).debug("paraNodes %", paraNodes);
		paraNodes.do({arg node;
			//[this.class, node, node.spritenum, node.origin, node.extent, node.rect, node.model].debug("drawing node");
			//[node.rect, this.gridRectToNormRect(node.rect), this.gridRectToPixelRect(node.rect)].debug("drawNodes:rect, norm, pixel");

			// is Set.contains quick enough with big selection ?
			if(node.visible) {
				if(node.class == TimelineViewLocatorLineNode or: { selNodes.matchItem(node) }) {
					defered_nodes.add(node)
				} {
					if(first) { // for env timelines
						Pen.moveTo(this.gridPointToPixelPoint(node.origin));
						first = false;
					};
					Log(\Param).debug("x==x==x node %", node.asCompileString);
					node.draw;
				}
			}
		});
		defered_nodes.do { arg node;
			node.draw;
		};

		//debug("stop drawing nodes");
		Log(\Param).debug("x==x==x==x==x))) stop drawing nodes DEBUG");
		Pen.stroke;		
	}

	mapModel { arg model;
		// FIXME: duplicated code in caller
		//Log(\Param).debug("x==x model % %", model, model[\eventlist]);
		if(model[\eventlist].notNil) {
			this.mapEventList(model.eventlist);
		};
		if(model[\timeline].notNil) {
			this.mapEventList(model.timeline.eventList);
		};


		// FIXME: this is specific code, should be generalized
		// FIXME: this code is broken, there is no posyKey in Preview class, it's a member of TimelineViewNodeBase
		//if(model.timeline.notNil) {
			//switch(model.timeline.eventType,
				//\clipTimeline, {
					//// FIXME: should optimize this out of here
					//var maxy = 1;
					//model.timeline.eventList.do { arg ev;
						//if(ev[this.posyKey].notNil and: { ev[this.posyKey]  > maxy }) {
							//maxy = ev[this.posyKey];
						//};
					//};
					//this.areasize.y = maxy + 1;
				//}, {

				//}
			//);
		//};
	}
}

TimelinePreview_Env : TimelineEnvView {
	drawFunc {
		Log(\Param).debug("preview drawFunc env");
		this.drawNodes;
		this.drawEndLine;
	}

	mapModel { arg model;
		if(model[\timeline].notNil) {
			this.mapEventList(model.timeline.eventList);
		};
		this.mapParam(model.timeline.levelParam);
	}
}

TimelinePreview_Sample : SampleTimelineView {
	drawFunc {
		Log(\Param).debug("preview drawFunc sample");
		this.drawWaveform;
		this.drawNodes;
		this.drawEndLine;
	}

	mapModel { arg model;
		this.mapData(model.timeline);
		this.mapEventList(model.timelines.eventList);
	}
}

