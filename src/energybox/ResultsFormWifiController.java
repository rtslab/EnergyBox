package energybox;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
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
    private AreaChart<Long, Integer> stateChart;
    @FXML
    private LineChart<?, ?> packetChart2;
    @FXML
    private LineChart<?, ?> packetChart3;
    @FXML
    private TextField descriptionField;
    @FXML
    private PieChart linkDistroPieChart;
    @FXML
    private LineChart<Long, Integer> powerChart;
    @FXML
    private AreaChart<?, ?> stateChart2;
    @FXML
    private TableView<StatisticsEntry> statsTable;

    @Override
    public void initialize(URL url, ResourceBundle rb){}
    
    void initData(EngineWifi engine)
    {
        descriptionField.setText(engine.sourceIP);
        //stateChart.getData().add(engine.modelStates());
        engine.modelStates();
        engine.getPower();
        stateChart.getData().add(new XYChart.Series("CAM", engine.getCAM().getData()));
        
        stateChart2.getData().add(new XYChart.Series("CAM", engine.getCAM().getData()));
        
        powerChart.getData().add(engine.getStates());
        
        packetTable.getItems().setAll(engine.packetList);
        statsTable.getItems().setAll(engine.statisticsList);
        
        //linkDistroPieChart.getData().addAll(engine.getLinkDistroData());
        
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
