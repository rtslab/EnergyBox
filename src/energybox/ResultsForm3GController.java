package energybox;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class ResultsForm3GController implements Initializable
{
    XYChart.Series<Long, Integer> states;
    List<Engine3G.TransitionPair> transitions = new ArrayList();
    
    @FXML
    private LineChart<Long, Integer> packetChart;
    @FXML
    private LineChart<Long, Integer> packetChart2;
    @FXML
    private LineChart<Long, Integer> packetChart3;
    @FXML
    private AnchorPane ActionPane1;
    @FXML
    private TableView<Packet> packetTable;
    @FXML
    private StackedAreaChart<Long, Long> throughputChart;
    @FXML
    private AreaChart<Long, Integer> stateChart;
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
    @FXML
    private MenuItem menuItemExportCSV;

    @Override
    public void initialize(URL url, ResourceBundle rb){}
    
    void initData(Engine3G engine)
    {
        transitions = engine.transitions;
        states = engine.getStates();
        
        descriptionField.setText(engine.sourceIP);
        packetChart.getXAxis().setAutoRanging(true);
        packetChart.getYAxis().setAutoRanging(true);
        stateChart.getXAxis().setAutoRanging(true);
        stateChart.getYAxis().setAutoRanging(true);
        engine.modelStates();
        engine.getPower();
        stateChart.getData().add(new XYChart.Series("FACH", engine.getFACH().getData()));
        stateChart.getData().add(new XYChart.Series("DCH", engine.getDCH().getData()));
        
        stateChart2.getData().add(new XYChart.Series("FACH", engine.getFACH().getData()));
        stateChart2.getData().add(new XYChart.Series("DCH", engine.getDCH().getData()));
        
        powerChart.getData().add(engine.getStates());
        
        linkDistroPieChart.getData().addAll(engine.getLinkDistroData());
        
        throughputChart.getData().add(engine.getUplinkThroughput());
        throughputChart.getData().add(engine.getDownlinkThroughput());
        
        packetChart.getData().add(new XYChart.Series("Uplink", engine.getUplinkPackets().getData()));
        packetChart.getData().add(new XYChart.Series("Downlink", engine.getDownlinkPackets().getData()));
        
        packetChart2.getData().add(new XYChart.Series("Uplink", engine.getUplinkPackets().getData()));
        packetChart2.getData().add(new XYChart.Series("Downlink", engine.getDownlinkPackets().getData()));
        
        packetChart3.getData().add(new XYChart.Series("Uplink", engine.getUplinkPackets().getData()));
        packetChart3.getData().add(new XYChart.Series("Downlink", engine.getDownlinkPackets().getData()));
        
        packetTable.getItems().setAll(engine.packetList);
        statsTable.getItems().setAll(engine.statisticsList);
        // TODO: put Property Factories in control instead of FXML
        /*
        timeCol.setCellFactory(new PropertyValueFactory<Packet, Long>("time"));
        lengthCol.setCellFactory(new PropertyValueFactory<Packet, Integer>("length"));
        sourceCol.setCellFactory(new PropertyValueFactory<Packet, String>("source"));
        destinationCol.setCellFactory(new PropertyValueFactory<Packet, String>("destination"));
        */
    }

    @FXML
    private void exportCSVAction(ActionEvent event)
    {
        try
        {
            FileWriter writer = new FileWriter("D:\\Source\\NetBeansProjects\\EnergyBox\\export.csv");
            for (int i = 0; i < states.getData().size(); i++)
            {
                writer.append(states.getData().get(i).getYValue().toString());
                writer.append(",");
                writer.append(Double.valueOf(states.getData().get(i).getXValue().doubleValue()/1000000).toString());
                writer.append("\n");
            }
            writer.flush();
	    writer.close();
        }
        catch(IOException e){ e.printStackTrace();}
    }
}
