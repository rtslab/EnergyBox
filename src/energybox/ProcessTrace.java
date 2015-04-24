package energybox;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public interface ProcessTrace extends Runnable {
    ObservableList<Packet> getPacketList();

    String getCriteria();

    HashMap<String, Integer> getAddressOccurrence();

    String getSourceIP();

    void notifyObservers();

    void addObserver(ProgressObserver observer);

    void removeObserver(ProgressObserver observer);

    List<String> getErrorMessages();

    boolean hasErrors();

    void setIp(String ip);
}
