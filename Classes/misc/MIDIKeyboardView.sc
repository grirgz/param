
// based on Thor Magnusson code - www.ixi-audio.net
// GNU license - google it.

MIDIKeyboardView : SCViewHolder {

	var <>keys; 
	var trackKey, chosenkey, mouseTracker;
	var win, octaves, startnote;
	var downAction, upAction, trackAction;
	var <>userView;
	var pix;
	
	*new { arg win, bounds, octaves, startnote; 
		^super.new.initMIDIKeyboard(win, bounds, octaves, startnote);
	}

	initMIDIKeyboard { arg win, argbounds, argoctaves=3, argstartnote;
		var bounds;
		var pen;
		octaves = argoctaves ? 4;
				
		//if((win= w).isNil, {
		//	win = Window.new("MIDI Keyboard",
		//		Rect(100, 250, bounds.width + 40, bounds.height+30));
		//	win.front
		//});

 		mouseTracker = UserView.new(win, argbounds); // thanks ron!
		//mouseTracker = this;
		this.view = mouseTracker;
		userView = mouseTracker;
		//mouseTracker.debug("bou1ds!!!!!");
		//mouseTracker.bounds.debug("bou1ds!!!!!");
 		bounds = mouseTracker.bounds;
		//bounds.debug("bouds!!!!!");

		pen	= Pen;

		startnote = argstartnote ? 48;
		trackKey = 0;
		//pix = [0, 6, 10, 16, 20, 30, 36, 40, 46, 50, 56, 60];
		pix = [ 0, 0.1, 0.17, 0.27, 0.33, 0.5, 0.6, 0.67, 0.77, 0.83, 0.93, 1 ]; // as above but normalized

		this.buildKeys;

		mouseTracker
			.canFocus_(false)
			//.relativeOrigin_(false)
			.mouseDownAction_({|me, x, y, mod|
				//[x, y].debug("xy");
				chosenkey = this.findNote(x, y);
				//chosenkey.debug("chosenkey");
				trackKey = chosenkey;
				chosenkey.color = Color.grey;
				downAction.value(chosenkey.note);
				this.refresh;	
			})
			.mouseMoveAction_({|me, x, y, mod|
				chosenkey = this.findNote(x, y);
				if(trackKey.note != chosenkey.note, {
					var oldnote = trackKey.note;
					trackKey.color = trackKey.scalecolor; // was : type
					trackKey = chosenkey;
					chosenkey.color = Color.grey;
					trackAction.value(chosenkey.note, oldnote);
					this.refresh;
				});
			})
			.mouseUpAction_({|me, x, y, mod|
				chosenkey = this.findNote(x, y);
				trackKey = chosenkey;
				chosenkey.color = chosenkey.scalecolor; // was:  type
				upAction.value(chosenkey.note);
				this.refresh;
			})
			.drawFunc_({
				this.refreshKeys;
				octaves.do({arg j;
					// first draw the white keys
					12.do({arg i;
						var key;
						key = keys[i+(j*12)];
						if(key.type == Color.white, {
							pen.color = Color.black;
							pen.strokeRect(Rect(key.rect.left+0.5, key.rect.top+0.5, key.rect.width+0.5, key.rect.height-0.5));
							pen.color = key.color; // white or grey
							pen.fillRect(Rect(key.rect.left+0.5, key.rect.top+0.5, key.rect.width+0.5, key.rect.height-0.5));
						});
					});
					// and then draw the black keys on top of the white
					12.do({arg i;
						var key;
						key = keys[i+(j*12)];
						if(key.type == Color.black, {
							pen.color = Color.black;
							pen.strokeRect(Rect(key.rect.left+0.5, key.rect.top+0.5, key.rect.width, key.rect.height));
							pen.color = key.color;
							pen.fillRect(Rect(key.rect.left+0.5, key.rect.top+0.5, key.rect.width, key.rect.height));
						});
					})
				})
			});
	}
	
	bounds { 
		^this.view.bounds
	}

	buildKeys {
		var r;
		var bounds = this.bounds;
		keys = List.new;
		
		octaves.do({arg j;
			12.do({arg i;
				if((i == 1) || (i == 3) || (i == 6) || (i == 8) || (i == 10), { // black keys
					r = Rect(	( (pix[i]*((bounds.width/octaves) -
								(bounds.width/octaves/7))).round(1) + ((bounds.width/octaves)*j)).round(1)+1,
							0, 
							bounds.width/octaves/10, 
							bounds.height/1.7);
					keys.add(XMIDIKey.new(startnote+i+(j*12), r, Color.black));
				}, { // white keys
					r = Rect(((pix[i]*((bounds.width/octaves) -
								(bounds.width/octaves/7))).round(1) + ((bounds.width/octaves)*j)).round(1),
							0, 
							bounds.width/octaves/7, 
							bounds.height);
					keys.add(XMIDIKey.new(startnote+i+(j*12), r, Color.white));
				});
			});
		});

	}

	refreshKeys {
		// TODO: XMIDIKey should be rewritten to have normalized coordinates instead of this hack function
		var r;
		var bounds = this.bounds;
		
		octaves.do({arg j;
			12.do({arg i;
				if((i == 1) || (i == 3) || (i == 6) || (i == 8) || (i == 10), { // black keys
					r = Rect(	( (pix[i]*((bounds.width/octaves) -
								(bounds.width/octaves/7))).round(1) + ((bounds.width/octaves)*j)).round(1)+1,
							0, 
							bounds.width/octaves/10, 
							bounds.height/1.7);
					keys[i+(j*12)].rect = r;
				}, { // white keys
					r = Rect(((pix[i]*((bounds.width/octaves) -
								(bounds.width/octaves/7))).round(1) + ((bounds.width/octaves)*j)).round(1),
							0, 
							bounds.width/octaves/7, 
							bounds.height);
					keys[i+(j*12)].rect = r;
				});
			});
		});

	}
	
	refresh {
		mouseTracker.refresh;
	}
	
	keyDown { arg note, color; // midinote
		if(note.isArray, {
			note.do({arg note;
				if(this.inRange(note), {
					keys[note - startnote].color = Color.grey;
				});
			});
		}, {
			if(this.inRange(note), {
				keys[note - startnote].color = Color.grey;
			});			
		});
		this.refresh;
	}
	
	keyUp { arg note; // midinote
//		if(this.inRange(note), {
//			keys[note - startnote].color = keys[note - startnote].scalecolor;
//		});
		if(note.isArray, {
			note.do({arg note;
				if(this.inRange(note), {
					keys[note - startnote].color = keys[note - startnote].scalecolor;
				});
			});
		}, {
			if(this.inRange(note), {
				keys[note - startnote].color = keys[note - startnote].scalecolor;
			});			
		});
		this.refresh;	
	}
	
	keyDownAction_ { arg func;
		downAction = func;
	}
	
	keyUpAction_ { arg func;
		upAction = func;
	}
	
	keyTrackAction_ { arg func;
		trackAction = func;
	}
	
	showScale {arg argscale, key=startnote, argcolor;
		var color, scale, counter, transp;
		this.clear; // erase scalecolors (make them their type)
		counter = 0;
		color = argcolor ? Color.red;
		transp = key%12;
		scale = argscale + transp + startnote;		
		keys.do({arg key, i;
			key.color = key.type; // back to original color
			if(((i-transp)%12 == 0)&&((i-transp)!=0), { counter = 0; scale = scale+12;});			if(key.note == scale[counter], {
				counter = counter + 1; 
				key.color = key.color.blend(color, 0.5);
				key.scalecolor = key.color;
				key.inscale = true;
			});
		});
		this.refresh;
	}
	
	clear {
		keys.do({arg key, i;
			key.color = key.type; // back to original color
			key.scalecolor = key.color;
			key.inscale = false;
		});
		this.refresh;
	}
	
	// just for fun
	playScale { arg argscale, key=startnote, int=0.5;
		var scale = argscale;
		SynthDef(\midikeyboardsine, {arg freq, amp = 0.25;
			Out.ar(0, (SinOsc.ar(freq,0,amp)*EnvGen.ar(Env.perc, doneAction:2)).dup)
		}).load(Server.default);
		Task({
			scale.mirror.do({arg note;
				Synth(\midikeyboardsine, [\freq, (key+note).midicps]);
				int.wait;
			});		}).start;
	}
	
	setColor {arg note, color;

		var newcolor = keys[note - startnote].color.blend(color, 0.5);
		keys[note - startnote].color = newcolor;
		keys[note - startnote].scalecolor = newcolor;
		this.refresh;
	}
	
	getColor { arg note;
		^keys[note - startnote].color;
	}
	
	getType { arg note;
		^keys[note - startnote].type;
	}

	
	removeColor {arg note;
		keys[note - startnote].scalecolor = keys[note - startnote].type;
		keys[note - startnote].color = keys[note - startnote].type;
		this.refresh;
	}
	
	inScale {arg key;
		^keys[key-startnote].inscale;
	}
	
	retNote {arg key;
		^keys[key].note;
	}
	
	remove {
		mouseTracker.remove;
		win.refresh;
	}
	
	// local function
	findNote {arg x, y;
		var chosenkeys;
		chosenkeys = [];
		keys.reverse.do({arg key;
			if(key.rect.containsPoint(Point.new(x,y)), {
				chosenkeys = chosenkeys.add(key);
			});
		});
		block{|break|
			chosenkeys.do({arg key;
				if(key.type == Color.black, { 
					chosenkey = key; 
					break.value; // the important part
				}, {
					chosenkey = key; 
				});
			});
		};
		^chosenkey;
	}
	
	// local
	inRange {arg note; // check if an input note is in the range of the keyboard
		if((note>startnote) && (note<(startnote + (octaves*12))), {^true}, {^false});
	}
	
	mapPattern { arg pattern, mode = \pre;
		var player;
		ParamProto.init;
		player = ProtoTemplateDef(\SeqPlayerGroup).new(pattern);
		player.chainingMode = mode;
		this.mapPlayer(player);
	}

	mapPlayer { arg player; // player should have .elAt interface
		this.keyDownAction = { arg midinote, old;
			if(old.notNil) {
				player.elAt(old).stop;
			};
			player.elAt(midinote).play;
		};
		this.keyTrackAction = { arg midinote, old;
			player.elAt(old).stop;
			player.elAt(midinote).play;
		};
		this.keyUpAction = { arg midinote;
			player.elAt(midinote).stop;
		};
		
	}


}

XMIDIKey {
	var <>rect, <>color, <note, <type;
	var <>scalecolor, <>inscale;
	
	*new { arg note, rect, type; 
		^super.new.initMIDIKey(note, rect, type);
	}
	
	initMIDIKey {arg argnote, argrect, argtype;
		note = argnote;
		rect = argrect;
		type = argtype;
		color = argtype;
		scalecolor = color;
		inscale = false;
	}	

}
