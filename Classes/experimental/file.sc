// not finished, not used

FileSystemResolver {
	var <>paths;
	*new {
		^super.new.paths_(List.new);
	}

	resolve { arg val;
		paths.do({ arg path;
			var pn = PathName(path +/+ val).exists;
			if(pn.exists) {
				^pn.fullPath
			}
		});
	}
}



/////////////////

+PathName {
	exists {
		^this.isFile.not and: { this.isFolder.not }
	}
}
