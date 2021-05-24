 

import java.util.Arrays;

public class Message{
    private byte[] message;
    private String[] header;
    private byte[] body;

    public Message(byte[] messageBytes){
        this.message = messageBytes;
        parse();
    }

    public String[] getHeader() {
        return header;
    }

    public byte[] getBody() {
        return body;
    }

    /**
     * Parses the message received in the constructor.
     */
    private void parse(){
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
                    return ChordPeer.getChordLayer().findSuccessor(Integer.parseInt(header[2].trim()));
            case "SUCCESSOR":
                ChordNode successor = new ChordNode(Integer.parseInt(header[2].trim()), header[3].trim(), header[4].trim());
                ChordPeer.getChordLayer().setSuccessor(successor);
                return "";
            case "GETPREDECESSOR":
                if(ChordPeer.getChordLayer().getPredecessor() == null){
                    return "1.0 PREDECESSOR NULL"; 
                }
                return "1.0 PREDECESSOR " + ChordPeer.getChordLayer().getPredecessor().getId() + " " + ChordPeer.getChordLayer().getPredecessor().getAddress() + " " + ChordPeer.getChordLayer().getPredecessor().getPortNumber() + " " + " \r\n\r\n";

            case "NOTIFY":
                ChordPeer.getChordLayer().updatePredecessor(header[2].trim(), header[3].trim(), header[4].trim());
                return "";
            case "CHECKCONNECTION":
                return "1.0 ALIVE";
            default:
                break;
            
        }

        return "";
        
    }
    
}
