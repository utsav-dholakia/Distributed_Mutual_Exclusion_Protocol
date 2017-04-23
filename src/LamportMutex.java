import sun.jvmstat.perfdata.monitor.CountedTimerTask;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LamportMutex {
    public static BlockingQueue<Message> messagesToBeProcessed = new LinkedBlockingQueue<Message>();
    public static List<Integer> replyPending;		//store list of pending nodes from which reply hasnt been received
    public static PriorityQueue<RequestObject> requestQueue = new PriorityQueue<RequestObject>(CriticalSection.totalNodes, new Comparator<RequestObject> (){
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
    public static Integer scalarClock = 0;
    public static boolean isExecutingCS = false;

    public static boolean csEnter(){
        //init reply monitor
        replyPending = Collections.synchronizedList(new ArrayList<Integer>(){
            public synchronized boolean add(int node){
                boolean ret = super.add(node);
                return ret;
            }
        });
        for(int i=0; i< CriticalSection.totalNodes ; i++){
            replyPending.add(i);
        }
        replyPending.remove(CriticalSection.self.getNodeId());
        //end initialization of reply monitor

        //Critical section entry request sent
        CriticalSection.isRequestSent = true;
        //Scalar clock update
        scalarClock++;
        //Add my request to request queue
        RequestObject requestObject = new RequestObject(scalarClock, CriticalSection.self.getNodeId());
        requestQueue.add(requestObject);

        //Send request message to all nodes
        Message request = new Message(MessageType.Request,CriticalSection.self.getNodeId(),scalarClock);
        Iterator<Integer> iterator = CriticalSection.nodeMap.keySet().iterator();
        try{
            while (iterator.hasNext()) {
                Node node = CriticalSection.nodeMap.get(iterator.next());
                Socket socket = new Socket(node.getNodeAddr(), node.getPort());
                ObjectOutputStream outMessage = new ObjectOutputStream(socket.getOutputStream());
                outMessage.writeObject(request);
                socket.close();
            }
        }catch(Exception e){
            System.out.println("Exception in sending release message");
            e.printStackTrace();
        }

        //Block enterCS function till isExecutingCS is not marked as true
        while(true){
            if(isExecutingCS) {
                break;
            }
        }
        return true;
    }


    public static void csExit(){
        //Mark isExecutingCS as false,
        isExecutingCS = false;
        //Critical section entry request is not sent
        CriticalSection.isRequestSent = false;
        //Send release message to all nodes
        sendReleaseMessage();
    }

    public static void sendReleaseMessage() {
        //Increment clock value
        LamportMutex.scalarClock = LamportMutex.scalarClock + 1;
        //Generate release message
        Message releaseMessage = new Message(MessageType.Release, CriticalSection.self.getNodeId(), LamportMutex.scalarClock);
        Iterator<Integer> iterator = CriticalSection.nodeMap.keySet().iterator();
        try{
            while (iterator.hasNext()) {
                Node node = CriticalSection.nodeMap.get(iterator.next());
                Socket socket = new Socket(node.getNodeAddr(), node.getPort());
                ObjectOutputStream outMessage = new ObjectOutputStream(socket.getOutputStream());
                outMessage.writeObject(releaseMessage);
                socket.close();
            }
        }catch(Exception e){
            System.out.println("Exception in sending release message");
            e.printStackTrace();
        }
    }

}