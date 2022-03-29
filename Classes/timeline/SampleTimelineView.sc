
/////////////////////// sample timeline view

SampleTimelineView : TimelineView {
	var <>bufferData;
	var >resampledData;
	var <>resampledDataLight;
	var >resampleRate;
	var <>resampleRateLight;
	var <>numChannels = 0;
	var <>waveformImage;
	var <>bufferDuration;
	classvar <>mydraw;

	mapData { arg model;
		this.view.onChange(model, \data, { 
			{
				Log(\Param).debug("SampleTimelineView: data change detected");
				if(
					model.enableWaveformView == true 
					and: {model.bufferInfo.notNil 
					and: {
						model.bufferInfo.bufferData.notNil 
						or: { model.bufferInfo.waveformImage.notNil }
					}}
				) {
					numChannels = model.bufferInfo.numChannels;
					this.areasize = Point(areasize.x, numChannels ? 2);
					
					resampleRate = model.bufferInfo.resampleRate;
					resampleRateLight = model.bufferInfo.resampleRateLight;
					resampledData = model.bufferInfo.resampledData;
					resampledDataLight = model.bufferInfo.resampledDataLight;
					waveformImage = model.bufferInfo.waveformImage;
					bufferDuration = model.bufferInfo.buffer.numFrames/model.bufferInfo.buffer.sampleRate;
				} {
					numChannels = nil;
					this.areasize = Point(areasize.x,2);
					
					resampleRate = nil;
					resampleRateLight = nil;
					resampledData = nil;
					resampledDataLight = nil;
					waveformImage = nil;
					model.askBufferData;

				};

				//[self.timeline.numChannels, model.numChannels].debug("A23");
				this.refresh;
			}.defer
		});
		model.changed(\data);
	}

	resampledData {
		^resampledData
	}

	resampleRate {
		^resampleRate
	}

	drawWaveform {
		if(this.class.mydraw.notNil) {
			//this.class.mydraw.debug("mydraw");
			this.class.mydraw.(this);
		} {
			this.drawImageWaveform;
			//this.drawResampledWaveform;
		}
	}

	drawImageWaveform_old {
		var bounds = this.virtualBounds;
		var height = bounds.height;
		var width = bounds.width;
		if(waveformImage.notNil) {
			// xfactor is displayedSeconds/totalSeconds * imageWidth
			//var xfactor = this.areasize.x / ( bufferDuration / this.clock.tempo ) * waveformImage.width; // bug
			var xfactor = this.areasize.x / ( bufferDuration * this.clock.tempo ) * waveformImage.width; // no bug
			//Pen.drawImage(Point(0,0), waveformImage, bounds);

			// in SampleTimeline, bounds is always equal to visible area
			// in ClipTimeline node preview, bounds is the rect of the node, so can be outside window
			waveformImage.drawInRect(bounds, Rect(
				xfactor * viewport.origin.x,
				waveformImage.height * viewport.origin.y,
				xfactor * viewport.width,
				waveformImage.height * viewport.height
			).flipScreen(waveformImage.height));
		} {
			Log(\Param).debug("drawImageWaveform: waveformImage is nil");
		}

	}

	// TODO: take in account event_dropdur
	drawImageWaveform { 

		var bounds = this.virtualBounds;
		var height = bounds.height;
		var width = bounds.width;
		var viewport = this.viewport;
		var waveformImage = this.waveformImage;
		var bufferDuration = this.bufferDuration;
		var visiblebounds;
		var parentTimeline = this.parentTimeline; // defined in preview mode
		var draw_width, draw_height;
		var areasize = this.areasize;
		var tempo = this.clock.tempo;
		//debug("blaaa");
		if(waveformImage.notNil) {
			// xfactor is displayedSeconds/totalSeconds * imageWidth
			//var xfactor = this.areasize.x / ( bufferDuration / this.clock.tempo ) * waveformImage.width; // bug
			var xoffset, image_portion_width;
			var yoffset, image_portion_height;
			var viewport_left, viewport_width;
			var viewport_top, viewport_height;
			var xfactor, yfactor;

			// in preview mode, draw_width is the clip width
			// in SampleTimeline, it is areasize.x
			//draw_width = if(parentTimeline.notNil) {
				//parentTimeline.pixelRectToGridRect(bounds).width;
				//areasize.x;
			//} {
				//areasize.x;
			//};

			//Pen.drawImage(Point(0,0), waveformImage, bounds);

			// in SampleTimeline, bounds is always equal to visible area
			// in ClipTimeline node preview, bounds is the rect of the node, so can be outside window
			// if clip is not visible, visiblebounds.width is negative
			//visiblebounds = if(parentTimeline.notNil) { 
				//parentTimeline.virtualBounds.sect(bounds);
				//bounds
			//} {
				//bounds;
			//};
			visiblebounds = bounds;

			// compute width of image (fromRect):
			// xfactor: clip_width_in_beats / buffer_duration_in_beats
			//		if the clip is half the buffer duration, total width should be halved	
			// visibility_ratio = visible_bounds / clip_bounds
			//		is equivalent to viewport.width
			// 		if half of the clip is out of parent bounds, total width should be halved
			// w = image.width * xfactor * visibility_ratio
			xfactor = areasize.x / ( bufferDuration * tempo ); 
			//viewport_width = if(parentTimeline.notNil) {
				//(visiblebounds.width / bounds.width);
				//viewport.width;
			//} {
				//viewport.width;
			//};
			image_portion_width = waveformImage.width * xfactor * viewport.width;

			//[bounds, visiblebounds, xthis.parentTimeline.virtualBounds].debug("bounds");

			// xoffset: if clip is ahead of parent left border, visiblebounds.left > bounds.left
			// xoffset should be 0, 
			// else should be visiblebounds.left - bounds.left * some_factor
			// if clip start a parentbound 0, and viewport j
			// bound offset: (visiblebounds.left - bounds.left)
			//		if left border of clip is visible, is 0
			//		if the first 100 pixels of the clip are hidden, is 100
			// ratio left offset: (visiblebounds.left - bounds.left) / bounds.width
			//		is equivalent to viewport.origin.x
			//		if left clip border is visible, is 0
			//		if half of clip is hidden, is 1/2
			//		if only last pixel of clip is visible, is 0.999
			//		if clip hidden far in the left, is > 1
			// xoffset = image.width * xfactor * ratio_left_offset
			//viewport_left = if(parentTimeline.notNil) {
				//(visiblebounds.left - bounds.left) / bounds.width;
				//viewport.origin.x;
			//} {
				//viewport.origin.x;
			//};
			xoffset = waveformImage.width * xfactor * viewport.left;

			/////

			//draw_height = if(parentTimeline.notNil) {
				//parentTimeline.pixelRectToGridRect(bounds).height;
				//areasize.y;
			//} {
				//areasize.y;
			//};
			//yfactor = 1;
			//viewport_top = if(parentTimeline.notNil) {
				//(bounds.bottom - visiblebounds.bottom) / bounds.height;
				//viewport.top;
				//viewport.origin.y;
			//} {
				//viewport.origin.y;
			//};
			////viewport_top.debug("viewport_top");
			//viewport_height = if(parentTimeline.notNil) {
				//(visiblebounds.height / bounds.height);
				//viewport.height;
			//} {
				//viewport.height;
			//};

			image_portion_height = waveformImage.height * viewport.height;
			yoffset = waveformImage.height * viewport.top;


			//Rect(0,0,10,10).sect(Rect(100,100,10,10))
			//Rect(100,100,10,10).sect(Rect(0,0,10,10))

			waveformImage.drawInRect(visiblebounds, Rect(
				xoffset,
				yoffset,
				image_portion_width, 
				image_portion_height
			).flipScreen(waveformImage.height));
		} {
			Log(\Param).debug("drawImageWaveform: waveformImage is nil");
		}
	}


	drawResampledWaveform {
		//Log(\Param).debug("draw waveform");
		//Pen.color = Color.green;
		var bounds = this.virtualBounds;
		var height = bounds.height;
		var width = bounds.width;
		var drawChannel = { arg chanidx, chandata;
			var drawWave = { arg yfac=1;
				var p;
				var offset = chanidx+0.5;
				Pen.moveTo(this.secondPointToPixelPoint(Point(0,0+offset)));
				//Log(\Param).debug("drawWaveform: opening point: %",this.secondPointToPixelPoint(Point(0,0)) );
				block { arg break;

					chandata.do{|y, x|
						y = y[chanidx] ? 0;
						p = this.secondPointToPixelPoint(Point(x/this.resampleRate,y*yfac+offset));
						if(x == 0) {
							//Log(\Param).debug("drawWaveform: first point: %", p );
						};
						//if(x%100==0) { p.debug("p % %".format(chanidx, yfac)); };
						//if(x==0, {Pen.moveTo(p)}, {Pen.lineTo(p)});
						Pen.lineTo(p);
						if(p.x > bounds.right) {
							//Log(\Param).debug("break!");
							break.value
						}
					};
				};
				//Log(\Param).debug("drawWaveform: last point: %",p);
				Pen.lineTo(Point(p.x, this.secondPointToPixelPoint(Point(0,0+offset)).y )); // finish at y center to close the path nicely
				//Log(\Param).debug("drawWaveform: closing point: %",Point(p.x, this.secondPointToPixelPoint(Point(0,0+offset)).y));
				Pen.lineTo(this.secondPointToPixelPoint(Point(0,0+offset)));
			};
			Pen.width = 1;

			drawWave.(1);
			Pen.color = Color.black;
			//Pen.fill;
			Pen.draw(3);

			//drawWave.(1);
			//Pen.color = Color.blue;
			//Pen.stroke;

			drawWave.(-1);
			Pen.color = Color.black;
			////Pen.fill;
			Pen.draw(3);

			//drawWave.(-1);
			//Pen.color = Color.blue;
			//Pen.stroke;
		};
		//Log(\Param).debug("draw waveform: bounds %, areasize %", bounds, this.areasize );
		numChannels.do { arg idx;
			drawChannel.(idx, this.resampledData);
		};
		//Pen.color = Color.red;
		//Pen.strokeRect(bounds);

	}
	
}
