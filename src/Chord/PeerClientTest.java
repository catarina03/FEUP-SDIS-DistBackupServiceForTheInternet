import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerClientTest extends Remote {
    String testCommunication(String port) throws Exception;

}