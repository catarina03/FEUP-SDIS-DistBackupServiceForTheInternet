import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChordPeer  implements PeerClientTest{
    static String id, address, portNumber;
    private static Listener listener;
    private static ScheduledThreadPoolExecutor threadPool;
    static String[] cipherSuites;

    public ChordPeer(String idNumber, String addr, String port, String[] Suites){
        id = idNumber;
        address = addr;
        portNumber = port;
        threadPool = new ScheduledThreadPoolExecutor(100);
    }

    /**
     * 
     * @param args - Array containing the following information:
     * args[0] --> peer ID;
     * args[1] --> address;
     * args[2] --> port;
     * args[3] --> RMI name;
     */
    public static void main(String[] args) {
        // Create Thread Pool
        threadPool = new ScheduledThreadPoolExecutor(100);

        // Create Listener
        cipherSuites = new String[0];
        listener = new Listener(args[2], cipherSuites);
        threadPool.execute(listener);

        // Create the peer object to be saved in the RMI
        ChordPeer obj = new ChordPeer(args[0], args[1], args[2], cipherSuites);
        
        // Save the object in the rmi
        try {
            PeerClientTest stub = (PeerClientTest) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[3], stub);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String testCommunication(String port) throws RemoteException {
        SendRequestTask task = new SendRequestTask(address, port, "WHATS UP", new String[0]);

        task.run();

        return "done";
    }

}