package ru.alexkulikov.peppachat.server.storage;

import com.google.common.collect.EvictingQueue;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;

import java.util.*;
import java.util.stream.Collectors;

public class MemoryStorage implements Storage {

    private EvictingQueue<Message> messages;
    private Map<Long, Session> sessions;

    public MemoryStorage() {
        messages = EvictingQueue.create(100);
        sessions = new HashMap<>();
    }

    @Override
    public void saveSession(Session session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public Session getSession(Long sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public List<Session> getAllSession() {
        return new ArrayList<>(sessions.values());
    }

    @Override
    public void removeSession(Long sessionId) {
        sessions.remove(sessionId);
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
