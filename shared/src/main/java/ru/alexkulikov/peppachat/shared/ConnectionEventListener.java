package ru.alexkulikov.peppachat.shared;

public interface ConnectionEventListener {
    void onDataArrived(String message);
}
