package me.sisko.left4hub;

import org.bukkit.scheduler.BukkitRunnable;

public class ExecuteCommand extends BukkitRunnable {
	private String command;

	public ExecuteCommand(String command) {
		this.command = command;
	}

	public void run() {
		Main.getPlugin().getServer().dispatchCommand(Main.getPlugin().getServer().getConsoleSender(), command);
	}
}
