package energybox;

import energybox.engines.*;
import energybox.properties.device.*;
import energybox.properties.network.*;
import java.awt.Dialog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialogs;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.swing.JOptionPane;
import org.jnetpcap.winpcap.WinPcap;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class MainFormController implements Initializable, Runnable
{
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
    String tracePath;
    String type; // for engine selection
    // Two possible criteria that can indicate the source IP:
    // -> "HTTP" - a phone is usually not running a web server, so any GET requests are from the the recording device
    // -> "DNS" - a phone is usually not running a DNS server so any requests are from the the recording device 
    HashMap<String, String> criteria = new HashMap();
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
        image.setImage(new Image("/energybox/gears.png", true));
        // Checks between the two supported operating systems and tries to add
        // the directory where the JAR file is locted to the PATH or CLASSPATH
        // variables in the JVM.
        String os = OSTools.getOS();
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
                    Dialogs.showErrorDialog(null, e.getMessage());
                }
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
                    JOptionPane.showMessageDialog(null, e.getMessage());
                }
                try { WinPcap.isSupported(); }
                catch (UnsatisfiedLinkError e)
                {
                    Dialogs.showErrorDialog(null, "Libpcap-dev not installed!");
                }
            }
            break;
        }
        
        // Default 3G values for testing
        /*
        tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\test\\\\test1UL.pcap";
        textField.setText("test1UL.pcap");
        //ipField.setText("10.209.43.104");
        type = "3G";
        Properties properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\device_3g.config");
        deviceProperties = new PropertiesDevice3G(properties);
        properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\3g_teliasonera.config");
        networkProperties = new Properties3G(properties);
        
        // Default Wifi values for testing
        //tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\test\\\\round2good.pcap";
        //textField.setText("round2good.pcap");
        tracePath = "D:\\\\Source\\\\NetBeansProjects\\\\EnergyBox\\\\test\\\\random31.pcap";
        textField.setText("random31.pcap");
        type = "Wifi";
        Properties properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\samsungS2_wifi.config");
        deviceField.setText("samsungS2_wifi.config");
        deviceProperties = new PropertiesDeviceWifi(properties);
        properties = pathToProperties("D:\\Source\\NetBeansProjects\\EnergyBox\\test\\wifi_general.config");
        networkField.setText("wifi_general.config");
        networkProperties = new PropertiesWifi(properties);*/
        
    }
    
    @FXML
    private void handleButtonAction(ActionEvent event)
    {
        modelButton.setDisable(true);
        networkButton.setDisable(true);
        deviceButton.setDisable(true);
        traceButton.setDisable(true);
        
        progressBar.visibleProperty().set(true);
        errorText.setText("Loading trace...");
        error = false;
        (new Thread(new ProcessTrace(this))).start();
    }
    
    // Executes in the Event Dispatch Thread but is called from the ProcessTrace
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
        fileChooser.setInitialDirectory((new File(path)).getParentFile().getParentFile());
        fileChooser.setTitle("Open Device Configuration File");
        File file = fileChooser.showOpenDialog(stage);
        Properties properties = fileToProperties(file);
        
        // deviceProperties is initiated the same way as networkProperties
        switch (properties.getProperty("TYPE"))
        {
            case "Device3G": deviceProperties = new PropertiesDevice3G(properties);
            break;
                
            case "DeviceWifi": deviceProperties = new PropertiesDeviceWifi(properties);
            break;
        }
        deviceField.setText(file.getName());
    }
    
    @FXML
    private void handleNetworkButton(ActionEvent event)
    {
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        String path = OSTools.getJarLocation();
        fileChooser.setInitialDirectory((new File(path)).getParentFile().getParentFile());
        fileChooser.setTitle("Open Network Configuration File");
        File file = fileChooser.showOpenDialog(stage);
        Properties properties = fileToProperties(file);
        // networkProperties is initiated with the constructor of the appropriate
        // class depending on the TYPE of the .config file
        switch (properties.getProperty("TYPE"))
        {
            case "3G": networkProperties = new Properties3G(properties);
            break;
                
            case "Wifi": networkProperties = new PropertiesWifi(properties);
            break;
        }
        type = properties.getProperty("TYPE");
        networkField.setText(file.getName());
    }
    
    @FXML
    private void handleTraceButton(ActionEvent event)
    {
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        String path = OSTools.getJarLocation();
        fileChooser.setInitialDirectory((new File(path)).getParentFile().getParentFile());
        fileChooser.setTitle("Open Network Configuration File");
        File file = fileChooser.showOpenDialog(stage);
        
        tracePath = file.getAbsolutePath();
        textField.setText(file.getName());
    }
    
    // method to launch the ResultsFormController with the custom init method
    public Stage showResultsForm3G(Engine3G engine) 
    {
        try
        {
            // Creates stage from loader which gets the scene from the fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ResultsForm3G.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene((Parent)loader.load()));
            stage.setTitle(textField.getText());

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ResultsFormWifi.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene((Parent)loader.load()));
            stage.setTitle(textField.getText());

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

    // Depricated! Only used for testing    
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
}
