package energybox;

import energybox.properties.device.PropertiesDevice3G;
import energybox.properties.network.Properties3G;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class Engine3G
{
    ObservableList<Packet> packetList; 
    Properties3G networkProperties; 
    PropertiesDevice3G deviceProperties;
    enum State 
    { 
        IDLE(0), FACH(1), DCH(2);
        private final int value;
        private State(int value){this.value = value;}
        public int getValue() { return this.value; }
    }
    XYChart.Series<Long, Integer> stateSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> uplinkPacketSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> downlinkPacketSeries = new XYChart.Series();
    
    public Engine3G(ObservableList<Packet> packetList,
            String sourceIP,
            Properties3G networkProperties, 
            PropertiesDevice3G deviceProperties)
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
        long throughput = 0;
        uplinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTime(), 0));
        for (int i = 1; i < packetList.size(); i++)
        {
            if (packetList.get(i).getUplink())
                uplinkSeries.getData().add(new XYChart.Data(packetList.get(i).getTime(), packetList.get(i).getLength()));   
        }
        return uplinkSeries;
    }
    
    public XYChart.Series<Long, Long> getDownlinkThroughput()
    {
        XYChart.Series<Long, Long> downlinkSeries = new XYChart.Series();
        downlinkSeries.setName("Downlink");
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTime(), 0));
        for (int i = 1; i < packetList.size(); i++)
        {
            if (!packetList.get(i).getUplink())
                downlinkSeries.getData().add(new XYChart.Data(packetList.get(i).getTime(), packetList.get(i).getLength()));
        }
        return downlinkSeries;
    }
    
    // Formula for calculating buffer empty time depending on buffer occupancy
    // (Downlink is modeled with a constant occupancy - the value in networkProperties)
    public double timeToEmptyUplink(int buffer) { return networkProperties.getUPLINK_BUFFER_EMPTY_TIME() * buffer + 10; }
    
    // GETTERS
    public XYChart.Series<Long, Integer> getUplinkPackets(){ return uplinkPacketSeries; }
    public XYChart.Series<Long, Integer> getDownlinkPackets(){ return downlinkPacketSeries; }
    public XYChart.Series<Long, Integer> getStates(){ return stateSeries; }
    
    public XYChart.Series<Long, Integer> modelStates()
    {
        // Buffer control variables
        int bufferIDLEtoFACHuplink = 0, bufferIDLEtoFACHdownlink = 0,
            bufferFACHtoDCHuplink = 0, bufferFACHtoDCHdownlink = 0;
        
        // Timer variables
        long deltaDownlink = 0, 
                deltaUplink = 0, 
                deltaT = 0,
                previousTimeUplink =  packetList.get(0).getTime(), 
                previousTimeDownlink = packetList.get(0).getTime(), 
                previousTime = packetList.get(0).getTime(), // might wanna replace the variable with packetList.get(i-1).getTime()
                timeToEmptyUplink = 0;
                // timeToEmptyDownlink = networkProperties.getDOWNLINK_BUFFER_EMPTY_TIME;
        State state = State.IDLE; // State enumeration
        // Start off in IDLE at time 0
        System.out.println("Start time:"+packetList.get(0).getTime()+" state "+ state.getValue());
        // Packet list points
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
            
            // Update deltas and previous times (uplink and downlink seperately
            // for buffer calculations)
            deltaT = packetList.get(i).getTime() - previousTime;
            
            if (packetList.get(i).getUplink())
                deltaUplink = packetList.get(i).getTime() - previousTimeUplink;
            else
                deltaDownlink = packetList.get(i).getTime() - previousTimeDownlink;
            
            // DEMOTIONS
            switch (state)
            {   
                case FACH:
                {
                    // FACH to IDLE
                    if (deltaT > networkProperties.getFACH_IDLE_INACTIVITY_TIME())
                    {
                        statePoint(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
                        state = State.IDLE;
                        System.out.println("Demote at: "+(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME())+" to "+ state.getValue());
                        statePoint(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
                    }
                }
                break;

                case DCH:
                {
                    // DCH to FACH to IDLE due to deltaT being higher than
                    // both timers combined.
                    if (deltaT > networkProperties.getDCH_FACH_INACTIVITY_TIME() + 
                            networkProperties.getFACH_IDLE_INACTIVITY_TIME())
                    {
                        statePoint(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
                        state = State.FACH;
                        statePoint(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
                        statePoint(previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                                networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
                        state = State.IDLE;
                        System.out.println("Demote at: "+((previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME() + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME()))+" to "+ state.getValue());
                        statePoint(previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                                networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
                        break; // so that it wouldn't demote to FACH twice
                    }
                    // DCH to FACH
                    if (deltaT > networkProperties.getDCH_FACH_INACTIVITY_TIME())
                    {
                        statePoint(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
                        state = State.FACH;
                        System.out.println("Demote at:"+(previousTime +
                                networkProperties.getDCH_FACH_INACTIVITY_TIME())+" to "+ state.getValue());
                        statePoint(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
                    }
                }
                break;
            }
            // PROMOTIONS
            switch (state)
            {
                case IDLE:
                {
                    // Uplink packets
                    if (packetList.get(i).getUplink())
                    {
                        // If the packet is larger than 
                        if (packetList.get(i).getLength() > networkProperties.getUPLINK_BUFFER_IDLE_TO_FACH_OR_DCH())
                        {
                            statePoint(packetList.get(i).getTime() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                            state = State.DCH;
                            System.out.println("Promote at:"+(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_DCH_TRANSITION_TIME())+" to "+ state.getValue());
                            statePoint(packetList.get(i).getTime() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                        }
                        else
                        {
                            statePoint(packetList.get(i).getTime() + networkProperties.getIDLE_TO_FACH_TRANSITION_TIME(), state.getValue());
                            state = State.FACH;
                            System.out.println("Promote at:"+(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_FACH_TRANSITION_TIME())+" to "+ state.getValue());
                            statePoint(packetList.get(i).getTime() + networkProperties.getIDLE_TO_FACH_TRANSITION_TIME(), state.getValue());
                            bufferIDLEtoFACHuplink += packetList.get(i).getLength();
                        }
                    }
                    // Downlink packets
                    else
                    {
                        if (packetList.get(i).getLength() > networkProperties.getDOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH())
                        {
                            statePoint(packetList.get(i).getTime() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                            state = State.DCH;
                            System.out.println("Promote at:"+(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_DCH_TRANSITION_TIME())+" to "+ state.getValue());
                            statePoint(packetList.get(i).getTime() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                        }
                        else
                        {
                            statePoint(packetList.get(i).getTime() + networkProperties.getIDLE_TO_FACH_TRANSITION_TIME(), state.getValue());
                            state = State.FACH;
                            System.out.println("Promote at:"+(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_FACH_TRANSITION_TIME())+" to "+ state.getValue());
                            statePoint(packetList.get(i).getTime() + networkProperties.getIDLE_TO_FACH_TRANSITION_TIME(), state.getValue());
                            bufferIDLEtoFACHdownlink += packetList.get(i).getLength();
                        }
                    }
                }
                break;
                    
                case FACH:
                {
                    bufferIDLEtoFACHdownlink = 0; // ?
                    bufferIDLEtoFACHuplink = 0; // ?
                    // calculates time but ROUNDS DOWN TO WHOLE MILLISECONDS
                    // TODO : correct rounding or switch all time variables
                    // to double so you wouldn't have to cast double to long
                    timeToEmptyUplink = (long)timeToEmptyUplink(bufferFACHtoDCHuplink);
                    // If timeToEmptyUplink or DOWNLINK_BUFFER_EMPTY_TIME has passed, clear the RLCBuffer
                    if (deltaUplink > timeToEmptyUplink)
                        bufferFACHtoDCHuplink = 0;
                    if (deltaDownlink > networkProperties.getDOWNLINK_BUFFER_EMPTY_TIME())
                        bufferFACHtoDCHdownlink = 0;
                    
                    // Uplink packets
                    if (packetList.get(i).getUplink())
                    {
                        statePoint((double)packetList.get(i).getTime(), state.getValue());
                        bufferFACHtoDCHuplink += packetList.get(i).getLength();
                        if (bufferFACHtoDCHuplink > networkProperties.getUPLINK_BUFFER_FACH_TO_DCH())
                        {
                            state = State.DCH;
                            bufferFACHtoDCHuplink = 0;
                        }
                        // TODO: Check weather this is accurate, since the original
                        // code had just the time of the packet, but it feels like
                        // it should be packetTime+trasitionTime
                        //stateMap.put(packetList.get(i).getTime(), state);
                        statePoint((double)packetList.get(i).getTime(), state.getValue());
                    }
                    // Downlink packets
                    else
                    {
                        statePoint((double)packetList.get(i).getTime(), state.getValue());
                        bufferFACHtoDCHdownlink += packetList.get(i).getLength();
                        if (bufferFACHtoDCHdownlink > networkProperties.getDOWNLINK_BUFFER_FACH_TO_DCH())
                        {
                            state = State.DCH;
                            bufferFACHtoDCHdownlink = 0;
                        }
                        // TODO: Check weather this is accurate, since the original
                        // code had just the time of the packet, but it feels like
                        // it should be packetTime+trasitionTime
                        //stateMap.put(packetList.get(i).getTime(), state);
                        statePoint((double)packetList.get(i).getTime(), state.getValue());
                    }
                }
                break;
                    
                case DCH:
                {
                    bufferIDLEtoFACHuplink = 0;
                    bufferIDLEtoFACHdownlink = 0;
                    bufferFACHtoDCHuplink = 0;
                    bufferFACHtoDCHdownlink = 0;
                }
                break;
            }
            // Save timestamps for the next loop
            previousTime = packetList.get(i).getTime();            
            if (packetList.get(i).getUplink())            
                previousTimeUplink = packetList.get(i).getTime();            
            else            
                previousTimeDownlink = packetList.get(i).getTime();
        }        
        return stateSeries;
    }
    private void statePoint(Double time, int state)
    {
        stateSeries.getData().add(new XYChart.Data(time, state));
    }
}
