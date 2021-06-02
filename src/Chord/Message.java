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
            body = Arrays.copyOfRange(this.message, headerSize + 4, this.message.length);
        }
        else{
            body = null;
        }

        //System.out.println("Message Parsed.");

    }

    public byte[] resolve(){

        // Parse the operation to be executed
        switch (header[0].trim()) {
            case "FINDSUCCESSOR":
                    return ChordPeer.getChordLayer().findSuccessor(Integer.parseInt(header[1].trim())).getBytes();
            case "SUCCESSOR":
                ChordNode successor = new ChordNode(Integer.parseInt(header[1].trim()), header[2].trim(), header[3].trim());
                ChordPeer.getChordLayer().setSuccessor(successor);
                return null;
            case "GETPREDECESSOR":
                String response;
                if(ChordPeer.getChordLayer().getPredecessor() == null){
                    response = "PREDECESSOR NULL"; 
                }
                else{
                    response = "PREDECESSOR " + ChordPeer.getChordLayer().getPredecessor().getId() + " " + ChordPeer.getChordLayer().getPredecessor().getAddress() + " " + ChordPeer.getChordLayer().getPredecessor().getPortNumber() + " " + " \r\n\r\n";
                }

                return response.getBytes();
            case "NOTIFY":
                ChordPeer.getChordLayer().updatePredecessor(header[1].trim(), header[2].trim(), header[3].trim());
                return null;
            case "CHECKCONNECTION":
                return "ALIVE".getBytes();
            case "SAVEFILE":
                return saveFile().getBytes();
            case "INITIATOR":
                int nodeID = Integer.parseInt(header[2].trim());
                ChordPeer.getFolder().getStoredFiles().get(header[1].trim()).setInitiatorPeer(new ChordNode(nodeID, header[3].trim(), header[4].trim()));
                return null;
            case "PUTCHUNK":
                saveFileChunk();
                return "STORED".getBytes();
            case "SAVECOMPLETED":
                locallySaveFile();
                return "LOCALLYSAVED".getBytes();
            case "GETCHUNK":
                return restoreChunk();
            case "RESTORECHUNK":
                saveRestoredChunk();
                return null;
            case "DELETE":
                deleteFile();
                return "DELETED".getBytes();
            case "REMOVED":
                dealWithRemovedFile();
                return null;
            default:
                break;
            
        }

        return null;
        
    }

    private String saveFile(){

        // If file already saved or is already trying to save a file, it can't save another file
        if(ChordPeer.getFolder().fileIsSaved(header[1].trim()) || ChordPeer.getSavingFile()){
            return "NOTSAVED";
        }

        ChordPeer.setSavingFile(true);

        // If it has space and the file is not stored, it can be stored
        if((ChordPeer.getFolder().getStorageUsed() + Integer.parseInt(header[4].trim()) < ChordPeer.getFolder().getStorageSize()) && !ChordPeer.getFolder().fileIsStored(header[5].trim())){
            FileData storedFile = new FileData(header[1].trim(), Integer.parseInt(header[2].trim()), Integer.parseInt(header[3].trim()), header[4].trim(), header[5].trim());
            ChordPeer.getFolder().storeFile(header[5].trim(), storedFile);
            
            ChordPeer.setSavingFile(false);
            System.out.println("Stored File");
            return "SAVED NULL";
        }

        // Find who can store the file
        ChordNode successor;
        Boolean triedPredecessor;

        if(ChordPeer.getChordLayer().getPredecessor() == null && ChordPeer.getChordLayer().getSuccessor() != null){
            successor = ChordPeer.getChordLayer().getSuccessor();
            triedPredecessor = false;
        }
        else{
            successor = ChordPeer.getChordLayer().getPredecessor();
            triedPredecessor = true;
        }

        
        RequestSender saveFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), message, ChordPeer.getChordLayer().getCipherSuites(), true);

        try {
            String response = new String(saveFileRequest.send());

            // If the predecessor can't, then try the successor
            if(response.equals("NOTSAVED") && triedPredecessor){
                successor = ChordPeer.getChordLayer().getSuccessor();

                saveFileRequest = new RequestSender(successor.getAddress(), "" + successor.getPortNumber(), message, ChordPeer.getChordLayer().getCipherSuites(), true);
            
                response = new String(saveFileRequest.send());

                if(response.equals("NOTSAVED")){
                    ChordPeer.setSavingFile(false);
                    return response;
                }
            }

        } catch (Exception e) {
            ChordPeer.getChordLayer().dealWithNodeFailure(successor.getAddress(), successor.getPortNumber());

            return saveFile();
            }


        ChordPeer.setSavingFile(false);
        System.out.println("SAVED " + successor.getId() + " " + successor.getAddress() + " " + successor.getPortNumber());
        // Respond with a message telling who stored the file
        return "SAVED " + successor.getId() + " " + successor.getAddress() + " " + successor.getPortNumber();
    }

    private void saveFileChunk(){
        Chunk chunkToStore = new Chunk(Integer.parseInt(header[2].trim()), body.length, body, header[1].trim());

        ChordPeer.getFolder().saveChunk(header[3].trim(), chunkToStore);
    }

    private void locallySaveFile(){
        // Get all the chunks of the file and sorted in order
        FileData file = ChordPeer.getFolder().getStoredFiles().get(header[1].trim());

        ArrayList<Chunk> fileChunks = file.getFileChunks();

        Collections.sort(fileChunks);

        // Write the chunks into the file 
        try (FileOutputStream fos = new FileOutputStream(ChordPeer.getFolder().getPath()+ "/" + file.getFileName())){
            for(Chunk c : fileChunks){
                fos.write(c.getData());
            }
            fos.close(); //There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
        }
        catch(Exception e){
            e.printStackTrace();
        }
        
    }

    private byte[] restoreChunk(){
        String filePath = header[1].trim();
        int chunkNr = Integer.parseInt(header[2].trim());

        Chunk chunkToSend = ChordPeer.getFolder().getStoredFile(filePath).getChunk(chunkNr);
       
        // Create the header and body of the message
        String header ="RESTORECHUNK " + chunkToSend.getFileID() + " " + chunkToSend.getNumber() + " \r\n\r\n";
        byte[] body = chunkToSend.getData();
        byte[] headerBytes = header.getBytes();
        
        // Join the header and the boddy into an array
        byte[] restoreChunkMessage = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, restoreChunkMessage, 0, headerBytes.length);
        System.arraycopy(body, 0, restoreChunkMessage, headerBytes.length, body.length);

        return restoreChunkMessage;

    }

    private void saveRestoredChunk(){
        Chunk chunkToRestore = new Chunk(Integer.parseInt(header[2].trim()), body.length, body, header[1].trim());

        ChordPeer.getFolder().restoreChunk(chunkToRestore);
    }

    private void deleteFile(){
        ChordPeer.getFolder().deleteStoredFile(header[1].trim());
    }

    private void dealWithRemovedFile() {
        FileData fileRemoved = ChordPeer.getFolder().getFilebyID(header[1].trim());
        ChordPeer.getFolder().deleteFileLocation(fileRemoved);
        ChordPeer.saveFile(fileRemoved);
    }
    
}
