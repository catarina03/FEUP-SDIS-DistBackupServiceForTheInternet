import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;


public class PeerFolder {
    private Path peerFolder;
    private int storageSize, storageUsed;
    private ArrayList<FileData> filesBackedUp;

    /*
        key --> File ID
        Value --> ID of the Peer who stored it
    */
    private ConcurrentHashMap<String, ChordNode> fileLocation;

    /*
        key --> File ID
        Value --> File Data
    */
    private ConcurrentHashMap<String, FileData> storedFiles;

    /*
        key --> File Path
        Value --> File Data
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
        fileLocation = new ConcurrentHashMap<>();
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
     * @return HashMap of the file locations
     */
    public ConcurrentHashMap<String, ChordNode> getFileLocation() {
        return fileLocation;
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
     * @param fileID - id of the file
     * @return true if the file is saved, false otherwise
     */
    public boolean fileIsStored(String fileID){
        return storedFiles.containsKey(fileID);
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
     * Check if a certain file is saved with its pathn
     * @param pathname - path of the file
     * @return  true if the file is saved, false otherwise
     */
    public boolean fileIsStoredPathname(String pathname){
        Collection<FileData> collection = storedFiles.values();

        for(FileData file : collection){
            if(file.getFilePath().equals(pathname)){
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a file to the storage
     * @param file - file to be stored
     */
    public void addFile(FileData file){
        filesBackedUp.add(file);
    }

    /**
     * Adds a file location
     * @param file - file to be stored
     */
    public void addFileLocation(String fileID, ChordNode node){
        fileLocation.put(fileID, node);
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
     * @param file - file to be stored
     */
    public void storeFile(String fileID, FileData file){
        storedFiles.put(fileID, file);

        storageUsed += file.getFileSize();
    }

    /**
     * Saves a chunk into the array and saves it into the peer folder
     * @param fileID - id of the file to which the chunk belongs
     * @param chunk - chunkToBeSaved
     */
    public void saveChunk(String fileID, Chunk chunk){
        storedFiles.get(fileID).addChunk(chunk);
    }
}
