package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import org.bukkit.Bukkit;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
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
    private static final String[] COMMANDS = {
        "list", "hire", "recall", "rest", "attack", "defend", "protect", "gear", "token"
    };
    private final VnMineHardcore plugin;
    private final File configFile;
    private final File dataFile;
    private final NamespacedKey unitKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey tokenClassKey;
    private final NamespacedKey tokenLevelKey;
    private final Map<UUID, Unit> units = new HashMap<>();
    private final Map<UUID, Mob> activeEntities = new HashMap<>();
    private final Map<UUID, Map<String, Long>> skillCooldowns = new HashMap<>();
    private YamlConfiguration settings;
    private YamlConfiguration data;
    private boolean enabled;
    private int maxUnits;
    private double followDistance;
    private double teleportDistance;
    private double targetRadius;
    private int aiIntervalTicks;
    private int skillCooldownTicks;
    private double healAmount;
    private double healRadius;
    private String ownerBuff;
    private int ownerBuffTicks;
    private BukkitRunnable aiTask;

    private enum Mode {
        ATTACK, DEFEND, PROTECT
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
        private final int level;
        private Mode mode;
        private boolean deployed;
        private UUID entityId;
        private String worldName;
        private double x;
        private double y;
        private double z;
        private final EnumMap<org.bukkit.inventory.EquipmentSlot, ItemStack> equipment =
            new EnumMap<>(org.bukkit.inventory.EquipmentSlot.class);

        private Unit(UUID id, UUID owner, String unitClass, int level, Mode mode, boolean deployed) {
            this.id = id;
            this.owner = owner;
            this.unitClass = unitClass;
            this.level = level;
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

    public MercenaryManager(VnMineHardcore plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "armies.yml");
        dataFile = new File(plugin.getDataFolder(), "mercenaries.yml");
        unitKey = new NamespacedKey(plugin, "army_unit");
        ownerKey = new NamespacedKey(plugin, "army_owner");
        tokenClassKey = new NamespacedKey(plugin, "army_token_class");
        tokenLevelKey = new NamespacedKey(plugin, "army_token_level");
        loadSettings();
        loadData();
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
            return true;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.SEVERE, "[Army] Could not reload armies.yml; keeping old settings.",
                exception);
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh này chỉ dành cho người chơi.");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> listUnits(player);
            case "hire" -> hire(player, args);
            case "recall" -> recall(player);
            case "rest" -> rest(player);
            case "attack" -> setMode(player, Mode.ATTACK);
            case "defend" -> setMode(player, Mode.DEFEND);
            case "protect" -> setMode(player, Mode.PROTECT);
            case "gear" -> openGear(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(COMMAND_PERMISSION)) return List.of();
        if (args.length == 1) return matching(args[0], COMMANDS);
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        despawnForOwner(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMercenaryDeath(EntityDeathEvent event) {
        String idValue = event.getEntity().getPersistentDataContainer().get(unitKey, PersistentDataType.STRING);
        if (idValue == null) return;
        UUID id;
        try {
            id = UUID.fromString(idValue);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("[Army] Ignoring invalid unit UUID on entity " + event.getEntity().getUniqueId());
            return;
        }
        Unit unit = units.remove(id);
        activeEntities.remove(id);
        skillCooldowns.remove(id);
        if (unit != null) {
            captureEquipment(unit, (Mob) event.getEntity());
            unit.equipment.values().stream()
                .filter(item -> item != null && !item.getType().isAir())
                .map(ItemStack::clone)
                .forEach(event.getDrops()::add);
            removeUnitData(unit);
            Player owner = Bukkit.getPlayer(unit.owner);
            if (owner != null) owner.sendMessage("§cLính đánh thuê §e" + displayName(unit)
                + " §cđã tử trận. Trang bị rơi tại vị trí chiến đấu.");
            if (!saveData()) {
                plugin.getLogger().severe("[Army] Could not persist the loss of mercenary " + id + ".");
            }
        }
    }

    @EventHandler
    public void onMercenaryTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !isMercenary(mob)) return;
        if (event.getTarget() != null && !(event.getTarget() instanceof Monster)) event.setCancelled(true);
    }

    @EventHandler
    public void onGearInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GearHolder holder)
                || !(event.getPlayer() instanceof Player player)
                || !holder.owner.equals(player.getUniqueId())) return;
        Unit unit = units.get(holder.unit);
        if (unit == null || !unit.owner.equals(player.getUniqueId())) return;
        Inventory inventory = event.getInventory();
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.HAND, copyOrNull(inventory.getItem(10)));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.HEAD, copyOrNull(inventory.getItem(12)));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.CHEST, copyOrNull(inventory.getItem(14)));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.LEGS, copyOrNull(inventory.getItem(16)));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.FEET, copyOrNull(inventory.getItem(19)));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.OFF_HAND, copyOrNull(inventory.getItem(22)));
        Mob mob = activeEntities.get(unit.id);
        if (mob != null && mob.isValid()) applyEquipment(unit, mob);
        if (!saveData()) {
            plugin.getLogger().severe("[Army] Could not save gear changes for mercenary " + unit.id + ".");
            player.sendMessage("§cKhông thể lưu trang bị lính vào mercenaries.yml; giữ lính hiện tại và thử lưu lại.");
            return;
        }
        player.sendMessage("§aĐã lưu trang bị cho lính §e#" + shortId(unit.id) + "§a.");
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
        if (loaded.getConfigurationSection("classes") == null
                || loaded.getConfigurationSection("tiers") == null) {
            throw new InvalidConfigurationException("armies.yml must define classes and tiers.");
        }
        if (loaded.contains("enabled") && !loaded.isBoolean("enabled")) {
            throw new InvalidConfigurationException("enabled must be true or false.");
        }
        if (loaded.getConfigurationSection("classes").getKeys(false).isEmpty()
                || loaded.getConfigurationSection("tiers").getKeys(false).isEmpty()) {
            throw new InvalidConfigurationException("armies.yml must define at least one class and one tier.");
        }
        for (String classId : loaded.getConfigurationSection("classes").getKeys(false)) {
            if (!classId.matches("[a-z0-9_-]{1,32}")) {
                throw new InvalidConfigurationException("Invalid mercenary class ID '" + classId + "'.");
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
                if (!Set.of("ATTACK", "HEAL_OWNER", "HEAL_ALLIES", "BUFF_OWNER")
                        .contains(skill.toUpperCase(Locale.ROOT))) {
                    throw new InvalidConfigurationException(
                        "Unknown skill '" + skill + "' in class '" + classId + "'.");
                }
            }
            double baseHealth = loaded.getDouble("classes." + classId + ".base-health", 24.0);
            double healthGrowth = loaded.getDouble("classes." + classId + ".health-per-level", 4.0);
            double baseDamage = loaded.getDouble("classes." + classId + ".base-damage", 4.0);
            double damageGrowth = loaded.getDouble("classes." + classId + ".damage-per-level", 0.5);
            if (!Double.isFinite(baseHealth) || baseHealth <= 0.0
                    || !Double.isFinite(healthGrowth) || healthGrowth < 0.0
                    || !Double.isFinite(baseDamage) || baseDamage < 0.0
                    || !Double.isFinite(damageGrowth) || damageGrowth < 0.0
                    || baseHealth + healthGrowth * 9.0 > 1024.0
                    || baseDamage + damageGrowth * 9.0 > 1024.0) {
                throw new InvalidConfigurationException("Invalid stats for class '" + classId + "'.");
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
        this.maxUnits = readBoundedInt(loaded, "max-units-per-player", 4, 1, 100);
        this.followDistance = readBoundedDouble(loaded, "follow-distance-blocks", 8.0, 2.0, 64.0);
        this.teleportDistance = readBoundedDouble(loaded, "teleport-distance-blocks", 40.0, 8.0, 256.0);
        this.targetRadius = readBoundedDouble(loaded, "target-radius-blocks", 20.0, 4.0, 64.0);
        this.aiIntervalTicks = readBoundedInt(loaded, "ai-interval-ticks", 20, 10, 100);
        this.skillCooldownTicks = readBoundedInt(loaded, "skills.cooldown-seconds", 15, 1, 3600) * 20;
        this.healAmount = readBoundedDouble(loaded, "skills.heal-amount", 4.0, 0.0, 100.0);
        this.healRadius = readBoundedDouble(loaded, "skills.heal-radius-blocks", 8.0, 1.0, 32.0);
        this.ownerBuff = resolvedBuff;
        this.ownerBuffTicks = readBoundedInt(loaded, "skills.owner-buff-seconds", 8, 1, 600) * 20;
        plugin.getLogger().info("[Army] Loaded " + getClassIds().size() + " class(es), enabled=" + enabled + ".");
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
                    int level = loaded.getInt("units." + idText + ".level", 1);
                    if (classId == null || !hasTier(level)) {
                        plugin.getLogger().warning("[Army] Skipping invalid mercenary record " + idText + ".");
                        continue;
                    }
                    Unit unit = new Unit(id, owner, classId, level,
                        parseMode(loaded.getString("units." + idText + ".mode", "PROTECT")),
                        loaded.getBoolean("units." + idText + ".deployed", false));
                    unit.entityId = parseUuid(loaded.getString("units." + idText + ".entity-uuid", ""));
                    unit.worldName = loaded.getString("units." + idText + ".world", "");
                    unit.x = loaded.getDouble("units." + idText + ".x");
                    unit.y = loaded.getDouble("units." + idText + ".y");
                    unit.z = loaded.getDouble("units." + idText + ".z");
                    for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
                        String path = "units." + idText + ".equipment." + slotPath(slot);
                        ItemStack item = loaded.getItemStack(path);
                        if (item != null && !item.getType().isAir()) unit.equipment.put(slot, item.clone());
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
                    if (owner == null || !owner.isOnline()) {
                        if (mob != null) {
                            captureEquipment(unit, mob);
                            updateLocation(unit, mob.getLocation());
                            mob.remove();
                            activeEntities.remove(unit.id);
                            unit.entityId = null;
                        }
                        continue;
                    }
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
                    updateLocation(unit, mob.getLocation());
                    runUnitAi(unit, mob, owner);
                }
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
        if (!owner.getWorld().equals(mob.getWorld())) {
            Location spawn = safeLocation(owner.getLocation());
            mob.teleport(spawn);
            return;
        }
        double distanceSquared = owner.getLocation().distanceSquared(mob.getLocation());
        if (distanceSquared > teleportDistance * teleportDistance) {
            mob.teleport(safeLocation(owner.getLocation()));
            mob.setTarget(null);
            return;
        }
        if (distanceSquared > followDistance * followDistance && mob.getTarget() == null) {
            mob.getPathfinder().moveTo(owner.getLocation(), 1.15);
        }

        LivingEntity target = findTarget(unit, mob, owner);
        if (target != null) {
            mob.setAware(true);
            if (mob.getTarget() != target) mob.setTarget(target);
        } else {
            mob.setTarget(null);
        }
        useSkills(unit, owner);
    }

    private LivingEntity findTarget(Unit unit, Mob mob, Player owner) {
        if (!hasSkill(unit, "ATTACK")) return null;
        double radius = targetRadius;
        Location center = unit.mode == Mode.ATTACK ? mob.getLocation() : owner.getLocation();
        if (unit.mode == Mode.PROTECT) {
            LivingEntity attacker = mob.getWorld().getNearbyEntities(owner.getLocation(), radius, radius, radius)
                .stream()
                .filter(entity -> entity instanceof Monster)
                .map(entity -> (Monster) entity)
                .filter(monster -> monster.getTarget() != null &&
                    monster.getTarget().getUniqueId().equals(owner.getUniqueId()))
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(owner.getLocation())))
                .orElse(null);
            if (attacker != null) return attacker;
            center = owner.getLocation();
        }
        Location searchCenter = center;
        return mob.getWorld().getNearbyEntities(searchCenter, radius, radius, radius).stream()
            .filter(entity -> entity instanceof Monster)
            .map(entity -> (Monster) entity)
            .filter(monster -> monster.isValid() && !monster.isDead() && !isMercenary(monster))
            .filter(monster -> !monster.isGlowing())
            .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(searchCenter)))
            .orElse(null);
    }

    private void useSkills(Unit unit, Player owner) {
        double ownerMaxHealth = maxHealth(owner);
        if (hasSkill(unit, "HEAL_OWNER") && ownerMaxHealth > 0 && owner.getHealth() < ownerMaxHealth) {
            if (ready(unit, "heal-owner")) {
                owner.setHealth(Math.min(ownerMaxHealth, owner.getHealth() + healAmount));
                markUsed(unit, "heal-owner");
            }
        }
        if (hasSkill(unit, "HEAL_ALLIES") && ready(unit, "heal-allies")) {
            List<LivingEntity> allies = new ArrayList<>();
            allies.add(owner);
            for (UUID siblingId : getOwnerUnits(unit.owner).stream()
                    .map(sibling -> sibling.id).toList()) {
                Mob sibling = activeEntities.get(siblingId);
                if (sibling != null && sibling.isValid()) allies.add(sibling);
            }
            boolean healed = false;
            for (LivingEntity ally : allies) {
                double maxHealth = maxHealth(ally);
                if (ally.isDead() || !ally.getWorld().equals(owner.getWorld())
                        || ally.getLocation().distanceSquared(owner.getLocation()) > healRadius * healRadius
                        || maxHealth <= 0 || ally.getHealth() >= maxHealth) continue;
                ally.setHealth(Math.min(maxHealth, ally.getHealth() + healAmount));
                healed = true;
            }
            if (healed) markUsed(unit, "heal-allies");
        }
        if (hasSkill(unit, "BUFF_OWNER") && ready(unit, "buff-owner")) {
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(ownerBuff.toLowerCase(Locale.ROOT)));
            if (type == null) {
                plugin.getLogger().warning("[Army] Unknown owner-buff effect '" + ownerBuff + "' in armies.yml.");
                markUsed(unit, "buff-owner");
            } else if (!owner.hasPotionEffect(type)) {
                owner.addPotionEffect(new PotionEffect(type, ownerBuffTicks, 0, true, true, true));
                markUsed(unit, "buff-owner");
            }
        }
    }

    private boolean ready(Unit unit, String skill) {
        return skillCooldowns.computeIfAbsent(unit.id, ignored -> new HashMap<>())
            .getOrDefault(skill, 0L) <= System.currentTimeMillis();
    }

    private void markUsed(Unit unit, String skill) {
        skillCooldowns.computeIfAbsent(unit.id, ignored -> new HashMap<>())
            .put(skill, System.currentTimeMillis() + skillCooldownTicks * 50L);
    }

    private boolean hasSkill(Unit unit, String skill) {
        List<String> skills = settings.getStringList("classes." + unit.unitClass + ".skills");
        return skills.stream().anyMatch(value -> value.equalsIgnoreCase(skill));
    }

    private void hire(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage("§eSử dụng: /vnarmy hire <loại> <cấp>");
            return;
        }
        String classId = normalizeClass(args[1]);
        int level = parsePositiveInt(args[2]);
        if (classId == null || !hasTier(level)) {
            player.sendMessage("§cLoại lính hoặc cấp không hợp lệ. Dùng /vnarmy để xem danh sách.");
            return;
        }
        List<Unit> owned = getOwnerUnits(player.getUniqueId());
        if (owned.size() >= maxUnits) {
            player.sendMessage("§cĐã đạt giới hạn sở hữu " + maxUnits + " lính đánh thuê.");
            return;
        }
        ItemStack token = findToken(player, classId, level);
        if (token == null) {
            player.sendMessage("§cCần có Lệnh Triệu Hồi " + classDisplay(classId) + " cấp " + level + ".");
            return;
        }
        int xpCost = settings.getInt("tiers." + level + ".experience-levels", -1);
        ConfigurationSection materialCosts = settings.getConfigurationSection("tiers." + level + ".materials");
        if (xpCost < 0 || materialCosts == null) {
            player.sendMessage("§cCấu hình chi phí cấp " + level + " không hợp lệ; chưa trừ tài nguyên.");
            plugin.getLogger().warning("[Army] Invalid cost configuration for tier " + level + ".");
            return;
        }
        Map<Material, Integer> costs = new EnumMap<>(Material.class);
        for (String materialName : materialCosts.getKeys(false)) {
            Material material = Material.matchMaterial(materialName);
            int amount = materialCosts.getInt(materialName);
            if (material == null || material.isAir() || amount < 0) {
                player.sendMessage("§cCấu hình vật phẩm chi phí không hợp lệ: " + materialName + ".");
                plugin.getLogger().warning("[Army] Invalid material cost '" + materialName + "' at tier " + level + ".");
                return;
            }
            if (amount > 0) costs.put(material, amount);
        }
        if (player.getLevel() < xpCost || !hasMaterials(player, costs)) {
            player.sendMessage("§cThiếu cấp kinh nghiệm hoặc vật phẩm để thuê lính cấp " + level + ".");
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
        Unit unit = new Unit(UUID.randomUUID(), player.getUniqueId(), classId, level, Mode.PROTECT, true);
        Location spawnAt = safeLocation(player.getLocation());
        Mob mob = spawnUnit(unit, spawnAt);
        if (mob == null) {
            player.getInventory().setContents(inventoryBefore);
            player.setLevel(levelsBefore);
            player.sendMessage("§cKhông thể tạo lính; toàn bộ chi phí đã được hoàn lại.");
            return;
        }
        units.put(unit.id, unit);
        activeEntities.put(unit.id, mob);
        updateLocation(unit, mob.getLocation());
        if (!saveData()) {
            units.remove(unit.id);
            removeUnitData(unit);
            activeEntities.remove(unit.id);
            mob.remove();
            player.getInventory().setContents(inventoryBefore);
            player.setLevel(levelsBefore);
            saveData();
            player.sendMessage("§cKhông thể lưu lính vào mercenaries.yml; chi phí đã được hoàn lại.");
            return;
        }
        player.sendMessage("§aĐã thuê §e" + displayName(unit) + " §acấp " + level
            + " (§7ID " + shortId(unit.id) + "§a). Dùng /vnarmy gear " + shortId(unit.id) + " để trang bị.");
    }

    private void recall(Player player) {
        List<Unit> owned = getOwnerUnits(player.getUniqueId());
        if (owned.isEmpty()) {
            player.sendMessage("§7Bạn chưa sở hữu lính đánh thuê nào.");
            return;
        }
        int restored = 0;
        for (Unit unit : owned) {
            unit.deployed = true;
            Mob mob = findExistingEntity(unit);
            if (mob == null) mob = spawnUnit(unit, safeLocation(player.getLocation()));
            if (mob == null) {
                unit.deployed = false;
                player.sendMessage("§cKhông thể triệu hồi lính §e#" + shortId(unit.id) + "§c.");
                continue;
            }
            activeEntities.put(unit.id, mob);
            updateLocation(unit, mob.getLocation());
            restored++;
        }
        if (!saveData()) {
            player.sendMessage("§cLính đã được gọi ra nhưng không thể lưu trạng thái; kiểm tra log máy chủ.");
            plugin.getLogger().severe("[Army] Could not save recall state for owner " + player.getUniqueId() + ".");
            return;
        }
        player.sendMessage("§aĐã triệu hồi " + restored + "/" + owned.size() + " lính đánh thuê.");
    }

    private void restoreDeployedUnits(Player player) {
        if (!enabled || !player.isOnline()) return;
        List<Unit> owned = getOwnerUnits(player.getUniqueId());
        boolean changed = false;
        for (Unit unit : owned) {
            if (!unit.deployed) continue;
            Mob mob = findExistingEntity(unit);
            if (mob == null) mob = spawnUnit(unit, safeLocation(player.getLocation()));
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

    private void rest(Player player) {
        List<Unit> owned = getOwnerUnits(player.getUniqueId());
        int rested = 0;
        for (Unit unit : owned) {
            Mob mob = activeEntities.get(unit.id);
            if (mob == null || !mob.isValid()) mob = findExistingEntity(unit);
            if (mob != null && mob.isValid()) {
                activeEntities.put(unit.id, mob);
                captureEquipment(unit, mob);
                updateLocation(unit, mob.getLocation());
                unit.entityId = null;
                unit.deployed = false;
                if (!saveData()) {
                    unit.deployed = true;
                    unit.entityId = mob.getUniqueId();
                    applyEquipment(unit, mob);
                    player.sendMessage("§cKhông thể lưu trạng thái nghỉ của lính §e#" + shortId(unit.id)
                        + "§c; lính vẫn được giữ nguyên.");
                    continue;
                }
                mob.remove();
                activeEntities.remove(unit.id);
                rested++;
            } else if (unit.deployed) {
                unit.deployed = false;
                unit.entityId = null;
                if (saveData()) rested++;
                else player.sendMessage("§cKhông thể lưu trạng thái nghỉ của lính §e#" + shortId(unit.id) + "§c.");
            }
        }
        player.sendMessage(rested == 0 ? "§7Không có lính nào đang triển khai."
            : "§aĐã cho " + rested + " lính nghỉ. Trang bị được lưu.");
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
            mob.remove();
            activeEntities.remove(unit.id);
            changed = true;
        }
        if (changed && !saveData()) {
            plugin.getLogger().severe("[Army] Could not persist mercenary state on player logout for " + ownerId + ".");
        }
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
        player.sendMessage("§6§lLính đánh thuê §7(" + owned.size() + "/" + maxUnits + ")");
        if (owned.isEmpty()) {
            player.sendMessage("§7Thuê lính bằng /vnarmy hire <loại> <cấp>.");
            return;
        }
        for (Unit unit : owned) {
            player.sendMessage("§e#" + shortId(unit.id) + " §f" + displayName(unit)
                + " §7| cấp " + unit.level + " | " + modeName(unit.mode)
                + (unit.deployed ? " | §ađang triển khai" : " | §7đang nghỉ"));
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
        GearHolder holder = new GearHolder(player.getUniqueId(), unit.id);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Trang bị lính #" + shortId(unit.id));
        holder.inventory = inventory;
        inventory.setItem(10, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.HAND)));
        inventory.setItem(12, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.HEAD)));
        inventory.setItem(14, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.CHEST)));
        inventory.setItem(16, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.LEGS)));
        inventory.setItem(19, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.FEET)));
        inventory.setItem(22, cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.OFF_HAND)));
        player.openInventory(inventory);
        player.sendMessage("§7Ô trang bị: tay chính (10), mũ (12), áo (14), quần (16), giày (19), tay phụ (22).");
    }

    private boolean handleTokenCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§cBạn không có quyền cấp lệnh triệu hồi.");
            return true;
        }
        if (args.length != 5 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§eSử dụng: /vnarmy token give <người chơi> <loại> <cấp>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        String classId = normalizeClass(args[3]);
        int level = parsePositiveInt(args[4]);
        if (target == null || classId == null || !hasTier(level)) {
            sender.sendMessage("§cNgười chơi phải đang online; loại/cấp lính không hợp lệ.");
            return true;
        }
        ItemStack token = createToken(classId, level);
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(token);
        overflow.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
        sender.sendMessage("§aĐã cấp lệnh triệu hồi " + classDisplay(classId) + " cấp " + level
            + " cho §e" + target.getName() + "§a.");
        target.sendMessage("§aBạn nhận được Lệnh Triệu Hồi " + classDisplay(classId) + " cấp " + level + "§a.");
        return true;
    }

    private ItemStack createToken(String classId, int level) {
        ItemStack token = new ItemStack(Material.PAPER);
        ItemMeta meta = token.getItemMeta();
        meta.setDisplayName("§6§lLệnh Triệu Hồi " + classDisplay(classId) + " §fCấp " + level);
        meta.setLore(List.of("§7Thuê một lính đánh thuê.", "§8Lệnh được tiêu thụ khi dùng.",
            "§8Lớp: " + classId, "§8Cấp: " + level));
        meta.getPersistentDataContainer().set(tokenClassKey, PersistentDataType.STRING, classId);
        meta.getPersistentDataContainer().set(tokenLevelKey, PersistentDataType.INTEGER, level);
        token.setItemMeta(meta);
        return token;
    }

    private ItemStack findToken(Player player, String classId, int level) {
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType() != Material.PAPER || !stack.hasItemMeta()) continue;
            PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
            if (classId.equals(pdc.get(tokenClassKey, PersistentDataType.STRING))
                    && Integer.valueOf(level).equals(pdc.get(tokenLevelKey, PersistentDataType.INTEGER))) {
                return stack;
            }
        }
        return null;
    }

    private Mob spawnUnit(Unit unit, Location location) {
        EntityType type = EntityType.fromName(settings.getString("classes." + unit.unitClass + ".entity-type", ""));
        if (type == null || !type.isAlive()) return null;
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
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        pdc.set(unitKey, PersistentDataType.STRING, unit.id.toString());
        pdc.set(ownerKey, PersistentDataType.STRING, unit.owner.toString());
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.setCanPickupItems(false);
        mob.setAware(true);
        mob.setTarget(null);
        String configuredName = settings.getString("classes." + unit.unitClass + ".display-name", unit.unitClass);
        mob.setCustomName("§6" + configuredName + " §fLv." + unit.level);
        mob.setCustomNameVisible(true);
        applyStats(unit, mob);
        applyEquipment(unit, mob);
        unit.entityId = mob.getUniqueId();
        updateLocation(unit, mob.getLocation());
        return mob;
    }

    private void applyStats(Unit unit, Mob mob) {
        double baseHealth = settings.getDouble("classes." + unit.unitClass + ".base-health", 24.0);
        double healthPerLevel = settings.getDouble("classes." + unit.unitClass + ".health-per-level", 4.0);
        double maxHealth = Math.max(1.0, baseHealth + Math.max(0, unit.level - 1) * healthPerLevel);
        if (mob.getAttribute(Attribute.MAX_HEALTH) != null) {
            mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            mob.setHealth(maxHealth);
        }
        if (mob.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            double baseDamage = settings.getDouble("classes." + unit.unitClass + ".base-damage", 4.0);
            double damagePerLevel = settings.getDouble("classes." + unit.unitClass + ".damage-per-level", 0.5);
            mob.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(
                Math.max(0.0, baseDamage + Math.max(0, unit.level - 1) * damagePerLevel));
        }
    }

    private void applyEquipment(Unit unit, Mob mob) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) return;
        equipment.setItemInMainHand(cloneOrNull(unit.equipment.get(org.bukkit.inventory.EquipmentSlot.HAND)));
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

    private void captureEquipment(Unit unit, Mob mob) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) return;
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.HAND, cloneOrNull(equipment.getItemInMainHand()));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.HEAD, cloneOrNull(equipment.getHelmet()));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.CHEST, cloneOrNull(equipment.getChestplate()));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.LEGS, cloneOrNull(equipment.getLeggings()));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.FEET, cloneOrNull(equipment.getBoots()));
        unit.equipment.put(org.bukkit.inventory.EquipmentSlot.OFF_HAND, cloneOrNull(equipment.getItemInOffHand()));
    }

    private Mob findExistingEntity(Unit unit) {
        if (unit.entityId != null) {
            Entity entity = Bukkit.getEntity(unit.entityId);
            if (entity instanceof Mob mob && isUnit(mob, unit.id)) {
                activeEntities.put(unit.id, mob);
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
            captureEquipment(unit, found);
            return found;
        }
        return null;
    }

    private boolean isUnit(Mob mob, UUID unitId) {
        return unitId.toString().equals(
            mob.getPersistentDataContainer().get(unitKey, PersistentDataType.STRING));
    }

    private boolean isMercenary(Entity entity) {
        return entity.getPersistentDataContainer().has(unitKey, PersistentDataType.STRING);
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
        data.set(path + ".level", unit.level);
        data.set(path + ".mode", unit.mode.name());
        data.set(path + ".deployed", unit.deployed);
        data.set(path + ".entity-uuid", unit.entityId == null ? null : unit.entityId.toString());
        data.set(path + ".world", unit.worldName);
        data.set(path + ".x", unit.x);
        data.set(path + ".y", unit.y);
        data.set(path + ".z", unit.z);
        for (org.bukkit.inventory.EquipmentSlot slot : List.of(
                org.bukkit.inventory.EquipmentSlot.HAND,
                org.bukkit.inventory.EquipmentSlot.HEAD,
                org.bukkit.inventory.EquipmentSlot.CHEST,
                org.bukkit.inventory.EquipmentSlot.LEGS,
                org.bukkit.inventory.EquipmentSlot.FEET,
                org.bukkit.inventory.EquipmentSlot.OFF_HAND)) {
            data.set(path + ".equipment." + slotPath(slot), cloneOrNull(unit.equipment.get(slot)));
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

    private String classDisplay(String classId) {
        return settings.getString("classes." + classId + ".display-name", classId);
    }

    private String displayName(Unit unit) {
        return classDisplay(unit.unitClass);
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
        sender.sendMessage("§e/vnarmy list §7- Xem quân số, cấp, chế độ và trạng thái.");
        sender.sendMessage("§e/vnarmy hire <loại> <cấp> §7- Thuê bằng XP, vật phẩm và lệnh triệu hồi.");
        sender.sendMessage("§e/vnarmy recall | rest §7- Triệu hồi hoặc cho lính nghỉ.");
        sender.sendMessage("§e/vnarmy attack | defend | protect §7- Đổi xu hướng AI của cả đội.");
        sender.sendMessage("§e/vnarmy gear <ID> §7- Mở kho trang bị cho một lính.");
        sender.sendMessage("§7Loại lính: " + String.join(", ", getClassIds()));
        sender.sendMessage("§7Cấp khả dụng: " + String.join(", ", getTierIds()));
    }

    public void stop() {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
        for (Unit unit : units.values()) {
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
    }
}
