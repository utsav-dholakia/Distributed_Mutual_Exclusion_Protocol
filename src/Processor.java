import java.io.ObjectOutputStream;
import java.net.Socket;

public class Processor extends Thread{

    @Override
    public void run() {
        try {
                Message inMessage = App.messagesToBeProcessed.take();

                switch(inMessage.getMessageType()) {
                    case Request:
                        updateClockValue(inMessage);
                        //Adding requestObject into own priority queue
                        RequestObject requestObject = new RequestObject(inMessage.getTimeStamp(),
                                                                        inMessage.getSrcNodeID());
                        App.requestQueue.add(requestObject);
                        //Send reply to go ahead with execution to the object at the head of priority queue
                        sendReplyMessage(inMessage);
                        break;
                    case Reply:
                        updateClockValue(inMessage);
                        if(App.replyPending != null) {
                            //Reply received from a neighbour, remove it from reply pending list
                            if (App.replyPending.contains(inMessage.getSrcNodeID())) {
                                App.replyPending.remove(inMessage.getSrcNodeID());
                            }
                            //If all neighbours have replied
                            if (App.replyPending.size() == 0) {
                                //If node is at the head of priority queue
                                if (App.requestQueue.peek().getNodeId() == App.self.getNodeId()) {
                                    //Enter critical section, L1,L2 conditions are true
                                    CriticalSection.csEnter();
                                }
                            }
                        }
                        break;
                    case Release:
                        updateClockValue(inMessage);
                        //If the head of request queue is the same as the node which sent release message
                        if(App.requestQueue.peek().getNodeId() == inMessage.getSrcNodeID()){
                            App.requestQueue.remove();
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
        Integer maxValue = Math.max(App.scalarClock, inMessage.getTimeStamp());
        App.scalarClock = maxValue + 1;

    }

    public static void sendReplyMessage(Message inMessage) {
        Message replyMessage = new Message(MessageType.Reply,App.self.getNodeId(), App.scalarClock);	//control msg to 0 saying I am permanently passive
        try{
            Socket socket = new Socket(App.nodeMap.get(inMessage.getSrcNodeID()).getNodeAddr(),App.nodeMap.get(inMessage.getSrcNodeID()).getPort());
            ObjectOutputStream outMessage = new ObjectOutputStream(socket.getOutputStream());
            outMessage.writeObject(replyMessage);
            socket.close();
        }catch(Exception e){
            System.out.println("Exception in sending reply message");
            e.printStackTrace();
        }
    }
}