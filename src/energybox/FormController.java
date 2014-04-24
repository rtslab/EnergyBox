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
import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
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
    String type;
    
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        /*
        // Default 3G values for testing
        tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\test\\\\random31.pcap";
        type = "3G";
        Properties properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\device_3g.config");
        deviceProperties = new PropertiesDevice3G(properties);
        properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\3g_teliasonera.config");
        networkProperties = new Properties3G(properties);
        */
        // Default Wifi values for testing
            tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\test\\\\round2.pcap";
            textField.setText("round2.pcap");
            type = "Wifi";
            Properties properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\samsungS2_wifi.config");
            deviceField.setText("samsungS2_wifi.config");
            deviceProperties = new PropertiesDeviceWifi(properties);
            properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\wifi_general.config");
            networkField.setText("wifi_general.config");
            networkProperties = new PropertiesWifi(properties);
    }
    
    @FXML
    public void handleButtonAction(ActionEvent event)
    {
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
                //packetList.add(packet);
                if (first)
                {
                    startTime = packet.getCaptureHeader().timestampInMillis();
                    first = false;
                }
                // Adding required values to list of objects with property attributes.
                // Property attributes are easier to display in a TableView and provide
                // the ability to display changes in the table automatically using events.
                try
                {
                    Ip4 ip = new Ip4();
                    // Check to see if it's a layer 3 packet
                    // TODO: Add support for layer 2
                    if (packet.hasHeader(ip))
                    {
                        packetList.add(new Packet(
                            // Time of packet's arrival relative to first packet
                            packet.getCaptureHeader().timestampInMillis() - startTime,
                            // Packet's full length (on the wire) could differ from
                            // the captured length if the capture happens before sending
                            //packetList.get(i).getCaptureHeader().caplen(), // CAPTURED LENGTH
                            packet.getPacketWirelen(), // LENGTH ON THE WIRE
                            // This terrible spaghetti code adds source and 
                            // destination IP addresses as Strings to the constructor
                            InetAddress.getByAddress(packet.getHeader(ip).source()).getHostAddress(),
                            InetAddress.getByAddress(packet.getHeader(ip).destination()).getHostAddress()));
                        //tableList.add(pack);
                    }
                }
                catch(UnknownHostException e){ e.printStackTrace(); }
            }  
        };
        try {  pcap.loop(pcap.LOOP_INFINATE, jpacketHandler, "") ; }
        
        finally 
        {
            pcap.close();
        } 
        // Keeping the engine in the main FormController because the Engine object
        // would also contain all of the data that would have to be passed
        // to instanciate the object in the ResultsFormController
        switch (type)
        {
            case "3G":
            {
                Engine3G engine = new Engine3G(packetList, 
                        ipField.getText(),
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
                        ipField.getText(),
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
