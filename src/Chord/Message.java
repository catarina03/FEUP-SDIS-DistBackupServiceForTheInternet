import java.io.IOException;
import java.util.Arrays;

public class Message{
    private byte[] message;
    private String[] header;
    private byte[] body;

    public Message(byte[] messageBytes){
        this.message = messageBytes;
        parse();
    }

    /**
     * Parses the message received in the constructor.
     */
    public void parse(){
        int headerSize;

        for(headerSize = 0; headerSize < this.message.length - 4; headerSize++){
            // Check for the <CRLF><CRLF> that always sperarates the header from the body
            if(this.message[headerSize] == 0xD && this.message[headerSize+1] == 0xA && this.message[headerSize+2] == 0xD && this.message[headerSize+3] == 0xA){
                break;
            }
        }

        byte[] headerBytes = Arrays.copyOfRange(this.message, 0, headerSize);

        // Transform header into a String array so it is easier to access
        header = (new String(headerBytes)).split(" "); 

        // Body only exists if the header isn't the same size as the message minus the <CRLF><CRLF>
        if(headerSize != this.message.length - 4){
            body = Arrays.copyOfRange(this.message, headerSize + 4, this.message.length);
        }
        else{
            body = null;
        }

        System.out.println("Message Parsed.");

    }

    public String resolve(){
        // Parse the operation to be executed
        switch (header[1].trim()) {
            case "FINDSUCCESSOR":
                try {
                    return ChordPeer.findSucessor(Integer.parseInt(header[2].trim()));
                } catch (NumberFormatException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            case "SUCCESSOR":
                ChordNode successor = new ChordNode(header[4].trim(), header[3].trim());
                ChordPeer.setSuccessor(successor);
                return "";
            default:
                break;
            
        }

        return "";
        
    }
    
}
