import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CheckBackupNodesTask implements Runnable{
    public CheckBackupNodesTask(){}

    @Override
    public void run(){
        System.out.println("Checking Backup nodes");
        Iterator<Map.Entry<String, ArrayList<ChordNode>>> iter = ChordPeer.getFolder().getBackupNodes().entrySet().iterator();
        String message = "CHECKCONNECTION" +  " \r\n\r\n";

        while(iter.hasNext()){
            Map.Entry<String, ArrayList<ChordNode>> entry = iter.next();

            Iterator<ChordNode> nodes = entry.getValue().iterator();
            while(nodes.hasNext()){
                ChordNode node = nodes.next();
                RequestSender checkConnectionRequest = new RequestSender(node.getAddress(),"" + node.getPortNumber(), message, ChordPeer.getChordLayer().getCipherSuites(), true);

                try {
                    System.out.println("Sending message: to node " + node.getPortNumber() + " " + node.getAddress() );
                    String response = new String(checkConnectionRequest.send());
                    System.out.println("Node responded with :" + response);
                } catch (Exception e) {
                    ChordPeer.getChordLayer().dealWithNodeFailure(node.getAddress(), node.getPortNumber());
                    entry.getValue().remove(node);
                    ChordPeer.saveFile(ChordPeer.getFolder().getFilebyPath(entry.getKey()));

                    ChordPeer.getThreadPool().scheduleWithFixedDelay(new CheckBackupNodesTask(), 5, 20, TimeUnit.SECONDS);
                }
            }
            
        }
    }
}
