package energybox.test;

import energybox.ProcessTrace;
import energybox.ProcessTraceLibpcap;
import energybox.UpdatesController;

import java.util.Map;

public class CliLibpcapOutputTest extends CliOutputTest {
    @Override
    public ProcessTrace getImplementation(Map<String, String> flags, UpdatesController updater) {
        return new ProcessTraceLibpcap(flags.get("t"), updater);
    }
}
