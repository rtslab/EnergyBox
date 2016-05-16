package se.liu.rtslab.energybox;

import javafx.application.Application;
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
        Parent root = FXMLLoader.load(this.getClass().getClassLoader().getResource("MainForm.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("EnergyBox");
        stage.setResizable(false);
        stage.getIcons().add(new Image("img/icon.png"));
        stage.show();
    }
}
