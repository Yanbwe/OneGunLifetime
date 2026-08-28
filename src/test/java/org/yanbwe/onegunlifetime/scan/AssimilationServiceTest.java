package org.yanbwe.onegunlifetime.scan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import org.junit.jupiter.api.Test;
import org.yanbwe.onegunlifetime.def.SoulGunId;
import org.yanbwe.modularshoot.component.GunData;
import org.yanbwe.modularshoot.component.ModularShootDataComponents;

class AssimilationServiceTest {

    static {
        net.neoforged.fml.loading.LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.setVersion(net.minecraft.DetectedVersion.BUILT_IN);
        net.minecraft.server.Bootstrap.bootStrap();
        net.neoforged.neoforge.registries.GameData.unfreezeData();
        COMPONENT_HOLDERS_BOUND = bindComponentHolders();
    }

    private static final boolean COMPONENT_HOLDERS_BOUND;

    private static final ResourceLocation REGULAR_GUN_ID =
            ResourceLocation.parse("test:regular_gun_" + UUID.randomUUID());

    private static boolean bindComponentHolders() {
        try {
            Field holderField = DeferredHolder.class.getDeclaredField("holder");
            holderField.setAccessible(true);
            DataComponentType<GunData> gunType =
                    new DataComponentType.Builder<GunData>().persistent(GunData.CODEC).build();
            holderField.set(ModularShootDataComponents.GUN_DATA, Holder.direct(gunType));
            return true;
        } catch (Exception e) {
            System.err.println("[AssimilationServiceTest] GUN_DATA holder bind failed: " + e);
            return false;
        }
    }

    private static Item newUniqueItem(String discriminator) {
        ResourceLocation id = ResourceLocation.parse(
                "minecraft:test_" + discriminator + "_" + UUID.randomUUID());
        Item item = new Item(new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id, item);
        return item;
    }

    private static ItemStack stackWithGunData(ResourceLocation gunId) {
        ItemStack stack = new ItemStack(newUniqueItem("gun"));
        stack.set(ModularShootDataComponents.GUN_DATA.get(),
                GunData.create(gunId, UUID.randomUUID()));
        return stack;
    }

    private static ItemStack plainStack() {
        return new ItemStack(newUniqueItem("plain"));
    }

    @Test
    void ownProjectionRecognizesOwnerSoulGun() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "GUN_DATA holder unbound in JUnit");
        UUID owner = UUID.randomUUID();

        assertTrue(AssimilationService.isOwnedProjection(
                stackWithGunData(SoulGunId.fromPlayer(owner)), owner));
    }

    @Test
    void ownProjectionRejectsOtherPlayersSoulGun() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "GUN_DATA holder unbound in JUnit");
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        assertFalse(AssimilationService.isOwnedProjection(
                stackWithGunData(SoulGunId.fromPlayer(other)), owner));
    }

    @Test
    void ownProjectionRejectsNonGunItems() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "GUN_DATA holder unbound in JUnit");

        assertFalse(AssimilationService.isOwnedProjection(plainStack(), UUID.randomUUID()));
    }

    @Test
    void ownProjectionRejectsRegularGunWithoutSoulId() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "GUN_DATA holder unbound in JUnit");

        assertFalse(AssimilationService.isOwnedProjection(
                stackWithGunData(REGULAR_GUN_ID), UUID.randomUUID()));
    }

    @Test
    void foreignGunRecognizesRegularGun() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "GUN_DATA holder unbound in JUnit");

        assertTrue(AssimilationService.isForeignGun(
                stackWithGunData(REGULAR_GUN_ID), RegistryAccess.EMPTY, UUID.randomUUID()));
    }

    @Test
    void foreignGunRejectsOwnProjection() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "GUN_DATA holder unbound in JUnit");
        UUID owner = UUID.randomUUID();

        assertFalse(AssimilationService.isForeignGun(
                stackWithGunData(SoulGunId.fromPlayer(owner)),
                RegistryAccess.EMPTY, owner));
    }

    @Test
    void foreignGunRejectsNonGunItems() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "GUN_DATA holder unbound in JUnit");

        assertFalse(AssimilationService.isForeignGun(
                plainStack(), RegistryAccess.EMPTY, UUID.randomUUID()));
    }

    @Test
    void stackedForeignGunIsStillTreatedAsOneGunEntry() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "GUN_DATA holder unbound in JUnit");
        ItemStack stack = stackWithGunData(REGULAR_GUN_ID);
        stack.setCount(5);

        assertTrue(stack.getCount() > 1);
        assertTrue(AssimilationService.isForeignGun(
                stack, RegistryAccess.EMPTY, UUID.randomUUID()));
    }
}