package ru.alexkulikov.peppachat.client;

import com.google.gson.Gson;
import ru.alexkulikov.peppachat.client.connection.ClientConnection;
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

    BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);

    private void run() throws Exception {
        ClientConnection connection = ClientConnectionFabric.getClientConnection();
        connection.setup(HOST, PORT);
        connection.setEventListener(this);
        connection.setDataProducer(this);

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
                        queue.put(serialize(line));
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

    private String serialize(String text) {
        Message message = new Message();
        message.setText(text);
        return new Gson().toJson(message);
    }

    @Override
    public String getDataToSend() {
        return queue.poll();
    }

    @Override
    public void onDataArrived(String messageStr) {
        Message message = new Gson().fromJson(messageStr, Message.class);
        System.out.println(message.getText());
    }
}
