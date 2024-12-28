
WindowDef(\SoundFileView, (

	compute_position: { arg self;
		var cur;
		var positions;
		var sfview = self.sfview;
		var sf = self.sf;
		cur = sfview.selections[sfview.currentSelection];
		// cur = [start, size] in frames
		positions = [
			cur++(cur[0]+cur[1]),					// frames
			cur++(cur[0]+cur[1]) / s.sampleRate,	// seconds
			cur++(cur[0]+cur[1]) / sf.numFrames,	// normalized
		];
		//"Current selection is now [start, size, end]:\nframes: %\nseconds: %\nnormalized: %".format(
			//*positions
		//).postln;
		self.positions = positions;
	},

	post_position: {  arg self;
		self.compute_position;
		"Current selection is now [start, size, end]:\nframes: %\nseconds: %\nnormalized: %".format(
			*self.positions
		).postln;
	},

	update_selection: { arg self;
		var sustain, start;
		var speed;
		var conv = self.accessor;
		var startParam = self.startParam, sustainParam = self.sustainParam, endParam = self.endParam;
		var speedParam = self.speedParam;
		var sampleRate = self.sampleRate;
		var sfview = self.sfview;
		var clock = self.tempoClock;
		//"0".debug("xxx");
		//[startParam.get].debug("update_selection: start really before");
		//[startParam.get, conv.start.get, sfview.selections[sfview.currentSelection]].debug("update_selection: start really before");
		//"0.2".debug("xxx");
		if(startParam.notNil) {

			speed = if(speedParam.notNil) {
				speedParam.get;
			} {
				1
			};
			sustain = if(sustainParam.notNil) {
				[conv.sustain.get, clock.tempo, sampleRate].debug("xxx");
				conv.sustain.get * sampleRate * speed.abs;
			} {
				//"1".debug("xxx");
				if(endParam.notNil) {
					endParam.get - conv.start.get * sampleRate;
				} {
					1
				}
			};
			//"1.2".debug("xxx");
			//[startParam.get, conv.start.get].debug("update_selection: start before");
			start = conv.start.get * sampleRate;
			//"2".debug("xxx");
			//(
				//[conv.start.get, conv.sustain.get, conv.start.get + conv.sustain.get] 
				//++ ( [conv.start.get, conv.sustain.get, conv.start.get + conv.sustain.get] * sampleRate)
				//++ ( [
					//( conv.start.get + conv.sustain.get * sampleRate ) - ( conv.start.get * sampleRate ), 
					//( conv.start.get + conv.sustain.get * sampleRate ) - ( conv.start.get * sampleRate ) / sampleRate, 
				//])
			//).debug("start, sus, end, srstart, srsus, srend");
			//"3".debug("xxx");
			sfview.setSelection(sfview.currentSelection, 
				if(speed < 0) {
					[start - sustain, sustain]
				} {
					[start, sustain]
				}
			);
		}
	},

	asPresetCompileString: { arg self;
		//"Pbind(\n%%%);".format(
		//	"\t'instrument', \\playersec,\n",
		//	"\t'bufnum', BufDef(%),\n".format(bufdef.key.asCompileString),
		//	[\pos, \speed, \sustain, \dur, \gain].collect({ arg key;
		//		"\t%, %,\n".format(key.asCompileString, ( Pdef(playerkey).get(key) ? 1 ).asCompileString )
		//	}).join;
		//).pbcopy;
	},

	compute_params: { arg self;
		var speed;
		var sustain;
		var dur;
		var conv = self.accessor;
		var startParam = self.startParam, sustainParam = self.sustainParam, endParam = self.endParam;
		var speedParam = self.speedParam;
		var sampleRate = self.sampleRate;
		var sfview = self.sfview;
		var positions;
		var clock = self.tempoClock;
		self.compute_position;
		positions = self.positions;
		speed = if(speedParam.notNil) {
			speedParam.get;
		} {
			1
		};
		if(startParam.notNil) {
			//[startParam.get, conv.start.get, positions[1][0]].debug("compute_params: start");
			conv.start.set( if(speed < 0) {
				positions[1][2]
			} {
				positions[1][0];
			});
			//[startParam.get, conv.start.get, positions[1][0]].debug("compute_params: start after");
		};
		sustain = positions[1][1] / speed.abs;
		if(sustain == 0) {
			if(sustainParam.notNil) {
				sustain = conv.sustain.get;
			} {
				sustain = 1;
			}
		};
		if(sustainParam.notNil) {
			conv.sustain.set(sustain);
		};
		//Pdef(playerkey).set(\dur, Pdef(playerkey).get(\sustain) * TempoClock.default.tempo);
	},

	makeListeners: { arg self;
		var startParam = self.startParam, sustainParam = self.sustainParam, endParam = self.endParam;
		var speedParam = self.speedParam;
		var sfview = self.sfview;

		if(self.bufferParam.notNil) {
			self.bufferParam.onChange({
				self.compute_params;
				self.update_selection;
				self.openFile(self.bufferParam);
			}, sfview);
		};

		if(speedParam.notNil) {
			speedParam.onChange({
				self.compute_params;
				self.update_selection;
			}, sfview);
		};

		if(startParam.notNil) {
			startParam.onChange({
				self.update_selection;
			}, sfview);
		};

		if(endParam.notNil) {
			endParam.onChange({
				self.update_selection;
			}, sfview);
		};

		if(sustainParam.notNil) {
			sustainParam.onChange({
				self.update_selection;
			}, sfview);
		};

		sfview.mouseUpAction = { arg a;
			self.post_position;
			self.compute_params;
			self.asPresetCompileString.postln;
		};

	},

	openFile: { arg self, file;
		var path;
		if(file.isKindOf(Param)) {
			var val, key;
			self.bufferParam = file;
			val = self.bufferParam.get;
			if(val.isKindOf(Number)) {
				key = self.bufferParam.spec.tagSpec.unmapKey(val);
				if(key.notNil) {
					file = BufDef(key)
				} {
					Log(\Param).error("Error: can't retrieve file name for buffer %", self.bufferParam.get);
					file = nil;
				}
			} {
				file = val;
			};
		};
		if(file.isKindOf(Buffer)) {
			path = file.path ?? { 
				var pa;
				//"ERROR: buffer has not be saved: %".format(file);
				//BufDef(\empty, "empty.wav").path 
				pa = "/tmp/sc_tmp_buffer_%.flac".format(file.hash);
				file.write(pa);
				file.path = pa;
				pa;
			};
		} {
			path = file;
		};
		if(path.notNil) {
			self.sf.openRead(path);
			self.sfview.soundfile = self.sf;
			self.sfview.read(0, self.sf.numFrames);
		}
	},

	makeAccessor: { arg self;
		var startParam = self.startParam, sustainParam = self.sustainParam, endParam = self.endParam;
		var speedParam = self.speedParam;
		var sampleRate = self.sampleRate;
		var clock = self.tempoClock;
		var sf = self.sf;
		var conv;

		// SoundFileView selection is in seconds
		// can't always use param spec because spec range is not same as buffer duration
		//[startParam, sustainParam, endParam, sf, sf.duration, clock, clock.tempo].debug("makeAccessor");
		//[self.startType, self.sustainType, self.endType].debug("makeAccessor: types");
		//self.sustainType.debug("sustainType");
		conv = [self.startType, self.sustainType, self.endType].collect({ arg ptype;
			{ arg pa;
				ProtoClass((
					get: switch(ptype,
						\seconds, { { pa.get } },
						\beats, { { pa.get / clock.tempo } },
						\unipolar, { { pa.get * sf.duration } },
						\spec, { { pa.normGet * sf.duration } },
						{ { pa.normGet * sf.duration } },
					),
					set: switch(ptype,
						\seconds, { { arg se, x; [pa,x].debug("set seconds"); pa.set(x) } },
						\beats, { { arg se, x; pa.set(x * clock.tempo) } },
						\unipolar, { { arg se, x; [pa,x].debug("set unipolar"); pa.set(x / sf.duration) } },
						\spec, { { arg se, x; pa.normSet(x / sf.duration) } },
						{ { arg se, x; pa.normSet(x / sf.duration) } },
					),
				))
			}
		});

		conv = (
			start: conv[0].(startParam),
			sustain: conv[1].(sustainParam),
			end: conv[2].(endParam),
		);
		//[conv[0].(startParam), conv[0].(startParam).get].debug("makeAccessor xxxi");
		//[startParam.get].debug("makeAccessor xxx");
		self.accessor = conv;
	},

	asView: { arg self, def, file, startParam, sustainParam, endParam, speedParam, startType, sustainType, endType;
		var player;
		var win, sf, sfview;
		var post_position, compute_position;
		var vlayout;
		var zoom_slider;
		var task;
		var playerkey = \smpslicerplayer;
		var path;
		var positions;
		var compute_params;
		var controller;
		var asPresetCompileString;
		var update_selection;
		var sampleRate = s.sampleRate;
		var clock = TempoClock.default;
		var conv; // conversion functions

		//BufDef(spec.tagSpec.unmapKey(param.get)),

		self.tempoClock = clock;
		self.sampleRate = sampleRate;

		self.startParam = startParam;
		self.sustainParam = sustainParam;
		self.endParam = endParam;
		self.speedParam = speedParam;

		self.sustainType = sustainType;
		self.startType = startType;
		self.endType = endType;

		vlayout = VLayout.new;
		sfview = SoundFileView.new;
		sf = SoundFile.new;
		self.sfview = sfview;
		self.sf = sf;

		//path.debug("soundfileview: file path");
		self.openFile(file);

		sfview.elasticMode = true;
		sfview.timeCursorOn = true;
		sfview.timeCursorColor = Color.red;
		sfview.timeCursorPosition = 0;
		sfview.drawsWaveForm = true;
		sfview.gridOn = false;
		sfview.gridResolution = 1;
		//~sfview = sfview;

		sfview.action = {
			//"yeahhh".debug;
			//sfview.scrollPos.debug("scrollPos");
		};

		sfview.currentSelection = 1;
		sfview.setSelectionColor(1, ParamViewToolBox.color_ligth);


		self.makeAccessor;


		self.post_position;

		// init selection
		self.update_selection;

		self.makeListeners;

		sfview.mouseUpAction = { arg a;
			self.post_position;
			self.compute_params;
			self.asPresetCompileString.postln;
		};

		sfview.keyDownAction = { arg view, char, modifiers, u, k; 
			var cur;
			var lastFrame;
			cur = view.selections[view.currentSelection];
			lastFrame = if(cur[1] == 0) { sf.numFrames } {cur[0]+cur[1]};
			//[char, modifiers, u, k].debug("KEYBOARD INPUT");
			if( u == 32 ) { // space
				if(player.notNil) {
					player.stop;
					player = nil;
				};
				player = sf.play(
					(
						firstFrame: cur[0],
						lastFrame: lastFrame
					)

				);
				if(task.notNil) {
					task.stop;
				};
				task = Task({
					var start;
					var x = 0;
					//[ sfview.timeCursorPosition, lastFrame, x, thisThread.beats , start, (thisThread.beats - start) ].debug("tc, lf, xbe, st, -");
					s.latency.wait;
					start = thisThread.beats;
					sfview.timeCursorPosition = cur[0];
					while({sfview.timeCursorPosition < lastFrame and: { x < 1000000 }}) {

						//[ thisThread.beats , start, (thisThread.beats - start) ].debug("be, st, -");
						sfview.timeCursorPosition = cur[0] + ( (thisThread.beats - start) * sf.sampleRate );
						x = x + 1;
						0.02.wait;
					};
					sfview.timeCursorPosition = cur[0];
				}).play(AppClock);
				self.post_position;
			};
			if( u == 13 or: { char == $f }) { // Enter
				if(player.notNil) {
					task.stop;
					sfview.timeCursorPosition = cur[0];
					task = nil;
					player.stop;
					player = nil;
				};
				self.post_position;
			}
		};

		if(def.window.notNil) {
			def.window.view.keyDownAction = { arg view, char, modifiers, u, k; 
				//[char, modifiers, u, k].debug("KEYBOARD INPUT");
				if( u == 27 ) { // Esc
					if(player.notNil) {
						player.stop;
						player = nil;
					};
					view.close();
				};
			};
		};

		sfview
	}

));


WindowDef(\ParamAsSoundFileView, { arg def, param;
	var spec = param.spec;
	var target = param.target;
	var buf = param.get;
	if(spec.isKindOf(ParamAudioBufferSpec)) {
		WindowDef(\SoundFileView).asView(
			//BufDef(spec.tagSpec.unmapKey(param.get)),
			param,
			spec.startParamName !? { Param(target, spec.startParamName) },
			spec.sustainParamName !? { Param(target, spec.sustainParamName) },
			spec.endParamName !? { Param(target, spec.endParamName) },
			spec.speedParamName !? { Param(target, spec.speedParamName) },
			spec.startType ? \unipolar,
			spec.sustainType ? \beats,
			spec.endType ? \unipolar,
		);
	} {
		Slider.new;
		//WindowDef(\soundfileview).asView(
		//BufDef(spec.tagSpec.unmapKey(param.get)),
		//Param(target, \pos),
		//Param(target, \sustain),
		//nil,
		//nil,
		//\seconds,
		//\seconds,
		//\seconds,
		//);
	};
});

// compat
WindowDef(\soundfileview, \SoundFileView);
