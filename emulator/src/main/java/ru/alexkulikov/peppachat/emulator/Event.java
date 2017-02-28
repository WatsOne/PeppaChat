package ru.alexkulikov.peppachat.emulator;

public class Event {

	private String message;
	private long time;
	private int botCount;

	public Event(String message, long time, int botCount) {
		this.message = message;
		this.time = time;
		this.botCount = botCount;
	}

	public String getMessage() {
		return message;
	}

	public long getTime() {
		return time;
	}

	public int getBotCount() {
		return botCount;
	}
}
