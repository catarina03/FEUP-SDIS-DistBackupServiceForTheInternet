 

import java.rmi.Remote;

public interface PeerClientTest extends Remote {
    String testCommunication(String port) throws Exception;

}