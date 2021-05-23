import java.io.IOException;

public class FixFingersTask implements Runnable{

    public FixFingersTask(){        
    }

    @Override
    public void run() {
        int next = ChordPeer.getNextFinger() + 1;

        if(next > 8){
            next = 1;
            System.out.println("FINGER TABLE.");
            for(int i = 1; i <= 8; i++){
                System.out.println("Finger nr " + (ChordPeer.getId() + Math.pow(2, i - 1)) % Math.pow(2, 8) + " : ");
                ChordPeer.getFingerTable().get(i).printInfo();
            }
        }

        try {
            String[] successorResponse = ChordPeer.findSuccessor((int) ((ChordPeer.getId() + Math.pow(2, next - 1)) % Math.pow(2, 8))).split(" ");

            ChordNode successor = new ChordNode(Integer.parseInt(successorResponse[2].trim()), successorResponse[3].trim(), successorResponse[4].trim());

            if(ChordPeer.getFingerTable().size() > next){
                ChordPeer.setFingerAtIndex(next, successor);
            }
            else{
                ChordPeer.getFingerTable().add(successor);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        ChordPeer.setNextFinger(next);
    }
    
}
