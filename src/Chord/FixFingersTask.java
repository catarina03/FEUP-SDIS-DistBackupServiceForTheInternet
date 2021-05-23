import java.io.IOException;

public class FixFingersTask implements Runnable{

    public FixFingersTask(){        
    }

    @Override
    public void run() {

        for(int i = 1; i <= 8; i++){
            try {
                int fingerStart = (int) ((ChordPeer.getId() + Math.pow(2, i - 1)) % Math.pow(2, 8));
                String[] successorResponse = ChordPeer.findSuccessor(fingerStart).split(" ");
    
                ChordNode successor = new ChordNode(Integer.parseInt(successorResponse[2].trim()), successorResponse[3].trim(), successorResponse[4].trim());
    
                ChordPeer.setFingerAtIndex((int) (Math.pow(2, i - 1)), successor);
            } catch (Exception e) {
                System.out.println("Node Failed While Trying to Search for a Successor");
            }
        }

        ChordPeer.printFingerTable();
    }
    
}
