import java.io.ObjectOutputStream;
import java.net.Socket;

public class Processor extends Thread{

    @Override
    public void run() {
        try {
                Message inMessage = App.messagesToBeProcessed.take();

                switch(inMessage.getMessageType()) {
                    case Request:
                        //Take max of local clock value and inmessage clock value
                        Integer maxValue = Math.max(App.scalarClock, inMessage.getTimeStamp());
                        App.scalarClock = maxValue + 1;
                        //Adding requestObject into own priority queue
                        RequestObject requestObject = new RequestObject(inMessage.getTimeStamp(),
                                                                        inMessage.getSrcNodeID());
                        App.queue.add(requestObject);
                        //Send reply to go ahead with execution to the object at the head of priority queue
                        sendReplyMessage(inMessage);
                        break;
                    case Reply:
                        break;
                    case Release:
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void sendReplyMessage(Message inMessage) {
        Message replyMessage = new Message(MessageType.Reply,App.self.getNodeId(), App.scalarClock);	//control msg to 0 saying I am permanently passive
        try{
            Socket socket = new Socket(App.tempMap.get(inMessage.getSrcNodeID()).getNodeAddr(),App.tempMap.get(inMessage.getSrcNodeID()).getPort());
            ObjectOutputStream outMessage = new ObjectOutputStream(socket.getOutputStream());
            outMessage.writeObject(replyMessage);
            socket.close();
        }catch(Exception e){

        }
    }
}