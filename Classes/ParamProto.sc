
/////////// this class is a ugly hack to have non-class code run inside a quark
/////////// call ParamProto.init to have all non-class code loaded


ParamProto {
	classvar <>base_path;
	classvar <>initialized = false;

	*initClass {
		//base_path = Platform.userExtensionDir +/+ "param/Proto/";
		base_path = PathName(PathName(this.filenameSymbol.asString).pathOnly +/+ "../Proto/").normalizedPath;
	}

	*path {
		^base_path
	}

	*init { arg force=false;
		if(initialized.not or: {force == true}) {
			(base_path +/+ "main.scd").load;
			initialized = true;
		}
	}
}



