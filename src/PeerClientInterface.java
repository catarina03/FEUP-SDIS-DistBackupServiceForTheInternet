import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerClientInterface extends Remote {
    String backup(String filePath, int repDegree) throws RemoteException;
    String restore(String filePath) throws RemoteException;
    String delete(String filePath) throws RemoteException;
    String reclaim(String size) throws RemoteException;
    String state() throws RemoteException;
}