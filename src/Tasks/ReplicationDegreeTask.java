package Tasks;

import java.util.concurrent.TimeUnit;

import Peer.Peer;

public class ReplicationDegreeTask implements Runnable{
    private String fileID;
    private byte[] putchunkMessage;
    private int desiredReplicationDegree, waitTime, messagessent, chunkNr;

    public ReplicationDegreeTask(byte[] message, String fileID, int chunkNR, int time, int repDegree){
        this.putchunkMessage = message;
        this.fileID = fileID;
        this.chunkNr = chunkNR;
        this.waitTime = time;
        this.desiredReplicationDegree = repDegree;
        this.messagessent = 1;
    }

    @Override
    public void run() {
        // Get all the times a chunk was stored
        int chunkSavedTimes = Peer.getFolder().getChunkReplication(this.fileID, this.chunkNr);

        // If the chunk hasn't been saved enough times, then send the putchunk message again
        if(chunkSavedTimes < this.desiredReplicationDegree){
            // Send the message
            Peer.getBackupChannel().sendMessage(this.putchunkMessage);

            // Double the wait time
            this.waitTime *= 2;
            this.messagessent++;
            System.out.println("sent PUTCHUNK waiting " + this.waitTime +"s, times sent: " + this.messagessent);

            // Send the putchunk message a maximum of 5 times
            if(this.messagessent < 5){
                Peer.getThreadPool().schedule(this, this.waitTime, TimeUnit.SECONDS);
            }
            else{
                System.out.println("Failed to get the desired replication degree of chunk " + this.chunkNr);
                System.out.println("Replication Degree is " + chunkSavedTimes + ", the desired one was " + this.desiredReplicationDegree);
            }
        }
        
    }
    
}