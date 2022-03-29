
~tl.bounds
~tl.virtualBounds
~tl.resizeTo(Rect(0,0,100,100))

~win = Window.new;
~win.layout = VLayout(~tl)
~win.front;
~tl
(
WindowDef(\win, { arg def;
	def.windowDo { arg win;
		win.bounds = Rect(100,100,500,500);
	};
	~tl = TimelineView.new;
	//~tl.virtualBoundsOffset = 15;
	~tl.virtualBoundsOffset = 0;
	VLayout (
		~tl
	).margins_(0).spacing_(0)
	
}).front;
);

/// bounds tests

~tl.bounds == Rect(0,0,500,500);
~tl.virtualBoundsOffset = 0;
~tl.virtualBounds == Rect(0,0,500,500);
~tl.virtualBoundsOffset = 15;
~tl.virtualBounds == Rect(15,15,500-15,500-15);

// conversions

~tl.virtualBoundsOffset = 0;
~tl.normRectToPixelRect(Rect(0,0,1,1)) == Rect(0,0,500,500);
~tl.normRectToPixelRect(Rect(1,1,1,1)) == Rect(500,-500,500,500);
~tl.normRectToPixelRect(Rect(1/4,1/4,3/4,3/4)) == Rect(125,0,375,375); // top right corner rect
~tl.normRectToPixelRect(Rect(1/4,1/4,2/4,2/4)) == Rect(125,125,250,250); // centered rect


~tl.virtualBoundsOffset = 15;

~tl.normRectToPixelRect(Rect(0,0,1,1))

// 15*2 because offset on both side reduce length by offset*2
~tl.normRectToPixelRect(Rect(0,0,1,1)) == Rect(15,15,500-( 15*2 ),500-( 15*2 ));

// don't understand why 15*3, but visually this seems correct
~tl.normRectToPixelRect(Rect(1,1,1,1)) == Rect(500-15,(500-( 15*3 )).neg,500-( 15*2 ),500-( 15*2 )); 

// TODO
~tl.normRectToPixelRect(Rect(1/4,1/4,2/4,2/4)) // centered rect
~tl.normRectToPixelRect(Rect(1,1,1,1))



//// visual inspection
(
WindowDef(\win, { arg def;
	def.windowDo { arg win;
		win.bounds = Rect(100,100,500,500);
	};
	~tl = TimelineView.new;
	//~tl.virtualBoundsOffset = 15;
	~tl.virtualBoundsOffset = 0;
	~tl.customDrawFunc = {
		
		// normal rect

		~tl.virtualBoundsOffset = 0;
		Pen.addRect( ~tl.normRectToPixelRect(Rect(0,0,1,1)) );
		Pen.color = Color.blue;
		Pen.width = 4;
		Pen.stroke;

		Pen.addRect( ~tl.normRectToPixelRect(Rect(1/4,1/4,2/4,2/4)) );
		Pen.color = Color.blue.blue_(0.6);
		Pen.width = 4;
		Pen.stroke;

		// with virtualBoundsOffset

		~tl.virtualBoundsOffset = 55;
		Pen.addRect( ~tl.normRectToPixelRect(Rect(0,0,1,1)) );
		Pen.color = Color.red;
		Pen.width = 4;
		Pen.stroke;

		Pen.addRect( ~tl.normRectToPixelRect(Rect(1/4,1/4,2/4,2/4)) );
		Pen.color = Color.red.red_(0.6);
		Pen.width = 4;
		Pen.stroke;

		// set virtualBounds

		~tl.virtualBounds = Rect(100,100,200,200);
		Pen.addRect( ~tl.normRectToPixelRect(Rect(0,0,1,1)) );
		Pen.color = Color.green;
		Pen.width = 4;
		Pen.stroke;

		Pen.addRect( ~tl.normRectToPixelRect(Rect(1/4,1/4,2/4,2/4)) );
		Pen.color = Color.green.green_(0.6);
		Pen.width = 4;
		Pen.stroke;

	};
	VLayout (
		~tl
	).margins_(0).spacing_(0)
	
}).front;
);


// visual inspection: virtual bounds and frame
(
WindowDef(\win, { arg def;
	def.windowDo { arg win;
		win.bounds = Rect(100,100,500,500);
	};
	~tl = TimelineView.new;
	//~tl.virtualBoundsOffset = 15;
	~tl.virtualBoundsOffset = 30;
	~tl.virtualBounds = Rect(100,100,250,210);
	~tl.areasize = Point(2.123,5.423);
	//~tl.customDrawFunc = {
		

		//// set virtualBounds

		//~tl.virtualBounds = Rect(100,100,200,200);
		//Pen.addRect( ~tl.normRectToPixelRect(Rect(0,0,1,1)) );
		//Pen.color = Color.green;
		//Pen.width = 4;
		//Pen.stroke;

		//Pen.addRect( ~tl.normRectToPixelRect(Rect(1/4,1/4,2/4,2/4)) );
		//Pen.color = Color.green.green_(0.6);
		//Pen.width = 4;
		//Pen.stroke;

	//};
	VLayout (
		~tl
	).margins_(0).spacing_(0)
	
}).front;
);
