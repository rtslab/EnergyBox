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
        // Run the method that opens the results forms
        if (!controller.ipField.getText().equals(""))
        {
            controller.sourceIP = controller.ipField.getText();
        }

        if (trace.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> iter = trace.getErrorMessages().iterator();
            while (iter.hasNext()) {
                sb.append(iter.next());
                if(iter.hasNext()) sb.append("\n");
            }
            controller.errorText.setText(sb.toString());
        }
        Platform.runLater(controller);
    }
}
