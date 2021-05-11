package Channels;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

import Peer.Peer;

public class BackupChannel implements Runnable{
    private InetAddress backupGroup;
    int port;

    public BackupChannel(String backupAddress, int backupPort){
        try {
            this.backupGroup = InetAddress.getByName(backupAddress);
            this.port = backupPort;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    /**
     * Connects to the Multicast Socket and creates a thread to handle messages that it receives
     */
    @Override
    public void run() {
        try {
            MulticastSocket receiverBackupSocket = new MulticastSocket(port);
            receiverBackupSocket.joinGroup(backupGroup);

            while(true){
                // Buffer that will hold the message received
                byte[] receiveBuffer = new byte[65000];

                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                receiverBackupSocket.receive(packet);
                
                // Get the data in an array of appropriate size
                byte[] packetData = Arrays.copyOf(receiveBuffer, packet.getLength());
                
                Peer.getThreadPool().execute(new MessageParser(packetData));

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    /**
     * Sends a message throught the multicast channel
     * @param msg --> byte array holding the data to be transmited via the multicast channel
     */
    public void sendMessage(byte[] msg){
        try {
            DatagramSocket senderBackupSocket = new DatagramSocket();
            DatagramPacket messagePacket = new DatagramPacket(msg, msg.length, backupGroup, port);
            
            senderBackupSocket.send(messagePacket);

            senderBackupSocket.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
