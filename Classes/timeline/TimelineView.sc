
// based on Thor Magnusson code - www.ixi-audio.net
// GNU licence - google it.


TimelineView : SCViewHolder {

	var <>mygrid; // debug;

	var <userView;
	var <selectionView;

	var >clock;
	var <viewport; // zero is topLeft, all is normalised between 0 and 1
	var <areasize;
	var <>createNodeHook;
	var <>deleteNodeHook;
	var <>paraNodes, connections; 
	var <chosennode; 
	var <>isClickOnSelection; 
	var <>selectionRefloc; 
	var <>quant;
	var <>enableQuant = true;
	var win;
	var >virtualBounds;
	var downAction, upAction, trackAction, keyDownAction, rightDownAction, overAction, connAction;
	var <>mouseDownAction;
	var <>mouseUpAction;
	var <>mouseMoveAction;
	var <>deleteSelectedNodesHook;
	var backgrDrawFunc;
	var background, fillcolor;
	var nodeCount, shape;
	var startSelPoint, endSelPoint, refPoint, refWidth;
	var rawStartSelPoint, rawEndSelPoint;
	var nilSelectionPoint;

	var refPoint; // grid_clicked_point
	var chosennode_old_origin; // in grid units

	var <>lastPixelPos;
	var <>lastGridPos;
	var <selNodes, outlinecolor, selectFillColor, selectStrokeColor;
	var keytracker, conFlag; // experimental
	var nodeSize, swapNode;
	var font, fontColor;

	var <>nodeAlign=\debug;
	
	//var refresh 			= true;	// deprecated
	var <>refreshDeferred	= false;
	var lazyRefreshFunc;
	var <>refreshEnabled = true;

	var action;
	var makeUpdater;
	var <model;
	var controller;
	var timeline_controller;
	var node_selection_controller;

	var endEvent;
	var mouseButtonNumber;
	var mouseClickCount;

	var createNodeDeferedAction;

	var >eventFactory;

	var <>selectionCursor;
	var <>selectionCursorController;
	var <>previousNormSelRect;

	var <>changeCurveMode = false;

	//// keys

	var <>valueKey = \midinote;

	//// options

	var <>forbidHorizontalNodeMove = false;
	var <>stayingSelection = true;
	var <>quantizedSelection = true;
	var <>enablePreview = true;

	*new { arg posyKey; 
		^super.new.initParaSpace(posyKey);
	}

	*newFromEventList { arg eventlist, posyKey;
		^super.new.initParaSpace(posyKey).mapEventList(eventlist);
	}

	initParaSpace { arg xposyKey;
		var pen;
		//bounds = argbounds ? Rect(20, 20, 400, 200);
		//bounds = Rect(bounds.left + 0.5, bounds.top + 0.5, bounds.width, bounds.height);

		this.refreshEnabled = false; // should disable refresh because not initialzed yet
		this.valueKey = xposyKey ? \midinote;
		this.refreshEnabled = true;

		//if((win= w).isNil, {
		//	win = GUI.window.new("ParaSpace",
		//		Rect(10, 250, bounds.left + bounds.width + 40, bounds.top + bounds.height+30));
		//	win.front;
		//});
		Log(\Param).debug("initParaSpace:%", paraNodes);

		this.makeUpdater;
		action = {
			//model.debug("timeline action before");
			model.reorder;
		};

		selNodes = IdentitySet.new;
		quant = Point(1/8,1);
		viewport = viewport ?? Rect(0,0,1,1);
		areasize = areasize ?? Point(2,128);
		userView = UserView.new;
		selectionView = UserView.new;
		this.view = userView;
		this.view.addUniqueMethod(\timeline, { this });
 		//bounds = mouseTracker.bounds; // thanks ron!
 		selectionView.bounds = userView.bounds; // thanks ron!
 		
		background = Color.white;
		this.view.background = Color.white(0.9);
		//fillcolor = Color.new255(103, 148, 103);
		//fillcolor = Color.green;
		outlinecolor = Color.red;
		selectFillColor = Color.green(alpha:0.2);
		selectStrokeColor = Color.black;
		paraNodes = List.new; // list of ParaNode objects
		connections = List.new; // list of arrays with connections eg. [2,3]
		nodeCount = 0;
		//nilSelectionPoint = Point(-1,-1);
		nilSelectionPoint = nil;
		this.startSelPoint = nilSelectionPoint;
		this.endSelPoint = nilSelectionPoint;
		refPoint = 0@0; // in grid units
		refWidth = 0;
		shape = "rect";
		conFlag = false;
		nodeSize = 8;
		font = Font("Arial", 9);
		fontColor = Color.black;
		pen	= GUI.pen;
		previousNormSelRect = Rect(0,0,1,1);

		selectionView.drawFunc_({ this.drawSelection });

		userView
			//.canFocus_(true)
			//.focusColor_(Color.red.alpha_(0.5))

			.mouseUpAction_({|me, x, y, mod|
				this.mouseUpActionBase(me, x, y, mod)
			})

			.mouseDownAction_({|me, px, py, mod, buttonNumber, clickCount|
				this.mouseDownActionBase(me, px, py, mod, buttonNumber, clickCount)
			})

			.mouseMoveAction_({|me, px, py, mod|
				this.mouseMoveActionBase(me, px, py, mod)
			})

			.mouseWheelAction_({ arg view, x, y, modifiers, xDelta, yDelta;
				this.mouseWheelActionBase(view, x, y, modifiers, xDelta, yDelta)
			})

			.mouseOverAction_({arg me, x, y;
				this.mouseOverActionBase(me, x, y)
			})

			.keyDownAction_({ |me, key, modifiers, unicode, keycode |
				this.keyDownActionBase(me, key, modifiers, unicode, keycode);
			})

			.keyUpAction_({ |me, key, modifiers, unicode |
				this.keyUpActionBase(me, key, modifiers, unicode )
			})

			.drawFunc_({		
				this.drawFunc;
			})
		;

		selectionView
			//.canFocus_(true)
			//.focusColor_(Color.red.alpha_(0.5))

			.mouseUpAction_({|me, x, y, mod|
				this.mouseUpActionBase(me, x, y, mod)
			})

			.mouseDownAction_({|me, px, py, mod, buttonNumber, clickCount|
				this.mouseDownActionBase(me, px, py, mod, buttonNumber, clickCount)
			})

			.mouseMoveAction_({|me, px, py, mod|
				this.mouseMoveActionBase(me, px, py, mod)
			})

			.mouseWheelAction_({ arg view, x, y, modifiers, xDelta, yDelta;
				this.mouseWheelActionBase(view, x, y, modifiers, xDelta, yDelta)
			})

			.mouseOverAction_({arg me, x, y;
				this.mouseOverActionBase(me, x, y)
			})

			.keyDownAction_({ |me, key, modifiers, unicode, keycode |
				this.keyDownActionBase(me, key, modifiers, unicode, keycode);
			})

			.keyUpAction_({ |me, key, modifiers, unicode |
				this.keyUpActionBase(me, key, modifiers, unicode )
			})

			.drawFunc_({		
				this.drawSelection;
			})
		;

		this.specialInit;
	}

	specialInit {
		// to be overriden
	}

	clock {
		^(clock ? TempoClock.default)
	}

	/////////////////

	mapEventList { arg eventlist;
		//paraNodes.debug("mapEventList");
		model = eventlist;
		this.refreshEventList;
		//paraNodes.debug("mapEventList2");

		this.makeUpdater;
		
		//[areasize, viewport, paraNodes].debug("mapEventList3");
	}
		
	refreshEventList {
		//model.debug("refreshEventList");
		this.clearSpace;
		model.do { arg event;
			this.addEvent(event)
		};
	}

	//////////////////////////

	startSelPoint_ { arg npoint; // in norm units
		if(this.selectionCursor.notNil) {
			if(npoint.isNil) {
				this.selectionCursor.startPoint = nil;
			} {
				this.selectionCursor.startPoint = this.normPointToGridPoint(npoint);
			};
		};
		startSelPoint = npoint;
	}

	endSelPoint_ { arg npoint; // in norm units
		if(this.selectionCursor.notNil) {
			if(npoint.isNil) {
				this.selectionCursor.endPoint = nil;
			} {
				this.selectionCursor.endPoint = this.normPointToGridPoint(npoint);
			};
		};
		endSelPoint = npoint;
	}

	startSelPoint { arg npoint; // in norm units
		if(this.selectionCursor.notNil) {
			if(this.selectionCursor.startPoint.notNil) {
				^this.gridPointToNormPoint(this.selectionCursor.startPoint);
			} {
				^nil
			}
		} {
			^startSelPoint;
		}
	}

	endSelPoint { arg npoint; // in norm units
		if(this.selectionCursor.notNil) {
			if(this.selectionCursor.startPoint.notNil) {
				^this.gridPointToNormPoint(this.selectionCursor.endPoint);
			} {
				^nil
			}
		} {
			^endSelPoint;
		}
	}
	///////////////////////////////////////////////////////// input events handling

	mouseDownActionBase {|me, px, py, mod, buttonNumber, clickCount|
		// select clicked node, or unselect all node is none is clicked, add connection if c is pushed

		var a, b;
		var bounds = this.bounds;
		var npos;
		var gpos;
		var nquant = this.gridPointToNormPoint(quant.value);

		mouseButtonNumber = buttonNumber;
		mouseClickCount = clickCount;

		npos = this.pixelPointToNormPoint(Point(px,py));
		gpos = this.pixelPointToGridPoint(Point(px,py));
		lastPixelPos = Point(px,py);
		lastGridPos = gpos.trunc(quant.value);
		this.changed(\lastGridPos);
		Log(\Param).debug("mouseDownAction_ px %,py %, npos %, gpos %", px, py, npos, gpos);

		chosennode = this.findNodeHandle(gpos.x, gpos.y);
		isClickOnSelection = this.startSelPoint != nilSelectionPoint and: {
			this.endSelPoint != nilSelectionPoint and: {
				Rect.fromPoints(this.startSelPoint, this.endSelPoint).contains(npos)
			}
		};
		//[chosennode, chosennode !? {chosennode.model}].debug("mouseDownAction: chosennode");
		//[px, py, npos, gpos].debug("amouseDownAction_ px,py, npos, gpos");
		mouseDownAction.(me, px, py, mod, buttonNumber, clickCount, chosennode);

		case
		{ mod.isCtrl and: { buttonNumber == 1 } } {
			this.setEndPosition(gpos.x.trunc(quant.value.x));
		}
		{ mod.isCtrl and: { buttonNumber == 0 } } {
			// create node mode

			//var nodesize = Point(1,1);
			var newpos;
			var newevent;

			//debug("---------mouseDownAction: create node mode");

			//nodesize = this.gridPointToNormPoint(nodesize);

			if(enableQuant) {
				newpos = gpos.trunc(quant.value); 
			} {
				newpos = gpos; 
			};

			//"mouseDownAction: 1".debug;
			newevent = this.eventFactory(newpos);
			Log(\Param).debug("new event!!!!!!!!! %", newevent);
			chosennode = this.addEvent(newevent);
			//newevent.debug("mouseDownAction: 2");
			Log(\Param).debug("new event after add!!!!!!!!! %", newevent);

			createNodeDeferedAction = {
				//debug("mouseDownAction: 3");
				model.addEvent(newevent);
				Log(\Param).debug("new event after add to model!!!!!!!!! %", newevent);
				//debug("mouseDownAction: 4");
				model.reorder; // reorder lose node selection
				//debug("mouseDownAction: 5");
				//model.changed(\refresh); // commented because refresh is already called in mouseUp
				//debug("mouseDownAction: 6");
				chosennode = nil; // chosennode was set to know which node to resize with mouseMove, but is not really selected
			};

			//[chosennode.posyKey, chosennode.model, chosennode.origin].debug("posy, model, origin");
			//[chosennode.width].debug("pppp");
			refPoint = newpos; // var used here for reference in trackfunc
			refWidth = chosennode.width;

			//chosennode.debug("mouseDownAction: chosennode!");
		}
		{ mod.isShift and: { buttonNumber == 0 } } {
			if(chosennode !=nil) { // a node is selected
				//debug("---------mouseDownAction: prepare for resizing mode");
				refPoint = gpos; // var used here for reference in trackfunc
				refWidth = chosennode.width;
			};
			if(this.changeCurveMode) {
				chosennode = this.findPreviousNode(gpos.x);
				refWidth = chosennode.curve;
				refPoint = gpos;
			};
		}
		{
			if(chosennode !=nil, { // a node is selected
				refPoint = gpos; // var used here for reference in trackfunc
				chosennode_old_origin = chosennode.origin; // used for reference when moving chosennode

				if(conFlag == true, { // if selected and "c" then connection is possible
					paraNodes.do({arg node, i; 
						if(node === chosennode, {
							a = i;
						});
					});
					selNodes.do({arg selnode, j; 
						paraNodes.do({arg node, i; 
							if(node === selnode, {
								b = i;
								//if(a != b) {
								this.createConnection(a, b);
								//}
							});
						});
					});
				});

				if(selNodes.size < 2) {
					//debug("---------mouseDownAction: deselect all and select clicked node");
					this.deselectAllNodes(chosennode);
				};
				this.selectNode(chosennode);

				downAction.value(chosennode);
			}, { 
				Log(\Param).debug("mouseDownAction: start % end % sel", this.startSelPoint, this.endSelPoint);
				if(isClickOnSelection == true) {
					// clicked on the selection area, ready to move the selection
					selectionRefloc = this.startSelPoint;
					refPoint = gpos; // to know where mouse clicked before moving
				} {
					// no node is selected
					Log(\Param).debug("---------mouseDownAction: deselect all and draw selrecti");
					this.deselectAllNodes;
					if(quantizedSelection) {
						rawStartSelPoint = npos;
						this.startSelPoint = npos.trunc(nquant);
						rawEndSelPoint = npos;
						this.endSelPoint = npos.trunc(nquant);
					} {
						this.startSelPoint = npos;
						this.endSelPoint = npos;
					};
					Log(\Param).debug("sel:% %", this.startSelPoint, this.endSelPoint);
					this.refreshSelectionView;
				};
			});
		};
	}

	mouseUpActionBase {|me, x, y, mod|
		// if a node was clicked (chosennode), reset refloc (for moving nodes), else select all nodes inside the selection rect

		var bounds = this.bounds;
		mouseUpAction.(me, x, y, mod);
		//selNodes.debug("-------------- mouseUpAction: selNodes");
		//chosennode.debug("mouseUpAction: chosennode");
		if(chosennode != nil, { // a node is selected
			createNodeDeferedAction.value; // function defined when a new node is created
			createNodeDeferedAction = nil;
			upAction.value(chosennode);
			selNodes.do({arg node; 
				node.refloc = node.nodeloc;
			});
			//this.refresh;
		},{ // no node is selected
			// find which nodes are selected
			var rect;
			var wasSelected = false;
			var ppos = Point(x,y);
			var gpos = this.pixelPointToGridPoint(ppos);
			//Log(\Param).debug("mouseUpAction st %, en %", this.startSelPoint, this.endSelPoint);
			if(this.startSelPoint.notNil and: {this.endSelPoint.notNil}) {

				if(isClickOnSelection == true and: { refPoint == gpos }) {
					// click on selection but no move: cancel selection
					//Log(\Param).debug("isClickOnSelection %, refPoint %, gpos: %");
					this.startSelPoint = nilSelectionPoint;
					this.endSelPoint = nilSelectionPoint;
					this.refreshSelectionView;
				} {
					rect = this.normRectToGridRect(Rect.fromPoints(this.startSelPoint, this.endSelPoint)).insetAll(0,0,0.1,0.1);
					wasSelected = selNodes.size > 0;
					selNodes = IdentitySet.new;
					this.selectNodes(this.findNodes(rect));
					if(stayingSelection.not) {
						this.startSelPoint = nilSelectionPoint;
						this.endSelPoint = nilSelectionPoint;
						this.refreshSelectionView;
					};
				}
			} {
				// FIXME: refresh is buggy, need to think again algo
				wasSelected = selNodes.size > 0;
				selNodes = IdentitySet.new;
			};
			if(wasSelected or:{ selNodes.size > 0 }) {
				this.refresh;
				this.refreshSelection;
			}
		});
		this.updatePreviousNormSelRect;
	}

	updatePreviousNormSelRect {
		this.previousNormSelRect = Rect.fromPoints(this.startSelPoint ? Point(0,0), this.endSelPoint ? Point(0,0));
	}

	mouseMoveActionBase {|me, px, py, mod|
		// if a node is clicked, move all selected nodes, else draw a selection rectangle

		var bounds = this.bounds;
		var x,y;
		var ppos = Point(px,py);
		var npos = this.pixelPointToNormPoint(ppos);
		var gpos = this.pixelPointToGridPoint(ppos);
		var nquant = this.gridPointToNormPoint(quant.value);
		var newpos;
		var buttonNumber = mouseButtonNumber;
		var clickCount = mouseClickCount;

		//[buttonNumber, clickCount].debug("mouseMoveAction");

		// old move algo
		//newpos = { arg node;
		//	// gpos is the mouse position in grid units
		//	// (gpos - refPoint) is the distance from old point position to mouse position
		//	var res;
		//	res = node.refloc + (gpos - refPoint);
		//	if ( enableQuant ) {
		//		res = res.round(quant.value);
		//	};
		//	if(this.forbidHorizontalNodeMove == true) {
		//		res.x = node.refloc.x;
		//	};
		//	res = this.clipGridPoint(res);
		//	res;
		//};


		mouseMoveAction.(me, px, py, mod);

		case
		{ (mod.isShift or: mod.isCtrl)  and: { buttonNumber == 0 } } {
			// resize mode
			if(chosennode != nil) { // a node is selected
				var newwidth;
				//Log(\Param).debug("---------mouseMoveAction: resize mode");
				newwidth = refWidth + (gpos.x - refPoint.x);
				if( enableQuant ) {
					newwidth = newwidth.round(quant.value.x);
					newwidth = newwidth.max(quant.value.x);
				} {
					newwidth = newwidth.max(0);
				};
				chosennode.width = newwidth;

				//resizeMode = true;
				//"resize mode!!!!".debug;
				trackAction.value(chosennode, chosennode.spritenum, this.normPointToGridPoint(chosennode.nodeloc));
				this.refresh;
			};

			if( this.changeCurveMode == true ) {
				// TODO
				var newcurve;
				var node = chosennode;
				if(node.notNil) {
					newcurve = refWidth + ( refPoint.y - gpos.y * 40 );
					Log(\Param).debug("newcurve %, gpos %, ref %", newcurve, gpos, refPoint);
					node.model.curve = newcurve;
					node.refresh;
					this.refresh;
				} {
					Log(\Param).debug("changeCurveMode: node nil %", gpos);
				}
			}


		} {
			// move node
			if( buttonNumber == 0 ) {

				if(chosennode != nil) { // a node is selected
					var pixel_newpos_point, pixel_clicked_point, pixel_click_offset, grid_diff, chosennode_new_origin;
					var norm_diff;
					//debug("---------mouseMoveAction: move mode");
					//Log(\Param).debug("---------mouseMoveAction: move mode");
					//debug("======= selected nodes will be moved!!!");
					//selNodes.collect({ arg x; [x.origin, x.extent, x.model] }).debug("======= selected nodes will be moved!!!");

					// ----------new algo
					// - pixel_click_offset = determine diff in pixel from clicked point to origin point of chosennode (chosennode_old_origin)
					// - the new location of node is the point where the mouse has moved (newpos) minus the pixel_click_offset
					// - then convert it to grid unit and quantize it
					// - now determine the diff between old node location and new node location (grid_diff) and apply this change to all selected nodes
					// - since the function is called continously, chosennode_old_origin should be fixed to the first position of node (refloc), and not his
					//		position changing continuously

					chosennode_old_origin = chosennode.refloc;
					pixel_clicked_point = this.gridPointToPixelPoint(refPoint);
					pixel_newpos_point = ppos;
					pixel_click_offset = pixel_clicked_point - this.gridPointToPixelPoint(chosennode_old_origin);
					chosennode_new_origin = this.pixelPointToGridPoint(pixel_newpos_point - pixel_click_offset);
					chosennode_new_origin = this.quantizeGridPoint(chosennode_new_origin);

					if(this.forbidHorizontalNodeMove == true) {
						chosennode_new_origin.x = chosennode_old_origin.x;
					};

					chosennode.setLoc(chosennode_new_origin);
					grid_diff = chosennode_new_origin - chosennode_old_origin;

					selNodes.do { arg node;
						node.setLoc(node.refloc + grid_diff)
					};

					norm_diff = this.gridPointToNormPoint(grid_diff);

					this.startSelPoint = this.previousNormSelRect.origin + norm_diff;
					this.endSelPoint = this.previousNormSelRect.rightBottom + norm_diff;

					// ----------debug algo
					//pixel_clicked_point = this.gridPointToPixelPoint(refPoint);
					//pixel_newpos_point = ppos;
					//pixel_click_offset = pixel_clicked_point - this.gridPointToPixelPoint(chosennode_old_origin);
					//chosennode_new_origin = this.pixelPointToGridPoint(pixel_newpos_point - pixel_click_offset);

					//chosennode.setLoc(chosennode_new_origin);

					//-----------


					this.changed(\nodeMoved);
					//debug("======= selected nodes was moved!!!");
					//selNodes.collect({ arg x; [x.origin, x.extent, x.model] }).debug("======= selected nodes was moved!!!");
					//model.print;  // debug
					this.refresh;
				} { // no node is selected
					if(isClickOnSelection == true ) {
						var pixel_newpos_point, pixel_clicked_point, pixel_click_offset, grid_diff, chosennode_new_origin;
						var norm_diff;
						// move whole selection, quantize on selection left edges
						grid_diff = (gpos - refPoint).trunc(this.quant.value);
						norm_diff = this.gridPointToNormPoint(grid_diff);

						selNodes.do { arg node;
							node.setLoc(node.refloc + grid_diff)
						};


						this.startSelPoint = this.previousNormSelRect.origin + norm_diff;
						this.endSelPoint = this.previousNormSelRect.rightBottom + norm_diff;
						//Log(\Param).debug("sel %, prevsel %, normdif % gdiff %", this.startSelPoint, this.previousNormSelRect, norm_diff, grid_diff);
						this.changed(\nodeMoved);
						this.refresh;

					} {

						if( this.startSelPoint != nilSelectionPoint ) {
							if(quantizedSelection) {
								var realLeftTop = { arg rect;
									// the first point of the selection can be after the second point
									// in this case we reverse it
									var x = rect.origin.x;
									var y = rect.origin.y;
									if(rect.height < 0) {
										y = rect.origin.y + rect.height;
									};
									if(rect.width < 0) {
										x = rect.origin.x + rect.width;
									};
									Point(x,y)
								};
								var realRightBottom = { arg rect;
									var x = rect.rightBottom.x;
									var y = rect.rightBottom.y;
									if(rect.height < 0) {
										y = rect.rightBottom.y - rect.height;
									};
									if(rect.width < 0) {
										x = rect.rightBottom.x - rect.width;
									};
									Point(x,y)
								};
								var selrec = Rect.fromPoints(rawStartSelPoint, npos);
								var leftTop = realLeftTop.(selrec);
								var rightBottom = realRightBottom.(selrec);
								var qleftTop = leftTop.trunc(nquant);
								var qrightBottom = rightBottom.trunc(nquant) + nquant;
								//Log(\Param).debug("rstart % rend % selrec % leftTop % % rightBottom % % ", rawStartSelPoint, npos, selrec, leftTop, qleftTop, rightBottom, qrightBottom);
								this.startSelPoint = qleftTop;
								this.endSelPoint = qrightBottom;
								rawEndSelPoint = npos;
							} {
								this.endSelPoint = npos;
							};
							this.refreshSelectionView;
						}
					};
				}
			}
		};
	}

	mouseWheelActionBase { arg view, x, y, modifiers, xDelta, yDelta;
		var newport;
		var oldport;
		var top;
		oldport = this.viewport;
		top = ( oldport.top + ( yDelta/this.virtualBounds.height ) ).clip(0,1-oldport.height);
		newport = Rect(oldport.left, top, oldport.width, oldport.height);
		//[oldport, newport, oldport.height, oldport.top, oldport.bottom].debug("oldport, newport");
		this.viewport = newport;
		this.refresh;
	}

	mouseOverActionBase {arg me, x, y;
		var bounds = this.bounds;
		chosennode = this.findNode(x, y);
		if(chosennode != nil, {  
			overAction.value(chosennode);
		});
	}

	deleteSelectedNodes {
		selNodes.copy.do({arg node; 
			this.deleteNode(node, false)
		});
	}

	keyDownActionBase { |me, key, modifiers, unicode, keycode |
		//[key, modifiers, unicode, keycode].debug("keyDownActionBase: key, modifiers, unicode, keycode");

		// deleting nodes

		if(unicode == 127, {
			if(this.deleteSelectedNodesHook.notNil) {
				this.deleteSelectedNodesHook.value(this) // hack to be able to addHistorySnapshot
			} {
				this.deleteSelectedNodes;
			}
		});

		// quantize

		if(key == $q) {
			// TODO: should be implemented by adding a new property to the event, understood by XEventList
			//			to be able to undo it
			//quant.value.debug("quantize!!");
			selNodes.do { arg node;
				//node.nodeloc.debug("before");
				node.nodeloc = node.nodeloc.round(quant.value);
				//node.nodeloc.debug("after");
			}
		};

		// connecting

		if(unicode == 99, {conFlag = true;}); // c is for connecting

		// refresh

		if(key == $r) {
			this.refreshEventList;
		};

		// hook

		keyDownAction.value(me, key, modifiers, unicode, keycode);
		this.refresh;
	}

	keyUpActionBase { |me, key, modifiers, unicode |
		if(unicode == 99, {conFlag = false;}); // c is for connecting
	}

	///////////////////////////////////////////////////////// custom input events handling

	nodeDownAction_ { arg func;
		downAction = func;
	}
	
	nodeUpAction_ { arg func;
		upAction = func;
	}
	
	nodeTrackAction_ { arg func;
		trackAction = func;
	}
	
	nodeOverAction_ { arg func;
		overAction = func;
		win.acceptsMouseOver = true;
	}
	
	connectAction_ {arg func;
		connAction = func;
	}
	
	setMouseOverState_ {arg state;
		win.acceptsMouseOver = state;
	}
	
	keyDownAction_ {arg func;
		keyDownAction = func;
	}

	action {
		action.();
	}

	action_ { arg fun;
		action = fun;
	}


	//////////////////////////////////

	quantizeGridPoint { arg point;
		var newpoint;
		newpoint = point;
		if ( enableQuant ) {
			newpoint = newpoint.round(quant.value);
		};
		newpoint = this.clipGridPoint(newpoint);
		^newpoint;
	}

	setEndPosition { arg time;
		model.setEndPosition(time);
		model.print;
		this.refreshEventList;
	}

	eventFactory { arg pos;
		var nodesize = Point(1,1);
		// why nodesize is in normalized form ???
		nodesize = this.gridPointToNormPoint(nodesize);
		if(eventFactory.isNil) {
			Log(\Param).debug("TimelineView: eventFactory is nil");
			^(absTime: pos.x, midinote: pos.y, sustain:nodesize.x);
		} {
			Log(\Param).debug("TimelineView: eventFactory is %", eventFactory.asCompileString);
			^eventFactory.(pos, nodesize.x);
		}
	}

	copyAtSelectionEdges {
		var selrect = this.normRectToGridRect(Rect.fromPoints(this.startSelPoint, this.endSelPoint));
		var el = this.model.clone;
		var nodes = el.collect({ arg ev; this.nodeClass.new(this, 0, ev) });
		this.findCrossedNodes(selrect.leftTop, selrect.leftBottom, nodes).do { arg node;
			this.copySplitNode(el, node, selrect.left)
		};
		nodes = el.collect({ arg ev; this.nodeClass.new(this, 0, ev) });
		this.findCrossedNodes(selrect.rightTop, selrect.rightBottom, nodes).do { arg node;
			this.copySplitNode(el, node, selrect.right)
		};
		nodes = el.collect({ arg ev; this.nodeClass.new(this, 0, ev) });
		^this.findContainedNodes(selrect, nodes).collect(_.model);
		//^noel.select { arg ev;
			//var node = this.nodeClass.new(this, 0, ev);
			//Log(\Param).debug("copyAtSelectionEdges: selrect:%, noderect:%, contains:%", selrect, node.rect, selrect.contains(node.rect));
			//selrect.contains(node.rect);
		//}
				//Rect(5,5,10,10).contains(Rect(6,6,9,3))
	}

	splitAtSelectionEdges {
		var selrect = this.normRectToGridRect(Rect.fromPoints(this.startSelPoint, this.endSelPoint));
		this.findCrossedNodes(selrect.leftTop, selrect.leftBottom).do { arg node;
			this.splitNode(node, selrect.left)
		};
		this.findCrossedNodes(selrect.rightTop, selrect.rightBottom).do { arg node;
			this.splitNode(node, selrect.right)
		};
	}

	splitNode { arg node, gridY;
		var newevent = this.model.splitEvent(node.model, gridY - node.model[node.timeKey]);
		node.decorateCopy(newevent);
	}

	copySplitNode { arg el, node, gridY;
		var newevent = el.splitEvent(node.model, gridY - node.model[node.timeKey]);
		node.decorateCopy(newevent);
	}

	///////////////////////////////////////////////////////// Drawing
	
	drawGridY {
		// pitch graduations
		// this draw horizontal lines along the Y axis
		// should move to TimelineDrawer, but need to check file dependency graph

		var y_lines_count;
		var y_lines_factor;
		Pen.alpha = 0.5;
		Pen.color = Color.black;

		y_lines_count = areasize.y;

		// naive way to reduce number of lines
		// see vertical_grid_do that use 2**(( y/128 ).log2.asInteger)
		7.do {
			if(y_lines_count > 128) {
				y_lines_count = y_lines_count / 2;
			};
		};
		y_lines_factor = areasize.y / y_lines_count;

		y_lines_count.do { arg py;
			py = py * y_lines_factor;
			Pen.line(this.gridPointToPixelPoint(Point(0,py)),this.gridPointToPixelPoint(Point(areasize.x, py)));
		};
		Pen.stroke;
		Pen.alpha = 1;
	}

	drawGridX {
		// time graduations
		// this draw vertical lines along the X axis
		var grid;
		//mygrid.debug("===============mygrid");
		grid = mygrid.(this.virtualBounds, this.areasize, this.viewport);
		//grid.debug("grid");

		if(grid.notNil) {
			grid.draw;
		} {
			TimelineRulerView.vertical_grid_do(this, { arg factor, x, oidx, idx;
				Pen.use{
					//~mygridx.(factor, x, oidx, idx);
					Pen.lineDash = FloatArray[1];
					Pen.color = Color.black;
					if( oidx % 16 == 0 ) { 
						Pen.alpha = 0.6;
						Pen.width = 2;
					} { 
						if(oidx % 4 == 0) {
							Pen.alpha = 0.4;
							Pen.width = 1;

						} {
							Pen.alpha = 0.4;
							Pen.width = 1;
							Pen.lineDash = FloatArray[2.0,2.0];
						}
					};

					Pen.line(Point(x,0), Point(x,this.virtualBounds.height));
					Pen.stroke;
				}
			});

		};
		Pen.alpha = 1;
		
		//DrawGrid(
		//	Rect(
		//		0 - (viewport.origin.x * bounds.width / viewport.width),
		//		0 - (viewport.origin.y * bounds.height / viewport.height), 
		//		bounds.width / viewport.width, 
		//		bounds.height / viewport.height
		//	),
		//	
		//	DenseGridLines(ControlSpec(
		//			0,areasize.x,
		//			\lin,
		//			0,
		//			0
		//	)).density_(1),
		//	//MidinoteGridLines(\midinote.asSpec).density_(8).labelDensity_(2)
		//	nil,
		//);
		

	}

	drawFunc {
		//var bounds = this.view.bounds;
		var pen = Pen;
		var bounds = this.virtualBounds;


		pen.width = 1;
		pen.color = background; // background color
		pen.fillRect(bounds); // background fill
		backgrDrawFunc.value; // background draw function

		/////////////// grid

		this.drawGridX;
		this.drawGridY;
		
		/////////////// the lines

		pen.color = Color.black;
		connections.do({arg conn;
			pen.line(this.normPointToPixelPoint(paraNodes[conn[0]].nodeloc)+0.5, this.normPointToPixelPoint(paraNodes[conn[1]].nodeloc)+0.5);
		});
		pen.stroke;

		/////////////// end line

		this.drawEndLine;
		
		/////////////// the nodes or circles

		this.drawNodes;

		/////////////// the optional waveform

		this.drawWaveform;

		/////////////// the optional curve

		this.drawCurve;
		
		/////////////// the selection node

		//this.drawSelection; // on its own layer now


		/////////////// background frame

		pen.color = Color.black;
		pen.strokeRect(bounds); 

	}


	drawSelection {
		var pstartSelPoint, pendSelPoint;
		var pen = Pen;
		if(this.startSelPoint.notNil and: {this.endSelPoint.notNil}) {

			pstartSelPoint = this.normPointToPixelPoint(this.startSelPoint);
			pendSelPoint = this.normPointToPixelPoint(this.endSelPoint);

			pen.color = selectFillColor;
			pen.fillRect(Rect(	pstartSelPoint.x + 0.5, 
				pstartSelPoint.y + 0.5,
				pendSelPoint.x - pstartSelPoint.x,
				pendSelPoint.y - pstartSelPoint.y
			));
			pen.color = selectStrokeColor;
			pen.strokeRect(Rect(	pstartSelPoint.x + 0.5, 
				pstartSelPoint.y + 0.5,
				pendSelPoint.x - pstartSelPoint.x,
				pendSelPoint.y - pstartSelPoint.y
			));
		}

		/////////////// the raw selection node (enable for debug)

		//pstartSelPoint = this.normPointToPixelPoint(rawStartSelPoint);
		//pendSelPoint = this.normPointToPixelPoint(rawEndSelPoint);

		//pen.color = selectFillColor;
		//pen.fillRect(Rect(	pstartSelPoint.x + 0.5, 
							//pstartSelPoint.y + 0.5,
							//pendSelPoint.x - pstartSelPoint.x,
							//pendSelPoint.y - pstartSelPoint.y
							//));
		//pen.color = selectStrokeColor;
		//pen.strokeRect(Rect(	pstartSelPoint.x + 0.5, 
							//pstartSelPoint.y + 0.5,
							//pendSelPoint.x - pstartSelPoint.x,
							//pendSelPoint.y - pstartSelPoint.y
							//));

		/////////////// debug marker

		//Pen.color = Color.red;
		//Pen.addArc(Point(1,1),2, 0, 2pi);
		//Pen.fill;
		//Pen.addArc(Point(20,20),5, 0, 2pi);
		//Pen.fill;
		
	}

	drawWaveform {} // for children classes
	drawCurve {} // for children classes

	drawNodes {

		var defered_nodes = List.new;
		var first = true;
		//debug("<<<<<<<<<<<< start drawing nodes");
		//[this.bounds, this.virtualBounds].debug("bounds, virtualBounds");

		//[this.viewport, this.bounds, this.virtualBounds, this.areasize].debug("drawNodes:bounds");
		paraNodes.do({arg node;
			//[this.class, node, node.spritenum, node.origin, node.extent, node.rect, node.model].debug("drawing node");
			//[node.rect, this.gridRectToNormRect(node.rect), this.gridRectToPixelRect(node.rect)].debug("drawNodes:rect, norm, pixel");

			// is Set.contains quick enough with big selection ?
			if(node.visible) {
				if(node.class == TimelineViewLocatorLineNode or: { selNodes.matchItem(node) }) {
					defered_nodes.add(node)
				} {
					if(first) { // for env timelines
						Pen.moveTo(this.gridPointToPixelPoint(node.origin));
						first = false;
					};
					node.draw;
				}
			}
		});
		defered_nodes.do { arg node;
			node.draw;
		};

		//debug(";;;;;;;;;;;;;;;;;; stop drawing nodes");
		Pen.stroke;		
	}

	drawEndLine {
		if(endEvent.notNil) {
			Pen.line(
				this.gridPointToPixelPoint(Point(endEvent[\absTime], areasize.y)),
				this.gridPointToPixelPoint(Point(endEvent[\absTime], 0))
			);
			Pen.stroke;
		}

	}
	
	setBackgrDrawFunc_ { arg func;
		backgrDrawFunc = func;
		this.efresh;
	}

	/////////////////////////////////////////////////////////

	bounds { 
		^this.view.bounds
	}

	virtualBounds {
		// virtualBounds use screen coordinates
		^(virtualBounds ? Rect(0,0,this.bounds.width, this.bounds.height));
	}

	makeUpdater {
		if(controller.notNil) {controller.remove};
		controller = SimpleController(model).put(\refresh, {
			if(this.view.isNil) {
				//"COOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOSLCLOCLOCLOSED".debug;
				controller.remove;
			} {
				//"TimelineView get a refresh signal!".debug;
				{
					this.refreshEventList;
					this.refresh;
				}.defer
			};
		});
		controller.put(\redraw, {
			{
				this.refresh;
			}.defer
		});
		controller.put(\enablePreview, {
			{
				this.enablePreview = model.enablePreview;
				this.refresh;
			}.defer
		});
	}

	mimicTimeline { arg timeline, orientation;
		// FIXME: why no changed signal ? ("this.viewport =" instead of "viewport =")
		var rect_copy_horizontal = { arg me, rect;
			me.width = rect.width;
			me.origin = Point(rect.origin.x, me.origin.y);
			me;
		};
		var rect_copy_vertical = { arg me, rect;
			me.height = rect.height;
			me.origin = Point(me.origin.x, rect.origin.y);
			me;
		};

		if(timeline_controller.notNil) {timeline_controller.remove};
		timeline_controller = SimpleController(timeline)
			.put(\viewport, {
				if(this.view.isClosed) {
					timeline_controller.remove;
				} {
					//[this].debug("refresh viewport because mimicTimeline!!");
					switch(orientation,
						\horizontal, {
							this.viewport = rect_copy_horizontal.(viewport, timeline.viewport);
						},
						\vertical, {
							this.viewport = rect_copy_vertical.(viewport, timeline.viewport);
						},
						// else
						{
							this.viewport = timeline.viewport;
						}
					);
					this.refresh;
				}
			})
			.put(\areasize, {
				if(this.view.isClosed) {
					timeline_controller.remove;
				} {
					//[this].debug("refresh viewport because mimicTimeline!!");
					switch(orientation,
						\horizontal, {
							this.areasize = Point(timeline.areasize.x, areasize.y);
						},
						\vertical, {
							this.areasize = Point(areasize.x, timeline.areasize.y);
						},
						// else
						{
							this.areasize = timeline.areasize;
						}
					);
					this.refresh;
				}
			})
			.put(\lastGridPos, {
				lastGridPos = timeline.lastGridPos;
				this.view.refresh;
			})
		;
		// init
		timeline.changed(\areasize);
		timeline.changed(\viewport); 

	}

	mapSelectionCursor { arg cur;
		selectionCursor = cur;
		if(selectionCursorController.notNil) {
			selectionCursorController.remove;
		};
		if(cur.notNil) {
			cur.enableSwapIfNegative = false; // should be in model
			selectionCursorController = SimpleController(selectionCursor).put(\refresh, {
				if(this.view.notNil) {
					if(this.view.isClosed) {
						selectionCursorController.remove;
					} {
						this.refreshSelectionView;
					};
				}
			})
		};
	}

	mimicNodeSelection { arg timeline;
		if(node_selection_controller.notNil) {node_selection_controller.remove};
		node_selection_controller = SimpleController(timeline)
			.put(\selectedNodes, {
				if(this.view.isClosed) {
					timeline_controller.remove;
				} {
					var to_select = List.new;
					//[this].debug("refresh selection because mimicNodeSelection!!");
					// FIXME: this is inneficient
					this.nodes.do { arg node;
						timeline.selectedNodes.do { arg selnode;
							if( node.model === selnode.model ) {
								to_select.add(node)
							};
						};
					};
					if( timeline.chosennode.isNil ) {
						chosennode = nil;
					} {
						this.nodes.do { arg node;
							if( node.model === timeline.chosennode.model ) {
								chosennode = node;
							};
						};
					};
					this.deselectAllNodes;
					this.selectNodes(to_select);
					this.refresh;
				}
			})
		;
		// init
		timeline.changed(\selectedNodes);
	}

	areasize_ { arg val;
		areasize = val;
		this.changed(\areasize);
	}

	viewport_ { arg val;
		viewport = val;
		this.changed(\viewport);
	}

	///////////////// coordinates conversion

	clipGridPoint { arg point;
		// FIXME: this prevent negative y values
		var x, y;
		x = point.x.clip(0,this.areasize.x-quant.value.x); // FIXME: not sure if -1 should be scaled to something
		y = point.y.clip(0,this.areasize.y-quant.value.y);
		^Point(x,y);
	}

	normRectToPixelRect_old { arg rect; //////////// OLD
		// 
		var bounds = this.virtualBounds;
		^Rect(
			rect.origin.x * bounds.extent.x / viewport.extent.x - ( viewport.origin.x * bounds.extent.x ) + bounds.origin.x, 
			( 1-rect.origin.y ) * bounds.extent.y / viewport.extent.y - ( viewport.origin.y * bounds.extent.y ) + bounds.origin.y,
			rect.width * bounds.extent.x/ viewport.extent.x,
			(0 - (rect.height * bounds.extent.y) ) / viewport.extent.y,
		);
	}

	pixelRectToNormRect_old { arg rect; //////////// OLD
		var bounds = this.virtualBounds;
		^Rect(
			rect.origin.x + ( viewport.origin.x * bounds.extent.x ) - bounds.origin.x / bounds.extent.x * viewport.extent.x,
			1-(rect.origin.y + ( viewport.origin.y * bounds.extent.y ) - bounds.origin.y / bounds.extent.y * viewport.extent.y),
			rect.width / bounds.extent.x * viewport.extent.x,
			rect.height / bounds.extent.y * viewport.extent.y,
		);
	}
	////////////
	
	// grid are in bottom coordinates
	// norm are in bottom coordinates
	// bounds are in screen coordinates
	// pixels are in screen coordinates

	normRectToPixelRect { arg rect;
		// 
		var bounds = this.virtualBounds;
		var x_norm_to_pixel;
		var y_norm_to_pixel;
	//	x_norm_to_pixel = 
	//		(rect.origin.x - viewport.origin.x)  	// if viewport.left = 0.15, a point at nx=0.15 would appear at the left border (px=0)
	//		* (bounds.width / viewport.width) // smaller the viewport width (zoom), bigger the interval between left border and the point
	//		+ bounds.origin.x;						// shifting for displaying the timeline as a block in a bigger timeline

	//	y_norm_to_pixel = 
	//		(rect.origin.y - viewport.origin.y)  	// if viewport.left = 0.15, a point at nx=0.15 would appear at the left border (px=0)
	//		* (bounds.height / viewport.height) // smaller the viewport width (zoom), bigger the interval between left border and the point
	//		+ bounds.origin.y;						// shifting for displaying the timeline as a block in a bigger timeline
	//		
		//y_norm_to_pixel = bounds.height - y_norm_to_pixel; // flipping Y because we want our canvas origin on the bottom


		rect = rect
			.translate(0-viewport.origin)
			.scale(1/viewport.extent)
			.scale(bounds.extent)
			// now in pixel in bottom coordinates
			.flipScreen(bounds.height)
			// now in pixel in screen coordinates
			.translate(bounds.origin) // bounds is in screen coordinates so need flipping before translating
		;

		^rect;

		//^Rect(
		//	x_norm_to_pixel,
		//	y_norm_to_pixel,
		//	rect.width * bounds.width / viewport.width,
		//	//(0 - (rect.height * bounds.height) ) / viewport.height, // flipping Y (needed or it's because node.rect return negative extent ?)
		//	rect.height * bounds.height / viewport.height,
		//).flipScreen(this.virtualBounds.height);
	}


	pixelRectToNormRect { arg rect;
		var bounds = this.virtualBounds;
		var x_pixel_to_norm, y_pixel_to_norm;

		rect = rect
			// now in pixels in screen coordinates
			.translate(0-bounds.origin)
			.flipScreen(bounds.height)
			// now in pixels in bottom coordinates
			.scale(1/bounds.extent)
			// now in normalized in bottom coordinate
			.scale(viewport.extent)
			.translate(viewport.origin)
		;

		^rect;

		//rect = rect.flipScreen(bounds.height);
		//x_pixel_to_norm = 
		//	(rect.origin.x - bounds.origin.x)
		//	/ (bounds.width / viewport.width)
		//	+ viewport.origin.x;

		//y_pixel_to_norm = rect.origin.y; // flipping Y
		//y_pixel_to_norm = 
		//	(y_pixel_to_norm - bounds.origin.y)
		//	/ (bounds.height / viewport.height)
		//	+ viewport.origin.y;

		////y_pixel_to_norm = 1-y_pixel_to_norm; // flipping Y

		//// proof: reversing normRectToPixelRect function
		////
		//// y_norm_to_pixel = 
		////	(rect.origin.y - viewport.origin.y)  
		////	* (bounds.height / viewport.height) 
		////	+ bounds.origin.y;				
		////
		//// y_norm_to_pixel - bounds.origin.y / (bounds.height / viewport.height) + viewport.origin.y = rect.origin.y
		////

		//^Rect(
		//	x_pixel_to_norm,
		//	y_pixel_to_norm,
		//	rect.width / bounds.extent.x * viewport.extent.x,
		//	rect.height / bounds.extent.y * viewport.extent.y, // FIXME: why no Y flipping here ???
		//); // finally added flipping
	}



	gridRectToNormRect { arg rect;
		//rect.debug("gridRectToNormRect");
		^Rect.fromPoints(
			this.gridPointToNormPoint(rect.origin),
			this.gridPointToNormPoint(rect.rightBottom),
		);
	}

	normRectToGridRect { arg rect;
		^Rect.fromPoints(
			this.normPointToGridPoint(rect.origin),
			this.normPointToGridPoint(rect.rightBottom),
		);
	}

	gridRectToPixelRect { arg rect;
		^this.normRectToPixelRect(this.gridRectToNormRect(rect));
	}

	pixelRectToGridRect { arg rect;
		^this.normRectToGridRect(this.pixelRectToNormRect(rect));
	}

	normPointToPixelPoint { arg point;
		^this.normRectToPixelRect(Rect.fromPoints(point, point+Point(0,0))).origin;
	}

	pixelPointToNormPoint { arg point;
		^this.pixelRectToNormRect(Rect.fromPoints(point, point+Point(0,0))).origin;
	}

	gridPointToNormPoint { arg point;
		^(point / areasize)
	}

	normPointToGridPoint { arg point;
		^(point * areasize)
	}

	pixelPointToGridPoint { arg point;
		^this.normPointToGridPoint(this.pixelPointToNormPoint(point))
	}

	gridPointToPixelPoint { arg point;
		^this.normPointToPixelPoint(this.gridPointToNormPoint(point))
	}


	pixelExtentToGridExtent { arg point;
		// not used by TimelineViewLocatorNode
		^(point / this.bounds.extent * areasize * viewport.extent);
	}

	//// seconds

	secondPointToPixelPoint { arg point;
		^this.gridPointToPixelPoint(point * Point(this.clock.tempo,1));
	}

	pixelPointToSecondPoint { arg point;
		^this.pixelPointToGridPoint(point) / Point(this.clock.tempo,1);
	}

	secondRectToPixelRect { arg rect;
		^Rect.fromPoints(
			this.secondPointToPixelPoint(rect.origin),
			this.secondPointToPixelPoint(rect.rightBottom),
		);
	}

	///////////////// 

	addEvent { arg event;
		var node;
		switch(event[\type],
			\xxx, {

			},
			//\start, {
			//	"start".debug;
			//	^nil;
			//},
			////\locator, {
			////	event.debug("label");
			////	^nil;
			////},
			//\end, {
			//	"end".debug;
			//	endEvent = event;
			//	^nil;
			//},
			// else
			{
				^this.addEventRaw(event);
			}
		)
	}

	addEventRaw { arg event;
		var node;
		node = this.nodeClass.new(this, nodeCount, event);
		nodeCount = nodeCount + 1;
		paraNodes.add(node);
		this.initNode(node);
		createNodeHook.(node, nodeCount);
		if(refreshEnabled) { this.refresh };
		^node;
	}

	selectedNodes {
		// better name
		^selNodes
	}

	selectNode { arg node;
		node.selectNode;
		selNodes.add(node);
		this.changed(\selectedNodes)
	}

	selectNodes { arg nodes;
		// method for sending only one changed signal when multiple nodes are selected
		nodes.do { arg node;
			node.selectNode;
		};
		selNodes.addAll(nodes);
		this.changed(\selectedNodes)
	}

	deselectNode { arg node;
		node.deselectNode;
		selNodes.remove(node);
		this.changed(\selectedNodes)
	}

	deselectNodes { arg nodes;
		nodes.do { arg node;
			node.deselectNode;
		};
		selNodes.removeAll(nodes);
		this.changed(\selectedNodes)
	}

	deselectAllNodes { arg chosennode;
		// deselect all but chosennode
		// should use selNodes, but at least we are sure there are no more selected nodes
		this.deselectNodes(paraNodes.reject({ arg x; x === chosennode }))
	}
	
	clearSpace {
		paraNodes.do { arg node;
			node.free;
		};
		selNodes = IdentitySet.new;
		paraNodes = List.new;
		connections = List.new;
		nodeCount = 0;
		this.refresh;
	}

	nodeClass {
		^TimelineViewNode
	}

	nodes {
		^paraNodes
	}

	initNode { arg node;

	}

	createConnection {arg node1, node2, refresh=true;
		if((nodeCount < node1) || (nodeCount < node2), {
			"Can't connect - there aren't that many nodes".postln;
		}, {
			block {|break|
				connections.do({arg conn; 
					if((conn == [node1, node2]) || (conn == [node2, node1]), {
						break.value;
					});	
				});
				// if not broken out of the block, then add the connection
				connections.add([node1, node2]);
				connAction.value(paraNodes[node1], paraNodes[node2]);
				if(refresh == true, {this.refresh});
			}
		});
	}

	deleteConnection {arg node1, node2, refresh=true;
		connections.do({arg conn, i; if((conn == [node1, node2]) || (conn == [node2, node1]),
			 { connections.removeAt(i)})});
		if(refresh == true, {this.refresh});
	}

	deleteConnections { arg refresh=true; // delete all connections
		connections = List.new; // list of arrays with connections eg. [2,3]
		if(refresh == true, {this.refresh});
	}
	
	deleteNode { arg node, refresh=true;
		var del;
		var nodenr = node.spritenum;
		if(node.deletable.not) { ^this };
		del = 0;
		connections.copy.do({arg conn, i; 
			if(conn.includes(nodenr), { connections.removeAt((i-del)); del=del+1;})
		});
		connections.do({arg conn, i; 
			if(conn[0]>nodenr,{conn[0]=conn[0]-1});if(conn[1]>nodenr,{conn[1]= conn[1]-1});
		});
		deleteNodeHook.(node, nodenr);
		if(paraNodes.size > 0, {paraNodes.remove(node)});
		node.free;
		model.removeEvent(node.model);
		if(refresh == true, {this.refresh});
	}
	
	reconstruct { arg aFunc;
		refreshEnabled = false;
		aFunc.value( this );
		refreshEnabled = true;
		this.refresh;
	}

	refresh {
		if( this.refreshEnabled, { {
			userView.refresh;
			selectionView.refresh;
		}.defer; });
	}

	refreshSelection {
		model.changed(\selection);
	}

	refreshSelectionView {
		if( this.refreshEnabled, { {
			selectionView.refresh;
		}.defer; });
	}

	lazyRefresh {
		if( refreshDeferred.not, {
			AppClock.sched( 0.02, lazyRefreshFunc );
			refreshDeferred = true;
		});
	}

	findNodeHandle { arg x, y;
		// this return only node if clicked on the label part
		// this allow to start dragged selection even when screen is crowded by nodes
		// only useful in ClipTimelineView
		^this.findNode(x,y)
	}
	
	// local function
	findNode {arg x, y;
		// findNode take x and y in grid coordinates, because TimelineNode.rect is in grid coordinates
		var point = Point.new(x,y);
		if(chosennode.notNil and: {chosennode.rect.containsPoint(point)}) {
			// priority to the already selected node
			^chosennode;
		};
		paraNodes.reverse.do({arg node;  // reverse because topmost is last
			//node.spritenum.debug("spritnum");
			//[node.rect, point].debug("findNode");
			if(node.selectable and: {node.rect.containsPoint(point)}, {
				//[node.rect, point].debug("findNode: found!!");
				^node;
			});
		});
		^nil;
	}

	findCrossedNodes { arg startPoint, endPoint, nodes;
		^(nodes ? paraNodes).collect({arg node; 
			var point = node.origin;
			//node.spritenum.debug("spritnum");
			//[rect, point].debug("findNodes");
			if(node.selectable and: {Rect.fromPoints(startPoint, endPoint).intersects(node.rect)}, {
				//[node.rect, point].debug("findNode: found!!");
				node;
			});
		}).select(_.notNil);
	}

	findNodes { arg rect, nodes;
		^(nodes ? paraNodes).collect({arg node; 
			var point = node.origin;
			//node.spritenum.debug("spritnum");
			//[rect, point].debug("findNodes");
			if(node.selectable and: {rect.containsPoint(point)}, {
				//[node.rect, point].debug("findNode: found!!");
				node;
			});
		}).select(_.notNil);
	}

	findPreviousNode { arg gposx;
		^paraNodes.reverse.detect { arg node;
			Log(\Param).debug("findPreviousNode gposx %, origin %, %", gposx, node.origin, node.origin.x >= gposx);
			node.origin.x <= gposx
		};
	}

	findContainedNodes { arg rect, nodes;
		^(nodes ? paraNodes).collect({arg node; 
			var point = node.origin;
			//node.spritenum.debug("spritnum");
			//[rect, point].debug("findNodes");
			if(node.selectable and: {rect.contains(node.rect)}, {
				//[node.rect, point].debug("findNode: found!!");
				node;
			});
		}).select(_.notNil);
	}

	free {
		paraNodes.reverse.do { arg node;
			node.free;
		};
		controller.remove;
	}


	////////////////////////////// To review
	// theses are old methods to review, rename or refactorize
	// all theses setXXX methods are ugly

				
	setNodeSize_ {arg index, size, refresh=true;
		paraNodes[index].extent_(size);
		if(refresh == true, {this.refresh});
	}

	getNodeSize {arg index;
		^paraNodes[index].extent;
	}
	
	setNodeColor_ {arg index, color, refresh=true;
		paraNodes[index].setColor_(color);
		if(refresh == true, {this.refresh});
	}
	
	getNodeColor {arg index;
		^paraNodes[index].getColor;	
	}
	
	setFont_ {arg f;
		font = f;
	}
	
	setFontColor_ {arg fc;
		fontColor = fc;
	}
	
	setNodeString_ {arg index, string;
		paraNodes[index].string = string;
		this.refresh;		
	}
	
	getNodeString {arg index;
		^paraNodes[index].string;
	}
	
	setNodeLoc_ {arg index, argX, argY, refresh=true;
		//var x, y;
		//x = argX+bounds.left;
		//y = argY+bounds.top;
		paraNodes[index].setLoc(Point(argX+0.5, argY+0.5));
		if(refresh == true, {this.refresh});
	}
	
	setNodeLocAction_ {arg index, argX, argY, action, refresh=true;
		//var x, y;
		//x = argX+bounds.left;
		//y = argY+bounds.top;
		paraNodes[index].setLoc(Point(argX, argY));
		switch (action)
			{\down} 	{downAction.value(paraNodes[index])}
			{\up} 	{upAction.value(paraNodes[index])}
			{\track} 	{trackAction.value(paraNodes[index])};
		if(refresh == true, {this.refresh});
	}
	
	getNodeLoc {arg index;
		var x, y;
		x = paraNodes[index].nodeloc.x-0.5;
		y = paraNodes[index].nodeloc.y-0.5;
		^[x, y];
	}

	setNodeLoc1_ {arg index, argX, argY, refresh=true;
		var x, y;
		var bounds = this.bounds;
		x = (argX * bounds.width).round(1);
		y = (argY * bounds.height).round(1);
		paraNodes[index].setLoc(Point(x+0.5, y+0.5));
		if(refresh == true, {this.refresh});
	}

	setNodeLoc1Action_ {arg index, argX, argY, action, refresh=true;
		var x, y;
		var bounds = this.bounds;
		x = (argX * bounds.width).round(1);
		y = (argY * bounds.height).round(1);
		paraNodes[index].setLoc(Point(x+bounds.left, y+bounds.top));
		switch (action)
			{\down} 	{downAction.value(paraNodes[index])}
			{\up} 	{upAction.value(paraNodes[index])}
			{\track} 	{trackAction.value(paraNodes[index])};
		if(refresh == true, {this.refresh});
	}

	getNodeLoc1 {arg index;
		var bounds = this.bounds;
		var x, y;
		x = paraNodes[index].nodeloc.x  / bounds.width;
		y = paraNodes[index].nodeloc.y  / bounds.height;
		^[x, y];
	}
	
	getNodeStates {
		var locs, color, size, string;
		locs = List.new; color = List.new; size = List.new; string = List.new;
		paraNodes.do({arg node; 
			locs.add(node.nodeloc);
			color.add(node.color); 
			size.add(node.size);
			string.add(node.string);
		});
		^[locs, connections, color, size, string];
	}

	setNodeStates_ {arg array; // array with [locs, connections, color, size, string]
		// FIXME: not maintened
		var bounds = this.bounds;
		if(array[0].isNil == false, {
			paraNodes = List.new; 
			array[0].do({arg loc; 
				paraNodes.add(TimelineViewNode.new(loc.x, loc.y, fillcolor, bounds, nodeCount, Point(1,1), nodeAlign)); // TODO: size
				nodeCount = nodeCount + 1;
				})
		});
		if(array[1].isNil == false, { connections = array[1];});
		if(array[2].isNil == false, { paraNodes.do({arg node, i; node.setColor_(array[2][i];)})});
		if(array[3].isNil == false, { paraNodes.do({arg node, i; node.extent_(array[3][i];)})});
		if(array[4].isNil == false, { paraNodes.do({arg node, i; node.string = array[4][i];})});
		this.refresh;
	}

	setBackgrColor_ {arg color, refresh=true;
		background = color;
		if(refresh == true, {this.refresh});
	}
		
	setFillColor_ {arg color, refresh=true;
		fillcolor = color;
		paraNodes.do({arg node; 
			node.setColor_(color);
		});
		if(refresh == true, {this.refresh});
	}
	
	setOutlineColor_ {arg color;
		outlinecolor = color;
		this.refresh;
	}
	
	setSelectFillColor_ {arg color, refresh=true;
		selectFillColor = color;
		if(refresh == true, {this.refresh});
	}

	setSelectStrokeColor_ {arg color, refresh=true;
		selectStrokeColor = color;
		if(refresh == true, {this.refresh});
	}
	
	setShape_ {arg argshape, refresh=true;
		shape = argshape;
		if(refresh == true, {this.refresh});
	}
}

