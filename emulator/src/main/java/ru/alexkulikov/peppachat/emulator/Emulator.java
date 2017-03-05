package ru.alexkulikov.peppachat.emulator;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class Emulator {

	private static final int DEFAULT_BOT_COUNT = 100;
	private static final int DEFAULT_SEND_COUNT = 200;

	private Queue<Event> send = new ConcurrentLinkedQueue<>();
	private Queue<Event> receive = new ConcurrentLinkedQueue<>();

	private Queue<Bot> bots = new ConcurrentLinkedQueue<>();

	private void run(int botCount, int sendCount) {
		final CountDownLatch doneLatch = new CountDownLatch(botCount);

		try {
			Bot observeBot = new Bot();
			bots.add(observeBot);
			new Thread(() -> observeBot.start("observeBot", true, send, receive, bots, sendCount, doneLatch)).start();

			for (int i = 1; i < botCount; i++) {
				createAndStartBot(i, sendCount, doneLatch);
				Thread.sleep(100);
			}

			doneLatch.await();
			showResults();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createAndStartBot(final int botNumber, final int sendCount, final CountDownLatch doneLatch) {
		Bot bot = new Bot();
		bots.add(bot);
		new Thread(() -> bot.start("bot" + botNumber, false, send, receive, bots, sendCount, doneLatch)).start();
	}

	private void showResults() {
		long planReceive = 0;
		long factReceive = receive.size();
		for (Event e : send) {
			planReceive += e.getBotCount();
		}
		System.out.println(">>> Plan receive: " + planReceive + " | fact receive: " + factReceive + " | lost: " + (planReceive - factReceive));

		OptionalDouble avgTimeOpt = receive.stream().map(Event::getTime).mapToLong(t -> t).average();
		avgTimeOpt.ifPresent(t -> System.out.println(">>> Average receive time: " + t + " ms."));
	}

	public static void main(String[] args) throws Exception {
		String botCountArg = args.length > 0 ? args[0] : "";
		String sendCountArg = args.length > 1 ? args[1] : "";

		int botCount = StringUtils.isEmpty(botCountArg) ? DEFAULT_BOT_COUNT : Integer.valueOf(botCountArg);
		int sendCount = StringUtils.isEmpty(sendCountArg) ? DEFAULT_SEND_COUNT : Integer.valueOf(sendCountArg);

		System.out.println("Starting test with bot count: " + botCount + " and send count: " + sendCount + "...");
		Thread.sleep(3000);
		new Emulator().run(botCount, sendCount);
	}
}
