import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Listener extends Thread{
    Integer currPortNum;
    public static volatile boolean serverOn = true;
    public static ServerSocket serverSocket;

    Listener(Integer portNum){
        this.currPortNum = portNum;
    }

    @Override
    public void run() {
        try{
            //Initialize the receiver as a continuous listening server
            serverSocket = new ServerSocket(currPortNum);
            System.out.println("Listening on port : " + currPortNum);
            while (serverOn) {
                Socket sock = serverSocket.accept();
                //System.out.print("Connected, ");
                //Enter a message that is received into the queue to be processed
                LamportMutex.messagesToBeProcessed.put((Message) new ObjectInputStream(sock.getInputStream()).readObject());
                //Initiate thread of a class to process the messages one by one from queue
                Processor processor = new Processor();
                //Create a new thread only if no thread exists
                if(!processor.isAlive()){
                    new Thread(processor).start();
                }
            }
        } catch(Exception e){
            serverOn = false;
            //
        }
    }

    public void stopListener(){
        serverOn = false;
        try {
            serverSocket.close();
        } catch (IOException e) {

        }
    }
}