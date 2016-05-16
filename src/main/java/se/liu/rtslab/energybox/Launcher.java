package se.liu.rtslab.energybox;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.jnetpcap.winpcap.WinPcap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Launcher {

    private static String os;
    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            //The environment is set later to deal with the input arguments
            launchCli(args);
        } else {
            setEnvironment();
            launchGui(args);
        }
    }

    public static void launchCli(String[] args) {
        String flagT = null;
        String flagN = null;
        String flagD = null;
        try {
            flagT = Launcher.parseParam("--t", args);
            flagD = Launcher.parseParam("--d", args);
            flagN = Launcher.parseParam("--n", args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printCliUsage();
            System.exit(1);
        }

        // ToDo: add the relative path in order to find the jnetpcap.dll in Windows
/*        String location = Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedLocation = "";
        try { decodedLocation = URLDecoder.decode(location, "UTF-8"); }
        catch (UnsupportedEncodingException e){ e.printStackTrace(); }
        StringBuilder jarPath = new StringBuilder();
        jarPath.append(new File(decodedLocation).getParent());
        jarPath.append(File.separator);
        try { OSTools.addDirectory(jarPath.toString()); }
        catch (IOException e)
        {
            OSTools.showErrorDialog("JVM path error!", e.getMessage());
        }
        System.out.println("MFC: "+jarPath.toString());*/
        setEnvironment();

        final UpdatesController updater = new NullUpdater();
        final ProcessTrace trace = ProcessTrace.Factory.build(flagT, updater,os);



        trace.run(); // block thread to avoid race conditions
        ConsoleBox consoleBox = new ConsoleBox(trace, flagT, flagN, flagD);
        consoleBox.printResults();

        String flagF = null;
        try {
            flagF = Launcher.parseParam("--f", args);
        } catch (IllegalArgumentException expected) {
            // Expected: only output to stdout when parameter "--f" is not set
        }
        if (flagF != null) {
            consoleBox.outputToFile(flagF);
            System.out.println("States saved in file '" + flagF + "'");
        }
    }

    public static void launchGui(String[] args) {
        javafx.application.Application.launch(EnergyBox.class, args);
    }

    /**
     * Parses args array for CLI arguments matching '--key=value'.
     *
     * @param key the key, e.g "--d"
     * @param args the array of arguments
     * @throws IllegalArgumentException if key cannot be found
     * @return the value corresponding to the key
     */
    private static String parseParam(String key, String[] args) {
        String start = key + "=";
        for (String arg : args) {
            if (arg.startsWith(start)) {
                return arg.substring(4);
            }
        }
        throw new IllegalArgumentException(String.format("Parameter %s not set (usage: %s=value)", key, key));
    }

    private static void printCliUsage() {
        System.out.println("To run the the application from the command line add flags:");
        System.out.println("--t=<trace file path>");
        System.out.println("--n=<network configuration file path>");
        System.out.println("--d=<device configuration file path>");
        System.out.println("Optional flags:");
        System.out.println("--f=<path to output file>");
    }

    private static void setEnvironment() {
        OSTools.checkOS();
        os = OSTools.getOS();
        //System.out.println("Operating system: " + os);
        switch (os) {
            case "Windows": {
                String location = MainFormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedLocation = "";
                try {
                    decodedLocation = URLDecoder.decode(location, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                StringBuilder relativePath = new StringBuilder();
                relativePath.append(new File(decodedLocation).getParent());
                relativePath.append(File.separator);
                try {
                    OSTools.addDirectory(relativePath.toString());
                } catch (IOException e) {
                    OSTools.showErrorDialog("JVM path error!", e.getMessage());
                }
                //System.out.println("EnergyBox executing in: " + relativePath.toString());
            }
            break;

            case "Linux": {
                String location = MainFormController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedLocation = "";
                try {
                    decodedLocation = URLDecoder.decode(location, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                StringBuilder relativePath = new StringBuilder();
                relativePath.append(new File(decodedLocation).getParent());
                relativePath.append(File.separator);
                try {
                    OSTools.addDirectory(relativePath.toString());
                } catch (IOException e) {
                    OSTools.showErrorDialog("JVM Path Error", e.getMessage());
                }
                try {
                    WinPcap.isSupported();
                } catch (UnsatisfiedLinkError e) {
                    OSTools.showErrorDialog("Libpcap Error", "Libpcap-dev not installed!");
                }
                System.out.println("EnergyBox executing in: " + relativePath.toString());
            }
            break;

            case "Mac": {
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
                    int exitValue = executor.execute(cmdLine, EnvironmentUtils.getProcEnvironment());
                    answer = outputStream.toString();
                    //System.out.println("Exit value: "+exitValue);
                    System.out.println("MainController, tshark found: " + answer);
                } catch (IOException ex) {
                    Logger.getLogger(MainFormController.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (answer == null) {
                    //Tshark is not installed, or its not in the path, report it
                    System.err.println("Tshark is not installed or in the path");
                    //ToDo: Add a dialog "Tshark is not installed or in the path"
                    OSTools.showErrorDialog("MainFormController", "Tshark is not installed or in the path");
                    //JOptionPane.showMessageDialog(null, "Tshark is not installed or in the path");
                }
                //It does not work if its launched from NetBeans, launch from the terminal

                //http://commons.apache.org/proper/commons-exec/tutorial.html
                //http://www.coderanch.com/t/624006/java/java/tshark-giving-output
            }
            break;

        }
    }


}
