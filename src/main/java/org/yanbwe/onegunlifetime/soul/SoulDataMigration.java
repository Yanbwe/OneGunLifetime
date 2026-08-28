package org.yanbwe.onegunlifetime.soul;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;

/**
 * One-way migration entry point for {@link SoulData}.
 *
 * <p>At schema version 1 there is nothing to migrate. The important safety
 * property is that unknown newer versions never crash: they are downgraded to
 * a minimal valid soul record that keeps the binding identity
 * (owner/template/stable gun id) but discards fields whose meaning we cannot
 * reliably interpret.</p>
 */
public final class SoulDataMigration {
    private SoulDataMigration() {
    }

    /**
     * Upgrades (or safely downgrades) a soul record to the current schema.
     *
     * @param data the raw soul data read from storage
     * @return a current-version {@link SoulData}; never throws for version
     *         mismatches
     */
    public static SoulData upgrade(SoulData data) {
        if (data.version() == SoulDataVersion.CURRENT) {
            return data;
        }

        if (data.version() < SoulDataVersion.CURRENT) {
            // Stage 1 only has v1, so no older migration is needed yet.
            return data;
        }

        // Unknown future version: keep the binding identity, clear everything else.
        return new SoulData(
                data.ownerId(),
                data.templateGunId(),
                Map.of(),
                Set.of(),
                List.of(),
                new CompoundTag(),
                SoulDataVersion.CURRENT,
                data.stableGunId()
        );
    }
}