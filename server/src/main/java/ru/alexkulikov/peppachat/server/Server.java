package ru.alexkulikov.peppachat.server;

import com.google.gson.Gson;
import ru.alexkulikov.peppachat.server.connection.ServerConnection;
import ru.alexkulikov.peppachat.server.connection.ServerConnectionFabric;
import ru.alexkulikov.peppachat.server.storage.Storage;
import ru.alexkulikov.peppachat.server.storage.StorageFactory;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Server implements ConnectionEventListener {
    private static final int PORT = 10521;
    private static final String HOST = "localhost";

    private Gson gson = new Gson();
    private Storage storage;
    private ServerConnection connection;

    private MessageWorker worker = new MessageWorker();
    private Map<Long, LinkedList<Message>> histories = new HashMap<>();

    private void run() throws Exception {
        new Thread(worker).start();
        storage = StorageFactory.getStorage();

        connection = ServerConnectionFabric.getServerConnection();
        connection.setup(HOST, PORT);
        connection.setEventListener(this);
        connection.start();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Server started...");
        new Server().run();
    }

    @Override
    public void onDataArrived(String messageStr) {
        try {
            Message message = gson.fromJson(messageStr, Message.class);
            System.out.println(message);
            switch (message.getCommand()) {
                case REGISTER:
                    register(message);
                    break;
                case HISTORY:
                    Long id = message.getSession().getId();
                    if (!histories.containsKey(id)) {
                        histories.put(id, storage.getLastMessages());
                    }

                    LinkedList<Message> queue = histories.get(id);
                    if (queue.size() == 0) {
                        System.out.println("DONE");
                    } else {
                        worker.submit(new MessageEvent(connection, buildMessage(message.getSession(), queue.poll().getText(), Command.HISTORY)));
                    }
                    break;
            }
        } catch (Exception e) {

        }
    }

    private Message buildMessage(Session session, String text, Command command) {
        Message message = new Message();
        message.setSession(session);
        message.setCommand(command);
        message.setText(text);
        return message;
    }

    private void register(Message message) throws IOException {
        Session serverSession = storage.getSession(message.getText());
        Session clientSession = message.getSession();
        if (serverSession == null) {
            clientSession.setName(message.getText());
            storage.saveSession(message.getSession());
//            for (int i = 1; i < 101; i++) {
                worker.submit(new MessageEvent(connection, buildMessage(clientSession, "Successfully register!", Command.REGISTER)));
//            }
//            for (int i = 1; i < 101; i++) {
                //connection.write(clientSession.getId(), buildMessage(clientSession, "Successfully register!", Command.REGISTER));
//            }
//            for (int i = 1; i < 101; i++) {
//                connection.write(clientSession, "x" + i, Command.SERVER_MESSAGE);
//            }
        } else{
//                connection.write(message.getSession(), "User already registered", Command.REGISTER);
        }
    }
}
