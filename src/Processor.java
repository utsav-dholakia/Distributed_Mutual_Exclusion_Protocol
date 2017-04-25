import sun.jvmstat.perfdata.monitor.CountedTimerTask;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Iterator;


public class Processor extends Thread{

    @Override
    public void run() {
        try {
                Message inMessage = LamportMutex.messagesToBeProcessed.take();
                System.out.println("Message - " + inMessage.getMessageType() + " from node - " + inMessage.getSrcNodeID() + " at time - " + inMessage.getTimeStamp());
                switch(inMessage.getMessageType()) {
                    case Request:
                        updateClockValue(inMessage);
                        //Adding requestObject into own priority queue
                        RequestObject requestObject = new RequestObject(inMessage.getTimeStamp(), inMessage.getSrcNodeID());
                        LamportMutex.requestQueue.add(requestObject);
                        //Send reply to go ahead with execution to the object at the head of priority queue
                        sendReplyMessage(inMessage);
                        break;
                    case Reply:
                        updateClockValue(inMessage);
                        verifyConditions(inMessage);
                        break;
                    case Release:
                        updateClockValue(inMessage);
                        //If the head of request queue is the same as the node which sent release message
                        /*if(LamportMutex.requestQueue.peek().getNodeId() == inMessage.getSrcNodeID()) {

                            LamportMutex.requestQueue.poll();
                        }*/
                        Iterator<RequestObject> itr = LamportMutex.requestQueue.iterator();
                        while(itr.hasNext()){
                            RequestObject obj = itr.next();
                            if(obj.getNodeId().equals(inMessage.getSrcNodeID())){
                                LamportMutex.requestQueue.remove(obj);
                                System.out.println("Released request of node - " + obj.getNodeId());
                                CriticalSection.bufferedWriter.write("\nRELEASE CS BY - " + inMessage.getSrcNodeID() + " AT TIME - " + inMessage.getTimeStamp());
                                CriticalSection.bufferedWriter.flush();
                                break;
                            }
                        }
                        /*else{
                            System.out.println("ERROR : Release received but request not at the head of the queue" + inMessage.getSrcNodeID().getClass().getName());
                            System.out.println("Queue head - " + LamportMutex.requestQueue.peek().getNodeId().getClass().getName());
                        }*/
                        verifyConditions(inMessage);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
            System.out.println("Exception in handling incoming message");
            e.printStackTrace();
        }

    }

    public static void verifyConditions(Message inMessage){
        //If all neighbours have replied
        if(LamportMutex.replyPending != null) {
            //Reply received from a neighbour, remove it from reply pending list
            if (LamportMutex.replyPending.contains(inMessage.getSrcNodeID())) {
                LamportMutex.replyPending.remove(inMessage.getSrcNodeID());
            }
            if (LamportMutex.replyPending.size() == 0 && LamportMutex.requestQueue.size() > 0) {
                //If node is at the head of priority queue
                if (LamportMutex.requestQueue.peek().getNodeId() == CriticalSection.self.getNodeId()) {
                    //Enter critical section, L1,L2 conditions are true
                    LamportMutex.isExecutingCS = true;
                }
            }
        }
    }

    public static void updateClockValue(Message inMessage){
        //Take max of local clock value and inMessage clock value
        Integer maxValue = Math.max(LamportMutex.scalarClock, inMessage.getTimeStamp());
        LamportMutex.scalarClock = maxValue + 1;

    }

    public static void sendReplyMessage(Message inMessage) {
        //Update scalar clock to mark a send event
        LamportMutex.scalarClock = LamportMutex.scalarClock + 1;
        System.out.println("Sending Reply src node - " + CriticalSection.self.getNodeId() + " time - " + LamportMutex.scalarClock);
        Message replyMessage = new Message(MessageType.Reply, CriticalSection.self.getNodeId(), LamportMutex.scalarClock);	//control msg to 0 saying I am permanently passive
        try{
            Socket socket = new Socket(CriticalSection.nodeMap.get(inMessage.getSrcNodeID()).getNodeAddr(), CriticalSection.nodeMap.get(inMessage.getSrcNodeID()).getPort());
            ObjectOutputStream outMessage = new ObjectOutputStream(socket.getOutputStream());
            outMessage.writeObject(replyMessage);
            socket.close();
        }catch(Exception e){
            System.out.println("Exception in sending reply message");
            e.printStackTrace();
        }
    }
}