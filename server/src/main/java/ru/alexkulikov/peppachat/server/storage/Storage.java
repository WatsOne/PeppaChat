package ru.alexkulikov.peppachat.server.storage;

import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;

import java.util.Queue;

public interface Storage {
    void saveSession(Session session);

    Session getSession(String name);

    void saveMessage(Message message);

    Queue<Message> getLastMessages();

}
