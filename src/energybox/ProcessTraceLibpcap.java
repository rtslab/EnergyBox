package energybox;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Arp;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.network.Ip6;
import org.jnetpcap.protocol.tcpip.Http;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class ProcessTraceLibpcap implements ProcessTrace
{
    long bytesProcessed = 0, totalBytes = 0;
    int i = 0; // For the progress update.
    private String sourceIP = "";
    private HashMap<String, Integer> addressOccurrence = new HashMap<>();
    private HashMap<String, String> criteria = new HashMap<>();
    private String usedCriteria = "";
    private final ObservableList<Packet> packetList = FXCollections.observableList(new ArrayList<Packet>());
    private String tracePath;
    private UpdatesController postExec;
    private boolean error;
    private List<String> errors = new ArrayList<>();
    private double progress = 0;
    private List<ProgressObserver> observers = new ArrayList<>();
    private String overrideIp = "";

    public ProcessTraceLibpcap(String tracePath, UpdatesController postExec) {
        this.tracePath = tracePath;
        this.postExec = postExec;
    }

    @Override
    public void run() 
    {
        // Clear variables in case the button was used before
        sourceIP = "";
        addressOccurrence.clear();
        criteria.clear();
        packetList.clear();
        errors.clear();
       // Error buffer for file handling
        StringBuilder errbuf = new StringBuilder();

        Pcap pcap = null;

        // Get size of trace for progress display.
        totalBytes = (new File(tracePath)).length();
        // Check weather the .dll file can be found
        try { pcap = Pcap.openOffline(tracePath, errbuf); }
        catch(UnsatisfiedLinkError e)
        {
            String errorMsg = "Libpcap not installed or jnetpcap.dll cannot be found!";
            errors.add(errorMsg);
            System.err.print(errorMsg);
            throw new UnsatisfiedLinkError(errorMsg);
        }

        if (pcap == null)
        {
            String errorMsg = "Error while opening device for capture: " + errbuf.toString();
            errors.add(errorMsg);
            System.err.printf(errorMsg);
            return;
        }

        PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() 
        {
            boolean first = true;
            long startTime = 0;
            @Override
            public void nextPacket(PcapPacket packet, String user) 
            {
                bytesProcessed += packet.getCaptureHeader().wirelen();
                progress = ((double)bytesProcessed / (double)totalBytes);
                notifyObservers();
                
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
                                    criteria.put("HTTP", InetAddress.getByAddress(packet.getHeader(new Ip4()).destination()).getHostAddress());
                            }
                        }
                    }
                    else if (packet.hasHeader(new Tcp())) 
                        protocol = "TCP";
                    else if (packet.hasHeader(new Udp())) 
                    {
                        // if either of the ports for any UDP packet is 53, it's
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
                        // The IP header needs to be at least 20 bytes long to
                        // be read. If the length is lower the packet may be corrupted.
                        if (packet.getHeader(new Ip4()).getHeaderLength() >= 20)
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
                        // If the header is not at least 20 bytes long then reading
                        // it would produce unwanted results, thus the source and
                        // destination fields are filled with layer 2 information
                        else
                        {
                            // Converts layer 2 addresses to String
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
                            "IPv4"));
                        }
                    }
                    else if (packet.hasHeader(new Ip6()))
                    {
                        String source = InetAddress.getByAddress(packet.getHeader(new Ip6()).source()).getHostAddress(),
                                destination = InetAddress.getByAddress(packet.getHeader(new Ip6()).destination()).getHostAddress();
                        packetList.add(new Packet(
                            // Time of packet's arrival relative to first packet
                            packet.getCaptureHeader().timestampInMicros() - startTime,
                            // Packet's full length (on the wire) could differ from
                            // the captured length if the capture happens before sending
                            //packetList.get(i).getCaptureHeader().caplen(), // CAPTURED LENGTH
                            packet.getPacketWirelen(), // LENGTH ON THE WIRE
                            source,
                            destination,
                            "IPv6"));
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
                            
                            // NON-DOT-DECIMAL ADDRESS COUNTER for SOURCE
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
                            packet.getCaptureHeader().timestampInMicros() - startTime,
                            packet.getPacketWirelen(),
                            source,
                            destination,
                            protocol));
                        }
                    }
                }
                catch(UnknownHostException e){ e.printStackTrace(); }
                // If the packet is unfinished the ByteBuffer will throw the
                // BufferUnderflowException, which would break the loop if not cought.
                catch(BufferUnderflowException e) 
                {
                    packetList.add(new Packet(
                            packet.getCaptureHeader().timestampInMicros() - startTime,
                            packet.getPacketWirelen(),
                            "N/A",
                            "N/A",
                            "Broken Packet"));
                }
                i++;
            }
        };
        try {  pcap.loop(Pcap.LOOP_INFINATE, jpacketHandler, "") ; }
        
        finally 
        {
            pcap.close();

            // Manual override, if there's anything written in the ipField
            if (!"".equals(overrideIp))
            {
                sourceIP = overrideIp;
            }
            // Gets most used IP address and chooses sourceIP in the following order:
            // DNS criteria, HTTP criteria and finally most frequent.
            // If the first two criteria aren't available and there are two IPs
            // with the same occurrence, the one that was added first is chosen.
            else
                {
                Map.Entry<String, Integer> maxEntry = null;
                for (Map.Entry<String, Integer> entry : addressOccurrence.entrySet())
                { 
                   if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                    {
                        maxEntry = entry;
                    }
                }
                if (criteria.containsKey("DNS")) {
                    usedCriteria = "DNS query";
                    sourceIP = criteria.get("DNS");
                }
                else if (criteria.containsKey("HTTP")) {
                    usedCriteria = "HTTP request";
                    sourceIP = criteria.get("HTTP");
                }
                else {
                    usedCriteria = "Common IP";
                    sourceIP = maxEntry.getKey();
                }
            }
            postExec.invoke(this);
        }
    }

    @Override
    public void notifyObservers() {
        for (ProgressObserver obs : observers)
            obs.updateProgress(this.progress);
    }

    @Override
    public ObservableList<Packet> getPacketList() {
        return packetList;
    }

    @Override
    public String getCriteria() {
        return usedCriteria;
    }

    @Override
    public HashMap<String, Integer> getAddressOccurrence() {
        return addressOccurrence;
    }

    @Override
    public String getSourceIP() {
        return sourceIP;
    }

    @Override
    public void addObserver(ProgressObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(ProgressObserver observer) {
        observers.remove(observer);
    }

    @Override
    public List<String> getErrorMessages() {
        return errors;
    }

    @Override
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public void setIp(String ip) {
        this.overrideIp = ip;
    }
}