///////////////// variants children

MidinoteTimelineView : TimelineView {
	// this timeline is the same that the basic timeline, with piano background
	drawGridY {
		TimelineDrawer.draw_piano_bar(this, 0, 0.2);
	}
}

KitTimelineView : TimelineView {
	// this timeline is the same that the basic timeline, with grouping rows by 4
	drawGridY {
		TimelineDrawer.draw_quad_lines(this);
	}
	
}

PdefTimelineView : TimelineView { // deprecated name

}

ClipTimelineView : TimelineView {
	findNodeHandle { arg x, y;
		// this return only node if clicked on the label part
		// this allow to start dragged selection even when screen is crowded by nodes
		// findNode take x and y in grid coordinates, because TimelineNode.rect is in grid coordinates
		var point = Point.new(x,y);
		if(chosennode.notNil and: {chosennode.handleRect.containsPoint(point)}) {
			// priority to the already selected node
			^chosennode;
		};
		paraNodes.reverse.do({arg node;  // reverse because topmost is last
			//node.spritenum.debug("spritnum");
			//[node.rect, point].debug("findNode");
			if(node.selectable and: {node.handleRect.containsPoint(point)}, {
				//[node.rect, point].debug("findNode: found!!");
				^node;
			});
		});
		^nil;
	}
	

}



