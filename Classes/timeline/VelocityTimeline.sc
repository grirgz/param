
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

}

