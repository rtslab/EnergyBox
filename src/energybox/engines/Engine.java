package energybox.engines;

import energybox.Packet;
import energybox.StatisticsEntry;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public abstract class Engine
{
    // CONSTRUCTOR VARIABLES
    protected String sourceIP;
    protected ObservableList<Packet> packetList;
    
    // CHART VARIABLES
    protected XYChart.Series<Double, Integer> stateSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> uplinkPacketSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> downlinkPacketSeries = new XYChart.Series();
    protected ObservableList<PieChart.Data> linkDistrData = 
            FXCollections.observableArrayList(new ArrayList());
    protected int uplinkPacketCount = 0;
    protected ObservableList<PieChart.Data> stateTimeData = 
            FXCollections.observableArrayList(new ArrayList());
    protected ObservableList<StatisticsEntry> statisticsList = FXCollections.observableList(new ArrayList());
    protected ObservableList<StatisticsEntry> distrStatisticsList = FXCollections.observableList(new ArrayList());
    
    // PACKET SORTING
    public ObservableList<Packet> sortUplinkDownlink(ObservableList<Packet> packetList, String sourceIP)
    {
        for (int i = 0; i < packetList.size(); i++) 
        {
            if (packetList.get(i).getSource().equals(sourceIP))
                packetList.get(i).setUplink(Boolean.TRUE);
        }
        return packetList;
    }
    
    // UPLINK THROUGHPUT CALCULATION
    public XYChart.Series<Long, Long> getUplinkThroughput(double chunkSize)
    {
        XYChart.Series<Long, Long> uplinkSeries = new XYChart.Series();
        uplinkSeries.setName("Uplink");
        long throughput = 0;
                //chunkSize = packetList.get(packetList.size()-1).getTime()/50, 
        double currentChunk = chunkSize;
        
        uplinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTime(), 0));
        int i = 0;
        while ((currentChunk < packetList.get(packetList.size()-1).getTime()) && (i<packetList.size()))
        {
            if (packetList.get(i).getUplink())
            {
                if (packetList.get(i).getTime() < currentChunk)
                {
                    throughput += packetList.get(i).getLength();
                    i++;
                }
                else
                {
                    uplinkSeries.getData().add(new XYChart.Data(currentChunk, throughput)); 
                    throughput = 0;
                    currentChunk += chunkSize;
                }
            }
            else i++;
        }
        uplinkSeries.getData().add(new XYChart.Data(packetList.get(packetList.size()-1).getTime(), throughput));
        return uplinkSeries;
    }
    
    // DOWNLOING THROUGHPUT CALCULATION
    public XYChart.Series<Long, Long> getDownlinkThroughput(double chunkSize)
    {
        XYChart.Series<Long, Long> downlinkSeries = new XYChart.Series();
        downlinkSeries.setName("Downlink");
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTime(), 0));
        long throughput = 0;
                //chunkSize = packetList.get(packetList.size()-1).getTime()/50, 
        double currentChunk = chunkSize;
        
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTime(), 0));
        int i = 0;
        while ((currentChunk < packetList.get(packetList.size()-1).getTime()) && (i<packetList.size()))
        {
            if (!packetList.get(i).getUplink())
            {
                if (packetList.get(i).getTime() < currentChunk)
                {
                    throughput += packetList.get(i).getLength();
                    i++;
                }
                else
                {
                    downlinkSeries.getData().add(new XYChart.Data(currentChunk, throughput)); 
                    throughput = 0;
                    currentChunk += chunkSize;
                }
            }
            else i++;
        }
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(packetList.size()-1).getTime(), throughput));
        return downlinkSeries;
    }
    
    // MAIN MODELING METHOD
    // Loops through the packetList once and calculates:
    // -State chart -State transitions -Packet chart -Distribution pie chart.
    // Implemented in every Engine type seperately.
    abstract XYChart.Series<Double, Integer> modelStates();
    
    public void packetChartEntry(Packet packet)
    {
        if (packet.getUplink())
            {
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packet.getTime() ,0));
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packet.getTime() , packet.getLength()));
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packet.getTime() ,0));
                uplinkPacketCount++;
            }
            else
            {
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packet.getTime() ,0));
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packet.getTime() , packet.getLength()));
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packet.getTime() ,0));
            }
    }
    
    public void drawState(Long time, int state)
    {
        Double tempTime = time.doubleValue()/1000000;
        stateSeries.getData().add(new XYChart.Data(tempTime, state));        
    }
    
    // GETTERS
    public XYChart.Series<Long, Integer> getUplinkPackets(){ return uplinkPacketSeries; }
    public XYChart.Series<Long, Integer> getDownlinkPackets(){ return downlinkPacketSeries; }
    public XYChart.Series<Double, Integer> getStates(){ return stateSeries; }
    public ObservableList<PieChart.Data> getLinkDistroData() { return linkDistrData; }
    public ObservableList<PieChart.Data> getStateTimeData() { return stateTimeData; }
    public String getSourceIP() {return sourceIP;}
    public ObservableList<Packet> getPacketList() {return packetList; }
    public ObservableList<StatisticsEntry> getStatisticsList() {return statisticsList; }
    public ObservableList<StatisticsEntry> getDistrStatisticsList() {return distrStatisticsList; }
}
