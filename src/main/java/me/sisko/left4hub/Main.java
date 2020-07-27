package me.sisko.left4hub;

import java.sql.Connection;
import java.sql.SQLException;

// import com.jcraft.jsch.JSch;
// import com.jcraft.jsch.JSchException;
// import com.jcraft.jsch.Session;

import litebans.api.*;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.ChatColor;
import redis.clients.jedis.Jedis;
import java.util.HashMap;
import org.json.JSONObject;

import org.apache.commons.dbcp2.BasicDataSource;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	private static BasicDataSource ds;
	//private static Connection wordpressConnection;
	//private static Connection phpbbConnection;
	private static Main plugin;
	private static LuckPerms perms;

	private ItemStack compass;
	private ItemStack book;
	private ItemStack chest;

	public void onEnable() {
		plugin = this;
		Bukkit.getPluginManager().registerEvents(this, this);

		// getLogger().info("Opening up SSH tunnel...");
		// JSch jsch = new JSch();
		// try {
		// 	Session session = jsch.getSession(getConfig().getString("ssh.user"), getConfig().getString("ssh.host"), getConfig().getInt("ssh.port"));
		// 	session.setPassword(getConfig().getString("ssh.pass"));
		// 	session.setConfig("StrictHostKeyChecking", "no");
		// 	session.setServerAliveCountMax(5);
		// 	session.setServerAliveInterval(10000);
		// 	session.connect(10000);
		// 	session.setPortForwardingL(3000, "127.0.0.1", 3306);
		// 	getLogger().info("Forwarded ports: " + session.getPortForwardingL()[0]);

		// } catch (JSchException e) {
		// 	e.printStackTrace();
		// }


		getCommand("givecosmetic").setExecutor(new GiveCosmeticCommand());
		getCommand("hubreload").setExecutor(new HubReloadCommand());

		ConfigManager.load();

		ds = new BasicDataSource();

		synchronized(this) {
			String host = plugin.getConfig().getString("local.host");
			String database = plugin.getConfig().getString("local.database");
			int port = plugin.getConfig().getInt("local.port");
			String user = plugin.getConfig().getString("local.user");
			String pass = plugin.getConfig().getString("local.pass");
	
			ds.setDriverClassName("com.mysql.jdbc.Driver");
			ds.setUrl("jdbc:mysql://" + host + ":" + port + "/" + database
			+ "?autoReconnect=true&verifyServerCertificate=false&useSSL=true");
			ds.setUsername(user);
			ds.setPassword(pass);
		}


		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager()
				.getRegistration(LuckPerms.class);
		if (provider != null) {
			perms = provider.getProvider();
		}

		Events.get().register(new Events$Listener() {
			public void broadcastSent(String message, String type) {
				message = ChatColor.stripColor(message);
				if (type.equals("broadcast")) {
                    Jedis j = new Jedis(Main.plugin.getConfig().getString("redisip"));
					j.auth(Main.plugin.getConfig().getString("redispass"));
					
					JSONObject json = new JSONObject();
					json.put("type", "broadcast");
					json.put("message", ChatColor.stripColor(message));
					
					j.publish("minecraft.chat.global.out", json.toString());
					if (message.split(" ")[1].contains("mute") || message.split(" ")[1].contains("ban")) {
						j.publish("minecraft.punish", "update");
					}
					j.close();
				}
			}
		});
		//new AsyncKeepAlive(getConnection()).runTaskTimerAsynchronously(this, 0L, 72000L);
		//new AsyncUpdateSubscriptions(connection, wordpressConnection, perms).runTaskTimerAsynchronously(this, 0l,
		//		1200l);
		//new AsyncLoginReward(connection, phpbbConnection).runTaskTimerAsynchronously(Main.getPlugin(), 0l, 1200l);

		new Announcer("&8Left&44&6Bot &7>> &aVote daily at www.left4craft.org/vote for rewards!").runTaskTimer(this, 0L,
				108000L);
		new Announcer("&8Left&44&6Bot &7>> &aGet cool perks at www.left4craft.org/shop").runTaskTimer(this, 36000L,
				108000L);
		// new Announcer("&8Left&44&6Bot &7>> &aPost on the forums at www.left4craft.org for weekly rewards!").runTaskTimer(this, 72000L,
		// 		108000L);

		ItemMeta meta;
		compass = new ItemStack(Material.COMPASS);
		meta = compass.getItemMeta();
		meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Server Selector");
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
				ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE);
		compass.setItemMeta(meta);

		book = new ItemStack(Material.BOOK);
		meta = book.getItemMeta();
		meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Help");
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
				ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE);
		book.setItemMeta(meta);

		chest = new ItemStack(Material.CHEST);
		meta = chest.getItemMeta();
		meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Cosmetics");
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
				ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE);
		chest.setItemMeta(meta);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		e.getPlayer().teleport(new Location(e.getPlayer().getWorld(), 4.5, 7, -115.5, 90, 0));
		e.getPlayer().getInventory().clear();
		e.getPlayer().getInventory().setItem(0, compass);
		e.getPlayer().getInventory().setItem(4, book);
		e.getPlayer().getInventory().setItem(8, chest);
		e.getPlayer().updateInventory();
		try {
			new AsyncCheckUser().setup(e.getPlayer().getUniqueId().toString(), e.getPlayer().getName())
					.runTaskAsynchronously(this);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Inventory inv = Bukkit.createInventory(null, InventoryType.PLAYER);

		inv.setItem(0, compass);
	}

	@EventHandler
	public void onHunger(FoodLevelChangeEvent e) {
		org.bukkit.entity.Entity en = e.getEntity();
		if ((en instanceof Player)) {
			e.setCancelled(true);
			((Player) en).setFoodLevel(20);
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if ((e.getEntity() instanceof Player))
			e.setCancelled(true);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if ((e.getWhoClicked() instanceof Player)) {
			Player p = (Player) e.getWhoClicked();
			if (p.getGameMode() != GameMode.CREATIVE) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
			e.setCancelled(true);
			e.getPlayer().updateInventory();
		}

	}

	@EventHandler
	public void onWeather(WeatherChangeEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
			e.setCancelled(true);
	}

	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
			e.setCancelled(true);
			e.getPlayer().updateInventory();
		}
	}

	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if (e.getPlayer().getGameMode() != GameMode.CREATIVE && p.getOpenInventory().getTitle().equalsIgnoreCase("crafting")) {
			ItemStack item = p.getInventory().getItemInMainHand();
			if (item.getType() == Material.COMPASS) {
				p.chat("/game");
			}
			else if (item.getType() == Material.BOOK) {
				p.chat("/help");
			}
			else if (item.getType() == Material.CHEST) {
				p.chat("/pc open main");
			}
		}
	}

	public static Connection getConnection() {
		try {
			return ds.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	// public Connection getPhpbbConnection() {
	// 	return phpbbConnection;
	// }

	public LuckPerms getPerms() {
		return perms;
	}

	public static Main getPlugin() {
		return plugin;
	}

	// public static synchronized void reconnectSQL() {
	// 	String host = plugin.getConfig().getString("local.host");
	// 	String database = plugin.getConfig().getString("local.database");
	// 	int port = plugin.getConfig().getInt("local.port");
	// 	String user = plugin.getConfig().getString("local.user");
	// 	String pass = plugin.getConfig().getString("local.pass");
	// 	try {
	// 		connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database
	// 				+ "?autoReconnect=true&verifyServerCertificate=false&useSSL=true", user, pass);
	// 		host = plugin.getConfig().getString("remote.host");
	// 		database = plugin.getConfig().getString("remote.database_wordpress");
	// 		port = plugin.getConfig().getInt("remote.port");
	// 		user = plugin.getConfig().getString("remote.user");
	// 		pass = plugin.getConfig().getString("remote.pass");
	// 		// wordpressConnection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database
	// 		// 		+ "?autoReconnect=true&verifyServerCertificate=false&useSSL=true", user, pass);
	// 		// host = plugin.getConfig().getString("remote.host");
	// 		// database = plugin.getConfig().getString("remote.database_phpbb");
	// 		// port = plugin.getConfig().getInt("remote.port");
	// 		// user = plugin.getConfig().getString("remote.user");
	// 		// pass = plugin.getConfig().getString("remote.pass");
	// 		// phpbbConnection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database
	// 		// 		+ "?autoReconnect=true&verifyServerCertificate=false&useSSL=true", user, pass);
	// 	} catch (SQLException e) {
	// 		e.printStackTrace();
	// 	}

	// }

	public void execute(String command) {
		new ExecuteCommand(command).runTask(this);
	}
}
