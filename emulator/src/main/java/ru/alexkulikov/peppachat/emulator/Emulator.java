package ru.alexkulikov.peppachat.emulator;

public class Emulator {
    public static void main(String[] args) throws Exception {
        new Thread(() -> new Bot().start("bot1")).start();
        new Thread(() -> new Bot().start("bot2")).start();
        new Thread(() -> new Bot().start("bot3")).start();
        new Thread(() -> new Bot().start("bot4")).start();
        new Thread(() -> new Bot().start("bot5")).start();
        new Thread(() -> new Bot().start("bot6")).start();
        new Thread(() -> new Bot().start("bot7")).start();
        new Thread(() -> new Bot().start("bot8")).start();
        new Thread(() -> new Bot().start("bot9")).start();
        new Thread(() -> new Bot().start("bot10")).start();
        new Thread(() -> new Bot().start("bot11")).start();
        new Thread(() -> new Bot().start("bot12")).start();
    }
}
