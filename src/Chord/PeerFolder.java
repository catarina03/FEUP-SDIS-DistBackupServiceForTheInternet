import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class PeerFolder {
    private Path peerFolder;
    private int storageSize, storageUsed;
    private ArrayList<FileData> filesBackedUp;
    private ArrayList<Chunk> fileToRestore;

    /*
        key --> File Path
        Value --> File Data
    */
    private ConcurrentHashMap<String, FileData> storedFiles;

    /*
        key --> FileData
        Value --> Node where the file is stored
    */
    private ConcurrentHashMap<FileData, ChordNode> fileLocation;
    
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
        fileLocation = new ConcurrentHashMap<>();
        storageSize = 1000000;
        storageUsed = 0;
        fileToRestore = new ArrayList<>();
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
     * Get a specific file stored in the folder
     * @param filePath - path of the file to be retrieved
     * @return null if the file isn't stored, or the file with the path given
     */
    public FileData getStoredFiles(String filePath){
        return storedFiles.get(filePath);
    }

    /**
     * @return array containing the files stored in the folder
     */
    public ArrayList<FileData> getFilesBackedUp(){
        return filesBackedUp;
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
     * @return file to restore
     */
    public ArrayList<Chunk> getFileToRestore() {
        return fileToRestore;
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
     * @return HashMap of the stored files
     */
    public ConcurrentHashMap<FileData, ChordNode> getFileLocation() {
        return fileLocation;
    }
    
    /**
     * Changes the storage size
     * @param new storage size
     */
    public void setStorageSize(int size){
        this.storageSize = size;
    }

    /**
     * Changes the file to restore
     * @param fileToRestore - new file to restore
     */
    public void setFileToRestore(ArrayList<Chunk> fileToRestore) {
        this.fileToRestore = fileToRestore;
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

    public FileData getStoredFile(String filePath){
        return storedFiles.get(filePath);
    }

     /**
     * Retrieves a  file stored by a specific node
     * @param filePath - path of the file
     * @param node - node that stored the file
     * @return  return file
     */
    public FileData getFilebyNode(String filePath, ChordNode node){
        Iterator<Map.Entry<FileData, ChordNode>> iterator = fileLocation.entrySet().iterator();

        while(iterator.hasNext()){
            Map.Entry<FileData, ChordNode> entry = iterator.next();
            if(entry.getKey().getFilePath().equals(filePath) && node.getId() == entry.getValue().getId()){
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Retrieves all  nodes that stored files with a specific filePath
     * @param filePath - path of the file
     * @return  return array containing all nodes
     */
    public ArrayList<ChordNode>  getFileLocation(String filePath){
        Iterator<Map.Entry<FileData, ChordNode>> iterator = fileLocation.entrySet().iterator();
        ArrayList<ChordNode> nodes = new ArrayList<>();
        while(iterator.hasNext()){
            Map.Entry<FileData, ChordNode> entry = iterator.next();
            if(entry.getKey().getFilePath().equals(filePath)){
                nodes.add(entry.getValue());
            }
        }
        
        return nodes;
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
     * Check if a certain file is saved in the folder
     * @param filePath - id of the file
     * @return true if the file is saved, false otherwise
     */
    public boolean fileIsStored(String filePath){
        return storedFiles.containsKey(filePath);
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
     * Adds a node that has backed up a file
     * @param file - file which node backed up
     * @param node - node that stored the file
     */
    public void addFileLocation(FileData file, ChordNode node){
        fileLocation.put(file, node);

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

    /**
     * Restores a chunk from a file
     * @param chunk - chunkToBeSaved
     */
    public void restoreChunk(Chunk chunk){
        fileToRestore.add(chunk);
    }

    /**
     * Physically restores the file into the peer folder
     */
    public void restoreFile(String fileName){
        Collections.sort(fileToRestore);

        // Write the chunks into the file 
        try (FileOutputStream fos = new FileOutputStream(peerFolder.toString() + "/" + fileName)){
            for(Chunk c : fileToRestore){
                fos.write(c.getData());
            }
            fos.close(); //There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Deletes all information about all files with a specific filePath
     * @param filePath - path of the file
     */
    public void  deleteFileLocations(String filePath){
        Iterator<Map.Entry<FileData, ChordNode>> iterator = fileLocation.entrySet().iterator();

        while(iterator.hasNext()){
            Map.Entry<FileData, ChordNode> entry = iterator.next();
            if(entry.getKey().getFilePath().equals(filePath)){
                fileLocation.remove(entry.getKey());
            }
        }
    }

    /**
     * Deletes the information about a file
     * @param fileToDelete - file to remove
     */
    public void  deleteFileLocation(FileData fileToDelete){
        fileLocation.remove(fileToDelete);
    }

    /**
     * Delete stored file information from storedFiles of a file that has been deleted
     * @param filePath - path of the file tha has been deleted
     */
    public void deleteStoredFile(String filePath){
        storedFiles.remove(filePath);
    }

    /**
     * Removes a file from storage
     * @param filePath - path of the file to be removed
     */
    public void removeFile(String filePath){
        Iterator<FileData> iter = filesBackedUp.iterator();

        while(iter.hasNext()){
            FileData file = iter.next();
            if(file.getFilePath().equals(filePath)){
                filesBackedUp.remove(file);
            }
        }
    }
}
