
////// Debug

(
WindowDef(\ParamDebugView, { arg def, param;
	var makeDebugView;
	var lay;
	makeDebugView = { arg param;
		GridLayout.rows(
			[
				StaticText.new.string_("fullLabel"),
				TextField.new.string_(param.fullLabel),
			],
			[
				StaticText.new.string_("target"),
				TextField.new.string_(param.target.asCompileString),
			],
			[
				StaticText.new.string_("property"),
				TextField.new.string_(param.property.asCompileString),
			],
			[
				StaticText.new.string_("spec"),
				TextField.new.string_(param.spec.asCompileString),
			],
			[
				StaticText.new.string_("wrapper"),
				TextField.new.string_(param.wrapper.asCompileString),
			],
			[
				StaticText.new.string_("accessor"),
				TextField.new.string_(try { param.accessor.key } { "no accessor" }),
			],
			[
				StaticText.new.string_("get"),
				TextField.new.string_(param.get.asCompileString),
			],
			[
				StaticText.new.string_("getBus"),
				TextField.new.string_(param.getBus.asCompileString),
			],
			[
				StaticText.new.string_("getRaw"),
				TextField.new.string_(param.getRaw.asCompileString),
			],
			[
				StaticText.new.string_("getVal"),
				TextField.new.string_(try {param.getVal.asCompileString} { "no getVal" }),
			],
			[
				StaticText.new.string_("hasCombinator"),
				TextField.new.string_(param.hasCombinator),
			],
			[
				StaticText.new.string_("inBusMode"),
				TextField.new.string_(param.inBusMode.asCompileString),
			],
			[
				StaticText.new.string_("isSet"),
				TextField.new.string_(param.isSet),
			],
			[
				StaticText.new.string_("source"),
				if(param.target.isKindOf(Pbindef)) {
					TextField.new.string_(param.target.source.at(param.propertyRoot).source.asCompileString)
				} {
					HLayout (
						StaticText.new.string_("no pbindef"),
						TextField.new.string_(param.target.source.asCompileString)
					)
				} 
			],
			[
				nil
			],
		)
	};
	if(param.isNil) {
		Button.new.string_("param is nil")
	} {

		lay = if(param.parent.notNil) {
			HLayout (
				makeDebugView.(param),
				makeDebugView.(param.parent),
			)
		} {
			makeDebugView.(param)
		};
		VLayout (
			HLayout (
				BasicButton.new.string_("copy param in ~param").action_({
					~param = param;
				}),
				BasicButton.new.string_("combinator debug").action_({
					WindowDef(\ParamCombinatorDebugView).front(param.getCombinator)
				}).enabled_(param.hasCombinator),
				BasicButton.new.string_("refresh").action_({
					def.front(param);
				}),
			),
			lay,
		)
	};
});

WindowDef(\ParamCombinatorDebugView, { arg def, combi;
	var grid = GridLayout.rows(
		[
			StaticText.new.string_("param (targetParam)"),
			TextField.new.string_(combi.targetParam.asCompileString),
		],
		[
			StaticText.new.string_("baseParam"),
			TextField.new.string_(combi.baseParam.asCompileString),
		],
		[
			StaticText.new.string_("resultParam"),
			TextField.new.string_(combi.resultParam.asCompileString),
		],
		[
			StaticText.new.string_("targetParam get"),
			TextField.new.string_(combi.targetParam.get.asCompileString),
		],
		[
			StaticText.new.string_("targetParam getRaw"),
			TextField.new.string_(combi.targetParam.getRaw.asCompileString),
		],
		[
			StaticText.new.string_("baseParam get"),
			TextField.new.string_(combi.baseParam.get.asCompileString),
		],
		[
			StaticText.new.string_("baseParam getRaw"),
			TextField.new.string_(combi.baseParam.getRaw.asCompileString),
		],
		[
			StaticText.new.string_("asMap"),
			TextField.new.string_(combi.asMap.asCompileString),
		],
		[
			StaticText.new.string_("size"),
			TextField.new.string_(combi.size),
		],
		[
			StaticText.new.string_("inBusMode"),
			TextField.new.string_(combi.inBusMode),
		],
		[
			StaticText.new.string_("existingInputObjects size"),
			TextField.new.string_(combi.existingInputObjects.size),
		],
		[
			StaticText.new.string_("ranges"),
			VLayout(*combi.rangeParam.collect { arg p, idx;
				p.asView
			});
		],
		[
			StaticText.new.string_("inputs"),
			VLayout(*combi.inputParam.collect { arg p, idx;
				TextField.new.string_(p.getBus.asCompileString)
			});
		],
		[
			StaticText.new.string_("inputs"),
			VLayout(*combi.inputObjects.collect { arg obj, idx;
				if(obj.isNil) {
					TextField.new.string_(obj.asCompileString)
				} {
					HLayout (
						TextField.new.string_(try { obj.key } { "no key" }),
						TextField.new.string_(obj.presetCompileString),
						TextField.new.string_(obj.asCompileString),
					)
				};
			});
		],
		[
			nil
		],
	);
	VLayout (
		BasicButton.new.string_("Refres").action_({ def.front(combi) }),
		grid
	)
	
});
);


