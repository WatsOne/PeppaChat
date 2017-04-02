package ru.alexkulikov.peppachat.server.storage;

public class StorageFactory {
	public static MemoryStorage getStorage() {
		return new MemoryStorage();
	}
}
