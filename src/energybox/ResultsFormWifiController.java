package energybox;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class ResultsFormWifiController implements Initializable
{
    @FXML
    private AnchorPane ActionPane1;
    @FXML
    private StackedAreaChart<Long, Long> throughputChart;
    @FXML
    private LineChart<?, ?> packetChart;
    @FXML
    private TableView<Packet> packetTable;
    @FXML
    private TableColumn<?, ?> timeCol;
    @FXML
    private TableColumn<?, ?> lengthCol;
    @FXML
    private TableColumn<?, ?> sourceCol;
    @FXML
    private TableColumn<?, ?> destinationCol;
    @FXML
    private TableColumn<?, ?> linkCol;
    @FXML
    private AreaChart<Long, Integer> stateChart;
    @FXML
    private LineChart<?, ?> packetChart2;
    @FXML
    private LineChart<?, ?> packetChart3;
    @FXML
    private TextField descriptionField;

    @Override
    public void initialize(URL url, ResourceBundle rb){}
    
    void initData(EngineWifi engine)
    {
        /*
        HashMap<String, Integer> count = new HashMap();
        count.put("MAXIMAL", 10);
        count.put("NOTHING", 5);
        count.put("MINIMAL", 10);
        Collections.max(count.values());*/
        stateChart.getData().add(engine.modelStates());
        
        packetTable.getItems().setAll(engine.packetList);
        
        throughputChart.getData().add(engine.getUplinkThroughput());
        throughputChart.getData().add(engine.getDownlinkThroughput());
        
        packetChart.getData().add(new XYChart.Series("Uplink", engine.getUplinkPackets().getData()));
        packetChart.getData().add(new XYChart.Series("Downlink", engine.getDownlinkPackets().getData()));
        
        packetChart2.getData().add(new XYChart.Series("Uplink", engine.getUplinkPackets().getData()));
        packetChart2.getData().add(new XYChart.Series("Downlink", engine.getDownlinkPackets().getData()));
        
        packetChart3.getData().add(new XYChart.Series("Uplink", engine.getUplinkPackets().getData()));
        packetChart3.getData().add(new XYChart.Series("Downlink", engine.getDownlinkPackets().getData()));
    }
}
