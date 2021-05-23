import java.io.IOException;

public class StabilizeTask implements Runnable{
    private ChordPeer peer;

    public StabilizeTask(ChordPeer peerToStabilize){
        peer = peerToStabilize;
    }

    @Override
    public void run() {
        System.out.println("Stabilizing....");
        String requestPredecessorMessage = "1.0 GETPREDECESSOR " + ChordPeer.getSuccessor().getId() + " \r\n\r\n";
        
        RequestSender requestPredecessor = new RequestSender(ChordPeer.getSuccessor().getAddress(), "" + ChordPeer.getSuccessor().getPortNumber(), requestPredecessorMessage, ChordPeer.getCipherSuites(), true);

        try {
            Message predecessorMessage = new Message(requestPredecessor.send());

            String notifyMessage = "1.0 NOTIFY " + ChordPeer.getId() + " " + ChordPeer.getAddress() + " " + ChordPeer.getPortNumber() + " \r\n\r\n";

            if(predecessorMessage.getHeader().length == 5){
                int predecessorID = Integer.parseInt(predecessorMessage.getHeader()[2].trim());

                if(ChordPeer.dealWithInterval(ChordPeer.getId(), false, ChordPeer.getSuccessor().getId(), false, predecessorID)){
                    ChordPeer.setSuccessor(new ChordNode(Integer.parseInt(predecessorMessage.getHeader()[2].trim()), predecessorMessage.getHeader()[3].trim(), predecessorMessage.getHeader()[4].trim()));
                }     
            }

            RequestSender requestNotify = new RequestSender(ChordPeer.getSuccessor().getAddress(), "" + ChordPeer.getSuccessor().getPortNumber(), notifyMessage, ChordPeer.getCipherSuites(), false);

            requestNotify.send();
           
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
