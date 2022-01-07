package me.sisko.left4hub;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;

public class AsyncChangeChests extends BukkitRunnable {
	private Connection conn;
	private final String[] tiers = {"normal-treasures", "mythical-treasures", "legendary-treasures"};
	private String name;
	private String uuid;
	private String tier;
	private int amount;

	public AsyncChangeChests(String name, String uuid, int tier, int amount) {
		this.name = name;
		this.uuid = uuid;
		this.tier = tiers[tier];
		this.amount = amount;
	}

	public void run() {		
		conn = Main.getConnection();
		try {
			ResultSet rs = conn.createStatement().executeQuery("SELECT data FROM procosmetics WHERE UUID=\"" + uuid + "\";");
			if (rs.next()) {
				JSONObject playerData = new JSONObject(rs.getString(1));
				if(playerData.has(tier)) {
					playerData.put(tier, playerData.getInt(tier) + amount);
				} else {
					playerData.put(tier, amount);
				}
				conn.createStatement().executeUpdate("UPDATE procosmetics SET data = '" + playerData.toString() + "' WHERE UUID=\"" + uuid + "\";");
				Jedis j = new Jedis(Main.getPlugin().getConfig().getString("redisip"));
				j.auth(Main.getPlugin().getConfig().getString("redispass"));
				j.publish("minecraft.console.hub.in", "pc give treasure " + name + " " + tier.split("-")[0] + " " + amount);
				j.publish("minecraft.console.survival.in", "pc give treasure " + name + " " + tier.split("-")[0] + " " + amount);
				j.publish("minecraft.console.creative.in", "pc give treasure " + name + " " + tier.split("-")[0] + " " + amount);
				j.publish("minecraft.console.build.in", "pc give treasure " + name + " " + tier.split("-")[0] + " " + amount);
				j.close();
				Main.getPlugin().getLogger().info("Added " + amount + " keys to offline player " + uuid + " with teir " + tier + ". New Balance: " + playerData.getInt(tier));
			} else {
				Main.getPlugin().getLogger().warning("Could not find offline player " + uuid + " in the procosmetics database!");
			}
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
