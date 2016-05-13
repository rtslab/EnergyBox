package se.liu.rtslab.energybox;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;

import org.jnetpcap.winpcap.WinPcap;
import se.liu.rtslab.energybox.engines.Engine3G;
import se.liu.rtslab.energybox.engines.EngineWifi;
import se.liu.rtslab.energybox.properties.device.Device;
import se.liu.rtslab.energybox.properties.device.PropertiesDevice3G;
import se.liu.rtslab.energybox.properties.device.PropertiesDeviceWifi;
import se.liu.rtslab.energybox.properties.network.Network;
import se.liu.rtslab.energybox.properties.network.Properties3G;
import se.liu.rtslab.energybox.properties.network.PropertiesWifi;

/**
 * @author Rihards Polis
 * Linkoping University
 */
public class MainFormController implements Initializable, Runnable, ProgressObserver {
    @FXML
    protected Label errorText;
    @FXML
    private TextField textField;
    @FXML
    private TextField deviceField;
    @FXML
    private TextField networkField;
    @FXML
    protected TextField ipField;
    @FXML
    protected ProgressBar progressBar;
    
    Network networkProperties = null;
    Device deviceProperties = null;
    String fieldError = "-fx-border-color: #FF3300; -fx-border-radius: 5; -fx-border-style: solid; -fx-border-width: 2;";
    String tracePath;
    String type; // for engine selection
    // Two possible criteria that can indicate the source IP:
    // -> "HTTP" - a phone is usually not running a web server, so any GET requests are from the the recording device
    // -> "DNS" - a phone is usually not running a DNS server so any requests are from the the recording device
    String sourceIP = "";
    HashMap<String, Integer> addressOccurrence = new HashMap();
    boolean error = false;
    
    final ObservableList<Packet> packetList = FXCollections.observableList(new ArrayList());
    @FXML
    private Button modelButton;
    @FXML
    private Button deviceButton;
    @FXML
    private Button networkButton;
    @FXML
    private Button traceButton;
    @FXML
    private ImageView image;
    
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        // Load the icon for the Model button
        try {
            image.setImage(new Image("img/gears.png", true));
        } catch (Exception e) {
            System.err.println("Gears image not found");
        }
        
        // Checks between the two supported operating systems and tries to add
        // the directory where the JAR file is located to the PATH or CLASSPATH
        // variables in the JVM.
        // OS X uses tshark instead, so the process is different.
/*
        os = OSTools.getOS();
        System.out.println("MFC: "+os);
        switch(os)
        {
            case "Windows":
            {
                String location = MainFormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedLocation = "";
                try { decodedLocation = URLDecoder.decode(location, "UTF-8"); }
                catch (UnsupportedEncodingException e){ e.printStackTrace(); }
                StringBuilder relativePath = new StringBuilder();
                relativePath.append(new File(decodedLocation).getParent());
                relativePath.append(File.separator);
                try { OSTools.addDirectory(relativePath.toString()); }
                catch (IOException e)
                {
                    OSTools.showErrorDialog("JVM path error!", e.getMessage());
                }
                System.out.println("MFC: "+relativePath.toString());
            }
            break;

            case "Linux":
            {
                String location = MainFormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedLocation = "";
                try { decodedLocation = URLDecoder.decode(location, "UTF-8"); }
                catch (UnsupportedEncodingException e){ e.printStackTrace(); }
                StringBuilder relativePath = new StringBuilder();
                relativePath.append(new File(decodedLocation).getParent());
                relativePath.append(File.separator);
                try { OSTools.addDirectory(relativePath.toString()); }
                catch (IOException e)
                {
                    OSTools.showErrorDialog("JVM Path Error", e.getMessage());
                }
                try { WinPcap.isSupported(); }
                catch (UnsatisfiedLinkError e)
                {
                    OSTools.showErrorDialog("Libpcap Error", "Libpcap-dev not installed!");
                }
            }
            break;

            case "Mac":
            {
                //The OS X version uses tshark as a temporary fix
                //This code only checks whether tshark is installed and it can be executed,
                //and shows an error otherwise.
                //Apache Commons Exec is used:
                 //Good tutorial http://blog.sanaulla.info/2010/09/07/execute-external-process-from-within-jvm-using-apache-commons-exec-library/
                String answer = null;
                final CommandLine cmdLine = new CommandLine("which");
                cmdLine.addArgument("tshark");

                DefaultExecutor executor = new DefaultExecutor();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                //Handle the output of the program
                PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
                executor.setStreamHandler(streamHandler);
                try {
                    int exitValue = executor.execute(cmdLine,EnvironmentUtils.getProcEnvironment());
                    answer=outputStream.toString();
                    //System.out.println("Exit value: "+exitValue);
                    System.out.println("MainController, tshark found: "+answer);
                } catch (IOException ex) {
                    Logger.getLogger(MainFormController.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(answer==null){
                    //Tshark is not installed, or its not in the path, report it
                    System.err.println("Tshark is not installed or in the path");
                    //ToDo: Add a dialog "Tshark is not installed or in the path"
                    OSTools.showErrorDialog("MainFormController","Tshark is not installed or in the path");
                    //JOptionPane.showMessageDialog(null, "Tshark is not installed or in the path");
                }
                //It does not work if its launched from NetBeans, launch from the terminal

                //http://commons.apache.org/proper/commons-exec/tutorial.html
                //http://www.coderanch.com/t/624006/java/java/tshark-giving-output
           }
           break;

        }
*/

