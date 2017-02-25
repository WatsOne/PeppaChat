package ru.alexkulikov.peppachat.emulator;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import ru.alexkulikov.peppachat.client.connection.ClientConnection;
import ru.alexkulikov.peppachat.client.connection.ClientConnectionFabric;
import ru.alexkulikov.peppachat.client.connection.DataProducer;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Bot implements ConnectionEventListener, DataProducer {
    private static final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private Random rnd = new Random();

    private static final int PORT = 10521;
    private static final String HOST = "localhost";

    private BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);
    private Session session;
    private Gson gson = new Gson();
    private String botName;
    private ClientConnection connection;

    public void start(String botName) {
        try {
            connection = ClientConnectionFabric.getClientConnection();
            connection.setup(HOST, PORT);
            connection.setEventListener(this);
            connection.setDataProducer(this);
            this.botName = botName;

            new Thread(() -> {
                try {
                    while (true) {
                        Thread.sleep(1000 * rnd.nextInt(10) + 500);
                        queue.put(gson.toJson(new Message(session, Command.MESSAGE, getRandomString(rnd.nextInt(20)))));
                        connection.notifyToSend();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            connection.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRandomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public String getDataToSend() {
        return queue.poll();
    }

    @Override
    public void onDataArrived(Message message) {
        try {
            switch (message.getCommand()) {
                case ID:
                    System.out.println("### Successfully connected, id: " + message.getSession().getId());
                    System.out.println("### Welcome to PeppaChat! Please enter your name:");
                    this.session = message.getSession();
                    queue.put(gson.toJson(new Message(session, Command.REGISTER, botName)));
                    connection.notifyToSend();
                    break;
                case REGISTER:
                    this.session = message.getSession();
                    System.out.println("### " + message.getText());
                    break;
                case SERVER_MESSAGE:
                    System.out.println("### " + message.getText());
                    break;
                case MESSAGE:
                    System.out.println("(" + botName + ") " + message.getText());
                    break;
                default:
                    System.out.println(message.getText());
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public void onDisconnect(Long sessionId) {
        System.out.println("### Server shutdown, try to restart the app");
    }
}
