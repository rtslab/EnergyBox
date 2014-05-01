package energybox;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class StatisticsEntry
{
    private final SimpleStringProperty parameter;
    SimpleDoubleProperty value;
    
    public StatisticsEntry(String parameter,double value)
    {
        this.parameter = new SimpleStringProperty(parameter);
        this.value = new SimpleDoubleProperty(value);   
    }
    
    // GETTERS
    public String getParameter() {return parameter.get();}
    public double getValue() {return value.get();}
    
    // SETTERS
    public void setParameter(String fName) {parameter.set(fName);}
    public void setValue(double fName) {value.set(fName);}
}
