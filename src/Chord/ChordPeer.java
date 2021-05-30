 

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
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
        //TODO: avisar o succesor que este node está responsavel pelos ficheiros
        for(int i = 0; i < repDegree; i++){
            // Create a file and save it in the folder
            FileData newFile = new FileData(filePath, repDegree, i);
            folder.addFile(newFile.getID(), newFile);

            saveFile(newFile);
        }

        return "done";
    }

    public static void saveFile(FileData newFile){
        
        // Check who will save the file
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
                    ChordPeer.getFolder().removeFileByID(newFile.getID());
                    return;
                }
            }
        }

        // Ask if the peer can save the file
        String saveFileMessage = "SAVEFILE " + newFile.getID() + " " + newFile.getTotalChunks() + " " + newFile.getReplicationDegree() + " " + newFile.getFileSize() + " " + newFile.getFilePath() + " \r\n\r\n";

        RequestSender saveFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), saveFileMessage, chordLayer.getCipherSuites(), true);
        
        try {
            String[] response = new String(saveFileRequest.send()).split(" ");
            if(response[0].equals("NOTSAVED")){
                System.out.println("File cannot be saved for there are no nodes that can save the file");
                ChordPeer.getFolder().removeFileByID(newFile.getID());
                return;
            }
            else if(!response[1].equals("NULL")){
                successor = new ChordNode(Integer.parseInt(response[1]), response[2], response[3].trim());
            }
        } catch (Exception e) {
            chordLayer.dealWithNodeFailure(successor.getAddress(), successor.getPortNumber());
                
            saveFile(newFile);

            return;
        }

        // Tell the Peer that we initiated the backup process
        String initiatorMessage = "INITIATOR " + newFile.getFilePath() + " " + ChordPeer.getId() + " " + ChordPeer.getChordLayer().getAddress() + " " + ChordPeer.getChordLayer().getPortNumber() + " \r\n\r\n";

        RequestSender initiatorRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), initiatorMessage, chordLayer.getCipherSuites(), false);
        
        try {
            initiatorRequest.send();
        } catch (Exception e1) {
            chordLayer.dealWithNodeFailure(successor.getAddress(), successor.getPortNumber());
                
            saveFile(newFile);

            return;
        }

        System.out.println("Storing file with id " + newFile.getID() + " in peer " + successor.getId() + " with port " + successor.getPortNumber() + " address" + successor.getAddress());

        // Iterate over the chunks, sending a message for each one thourgh the multicast channel
        for(int i = 0; i < newFile.getTotalChunks(); i++){

            // Create the header and body of the message
            String header ="PUTCHUNK " + newFile.getID() + " " + i + " " + newFile.getFilePath() + " \r\n\r\n";
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

        String fileSavedMessage = "SAVECOMPLETED " + newFile.getFilePath() + " \r\n\r\n";
        RequestSender savesFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), fileSavedMessage, chordLayer.getCipherSuites(), false);

        try {
            savesFileRequest.send();
        } catch (Exception e) {
            
        }
        
        System.out.println("File backup in peer: " + successor.getPortNumber());
        System.out.println("Saving file with path: " + newFile.getFilePath());
        folder.addFileLocation(newFile, successor);
    }

    /**
     * Ask other peers for each chunk of a file so that it can be restored
     * @param filePath - path of the file to be restored
     * @param repDegree - desired replication degree of the file
     * @return "done" if success
     */
    @Override
    public String restore(String filePath) throws RemoteException {

        // Check if the file was stored in this peer
        if(ChordPeer.getFolder().fileIsSavedPathname(filePath)){
            ArrayList<ChordNode> nodes = ChordPeer.getFolder().getFileLocation(filePath);

            int nodeIndex = 0;

            ChordNode fileLocation = nodes.get(nodeIndex);
            
            FileData file = ChordPeer.getFolder().getFilebyNode(filePath, fileLocation);

            // For each chunk, send a message requesting the chunk
            for(int i = 0 ; i < file.getTotalChunks(); i ++){
                // Create the message
                String message = "GETCHUNK " + file.getFilePath() + " " + i + " \r\n\r\n";

                RequestSender restoreChunkRequest = new RequestSender(fileLocation.getAddress(), "" + fileLocation.getPortNumber(), message, ChordPeer.getChordLayer().getCipherSuites(), true);

                try {
                    Message restoredChunkInfo = new Message(restoreChunkRequest.send());

                    restoredChunkInfo.resolve();
                } catch (Exception e) {
                    ChordPeer.getChordLayer().dealWithNodeFailure(fileLocation.getAddress(), fileLocation.getPortNumber());
                    ChordPeer.getFolder().deleteFileLocation(file);

                    nodeIndex++;

                    if(nodeIndex == nodes.size()){
                        ChordPeer.getFolder().setFileToRestore(new ArrayList<>());
                        System.out.println("Can't restore for all the peer who had the file went offline");
                    }

                    fileLocation = nodes.get(nodeIndex);
                    file = ChordPeer.getFolder().getFilebyNode(filePath, fileLocation);
                    i--;
                }
                
            }

            ChordPeer.getFolder().restoreFile(file.getFileName());

            System.out.println("File restored");
        } else {
            System.out.println("File was never backed up...");
        }

        return "done";
    }

    /**
     * Ask other peers to delete all the chunks of a certain file
     * @param filePath - path of the file to be deleted
     * @return "done" if success
     */
    @Override
    public String delete(String filePath){
        System.out.println("Deleting file " + filePath);
        // Check if the file is saved
        if(folder.fileIsSavedPathname(filePath)){
            ArrayList<ChordNode> nodes = ChordPeer.getFolder().getFileLocation(filePath);

            for(ChordNode node : nodes){
                // Create the message
                String message = "DELETE " + filePath + " \r\n\r\n";
                
                // Send the message
                RequestSender deleteRequest = new RequestSender(node.getAddress(), "" + node.getPortNumber(), message, ChordPeer.getChordLayer().getCipherSuites(), true);

                try {
                    deleteRequest.send();
                } catch (Exception e) {
                    System.out.println("Node didn't response to delete request. Removing it");
                    ChordPeer.getChordLayer().dealWithNodeFailure(node.getAddress(), node.getPortNumber());


                }

                System.out.println("Deleting backup of file " + folder.getFile(filePath).getName() + " from peer " + node.getId());
            }


            ChordPeer.getFolder().deleteFileLocations(filePath);
            ChordPeer.getFolder().removeFile(filePath);
            
        } else {
            System.out.println("File was never backed up...");
        }

        return "done";
    }

    /**
     * Reclaims the space used by the peer
     * @param size - maximum space the peer can use
     * @return "done" if success
     */
    @Override
    public String reclaim(String size) throws RemoteException {
        try{
            // Set the peer total storage to the argument given
            int maxSize = Integer.parseInt(size);
            folder.setStorageSize(maxSize);
            System.out.println("New filder Size: " + folder.getStorageSize() + " and i'm using " + folder.getStorageUsed());
            // If the storage used is bigger then the total storage of a peer, the peer needs to delete files
            if(folder.getStorageSize() < folder.getStorageUsed()){
                Iterator<Map.Entry<String, FileData>> iterator = ChordPeer.getFolder().getStoredFiles().entrySet().iterator();

                while(iterator.hasNext()){
                    Map.Entry<String, FileData> entry = iterator.next();

                    // Warn Initaitor Peer of the backup that one of the files was removed
                    String removedMessage = "REMOVED " + entry.getValue().getID() + " \r\n\r\n";

                    System.out.println(removedMessage);
                    System.out.println("Sending to : " + entry.getValue().getInitiatorPeer().getPortNumber());
                    RequestSender removedRequest = new RequestSender(entry.getValue().getInitiatorPeer().getAddress(), "" + entry.getValue().getInitiatorPeer().getPortNumber(), removedMessage, ChordPeer.getChordLayer().getCipherSuites(), false);
                    
                    try {
                        removedRequest.send();
                    } catch (Exception e) {
                        System.out.println("Initiator peer went offline");
                        ChordPeer.getChordLayer().dealWithNodeFailure(entry.getValue().getInitiatorPeer().getAddress(), entry.getValue().getInitiatorPeer().getPortNumber());
                    }

                    ChordPeer.getFolder().deleteStoredFile(entry.getKey());

                     // If the storage size is bigger than the used, than the loop can break
                     if(folder.getStorageSize() > folder.getStorageUsed()){
                        break;
                    }
                }
            }

        }catch(NumberFormatException e){
            return "Invalid number input";
        }

        return "done";
    }


}