package Channels;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * TCP class for the enhanced recover.
*/
public class TCPRestoreChannel implements Runnable{

    private int port;
    private ServerSocket serverSocket;
    private boolean on;

    public TCPRestoreChannel(int port){
        this.port = port;
        this.on = true;
    }

    public void stop() throws IOException {
        serverSocket.close();
        this.on = false;
    }

    /**
    * Run method for the runnable object. 
    * This function just handles connections to the tcp socket by creating a thread for each peer that is trying to connect.
    * the accept method is blocking meaning that we only create a new ChunkHandler when someone conects to the server Socket
    */
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(this.port);
            while (on){
                new ChunkHandler(serverSocket.accept()).start();
            }
                
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}
