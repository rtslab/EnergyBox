package energybox;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;

public class Packet
{
    private final SimpleLongProperty time;
    private final SimpleIntegerProperty length;
    
    public Packet(long time, int source)
    {
        this.time = new SimpleLongProperty(time);
        this.length = new SimpleIntegerProperty(source);
    }
    
    // GETTERS
    public long getTime() {return time.get();}
    public int getLength() {return length.get();}
    
    // SETTERS
    public void setTime(long fName) {time.set(fName);}
    public void setLength(int fName) {length.set(fName);}   
}
