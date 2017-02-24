package ru.alexkulikov.peppachat.server;


import com.google.gson.Gson;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageWorker implements Runnable {
    private final Queue<MessageEvent> sendQueue = new LinkedBlockingQueue<>();
    private final Gson gson = new Gson();

    public void submit(MessageEvent event) {
        sendQueue.add(event);
    }

    @Override
    public void run() {
        while (true) {
            try {
                MessageEvent message = sendQueue.poll();
                switch (message.getMode()) {
                    case RESPONSE:
                        Long id = message.getMessage().getSession().getId();
                        message.getConnection().send(id, gson.toJson(message.getMessage()));
                        break;
                    case BROADCAST:
                        message.getConnection().sendBroadcast(gson.toJson(message.getMessage()));
                        break;
                }

            } catch (Exception e) {

            }
        }
    }
}
