package me.sisko.left4hub;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class HubReloadCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			((Player) sender).sendMessage(ChatColor.RED + "No permission.");
		} else {
			Main.getPlugin().getLogger().info(ChatColor.GREEN + "Reloading Left4Hub...");
			ConfigManager.reload();
			Main.getPlugin().getDonationListener().readStripeConfig("stripe.json");
		}
		return true;
	}
}
