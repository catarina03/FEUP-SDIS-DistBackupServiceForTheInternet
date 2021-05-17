import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SSLClient {
    public static void main(String[] args) {
        setSystemProperties();
        String hostname = args[0];
        int port = getPort(args[1]);
        System.out.println("Host: " + hostname + " port: " + port);
        int startCyphor;
        String requestArgs = " ";

        
        if(args[2] == "REGISTER"){
            requestArgs = args[2] + " " + args[3] + " " + args[4];
            startCyphor = 5;
        }
        else{
            requestArgs = args[2] + " " + args[3];
            startCyphor = 4;
        }
        
        try {  

            SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();  
            SSLSocket s = (SSLSocket) ssf.createSocket(hostname, port);  

            if(startCyphor == args.length - 1){
                System.out.println("no cypher");
                s.setSSLParameters( new SSLParameters(ssf.getDefaultCipherSuites()));
            }
            else{

                String[] cyphorSuites = new String[args.length - startCyphor];

                for(int i = startCyphor; i < args.length; i++){
                    cyphorSuites[i - startCyphor] = args[i];
                }
                
                s.setSSLParameters(new SSLParameters(cyphorSuites));
            }

            s.startHandshake();

            OutputStream out = s.getOutputStream();

            out.write(requestArgs.getBytes());

            InputStream in = s.getInputStream();
            
            byte[] response = new byte[256];
            in.read(response, 0, in.available());
            //in.read(response);
            System.out.println("SSLClient: " + new String(response));

        }  
        catch( IOException e) {  
            System.out.println("Server - Failed to create SSLSocket");  
            e.getMessage();  
            return;  
        }       
    }

    private static void setSystemProperties(){
        //set the type of trust store
        System.setProperty("javax.net.ssl.trustStoreType","JKS");
        //set the password with which the truststore is encripted
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        //set the name of the trust store containing the server's public key and certificate           
        System.setProperty("javax.net.ssl.trustStore", "./truststore");
        //set the password with which the client keystore is encripted
        System.setProperty("javax.net.ssl.keyStorePassword","123456");
        //set the name of the keystore containing the client's private and public keys
        System.setProperty("javax.net.ssl.keyStore","./client.keys");
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
