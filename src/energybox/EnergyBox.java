package energybox;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
/**
 * @author Rihards Polis
 * Linkoping University
 */
public class EnergyBox extends Application
{
    
    @Override
    public void start(Stage stage) throws Exception
    {
        String[] testArgs = new String[] {"D:\\Source\\NetBeansProjects\\EnergyBox\\test\\test1UL.pcap", "D:\\Source\\NetBeansProjects\\EnergyBox\\test\\3g_teliasonera.config", "D:\\Source\\NetBeansProjects\\EnergyBox\\test\\device_3g.config"};
        // If there are no command line arguments, the application launches the
        // graphical interface.
        
        if (this.getParameters().getUnnamed().isEmpty())
        {
            Parent root = FXMLLoader.load(getClass().getResource("Forms.fxml"));

            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("EnergyBox");
            stage.show();
        }
        // Otherwise it launches the terminal version
        else
        {
            //List<String> args = this.getParameters().getUnnamed();
            //ConsoleBox consoleBox = new ConsoleBox(args.get(0), args.get(1), args.get(2));
        }
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        launch(args);
    }
}
