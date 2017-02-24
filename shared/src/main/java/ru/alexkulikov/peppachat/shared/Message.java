package ru.alexkulikov.peppachat.shared;

import java.io.Serializable;

public class Message implements Serializable {

    private Session session;
    private Command command;
    private String text;

    public Message() {
    }

    public Message(Session session, Command command, String text) {
        this.session = session;
        this.command = command;
        this.text = text;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Message{" +
                "session=" + session +
                ", command=" + command +
                ", text='" + text + '\'' +
                '}';
    }
}
