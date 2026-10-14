package com.vnmine.hardcore.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.vnmine.hardcore.VnMineHardcore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class MercenaryNamePacketListener implements Listener, AutoCloseable {
    private static final int CUSTOM_NAME_INDEX = 2;
    private static final WrappedDataWatcher.Serializer CHAT_COMPONENT_SERIALIZER =
        WrappedDataWatcher.Registry.getChatComponentSerializer(true);

    private record NameEntry(UUID ownerId, Object ownerComponent, Object viewerComponent) {
    }

    private final Map<Player, UUID> viewerIds = Collections.synchronizedMap(new IdentityHashMap<>());
    private final ConcurrentMap<Integer, NameEntry> namesByEntityId = new ConcurrentHashMap<>();
    private final PacketAdapter packetListener;

    MercenaryNamePacketListener(VnMineHardcore plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            viewerIds.put(player, player.getUniqueId());
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        packetListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                updateViewerName(event);
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
    }

    void update(Mob mob, UUID ownerId, String ownerName, String unitName, int tier, int level) {
        String ownerLabel = "§a§l[" + ownerName + "]§r §f" + unitName + " §eBậc " + tier + " §fCấp " + level;
        String viewerLabel = "§b§l[" + ownerName + "]§r §f" + unitName + " §eBậc " + tier + " §fCấp " + level;
        namesByEntityId.put(mob.getEntityId(), new NameEntry(ownerId,
            WrappedChatComponent.fromLegacyText(ownerLabel).getHandle(),
            WrappedChatComponent.fromLegacyText(viewerLabel).getHandle()));
    }

    void remove(Entity entity) {
        namesByEntityId.remove(entity.getEntityId());
    }

    @EventHandler
    public void onViewerJoin(PlayerJoinEvent event) {
        viewerIds.put(event.getPlayer(), event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onViewerQuit(PlayerQuitEvent event) {
        viewerIds.remove(event.getPlayer());
    }

    private void updateViewerName(PacketEvent event) {
        int entityId = event.getPacket().getIntegers().read(0);
        NameEntry entry = namesByEntityId.get(entityId);
        if (entry == null) return;

        UUID viewerId = viewerIds.get(event.getPlayer());
        if (viewerId == null) return;
        Object component = viewerId.equals(entry.ownerId()) ? entry.ownerComponent() : entry.viewerComponent();

        List<WrappedDataValue> dataValues = new ArrayList<>(
            event.getPacket().getDataValueCollectionModifier().read(0));
        for (WrappedDataValue dataValue : dataValues) {
            if (dataValue.getIndex() == CUSTOM_NAME_INDEX) {
                dataValue.setValue(Optional.of(component));
                event.getPacket().getDataValueCollectionModifier().write(0, dataValues);
                return;
            }
        }
        dataValues.add(new WrappedDataValue(CUSTOM_NAME_INDEX, CHAT_COMPONENT_SERIALIZER, Optional.of(component)));
        event.getPacket().getDataValueCollectionModifier().write(0, dataValues);
    }

    @Override
    public void close() {
        ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
        HandlerList.unregisterAll(this);
        namesByEntityId.clear();
        viewerIds.clear();
    }
}