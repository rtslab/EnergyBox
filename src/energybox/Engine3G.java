package energybox;

import energybox.properties.device.PropertiesDevice3G;
import energybox.properties.network.Properties3G;
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
    
    
}
