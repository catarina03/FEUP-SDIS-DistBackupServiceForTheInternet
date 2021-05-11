package Peer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class PeerFolder {
    private Path peerFolder;
    private int storageSize, storageUsed;
    private ArrayList<FileData> storedFiles;
    
    /*
        key --> File ID
        Value --> Array with the chunks of that file
    */
    private ConcurrentHashMap<String, ArrayList<Chunk>> storedChunks;

    /*
        Array with the chunks of a file to be restored
    */
    private ArrayList<Chunk> restoredChunks;

    /*
        Array with the deleted chunks
    */
    private ArrayList<String> deletedChunks;

    /*
        Array with the chunks ids of the chunks that a peer wanted
    */
    private ArrayList<String> mdrChunkIds;

    /*
        key --> FileID:ChunkNr
        Value --> Boolean representing if the chunk was received or not
    */
    private ConcurrentHashMap<String, Boolean> wantedChunks;


    /*
        key --> fileID:Chunkr
        value --> number of times the chunk is stored in other peers
    */
    private ConcurrentHashMap<String, ArrayList<String>> chunkReplicationStatus;

    
    
    public PeerFolder(String peerID){
        try {
            this.peerFolder = Paths.get("../PeersStorage/Peer"+peerID);

            if(!Files.exists(peerFolder)){
                Files.createDirectories(peerFolder);
            }
            
            
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        storedChunks = new ConcurrentHashMap<>();
        storedFiles = new ArrayList<>();
        deletedChunks = new ArrayList<>();
        restoredChunks = new ArrayList<>();
        chunkReplicationStatus = new ConcurrentHashMap<>();
        wantedChunks = new ConcurrentHashMap<>();
        storageSize = 1000000;
        storageUsed = 0;
        mdrChunkIds = new ArrayList<>();
    }

    /**
     * Get a specific file saved in the folder
     * @param filePath - path of the file to be retrieved
     * @return null if the file isn't saved, or the file with the path given
     */
    public File getFile(String filePath){
        for(int i = 0; i < storedFiles.size(); i++){
            if(storedFiles.get(i).getFilePath().equals(filePath)){
                return storedFiles.get(i).getFile();
            }
        }
        return null;
    }

    /**
     * @return array containing the files stored in the folder
     */
    public ArrayList<FileData> getStoredFiles(){
        return storedFiles;
    }

    /**
     * @return size of the storage
     */
    public int getStorageSize(){
        return storageSize;
    }

    /**
     * @return amount of storage used
     */
    public int getStorageUsed(){
        return storageUsed;
    }

    /**
     * @return path of the folder
     */
    public String getPath(){
        return peerFolder.toString();
    }

    /**
     * @return ConcurrentHashMap representing a checklist of the chunks requested
     */
    public ConcurrentHashMap<String, Boolean> getWantedChunks(){
        return wantedChunks;
    }

    /**
     * @return ConcurrentHashMap containing the chunks stored
     */
    public ConcurrentHashMap<String, ArrayList<Chunk>> getStoredChunks(){
        return storedChunks;
    }

    public ArrayList<String> getDeletedChunks() {
        return deletedChunks;
    }

    /**
     * Retrieves all the restored chunks
     * @return array containg all the chunks
     */
    public ArrayList<Chunk> getRestoredChunks(){
        return this.restoredChunks;
    }

    /**
     * Changes the storage size
     * @param new storage size
     */
    public void setStorageSize(int size){
        this.storageSize = size;
    }

    /**
     * Retrieves the ID of a  file saved by its path
     * @param filePath - path of the file to get the ID
     * @return  return the ID of the file
     */
    public String getFileIDbyPath(String filePath){
        for(int i = 0; i < storedFiles.size(); i++){
            if(storedFiles.get(i).getFilePath().equals(filePath)){
                return storedFiles.get(i).getID();
            }
        }
        return null;
    }

    /**
     * Retrieves the number of chunks saved of a certain file
     * @param pathname of the file
     * @return number of chunks stored that belong to the file with pathname given
     */
    public int getFileChunksSize(String pathname){
        for(int i = 0; i < storedFiles.size(); i++){
            if(storedFiles.get(i).getFilePath().equals(pathname)){
                return storedFiles.get(i).getFileChunks().size();
            }
        }
        return 0;
    }

    /**
     * Retrieves the perceived replication degree of a certain chunk
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     * @return  perceived replication degree of the chunk
     */
    public Integer getChunkReplication(String fileID, int chunkNr){
        String hashKey =  fileID + ":" + chunkNr;
        
        return chunkReplicationStatus.get(hashKey).size();
    }

    /**
     * Searchs for a certain chunk of a file
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk to be searched
     * @return chunk if found, null if no chunk was found
     */
    public Chunk getFileChunk(String fileID, int chunkNr){
        ArrayList<Chunk> fileChunks = storedChunks.get(fileID);
        
        for(int i = 0; i < fileChunks.size(); i++){
            if(fileChunks.get(i).getNumber() == chunkNr){
                return fileChunks.get(i);
            }
        }

        return null;
    }

    /**
     * Check if a certain file is saved in the folder
     * @param fileID - id of the file
     * @return true if the file is saved, false otherwise
     */
    public boolean fileIsSaved(String fileID){
        for(int i = 0; i < storedFiles.size(); i++){
            if(storedFiles.get(i).getID().equals(fileID)){
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a certain file is saved with its pathn
     * @param pathname - path of the file
     * @return  true if the file is saved, false otherwise
     */
    public boolean fileIsSavedPathname(String pathname){
        for(int i = 0; i < storedFiles.size(); i++){
            if(storedFiles.get(i).getFilePath().equals(pathname)){
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a certain chunk is stored
     * @param chunk - Chunk object
     * @return true if the chunk is saved, false otherwise
     */
    public boolean chunkIsStored(Chunk chunk) {

        if(storedChunks.containsKey(chunk.getFileID())){
            ArrayList<Chunk> savedChunks = this.storedChunks.get(chunk.getFileID());

            for(int i = 0; i < savedChunks.size(); i++){
                if(chunk.getNumber() == savedChunks.get(i).getNumber()){
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if all the chunks requested have been delivered
     * @return true if all the wanted chunks were received
     */
    public boolean allChunksRestored(){
        return !wantedChunks.containsValue(false);
    }

    /**
     * Check if a chunk that was requested was already stored
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     * @return true if the chunk was stored, false otherwise
     */
    public boolean chunkAlreadyRestored(String fileID, int chunkNr){
        return wantedChunks.get(fileID + ":" + chunkNr);
    }

    /**
     * Checks if a chunk that the peer has stored has the desired replication degree
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     * @return true if the chunk has the desired replication degree, false otherwise
     */
    public Boolean chunkStoredHasDesiredReplication(String fileID, int chunkNr){
        int chunkRep = getChunkReplication(fileID, chunkNr);

        Chunk chunk = getFileChunk(fileID, chunkNr);

        return chunkRep >= chunk.getDesiredRep();
    }

    /**
     * Checks if a chunk that the peer is going to store has the desired replication degree
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     * @param desiredRep - desired replication degree of the chunk
     * @return true if the chunk has the desired replication degree, false otherwise
     */
    public Boolean chunkToStoreHasDesiredReplication(String fileID, int chunkNr, int desiredRep){
        String hashKey = fileID + ":" + chunkNr;

        if(!chunkReplicationStatus.containsKey(hashKey)){
            return false;
        }
        
        int chunkRep = getChunkReplication(fileID, chunkNr);

        return chunkRep >= desiredRep;
    }

    /**
     * Adds a file to the storage
     * @param file - file to be stored
     */
    public void addFile(FileData file){
        storedFiles.add(file);
    }
    
    /**
     * Add a chunk that was requested by the peer
     * @param chunk - chunk received
     */
    public void addRestoredChunk(Chunk chunk){
        restoredChunks.add(chunk);
    }

    /**
     * Adds the peerID of the peer who saved a specific chunk
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     * @param peerID - id of the peer that stored the chunk
     */
    public void addChunkReplication(String fileID, int chunkNr, String peerID ){
        String hashKey =  fileID + ":" + chunkNr;

        if(chunkReplicationStatus.containsKey(hashKey)){
            if(chunkReplicationStatus.get(hashKey).contains(peerID)) return;

            chunkReplicationStatus.get(hashKey).add(peerID);
            return;
        }
        else{
            // If the key doens't exist, create the array and save it with the hashKey as a key
            ArrayList<String> peerIDs = new ArrayList<String>();
            peerIDs.add(peerID);
            chunkReplicationStatus.put(hashKey, peerIDs);
        }
    }

    /**
     * Removes the peer id of a peer that removed a chunk
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     * @param peerID - id of the peer that removed the chunk
     */
    public void decreaseChunkReplication(String fileID, int chunkNr, String peerID){
        String hashKey =  fileID + ":" + chunkNr;

        if(chunkReplicationStatus.containsKey(hashKey) && chunkReplicationStatus.get(hashKey).contains(peerID)){
            chunkReplicationStatus.get(hashKey).remove(peerID);
            return;
        }
    }

    /**
     * Adds a wanted chunk to the hashmap, so that the peer can check it in the future
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     */
    public void addWantedChunk(String fileID, int chunkNr){
        wantedChunks.put(fileID + ":" + chunkNr, false);
    }

    /**
     * Saves a chunk into the array and saves it into the peer folder
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     * @param peerID - id of the peer that stored the chunk
     */
    public void saveChunk(String fileID, Chunk chunk, String peerID){
        // If the key exists just add the chunk into the array
        if(storedChunks.containsKey(fileID)){
            storedChunks.get(fileID).add(chunk);
        }
        else{
            // If the key doens't exist, create the array and save it with the fileID as a key
            ArrayList<Chunk> chunks = new ArrayList<Chunk>();
            chunks.add(chunk);
            storedChunks.put(fileID, chunks);
        }

        addChunkReplication(fileID, chunk.getNumber(), peerID);

        // Save the chunk into a real file in the peer's folder directory
        try (FileOutputStream fos = new FileOutputStream(peerFolder.toString()+"/"+chunk.getFileID()+chunk.getNumber())){
            fos.write(chunk.getData());
            storageUsed += chunk.getSize()/1000;
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Delete a specific chunk from the array and from the peer's folder directory
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     */
    public void deleteChunk(String fileID, Chunk chunk){
        // Check if the chunk is indeed stored
        if(storedChunks.containsKey(fileID)){
            storageUsed -= chunk.getSize() / 1000;
            
            storedChunks.get(fileID).remove(chunk);

            File fileToDelete = new File(peerFolder.toString()+"/"+chunk.getFileID()+chunk.getNumber());
            
            // Add the deleted chunk to an array
            if(fileToDelete.delete()) {
                this.deletedChunks.add(chunk.getFileID()+chunk.getNumber());
                System.out.println("Deleted chunk:" + chunk.getFileID()+chunk.getNumber());
            }
        }

        
        if(chunkReplicationStatus.containsKey(fileID+":"+chunk.getNumber())){
            chunkReplicationStatus.remove(fileID+":"+chunk.getNumber());
        }
    }

    /**
     * Updates the wantedChunks hashmap
     * @param fileID - id of the file to which the chunk belongs
     * @param chunkNr - number of the chunk
     */
    public void wantedChunkReceived(String fileID, int chunkNr){
        wantedChunks.put(fileID + ":" + chunkNr, true);
    }

    /**
     * Restore all the data structs used to restore a file
     */
    public void fileRestored(){
        this.wantedChunks.clear();
        this.restoredChunks.clear();
    }
    
    /**
     * Deletes all the chunks of a certain file
     * @param filename - name of the file
     */
    public void deleteChunks(String filename){
        if(storedChunks.containsKey(filename)){
            while(!storedChunks.get(filename).isEmpty()){
                deleteChunk(filename, storedChunks.get(filename).get(0));
            }
            storedChunks.remove(filename);
        }
    }

    /**
     * Get the array containing the IDs of all the chunks received from de MDR channel
     * @return array with all the chunks
     */
    public ArrayList<String> getMdrChunkIds() {
        return mdrChunkIds;
    }
}
