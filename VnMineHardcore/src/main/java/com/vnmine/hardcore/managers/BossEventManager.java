package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class BossEventManager implements Listener {

    private final VnMineHardcore plugin;
    private final ConfigManager config;
    private final LogManager logManager;
    private final Logger logger;
    private final Random random = ThreadLocalRandom.current();

    private boolean bossActive = false;
    private String currentBossName = null;
    private LivingEntity currentBoss = null;
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
    private Location lastBossLocation = null;
    private Location currentBossSpawnLocation = null;
    private int currentBossActivityRadius = 100;
    private int anchorReturnTicks;
    private int anchorReturnCooldownTicks;
    private int stuckTicks = 0;
    private BukkitTask aiTask;
    private int currentBossLevel = 1;
    private final Map<String, Long> bossSkillCooldowns = new HashMap<>();
    private final Set<Projectile> activeBossProjectiles = new HashSet<>();
    private final Map<UUID, BossProjectileConfig> bossProjectileConfigs = new HashMap<>();
    private final Set<BukkitTask> bossSkillTasks = new HashSet<>();
    private static final double MAX_BOSS_HEALTH = 1024.0;

    private record BossProjectileConfig(double damage, double radius, boolean splashOnImpact) { }

    public BossEventManager(VnMineHardcore plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logManager = plugin.getLogManager();
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

    public void start() {
        if (!config.bossEventsEnabled || (schedulerTask != null && !schedulerTask.isCancelled())) return;
        timeSinceLastBoss = 0;
        bossEventIntervalSeconds = sampleBossEventInterval();
        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getWorlds().isEmpty()) return;
                timeSinceLastBoss++;

                if (!bossActive && pendingBoss == null && timeSinceLastBoss >= bossEventIntervalSeconds) {
                    trySpawnBoss();
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

        long worldDay = 0L;
        if (!Bukkit.getWorlds().isEmpty()) {
            worldDay = Math.max(0L, Bukkit.getWorlds().get(0).getFullTime() / 24000L);
        }

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
        BossProjectileConfig skill = bossProjectileConfigs.remove(projectile.getUniqueId());
        activeBossProjectiles.remove(projectile);
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
    public void onBossTerrainChange(EntityChangeBlockEvent event) {
        if (belongsToCurrentBoss(event.getEntity())) {
            event.setCancelled(true);
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

    private boolean useBossSkill(ConfigManager.BossConfig bc, LivingEntity target) {
        if (currentBoss == null || currentBoss.isDead() || bc == null || target == null || bc.skills == null || bc.skills.isEmpty()) {
            return false;
        }

        String skillKey = null;
        switch (currentBoss.getType()) {
            case SKELETON -> skillKey = findSkillKey(bc, "arrow-barrage", "skeleton-arrow", "arrow-fan", "fire-arrow-fan");
            case ENDER_DRAGON -> skillKey = findSkillKey(bc, "fireball-volley", "dragon-fireball", "ender-dragon-fireball", "fireball-fan");
            case GIANT -> skillKey = findSkillKey(bc, "giant-stomp", "stomp", "giant_stomp");
            case CREEPER -> skillKey = findSkillKey(bc, "charged-explosion", "creeper-blast", "blast");
            case WITHER -> skillKey = findSkillKey(bc, "skull-volley", "wither-skull", "skull");
            case SPIDER -> skillKey = findSkillKey(bc, "venom-web", "spider-web", "spider-poison", "web");
            case RAVAGER -> skillKey = findSkillKey(bc, "charge", "ravager-charge");
            case PHANTOM -> skillKey = findSkillKey(bc, "dive-bomb", "phantom-dive", "dive");
            case WITCH -> skillKey = findSkillKey(bc, "potion-volley", "witch-potion", "potion");
            case ENDERMAN -> skillKey = findSkillKey(bc, "shadow-strike", "enderman-teleport", "teleport");
            case VINDICATOR -> skillKey = findSkillKey(bc, "vindicator-shockwave", "vindicator_shockwave", "shockwave");
            case ZOMBIE -> skillKey = findSkillKey(bc, "ground-slam", "zombie-slam", "slam");
            case GHAST -> skillKey = findSkillKey(bc, "fireball-volley", "ghast-fireball", "fireball");
            default -> { }
        }

        if (skillKey == null) return false;
        ConfigManager.BossSkillConfig skill = bc.skills.get(skillKey);
        if (skill == null || !skill.enabled) return false;

        World world = currentBoss.getWorld();
        if (world == null) return false;

        long now = System.currentTimeMillis();
        String cooldownKey = currentBoss.getUniqueId() + ":" + skillKey;
        Long lastUse = bossSkillCooldowns.get(cooldownKey);
        double cooldownSeconds = safeSkillValue(skill.cooldownSeconds, 0.0, 0.0, 86400.0);
        long cooldownMs = (long) (cooldownSeconds * 1000.0);
        if (lastUse != null && now - lastUse < cooldownMs) {
            return false;
        }

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
        bossSkillCooldowns.put(cooldownKey, now);

        switch (currentBoss.getType()) {
            case SKELETON -> {
                launchArrowBarrage(world, bossLoc, targetLoc, castCount, skillDamage,
                    skillRadius, skillVelocity, skillSpread);
                return true;
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
                return true;
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
                return true;
            }
            case CREEPER -> {
                damageEntitiesInRadius(world, bossLoc, skillRadius, skillDamage);
                world.spawnParticle(Particle.EXPLOSION, bossLoc, 1);
                world.playSound(bossLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
                return true;
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
                return true;
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
                return true;
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
                return true;
            }
            case PHANTOM -> {
                Vector dive = targetLoc.toVector().subtract(bossLoc.toVector()).normalize();
                dive.setY(Math.max(0.2, dive.getY() + 0.25));
                currentBoss.setVelocity(dive.multiply(skillVelocity > 0 ? skillVelocity : 1.6));
                if (target.getLocation().distanceSquared(bossLoc) <= skillRadius * skillRadius) {
                    target.damage(skillDamage, currentBoss);
                }
                return true;
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
                return true;
            }
            case ENDERMAN -> {
                if (target == null) return false;
                Location tp = target.getLocation().clone().add((Math.random() - 0.5) * 4.0, 0.0, (Math.random() - 0.5) * 4.0);
                tp.setY(world.getHighestBlockYAt(tp) + 1.0);
                if (tp.getBlock().getType().isAir()) {
                    currentBoss.teleport(tp);
                    if (target.getLocation().distanceSquared(tp) <= skillRadius * skillRadius) {
                        target.damage(skillDamage, currentBoss);
                    }
                }
                return true;
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
                return true;
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
                return true;
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
                return true;
            }
            default -> {
                return false;
            }
        }
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
        bossBar.name(Component.text("§4§l⚠ " + displayName +
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
            totalChance += entry.getValue().chance;
        }

        if (totalChance <= 0) {
            // Nếu tổng chance = 0, chọn boss đầu tiên
            startWarning(enabledBosses.get(0).getValue());
            return;
        }

        // Weighted random: roll trong [0, totalChance)
        int roll = random.nextInt(totalChance);
        int cumulative = 0;
        for (Map.Entry<String, ConfigManager.BossConfig> entry : enabledBosses) {
            ConfigManager.BossConfig bc = entry.getValue();
            cumulative += bc.chance;
            if (roll < cumulative) {
                startWarning(bc);
                return;
            }
        }

        // Fallback: không bao giờ chạy tới đây nếu totalChance > 0
        startWarning(enabledBosses.get(enabledBosses.size() - 1).getValue());
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
        currentBossSpawnLocation = spawnLoc.clone();
        currentBossActivityRadius = bc.activityRadius;
        anchorReturnTicks = 0;
        anchorReturnCooldownTicks = 0;
        stuckTicks = 0;
        if (currentBoss instanceof Creeper creeper) {
            creeper.setExplosionRadius(0);
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
        currentBoss.setCustomNameVisible(true);
        updateBossDisplay(bc.displayName, Math.max(0, bc.durationSeconds));

        // Remove default AI để dùng AI custom
        if (currentBoss instanceof Mob mob) {
            mob.setAI(true); // Vẫn giữ AI mặc định, nhưng chúng ta sẽ điều khiển thêm
        }

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
        timeSinceLastBoss = 0;
        bossEventIntervalSeconds = sampleBossEventInterval();
        lastBossLocation = spawnLoc.clone();
        stuckTicks = 0;
        bossElapsedSeconds = 0;
        bossDurationSeconds = bc.durationSeconds;

        // Thông báo cho tất cả player
        String msg = "§4§l⚠ BOSS: §c" + bc.displayName + " §4§lđã xuất hiện tại §e" +
            x + ", " + y + ", " + z + " §4§l(" + world.getName() + ")";
        Bukkit.broadcastMessage("§4§m========================================");
        Bukkit.broadcastMessage(msg);
        Bukkit.broadcastMessage("§e§lHP: §c" + (int) effectiveHp + " §e| Sát thương: x" + bc.damageMultiplier + " §e| Lv." + currentBossLevel);
        Bukkit.broadcastMessage("§eThời gian tồn tại: " + bc.durationSeconds + " giây");
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
                    if (currentBoss != null && currentBoss.getKiller() != null) {
                        onBossKilled(currentBoss.getKiller(), bc);
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

                anchorReturnCooldownTicks = Math.max(0, anchorReturnCooldownTicks - 10);
                double distanceFromAnchorSquared = currentBossSpawnLocation == null
                    ? 0.0 : horizontalDistanceSquared(bossLoc, currentBossSpawnLocation);
                double edgeBuffer = Math.min(10.0, currentBossActivityRadius * 0.15);
                double returnThreshold = Math.max(0.0, currentBossActivityRadius - edgeBuffer);
                boolean nearActivityEdge = distanceFromAnchorSquared >= returnThreshold * returnThreshold;

                if (anchorReturnTicks > 0) {
                    steerBossTowardAnchor(bossLoc, bossWorld);
                    anchorReturnTicks = Math.max(0, anchorReturnTicks - 10);
                    return;
                }
                if (currentBossSpawnLocation != null &&
                        distanceFromAnchorSquared > (double) currentBossActivityRadius * currentBossActivityRadius) {
                    beginAnchorReturn();
                    steerBossTowardAnchor(bossLoc, bossWorld);
                    anchorReturnTicks = Math.max(0, anchorReturnTicks - 10);
                    return;
                }
                if (nearActivityEdge || (anchorReturnCooldownTicks == 0 && random.nextDouble() < 0.08)) {
                    beginAnchorReturn();
                    steerBossTowardAnchor(bossLoc, bossWorld);
                    anchorReturnTicks = Math.max(0, anchorReturnTicks - 10);
                    return;
                }

                // === KIỂM TRA KẸT ===
                if (lastBossLocation != null) {
                    double dist = bossLoc.distanceSquared(lastBossLocation);
                    if (dist < 0.5) {
                        stuckTicks++;
                    } else {
                        stuckTicks = 0;
                    }
                }
                lastBossLocation = bossLoc.clone();

                // Nếu bị kẹt quá 5 giây (10 lần kiểm tra * 0.5s = 5s)
                if (stuckTicks >= 10) {
                    // Teleport lên cao hơn để thoát kẹt
                    Location newLoc = bossLoc.clone().add(0, 5, 0);
                    if (newLoc.getBlock().getType() == Material.AIR) {
                        currentBoss.teleport(newLoc);
                    } else {
                        // Nếu phía trên có block, teleport ngẫu nhiên xung quanh
                        int tryX = bossLoc.getBlockX() + random.nextInt(10) - 5;
                        int tryZ = bossLoc.getBlockZ() + random.nextInt(10) - 5;
                        int tryY = bossWorld.getHighestBlockYAt(tryX, tryZ) + 1;
                        currentBoss.teleport(new Location(bossWorld, tryX + 0.5, tryY, tryZ + 0.5));
                    }
                    stuckTicks = 0;
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
                    if (useBossSkill(bc, nearestFriendly)) {
                        return;
                    }
                    attackTarget(nearestFriendly, bc);
                    return;
                }

                if (nearestMob != null) {
                    if (useBossSkill(bc, nearestMob)) {
                        return;
                    }
                    attackTarget(nearestMob, bc);
                    return;
                }

                // Ưu tiên 4: Di chuyển ngẫu nhiên.
                wander(bossLoc, bossWorld);
            }
        };
        aiTask = aiRunnable.runTaskTimer(plugin, 0L, 10L);
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
        } else if (currentBoss instanceof Giant) {
            // Giant: di chuyển nhanh.
            if (distance > 3.0) {
                // Di chuyển về phía mục tiêu với tốc độ cao
                Vector dir = targetLoc.toVector().subtract(bossLoc.toVector()).normalize();
                dir.setY(0);
                currentBoss.setVelocity(dir.multiply(0.5));
            } else {
                // Ở gần - tấn công mạnh
                target.damage(12.0 * bc.damageMultiplier, currentBoss);
                // Đẩy lùi mục tiêu
                Vector knockback = target.getLocation().toVector().subtract(bossLoc.toVector()).normalize();
                knockback.setY(0.3);
                target.setVelocity(knockback.multiply(2.0));
            }
        } else if (currentBoss instanceof Mob mob) {
            mob.setAware(true);
            mob.setTarget(target);
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

    /**
     * Di chuyển ngẫu nhiên khi không có mục tiêu.
     */
    private void wander(Location bossLoc, World world) {
        if (currentBoss == null || currentBoss.isDead()) return;

        Location center = currentBossSpawnLocation != null ? currentBossSpawnLocation : bossLoc;
        double wanderRadius = Math.max(1.0, currentBossActivityRadius * 0.75);
        if (currentBoss instanceof EnderDragon) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = Math.sqrt(random.nextDouble()) * wanderRadius;
            double targetY = Math.max(20.0, Math.min(world.getMaxHeight() - 10.0,
                center.getY() + random.nextDouble() * 30.0 - 15.0));
            Location flightPoint = new Location(world,
                center.getX() + Math.cos(angle) * radius,
                targetY,
                center.getZ() + Math.sin(angle) * radius);
            Vector direction = flightPoint.toVector().subtract(bossLoc.toVector());
            if (direction.lengthSquared() > 0.0001) {
                currentBoss.setVelocity(direction.normalize().multiply(0.5));
            }
            ((EnderDragon) currentBoss).setPhase(EnderDragon.Phase.CIRCLING);

        } else if (currentBoss instanceof Giant) {
            // Giant: di chuyển ngẫu nhiên.
            if (random.nextInt(3) == 0) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double radius = Math.sqrt(random.nextDouble()) * wanderRadius;
                Location targetLoc = new Location(world,
                    center.getX() + Math.cos(angle) * radius,
                    bossLoc.getY(),
                    center.getZ() + Math.sin(angle) * radius);
                
                if (targetLoc.getBlock().getType() == Material.AIR) {
                    Vector dir = targetLoc.toVector().subtract(bossLoc.toVector()).normalize();
                    dir.setY(0);
                    currentBoss.setVelocity(dir.multiply(0.4));
                }
            }
        } else {
            // Các boss khác: di chuyển ngẫu nhiên
            if (random.nextInt(5) == 0) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double radius = Math.sqrt(random.nextDouble()) * wanderRadius;
                Location targetLoc = new Location(world,
                    center.getX() + Math.cos(angle) * radius,
                    bossLoc.getY(),
                    center.getZ() + Math.sin(angle) * radius);
                
                if (targetLoc.getBlock().getType() == Material.AIR) {
                    Vector dir = targetLoc.toVector().subtract(bossLoc.toVector()).normalize();
                    dir.setY(0);
                    currentBoss.setVelocity(dir.multiply(0.2));
                }
            }
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

    private void beginAnchorReturn() {
        if (currentBossSpawnLocation == null || anchorReturnTicks > 0) return;
        anchorReturnTicks = 80 + random.nextInt(61);
        anchorReturnCooldownTicks = 100 + random.nextInt(101);
        stuckTicks = 0;
    }

    private void steerBossTowardAnchor(Location bossLoc, World world) {
        if (currentBoss == null || currentBossSpawnLocation == null) return;

        Location anchor = currentBossSpawnLocation;
        Vector direction = new Vector(anchor.getX() - bossLoc.getX(), 0.0, anchor.getZ() - bossLoc.getZ());
        if (direction.lengthSquared() < 0.0001) {
            anchorReturnTicks = 0;
            return;
        }
        direction.normalize();

        if (currentBoss instanceof EnderDragon dragon) {
            dragon.setTarget(null);
            currentBoss.setVelocity(new Vector(direction.getX() * 0.65, 0.0, direction.getZ() * 0.65));
        } else if (currentBoss instanceof Mob mob) {
            mob.setTarget(null);
            Location destination = anchor.clone();
            destination.setY(world.getHighestBlockYAt(destination) + 1.0);
            boolean pathStarted = mob.getPathfinder().moveTo(destination, currentBoss instanceof Giant ? 1.1 : 1.0);
            if (!pathStarted) {
                currentBoss.setVelocity(new Vector(direction.getX() * 0.25,
                    currentBoss.getVelocity().getY(), direction.getZ() * 0.25));
            }
        } else {
            currentBoss.setVelocity(new Vector(direction.getX() * 0.35,
                currentBoss.getVelocity().getY(), direction.getZ() * 0.35));
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

    private void onBossKilled(Player killer, ConfigManager.BossConfig bc) {
        Bukkit.broadcastMessage("§a§l🎉 " + killer.getName() + " đã tiêu diệt " + bc.displayName + "!");

        // Drop items
        for (Map.Entry<String, ConfigManager.DropConfig> dropEntry : bc.drops.entrySet()) {
            ConfigManager.DropConfig dc = dropEntry.getValue();
            if (random.nextDouble() < dc.chance) {
                String materialName = dropEntry.getKey();
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    int amount = random.nextInt(dc.maxAmount - dc.minAmount + 1) + dc.minAmount;
                    ItemStack dropItem = new ItemStack(material, amount);
                    if (currentBoss != null) {
                        currentBoss.getWorld().dropItemNaturally(currentBoss.getLocation(), dropItem);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warning("[BossEvent] Invalid drop material: " + materialName);
                }
            }
        }

        logManager.logDisaster("BOSS: " + bc.displayName + " killed by " + killer.getName());
    }

    private void cleanup() {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
        if (bossMonitorTask != null) {
            bossMonitorTask.cancel();
            bossMonitorTask = null;
        }
        for (Projectile projectile : activeBossProjectiles) {
            if (projectile != null && !projectile.isDead()) {
                projectile.remove();
            }
        }
        activeBossProjectiles.clear();
        for (BukkitTask task : bossSkillTasks) {
            if (task != null) task.cancel();
        }
        bossSkillTasks.clear();
        bossProjectileConfigs.clear();
        bossSkillCooldowns.clear();

        if (currentBoss != null && !currentBoss.isDead()) {
            currentBoss.remove();
        }
        bossActive = false;
        currentBoss = null;
        currentBossSpawnLocation = null;
        currentBossActivityRadius = 100;
        anchorReturnTicks = 0;
        anchorReturnCooldownTicks = 0;
        currentBossName = null;
        pendingBoss = null;
        pendingBossLevel = null;
        warningTimeLeft = 0;
        lastBossLocation = null;
        stuckTicks = 0;
        currentBossLevel = 1;
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
        if (bc.hpByLevel != null) manualBoss.hpByLevel.putAll(bc.hpByLevel);
        if (bc.skills != null) manualBoss.skills.putAll(bc.skills);

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