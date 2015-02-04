package energybox;

import energybox.engines.Engine;
import energybox.engines.EngineWifi;
import energybox.engines.Engine3G;
import energybox.properties.device.Device;
import energybox.properties.device.PropertiesDevice3G;
import energybox.properties.device.PropertiesDeviceWifi;
import energybox.properties.network.Network;
import energybox.properties.network.Properties3G;
import energybox.properties.network.PropertiesWifi;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class ConsoleBox
{
    private final String networkPath, devicePath;
    private final Engine engine;
    // populated within printResults() to be printed by outputToFile()
    private XYChart.Series<Double, Integer> printStates;
    private Double power;
    private String sourceIP = "";
    private ProcessTraceOSX trace;

    public ConsoleBox(ProcessTraceOSX trace, String tracePath, String networkPath, String devicePath) {
        this.trace = trace;

        this.networkPath = networkPath;
        this.devicePath = devicePath;

        ObservableList<Packet> packetList = trace.getPacketList();

        Properties networkConfig;
        Network networkProperties = null;
        Device deviceProperties = null;
        try {
            networkConfig = pathToProperties(networkPath);
            networkProperties = getNetworkProperties(networkConfig);
            deviceProperties = buildDeviceProperties(pathToProperties(devicePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        engine = buildEngine(packetList, deviceProperties, networkProperties);

        engine.modelStates();
        engine.calculatePower();
        printStates = engine.getPower(); // so that the states could be accesed for outputToFile
    }
    
    public void printResults()
    {
        power = engine.getPowerValue();
        System.out.println("Network model: " + engine.getName());
        System.out.println("Detected recorder device IP: " + trace.getSourceIP());
        System.out.println("Total power in Joules: " + engine.getStatisticsList().get(0).getValue());
    }

    private Network getNetworkProperties(Properties networkConfig) {
        switch (networkConfig.getProperty("TYPE"))
        {
            case "3G": return new Properties3G(networkConfig);
            case "Wifi": return new PropertiesWifi(networkConfig);
            default: throw new IllegalArgumentException("Could not determine NETWORK type. Check network config.");
        }
    }

    private Engine buildEngine(ObservableList<Packet> packetList,
                             Device deviceProperties,
                             Network networkProperties) {
        // CHOOSING THE ENGINE
        if (networkProperties instanceof Properties3G) {
            return new Engine3G(packetList, sourceIP, ((Properties3G) networkProperties), ((PropertiesDevice3G) deviceProperties));
        } else if (networkProperties instanceof PropertiesWifi) {
            return new EngineWifi(packetList, sourceIP, ((PropertiesWifi) networkProperties), ((PropertiesDeviceWifi) deviceProperties));
        } else {
            throw new IllegalArgumentException("Could not determine ENGINE type. Check network config.");
        }
    }

    private Device buildDeviceProperties(Properties deviceConfig) {
        switch (deviceConfig.getProperty("TYPE"))
        {
            case "Device3G": return new PropertiesDevice3G(deviceConfig);
            case "DeviceWifi": return new PropertiesDeviceWifi(deviceConfig);
            default: throw new IllegalArgumentException("Could not determine DEVICE type. Check device config.");
        }
    }

    public Properties pathToProperties(String path) throws IOException
    {
        Properties properties = new Properties();
        if (new File(path).exists())
        {
            File f = new File(path);
            InputStream in = new FileInputStream (f);
            properties.load(in);
            return properties;
        }
        else
        {
            String location = OSTools.getJarLocation();
            if (new File(location).exists())
            {
                StringBuilder relativePath = new StringBuilder();
                relativePath.append(new File(location).getParent());
                relativePath.append(File.separator);
                relativePath.append(path);
                File f = new File(relativePath.toString());
                InputStream in = new FileInputStream (f);
                properties.load(in);
            }
            return properties;
        }
    }
    
    public void outputToFile(String path)
    {
        File file = new File(path);
        try
        {
            FileWriter writer = new FileWriter(file.getAbsolutePath());
            writer.append(sourceIP);
            writer.append("\n");
            writer.append(power.toString());
            writer.append("\n");
            for (int i = 0; i < printStates.getData().size(); i++)
            {
                writer.append(printStates.getData().get(i).getYValue().toString());
                writer.append(",");
                writer.append(Double.valueOf(printStates.getData().get(i).getXValue().doubleValue()).toString());
                writer.append("\n");
            }
            writer.flush();
	    writer.close();
        }
        catch(IOException e){ e.printStackTrace();}
    }
}
