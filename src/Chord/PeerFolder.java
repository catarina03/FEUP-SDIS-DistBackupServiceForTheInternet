import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class PeerFolder {
    private Path peerFolder;
    private int storageSize, storageUsed;
    private ArrayList<FileData> filesBackedUp;

    /*
        key --> File Path
        Value --> File Data
    */
    private ConcurrentHashMap<String, FileData> storedFiles;

    /*
        key --> File Path
        Value --> Nodes who backupd the File
    */
    private ConcurrentHashMap<String, ArrayList<ChordNode>> backupNodes;
    
    public PeerFolder(String peerID){
        try {
            this.peerFolder = Paths.get("../PeersStorage/Peer"+peerID);

            if(!Files.exists(peerFolder)){
                Files.createDirectories(peerFolder);
            }
            
            
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        filesBackedUp = new ArrayList<>();
        storedFiles = new ConcurrentHashMap<>();
        backupNodes= new ConcurrentHashMap<>();
        storageSize = 1000000;
        storageUsed = 0;
    }

    /**
     * Get a specific file saved in the folder
     * @param filePath - path of the file to be retrieved
     * @return null if the file isn't saved, or the file with the path given
     */
    public File getFile(String filePath){
        for(int i = 0; i < filesBackedUp.size(); i++){
            if(filesBackedUp.get(i).getFilePath().equals(filePath)){
                return filesBackedUp.get(i).getFile();
            }
        }
        return null;
    }

    /**
     * @return array containing the files stored in the folder
     */
    public ArrayList<FileData> getFilesBackedUp(){
        return filesBackedUp;
    }

    public ConcurrentHashMap<String, ArrayList<ChordNode>> getBackupNodes() {
        return backupNodes;
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
     * @return HashMap of the stored files
     */
    public ConcurrentHashMap<String, FileData> getStoredFiles() {
        return storedFiles;
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
        for(int i = 0; i < filesBackedUp.size(); i++){
            if(filesBackedUp.get(i).getFilePath().equals(filePath)){
                return filesBackedUp.get(i).getID();
            }
        }
        return null;
    }

    /**
     * Retrieves the ID of a  file saved by its path
     * @param filePath - path of the file to get the ID
     * @return  return the ID of the file
     */
    public FileData getFilebyPath(String filePath){
        for(int i = 0; i < filesBackedUp.size(); i++){
            if(filesBackedUp.get(i).getFilePath().equals(filePath)){
                return filesBackedUp.get(i);
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
        for(int i = 0; i < filesBackedUp.size(); i++){
            if(filesBackedUp.get(i).getFilePath().equals(pathname)){
                return filesBackedUp.get(i).getFileChunks().size();
            }
        }
        return 0;
    }

    /**
     * Check if a certain file is saved in the folder
     * @param fileID - id of the file
     * @return true if the file is saved, false otherwise
     */
    public boolean fileIsSaved(String fileID){
        for(int i = 0; i < filesBackedUp.size(); i++){
            if(filesBackedUp.get(i).getID().equals(fileID)){
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a certain file is saved in the folder
     * @param filePath - id of the file
     * @return true if the file is saved, false otherwise
     */
    public boolean fileIsStored(String filePath){
        return storedFiles.containsKey(filePath);
    }

    /**
     * Check if a certain file is saved with its pathn
     * @param pathname - path of the file
     * @return  true if the file is saved, false otherwise
     */
    public boolean fileIsSavedPathname(String pathname){
        for(int i = 0; i < filesBackedUp.size(); i++){
            if(filesBackedUp.get(i).getFilePath().equals(pathname)){
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a certain file is saved with its path
     * @param pathname - path of the file
     * @return  true if the file is saved, false otherwise
     */
    public boolean fileIsStoredPathname(String pathname){

        return storedFiles.containsKey(pathname);
    }

    /**
     * Adds a file to the storage
     * @param file - file to be stored
     */
    public void addFile(FileData file){
        filesBackedUp.add(file);
    }

    /**
     * Adds a node that has backed up a file with filePath
     * @param filePath - path of the file which node backed up
     * @param node - node that stored the file
     */
    public void addBackupNode(String filePath, ChordNode node){
        if(backupNodes.containsKey(filePath)){
            backupNodes.get(filePath).add(node);
            return;
        }

        backupNodes.put(filePath, new ArrayList<>());
        backupNodes.get(filePath).add(node);
    }

    /**
     * Stores a file
     * @param filePath - path of the file to be saved
     * @param file - file to be stored
     */
    public void storeFile(String filePath, FileData file){
        storedFiles.put(filePath, file);

        storageUsed += file.getFileSize();
    }

    /**
     * Saves a chunk into the array and saves it into the peer folder
     * @param filePath - path of the file to which the chunk belongs
     * @param chunk - chunkToBeSaved
     */
    public void saveChunk(String filePath, Chunk chunk){
        storedFiles.get(filePath).addChunk(chunk);
    }
}
