import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLSocket;

public class ReceiveRequestTask implements Runnable{

    private SSLSocket socket;

    public ReceiveRequestTask(SSLSocket s){
        socket = s;
    }

    @Override
    public void run() {
        InputStream in;
        try {
            in = socket.getInputStream();
            byte[] requestReceived = new byte[256];

            in.read(requestReceived, 0, requestReceived.length);
            System.out.println("Request received: " + new String(requestReceived));
            
            Message request = new Message(requestReceived);

            String response = request.resolve();

            OutputStream out = socket.getOutputStream();
            out.write(response.getBytes(), 0, response.getBytes().length);
            
            System.out.println("Response sented: " + response);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
    
}
