package ru.alexkulikov.peppachat.emulator;

import com.google.gson.Gson;
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

    private BlockingQueue<Message> queue = new ArrayBlockingQueue<>(2);
    private Session session;
    private String botName;
    private ClientConnection connection;
    private boolean observe;

    public void start(String botName, boolean observe) {
        try {
            connection = ClientConnectionFabric.getClientConnection();
            connection.setup(HOST, PORT);
            connection.setEventListener(this);
            connection.setDataProducer(this);
            this.botName = botName;
            this.observe = observe;

            new Thread(() -> {
                try {
                    while (true) {
                        Thread.sleep(1000 * (rnd.nextInt(10) + 1));
                        queue.put(new Message(session, Command.MESSAGE, getRandomString(rnd.nextInt(20) + 1)));
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
    public Message getDataToSend() {
        return queue.poll();
    }

    @Override
    public void onDataArrived(Message message) {
        try {
            switch (message.getCommand()) {
                case ID:
                    if (observe) {
                        System.out.println("### Successfully connected, id: " + message.getSession().getId());
                        System.out.println("### Welcome to PeppaChat! Please enter your name:");
                    }
                    this.session = message.getSession();
                    queue.put(new Message(session, Command.REGISTER, botName));
                    connection.notifyToSend();
                    break;
                case REGISTER:
                    this.session = message.getSession();
                    if (observe) {
                        serverMessage(message.getText());
                    }
                    break;
                case SERVER_MESSAGE:
                    if (observe) {
                        serverMessage(message.getText());
                    }
                    break;
                case MESSAGE:
                default:
                    if (observe) {
                        System.out.println("(" + botName + ") " + message.getText());
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void serverMessage(String message) {
        System.out.println("### " + message);
    }

    @Override
    public void onDisconnect(Long sessionId) {
        System.out.println("### Server shutdown, try to restart the app");
    }
}
