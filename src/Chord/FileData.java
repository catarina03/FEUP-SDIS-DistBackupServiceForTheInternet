import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

public class FileData {
    private int nrChunks, replicationDegree;
    private ArrayList<Chunk> chunks;
    private File file;
    private String fileID;
    private int fileNumber, fileSize;
    private ChordNode initiatorPeer;

    public FileData(String filePath, int repDegree, int number){
        this.file = new File(filePath);
        this.chunks = new ArrayList<>();
        this.nrChunks = 0;
        this.replicationDegree = repDegree;
        this.fileNumber = number;
        calculatID();

        splitFile();
    }

    public FileData(String id, int chunks, int repDegree, String size, String filePath){
        this.fileID = id;
        this.nrChunks = chunks;
        this.replicationDegree = repDegree;
        this.chunks = new ArrayList<>();
        this.file = new File(filePath);
        fileSize = Integer.parseInt(size);
    }

    public int getFileSize(){
        if(chunks.isEmpty()){
            return fileSize;
        }
        
        int size = 0;

        for(int i = 0; i < chunks.size(); i++){
            size += chunks.get(i).getSize();
        }

        return size;
    }

    public void addChunk(Chunk chunk){
        chunks.add(chunk);
    }

    private void splitFile(){
        int chunkSize = 10000;
        byte[] chunkBuffer = new byte[chunkSize];

        // Loop thourgh the file while saving its content into chunks
        try(FileInputStream fis = new FileInputStream(this.file); BufferedInputStream bis = new BufferedInputStream(fis)){
            int bytesRead = 0;
            while((bytesRead = bis.read(chunkBuffer)) > 0){
                // Get the information read
                byte[] chunkContent = Arrays.copyOf(chunkBuffer, bytesRead);

                // Create a new chunk with the information read
                this.nrChunks++;
                Chunk newChunk = new Chunk(this.nrChunks, bytesRead, chunkContent, this.fileID);

                this.chunks.add(newChunk);

                // Clear the buffer
                chunkBuffer = new byte[chunkSize];
            }

            // If the last chunk  read has 64Kb in size, create a chunk with size 0 to be the last one
            if(this.file.length() % chunkSize == 0){
                this.nrChunks++;
                Chunk newChunk = new Chunk(this.nrChunks, 0, null, this.fileID);
                this.chunks.add(newChunk);
            } 
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private void calculatID(){
        // Create a String to be used to generate the file ID. 
        // Uses the name of the files, the last time it was modified and the owner to ensure it's a unique string
        String fileMetadata = this.file.getName() + this.file.lastModified() + this.file.getParent() + this.fileNumber;

        String encondedID = sha1Encode(fileMetadata); 
        this.fileID = "" + Integer.parseInt(encondedID.substring(encondedID.length() - ChordPeer.getIdBits()/4), 16);

    }

    private String sha1Encode(final String stringToEncode) {
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-1");
            final byte[] hash = digest.digest(stringToEncode.getBytes("UTF-8"));
            final StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < hash.length; i++) {
                final String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) 
                  hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception e){
           e.printStackTrace();
           return "";
        }
    }

    /**
     * @return file ID
     */
    public String getID(){
        return this.fileID;
    }

    /**
     * @return total numbers of chunk of the file
     */
    public int getTotalChunks(){
        return this.nrChunks;
    } 

    /**
     * @return Desired replicaton degree of each chunk of the file
     */
    public int getReplicationDegree(){
        return this.replicationDegree;
    }
    
    /**
     * Search and return a specific number
     * @param chunkNumber - number of the chunk to be retrieved
     * @return the chunk that was asked
     */
    public Chunk getChunk(int chunkNumber){
        return this.chunks.get(chunkNumber);
    }

    /**
     * @return the path of the file
     */
    public String getFilePath(){
        return this.file.getPath();
    }

    /**
     * @return the name of the file
     */
    public String getFileName(){
        return this.file.getName();
    }

    /**
     * @return All the chunks that make up the file
     */
    public ArrayList<Chunk> getFileChunks(){
        return this.chunks;
    }
    
    /**
     * @return file object that represents the file
     */
    public File getFile(){
        return this.file;
    } 

    /**
     * @return ChordNode representing the initiator peer
     */
    public ChordNode getInitiatorPeer() {
        return initiatorPeer;
    }

    /**
     * @param initiatorPeer - peer who initiated the backup process of the file
     */
    public void setInitiatorPeer(ChordNode initiatorPeer) {
        this.initiatorPeer = initiatorPeer;
    }
}
