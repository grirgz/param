
XSamplePlaceholder {
	// used to store the path and numchannel when storing to disk
	// TODO: look at crucial Sample

	var <>path, <>numChannels;

	*new { arg path, numChannels=2;
		^super.newCopyArgs(path, numChannels)
	}

	//init { arg xpath, xnumChannels; 
	//	path = xpath;
	//	numChannels = xnumChannels;
	//}

	storeArgs { arg stream; 
		^[path,numChannels]     
	}


	printOn { arg stream;
		this.storeOn(stream); // storeOn call storeArgs
	}

	//storeOn { arg stream; 
	//	stream << "MyTestPoint.new(" << path << ", " << numChannels << ")";    
	//}

	load {
		if(path.notNil) {
			^BufferPool.get_stereo_sample(\param, path); // TODO: use numChannels
		} {
			debug("XSamplePlaceholder: error, path is nil");
			^\error_path_is_nil
		}
	}
}
