
ModKnob : SCViewHolder {

	var <>x_offset=0;
	var <>y_offset=0;
	var <>value_offset=0;
	var <value = 0;
	var <>midi_value = nil;
	var <>range1 = 0, <>range2 = 0, <>range3 = 0;
	var <>polarity1, <>polarity2, <>polarity3;
	var <valueAction;
	var <keyDownAction;
	var <>mouse_edit_pixel_range = 1000;
	var <>color1, <>color2, <>color3;

	*new { |argParent, argBounds|
			^super.new.prInit( argParent, argBounds );
	}

	valueAction_ { arg val;
		//val.debug("ModKnob: valueAction: val");
		this.value = val;
		//this.value.debug("ModKnob: valueAction: value");
		this.action.value;
	
	}

	value_ { arg val;
		value = val;
		this.refresh;
	}

	set_range { arg idx, val;
		switch(idx,
			0, { range1 = val },
			1, { range2 = val },
			2, { range3 = val }
		)
	}

	get_range { arg idx, val;
		^switch(idx,
			0, { range1 },
			1, { range2 },
			2, { range3 }
		)
	}

	set_polarity { arg idx, val;
		switch(idx,
			0, { polarity1 = val },
			1, { polarity2 = val },
			2, { polarity3 = val }
		)
	}

	keyDownAction_ { arg fun;
		this.view.keyDownAction = fun;
	}

	prInit { |argParent, argBounds|

		color1 = Color.green;
		color2 = Color.red;
		color3 = Color.blue;

		//this.view = UserView.new(argParent, argBounds.insetBy(20,20));
		this.view = UserView.new(argParent, argBounds);
		this.view.minSize = Point(30,30);

		this.view.mouseDownAction =  { arg view, x, y;
			this.x_offset = x;
			y_offset = y;
			this.value_offset = this.value;
			//"down".postln;
		};

		this.view.mouseMoveAction =  { arg view, x, y;
			var ro, nx;
			var pixel_range;
			y = y - y_offset;
			pixel_range = (mouse_edit_pixel_range-(y.abs*4)).clip(20,20000);
			//[y, pixel_range].debug("y, pixel_range");
			//[x, x - ~x, ~x, ro].debug("x, x-~x, ~x");
			nx = x - this.x_offset;
			//"before doubleclip".debug;
			ro = ((nx/pixel_range) + this.value_offset).clip(0, 0.999 );
			//"after doubleclip".debug;
			
			this.valueAction = ro;
			this.refresh;
		};
		this.view.drawFunc = { arg view;
			var bounds = view.bounds;
			var start_angle = (5/8*pi);
			var pos, angle;
			var midi_pos, midi_angle;
			var draw_arc;

			var rayon = [2.3, 2.6, 3.0];
			var full_len; 
			var move_angle = 14/8*pi;
			var mysize;

			draw_arc = { arg range, pos, polarity, radius, color;
				var len, ilen;
				if(range.notNil) {

					if(polarity.notNil and: {polarity == \bipolar}) {
						range = range/2;
					};
					//range.debug("modknob: range");
					range = range.clip2(1);
					if(range < 0) {
						ilen = range.clip2(1 - pos) * move_angle;
						len = range.clip2(pos) * move_angle;
					} {
						len = range.clip2(1 - pos) * move_angle;
						ilen = range.clip2(pos) * move_angle;
					};

					Pen.color = Color.gray(0.8);
					Pen.addArc(bounds.extent/2, radius, start_angle,full_len);
					Pen.stroke;

					//polarity.debug("POLARITY");
					if(polarity.isNil) {
						polarity = \unipolar;
					};
					if(polarity == \bipolar) {
						Pen.color = color.copy.alpha_(0.5);
						Pen.addArc(bounds.extent/2, radius, angle,0-ilen);
						Pen.stroke;
					};

					Pen.color = color;
					Pen.addArc(bounds.extent/2, radius, angle,len);
					Pen.stroke;


					if(range > (1-pos)) {
						Pen.addArc(bounds.extent/2, radius, move_angle+start_angle+0.1,0.1);
					};

					if(((range < 0) or: { polarity == \bipolar }) and: {range.abs > (pos)}) {
						Pen.addArc(bounds.extent/2, radius, start_angle-0.1,-0.1);
					};
					Pen.stroke;
				}
			
			};

			//this.value.debug("modknob: value");
			pos = this.value.clip2(1);
			angle = ((pos*move_angle)+start_angle) % 2pi;
			full_len = move_angle;
			bounds = (0@0) @ bounds.extent;
			mysize = min(bounds.height, bounds.width);

			//////////////// regular knob
			//"QUOI__0".debug;

			Pen.color = Color.black;
			Pen.addArc(bounds.extent/2, mysize/4.0, 0,2pi);
			Pen.stroke;
			Pen.addAnnularWedge(bounds.extent/2, mysize/9.0, mysize/4.0, angle,0.1);
			Pen.stroke;

			//"QUOIOO1".debug;

			// midi position

			if(this.midi_value.notNil) {
				midi_pos = this.midi_value.clip2(1);
				midi_angle = ((midi_pos*move_angle)+start_angle) % 2pi;
				Pen.color = Color.blue(0.9);
				Pen.addAnnularWedge(bounds.extent/2, mysize/5.0, mysize/4.0, midi_angle,0.1);
				Pen.stroke;
			};

			//"QUOI__2".debug;

			//////////////// range knobs

			Pen.width = 2;

			draw_arc.(range1, pos, polarity1, mysize/rayon[2], color1);
			draw_arc.(range2, pos, polarity2, mysize/rayon[1], color2);
			draw_arc.(range3, pos, polarity3, mysize/rayon[0], color3);
			//"QUOI__3".debug;

		};
		this.view.refresh;
	}

	*doubleclip { arg val, mi, ma;
		//"in doubleclip".debug;
		if(val < mi) {
			val = mi;
		} {
			if(val > ma) {
				val = ma;
			}
		};
		^val;

	}

}

