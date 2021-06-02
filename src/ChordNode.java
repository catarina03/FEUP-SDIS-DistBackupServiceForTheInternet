 

public class ChordNode {
    private String address;
    private int portNumber, id;

    public ChordNode(int nodeID, String addr, String port){
        address = addr;
        portNumber = Integer.parseInt(port);
        id = nodeID;
    }

    /**
     * Gets the address of the node
     * @return node address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets the port number of the node
     * @return node port number
     */
    public int getPortNumber() {
        return portNumber;
    }

    /**
     * Gets the id of the node
     * @return node id
     */
    public int getId() {
        return id;
    }

    /**
     * Transforms the node information into a string
     * @return node address
     */
    public String printInfo(){
        return "Chord Node with id " + id + " at port " + portNumber + " with address " + address + "\n";
    }
}
