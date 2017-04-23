import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Iterator;

public class CriticalSection {
    public static void csEnter(){
        //Execute critical section logic

        //After task is done
        csExit();
    }

    public static void csExit(){
        //Execute critical section exit logic
        sendReleaseMessage();
    }

    public static void sendReleaseMessage() {
        //Increment clock value
        App.scalarClock = App.scalarClock + 1;
        //Generate release message
        Message replyMessage = new Message(MessageType.Release,App.self.getNodeId(), App.scalarClock);
        Iterator<Integer> iterator = App.nodeMap.keySet().iterator();
        try{
            while (iterator.hasNext()) {
                Node node = App.nodeMap.get(iterator.next());
                Socket socket = new Socket(node.getNodeAddr(), node.getPort());
                ObjectOutputStream outMessage = new ObjectOutputStream(socket.getOutputStream());
                outMessage.writeObject(replyMessage);
                socket.close();
            }
        }catch(Exception e){
            System.out.println("Exception in sending release message");
            e.printStackTrace();
        }
    }
}
