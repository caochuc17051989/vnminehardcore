package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class BossEventManager implements Listener {

    private final VnMineHardcore plugin;
    private final NamespacedKey armyOwnerKey;
    private final ConfigManager config;
    private final LogManager logManager;
    private final BossLeaderboardManager leaderboardManager;
    private final Logger logger;
    private final Random random = ThreadLocalRandom.current();

    private boolean bossActive = false;
    private double bossRewardThresholdHealth;
    private final Map<UUID, Double> bossDamageContributions = new HashMap<>();
    private String currentBossName = null;
    private LivingEntity currentBoss = null;
    private ConfigManager.BossConfig activeBossConfig;
    private BossBar bossBar;
    private int timeSinceLastBoss = 0;
    private int bossEventIntervalSeconds = 0;
    private int warningTimeLeft = 0;
    private ConfigManager.BossConfig pendingBoss = null;
    private Integer pendingBossLevel = null;

    // Tracking thời gian boss đã tồn tại
    private int bossElapsedSeconds = 0;
    private int bossDurationSeconds = 0;
    private BukkitTask schedulerTask;
    private BukkitTask bossMonitorTask;

    // AI tracking
    private Location currentBossSpawnLocation = null;
    private int currentBossActivityRadius = 100;
    private World forcedBossChunkWorld;
    private int forcedBossChunkX;
    private int forcedBossChunkZ;
    private boolean bossChunkWasAlreadyForceLoaded;
    private boolean returningToAnchor;
    private long anchorPathUpdatedAtMs;
    private Location giantWanderTarget;
    private long giantWanderUntilMs;
    private BukkitTask aiTask;
    private int currentBossLevel = 1;
    private long nextCreeperExplosionAtMs;
    private BukkitTask creeperChargeTask;
    private final Map<String, Long> bossSkillCooldowns = new HashMap<>();
    private final Set<Entity> activeBossProjectiles = new HashSet<>();
    private final Map<UUID, BossProjectileConfig> bossProjectileConfigs = new HashMap<>();
    private final Set<BukkitTask> bossSkillTasks = new HashSet<>();
    private final Set<UUID> bossSummons = new HashSet<>();
    private final Set<UUID> bossTerrainEntities = new HashSet<>();
    private final Map<UUID, Boolean> bossTerrainDrops = new HashMap<>();
    private final Set<UUID> bossBoulders = new HashSet<>();
    private final Set<UUID> webTrapProjectiles = new HashSet<>();
    private final Set<UUID> disarmProjectiles = new HashSet<>();
    private final Map<UUID, Location> splitFireballs = new HashMap<>();
    private final Map<UUID, Long> ravagerThornsCooldowns = new HashMap<>();
    private final Map<UUID, Long> spinDamageCooldowns = new HashMap<>();
    private final Map<BlockPosition, TemporaryBlock> temporaryBlocks = new HashMap<>();
    private final Set<BlockPosition> playerModifiedTemporaryBlocks = new HashSet<>();
    private final Map<UUID, Long> phantomRevealedUntil = new HashMap<>();
    private boolean spiderBroodReleased;
    private static final double MAX_BOSS_HEALTH = 1024.0;

    private record BossProjectileConfig(double damage, double radius, boolean splashOnImpact) { }
    private record BlockPosition(UUID worldId, int x, int y, int z) { }
    private record TemporaryBlock(BlockState original, BlockData placedData) { }

    public BossEventManager(VnMineHardcore plugin, ConfigManager config) {
        this.plugin = plugin;
        this.armyOwnerKey = new NamespacedKey(plugin, "army_owner");
        this.config = config;
        this.logManager = plugin.getLogManager();
        this.leaderboardManager = plugin.getBossLeaderboardManager();
        this.logger = plugin.getLogger();

        this.bossBar = BossBar.bossBar(
            Component.text(""),
            1.0f,
            BossBar.Color.PURPLE,
            BossBar.Overlay.PROGRESS
        );

        if (config.bossEventsEnabled) start();

        // Đăng ký listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        logger.info("[BossEvent] Initialized: enabled=" + config.bossEventsEnabled +
            ", interval=" + config.bossEventMinIntervalRaw + "s" +
            ", bosses=" + config.bossConfigs.size());
    }

    private void keepBossChunkLoaded() {
        if (currentBoss == null || !currentBoss.isValid()) return;
        Location location = currentBoss.getLocation();
        World world = location.getWorld();
        if (world == null) return;
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (world.equals(forcedBossChunkWorld) && chunkX == forcedBossChunkX
                && chunkZ == forcedBossChunkZ) return;

        if (forcedBossChunkWorld != null && !bossChunkWasAlreadyForceLoaded) {
            forcedBossChunkWorld.setChunkForceLoaded(forcedBossChunkX, forcedBossChunkZ, false);
        }
        bossChunkWasAlreadyForceLoaded = world.isChunkForceLoaded(chunkX, chunkZ);
        if (!bossChunkWasAlreadyForceLoaded) world.setChunkForceLoaded(chunkX, chunkZ, true);
        forcedBossChunkWorld = world;
        forcedBossChunkX = chunkX;
        forcedBossChunkZ = chunkZ;
    }

    public void start() {
        if (!config.bossEventsEnabled || (schedulerTask != null && !schedulerTask.isCancelled())) return;
        timeSinceLastBoss = 0;
        bossEventIntervalSeconds = sampleBossEventInterval();
        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getWorlds().isEmpty()) return;
                if (!bossActive && pendingBoss == null) {
                    timeSinceLastBoss++;
                    if (timeSinceLastBoss >= bossEventIntervalSeconds) {
                        trySpawnBoss();
                    }
                }

                // Update warning bar
                if (warningTimeLeft > 0 && pendingBoss != null) {
                    warningTimeLeft--;
                    updateWarningBar();
                    if (warningTimeLeft <= 0) {
                        // Warning ended, spawn boss now
                        ConfigManager.BossConfig bossToSpawn = pendingBoss;
                        Integer levelToSpawn = pendingBossLevel;
                        pendingBoss = null;
                        pendingBossLevel = null;
                        spawnBoss(bossToSpawn, levelToSpawn);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Every second
    }

    private int sampleBossEventInterval() {
        return Math.max(1, ConfigManager.parseRangeOrInt(config.bossEventMinIntervalRaw, 1200));
    }

    private int sampleBossLevel(ConfigManager.BossConfig bc) {
        if (!config.bossLevelProgressionEnabled || bc == null) return 1;
        if (config.bossLevelThresholds == null || config.bossLevelThresholds.isEmpty()) return 1;

        long worldDay = getCurrentWorldDay();

        ConfigManager.BossLevelThreshold selected = null;
        for (ConfigManager.BossLevelThreshold threshold : config.bossLevelThresholds) {
            if (threshold == null) continue;
            if (threshold.minDay <= worldDay && (selected == null || threshold.minDay > selected.minDay)) {
                selected = threshold;
            }
        }
        if (selected == null) return 1;

        int min = Math.max(1, Math.min(1000, selected.minLevel));
        int max = Math.max(min, Math.min(1000, selected.maxLevel));
        return random.nextInt(min, max + 1);
    }

    private long getCurrentWorldDay() {
        if (Bukkit.getWorlds().isEmpty()) return 0L;
        return Math.max(0L, Bukkit.getWorlds().get(0).getFullTime() / 24000L);
    }

    private double getBossSpawnSuccessChance() {
        if (!config.bossSpawnSuccessProgressionEnabled ||
                config.bossSpawnSuccessThresholds == null ||
                config.bossSpawnSuccessThresholds.isEmpty()) {
            return config.bossSpawnSuccessProgressionEnabled
                ? config.bossSpawnSuccessDefaultPercent
                : 100.0;
        }

        long worldDay = getCurrentWorldDay();
        ConfigManager.BossSpawnSuccessThreshold selected = null;
        for (ConfigManager.BossSpawnSuccessThreshold threshold : config.bossSpawnSuccessThresholds) {
            if (threshold != null && threshold.minDay <= worldDay &&
                    (selected == null || threshold.minDay > selected.minDay)) {
                selected = threshold;
            }
        }
        return selected == null ? config.bossSpawnSuccessDefaultPercent : selected.chancePercent;
    }

    private double resolveBossHp(ConfigManager.BossConfig bc, int level) {
        if (bc == null) return 1.0;
        if (bc.hpByLevel != null && !bc.hpByLevel.isEmpty()) {
            Double exact = bc.hpByLevel.get(level);
            if (isValidBossHealth(exact)) return Math.min(MAX_BOSS_HEALTH, exact);
            Integer bestLevel = null;
            Double bestHp = null;
            for (Map.Entry<Integer, Double> entry : bc.hpByLevel.entrySet()) {
                if (entry.getKey() != null && entry.getKey() <= level && isValidBossHealth(entry.getValue())
                        && (bestLevel == null || entry.getKey() > bestLevel)) {
                    bestLevel = entry.getKey();
                    bestHp = entry.getValue();
                }
            }
            if (bestHp != null) return Math.min(MAX_BOSS_HEALTH, bestHp);
        }
        double baseHp = isValidBossHealth(bc.hp) ? bc.hp : 1.0;
        double scaledHp = baseHp * Math.pow(1.5, Math.max(0, level - 1));
        if (!Double.isFinite(scaledHp)) return MAX_BOSS_HEALTH;
        return Math.max(1.0, Math.min(MAX_BOSS_HEALTH, scaledHp));
    }

    private boolean isValidBossHealth(Double hp) {
        return hp != null && Double.isFinite(hp) && hp > 0.0;
    }

    private double safeSkillValue(double value, double fallback, double minimum, double maximum) {
        if (!Double.isFinite(value)) return fallback;
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void registerBossProjectile(Projectile projectile, double damage, double radius, boolean splashOnImpact) {
        activeBossProjectiles.add(projectile);
        bossProjectileConfigs.put(projectile.getUniqueId(),
            new BossProjectileConfig(damage, radius, splashOnImpact));
    }

    private void tickSplitFireballs() {
        if (splitFireballs.isEmpty()) return;
        for (Map.Entry<UUID, Location> entry : new ArrayList<>(splitFireballs.entrySet())) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Fireball fireball) || !fireball.isValid()) {
                splitFireballs.remove(entry.getKey());
                continue;
            }
            if (fireball.getLocation().distanceSquared(entry.getValue()) >= 100.0) {
                splitFireballs.remove(entry.getKey());
                bossProjectileConfigs.remove(entry.getKey());
                activeBossProjectiles.remove(fireball);
                Location splitAt = fireball.getLocation().clone();
                Vector incoming = fireball.getVelocity();
                fireball.remove();
                splitGhastFireball(splitAt, incoming);
            }
        }
    }

    private void tickBossTraits(ConfigManager.BossConfig bc) {
        if (currentBoss == null || currentBoss.isDead()) return;
        if (currentBoss.getType() == EntityType.ENDERMAN && traitEnabled(bc, "hydro-phobic-aura", true)) {
            Block feet = currentBoss.getLocation().getBlock();
            if (feet.isLiquid() || feet.getRelative(0, -1, 0).isLiquid()
                    || currentBoss.getWorld().hasStorm()) {
                String cooldownKey = currentBoss.getUniqueId() + ":hydro-aura";
                long now = System.currentTimeMillis();
                if (now >= bossSkillCooldowns.getOrDefault(cooldownKey, 0L)) {
                    double range = traitNumber(bc, "hydro-phobic-aura", "radius", 5.0, 1.0, 12.0);
                    int wear = (int) traitNumber(bc, "hydro-phobic-aura", "armor-damage", 1.0, 0.0, 5.0);
                    for (Entity entity : currentBoss.getWorld().getNearbyEntities(
                            currentBoss.getLocation(), range, range, range)) {
                        if (!(entity instanceof Player player)
                                || player.getLocation().distanceSquared(currentBoss.getLocation()) > range * range) continue;
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, false, true, true));
                        damageArmorDurability(player, wear);
                    }
                    bossSkillCooldowns.put(cooldownKey,
                        now + (long) traitNumber(bc, "hydro-phobic-aura", "cooldown-seconds", 3.0, 1.0, 30.0) * 1000L);
                }
            }
        }
        if (currentBoss.getType() == EntityType.PHANTOM && traitEnabled(bc, "nocturnal-cloak", true)) {
            long revealedUntil = phantomRevealedUntil.getOrDefault(currentBoss.getUniqueId(), 0L);
            int lightThreshold = (int) traitNumber(bc, "nocturnal-cloak", "light-threshold", 7.0, 0.0, 15.0);
            if (currentBoss.getLocation().getBlock().getLightLevel() < lightThreshold
                    && System.currentTimeMillis() >= revealedUntil) {
                currentBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0,
                    false, false, false));
            } else {
                currentBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }
        if (currentBoss.getType() == EntityType.SPIDER && !spiderBroodReleased
                && traitEnabled(bc, "brood-release", true)
                && currentBoss.getHealth() <= currentBoss.getMaxHealth()
                    * traitNumber(bc, "brood-release", "health-threshold", 0.5, 0.1, 0.9)) {
            spiderBroodReleased = true;
            int minimum = (int) traitNumber(bc, "brood-release", "minions-min", 4.0, 1.0, 8.0);
            int maximum = (int) traitNumber(bc, "brood-release", "minions-max", 6.0, minimum, 8.0);
            for (int i = 0; i < random.nextInt(minimum, maximum + 1); i++) {
                Location spawn = currentBoss.getLocation().add(
                    random.nextDouble() * 3.0 - 1.5, 0.0, random.nextDouble() * 3.0 - 1.5);
                CaveSpider minion = currentBoss.getWorld().spawn(spawn, CaveSpider.class);
                bossSummons.add(minion.getUniqueId());
            }
        }
        if (currentBoss.getType() == EntityType.VINDICATOR && traitEnabled(bc, "enrage", true)
                && currentBoss.getHealth() <= currentBoss.getMaxHealth()
                    * traitNumber(bc, "enrage", "health-threshold", 0.3, 0.05, 0.8)) {
            currentBoss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, true, true));
            currentBoss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, true, true));
            if (currentBoss.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
                currentBoss.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
            }
        }
    }

    private void damageArmorDurability(Player player, int amount) {
        if (amount <= 0) return;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || armor.getType().isAir()
                    || !(armor.getItemMeta() instanceof Damageable damageable)) continue;
            if (armor.getItemMeta().isUnbreakable()) continue;
            int unbreaking = armor.getEnchantmentLevel(Enchantment.UNBREAKING);
            int actualDamage = 0;
            for (int point = 0; point < amount; point++) {
                if (random.nextDouble() < 0.6 / (unbreaking + 1.0)) actualDamage++;
            }
            if (actualDamage == 0) continue;
            int nextDamage = damageable.getDamage() + actualDamage;
            if (nextDamage >= armor.getType().getMaxDurability()) {
                armor.setAmount(0);
            } else {
                damageable.setDamage(nextDamage);
                armor.setItemMeta(damageable);
            }
        }
    }

    private void damageEntitiesInRadius(World world, Location center, double radius, double damage) {
        double radiusSquared = radius * radius;
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !living.equals(currentBoss)
                    && living.isValid() && !living.isDead()
                    && living.getLocation().distanceSquared(center) <= radiusSquared) {
                living.damage(damage, currentBoss);
            }
        }
    }

    private boolean belongsToCurrentBoss(Entity entity) {
        if (currentBoss == null) return false;
        if (entity.equals(currentBoss)) return true;
        return entity instanceof Projectile projectile
            && projectile.getShooter() instanceof Entity shooter
            && shooter.equals(currentBoss);
    }

    @EventHandler
    public void onBossSkillProjectileDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Projectile fireball
                && event.getDamager() instanceof Player
                && currentBoss instanceof Ghast
                && traitEnabled(activeBossConfig, "fireball-shield", true)
                && fireball.getShooter() instanceof Entity shooter && shooter.equals(currentBoss)) {
            event.setCancelled(true);
            splitFireballs.remove(fireball.getUniqueId());
            bossProjectileConfigs.remove(fireball.getUniqueId());
            activeBossProjectiles.remove(fireball);
            fireball.remove();
            return;
        }
        if (!(event.getDamager() instanceof Projectile projectile)) return;
        BossProjectileConfig skill = bossProjectileConfigs.get(projectile.getUniqueId());
        if (skill != null) {
            if (event.getEntity().getLocation().distanceSquared(projectile.getLocation())
                    > skill.radius() * skill.radius()) {
                event.setCancelled(true);
                return;
            }
            event.setDamage(skill.damage());
        }
    }

    @EventHandler
    public void onBossSkillProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        UUID projectileId = projectile.getUniqueId();
        if (splitFireballs.remove(projectileId) != null) {
            splitGhastFireball(projectile.getLocation(), projectile.getVelocity());
        }
        if (webTrapProjectiles.remove(projectileId) && currentBoss != null && !currentBoss.isDead()) {
            ConfigManager.BossSkillConfig trap = skillConfig(activeBossConfig, "cobweb-trap");
            placeTemporaryWebTrap(projectile.getLocation(), trap);
        }
        if (disarmProjectiles.remove(projectileId)
                && event.getHitEntity() instanceof Player player) {
            disarmPlayer(player, skillConfig(activeBossConfig, "disarm-magic"));
        }
        BossProjectileConfig skill = bossProjectileConfigs.remove(projectile.getUniqueId());
        activeBossProjectiles.remove(projectile);
        projectile.remove();
        if (skill != null && skill.splashOnImpact() && currentBoss != null && !currentBoss.isDead()) {
            World world = projectile.getWorld();
            Location hitLocation = projectile.getLocation();
            double radiusSquared = skill.radius() * skill.radius();
            for (Entity entity : world.getNearbyEntities(hitLocation, skill.radius(), skill.radius(), skill.radius())) {
                if (entity instanceof LivingEntity living && !living.equals(currentBoss)
                        && living.isValid() && !living.isDead()
                        && living.getLocation().distanceSquared(hitLocation) <= radiusSquared) {
                    living.damage(skill.damage(), currentBoss);
                }
            }
        }
    }

    @EventHandler
    public void onBossTerrainExplosion(EntityExplodeEvent event) {
        if (!belongsToCurrentBoss(event.getEntity())
                && !bossProjectileConfigs.containsKey(event.getEntity().getUniqueId())) return;
        event.blockList().clear();
        event.setYield(0.0F);
    }

    @EventHandler
    public void onBossCreeperExplosionPrime(ExplosionPrimeEvent event) {
        if (currentBoss instanceof Creeper && event.getEntity().equals(currentBoss)) {
            event.setCancelled(true);
            ((Creeper) currentBoss).setIgnited(false);
            ((Creeper) currentBoss).setFuseTicks(0);
        }
    }

    @EventHandler
    public void onBossTerrainChange(EntityChangeBlockEvent event) {
        UUID id = event.getEntity().getUniqueId();
        if (bossTerrainEntities.remove(id)) {
            event.setCancelled(true);
            if (bossBoulders.remove(id)) detonateBoulder(event.getEntity().getLocation());
            boolean dropBlock = bossTerrainDrops.remove(id) == Boolean.TRUE;
            if (dropBlock && event.getEntity() instanceof FallingBlock falling) {
                falling.getWorld().dropItemNaturally(falling.getLocation(),
                    new ItemStack(event.getTo()));
            }
            event.getEntity().remove();
            activeBossProjectiles.remove(event.getEntity());
            return;
        }
        if (belongsToCurrentBoss(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBossDamageProtection(EntityDamageEvent event) {
        if (currentBoss == null || event.getEntity() != currentBoss) return;
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)
                || playerAttacker(damageEvent.getDamager()) == null) {
            event.setCancelled(true);
            return;
        }
        if (currentBoss.getType() == EntityType.GIANT && traitEnabled(activeBossConfig, "giant-armor", true)) {
            boolean headshot = damageEvent.getDamager() instanceof Arrow projectile
                && projectile.getShooter() instanceof Player
                && projectile.getLocation().getY() >= currentBoss.getLocation().getY()
                    + currentBoss.getHeight() * 0.72;
            if (headshot) {
                damageEvent.setDamage(damageEvent.getDamage() * traitNumber(
                    activeBossConfig, "giant-armor", "headshot-multiplier", 2.0, 1.0, 5.0));
                currentBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 8, false, true, true));
                currentBoss.setVelocity(new Vector(0.0, 0.0, 0.0));
            } else {
                damageEvent.setDamage(damageEvent.getDamage() * 0.2);
            }
        }
        if (currentBoss.getType() == EntityType.RAVAGER
                && damageEvent.getDamager() instanceof Player player
                && damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && traitEnabled(activeBossConfig, "thorns-armor", true)
                && ravagerThornsCooldowns.getOrDefault(player.getUniqueId(), 0L) < System.currentTimeMillis()) {
            double reflected = Math.max(0.0, damageEvent.getDamage() * traitNumber(
                activeBossConfig, "thorns-armor", "reflection-percent", 0.2, 0.0, 1.0));
            ravagerThornsCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 250L);
            if (reflected > 0.0) {
                LivingEntity source = currentBoss;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (source != null && source.isValid() && player.isOnline() && player.getHealth() > 0.0) {
                        player.damage(reflected, source);
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossPlayerDamage(EntityDamageByEntityEvent event) {
        if (currentBoss == null || event.getEntity() != currentBoss
                || playerAttacker(event.getDamager()) == null) return;
        double damage = Math.min(event.getFinalDamage(), currentBoss.getHealth());
        if (Double.isFinite(damage) && damage > 0.0) {
            Player attacker = playerAttacker(event.getDamager());
            bossDamageContributions.merge(attacker.getUniqueId(), damage, Double::sum);
            if (currentBoss.getType() == EntityType.PHANTOM) revealPhantom(3000L);
            if (currentBoss.getType() == EntityType.PHANTOM
                    && event.getDamager() instanceof Projectile projectile
                    && projectile.getShooter() instanceof Player) {
                phantomRevealedUntil.put(currentBoss.getUniqueId(), System.currentTimeMillis() + 3000L);
                currentBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossEffectDrain(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || currentBoss == null
                || currentBoss.getType() != EntityType.WITCH || currentBoss.isDead()
                || !traitEnabled(activeBossConfig, "witchcraft-drain", true)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.POISON
                && cause != EntityDamageEvent.DamageCause.WITHER
                && cause != EntityDamageEvent.DamageCause.MAGIC
                && cause != EntityDamageEvent.DamageCause.DRAGON_BREATH) return;
        double radius = traitNumber(activeBossConfig, "witchcraft-drain", "radius", 16.0, 1.0, 64.0);
        if (!player.getWorld().equals(currentBoss.getWorld())
                || player.getLocation().distanceSquared(currentBoss.getLocation()) > radius * radius) return;
        double heal = event.getFinalDamage() * traitNumber(
            activeBossConfig, "witchcraft-drain", "heal-percent", 0.5, 0.0, 1.0);
        if (Double.isFinite(heal) && heal > 0.0) {
            currentBoss.setHealth(Math.min(currentBoss.getMaxHealth(), currentBoss.getHealth() + heal));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBossDeathDrops(EntityDeathEvent event) {
        if (currentBoss == event.getEntity()) event.setDroppedExp(0);
        if (currentBoss == event.getEntity() && getRewardEligiblePlayers().isEmpty()) {
            event.getDrops().clear();
        }
    }

    private Set<UUID> getRewardEligiblePlayers() {
        if (bossRewardThresholdHealth <= 0.0) return Set.of();
        double requiredDamage = bossRewardThresholdHealth * config.bossRewardDamageThresholdPercent / 100.0;
        Set<UUID> eligible = new HashSet<>();
        for (Map.Entry<UUID, Double> contribution : bossDamageContributions.entrySet()) {
            if (contribution.getValue() > requiredDamage) eligible.add(contribution.getKey());
        }
        return eligible;
    }

    private Player playerAttacker(Entity damager) {
        Entity source = damager;
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            source = shooter;
        }
        if (source instanceof Player player) return player;
        String owner = source.getPersistentDataContainer().get(armyOwnerKey, PersistentDataType.STRING);
        try {
            return owner == null ? null : Bukkit.getPlayer(UUID.fromString(owner));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String findSkillKey(ConfigManager.BossConfig bc, String... candidates) {
        if (bc == null || bc.skills == null || bc.skills.isEmpty()) return null;
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) continue;
            for (String key : bc.skills.keySet()) {
                if (key == null) continue;
                if (key.equalsIgnoreCase(candidate)) return key;
                if (key.replace("-", "").replace("_", "").equalsIgnoreCase(candidate.replace("-", "").replace("_", ""))) {
                    return key;
                }
            }
        }
        return null;
    }

    private ConfigManager.BossSkillConfig getCreeperSkillConfig(ConfigManager.BossConfig bc) {
        String skillKey = findSkillKey(bc, "charged-explosion", "creeper-blast", "blast");
        return skillKey == null ? null : bc.skills.get(skillKey);
    }

    private double skillNumber(ConfigManager.BossSkillConfig skill, String key,
                               double fallback, double minimum, double maximum) {
        Object value = skill == null ? null : skill.options.get(key);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) return fallback;
        return Math.max(minimum, Math.min(maximum, number.doubleValue()));
    }

    private int skillInteger(ConfigManager.BossSkillConfig skill, String key,
                             int fallback, int minimum, int maximum) {
        Object value = skill == null ? null : skill.options.get(key);
        if (!(value instanceof Number number)) return fallback;
        return Math.max(minimum, Math.min(maximum, number.intValue()));
    }

    private boolean skillBoolean(ConfigManager.BossSkillConfig skill, String key, boolean fallback) {
        Object value = skill == null ? null : skill.options.get(key);
        return value instanceof Boolean enabled ? enabled : fallback;
    }

    private String skillString(ConfigManager.BossSkillConfig skill, String key, String fallback) {
        Object value = skill == null ? null : skill.options.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private ConfigManager.BossSkillConfig skillConfig(ConfigManager.BossConfig boss, String key) {
        return boss == null ? null : boss.skills.get(key);
    }

    private double traitNumber(ConfigManager.BossConfig boss, String trait, String key,
                               double fallback, double minimum, double maximum) {
        Object value = boss == null ? null : boss.traits.getOrDefault(trait, Map.of());
        if (!(value instanceof Map<?, ?> settings) || !(settings.get(key) instanceof Number number)
                || !Double.isFinite(number.doubleValue())) return fallback;
        return Math.max(minimum, Math.min(maximum, number.doubleValue()));
    }

    private boolean traitEnabled(ConfigManager.BossConfig boss, String trait, boolean fallback) {
        Object value = boss == null ? null : boss.traits.getOrDefault(trait, Map.of());
        if (!(value instanceof Map<?, ?> settings)) return fallback;
        Object enabled = settings.get("enabled");
        return enabled instanceof Boolean flag ? flag : fallback;
    }

    private List<String> skillStringList(ConfigManager.BossSkillConfig skill, String key) {
        Object value = skill == null ? null : skill.options.get(key);
        if (!(value instanceof List<?> entries)) return List.of();
        return entries.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private boolean isExtendedBossSkill(String key) {
        return switch (currentBoss.getType()) {
            case ENDERMAN -> key.equals("void-snatch") || key.equals("end-rift");
            case RAVAGER -> key.equals("siege-charge") || key.equals("roar-weakness");
            case PHANTOM -> key.equals("dive-bomb") || key.equals("phantom-minions");
            case GHAST -> key.equals("fireball-volley") || key.equals("fiery-drag");
            case SPIDER -> key.equals("cobweb-trap") || key.equals("venom-pounce");
            case VINDICATOR -> key.equals("axe-spin") || key.equals("axe-throw");
            case WITCH -> key.equals("potion-rain") || key.equals("disarm-magic");
            case GIANT -> key.equals("ground-slam") || key.equals("boulder-toss");
            default -> false;
        };
    }

    private boolean castExtendedBossSkill(String key, ConfigManager.BossSkillConfig skill, World world,
                                          LivingEntity target, Location bossLoc, Location targetLoc,
                                          Vector direction, double damage, double radius, double velocity,
                                          double spread, int count) {
        return switch (key) {
            case "void-snatch" -> castVoidSnatch(skill, target, targetLoc, damage);
            case "end-rift" -> castEndRift(skill, world, targetLoc);
            case "siege-charge" -> castSiegeCharge(skill, target, direction, damage);
            case "roar-weakness" -> castRavagerRoar(skill, world, bossLoc, damage, radius);
            case "dive-bomb" -> castPhantomDive(skill, target, targetLoc, direction, damage, radius, velocity);
            case "phantom-minions" -> castPhantomMinions(skill, world, bossLoc);
            case "fireball-volley" -> currentBoss instanceof Ghast
                ? castGhastFireball(skill, world, bossLoc, target, targetLoc, direction, damage, radius, velocity)
                : false;
            case "fiery-drag" -> castFieryDrag(skill, world, bossLoc, damage, radius);
            case "cobweb-trap" -> castCobwebTrap(skill, world, bossLoc, targetLoc, direction, damage, radius, velocity);
            case "venom-pounce" -> castVenomPounce(skill, target, targetLoc, direction, damage, radius, velocity);
            case "axe-spin" -> castAxeSpin(skill, target, world, radius, damage);
            case "axe-throw" -> castAxeThrow(skill, target, direction, damage, velocity);
            case "potion-rain" -> castPotionRain(skill, world, bossLoc, targetLoc, direction, damage, radius, count);
            case "disarm-magic" -> castDisarm(skill, world, bossLoc, targetLoc, direction, damage, radius, velocity);
            case "ground-slam" -> castGiantGroundSlam(skill, world, bossLoc, targetLoc, direction, damage, radius);
            case "boulder-toss" -> castBoulderToss(skill, world, bossLoc, targetLoc, direction, damage, radius, velocity);
            default -> false;
        };
    }

    private void trackSkillTask(BukkitTask task) {
        if (task != null) bossSkillTasks.add(task);
    }

    private boolean castVoidSnatch(ConfigManager.BossSkillConfig skill, LivingEntity target,
                                   Location targetLoc, double damage) {
        if (!(target instanceof Player)) return false;
        Vector facing = targetLoc.getDirection().setY(0.0);
        if (facing.lengthSquared() < 0.0001) facing = new Vector(0.0, 0.0, 1.0);
        Location behind = targetLoc.clone().subtract(facing.normalize().multiply(
            skillNumber(skill, "behind-distance", 1.5, 0.5, 4.0)));
        behind.setY(targetLoc.getY());
        if (!behind.getBlock().isPassable() || !behind.clone().add(0.0, 1.0, 0.0).getBlock().isPassable()) {
            behind = currentBoss.getLocation().clone();
        }
        if (!currentBoss.teleport(behind)) return false;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 9, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, true, true));
        target.damage(damage, currentBoss);
        currentBoss.getWorld().spawnParticle(Particle.PORTAL, behind.clone().add(0.0, 1.0, 0.0),
            28, 0.45, 0.65, 0.45, 0.2);
        currentBoss.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        return true;
    }

    private boolean castEndRift(ConfigManager.BossSkillConfig skill, World world, Location center) {
        int length = skillInteger(skill, "length", 6, 2, 12);
        int halfWidth = skillInteger(skill, "half-width", 1, 0, 2);
        int warningTicks = skillInteger(skill, "warning-ticks", 30, 10, 100);
        world.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.7f);
        BukkitRunnable rift = new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!bossActive || currentBoss == null || currentBoss.isDead()) {
                    bossSkillTasks.remove(this);
                    cancel();
                    return;
                }
                for (int offset = -length; offset <= length; offset++) {
                    for (int side = -halfWidth; side <= halfWidth; side++) {
                        world.spawnParticle(Particle.DRAGON_BREATH,
                            center.clone().add(offset, 0.12, side), 2, 0.08, 0.02, 0.08, 0.0);
                        world.spawnParticle(Particle.PORTAL,
                            center.clone().add(side, 0.12, offset), 1, 0.05, 0.02, 0.05, 0.0);
                    }
                }
                if (++tick < warningTicks) return;
                for (Entity entity : world.getNearbyEntities(center, length + 1.0, 2.5, length + 1.0)) {
                    if (!(entity instanceof Player player) || player.isDead()) continue;
                    double dx = Math.abs(player.getLocation().getX() - center.getX());
                    double dz = Math.abs(player.getLocation().getZ() - center.getZ());
                    boolean inCross = (dx <= halfWidth + 0.5 && dz <= length)
                        || (dz <= halfWidth + 0.5 && dx <= length);
                    if (!inCross || Math.abs(player.getLocation().getY() - center.getY()) > 2.0) continue;
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 4, 0, false, true, true));
                    player.setVelocity(new Vector(player.getVelocity().getX(), 1.55, player.getVelocity().getZ()));
                }
                bossSkillTasks.remove(this);
                cancel();
            }
        };
        trackSkillTask(rift.runTaskTimer(plugin, 0L, 1L));
        return true;
    }

    private boolean castSiegeCharge(ConfigManager.BossSkillConfig skill, LivingEntity target,
                                   Vector initialDirection, double damage) {
        Vector chargeDirection = initialDirection.clone().setY(0.0);
        if (chargeDirection.lengthSquared() < 0.0001) return false;
        chargeDirection.normalize();
        int durationTicks = skillInteger(skill, "duration-seconds", 3, 1, 6) * 20;
        int maxBlocks = skillInteger(skill, "max-break-blocks", 12, 0, 40);
        double speed = skillNumber(skill, "charge-speed", 1.15, 0.4, 2.0);
        List<Material> breakable = skillStringList(skill, "breakable-blocks").stream()
            .map(Material::matchMaterial).filter(Objects::nonNull).toList();
        Set<UUID> hitEntities = new HashSet<>();
        BukkitRunnable charge = new BukkitRunnable() {
            private int ticks;
            private int broken;

            @Override
            public void run() {
                if (!bossActive || currentBoss == null || currentBoss.isDead() || ticks++ >= durationTicks) {
                    bossSkillTasks.remove(this);
                    cancel();
                    return;
                }
                Location next = currentBoss.getLocation().add(chargeDirection.clone().multiply(0.7));
                Block obstacle = next.clone().add(0.0, 0.8, 0.0).getBlock();
                if (!obstacle.isPassable()) {
                    boolean canBreak = skillBoolean(skill, "break-blocks", false)
                        && broken < maxBlocks && breakable.contains(obstacle.getType())
                        && !obstacle.isLiquid() && !isBossBlockProtected(obstacle);
                    if (!canBreak) {
                        bossSkillTasks.remove(this);
                        cancel();
                        return;
                    }
                    Material material = obstacle.getType();
                    FallingBlock falling = obstacle.getWorld().spawnFallingBlock(
                        obstacle.getLocation().add(0.5, 0.0, 0.5), material.createBlockData());
                    falling.setDropItem(true);
                    falling.setHurtEntities(false);
                    falling.setVelocity(chargeDirection.clone().multiply(0.35).setY(0.35));
                    bossTerrainEntities.add(falling.getUniqueId());
                    bossTerrainDrops.put(falling.getUniqueId(), true);
                    activeBossProjectiles.add(falling);
                    obstacle.setType(Material.AIR, false);
                    broken++;
                }
                currentBoss.setVelocity(chargeDirection.clone().multiply(speed)
                    .setY(currentBoss.getVelocity().getY()));
                currentBoss.getWorld().spawnParticle(Particle.CLOUD,
                    currentBoss.getLocation(), 8, 0.45, 0.2, 0.45, 0.03);
                for (Entity entity : currentBoss.getWorld().getNearbyEntities(
                        currentBoss.getLocation(), 1.8, 1.4, 1.8)) {
                    if (!(entity instanceof Player victim) || victim.isDead()
                            || !hitEntities.add(victim.getUniqueId())) continue;
                    victim.damage(damage, currentBoss);
                    Vector knockback = victim.getLocation().toVector()
                        .subtract(currentBoss.getLocation().toVector()).setY(0.55);
                    if (knockback.lengthSquared() > 0.0001) victim.setVelocity(
                        knockback.normalize().multiply(skillNumber(skill, "knockback", 1.8, 0.5, 3.0)));
                }
            }
        };
        currentBoss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 4, false, true, true));
        trackSkillTask(charge.runTaskTimer(plugin, 0L, 1L));
        return true;
    }

    private boolean castRavagerRoar(ConfigManager.BossSkillConfig skill, World world,
                                    Location center, double damage, double radius) {
        radius = skillNumber(skill, "radius", 8.0, 2.0, 20.0);
        world.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        double range = radius;
        for (Entity entity : world.getNearbyEntities(center, range, range, range)) {
            if (!(entity instanceof Player player) || player.isDead()
                    || player.getLocation().distanceSquared(center) > range * range) continue;
            Vector push = player.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < 0.0001) push = new Vector(0.0, 0.0, 1.0);
            player.setVelocity(push.normalize().multiply(skillNumber(skill, "knockback", 1.2, 0.3, 2.5))
                .setY(0.35));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, false, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1, false, true, true));
        }
        world.spawnParticle(Particle.SONIC_BOOM, center.clone().add(0.0, 1.0, 0.0), 1);
        return true;
    }

    private boolean castPhantomDive(ConfigManager.BossSkillConfig skill, LivingEntity target,
                                    Location targetLoc, Vector direction, double damage,
                                    double radius, double velocity) {
        Vector dive = direction.clone();
        if (dive.lengthSquared() < 0.0001) dive = new Vector(0.0, -0.6, 0.0);
        dive.normalize().multiply(velocity);
        dive.setY(Math.min(-0.45, dive.getY() - 0.35));
        currentBoss.setVelocity(dive);
        revealPhantom(3000L);
        BukkitRunnable impact = new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!bossActive || currentBoss == null || currentBoss.isDead() || ticks++ >= 50) {
                    bossSkillTasks.remove(this);
                    cancel();
                    return;
                }
                Location at = currentBoss.getLocation();
                if (!currentBoss.isOnGround()) return;
                at.getWorld().spawnParticle(Particle.EXPLOSION, at, 2, 0.4, 0.2, 0.4, 0.0);
                at.getWorld().createExplosion(at, 0.0F, false, false, currentBoss);
                double effectRadius = skillNumber(skill, "blindness-radius", 5.0, 1.0, 12.0);
                for (Entity entity : at.getWorld().getNearbyEntities(
                        at, effectRadius, effectRadius, effectRadius)) {
                    if (entity instanceof Player player
                            && player.getLocation().distanceSquared(at) <= effectRadius * effectRadius) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0,
                            false, true, true));
                        if (player.equals(target)
                                && player.getLocation().distanceSquared(at) <= radius * radius) {
                            player.damage(damage, currentBoss);
                        }
                    }
                }
                bossSkillTasks.remove(this);
                cancel();
            }
        };
        trackSkillTask(impact.runTaskTimer(plugin, 1L, 1L));
        return true;
    }

    private void revealPhantom(long milliseconds) {
        if (currentBoss == null || currentBoss.getType() != EntityType.PHANTOM) return;
        phantomRevealedUntil.put(currentBoss.getUniqueId(), System.currentTimeMillis() + milliseconds);
        currentBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private boolean castPhantomMinions(ConfigManager.BossSkillConfig skill, World world, Location center) {
        int minimum = skillInteger(skill, "minions-min", 3, 1, 8);
        int maximum = skillInteger(skill, "minions-max", 5, minimum, 8);
        currentBoss.setVelocity(new Vector(currentBoss.getVelocity().getX(), 0.7,
            currentBoss.getVelocity().getZ()));
        int amount = random.nextInt(minimum, maximum + 1);
        for (int i = 0; i < amount; i++) {
            double angle = Math.PI * 2.0 * i / amount;
            Location spawn = center.clone().add(Math.cos(angle) * 2.0, 2.0, Math.sin(angle) * 2.0);
            Phantom minion = world.spawn(spawn, Phantom.class);
            minion.setSize(1);
            minion.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1, false, true, true));
            minion.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 0, false, true, true));
            bossSummons.add(minion.getUniqueId());
        }
        return true;
    }

    private boolean castGhastFireball(ConfigManager.BossSkillConfig skill, World world, Location bossLoc,
                                      LivingEntity target, Location targetLoc, Vector direction,
                                      double damage, double radius, double velocity) {
        Vector aim = targetLoc.toVector().subtract(bossLoc.clone().add(0.0, 1.0, 0.0).toVector());
        if (aim.lengthSquared() < 0.0001) aim = direction;
        aim.normalize().multiply(velocity);
        Location launch = bossLoc.clone().add(0.0, 1.0, 0.0);
        LargeFireball fireball = world.spawn(launch, LargeFireball.class);
        fireball.setShooter(currentBoss);
        fireball.setDirection(aim);
        fireball.setVelocity(aim);
        fireball.setYield(0.0F);
        fireball.setIsIncendiary(false);
        registerBossProjectile(fireball, damage, radius, false);
        splitFireballs.put(fireball.getUniqueId(), launch.clone());
        return true;
    }

    private void splitGhastFireball(Location location, Vector incoming) {
        if (currentBoss == null || currentBoss.isDead() || currentBoss.getType() != EntityType.GHAST) return;
        ConfigManager.BossSkillConfig skill = skillConfig(activeBossConfig, "fireball-volley");
        int count = skillInteger(skill, "split-count", 6, 5, 8);
        double velocity = skillNumber(skill, "split-velocity", 1.2, 0.4, 3.0);
        double damage = skillNumber(skill, "damage", 8.0, 0.0, 1024.0) * Math.max(1, currentBossLevel);
        double radius = skillNumber(skill, "radius", 5.0, 1.0, 64.0);
        Vector base = incoming.lengthSquared() < 0.0001 ? currentBoss.getLocation().getDirection() : incoming.clone();
        base.normalize();
        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) / 2.0) * skillNumber(skill, "split-spread", 0.3, 0.05, 1.0);
            Vector shot = base.clone().rotateAroundY(offset);
            shot.setY(Math.max(-0.4, Math.min(0.5,
                shot.getY() + (random.nextDouble() - 0.5) * 0.25))).normalize().multiply(velocity);
            SmallFireball small = location.getWorld().spawn(location, SmallFireball.class);
            small.setShooter(currentBoss);
            small.setDirection(shot);
            small.setVelocity(shot);
            small.setIsIncendiary(false);
            registerBossProjectile(small, damage, radius, false);
        }
    }

    private boolean castFieryDrag(ConfigManager.BossSkillConfig skill, World world, Location center,
                                  double damage, double radius) {
        double range = skillNumber(skill, "radius", 10.0, 2.0, 24.0);
        for (Entity entity : world.getNearbyEntities(center, range, range, range)) {
            if (!(entity instanceof Player player) || player.isDead()
                    || player.getLocation().distanceSquared(center) > range * range) continue;
            Vector pull = center.toVector().subtract(player.getLocation().toVector());
            if (pull.lengthSquared() > 0.0001) player.setVelocity(pull.normalize().multiply(
                skillNumber(skill, "pull-speed", 0.8, 0.2, 1.5)).setY(0.2));
            if (skillBoolean(skill, "terrain-enabled", true)) {
                Block feet = player.getLocation().getBlock();
                Block below = feet.getRelative(0, -1, 0);
                Material fireFloor = Material.matchMaterial(skillString(skill, "terrain-material", "MAGMA_BLOCK"));
                if (fireFloor != Material.MAGMA_BLOCK && fireFloor != Material.FIRE) {
                    fireFloor = Material.MAGMA_BLOCK;
                }
                Block hazard = fireFloor == Material.FIRE ? feet : below;
                List<String> configuredReplaceables = skillStringList(skill, "replaceable-blocks");
                Set<Material> defaultReplaceables = EnumSet.of(Material.DIRT, Material.GRASS_BLOCK, Material.STONE,
                    Material.COBBLESTONE, Material.DEEPSLATE, Material.NETHERRACK, Material.SAND,
                    Material.RED_SAND, Material.GRAVEL);
                boolean replaceable = configuredReplaceables.isEmpty()
                    ? defaultReplaceables.contains(hazard.getType())
                    : configuredReplaceables.stream().anyMatch(name -> name.equalsIgnoreCase(hazard.getType().name()));
                if (hazard.getType().isAir() || hazard.isPassable()
                        || fireFloor == Material.MAGMA_BLOCK && replaceable
                            && !(hazard.getState() instanceof org.bukkit.block.TileState)) {
                    placeTemporaryBlock(hazard, fireFloor,
                        skillInteger(skill, "terrain-duration-seconds", 5, 1, 30));
                }
            }
        }
        world.spawnParticle(Particle.FLAME, center, 24, radius * 0.4, 0.3, radius * 0.4, 0.02);
        return true;
    }

    private boolean castCobwebTrap(ConfigManager.BossSkillConfig skill, World world, Location bossLoc,
                                   Location targetLoc, Vector direction, double damage, double radius,
                                   double velocity) {
        Snowball projectile = world.spawn(bossLoc.clone().add(0.0, 1.0, 0.0), Snowball.class);
        projectile.setShooter(currentBoss);
        Vector shot = targetLoc.toVector().subtract(projectile.getLocation().toVector());
        if (shot.lengthSquared() < 0.0001) shot = direction;
        projectile.setVelocity(shot.normalize().multiply(velocity));
        webTrapProjectiles.add(projectile.getUniqueId());
        activeBossProjectiles.add(projectile);
        return true;
    }

    private void placeTemporaryWebTrap(Location impact, ConfigManager.BossSkillConfig skill) {
        int radius = skillInteger(skill, "trap-radius", 1, 1, 2);
        int duration = skillInteger(skill, "trap-duration-seconds", 6, 1, 30);
        Block center = impact.getBlock();
        if (center.getType().isAir() || center.isPassable()) center = center.getRelative(0, -1, 0);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = center.getRelative(x, 1, z);
                if (block.getType().isAir() || block.isPassable()) {
                    placeTemporaryBlock(block, Material.COBWEB, duration);
                }
            }
        }
    }

    private void placeTemporaryBlock(Block block, Material material, int durationSeconds) {
        if (isBossBlockProtected(block)) return;
        BlockPosition key = new BlockPosition(block.getWorld().getUID(),
            block.getX(), block.getY(), block.getZ());
        boolean replaceableFloor = material == Material.MAGMA_BLOCK && block.getType().isSolid()
            && !block.getType().isAir() && !(block.getState() instanceof org.bukkit.block.TileState)
            && block.getType() != Material.BEDROCK && block.getType() != Material.BARRIER;
        if (temporaryBlocks.containsKey(key) || (!block.getType().isAir() && !replaceableFloor)) return;
        BlockState original = block.getState();
        block.setType(material, false);
        temporaryBlocks.put(key, new TemporaryBlock(original, block.getBlockData().clone()));
        BukkitTask expiry = Bukkit.getScheduler().runTaskLater(plugin, () -> restoreTemporaryBlock(key), durationSeconds * 20L);
        trackSkillTask(expiry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBreakTemporaryBlock(BlockBreakEvent event) {
        markPlayerModifiedTemporaryBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPlaceTemporaryBlock(BlockPlaceEvent event) {
        markPlayerModifiedTemporaryBlock(event.getBlockPlaced());
    }

    private void markPlayerModifiedTemporaryBlock(Block block) {
        BlockPosition key = new BlockPosition(block.getWorld().getUID(),
            block.getX(), block.getY(), block.getZ());
        if (temporaryBlocks.containsKey(key)) playerModifiedTemporaryBlocks.add(key);
    }

    private boolean isBossBlockProtected(Block block) {
        return plugin.getSpawnProtectionManager() != null
            && plugin.getSpawnProtectionManager().isProtected(block.getLocation());
    }

    private void restoreTemporaryBlock(BlockPosition key) {
        TemporaryBlock temporary = temporaryBlocks.remove(key);
        if (temporary == null) return;
        World world = Bukkit.getWorld(key.worldId());
        if (world == null) return;
        Block block = world.getBlockAt(key.x(), key.y(), key.z());
        boolean playerModified = playerModifiedTemporaryBlocks.remove(key);
        if (!playerModified && temporary.placedData().matches(block.getBlockData())) {
            temporary.original().update(true, false);
        }
    }

    private void detonateBoulder(Location impact) {
        if (currentBoss == null || currentBoss.isDead()) return;
        ConfigManager.BossSkillConfig skill = skillConfig(activeBossConfig, "boulder-toss");
        double radius = skill == null ? 3.0 : safeSkillValue(skill.radius, 3.0, 1.0, 64.0);
        double damage = skill == null ? 12.0 : safeSkillValue(skill.damage, 12.0, 0.0, 1024.0)
            * Math.max(1, currentBossLevel);
        impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 2);
        impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
        impact.getWorld().createExplosion(impact, 0.0F, false, false, currentBoss);
        for (Entity entity : impact.getWorld().getNearbyEntities(impact, radius, radius, radius)) {
            if (entity instanceof Player victim && !victim.isDead()
                    && victim.getLocation().distanceSquared(impact) <= radius * radius) {
                victim.damage(damage, currentBoss);
            }
        }
    }

    private boolean castVenomPounce(ConfigManager.BossSkillConfig skill, LivingEntity target,
                                    Location targetLoc, Vector direction, double damage,
                                    double radius, double velocity) {
        Vector leap = direction.clone().normalize().multiply(velocity);
        leap.setY(skillNumber(skill, "jump-y", 0.75, 0.3, 1.5));
        currentBoss.setVelocity(leap);
        BukkitRunnable landing = new BukkitRunnable() {
            private int ticks;
            private boolean hit;

            @Override
            public void run() {
                if (!bossActive || currentBoss == null || currentBoss.isDead() || hit || ticks++ >= 50) {
                    bossSkillTasks.remove(this);
                    cancel();
                    return;
                }
                        if (!currentBoss.isOnGround()) return;
                for (Entity entity : currentBoss.getWorld().getNearbyEntities(currentBoss.getLocation(),
                        radius, radius, radius)) {
                    if (!(entity instanceof Player player) || player.isDead()
                            || player.getLocation().distanceSquared(currentBoss.getLocation()) > radius * radius) continue;
                    player.damage(damage, currentBoss);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 1, false, true, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 120, 2, false, true, true));
                    hit = true;
                    break;
                }
                if (hit || currentBoss.isOnGround() && ticks > 4 || ticks >= 45) {
                    bossSkillTasks.remove(this);
                    cancel();
                }
            }
        };
        trackSkillTask(landing.runTaskTimer(plugin, 1L, 1L));
        return true;
    }

    private boolean castAxeSpin(ConfigManager.BossSkillConfig skill, LivingEntity target, World world,
                                double radius, double damage) {
        int durationTicks = skillInteger(skill, "duration-seconds", 3, 1, 6) * 20;
        long hitCooldownMs = skillInteger(skill, "hit-interval-ticks", 10, 5, 40) * 50L;
        BukkitRunnable spin = new BukkitRunnable() {
            private int ticks;
            private float yaw = currentBoss.getLocation().getYaw();

            @Override
            public void run() {
                if (!bossActive || currentBoss == null || currentBoss.isDead() || ticks++ >= durationTicks) {
                    bossSkillTasks.remove(this);
                    cancel();
                    return;
                }
                yaw += 45.0F;
                Location at = currentBoss.getLocation();
                currentBoss.setRotation(yaw, at.getPitch());
                Vector approach = target.getLocation().toVector().subtract(at.toVector()).setY(0.0);
                if (approach.lengthSquared() > 1.0) currentBoss.setVelocity(approach.normalize().multiply(0.22));
                world.spawnParticle(Particle.SWEEP_ATTACK, at.clone().add(0.0, 1.0, 0.0), 3,
                    radius * 0.4, 0.35, radius * 0.4, 0.0);
                for (Entity entity : world.getNearbyEntities(at, radius, 1.5, radius)) {
                    if (!(entity instanceof Player player) || player.isDead()
                            || player.getLocation().distanceSquared(at) > radius * radius
                            || spinDamageCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()) continue;
                    player.damage(damage, currentBoss);
                    if (player.isBlocking()) {
                        player.setCooldown(Material.SHIELD, skillInteger(skill, "shield-cooldown-ticks", 100, 20, 200));
                    }
                    spinDamageCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + hitCooldownMs);
                }
            }
        };
        trackSkillTask(spin.runTaskTimer(plugin, 0L, 1L));
        return true;
    }

    private boolean castAxeThrow(ConfigManager.BossSkillConfig skill, LivingEntity target,
                                 Vector direction, double damage, double velocity) {
        LivingEntity owner = currentBoss;
        org.bukkit.inventory.EntityEquipment equipment = owner.getEquipment();
        ItemStack originalWeapon = equipment == null || equipment.getItemInMainHand().getType().isAir()
            ? null : equipment.getItemInMainHand().clone();
        if (originalWeapon != null) equipment.setItemInMainHand(new ItemStack(Material.AIR));
        ArmorStand axe = currentBoss.getWorld().spawn(currentBoss.getLocation().add(0.0, 1.0, 0.0),
            ArmorStand.class);
        axe.setVisible(false);
        axe.setArms(true);
        axe.setMarker(true);
        axe.setGravity(false);
        axe.setInvulnerable(true);
        axe.setSilent(true);
        axe.setCollidable(false);
        axe.getEquipment().setItemInMainHand(new ItemStack(
            Material.matchMaterial(skillString(skill, "axe-material", "IRON_AXE")) == null
                ? Material.IRON_AXE
                : Material.matchMaterial(skillString(skill, "axe-material", "IRON_AXE"))));
        bossSummons.add(axe.getUniqueId());
        Vector outbound = direction.clone().normalize().multiply(velocity);
        int maxDistance = skillInteger(skill, "max-distance", 16, 6, 32);
        int maxTicks = Math.max(6, Math.min(80, (int) Math.ceil(maxDistance / Math.max(0.2, velocity))));
        BukkitRunnable flight = new BukkitRunnable() {
            private int ticks;
            private boolean returning;
            private final Set<UUID> outwardHits = new HashSet<>();
            private final Set<UUID> returnHits = new HashSet<>();

            private void restoreWeapon() {
                if (originalWeapon != null && owner.isValid() && !owner.isDead()
                        && owner.getEquipment() != null
                        && owner.getEquipment().getItemInMainHand().getType().isAir()) {
                    owner.getEquipment().setItemInMainHand(originalWeapon);
                }
            }

            @Override
            public void run() {
                if (!bossActive || currentBoss == null || currentBoss.isDead() || !axe.isValid()
                        || ticks++ >= maxTicks * 3) {
                    restoreWeapon();
                    bossSummons.remove(axe.getUniqueId());
                    axe.remove();
                    bossSkillTasks.remove(this);
                    cancel();
                    return;
                }
                if (!returning && ticks >= maxTicks) returning = true;
                Vector travel;
                if (returning) {
                    travel = currentBoss.getLocation().toVector().subtract(axe.getLocation().toVector());
                    if (travel.lengthSquared() < 1.0) {
                        restoreWeapon();
                        bossSummons.remove(axe.getUniqueId());
                        axe.remove();
                        bossSkillTasks.remove(this);
                        cancel();
                        return;
                    }
                    travel.normalize().multiply(velocity);
                } else {
                    travel = outbound;
                }
                Location next = axe.getLocation().add(travel);
                if (!next.getBlock().isPassable()) returning = true;
                axe.teleport(next);
                axe.setRightArmPose(new EulerAngle(ticks * 0.55, 0.0, 0.0));
                for (Entity entity : axe.getWorld().getNearbyEntities(next, 0.8, 0.8, 0.8)) {
                    if (!(entity instanceof Player player) || player.isDead()) continue;
                    Set<UUID> passHits = returning ? returnHits : outwardHits;
                    if (passHits.add(player.getUniqueId())) player.damage(damage, currentBoss);
                }
            }
        };
        trackSkillTask(flight.runTaskTimer(plugin, 0L, 1L));
        return true;
    }

    private boolean castPotionRain(ConfigManager.BossSkillConfig skill, World world, Location bossLoc,
                                  Location targetLoc, Vector direction, double damage, double radius, int count) {
        int potionCount = skillInteger(skill, "potion-count", 8, 3, 16);
        for (int i = 0; i < potionCount; i++) {
            ThrownPotion potion = world.spawn(bossLoc.clone().add(0.0, 1.2, 0.0), ThrownPotion.class);
            ItemStack potionItem = new ItemStack(Material.SPLASH_POTION);
            org.bukkit.inventory.meta.PotionMeta meta =
                (org.bukkit.inventory.meta.PotionMeta) potionItem.getItemMeta();
            meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0), true);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 0), true);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1), true);
            potionItem.setItemMeta(meta);
            potion.setItem(potionItem);
            potion.setShooter(currentBoss);
            Vector launch = direction.clone().rotateAroundY((random.nextDouble() - 0.5)
                * skillNumber(skill, "spread", 0.8, 0.0, Math.PI))
                .setY(skillNumber(skill, "launch-y", 0.55, 0.1, 1.5))
                .normalize().multiply(skillNumber(skill, "velocity", 1.0, 0.2, 2.0));
            potion.setVelocity(launch);
            registerBossProjectile(potion, damage, radius, true);
        }
        return true;
    }

    private boolean castDisarm(ConfigManager.BossSkillConfig skill, World world, Location bossLoc,
                               Location targetLoc, Vector direction, double damage, double radius,
                               double velocity) {
        Snowball projectile = world.spawn(bossLoc.clone().add(0.0, 1.0, 0.0), Snowball.class);
        projectile.setShooter(currentBoss);
        Vector shot = targetLoc.toVector().subtract(projectile.getLocation().toVector());
        if (shot.lengthSquared() < 0.0001) shot = direction;
        projectile.setVelocity(shot.normalize().multiply(velocity));
        disarmProjectiles.add(projectile.getUniqueId());
        registerBossProjectile(projectile, damage, radius, false);
        return true;
    }

    private void disarmPlayer(Player player, ConfigManager.BossSkillConfig skill) {
        int selected = player.getInventory().getHeldItemSlot();
        int replacement = -1;
        for (int slot = 0; slot < 9; slot++) {
            if (slot != selected && player.getInventory().getItem(slot) == null) {
                replacement = slot;
                break;
            }
        }
        if (replacement < 0) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (slot != selected && item != null && item.getType() == Material.CARROT) {
                    replacement = slot;
                    break;
                }
            }
        }
        if (replacement >= 0) player.getInventory().setHeldItemSlot(replacement);
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE,
            skillInteger(skill, "fatigue-ticks", 80, 20, 200), 4, false, true, true));
    }

    private boolean castGiantGroundSlam(ConfigManager.BossSkillConfig skill, World world, Location bossLoc,
                                        Location targetLoc, Vector direction, double damage, double radius) {
        Vector path = direction.clone().setY(0.0);
        if (path.lengthSquared() < 0.0001) return false;
        path.normalize();
        int length = skillInteger(skill, "line-length", 15, 4, 24);
        Set<UUID> hitPlayers = new HashSet<>();
        for (int step = 1; step <= length; step++) {
            Location point = bossLoc.clone().add(path.clone().multiply(step));
            point.setY(world.getHighestBlockYAt(point) + 0.15);
            world.spawnParticle(Particle.EXPLOSION, point, 1, 0.15, 0.05, 0.15, 0.0);
            world.spawnParticle(Particle.BLOCK, point, 8, 0.3, 0.15, 0.3, 0.0,
                point.clone().add(0.0, -0.1, 0.0).getBlock().getBlockData());
            for (Entity entity : world.getNearbyEntities(point, 1.25, 1.0, 1.25)) {
                if (!(entity instanceof Player victim) || victim.isDead()
                        || victim.getLocation().distanceSquared(point) > 2.25
                        || !hitPlayers.add(victim.getUniqueId())) continue;
                victim.damage(damage, currentBoss);
                victim.setVelocity(new Vector(path.getX() * 0.5, 0.85, path.getZ() * 0.5));
            }
        }
        world.playSound(bossLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
        return true;
    }

    private boolean castBoulderToss(ConfigManager.BossSkillConfig skill, World world, Location bossLoc,
                                    Location targetLoc, Vector direction, double damage, double radius,
                                    double velocity) {
        FallingBlock boulder = world.spawnFallingBlock(
            bossLoc.clone().add(0.0, 3.0, 0.0), Material.COBBLESTONE.createBlockData());
        boulder.setDropItem(false);
        boulder.setHurtEntities(false);
        boulder.setVelocity(direction.clone().normalize().multiply(velocity));
        bossTerrainEntities.add(boulder.getUniqueId());
        bossTerrainDrops.put(boulder.getUniqueId(), false);
        bossBoulders.add(boulder.getUniqueId());
        activeBossProjectiles.add(boulder);
        BukkitRunnable impact = new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!bossActive || currentBoss == null || currentBoss.isDead() || !boulder.isValid()
                        || ticks++ >= 100) {
                    bossTerrainEntities.remove(boulder.getUniqueId());
                    activeBossProjectiles.remove(boulder);
                    bossTerrainDrops.remove(boulder.getUniqueId());
                    bossBoulders.remove(boulder.getUniqueId());
                    boulder.remove();
                    bossSkillTasks.remove(this);
                    cancel();
                    return;
                }
                if (boulder.isOnGround() || boulder.getVelocity().lengthSquared() < 0.03) {
                    Location impact = boulder.getLocation();
                    detonateBoulder(impact);
                    bossTerrainEntities.remove(boulder.getUniqueId());
                    activeBossProjectiles.remove(boulder);
                    bossTerrainDrops.remove(boulder.getUniqueId());
                    bossBoulders.remove(boulder.getUniqueId());
                    boulder.remove();
                    bossSkillTasks.remove(this);
                    cancel();
                }
            }
        };
        trackSkillTask(impact.runTaskTimer(plugin, 1L, 1L));
        return true;
    }

    private boolean useBossSkill(ConfigManager.BossConfig bc, LivingEntity target) {
        if (currentBoss == null || currentBoss.isDead() || bc == null || target == null || bc.skills == null || bc.skills.isEmpty()) {
            return false;
        }

        List<String> candidates = new ArrayList<>();
        switch (currentBoss.getType()) {
            case SKELETON -> candidates.add(findSkillKey(bc, "arrow-barrage", "skeleton-arrow", "arrow-fan", "fire-arrow-fan"));
            case ENDER_DRAGON -> candidates.add(findSkillKey(bc, "fireball-volley", "dragon-fireball", "ender-dragon-fireball", "fireball-fan"));
            case GIANT -> {
                candidates.add(findSkillKey(bc, "ground-slam", "stomp", "giant-stomp"));
                candidates.add(findSkillKey(bc, "boulder-toss"));
            }
            case CREEPER -> candidates.add(findSkillKey(bc, "charged-explosion", "creeper-blast", "blast"));
            case WITHER -> candidates.add(findSkillKey(bc, "skull-volley", "wither-skull", "skull"));
            case SPIDER -> {
                candidates.add(findSkillKey(bc, "cobweb-trap"));
                candidates.add(findSkillKey(bc, "venom-pounce"));
                candidates.add(findSkillKey(bc, "venom-web", "spider-web", "spider-poison", "web"));
            }
            case RAVAGER -> {
                candidates.add(findSkillKey(bc, "siege-charge", "charge", "ravager-charge"));
                candidates.add(findSkillKey(bc, "roar-weakness"));
            }
            case PHANTOM -> {
                candidates.add(findSkillKey(bc, "dive-bomb", "phantom-dive", "dive"));
                candidates.add(findSkillKey(bc, "phantom-minions"));
            }
            case WITCH -> {
                candidates.add(findSkillKey(bc, "potion-rain", "potion-volley", "witch-potion", "potion"));
                candidates.add(findSkillKey(bc, "disarm-magic"));
            }
            case ENDERMAN -> {
                candidates.add(findSkillKey(bc, "void-snatch", "shadow-strike", "enderman-teleport", "teleport"));
                candidates.add(findSkillKey(bc, "end-rift"));
            }
            case VINDICATOR -> {
                candidates.add(findSkillKey(bc, "axe-spin", "vindicator-shockwave", "vindicator_shockwave", "shockwave"));
                candidates.add(findSkillKey(bc, "axe-throw"));
            }
            case ZOMBIE -> candidates.add(findSkillKey(bc, "ground-slam", "zombie-slam", "slam"));
            case GHAST -> {
                candidates.add(findSkillKey(bc, "fireball-volley", "ghast-fireball", "fireball"));
                candidates.add(findSkillKey(bc, "fiery-drag"));
            }
            default -> { }
        }

        long now = System.currentTimeMillis();
        String skillKey = null;
        ConfigManager.BossSkillConfig skill = null;
        for (String candidate : candidates) {
            if (candidate == null) continue;
            ConfigManager.BossSkillConfig option = bc.skills.get(candidate);
            if (option == null || !option.enabled) continue;
            Long lastUse = bossSkillCooldowns.get(currentBoss.getUniqueId() + ":" + candidate);
            if (lastUse != null && now - lastUse < (long) (safeSkillValue(
                    option.cooldownSeconds, 8.0, 1.0, 86400.0) * 1000.0)) continue;
            skillKey = candidate;
            skill = option;
            break;
        }
        if (skillKey == null || skill == null) return false;

        World world = currentBoss.getWorld();
        if (world == null) return false;

        double multiply = safeSkillValue(skill.multiply, 1.0, 0.0, 40.0);
        if (multiply <= 0.0) return false;
        int castCount = (int) Math.max(1, Math.min(40, Math.ceil(currentBossLevel * multiply)));
        double skillDamage = safeSkillValue(skill.damage, 0.0, 0.0, 1024.0)
            * Math.max(1, currentBossLevel);
        double skillRadius = safeSkillValue(skill.radius, 1.0, 1.0, 64.0);
        double skillVelocity = safeSkillValue(skill.velocity, 1.0, 0.2, 4.0);
        double skillSpread = safeSkillValue(skill.spread, 0.0, 0.0, Math.PI);

        Location bossLoc = currentBoss.getLocation();
        Location targetLoc = target.getLocation();
        Vector dir = targetLoc.toVector().subtract(bossLoc.toVector());
        if (dir.lengthSquared() < 0.0001) dir = currentBoss.getLocation().getDirection();
        dir.normalize();
        if (isExtendedBossSkill(skillKey)) {
            boolean used = castExtendedBossSkill(skillKey, skill, world, target, bossLoc, targetLoc,
                dir, skillDamage, skillRadius, skillVelocity, skillSpread, castCount);
            if (used) completeBossSkillCast(skillKey, target, now);
            return used;
        }

        switch (currentBoss.getType()) {
            case SKELETON -> {
                launchArrowBarrage(world, bossLoc, targetLoc, castCount, skillDamage,
                    skillRadius, skillVelocity, skillSpread);
                return completeBossSkillCast(skillKey, target, now);
            }
            case ENDER_DRAGON -> {
                EnderDragon dragon = (EnderDragon) currentBoss;
                dragon.setAware(true);
                dragon.setTarget(target);
                dragon.setPhase(EnderDragon.Phase.STRAFING);
                Vector aim = target.getEyeLocation().toVector()
                    .subtract(bossLoc.clone().add(0.0, 2.0, 0.0).toVector());
                if (aim.lengthSquared() < 0.0001) aim = currentBoss.getLocation().getDirection();
                aim.normalize();
                Location launchLocation = bossLoc.clone().add(aim.clone().multiply(4.0)).add(0.0, 1.5, 0.0);
                for (int i = 0; i < castCount; i++) {
                    Vector fireballDir = target.getEyeLocation().toVector().subtract(launchLocation.toVector());
                    if (fireballDir.lengthSquared() < 0.0001) fireballDir = aim.clone();
                    if (skillSpread > 0.0) {
                        double offset = (i - (castCount - 1) / 2.0) * skillSpread / Math.max(1, castCount - 1);
                        fireballDir.rotateAroundY(offset);
                        fireballDir.setY(Math.max(-0.7, Math.min(0.9, fireballDir.getY() + (random.nextDouble() - 0.5) * skillSpread * 0.25)));
                    }
                    fireballDir.normalize().multiply(skillVelocity);
                    LargeFireball fireball = world.spawn(launchLocation, LargeFireball.class);
                    fireball.setDirection(fireballDir);
                    fireball.setVelocity(fireballDir);
                    fireball.setYield(0.0F);
                    fireball.setIsIncendiary(false);
                    fireball.setShooter(currentBoss);
                    registerBossProjectile(fireball, skillDamage, skillRadius, false);
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case GIANT -> {
                damageEntitiesInRadius(world, bossLoc, skillRadius, skillDamage);
                for (Entity entity : world.getNearbyEntities(bossLoc, skillRadius, skillRadius, skillRadius)) {
                    if (entity instanceof LivingEntity living && !living.equals(currentBoss)
                            && living.getLocation().distanceSquared(bossLoc) <= skillRadius * skillRadius) {
                        Vector knockback = living.getLocation().toVector().subtract(bossLoc.toVector());
                        if (knockback.lengthSquared() < 0.0001) knockback = new Vector(0.0, 0.0, 1.0);
                        knockback.normalize().setY(0.7);
                        living.setVelocity(knockback.multiply(skillVelocity));
                    }
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case CREEPER -> {
                if (!(currentBoss instanceof Creeper creeper)) return false;
                double explosionPower = safeSkillValue(skill.explosionPower, 3.0, 1.0, 64.0);

                if (creeperChargeTask != null) {
                    creeperChargeTask.cancel();
                    bossSkillTasks.remove(creeperChargeTask);
                }
                creeper.setPowered(true);
                world.strikeLightningEffect(bossLoc);
                damageEntitiesInRadius(world, bossLoc, skillRadius, skillDamage);

                int durationSeconds = Math.max(1, Math.min(600, skill.durationSeconds));
                BukkitRunnable chargeRunnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        bossSkillTasks.remove(this);
                        if (currentBoss == creeper && creeper.isValid() && !creeper.isDead()) {
                            creeper.setPowered(false);
                            creeper.setExplosionRadius((int) Math.round(explosionPower));
                        }
                        if (creeperChargeTask == this) creeperChargeTask = null;
                    }
                };
                creeperChargeTask = chargeRunnable.runTaskLater(plugin, durationSeconds * 20L);
                bossSkillTasks.add(creeperChargeTask);
                return completeBossSkillCast(skillKey, target, now);
            }
            case WITHER -> {
                for (int i = 0; i < castCount; i++) {
                    Vector skullDir = dir.clone();
                    if (skillSpread > 0.0) {
                        skullDir.rotateAroundY((Math.random() - 0.5) * skillSpread * 0.7);
                    }
                    WitherSkull skull = world.spawn(bossLoc.clone().add(0.0, 1.0, 0.0), WitherSkull.class);
                    skull.setCharged(true);
                    skull.setDirection(skullDir.normalize().multiply(skillVelocity));
                    skull.setVelocity(skullDir.normalize().multiply(skillVelocity));
                    skull.setYield(0.0F);
                    skull.setIsIncendiary(false);
                    registerBossProjectile(skull, skillDamage, skillRadius, false);
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case SPIDER -> {
                damageEntitiesInRadius(world, targetLoc, skillRadius, skillDamage);
                for (Entity entity : world.getNearbyEntities(targetLoc, skillRadius, skillRadius, skillRadius)) {
                    if (entity instanceof LivingEntity living && !living.equals(currentBoss)
                            && living.getLocation().distanceSquared(targetLoc) <= skillRadius * skillRadius) {
                        living.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.POISON, 80, Math.max(0, currentBossLevel / 3), false, false));
                    }
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case RAVAGER -> {
                if (target != null) {
                    Vector charge = targetLoc.toVector().subtract(bossLoc.toVector()).normalize();
                    currentBoss.setVelocity(charge.multiply(skillVelocity > 0 ? skillVelocity : 1.3));
                    currentBoss.setRotation((float) Math.toDegrees(Math.atan2(charge.getZ(), charge.getX())), 0.0F);
                }
                for (Entity entity : world.getNearbyEntities(bossLoc, skillRadius, skillRadius, skillRadius)) {
                    if (entity instanceof LivingEntity living && !entity.equals(currentBoss)
                            && living.getLocation().distanceSquared(bossLoc) <= skillRadius * skillRadius) {
                        living.damage(skillDamage, currentBoss);
                        Vector knock = living.getLocation().toVector().subtract(bossLoc.toVector()).normalize();
                        knock.setY(0.65);
                        living.setVelocity(knock.multiply(skillVelocity));
                    }
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case PHANTOM -> {
                Vector dive = targetLoc.toVector().subtract(bossLoc.toVector()).normalize();
                dive.setY(Math.max(0.2, dive.getY() + 0.25));
                currentBoss.setVelocity(dive.multiply(skillVelocity > 0 ? skillVelocity : 1.6));
                if (target.getLocation().distanceSquared(bossLoc) <= skillRadius * skillRadius) {
                    target.damage(skillDamage, currentBoss);
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case WITCH -> {
                for (int i = 0; i < castCount; i++) {
                    ThrownPotion potion = world.spawn(bossLoc.clone().add(0.0, 1.0, 0.0), ThrownPotion.class);
                    Vector potionDir = dir.clone();
                    if (skillSpread > 0.0) potionDir.rotateAroundY((random.nextDouble() - 0.5) * skillSpread);
                    potion.setItem(new ItemStack(Material.SPLASH_POTION));
                    potion.setVelocity(potionDir.normalize().multiply(skillVelocity));
                    registerBossProjectile(potion, skillDamage, skillRadius, true);
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case ENDERMAN -> {
                if (target == null) return false;
                Location tp = target.getLocation().clone().add((Math.random() - 0.5) * 4.0, 0.0, (Math.random() - 0.5) * 4.0);
                tp.setY(world.getHighestBlockYAt(tp) + 1.0);
                if (!tp.getBlock().getType().isAir() || !currentBoss.teleport(tp)) return false;
                world.spawnParticle(Particle.PORTAL, tp.clone().add(0.0, 1.0, 0.0), 24, 0.4, 0.6, 0.4, 0.2);
                world.playSound(tp, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                if (target.getLocation().distanceSquared(tp) <= skillRadius * skillRadius) {
                    target.damage(skillDamage, currentBoss);
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case VINDICATOR -> {
                for (Entity entity : world.getNearbyEntities(bossLoc, skillRadius, skillRadius, skillRadius)) {
                    if (entity instanceof LivingEntity living && !entity.equals(currentBoss)
                            && living.getLocation().distanceSquared(bossLoc) <= skillRadius * skillRadius) {
                        living.damage(skillDamage, currentBoss);
                        Vector knock = living.getLocation().toVector().subtract(bossLoc.toVector()).normalize();
                        knock.setY(0.5);
                        living.setVelocity(knock.multiply(skillVelocity));
                    }
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case ZOMBIE -> {
                for (Entity entity : world.getNearbyEntities(bossLoc, skillRadius, skillRadius, skillRadius)) {
                    if (entity instanceof LivingEntity living && !entity.equals(currentBoss)
                            && living.getLocation().distanceSquared(bossLoc) <= skillRadius * skillRadius) {
                        living.damage(skillDamage, currentBoss);
                        Vector slam = living.getLocation().toVector().subtract(bossLoc.toVector()).normalize();
                        slam.setY(0.6);
                        living.setVelocity(slam.multiply(skillVelocity));
                    }
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            case GHAST -> {
                for (int i = 0; i < castCount; i++) {
                    Vector fireballDir = dir.clone();
                    if (skillSpread > 0.0) {
                        fireballDir.rotateAroundY((random.nextDouble() - 0.5) * skillSpread);
                    }
                    LargeFireball fireball = world.spawn(bossLoc.clone().add(0.0, 2.0, 0.0), LargeFireball.class);
                    fireball.setDirection(fireballDir.normalize().multiply(skillVelocity));
                    fireball.setYield(0.0F);
                    fireball.setIsIncendiary(false);
                    fireball.setShooter(currentBoss);
                    registerBossProjectile(fireball, skillDamage, skillRadius, false);
                }
                return completeBossSkillCast(skillKey, target, now);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean completeBossSkillCast(String skillKey, LivingEntity target, long castTimeMillis) {
        if (currentBoss == null || skillKey == null) return false;
        bossSkillCooldowns.put(currentBoss.getUniqueId() + ":" + skillKey, castTimeMillis);
        announceBossSkillCast(skillKey, target);
        return true;
    }

    private void announceBossSkillCast(String skillKey, LivingEntity target) {
        if (currentBoss == null) return;
        String bossName = currentBossName == null ? currentBoss.getType().name() : currentBossName;
        String localizedSkillName = getLocalizedSkillName(currentBoss.getType(), skillKey);
        String targetName = target instanceof Player player ? player.getName() : target.getType().name();
        Location location = currentBoss.getLocation();
        String details = bossName + " Lv." + currentBossLevel + " cast " + localizedSkillName
            + " [" + skillKey + "]"
            + " at target=" + targetName + " position="
            + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ()
            + " world=" + location.getWorld().getName();

        if (config.bossSkillCastLogsEnabled) logger.info("[BossSkill] " + details);
        String message = "§5§l[BOSS] §d" + bossName + " §7(Lv." + currentBossLevel
            + ") dùng kỹ năng §e" + localizedSkillName + "§7 nhắm tới §f" + targetName + "§7.";
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() ? config.bossSkillCastOpBroadcastEnabled
                    : config.bossSkillCastPlayerBroadcastEnabled) {
                player.sendMessage(message);
            }
        }
    }

    private String getLocalizedSkillName(EntityType bossType, String skillKey) {
        return switch (bossType) {
            case CREEPER -> anySkillKey(skillKey, "charged-explosion", "creeper-blast", "blast")
                ? "Lôi Nộ Tích Điện" : skillKey;
            case ENDERMAN -> anySkillKey(skillKey, "void-snatch", "shadow-strike", "enderman-teleport", "teleport")
                ? "Bắt Hư Không" : "end-rift".equalsIgnoreCase(skillKey) ? "Vết Nứt Hư Không" : skillKey;
            case RAVAGER -> anySkillKey(skillKey, "siege-charge", "charge", "ravager-charge")
                ? "Cú Tông Địa Chấn" : "roar-weakness".equalsIgnoreCase(skillKey)
                    ? "Tiếng Gầm Áp Đảo" : skillKey;
            case PHANTOM -> anySkillKey(skillKey, "dive-bomb", "phantom-dive", "dive")
                ? "Cú Lao Đêm Mộng" : "phantom-minions".equalsIgnoreCase(skillKey)
                    ? "Triệu Hồi Bóng Ma" : skillKey;
            case GHAST -> anySkillKey(skillKey, "fireball-volley", "ghast-fireball", "fireball")
                ? "Mưa Cầu Lửa" : "fiery-drag".equalsIgnoreCase(skillKey) ? "Kéo Lửa" : skillKey;
            case SPIDER -> anySkillKey(skillKey, "venom-web", "spider-web", "spider-poison", "web")
                ? "Tơ Độc" : "cobweb-trap".equalsIgnoreCase(skillKey) ? "Bẫy Tơ Nhện"
                    : "venom-pounce".equalsIgnoreCase(skillKey) ? "Nhảy Độc" : skillKey;
            case VINDICATOR -> anySkillKey(skillKey, "axe-spin", "vindicator-shockwave",
                "vindicator_shockwave", "shockwave") ? "Múa Rìu Xoay"
                : "axe-throw".equalsIgnoreCase(skillKey) ? "Phóng Rìu" : skillKey;
            case WITCH -> anySkillKey(skillKey, "potion-rain", "potion-volley", "witch-potion", "potion")
                ? "Mưa Thuốc Độc" : "disarm-magic".equalsIgnoreCase(skillKey) ? "Tước Vũ Khí" : skillKey;
            case GIANT -> anySkillKey(skillKey, "ground-slam", "stomp", "giant-stomp")
                ? "Đập Đất Sóng Xung Kích" : "boulder-toss".equalsIgnoreCase(skillKey)
                    ? "Ném Đá Khổng Lồ" : skillKey;
            case SKELETON -> anySkillKey(skillKey, "arrow-barrage", "skeleton-arrow", "arrow-fan", "fire-arrow-fan")
                ? "Mưa Tên Vạn Cốt" : skillKey;
            case ENDER_DRAGON -> anySkillKey(skillKey, "fireball-volley", "dragon-fireball",
                "ender-dragon-fireball", "fireball-fan") ? "Long Viêm Liên Xạ" : skillKey;
            case WITHER -> anySkillKey(skillKey, "skull-volley", "wither-skull", "skull")
                ? "Mưa Sọ Hư Vô" : skillKey;
            case ZOMBIE -> anySkillKey(skillKey, "ground-slam", "zombie-slam", "slam") ? "Đập Đất" : skillKey;
            default -> skillKey;
        };
    }

    private boolean anySkillKey(String actualKey, String... aliases) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(actualKey)) return true;
        }
        return false;
    }

    private void launchArrowBarrage(World world, Location bossLoc, Location targetLoc, int arrowCount,
            double damage, double radius, double velocity, double spread) {
        double launchHeight = Math.min(7.0, Math.max(2.0, world.getMaxHeight() - bossLoc.getY() - 2.0));
        Location launchLoc = bossLoc.clone().add(0.0, launchHeight, 0.0);
        Vector launchDirection = targetLoc.toVector().subtract(launchLoc.toVector());
        launchDirection.setY(Math.max(0.8, launchDirection.getY() + 1.2));
        if (launchDirection.lengthSquared() < 0.0001) launchDirection.setY(1.0);

        Arrow signalArrow = world.spawn(launchLoc, Arrow.class);
        signalArrow.setShooter(currentBoss);
        signalArrow.setFireTicks(60);
        signalArrow.setVelocity(launchDirection.normalize().multiply(velocity));
        activeBossProjectiles.add(signalArrow);

        BukkitTask[] taskRef = new BukkitTask[1];
        BukkitRunnable burstTask = new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                ticks++;
                if (!signalArrow.isValid() || signalArrow.isDead() || ticks >= 100) {
                    finish();
                    return;
                }
                if (ticks > 1 && signalArrow.getVelocity().getY() <= 0.0) {
                    Location apex = signalArrow.getLocation().clone();
                    signalArrow.remove();
                    activeBossProjectiles.remove(signalArrow);
                    world.spawnParticle(Particle.FIREWORK, apex, 24, 0.3, 0.3, 0.3, 0.08);
                    world.spawnParticle(Particle.EXPLOSION, apex, 1);
                    world.playSound(apex, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.2F);

                    double spreadRadius = Math.min(radius, spread * 3.0);
                    for (int i = 0; i < arrowCount; i++) {
                        double angle = 2.0 * Math.PI * i / arrowCount;
                        Location aim = targetLoc.clone().add(
                            Math.cos(angle) * spreadRadius, 0.0, Math.sin(angle) * spreadRadius);
                        Vector direction = aim.toVector().subtract(apex.toVector());
                        direction.setY(Math.min(-0.15, direction.getY()));
                        if (direction.lengthSquared() < 0.0001) direction.setY(-1.0);
                        Arrow arrow = world.spawn(apex, Arrow.class);
                        arrow.setShooter(currentBoss);
                        arrow.setFireTicks(60);
                        arrow.setCritical(true);
                        arrow.setDamage(damage);
                        arrow.setVelocity(direction.normalize().multiply(velocity));
                        activeBossProjectiles.add(arrow);
                    }
                    finish();
                }
            }

            private void finish() {
                cancel();
                bossSkillTasks.remove(taskRef[0]);
            }
        };
        taskRef[0] = burstTask.runTaskTimer(plugin, 1L, 1L);
        bossSkillTasks.add(taskRef[0]);
    }

    /**
     * Khi player join, show boss bar hoặc warning bar nếu đang có sự kiện
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (pendingBoss != null && warningTimeLeft > 0) {
            // Đang trong giai đoạn warning
            showWarningBarToPlayer(player);
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.5f);
        } else if (bossActive && currentBoss != null && !currentBoss.isDead()) {
            // Boss đang hoạt động
            showBossBarToPlayer(player);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }
    }

    /**
     * Hiển thị warning bar cho một player cụ thể
     */
    private void showWarningBarToPlayer(Player player) {
        if (pendingBoss == null) return;
        bossBar.name(Component.text("§e§l⚠ " + pendingBoss.displayName + " §7- §c§l" + warningTimeLeft + "s"));
        bossBar.progress((float) warningTimeLeft / pendingBoss.warningSeconds);
        player.showBossBar(bossBar);
    }

    /**
     * Hiển thị boss bar cho một player cụ thể
     */
    private void showBossBarToPlayer(Player player) {
        if (currentBoss == null || currentBoss.isDead()) return;
        int remaining = Math.max(0, bossDurationSeconds - bossElapsedSeconds);
        updateBossDisplay(currentBossName, remaining);
        player.showBossBar(bossBar);
    }

    private void updateBossDisplay(String displayName, int secondsRemaining) {
        if (currentBoss == null || !currentBoss.isValid() || currentBoss.isDead()) return;
        double maximum = Math.max(1.0, currentBoss.getMaxHealth());
        double health = Math.max(0.0, Math.min(maximum, currentBoss.getHealth()));
        currentBoss.setCustomName(displayName + " §8[Lv." + currentBossLevel + "] §c"
            + String.format(java.util.Locale.US, "%.0f", health) + "/"
            + String.format(java.util.Locale.US, "%.0f", maximum) + " HP");
        bossBar.progress((float) Math.max(0.0, Math.min(1.0, health / maximum)));
        bossBar.name(LegacyComponentSerializer.legacySection().deserialize("§4§l⚠ " + displayName +
            " §7[Lv." + currentBossLevel + "] §7- §c" + String.format(java.util.Locale.US, "%.0f", health) + "/" +
            String.format(java.util.Locale.US, "%.0f", maximum) + " HP" +
            " §7| §e" + Math.max(0, secondsRemaining) + "s"));
    }

    private void trySpawnBoss() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        if (pendingBoss != null) return; // Already has a warning in progress

        // Lọc các boss enabled từ config.bossConfigs
        List<Map.Entry<String, ConfigManager.BossConfig>> enabledBosses = new ArrayList<>();
        for (Map.Entry<String, ConfigManager.BossConfig> entry : config.bossConfigs.entrySet()) {
            if (entry.getValue().enabled) {
                enabledBosses.add(entry);
            }
        }

        if (enabledBosses.isEmpty()) return;

        // Tính tổng chance
        int totalChance = 0;
        for (Map.Entry<String, ConfigManager.BossConfig> entry : enabledBosses) {
            totalChance += Math.max(0, entry.getValue().chance);
        }

        ConfigManager.BossConfig selectedBoss;
        if (totalChance <= 0) {
            // Preserve legacy behavior when all per-boss weights are zero.
            selectedBoss = enabledBosses.get(0).getValue();
        } else {
            // First select boss by its individual weight, then independently roll
            // the day-based chance that this selected boss event proceeds.
            int roll = random.nextInt(totalChance);
            int cumulative = 0;
            selectedBoss = enabledBosses.get(enabledBosses.size() - 1).getValue();
            for (Map.Entry<String, ConfigManager.BossConfig> entry : enabledBosses) {
                cumulative += Math.max(0, entry.getValue().chance);
                if (roll < cumulative) {
                    selectedBoss = entry.getValue();
                    break;
                }
            }
        }

        double successChance = getBossSpawnSuccessChance();
        if (random.nextDouble() * 100.0 >= successChance) {
            long worldDay = getCurrentWorldDay();
            logger.info("[BossEvent] " + selectedBoss.displayName +
                " rolled but did not pass the day " + worldDay +
                " spawn-success check (" + successChance + "%).");
            timeSinceLastBoss = 0;
            bossEventIntervalSeconds = sampleBossEventInterval();
            return;
        }
        startWarning(selectedBoss);
    }

    private void startWarning(ConfigManager.BossConfig bc) {
        startWarning(bc, null);
    }

    private void startWarning(ConfigManager.BossConfig bc, Integer forcedLevel) {
        if (bossActive || pendingBoss != null) return;
        
        pendingBoss = bc;
        pendingBossLevel = forcedLevel;
        warningTimeLeft = Math.max(0, bc.warningSeconds);
        
        // Show warning bar cho tất cả
        bossBar.name(Component.text("§e§l⚠ BOSS SẮP XUẤT HIỆN ⚠"));
        bossBar.progress(1.0f);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.5f);
        }
        
        Bukkit.broadcastMessage("§4§l⚠ CẢNH BÁO: §c" + bc.displayName + " §4§lsẽ xuất hiện trong §e§l" + bc.warningSeconds + " giây§4§l!");

        if (warningTimeLeft == 0) {
            Integer levelToSpawn = pendingBossLevel;
            pendingBoss = null;
            pendingBossLevel = null;
            spawnBoss(bc, levelToSpawn);
        }
    }

    private void updateWarningBar() {
        if (warningTimeLeft > 0 && pendingBoss != null) {
            bossBar.name(Component.text("§e§l⚠ " + pendingBoss.displayName + " §7- §c§l" + warningTimeLeft + "s"));
            bossBar.progress((float) warningTimeLeft / pendingBoss.warningSeconds);
            
            if (warningTimeLeft <= 10 || warningTimeLeft == 30) {
                Bukkit.broadcastMessage("§c§l⚠ " + pendingBoss.displayName + " §csẽ xuất hiện trong §4§l" + warningTimeLeft + "§c giây!");
            }
        }
    }

    /**
     * Ngăn boss bị cháy dưới ánh nắng nếu có immunity sunlight-burn
     */
    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (currentBoss == null || !entity.equals(currentBoss)) return;

        // Kiểm tra boss config của currentBoss
        for (Map.Entry<String, ConfigManager.BossConfig> entry : config.bossConfigs.entrySet()) {
            ConfigManager.BossConfig bc = entry.getValue();
            if (!bc.displayName.equals(currentBossName)) continue;
            
            // Kiểm tra sunlight-burn immunity
            if (bc.immunities.getOrDefault("sunlight-burn", false)) {
                event.setCancelled(true);
                return;
            }
            
            // Kiểm tra fire immunity
            if (bc.immunities.getOrDefault("fire", false) && event instanceof EntityCombustEvent) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void spawnBoss(ConfigManager.BossConfig bc) {
        spawnBoss(bc, null);
    }

    private void spawnBoss(ConfigManager.BossConfig bc, Integer forcedLevel) {
        if (bossActive) {
            clearWarningBar();
            return;
        }

        if (forcedLevel != null) {
            currentBossLevel = Math.max(1, forcedLevel);
        } else {
            currentBossLevel = sampleBossLevel(bc);
        }

        // Chọn player ngẫu nhiên
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty()) {
            clearWarningBar();
            return;
        }
        Player targetPlayer = onlinePlayers.get(random.nextInt(onlinePlayers.size()));

        // Tính vị trí spawn ngẫu nhiên quanh player
        int radius = config.bossEventSpawnRadius;
        int x = targetPlayer.getLocation().getBlockX() + random.nextInt(radius * 2) - radius;
        int z = targetPlayer.getLocation().getBlockZ() + random.nextInt(radius * 2) - radius;
        World world = targetPlayer.getWorld();
        int y = world.getHighestBlockYAt(x, z) + 1;
        Location spawnLoc = new Location(world, x, y, z);

        // Get entity type from config
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(bc.entityType.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("[BossEvent] Invalid entity-type '" + bc.entityType + "' for boss");
            clearWarningBar();
            return;
        }

        // Hide warning bar cho tất cả after validating the spawn request.
        clearWarningBar();

        // Spawn boss
        Entity spawnedEntity = world.spawnEntity(spawnLoc, entityType);
        if (!(spawnedEntity instanceof LivingEntity livingBoss)) {
            spawnedEntity.remove();
            logger.warning("[BossEvent] Entity-type '" + bc.entityType + "' is not a living entity");
            return;
        }
        currentBoss = livingBoss;
        activeBossConfig = bc;
        bossDamageContributions.clear();
        spiderBroodReleased = false;
        currentBossSpawnLocation = spawnLoc.clone();
        currentBossActivityRadius = bc.activityRadius;
        returningToAnchor = false;
        anchorPathUpdatedAtMs = 0L;
        giantWanderTarget = null;
        giantWanderUntilMs = 0L;
        if (currentBoss instanceof Creeper creeper) {
            ConfigManager.BossSkillConfig skill = getCreeperSkillConfig(bc);
            double explosionPower = skill == null ? 3.0
                : safeSkillValue(skill.explosionPower, 3.0, 1.0, 64.0);
            creeper.setExplosionRadius((int) Math.round(explosionPower));
            creeper.setPowered(false);
            creeper.setIgnited(false);
            creeper.setFuseTicks(0);
            creeper.setGravity(true);
            nextCreeperExplosionAtMs = 0L;
        }
        double effectiveHp = resolveBossHp(bc, currentBossLevel);
        currentBossName = bc.displayName;
        try {
            if (currentBoss.getAttribute(Attribute.MAX_HEALTH) != null) {
                currentBoss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(effectiveHp);
            } else {
                currentBoss.setMaxHealth(effectiveHp);
            }
        } catch (IllegalArgumentException exception) {
            effectiveHp = Math.min(effectiveHp, currentBoss.getMaxHealth());
            logger.warning("[BossEvent] Unable to set boss max health; using " + effectiveHp + " HP");
        }
        effectiveHp = Math.max(1.0, Math.min(MAX_BOSS_HEALTH, Math.min(effectiveHp, currentBoss.getMaxHealth())));
        currentBoss.setHealth(effectiveHp);
        bossRewardThresholdHealth = effectiveHp;
        currentBoss.setCustomNameVisible(true);
        currentBoss.setGlowing(true);
        updateBossDisplay(bc.displayName, Math.max(0, bc.durationSeconds));

        // Keep native targeting and navigation active; boss skills augment its behavior.
        if (currentBoss instanceof Mob mob) {
            mob.setAI(true);
            mob.setPersistent(true);
            mob.setRemoveWhenFarAway(false);
        }
        keepBossChunkLoaded();

        // ===== ÁP DỤNG IMMUNITIES =====
        // Sunlight burn immunity: thêm fire resistance effect vĩnh viễn
        if (bc.immunities.getOrDefault("sunlight-burn", false)) {
            currentBoss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE,
                Integer.MAX_VALUE,
                0,
                false,
                false
            ));
        }
        // Fire immunity
        if (bc.immunities.getOrDefault("fire", false)) {
            currentBoss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE,
                Integer.MAX_VALUE,
                0,
                false,
                false
            ));
        }
        // Fall damage immunity
        if (bc.immunities.getOrDefault("fall-damage", false)) {
            currentBoss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOW_FALLING,
                Integer.MAX_VALUE,
                0,
                false,
                false
            ));
        }
        // Wither immunity
        if (bc.immunities.getOrDefault("wither", false)) {
            currentBoss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE,
                Integer.MAX_VALUE,
                0,
                false,
                false
            ));
        }
        // Poison immunity
        if (bc.immunities.getOrDefault("poison", false)) {
            currentBoss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE,
                Integer.MAX_VALUE,
                0,
                false,
                false
            ));
        }

        bossActive = true;
        leaderboardManager.startBoss(currentBoss);
        timeSinceLastBoss = 0;
        bossEventIntervalSeconds = sampleBossEventInterval();
        bossElapsedSeconds = 0;
        bossDurationSeconds = bc.durationSeconds;

        // Thông báo cho tất cả player
        String msg = "§4§l⚠ BOSS: §c" + bc.displayName + " §4§lđã xuất hiện tại §e" +
            x + ", " + y + ", " + z + " §4§l(" + world.getName() + ")";
        Bukkit.broadcastMessage("§4§m========================================");
        Bukkit.broadcastMessage(msg);
        Bukkit.broadcastMessage("§e§lHP: §c" + (int) effectiveHp + " §e| Sát thương: x" + bc.damageMultiplier + " §e| Lv." + currentBossLevel);
        Bukkit.broadcastMessage("§eThời gian tồn tại: " + bc.durationSeconds + " giây");
        broadcastPossibleDrops(bc, currentBossLevel);
        Bukkit.broadcastMessage("§4§m========================================");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§4§l" + bc.displayName, "§eLv." + currentBossLevel + " • "
                + String.format(java.util.Locale.US, "%.0f", currentBoss.getHealth()) + "/"
                + String.format(java.util.Locale.US, "%.0f", currentBoss.getMaxHealth()) + " HP", 10, 40, 20);
            showBossBarToPlayer(p);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }

        logManager.logDisaster("BOSS: " + bc.displayName + " spawned at " + x + "," + y + "," + z + " (Lv." + currentBossLevel + ")");

        // Khởi động AI task cho boss
        startBossAI(bc);

        // Task theo dõi boss (cập nhật boss bar, kiểm tra chết/hết thời gian)
        bossMonitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentBoss == null || currentBoss.isDead() || !currentBoss.isValid()) {
                    // Boss đã chết
                    if (currentBoss != null && currentBoss.isDead()) {
                        onBossKilled(bc);
                    } else if (currentBoss != null) {
                        logger.warning("[BossEvent] Boss entity became invalid before timeout: "
                            + currentBoss.getType() + " uuid=" + currentBoss.getUniqueId());
                    }
                    cleanup();
                    this.cancel();
                    return;
                }

                bossElapsedSeconds++;
                int remaining = Math.max(0, bossDurationSeconds - bossElapsedSeconds);

                // Update boss bar - cập nhật cho tất cả online player
                updateBossDisplay(bc.displayName, remaining);

                if (bossElapsedSeconds >= bossDurationSeconds) {
                    // Hết thời gian - boss biến mất
                    Bukkit.broadcastMessage("§c§l⏰ " + bc.displayName + " đã biến mất!");
                    cleanup();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * AI cho boss: chạy mỗi 10 ticks (0.5 giây).
     */
    private void startBossAI(ConfigManager.BossConfig bc) {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }

        BukkitRunnable aiRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentBoss == null || currentBoss.isDead() || !bossActive) {
                    this.cancel();
                    return;
                }

                Location bossLoc = currentBoss.getLocation();
                World bossWorld = bossLoc.getWorld();
                if (bossWorld == null) return;
                keepBossChunkLoaded();
                tickBossTraits(bc);
                tickSplitFireballs();

                double distanceFromAnchorSquared = currentBossSpawnLocation == null
                    ? 0.0 : horizontalDistanceSquared(bossLoc, currentBossSpawnLocation);
                boolean outsideActivityRadius = currentBossSpawnLocation != null
                    && distanceFromAnchorSquared > (double) currentBossActivityRadius * currentBossActivityRadius;
                if (!outsideActivityRadius) {
                    returningToAnchor = false;
                } else if (currentBoss instanceof EnderDragon) {
                    long now = System.currentTimeMillis();
                    if (!returningToAnchor || now >= anchorPathUpdatedAtMs) {
                        steerBossTowardAnchor(bossLoc);
                        returningToAnchor = true;
                        anchorPathUpdatedAtMs = now + 1000L;
                    }
                    return;
                } else if (currentBoss instanceof Mob mob) {
                    if (!returningToAnchor) {
                        if (mob.getTarget() != null) mob.setTarget(null);
                    }
                    if (!returningToAnchor || System.currentTimeMillis() >= anchorPathUpdatedAtMs) {
                        Location destination = currentBossSpawnLocation.clone();
                        destination.setY(bossWorld.getHighestBlockYAt(destination) + 1.0);
                        mob.getPathfinder().moveTo(destination, currentBoss instanceof Giant ? 1.1 : 1.0);
                        returningToAnchor = true;
                        anchorPathUpdatedAtMs = System.currentTimeMillis() + 2000L;
                    }
                    return;
                } else {
                    steerBossTowardAnchor(bossLoc);
                    return;
                }

                // === TÌM MỤC TIÊU ===
                // Bán kính tìm kiếm: 20 blocks cho player, 15 cho mob
                double searchRadius = 20.0;

                // Ưu tiên 1: Tấn công người chơi
                Player nearestPlayer = null;
                double nearestPlayerDist = Double.MAX_VALUE;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().equals(bossWorld)) continue;
                    if (p.isDead() || !p.isOnline()) continue;
                    double d = p.getLocation().distanceSquared(bossLoc);
                    if (!isWithinBossArea(p.getLocation())) continue;
                    if (d < nearestPlayerDist && d < searchRadius * searchRadius) {
                        nearestPlayerDist = d;
                        nearestPlayer = p;
                    }
                }

                if (nearestPlayer != null) {
                    updateNativeTarget(nearestPlayer);
                    if (useBossSkill(bc, nearestPlayer)) {
                        return;
                    }
                    // Tấn công người chơi
                    attackTarget(nearestPlayer, bc);
                    return;
                }

                // Ưu tiên 2: Tấn công friendly mobs (Villager, Iron Golem, Snowman, etc.)
                LivingEntity nearestFriendly = null;
                double nearestFriendlyDist = Double.MAX_VALUE;
                LivingEntity nearestMob = null;
                double nearestMobDist = Double.MAX_VALUE;
                for (Entity e : bossWorld.getNearbyEntities(bossLoc, 15, 10, 15)) {
                    if (e.equals(currentBoss)) continue;
                    if (e instanceof LivingEntity le && !(e instanceof Player)) {
                        double d = le.getLocation().distanceSquared(bossLoc);
                        if (!isWithinBossArea(le.getLocation())) continue;
                        if (isFriendlyMob(le)) {
                            if (d < nearestFriendlyDist) {
                                nearestFriendlyDist = d;
                                nearestFriendly = le;
                            }
                        } else if (!(le instanceof Boss)) {
                            if (d < nearestMobDist) {
                                nearestMobDist = d;
                                nearestMob = le;
                            }
                        }
                    }
                }

                if (nearestFriendly != null) {
                    updateNativeTarget(nearestFriendly);
                    if (useBossSkill(bc, nearestFriendly)) {
                        return;
                    }
                    attackTarget(nearestFriendly, bc);
                    return;
                }

                if (nearestMob != null) {
                    updateNativeTarget(nearestMob);
                    if (useBossSkill(bc, nearestMob)) {
                        return;
                    }
                    attackTarget(nearestMob, bc);
                    return;
                }

                if (currentBoss instanceof Giant) {
                    wanderGiant(bossLoc);
                } else if (currentBoss instanceof EnderDragon dragon) {
                    if (dragon.getTarget() != null) dragon.setTarget(null);
                    if (dragon.getPhase() != EnderDragon.Phase.CIRCLING) {
                        dragon.setPhase(EnderDragon.Phase.CIRCLING);
                    }
                }
            }
        };
        aiTask = aiRunnable.runTaskTimer(plugin, 0L, 10L);
    }

    private void wanderGiant(Location bossLoc) {
        long now = System.currentTimeMillis();
        if (giantWanderTarget == null || now >= giantWanderUntilMs
                || bossLoc.distanceSquared(giantWanderTarget) < 2.25) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 4.0 + random.nextDouble() * 6.0;
            Location candidate = bossLoc.clone().add(Math.cos(angle) * distance, 0.0,
                Math.sin(angle) * distance);
            Block feet = candidate.getBlock();
            if (!isWithinBossArea(candidate) || !feet.isPassable()
                    || !feet.getRelative(0, -1, 0).getType().isSolid()) {
                giantWanderTarget = null;
                giantWanderUntilMs = now + 1500L;
                return;
            }
            giantWanderTarget = candidate;
            giantWanderUntilMs = now + 5000L;
        }
        if (currentBoss instanceof Giant giant && giantWanderTarget != null) {
            Vector direction = giantWanderTarget.toVector().subtract(bossLoc.toVector()).setY(0.0);
            if (direction.lengthSquared() > 0.25) {
                Vector velocity = direction.normalize().multiply(0.07);
                velocity.setY(giant.getVelocity().getY());
                giant.setVelocity(velocity);
            }
        }
    }

    /**
     * Tấn công mục tiêu: di chuyển đến và gây sát thương
     */
    private void attackTarget(LivingEntity target, ConfigManager.BossConfig bc) {
        if (currentBoss == null || currentBoss.isDead()) return;

        Location bossLoc = currentBoss.getLocation();
        Location targetLoc = target.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;
        double distance = bossLoc.distance(targetLoc);

        if (currentBoss instanceof EnderDragon) {
            EnderDragon dragon = (EnderDragon) currentBoss;
            dragon.setAware(true);
            dragon.setTarget(target);
            dragon.setPhase(EnderDragon.Phase.STRAFING);
            if (distance <= 3.0) {
                target.damage(10.0 * bc.damageMultiplier, currentBoss);
                Vector knockback = target.getLocation().toVector().subtract(bossLoc.toVector()).normalize();
                knockback.setY(0.5);
                target.setVelocity(knockback.multiply(1.5));
            }
        } else if (currentBoss instanceof Creeper creeper) {
            if (creeper.getTarget() != target) creeper.setTarget(target);
            if (distance <= 3.0) {
                ConfigManager.BossSkillConfig skill = getCreeperSkillConfig(bc);
                int cooldownSeconds = skill == null ? 4
                    : Math.max(1, Math.min(120, skill.explosionCooldownSeconds));
                long now = System.currentTimeMillis();
                if (now >= nextCreeperExplosionAtMs) {
                    double explosionPower = creeper.isPowered() && skill != null
                        ? safeSkillValue(skill.chargedExplosionPower, 6.0, 1.0, 64.0)
                        : skill == null ? Math.max(1, creeper.getExplosionRadius())
                            : safeSkillValue(skill.explosionPower, 3.0, 1.0, 64.0);
                    world.createExplosion(bossLoc, (float) explosionPower, false, false, creeper);
                    Vector postExplosionVelocity = creeper.getVelocity();
                    creeper.setVelocity(new Vector(postExplosionVelocity.getX(),
                        Math.min(0.0, postExplosionVelocity.getY()), postExplosionVelocity.getZ()));
                    nextCreeperExplosionAtMs = now + cooldownSeconds * 1000L;
                }
            }
            return;
        } else if (currentBoss instanceof Giant giant) {
            giant.setAware(true);
            if (giant.getTarget() != target) giant.setTarget(target);
            if (distance > 3.0) {
                Vector direction = targetLoc.toVector().subtract(bossLoc.toVector()).setY(0.0);
                if (direction.lengthSquared() > 0.0001) {
                    Vector velocity = direction.normalize().multiply(0.12);
                    velocity.setY(giant.getVelocity().getY());
                    giant.setVelocity(velocity);
                }
            } else {
                target.damage(12.0 * bc.damageMultiplier, currentBoss);
                Vector knockback = target.getLocation().toVector().subtract(bossLoc.toVector());
                if (knockback.lengthSquared() < 0.0001) knockback.setZ(1.0);
                target.setVelocity(knockback.normalize().setY(0.3).multiply(2.0));
            }
            return;
        } else if (currentBoss instanceof Mob mob) {
            mob.setAware(true);
            if (mob.getTarget() != target) mob.setTarget(target);
        } else if (currentBoss instanceof Flying flying) {
            // Nếu là flying entity (Ghast)
            if (distance > 4.0) {
                Vector dir = targetLoc.toVector().subtract(bossLoc.toVector()).normalize();
                currentBoss.setVelocity(dir.multiply(0.4));
            }
        } else {
            // Các entity khác
            if (distance > 2.0) {
                Vector dir = targetLoc.toVector().subtract(bossLoc.toVector()).normalize();
                dir.setY(0);
                currentBoss.setVelocity(dir.multiply(0.3));
            }
        }

        // Tấn công nếu ở gần (cho các entity không phải dragon/giant)
        if (!(currentBoss instanceof EnderDragon) && !(currentBoss instanceof Giant) && distance <= 4.0) {
            target.damage(8.0 * bc.damageMultiplier, currentBoss);
        }
    }

    private void updateNativeTarget(LivingEntity target) {
        if (currentBoss instanceof Mob mob) {
            mob.setAware(true);
            if (mob.getTarget() != target) mob.setTarget(target);
        }
    }

    private boolean isWithinBossArea(Location location) {
        return currentBossSpawnLocation == null ||
            location.getWorld() == currentBossSpawnLocation.getWorld() &&
            horizontalDistanceSquared(location, currentBossSpawnLocation) <=
                (double) currentBossActivityRadius * currentBossActivityRadius;
    }

    private double horizontalDistanceSquared(Location first, Location second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private void steerBossTowardAnchor(Location bossLoc) {
        if (currentBoss == null || currentBossSpawnLocation == null) return;

        Location anchor = currentBossSpawnLocation;
        Vector direction = new Vector(anchor.getX() - bossLoc.getX(), 0.0, anchor.getZ() - bossLoc.getZ());
        if (direction.lengthSquared() < 0.0001) return;
        direction.normalize();

        if (currentBoss instanceof EnderDragon dragon) {
            if (dragon.getTarget() != null) dragon.setTarget(null);
            currentBoss.setVelocity(new Vector(direction.getX() * 0.65, 0.0, direction.getZ() * 0.65));
        } else {
            currentBoss.setVelocity(new Vector(direction.getX() * 0.35,
                Math.min(0.0, currentBoss.getVelocity().getY()), direction.getZ() * 0.35));
        }
    }

    /**
     * Kiểm tra entity có phải friendly mob không
     */
    private boolean isFriendlyMob(LivingEntity entity) {
        return entity instanceof Villager || 
               entity instanceof IronGolem || 
               entity instanceof Snowman ||
               entity instanceof Cat ||
               entity instanceof Wolf wolf && !wolf.isAngry() ||
               entity instanceof Parrot ||
               entity instanceof Bee ||
               entity instanceof Dolphin ||
               entity instanceof Fox ||
               entity instanceof Panda ||
               entity instanceof Turtle ||
               entity instanceof Axolotl ||
               entity instanceof Goat ||
               entity instanceof Allay ||
               entity instanceof Camel ||
               entity instanceof Sniffer ||
               entity instanceof Armadillo;
    }

    private void onBossKilled(ConfigManager.BossConfig bc) {
        EntityDamageEvent lastDamage = currentBoss == null ? null : currentBoss.getLastDamageCause();
        Set<UUID> eligiblePlayers = getRewardEligiblePlayers();
        awardBossExperience(bc);
        boolean playerEarnedRewards = !eligiblePlayers.isEmpty();
        if (!playerEarnedRewards) {
            String damageSource = lastDamage == null ? "unknown" : lastDamage.getCause().name();
            if (lastDamage instanceof EntityDamageByEntityEvent damageByEntity) {
                damageSource += " from " + damageByEntity.getDamager().getType();
            }

            logger.warning("[BossEvent] Boss died without a qualified player contribution: " + bc.displayName
                + ", cause=" + damageSource
                + ", requiredDamage=" + bossRewardThresholdHealth * config.bossRewardDamageThresholdPercent / 100.0
                + ", entity=" + currentBoss.getType() + ", uuid=" + currentBoss.getUniqueId());
        }
        if (playerEarnedRewards) {
            Bukkit.broadcastMessage("§a§l🎉 " + bc.displayName + " đã bị hạ gục; người chơi đủ đóng góp nhận điểm và vật phẩm.");
        } else {
            Bukkit.broadcastMessage("§c" + bc.displayName + " đã chết nhưng không có người chơi đạt ngưỡng sát thương "
                + config.bossRewardDamageThresholdPercent + "%.");
        }

        if (playerEarnedRewards) {
            // Drop items
            for (Map.Entry<String, ConfigManager.DropConfig> dropEntry : bc.drops.entrySet()) {
                ConfigManager.DropConfig dc = dropEntry.getValue();
                if (Double.isFinite(dc.chance) && dc.chance >= 0.0 && dc.chance <= 1.0 &&
                        random.nextDouble() < dc.chance) {
                    String materialName = dropEntry.getKey();
                    try {
                        Material material = Material.valueOf(materialName.toUpperCase());
                        if (material.isAir() || dc.minAmount < 1 || dc.maxAmount < dc.minAmount ||
                                dc.maxAmount > 64) continue;
                        int amount = random.nextInt(dc.maxAmount - dc.minAmount + 1) + dc.minAmount;
                        ItemStack dropItem = new ItemStack(material);
                        String itemName = dc.name;
                        List<String> itemLore = dc.lore;
                        if (material == Material.ENCHANTED_BOOK) {
                            if (itemName == null || itemName.isBlank()) itemName = bc.displayName + " §dBí Kíp";
                            if (itemLore.isEmpty()) itemLore = List.of("§7Sách enchant dùng lên đồ.");
                        }
                        BossLeaderboardManager.applyMetadata(dropItem, itemName, itemLore, null, null);
                        if (material == Material.ENCHANTED_BOOK && dc.bookEnchantments.isEmpty()) {
                            addConfiguredEnchantment(dropItem, "unbreaking", 1, true);
                        }
                        for (Map.Entry<String, Integer> enchantment : dc.bookEnchantments.entrySet()) {
                            addConfiguredEnchantment(dropItem, enchantment.getKey(), enchantment.getValue(), true);
                        }
                        if (currentBoss != null) {
                            dropItemsNaturally(currentBoss.getLocation(), dropItem, amount);
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warning("[BossEvent] Invalid drop material: " + materialName);
                    }
                }
            }

            if (currentBoss != null) {
                for (ConfigManager.ConfiguredItem configuredItem : levelDropsFor(bc, currentBossLevel)) {
                    if (random.nextDouble() >= configuredItem.chance) continue;
                    Material material = Material.matchMaterial(configuredItem.material);
                    if (material == null || material.isAir()) {
                        logger.warning("[BossEvent] Invalid configured drop material: " + configuredItem.material);
                        continue;
                    }
                    int amount = random.nextInt(configuredItem.maxAmount - configuredItem.minAmount + 1) +
                        configuredItem.minAmount;
                    ItemStack item = new ItemStack(material);
                    BossLeaderboardManager.applyMetadata(item, configuredItem.name, configuredItem.lore,
                        null, null);
                    for (Map.Entry<String, Integer> enchantment : configuredItem.enchantments.entrySet()) {
                        addConfiguredEnchantment(item, enchantment.getKey(), enchantment.getValue(), false);
                    }
                    for (Map.Entry<String, Integer> enchantment : configuredItem.bookEnchantments.entrySet()) {
                        addConfiguredEnchantment(item, enchantment.getKey(), enchantment.getValue(), true);
                    }
                    dropItemsNaturally(currentBoss.getLocation(), item, amount);
                }
                dropBossArmorSet(bc, currentBossLevel);
            }
        }
        if (currentBoss != null && bossDamageContributions.values().stream().anyMatch(damage -> damage > 0.0)) {
            dropArmySummonToken(currentBoss.getLocation(), currentBossLevel);
        }
        leaderboardManager.finishBoss(currentBossLevel, eligiblePlayers);
        logManager.logDisaster("BOSS: " + bc.displayName +
            (playerEarnedRewards ? " defeated; eligible contributors=" + eligiblePlayers
                : " died without eligible contribution") +
            "; cause=" + (lastDamage == null ? "unknown" : lastDamage.getCause().name()));
    }

    private void awardBossExperience(ConfigManager.BossConfig bossConfig) {
        int level = Math.max(1, currentBossLevel);
        double multiplier = 1.0 + (level - 1) * config.bossExperiencePerLevelMultiplier;
        int experience = (int) Math.max(1L, Math.min(1_000_000L,
            Math.round(bossConfig.experience * multiplier)));
        int playersRewarded = 0;
        int mercenariesRewarded = 0;
        for (Map.Entry<UUID, Double> entry : bossDamageContributions.entrySet()) {
            if (entry.getValue() <= 0.0) continue;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            player.giveExp(experience);
            playersRewarded++;
            if (plugin.getMercenaryManager() != null) {
                mercenariesRewarded += plugin.getMercenaryManager()
                    .awardBossExperience(player.getUniqueId(), experience);
            }
            player.sendMessage("§aBạn nhận được §e" + experience + " EXP§a khi hạ "
                + bossConfig.displayName + ".");
        }
        logger.info("[BossEvent] Awarded " + experience + " XP to " + playersRewarded
            + " contributor(s) and " + mercenariesRewarded + " deployed mercenary unit(s).");
    }

    private List<ConfigManager.ConfiguredItem> levelDropsFor(ConfigManager.BossConfig bc, int level) {
        if (bc.levelDrops.isEmpty()) return List.of();
        int selectedLevel = bc.levelDrops.keySet().stream()
            .filter(configuredLevel -> configuredLevel <= level)
            .mapToInt(Integer::intValue)
            .max()
            .orElse(-1);
        return selectedLevel < 0 ? List.of() : bc.levelDrops.getOrDefault(selectedLevel, List.of());
    }

    private void dropArmySummonToken(Location location, int level) {
        if (!config.bossArmyTokenDropEnabled || random.nextDouble() >= config.bossArmyTokenDropChance) return;
        ItemStack token = new ItemStack(Material.PAPER);
        ItemMeta meta = token.getItemMeta();
        meta.setDisplayName("§6§lLệnh Triệu Hồi §fBậc " + level);
        meta.setLore(List.of("§7Gọi lính Bậc " + level + ".", "§8Dùng /vnarmy hire."));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "army_token_class"),
            PersistentDataType.STRING, "any");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "army_token_level"),
            PersistentDataType.INTEGER, level);
        token.setItemMeta(meta);
        dropItemsNaturally(location, token, 1);
    }

    private void dropBossArmorSet(ConfigManager.BossConfig bossConfig, int level) {
        ConfigManager.BossArmorSetConfig armorSet = bossConfig.armorSet;
        if (armorSet == null || !armorSet.enabled || armorSet.materialTiers.isEmpty()) return;
        int tierLevel = armorSet.materialTiers.keySet().stream()
            .filter(configuredLevel -> configuredLevel <= level)
            .mapToInt(Integer::intValue)
            .max()
            .orElse(-1);
        if (tierLevel < 0) return;

        String materialTier = armorSet.materialTiers.get(tierLevel);
        String[][] pieces = {
            {"HELMET", "Mũ"},
            {"CHESTPLATE", "Áo Giáp"},
            {"LEGGINGS", "Quần Giáp"},
            {"BOOTS", "Ủng"}
        };
        List<String> lore = config.getBossItemStory(armorSet.storyId, level);
        if (lore.isEmpty()) {
            logger.warning("[BossEvent] Armor set story is unavailable: " + armorSet.storyId);
            return;
        }
        for (String[] piece : pieces) {
            if (random.nextDouble() >= armorSet.dropChance) continue;
            Material material = Material.matchMaterial(materialTier + "_" + piece[0]);
            if (material == null || material.isAir()) {
                logger.warning("[BossEvent] Invalid armor material tier '" + materialTier +
                    "' for " + bossConfig.displayName + ".");
                continue;
            }
            ItemStack item = new ItemStack(material);
            String itemName = armorSet.name + " " + piece[1] + " · Cấp " + level;
            BossLeaderboardManager.applyMetadata(item, itemName, lore, null, null);
            for (String enchantment : armorEnchantments(piece[0])) {
                int enchantmentLevel = armorEnchantmentLevel(enchantment, level);
                addConfiguredEnchantment(item, enchantment, enchantmentLevel, false);
            }
            dropItemsNaturally(currentBoss.getLocation(), item, 1);
        }
    }

    private List<String> armorEnchantments(String armorSlot) {
        return switch (armorSlot) {
            case "HELMET" -> List.of("protection", "unbreaking", "respiration", "aqua_affinity");
            case "CHESTPLATE" -> List.of("protection", "unbreaking", "thorns");
            case "LEGGINGS" -> List.of("protection", "unbreaking", "swift_sneak");
            case "BOOTS" -> List.of("protection", "unbreaking", "feather_falling", "depth_strider");
            default -> List.of();
        };
    }

    private int armorEnchantmentLevel(String enchantmentKey, int bossLevel) {
        Enchantment enchantment;
        try {
            enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentKey));
        } catch (IllegalArgumentException exception) {
            enchantment = null;
        }
        if (enchantment == null) {
            logger.warning("[BossEvent] Unknown armor enchantment: " + enchantmentKey);
            return 0;
        }
        int naturalMaximum = enchantment.getMaxLevel();
        if (naturalMaximum <= 2) return naturalMaximum;
        int initialLevel = Math.max(1, naturalMaximum - config.bossArmorNaturalLevelGap);
        int levelBonus = (int) Math.floor((bossLevel - 1) * config.bossArmorLevelsPerEnchant);
        return Math.min(config.bossArmorMaximumEnchantLevel, initialLevel + levelBonus);
    }

    private void broadcastPossibleDrops(ConfigManager.BossConfig bc, int level) {
        Bukkit.broadcastMessage("§d§lVật phẩm có thể rơi (tỉ lệ từng món):");
        for (Map.Entry<String, ConfigManager.DropConfig> entry : bc.drops.entrySet()) {
            ConfigManager.DropConfig drop = entry.getValue();
            if (drop.chance < 0.0 || drop.chance > 1.0 || !Double.isFinite(drop.chance)) continue;
            String itemName = drop.name;
            if (itemName == null || itemName.isBlank()) {
                itemName = entry.getKey().equalsIgnoreCase("ENCHANTED_BOOK")
                    ? bc.displayName + " §dBí Kíp" : entry.getKey();
            }
            Bukkit.broadcastMessage("§d• " + itemName + " §7— §e" +
                String.format(java.util.Locale.US, "%.1f%%", drop.chance * 100.0));
        }
        for (ConfigManager.ConfiguredItem item : levelDropsFor(bc, level)) {
            String displayName = item.name == null || item.name.isBlank() ? item.material : item.name;
            Bukkit.broadcastMessage("§d• " + displayName + " §7— §e" +
                String.format(java.util.Locale.US, "%.1f%%", item.chance * 100.0));
        }
        if (bc.armorSet != null && bc.armorSet.enabled) {
            Bukkit.broadcastMessage("§d• " + bc.armorSet.name + " (4 món) §7— §e" +
                String.format(java.util.Locale.US, "%.1f%% mỗi món", bc.armorSet.dropChance * 100.0));
        }
    }

    private void addConfiguredEnchantment(ItemStack item, String name, int level, boolean stored) {
        if (level < 1 || level > 255) return;
        String id = name.toLowerCase(java.util.Locale.ROOT);
        if (id.equals("damage_all")) id = "sharpness";
        else if (id.equals("damage_undead")) id = "smite";
        else if (id.equals("damage_arthropods")) id = "bane_of_arthropods";
        else if (id.equals("durability")) id = "unbreaking";
        else if (id.equals("loot_bonus_mobs")) id = "looting";
        org.bukkit.enchantments.Enchantment enchantment;
        try {
            enchantment = org.bukkit.enchantments.Enchantment.getByKey(
                org.bukkit.NamespacedKey.minecraft(id));
        } catch (IllegalArgumentException exception) {
            enchantment = null;
        }
        if (enchantment == null) {
            logger.warning("[BossEvent] Unknown configured enchantment: " + name);
            return;
        }
        if (stored && item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta meta) {
            meta.addStoredEnchant(enchantment, level, true);
            item.setItemMeta(meta);
        } else if (!stored) {
            item.addUnsafeEnchantment(enchantment, level);
        }
    }

    private void dropItemsNaturally(Location location, ItemStack template, int amount) {
        int remaining = amount;
        int stackLimit = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            ItemStack stack = template.clone();
            int stackAmount = Math.min(remaining, stackLimit);
            stack.setAmount(stackAmount);
            location.getWorld().dropItemNaturally(location, stack);
            remaining -= stackAmount;
        }
    }

    private void cleanup() {
        leaderboardManager.finishBoss(currentBossLevel, false);
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
        if (bossMonitorTask != null) {
            bossMonitorTask.cancel();
            bossMonitorTask = null;
        }
        for (Entity projectile : activeBossProjectiles) {
            if (projectile != null && !projectile.isDead()) {
                projectile.remove();
            }
        }
        activeBossProjectiles.clear();
        for (BukkitTask task : bossSkillTasks) {
            if (task != null) task.cancel();
        }
        bossSkillTasks.clear();
        creeperChargeTask = null;
        nextCreeperExplosionAtMs = 0L;
        bossProjectileConfigs.clear();
        bossSkillCooldowns.clear();
        splitFireballs.clear();
        webTrapProjectiles.clear();
        disarmProjectiles.clear();
        ravagerThornsCooldowns.clear();
        spinDamageCooldowns.clear();
        phantomRevealedUntil.clear();

        for (UUID summonId : bossSummons) {
            Entity summon = Bukkit.getEntity(summonId);
            if (summon != null && summon.isValid()) summon.remove();
        }
        bossSummons.clear();
        bossTerrainEntities.clear();
        bossTerrainDrops.clear();
        bossBoulders.clear();
        for (BlockPosition position : new ArrayList<>(temporaryBlocks.keySet())) {
            restoreTemporaryBlock(position);
        }
        playerModifiedTemporaryBlocks.clear();

        if (currentBoss != null && !currentBoss.isDead()) {
            currentBoss.remove();
        }
        if (forcedBossChunkWorld != null && !bossChunkWasAlreadyForceLoaded) {
            forcedBossChunkWorld.setChunkForceLoaded(forcedBossChunkX, forcedBossChunkZ, false);
        }
        forcedBossChunkWorld = null;
        forcedBossChunkX = 0;
        forcedBossChunkZ = 0;
        bossChunkWasAlreadyForceLoaded = false;
        bossActive = false;
        currentBoss = null;
        activeBossConfig = null;
        currentBossSpawnLocation = null;
        currentBossActivityRadius = 100;
        returningToAnchor = false;
        anchorPathUpdatedAtMs = 0L;
        giantWanderTarget = null;
        giantWanderUntilMs = 0L;
        currentBossName = null;
        pendingBoss = null;
        pendingBossLevel = null;
        warningTimeLeft = 0;
        currentBossLevel = 1;
        bossRewardThresholdHealth = 0.0;
        bossDamageContributions.clear();
        spiderBroodReleased = false;
        bossElapsedSeconds = 0;
        bossDurationSeconds = 0;
        bossBar.name(Component.text(""));
        bossBar.progress(0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(bossBar);
        }
    }

    private void clearWarningBar() {
        warningTimeLeft = 0;
        pendingBoss = null;
        pendingBossLevel = null;
        bossBar.name(Component.text(""));
        bossBar.progress(0);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    public boolean isBossActive() {
        return bossActive;
    }

    public boolean killActiveBoss() {
        if (!bossActive || currentBoss == null || !currentBoss.isValid() || currentBoss.isDead()) return false;
        String name = currentBossName == null ? "Boss" : currentBossName;
        cleanup();
        Bukkit.broadcastMessage("§c§l" + name + " đã bị quản trị viên kết thúc.");
        return true;
    }

    public String getCurrentBossName() {
        return currentBossName;
    }

    public Set<String> getBossIds() {
        return config.bossConfigs.keySet();
    }

    public Set<String> getEnabledBossIds() {
        Set<String> ids = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, ConfigManager.BossConfig> entry : config.bossConfigs.entrySet()) {
            if (entry.getValue().enabled) ids.add(entry.getKey());
        }
        return ids;
    }

    public String getBossName(String id) {
        ConfigManager.BossConfig bc = config.bossConfigs.get(id.toLowerCase());
        if (bc == null) return "Unknown Boss";
        return bc.displayName;
    }

    public String getBossEntityType(String id) {
        ConfigManager.BossConfig bc = config.bossConfigs.get(id.toLowerCase());
        return bc == null ? null : bc.entityType;
    }

    public boolean triggerBoss(String bossId, int warningTimeSeconds, int durationSeconds) {
        return triggerBoss(bossId, 1, warningTimeSeconds, durationSeconds);
    }

    public boolean triggerBoss(String bossId, int level, int warningTimeSeconds, int durationSeconds) {
        if (bossActive || pendingBoss != null) return false;
        if (level < 1 || level > 1000 || warningTimeSeconds < 1 || durationSeconds < 1) return false;

        ConfigManager.BossConfig bc = config.bossConfigs.get(bossId.toLowerCase());
        if (bc == null || !bc.enabled) return false;

        ConfigManager.BossConfig manualBoss = new ConfigManager.BossConfig();
        manualBoss.enabled = bc.enabled;
        manualBoss.entityType = bc.entityType;
        manualBoss.displayName = bc.displayName;
        manualBoss.hp = bc.hp;
        manualBoss.damageMultiplier = bc.damageMultiplier;
        manualBoss.chance = bc.chance;
        manualBoss.warningSeconds = warningTimeSeconds;
        manualBoss.durationSeconds = durationSeconds;
        manualBoss.activityRadius = bc.activityRadius;
        manualBoss.immunities.putAll(bc.immunities);
        manualBoss.immunityPotionEffects = new ArrayList<>(bc.immunityPotionEffects);
        manualBoss.drops.putAll(bc.drops);
        bc.levelDrops.forEach((configuredLevel, items) ->
            manualBoss.levelDrops.put(configuredLevel, new ArrayList<>(items)));
        if (bc.hpByLevel != null) manualBoss.hpByLevel.putAll(bc.hpByLevel);
        if (bc.skills != null) manualBoss.skills.putAll(bc.skills);
        if (bc.traits != null) manualBoss.traits.putAll(bc.traits);

        startWarning(manualBoss, level);
        return true;
    }

    public void stop() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
        cleanup();
        timeSinceLastBoss = 0;
        bossEventIntervalSeconds = 0;
    }

    public void reload() {
        stop();
        if (config.bossEventsEnabled) start();
    }
}