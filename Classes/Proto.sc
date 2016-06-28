
/////////// just to init proto events code


ParamProto {
	classvar <>base_path;
	classvar <>initialized = false;

	*initClass {
		//base_path = Platform.userExtensionDir +/+ "param/Proto/";
		base_path = PathName(this.filenameSymbol.asString).pathOnly +/+ "../Proto/";
	}

	*init { arg force=false;
		if(initialized.not or: {force == true}) {
			(base_path +/+ "main.scd").load;
			initialized = true;
		}
	}
}


// see also data.sc : ProtoClass

