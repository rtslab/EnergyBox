package se.liu.rtslab.energybox.test;

import se.liu.rtslab.energybox.ProcessTrace;
import se.liu.rtslab.energybox.ProcessTraceLibpcap;
import se.liu.rtslab.energybox.UpdatesController;

import java.util.Map;

public class CliLibpcapOutputTest extends CliOutputTest {
    @Override
    public ProcessTrace getImplementation(Map<String, String> flags, UpdatesController updater) {
        return new ProcessTraceLibpcap(flags.get("t"), updater);
    }
}
