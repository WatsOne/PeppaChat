package ru.alexkulikov.peppachat.client;

import ru.alexkulikov.peppachat.shared.ConnectionDataProducer;
import ru.alexkulikov.peppachat.shared.ConnectionEventListener;

import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Client implements ConnectionEventListener, ConnectionDataProducer {

    static final int PORT = 10521;
    static final String HOST = "localhost";

    BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);

    private void run() throws Exception {

        ClientConnection connection = ClientConnectionFabric.getClienConnection();
        connection.setup(HOST, PORT);
        connection.setListener(this);
        connection.setDataProducer(this);

        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine();
                if ("q".equals(line)) {
                    System.exit(0);
                }
                try {
                    queue.put(line);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    connection.notifyToSend();
                } catch (ClientConnectionException e) {

                }
            }
        }).start();

        connection.start();
    }

    public static void main(String[] args) throws Exception {
        new Client().run();
    }

    @Override
    public String getDataToSend() {
        return queue.poll();
    }

    @Override
    public void onDataArrived(String message) {
        System.out.println(message);
    }
}
