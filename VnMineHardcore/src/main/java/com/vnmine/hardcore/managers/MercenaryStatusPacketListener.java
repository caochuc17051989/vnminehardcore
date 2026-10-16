package com.vnmine.hardcore.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.vnmine.hardcore.VnMineHardcore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

final class MercenaryStatusPacketListener implements Listener, AutoCloseable {
    private static final AtomicInteger NEXT_VIRTUAL_ENTITY_ID = new AtomicInteger(-2_000_000);
    private static final WrappedDataWatcher.Serializer CHAT_COMPONENT_SERIALIZER =
        WrappedDataWatcher.Registry.getChatComponentSerializer(false);
    private static final WrappedDataWatcher.Serializer INTEGER_SERIALIZER =
        WrappedDataWatcher.Registry.get(Integer.class);
    private static final WrappedDataWatcher.Serializer BYTE_SERIALIZER =
        WrappedDataWatcher.Registry.get(Byte.class);

    private static final class StatusEntry {
        private final UUID mobId;
        private final UUID ownerId;
        private final int mobEntityId;
        private final int virtualEntityId;
        private volatile String text;
        private String sentText;
        private volatile boolean spawned;

        private StatusEntry(Mob mob, UUID ownerId, String text) {
            this.mobId = mob.getUniqueId();
            this.ownerId = ownerId;
            this.mobEntityId = mob.getEntityId();
            this.virtualEntityId = NEXT_VIRTUAL_ENTITY_ID.getAndDecrement();
            this.text = text;
        }
    }

    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, StatusEntry> entriesByUnit = new ConcurrentHashMap<>();
    private final Map<UUID, StatusEntry> entriesByMobId = new ConcurrentHashMap<>();
    private final Map<Integer, StatusEntry> entriesByMobEntityId = new ConcurrentHashMap<>();
    private final PacketAdapter packetListener;

