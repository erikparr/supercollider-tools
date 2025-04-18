// File: EnvironmentUI.sc
EnvironmentUI {
    var midiDevices, audioDevices;
    var selectedMidiDevice, selectedAudioDevice;
    var window, midiDropdown, audioDropdown, vstListView;
    var serverBooted = false;
    var s; // Declare 's' as an instance variable

    *new {
        ^super.new.init;
    }

    init {
        // Initialize variables
        midiDevices = [];
        audioDevices = [];

        selectedMidiDevice = nil;
        selectedAudioDevice = nil;

        this.refreshDevices;
        this.loadSettings;
        this.initializeUI;
    }

    initializeUI {
        // Create the GUI Window
        window = Window("EnvironmentUI", Rect(100, 100, 400, 400)).front;

        // MIDI Controller Panel
        StaticText(window, Rect(10, 10, 380, 20))
            .string_("MIDI Controllers:");
        midiDropdown = PopUpMenu(window, Rect(10, 30, 380, 20))
            .items_(midiDevices.collect(_.name))
            .action_({ |menu|
                selectedMidiDevice = midiDevices[menu.value];
                "Selected MIDI Device: %\n".format(selectedMidiDevice.name).postln;
                this.connectMidiDevice;
                this.saveSettings;
            });

        // Audio Device Panel
        StaticText(window, Rect(10, 60, 380, 20))
            .string_("Audio Devices:");
        audioDropdown = PopUpMenu(window, Rect(10, 80, 380, 20))
            .items_(audioDevices)
            .action_({ |menu|
                selectedAudioDevice = audioDevices[menu.value];
                "Selected Audio Device: %\n".format(selectedAudioDevice).postln;
                this.saveSettings;
            });

        // VST Plugins List View
        StaticText(window, Rect(10, 110, 380, 20))
            .string_("VST Plugins (from vstList):");
        vstListView = ListView(window, Rect(10, 130, 380, 200))
            .items_(this.getVSTListItems);

        // Set initial selections
        this.updateDropdowns;
    }

    refreshDevices {
        // Retrieve MIDI and Audio Devices
        MIDIClient.init;
        midiDevices = MIDIClient.destinations;
        audioDevices = ServerOptions.devices;

        // Update dropdown items if UI is initialized
        if(midiDropdown.notNil) {
            midiDropdown.items_(midiDevices.collect(_.name));
        };
        if(audioDropdown.notNil) {
            audioDropdown.items_(audioDevices);
        };
    }

    connectMidiDevice {
        // Connect to the selected MIDI device
        if(selectedMidiDevice.notNil) {
            MIDIIn.connectAll;
            "Connected to MIDI Device: %\n".format(selectedMidiDevice.name).postln;
        };
    }

    loadSettings {
        var path, settingsString, settings;
        var midiDeviceName, audioDeviceName, midiDevice, audioDevice;
        path = Platform.userAppSupportDir +/+ "EnvironmentUISettings.scd";

        if(File.exists(path)) {
            try {
                settingsString = File.use(path, "r", { |file|
                    file.readAllString.stripWhiteSpace;
                });
                
                settings = settingsString.interpret;

                if(settings.isKindOf(Dictionary)) {
                    // Retrieve stored device names
                    midiDeviceName = settings.at(\selectedMidiDeviceName, nil);
                    audioDeviceName = settings.at(\selectedAudioDevice, nil);

                    // Find the MIDI device with the matching name
                    if(midiDeviceName.notNil) {
                        midiDevice = midiDevices.detect { |device| device.name == midiDeviceName };
                        if(midiDevice.notNil) {
                            selectedMidiDevice = midiDevice;
                        } {
                            ("Saved MIDI device '%s' not found. Using default.".format(midiDeviceName)).warn;
                            selectedMidiDevice = nil;
                        };
                    };

                    // Find the audio device with the matching name
                    if(audioDeviceName.notNil) {
                        audioDevice = audioDevices.detect { |device| device == audioDeviceName };
                        if(audioDevice.notNil) {
                            selectedAudioDevice = audioDevice;
                        } {
                            ("Saved audio device '%s' not found. Using default.".format(audioDeviceName)).warn;
                            selectedAudioDevice = nil;
                        };
                    };

                    "Settings loaded successfully.".postln;
                } {
                    "Settings file is not a valid Dictionary. Using default settings.".warn;
                };
            } {
                |error|
                "Error loading settings file: %\nUsing default settings.".format(error.errorString).warn;
                // Delete corrupted settings file
                File.delete(path);
            };
        } {
            "Settings file not found. Using default settings.".postln;
        };
    }

    saveSettings {
        var path, settings;
        path = Platform.userAppSupportDir +/+ "EnvironmentUISettings.scd";
        
        // Log what we're saving
        "Saving settings:".postln;
        ("MIDI Device: " ++ (selectedMidiDevice.tryPerform(\name) ?? "None")).postln;
        ("Audio Device: " ++ (selectedAudioDevice ?? "None")).postln;

        // Create clean dictionary
        settings = Dictionary.new;
        if(selectedMidiDevice.notNil) {
            settings.put(\selectedMidiDeviceName, selectedMidiDevice.name.asString);
        };
        if(selectedAudioDevice.notNil) {
            settings.put(\selectedAudioDevice, selectedAudioDevice.asString);
        };

        // Write settings with proper string formatting
        File.use(path, "w", { |file|
            file.write(settings.asCompileString);
        });
        
        "Settings saved.".postln;
    }

    updateDropdowns {
        // Update the dropdown selections based on loaded settings
        var midiIndex, audioIndex;

        if(selectedMidiDevice.notNil) {
            midiIndex = midiDevices.indexOf(selectedMidiDevice);
            if(midiIndex.isInteger && (midiIndex != -1)) {
                midiDropdown.value_(midiIndex);
            } {
                "Selected MIDI device not found in device list.".warn;
            };
        };

        if(selectedAudioDevice.notNil) {
            audioIndex = audioDevices.indexOf(selectedAudioDevice);
            if(audioIndex.isInteger && (audioIndex != -1)) {
                audioDropdown.value_(audioIndex);
            } {
                "Selected audio device not found in device list.".warn;
            };
        };

        // Update VST List View
        if(vstListView.notNil) {
            vstListView.items_(this.getVSTListItems);
        };
    }

    getVSTListItems {
        // Retrieve vstList items for display
        if(~vstList.notNil) {
            ^~vstList.keys.as(Array);
        } {
            ^["No VST Plugins loaded in vstList"];
        };
    }
}