//////////////////////////////////////////////////////////////
//////////////////////////////// Nodes


//// dispatcher

TimelineViewNode {
	*new { arg parent, nodeidx, event;
		var type;
		var node;
		// FIXME: choose a better type system
		type = event[\nodeType] ? event[\eventType] ? event[\type];
		if(type == \pattern and:{  event[\timeline].notNil }) {
			type = \timeline;
		};

		Log(\Param).debug("% %".format("TimelineViewNode: new: parent, nodeType/type",[ parent.class, type.asCompileString ]));
		Log(\Param).debug("TimelineViewNode: nodeType %".format([ event[\nodeType], event, event.parent ]));
		node = switch(type,
			\start, {
				var res = TimelineViewLocatorLineNode(parent, nodeidx, event);
				res.alpha = 1;
				res;
			},
			\end, {
				var res = TimelineViewLocatorLineNode(parent, nodeidx, event);
				res.alpha = 1;
				res;
			},
			\eventenv, {
				TimelineViewEventEnvNode(parent, nodeidx, event)
			},
			\eventlist, {
				TimelineViewEventListNode(parent, nodeidx, event)
			},
			\timeline, {
				//if(event.timeline.isKindOf(EnvTimeline)) {
				switch(event.timeline.eventType,
					\envTimeline, {
						TimelineViewEventEnvNode(parent, nodeidx, event)
					},
					\sampleTimeline, {
						TimelineViewEventSampleNode(parent, nodeidx, event)
					}, {
						TimelineViewEventListNode(parent, nodeidx, event)
					}
				)
			},
			\player, {
				TimelineViewEventListNode(parent, nodeidx, event)
			},
			\pattern, {
				TimelineViewEventListNode(parent, nodeidx, event)
			},
			\eventloop, {
				TimelineViewEventLoopNode(parent, nodeidx, event)
			},
			\locator, {
				TimelineViewLocatorLineNode(parent, nodeidx, event)
			},
			{
				//type.asCompileString.debug("mais pourquoi ?? :(");
				TimelineViewEventNode(parent, nodeidx, event)
			}
		);

		Log(\Param).debug("node created: % %".format(node.class, node));

		^node;
	}
}


