package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class ConfigManager {

    private static final Random RANDOM = ThreadLocalRandom.current();

    // Utility: parse range string like "500-1200" and return random value in that range
    public static int parseRangeOrInt(String value, int defaultValue) {
        if (value == null) return defaultValue;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return defaultValue;
        if (trimmed.contains("-")) {
            String[] parts = trimmed.split("-");
            try {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                if (min > max) { int temp = min; min = max; max = temp; }
                return min + RANDOM.nextInt(max - min + 1);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Utility: get raw string from config (supports both string and int)
    public static String getStringFromConfig(ConfigurationSection section, String path, String defaultValue) {
        if (section == null) return defaultValue;
        if (section.isString(path)) return section.getString(path, defaultValue);
        if (section.isInt(path)) return String.valueOf(section.getInt(path));
        return defaultValue;
    }

    private final VnMineHardcore plugin;
    private FileConfiguration config;

    // Death
    public boolean broadcastDeath;
    public boolean playSound;
    public boolean respawnProtectionEnabled;
    public int respawnProtectionSeconds;
    public int respawnMovementRadiusBlocks;
    public int respawnDamageImmunitySeconds;

    // Death Penalty
    public boolean deathPenaltyEnabled;
    public int deathPenaltyRecoverySeconds;
    public int deathPenaltyMaxStack;
    public double deathPenaltyMaxHpPerDeath;
    public double deathPenaltyMaxThirstPerDeath;
    public double deathPenaltyDamagePerDeath;
    public double deathPenaltyIncomingDamagePerDeath;
    public double deathPenaltyRegenPerDeath;
    public double deathPenaltyFoodDrainPerDeath;
    public double deathPenaltyThirstDrainPerDeath;

    // Combat
    public double mobDamageMultiplier;
    public double fallDamageMultiplier;
    public double fireDamageMultiplier;
    public double drowningDamageMultiplier;
    public double explosionDamageMultiplier;
    public double pvpDamageMultiplier;
    public double mobExtraHpMultiplier;
    public boolean disableNaturalRegen;
    public double regenMultiplier;
    public long combatTagDurationMs;
    public double enderPearlDamage;

    // Hunger
    public boolean hungerEnabled;
    public int drainIntervalSeconds;
    public double foodRestoreMultiplier;
    public double starvationDamage;
    public int sprintMinFood;
    public boolean rawFoodPoison;
    public boolean chokeEnabled;
    public int chokeMaxEats;
    public int chokeWindowSeconds;
    public double chokeDamage;
    public boolean notchAppleExplode;
    public double notchAppleDamage;

    // Thirst
    public boolean thirstEnabled;
    public int thirstDrainIntervalSeconds;
    public double thirstDamage;
    public int maxThirst;
    public boolean drinkFromSource;
    public int drinkSourceRestore;
    public int drinkSourceCooldownSeconds;
    public boolean bucketDrinkEnabled;
    public int bucketRestore;
    public boolean bottleDrinkEnabled;
    public int bottleRestore;
    public boolean naturalWaterEnabled;
    public double naturalWaterDamagePerSecond;
    public int naturalWaterDurationSeconds;
    public int naturalWaterNauseaAmplifier;
    public boolean bottleFill;

    // Environment
    public boolean temperatureEnabled;
    public int tempCheckIntervalSeconds;
    public double heatDamage;
    public int heatAmplifier;
    public double helmetReducePercent;
    public double helmetDamageReducePercent;
    public double coldDamage;
    public int coldAmplifier;
    public double armorReducePercent;
    public double armorDamageReducePercent;
    public int heatExposureDelaySeconds;
    public int coldExposureDelaySeconds;
    public double waterDamage;
    public double acidRainDamage;
    public long waterDamageIntervalMs;
    public long acidRainIntervalMs;
    public boolean rainControlEnabled;
    public int rainChancePercent;
    public int maxRainDurationMinutes;
    public boolean rainRandomDurationEnabled;
    public int minRainDurationMinutes;
    public boolean fogEnabled;
    public int fogIntervalSeconds;
    public int fogEffectDurationSeconds;
    public int fogEffectAmplifier;
    public boolean sleepBlock;
    public double sleepDamage;
    public boolean cropSlowEnabled;
    public double cropSlowMultiplier;
    public boolean toolWearEnabled;
    public double toolWearMultiplier;
    public boolean flightBlock;
    public boolean claustrophobiaEnabled;
    public int claustrophobiaYLevel;
    public int claustrophobiaChance;
    public int claustrophobiaAmplifier;
    public double claustrophobiaTorchReducePercent;
    public boolean vertigoEnabled;
    public int vertigoYLevel;
    public int vertigoChance;
    public int vertigoAmplifier;
    public double vertigoElytraReducePercent;

    // Villager Trading
    public boolean villagerTradingEnabled;
    public boolean disableRandomVillager;
    public int villagerRegionSize;

    // Spawner Control
    public boolean spawnerControlEnabled;
    public double spawnerSpawnRateReduction;
    public double spawnerHpMultiplier;
    public double spawnerDamageMultiplier;

    // Disasters - General settings (loaded from disasters/_settings.yml)
    public boolean disastersEnabled;
    public String disasterMinIntervalRaw;
    public int disasterWarningSeconds;
    public int disasterDefaultSuccessProbability;
    public List<DisasterSuccessThreshold> disasterSuccessThresholds = new ArrayList<>();

    public static class DisasterSuccessThreshold {
        public final int minDay;
        public final int probabilityPercent;

        public DisasterSuccessThreshold(int minDay, int probabilityPercent) {
            this.minDay = minDay;
            this.probabilityPercent = probabilityPercent;
        }
    }

    // Safe Zone settings (loaded from disasters/_settings.yml)
    public boolean safeZoneEnabled;
    public int safeZoneCheckRadius;
    public int safeZoneRoofHeight;
    public boolean safeZoneCheckWalls;
    public int safeZoneMinWalls;

    // Disaster config map - each disaster is a complete object
    public Map<String, DisasterConfig> disasterConfigs = new HashMap<>();

    // Disaster messages (shared across all disasters, loaded from disasters/_settings.yml)
    public Map<String, String> disasterMessages = new HashMap<>();

    // Boss Events - General settings (loaded from bosses/_settings.yml)
    public boolean bossEventsEnabled;
    public boolean bossSkillCastLogsEnabled;
    public boolean bossSkillCastOpBroadcastEnabled;
    public boolean bossSkillCastPlayerBroadcastEnabled;
    public double bossRewardDamageThresholdPercent;
    public boolean bossArmyTokenDropEnabled = true;
    public double bossArmyTokenDropChance = 0.30;
    public double bossExperiencePerLevelMultiplier = 1.0;
    public String bossEventMinIntervalRaw;
    public int bossEventSpawnRadius;
    public boolean bossLevelProgressionEnabled;
    public List<BossLevelThreshold> bossLevelThresholds = new ArrayList<>();
    public boolean bossSpawnSuccessProgressionEnabled;
    public double bossSpawnSuccessDefaultPercent = 100.0;
    public List<BossSpawnSuccessThreshold> bossSpawnSuccessThresholds = new ArrayList<>();
    public double bossArmorPieceDropChance = 0.10;
    public int bossArmorNaturalLevelGap = 2;
    public double bossArmorLevelsPerEnchant = 0.8;
    public int bossArmorMaximumEnchantLevel = 5;
    public boolean bossArmorAllowUnsafeEnchants = true;

    // Boss config map - each boss is a complete object (loaded from bosses/*.yml)
    public Map<String, BossConfig> bossConfigs = new HashMap<>();
    private final Map<String, List<String>> bossItemStories = new HashMap<>();
    private final Map<String, BossArmorPieceStory> bossArmorPieceStories = new HashMap<>();

    // Ore Control
    public boolean oreControlEnabled;
    public Map<String, Map<String, Double>> oreControlWorlds = new HashMap<>();

    // Time Control
    public boolean timeControlEnabled;
    public int dayDurationMinutes;
    public int nightDurationMinutes;
    public Map<String, WorldTimeConfig> worldTimeConfigs = new HashMap<>();

    public static class WorldTimeConfig {
        public Integer dayDurationMinutes;
        public Integer nightDurationMinutes;
    }

    // Block drop, tool break, and explosion rules
    public boolean blockDropSettingsEnabled;
    public boolean dropReplaceEnabled;
    public boolean toolBreakEnabled;
    public boolean oreExplodeEnabled;
    public Map<Material, Map<Material, BlockMiningRule>> blockMiningRules = new HashMap<>();

    // Rename
    public boolean renameEnabled;
    public String nameStructure;

    // Logging
    public boolean consoleDebug;
    public boolean logDeaths;
    public boolean logDisasters;

    // DisasterConfig class - holds all config for a single disaster
    public static class DisasterConfig {
        public boolean enabled = true;
        public int chance = 5;
        public String name = "Unknown Disaster";
        public List<String> worlds = new ArrayList<>();
        public String chatDescription = "";
        public int effectIntervalSeconds = 5;
        public int effectDurationSeconds = 5;
        public int durationSeconds = 600;
        public String startTitle = "";
        public String startSubtitle = "";
        public int blockFallChance = 15;
        public int radius = 15;
        public int minY = 30;
        public double blastResistanceFactor = 0.1;
        public double damage = 2.0;
        public int fireTicks = 100;
        public int witherAmplifier = 1;
        public int shulkerChance = 20;
        
        // Action engine - parsed from YAML actions: list
        public List<DisasterAction> actions = new ArrayList<>();
        public List<DisasterAction> onEnd = new ArrayList<>();
    }

    // BossConfig class - holds all config for a single boss
    public static class BossConfig {
        public boolean enabled = true;
        public String entityType = "WITHER";
        public String displayName = "§c§lBoss";
        public String armyTokenClass = "guardian";
        public double hp = 100.0;
        public double damageMultiplier = 1.0;
        public int chance = 10;
        public int experience = 100;
        public int durationSeconds = 1200;
        public int warningSeconds = 60;
        public int activityRadius = 100;
        public Map<String, Boolean> immunities = new HashMap<>();
        public java.util.List<String> immunityPotionEffects = new java.util.ArrayList<>();
        public Map<String, DropConfig> drops = new HashMap<>();
        public Map<Integer, List<ConfiguredItem>> levelDrops = new HashMap<>();
        public Map<Integer, Double> hpByLevel = new HashMap<>();
        public Map<String, BossSkillConfig> skills = new HashMap<>();
        public Map<String, Object> traits = new HashMap<>();
        public BossArmorSetConfig armorSet;
    }

    public static class BossArmorSetConfig {
        public boolean enabled = true;
        public String name = "Di Vật";
        public String storyId;
        public double dropChance = 0.45;
        public Map<Integer, String> materialTiers = new HashMap<>();
        public Map<String, String> pieceNames = new HashMap<>();
        public Map<String, List<String>> enchantmentsBySlot = new HashMap<>();
        public Map<Integer, BossArmorBonusConfig> bonuses = new HashMap<>();
    }

    public static class BossArmorBonusConfig {
        public double damageReductionPercent;
        public List<String> damageCauses = new ArrayList<>();
        public double knockbackReductionPercent;
        public double blockingDamageReductionPercent;
        public double effectDurationReductionPercent;
        public double healOnUndeadKill;
        public int healCooldownSeconds = 10;
    }

    public static class BossArmorPieceStory {
        public String itemName;
        public List<String> lore = List.of();
    }

    public static class BossLevelThreshold {
        public long minDay;
        public int minLevel;
        public int maxLevel;
    }

    public static class BossSpawnSuccessThreshold {
        public long minDay;
        public double chancePercent;
    }

    public static class BossSkillConfig {
        public boolean enabled;
        public double damage;
        public int multiply;
        public int cooldownSeconds;
        public double radius;
        public double velocity;
        public double spread;
        public int durationSeconds;
        public double explosionPower;
        public double chargedExplosionPower;
        public int explosionCooldownSeconds;
        public Map<String, Object> options = new HashMap<>();
    }

    // DropConfig class - holds drop configuration
    public static class DropConfig {
        public int minAmount = 1;
        public int maxAmount = 1;
        public double chance = 0.5;
        public String name;
        public List<String> lore = new ArrayList<>();
        public Map<String, Integer> bookEnchantments = new HashMap<>();
    }

    public static class ConfiguredItem {
        public String material = "DIAMOND";
        public String name;
        public List<String> lore = new ArrayList<>();
        public int minAmount = 1;
        public int maxAmount = 1;
        public double chance;
        public Map<String, Integer> enchantments = new HashMap<>();
        public Map<String, Integer> bookEnchantments = new HashMap<>();
    }

    public static class BlockMiningRule {
        public boolean replaceEnabled;
        public Material replaceMaterial;
        public double replaceChance;
        public boolean toolBreakEnabled;
        public double toolBreakChance;
        public String toolBreakMessage;
        public boolean explodeEnabled;
        public float explosionPower;
        public boolean explosionBreakBlocks;
        public boolean explosionSetFire;
        public boolean explosionDamagePlayer;
        public double explosionChance;
        public String explosionMessage;
    }

    public ConfigManager(VnMineHardcore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        migrateDeathPenaltyDefaults();

        mergeDefaultResource("config.yml");
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // ===== Save default external configs (bosses/, disasters/) =====
        saveDefaultExternalConfigs();

        // Death
        broadcastDeath = config.getBoolean("death.broadcast-death", true);
        playSound = config.getBoolean("death.play-sound", true);
        respawnProtectionEnabled = config.getBoolean("death.respawn-protection.enabled", true);
        respawnProtectionSeconds = config.getInt("death.respawn-protection.delay-seconds", 10);
        respawnMovementRadiusBlocks = config.getInt("death.respawn-protection.movement-radius-blocks", 10);
        respawnDamageImmunitySeconds = config.getInt("death.respawn-protection.damage-immunity-seconds", 10);
        if (respawnProtectionSeconds < 0) {
            plugin.getLogger().warning("[Config] death.respawn-protection.delay-seconds cannot be negative; using 0.");
            respawnProtectionSeconds = 0;
        }
        if (respawnMovementRadiusBlocks < 0) {
            plugin.getLogger().warning("[Config] death.respawn-protection.movement-radius-blocks cannot be negative; using 0.");
            respawnMovementRadiusBlocks = 0;
        }
        if (respawnDamageImmunitySeconds < 0) {
            plugin.getLogger().warning("[Config] death.respawn-protection.damage-immunity-seconds cannot be negative; using 0.");
            respawnDamageImmunitySeconds = 0;
        }

        // Death Penalty
        deathPenaltyEnabled = config.getBoolean("death-penalty.enabled", true);
        deathPenaltyRecoverySeconds = config.getInt("death-penalty.recovery-seconds", 300);
        deathPenaltyMaxStack = config.getInt("death-penalty.max-penalty-stack", 5);
        deathPenaltyMaxHpPerDeath = config.getDouble("death-penalty.stats.max-hp-per-death", 0.1);
        deathPenaltyMaxThirstPerDeath = config.getDouble("death-penalty.stats.max-thirst-per-death", 0.1);
        deathPenaltyDamagePerDeath = config.getDouble("death-penalty.stats.damage-per-death", 0.1);
        deathPenaltyIncomingDamagePerDeath = config.getDouble("death-penalty.stats.incoming-damage-per-death", 0.1);
        deathPenaltyRegenPerDeath = config.getDouble("death-penalty.stats.regen-per-death", 0.1);
        deathPenaltyFoodDrainPerDeath = config.getDouble("death-penalty.stats.food-drain-per-death", 0.1);
        deathPenaltyThirstDrainPerDeath = config.getDouble("death-penalty.stats.thirst-drain-per-death", 0.1);

        // Combat
        mobDamageMultiplier = config.getDouble("combat.mob-damage-multiplier", 2.0);
        fallDamageMultiplier = config.getDouble("combat.fall-damage-multiplier", 2.0);
        fireDamageMultiplier = config.getDouble("combat.fire-damage-multiplier", 2.0);
        drowningDamageMultiplier = config.getDouble("combat.drowning-damage-multiplier", 2.0);
        explosionDamageMultiplier = config.getDouble("combat.explosion-damage-multiplier", 1.5);
        pvpDamageMultiplier = config.getDouble("combat.pvp-damage-multiplier", 1.25);
        mobExtraHpMultiplier = config.getDouble("combat.mob-extra-hp-multiplier", 1.5);
        disableNaturalRegen = config.getBoolean("combat.disable-natural-regen", false);
        regenMultiplier = config.getDouble("combat.regen-multiplier", 0.5);
        combatTagDurationMs = config.getLong("combat.combat-tag-duration-seconds", 30) * 1000L;
        enderPearlDamage = config.getDouble("combat.ender-pearl-damage", 2.0);

        // Hunger
        hungerEnabled = config.getBoolean("hunger.enabled", true);
        drainIntervalSeconds = config.getInt("hunger.drain-interval-seconds", 180);
        foodRestoreMultiplier = config.getDouble("hunger.food-restore-multiplier", 0.5);
        starvationDamage = config.getDouble("hunger.starvation-damage", 1.0);
        sprintMinFood = config.getInt("hunger.sprint-min-food", 6);
        rawFoodPoison = config.getBoolean("hunger.raw-food-poison", true);
        chokeEnabled = config.getBoolean("hunger.choke-enabled", true);
        chokeMaxEats = config.getInt("hunger.choke-max-eats", 3);
        chokeWindowSeconds = config.getInt("hunger.choke-window-seconds", 10);
        chokeDamage = config.getDouble("hunger.choke-damage", 1.0);
        notchAppleExplode = config.getBoolean("hunger.notch-apple-explode", true);
        notchAppleDamage = config.getDouble("hunger.notch-apple-damage", 2.0);

        // Thirst
        thirstEnabled = config.getBoolean("thirst.enabled", true);
        thirstDrainIntervalSeconds = config.getInt("thirst.drain-interval-seconds", 120);
        thirstDamage = config.getDouble("thirst.thirst-damage", 1.0);
        maxThirst = config.getInt("thirst.max-thirst", 20);
        drinkFromSource = config.getBoolean("thirst.drink-from-source", true);
        drinkSourceRestore = config.getInt("thirst.drink-source-restore", 4);
        drinkSourceCooldownSeconds = config.getInt("thirst.drink-source-cooldown-seconds", 3);
        bucketDrinkEnabled = config.getBoolean("thirst.bucket-drink.enabled", true);
        bucketRestore = config.getInt("thirst.bucket-drink.restore", 10);
        bottleDrinkEnabled = config.getBoolean("thirst.bottle-drink.enabled", true);
        bottleRestore = config.getInt("thirst.bottle-drink.restore", 6);
        naturalWaterEnabled = config.getBoolean("thirst.natural-water.enabled", true);
        naturalWaterDamagePerSecond = config.getDouble("thirst.natural-water.damage-per-second", 0.5);
        naturalWaterDurationSeconds = config.getInt("thirst.natural-water.duration-seconds", 5);
        naturalWaterNauseaAmplifier = config.getInt("thirst.natural-water.nausea-amplifier", 0);
        bottleFill = config.getBoolean("thirst.bottle-fill", true);

        // Environment
        temperatureEnabled = config.getBoolean("environment.temperature.enabled", true);
        tempCheckIntervalSeconds = config.getInt("environment.temperature.check-interval-seconds", 30);
        heatDamage = config.getDouble("environment.temperature.heat-damage", 1.0);
        heatAmplifier = config.getInt("environment.temperature.heat-amplifier", 0);
        helmetReducePercent = config.getDouble("environment.temperature.helmet-reduce-percent", 0.5);
        helmetDamageReducePercent = config.getDouble("environment.temperature.helmet-damage-reduce-percent", 0.5);
        coldDamage = config.getDouble("environment.temperature.cold-damage", 1.0);
        coldAmplifier = config.getInt("environment.temperature.cold-amplifier", 0);
        armorReducePercent = config.getDouble("environment.temperature.armor-reduce-percent", 0.25);
        armorDamageReducePercent = config.getDouble("environment.temperature.armor-damage-reduce-percent", 0.20);
        heatExposureDelaySeconds = config.getInt("environment.temperature.heat-exposure-delay-seconds", 10);
        coldExposureDelaySeconds = config.getInt("environment.temperature.cold-exposure-delay-seconds", 10);
        waterDamage = config.getDouble("environment.temperature.water-damage", 1.0);
        acidRainDamage = config.getDouble("environment.temperature.acid-rain-damage", 1.0);
        waterDamageIntervalMs = config.getLong("environment.temperature.water-damage-interval-ms",
            config.getLong("environment.temperature.water-damage-interval-seconds", 10) * 1000L);
        acidRainIntervalMs = config.getLong("environment.temperature.acid-rain-interval-ms",
            config.getLong("environment.temperature.acid-rain-interval-seconds", 5) * 1000L);
        rainControlEnabled = config.getBoolean("environment.rain-control.enabled", true);
        int configuredRainChance = config.getInt("environment.rain-control.chance-percent", 40);
        if (configuredRainChance < 0 || configuredRainChance > 100) {
            plugin.getLogger().warning("[Config] environment.rain-control.chance-percent must be 0-100; clamping.");
        }
        rainChancePercent = Math.max(0, Math.min(100, configuredRainChance));
        int configuredMaxRainMinutes = config.getInt("environment.rain-control.max-duration-minutes", 5);
        if (configuredMaxRainMinutes < 1 || configuredMaxRainMinutes > 120) {
            plugin.getLogger().warning("[Config] environment.rain-control.max-duration-minutes must be 1-120; clamping.");
        }
        maxRainDurationMinutes = Math.max(1, Math.min(120, configuredMaxRainMinutes));
        rainRandomDurationEnabled = config.getBoolean("environment.rain-control.random-duration.enabled", true);
        int configuredMinRainMinutes = config.getInt(
            "environment.rain-control.random-duration.minimum-minutes", 1);
        if (configuredMinRainMinutes < 1 || configuredMinRainMinutes > maxRainDurationMinutes) {
            plugin.getLogger().warning("[Config] environment.rain-control.random-duration.minimum-minutes " +
                "must be between 1 and max-duration-minutes; clamping.");
        }
        minRainDurationMinutes = Math.max(1, Math.min(maxRainDurationMinutes, configuredMinRainMinutes));
        fogEnabled = config.getBoolean("environment.fog.enabled", true);
        fogIntervalSeconds = config.getInt("environment.fog.interval-seconds", 60);
        fogEffectDurationSeconds = config.getInt("environment.fog.effect-duration-seconds", 30);
        fogEffectAmplifier = config.getInt("environment.fog.effect-amplifier", 0);
        sleepBlock = config.getBoolean("environment.sleep.block", true);
        sleepDamage = config.getDouble("environment.sleep.damage", 1.0);
        cropSlowEnabled = config.getBoolean("environment.crop-slow.enabled", true);
        cropSlowMultiplier = config.getDouble("environment.crop-slow.multiplier", 0.75);
        toolWearEnabled = config.getBoolean("environment.tool-wear.enabled", true);
        toolWearMultiplier = config.getDouble("environment.tool-wear.multiplier", 1.5);
        flightBlock = config.getBoolean("environment.flight.block", false);
        claustrophobiaEnabled = config.getBoolean("environment.claustrophobia.enabled", true);
        claustrophobiaYLevel = config.getInt("environment.claustrophobia.y-level", 30);
        claustrophobiaChance = config.getInt("environment.claustrophobia.chance", 5);
        claustrophobiaAmplifier = config.getInt("environment.claustrophobia.amplifier", 0);
        claustrophobiaTorchReducePercent = config.getDouble("environment.claustrophobia.torch-reduce-percent", 0.5);
        vertigoEnabled = config.getBoolean("environment.vertigo.enabled", true);
        vertigoYLevel = config.getInt("environment.vertigo.y-level", 100);
        vertigoChance = config.getInt("environment.vertigo.chance", 10);
        vertigoAmplifier = config.getInt("environment.vertigo.amplifier", 0);
        vertigoElytraReducePercent = config.getDouble("environment.vertigo.elytra-reduce-percent", 0.5);

        // Villager Trading
        villagerTradingEnabled = config.getBoolean("villager-trading.enabled", true);
        disableRandomVillager = config.getBoolean("villager-trading.disable-random-villager", true);
        villagerRegionSize = config.getInt("villager-trading.region-size", 500);

        // Spawner Control
        spawnerControlEnabled = config.getBoolean("spawner-control.enabled", true);
        spawnerSpawnRateReduction = config.getDouble("spawner-control.spawn-rate-reduction", 0.3);
        spawnerHpMultiplier = config.getDouble("spawner-control.hp-multiplier", 3.0);
        spawnerDamageMultiplier = config.getDouble("spawner-control.damage-multiplier", 2.0);

        // ===== LOAD FROM EXTERNAL FILES =====
        loadDisasterSettings();
        loadDisasterConfigs();
        loadDisasterMessages();
        loadBossSettings();
        loadBossItemStories();
        loadBossConfigs();

        // Ore Control
        oreControlEnabled = config.getBoolean("ore-control.enabled", false);
        oreControlWorlds.clear();
        ConfigurationSection oreSection = config.getConfigurationSection("ore-control.worlds");
        if (oreSection != null) {
            for (String worldName : oreSection.getKeys(false)) {
                ConfigurationSection worldSection = oreSection.getConfigurationSection(worldName);
                Map<String, Double> oreRates = new HashMap<>();
                if (worldSection != null) {
                    for (String oreKey : worldSection.getKeys(false)) {
                        oreRates.put(oreKey, worldSection.getDouble(oreKey, 0.0));
                    }
                }
                oreControlWorlds.put(worldName, oreRates);
            }
        }

        // Time Control
        timeControlEnabled = config.getBoolean("time-control.enabled", true);
        String globalTimePath = config.isConfigurationSection("time-control.global")
            ? "time-control.global."
            : "time-control.";
        dayDurationMinutes = getPositiveMinutes(globalTimePath + "day-duration-minutes",
            config.getInt("time-control.day-duration-minutes", 10), 10);
        nightDurationMinutes = getPositiveMinutes(globalTimePath + "night-duration-minutes",
            config.getInt("time-control.night-duration-minutes", 10), 10);
        worldTimeConfigs.clear();
        ConfigurationSection worldTimeSection = config.getConfigurationSection("time-control.worlds");
        if (worldTimeSection != null) {
            for (String worldName : worldTimeSection.getKeys(false)) {
                ConfigurationSection worldSection = worldTimeSection.getConfigurationSection(worldName);
                if (worldSection == null) {
                    plugin.getLogger().warning("[Config] time-control.worlds." + worldName +
                        " must be a configuration section; using global time settings.");
                    continue;
                }

                WorldTimeConfig worldTimeConfig = new WorldTimeConfig();
                String worldPath = "time-control.worlds." + worldName + ".";
                if (worldSection.contains("day-duration-minutes")) {
                    worldTimeConfig.dayDurationMinutes = getPositiveMinutes(
                        worldPath + "day-duration-minutes",
                        worldSection.getInt("day-duration-minutes"),
                        dayDurationMinutes
                    );
                }
                if (worldSection.contains("night-duration-minutes")) {
                    worldTimeConfig.nightDurationMinutes = getPositiveMinutes(
                        worldPath + "night-duration-minutes",
                        worldSection.getInt("night-duration-minutes"),
                        nightDurationMinutes
                    );
                }
                worldTimeConfigs.put(worldName, worldTimeConfig);
            }
        }

        // Block drop, tool break, and explosion rules
        loadBlockMiningRules();

        // Rename
        renameEnabled = config.getBoolean("rename.enabled", true);
        nameStructure = config.getString("rename.name-structure", "<name> ☠ 000");

        // Logging
        consoleDebug = config.getBoolean("logging.console-debug", false);
        logDeaths = config.getBoolean("logging.log-deaths", true);
        logDisasters = config.getBoolean("logging.log-disasters", true);

        plugin.getLogger().info("[Config] Loaded configuration with " + config.getKeys(true).size() + " keys");
        plugin.getLogger().info("[Config] Loaded " + bossConfigs.size() + " bosses from bosses/");
        plugin.getLogger().info("[Config] Loaded " + disasterConfigs.size() + " disasters from disasters/");
    }

    private int getPositiveMinutes(String path, int minutes, int defaultValue) {
        if (minutes > 0) return minutes;

        plugin.getLogger().warning("[Config] " + path + " must be greater than 0; using " + defaultValue + ".");
        return defaultValue;
    }

    public int getDayDurationMinutes(String worldName) {
        WorldTimeConfig worldConfig = worldTimeConfigs.get(worldName);
        return worldConfig != null && worldConfig.dayDurationMinutes != null
            ? worldConfig.dayDurationMinutes
            : dayDurationMinutes;
    }

    public int getNightDurationMinutes(String worldName) {
        WorldTimeConfig worldConfig = worldTimeConfigs.get(worldName);
        return worldConfig != null && worldConfig.nightDurationMinutes != null
            ? worldConfig.nightDurationMinutes
            : nightDurationMinutes;
    }

    public boolean setGlobalDayDurationMinutes(int minutes) {
        if (minutes < 1) return false;
        return setGlobalTimeDuration("day-duration-minutes", minutes, true);
    }

    public boolean setGlobalNightDurationMinutes(int minutes) {
        if (minutes < 1) return false;
        return setGlobalTimeDuration("night-duration-minutes", minutes, false);
    }

    public boolean setWorldDayDurationMinutes(String worldName, int minutes) {
        if (worldName == null || worldName.isBlank() || minutes < 1) return false;
        return setWorldTimeDuration(worldName, "day-duration-minutes", minutes, true);
    }

    public boolean setWorldNightDurationMinutes(String worldName, int minutes) {
        if (worldName == null || worldName.isBlank() || minutes < 1) return false;
        return setWorldTimeDuration(worldName, "night-duration-minutes", minutes, false);
    }

    private boolean setGlobalTimeDuration(String setting, int minutes, boolean day) {
        String path = config.isConfigurationSection("time-control.global")
            ? "time-control.global." + setting
            : "time-control." + setting;
        Object previousValue = config.get(path);
        config.set(path, minutes);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException exception) {
            config.set(path, previousValue);
            plugin.getLogger().log(Level.SEVERE, "[Config] Could not save " + path + ".", exception);
            return false;
        }
        if (day) {
            dayDurationMinutes = minutes;
        } else {
            nightDurationMinutes = minutes;
        }
        return true;
    }

    private boolean setWorldTimeDuration(String worldName, String setting, int minutes, boolean day) {
        String path = "time-control.worlds." + worldName + "." + setting;
        Object previousValue = config.get(path);
        config.set(path, minutes);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException exception) {
            config.set(path, previousValue);
            plugin.getLogger().log(Level.SEVERE, "[Config] Could not save " + path + ".", exception);
            return false;
        }

        WorldTimeConfig worldConfig = worldTimeConfigs.computeIfAbsent(worldName, ignored -> new WorldTimeConfig());
        if (day) {
            worldConfig.dayDurationMinutes = minutes;
        } else {
            worldConfig.nightDurationMinutes = minutes;
        }
        return true;
    }

    private void loadBlockMiningRules() {
        blockDropSettingsEnabled = config.getBoolean("block-drop-settings.enabled", false);
        dropReplaceEnabled = config.getBoolean("block-drop-settings.replace-enabled", true);
        toolBreakEnabled = config.getBoolean("block-drop-settings.break-enabled", true);
        oreExplodeEnabled = config.getBoolean("block-drop-settings.explode-enabled", true);
        blockMiningRules.clear();

        List<Map<?, ?>> rules = config.getMapList("block-drop-settings.rules");
        for (int index = 0; index < rules.size(); index++) {
            Map<?, ?> ruleMap = rules.get(index);
            Material blockMaterial = parseMaterial(ruleMap.get("block"),
                "block-drop-settings.rules[" + index + "].block");
            if (blockMaterial == null || !blockMaterial.isBlock()) continue;

            for (Map.Entry<?, ?> entry : ruleMap.entrySet()) {
                if (!(entry.getKey() instanceof String toolName) || "block".equalsIgnoreCase(toolName)) continue;
                Material toolMaterial = parseMaterial(toolName,
                    "block-drop-settings.rules[" + index + "]." + toolName);
                if (toolMaterial == null || !(entry.getValue() instanceof Map<?, ?> toolMap)) continue;

                BlockMiningRule miningRule = new BlockMiningRule();
                Map<?, ?> replaceMap = nestedMap(toolMap.get("drop-replace"));
                if (replaceMap != null) {
                    miningRule.replaceEnabled = readBoolean(replaceMap, "enabled", false);
                    miningRule.replaceMaterial = parseMaterial(replaceMap.get("replace-block"),
                        "block-drop-settings.rules[" + index + "]." + toolName + ".drop-replace.replace-block");
                    miningRule.replaceChance = readChance(replaceMap, "chance",
                        "block-drop-settings.rules[" + index + "]." + toolName + ".drop-replace.chance");
                    if (miningRule.replaceMaterial != null &&
                        (!miningRule.replaceMaterial.isItem() || miningRule.replaceMaterial.isAir())) {
                        plugin.getLogger().warning("[Config] Replacement material " +
                            miningRule.replaceMaterial + " is not an item; drop replacement disabled.");
                        miningRule.replaceEnabled = false;
                    }
                }

                Map<?, ?> breakMap = nestedMap(toolMap.get("tool-break"));
                if (breakMap != null) {
                    miningRule.toolBreakEnabled = readBoolean(breakMap, "enabled", false);
                    miningRule.toolBreakChance = readChance(breakMap, "chance",
                        "block-drop-settings.rules[" + index + "]." + toolName + ".tool-break.chance");
                    miningRule.toolBreakMessage = readString(breakMap, "message", "");
                }

                Map<?, ?> explodeMap = nestedMap(toolMap.get("explode"));
                if (explodeMap != null) {
                    miningRule.explodeEnabled = readBoolean(explodeMap, "enabled", false);
                    miningRule.explosionPower = (float) readNonNegativeNumber(explodeMap, "power",
                        "block-drop-settings.rules[" + index + "]." + toolName + ".explode.power", 0.0);
                    miningRule.explosionBreakBlocks = readBoolean(explodeMap, "break-blocks", false);
                    miningRule.explosionSetFire = readBoolean(explodeMap, "set-fire", false);
                    miningRule.explosionDamagePlayer = readBoolean(explodeMap, "damage-player", true);
                    miningRule.explosionChance = readChance(explodeMap, "chance",
                        "block-drop-settings.rules[" + index + "]." + toolName + ".explode.chance");
                    miningRule.explosionMessage = readString(explodeMap, "message", "");
                    if (miningRule.explosionPower <= 0.0f) miningRule.explodeEnabled = false;
                }

                blockMiningRules.computeIfAbsent(blockMaterial, ignored -> new HashMap<>())
                    .put(toolMaterial, miningRule);
            }
        }
    }

    private Material parseMaterial(Object value, String path) {
        if (!(value instanceof String name) || name.isBlank()) {
            plugin.getLogger().warning("[Config] Missing or invalid material at " + path + ".");
            return null;
        }
        Material material = Material.matchMaterial(name.trim());
        if (material == null) plugin.getLogger().warning("[Config] Unknown material '" + name + "' at " + path + ".");
        return material;
    }

    private Map<?, ?> nestedMap(Object value) {
        return value instanceof Map<?, ?> map ? map : null;
    }

    private boolean readBoolean(Map<?, ?> values, String key, boolean defaultValue) {
        Object value = values.get(key);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    private String readString(Map<?, ?> values, String key, String defaultValue) {
        Object value = values.get(key);
        return value instanceof String string ? string : defaultValue;
    }

    private double readChance(Map<?, ?> values, String key, String path) {
        double chance = readNonNegativeNumber(values, key, path, 0.0);
        if (chance <= 100.0) return chance;

        plugin.getLogger().warning("[Config] " + path + " must be between 0 and 100; using 0.");
        return 0.0;
    }

    private double readNonNegativeNumber(Map<?, ?> values, String key, String path, double defaultValue) {
        Object value = values.get(key);
        if (!(value instanceof Number number)) return defaultValue;
        double result = number.doubleValue();
        if (Double.isFinite(result) && result >= 0.0) return result;

        plugin.getLogger().warning("[Config] " + path + " must be a finite non-negative number; using " +
            defaultValue + ".");
        return defaultValue;
    }

    private void migrateDeathPenaltyDefaults() {
        int version = config.getInt("config-version", 0);
        if (version >= 2) return;

        migrateDouble("death-penalty.stats.max-hp-per-death", 1.0, 0.1);
        migrateDouble("death-penalty.stats.max-thirst-per-death", 1.0, 0.1);
        migrateDouble("death-penalty.stats.damage-per-death", 0.5, 0.1);
        migrateDouble("death-penalty.stats.incoming-damage-per-death", 0.5, 0.1);
        migrateDouble("death-penalty.stats.regen-per-death", 0.5, 0.1);
        migrateDouble("death-penalty.stats.food-drain-per-death", 0.5, 0.1);
        migrateDouble("death-penalty.stats.thirst-drain-per-death", 0.5, 0.1);
        config.set("config-version", 2);
        plugin.saveConfig();
        plugin.getLogger().info("[Config] Migrated bundled death-penalty defaults to 10% per death; custom values were preserved.");
    }

    private void migrateDouble(String path, double oldDefault, double newDefault) {
        if (config.contains(path) && Double.compare(config.getDouble(path), oldDefault) == 0) {
            config.set(path, newDefault);
        }
    }

    /**
     * Save default external config files (bosses/*.yml, disasters/*.yml)
     */
    private void saveDefaultExternalConfigs() {
        // Save disasters/_settings.yml
        saveResourceIfNotExists("disasters/_settings.yml");
        // Save disaster files
        saveResourceIfNotExists("disasters/blood-moon.yml");
        saveResourceIfNotExists("disasters/meteor.yml");
        saveResourceIfNotExists("disasters/mega-storm.yml");
        saveResourceIfNotExists("disasters/solar-flare.yml");
        saveResourceIfNotExists("disasters/plague.yml");
        saveResourceIfNotExists("disasters/tornado.yml");
        saveResourceIfNotExists("disasters/eclipse.yml");
        saveResourceIfNotExists("disasters/earthquake.yml");
        saveResourceIfNotExists("disasters/inferno-storm.yml");
        saveResourceIfNotExists("disasters/soul-eruption.yml");
        saveResourceIfNotExists("disasters/lava-geyser.yml");
        saveResourceIfNotExists("disasters/end-surge.yml");
        saveResourceIfNotExists("disasters/void-storm.yml");
        saveResourceIfNotExists("disasters/chorus-explosion.yml");

        // Merge bundled samples into old disaster files without overwriting admin values.
        mergeDefaultResource("disasters/_settings.yml");
        mergeDefaultResource("disasters/blood-moon.yml");
        mergeDefaultResource("disasters/meteor.yml");
        mergeDefaultResource("disasters/mega-storm.yml");
        mergeDefaultResource("disasters/solar-flare.yml");
        mergeDefaultResource("disasters/plague.yml");
        mergeDefaultResource("disasters/tornado.yml");
        mergeDefaultResource("disasters/eclipse.yml");
        mergeDefaultResource("disasters/earthquake.yml");
        mergeDefaultResource("disasters/inferno-storm.yml");
        mergeDefaultResource("disasters/soul-eruption.yml");
        mergeDefaultResource("disasters/lava-geyser.yml");
        mergeDefaultResource("disasters/end-surge.yml");
        mergeDefaultResource("disasters/void-storm.yml");
        mergeDefaultResource("disasters/chorus-explosion.yml");

        saveResourceIfNotExists("scoreboard.yml");
        mergeDefaultResource("scoreboard.yml");
        mergeDefaultResource("story.yml");

        // Merge new boss settings into existing files without overwriting admin values.
        mergeDefaultResource("bosses/_settings.yml");
        mergeDefaultResource("bosses/wither.yml");
        mergeDefaultResource("bosses/ender_dragon.yml");
        mergeDefaultResource("bosses/ghast.yml");
        mergeDefaultResource("bosses/zombie_boss.yml");
        mergeDefaultResource("bosses/skeleton_boss.yml");
        mergeDefaultResource("bosses/spider_boss.yml");
        mergeDefaultResource("bosses/creeper_boss.yml");
        mergeDefaultResource("bosses/enderman_boss.yml");
        mergeDefaultResource("bosses/witch_boss.yml");
        mergeDefaultResource("bosses/ravager_boss.yml");
        mergeDefaultResource("bosses/vindicator_boss.yml");
        mergeDefaultResource("bosses/phantom_boss.yml");
        mergeDefaultResource("bosses/giant_boss.yml");
    }

    private void mergeDefaultResource(String resourcePath) {
        File targetFile = new File(plugin.getDataFolder(), resourcePath);
        if (!targetFile.exists()) {
            saveResourceIfNotExists(resourcePath);
            return;
        }

        try (InputStream resource = plugin.getResource(resourcePath)) {
            if (resource == null) {
                plugin.getLogger().warning("[Config] Missing bundled default resource: " + resourcePath);
                return;
            }

            YamlConfiguration target = new YamlConfiguration();
            try {
                target.load(targetFile);
            } catch (IOException | InvalidConfigurationException exception) {
                plugin.getLogger().severe("[Config] Cannot merge defaults into invalid " + resourcePath +
                    ": " + exception.getMessage());
                return;
            }

            YamlConfiguration defaults;
            try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                defaults = YamlConfiguration.loadConfiguration(reader);
            }

            if (mergeMissingKeys(target, defaults)) {
                target.save(targetFile);
                plugin.getLogger().info("[Config] Added missing defaults to " + resourcePath +
                    " (existing values were preserved)");
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("[Config] Cannot update " + resourcePath + ": " + exception.getMessage());
        }
    }

    private boolean mergeMissingKeys(ConfigurationSection target, ConfigurationSection defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            ConfigurationSection defaultChild = defaults.getConfigurationSection(key);
            if (defaultChild != null) {
                ConfigurationSection targetChild = target.getConfigurationSection(key);
                if (targetChild == null) {
                    if (target.contains(key)) {
                        plugin.getLogger().warning("[Config] Preserving non-section custom value at " + key +
                            " while merging defaults.");
                        continue;
                    }
                    targetChild = target.createSection(key);
                    changed = true;
                }
                changed |= mergeMissingKeys(targetChild, defaultChild);
            } else if (!target.contains(key)) {
                target.set(key, defaults.get(key));
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Save a resource from the JAR to the plugin's data folder if it doesn't exist already
     */
    private void saveResourceIfNotExists(String resourcePath) {
        File outFile = new File(plugin.getDataFolder(), resourcePath);
        if (!outFile.exists()) {
            // Create parent directories
            outFile.getParentFile().mkdirs();
            // Save the resource
            plugin.saveResource(resourcePath, false);
            plugin.getLogger().info("[Config] Created default: " + resourcePath);
        }
    }

    /**
     * Load disaster general settings from disasters/_settings.yml
     */
    private void loadDisasterSettings() {
        File settingsFile = new File(plugin.getDataFolder(), "disasters/_settings.yml");
        if (!settingsFile.exists()) {
            disastersEnabled = false;
            disasterMinIntervalRaw = "2400";
            disasterWarningSeconds = 120;
            disasterDefaultSuccessProbability = 100;
            disasterSuccessThresholds.clear();
            safeZoneEnabled = false;
            return;
        }

        FileConfiguration ds = YamlConfiguration.loadConfiguration(settingsFile);

        disastersEnabled = ds.getBoolean("enabled", true);
        disasterMinIntervalRaw = getStringFromConfig(ds.getConfigurationSection(""), "min-interval-seconds", "2400-4800");
        disasterWarningSeconds = ds.getInt("warning-seconds", 120);
        disasterDefaultSuccessProbability = clampPercent(
            ds.getInt("success-probability.default-percent", 100));
        disasterSuccessThresholds.clear();
        for (Map<?, ?> row : ds.getMapList("success-probability.day-thresholds")) {
            Object minDayValue = row.get("min-day");
            Object probabilityValue = row.get("percent");
            if (!(minDayValue instanceof Number minDayNumber)
                || !(probabilityValue instanceof Number probabilityNumber)) {
                continue;
            }
            int minDay = minDayNumber.intValue();
            if (minDay < 0) continue;
            disasterSuccessThresholds.add(new DisasterSuccessThreshold(
                minDay, clampPercent(probabilityNumber.intValue())));
        }
        disasterSuccessThresholds.sort((first, second) -> Integer.compare(first.minDay, second.minDay));

        // Safe Zone
        ConfigurationSection sz = ds.getConfigurationSection("safe-zone");
        if (sz != null) {
            safeZoneEnabled = sz.getBoolean("enabled", true);
            safeZoneCheckRadius = sz.getInt("check-radius", 2);
            safeZoneRoofHeight = sz.getInt("roof-height", 5);
            safeZoneCheckWalls = sz.getBoolean("check-walls", false);
            safeZoneMinWalls = sz.getInt("min-walls", 2);
        } else {
            safeZoneEnabled = false;
        }
    }

    private int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    private double readBoundedDouble(ConfigurationSection section, String path,
                                     double fallback, double minimum, double maximum) {
        double value = section.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            plugin.getLogger().warning("[Boss] " + path + " must be between " + minimum +
                " and " + maximum + "; using a clamped value.");
            return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : fallback;
        }
        return value;
    }

    /**
     * Load all disaster configurations from disasters/*.yml files
     * Now supports dynamic disaster IDs by scanning all .yml files
     */
    private void loadDisasterConfigs() {
        disasterConfigs.clear();

        File disastersDir = new File(plugin.getDataFolder(), "disasters");
        if (!disastersDir.exists() || !disastersDir.isDirectory()) return;

        File[] disasterFiles = disastersDir.listFiles((dir, name) -> name.endsWith(".yml") && !name.equals("_settings.yml"));
        if (disasterFiles == null) return;

        for (File file : disasterFiles) {
            String fileName = file.getName();
            String disasterId = fileName.substring(0, fileName.length() - 4); // Remove .yml

            FileConfiguration section = YamlConfiguration.loadConfiguration(file);
            if (section == null) continue;

            DisasterConfig dc = new DisasterConfig();
            dc.enabled = section.getBoolean("enabled", true);
            dc.chance = section.getInt("chance", 5);
            dc.name = section.getString("name", disasterId);
            dc.worlds = section.getStringList("conditions.worlds");
            dc.chatDescription = section.getString("chat-description", "");
            dc.effectIntervalSeconds = section.getInt("effect-interval-seconds", 5);
            dc.effectDurationSeconds = section.getInt("effect-duration-seconds", 5);
            dc.durationSeconds = section.getInt("duration-seconds", 600);

            // Custom title/subtitle for disaster start
            dc.startTitle = section.getString("start-title", "");
            dc.startSubtitle = section.getString("start-subtitle", "");

            // Earthquake specific
            dc.blockFallChance = section.getInt("block-fall-chance", 15);
            dc.radius = section.getInt("radius", 15);
            dc.minY = section.getInt("min-y", 30);
            dc.blastResistanceFactor = section.getDouble("blast-resistance-factor", 0.1);

            // Damage/Fire/Wither
            dc.damage = section.getDouble("damage", 2.0);
            dc.fireTicks = section.getInt("fire-ticks", 100);
            dc.witherAmplifier = section.getInt("wither-amplifier", 1);

            // End Surge specific
            dc.shulkerChance = section.getInt("shulker-chance", 20);

            // Parse actions from YAML list
            List<Map<?, ?>> actionsList = section.getMapList("actions");
            if (!actionsList.isEmpty()) {
                dc.actions = DisasterAction.parseActionList(actionsList);
            }
            List<Map<?, ?>> onEndList = section.getMapList("on-end");
            if (!onEndList.isEmpty()) {
                dc.onEnd = DisasterAction.parseActionList(onEndList);
            }

            disasterConfigs.put(disasterId, dc);
        }
    }

    /**
     * Load disaster messages from disasters/_settings.yml
     */
    private void loadDisasterMessages() {
        disasterMessages.clear();

        File settingsFile = new File(plugin.getDataFolder(), "disasters/_settings.yml");
        if (!settingsFile.exists()) return;

        FileConfiguration ds = YamlConfiguration.loadConfiguration(settingsFile);
        ConfigurationSection messagesSection = ds.getConfigurationSection("messages");
        if (messagesSection == null) return;

        disasterMessages.put("warning-title", messagesSection.getString("warning-title", "§4§l⚠ CẢNH BÁO THIÊN TAI ⚠"));
        disasterMessages.put("warning-subtitle", messagesSection.getString("warning-subtitle", "§c{name}\n§e§lSẽ xảy ra trong {time} giây!"));
        disasterMessages.put("warning-broadcast", messagesSection.getString("warning-broadcast", "§4§l⚠ {name} §r§cđang đến gần!"));
        disasterMessages.put("countdown-broadcast", messagesSection.getString("countdown-broadcast", "§4§l⚠ {name} §csẽ xảy ra trong §4§l{time}§c giây!"));
        disasterMessages.put("active-broadcast", messagesSection.getString("active-broadcast", "§4§l{name} - {message} (§e{duration}s§4)"));
        disasterMessages.put("end-broadcast", messagesSection.getString("end-broadcast", "§a§l✅ {name} đã kết thúc!"));
    }

    /**
     * Load boss general settings from bosses/_settings.yml
     */
    private void loadBossSettings() {
        File settingsFile = new File(plugin.getDataFolder(), "bosses/_settings.yml");
        if (!settingsFile.exists()) {
            File settingsDirectory = settingsFile.getParentFile();
            if (settingsDirectory != null && !settingsDirectory.exists() && !settingsDirectory.mkdirs()) {
                plugin.getLogger().warning("[Boss] Could not create bosses directory; boss settings will use defaults.");
            }
            plugin.saveResource("bosses/_settings.yml", false);
        }

        FileConfiguration bs = YamlConfiguration.loadConfiguration(settingsFile);
        boolean migrateBossSettings = false;
        if (Math.abs(bs.getDouble("army-token-drop.chance", 0.30) - 0.35) < 0.000001) {
            bs.set("army-token-drop.chance", 0.30);
            bs.setComments("army-token-drop.chance", List.of(
                "[VI] Tỉ lệ rơi 1 token khi boss bị người chơi gây sát thương; mặc định 30%, hợp lệ 0-1.",
                "[EN] Chance to drop 1 token when a player damages the boss; default 30%, valid range 0-1."));
            migrateBossSettings = true;
        }
        if (!bs.contains("experience-per-level-multiplier")) {
            bs.set("experience-per-level-multiplier", 1.0);
            bs.setComments("experience-per-level-multiplier", List.of(
                "[VI] Hệ số tăng EXP boss theo level; mặc định 1, hợp lệ 0-10.",
                "[EN] Boss XP scaling by level; default 1, valid range 0-10."));
            migrateBossSettings = true;
        }
        if (migrateBossSettings) {
            try {
                bs.save(settingsFile);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.SEVERE, "[Boss] Could not migrate bosses/_settings.yml.", exception);
            }
        }

        bossEventsEnabled = bs.getBoolean("enabled", true);
        bossSkillCastLogsEnabled = bs.getBoolean("skill-cast-feedback.console-log", true);
        bossSkillCastOpBroadcastEnabled = bs.getBoolean("skill-cast-feedback.broadcast-to-ops", true);
        bossSkillCastPlayerBroadcastEnabled = bs.getBoolean("skill-cast-feedback.broadcast-to-players", true);
        double rewardDamageThreshold = bs.getDouble("reward-damage-threshold-percent", 50.0);
        if (!Double.isFinite(rewardDamageThreshold) || rewardDamageThreshold < 0.0 ||
                rewardDamageThreshold > 100.0) {
            plugin.getLogger().warning("[Boss] reward-damage-threshold-percent must be 0-100; clamping.");
        }
        bossRewardDamageThresholdPercent = Double.isFinite(rewardDamageThreshold)
            ? Math.max(0.0, Math.min(100.0, rewardDamageThreshold)) : 50.0;
        bossArmyTokenDropEnabled = bs.getBoolean("army-token-drop.enabled", true);
        bossArmyTokenDropChance = readBoundedDouble(bs, "army-token-drop.chance", 0.30, 0.0, 1.0);
        bossExperiencePerLevelMultiplier = readBoundedDouble(
            bs, "experience-per-level-multiplier", 1.0, 0.0, 10.0);
        bossArmorPieceDropChance = readBoundedDouble(bs, "reward-items.armor-piece-chance",
            0.10, 0.0, 1.0);
        int armorLevelGap = bs.getInt("reward-items.armor-natural-level-gap", 2);
        if (armorLevelGap < 0 || armorLevelGap > 5) {
            plugin.getLogger().warning("[Boss] reward-items.armor-natural-level-gap must be 0-5; clamping.");
        }
        bossArmorNaturalLevelGap = Math.max(0, Math.min(5, armorLevelGap));
        bossArmorLevelsPerEnchant = readBoundedDouble(bs, "reward-items.enchant-levels-per-boss-level",
            0.8, 0.0, 5.0);
        int maximumArmorEnchantLevel = bs.getInt("reward-items.maximum-enchant-level", 5);
        if (maximumArmorEnchantLevel < 1 || maximumArmorEnchantLevel > 255) {
            plugin.getLogger().warning("[Boss] reward-items.maximum-enchant-level must be 1-255; clamping.");
        }
        bossArmorMaximumEnchantLevel = Math.max(1, Math.min(255, maximumArmorEnchantLevel));
        bossArmorAllowUnsafeEnchants = bs.getBoolean("reward-items.allow-over-vanilla-enchants", true);
        bossEventMinIntervalRaw = getStringFromConfig(bs.getConfigurationSection(""), "min-interval-seconds", "1200-2400");
        bossEventSpawnRadius = bs.getInt("spawn-radius", 50);
        bossLevelProgressionEnabled = bs.getBoolean("level-progression.enabled", true);
        bossLevelThresholds.clear();
        for (Map<?, ?> thresholdMap : bs.getMapList("level-progression.thresholds")) {
            Object dayValue = thresholdMap.get("min-day");
            Object minLevelValue = thresholdMap.get("min-level");
            Object maxLevelValue = thresholdMap.get("max-level");
            if (!(dayValue instanceof Number dayNumber) ||
                !(minLevelValue instanceof Number minNumber) ||
                !(maxLevelValue instanceof Number maxNumber)) {
                plugin.getLogger().warning("[Boss] Skipping malformed level-progression threshold in bosses/_settings.yml.");
                continue;
            }

            BossLevelThreshold threshold = new BossLevelThreshold();
            threshold.minDay = Math.max(0, dayNumber.longValue());
            threshold.minLevel = Math.max(1, minNumber.intValue());
            threshold.maxLevel = Math.max(threshold.minLevel, maxNumber.intValue());
            bossLevelThresholds.add(threshold);
        }
        bossLevelThresholds.sort(Comparator.comparingLong(threshold -> threshold.minDay));

        bossSpawnSuccessProgressionEnabled = bs.getBoolean("spawn-success-progression.enabled", true);
        double defaultChance = bs.getDouble("spawn-success-progression.default-percent", 100.0);
        if (!Double.isFinite(defaultChance)) {
            plugin.getLogger().warning("[Boss] Invalid spawn-success-progression.default-percent; using 100%.");
            defaultChance = 100.0;
        }
        bossSpawnSuccessDefaultPercent = Math.max(0.0, Math.min(100.0, defaultChance));
        if (bossSpawnSuccessDefaultPercent != defaultChance) {
            plugin.getLogger().warning("[Boss] Clamped default boss spawn success chance to " +
                bossSpawnSuccessDefaultPercent + "%.");
        }
        bossSpawnSuccessThresholds.clear();
        for (Map<?, ?> thresholdMap : bs.getMapList("spawn-success-progression.thresholds")) {
            Object dayValue = thresholdMap.get("min-day");
            Object chanceValue = thresholdMap.get("chance-percent");
            if (!(dayValue instanceof Number dayNumber) || !(chanceValue instanceof Number chanceNumber)) {
                plugin.getLogger().warning("[Boss] Skipping malformed spawn-success-progression threshold in bosses/_settings.yml.");
                continue;
            }

            double chance = chanceNumber.doubleValue();
            if (!Double.isFinite(chance)) {
                plugin.getLogger().warning("[Boss] Skipping non-finite spawn success chance in bosses/_settings.yml.");
                continue;
            }
            BossSpawnSuccessThreshold threshold = new BossSpawnSuccessThreshold();
            threshold.minDay = Math.max(0, dayNumber.longValue());
            threshold.chancePercent = Math.max(0.0, Math.min(100.0, chance));
            if (threshold.chancePercent != chance) {
                plugin.getLogger().warning("[Boss] Clamped spawn success chance at day " +
                    threshold.minDay + " to " + threshold.chancePercent + "%.");
            }
            bossSpawnSuccessThresholds.add(threshold);
        }
        bossSpawnSuccessThresholds.sort(Comparator.comparingLong(threshold -> threshold.minDay));
        plugin.getLogger().info("[Boss] Loaded " + bossLevelThresholds.size() +
            " daily level threshold(s) and " + bossSpawnSuccessThresholds.size() +
            " daily spawn-success threshold(s) from bosses/_settings.yml.");
    }

    /**
     * Load all boss configurations from bosses/*.yml files
     * Dynamically discovers all .yml files in the bosses/ directory
     */
    private void loadBossConfigs() {
        bossConfigs.clear();

        File bossesDir = new File(plugin.getDataFolder(), "bosses");
        if (!bossesDir.exists() || !bossesDir.isDirectory()) return;

        File[] bossFiles = bossesDir.listFiles((dir, name) -> name.endsWith(".yml") && !name.equals("_settings.yml"));
        if (bossFiles == null) return;

        for (File file : bossFiles) {
            String fileName = file.getName();
            String bossId = fileName.substring(0, fileName.length() - 4); // Remove .yml

            FileConfiguration section = YamlConfiguration.loadConfiguration(file);
            if (section == null) continue;

            BossConfig bc = new BossConfig();
            bc.enabled = section.getBoolean("enabled", true);
            bc.entityType = section.getString("entity-type", "WITHER");
            bc.displayName = section.getString("display-name", "§c§lBoss");
            bc.armyTokenClass = section.getString("army-token-class", "guardian");
            if (bc.armyTokenClass == null || !bc.armyTokenClass.matches("[a-z0-9_]+")) {
                plugin.getLogger().warning("[Boss] Invalid army-token-class in " + fileName
                    + "; using guardian.");
                bc.armyTokenClass = "guardian";
            }
            bc.hp = section.getDouble("hp", 100.0);
            bc.damageMultiplier = section.getDouble("damage-multiplier", 1.0);
            bc.chance = section.getInt("chance", 10);
            bc.experience = section.getInt("experience", defaultBossExperience(bossId));
            if (bc.experience < 1 || bc.experience > 1_000_000) {
                plugin.getLogger().warning("[Boss] Invalid experience in " + fileName
                    + "; using default 100.");
                bc.experience = 100;
            }
            bc.durationSeconds = section.getInt("duration-seconds", 1200);
            bc.warningSeconds = section.getInt("warning-seconds", 60);
            bc.activityRadius = section.getInt("activity-radius", 100);
            if (bc.activityRadius < 1 || bc.activityRadius > 10000) {
                plugin.getLogger().warning("[Boss] Invalid activity-radius in " + fileName +
                    "; using default 100 blocks.");
                bc.activityRadius = 100;
            }

            ConfigurationSection hpByLevelSection = section.getConfigurationSection("hp-by-level");
            if (hpByLevelSection != null) {
                for (String levelKey : hpByLevelSection.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelKey);
                        double hp = hpByLevelSection.getDouble(levelKey);
                        if (level > 0 && Double.isFinite(hp) && hp > 0) {
                            bc.hpByLevel.put(level, hp);
                        } else {
                            plugin.getLogger().warning("[Boss] Ignoring invalid hp-by-level." + levelKey +
                                " in " + fileName);
                        }
                    } catch (NumberFormatException exception) {
                        plugin.getLogger().warning("[Boss] Ignoring non-numeric hp-by-level key '" +
                            levelKey + "' in " + fileName);
                    }
                }
            }

            ConfigurationSection skillsSection = section.getConfigurationSection("skills");
            if (skillsSection != null) {
                for (String skillId : skillsSection.getKeys(false)) {
                    ConfigurationSection skillSection = skillsSection.getConfigurationSection(skillId);
                    if (skillSection == null) continue;
                    BossSkillConfig skill = new BossSkillConfig();
                    skill.enabled = skillSection.getBoolean("enabled", false);
                    skill.damage = Math.max(0, skillSection.getDouble("damage", 4.0));
                    skill.multiply = Math.max(1, skillSection.getInt("multiply", 1));
                    skill.cooldownSeconds = Math.max(1, skillSection.getInt("cooldown-seconds", 8));
                    skill.radius = Math.max(0, skillSection.getDouble("radius", 4.0));
                    skill.velocity = Math.max(0.1, skillSection.getDouble("velocity", 1.0));
                    skill.spread = Math.max(0, skillSection.getDouble("spread", 1.0));
                    skill.durationSeconds = Math.max(1, Math.min(600,
                        skillSection.getInt("duration-seconds", 5)));
                    skill.explosionPower = Math.max(1.0, Math.min(64.0,
                        skillSection.getDouble("explosion-power", 3.0)));
                    skill.chargedExplosionPower = Math.max(skill.explosionPower, Math.min(64.0,
                        skillSection.getDouble("charged-explosion-power", 6.0)));
                    skill.explosionCooldownSeconds = Math.max(1, Math.min(120,
                        skillSection.getInt("explosion-cooldown-seconds", 4)));
                    skill.options.putAll(skillSection.getValues(false));
                    bc.skills.put(skillId.toLowerCase(Locale.ROOT), skill);
                }
            }

            ConfigurationSection traitsSection = section.getConfigurationSection("traits");
            if (traitsSection != null) {
                for (String traitId : traitsSection.getKeys(false)) {
                    ConfigurationSection trait = traitsSection.getConfigurationSection(traitId);
                    if (trait != null) bc.traits.put(traitId, trait.getValues(false));
                }
            }

            // Load immunities
            ConfigurationSection immunitiesSection = section.getConfigurationSection("immunities");
            if (immunitiesSection != null) {
                // Boolean immunities
                for (String key : immunitiesSection.getKeys(false)) {
                    if (key.equals("potion-effects")) continue;
                    bc.immunities.put(key, immunitiesSection.getBoolean(key, false));
                }
                // Potion effect list immunity
                bc.immunityPotionEffects = immunitiesSection.getStringList("potion-effects");
            }

            // Load drops
            ConfigurationSection dropsSection = section.getConfigurationSection("drops");
            if (dropsSection != null) {
                for (String dropKey : dropsSection.getKeys(false)) {
                    ConfigurationSection dropSection = dropsSection.getConfigurationSection(dropKey);
                    if (dropSection == null) continue;
                    DropConfig dc = new DropConfig();
                    dc.minAmount = dropSection.getInt("min-amount", 1);
                    dc.maxAmount = dropSection.getInt("max-amount", 1);
                    dc.chance = dropSection.getDouble("chance", 0.5);
                    dc.name = dropSection.getString("name");
                    dc.lore = dropSection.getStringList("lore");
                    ConfigurationSection bookEnchants = dropSection.getConfigurationSection("book-enchantments");
                    if (bookEnchants != null) {
                        for (String enchantment : bookEnchants.getKeys(false)) {
                            int enchantLevel = bookEnchants.getInt(enchantment, 0);
                            if (enchantLevel > 0 && enchantLevel <= 255) {
                                dc.bookEnchantments.put(enchantment, enchantLevel);
                            } else {
                                plugin.getLogger().warning("[Boss] Ignoring invalid book enchantment '" +
                                    enchantment + "' for " + dropKey + " in " + fileName + ".");
                            }
                        }
                    }
                    bc.drops.put(dropKey, dc);
                }
            }

            ConfigurationSection armorSetSection = section.getConfigurationSection("armor-set");
            if (armorSetSection != null) {
                BossArmorSetConfig armorSet = new BossArmorSetConfig();
                armorSet.enabled = armorSetSection.getBoolean("enabled", true);
                armorSet.name = armorSetSection.getString("name", bc.displayName + " Di Vật");
                armorSet.storyId = armorSetSection.getString("story-id");
                double dropChance = armorSetSection.getDouble("drop-chance", bossArmorPieceDropChance);
                if (!Double.isFinite(dropChance) || dropChance < 0.0 || dropChance > 1.0) {
                    plugin.getLogger().warning("[Boss] Invalid armor-set.drop-chance in " + fileName +
                        "; using global default.");
                    dropChance = bossArmorPieceDropChance;
                }
                armorSet.dropChance = dropChance;
                ConfigurationSection pieceNames = armorSetSection.getConfigurationSection("piece-names");
                if (pieceNames != null) {
                    for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
                        String pieceName = pieceNames.getString(slot);
                        if (pieceName != null && !pieceName.isBlank()) {
                            armorSet.pieceNames.put(slot.toUpperCase(Locale.ROOT), pieceName);
                        }
                    }
                }
                ConfigurationSection enchantments = armorSetSection.getConfigurationSection("enchantments");
                if (enchantments != null) {
                    for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
                        List<String> configuredEnchants = enchantments.getStringList(slot);
                        if (!configuredEnchants.isEmpty()) {
                            armorSet.enchantmentsBySlot.put(slot.toUpperCase(Locale.ROOT),
                                configuredEnchants.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList());
                        }
                    }
                }
                ConfigurationSection bonuses = armorSetSection.getConfigurationSection("bonuses");
                if (bonuses != null) {
                    for (String piecesKey : bonuses.getKeys(false)) {
                        int pieces;
                        try {
                            pieces = Integer.parseInt(piecesKey);
                        } catch (NumberFormatException exception) {
                            plugin.getLogger().warning("[Boss] Ignoring invalid armor bonus threshold '"
                                + piecesKey + "' in " + fileName + ".");
                            continue;
                        }
                        if (pieces < 2 || pieces > 4) continue;
                        ConfigurationSection bonusSection = bonuses.getConfigurationSection(piecesKey);
                        if (bonusSection == null) continue;
                        BossArmorBonusConfig bonus = new BossArmorBonusConfig();
                        bonus.damageReductionPercent = readBoundedDouble(bonusSection,
                            "damage-reduction-percent", 0.0, 0.0, 1.0);
                        bonus.damageCauses = bonusSection.getStringList("damage-causes").stream()
                            .map(value -> value.toUpperCase(Locale.ROOT)).toList();
                        bonus.knockbackReductionPercent = readBoundedDouble(bonusSection,
                            "knockback-reduction-percent", 0.0, 0.0, 1.0);
                        bonus.blockingDamageReductionPercent = readBoundedDouble(bonusSection,
                            "blocking-damage-reduction-percent", 0.0, 0.0, 1.0);
                        bonus.effectDurationReductionPercent = readBoundedDouble(bonusSection,
                            "effect-duration-reduction-percent", 0.0, 0.0, 1.0);
                        bonus.healOnUndeadKill = readBoundedDouble(bonusSection,
                            "heal-on-undead-kill", 0.0, 0.0, 20.0);
                        bonus.healCooldownSeconds = Math.max(1, Math.min(3600,
                            bonusSection.getInt("heal-cooldown-seconds", 10)));
                        armorSet.bonuses.put(pieces, bonus);
                    }
                }
                ConfigurationSection materialTiers = armorSetSection.getConfigurationSection("material-tiers");
                if (materialTiers != null) {
                    for (String levelKey : materialTiers.getKeys(false)) {
                        try {
                            int level = Integer.parseInt(levelKey);
                            String material = materialTiers.getString(levelKey, "").trim().toUpperCase(Locale.ROOT);
                            if (level >= 1 && level <= 1000 && !material.isBlank()) {
                                if (Material.matchMaterial(material + "_HELMET") == null) {
                                    plugin.getLogger().warning("[Boss] Invalid armor material tier '" +
                                        material + "' in " + fileName + ".");
                                } else {
                                    armorSet.materialTiers.put(level, material);
                                }
                            }
                        } catch (NumberFormatException exception) {
                            plugin.getLogger().warning("[Boss] Ignoring invalid armor material tier '" +
                                levelKey + "' in " + fileName + ".");
                        }
                    }
                }
                if (armorSet.enabled && (armorSet.storyId == null || armorSet.storyId.isBlank())) {
                    plugin.getLogger().warning("[Boss] armor-set.story-id is missing in " + fileName +
                        "; armor rewards will be skipped until a story ID is configured.");
                    armorSet.enabled = false;
                } else if (armorSet.enabled && !bossItemStories.containsKey(armorSet.storyId)
                        && List.of("helmet", "chestplate", "leggings", "boots").stream()
                            .noneMatch(slot -> bossArmorPieceStories.containsKey(armorSet.storyId + ":" + slot))) {
                    plugin.getLogger().warning("[Boss] Missing story-id '" + armorSet.storyId +
                        "' in story.yml for armor set in " + fileName + ".");
                    armorSet.enabled = false;
                }
                if (armorSet.enabled && armorSet.materialTiers.isEmpty()) {
                    plugin.getLogger().warning("[Boss] armor-set has no valid material tiers in " + fileName +
                        "; armor rewards are disabled.");
                    armorSet.enabled = false;
                }
                bc.armorSet = armorSet;
            }

            ConfigurationSection levelDropsSection = section.getConfigurationSection("level-drops");
            if (levelDropsSection != null) {
                for (String levelKey : levelDropsSection.getKeys(false)) {
                    int level;
                    try {
                        level = Integer.parseInt(levelKey);
                    } catch (NumberFormatException exception) {
                        plugin.getLogger().warning("[Boss] Ignoring invalid level-drops key '" +
                            levelKey + "' in " + fileName + ".");
                        continue;
                    }
                    if (level < 1 || level > 1000) {
                        plugin.getLogger().warning("[Boss] Ignoring out-of-range level-drops key '" +
                            levelKey + "' in " + fileName + ".");
                        continue;
                    }

                    List<ConfiguredItem> items = new ArrayList<>();
                    for (Map<?, ?> itemMap : section.getMapList("level-drops." + levelKey)) {
                        ConfiguredItem item = new ConfiguredItem();
                        Object materialValue = itemMap.get("material");
                        if (materialValue instanceof String material) item.material = material;
                        Object nameValue = itemMap.get("name");
                        if (nameValue instanceof String name) item.name = name;
                        Object storyIdValue = itemMap.get("story-id");
                        if (storyIdValue instanceof String storyId) {
                            List<String> story = bossItemStories.get(storyId);
                            if (story == null) {
                                plugin.getLogger().warning("[Boss] Missing story-id '" + storyId +
                                    "' in story.yml for " + fileName + " level " + level + ".");
                            } else {
                                for (String line : story) item.lore.add(line.replace("{level}",
                                    Integer.toString(level)));
                            }
                        }
                        Object loreValue = itemMap.get("lore");
                        if (item.lore.isEmpty() && loreValue instanceof List<?> lore) {
                            for (Object line : lore) {
                                if (line instanceof String text) item.lore.add(text);
                            }
                        }
                        Object minValue = itemMap.get("min-amount");
                        if (minValue instanceof Number number) item.minAmount = number.intValue();
                        Object maxValue = itemMap.get("max-amount");
                        if (maxValue instanceof Number number) item.maxAmount = number.intValue();
                        Object chanceValue = itemMap.get("chance");
                        if (chanceValue instanceof Number number) item.chance = number.doubleValue();
                        readEnchantments(itemMap.get("enchantments"), item.enchantments);
                        readEnchantments(itemMap.get("book-enchantments"), item.bookEnchantments);

                        if (item.minAmount < 1 || item.maxAmount < item.minAmount ||
                                item.maxAmount > 64 || !Double.isFinite(item.chance) ||
                                item.chance < 0.0 || item.chance > 1.0) {
                            plugin.getLogger().warning("[Boss] Ignoring invalid level drop at " + fileName +
                                " level " + level + ".");
                            continue;
                        }
                        items.add(item);
                    }
                    bc.levelDrops.put(level, items);
                }
            }

            bossConfigs.put(bossId, bc);
        }
    }

    private static int defaultBossExperience(String bossId) {
        return switch (bossId) {
            case "zombie_boss" -> 200;
            case "skeleton_boss", "spider_boss" -> 250;
            case "enderman_boss", "phantom_boss" -> 300;
            case "creeper_boss", "witch_boss" -> 350;
            case "ghast" -> 400;
            case "vindicator_boss" -> 450;
            case "ravager_boss" -> 600;
            case "giant_boss" -> 1000;
            case "wither" -> 1500;
            case "ender_dragon" -> 2000;
            default -> 100;
        };
    }

    private void loadBossItemStories() {
        bossItemStories.clear();
        bossArmorPieceStories.clear();
        File storyFile = new File(plugin.getDataFolder(), "story.yml");
        YamlConfiguration storyConfig = YamlConfiguration.loadConfiguration(storyFile);
        ConfigurationSection artifacts = storyConfig.getConfigurationSection("artifacts");
        if (artifacts == null) {
            plugin.getLogger().warning("[Boss] story.yml has no 'artifacts' section; item lore will use boss files.");
            return;
        }

        for (String storyId : artifacts.getKeys(false)) {
            ConfigurationSection artifact = artifacts.getConfigurationSection(storyId);
            if (artifact == null) continue;
            List<String> lore = artifact.getStringList("lore");
            if (!lore.isEmpty()) bossItemStories.put(storyId, List.copyOf(lore));
            ConfigurationSection pieces = artifact.getConfigurationSection("pieces");
            if (pieces != null) {
                for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
                    ConfigurationSection piece = pieces.getConfigurationSection(slot);
                    if (piece == null) continue;
                    BossArmorPieceStory pieceStory = new BossArmorPieceStory();
                    pieceStory.itemName = piece.getString("item");
                    pieceStory.lore = List.copyOf(piece.getStringList("lore"));
                    if (pieceStory.itemName != null || !pieceStory.lore.isEmpty()) {
                        bossArmorPieceStories.put(storyId + ":" + slot, pieceStory);
                    }
                }
            }
        }
        plugin.getLogger().info("[Boss] Loaded " + bossItemStories.size() +
            " item stories and " + bossArmorPieceStories.size() + " armor-piece stories from story.yml.");
    }

    public List<String> getBossItemStory(String storyId, int level) {
        List<String> lore = bossItemStories.get(storyId);
        if (lore == null) return List.of();
        return lore.stream().map(line -> line.replace("{level}", Integer.toString(level))).toList();
    }

    public BossArmorPieceStory getBossArmorPieceStory(String storyId, String slot, int level) {
        BossArmorPieceStory story = bossArmorPieceStories.get(storyId + ":" + slot.toLowerCase(Locale.ROOT));
        if (story == null) return null;
        BossArmorPieceStory resolved = new BossArmorPieceStory();
        resolved.itemName = story.itemName == null ? null
            : story.itemName.replace("{level}", Integer.toString(level));
        resolved.lore = story.lore.stream()
            .map(line -> line.replace("{level}", Integer.toString(level))).toList();
        return resolved;
    }

    private void readEnchantments(Object source, Map<String, Integer> destination) {
        if (!(source instanceof Map<?, ?> enchantments)) return;
        for (Map.Entry<?, ?> entry : enchantments.entrySet()) {
            if (!(entry.getKey() instanceof String name) || !(entry.getValue() instanceof Number level)) continue;
            if (name.isBlank() || level.intValue() < 1 || level.intValue() > 255) {
                plugin.getLogger().warning("[Boss] Ignoring invalid enchantment '" + name + "'.");
                continue;
            }
            destination.put(name, level.intValue());
        }
    }
}