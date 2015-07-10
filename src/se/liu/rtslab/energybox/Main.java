package se.liu.rtslab.energybox;

public class Main {

    public static void main(String[] args) {
        final String tracePath = args[0];
        final String networkConfig = "3g_teliasonera.config";
        final String deviceConfig = "nexus_one_3g.config";

        ProcessTrace processTrace = new ProcessTraceOSX(tracePath, new NullUpdater());
        processTrace.run(); // Process the trace synchronously
        ConsoleBox consoleBox = new ConsoleBox(processTrace, networkConfig, deviceConfig);

        double energy = consoleBox.getTotalPower();
        System.out.println("Energy: " + energy);
    }
}
