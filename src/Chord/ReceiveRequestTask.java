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
            
            Message request = new Message(requestReceived);

            String response = request.resolve();

            if(response.equals("")){
                return;
            }

            OutputStream out = socket.getOutputStream();
            out.write(response.getBytes(), 0, response.getBytes().length);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
    
}
