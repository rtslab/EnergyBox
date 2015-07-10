package se.liu.rtslab.energybox;

import javafx.collections.ObservableList;

import java.util.HashMap;
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
