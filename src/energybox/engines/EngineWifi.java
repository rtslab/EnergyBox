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
        //System.out.println(hasCAMH());
        //testThroughput();
        State state = State.PSM;
        // For CAMH calculation
        //int dataSum = 0, firstSeriesIndex = 0;
        //double chunkStart = 0, chunkEnd = networkProperties.getCAM_TIME_WIMDOW();
        
        for (int i = 0; i < packetList.size(); i++) 
        {
            // to temporary data structure, so too many events are not sent
            // to observers. Very bad for performance!
            // Actually update the Chart data using updatePacketChart() later when 
            // all packets have been added.
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
                    /*
                    // CAMH CALCULCATION
                    // If the packet is within the current chunk, add the packet's
                    // length to the chunk's dataSum.
                    if ((chunkStart <= packetList.get(i).getTimeInMicros()) && (packetList.get(i).getTimeInMicros() < chunkEnd))
                    {
                        dataSum += packetList.get(i).getLength();
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
                            for (int j = firstSeriesIndex-1; j < stateSeries.getData().size(); j++)
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
                                        // If the start of the current streak is
                                        // the end of the previous chunk's streak
                                        // promote the first CAM point as well to
                                        // have a continuous streak.
                                        if (stateSeries.getData().get(j-1).getYValue() == State.CAMH.getValue())
                                            stateSeries.getData().get(j).setYValue(State.CAMH.getValue());
                                        
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
                        //if ((chunkEnd/1000000 > 8) && (chunkEnd/1000000 < 10)) System.out.println(chunkStart + " | " + dataSum + " | " + chunkEnd);
                        dataSum = 0;
                        // The original implementation rounds the chunk start and
                        // end values to one tenth of the window size.
                        chunkStart = packetList.get(i).getTimeInMicros()-(packetList.get(i).getTimeInMicros() % (networkProperties.getCAM_TIME_WIMDOW()/10));
                        chunkEnd = packetList.get(i).getTimeInMicros()-(packetList.get(i).getTimeInMicros() % (networkProperties.getCAM_TIME_WIMDOW()/10))+(networkProperties.getCAM_TIME_WIMDOW()/10);
                        dataSum = packetList.get(i).getLength();
                        firstSeriesIndex = stateSeries.getData().size()-1;
                    }
                    */
                }
                break;
            }
            // Save timestamps for the next loop
            previousTime = packetList.get(i).getTimeInMicros();
        }
        
        // Update charts here for performance reasons
        updatePacketCharts();
        
        double chunkEnd = 0;
        // Indexes for the stateSries and uplinkSeries entries. We can start with
        // the second point because the first will always be (0,0) and it helps
        // with the look-back when determening streaks.
        int i = 1, chunk = 0, lastCAMH = 0;
        if (hasCAMH())
        {

            stateSeriesData.beforeChanges();

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
            stateSeriesData.afterChanges();

        }
        if (state != State.PSM)
        {
            // Needs camhToPsm if the end state is CAMH
            camToPsm(previousTime + networkProperties.getCAM_PSM_INACTIVITY_TIME());
            //drawState(previousTime, state.getValue());// + (long)networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
            drawState(previousTime + (long)networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
            state = State.PSM;
            //drawState(previousTime, state.getValue());// + (long)networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
            drawState(previousTime + (long)networkProperties.getCAM_PSM_INACTIVITY_TIME(), state.getValue());
            //drawState(Long.valueOf(270046000L), state.getValue());
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
            if (true)//(stateSeries.getData().get(i).getXValue() <= Double.valueOf(310.97))
            {//////
            double timeDifference = (stateSeries.getData().get(i).getXValue() - stateSeries.getData().get(i-1).getXValue());
            switch(stateSeries.getData().get(i-1).getYValue())
            {
                case 0:
                {
                    power += timeDifference * deviceProperties.getPOWER_IN_PSM();
                    timeInPSM += timeDifference;
                    //System.out.println(stateSeries.getData().get(i).getXValue() + " : " + power);
                }
                break;
                    
                case 2:
                {
                    power += timeDifference * deviceProperties.getPOWER_IN_CAM();
                    timeInCAM += timeDifference;
                    //System.out.println(stateSeries.getData().get(i).getXValue() + " : " + power);
               }
                break;
                    
                case 3:
                {
                    power += timeDifference * deviceProperties.getPOWER_IN_CAMH();
                    timeInCAMH += timeDifference;
                    //System.out.println(stateSeries.getData().get(i).getXValue() + " : " + power);
                }
                break;
            }/////
            }
        }
        // Total power used rounded down to four decimal places
        statisticsList.add(new StatisticsEntry("Total Power Used",((double) Math.round(power * 10000) / 10000)));
        stateTimeData.add(new PieChart.Data("DCH", timeInPSM));
        stateTimeData.add(new PieChart.Data("IDLE", timeInCAM));
    }

    @Override
    public String getName() {
        return "Wifi";
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
                //return false;
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
    
    
    
    private void testThroughput()
    {
        File file = new File("D:\\testUL.csv");

        try
        {
            FileWriter writer = new FileWriter(file.getAbsolutePath());
            for (int i = 0; i < uplinkSeries.getData().size(); i++)
            {
                writer.append(uplinkSeries.getData().get(i).getYValue().toString());
                writer.append(",");
                writer.append(Double.valueOf(uplinkSeries.getData().get(i).getXValue().doubleValue()).toString());
                writer.append("\n");
            }
            writer.flush();
	    writer.close();
        }
        catch(IOException e){ e.printStackTrace();}
        
        
        File file2 = new File("D:\\testDL.csv");

        try
        {
            FileWriter writer = new FileWriter(file2.getAbsolutePath());
            for (int i = 0; i < downlinkSeries.getData().size(); i++)
            {
                writer.append(downlinkSeries.getData().get(i).getYValue().toString());
                writer.append(",");
                writer.append(Double.valueOf(downlinkSeries.getData().get(i).getXValue().doubleValue()).toString());
                writer.append("\n");
            }
            writer.flush();
	    writer.close();
        }
        catch(IOException e){ e.printStackTrace();}
    }
}
