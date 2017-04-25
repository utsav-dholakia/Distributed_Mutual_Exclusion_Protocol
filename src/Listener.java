import java.io.BufferedWriter;
import java.io.FileWriter;
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
            CriticalSection.fileWriter = new FileWriter("log-" + CriticalSection.self.getNodeId() + ".txt", true);
            CriticalSection.bufferedWriter = new BufferedWriter(CriticalSection.fileWriter);
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
                    //System.out.println("PROCESSOR PROCESSOR PROCESSOR START START START");
                    new Thread(processor).start();
                }
            }
            CriticalSection.bufferedWriter.close();
            CriticalSection.fileWriter.close();
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