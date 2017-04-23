import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class App {
    public static Integer totalNodes;   //Total nodes in topology
    public static Node self;
    //public static Map<Integer, Node> tempMap = new HashMap<Integer, Node>();
    public static Map<Integer, Node> nodeMap = new HashMap<Integer, Node>();        //Stores all nodes
    public static BlockingQueue<Message> messagesToBeProcessed = new LinkedBlockingQueue<Message>();

    public static List<Integer> replyPending;		//store list of pending nodes from which reply hasnt been received
    public static volatile boolean isRequestSent = false;		// keeps track whether current process has sent a request or not
    public static Integer countRequestsSent = 0;
    
    public static Integer meanInterReqDelay = 0;
    public static Integer meanCSExecTime = 0;
    public static Integer countOfRequestsAllowed = 0;
    public static PriorityQueue<RequestObject> requestQueue;
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
        requestQueue = new PriorityQueue<RequestObject>(totalNodes, new Comparator<RequestObject> (){
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
        
        while(countRequestsSent <= countOfRequestsAllowed){
        	
        	//init reply monitor
        	replyPending = Collections.synchronizedList(new ArrayList<Integer>(){
        		public synchronized boolean add(int node){
        			boolean ret = super.add(node);
        			return ret;
        		}
        	});
        	for(int i=0; i<totalNodes ; i++){
        		replyPending.add(i);
        	}
        	replyPending.remove(self.getNodeId());
        	//end initialization of reply monitor
        	
        	 Random r = new Random();
             long waitTime = (long)r.nextGaussian()+meanInterReqDelay;		//generate random wait interval between requests
             try {
				Thread.sleep(waitTime);		//sleep for random time
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
             if(!isRequestSent){		//if no request message has been sent, then send one
            	 isRequestSent = true;
            	 scalarClock++;
            	 Message request = new Message(MessageType.Request,self.getNodeId(),scalarClock);
            	 messagesToBeProcessed.add(request);
            	 
            	 Iterator<Integer> iterator = App.nodeMap.keySet().iterator();
                 try{
                     while (iterator.hasNext()) {
                         Node node = App.nodeMap.get(iterator.next());
                         Socket socket = new Socket(node.getNodeAddr(), node.getPort());
                         ObjectOutputStream outMessage = new ObjectOutputStream(socket.getOutputStream());
                         outMessage.writeObject(request);
                         socket.close();
                     }
                 }catch(Exception e){
                     System.out.println("Exception in sending release message");
                     e.printStackTrace();
                 }
            	 
             }
                     	
        }

    }
}