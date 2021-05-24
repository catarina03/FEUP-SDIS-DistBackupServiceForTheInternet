import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class TestApp {

    private TestApp() {}

    /**
     * 
     * @param args:
     *  args[0] --> RMI remote object name
     *  args[1] --> Type of operation
     *  args[2] --> First Operation argument (optional)
     *  args[3] --> Second Operation argument (optional)
     */
    public static void main(String[] args) {

        String remoteObjectName = args[0];

        try {
            Registry registry = LocateRegistry.getRegistry();
            PeerClientTest stub = (PeerClientTest) registry.lookup(remoteObjectName);
            String response = stub.testCommunication(args[1]);
            
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

        
    }
}
