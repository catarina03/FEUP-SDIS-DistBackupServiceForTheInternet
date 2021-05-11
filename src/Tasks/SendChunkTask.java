package Tasks;


import java.io.IOException;

import Peer.Chunk;
import Peer.Peer;
import Peer.TCPClient;

public class SendChunkTask implements Runnable{

    private String[] header;

    public SendChunkTask(String[] header){
        this.header = header;
    }



    @Override
    public void run() {
        // Get the chunk to be sent
        Chunk chunkToSend = Peer.getFolder().getFileChunk(this.header[3].trim(), Integer.parseInt(this.header[4].trim()));
        
        // Check if the chunk was already sent by other peers
        if (Peer.getFolder().getMdrChunkIds().contains(chunkToSend.getFileID() + chunkToSend.getNumber())){
            Peer.getFolder().getMdrChunkIds().remove(chunkToSend.getFileID() + chunkToSend.getNumber());
            return;
        }

        // Create header and body of the message
        String header = Peer.getProtocolVersion() + " CHUNK " + Peer.getID() + " " + chunkToSend.getFileID() + " " + chunkToSend.getNumber() + " \r\n\r\n";
        byte[] headerBytes = header.getBytes();
        byte[] body = chunkToSend.getData();

        // Join the header and the boddy into an array
        byte[] message = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, message, 0, headerBytes.length);
        System.arraycopy(body, 0, message, headerBytes.length, body.length);

        // For the version 2.0 the message in sent via TCP, otherwise it is sent via a multicast channel
        if(this.header[0].equals("2.0") && Peer.getProtocolVersion().equals("2.0")){
            TCPClient client = new TCPClient();
            try {
                client.startConnection("localhost", 5555);
                client.sendMessage(message);
                client.stopConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Peer.getRecoveryChannel().sendMessage(message);
        }

        
        System.out.println("Header Sent: " + header);
    }
    
}
