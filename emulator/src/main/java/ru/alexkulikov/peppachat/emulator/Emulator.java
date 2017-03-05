package ru.alexkulikov.peppachat.emulator;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class Emulator {

	private static final int FIX_BOT_COUNT = 300;
	private static final int SEND_COUNT = 500;

	private Queue<Event> send = new ConcurrentLinkedQueue<>();
	private Queue<Event> receive = new ConcurrentLinkedQueue<>();

	private Queue<Bot> bots = new ConcurrentLinkedQueue<>();
	private static final CountDownLatch DONE_LATCH = new CountDownLatch(FIX_BOT_COUNT);

	private void run() {
		try {
			Bot observeBot = new Bot();
			bots.add(observeBot);
			new Thread(() -> observeBot.start("observeBot", true, send, receive, bots, SEND_COUNT, DONE_LATCH)).start();

			for (int i = 1; i < FIX_BOT_COUNT; i++) {
				createAndStartBot(i);
				Thread.sleep(100);
			}

			DONE_LATCH.await();
			showResults();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createAndStartBot(final int botNumber) {
		Bot bot = new Bot();
		bots.add(bot);
		new Thread(() -> bot.start("bot" + botNumber, false, send, receive, bots, SEND_COUNT, DONE_LATCH)).start();
	}

	private void showResults() {
		long planReceive = 0;
		long factReceive = receive.size();
		for (Event e : send) {
			planReceive += e.getBotCount();
		}
		System.out.println(">>> Plan receive: " + planReceive + " | fact receive: " + factReceive + " | lost: " + (planReceive - factReceive));

		OptionalDouble avgTimeOpt = receive.stream().map(Event::getTime).mapToLong(t -> t).average();
		avgTimeOpt.ifPresent(t -> System.out.println(">>> Average receive time: " + t / 1000000 + " ms."));
	}

	public static void main(String[] args) throws Exception {
		new Emulator().run();
	}
}
