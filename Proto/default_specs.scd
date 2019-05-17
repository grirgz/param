
// standard specs
Spec.add(\out, XBusSpec.new);
Spec.add(\gate, XGateSpec.new);
Spec.add(\doneAction, XDoneActionSpec.new);
Spec.add(\bufnum, XBufferSpec(2));
Spec.add(\mbufnum, XBufferSpec(1));
Spec.add(\adsr, XEnvSpec.adsr);
