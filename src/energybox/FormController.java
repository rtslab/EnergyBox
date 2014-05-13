package energybox;

import energybox.properties.device.*;
import energybox.properties.network.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.swing.JOptionPane;
import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Arp;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Http;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;
import org.jnetpcap.winpcap.WinPcap;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class FormController implements Initializable
{
    @FXML
    private Label errorText;
    @FXML
    private Button button;
    @FXML
    private TextField textField;
    @FXML
    private TextField deviceField;
    @FXML
    private TextField networkField;
    @FXML
    private Button deviceButton;
    @FXML
    private Button networkButton;
    @FXML
    private TextField ipField;
    @FXML
    private Button traceButton;
    
    Network networkProperties = null;
    Device deviceProperties = null;
    String tracePath;
    String type; // for engine selection
    // Two possible criteria that can indicate the source IP:
    // -> "HTTP" - a phone is usually not running a web server, so any GET requests are from the the recording device
    // -> "DNS" - a phone is usually not running a DNS server so any requests are from the the recording device 
    HashMap<String, String> criteria = new HashMap();
    String sourceIP = "";
    HashMap<String, Integer> addressOccurrence = new HashMap();
    
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        
        String os = OSTools.getOS();
        //System.out.println(System.getProperty("java.library.path"));
        switch(os)
        {
            case "Windows":
            {
                String location = FormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedLocation = "";
                try { decodedLocation = URLDecoder.decode(location, "UTF-8"); }
                catch (UnsupportedEncodingException e){ e.printStackTrace(); }
                StringBuilder relativePath = new StringBuilder();
                relativePath.append(new File(decodedLocation).getParent());
                relativePath.append(File.separator);
                try { OSTools.addDirectory(relativePath.toString()); }
                catch (IOException e)
                {
                    JOptionPane.showMessageDialog(null, e.getMessage());
                }
            }
            break;
                
            case "Linux":
            {
                String location = FormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedLocation = "";
                try { decodedLocation = URLDecoder.decode(location, "UTF-8"); }
                catch (UnsupportedEncodingException e){ e.printStackTrace(); }
                StringBuilder relativePath = new StringBuilder();
                relativePath.append(new File(decodedLocation).getParent());
                relativePath.append(File.separator);
                try { OSTools.addDirectory(relativePath.toString()); }
                catch (IOException e)
                {
                    JOptionPane.showMessageDialog(null, e.getMessage());
                }
                try { WinPcap.isSupported(); }
                catch (UnsatisfiedLinkError e)
                {
                    JOptionPane.showMessageDialog(null, "Libpcap-dev not installed!");
                }
            }
            break;
        }
        //System.out.println(System.getProperty("java.library.path"));
        /*
        // Default 3G values for testing
        tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\test\\\\test1UL.pcap";
        textField.setText("test1UL.pcap");
        //ipField.setText("10.209.43.104");
        type = "3G";
        Properties properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\device_3g.config");
        deviceProperties = new PropertiesDevice3G(properties);
        properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\3g_teliasonera.config");
        networkProperties = new Properties3G(properties);
        
        // Default Wifi values for testing
            tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\test\\\\round2.pcap";
            textField.setText("round2.pcap");
            type = "Wifi";
            Properties properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\samsungS2_wifi.config");
            deviceField.setText("samsungS2_wifi.config");
            deviceProperties = new PropertiesDeviceWifi(properties);
            properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\wifi_general.config");
            networkField.setText("wifi_general.config");
            networkProperties = new PropertiesWifi(properties);*/
    }
    
    @FXML
    public void handleButtonAction(ActionEvent event)
    {
        // Clear variables in case the button was used before
        sourceIP = "";
        addressOccurrence.clear();
        criteria.clear();
        // Error buffer for file handling
        StringBuilder errbuf = new StringBuilder();
        // Wrapped lists in JavaFX ObservableList for the table view
        final ObservableList<Packet> packetList = FXCollections.observableList(new ArrayList());
        
        errorText.setText("");
        final Pcap pcap = Pcap.openOffline(tracePath, errbuf);
        
        if (pcap == null)
        {
            System.err.printf("Error while opening device for capture: " + errbuf.toString());
            errorText.setText("Error: " + errbuf.toString());
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
        // Keeping the engine in the main FormController because the Engine object
        // would also contain all of the data that would have to be passed
        // to instanciate the object in the ResultsFormController
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
                // Opens a new ResultsForm window and passes appropriate engine
                showResultsForm3G(engine);
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
                // Opens a new ResultsForm window and passes appropriate engine
                showResultsFormWifi(engine);
            }
        }
    }
    
    @FXML
    private void handleDeviceButton(ActionEvent event)
    {
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        String path = FormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = "";
        
        try { decodedPath = URLDecoder.decode(path, "UTF-8"); }
        catch (UnsupportedEncodingException e){ e.printStackTrace(); }
        fileChooser.setInitialDirectory((new File(decodedPath)).getParentFile().getParentFile());
        fileChooser.setTitle("Open Device Configuration File");
        File file = fileChooser.showOpenDialog(stage);
        Properties properties = fileToProperties(file);
        
        // deviceProperties is initiated the same way as networkProperties
        switch (properties.getProperty("TYPE"))
        {
            case "Device3G": deviceProperties = new PropertiesDevice3G(properties);
            break;
                
            case "DeviceWifi": deviceProperties = new PropertiesDeviceWifi(properties);
            break;
        }
        deviceField.setText(file.getName());
    }
    
    @FXML
    private void handleNetworkButton(ActionEvent event)
    {
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        String path = FormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = "";
        
        try { decodedPath = URLDecoder.decode(path, "UTF-8"); }
        catch (UnsupportedEncodingException e){ e.printStackTrace(); }
        fileChooser.setInitialDirectory((new File(decodedPath)).getParentFile().getParentFile());
        fileChooser.setTitle("Open Network Configuration File");
        File file = fileChooser.showOpenDialog(stage);
        Properties properties = fileToProperties(file);
        // networkProperties is initiated with the constructor of the appropriate
        // class depending on the TYPE of the .config file
        switch (properties.getProperty("TYPE"))
        {
            case "3G": networkProperties = new Properties3G(properties);
            break;
                
            case "Wifi": networkProperties = new PropertiesWifi(properties);
            break;
        }
        type = properties.getProperty("TYPE");
        networkField.setText(file.getName());
    }
    
    @FXML
    private void handleTraceButton(ActionEvent event)
    {
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        String path = FormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = "";
        
        try { decodedPath = URLDecoder.decode(path, "UTF-8"); }
        catch (UnsupportedEncodingException e){ e.printStackTrace(); }
        fileChooser.setInitialDirectory((new File(decodedPath)).getParentFile().getParentFile());
        fileChooser.setTitle("Open Network Configuration File");
        File file = fileChooser.showOpenDialog(stage);
        
        tracePath = file.getAbsolutePath();
        textField.setText(file.getName());
    }
    
    // method to launch the ResultsFormController with the custom init method
    public Stage showResultsForm3G(Engine3G engine) 
    {
        try
        {
            // Creates stage from loader which gets the scene from the fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ResultsForm3G.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene((Parent)loader.load()));
            stage.setTitle(textField.getText());

            // Calls a method on the controller to initialize it with the required data values
            ResultsForm3GController controller = 
            loader.<ResultsForm3GController>getController();
            controller.initData(engine);
            stage.show();
            return stage;
        }
        catch (IOException e){ e.printStackTrace(); }
        return null;
    }
    
    // method to launch the ResultsFormWifiController with the custom init method
    public Stage showResultsFormWifi(EngineWifi engine) 
    {
        try
        {
            // Creates stage from loader which gets the scene from the fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ResultsFormWifi.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene((Parent)loader.load()));
            stage.setTitle(textField.getText());

            // Calls a method on the controller to initialize it with the required data values
            ResultsFormWifiController controller = 
            loader.<ResultsFormWifiController>getController();
            controller.initData(engine);
            stage.show();
            return stage;
        }
        catch (IOException e){ e.printStackTrace(); }
        return null;
    }

    // Depricated! Only used for testing    
    public Properties pathToProperties(String path)
    {
        Properties properties = new Properties();
        try
        {
            File f = new File(path);
            InputStream in = new FileInputStream (f);
            properties.load(in);
        }
        catch (IOException e){ e.printStackTrace(); }
        return properties;
    }
    
    public Properties fileToProperties(File f)
    {
        Properties properties = new Properties();
        try
        {
            InputStream in = new FileInputStream (f);
            properties.load(in);
        }
        catch (IOException e){ e.printStackTrace(); }
        return properties;
    }
}
