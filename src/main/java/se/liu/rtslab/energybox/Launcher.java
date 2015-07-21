package se.liu.rtslab.energybox;

public class Launcher {

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
            launchCli(args);
        } else {
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


        final UpdatesController updater = new NullUpdater();
        final ProcessTrace trace = ProcessTrace.Factory.getInstance(flagT, updater);

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

}
