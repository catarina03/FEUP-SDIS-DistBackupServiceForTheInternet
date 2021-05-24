 

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChordPeer implements PeerClientInterface{
    private static int id, idBits = 16;
    private static Listener listener;
    private static ScheduledThreadPoolExecutor threadPool;
    private static ChordLayer chordLayer;

    public ChordPeer(String idNumber, String addr, String port, String[] suites){
        chordLayer = new ChordLayer(addr, port, suites);

        String encondedID = chordLayer.sha1Encode(addr + port);
        id = Integer.parseInt(encondedID.substring(encondedID.length() - idBits/4), 16);
        System.out.println("Peer started with ID: " + id);

        threadPool = new ScheduledThreadPoolExecutor(100);
    }

    public static int getId() {
        return id;
    }

    public static ChordLayer getChordLayer(){
        return chordLayer;
    }

    public static Listener getListener() {
        return listener;
    }

    public static ScheduledThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    public static int getIdBits() {
        return idBits;
    }

    /**
     * 
     * @param args - Array containing the following information:
     * args[0] --> peer ID;
     * args[1] --> address;
     * args[2] --> port;
     * args[3] --> RMI name;
     * args[4] --> Known Chord Peer address;
     * args[5] --> Known Chord Peer port;
     * @throws IOException
     */
    public static void main(String[] args) throws Exception {
        // Create Thread Pool
        threadPool = new ScheduledThreadPoolExecutor(100);

        // Create Listener
        String[] cipherSuites = new String[0];
        listener = new Listener(args[2], cipherSuites);
        threadPool.execute(listener);

        // Create the peer object to be saved in the RMI
        ChordPeer obj = new ChordPeer(args[0], args[1], args[2], cipherSuites);

        if(args.length < 5){
            ChordLayer.createChord();
        }
        else{
            ChordLayer.joinChord(args[4], args[5]);
        }


        threadPool.scheduleWithFixedDelay(new StabilizeTask(), 5, 10, TimeUnit.SECONDS);
        threadPool.scheduleWithFixedDelay(new FixFingersTask(), 20, 20, TimeUnit.SECONDS);
        threadPool.scheduleWithFixedDelay(new CheckPredecessorTask(), 5, 20, TimeUnit.SECONDS);

        // // Save the object in the rmi
        // try {
        //     PeerClientTest stub = (PeerClientTest) UnicastRemoteObject.exportObject(obj, 0);
        //     Registry registry = LocateRegistry.getRegistry();
        //     registry.bind(args[3], stub);

        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

    }

    

    @Override
    public String backup(String filePath, int repDegree) throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

}