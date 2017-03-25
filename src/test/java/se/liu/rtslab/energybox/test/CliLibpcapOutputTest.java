package se.liu.rtslab.energybox.test;

import org.junit.BeforeClass;
import se.liu.rtslab.energybox.OSTools;
import se.liu.rtslab.energybox.ProcessTrace;
import se.liu.rtslab.energybox.ProcessTraceLibpcap;
import se.liu.rtslab.energybox.UpdatesController;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assume.assumeTrue;


public class CliLibpcapOutputTest extends CliOutputTest {
    @Override
    public ProcessTrace getImplementation(Map<String, String> flags, UpdatesController updater) {
        return new ProcessTraceLibpcap(flags.get("t"), updater);
    }

    @BeforeClass
    public static void skipUnlessWindows() {
        final boolean isWindows = OSTools.isWindows();
        assumeTrue(isWindows);
        if (!isWindows) {
            Logger.getLogger(CliLibpcapOutputTest.class.getName()).
                    warning("Skipping libpcap tests -- not a UNIX system");
        }
    }
}
