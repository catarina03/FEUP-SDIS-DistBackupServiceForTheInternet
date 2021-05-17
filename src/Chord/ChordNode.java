public class ChordNode {
    private String address;
    private int portNumber, id;

    public ChordNode(String addr, String port){
        address = addr;
        portNumber = Integer.parseInt(port);
        id = portNumber;
    }

    public String getAddress() {
        return address;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public int getId() {
        return id;
    }

    public void printInfo(){
        System.out.println("Chord Node with addr " + address + " at port " + portNumber);
    }
}
