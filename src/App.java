import java.io.*;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class App {
    public static Integer totalNodes;   //Total nodes in topology
    public static Node self;
    public static Map<Integer, Node> tempMap = new HashMap<Integer, Node>();
    public static Map<Integer, Node> nodeMap = new HashMap<Integer, Node>();        //Stores my neighbors
    public static BlockingQueue<Message> messagesToBeProcessed = new LinkedBlockingQueue<Message>();
    public static Integer meanInterReqDelay = 0;
    public static Integer meanCSExecTime = 0;
    public static Integer noRequestGenerated = 0;
    public static PriorityQueue<RequestObject> queue;
    public static Integer scalarClock = 0;

    public static void main(String args[]) {
        String line = null;
        String hostName = null;
        boolean[] lines;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            System.out.println("HOST " + hostName);
        } catch (IOException e1) {
            System.out.println("Error fetching host name!");
        }
        try {
            FileReader fileReader = new FileReader("configuration.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            int lineCount = 0, linesToRead = 0;
            int counter = 0;

            //Read file correctly
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                //System.out.println(">>"+line);
                if (line.length() == 0) {              //if empty line
                    continue;
                } else if (line.startsWith("#")) {    //if comment
                    continue;
                } else if (line.indexOf("#") != -1) {        //if comment in between line
                    line = line.split("#")[0];
                }
                if (line.startsWith("0") ||
                        line.startsWith("1") ||
                        line.startsWith("2") ||
                        line.startsWith("3") ||
                        line.startsWith("4") ||
                        line.startsWith("5") ||
                        line.startsWith("6") ||
                        line.startsWith("7") ||
                        line.startsWith("8") ||
                        line.startsWith("9")) {              // Required scenario. Work here.
                    //System.out.println(""+line+" "+lineCount+" "+linesToRead);
                    if (lineCount == 0) {
                        //get stuff for BS
                        String[] info = line.split("\\s+");
                        totalNodes = Integer.parseInt(info[0]);
                        meanInterReqDelay = Integer.parseInt(info[1]);
                        meanCSExecTime = Integer.parseInt(info[2]);
                        noRequestGenerated = Integer.parseInt(info[3]);
                       
                        System.out.println("Read 1st line : " + totalNodes + " " + meanInterReqDelay + " " + meanCSExecTime + " " + noRequestGenerated);
                        //ignore first line
                        lineCount++;
                        linesToRead = totalNodes;      //Remembering the number of lines to read,say, N
                        continue;
                    } else if (lineCount > 0) {
                        if (lineCount <= linesToRead) {
                            //Store dcXX.utdallas and port number
                            String[] sysInfo = line.split("\\s+");
                            System.out.println(">>>>>>>" + line);
                            if (sysInfo.length == 3) {
                                Node node = new Node();
                                node.setNodeId(Integer.parseInt(sysInfo[0]));
                                node.setNodeAddr(sysInfo[1]);        //for local system
                                node.setPort(Integer.parseInt(sysInfo[2]));
                                if (node.getNodeAddr().equals(hostName)) {      //identifying if the node is itself, then storing it in SELF.
                                    self = node;
                                }
                                tempMap.put(node.getNodeId(), node);      //temporarily storing all nodes as we are reading config file for buffering.
                                lineCount++;
                            }
                            continue;
                        }
                    }
                } else {
                    //line doesn't start with numeric value, ignored!
                    continue;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        queue = new PriorityQueue<RequestObject>(totalNodes, new Comparator<RequestObject> (){
            public int compare(RequestObject lhs, RequestObject rhs) {
                //Compare clock value and sort requestObjects accordingly
                int comparedValue = lhs.getTimeStamp().compareTo(rhs.getTimeStamp());
                if(comparedValue != 0){
                    return comparedValue;
                }
                //Resolve ties with nodeID values
                else{
                    return lhs.getNodeId().compareTo(rhs.getNodeId());
                }
            }
        });

        /*Timer timer = new Timer();
            //If the node is co-ordinator node (node 0), then start sending snapshot initiating marker messages to neighbors
            if(self.getNodeId() == 0){
                TimerTask tasknew = new TimerTask() {
                    @Override
                    public void run() {
                        //Stop initiating snapshots after you sent map protocol termination messages to everyone
                        while(!App.stopMapProtocolsMessageSent){
                            if(App.markerMessageSent != null && !App.markerMessageSent.containsKey(snapshotNumber)) {
                                //Record local state (local vector clock value)
                                TreeMap<Integer, Integer> channelState = new TreeMap<Integer, Integer>();
                                for(int node = 0; node < App.totalNodes; node++) {
                                    channelState.put(node, App.vectorClock.get(node));
                                }
                                App.channelStates.add(snapshotNumber, channelState);
                                System.out.println("Sending marker message with snapshot ID : " + snapshotNumber);
                                Message outMessage = new Message(MessageType.Marker, 0, null, snapshotNumber);
                                //Mark that marker message has been sent for this snapshot ID
                                App.markerMessageSent.put(snapshotNumber, true);
                                Processor.sendMarkerMessages(outMessage);
                                snapshotNumber++;
                            }
                        }
                    }
                };
                if(!App.stopMapProtocolsMessageSent) {
                    tasknew.run();
                    timer.schedule(tasknew, snapshotDelay);
                }
            }
            Random r = new Random();
            double mySample = r.nextGaussian()*desiredStandardDeviation+desiredMean;
            http://stackoverflow.com/questions/31754209/can-random-nextgaussian-sample-values-from-a-distribution-with-different-mean
            */
    }
}