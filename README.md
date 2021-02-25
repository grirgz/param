# Param

Param is a framework that allow you to easily develop your custom graphical interface and connect your controllers to it.

In an environment like Supercollider, you don't want a monolithic DAW that force you to work like the developpers have decided. Your interface should evolve with your workflow. But you don't want take hours to manage details of GUI building. The solution is a framework offering basic building blocks and managing internal circuitery for you.

Disclaimer: this is a work in progress, some parts are broken, some parts are unfinished, some API can change. Help is welcome

## Design your sound
- use the standard SynthDef, Pdef and Ndef
- BusDef: never worry about allocating or freeing your bus, just name them
- BufDef: never load two times the same sample, just type the filename in the path
- WavetableDef: load wavetable from files
- GroupDef: name groups

## Build a GUI:
- WindowDef: group and reuse your GUI code and remember window position and size
- Param: build automatically GUI for your SynthDefs, control any synth parameter with GUI or controller, support for arrays and enveloppes
- PlayerWrapper: a nice wrapper to control any player (Pdef, Timeline, ...) with a standard interface
- Timelines: NoteTimelines for notes, KitTimeline for drums, SampleTimeline for Buffers, ParamTimeline for automations, ClipTimeline to sequence any player
- Builder: a special function that is executed each time an argument change value, you can control it with GUI/controllers

## Use your controllers:
- ControllerDef: group your controller code and define standard interfaces to instantly control your objects with your controller
- MIDIMap: a quick way to assign your often used midi controller buttons
- PlayerGroup: control any Pdef with your piano controller and record it in a timeline
- integration with Modality

## DAW tools that can be saved as a project or used independently
- Main window: with main level view, tempo, quant and lot of useful buttons
- Mixer: with level view and send busses
- Fx manger: setup and control reverb, eq, compressors and any other fx SynthDef in a GUI
- Drum Kit Sampler: create drum kit from code or GUI, play and record it in timelines
- PlayerGrid: a grid of player which can be connected to launchpad or other controllers
- Tag system: every object can be tagged and retrieved in the tag explorer
- Project system to group everything and save it in a project folder

## Design goals:
- Modular: you can use any tool from the framework without loading the others, GUI is optional, you can replace any part
- Integrated with JITLib and pattern system: you can mix code and GUI/control/record as you please
- Saving system for everything that is not in code file: a value set with your controller is lost at reboot, save it easily, the format of save files is human readable code so you can recover it even if the savefile is corrupt

## Dependencies

Quarks
- JITLibExtensions: getHalo/addHalo
- WindowViewRecall
	- Collapse: dependency of WindowRecallView
- Log
	- Singleton: dependency of Log
	
Optional quarks
- Modality-toolkit
- vim patch loadRelative


## Configuration

For you projects to be independent of location on your disk, you can add paths
So instead of writing 
```BufDef("~/mysamples/kicks/kick.wav")```
you can write
```BufDef("kicks/kick.wav")```

If you move your samples elsewhere, no need to update every code file, just change the path in your startup.scd

Place this code in your startup.scd
```
// paths for loading files and projects
FileSystemProject.addPath("~/code/sc/projects/"); // change by your own paths
FileSystemProject.addPath("~/drafts/");
// paths for loading buffers and wavetables
BufDef.addPath("~/Musique/samples/");
WavetableDef.addPath("~/Musique/wavetables/");
FileSystemProject.recordFolder = "~/Musique/records/";
```

Disable annoying debug messages:
```
Log(\Param).level = \info;
```

## Troubleshooting


```
    ERROR: Class extension for nonexistent class ‘MKtlElement’

    ERROR: Class extension for nonexistent class ‘MKtlElementGroup’
```

Param quark add a few methods to Modality-toolkit quark, but they are optional, you can ignore theses errors
