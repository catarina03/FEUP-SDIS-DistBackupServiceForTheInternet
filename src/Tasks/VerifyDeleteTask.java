package Tasks;

import Peer.Peer;

public class VerifyDeleteTask implements Runnable{

    public VerifyDeleteTask(){}

    @Override
    public void run() {
        // For each chunk that was deleted, send a message
        for(int i = 0 ; i < Peer.getFolder().getDeletedChunks().size() ; i++){
            // Create message
            String msg = Peer.getProtocolVersion() + " DELETETHISCHUNK " + Peer.getFolder().getDeletedChunks().get(i) +  " \r\n\r\n";
            
            // Send message
            Peer.getControlChannel().sendMessage(msg.getBytes());
        }
    }
    
}
