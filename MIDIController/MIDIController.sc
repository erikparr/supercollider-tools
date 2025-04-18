MIDIController {
    var <sliderValues;
    var <knobValues;
    var <knobRanges;
    var <midiFuncs;
    var <vstList;
    var <oscNetAddr;
    var <glissandoMode;
    var <glissandoNoteMode;
    var <bendSynth;
    var <numNotesPlaying;
    var <velocity;
    var <numKnobs;
    var <startCC;
    var <controlRoutine;
    var <pollRate;
    var <debug;
    var <multiChannelMode;
    var <multiInstrumentMode;
    var <velocityCtrlMode;
    var <>activeNotes;
    var <noteHandlingEnabled;
    
    // Bend-related properties
    var <bendEnabled;
    var <bendRange;
    var <bendDuration;
    var <bendCurve;
    var <bendEnvelopeSynth;
    
    // Snapshot-related properties
    var <snapshots;
    var <currentSnapshot;
    var <programmedMode;
    var <snapshotDataPath;

    *new { |vstList, oscNetAddr, bendSynth = nil, numKnobs = 16, startCC = 0, debug = false|
        ^super.new.init(vstList, oscNetAddr, bendSynth, numKnobs, startCC, debug);
    }

    init { |inVstList, inOscNetAddr, inBendSynth, inNumKnobs, inStartCC, inDebug|
        debug = inDebug;
        
        this.debug("Initializing MIDIController");
        
        // Initialize MIDI client if not already initialized
        MIDIClient.initialized.not.if {
            MIDIClient.init;
        };
        
        // Connect to all available MIDI sources and destinations
        MIDIIn.connectAll;
        MIDIClient.destinations.do { |dest|
            this.debug("Connected to MIDI destination: %".format(dest.name));
        };
        
        vstList = inVstList;
        oscNetAddr = inOscNetAddr;
        bendSynth = inBendSynth;
        numKnobs = inNumKnobs;
        startCC = inStartCC;
        
        // Initialize arrays for both sliders and knobs
        sliderValues = Array.fill(8, 0);
        knobValues = Array.fill(8, 0);
        knobRanges = Array.fill(8, 0);
        midiFuncs = IdentityDictionary.new;
        numNotesPlaying = 0;
        velocity = 100;
        
        // Initialize modes as false by default
        glissandoMode = false;
        glissandoNoteMode = false;
        multiChannelMode = true;
        multiInstrumentMode = false;
        velocityCtrlMode = false;
        activeNotes = Dictionary.new;
        noteHandlingEnabled = true;
        
        // Initialize bend-related properties
        bendEnabled = false;
        bendRange = 6;  // half octave up
        bendDuration = 1.5;  // seconds
        bendCurve = \sin;  // smooth curve
        bendEnvelopeSynth = nil;
        
        // Initialize snapshot-related properties
        snapshots = Dictionary.new;
        currentSnapshot = nil;
        programmedMode = false;
        snapshotDataPath = this.class.getSnapshotDataPath;
        
        this.initMIDIFuncs;
    }

    // Class method to get the snapshot data path
    *getSnapshotDataPath {
        var snapshotDir, dir, projectPath;
        
        // Print diagnostic information
        "Current working directory: %".format(File.getcwd).postln;
        "User home directory: %".format(Platform.userHomeDir).postln;
        
        // Try to find the project directory by looking for the setup directory
        projectPath = Platform.userHomeDir ++ "/Documents/_Music/sc-projects/intro/first-light";
        snapshotDir = projectPath ++ "/snapshotData";
        
        "Using project path: %".format(projectPath).postln;
        "Using snapshot directory: %".format(snapshotDir).postln;
        
        dir = PathName(snapshotDir);
        
        if(dir.isFolder.not) {
            "Directory does not exist, attempting to create: %".format(snapshotDir).postln;
            try {
                File.mkdir(snapshotDir);
                "Directory created successfully".postln;
            } { |error|
                "Failed to create directory: %".format(error).postln;
                // If we can't create the directory, at least return the path
                "Returning path even though directory creation failed".postln;
            };
        } {
            "Directory already exists: %".format(snapshotDir).postln;
        };
        
        ^snapshotDir;
    }

    // Save current slider and knob values as a snapshot
    saveSnapshot { |name|
        var snapshot = Dictionary.new;
        var timestamp = Date.getDate.format("%Y-%m-%d %H:%M:%S");
        
        // Store slider values
        snapshot.put(\sliders, sliderValues.copy);
        
        // Store knob values
        snapshot.put(\knobs, knobValues.copy);
        
        // Store timestamp
        snapshot.put(\timestamp, timestamp);
        
        // Add to snapshots dictionary
        snapshots.put(name, snapshot);
        
        this.debug("Saved snapshot: %".format(name));
        ^snapshot;
    }

    // Load a snapshot
    loadSnapshot { |name|
        var snapshot = snapshots.at(name);
        
        if(snapshot.notNil) {
            // Restore slider values
            snapshot.at(\sliders).do { |val, i|
                sliderValues[i] = val;
            };
            
            // Restore knob values
            snapshot.at(\knobs).do { |val, i|
                knobValues[i] = val;
            };
            
            this.debug("Loaded snapshot: %".format(name));
            ^true;
        } {
            this.debug("Snapshot not found: %".format(name));
            ^false;
        }
    }

    // List all available snapshots
    listSnapshots {
        this.debug("Available snapshots:");
        snapshots.keys.do { |name|
            var snapshot = snapshots.at(name);
            var timestamp = snapshot.at(\timestamp);
            "  % (saved: %)".format(name, timestamp).postln;
        };
    }

    // Delete a snapshot
    deleteSnapshot { |name|
        if(snapshots.includesKey(name)) {
            snapshots.removeAt(name);
            this.debug("Deleted snapshot: %".format(name));
            ^true;
        } {
            this.debug("Snapshot not found: %".format(name));
            ^false;
        }
    }

    // Save snapshots to a file in the snapshotData directory
    saveSnapshotsToFile { |filename|
        var fullPath = snapshotDataPath ++ "/" ++ filename;
        var file;
        
        // Ensure filename has .scd extension
        if(fullPath.endsWith(".scd").not) {
            fullPath = fullPath ++ ".scd";
        };
        
        file = File(fullPath, "w");
        if(file.isOpen) {
            file.write("~snapshots = ");
            file.write(snapshots.asCompileString);
            file.close;
            this.debug("Snapshots saved to %".format(fullPath));
            ^true;
        } {
            this.debug("Failed to open file for writing: %".format(fullPath));
            ^false;
        }
    }

    // Load snapshots from a file in the snapshotData directory
    loadSnapshotsFromFile { |filename|
        var fullPath = snapshotDataPath ++ "/" ++ filename;
        var file;
        var data;
        
        // Ensure filename has .scd extension
        if(fullPath.endsWith(".scd").not) {
            fullPath = fullPath ++ ".scd";
        };
        
        file = File(fullPath, "r");
        if(file.isOpen) {
            data = file.readAllString;
            file.close;
            data.interpret;
            this.debug("Snapshots loaded from %".format(fullPath));
            ^true;
        } {
            this.debug("Failed to open file for reading: %".format(fullPath));
            ^false;
        }
    }

    // List all available snapshot files
    listSnapshotFiles {
        var dir = PathName(snapshotDataPath);
        
        if(dir.isFolder) {
            var files = dir.files.select { |f| f.extension == "scd" };
            this.debug("Available snapshot files:");
            files.do { |f| "  %".format(f.fileName).postln };
            ^files;
        } {
            this.debug("No snapshotData directory found.");
            ^[];
        }
    }

    // Enable/disable programmed mode
    setProgrammedMode { |bool, snapshotName|
        if(bool) {
            if(snapshotName.notNil) {
                if(this.loadSnapshot(snapshotName)) {
                    currentSnapshot = snapshotName;
                    programmedMode = true;
                    this.debug("Programmed mode enabled with snapshot: %".format(snapshotName));
                    ^true;
                } {
                    this.debug("Failed to enable programmed mode: snapshot not found");
                    ^false;
                };
            } {
                this.debug("Failed to enable programmed mode: no snapshot specified");
                ^false;
            };
        } {
            currentSnapshot = nil;
            programmedMode = false;
            this.debug("Programmed mode disabled");
            ^true;
        }
    }

    // Get current snapshot name
    getCurrentSnapshot {
        ^currentSnapshot;
    }

    // Check if programmed mode is active
    isProgrammedMode {
        ^programmedMode;
    }

    // Get slider value with programmed mode support
    getSliderValue { |index|
        if(this.isProgrammedMode) {
            // In programmed mode, return the snapshot value
            var snapshot = snapshots.at(currentSnapshot);
            if(snapshot.notNil) {
                ^snapshot.at(\sliders).at(index);
            } {
                ^sliderValues.at(index);
            };
        } {
            // In normal mode, return the actual slider value
            ^sliderValues.at(index);
        }
    }

    // Get knob value with programmed mode support
    getKnobValue { |index|
        if(this.isProgrammedMode) {
            // In programmed mode, return the snapshot value
            var snapshot = snapshots.at(currentSnapshot);
            if(snapshot.notNil) {
                ^snapshot.at(\knobs).at(index);
            } {
                ^knobValues.at(index);
            };
        } {
            // In normal mode, return the actual knob value
            ^knobValues.at(index);
        }
    }

    initMIDIFuncs {
        // Note On
        midiFuncs[\noteOn] = MIDIFunc.noteOn({ |veloc, pitch, chan|
            var outChan;
            
            // Skip processing if note handling is disabled
            if(noteHandlingEnabled) {
                // Determine channel based on mode
                if(multiChannelMode) {
                    // Check if this pitch is already in activeNotes (retriggered note)
                    if(activeNotes.includesKey(pitch)) {
                        outChan = activeNotes[pitch];
                        if(debug) { "Retriggering existing note on channel %".format(outChan).postln; };
                    } {
                        // Find the first available channel (not currently in use)
                        var usedChannels = activeNotes.values.asSet;
                        var availableChannels = (0..15).difference(usedChannels);
                        
                        if(availableChannels.size > 0) {
                            // Use the first available channel
                            outChan = availableChannels.asArray.sort[0];
                        } {
                            // If all channels are in use, use a modulo approach
                            outChan = numNotesPlaying % 16;
                        };
                        
                        // Store the channel assignment
                        activeNotes[pitch] = outChan;
                    };
                } {
                    outChan = 0; // Single channel mode
                };
                
                // Use the incoming MIDI velocity instead of the fixed value
                // Store it for potential future use
                velocity = veloc;
                
                if(glissandoMode) {
                    oscNetAddr.sendMsg('/glissOn', outChan, pitch);
                } {
                    if(multiInstrumentMode && multiChannelMode && vstList.notNil) {
                        // In multi-instrument mode, send to the VST that corresponds to the channel
                        var vstIndex = outChan % vstList.size;
                        var vstKey = vstList.keys.asArray[vstIndex];
                        var vst = vstList[vstKey];
                        
                        if(vst.notNil) {
                            vst.midi.noteOn(outChan, pitch, veloc); // Use incoming velocity
                            if(debug) { "Note On: pitch % chan % vel % to VST %".format(pitch, outChan, veloc, vstKey).postln; };
                            
                            // Start bend if enabled
                            this.startBend(pitch, outChan);
                        };
                    } {
                        // Normal mode - send to all VSTs
                        vstList.do { |vst| 
                            vst.midi.noteOn(outChan, pitch, veloc); // Use incoming velocity
                        };
                        
                        // Start bend if enabled
                        this.startBend(pitch, outChan);
                    };
                    
                    if(oscNetAddr.notNil) {
                        oscNetAddr.sendMsg('/keyOn', outChan, pitch, veloc); // Include velocity in OSC message
                    };
                };
                
                numNotesPlaying = numNotesPlaying + 1;
                if(debug && multiInstrumentMode.not) { "Note On: pitch % chan % vel %".format(pitch, outChan, veloc).postln; };
            } { 
                // Note handling is disabled, do nothing
                if(debug) { "Note On ignored - note handling disabled".postln; };
            };
        });

        // Note Off
        midiFuncs[\noteOff] = MIDIFunc.noteOff({ |veloc, pitch, chan|
            var outChan;
            
            // Skip processing if note handling is disabled
            if(noteHandlingEnabled) {
                // Retrieve the channel this note was assigned to
                if(multiChannelMode) {
                    outChan = activeNotes[pitch] ? 0;
                    // Only remove from activeNotes if we're sure the note is fully released
                    // This helps with quick re-triggering of the same note
                    activeNotes.removeAt(pitch);
                } {
                    outChan = 0;
                };
                
                // Reset bend before note off
                this.resetBend(outChan);
                
                if(multiInstrumentMode && multiChannelMode && vstList.notNil) {
                    // In multi-instrument mode, send to the VST that corresponds to the channel
                    var vstIndex = outChan % vstList.size;
                    var vstKey = vstList.keys.asArray[vstIndex];
                    var vst = vstList[vstKey];
                    
                    if(vst.notNil) {
                        vst.midi.noteOff(outChan, pitch, veloc);
                        if(debug) { "Note Off: pitch % chan % from VST %".format(pitch, outChan, vstKey).postln; };
                    };
                } {
                    // Normal mode - send to all VSTs
                    vstList.do { |vst| 
                        vst.midi.noteOff(outChan, pitch, veloc);
                    };
                };
                
                if(oscNetAddr.notNil) {
                    oscNetAddr.sendMsg('/keyOff', outChan, pitch);
                };
                
                numNotesPlaying = max(0, numNotesPlaying - 1);
                if(debug && multiInstrumentMode.not) { "Note Off: pitch % chan %".format(pitch, outChan).postln; };
            } {
                // Note handling is disabled, do nothing
                if(debug) { "Note Off ignored - note handling disabled".postln; };
            };
        });

        // Pitch Bend
        midiFuncs[\bend] = MIDIFunc.bend({ |bendval, channel|
            channel = 0;
            bendval.postln;
            vstList.do { |item| 
                item.midi.bend(channel, bendval);
            };
        });

        // Initialize Sliders (CC 0-7)
        8.do { |i|
            var ccNum = i;
            var sliderKey = ("slider" ++ (i+1)).asSymbol;
            
            this.debug("Setting up % for CC number %".format(sliderKey, ccNum));
            
            midiFuncs[sliderKey] = MIDIFunc.cc({ |val, num, chan, src|
                this.debug("=== MIDI Input Debug ===");
                this.debug("Slider: %".format(sliderKey));
                this.debug("CC Number: %".format(num));
                this.debug("Value: %".format(val));
                
                // Only update slider values if not in programmed mode
                if(this.isProgrammedMode.not) {
                    sliderValues[i] = val;
                    
                    if(oscNetAddr.notNil) {
                        oscNetAddr.sendMsg(("/slider" ++ (i+1)).asSymbol, val);
                    };
                } {
                    this.debug("Ignoring slider input in programmed mode");
                };
            }, ccNum);
        };

        // Initialize Knobs (CC 16-23)
        8.do { |i|
            var ccNum = i + 16;
            var knobKey = ("knob" ++ (i+1)).asSymbol;
            
            this.debug("Setting up % for CC number %".format(knobKey, ccNum));
            
            midiFuncs[knobKey] = MIDIFunc.cc({ |val, num, chan, src|
                this.debug("=== MIDI Input Debug ===");
                this.debug("Knob: %".format(knobKey));
                this.debug("CC Number: %".format(num));
                this.debug("Value: %".format(val));
                
                // Only update knob values if not in programmed mode
                if(this.isProgrammedMode.not) {
                    knobValues[i] = val;
                    
                    if(oscNetAddr.notNil) {
                        oscNetAddr.sendMsg(("/knob" ++ (i+1)).asSymbol, val);
                    };
                } {
                    this.debug("Ignoring knob input in programmed mode");
                };
            }, ccNum);
        };

        // All Notes Off Button
        midiFuncs[\allNotesOff] = MIDIFunc.cc({ |val|
            vstList.do { |item|
                5.do { |chan|
                    item.midi.allNotesOff(chan);
                };
            };
        }, 48);
    }

    // New method to enable/disable note handling without freeing the handlers
    setNoteHandlingEnabled { |enabled|
        noteHandlingEnabled = enabled;
        if(debug) {
            if(enabled) {
                "MIDIController: Note handling ENABLED".postln;
            } {
                "MIDIController: Note handling DISABLED".postln;
            };
        };
    }

    // Method to process all knobs with a function
    processKnobs { |func|
        numKnobs.do { |i|
            func.value(i, knobValues[i], knobRanges[i]);
        };
    }

    // Method to get a specific knob's values
    getKnob { |index|
        if(index < 8) {
            ^(
                value: knobValues[index],
                cc: index + 16
            )
        } {
            "Knob index out of range".error;
            ^nil
        }
    }

    // Method to set a specific knob's value
    setKnob { |index, value|
        if(index < 8) {
            knobValues[index] = value;
            knobRanges[index] = value.linlin(0, 127, 0.0, 1.0);
            oscNetAddr.sendMsg(("/knob" ++ (index+1)).asSymbol, value);
        } {
            "Knob index out of range".error;
        }
    }

    free {
        this.freeBend;
        midiFuncs.do(_.free);
    }

    stop{ 
        controlRoutine.stop;
         }

    setGlissandoMode { |bool|
        glissandoMode = bool;
    }

    setGlissandoNoteMode { |bool|
        glissandoNoteMode = bool;
    }

    // Method to start continuous VST parameter mapping
    startVSTMapping { |vstMappings, ccMappings, rate = 0.02|
        this.debug("Starting VST mapping");
        
        pollRate = rate;
        controlRoutine.stop;
        
        // If old-style single VST mapping is provided, convert to new format
        if(vstMappings.isKindOf(Symbol)) {
            var vstKey = vstMappings;
            var mappings = ccMappings ?? {[
                [0, 16, 0],
                [0, 17, 1],
                [0, 18, 2]
            ]};
            vstMappings = Dictionary.new;
            vstMappings[vstKey] = mappings;
        };
        
        // Default mapping if none provided
        vstMappings = vstMappings ?? {Dictionary[\vsti -> [
            [0, 16, 0],
            [0, 17, 1],
            [0, 18, 2]
        ]]};
        
        if(debug) {
            "VST Mappings:".postln;
            vstMappings.keysValuesDo { |vstKey, mappings|
                "VST: %".format(vstKey).postln;
                mappings.do { |mapping|
                    "Channel: %, CC: %, Knob: % (current value: %)"
                    .format(mapping[0], mapping[1], mapping[2], knobValues[mapping[2]])
                    .postln;
                };
            };
        };
        
        controlRoutine = Routine({
            inf.do {
                vstMappings.keysValuesDo { |vstKey, mappings|
                    var vst = vstList.at(vstKey);
                    if(vst.notNil) {
                        mappings.do { |mapping|
                            var chan, cc, knobIndex;
                            #chan, cc, knobIndex = mapping;
                            
                            this.debug("Sending to VST '%': chan %, cc %, knobIndex %, value %"
                                .format(vstKey, chan, cc, knobIndex, knobValues[knobIndex]));
                            
                            vst.midi.control(
                                chan, 
                                cc, 
                                knobValues[knobIndex]
                            );
                        };
                    };
                };
                pollRate.wait;
            }
        }).play;
    }
    
    // Method to change polling rate while running
    setPollRate { |newRate|
        pollRate = newRate;
    }

    // Method to toggle debug mode
    setDebug { |bool|
        debug = bool;
        this.debug("Debug mode %".format(if(bool, "enabled", "disabled")));
    }

    // Method to toggle multi-channel mode
    setMultiChannelMode { |bool|
        multiChannelMode = bool;
        // Clear active notes when changing mode
        activeNotes.clear;
        this.debug("Multi-channel mode %".format(if(bool, "enabled", "disabled")));
        
        // Disable multi-instrument mode if multi-channel mode is disabled
        if(bool.not && multiInstrumentMode) {
            multiInstrumentMode = false;
            this.debug("Multi-instrument mode disabled (requires multi-channel mode)");
        };
    }
    
    // Method to toggle multi-instrument mode
    setMultiInstrumentMode { |bool|
        if(bool && multiChannelMode.not) {
            this.debug("Cannot enable multi-instrument mode without multi-channel mode");
            ^this;
        };
        
        multiInstrumentMode = bool;
        this.debug("Multi-instrument mode %".format(if(bool, "enabled", "disabled")));
    }

    // Method to toggle velocity control mode
    setVelocityCtrlMode { |bool|
        velocityCtrlMode = bool;
        this.debug("Velocity control mode %".format(if(bool, "enabled (using knob 7)", "disabled (using MIDI input)")));
    }

    // Bend calculation helper
    calcBendValue { |fromNote, toNote, currentBend=8192|
        var semitones = toNote - fromNote;
        var unitsPerSemitone = 682;  // 8192/12 = 682.666... units per semitone
        var bendOffset = semitones * unitsPerSemitone;
        var bendValue;

        // Calculate relative to current bend position
        bendValue = currentBend + bendOffset;
        bendValue = bendValue.clip(0, 16383).asInteger;

        // For debugging
        if(debug) {
            ["Bend calculation:",
                "From:", fromNote,
                "To:", toNote,
                "Semitones:", semitones,
                "Current:", currentBend,
                "Offset:", bendOffset,
                "Final:", bendValue
            ].postln;
        };

        ^bendValue;
    }

    // Bend control methods
    setBendEnabled { |bool|
        bendEnabled = bool;
        this.debug("Bend mode %".format(if(bool, "enabled", "disabled")));
    }

    setBendRange { |semitones|
        bendRange = semitones;
        this.debug("Bend range set to % semitones".format(semitones));
    }

    setBendDuration { |seconds|
        bendDuration = seconds;
        this.debug("Bend duration set to % seconds".format(seconds));
    }

    setBendCurve { |curve|
        bendCurve = curve;
        this.debug("Bend curve set to %".format(curve));
    }

    // Start bend for a note
    startBend { |pitch, outChan|
        var targetPitch, startBend, targetBend;
        
        if(bendEnabled.not) { ^this };
        
        targetPitch = pitch + bendRange;
        startBend = 8192;  // Center position
        targetBend = this.calcBendValue(pitch, targetPitch, startBend);
        
        if(debug) {
            ["Starting Bend:",
                "Channel:", outChan,
                "Current Pitch:", pitch,
                "Target Pitch:", targetPitch,
                "Start Bend:", startBend,
                "Target Bend:", targetBend
            ].postln;
        };
        
        // Start the bend envelope
        bendEnvelopeSynth = Synth(\BendEnvelope1, [
            \start, startBend,
            \end, targetBend,
            \dur, bendDuration,
            \chanIndex, outChan
        ]);
    }

    // Reset bend for a note
    resetBend { |outChan|
        var vstIndex, vstKey, vst;
        
        if(bendEnabled.not) { ^this };
        
        if(debug) {
            ["Resetting bend for channel:", outChan].postln;
        };
        
        // Reset bend to center
        if(multiInstrumentMode && multiChannelMode && vstList.notNil) {
            vstIndex = outChan % vstList.size;
            vstKey = vstList.keys.asArray[vstIndex];
            vst = vstList[vstKey];
            
            if(vst.notNil) {
                vst.midi.bend(outChan, 8192);
            };
        } {
            vstList.do { |vst| 
                vst.midi.bend(outChan, 8192);
            };
        };
    }

    // Free bend resources
    freeBend {
        if(bendEnvelopeSynth.notNil) {
            bendEnvelopeSynth.free;
            bendEnvelopeSynth = nil;
        };
    }
}
