 

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

    public ChordPeer(String addr, String port, String[] suites){
        chordLayer = new ChordLayer(addr, port, suites);

        String encondedID = chordLayer.sha1Encode(addr + port);
        id = Integer.parseInt(encondedID.substring(encondedID.length() - idBits/4), 16);
        System.out.println("Peer started with ID: " + id);

        folder = new PeerFolder("" + id);
        threadPool = new ScheduledThreadPoolExecutor(100);
    }

    /**
     * Get the ip address of peer
     * @return peer's id
     */
    public static int getId() {
        return id;
    }

    /**
     * Get the Chord Layer class of the noode
     * @return Chord Layer
     */
    public static ChordLayer getChordLayer(){
        return chordLayer;
    }

    /**
     * Get the thread pool of the peer
     * @return SchdulesThreadPoolExecutor of peer
     */
    public static ScheduledThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    /**
     * Get the number of bits used to create the ID
     * @return int representing the number of bits used
     */
    public static int getIdBits() {
        return idBits;
    }

    /**
     * Get the Folder of the peer
     * @return
     */
    public static PeerFolder getFolder() {
        return folder;
    }

    /**
     * Get if the peer is being saved
     * @return
     */
    public static Boolean getSavingFile() {
        return savingFile;
    }

    /**
     * Set if the peer is saving a file
     * @param savingFile - boolean representing the state
     */
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
        listener = new Listener(args[1], cipherSuites);
        threadPool.execute(listener);

        // Create the peer object to be saved in the RMI
        ChordPeer obj = new ChordPeer(args[0], args[1], cipherSuites);

        if(args.length < 5){
            ChordLayer.createChord();
        }
        else{
            ChordLayer.joinChord(args[3], args[4]);
        }

        
        // Schedule Stabilize Task
        threadPool.scheduleWithFixedDelay(new StabilizeTask(), 5, 5, TimeUnit.SECONDS);
        // Schedule Fix Finger Task to check if the finger table is correct
        threadPool.scheduleWithFixedDelay(new FixFingersTask(), 5, 10, TimeUnit.SECONDS);
        // Schedule the task  to check if the predecessor is alive
        threadPool.scheduleWithFixedDelay(new CheckPredecessorTask(), 5, 5, TimeUnit.SECONDS);
        // Schedule the task to check if the nodes who saved files are alive
        threadPool.scheduleWithFixedDelay(new CheckBackupNodesTask(), 15, 5, TimeUnit.SECONDS);
       

        // Save the object in the rmi
        try {
            PeerClientInterface stub = (PeerClientInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[2], stub);

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

        // Create a number of files equal to the replication degree
        for(int i = 0; i < repDegree; i++){
            // Create a file and save it in the folder
            FileData newFile = new FileData(filePath, repDegree, i);
            folder.addFile(newFile.getID(), newFile);

            saveFile(newFile);
        }

        return "done";
    }

    /**
     * Calculates who saves the file and sends the chunks to them
     * @param newFile - file to be saved
     */
    public static void saveFile(FileData newFile){
        
        // Check who will save the file
        String[] successorResponse = ChordPeer.getChordLayer().findSuccessor(Integer.parseInt(newFile.getID())).split(" ");
        ChordNode successor = new ChordNode(Integer.parseInt(successorResponse[1].trim()), successorResponse[2].trim(), successorResponse[3].trim());

        // If the initiator peer is the succesor of the file, save the file in the predecessor
        if(successor.getId() == id){
            System.out.println("Saving file in predecessor.");
            successor = ChordPeer.getChordLayer().getPredecessor();

            // If the peer is his own predecessor then try to save it in the peer's successor
            if(successor == null || successor.getId() == id){
                System.out.println("Saving file in successor.");
                successor = ChordPeer.getChordLayer().getSuccessor();

                // If both the peer's succcessor and predecessor are himself or don't exist then there are no nodes in the chord
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

            // If the response is "NOTSAVEd" then there are no peers who can save the file
            if(response[0].equals("NOTSAVED")){
                System.out.println("File cannot be saved for there are no nodes that can save the file");
                ChordPeer.getFolder().removeFileByID(newFile.getID());
                return;
            }
            // If the response is not NULL then the peer who can save the file is not the one we contacted
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

        // Tell the peer that there are no more chunks
        String fileSavedMessage = "SAVECOMPLETED " + newFile.getFilePath() + " \r\n\r\n";
        RequestSender savesFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), fileSavedMessage, chordLayer.getCipherSuites(), false);

        try {
            savesFileRequest.send();
        } catch (Exception e) {
            chordLayer.dealWithNodeFailure(successor.getAddress(), successor.getPortNumber());

            saveFile(newFile);

            return;
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
            // Get where the file is saved
            ArrayList<ChordNode> nodes = ChordPeer.getFolder().getFileLocation(filePath);

            // Index of the node we are contacting
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
                    // If the node is not online, then we contact the next node who saved the file
                    ChordPeer.getChordLayer().dealWithNodeFailure(fileLocation.getAddress(), fileLocation.getPortNumber());
                    ChordPeer.getFolder().deleteFileLocation(file);

                    nodeIndex++;

                    // When nodeIndex is equal to the nodes size then there are no more nodes online that backup the file
                    if(nodeIndex == nodes.size()){
                        ChordPeer.getFolder().setFileToRestore(new ArrayList<>());
                        System.out.println("Can't restore for all the peer who had the file went offline");
                    }

                    // Update the fileLocation and decriment i so that the peer asks for the chunk that failed
                    fileLocation = nodes.get(nodeIndex);
                    file = ChordPeer.getFolder().getFilebyNode(filePath, fileLocation);
                    i--;
                }
                
            }

            // Restore the file into the peer's folder
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
            // Get where the file is saved
            ArrayList<ChordNode> nodes = ChordPeer.getFolder().getFileLocation(filePath);

            // For each of the nodes that saved the file, tell them to delete it
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

            // Remove the file from the backup files and remove its location
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

                // Iterate over all the saved files
                Iterator<Map.Entry<String, FileData>> iterator = ChordPeer.getFolder().getStoredFiles().entrySet().iterator();

                // Delete one by one until the space used is less then the total space
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


    /**
     * Retrieves the stat of the peer
     * @return a string containing the state of the peer
     */
    @Override
    public String state() throws RemoteException {
        StringBuilder state = new StringBuilder();
        state.append("\n------------- Node Information -------------\n");
        state.append("Peer ID: " + ChordPeer.getId() + ";\n");
        state.append("Peer Port: " + ChordPeer.getChordLayer().getPortNumber() + ";\n");
        state.append("Peer Address: " + ChordPeer.getChordLayer().getAddress() + ";\n");
        state.append("Peer Port: " + ChordPeer.getChordLayer().getPortNumber() + ";\n");
        state.append(ChordPeer.getChordLayer().printFingerTable());

        // Retrieve the information about all the files stored by the peer
        Iterator<Map.Entry<FileData, ChordNode>> locationIter = ChordPeer.getFolder().getFileLocation().entrySet().iterator();

        state.append("\n------------- Files Backed Up -------------\n");
        while(locationIter.hasNext()){
            Map.Entry<FileData, ChordNode> entry = locationIter.next();
            FileData fileState = entry.getKey();
            ChordNode nodeLocation = entry.getValue();
            state.append("File number " + fileState.getID() +":\n");
            state.append("  - Pathname: " + fileState.getFilePath() + ";\n");
            state.append("  - Node Location: \n");
            state.append("      - Node IP: " + nodeLocation.getId() + ";\n");
            state.append("      - Node Port: " + nodeLocation.getPortNumber() + ";\n");
            state.append("      - Node Address: " + nodeLocation.getAddress() + ";\n");
        }

        Iterator<Map.Entry<String, FileData>> storedIter = ChordPeer.getFolder().getStoredFiles().entrySet().iterator();

        state.append("\n------------- Files Stored -------------\n");
        while(storedIter.hasNext()){
            Map.Entry<String, FileData> storedEntry = storedIter.next();
            FileData fileState = storedEntry.getValue();
            state.append("File number " + fileState.getID() +":\n");
            state.append("  - Pathname: " + fileState.getFilePath() + ";\n");
            state.append("  - File Size: " + fileState.getFileSize() + ";\n");
        }
        // Retrieve the information about the peer's storage
        state.append("\n Peer maximum storage: " + folder.getStorageSize() + "kB;\n");
        state.append("\n Peer used storage: " + folder.getStorageUsed() + "kB;\n");

        return state.toString();
    }


}