//// base

TimelineViewNodeBase {
	// TODO: take default key names from PlayerEvent
	var <spritenum;
	var <model;
	var <>absTime;
	var <>xpos;
	var <>refreshAction;
	var <>timeKey = \absTime;
	var >posyKey = \midinote;
	var <>defaultPosyValue = 0;
	var <>lenKey = \sustain;
	var <>startOffsetKey = \event_dropdur;
	var <>startOffset;
	var <>origin;
	var <>extent;
	var <>defaultHeight = 1;
	var <>color, <>outlineColor;
	var <>parent;
	var <>action;
	var <>refresh;
	var <>controller;
	var <>selectable = true; // to be or not ignored by findNode
	var <>deletable = true;
	var <>visible = true;
	var <>enablePreview = true;
	*new {
		^super.new
	}
}

TimelineViewEventNode : TimelineViewNodeBase {
	var <>colorSelected;
	var <>colorDeselected;

	var <>refloc; // used to store start point when moving node, should be named oldOrigin

	*new { arg parent, nodeidx, event;
		^super.new.init(parent, nodeidx, event).baseInit;
	}

	baseInit {
		this.colorDeselected = Color.black;
		this.colorSelected = Color.red;
	}

	posyKey {
		if(parent.valueKey.notNil) {
			^parent.valueKey
		} {
			^posyKey
		}
	}

	init { arg xparent, nodeidx, event;
		parent = xparent;
		spritenum = nodeidx;
		model = event;

		//[spritenum, model].debug(this.class.debug("CREATE EVENT NODE !"));

		action = {
			//[model, origin, extent].debug("node action before");
			model[timeKey] = origin.x;
			model[this.posyKey] = origin.y;
			model[lenKey] = extent.x;
		};

		refresh = {
			origin = Point(model[timeKey], model[this.posyKey] ? this.defaultPosyValue);
			color = ParamViewToolBox.color_ligth;
			outlineColor = outlineColor ? Color.black;
			extent = Point(model.use { currentEnvironment[lenKey].value(model) } ? 1, this.defaultHeight); // * tempo ?
			//[spritenum, origin, extent, color].debug("node refresh");
		};

		this.makeUpdater;
		this.refresh;
		this.action;
	}

	enablePreview {
		^parent.enablePreview;
	}

	selectable {
		^[\rest, \start, \end].includes(this.model[\type]).not
	}

	deletable {
		^[\rest, \start, \end].includes(this.model[\type]).not
	}

	rect {
		// in grid coordinates
		^Rect(origin.x, origin.y, extent.x, extent.y)
	}

	handleRect {
		^this.rect
	}

	width {
		^extent.x;
	}

	width_ { arg val;
		extent.x = val;
		this.action;
	}

	nodeloc { // deprecated origin
		^origin
	}

	nodeloc_ { arg val;
		this.setLoc(val);
	}

	setLoc { arg val;
		origin = val;
		this.action;
		parent.action;
		//parent.model.changed(\refresh);
		model.changed(\refresh);
		parent.model.changed(\redraw);
	}

	action {
		action.();
	}

	action_ { arg fun;
		action = fun;
	}

	makeUpdater {
		if(controller.notNil) {controller.remove};
		controller = SimpleController(model).put(\refresh, {
			if(parent.view.isNil) {
				//"YOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOo".debug;
				this.free;
			} {
				{
					this.refresh;
				}.defer
			}
		})
	}

	refresh {
		refresh.()
	}

	refresh_ { arg fun;
		refresh = fun;
	}

	draw {
		var myrect;
		var pos;
		Pen.color = this.color;
		pos = this.origin;
		myrect = parent.gridRectToPixelRect(this.rect);
		//[spritenum, rect, this.class].debug("draw");
		Pen.fillRect(myrect);
		Pen.color = this.outlineColor;
		Pen.strokeRect(myrect);
		//Pen.stroke;
	}

	selectNode {
		this.refloc = this.nodeloc;
		outlineColor = this.colorSelected;
	}

	deselectNode {
		outlineColor = this.colorDeselected;
	}

	free {
		if(controller.notNil) {controller.remove};
	}

	decorateCopy {} // to set name when copied
}

