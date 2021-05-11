package Channels;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

import Peer.Peer;

public class RecoveryChannel implements Runnable{
    int  port;
    private InetAddress recoveryGroup;

    public RecoveryChannel(String recoveryAddress, int recoveryPort){
        try {
            this.recoveryGroup = InetAddress.getByName(recoveryAddress);
            this.port = recoveryPort;
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
            MulticastSocket receiverRecoverySocket = new MulticastSocket(port);
            receiverRecoverySocket.joinGroup(recoveryGroup);

            while(true){

                // Buffer that will hold the message received
                byte[] receiveBuffer = new byte[65000];

                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                receiverRecoverySocket.receive(packet);

                byte[] packetData = Arrays.copyOf(receiveBuffer, packet.getLength());
                System.out.println("\n\nMDR received message with size: " + packetData.length);
                
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
            DatagramSocket senderRecoverySocket = new DatagramSocket();

            DatagramPacket messagePacket = new DatagramPacket(msg, msg.length, recoveryGroup, port);

            senderRecoverySocket.send(messagePacket);

            senderRecoverySocket.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
