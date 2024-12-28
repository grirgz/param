
// standard specs
Spec.add(\out, ParamBusSpec.new);
Spec.add(\inbus, ParamBusSpec.new);
Spec.add(\in, ParamAudioSpec.new);
Spec.add(\gate, ParamGateSpec.new);
Spec.add(\doneAction, ParamDoneActionSpec.new);
Spec.add(\bufnum, ParamBufferSpec(2));
Spec.add(\mbufnum, ParamBufferSpec(1));
Spec.add(\adsr, ParamEnvSpec.adsr);
Spec.add(\trig, ParamTrigSpec.new);

// 
Spec.add(\mix, \unipolar.asSpec);
Spec.add(\wet, \unipolar.asSpec);
Spec.add(\gain, \unipolar.asSpec);

// Event default specs

Spec.add(\legato, ControlSpec(0.05,2,\exp,0,0.8));
Spec.add(\dur, ControlSpec(1/32,10,\lin,0,1));
Spec.add(\sustain, ControlSpec(0.05,4,\exp));
Spec.add(\degree, ControlSpec(0,14,\lin,1));
Spec.add(\mtranspose, ControlSpec(0,14,\lin,1));
Spec.add(\root, ControlSpec(0,14,\lin,1));
Spec.add(\note, ControlSpec(0,24,\lin,1));
Spec.add(\ctranspose, ControlSpec(0,24,\lin,1));
Spec.add(\midinote, ControlSpec(1,126,\lin,1,60));
Spec.add(\octave, ControlSpec(1,7,\lin,1,4));
Spec.add(\scale, TagSpecDef(\scale, { Scale.all.parent.asAssociations } ));
