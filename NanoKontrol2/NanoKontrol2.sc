// Save this as NanoKontrol2.scd in your SuperCollider extensions directory

NanoKontrol2 : Object {
    var <>midiIn, <>midiOut, <>midiCCMap, <>paramMap, <>remapFunc, <>verbose;

    *new {
        |midiDeviceName = "nanoKONTROL2"|
        ^super.new.init(midiDeviceName);
    }

    init {
        |midiDeviceName|
        midiIn = MIDIIn.find(midiDeviceName);
        midiOut = MIDIOut.find(midiDeviceName);
        midiCCMap = ();
        paramMap = ();
        remapFunc = { |val, min, max| val.linlin(0, 127, min, max) };
        verbose = false;
        this.mapControls;
    }

    mapControls {
        // Map of NanoKONTROL2 controls to MIDI CC channels
        midiCCMap = (
            slider1: 0, slider2: 1, slider3: 2, slider4: 3,
            slider5: 4, slider6: 5, slider7: 6, slider8: 7,
            knob1: 16, knob2: 17, knob3: 18, knob4: 19,
            knob5: 20, knob6: 21, knob7: 22, knob8: 23,
            sButton1: 32, sButton2: 33, sButton3: 34, sButton4: 35,
            sButton5: 36, sButton6: 37, sButton7: 38, sButton8: 39,
            mButton1: 48, mButton2: 49, mButton3: 50, mButton4: 51,
            mButton5: 52, mButton6: 53, mButton7: 54, mButton8: 55,
            rButton1: 64, rButton2: 65, rButton3: 66, rButton4: 67,
            rButton5: 68, rButton6: 69, rButton7: 70, rButton8: 71
        );
    }

    getMIDIValue {
        |control|
        ^midiCCMap[control];
    }

    setParam {
        |control, param, min = 0, max = 1|
        var cc = this.getMIDIValue(control);
        paramMap[cc] = (param: param, min: min, max: max);
        if (verbose) { ("Mapped " ++ control ++ " (CC: " ++ cc ++ ") to param: " ++ param ++ " with range: [" ++ min ++ ", " ++ max ++ "]").postln; }
    }

    unsetParam {
        |control|
        var cc = this.getMIDIValue(control);
        paramMap.removeAt(cc);
        if (verbose) { ("Unmapped " ++ control ++ " (CC: " ++ cc ++ ")").postln; }
    }

    setRemapFunc {
        |func|
        remapFunc = func;
    }

    remapValues {
        |val, min, max|
        ^remapFunc.value(val, min, max);
    }

    listen {
        midiIn.connect;
        MIDIdef.cc(\nk2Listener, {
            |val, ccNum|
            var mapping, param, remappedVal;
            mapping = paramMap[ccNum];
            if (mapping.notNil) {
                param = mapping[\param];
                remappedVal = this.remapValues(val, mapping[\min], mapping[\max]);
                param.value = remappedVal;
                if (verbose) { ("CC: " ++ ccNum ++ ", Value: " ++ val ++ ", Remapped to: " ++ remappedVal).postln; }
            }
        }, nil, nil, midiIn);
    }

    showMIDICCMap {
        midiCCMap.keysValuesDo { |key, val|
            ("Control: " ++ key ++ ", MIDI CC: " ++ val).postln;
        }
    }

    saveMappings {
        |path|
        File.write(path, paramMap.asCompileString);
        if (verbose) { ("Mappings saved to " ++ path).postln; }
    }

    loadMappings {
        |path|
        var file = File(path, "r");
        if (file.isOpen) {
            paramMap = file.readAll.asString.interpret;
            file.close;
            if (verbose) { ("Mappings loaded from " ++ path).postln; }
        } {
            "Error: Could not open file.".postln;
        }
    }

    toggleVerbose {
        verbose = verbose.not;
        ("Verbose mode: " ++ verbose).postln;
    }

    // Additional useful features can be added here
}
