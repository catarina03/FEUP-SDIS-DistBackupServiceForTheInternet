package Peer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import Channels.BackupChannel;
import Channels.ControlChannel;
import Channels.RecoveryChannel;
import Channels.TCPRestoreChannel;
import Tasks.ReplicationDegreeTask;
import Tasks.RestoreChunkTask;

public class Peer  implements PeerClientInterface{
    private static String id, protocolVersion;
    private static ControlChannel MC;
    private static BackupChannel MDB;
    private static RecoveryChannel MDR;
    private static ScheduledThreadPoolExecutor threadPool;
    private static ScheduledThreadPoolExecutor chunkPool;
    private static PeerFolder folder;
    private static boolean started;

    public Peer(String protocolVs, String peerID, String controlAddress, int controlPort, String backupAddress, int backupPort, String recoveryAddress, int recoveryPort) {

        MC = new ControlChannel(controlAddress, controlPort);
        MDB = new BackupChannel(backupAddress, backupPort);
        MDR = new RecoveryChannel(recoveryAddress, recoveryPort);
        id = peerID;
        folder = new PeerFolder(peerID);
        protocolVersion = protocolVs;
        threadPool = new ScheduledThreadPoolExecutor(100);
        chunkPool = new ScheduledThreadPoolExecutor(100);
        started = true;
    }

    public static BackupChannel getBackupChannel(){
        return MDB;
    }

    public static ControlChannel getControlChannel(){
        return MC;
    }

    public static RecoveryChannel getRecoveryChannel(){
        return MDR;
    }

    public static ScheduledThreadPoolExecutor getThreadPool(){
        return threadPool;
    }

    public static ScheduledThreadPoolExecutor getChunkPool(){
        return chunkPool;
    }

    public static String getID(){
        return id;
    }

    public static String getProtocolVersion(){
        return protocolVersion;
    }

    public static PeerFolder getFolder(){
        return folder;
    }

    public static boolean justStarted(){
        return started;
    }
        
