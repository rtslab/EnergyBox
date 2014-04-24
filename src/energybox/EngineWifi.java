package energybox;

import energybox.properties.device.PropertiesDeviceWifi;
import energybox.properties.network.PropertiesWifi;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class EngineWifi
{
    ObservableList<Packet> packetList; 
    PropertiesWifi networkProperties; 
    PropertiesDeviceWifi deviceProperties;
    enum State 
    { 
        PSM(0), CAM(2), CAMH(3);
        private final int value;
        private State(int value){this.value = value;}
        public int getValue() { return this.value; }
    }
    XYChart.Series<Long, Integer> stateSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> uplinkPacketSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> downlinkPacketSeries = new XYChart.Series();
    
    public EngineWifi(ObservableList<Packet> packetList,
            String sourceIP,
            PropertiesWifi networkProperties, 
            PropertiesDeviceWifi deviceProperties)
    {
        this.packetList = packetList;
        this.networkProperties = networkProperties;
        this.deviceProperties = deviceProperties;
        this.packetList = sortUplinkDownlink(packetList, sourceIP);
    }
    
    public ObservableList<Packet> sortUplinkDownlink(ObservableList<Packet> packetList, String sourceIP)
    {
        for (int i = 0; i < packetList.size(); i++) 
        {
            if (packetList.get(i).getSource().equals(sourceIP))
                packetList.get(i).setUplink(Boolean.TRUE);
        }
        return packetList;
    }
    
    // TODO: integrate into modelStates() so that you wouldn't have to loop
    // through the lacketList twice
    public XYChart.Series<Long, Long> getUplinkThroughput()
    {
        XYChart.Series<Long, Long> uplinkSeries = new XYChart.Series();
        uplinkSeries.setName("Uplink");
        long throughput = 0,
                chunkSize = packetList.get(packetList.size()-1).getTime()/50, 
                currentChunk = chunkSize;
        
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
    
    public XYChart.Series<Long, Long> getDownlinkThroughput()
    {
        XYChart.Series<Long, Long> downlinkSeries = new XYChart.Series();
        downlinkSeries.setName("Downlink");
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTime(), 0));
        long throughput = 0,
                chunkSize = packetList.get(packetList.size()-1).getTime()/50, 
                currentChunk = chunkSize;
        
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
    
    public XYChart.Series<Long, Integer> modelStates()
    {
        // Timer variables
        long deltaT = 0;
        long previousTime = packetList.get(0).getTime();
        
        State state = State.CAM;
        
        for (int i = 0; i < packetList.size(); i++) 
        {
            if (packetList.get(i).getUplink())
            {
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,2));
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));
            }
            else
            {
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,1));
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTime() ,0));
            }
            
            deltaT = packetList.get(i).getTime() - previousTime;
            
            // DEMOTIONS
            switch (state)
            {
                case CAM:
                {
                    // If the time between packets is longer than the timeout
                    // demote to PSM
                    if (deltaT > networkProperties.getCAM_PSM_INACTIVITY_TIME())
                    {
                        drawState(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
                        state = State.PSM;
                        System.out.println("Demote at: "+(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME())+" to "+ state.getValue());
                        drawState(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
                    }
                }
                break;
                    
                case CAMH:
                {
                    // TODO: Calculate 
                }
                break;
            }
            
            // PROMOTIONS
            switch (state)
            {
                // In PSM promotion happens whenever a packet is sent
                case PSM:
                {
                    drawState((double)packetList.get(i).getTime(), state.getValue());
                    state = State.CAM;
                    drawState((double)packetList.get(i).getTime(), state.getValue());
                }
                break;
                    
                case CAM:
                {
                    
                }
                break;
            }
            // Save timestamps for the next loop
            previousTime = packetList.get(i).getTime();
        }
        return stateSeries;
    }
    
    private void drawState(Double time, int state)
    {
        stateSeries.getData().add(new XYChart.Data(time, state));
    }
    
    // GETTERS
    public XYChart.Series<Long, Integer> getUplinkPackets(){ return uplinkPacketSeries; }
    public XYChart.Series<Long, Integer> getDownlinkPackets(){ return downlinkPacketSeries; }
    public XYChart.Series<Long, Integer> getStates(){ return stateSeries; }
}
