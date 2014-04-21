package energybox;

import energybox.properties.device.PropertiesDevice3G;
import energybox.properties.network.Properties3G;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    enum State { IDLE, FACH, DCH }
    
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
            {
                // The time difference may be so little that rounding it in milliseconds
                // results in the same timestamp
                if (packetList.get(i).getTime()-packetList.get(i-1).getTime() != 0)
                    throughput = packetList.get(i).getLength()/(packetList.get(i).getTime()-packetList.get(i-1).getTime());
                else
                    throughput = packetList.get(i).getLength();

                uplinkSeries.getData().add(new XYChart.Data(packetList.get(i).getTime(), throughput));
            }
        }
        return uplinkSeries;
    }
    public XYChart.Series<Long, Long> getDownlinkThroughput()
    {
        XYChart.Series<Long, Long> downlinkSeries = new XYChart.Series();
        downlinkSeries.setName("Downlink");
        long throughput = 0;
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTime(), 0));
        for (int i = 1; i < packetList.size(); i++)
        {
            if (!packetList.get(i).getUplink())
            {
                // The time difference may be so little that rounding it in milliseconds
                // results in the same timestamp
                if (packetList.get(i).getTime()-packetList.get(i-1).getTime() != 0)
                    throughput = packetList.get(i).getLength()/(packetList.get(i).getTime()-packetList.get(i-1).getTime());
                else
                    throughput = packetList.get(i).getLength();

                downlinkSeries.getData().add(new XYChart.Data(packetList.get(i).getTime(), throughput));
            }
        }
        return downlinkSeries;
    }
    
    // Formula for calculating buffer empty time depending on buffer occupancy
    // (Downlink is modeled with a constant occupancy - the value in networkProperties)
    public double timeToEmptyUplink(int buffer) { return networkProperties.getUPLINK_BUFFER_EMPTY_TIME() * buffer + 10; }
    
    public HashMap modelStates()
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
                previousTime = packetList.get(0).getTime(), // might wanna switch to packetList.get(i-1).getTime()
                timeToEmptyUplink = 0;
                // timeToEmptyDownlink = networkProperties.getDOWNLINK_BUFFER_EMPTY_TIME;
        State state = State.IDLE; // State enumeration
        HashMap stateMap = new HashMap();
        
        // Start off in IDLE at time 0
        System.out.println("Start time:"+packetList.get(0).getTime()+" state "+ state);
        stateMap.put(packetList.get(0).getTime(), state);
        for (int i = 0; i < packetList.size(); i++) 
        {
            // Update deltas and previous times (uplink and downlink seperately
            // for buffer calculations)
            deltaT = packetList.get(i).getTime() - previousTime;
            previousTime = packetList.get(i).getTime();
            
            if (packetList.get(i).getUplink())
            {
                deltaUplink = packetList.get(i).getTime() - previousTimeUplink;
                previousTimeUplink = packetList.get(i).getTime();
            }
            else
            {
                deltaDownlink = packetList.get(i).getTime() - previousTimeDownlink;
                previousTimeDownlink = packetList.get(i).getTime();
            }
            
            // DEMOTIONS
            switch (state)
            {   
                case FACH:
                {
                    // FACH to IDLE
                    if (deltaT > networkProperties.getFACH_IDLE_INACTIVITY_TIME())
                    {
                        state = State.IDLE;
                        System.out.println("Demote at: "+(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME())+" to "+ state);
                        stateMap.put(previousTime + 
                                networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state);
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
                        state = State.FACH;
                        //System.out.println("Demote at:"+(previousTime +networkProperties.getDCH_FACH_INACTIVITY_TIME())+" to "+ state);
                        stateMap.put(previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME(), state);
                        state = State.IDLE;
                        System.out.println("Demote at: "+((previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME() + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME()))+" to "+ state);
                        stateMap.put(previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME() + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME(), state);
                        break; // so that it wouldn't demote to FACH twice
                    }
                    // DCH to FACH
                    if (deltaT > networkProperties.getDCH_FACH_INACTIVITY_TIME())
                    {
                        state = State.FACH;
                        System.out.println("Demote at:"+(previousTime +
                                networkProperties.getDCH_FACH_INACTIVITY_TIME())+" to "+ state);
                        stateMap.put(previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME(), state);
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
                            state = State.DCH;
                            System.out.println("Promote at:"+(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_DCH_TRANSITION_TIME())+" to "+ state);
                            stateMap.put(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state);
                        }
                        else
                        {
                            state = State.FACH;
                            System.out.println("Promote at:"+(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_FACH_TRANSITION_TIME())+" to "+ state);
                            stateMap.put(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_FACH_TRANSITION_TIME(), state);
                            bufferIDLEtoFACHuplink += packetList.get(i).getLength();
                        }
                    }
                    // Downlink packets
                    else
                    {
                        if (packetList.get(i).getLength() > networkProperties.getDOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH())
                        {
                            state = State.DCH;
                            System.out.println("Promote at:"+(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_DCH_TRANSITION_TIME())+" to "+ state);
                            stateMap.put(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state);
                        }
                        else
                        {
                            state = State.FACH;
                            System.out.println("Promote at:"+(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_FACH_TRANSITION_TIME())+" to "+ state);
                            stateMap.put(packetList.get(i).getTime() + 
                                    networkProperties.getIDLE_TO_FACH_TRANSITION_TIME(), state);
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
                    // TODO : correct rounding or switching all time variables
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
                        bufferFACHtoDCHuplink += packetList.get(i).getLength();
                        if (bufferFACHtoDCHuplink > networkProperties.getUPLINK_BUFFER_FACH_TO_DCH())
                        {
                            state = State.DCH;
                            bufferFACHtoDCHuplink = 0;
                        }
                        // TODO: Check weather this is accurate, since the original
                        // code had just the time of the packet, but it feels like
                        // it should be packetTime+trasitionTime
                        stateMap.put(packetList.get(i).getTime(), state);
                    }
                    // Downlink packets
                    else
                    {
                        bufferFACHtoDCHdownlink += packetList.get(i).getLength();
                        if (bufferFACHtoDCHdownlink > networkProperties.getDOWNLINK_BUFFER_FACH_TO_DCH())
                        {
                            state = State.DCH;
                            bufferFACHtoDCHdownlink = 0;
                        }
                        // TODO: Check weather this is accurate, since the original
                        // code had just the time of the packet, but it feels like
                        // it should be packetTime+trasitionTime
                        stateMap.put(packetList.get(i).getTime(), state);
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
            
        }
        
        return stateMap;
    }
    
}
