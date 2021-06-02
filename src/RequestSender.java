import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class RequestSender{
    int portNumber;
    String address;
    byte[] requestData;
    String[] cipherSuites;
    Boolean wait;

    /**
     * Constructor for a string request
     */
    public RequestSender(String addr, String port, String request, String[] suites, Boolean waitResponse){
        requestData = request.getBytes();
        address = addr;
        portNumber = getPort(port);
        cipherSuites = suites;
        wait = waitResponse;
    }

    /**
     * Constructor for a byte array request
     */
    public RequestSender(String addr, String port, byte[] request, String[] suites, Boolean waitResponse){
        requestData = request;
        address = addr;
        portNumber = getPort(port);
        cipherSuites = suites;
        wait = waitResponse;
    }

    public byte[] send() throws Exception{
        setSystemProperties();
  
        // Create the client soccket
        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();  
        SSLSocket s;

        s = (SSLSocket) ssf.createSocket(address, portNumber);

        if(cipherSuites.length == 0){
            s.setSSLParameters( new SSLParameters(ssf.getDefaultCipherSuites()));
        }
        else{
            
            s.setSSLParameters(new SSLParameters(cipherSuites));
        }

        s.startHandshake();

        // Get the Output Stream
        OutputStream out = s.getOutputStream();

        // Write the Request
        out.write(requestData, 0, requestData.length);

        if(!wait){
            return null;
        }

        // Get the Input Stream
        InputStream in = s.getInputStream();
        
        // Buffer to save the request
        byte[] response = new byte[11000];

        // Read Response
        int bytesRead = in.read(response, 0, response.length);
    
        // Write what was read into a ByteArrayOutputStream so that the message is of the right size
        ByteArrayOutputStream baos = new ByteArrayOutputStream(11000);
        baos.write(response, 0, bytesRead);

        return baos.toByteArray();

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
