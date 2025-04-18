# EnvironmentUI Class

The EnvironmentUI class provides a graphical user interface for managing MIDI and audio device settings, as well as VST plugin listings in SuperCollider.

## Features

### Device Management
- Dropdown menus for selecting MIDI and audio devices
- Automatic device detection and listing
- MIDI device connection handling
- Audio device selection and configuration

### VST Plugin Integration 
- List view of available VST plugins
- Integration with SuperCollider's VST plugin system
- Display of loaded plugins from global vstList

### Settings Persistence
- Automatic saving of device preferences
- Settings stored in user's application support directory
- Loads previous device selections on startup
- Graceful fallback handling for missing devices

### User Interface
- Clean, organized window layout
- Separate sections for:
  - MIDI Controllers
  - Audio Devices
  - VST Plugin List
- Real-time updates when device configurations change

## Usage
e = EnvironmentUI.new;
// The window will appear automatically with:
// - MIDI device dropdown
// - Audio device dropdown
// - VST plugin list


## Implementation Details

- Maintains lists of available MIDI and audio devices
- Automatically refreshes device listings
- Handles device connection/disconnection
- Integrates with SuperCollider's VST plugin system
- Saves settings to: `Platform.userAppSupportDir +/+ "EnvironmentUISettings.scd"`

## Dependencies
- Requires SuperCollider's VST plugin system
- Uses standard SuperCollider GUI classes
- Integrates with MIDI and audio device management systems
