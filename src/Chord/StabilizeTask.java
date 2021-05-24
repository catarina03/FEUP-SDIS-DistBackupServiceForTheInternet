

public class StabilizeTask implements Runnable{

    public StabilizeTask(){}

    @Override
    public void run() {
        try{
        System.out.println("Stabilizing....");
        String requestPredecessorMessage = "1.0 GETPREDECESSOR " + ChordPeer.getChordLayer().getSuccessor().getId() + " \r\n\r\n";
        
        RequestSender requestPredecessor = new RequestSender(ChordPeer.getChordLayer().getSuccessor().getAddress(), "" + ChordPeer.getChordLayer().getSuccessor().getPortNumber(), requestPredecessorMessage, ChordPeer.getChordLayer().getCipherSuites(), true);

        Message predecessorMessage = new Message(requestPredecessor.send());

        String notifyMessage = "1.0 NOTIFY " + ChordPeer.getId() + " " + ChordPeer.getChordLayer().getAddress() + " " + ChordPeer.getChordLayer().getPortNumber() + " \r\n\r\n";

        if(predecessorMessage.getHeader().length == 5){
            int predecessorID = Integer.parseInt(predecessorMessage.getHeader()[2].trim());

            if(ChordPeer.getChordLayer().dealWithInterval(ChordPeer.getId(), false, ChordPeer.getChordLayer().getSuccessor().getId(), false, predecessorID)){
                ChordPeer.getChordLayer().setSuccessor(new ChordNode(Integer.parseInt(predecessorMessage.getHeader()[2].trim()), predecessorMessage.getHeader()[3].trim(), predecessorMessage.getHeader()[4].trim()));
            }     
        }

        RequestSender requestNotify = new RequestSender(ChordPeer.getChordLayer().getSuccessor().getAddress(), "" + ChordPeer.getChordLayer().getSuccessor().getPortNumber(), notifyMessage, ChordPeer.getChordLayer().getCipherSuites(), false);

        requestNotify.send();

        System.out.println("Stabilized.");
    }
    catch (Exception e) {
        System.out.println("Successor failed while trying to stabilize.");
        ChordPeer.getChordLayer().dealWithNodeFailure(ChordPeer.getChordLayer().getSuccessor().getAddress(), ChordPeer.getChordLayer().getSuccessor().getPortNumber());
        run();
    } 
           
    }
    
}
