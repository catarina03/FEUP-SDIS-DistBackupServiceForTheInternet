import java.util.concurrent.TimeUnit;

public class StabilizeTask implements Runnable{

    public StabilizeTask(){}

    @Override
    public void run() {
        try{
            System.out.println("Stabilizing....");
            
            // Ask who is the predecessor of this node successor      
            String requestPredecessorMessage = "GETPREDECESSOR " + ChordPeer.getChordLayer().getSuccessor().getId() + " \r\n\r\n";
            
            RequestSender requestPredecessor = new RequestSender(ChordPeer.getChordLayer().getSuccessor().getAddress(), "" + ChordPeer.getChordLayer().getSuccessor().getPortNumber(), requestPredecessorMessage, ChordPeer.getChordLayer().getCipherSuites(), true);

            Message predecessorMessage = new Message(requestPredecessor.send());

            String notifyMessage = "NOTIFY " + ChordPeer.getId() + " " + ChordPeer.getChordLayer().getAddress() + " " + ChordPeer.getChordLayer().getPortNumber() + " \r\n\r\n";

            // If the sucessor's message has 4 then that means that he has a predecessor
            if(predecessorMessage.getHeader().length == 4){
                int predecessorID = Integer.parseInt(predecessorMessage.getHeader()[1].trim());

                // If the predecessor is of our sucessor has na id bigger then the current node, the predecessor is this node's successor
                if(ChordPeer.getChordLayer().dealWithInterval(ChordPeer.getId(), false, ChordPeer.getChordLayer().getSuccessor().getId(), false, predecessorID)){
                    ChordPeer.getChordLayer().setSuccessor(new ChordNode(Integer.parseInt(predecessorMessage.getHeader()[1].trim()), predecessorMessage.getHeader()[2].trim(), predecessorMessage.getHeader()[3].trim()));
                }     
            }

            // Notify the node's succcessor that this node is his predecessor
            RequestSender requestNotify = new RequestSender(ChordPeer.getChordLayer().getSuccessor().getAddress(), "" + ChordPeer.getChordLayer().getSuccessor().getPortNumber(), notifyMessage, ChordPeer.getChordLayer().getCipherSuites(), false);

            requestNotify.send();

            System.out.println("Stabilized.");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Successor failed while trying to stabilize.");
            ChordPeer.getChordLayer().dealWithNodeFailure(ChordPeer.getChordLayer().getSuccessor().getAddress(), ChordPeer.getChordLayer().getSuccessor().getPortNumber());
            
            ChordPeer.getThreadPool().scheduleWithFixedDelay(new StabilizeTask(), 5, 20, TimeUnit.SECONDS);
        } 
            
        }
    
}
