package energybox;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class NetworkProperties
{
    Properties properties = new Properties();
    
    public NetworkProperties(Reader reader)
    {
        try
        {
            properties.load(reader);
        }
        catch (IOException e){ e.printStackTrace(); }
    }
}
