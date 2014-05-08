package energybox;

import energybox.properties.device.Device;
import energybox.properties.device.PropertiesDevice3G;
import energybox.properties.device.PropertiesDeviceWifi;
import energybox.properties.network.Network;
import energybox.properties.network.Properties3G;
import energybox.properties.network.PropertiesWifi;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Arp;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Http;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class ConsoleBox
{
    private final String tracePath, 
            networkPath, 
            devicePath;
    
    public ConsoleBox(String tracePath, 
            String networkPath, 
            String devicePath)
    {
        this.tracePath = tracePath;
        this.networkPath = networkPath;
        this.devicePath = devicePath;
    }
    
    public void printResults()
    {
        Network networkProperties = null;
        Device deviceProperties = null;
        final HashMap<String, String> criteria = new HashMap();
        final HashMap<String, Integer> addressOccurrence = new HashMap();
        String sourceIP = "", type = "";
        StringBuilder errbuf = new StringBuilder();
        // Wrapped lists in JavaFX ObservableList for the table view
        final ObservableList<Packet> packetList = FXCollections.observableList(new ArrayList());
        
        // READING THE PACKET TRACE
        final Pcap pcap = Pcap.openOffline(tracePath, errbuf);
        
        if (pcap == null)
        {
            System.err.printf("Error while opening device for capture: " + errbuf.toString());
            System.out.println("Error: " + errbuf.toString());
        }
        
        PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() 
        {
            boolean first = true;
            long startTime = 0;
            @Override
            public void nextPacket(PcapPacket packet, String user) 
            {
                // Copies every packet in the loop to an ArrayList for later use
                if (first)
                {
                    startTime = packet.getCaptureHeader().timestampInMicros();
                    first = false;
                }
                // Adding required values to list of objects with property attributes.
                // Property attributes are easier to display in a TableView and provide
                // the ability to display changes in the table automatically using events.
                try
                {
                    // PROTOCOL AND SOURCEIP DETECTION
                    // Marks packets with the appropriate protocol and adds an
                    // entry to the HashMap if there's a protocol related with
                    // a criteria for sourceIP
                    String protocol = "";  
                    if (packet.hasHeader(new Http())) 
                    {
                        if ((packet.getHeader(new Tcp()).source() == 443) ||
                            (packet.getHeader(new Tcp()).destination() == 443))
                        protocol = "HTTPS";
                        else 
                        {
                            protocol = "HTTP";
                            // Source if it's a request, destination if response
                            if (!criteria.containsKey("HTTP"))
                            {
                                if (!packet.getHeader(new Http()).isResponse())
                                    criteria.put("HTTP", InetAddress.getByAddress(packet.getHeader(new Ip4()).source()).getHostAddress());
                                else 
                                    criteria.put("HTTP", InetAddress.getByAddress(packet.getHeader(new Ip4()).source()).getHostAddress());
                            }
                        }
                    }
                    else if (packet.hasHeader(new Tcp())) 
                        protocol = "TCP";
                    else if (packet.hasHeader(new Udp())) 
                    {
                        // if either of the ports for any UDP pakcet is 53, it's
                        // a DNS packet
                        if (packet.getHeader(new Udp()).source() == 53)
                        {
                            protocol = "DNS";
                            if (!criteria.containsKey("DNS"))
                            {
                                criteria.put("DNS", InetAddress.getByAddress(packet.getHeader(new Ip4()).destination()).getHostAddress());
                            }
                        }
                        if (packet.getHeader(new Udp()).destination() == 53)
                        {
                            if (!criteria.containsKey("DNS"))
                            {
                                criteria.put("DNS", InetAddress.getByAddress(packet.getHeader(new Ip4()).source()).getHostAddress());
                            }
                            protocol = "DNS";
                        }
                        
                        else protocol = "UDP";
                    }
                    else if (packet.hasHeader(new Arp())) 
                        protocol = "ARP";
                    
                    // LAYER 3+ PACKETS
                    if (packet.hasHeader(new Ip4()))
                    {
                        // This terrible spaghetti code extracts source and
                        // destination IP addresses as strings
                        String source = InetAddress.getByAddress(packet.getHeader(new Ip4()).source()).getHostAddress(),
                                destination = InetAddress.getByAddress(packet.getHeader(new Ip4()).destination()).getHostAddress();
                        
                        // IP ADDRESS COUNTER for SOURCE
                        if (addressOccurrence.containsKey(source))
                            addressOccurrence.put(source, addressOccurrence.get(source)+1);
                        else
                            addressOccurrence.put(source, 1);
                        // ... and DESTINATION
                        if (addressOccurrence.containsKey(destination))
                            addressOccurrence.put(destination, addressOccurrence.get(destination)+1);
                        else
                            addressOccurrence.put(destination, 1);
                        
                        packetList.add(new Packet(
                            // Time of packet's arrival relative to first packet
                            packet.getCaptureHeader().timestampInMicros() - startTime,
                            // Packet's full length (on the wire) could differ from
                            // the captured length if the capture happens before sending
                            //packetList.get(i).getCaptureHeader().caplen(), // CAPTURED LENGTH
                            packet.getPacketWirelen(), // LENGTH ON THE WIRE
                            source,
                            destination,
                            protocol));
                    }
                    // LAYER 2 PACKETS
                    else
                    {
                        if (packet.hasHeader(new Ethernet()))
                        {
                            // Converts layers 2 addresses to String
                            byte[] mac = packet.getHeader(new Ethernet()).source();
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < mac.length; i++) {
                                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));		
                            }
                            String source = sb.toString();
                            mac = packet.getHeader(new Ethernet()).destination();
                            for (int i = 0; i < mac.length; i++) {
                                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));		
                            }
                            String destination = sb.toString();
                            
                            packetList.add(new Packet(
                            packet.getCaptureHeader().timestampInMicros() - startTime,
                            packet.getPacketWirelen(),
                            source,
                            destination,
                            protocol));
                        }
                    }
                }
                catch(UnknownHostException e){ e.printStackTrace(); }
            }  
        };
        try {  pcap.loop(pcap.LOOP_INFINATE, jpacketHandler, "") ; }
        
        finally 
        {
            pcap.close();
            // Gets most used IP address and chooses sourceIP in the following order:
            // DNS criteria, HTTP criteria and finally most frequent.
            // If the first two criteria aren't available and there are two IPs
            // with the same occurrence, the one that was added first is chosen.
            Map.Entry<String, Integer> maxEntry = null;
            for (Map.Entry<String, Integer> entry : addressOccurrence.entrySet())
            {
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                {
                    maxEntry = entry;
                }
            }
            if (criteria.containsKey("DNS"))
                    sourceIP = criteria.get("DNS");
            else if (criteria.containsKey("HTTP"))
                sourceIP = criteria.get("HTTP");
            else
                sourceIP = maxEntry.getKey();
        }
        
        // OPENING THE CONFIGURATION FILES
        // DEVICE PROPERTIES
        Properties properties = pathToProperties(devicePath);
        
        // deviceProperties is initiated the same way as networkProperties
        switch (properties.getProperty("TYPE"))
        {
            case "Device3G": deviceProperties = new PropertiesDevice3G(properties);
            break;
                
            case "DeviceWifi": deviceProperties = new PropertiesDeviceWifi(properties);
            break;
        }
        
        // NETWORK PROPERTIES
        properties = pathToProperties(networkPath);
        // networkProperties is initiated with the constructor of the appropriate
        // class depending on the TYPE of the .config file
        switch (properties.getProperty("TYPE"))
        {
            case "3G": networkProperties = new Properties3G(properties);
            break;
                
            case "Wifi": networkProperties = new PropertiesWifi(properties);
            break;
        }
        
        // CHOOSING THE ENGINE
        switch (type)
        {
            case "3G":
            {
                Engine3G engine = new Engine3G(packetList, 
                        sourceIP,
                        //networkProperties instanced as Properties3G
                        ((Properties3G)networkProperties), 
                        // deviceProperties instanced as PropertiesDevice3G
                        ((PropertiesDevice3G)deviceProperties));
                engine.getPower();
                System.out.println("Network model: 3G");
                System.out.println("Detected recorder device IP: " + sourceIP);
                System.out.println("Total power in Joules: " + engine.statisticsList.get(0).getValue());
            }
            break;
            
            case "Wifi":
            {
                EngineWifi engine = new EngineWifi(packetList, 
                        sourceIP,
                        //networkProperties instanced as Properties3G
                        ((PropertiesWifi)networkProperties), 
                        // deviceProperties instanced as PropertiesDevice3G
                        ((PropertiesDeviceWifi)deviceProperties));
                engine.getPower();
                System.out.println("Network model: 3G");
                System.out.println("Detected recorder device IP: " + sourceIP);
                System.out.println("Total power in Joules: " + engine.statisticsList.get(0).getValue());
            }
        }
    }
    
    public Properties pathToProperties(String path)
    {
        Properties properties = new Properties();
        try
        {
            File f = new File(path);
            InputStream in = new FileInputStream (f);
            properties.load(in);
        }
        catch (IOException e)
        { 
            e.printStackTrace(); 
            System.out.println("Properties file in '"+path+"' could not be opened!");
        }
        return properties;
    }
}
