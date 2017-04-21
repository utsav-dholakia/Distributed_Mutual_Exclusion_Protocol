import java.io.Serializable;
import java.util.TreeMap;
import java.util.Vector;

/**
 * Created by utsavdholakia on 3/11/17.
 */

enum MessageType {
    Request,
    Reply,
    Release;
}

public class Message implements Serializable{
    MessageType messageType;
    Integer srcNodeID;
    Integer timeStamp;

    public Message(MessageType messageType, Integer srcNodeID, Integer timeStamp) {
        this.messageType = messageType;
        this.srcNodeID = srcNodeID;
        this.timeStamp = timeStamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Integer getSrcNodeID() {
        return srcNodeID;
    }

    public void setSrcNodeID(Integer srcNodeID) {
        this.srcNodeID = srcNodeID;
    }

    public Integer getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Integer timeStamp) {
        this.timeStamp = timeStamp;
    }

}