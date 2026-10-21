package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Armadillo;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Bogged;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.GlowSquid;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Sniffer;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Squid;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.Turtle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.Wolf;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class DisasterManager {
   private final VnMineHardcore plugin;
   private final LogManager logManager;
   private final Random random = ThreadLocalRandom.current();
   private final ConfigManager config;
   private boolean disasterActive = false;
   private String currentDisaster = null;
   private String currentDisasterId = null;
   private BossBar warningBar;
   private int warningTimeLeft = 0;
   private int timeSinceLastDisaster = 0;
   private BukkitTask schedulerTask;
   private final Set<BukkitTask> ownedTasks = new HashSet<>();
   private final Map<UUID, DisasterManager.WorldState> originalWorldStates = new HashMap<>();
   private final Set<UUID> worldsWithModifiedTime = new HashSet<>();
   private final Set<UUID> worldsWithModifiedWeather = new HashSet<>();
   private final Map<UUID, Map<PotionEffectType, PotionEffect>> originalPotionEffects = new HashMap<>();
   private final Map<UUID, Set<PotionEffectType>> trackedPotionEffects = new HashMap<>();
   private final Map<UUID, DisasterManager.LivingEntityState> originalLivingEntityStates = new HashMap<>();
   private final Set<UUID> entitiesWithModifiedFire = new HashSet<>();
   private final Set<UUID> entitiesWithModifiedFreeze = new HashSet<>();
   private final Set<UUID> spawnedEntities = new HashSet<>();
   private static final Material CHAIN_MATERIAL = resolveChainMaterial();
   private static final Pattern VELOCITY_RANGE_PATTERN = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*-\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");
   private static final Set<Material> TRANSPARENT_BLOCKS = new HashSet<>(
      Arrays.asList(
         Material.AIR,
         Material.WATER,
         Material.LAVA,
         Material.BUBBLE_COLUMN,
         Material.GLASS,
         Material.WHITE_STAINED_GLASS,
         Material.ORANGE_STAINED_GLASS,
         Material.MAGENTA_STAINED_GLASS,
         Material.LIGHT_BLUE_STAINED_GLASS,
         Material.YELLOW_STAINED_GLASS,
         Material.LIME_STAINED_GLASS,
         Material.PINK_STAINED_GLASS,
         Material.GRAY_STAINED_GLASS,
         Material.LIGHT_GRAY_STAINED_GLASS,
         Material.CYAN_STAINED_GLASS,
         Material.PURPLE_STAINED_GLASS,
         Material.BLUE_STAINED_GLASS,
         Material.BROWN_STAINED_GLASS,
         Material.GREEN_STAINED_GLASS,
         Material.RED_STAINED_GLASS,
         Material.BLACK_STAINED_GLASS,
         Material.TINTED_GLASS,
         Material.GLASS_PANE,
         Material.WHITE_STAINED_GLASS_PANE,
         Material.ORANGE_STAINED_GLASS_PANE,
         Material.MAGENTA_STAINED_GLASS_PANE,
         Material.LIGHT_BLUE_STAINED_GLASS_PANE,
         Material.YELLOW_STAINED_GLASS_PANE,
         Material.LIME_STAINED_GLASS_PANE,
         Material.PINK_STAINED_GLASS_PANE,
         Material.GRAY_STAINED_GLASS_PANE,
         Material.LIGHT_GRAY_STAINED_GLASS_PANE,
         Material.CYAN_STAINED_GLASS_PANE,
         Material.PURPLE_STAINED_GLASS_PANE,
         Material.BLUE_STAINED_GLASS_PANE,
         Material.BROWN_STAINED_GLASS_PANE,
         Material.GREEN_STAINED_GLASS_PANE,
         Material.RED_STAINED_GLASS_PANE,
         Material.BLACK_STAINED_GLASS_PANE,
         Material.OAK_LEAVES,
         Material.SPRUCE_LEAVES,
         Material.BIRCH_LEAVES,
         Material.JUNGLE_LEAVES,
         Material.ACACIA_LEAVES,
         Material.DARK_OAK_LEAVES,
         Material.MANGROVE_LEAVES,
         Material.CHERRY_LEAVES,
         Material.AZALEA_LEAVES,
         Material.FLOWERING_AZALEA_LEAVES,
         Material.OAK_SLAB,
         Material.SPRUCE_SLAB,
         Material.BIRCH_SLAB,
         Material.JUNGLE_SLAB,
         Material.ACACIA_SLAB,
         Material.DARK_OAK_SLAB,
         Material.MANGROVE_SLAB,
         Material.CHERRY_SLAB,
         Material.OAK_STAIRS,
         Material.SPRUCE_STAIRS,
         Material.BIRCH_STAIRS,
         Material.JUNGLE_STAIRS,
         Material.ACACIA_STAIRS,
         Material.DARK_OAK_STAIRS,
         Material.MANGROVE_STAIRS,
         Material.CHERRY_STAIRS,
         Material.OAK_FENCE,
         Material.SPRUCE_FENCE,
         Material.BIRCH_FENCE,
         Material.JUNGLE_FENCE,
         Material.ACACIA_FENCE,
         Material.DARK_OAK_FENCE,
         Material.MANGROVE_FENCE,
         Material.CHERRY_FENCE,
         Material.OAK_FENCE_GATE,
         Material.SPRUCE_FENCE_GATE,
         Material.BIRCH_FENCE_GATE,
         Material.JUNGLE_FENCE_GATE,
         Material.ACACIA_FENCE_GATE,
         Material.DARK_OAK_FENCE_GATE,
         Material.MANGROVE_FENCE_GATE,
         Material.CHERRY_FENCE_GATE,
         Material.OAK_TRAPDOOR,
         Material.SPRUCE_TRAPDOOR,
         Material.BIRCH_TRAPDOOR,
         Material.JUNGLE_TRAPDOOR,
         Material.ACACIA_TRAPDOOR,
         Material.DARK_OAK_TRAPDOOR,
         Material.MANGROVE_TRAPDOOR,
         Material.CHERRY_TRAPDOOR,
         Material.VINE,
         Material.LADDER,
         Material.SCAFFOLDING,
         Material.IRON_BARS,
         Material.COBWEB,
         Material.TORCH,
         Material.SOUL_TORCH,
         Material.REDSTONE_TORCH,
         Material.LANTERN,
         Material.SOUL_LANTERN,
         Material.END_ROD,
         Material.SHORT_GRASS,
         Material.TALL_GRASS,
         Material.SNOW,
         Material.CHORUS_PLANT,
         Material.CHORUS_FLOWER,
         Material.COBWEB,
         Material.BAMBOO,
         Material.BAMBOO_SAPLING,
         Material.SUGAR_CANE,
         Material.CACTUS,
         Material.KELP,
         Material.KELP_PLANT,
         Material.SEAGRASS,
         Material.TALL_SEAGRASS,
         Material.LILY_PAD,
         Material.BIG_DRIPLEAF,
         Material.SMALL_DRIPLEAF,
         Material.HANGING_ROOTS,
         Material.ROOTED_DIRT,
         Material.MOSS_CARPET,
         Material.AZALEA,
         Material.FLOWERING_AZALEA,
         Material.BROWN_MUSHROOM,
         Material.RED_MUSHROOM,
         Material.CRIMSON_FUNGUS,
         Material.WARPED_FUNGUS,
         Material.CRIMSON_ROOTS,
         Material.WARPED_ROOTS,
         Material.NETHER_SPROUTS,
         Material.TWISTING_VINES,
         Material.TWISTING_VINES_PLANT,
         Material.WEEPING_VINES,
         Material.WEEPING_VINES_PLANT,
         Material.DEAD_BUSH,
         Material.FERN,
         Material.LARGE_FERN,
         Material.DANDELION,
         Material.POPPY,
         Material.BLUE_ORCHID,
         Material.ALLIUM,
         Material.AZURE_BLUET,
         Material.OXEYE_DAISY,
         Material.CORNFLOWER,
         Material.LILY_OF_THE_VALLEY,
         Material.WITHER_ROSE,
         Material.SUNFLOWER,
         Material.LILAC,
         Material.PEONY,
         Material.ROSE_BUSH,
         Material.PITCHER_PLANT,
         Material.TORCHFLOWER,
         Material.SPORE_BLOSSOM,
         Material.BELL,
         CHAIN_MATERIAL,
         Material.LIGHTNING_ROD,
         Material.DAYLIGHT_DETECTOR,
         Material.COMPARATOR,
         Material.REPEATER,
         Material.REDSTONE_WIRE,
         Material.REDSTONE_TORCH,
         Material.LEVER,
         Material.TRIPWIRE,
         Material.TRIPWIRE_HOOK,
         Material.PAINTING,
         Material.ITEM_FRAME,
         Material.GLOW_ITEM_FRAME,
         Material.FLOWER_POT,
         Material.DECORATED_POT,
         Material.BREWING_STAND,
         Material.CAULDRON,
         Material.WATER_CAULDRON,
         Material.LAVA_CAULDRON,
         Material.POWDER_SNOW_CAULDRON,
         Material.END_PORTAL,
         Material.END_GATEWAY,
         Material.NETHER_PORTAL,
         Material.LECTERN,
         Material.COMPOSTER,
         Material.HOPPER,
         Material.SNIFFER_EGG,
         Material.TURTLE_EGG,
         Material.FROGSPAWN,
         Material.CANDLE,
         Material.WHITE_CANDLE,
         Material.ORANGE_CANDLE,
         Material.MAGENTA_CANDLE,
         Material.LIGHT_BLUE_CANDLE,
         Material.YELLOW_CANDLE,
         Material.LIME_CANDLE,
         Material.PINK_CANDLE,
         Material.GRAY_CANDLE,
         Material.LIGHT_GRAY_CANDLE,
         Material.CYAN_CANDLE,
         Material.PURPLE_CANDLE,
         Material.BLUE_CANDLE,
         Material.BROWN_CANDLE,
         Material.GREEN_CANDLE,
         Material.RED_CANDLE,
         Material.BLACK_CANDLE,
         Material.CAKE,
         Material.AMETHYST_CLUSTER,
         Material.LARGE_AMETHYST_BUD,
         Material.MEDIUM_AMETHYST_BUD,
         Material.SMALL_AMETHYST_BUD,
         Material.POINTED_DRIPSTONE,
         Material.BIG_DRIPLEAF_STEM,
         Material.POWDER_SNOW,
         Material.BUBBLE_COLUMN
      )
   );
   private static final Set<Material> TREE_MATERIALS = new HashSet<>(
      Arrays.asList(
         Material.OAK_LOG,
         Material.SPRUCE_LOG,
         Material.BIRCH_LOG,
         Material.JUNGLE_LOG,
         Material.ACACIA_LOG,
         Material.DARK_OAK_LOG,
         Material.MANGROVE_LOG,
         Material.CHERRY_LOG,
         Material.OAK_WOOD,
         Material.SPRUCE_WOOD,
         Material.BIRCH_WOOD,
         Material.JUNGLE_WOOD,
         Material.ACACIA_WOOD,
         Material.DARK_OAK_WOOD,
         Material.MANGROVE_WOOD,
         Material.CHERRY_WOOD,
         Material.STRIPPED_OAK_LOG,
         Material.STRIPPED_SPRUCE_LOG,
         Material.STRIPPED_BIRCH_LOG,
         Material.STRIPPED_JUNGLE_LOG,
         Material.STRIPPED_ACACIA_LOG,
         Material.STRIPPED_DARK_OAK_LOG,
         Material.STRIPPED_MANGROVE_LOG,
         Material.STRIPPED_CHERRY_LOG,
         Material.OAK_LEAVES,
         Material.SPRUCE_LEAVES,
         Material.BIRCH_LEAVES,
         Material.JUNGLE_LEAVES,
         Material.ACACIA_LEAVES,
         Material.DARK_OAK_LEAVES,
         Material.MANGROVE_LEAVES,
         Material.CHERRY_LEAVES,
         Material.AZALEA_LEAVES,
         Material.FLOWERING_AZALEA_LEAVES,
         Material.MANGROVE_ROOTS,
         Material.MUDDY_MANGROVE_ROOTS
      )
   );
   private static final Set<Class<? extends Entity>> FRIENDLY_MOB_CLASSES = new HashSet<>(
      Arrays.asList(
         Cow.class,
         Sheep.class,
         Pig.class,
         Chicken.class,
         Rabbit.class,
         Horse.class,
         Donkey.class,
         Mule.class,
         Llama.class,
         TraderLlama.class,
         Wolf.class,
         Cat.class,
         Ocelot.class,
         Parrot.class,
         Villager.class,
         WanderingTrader.class,
         IronGolem.class,
         Snowman.class,
         Bee.class,
         Fox.class,
         Panda.class,
         PolarBear.class,
         Turtle.class,
         Dolphin.class,
         Squid.class,
         GlowSquid.class,
         Allay.class,
         Axolotl.class,
         Camel.class,
         Frog.class,
         Goat.class,
         Sniffer.class,
         Armadillo.class,
         Bogged.class
      )
   );

   private static Material resolveChainMaterial() {
      Material material = Material.matchMaterial("IRON_CHAIN");
      if (material == null) {
         material = Material.matchMaterial("CHAIN");
      }

      if (material == null) {
         throw new IllegalStateException("No chain block material is available.");
      } else {
         return material;
      }
   }

   public DisasterManager(VnMineHardcore plugin, ConfigManager config) {
      this.plugin = plugin;
      this.config = config;
      this.logManager = plugin.getLogManager();
      this.warningBar = BossBar.bossBar(Component.text(""), 1.0F, Color.RED, Overlay.PROGRESS);
      if (config.disastersEnabled) {
         this.start();
      }
   }

   public Set<String> getDisasterIds() {
      return this.config.disasterConfigs.keySet();
   }

   public String getDisasterName(String id) {
      ConfigManager.DisasterConfig dc = this.config.disasterConfigs.get(id.toLowerCase());
      return dc != null && dc.name != null && !dc.name.isEmpty() ? dc.name : id;
   }

   // ===== SAFE ZONE =====
   public boolean isPlayerSafe(Player player) {
      return !this.config.safeZoneEnabled ? false : this.isLocationSafe(player.getLocation());
   }

   private boolean isPlayerExposedToSky(Player player) {
      Location location = player.getLocation();
      World world = location.getWorld();
      return world == null ? false : world.getHighestBlockYAt(location.getBlockX(), location.getBlockZ()) < location.getBlockY();
   }

   private boolean canRainAt(Location location) {
      World world = location.getWorld();
      if (world == null) {
         return false;
      }

      String biome = location.getBlock().getBiome().getKey().getKey();

      boolean precipitationEnabled = switch (biome) {
         case "desert", "savanna", "savanna_plateau", "windswept_savanna", "badlands", "eroded_badlands", "wooded_badlands", "mushroom_fields", "nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas", "the_end", "small_end_islands", "end_midlands", "end_highlands", "end_barrens", "the_void" -> false;
         default -> true;
      };
      return precipitationEnabled && world.getTemperature(location.getBlockX(), location.getBlockY(), location.getBlockZ()) > 0.15;
   }

   private boolean isRainingOn(Player player) {
      Location location = player.getLocation();
      World world = location.getWorld();
      return world != null && world.hasStorm() && this.canRainAt(location) && this.isPlayerExposedToSky(player);
   }

   /**
    * Kiểm tra một vị trí bất kỳ có nằm trong khu vực an toàn (dưới mái che) không
    * @param loc Vị trí cần kiểm tra
    * @return true nếu vị trí nằm dưới mái che (có block rắn phía trên)
    */
   public boolean isLocationSafe(Location loc) {
      if (!this.config.safeZoneEnabled) {
         return false;
      }

      World world = loc.getWorld();
      if (world == null) {
         return false;
      }

      int blockX = loc.getBlockX();
      int blockY = loc.getBlockY();
      int blockZ = loc.getBlockZ();
      int checkRadius = this.config.safeZoneCheckRadius;
      // Scan the full column so high cave roofs still count as shelter.
      boolean hasSolidRoof = false;

      for (int y = blockY + 1; y < world.getMaxHeight(); y++) {
         Block blockAbove = world.getBlockAt(blockX, y, blockZ);
         if (!TRANSPARENT_BLOCKS.contains(blockAbove.getType())) {
            hasSolidRoof = true;
            break;
         }
      }

      if (!hasSolidRoof) {
         return false;
      }

      if (!this.config.safeZoneCheckWalls) {
         return true;
      }

      int solidWalls = 0;

      for (int dx = -checkRadius; dx <= checkRadius; dx++) {
         for (int dz = -checkRadius; dz <= checkRadius; dz++) {
            if (dx != 0 || dz != 0) {
               Block sideBlock = world.getBlockAt(blockX + dx, blockY, blockZ + dz);
               if (!TRANSPARENT_BLOCKS.contains(sideBlock.getType())) {
                  solidWalls++;
               }
            }
         }
      }

      return solidWalls >= this.config.safeZoneMinWalls;
   }

   private void sendSafeZoneMessage(Player player) {
      if (this.config.safeZoneEnabled) {
         if (this.isPlayerSafe(player)) {
            player.sendActionBar("§a§l\ud83c\udfe0 Bạn đang an toàn trong nhà!");
         } else if (this.disasterActive) {
            player.sendActionBar("§c§l⚠ Bạn đang ở ngoài trời! Vào nhà ngay!");
         }
      }
   }

   // ===== TRIGGER (MANUAL) =====
   public boolean triggerDisaster(String disasterId, final int warningTimeSeconds, final int durationSeconds) {
      if (!this.disasterActive && this.currentDisaster == null) {
         final String normalizedId = disasterId.toLowerCase(Locale.ROOT);
         ConfigManager.DisasterConfig dc = this.config.disasterConfigs.get(normalizedId);
         if (dc == null) {
            return false;
         }

         final String name = dc.name != null && !dc.name.isEmpty() ? dc.name : disasterId;
         this.currentDisaster = name;
         this.currentDisasterId = normalizedId;
         this.warningTimeLeft = warningTimeSeconds;
         this.broadcastWarning(name, warningTimeSeconds, dc);
         this.scheduleTaskTimer(new BukkitRunnable() {
            int cd = warningTimeSeconds;

            public void run() {
               this.cd--;
               if (this.cd <= 0) {
                  this.cancel();
                  DisasterManager.this.startDisasterById(normalizedId, durationSeconds);
               } else {
                  if (this.cd <= 5 || this.cd == 10 || this.cd == 30) {
                     DisasterManager.this.broadcastCountdown(name, this.cd);
                  }

                  if (DisasterManager.this.config.safeZoneEnabled && this.cd % 5 == 0) {
                     for (Player p : Bukkit.getOnlinePlayers()) {
                        DisasterManager.this.sendSafeZoneMessage(p);
                     }
                  }
               }
            }
         }, 0L, 20L);
         return true;
      } else {
         return false;
      }
   }

   // ===== SCHEDULER =====
   public void start() {
      if (this.config.disastersEnabled && (this.schedulerTask == null || this.schedulerTask.isCancelled())) {
         this.schedulerTask = (new BukkitRunnable() {
            public void run() {
               DisasterManager.this.pruneFinishedTasks();
               if (!Bukkit.getWorlds().isEmpty()) {
                  DisasterManager.this.timeSinceLastDisaster++;
                  int currentInterval = ConfigManager.parseRangeOrInt(DisasterManager.this.config.disasterMinIntervalRaw, 1200);
                  if (!DisasterManager.this.disasterActive && DisasterManager.this.timeSinceLastDisaster >= currentInterval) {
                     DisasterManager.this.tryScheduleDisaster();
                  }

                  if (DisasterManager.this.warningTimeLeft > 0) {
                     DisasterManager.this.warningTimeLeft--;
                     DisasterManager.this.updateWarningBar();
                     if (DisasterManager.this.warningTimeLeft <= 0) {
                        DisasterManager.this.warningBar.name(Component.text(""));
                        DisasterManager.this.warningBar.progress(0.0F);

                        for (Player p : Bukkit.getOnlinePlayers()) {
                           p.hideBossBar(DisasterManager.this.warningBar);
                        }
                     }
                  }
               }
            }
         }).runTaskTimer(this.plugin, 20L, 20L);
      }
   }

   public void stop() {
      if (this.schedulerTask != null) {
         this.schedulerTask.cancel();
         this.schedulerTask = null;
      }

      this.cancelOwnedTasks();
      this.removeSpawnedEntities();
      this.restoreDisasterState();
      this.clearCurrentDisaster();
   }

   public String stopCurrentDisaster() {
      if (this.currentDisaster == null) {
         return null;
      }

      String stoppedName = this.currentDisaster;
      this.cancelOwnedTasks();
      this.removeSpawnedEntities();
      this.restoreDisasterState();
      this.timeSinceLastDisaster = 0;
      this.clearCurrentDisaster();
      this.broadcast("§e§l⏹ " + stoppedName + " đã bị dừng bởi quản trị viên.");
      return stoppedName;
   }

   private void scheduleTaskTimer(BukkitRunnable task, long delayTicks, long periodTicks) {
      this.trackTask(task.runTaskTimer(this.plugin, delayTicks, periodTicks));
   }

   private void scheduleTaskLater(BukkitRunnable task, long delayTicks) {
      this.trackTask(task.runTaskLater(this.plugin, delayTicks));
   }

   private void trackTask(BukkitTask task) {
      this.pruneFinishedTasks();
      this.ownedTasks.add(task);
   }

   private void pruneFinishedTasks() {
      BukkitScheduler scheduler = Bukkit.getScheduler();
      this.ownedTasks.removeIf(owned -> owned.isCancelled() || !scheduler.isQueued(owned.getTaskId()) && !scheduler.isCurrentlyRunning(owned.getTaskId()));
   }

   private void cancelOwnedTasks() {
      for (BukkitTask task : this.ownedTasks) {
         task.cancel();
      }

      this.ownedTasks.clear();
   }

   private void finishDisaster() {
      this.restoreDisasterState();
      this.spawnedEntities.clear();
      this.clearCurrentDisaster();
   }

   private void trackSpawnedEntity(Entity entity) {
      this.spawnedEntities.add(entity.getUniqueId());
   }

   private void removeSpawnedEntities() {
      for (UUID entityId : this.spawnedEntities) {
         Entity entity = Bukkit.getEntity(entityId);
         if (entity != null) {
            entity.remove();
         }
      }

      this.spawnedEntities.clear();
   }

   private void clearCurrentDisaster() {
      this.disasterActive = false;
      this.currentDisaster = null;
      this.currentDisasterId = null;
      this.warningTimeLeft = 0;
      this.warningBar.name(Component.text(""));
      this.warningBar.progress(0.0F);

      for (Player player : Bukkit.getOnlinePlayers()) {
         player.hideBossBar(this.warningBar);
      }
   }

   private DisasterManager.WorldState rememberWorldState(World world) {
      return this.originalWorldStates
         .computeIfAbsent(
            world.getUID(),
            id -> new DisasterManager.WorldState(
               world, world.getTime(), world.hasStorm(), world.isThundering(), world.getWeatherDuration(), world.getThunderDuration()
            )
         );
   }

   private void restoreDisasterState() {
      for (Entry<UUID, DisasterManager.WorldState> entry : this.originalWorldStates.entrySet()) {
         DisasterManager.WorldState state = entry.getValue();
         if (this.worldsWithModifiedTime.contains(entry.getKey())) {
            state.world().setTime(state.time());
         }

         if (this.worldsWithModifiedWeather.contains(entry.getKey())) {
            state.world().setStorm(state.storm());
            state.world().setThundering(state.thundering());
            state.world().setWeatherDuration(state.weatherDuration());
            state.world().setThunderDuration(state.thunderDuration());
         }
      }

      this.originalWorldStates.clear();
      this.worldsWithModifiedTime.clear();
      this.worldsWithModifiedWeather.clear();

      for (Entry<UUID, Set<PotionEffectType>> entityEntry : this.trackedPotionEffects.entrySet()) {
         if (Bukkit.getEntity(entityEntry.getKey()) instanceof LivingEntity livingEntity) {
            Map<PotionEffectType, PotionEffect> previousEffects = this.originalPotionEffects.getOrDefault(entityEntry.getKey(), Collections.emptyMap());

            for (PotionEffectType effectType : entityEntry.getValue()) {
               livingEntity.removePotionEffect(effectType);
               PotionEffect previousEffect = previousEffects.get(effectType);
               if (previousEffect != null) {
                  livingEntity.addPotionEffect(previousEffect);
               }
            }
         }
      }

      this.trackedPotionEffects.clear();
      this.originalPotionEffects.clear();

      for (Entry<UUID, DisasterManager.LivingEntityState> entry : this.originalLivingEntityStates.entrySet()) {
         if (Bukkit.getEntity(entry.getKey()) instanceof LivingEntity livingEntity) {
            if (this.entitiesWithModifiedFire.contains(entry.getKey())) {
               livingEntity.setFireTicks(entry.getValue().fireTicks());
            }

            if (this.entitiesWithModifiedFreeze.contains(entry.getKey())) {
               livingEntity.setFreezeTicks(entry.getValue().freezeTicks());
            }
         }
      }

      this.originalLivingEntityStates.clear();
      this.entitiesWithModifiedFire.clear();
      this.entitiesWithModifiedFreeze.clear();
   }

   private void addTrackedPotionEffect(LivingEntity entity, PotionEffect effect) {
      UUID entityId = entity.getUniqueId();
      PotionEffectType effectType = effect.getType();
      Set<PotionEffectType> trackedTypes = this.trackedPotionEffects.computeIfAbsent(entityId, id -> new HashSet<>());
      if (trackedTypes.add(effectType)) {
         PotionEffect previousEffect = entity.getPotionEffect(effectType);
         if (previousEffect != null) {
            this.originalPotionEffects.computeIfAbsent(entityId, id -> new HashMap<>()).put(effectType, previousEffect);
         }
      }

      entity.addPotionEffect(effect);
   }

   private void setTrackedFireTicks(LivingEntity entity, int ticks) {
      UUID entityId = entity.getUniqueId();
      this.originalLivingEntityStates.computeIfAbsent(entityId, id -> new DisasterManager.LivingEntityState(entity.getFireTicks(), entity.getFreezeTicks()));
      this.entitiesWithModifiedFire.add(entityId);
      entity.setFireTicks(ticks);
   }

   private void setTrackedFreezeTicks(LivingEntity entity, int ticks) {
      UUID entityId = entity.getUniqueId();
      this.originalLivingEntityStates.computeIfAbsent(entityId, id -> new DisasterManager.LivingEntityState(entity.getFireTicks(), entity.getFreezeTicks()));
      this.entitiesWithModifiedFreeze.add(entityId);
      entity.setFreezeTicks(ticks);
   }

   public void reload() {
      this.stop();
      if (this.config.disastersEnabled) {
         this.start();
      }
   }

   public boolean isDisasterActive() {
      return this.disasterActive;
   }

   public String getCurrentDisaster() {
      return this.currentDisaster;
   }

   private void tryScheduleDisaster() {
      int currentInterval = ConfigManager.parseRangeOrInt(this.config.disasterMinIntervalRaw, 1200);
      if (this.timeSinceLastDisaster >= currentInterval) {
         if (this.currentDisaster == null) {
            List<Entry<String, ConfigManager.DisasterConfig>> eligibleDisasters = new ArrayList<>();

            for (Entry<String, ConfigManager.DisasterConfig> entry : this.config.disasterConfigs.entrySet()) {
               ConfigManager.DisasterConfig dc = entry.getValue();
               if (dc.enabled && !dc.actions.isEmpty() && dc.chance > 0 && this.canDisasterHappen(entry.getKey(), dc)) {
                  eligibleDisasters.add(entry);
               }
            }

            if (!eligibleDisasters.isEmpty()) {
               int totalChance = 0;

               for (Entry<String, ConfigManager.DisasterConfig> entry : eligibleDisasters) {
                  totalChance += entry.getValue().chance;
               }

               if (totalChance > 0) {
                  int roll = this.random.nextInt(totalChance);
                  int cumulative = 0;
                  Entry<String, ConfigManager.DisasterConfig> selected = null;

                  for (Entry<String, ConfigManager.DisasterConfig> entry : eligibleDisasters) {
                     cumulative += entry.getValue().chance;
                     if (roll < cumulative) {
                        selected = entry;
                        break;
                     }
                  }

                  if (selected != null) {
                     if (!this.rollDisasterSuccessProbability()) {
                        this.timeSinceLastDisaster = 0;
                     } else {
                        this.timeSinceLastDisaster = 0;
                        this.scheduleDisaster(selected.getKey(), selected.getValue());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean rollDisasterSuccessProbability() {
      int probability = this.config.disasterDefaultSuccessProbability;
      World primaryWorld = (World)Bukkit.getWorlds().get(0);
      long currentDay = Math.max(1L, primaryWorld.getFullTime() / 24000L + 1L);

      for (ConfigManager.DisasterSuccessThreshold threshold : this.config.disasterSuccessThresholds) {
         if (threshold.minDay > currentDay) {
            break;
         }

         probability = threshold.probabilityPercent;
      }

      return probability <= 0 ? false : probability >= 100 || this.random.nextInt(100) < probability;
   }

   /**
    * Kiểm tra điều kiện disaster có thể xảy ra không
    */
   private boolean canDisasterHappen(String disasterId, ConfigManager.DisasterConfig dc) {
      if (!dc.worlds.isEmpty() && Bukkit.getOnlinePlayers().stream().noneMatch(player -> this.matchesWorld(dc.worlds, player.getWorld()))) {
         return false;
      }

      boolean isNight = ((World)Bukkit.getWorlds().get(0)).getTime() > 13000L;
      boolean hasNether = false;
      boolean hasEnd = false;
      boolean hasOverworld = false;

      for (Player p : Bukkit.getOnlinePlayers()) {
         String env = p.getWorld().getEnvironment().name();
         if ("NETHER".equals(env)) {
            hasNether = true;
         } else if ("THE_END".equals(env)) {
            hasEnd = true;
         } else {
            hasOverworld = true;
         }
      }

      // Xác định disaster thuộc dimension nào dựa trên tên
      String id = disasterId.toLowerCase();
      boolean isNether = id.contains("inferno") || id.contains("soul") || id.contains("lava");
      boolean isEnd = id.contains("end-surge") || id.contains("void") || id.contains("chorus");
      if (isNether && !hasNether) {
         return false;
      } else if (isEnd && !hasEnd) {
         return false;
      } else if (!isNether && !isEnd && !hasOverworld) {
         return false;
      } else if (id.equals("blood-moon") && !isNight) {
         return false;
      } else {
         return id.equals("solar-flare") && isNight ? false : !id.equals("eclipse") || !isNight;
      }
   }

   // ===== SCHEDULE WITH WARNING =====
   private void scheduleDisaster(final String disasterId, final ConfigManager.DisasterConfig dc) {
      final String name = dc.name != null && !dc.name.isEmpty() ? dc.name : disasterId;
      this.currentDisaster = name;
      this.currentDisasterId = disasterId.toLowerCase(Locale.ROOT);
      this.warningTimeLeft = this.config.disasterWarningSeconds;
      this.broadcastWarning(name, this.config.disasterWarningSeconds, dc);
      this.logManager.logDisaster(name + " (WARNING)");
      this.scheduleTaskTimer(new BukkitRunnable() {
         int cd = DisasterManager.this.config.disasterWarningSeconds;

         public void run() {
            this.cd--;
            if (this.cd <= 0) {
               this.cancel();
               DisasterManager.this.startDisasterById(disasterId, dc.durationSeconds);
            } else {
               if (this.cd == 60 || this.cd == 30 || this.cd == 10 || this.cd <= 5) {
                  DisasterManager.this.broadcastCountdown(name, this.cd);
               }

               if (DisasterManager.this.config.safeZoneEnabled && this.cd % 5 == 0) {
                  for (Player p : Bukkit.getOnlinePlayers()) {
                     DisasterManager.this.sendSafeZoneMessage(p);
                  }
               }
            }
         }
      }, 0L, 20L);
   }

   // ===== START DISASTER (ACTION ENGINE) =====
   private void startDisasterById(String disasterId, int durationSecondsOverride) {
      String normalizedId = disasterId.toLowerCase(Locale.ROOT);
      if (!this.disasterActive && normalizedId.equals(this.currentDisasterId)) {
         final ConfigManager.DisasterConfig dc = this.config.disasterConfigs.get(normalizedId);
         if (dc == null) {
            this.clearCurrentDisaster();
         } else {
            this.disasterActive = true;
            final String name = dc.name != null && !dc.name.isEmpty() ? dc.name : disasterId;
            final int totalDurationTicks = (durationSecondsOverride > 0 ? durationSecondsOverride : dc.durationSeconds) * 20;
            String startTitle = dc.startTitle != null && !dc.startTitle.isEmpty() ? dc.startTitle : "§4§l⚠ " + name;
            String startSubtitle = dc.startSubtitle != null && !dc.startSubtitle.isEmpty() ? dc.startSubtitle : "§cĐã bắt đầu!";
            this.warningBar.name(Component.text("§4§l⚠ " + name + " ĐANG HOẠT ĐỘNG ⚠"));
            this.warningBar.progress(1.0F);

            for (Player p : Bukkit.getOnlinePlayers()) {
               p.showBossBar(this.warningBar);
               p.sendTitle(startTitle, startSubtitle, 10, 60, 20);
               p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0F, 0.5F);
            }

            this.logManager.logDisaster(name + " (ACTIVE)");
            String activeBroadcast = this.config.disasterMessages.getOrDefault("active-broadcast", "§4§l{name} - {message} (§e{duration}s§4)");
            this.broadcast(
               activeBroadcast.replace("{name}", name).replace("{message}", startSubtitle).replace("{duration}", String.valueOf(totalDurationTicks / 20))
            );
            this.broadcastDisasterDetails(dc);
            final int effectIntervalTicks = dc.effectIntervalSeconds * 20;
            this.scheduleTaskTimer(new BukkitRunnable() {
               int elapsed = 0;
               int tickCounter = 0;
               boolean started = false;

               public void run() {
                  if (!this.started) {
                     this.started = true;
                     DisasterManager.this.executeActions(dc.actions, dc);
                  }

                  this.elapsed += 20;
                  if (this.elapsed >= totalDurationTicks) {
                     this.cancel();
                     DisasterManager.this.executeActions(dc.onEnd, dc);
                     DisasterManager.this.finishDisaster();
                     String endBroadcast = DisasterManager.this.config.disasterMessages.getOrDefault("end-broadcast", "§a§l✅ {name} đã kết thúc!");
                     DisasterManager.this.broadcast(endBroadcast.replace("{name}", name));
                  } else {
                     DisasterManager.this.warningBar.progress(1.0F - (float)this.elapsed / totalDurationTicks);
                     this.tickCounter += 20;
                     if (this.tickCounter >= effectIntervalTicks) {
                        this.tickCounter = 0;
                        DisasterManager.this.executeActions(dc.actions, dc);
                     }

                     if (DisasterManager.this.config.safeZoneEnabled && this.tickCounter % 100 == 0) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                           DisasterManager.this.sendSafeZoneMessage(p);
                        }
                     }
                  }
               }
            }, 0L, 20L);
         }
      }
   }

   // ===== ACTION ENGINE =====
   /**
    * Thực thi danh sách actions từ config
    */
   private void executeActions(List<DisasterAction> actions, ConfigManager.DisasterConfig dc) {
      if (actions != null && !actions.isEmpty()) {
         for (DisasterAction action : actions) {
            try {
               this.executeSingleAction(action, dc);
            } catch (Exception e) {
               this.plugin.getLogger().warning("[Disaster] Error executing action " + action.type + ": " + e.getMessage());
            }
         }
      }
   }

   /**
    * Thực thi một action và chain-actions của nó
    */
   private void executeSingleAction(DisasterAction action, ConfigManager.DisasterConfig dc) {
      // Lấy danh sách targets dựa trên action targets
      List<Object> targets = this.getAffectedTargets(action, dc.worlds);
      if (!targets.isEmpty()) {
         for (Object target : targets) {
            this.executeActionOnTarget(action, target, dc);
         }

         // Thực thi chain-actions nếu có
         if (!action.chainActions.isEmpty()) {
            for (Object target : targets) {
               for (DisasterAction chainAction : action.chainActions) {
                  // Chain action thực thi trên vị trí của target chính
                  Location chainLocation = this.getTargetLocation(target);
                  if (chainLocation != null) {
                     this.executeChainActionAt(chainAction, chainLocation, target);
                  }
               }
            }
         }
      }
   }

   /**
    * Thực thi action trên một target cụ thể
    */
   private void executeActionOnTarget(DisasterAction action, Object target, ConfigManager.DisasterConfig dc) {
      final Location targetLoc = this.getTargetLocation(target);
      if (targetLoc != null) {
         switch (action.type) {
            case DAMAGE:
               double damage = this.getParamDouble(action.params, "damage", 2.0);
               boolean ignoreArmor = this.getParamBool(action.params, "ignore-armor", false);
               if (target instanceof LivingEntity le) {
                  le.damage(damage);
               }
               break;
            case POTION_EFFECT:
               List<Map<?, ?>> effectsList = (List<Map<?, ?>>)action.params.get("effects");
               if (effectsList != null && target instanceof LivingEntity le) {
                  for (Map<?, ?> effectMap : effectsList) {
                     String effectType = (String)effectMap.get("type");
                     int duration = this.toInt(effectMap.get("duration-ticks"), 100);
                     int amplifier = this.toInt(effectMap.get("amplifier"), 0);
                     if (effectType != null) {
                        try {
                           PotionEffectType pet = PotionEffectType.getByName(effectType.toUpperCase());
                           if (pet != null) {
                              this.addTrackedPotionEffect(le, new PotionEffect(pet, duration, amplifier, false, false));
                           }
                        } catch (Exception var31) {
                        }
                     }
                  }
               }
               break;
            case SPAWN_MOBS:
               Map<?, ?> mobsMap = (Map<?, ?>)action.params.get("mobs");
               if (mobsMap != null) {
                  int countPerTarget = this.getParamInt(action.params, "count-per-target", 1);
                  int radiusxxx = this.getParamInt(action.params, "radius", 10);
                  // Tính tổng weight
                  Map<String, Map<?, ?>> mobEntries = new HashMap<>();
                  int totalWeight = 0;

                  for (Entry<?, ?> entry : mobsMap.entrySet()) {
                     String entityType = entry.getKey().toString();
                     Map<?, ?> mobData = (Map<?, ?>)entry.getValue();
                     mobEntries.put(entityType, mobData);
                     totalWeight += this.toInt(mobData.get("weight"), 100);
                  }

                  for (int i = 0; i < countPerTarget && totalWeight > 0; i++) {
                     int roll = this.random.nextInt(totalWeight);
                     int cum = 0;

                     for (Entry<String, Map<?, ?>> entry : mobEntries.entrySet()) {
                        cum += this.toInt(entry.getValue().get("weight"), 100);
                        if (roll < cum) {
                           this.spawnMobAt(targetLoc, entry.getKey(), radiusxxx, entry.getValue());
                           break;
                        }
                     }
                  }
               }
               break;
            case LIGHTNING_STRIKE:
               int countx = this.getParamInt(action.params, "count-per-target", 1);
               int radiusxxx = this.getParamInt(action.params, "radius", 15);
               final float explosionPower = (float)this.getParamDouble(action.params, "explosion-power", 3.0);
               final boolean fire = this.getParamBool(action.params, "explosion-fire", false);
               final boolean breakBlocks = this.getParamBool(action.params, "explosion-break-blocks", true);
               int delayTicks = this.getParamInt(action.params, "delay-ticks", 0);
               final double damageMultiplier = this.getParamDouble(action.params, "damage-multiplier", 1.0);

               for (int i = 0; i < countx; i++) {
                  Location strikeLoc = this.getRandomLocationAround(targetLoc, radiusxxx);
                  if (delayTicks > 0) {
                     final Location finalTarget = strikeLoc;
                     this.scheduleTaskLater(new BukkitRunnable() {
                        public void run() {
                           DisasterManager.this.strikeLightningAt(finalTarget, explosionPower, fire, breakBlocks, damageMultiplier);
                        }
                     }, delayTicks);
                  } else {
                     this.strikeLightningAt(strikeLoc, explosionPower, fire, breakBlocks, damageMultiplier);
                  }
               }
               break;
            case EXPLOSION:
               int explosionCount = this.getParamInt(action.params, "count-per-target", 1);
               int explosionRadius = this.getParamInt(action.params, "radius", 15);
               float powerx = (float)this.getParamDouble(action.params, "explosion-power", 3.0);
               boolean explosionFire = this.getParamBool(action.params, "explosion-fire", false);
               boolean explosionBreakBlocks = this.getParamBool(action.params, "explosion-break-blocks", true);

               for (int i = 0; i < explosionCount; i++) {
                  Location explLoc = this.getRandomLocationAround(targetLoc, explosionRadius);
                  targetLoc.getWorld().createExplosion(explLoc, powerx, explosionFire, explosionBreakBlocks);
               }
               break;
            case SET_FIRE:
               int fireTicks = this.getParamInt(action.params, "fire-ticks", 100);
               if (target instanceof LivingEntity le) {
                  this.setTrackedFireTicks(le, fireTicks);
               } else if (target instanceof Block block && block.getType() == Material.AIR && !this.isProtected(block)) {
                  block.setType(Material.FIRE);
               }
               break;
            case PLACE_BLOCK:
               String blockType = (String)action.params.get("block-type");
               int placeBlockCount = this.getParamInt(action.params, "count-per-target", 3);
               int placeBlockRadius = this.getParamInt(action.params, "radius", 10);
               int placeHeight = this.getParamInt(action.params, "place-height", 1);
               if (blockType != null) {
                  Material material;
                  try {
                     material = Material.valueOf(blockType.toUpperCase());
                  } catch (Exception e) {
                     return;
                  }

                  for (int i = 0; i < placeBlockCount; i++) {
                     Location loc = this.getRandomLocationAround(targetLoc, placeBlockRadius);

                     for (int dy = 0; dy < placeHeight; dy++) {
                        Location blockLoc = loc.clone().add(0.0, dy, 0.0);
                        if (blockLoc.getBlock().getType() == Material.AIR && !this.isProtected(blockLoc.getBlock())) {
                           blockLoc.getBlock().setType(material);
                        }
                     }
                  }
               }
               break;
            case FALLING_BLOCK:
               int fallingBlockRadius = this.getParamInt(action.params, "radius", 15);
               int minY = this.getParamInt(action.params, "min-y", 30);
               int blockFallChance = this.getParamInt(action.params, "block-fall-chance", 30);
               double resistanceFactor = this.getParamDouble(action.params, "blast-resistance-factor", 0.1);
               int blocksPerTarget = this.getParamInt(action.params, "blocks-per-target", 5);

               for (int i = 0; i < blocksPerTarget; i++) {
                  int bx = targetLoc.getBlockX() + this.random.nextInt(fallingBlockRadius * 2) - fallingBlockRadius;
                  int bz = targetLoc.getBlockZ() + this.random.nextInt(fallingBlockRadius * 2) - fallingBlockRadius;
                  int by = minY + this.random.nextInt(60);
                  Block block = new Location(targetLoc.getWorld(), bx, by, bz).getBlock();
                  Material bt = block.getType();
                  if (!this.isProtected(block) && bt != Material.AIR && bt != Material.WATER && bt != Material.LAVA && bt != Material.BEDROCK) {
                     double blastRes = bt.getBlastResistance();
                     double fallProb = blockFallChance / (1.0 + blastRes * resistanceFactor);
                     if (this.random.nextDouble() * 100.0 < fallProb) {
                        FallingBlock fb = targetLoc.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0.0, 0.5), block.getBlockData());
                        this.trackSpawnedEntity(fb);
                        fb.setDropItem(true);
                        fb.setHurtEntities(true);
                        block.setType(Material.AIR);
                     }
                  }
               }
               break;
            case VELOCITY:
               double velYx = this.getParamDouble(action.params, "velocity-y", 1.5);
               String xRange = (String)action.params.get("velocity-x-range");
               String zRange = (String)action.params.get("velocity-z-range");
               if (target instanceof LivingEntity le) {
                  Vector v = le.getVelocity();
                  v.setY(v.getY() + velYx);
                  if (xRange != null) {
                     v.setX(v.getX() + this.randomVelocityFromRange(xRange));
                  } else {
                     v.setX(v.getX() + (this.random.nextDouble() - 0.5) * 2.0);
                  }

                  if (zRange != null) {
                     v.setZ(v.getZ() + this.randomVelocityFromRange(zRange));
                  } else {
                     v.setZ(v.getZ() + (this.random.nextDouble() - 0.5) * 2.0);
                  }

                  le.setVelocity(v);
                  le.setFallDistance(0.0F);
               }
               break;
            case SET_TIME:
            case WORLD_TIME:
               int time = this.getParamInt(action.params, "time", 1000);

               for (World w : this.getAffectedWorlds(dc)) {
                  this.rememberWorldState(w);
                  this.worldsWithModifiedTime.add(w.getUID());
                  w.setTime(time);
               }
               break;
            case SET_WEATHER:
            case WORLD_WEATHER:
               boolean storm = this.getParamBool(action.params, "storm", false);
               boolean thunder = this.getParamBool(action.params, "thunder", false);
               int durationTicks = this.getParamInt(action.params, "duration-ticks", 200000);

               for (World w : this.getAffectedWorlds(dc)) {
                  this.rememberWorldState(w);
                  this.worldsWithModifiedWeather.add(w.getUID());
                  w.setStorm(storm);
                  w.setThundering(thunder);
                  w.setWeatherDuration(durationTicks);
                  w.setThunderDuration(durationTicks);
               }
               break;
            case CLEAR_WEATHER:
               for (World w : this.getAffectedWorlds(dc)) {
                  this.rememberWorldState(w);
                  this.worldsWithModifiedWeather.add(w.getUID());
                  w.setStorm(false);
                  w.setThundering(false);
               }
               break;
            case BROADCAST:
               String messagex = (String)action.params.get("message");
               if (messagex != null) {
                  this.broadcast(messagex.replace("{name}", this.currentDisaster != null ? this.currentDisaster : ""));
               }
               break;
            case ACTION_BAR:
               String message = (String)action.params.get("message");
               if (message != null && target instanceof Player player) {
                  player.sendActionBar(message.replace("{name}", this.currentDisaster != null ? this.currentDisaster : ""));
               }
               break;
            case TELEPORT_RANDOM:
               int radiusx = this.getParamInt(action.params, "radius", 30);
               if (target instanceof Player p) {
                  Location randomLoc = this.getRandomLocationAround(p.getLocation(), radiusx);
                  randomLoc.setY(p.getWorld().getHighestBlockYAt(randomLoc) + 1);
                  p.teleport(randomLoc);
               }
               break;
            case PLAY_SOUND:
               String soundStr = (String)action.params.get("sound");
               if (soundStr != null) {
                  try {
                     Sound sound = Sound.valueOf(soundStr.toUpperCase());
                     float volume = (float)this.getParamDouble(action.params, "volume", 1.0);
                     float pitch = (float)this.getParamDouble(action.params, "pitch", 1.0);

                     for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), sound, volume, pitch);
                     }
                  } catch (Exception var36) {
                  }
               }
               break;
            case BLOCK_VELOCITY:
               double velY = this.getParamDouble(action.params, "velocity-y", 2.0);
               boolean damageOnLand = this.getParamBool(action.params, "damage-on-land", true);
               double damageAmount = this.getParamDouble(action.params, "damage-amount", 4.0);
               boolean replaceWithAir = this.getParamBool(action.params, "replace-with-air", true);
               if (target instanceof Block block) {
                  Material bt = block.getType();
                  if (!this.isProtected(block) && bt != Material.AIR && bt != Material.WATER && bt != Material.LAVA && bt != Material.BEDROCK) {
                     FallingBlock fb = targetLoc.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0.0, 0.5), block.getBlockData());
                     this.trackSpawnedEntity(fb);
                     fb.setDropItem(true);
                     fb.setHurtEntities(damageOnLand);
                     if (replaceWithAir) {
                        block.setType(Material.AIR);
                     }

                     fb.setVelocity(new Vector((this.random.nextDouble() - 0.5) * 2.0, velY, (this.random.nextDouble() - 0.5) * 2.0));
                  }
               }
               break;
            case BLOCK_EXPLOSION:
               int radius = this.getParamInt(action.params, "radius", 5);
               float power = (float)this.getParamDouble(action.params, "power", 2.0);
               boolean dropItems = this.getParamBool(action.params, "drop-items", true);
               if (target instanceof Block block) {
                  // Phá hủy block dạng nổ
                  block.getWorld().createExplosion(block.getLocation(), power, false, dropItems);
               } else {
                  // Nổ tại vị trí target
                  targetLoc.getWorld().createExplosion(targetLoc, power, false, dropItems);
               }
               break;
            case BLOCK_REPLACE:
               String fromMaterial = (String)action.params.get("from-material");
               String toMaterial = (String)action.params.get("to-material");
               int replaceRadius = this.getParamInt(action.params, "radius", 3);
               if (fromMaterial != null && toMaterial != null) {
                  Material fromMat;
                  Material toMat;
                  try {
                     fromMat = Material.valueOf(fromMaterial.toUpperCase());
                     toMat = Material.valueOf(toMaterial.toUpperCase());
                  } catch (Exception e) {
                     return;
                  }

                  for (int dx = -replaceRadius; dx <= replaceRadius; dx++) {
                     for (int dy = -replaceRadius; dy <= replaceRadius; dy++) {
                        for (int dz = -replaceRadius; dz <= replaceRadius; dz++) {
                           Block b = targetLoc.getBlock().getRelative(dx, dy, dz);
                           if (b.getType() == fromMat && !this.isProtected(b)) {
                              b.setType(toMat);
                           }
                        }
                     }
                  }
               }
               break;
            case BLOCK_IGNITE:
               int igniteRadius = this.getParamInt(action.params, "radius", 3);
               int fireTicksx = this.getParamInt(action.params, "fire-ticks", 100);

               for (int dx = -igniteRadius; dx <= igniteRadius; dx++) {
                  for (int dy = -igniteRadius; dy <= igniteRadius; dy++) {
                     for (int dz = -igniteRadius; dz <= igniteRadius; dz++) {
                        Block b = targetLoc.getBlock().getRelative(dx, dy, dz);
                        if (b.getType() == Material.AIR && b.getRelative(0, -1, 0).getType().isSolid() && !this.isProtected(b)) {
                           b.setType(Material.FIRE);
                        }
                     }
                  }
               }
               break;
            case BLOCK_LIQUID:
               String liquidType = (String)action.params.get("liquid-type");
               int liquidRadius = this.getParamInt(action.params, "radius", 3);
               if (liquidType != null) {
                  Material liquidMat;
                  try {
                     liquidMat = Material.valueOf(liquidType.toUpperCase());
                  } catch (Exception e) {
                     return;
                  }

                  if (liquidMat == Material.WATER || liquidMat == Material.LAVA) {
                     for (int dx = -liquidRadius; dx <= liquidRadius; dx++) {
                        for (int dy = -liquidRadius; dy <= liquidRadius; dy++) {
                           for (int dz = -liquidRadius; dz <= liquidRadius; dz++) {
                              Block b = targetLoc.getBlock().getRelative(dx, dy, dz);
                              if (b.getType() == Material.AIR && !this.isProtected(b)) {
                                 b.setType(liquidMat);
                              }
                           }
                        }
                     }
                  }
               }
               break;
            case BLOCK_FERTILIZE:
               int fertilizeRadius = this.getParamInt(action.params, "radius", 5);
               double bonemealChance = this.getParamDouble(action.params, "bonemeal-chance", 0.3);

               for (int dx = -fertilizeRadius; dx <= fertilizeRadius; dx++) {
                  for (int dz = -fertilizeRadius; dz <= fertilizeRadius; dz++) {
                     Block b = targetLoc.getBlock().getRelative(dx, 0, dz);
                     if (this.random.nextDouble() < bonemealChance && !this.isProtected(b)) {
                        b.applyBoneMeal(BlockFace.UP);
                     }
                  }
               }
               break;
            case ENTITY_PULL:
               double pullStrength = this.getParamDouble(action.params, "pull-strength", 1.0);
               int pullRadius = this.getParamInt(action.params, "radius", 10);

               for (Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, pullRadius, pullRadius, pullRadius)) {
                  if (e instanceof LivingEntity le && !(e instanceof Player)) {
                     Vector pull = targetLoc.toVector().subtract(le.getLocation().toVector()).normalize().multiply(pullStrength);
                     le.setVelocity(le.getVelocity().add(pull));
                  }
               }
               break;
            case ENTITY_PUSH:
               double pushStrength = this.getParamDouble(action.params, "push-strength", 1.0);
               int pushRadius = this.getParamInt(action.params, "radius", 10);

               for (Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, pushRadius, pushRadius, pushRadius)) {
                  if (e instanceof LivingEntity le && !(e instanceof Player)) {
                     Vector push = le.getLocation().toVector().subtract(targetLoc.toVector()).normalize().multiply(pushStrength);
                     le.setVelocity(le.getVelocity().add(push));
                  }
               }
               break;
            case ENTITY_FREEZE:
               int freezeDuration = this.getParamInt(action.params, "duration-ticks", 100);
               int freezeRadius = this.getParamInt(action.params, "radius", 5);

               for (Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, freezeRadius, freezeRadius, freezeRadius)) {
                  if (e instanceof LivingEntity le) {
                     this.setTrackedFreezeTicks(le, freezeDuration);
                  }
               }
               break;
            case ENTITY_DISMOUNT:
               int dismountRadius = this.getParamInt(action.params, "radius", 10);

               for (Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, dismountRadius, dismountRadius, dismountRadius)) {
                  if (e instanceof LivingEntity le) {
                     le.eject();
                     le.leaveVehicle();
                  }
               }
               break;
            case ENTITY_MOUNT:
               String mountType = (String)action.params.get("mount-type");
               int mountRadius = this.getParamInt(action.params, "radius", 10);
               if (mountType != null) {
                  try {
                     EntityType mountEntityType = EntityType.valueOf(mountType.toUpperCase());

                     for (Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, mountRadius, mountRadius, mountRadius)) {
                        if (e instanceof LivingEntity le && !(e instanceof Player)) {
                           Entity mount = targetLoc.getWorld().spawnEntity(le.getLocation(), mountEntityType);
                           this.trackSpawnedEntity(mount);
                           if (mount instanceof LivingEntity) {
                              le.addPassenger(mount);
                           }
                        }
                     }
                  } catch (Exception var33) {
                  }
               }
               break;
            case ITEM_DROP:
               String itemMaterial = (String)action.params.get("material");
               int itemAmount = this.getParamInt(action.params, "amount", 1);
               int itemRadius = this.getParamInt(action.params, "radius", 3);
               if (itemMaterial != null) {
                  try {
                     Material mat = Material.valueOf(itemMaterial.toUpperCase());

                     for (int i = 0; i < itemAmount; i++) {
                        Location dropLoc = this.getRandomLocationAround(targetLoc, itemRadius);
                        targetLoc.getWorld().dropItemNaturally(dropLoc, new ItemStack(mat, 1));
                     }
                  } catch (Exception var32) {
                  }
               }
               break;
            case FIREBALL_RAIN:
               int fireballCount = this.getParamInt(action.params, "count-per-target", 3);
               int fireballRadius = this.getParamInt(action.params, "radius", 20);
               int fireballHeight = this.getParamInt(action.params, "fireball-height", 60);
               double fireballSpeed = this.getParamDouble(action.params, "fireball-speed", 1.5);
               String fireballType = (String)action.params.get("fireball-type");
               final boolean isLarge = "LARGE".equalsIgnoreCase(fireballType);
               final float fireballExplosionPower = (float)this.getParamDouble(action.params, "explosion-power", 3.0);
               final boolean fireballIncendiary = this.getParamBool(action.params, "explosion-fire", true);
               final boolean fireballBreakBlocks = this.getParamBool(action.params, "explosion-break-blocks", true);
               final int fireballFireTicks = this.getParamInt(action.params, "fire-ticks", 100);
               final double fireballDamage = this.getParamDouble(action.params, "damage", 6.0);
               int fireballDelayTicks = this.getParamInt(action.params, "delay-ticks", 20);

               for (int i = 0; i < fireballCount; i++) {
                  Location impactLoc = this.getRandomLocationAround(targetLoc, fireballRadius);
                  double angle = this.random.nextDouble() * Math.PI * 2.0;
                  double horizOffset = 10.0 + this.random.nextDouble() * 20.0;
                  int spawnX = impactLoc.getBlockX() + (int)(Math.cos(angle) * horizOffset);
                  int spawnZ = impactLoc.getBlockZ() + (int)(Math.sin(angle) * horizOffset);
                  int spawnY = impactLoc.getBlockY() + fireballHeight;
                  final Location spawnLoc = new Location(targetLoc.getWorld(), spawnX + 0.5, spawnY, spawnZ + 0.5);
                  final Vector velocity = new Vector(impactLoc.getX() - spawnLoc.getX(), impactLoc.getY() - spawnLoc.getY(), impactLoc.getZ() - spawnLoc.getZ())
                     .normalize()
                     .multiply(fireballSpeed);
                  final Location finalImpactLoc = impactLoc;
                  this.scheduleTaskLater(new BukkitRunnable() {
                     public void run() {
                        try {
                           if (isLarge) {
                              Fireball fireball = (Fireball)targetLoc.getWorld().spawn(spawnLoc, Fireball.class);
                              DisasterManager.this.trackSpawnedEntity(fireball);
                              fireball.setVelocity(velocity);
                              fireball.setDirection(velocity);
                              fireball.setYield(fireballExplosionPower);
                              fireball.setIsIncendiary(fireballIncendiary);
                              DisasterManager.this.trackFireball(fireball, finalImpactLoc, fireballExplosionPower, fireballIncendiary, fireballBreakBlocks, fireballDamage, fireballFireTicks);
                           } else {
                              SmallFireball fireball = (SmallFireball)targetLoc.getWorld().spawn(spawnLoc, SmallFireball.class);
                              DisasterManager.this.trackSpawnedEntity(fireball);
                              fireball.setVelocity(velocity);
                              fireball.setDirection(velocity);
                              DisasterManager.this.trackFireball(fireball, finalImpactLoc, fireballExplosionPower, fireballIncendiary, fireballBreakBlocks, fireballDamage, fireballFireTicks);
                           }
                        } catch (Exception e) {
                           targetLoc.getWorld().createExplosion(finalImpactLoc, fireballExplosionPower, fireballIncendiary, fireballBreakBlocks);
                        }
                     }
                  }, (long)i * fireballDelayTicks);
               }
         }
      }
   }

   private double randomVelocityFromRange(String range) {
      Matcher matcher = VELOCITY_RANGE_PATTERN.matcher(range);
      if (!matcher.matches()) {
         throw new IllegalArgumentException("Invalid velocity range: " + range);
      } else {
         double min = Double.parseDouble(matcher.group(1));
         double max = Double.parseDouble(matcher.group(2));
         if (Double.isFinite(min) && Double.isFinite(max) && !(min > max)) {
            return min + this.random.nextDouble() * (max - min);
         } else {
            throw new IllegalArgumentException("Invalid velocity range: " + range);
         }
      }
   }

   /**
    * Thực thi chain action tại một vị trí cụ thể
    */
   private void executeChainActionAt(DisasterAction chainAction, Location location, Object originalTarget) {
      // Tạo một action tạm thời với target là vị trí hiện tại
      // Chain action luôn thực thi tại vị trí của target chính
      switch (chainAction.type) {
         case DAMAGE:
            double damage = this.getParamDouble(chainAction.params, "damage", 2.0);
            int damageRadius = this.getParamInt(chainAction.params, "radius", 5);
            boolean ignoreArmor = this.getParamBool(chainAction.params, "ignore-armor", false);

            for (Entity e : location.getWorld().getNearbyEntities(location, damageRadius, damageRadius, damageRadius)) {
               if (e instanceof LivingEntity le) {
                  le.damage(damage);
               }
            }
            break;
         case POTION_EFFECT:
            List<Map<?, ?>> effectsList = (List<Map<?, ?>>)chainAction.params.get("effects");
            if (effectsList != null) {
               int effectRadius = this.getParamInt(chainAction.params, "radius", 5);

               for (Entity e : location.getWorld().getNearbyEntities(location, effectRadius, effectRadius, effectRadius)) {
                  if (e instanceof LivingEntity le) {
                     for (Map<?, ?> effectMap : effectsList) {
                        String effectType = (String)effectMap.get("type");
                        int duration = this.toInt(effectMap.get("duration-ticks"), 100);
                        int amplifier = this.toInt(effectMap.get("amplifier"), 0);
                        if (effectType != null) {
                           try {
                              PotionEffectType pet = PotionEffectType.getByName(effectType.toUpperCase());
                              if (pet != null) {
                                 this.addTrackedPotionEffect(le, new PotionEffect(pet, duration, amplifier, false, false));
                              }
                           } catch (Exception var16) {
                           }
                        }
                     }
                  }
               }
            }
         case SPAWN_MOBS:
         case PLACE_BLOCK:
         case SET_TIME:
         case WORLD_TIME:
         case SET_WEATHER:
         case WORLD_WEATHER:
         case CLEAR_WEATHER:
         case BROADCAST:
         case ACTION_BAR:
         case TELEPORT_RANDOM:
         case BLOCK_EXPLOSION:
         case BLOCK_REPLACE:
         case BLOCK_IGNITE:
         case BLOCK_LIQUID:
         case BLOCK_FERTILIZE:
         case ENTITY_PULL:
         case ENTITY_PUSH:
         case ENTITY_FREEZE:
         case ENTITY_DISMOUNT:
         case ENTITY_MOUNT:
         default:
            break;
         case LIGHTNING_STRIKE: {
            float explosionPower = (float)this.getParamDouble(chainAction.params, "explosion-power", 0.0);
            boolean fire = this.getParamBool(chainAction.params, "fire", false);
            boolean breakBlocks = this.getParamBool(chainAction.params, "break-blocks", false);
            double damageMultiplier = this.getParamDouble(chainAction.params, "damage-multiplier", 1.0);
            this.strikeLightningAt(location, explosionPower, fire, breakBlocks, damageMultiplier);
            break;
         }
         case EXPLOSION: {
            float power = (float)this.getParamDouble(chainAction.params, "power", 2.0);
            boolean fire = this.getParamBool(chainAction.params, "fire", false);
            boolean breakBlocks = this.getParamBool(chainAction.params, "break-blocks", true);
            location.getWorld().createExplosion(location, power, fire, breakBlocks);
            break;
         }
         case SET_FIRE:
            int fireTicks = this.getParamInt(chainAction.params, "fire-ticks", 100);
            int fireRadius = this.getParamInt(chainAction.params, "radius", 3);

            for (int dx = -fireRadius; dx <= fireRadius; dx++) {
               for (int dy = -fireRadius; dy <= fireRadius; dy++) {
                  for (int dz = -fireRadius; dz <= fireRadius; dz++) {
                     Block b = location.getBlock().getRelative(dx, dy, dz);
                     if (b.getType() == Material.AIR && b.getRelative(0, -1, 0).getType().isSolid() && !this.isProtected(b)) {
                        b.setType(Material.FIRE);
                     }
                  }
               }
            }

            // Đốt entity trong bán kính
            for (Entity e : location.getWorld().getNearbyEntities(location, fireRadius, fireRadius, fireRadius)) {
               if (e instanceof LivingEntity le) {
                  this.setTrackedFireTicks(le, fireTicks);
               }
            }
            break;
         case FALLING_BLOCK:
            boolean damageOnLand = this.getParamBool(chainAction.params, "damage-on-land", true);
            double damageAmount = this.getParamDouble(chainAction.params, "damage-amount", 4.0);
            boolean replaceWithAir = this.getParamBool(chainAction.params, "replace-with-air", true);
            // Tạo falling block tại vị trí
            Block block = location.getBlock();
            if (!this.isProtected(block) && block.getType() != Material.AIR && block.getType() != Material.WATER && block.getType() != Material.LAVA) {
               FallingBlock fb = location.getWorld().spawnFallingBlock(location.add(0.5, 2.0, 0.5), block.getBlockData());
               this.trackSpawnedEntity(fb);
               fb.setDropItem(true);
               fb.setHurtEntities(damageOnLand);
               if (replaceWithAir) {
                  block.setType(Material.AIR);
               }
            }
            break;
         case VELOCITY:
            double velY = this.getParamDouble(chainAction.params, "velocity-y", 1.5);
            int velRadius = this.getParamInt(chainAction.params, "radius", 5);

            for (Entity e : location.getWorld().getNearbyEntities(location, velRadius, velRadius, velRadius)) {
               if (e instanceof LivingEntity le) {
                  Vector v = le.getVelocity();
                  v.setY(v.getY() + velY);
                  v.setX(v.getX() + (this.random.nextDouble() - 0.5) * 2.0);
                  v.setZ(v.getZ() + (this.random.nextDouble() - 0.5) * 2.0);
                  le.setVelocity(v);
               }
            }
            break;
         case PLAY_SOUND:
            String soundStr = (String)chainAction.params.get("sound");
            if (soundStr != null) {
               try {
                  Sound sound = Sound.valueOf(soundStr.toUpperCase());
                  float volume = (float)this.getParamDouble(chainAction.params, "volume", 1.0);
                  float pitch = (float)this.getParamDouble(chainAction.params, "pitch", 1.0);
                  location.getWorld().playSound(location, sound, volume, pitch);
               } catch (Exception var15) {
               }
            }
            break;
         case BLOCK_VELOCITY:
            double blockVelocityY = this.getParamDouble(chainAction.params, "velocity-y", 2.0);
            boolean damageOnLandx = this.getParamBool(chainAction.params, "damage-on-land", true);
            boolean replaceWithAirx = this.getParamBool(chainAction.params, "replace-with-air", true);
            int bvRadius = this.getParamInt(chainAction.params, "radius", 3);

            for (int dx = -bvRadius; dx <= bvRadius; dx++) {
               for (int dz = -bvRadius; dz <= bvRadius; dz++) {
                  Block b = location.getBlock().getRelative(dx, 0, dz);
                  Material bt = b.getType();
                  if (!this.isProtected(b)
                     && bt != Material.AIR
                     && bt != Material.WATER
                     && bt != Material.LAVA
                     && bt != Material.BEDROCK
                     && this.random.nextInt(3) == 0) {
                     FallingBlock fb = location.getWorld().spawnFallingBlock(b.getLocation().add(0.5, 0.0, 0.5), b.getBlockData());
                     this.trackSpawnedEntity(fb);
                     fb.setDropItem(true);
                     fb.setHurtEntities(damageOnLandx);
                     if (replaceWithAirx) {
                        b.setType(Material.AIR);
                     }

                     fb.setVelocity(new Vector((this.random.nextDouble() - 0.5) * 2.0, blockVelocityY, (this.random.nextDouble() - 0.5) * 2.0));
                  }
               }
            }
            break;
         case ITEM_DROP:
            String itemMaterial = (String)chainAction.params.get("material");
            int itemAmount = this.getParamInt(chainAction.params, "amount", 1);
            if (itemMaterial != null) {
               try {
                  Material mat = Material.valueOf(itemMaterial.toUpperCase());

                  for (int i = 0; i < itemAmount; i++) {
                     location.getWorld().dropItemNaturally(location, new ItemStack(mat, 1));
                  }
               } catch (Exception var17) {
               }
            }
      }
   }

   // ===== TARGET SYSTEM =====
   /**
    * Lấy danh sách các đối tượng target dựa trên action targets
    * Trả về List<Object> có thể chứa Player, LivingEntity, Block, Location
    */
   private List<Object> getAffectedTargets(DisasterAction action, List<String> eventWorlds) {
      List<Object> result = new ArrayList<>();
      boolean requireOutdoor = action.condition.requireOutdoor;
      boolean ignoreSafeZone = action.condition.ignoreSafeZone;
      boolean requireRain = action.condition.requireRain;
      // Nếu không có targets, mặc định là tất cả player
      if (action.targets.isEmpty()) {
         for (Player p : Bukkit.getOnlinePlayers()) {
            if (this.matchesOptionalWorlds(eventWorlds, p.getWorld())
               && (!requireRain || this.isRainingOn(p))
               && (!requireOutdoor || this.isPlayerExposedToSky(p) && (ignoreSafeZone || !this.isPlayerSafe(p)))) {
               result.add(p);
            }
         }

         return result;
      } else {
         // Xử lý từng target type
         for (DisasterAction.ActionTarget target : action.targets) {
            String entityType = target.entityType;
            int weight = target.weight;
            int radius = target.radius;
            if (!entityType.equals("player") && !entityType.equals("all_players")) {
               if (!entityType.equals("friendly_mobs") && !entityType.equals("hostile_mobs") && !entityType.equals("all_mobs")) {
                  if (entityType.equals("ground")) {
                     for (Player p : Bukkit.getOnlinePlayers()) {
                        if (this.matchesWorldScope(eventWorlds, target.worlds, p.getWorld()) && (!requireOutdoor || ignoreSafeZone || !this.isPlayerSafe(p))) {
                           Block ground = p.getLocation().getBlock().getRelative(0, -1, 0);
                           if (ground.getType() != Material.AIR
                              && ground.getType() != Material.WATER
                              && ground.getType() != Material.LAVA
                              && (!requireOutdoor || ignoreSafeZone || !this.isLocationSafe(ground.getLocation()))) {
                              result.add(ground);
                           }
                        }
                     }
                  } else if (entityType.equals("trees")) {
                     for (Player p : Bukkit.getOnlinePlayers()) {
                        if (this.matchesWorldScope(eventWorlds, target.worlds, p.getWorld()) && (!requireOutdoor || ignoreSafeZone || !this.isPlayerSafe(p))) {
                           for (int dx = -radius; dx <= radius; dx++) {
                              for (int dy = -radius; dy <= radius; dy++) {
                                 for (int dz = -radius; dz <= radius; dz++) {
                                    Block b = p.getLocation().getBlock().getRelative(dx, dy, dz);
                                    if (TREE_MATERIALS.contains(b.getType()) && (!requireOutdoor || ignoreSafeZone || !this.isLocationSafe(b.getLocation()))) {
                                       result.add(b);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  } else if (entityType.equals("blocks")) {
                     for (Player p : Bukkit.getOnlinePlayers()) {
                        if (this.matchesWorldScope(eventWorlds, target.worlds, p.getWorld()) && (!requireOutdoor || ignoreSafeZone || !this.isPlayerSafe(p))) {
                           for (int dx = -radius; dx <= radius; dx++) {
                              for (int dy = -radius; dy <= radius; dy++) {
                                 for (int dz = -radius; dz <= radius; dz++) {
                                    Block b = p.getLocation().getBlock().getRelative(dx, dy, dz);
                                    if (!TRANSPARENT_BLOCKS.contains(b.getType())
                                       && b.getType().isSolid()
                                       && (!requireOutdoor || ignoreSafeZone || !this.isLocationSafe(b.getLocation()))) {
                                       result.add(b);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  } else if (entityType.equals("metal_blocks")) {
                     Set<Material> METAL_MATERIALS = new HashSet<>(
                        Arrays.asList(
                           Material.IRON_BLOCK,
                           Material.GOLD_BLOCK,
                           Material.COPPER_BLOCK,
                           Material.WAXED_COPPER_BLOCK,
                           Material.EXPOSED_COPPER,
                           Material.WEATHERED_COPPER,
                           Material.OXIDIZED_COPPER,
                           Material.WAXED_EXPOSED_COPPER,
                           Material.WAXED_WEATHERED_COPPER,
                           Material.WAXED_OXIDIZED_COPPER,
                           Material.CUT_COPPER,
                           Material.EXPOSED_CUT_COPPER,
                           Material.WEATHERED_CUT_COPPER,
                           Material.OXIDIZED_CUT_COPPER,
                           Material.WAXED_CUT_COPPER,
                           Material.WAXED_EXPOSED_CUT_COPPER,
                           Material.WAXED_WEATHERED_CUT_COPPER,
                           Material.WAXED_OXIDIZED_CUT_COPPER,
                           Material.NETHERITE_BLOCK,
                           Material.LIGHTNING_ROD,
                           Material.IRON_ORE,
                           Material.GOLD_ORE,
                           Material.COPPER_ORE,
                           Material.DEEPSLATE_IRON_ORE,
                           Material.DEEPSLATE_GOLD_ORE,
                           Material.DEEPSLATE_COPPER_ORE,
                           Material.NETHER_GOLD_ORE,
                           Material.RAW_IRON_BLOCK,
                           Material.RAW_GOLD_BLOCK,
                           Material.RAW_COPPER_BLOCK,
                           Material.ANVIL,
                           Material.CHIPPED_ANVIL,
                           Material.DAMAGED_ANVIL,
                           Material.CAULDRON,
                           Material.HOPPER,
                           Material.IRON_BARS,
                           Material.IRON_DOOR,
                           Material.IRON_TRAPDOOR,
                           Material.LANTERN,
                           Material.SOUL_LANTERN,
                           CHAIN_MATERIAL
                        )
                     );

                     for (Player p : Bukkit.getOnlinePlayers()) {
                        if (this.matchesWorldScope(eventWorlds, target.worlds, p.getWorld()) && (!requireOutdoor || ignoreSafeZone || !this.isPlayerSafe(p))) {
                           for (int dx = -radius; dx <= radius; dx++) {
                              for (int dy = -radius; dy <= radius; dy++) {
                                 for (int dz = -radius; dz <= radius; dz++) {
                                    Block b = p.getLocation().getBlock().getRelative(dx, dy, dz);
                                    if (METAL_MATERIALS.contains(b.getType()) && (!requireOutdoor || ignoreSafeZone || !this.isLocationSafe(b.getLocation()))) {
                                       result.add(b);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  } else if (entityType.equals("surface_blocks")) {
                     for (Player p : Bukkit.getOnlinePlayers()) {
                        if (this.matchesWorldScope(eventWorlds, target.worlds, p.getWorld()) && (!requireOutdoor || ignoreSafeZone || !this.isPlayerSafe(p))) {
                           Location loc = p.getLocation();
                           int highestY = loc.getWorld().getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
                           Block surface = loc.getWorld().getBlockAt(loc.getBlockX(), highestY, loc.getBlockZ());
                           if (!TRANSPARENT_BLOCKS.contains(surface.getType())
                              && (!requireOutdoor || ignoreSafeZone || !this.isLocationSafe(surface.getLocation()))) {
                              result.add(surface);
                           }
                        }
                     }
                  } else if (!entityType.startsWith("random_blocks:")) {
                     if (entityType.startsWith("random:")) {
                        // Random từ array target types
                        String targetsStr = entityType.substring("random:".length());
                        String[] targetTypes = targetsStr.split(",");
                        List<String> targetList = Arrays.asList(targetTypes);
                        String selectedType = targetList.get(this.random.nextInt(targetList.size()));
                        // Tạo action target tạm thời và đệ quy
                        DisasterAction.ActionTarget tempTarget = new DisasterAction.ActionTarget();
                        tempTarget.entityType = selectedType.trim();
                        tempTarget.weight = weight;
                        tempTarget.radius = radius;
                        tempTarget.worlds = target.worlds;
                        tempTarget.blockMaterials = target.blockMaterials;
                        DisasterAction tempAction = new DisasterAction(action.type);
                        tempAction.targets.add(tempTarget);
                        tempAction.condition = action.condition;
                        result.addAll(this.getAffectedTargets(tempAction, eventWorlds));
                     }
                  } else {
                     // Random blocks from material list
                     String materialsStr = entityType.substring("random_blocks:".length());
                     String[] materialNames = materialsStr.split(",");
                     List<Material> targetMaterials = new ArrayList<>();

                     for (String matName : materialNames) {
                        try {
                           targetMaterials.add(Material.valueOf(matName.trim().toUpperCase()));
                        } catch (Exception var21) {
                        }
                     }

                     if (!targetMaterials.isEmpty()) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                           if (this.matchesWorldScope(eventWorlds, target.worlds, p.getWorld()) && (!requireOutdoor || ignoreSafeZone || !this.isPlayerSafe(p))
                              )
                            {
                              for (int dx = -radius; dx <= radius; dx++) {
                                 for (int dy = -radius; dy <= radius; dy++) {
                                    for (int dz = -radius; dz <= radius; dz++) {
                                       Block b = p.getLocation().getBlock().getRelative(dx, dy, dz);
                                       if (targetMaterials.contains(b.getType())
                                          && (!requireOutdoor || ignoreSafeZone || !this.isLocationSafe(b.getLocation()))) {
                                          result.add(b);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               } else {
                  for (Player p : Bukkit.getOnlinePlayers()) {
                     if (this.matchesWorldScope(eventWorlds, target.worlds, p.getWorld()) && (!requireOutdoor || ignoreSafeZone || !this.isPlayerSafe(p))) {
                        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius, radius, radius)) {
                           if (e instanceof LivingEntity le && !(e instanceof Player)) {
                              boolean isFriendly = this.isFriendlyMob(le);
                              boolean isHostile = !isFriendly && !(le instanceof Boss);
                              if (entityType.equals("friendly_mobs") && isFriendly) {
                                 result.add(le);
                              } else if (entityType.equals("hostile_mobs") && isHostile) {
                                 result.add(le);
                              } else if (entityType.equals("all_mobs")) {
                                 result.add(le);
                              }
                           }
                        }
                     }
                  }
               }
            } else {
               // Player targets
               List<Player> players = new ArrayList<>();

               for (Player p : Bukkit.getOnlinePlayers()) {
                  if (this.matchesWorldScope(eventWorlds, target.worlds, p.getWorld())
                     && (!requireRain || this.isRainingOn(p))
                     && (!requireOutdoor || this.isPlayerExposedToSky(p) && (ignoreSafeZone || !this.isPlayerSafe(p)))) {
                     players.add(p);
                  }
               }

               if (entityType.equals("all_players")) {
                  result.addAll(players);
               } else if (!players.isEmpty()) {
                  result.add(players.get(this.random.nextInt(players.size())));
               }
            }
         }

         // Giới hạn số lượng target để tránh lag
         int maxTargets = this.getParamInt(action.params, "max-targets", 50);
         if (result.size() > maxTargets) {
            // Random subset
            Collections.shuffle(result, this.random);
            result = result.subList(0, maxTargets);
         }

         return result;
      }
   }

   private boolean matchesWorld(List<String> worldFilters, World world) {
      if (worldFilters.stream().anyMatch(filter -> filter.equalsIgnoreCase("all"))) {
         return true;
      }

      String worldName = world.getName();
      String environmentName = world.getEnvironment().name();

      String configuredEnvironment = switch (world.getEnvironment()) {
         case NORMAL -> "overworld";
         case NETHER -> "nether";
         case THE_END -> "end";
         default -> "";
      };
      return worldFilters.stream()
         .anyMatch(
            filter -> filter.equalsIgnoreCase(worldName)
               || filter.equalsIgnoreCase(environmentName)
               || filter.equalsIgnoreCase(configuredEnvironment)
               || filter.equalsIgnoreCase("the_" + configuredEnvironment)
         );
   }

   private boolean matchesOptionalWorlds(List<String> worldFilters, World world) {
      return worldFilters == null || worldFilters.isEmpty() || this.matchesWorld(worldFilters, world);
   }

   private boolean matchesWorldScope(List<String> eventWorlds, List<String> targetWorlds, World world) {
      return this.matchesOptionalWorlds(eventWorlds, world) && this.matchesOptionalWorlds(targetWorlds, world);
   }

   private List<World> getAffectedWorlds(ConfigManager.DisasterConfig dc) {
      return Bukkit.getWorlds()
         .stream()
         .filter(world -> this.matchesOptionalWorlds(dc.worlds, world))
         .filter(
            world -> dc.actions.isEmpty()
               || dc.actions
                  .stream()
                  .anyMatch(action -> action.targets.isEmpty() || action.targets.stream().anyMatch(target -> this.matchesOptionalWorlds(target.worlds, world)))
         )
         .toList();
   }

   private String getAffectedWorldNames(ConfigManager.DisasterConfig dc) {
      List<String> loadedWorlds = this.getAffectedWorlds(dc).stream().<String>map(WorldInfo::getName).toList();
      if (!loadedWorlds.isEmpty()) {
         return String.join(", ", loadedWorlds);
      } else {
         return !dc.worlds.isEmpty() ? String.join(", ", dc.worlds) : "tất cả world đang tải";
      }
   }

   private void broadcastDisasterDetails(ConfigManager.DisasterConfig dc) {
      this.broadcast("§6§lWorld bị ảnh hưởng: §f" + this.getAffectedWorldNames(dc));
   }

   /**
    * Lấy vị trí của target
    */
   private Location getTargetLocation(Object target) {
      if (target instanceof Player p) {
         return p.getLocation();
      } else if (target instanceof LivingEntity le) {
         return le.getLocation();
      } else if (target instanceof Block b) {
         return b.getLocation();
      } else if (target instanceof Location loc) {
         return loc;
      } else {
         return target instanceof Entity e ? e.getLocation() : null;
      }
   }

   /**
    * Kiểm tra entity có phải friendly mob không
    */
   private boolean isFriendlyMob(LivingEntity entity) {
      for (Class<? extends Entity> clazz : FRIENDLY_MOB_CLASSES) {
         if (clazz.isInstance(entity)) {
            return true;
         }
      }

      return false;
   }

   // ===== HELPERS =====
   private Location getRandomLocationAround(Location center, int radius) {
      int x = center.getBlockX() + this.random.nextInt(radius * 2) - radius;
      int z = center.getBlockZ() + this.random.nextInt(radius * 2) - radius;
      int y = center.getWorld().getHighestBlockYAt(x, z) + 1;
      return new Location(center.getWorld(), x, y, z);
   }

   /**
    * Theo dõi fireball và kích nổ khi đến gần mặt đất
    */
   private void trackFireball(
      final Projectile fireball,
      final Location impactLoc,
      final float explosionPower,
      final boolean fire,
      final boolean breakBlocks,
      final double damage,
      final int fireTicks
   ) {
      this.scheduleTaskTimer(new BukkitRunnable() {
         int ticks = 0;

         public void run() {
            if (!fireball.isDead() && fireball.isValid()) {
               this.ticks++;
               // Kiểm tra nếu fireball ở gần mặt đất (Y thấp) hoặc chạm block
               Location loc = fireball.getLocation();
               Block blockBelow = loc.getBlock().getRelative(0, -1, 0);
               if (!(loc.getY() <= impactLoc.getY() + 3.0) && blockBelow.getType() == Material.AIR && loc.getBlock().getType() == Material.AIR) {
                  // Timeout sau 10 giây
                  if (this.ticks > 200) {
                     this.cancel();
                     fireball.remove();
                  }
               } else {
                  this.cancel();
                  loc.getWorld().createExplosion(loc, explosionPower, fire, breakBlocks);
                  if (fire) {
                     DisasterManager.this.igniteArea(loc, 5, fireTicks);
                  }

                  DisasterManager.this.damageEntitiesInRadius(loc, damage, 6);
                  fireball.remove();
               }
            } else {
               this.cancel();
               // Kích nổ tại vị trí hiện tại nếu fireball chết
               Location currentLoc = fireball.getLocation();
               currentLoc.getWorld().createExplosion(currentLoc, explosionPower, fire, breakBlocks);
               if (fire) {
                  DisasterManager.this.igniteArea(currentLoc, 5, fireTicks);
               }

               DisasterManager.this.damageEntitiesInRadius(currentLoc, damage, 6);
            }
         }
      }, 0L, 1L);
   }

   /**
    * Đốt cháy khu vực xung quanh
    */
   private void igniteArea(Location center, int radius, int fireTicks) {
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (!(this.random.nextDouble() > 0.3)) {
               Block block = center.getBlock().getRelative(dx, 0, dz);
               if (block.getType() == Material.AIR && block.getRelative(0, -1, 0).getType().isSolid() && !this.isProtected(block)) {
                  block.setType(Material.FIRE);
               }
            }
         }
      }
   }

   /**
    * Gây damage cho entity trong bán kính
    */
   private void damageEntitiesInRadius(Location center, double damage, int radius) {
      for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
         if (e instanceof LivingEntity le && !(e instanceof Player)) {
            double distance = le.getLocation().distance(center);
            double dmg = damage * (1.0 - distance / radius);
            if (dmg > 0.0) {
               le.damage(dmg);
            }
         }
      }
   }

   private void spawnMobAt(Location location, String entityTypeStr, int radius, Map<?, ?> mobData) {
      try {
         EntityType entityType = EntityType.valueOf(entityTypeStr.toUpperCase());
         Location spawnLoc = this.getRandomLocationAround(location, radius);
         Entity entity = location.getWorld().spawnEntity(spawnLoc, entityType);
         this.trackSpawnedEntity(entity);
         // Apply potion effects from mob config
         List<Map<?, ?>> effectsList = (List<Map<?, ?>>)mobData.get("effects");
         if (effectsList != null && entity instanceof LivingEntity le) {
            for (Map<?, ?> effectMap : effectsList) {
               String effectType = (String)effectMap.get("type");
               int duration = this.toInt(effectMap.get("duration-ticks"), 600);
               int amplifier = this.toInt(effectMap.get("amplifier"), 0);
               if (effectType != null) {
                  try {
                     PotionEffectType pet = PotionEffectType.getByName(effectType.toUpperCase());
                     if (pet != null) {
                        this.addTrackedPotionEffect(le, new PotionEffect(pet, duration, amplifier, true, false));
                     }
                  } catch (Exception var16) {
                  }
               }
            }
         }
      } catch (IllegalArgumentException e) {
         this.plugin.getLogger().warning("[Disaster] Invalid entity type: " + entityTypeStr);
      }
   }

   private void strikeLightningAt(final Location loc, final float explosionPower, final boolean fire, final boolean breakBlocks, final double damageMultiplier) {
      loc.getWorld().strikeLightningEffect(new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY() + 30, loc.getBlockZ()));
      if (explosionPower > 0.0F) {
         this.scheduleTaskLater(new BukkitRunnable() {
            public void run() {
               loc.getWorld().createExplosion(loc, explosionPower, fire, breakBlocks);

               for (Entity e : loc.getWorld().getNearbyEntities(loc, 8.0, 8.0, 8.0)) {
                  if (e instanceof LivingEntity le) {
                     double dmg = 8.0 * damageMultiplier * (1.0 - le.getLocation().distance(loc) / 8.0);
                     if (dmg > 0.0) {
                        le.damage(dmg);
                     }
                  }
               }
            }
         }, 30L);
      }
   }

   private boolean isProtected(Block block) {
      SpawnProtectionManager manager = this.plugin.getSpawnProtectionManager();
      return manager != null && manager.isProtected(block.getLocation());
   }

   private void broadcast(String message) {
      Bukkit.broadcastMessage(message);
   }

   private void broadcastWarning(String name, int seconds, ConfigManager.DisasterConfig dc) {
      String warningTitle = this.config.disasterMessages.getOrDefault("warning-title", "§4§l⚠ CẢNH BÁO THIÊN TAI ⚠");
      String warningSubtitle = this.config.disasterMessages.getOrDefault("warning-subtitle", "§c{name}\n§e§lSẽ xảy ra trong {time} giây!");
      String warningBroadcast = this.config.disasterMessages.getOrDefault("warning-broadcast", "§4§l⚠ {name} §r§cđang đến gần!");

      for (Player p : Bukkit.getOnlinePlayers()) {
         p.showBossBar(this.warningBar);
         p.sendTitle(warningTitle, warningSubtitle.replace("{name}", name).replace("{time}", String.valueOf(seconds)), 10, 70, 20);
         p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1.0F, 0.5F);
         p.sendMessage(warningBroadcast.replace("{name}", name));
      }

      this.broadcastDisasterDetails(dc);
      this.logManager.logDisaster(name + " (WARNING - " + seconds + "s)");
   }

   private void broadcastCountdown(String name, int seconds) {
      String countdownMsg = this.config.disasterMessages.getOrDefault("countdown-broadcast", "§4§l⚠ {name} §csẽ xảy ra trong §4§l{time}§c giây!");
      this.broadcast(countdownMsg.replace("{name}", name).replace("{time}", String.valueOf(seconds)));
   }

   private void updateWarningBar() {
      if (this.warningTimeLeft > 0 && this.currentDisaster != null) {
         this.warningBar
            .name(
               Component.text(
                  "§4§l⚠ " + this.currentDisaster + " §7- §e§l" + this.warningTimeLeft / 60 + ":" + String.format("%02d", this.warningTimeLeft % 60)
               )
            );
         this.warningBar.progress(this.warningTimeLeft / 300.0F);
      }
   }

   // ===== PARAM HELPERS =====
   private int getParamInt(Map<String, Object> params, String key, int defaultValue) {
      Object val = params.get(key);
      if (val instanceof Number) {
         return ((Number)val).intValue();
      }

      if (val instanceof String) {
         try {
            return Integer.parseInt((String)val);
         } catch (Exception e) {
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   private double getParamDouble(Map<String, Object> params, String key, double defaultValue) {
      Object val = params.get(key);
      if (val instanceof Number) {
         return ((Number)val).doubleValue();
      }

      if (val instanceof String) {
         try {
            return Double.parseDouble((String)val);
         } catch (Exception e) {
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   private boolean getParamBool(Map<String, Object> params, String key, boolean defaultValue) {
      Object val = params.get(key);
      if (val instanceof Boolean) {
         return (Boolean)val;
      } else {
         return val instanceof String ? Boolean.parseBoolean((String)val) : defaultValue;
      }
   }

   private int toInt(Object value, int defaultValue) {
      if (value instanceof Number) {
         return ((Number)value).intValue();
      }

      if (value instanceof String) {
         try {
            return Integer.parseInt((String)value);
         } catch (Exception e) {
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   private record LivingEntityState(int fireTicks, int freezeTicks) {
   }

   private record WorldState(World world, long time, boolean storm, boolean thundering, int weatherDuration, int thunderDuration) {
   }
}
