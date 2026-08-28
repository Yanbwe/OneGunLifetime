package org.yanbwe.onegunlifetime.soul;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the NeoForge event bus after a player's soul data is successfully
 * written back by {@link SoulDataManager}.
 *
 * @param player the server player whose soul data changed
 * @param data   the new soul data, or {@code null} when the player was unbound
 */
public class SoulDataChangedEvent extends Event {
    private final ServerPlayer player;
    @Nullable
    private final SoulData data;

    public SoulDataChangedEvent(ServerPlayer player, @Nullable SoulData data) {
        this.player = player;
        this.data = data;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    @Nullable
    public SoulData getData() {
        return data;
    }
}