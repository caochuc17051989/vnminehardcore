package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class TooltipManager {
    private final VnMineHardcore plugin;
    private final File configFile;
    private YamlConfiguration settings;
    private List<String> tooltips = List.of();
    private String mode = "random";
    private int intervalSeconds = 300;
    private int nextIndex;
    private BukkitTask task;

    public TooltipManager(VnMineHardcore plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "tooltips.yml");
        if (!configFile.isFile()) plugin.saveResource("tooltips.yml", false);
        if (!reload()) throw new IllegalStateException("Could not load tooltips.yml.");
    }

    public boolean reload() {
        YamlConfiguration loaded = new YamlConfiguration();
        try {
            loaded.load(configFile);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.SEVERE, "[Tooltip] Could not load tooltips.yml.", exception);
            return false;
        }
        int interval = loaded.getInt("settings.interval-seconds", 300);
        String selection = loaded.getString("settings.mode", "random");
        if (interval < 1 || interval > 86_400 || selection == null
                || !List.of("random", "sequential").contains(selection.toLowerCase(Locale.ROOT))) {
            plugin.getLogger().severe("[Tooltip] Invalid settings.interval-seconds or settings.mode in tooltips.yml.");
            return false;
        }
        List<String> messages = loaded.getStringList("tooltips").stream()
            .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (messages.isEmpty()) {
            plugin.getLogger().severe("[Tooltip] tooltips.yml must define at least one non-empty tooltip.");
            return false;
        }
        settings = loaded;
        intervalSeconds = interval;
        mode = selection.toLowerCase(Locale.ROOT);
        tooltips = messages;
        nextIndex = Math.floorMod(nextIndex, tooltips.size());
        startTask();
        return true;
    }

    private void startTask() {
        if (task != null) task.cancel();
        long intervalTicks = intervalSeconds * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastNext, intervalTicks, intervalTicks);
    }

    private void broadcastNext() {
        if (tooltips.isEmpty()) return;
        String message;
        if (mode.equals("random")) {
            message = tooltips.get(ThreadLocalRandom.current().nextInt(tooltips.size()));
        } else {
            message = tooltips.get(nextIndex);
            nextIndex = (nextIndex + 1) % tooltips.size();
        }
        for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(message);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
