package energybox;

import energybox.properties.device.PropertiesDevice3G;
import energybox.properties.network.Properties3G;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class Engine3G
{
    enum State 
    { 
        IDLE(0), FACH(1), DCH(3);
        private final int value;
        private State(int value){this.value = value;}
        public int getValue() { return this.value; }
    }
    
    // used for a list of transitions for the total power used
    private class TransitionPair
    {
        private double time;
        private String state;
        private TransitionPair(double time, String state)
        {
            this.time = time;
            this.state = state;
        }
    }
    
    // VARIABLES TAKEN FROM THE CONSTRUCTOR
    String sourceIP;
    ObservableList<Packet> packetList;
    Properties3G networkProperties; 
    PropertiesDevice3G deviceProperties;
    
    // CHART VARIABLES
    XYChart.Series<Long, Integer> stateSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> fachSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> dchSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> uplinkPacketSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> downlinkPacketSeries = new XYChart.Series();
    ObservableList<PieChart.Data> linkDistrData = 
            FXCollections.observableArrayList(new ArrayList());
    int uplinkPacketCount = 0;
    ObservableList<PieChart.Data> stateTimeData = 
            FXCollections.observableArrayList(new ArrayList());
    List<TransitionPair> transitions = new ArrayList();
    ObservableList<StatisticsEntry> statisticsList = FXCollections.observableList(new ArrayList());
    ObservableList<StatisticsEntry> distrStatisticsList = FXCollections.observableList(new ArrayList());
    
    // MAIN CONSTRUCTOR
    public Engine3G(ObservableList<Packet> packetList,
            String sourceIP,
            Properties3G networkProperties, 
            PropertiesDevice3G deviceProperties)
    {
        this.packetList = packetList;
        this.networkProperties = networkProperties;
        this.deviceProperties = deviceProperties;
        this.packetList = sortUplinkDownlink(packetList, sourceIP);
        this.sourceIP = sourceIP;
    }
    
    // PACKET SORTING METHOD
    private ObservableList<Packet> sortUplinkDownlink(ObservableList<Packet> packetList, String sourceIP)
    {
        for (int i = 0; i < packetList.size(); i++) 
        {
            if (packetList.get(i).getSource().equals(sourceIP))
                packetList.get(i).setUplink(Boolean.TRUE);
        }
        return packetList;
    }
    
    // TODO: integrate into modelStates() so that you wouldn't have to loop
    // through the lacketList twice
    public XYChart.Series<Long, Long> getUplinkThroughput()
    {
        XYChart.Series<Long, Long> uplinkSeries = new XYChart.Series();
        uplinkSeries.setName("Uplink");
        long throughput = 0,
                chunkSize = packetList.get(packetList.size()-1).getTimeInMicros()/50, 
                currentChunk = chunkSize;
        
        uplinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTimeInMicros(), 0));
        int i = 0;
        while ((currentChunk < packetList.get(packetList.size()-1).getTimeInMicros()) && (i<packetList.size()))
        {
            if (packetList.get(i).getUplink())
            {
                if (packetList.get(i).getTimeInMicros() < currentChunk)
                {
                    throughput += packetList.get(i).getLength();
                    i++;
                }
                else
                {
                    uplinkSeries.getData().add(new XYChart.Data(currentChunk, throughput)); 
                    throughput = 0;
                    currentChunk += chunkSize;
                }
            }
            else i++;
        }
        uplinkSeries.getData().add(new XYChart.Data(packetList.get(packetList.size()-1).getTimeInMicros(), throughput));
        return uplinkSeries;
    }
    
    public XYChart.Series<Long, Long> getDownlinkThroughput()
    {
        XYChart.Series<Long, Long> downlinkSeries = new XYChart.Series();
        downlinkSeries.setName("Downlink");
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTimeInMicros(), 0));
        long throughput = 0,
                chunkSize = packetList.get(packetList.size()-1).getTimeInMicros()/50, 
                currentChunk = chunkSize;
        
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(0).getTimeInMicros(), 0));
        int i = 0;
        while ((currentChunk < packetList.get(packetList.size()-1).getTimeInMicros()) && (i<packetList.size()))
        {
            if (!packetList.get(i).getUplink())
            {
                if (packetList.get(i).getTimeInMicros() < currentChunk)
                {
                    throughput += packetList.get(i).getLength();
                    i++;
                }
                else
                {
                    downlinkSeries.getData().add(new XYChart.Data(currentChunk, throughput)); 
                    throughput = 0;
                    currentChunk += chunkSize;
                }
            }
            else i++;
        }
        downlinkSeries.getData().add(new XYChart.Data(packetList.get(packetList.size()-1).getTimeInMicros(), throughput));
        return downlinkSeries;
    }
    
    // MAIN MODELING METHOD
    // Loops through the packetList once and calculates:
    // -State chart -State transitions -Packet chart -Distribution pie chart
    public XYChart.Series<Long, Integer> modelStates()
    {
        // Buffer control variables
        int bufferIDLEtoFACHuplink = 0, bufferIDLEtoFACHdownlink = 0,
            bufferFACHtoDCHuplink = 0, bufferFACHtoDCHdownlink = 0;
        // If set to true, implements original EnergyBox graph point calculation
        // using microsecond deviations between some transition points to create
        // a continuous function graph
        boolean oldEB = false;
        // Timer variables
        long deltaDownlink = 0, 
                deltaUplink = 0, 
                deltaT = 0,
                previousTimeUplink =  packetList.get(0).getTimeInMicros(), 
                previousTimeDownlink = packetList.get(0).getTimeInMicros(), 
                previousTime = packetList.get(0).getTimeInMicros(), // might wanna replace the variable with packetList.get(i-1).getTime()
                timeToEmptyUplink = 0;
                // timeToEmptyDownlink = networkProperties.getDOWNLINK_BUFFER_EMPTY_TIME;
        State state = State.IDLE; // State enumeration
         
        // Start off in IDLE at time 0
        transitions.add(new TransitionPair(0, state.toString()));
        // Packet list points
        for (int i = 0; i < packetList.size(); i++) 
        {
            // Populating the packetChart series
            if (packetList.get(i).getUplink())
            {
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,0));
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() , packetList.get(i).getLength()));
                uplinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,0));
                uplinkPacketCount++;
            }
            else
            {
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,0));
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() , packetList.get(i).getLength()));
                downlinkPacketSeries.getData().add(new XYChart.Data(
                        packetList.get(i).getTimeInMicros() ,0));
            }
            
            // Update deltas and previous times (uplink and downlink seperately
            // for buffer calculations)
            deltaT = packetList.get(i).getTimeInMicros() - previousTime;
            
            if (packetList.get(i).getUplink())
                deltaUplink = packetList.get(i).getTimeInMicros() - previousTimeUplink;
            else
                deltaDownlink = packetList.get(i).getTimeInMicros() - previousTimeDownlink;
            
            // DEMOTIONS
            switch (state)
            {   
                case FACH:
                {
                    // FACH to IDLE
                    if (deltaT > networkProperties.getFACH_IDLE_INACTIVITY_TIME())
                    {
                        fachToIdle(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME());
                        drawState(previousTime + (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
                        state = State.IDLE;
                        transitions.add(new TransitionPair(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.toString()));
                        if (oldEB)
                            drawState(previousTime + (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME()+1, state.getValue());
                        else
                            drawState(previousTime + (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
                    }
                }
                break;

                case DCH:
                {
                    // DCH to FACH to IDLE due to deltaT being higher than
                    // both timers combined.
                    if (deltaT > networkProperties.getDCH_FACH_INACTIVITY_TIME() + 
                            networkProperties.getFACH_IDLE_INACTIVITY_TIME())
                    {
                        dchToFach(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME());
                        fachToIdle(previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                                networkProperties.getFACH_IDLE_INACTIVITY_TIME());
                        drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
                        state = State.FACH;
                        transitions.add(new TransitionPair(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.toString()));
                        if (oldEB)
                            drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME()+1, state.getValue());
                        else
                            drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
                        
                        drawState(previousTime + 
                                (long)networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                                (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
                        state = State.IDLE;
                        transitions.add(new TransitionPair(previousTime + 
                                networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                                networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.toString()));
                        if (oldEB)
                            drawState(previousTime + 
                                    (long)networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                                    (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME()+1, state.getValue());
                        else
                            drawState(previousTime + 
                                    (long)networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                                    (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
                        break; // so that it wouldn't demote to FACH twice
                    }
                    // DCH to FACH
                    if (deltaT > networkProperties.getDCH_FACH_INACTIVITY_TIME())
                    {
                        dchToFach(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME());
                        drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
                        state = State.FACH;
                        transitions.add(new TransitionPair(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.toString()));
                        if (oldEB)
                            drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME()+1, state.getValue());
                        else
                            drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
                    }
                }
                break;
            }
            // PROMOTIONS
            switch (state)
            {
                case IDLE:
                {
                    // Uplink packets
                    if (packetList.get(i).getUplink())
                    {
                        // If the packet is larger than 
                        if (packetList.get(i).getLength() > networkProperties.getUPLINK_BUFFER_IDLE_TO_FACH_OR_DCH())
                        {
                            idleToDch(packetList.get(i).getTimeInMicros() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME());
                            if (oldEB)
                                drawState(packetList.get(i).getTimeInMicros() + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME()-1, state.getValue());
                            else
                                drawState(packetList.get(i).getTimeInMicros() + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                            
                            state = State.DCH;
                            transitions.add(new TransitionPair(packetList.get(i).getTimeInMicros() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.toString()));
                            drawState(packetList.get(i).getTimeInMicros() + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                        }
                        else
                        {
                            idleToFach((double)packetList.get(i).getTimeInMicros());
                            // 
                            if (oldEB)
                                drawState(packetList.get(i).getTimeInMicros()-1, state.getValue());
                            else
                                drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                            //drawState(packetList.get(i).getTime() + networkProperties.getIDLE_TO_FACH_TRANSITION_TIME(), state.getValue());
                            state = State.FACH;
                            transitions.add(new TransitionPair(packetList.get(i).getTimeInMicros(), state.toString()));
                            drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                            bufferIDLEtoFACHuplink += packetList.get(i).getLength();
                        }
                    }
                    // Downlink packets
                    else
                    {
                        if (packetList.get(i).getLength() > networkProperties.getDOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH())
                        {
                            idleToDch(packetList.get(i).getTimeInMicros() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME());
                            if (oldEB)
                                drawState(packetList.get(i).getTimeInMicros() + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME()-1, state.getValue());
                            else
                                drawState(packetList.get(i).getTimeInMicros() + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                            state = State.DCH;
                            //transitions.add(new TransitionPair(packetList.get(i).getTimeInMicros() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.toString()));
                            transitions.add(new TransitionPair(packetList.get(i).getTimeInMicros() + 1, state.toString()));
                            drawState(packetList.get(i).getTimeInMicros() + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                        }
                        else
                        {
                            idleToFach((double)packetList.get(i).getTimeInMicros());
                            
                            if (oldEB)
                                drawState(packetList.get(i).getTimeInMicros()-1, state.getValue());
                            else
                                drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                            state = State.FACH;
                            transitions.add(new TransitionPair(packetList.get(i).getTimeInMicros(), state.toString()));
                            drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                            bufferIDLEtoFACHdownlink += packetList.get(i).getLength();
                        }
                    }
                }
                break;
                    
                case FACH:
                {
                    bufferIDLEtoFACHdownlink = 0;
                    bufferIDLEtoFACHuplink = 0;
                    timeToEmptyUplink = timeToEmptyUplink(bufferFACHtoDCHuplink);
                    
                    // If timeToEmptyUplink or DOWNLINK_BUFFER_EMPTY_TIME has passed, clear the RLCBuffer
                    if (deltaUplink > timeToEmptyUplink)
                        bufferFACHtoDCHuplink = 0;
                    if (deltaDownlink > networkProperties.getDOWNLINK_BUFFER_EMPTY_TIME())
                        bufferFACHtoDCHdownlink = 0;
                    
                    // Uplink packets
                    if (packetList.get(i).getUplink())
                    {
                        if (oldEB)
                            drawState(packetList.get(i).getTimeInMicros()-1, state.getValue());
                        else
                            drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                        bufferFACHtoDCHuplink += packetList.get(i).getLength();
                        if (bufferFACHtoDCHuplink > networkProperties.getUPLINK_BUFFER_FACH_TO_DCH())
                        {
                            fachToDch((double)packetList.get(i).getTimeInMicros());
                            state = State.DCH;
                            transitions.add(new TransitionPair((double)packetList.get(i).getTimeInMicros(), state.toString()));
                            bufferFACHtoDCHuplink = 0;
                        }
                        drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                    }
                    // Downlink packets
                    else
                    {                        
                        if (oldEB)
                            drawState(packetList.get(i).getTimeInMicros()-1, state.getValue());
                        else
                            drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                        bufferFACHtoDCHdownlink += packetList.get(i).getLength();
                        if (bufferFACHtoDCHdownlink > networkProperties.getDOWNLINK_BUFFER_FACH_TO_DCH())
                        {
                            fachToDch((double)packetList.get(i).getTimeInMicros());
                            state = State.DCH;
                            transitions.add(new TransitionPair((double)packetList.get(i).getTimeInMicros(), state.toString()));
                            bufferFACHtoDCHdownlink = 0;
                        }
                        drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                    }
                }
                break;
                    
                case DCH:
                {
                    drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                    bufferIDLEtoFACHuplink = 0;
                    bufferIDLEtoFACHdownlink = 0;
                    bufferFACHtoDCHuplink = 0;
                    bufferFACHtoDCHdownlink = 0;
                }
                break;
            }
            // Save timestamps for the next loop
            previousTime = packetList.get(i).getTimeInMicros();            
            if (packetList.get(i).getUplink())            
                previousTimeUplink = packetList.get(i).getTimeInMicros();            
            else            
                previousTimeDownlink = packetList.get(i).getTimeInMicros();
        }
        
        // Finish the trace if the final state is FACH or DCH
        if (state == State.DCH)
        {
            dchToFach(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME());
            fachToIdle(previousTime + 
                    networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                    networkProperties.getFACH_IDLE_INACTIVITY_TIME());
            drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
            state = State.FACH;
            transitions.add(new TransitionPair(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.toString()));
            if (oldEB)
                drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME()+1, state.getValue());
            else
                drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());

            drawState(previousTime + 
                    (long)networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                    (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
            state = State.IDLE;
            transitions.add(new TransitionPair(previousTime + 
                    networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                    networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.toString()));
            if (oldEB)
                drawState(previousTime + 
                        (long)networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                        (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME()+1, state.getValue());
            else
                drawState(previousTime + 
                        (long)networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                        (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
        }
        else if (state == State.FACH)
        {
            fachToIdle(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME());
            drawState(previousTime + (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
            state = State.IDLE;
            transitions.add(new TransitionPair(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.toString()));
            if (oldEB)
                drawState(previousTime + (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME()+1, state.getValue());
            else
                drawState(previousTime + (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
        }
        linkDistrData.add(new PieChart.Data("Uplink", uplinkPacketCount));
        linkDistrData.add(new PieChart.Data("Downlink", packetList.size()-uplinkPacketCount));
        distrStatisticsList.add(new StatisticsEntry("Nr of UL packets",uplinkPacketCount));
        distrStatisticsList.add(new StatisticsEntry("Nr of DL packets",packetList.size()-uplinkPacketCount));
        transitions.add(new TransitionPair((double)packetList.get(packetList.size()-1).getTimeInMicros(), state.toString()));
        return stateSeries;
    }
    
    private void drawState(Long time, int state)
    {
        //Long tempTime = time.longValue();
        stateSeries.getData().add(new XYChart.Data(time, state));
        
    }
    
    public void getPower()
    {
        Double power = Double.valueOf(0);
        int timeInIDLE = 0, timeInFACH = 0, timeInDCH = 0;
        for (int i = 1; i < stateSeries.getData().size(); i++)
        {
            double timeDifference = (stateSeries.getData().get(i).getXValue() - stateSeries.getData().get(i-1).getXValue()) * 0.000001;
            switch(stateSeries.getData().get(i-1).getYValue())
            {
                case 0:
                {
                    power += timeDifference * deviceProperties.getPOWER_IN_IDLE();
                    timeInIDLE += timeDifference;
                }
                break;
                    
                case 1:
                {
                    power += timeDifference * deviceProperties.getPOWER_IN_FACH();
                    timeInFACH += timeDifference;
                }
                break;
                    
                case 3:
                {
                    power += timeDifference * deviceProperties.getPOWER_IN_DCH();
                    timeInDCH += timeDifference;
                }
                break;
            }
        }
        // Total power used rounded down to four decimal places
        statisticsList.add(new StatisticsEntry("Total Power Used",((double) Math.round(power * 10000) / 10000)));
        stateTimeData.add(new PieChart.Data("FACH", timeInFACH));
        stateTimeData.add(new PieChart.Data("DCH", timeInDCH));
        stateTimeData.add(new PieChart.Data("IDLE", timeInIDLE));
    }
    // State transition drawing methods to seperate state series
    private void dchToFach(Double time)
    {
        //time = time / 1000000;
        dchSeries.getData().add(new XYChart.Data(time, State.DCH.getValue()));
        dchSeries.getData().add(new XYChart.Data(time, 0));
        
        fachSeries.getData().add(new XYChart.Data(time, 0));
        fachSeries.getData().add(new XYChart.Data(time, State.FACH.getValue()));
    }
    
    private void fachToIdle(Double time)
    {
        //time = time / 1000000;
        fachSeries.getData().add(new XYChart.Data(time, State.FACH.getValue()));
        fachSeries.getData().add(new XYChart.Data(time, 0));
    }
    
    private void idleToFach(Double time)
    {
        //time = time / 1000000;
        fachSeries.getData().add(new XYChart.Data(time, 0));
        fachSeries.getData().add(new XYChart.Data(time, State.FACH.getValue()));
    }
    
    private void idleToDch(Double time)
    {
        //time = time / 1000000;
        dchSeries.getData().add(new XYChart.Data(time, 0));
        dchSeries.getData().add(new XYChart.Data(time, State.DCH.getValue()));
    }
    
    private void fachToDch(Double time)
    {
        //time = time / 1000000;
        fachSeries.getData().add(new XYChart.Data(time, State.FACH.getValue()));
        fachSeries.getData().add(new XYChart.Data(time, 0));
        
        dchSeries.getData().add(new XYChart.Data(time, 0));
        dchSeries.getData().add(new XYChart.Data(time, State.DCH.getValue()));
    }
    
    // Formula for calculating buffer empty time depending on buffer occupancy
    // (Downlink is modeled with a constant occupancy - the value in networkProperties)
    public long timeToEmptyUplink(int buffer) { return (long)networkProperties.getUPLINK_BUFFER_EMPTY_TIME() * buffer + 10; }
    
    // GETTERS
    public XYChart.Series<Long, Integer> getUplinkPackets(){ return uplinkPacketSeries; }
    public XYChart.Series<Long, Integer> getDownlinkPackets(){ return downlinkPacketSeries; }
    public XYChart.Series<Long, Integer> getStates(){ return stateSeries; }
    public XYChart.Series<Long, Integer> getFACH(){ return fachSeries; }
    public XYChart.Series<Long, Integer> getDCH(){ return dchSeries; }
    public ObservableList<PieChart.Data> getLinkDistroData() { return linkDistrData; }
    public ObservableList<PieChart.Data> getStateTimeData() { return stateTimeData; }
}
