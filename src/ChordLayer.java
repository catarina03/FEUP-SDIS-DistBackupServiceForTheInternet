import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class ChordLayer {
    private static String address, portNumber;
    private static String[] cipherSuites;
    private static ChordNode predecessor, successor;
    private static ConcurrentSkipListMap<Integer, ChordNode> fingerTable;

    public ChordLayer(String addr, String port, String[] suites){
        address = addr;
        portNumber = port;
        cipherSuites = suites;
        fingerTable = new ConcurrentSkipListMap<>();
    }
    
    /**
     * Get the address of the node
     * @return address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Get the port number
     * @return port number
     */
    public String getPortNumber() {
        return portNumber;
    }

    /**
     * Get the Cipher Suites used
     * @return array with all the cipher suites
     */
    public String[] getCipherSuites() {
        return cipherSuites;
    }

    /**
     * Get the predecessor
     * @return ChordNode with the information of the predecessor
     */
    public ChordNode getPredecessor() {
        return predecessor;
    }

    /**
     * Get the successor
     * @return ChordNode with the information of the successor
     */
    public ChordNode getSuccessor() {
        return successor;
    }

    /**
     * Get the finger table of the node
     * @return ConcurrentSkipListMap with the infroamtion of the finger table
     */
    public ConcurrentSkipListMap<Integer, ChordNode> getFingerTable() {
        return fingerTable;
    }

    /**
     * Changes the nodes' successor
     * @param successor - ChordNode representing the successor
     */
    public void setSuccessor(ChordNode successor) {
        ChordLayer.successor = successor;
        ChordLayer.fingerTable.put(1, successor);
    }

    /**
     * Changes the nodes' predecessor
     * @param predecessor - ChordNode representing the predecessor
     */
    public void setPredecessor(ChordNode predecessor) {
        ChordLayer.predecessor = predecessor;
    }

    /**
     * Changes a specific finger of the finger table
     * @param index - index of the finger
     * @param fingerNode - ChordNode with the information of the new finger
     */
    public void setFingerAtIndex(int index, ChordNode fingerNode){
        ChordLayer.fingerTable.put(index, fingerNode);
    }
    
    /**
     * Start the Chord
     * The predecessor is set to null and the node is his own successor
     */
    public static void createChord(){
        ChordLayer.predecessor = null;
        ChordLayer.successor = new ChordNode(ChordPeer.getId(), ChordLayer.address, ChordLayer.portNumber);
        ChordLayer.fingerTable.put(1, ChordLayer.successor);

        System.out.println("Chord Initiated");
    }

    /**
     * Joins a already created Chord
     * Ask the node that is already in the chord for the node's successor
     * @param addr - address of the node who is already in the chord
     * @param port - port of the node who is already in the chord
     * @throws Exception - When the message can't be sent
     */
    public static void joinChord(String addr, String port) throws Exception{
        System.out.println("Joining Chord with node " + port);

        // Ask the node of this node successor
        String message = "FINDSUCCESSOR " + ChordPeer.getId() +  " \r\n\r\n";
        RequestSender requestSender = new RequestSender(addr, port, message, ChordLayer.cipherSuites, true);

        Message response = new Message(requestSender.send());

        // Set the node successor to the one received
        response.resolve();

        System.out.println(ChordLayer.successor.printInfo());
        System.out.println("Joined Chord");
    }

    /**
     * Find the successor of a specific ID
     * @param nodeID - id to find the succcessor
     * @return - String containing information about the successor
     */
    public String findSuccessor(int nodeID){
        String message = "SUCCESSOR ";

        // Check if this node is the successor of nodeID
        if(dealWithInterval(ChordPeer.getId(), false, ChordLayer.successor.getId(), true, nodeID)){
            return message + ChordLayer.successor.getId() + " " + ChordLayer.successor.getAddress() + " " + ChordLayer.successor.getPortNumber() + " \r\n\r\n";
        }

        // Get the closest node to the nodeID and ask them who is the successor of nodeID
        ChordNode closestNode = closestPrecedingNode(nodeID);
        String requestMessage = "FINDSUCCESSOR " + nodeID + " \r\n\r\n";

        RequestSender request = new RequestSender(closestNode.getAddress(),"" + closestNode.getPortNumber(), requestMessage, ChordLayer.cipherSuites, true);

        // Return a String cointaing the information given to the node about the successor of nodeID
        try {
            return new String(request.send());
        } catch (Exception e) {
            dealWithNodeFailure(closestNode.getAddress(), closestNode.getPortNumber());

            return findSuccessor(nodeID);
        }

    }

    /**
     * Iterate over the finger table to see who is the closest preceding node to nodeID
     * @param nodeID - id to find the closest preceding node
     * @return  ChordNode with the information about the closest preceding node
     */
    public ChordNode closestPrecedingNode(int nodeID){
        // For each node in the finger table, see if the node is a predecessor of nodeID
        for(Iterator<ChordNode> e = ChordLayer.fingerTable.values().iterator(); e.hasNext();){
            ChordNode node = e.next();
            // If it is, return the node
            if(dealWithInterval(ChordPeer.getId(), false, nodeID, false, node.getId())){
                return node;
            }
        }

        // If there are no nodes who precede nodeID in the finger table, return the node who invoked the method
        return new ChordNode(ChordPeer.getId(), ChordLayer.address, ChordLayer.portNumber);
    }

    /**
     * Update the predecessor with the information given
     * @param nodeID - id of the node's predecessor
     * @param addr - address of the node's predecessor
     * @param port - port number of the node's predecessor
     */
    public void updatePredecessor(String nodeID, String addr, String port){
        int predecessorID = Integer.parseInt(nodeID);

        // See if the node is really this node's predecessor
        if(ChordLayer.predecessor == null || dealWithInterval(ChordLayer.predecessor.getId(), false, ChordPeer.getId(), false, predecessorID)){
            setPredecessor(new ChordNode(Integer.parseInt(nodeID), addr, port));
            System.out.println("Predecessor Updated.");
        }

        System.out.println("Current Predecessor: " + ChordLayer.predecessor.getId());
        System.out.println("Current Successor: " + ChordLayer.successor.getId());
    }

    /**
     * In case of node failure, the node needs to take the node who fails from the finger table
     * @param nodeAddres - address of the node who failed
     * @param nodePort - port number of the node who failed
     */
    public void dealWithNodeFailure(String nodeAddres, int nodePort){
        System.out.println("Node " + nodePort + " has failed.");

        // Iterate over all the nodes in the finger table and check if any of them is the node who failed
        Iterator<Map.Entry<Integer, ChordNode>> iter = ChordLayer.fingerTable.entrySet().iterator();

        while(iter.hasNext()){
            Map.Entry<Integer, ChordNode> entry = iter.next();
            
            // Remove the node that failed
            if(entry.getValue().getAddress().equals(nodeAddres) && entry.getValue().getPortNumber() == nodePort){
                ChordLayer.fingerTable.remove(entry.getKey());
            }
        }

        // In case the first finger of the table was removed, set the new successor
        if(ChordLayer.fingerTable.isEmpty()){
            setSuccessor(new ChordNode(ChordPeer.getId(), ChordLayer.address, ChordLayer.portNumber));
        }
        else{
            setSuccessor(ChordLayer.fingerTable.firstEntry().getValue());
        }
    }

    /** */
    public String printFingerTable(){
        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<Integer, ChordNode>> iter = ChordLayer.fingerTable.entrySet().iterator();
        builder.append("\n------------- Finger Table -------------\n");
        while(iter.hasNext()){
            Map.Entry<Integer, ChordNode> entry = iter.next();
            builder.append("Finger Number "  + entry.getKey() + " : ");
            builder.append(entry.getValue().printInfo());
        }

        return builder.toString();
    }

    /**
     * Calculates if a value is inside a circular interval
     * @param leftEndPoint - left limit of the interval
     * @param containsLeft - boolean telling if the value can be equal to the left limit
     * @param rightEndPoint - right limit of the interval
     * @param containsRight - boolean telling if the value can be equal to the right limit
     * @param value - value that should be inside the interval
     * @return - true if the value is inside the interval, false otherwise
     */
    public Boolean dealWithInterval(int leftEndPoint, Boolean containsLeft, int rightEndPoint, Boolean containsRight, int value){
    
        // If the left limit is bigger then the right limit then the interval needs to be interperted differently
        if(leftEndPoint >= rightEndPoint){
            if(containsLeft){
                if(containsRight){
                    return value >= leftEndPoint || value <= rightEndPoint;
                }

                return value >= leftEndPoint || value < rightEndPoint;
            }
            else if(containsRight){
                return value > leftEndPoint || value <= rightEndPoint;
            }

            return value > leftEndPoint || value < rightEndPoint;
        }

        if(containsLeft){
            if(containsRight){
                return value >= leftEndPoint && value <= rightEndPoint;
            }

            return value >= leftEndPoint && value < rightEndPoint;
        }
        else if(containsRight){
            return value > leftEndPoint && value <= rightEndPoint;
        }

        return value > leftEndPoint && value < rightEndPoint;
    }

    /**
     * Encodes a string with sha-1
     * @param stringToEncode - string that will be enconded
     * @return - enconded string
     */
    public String sha1Encode(final String stringToEncode) {
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-1");
            final byte[] hash = digest.digest(stringToEncode.getBytes("UTF-8"));
            final StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < hash.length; i++) {
                final String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) 
                  hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception e){
           e.printStackTrace();
           return "";
        }
    }
}
