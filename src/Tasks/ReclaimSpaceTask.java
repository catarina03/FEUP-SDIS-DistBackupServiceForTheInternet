package Tasks;

import java.util.concurrent.TimeUnit;

import Peer.Chunk;
import Peer.Peer;

public class ReclaimSpaceTask implements Runnable{
    
    private String[] message;

    public ReclaimSpaceTask(String[] header){
        this.message = header;
    }
    

    @Override
    public void run() {
        String fileID = this.message[3].trim();
        int chunkNr = Integer.parseInt(this.message[4].trim());

        // Get the file to be sent
        Chunk chunk = Peer.getFolder().getFileChunk(fileID, chunkNr);

        // Only send the chunk if the chunk doesn't have the desired replication degree
        if(!Peer.getFolder().chunkStoredHasDesiredReplication(fileID, chunkNr)){
            // Create header and body of the message
            String header = this.message[0] + " PUTCHUNK " + Peer.getID() + " " + fileID + " " + chunkNr + " " + chunk.getDesiredRep() + " \r\n\r\n";
            byte[] body = chunk.getData();
            byte[] headerBytes = header.getBytes();

            // Join the header and the boddy into an array
            byte[] putchunkMessage = new byte[headerBytes.length + body.length];
            System.arraycopy(headerBytes, 0, putchunkMessage, 0, headerBytes.length);
            System.arraycopy(body, 0, putchunkMessage, headerBytes.length, body.length);

            // Send the message
            Peer.getBackupChannel().sendMessage(putchunkMessage);
            
            // Schedule a task to check if the desired replication degree was achieved
            Peer.getThreadPool().schedule(new ReplicationDegreeTask(putchunkMessage, fileID, chunkNr, 1, chunk.getDesiredRep()), 1, TimeUnit.SECONDS);
        }

    }

    
}
