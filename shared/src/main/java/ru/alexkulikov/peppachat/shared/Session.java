package ru.alexkulikov.peppachat.shared;

import java.io.Serializable;

public class Session implements Serializable {

    private Long id;
    private String userName;

    public Session() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                '}';
    }
}
