package org.yanbwe.onegunlifetime.plugin;

import net.minecraft.server.level.ServerPlayer;
import org.yanbwe.onegunlifetime.attribute.PlayerGunAttributeModifierService;

/**
 * Semantic facade for refreshing a server player's mounted gun attributes
 * after plugin-related soul data changes.
 *
 * <p>Currently this simply delegates to
 * {@link PlayerGunAttributeModifierService#refresh}; it exists as a stable
 * extension point for later plugin-aware refresh logic.</p>
 */
public final class PluginRefresher {

    private PluginRefresher() {
    }

    /**
     * Refreshes the player's mounted soul-gun attributes from current soul
     * data.
     *
     * @param player the server player to refresh; may be {@code null}
     */
    public static void refresh(ServerPlayer player) {
        PlayerGunAttributeModifierService.refresh(player);
    }
}