![Hardware Monitor Logo](https://github.com/ChristianBenner/HardwareMonitorEditor/blob/main/res/hardware_monitor_cover.png?raw=true)

# Hardware Monitor
The Hardware Monitor system (including all Hardware Monitor subsystems) are provided for free under GPLv3 Open-Source licenses. There is no monetary gain on the project except for voluntary donations through [PayPal](https://www.paypal.com/donate/?hosted_button_id=R7QL6UW899UJU), which are greatly appreciated!

**Requires the [HardwareMonitorEditor](https://github.com/ChristianBenner/HardwareMonitorEditor)** on the machine that
you want to monitor the hardware of.

## System Purpose
The system is designed to provide a method of monitoring computer systems over the network. It's purpose is to display the hardware information of one device on to another, with high levels of customisation, good visuals and most importantly, minimal hardware utilisation. It works great with a Raspberry Pi to monitor the status of your main device.

## What you will need
- Two devices, one to be monitored, another to display the information (you can test the software on just one device!)
  - The device being monitored must be running Windows
  - The device displaying the information can run on many operating systems such as Windows, Mac OS, Ubuntu, or Raspberry Pi OS
- A reliable network connection between the two devices (wired or wireless)

## Set-Up
### Monitored Device
Here we will install the Hardware Monitor Editor on the device that will be monitored. This device must be running Windows.
- Download the latest Hardware Monitor Editor version from the [editor releases page](https://github.com/ChristianBenner/HardwareMonitorEditor/releases)
- Run the installation application

### Display Device
Here we will install the Hardware Monitor Display on the device that will display the information collected from the monitored device. This device can be running Windows, Raspberry Pi OS, Ubuntu (Untested) or MacOS (Untested). Please follow the necessary steps for your target operating system.
#### Dedicated Raspberry Pi
A custom image has been created so you can easily turn your Raspberry Pi into a dedicated Hardware Monitor Display. This means that it will boot into the software seamlessly with no tricky set-up involved.
- Download the latest Hardware Monitor Display disc image file from the [releases page](https://github.com/ChristianBenner/HardwareMonitor/releases)
- Extract the image file from the ZIP
- Download Raspberry Pi Imager https://www.raspberrypi.com/software/
- Plug-in the SD card you wish to install the system to
- Launch the Raspberry Pi Imager software
- Select ‘Choose OS’, scroll down to the bottom of the list and select ‘Use custom’
- Locate and select the disc image file downloaded earlier
- Choose the SD card you wish to install the system to WARNING: This will erase all the contents of the selected SD card
- Click the settings icon
- Set a hostname e.g. ‘hardwaremonitor’ or ‘gamingpcmonitor’
- Uncheck ‘Set username and password’ as this will cause issues with auto-login and running the Hardware Monitor Display software
- To have the ability to use the device wirelessly in the future, check ‘Configure wireless LAN’ and change ‘Wireless LAN country’ to your country code. Enter your wireless network details, you can change this later on the device if necessary. You can find your country code in this [list](https://en.wikipedia.org/wiki/ISO_3166-1)
- Select ‘Write’
- Once complete, insert the SD into the Raspberry Pi, switch it on and you are ready to go. You can scan the network for your Hardware Monitor Display from the Editor
#### Windows
- Download the Hardware Monitor Display Windows from the [releases page](https://github.com/ChristianBenner/HardwareMonitor/releases)
- Run the installation application


## Features
- **Customisable Pages**: Multiple pages can be used to categorise different sensor data types. Users can customise titles, colours, layout, gauges and transitions of each page to achieve a unique design.
- **Save System**: Users can create as many designs as they want, save files can be shared with other users. Designs are saved automatically so that no changes are lost.
- **Page Overview**: A useful overview page will display all of the different customised pages within one design.
- **Network Scanning**: The system will scan the network for Hardware Monitors to connect to and provide a list of available devices to the user. Automatic connection to the last connected device means that the user does not need to configure the application to connect to a Hardware Monitor every time it is launched. Eliminates the need for determining and entering IP addresses.
- **Gauges**: Tons of animated and customisable gauges provided by [Medusa](https://github.com/HanSolo/Medusa) library. Provides many ways to present hardware sensor data.
- **Efficiency**: Being a Hardware Monitor, the system is designed to have as little impact on system hardware resource as possible. The usage of JNI through my other library [NativeInterface](https://github.com/ChristianBenner/NativeInterface) to communicate with [OpenHardwareMonitor](https://github.com/openhardwaremonitor/openhardwaremonitor) library results in miniscule CPU usage. The software has a state tracking system to construct the GUI only when in use.
- **Not Intrusive**: After the user has created their first design, the GUI can be closed and the software will continue to run in the background/system tray. Upon user sign-in, the software will run in the background, automatically connecting and communicating with the previously connected to Hardware Monitor.
- **Extensive Hardware Monitoring**: The usage of [OpenHardwareMonitor](https://github.com/openhardwaremonitor/openhardwaremonitor) library means that many of the users hardware sensors can be monitored such as CPU, GPU, memory and storage devices.
