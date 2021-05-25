 

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
            System.out.println("Received message with length " + message.length);
            body = Arrays.copyOfRange(this.message, headerSize + 4, this.message.length);
            System.out.println("Read body with length " + body.length);
        }
        else{
            body = null;
        }

        //System.out.println("Message Parsed.");

    }

    public String resolve(){

        // Parse the operation to be executed
        switch (header[0].trim()) {
            case "FINDSUCCESSOR":
                    return ChordPeer.getChordLayer().findSuccessor(Integer.parseInt(header[1].trim()));
            case "SUCCESSOR":
                ChordNode successor = new ChordNode(Integer.parseInt(header[1].trim()), header[2].trim(), header[3].trim());
                ChordPeer.getChordLayer().setSuccessor(successor);
                return "";
            case "GETPREDECESSOR":
                if(ChordPeer.getChordLayer().getPredecessor() == null){
                    return "PREDECESSOR NULL"; 
                }
                return "PREDECESSOR " + ChordPeer.getChordLayer().getPredecessor().getId() + " " + ChordPeer.getChordLayer().getPredecessor().getAddress() + " " + ChordPeer.getChordLayer().getPredecessor().getPortNumber() + " " + " \r\n\r\n";

            case "NOTIFY":
                ChordPeer.getChordLayer().updatePredecessor(header[1].trim(), header[2].trim(), header[3].trim());
                return "";
            case "CHECKCONNECTION":
                return "ALIVE";
            case "SAVEFILE":
                saveFile();
                return "SAVED";

            case "PUTCHUNK":
                saveFileChunk();
                return "STORED";
            case "SAVECOMPLETED":
                locallySaveFile();
                return "LOCALLYSAVED";
            default:
                break;
            
        }

        return "";
        
    }
    private void saveFile(){
        System.out.println("SAVING FILE");
        
        boolean fileSuccessor  = ChordPeer.getChordLayer().dealWithInterval(ChordPeer.getChordLayer().getPredecessor().getId(), false, ChordPeer.getId(), false, Integer.parseInt(header[1].trim()));

        if(fileSuccessor && (ChordPeer.getFolder().getStorageUsed() + Integer.parseInt(header[4].trim()) < ChordPeer.getFolder().getStorageSize())){
            FileData storedFile = new FileData(header[1].trim(), Integer.parseInt(header[2].trim()), Integer.parseInt(header[3].trim()), header[5].trim());
            ChordPeer.getFolder().storeFile(storedFile.getID(), storedFile);
            System.out.println("FILE STORED Started.");
            System.out.println("File with name: " + ChordPeer.getFolder().getStoredFiles().get(storedFile.getID()).getFileName());
        }
        else{
            String[] successorResponse = ChordPeer.getChordLayer().findSuccessor(Integer.parseInt(header[1].trim())).split(" ");
            ChordNode successor = new ChordNode(Integer.parseInt(successorResponse[2].trim()), successorResponse[3].trim(), successorResponse[4].trim());
            
            RequestSender saveFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), message, ChordPeer.getChordLayer().getCipherSuites(), false);

            try {
                saveFileRequest.send();

                ChordPeer.getFolder().addFileLocation(header[1].trim(), successor);
            } catch (Exception e) {
                ChordPeer.getChordLayer().dealWithNodeFailure(successor.getAddress(), successor.getPortNumber());

                saveFile();
            }
        }
    }

    private void saveFileChunk(){
        System.out.println("PUTCHUNK RECEIVED");
        if(ChordPeer.getFolder().getFileLocation().containsKey(header[1].trim())){
            ChordNode fileSuccessor = ChordPeer.getFolder().getFileLocation().get(header[1].trim());
            
            RequestSender saveChunkRequest = new RequestSender(fileSuccessor.getAddress(), "" + fileSuccessor.getPortNumber(), message, ChordPeer.getChordLayer().getCipherSuites(), false);

            try {
                saveChunkRequest.send();
            } catch (Exception e) {
                ChordPeer.getChordLayer().dealWithNodeFailure(fileSuccessor.getAddress(), fileSuccessor.getPortNumber());

                saveFileChunk();
            }

            return;
        }
        System.out.println("CREATING CHUNK");
        System.out.println("Nr: " + header[2].trim() + " Size: " + body.length);
        Chunk chunkToStore = new Chunk(Integer.parseInt(header[2].trim()), body.length, body, header[1].trim());
        System.out.println("CREATED CHUNK");

        ChordPeer.getFolder().saveChunk(chunkToStore.getFileID(), chunkToStore);

        System.out.println("Save Chunk " + chunkToStore.getNumber());

    }

    private void locallySaveFile(){
        System.out.println("LOCALLY SAVING");
        if(ChordPeer.getFolder().getFileLocation().containsKey(header[1].trim())){
            ChordNode fileSuccessor = ChordPeer.getFolder().getFileLocation().get(header[1].trim());
            
            RequestSender saveCompletedRequest = new RequestSender(fileSuccessor.getAddress(), "" + fileSuccessor.getPortNumber(), message, ChordPeer.getChordLayer().getCipherSuites(), false);

            try {
                saveCompletedRequest.send();
            } catch (Exception e) {
                ChordPeer.getChordLayer().dealWithNodeFailure(fileSuccessor.getAddress(), fileSuccessor.getPortNumber());
            }

            return;
        }
        System.out.println("ACTUALLY LOCALLY SAVING FILE " + header[1].trim() + " or " + header[1]);
        // Get all the chunks of the file and sorted in order
        FileData file = ChordPeer.getFolder().getStoredFiles().get(header[1].trim());
        System.out.println("File exists");
        ArrayList<Chunk> fileChunks = file.getFileChunks();
        System.out.println("File has chunks " + fileChunks.size());
        Collections.sort(fileChunks);
        System.out.println("Saving file at " + ChordPeer.getFolder().getPath()+ "/" + file.getFileName());
        // Write the chunks into the file 
        try (FileOutputStream fos = new FileOutputStream(ChordPeer.getFolder().getPath()+"/" + file.getFileName())){
            for(Chunk c : fileChunks){
                System.out.println("Writing chunk with number: " + c.getNumber());
                fos.write(c.getData());
            }
            fos.close(); //There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
        }
        catch(Exception e){
            e.printStackTrace();
        }
        
    }
    
}
