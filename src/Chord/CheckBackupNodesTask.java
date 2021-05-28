
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CheckBackupNodesTask implements Runnable{
    public CheckBackupNodesTask(){}

    @Override
    public void run(){
        System.out.println("Checking Backup nodes");
        Iterator<Map.Entry<FileData, ChordNode>> iter = ChordPeer.getFolder().getFileLocation().entrySet().iterator();
        String message = "CHECKCONNECTION" +  " \r\n\r\n";

        while(iter.hasNext()){
            Map.Entry<FileData, ChordNode> entry = iter.next();

            ChordNode node = entry.getValue();
            RequestSender checkConnectionRequest = new RequestSender(node.getAddress(),"" + node.getPortNumber(), message, ChordPeer.getChordLayer().getCipherSuites(), true);

            try {
                System.out.println("Sending message: to node " + node.getPortNumber() + " " + node.getAddress() );
                String response = new String(checkConnectionRequest.send());
                System.out.println("Node responded with :" + response);
            } catch (Exception e) {
                ChordPeer.getChordLayer().dealWithNodeFailure(node.getAddress(), node.getPortNumber());

                ChordPeer.getFolder().getFileLocation().remove(entry.getKey());

                ChordPeer.saveFile(entry.getKey());

                ChordPeer.getThreadPool().scheduleWithFixedDelay(new CheckBackupNodesTask(), 5, 20, TimeUnit.SECONDS);
            }
        }
            
    }
}
