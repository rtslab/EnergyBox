package energybox;

import energybox.properties.device.PropertiesDevice3G;
import energybox.properties.network.Properties3G;
import java.util.HashMap;
import javafx.collections.ObservableList;

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
        packetList = sortUplinkDownlink(packetList, sourceIP);
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
    
    public HashMap modelStates()
    {
        long deltaDownlink, deltaUplink, deltaT;
        long previousTimeUplink =  packetList.get(0).getTime(), 
                previousTimeDownlink = packetList.get(0).getTime(), 
                previousTime = packetList.get(0).getTime();
        State state = State.DCH;
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
                        System.out.println("Demote at:"+(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME())+" to "+ state);
                        stateMap.put(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state);
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
                        System.out.println("Demote at:"+(previousTime +networkProperties.getDCH_FACH_INACTIVITY_TIME())+" to "+ state);
                        stateMap.put(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME(), state);
                        state = State.IDLE;
                        System.out.println("Demote at:"+((previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME() + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME()))+" to "+ state);
                        stateMap.put(previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME() + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME(), state);
                        break;
                    }
                    // DCH to FACH
                    if (deltaT > networkProperties.getDCH_FACH_INACTIVITY_TIME())
                    {
                        state = State.FACH;
                        System.out.println("Demote at:"+(previousTime +networkProperties.getDCH_FACH_INACTIVITY_TIME())+" to "+ state);
                        stateMap.put(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME(), state);
                    }
                }
                break;
            }
        }
        
        return stateMap;
    }
    
}
