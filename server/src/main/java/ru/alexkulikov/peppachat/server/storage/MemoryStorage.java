package ru.alexkulikov.peppachat.server.storage;

import com.google.common.collect.EvictingQueue;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;

import java.util.*;
import java.util.stream.Collectors;

public class MemoryStorage implements Storage {

    private EvictingQueue<Message> messages;
    private Map<String, Session> sessions;

    public MemoryStorage() {
        messages = EvictingQueue.create(100);
        sessions = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            Message message = new Message();
            message.setText("Hello" + i);
            saveMessage(message);
        }
    }

    @Override
    public void saveSession(Session session) {
        sessions.put(session.getName(), session);
    }

    @Override
    public Session getSession(String name) {
        return sessions.get(name);
    }

    @Override
    public void saveMessage(Message message) {
        messages.add(message);
    }

    @Override
    public LinkedList<Message> getLastMessages() {
        return messages.stream().collect(Collectors.toCollection(LinkedList::new));
    }
}
