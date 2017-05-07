
///////////////////////// special control view

SampleChooserView : SCViewHolder {
	var <>controller;
	var <>waveview, <>pathfield;
	var <buffer;
	var <>layout;

	*new { arg buf;
		^super.new.init(buf);
	}

	mapParam { arg param;
		param.makeSimpleController(this.view, 
			action: { arg view, param;
				"this is action!!!!!!!".debug;
				param.set(this.buffer);
			}, 
			updateAction: { arg view, param;
				{
					this.buffer = param.get;
				}.defer;
			},
			initAction: nil,
			//customAction: action
		)

	}

	unmapParam {
		this.getHalo(\simpleController).remove;
	}

	init { arg buf;
		this.makeView;
		if(buf.notNil) {
			this.buffer = buf;
		};
	}

	makeView { arg self;
		this.view = View.new;
		pathfield = TextField.new;
		waveview = SoundFileView.new;
		layout = VLayout(
			waveview,
			HLayout(
				pathfield,
				Button.new.action_( {
					Dialog.openPanel({ arg path;
						path.postln;
						path.debug( "filedialog: set buf" );
						this.buffer = SampleProxy(path);
						"bientot action".debug;
						this.view.action.(this);
					},{
						"cancelled".postln; 
					});
				})
			)
		);
		this.view.layout = layout;
		this.view.addUniqueMethod(\action_, { arg view, val; val.debug("fuckset"); this.action = val; }); // compat with makeSimpleController
		this.view.addUniqueMethod(\action, { arg view, val;"fuckget".debug; this.action });
	}

	buffer_ { arg buf;
		buf.debug("set buffer");
		if(buf.notNil and: { buf.respondsTo(\bufnum) and: {buffer != buf} }) {
			if(buf.path.notNil) {
				var soundfile;
				buffer = buf;
				pathfield.string = buf.path;
				soundfile = SoundFile(buf.path);
				waveview.readFile(soundfile);
				soundfile.close;
			} {
				// TODO
				pathfield.string = buf.asCompileString
			}
		}
	}

	value_ { arg val;
		this.buffer = val;
	}

	value {
		^this.buffer;
	}

}
