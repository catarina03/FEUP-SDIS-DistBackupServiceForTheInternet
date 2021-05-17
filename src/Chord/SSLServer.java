import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLParameters;

public class SSLServer {

    HashMap<String, Integer> dnsDatabase = new HashMap<String, Integer>();
    
    public SSLServer() {}
    
    public static void main(String[] args){
        SSLServer serverDNS = new SSLServer();

        int port = getPort(args[0]);
        setSystemProperties();

        try {  
            SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();  

            SSLServerSocket s = (SSLServerSocket) ssf.createServerSocket(port); 

            if(1 == args.length){
                System.out.println("no cypher");
                s.setSSLParameters( new SSLParameters(ssf.getDefaultCipherSuites()));
            }
            else{

                String[] cyphorSuites = new String[args.length - 1];

                for(int i = 1; i < args.length; i++){
                    cyphorSuites[i - 1] = args[i];
                }
                
                s.setSSLParameters(new SSLParameters(cyphorSuites));
            }

  
            
            s.setNeedClientAuth(true);


            while(true){
                SSLSocket c = (SSLSocket)s.accept();

                OutputStream out = c.getOutputStream();

                InputStream in = c.getInputStream();

                byte[] request = new byte[256];

                in.read(request);
                System.out.println("SSLServer: " + new String(request));
                
                String response = serverDNS.handleRequest((new String(request)).split(" "));

                out.write(response.getBytes());

                System.out.println("SSLServer: " + new String(response));
            }    

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
        //set the name of the trust store containing the client public key and certificate
        System.setProperty("javax.net.ssl.trustStore", "./truststore");
        //set the password with which the server keystore is encripted
        System.setProperty("javax.net.ssl.keyStorePassword","123456");
        //set the name of the keystore containing the server's private and public keys
        System.setProperty("javax.net.ssl.keyStore","./server.keys");
    }


    private String handleRequest(String[] requestArguments) {
        StringBuilder responseBuilder = new StringBuilder();

        resolveRequest(requestArguments, responseBuilder);

        return responseBuilder.toString();
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

    private int resolveRequest(String[] requestData, StringBuilder responseBuilder){
        int responseVal = 0;
        
        switch(requestData.length){
            case 2:
                responseVal = addEntry(requestData[0], requestData[1].trim());
                break;
            case 1:

                responseVal = getEntry(requestData[0].trim());
                break;
            default:
                System.out.println("Wrong request format.\n");
                return -1;
        }

        buildResponse(responseBuilder, requestData, responseVal);
        return 0;
    }

    private int addEntry(String dnsName, String dnsIP){

        if(this.dnsDatabase.containsKey(dnsName)){
            return -1;
        }

        try{
            Integer ip = Integer.valueOf(dnsIP.trim());

            this.dnsDatabase.put(dnsName, ip);

            System.out.println("Server: Register " + dnsName + " " + ip + "\n");
        } catch(NumberFormatException e){
            System.out.println("IP addres given was not a number.\n");

            return -1;
        }
        

        return this.dnsDatabase.size();
    }

    private int getEntry(String dnsName){
        System.out.println("DNSname: " + dnsName + "\n.");
        if(this.dnsDatabase.containsKey(dnsName)){
            System.out.println("Server: Lookup " + dnsName + "\n");
            return this.dnsDatabase.get(dnsName);
        }
        else{
            System.out.println("The DNS name given doens't exist in the database.\n");
            return -1;
        }
    
    }

    private void buildResponse(StringBuilder responseBuilder, String[] requestArgs, int responseVal){
        for(int i = 0; i < requestArgs.length; i++){
            responseBuilder.append(requestArgs[i] + " ");
        }
        
        if(responseVal == -1){
            responseBuilder.append(": ERROR");
        }
        else{
            responseBuilder.append(": " + responseVal);
        }
    }
}