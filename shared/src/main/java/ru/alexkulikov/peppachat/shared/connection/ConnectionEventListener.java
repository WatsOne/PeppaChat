package ru.alexkulikov.peppachat.shared.connection;

public interface ConnectionEventListener {
    void onDataArrived(String message);
}
