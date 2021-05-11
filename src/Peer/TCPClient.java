package Peer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Each Peer that is sending chunk data over the tcp connection is a client.
 * Every time a enhanced Peer is sending chunks a new TCPClient object is created and the method sendMessage is called
 */
public class TCPClient {
    private Socket clientSocket;
    private OutputStream out;

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = clientSocket.getOutputStream();
    }

    /**
     * Method to send a byte array over a tcp socket (we dont need to read server response since that response is handled by the message parser)
     *@throws IOException
     */
    public void sendMessage(byte[] msg) throws IOException {
        out.write(msg);
    }

    /**
     * Method called whenever we want to close the client sided connection
     * @throws IOException
     */
    public void stopConnection() throws IOException {
        out.close();
        clientSocket.close();
    }
}
