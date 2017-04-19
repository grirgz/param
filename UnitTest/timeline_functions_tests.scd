
(
ParamProto.init(true);

SynthDef(\babass, { arg out=0, amp=0.1, gate=1, pan=0, freq=200;
	var sig;
	sig = 0;
	sig = sig + SinOsc.ar(freq * [2,1,1/2]);
	sig = sig * EnvGen.ar(Env.adsr(0.01,0.1,0.8,0.1),gate,doneAction:2);
	sig = Pan2.ar(sig, pan, amp).flop;
	Out.ar(out, sig);
}).add;

Pdef(\default, Pbind(
	\instrument, \babass,
	\degree, Pseq([0],inf),
	\dur, 1,
	\amp, Pkeyd(\velocity, 64)/127 * 2 * 0.1
));

NoteTimeline(\bli).clear;
NoteTimeline(\bli, Param(Pdef(\default), \midinote));
);
NoteTimeline(\bli).edit


///////////////// algo for moving nodes
(
// ----------new algo
// - pixel_click_offset = determine diff in pixel from clicked point to origin point of chosennode (chosennode_old_origin)
// - the new location of node is the point where the mouse has moved (newpos) minus the pixel_click_offset
// - then convert it to grid unit and quantize it
// - now determine the diff between old node location and new node location (grid_diff) and apply this change to all selected nodes
// - since the function is called continously, chosennode_old_origin should be fixed to the first position of node (refloc), and not his
//		position changing continuously
~move_node = {
	var pixel_newpos_point, pixel_clicked_point, chosennode_old_origin, pixel_click_offset, grid_diff, chosennode_new_origin;
	var xthis = NoteTimeline(\bli).editor.timeline;
	chosennode_old_origin = xthis.pixelPointToGridPoint(Point(10,10));
	pixel_clicked_point = Point(15,15);
	pixel_newpos_point = Point(50,50);
	pixel_click_offset = pixel_clicked_point - xthis.gridPointToPixelPoint(chosennode_old_origin);
	chosennode_new_origin = xthis.pixelPointToGridPoint(pixel_newpos_point - pixel_click_offset);

	[
		chosennode_old_origin: chosennode_old_origin,
		chosennode_pixel_old_origin: xthis.gridPointToPixelPoint(chosennode_old_origin),
		pixel_clicked_point: pixel_clicked_point,
		pixel_newpos_point: pixel_newpos_point,
		pixel_click_offset: pixel_click_offset,
		pixel_newpos_point_x_pixel_diff: pixel_newpos_point - pixel_click_offset,
		chosennode_new_origin_raw: xthis.pixelPointToGridPoint(pixel_newpos_point),
		chosennode_new_origin: xthis.pixelPointToGridPoint(pixel_newpos_point - pixel_click_offset),
	].debug("debug move data2");
};
~move_node.();
)

the bug is the chosennode_old_origin change continously when moving, but pixel_clicked_point never change
