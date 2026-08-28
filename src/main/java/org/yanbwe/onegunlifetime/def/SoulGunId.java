package org.yanbwe.onegunlifetime.def;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.yanbwe.onegunlifetime.OneGunLifetime;

/**
 * Encoding/decoding for OneGunLifetime's exclusive dynamic gun ids.
 *
 * <p>The id form is {@code onegunlifetime:soul_<uuid-without-hyphens>}.</p>
 *
 * <p>These ids are never part of any registry; they are resolved at query time
 * by {@link DynamicGunDefinitionProvider} from the per-player soul data that
 * has been synchronised to the current side.</p>
 */
public final class SoulGunId {
    private static final String PREFIX = "soul_";
    private static final int UUID_HEX_LENGTH = 32;

    private SoulGunId() {
    }

    /**
     * Creates the dynamic gun id for a soul owner.
     *
     * @param ownerId the owning player's UUID
     * @return a {@code onegunlifetime:soul_*} {@link ResourceLocation}
     */
    public static ResourceLocation fromPlayer(UUID ownerId) {
        String uuidHex = ownerId.toString().replace("-", "").toLowerCase();
        return ResourceLocation.fromNamespaceAndPath(OneGunLifetime.MODID, PREFIX + uuidHex);
    }

    /**
     * Attempts to parse a soul gun id back to the owner UUID.
     *
     * <p>Only ids whose namespace is {@code onegunlifetime} and whose path is
     * exactly {@code soul_<32 hex chars>} are accepted. Parsing is
     * case-insensitive for the hex digits so manually produced ids remain
     * usable, while the canonical {@linkplain #fromPlayer encoded} form is
     * always lowercase.</p>
     *
     * @param gunId the gun id to parse
     * @return the owner UUID, or {@link Optional#empty()} when the id is not a
     *         well-formed soul gun id
     */
    public static Optional<UUID> parse(ResourceLocation gunId) {
        if (!isSoulGunId(gunId)) {
            return Optional.empty();
        }
        String hex = gunId.getPath().substring(PREFIX.length());
        try {
            long mostSigBits = Long.parseUnsignedLong(hex.substring(0, 16), 16);
            long leastSigBits = Long.parseUnsignedLong(hex.substring(16), 16);
            return Optional.of(new UUID(mostSigBits, leastSigBits));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns whether the given id is a syntactically valid soul gun id.
     *
     * @param gunId the gun id to test; may be {@code null}
     * @return {@code true} when the id is a valid {@code onegunlifetime:soul_*}
     *         id
     */
    public static boolean isSoulGunId(ResourceLocation gunId) {
        if (gunId == null || !OneGunLifetime.MODID.equals(gunId.getNamespace())) {
            return false;
        }
        String path = gunId.getPath();
        return path.startsWith(PREFIX)
                && path.length() == PREFIX.length() + UUID_HEX_LENGTH
                && path.substring(PREFIX.length()).chars().allMatch(SoulGunId::isHexDigit);
    }

    private static boolean isHexDigit(int c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }
}