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
            PeerClientInterface stub = (PeerClientInterface) registry.lookup(remoteObjectName);
            String response = "";
            
            switch(args[1].trim()){
                // Start File Backup Protocol
                case "BACKUP":
                    response = stub.backup(args[2], Integer.valueOf(args[3].trim()));
                    System.out.println(response);
                    break;

                // Start Restore File Protocol
                case "RESTORE":
                    response = stub.restore(args[2]);
                    System.out.println(response);
                    break;

                // Start Delete File Protocol
                case "DELETE":
                    response = stub.delete(args[2]);
                    System.out.println(response);
                    break;

                // Start Space Reclaim Protocol
                case "RECLAIM":
                    response = stub.reclaim(args[2]);
                    System.out.println(response);
                    break;

                // Start Peer State Protocol
                case "STATE":
                    response = stub.state();
                    System.out.println(response);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

        
    }
}
