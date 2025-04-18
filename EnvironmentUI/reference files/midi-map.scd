
// send control message

~ctrl1 = Bus.control;
~ctrl2 = Bus.control;
~ctrl3 = Bus.control;
~lfo = {
	var lfoSignal = SinOsc.kr(0.05, 0, 0.5, 0.5); // expression control
	var lfoSignal2 = SinOsc.kr(0.1, 0, 0.5, 0.5);
	var lfoSignal3 = SinOsc.kr(0.0175, 0, 0.5, 0.5);
	var mappedSignal = lfoSignal.linlin(0, 1.0, 45,  70); // 0-127
	var mappedSignal2 = lfoSignal2.linlin(0, 1.0, 0, 127/2);
	var mappedSignal3 = lfoSignal3.linlin(0, 1.0, 18, 64);
	Out.kr(~ctrl1, mappedSignal);
	Out.kr(~ctrl2, mappedSignal2);
	Out.kr(~ctrl3, mappedSignal3);
}.play;
~pollRoutine = Routine({
	inf.do {
		var value = ~ctrl1.getSynchronous; // Read the current value from the control bus
		var value2 = ~ctrl2.getSynchronous; // Read the current value from the control bus
		var value3 = ~ctrl3.getSynchronous; // Read the current value from the control bus
		if(~vstList.at(\vsti) != nil){
			~vstList.at(\vsti).midi.control(0, 18, ~knob1Val); // touch range = 0-127
			~vstList.at(\vsti).midi.control(0, 19, ~knob2Val); // touch range = 0-127
			~vstList.at(\vsti).midi.control(0, 16, ~knob3Val); // touch range = 0-127
		};
		0.02.wait; // Wait for 20ms (50Hz polling rate)
	}
}).play;

/*
~pollRoutine.stop
~lfo.free
*/