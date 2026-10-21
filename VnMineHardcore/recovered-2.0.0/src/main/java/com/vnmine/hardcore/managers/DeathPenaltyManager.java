package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Quản lý hình phạt khi chết (Death Penalty).
 * Mỗi lần chết, người chơi bị giảm các chỉ số.
 * Sau khi sống sót đủ thời gian, chỉ số phục hồi.
 */
public class DeathPenaltyManager {
   private final VnMineHardcore plugin;
   private final ConfigManager config;
   private final Logger logger;
   // Lưu số lần chết tích lũy chưa được phục hồi cho mỗi player
   private final Map<UUID, Integer> penaltyStacks = new HashMap<>();
   // Lưu thời điểm bắt đầu hồi phục (khi player hồi sinh)
   private final Map<UUID, Long> recoveryStartTime = new HashMap<>();
   // Lưu trạng thái đã phục hồi chưa
   private final Set<UUID> recovered = new HashSet<>();
   private final File dataFile;
   private final YamlConfiguration data;
   private BukkitRunnable recoveryTask;
   // Attribute modifier names (unique names for Bukkit API)
   private static final String MODIFIER_NAME_MAX_HP = "vnmine_death_penalty_max_hp";
   private static final UUID MODIFIER_UUID_MAX_HP = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

   public DeathPenaltyManager(VnMineHardcore plugin, ConfigManager config) {
      this.plugin = plugin;
      this.config = config;
      this.logger = plugin.getLogger();
      this.dataFile = new File(plugin.getDataFolder(), "death-penalty.yml");
      this.data = YamlConfiguration.loadConfiguration(this.dataFile);
      this.loadData();
      if (config.deathPenaltyEnabled) {
         this.startRecoveryTask();
      }

      this.logger
         .info(
            "[DeathPenalty] Initialized: enabled="
               + config.deathPenaltyEnabled
               + ", recovery="
               + config.deathPenaltyRecoverySeconds
               + "s, maxStack="
               + config.deathPenaltyMaxStack
         );
   }

   /**
    * Gọi khi người chơi chết - tăng penalty stack.
    */
   public void onPlayerDeath(Player player) {
      if (this.config.deathPenaltyEnabled) {
         UUID uuid = player.getUniqueId();
         int current = this.penaltyStacks.getOrDefault(uuid, 0);
         int maxStack = this.config.deathPenaltyMaxStack;
         if (current < maxStack) {
            this.penaltyStacks.put(uuid, current + 1);
            this.logger.info("[DeathPenalty] " + player.getName() + " died. Penalty stack: " + (current + 1) + "/" + maxStack);
         } else {
            this.logger.info("[DeathPenalty] " + player.getName() + " died. Penalty already at max: " + maxStack);
         }

         this.recoveryStartTime.remove(uuid);
         this.recovered.remove(uuid);
         this.saveData();
      }
   }

   /**
    * Gọi khi người chơi hồi sinh - bắt đầu đếm thời gian hồi phục.
    */
   public void onPlayerRespawn(Player player) {
      if (this.config.deathPenaltyEnabled) {
         UUID uuid = player.getUniqueId();
         int stack = this.penaltyStacks.getOrDefault(uuid, 0);
         if (stack > 0) {
            // Bắt đầu đếm thời gian hồi phục
            this.recoveryStartTime.put(uuid, System.currentTimeMillis());
            this.recovered.remove(uuid);
            this.saveData();
            this.applyPenalty(player);
            player.sendMessage("§c§l\ud83d\udc80 HÌNH PHẠT TỬ THẦN!");
            player.sendMessage("§7Bạn đã chết §c" + stack + " §7lần. Chỉ số bị giảm!");
            player.sendMessage("§eSống sót §6" + this.config.deathPenaltyRecoverySeconds + "§e giây để phục hồi!");
            this.logger.info("[DeathPenalty] " + player.getName() + " respawned with stack=" + stack);
         }
      }
   }

