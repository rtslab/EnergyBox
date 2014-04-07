package energybox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class FormController implements Initializable
{
    @FXML
    private TableView<Packet> packetTable;     
    @FXML
    private Label errorText;
    @FXML
    private Button button;
    @FXML
    private Label label;
    @FXML
    private TextField textField;
    @FXML
    private TableColumn<?, ?> timeCol;
    @FXML
    private TableColumn<?, ?> lengthCol;
    @FXML
    private void handleButtonAction(ActionEvent event)
    {
        // Error buffer for file handling
        StringBuilder errbuf = new StringBuilder();
        // Wrapped lists in JavaFX ObservableList for the table view
        final ObservableList<PcapPacket> packetList = FXCollections.observableList(new ArrayList());
        final ObservableList<Packet> tableList = FXCollections.observableArrayList(new ArrayList());
        String path = "D:\\Source\\NetBeansProjects\\EnergyBox\\src\\energybox\\chunks5mins3.pcap"; // FOR TESTING
        
        //String path = textField.getText();
        errorText.setText("");
        final Pcap pcap = Pcap.openOffline(path, errbuf);
        
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
            for (int i = 0; i < packetList.size(); i++) {
                // Adding required values to list of objects with property attributes.
                // Property attributes are easier to display in a TableView and provide
                // the ability to display changes in the table automatically using events.
                Packet pack = new Packet(
                    packetList.get(i).getCaptureHeader().timestampInMicros(),
                    packetList.get(i).getCaptureHeader().caplen());
                tableList.add(pack);
            }
        }
        // TODO: put Property Factories in control instead of FXML
        packetTable.getItems().setAll(tableList);
        
        // Opens a new ResultsForm window and passes packet list
        showResultsForm(tableList);        
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb){}
    
    public Stage showResultsForm(ObservableList<Packet> packets) 
    {
        try
        {
            // Creates stage from loader which gets scene from the fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ResultsForm.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene((Parent)loader.load()));

            // Calls a method on the controller to initialize it with the required data values
            ResultsFormController controller = 
            loader.<ResultsFormController>getController();
            controller.initData(packets);
            stage.show();
            return stage;
        }
        catch (IOException e){ e.printStackTrace(); }
        return null;
    }
}
