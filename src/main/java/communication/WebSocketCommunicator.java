package communication;

import controllers.MainWindowController;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

/**
 * The communicator handles the network traffic between all chat clients.
 * Messages are sent and received via the WebSocket.
 * 
 * @author Martin
 */
public class WebSocketCommunicator {
    //the server might be sleeping if not used in a while, first call can take up to a minute to get through.
    private final String WEB_SOCKET_ADDRESS = "https://work-58y8.onrender.com/";
    private Socket IOSocket;
    private MainWindowController _chat = null;

    public WebSocketCommunicator(MainWindowController chat) {
        this._chat = chat;
    }

    /**
     * Send the chat message to all clients.
     *
     * @param sender Name of the sender.
     * @param message Text message to send.
     * @throws java.lang.Exception
     */
    public void sendChatMessage(String sender, String message) throws Exception {

        try {
            this.IOSocket.emit("message", sender + ": " + message);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Start to listen for messages from other clients.
     */
    public void startListening() {
        try {
            this.listenForIncomingMessages();
        } catch (Exception e) {
            _chat.error(e);
        }
    }
    
    public void stopListening() throws Exception {
        IOSocket.disconnect();
        IOSocket.close();
    }

    /**
     * Listen for messages from other clients.
     */
    private void listenForIncomingMessages() throws Exception {
        try {
            IOSocket = IO.socket(WEB_SOCKET_ADDRESS);

            IOSocket.on("message", (final Object... args) -> {
                _chat.receiveMessage(args[0].toString());
            });
            IOSocket.connect();

        } catch (URISyntaxException e) {
            throw e;
        }
    }

}
