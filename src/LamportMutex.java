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

        CriticalSection.isRequestSent = true;
        scalarClock++;
        Message request = new Message(MessageType.Request,CriticalSection.self.getNodeId(),scalarClock);
        messagesToBeProcessed.add(request);

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
        isExecutingCS = false;
        //Execute critical section exit logic
        sendReleaseMessage();
    }

    public static void sendReleaseMessage() {
        CriticalSection.isRequestSent = false;
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