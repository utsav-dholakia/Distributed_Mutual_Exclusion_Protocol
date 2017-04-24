import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.PriorityQueue;

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
                        RequestObject requestObject = new RequestObject(inMessage.getTimeStamp(),
                                                                        inMessage.getSrcNodeID());
                        LamportMutex.requestQueue.add(requestObject);
                        PriorityQueue<RequestObject> traceQueue = LamportMutex.requestQueue;
                        //Send reply to go ahead with execution to the object at the head of priority queue
                        sendReplyMessage(inMessage);
                        break;
                    case Reply:
                        updateClockValue(inMessage);
                        if(LamportMutex.replyPending != null) {
                            //Reply received from a neighbour, remove it from reply pending list
                            if (LamportMutex.replyPending.contains(inMessage.getSrcNodeID())) {
                                LamportMutex.replyPending.remove(inMessage.getSrcNodeID());
                            }
                            //If all neighbours have replied
                            if (LamportMutex.replyPending.size() == 0) {
                                //If node is at the head of priority queue
                                if (LamportMutex.requestQueue.peek().getNodeId() == CriticalSection.self.getNodeId()) {
                                    //Enter critical section, L1,L2 conditions are true
                                    LamportMutex.isExecutingCS = true;
                                }
                            }
                        }
                        break;
                    case Release:
                        updateClockValue(inMessage);
                        //If the head of request queue is the same as the node which sent release message
                        if(LamportMutex.requestQueue.peek().getNodeId() == inMessage.getSrcNodeID()){
                            System.out.println("Released request of node - " + LamportMutex.requestQueue.peek().getNodeId());
                            LamportMutex.requestQueue.poll();
                        }
                        //If all neighbours have replied
                        if(LamportMutex.replyPending != null) {
                            //Reply received from a neighbour, remove it from reply pending list
                            if (LamportMutex.replyPending.contains(inMessage.getSrcNodeID())) {
                                LamportMutex.replyPending.remove(inMessage.getSrcNodeID());
                            }
                            if (LamportMutex.replyPending.size() == 0) {
                                //If node is at the head of priority queue
                                if (LamportMutex.requestQueue.peek().getNodeId() == CriticalSection.self.getNodeId()) {
                                    //Enter critical section, L1,L2 conditions are true
                                    LamportMutex.isExecutingCS = true;
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
            System.out.println("Exception in handling incoming message");
            e.printStackTrace();
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
        System.out.println("Sending reply from node - " + CriticalSection.self.getNodeId() + " at time - " + LamportMutex.scalarClock);
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