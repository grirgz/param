
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
			this.class.mydraw.debug("mydraw");
			this.class.mydraw.(this);
		} {
			this.drawImageWaveform;
			//this.drawResampledWaveform;
		}
	}

	drawImageWaveform {
		var bounds = this.virtualBounds;
		var height = bounds.height;
		var width = bounds.width;
		if(waveformImage.notNil) {
			// xfactor is displayedSeconds/totalSeconds * imageWidth
			var xfactor = this.areasize.x / ( bufferDuration / this.clock.tempo ) * waveformImage.width;
			//Pen.drawImage(Point(0,0), waveformImage, bounds);

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