    MercenaryStatusPacketListener(VnMineHardcore plugin) {
        this.plugin = plugin;
        protocolManager = ProtocolLibrary.getProtocolManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        packetListener = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.SPAWN_ENTITY, PacketType.Play.Server.ENTITY_DESTROY,
                PacketType.Play.Server.MOUNT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                    onMobSpawnPacket(event);
                } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_DESTROY) {
                    onMobDestroyPacket(event);
                } else {
                    preserveVirtualPassenger(event);
                }
            }
        };
        protocolManager.addPacketListener(packetListener);
    }

    void update(UUID unitId, Mob mob, UUID ownerId, String text) {
        StatusEntry entry = entriesByUnit.get(unitId);
        if (entry != null && !entry.mobId.equals(mob.getUniqueId())) {
            remove(unitId);
            entry = null;
        }
        if (entry == null) {
            entry = new StatusEntry(mob, ownerId, text);
            entriesByUnit.put(unitId, entry);
            entriesByMobId.put(entry.mobId, entry);
            entriesByMobEntityId.put(entry.mobEntityId, entry);
        } else {
            entry.text = text;
        }
        reconcile(entry, mob);
    }

    void remove(UUID unitId) {
        StatusEntry entry = entriesByUnit.remove(unitId);
        if (entry == null) return;
        entriesByMobId.remove(entry.mobId, entry);
        entriesByMobEntityId.remove(entry.mobEntityId, entry);
        Player owner = Bukkit.getPlayer(entry.ownerId);
        if (entry.spawned && owner != null && owner.isOnline()) destroyVirtualEntity(owner, entry);
        entry.spawned = false;
    }

    @EventHandler
    public void onViewerQuit(PlayerQuitEvent event) {
        for (StatusEntry entry : entriesByUnit.values()) {
            if (entry.ownerId.equals(event.getPlayer().getUniqueId())) entry.spawned = false;
        }
    }

    private void onMobSpawnPacket(PacketEvent event) {
        int mobEntityId = event.getPacket().getIntegers().read(0);
        StatusEntry entry = entriesByMobEntityId.get(mobEntityId);
        if (entry == null || !entry.ownerId.equals(event.getPlayer().getUniqueId())) return;
        scheduleReconcile(entry);
    }

    private void onMobDestroyPacket(PacketEvent event) {
        List<Integer> destroyedEntityIds = event.getPacket().getIntLists().read(0);
        Player viewer = event.getPlayer();
        UUID viewerId = viewer.getUniqueId();
        for (int mobEntityId : destroyedEntityIds) {
            StatusEntry entry = entriesByMobEntityId.get(mobEntityId);
            if (entry == null || !entry.ownerId.equals(viewerId)) continue;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (entriesByMobEntityId.get(mobEntityId) == entry) hide(viewer, entry);
            });
        }
    }

    private void preserveVirtualPassenger(PacketEvent event) {
        int vehicleEntityId = event.getPacket().getIntegers().read(0);
        StatusEntry entry = entriesByMobEntityId.get(vehicleEntityId);
        if (entry == null || !entry.spawned
                || !entry.ownerId.equals(event.getPlayer().getUniqueId())) return;
        int[] passengerIds = event.getPacket().getIntegerArrays().read(0);
        if (Arrays.stream(passengerIds).anyMatch(id -> id == entry.virtualEntityId)) return;
        int[] updatedPassengerIds = Arrays.copyOf(passengerIds, passengerIds.length + 1);
        updatedPassengerIds[passengerIds.length] = entry.virtualEntityId;
        event.getPacket().getIntegerArrays().write(0, updatedPassengerIds);
    }

    private void scheduleReconcile(StatusEntry entry) {
        Player owner = Bukkit.getPlayer(entry.ownerId);
        if (owner == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (entriesByMobEntityId.get(entry.mobEntityId) != entry || !owner.isOnline()) return;
            Entity entity = Bukkit.getEntity(entry.mobId);
            if (entity instanceof Mob mob && mob.isValid()) reconcile(entry, mob);
        });
    }

    private void reconcile(StatusEntry entry, Mob mob) {
        Player owner = Bukkit.getPlayer(entry.ownerId);
        boolean ownerTracksMob = owner != null && owner.isOnline() && mob.getTrackedBy().contains(owner);
        if (!ownerTracksMob) {
            if (entry.spawned && owner != null && owner.isOnline()) hide(owner, entry);
            return;
        }
        if (!entry.spawned) {
            spawnVirtualEntity(owner, mob, entry);
        } else if (!entry.text.equals(entry.sentText) && sendMetadata(owner, entry)) {
            entry.sentText = entry.text;
        }
    }

    private void spawnVirtualEntity(Player owner, Mob mob, StatusEntry entry) {
        PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawn.getIntegers().write(0, entry.virtualEntityId).write(1, 0);
        spawn.getUUIDs().write(0, UUID.randomUUID());
        spawn.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);
        spawn.getDoubles().write(0, mob.getLocation().getX())
            .write(1, mob.getLocation().getY() + 0.5)
            .write(2, mob.getLocation().getZ());
        spawn.getBytes().write(0, (byte) 0).write(1, (byte) 0).write(2, (byte) 0);
        spawn.getVectors().write(0, new Vector());
        if (!sendPacket(owner, spawn)) return;

        entry.spawned = true;
        if (sendMetadata(owner, entry)) entry.sentText = entry.text;
        sendPassengers(owner, mob, entry);
    }

    private boolean sendMetadata(Player viewer, StatusEntry entry) {
        PacketContainer metadata = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, entry.virtualEntityId);
        metadata.getDataValueCollectionModifier().write(0, List.of(
            new WrappedDataValue(15, BYTE_SERIALIZER, (byte) 3),
            new WrappedDataValue(23, CHAT_COMPONENT_SERIALIZER,
                WrappedChatComponent.fromLegacyText(entry.text).getHandle()),
            new WrappedDataValue(25, INTEGER_SERIALIZER, 0),
            new WrappedDataValue(26, BYTE_SERIALIZER, (byte) -1)
        ));
        return sendPacket(viewer, metadata);
    }

    private void sendPassengers(Player viewer, Mob mob, StatusEntry entry) {
        PacketContainer passengers = protocolManager.createPacket(PacketType.Play.Server.MOUNT);
        passengers.getIntegers().write(0, entry.mobEntityId);
        List<Integer> passengerIds = new ArrayList<>();
        for (Entity passenger : mob.getPassengers()) passengerIds.add(passenger.getEntityId());
        passengerIds.add(entry.virtualEntityId);
        passengers.getIntegerArrays().write(0,
            passengerIds.stream().mapToInt(Integer::intValue).toArray());
        sendPacket(viewer, passengers);
    }

    private void hide(Player viewer, StatusEntry entry) {
        if (!entry.spawned) return;
        destroyVirtualEntity(viewer, entry);
        entry.spawned = false;
        entry.sentText = null;
    }

    private void destroyVirtualEntity(Player viewer, StatusEntry entry) {
        PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntLists().write(0, List.of(entry.virtualEntityId));
        sendPacket(viewer, destroy);
    }

    private boolean sendPacket(Player viewer, PacketContainer packet) {
        try {
            protocolManager.sendServerPacket(viewer, packet);
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING,
                "Could not send mercenary status packet to " + viewer.getName() + ".", exception);
            return false;
        }
    }

    @Override
    public void close() {
        for (UUID unitId : List.copyOf(entriesByUnit.keySet())) remove(unitId);
        protocolManager.removePacketListener(packetListener);
        HandlerList.unregisterAll(this);
        entriesByMobId.clear();
        entriesByMobEntityId.clear();
    }
}