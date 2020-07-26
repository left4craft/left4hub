package me.sisko.left4hub;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;

public class AsyncChangeCoins extends BukkitRunnable {
	private Connection conn;
	private String name;
	private String uuid;
	private int amount;

	public AsyncChangeCoins(String name, String uuid, int amount) {
		this.name = name;
		this.uuid = uuid;
		this.amount = amount;
	}

	public void run() {
		conn = Main.getConnection();
		try {
			ResultSet rs = conn.createStatement().executeQuery("SELECT data FROM procosmetics WHERE UUID=\"" + uuid + "\";");
			if (rs.next()) {
				JSONObject playerData = new JSONObject(rs.getString(1));
				if(playerData.has("coins")) {
					playerData.put("coins", playerData.getInt("coins") + amount);
				} else {
					playerData.put("coins", amount);
				}

				conn.createStatement().executeUpdate("UPDATE procosmetics SET data = '" + playerData.toString() + "' WHERE UUID=\"" + uuid + "\";");
				Jedis j = new Jedis(Main.getPlugin().getConfig().getString("redisip"));
				j.auth(Main.getPlugin().getConfig().getString("redispass"));
				j.publish("minecraft.console.hub.in", "pc give coins " + name + " " + amount);
				j.publish("minecraft.console.survival.in", "pc give coins " + name + " " + amount);
				j.publish("minecraft.console.creative.in", "pc give coins " + name + " " + amount);
				j.publish("minecraft.console.build.in", "pc give coins " + name + " " + amount);
				j.close();
				Main.getPlugin().getLogger().info("Added " + amount + " coins to offline player " + uuid + ". New Balance: " + playerData.getInt("coins"));
			} else {
				Main.getPlugin().getLogger().warning("Could not find offline player " + uuid + " in the procosmetics database!");
			}
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
