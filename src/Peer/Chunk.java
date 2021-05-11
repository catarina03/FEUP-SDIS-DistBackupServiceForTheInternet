package Peer;

public class Chunk implements Comparable{
    private int number, size, desiredRep;
    private String fileID;
    private byte[] data;

    public Chunk(int nr, int size, byte[] content, String file, int desiredRep){
        this.number = nr;
        this.size = size;
        this.data = content;
        this.fileID = file;
        this.desiredRep = desiredRep;
    }

    // To be used when creating a chunk which is used to restore a file (replciation degree doesn't matter)
    public Chunk(int nr, int size, byte[] content, String file){
        this.number = nr;
        this.size = size;
        this.data = content;
        this.fileID = file;
    }

    /**
     * @return data of the chunk
     */
    public byte[] getData(){
        return data;
    }

    /**
     * @return id of the file to which the peer belongs
     */
    public String getFileID(){
        return fileID;
    }

    /**
     * @return number of the chunk
     */
    public int getNumber (){
        return number;
    }

    /**
     * @return chunk size in bytes
     */
    public int getSize(){
        return size;
    }

    /**
     * @return chunk desired replication degree
     */
    public int getDesiredRep() {
        return desiredRep;
    }

    @Override
    public int compareTo(Object o) {
        int comparableValue = ((Chunk) o).getNumber();
        
        return this.number - comparableValue;
    }
}
