package energybox;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.util.Map;
import javafx.application.Platform;
import javax.swing.JOptionPane;
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
public class ProcessTrace implements Runnable
{
    MainFormController controller;
    long bytesProcessed = 0, totalBytes = 0;
    int i = 0; // For the progress update.
    
    ProcessTrace(MainFormController controller)
    {
        this.controller = controller;
    }
    
    @Override
    public void run() 
    {
        // Clear variables in case the button was used before
        controller.sourceIP = "";
        controller.addressOccurrence.clear();
        controller.criteria.clear(); 
        controller.packetList.clear();
       // Error buffer for file handling
        StringBuilder errbuf = new StringBuilder();
        // Wrapped lists in JavaFX ObservableList for the table view
        
        Pcap pcap = null;
        //controller.errorText.setText("");
        
        // Get size of trace for progress display.
        totalBytes = (new File(controller.tracePath)).length();
        // Check weather the .dll file can be found
        try { pcap = Pcap.openOffline(controller.tracePath, errbuf); }
        catch(UnsatisfiedLinkError e) 
        { 
            //Platform.runLater(new Runnable() 
            //{
                //@Override
                //public void run() 
                //{
                    //Dialogs.showErrorDialog(null, "jnetpcap.dll cannot be found!");
                    JOptionPane.showMessageDialog(null, "Libpcap not installed or jnetpcap.dll cannot be found!");
                    Thread.dumpStack();
                //}
            //});
            controller.error = true;
            Platform.runLater(controller);
            return;
        }
        
        if (pcap == null)
        {
            System.err.printf("Error while opening device for capture: " + errbuf.toString());
            //controller.errorText.setText("Error: " + errbuf.toString());
        }
        System.out.println(errbuf.toString());
        
        PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() 
        {
            boolean first = true;
            long startTime = 0;
            @Override
            public void nextPacket(PcapPacket packet, String user) 
            {
                bytesProcessed += packet.getCaptureHeader().wirelen();
                
                Platform.runLater(new Runnable() 
                {
                    @Override
                    public void run() 
                    {
                        final double progress = ((double)bytesProcessed / (double)totalBytes) / 2;
                        controller.progressBar.setProgress(progress);
                    }
                });
                
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
                            if (!controller.criteria.containsKey("HTTP"))
                            {
                                if (!packet.getHeader(new Http()).isResponse())
                                    controller.criteria.put("HTTP", InetAddress.getByAddress(packet.getHeader(new Ip4()).source()).getHostAddress());
                                else 
                                    controller.criteria.put("HTTP", InetAddress.getByAddress(packet.getHeader(new Ip4()).destination()).getHostAddress());
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
                            if (!controller.criteria.containsKey("DNS"))
                            {
                                controller.criteria.put("DNS", InetAddress.getByAddress(packet.getHeader(new Ip4()).destination()).getHostAddress());
                            }
                        }
                        if (packet.getHeader(new Udp()).destination() == 53)
                        {
                            if (!controller.criteria.containsKey("DNS"))
                            {
                                controller.criteria.put("DNS", InetAddress.getByAddress(packet.getHeader(new Ip4()).source()).getHostAddress());
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
                            if (controller.addressOccurrence.containsKey(source))
                                controller.addressOccurrence.put(source, controller.addressOccurrence.get(source)+1);
                            else
                                controller.addressOccurrence.put(source, 1);
                            // ... and DESTINATION
                            if (controller.addressOccurrence.containsKey(destination))
                                controller.addressOccurrence.put(destination, controller.addressOccurrence.get(destination)+1);
                            else
                                controller.addressOccurrence.put(destination, 1);
                            
                            controller.packetList.add(new Packet(
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
                            
                            controller.packetList.add(new Packet(
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
                        controller.packetList.add(new Packet(
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
                            if (controller.addressOccurrence.containsKey(source))
                                controller.addressOccurrence.put(source, controller.addressOccurrence.get(source)+1);
                            else
                                controller.addressOccurrence.put(source, 1);
                            // ... and DESTINATION
                            if (controller.addressOccurrence.containsKey(destination))
                                controller.addressOccurrence.put(destination, controller.addressOccurrence.get(destination)+1);
                            else
                                controller.addressOccurrence.put(destination, 1);
                            
                            controller.packetList.add(new Packet(
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
                    controller.packetList.add(new Packet(
                            packet.getCaptureHeader().timestampInMicros() - startTime,
                            packet.getPacketWirelen(),
                            "N/A",
                            "N/A",
                            "Broken Packet"));
                }
                i++;
            }  
        };
        try {  pcap.loop(pcap.LOOP_INFINATE, jpacketHandler, "") ; }
        
        finally 
        {
            pcap.close();
            
            // Manual override, if there's anything written in the ipField
            if (!controller.ipField.getText().equals(""))
            {
                controller.sourceIP = controller.ipField.getText();
            }
            // Gets most used IP address and chooses sourceIP in the following order:
            // DNS criteria, HTTP criteria and finally most frequent.
            // If the first two criteria aren't available and there are two IPs
            // with the same occurrence, the one that was added first is chosen.
            else
            {
                Map.Entry<String, Integer> maxEntry = null;
                for (Map.Entry<String, Integer> entry : controller.addressOccurrence.entrySet())
                { 
                   if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                    {
                        maxEntry = entry;
                    }
                }
                if (controller.criteria.containsKey("DNS"))
                        controller.sourceIP = controller.criteria.get("DNS");
                else if (controller.criteria.containsKey("HTTP"))
                    controller.sourceIP = controller.criteria.get("HTTP");
                else
                    controller.sourceIP = maxEntry.getKey();
            }
        }
        // Run the method that opens the results forms
        Platform.runLater(controller);
    }
}
