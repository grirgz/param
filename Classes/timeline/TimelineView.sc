
// based on Thor Magnusson code - www.ixi-audio.net
// GNU licence - google it.


TimelineView : SCViewHolder {


	var <userView;
	var <selectionView;

	var >clock;
	var <viewport; // zero is topLeft, all is normalised between 0 and 1
	var <areasize;
	var <>preCreateNodeHook;
	var <>createNodeHook;
	var <>deleteNodeHook;
	var <>addHistorySnapshotHook;
	var <>paraNodes, connections; 
	var <chosennode; 
	var clickedNextNode; // used to change curve in mouse handlers
	var <>isClickOnSelection; 
	var <>selectionRefloc; 
	var <>quant;
	var <>enableQuant = true;
	var win;
	// virtualBounds because it may draw on a sub rectangle of a parent UserView
	var >virtualBounds, <>virtualBoundsOffsetX = 5, <>virtualBoundsOffsetY = 5;
	var downAction, upAction, trackAction, keyDownAction, rightDownAction, overAction, connAction;
	var <>mouseDownAction;
	var <>mouseUpAction;
	var <>mouseMoveAction;
	var <>deleteSelectedNodesHook;
	var backgrDrawFunc;
	var <>customDrawFunc;
	var background, fillcolor;
	var nodeCount, shape;
	var startSelPoint, endSelPoint;
	var rawStartSelPoint, rawEndSelPoint;
	var nilSelectionPoint;

	var refPoint, refWidth; // grid_clicked_point
	var chosennode_old_origin; // in grid units

	var gridRulerOffset = 5; // put the 0 mark at start event time

	var <>lastPixelPos;
	var <lastGridPos;
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

	var <>selectionCursor; // a CursorTimeline externally set
	var <>selectionCursorController;
	var <>previousNormSelRect;

	var <>changeCurveMode = false;

	var useSpecInConversions = true; // not used
	
	var <>currentBrush = \move;
	
	// debug
	
	var <>mygrid;

	//// keys

	var <>valueKey = \midinote;

	//// options

	var <>forbidHorizontalNodeMove = false;
	var <>stayingSelection = true;
	var <>quantizedSelection = true;
	var <>enablePreview = true;


	var <>parentTimeline; // used by PreviewTimeline for drawing

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

		//if((win= w).isNil, {
		//	win = GUI.window.new("ParaSpace",
		//		Rect(10, 250, bounds.left + bounds.width + 40, bounds.top + bounds.height+30));
		//	win.front;
		//});
		//Log(\Param).debug("initParaSpace: this:% %", this, this.hash);

		this.makeUpdater;
		action = {
			//model.debug("timeline action before");
			model.reorder;
		};

		selNodes = IdentitySet.new;
		//selNodes.debug("initParaSpace: selNodes after");
		quant = Point(1/8,1);
		viewport = viewport ?? Rect(0,0,1,1);
		areasize = areasize ?? Point(2,128);
		userView = UserView.new;
		selectionView = UserView.new;
		this.view = userView;
		this.view.onClose = this.view.onClose.addFunc({ this.free });
		this.view.addUniqueMethod(\timeline, { this });
 		selectionView.bounds = userView.bounds; 
 		
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

		lazyRefreshFunc = {
			this.refresh;
			refreshDeferred = false;
		};

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
		this.refreshEnabled = true;
	}

	specialInit {
		// to be overriden
	}

	clock {
		^(clock ? TempoClock.default)
	}

	/////////////////

	mapEventList { arg eventlist;
		//this.class.debug("mapEventList start --");
		//this.dumpBackTrace;

		model = eventlist;
		this.refreshEventList;
		this.makeUpdater;
		
		//[areasize, viewport, paraNodes].debug("mapEventList3");
		//debug("mapEventList end");
	}
		
	refreshEventList {
		//model.debug("refreshEventList");
		this.noRefreshDo {
			this.clearSpace;
			model.do { arg event;
				this.addEvent(event)
			};
		};
		this.refresh;
	}

	addHistorySnapshot {
		addHistorySnapshotHook.(this)
	}

	////////////////////////// properties
	
	areasize_ { arg val;
		//[this, areasize, val].debug("set areasize");
		if(val != areasize) {
			// copy else there is no changed msg sent because already sync
			areasize = val.copy;
			//[this, areasize, val].debug("send changed areasize");
			this.changed(\areasize);
		};
	}

	viewport_ { arg val;
		//[this, viewport, val].debug("set viewport");
		if(val != viewport) {
			viewport = val.copy;
			//[this, viewport, val].debug("send changed viewport");
			this.changed(\viewport);
		};
	}

	lastGridPos_ { arg val;
		lastGridPos = val;
		this.changed(\lastGridPos);
	}

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

	hasSelection {
		^this.startSelPoint != nilSelectionPoint and: {
			this.endSelPoint != nilSelectionPoint
		};
	}

	selectionRect { arg defaultVal;
		^Rect.fromPoints(this.startSelPoint ? defaultVal.copy, this.endSelPoint ? defaultVal.copy)
	}

	///////////////////////////////////////////////////////// input events handling
	
	createNode { arg gpos;
		//var nodesize = Point(1,1);
		var newpos;
		var newevent;

		//debug("---------mouseDownAction: create node mode");

		//nodesize = this.gridPointToNormPoint(nodesize);
		if(gpos.isNil) {
			gpos = lastGridPos;
		};

		if(enableQuant) {
			newpos = gpos.trunc(quant.value); 
		} {
			newpos = gpos; 
		};

		//"mouseDownAction: 1".debug;
		preCreateNodeHook.(newpos); // to call addHistorySnapshot
		newevent = this.eventFactory(newpos);
		if(newevent.notNil) {

			//Log(\Param).debug("new event!!!!!!!!! %", newevent);
			chosennode = this.addEvent(newevent);
			//newevent.debug("mouseDownAction: 2");
			//Log(\Param).debug("new event after add!!!!!!!!! %", newevent);

			createNodeDeferedAction = {
				//debug("mouseDownAction: 3");
				model.addEvent(newevent);
				//Log(\Param).debug("new event after add to model!!!!!!!!! %", newevent);
				//debug("mouseDownAction: 4");
				model.reorder; // reorder lose node selection
				//debug("mouseDownAction: 5");
				//model.changed(\refresh); // commented because refresh is already called in mouseUp
				//debug("mouseDownAction: 6");
				chosennode = nil; // chosennode was set to know which node to resize with mouseMove, but is not really selected
				createNodeHook.(newevent);
			};

			//[chosennode.posyKey, chosennode.model, chosennode.origin].debug("posy, model, origin");
			//[chosennode.width].debug("pppp");
			refPoint = newpos; // var used here for reference in trackfunc
			refWidth = chosennode.width;
		}

		//chosennode.debug("mouseDownAction: chosennode!");
	}

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
		//Log(\Param).debug("mouseDownAction_ px %,py %, npos %, gpos %", px, py, npos, gpos);

		chosennode = this.findNodeHandle(gpos.x, gpos.y);
		isClickOnSelection = this.hasSelection and: {
			this.selectionRect.contains(npos)
		};
		//[chosennode, chosennode !? {chosennode.model}].debug("mouseDownAction: chosennode");
		//[px, py, npos, gpos].debug("amouseDownAction_ px,py, npos, gpos");
		mouseDownAction.(me, px, py, mod, buttonNumber, clickCount, chosennode);

		case
		{ mod.isCtrl and: { buttonNumber == 1 } } {
			this.setEndPosition(gpos.x.trunc(quant.value.x));
		}
		{ buttonNumber == 0 and: { currentBrush == \eraser } } {
			// TODO
			if(chosennode.notNil and: { chosennode.isKindOf(TimelineViewLocatorLineNode).not }) {
				this.addHistorySnapshot;
				this.deleteNode(chosennode, true)
			};
		}
		{ chosennode.isNil and: {buttonNumber == 0} and: { mod.isCtrl or: { currentBrush == \pen } } } {
			// create node mode
			this.createNode(gpos);
		}
		{ mod.isShift and: { buttonNumber == 0 } } {
			if(chosennode !=nil) { // a node is selected
				//debug("---------mouseDownAction: prepare for resizing mode");
				refPoint = gpos; // var used here for reference in trackfunc
				refWidth = chosennode.width;
			};
			if(this.changeCurveMode) {
				var pair;
				pair = this.findPreviousAndNextNode(gpos.x, { arg node;
					//[ node, node.visible == true and: { node.class != TimelineViewLocatorLineNode } ].debug("merde");
					node.visible == true and: { node.class != TimelineViewLocatorLineNode }
				});
				//Log(\Param).debug("changeCurveMode: pair found %", pair);
				if(pair.notNil) {
					chosennode = pair.first;
					clickedNextNode = pair.last;
				} {
					chosennode = nil;
					clickedNextNode = nil;
				};
				if(chosennode.notNil) {
					refWidth = chosennode.curve;
				} {
					refWidth = nil;
				};
				refPoint = gpos;
			};
		}
		{
			if(chosennode !=nil, { // a node is selected
				refPoint = gpos; // var used here for reference in trackfunc
				chosennode_old_origin = chosennode.origin; // used for reference when moving chosennode
				refWidth = chosennode.width;

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

				if(isClickOnSelection == true) {
					// NOOP: already selected
				} {
					this.deselectAllNodes;
					this.selectNode(chosennode);
					this.clearSelectionRect;
				};
				//if(selNodes.size < 2) {
					////debug("---------mouseDownAction: deselect all and select clicked node");
					//this.deselectAllNodes(chosennode);
				//};
				downAction.value(chosennode);

				this.refresh;
			}, { 
				//Log(\Param).debug("mouseDownAction: start % end % sel", this.startSelPoint, this.endSelPoint);
				if(isClickOnSelection == true) {
					// clicked on the selection area, ready to move the selection
					selectionRefloc = this.startSelPoint;
					refPoint = gpos; // to know where mouse clicked before moving
				} {
					var oldsel;
					// no node is selected
					//Log(\Param).debug("---------mouseDownAction: deselect all and draw selrecti");
					
					oldsel = this.selectedNodes.size;
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
					//Log(\Param).debug("sel:% %", this.startSelPoint, this.endSelPoint);
					if(oldsel > 0) {
						this.refresh;
					};
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
		//Log(\Param).debug("------------- mouseUpAction: chosennode %", chosennode);
		if(chosennode != nil, { // a node is selected
			//Log(\Param).debug("mouseUpAction: a node is selected %", chosennode);
			createNodeDeferedAction.value; // function defined when a new node is created
			createNodeDeferedAction = nil;
			upAction.value(chosennode);
			selNodes.do({arg node; 
				node.refloc = node.nodeloc;
			});
			//this.refresh;
		},{ // no node is selected
			// find which nodes are inside selection rect
			var rect;
			var wasSelected = false;
			var ppos = Point(x,y);
			var gpos = this.pixelPointToGridPoint(ppos);
			//Log(\Param).debug("mouseUpAction: no node is selected");
			//Log(\Param).debug("mouseUpAction st %, en %", this.startSelPoint, this.endSelPoint);
			if(this.startSelPoint.notNil and: {this.endSelPoint.notNil}) {

				if(isClickOnSelection == true and: { refPoint == gpos }) {
					// click on selection but no move: cancel selection
					//Log(\Param).debug("isClickOnSelection %, refPoint %, gpos: %");
					this.startSelPoint = nilSelectionPoint;
					this.endSelPoint = nilSelectionPoint;
					this.refreshSelectionView;
				} {
					var found;
					rect = this.normRectToPixelRect(this.selectionRect);
					rect = rect.insetAll(-1,1,1,-1); // prevent selecting next nodes in grid
					rect = this.pixelRectToGridRect(rect);
					wasSelected = selNodes.size > 0;
					//selNodes = IdentitySet.new;
					this.deselectAllNodes;
					//Log(\Param).debug("find nodes in selection rect");
					found = this.findNodes(rect);
					//found.debug("found notes in selection rect");
					this.selectNodes(found);
					if(stayingSelection.not) {
						this.startSelPoint = nilSelectionPoint;
						this.endSelPoint = nilSelectionPoint;
						this.refreshSelectionView;
					};
					//Log(\Param).debug("end find");
				}
			} {
				// click on blank and no selection: unselect
				// FIXME: refresh is buggy, need to think again algo
				//		in fact, deselecting is done on mouse down, that's why this does nothing
				wasSelected = selNodes.size > 0;
				this.deselectAllNodes;
				//selNodes = IdentitySet.new;
				//this.changed(\selectedNodes); //does nothing
			};
			if(wasSelected or:{ selNodes.size > 0 }) {
				this.refresh;
				this.refreshSelection;
			}
		});
		this.updatePreviousNormSelRect;
	}

	updatePreviousNormSelRect {
		this.previousNormSelRect = this.selectionRect(Point(0,0));
	}

	mouseMoveActionBase {|me, px, py, mod|
		// if a node is clicked, move all selected nodes, else draw a selection rectangle

		var bounds = this.bounds;
		var x,y;
		var ppos = Point(px,py);
		var npos = this.pixelPointToNormPoint(ppos);
		var gpos = this.pixelPointToGridPoint(ppos);
		var nquant = this.gridPointToNormPoint(this.quant.value);
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
		{ (mod.isShift or: mod.isCtrl or:{ currentBrush == \pen })  and: { buttonNumber == 0 } } {
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
				var newcurve;
				var node = chosennode;
				if(node.notNil) {
					var direction = 1;
					direction = if(clickedNextNode.notNil and: {clickedNextNode.origin.x < chosennode.origin.x} ) {
						-1
					} {
						1;
					};
					newcurve = refWidth + ( refPoint.y - gpos.y / this.pixelExtentToGridExtent(Point(1,1)).y / 10 * direction );
					//Log(\Param).debug("newcurve %, gpos %, ref %", newcurve, gpos, refPoint);
					node.model.curve = newcurve;
					model.changed(\redraw);
					node.refresh;
					this.refresh;
				} {
					Log(\Param).debug("changeCurveMode: node nil %", gpos);
				}
			}


		} {
			////// moving

			if( buttonNumber == 0 and: { currentBrush != \eraser } ) {

				////// move selection
				if(isClickOnSelection == true ) {
					var freeze_position = false;
					var lownode, clipped_npos, clipped_gpos, clipped_newpos;
					var pixel_newpos_point, pixel_clicked_point, pixel_click_offset, grid_diff, chosennode_new_origin;
					var norm_diff;
					//debug("---------mouseMoveAction: move selection");

					if(selNodes.size > 0) {

						lownode = selNodes.asArray.first;
						selNodes.do { arg node, idx;
							if(node.refloc.y < lownode.refloc.y) {
								lownode = node
							};
						};

						if(freeze_position) {
							// is freeze_position mode, low nodes will block whole selection from moving lower
							// only low clip is implemented, need to do high clip value

							clipped_npos = npos.clip(this.gridPointToNormPoint(refPoint) - this.gridPointToNormPoint(lownode.refloc),inf);
							clipped_gpos = this.normPointToGridPoint(clipped_npos);

							// move whole selection, quantize on selection left edges
							grid_diff = (clipped_gpos - refPoint).trunc(this.quant.value);
							norm_diff = (clipped_npos - this.gridPointToNormPoint(refPoint)).trunc(nquant);
						} {
							grid_diff = (gpos - refPoint).trunc(this.quant.value);
							norm_diff = (npos - this.gridPointToNormPoint(refPoint)).trunc(nquant);

						};

						if(forbidHorizontalNodeMove) {
							grid_diff.x = 0;
							norm_diff.x = 0;
						};

						//useSpecInConversions = false; // FIXME: this doesn't work with exponential spec 
						//norm_diff = this.gridPointToNormPoint(grid_diff); // TimelineEnvView use spec so can't go below zero, pass the flag to avoid this
						//useSpecInConversions = true;

						//Log(\Param).debug("before grid_diff %", grid_diff);


						//Log(\Param).debug("lownode % y=% ny=%", lownode, lownode.refloc.y, this.gridPointToNormPoint(lownode.refloc).y);
						//Log(\Param).debug("norm_diff.y %", norm_diff.y);
						//norm_diff.y = norm_diff.y.clip(this.gridPointToNormPoint(lownode.refloc).y.neg, inf);
						//norm_diff.y.clip(this.gridPointToNormPoint(lownode.refloc).y.neg, inf).debug("norm_diff.y after");
						//Log(\Param).debug("norm_diff.y afterk%", norm_diff.y);
						//grid_diff.y = this.normPointToGridPoint(Point(0,norm_diff.y.neg)).y.neg;

						//if( // a node is going out of frame, abort moving
						//lownode = selNodes.detect { arg node;
						//mpos = this.gridPointToNormPoint(node.refloc) + norm_diff;
						//mpos.debug("mpos");
						//mpos.y < 0; // FIXME: should use spec
						//};
						//lownode.notNil;
						//) {
						////grid_diff = Point(0,0)
						////norm_diff.y.debug("normdiff before");
						//Log(\Param).debug("lownode");

						//Log(\Param).debug("Prevent out of bound node: %, mposy=%", lownode, mpos.y);
						//grid_diff.y = grid_diff.y.clip(lownode.nodeloc.y, inf);
						//////grid_diff.y = grid_diff.y - mpos.y
						////this.gridPointToNormPoint(lownode.refloc).y.debug("refloc norm y");
						////norm_diff.y = norm_diff.y.clip(0, this.gridPointToNormPoint(lownode.refloc).y);
						////grid_diff = this.normPointToGridPoint(norm_diff);
						////norm_diff.y.debug("normdiff after");
						//};


						//Log(\Param).debug("after grid_diff %", grid_diff);

						this.noRefreshDo {
							selNodes.do { arg node;
								if(freeze_position) {
									clipped_newpos = node.refloc + grid_diff
								} {
									clipped_newpos = this.gridPointToNormPoint(node.refloc) + norm_diff;
									clipped_newpos = this.normPointToGridPoint(clipped_newpos);
								};
								//node.debug("setLoc %".format(clipped_newpos));
								node.setLoc(clipped_newpos)
							};
						};
						//Log(\Param).debug("end move");

						this.changed(\nodeMoved);
					} {

						norm_diff = (npos - this.gridPointToNormPoint(refPoint)).trunc(nquant);
					};

					this.startSelPoint = this.previousNormSelRect.origin + norm_diff;
					this.endSelPoint = this.previousNormSelRect.rightBottom + norm_diff;

					//Log(\Param).debug("sel %, prevsel %, normdif % gdiff %", this.startSelPoint, this.previousNormSelRect, norm_diff, grid_diff);
					this.refresh;
				} {
					////// move node
					if(chosennode != nil) { // a node is selected
						var pixel_newpos_point, pixel_clicked_point, pixel_click_offset, grid_diff, chosennode_new_origin;
						var norm_diff;
						//debug("---------mouseMoveAction: move node");
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

						useSpecInConversions = false;
						norm_diff = this.gridPointToNormPoint(grid_diff);

						//this.startSelPoint = this.previousNormSelRect.origin + norm_diff;
						//this.endSelPoint = this.previousNormSelRect.rightBottom + norm_diff;
						useSpecInConversions = true;

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
					} {
						////// draw selection rect
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
					}
				}
			};


			///////////////////////////


			//if( buttonNumber == 0 ) {

				//if(chosennode != nil) { // a node is selected
					//var pixel_newpos_point, pixel_clicked_point, pixel_click_offset, grid_diff, chosennode_new_origin;
					//var norm_diff;
					//debug("---------mouseMoveAction: move node");
					////Log(\Param).debug("---------mouseMoveAction: move mode");
					////debug("======= selected nodes will be moved!!!");
					////selNodes.collect({ arg x; [x.origin, x.extent, x.model] }).debug("======= selected nodes will be moved!!!");

					//// ----------new algo
					//// - pixel_click_offset = determine diff in pixel from clicked point to origin point of chosennode (chosennode_old_origin)
					//// - the new location of node is the point where the mouse has moved (newpos) minus the pixel_click_offset
					//// - then convert it to grid unit and quantize it
					//// - now determine the diff between old node location and new node location (grid_diff) and apply this change to all selected nodes
					//// - since the function is called continously, chosennode_old_origin should be fixed to the first position of node (refloc), and not his
					////		position changing continuously

					//chosennode_old_origin = chosennode.refloc;
					//pixel_clicked_point = this.gridPointToPixelPoint(refPoint);
					//pixel_newpos_point = ppos;
					//pixel_click_offset = pixel_clicked_point - this.gridPointToPixelPoint(chosennode_old_origin);
					//chosennode_new_origin = this.pixelPointToGridPoint(pixel_newpos_point - pixel_click_offset);
					//chosennode_new_origin = this.quantizeGridPoint(chosennode_new_origin);

					//if(this.forbidHorizontalNodeMove == true) {
						//chosennode_new_origin.x = chosennode_old_origin.x;
					//};

					//chosennode.setLoc(chosennode_new_origin);
					//grid_diff = chosennode_new_origin - chosennode_old_origin;

					//selNodes.do { arg node;
						//node.setLoc(node.refloc + grid_diff)
					//};

					//useSpecInConversions = false;
					//norm_diff = this.gridPointToNormPoint(grid_diff);

					////this.startSelPoint = this.previousNormSelRect.origin + norm_diff;
					////this.endSelPoint = this.previousNormSelRect.rightBottom + norm_diff;
					//useSpecInConversions = true;

					//// ----------debug algo
					////pixel_clicked_point = this.gridPointToPixelPoint(refPoint);
					////pixel_newpos_point = ppos;
					////pixel_click_offset = pixel_clicked_point - this.gridPointToPixelPoint(chosennode_old_origin);
					////chosennode_new_origin = this.pixelPointToGridPoint(pixel_newpos_point - pixel_click_offset);

					////chosennode.setLoc(chosennode_new_origin);

					////-----------


					//this.changed(\nodeMoved);
					////debug("======= selected nodes was moved!!!");
					////selNodes.collect({ arg x; [x.origin, x.extent, x.model] }).debug("======= selected nodes was moved!!!");
					////model.print;  // debug
					//this.refresh;
				//} { // no node is selected
					//if(isClickOnSelection == true ) {
						//var pixel_newpos_point, pixel_clicked_point, pixel_click_offset, grid_diff, chosennode_new_origin;
						//var norm_diff;
						//debug("---------mouseMoveAction: move selection");
						//// move whole selection, quantize on selection left edges
						//grid_diff = (gpos - refPoint).trunc(this.quant.value);

						//selNodes.do { arg node;
							//node.setLoc(node.refloc + grid_diff)
						//};

						//useSpecInConversions = false; // FIXME: this doesn't work with exponential spec 
						//norm_diff = this.gridPointToNormPoint(grid_diff); // TimelineEnvView use spec so can't go below zero, pass the flag to avoid this

						//this.startSelPoint = this.previousNormSelRect.origin + norm_diff;
						//this.endSelPoint = this.previousNormSelRect.rightBottom + norm_diff;
						//useSpecInConversions = true;

						////Log(\Param).debug("sel %, prevsel %, normdif % gdiff %", this.startSelPoint, this.previousNormSelRect, norm_diff, grid_diff);
						//this.changed(\nodeMoved);
						//this.refresh;

					//} {

						//if( this.startSelPoint != nilSelectionPoint ) {
							//if(quantizedSelection) {
								//var realLeftTop = { arg rect;
									//// the first point of the selection can be after the second point
									//// in this case we reverse it
									//var x = rect.origin.x;
									//var y = rect.origin.y;
									//if(rect.height < 0) {
										//y = rect.origin.y + rect.height;
									//};
									//if(rect.width < 0) {
										//x = rect.origin.x + rect.width;
									//};
									//Point(x,y)
								//};
								//var realRightBottom = { arg rect;
									//var x = rect.rightBottom.x;
									//var y = rect.rightBottom.y;
									//if(rect.height < 0) {
										//y = rect.rightBottom.y - rect.height;
									//};
									//if(rect.width < 0) {
										//x = rect.rightBottom.x - rect.width;
									//};
									//Point(x,y)
								//};
								//var selrec = Rect.fromPoints(rawStartSelPoint, npos);
								//var leftTop = realLeftTop.(selrec);
								//var rightBottom = realRightBottom.(selrec);
								//var qleftTop = leftTop.trunc(nquant);
								//var qrightBottom = rightBottom.trunc(nquant) + nquant;
								////Log(\Param).debug("rstart % rend % selrec % leftTop % % rightBottom % % ", rawStartSelPoint, npos, selrec, leftTop, qleftTop, rightBottom, qrightBottom);
								//this.startSelPoint = qleftTop;
								//this.endSelPoint = qrightBottom;
								//rawEndSelPoint = npos;
							//} {
								//this.endSelPoint = npos;
							//};
							//this.refreshSelectionView;
						//}
					//};
				//}
			//}
		};
	}

	mouseWheelActionBase { arg view, x, y, modifiers, xDelta, yDelta;
		var newport;
		var oldport;
		var top;
		var minport = ( 1/this.virtualBounds.extent ).clip(0,0.5);
		//[ view, x, y, modifiers, xDelta, yDelta ].debug("mouseWheelAction");
		if(modifiers.isCtrl) { // zoom horizontally
			oldport = this.viewport;
			newport = oldport.insetBy( ( oldport.extent.x * ( yDelta.clip2(150)/300 + 1 ) ) - oldport.extent.x, 0).sect(Rect(0,0,1,1));
			if(newport.extent.x >= minport.x) {
				
				this.viewport = newport;
				//this.viewport.debug("end viewport");
				this.refresh;
			};
			//newport.extent = Point(newport.extent.x.clip(minport.x,1), newport.extent.y.clip(minport.y,1));
		};
		if(modifiers.isShift) { // zoom horizontally
			oldport = this.viewport;
			newport = oldport.insetBy(0, ( oldport.extent.y * ( yDelta.clip2(150)/300 + 1 ) ) - oldport.extent.y).sect(Rect(0,0,1,1));
			if(newport.extent.y >= minport.y) {
				
				this.viewport = newport;
				//this.viewport.debug("end viewport");
				this.refresh;
			};
			
		};
		if(modifiers.isCtrl.not and: { modifiers.isShift.not }) {
			oldport = this.viewport;
			top = ( oldport.top + ( yDelta/this.virtualBounds.height/4 ) ).clip(0,1-oldport.height);
			newport = Rect(oldport.left, top, oldport.width, oldport.height);
			//[oldport, newport, oldport.height, oldport.top, oldport.bottom].debug("oldport, newport");
			this.viewport = newport;
			this.refresh;
		};
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
		this.refresh;
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


		// connecting

		if(unicode == 99, {conFlag = true;}); // c is for connecting

		// refresh

		if(key == $r) {
			this.refreshEventList;
		};

		// hook

		keyDownAction.value(me, key, modifiers, unicode, keycode);
		//this.refresh;
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
		//this.class.debug("setEndPosition start --");
		model.setEndPosition(time);
		//model.print;
		//this.class.debug("setEndPosition done, now refresh");
		this.refreshEventList;
		//this.class.debug("setEndPosition end");
	}

	setStartPosition { arg time;
		model.setStartPosition(time);
		//model.print;
		this.refreshEventList;
	}

	eventFactory { arg pos, len;
		var nodesize = Point(len ?? { this.quant.value.x },1);
		// why nodesize is in normalized form ???
		//nodesize = this.gridPointToNormPoint(nodesize);
		if(eventFactory.isNil) {
			//Log(\Param).debug("TimelineView: eventFactory is nil");
			^(absTime: pos.x, midinote: pos.y, sustain:nodesize.x);
		} {
			//Log(\Param).debug("TimelineView: eventFactory is %", eventFactory.asCompileString);
			^eventFactory.(pos, nodesize.x);
		}
	}

	copyAtSelectionEdges {
		var selrect;
		var el;
		var nodes;
		if(this.hasSelection) {

			selrect = this.normRectToGridRect(this.selectionRect);
			el = this.model.clone;
			nodes = el.collect({ arg ev; this.nodeClass.new(this, 0, ev) });
			this.findCrossedNodes(selrect.leftTop, selrect.leftBottom, nodes).do { arg node;
				this.copySplitNode(el, node, selrect.left)
			};
			nodes = el.collect({ arg ev; this.nodeClass.new(this, 0, ev) });
			this.findCrossedNodes(selrect.rightTop, selrect.rightBottom, nodes).do { arg node;
				this.copySplitNode(el, node, selrect.right)
			};
			nodes = el.collect({ arg ev; this.nodeClass.new(this, 0, ev) });
			//nodes.do({  arg nn; // debug
			//[nn.rect, nn.model].debug("copyAtSelectionEdges3 nodes");
			//});
			^this.findContainedNodes(selrect, nodes).collect(_.model);
			//^noel.select { arg ev;
			//var node = this.nodeClass.new(this, 0, ev);
			//Log(\Param).debug("copyAtSelectionEdges: selrect:%, noderect:%, contains:%", selrect, node.rect, selrect.contains(node.rect));
			//selrect.contains(node.rect);
			//}
			//Rect(5,5,10,10).contains(Rect(6,6,9,3))
		} {
			if(chosennode.notNil) {
				^[chosennode.model]
			};
		};
	}

	splitAtSelectionEdges {
		var selrect = this.normRectToGridRect(this.selectionRect);
		this.findCrossedNodes(selrect.leftTop, selrect.leftBottom).do { arg node;
			this.splitNode(node, selrect.left)
		};
		this.findCrossedNodes(selrect.rightTop, selrect.rightBottom).do { arg node;
			this.splitNode(node, selrect.right)
		};
	}

	selectionHasCrossingNodes {
		if(this.hasSelection) {
			var selrect = this.normRectToGridRect(this.selectionRect);
			if(selrect.notNil) {
				^this.findCrossedNodes(selrect.leftTop, selrect.leftBottom).size > 0 or: {
					this.findCrossedNodes(selrect.rightTop, selrect.rightBottom).size > 0
				};
			};
		};
		^false;
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

		( y_lines_count.asInteger + 1 ).do { arg py;
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
					//x = x + this.gridPointToPixelPoint(gridRulerOffset,0).x;
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

					Pen.line(Point(x,this.virtualBounds.origin.y), Point(x,this.virtualBounds.origin.y + this.virtualBounds.height));
					Pen.stroke;
				}
			}, gridRulerOffset);

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



		/////////////// background

		pen.width = 1;
		pen.color = background; // background color
		pen.fillRect(bounds); // background fill
		backgrDrawFunc.value; // background draw function


		/////////////// grid

		this.drawGridX;
		this.drawGridY;

		/////////////// background frame
		// after grid to erase black grid frame to be pretty

		pen.color = Color.white;
		pen.strokeRect(bounds); 
		
		/////////////// the lines

		pen.color = Color.black;
		connections.do({arg conn;
			pen.line(this.normPointToPixelPoint(paraNodes[conn[0]].nodeloc)+0.5, this.normPointToPixelPoint(paraNodes[conn[1]].nodeloc)+0.5);
		});
		pen.stroke;

		/////////////// end line

		this.drawEndLine;
		
		/////////////// the optional curve

		this.drawCurve;
		
		/////////////// the nodes or circles

		this.drawNodes;

		/////////////// the optional waveform

		this.drawWaveform;

		/////////////// the selection node

		//this.drawSelection; // on its own layer now


		//////////////// custom draw func

		this.customDrawFunc.();

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
		var screen_gridrect;
		var onScreen = { arg node;
			// rendering is way faster with this function
			//[ this.class, node.class, node.rect, screen_gridrect, node.rect.intersects(screen_gridrect) ].debug("onScreen node");
			node.rect.intersects(screen_gridrect)
		};
		//this.class.debug("<<<<<<<<<<<< start drawing nodes");
		screen_gridrect = this.pixelRectToGridRect(this.virtualBounds);
		//[this.bounds, this.virtualBounds].debug("bounds, virtualBounds");
		//[this.class, paraNodes.size, paraNodes.select(onScreen).size].debug("drawNodes onScreen filter result");

		//[this.viewport, this.bounds, this.virtualBounds, this.areasize].debug("drawNodes:bounds");
		paraNodes.select(onScreen).do({arg node;
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
		// not used anymore, endEvent is nil, end event have its own draw function
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

	defineZeroMarkAtStartTime { arg time;
		// TODO: does not work, should find how to add this offset to grid drawing
		time = time ? this.model.startTime;
		gridRulerOffset = time;
	}

	/////////////////////////////////////////////////////////

	bounds { 
		^this.view.bounds
	}

	virtualBounds {
		// bounds.origin is the pos of the view on the parent, but virtualBounds.origin should be zero, or a margin offset
		// it is used when we want to draw the view in a small rect inside a parent UserView like the preview of nodes
		var offsetx = virtualBoundsOffsetX;
		var offsety = virtualBoundsOffsetY;
		//var offset = 15;
		// virtualBounds use screen coordinates
		//^(virtualBounds ? Rect(0,0,this.bounds.width, this.bounds.height));
		// offset is multiplied by two because length is reduced by both left offset and right offset
		^(virtualBounds ?? { Rect(offsetx,offsety,this.bounds.width-( offsetx*2 ), this.bounds.height-( offsety*2 )) });
	}

	makeUpdater {
		if(controller.notNil) {controller.remove};
		controller = SimpleController(model).put(\refresh, {
			if(this.view.isNil) {
				controller.remove;
			} {
				//"TimelineView get a refresh signal!".debug;
				ParamViewToolBox.refreshLimit(this, {
					{
						this.refreshEventList;
					}.defer
				})
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
		var rect_copy_horizontal = { arg me, rect;
			me = me.copy;
			me.width = rect.width;
			me.origin = Point(rect.origin.x, me.origin.y);
			me;
		};
		var rect_copy_vertical = { arg me, rect;
			me = me.copy;
			me.height = rect.height;
			me.origin = Point(me.origin.x, rect.origin.y);
			me;
		};

		if(timeline_controller.notNil) {timeline_controller.remove};
		timeline_controller = SimpleController(timeline)
			.put(\viewport, {
				if(this.view.isNil or: { this.view.isClosed }) {
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
				if(this.view.isNil or: {this.view.isClosed}) {
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
				//this.view.refresh;
				this.refresh;
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
				if(this.view.isNil or: {this.view.isClosed}) {
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

	setViewportToClipBounds { arg totalDur;
		var el = this.model;
		this.areasize = Point(el.endTime, this.areasize.y);
		this.viewport = this.gridRectToNormRect(Rect(el.startTime, 0, totalDur ?? { el.totalDur }, this.areasize.y));
	}

	viewWithCursor {
		// WIP
		var cursorview = CursorTimelineView.new;
		^StackLayout(
			cursorview.view,
			this.selectionView,
			this.view,
		).mode_(1)
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
		//^(point / this.bounds.extent * areasize * viewport.extent);
		^(point / this.virtualBounds.extent * areasize * viewport.extent);
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
		^this.addEventRaw(event);
		//switch(event[\type],
			//\xxx, {

			//},
			////\start, {
			////	"start".debug;
			////	^nil;
			////},
			//////\locator, {
			//////	event.debug("label");
			//////	^nil;
			//////},
			////\end, {
			////	"end".debug;
			////	endEvent = event;
			////	^nil;
			////},
			//// else
			//{
				//^this.addEventRaw(event);
			//}
		//)
	}

	addEventRaw { arg event;
		var node;
		node = this.nodeClass.new(this, nodeCount, event);
		nodeCount = nodeCount + 1;
		paraNodes.add(node);
		this.initNode(node);
		//createNodeHook.(node, nodeCount); // moved this to real event creation, this is graphic creation
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
		//selNodes.debug("selectNode: selNodes after");
		this.changed(\selectedNodes)
	}

	selectNodes { arg nodes;
		// method for sending only one changed signal when multiple nodes are selected
		nodes.do { arg node;
			node.selectNode;
		};
		selNodes.addAll(nodes);
		//selNodes.debug("selectNodeS: selNodes after");
		this.changed(\selectedNodes);
		//selNodes.debug("selectNodeS: selNodes after2");
	}

	deselectNode { arg node;
		node.deselectNode;
		selNodes.remove(node);
		//selNodes.debug("DEselectNode: selNodes after2");
		this.changed(\selectedNodes)
	}

	deselectNodes { arg nodes;
		nodes.do { arg node;
			node.deselectNode;
		};
		selNodes.removeAll(nodes);
		//selNodes.debug("DEselectNodeS: selNodes after2");
		this.changed(\selectedNodes);
		//this.refresh;
	}

	deselectAllNodes { arg excluded;
		var nodes;
		// deselect all but chosennode
		// should use selNodes, but at least we are sure there are no more selected nodes
		if(excluded.notNil) {
			nodes = paraNodes.reject({ arg x; x === excluded });
		} {
			nodes = paraNodes;
		};
		// when selecting a locator node then creating a node, it seems selNodes contains a node not in paraNodes
		nodes = nodes ++ selNodes; // remove them
		this.deselectNodes(nodes);
	}

	clearSelectionRect {
		this.startSelPoint = nilSelectionPoint;
		this.endSelPoint = nilSelectionPoint;
		this.refreshSelectionView;
	}
	
	clearSpace {
		paraNodes.do { arg node;
			node.free;
		};
		selNodes = IdentitySet.new;
		//selNodes.debug("clearSpace: selNodes after");
		//this.dumpBackTrace;
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
		model.removeEvent(node.model, refresh);
		if(refresh == true, {this.refresh});
	}
	
	reconstruct { arg aFunc; // deprecated for noRefreshDo
		refreshEnabled = false;
		aFunc.value( this );
		refreshEnabled = true;
		this.refresh;
	}

	refresh {
		//Log(\Param).debug("refresh called %", this);
		if(this.refreshEnabled != false) {
			{
				//Log(\Param).debug("refresh run %", this);
				//this.dumpBackTrace;
				userView.refresh;
				selectionView.refresh;
			}.defer; 
		};
	}

	noRefreshDo { arg fun;
		var oldrefresh = this.refreshEnabled;
		var ret;
		this.refreshEnabled = false;
		ret = fun.();
		this.refreshEnabled = oldrefresh;
		^ret;
	}

	refreshSelection {
		model.changed(\selection);
	}

	refreshSelectionView {
		if( this.refreshEnabled, { {
			selectionView.refresh;
		}.defer; });
	}

	lazyRefresh { arg fun;
		if( refreshDeferred.not, {
			AppClock.sched( 0.02, fun ? lazyRefreshFunc );
			refreshDeferred = true;
		});
	}

	findNodeHandle { arg x, y;
		// this return only node if clicked on the label part
		// this allow to start dragged selection even when screen is crowded by nodes
		// useful in ClipTimelineView and ParamTimeline
		^this.findNode(x,y)
	}
	
	// local function
	findNode {arg x, y;
		// findNode take x and y in grid coordinates, because TimelineNode.rect is in grid coordinates
		// this use .handleRect instead of .rect because it is used when clicking on a node
		var point = Point.new(x,y);
		var found;
		//Log(\Param).debug("::: findNode: %", point);
		if(chosennode.notNil and: {chosennode.handleRect.containsPoint(point)}) {
			// priority to the already selected node
			^chosennode;
		};
		found = this.getNodesNearPosx(point.x).reverse.detect { arg node;
			node.selectable and: {node.handleRect.containsPoint(point)}
		};
		//found.debug("findNode: found");
		if(found.notNil) {
			^found
		};
		//Log(\Param).debug("findNode: not found");
		^nil;
	}

	findCrossedNodes { arg startPoint, endPoint, nodes;
		var rect = Rect.fromPoints(startPoint, endPoint);
		^this.getNodesNearRange(rect.left, rect.right, nodes).select({arg node; 
			var point = node.origin;
			//node.spritenum.debug("spritnum");
			//[rect, point].debug("findNodes");
			node.selectable and: {Rect.fromPoints(startPoint, endPoint).intersects(node.rect)}
		});
	}

	getNodeSliceIndex { arg x, nodes, startIdx=0;
		// cut the paraNodes in slices and return start and end indexes of the slice containing x
		// x is in grid coordinates
		var step, cidx;
		var upperidx;
		var loweridx;
		var psize;
		nodes = nodes ? paraNodes;
		psize = nodes.size;
		cidx = startIdx;
		if(psize < 50) {
			^[0,psize-1]
		};
		if(psize > 1000) {
			step = ( psize / 100 ).asInteger.clip(1, inf);
		} {
			step = ( psize / 10 ).asInteger.clip(1, inf);
		};
		while { 
			cidx < psize and:{
				nodes[cidx].origin.x < x 
			}
		} {
			cidx = cidx + step;
		};
		cidx = cidx;
		upperidx = cidx.clip(0, psize-1).asInteger;
		loweridx = ( cidx - step ).clip(0, psize-1).asInteger;
		//[loweridx, upperidx, step, psize].debug("findNode: node is between, step, total");
		^[loweridx, upperidx]
	}

	getNodesNearRange { arg left, right, nodes;
		var startidx, endidx;
		startidx = this.getNodeSliceIndex(left, nodes).first;
		endidx = this.getNodeSliceIndex(right, nodes, startIdx:startidx).last;
		^( nodes ? paraNodes )[startidx..endidx]
	}

	getNodesNearPosx { arg x, nodes;
		var range;
		range = this.getNodeSliceIndex(x, nodes);
		^( nodes ? paraNodes )[range.first..range.last]
	}

	findNodes { arg rect, nodes;
		//if(~debugfind == true) {
			
		//^paraNodes.select { arg node;
			////[node, node.origin, node.selectable, rect.containsPoint(node.origin),rect].debug("findNodes: node");
			//node.selectable and: {rect.containsPoint(node.origin)};
		//};
		//} {

		^this.getNodesNearRange(rect.left, rect.right, nodes).select { arg node;
			//[node, node.origin, node.selectable, rect.containsPoint(node.origin), rect].debug("findNodes: node");
			node.selectable and: {rect.containsPoint(node.origin)};
		};
		//};
	}

	findPreviousNode { arg gposx;
		^this.getNodesNearPosx(gposx).reverse.detect { arg node;
			//Log(\Param).debug("findPreviousNode gposx %, origin %, %", gposx, node.origin, node.origin.x >= gposx);
			node.origin.x <= gposx
		};
	}

	findPreviousAndNextNode { arg gposx, includeFilter;
		var idx;
		var revnodes;
		revnodes = this.getNodesNearPosx(gposx).reverse;
		idx = revnodes.detectIndex { arg node;
			//Log(\Param).debug("findPreviousAndNextNode gposx %, origin %, %", gposx, node.origin, node.origin.x >= gposx);
			( includeFilter.isNil or: { includeFilter.value(node) == true } ) and: {node.origin.x <= gposx }
		};
		if(idx.notNil) {
			^[revnodes[idx], revnodes[idx-1]]
		} {
			^nil
		}
	}

	findContainedNodes { arg rect, nodes;
		var contains = { arg rect, smallrect; 
			rect.left <= smallrect.left or: {
				rect.left.equalWithPrecision(smallrect.left)
			} and: {
				rect.top <= smallrect.top
			} and: {
				rect.containsPoint(smallrect.rightBottom)
			};
		};
		^this.getNodesNearRange(rect.left, rect.right, nodes).select({arg node; 
			//node.spritenum.debug("spritnum");
			//[rect, point].debug("findNodes");

			//[node.class, node.rect, node.selectable,  contains.(rect, node.rect), rect.contains(node.rect), rect ].debug("findContainedNodes");
			node.selectable and: { contains.(rect, node.rect)}
		});
	}

	free {
		//Log(\Param).debug("TimelineView: free % %", this, this.hash);
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
	//findNodeHandle { arg x, y;
		//// this return only node if clicked on the label part
		//// this allow to start dragged selection even when screen is crowded by nodes
		//// findNode take x and y in grid coordinates, because TimelineNode.rect is in grid coordinates
		//var point = Point.new(x,y);
		//if(chosennode.notNil and: {chosennode.handleRect.containsPoint(point)}) {
			//// priority to the already selected node
			//^chosennode;
		//};
		//paraNodes.reverse.do({arg node;  // reverse because topmost is last
			////node.spritenum.debug("spritnum");
			////[node.rect, point].debug("findNode");
			//if(node.selectable and: {node.handleRect.containsPoint(point)}, {
				////[node.rect, point].debug("findNode: found!!");
				//^node;
			//});
		//});
		//^nil;
	//}
	

}



//////////////////////////////////////////////////////////////
//////////////////////////////// Nodes


//// dispatcher

TimelineViewNode {
	classvar <>nodedict;
	*initClass {
		nodedict = [
			\start, { arg parent, nodeidx, event;
				var res = TimelineViewLocatorLineNode(parent, nodeidx, event);
				res.alpha = 1;
				res;
			},
			\end, { arg parent, nodeidx, event;
				var res = TimelineViewLocatorLineNode(parent, nodeidx, event);
				res.alpha = 1;
				res;
			},
			\eventenv, { arg parent, nodeidx, event;
				TimelineViewEventEnvNode(parent, nodeidx, event)
			},
			\eventlist, { arg parent, nodeidx, event;
				TimelineViewEventListNode(parent, nodeidx, event)
			},
			\timeline, { arg parent, nodeidx, event;
				switch(event.timeline.eventType,
					\paramTimeline, {
						TimelineViewEventEnvNode(parent, nodeidx, event)
					},
					\sampleTimeline, {
						TimelineViewEventSampleNode(parent, nodeidx, event)
					}, {
						TimelineViewEventListNode(parent, nodeidx, event)
					}
				)
			},
			\player, { arg parent, nodeidx, event;
				TimelineViewEventListNode(parent, nodeidx, event)
			},
			\pattern, { arg parent, nodeidx, event;
				TimelineViewEventListNode(parent, nodeidx, event)
			},
			\eventloop, { arg parent, nodeidx, event;
				TimelineViewEventLoopNode(parent, nodeidx, event)
			},
			\locator, { arg parent, nodeidx, event;
				TimelineViewLocatorLineNode(parent, nodeidx, event)
			},
		].asDict;
		
	}
	*new { arg parent, nodeidx, event;
		var type;
		var node;
		var nodedict;
		// FIXME: choose a better type system
		type = event[\nodeType] ? event[\eventType] ? event[\type];
		if(event[\timeline].notNil ) {
			type = \timeline;
		};

		//Log(\Param).debug("% %".format("TimelineViewNode: new: parent, nodeType/type",[ parent.class, type.asCompileString ]));
		//Log(\Param).debug("TimelineViewNode: nodeType %".format([ event[\nodeType], event, event.parent ]));
		//

		node = this.nodedict[type] ??  {
			{ TimelineViewEventNode(parent, nodeidx, event) }
		};
		node = node.value(parent, nodeidx, event);

		//Log(\Param).debug("node created: % %".format(node.class, node));

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
	var <>selected = false;
	var <>deletable = true;
	var <>visible = true;
	var <>enablePreview = true;
	var <>preview;
	*new {
		^super.new
	}
}

TimelineViewEventNode : TimelineViewNodeBase {
	var <>refreshLimiter;
	var <>colorSelected;
	var <>colorDeselected;

	var <>refloc; // used to store start point when moving node, should be named oldOrigin

	*new { arg parent, nodeidx, event;
		^super.new.init(parent, nodeidx, event).baseInit;
	}

	baseInit {
		this.colorDeselected = Color.black;
		this.colorSelected = ParamViewToolBox.color_ligth.complementary;
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

		action = { // change model according to graphical state
			//[model, origin, extent].debug("node action before");
			model[timeKey] = origin.x;
			model[this.posyKey] = origin.y;
			model[lenKey] = extent.x;
		};

		refresh = { // refresh graphical state from model
			origin = Point(model[timeKey], model[this.posyKey] ? this.defaultPosyValue);
			color = ParamViewToolBox.color_ligth;
			outlineColor = outlineColor ? Color.black;
			extent = Point(model.use { currentEnvironment[lenKey].value(model) } ? 1, this.defaultHeight); // * tempo ?
			//[spritenum, origin, extent, color].debug("node refresh");
		};

		this.makeUpdater;
		this.refresh;
		//this.action; // FIXME: why calling action ?
	}

	enablePreview {
		^parent.enablePreview;
	}

	selectable {
		^[\rest, \start, \end, \locator].includes(this.model[\type]).not
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
		//parent.model.changed(\refresh);
		this.refreshEnabledDo {
			parent.action;
			model.changed(\refresh);
			parent.model.changed(\redraw);
		}
	}

	refreshEnabledDo { arg fun;
		if(parent.notNil and: { parent.refreshEnabled == true}) {
			^fun.()
		};
		^nil
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
				this.free;
			} {
				ParamViewToolBox.refreshLimit(this, {
					//{
						this.refresh;
					//}.defer
				})
			}
		})
	}

	refresh {
		refresh.()
	}

	refresh_ { arg fun;
		refresh = fun;
	}

	//draw {
		//var myrect;
		//var pos;
		//Pen.color = this.color;
		//pos = this.origin;
		//myrect = parent.gridRectToPixelRect(this.rect);
		////[spritenum, rect, this.class].debug("draw");
		//Pen.fillRect(myrect);
		//Pen.color = this.outlineColor;
		//Pen.strokeRect(myrect);
		////Pen.stroke;
	//}
	
	draw {
		var myrect;
		var pos;
		//Pen.color = this.color;
		pos = this.origin;
		myrect = parent.gridRectToPixelRect(this.rect);
		////[spritenum, rect, this.class].debug("draw");
		//Pen.fillRect(myrect);
		//Pen.color = this.outlineColor;
		//Pen.strokeRect(myrect);
		////Pen.stroke;
		Pen.addRect(myrect.insetBy(2));
		Pen.width = 4;
		Pen.color = this.outlineColor;
		Pen.stroke;

		Pen.addRect(myrect.insetBy(2));
		Pen.width = 3;
		Pen.color = this.color;
		Pen.stroke;

		Pen.width = 1;
		Pen.addRect(myrect.insetBy(2));
		if(selected) {
			Pen.color = this.colorSelected;
		} {
			Pen.color = this.color;
		};
		Pen.fill;

	}

	selectNode {
		//[this, this.identityHash, this.model].debug("select node");
		selected = true;
		this.refloc = this.nodeloc;
		//outlineColor = this.colorSelected;
	}

	deselectNode {
		//[this, this.identityHash, this.model].debug("deselect node");
		selected = true;
		selected = false;
		//outlineColor = this.colorDeselected;
	}

	free {
		if(controller.notNil) {controller.remove};
		if(preview.notNil) { preview.free };
	}

	decorateCopy {} // to set name when copied
}

//// children

TimelineViewEventListNode : TimelineViewEventNode {
	// is used for representing Ndef, Pdef, TrackDef and timelines as a node in a bigger timeline
	// that is player and pattern event types
	// if timeline, can display content of timeline
	var <>label;
	var <>labelheight = 20;
	//*new { arg parent, nodeidx, event;
	//	^super.new.init(parent, nodeidx, event);
	//}

	timelinePreviewClass {
		^TimelinePreview
	}

	initPreview {
		//Log(\Param).debug("TimelineViewEventListNode: initPreview %", model);
		if(this.enablePreview == true) {
			if(preview.isNil) {
				preview = this.timelinePreviewClass.new;
				preview.areasize.x = parent.areasize.x;
				preview.parentTimeline = parent;
				//model.eventlist.debug("eventlist");
				//model.timeline.debug("timeline");
				if(model[\eventlist].notNil) {
					//Log(\Param).debug("mapEventList");
					preview.mapEventList(model.eventlist);
				};

				if(model.timeline.notNil) {
					//Log(\Param).debug("mapModel");
					preview.mapModel(model)
				};
			}
		}

	}

	refreshPreview {
		if(preview.notNil) {
			preview.refresh;
		}
	}

	init { arg xparent, nodeidx, event;
		parent = xparent;
		spritenum = nodeidx;
		model = event;


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
		var preview_background = ParamViewToolBox.color_pale;
		var label_background = ParamViewToolBox.color_ligth;
		var font = Font.default.copy;
		if(~drawDebug.notNil) {
			~drawDebug.(this)
		} {


			if(parent.parentTimeline.notNil) {
				// we are drawing a node preview inside a node preview
				labelheight = parent.gridRectToPixelRect(this.rect).height;
				label_background = Color.white.lighten(ParamViewToolBox.color_pale, 0.2);
				font.size = 9;
				this.enablePreview = false;
			};

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

			if(selected) {
				Pen.color = this.colorSelected;
			} {
				Pen.color = label_background;
			};
			Pen.fillRect(rect);
			Pen.color = preview_background;
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
			Pen.stringLeftJustIn(" "++label, labelrect, font);

			// preview

			if(this.enablePreview) {
				this.initPreview;
				this.drawPreview(previewrect);
			}
			//Pen.stroke;

		};
	}

	//drawPreview_orig { arg previewrect;
		//var virtualBounds_rect;
		//var visiblebounds;
		//// FIXME: we use wide virtualBounds_rect as a workaround for a size bug to be found
		//virtualBounds_rect = Rect(previewrect.leftTop.x, previewrect.leftTop.y, this.parent.virtualBounds.width, previewrect.height);
		//virtualBounds_rect = previewrect;
		////virtualBounds_rect = previewrect;

		////preview.virtualBounds = virtualBounds_rect;
		//preview.virtualBounds = parent.virtualBounds.sect(virtualBounds_rect);
		////preview.areasize = preview.areasize.x_(this.parent.areasize.x);
		//preview.areasize = preview.areasize.x_(this.extent.x); // areasize should be clip width in beats, so extent.x
		////preview.viewport.width_(this.parent.viewport.width);
		//preview.viewport.width = parent.virtualBounds.sect(previewrect).width / previewrect.width;
		//preview.viewport.left = parent.virtualBounds.sect(previewrect).left - previewrect.left / previewrect.width;
		//preview.viewport.height = parent.virtualBounds.sect(previewrect).height / previewrect.height;
		//preview.viewport.top = (previewrect.bottom - visiblebounds.bottom) / previewrect.height;
		////preview.viewport.bottom = parent.virtualBounds.sect(previewrect).bottom - previewrect.bottom / previewrect.height;
		////preview.viewport.width_(1); // debug
		////preview.viewport.origin = this.parent.viewport.origin;
		//preview.viewport = preview.viewport; // trigger update

		//Pen.use {
			//Pen.addRect(previewrect);
			//Pen.clip;
			//Log(\Param).debug("preview");
			//preview.drawFunc;
		//};
		
	//}

	drawPreview { arg previewrect;
		var visiblebounds;
		var cutOffset;
		var cutNormOffset;

		// preview cut offset from event_dropdur and offset from timeine \start event
		cutOffset = (this.startOffset ? 0) + ( preview.model !? _.startTime ? 0);

		visiblebounds = parent.virtualBounds.sect(previewrect);
		preview.virtualBounds = visiblebounds;


		preview.areasize = preview.areasize.x_(this.extent.x);
		preview.viewport.width = visiblebounds.width / previewrect.width;
		preview.viewport.left = visiblebounds.left - previewrect.left / previewrect.width;
		preview.viewport.height = visiblebounds.height / previewrect.height;
		preview.viewport.top = (previewrect.bottom - visiblebounds.bottom) / previewrect.height;

		cutNormOffset = cutOffset / preview.areasize.x;
		preview.viewport.left = preview.viewport.left + cutNormOffset;

		preview.viewport = preview.viewport; // trigger update

		Pen.use {
			//Pen.addRect(previewrect);
			Pen.addRect(visiblebounds);
			Pen.clip;
			//Log(\Param).debug("preview");
			preview.drawFunc;
		};
		
	}

	enablePreview {
		^enablePreview && this.parent.enablePreview;
	}

	makeUpdater_old {
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

TimelineViewEventEnvNode : TimelineViewEventListNode { // for ParamTimeline clip
	timelinePreviewClass {
		^TimelinePreview_Env
	}
}

TimelineViewEventSampleNode : TimelineViewEventListNode { // for SampleTimeline clip
	// NOTE: drawPreview is now factored inside TimelineViewEventListNode
	//drawPreview { arg previewrect;
		//// NOTE: had to fork drawPreview from TimelineViewEventListNode because need to find how to factor them
		//var virtualBounds_rect;
		//var visiblebounds;
		//// FIXME: we use wide virtualBounds_rect as a workaround for a size bug to be found
		////virtualBounds_rect = Rect(previewrect.leftTop.x, previewrect.leftTop.y, this.parent.virtualBounds.width, previewrect.height);
		////virtualBounds_rect = previewrect;
		////virtualBounds_rect = previewrect;

		////preview.virtualBounds = virtualBounds_rect;
		//visiblebounds = parent.virtualBounds.sect(previewrect);
		//preview.virtualBounds = visiblebounds;

		//// using visible bounds (intersection of parent bounds and previewrect) as PreviewTimeline.bounds work well with NoteTimeline, but not with SampleTimeline because in SampleTimeline.drawImageWaveform i only draw inside the visible bounds but in NoteTimeline i draw all the clip preview
		////preview.virtualBounds = parent.virtualBounds.sect(virtualBounds_rect);


		////preview.areasize = preview.areasize.x_(this.parent.areasize.x);
		//preview.areasize = preview.areasize.x_(this.extent.x);
		//preview.viewport.width_(this.parent.viewport.width);
		//preview.viewport.width = visiblebounds.width / previewrect.width;
		//preview.viewport.left = visiblebounds.left - previewrect.left / previewrect.width;
		//preview.viewport.height = visiblebounds.height / previewrect.height;
		//preview.viewport.top = (previewrect.bottom - visiblebounds.bottom) / previewrect.height;
		////preview.viewport.width_(1); // debug
		////preview.viewport.origin = this.parent.viewport.origin;
		//preview.viewport = preview.viewport; // trigger update

		//Pen.use {
			//Pen.addRect(previewrect);
			//Pen.clip;
			//Log(\Param).debug("preview");
			//preview.drawFunc;
		//};
		
	//}

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


