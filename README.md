EnergyBox
=========
[![Build Status](https://travis-ci.org/rtslab/EnergyBox.svg?branch=master)](https://travis-ci.org/rtslab/EnergyBox)

EnergyBox is a parametrised tool that enables accurate studies of communication energy
at the user end, using real traffic traces as input. Currently it focuses on the most
widespread wireless technologies (3G and WiFi) and emulates application data communication
energy footprint at the user device. EnergyBox performs trace based iterative packet-driven simulation.
The following figure shows an overview of the EnergyBox:

![EnergyBox](http://www.ida.liu.se/labs/rtslab/energy-efficient-networking/images/energybox.jpg)

For a given packet trace and input configuration parameters, EnergyBox simulates the underlying states
of operation in the wireless interfaces (device states). The configuration parameters relate to the cellular
operator network parameters for 3G (e.g., inactivity timers), and the adaptive power saving mode for WiFi
specified at driver level.

The tool takes as input the power draw for a given mobile device in the 3G and WiFi transmission states
(device power levels), and outputs an estimate of the consumed energy for the given packet trace, either
synthetic or captured in a device using real applications.

The versatility and accuracy of EnergyBox was evaluated in a recent paper using nine different applications
with different data patterns. A comparison with real power traces indicates that EnergyBox is a valuable tool
for repeatable and convenient studies.

EnergyBox is developed as part of the PhD thesis of Ekhiotz Vergara.
The current implementation is performed as part of the bachelor thesis of Rihards Polis.
Jens Green Olander improved the performance and refactored the code as part of his master thesis at Spotify.
EnergyBox has been used to improved the energy-efficiency of several mobile applications (e.g., Spotify).

Overview:
http://www.ida.liu.se/labs/rtslab/energy-efficient-networking/tools.html

Original paper:
http://www.sciencedirect.com/science/article/pii/S2210537914000195

PhD thesis:
http://urn.kb.se/resolve?urn=urn:nbn:se:liu:diva-124538

Bachelor thesis (Rihards Polis):
http://liu.diva-portal.org/smash/record.jsf?pid=diva2%3A783681

Master thesis (Jens Green Olander):
http://urn.kb.se/resolve?urn=urn:nbn:se:liu:diva-125789

Additional publications using EnergyBox can be found in the following website:
http://www.ida.liu.se/labs/rtslab/energy-efficient-networking/publications.html

## Requirements
* Java JDK 8u40 or greater
* tshark (OS X and Linux only)

OS X:
* `brew install tshark`

Linux:
* `sudo apt-get install tshark`

Windows:
* Install WinPcap from https://www.winpcap.org/install/

## Build
```
git clone git@github.com:rtslab/EnergyBox.git
cd EnergyBox
```
For Windows platforms use the following command: `gradle.bat jar`

For OS X and Linux use the following command: `./gradlew jar`

Optionally, the IDE *IntelliJ IDEA* comes with good support for gradle projects.

The build in Windows includes the jnetpcap library used to read pcap files. 
In order to work the jnetpcap.dll needs to be in the same directory as the compiled Jar file.

Make sure the `JAVA_HOME` environment variable points to the JDK directory.

## Run
CLI:  
```java -jar build/libs/energybox-x.y.jar --t=<trace pcap> --n=<network config> --d=<device config>```

GUI:  
```java -jar build/libs/energybox-x.y.jar```

Examples:  
```
java -jar build/libs/energybox-2.0.jar --t=path/to/trace.pcap --n=3g_teliasonera.config --d=nexus_one_3g.config
```  
```
java -jar build/libs/energybox-2.0.jar --t=path/to/trace.pcap --n=path/to/external/network.config --d=path/to/external/device.config
```
