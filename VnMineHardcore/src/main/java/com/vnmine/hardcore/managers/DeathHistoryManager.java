package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class DeathHistoryManager implements Listener, CommandExecutor, TabCompleter {
    private static final String HISTORY_PERMISSION = "vnmine.command.death.history";
    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final VnMineHardcore plugin;
    private final File dataFile;
    private final File backupFile;
    private int maxRecords;
    private final Map<UUID, List<DeathSnapshot>> histories = new HashMap<>();
    private boolean persistenceAvailable = true;
    private boolean preserveBackup;

    private enum MenuType {
        ONLINE_PLAYERS, DEATHS, CONFIRM_RESTORE
    }

    private static final class DeathSnapshot {
        private final UUID id;
        private final String playerName;
        private final long diedAt;
        private final int deathCount;
        private final ItemStack[] storage;
        private final ItemStack[] armor;
        private final ItemStack offhand;

        private DeathSnapshot(UUID id, String playerName, long diedAt, int deathCount,
                              ItemStack[] storage, ItemStack[] armor, ItemStack offhand) {
            this.id = id;
            this.playerName = playerName;
            this.diedAt = diedAt;
            this.deathCount = deathCount;
            this.storage = storage;
            this.armor = armor;
            this.offhand = offhand;
        }
    }

    private static final class HistoryMenuHolder implements InventoryHolder {
        private final UUID viewer;
        private final MenuType type;
        private final UUID target;
        private final String snapshotId;
        private final int page;
        private final Map<Integer, String> choices = new HashMap<>();
        private Inventory inventory;

        private HistoryMenuHolder(UUID viewer, MenuType type, UUID target, String snapshotId, int page) {
            this.viewer = viewer;
            this.type = type;
            this.target = target;
            this.snapshotId = snapshotId;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public DeathHistoryManager(VnMineHardcore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "death-history.yml");
        this.backupFile = new File(plugin.getDataFolder(), "death-history.yml.bak");
        this.maxRecords = Math.max(1, Math.min(10,
            plugin.getConfig().getInt("death.history.max-records-per-player", 10)));
        loadData();
    }

    public boolean reload() {
        int configuredMaximum = Math.max(1, Math.min(10,
            plugin.getConfig().getInt("death.history.max-records-per-player", 10)));
        if (configuredMaximum == maxRecords) return true;

        Map<UUID, List<DeathSnapshot>> previousHistories = new HashMap<>();
        int previousMaximum = maxRecords;
        maxRecords = configuredMaximum;
        for (Map.Entry<UUID, List<DeathSnapshot>> entry : histories.entrySet()) {
            List<DeathSnapshot> records = entry.getValue();
            if (records.size() > maxRecords) {
                previousHistories.put(entry.getKey(), records);
                entry.setValue(new ArrayList<>(records.subList(records.size() - maxRecords, records.size())));
            }
        }
        if (previousHistories.isEmpty() || saveData()) return true;

        histories.putAll(previousHistories);
        maxRecords = previousMaximum;
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        ItemStack[] storage = cloneItems(player.getInventory().getStorageContents());
        ItemStack[] armor = cloneItems(player.getInventory().getArmorContents());
        ItemStack offhand = cloneItem(player.getInventory().getItemInOffHand());
        UUID playerId = player.getUniqueId();
        List<DeathSnapshot> previous = histories.getOrDefault(playerId, List.of());
        List<DeathSnapshot> updated = new ArrayList<>(previous);
        updated.add(new DeathSnapshot(UUID.randomUUID(), player.getName(), System.currentTimeMillis(),
            plugin.getLogManager().getDeathCount(player), storage, armor, offhand));
        while (updated.size() > maxRecords) updated.removeFirst();
        histories.put(playerId, updated);
        if (!saveData()) {
            if (previous.isEmpty()) histories.remove(playerId);
            else histories.put(playerId, new ArrayList<>(previous));
            plugin.getLogger().severe("[DeathHistory] Could not save death inventory for " + player.getName() + ".");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(HISTORY_PERMISSION)) {
            sender.sendMessage("§cBạn không có quyền!");
            return true;
        }
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("§cLệnh này chỉ dành cho người chơi.");
            return true;
        }
        if (args.length == 0) {
            openOnlinePlayers(viewer, 0);
            return true;
        }
        if (args.length == 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                viewer.sendMessage("§cNgười chơi phải đang online để xem/khôi phục lịch sử.");
                return true;
            }
            openDeaths(viewer, target.getUniqueId(), 0);
            return true;
        }
        viewer.sendMessage("§eSử dụng: /vndeathhistory [player]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(HISTORY_PERMISSION) || args.length != 1) return List.of();
        String partial = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream().map(Player::getName)
            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial)).toList();
    }

    private void openOnlinePlayers(Player viewer, int page) {
        List<? extends Player> online = Bukkit.getOnlinePlayers().stream()
            .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        int pages = Math.max(1, (online.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        HistoryMenuHolder holder = createMenu(viewer, MenuType.ONLINE_PLAYERS, null, null, safePage,
            "Lịch sử chết • Người chơi " + (safePage + 1) + "/" + pages);
        for (int index = safePage * PAGE_SIZE; index < Math.min((safePage + 1) * PAGE_SIZE, online.size()); index++) {
            Player target = online.get(index);
            int slot = index - safePage * PAGE_SIZE;
            holder.choices.put(slot, target.getUniqueId().toString());
            holder.inventory.setItem(slot, playerHead(target, "§e" + target.getName(),
                List.of("§7Chọn để xem lịch sử chết.")));
        }
        if (online.isEmpty()) holder.inventory.setItem(22,
            menuItem(Material.BARRIER, "§cKhông có người chơi online", List.of()));
        addNavigation(holder, safePage, pages, false);
        viewer.openInventory(holder.inventory);
    }

    private void openDeaths(Player viewer, UUID targetId, int page) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            viewer.sendMessage("§cNgười chơi đã offline.");
            openOnlinePlayers(viewer, 0);
            return;
        }
        List<DeathSnapshot> records = histories.getOrDefault(targetId, List.of());
        if (records.isEmpty()) {
            viewer.sendMessage("§eChưa có lịch sử chết được lưu cho §f" + target.getName() + "§e.");
            openOnlinePlayers(viewer, 0);
            return;
        }
        List<DeathSnapshot> newestFirst = new ArrayList<>(records);
        newestFirst.sort(Comparator.comparingLong((DeathSnapshot record) -> record.diedAt).reversed());
        int pages = Math.max(1, (newestFirst.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        HistoryMenuHolder holder = createMenu(viewer, MenuType.DEATHS, targetId, null, safePage,
            "Lịch sử chết • " + target.getName() + " • " + (safePage + 1) + "/" + pages);
        for (int index = safePage * PAGE_SIZE; index < Math.min((safePage + 1) * PAGE_SIZE, newestFirst.size()); index++) {
            DeathSnapshot record = newestFirst.get(index);
            int slot = index - safePage * PAGE_SIZE;
            holder.choices.put(slot, record.id.toString());
            holder.inventory.setItem(slot, playerHead(target, "§cLần chết #" + record.deathCount,
                List.of("§7Thời gian: §f" + TIME_FORMAT.format(Instant.ofEpochMilli(record.diedAt)),
                    "§7Snapshot: §f" + record.storage.length + " ô túi + giáp + tay phụ",
                    "§eNhấn để chọn khôi phục.")));
        }
        addNavigation(holder, safePage, pages, true);
        viewer.openInventory(holder.inventory);
    }

    private void openRestoreConfirmation(Player viewer, UUID targetId, UUID snapshotId) {
        Player target = Bukkit.getPlayer(targetId);
        DeathSnapshot snapshot = findSnapshot(targetId, snapshotId);
        if (target == null || snapshot == null) {
            viewer.sendMessage("§cNgười chơi hoặc snapshot không còn khả dụng.");
            openOnlinePlayers(viewer, 0);
            return;
        }
        HistoryMenuHolder holder = createMenu(viewer, MenuType.CONFIRM_RESTORE, targetId,
            snapshotId.toString(), 0, "Xác nhận khôi phục inventory");
        holder.inventory.setItem(4, playerHead(target, "§e" + target.getName(),
            List.of("§7Lần chết #" + snapshot.deathCount,
                "§7Thời gian: §f" + TIME_FORMAT.format(Instant.ofEpochMilli(snapshot.diedAt)),
                "§cInventory hiện tại sẽ bị thay thế.")));
        holder.inventory.setItem(11, menuItem(Material.LIME_WOOL, "§aXÁC NHẬN",
            List.of("§7Thay inventory của người chơi bằng snapshot này.")));
        holder.inventory.setItem(15, menuItem(Material.RED_WOOL, "§cHỦY",
            List.of("§7Không thay đổi inventory.")));
        holder.inventory.setItem(22, menuItem(Material.ARROW, "§7Quay lại lịch sử", List.of()));
        viewer.openInventory(holder.inventory);
    }

    private HistoryMenuHolder createMenu(Player viewer, MenuType type, UUID target, String snapshotId,
                                         int page, String title) {
        HistoryMenuHolder holder = new HistoryMenuHolder(viewer.getUniqueId(), type, target, snapshotId, page);
        holder.inventory = Bukkit.createInventory(holder, 54, title);
        return holder;
    }

    private void addNavigation(HistoryMenuHolder holder, int page, int pages, boolean hasBack) {
        if (page > 0) holder.inventory.setItem(45, menuItem(Material.ARROW, "§eTrang trước", List.of()));
        holder.inventory.setItem(49, menuItem(Material.ARROW,
            hasBack ? "§7Danh sách người chơi" : "§7Đóng", List.of()));
        if (page + 1 < pages) holder.inventory.setItem(53, menuItem(Material.ARROW, "§eTrang sau", List.of()));
    }

    @EventHandler
    public void onHistoryMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof HistoryMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player viewer)
                || !holder.viewer.equals(viewer.getUniqueId())
                || event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (!viewer.hasPermission(HISTORY_PERMISSION)) {
            viewer.closeInventory();
            viewer.sendMessage("§cBạn không còn quyền xem lịch sử chết.");
            return;
        }
        int slot = event.getRawSlot();
        String choice = holder.choices.get(slot);
        switch (holder.type) {
            case ONLINE_PLAYERS -> {
                if (slot == 49) viewer.closeInventory();
                else if (slot == 45 && holder.page > 0) openOnlinePlayers(viewer, holder.page - 1);
                else if (slot == 53) openOnlinePlayers(viewer, holder.page + 1);
                else if (choice != null) openDeaths(viewer, UUID.fromString(choice), 0);
            }
            case DEATHS -> {
                if (slot == 49) openOnlinePlayers(viewer, 0);
                else if (slot == 45 && holder.page > 0) openDeaths(viewer, holder.target, holder.page - 1);
                else if (slot == 53) openDeaths(viewer, holder.target, holder.page + 1);
                else if (choice != null) openRestoreConfirmation(viewer, holder.target, UUID.fromString(choice));
            }
            case CONFIRM_RESTORE -> {
                if (slot == 15) openDeaths(viewer, holder.target, 0);
                else if (slot == 22) openDeaths(viewer, holder.target, 0);
                else if (slot == 11) restoreSnapshot(viewer, holder.target, holder.snapshotId);
            }
        }
    }

    @EventHandler
    public void onHistoryMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof HistoryMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void restoreSnapshot(Player viewer, UUID targetId, String snapshotId) {
        Player target = Bukkit.getPlayer(targetId);
        UUID parsedSnapshotId;
        try {
            parsedSnapshotId = UUID.fromString(snapshotId);
        } catch (IllegalArgumentException exception) {
            viewer.sendMessage("§cSnapshot không hợp lệ.");
            return;
        }
        DeathSnapshot snapshot = findSnapshot(targetId, parsedSnapshotId);
        if (target == null || snapshot == null) {
            viewer.sendMessage("§cNgười chơi hoặc snapshot không còn khả dụng.");
            openOnlinePlayers(viewer, 0);
            return;
        }
        target.getInventory().clear();
        target.getInventory().setStorageContents(cloneItems(snapshot.storage));
        target.getInventory().setArmorContents(cloneItems(snapshot.armor));
        target.getInventory().setItemInOffHand(cloneItem(snapshot.offhand));
        target.updateInventory();
        viewer.closeInventory();
        viewer.sendMessage("§aĐã khôi phục inventory của §e" + target.getName() + "§a từ lần chết #"
            + snapshot.deathCount + ".");
        if (!viewer.getUniqueId().equals(targetId)) {
            target.sendMessage("§eAdmin đã khôi phục inventory của bạn từ snapshot lần chết #"
                + snapshot.deathCount + ".");
        }
    }

    private DeathSnapshot findSnapshot(UUID playerId, UUID snapshotId) {
        return histories.getOrDefault(playerId, List.of()).stream()
            .filter(snapshot -> snapshot.id.equals(snapshotId)).findFirst().orElse(null);
    }

    private ItemStack playerHead(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta itemMeta = head.getItemMeta();
        if (itemMeta instanceof SkullMeta skullMeta) skullMeta.setOwningPlayer(owner);
        itemMeta.setDisplayName(name);
        itemMeta.setLore(lore);
        head.setItemMeta(itemMeta);
        return head;
    }

    private ItemStack menuItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] result = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) result[index] = cloneItem(items[index]);
        return result;
    }

    private void loadData() {
        if (!dataFile.isFile()) return;
        YamlConfiguration loaded = loadFile(dataFile);
        if (loaded == null && backupFile.isFile()) {
            plugin.getLogger().warning("[DeathHistory] Primary file is invalid; attempting backup.");
            preserveBackup = true;
            loaded = loadFile(backupFile);
        }
        if (loaded == null) {
            persistenceAvailable = false;
            plugin.getLogger().severe("[DeathHistory] Could not load history data; writes are disabled to protect existing data.");
            return;
        }
        if (!loaded.isConfigurationSection("players")) return;
        for (String playerKey : loaded.getConfigurationSection("players").getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("[DeathHistory] Ignoring invalid player UUID: " + playerKey);
                continue;
            }
            String root = "players." + playerKey;
            List<DeathSnapshot> records = new ArrayList<>();
            for (String snapshotKey : loaded.getStringList(root + ".order")) {
                try {
                    UUID snapshotId = UUID.fromString(snapshotKey);
                    String path = root + ".records." + snapshotKey;
                    ItemStack[] storage = loadItems(loaded, path + ".storage", 36);
                    ItemStack[] armor = loadItems(loaded, path + ".armor", 4);
                    ItemStack offhand = loaded.getItemStack(path + ".offhand");
                    records.add(new DeathSnapshot(snapshotId, loaded.getString(path + ".name", "Unknown"),
                        loaded.getLong(path + ".diedAt", 0), loaded.getInt(path + ".deathCount", 0),
                        storage, armor, offhand));
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("[DeathHistory] Ignoring invalid snapshot ID: " + snapshotKey);
                }
            }
            if (records.size() > maxRecords) {
                records = new ArrayList<>(records.subList(records.size() - maxRecords, records.size()));
            }
            if (!records.isEmpty()) histories.put(playerId, records);
        }
    }

    private YamlConfiguration loadFile(File file) {
        YamlConfiguration loaded = new YamlConfiguration();
        try {
            loaded.load(file);
            return loaded;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.SEVERE, "[DeathHistory] Could not read " + file.getName() + ".", exception);
            return null;
        }
    }

    private ItemStack[] loadItems(YamlConfiguration loaded, String path, int length) {
        ItemStack[] items = new ItemStack[length];
        for (int index = 0; index < length; index++) items[index] = loaded.getItemStack(path + "." + index);
        return items;
    }

    private boolean saveData() {
        if (!persistenceAvailable) return false;
        YamlConfiguration output = new YamlConfiguration();
        for (Map.Entry<UUID, List<DeathSnapshot>> entry : histories.entrySet()) {
            String root = "players." + entry.getKey();
            List<String> order = new ArrayList<>();
            for (DeathSnapshot snapshot : entry.getValue()) {
                String path = root + ".records." + snapshot.id;
                order.add(snapshot.id.toString());
                output.set(path + ".name", snapshot.playerName);
                output.set(path + ".diedAt", snapshot.diedAt);
                output.set(path + ".deathCount", snapshot.deathCount);
                saveItems(output, path + ".storage", snapshot.storage);
                saveItems(output, path + ".armor", snapshot.armor);
                output.set(path + ".offhand", snapshot.offhand);
            }
            output.set(root + ".order", order);
        }
        try {
            if (dataFile.isFile() && !preserveBackup) {
                java.nio.file.Files.copy(dataFile.toPath(), backupFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            output.save(dataFile);
            preserveBackup = false;
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "[DeathHistory] Could not save " + dataFile.getName() + ".", exception);
            return false;
        }
    }

    private void saveItems(YamlConfiguration output, String path, ItemStack[] items) {
        for (int index = 0; index < items.length; index++) {
            if (items[index] != null && !items[index].getType().isAir()) {
                output.set(path + "." + index, items[index]);
            }
        }
    }
}
