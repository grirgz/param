
BasicButton : Button {
	// need to access to properties individually and setting them independently of state index (.value)
	var <color, <label, <background, myValue=0;

	color_ { arg val;
		color = val;
		this.updateStates;
	}

	background_ { arg val;
		background = val;
		this.updateStates;
	}

	label_ { arg val;
		label = val;
		this.updateStates;
	}

	string_ { arg val;
		this.label = val;
	}

	string { ^this.label }

	value_ { arg val;
		myValue = val;
	}

	value { arg val;
		^myValue;
	}

	updateStates {
		this.states = [[label, color, background]];
	}

	unmapParam {
		Param.unmapView(this);
		this.value = 0;
		this.states = [[""]];
	}

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			this.updateStates;
			param.mapButton(this);
		}
	}

}

BoolButton : Button {
	// need to access to properties individually and setting them independently of state index (.value)
	// FIXME: without setting a label or color, button has only one state, but no way to init from superclass
	var <color, <label, <background;

	color_ { arg val;
		color = val;
		this.updateStates;
	}

	background_ { arg val;
		background = val;
		this.updateStates;
	}

	label_ { arg val;
		label = val;
		this.updateStates;
	}

	string_ { arg val;
		this.label = val;
	}

	string { ^this.label }

	updateStates {
		var old = this.value;
		this.states_([
			[label, color ? Color.black, Color.white],
			[label, color ? Color.black, background ? ParamViewToolBox.color_ligth],
		]);
		this.value = old;
	}

	unmapParam {
		Param.unmapView(this);
		this.value = 0;
		this.states = [[""]];
	}

	mapParam { arg param;
		if(param.isNil) {
			this.unmapParam
		} {
			this.updateStates;
			param.mapButton(this);
		}
	}
}


XSimpleButton : BasicButton {} // backward compat

