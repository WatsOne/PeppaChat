package ru.alexkulikov.peppachat.emulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Emulator {

    private static final boolean OBSERVE_ONE = true;
    private static final int SPEED = 3;

    private List<Bot> bots = new ArrayList<>();
    private int botNumber = 0;
    private Random rnd = new Random();

    private void run() {
        if (OBSERVE_ONE) {
            new Thread(() -> new Bot().start("observeBot", true)).start();
        }

        while (true) {
            try {
                Thread.sleep(1000 * (rnd.nextInt(SPEED) + 1));
                Bot bot = new Bot();
                bots.add(bot);
                new Thread(() -> bot.start("bot" + botNumber, !OBSERVE_ONE)).start();
                botNumber++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Emulator().run();
    }
}
