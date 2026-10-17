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
    private List<String> playerTooltips = List.of();
    private List<String> adminTooltips = List.of();
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
        if (loaded.isList("tooltips")) {
            List<String> legacy = loaded.getStringList("tooltips");
            List<String> players = legacy.stream().filter(message -> !isLegacyAdminTooltip(message)).toList();
            List<String> admins = legacy.stream().filter(TooltipManager::isLegacyAdminTooltip).toList();
            loaded.set("tooltips", null);
            loaded.set("tooltips.players", players);
            loaded.set("tooltips.admins", admins);
            try {
                loaded.save(configFile);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.SEVERE, "[Tooltip] Could not migrate tooltips.yml.", exception);
                return false;
            }
        }
        List<String> players = loaded.getStringList("tooltips.players").stream()
            .map(String::trim).filter(value -> !value.isEmpty()).toList();
        List<String> admins = loaded.getStringList("tooltips.admins").stream()
            .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (players.isEmpty()) {
            plugin.getLogger().severe("[Tooltip] tooltips.yml must define at least one player tooltip.");
            return false;
        }
        settings = loaded;
        intervalSeconds = interval;
        mode = selection.toLowerCase(Locale.ROOT);
        playerTooltips = players;
        adminTooltips = admins;
        nextIndex = Math.floorMod(nextIndex, Math.max(players.size(), admins.size()));
        startTask();
        return true;
    }

    private static boolean isLegacyAdminTooltip(String message) {
        return message.contains("/vnevent") || message.contains("/vnboss");
    }

    private void startTask() {
        if (task != null) task.cancel();
        long intervalTicks = intervalSeconds * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastNext, intervalTicks, intervalTicks);
    }

    private void broadcastNext() {
        if (playerTooltips.isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            List<String> messages = new java.util.ArrayList<>(playerTooltips);
            if (isAdmin(player)) messages.addAll(adminTooltips);
            if (messages.isEmpty()) continue;
            int index = mode.equals("random")
                ? ThreadLocalRandom.current().nextInt(messages.size())
                : Math.floorMod(nextIndex, messages.size());
            player.sendMessage(messages.get(index));
        }
        if (mode.equals("sequential")) nextIndex++;
    }

    private boolean isAdmin(Player player) {
        return player.isOp() || List.of(
            "vnmine.command.reload",
            "vnmine.command.disasters",
            "vnmine.command.bosses",
            "vnmine.command.utility",
            "vnmine.command.scoreboard.reset",
            "vnmine.command.time.manage",
            "vnmine.command.day.manage",
            "vnmine.command.spawn.manage",
            "vnmine.command.teleport.other",
            "vnmine.command.gamemode",
            "vnmine.command.fly",
            "vnmine.command.nodame",
            "vnmine.command.groups.manage",
            "vnmine.command.users.manage",
            "vnmine.command.army.admin",
            "vnmine.command.death.history"
        ).stream().anyMatch(permission ->
            plugin.getPermissionManager().hasPermission(player, permission));
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
