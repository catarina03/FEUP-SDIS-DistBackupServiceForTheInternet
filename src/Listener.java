 

import java.io.IOException;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class Listener implements Runnable{

    int portNumber;
    String[] cipherSuites;

    public Listener(String port, String[] suites){
        portNumber = getPort(port);
        cipherSuites = suites;
    }

    @Override
    public void run() {
        setSystemProperties();

        // Create the SSL Server Socket to receive the requests
        try {  
            SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();  

            SSLServerSocket s = (SSLServerSocket) ssf.createServerSocket(portNumber); 

            System.out.println("Listener Using Default Cipher Suites");
            s.setSSLParameters( new SSLParameters(ssf.getDefaultCipherSuites()));
        

  
            
            s.setNeedClientAuth(true);


            while(true){
                // Accept the Connection
                SSLSocket c = (SSLSocket)s.accept();
                
                // Deal with the request
                ChordPeer.getThreadPool().execute(new ReceiveRequestTask(c));
            }    

        }  
        catch( IOException e) {  
            System.out.println("Server - Failed to create SSLSocket");  
            e.printStackTrace();  
            return;  
        }
        
    }

    /**
     * Set the system properties requiered for the SSL connection
     */
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
    
    /**
     * Transform an string into a int
     */
    private static int getPort(String portString){
        Integer port = 0;
        try{
            port = Integer.valueOf(portString);

        } catch(NumberFormatException e){
            System.out.println("Port given was not a number.\n");
            System.exit(1);
        }

        return port;
    }
}
