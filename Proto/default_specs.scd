
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
