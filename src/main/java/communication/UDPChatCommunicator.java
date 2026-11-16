package communication;

import controllers.MainWindowController;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

/**
 * The communicator handles the network traffic between all chat clients.
 * Messages are sent and received via the UDP protocol which may lead to
 * messages being lost.
 * 
 * MAC USERS: 
 * https://blog.xnsio.com/2009/11/08/enabling-multicast-on-your-macos-unix/
 * IF you HAVE PROBLEMS receiving UDP MULTICAST MESSAGES ON MACOS
 * See the above link for instructions on how to enable multicast on MacOS
 * TL;DR: Usaly You need to enable the MULTICAST IP we use (or change it to another one).
 * put the following line in a terminal (change en0 to your network interface if needed):
 * 
 * sudo route -nv add -net 228.28.28.28 -interface en0
 *
 * @author Martin
 */
public class UDPChatCommunicator implements Runnable {

    private final int DATAGRAM_LENGTH = 100;
    private final int PORT = 6789;
    private final String MULTICAST_ADDRESS = "228.28.28.28";

    private InetSocketAddress _group;
    private NetworkInterface _netIf;
    private MainWindowController _chat = null;
    private MulticastSocket _socket = null;

    /**
     * Create a chat communicator that communicates over UDP.
     *
     * @param chat The UI that want to receive incoming messages.
     */
    public UDPChatCommunicator(MainWindowController chat) {
        forceIp4();
        _chat = chat;
    }

    /*
     * force java to use IPv4 so we do not get a problem when using IPv4 multicast
     * address.
     *
     * This might not work !! set the below row as an JVM option when starting your application
       if you're having problems:
       -Djava.net.preferIPv4Stack=true
     */
    private void forceIp4() {

        System.setProperty("java.net.preferIPv4Stack", "true");

        String propertyValue = System.getProperty("java.net.preferIPv4Stack");
        if (propertyValue != null) {
            System.out.println("java.net.preferIPv4Stack = " + propertyValue);
        } else {
            System.out.println("java.net.preferIPv4Stack was null");
            System.out.println("If you see null AND your UDP messages dont get through in the application");
            System.out.println("set '-Djava.net.preferIPv4Stack=true' as a JVM argument when starting your application");
        }
    }

    /**
     * Send the chat message to all clients.
     *
     * @param sender  Name of the sender.
     * @param message Text message to send.
     * @throws IOException If there is an IO error.
     */
    public void sendChatMessage(String sender, String message) throws Exception {

        try (DatagramSocket socket = new DatagramSocket()) {
            String toSend = sender + ": " + message;
            byte[] b = toSend.getBytes();

            DatagramPacket datagram = new DatagramPacket(b, b.length, InetAddress.getByName(MULTICAST_ADDRESS), PORT);

            socket.send(datagram);
            socket.disconnect();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Start to listen for messages from other clients.
     */
    public void startListening() {
        new Thread(this).start();
    }

    @SuppressWarnings("finally")
    private String retriveNetworkInterface() {
        String answer = "bge0"; // default value windows

        try {
            final Enumeration<NetworkInterface> netifs = NetworkInterface.getNetworkInterfaces();
            InetAddress myAddr = InetAddress.getLocalHost();

            while (netifs.hasMoreElements()) {
                NetworkInterface networkInterface = netifs.nextElement();
                Enumeration<InetAddress> inAddrs = networkInterface.getInetAddresses();
                while (inAddrs.hasMoreElements()) {
                    InetAddress inAddr = inAddrs.nextElement();
                    if (inAddr.equals(myAddr)) {
                        System.out.println("Using network interface: " + networkInterface.getName());
                        answer = networkInterface.getName();
                    }
                }
            }
        } catch (SocketException | UnknownHostException e) {
            _chat.error(e);
        } finally {
            return answer;
        }
    }

    /**
     * Listen for messages from other clients.
     *
     * @throws Exception If there is an IO error.
     */
    private void listenForIncomingMessages() throws Exception {
        byte[] b = new byte[DATAGRAM_LENGTH];
        DatagramPacket datagram = new DatagramPacket(b, b.length);

        if (_socket == null) {
            _group = new InetSocketAddress(InetAddress.getByName(MULTICAST_ADDRESS), PORT);
            _netIf = NetworkInterface.getByName(retriveNetworkInterface());
            _socket = new MulticastSocket(PORT);
        }

        _socket.joinGroup(_group, _netIf);

        while (true) {
            _socket.receive(datagram);
            String message = new String(datagram.getData());
            message = message.substring(0, datagram.getLength());

            _chat.receiveMessage(message); // <<<<<<------------ send the message to the UI

            datagram.setLength(b.length);
        }

    }

    public void stopListening() throws Exception {
        _socket.leaveGroup(_group, _netIf);
    }

    @Override
    public void run() {
        try {
            this.listenForIncomingMessages();
        } catch (Exception e) {
            _chat.error(e);
        }
    }
}
