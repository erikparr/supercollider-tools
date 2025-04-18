// Define a function that initializes the SuperCollider server
Server.killAll;
Server.default = Server.local;
s.options.sampleRate = 48000;
s = Server.default;

// Set the SuperCollider server output device
ServerOptions.devices; // all devices
s.options.numInputBusChannels = 0;
//Server.default.options.inDevice_("SC Route");
//enable when routing to loopback:
Server.default.options.outDevice_("Babyface Pro");

//It is a good idea to go ahead and clear the plugin cache, in case some quarantined plugins have been black-listed already.
// VSTPlugin.clear

// Wait for the server to finish booting
s.waitForBoot {

	// Initialize the MIDI client and connect to all MIDI inputs
	MIDIClient.init;
	MIDIIn.connectAll;
	MIDIClient.destinations.postln;

	// record from supercollider (not working)
	// ~midiOut = MIDIOut.newByName("IAC Driver", "CustomMIDIReaperCTRL");
	// ~midiOut = MIDIOut.newByName("V61", "Out");
	//
	// ~midiRecordReaper = {
	// ~midiOut.control(0, 63, 127);
	// };
	// ~midiRecordReaper.value;

	// Define your synth
	SynthDef(\insert, { arg bus ;
		ReplaceOut.ar(bus, VSTPlugin.ar(In.ar(bus, 2, 2), id: \vsti));
	}).add;

	// synthdef for creating two synths in parallel
	SynthDef(\insert2, { arg bus1, bus2;
		var sig1, sig2;
		sig1 = In.ar(bus1, 2);
		sig2 = In.ar(bus2, 2);
		sig1 = VSTPlugin.ar(sig1, 2, id: \vsti);
		sig2 = VSTPlugin.ar(sig2, 2, id: \vsti1);

		ReplaceOut.ar(bus1, Mix.new([sig1, sig2]));
	}).add;

	SynthDef(\insert3, { arg bus;
		var sig;
		sig = In.ar(bus, 2);
		sig = VSTPlugin.ar(sig, 2, id: \vsti);
		sig = VSTPlugin.ar(sig, 2, id: \vstiRoute);
		ReplaceOut.ar(bus, sig);
	}).add;

	SynthDef(\insert4, { arg bus1, bus2;
		var sig1, sig2;
		sig1 = In.ar(bus1, 2);
		sig2 = In.ar(bus2, 2);
		sig1 = VSTPlugin.ar(sig1, 2, id: \vsti);
		sig2 = VSTPlugin.ar(sig2, 2, id: \vsti1);
		sig2 = VSTPlugin.ar( Mix.new([sig1, sig2]), 2, id: \vstiRoute);
		ReplaceOut.ar(bus1, sig2);
	}).add;



	VSTPlugin.search(options: ( exclude: [ "/Library/Audio/Plug-Ins/VST3/Komplete Kontrol.vst3" ]));
	2.5.wait;

	// ~synth = Synth(\insert3, [\bus, 0]);
	// ~testfx = VSTPluginController(~synth, \vsti).open("/Library/Audio/Plug-Ins/VST3/Omnisphere.vst3", editor: true);
	// ~routefx = VSTPluginController(~synth, \vstiRoute).open("/Library/Audio/Plug-Ins/VST3/Pigments.vst3");
	// 1.wait;
	// ~testfx.editor;
	// ~routefx.editor;
	// automate with Dictionary

	// SWAM TO PIGMENTS ROUTING
	~vstList = Dictionary.new;
	~synth = Synth(\insert4, [\bus1, 0, \bus2, 1]);
	~cello1 = VSTPluginController(~synth, \vsti).open("/Library/Audio/Plug-Ins/VST3/SWAM Cello 3.vst3", editor: true);
	~cello2 = VSTPluginController(~synth, \vsti1).open("/Library/Audio/Plug-Ins/VST3/SWAM Cello 3.vst3", editor: true);
	~pigments = VSTPluginController(~synth, \vstiRoute).open("/Library/Audio/Plug-Ins/VST3/Pigments.vst3");
	~vstList.add(\vsti -> ~cello1);
	~vstList.add(\vsti1 -> ~cello2);
	~vstList.add(\vstiRoute ->  ~pigments);

	// KONTAKT TO PIGMENTS ROUTING
	// ~vstList = Dictionary.new;
	// ~synth = Synth(\insert3, [\bus, 0]);
	// ~vstList.add(\vsti -> VSTPluginController(~synth, \vsti).open("/Library/Audio/Plug-Ins/VST3/Komplete Kontrol.vst3", editor: true));
	// ~vstList.add(\vstiRoute ->  VSTPluginController(~synth, \vstiRoute).open("/Library/Audio/Plug-Ins/VST3/Pigments.vst3"));
	//
	// OMNISPHERE TO PIGMENTS ROUTING
	// ~vstList = Dictionary.new;
	// ~synth = Synth(\insert3, [\bus, 0]);
	// ~vstList.add(\vsti -> VSTPluginController(~synth, \vsti).open("/Library/Audio/Plug-Ins/VST3/Omnisphere.vst3", editor: true));
	// ~vstList.add(\vstiRoute ->  VSTPluginController(~synth, \vstiRoute).open("/Library/Audio/Plug-Ins/VST3/Pigments.vst3"));



	// Set the current program number for the VST plugin controller
	// ~vsti = VSTPluginController(Synth(\insert, [\bus, 0])).open("/Library/Audio/Plug-Ins/VST3/Choreographs.vst3", editor: true);
	// ~vsti = VSTPluginController(Synth(\insert, [\bus, 0])).open("Kontakt", editor: true);
	// ~vsti = VSTPluginController(Synth(\insert, [\bus, 0])).open("/Library/Audio/Plug-Ins/VST3/Komplete Kontrol.vst3", editor: true);
	// ~vsti = VSTPluginController(Synth(\insert, [\bus, 0])).open("/Library/Audio/Plug-Ins/VST3/Kontakt.vst3", editor: true);
	//	~synth = Synth(\insert3, [\bus, 0]);
	// ~synth = Synth(\insert, [\bus1, 0, \bus2, 1]);


	// ~vsti = VSTPluginController(Synth(\insert, [\bus, 0])).open("/Library/Audio/Plug-Ins/VST3/Pigments.vst3", editor: true);
	// ~vsti = VSTPluginController(Synth(\insert, [\bus, 0])).open("/Library/Audio/Plug-Ins/VST3/Analog Lab V.vst3", editor: true);
	// ~vsti = VSTPluginController(Synth(\insert, [\bus, 0])).open("Omnisphere", editor: true);
	//
	// //envelope vst control
	~bend1 = Bus.control;
	SynthDef(\BendEnvelope1, {
		arg start = 0, end = 1, dur = 1, chanIndex = 0;
		var env;
		env = EnvGen.kr(
			Env([start, end], [dur], \sin),
			gate: \gate.tr,
			doneAction: 2
		);
		SendTrig.kr(Impulse.kr(64), id:chanIndex, value:env);  // Send triggers at a control rate of 64 Hz
		// Out.kr(~bend1, env);
	}).add;

	SynthDef(\BendEnvelopeLoop, {
		arg start = 0, end = 1, dur = 1, chanIndex = 0;
		var env;
		env = EnvGen.kr(
			Env( [start, start, end, end, start], [dur*0.4, dur*0.1, dur*0.4, dur*0.1], \sin),
			gate: Trig.kr(Impulse.kr(1), dur),
			doneAction: 0
		);
		SendTrig.kr(Impulse.kr(64), id:chanIndex, value:env);  // Send triggers at a control rate of 64 Hz
		// Out.kr(~bend1, env);
	}).add;

	5.wait;
	// Open the editor window for the VST plugin controller
	~vstList.do { |item, i| item.editor; };
	n = NetAddr("127.0.0.1", 57121); // local machine

	// Replace "example.scd" with the path to your .scd file
	"/Users/erikparr/Documents/_Prototypes/ethersonic-electron/ethersonic/sc/ethersonic-interface.scd".load;
	"/Users/erikparr/Documents/_Prototypes/ethersonic-electron/ethersonic/sc/SWAM-ctrl.scd".load;

	//
	// ~f1 = MIDIFunc.noteOn({arg veloc, pitch, chan;
	// 	chan = 0;
	// 	~vsti.midi.noteOn(chan, pitch, veloc);
	// 	pitch.postln;
	// 	pitch.midinote.postln;
	// 	n.sendMsg('/keyOn', chan, pitch);
	// 	// ~vsti2.midi.noteOn(chan, pitch, veloc);
	// });
	// ~f2 = MIDIFunc.noteOff({arg veloc, pitch, chan;
	// 	var freq = pitch.midicps; // Convert MIDI note number to frequency
	// 	chan =0;
	// 	~vsti.midi.noteOff(chan, pitch, veloc);
	// 	n.sendMsg('/keyOff', chan, pitch);
	// });
	// ~f4 = MIDIFunc.bend({ arg bendval, channel;
	// 	channel =0;
	// 	~vsti.midi.bend(channel, bendval);
	// });

	// Load SynthDef and sample files here
	// loadSynthDef("/path/to/synthDef.scsyndef");
	// loadSample("/path/to/sample.wav");

	// Print a message to indicate that the startup file has finished loading
	"Startup file loaded".postln;
};
