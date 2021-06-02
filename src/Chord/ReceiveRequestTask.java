 

import java.io.ByteArrayOutputStream;
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
            // Get the input stream from the socket
            in = socket.getInputStream();

            // Buffer to save the request
            byte[] requestReceived = new byte[11000];

            // Read request
            int bytesRead = in.read(requestReceived, 0, requestReceived.length);

            // Write what was read into a ByteArrayOutputStream so that the message is of the right size
            ByteArrayOutputStream baos = new ByteArrayOutputStream(11000);
            baos.write(requestReceived, 0, bytesRead);
            
            // Resolve the request
            Message request = new Message(baos.toByteArray());

            byte[] response = request.resolve();

            if(response == null){
                return;
            }

            // Sent the message
            OutputStream out = socket.getOutputStream();
            out.write(response, 0, response.length);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
    
}
