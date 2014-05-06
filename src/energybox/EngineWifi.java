package energybox;

import energybox.properties.device.PropertiesDeviceWifi;
import energybox.properties.network.PropertiesWifi;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class EngineWifi
{
    String sourceIP;
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
    private class TransitionPair
    {
        private double time;
        private String state;
        public TransitionPair(double time, String state)
        {
            this.time = time;
            this.state = state;
        }
    }
    XYChart.Series<Long, Integer> stateSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> camSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> uplinkPacketSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> downlinkPacketSeries = new XYChart.Series();
    List<TransitionPair> transitions = new ArrayList();
    ObservableList<StatisticsEntry> statisticsList = FXCollections.observableList(new ArrayList());
    
    public EngineWifi(ObservableList<Packet> packetList,
            String sourceIP,
            PropertiesWifi networkProperties, 
            PropertiesDeviceWifi deviceProperties)
    {
        this.packetList = packetList;
        this.networkProperties = networkProperties;
        this.deviceProperties = deviceProperties;
        this.packetList = sortUplinkDownlink(packetList, sourceIP);
        this.sourceIP = sourceIP;
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
                chunkSize = packetList.get(packetList.size()-1).getTimeInMicros()/50, 
                currentChunk = chunkSize;
        
        uplinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTimeInMicros(), 0));
        int i = 0;
        while ((currentChunk < packetList.get(packetList.size()-1).getTimeInMicros()) && (i<packetList.size()))
        {
            if (packetList.get(i).getUplink())
            {
                if (packetList.get(i).getTimeInMicros() < currentChunk)
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
        uplinkSeries.getData().add(new XYChart.Data(packetList.get(packetList.size()-1).getTimeInMicros(), throughput));
        return uplinkSeries;
    }
    
    public XYChart.Series<Long, Long> getDownlinkThroughput()
    {
        XYChart.Series<Long, Long> downlinkSeries = new XYChart.Series();
        downlinkSeries.setName("Downlink");
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTimeInMicros(), 0));
        long throughput = 0,
                chunkSize = packetList.get(packetList.size()-1).getTimeInMicros()/50, 
                currentChunk = chunkSize;
        
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTimeInMicros(), 0));
        int i = 0;
        while ((currentChunk < packetList.get(packetList.size()-1).getTimeInMicros()) && (i<packetList.size()))
        {
            if (!packetList.get(i).getUplink())
            {
                if (packetList.get(i).getTimeInMicros() < currentChunk)
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
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(packetList.size()-1).getTimeInMicros(), throughput));
        return downlinkSeries;
    }
    
    public XYChart.Series<Long, Integer> modelStates()
    {
        // Timer variables
        long deltaT = 0;
        long previousTime = packetList.get(0).getTimeInMicros();
        
        State state = State.CAM;
        transitions.add(new TransitionPair(0, state.toString()));
        
        for (int i = 0; i < packetList.size(); i++) 
        {
            if (packetList.get(i).getUplink())
            {
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,0));
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,2));
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,0));
            }
            else
            {
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,0));
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,1));
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,0));
            }
            
            deltaT = packetList.get(i).getTimeInMicros() - previousTime;
            
            // DEMOTIONS
            switch (state)
            {
                case CAM:
                {
                    // If the time between packets is longer than the timeout
                    // demote to PSM
                    if (deltaT > networkProperties.getCAM_PSM_INACTIVITY_TIME())
                    {
                        camToPsm(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME());
                        drawState(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
                        state = State.PSM;
                        transitions.add(new TransitionPair(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.toString()));
                        //System.out.println("Demote at: "+(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME())+" to "+ state.getValue());
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
                    psmToCam((double)packetList.get(i).getTimeInMicros());
                    drawState((double)packetList.get(i).getTimeInMicros(), state.getValue());
                    state = State.CAM;
                    transitions.add(new TransitionPair((double)packetList.get(i).getTimeInMicros(), state.toString()));
                    drawState((double)packetList.get(i).getTimeInMicros(), state.getValue());
                }
                break;
                    
                case CAM:
                {
                    // TODO
                }
                break;
            }
            // Save timestamps for the next loop
            previousTime = packetList.get(i).getTimeInMicros();
        }
        return stateSeries;
    }
    
    private void drawState(Double time, int state)
    {
        stateSeries.getData().add(new XYChart.Data(time, state));
    }
    
    public void getPower()
    {
        Double power = Double.valueOf(0);
        for (int i = 1; i < transitions.size(); i++) 
        {
            switch(transitions.get(i-1).state)
            {
                case "PSM":
                    power += (transitions.get(i).time - transitions.get(i-1).time) 
                            / 1000 * deviceProperties.getPOWER_IN_PSM();
                    
                case "CAM":
                    power += (transitions.get(i).time - transitions.get(i-1).time) 
                            / 1000 * deviceProperties.getPOWER_IN_CAM();
            }
        }
        // Total power used rounded down to two decimal places
        statisticsList.add(new StatisticsEntry("Total Power Used",((double) Math.round(power * 100) / 100)));
    }
    
    private void psmToCam(Double time)
    {
        camSeries.getData().add(new XYChart.Data(time, 0));
        camSeries.getData().add(new XYChart.Data(time, State.CAM.getValue()));
    }
    
    private void camToPsm(Double time)
    {
        camSeries.getData().add(new XYChart.Data(time, State.CAM.getValue()));
        camSeries.getData().add(new XYChart.Data(time, 0));
    }
    
    // GETTERS
    public XYChart.Series<Long, Integer> getUplinkPackets(){ return uplinkPacketSeries; }
    public XYChart.Series<Long, Integer> getDownlinkPackets(){ return downlinkPacketSeries; }
    public XYChart.Series<Long, Integer> getStates(){ return stateSeries; }
    public XYChart.Series<Long, Integer> getCAM(){ return camSeries; }
}
