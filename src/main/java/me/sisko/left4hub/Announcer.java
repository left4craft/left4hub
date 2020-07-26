package me.sisko.left4hub;

import org.bukkit.scheduler.BukkitRunnable;

public class Announcer extends BukkitRunnable {
	private String message;

	public Announcer(String message) {
		this.message = message;
	}

	public void run() {
		Main.getPlugin().getServer().dispatchCommand(Main.getPlugin().getServer().getConsoleSender(), "announce " + message);
	}
}
