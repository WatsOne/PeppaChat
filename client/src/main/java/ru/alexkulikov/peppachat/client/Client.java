package ru.alexkulikov.peppachat.client;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import ru.alexkulikov.peppachat.client.connection.ClientConnection;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Session;
import ru.alexkulikov.peppachat.shared.connection.ConnectionException;
import ru.alexkulikov.peppachat.client.connection.ClientConnectionFabric;
import ru.alexkulikov.peppachat.client.connection.DataProducer;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;
import ru.alexkulikov.peppachat.shared.Message;

import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Client implements ConnectionEventListener, DataProducer {

    private static final int PORT = 10521;
    private static final String HOST = "localhost";

    private BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);
    private Session session;
    private Gson gson = new Gson();
    private ClientConnection connection;

    private void run() throws Exception {
        connection = ClientConnectionFabric.getClientConnection();
        connection.setup(HOST, PORT);
        connection.setEventListener(this);
        connection.setDataProducer(this);

        System.out.println("### Welcome to PeppaChat! Please enter your name:");

        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                try {
                    String line = scanner.nextLine();

                    if ("exit".equals(line)) {
                        connection.shutDown();
                        System.out.println("### Successfully disconnected");
                        continue;
                    }

                    try {
                        if (session == null) {
                            throw new ConnectionException("Session is null");
                        }

                        if (StringUtils.isEmpty(session.getName())) {
                            queue.put(buildMessage(line, Command.REGISTER));
                        } else {
                            queue.put(buildMessage(line, Command.MESSAGE));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    connection.notifyToSend();
                } catch (ConnectionException e) {
                    System.out.println("### Error: " + e.getMessage());
                    System.out.println("### Please, try to restart the application");
                }
            }
        }).start();
        connection.start();
    }

    public static void main(String[] args) throws Exception {
        new Client().run();
    }

    private String buildMessage(String text, Command command) {
        Message message = new Message();
        message.setText(text);
        message.setSession(session);
        message.setCommand(command);
        return gson.toJson(message);
    }

    @Override
    public String getDataToSend() {
        return queue.poll();
    }

    @Override
    public void onDataArrived(String messageStr) {
        try {
            Message message = gson.fromJson(messageStr, Message.class);
            switch (message.getCommand()) {
                case ID:
                    System.out.println("### Get id: " + message.getSession().getId());
                    this.session = message.getSession();
                    break;
                case REGISTER:
                    this.session = message.getSession();
                    System.out.println("### " + message.getText());
                    queue.put(buildMessage("", Command.HISTORY));
                    connection.notifyToSend();
                    break;
                case SERVER_MESSAGE:
                    System.out.println("### " + message.getText());
                    break;
                case HISTORY:
                    queue.put(buildMessage("", Command.HISTORY));
                    connection.notifyToSend();
                case MESSAGE:
                    System.out.println(message.getText());
                    break;
                default:
                    System.out.println(message.getText());
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
