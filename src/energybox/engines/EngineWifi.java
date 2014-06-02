package energybox.engines;

import energybox.Packet;
import energybox.StatisticsEntry;
import energybox.properties.device.PropertiesDeviceWifi;
import energybox.properties.network.PropertiesWifi;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class EngineWifi extends Engine
{ 
    PropertiesWifi networkProperties; 
    PropertiesDeviceWifi deviceProperties;
    enum State 
    { 
        PSM(0), CAM(2), CAMH(3);
        private final int value;
        private State(int value){this.value = value;}
        public int getValue() { return this.value; }
    }
    XYChart.Series<Long, Integer> camSeries = new XYChart.Series();
    
    public EngineWifi(ObservableList<Packet> packetList,
            String sourceIP,
            PropertiesWifi networkProperties, 
            PropertiesDeviceWifi deviceProperties)
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
        // Timer variables
        long deltaT = 0;
        long previousTime = packetList.get(0).getTimeInMicros();
        State state = State.PSM;
        // For CAMH calculation
        int dataSum = 0, firstSeriesIndex = 0;
        double chunkStart = 0, chunkEnd = networkProperties.getCAM_TIME_WIMDOW();
        
        for (int i = 0; i < packetList.size(); i++) 
        {
            packetChartEntry(packetList.get(i));
            deltaT = packetList.get(i).getTimeInMicros() - previousTime;
            
            // DEMOTIONS
            switch (state)
            {
                case CAM:
                {
                    // If the time between packets is longer than the timeout
                    // demote to PSM
                    if (deltaT > networkProperties.getCAM_PSM_INACTIVITY_TIME())
                    {
                        camToPsm(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME());
                        drawState(previousTime + (long)networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
                        state = State.PSM;
                        drawState(previousTime + (long)networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
                    }
                }
                break;
                    
                case CAMH:
                {
                    // CAMH has been implemented in the same way as in the origina
                    // version - by altering the CAM points that are within CAMH
                    // throughput chunks.
                }
                break;
            }
            
            // PROMOTIONS
            switch (state)
            {
                // In PSM promotion happens whenever a packet is sent
                case PSM:
                {
                    psmToCam((double)packetList.get(i).getTimeInMicros());
                    drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                    state = State.CAM;
                    drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                }
                break;
                    
                case CAM:
                {
                    drawState(packetList.get(i).getTimeInMicros(), state.getValue());
                    
                    // CAMH CALCULCATION
                    // If the packet is within the current chunk, add the packet's
                    // length to the chunk's dataSum.
                    if ((chunkStart <= packetList.get(i).getTimeInMicros()) && (packetList.get(i).getTimeInMicros() < chunkEnd))
                    {
                        dataSum = dataSum+packetList.get(i).getLength();
                    }
                    // If the packet is beyond chunkEnd and the dataSum exceeds
                    // the data rate threshold, take all the packets in the previous
                    // chunk and bump up the CAM points to CAMH.
                    else
                    {
                        
                        if (dataSum > networkProperties.getWINDOW_DATA_RATE_THRESHOLD())
                        {
                            int last = 0;
                            boolean first = true;
                            // Goes through the chart point series starting from
                            // the begining of the last chunks end (to save on iterations)
                            for (int j = firstSeriesIndex; j < stateSeries.getData().size(); j++)
                            {
                                // Checks for weather the point is between the 
                                // chunk boundries and if it's CAM
                                if ((stateSeries.getData().get(j).getYValue() == State.CAM.getValue()) &&
                                        ((chunkStart/1000000) <= stateSeries.getData().get(j).getXValue()) &&
                                        (stateSeries.getData().get(j).getXValue() < (chunkEnd/1000000)))
                                {
                                    // Inserts a new point if it's the start of a
                                    // streak. Bumps up the point if it's in the
                                    // middle of a streak.
                                    if (first)
                                    {
                                        stateSeries.getData().add(j+1, new XYChart.Data(
                                                stateSeries.getData().get(j).getXValue(), 
                                                State.CAMH.getValue()));
                                        first = false;
                                    }
                                    else
                                        stateSeries.getData().get(j).setYValue(State.CAMH.getValue());
                                    // saves the index of the last altered point
                                    // for the final two points after the loop.
                                    last = j;
                                }
                            }
                            // Two final points following the original tool's implementation
                            stateSeries.getData().add(last+1, new XYChart.Data(
                                    (chunkEnd/1000000), 
                                    State.CAM.getValue()));
                            stateSeries.getData().add(last+1, new XYChart.Data(
                                    (chunkEnd/1000000), 
                                    State.CAMH.getValue()));
                        }
                        dataSum = 0;
                        // The original implementation rounds the chunk start and
                        // end values to one tenth of the window size.
                        chunkStart = packetList.get(i).getTimeInMicros()-(packetList.get(i).getTimeInMicros() % (networkProperties.getCAM_TIME_WIMDOW()/10));
                        chunkEnd = packetList.get(i).getTimeInMicros()-(packetList.get(i).getTimeInMicros() % (networkProperties.getCAM_TIME_WIMDOW()/10))+(networkProperties.getCAM_TIME_WIMDOW()/10);
                        dataSum = packetList.get(i).getLength();
                        firstSeriesIndex = stateSeries.getData().size()-1;
                    }
                }
                break;
            }
            // Save timestamps for the next loop
            previousTime = packetList.get(i).getTimeInMicros();
        }

        if (state != State.PSM)
        {
            // Needs camhToPsm if the end state is CAMH
            camToPsm(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME());
            drawState(previousTime + (long)networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
            state = State.PSM;
            drawState(previousTime + (long)networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
        }
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
        int timeInPSM = 0, timeInCAM = 0, timeInCAMH = 0;
        for (int i = 1; i < stateSeries.getData().size(); i++)
        {
            double timeDifference = (stateSeries.getData().get(i).getXValue() - stateSeries.getData().get(i-1).getXValue());
            switch(stateSeries.getData().get(i-1).getYValue())
            {
                case 0:
                {
                    power += timeDifference * deviceProperties.getPOWER_IN_PSM();
                    timeInPSM += timeDifference;
                }
                break;
                    
                case 2:
                {
                    power += timeDifference * deviceProperties.getPOWER_IN_CAM();
                    timeInCAM += timeDifference;
                }
                break;
                    
                case 3:
                {
                    power += timeDifference * deviceProperties.getPOWER_IN_CAMH();
                    timeInCAMH += timeDifference;
                }
                break;
            }
        }
        // Total power used rounded down to four decimal places
        statisticsList.add(new StatisticsEntry("Total Power Used",((double) Math.round(power * 10000) / 10000)));
        stateTimeData.add(new PieChart.Data("DCH", timeInPSM));
        stateTimeData.add(new PieChart.Data("IDLE", timeInCAM));
    }
    
    private void psmToCam(Double time)
    {
        time = time / 1000000;
        camSeries.getData().add(new XYChart.Data(time, 0));
        camSeries.getData().add(new XYChart.Data(time, State.CAM.getValue()));
    }
    
    private void camToPsm(Double time)
    {
        time = time / 1000000;
        camSeries.getData().add(new XYChart.Data(time, State.CAM.getValue()));
        camSeries.getData().add(new XYChart.Data(time, 0));
    }
    
    // GETTERS
    public XYChart.Series<Long, Integer> getCAM(){ return camSeries; }
}