//// children

TimelineViewEventListNode : TimelineViewEventNode {
	// is used for representing Ndef, Pdef, TrackDef and timelines as a node in a bigger timeline
	// that is player and pattern event types
	// if timeline, can display content of timeline
	var <>label;
	var <>preview;
	var <>labelheight = 20;
	//*new { arg parent, nodeidx, event;
	//	^super.new.init(parent, nodeidx, event);
	//}

	timelinePreviewClass {
		^TimelinePreview
	}

	initPreview {
		if(this.enablePreview == true) {
			if(model[\eventlist].notNil) {
				preview.mapEventList(model.eventlist);
			};

			if(model.timeline.notNil) {
				preview.mapModel(model)
			};
		}

	}

	refreshPreview {
		preview.refresh;
	}

	init { arg xparent, nodeidx, event;
		parent = xparent;
		spritenum = nodeidx;
		model = event;

		preview = this.timelinePreviewClass.new;
		preview.areasize.x = parent.areasize.x;
		this.initPreview;

		//[spritenum, model].debug(this.class.debug("CREATE EVENT NODE !"));

		action = {
			var debug_action;
			//[model, origin, extent].debug("node action before");
			model[timeKey] = origin.x;
			model[this.posyKey] = origin.y;
			model[lenKey] = extent.x;
			//model[startOffsetKey] = startOffset; // user should change model directly
		};

		refresh = {
			var debug_refresh;
			origin = Point(model[timeKey], model[this.posyKey]);
			color = ParamViewToolBox.color_ligth;
			outlineColor = Color.black;
			extent = Point(model.use { currentEnvironment[lenKey].value(model) } ? 1, 1); // * tempo ?
			label = model.use {  model.label } ? "unnamed";
			startOffset = model[startOffsetKey];
			this.refreshPreview;

			parent.refresh;
			//[spritenum, origin, extent, color].debug("node refresh");
		};

		this.makeUpdater;
		this.refresh;
		this.action;
	}

	drawSpecialRect { arg rect;
		Pen.moveTo(rect.rightTop);
		Pen.moveTo(rect.rightBottom);
		//TODO

	}

	drawRectTriangle { arg rect, anglePos='leftTop';
		var poslist = ['leftTop', 'rightTop', 'rightBottom', 'leftBottom'];
		var startIdx = poslist.indexOf(anglePos);

		Pen.moveTo(rect.perform(poslist.wrapAt(startIdx)));
		Pen.lineTo(rect.perform(poslist.wrapAt(startIdx+1)));
		Pen.lineTo(rect.perform(poslist.wrapAt(startIdx+3)));
		Pen.lineTo(rect.perform(poslist.wrapAt(startIdx+4)));

	}

	drawRectDiagonal { arg rect, anglePos='leftTop';
		var poslist = ['leftTop', 'rightTop', 'rightBottom', 'leftBottom'];
		var startIdx = poslist.indexOf(anglePos);

		Pen.moveTo(rect.perform(poslist.wrapAt(startIdx+1)));
		Pen.lineTo(rect.perform(poslist.wrapAt(startIdx+3)));
	}

	handleRect {
		var prect;
		var hrect;
		prect = parent.gridRectToPixelRect(this.rect);
		prect = prect.insetAll(0,0,1,1); // cleaner drawing
		hrect = prect.insetAll(0,0,0,prect.height-labelheight);
		//Log(\Param).debug("pixel rect:%, grid rect:%, pixel hrect:%, grid hrect:%", prect, this.rect, hrect, parent.pixelRectToGridRect(hrect));
		^parent.pixelRectToGridRect(hrect);
	}

	draw {
		var rect;
		var pos;
		var previewrect;
		var labelrect;
		//var preview_background = Color.new255(101, 166, 62);
		//var label_background = Color.new255(130, 173, 105);
		var preview_background = ParamViewToolBox.color_ligth;
		var label_background = ParamViewToolBox.color_pale;
		var virtualBounds_rect;

		pos = this.origin;

		rect = parent.gridRectToPixelRect(this.rect);
		rect = rect.insetAll(0,0,1,1); // cleaner drawing
		// now rect is in screen coordinates
		previewrect = rect.insetAll(0,labelheight,0,0);
		labelrect = rect.insetAll(0,0,0,rect.height-labelheight); // should be same as handleRect but in pixel
		//Log(\Param).debug("label px rect:%", labelrect);

		//labelrect.debug("labelrect");
		//previewrect.debug("previewrect");
		//rect.debug("rect");

		//[spritenum, rect].debug("draw");

		Pen.color = preview_background;
		Pen.fillRect(rect);
		Pen.color = label_background;
		Pen.fillRect(previewrect);

		//Pen.color = Color.red;
		//Pen.fillRect(labelrect);

		// outline

		Pen.color = this.outlineColor;
		Pen.strokeRect(rect);

		// top left triangle
		if(startOffset.notNil and: { startOffset > 0 }) {
			Pen.color = this.outlineColor;
		} {
			Pen.color = Color.white;
		};

		this.drawRectTriangle(
			Rect(labelrect.origin.x, labelrect.origin.y, labelheight/4, labelheight/4),
			'leftTop'
		);
		Pen.fill;

		this.drawRectDiagonal(
			Rect(labelrect.origin.x, labelrect.origin.y, labelheight/4, labelheight/4),
			'leftTop'
		);
		Pen.color = this.outlineColor;
		Pen.stroke;

		// top right triangle
		Pen.color = Color.white;

		this.drawRectTriangle(
			Rect(labelrect.rightTop.x, labelrect.rightTop.y, labelheight.neg/4, labelheight/4),
			'leftTop'
		);
		Pen.fill;

		this.drawRectDiagonal(
			Rect(labelrect.rightTop.x, labelrect.rightTop.y, labelheight.neg/4, labelheight/4),
			'leftTop'
		);
		Pen.color = this.outlineColor;
		Pen.stroke;

		// label
		Pen.color = Color.black;
		Pen.stringLeftJustIn(" "++label, labelrect);

		// preview

		if(this.enablePreview) {

			// we use wide virtualBounds_rect as a workaround for a size bug to be found
			virtualBounds_rect = Rect(previewrect.leftTop.x, previewrect.leftTop.y, this.parent.virtualBounds.width, previewrect.height);
			//virtualBounds_rect = previewrect;

			preview.virtualBounds = virtualBounds_rect;
			preview.areasize = preview.areasize.x_(this.parent.areasize.x);
			preview.viewport = preview.viewport.width_(this.parent.viewport.width);

			Pen.use {
				Pen.addRect(previewrect);
				Pen.clip;
				Log(\Param).debug("preview");
				preview.drawFunc;
			};
		}
		//Pen.stroke;
	}

	enablePreview {
		^this.parent.enablePreview;
	}

	makeUpdater {
		if(controller.notNil) {controller.remove};
		controller = SimpleController(model).put(\refresh, {
			{
				this.refresh;
			}.defer
		})
		//controller = SimpleController(model[\eventlist]).put(\refresh, {
		//	this.refresh;
		//})
	}

	decorateCopy { arg ev; // to set name when copied
 		ev.label = "%-%".format(label, (ev[Pembed.startOffsetKey] ? 0).asStringPrec(4));
	}
}

