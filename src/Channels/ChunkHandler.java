package Channels;


import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;

import Peer.Peer;
/**
* Thread that takes care of reading the contents send over the tcp socket
*/
class ChunkHandler extends Thread {
    private Socket clientSocket;

    public ChunkHandler(Socket socket) {
        this.clientSocket = socket;
    }


    /**
    * Run method for the thread object. 
    * This method tries to read from the input stream of the client socket, meaning its reading the contents of whatever was sent over the tcp connection
    * the while method exists to prevent some weird behavior where sometimes the read method would not read the entire buffer right away
    * We then get the byte array size and do an arraycopy to message byte array and then let our messageParser handle the message.
    */
    public void run(){
        try {
            InputStream in = clientSocket.getInputStream();
            DataInputStream dis = new DataInputStream(in);

            byte[] bytes = new byte[65000];

            int numBytes = 0;
            int tempB = 0;

            while ((tempB = dis.read(bytes)) > 0){
                numBytes += tempB;
                
            }
            
            byte[] message = new byte[numBytes];

            System.arraycopy(bytes, 0, message, 0, numBytes);

            Peer.getThreadPool().execute(new MessageParser(message));

            in.close();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
