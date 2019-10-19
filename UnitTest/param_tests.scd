
///////// test instrument


// EventPatternProxy
(
	~test = UnitTest.new;
	~target = EventPatternProxy.new.source_(Pbind());
	~test.assertEquals(~target.get(\instrument), nil, "instr is nil when envir is nil");
	~test.assertEquals(Param(~target, \freq).instrument, \default, "param return default");
	~target.set(\freq, 300);
	~test.assertEquals(~target.get(\instrument), \default, "instr is default when envir is set");
	~test.assertEquals(Param(~target, \freq).instrument, \default, "param return default");

	~target = EventPatternProxy.new.source_(Pbind());
	~target.addHalo(\instrument, \bla);
	~test.assertEquals(Param(~target, \freq).instrument, \bla, "param return bla when envir is nil");
	~target.set(\freq, 300);
	~test.assertEquals(Param(~target, \freq).instrument, \bla, "param return bla when envir is set");
)
