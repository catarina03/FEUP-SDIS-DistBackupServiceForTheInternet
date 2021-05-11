package Tasks;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;

import Peer.Chunk;
import Peer.Peer;

public class RestoreChunkTask implements Runnable {
    private String fileName;

    public RestoreChunkTask(String name){
        this.fileName = name;
    }
    
    @Override
    public void run() {
        if(Peer.getFolder().allChunksRestored()){
            restoreFile();
        }
        
    }

    /**
     * Restores a file using the chunks received
     */
    public void restoreFile(){
        // Create the path where the file will be stored
        String filePath = "../PeersStorage/Peer" + Peer.getID() + "/" + fileName;

        // Get all the chunks of the file and sorted in order
        ArrayList<Chunk> fileChunks = Peer.getFolder().getRestoredChunks();
        Collections.sort(fileChunks);

        // Write the chunks into the file
        try (FileOutputStream fos = new FileOutputStream(filePath)){
            for(Chunk c : fileChunks){
                System.out.println("Writing chunk with number: " + c.getNumber());
                fos.write(c.getData());
            }
            fos.close(); //There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
            
            Peer.getFolder().fileRestored();
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }
    
}
