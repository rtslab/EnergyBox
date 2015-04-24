package energybox;

import java.util.Map;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
        Map<String, String> flags = this.getParameters().getNamed();
        // If there are no command line arguments, the application launches the
        // graphical interface.
        if (this.getParameters().getRaw().isEmpty())
        {
            Parent root = FXMLLoader.load(getClass().getResource("MainForm.fxml"));

            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("EnergyBox");
            stage.setResizable(false);
            stage.getIcons().add(new Image("/energybox/img/icon.png"));
            stage.show();
        }
        // Otherwise it launches the terminal version
        else if (flags.containsKey("t") && flags.containsKey("n") && flags.containsKey("d"))
        {
            final UpdatesController updater = new NullUpdater();
            final ProcessTrace trace = OSTools.isMac() ?
                    new ProcessTraceOSX(flags.get("t"), updater) :
                    new ProcessTraceLibpcap(flags.get("t"), updater);

            trace.run(); // block thread to avoid race conditions
            ConsoleBox consoleBox = new ConsoleBox(trace, flags.get("t"), flags.get("n"), flags.get("d"));
            consoleBox.printResults();
            if (flags.containsKey("f"))
            {
                consoleBox.outputToFile(flags.get("f"));
                System.out.println("States saved in file '" + flags.get("f") + "'");
            }
            Platform.exit();
            this.stop();
        }
        else if (!this.getParameters().getRaw().isEmpty())
        {
            System.out.println("To run the the application from the command line add flags:");
            System.out.println("--t=<trace file path>");
            System.out.println("--n=<network configuration file path>");
            System.out.println("--d=<device configuration file path>");
            System.out.println("Optional flags:");
            System.out.println("--f=<path to output file>");
            Platform.exit();
            this.stop();
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
