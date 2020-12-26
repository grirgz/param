
if( ~iProxyChain_all.isNil) { 
	~iProxyChain_all = IdentityDictionary.new; 
};
~iProxyChain = (
	all: { ~iProxyChain_all },
	new: { arg self, name, slots, inKey=\inBus;

		if(self.all[name].notNil) {
			self = self.all[name];
		} {
			self = self.deepCopy;
			self.init(slots, inKey).prAdd(name);
		};
	
		self;
	},

	label: { arg self;
		self.name;	
	},

	prAdd: { arg self, name;
		self.name = name;
		self.all[name] = self;
	},

	init: { arg self, slots, inKey;
		self.slots = slots;
		self.inKey = inKey;
		self.isEnabled = 1 ! self.slots.size;
		self.monitor = NodeProxy.new.source_({ arg inBus; InFeedback.ar(inBus, 2); });

		self.resetOrder;
		//self.updateRouting;
		//self.busses = { Bus.audio(s,2) } ! self.slots.size;

		self;
		
	},

	proxyGroup: { arg self;
		GroupDef(self.name);
	},

	updateRouting: { arg self;
		var in = self.inBus;
		var out;
		if(in.notNil) {
			self.proxyOrder.do { arg slot_idx, i;
				var slot;
				//slot_idx.debug("slot_idx");
				slot = self.slots[slot_idx];
				if(slot.notNil and: { self.isEnabled[slot_idx] != 0 }) {
					//[slot_idx, in, i].debug("in!!!");
					slot.xset(self.inKey, in.asControlInput);
					//slot.parentGroup = self.proxyGroup;
					//self.proxyGroup.moveNodeToTail(slot.group);
					//slot.play;
					slot.send;
					in = slot.bus;
				}
			};
			self.lastIn = in;
			//self.proxyOrder.asArray.reverse.do { arg slot_idx;
			//	var slot;
			//	slot_idx.debug("slot_idx2");
			//	slot = self.slots[slot_idx];
			//	if(slot.notNil and: { self.isEnabled[slot_idx] != 0 }) {
			//		slot.play(self.inBus.asControlInput, 2);
			//		slot.set(self.inKey, self.inBus.asControlInput);
			//	}
			//};

			//[self.monitor, in, self.monitor.get(\inBus)].debug("monitor!!!");
			self.monitor.xset(\inBus, in);
			//[self.monitor, in, self.monitor.get(\inBus)].debug("monitor!!! after");
			//self.proxyGroup.moveNodeToHead(self.monitor.group);
			//self.monitor.group.moveNodeToTail(Group(s));
			//self.slots[self.proxyOrder.asArray.last].play(0)
		};
	},

	eventPlay: { arg self;
		self.updateRouting;
		self.monitor.play;
	},

	eventStop: { arg self;
		self.monitor.xset(\inBus, self.inBus);
		self.monitor.play;
	},

	eventIsPlaying: { arg self;
		self.monitor.monitor.isPlaying and: { 
			self.monitor.get(\inBus) != self.inBus
		};
	},

	enableLink: { arg self, x, onezero=1;
		self.isEnabled[x] = onezero;
		self.updateRouting;
	},

	enabledLinkParam: { arg self, x;
		Param({ self.isEnabled[x] }, { arg val; self.enableLink(x, val) }, ControlSpec(0,1,\lin,1));
	},

	resetOrder: { arg self;
		self.proxyOrder = (0..self.slots.size-1);
	},

	proxyOrder_: { arg self, seq;
		self[\proxyOrder] = OrderedIdentitySet.newFrom(seq.asArray.keep(self.slots.size));
		self.updateRouting;
	},

	xadd: { arg self, proxy;
		self.slots.add(proxy);
		self.isEnabled.add(1);
		self.proxyOder.add(self.slots.size-1);
		self.updateRouting;
	},

	xput: { arg self, i, proxy;
		self.slots.put(i, proxy);
		self.isEnabled.put(i, 1);
		self.updateRouting;
		//self.order.pu(self.slots.size-1);
	},

	xinsert: { arg self, i, proxy;
		self.slots.insert(i, proxy);
		self.isEnabled.insert(i, 1);
		self.updateRouting;
		//self.order.pu(self.slots.size-1);
	},

	removeAt: { arg self, idx;
		self.slots.removeAt(idx);
		self.isEnabled.removeAt(idx);
		self.proxyOrder.remove(idx);
		self.updateRouting;
	}

);


