import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CriticalSection {

    public static Integer totalNodes;   //Total nodes in topology
    public static Node self;
    //public static Map<Integer, Node> tempMap = new HashMap<Integer, Node>();
    public static Map<Integer, Node> nodeMap = new HashMap<Integer, Node>();        //Stores all nodes
    public static Integer countRequestsSent = 0;
    public static Integer meanInterReqDelay = 0;
    public static Integer meanCSExecTime = 0;
    public static Integer countOfRequestsAllowed = 0;
    public static volatile boolean isRequestSent = false;		// keeps track whether current process has sent a request or not
    public static volatile boolean enterCriticalSection = false;
    public static Date now = new Date();
    public static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static Listener listener;

    public static void main(String args[]){
        //Do local configuration of node
        initialize();
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter("log-" + self.getNodeId() + ".txt", true);
            bufferedWriter = new BufferedWriter(fileWriter);

            while(countRequestsSent <= countOfRequestsAllowed){

                Random r = new Random();
                long waitTime = (long)r.nextGaussian()+meanInterReqDelay;		//generate random wait interval between requests
                try {
                    Thread.sleep(waitTime);		//sleep for random time
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if(!isRequestSent){		//if no request message has been sent, then send one
                    countRequestsSent++;
                    //Request to enter criticalSection
                    enterCriticalSection = LamportMutex.csEnter();
                    if(enterCriticalSection){
                        //Print log of entering critical section
                        now = new Date();
                        String s = df.format(now);
                        String result = s.substring(0, 26) + ":" + s.substring(27);
                        bufferedWriter.write("Entering critical section - " + result);
                        //Invoke execute critical section method
                        executeCriticalSection(bufferedWriter);
                    }
                }

            }
            //Thread.sleep(15000);
            //listener.stopListener();
            bufferedWriter.close();
            fileWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void executeCriticalSection(BufferedWriter bufferedWriter){
        System.out.println("Executing critical section");
        //Execute critical section logic
    	 Random r = new Random();
         long waitTime = (long)r.nextGaussian()+meanCSExecTime;		//generate random wait interval between requests
         try {
             Thread.sleep(waitTime);		//sleep for random time
         } catch (InterruptedException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
        //After task is done
        //Print log of exiting critical section
        Date now = new Date();
        String s = df.format(now);
        String result = s.substring(0, 26) + ":" + s.substring(27);
        try {
            bufferedWriter.write("Exiting critical section - " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //invoke csExit method of lamport protocol
        LamportMutex.csExit();
    }

    public static void initialize(){
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
                        countOfRequestsAllowed = Integer.parseInt(info[3]);

                        System.out.println("Read 1st line : " + totalNodes + " " + meanInterReqDelay + " " + meanCSExecTime + " " + countOfRequestsAllowed);
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
                                }else{
                                    nodeMap.put(node.getNodeId(), node);      //temporarily storing all nodes as we are reading config file for buffering.
                                }

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

        //Listener(Server) class initiated
        listener = new Listener(self.getPort());
        Thread listenerThread = new Thread(listener, "Listener Thread");
        listenerThread.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


}
