package energybox;

import javafx.application.Platform;

import java.util.Iterator;

public final class ControllerUpdater implements UpdatesController {
    private final MainFormController controller;

    public ControllerUpdater(MainFormController controller) {
        this.controller = controller;
    }

    public void invoke(ProcessTrace trace) {
        controller.sourceIP = trace.getSourceIP();
        controller.addressOccurrence = trace.getAddressOccurrence();
//            controller.criteria = trace.getCriteria();
        controller.packetList.clear();
        controller.packetList.addAll(trace.getPacketList());
        System.out.println("ProcessTraceOSX, IPsource: " + controller.sourceIP + " Criteria: "+ trace.getCriteria());

        if (trace.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> iter = trace.getErrorMessages().iterator();
            while (iter.hasNext()) {
                sb.append(iter.next());
                if(iter.hasNext()) sb.append("\n");
            }
            final String errorMessage = sb.toString();
            controller.errorText.setText(errorMessage);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    OSTools.showErrorDialog("Error", errorMessage);
                }
            });
        }
        Platform.runLater(controller);
    }
}