   /**
    * Áp dụng penalty lên người chơi.
    */
   public void applyPenalty(Player player) {
      if (this.config.deathPenaltyEnabled) {
         int stack = this.penaltyStacks.getOrDefault(player.getUniqueId(), 0);
         if (stack > 0) {
            // Giảm max HP
            double hpReduction = Math.min(1.0, Math.max(0.0, stack * this.config.deathPenaltyMaxHpPerDeath));
            AttributeInstance maxHpAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHpAttr != null) {
               this.removeHealthPenalty(player);
               double baseMaxHp = maxHpAttr.getBaseValue();
               double newMaxHp = baseMaxHp * (1.0 - hpReduction);
               if (newMaxHp < 1.0) {
                  newMaxHp = 1.0;
               }

               AttributeModifier modifier = new AttributeModifier(
                  MODIFIER_UUID_MAX_HP, "vnmine_death_penalty_max_hp", newMaxHp - baseMaxHp, Operation.ADD_NUMBER
               );
               maxHpAttr.addTransientModifier(modifier);
               // Clamp current health
               if (player.getHealth() > maxHpAttr.getValue()) {
                  player.setHealth(maxHpAttr.getValue());
               }
            }

            player.sendMessage("§c§l☠ Penalty: §7HP giảm " + String.format("%.0f", hpReduction * 100.0) + "%");
         }
      }
   }

   public void onPlayerJoin(Player player) {
      UUID uuid = player.getUniqueId();
      if (this.config.deathPenaltyEnabled && this.penaltyStacks.getOrDefault(uuid, 0) > 0) {
         Long startTime = this.recoveryStartTime.get(uuid);
         if (startTime != null && System.currentTimeMillis() - startTime >= this.config.deathPenaltyRecoverySeconds * 1000L) {
            this.removePenalty(player);
         } else {
            this.applyPenalty(player);
         }
      } else {
         this.removeHealthPenalty(player);
      }
   }

   /**
    * Gỡ bỏ penalty khỏi người chơi.
    */
   public void removePenalty(Player player) {
      UUID uuid = player.getUniqueId();
      this.penaltyStacks.remove(uuid);
      this.recoveryStartTime.remove(uuid);
      this.recovered.add(uuid);
      this.data.set("players." + uuid, null);
      this.removeHealthPenalty(player);
      player.sendMessage("§a§l✅ PHỤC HỒI! §7Chỉ số đã trở lại bình thường!");
      this.logger.info("[DeathPenalty] " + player.getName() + " recovered from death penalty");
      this.saveData();
   }

   /**
    * Lấy số stack penalty hiện tại.
    */
   public int getPenaltyStack(Player player) {
      return this.penaltyStacks.getOrDefault(player.getUniqueId(), 0);
   }

   /**
    * Lấy hệ số nhân sát thương gây ra (0.0 - 1.0).
    * 1.0 = bình thường, 0.8 = giảm 20%.
    */
   public double getDamageDealtMultiplier(Player player) {
      if (!this.config.deathPenaltyEnabled) {
         return 1.0;
      }

      int stack = this.penaltyStacks.getOrDefault(player.getUniqueId(), 0);
      return stack <= 0 ? 1.0 : Math.max(0.1, 1.0 - stack * this.config.deathPenaltyDamagePerDeath);
   }

   /**
    * Lấy hệ số nhân sát thương nhận vào (1.0+).
    * 1.0 = bình thường, 1.2 = tăng 20%.
    */
   public double getIncomingDamageMultiplier(Player player) {
      if (!this.config.deathPenaltyEnabled) {
         return 1.0;
      }

      int stack = this.penaltyStacks.getOrDefault(player.getUniqueId(), 0);
      return stack <= 0 ? 1.0 : Math.max(0.0, 1.0 + stack * this.config.deathPenaltyIncomingDamagePerDeath);
   }

   /**
    * Lấy hệ số nhân hồi phục (0.0 - 1.0).
    */
   public double getRegenMultiplier(Player player) {
      if (!this.config.deathPenaltyEnabled) {
         return 1.0;
      }

      int stack = this.penaltyStacks.getOrDefault(player.getUniqueId(), 0);
      return stack <= 0 ? 1.0 : Math.max(0.0, 1.0 - stack * this.config.deathPenaltyRegenPerDeath);
   }

   /**
    * Lấy hệ số nhân drain thức ăn (1.0+).
    */
   public double getFoodDrainMultiplier(Player player) {
      if (!this.config.deathPenaltyEnabled) {
         return 1.0;
      }

      int stack = this.penaltyStacks.getOrDefault(player.getUniqueId(), 0);
      return stack <= 0 ? 1.0 : Math.max(0.0, 1.0 + stack * this.config.deathPenaltyFoodDrainPerDeath);
   }

   /**
    * Lấy hệ số nhân drain khát (1.0+).
    */
   public double getThirstDrainMultiplier(Player player) {
      if (!this.config.deathPenaltyEnabled) {
         return 1.0;
      }

      int stack = this.penaltyStacks.getOrDefault(player.getUniqueId(), 0);
      return stack <= 0 ? 1.0 : Math.max(0.0, 1.0 + stack * this.config.deathPenaltyThirstDrainPerDeath);
   }

   /**
    * Lấy max thirst offset (giảm dần theo stack).
    */
   public int getMaxThirstOffset(Player player) {
      if (!this.config.deathPenaltyEnabled) {
         return 0;
      }

      int stack = this.penaltyStacks.getOrDefault(player.getUniqueId(), 0);
      if (stack <= 0) {
         return 0;
      }

      double reduction = stack * this.config.deathPenaltyMaxThirstPerDeath;
      int maxOffset = Math.max(0, this.config.maxThirst - 1);
      return Math.min(maxOffset, (int)Math.round(this.config.maxThirst * reduction));
   }

   /**
    * Task kiểm tra hồi phục mỗi giây.
    */
   private void startRecoveryTask() {
      this.recoveryTask = new BukkitRunnable() {
         public void run() {
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
               UUID uuid = player.getUniqueId();
               if (DeathPenaltyManager.this.penaltyStacks.containsKey(uuid) && !DeathPenaltyManager.this.recovered.contains(uuid)) {
                  Long startTime = DeathPenaltyManager.this.recoveryStartTime.get(uuid);
                  if (startTime != null) {
                     long elapsed = (now - startTime) / 1000L;
                     long required = DeathPenaltyManager.this.config.deathPenaltyRecoverySeconds;
                     if (elapsed >= required) {
                        DeathPenaltyManager.this.removePenalty(player);
                     } else if (elapsed % 30L == 0L && elapsed > 0L) {
                        long remaining = required - elapsed;
                        player.sendMessage("§e⏳ Còn §6" + remaining + "§e giây để phục hồi chỉ số...");
                     }
                  }
               }
            }
         }
      };
      this.recoveryTask.runTaskTimer(this.plugin, 20L, 20L);
   }

   public void stop() {
      if (this.recoveryTask != null) {
         this.recoveryTask.cancel();
         this.recoveryTask = null;
      }

      this.saveData();
   }

   public void reload() {
      this.stop();
      this.loadData();
      if (this.config.deathPenaltyEnabled) {
         this.startRecoveryTask();
      }

      for (Player player : Bukkit.getOnlinePlayers()) {
         this.onPlayerJoin(player);
      }
   }

   private void saveData() {
      for (Entry<UUID, Integer> entry : this.penaltyStacks.entrySet()) {
         String path = "players." + entry.getKey().toString();
         this.data.set(path + ".stack", entry.getValue());
         this.data.set(path + ".recoveryStart", this.recoveryStartTime.get(entry.getKey()));
      }

      try {
         this.data.save(this.dataFile);
      } catch (IOException e) {
         this.logger.warning("[DeathPenalty] Could not save data: " + e.getMessage());
      }
   }

   private void loadData() {
      this.penaltyStacks.clear();
      this.recoveryStartTime.clear();
      this.recovered.clear();
      if (this.data.contains("players")) {
         for (String uuidStr : this.data.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            int stack = this.data.getInt("players." + uuidStr + ".stack", 0);
            if (stack > 0) {
               this.penaltyStacks.put(uuid, stack);
               if (this.data.contains("players." + uuidStr + ".recoveryStart")) {
                  this.recoveryStartTime.put(uuid, this.data.getLong("players." + uuidStr + ".recoveryStart"));
               }
            }
         }

         this.logger.info("[DeathPenalty] Loaded " + this.penaltyStacks.size() + " player penalties");
      }
   }

   private void removeHealthPenalty(Player player) {
      AttributeInstance maxHpAttr = player.getAttribute(Attribute.MAX_HEALTH);
      if (maxHpAttr != null) {
         maxHpAttr.removeModifier(MODIFIER_UUID_MAX_HP);

         for (AttributeModifier modifier : new ArrayList<AttributeModifier>(maxHpAttr.getModifiers())) {
            if (modifier.getName().equals("vnmine_death_penalty_max_hp")) {
               maxHpAttr.removeModifier(modifier);
            }
         }
      }
   }
}
