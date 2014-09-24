package energybox.properties.device;

import java.util.Properties;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class PropertiesDeviceWifi extends Device
{
    public SimpleDoubleProperty POWER_IN_PSM = new SimpleDoubleProperty(30.0);
    public SimpleDoubleProperty POWER_IN_CAM = new SimpleDoubleProperty(250.0);
    public SimpleDoubleProperty POWER_IN_CAMH = new SimpleDoubleProperty(500.0);
    
    public PropertiesDeviceWifi(Properties properties)
    {
        POWER_IN_PSM = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("POWER_IN_PSM")));
        POWER_IN_CAM = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("POWER_IN_CAM")));
        POWER_IN_CAMH = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("POWER_IN_CAMH")));
    }
    
    // GETTERS
    public double getPOWER_IN_PSM() {return POWER_IN_PSM.get();}
    public double getPOWER_IN_CAM() {return POWER_IN_CAM.get();} 
    public double getPOWER_IN_CAMH() {return POWER_IN_CAMH.get();}
    
    // SETTERS
    public void setPOWER_IN_PSM(double fName) {POWER_IN_PSM.set(fName);}
    public void setPOWER_IN_CAM(double fName) {POWER_IN_CAM.set(fName);}
    public void setPOWER_IN_CAMH(double fName) {POWER_IN_CAMH.set(fName);}
}
