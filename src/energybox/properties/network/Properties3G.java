package energybox.properties.network;

import java.util.Properties;
import javafx.beans.property.SimpleDoubleProperty;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class Properties3G extends Network
{ 
    // The default values are taken from the TelianSonera configuration
    SimpleDoubleProperty DCH_FACH_INACTIVITY_TIME = new SimpleDoubleProperty(4.1);
    SimpleDoubleProperty FACH_IDLE_INACTIVITY_TIME = new SimpleDoubleProperty(5.6);
    SimpleDoubleProperty DCH_LOW_ACTIVITY_TIME = new SimpleDoubleProperty(4);
    SimpleDoubleProperty DATA_THRESHOLD = new SimpleDoubleProperty(1000);

    SimpleDoubleProperty UPLINK_BUFFER_IDLE_TO_FACH_OR_DCH = new SimpleDoubleProperty(1000);
    SimpleDoubleProperty DOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH = new SimpleDoubleProperty(515);
    SimpleDoubleProperty UPLINK_BUFFER_FACH_TO_DCH = new SimpleDoubleProperty(294);
    SimpleDoubleProperty DOWNLINK_BUFFER_FACH_TO_DCH = new SimpleDoubleProperty(515);
    SimpleDoubleProperty UPLINK_BUFFER_EMPTY_TIME = new SimpleDoubleProperty(1.2);
    SimpleDoubleProperty DOWNLINK_BUFFER_EMPTY_TIME = new SimpleDoubleProperty(10.0);

    SimpleDoubleProperty IDLE_TO_FACH_TRANSITION_TIME = new SimpleDoubleProperty(0.43);
    SimpleDoubleProperty IDLE_TO_DCH_TRANSITION_TIME = new SimpleDoubleProperty(1.7);
    SimpleDoubleProperty FACH_TO_DCH_TRANSITION_TIME = new SimpleDoubleProperty(0.65);
    SimpleDoubleProperty DCH_TO_FACH_TRANSITION_TIME = new SimpleDoubleProperty(0.7);
    SimpleDoubleProperty FACH_TO_IDLE_TRANSITION_TIME = new SimpleDoubleProperty(0.3);
    
    public Properties3G(Properties properties)
    {
            DCH_FACH_INACTIVITY_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("DCH_FACH_INACTIVITY_TIME")));
            FACH_IDLE_INACTIVITY_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("FACH_IDLE_INACTIVITY_TIME")));
            DCH_LOW_ACTIVITY_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("DCH_LOW_ACTIVITY_TIME")));
            DATA_THRESHOLD = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("DATA_THRESHOLD")));
            
            UPLINK_BUFFER_IDLE_TO_FACH_OR_DCH = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("UPLINK_BUFFER_IDLE_TO_FACH_OR_DCH")));
            DOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("DOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH")));
            UPLINK_BUFFER_FACH_TO_DCH = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("UPLINK_BUFFER_FACH_TO_DCH")));
            DOWNLINK_BUFFER_FACH_TO_DCH = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("DOWNLINK_BUFFER_FACH_TO_DCH")));
            UPLINK_BUFFER_EMPTY_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("UPLINK_BUFFER_EMPTY_TIME")));
            DOWNLINK_BUFFER_EMPTY_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("DOWNLINK_BUFFER_EMPTY_TIME")));
            
            IDLE_TO_FACH_TRANSITION_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("IDLE_TO_FACH_TRANSITION_TIME")));
            IDLE_TO_DCH_TRANSITION_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("IDLE_TO_DCH_TRANSITION_TIME")));
            FACH_TO_DCH_TRANSITION_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("FACH_TO_DCH_TRANSITION_TIME")));
            DCH_TO_FACH_TRANSITION_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("DCH_TO_FACH_TRANSITION_TIME")));
            FACH_TO_IDLE_TRANSITION_TIME = new SimpleDoubleProperty(Double.parseDouble(properties.getProperty("FACH_TO_IDLE_TRANSITION_TIME")));
    }
    
    // GETTERS
    public double getDCH_FACH_INACTIVITY_TIME() {return DCH_FACH_INACTIVITY_TIME.get();}
    public double getFACH_IDLE_INACTIVITY_TIME() {return FACH_IDLE_INACTIVITY_TIME.get();} 
    public double getDCH_LOW_ACTIVITY_TIME() {return DCH_LOW_ACTIVITY_TIME.get();} 
    public double getDATA_THRESHOLD() {return DATA_THRESHOLD.get();} 
    
    public double getUPLINK_BUFFER_IDLE_TO_FACH_OR_DCH() {return UPLINK_BUFFER_IDLE_TO_FACH_OR_DCH.get();} 
    public double getDOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH() {return DOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH.get();} 
    public double getUPLINK_BUFFER_FACH_TO_DCH() {return UPLINK_BUFFER_FACH_TO_DCH.get();}
    public double getDOWNLINK_BUFFER_FACH_TO_DCH() {return DOWNLINK_BUFFER_FACH_TO_DCH.get();}
    public double getUPLINK_BUFFER_EMPTY_TIME() {return UPLINK_BUFFER_EMPTY_TIME.get();}
    public double getDOWNLINK_BUFFER_EMPTY_TIME() {return DOWNLINK_BUFFER_EMPTY_TIME.get();}
    
    public double getIDLE_TO_FACH_TRANSITION_TIME() {return IDLE_TO_FACH_TRANSITION_TIME.get();}
    public double getIDLE_TO_DCH_TRANSITION_TIME() {return IDLE_TO_DCH_TRANSITION_TIME.get();}
    public double getFACH_TO_DCH_TRANSITION_TIME() {return FACH_TO_DCH_TRANSITION_TIME.get();}
    public double getDCH_TO_FACH_TRANSITION_TIME() {return DCH_TO_FACH_TRANSITION_TIME.get();}
    public double getFACH_TO_IDLE_TRANSITION_TIME() {return FACH_TO_IDLE_TRANSITION_TIME.get();}
    
    // SETTERS
    public void setDCH_FACH_INACTIVITY_TIME(double fName) {DCH_FACH_INACTIVITY_TIME.set(fName);}
    public void setFACH_IDLE_INACTIVITY_TIME(double fName) {FACH_IDLE_INACTIVITY_TIME.set(fName);}
    public void setDCH_LOW_ACTIVITY_TIME(double fName) {DCH_LOW_ACTIVITY_TIME.set(fName);}
    public void setDATA_THRESHOLD(double fName) {DATA_THRESHOLD.set(fName);}
    
    public void setUPLINK_BUFFER_IDLE_TO_FACH_OR_DCH(double fName) {UPLINK_BUFFER_IDLE_TO_FACH_OR_DCH.set(fName);}
    public void setDOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH(double fName) {DOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH.set(fName);}
    public void setUPLINK_BUFFER_FACH_TO_DCH(double fName) {UPLINK_BUFFER_FACH_TO_DCH.set(fName);}
    public void setDOWNLINK_BUFFER_FACH_TO_DCH(double fName) {DOWNLINK_BUFFER_FACH_TO_DCH.set(fName);}
    public void setUPLINK_BUFFER_EMPTY_TIME(double fName) {UPLINK_BUFFER_EMPTY_TIME.set(fName);}
    public void setDOWNLINK_BUFFER_EMPTY_TIME(double fName) {DOWNLINK_BUFFER_EMPTY_TIME.set(fName);}
    
    public void setIDLE_TO_FACH_TRANSITION_TIME(double fName) {IDLE_TO_FACH_TRANSITION_TIME.set(fName);}
    public void setIDLE_TO_DCH_TRANSITION_TIME(double fName) {IDLE_TO_DCH_TRANSITION_TIME.set(fName);}
    public void setFACH_TO_DCH_TRANSITION_TIME(double fName) {FACH_TO_DCH_TRANSITION_TIME.set(fName);}
    public void setDCH_TO_FACH_TRANSITION_TIME(double fName) {DCH_TO_FACH_TRANSITION_TIME.set(fName);}
    public void setFACH_TO_IDLE_TRANSITION_TIME(double fName) {FACH_TO_IDLE_TRANSITION_TIME.set(fName);}
}
