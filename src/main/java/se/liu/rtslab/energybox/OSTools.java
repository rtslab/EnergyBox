package se.liu.rtslab.energybox;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import javax.swing.JOptionPane;

import javafx.scene.control.Alert;

/**
 * @author Rihards Polis
 * Linkoping University
 */

public class OSTools
{
    private OSTools() {}

    private static String OS = null;

    public static String getOS(){
        if (OS == null) {
            String systemOS = System.getProperty("os.name").toLowerCase();
            OS = OSFamily(systemOS);
        }
        return OS;
    }

    private static String OSFamily(String os)
    {
        if (isWindows(os))
            return "Windows";
        else if (isMac(os))
            return "Mac";
        else if (isUnix(os))
            return "Linux";
        else if (isSolaris(os))
            return "Solaris";
        else 
            throw new IllegalArgumentException("Could not determine OS family of " + os);
    }

    private static boolean isWindows(String os) { return os.toLowerCase().contains("win"); }
    public static boolean isWindows() { return isWindows(getOS()); }

    private static boolean isMac(String os) { return os.toLowerCase().contains("mac"); }
    public static boolean isMac() { return isMac(getOS()); }

    private static boolean isUnix(String os) {
        String osLower = os.toLowerCase();
        return osLower.contains("nix") || osLower.contains("nux") || osLower.indexOf("aix") > 0;
    }
    public static boolean isUnix() { return isUnix(getOS()); }

    private static boolean isSolaris(String os) { return os.contains("sunos"); }
    public static boolean isSolaris() { return isSolaris(getOS()); }
    
    public static void addDirectory(String s) throws IOException 
    {
        try 
        {
            // This enables the java.library.path to be modified at runtime
            // From a Sun engineer at forums.sun.com
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            String[] paths = (String[])field.get(null);
            for (int i = 0; i < paths.length; i++) 
            {
                if (s.equals(paths[i])) 
                    return;
            }
            String[] tmp = new String[paths.length+1];
            System.arraycopy(paths,0,tmp,0,paths.length);
            tmp[paths.length] = s;
            field.set(null,tmp);
            System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
        } 
        catch (IllegalAccessException e) { throw new IOException("Failed to get permissions to set library path"); } 
        catch (NoSuchFieldException e) { throw new IOException("Failed to get field handle to set library path"); }
    }
    public static String getJarLocation()
    {
        String path = MainFormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = "";
        try { decodedPath = URLDecoder.decode(path, "UTF-8"); }
        catch (UnsupportedEncodingException e){ e.printStackTrace(); }
        return decodedPath;
    }
    
    public static void showErrorDialog(String title, String message)
    {
        try
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(null, message);
        }
    }

    public static void showNumberErrorDialog() {
        showErrorDialog("Not a number", "Please input a number with decimal seperator '.'");
    }
}
