package energybox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class ResultsForm3GController implements Initializable
{
    XYChart.Series<Long, Integer> states;
    Engine3G engine = null;
    
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
    private AreaChart<Long, Integer> stateChart2;
    @FXML
    private TextField descriptionField;
    @FXML
    private PieChart linkDistroPieChart;
    @FXML
    private LineChart<Long, Integer> powerChart;
    @FXML
    private TableView<StatisticsEntry> statsTable;
    @FXML
    private MenuItem menuItemExportCSV;
    @FXML
    private TableView<StatisticsEntry> linkDistroTable;
    @FXML
    private PieChart stateTimePieChart;
    @FXML
    private TextField chuckSizeField;
    @FXML
    private TextField fromTimeField;
    @FXML
    private TextField toTimeField;

    @Override
    public void initialize(URL url, ResourceBundle rb){}
    
    void initData(Engine3G engine)
    {
        this.engine = engine;
        states = engine.getStates();
        
        descriptionField.setText(engine.sourceIP);
        /*
        packetChart.getXAxis().setAutoRanging(true);
        packetChart.getYAxis().setAutoRanging(true);
        stateChart.getXAxis().setAutoRanging(true);
        stateChart.getYAxis().setAutoRanging(true);*/
        engine.modelStates();
        engine.getPower();
        stateChart.getXAxis().setLabel("Time(s)");
        stateChart.getYAxis().setLabel("States");
        stateChart.getData().add(new XYChart.Series("FACH", engine.getFACH().getData()));
        stateChart.getData().add(new XYChart.Series("DCH", engine.getDCH().getData()));
        
        stateChart2.getXAxis().setLabel("Time(s)");
        stateChart2.getYAxis().setLabel("States");
        stateChart2.getData().add(new XYChart.Series("FACH", engine.getFACH().getData()));
        stateChart2.getData().add(new XYChart.Series("DCH", engine.getDCH().getData()));
        
        powerChart.getXAxis().setLabel("Time(s)");
        powerChart.getYAxis().setLabel("Power(J)");
        powerChart.getData().add(engine.getStates());
        
        linkDistroPieChart.getData().addAll(engine.getLinkDistroData());
        stateTimePieChart.getData().addAll(engine.getStateTimeData());
        
        throughputChart.getXAxis().setLabel("Time(s)");
        throughputChart.getYAxis().setLabel("Bytes/s");
        throughputChart.getData().add(engine.getUplinkThroughput(
                engine.packetList.get(engine.packetList.size()-1).getTimeInMicros()/50));
        throughputChart.getData().add(engine.getDownlinkThroughput(
                engine.packetList.get(engine.packetList.size()-1).getTimeInMicros()/50));
        
        packetChart.getXAxis().setLabel("Time(s)");
        packetChart.getYAxis().setLabel("Size(bytes)");
        packetChart.getData().add(new XYChart.Series("Uplink", engine.getUplinkPackets().getData()));
        packetChart.getData().add(new XYChart.Series("Downlink", engine.getDownlinkPackets().getData()));
        
        packetChart2.getXAxis().setLabel("Time(s)");
        packetChart2.getYAxis().setLabel("Size(bytes)");
        packetChart2.getData().add(new XYChart.Series("Uplink", engine.getUplinkPackets().getData()));
        packetChart2.getData().add(new XYChart.Series("Downlink", engine.getDownlinkPackets().getData()));
        
        packetChart3.getXAxis().setLabel("Time(s)");
        packetChart3.getYAxis().setLabel("Size(bytes)");
        packetChart3.getData().add(new XYChart.Series("Uplink", engine.getUplinkPackets().getData()));
        packetChart3.getData().add(new XYChart.Series("Downlink", engine.getDownlinkPackets().getData()));
        
        packetTable.getItems().setAll(engine.packetList);
        statsTable.getItems().setAll(engine.statisticsList);
        linkDistroTable.getItems().setAll(engine.distrStatisticsList);
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
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        String path = FormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = "";
        
        try { decodedPath = URLDecoder.decode(path, "UTF-8"); }
        catch (UnsupportedEncodingException e){ e.printStackTrace(); }
        fileChooser.setInitialDirectory((new File(decodedPath)).getParentFile().getParentFile());
        fileChooser.setTitle("Save CSV File");
        File file = fileChooser.showSaveDialog(stage);

        try
        {
            FileWriter writer = new FileWriter(file.getAbsolutePath());
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

    @FXML
    private void fromTimeAction(ActionEvent event)
    {
    }

    @FXML
    private void toTimeAction(ActionEvent event)
    {
        long newToTime = (long)(Double.parseDouble(toTimeField.getText())*1000000);
        //throughputChart.getXAxis()
    }

    @FXML
    private void chunkSizeAction(ActionEvent event)
    {
        long newChunkValue = (long)(Double.parseDouble(chuckSizeField.getText())*1000000);
        throughputChart.getData().clear();
        throughputChart.getData().add(engine.getUplinkThroughput(newChunkValue));
        throughputChart.getData().add(engine.getDownlinkThroughput(newChunkValue));
    }
}
