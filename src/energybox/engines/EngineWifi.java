package energybox.engines;

import energybox.Packet;
import energybox.StatisticsEntry;
import energybox.properties.device.PropertiesDeviceWifi;
import energybox.properties.network.PropertiesWifi;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
        this.uplinkSeries = this.getUplinkThroughput(networkProperties.getCAM_TIME_WIMDOW()/1000000);
        this.downlinkSeries = this.getDownlinkThroughput(networkProperties.getCAM_TIME_WIMDOW()/1000000);
    }
    
    @Override
    public XYChart.Series<Double, Integer> modelStates()
    {
        // Timer variables
        long deltaT = 0;
        long previousTime = packetList.get(0).getTimeInMicros();
        State state = State.PSM;
        // For CAMH calculation
        
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
                }
                break;
            }
            // Save timestamps for the next loop
            previousTime = packetList.get(i).getTimeInMicros();
        }
        
        double chunkEnd = 0;
        // Indexes for the stateSries and uplinkSeries entries. We can start with
        // the second point because the first will always be (0,0) and it helps
        // with the look-back when determening streaks.
        int i = 1, chunk = 0, lastCAMH = 0;
        if (hasCAMH())
        {
            // Cycles through both the state points and throughput chunks at the same
            // time and promotes CAM to CAMH where needed.
            while ((i < stateSeries.getData().size()) && (chunk < uplinkSeries.getData().size()) )
            {
                // If the current chunk is CAMH
                if (isHighChunk(chunk))
                {
                    lastCAMH = chunk;
                    //If the current point is within the current chunk
                    if (stateSeries.getData().get(i).getXValue() < uplinkSeries.getData().get(chunk).getXValue())
                    {
                        // If the current point is CAM, check if it's in the middle
                        // of a streak. If it is then just bump up the state, if it
                        // isn't then add the streak beginning.
                        if (stateSeries.getData().get(i).getYValue() == State.CAM.getValue())
                        {
                            // Promote current point
                            if (stateSeries.getData().get(i-1).getYValue() == State.CAMH.getValue())
                                stateSeries.getData().get(i).setYValue(State.CAMH.getValue());

                            // There is only one situation where the previous point
                            // of a CAM point within a CAMH chunk would have a CAM 
                            // point before it - previous chunk was non-CAMH and this
                            // is the first point of the chunk, thus needs a promotion
                            else if (stateSeries.getData().get(i-1).getYValue() == State.CAM.getValue())
                            {
                                stateSeries.getData().add(i,
                                        new XYChart.Data(
                                                uplinkSeries.getData().get(chunk-1).getXValue(), 
                                                State.CAMH.getValue()));
                                stateSeries.getData().add(i,
                                        new XYChart.Data(
                                                uplinkSeries.getData().get(chunk-1).getXValue(), 
                                                State.CAM.getValue()));
                            }
                            // Add new point after current one
                            else
                            {
                                stateSeries.getData().add(i+1, 
                                        new XYChart.Data(
                                                stateSeries.getData().get(i).getXValue(), 
                                                State.CAMH.getValue()));
                            }
                            i++;
                        }
                        else i++;
                    }
                    // Try the next chunk with the same point.
                    else chunk++;
                }
                // If the current chunk is not CAMH
                else
                {

                    //If the current point is within the current chunk
                    if (stateSeries.getData().get(i).getXValue() < uplinkSeries.getData().get(chunk).getXValue())
                    {
                        // If the streak was still on when the chunk changed to non CAMH,
                        // insert a demotion at the end of the chunk.
                        if (stateSeries.getData().get(i-1).getYValue() == State.CAMH.getValue())
                        {
                            // Since there's a chance that the current chunk might be
                            // more than one chunk after the previous CAMH chunk,
                            // find the last CAMH chunk's end time
                            stateSeries.getData().add(i,
                                    new XYChart.Data(
                                            uplinkSeries.getData().get(lastCAMH).getXValue()+Double.valueOf(0.001), 
                                            State.CAM.getValue()));

                            stateSeries.getData().add(i,
                                    new XYChart.Data(
                                            uplinkSeries.getData().get(lastCAMH).getXValue()+Double.valueOf(0.001), 
                                            State.CAMH.getValue()));
                            i++;
                        }
                        i++;
                    }
                    else chunk++;
                }
            }
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
    
    private Long getChunkThroughput(int i)
    {
        return (uplinkSeries.getData().get(i).getYValue() + downlinkSeries.getData().get(i).getYValue());
    }
    
    private boolean isHighChunk(int i)
    {
        return getChunkThroughput(i) > networkProperties.getWINDOW_DATA_RATE_THRESHOLD();
    }
    
    private boolean hasCAMH()
    {
        for (int i = 0; i < uplinkSeries.getData().size(); i++)
        {
            if(isHighChunk(i)) 
            {
                return true;
            }
        }
        return false;
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
