package ru.alexkulikov.peppachat.shared;

import java.io.Serializable;

public class Session implements Serializable {

    private long id;
    private String name;

    public Session() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
