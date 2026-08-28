package org.yanbwe.onegunlifetime.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SoulGunIdTest {

    @Test
    void fromPlayerRoundTripsThroughParse() {
        UUID ownerId = UUID.randomUUID();

        ResourceLocation gunId = SoulGunId.fromPlayer(ownerId);
        Optional<UUID> parsed = SoulGunId.parse(gunId);

        assertTrue(parsed.isPresent());
        assertEquals(ownerId, parsed.get());
        assertTrue(SoulGunId.isSoulGunId(gunId));
    }

    @Test
    void encodedIdUsesExpectedNamespaceAndLowercaseNoDashUuid() {
        UUID ownerId = UUID.fromString("12345678-9abc-def0-1234-56789abcdef0");

        ResourceLocation gunId = SoulGunId.fromPlayer(ownerId);

        assertEquals("onegunlifetime", gunId.getNamespace());
        assertEquals("soul_123456789abcdef0123456789abcdef0", gunId.getPath());
        assertTrue(gunId.getPath().chars().noneMatch(Character::isUpperCase));
    }

    @Test
    void parseRejectsNonSoulIds() {
        assertFalse(SoulGunId.isSoulGunId(ResourceLocation.fromNamespaceAndPath("other", "soul_123456789abcdef0123456789abcdef0")));
        assertTrue(SoulGunId.parse(ResourceLocation.fromNamespaceAndPath("other", "soul_123456789abcdef0123456789abcdef0")).isEmpty());
        assertFalse(SoulGunId.isSoulGunId(ResourceLocation.fromNamespaceAndPath("onegunlifetime", "notsoul_123456789abcdef0123456789abcdef0")));
        assertTrue(SoulGunId.parse(ResourceLocation.fromNamespaceAndPath("onegunlifetime", "notsoul_123456789abcdef0123456789abcdef0")).isEmpty());
    }

    @Test
    void parseRejectsWrongUuidLengthOrBadCharacters() {
        assertFalse(SoulGunId.isSoulGunId(ResourceLocation.fromNamespaceAndPath("onegunlifetime", "soul_123456789abcdef0")));
        assertTrue(SoulGunId.parse(ResourceLocation.fromNamespaceAndPath("onegunlifetime", "soul_123456789abcdef0")).isEmpty());

        assertFalse(SoulGunId.isSoulGunId(ResourceLocation.fromNamespaceAndPath("onegunlifetime", "soul_zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")));
        assertTrue(SoulGunId.parse(ResourceLocation.fromNamespaceAndPath("onegunlifetime", "soul_zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz")).isEmpty());
    }
}