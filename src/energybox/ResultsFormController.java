package energybox;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class ResultsFormController implements Initializable
{
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
    private TableColumn<?, ?> timeCol;
    @FXML
    private TableColumn<?, ?> lengthCol;
    @FXML
    private TableColumn<?, ?> sourceCol;
    @FXML
    private TableColumn<?, ?> destinationCol;
    @FXML
    private TableColumn<?, ?> linkCol;
    
    @Override
    public void initialize(URL url, ResourceBundle rb){}
    
    void initData(ObservableList<Packet> packetList) 
    {
        packetChart.getXAxis().setAutoRanging(true);
        packetChart.getYAxis().setAutoRanging(true);
        
        // Temp fix to charts not drawing with the same data series
        // TODO: get all three charts to draw using the same data series without
        // creating redundant series that take up memmory
        XYChart.Series<Long, Integer> uplinkSeries = new XYChart.Series();
        XYChart.Series<Long, Integer> uplinkSeries2 = new XYChart.Series();
        XYChart.Series<Long, Integer> uplinkSeries3 = new XYChart.Series();
        
        XYChart.Series<Long, Integer> downlinkSeries = new XYChart.Series();
        XYChart.Series<Long, Integer> downlinkSeries2 = new XYChart.Series();
        XYChart.Series<Long, Integer> downlinkSeries3 = new XYChart.Series();
        
        uplinkSeries.setName("Uplink");
        downlinkSeries.setName("Downlink");
        for (int i = 0; i < packetList.size(); i++) {
            // Adds three points to either the UL or DL series - 
            // one on the axis, one at 2(UL) or 1(DL), and one more on the axis
            if (packetList.get(i).getUplink())
            {
                uplinkSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                uplinkSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,2));//- packetList.get(0).getTime(), 2));
                uplinkSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                
                uplinkSeries2.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                uplinkSeries2.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,2));//- packetList.get(0).getTime(), 2));
                uplinkSeries2.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                
                uplinkSeries3.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                uplinkSeries3.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,2));//- packetList.get(0).getTime(), 2));
                uplinkSeries3.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
            }
            else
            {
                downlinkSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                downlinkSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,1));//- packetList.get(0).getTime(), 1));
                downlinkSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                
                downlinkSeries2.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                downlinkSeries2.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,1));//- packetList.get(0).getTime(), 1));
                downlinkSeries2.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                
                downlinkSeries3.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
                downlinkSeries3.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,1));//- packetList.get(0).getTime(), 1));
                downlinkSeries3.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));//- packetList.get(0).getTime(), 0));
            }
        }

        packetChart.getData().add(uplinkSeries);
        packetChart.getData().add(downlinkSeries);
        
        packetChart2.getData().add(uplinkSeries2);
        packetChart2.getData().add(downlinkSeries2);
        
        packetChart3.getData().add(uplinkSeries3);
        packetChart3.getData().add(downlinkSeries3);
        
        // Populates the packet detail list
        packetTable.getItems().setAll(packetList);
        // TODO: put Property Factories in control instead of FXML
        /*
        timeCol.setCellFactory(new PropertyValueFactory<Packet, Long>("time"));
        lengthCol.setCellFactory(new PropertyValueFactory<Packet, Integer>("length"));
        sourceCol.setCellFactory(new PropertyValueFactory<Packet, String>("source"));
        destinationCol.setCellFactory(new PropertyValueFactory<Packet, String>("destination"));
        */
    }
}
