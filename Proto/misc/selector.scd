
ProtoTemplateDef(\ViewSelector, (
	// this selector version added possibility to regenere the views if they have been removed
	// not usable if not using internal buttons or custom makeViews
	//		because select_item will want to regenerate the views and self.views will call select_item
	selected_color: ParamViewToolBox.color_ligth,
	deselected_color: Color.white,
	new: { arg self, selector_count=8, action, viewInit;
		self = self.deepCopy;
	
		//"aA.".debug;
		self.selector_count = selector_count;
		self.selected_index = 0;
		self.action = {action};
		self.viewInit = {viewInit};
		
		self.select_item(0);
	
		//"xr.".debug;
		//self.views.debug("wtf");
		//self.debug("self!!!");
		self.changed(\selected_index);
		self;
	},

	add_view: { arg self, view;
		self.pr_views = self.pr_views.add(view);
	},

	views: { arg self;
		if(self.pr_views.isNil or: { self.pr_views.size > 0 and: {self.pr_views.first.isClosed }}) {
			//[self.selector_count, self.selected_index].debug("refreshing selector.pr_views");
			self.pr_views = self.makeViews;
			self.pr_views.do(self.viewInit);
			self.select_item(self.selected_index, self.pr_views);
			self.pr_views;
		} {
			self.pr_views
		}
	},

	views_: { arg self, views;
		self.pr_views = views;
	},

	makeViews: { arg self;
		self.selector_count.collect({ arg x;
			BasicButton.new
				.minWidth_(15) 
				.action_({
					self.select_item(x);
					self.action.(x); // FIXME: should perhaps pass self as first argument (or view ?)
					//self.selector_count.debug("end selector action");
				})
				.background_(self.deselected_color);
		});
	},

	resize: { arg self, size;
		//size.debug("selector.resize");
		self.selector_count = size;
		self.selected_index = self.selected_index.clip(0, size-1);
		self.changed(\selected_index);
		self.pr_views = nil; // don't know what bug can be caused by having a no more referenced views
			// but must find a way to update views because there is more or less now
	},

	select_item: { arg self, x, views;
		//"select_item".debug;
		views = views ?? {self.views};
		//[self.selected_index, x, views].debug("selector.select_item: old selected_index, new one, views");
		x = x.clip(0, views.size-1);
		if(views.size > 0) {
			self.old_selected_index = self.selected_index;
			self.selected_index = x;
			self.changed(\selected_index);
			if(self.old_selected_index.notNil and: { self.old_selected_index != self.selected_index } and: { views[self.old_selected_index].notNil }) {
				if(self[\deselect_action].notNil) {
					self.deselect_action(views[self.old_selected_index], self.old_selected_index);
				} {
					//[ views[self.selected_index], views[self.selected_index].background ].debug("kkk");
					views[self.old_selected_index].background_(self.deselected_color);
					//[ views[self.selected_index], views[self.selected_index].background ].debug("kkk2");
				}
			};
			if(views[x].notNil) {
				if(self[\select_action].notNil) {
					self.select_action(views[x], x);
				} {
					views[x].background_(self.selected_color);
				}
			};
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

));

ProtoTemplateDef(\SelectorGroup, (
	new: { arg self;
		self = self.deepCopy;
	
		self.selectedIndex = 0;
		self.viewList = List.new;
	
		self;
	},

	selectChild: { arg self, idx;
		if(self.selectedIndex.notNil) {
			self.viewList[self.selectedIndex].selected = false;
		};
		self.selectedIndex = idx;
		self.viewList[idx].selected = true;
		self.selectAction(self.viewList[idx], idx);
	},

	addChild: { arg self, view;
		var idx;
		self.viewList.add(view);
		idx = self.viewList.size - 1;
		idx;
	},

	addChildAndAction: { arg self, view;
		var idx = self.addChild(view);
		view.selectAction = {
			self.selectChild(idx)
		};
		view;
	}
));


ProtoTemplateDef(\SimpleViewSelector, (
	
	new: { arg self;
		self = ProtoClass((parent:self));
	
		
	
		self;
	},

	addView: { arg self, view;
		self.views = self.views.add(view)
	},

	selectIndex: { arg self, idx;
		//idx.debug("selectIndex");
		self.oldSelectedIndex = self.selectedIndex;
		self.selectedIndex = idx;
		self.changed(\selectedIndex, idx);
		self.selectAction(idx);
		if(idx != self.oldSelectedIndex and: { self.oldSelectedIndex.notNil }) {
			self.changed(\selectedIndex, self.oldSelectedIndex);
			self.deselectAction(self.oldSelectedIndex);
		};
		
	},
));


if(currentEnvironment.isKindOf(ProxySpace).not) {

	~view_selector = ProtoTemplateDef(\ViewSelector); // new backwarp compat
	~selector = ~view_selector; // backward compat, i needed a less common name

	~selectorGroup_set = { arg view, group;
		// deprecated, use addChildAndAction
		var idx;
		idx = group.addChild(view);
		view.selectAction = {
			group.selectChild(idx)
		};
		view;
	};
	~selectorGroup = ProtoTemplateDef(\SelectorGroup);
}
