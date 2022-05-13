# HardwareMonitor

**Requires the [HardwareMonitorClient](https://github.com/ChristianBenner/HardwareMonitorClient)** on the machine that
you want to monitor the hardware of.

A network driven hardware monitor that can display hardware sensor data in a fully customizable and efficient way. The
monitor is compatible with multiple operating systems including Raspberry Pi OS, Windows, Ubuntu and MacOS.

The reason this project exists was to display my computers hardware sensor information such as CPU and GPU temperatures.
I've taken a step further so that less technical users can pick up the software - there is a large amount of
customisation on how sensor data is displayed and automatic scanning for devices on the network.

# Features

*(Of the Hardware Monitor System as a whole not just this project)*

- **Customisable Pages**: Multiple pages can be used to categorise different sensor data types. Users can customise
  titles, colours, layout, gauges and transitions of each page to achieve a unique design.
- **Save System**: Users can create as many designs as they want, save files can be shared with other users. Designs are
  saved automatically so that no changes are lost.
- **Page Overview**: A useful overview page will display all of the different customised pages within one design.
- **Network Scanning**: The system will scan the network for Hardware Monitors to connect to and provide a list of
  available devices to the user. Automatic connection to the last connected device means that the user does not need to
  configure the application to connect to a Hardware Monitor every time it is launched. Eliminates the need for
  determining and entering IP addresses.
- **Gauges**: Tons of animated and customisable gauges provided by [Medusa](https://github.com/HanSolo/Medusa) library.
  Provides many ways to present hardware sensor data.
- **Efficiency**: Being a Hardware Monitor, the system is designed to have as little impact on system hardware resource
  as possible. The usage of JNI through my other
  library [NativeInterface](https://github.com/ChristianBenner/NativeInterface) to communicate
  with [OpenHardwareMonitor](https://github.com/openhardwaremonitor/openhardwaremonitor) library results in miniscule
  CPU usage. The software has a state tracking system to construct the GUI only when in use.
- **Not Intrusive**: After the user has created their first design, the GUI can be closed and the software will continue
  to run in the background/system tray. Upon user sign-in, the software will run in the background, automatically
  connecting and communicating with the previously connected to Hardware Monitor.
- **Extensive Hardware Monitoring**: The usage
  of [OpenHardwareMonitor](https://github.com/openhardwaremonitor/openhardwaremonitor) library means that many of the
  users hardware sensors can be monitored such as CPU, GPU, memory and storage devices.
