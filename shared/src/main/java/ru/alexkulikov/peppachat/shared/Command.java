package ru.alexkulikov.peppachat.shared;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Command {
    REGISTER,
    MESSAGE,
    SERVER_MESSAGE,
    ID,

    HELP(false, "Get information about commands"),
    ONLINE(false, "Get current online"),
    EXIT(false, "Left chat");

    private boolean service;
    private String description;

    private Command() {
        this.service = true;
        this.description = null;
    }

    private Command(boolean service, String description) {
        this.service = service;
        this.description = description;
    }

    public boolean isService() {
        return service;
    }

    public static Command getCommand(String value) {
        for (Command command : values()) {
            if (!command.isService() && command.name().equalsIgnoreCase(value)) {
                return command;
            }
        }

        return null;
    }

    public static String getCommandInfo() {
        return Stream.of(values())
                .filter(c -> !c.isService())
                .map(c -> c.name() + " - " + c.description)
                .collect(Collectors.joining("\n"));
    }
}
