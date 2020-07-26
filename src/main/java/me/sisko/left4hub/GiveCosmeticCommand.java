package me.sisko.left4hub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class GiveCosmeticCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			((Player) sender).sendMessage(ChatColor.RED + "No permission.");
		} else {
			if(args.length < 2) {
				Main.getPlugin().getLogger().info("Usage: /givecosmetic <name> <amount> [tier]");
			} else {
				for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
					if (op.getName().equalsIgnoreCase(args[0])) {
						String uuid = op.getUniqueId().toString();
						if (args.length == 2) {
							new AsyncChangeCoins(op.getName(), uuid, Integer.parseInt(args[1])).runTaskAsynchronously(Main.getPlugin());

						} else {
							new AsyncChangeChests(op.getName(), uuid, Integer.parseInt(args[2]), Integer.parseInt(args[1])).runTaskAsynchronously(Main.getPlugin());
						}
						return true;
					}
				}
				Main.getPlugin().getLogger().warning("Could not find player " + args[0] + "!");
			}
		}
		return true;
	}
}
