package ru.alexkulikov.peppachat.server;


import com.google.gson.Gson;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageWorker implements Runnable {
    private final Queue<MessageEvent> queue = new LinkedBlockingQueue<>();
    private final Gson gson = new Gson();

    public void submit(MessageEvent event) {
        queue.add(event);
    }

    @Override
    public void run() {
        while (true) {
            try {
                MessageEvent message = queue.poll();
                Long id = message.getMessage().getSession().getId();
                message.getConnection().write(id, gson.toJson(message.getMessage()));
            } catch (Exception e) {

            }
        }
    }
}
