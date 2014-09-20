package energybox.properties.device;

import java.util.Properties;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class PropertiesDevice3G extends Device
{
    public SimpleDoubleProperty POWER_IN_IDLE = new SimpleDoubleProperty(0.2);
    public SimpleDoubleProperty POWER_IN_FACH = new SimpleDoubleProperty(0.5);
    public SimpleDoubleProperty POWER_IN_DCH = new SimpleDoubleProperty(1.3);
    
    public PropertiesDevice3G(Properties properties)
    {
        POWER_IN_IDLE = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("POWER_IN_IDLE")));
        POWER_IN_FACH = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("POWER_IN_FACH")));
        POWER_IN_DCH = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("POWER_IN_DCH")));
    }
    
    // GETTERS
    public double getPOWER_IN_IDLE() {return POWER_IN_IDLE.get();}
    public double getPOWER_IN_FACH() {return POWER_IN_FACH.get();} 
    public double getPOWER_IN_DCH() {return POWER_IN_DCH.get();}
    
    // SETTERS
    public void setPOWER_IN_IDLE(double fName) {POWER_IN_IDLE.set(fName);}
    public void setPOWER_IN_FACH(double fName) {POWER_IN_FACH.set(fName);}
    public void setPOWER_IN_DCH(double fName) {POWER_IN_DCH.set(fName);}
}