        // Default 3G values for testing
        /*
        tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\traces\\\\test1UL.pcap";
        textField.setText("test1UL.pcap");
        //ipField.setText("10.209.43.104");
        type = "3G";
        Properties properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\config\\device_3g.config");
        deviceProperties = new PropertiesDevice3G(properties);
        properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\config\\3g_teliasonera.config");
        networkProperties = new Properties3G(properties);
        
        // Default Wifi values for testing
        //tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\test\\\\round2good.pcap";
        //textField.setText("round2good.pcap");
        tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\traces\\\\random31.pcap";
        textField.setText("random31.pcap");
        type = "Wifi";
        Properties properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\config\\samsungS2_wifi.config");
        deviceField.setText("samsungS2_wifi.config");
        deviceProperties = new PropertiesDeviceWifi(properties);
        properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\config\\wifi_general.config");
        networkField.setText("wifi_general.config");
        networkProperties = new PropertiesWifi(properties);*/
        
    }
    
    @FXML
    private void handleButtonAction(ActionEvent event)
    {
        checkFieldError(textField);
        checkFieldError(deviceField);
        checkFieldError(networkField);
        if (error) 
        {
            run();
            return;
        }
        
        modelButton.setDisable(true);
        networkButton.setDisable(true);
        deviceButton.setDisable(true);
        traceButton.setDisable(true);
        
        progressBar.visibleProperty().set(true);
        errorText.setText("Loading trace...");
        error = false;

        final UpdatesController controller = new ControllerUpdater(this);
        final ProcessTrace trace = ProcessTrace.Factory.build(tracePath, controller,OSTools.getOS());
        System.out.println("Running " + trace.getClass().getSimpleName());

        // override ip. if "", will be calculated in ProcessTrace
        trace.setIp(ipField.getText());
        trace.addObserver(this);
        (new Thread(trace)).start();
    }
    
    // Executes in the Event Dispatch Thread but is called from the ProcessTraceLibpcap
    // thread because the showResults methods perform Event Dispatch Thread
    // specific tasks.
    @Override
    public void run()
    {
        if (error)
        {
            // Return the UI the it's original state
            modelButton.setDisable(false);
            networkButton.setDisable(false);
            deviceButton.setDisable(false);
            traceButton.setDisable(false);
            progressBar.setVisible(false);
            errorText.setText("");
            return;
        }
        
        errorText.setText("Modelling states...");
        progressBar.setProgress(0.75);
        // Keeping the engine in the main FormController because the Engine object
        // would also contain all of the data that would have to be passed
        // to instanciate the object in the ResultsFormController.
        System.out.println("MainFormController, Packets: "+packetList.size()+" IPsrc: "+sourceIP);
        switch (type)
        {
            case "3G":
            {
                Engine3G engine = new Engine3G(packetList,
                        sourceIP,
                        //networkProperties instanced as Properties3G
                        ((Properties3G)networkProperties), 
                        // deviceProperties instanced as PropertiesDevice3G
                        ((PropertiesDevice3G)deviceProperties));
                // Runs the modelling and calculates the total power
                engine.modelStates();
                engine.calculatePower();
                errorText.setText("Opening...");
                progressBar.setProgress(0.99);
                // Opens a new ResultsForm window and passes appropriate engine
                showResultsForm3G(engine);
                
                // Return the UI to it's original state
                modelButton.setDisable(false);
                networkButton.setDisable(false);
                deviceButton.setDisable(false);
                traceButton.setDisable(false);
                progressBar.setVisible(false);
                errorText.setText("");
            }
            break;

            case "Wifi":
            {
                EngineWifi engine = new EngineWifi(packetList,
                        sourceIP,
                        //networkProperties instanced as Properties3G
                        ((PropertiesWifi)networkProperties), 
                        // deviceProperties instanced as PropertiesDevice3G
                        ((PropertiesDeviceWifi)deviceProperties));
                // Runs the modelling and calculates the total power
                engine.modelStates();
                engine.calculatePower();
                errorText.setText("Opening...");
                progressBar.setProgress(0.99);
                // Opens a new ResultsForm window and passes appropriate engine
                showResultsFormWifi(engine);
                
                // Return the UI the it's original state
                modelButton.setDisable(false);
                networkButton.setDisable(false);
                deviceButton.setDisable(false);
                traceButton.setDisable(false);
                progressBar.setVisible(false);
                errorText.setText("");
            }
        }
    }
    
    @FXML
    private void handleDeviceButton(ActionEvent event)
    {
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        String path = OSTools.getJarLocation();
        fileChooser.setInitialDirectory((new File(path)).getParentFile());
        fileChooser.setTitle("Open Device Configuration File");
        File file = fileChooser.showOpenDialog(stage);
        try
        {
            Properties properties = fileToProperties(file);
            deviceField.setStyle("-fx-border-width: 0;");
            // deviceProperties is initiated the same way as networkProperties
            if (properties.getProperty("TYPE") == null)
            {
                OSTools.showErrorDialog("Config File Error!", "Not a valid config file");
                throw new NullPointerException();
            }
            switch (properties.getProperty("TYPE"))
            {
                case "Device3G": 
                {
                    String propError = validate(PropertiesDevice3G.class.getFields(), properties);
                    if (!propError.equals(""))
                    {
                        OSTools.showErrorDialog("Config File Error!", propError);
                        throw new NullPointerException();
                    }
                    deviceProperties = new PropertiesDevice3G(properties);
                }
                break;

                case "DeviceWifi": 
                {
                    String propError = validate(PropertiesDeviceWifi.class.getFields(), properties);
                    if (!propError.equals(""))
                    {
                        OSTools.showErrorDialog("Config File Error!", propError);
                        throw new NullPointerException();
                    }
                    deviceProperties = new PropertiesDeviceWifi(properties);
                }
                break;
                    
                default:
                {
                    OSTools.showErrorDialog("Config File Error!", "Not a valid config type");
                    // No idea why you can't just throw an exception
                    if (true) throw new NullPointerException();
                }
                break;
            }
            deviceField.setText(file.getName());
        }
        catch(NullPointerException e){ deviceField.setText(""); }
    }
    
    @FXML
    private void handleNetworkButton(ActionEvent event)
    {
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        String path = OSTools.getJarLocation();
        fileChooser.setInitialDirectory((new File(path)).getParentFile());
        fileChooser.setTitle("Open Network Configuration File");
        File file = fileChooser.showOpenDialog(stage);
        try
        {
            Properties properties = fileToProperties(file);
            networkField.setStyle("-fx-border-width: 0;");
            // networkProperties is initiated with the constructor of the appropriate
            // class depending on the TYPE of the .config file
            if (properties.getProperty("TYPE") == null)
            {
                OSTools.showErrorDialog("Config File Error!", "Not a valid config file");
                throw new NullPointerException();
            }
            switch (properties.getProperty("TYPE"))
            {
                case "3G": 
                {
                    String propError = validate(Properties3G.class.getFields(), properties);
                    if (!propError.equals(""))
                    {
                        OSTools.showErrorDialog("Config File Error!", propError);
                        throw new NullPointerException();
                    }
                    networkProperties = new Properties3G(properties);
                }
                break;

                case "Wifi": 
                {
                    String propError = validate(PropertiesWifi.class.getFields(), properties);
                    if (!propError.equals(""))
                    {
                        OSTools.showErrorDialog("Config File Error!", propError);
                        throw new NullPointerException();
                    }
                    networkProperties = new PropertiesWifi(properties);
                }
                break;
                    
                default:
                {
                    OSTools.showErrorDialog("Config File Error!", "Not a valid config type");
                    // No idea why you can't just throw an exception
                    if (true) throw new NullPointerException();
                }
                break;
            }
            type = properties.getProperty("TYPE");
            networkField.setText(file.getName());
        }
        catch(NullPointerException e){ networkField.setText(""); }
    }
    
    @FXML
    private void handleTraceButton(ActionEvent event)
    {
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        String path = OSTools.getJarLocation();
        fileChooser.setInitialDirectory((new File(path)).getParentFile());
        fileChooser.setTitle("Open Network Configuration File");
        try
        {
            File file = fileChooser.showOpenDialog(stage);
            tracePath = file.getAbsolutePath();
            textField.setText(file.getName());
            textField.setStyle("-fx-border-width: 0;");
        }
        catch(NullPointerException e){ textField.setText(""); }
    }
    
    // method to launch the ResultsFormController with the custom init method
    public Stage showResultsForm3G(Engine3G engine) 
    {
        try
        {
            // Creates stage from loader which gets the scene from the fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("ResultsForm3G.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene((Parent)loader.load()));
            stage.setTitle(textField.getText());
            stage.getIcons().add(new Image("img/icon.png"));

            // Calls a method on the controller to initialize it with the required data values
            ResultsForm3GController controller = 
            loader.<ResultsForm3GController>getController();
            controller.initData(engine);
            stage.show();
            return stage;
        }
        catch (IOException e){ e.printStackTrace(); }
        return null;
    }
    
    // method to launch the ResultsFormWifiController with the custom init method
    public Stage showResultsFormWifi(EngineWifi engine) 
    {
        try
        {
            // Creates stage from loader which gets the scene from the fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("ResultsFormWifi.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene((Parent)loader.load()));
            stage.setTitle(textField.getText());
            stage.getIcons().add(new Image("img/icon.png"));

            // Calls a method on the controller to initialize it with the required data values
            ResultsFormWifiController controller = 
            loader.<ResultsFormWifiController>getController();
            controller.initData(engine);
            stage.show();
            return stage;
        }
        catch (IOException e){ e.printStackTrace(); }
        return null;
    }

    // Deprecated! Only used for testing
    public Properties pathToProperties(String path)
    {
        Properties properties = new Properties();
        try
        {
            File f = new File(path);
            InputStream in = new FileInputStream (f);
            properties.load(in);
        }
        catch (IOException e){ e.printStackTrace(); }
        return properties;
    }
    
    public Properties fileToProperties(File f)
    {
        Properties properties = new Properties();
        try
        {
            InputStream in = new FileInputStream (f);
            properties.load(in);
        }
        catch (IOException e){ e.printStackTrace(); }
        return properties;
    }
    private void checkFieldError(TextField field)
    {
        if (field.getText().equals("")) 
        {
            field.setStyle(fieldError);
            error = true;
        }
    }
    
    private String validate(Field[] fields, Properties properties)
    {
        String propError = "";
        ArrayList<String> missingFields = new ArrayList();
        for (Field field : fields)
        {
            if (properties.getProperty(field.getName()) == null)
            {
                missingFields.add(field.getName());
            }
        }

        if (!missingFields.isEmpty())
        {
            propError += "Configuration of type '" + properties.getProperty("TYPE")
                    + "' is missing properties: \n" + missingFields.toString();
        }
        
        return propError;
    }

    @Override
    public void updateProgress(double progress) {
        this.progressBar.setProgress(progress);
    }
}
