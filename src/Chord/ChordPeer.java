import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.ArrayList;

import java.util.concurrent.ScheduledThreadPoolExecutor;


public class ChordPeer  implements PeerClientTest{
    static String address, portNumber;
    static int id;
    private static Listener listener;
    private static ScheduledThreadPoolExecutor threadPool;
    static String[] cipherSuites;
    private static ChordNode predecessor, successor;
    private static ArrayList<ChordNode> fingerTable;

    public ChordPeer(String idNumber, String addr, String port, String[] Suites){
        id = Integer.parseInt(port);
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

    public static void setSuccessor(ChordNode successor) {
        ChordPeer.successor = successor;
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
        RequestSender requestSender = new RequestSender(address, port, "WHATS UP", new String[0]);

   
        return new String(requestSender.send());
    }

    private static void createChord(){
        predecessor = null;
        successor = new ChordNode(address, portNumber);
        System.out.println("Chord Initiated");
    }

    private static void joinChord(String addr, String port) throws IOException{
        String message = "1.0 FINDSUCCESSOR " + portNumber +  " \r\n\r\n";

        RequestSender requestSender = new RequestSender(addr, port, message, cipherSuites);

        Message response = new Message(requestSender.send());

        response.resolve();

        successor.printInfo();
        
        System.out.println("Joined Chord");
    }

    public static String findSucessor(int nodeID) throws IOException{
        String message = "1.0 SUCCESSOR ";

        // TODO: Verificar o que é para fazer quando só existe um node no chord
        if(ChordPeer.getId() == ChordPeer.getSuccessor().getId()){
            if(nodeID < ChordPeer.getId()){
                return message + ChordPeer.getId() + " " + ChordPeer.getPortNumber() + " " + ChordPeer.getAddress() + " \r\n\r\n";
            }
        }


        if(nodeID > ChordPeer.getId() && nodeID < ChordPeer.getSuccessor().getId()){
            return message + ChordPeer.getSuccessor().getId() + " " + ChordPeer.getSuccessor().getPortNumber() + " " + ChordPeer.getSuccessor().getAddress() + " \r\n\r\n";
        }
        else{
            ChordNode closestNode = closestPrecedingNode(nodeID);
            String requestMessage = "1.0 FINDSUCCESSOR " + nodeID + " \r\n\r\n";

            RequestSender request = new RequestSender(closestNode.getAddress(),"" + closestNode.getPortNumber(), requestMessage, cipherSuites);

            return new String(request.send());
        }
    }


    public static ChordNode closestPrecedingNode(int id){
        for(int i = fingerTable.size() - 1; i >=0; i--){
            if(fingerTable.get(i).getId() < id){
                return fingerTable.get(i);
            }
        }

        return new ChordNode(address, portNumber);
    }

}