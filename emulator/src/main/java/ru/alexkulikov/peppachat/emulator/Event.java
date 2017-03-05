package ru.alexkulikov.peppachat.emulator;

public class Event {

	private long time;
	private int botCount;

	public Event(long time, int botCount) {
		this.time = time;
		this.botCount = botCount;
	}

	public long getTime() {
		return time;
	}

	public int getBotCount() {
		return botCount;
	}
}