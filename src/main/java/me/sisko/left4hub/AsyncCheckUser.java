package me.sisko.left4hub;

import java.sql.Connection;
import java.sql.Statement;

import org.json.JSONObject;
import org.bukkit.scheduler.BukkitRunnable;

import redis.clients.jedis.Jedis;

public class AsyncCheckUser extends BukkitRunnable {
	private Connection connection;
	private String uuid;
	private String name;

	public AsyncCheckUser setup(String uuid, String name) {
		this.uuid = uuid;
		this.name = name;
		return this;
	}

	public void run() {
		connection = Main.getConnection();
		uuid = uuid.replace("-", "");
		try {
			Statement statement = connection.createStatement();
			java.sql.ResultSet result = statement
					.executeQuery("SELECT * FROM joined_uuids WHERE UUID = UNHEX('" + uuid + "');");
			if (result.next()) {
				Main.getPlugin().getLogger().info(uuid + " has joined before.");
			} else {
				Jedis j = new Jedis(Main.getPlugin().getConfig().getString("redisip"));
				j.auth(Main.getPlugin().getConfig().getString("redispass"));

				JSONObject json = new JSONObject();
				json.put("type", "welcome");
				json.put("name", name);
				
				j.publish("minecraft.chat", json.toString());
				j.close();
				statement.executeUpdate("INSERT INTO joined_uuids (uuid) VALUES (UNHEX('" + uuid + "'))");
			}
			connection.close();

		} catch (java.sql.SQLException e) {
			e.printStackTrace();
		}
	}
}
