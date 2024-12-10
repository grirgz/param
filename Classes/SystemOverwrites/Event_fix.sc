
+Event {

	// prevent infinite loop with custom events
	storeOn { arg stream, itemsPerLine = 5;
		var max, itemsPerLinem1, i=0;
		itemsPerLinem1 = itemsPerLine - 1;
		max = this.size;
		stream << "( ";
		this.keysValuesDo({ arg key, val;
			stream <<< key << ": " <<< val;
			if ((i=i+1) < max, { stream.comma.space;
				if (i % itemsPerLine == itemsPerLinem1, { stream.nl.space.space });
			});
		});
		stream << " )";
		//if(proto.notNil) { stream << "\n.proto_(" <<< proto << ")" };
		//if(parent.notNil) { stream << "\n.parent_(" <<< parent << ")" };
	}
}
