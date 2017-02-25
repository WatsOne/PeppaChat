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
                MessageEvent event = sendQueue.poll();
                switch (event.getMode()) {
                    case RESPONSE:
                        Long id = event.getMessage().getSession().getId();
                        event.getConnection().send(id, event.getMessage());
                        break;
                    case BROADCAST:
                        event.getConnection().sendBroadcast(event.getMessage());
                        break;
                }

            } catch (Exception e) {

            }
        }
    }
}
