package energybox;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class Packet
{
    private final SimpleLongProperty time;
    private final SimpleIntegerProperty length;
    private final SimpleStringProperty source;
    private final SimpleStringProperty destination;
    private final SimpleStringProperty protocol;
    private final SimpleBooleanProperty uplink = new SimpleBooleanProperty(false);
    
    public Packet(long time, int length, String source, String destination, String protocol)
    {
        this.time = new SimpleLongProperty(time);
        this.length = new SimpleIntegerProperty(length);
        this.source = new SimpleStringProperty(source);
        this.destination = new SimpleStringProperty(destination);
        this.protocol = new SimpleStringProperty(protocol);
    }
    
    // GETTERS
    public long getTimeInMicros() {return (time.get());}
    public double getTime() {return Double.valueOf(time.get())/1000000;}
    public int getLength() {return length.get();}
    public String getSource() {return source.get();}
    public String getDestination() {return destination.get();}
    public String getProtocol() {return protocol.get();}
    public Boolean getUplink() {return uplink.get();}
    public String getLink()
    {
        if (uplink.get()) return "Uplink";
        else return "Downlink";
    }
    
    // SETTERS
    public void setTime(long fName) {time.set(fName);}
    public void setLength(int fName) {length.set(fName);}
    public void setSource(String fName) {source.set(fName);}
    public void setDestination(String fName) {destination.set(fName);}
    public void setProtocol(String fName) {protocol.set(fName);}
    public void setUplink(Boolean fName) {uplink.set(fName);}
}
