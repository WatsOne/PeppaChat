package ru.alexkulikov.peppachat.server.storage;

import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public interface Storage {
	void saveSession(Session session);

	Session getSession(Long sessionId);

	List<Session> getAllSession();

	void removeSession(Long sessionId);

	void saveMessage(Message message);

	String getLastMessages();
}
