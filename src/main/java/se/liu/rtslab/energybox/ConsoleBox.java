package se.liu.rtslab.energybox;

import se.liu.rtslab.energybox.engines.Engine;
import se.liu.rtslab.energybox.engines.EngineWifi;
import se.liu.rtslab.energybox.engines.Engine3G;
import se.liu.rtslab.energybox.properties.device.Device;
import se.liu.rtslab.energybox.properties.device.PropertiesDevice3G;
import se.liu.rtslab.energybox.properties.device.PropertiesDeviceWifi;
import se.liu.rtslab.energybox.properties.network.Network;
import se.liu.rtslab.energybox.properties.network.Properties3G;
import se.liu.rtslab.energybox.properties.network.PropertiesWifi;
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
    private final Engine engine;
    // populated within printResults() to be printed by outputToFile()
    private XYChart.Series<Double, Integer> printStates;
    private Double power;
    private String sourceIP = "";
    private ProcessTrace trace;

    /**
     * Initialize using paths to config files, e.g. "/home/username/EnergyBox/config/3g_teliasonera.config"
     *
     * This constructor is used when calling EnergyBox from the shell.
     */
    public ConsoleBox(ProcessTrace trace, String tracePath, String networkPath, String devicePath) {
        this.trace = trace;
        this.sourceIP = trace.getSourceIP();

        Network networkProperties = null;
        Device deviceProperties = null;
        try {
            networkProperties = getNetworkProperties(pathToProperties(networkPath));
            deviceProperties = buildDeviceProperties(pathToProperties(devicePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        engine = tryBuildEngine(trace.getPacketList(), deviceProperties, networkProperties);
        engine.modelStates();
        engine.calculatePower();
        printStates = engine.getPower(); // so that the states could be accesed for outputToFile
    }

    /**
     * Initialize using name of config files, e.g. "3g_teliasonera.config".
     *
     * This constructor is used when using the EnergyBox library packaged as a .jar file.
     */
    public ConsoleBox(ProcessTrace trace, String networkConfig, String deviceConfig) {
        Properties networkProperties = null;
        try {
            InputStream networkStream = ClassLoader.getSystemResourceAsStream(networkConfig);
            networkProperties = streamToProperties(networkStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Properties deviceProperties = null;
        try {
            InputStream deviceStream = ClassLoader.getSystemResourceAsStream(deviceConfig);
            deviceProperties = streamToProperties(deviceStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        engine = tryBuildEngine(trace.getPacketList(), buildDeviceProperties(deviceProperties), getNetworkProperties(networkProperties));
        engine.modelStates();
        engine.calculatePower();
        printStates = engine.getPower(); // so that the states could be accesed for outputToFile
    }

    // Build engine. If pcap if empty, bail out by exiting application and printing 0.0 Joules
    // This ensures that energy can always be grepped from stdout.
    private Engine tryBuildEngine(ObservableList<Packet> tracePackets, Device device, Network network) {
        try {
            return buildEngine(tracePackets, device, network);
        } catch (IllegalArgumentException e) {
            if (tracePackets.size() == 0) {
                System.out.println("Total power in Joules: 0.0");
                System.exit(0);
            }
            throw e;
        }
    }

    public void printResults()
    {
        power = engine.getPowerValue();
        System.out.println("Network model: " + getModelName());
        System.out.println("Detected recorder device IP: " + trace.getSourceIP());
        System.out.println("Total power in Joules: " + getTotalPower());
    }

    public String getModelName() {
        return engine.getName();
    }

    public double getTotalPower() {
        return engine.getStatisticsList().get(0).getValue();
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
        // Engines do not handle empty pcaps gracefully, so fail fast if one is encountered
        if (packetList.size() == 0) {
            throw new IllegalArgumentException("Packet trace file is empty.");
        }

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
        // try path...
        File file = new File(path);
        if (file.exists())
        {
            InputStream in = new FileInputStream(file);
            return streamToProperties(in);
        }

        // ...then try jar dir...
        String jarLocation = OSTools.getJarLocation();
        StringBuilder propertyPath = new StringBuilder();
        propertyPath.append(new File(jarLocation).getParent());
        propertyPath.append(File.separator);
        propertyPath.append(path);
        file = new File(propertyPath.toString());
        if (file.exists())
        {
            InputStream in = new FileInputStream(file);
            return streamToProperties(in);
        }

        // ...then try bundled resources...
        InputStream stream = ClassLoader.getSystemResourceAsStream(path);
        if (stream != null) {
            return streamToProperties(stream);
        }

        // ...finally: resource cannot be found! s
        throw new IllegalArgumentException("Could not parse file configuration file: " + path);
    }

    public Properties streamToProperties(InputStream in) throws IOException
    {
        Properties properties = new Properties();
        properties.load(in);
        return properties;
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
