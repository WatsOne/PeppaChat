package ru.alexkulikov.peppachat.emulator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Emulator {

	private static final boolean OBSERVE_ONE = true;
	private static final int SPEED = 3;
	private static final int FIX_BOT_COUNT = 100;
	private static final int SEND_COUNT = 200;

	private Random rnd = new Random();

	private Queue<Event> send = new ConcurrentLinkedQueue<>();
	private Queue<Event> receive = new ConcurrentLinkedQueue<>();

	private Queue<Bot> bots = new ConcurrentLinkedQueue<>();
	private Object stopEvent = new Object();

	private void run() {
		int botNumber = 0;

		try {
			if (OBSERVE_ONE) {
				Bot observeBot = new Bot();
				bots.add(observeBot);
				new Thread(() -> observeBot.start("observeBot", true, send, receive, bots)).start();
			}

			if (FIX_BOT_COUNT > 0) {
				for (int i = 0; i < (FIX_BOT_COUNT - 2); i++) {
					createAndStartBot(botNumber);
					botNumber++;
					Thread.sleep(100);
				}

				while (send.size() <= SEND_COUNT) {
					Thread.sleep(1);
				}
			} else {
				while (send.size() <= SEND_COUNT) {
					System.out.println(">>> Send messages: " + send.size());
					Thread.sleep(1000 * (rnd.nextInt(SPEED) + 1));
					createAndStartBot(botNumber);
					botNumber++;
				}
			}

			System.out.println("### Stop and calculating....");
			bots.forEach(Bot::stop);
			showResults();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createAndStartBot(final int botNumber) {
		Bot bot = new Bot();
		bots.add(bot);
		new Thread(() -> bot.start("bot" + botNumber, !OBSERVE_ONE, send, receive, bots)).start();
	}

	private void showResults() {
		long planReceive = 0;
		long factReceive = receive.size();
		for (Event e : send) planReceive += e.getBotCount();
		System.out.println(">>> Plan receive: " + planReceive + " | fact receive: " + factReceive + " | lost: " + (planReceive - factReceive));

		ArrayList<Long> times = new ArrayList<>();

		String sendMessage;
		long sendTime;

		for (Event sendEvent : send) {
			sendMessage = sendEvent.getMessage();
			sendTime = sendEvent.getTime();
			for (Event receiveEvent : receive) {
				if (sendMessage.equals(receiveEvent.getMessage())) {
					times.add(receiveEvent.getTime() - sendTime);
					break;
				}
			}
		}

		OptionalDouble avgTimeOpt = times.stream().mapToLong(t -> t).average();
		avgTimeOpt.ifPresent(t -> System.out.println(">>> Average receive time: " + t / 1000000 + " ms."));
	}

	public static void main(String[] args) throws Exception {
		new Emulator().run();
	}
}
