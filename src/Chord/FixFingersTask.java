 

public class FixFingersTask implements Runnable{

    public FixFingersTask(){        
    }

    @Override
    public void run() {

        // Iterate over the finger numbers and see who is the successor of each
        for(int i = 1; i <= ChordPeer.getIdBits(); i++){
            int fingerStart = (int) ((ChordPeer.getId() + Math.pow(2, i - 1)) % Math.pow(2, ChordPeer.getIdBits()));
            String[] successorResponse = ChordPeer.getChordLayer().findSuccessor(fingerStart).split(" ");

            ChordNode successor = new ChordNode(Integer.parseInt(successorResponse[1].trim()), successorResponse[2].trim(), successorResponse[3].trim());

            ChordPeer.getChordLayer().setFingerAtIndex((int) (Math.pow(2, i - 1)), successor);

        }
    }
    
}
