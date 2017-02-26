package ru.alexkulikov.peppachat.shared.connection;

import ru.alexkulikov.peppachat.shared.Message;

public interface ConnectionEventListener {

    void onDataArrived(Message message);

    void onDisconnect(Long sessionId);
}
