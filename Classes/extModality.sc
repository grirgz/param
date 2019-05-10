
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

	mapPlayer { arg player;
		if(player.isNil) {
			this.action = nil
		} {
			this.action = { arg me;
				player.togglePlay
			};
		}
		
	}
	
	unmapPlayer {
		this.mapPlayer(nil)
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

	mapPlayer { arg player;
		if(this.elements.size == 2) { // button with on/off
			if(player.isNil) {
				this[0].action = nil;
				this[1].action = nil;
			} {
				// on
				this[0].action = { arg me; 
					if(player.playMode == \gate) {
						player.play
					} {
						player.togglePlay
					}
				};
				// off
				this[1].action = { arg me;
					if(player.playMode == \gate) {
						player.stop
					}
				};
			}
		} {
			"MKtlElementGroup.mapPlayer: Not a button".error;
		}
		
	}

	mapPlayerByMode { arg player, mode=\gate;
		// this map player in gate mode by default
		if(this.elements.size == 2) { // button with on/off
			if(player.isNil) {
				this[0].action = nil;
				this[1].action = nil;
			} {
				// on
				this[0].action = { arg me; 
					if(mode.value == \gate) {
						player.play
					} {
						player.togglePlay
					}
				};
				// off
				this[1].action = { arg me;
					if(mode.value == \gate) {
						player.stop
					}
				};
			}
		} {
			"MKtlElementGroup.mapPlayerByMode: Not a button".error;
		}
	}

	unmapPlayer {
		this.mapPlayer(nil)
	}

}


/*
MKtl('icon', "icon-icontrols")[\kn][0].elemDesc;
MKtl('icon', "icon-icontrols")[\tr][0][0].type
MKtl('icon', "icon-icontrols")[\tr][0].elements
*/