    /**
     * 
     * @param args - Array containing the following information:
     * args[0] --> protocol Version;
     * args[1] --> peer ID;
     * args[2] --> RMI Addres;
     * args[3] --> RMI name;
     * args[4] --> MC multicast IP;
     * args[5] --> MC multicast Port;
     * args[6] --> MDB multicast IP;
     * args[7] --> MDB multicast Port;
     * args[8] --> MDR multicast IP;
     * args[9] --> MDR multicast Port;
     */
    public static void main(String args[]) {
        
        if(args.length != 9){
            System.out.println("Arguments given are not in the right format. Arguments lenght should have only 1 argument. The given argument as a lenght of " + args.length + ".\n");
            return;
        }

        try {
            Path peerFolder = Paths.get("./Peers");
            Files.createDirectories(peerFolder);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // Create Thread Pool
        threadPool = new ScheduledThreadPoolExecutor(100);

        // Create MC Multicast Socket
        String controlAddress = args[3];
        int controlPort = getPort(args[4]);
        MC = new ControlChannel(controlAddress, controlPort);
        threadPool.execute(MC);

        // Create MDB Multicast Socket
        String backupAddress = args[5];
        int backupPort = getPort(args[6]);
        MDB = new BackupChannel(backupAddress, backupPort);
        threadPool.execute(MDB);

        // Create MDR Multicast Socket
        String recoveryAddress = args[7];
        int recoveryPort = getPort(args[8]);
        MDR = new RecoveryChannel(recoveryAddress, recoveryPort);
        threadPool.execute(MDR);

        // Create the peer object to be saved in the RMI
        Peer obj = new Peer(args[0], args[1], controlAddress, controlPort, backupAddress, backupPort, recoveryAddress, recoveryPort);
        
        // Save the object in the rmi
        try {
            PeerClientInterface stub = (PeerClientInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[2], stub);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if(Peer.protocolVersion.equals("2.0")){
            verifyDeletion();
        }

        started = false;

        
    }

    /**
     * Verifies if deletions where made while the peer was offline
     */
    private static void verifyDeletion() {
        // Create messages
        String message = protocolVersion + " VERIFYFILES " + id + " \r\n\r\n";

        // Send message
        MC.sendMessage(message.getBytes());
    }

    
    /**
     * Transform a string to a int, representing a port
     * @param portString - string to be transformed into a int, contains the number of the port
     * @return the port as an int
     */
    private static int getPort(String portString){
        Integer port = 0;

        try{
            port = Integer.valueOf(portString.trim());

        } catch(NumberFormatException e){
            System.out.println("Port given was not a number.\n");
            System.exit(1);
        }

        return port;
    }
    
    /**
     * Backup the file given, for that, send a message for each chunk, so that other peers can save each chunk
     * @param filePath - path of the file to be backed up
     * @param repDegree - desired replication degree of the file
     * @return "done" if success
     */
    @Override
    public String backup(String filePath, int repDegree) throws RemoteException {
        // Create a file and save it in the folder
        FileData newFile = new FileData(filePath, repDegree);
        folder.addFile(newFile);

        // Iterate over the chunks, sending a message for each one thourgh the multicast channel
        for(int i = 0; i < newFile.getTotalChunks(); i++){
            // Create the header and body of the message
            String header = protocolVersion + " PUTCHUNK " + id + " " + newFile.getID() + " " + i + " " + newFile.getReplicationDegree() + " \r\n\r\n";
            byte[] body = newFile.getChunk(i).getData();
            byte[] headerBytes = header.getBytes();

            // Join the header and the boddy into an array
            byte[] message = new byte[headerBytes.length + body.length];
            System.arraycopy(headerBytes, 0, message, 0, headerBytes.length);
            System.arraycopy(body, 0, message, headerBytes.length, body.length);

            // Send the message
            MDB.sendMessage(message);
            
            // Schedule a task to check if the desired replication degree was achieved
            threadPool.schedule(new ReplicationDegreeTask(message, newFile.getID(), i, 1, repDegree), 1, TimeUnit.SECONDS);
        }

        return "done";
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
        if(folder.fileIsSavedPathname(filePath)){
            String fileName = folder.getFile(filePath).getName();

            // If the version is 2.0, then the messages are sent via TCP
            if(Peer.protocolVersion.equals("2.0")){
                TCPRestoreChannel tcp = new TCPRestoreChannel(5555);
                threadPool.execute(tcp); //Start TCP Server
            } 
            
            // For each chunk, send a message requesting the chunk
            for(int i = 0 ; i < folder.getFileChunksSize(filePath); i ++){
                // Create the message
                String message = protocolVersion + " GETCHUNK " + id + " " + folder.getFileIDbyPath(filePath) + " " + i + " \r\n\r\n";
                
                // Send the message
                MC.sendMessage(message.getBytes());

                // Add the chunk to a checklist, so that it can be checked later if the peer received it
                folder.addWantedChunk(folder.getFileIDbyPath(filePath), i);
            }

            // Schedule a task to restore the file, when all the chunks are returned
            threadPool.schedule(new RestoreChunkTask(fileName), 2, TimeUnit.SECONDS);

            System.out.println("Restoring file.");
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

        // Check if the file is saved
        if(folder.fileIsSavedPathname(filePath)){
            // Create the message
            String message = protocolVersion + " DELETE " + id + " " + folder.getFileIDbyPath(filePath) + " \r\n\r\n";
            
            // Send the message
            MC.sendMessage(message.getBytes());

            System.out.println("Deleting backup of file :" + folder.getFile(filePath).getName());
        } else {
            System.out.println("File was never backed up...");
        }

        return "done";
    }

    /**
     * Reclaims the space used by the peer
     * @param maxSizeString - maximum space the peer can use
     * @return "done" if success
     */
    @Override
    public String reclaim(String maxSizeString){
        try{
            // Set the peer total storage to the argument given
            int maxSize = Integer.parseInt(maxSizeString);
            folder.setStorageSize(maxSize);

            // If the storage used is bigger then the total storage of a peer, the peer needs to delete chunks
            if(maxSize < folder.getStorageUsed()){
                // Boolean used to check if the peer has deleted enough chunks
                Boolean spaceRaclaimed = false;

                // For each file, delete one chunk at a time until the space is reclaimed
                for(String fileID : folder.getStoredChunks().keySet()){
                    // Iterate over the file chunks
                    while(!folder.getStoredChunks().get(fileID).isEmpty()){
                        // Get a chunk to be deleted
                        Chunk chunkToDelete = folder.getStoredChunks().get(fileID).remove(0);

                        // Delete it
                        folder.deleteChunk(fileID, chunkToDelete);

                        // Send a message warning other that a chunk has been removed
                        String message = protocolVersion + " REMOVED " + id + " " + chunkToDelete.getFileID() + " " + chunkToDelete.getNumber() + " \r\n\r\n";
                        MC.sendMessage(message.getBytes());

                        // If the storage size is bigger than the used, than the loop can break
                        if(folder.getStorageSize() > folder.getStorageUsed()){
                            spaceRaclaimed = true;
                            break;
                        }
                    }

                    if(spaceRaclaimed){
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

        // Retrieve the information about all the files stored by the peer
        for(int i = 0; i < folder.getStoredFiles().size(); i++){
            FileData fileState = folder.getStoredFiles().get(i);
            state.append("File number " + i +":\n");
            state.append("  - Pathname: " + fileState.getFilePath() + "\n");
            state.append("  - File ID: " + fileState.getID() + "\n");
            state.append("  - Desired Replication Degree: " + fileState.getReplicationDegree() + "\n");
            for(int j = 0; j < fileState.getFileChunks().size(); j++){
                state.append(" - Chunks number " + i + ":\n");
                state.append("      - ID: " + fileState.getFileChunks().get(i).getNumber() + "\n");
                state.append("      - Perceived Replication Degree: " + folder.getChunkReplication(fileState.getID(), fileState.getFileChunks().get(i).getNumber()) + "\n");
            }
        }

        // Retrieve the information about all the chunks stored by the peer
        for(String key : folder.getStoredChunks().keySet()){
            state.append("Stored Chunks of file " + key + ":\n");
            for(int i = 0; i < folder.getStoredChunks().get(key).size(); i++){
                state.append("- Chunk ID: " + folder.getStoredChunks().get(key).get(i).getNumber() + "\n");
                state.append("  - Size: " + folder.getStoredChunks().get(key).get(i).getSize() + "\n");
                state.append("  - Desired Replication Degree: " + folder.getStoredChunks().get(key).get(i).getDesiredRep() + "\n");
                state.append("  - Perceived Replication Degree: " + folder.getChunkReplication(key, folder.getStoredChunks().get(key).get(i).getNumber()) + "\n");
            }
        }

        // Retrieve the information about the peer's storage
        state.append("\n Peer maximum storage: " + folder.getStorageSize() + "kB \n");
        state.append("\n Peer used storage: " + folder.getStorageUsed() + "kB \n");

        return state.toString();
    }
}