package Tasks;

import java.util.concurrent.TimeUnit;

import Peer.Chunk;
import Peer.Peer;

public class SaveChunkTask implements Runnable{

    private String[] header;
    private Chunk chunkToSave;

    public SaveChunkTask(String[] header, Chunk c){
        this.header = header;
        this.chunkToSave = c;
    }

    @Override
    public void run() {
        // If the version is 2.0 then the peer only stores the chunk if it doesn't have the desired replication degree
        if(header[0].equals("2.0") && Peer.getProtocolVersion().equals("2.0")){
            if(Peer.getFolder().chunkToStoreHasDesiredReplication(this.chunkToSave.getFileID(), this.chunkToSave.getNumber(), this.chunkToSave.getDesiredRep())){
                System.out.println("Replication degree achived");
                return;
            }
        }

        // Save the chunk into the peer's folder
        Peer.getFolder().saveChunk(this.header[3], this.chunkToSave, Peer.getID());

        // Send a message warning other peers that the chunk was stored
        Peer.getThreadPool().schedule(new SendStoreMessageTask(header), 0, TimeUnit.MILLISECONDS); 
        
    }
    
}
