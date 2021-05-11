package Channels;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

import Peer.Peer;

public class ControlChannel implements Runnable{
    int  port;
    private InetAddress controlGroup;

    public ControlChannel(String controlAddress, int controlPort){
        try {
            this.controlGroup = InetAddress.getByName(controlAddress);
            this.port = controlPort;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Connects to the Multicast Socket and creates a thread to handle messages that it receives
     */
    @Override
    public void run() {
        try {
            MulticastSocket receiverControlSocket = new MulticastSocket(port);
            receiverControlSocket.joinGroup(controlGroup);

            while(true){

                // Buffer that will hold the message received
                byte[] receiveBuffer = new byte[65000];

                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                receiverControlSocket.receive(packet);
 
                byte[] packetData = Arrays.copyOf(receiveBuffer, packet.getLength());
                
                Peer.getThreadPool().execute(new MessageParser(packetData));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    /**
     * Sends a message throught the multicast channel
     * @param msg --> byte array holding the data to be transmited via the multicast channel
     */
    public void sendMessage(byte[] msg){
        try {
            DatagramSocket senderControlSocket = new DatagramSocket();

            DatagramPacket messagePacket = new DatagramPacket(msg, msg.length, controlGroup, port);

            senderControlSocket.send(messagePacket);

            senderControlSocket.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

