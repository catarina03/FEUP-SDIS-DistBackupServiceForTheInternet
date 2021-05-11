package Tasks;

import Peer.Peer;

public class SendStoreMessageTask implements Runnable{
    private String[] header;


    public SendStoreMessageTask(String [] header){
        this.header = header;
    }

    @Override
    public void run() {
        // Create the stored message
        String storedConfirmationMessage = this.header[0] + " STORED " + Peer.getID() + " " + this.header[3] + " " + this.header[4] +  " \r\n\r\n";
        
        // Sent it via the MC channel
        Peer.getControlChannel().sendMessage(storedConfirmationMessage.getBytes());

        System.out.println("Chunk Stored.");
        
    }
    
}
