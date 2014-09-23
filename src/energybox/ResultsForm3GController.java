package energybox;

import energybox.engines.Engine3G;
import java.io.File;
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
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Dialogs;
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
    XYChart.Series<Double, Integer> states;
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
    private StackedAreaChart<Double, Long> throughputChart;
    @FXML
    private AreaChart<Long, Integer> stateChart;
    @FXML
    private AreaChart<Long, Integer> stateChart2;
    @FXML
    private TextField descriptionField;
    @FXML
    private PieChart linkDistroPieChart;
    @FXML
    private LineChart<Double, Integer> powerChart;
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
    @FXML
    private NumberAxis throughputXAxis;
    @FXML
    private NumberAxis packetXAxis;
    @FXML
    private NumberAxis stateXAxis;
    @FXML
    private NumberAxis packetXAxis2;
    @FXML
    private NumberAxis powerXAxis;
    @FXML
    private NumberAxis stateXAxis2;
    @FXML
    private NumberAxis packetXAxis3;
    
    private List<NumberAxis> axisList = new ArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb){}
    
    void initData(Engine3G engine)
    {
        this.engine = engine; 

        axisList.add(throughputXAxis);
        axisList.add(packetXAxis);
        axisList.add(stateXAxis);
        axisList.add(packetXAxis2);
        axisList.add(powerXAxis);
        axisList.add(stateXAxis2);
        axisList.add(packetXAxis3);
        descriptionField.setText(engine.getSourceIP());
        //engine.modelStates();
        //engine.calculatePower();
        states = engine.getPower();
        stateChart.getXAxis().setLabel("Time(s)");
        stateChart.getYAxis().setLabel("RRC States");
        stateChart.getData().add(new XYChart.Series("FACH", engine.getFACH().getData()));
        stateChart.getData().add(new XYChart.Series("DCH", engine.getDCH().getData()));
        
        stateChart2.getXAxis().setLabel("Time(s)");
        stateChart2.getYAxis().setLabel("RRC States");
        stateChart2.getData().add(new XYChart.Series("FACH", engine.getFACH().getData()));
        stateChart2.getData().add(new XYChart.Series("DCH", engine.getDCH().getData()));
        
        powerChart.getXAxis().setLabel("Time(s)");
        powerChart.getYAxis().setLabel("Power(W)");
        powerChart.getData().add(engine.getPower());
        
        linkDistroPieChart.getData().addAll(engine.getLinkDistroData());
        stateTimePieChart.getData().addAll(engine.getStateTimeData());
        
        throughputChart.getXAxis().setLabel("Time(s)");
        throughputChart.getYAxis().setLabel("Bytes/s");
        throughputChart.getData().add(engine.getUplinkThroughput(
                engine.getPacketList().get(engine.getPacketList().size()-1).getTime()/50));
        throughputChart.getData().add(engine.getDownlinkThroughput(
                engine.getPacketList().get(engine.getPacketList().size()-1).getTime()/50));
        
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
        
        packetTable.getItems().setAll(engine.getPacketList());
        statsTable.getItems().setAll(engine.getStatisticsList());
        linkDistroTable.getItems().setAll(engine.getDistrStatisticsList());
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
        String path = OSTools.getJarLocation();
        fileChooser.setInitialDirectory((new File(path)).getParentFile().getParentFile());
        fileChooser.setTitle("Save CSV File");
        File file = fileChooser.showSaveDialog(stage);

        try
        {
            FileWriter writer = new FileWriter(file.getAbsolutePath());
            for (int i = 0; i < states.getData().size(); i++)
            {
                writer.append(states.getData().get(i).getYValue().toString());
                writer.append(",");
                writer.append(Double.valueOf(states.getData().get(i).getXValue().doubleValue()).toString());
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
        if (!fromTimeField.getText().equals(""))
        {
            try
            {
                double newFromTime = Double.parseDouble(fromTimeField.getText());
                NumberAxis something = new NumberAxis();
                for (NumberAxis axis : axisList)
                {
                    axis.setAutoRanging(false);
                    axis.setLowerBound(newFromTime);
                    axis.setTickUnit(Math.round((axis.getUpperBound()-newFromTime)*0.1));
                }
            }
            catch (NumberFormatException e)
            {
                Dialogs.showErrorDialog(null, "Not a number! Please input a number with decimal seperator '.'");
            }
        }
        else
        {
            for (NumberAxis axis : axisList)
            {
                axis.setAutoRanging(false);
                axis.setLowerBound(0);
                axis.setTickUnit(Math.round(axis.getUpperBound()*0.1));
            }
        }
    }

    @FXML
    private void toTimeAction(ActionEvent event)
    {
        if (!toTimeField.getText().equals(""))
        {
            try
            {
                double newToTime = Double.parseDouble(toTimeField.getText());
                for (NumberAxis axis : axisList)
                {
                    axis.setAutoRanging(false);
                    axis.setUpperBound(newToTime);
                    axis.setTickUnit(Math.round((newToTime-axis.getLowerBound())*0.1));
                }
            }
            catch (NumberFormatException e)
            {
                Dialogs.showErrorDialog(null, "Not a number! Please input a number with decimal seperator '.'");
            }
        }
        else
        {
            double newToTime = Double.valueOf(engine.getPacketList().get(engine.getPacketList().size()-1).getTime());
            for (NumberAxis axis : axisList)
            {
                axis.setAutoRanging(false);
                axis.setUpperBound(newToTime);
                axis.getParent().layout();
                axis.setTickUnit(Math.round((newToTime-axis.getLowerBound())*0.1));
            }
        }        
    }

    @FXML
    private void chunkSizeAction(ActionEvent event)
    {
        if (!chuckSizeField.getText().equals(""))
        {
            try
            {
                long newChunkValue = (long)(Double.parseDouble(chuckSizeField.getText()));
                throughputChart.getData().clear();
                throughputChart.getData().add(engine.getUplinkThroughput(newChunkValue));
                throughputChart.getData().add(engine.getDownlinkThroughput(newChunkValue));
            }
            catch (NumberFormatException e)
            {
                Dialogs.showErrorDialog(null, "Not a number! Please input a number with decimal seperator '.'");
            }
        }
        else
        {
            long newChunkValue = engine.getPacketList().get(engine.getPacketList().size()-1).getTimeInMicros()/50;
            throughputChart.getData().clear();
            throughputChart.getData().add(engine.getUplinkThroughput(newChunkValue));
            throughputChart.getData().add(engine.getDownlinkThroughput(newChunkValue));
        }
    }
}
