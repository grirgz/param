
+ MKtlElement {
	mapParam { arg param;
		if(param.isNil) {
			this.action = nil
		} {
			this.action = { arg me;
				param.normSet(me.value)
			};
		}
	}

	unmapParam { 
		this.mapParam(nil)
	}
}

+ MKtlElementGroup {
	mapParam { arg param;
		if(this.elements.size == 2) { // button with on/off
			if(param.isNil) {
				this[0].action = nil;
				this[1].action = nil;
			} {
				// on
				this[0].action = { arg me; 
					param.normSet(1)
				};
				// off
				this[1].action = { arg me;
					param.normSet(0)
				};
			}
		} {
			"MKtlElementGroup.mapParam: Not a button".error;
		}
	}

	unmapParam { 
		this.mapParam(nil)
	}
}


/*
MKtl('icon', "icon-icontrols")[\kn][0].elemDesc;
MKtl('icon', "icon-icontrols")[\tr][0][0].type
MKtl('icon', "icon-icontrols")[\tr][0].elements
*/
