package energybox.engines;

import energybox.Packet;
import energybox.StatisticsEntry;
import energybox.properties.device.PropertiesDevice3G;
import energybox.properties.network.Properties3G;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class Engine3G extends Engine
{
    enum State 
    { 
        IDLE(0), FACH(1), DCH(3);
        private final int value;
        private State(int value){this.value = value;}
        public int getValue() { return this.value; }
    }
    
    // VARIABLES TAKEN FROM THE CONSTRUCTOR
    Properties3G networkProperties; 
    PropertiesDevice3G deviceProperties;
    
    // CHART VARIABLES
    XYChart.Series<Long, Integer> fachSeries = new XYChart.Series();
    XYChart.Series<Long, Integer> dchSeries = new XYChart.Series();
    
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
    
    @Override
    public XYChart.Series<Double, Integer> modelStates()
    {
        // Buffer control variables
        int bufferIDLEtoFACHuplink = 0, bufferIDLEtoFACHdownlink = 0,
            bufferFACHtoDCHuplink = 0, bufferFACHtoDCHdownlink = 0;
        // Timer variables
        long deltaDownlink = 0, 
                deltaUplink = 0, 
                deltaT = 0,
                previousTimeUplink =  packetList.get(0).getTimeInMicros(), 
                previousTimeDownlink = packetList.get(0).getTimeInMicros(), 
                previousTime = packetList.get(0).getTimeInMicros(), // might wanna replace the variable with packetList.get(i-1).getTime()
                timeToEmptyUplink = 0; // timeToEmptyDownlink is a constant : networkProperties.getDOWNLINK_BUFFER_EMPTY_TIME; 
        State state = State.IDLE; // State enumeration

        stateSeriesData.beforeChanges();

        // Packet list points
        for (int i = 0; i < packetList.size(); i++) 
        {
            // Populating the packetChart series
            // to temporary data structure, so too many events are not sent
            // to observers. Very bad for performance!
            // Actually update the Chart data using updatePacketChart() later when 
            // all packets have been added.
            packetChartEntry(packetList.get(i));
            
            
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
                        drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
                        drawState(previousTime + 
                                (long)networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                                (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
                        state = State.IDLE;
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
                            // Bug correction for when the trace is not realistic.
                            // Ignores the the transition time if there are packets
                            // before the transition is suppose to end.
                            //if (packetList.get(i+1).getTimeInMicros() > packetList.get(i).getTimeInMicros() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME())
                            if ((packetList.get(i).getTimeInMicros() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME()) < (double)packetList.get(i+1).getTimeInMicros())
                            {
                                System.out.println("Next packet at : " + (double)packetList.get(i+1).getTimeInMicros());
                                System.out.println("Proomotion at: " + (packetList.get(i).getTimeInMicros() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME()));
                                idleToDch(packetList.get(i).getTimeInMicros() + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME());
                                drawState(packetList.get(i).getTimeInMicros() + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());                            
                                state = State.DCH;
                                drawState(packetList.get(i).getTimeInMicros() + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                            }
                            else
                            {
                                idleToDch((double)packetList.get(i).getTimeInMicros());
                                drawState(packetList.get(i).getTimeInMicros(), state.getValue());                            
                                state = State.DCH;
                                drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                            }
                        }
                        else
                        {
                            idleToFach((double)packetList.get(i).getTimeInMicros());
                            drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                            state = State.FACH;
                            drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                            bufferIDLEtoFACHuplink += packetList.get(i).getLength();
                        }
                    }
                    // Downlink packets
                    else
                    {
                        if (packetList.get(i).getLength() > networkProperties.getDOWNLINK_BUFFER_IDLE_TO_FACH_OR_DCH())
                        {
                            idleToDch((double)packetList.get(i).getTimeInMicros());// + networkProperties.getIDLE_TO_DCH_TRANSITION_TIME());
                            drawState(packetList.get(i).getTimeInMicros(), state.getValue());// + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                            state = State.DCH;
                            drawState(packetList.get(i).getTimeInMicros(), state.getValue());// + (long)networkProperties.getIDLE_TO_DCH_TRANSITION_TIME(), state.getValue());
                        }
                        else
                        {
                            idleToFach((double)packetList.get(i).getTimeInMicros());
                            drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                            state = State.FACH;
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
                        drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                        bufferFACHtoDCHuplink += packetList.get(i).getLength();
                        if (bufferFACHtoDCHuplink > networkProperties.getUPLINK_BUFFER_FACH_TO_DCH())
                        {
                            fachToDch((double)packetList.get(i).getTimeInMicros());
                            state = State.DCH;
                            bufferFACHtoDCHuplink = 0;
                        }
                        drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                    }
                    // Downlink packets
                    else
                    {                        
                        drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                        bufferFACHtoDCHdownlink += packetList.get(i).getLength();
                        if (bufferFACHtoDCHdownlink > networkProperties.getDOWNLINK_BUFFER_FACH_TO_DCH())
                        {
                            fachToDch((double)packetList.get(i).getTimeInMicros());
                            state = State.DCH;
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
        
        // update charts here for performance reasons
        updatePacketCharts();
        
        // Finish the trace if the final state is FACH or DCH
        if (state == State.DCH)
        {
            dchToFach(previousTime + networkProperties.getDCH_FACH_INACTIVITY_TIME());
            fachToIdle(previousTime + 
                    networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                    networkProperties.getFACH_IDLE_INACTIVITY_TIME());
            drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());
            state = State.FACH;
            drawState(previousTime + (long)networkProperties.getDCH_FACH_INACTIVITY_TIME(), state.getValue());

            drawState(previousTime + 
                    (long)networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                    (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
            state = State.IDLE;
            drawState(previousTime + 
                    (long)networkProperties.getDCH_FACH_INACTIVITY_TIME() +
                    (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
        }
        else if (state == State.FACH)
        {
            fachToIdle(previousTime + networkProperties.getFACH_IDLE_INACTIVITY_TIME());
            drawState(previousTime + (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
            state = State.IDLE;
            drawState(previousTime + (long)networkProperties.getFACH_IDLE_INACTIVITY_TIME(), state.getValue());
        }

        stateSeriesData.afterChanges();

        linkDistrData.add(new PieChart.Data("Uplink", uplinkPacketCount));
        linkDistrData.add(new PieChart.Data("Downlink", packetList.size()-uplinkPacketCount));
        distrStatisticsList.add(new StatisticsEntry("Nr of UL packets",uplinkPacketCount));
        distrStatisticsList.add(new StatisticsEntry("Nr of DL packets",packetList.size()-uplinkPacketCount));
        return stateSeries;
    }
    
    @Override
    public void calculatePower()
    {
        //Double power = Double.valueOf(0);
        int timeInIDLE = 0, timeInFACH = 0, timeInDCH = 0;
        for (int i = 1; i < stateSeries.getData().size(); i++)
        {
            double timeDifference = (stateSeries.getData().get(i).getXValue() - stateSeries.getData().get(i-1).getXValue());
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

    @Override
    public String getName() {
        return "3G";
    }

    // State transition drawing methods to seperate state series
    private void dchToFach(Double time)
    {
        time = time / 1000000;
        dchSeries.getData().add(new XYChart.Data(time, State.DCH.getValue()));
        dchSeries.getData().add(new XYChart.Data(time, 0));
        
        fachSeries.getData().add(new XYChart.Data(time, 0));
        fachSeries.getData().add(new XYChart.Data(time, State.FACH.getValue()));
    }
    
    private void fachToIdle(Double time)
    {
        time = time / 1000000;
        fachSeries.getData().add(new XYChart.Data(time, State.FACH.getValue()));
        fachSeries.getData().add(new XYChart.Data(time, 0));
    }
    
    private void idleToFach(Double time)
    {
        time = time / 1000000;
        fachSeries.getData().add(new XYChart.Data(time, 0));
        fachSeries.getData().add(new XYChart.Data(time, State.FACH.getValue()));
    }
    
    private void idleToDch(Double time)
    {
        time = time / 1000000;
        dchSeries.getData().add(new XYChart.Data(time, 0));
        dchSeries.getData().add(new XYChart.Data(time, State.DCH.getValue()));
    }
    
    private void fachToDch(Double time)
    {
        time = time / 1000000;
        fachSeries.getData().add(new XYChart.Data(time, State.FACH.getValue()));
        fachSeries.getData().add(new XYChart.Data(time, 0));
        
        dchSeries.getData().add(new XYChart.Data(time, 0));
        dchSeries.getData().add(new XYChart.Data(time, State.DCH.getValue()));
    }
    
    // Formula for calculating buffer empty time depending on buffer occupancy
    // (Downlink is modeled with a constant occupancy - the value in networkProperties)
    public long timeToEmptyUplink(int buffer) { return (long)networkProperties.getUPLINK_BUFFER_EMPTY_TIME() * buffer + 10; }
    
    
    // GETTERS
    public XYChart.Series<Long, Integer> getFACH(){ return fachSeries; }
    public XYChart.Series<Long, Integer> getDCH(){ return dchSeries; }
}
