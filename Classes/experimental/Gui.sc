



//////////////////////////////////////////// DRAFT

XVLayout : VLayout {
	// does not work :( not recognized as a Layout )
	var <>list;
	*new { arg ...items;
		var serializedItems = items.collect( { |x| this.parse(x) } );
		var li;
		var ins;
		li = serializedItems.collect({ arg x; x[0] });
		ins = super.new( [serializedItems] );
		ins.list = li;
		^ins;
	}

	//add { arg item, stretch = 0, align;
	//	this.invokeMethod( \addItem, [[item, stretch, QAlignment(align)]], true );
	//}

	//insert { arg item, index=0, stretch = 0, align;
	//	this.invokeMethod( \insertItem, [[item, index, stretch, QAlignment(align)]], true );
	//}

}



///////////////////////////////


////////////////////////////////





////////////////////////////////////////