ModSlider : SCViewHolder {

	var <>x_offset=0;
	var <>value_offset=0;
	var <value = 0;
	var <>midi_value = nil;
	var <>range1 = 0, <>range2 = 0, <>range3 = 0;
	var <>polarity1, <>polarity2, <>polarity3;
	var <valueAction;
	var <>slider;
	var <>rangeview;
	var is_vertical = true;

	*new { |argParent, argBounds|
			^super.new.prInit( argParent, argBounds );
	}

	valueAction_ { arg val;
		this.value = val;
		this.action.value;
	
	}

	value_ { arg val;
		value = val;
		this.slider.value = val;	
		this.rangeview.refresh;
	}

	refresh {
		this.rangeview.refresh;
	}

	set_range { arg idx, val;
		switch(idx,
			0, { range1 = val },
			1, { range2 = val },
			2, { range3 = val }
		)
	}

	set_polarity { arg idx, val;
		switch(idx,
			0, { polarity1 = val },
			1, { polarity2 = val },
			2, { polarity3 = val }
		)
	}

	make_widgets { arg argParent, argBounds;
		var slider_rect, range_rect;
		if(is_vertical) {
			this.view = HLayoutView.new(argParent, argBounds);
			slider_rect = Rect(15,0,10,argBounds.height);
			range_rect = Rect(0,0,15,argBounds.height);
		} {
			this.view = VLayoutView.new(argParent, argBounds);
			slider_rect = Rect(0,0,argBounds.width, 10);
			range_rect = Rect(0,0,argBounds.width, 15);
		};

		this.slider = Slider.new(this.view, slider_rect);
		this.rangeview = UserView.new(this.view, range_rect);

	}

	prInit { |argParent, argBounds|

		var layout;
		var penline;
		var slider_rect, range_rect;

		//"soul".debug;
		argBounds = argBounds ?? Rect(0,0,100,20);

		if(argBounds.height < argBounds.width) {
			is_vertical = false;
		};

		this.make_widgets(argParent, argBounds);

		//if(is_vertical) {
		//	this.view = HLayoutView.new(argParent, argBounds);
		//	slider_rect = Rect(15,0,10,argBounds.height);
		//	range_rect = Rect(0,0,15,argBounds.height);
		//} {
		//	this.view = VLayoutView.new(argParent, argBounds);
		//	slider_rect = Rect(0,0,argBounds.width, 10);
		//	range_rect = Rect(0,0,argBounds.width, 15);
		//};

		//this.slider = Slider.new(this.view, slider_rect);
		//this.rangeview = UserView.new(this.view, range_rect);

		this.slider.action = {
			value = this.slider.value;
			this.action.value;
			this.rangeview.refresh;
		};

		this.rangeview.drawFunc={|uview|
			var bounds = uview.bounds;
			var pos = 0.9;
			var draw_band;
			var start, end, len;
			var mysize;
			//"soul2".debug;
			bounds = (0@0) @ bounds.extent;
			mysize = max(bounds.height, bounds.width);
			start = 10;
			end = mysize-10;
			len = end-start;
			pos = this.slider.value;
			pos = pos.clip(0, 1);

			//[start, pos, len, end].debug("blayyyii");

			draw_band = { arg offset, range, polarity, color;
				var nstart;
				var nend;
				offset = offset + 1;
				if(range.notNil) {

					range = range.clip2(1);
					Pen.width = 2;

					if(polarity == \bipolar) {
						range = range/2;
					};

					nstart = (start+((1-(pos+range))*len));
					nend = (end-(pos*len));
					//[nstart, start, pos, len, start, end].debug("blaii");

					if( nstart < start) {
						Pen.color = color;
						nstart = start;
						Pen.line(offset@(start-5), offset@(start-2));
						Pen.stroke;
					};
					if( nstart > end ) {
						Pen.color = color;
						nstart = end;
						Pen.line(offset@(end+5), offset@(end+2));
						Pen.stroke;

					};
					
					Pen.color = Color.gray(0.8);
					Pen.line(offset@start, offset@end);
					Pen.stroke;

					Pen.color = color;
					//[mysize, len, start, end, ((pos-range)*len), start+((pos-range)*len)].debug;
					Pen.line(offset@nend, offset@nstart);
					Pen.stroke;

					if(polarity == \bipolar) {
						range = 0-range;
						nstart = (start+((1-(pos+range))*len));
						nend = (end-(pos*len));

						if( nstart < start) {
							Pen.color = color;
							nstart = start;
							Pen.line(offset@(start-5), offset@(start-2));
							Pen.stroke;
						};
						if( nstart > end ) {
							Pen.color = color;
							nstart = end;
							Pen.line(offset@(end+5), offset@(end+2));
							Pen.stroke;

						};

						Pen.color = color.copy.alpha_(0.5);

						Pen.line(offset@nend, offset@nstart);
						Pen.stroke;
					};
				};


			};

			if(is_vertical.not) {
				Pen.translate(mysize+4,0);
				Pen.rotate(1/4*2*pi,0,0);
			};

			draw_band.(0, range1, polarity1, Color.red);
			draw_band.(5, range2, polarity2, Color.blue);
			draw_band.(10, range3, polarity3, Color.green);
			


		};
	}

}

ModLayoutSlider : ModSlider {

	make_widgets { arg argParent, argBounds;
		var slider_rect, range_rect;
		var layout;
		if(is_vertical) {
			layout = HLayout.new;
			slider_rect = Rect(15,0,10,argBounds.height);
			range_rect = Rect(0,0,15,argBounds.height);
		} {
			layout = VLayout.new;
			slider_rect = Rect(0,0,argBounds.width, 10);
			range_rect = Rect(0,0,argBounds.width, 15);
		};

		this.view = View.new;
		this.view.layout = layout;

		
		this.slider = Slider.new;
		this.rangeview = UserView.new;

		layout.add(this.slider, stretch:1);
		layout.add(this.rangeview, stretch:1);

	}

}
