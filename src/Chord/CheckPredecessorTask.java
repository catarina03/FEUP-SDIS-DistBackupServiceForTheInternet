 

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class CheckPredecessorTask implements Runnable{

    public CheckPredecessorTask(){}

    public void run(){
        setSystemProperties();
        
        // If the predecessor is null, there is no need to check if he is alive
        if(ChordPeer.getChordLayer().getPredecessor() == null){
            return;
        }

        // Start the connection
        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();  
        SSLSocket s;
        try {
            s = (SSLSocket) ssf.createSocket(ChordPeer.getChordLayer().getPredecessor().getAddress(), ChordPeer.getChordLayer().getPredecessor().getPortNumber());

            if(ChordPeer.getChordLayer().getCipherSuites().length == 0){
                s.setSSLParameters( new SSLParameters(ssf.getDefaultCipherSuites()));
            }
            else{
                
                s.setSSLParameters(new SSLParameters(ChordPeer.getChordLayer().getCipherSuites()));
            }
    
            s.startHandshake();
            
            // Send the checkconnection message
            OutputStream out = s.getOutputStream();
            String checkPredecessor = "CHECKCONNECTION" +  " \r\n\r\n";
            out.write(checkPredecessor.getBytes(), 0, checkPredecessor.getBytes().length);
            
            // Check if the peer responds
            InputStream in = s.getInputStream();
            
            byte[] response = new byte[10000];
            in.read(response, 0, response.length);
    
        } catch (Exception e) {
            ChordPeer.getChordLayer().setPredecessor(null);
            System.out.println("Predecessor Offline");

            ChordPeer.getThreadPool().scheduleWithFixedDelay(new CheckPredecessorTask(), 5, 20, TimeUnit.SECONDS);
        } 
    }

    private static void setSystemProperties(){
        //set the type of trust store
        System.setProperty("javax.net.ssl.trustStoreType","JKS");
        //set the password with which the truststore is encripted
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        //set the name of the trust store containing the client public key and certificate
        System.setProperty("javax.net.ssl.trustStore", "./truststore");
        //set the password with which the server keystore is encripted
        System.setProperty("javax.net.ssl.keyStorePassword","123456");
        //set the name of the keystore containing the server's private and public keys
        System.setProperty("javax.net.ssl.keyStore","./server.keys");
    }
    
}
