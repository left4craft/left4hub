package me.sisko.left4hub;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    public static void load() {
        FileConfiguration config = Main.getPlugin().getConfig();
        File dataFolder = Main.getPlugin().getDataFolder();
        config.addDefault("local.host", "127.0.0.1");
        config.addDefault("local.database", "data");
        config.addDefault("local.port", 3306);
        config.addDefault("local.user", "user");
        config.addDefault("local.pass", "password");
        config.addDefault("remote.host", "127.0.0.1");
        config.addDefault("remote.database_wordpress", "data");
        config.addDefault("remote.database_phpbb", "data");
        config.addDefault("remote.port", 3306);
        config.addDefault("remote.user", "user");
        config.addDefault("remote.pass", "password");
        config.addDefault("ssh.host", "left4craft.org");
        config.addDefault("ssh.port", 22);
        config.addDefault("ssh.user", "user");
        config.addDefault("ssh.pass", "password");

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!new File(dataFolder, "config.yml").exists()) {
            Main.getPlugin().getLogger().info("Config.yml not found, creating!");
            config.options().copyDefaults(true);
            Main.getPlugin().saveConfig();
        } else {
            Main.getPlugin().getLogger().info("Config.yml found, loading!");
        }
    }

    public static void reload() {
        Main.getPlugin().reloadConfig();
    }
}

