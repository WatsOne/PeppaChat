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
    }

    @Override
    public void saveSession(Session session) {
        sessions.put(session.getUserName(), session);
    }

    @Override
    public Session getSession(String userName) {
        return sessions.get(userName);
    }

    @Override
    public void saveMessage(Message message) {
        messages.add(message);
    }

    @Override
    public String getLastMessages() {
        return messages.stream().map(Message::getText).collect(Collectors.joining("\n"));
    }
}
