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