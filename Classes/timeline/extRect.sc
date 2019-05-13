
///////////////////////////////////////

+ Rect {
	flipY {
		^Rect(this.origin.x, this.origin.y, this.extent.x, 0-this.extent.y);
	}

	flipScreen { arg screen_height;
		^Rect(this.origin.x, screen_height - (this.origin.y+this.extent.y), this.extent.x, this.extent.y);
	}

	translate { arg point;
		^Rect(this.origin.x + point.x, this.origin.y + point.y, this.width, this.height)
	}

	scale { arg point;
		^Rect(this.origin.x * point.x, this.origin.y * point.y, this.width * point.x, this.height * point.y)
	}
}

