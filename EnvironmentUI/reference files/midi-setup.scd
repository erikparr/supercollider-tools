 ~knob1Val = 0;
~knob2Val = 0;
~knob3Val = 0;
~knob4Val = 0;
~knob5Val = 0;
~knob6Val = 0;
~knob7Val = 0;
~knob8Val = 0;
~knob1Range = 0;
~knob2Range = 0;
~knob3Range = 0;
~knob4Range = 0;
~knob5Range = 0;
~knob6Range = 0;
~knob7Range = 0;
~knob8Range = 0;


~f1 = MIDIFunc.noteOn({arg veloc, pitch, chan;
	chan = 0;
	~bendSynth1.set(\gate, 1, \start, 0, \end, 0, \dur,0.0001);
	// play glissando note mode
	if (~glissandoNoteMode) {
		SystemClock.sched(~knob2Val, {
			// ~bendSynth1.set(\gate, 1, \start, 8192, \end, 16383, \dur,~knob1Val);
			~bendSynth1.set(\gate, 1, \start, 0, \end, 8192, \dur,~knob1Val);
		});
	};
	// play midi note and send osc to ethersonic, if glissando mode don't play note just glissssss
	if(~glissandoMode == true, {
		n.sendMsg('/glissOn', chan, pitch);
	},{
			~vstList.do { |item, i| item.midi.noteOn(chan, pitch, ~veloc); };
			// ~testfx.midi.noteOn(chan, pitch, veloc);
		n.sendMsg('/keyOn', chan, pitch);
	});
	~numNotesPlaying = ~numNotesPlaying + 1;
	pitch.postln;
});
// ~vstList.at('vsti').midi.noteOn(0,55,100);
// ~f1.free;
~f2 = MIDIFunc.noteOff({arg veloc, pitch, chan;
	var freq = pitch.midicps; // Convert MIDI note number to frequency
	chan =0;
	~vstList.do { |item, i| item.midi.noteOff(chan, pitch, veloc); };
	n.sendMsg('/keyOff', chan, pitch);
	// ~numNotesPlaying = ~numNotesPlaying - 1;
	~numNotesPlaying = 0;
});

~f3 = MIDIFunc.cc({arg ...args;
	// n.sendMsg('/transpose', 0);
	args.postln;
}, 49); // match cc 49

~f4 = MIDIFunc.bend({ arg bendval, channel;
	channel =0;
	bendval.postln;
	~vstList.do { |item, i| item.midi.bend(channel, bendval); };
});

~knob1 = MIDIFunc.cc({|val, ccNum, chan, src|
	n.sendMsg('/knob1', val);
	~knob1Range = val.linlin(0, 127, 0.0, 1.0);
	~knob1Val = val;
	~knob1Val.postln;
}, 16); // match cc 16

~knob2 = MIDIFunc.cc({|val, ccNum, chan, src|
	n.sendMsg('/knob2', val);
	~knob2Range = val.linlin(0, 127, 0.0, 1.0);
	~knob2Val = val;
}, 17); // match cc 17

~knob3 = MIDIFunc.cc({|val, ccNum, chan, src|
	n.sendMsg('/knob3', val);
	~knob3Range = val.linlin(0, 127, 0.0, 1.0);
	~knob3Val = val;
}, 18); // match cc 18
~knob4 = MIDIFunc.cc({|val, ccNum, chan, src|
	~knob4Range = val.linlin(0, 127, 0.0, 1.0);
	~knob4Val = val;
	n.sendMsg('/knob4', val);

}, 19); // match cc 19
~knob5 = MIDIFunc.cc({|val, ccNum, chan, src|
	~knob5Range = val.linlin(0, 127, 0.0, 1.0);
	~knob5Val = val;
	n.sendMsg('/knob5', val);

}, 20); // match cc 20
~knob6 = MIDIFunc.cc({|val, ccNum, chan, src|
	~knob6Range = val.linlin(0, 127, 0.0, 1.0);
	~knob6Val = val;
	n.sendMsg('/knob6', val);

}, 21); // match cc 21
~knob7 = MIDIFunc.cc({|val, ccNum, chan, src|
	~knob7Range = val.linlin(0, 127, 0.0, 1.0);
	~knob7Val = val;
	n.sendMsg('/knob7', val);
~veloc = ~knob7Val
}, 22); // match cc 22

~bt1 = MIDIFunc.cc({|val, ccNum, chan, src|
	~vstList.do { |item, i|
		5.do{arg i;
			item.midi.allNotesOff(i);
		};
	};
}, 48); // match cc 23