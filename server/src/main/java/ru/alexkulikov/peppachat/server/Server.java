package ru.alexkulikov.peppachat.server;

import org.apache.commons.lang3.StringUtils;
import ru.alexkulikov.peppachat.server.connection.ServerConnection;
import ru.alexkulikov.peppachat.server.connection.ServerConnectionFabric;
import ru.alexkulikov.peppachat.server.storage.Storage;
import ru.alexkulikov.peppachat.server.storage.StorageFactory;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;

import java.io.IOException;

public class Server implements ConnectionEventListener {
    private static final int PORT = 10521;
    private static final String HOST = "localhost";

    private Storage storage;
    private ServerConnection connection;

    private MessageWorker worker;

    private void run(String host, int port) throws Exception {
        storage = StorageFactory.getStorage();

        worker = new MessageWorker(storage);
	    new Thread(worker).start();

        connection = ServerConnectionFabric.getServerConnection();
        connection.setup(host, port);
        connection.setEventListener(this);
        connection.start();
    }

    public static void main(String[] args) throws Exception {
	    String hostArg = args.length > 0 ? args[0] : "";
	    String portArg = args.length > 1 ? args[1] : "";

	    hostArg = StringUtils.isEmpty(hostArg) ? HOST : hostArg;
	    int port = StringUtils.isEmpty(portArg) ? PORT : Integer.valueOf(portArg);

        System.out.println("Server started on host: " + hostArg + " and port: " + port + "...");
        try {
	        new Server().run(hostArg, port);
        } catch (Exception e) {
	        e.printStackTrace();
        }
    }

    @Override
    public void onDataArrived(Message message) {
        System.out.println(message);

        switch (message.getCommand()) {
            case REGISTER:
                register(message);
                break;
            case MESSAGE:
                sendMessage(message);
                break;
            default:
                processCommand(message);
        }
    }

    @Override
    public void onDisconnect(Long sessionId) {
        Session session = storage.getSession(sessionId);
        storage.removeSession(sessionId);
        Message message = new Message(session, Command.SERVER_MESSAGE, "User \"" + session.getUserName() + "\" has left the chat.");

        submit(message, SendMode.BROADCAST_AUTHORIZED);
    }

    private void sendMessage(Message message) {
        storage.saveMessage(message);
        message.setText(message.getSession().getUserName() + ": " + message.getText());

        submit(message, SendMode.BROADCAST_AUTHORIZED);
    }

    private void register(Message message) {
        Session clientSession = message.getSession();
        if (userNameExists(message.getText())) {
        	submit(new Message(clientSession, Command.REGISTER, "User already register, please, enter a new name:"));
        } else {
            clientSession.setUserName(message.getText());
            storage.saveSession(message.getSession());

            submit(new Message(clientSession, Command.REGISTER, "Successfully register!"));
            submit(new Message(clientSession, Command.SERVER_MESSAGE, "User \""+ clientSession.getUserName() +"\" enter the chat!"), SendMode.BROADCAST_AUTHORIZED);

            String history = storage.getLastMessages();
            if (!StringUtils.isEmpty(history)) {
                submit(new Message(clientSession, Command.HISTORY, history));
            }
        }
    }

    private boolean userNameExists(String userName) {
        return storage.getAllSession().stream().anyMatch(s -> s.getUserName().equals(userName));
    }

    private void processCommand(Message message) {
        String commandResult;
        switch (message.getCommand()) {
            case ONLINE:
                commandResult = "Current online: " + String.valueOf(storage.getAllSession().size());
                break;
            default:
                commandResult = "Command not implemented";
        }

        submit(new Message(message.getSession(), Command.SERVER_MESSAGE, commandResult));
    }

    private void submit(Message message) {
    	submit(message, SendMode.RESPONSE);
    }

    private void submit(Message message, SendMode mode) {
    	worker.submit(new MessageEvent(connection, message, mode));
    }
}
