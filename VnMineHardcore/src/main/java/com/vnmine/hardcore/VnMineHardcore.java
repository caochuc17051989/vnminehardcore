package com.vnmine.hardcore;

import com.vnmine.hardcore.listeners.*;
import com.vnmine.hardcore.managers.*;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VnMineHardcore extends JavaPlugin {

    private static VnMineHardcore instance;
    private ConfigManager configManager;
    private LogManager logManager;
    private ThirstManager thirstManager;
    private DisasterManager disasterManager;
    private DeathRenameManager deathRenameManager;
    private DeathPenaltyManager deathPenaltyManager;
    private BossEventManager bossEventManager;
    private GraveManager graveManager;
    private VillagerDataManager villagerDataManager;
    private DeathListener deathListener;
    private CombatListener combatListener;
    private EnvironmentListener environmentListener;
    private HungerListener hungerListener;
    private WaterListener waterListener;
    private WorldInteractionListener worldInteractionListener;
    private SpawnerControlListener spawnerControlListener;
    private VillagerTradeManager villagerTradeManager;
    private OreControlListener oreControlListener;
    private TimeControlManager timeControlManager;
    private GraveListener graveListener;

    @Override
    public void onEnable() {
        instance = this;
        clearLegacyPluginBans();

        getLogger().info("§a==========================================");
        getLogger().info("§a  VnMineHardcore v1.3.4 - KHỞI ĐỘNG");
        getLogger().info("§a==========================================");

        // Load config first
        this.configManager = new ConfigManager(this);
        this.timeControlManager = new TimeControlManager(this, configManager);
        this.timeControlManager.reload();

        // Initialize managers (pass config)
        this.logManager = new LogManager(this, configManager);
        this.deathPenaltyManager = new DeathPenaltyManager(this, configManager);
        this.thirstManager = new ThirstManager(this, configManager);
        this.disasterManager = new DisasterManager(this, configManager);
        this.deathRenameManager = new DeathRenameManager(this, configManager);
        this.bossEventManager = new BossEventManager(this, configManager);
        this.villagerDataManager = new VillagerDataManager(this, configManager);
        this.graveManager = new GraveManager(this, configManager);

        // Create listeners (pass config)
        this.deathListener = new DeathListener(this, configManager, deathRenameManager, deathPenaltyManager);
        this.combatListener = new CombatListener(this, deathListener, configManager);
        this.environmentListener = new EnvironmentListener(this, configManager);
        this.hungerListener = new HungerListener(this, configManager);
        this.waterListener = new WaterListener(this);
        this.worldInteractionListener = new WorldInteractionListener(this);
        this.spawnerControlListener = new SpawnerControlListener(this, configManager);
        this.villagerTradeManager = new VillagerTradeManager(this, configManager, villagerDataManager);
        this.oreControlListener = new OreControlListener(this, configManager);
        this.graveListener = new GraveListener(this, graveManager);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(deathListener, this);
        Bukkit.getPluginManager().registerEvents(hungerListener, this);
        Bukkit.getPluginManager().registerEvents(combatListener, this);
        Bukkit.getPluginManager().registerEvents(environmentListener, this);
        Bukkit.getPluginManager().registerEvents(waterListener, this);
        Bukkit.getPluginManager().registerEvents(worldInteractionListener, this);
        Bukkit.getPluginManager().registerEvents(spawnerControlListener, this);
        Bukkit.getPluginManager().registerEvents(villagerTradeManager, this);
        Bukkit.getPluginManager().registerEvents(oreControlListener, this);
        Bukkit.getPluginManager().registerEvents(graveListener, this);

        // Register commands
        this.getCommand("vnstats").setExecutor(this);
        this.getCommand("vnhardcore").setExecutor(this);
        this.getCommand("vnreload").setExecutor(this);
        this.getCommand("vnhelp").setExecutor(this);
        this.getCommand("vnevent").setExecutor(this);
        this.getCommand("vnboss").setExecutor(this);
        this.getCommand("vn").setExecutor(this);
        this.getCommand("vnrespawn").setExecutor(this);
        this.getCommand("vnleave").setExecutor(this);
        this.getCommand("fly").setExecutor(this);
        this.getCommand("nodame").setExecutor(this);
        this.getCommand("spawn").setExecutor(this);
        this.getCommand("gm0").setExecutor(this);
        this.getCommand("gm1").setExecutor(this);
        this.getCommand("gm2").setExecutor(this);
        this.getCommand("gm3").setExecutor(this);

        getLogger().info("§a  VnMineHardcore enabled - Hell difficulty!");
        getLogger().info("§a  Deaths are recorded; no death bans are applied.");
        getLogger().info("§a  Use /vnhelp for command list");
        getLogger().info("§a  Use /vnreload to reload config");
        getLogger().info("§a  Use /vnevent to manually trigger disasters");
        getLogger().info("§a==========================================");
    }

    @Override
    public void onDisable() {
        if (thirstManager != null) thirstManager.stop();
        if (deathPenaltyManager != null) deathPenaltyManager.stop();
        if (disasterManager != null) disasterManager.stop();
        if (bossEventManager != null) bossEventManager.stop();
        if (graveManager != null) graveManager.stop();
        if (logManager != null) logManager.stop();
        if (timeControlManager != null) timeControlManager.stop();
        getLogger().info("VnMineHardcore disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!sender.hasPermission("vnmine.hardcore.admin")) {
                sender.sendMessage("§cBạn không có quyền!");
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
                return true;
            }
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage("§aĐã dịch chuyển về điểm spawn của thế giới.");
            return true;
        }

        if (command.getName().matches("(?i)gm[0-3]")) {
            if (!sender.hasPermission("vnmine.hardcore.admin")) {
                sender.sendMessage("§cBạn không có quyền!");
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
                return true;
            }
            org.bukkit.GameMode gameMode = switch (command.getName().toLowerCase(Locale.ROOT)) {
                case "gm0" -> org.bukkit.GameMode.SURVIVAL;
                case "gm1" -> org.bukkit.GameMode.CREATIVE;
                case "gm2" -> org.bukkit.GameMode.ADVENTURE;
                case "gm3" -> org.bukkit.GameMode.SPECTATOR;
                default -> throw new IllegalStateException("Unexpected game mode command");
            };
            player.setGameMode(gameMode);
            player.sendMessage("§aĐã chuyển sang chế độ §e" + gameMode.name().toLowerCase(Locale.ROOT) + "§a.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("fly")) {
            if (!sender.hasPermission("vnmine.hardcore.admin")) {
                sender.sendMessage("§cBạn không có quyền!");
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
                return true;
            }
            if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL) {
                player.sendMessage("§cLệnh /fly chỉ hoạt động trong chế độ Sinh tồn.");
                return true;
            }
            if (player.getAllowFlight()) {
                player.setFlying(false);
                player.setAllowFlight(false);
                player.sendMessage("§eĐã tắt bay trong chế độ Sinh tồn.");
            } else {
                player.setAllowFlight(true);
                player.sendMessage("§aĐã bật bay trong chế độ Sinh tồn.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("nodame")) {
            if (!sender.hasPermission("vnmine.hardcore.admin")) {
                sender.sendMessage("§cBạn không có quyền!");
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
                return true;
            }
            boolean enabled = deathListener.toggleNoDamageMode(player);
            player.sendMessage(enabled
                ? "§aĐã bật /nodame: quái vẫn có thể chọn bạn làm mục tiêu nhưng bạn không nhận sát thương."
                : "§eĐã tắt /nodame.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("vnrespawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
                return true;
            }
            if (!deathListener.requestRespawn(player)) {
                player.sendMessage("§cBạn hiện không chờ chọn hồi sinh.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("vnleave")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
                return true;
            }
            if (!deathListener.isAwaitingRespawnChoice(player)) {
                player.sendMessage("§cBạn chỉ có thể dùng lệnh này sau khi chết.");
                return true;
            }
            player.kick(net.kyori.adventure.text.Component.text("Bạn đã chọn thoát game sau khi chết."));
            return true;
        }

        if (command.getName().equalsIgnoreCase("vnhelp")) {
            sender.sendMessage("§6§m========================================");
            sender.sendMessage("§6§l  VnMineHardcore - Command Help");
            sender.sendMessage("§6§m========================================");
            sender.sendMessage("§e/vnstats §7- Xem thống kê của bạn");
            sender.sendMessage("§e/vnhardcore §7- Xem trạng thái plugin");
            sender.sendMessage("§e/vn death reset <player> §7- Reset số lần chết về 0");
            sender.sendMessage("§e/vnevent §7- Gọi thiên tai thủ công (xem danh sách)");
            sender.sendMessage("§e/vnevent <id> <time> <duration> §7- Gọi thiên tai cụ thể");
            sender.sendMessage("§e/vnboss <id> <level> <warning> <duration> §7- Gọi boss theo level");
            sender.sendMessage("§e/fly §7- Bật/tắt bay Survival (admin)");
            sender.sendMessage("§e/nodame §7- Bật/tắt miễn sát thương để thử boss (admin)");
            sender.sendMessage("§e/spawn §7- Về điểm spawn của thế giới (admin)");
            sender.sendMessage("§e/gm0 /gm1 /gm2 /gm3 §7- Chuyển game mode (admin)");
            sender.sendMessage("§e/vnreload §7- Tải lại config.yml");
            sender.sendMessage("§e/vnhelp §7- Hiển thị help này");
            sender.sendMessage("§6§m========================================");
            return true;
        }

        if (command.getName().equalsIgnoreCase("vnreload")) {
            if (!sender.hasPermission("vnmine.hardcore.admin")) {
                sender.sendMessage("§cBạn không có quyền!");
                return true;
            }
            configManager.reload();
            timeControlManager.reload();
            hungerListener.reload();
            environmentListener.reload();
            thirstManager.reload();
            deathListener.reload();
            deathPenaltyManager.reload();
            graveManager.reload();
            disasterManager.reload();
            bossEventManager.reload();
            sender.sendMessage("§a✅ Config và các hệ thống đã được tải lại!");
            getLogger().info("[Cmd] " + sender.getName() + " reloaded config");
            return true;
        }

        if (command.getName().equalsIgnoreCase("vnevent")) {
            if (!sender.hasPermission("vnmine.hardcore.admin")) {
                sender.sendMessage("§cBạn không có quyền!");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("§6§m=======================================");
                sender.sendMessage("§6§l  /vnevent - Gọi thiên tai thủ công");
                sender.sendMessage("§6§m=======================================");
                sender.sendMessage("§eUsage: §7/vnevent <id> <warning(s)> <duration(s)>");
                sender.sendMessage("§e  /vnevent §7- Hiển thị danh sách này");
                sender.sendMessage("");
                sender.sendMessage("§eDanh sách EVENT_ID có thể dùng:");
                for (String id : disasterManager.getDisasterIds()) {
                    String name = disasterManager.getDisasterName(id);
                    sender.sendMessage("§c  " + id + "  §7- " + name);
                }
                sender.sendMessage("");
                sender.sendMessage("§eVí dụ: §7/vnevent blood-moon 30 60");
                sender.sendMessage("§6§m=======================================");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cSử dụng: /vnevent <id> <warning(s)> <duration(s)>");
                return true;
            }
            String eventId = args[0].toLowerCase();
            int warningTime, duration;
            try {
                warningTime = Integer.parseInt(args[1]);
                duration = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cThời gian phải là số nguyên (giây)!");
                return true;
            }
            if (warningTime < 1 || duration < 1) {
                sender.sendMessage("§cThời gian phải >= 1 giây!");
                return true;
            }
            boolean success = disasterManager.triggerDisaster(eventId, warningTime, duration);
            if (success) {
                sender.sendMessage("§a✅ Đã kích hoạt §e" + disasterManager.getDisasterName(eventId));
            } else {
                if (disasterManager.isDisasterActive()) {
                    sender.sendMessage("§c❌ Đã có thiên tai đang hoạt động: " + disasterManager.getCurrentDisaster());
                } else {
                    sender.sendMessage("§c❌ ID '" + eventId + "' không hợp lệ!");
                }
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("vnboss")) {
            if (!sender.hasPermission("vnmine.hardcore.admin")) {
                sender.sendMessage("§cBạn không có quyền!");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("§6§m=======================================");
                sender.sendMessage("§6§l  /vnboss - Gọi boss thủ công");
                sender.sendMessage("§6§m=======================================");
                sender.sendMessage("§eUsage: §7/vnboss <id> <level> <warning(s)> <duration(s)>");
                sender.sendMessage("§e  /vnboss §7- Hiển thị danh sách này");
                sender.sendMessage("");
                sender.sendMessage("§eDanh sách BOSS_ID có thể dùng:");
                for (String id : bossEventManager.getBossIds()) {
                    sender.sendMessage("§c  " + id + "  §7- " + bossEventManager.getBossName(id));
                }
                sender.sendMessage("");
                sender.sendMessage("§eVí dụ: §7/vnboss wither 1 30 120");
                sender.sendMessage("§e  /vnboss wither 3 30 120 §7- Gọi Wither level 3");
                sender.sendMessage("§6§m=======================================");
                return true;
            }
            if (args.length != 4) {
                sender.sendMessage("§cSử dụng: /vnboss <id> <level> <warning(s)> <duration(s)>");
                return true;
            }
            String bossId = args[0].toLowerCase(Locale.ROOT);
            int level, warningTime, duration;
            try {
                level = Integer.parseInt(args[1]);
                warningTime = Integer.parseInt(args[2]);
                duration = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cLevel và thời gian phải là số nguyên!");
                return true;
            }
            if (level < 1 || level > 1000 || warningTime < 1 || duration < 1) {
                sender.sendMessage("§cLevel phải từ 1 đến 1000; thời gian phải >= 1 giây!");
                return true;
            }
            boolean success = bossEventManager.triggerBoss(bossId, level, warningTime, duration);
            if (success) {
                sender.sendMessage("§a✅ Đã kích hoạt boss §e" + bossEventManager.getBossName(bossId) +
                    " §7(level " + level + ")");
            } else {
                if (bossEventManager.isBossActive()) {
                    sender.sendMessage("§c❌ Đã có boss đang hoạt động: " + bossEventManager.getCurrentBossName());
                } else {
                    sender.sendMessage("§c❌ ID boss '" + bossId + "' không hợp lệ hoặc đã bị tắt!");
                }
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("vnstats")) {
            if (sender instanceof Player player) {
                sender.sendMessage("§6=== VnMineHardcore Stats ===");
                sender.sendMessage("§eDeaths: §c" + logManager.getDeathCount(player));
                sender.sendMessage("§eSurvival Time: §a" + logManager.getSurvivalTime(player));
                sender.sendMessage("§eMob Kills: §b" + logManager.getMobKills(player));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("vnhardcore")) {
            sender.sendMessage("§6§m========================================");
            sender.sendMessage("§6§l  VnMineHardcore - Admin Status");
            sender.sendMessage("§6§m========================================");
            sender.sendMessage("§c☠ DEATH: §aTracked | Tag=§aON (" +
                (configManager.combatTagDurationMs / 1000) + "s)");
            sender.sendMessage("§e🍔 HUNGER: " + (configManager.hungerEnabled ? "§aON" : "§cOFF"));
            sender.sendMessage("§b💧 THIRST: " + (configManager.thirstEnabled ? "§aON" : "§cOFF") +
                " | Source=" + (configManager.drinkFromSource ? "§aON" : "§cOFF"));
            sender.sendMessage("§c⚔ COMBAT: Mob=x" + configManager.mobDamageMultiplier +
                " Fall=x" + configManager.fallDamageMultiplier +
                " Regen=" + (!configManager.disableNaturalRegen ? "§aON" : "§cOFF"));
            sender.sendMessage("§a🌍 ENV: Temp=" + (configManager.temperatureEnabled ? "§aON" : "§cOFF") +
                " Fog=" + (configManager.fogEnabled ? "§aON" : "§cOFF") +
                " Fly=" + (configManager.flightBlock ? "§cBLOCK" : "§aOK"));
            sender.sendMessage("§4🌋 DISASTERS: " + (configManager.disastersEnabled ? "§aON" : "§cOFF") +
                (disasterManager.isDisasterActive() ? " §c⚠ ACTIVE: " + disasterManager.getCurrentDisaster() : ""));
            sender.sendMessage("§c👹 BOSS: " + (configManager.bossEventsEnabled ? "§aON" : "§cOFF") +
                (bossEventManager.isBossActive() ? " §c⚠ ACTIVE: " + bossEventManager.getCurrentBossName() : ""));
            sender.sendMessage("§e🏷 RENAME: " + (configManager.renameEnabled ? "§aON" : "§cOFF"));
            sender.sendMessage("§eCommands: §7/vnhelp | /vnreload | /vnevent | /vn death reset <player>");
            sender.sendMessage("§6§m========================================");
            return true;
        }

        if (command.getName().equalsIgnoreCase("vn")) {
            if (args.length == 0) {
                sender.sendMessage("§cSử dụng: /vn <subcommand>");
                sender.sendMessage("§cGõ /vnhelp để xem hướng dẫn.");
                return true;
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("death") && args[1].equalsIgnoreCase("reset")) {
                if (!sender.hasPermission("vnmine.hardcore.admin")) {
                    sender.sendMessage("§cBạn không có quyền!");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cSử dụng: /vn death reset <player>");
                    return true;
                }
                String targetName = args[2];
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    logManager.resetDeathCount(target.getUniqueId());
                    deathRenameManager.updateDisplayName(target, 0);
                    sender.sendMessage("§a✅ Đã reset số lần chết của §e" + target.getName());
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
                    if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
                        logManager.resetDeathCount(offlinePlayer.getUniqueId());
                        sender.sendMessage("§a✅ Đã reset số lần chết của §e" + offlinePlayer.getName());
                    } else {
                        sender.sendMessage("§c❌ Không tìm thấy người chơi §e" + targetName);
                    }
                }
                return true;
            }
            sender.sendMessage("§cLệnh phụ không hợp lệ. Gõ /vnhelp để xem hướng dẫn.");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("vnboss") && args.length == 1
                && sender.hasPermission("vnmine.hardcore.admin")) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (String id : bossEventManager.getEnabledBossIds()) {
                if (id.toLowerCase(Locale.ROOT).startsWith(partial)) matches.add(id);
            }
            return matches;
        }
        return List.of();
    }

    public static VnMineHardcore getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public LogManager getLogManager() { return logManager; }
    public ThirstManager getThirstManager() { return thirstManager; }
    public DisasterManager getDisasterManager() { return disasterManager; }
    public DeathPenaltyManager getDeathPenaltyManager() { return deathPenaltyManager; }
    public BossEventManager getBossEventManager() { return bossEventManager; }
    public GraveManager getGraveManager() { return graveManager; }

    private void clearLegacyPluginBans() {
        int cleared = 0;
        for (BanList.Type type : new BanList.Type[] {BanList.Type.NAME, BanList.Type.IP}) {
            BanList banList = Bukkit.getBanList(type);
            java.util.Set<String> legacyTargets = new java.util.HashSet<>();
            for (Object entryObject : banList.getBanEntries()) {
                if (entryObject instanceof org.bukkit.BanEntry entry &&
                    "VnMineHardcore".equals(entry.getSource())) {
                    legacyTargets.add(entry.getTarget());
                }
            }
            for (String target : legacyTargets) {
                banList.pardon(target);
                cleared++;
            }
        }
        if (cleared > 0) {
            getLogger().info("[Death] Removed " + cleared + " legacy plugin bans; deaths no longer ban players.");
        }
    }
}
