import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.ArrayList;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ChordPeer  implements PeerClientTest{
    static String address, portNumber;
    static int id, nextFinger = 0;
    private static Listener listener;
    private static ScheduledThreadPoolExecutor threadPool;
    static String[] cipherSuites;
    private static ChordNode predecessor, successor;
    private static ArrayList<ChordNode> fingerTable;

    public ChordPeer(String idNumber, String addr, String port, String[] Suites){
        String encondedID = sha1Encode(addr + port);
        id = Integer.parseInt(encondedID.substring(encondedID.length() - 2), 16);
        System.out.println("ID: " + id);
        address = addr;
        portNumber = port;
        threadPool = new ScheduledThreadPoolExecutor(100);
        fingerTable = new ArrayList<>();
    }

    public static String getAddress() {
        return address;
    }

    public static String[] getCipherSuites() {
        return cipherSuites;
    }

    public static ArrayList<ChordNode> getFingerTable() {
        return fingerTable;
    }

    public static int getId() {
        return id;
    }

    public static Listener getListener() {
        return listener;
    }

    public static String getPortNumber() {
        return portNumber;
    }

    public static ChordNode getPredecessor() {
        return predecessor;
    }

    public static ChordNode getSuccessor() {
        return successor;
    }

    public static ScheduledThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    public static int getNextFinger() {
        return nextFinger;
    }

    public static void setNextFinger(int next) {
        ChordPeer.nextFinger = next;
    }

    public static void setSuccessor(ChordNode successor) {
        ChordPeer.successor = successor;

        if(ChordPeer.fingerTable.size() > 1){
            ChordPeer.fingerTable.set(0, successor);
        }
        else{
            ChordPeer.fingerTable.add(successor);
        }
    }

    public static void setPredecessor(ChordNode predecessor) {
        ChordPeer.predecessor = predecessor;
    }

    public static void setFingerAtIndex(int index, ChordNode fingerNode){
        ChordPeer.fingerTable.set(index, fingerNode);
    }

    /**
     * 
     * @param args - Array containing the following information:
     * args[0] --> peer ID;
     * args[1] --> address;
     * args[2] --> port;
     * args[3] --> RMI name;
     * args[4] --> Known Chord Peer address;
     * args[5] --> Known Chord Peer port;
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // Create Thread Pool
        threadPool = new ScheduledThreadPoolExecutor(100);

        // Create Listener
        cipherSuites = new String[0];
        listener = new Listener(args[2], cipherSuites);
        threadPool.execute(listener);

        // Create the peer object to be saved in the RMI
        ChordPeer obj = new ChordPeer(args[0], args[1], args[2], cipherSuites);

        if(args.length < 5){
            createChord();
        }
        else{
            joinChord(args[4], args[5]);
        }

        StabilizeTask stabilize = new StabilizeTask(obj);


        threadPool.scheduleWithFixedDelay(stabilize, 5, 5, TimeUnit.SECONDS);
        threadPool.scheduleWithFixedDelay(new FixFingersTask(), 25, 5, TimeUnit.SECONDS);

        // // Save the object in the rmi
        // try {
        //     PeerClientTest stub = (PeerClientTest) UnicastRemoteObject.exportObject(obj, 0);
        //     Registry registry = LocateRegistry.getRegistry();
        //     registry.bind(args[3], stub);

        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

    }
   
    @Override
    public String testCommunication(String port) throws Exception {
        RequestSender requestSender = new RequestSender(address, port, "WHATS UP", new String[0], true);

   
        return new String(requestSender.send());
    }

    private static void createChord(){
        predecessor = null;
        successor = new ChordNode(id, address, portNumber);
        fingerTable.add(successor);
        System.out.println("Chord Initiated");
    }

    private static void joinChord(String addr, String port) throws IOException{
        String message = "1.0 FINDSUCCESSOR " + id +  " \r\n\r\n";

        RequestSender requestSender = new RequestSender(addr, port, message, cipherSuites, true);

        Message response = new Message(requestSender.send());

        response.resolve();

        successor.printInfo();
        System.out.println("Joined Chord");
    }

    public static String findSuccessor(int nodeID) throws IOException{
        String message = "1.0 SUCCESSOR ";

        if(dealWithInterval(ChordPeer.getId(), false, ChordPeer.getSuccessor().getId(), true, nodeID)){
            return message + ChordPeer.getSuccessor().getId() + " " + ChordPeer.getSuccessor().getAddress() + " " + ChordPeer.getSuccessor().getPortNumber() + " \r\n\r\n";
        }

        ChordNode closestNode = closestPrecedingNode(nodeID);
        String requestMessage = "1.0 FINDSUCCESSOR " + nodeID + " \r\n\r\n";
        System.out.println(("Asking node " + closestNode.getId() + " for successor"));
        RequestSender request = new RequestSender(closestNode.getAddress(),"" + closestNode.getPortNumber(), requestMessage, cipherSuites, true);

        return new String(request.send());

    }

    public static ChordNode closestPrecedingNode(int nodeID){

        for(int i = fingerTable.size() - 1; i >=0; i--){
            if(dealWithInterval(id, false, nodeID, false, fingerTable.get(i).getId())){
                return fingerTable.get(i);
            }
        }

        return new ChordNode(id, address, portNumber);
    }

    public static void updatePredecessor(String nodeID, String addr, String port){
        int predecessorID = Integer.parseInt(nodeID);

        if(predecessor == null || dealWithInterval(predecessor.getId(), false, id, false, predecessorID)){
            predecessor = new ChordNode(Integer.parseInt(nodeID), addr, port);
        }

        System.out.println("Predecessor Updated.");
        System.out.println("Current Predecessor: " + predecessor.getId());
        System.out.println("Current Successor: " + successor.getId());
    }

    public static Boolean dealWithInterval(int leftEndPoint, Boolean containsLeft, int rightEndPoint, Boolean containsRight, int value){

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

    private String sha1Encode(final String stringToEncode) {
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
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