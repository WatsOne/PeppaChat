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

    private void run() throws Exception {
        storage = StorageFactory.getStorage();

        worker = new MessageWorker(storage);
	    new Thread(worker).start();

        connection = ServerConnectionFabric.getServerConnection();
        connection.setup(HOST, PORT);
        connection.setEventListener(this);
        connection.start();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Server started...");
        try {
	        new Server().run();
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
        worker.submit(new MessageEvent(connection, message, SendMode.BROADCAST_AUTHORIZED));
    }

    private void sendMessage(Message message) {
        storage.saveMessage(message);
        message.setText(message.getSession().getUserName() + ": " + message.getText());
        worker.submit(new MessageEvent(connection, message, SendMode.BROADCAST_AUTHORIZED));
    }

    private void register(Message message) {
        Session clientSession = message.getSession();
        if (userNameExists(message.getText())) {
            worker.submit(new MessageEvent(connection, new Message(clientSession, Command.REGISTER, "User already register, please, enter a new name:")));
        } else {
            clientSession.setUserName(message.getText());
            storage.saveSession(message.getSession());
            worker.submit(new MessageEvent(connection, new Message(clientSession, Command.REGISTER, "Successfully register!")));
            worker.submit(new MessageEvent(connection, new Message(clientSession, Command.SERVER_MESSAGE, "User \""+ clientSession.getUserName() +"\" enter the chat!"), SendMode.BROADCAST_AUTHORIZED));
            String history = storage.getLastMessages();
            if (!StringUtils.isEmpty(history)) {
                worker.submit(new MessageEvent(connection, new Message(clientSession, Command.MESSAGE, history)));
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

        worker.submit(new MessageEvent(connection, new Message(message.getSession(), Command.SERVER_MESSAGE, commandResult)));
    }
}
