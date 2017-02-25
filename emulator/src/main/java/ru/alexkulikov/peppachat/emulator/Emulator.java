package ru.alexkulikov.peppachat.emulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Emulator {

    private List<Bot> bots = new ArrayList<>();
    private int botNumber = 0;
    private Random rnd = new Random();

    private void run() {
        while (true) {
            try {
                Thread.sleep(1000 * rnd.nextInt(10));
                Bot bot = new Bot();
                bots.add(bot);
                new Thread(() -> bot.start("bot" + botNumber)).start();
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
