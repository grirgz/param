
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
		this.states_([
			[label, color ? Color.black, Color.white],
			[label, color ? Color.black, background ? ParamViewToolBox.color_ligth],
		]);
	}

}


XSimpleButton : BasicButton {} // backward compat

