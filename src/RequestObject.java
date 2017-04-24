import java.util.Comparator;

public class RequestObject {
    Integer timeStamp;
    Integer nodeId;


    public RequestObject(Integer timeStamp, Integer nodeId) {
        this.timeStamp = timeStamp;
        this.nodeId = nodeId;
    }

    public Integer getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Integer timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }
}

class ComparatorForQueue implements Comparator<RequestObject> {
    @Override
    public int compare(RequestObject t1, RequestObject requestObject) {
        if(requestObject.getTimeStamp() < t1.getTimeStamp()){
            return -1;
        }
        else if(requestObject.getTimeStamp() > t1.getTimeStamp()){
            return +1;
        }
        else{
            if(requestObject.getNodeId() < t1.getNodeId()){
                return -1;
            }
            else if(requestObject.getNodeId() > t1.getNodeId()){
                return +1;
            }
            else {
                return 0;
            }
        }
    }
}