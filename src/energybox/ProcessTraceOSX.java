package energybox;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.swing.JOptionPane;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;

/**
 * @author Ekhiotz Vergara
 * Linkoping University
 */
public class ProcessTraceOSX implements Runnable
{
    MainFormController controller;
    long recordsProcessed = 0, totalRecords = 0;
    int i = 0; // For the progress update.
    private final boolean debug = true;
    
    ProcessTraceOSX(MainFormController controller)
    {
        this.controller = controller;
    }
    
    @Override
    public void run() 
    {
        // Clear variables in case the button was used before
        controller.sourceIP = "";
        controller.addressOccurrence.clear();
        controller.criteria.clear(); 
        // Error buffer for file handling
        StringBuilder errbuf = new StringBuilder();
        // Wrapped lists in JavaFX ObservableList for the table view
        
        //controller.errorText.setText("");
        controller.packetList.clear();
        
        System.out.println(controller.tracePath);
        
        //Run tshark to create the csv file that you will access using the StreamHandler
        //cannot execute > (output redirection)
        //http://darthanthony.wordpress.com/2009/06/19/running-a-command-line-executable-in-java-with-redirected-output/
        
        //tshark -r file.pcap -T fields -E separator=, -e frame.number 
        //frame.time_relative frame.len ip.src  ip.dst  ip.version  ip.proto http.request dns.qry.name > packetrace.csv
        //https://www.wireshark.org/docs/dfref/i/ip.html
        //https://www.wireshark.org/docs/dfref/h/http.html
        //https://www.wireshark.org/docs/dfref/d/dns.html
        //DNS queries: http://www.netresec.com/?page=Blog&month=2012-06&post=Extracting-DNS-queries
        String csvFile = null; //it is just accessed in memory
        final CommandLine cmdLine = new CommandLine("tshark");
        cmdLine.addArgument("-r");
        cmdLine.addArgument(controller.tracePath); //THIS COMES FROM THE CONTROLLER!!!
        //Check that it does not contain spaces or similar
        cmdLine.addArgument("-T");
        cmdLine.addArgument("fields");
        cmdLine.addArgument("-E");
        cmdLine.addArgument("separator=,");
        cmdLine.addArgument("-e");
        cmdLine.addArgument("frame.number"); //0
        cmdLine.addArgument("-e");
        cmdLine.addArgument("frame.time_relative"); //1
        cmdLine.addArgument("-e");
        cmdLine.addArgument("frame.len"); //2
        cmdLine.addArgument("-e");
        cmdLine.addArgument("ip.src"); //3
        cmdLine.addArgument("-e");
        cmdLine.addArgument("ip.dst"); //4
        cmdLine.addArgument("-e");
        cmdLine.addArgument("ip.version"); //5
        cmdLine.addArgument("-e");
        cmdLine.addArgument("_ws.col.Protocol"); //6
        cmdLine.addArgument("-e");
        cmdLine.addArgument("http.request"); //7
        cmdLine.addArgument("-e");
        cmdLine.addArgument("dns.qry.name"); //8
        cmdLine.addArgument("-e");
        cmdLine.addArgument("dns.flags.response"); //9 - if 0 query
        cmdLine.addArgument("-e");
        cmdLine.addArgument("ip.proto"); //10 - 6 TCP, 17 UDP
        cmdLine.addArgument("-e");
        cmdLine.addArgument("tcp.port"); //11
        cmdLine.addArgument("-e");
        cmdLine.addArgument("tcp.srcport"); //12
        cmdLine.addArgument("-e");
        cmdLine.addArgument("udp.port"); //13
        cmdLine.addArgument("-e");
        cmdLine.addArgument("udp.srcport"); //14
        //cmdLine.addArgument("-e");
        //cmdLine.addArgument("_ws.col.Info");
        
        /*tshark -r http2.pcap -T fields -E separator=, -e frame.number -e frame.time_relative -e frame.len -e ip.src -e ip.dst -e ip.version 
                -e ip.proto -e http.request -e dns.qry.name -e dns.flags.response -e tcp.port -e tcp.port -e udp.port 
                        -e udp.srcport -e _ws.col.Info -e _ws.col.Protocol */
        
        /*cmdLine.addArgument("dns.qry.name");
        cmdLine.addArgument("-Y");
        cmdLine.addArgument("dns.flags.response eq 0",false);*/
        System.out.println(cmdLine.toString());

        //-e dns.qry.name -R "dns.flags.response eq 0"
        //WHERE IS THIS BEING EXECUTED???? use pwd maybe
        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //Handle the output of the program
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);
        try {
            int exitValue = executor.execute(cmdLine,EnvironmentUtils.getProcEnvironment());
            csvFile=outputStream.toString();
            System.out.println("Exit value: "+exitValue);
            //System.out.println(csvFile);
        } catch (IOException ex) {
            Logger.getLogger(MainFormController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(csvFile);
        }
        
        
        //Read the csv file
        if(csvFile!=null){
            try {
                //Hashmaps used to count the number of HTTP requests of IPs
                //The number of DNS requests of IPs
                //The total number of IP occurences
                HashMap<String, Integer> HTTPrequest = new HashMap<String, Integer>();
                HashMap<String, Integer> DNSquery = new HashMap<String, Integer>();
                HashMap<String, Integer> IPlist = new HashMap<String, Integer>();
                //Record
                List<CSVRecord> records = CSVParser.parse(csvFile, CSVFormat.DEFAULT).getRecords();
                recordsProcessed=0;
                totalRecords=records.size();
                System.out.println(totalRecords);
                //IP detection needs to be done here
                for(CSVRecord record : records){
                    //[Number 0, Time 1, Length 2, Ip src 3, Ip dest 4, IPv 5, Ip proto 6, HTTPreq 7,DNSquery 8]
                    //[2489, 285.096593000, 56, 66.220.153.21, 95.194.8.196, 4, 6, , ]
                    System.out.println(record.toString());
                    //Packet(long time, int length, String source, String destination, String protocol)
                    Double time=Double.parseDouble(record.get(1));
                    time=time * 1000000;
                    controller.packetList.add(new Packet(
                        time.longValue(),
                        Integer.parseInt(record.get(2)),
                        record.get(3),
                        record.get(4),
                        record.get(6))); 
                        //ToDo: Add Port number and info             
                    
                    
                    //IP DETECTION - Populate the hashmaps - ToDo: make it a method
                    //Source
                    if(IPlist.containsKey(record.get(3))){
                        //increase the value
                        IPlist.put(record.get(3), IPlist.get(record.get(3)) + 1);
                    } else {
                        //add the value
                        IPlist.put(record.get(3), 1);
                    }    
                    //Dest
                    if(IPlist.containsKey(record.get(4))){
                        //increase the value
                        IPlist.put(record.get(4), IPlist.get(record.get(4)) + 1);
                    } else {
                        //add the value
                        IPlist.put(record.get(4), 1);
                    } 
                    //HTTP requests
                    if(record.get(7).equalsIgnoreCase("1")){
                        if(HTTPrequest.containsKey(record.get(3))){
                            //increase the value
                            HTTPrequest.put(record.get(3), HTTPrequest.get(record.get(3)) +1);
                        } else {
                            //add the value
                            HTTPrequest.put(record.get(3), 1);
                        }    
                    }
                    //DNS queries, only request
                    if(record.get(8).length() > 2 && record.get(9).equals("0") ){
                        if(DNSquery.containsKey(record.get(3))){
                            //increase the value
                            DNSquery.put(record.get(3), DNSquery.get(record.get(3)) +1);
                        } else {
                            //add the value
                            DNSquery.put(record.get(3), 1);
                        }    
                    }
                    
                    if(debug) System.out.println(IPlist.size()+" "+HTTPrequest.size()+" "+DNSquery.size());
                    
                    //Update progress bar
                    recordsProcessed++;
                    //Reading the packet file is 50% of the progress, 25% is to model the states, and the rest to run the GUI
                    final double progress = ((double)recordsProcessed / (double)totalRecords) / 2 ;
                    controller.progressBar.setProgress(progress);
                }
                
                /*for(Packet p : controller.packetList){
                    System.out.println(p.getTime()+" "+p.getLength()+" "+p.getSource()+" "+p.getDestination()+" "+p.getProtocol());
                }*/
                if(debug){
                    for (Entry<String, Integer> entry : HTTPrequest.entrySet()) {  // Iterate through hashmap
                        System.out.println("HTTP "+entry.getKey()+" "+entry.getValue());
                    }
                    for (Entry<String, Integer> entry : DNSquery.entrySet()) {  // Iterate through hashmap
                        System.out.println("DNS "+entry.getKey()+" "+entry.getValue());
                    }
                    for (Entry<String, Integer> entry : IPlist.entrySet()) {  // Iterate through hashmap
                        System.out.println("IP "+entry.getKey()+" "+entry.getValue());
                    }
                }
                
                //IP detection 
                //Avoid calculating 
                String criteria="none";
                if(!HTTPrequest.isEmpty()){
                    criteria="HTTP request";
                    Entry<String, Integer> maxEntry = null;
                    for (Entry<String, Integer> entry : HTTPrequest.entrySet()) {  // Iterate through hashmap
                        //System.out.println(entry.getKey()+" "+entry.getValue());
                        if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                        {
                            maxEntry = entry;
                        }
                    }
                    controller.sourceIP=maxEntry.getKey();
                } else if (!DNSquery.isEmpty()) {
                    criteria="DNS query";
                    Entry<String, Integer> maxEntry = null;
                    for (Entry<String, Integer> entry : DNSquery.entrySet()) {  // Iterate through hashmap
                        if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                        {
                            maxEntry = entry;
                        }
                    }
                    controller.sourceIP=maxEntry.getKey();
                    //most common is the ip
                } else {
                    //Calculate the most common IP
                    criteria="Common IP";
                    Entry<String, Integer> maxEntry = null;
                    for (Entry<String, Integer> entry : IPlist.entrySet()) {  // Iterate through hashmap
                        if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                        {
                            maxEntry = entry;
                        }
                    }
                    controller.sourceIP=maxEntry.getKey();

                }
                System.out.println("ProcessTraceOSX, IPsource: "+controller.sourceIP+" Criteria: "+criteria);
                
                
                                
                // Run the method that opens the results forms
                if (!controller.ipField.getText().equals(""))
                {
                    controller.sourceIP = controller.ipField.getText();
                }
                Platform.runLater(controller);
                
            } catch (IOException ex) {
                Logger.getLogger(ProcessTraceOSX.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("Error processing the trace");
            }
            
        } else {
            System.err.println("Could not read packet trace");
        }
    }
}
