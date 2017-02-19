package ru.alexkulikov.peppachat.server;

import java.io.IOException;

public class Server {

    public static void main(String[] args) throws IOException {
        try {
            ServerWorker worker = new ServerWorker();
            new Thread(worker).start();
            System.out.println("Server started... ");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
