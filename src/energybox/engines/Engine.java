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
    
    // RESULTS VARIABLES
    protected XYChart.Series<Double, Integer> stateSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> uplinkPacketSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> downlinkPacketSeries = new XYChart.Series();
    XYChart.Series<Double, Long> uplinkSeries = new XYChart.Series();
    XYChart.Series<Double, Long> downlinkSeries = new XYChart.Series();
    protected ObservableList<PieChart.Data> linkDistrData = 
            FXCollections.observableArrayList(new ArrayList());
    protected int uplinkPacketCount = 0;
    protected ObservableList<PieChart.Data> stateTimeData = 
            FXCollections.observableArrayList(new ArrayList());
    protected ObservableList<StatisticsEntry> statisticsList = FXCollections.observableList(new ArrayList());
    protected ObservableList<StatisticsEntry> distrStatisticsList = FXCollections.observableList(new ArrayList());
    protected Double power = Double.valueOf(0);
    
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
    public XYChart.Series<Double, Long> getUplinkThroughput(double chunkSize)
    {
        uplinkSeries.getData().clear();
        uplinkSeries.setName("Uplink");
        Long throughput = Long.valueOf(0); 
        double currentChunk = chunkSize;
        
        uplinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTime(), Long.valueOf(0)));
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
                    throughput = Long.valueOf(0);
                    currentChunk += chunkSize;
                }
            }
            else
            {
                if (packetList.get(i).getTime() < currentChunk) i++;
                else 
                {
                    uplinkSeries.getData().add(new XYChart.Data(currentChunk, throughput));
                    throughput = Long.valueOf(0);
                    currentChunk += chunkSize;
                }
            }
        }
        uplinkSeries.getData().add(new XYChart.Data(packetList.get(packetList.size()-1).getTime(), throughput));
        return uplinkSeries;
    }
    
    // DOWNLOING THROUGHPUT CALCULATION
    public XYChart.Series<Double, Long> getDownlinkThroughput(double chunkSize)
    {
        downlinkSeries.getData().clear();
        downlinkSeries.setName("Downlink");
        Long throughput = Long.valueOf(0);
        double currentChunk = chunkSize;
        
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTime(), Long.valueOf(0)));
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
                    throughput = Long.valueOf(0);
                    currentChunk += chunkSize;
                }
            }
            else 
            {
                if (packetList.get(i).getTime() < currentChunk) i++;
                else 
                {
                    downlinkSeries.getData().add(new XYChart.Data(currentChunk, throughput));
                    throughput = Long.valueOf(0);
                    currentChunk += chunkSize;
                }
            }
        }
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(packetList.size()-1).getTime(), throughput));
        return downlinkSeries;
    }
    
    // MAIN MODELING METHOD
    // Loops through the packetList once and calculates:
    // -State chart -Packet chart -Distribution pie chart.
    // Implemented in every Engine type seperately.
    public abstract XYChart.Series<Double, Integer> modelStates();
    
    // Calculates power based on the power graph.
    // Implemented in every Engine type seperately.
    public abstract void calculatePower();
    
    private ArrayList<XYChart.Data<Long, Integer>> uplinkChartData = new ArrayList<>();
    private ArrayList<XYChart.Data<Long, Integer>> downlinkChartData = new ArrayList<>();

    public void packetChartEntry(Packet packet)
    {
        ArrayList<XYChart.Data<Long, Integer>> target;
        if (packet.getUplink()) {
            uplinkPacketCount++;
            target = uplinkChartData;
        } else {
            target = downlinkChartData;
        }
        
        double time = packet.getTime();
        
        target.add(new XYChart.Data(time,0));
        target.add(new XYChart.Data(time, packet.getLength()));
        target.add(new XYChart.Data(time,0));
    }
    
    public void updatePacketCharts() {
        uplinkPacketSeries.getData().setAll(uplinkChartData);
        downlinkPacketSeries.getData().setAll(downlinkChartData);
    }
    
    public void drawState(Long time, int state)
    {
        Double tempTime = time.doubleValue()/1000000;
        stateSeries.getData().add(new XYChart.Data(tempTime, state));        
    }
    
    // GETTERS
    public XYChart.Series<Long, Integer> getUplinkPackets(){ return uplinkPacketSeries; }
    public XYChart.Series<Long, Integer> getDownlinkPackets(){ return downlinkPacketSeries; }
    public XYChart.Series<Double, Integer> getPower(){ return stateSeries; }
    public ObservableList<PieChart.Data> getLinkDistroData() { return linkDistrData; }
    public ObservableList<PieChart.Data> getStateTimeData() { return stateTimeData; }
    public String getSourceIP() {return sourceIP;}
    public ObservableList<Packet> getPacketList() {return packetList; }
    public ObservableList<StatisticsEntry> getStatisticsList() {return statisticsList; }
    public ObservableList<StatisticsEntry> getDistrStatisticsList() {return distrStatisticsList; }
    public Double getPowerValue() { return power; }
    // Name: "3G" or "Wifi"
    abstract public String getName();
}
