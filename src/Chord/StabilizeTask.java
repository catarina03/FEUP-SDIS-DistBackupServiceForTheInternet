import java.util.concurrent.TimeUnit;

public class StabilizeTask implements Runnable{

    public StabilizeTask(){}

    @Override
    public void run() {
        try{
        //System.out.println("Stabilizing....");
        String requestPredecessorMessage = "GETPREDECESSOR " + ChordPeer.getChordLayer().getSuccessor().getId() + " \r\n\r\n";
        
        RequestSender requestPredecessor = new RequestSender(ChordPeer.getChordLayer().getSuccessor().getAddress(), "" + ChordPeer.getChordLayer().getSuccessor().getPortNumber(), requestPredecessorMessage, ChordPeer.getChordLayer().getCipherSuites(), true);

        Message predecessorMessage = new Message(requestPredecessor.send());

        String notifyMessage = "NOTIFY " + ChordPeer.getId() + " " + ChordPeer.getChordLayer().getAddress() + " " + ChordPeer.getChordLayer().getPortNumber() + " \r\n\r\n";

        if(predecessorMessage.getHeader().length == 4){
            int predecessorID = Integer.parseInt(predecessorMessage.getHeader()[1].trim());

            if(ChordPeer.getChordLayer().dealWithInterval(ChordPeer.getId(), false, ChordPeer.getChordLayer().getSuccessor().getId(), false, predecessorID)){
                ChordPeer.getChordLayer().setSuccessor(new ChordNode(Integer.parseInt(predecessorMessage.getHeader()[1].trim()), predecessorMessage.getHeader()[2].trim(), predecessorMessage.getHeader()[3].trim()));
            }     
        }

        RequestSender requestNotify = new RequestSender(ChordPeer.getChordLayer().getSuccessor().getAddress(), "" + ChordPeer.getChordLayer().getSuccessor().getPortNumber(), notifyMessage, ChordPeer.getChordLayer().getCipherSuites(), false);

        requestNotify.send();

       // System.out.println("Stabilized.");
    }
    catch (Exception e) {
        System.out.println("Successor failed while trying to stabilize.");
        ChordPeer.getChordLayer().dealWithNodeFailure(ChordPeer.getChordLayer().getSuccessor().getAddress(), ChordPeer.getChordLayer().getSuccessor().getPortNumber());
        
        ChordPeer.getThreadPool().scheduleWithFixedDelay(new StabilizeTask(), 5, 20, TimeUnit.SECONDS);
    } 
           
    }
    
}
