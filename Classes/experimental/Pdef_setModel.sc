

+ Pdef {
	setModel { arg fun;
		var model = fun.(this);
		this.addHalo(\model, model);
		this.addUniqueMethod(\model, { model });
		this.addUniqueMethod(\edit, { model.edit });
		this.addUniqueMethod(\asView, { model.asView });
	}
}

+ EventPatternProxy {
	setModel { arg fun;
		var model = fun.(this);
		this.addHalo(\model, model);
		this.addUniqueMethod(\model, { model });
		this.addUniqueMethod(\edit, { model.edit });
		this.addUniqueMethod(\asView, { model.asView });
	}
}
