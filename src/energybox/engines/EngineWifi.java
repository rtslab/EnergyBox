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
                    // TODO: Calculate 
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
    
    public void getPower()
    {
        Double power = Double.valueOf(0);
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
