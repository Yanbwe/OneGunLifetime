package org.yanbwe.onegunlifetime.soul;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.yanbwe.modularshoot.registry.gun.GunRegistry;

/**
 * Static service for reading and writing a player's {@link SoulData}
 * attachment.
 *
 * <p>All server-side mutations write the new value back through
 * {@code Player#setData} and then fire a {@link SoulDataChangedEvent} on the
 * NeoForge event bus. On the client these mutators are deliberately no-ops;
 * the server is authoritative for soul data.</p>
 *
 * <p>“Unbound” is represented by <em>no attachment entry</em>. This class
 * uses {@code getExistingDataOrNull}, so unlike {@code getData} it never
 * materialises the {@code null} default into the attachment map.</p>
 */
public final class SoulDataManager {
    private SoulDataManager() {
    }

    /**
     * Returns the player's soul data, or {@code null} when unbound.
     *
     * @param player the player to query
     * @return the bound {@link SoulData}, or {@code null}
     */
    @Nullable
    public static SoulData get(Player player) {
        SoulData data = player.getExistingDataOrNull(OneGunAttachmentTypes.PLAYER_SOUL);
        if (data == null || data.version() == SoulDataVersion.CURRENT) {
            return data;
        }

        SoulData upgraded = SoulDataMigration.upgrade(data);
        if (upgraded != data && player instanceof ServerPlayer serverPlayer) {
            write(serverPlayer, upgraded);
        }
        return upgraded;
    }

    /**
     * Returns whether the player currently has a soul binding.
     */
    public static boolean isBound(Player player) {
        return get(player) != null;
    }

    /**
     * Creates and stores a fresh soul binding.
     *
     * <p><strong>Policy:</strong> binding is one-time. If the player is already
     * bound this method throws {@link IllegalStateException}; the command layer
     * is responsible for translating that into a user-friendly rejection.</p>
     *
     * @param player        the player to bind
     * @param templateGunId the template gun definition id
     * @return the newly stored {@link SoulData}
     * @throws IllegalStateException when the player is already bound
     */
    public static SoulData bind(Player player, ResourceLocation templateGunId) {
        if (player.level().isClientSide()) {
            return get(player);
        }
        SoulData existing = get(player);
        if (existing != null) {
            throw new IllegalStateException("Player " + player.getGameProfile().getName()
                    + " is already bound to " + existing.templateGunId());
        }
        SoulData data = SoulData.create(player.getUUID(), templateGunId);
        // A fresh binding carries the template's inherent traits as the
        // player's initial final-trait set; an empty trait set thereafter
        // explicitly means the soul gun has no traits.
        var templateOpt = GunRegistry.getGun(player.registryAccess(), templateGunId);
        if (templateOpt.isPresent()) {
            data = data.withTraits(templateOpt.get().traits().keySet());
        }
        if (player instanceof ServerPlayer serverPlayer) {
            write(serverPlayer, data);
        }
        return data;
    }

    /**
     * Removes the player's soul binding. Safe to call on an already unbound player.
     */
    public static void unbind(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        player.removeData(OneGunAttachmentTypes.PLAYER_SOUL);
        if (player instanceof ServerPlayer serverPlayer) {
            NeoForge.EVENT_BUS.post(new SoulDataChangedEvent(serverPlayer, null));
        }
    }

    public static SoulData setStatOverride(ServerPlayer player, ResourceLocation key, double value) {
        if (player.level().isClientSide()) {
            return get(player);
        }
        SoulData newData = requireBound(player).withStatOverride(key, value);
        return write(player, newData);
    }

    public static SoulData clearStatOverrides(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return get(player);
        }
        SoulData newData = requireBound(player).clearStatOverrides();
        return write(player, newData);
    }

    public static SoulData removeStatOverride(ServerPlayer player, ResourceLocation key) {
        if (player.level().isClientSide()) {
            return get(player);
        }
        SoulData newData = requireBound(player).withoutStatOverride(key);
        return write(player, newData);
    }

    public static SoulData addTrait(ServerPlayer player, ResourceLocation traitId) {
        if (player.level().isClientSide()) {
            return get(player);
        }
        SoulData newData = requireBound(player).withTrait(traitId);
        return write(player, newData);
    }

    public static SoulData removeTrait(ServerPlayer player, ResourceLocation traitId) {
        if (player.level().isClientSide()) {
            return get(player);
        }
        SoulData newData = requireBound(player).withoutTrait(traitId);
        return write(player, newData);
    }

    public static SoulData setPlugins(ServerPlayer player, List<org.yanbwe.modularshoot.component.PluginInstance> plugins) {
        if (player.level().isClientSide()) {
            return get(player);
        }
        SoulData newData = requireBound(player).withPlugins(plugins);
        return write(player, newData);
    }

    public static SoulData setGunState(ServerPlayer player, CompoundTag state) {
        if (player.level().isClientSide()) {
            return get(player);
        }
        SoulData newData = requireBound(player).withGunState(state);
        return write(player, newData);
    }

    private static SoulData requireBound(ServerPlayer player) {
        SoulData data = get(player);
        if (data == null) {
            throw new IllegalStateException("Player " + player.getGameProfile().getName()
                    + " has no soul binding");
        }
        return data;
    }

    private static SoulData write(ServerPlayer player, SoulData data) {
        player.setData(OneGunAttachmentTypes.PLAYER_SOUL, data);
        NeoForge.EVENT_BUS.post(new SoulDataChangedEvent(player, data));
        return data;
    }
}