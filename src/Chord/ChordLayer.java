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

    public String getAddress() {
        return address;
    }
    public String getPortNumber() {
        return portNumber;
    }

    public String[] getCipherSuites() {
        return cipherSuites;
    }

    public ChordNode getPredecessor() {
        return predecessor;
    }

    public ChordNode getSuccessor() {
        return successor;
    }

    public ConcurrentSkipListMap<Integer, ChordNode> getFingerTable() {
        return fingerTable;
    }

    public void setSuccessor(ChordNode successor) {
        ChordLayer.successor = successor;
        ChordLayer.fingerTable.put(1, successor);
    }

    public void setPredecessor(ChordNode predecessor) {
        ChordLayer.predecessor = predecessor;
    }

    public void setFingerAtIndex(int index, ChordNode fingerNode){
        ChordLayer.fingerTable.put(index, fingerNode);
    }
    
    public static void createChord(){
        ChordLayer.predecessor = null;
        ChordLayer.successor = new ChordNode(ChordPeer.getId(), ChordLayer.address, ChordLayer.portNumber);
        ChordLayer.fingerTable.put(1, ChordLayer.successor);

        System.out.println("Chord Initiated");
    }

    public static void joinChord(String addr, String port) throws Exception{
        System.out.println("Joining Chord with node " + port);
        String message = "FINDSUCCESSOR " + ChordPeer.getId() +  " \r\n\r\n";

        RequestSender requestSender = new RequestSender(addr, port, message, ChordLayer.cipherSuites, true);

        Message response = new Message(requestSender.send());

        response.resolve();

        ChordLayer.successor.printInfo();
        System.out.println("Joined Chord");
    }

    public String findSuccessor(int nodeID){
        String message = "SUCCESSOR ";

        if(dealWithInterval(ChordPeer.getId(), false, ChordLayer.successor.getId(), true, nodeID)){
            return message + ChordLayer.successor.getId() + " " + ChordLayer.successor.getAddress() + " " + ChordLayer.successor.getPortNumber() + " \r\n\r\n";
        }

        ChordNode closestNode = closestPrecedingNode(nodeID);
        String requestMessage = "FINDSUCCESSOR " + nodeID + " \r\n\r\n";
        // System.out.println(("Asking node " + closestNode.getId() + " for successor"));
        RequestSender request = new RequestSender(closestNode.getAddress(),"" + closestNode.getPortNumber(), requestMessage, ChordLayer.cipherSuites, true);

        try {
            return new String(request.send());
        } catch (Exception e) {
            dealWithNodeFailure(closestNode.getAddress(), closestNode.getPortNumber());

            return findSuccessor(nodeID);
        }

    }

    public ChordNode closestPrecedingNode(int nodeID){

        for(Iterator<ChordNode> e = ChordLayer.fingerTable.values().iterator(); e.hasNext();){
            ChordNode node = e.next();
            if(dealWithInterval(ChordPeer.getId(), false, nodeID, false, node.getId())){
                return node;
            }
        }

        return new ChordNode(ChordPeer.getId(), ChordLayer.address, ChordLayer.portNumber);
    }

    public void updatePredecessor(String nodeID, String addr, String port){
        int predecessorID = Integer.parseInt(nodeID);

        if(ChordLayer.predecessor == null || dealWithInterval(ChordLayer.predecessor.getId(), false, ChordPeer.getId(), false, predecessorID)){
            setPredecessor(new ChordNode(Integer.parseInt(nodeID), addr, port));
        }

        // System.out.println("Predecessor Updated.");
        // System.out.println("Current Predecessor: " + ChordLayer.predecessor.getId());
        // System.out.println("Current Successor: " + ChordLayer.successor.getId());
    }

    public void dealWithNodeFailure(String nodeAddres, int nodePort){

        Iterator<Map.Entry<Integer, ChordNode>> iter = ChordLayer.fingerTable.entrySet().iterator();

        while(iter.hasNext()){
            Map.Entry<Integer, ChordNode> entry = iter.next();
            
            if(entry.getValue().getAddress().equals(nodeAddres) && entry.getValue().getPortNumber() == nodePort){
                ChordLayer.fingerTable.remove(entry.getKey());
            }
        }

        if(ChordLayer.fingerTable.isEmpty()){
            setSuccessor(new ChordNode(ChordPeer.getId(), ChordLayer.address, ChordLayer.portNumber));
        }
        else{
            setSuccessor(ChordLayer.fingerTable.firstEntry().getValue());
        }

        printFingerTable();
    }

    public void printFingerTable(){
        // Iterator<Map.Entry<Integer, ChordNode>> iter = ChordLayer.fingerTable.entrySet().iterator();
        // System.out.println("Finger Table Updated:");
        // while(iter.hasNext()){
        //     Map.Entry<Integer, ChordNode> entry = iter.next();
        //     System.out.println("Finger nr "  + entry.getKey() + " :");
        //     entry.getValue().printInfo();
        // }
    }

    // public ChordNode calculateFileNode(FileData fileToStore){
    //     String[] successorResponse = findSuccessor(Integer.parseInt(fileToStore.getID())).split(" ");
    //     ChordNode fileSuccessor = new ChordNode(Integer.parseInt(successorResponse[1].trim()), successorResponse[2].trim(), successorResponse[3].trim());

    //     if(fileSuccessor.getId() == ChordPeer.getId()){
    //         System.out.println("Saving file in predecessor.");
    //         fileSuccessor = ChordLayer.predecessor;

    //         if(fileSuccessor == null || fileSuccessor.getId() == ChordPeer.getId()){
    //             System.out.println("Saving file in successor.");
    //             fileSuccessor = ChordLayer.successor;
    //             if(fileSuccessor == null || successor.getId() == ChordPeer.getId()){
    //                 System.out.println("File cannot be saved because i'm alone");
    //                 return null;
    //             }
    //         }
    //     }

    //     String saveFileMessage = "SAVEFILE " + fileToStore.getID() + " " + fileToStore.getTotalChunks() + " " + fileToStore.getReplicationDegree() + " " + fileToStore.getFileSize() + " " + fileToStore.getFilePath() + " \r\n\r\n";

    //     RequestSender saveFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), saveFileMessage, ChordLayer.cipherSuites, true);
        
    //     try {
    //         String[] response = new String(saveFileRequest.send()).split(" ");
    //         if(response[0].equals("NOTSAVED")){
    //             System.out.println("File cannot be saved for there are no nodes that can save the file");
    //             return null;
    //         }
    //         else if(!response[1].equals("NULL")){
    //             successor = new ChordNode(Integer.parseInt(response[1]), response[2], response[3].trim());
    //         }
    //     } catch (Exception e) {
    //         return null;
    //     }

    //     return fileSuccessor;
    // }

    public Boolean dealWithInterval(int leftEndPoint, Boolean containsLeft, int rightEndPoint, Boolean containsRight, int value){

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
