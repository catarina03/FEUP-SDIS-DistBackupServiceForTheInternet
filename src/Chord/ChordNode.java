 

public class ChordNode {
    private String address;
    private int portNumber, id;

    public ChordNode(int nodeID, String addr, String port){
        address = addr;
        portNumber = Integer.parseInt(port);
        id = nodeID;
    }

    public String getAddress() {
        return address;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public int getId() {
        return id;
    }

    public void printInfo(){
        System.out.println("Chord Node with id " + id + " at port " + portNumber);
    }
}
