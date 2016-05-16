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

    /**
     * Convenience factory class for creating an instance of appropriate class, according to the host OS.
     */
    class Factory {
        public static ProcessTrace build(String tracePath, UpdatesController controllerUpdater, String OS) {
            if (OS=="Windows") {
                return new ProcessTraceLibpcap(tracePath, controllerUpdater);
            }
            else {
                return new ProcessTraceTshark(tracePath, controllerUpdater);
            }
        }

    }
}
