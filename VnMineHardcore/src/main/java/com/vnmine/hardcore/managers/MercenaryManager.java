package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.TippedArrow;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpellCastEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.entity.Spellcaster;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class MercenaryManager implements CommandExecutor, TabCompleter, Listener {
    private static final String COMMAND_PERMISSION = "vnmine.command.army";
    private static final String ADMIN_PERMISSION = "vnmine.command.army.admin";
    private static final List<org.bukkit.inventory.EquipmentSlot> EQUIPMENT_SLOTS = List.of(
        org.bukkit.inventory.EquipmentSlot.HAND,
        org.bukkit.inventory.EquipmentSlot.HEAD,
        org.bukkit.inventory.EquipmentSlot.CHEST,
        org.bukkit.inventory.EquipmentSlot.LEGS,
        org.bukkit.inventory.EquipmentSlot.FEET,
        org.bukkit.inventory.EquipmentSlot.OFF_HAND);
    private static final String[] COMMANDS = {
        "list", "gui", "hire", "recall", "rest", "attack", "defend", "protect", "gear", "give", "token",
        "remove", "clear", "mute", "rename"
    };
    private final VnMineHardcore plugin;
    private final File configFile;
    private final File dataFile;
    private final File torchFile;
    private final NamespacedKey unitKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey horseMountKey;
    private final NamespacedKey summonedMinionKey;
    private final NamespacedKey tokenClassKey;
    private final NamespacedKey tokenLevelKey;
    private final NamespacedKey welcomeGiftKey;
    private final NamespacedKey illuminationArrowKey;
    private final File welcomeGiftFile;
    private final Set<UUID> claimedWelcomeGiftPlayers = new HashSet<>();
    private final Map<UUID, TemporaryTorch> temporaryTorches = new HashMap<>();
    private final Map<UUID, BukkitTask> torchRemovalTasks = new HashMap<>();
    private final MercenaryNamePacketListener namePacketListener;
    private final MercenaryStatusPacketListener statusPacketListener;
    private final Map<UUID, Unit> units = new HashMap<>();
    private final Map<UUID, Mob> activeEntities = new HashMap<>();
    private final Map<UUID, Map<String, Long>> skillCooldowns = new HashMap<>();
    private final Map<UUID, Set<UUID>> summonedMinions = new HashMap<>();
    private final Map<UUID, UUID> ownerAttackTargets = new HashMap<>();
    private final Map<UUID, Long> ownerAttackTargetExpiries = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> ownerThreats = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> ownerAttackers = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> unitThreats = new HashMap<>();
    private final Map<UUID, UUID> mountByUnit = new HashMap<>();
    private final Map<UUID, Location> wanderTargets = new HashMap<>();
    private final Map<UUID, Long> wanderTargetExpiries = new HashMap<>();
    private final Set<UUID> awaitingGroupName = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private YamlConfiguration settings;
    private YamlConfiguration data;
    private boolean enabled;
    private boolean ownerCommandedPvpEnabled;
    private int maxUnits;
    private int maxDeployedUnits;
    private double followDistance;
    private double teleportDistance;
    private double targetRadius;
    private double wanderRadius;
    private boolean requireLineOfSight;
    private int aiIntervalTicks;
    private double healAmount;
    private double healRadius;
    private String ownerBuff;
    private int ownerBuffTicks;
    private double shieldReduction;
    private int experiencePerKill;
    private int experiencePerLevel;
    private int maxLevel;
    private int recoverySeconds;
    private double skillPowerPerLevel;
    private double maxSkillPowerMultiplier;
    private double skillCooldownReductionAtMaxLevel;
    private double foodHealAmount;
    private boolean experienceOrbPickupEnabled;
    private double experienceOrbPickupRadius;
    private boolean statusDisplayEnabled;
    private boolean statusDisplaySeeThrough;
    private int statusDisplayBarLength;
    private ChatColor vexHealthColor;
    private String vexAttackIcon;
    private String vexFollowIcon;
    private String vexIdleIcon;
    private BukkitRunnable aiTask;

    private static final class TemporaryTorch {
        private final UUID id;
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final long expiresAt;

        private TemporaryTorch(UUID id, String worldName, int x, int y, int z, long expiresAt) {
            this.id = id;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.expiresAt = expiresAt;
        }
    }

    private enum Mode {
        ATTACK, DEFEND, PROTECT
    }

    @EventHandler
    public void onOwnerDamage(EntityDamageByEntityEvent event) {
        applyOwnerShield(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOwnerExperienceOrbPickup(PlayerExpChangeEvent event) {
        if (!experienceOrbPickupEnabled) return;
        Player owner = event.getPlayer();
        double radiusSquared = experienceOrbPickupRadius * experienceOrbPickupRadius;
        boolean mercenaryNearby = getOwnerUnits(owner.getUniqueId()).stream()
            .filter(unit -> unit.deployed && unit.recoveryUntilMillis <= System.currentTimeMillis())
            .map(unit -> activeEntities.get(unit.id))
            .filter(mob -> mob != null && mob.isValid() && mob.getWorld().equals(owner.getWorld()))
            .anyMatch(mob -> mob.getLocation().distanceSquared(owner.getLocation()) <= radiusSquared);
        int experience = event.getAmount();
        if (!mercenaryNearby || experience <= 0) return;
        event.setAmount(0);
        owner.giveExp(experience);
    }

    private static final class ArmyMenuHolder implements InventoryHolder {
        private final UUID owner;
        private final int page;
        private final List<UUID> unitIds;
        private Inventory inventory;

        private ArmyMenuHolder(UUID owner, int page, List<UUID> unitIds) {
            this.owner = owner;
            this.page = page;
            this.unitIds = unitIds;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class UnitMenuHolder implements InventoryHolder {
        private final UUID owner;
        private final UUID unit;
        private Inventory inventory;

        private UnitMenuHolder(UUID owner, UUID unit) {
            this.owner = owner;
            this.unit = unit;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private void despawnAllForReload() {
        for (UUID ownerId : units.values().stream().map(unit -> unit.owner).distinct().toList()) {
            despawnForOwner(ownerId);
        }
    }

    private static final class Unit {
        private final UUID id;
        private final UUID owner;
        private final String unitClass;
        private int originalTier;
        private int tier;
        private int level = 1;
        private int experience;
        private String customName = "";
        private long recoveryUntilMillis;
        private boolean gearCustomized;
        private Mode mode;
        private boolean deployed;
        private boolean muted;
        private boolean horseDead;
        private UUID entityId;
        private String worldName;
        private double x;
        private double y;
        private double z;
        private final EnumMap<org.bukkit.inventory.EquipmentSlot, ItemStack> equipment =
            new EnumMap<>(org.bukkit.inventory.EquipmentSlot.class);
        private final ItemStack[] inventory = new ItemStack[36];
        private ItemStack horseArmor;

        private Unit(UUID id, UUID owner, String unitClass, int level, Mode mode, boolean deployed) {
            this.id = id;
            this.owner = owner;
            this.unitClass = unitClass;
            this.originalTier = level;
            this.tier = level;
            this.mode = mode;
            this.deployed = deployed;
        }
    }

    private static final class GearHolder implements InventoryHolder {
        private final UUID owner;
        private final UUID unit;
        private Inventory inventory;

        private GearHolder(UUID owner, UUID unit) {
            this.owner = owner;
            this.unit = unit;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private enum MenuType {
        MAIN, ADMIN, ADMIN_PLAYERS, GIVE_TYPE, CLASS_SELECT, TIER_SELECT, WELCOME_GIFT,
        DISASTER_SELECT, BOSS_SELECT, BOSS_LEVEL_SELECT, ADMIN_TIME, TIME_DURATION,
        ADMIN_DAY, ADMIN_PROTECT, PROTECTION_REMOVE, TELEPORT_PLAYERS, TELEPORT_WORLDS,
        ADMIN_USER_PLAYERS, ADMIN_USER_ACTION, ADMIN_GROUPS, ADMIN_GROUP_SELECT,
        ADMIN_GROUP_DELETE_CONFIRM, ADMIN_PERMISSION_SELECT, ADMIN_USER_GROUP_SELECT,
        ADMIN_USER_PERMISSION_ACTIONS, ADMIN_USER_PERMISSION_SELECT,
        ADMIN_SCOREBOARD_CONFIRM, ADMIN_DEATH_PLAYERS, ADMIN_SPAWN_WORLD, UNIT_DELETE_CONFIRM
    }

    private static final class ServiceMenuHolder implements InventoryHolder {
        private final UUID viewer;
        private final MenuType type;
        private final UUID target;
        private final String itemType;
        private final String classId;
        private final int page;
        private final Map<Integer, String> choices = new HashMap<>();
        private Inventory inventory;

        private ServiceMenuHolder(UUID viewer, MenuType type, UUID target, String itemType,
                                  String classId, int page) {
            this.viewer = viewer;
            this.type = type;
            this.target = target;
            this.itemType = itemType;
            this.classId = classId;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public MercenaryManager(VnMineHardcore plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "armies.yml");
        dataFile = new File(plugin.getDataFolder(), "mercenaries.yml");
        torchFile = new File(plugin.getDataFolder(), "ranger-torches.yml");
        welcomeGiftFile = new File(plugin.getDataFolder(), "new-player-gifts.yml");
        unitKey = new NamespacedKey(plugin, "army_unit");
        ownerKey = new NamespacedKey(plugin, "army_owner");
        horseMountKey = new NamespacedKey(plugin, "army_horse_mount");
        summonedMinionKey = new NamespacedKey(plugin, "army_summoned_minion");
        tokenClassKey = new NamespacedKey(plugin, "army_token_class");
        tokenLevelKey = new NamespacedKey(plugin, "army_token_level");
        welcomeGiftKey = new NamespacedKey(plugin, "welcome_gift");
        illuminationArrowKey = new NamespacedKey(plugin, "ranger_illumination_arrow");
        namePacketListener = new MercenaryNamePacketListener(plugin);
        statusPacketListener = new MercenaryStatusPacketListener(plugin);
        loadSettings();
        loadData();
        loadWelcomeGiftClaims();
        loadTemporaryTorches();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startAiTask();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTask(plugin, () -> restoreDeployedUnits(player));
        }
        plugin.getLogger().info("[Army] Loaded " + units.size() + " mercenary record(s).");
    }

    public boolean reload() {
        try {
            loadSettingsFromDisk();
            if (!enabled) despawnAllForReload();
            if (aiTask != null) {
                aiTask.cancel();
                aiTask = null;
            }

            startAiTask();
            if (enabled) {
                for (Player player : Bukkit.getOnlinePlayers()) restoreDeployedUnits(player);
            }
            return true;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.SEVERE, "[Army] Could not reload armies.yml; keeping old settings.",
                exception);
            return false;
        }
    }

    public int awardBossExperience(UUID ownerId, int amount) {
        if (amount <= 0) return 0;
        int recipients = 0;
        for (Unit unit : getOwnerUnits(ownerId)) {
            if (!unit.deployed || unit.recoveryUntilMillis > System.currentTimeMillis()) continue;
            if (addExperience(unit, amount)) recipients++;
        }
        if (recipients > 0 && !saveData()) {
            plugin.getLogger().severe("[Army] Could not save boss XP for owner " + ownerId + ".");
        }
        return recipients;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("vnmenu")) {
            if (sender instanceof Player player) openMainMenu(player);
            else sender.sendMessage("§cMenu chỉ dành cho người chơi.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("quatanthu")) {
            return handleWelcomeGiftCommand(sender, args);
        }
        if (!sender.hasPermission(COMMAND_PERMISSION)) {
            sender.sendMessage("§cBạn không có quyền dùng /vnarmy.");
            return true;
        }
        if (!enabled) {
            sender.sendMessage("§cHệ thống lính đánh thuê hiện đang tắt.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("token")) return handleTokenCommand(sender, args);
        if (args[0].equalsIgnoreCase("give")) return handleGiveCommand(sender, args);
        if (args[0].equalsIgnoreCase("remove")) return handleRemoveCommand(sender, args);
        if (args[0].equalsIgnoreCase("clear")) return handleClearCommand(sender, args);
        if (args[0].equalsIgnoreCase("list") && args.length == 2 && sender.hasPermission(ADMIN_PERMISSION)) {
            listUnits(sender, args[1]);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh này chỉ dành cho người chơi.");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> listUnits(player);
            case "gui" -> openArmyMenu(player, 0);
            case "hire" -> hire(player, args);
            case "recall" -> recall(player, args);
            case "rest" -> rest(player, args);
            case "attack" -> setMode(player, Mode.ATTACK);
            case "defend" -> setMode(player, Mode.DEFEND);
            case "protect" -> setMode(player, Mode.PROTECT);
            case "gear" -> openGear(player, args);
            case "mute" -> toggleMuteCommand(player, args);
            case "rename" -> rename(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(COMMAND_PERMISSION)) return List.of();
        if (args.length == 1) return matching(args[0], COMMANDS);
        if (args.length == 2 && (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("clear"))
                && sender.hasPermission(ADMIN_PERMISSION)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove") && sender instanceof Player player) {
            List<String> ids = getOwnerUnits(player.getUniqueId()).stream().map(unit -> shortId(unit.id)).toList();
            if (!sender.hasPermission(ADMIN_PERMISSION)) return ids;
            return java.util.stream.Stream.concat(Bukkit.getOnlinePlayers().stream().map(Player::getName),
                ids.stream()).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("remove")
                && sender.hasPermission(ADMIN_PERMISSION)) {
            Player owner = Bukkit.getPlayerExact(args[1]);
            return owner == null ? List.of() : getOwnerUnits(owner.getUniqueId()).stream()
                .map(unit -> shortId(unit.id)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mute") && sender instanceof Player player) {
            return getOwnerUnits(player.getUniqueId()).stream().map(unit -> shortId(unit.id)).toList();
        }
        if (args.length == 2 && Set.of("rename", "recall", "rest")
                .contains(args[0].toLowerCase(Locale.ROOT)) && sender instanceof Player player) {
            return getOwnerUnits(player.getUniqueId()).stream().map(unit -> shortId(unit.id)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")
                && sender.hasPermission(ADMIN_PERMISSION)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")
                && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(args[2], "token", "army");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")
                && sender.hasPermission(ADMIN_PERMISSION) && args[2].equalsIgnoreCase("army")) {
            return matching(args[3], getClassIds().toArray(String[]::new));
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("give")
                && sender.hasPermission(ADMIN_PERMISSION) && args[2].equalsIgnoreCase("army")) {
            return matching(args[4], getTierIds().toArray(String[]::new));
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")
                && sender.hasPermission(ADMIN_PERMISSION) && args[2].equalsIgnoreCase("token")) {
            return matching(args[3], getTierIds().toArray(String[]::new));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("hire")) {
            return matching(args[1], getClassIds().toArray(String[]::new));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("hire")) {
            return matching(args[2], getTierIds().toArray(String[]::new));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("token")
                && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(args[1], "give");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("token")
                && args[1].equalsIgnoreCase("give") && sender.hasPermission(ADMIN_PERMISSION)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("token")
                && args[1].equalsIgnoreCase("give") && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(args[3], getClassIds().toArray(String[]::new));
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("token")
                && sender.hasPermission(ADMIN_PERMISSION)) {
            return matching(args[4], getTierIds().toArray(String[]::new));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("gear") && sender instanceof Player player) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return getOwnerUnits(player.getUniqueId()).stream()
                .map(unit -> shortId(unit.id))
                .filter(id -> id.startsWith(prefix))
                .toList();
        }
        return List.of();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> restoreDeployedUnits(event.getPlayer()), 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onWelcomeGiftHold(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isWelcomeGiftClaimed(player.getUniqueId())) return;
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (!isWelcomeGiftBag(newItem)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || isWelcomeGiftClaimed(player.getUniqueId())) return;
            if (isWelcomeGiftBag(player.getInventory().getItemInMainHand())) {
                openWelcomeGiftMenu(player);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onWelcomeGiftRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (!isWelcomeGiftBag(item)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (isWelcomeGiftClaimed(player.getUniqueId())) {
            player.sendMessage("§cBạn đã nhận quà tân thủ rồi.");
            return;
        }
        openWelcomeGiftMenu(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpenMainMenu(PlayerSwapHandItemsEvent event) {
        if (!plugin.getConfig().getBoolean("menus.main.enabled", true)
                || !plugin.getConfig().getBoolean("menus.main.intercept-f-key", true)) return;
        event.setCancelled(true);
        openMainMenu(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onShiftRightClickMercenary(PlayerInteractEntityEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Mob mob) || !isMercenary(mob)) return;
        UUID ownerId = getOwnerId(mob);
        if (ownerId == null || !ownerId.equals(event.getPlayer().getUniqueId())) return;
        Unit unit = units.get(getUnitId(mob));
        if (unit == null) return;
        event.setCancelled(true);
        openGear(event.getPlayer(), unit);
    }

    @EventHandler
    public void onAdminGroupNameInput(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!awaitingGroupName.remove(playerId)) return;
        event.setCancelled(true);
        String groupName = event.getMessage().trim().toLowerCase(Locale.ROOT);
        Player player = event.getPlayer();
        if (!groupName.matches("[a-z0-9_-]{1,32}")) {
            Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage("§cTên nhóm chỉ gồm chữ thường, số, _ hoặc - (1-32 ký tự)."));
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) player.performCommand("vngroup create " + groupName);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ownerAttackTargets.remove(event.getPlayer().getUniqueId());
        ownerAttackTargetExpiries.remove(event.getPlayer().getUniqueId());
        awaitingGroupName.remove(event.getPlayer().getUniqueId());
        despawnForOwner(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMercenaryDeath(EntityDeathEvent event) {
        String idValue = event.getEntity().getPersistentDataContainer().get(unitKey, PersistentDataType.STRING);
        if (idValue == null) return;
        namePacketListener.remove(event.getEntity());
        UUID id;
        try {
            id = UUID.fromString(idValue);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("[Army] Ignoring invalid unit UUID on entity " + event.getEntity().getUniqueId());
            return;
        }
        Unit unit = units.get(id);
        removeStatusDisplay(id);
        activeEntities.remove(id);
        skillCooldowns.remove(id);
        cleanupSummonedMinions(id);
        if (unit != null) {
            removeMount(id, event.getEntity() instanceof Mob deadMob ? deadMob : null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            unit.deployed = false;
            unit.entityId = null;
            unit.recoveryUntilMillis = System.currentTimeMillis() + recoverySeconds * 1000L;
            Player owner = Bukkit.getPlayer(unit.owner);
            if (owner != null) owner.sendMessage("§cLính đánh thuê §e" + displayName(unit)
                + " §cđã bị hạ và đang hồi phục trong §e" + recoverySeconds + " giây§c. Trang bị được bảo toàn.");
            if (!saveData()) {
                plugin.getLogger().severe("[Army] Could not persist recovery state for mercenary " + id + ".");
            }
        }
    }

    @EventHandler
    public void onMercenaryHorseDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) return;
        String unitIdValue = horse.getPersistentDataContainer()
            .get(horseMountKey, PersistentDataType.STRING);
        if (unitIdValue == null) return;
        UUID unitId;
        try {
            unitId = UUID.fromString(unitIdValue);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("[Army] Ignoring invalid horse unit UUID on entity " + horse.getUniqueId());
            return;
        }
        Unit unit = units.get(unitId);
        if (unit == null || !"paladin".equals(unit.unitClass)) return;
        unit.horseDead = true;
        mountByUnit.remove(unitId, horse.getUniqueId());
        event.getDrops().clear();
        event.setDroppedExp(0);
        Player owner = Bukkit.getPlayer(unit.owner);
        if (owner != null) {
            owner.sendMessage("§eNgựa của §6" + displayName(unit)
                + " §eđã chết. Kỵ sĩ sẽ tiếp tục đi bộ; hãy cho lính nghỉ rồi xuất chiến lại để có ngựa mới.");
        }
        if (!saveData()) {
            plugin.getLogger().severe("[Army] Could not persist dead horse state for mercenary " + unitId + ".");
        }
    }

    @EventHandler
    public void onMercenaryEntityRemove(EntityRemoveEvent event) {
        if (isMercenary(event.getEntity())) namePacketListener.remove(event.getEntity());
        UUID unitId = getUnitId(event.getEntity());
        if (unitId != null) removeStatusDisplay(unitId);
    }

    @EventHandler
    public void onMercenaryEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (isMercenary(entity)) namePacketListener.remove(entity);
            UUID unitId = getUnitId(entity);
            if (unitId != null) removeStatusDisplay(unitId);
        }
    }

    @EventHandler
    public void onMercenaryEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof Mob mob) || !isMercenary(mob)) continue;
            String idValue = mob.getPersistentDataContainer().get(unitKey, PersistentDataType.STRING);
            try {
                Unit unit = idValue == null ? null : units.get(UUID.fromString(idValue));
                if (unit != null && unit.deployed) {
                    preventZombification(mob);
                    activeEntities.put(unit.id, mob);
                    unit.entityId = mob.getUniqueId();
                    repairEquipmentFromSaved(unit, mob);
                    updateEntityName(unit, mob);
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("[Army] Ignoring invalid unit UUID on entity " + mob.getUniqueId());
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMercenaryEnvironmentDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                && isMercenaryMount(event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        if (!isMercenary(event.getEntity())) return;
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            event.setCancelled(true);
            return;
        }
        Entity attacker = byEntity.getDamager();
        if (attacker instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) attacker = shooter;
        if (!(attacker instanceof LivingEntity) || isMercenary(attacker)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMercenarySunCombust(EntityCombustEvent event) {
        if (event.getClass() != EntityCombustEvent.class) return;
        if (isMercenary(event.getEntity()) || isMercenaryMount(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMercenaryKillExperience(EntityDeathEvent event) {
        Entity source = event.getDamageSource().getCausingEntity();
        if (source instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) source = shooter;
        if (source == null) return;
        boolean mercenaryKill = isMercenary(source) || isSummonedMinion(source);
        UUID ownerId = null;
        String unitId = source.getPersistentDataContainer().get(unitKey, PersistentDataType.STRING);
        if (unitId != null) {
            try {
                Unit killer = units.get(UUID.fromString(unitId));
                if (killer != null) ownerId = killer.owner;
            } catch (IllegalArgumentException ignored) {
                return;
            }
        } else {
            ownerId = getOwnerId(source);
            if (ownerId == null && source instanceof Player player) ownerId = player.getUniqueId();
        }
        if (ownerId == null) return;
        if (mercenaryKill) {
            Player owner = Bukkit.getPlayer(ownerId);
            int droppedExperience = event.getDroppedExp();
            if (owner != null && owner.isOnline() && droppedExperience > 0) {
                owner.giveExp(droppedExperience);
                event.setDroppedExp(0);
            }
        }
        if (experiencePerKill <= 0) return;
        List<Unit> recipients = getOwnerUnits(ownerId).stream()
            .filter(unit -> unit.deployed && unit.recoveryUntilMillis <= System.currentTimeMillis())
            .sorted(Comparator.comparing(unit -> unit.id.toString()))
            .toList();
        if (recipients.isEmpty()) return;
        int baseShare = experiencePerKill / recipients.size();
        int remainder = experiencePerKill % recipients.size();
        boolean changed = false;
        for (int index = 0; index < recipients.size(); index++) {
            Unit unit = recipients.get(index);
            int share = baseShare + (index < remainder ? 1 : 0);
            if (share <= 0) continue;
            changed |= addExperience(unit, share);
        }
        if (changed && !saveData()) {
            plugin.getLogger().warning("[Army] Could not save kill XP for owner " + ownerId + ".");
        }
    }

    @EventHandler
    public void onMercenaryTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !isMercenary(mob)) return;
        if (isSummonedMinion(mob)) {
            if (!canMinionTarget(mob, event.getTarget())) {
                event.setCancelled(true);
                mob.setTarget(null);
            }
            return;
        }
        LivingEntity target = event.getTarget();
        if (target == null) return;
        if (mob instanceof Villager) {
            event.setCancelled(true);
            mob.setTarget(null);
            return;
        }
        UUID ownerId = getOwnerId(mob);
        boolean ownerCommandedTarget = ownerId != null
            && isActiveOwnerAttackTarget(ownerId, target.getUniqueId());
        if (target.getUniqueId().equals(ownerId)
                || !isAllowedMercenaryTarget(ownerId, target, ownerCommandedTarget)
                || (requireLineOfSight && !mob.hasLineOfSight(target))
                || isHiddenUndergroundTarget(mob, target)) {
            event.setCancelled(true);
            mob.setTarget(null);
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMercenarySpellCast(EntitySpellCastEvent event) {
        if (event.getEntity() instanceof Evoker evoker && isMercenary(evoker)
                && event.getSpell() == Spellcaster.Spell.SUMMON_VEX) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMercenaryTransform(EntityTransformEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Mob mob) || !isMercenary(mob)) return;
        boolean converting = false;
        String conversionTime = "n/a";
        boolean immune = false;
        if (mob instanceof PiglinAbstract piglin) {
            immune = piglin.isImmuneToZombification();
            if (piglin.isConverting()) {
                converting = true;
                conversionTime = Integer.toString(piglin.getConversionTime());
            }
        }
        List<String> equipmentBefore = occupiedEquipmentSlots(mob);
        event.setCancelled(true);
        preventZombification(mob);
        plugin.getLogger().warning("[Army] Cancelled transform for mercenary " + mob.getUniqueId()
            + " (reason=" + event.getTransformReason() + ", converting=" + converting
            + ", conversionTicks=" + conversionTime + ", immuneBefore=" + immune
            + ", equipmentBefore=" + equipmentBefore + ").");

        UUID unitId = getUnitId(mob);
        Unit unit = unitId == null ? null : units.get(unitId);
        if (unit != null) {
            reconcileEquipment(unit, mob, false);
            if (!saveData()) {
                plugin.getLogger().warning("[Army] Could not save gear after cancelling transform for " + unit.id);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!mob.isValid() || !isUnit(mob, unit.id)) return;
                preventZombification(mob);
                repairEquipmentFromSaved(unit, mob);
                if (!saveData()) {
                    plugin.getLogger().warning("[Army] Could not save restored gear for mercenary " + unit.id + ".");
                }
            });
        }
    }

    private void repairEquipmentFromSaved(Unit unit, Mob mob) {
        List<String> restored = reconcileEquipment(unit, mob, true);
        if (!restored.isEmpty()) {
            plugin.getLogger().warning("[Army] Restored missing equipment slots " + restored
                + " for mercenary " + unit.id + ".");
        }
    }

    private List<String> reconcileEquipment(Unit unit, Mob mob, boolean restoreMissing) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) return List.of();
        List<String> restored = new ArrayList<>();
        for (org.bukkit.inventory.EquipmentSlot slot : EQUIPMENT_SLOTS) {
            ItemStack item = equipment.getItem(slot);
            ItemStack saved = unit.equipment.get(slot);
            if (restoreMissing && (item == null || item.getType().isAir())
                    && saved != null && !saved.getType().isAir()) {
                equipment.setItem(slot, saved.clone());
                restored.add(slot.name());
                item = equipment.getItem(slot);
            }
            if (item != null && !item.getType().isAir()) {
                unit.equipment.put(slot, item.clone());
            }
        }
        return restored;
    }

    private List<String> occupiedEquipmentSlots(Mob mob) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) return List.of();
        List<String> slots = new ArrayList<>();
        for (org.bukkit.inventory.EquipmentSlot slot : EQUIPMENT_SLOTS) {
            ItemStack item = equipment.getItem(slot);
            if (item != null && !item.getType().isAir()) slots.add(slot.name());
        }
        return slots;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSummonedMinionTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !isSummonedMinion(mob)) return;
        if (!canMinionTarget(mob, event.getTarget())) {
            event.setCancelled(true);
            mob.setTarget(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMercenaryFriendlyFire(EntityDamageByEntityEvent event) {
        Entity source = resolveDamageSource(event.getDamager());
        if (!isMercenary(source)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!canMercenaryDamageTarget(getOwnerId(source), target)) event.setCancelled(true);
    }

    @EventHandler
    public void onMercenaryItemDamage(EntityDamageItemEvent event) {
        if (isMercenary(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onMercenaryCombat(EntityDamageByEntityEvent event) {
        Entity source = resolveDamageSource(event.getDamager());
        if (isMercenary(source) && event.getEntity() instanceof LivingEntity victim
                && !canMercenaryDamageTarget(getOwnerId(source), victim)) {
            event.setCancelled(true);
            return;
        }
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Player player) {
            registerOwnerThreat(player.getUniqueId(), source);
            registerThreat(ownerAttackers, player.getUniqueId(), source);
        }
        if (event.getEntity() instanceof LivingEntity victim && !(victim instanceof Player)) {
            UUID victimOwner = getOwnerId(victim);
            if (victimOwner != null) {
                registerOwnerThreat(victimOwner, source);
            }
            UUID victimUnitId = getUnitId(victim);
            if (victimUnitId != null) registerThreat(unitThreats, victimUnitId, source);
        }
        UUID attackingOwner = source instanceof LivingEntity living ? getOwnerId(living) : null;
        if (attackingOwner != null && event.getEntity() instanceof LivingEntity victim) {
            Player owner = Bukkit.getPlayer(attackingOwner);
            if (victim.getUniqueId().equals(attackingOwner)) {
                event.setCancelled(true);
                return;
            }
            if (owner != null && owner.isOnline()) victim.setKiller(owner);
        }
        if (isMercenary(event.getEntity())) {
            UUID ownerId = getOwnerId(event.getEntity());
            Entity attacker = event.getDamager();
            if (attacker instanceof org.bukkit.entity.Projectile projectile
                    && projectile.getShooter() instanceof Entity shooter) attacker = shooter;
                if (ownerId != null && (attacker.getUniqueId().equals(ownerId)
                    || (isMercenary(attacker) && ownerId.equals(getOwnerId(attacker))))) {
                event.setCancelled(true);
                return;
            }
        }
        if (!(source instanceof Player owner) || !(event.getEntity() instanceof LivingEntity target)
                || target.getUniqueId().equals(owner.getUniqueId())) return;
        UUID targetOwner = getOwnerId(target);
        if (targetOwner != null && targetOwner.equals(owner.getUniqueId())) return;
        ownerAttackTargets.put(owner.getUniqueId(), target.getUniqueId());
        ownerAttackTargetExpiries.put(owner.getUniqueId(), System.currentTimeMillis() + 15_000L);
        for (Unit unit : getOwnerUnits(owner.getUniqueId())) {
            Mob mob = activeEntities.get(unit.id);
            if (unit.deployed && mob != null && mob.isValid()
                    && ((isValidTarget(target) && !isMercenary(target)) || isMercenary(target))
                    && isAllowedMercenaryTarget(unit.owner, target, true)) {
                mob.setTarget(target);
            }
        }
    }

    @EventHandler
    public void onGearInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GearHolder holder)
                || !(event.getPlayer() instanceof Player player)
                || !holder.owner.equals(player.getUniqueId())) return;
        Unit unit = units.get(holder.unit);
        if (unit == null || !unit.owner.equals(player.getUniqueId())) return;
        Inventory inventory = event.getInventory();
        EnumMap<org.bukkit.inventory.EquipmentSlot, ItemStack> previousEquipment =
            new EnumMap<>(org.bukkit.inventory.EquipmentSlot.class);
        unit.equipment.forEach((slot, item) -> previousEquipment.put(slot, cloneOrNull(item)));
        ItemStack[] previousInventory = new ItemStack[unit.inventory.length];
        for (int slot = 0; slot < unit.inventory.length; slot++) {
            previousInventory[slot] = cloneOrNull(unit.inventory[slot]);
        }
        boolean previousCustomized = unit.gearCustomized;
        ItemStack previousHorseArmor = cloneOrNull(unit.horseArmor);
        captureGearInventory(player, unit, inventory);
        unit.gearCustomized = true;
        Mob mob = activeEntities.get(unit.id);
        if (mob != null && mob.isValid()) applyEquipment(unit, mob);
        applyHorseArmorToMount(unit);
        if (!saveData()) {
            unit.equipment.clear();
            unit.equipment.putAll(previousEquipment);
            System.arraycopy(previousInventory, 0, unit.inventory, 0, unit.inventory.length);
            unit.gearCustomized = previousCustomized;
            unit.horseArmor = previousHorseArmor;
            if (mob != null && mob.isValid()) applyEquipment(unit, mob);
            applyHorseArmorToMount(unit);
            plugin.getLogger().severe("[Army] Could not save gear changes for mercenary " + unit.id + ".");
            player.sendMessage("§cKhông thể lưu trang bị lính vào mercenaries.yml; giữ lính hiện tại và thử lưu lại.");
            return;
        }
        player.sendMessage("§aĐã lưu trang bị cho lính §e#" + shortId(unit.id) + "§a.");
    }

    @EventHandler
    public void onGearInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GearHolder)) return;
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if ((slot >= 36 && slot < 45) || slot > 51) event.setCancelled(true);
    }

    @EventHandler
    public void onGearInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GearHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> (slot >= 36 && slot < 45) || slot > 51)) {
            event.setCancelled(true);
        }
    }

    private void loadSettings() {
        if (!configFile.isFile()) {
            if (!plugin.getDataFolder().isDirectory() && !plugin.getDataFolder().mkdirs()) {
                throw new IllegalStateException("Could not create plugin data directory for armies.yml.");
            }
            plugin.saveResource("armies.yml", false);
        }
        try {
            loadSettingsFromDisk();
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Could not load armies.yml.", exception);
        }
    }

    private void loadSettingsFromDisk() throws IOException, InvalidConfigurationException {
        YamlConfiguration loaded = new YamlConfiguration();
        loaded.options().parseComments(true);
        loaded.load(configFile);
        mergeBundledDefaults(loaded);
        if (loaded.getConfigurationSection("classes") == null
                || loaded.getConfigurationSection("tiers") == null) {
            throw new InvalidConfigurationException("armies.yml must define classes and tiers.");
        }
        if (loaded.contains("enabled") && !loaded.isBoolean("enabled")) {
            throw new InvalidConfigurationException("enabled must be true or false.");
        }
        if (loaded.contains("pvp.allow-owner-commanded-targets")
                && !loaded.isBoolean("pvp.allow-owner-commanded-targets")) {
            throw new InvalidConfigurationException("pvp.allow-owner-commanded-targets must be true or false.");
        }
        if (loaded.getConfigurationSection("classes").getKeys(false).isEmpty()
                || loaded.getConfigurationSection("tiers").getKeys(false).isEmpty()) {
            throw new InvalidConfigurationException("armies.yml must define at least one class and one tier.");
        }
        int configuredMaxLevel = readBoundedInt(loaded, "progression.max-level", 30, 1, 30);
        for (String classId : loaded.getConfigurationSection("classes").getKeys(false)) {
            if (!classId.matches("[a-z0-9_-]{1,32}")) {
                throw new InvalidConfigurationException("Invalid mercenary class ID '" + classId + "'.");
            }
            Material menuIcon = Material.matchMaterial(
                loaded.getString("classes." + classId + ".menu-icon", "IRON_SWORD"));
            if (menuIcon == null || menuIcon.isAir()) {
                throw new InvalidConfigurationException("Invalid menu-icon for class '" + classId + "'.");
            }
            Material startingWeapon = Material.matchMaterial(
                loaded.getString("classes." + classId + ".starting-weapon", "IRON_SWORD"));
            String startingWeaponName = loaded.getString("classes." + classId + ".starting-weapon", "IRON_SWORD");
            boolean deferredSpear = "paladin".equals(classId)
                && "IRON_SPEAR".equalsIgnoreCase(startingWeaponName);
            if ((startingWeapon == null || startingWeapon.isAir()) && !deferredSpear) {
                throw new InvalidConfigurationException("Invalid starting-weapon for class '" + classId + "'.");
            }
            String skillsPath = "classes." + classId + ".skills";
            Object skillsValue = loaded.get(skillsPath);
            if (!(skillsValue instanceof List<?> configuredSkills)
                    || configuredSkills.stream().anyMatch(skill -> !(skill instanceof String))) {
                throw new InvalidConfigurationException(skillsPath + " must be a list of skill names.");
            }
            String entityName = loaded.getString("classes." + classId + ".entity-type", "");
            EntityType entityType = EntityType.fromName(entityName);
            Class<? extends Entity> entityClass = entityType == null ? null : entityType.getEntityClass();
            if (entityType == null || !entityType.isAlive()
                    || entityClass == null || !Mob.class.isAssignableFrom(entityClass)) {
                throw new InvalidConfigurationException("Entity type for class '" + classId + "' must be an AI mob.");
            }
            for (String skill : loaded.getStringList("classes." + classId + ".skills")) {
                if (!Set.of("ATTACK", "RANGED_ATTACK", "HEAL_OWNER", "HEAL_ALLIES",
                            "BUFF_OWNER", "BUFF_ALLIES", "SHIELD_OWNER", "VOLLEY",
                            "POWER_STRIKE", "TAUNT", "CLEANSE_OWNER", "FIRE_BOLT",
                            "ROOT_TARGET", "LIFESTEAL", "BERSERK", "SUMMON_VEXES", "LANCE_CHARGE",
                            "ILLUMINATION")
                        .contains(skill.toUpperCase(Locale.ROOT))) {
                    throw new InvalidConfigurationException(
                        "Unknown skill '" + skill + "' in class '" + classId + "'.");
                }
            }
            ConfigurationSection skillSettings =
                loaded.getConfigurationSection("classes." + classId + ".skill-settings");
            if (skillSettings != null) {
                for (String skill : skillSettings.getKeys(false)) {
                    if (!Set.of("ATTACK", "RANGED_ATTACK", "HEAL_OWNER", "HEAL_ALLIES",
                                "BUFF_OWNER", "BUFF_ALLIES", "SHIELD_OWNER", "VOLLEY",
                                "POWER_STRIKE", "TAUNT", "CLEANSE_OWNER", "FIRE_BOLT",
                                "ROOT_TARGET", "LIFESTEAL", "BERSERK", "SUMMON_VEXES", "LANCE_CHARGE",
                                "ILLUMINATION")
                            .contains(skill.toUpperCase(Locale.ROOT))) {
                        throw new InvalidConfigurationException(
                            "Unknown skill-settings entry '" + skill + "' in class '" + classId + "'.");
                    }
                    String target = loaded.getString("classes." + classId + ".skill-settings." + skill + ".target");
                    if (target != null && !Set.of("OWNER", "ALLIES", "OWNER_AND_ALLIES")
                            .contains(target.toUpperCase(Locale.ROOT))) {
                        throw new InvalidConfigurationException("Invalid target '" + target
                            + "' for " + classId + "." + skill + "; expected OWNER, ALLIES, or OWNER_AND_ALLIES.");
                    }
                    if (skill.equalsIgnoreCase("CLEANSE_OWNER")) {
                        for (String effect : loaded.getStringList("classes." + classId
                                + ".skill-settings." + skill + ".effects")) {
                            if (Registry.EFFECT.get(NamespacedKey.minecraft(
                                    effect.toLowerCase(Locale.ROOT))) == null) {
                                throw new InvalidConfigurationException("Unknown potion effect '" + effect
                                    + "' in " + classId + ".CLEANSE_OWNER.effects.");
                            }
                        }
                    }
                    if (skill.equalsIgnoreCase("ROOT_TARGET") || skill.equalsIgnoreCase("BERSERK")) {
                        String defaultEffect = skill.equalsIgnoreCase("ROOT_TARGET") ? "SLOWNESS" : "STRENGTH";
                        String effect = loaded.getString("classes." + classId
                            + ".skill-settings." + skill + ".effect", defaultEffect);
                        if (effect == null || effect.isBlank()) effect = defaultEffect;
                        if (Registry.EFFECT.get(NamespacedKey.minecraft(
                                effect.toLowerCase(Locale.ROOT))) == null) {
                            throw new InvalidConfigurationException("Unknown potion effect '" + effect
                                + "' in " + classId + "." + skill + ".effect.");
                        }
                    }
                }
            }
            double baseHealth = loaded.getDouble("classes." + classId + ".base-health", 24.0);
            double healthGrowth = loaded.getDouble("classes." + classId + ".health-per-level", 4.0);
            double baseDamage = loaded.getDouble("classes." + classId + ".base-damage", 4.0);
            double damageGrowth = loaded.getDouble("classes." + classId + ".damage-per-level", 0.5);
            double baseArmor = loaded.getDouble("classes." + classId + ".base-armor", 0.0);
            double armorGrowth = loaded.getDouble("classes." + classId + ".armor-per-level", 0.0);
            if (!Double.isFinite(baseHealth) || baseHealth <= 0.0
                    || !Double.isFinite(healthGrowth) || healthGrowth < 0.0
                    || !Double.isFinite(baseDamage) || baseDamage < 0.0
                    || !Double.isFinite(damageGrowth) || damageGrowth < 0.0
                    || !Double.isFinite(baseArmor) || baseArmor < 0.0
                    || !Double.isFinite(armorGrowth) || armorGrowth < 0.0
                    || baseHealth + healthGrowth * (configuredMaxLevel + 12.0) > 1024.0
                    || baseDamage + damageGrowth * (configuredMaxLevel + 12.0) > 1024.0
                    || baseArmor + armorGrowth * (configuredMaxLevel + 12.0) > 100.0) {
                throw new InvalidConfigurationException("Invalid stats for class '" + classId + "'.");
            }
            ConfigurationSection upgrades = loaded.getConfigurationSection(
                "classes." + classId + ".tier-upgrades");
            if (upgrades != null) {
                for (String targetTier : upgrades.getKeys(false)) {
                    int tierNumber = parsePositiveInt(targetTier);
                    ConfigurationSection costs = upgrades.getConfigurationSection(
                        targetTier + ".materials");
                    if (tierNumber < 2 || tierNumber > 13 || costs == null) {
                        throw new InvalidConfigurationException("Invalid tier upgrade cost for class '"
                            + classId + "' at tier " + targetTier + ".");
                    }
                    for (String materialName : costs.getKeys(false)) {
                        Material material = Material.matchMaterial(materialName);
                        int amount = costs.getInt(materialName, -1);
                        if (material == null || material.isAir() || amount < 0 || amount > 1_000_000) {
                            throw new InvalidConfigurationException("Invalid tier upgrade material '"
                                + materialName + "' for class '" + classId + "' at tier " + targetTier + ".");
                        }
                    }
                }
            }
        }
        Set<String> validAiActions = Set.of("OWNER_ATTACK_TARGET", "OWNER_ATTACKER", "OWNER_THREAT",
            "NEAREST_HOSTILE", "SEARCH", "FOLLOW_OWNER", "RETURN_TO_OWNER", "WANDER");
        for (String mode : List.of("attack", "defend", "protect")) {
            String path = "ai.priorities." + mode;
            List<String> priorities = loaded.getStringList(path);
            Set<String> normalized = priorities.stream()
                .map(value -> value.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
            if (priorities.isEmpty() || normalized.size() != priorities.size()
                    || !validAiActions.containsAll(normalized)) {
                throw new InvalidConfigurationException("Invalid AI priority list at '" + path + "'.");
            }
        }
        for (String tier : loaded.getConfigurationSection("tiers").getKeys(false)) {
            int level = parsePositiveInt(tier);
            int xp = loaded.getInt("tiers." + tier + ".experience-levels", -1);
            ConfigurationSection materials = loaded.getConfigurationSection("tiers." + tier + ".materials");
            if (level < 1 || level > 10 || xp < 0 || xp > 100_000 || materials == null) {
                throw new InvalidConfigurationException("Invalid costs in tier '" + tier + "'.");
            }
            for (String materialName : materials.getKeys(false)) {
                Material material = Material.matchMaterial(materialName);
                int amount = materials.getInt(materialName, -1);
                if (material == null || material.isAir() || amount < 0 || amount > 1_000_000) {
                    throw new InvalidConfigurationException(
                        "Invalid material cost '" + materialName + "' in tier '" + tier + "'.");
                }
            }
        }
        String configuredBuff = loaded.getString("skills.owner-buff", "SPEED");
        String resolvedBuff = configuredBuff == null || configuredBuff.isBlank()
            ? "SPEED" : configuredBuff.toUpperCase(Locale.ROOT);
        if (Registry.EFFECT.get(NamespacedKey.minecraft(resolvedBuff.toLowerCase(Locale.ROOT))) == null) {
            throw new InvalidConfigurationException("Unknown skills.owner-buff effect '" + resolvedBuff + "'.");
        }
        this.settings = loaded;
        this.enabled = loaded.getBoolean("enabled", true);
        this.ownerCommandedPvpEnabled =
            loaded.getBoolean("pvp.allow-owner-commanded-targets", true);
        this.maxUnits = readBoundedInt(loaded, "max-units-per-player", 10, 1, 100);
        this.maxDeployedUnits = readBoundedInt(loaded, "max-summoned-per-player", 3, 1, 10);
        this.followDistance = readBoundedDouble(loaded, "follow-distance-blocks", 8.0, 2.0, 64.0);
        this.teleportDistance = readBoundedDouble(loaded, "teleport-distance-blocks", 40.0, 8.0, 256.0);
        this.targetRadius = readBoundedDouble(loaded, "target-radius-blocks", 50.0, 4.0, 50.0);
        this.wanderRadius = readBoundedDouble(loaded, "ai.wander-radius-blocks", 6.0, 1.0, 32.0);
        this.requireLineOfSight = loaded.getBoolean("ai.require-line-of-sight", true);
        this.aiIntervalTicks = readBoundedInt(loaded, "ai-interval-ticks", 20, 10, 100);
        this.healAmount = readBoundedDouble(loaded, "skills.heal-amount", 4.0, 0.0, 100.0);
        this.healRadius = readBoundedDouble(loaded, "skills.heal-radius-blocks", 8.0, 1.0, 32.0);
        this.ownerBuff = resolvedBuff;
        this.ownerBuffTicks = readBoundedInt(loaded, "skills.owner-buff-seconds", 8, 1, 600) * 20;
        this.shieldReduction = readBoundedDouble(loaded, "skills.owner-damage-reduction", 0.2, 0.0, 0.9);
        this.experiencePerKill = readBoundedInt(loaded, "progression.experience-per-kill", 5, 0, 100_000);
        this.experiencePerLevel = readBoundedInt(loaded, "progression.experience-per-level", 100, 1, 100_000);
        this.skillPowerPerLevel = readBoundedDouble(loaded, "progression.skill-power-per-level", 0.05, 0.0, 1.0);
        this.maxSkillPowerMultiplier = readBoundedDouble(
            loaded, "progression.max-skill-power-multiplier", 2.5, 1.0, 5.0);
        this.skillCooldownReductionAtMaxLevel = readBoundedDouble(
            loaded, "progression.skill-cooldown-reduction-at-max-level-seconds", 3.0, 0.0, 10.0);
        this.recoverySeconds = readBoundedInt(loaded, "recovery.seconds", 300, 1, 86_400);
        this.foodHealAmount = readBoundedDouble(loaded, "gear.food-heal-amount", 6.0, 0.0, 100.0);
        this.experienceOrbPickupEnabled = loaded.getBoolean("experience-orb-pickup.enabled", true);
        this.experienceOrbPickupRadius = readBoundedDouble(
            loaded, "experience-orb-pickup.radius-blocks", 4.0, 1.0, 16.0);
        this.statusDisplayEnabled = loaded.getBoolean("status-display.enabled", true);
        this.statusDisplaySeeThrough = loaded.getBoolean("status-display.see-through", true);
        this.statusDisplayBarLength = readBoundedInt(loaded, "status-display.bar-length", 10, 5, 20);
        this.vexHealthColor = readVexHealthColor(loaded);
        this.vexAttackIcon = readVexActionIcon(loaded, "attack", "⚔");
        this.vexFollowIcon = readVexActionIcon(loaded, "follow", "🐾");
        this.vexIdleIcon = readVexActionIcon(loaded, "idle", "•");
        this.maxLevel = configuredMaxLevel;
        plugin.getLogger().info("[Army] Loaded " + getClassIds().size() + " class(es), enabled=" + enabled + ".");
    }

    private void mergeBundledDefaults(YamlConfiguration loaded)
            throws IOException, InvalidConfigurationException {
        boolean rangerIlluminationMigrationComplete =
            loaded.getBoolean("_migrations.ranger-illumination-skill", false);
        boolean rangerRangeTuningMigrationComplete =
            loaded.getBoolean("_migrations.ranger-range-and-damage-tuning", false);
        InputStream resource = plugin.getResource("armies.yml");
        if (resource == null) throw new IOException("Bundled armies.yml is missing.");
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.options().parseComments(true);
        try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            defaults.load(reader);
        }
        boolean changed = mergeSettingsSection(loaded, defaults, "");
        List<String> rangerSkills = new ArrayList<>(loaded.getStringList("classes.ranger.skills"));
        if (!rangerIlluminationMigrationComplete) {
            boolean addedIllumination = false;
            if (rangerSkills.stream().noneMatch(skill -> skill.equalsIgnoreCase("ILLUMINATION"))) {
                rangerSkills.add("ILLUMINATION");
                loaded.set("classes.ranger.skills", rangerSkills);
                loaded.setComments("classes.ranger.skills", defaults.getComments("classes.ranger.skills"));
                addedIllumination = true;
            }
            loaded.set("_migrations.ranger-illumination-skill", true);
            changed = true;
            if (addedIllumination) {
                plugin.getLogger().info("[Army] Enabled the new ILLUMINATION skill for the Ranger in armies.yml.");
            }
        }
        if (!rangerRangeTuningMigrationComplete) {
            migrateRangerDefault(loaded, defaults, "classes.ranger.base-damage", 4.0, 2.8);
            migrateRangerDefault(loaded, defaults, "classes.ranger.damage-per-level", 0.45, 0.35);
            migrateRangerDefault(loaded, defaults,
                "classes.ranger.skill-settings.RANGED_ATTACK.damage", 4.5, 4.2);
            migrateRangerDefault(loaded, defaults,
                "classes.ranger.skill-settings.VOLLEY.damage", 2.5, 2.1);
            loaded.set("_migrations.ranger-range-and-damage-tuning", true);
            changed = true;
        }
        if (loaded.getInt("max-units-per-player", 10) == 4) {
            loaded.set("max-units-per-player", 10);
            changed = true;
        }
        if (loaded.getInt("progression.max-level", 30) > 30) {
            loaded.set("progression.max-level", 30);
            changed = true;
        }
        if ("TRIDENT".equalsIgnoreCase(loaded.getString("classes.paladin.starting-weapon", ""))) {
            String weaponPath = "classes.paladin.starting-weapon";
            loaded.set(weaponPath, "IRON_SPEAR");
            loaded.setComments(weaponPath, defaults.getComments(weaponPath));
            changed = true;
        }
        String berserkerEntityPath = "classes.berserker.entity-type";
        if ("PIGLIN_BRUTE".equalsIgnoreCase(loaded.getString(berserkerEntityPath, ""))) {
            loaded.set(berserkerEntityPath, defaults.getString(berserkerEntityPath, "ZOMBIFIED_PIGLIN"));
            loaded.setComments(berserkerEntityPath, defaults.getComments(berserkerEntityPath));
            changed = true;
            plugin.getLogger().info("[Army] Migrated berserker entity to ZOMBIFIED_PIGLIN to prevent transformation.");
        }
        if (loaded.getStringList("classes.archer.skills").equals(List.of("RANGED_ATTACK"))) {
            loaded.set("classes.archer.skills", defaults.getStringList("classes.archer.skills"));
            changed = true;
        }
        if (loaded.getStringList("classes.archer.skills")
                .equals(List.of("ATTACK", "RANGED_ATTACK", "BUFF_OWNER"))) {
            loaded.set("classes.archer.skills", defaults.getStringList("classes.archer.skills"));
            changed = true;
        }
        if (loaded.getStringList("classes.guardian.skills").equals(List.of("ATTACK", "SHIELD_OWNER"))) {
            loaded.set("classes.guardian.skills", defaults.getStringList("classes.guardian.skills"));
            changed = true;
        }
        if (loaded.getStringList("classes.guardian.skills")
                .equals(List.of("ATTACK", "SHIELD_OWNER", "HEAL_OWNER"))) {
            loaded.set("classes.guardian.skills", defaults.getStringList("classes.guardian.skills"));
            changed = true;
        }
        if (loaded.getStringList("classes.medic.skills")
                .equals(List.of("HEAL_OWNER", "HEAL_ALLIES", "BUFF_OWNER", "BUFF_ALLIES"))) {
            loaded.set("classes.medic.skills", defaults.getStringList("classes.medic.skills"));
            changed = true;
        }
        String vexMaximumPath = "classes.druid.skill-settings.SUMMON_VEXES.max-active";
        int oldVexMaximum = loaded.getInt(vexMaximumPath, 7);
        if (oldVexMaximum == 2 || oldVexMaximum == 4) {
            loaded.set(vexMaximumPath, 7);
            loaded.setComments(vexMaximumPath, defaults.getComments(vexMaximumPath));
            changed = true;
        }
        String druidNamePath = "classes.druid.display-name";
        if ("Druid".equals(loaded.getString(druidNamePath))) {
            loaded.set(druidNamePath, defaults.getString(druidNamePath, "Tu sĩ rừng"));
            loaded.setComments(druidNamePath, defaults.getComments(druidNamePath));
            changed = true;
        }
        String skillPowerPath = "progression.skill-power-per-level";
        if (Math.abs(loaded.getDouble(skillPowerPath, 0.05) - 0.02) < 0.000001) {
            loaded.set(skillPowerPath, 0.05);
            loaded.setComments(skillPowerPath, defaults.getComments(skillPowerPath));
            changed = true;
        }
        if (changed) loaded.save(configFile);
    }

    private void migrateRangerDefault(YamlConfiguration loaded, YamlConfiguration defaults, String path,
                                     double previous, double updated) {
        if (loaded.contains(path) && Double.compare(loaded.getDouble(path), previous) == 0) {
            loaded.set(path, updated);
            loaded.setComments(path, defaults.getComments(path));
        }
    }

    private boolean mergeSettingsSection(YamlConfiguration target, ConfigurationSection defaults, String prefix)
            throws InvalidConfigurationException {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            ConfigurationSection defaultChild = defaults.getConfigurationSection(key);
            if (defaultChild != null) {
                if (target.contains(path) && !target.isConfigurationSection(path)) {
                    throw new InvalidConfigurationException("Expected a configuration section at '" + path + "'.");
                }
                if (!target.isConfigurationSection(path)) {
                    target.createSection(path);
                    target.setComments(path, defaults.getComments(key));
                    changed = true;
                }
                changed |= mergeSettingsSection(target, defaultChild, path);
            } else if (!target.contains(path)) {
                target.set(path, defaults.get(key));
                target.setComments(path, defaults.getComments(key));
                changed = true;
            }
        }
        return changed;
    }

    private void loadData() {
        if (!dataFile.isFile()) {
            data = new YamlConfiguration();
            data.createSection("units");
            if (!saveData()) throw new IllegalStateException("Could not create mercenaries.yml.");
            return;
        }
        YamlConfiguration loaded = new YamlConfiguration();
        try {
            loaded.load(dataFile);
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Could not load mercenaries.yml; refusing to overwrite stored army data.",
                exception);
        }
        ConfigurationSection section = loaded.getConfigurationSection("units");
        if (section != null) {
            for (String idText : section.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(idText);
                    UUID owner = UUID.fromString(loaded.getString("units." + idText + ".owner", ""));
                    String classId = normalizeClass(loaded.getString("units." + idText + ".class", ""));
                    boolean hasSeparateTier = loaded.contains("units." + idText + ".tier");
                    int tier = loaded.getInt("units." + idText + ".tier",
                        loaded.getInt("units." + idText + ".level", 1));
                    int originalTier = loaded.getInt("units." + idText + ".original-tier", tier);
                    if (classId == null || originalTier < 1 || originalTier > 10
                            || tier < originalTier || tier > originalTier + 3 || tier > 13) {
                        plugin.getLogger().warning("[Army] Skipping invalid mercenary record " + idText + ".");
                        continue;
                    }
                    Unit unit = new Unit(id, owner, classId, tier,
                        parseMode(loaded.getString("units." + idText + ".mode", "PROTECT")),
                        loaded.getBoolean("units." + idText + ".deployed", false));
                    unit.originalTier = originalTier;
                    unit.level = hasSeparateTier
                        ? Math.max(1, Math.min(maxLevel, loaded.getInt("units." + idText + ".level", 1))) : 1;
                    unit.experience = Math.max(0, loaded.getInt("units." + idText + ".experience", 0));
                    unit.customName = loaded.getString("units." + idText + ".custom-name", "");
                    unit.recoveryUntilMillis = Math.max(0L,
                        loaded.getLong("units." + idText + ".recovery-until", 0L));
                    unit.gearCustomized = loaded.getBoolean("units." + idText + ".gear-customized", false);
                    unit.horseDead = loaded.getBoolean("units." + idText + ".horse-dead", false);
                    unit.entityId = parseUuid(loaded.getString("units." + idText + ".entity-uuid", ""));
                    unit.muted = loaded.getBoolean("units." + idText + ".muted", false);
                    unit.worldName = loaded.getString("units." + idText + ".world", "");
                    unit.x = loaded.getDouble("units." + idText + ".x");
                    unit.y = loaded.getDouble("units." + idText + ".y");
                    unit.z = loaded.getDouble("units." + idText + ".z");
                    ItemStack horseArmor = loaded.getItemStack("units." + idText + ".horse-armor");
                    if (isHorseArmor(horseArmor)) unit.horseArmor = horseArmor.clone();
                    for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
                        String path = "units." + idText + ".equipment." + slotPath(slot);
                        ItemStack item = loaded.getItemStack(path);
                        if (item != null && !item.getType().isAir()) unit.equipment.put(slot, item.clone());
                    }
                    for (int slot = 0; slot < unit.inventory.length; slot++) {
                        ItemStack item = loaded.getItemStack("units." + idText + ".inventory." + slot);
                        if (item != null && !item.getType().isAir()) unit.inventory[slot] = item.clone();
                    }
                    units.put(id, unit);
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("[Army] Skipping malformed mercenary data at units." + idText + ".");
                }
            }
        }
        data = loaded;
    }

    private void startAiTask() {
        aiTask = new BukkitRunnable() {
            private int saveCounter;

            @Override
            public void run() {
                if (!enabled) return;
                for (Unit unit : new ArrayList<>(units.values())) {
                    if (!unit.deployed) continue;
                    Player owner = Bukkit.getPlayer(unit.owner);
                    Mob mob = activeEntities.get(unit.id);
                    if (mob != null && mob.isValid()) preventZombification(mob);
                    if (owner == null || !owner.isOnline()) {
                        cleanupSummonedMinions(unit.id);
                        if (mob != null) {
                            captureEquipment(unit, mob);
                            updateLocation(unit, mob.getLocation());
                            removeMount(unit.id, mob);
                            mob.remove();
                            activeEntities.remove(unit.id);
                            unit.entityId = null;
                        } else {
                            removeMount(unit.id, null);
                        }
                        continue;
                    }
                    if (isGearOpen(owner, unit.id)) continue;
                    if (mob == null || !mob.isValid() || mob.isDead()) {
                        mob = findExistingEntity(unit);
                        if (mob == null) mob = spawnUnit(unit, owner.getLocation());
                        if (mob == null) {
                            owner.sendMessage("§cKhông thể triệu hồi lính §e#" + shortId(unit.id) + "§c; kiểm tra cấu hình entity.");
                            unit.deployed = false;
                            continue;
                        }
                        activeEntities.put(unit.id, mob);
                    }
                    EntityType configuredType = configuredEntityType(unit);
                    if (configuredType != null && mob.getType() != configuredType) {
                        captureEquipment(unit, mob);
                        updateLocation(unit, mob.getLocation());
                        if (!saveData()) {
                            plugin.getLogger().severe("[Army] Could not save gear before replacing entity "
                                + "for mercenary " + unit.id + "; keeping the current entity.");
                            continue;
                        }
                        Mob replacement = spawnUnit(unit, mob.getLocation());
                        if (replacement == null) {
                            plugin.getLogger().severe("[Army] Could not replace outdated entity for mercenary "
                                + unit.id + "; keeping the current entity.");
                            unit.entityId = mob.getUniqueId();
                            continue;
                        }
                        mob.remove();
                        mob = replacement;
                        activeEntities.put(unit.id, mob);
                    }
                    mob.setSilent(unit.muted);
                    removeUnskilledVexes(mob);
                    ensurePaladinMounted(unit, mob);
                    updateStatusDisplay(unit, mob);
                    updateLocation(unit, mob.getLocation());
                    runUnitAi(unit, mob, owner);
                }
                collectExperienceOrbs();
                if (++saveCounter >= Math.max(1, 100 / aiIntervalTicks)) {
                    saveCounter = 0;
                    for (Unit unit : units.values()) {
                        Mob active = activeEntities.get(unit.id);
                        if (active != null && active.isValid()) {
                            captureEquipment(unit, active);
                            updateLocation(unit, active.getLocation());
                        }
                    }
                    saveData();
                }
            }
        };
        aiTask.runTaskTimer(plugin, aiIntervalTicks, aiIntervalTicks);
    }

    private void runUnitAi(Unit unit, Mob mob, Player owner) {
        mob.setAware(true);
        if (!owner.getWorld().equals(mob.getWorld())) {
            Location spawn = safeLocation(owner.getLocation());
            teleportUnit(unit, mob, spawn);
            wanderTargets.remove(unit.id);
            wanderTargetExpiries.remove(unit.id);
            updateSummonedMinions(unit, null);
            return;
        }
        double distanceSquared = owner.getLocation().distanceSquared(mob.getLocation());
        if (distanceSquared > teleportDistance * teleportDistance) {
            teleportUnit(unit, mob, owner.getLocation().clone().add(0.8, 0.0, 0.8));
            mob.setTarget(null);
            wanderTargets.remove(unit.id);
            wanderTargetExpiries.remove(unit.id);
            updateSummonedMinions(unit, null);
            return;
        }
        LivingEntity target = findTarget(unit, mob, owner);
        if (target == null && unit.mode == Mode.PROTECT && hasAiPriority(unit, "FOLLOW_OWNER")
                && distanceSquared > followDistance * followDistance) {
            mob.setTarget(null);
            mob.getPathfinder().moveTo(owner.getLocation(), 1.15);
            useSkills(unit, owner, null);
            updateSummonedMinions(unit, null);
            feedMercenary(unit, mob, owner);
            return;
        }

        if (target != null) {
            mob.setAware(true);
            mob.setSilent(unit.muted);
            if (hasSkill(unit, "RANGED_ATTACK") && !hasSkill(unit, "ATTACK")) {
                mob.setTarget(null);
                double range = preferredRangedPositionRange(unit, mob);
                if (mob.getLocation().distanceSquared(target.getLocation()) > range * range
                        || !mob.hasLineOfSight(target)) {
                    mob.getPathfinder().moveTo(target, 1.0);
                } else {
                    mob.getPathfinder().stopPathfinding();
                }
            } else if (mob.getTarget() != target) {
                mob.setTarget(target);
                if (hasSkill(unit, "ATTACK") && ready(unit, "attack")) {
                    markUsed(unit, "attack", skillCooldownSeconds(unit, "ATTACK"));
                    announceSkillCast(unit, owner, "ATTACK", Particle.CRIT);
                }
            }
        } else {
            mob.setTarget(null);
            if (unit.mode == Mode.ATTACK && distanceSquared > followDistance * followDistance
                    && hasAiPriority(unit, "RETURN_TO_OWNER")) {
                mob.getPathfinder().moveTo(owner.getLocation(), 1.15);
            } else if (hasAiPriority(unit, "WANDER")) {
                Location center = unit.mode == Mode.ATTACK ? mob.getLocation() : owner.getLocation();
                moveToWanderTarget(unit, mob, center);
            } else {
                mob.getPathfinder().stopPathfinding();
            }
        }
        useSkills(unit, owner, target);
        updateSummonedMinions(unit, target);
        feedMercenary(unit, mob, owner);
    }

    private double preferredRangedPositionRange(Unit unit, Mob mob) {
        double range = skillOptionDouble(unit, "RANGED_ATTACK", "range-blocks", 16.0, 4.0, 64.0);
        if (!"ranger".equals(unit.unitClass)) return range;
        if (hasSkill(unit, "VOLLEY") && ready(unit, "volley")) {
            range = Math.min(range, skillOptionDouble(unit, "VOLLEY", "range-blocks", range, 4.0, 64.0));
        }
        if (hasSkill(unit, "ILLUMINATION") && ready(unit, "illumination")
                && canUseIlluminationInCurrentLight(mob)) {
            range = Math.min(range,
                skillOptionDouble(unit, "ILLUMINATION", "range-blocks", range, 4.0, 64.0));
        }
        return range;
    }

    private void feedMercenary(Unit unit, Mob mob, Player owner) {
        double maximum = maxHealth(mob);
        if (maximum <= 0.0 || mob.getHealth() >= maximum || foodHealAmount <= 0.0) return;
        for (int slot = 0; slot < unit.inventory.length; slot++) {
            ItemStack food = unit.inventory[slot];
            if (food == null || !food.getType().isEdible()) continue;
            double oldHealth = mob.getHealth();
            mob.setHealth(Math.min(maximum, oldHealth + foodHealAmount));
            if (food.getAmount() <= 1) unit.inventory[slot] = null;
            else {
                ItemStack remainder = food.clone();
                remainder.setAmount(food.getAmount() - 1);
                unit.inventory[slot] = remainder;
            }
            mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, 1, 0),
                6, 0.3, 0.35, 0.3, 0.02);
            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.0f);
            owner.sendActionBar("§a" + displayName(unit) + " §eđã dùng thức ăn để hồi máu");
            break;
        }
    }

    private void collectExperienceOrbs() {
        if (!experienceOrbPickupEnabled) return;
        Map<UUID, org.bukkit.entity.ExperienceOrb> nearbyOrbs = new HashMap<>();
        Map<UUID, UUID> orbOwners = new HashMap<>();
        Map<UUID, Double> closestDistances = new HashMap<>();
        double radiusSquared = experienceOrbPickupRadius * experienceOrbPickupRadius;
        for (Unit unit : units.values()) {
            if (!unit.deployed || unit.recoveryUntilMillis > System.currentTimeMillis()) continue;
            Player owner = Bukkit.getPlayer(unit.owner);
            Mob mob = activeEntities.get(unit.id);
            if (owner == null || !owner.isOnline() || mob == null || !mob.isValid()
                    || !owner.getWorld().equals(mob.getWorld())) continue;
            for (Entity entity : mob.getNearbyEntities(experienceOrbPickupRadius,
                    experienceOrbPickupRadius, experienceOrbPickupRadius)) {
                if (!(entity instanceof org.bukkit.entity.ExperienceOrb orb)
                        || !orb.isValid() || orb.isDead()) continue;
                double distance = mob.getLocation().distanceSquared(orb.getLocation());
                if (distance > radiusSquared || distance >= closestDistances.getOrDefault(
                        orb.getUniqueId(), Double.MAX_VALUE)) continue;
                nearbyOrbs.put(orb.getUniqueId(), orb);
                orbOwners.put(orb.getUniqueId(), unit.owner);
                closestDistances.put(orb.getUniqueId(), distance);
            }
        }
        for (Map.Entry<UUID, org.bukkit.entity.ExperienceOrb> entry : nearbyOrbs.entrySet()) {
            org.bukkit.entity.ExperienceOrb orb = entry.getValue();
            Player owner = Bukkit.getPlayer(orbOwners.get(entry.getKey()));
            if (owner == null || !owner.isOnline() || !orb.isValid() || orb.isDead()) continue;
            int experience = orb.getExperience();
            if (experience <= 0) continue;
            owner.giveExp(experience);
            orb.remove();
        }
    }

    private LivingEntity findTarget(Unit unit, Mob mob, Player owner) {
        if (!hasSkill(unit, "ATTACK") && !hasSkill(unit, "RANGED_ATTACK")
            && !hasSkill(unit, "FIRE_BOLT") && !hasSkill(unit, "ROOT_TARGET")) return null;
        double classRadius = readBoundedDouble(settings, "classes." + unit.unitClass + ".target-radius-blocks",
            targetRadius, 4.0, 50.0);
        Location center = unit.mode == Mode.ATTACK ? mob.getLocation() : owner.getLocation();
        for (String action : aiPriorities(unit)) {
            LivingEntity target = switch (action) {
                case "OWNER_ATTACK_TARGET" -> unit.mode == Mode.DEFEND ? null : ownerAttackTarget(unit, mob, owner);
                case "OWNER_ATTACKER" -> unit.mode == Mode.PROTECT ? findOwnerAttacker(mob, owner) : null;
                case "OWNER_THREAT" -> unit.mode == Mode.DEFEND ? null : findOwnerThreatTarget(unit.owner, mob);
                case "NEAREST_HOSTILE" -> findNearestTarget(unit, mob, center, classRadius);
                case "SEARCH" -> findNearestTarget(unit, mob, center,
                    "ranger".equals(unit.unitClass) ? classRadius : targetRadius);
                default -> null;
            };
            if (target != null
                    && (!requireLineOfSight || mob.hasLineOfSight(target))
                    && !isHiddenUndergroundTarget(mob, target)) return target;
        }
        return null;
    }

    private LivingEntity ownerAttackTarget(Unit unit, Mob mob, Player owner) {
        UUID targetId = ownerAttackTargets.get(owner.getUniqueId());
        if (targetId == null || !isActiveOwnerAttackTarget(owner.getUniqueId(), targetId)) return null;
        Entity entity = Bukkit.getEntity(targetId);
        if (entity instanceof LivingEntity target && isAllowedMercenaryTarget(unit.owner, target, true)
                && mob.getWorld().equals(target.getWorld())) return target;
        if (targetId != null && (entity == null || entity.isDead())) ownerAttackTargets.remove(owner.getUniqueId());
        return null;
    }

    private boolean isActiveOwnerAttackTarget(UUID ownerId, UUID targetId) {
        Long expiresAt = ownerAttackTargetExpiries.get(ownerId);
        if (expiresAt == null || System.currentTimeMillis() >= expiresAt) {
            ownerAttackTargets.remove(ownerId);
            ownerAttackTargetExpiries.remove(ownerId);
            return false;
        }
        return targetId.equals(ownerAttackTargets.get(ownerId));
    }

    private LivingEntity findOwnerAttacker(Mob mob, Player owner) {
        return mob.getWorld().getNearbyEntities(owner.getLocation(), targetRadius, targetRadius, targetRadius)
            .stream()
            .filter(entity -> entity instanceof LivingEntity)
            .map(entity -> (LivingEntity) entity)
            .filter(this::isHostileMob)
            .filter(entity -> {
                if (entity instanceof Mob attacker) {
                    return attacker.getTarget() != null
                        && attacker.getTarget().getUniqueId().equals(owner.getUniqueId());
                }
                if (entity instanceof org.bukkit.entity.EnderDragon dragon) {
                    return dragon.getTarget() != null
                        && dragon.getTarget().getUniqueId().equals(owner.getUniqueId());
                }
                return false;
            })
            .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(owner.getLocation())))
            .orElse(null);
    }

    private LivingEntity findOwnerThreatTarget(UUID ownerId, Mob mob) {
        Map<UUID, Long> threats = ownerThreats.get(ownerId);
        if (threats == null) return null;
        long now = System.currentTimeMillis();
        threats.entrySet().removeIf(entry -> now - entry.getValue() > 15_000L);
        for (Map.Entry<UUID, Long> threat : threats.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed()).toList()) {
            Entity entity = Bukkit.getEntity(threat.getKey());
            if (entity instanceof LivingEntity target && !target.isDead()
                    && mob.getWorld().equals(target.getWorld())
                    && isAllowedMercenaryTarget(ownerId, target, false)) return target;
        }
        return null;
    }

    private LivingEntity findNearestTarget(Unit unit, Mob mob, Location center, double radius) {
        return mob.getWorld().getNearbyEntities(center, radius, radius, radius).stream()
            .filter(entity -> entity instanceof LivingEntity)
            .map(entity -> (LivingEntity) entity)
            .filter(living -> !living.equals(mob))
            .filter(living -> living.getLocation().distanceSquared(center) <= radius * radius)
            .filter(living -> isAllowedMercenaryTarget(unit.owner, living, false))
            .filter(living -> !requireLineOfSight || mob.hasLineOfSight(living))
            .filter(living -> !isHiddenUndergroundTarget(mob, living))
            .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)))
            .orElse(null);
    }

    private boolean isHiddenUndergroundTarget(Mob mob, LivingEntity target) {
        if (!settings.getBoolean("ai.ignore-hidden-underground-targets", true)
                || mob.hasLineOfSight(target) || !mob.getWorld().equals(target.getWorld())) return false;
        Location targetLocation = target.getLocation();
        int terrainHeight = target.getWorld().getHighestBlockYAt(targetLocation);
        return targetLocation.getBlockY() <= terrainHeight;
    }

    private List<String> aiPriorities(Unit unit) {
        return settings.getStringList("ai.priorities." + unit.mode.name().toLowerCase(Locale.ROOT)).stream()
            .map(value -> value.toUpperCase(Locale.ROOT)).toList();
    }

    private boolean hasAiPriority(Unit unit, String action) {
        return aiPriorities(unit).contains(action);
    }

    private void moveToWanderTarget(Unit unit, Mob mob, Location center) {
        long now = System.currentTimeMillis();
        Location destination = wanderTargets.get(unit.id);
        if (destination == null || !destination.getWorld().equals(center.getWorld())
                || now >= wanderTargetExpiries.getOrDefault(unit.id, 0L)
                || mob.getLocation().distanceSquared(destination) < 2.25) {
            double angle = java.util.concurrent.ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
            double radius = java.util.concurrent.ThreadLocalRandom.current().nextDouble(1.0, wanderRadius + 1.0);
            destination = center.clone().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            wanderTargets.put(unit.id, destination);
            wanderTargetExpiries.put(unit.id, now + 8_000L);
        }
        mob.getPathfinder().moveTo(destination, 0.8);
    }

    private void updateSummonedMinions(Unit unit, LivingEntity target) {
        Set<UUID> minionIds = summonedMinions.get(unit.id);
        if (minionIds == null) return;
        Mob druid = activeEntities.get(unit.id);
        Player owner = Bukkit.getPlayer(unit.owner);
        for (UUID minionId : List.copyOf(minionIds)) {
            Entity entity = Bukkit.getEntity(minionId);
            if (!(entity instanceof Vex vex) || !vex.isValid() || vex.isDead()) {
                removeStatusDisplay(minionId);
                continue;
            }
            if (druid != null && druid.isValid() && owner != null && owner.isOnline()) {
                LivingEntity selectedTarget = selectVexTarget(unit, druid, owner, vex, target);
                if (selectedTarget != null) {
                    if (vex.getTarget() != selectedTarget) vex.setTarget(selectedTarget);
                } else {
                    if (vex.getTarget() != null) vex.setTarget(null);
                    if (!vex.getWorld().equals(druid.getWorld())) {
                        removeStatusDisplay(vex.getUniqueId());
                        vex.remove();
                        minionIds.remove(minionId);
                        continue;
                    }
                    double returnRadius = skillOptionDouble(
                        unit, "SUMMON_VEXES", "follow-radius-blocks", 5.0, 2.0, 16.0);
                    if (vex.getLocation().distanceSquared(druid.getLocation()) > returnRadius * returnRadius) {
                        vex.getPathfinder().moveTo(druid.getLocation(), 0.6);
                    } else {
                        vex.getPathfinder().stopPathfinding();
                    }
                }
            }
            updateVexStatusDisplay(vex, unit.owner, vexActionIcon(vex, druid));
        }
    }

    private LivingEntity selectVexTarget(Unit unit, Mob druid, Player owner, Vex vex,
                                         LivingEntity druidTarget) {
        List<String> priorities = settings.getStringList(
            "classes.druid.skill-settings.SUMMON_VEXES.ai-priorities").stream()
            .map(value -> value.toUpperCase(Locale.ROOT)).toList();
        if (priorities.isEmpty()) {
            priorities = List.of("DRUID_TARGET", "DRUID_ATTACKER", "OWNER_ATTACKER", "FOLLOW_DRUID");
        }
        for (String priority : priorities) {
            if ("FOLLOW_DRUID".equals(priority)) return null;
            LivingEntity candidate = switch (priority) {
                case "DRUID_TARGET" -> druidTarget != null ? druidTarget : druid.getTarget();
                case "DRUID_ATTACKER" -> findRecentThreatTarget(vex, unitThreats.get(unit.id));
                case "OWNER_ATTACKER" -> {
                    LivingEntity recentAttacker = findRecentThreatTarget(vex, ownerAttackers.get(unit.owner));
                    yield recentAttacker != null ? recentAttacker : findOwnerAttacker(vex, owner);
                }
                default -> null;
            };
            if (candidate != null && canMinionTarget(vex, candidate)) return candidate;
        }
        return null;
    }

    private LivingEntity findRecentThreatTarget(Mob mob, Map<UUID, Long> threats) {
        if (threats == null) return null;
        long now = System.currentTimeMillis();
        threats.entrySet().removeIf(entry -> now - entry.getValue() > 15_000L);
        for (Map.Entry<UUID, Long> threat : threats.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed()).toList()) {
            Entity entity = Bukkit.getEntity(threat.getKey());
            if (entity instanceof LivingEntity target && !target.isDead()
                    && canMinionTarget(mob, target)) return target;
        }
        return null;
    }

    private String vexActionIcon(Vex vex, Mob druid) {
        LivingEntity target = vex.getTarget();
        if (target != null && canMinionTarget(vex, target)) return vexAttackIcon;
        if (druid != null && druid.isValid()
                && (!vex.getWorld().equals(druid.getWorld())
                    || vex.getLocation().distanceSquared(druid.getLocation()) > 16.0)) {
            return vexFollowIcon;
        }
        return vexIdleIcon;
    }

    private void updateVexStatusDisplay(Vex vex, UUID ownerId, String actionIcon) {
        if (!statusDisplayEnabled) {
            removeStatusDisplay(vex.getUniqueId());
            return;
        }
        double maximumHealth = vex.getAttribute(Attribute.MAX_HEALTH) == null
            ? vex.getHealth() : vex.getAttribute(Attribute.MAX_HEALTH).getValue();
        int filledBars = statusBarFilled(vex.getHealth(), maximumHealth);
        String healthBar = "§e" + actionIcon + " " + vexHealthColor
            + "■".repeat(filledBars)
            + "§7" + "□".repeat(statusDisplayBarLength - filledBars);
        statusPacketListener.update(vex.getUniqueId(), vex, ownerId, healthBar, statusDisplaySeeThrough);
    }

    private ChatColor readVexHealthColor(YamlConfiguration loaded) throws InvalidConfigurationException {
        String configuredColor = loaded.getString("status-display.vex-health-color", "GREEN");
        if (configuredColor == null || configuredColor.isBlank()) {
            throw new InvalidConfigurationException(
                "status-display.vex-health-color must be a valid ChatColor color name.");
        }
        try {
            ChatColor color = ChatColor.valueOf(configuredColor.toUpperCase(Locale.ROOT));
            if (!color.isColor()) throw new IllegalArgumentException("Formatting colors are not allowed.");
            return color;
        } catch (IllegalArgumentException exception) {
            throw new InvalidConfigurationException(
                "Unknown status-display.vex-health-color '" + configuredColor + "'. Use a ChatColor color name.");
        }
    }

    private String readVexActionIcon(YamlConfiguration loaded, String action, String defaultIcon)
            throws InvalidConfigurationException {
        String path = "status-display.vex-action-icons." + action;
        String icon = loaded.getString(path, defaultIcon);
        if (icon == null || icon.isBlank() || icon.codePointCount(0, icon.length()) > 4) {
            throw new InvalidConfigurationException(
                "Invalid " + path + "; use a non-empty symbol of at most 4 Unicode code points.");
        }
        return icon;
    }

    private void useSkills(Unit unit, Player owner, LivingEntity target) {
        Mob mob = activeEntities.get(unit.id);
        if (target != null && (mob == null || !mob.isValid() || !target.isValid() || target.isDead()
                || !mob.getWorld().equals(target.getWorld()) || !mob.hasLineOfSight(target)
                || isHiddenUndergroundTarget(mob, target))) {
            if (mob != null && mob.isValid()) mob.setTarget(null);
            target = null;
        }
        if (hasSkill(unit, "SUMMON_VEXES") && mob != null && target instanceof Monster hostile
                && !isMercenary(hostile) && ready(unit, "summon-vexes")) {
            int maximum = maxSummonedMinions(unit);
            int lifetime = (int) skillOptionDouble(unit, "SUMMON_VEXES",
                "lifetime-seconds", 15.0, 1.0, 120.0);
            double damage = skillOptionDouble(unit, "SUMMON_VEXES", "damage", 4.0, 0.0, 100.0)
                * skillPowerMultiplier(unit);
            Set<UUID> active = summonedMinions.computeIfAbsent(unit.id, ignored -> new HashSet<>());
            active.removeIf(id -> {
                Entity entity = Bukkit.getEntity(id);
                return entity == null || !entity.isValid() || entity.isDead();
            });
            int summoned = 0;
            for (int index = active.size(); index < maximum; index++) {
                Location spawnLocation = mob.getLocation().clone().add(
                    index % 2 == 0 ? 0.7 : -0.7, 1.0, index % 2 == 0 ? -0.7 : 0.7);
                Entity entity;
                try {
                    entity = mob.getWorld().spawnEntity(spawnLocation, EntityType.VEX);
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("[Army] Could not summon Vex for class "
                        + unit.unitClass + ": " + exception.getMessage());
                    break;
                }
                if (!(entity instanceof Vex vex)) {
                    entity.remove();
                    continue;
                }
                vex.getPersistentDataContainer().set(summonedMinionKey,
                    PersistentDataType.STRING, unit.id.toString());
                vex.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, unit.owner.toString());
                vex.setSummoner(mob);
                vex.setPersistent(false);
                vex.setRemoveWhenFarAway(true);
                vex.setCanPickupItems(false);
                vex.setAware(true);
                vex.setLimitedLifetime(true);
                vex.setLimitedLifetimeTicks(lifetime * 20);
                if (vex.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                    vex.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(Math.min(100.0, damage));
                }
                vex.setTarget(hostile);
                active.add(vex.getUniqueId());
                summoned++;
                break;
            }
            if (summoned > 0) {
                markUsed(unit, "summon-vexes", skillCooldownSeconds(unit, "SUMMON_VEXES"));
                announceSkillCast(unit, owner, "SUMMON_VEXES", Particle.SOUL);
            }
        }
        if (hasSkill(unit, "LANCE_CHARGE") && mob != null && target != null && ready(unit, "lance-charge")
                && "paladin".equals(unit.unitClass) && mob instanceof Zombie zombie) {
            Horse horse = getMountedHorse(unit, zombie);
            if (horse != null && target instanceof Monster hostile && !isMercenary(hostile)) {
                double damage = Math.min(100.0,
                    skillOptionDouble(unit, "LANCE_CHARGE", "damage", 12.0, 0.0, 100.0)
                        * skillPowerMultiplier(unit));
                double radius = skillOptionDouble(unit, "LANCE_CHARGE", "radius-blocks", 4.0, 1.0, 12.0);
                Vector chargeDir = hostile.getLocation().toVector().subtract(horse.getLocation().toVector());
                if (chargeDir.lengthSquared() <= 0.0001) chargeDir = new Vector(0.0, 0.0, 1.0);
                else chargeDir.normalize();
                horse.setVelocity(chargeDir.clone().multiply(1.2));
                zombie.swingMainHand();
                zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 0.8f);
                horse.getWorld().spawnParticle(Particle.CRIT, horse.getLocation().add(0, 1, 0),
                    18, 0.6, 0.7, 0.6, 0.05);
                horse.getWorld().spawnParticle(Particle.SWEEP_ATTACK, zombie.getLocation().add(0, 1, 0),
                    1, 0.15, 0.15, 0.15, 0.0);
                hostile.getWorld().spawnParticle(Particle.CRIT, hostile.getLocation().add(0, 1, 0),
                    12, 0.35, 0.5, 0.35, 0.04);
                for (Entity nearby : hostile.getWorld().getNearbyEntities(hostile.getLocation(), radius, radius, radius)) {
                    if (!(nearby instanceof Monster victim) || !isValidTarget(victim) || isMercenary(victim)) continue;
                    if (zombie.hasLineOfSight(victim)
                            && victim.getLocation().distanceSquared(hostile.getLocation()) <= radius * radius) {
                        victim.damage(damage, zombie);
                        victim.setVelocity(victim.getVelocity().add(chargeDir.clone().multiply(0.35)));
                    }
                }
                markUsed(unit, "lance-charge", skillCooldownSeconds(unit, "LANCE_CHARGE"));
                announceSkillCast(unit, owner, "LANCE_CHARGE", Particle.CRIT);
            }
        }
        if (hasSkill(unit, "ILLUMINATION") && mob != null && target != null
                && isWithinConfiguredSkillRange(unit, mob, target, "ILLUMINATION")
                && canUseIlluminationInCurrentLight(mob)
                && ready(unit, "illumination")) {
            fireIlluminationVolley(unit, mob, target, owner);
        }
        if (hasSkill(unit, "RANGED_ATTACK") && target != null && mob != null
                && isWithinConfiguredSkillRange(unit, mob, target, "RANGED_ATTACK")
                && ready(unit, "ranged-attack")) {
            try {
                int ammoBefore = countStoredArrows(unit);
                AbstractArrow arrow = launchConfiguredArrow(mob, unit);
                arrow.setDamage(Math.min(100.0, skillOptionDouble(unit, "RANGED_ATTACK",
                    "damage", 4.0, 0.0, 100.0) * skillPowerMultiplier(unit)));
                arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                if ("ranger".equals(unit.unitClass)) {
                    Vector aim = target.getEyeLocation().toVector().subtract(mob.getEyeLocation().toVector());
                    if (aim.lengthSquared() > 0.0001) arrow.setVelocity(aim.normalize().multiply(2.4));
                }
                persistAmmoIfConsumed(unit, ammoBefore);
                markUsed(unit, "ranged-attack", skillCooldownSeconds(unit, "RANGED_ATTACK"));
                announceSkillCast(unit, owner, "RANGED_ATTACK", Particle.CRIT);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("[Army] Could not launch RANGED_ATTACK for class "
                    + unit.unitClass + ": " + exception.getMessage());
            }
        }

        if (hasSkill(unit, "VOLLEY") && target != null && mob != null
                && isWithinConfiguredSkillRange(unit, mob, target, "VOLLEY") && ready(unit, "volley")) {
            int arrowCount = (int) skillOptionDouble(unit, "VOLLEY", "arrow-count", 3.0, 2.0, 5.0);
            double speed = skillOptionDouble(unit, "VOLLEY", "arrow-speed", 2.0, 0.5, 4.0);
            double spread = skillOptionDouble(unit, "VOLLEY", "spread-degrees", 8.0, 0.0, 30.0);
            double damage = Math.min(100.0, skillOptionDouble(unit, "VOLLEY", "damage", 3.0,
                0.0, 100.0) * skillPowerMultiplier(unit));
            int ammoBefore = countStoredArrows(unit);
            Vector direction = target.getEyeLocation().toVector().subtract(mob.getEyeLocation().toVector());
            if (direction.lengthSquared() > 0.0) {
                for (int index = 0; index < arrowCount; index++) {
                    double offset = index - (arrowCount - 1) / 2.0;
                    AbstractArrow arrow = launchConfiguredArrow(mob, unit);
                    arrow.setVelocity(direction.clone().normalize()
                        .rotateAroundY(Math.toRadians(offset * spread)).multiply(speed));
                    arrow.setDamage(damage);
                    arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                }
                persistAmmoIfConsumed(unit, ammoBefore);
                markUsed(unit, "volley", skillCooldownSeconds(unit, "VOLLEY"));
                announceSkillCast(unit, owner, "VOLLEY", Particle.CRIT);
            }
        }

        if (hasSkill(unit, "POWER_STRIKE") && target != null && mob != null
                && ready(unit, "power-strike")) {
            double damage = Math.min(100.0, skillOptionDouble(unit, "POWER_STRIKE",
                "damage", 8.0, 0.0, 100.0) * skillPowerMultiplier(unit));
            target.damage(damage, mob);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                16, 0.35, 0.45, 0.35, 0.15);
            markUsed(unit, "power-strike", skillCooldownSeconds(unit, "POWER_STRIKE"));
            announceSkillCast(unit, owner, "POWER_STRIKE", Particle.SWEEP_ATTACK);
        }

        if (hasSkill(unit, "TAUNT") && mob != null && ready(unit, "taunt")) {
            double radius = skillOptionDouble(unit, "TAUNT", "radius-blocks", 8.0, 1.0, 32.0);
            List<Monster> taunted = mob.getWorld().getNearbyEntities(owner.getLocation(), radius, radius, radius)
                .stream().filter(Monster.class::isInstance).map(Monster.class::cast)
                .filter(monster -> monster.isValid() && !monster.isDead()
                    && (monster.getTarget() == null
                        || monster.getTarget().getUniqueId().equals(owner.getUniqueId())))
                .toList();
            if (!taunted.isEmpty()) {
                taunted.forEach(monster -> monster.setTarget(mob));
                markUsed(unit, "taunt", skillCooldownSeconds(unit, "TAUNT"));
                announceSkillCast(unit, owner, "TAUNT", Particle.WAX_ON);
            }
        }

        if (hasSkill(unit, "CLEANSE_OWNER") && ready(unit, "cleanse-owner")) {
            boolean cleansed = false;
            for (String effect : settings.getStringList("classes." + unit.unitClass
                    + ".skill-settings.CLEANSE_OWNER.effects")) {
                PotionEffectType type = Registry.EFFECT.get(
                    NamespacedKey.minecraft(effect.toLowerCase(Locale.ROOT)));
                if (type != null && owner.hasPotionEffect(type)) {
                    owner.removePotionEffect(type);
                    cleansed = true;
                }
            }
            if (cleansed) {
                markUsed(unit, "cleanse-owner", skillCooldownSeconds(unit, "CLEANSE_OWNER"));
                announceSkillCast(unit, owner, "CLEANSE_OWNER", Particle.HAPPY_VILLAGER);
            }
        }

        if (hasSkill(unit, "FIRE_BOLT") && target != null && mob != null && ready(unit, "fire-bolt")) {
            double damage = Math.min(100.0, skillOptionDouble(unit, "FIRE_BOLT",
                "damage", 7.0, 0.0, 100.0) * skillPowerMultiplier(unit));
            int fireSeconds = (int) skillOptionDouble(unit, "FIRE_BOLT", "fire-seconds", 3.0, 0.0, 15.0);
            target.damage(damage, mob);
            if (fireSeconds > 0) target.setFireTicks(Math.max(target.getFireTicks(), fireSeconds * 20));
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0),
                18, 0.35, 0.45, 0.35, 0.03);
            markUsed(unit, "fire-bolt", skillCooldownSeconds(unit, "FIRE_BOLT"));
            announceSkillCast(unit, owner, "FIRE_BOLT", Particle.FLAME);
        }

        if (hasSkill(unit, "ROOT_TARGET") && target != null && ready(unit, "root-target")) {
            String effectName = skillOptionString(unit, "ROOT_TARGET", "effect", "SLOWNESS");
            PotionEffectType effect = Registry.EFFECT.get(
                NamespacedKey.minecraft(effectName.toLowerCase(Locale.ROOT)));
            if (effect != null) {
                int duration = (int) skillOptionDouble(unit, "ROOT_TARGET",
                    "duration-seconds", 4.0, 1.0, 600.0);
                int amplifier = (int) skillOptionDouble(unit, "ROOT_TARGET", "amplifier", 1.0, 0.0, 10.0);
                target.addPotionEffect(new PotionEffect(effect, duration * 20, amplifier, true, true, true));
                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0),
                    14, 0.35, 0.45, 0.35, 0.02);
                markUsed(unit, "root-target", skillCooldownSeconds(unit, "ROOT_TARGET"));
                announceSkillCast(unit, owner, "ROOT_TARGET", Particle.SNOWFLAKE);
            }
        }

        if (hasSkill(unit, "LIFESTEAL") && target != null && mob != null && ready(unit, "lifesteal")) {
            double maximum = maxHealth(mob);
            if (maximum > 0.0 && mob.getHealth() < maximum) {
                double damage = Math.min(100.0, skillOptionDouble(unit, "LIFESTEAL",
                    "damage", 6.0, 0.0, 100.0) * skillPowerMultiplier(unit));
                double healing = Math.min(100.0, skillOptionDouble(unit, "LIFESTEAL",
                    "heal-amount", 4.0, 0.0, 100.0) * skillPowerMultiplier(unit));
                target.damage(damage, mob);
                mob.setHealth(Math.min(maximum, mob.getHealth() + healing));
                mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, 1, 0),
                    10, 0.3, 0.4, 0.3, 0.02);
                markUsed(unit, "lifesteal", skillCooldownSeconds(unit, "LIFESTEAL"));
                announceSkillCast(unit, owner, "LIFESTEAL", Particle.DAMAGE_INDICATOR);
            }
        }

        if (hasSkill(unit, "BERSERK") && target != null && mob != null && ready(unit, "berserk")) {
            double maximum = maxHealth(mob);
            double threshold = skillOptionDouble(unit, "BERSERK", "health-percent", 50.0, 1.0, 99.0);
            String effectName = skillOptionString(unit, "BERSERK", "effect", "STRENGTH");
            PotionEffectType effect = Registry.EFFECT.get(
                NamespacedKey.minecraft(effectName.toLowerCase(Locale.ROOT)));
            if (maximum > 0.0 && mob.getHealth() / maximum * 100.0 <= threshold
                    && effect != null && !mob.hasPotionEffect(effect)) {
                int duration = (int) skillOptionDouble(unit, "BERSERK",
                    "duration-seconds", 8.0, 1.0, 600.0);
                int amplifier = (int) skillOptionDouble(unit, "BERSERK", "amplifier", 0.0, 0.0, 10.0);
                mob.addPotionEffect(new PotionEffect(effect, duration * 20, amplifier, true, true, true));
                mob.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, mob.getLocation().add(0, 1, 0),
                    12, 0.35, 0.45, 0.35, 0.02);
                markUsed(unit, "berserk", skillCooldownSeconds(unit, "BERSERK"));
                announceSkillCast(unit, owner, "BERSERK", Particle.ANGRY_VILLAGER);
            }
        }

        double ownerMaxHealth = maxHealth(owner);
        if (hasSkill(unit, "HEAL_OWNER") && ownerMaxHealth > 0 && owner.getHealth() < ownerMaxHealth) {
            if (ready(unit, "heal-owner")) {
                double amount = Math.min(100.0, skillOptionDouble(unit, "HEAL_OWNER",
                    "amount", healAmount, 0.0, 100.0) * skillPowerMultiplier(unit));
                owner.setHealth(Math.min(ownerMaxHealth, owner.getHealth() + amount));
                markUsed(unit, "heal-owner", skillCooldownSeconds(unit, "HEAL_OWNER"));
                announceSkillCast(unit, owner, "HEAL_OWNER", Particle.HEART);
            }
        }
        if (hasSkill(unit, "HEAL_ALLIES") && ready(unit, "heal-allies")) {
            List<LivingEntity> allies = new ArrayList<>();
            String targetRule = skillOptionString(unit, "HEAL_ALLIES", "target", "OWNER_AND_ALLIES");
            if (!targetRule.equals("ALLIES")) allies.add(owner);
            for (UUID siblingId : getOwnerUnits(unit.owner).stream()
                    .map(sibling -> sibling.id).toList()) {
                Mob sibling = activeEntities.get(siblingId);
                if (sibling != null && sibling.isValid() && !targetRule.equals("OWNER")) allies.add(sibling);
            }
            boolean healed = false;
            for (LivingEntity ally : allies) {
                double maxHealth = maxHealth(ally);
                if (ally.isDead() || !ally.getWorld().equals(owner.getWorld())
                        || ally.getLocation().distanceSquared(owner.getLocation()) >
                            Math.pow(skillOptionDouble(unit, "HEAL_ALLIES", "radius-blocks", healRadius, 1.0, 32.0), 2)
                            || maxHealth <= 0 || ally.getHealth() >= maxHealth) continue;
                double amount = Math.min(100.0, skillOptionDouble(unit, "HEAL_ALLIES",
                    "amount", healAmount, 0.0, 100.0) * skillPowerMultiplier(unit));
                ally.setHealth(Math.min(maxHealth, ally.getHealth() + amount));
                healed = true;
            }
            if (healed) {
                markUsed(unit, "heal-allies", skillCooldownSeconds(unit, "HEAL_ALLIES"));
                announceSkillCast(unit, owner, "HEAL_ALLIES", Particle.HEART);
            }
        }
        if (hasSkill(unit, "BUFF_OWNER") && ready(unit, "buff-owner")) {
            applyConfiguredBuff(unit, owner, "BUFF_OWNER", "buff-owner");
        }
        if (hasSkill(unit, "BUFF_ALLIES") && ready(unit, "buff-allies")) {
            double radius = skillOptionDouble(unit, "BUFF_ALLIES", "radius-blocks", healRadius, 1.0, 32.0);
            String targetRule = skillOptionString(unit, "BUFF_ALLIES", "target", "OWNER_AND_ALLIES");
            List<LivingEntity> allies = new ArrayList<>();
            if (!targetRule.equals("ALLIES")) allies.add(owner);
            if (!targetRule.equals("OWNER")) {
                for (UUID siblingId : getOwnerUnits(unit.owner).stream().map(sibling -> sibling.id).toList()) {
                    Mob sibling = activeEntities.get(siblingId);
                    if (sibling != null && sibling.isValid()) allies.add(sibling);
                }
            }
            boolean buffed = false;
            for (LivingEntity ally : allies) {
                if (ally.isDead() || !ally.getWorld().equals(owner.getWorld())
                            || ally.getLocation().distanceSquared(owner.getLocation()) > radius * radius) continue;
                buffed |= applyConfiguredBuff(unit, ally, "BUFF_ALLIES", "buff-allies");
            }
            if (buffed) {
                markUsed(unit, "buff-allies", skillCooldownSeconds(unit, "BUFF_ALLIES"));
                announceSkillCast(unit, owner, "BUFF_ALLIES", Particle.ENCHANT);
            }
        }
    }

    private ItemStack findStoredArrow(Unit unit) {
        ItemStack offhand = unit.equipment.get(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
        if (isArrowItem(offhand)) return offhand;
        for (ItemStack item : unit.inventory) {
            if (isArrowItem(item)) return item;
        }
        return null;
    }

    private boolean isWithinConfiguredSkillRange(Unit unit, Mob mob, LivingEntity target, String skill) {
        if (!"ranger".equals(unit.unitClass)) return true;
        double defaultRange = skillOptionDouble(unit, "RANGED_ATTACK", "range-blocks", 16.0, 4.0, 64.0);
        double range = skillOptionDouble(unit, skill, "range-blocks", defaultRange, 4.0, 64.0);
        return mob.getWorld().equals(target.getWorld())
            && mob.getLocation().distanceSquared(target.getLocation()) <= range * range;
    }

    private boolean isArrowItem(ItemStack item) {
        return item != null && Set.of(Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW)
            .contains(item.getType());
    }

    private AbstractArrow launchConfiguredArrow(Mob mob, Unit unit) {
        ItemStack ammunition = findStoredArrow(unit);
        AbstractArrow arrow;
        boolean scout = "ranger".equals(unit.unitClass);
        if (ammunition == null) {
            arrow = scout ? mob.launchProjectile(SpectralArrow.class) : mob.launchProjectile(Arrow.class);
        } else if (ammunition.getType() == Material.SPECTRAL_ARROW) {
            arrow = mob.launchProjectile(SpectralArrow.class);
            if (scout) consumeStoredArrow(unit, ammunition);
        } else if (ammunition.getType() == Material.TIPPED_ARROW) {
            TippedArrow tippedArrow = mob.launchProjectile(TippedArrow.class);
            if (ammunition.getItemMeta() instanceof PotionMeta potionMeta) {
                PotionType potionType = potionMeta.getBasePotionType();
                if (potionType != null) tippedArrow.setBasePotionType(potionType);
                for (PotionEffect effect : potionMeta.getCustomEffects()) {
                    tippedArrow.addCustomEffect(effect, true);
                }
            }
            arrow = tippedArrow;
            if (scout) consumeStoredArrow(unit, ammunition);
        } else {
            arrow = mob.launchProjectile(Arrow.class);
            if (scout) consumeStoredArrow(unit, ammunition);
        }
        return arrow;
    }

    private void consumeStoredArrow(Unit unit, ItemStack ammunition) {
        org.bukkit.inventory.EquipmentSlot offhandSlot = org.bukkit.inventory.EquipmentSlot.OFF_HAND;
        if (unit.equipment.get(offhandSlot) == ammunition) {
            ItemStack remaining = decrementStack(ammunition);
            if (remaining == null) unit.equipment.remove(offhandSlot);
            else unit.equipment.put(offhandSlot, remaining);
            return;
        }
        for (int slot = 0; slot < unit.inventory.length; slot++) {
            if (unit.inventory[slot] == ammunition) {
                unit.inventory[slot] = decrementStack(ammunition);
                return;
            }
        }
    }

    private ItemStack decrementStack(ItemStack item) {
        if (item.getAmount() <= 1) return null;
        ItemStack remaining = item.clone();
        remaining.setAmount(item.getAmount() - 1);
        return remaining;
    }

    private int countStoredArrows(Unit unit) {
        int count = 0;
        ItemStack offhand = unit.equipment.get(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
        if (isArrowItem(offhand)) count += offhand.getAmount();
        for (ItemStack item : unit.inventory) {
            if (isArrowItem(item)) count += item.getAmount();
        }
        return count;
    }

    private void persistAmmoIfConsumed(Unit unit, int previousCount) {
        if (countStoredArrows(unit) < previousCount && !saveData()) {
            plugin.getLogger().warning("[Army] Could not persist consumed arrows for mercenary " + unit.id + ".");
        }
    }

    private void fireIlluminationVolley(Unit unit, Mob mob, LivingEntity target, Player owner) {
        String skill = "ILLUMINATION";
        int baseArrowCount = (int) skillOptionDouble(unit, skill, "base-arrow-count", 1.0, 1.0, 20.0);
        int levelsPerArrow = (int) skillOptionDouble(unit, skill, "levels-per-extra-arrow", 5.0, 1.0, 30.0);
        int arrowCount = Math.min(20, baseArrowCount + unit.level / levelsPerArrow);
        double radius = skillOptionDouble(unit, skill, "spread-radius-blocks", 5.0, 0.0, 16.0);
        double angle = Math.toRadians(skillOptionDouble(unit, skill, "upward-angle-degrees", 55.0, 10.0, 85.0));
        double speedMultiplier = skillOptionDouble(unit, skill, "speed-multiplier", 1.0, 0.5, 2.0);
        Vector origin = mob.getEyeLocation().toVector();
        Vector targetPosition = target.getLocation().toVector();
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();

        for (int index = 0; index < arrowCount; index++) {
            double spreadAngle = random.nextDouble(Math.PI * 2.0);
            double spreadDistance = radius * Math.sqrt(random.nextDouble());
            Vector landingPoint = targetPosition.clone().add(new Vector(
                Math.cos(spreadAngle) * spreadDistance, 0.0, Math.sin(spreadAngle) * spreadDistance));
            Vector horizontal = landingPoint.subtract(origin.clone());
            horizontal.setY(0.0);
            double horizontalDistance = Math.max(1.0, horizontal.length());
            double sinDoubleAngle = Math.max(0.1, Math.sin(angle * 2.0));
            double speed = Math.sqrt(horizontalDistance * 0.05 / sinDoubleAngle) * speedMultiplier;
            speed = Math.max(0.5, Math.min(4.0, speed));

            Arrow arrow = mob.launchProjectile(Arrow.class);
            Vector velocity = horizontal.normalize().multiply(speed * Math.cos(angle));
            velocity.setY(speed * Math.sin(angle));
            arrow.setVelocity(velocity);
            arrow.setDamage(0.0);
            arrow.setFireTicks(1_200);
            arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            arrow.getPersistentDataContainer().set(illuminationArrowKey, PersistentDataType.BYTE, (byte) 1);
        }

        markUsed(unit, "illumination", illuminationCooldownSeconds(unit));
        announceSkillCast(unit, owner, skill, Particle.FLAME);
    }

    private double illuminationCooldownSeconds(Unit unit) {
        double cooldown = skillOptionDouble(unit, "ILLUMINATION", "cooldown-seconds", 30.0, 1.0, 3600.0);
        double reduction = skillOptionDouble(unit, "ILLUMINATION",
            "cooldown-reduction-per-level-seconds", 0.2, 0.0, 30.0) * (unit.level - 1);
        return Math.max(1.0, cooldown - reduction);
    }

    private boolean canUseIlluminationInCurrentLight(Mob mob) {
        if (!settings.getBoolean("classes.ranger.skill-settings.ILLUMINATION.darkness-only", true)) return true;
        long time = mob.getWorld().getTime();
        if (time >= 13_000L && time <= 23_000L) return true;
        int threshold = (int) readBoundedDouble(settings,
            "classes.ranger.skill-settings.ILLUMINATION.low-light-threshold", 7.0, 0.0, 15.0);
        return mob.getLocation().getBlock().getLightLevel() <= threshold;
    }

    @EventHandler
    public void onIlluminationArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)
                || !arrow.getPersistentDataContainer().has(illuminationArrowKey, PersistentDataType.BYTE)) return;
        Block hitBlock = event.getHitBlock();
        if (hitBlock == null) return;

        Block torchBlock = hitBlock.getRelative(org.bukkit.block.BlockFace.UP);
        if (!hitBlock.getType().isSolid() || !torchBlock.getType().isAir()) return;
        torchBlock.setType(Material.TORCH, false);
        int duration = (int) skillOptionDoubleForRanger(
            "ILLUMINATION", "torch-duration-seconds", 30.0, 1.0, 600.0);
        TemporaryTorch temporaryTorch = new TemporaryTorch(UUID.randomUUID(),
            torchBlock.getWorld().getName(), torchBlock.getX(), torchBlock.getY(), torchBlock.getZ(),
            System.currentTimeMillis() + duration * 1000L);
        temporaryTorches.put(temporaryTorch.id, temporaryTorch);
        saveTemporaryTorches();
        scheduleTorchRemoval(temporaryTorch);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTemporaryTorchBreak(BlockBreakEvent event) {
        if (isTemporaryTorchOrSupport(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTemporaryTorchPhysics(BlockPhysicsEvent event) {
        if (isTemporaryTorch(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTemporaryTorchWaterFlow(BlockFromToEvent event) {
        if (isTemporaryTorch(event.getToBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTemporaryTorchEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isTemporaryTorchOrSupport);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTemporaryTorchBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isTemporaryTorchOrSupport);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTemporaryTorchPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isTemporaryTorchOrSupport)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTemporaryTorchPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isTemporaryTorchOrSupport)) event.setCancelled(true);
    }

    private boolean isTemporaryTorch(Block block) {
        return temporaryTorches.values().stream().anyMatch(torch ->
            torch.worldName.equals(block.getWorld().getName())
                && torch.x == block.getX() && torch.y == block.getY() && torch.z == block.getZ());
    }

    private boolean isTemporaryTorchOrSupport(Block block) {
        return temporaryTorches.values().stream().anyMatch(torch ->
            torch.worldName.equals(block.getWorld().getName())
                && torch.x == block.getX() && torch.z == block.getZ()
                && (torch.y == block.getY() || torch.y - 1 == block.getY()));
    }

    private double skillOptionDoubleForRanger(String skill, String key, double fallback,
                                               double minimum, double maximum) {
        return readBoundedDouble(settings, "classes.ranger.skill-settings." + skill + "." + key,
            fallback, minimum, maximum);
    }

    @EventHandler
    public void onTemporaryTorchChunkLoad(ChunkLoadEvent event) {
        for (TemporaryTorch temporaryTorch : temporaryTorches.values()) {
            if (temporaryTorch.worldName.equals(event.getWorld().getName())
                    && temporaryTorch.x >> 4 == event.getChunk().getX()
                    && temporaryTorch.z >> 4 == event.getChunk().getZ()) {
                scheduleTorchRemoval(temporaryTorch);
            }
        }
    }

    private void loadTemporaryTorches() {
        if (!torchFile.isFile()) return;
        YamlConfiguration loaded = new YamlConfiguration();
        try {
            loaded.load(torchFile);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.SEVERE, "[Army] Could not load temporary Ranger torches.", exception);
            return;
        }
        if (!loaded.isConfigurationSection("torches")) return;
        for (String id : loaded.getConfigurationSection("torches").getKeys(false)) {
            try {
                TemporaryTorch torch = new TemporaryTorch(UUID.fromString(id),
                    loaded.getString("torches." + id + ".world", ""),
                    loaded.getInt("torches." + id + ".x"), loaded.getInt("torches." + id + ".y"),
                    loaded.getInt("torches." + id + ".z"), loaded.getLong("torches." + id + ".expiresAt"));
                temporaryTorches.put(torch.id, torch);
                scheduleTorchRemoval(torch);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("[Army] Ignoring invalid temporary Ranger torch ID: " + id);
            }
        }
    }

    private void scheduleTorchRemoval(TemporaryTorch torch) {
        if (torchRemovalTasks.containsKey(torch.id)) return;
        World world = Bukkit.getWorld(torch.worldName);
        if (world == null || !world.isChunkLoaded(torch.x >> 4, torch.z >> 4)) return;
        long delay = Math.max(1L, (torch.expiresAt - System.currentTimeMillis() + 49L) / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> expireTemporaryTorch(torch), delay);
        torchRemovalTasks.put(torch.id, task);
    }

    private void expireTemporaryTorch(TemporaryTorch torch) {
        torchRemovalTasks.remove(torch.id);
        World world = Bukkit.getWorld(torch.worldName);
        if (world == null || !world.isChunkLoaded(torch.x >> 4, torch.z >> 4)) return;
        Block block = world.getBlockAt(torch.x, torch.y, torch.z);
        if (block.getType() == Material.TORCH) block.setType(Material.AIR, false);
        temporaryTorches.remove(torch.id);
        saveTemporaryTorches();
    }

    private void saveTemporaryTorches() {
        YamlConfiguration output = new YamlConfiguration();
        for (TemporaryTorch torch : temporaryTorches.values()) {
            String path = "torches." + torch.id;
            output.set(path + ".world", torch.worldName);
            output.set(path + ".x", torch.x);
            output.set(path + ".y", torch.y);
            output.set(path + ".z", torch.z);
            output.set(path + ".expiresAt", torch.expiresAt);
        }
        try {
            output.save(torchFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "[Army] Could not save temporary Ranger torch data.", exception);
        }
    }

    private void announceSkillCast(Unit unit, Player owner, String skill, Particle particle) {
        Mob mob = activeEntities.get(unit.id);
        if (mob != null && mob.isValid()) {
            Location effectLocation = mob.getLocation().add(0, 1, 0);
            mob.getWorld().spawnParticle(particle, effectLocation, 14, 0.35, 0.45, 0.35, 0.05);
        }
        owner.sendActionBar("§a" + displayName(unit) + " §eđã dùng kỹ năng §b" + skillDisplayName(skill));
    }

    private boolean applyConfiguredBuff(Unit unit, LivingEntity target, String skill, String cooldownKey) {
        String configured = settings.getString("classes." + unit.unitClass + ".skill-settings."
            + skill + ".effect", ownerBuff);
        String effectName = configured == null || configured.isBlank()
            ? ownerBuff : configured.toUpperCase(Locale.ROOT);
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effectName.toLowerCase(Locale.ROOT)));
        if (type == null) {
            plugin.getLogger().warning("[Army] Unknown " + skill + " effect '" + effectName
                + "' for class " + unit.unitClass + ".");
            return false;
        }
        if (target.hasPotionEffect(type)) return false;
        int seconds = (int) Math.min(600, Math.round(skillOptionDouble(unit, skill,
            "duration-seconds", ownerBuffTicks / 20.0, 1.0, 600.0) * skillPowerMultiplier(unit)));
        int amplifier = (int) skillOptionDouble(unit, skill, "amplifier", 0.0, 0.0, 10.0);
        target.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier, true, true, true));
        markUsed(unit, cooldownKey, skillCooldownSeconds(unit, skill));
        if (skill.equalsIgnoreCase("BUFF_OWNER")) {
            Player owner = Bukkit.getPlayer(unit.owner);
            if (owner != null) announceSkillCast(unit, owner, skill, Particle.ENCHANT);
        }
        return true;
    }

    private double skillOptionDouble(Unit unit, String skill, String option, double fallback,
                                         double minimum, double maximum) {
        String path = "classes." + unit.unitClass + ".skill-settings." + skill + "." + option;
        return readBoundedDouble(settings, path, fallback, minimum, maximum);
    }

    private double skillCooldownSeconds(Unit unit, String skill) {
        double configured = readBoundedInt(settings, "classes." + unit.unitClass + ".skill-settings."
            + skill + ".cooldown-seconds", settings.getInt("skills.cooldown-seconds", 15), 1, 3600);
        double levelProgress = maxLevel <= 1 ? 0.0 : (double) (unit.level - 1) / (maxLevel - 1);
        return Math.max(1.0, configured - skillCooldownReductionAtMaxLevel * levelProgress);
    }

    private String skillOptionString(Unit unit, String skill, String option, String fallback) {
        String value = settings.getString("classes." + unit.unitClass + ".skill-settings."
            + skill + "." + option, fallback);
        if (value == null || value.isBlank()) return fallback;
        return value.toUpperCase(Locale.ROOT);
    }

    private void applyOwnerShield(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player owner)) return;
        boolean hostile = event.getDamager() instanceof Monster;
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Monster) hostile = true;
        if (!hostile) return;
        for (Unit unit : getOwnerUnits(owner.getUniqueId())) {
            Mob mob = activeEntities.get(unit.id);
            if (!unit.deployed || mob == null || !mob.isValid() || !hasSkill(unit, "SHIELD_OWNER")
                        || !mob.getWorld().equals(owner.getWorld())) continue;
            double radius = skillOptionDouble(unit, "SHIELD_OWNER", "radius-blocks", 8.0, 1.0, 32.0);
            if (mob.getLocation().distanceSquared(owner.getLocation()) > radius * radius
                        || !ready(unit, "shield-owner")) continue;
            double reduction = Math.min(0.9, skillOptionDouble(unit, "SHIELD_OWNER",
                "damage-reduction", shieldReduction, 0.0, 0.9) * skillPowerMultiplier(unit));
            event.setDamage(event.getDamage() * (1.0 - reduction));
            markUsed(unit, "shield-owner", skillCooldownSeconds(unit, "SHIELD_OWNER"));
            announceSkillCast(unit, owner, "SHIELD_OWNER", Particle.WAX_ON);
            return;
        }
    }

    private boolean ready(Unit unit, String skill) {
        return skillCooldowns.computeIfAbsent(unit.id, ignored -> new HashMap<>())
            .getOrDefault(skill, 0L) <= System.currentTimeMillis();
    }

    private void markUsed(Unit unit, String skill, double cooldownSeconds) {
        skillCooldowns.computeIfAbsent(unit.id, ignored -> new HashMap<>())
            .put(skill, System.currentTimeMillis() + Math.round(cooldownSeconds * 1000.0));
    }

    private boolean hasSkill(Unit unit, String skill) {
        List<String> skills = settings.getStringList("classes." + unit.unitClass + ".skills");
        return skills.stream().anyMatch(value -> value.equalsIgnoreCase(skill));
    }

    private boolean isHostileMob(LivingEntity target) {
        if (target == null || !target.isValid() || target.isDead()) return false;
        if (target instanceof Player || isMercenary(target)) return false;
        if (target instanceof Monster) return true;
        return target instanceof org.bukkit.entity.EnderDragon
            || target instanceof org.bukkit.entity.Ghast
            || target instanceof org.bukkit.entity.Phantom
            || target instanceof org.bukkit.entity.Wither
            || target instanceof org.bukkit.entity.Slime
            || target instanceof org.bukkit.entity.MagmaCube;
    }

    private boolean isValidTarget(LivingEntity target) {
        return isHostileMob(target);
    }

    private boolean isAllowedMercenaryTarget(UUID ownerId, LivingEntity target, boolean ownerCommandedTarget) {
        if (target == null || !target.isValid() || target.isDead()) return false;
        if (target.getUniqueId().equals(ownerId)) return false;
        if (target instanceof Player || isMercenary(target)) {
            return canMercenaryDamageTarget(ownerId, target)
                && ownerCommandedTarget
                && isActiveOwnerAttackTarget(ownerId, target.getUniqueId());
        }
        return isHostileMob(target);
    }

    private boolean canMinionTarget(Mob minion, LivingEntity target) {
        UUID ownerId = getOwnerId(minion);
        return target != null && isAllowedMinionVictim(minion, target)
            && (isHostileMob(target) || isRetaliationTarget(ownerId, target))
            && minion.hasLineOfSight(target);
    }

    private int maxSummonedMinions(Unit unit) {
        int configuredMaximum = (int) skillOptionDouble(unit, "SUMMON_VEXES", "max-active", 7.0, 1.0, 10.0);
        return Math.min(configuredMaximum, 1 + unit.level / 5);
    }

    private boolean isAllowedMinionVictim(Mob minion, LivingEntity target) {
        UUID ownerId = getOwnerId(minion);
        if (target == null || ownerId == null || target.getUniqueId().equals(ownerId)) return false;
        if (target instanceof Player || isMercenary(target)) {
            return canMercenaryDamageTarget(ownerId, target)
                && isActiveOwnerAttackTarget(ownerId, target.getUniqueId());
        }
        return isHostileMob(target);
    }

    private boolean canMercenaryDamageTarget(UUID sourceOwnerId, LivingEntity target) {
        if (target == null) return false;
        if (!(target instanceof Player) && !isMercenary(target)) return true;
        if (sourceOwnerId == null) return false;
        UUID targetOwnerId = target instanceof Player player
            ? player.getUniqueId() : getOwnerId(target);
        if (sourceOwnerId.equals(targetOwnerId)) return false;
        return isOwnerCommandedPvpTarget(sourceOwnerId, target);
    }

    private boolean isOwnerCommandedPvpTarget(UUID ownerId, LivingEntity target) {
        return ownerCommandedPvpEnabled && target != null
            && isActiveOwnerAttackTarget(ownerId, target.getUniqueId());
    }

    private Entity resolveDamageSource(Entity damager) {
        if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        return damager;
    }

    private void registerOwnerThreat(UUID ownerId, Entity source) {
        if (ownerId == null || source == null) return;
        registerThreat(ownerThreats, ownerId, source);
        UUID sourceOwner = getOwnerId(source);
        if (sourceOwner != null && !sourceOwner.equals(ownerId)) {
            ownerThreats.get(ownerId).put(sourceOwner, System.currentTimeMillis());
        }
    }

    private void registerThreat(Map<UUID, Map<UUID, Long>> threatMap, UUID subjectId, Entity source) {
        if (subjectId == null || source == null) return;
        Map<UUID, Long> threats = threatMap.computeIfAbsent(subjectId, ignored -> new HashMap<>());
        threats.put(source.getUniqueId(), System.currentTimeMillis());
    }

    private boolean isRetaliationTarget(UUID ownerId, LivingEntity target) {
        if (ownerId == null || target == null) return false;
        if (target instanceof Player || isMercenary(target)) {
            return isOwnerCommandedPvpTarget(ownerId, target);
        }
        Map<UUID, Long> threats = ownerThreats.get(ownerId);
        long now = System.currentTimeMillis();
        if (threats != null) {
            threats.entrySet().removeIf(entry -> now - entry.getValue() > 15000L);
            if (threats.containsKey(target.getUniqueId())) return true;
            UUID targetOwner = getOwnerId(target);
            if (targetOwner != null && threats.containsKey(targetOwner)) return true;
        }
        return isActiveOwnerAttackTarget(ownerId, target.getUniqueId());
    }

    private void sanitizeMobAi(Mob mob) {
        if (mob instanceof Villager) {
            mob.setAware(true);
            mob.setTarget(null);
        }
    }

    private boolean addExperience(Unit unit, int amount) {
        if (amount <= 0 || unit.level >= maxLevel) return false;
        unit.experience = (int) Math.min(Integer.MAX_VALUE, (long) unit.experience + amount);
        boolean leveledUp = false;
        while (unit.level < maxLevel && unit.experience >= nextLevelExperience(unit)) {
            unit.experience -= nextLevelExperience(unit);
            unit.level++;
            leveledUp = true;
        }
        if (leveledUp) {
            Mob mob = activeEntities.get(unit.id);
            if (mob != null && mob.isValid()) {
                applyStats(unit, mob);
                updateEntityName(unit, mob);
                mob.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, mob.getLocation().add(0, 1, 0),
                    18, 0.45, 0.5, 0.45, 0.02);
                mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.35f);
            }
            Player owner = Bukkit.getPlayer(unit.owner);
            if (owner != null) owner.sendMessage("§aLính §e" + displayName(unit) + "§a lên cấp §f"
                + unit.level + "§a (Bậc " + unit.tier + ").");
        }
        return true;
    }

    private double skillPowerMultiplier(Unit unit) {
        return Math.min(maxSkillPowerMultiplier,
            1.0 + Math.max(0, unit.level - 1) * skillPowerPerLevel);
    }

    private UUID getOwnerId(Entity entity) {
        String value = entity.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private UUID getUnitId(Entity entity) {
        String value = entity.getPersistentDataContainer().get(unitKey, PersistentDataType.STRING);
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void hire(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage("§eSử dụng: /vnarmy hire <loại> <bậc>");
            return;
        }
        String classId = normalizeClass(args[1]);
        int tier = parsePositiveInt(args[2]);
        if (classId == null || !hasTier(tier)) {
            player.sendMessage("§cLoại lính hoặc bậc không hợp lệ. Dùng /vnarmy để xem danh sách.");
            return;
        }
        List<Unit> owned = getOwnerUnits(player.getUniqueId());
        if (owned.size() >= maxUnits) {
            player.sendMessage("§cĐã đạt giới hạn sở hữu " + maxUnits + " lính đánh thuê.");
            return;
        }
        ItemStack token = findToken(player, classId, tier);
        int xpCost = settings.getInt("tiers." + tier + ".experience-levels", -1);
        ConfigurationSection materialCosts = settings.getConfigurationSection("tiers." + tier + ".materials");
        if (xpCost < 0 || materialCosts == null) {
            player.sendMessage("§cCấu hình chi phí Bậc " + tier + " không hợp lệ; chưa trừ tài nguyên.");
            plugin.getLogger().warning("[Army] Invalid cost configuration for tier " + tier + ".");
            return;
        }
        Map<Material, Integer> costs = readMaterialCosts(materialCosts, player, "thuê Bậc " + tier);
        if (costs == null) return;
        List<String> missing = missingMaterials(player, costs);
        if (token == null) {
            missing.add(0, "Lệnh Triệu Hồi " + classDisplay(classId) + " Bậc " + tier);
        }
        if (player.getLevel() < xpCost) {
            missing.add("Cấp kinh nghiệm: cần " + xpCost + ", hiện có " + player.getLevel());
        }
        if (!missing.isEmpty()) {
            player.sendMessage("§cThiếu điều kiện để thuê lính Bậc " + tier + ":");
            missing.forEach(value -> player.sendMessage("§7- " + value));
            return;
        }

        ItemStack[] inventoryBefore = cloneContents(player.getInventory().getContents());
        int levelsBefore = player.getLevel();
        if (!removeOne(player, token) || !removeMaterials(player, costs)) {
            player.getInventory().setContents(inventoryBefore);
            player.sendMessage("§cKhông thể trừ đủ vật phẩm; giao dịch đã được hoàn tác.");
            return;
        }
        player.setLevel(levelsBefore - xpCost);
        boolean deployImmediately = deployedUnitCount(player.getUniqueId()) < maxDeployedUnits;
        Unit unit = new Unit(UUID.randomUUID(), player.getUniqueId(), classId, tier,
            Mode.PROTECT, deployImmediately);
        Location spawnAt = player.getLocation().clone().add(0.8, 0.0, 0.8);
        Mob mob = deployImmediately ? spawnUnit(unit, spawnAt) : null;
        if (deployImmediately && mob == null) {
            player.getInventory().setContents(inventoryBefore);
            player.setLevel(levelsBefore);
            player.sendMessage("§cKhông thể tạo lính; toàn bộ chi phí đã được hoàn lại.");
            return;
        }
        units.put(unit.id, unit);
        if (mob != null) {
            activeEntities.put(unit.id, mob);
            updateLocation(unit, mob.getLocation());
        }
        if (!saveData()) {
            units.remove(unit.id);
            removeUnitData(unit);
            if (mob != null) {
                activeEntities.remove(unit.id);
                mob.remove();
            }
            player.getInventory().setContents(inventoryBefore);
            player.setLevel(levelsBefore);
            saveData();
            player.sendMessage("§cKhông thể lưu lính vào mercenaries.yml; chi phí đã được hoàn lại.");
            return;
        }
        player.sendMessage("§aĐã thuê §e" + displayName(unit) + " §aBậc " + tier
            + " (§7ID " + shortId(unit.id) + "§a)."
            + (deployImmediately ? "" : " §7Lính đang nghỉ vì đã đủ giới hạn triệu hồi.")
            + " Dùng /vnarmy gear " + shortId(unit.id) + " để trang bị.");
    }

    private void recall(Player player, String[] args) {
        List<Unit> requested = selectedUnits(player, args, "recall");
        if (requested == null) return;
        if (requested.isEmpty()) {
            player.sendMessage("§7Bạn chưa sở hữu lính đánh thuê nào.");
            return;
        }
        boolean summonAll = args.length == 1;
        List<Unit> candidates = summonAll
            ? getOwnerUnits(player.getUniqueId()).stream()
                .filter(unit -> unit.recoveryUntilMillis <= System.currentTimeMillis())
                .sorted(summonPriority())
                .limit(maxDeployedUnits)
                .toList()
            : requested;
        Set<UUID> selectedIds = candidates.stream().map(unit -> unit.id)
            .collect(java.util.stream.Collectors.toSet());
        if (summonAll) {
            for (Unit unit : getOwnerUnits(player.getUniqueId())) {
                if (unit.deployed && unit.recoveryUntilMillis <= System.currentTimeMillis()
                        && !selectedIds.contains(unit.id)) {
                    restForQuickRecall(player, unit);
                }
            }
        } else if (candidates.get(0).recoveryUntilMillis <= System.currentTimeMillis()
                && !candidates.get(0).deployed
                && deployedUnitCount(player.getUniqueId()) >= maxDeployedUnits) {
            player.sendMessage("§cBạn đã đạt giới hạn triệu hồi " + maxDeployedUnits
                + " lính. Hãy cho một lính nghỉ trước.");
            return;
        }

        int restored = 0;
        int recovering = (int) (summonAll ? getOwnerUnits(player.getUniqueId()).stream()
            : candidates.stream()).filter(unit -> unit.recoveryUntilMillis > System.currentTimeMillis()).count();
        for (Unit unit : candidates) {
            if (unit.recoveryUntilMillis > System.currentTimeMillis()) {
                player.sendMessage("§e" + displayName(unit) + " §7đang hồi phục, còn "
                    + remainingRecoverySeconds(unit) + " giây.");
                continue;
            }
            if (!unit.deployed && deployedUnitCount(player.getUniqueId()) >= maxDeployedUnits) {
                player.sendMessage("§cKhông thể gọi thêm lính: đã đạt giới hạn triệu hồi "
                    + maxDeployedUnits + ".");
                continue;
            }
            unit.deployed = true;
            Mob mob = findExistingEntity(unit);
            if (mob == null) mob = spawnUnit(unit, player.getLocation().clone().add(0.8, 0.0, 0.8));
            if (mob == null) {
                unit.deployed = false;
                player.sendMessage("§cKhông thể triệu hồi lính §e#" + shortId(unit.id) + "§c.");
                continue;
            }
            mob.teleport(player.getLocation().clone().add(0.8, 0.0, 0.8));
            mob.setTarget(null);
            ensurePaladinMounted(unit, mob);
            activeEntities.put(unit.id, mob);
            updateLocation(unit, mob.getLocation());
            restored++;
        }
        if (!saveData()) {
            player.sendMessage("§cLính đã được gọi ra nhưng không thể lưu trạng thái; kiểm tra log máy chủ.");
            plugin.getLogger().severe("[Army] Could not save recall state for owner " + player.getUniqueId() + ".");
            return;
        }
        player.sendMessage("§aĐã triệu hồi " + restored + "/" + candidates.size() + " lính đánh thuê."
            + (recovering > 0 ? " §7(" + recovering + " đang hồi phục)" : ""));
    }

    private void restoreDeployedUnits(Player player) {
        if (!enabled || !player.isOnline()) return;
        List<Unit> owned = getOwnerUnits(player.getUniqueId()).stream()
            .filter(unit -> unit.deployed && unit.recoveryUntilMillis <= System.currentTimeMillis())
            .sorted(summonPriority())
            .toList();
        boolean changed = false;
        for (int index = 0; index < owned.size(); index++) {
            Unit unit = owned.get(index);
            if (index >= maxDeployedUnits) {
                changed |= restForQuickRecall(player, unit);
                continue;
            }
            Mob mob = findExistingEntity(unit);
            if (mob == null) mob = spawnUnit(unit, player.getLocation().clone().add(0.8, 0.0, 0.8));
            if (mob == null) {
                unit.deployed = false;
                changed = true;
                plugin.getLogger().warning("[Army] Could not restore unit " + unit.id
                    + " for owner " + player.getUniqueId() + ".");
            } else {
                activeEntities.put(unit.id, mob);
                updateLocation(unit, mob.getLocation());
            }
        }
        if (changed || !owned.isEmpty()) saveData();
    }

    private Comparator<Unit> summonPriority() {
        return Comparator.comparingInt((Unit unit) -> unit.tier).reversed()
            .thenComparing(Comparator.comparingInt((Unit unit) -> unit.level).reversed())
            .thenComparing(unit -> unit.id.toString());
    }

    private int deployedUnitCount(UUID ownerId) {
        long now = System.currentTimeMillis();
        return (int) getOwnerUnits(ownerId).stream()
            .filter(unit -> unit.deployed && unit.recoveryUntilMillis <= now)
            .count();
    }

    private boolean restForQuickRecall(Player player, Unit unit) {
        Mob mob = activeEntities.get(unit.id);
        if (mob == null || !mob.isValid()) mob = findExistingEntity(unit);
        boolean wasDeployed = unit.deployed;
        boolean previousHorseDead = unit.horseDead;
        UUID previousEntityId = unit.entityId;
        if (mob != null && mob.isValid()) {
            captureEquipment(unit, mob);
            updateLocation(unit, mob.getLocation());
        }
        unit.deployed = false;
        unit.horseDead = false;
        unit.entityId = null;
        if (!saveData()) {
            unit.deployed = wasDeployed;
            unit.horseDead = previousHorseDead;
            unit.entityId = previousEntityId;
            player.sendMessage("§cKhông thể lưu trạng thái nghỉ của lính §e#" + shortId(unit.id)
                + "§c; lính vẫn được giữ nguyên.");
            plugin.getLogger().severe("[Army] Could not save deployment-limit rest for unit " + unit.id + ".");
            return false;
        }
        cleanupSummonedMinions(unit.id);
        removeMount(unit.id, mob);
        if (mob != null && mob.isValid()) mob.remove();
        activeEntities.remove(unit.id);
        return true;
    }

    private void rest(Player player, String[] args) {
        List<Unit> owned = selectedUnits(player, args, "rest");
        if (owned == null) return;
        if (owned.isEmpty()) {
            player.sendMessage("§7Bạn chưa sở hữu lính đánh thuê nào.");
            return;
        }
        int rested = 0;
        for (Unit unit : owned) {
            if (unit.recoveryUntilMillis > System.currentTimeMillis()) continue;
            Mob mob = activeEntities.get(unit.id);
            if (mob == null || !mob.isValid()) mob = findExistingEntity(unit);
            if (mob != null && mob.isValid()) {
                activeEntities.put(unit.id, mob);
                captureEquipment(unit, mob);
                updateLocation(unit, mob.getLocation());
                boolean previousHorseDead = unit.horseDead;
                unit.horseDead = false;
                unit.entityId = null;
                unit.deployed = false;
                if (!saveData()) {
                    unit.horseDead = previousHorseDead;
                    unit.deployed = true;
                    unit.entityId = mob.getUniqueId();
                    applyEquipment(unit, mob);
                    player.sendMessage("§cKhông thể lưu trạng thái nghỉ của lính §e#" + shortId(unit.id)
                        + "§c; lính vẫn được giữ nguyên.");
                    continue;
                }
                cleanupSummonedMinions(unit.id);
                removeMount(unit.id, mob);
                mob.remove();
                activeEntities.remove(unit.id);
                rested++;
            } else if (unit.deployed) {
                boolean previousHorseDead = unit.horseDead;
                unit.horseDead = false;
                unit.deployed = false;
                unit.entityId = null;
                if (saveData()) {
                    cleanupSummonedMinions(unit.id);
                    removeMount(unit.id, null);
                    rested++;
                } else {
                    unit.horseDead = previousHorseDead;
                    unit.deployed = true;
                    player.sendMessage("§cKhông thể lưu trạng thái nghỉ của lính §e#" + shortId(unit.id) + "§c.");
                }
            }
        }
        player.sendMessage(rested == 0 ? "§7Không có lính nào đang triển khai."
            : "§aĐã cho " + rested + " lính nghỉ. Trang bị được lưu.");
    }

    private List<Unit> selectedUnits(Player player, String[] args, String commandName) {
        if (args.length > 2) {
            player.sendMessage("§eSử dụng: /vnarmy " + commandName + " [ID]");
            return null;
        }
        List<Unit> owned = getOwnerUnits(player.getUniqueId());
        if (args.length == 1) return owned;
        Unit unit = findOwnedByShortId(player.getUniqueId(), args[1]);
        if (unit == null) {
            player.sendMessage("§cKhông tìm thấy lính của bạn với ID bắt đầu bằng §e" + args[1] + "§c.");
            return null;
        }
        return List.of(unit);
    }

    private void despawnForOwner(UUID ownerId) {
        boolean changed = false;
        for (Unit unit : getOwnerUnits(ownerId)) {
            Mob mob = activeEntities.remove(unit.id);
            if (mob == null || !mob.isValid()) mob = findExistingEntity(unit);
            if (mob == null || !mob.isValid()) continue;
            captureEquipment(unit, mob);
            updateLocation(unit, mob.getLocation());
            unit.entityId = null;
            removeMount(unit.id, mob);
            mob.remove();
            activeEntities.remove(unit.id);
            changed = true;
        }
        if (changed && !saveData()) {
            plugin.getLogger().severe("[Army] Could not persist mercenary state on player logout for " + ownerId + ".");
        }
    }

    private void removeMount(UUID unitId, Mob rider) {
        UUID mountId = mountByUnit.remove(unitId);
        Entity mount = mountId == null ? null : Bukkit.getEntity(mountId);
        if (mount == null && rider != null && rider.getVehicle() instanceof Horse) {
            mount = rider.getVehicle();
        }
        if (mount instanceof Horse horse && horse.isValid()) {
            captureHorseArmor(unitId, horse);
            horse.remove();
        }
    }

    private void ensurePaladinMounted(Unit unit, Mob rider) {
        if (!(rider instanceof Zombie) || !"paladin".equals(unit.unitClass) || unit.horseDead) return;
        if (rider.getVehicle() instanceof Horse current && isUnitHorse(current, unit)) {
            mountByUnit.put(unit.id, current.getUniqueId());
            applyHorseStatsAndArmor(unit, current);
            return;
        }
        Horse horse = getOrCreateUnitHorse(unit, rider.getLocation());
        if (horse == null) return;
        if (horse.getPassengers().isEmpty() || horse.getPassengers().contains(rider)) {
            horse.addPassenger(rider);
            applyHorseStatsAndArmor(unit, horse);
        }
    }

    private Horse getOrCreateUnitHorse(Unit unit, Location location) {
        if (unit.horseDead) return null;
        Entity tracked = mountByUnit.containsKey(unit.id) ? Bukkit.getEntity(mountByUnit.get(unit.id)) : null;
        if (tracked instanceof Horse horse && horse.isValid() && isUnitHorse(horse, unit)
                && (horse.getPassengers().isEmpty() || horse.getPassengers().stream()
                    .anyMatch(passenger -> isUnit(passenger, unit.id)))) {
            applyHorseStatsAndArmor(unit, horse);
            return horse;
        }
        mountByUnit.remove(unit.id);
        for (Entity nearby : location.getWorld().getNearbyEntities(location, 12.0, 8.0, 12.0)) {
            if (nearby instanceof Horse horse && horse.isValid() && isUnitHorse(horse, unit)
                    && (horse.getPassengers().isEmpty() || horse.getPassengers().stream()
                        .anyMatch(passenger -> isUnit(passenger, unit.id)))) {
                mountByUnit.put(unit.id, horse.getUniqueId());
                applyHorseStatsAndArmor(unit, horse);
                return horse;
            }
        }
        Entity created = location.getWorld().spawnEntity(location.clone().add(1.2, 0.0, 0.0), EntityType.HORSE);
        if (!(created instanceof Horse horse)) {
            created.remove();
            return null;
        }
        horse.setPersistent(false);
        horse.setRemoveWhenFarAway(false);
        horse.setTamed(true);
        horse.getPersistentDataContainer().set(horseMountKey, PersistentDataType.STRING, unit.id.toString());
        horse.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, unit.owner.toString());
        mountByUnit.put(unit.id, horse.getUniqueId());
        applyHorseStatsAndArmor(unit, horse);
        return horse;
    }

    private boolean isUnitHorse(Horse horse, Unit unit) {
        return unit.id.toString().equals(horse.getPersistentDataContainer()
            .get(horseMountKey, PersistentDataType.STRING));
    }

    private void applyHorseStatsAndArmor(Unit unit, Horse horse) {
        double horseHealth = readBoundedDouble(settings, "classes.paladin.horse-health", 50.0, 1.0, 1024.0);
        double horseSpeed = readBoundedDouble(settings, "classes.paladin.horse-speed", 0.3375, 0.1, 1.0);
        if (horse.getAttribute(Attribute.MAX_HEALTH) != null) {
            double previousMaximum = horse.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            horse.getAttribute(Attribute.MAX_HEALTH).setBaseValue(horseHealth);
            double currentHealth = horse.getHealth();
            horse.setHealth(currentHealth >= previousMaximum
                ? horseHealth : Math.min(currentHealth, horseHealth));
        }
        if (horse.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(horseSpeed);
        }
        if (isHorseArmor(horse.getInventory().getArmor())) {
            unit.horseArmor = selectBestHorseArmor(unit.horseArmor, horse.getInventory().getArmor());
        }
        horse.getInventory().setArmor(cloneOrNull(unit.horseArmor));
    }

    private void applyHorseArmorToMount(Unit unit) {
        Entity mount = mountByUnit.containsKey(unit.id) ? Bukkit.getEntity(mountByUnit.get(unit.id)) : null;
        if (mount instanceof Horse horse && horse.isValid()) applyHorseStatsAndArmor(unit, horse);
    }

    private void captureHorseArmor(UUID unitId, Horse horse) {
        Unit unit = units.get(unitId);
        ItemStack armor = horse.getInventory().getArmor();
        if (unit != null && isHorseArmor(armor)) {
            unit.horseArmor = selectBestHorseArmor(unit.horseArmor, armor);
        }
    }

    private boolean isHorseArmor(ItemStack item) {
        return item != null && item.getType().name().endsWith("_HORSE_ARMOR");
    }

    private ItemStack selectBestHorseArmor(ItemStack first, ItemStack second) {
        if (!isHorseArmor(first)) return cloneOrNull(second);
        if (!isHorseArmor(second)) return first.clone();
        return horseArmorScore(first) >= horseArmorScore(second) ? first.clone() : second.clone();
    }

    private int horseArmorScore(ItemStack armor) {
        String material = armor.getType().name();
        return material.startsWith("NETHERITE_") ? 5
            : material.startsWith("DIAMOND_") ? 4
            : material.startsWith("GOLDEN_") ? 3
            : material.startsWith("IRON_") ? 2 : 1;
    }

    private Horse getMountedHorse(Unit unit, Zombie rider) {
        if (unit.horseDead) return null;
        UUID mountId = mountByUnit.get(unit.id);
        Entity mount = mountId == null ? null : Bukkit.getEntity(mountId);
        Horse horse;
        if (mount instanceof Horse existing && existing.isValid()) {
            horse = existing;
        } else {
            mountByUnit.remove(unit.id);
            if (!(rider.getVehicle() instanceof Horse attached) || !attached.isValid()) return null;
            horse = attached;
            mountByUnit.put(unit.id, horse.getUniqueId());
        }
        return rider.getVehicle() == horse ? horse : null;
    }

    private void teleportUnit(Unit unit, Mob mob, Location destination) {
        if ("paladin".equals(unit.unitClass) && mob instanceof Zombie rider) {
            Horse horse = getMountedHorse(unit, rider);
            if (horse != null) {
                if (!horse.teleport(destination)) {
                    plugin.getLogger().warning("[Army] Could not teleport horse for mercenary " + unit.id + ".");
                    return;
                }
                if (rider.getVehicle() != horse) horse.addPassenger(rider);
                return;
            }
        }
        mob.teleport(destination);
    }

    private void setMode(Player player, Mode mode) {
        List<Unit> owned = getOwnerUnits(player.getUniqueId());
        if (owned.isEmpty()) {
            player.sendMessage("§7Bạn chưa sở hữu lính đánh thuê nào.");
            return;
        }
        owned.forEach(unit -> unit.mode = mode);
        if (!saveData()) {
            player.sendMessage("§cKhông thể lưu chế độ AI; thử lại sau.");
            return;
        }
        player.sendMessage("§aChế độ của " + owned.size() + " lính đã chuyển thành §e" + modeName(mode) + "§a.");
    }

    private void listUnits(Player player) {
        List<Unit> owned = getOwnerUnits(player.getUniqueId());
        player.sendMessage("§6§lLính đánh thuê §7(Sở hữu " + owned.size() + "/" + maxUnits
            + " | Đang gọi " + deployedUnitCount(player.getUniqueId()) + "/" + maxDeployedUnits + ")");
        if (owned.isEmpty()) {
            player.sendMessage("§7Thuê lính bằng /vnarmy hire <loại> <bậc>.");
            return;
        }
        for (Unit unit : owned) {
            player.sendMessage("§e#" + shortId(unit.id) + " §f" + unitLabel(unit, true)
                + " §7| Bậc " + unit.tier + " | Cấp " + unit.level + " | EXP "
                + unit.experience + "/" + nextLevelExperience(unit) + " | " + modeName(unit.mode)
                + " | " + unitStatus(unit));
        }
    }

    private void listUnits(CommandSender sender, String playerName) {
        UUID ownerId = resolvePlayerId(playerName);
        if (ownerId == null) {
            sender.sendMessage("§cKhông tìm thấy hồ sơ người chơi " + playerName + ".");
            return;
        }
        List<Unit> owned = getOwnerUnits(ownerId);
        sender.sendMessage("§6§lLính của " + playerName + " §7(" + owned.size() + ")");
        if (owned.isEmpty()) {
            sender.sendMessage("§7Người chơi chưa sở hữu lính đánh thuê.");
            return;
        }
        for (Unit unit : owned) {
            sender.sendMessage("§e#" + shortId(unit.id) + " §f" + unitLabel(unit, false)
                + " §7| Bậc " + unit.tier + " | Cấp " + unit.level + " | EXP "
                + unit.experience + "/" + nextLevelExperience(unit) + " | " + modeName(unit.mode)
                + " | " + unitStatus(unit));
        }
    }

    private String unitStatus(Unit unit) {
        if (unit.recoveryUntilMillis > System.currentTimeMillis()) {
            return "§cHồi phục (" + remainingRecoverySeconds(unit) + "s)";
        }
        return unit.deployed ? "§aXuất chiến" : "§7Nghỉ ngơi";
    }

    private long remainingRecoverySeconds(Unit unit) {
        return Math.max(0L, (unit.recoveryUntilMillis - System.currentTimeMillis() + 999L) / 1000L);
    }

    private String unitLabel(Unit unit, boolean ownerView) {
        String ownerName = Bukkit.getOfflinePlayer(unit.owner).getName();
        if (ownerName == null || ownerName.isBlank()) ownerName = "Người chơi";
        return (ownerView ? "§a§l[" : "§b§l[") + ownerName + "]§r §f" + displayName(unit);
    }

    private void rename(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§eSử dụng: /vnarmy rename <ID> <Tên mới>");
            return;
        }
        Unit unit = findOwnedByShortId(player.getUniqueId(), args[1]);
        if (unit == null) {
            player.sendMessage("§cKhông tìm thấy lính của bạn với ID bắt đầu bằng §e" + args[1] + "§c.");
            return;
        }
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
            .replace("§", "").replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isBlank()) {
            player.sendMessage("§cTên lính không được để trống.");
            return;
        }
        if (name.length() > 32) name = name.substring(0, 32);
        String previous = unit.customName;
        unit.customName = name;
        Mob mob = activeEntities.get(unit.id);
        if (mob != null && mob.isValid()) updateEntityName(unit, mob);
        if (!saveData()) {
            unit.customName = previous;
            if (mob != null && mob.isValid()) updateEntityName(unit, mob);
            player.sendMessage("§cKhông thể lưu tên mới; tên lính được khôi phục.");
            return;
        }
        player.sendMessage("§aĐã đổi tên lính thành §f" + name + "§a.");
    }

    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        Unit unit;
        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§eDùng /vnarmy remove <ID> hoặc /vnarmy remove <user> <id>.");
                return true;
            }
            unit = findOwnedByShortId(player.getUniqueId(), args[1]);
            if (unit == null && player.hasPermission(ADMIN_PERMISSION)) unit = findUnitByShortId(args[1]);
        } else if (args.length == 3 && sender.hasPermission(ADMIN_PERMISSION)) {
            UUID ownerId = resolvePlayerId(args[1]);
            unit = ownerId == null ? null : findOwnedByShortId(ownerId, args[2]);
        } else {
            sender.sendMessage("§eDùng /vnarmy remove <ID> hoặc /vnarmy remove <user> <id> (admin).");
            return true;
        }
        if (unit == null) {
            sender.sendMessage("§cKhông tìm thấy lính hoặc ID chưa đủ chính xác.");
            return true;
        }
        units.remove(unit.id);
        removeUnitData(unit);
        if (!saveData()) {
            units.put(unit.id, unit);
            sender.sendMessage("§cKhông lưu được; lính vẫn được giữ nguyên.");
            return true;
        }
        Mob mob = activeEntities.remove(unit.id);
        if (mob != null && mob.isValid()) mob.remove();
        skillCooldowns.remove(unit.id);
        sender.sendMessage("§aĐã xóa lính §e#" + shortId(unit.id) + "§a.");
        return true;
    }

    private boolean handleClearCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§cBạn không có quyền xóa toàn bộ lính của người chơi.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage("§eDùng /vnarmy clear <user>.");
            return true;
        }
        UUID ownerId = resolvePlayerId(args[1]);
        if (ownerId == null) {
            sender.sendMessage("§cKhông tìm thấy hồ sơ người chơi " + args[1] + ".");
            return true;
        }
        List<Unit> removed = getOwnerUnits(ownerId);
        removed.forEach(unit -> {
            units.remove(unit.id);
            removeUnitData(unit);
        });
        if (!saveData()) {
            removed.forEach(unit -> units.put(unit.id, unit));
            sender.sendMessage("§cKhông lưu được; dữ liệu lính đã được giữ lại trong bộ nhớ.");
            return true;
        }
        for (Unit unit : removed) {
            Mob mob = activeEntities.remove(unit.id);
            if (mob != null && mob.isValid()) mob.remove();
            skillCooldowns.remove(unit.id);
        }
        sender.sendMessage("§aĐã xóa " + removed.size() + " lính của " + args[1] + ".");
        return true;
    }

    private UUID resolvePlayerId(String playerName) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) return online.getUniqueId();
        return java.util.Arrays.stream(Bukkit.getOfflinePlayers())
            .filter(player -> playerName.equalsIgnoreCase(player.getName()))
            .map(org.bukkit.OfflinePlayer::getUniqueId).findFirst().orElse(null);
    }

    private Unit findUnitByShortId(String shortId) {
        String prefix = shortId.toLowerCase(Locale.ROOT);
        List<Unit> matches = units.values().stream()
            .filter(unit -> unit.id.toString().startsWith(prefix)).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private void toggleMuteCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§eDùng /vnarmy mute <ID>.");
            return;
        }
        Unit unit = findOwnedByShortId(player.getUniqueId(), args[1]);
        if (unit == null) {
            player.sendMessage("§cKhông tìm thấy lính với ID đó.");
            return;
        }
        toggleMute(player, unit);
    }

    private void toggleMute(Player player, Unit unit) {
        boolean previous = unit.muted;
        unit.muted = !previous;
        Mob mob = activeEntities.get(unit.id);
        if (mob != null && mob.isValid()) mob.setSilent(unit.muted);
        if (!saveData()) {
            unit.muted = previous;
            if (mob != null && mob.isValid()) mob.setSilent(previous);
            player.sendMessage("§cKhông lưu được thiết lập âm thanh.");
            return;
        }
        player.sendMessage(unit.muted ? "§aĐã tắt âm thanh lính." : "§aĐã bật âm thanh lính.");
        openUnitMenu(player, unit);
    }

    private ServiceMenuHolder createServiceMenu(Player player, MenuType type, UUID target,
                                                String itemType, String classId, int page,
                                                int size, String title) {
        ServiceMenuHolder holder = new ServiceMenuHolder(player.getUniqueId(), type, target, itemType, classId, page);
        holder.inventory = Bukkit.createInventory(holder, size, title);
        return holder;
    }

    private void openMainMenu(Player player) {
        if (!plugin.getConfig().getBoolean("menus.main.enabled", true)) {
            player.sendMessage("§cMenu chính hiện đang tắt.");
            return;
        }
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.MAIN, null, null, null, 0,
            27, "VnMine • Menu chính");
        Inventory inventory = holder.inventory;
        if (player.hasPermission("vnmine.command.stats")) inventory.setItem(10,
            menuItem(Material.BOOK, "§eThống kê", List.of("§7Xem thống kê sinh tồn của bạn.")));
        if (player.hasPermission("vnmine.command.status")) inventory.setItem(12,
            menuItem(Material.COMPASS, "§bTrạng thái server", List.of("§7Xem tình trạng VnMineHardcore.")));
        if (player.hasPermission(COMMAND_PERMISSION) && enabled) {
            inventory.setItem(14, menuItem(Material.IRON_SWORD, "§6Quản lý quân đội", List.of("§7Xem và điều khiển lính.")));
        }
        if (player.hasPermission("vnmine.command.reward")) inventory.setItem(16,
            menuItem(Material.CHEST, "§6Phần thưởng", List.of("§7Nhận phần thưởng boss đang chờ.")));
        inventory.setItem(22, menuItem(Material.WRITABLE_BOOK, "§bTrợ giúp", List.of("§7Mở hướng dẫn lệnh máy chủ.")));
        if (hasAdminMenuAccess(player)
                && plugin.getConfig().getBoolean("menus.admin.enabled", true)) {
            inventory.setItem(24, menuItem(Material.COMMAND_BLOCK, "§cAdmin", List.of("§7Mở công cụ quản trị.")));
        }
        player.openInventory(inventory);
    }

    private void openAdminMenu(Player player) {
        if (!hasAdminMenuAccess(player)) return;
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN, null, null, null, 0,
            54, "VnMine • Admin");
        if (player.hasPermission(ADMIN_PERMISSION)) holder.inventory.setItem(11,
            menuItem(Material.IRON_SWORD, "§6Quân đội", List.of("§7Chọn người chơi để cấp lính hoặc token.")));
        holder.inventory.setItem(12, menuItem(Material.CLOCK, "§eGọi thiên tai", List.of("§7Chọn một thiên tai để kích hoạt.")));
        holder.inventory.setItem(13, menuItem(Material.BARRIER, "§cDừng thiên tai", List.of("§7Dừng cảnh báo hoặc sự kiện đang chạy.")));
        holder.inventory.setItem(14, menuItem(Material.DRAGON_HEAD, "§cGọi boss", List.of("§7Chọn boss và level để kích hoạt.")));
        holder.inventory.setItem(15, menuItem(Material.DIAMOND_SWORD, "§4Kết thúc boss", List.of("§7Kết thúc boss hiện tại.")));
        holder.inventory.setItem(16, menuItem(Material.REDSTONE, "§eTải lại cấu hình", List.of("§7Tải lại cấu hình plugin.")));
        holder.inventory.setItem(17, menuItem(Material.COMPASS, "§bVề spawn", List.of("§7Dịch chuyển về spawn thế giới.")));
        holder.inventory.setItem(18, menuItem(Material.LODESTONE, "§bĐặt spawn world", List.of("§7Đặt spawn tại vị trí hiện tại.")));
        holder.inventory.setItem(19, menuItem(Material.CLOCK, "§eThời lượng ngày/đêm", List.of("§7Chọn chu kỳ và số phút.")));
        holder.inventory.setItem(20, menuItem(Material.SUNFLOWER, "§eĐặt ngày", List.of("§7Chọn ngày cho world hiện tại.")));
        holder.inventory.setItem(21, menuItem(Material.SHIELD, "§6Spawn protection", List.of("§7Tạo, xem hoặc xóa vùng bảo vệ.")));
        holder.inventory.setItem(22, menuItem(Material.ENDER_PEARL, "§bDịch chuyển người chơi", List.of("§7Chọn người chơi và world spawn.")));
        holder.inventory.setItem(23, menuItem(Material.BOOKSHELF, "§dQuản lý nhóm/quyền", List.of("§7Xem, tạo, xóa nhóm và chỉnh permission.")));
        holder.inventory.setItem(24, menuItem(Material.PLAYER_HEAD, "§dQuản lý người chơi", List.of("§7Xem thông tin hoặc đặt lại quyền.")));
        holder.inventory.setItem(25, menuItem(Material.FEATHER, "§aBật/tắt bay", List.of("§7Chuyển trạng thái bay của bạn.")));
        holder.inventory.setItem(26, menuItem(Material.BARRIER, "§cMiễn sát thương", List.of("§7Bật/tắt chế độ thử boss.")));
        holder.inventory.setItem(27, menuItem(Material.IRON_PICKAXE, "§eSurvival", List.of("§7Đổi game mode.")));
        holder.inventory.setItem(28, menuItem(Material.GRASS_BLOCK, "§aCreative", List.of("§7Đổi game mode.")));
        holder.inventory.setItem(29, menuItem(Material.MAP, "§6Adventure", List.of("§7Đổi game mode.")));
        holder.inventory.setItem(30, menuItem(Material.ENDER_EYE, "§7Spectator", List.of("§7Đổi game mode.")));
        holder.inventory.setItem(31, menuItem(Material.REDSTONE_BLOCK, "§cReset scoreboard",
            List.of("§7Xóa điểm mùa và phần thưởng chờ.")));
        holder.inventory.setItem(32, menuItem(Material.SKELETON_SKULL, "§cReset số lần chết",
            List.of("§7Chọn người chơi.")));
        holder.inventory.setItem(33, menuItem(Material.MAP, "§bĐi tới spawn world",
            List.of("§7Chọn world đích.")));
        if (player.hasPermission("vnmine.command.death.history")) {
            holder.inventory.setItem(34, menuItem(Material.CLOCK, "§cLịch sử chết",
                List.of("§7Chọn người chơi online và xem inventory lúc chết.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Menu chính", List.of()));
        player.openInventory(holder.inventory);
    }

    private boolean hasAdminMenuAccess(Player player) {
        return List.of(ADMIN_PERMISSION, "vnmine.command.disasters", "vnmine.command.bosses",
            "vnmine.command.reload", "vnmine.command.time.manage", "vnmine.command.day.manage",
            "vnmine.command.spawn.manage", "vnmine.command.teleport.other", "vnmine.command.groups.manage",
            "vnmine.command.users.manage", "vnmine.command.fly", "vnmine.command.nodame",
            "vnmine.command.gamemode", "vnmine.command.scoreboard.reset", "vnmine.command.death.reset")
            .stream().anyMatch(player::hasPermission)
            || player.hasPermission("vnmine.command.death.history");
    }

    private void openAdminArmyPlayers(Player player, int page) {
        List<? extends Player> online = Bukkit.getOnlinePlayers().stream()
            .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        int pages = Math.max(1, (online.size() + 44) / 45);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_PLAYERS, null, null, null,
            safePage, 54, "Cấp lính • Người chơi " + (safePage + 1) + "/" + pages);
        for (int index = safePage * 45; index < Math.min((safePage + 1) * 45, online.size()); index++) {
            Player target = online.get(index);
            int slot = index - safePage * 45;
            holder.choices.put(slot, target.getUniqueId().toString());
            holder.inventory.setItem(slot, menuItem(Material.PLAYER_HEAD, "§e" + target.getName(),
                List.of("§7Chọn người chơi này.")));
        }
        if (safePage > 0) holder.inventory.setItem(45, menuItem(Material.ARROW, "§eTrang trước", List.of()));
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại", List.of()));
        if (safePage + 1 < pages) holder.inventory.setItem(53, menuItem(Material.ARROW, "§eTrang sau", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openTimeMenu(Player player) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_TIME, null, null, null,
            0, 27, "Admin • Thời lượng ngày/đêm");
        holder.inventory.setItem(11, menuItem(Material.SUNFLOWER, "§eBan ngày", List.of("§7Chọn thời lượng.")));
        holder.inventory.setItem(15, menuItem(Material.BLUE_ICE, "§9Ban đêm", List.of("§7Chọn thời lượng.")));
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openTimeDurationMenu(Player player, String period) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.TIME_DURATION, null, period, null,
            0, 27, "Admin • " + (period.equals("day") ? "Ban ngày" : "Ban đêm"));
        int[] minutes = {10, 20, 30, 60};
        for (int index = 0; index < minutes.length; index++) {
            int slot = 10 + index;
            holder.choices.put(slot, Integer.toString(minutes[index]));
            holder.inventory.setItem(slot, menuItem(Material.CLOCK, "§e" + minutes[index] + " phút",
                List.of("§7Áp dụng toàn cục.")));
        }
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openDayMenu(Player player) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_DAY, null, null, null,
            0, 27, "Admin • Chọn ngày game");
        long[] days = {0, 1, 7, 30, 100, 400};
        for (int index = 0; index < days.length; index++) {
            int slot = 10 + index;
            holder.choices.put(slot, Long.toString(days[index]));
            holder.inventory.setItem(slot, menuItem(Material.SUNFLOWER, "§eNgày " + days[index],
                List.of("§7Áp dụng cho world hiện tại.")));
        }
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openProtectionMenu(Player player) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_PROTECT, null, null, null,
            0, 27, "Admin • Spawn protection");
        int[] radii = {10, 25, 50, 100};
        for (int index = 0; index < radii.length; index++) {
            int slot = 10 + index;
            holder.choices.put(slot, "protect:" + radii[index]);
            holder.inventory.setItem(slot, menuItem(Material.SHIELD, "§eBảo vệ " + radii[index] + " block",
                List.of("§7Tâm tại vị trí hiện tại.")));
        }
        holder.inventory.setItem(15, menuItem(Material.BOOK, "§bDanh sách khu vực", List.of("§7Xem các vùng đang bảo vệ.")));
        holder.inventory.setItem(16, menuItem(Material.BARRIER, "§cXóa khu vực", List.of("§7Chọn ID cần xóa.")));
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openProtectionRemoveMenu(Player player) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.PROTECTION_REMOVE, null, null, null,
            0, 54, "Admin • Xóa spawn protection");
        List<Integer> ids = plugin.getSpawnProtectionManager().getProtectionIds();
        for (int index = 0; index < ids.size() && index < 28; index++) {
            int id = ids.get(index);
            int slot = 10 + index / 7 * 9 + index % 7;
            holder.choices.put(slot, Integer.toString(id));
            holder.inventory.setItem(slot, menuItem(Material.BARRIER, "§cKhu vực #" + id, List.of("§eNhấn để xóa.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openTeleportPlayers(Player player, int page) {
        List<? extends Player> online = Bukkit.getOnlinePlayers().stream()
            .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        int pages = Math.max(1, (online.size() + 44) / 45);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.TELEPORT_PLAYERS, null, null, null,
            safePage, 54, "Admin • Dịch chuyển người chơi");
        for (int index = safePage * 45; index < Math.min((safePage + 1) * 45, online.size()); index++) {
            Player target = online.get(index);
            int slot = index - safePage * 45;
            holder.choices.put(slot, target.getUniqueId().toString());
            holder.inventory.setItem(slot, menuItem(Material.PLAYER_HEAD, "§e" + target.getName(), List.of("§7Chọn người chơi.")));
        }
        if (safePage > 0) holder.inventory.setItem(45, menuItem(Material.ARROW, "§eTrang trước", List.of()));
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        if (safePage + 1 < pages) holder.inventory.setItem(53, menuItem(Material.ARROW, "§eTrang sau", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openTeleportWorlds(Player player, UUID target) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.TELEPORT_WORLDS, target, null, null,
            0, 54, "Admin • Chọn world spawn");
        List<World> worlds = Bukkit.getWorlds();
        for (int index = 0; index < worlds.size() && index < 28; index++) {
            World world = worlds.get(index);
            int slot = 10 + index / 7 * 9 + index % 7;
            holder.choices.put(slot, world.getName());
            holder.inventory.setItem(slot, menuItem(Material.GRASS_BLOCK, "§e" + world.getName(),
                List.of("§7Dịch chuyển người chơi đến spawn.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại người chơi", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminUserPlayers(Player player, int page) {
        List<? extends Player> online = Bukkit.getOnlinePlayers().stream()
            .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        int pages = Math.max(1, (online.size() + 44) / 45);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_USER_PLAYERS, null, null, null,
            safePage, 54, "Admin • Quyền người chơi");
        for (int index = safePage * 45; index < Math.min((safePage + 1) * 45, online.size()); index++) {
            Player target = online.get(index);
            int slot = index - safePage * 45;
            holder.choices.put(slot, target.getUniqueId().toString());
            holder.inventory.setItem(slot, menuItem(Material.PLAYER_HEAD, "§e" + target.getName(), List.of("§7Chọn người chơi.")));
        }
        if (safePage > 0) holder.inventory.setItem(45, menuItem(Material.ARROW, "§eTrang trước", List.of()));
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        if (safePage + 1 < pages) holder.inventory.setItem(53, menuItem(Material.ARROW, "§eTrang sau", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminUserActions(Player player, UUID target) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_USER_ACTION, target, null, null,
            0, 27, "Admin • Người chơi");
        holder.inventory.setItem(11, menuItem(Material.BOOK, "§eXem quyền", List.of("§7Hiển thị nhóm và quyền riêng.")));
        holder.inventory.setItem(13, menuItem(Material.BARRIER, "§cĐặt lại quyền", List.of("§7Đưa về nhóm mặc định.")));
        holder.inventory.setItem(15, menuItem(Material.NAME_TAG, "§bGán nhóm", List.of("§7Chọn nhóm cho người chơi.")));
        holder.inventory.setItem(16, menuItem(Material.PAPER, "§dQuyền cá nhân", List.of("§7Cấp hoặc thu hồi permission.")));
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminGroupMenu(Player player) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_GROUPS, null, null, null,
            0, 27, "Admin • Nhóm quyền");
        holder.inventory.setItem(10, menuItem(Material.BOOK, "§eDanh sách nhóm", List.of()));
        holder.inventory.setItem(11, menuItem(Material.WRITABLE_BOOK, "§aTạo nhóm", List.of("§7Nhập tên nhóm trong chat.")));
        holder.inventory.setItem(12, menuItem(Material.KNOWLEDGE_BOOK, "§bThông tin nhóm", List.of("§7Chọn nhóm cần xem.")));
        holder.inventory.setItem(13, menuItem(Material.BARRIER, "§cXóa nhóm", List.of("§7Chọn nhóm cần xóa.")));
        holder.inventory.setItem(14, menuItem(Material.LIME_DYE, "§aCấp quyền nhóm", List.of("§7Chọn nhóm và permission.")));
        holder.inventory.setItem(15, menuItem(Material.RED_DYE, "§cThu hồi quyền nhóm", List.of("§7Chọn nhóm và permission.")));
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminGroupSelect(Player player, String action) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_GROUP_SELECT, null, action, null,
            0, 54, "Admin • Chọn nhóm");
        List<String> groups = plugin.getPermissionManager().getGroupNames();
        for (int index = 0; index < groups.size() && index < 28; index++) {
            String group = groups.get(index);
            int slot = 10 + index / 7 * 9 + index % 7;
            holder.choices.put(slot, group);
            holder.inventory.setItem(slot, menuItem(Material.NAME_TAG, "§e" + group, List.of("§7Chọn nhóm.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminPermissionSelect(Player player, UUID target, String action, String group) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_PERMISSION_SELECT, target, action,
            group, 0, 54, "Admin • Chọn permission");
        List<String> permissions = plugin.getDescription().getPermissions().stream()
            .map(org.bukkit.permissions.Permission::getName).sorted().toList();
        for (int index = 0; index < permissions.size() && index < 45; index++) {
            String permission = permissions.get(index);
            holder.choices.put(index, permission);
            holder.inventory.setItem(index, menuItem(Material.PAPER, "§e" + permission,
                List.of("§7Nhấn để áp dụng.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminUserPermissionActions(Player player, UUID target) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_USER_PERMISSION_ACTIONS,
            target, null, null, 0, 27, "Admin • Quyền cá nhân");
        holder.inventory.setItem(11, menuItem(Material.LIME_DYE, "§aCấp permission", List.of()));
        holder.inventory.setItem(15, menuItem(Material.RED_DYE, "§cThu hồi permission", List.of()));
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminUserGroupSelect(Player player, UUID target) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_USER_GROUP_SELECT, target,
            null, null, 0, 54, "Admin • Gán nhóm");
        List<String> groups = plugin.getPermissionManager().getGroupNames();
        for (int index = 0; index < groups.size() && index < 28; index++) {
            String group = groups.get(index);
            int slot = 10 + index / 7 * 9 + index % 7;
            holder.choices.put(slot, group);
            holder.inventory.setItem(slot, menuItem(Material.NAME_TAG, "§e" + group, List.of("§7Gán nhóm.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminDeathPlayers(Player player) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_DEATH_PLAYERS,
            null, null, null, 0, 54, "Admin • Reset số lần chết");
        List<? extends Player> online = Bukkit.getOnlinePlayers().stream()
            .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        for (int index = 0; index < online.size() && index < 45; index++) {
            Player target = online.get(index);
            holder.choices.put(index, target.getUniqueId().toString());
            holder.inventory.setItem(index, menuItem(Material.PLAYER_HEAD, "§e" + target.getName(),
                List.of("§7Reset số lần chết.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminSpawnWorlds(Player player) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_SPAWN_WORLD,
            null, null, null, 0, 54, "Admin • Spawn world");
        List<World> worlds = Bukkit.getWorlds();
        for (int index = 0; index < worlds.size() && index < 28; index++) {
            World world = worlds.get(index);
            int slot = 10 + index / 7 * 9 + index % 7;
            holder.choices.put(slot, world.getName());
            holder.inventory.setItem(slot, menuItem(Material.GRASS_BLOCK, "§e" + world.getName(),
                List.of("§7Dịch chuyển bạn đến spawn.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openAdminScoreboardConfirm(Player player) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.ADMIN_SCOREBOARD_CONFIRM,
            null, null, null, 0, 27, "Admin • Reset scoreboard");
        holder.inventory.setItem(11, menuItem(Material.REDSTONE_BLOCK, "§cXác nhận reset",
            List.of("§7Xóa toàn bộ điểm mùa và thưởng chờ.")));
        holder.inventory.setItem(15, menuItem(Material.ARROW, "§7Hủy", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openDisasterMenu(Player player) {
        List<String> ids = plugin.getDisasterManager().getDisasterIds().stream().sorted().toList();
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.DISASTER_SELECT, null, null, null,
            0, 54, "Admin • Chọn thiên tai");
        for (int index = 0; index < ids.size() && index < 28; index++) {
            String id = ids.get(index);
            int slot = 10 + index / 7 * 9 + index % 7;
            holder.choices.put(slot, id);
            holder.inventory.setItem(slot, menuItem(Material.CLOCK,
                "§e" + plugin.getDisasterManager().getDisasterName(id), List.of("§7ID: §f" + id,
                    "§eNhấn để kích hoạt.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openBossMenu(Player player) {
        List<String> ids = plugin.getBossEventManager().getEnabledBossIds().stream().sorted().toList();
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.BOSS_SELECT, null, null, null,
            0, 54, "Admin • Chọn boss");
        for (int index = 0; index < ids.size() && index < 28; index++) {
            String id = ids.get(index);
            int slot = 10 + index / 7 * 9 + index % 7;
            holder.choices.put(slot, id);
            holder.inventory.setItem(slot, menuItem(bossMenuIcon(id),
                "§c" + plugin.getBossEventManager().getBossName(id), List.of("§7ID: §f" + id,
                    "§eNhấn để chọn level boss.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại admin", List.of()));
        player.openInventory(holder.inventory);
    }

    private Material bossMenuIcon(String bossId) {
        String configuredType = plugin.getBossEventManager().getBossEntityType(bossId);
        if (configuredType == null || configuredType.isBlank()) return Material.BARRIER;
        String entityType = configuredType.toUpperCase(Locale.ROOT);
        Material head = switch (entityType) {
            case "CREEPER" -> Material.CREEPER_HEAD;
            case "ENDER_DRAGON" -> Material.DRAGON_HEAD;
            case "PIGLIN", "PIGLIN_BRUTE" -> Material.PIGLIN_HEAD;
            case "PLAYER" -> Material.PLAYER_HEAD;
            case "SKELETON" -> Material.SKELETON_SKULL;
            case "WITHER_SKELETON" -> Material.WITHER_SKELETON_SKULL;
            case "ZOMBIE" -> Material.ZOMBIE_HEAD;
            default -> null;
        };
        if (head != null) return head;
        Material spawnEgg = Material.matchMaterial(entityType + "_SPAWN_EGG");
        return spawnEgg == null ? Material.BARRIER : spawnEgg;
    }

    private void openBossLevelMenu(Player player, String bossId) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.BOSS_LEVEL_SELECT, null, null,
            bossId, 0, 27, "Boss • Chọn level");
        for (int level = 1; level <= 10; level++) {
            int slot = 9 + level;
            holder.choices.put(slot, Integer.toString(level));
            holder.inventory.setItem(slot, menuItem(Material.NETHER_STAR, "§cLevel " + level,
                List.of("§7Sự kiện boss: §f" + plugin.getBossEventManager().getBossName(bossId),
                    "§eNhấn để gọi boss.")));
        }
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại", List.of("§7Chọn boss")));
        player.openInventory(holder.inventory);
    }

    private void openGiveTypeMenu(Player player, UUID target) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.GIVE_TYPE, target, null, null,
            0, 27, "Cấp cho " + Bukkit.getOfflinePlayer(target).getName());
        holder.inventory.setItem(11, menuItem(Material.IRON_SWORD, "§6Lính đánh thuê",
            List.of("§7Tạo đơn vị trực tiếp; không tiêu thụ vật phẩm.")));
        holder.inventory.setItem(15, menuItem(Material.PAPER, "§eToken triệu hồi",
            List.of("§7Token gắn lớp và tier đã chọn.")));
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại", List.of()));
        player.openInventory(holder.inventory);
    }

    private void openClassMenu(Player player, UUID target, String itemType) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.CLASS_SELECT, target, itemType,
            null, 0, 54, itemType.equals("hire") ? "Thuê lính • Chọn lớp" : "Cấp lính • Chọn lớp");
        List<String> classes = getClassIds();
        for (int index = 0; index < classes.size() && index < 28; index++) {
            String classId = classes.get(index);
            int slot = 10 + index / 7 * 9 + index % 7;
            Material icon = Material.matchMaterial(settings.getString("classes." + classId + ".menu-icon", "IRON_SWORD"));
            if (icon == null || icon.isAir()) icon = Material.IRON_SWORD;
            holder.choices.put(slot, classId);
            holder.inventory.setItem(slot, menuItem(icon, "§e" + classDisplay(classId), classSummary(classId)));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại",
            List.of(itemType.equals("hire") ? "§7Menu chính" : "§7Chọn loại item")));
        player.openInventory(holder.inventory);
    }

    private List<String> classSummary(String classId) {
        List<String> lore = new ArrayList<>();
        Unit preview = new Unit(new UUID(0L, 0L), new UUID(0L, 1L), classId, 1, Mode.PROTECT, false);
        lore.add("§7" + settings.getString("classes." + classId + ".description", "Lính " + classDisplay(classId)));
        lore.add("§7Skill:");
        for (String skill : settings.getStringList("classes." + classId + ".skills")) {
            lore.add("§f• " + skillDisplayName(skill) + ": " + skillDescription(preview, skill, 0.0));
            lore.add("  §7Hồi chiêu: §f" + formatStat(skillCooldownSeconds(preview, skill)) + " giây");
        }
        lore.add("§eNhấn để chọn Bậc.");
        return lore;
    }

    private void openTierMenu(Player player, UUID target, String itemType, String classId) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.TIER_SELECT, target, itemType,
            classId, 0, 54, classDisplay(classId) + " • Chọn Bậc");
        List<Integer> tiers = getTierIds().stream().map(MercenaryManager::parsePositiveInt).toList();
        for (int index = 0; index < tiers.size() && index < 28; index++) {
            int tier = tiers.get(index);
            int slot = 10 + index / 7 * 9 + index % 7;
            holder.choices.put(slot, Integer.toString(tier));
            holder.inventory.setItem(slot, menuItem(Material.AMETHYST_SHARD,
                "§eBậc " + tier, tierSummary(classId, tier, itemType)));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại", List.of("§7Chọn lớp")));
        player.openInventory(holder.inventory);
    }

    private List<String> tierSummary(String classId, int tier, String itemType) {
        List<String> lore = new ArrayList<>();
        double health = settings.getDouble("classes." + classId + ".base-health", 24.0)
            + Math.max(0, tier - 1) * settings.getDouble("classes." + classId + ".health-per-level", 4.0);
        double damage = settings.getDouble("classes." + classId + ".base-damage", 4.0)
            + Math.max(0, tier - 1) * settings.getDouble("classes." + classId + ".damage-per-level", 0.5);
        double armor = settings.getDouble("classes." + classId + ".base-armor", 0.0)
            + Math.max(0, tier - 1) * settings.getDouble("classes." + classId + ".armor-per-level", 0.0);
        lore.add("§7Bậc triệu hồi: §f" + tier + " §8| §7Level ban đầu: §f1");
        lore.add("§cHP: §f" + formatStat(health) + " §7| §bGiáp: §f" + formatStat(armor));
        lore.add("§6Sát thương: §f" + formatStat(damage));
        if (itemType.equals("hire")) {
            lore.add("§7Token: §f" + classDisplay(classId) + " Bậc " + tier);
            lore.add("§7XP người chơi: §f" + settings.getInt("tiers." + tier + ".experience-levels", 0) + " level");
            ConfigurationSection costs = settings.getConfigurationSection("tiers." + tier + ".materials");
            if (costs != null) for (String material : costs.getKeys(false)) {
                int amount = costs.getInt(material);
                if (amount > 0) lore.add("§7" + material + ": §f" + amount);
            }
        } else {
            lore.add("§7Loại item: §f" + itemType.toUpperCase(Locale.ROOT));
        }
        lore.add("§7Skill: §f" + String.join(", ", settings.getStringList("classes." + classId + ".skills")));
        lore.add("§eNhấn để " + (itemType.equals("hire") ? "thuê" : "cấp") + ".");
        return lore;
    }

    private void openArmyMenu(Player player, int page) {
        List<Unit> owned = getOwnerUnits(player.getUniqueId());
        int pages = Math.max(1, (owned.size() + 27) / 28);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        int start = safePage * 28;
        List<UUID> pageIds = owned.subList(Math.min(start, owned.size()),
            Math.min(start + 28, owned.size())).stream().map(unit -> unit.id).toList();
        ArmyMenuHolder holder = new ArmyMenuHolder(player.getUniqueId(), safePage, pageIds);
        Inventory inventory = Bukkit.createInventory(holder, 54, "Đội quân • " + (safePage + 1) + "/" + pages);
        holder.inventory = inventory;
        inventory.setItem(2, menuItem(Material.WOODEN_SWORD, "§aTriệu hồi đội hình",
            List.of("§7Gọi tối đa " + maxDeployedUnits + " lính.",
                "§7Ưu tiên bậc cao nhất, sau đó level cao nhất.")));
        inventory.setItem(4, menuItem(Material.EMERALD, "§aThuê lính", List.of("§7Chọn lớp và Bậc.")));
        inventory.setItem(6, menuItem(Material.RED_BED, "§7Nghỉ tất cả", List.of("§7Cho tất cả lính nghỉ ngơi.")));
        for (int index = 0; index < pageIds.size(); index++) {
            Unit unit = units.get(pageIds.get(index));
            if (unit == null) continue;
            Material icon = Material.matchMaterial(settings.getString(
                "classes." + unit.unitClass + ".menu-icon", "IRON_SWORD"));
            if (icon == null || icon.isAir()) icon = Material.IRON_SWORD;
            ItemStack entry = new ItemStack(icon);
            ItemMeta meta = entry.getItemMeta();
            meta.setDisplayName(unitLabel(unit, true) + " §7• #" + shortId(unit.id));
            meta.setLore(List.of("§7Bậc: §f" + unit.tier + " §8| §7Cấp: §f" + unit.level,
                "§7XP: §f" + unit.experience + "/" + nextLevelExperience(unit),
                "§7Chế độ: §f" + modeName(unit.mode),
                unitStatus(unit),
                "§eNhấn để quản lý"));
            entry.setItemMeta(meta);
            inventory.setItem(10 + (index / 7) * 9 + index % 7, entry);
        }
        inventory.setItem(49, menuItem(Material.ARROW, "§7Quay lại menu chính",
            List.of("§7Trở về menu chính của plugin.")));
        if (safePage > 0) inventory.setItem(45, menuItem(Material.ARROW, "§eTrang trước", List.of()));
        if (safePage + 1 < pages) inventory.setItem(53, menuItem(Material.ARROW, "§eTrang sau", List.of()));
        if (owned.isEmpty()) inventory.setItem(22, menuItem(Material.BARRIER, "§7Chưa có lính",
            List.of("§7Thuê bằng /vnarmy hire <loại> <bậc>.")));
        player.openInventory(inventory);
    }

    private void openUnitMenu(Player player, Unit unit) {
        UnitMenuHolder holder = new UnitMenuHolder(player.getUniqueId(), unit.id);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Quản lý lính #" + shortId(unit.id));
        holder.inventory = inventory;
        inventory.setItem(4, menuItem(Material.NAME_TAG, unitLabel(unit, true), unitDetails(unit)));
        inventory.setItem(10, menuItem(Material.IRON_SWORD, "§cChế độ tấn công", List.of()));
        inventory.setItem(11, menuItem(Material.SHIELD, "§6Chế độ phòng thủ", List.of()));
        inventory.setItem(12, menuItem(Material.LEAD, "§aBảo vệ chủ nhân", List.of()));
        inventory.setItem(13, menuItem(unit.muted ? Material.MUSIC_DISC_13 : Material.NOTE_BLOCK,
            unit.muted ? "§7Bật âm thanh" : "§aTắt âm thanh", List.of()));
        inventory.setItem(15, menuItem(Material.BARRIER, "§cXóa lính", List.of("§7Xóa đơn vị này khỏi đội quân.")));
        inventory.setItem(14, menuItem(unit.deployed ? Material.RED_BED : Material.LIME_BED,
            unit.deployed ? "§cCho lính nghỉ" : "§aTriệu hồi lính", List.of()));
        inventory.setItem(16, menuItem(Material.CHEST, "§bTrang bị", List.of()));
        inventory.setItem(17, menuItem(canUpgrade(unit) ? Material.EMERALD : Material.GRAY_DYE,
            "§aNâng bậc", tierUpgradeLore(unit)));
        inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại đội quân", List.of()));
        player.openInventory(inventory);
    }

    private void openUnitDeleteConfirm(Player player, Unit unit) {
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.UNIT_DELETE_CONFIRM,
            unit.owner, unit.id.toString(), null, 0, 27, "Xác nhận xóa lính");
        holder.inventory.setItem(4, menuItem(Material.BARRIER, "§cXóa " + unitLabel(unit, true) + "?",
            List.of("§7ID: §f#" + shortId(unit.id), "§cThao tác này không thể hoàn tác.")));
        holder.inventory.setItem(11, menuItem(Material.LIME_WOOL, "§aXÁC NHẬN XÓA",
            List.of("§7Xóa lính khỏi đội quân.")));
        holder.inventory.setItem(15, menuItem(Material.RED_WOOL, "§cHỦY",
            List.of("§7Giữ lại lính.")));
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại lính", List.of()));
        player.openInventory(holder.inventory);
    }

    private List<String> unitDetails(Unit unit) {
        List<String> details = new ArrayList<>();
        Mob mob = activeEntities.get(unit.id);
        double maxHealth = mob != null && mob.isValid() ? maxHealth(mob)
            : settings.getDouble("classes." + unit.unitClass + ".base-health", 24.0)
                + Math.max(0, statRank(unit) - 1)
                    * settings.getDouble("classes." + unit.unitClass + ".health-per-level", 4.0);
        double health = mob != null && mob.isValid() ? mob.getHealth() : maxHealth;
        double damage = mob != null && mob.getAttribute(Attribute.ATTACK_DAMAGE) != null
            ? mob.getAttribute(Attribute.ATTACK_DAMAGE).getValue()
            : settings.getDouble("classes." + unit.unitClass + ".base-damage", 4.0)
                + Math.max(0, statRank(unit) - 1)
                    * settings.getDouble("classes." + unit.unitClass + ".damage-per-level", 0.5);
        double armor = mob != null && mob.getAttribute(Attribute.ARMOR) != null
            ? mob.getAttribute(Attribute.ARMOR).getValue()
            : settings.getDouble("classes." + unit.unitClass + ".base-armor", 0.0)
                + Math.max(0, statRank(unit) - 1)
                    * settings.getDouble("classes." + unit.unitClass + ".armor-per-level", 0.0);
        String configuredWeapon = settings.getString("classes." + unit.unitClass + ".starting-weapon", "");
        Material recommendedWeapon = Material.matchMaterial(configuredWeapon);
        if (recommendedWeapon == null && "IRON_SPEAR".equalsIgnoreCase(configuredWeapon)) {
            recommendedWeapon = Material.TRIDENT;
        }
        String weaponName = recommendedWeapon == null || recommendedWeapon.isAir()
            ? "Chưa cấu hình" : recommendedWeapon.name().replace('_', ' ');
        details.add("§7Bậc: §f" + unit.tier + " (gốc " + unit.originalTier + ") §8| §7Cấp: §f" + unit.level
            + " §8| §7XP: §f" + unit.experience + "/" + nextLevelExperience(unit));
        details.add("§7Trạng thái: " + unitStatus(unit));
        details.add("§7Vũ khí phù hợp nhất: §f" + weaponName);
        details.add("§cHP: §f" + formatStat(health) + "/" + formatStat(maxHealth)
            + " §7| Giáp: §f" + formatStat(armor));
        details.add("§6Sát thương: §f" + formatStat(damage));
        details.add("§8Skill:");
        for (String skill : settings.getStringList("classes." + unit.unitClass + ".skills")) {
            String normalized = skill.toUpperCase(Locale.ROOT);
            String cooldownKey = skillCooldownKey(normalized);
            long remaining = Math.max(0L, skillCooldowns.getOrDefault(unit.id, Map.of())
                .getOrDefault(cooldownKey, 0L) - System.currentTimeMillis());
            double cooldownSeconds = normalized.equals("ILLUMINATION")
                ? illuminationCooldownSeconds(unit) : skillCooldownSeconds(unit, normalized);
            String cooldown = formatStat(cooldownSeconds) + " giây";
            if (remaining > 0L) cooldown += " (còn " + formatStat(remaining / 1000.0) + " giây)";
            details.add("§7" + skillDisplayName(normalized) + ": §f"
                + skillDescription(unit, normalized, damage) + " §8| §7Hồi chiêu: §f" + cooldown);
        }
        return details;
    }

    private String skillDescription(Unit unit, String skill, double meleeDamage) {
        double power = skillPowerMultiplier(unit);
        return switch (skill.toUpperCase(Locale.ROOT)) {
            case "ATTACK" -> "Gây " + formatStat(meleeDamage) + " sát thương cận chiến mỗi đòn";
            case "RANGED_ATTACK" -> "Bắn gây " + formatStat(skillOptionDouble(unit, skill, "damage", 4.0,
                0.0, 100.0) * power) + " sát thương, tầm " + formatStat(skillOptionDouble(unit,
                skill, "range-blocks", 16.0, 4.0, 64.0)) + " block";
            case "HEAL_OWNER" -> "Hồi " + formatStat(skillOptionDouble(unit, skill, "amount",
                healAmount, 0.0, 100.0) * power) + " HP cho chủ nhân";
            case "HEAL_ALLIES" -> "Hồi " + formatStat(skillOptionDouble(unit, skill, "amount",
                healAmount, 0.0, 100.0) * power) + " HP cho "
                + skillTargetDescription(unit, skill) + ", bán kính " + formatStat(skillOptionDouble(
                unit, skill, "radius-blocks", healRadius, 1.0, 32.0)) + " block";
            case "SHIELD_OWNER" -> "Giảm " + formatStat(Math.min(0.9, skillOptionDouble(unit, skill,
                "damage-reduction", shieldReduction, 0.0, 0.9) * power) * 100.0)
                + "% sát thương lên chủ nhân, bán kính " + formatStat(skillOptionDouble(unit,
                skill, "radius-blocks", 8.0, 1.0, 32.0)) + " block";
            case "BUFF_OWNER" -> "Tăng " + localizedEffectName(skillOptionString(unit, skill,
                "effect", ownerBuff)) + " cho chủ nhân trong " + scaledDuration(unit, skill, ownerBuffTicks / 20.0)
                + " giây";
            case "BUFF_ALLIES" -> "Tăng " + localizedEffectName(skillOptionString(unit, skill,
                "effect", ownerBuff)) + " cho " + skillTargetDescription(unit, skill) + " trong "
                + scaledDuration(unit, skill, ownerBuffTicks / 20.0) + " giây, bán kính "
                + formatStat(skillOptionDouble(unit, skill, "radius-blocks", healRadius, 1.0, 32.0)) + " block";
            case "VOLLEY" -> "Bắn " + (int) skillOptionDouble(unit, skill, "arrow-count", 3.0, 2.0, 5.0)
                + " mũi tên, mỗi mũi gây " + formatStat(skillOptionDouble(unit, skill, "damage", 3.0,
                0.0, 100.0) * power) + " sát thương"
                + ("ranger".equals(unit.unitClass) ? ", tầm " + formatStat(skillOptionDouble(unit, skill,
                "range-blocks", 20.8, 4.0, 64.0)) + " block" : "");
            case "POWER_STRIKE" -> "Cộng " + formatStat(skillOptionDouble(unit, skill, "damage", 8.0,
                0.0, 100.0) * power) + " sát thương cho đòn đánh mạnh";
            case "TAUNT" -> "Khiêu khích quái trong bán kính "
                + formatStat(skillOptionDouble(unit, skill, "radius-blocks", 8.0, 1.0, 32.0)) + " block";
            case "CLEANSE_OWNER" -> "Gỡ hiệu ứng " + settings.getStringList("classes." + unit.unitClass
                + ".skill-settings.CLEANSE_OWNER.effects").stream().map(MercenaryManager::localizedEffectName)
                .reduce((left, right) -> left + ", " + right).orElse("bất lợi") + " khỏi chủ nhân";
            case "FIRE_BOLT" -> "Gây " + formatStat(skillOptionDouble(unit, skill, "damage", 6.0,
                0.0, 100.0) * power) + " sát thương và đốt mục tiêu trong "
                + (int) skillOptionDouble(unit, skill, "fire-seconds", 3.0, 0.0, 15.0) + " giây";
            case "ROOT_TARGET" -> "Gây " + localizedEffectName(skillOptionString(unit, skill,
                "effect", "SLOWNESS")) + " lên mục tiêu trong " + scaledDuration(unit, skill, 5.0) + " giây";
            case "LIFESTEAL" -> "Gây " + formatStat(skillOptionDouble(unit, skill, "damage", 8.0,
                0.0, 100.0) * power) + " sát thương và hồi " + formatStat(skillOptionDouble(unit, skill,
                "heal", 4.0, 0.0, 100.0) * power) + " HP";
            case "BERSERK" -> "Kích hoạt dưới " + formatStat(skillOptionDouble(unit, skill,
                "health-percent", 50.0, 1.0, 99.0)) + "% HP, tăng sức mạnh trong "
                + scaledDuration(unit, skill, 8.0) + " giây";
            case "SUMMON_VEXES" -> "Triệu hồi tối đa " + maxSummonedMinions(unit)
                + " Vex, mỗi lần gọi thêm một con, mỗi con gây " + formatStat(skillOptionDouble(
                unit, skill, "damage", 4.0, 0.0, 100.0) * power) + " sát thương";
            case "LANCE_CHARGE" -> "Khi cưỡi ngựa gây " + formatStat(skillOptionDouble(unit, skill,
                "damage", 12.0, 0.0, 100.0) * power) + " sát thương trong bán kính "
                + formatStat(skillOptionDouble(unit, skill, "radius-blocks", 4.0, 1.0, 12.0)) + " block";
            case "ILLUMINATION" -> "Bắn " + Math.min(20, (int) skillOptionDouble(unit, skill,
                "base-arrow-count", 1.0, 1.0, 20.0) + unit.level / (int) skillOptionDouble(unit, skill,
                "levels-per-extra-arrow", 5.0, 1.0, 30.0))
                + " tên lửa rơi quanh mục tiêu, cắm đuốc "
                + (int) skillOptionDouble(unit, skill, "torch-duration-seconds", 30.0, 1.0, 600.0)
                + " giây, tầm " + formatStat(skillOptionDouble(unit, skill,
                    "range-blocks", 20.8, 4.0, 64.0)) + " block";
            default -> "Chiến đấu với mục tiêu";
        };
    }

    private double scaledDuration(Unit unit, String skill, double fallback) {
        return Math.min(600.0, Math.round(skillOptionDouble(unit, skill, "duration-seconds",
            fallback, 1.0, 600.0) * skillPowerMultiplier(unit)));
    }

    private String skillTargetDescription(Unit unit, String skill) {
        return switch (skillOptionString(unit, skill, "target", "OWNER_AND_ALLIES")) {
            case "OWNER" -> "chủ nhân";
            case "ALLIES" -> "đồng minh";
            default -> "chủ nhân và đồng minh";
        };
    }

    private static String skillDisplayName(String skill) {
        return switch (skill.toUpperCase(Locale.ROOT)) {
            case "ATTACK" -> "Đánh cận chiến";
            case "RANGED_ATTACK" -> "Tấn công tầm xa";
            case "HEAL_OWNER" -> "Hồi máu chủ nhân";
            case "HEAL_ALLIES" -> "Hồi máu đồng đội";
            case "BUFF_OWNER" -> "Cường hóa chủ nhân";
            case "BUFF_ALLIES" -> "Cường hóa đồng đội";
            case "SHIELD_OWNER" -> "Khiên bảo hộ";
            case "VOLLEY" -> "Bắn loạt tên";
            case "POWER_STRIKE" -> "Cường kích";
            case "TAUNT" -> "Khiêu khích";
            case "CLEANSE_OWNER" -> "Thanh tẩy chủ nhân";
            case "FIRE_BOLT" -> "Hỏa cầu";
            case "ROOT_TARGET" -> "Trói chân mục tiêu";
            case "LIFESTEAL" -> "Hút máu";
            case "BERSERK" -> "Cuồng nộ";
            case "SUMMON_VEXES" -> "Triệu hồi Vex";
            case "LANCE_CHARGE" -> "Lao giáo";
            case "ILLUMINATION" -> "Soi Sáng";
            default -> "Kỹ năng chiến đấu";
        };
    }

    private static String localizedEffectName(String effect) {
        return switch (effect.toUpperCase(Locale.ROOT)) {
            case "SPEED" -> "Tốc độ";
            case "SLOWNESS" -> "Chậm chạp";
            case "REGENERATION" -> "Hồi phục";
            case "RESISTANCE" -> "Kháng cự";
            case "STRENGTH" -> "Sức mạnh";
            case "POISON" -> "Độc";
            case "WITHER" -> "Khô héo";
            case "WEAKNESS" -> "Suy yếu";
            case "BLINDNESS" -> "Mù";
            case "HUNGER" -> "Đói";
            case "FIRE_RESISTANCE" -> "Kháng lửa";
            case "NIGHT_VISION" -> "Nhìn trong đêm";
            case "INVISIBILITY" -> "Tàng hình";
            default -> "Hiệu ứng hỗ trợ";
        };
    }

    private static String skillCooldownKey(String skill) {
        return skill.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String formatStat(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private ItemStack menuItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onServiceMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ServiceMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || !holder.viewer.equals(player.getUniqueId())
                || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        String choice = holder.choices.get(slot);
        switch (holder.type) {
            case WELCOME_GIFT -> {
                if (slot == 49) player.closeInventory();
                else if (choice != null) selectWelcomeGift(player, choice);
            }
            case MAIN -> {
                if (slot == 10) player.performCommand("vnstats");
                else if (slot == 12) player.performCommand("vnhardcore");
                else if (slot == 14 && enabled) openArmyMenu(player, 0);
                else if (slot == 16) player.performCommand("reward");
                else if (slot == 22) player.performCommand("vnhelp");
                else if (slot == 24 && hasAdminMenuAccess(player)) openAdminMenu(player);
            }
            case ADMIN -> {
                if (slot == 11 && player.hasPermission(ADMIN_PERMISSION)) openAdminArmyPlayers(player, 0);
                else if (slot == 12) openDisasterMenu(player);
                else if (slot == 13) player.performCommand("vnevent stop");
                else if (slot == 14) openBossMenu(player);
                else if (slot == 15) player.performCommand("vnboss kill");
                else if (slot == 16) player.performCommand("vnreload");
                else if (slot == 17) player.performCommand("spawn");
                else if (slot == 18) player.performCommand("vnspawn set");
                else if (slot == 19) openTimeMenu(player);
                else if (slot == 20) openDayMenu(player);
                else if (slot == 21) openProtectionMenu(player);
                else if (slot == 22) openTeleportPlayers(player, 0);
                else if (slot == 23) openAdminGroupMenu(player);
                else if (slot == 24) openAdminUserPlayers(player, 0);
                else if (slot == 25) player.performCommand("fly");
                else if (slot == 26) player.performCommand("nodame");
                else if (slot == 27) player.performCommand("gm0");
                else if (slot == 28) player.performCommand("gm1");
                else if (slot == 29) player.performCommand("gm2");
                else if (slot == 30) player.performCommand("gm3");
                else if (slot == 31) openAdminScoreboardConfirm(player);
                else if (slot == 32) openAdminDeathPlayers(player);
                else if (slot == 33) openAdminSpawnWorlds(player);
                else if (slot == 34 && player.hasPermission("vnmine.command.death.history")) {
                    player.performCommand("vndeathhistory");
                }
                else if (slot == 49) openMainMenu(player);
            }
            case UNIT_DELETE_CONFIRM -> {
                if (slot == 15 || slot == 22) {
                    UUID unitId = holder.itemType == null ? null : UUID.fromString(holder.itemType);
                    Unit unit = unitId == null ? null : units.get(unitId);
                    if (unit == null || !unit.owner.equals(player.getUniqueId())) openArmyMenu(player, 0);
                    else openUnitMenu(player, unit);
                }
                else if (slot == 11) {
                    Unit unit = units.get(UUID.fromString(holder.itemType));
                    if (unit == null || !unit.owner.equals(player.getUniqueId())) {
                        player.sendMessage("§cKhông tìm thấy lính cần xóa.");
                        player.closeInventory();
                    } else {
                        handleRemoveCommand(player, new String[] {"remove", shortId(unit.id)});
                        if (!units.containsKey(unit.id)) player.closeInventory();
                    }
                }
            }
            case ADMIN_PLAYERS -> {
                if (slot == 45 && holder.page > 0) openAdminArmyPlayers(player, holder.page - 1);
                else if (slot == 53) openAdminArmyPlayers(player, holder.page + 1);
                else if (slot == 49) openAdminMenu(player);
                else if (choice != null) openGiveTypeMenu(player, UUID.fromString(choice));
            }
            case GIVE_TYPE -> {
                if (slot == 11) openClassMenu(player, holder.target, "army");
                else if (slot == 15) openClassMenu(player, holder.target, "token");
                else if (slot == 22) openAdminArmyPlayers(player, 0);
            }
            case CLASS_SELECT -> {
                if (slot == 49) {
                    if (holder.itemType.equals("hire")) openMainMenu(player);
                    else openGiveTypeMenu(player, holder.target);
                } else if (choice != null) {
                    openTierMenu(player, holder.target, holder.itemType, choice);
                }
            }
            case TIER_SELECT -> {
                if (slot == 49) openClassMenu(player, holder.target, holder.itemType);
                else if (choice != null) {
                    int tier = parsePositiveInt(choice);
                    player.closeInventory();
                    switch (holder.itemType) {
                        case "hire" -> hire(player, new String[] {"hire", holder.classId, Integer.toString(tier)});
                        case "token" -> {
                            Player target = Bukkit.getPlayer(holder.target);
                            if (target == null) player.sendMessage("§cNgười nhận đã offline.");
                            else handleTokenCommand(player, new String[] {"token", "give", target.getName(),
                                holder.classId, Integer.toString(tier)});
                        }
                        case "army" -> {
                            Player target = Bukkit.getPlayer(holder.target);
                            if (target == null) player.sendMessage("§cNgười nhận đã offline.");
                            else handleGiveCommand(player, new String[] {"give", target.getName(), "army",
                                holder.classId, Integer.toString(tier)});
                        }
                        default -> player.sendMessage("§cLoại cấp không hợp lệ.");
                    }
                }
            }
            case DISASTER_SELECT -> {
                if (slot == 49) openAdminMenu(player);
                else if (choice != null) {
                    if (!player.hasPermission("vnmine.command.disasters")) {
                        player.sendMessage("§cBạn không có quyền gọi thiên tai.");
                        return;
                    }
                    int warning = Math.max(1, Math.min(3600,
                        plugin.getConfig().getInt("menus.admin.warning-seconds", 30)));
                    int duration = Math.max(1, Math.min(86400,
                        plugin.getConfig().getInt("menus.admin.duration-seconds", 120)));
                    if (plugin.getDisasterManager().triggerDisaster(choice, warning, duration)) {
                        player.sendMessage("§aĐã kích hoạt §e" + plugin.getDisasterManager().getDisasterName(choice) + "§a.");
                    } else {
                        player.sendMessage("§cKhông thể kích hoạt thiên tai; có thể đang có sự kiện khác chạy.");
                    }
                    player.closeInventory();
                }
            }
            case BOSS_SELECT -> {
                if (slot == 49) openAdminMenu(player);
                else if (choice != null) {
                    if (!player.hasPermission("vnmine.command.bosses")) {
                        player.sendMessage("§cBạn không có quyền gọi boss.");
                        return;
                    }
                    openBossLevelMenu(player, choice);
                }
            }
            case BOSS_LEVEL_SELECT -> {
                if (slot == 22) openBossMenu(player);
                else if (choice != null) {
                    if (!player.hasPermission("vnmine.command.bosses")) {
                        player.sendMessage("§cBạn không có quyền gọi boss.");
                        return;
                    }
                    int warning = Math.max(1, Math.min(3600,
                        plugin.getConfig().getInt("menus.admin.warning-seconds", 30)));
                    int duration = Math.max(1, Math.min(86400,
                        plugin.getConfig().getInt("menus.admin.duration-seconds", 120)));
                    int bossLevel = parsePositiveInt(choice);
                    if (plugin.getBossEventManager().triggerBoss(holder.classId, bossLevel, warning, duration)) {
                        player.sendMessage("§aĐã gọi §e" + plugin.getBossEventManager().getBossName(holder.classId)
                            + " §alevel " + bossLevel + ".");
                    } else {
                        player.sendMessage("§cKhông thể gọi boss; có thể đang có boss khác chạy.");
                    }
                    player.closeInventory();
                }
            }
            case ADMIN_TIME -> {
                if (slot == 11) openTimeDurationMenu(player, "day");
                else if (slot == 15) openTimeDurationMenu(player, "night");
                else if (slot == 22) openAdminMenu(player);
            }
            case TIME_DURATION -> {
                if (slot == 22) openTimeMenu(player);
                else if (choice != null) {
                    player.closeInventory();
                    player.performCommand("vntime set " + holder.itemType + " " + choice);
                }
            }
            case ADMIN_DAY -> {
                if (slot == 22) openAdminMenu(player);
                else if (choice != null) {
                    player.closeInventory();
                    player.performCommand("vnday set " + choice);
                }
            }
            case ADMIN_PROTECT -> {
                if (slot == 22) openAdminMenu(player);
                else if (slot == 15) player.performCommand("vnspawn list");
                else if (slot == 16) openProtectionRemoveMenu(player);
                else if (choice != null && choice.startsWith("protect:")) {
                    player.closeInventory();
                    player.performCommand("vnspawn protect " + choice.substring("protect:".length()));
                }
            }
            case PROTECTION_REMOVE -> {
                if (slot == 49) openProtectionMenu(player);
                else if (choice != null) {
                    player.closeInventory();
                    player.performCommand("vnspawn remove " + choice);
                }
            }
            case TELEPORT_PLAYERS -> {
                if (slot == 45 && holder.page > 0) openTeleportPlayers(player, holder.page - 1);
                else if (slot == 53) openTeleportPlayers(player, holder.page + 1);
                else if (slot == 49) openAdminMenu(player);
                else if (choice != null) openTeleportWorlds(player, UUID.fromString(choice));
            }
            case TELEPORT_WORLDS -> {
                if (slot == 49) openTeleportPlayers(player, 0);
                else if (choice != null) {
                    Player target = Bukkit.getPlayer(holder.target);
                    if (target == null) player.sendMessage("§cNgười chơi đã offline.");
                    else {
                        player.closeInventory();
                        player.performCommand("tp " + target.getName() + " " + choice);
                    }
                }
            }
            case ADMIN_USER_PLAYERS -> {
                if (slot == 45 && holder.page > 0) openAdminUserPlayers(player, holder.page - 1);
                else if (slot == 53) openAdminUserPlayers(player, holder.page + 1);
                else if (slot == 49) openAdminMenu(player);
                else if (choice != null) openAdminUserActions(player, UUID.fromString(choice));
            }
            case ADMIN_USER_ACTION -> {
                if (slot == 22) openAdminUserPlayers(player, 0);
                else if (slot == 15) openAdminUserGroupSelect(player, holder.target);
                else if (slot == 16) openAdminUserPermissionActions(player, holder.target);
                else if (slot == 11 || slot == 13) {
                    Player target = Bukkit.getPlayer(holder.target);
                    if (target == null) player.sendMessage("§cNgười chơi đã offline.");
                    else {
                        player.closeInventory();
                        player.performCommand(slot == 11
                            ? "vnuser info " + target.getName() : "vnuser reset " + target.getName());
                    }
                }
            }
            case ADMIN_GROUPS -> {
                if (slot == 10) player.performCommand("vngroup list");
                else if (slot == 11) {
                    if (!player.hasPermission("vnmine.command.groups.manage")) {
                        player.sendMessage("§cBạn không có quyền quản lý nhóm.");
                        return;
                    }
                    awaitingGroupName.add(player.getUniqueId());
                    player.closeInventory();
                    player.sendMessage("§eNhập tên nhóm (1-32 ký tự, chữ thường/số/_/-) vào chat.");
                } else if (slot == 12) openAdminGroupSelect(player, "info");
                else if (slot == 13) openAdminGroupSelect(player, "delete");
                else if (slot == 14) openAdminGroupSelect(player, "grant");
                else if (slot == 15) openAdminGroupSelect(player, "revoke");
                else if (slot == 22) openAdminMenu(player);
            }
            case ADMIN_GROUP_SELECT -> {
                if (slot == 49) openAdminGroupMenu(player);
                else if (choice != null) {
                    switch (holder.itemType) {
                        case "info" -> {
                            player.closeInventory();
                            player.performCommand("vngroup info " + choice);
                        }
                        case "delete" -> {
                            ServiceMenuHolder confirm = createServiceMenu(player,
                                MenuType.ADMIN_GROUP_DELETE_CONFIRM, null, null, choice, 0, 27,
                                "Admin • Xóa nhóm " + choice);
                            confirm.inventory.setItem(11, menuItem(Material.RED_DYE, "§cXác nhận xóa",
                                List.of("§7Nhóm có thành viên hoặc nhóm hệ thống sẽ được giữ lại.")));
                            confirm.inventory.setItem(15, menuItem(Material.ARROW, "§7Hủy", List.of()));
                            player.openInventory(confirm.inventory);
                        }
                        case "grant", "revoke" -> openAdminPermissionSelect(player, null,
                            holder.itemType.equals("grant") ? "group-add" : "group-remove", choice);
                        default -> { }
                    }
                }
            }
            case ADMIN_GROUP_DELETE_CONFIRM -> {
                if (slot == 11) {
                    player.closeInventory();
                    player.performCommand("vngroup delete " + holder.classId);
                } else if (slot == 15) openAdminGroupSelect(player, "delete");
            }
            case ADMIN_PERMISSION_SELECT -> {
                if (slot == 49) {
                    if (holder.itemType.startsWith("group-")) {
                        openAdminGroupSelect(player,
                            holder.itemType.equals("group-add") ? "grant" : "revoke");
                    } else {
                        openAdminUserPermissionActions(player, holder.target);
                    }
                } else if (choice != null) {
                    player.closeInventory();
                    if (holder.itemType.startsWith("group-")) {
                        String action = holder.itemType.equals("group-add") ? "add" : "remove";
                        player.performCommand("vngroup permission " + action + " " + holder.classId + " " + choice);
                    } else {
                        Player target = Bukkit.getPlayer(holder.target);
                        if (target == null) player.sendMessage("§cNgười chơi đã offline.");
                        else {
                            String action = holder.itemType.equals("user-add") ? "add" : "remove";
                            player.performCommand("vnuser permission " + action + " " + target.getName() + " " + choice);
                        }
                    }
                }
            }
            case ADMIN_USER_GROUP_SELECT -> {
                if (slot == 49) openAdminUserActions(player, holder.target);
                else if (choice != null) {
                    Player target = Bukkit.getPlayer(holder.target);
                    if (target == null) player.sendMessage("§cNgười chơi đã offline.");
                    else {
                        player.closeInventory();
                        player.performCommand("vnuser group " + target.getName() + " " + choice);
                    }
                }
            }
            case ADMIN_USER_PERMISSION_ACTIONS -> {
                if (slot == 22) openAdminUserActions(player, holder.target);
                else if (slot == 11) openAdminPermissionSelect(player, holder.target, "user-add", null);
                else if (slot == 15) openAdminPermissionSelect(player, holder.target, "user-remove", null);
            }
            case ADMIN_SCOREBOARD_CONFIRM -> {
                if (slot == 11) {
                    player.closeInventory();
                    player.performCommand("scoreboard reset");
                } else if (slot == 15) openAdminMenu(player);
            }
            case ADMIN_DEATH_PLAYERS -> {
                if (slot == 49) openAdminMenu(player);
                else if (choice != null) {
                    Player target = Bukkit.getPlayer(UUID.fromString(choice));
                    if (target == null) player.sendMessage("§cNgười chơi đã offline.");
                    else {
                        player.closeInventory();
                        player.performCommand("vn death reset " + target.getName());
                    }
                }
            }
            case ADMIN_SPAWN_WORLD -> {
                if (slot == 49) openAdminMenu(player);
                else if (choice != null) {
                    player.closeInventory();
                    player.performCommand("vnspawn go " + choice);
                }
            }
        }
    }

    @EventHandler
    public void onServiceMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ServiceMenuHolder) event.setCancelled(true);
    }

    @EventHandler
    public void onArmyMenuClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        if (!(inventoryHolder instanceof ArmyMenuHolder)
                && !(inventoryHolder instanceof UnitMenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (inventoryHolder instanceof ArmyMenuHolder holder) {
            if (!holder.owner.equals(player.getUniqueId()) || event.getClickedInventory() != event.getView().getTopInventory()) return;
            int slot = event.getRawSlot();
            if (slot == 2) {
                recall(player, new String[] {"recall"});
                return;
            }
            if (slot == 4) {
                openClassMenu(player, null, "hire");
                return;
            }
            if (slot == 6) {
                rest(player, new String[] {"rest"});
                return;
            }
            if (slot == 49) {
                openMainMenu(player);
                return;
            }
            if (slot == 45 && holder.page > 0) {
                openArmyMenu(player, holder.page - 1);
                return;
            }
            if (slot == 53) {
                openArmyMenu(player, holder.page + 1);
                return;
            }
            if (slot < 0 || slot >= 54 || slot == 49) return;
            int row = slot / 9 - 1;
            int column = slot % 9;
            if (row < 0 || row > 3 || column < 1 || column > 7) return;
            int index = row * 7 + column - 1;
            if (index < holder.unitIds.size()) {
                Unit unit = units.get(holder.unitIds.get(index));
                if (unit != null && unit.owner.equals(player.getUniqueId())) openUnitMenu(player, unit);
            }
            return;
        }
        UnitMenuHolder holder = (UnitMenuHolder) inventoryHolder;
        if (!holder.owner.equals(player.getUniqueId())
                || event.getClickedInventory() != event.getView().getTopInventory()) return;
        Unit unit = units.get(holder.unit);
        if (unit == null || !unit.owner.equals(player.getUniqueId())) {
            openArmyMenu(player, 0);
            return;
        }
        switch (event.getRawSlot()) {
            case 10 -> updateUnitMode(player, unit, Mode.ATTACK);
            case 11 -> updateUnitMode(player, unit, Mode.DEFEND);
            case 12 -> updateUnitMode(player, unit, Mode.PROTECT);
            case 13 -> toggleMute(player, unit);
            case 14 -> toggleUnitDeployment(player, unit);
            case 16 -> openGear(player, new String[] {"gear", shortId(unit.id)});
            case 17 -> upgradeTier(player, unit);
            case 22 -> openArmyMenu(player, 0);
            case 15 -> openUnitDeleteConfirm(player, unit);

            default -> { return; }
        }
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof UnitMenuHolder) {
            openUnitMenu(player, unit);
        }
    }

    private boolean canUpgrade(Unit unit) {
        return unit.level >= maxLevel && unit.tier < unit.originalTier + 3;
    }

    private List<String> tierUpgradeLore(Unit unit) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Bậc hiện tại: §f" + unit.tier + " §7| Bậc gốc: §f" + unit.originalTier);
        if (unit.level < maxLevel) {
            lore.add("§cCần đạt cấp " + maxLevel + ".");
        } else if (unit.tier >= unit.originalTier + 3) {
            lore.add("§7Đã đạt giới hạn nâng bậc.");
        } else {
            int nextTier = unit.tier + 1;
            lore.add("§eNâng lên Bậc " + nextTier + ":");
            ConfigurationSection costs = settings.getConfigurationSection("classes." + unit.unitClass
                + ".tier-upgrades." + nextTier + ".materials");
            if (costs == null || costs.getKeys(false).isEmpty()) {
                lore.add("§cChưa cấu hình vật phẩm nâng bậc.");
            } else {
                for (String material : costs.getKeys(false)) {
                    lore.add("§7" + material + " x" + costs.getInt(material));
                }
            }
            lore.add("§aNhấn để nâng bậc.");
        }
        return lore;
    }

    private void upgradeTier(Player player, Unit unit) {
        if (!canUpgrade(unit)) {
            player.sendMessage(unit.level < maxLevel
                ? "§cLính phải đạt cấp " + maxLevel + " mới nâng bậc được."
                : "§cLính đã đạt giới hạn tăng tối đa 3 bậc so với bậc gốc.");
            return;
        }
        int nextTier = unit.tier + 1;
        ConfigurationSection section = settings.getConfigurationSection("classes." + unit.unitClass
            + ".tier-upgrades." + nextTier + ".materials");
        if (section == null || section.getKeys(false).isEmpty()) {
            player.sendMessage("§cChưa cấu hình vật phẩm nâng lên Bậc " + nextTier + " cho class này.");
            return;
        }
        Map<Material, Integer> costs = readMaterialCosts(section, player, "nâng lên Bậc " + nextTier);
        if (costs == null) return;
        List<String> missing = missingMaterials(player, costs);
        if (!missing.isEmpty()) {
            player.sendMessage("§cThiếu điều kiện nâng bậc:");
            missing.forEach(value -> player.sendMessage("§7- " + value));
            return;
        }
        ItemStack[] before = cloneContents(player.getInventory().getContents());
        int previousTier = unit.tier;
        int previousLevel = unit.level;
        int previousExperience = unit.experience;
        if (!removeMaterials(player, costs)) {
            player.getInventory().setContents(before);
            player.sendMessage("§cKhông thể trừ đủ vật phẩm; giao dịch đã được hoàn tác.");
            return;
        }
        unit.tier = nextTier;
        unit.level = 1;
        unit.experience = 0;
        Mob mob = activeEntities.get(unit.id);
        if (mob != null && mob.isValid()) {
            applyStats(unit, mob);
            updateEntityName(unit, mob);
        }
        if (!saveData()) {
            unit.tier = previousTier;
            unit.level = previousLevel;
            unit.experience = previousExperience;
            player.getInventory().setContents(before);
            if (mob != null && mob.isValid()) {
                applyStats(unit, mob);
                updateEntityName(unit, mob);
            }
            player.sendMessage("§cKhông thể lưu bậc mới; vật phẩm đã được hoàn lại.");
            return;
        }
        player.sendMessage("§a" + displayName(unit) + " đã nâng lên Bậc " + nextTier + "!");
    }

    @EventHandler
    public void onArmyMenuDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof ArmyMenuHolder || holder instanceof UnitMenuHolder) event.setCancelled(true);
    }

    private void updateUnitMode(Player player, Unit unit, Mode mode) {
        Mode previous = unit.mode;
        unit.mode = mode;
        if (!saveData()) {
            unit.mode = previous;
            player.sendMessage("§cKhông thể lưu chế độ lính; vui lòng thử lại.");
        }
    }

    private void toggleUnitDeployment(Player player, Unit unit) {
        if (!unit.deployed && unit.recoveryUntilMillis > System.currentTimeMillis()) {
            player.sendMessage("§cLính đang hồi phục, còn " + remainingRecoverySeconds(unit) + " giây.");
            return;
        }
        if (unit.deployed) {
            Mob mob = activeEntities.get(unit.id);
            if (mob == null || !mob.isValid()) mob = findExistingEntity(unit);
            if (mob != null && mob.isValid()) {
                captureEquipment(unit, mob);
                updateLocation(unit, mob.getLocation());
            }
            boolean previousHorseDead = unit.horseDead;
            UUID previousEntityId = unit.entityId;
            unit.entityId = null;
            unit.deployed = false;
            unit.horseDead = false;
            if (!saveData()) {
                unit.deployed = true;
                unit.horseDead = previousHorseDead;
                unit.entityId = previousEntityId;
                player.sendMessage("§cKhông thể lưu trạng thái lính; hãy thử lại.");
                return;
            }
            cleanupSummonedMinions(unit.id);
            removeMount(unit.id, mob);
            Mob removed = activeEntities.remove(unit.id);
            if (removed == null) removed = mob;
            if (removed != null && removed.isValid()) removed.remove();
            return;
        }
        if (deployedUnitCount(player.getUniqueId()) >= maxDeployedUnits) {
            player.sendMessage("§cBạn đã đạt giới hạn triệu hồi " + maxDeployedUnits
                + " lính. Hãy cho một lính nghỉ trước.");
            return;
        }
        unit.deployed = true;
        Mob mob = findExistingEntity(unit);
        if (mob == null) mob = spawnUnit(unit, player.getLocation().clone().add(0.8, 0.0, 0.8));
        if (mob == null) {
            unit.deployed = false;
            player.sendMessage("§cKhông thể triệu hồi lính này.");
            return;
        }
        ensurePaladinMounted(unit, mob);
        activeEntities.put(unit.id, mob);
        updateLocation(unit, mob.getLocation());
        if (!saveData()) {
            mob.remove();
            activeEntities.remove(unit.id);
            unit.deployed = false;
            unit.entityId = null;
            player.sendMessage("§cKhông thể lưu trạng thái lính; đã hủy triệu hồi.");
        }
    }

    private void openGear(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§eSử dụng: /vnarmy gear <ID>");
            return;
        }
        Unit unit = findOwnedByShortId(player.getUniqueId(), args[1]);
        if (unit == null) {
            player.sendMessage("§cKhông tìm thấy lính của bạn với ID bắt đầu bằng §e" + args[1] + "§c.");
            return;
        }
        openGear(player, unit);
    }

    private void openGear(Player player, Unit unit) {
        Mob active = activeEntities.get(unit.id);
        if (active != null && active.isValid()) captureEquipment(unit, active);
        GearHolder holder = new GearHolder(player.getUniqueId(), unit.id);
        Inventory inventory = Bukkit.createInventory(holder, 54, "Kho và trang bị #" + shortId(unit.id));
        holder.inventory = inventory;
        for (int slot = 0; slot < unit.inventory.length; slot++) {
            inventory.setItem(slot, cloneOrNull(unit.inventory[slot]));
        }
        ItemStack divider = menuItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 36; slot < 45; slot++) inventory.setItem(slot, divider.clone());
        inventory.setItem(45, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.HAND)));
        inventory.setItem(46, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.HEAD)));
        inventory.setItem(47, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.CHEST)));
        inventory.setItem(48, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.LEGS)));
        inventory.setItem(49, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.FEET)));
        inventory.setItem(50, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.OFF_HAND)));
        inventory.setItem(51, cloneOrNull(unit.horseArmor));
        for (int slot = 52; slot < 54; slot++) inventory.setItem(slot, divider.clone());
        player.openInventory(inventory);
        player.sendMessage("§7Kho lính: 36 ô đầu để chứa đồ; hàng cuối là tay chính, mũ, áo, quần, giày, tay phụ và giáp ngựa.");
    }

    private void loadWelcomeGiftClaims() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("[Gift] Could not create data folder for welcome-gift claims.");
            return;
        }
        YamlConfiguration loaded = new YamlConfiguration();
        if (!welcomeGiftFile.isFile()) {
            saveWelcomeGiftClaims();
            return;
        }
        try {
            loaded.load(welcomeGiftFile);
            for (String entry : loaded.getStringList("claimed")) {
                try {
                    claimedWelcomeGiftPlayers.add(UUID.fromString(entry));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("[Gift] Ignoring invalid UUID in new-player-gifts.yml: " + entry);
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "[Gift] Could not load new-player-gifts.yml; resetting claim state.", exception);
            claimedWelcomeGiftPlayers.clear();
        }
    }

    private void saveWelcomeGiftClaims() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("claimed", claimedWelcomeGiftPlayers.stream().map(UUID::toString).sorted().toList());
        try {
            data.save(welcomeGiftFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "[Gift] Could not save new-player-gifts.yml.", exception);
        }
    }

    private boolean isWelcomeGiftClaimed(UUID playerId) {
        return claimedWelcomeGiftPlayers.contains(playerId);
    }

    private boolean hasWelcomeGiftBag(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isWelcomeGiftBag(stack)) return true;
        }
        return false;
    }

    private boolean consumeWelcomeGiftBag(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isWelcomeGiftBag(stack)) {
                player.getInventory().setItem(slot, null);
                return true;
            }
        }
        return false;
    }

    private ItemStack createWelcomeGiftBag() {
        ItemStack bag = new ItemStack(Material.BLACK_BUNDLE);
        ItemMeta meta = bag.getItemMeta();
        meta.setDisplayName("§5§lTúi Quà Tân Thủ");
        meta.setLore(List.of("§7Nhận token triệu hồi lính đánh thuê bậc 1.", "§8Cầm hoặc nhấn chuột phải để mở."));
        meta.getPersistentDataContainer().set(welcomeGiftKey, PersistentDataType.BYTE, (byte) 1);
        bag.setItemMeta(meta);
        return bag;
    }

    private boolean isWelcomeGiftBag(ItemStack stack) {
        if (stack == null || stack.getType() != Material.BLACK_BUNDLE || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().getPersistentDataContainer().has(welcomeGiftKey, PersistentDataType.BYTE);
    }

    private boolean handleWelcomeGiftCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage("§eSử dụng: /quatanthu");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh chỉ dành cho người chơi.");
            return true;
        }
        if (isWelcomeGiftClaimed(player.getUniqueId())) {
            sender.sendMessage("§cBạn đã nhận quà tân thủ rồi.");
            return true;
        }
        if (hasOwnedMercenaries(player.getUniqueId())) {
            sender.sendMessage("§cBạn đã sở hữu lính đánh thuê nên không thể nhận quà tân thủ.");
            return true;
        }
        if (hasWelcomeGiftBag(player)) {
            sender.sendMessage("§eBạn đã có túi quà tân thủ trong túi đồ.");
            return true;
        }
        giveItem(player, createWelcomeGiftBag());
        sender.sendMessage("§aBạn nhận được §5Túi Quà Tân Thủ§a. Cầm lên hoặc nhấn chuột phải để mở.");
        return true;
    }

    private void openWelcomeGiftMenu(Player player) {
        if (isWelcomeGiftClaimed(player.getUniqueId())) {
            player.sendMessage("§cBạn đã nhận quà tân thủ rồi.");
            return;
        }
        if (hasOwnedMercenaries(player.getUniqueId())) {
            player.sendMessage("§cBạn đã sở hữu lính đánh thuê nên không thể dùng quà tân thủ.");
            return;
        }
        ServiceMenuHolder holder = createServiceMenu(player, MenuType.WELCOME_GIFT, null, null, null,
            0, 54, "Quà Tân Thủ");
        List<String> classIds = getClassIds();
        for (int index = 0; index < classIds.size() && index < 45; index++) {
            String classId = classIds.get(index);
            Material icon = Material.matchMaterial(settings.getString("classes." + classId + ".menu-icon", "IRON_SWORD"));
            if (icon == null || icon.isAir()) icon = Material.IRON_SWORD;
            holder.choices.put(index + 1, classId);
            holder.inventory.setItem(index + 1, menuItem(icon, "§e" + classDisplay(classId),
                List.of("§7Nhận token triệu hồi lính Bậc 1.", "§8Nhấn để chọn.")));
        }
        holder.inventory.setItem(49, menuItem(Material.ARROW, "§7Đóng", List.of("§8Quay lại chơi.")));
        player.openInventory(holder.inventory);
    }

    private void selectWelcomeGift(Player player, String classId) {
        if (isWelcomeGiftClaimed(player.getUniqueId())) {
            player.sendMessage("§cBạn đã nhận quà tân thủ rồi.");
            player.closeInventory();
            return;
        }
        if (hasOwnedMercenaries(player.getUniqueId())) {
            player.sendMessage("§cBạn đã sở hữu lính đánh thuê nên không thể dùng quà tân thủ.");
            player.closeInventory();
            return;
        }
        if (classId == null || normalizeClass(classId) == null) {
            player.sendMessage("§cLoại token không hợp lệ.");
            player.closeInventory();
            return;
        }
        if (!consumeWelcomeGiftBag(player)) {
            player.sendMessage("§cKhông tìm thấy túi quà tân thủ trong túi đồ của bạn.");
            player.closeInventory();
            return;
        }
        giveItem(player, createToken(classId, 1));
        giveItem(player, new ItemStack(Material.EMERALD, 8));
        claimedWelcomeGiftPlayers.add(player.getUniqueId());
        saveWelcomeGiftClaims();
        player.closeInventory();
        player.sendMessage("§aBạn đã nhận §e" + classDisplay(classId) + " §aBậc 1, thêm §e8 viên lục bảo §a, và túi quà tân thủ đã biến mất.");
    }

    private boolean isGearOpen(Player player, UUID unitId) {
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        return holder instanceof GearHolder gearHolder && gearHolder.unit.equals(unitId);
    }

    private void captureGearInventory(Player player, Unit unit, Inventory inventory) {
        EnumMap<org.bukkit.inventory.EquipmentSlot, ItemStack> equipment =
            new EnumMap<>(org.bukkit.inventory.EquipmentSlot.class);
        List<ItemStack> stored = new ArrayList<>();
        ItemStack bestHorseArmor = null;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isGearDecorationSlot(slot) && isGearDecoration(item)) continue;
            if (isHorseArmor(item)) {
                if (bestHorseArmor == null || horseArmorScore(item) > horseArmorScore(bestHorseArmor)) {
                    if (bestHorseArmor != null) stored.add(bestHorseArmor);
                    bestHorseArmor = item.clone();
                } else {
                    stored.add(item.clone());
                }
                continue;
            }
            addGearItem(stored, equipment, item);
        }
        unit.horseArmor = bestHorseArmor;
        for (org.bukkit.inventory.EquipmentSlot slot : List.of(
                org.bukkit.inventory.EquipmentSlot.HAND, org.bukkit.inventory.EquipmentSlot.HEAD,
                org.bukkit.inventory.EquipmentSlot.CHEST, org.bukkit.inventory.EquipmentSlot.LEGS,
                org.bukkit.inventory.EquipmentSlot.FEET, org.bukkit.inventory.EquipmentSlot.OFF_HAND)) {
            unit.equipment.put(slot, cloneOrNull(equipment.get(slot)));
        }
        java.util.Arrays.fill(unit.inventory, null);
        for (int index = 0; index < Math.min(unit.inventory.length, stored.size()); index++) {
            unit.inventory[index] = stored.get(index).clone();
        }
        for (int index = unit.inventory.length; index < stored.size(); index++) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stored.get(index).clone());
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage("§eKho lính đầy; vật phẩm vượt giới hạn đã trả lại cho bạn.");
        }
    }

    private boolean isGearDecorationSlot(int slot) {
        return (slot >= 36 && slot < 45) || slot >= 52;
    }

    private boolean isGearDecoration(ItemStack item) {
        return item != null && item.getType() == Material.GRAY_STAINED_GLASS_PANE
            && item.hasItemMeta() && " ".equals(item.getItemMeta().getDisplayName());
    }

    private void addGearItem(List<ItemStack> stored,
                             EnumMap<org.bukkit.inventory.EquipmentSlot, ItemStack> equipment,
                             ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        org.bukkit.inventory.EquipmentSlot slot = detectedEquipmentSlot(item.getType());
        if (slot != null && !equipment.containsKey(slot)) equipment.put(slot, item.clone());
        else stored.add(item.clone());
    }

    private org.bukkit.inventory.EquipmentSlot detectedEquipmentSlot(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET") || material == Material.TURTLE_HELMET
                || material == Material.CARVED_PUMPKIN) return org.bukkit.inventory.EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE") || material == Material.ELYTRA) return org.bukkit.inventory.EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return org.bukkit.inventory.EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS")) return org.bukkit.inventory.EquipmentSlot.FEET;
        if (material == Material.SHIELD) return org.bukkit.inventory.EquipmentSlot.OFF_HAND;
        if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("BOW")
                || name.equals("CROSSBOW") || name.equals("TRIDENT") || name.endsWith("_SPEAR")
                || name.equals("MACE")) {
            return org.bukkit.inventory.EquipmentSlot.HAND;
        }
        return null;
    }

    private boolean handleTokenCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§cBạn không có quyền cấp lệnh triệu hồi.");
            return true;
        }
        if (args.length != 5 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§eSử dụng: /vnarmy token give <người chơi> <loại> <tier>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        String classId = normalizeClass(args[3]);
        int level = parsePositiveInt(args[4]);
        if (target == null || classId == null || !hasTier(level)) {
            sender.sendMessage("§cNgười chơi phải đang online; lớp/tier lính không hợp lệ.");
            return true;
        }
        ItemStack token = createToken(classId, level);
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(token);
        overflow.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
        sender.sendMessage("§aĐã cấp lệnh triệu hồi " + classDisplay(classId) + " tier " + level
            + " cho §e" + target.getName() + "§a.");
        target.sendMessage("§aBạn nhận được Lệnh Triệu Hồi " + classDisplay(classId) + " tier " + level + "§a.");
        return true;
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§cBạn không có quyền cấp token hoặc lính.");
            return true;
        }
        if (args.length != 4 && args.length != 5) {
            sender.sendMessage("§eDùng: /vnarmy give <người chơi> <token> <tier>");
            sender.sendMessage("§eHoặc: /vnarmy give <người chơi> <army> <loại> <tier>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cNgười chơi phải đang online.");
            return true;
        }
        if (args.length == 4 && args[2].equalsIgnoreCase("token")) {
            int level = parsePositiveInt(args[3]);
            if (!hasTier(level)) {
                sender.sendMessage("§cTier token không hợp lệ.");
                return true;
            }
            giveItem(target, createToken("any", level));
            sender.sendMessage("§aĐã cấp token triệu hồi tier " + level + " cho §e" + target.getName() + "§a.");
            target.sendMessage("§aBạn nhận được Lệnh Triệu Hồi tier " + level + "§a.");
            return true;
        }
        if (args.length == 5 && args[2].equalsIgnoreCase("army")) {
            String classId = normalizeClass(args[3]);
            int level = parsePositiveInt(args[4]);
            if (classId == null || !hasTier(level)) {
                sender.sendMessage("§cLớp lính hoặc tier không hợp lệ.");
                return true;
            }
            if (getOwnerUnits(target.getUniqueId()).size() >= maxUnits) {
                sender.sendMessage("§cNgười chơi đã đạt giới hạn " + maxUnits + " lính.");
                return true;
            }
            boolean deployImmediately = deployedUnitCount(target.getUniqueId()) < maxDeployedUnits;
            Unit unit = new Unit(UUID.randomUUID(), target.getUniqueId(), classId, level,
                Mode.PROTECT, deployImmediately);
            Mob mob = deployImmediately ? spawnUnit(unit, safeLocation(target.getLocation())) : null;
            if (deployImmediately && mob == null) {
                sender.sendMessage("§cKhông thể tạo lính; kiểm tra cấu hình entity.");
                return true;
            }
            units.put(unit.id, unit);
            if (mob != null) {
                activeEntities.put(unit.id, mob);
                updateLocation(unit, mob.getLocation());
            }
            if (!saveData()) {
                units.remove(unit.id);
                if (mob != null) {
                    activeEntities.remove(unit.id);
                    mob.remove();
                }
                removeUnitData(unit);
                saveData();
                sender.sendMessage("§cKhông thể lưu lính; chưa hoàn tất việc cấp.");
                return true;
            }
            String deploymentMessage = deployImmediately ? "" : " (đang nghỉ do đã đủ giới hạn triệu hồi)";
            sender.sendMessage("§aĐã cấp " + displayName(unit) + " tier " + level + " cho §e"
                + target.getName() + "§a" + deploymentMessage + ".");
            target.sendMessage("§aBạn được cấp " + displayName(unit) + " tier " + level
                + "§a" + deploymentMessage + ".");
            return true;
        }
        sender.sendMessage("§eDùng: /vnarmy give <người chơi> <token> <tier>");
        sender.sendMessage("§eHoặc: /vnarmy give <người chơi> <army> <loại> <tier>");
        return true;
    }

    private void giveItem(Player target, ItemStack item) {
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(item);
        overflow.values().forEach(stack -> target.getWorld().dropItemNaturally(target.getLocation(), stack));
    }

    private ItemStack createToken(String classId, int level) {
        ItemStack token = new ItemStack(Material.PAPER);
        ItemMeta meta = token.getItemMeta();
        String display = classId.equals("any") ? "Lính Đánh Thuê" : classDisplay(classId);
        meta.setDisplayName("§6§lLệnh Triệu Hồi " + display + " §fTier " + level);
        meta.setLore(List.of("§7Thuê lính tier " + level + ".", "§8Lệnh sẽ bị tiêu thụ khi dùng."));
        meta.getPersistentDataContainer().set(tokenClassKey, PersistentDataType.STRING, classId);
        meta.getPersistentDataContainer().set(tokenLevelKey, PersistentDataType.INTEGER, level);
        token.setItemMeta(meta);
        return token;
    }

    public ItemStack createBossSummonToken(String classId, int level) {
        String normalizedClass = normalizeClass(classId);
        return normalizedClass == null || !hasTier(level) ? null : createToken(normalizedClass, level);
    }

    private ItemStack findToken(Player player, String classId, int level) {
        ItemStack generic = null;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType() != Material.PAPER || !stack.hasItemMeta()) continue;
            PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
            if (!Integer.valueOf(level).equals(pdc.get(tokenLevelKey, PersistentDataType.INTEGER))) continue;
            String tokenClass = pdc.get(tokenClassKey, PersistentDataType.STRING);
            if (classId.equals(tokenClass)) return stack;
            if ("any".equals(tokenClass)) generic = stack;
        }
        return generic;
    }

    private Mob spawnUnit(Unit unit, Location location) {
        EntityType type = configuredEntityType(unit);
        if (type == null || !type.isAlive()) return null;
        String configuredType = settings.getString("classes." + unit.unitClass + ".entity-type", "");
        if ("berserker".equals(unit.unitClass) && "PIGLIN_BRUTE".equalsIgnoreCase(configuredType)) {
            plugin.getLogger().warning("[Army] Refusing to spawn PIGLIN_BRUTE for berserker; "
                + "using ZOMBIFIED_PIGLIN.");
        }
        Entity entity;
        try {
            entity = location.getWorld().spawnEntity(location, type);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING, "[Army] Could not spawn class " + unit.unitClass + ".", exception);
            return null;
        }
        if (!(entity instanceof Mob mob)) {
            entity.remove();
            plugin.getLogger().warning("[Army] Configured entity type is not a Mob: " + type.name());
            return null;
        }
        if (mob instanceof Villager) {
            mob.setAware(true);
            mob.setTarget(null);
        }
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        pdc.set(unitKey, PersistentDataType.STRING, unit.id.toString());
        pdc.set(ownerKey, PersistentDataType.STRING, unit.owner.toString());
        preventZombification(mob);
        if ("paladin".equals(unit.unitClass)) {
            Horse horse = getOrCreateUnitHorse(unit, location);
            if (horse != null) {
                horse.addPassenger(mob);
            }
        }
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.setCanPickupItems(false);
        mob.setAware(true);
        mob.setTarget(null);
        updateEntityName(unit, mob);
        applyStats(unit, mob);
        org.bukkit.inventory.EquipmentSlot handSlot = org.bukkit.inventory.EquipmentSlot.HAND;
        ItemStack savedWeapon = unit.equipment.get(handSlot);
        unit.equipment.put(handSlot, selectBestWeapon(unit, savedWeapon, configuredStartingWeapon(unit)));
        applyEquipment(unit, mob);
        unit.entityId = mob.getUniqueId();
        updateLocation(unit, mob.getLocation());
        return mob;
    }

    private EntityType configuredEntityType(Unit unit) {
        EntityType configured = EntityType.fromName(
            settings.getString("classes." + unit.unitClass + ".entity-type", ""));
        if ("berserker".equals(unit.unitClass) && configured == EntityType.PIGLIN_BRUTE) {
            return EntityType.ZOMBIFIED_PIGLIN;
        }
        return configured;
    }

    private void removeUnskilledVexes(Mob mob) {
        if (!(mob instanceof Evoker)) return;
        for (Entity nearby : mob.getNearbyEntities(24.0, 24.0, 24.0)) {
            if (nearby instanceof Vex vex && vex.getSummoner() == mob && !isSummonedMinion(vex)) {
                vex.remove();
            }
        }
    }

    private void cleanupSummonedMinions(UUID unitId) {
        Set<UUID> minionIds = summonedMinions.remove(unitId);
        if (minionIds == null) return;
        for (UUID minionId : minionIds) {
            removeStatusDisplay(minionId);
            Entity minion = Bukkit.getEntity(minionId);
            if (minion != null && minion.isValid()) minion.remove();
        }
    }

    private void applyStats(Unit unit, Mob mob) {
        double baseHealth = settings.getDouble("classes." + unit.unitClass + ".base-health", 24.0);
        double healthPerLevel = settings.getDouble("classes." + unit.unitClass + ".health-per-level", 4.0);
        int statRank = statRank(unit);
        double maxHealth = Math.max(1.0, baseHealth + Math.max(0, statRank - 1) * healthPerLevel);
        if (mob.getAttribute(Attribute.MAX_HEALTH) != null) {
            mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            mob.setHealth(maxHealth);
        }
        if (mob.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            double baseDamage = settings.getDouble("classes." + unit.unitClass + ".base-damage", 4.0);
            double damagePerLevel = settings.getDouble("classes." + unit.unitClass + ".damage-per-level", 0.5);
            mob.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(
                Math.max(0.0, baseDamage + Math.max(0, statRank - 1) * damagePerLevel));
        }
        if (mob.getAttribute(Attribute.ARMOR) != null) {
            double baseArmor = settings.getDouble("classes." + unit.unitClass + ".base-armor", 0.0);
            double armorPerLevel = settings.getDouble("classes." + unit.unitClass + ".armor-per-level", 0.0);
            mob.getAttribute(Attribute.ARMOR).setBaseValue(
                Math.max(0.0, baseArmor + Math.max(0, statRank - 1) * armorPerLevel));
        }
    }

    private void applyEquipment(Unit unit, Mob mob) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) return;
        ItemStack mainHand = selectBestWeapon(unit,
            unit.equipment.get(org.bukkit.inventory.EquipmentSlot.HAND), configuredStartingWeapon(unit));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.HAND, cloneOrNull(mainHand));
        equipment.setItemInMainHand(mainHand);
        equipment.setHelmet(cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.HEAD)));
        equipment.setChestplate(cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.CHEST)));
        equipment.setLeggings(cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.LEGS)));
        equipment.setBoots(cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.FEET)));
        equipment.setItemInOffHand(cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.OFF_HAND)));
        equipment.setItemInMainHandDropChance(0.0f);
        equipment.setHelmetDropChance(0.0f);
        equipment.setChestplateDropChance(0.0f);
        equipment.setLeggingsDropChance(0.0f);
        equipment.setBootsDropChance(0.0f);
        equipment.setItemInOffHandDropChance(0.0f);
    }

    private ItemStack configuredStartingWeapon(Unit unit) {
        String configuredWeapon = settings.getString("classes." + unit.unitClass + ".starting-weapon", "");
        Material material = Material.matchMaterial(configuredWeapon);
        if (material == null && "paladin".equals(unit.unitClass)
                && "IRON_SPEAR".equalsIgnoreCase(configuredWeapon)) {
            material = Material.TRIDENT;
        }
        return material == null || material.isAir() ? null : new ItemStack(material);
    }

    private ItemStack selectBestWeapon(Unit unit, ItemStack current, ItemStack starting) {
        ItemStack best = isCompatibleWeapon(current, starting) ? current : null;
        int bestInventorySlot = -1;
        if (isWeaponItem(starting) && (best == null || weaponScore(starting) > weaponScore(best))) best = starting;
        for (int slot = 0; slot < unit.inventory.length; slot++) {
            ItemStack candidate = unit.inventory[slot];
            if (isCompatibleWeapon(candidate, starting)
                    && (best == null || weaponScore(candidate) > weaponScore(best))) {
                best = candidate;
                bestInventorySlot = slot;
            }
        }
        if (bestInventorySlot >= 0) {
            unit.inventory[bestInventorySlot] = cloneOrNull(current);
        } else if (current != null && !current.getType().isAir() && !isWeaponItem(current)
                && best != current && !storeInUnitInventory(unit, current)) {
            return current.clone();
        }
        return best == null ? cloneOrNull(current) : best.clone();
    }

    private boolean isCompatibleWeapon(ItemStack candidate, ItemStack starting) {
        if (!isWeaponItem(candidate)) return false;
        return starting == null || starting.getType().isAir()
            || weaponFamily(candidate.getType()).equals(weaponFamily(starting.getType()));
    }

    private String weaponFamily(Material material) {
        String name = material.name();
        if (name.equals("BOW") || name.equals("CROSSBOW")) return "ranged";
        if (name.equals("TRIDENT") || name.endsWith("_SPEAR")) return "spear";
        if (name.endsWith("_HOE")) return "hoe";
        if (name.equals("STICK") || name.endsWith("_SWORD") || name.endsWith("_AXE")
                || name.equals("MACE")) return "melee";
        return name;
    }

    private boolean isWeaponItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String material = item.getType().name();
        return detectedEquipmentSlot(item.getType()) == org.bukkit.inventory.EquipmentSlot.HAND
            || material.endsWith("_HOE") || material.equals("STICK");
    }

    private boolean storeInUnitInventory(Unit unit, ItemStack item) {
        for (int slot = 0; slot < unit.inventory.length; slot++) {
            if (unit.inventory[slot] == null || unit.inventory[slot].getType().isAir()) {
                unit.inventory[slot] = item.clone();
                return true;
            }
        }
        return false;
    }

    private int weaponScore(ItemStack item) {
        String material = item.getType().name();
        int tier = material.startsWith("NETHERITE_") || material.equals("MACE") ? 6
            : material.startsWith("DIAMOND_") ? 5
            : material.startsWith("IRON_") || material.equals("TRIDENT") ? 4
            : material.startsWith("STONE_") ? 3
            : material.startsWith("GOLDEN_") ? 2
            : material.startsWith("WOODEN_") ? 1
            : material.equals("STICK") ? 0 : -1;
        int enchantmentBonus = item.getEnchantments().values().stream()
            .mapToInt(Integer::intValue).sum();
        return tier * 100 + enchantmentBonus;
    }

    private void updateEntityName(Unit unit, Mob mob) {
        String ownerName = Bukkit.getOfflinePlayer(unit.owner).getName();
        if (ownerName == null || ownerName.isBlank()) ownerName = "Người chơi";
        mob.setCustomName("§b§l[" + ownerName + "]§r §f" + displayName(unit)
            + " §eBậc " + unit.tier + " §fCấp " + unit.level);
        mob.setCustomNameVisible(true);
        namePacketListener.update(mob, unit.owner, ownerName, displayName(unit), unit.tier, unit.level);
    }

    private void updateStatusDisplay(Unit unit, Mob mob) {
        if (!statusDisplayEnabled) {
            statusPacketListener.remove(unit.id);
            return;
        }
        double maximumHealth = maxHealth(mob);
        double requiredExperience = nextLevelExperience(unit);
        int filledHealthBars = statusBarFilled(mob.getHealth(), maximumHealth);
        String healthBar = "§c" + "■".repeat(filledHealthBars)
            + "§7" + "□".repeat(statusDisplayBarLength - filledHealthBars);
        String statusLine = "§c❤: §f" + formatStat(mob.getHealth()) + "/" + formatStat(maximumHealth)
            + " §e✦ EXP: " + unit.experience + "/" + (int) requiredExperience;
        String ownerName = Bukkit.getOfflinePlayer(unit.owner).getName();
        if (ownerName == null || ownerName.isBlank()) ownerName = "Người chơi";
        String nameLine = "§a[" + ownerName + "] §f" + displayName(unit)
            + " §e[Bậc " + unit.tier + "] §f[Level: " + unit.level + "]";
        statusPacketListener.update(unit.id, mob, unit.owner,
            healthBar + "\n" + statusLine + "\n" + nameLine,
            statusDisplaySeeThrough);
    }

    private int statusBarFilled(double value, double maximum) {
        double progress = maximum <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, value / maximum));
        return (int) Math.round(progress * statusDisplayBarLength);
    }

    private void removeStatusDisplay(UUID unitId) {
        statusPacketListener.remove(unitId);
    }

    private void captureEquipment(Unit unit, Mob mob) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) return;
        if (mob instanceof PiglinAbstract) {
            reconcileEquipment(unit, mob, false);
        } else {
            unit.equipment.put(org.bukkit.inventory.EquipmentSlot.HAND, cloneOrNull(equipment.getItemInMainHand()));
            unit.equipment.put(org.bukkit.inventory.EquipmentSlot.HEAD, cloneOrNull(equipment.getHelmet()));
            unit.equipment.put(org.bukkit.inventory.EquipmentSlot.CHEST, cloneOrNull(equipment.getChestplate()));
            unit.equipment.put(org.bukkit.inventory.EquipmentSlot.LEGS, cloneOrNull(equipment.getLeggings()));
            unit.equipment.put(org.bukkit.inventory.EquipmentSlot.FEET, cloneOrNull(equipment.getBoots()));
            unit.equipment.put(org.bukkit.inventory.EquipmentSlot.OFF_HAND, cloneOrNull(equipment.getItemInOffHand()));
        }
        Entity mount = mountByUnit.containsKey(unit.id) ? Bukkit.getEntity(mountByUnit.get(unit.id)) : null;
        if (mount instanceof Horse horse && horse.isValid()) captureHorseArmor(unit.id, horse);
    }

    private Mob findExistingEntity(Unit unit) {
        if (unit.entityId != null) {
            Entity entity = Bukkit.getEntity(unit.entityId);
            if (entity instanceof Mob mob && isUnit(mob, unit.id)) {
                preventZombification(mob);
                activeEntities.put(unit.id, mob);
                repairEquipmentFromSaved(unit, mob);
                updateEntityName(unit, mob);
                return mob;
            }
        }
        World world = Bukkit.getWorld(unit.worldName == null ? "" : unit.worldName);
        if (world == null) return null;
        int centerX = ((int) Math.floor(unit.x)) >> 4;
        int centerZ = ((int) Math.floor(unit.z)) >> 4;
        List<org.bukkit.Chunk> newlyLoaded = new ArrayList<>();
        Set<UUID> seenEntities = new HashSet<>();
        Mob found = null;
        try {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int chunkX = centerX + dx;
                    int chunkZ = centerZ + dz;
                    if (!world.isChunkGenerated(chunkX, chunkZ)) continue;
                    boolean wasLoaded = world.isChunkLoaded(chunkX, chunkZ);
                    org.bukkit.Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                    if (!wasLoaded) newlyLoaded.add(chunk);
                    for (Entity entity : chunk.getEntities()) {
                        if (!(entity instanceof Mob mob) || !isUnit(mob, unit.id)) continue;
                        if (seenEntities.add(entity.getUniqueId()) && found == null) found = mob;
                        else entity.remove();
                    }
                }
            }
        } finally {
            for (org.bukkit.Chunk chunk : newlyLoaded) world.unloadChunkRequest(chunk.getX(), chunk.getZ());
        }
        if (found != null) {
            unit.entityId = found.getUniqueId();
            activeEntities.put(unit.id, found);
            preventZombification(found);
            updateEntityName(unit, found);
            repairEquipmentFromSaved(unit, found);
            return found;
        }
        return null;
    }

    private boolean isUnit(Entity mob, UUID unitId) {
        return unitId.toString().equals(
            mob.getPersistentDataContainer().get(unitKey, PersistentDataType.STRING));
    }

    private boolean isMercenary(Entity entity) {
        return entity.getPersistentDataContainer().has(unitKey, PersistentDataType.STRING)
            || isSummonedMinion(entity);
    }

    private void preventZombification(Mob mob) {
        if (mob instanceof PiglinAbstract piglin) piglin.setImmuneToZombification(true);
    }

    private boolean isMercenaryMount(Entity entity) {
        if (!(entity instanceof Horse)) return false;
        UUID entityId = entity.getUniqueId();
        return entity.getPersistentDataContainer().has(horseMountKey, PersistentDataType.STRING)
            || mountByUnit.containsValue(entityId) || activeEntities.values().stream()
            .anyMatch(rider -> rider.isValid() && isMercenary(rider) && rider.getVehicle() == entity);
    }

    private boolean isSummonedMinion(Entity entity) {
        return entity.getPersistentDataContainer().has(summonedMinionKey, PersistentDataType.STRING);
    }

    private Location safeLocation(Location origin) {
        World world = origin.getWorld();
        if (world == null) return origin;
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = origin.getBlockX() + dx;
                    int z = origin.getBlockZ() + dz;
                    int y = world.getHighestBlockYAt(x, z) + 1;
                    if (y >= world.getMaxHeight() - 1) continue;
                    Location candidate = new Location(world, x + 0.5, y, z + 0.5, origin.getYaw(), origin.getPitch());
                    if (candidate.getBlock().isPassable()
                            && candidate.clone().add(0, 1, 0).getBlock().isPassable()
                            && candidate.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) return candidate;
                }
            }
        }
        return origin.clone().add(1.5, 0, 1.5);
    }

    private void updateLocation(Unit unit, Location location) {
        if (location.getWorld() == null) return;
        unit.worldName = location.getWorld().getName();
        unit.x = location.getX();
        unit.y = location.getY();
        unit.z = location.getZ();
    }

    private void saveUnit(Unit unit) {
        String path = "units." + unit.id;
        data.set(path + ".owner", unit.owner.toString());
        data.set(path + ".class", unit.unitClass);
        data.set(path + ".tier", unit.tier);
        data.set(path + ".original-tier", unit.originalTier);
        data.set(path + ".level", unit.level);
        data.set(path + ".experience", unit.experience);
        data.set(path + ".custom-name", unit.customName);
        data.set(path + ".recovery-until", unit.recoveryUntilMillis);
        data.set(path + ".gear-customized", unit.gearCustomized);
        data.set(path + ".mode", unit.mode.name());
        data.set(path + ".deployed", unit.deployed);
        data.set(path + ".horse-dead", unit.horseDead);
        data.set(path + ".muted", unit.muted);
        data.set(path + ".entity-uuid", unit.entityId == null ? null : unit.entityId.toString());
        data.set(path + ".world", unit.worldName);
        data.set(path + ".x", unit.x);
        data.set(path + ".y", unit.y);
        data.set(path + ".z", unit.z);
        data.set(path + ".horse-armor", cloneOrNull(unit.horseArmor));
        for (org.bukkit.inventory.EquipmentSlot slot : List.of(
                org.bukkit.inventory.EquipmentSlot.HAND,
                org.bukkit.inventory.EquipmentSlot.HEAD,
                org.bukkit.inventory.EquipmentSlot.CHEST,
                org.bukkit.inventory.EquipmentSlot.LEGS,
                org.bukkit.inventory.EquipmentSlot.FEET,
                org.bukkit.inventory.EquipmentSlot.OFF_HAND)) {
            data.set(path + ".equipment." + slotPath(slot), cloneOrNull(unit.equipment.get(slot)));
        }
        for (int slot = 0; slot < unit.inventory.length; slot++) {
            data.set(path + ".inventory." + slot, cloneOrNull(unit.inventory[slot]));
        }
    }

    private boolean saveData() {
        for (Unit unit : units.values()) saveUnit(unit);
        if (data == null) data = new YamlConfiguration();
        File temporary = new File(dataFile.getParentFile(), dataFile.getName() + ".tmp");
        try {
            if (!dataFile.getParentFile().isDirectory() && !dataFile.getParentFile().mkdirs()) {
                throw new IOException("Could not create plugin data directory.");
            }
            data.save(temporary);
            try {
                Files.move(temporary.toPath(), dataFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "[Army] Could not save mercenaries.yml.", exception);
            return false;
        } finally {
            if (temporary.exists() && !temporary.delete()) {
                plugin.getLogger().warning("[Army] Could not remove temporary data file " + temporary.getName() + ".");
            }
        }
    }

    private void removeUnitData(Unit unit) {
        cleanupSummonedMinions(unit.id);
        data.set("units." + unit.id, null);
    }

    private static double maxHealth(LivingEntity entity) {
        return entity.getAttribute(Attribute.MAX_HEALTH) == null
            ? 0.0 : entity.getAttribute(Attribute.MAX_HEALTH).getValue();
    }

    private boolean hasMaterials(Player player, Map<Material, Integer> costs) {
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            int count = 0;
            for (ItemStack stack : player.getInventory().getStorageContents()) {
                if (stack != null && stack.getType() == cost.getKey()) count += stack.getAmount();
            }
            if (count < cost.getValue()) return false;
        }
        return true;
    }

    private List<String> missingMaterials(Player player, Map<Material, Integer> costs) {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            int count = 0;
            for (ItemStack stack : player.getInventory().getStorageContents()) {
                if (stack != null && stack.getType() == cost.getKey()) count += stack.getAmount();
            }
            if (count < cost.getValue()) {
                missing.add(cost.getKey().name() + ": cần " + cost.getValue() + ", hiện có " + count);
            }
        }
        return missing;
    }

    private Map<Material, Integer> readMaterialCosts(ConfigurationSection section,
                                                     Player player, String description) {
        Map<Material, Integer> costs = new EnumMap<>(Material.class);
        for (String materialName : section.getKeys(false)) {
            Material material = Material.matchMaterial(materialName);
            int amount = section.getInt(materialName, -1);
            if (material == null || material.isAir() || amount < 0 || amount > 1_000_000) {
                player.sendMessage("§cCấu hình vật phẩm chi phí không hợp lệ: " + materialName + ".");
                plugin.getLogger().warning("[Army] Invalid material cost '" + materialName
                    + "' for " + description + ".");
                return null;
            }
            if (amount > 0) costs.put(material, amount);
        }
        return costs;
    }

    private boolean removeMaterials(Player player, Map<Material, Integer> costs) {
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            int remaining = cost.getValue();
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack stack = contents[slot];
                if (stack == null || stack.getType() != cost.getKey()) continue;
                int removed = Math.min(remaining, stack.getAmount());
                int updatedAmount = stack.getAmount() - removed;
                remaining -= removed;
                if (updatedAmount <= 0) {
                    contents[slot] = null;
                } else {
                    ItemStack remainder = stack.clone();
                    remainder.setAmount(updatedAmount);
                    contents[slot] = remainder;
                }
                if (remaining == 0) break;
            }
            if (remaining > 0) return false;
            player.getInventory().setStorageContents(contents);
        }
        return true;
    }

    private boolean removeOne(Player player, ItemStack token) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || !stack.isSimilar(token) || stack.getAmount() <= 0) continue;
            if (stack.getAmount() == 1) contents[slot] = null;
            else stack.setAmount(stack.getAmount() - 1);
            player.getInventory().setStorageContents(contents);
            return true;
        }
        return false;
    }

    private List<Unit> getOwnerUnits(UUID owner) {
        return units.values().stream().filter(unit -> unit.owner.equals(owner))
            .sorted(Comparator.comparing(unit -> unit.id)).toList();
    }

    private boolean hasOwnedMercenaries(UUID owner) {
        return !getOwnerUnits(owner).isEmpty();
    }

    private Unit findOwnedByShortId(UUID owner, String shortId) {
        String prefix = shortId.toLowerCase(Locale.ROOT);
        List<Unit> matches = getOwnerUnits(owner).stream()
            .filter(unit -> unit.id.toString().startsWith(prefix)).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private List<String> getClassIds() {
        ConfigurationSection classes = settings.getConfigurationSection("classes");
        return classes == null ? List.of() : classes.getKeys(false).stream().sorted().toList();
    }

    private List<String> getTierIds() {
        ConfigurationSection tiers = settings.getConfigurationSection("tiers");
        return tiers == null ? List.of() : tiers.getKeys(false).stream()
            .sorted(Comparator.comparingInt(MercenaryManager::parsePositiveInt)).toList();
    }

    private String normalizeClass(String value) {
        if (value == null) return null;
        String normalized = value.toLowerCase(Locale.ROOT);
        return settings.isConfigurationSection("classes." + normalized) ? normalized : null;
    }

    private boolean hasTier(int level) {
        return level >= 1 && level <= 10 && settings.isConfigurationSection("tiers." + level);
    }

    private int statRank(Unit unit) {
        return Math.min(1000, unit.tier + unit.level - 1);
    }

    private int nextLevelExperience(Unit unit) {
        return (int) Math.min(Integer.MAX_VALUE, (long) experiencePerLevel * unit.level);
    }

    private String classDisplay(String classId) {
        return settings.getString("classes." + classId + ".display-name", classId);
    }

    private String displayName(Unit unit) {
        return unit.customName == null || unit.customName.isBlank()
            ? classDisplay(unit.unitClass) : unit.customName;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static Mode parseMode(String value) {
        try {
            return Mode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Mode.PROTECT;
        }
    }

    private static String modeName(Mode mode) {
        return switch (mode) {
            case ATTACK -> "Tấn công";
            case DEFEND -> "Phòng thủ";
            case PROTECT -> "Bảo vệ chủ";
        };
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static int parsePositiveInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private int readBoundedInt(YamlConfiguration source, String path, int fallback, int min, int max) {
        if (source.contains(path) && !source.isInt(path) && !source.isDouble(path)) {
            plugin.getLogger().warning("[Army] " + path + " is not numeric; using " + fallback + ".");
            return fallback;
        }
        int value = source.getInt(path, fallback);
        if (value < min || value > max) {
            plugin.getLogger().warning("[Army] " + path + " must be between " + min + " and " + max
                + "; using " + fallback + ".");
            return fallback;
        }
        return value;
    }

    private double readBoundedDouble(YamlConfiguration source, String path, double fallback, double min, double max) {
        if (source.contains(path) && !source.isInt(path) && !source.isDouble(path)) {
            plugin.getLogger().warning("[Army] " + path + " is not numeric; using " + fallback + ".");
            return fallback;
        }
        double value = source.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < min || value > max) {
            plugin.getLogger().warning("[Army] " + path + " must be between " + min + " and " + max
                + "; using " + fallback + ".");
            return fallback;
        }
        return value;
    }

    private static ItemStack copyOrNull(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) copy[i] = cloneOrNull(contents[i]);
        return copy;
    }

    private static String slotPath(org.bukkit.inventory.EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> "main-hand";
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            case OFF_HAND -> "off-hand";
            default -> slot.name().toLowerCase(Locale.ROOT);
        };
    }

    private static List<String> matching(String input, String... options) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) matches.add(option);
        return matches;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§lQuản lý lính đánh thuê:");
        sender.sendMessage("§e/vnarmy gui §7- Mở giao diện quản lý đội quân.");
        sender.sendMessage("§e/vnarmy list [user] §7- Xem đội quân của bạn; admin có thể xem người chơi khác.");
        sender.sendMessage("§e/vnarmy hire <loại> <bậc> §7- Thuê bằng token, XP và vật phẩm.");
        sender.sendMessage("§e/vnarmy recall [ID] | rest [ID] §7- Gọi tối đa " + maxDeployedUnits
            + " lính bậc/level cao nhất hoặc gọi/nghỉ một lính.");
        sender.sendMessage("§e/vnarmy rename <ID> <tên mới> §7- Đổi tên lính.");
        sender.sendMessage("§e/vnarmy attack | defend | protect §7- Đổi xu hướng AI của cả đội.");
        sender.sendMessage("§e/vnarmy gear <ID> §7- Mở kho trang bị cho một lính.");
        sender.sendMessage("§e/vnarmy mute <ID> §7- Bật/tắt âm thanh của một lính.");
        sender.sendMessage("§e/vnarmy remove <ID> §7- Xóa lính khỏi đội của bạn.");
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§e/vnarmy remove <user> <ID> §7- Xóa lính của người chơi.");
            sender.sendMessage("§e/vnarmy clear <user> §7- Xóa toàn bộ lính của người chơi.");
            sender.sendMessage("§e/vnarmy give <người chơi> token <tier> §7- Cấp lệnh triệu hồi.");
            sender.sendMessage("§e/vnarmy give <người chơi> army <loại> <tier> §7- Cấp lính trực tiếp.");
        }
        sender.sendMessage("§7Loại lính: " + String.join(", ", getClassIds()));
        sender.sendMessage("§7Bậc thuê: " + String.join(", ", getTierIds())
            + " §8| §7Cấp tối đa: §f" + maxLevel + " §8| §7Sở hữu: §f" + maxUnits
            + " §8| §7Triệu hồi: §f" + maxDeployedUnits);
    }

    public void stop() {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
        for (BukkitTask task : torchRemovalTasks.values()) task.cancel();
        torchRemovalTasks.clear();
        for (Unit unit : units.values()) {
            removeStatusDisplay(unit.id);
            Mob mob = activeEntities.remove(unit.id);
            if (mob == null || !mob.isValid()) mob = findExistingEntity(unit);
            if (mob != null && mob.isValid()) {
                captureEquipment(unit, mob);
                updateLocation(unit, mob.getLocation());
                unit.entityId = null;
                mob.remove();
                activeEntities.remove(unit.id);
            }
        }
        if (!saveData()) plugin.getLogger().severe("[Army] Could not persist mercenaries on plugin shutdown.");
        namePacketListener.close();
        statusPacketListener.close();
    }
}
