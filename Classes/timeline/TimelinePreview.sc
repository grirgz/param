
//////////////////////////////// preview

TimelinePreview : TimelineView {
	drawFunc {
		Log(\Param).debug("preview drawFunc");
		this.drawNodes;
		this.drawEndLine;
	}

	mapModel { arg model;
		if(model[\eventlist].notNil) {
			this.mapEventList(model.eventlist);
		};

		// FIXME: this is specific code, should be generalized
		if(model.timeline.notNil) {
			switch(model.timeline.eventType,
				\clipTimeline, {
					// FIXME: should optimize this out of here
					var maxy = 1;
					model.timeline.eventList.do { arg ev;
						if(ev[this.posyKey].notNil and: { ev[this.posyKey]  > maxy }) {
							maxy = ev[this.posyKey];
						};
					};
					this.areasize.y = maxy + 1;
				}, {

				}
			);
		};
	}
}

TimelinePreview_Env : TimelineEnvView {
	drawFunc {
		Log(\Param).debug("preview drawFunc env");
		this.drawNodes;
		this.drawEndLine;
	}

	mapModel { arg model;
		this.mapParam(model.timeline.levelParam);
	}
}

TimelinePreview_Sample : SampleTimelineView {
	drawFunc {
		Log(\Param).debug("preview drawFunc sample");
		this.drawWaveform;
		this.drawEndLine;
	}

	mapModel { arg model;
		this.mapData(model.timeline);
	}
}