TimelineViewEventEnvNode : TimelineViewEventListNode {
	timelinePreviewClass {
		^TimelinePreview_Env
	}
}

TimelineViewEventSampleNode : TimelineViewEventListNode {
	timelinePreviewClass {
		^TimelinePreview_Sample
	}
}

////////////////////////////////////////////////////////////////

// node for EventLoop instead of EventList
// not maintened currently
// FIXME: lot of common code with TimelineViewEventListNode
TimelineViewEventLoopNode : TimelineViewEventListNode {

	init { arg xparent, nodeidx, event;
		parent = xparent;
		spritenum = nodeidx;
		model = event;

		preview = TimelinePreview.new;
		preview.areasize.x = parent.areasize.x;

		//[spritenum, model].debug(this.class.debug("CREATE EVENT NODE !"));

		action = {
			//[model, origin, extent].debug("node action before");
			model[timeKey] = origin.x;
			model[this.posyKey] = origin.y;
			model[lenKey] = extent.x;
		};

		refresh = {
			origin = Point(model[timeKey], model[this.posyKey]);
			color = ParamViewToolBox.color_ligth;
			outlineColor = Color.black;
			extent = Point(model.use { currentEnvironment[lenKey].value(model) } ? 1, 1); // * tempo ?
			label = model[\label] ? "unnamed";
			preview.mapEventList(model[\eventloop].list);
			//[spritenum, origin, extent, color].debug("node refresh");
		};

		this.makeUpdater;
		this.refresh;
		this.action;
	}

	makeUpdater {
		if(controller.notNil) {controller.remove};
		controller = SimpleController(model).put(\refresh, {
			{
				this.refresh;
			}.defer
		})
		// TODO: 
		// - add inner_controller to eventlist too
		// - free inner_controller
		// - I think eventloop doesnt broadcast signals, maybe he should listen to list also
		//		- maybe it's too much controller, think of another approach
		//inner_controller = SimpleController(model[\eventloop]).put(\refresh, {
		//	this.refresh;
		//})
	}
}


