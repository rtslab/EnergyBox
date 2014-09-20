package energybox.properties.network;

import java.util.Properties;
import javafx.beans.property.SimpleDoubleProperty;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class PropertiesWifi extends Network
{
    public SimpleDoubleProperty PSM_TO_CAM_THRESHOLD = new SimpleDoubleProperty(1.0);
    public SimpleDoubleProperty CAM_PSM_INACTIVITY_TIME = new SimpleDoubleProperty(200.0);
    public SimpleDoubleProperty CAM_TIME_WIMDOW = new SimpleDoubleProperty(50.0);
    public SimpleDoubleProperty WINDOW_DATA_RATE_THRESHOLD = new SimpleDoubleProperty(3000.0);
    
    public PropertiesWifi(Properties properties)
    {
        PSM_TO_CAM_THRESHOLD = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("PSM_TO_CAM_THRESHOLD")));
        CAM_PSM_INACTIVITY_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("CAM_PSM_INACTIVITY_TIME"))*1000);
        CAM_TIME_WIMDOW = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("CAM_TIME_WIMDOW"))*1000);
        WINDOW_DATA_RATE_THRESHOLD = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("WINDOW_DATA_RATE_THRESHOLD")));
    }
    
    // GETTERS
    public double getPSM_TO_CAM_THRESHOLD() {return PSM_TO_CAM_THRESHOLD.get();}
    public double getCAM_PSM_INACTIVITY_TIME() {return CAM_PSM_INACTIVITY_TIME.get();} 
    public double getCAM_TIME_WIMDOW() {return CAM_TIME_WIMDOW.get();}
    public double getWINDOW_DATA_RATE_THRESHOLD() {return WINDOW_DATA_RATE_THRESHOLD.get();}
    
    // SETTERS
    public void setPSM_TO_CAM_THRESHOLD(double fName) {PSM_TO_CAM_THRESHOLD.set(fName);}
    public void setCAM_PSM_INACTIVITY_TIME(double fName) {CAM_PSM_INACTIVITY_TIME.set(fName);}
    public void setCAM_TIME_WIMDOW(double fName) {CAM_TIME_WIMDOW.set(fName);}
    public void setWINDOW_DATA_RATE_THRESHOLD(double fName) {WINDOW_DATA_RATE_THRESHOLD.set(fName);}
}