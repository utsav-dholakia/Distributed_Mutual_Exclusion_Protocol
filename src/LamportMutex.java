import sun.jvmstat.perfdata.monitor.CountedTimerTask;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LamportMutex {
    public static volatile BlockingQueue<Message> messagesToBeProcessed = new LinkedBlockingQueue<Message>();
    public static volatile List<Integer> replyPending;		//store list of pending nodes from which reply hasnt been received
    public static volatile Comparator<RequestObject> comparatorForQueue = new ComparatorForQueue ();
    public static volatile PriorityQueue<RequestObject> requestQueue = new PriorityQueue<RequestObject>(50, comparatorForQueue);
    public static volatile Integer scalarClock = 0;
    public static volatile boolean isExecutingCS = false;

    public static boolean csEnter(){
        //init reply monitor
        replyPending = Collections.synchronizedList(new ArrayList<Integer>(){
            public synchronized boolean add(int node){
                boolean ret = super.add(node);
                return ret;
            }
        });
        Iterator<Integer> itr = CriticalSection.nodeMap.keySet().iterator();
        while(itr.hasNext()){
            replyPending.add(itr.next());
        }
        //replyPending.remove(CriticalSection.self.getNodeId());
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
            CriticalSection.isRequestSent = false;
            System.out.println("Exception in sending request message");
            e.printStackTrace();
            return false;
        }
        System.out.println("Sending Request time - " + LamportMutex.scalarClock + " request Number - " + CriticalSection.countRequestsSent);
        //Block enterCS function till isExecutingCS is not marked as true
        while(true){
            if(isExecutingCS) {
                try {
                    CriticalSection.bufferedWriter.write("\n" + CriticalSection.self.getNodeId() +  "   START   " + LamportMutex.scalarClock);
                    CriticalSection.bufferedWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return true;
    }


    public static void csExit(){
        //Mark isExecutingCS as false,
        isExecutingCS = false;
        //Remove yourself from requestQueue
        LamportMutex.requestQueue.poll();
        //Critical section entry request is not sent
        CriticalSection.isRequestSent = false;
        //Send release message to all nodes
        sendReleaseMessage();
    }

    public static void sendReleaseMessage() {
        try{
            //Increment clock value
            LamportMutex.scalarClock = LamportMutex.scalarClock + 1;
            CriticalSection.bufferedWriter.write("\n" + CriticalSection.self.getNodeId()  + "   RELEASE    " + LamportMutex.scalarClock);
            CriticalSection.bufferedWriter.flush();
            //Generate release message
            Message releaseMessage = new Message(MessageType.Release, CriticalSection.self.getNodeId(), LamportMutex.scalarClock);
            Iterator<Integer> iterator = CriticalSection.nodeMap.keySet().iterator();
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

        System.out.println("Release message sent to all neighbours");
    }

}