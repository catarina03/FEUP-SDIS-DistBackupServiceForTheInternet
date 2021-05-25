 

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChordPeer implements PeerClientInterface{
    private static int id, idBits = 16;
    private static Listener listener;
    private static ScheduledThreadPoolExecutor threadPool;
    private static ChordLayer chordLayer;
    private static PeerFolder folder;

    public ChordPeer(String idNumber, String addr, String port, String[] suites){
        chordLayer = new ChordLayer(addr, port, suites);

        String encondedID = chordLayer.sha1Encode(addr + port);
        id = Integer.parseInt(encondedID.substring(encondedID.length() - idBits/4), 16);
        System.out.println("Peer started with ID: " + id);

        folder = new PeerFolder("" + id);
        threadPool = new ScheduledThreadPoolExecutor(100);
    }

    public static int getId() {
        return id;
    }

    public static ChordLayer getChordLayer(){
        return chordLayer;
    }

    public static Listener getListener() {
        return listener;
    }

    public static ScheduledThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    public static int getIdBits() {
        return idBits;
    }

    public static PeerFolder getFolder() {
        return folder;
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
    public static void main(String[] args) throws Exception {
        // Create Thread Pool
        threadPool = new ScheduledThreadPoolExecutor(100);

        // Create Listener
        String[] cipherSuites = new String[0];
        listener = new Listener(args[2], cipherSuites);
        threadPool.execute(listener);

        // Create the peer object to be saved in the RMI
        ChordPeer obj = new ChordPeer(args[0], args[1], args[2], cipherSuites);

        if(args.length < 5){
            ChordLayer.createChord();
        }
        else{
            ChordLayer.joinChord(args[4], args[5]);
        }

        
        threadPool.scheduleWithFixedDelay(new StabilizeTask(), 5, 10, TimeUnit.SECONDS);
        threadPool.scheduleWithFixedDelay(new FixFingersTask(), 20, 20, TimeUnit.SECONDS);
        threadPool.scheduleWithFixedDelay(new CheckPredecessorTask(), 5, 20, TimeUnit.SECONDS);
       

        // Save the object in the rmi
        try {
            PeerClientInterface stub = (PeerClientInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[3], stub);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Backup the file given, for that, send a message for each chunk, so that other peers can save each chunk
     * @param filePath - path of the file to be backed up
     * @param repDegree - desired replication degree of the file
     * @return "done" if success
     */
    @Override
    public String backup(String filePath, int repDegree) throws RemoteException {
        System.out.println("Received Backup Request of file " + filePath);
        // Create a file and save it in the folder
        FileData newFile = new FileData(filePath, repDegree);
        folder.addFile(newFile);

        String[] successorResponse = ChordPeer.getChordLayer().findSuccessor(Integer.parseInt(newFile.getID())).split(" ");
        ChordNode successor = new ChordNode(Integer.parseInt(successorResponse[1].trim()), successorResponse[2].trim(), successorResponse[3].trim());

        System.out.println("Backing up File with id " + newFile.getID() + " in peer " + successor.getId() + "with size");

        if(successor.getId() == id){
            System.out.println("Saving file in predecessor.");
            successor = ChordPeer.getChordLayer().getPredecessor();
        }

        String saveFileMessage = "SAVEFILE " + newFile.getID() + " " + newFile.getTotalChunks() + " " + newFile.getReplicationDegree() + " " + newFile.getFileSize() + " " + newFile.getFilePath() + " \r\n\r\n";

        RequestSender saveFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), saveFileMessage, chordLayer.getCipherSuites(), false);
        
        try {
            saveFileRequest.send();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Sent save file message");
        System.out.println("Sending " + newFile.getTotalChunks() + " chunks");
        // Iterate over the chunks, sending a message for each one thourgh the multicast channel
        for(int i = 0; i < newFile.getTotalChunks(); i++){
            // Create the header and body of the message
            String header ="PUTCHUNK " + newFile.getID() + " " + i + " " + newFile.getReplicationDegree() + " " + newFile.getChunk(i).getData().length + " \r\n\r\n";
            byte[] body = newFile.getChunk(i).getData();
            byte[] headerBytes = header.getBytes();
            // Join the header and the boddy into an array
            byte[] message = new byte[headerBytes.length + body.length];
            System.arraycopy(headerBytes, 0, message, 0, headerBytes.length);
            System.arraycopy(body, 0, message, headerBytes.length, body.length);
            System.out.println("Sending message with length " + message.length + " and body size " + body.length);
            // Send the message
            RequestSender putChunkRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), message, chordLayer.getCipherSuites(), true);

            try {
                System.out.println("Sending Chunk");
                putChunkRequest.send();

                System.out.println("Sent putchunk message");
            } catch (Exception e) {
                chordLayer.dealWithNodeFailure(successor.getAddress(), successor.getPortNumber());
                
                backup(filePath, repDegree);

                return "done";
            }
        }

        String fileSavedMessage = "SAVECOMPLETED " + newFile.getID() + " \r\n\r\n";
        RequestSender savesFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), fileSavedMessage, chordLayer.getCipherSuites(), false);
        System.out.println("Sent " + fileSavedMessage);
        try {
            savesFileRequest.send();
        } catch (Exception e) {
            
        }

        return "done";
    }


}