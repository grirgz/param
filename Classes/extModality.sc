
+ MKtlElement {
	mapParam { arg param;
		if(param.isNil) {
			this.action = nil;
			this.value = 0;
		} {
			this.action = { arg me;
				param.normSet(me.value);
				Param.lastTweaked = param;
			};
			this.value = param.normGet;
		}
	}

	unmapParam { 
		this.mapParam(nil)
	}

	mapPlayer { arg player, updateAction;
		if(player.isNil) {
			this.action = nil;
			this.value = 0;
		} {
			this.action = { arg me;
				player.togglePlay
			};
			this.getHalo(\ParamListener) !? { arg x; x.remove };
			this.addHalo(\ParamListener, PlayerWrapper(player).makeListener({ arg me, msg, args;
				//[ msg, player, PlayerWrapper(player).isPlaying.asInteger ].debug("MKtlElement.mapPlayer listener: receive");
				if(updateAction.notNil) {
					updateAction.(player, msg, args)
				} {
					this.value = PlayerWrapper(player).isPlaying.asInteger;
				};
			}));
		}
		
	}
	
	unmapPlayer {
		this.mapPlayer(nil)
	}
}

+ MKtlElementGroup {
	mapParam { arg param, toggleMode=false;
		if(param.isNil) {
			this[0].action = nil;
			this[1].action = nil;
			this[0].value = 0;
		} {
			if(param.size > 0) {
				if(this.elements.size > 0) {
					min(this.elements.size, param.size).do { arg idx;
						this[idx].mapParam(param.at(idx))
					};
				} {
					"MKtlElementGroup.mapParam: can't map subparams because not an array of slider".error;
				};
			} {
				if(this.elements.size == 2) { // button with on/off
					if(toggleMode == true) {
						// on
						this[0].action = { arg me; 
							if(param.normGet == 0) {
								param.normSet(1);
							} {
								param.normSet(0);
							};
							Param.lastTweaked = param;
						};
						this[0].value = param.normGet;
						// off
						this[1].action = nil
					} {
						// on
						this[0].action = { arg me; 
							param.normSet(1);
							Param.lastTweaked = param;
						};
						// off
						this[1].action = { arg me;
							param.normSet(0);
							Param.lastTweaked = param;
						};
					}
				} {
					"MKtlElementGroup.mapParam: Not a button".error;
				}
			}
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
