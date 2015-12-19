
"Init base.scd".postln;

~view_selector = (
	// this selector version added possibility to regenere the views if they have been removed
	selected_color: ParamView.color_dark,
	deselected_color: ParamView.color_ligth,
	new: { arg self, selector_count=8, action, viewInit;
		self = self.deepCopy;
	
		"aA.".debug;
		self.selector_count = selector_count;
		self.selected_index = 0;
		self.action = {action};
		self.viewInit = {viewInit};
		
		self.select_item(0);
	
		"xr.".debug;
		self.views.debug("wtf");
		self.debug("self!!!");
		self.changed(\selected_index);
		self;
	},

	views: { arg self;
		if(self.pr_views.isNil or: { self.pr_views.size > 0 and: {self.pr_views.first.isClosed }}) {
			[self.selector_count, self.selected_index].debug("refreshing selector.pr_views");
			self.pr_views = self.selector_count.collect({ arg x;
				XSimpleButton.new
					.minWidth_(15) 
					.action_({
						self.select_item(x);
						self.action.(x); // FIXME: should perhaps pass self as first argument
						self.selector_count.debug("end selector action");
					})
					.background_(self.deselected_color).debug("WWWTTTFF");
			}).debug("selector.views: created views");
			self.pr_views.do(self.viewInit);
			self.select_item(self.selected_index, self.pr_views);
			self.pr_views;
		} {
			self.pr_views
		}
	},

	resize: { arg self, size;
		size.debug("selector.resize");
		self.selector_count = size;
		self.selected_index = self.selected_index.clip(0, size-1);
		self.changed(\selected_index);
		self.pr_views = nil; // don't know what bug can be caused by having a no more referenced views
			// but must find a way to update views because there is more or less now
	},

	select_item: { arg self, x, views;
		views = views ?? {self.views};
		[self.selected_index, x, views].debug("selector.select_item: old selected_index, new one, views");
		x = x.clip(0, views.size-1);
		if(views.size > 0) {
			if(views[self.selected_index].notNil) {
				views[self.selected_index].background_(self.deselected_color);
			};
			if(views[x].notNil) {
				views[x].background_(self.selected_color);
			};
			self.selected_index = x;
			self.changed(\selected_index);
		}
	},

	refresh_view: { arg self, idx;
		self.viewInit.(self.views[idx], idx);
	},

	selected_view: { arg self;
		self.views[self.selected_index]
	},

	is_selected: { arg self, x;
		self.selected_index == x
	},

);

~selector = ~view_selector; // backward compat, i needed a less common name

~pdemul = { arg pat;
	Prout({ arg proutinval;
		var str;
		str = pat.asStream;

		"what".debug;
		pat.debug;
		inf.do{ 
			var inval = str.next(proutinval);
			inval.debug("inval");
			inval.drop(-1).do { arg in;
				var in1 = in.copy;
				"hhh".debug;
				in1[\delta] = 0;
				in1.yield;
			};
			inval.last.debug("what").yield(Event.default);
		}
	}).trace
};
