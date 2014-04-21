package energybox;

import energybox.properties.device.*;
import energybox.properties.network.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    private void handleButtonAction(ActionEvent event)
    {
        // Error buffer for file handling
        StringBuilder errbuf = new StringBuilder();
        // Wrapped lists in JavaFX ObservableList for the table view
        final ObservableList<PcapPacket> packetList = FXCollections.observableList(new ArrayList());
        final ObservableList<Packet> tableList = FXCollections.observableArrayList(new ArrayList());
        
        errorText.setText("");
        final Pcap pcap = Pcap.openOffline(textField.getText(), errbuf);
        
        if (pcap == null)
        {
            System.err.printf("Error while opening device for capture: " + errbuf.toString());
            errorText.setText("Error: " + errbuf.toString());
        }
        
        PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() 
        {  
            @Override
            public void nextPacket(PcapPacket packet, String user) 
            {
                // Copies every packet in the loop to an ArrayList for later use
                packetList.add(packet);
            }  
        };
        
        try {  pcap.loop(pcap.LOOP_INFINATE, jpacketHandler, "") ; }
        
        finally 
        {   
            pcap.close();
            for (int i = 0; i < packetList.size(); i++) 
            {
                // Adding required values to list of objects with property attributes.
                // Property attributes are easier to display in a TableView and provide
                // the ability to display changes in the table automatically using events.
                try
                {
                    Ip4 ip = new Ip4();
                    // Check to see if it's a layer 3 packet
                    // TODO: Add support for layer 2
                    if (packetList.get(i).hasHeader(ip))
                    {
                        Packet pack = new Packet(
                            // Time of packet's arrival relative to first packet
                            packetList.get(i).getCaptureHeader().timestampInMillis()-packetList.get(0).getCaptureHeader().timestampInMillis(),
                            // Packet's full length (on the wire) could differ from
                            // the captured length if the capture happens before sending
                            //packetList.get(i).getCaptureHeader().caplen(), // CAPTURED LENGTH
                            packetList.get(i).getPacketWirelen(), // LENGTH ON THE WIRE
                            // This terrible spaghetti code adds source and 
                            // destination IP addresses as Strings to the constructor
                            InetAddress.getByAddress(packetList.get(i).getHeader(ip).source()).getHostAddress(),
                            InetAddress.getByAddress(packetList.get(i).getHeader(ip).destination()).getHostAddress());
                        tableList.add(pack);
                    }
                }
                catch(UnknownHostException e){ e.printStackTrace(); }
            }
        }
        
        Properties properties = new Properties();
        // First the two variable are defined as abstract classes
        Network networkProperties = null;
        Device deviceProperties = null;
        
        properties = pathToProperties(networkField.getText());
        String type = properties.getProperty("TYPE");
        // networkProperties is initiated with the constructor of the appropriate
        // class depending on the TYPE of the .config file
        switch (type)
        {
            case "3G": networkProperties = new Properties3G(properties);
            break;
                
            case "Wifi": //TODO
            break;
        }
        
        // deviceProperties is initiated the same way as networkProperties
        properties = pathToProperties(deviceField.getText());
        type = properties.getProperty("TYPE");
        switch (type)
        {
            case "Device3G": deviceProperties = new PropertiesDevice3G(properties);
            break;
                
            case "DeviceWifi": //TODO
            break;
        }

        // Keeping the engine in the main FormController because the Engine object
        // would also contain all of the data that would have to be passed
        // to instanciate the object in the ResultsFormController
        Engine3G engine3g = new Engine3G(tableList, 
                ipField.getText(),
                //networkProperties instanced as Properties3G
                ((Properties3G)networkProperties), 
                // deviceProperties instanced as PropertiesDevice3G
                ((PropertiesDevice3G)deviceProperties));
        // Opens a new ResultsForm window and passes packet list
         showResultsForm(engine3g);        
    }
    
    @FXML
    private void handleDeviceButton(ActionEvent event)
    {
        // TODO
    }
    
    @FXML
    private void handleNetworkButton(ActionEvent event)
    {
        // TODO
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb){}
    
    
    // method to launch the ResultsFormController with the custom init method
    public Stage showResultsForm(Engine3G engine) 
    {
        try
        {
            // Creates stage from loader which gets the scene from the fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ResultsForm.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene((Parent)loader.load()));

            // Calls a method on the controller to initialize it with the required data values
            ResultsFormController controller = 
            loader.<ResultsFormController>getController();
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
}
