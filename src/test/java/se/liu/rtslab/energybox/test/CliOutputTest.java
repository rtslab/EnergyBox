package se.liu.rtslab.energybox.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import se.liu.rtslab.energybox.ConsoleBox;
import se.liu.rtslab.energybox.NullUpdater;
import se.liu.rtslab.energybox.ProcessTrace;
import se.liu.rtslab.energybox.UpdatesController;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


public abstract class CliOutputTest {
    private File resources;
    private File configs;

    private enum TYPE {TYPE_3G, TYPE_WIFI}

    public abstract ProcessTrace getImplementation(Map<String, String> flags, UpdatesController updater);

    @Before
    public void setUp() throws Exception {
        URL resUrl = this.getClass().getClassLoader().getResource("");
        assertNotNull("Resource path was null. Did you run with gradle? (command is `gradle test`)", resUrl);
        this.resources = FileUtils.getFile(resUrl.getPath());
        this.configs = FileUtils.getFile(this.resources, "config");
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.forceDelete(getTempFile());
    }

    @Test
    public void test3G_1() throws IOException {
        testOutput(1, getPcap("test1.pcap"), TYPE.TYPE_3G);
    }

    @Test
    public void test3G_2() throws IOException {
        testOutput(2, getPcap("test2.pcap"), TYPE.TYPE_3G);
    }

    @Test
    public void testWifi_1() throws IOException {
        testOutput(1, getPcap("test1.pcap"), TYPE.TYPE_WIFI);
    }

    @Test
    public void testWifi_2() throws IOException {
        testOutput(2, getPcap("test2.pcap"), TYPE.TYPE_WIFI);
    }

    private void testOutput(int number, File pcap, TYPE type) throws IOException {
        File networkConfig = getConfigNetwork(type);
        File deviceConfig = getConfigDevice(type);

        Map<String, String> flags = new HashMap<>();
        Collection<File> files = FileUtils.listFiles(resources, FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
        flags.put("t", pcap.getAbsolutePath());
        flags.put("n", networkConfig.getAbsolutePath());
        flags.put("d", deviceConfig.getAbsolutePath());
        flags.put("f", getTempFile().getAbsolutePath());

        final UpdatesController updater = new NullUpdater();
        final ProcessTrace trace = getImplementation(flags, updater);
        trace.run();
        ConsoleBox consoleBox = new ConsoleBox(trace, flags.get("t"), flags.get("n"), flags.get("d"));
        consoleBox.printResults();

        if (flags.containsKey("f"))
        {
            consoleBox.outputToFile(flags.get("f"));
            System.out.println("States saved in file '" + flags.get("f") + "'");
        }

        File oracleCsv = getOracleCsv(number, type);
        System.out.println(String.format("Using oracle: %s", oracleCsv.getAbsolutePath()));
        File output = FileUtils.getFile(flags.get("f"));

        LineIterator itOracle = FileUtils.lineIterator(oracleCsv);
        LineIterator itOutput = FileUtils.lineIterator(output);

        // Oracle is generated using GUI.
        // CLI version prints source IP on line 1, Total power on line 2.
        assertEquals("First line is not source IP", trace.getSourceIP(), itOutput.nextLine());
        // Expected power has been rounded.
        final double delta = 1e-4;
        assertEquals("Second line is not total power",
                consoleBox.getTotalPower(),
                Double.parseDouble(itOutput.nextLine()), delta);


        // Compare state output
        // States start at output line 3
        int outputLine = 3;
        try {
            while (itOracle.hasNext()) {
                String expected = itOracle.nextLine();
                String actual = itOutput.nextLine();
                outputLine++;
                assertLineMatches(outputLine, expected, actual);
            }
        } finally {
            itOracle.close();
            itOutput.close();
        }
        assertFalse("Did not reach end of file. Stopped at line " + outputLine, itOracle.hasNext() || itOutput.hasNext());
    }

    // Allow some (1e-5) errors because for numerical reasons/different pcap libraries.
    private void assertLineMatches(int lineNum, String expected, String actual) {
        final String lineRegex = "^[0-3],[0-9]+.[0-9]+$";
        assertTrue(String.format("Input line %d does not match regex /%s/", lineNum, lineRegex),
                actual.matches(lineRegex));
        final String[] expectedSplit = expected.split(",");
        final String[] actualSplit = actual.split(",");
        assertEquals("Mismatch at line " + lineNum, expectedSplit[0], actualSplit[0]);
        assertEquals("Mismatch at line " + lineNum, Double.parseDouble(expectedSplit[1]), Double.parseDouble(actualSplit[1]), 1e-5);
    }

    private int parseState(String line) {
        return Integer.parseInt(line.split(",")[0]);
    }

    private double parseTime(String line) {
        return Double.parseDouble(line.split(",")[1]);
    }

    private File getConfigNetwork(TYPE type) {
        switch (type) {
            case TYPE_3G:
                return FileUtils.getFile(configs, "3g_teliasonera.config");
            case TYPE_WIFI:
                return FileUtils.getFile(configs, "wifi_general.config");
        }
        throw new IllegalArgumentException();
    }

    private File getConfigDevice(TYPE type) {
        switch (type) {
            case TYPE_3G:
                return FileUtils.getFile(configs, "device_3g.config");
            case TYPE_WIFI:
                return FileUtils.getFile(configs, "samsungS2_wifi.config");
        }
        throw new IllegalArgumentException();
    }

    private File getPcap(String filename) {
        return FileUtils.getFile(resources, filename);
    }

    private File getTempFile() {
        return FileUtils.getFile(resources, "temp.csv");
    }

    private File getOracleCsv(int number, TYPE type) {
        StringBuilder sb = new StringBuilder("oracle/test");
        sb.append(number);
        sb.append("_");
        switch (type) {
            case TYPE_3G:
                sb.append("3g");
                break;
            case TYPE_WIFI:
                sb.append("wifi");
                break;
            default:
                throw new IllegalArgumentException();
        }
        sb.append(".csv");
        return FileUtils.getFile(resources, sb.toString());
    }
}
