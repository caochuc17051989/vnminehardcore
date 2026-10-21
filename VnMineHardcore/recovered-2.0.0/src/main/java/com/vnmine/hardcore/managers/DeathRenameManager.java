package com.vnmine.hardcore.managers;

import com.vnmine.hardcore.VnMineHardcore;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeathRenameManager {

    private final VnMineHardcore plugin;
    private final ConfigManager config;
    private final Logger logger;

    public DeathRenameManager(VnMineHardcore plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    /**
     * Cập nhật tên hiển thị trong chat và danh sách người chơi theo cấu hình,
     * không thay đổi tên tài khoản gốc.
     */
    public void updateDisplayName(Player player, int deathCount) {
        if (!config.renameEnabled) {
            return;
        }

        String nameStructure = config.nameStructure;
        if (nameStructure == null || nameStructure.isEmpty()) {
            return;
        }

        String displayName = parseNameStructure(nameStructure, player.getName(), deathCount);
        player.displayName(Component.text(displayName));
        player.playerListName(Component.text(displayName));
        logger.fine("[Rename] Updated display name for " + player.getName()
            + " (UUID: " + player.getUniqueId() + ") -> \"" + displayName + "\"");
    }

    /** Reset tên hiển thị về giá trị mặc định của Minecraft. */
    public void resetToOriginalName(Player player) {
        player.displayName(null);
        player.playerListName(null);
    }

    /**
     * Thay thế <name> và <count>; chuỗi số 0 đứng riêng được dùng làm độ rộng
     * tối thiểu để đệm số lần chết bằng số 0 ở đầu.
     */
    private String parseNameStructure(String nameStructure, String playerName, int deathCount) {
        String nameMarker = "\u0000NAME\u0000";
        String countMarker = "\u0000COUNT\u0000";
        String protectedStructure = nameStructure
            .replace("<name>", nameMarker)
            .replace("<count>", countMarker);

        Pattern paddingPattern = Pattern.compile("(?<![0-9])(0+)(?!\\d)");
        Matcher matcher = paddingPattern.matcher(protectedStructure);
        StringBuffer formattedStructure = new StringBuffer();

        while (matcher.find()) {
            int minimumDigits = matcher.group(1).length();
            String paddedCount = String.format("%0" + minimumDigits + "d", deathCount);
            matcher.appendReplacement(formattedStructure, Matcher.quoteReplacement(paddedCount));
        }

        matcher.appendTail(formattedStructure);
        return formattedStructure.toString()
            .replace(nameMarker, playerName)
            .replace(countMarker, String.valueOf(deathCount));
    }
}
