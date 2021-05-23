import java.io.IOException;
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

    public RequestSender(String addr, String port, String request, String[] suites, Boolean waitResponse){
        requestData = request.getBytes();
        address = addr;
        portNumber = getPort(port);
        cipherSuites = suites;
        wait = waitResponse;
    }

    public byte[] send() throws Exception{
        setSystemProperties();
  

        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();  
        SSLSocket s;

        s = (SSLSocket) ssf.createSocket(address, portNumber);

        if(cipherSuites.length == 0){
            System.out.println("Sender Using Default Cipher Suites");

            s.setSSLParameters( new SSLParameters(ssf.getDefaultCipherSuites()));
        }
        else{
            
            s.setSSLParameters(new SSLParameters(cipherSuites));
        }

        s.startHandshake();

        OutputStream out = s.getOutputStream();

        out.write(requestData, 0, requestData.length);
        System.out.println("Request sent to " + portNumber+ ": " + new String(requestData));

        if(!wait){
            System.out.println("Response not needed.");
            return null;
        }

        InputStream in = s.getInputStream();
        
        byte[] response = new byte[10000];
        in.read(response, 0, response.length);

        System.out.println("Response received: " + new String(response));

        return response;

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
