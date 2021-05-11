package Channels;

import java.io.File;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import Peer.Chunk;
import Peer.Peer;
import Tasks.ReclaimSpaceTask;
import Tasks.SaveChunkTask;
import Tasks.SendChunkTask;
import Tasks.SendStoreMessageTask;
import Tasks.VerifyDeleteTask;

public class MessageParser implements Runnable{
    private byte[] message;

    public MessageParser(byte[] messageBytes){
        this.message = messageBytes;
    }

    /**
     * Parses the message received in the constructor.
     * Executes the command depending on the message.
     */
    public void run(){
        int headerSize;

        for(headerSize = 0; headerSize < this.message.length - 4; headerSize++){
            // Check for the <CRLF><CRLF> that always sperarates the header from the body
            if(this.message[headerSize] == 0xD && this.message[headerSize+1] == 0xA && this.message[headerSize+2] == 0xD && this.message[headerSize+3] == 0xA){
                break;
            }
        }

        byte[] headerBytes = Arrays.copyOfRange(this.message, 0, headerSize);
        byte[] body;

        // Transform header into a String array so it is easier to access
        String[] header = (new String(headerBytes)).split(" ");

        // If it was the peer who sent the message we can ignore it
        if(Peer.getID().equals(header[2].trim())){
            return;
        } 

        // Body only exists if the header isn't the same size as the message minus the <CRLF><CRLF>
        if(headerSize != this.message.length - 4){
            body = Arrays.copyOfRange(this.message, headerSize + 4, this.message.length);
        }
        else{
            body = null;
        }

        System.out.println("Message Parsed.");

        // Parse the operation to be executed
        switch (header[1].trim()) {
            // The message contained chunk data to be saved
            case "PUTCHUNK":
                saveChunk(header, body);
                break;

            // One peer saved a chunk
            case "STORED":
                updateReplicationDegree(header);
                break;

            // One peer needs a certain chunk
            case "GETCHUNK":
                sendChunk(header);
                break;
            
            // One peer sent the chunk this peer needed
            case "CHUNK":
                restoreChunk(header, body);
                break;

            // The message contained the id of a file and chunk to be deleted
            case "DELETE":
                deleteChunk(header);
                break;
            
            // One peer removed a chunk due to space constrains
            case "REMOVED":
                restoreChunkReplication(header);
                break;
            
            // One peer wants to know if any of the files where deleted
             case "VERIFYFILES":
                if(!Peer.getProtocolVersion().equals("2.0")) return;
                verifyPeerFiles();
                break;
            
            // One Peer has sent a chunk to be deleted
            case "DELETETHISCHUNK":
                
                deleteOneChunk(header[2].trim());
                break;

            default:
                break;
            
        }
        
    }

    /**
     * Checks if the chunk received can be saved, if it is then saves it into the peer folder.
     * @param header - contains basic information about the operation and the peer who sent it;
     * @param body - contains the data of the chunk to be saved;
     */
    private static void saveChunk(String[] header, byte[] body){
        
        // Create the chunk based on the information received
        Chunk newChunk = new Chunk(Integer.parseInt(header[4].trim()), body.length, body, header[3], Integer.parseInt(header[5].trim()));
        
        // Generate the time that the peer will wait before sending the stored message
        Random random = new Random();
        int rndTime = random.nextInt(401);

        // If the chunk already stored, alert other peers that you have it stored already
        if(Peer.getFolder().chunkIsStored(newChunk)){
            System.out.println("Chunk already Stored.");
            Peer.getThreadPool().schedule(new SendStoreMessageTask(header), rndTime, TimeUnit.MILLISECONDS); 
            return;
        }

        // Peers that have the filed save can't save chunks of that file
        if(Peer.getFolder().fileIsSaved(newChunk.getFileID())){
            return;
        }

        // A peer can only store a chunk if it has enought space for it
        if((Peer.getFolder().getStorageUsed() + newChunk.getSize()/1000) > Peer.getFolder().getStorageSize()){
            System.out.println("Not enough space to store chunk.");
            return;
        }    
        
        // Schedule the task with the time generate to save the chunk
        Peer.getThreadPool().schedule(new SaveChunkTask(header, newChunk), rndTime, TimeUnit.MILLISECONDS);  
    }

