 

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
    private static Boolean savingFile = false;

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

    public static Boolean getSavingFile() {
        return savingFile;
    }

    public static void setSavingFile(Boolean savingFile) {
        ChordPeer.savingFile = savingFile;
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
        threadPool.scheduleWithFixedDelay(new CheckBackupNodesTask(), 25, 20, TimeUnit.SECONDS);
       

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
        
        for(int i = 0; i < repDegree; i++){
            // Create a file and save it in the folder
            FileData newFile = new FileData(filePath, repDegree, i);
            folder.addFile(newFile);

            saveFile(newFile);
        }

        return "done";
    }

    public static void saveFile(FileData newFile){
        
        String[] successorResponse = ChordPeer.getChordLayer().findSuccessor(Integer.parseInt(newFile.getID())).split(" ");
        ChordNode successor = new ChordNode(Integer.parseInt(successorResponse[1].trim()), successorResponse[2].trim(), successorResponse[3].trim());

        if(successor.getId() == id){
            System.out.println("Saving file in predecessor.");
            successor = ChordPeer.getChordLayer().getPredecessor();

            if(successor == null || successor.getId() == id){
                System.out.println("Saving file in successor.");
                successor = ChordPeer.getChordLayer().getSuccessor();
                if(successor == null || successor.getId() == id){
                    System.out.println("File cannot be saved because i'm alone");
                    return;
                }
            }
        }

        String saveFileMessage = "SAVEFILE " + newFile.getID() + " " + newFile.getTotalChunks() + " " + newFile.getReplicationDegree() + " " + newFile.getFileSize() + " " + newFile.getFilePath() + " \r\n\r\n";

        RequestSender saveFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), saveFileMessage, chordLayer.getCipherSuites(), true);
        
        try {
            String[] response = new String(saveFileRequest.send()).split(" ");
            if(response[0].equals("NOTSAVED")){
                System.out.println("File cannot be saved for there are no nodes that can save the file");
                return;
            }
            else if(!response[1].equals("NULL")){
                successor = new ChordNode(Integer.parseInt(response[1]), response[2], response[3].trim());
            }
        } catch (Exception e) {
        }

        System.out.println("Storing file with id " + newFile.getID() + " in peer " + successor.getId());

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
            // Send the message
            RequestSender putChunkRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), message, chordLayer.getCipherSuites(), true);

            try {
                putChunkRequest.send();

                System.out.println("Sent putchunk message");
            } catch (Exception e) {
                chordLayer.dealWithNodeFailure(successor.getAddress(), successor.getPortNumber());
                
                saveFile(newFile);

                return;
            }
        }

        String fileSavedMessage = "SAVECOMPLETED " + newFile.getID() + " \r\n\r\n";
        RequestSender savesFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), fileSavedMessage, chordLayer.getCipherSuites(), false);

        try {
            savesFileRequest.send();
        } catch (Exception e) {
            
        }
        
        System.out.println("File backup in peer: " + successor.getPortNumber());
        folder.addBackupNode(newFile.getFilePath(), successor);
    }


}