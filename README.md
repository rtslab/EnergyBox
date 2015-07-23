EnergyBox
=========

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

EnergyBox is developed as part of the licenciate thesis of Ekhiotz Vergara.
The current implementation is performed as part of the bachelor thesis of Rihards Polis.

Overview:
http://www.ida.liu.se/labs/rtslab/energy-efficient-networking/tools.html

Original paper:
http://www.sciencedirect.com/science/article/pii/S2210537914000195

Licenciate thesis:
http://urn.kb.se/resolve?urn=urn:nbn:se:liu:diva-98656

Bachelor thesis:
http://liu.diva-portal.org/smash/record.jsf?pid=diva2%3A783681

## Requirements:
* Java JDK 8u40 or greater
* Gradle
* tshark (OSX and Linux only)

OS X:
* `brew install tshark gradle`

Linux:
* `sudo apt-get install tshark`
* Install gradle from http://gradle.org ([instructions](http://exponential.io/blog/2015/03/30/install-gradle-on-ubuntu-linux/))

Windows:
* Install gradle from http://gradle.org

## Build
```
git clone git@github.com:rtslab/EnergyBox.git
cd EnergyBox
gradle jar
```
or `gradlew jnetpcapJar` to bundle jnetpcap library (for Windows platforms).

Run `gradle runCli` or `gradle runGui` to verify that build works.

Optionally, the IDE *IntelliJ IDEA* comes with good support for gradle projects.

## Run
CLI:  
```java -jar build/libs/energybox-x.y.z.jar --t=<trace pcap> --n=<netork config> --d=<device config>```

GUI:  
```java -jar build/libs/energybox-x.y.z.jar``` or ```gradle runGui```

Examples:  
```
java -jar build/libs/energybox-2.0.0-SNAPSHOT.jar --t=path/to/trace.pcap --n=3g_teliasonera.config --d=nexus_one_3g.config
```  
```
java -jar build/libs/energybox-2.0.0-SNAPSHOT.jar --t=path/to/trace.pcap --n=path/to/external/network.config --d=path/to/external/device.config
```