    /**
     * Updates the replication degree of a certain chunk
     * @param header - contains the information about who stored the chunk and which chunk was it
     */
    private static void updateReplicationDegree(String[] header){
        // Since a peer saved a chunk, increment by 1 the replication degree of that chunk
        Peer.getFolder().addChunkReplication(header[3].trim(), Integer.parseInt(header[4].trim()), header[2].trim());
        System.out.println("Updated chunk replication degree.");

    }

    /**
     * Send a chunk thourgh the multicast channel or thourgh the TCP to the peer who asked for the chunk
     * @param header - contains the information about which chunk to send and which peer wanted it
     */
    private static void sendChunk(String[] header){
        // Generate the time that the peer will wait before sending the chunk
        Random random = new Random();
        int rndTime = random.nextInt(401);

        // Schedule the task with the time generate to send the chunk
        Peer.getChunkPool().schedule(new SendChunkTask(header), rndTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Saves a chunk that was requested to restore a file.
     * @param header - contains information about which chunk it is and who sent it;
     * @param body - data of the chunk sent;
     */
    private static void restoreChunk(String[] header, byte[] body){
        // Create the chunk based on the information received
        Chunk newChunk = new Chunk(Integer.parseInt(header[4].trim()), body.length, body, header[3]);

        // Check if the peer asked for a chunk
        if(Peer.getFolder().getWantedChunks().isEmpty()){
            // Save the chunk ID so that the peer can check which chunks have been sent before sending a request chunk
            Peer.getFolder().getMdrChunkIds().add(newChunk.getFileID() + newChunk.getNumber());
            return;
        }

        if(Peer.getFolder().chunkAlreadyRestored(newChunk.getFileID(), newChunk.getNumber())){
            System.out.println("Chunk already restored");
            return;
        }

        Peer.getFolder().addRestoredChunk(newChunk);

        Peer.getFolder().wantedChunkReceived(newChunk.getFileID(), newChunk.getNumber());
        
        System.out.println("Restored Chunk.");
    }

    /**
     * Deletes all the chunks stored of a certain file
     * @param header - contains the id of the file whose chunks are to be deleted
     */
    private static void deleteChunk(String[] header){
        Peer.getFolder().deleteChunks(header[3].trim());
    }

    /**
     * When a chunk has a replication degree lower then what it is desired, send the chunk for other peers to save
     * @param header - contains the information about the chunk that had its degree lowered and the file it belongs
     */
    private static void restoreChunkReplication(String [] header){
        String fileID = header[3].trim();
        int chunkNr = Integer.parseInt(header[4].trim());

        // Since the file was removed from a peer, the peer needs to decrement the replication degree by 1
        Peer.getFolder().decreaseChunkReplication(fileID, chunkNr, header[2].trim());
        
        // If the peer doesn't have the chunk stored, he can't do anything
        if(!Peer.getFolder().getStoredChunks().containsKey(fileID) || Peer.getFolder().getFileChunk(fileID, chunkNr) == null){
            System.out.println("I don't have the desired chunk stored");
            return;
        }

        // Check if the chunk has the replication degree desired
        if(!Peer.getFolder().chunkStoredHasDesiredReplication(fileID, chunkNr)){
            // Generate the time that the peer will wait before sending the chunk
            Random random = new Random();
            int rndTime = random.nextInt(401);

            // Schedule the task with the time generate to send the chunk
            Peer.getThreadPool().schedule(new ReclaimSpaceTask(header), rndTime, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Deletes one chunk
     * @param chunkFileName - file name of the chunk to be deleted
     */
    private void deleteOneChunk(String chunkFileName) {
        File file = new File(Peer.getFolder().getPath() + "/" + chunkFileName);
        file.delete();
    }

    /**
     * Sends a message for each chunk to be deleted
     */
    private void verifyPeerFiles() {
        Peer.getThreadPool().execute(new VerifyDeleteTask());
    }

}
