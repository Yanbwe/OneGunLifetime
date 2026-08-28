package org.yanbwe.onegunlifetime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredHolder;

import org.junit.jupiter.api.Test;
import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.modularshoot.component.GunData;
import org.yanbwe.modularshoot.component.ModularShootDataComponents;
import org.yanbwe.modularshoot.component.PluginData;
import org.yanbwe.onegunlifetime.def.SoulGunId;

class MemorialConverterTest {

    static {
        net.neoforged.fml.loading.LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.setVersion(net.minecraft.DetectedVersion.BUILT_IN);
        net.minecraft.server.Bootstrap.bootStrap();
        net.neoforged.neoforge.registries.GameData.unfreezeData();
        COMPONENT_HOLDERS_BOUND = bindComponentHolders();
    }

    private static final boolean COMPONENT_HOLDERS_BOUND;

    private static boolean bindComponentHolders() {
        try {
            bind(ModularShootDataComponents.GUN_DATA,
                    new DataComponentType.Builder<GunData>().persistent(GunData.CODEC).build());
            bind(ModularShootDataComponents.PLUGIN_DATA,
                    new DataComponentType.Builder<PluginData>().persistent(PluginData.CODEC).build());
            return true;
        } catch (Exception e) {
            System.err.println("[MemorialConverterTest] component holder bind failed: " + e);
            return false;
        }
    }

    private static <T> void bind(
            DeferredHolder<DataComponentType<?>, DataComponentType<T>> holder,
            DataComponentType<T> componentType) throws Exception {
        Field holderField = DeferredHolder.class.getDeclaredField("holder");
        holderField.setAccessible(true);
        holderField.set(holder, Holder.direct(componentType));
    }

    private static ItemStack projectionStack() {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(ModularShootDataComponents.GUN_DATA.get(),
                GunData.create(SoulGunId.fromPlayer(UUID.randomUUID()), UUID.randomUUID()));
        stack.set(ModularShootDataComponents.PLUGIN_DATA.get(),
                new PluginData(ResourceLocation.parse("test:memorial_plugin_" + UUID.randomUUID())));
        return stack;
    }

    @Test
    void toMemorialRemovesGunAndPluginComponents() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "ModularShoot component holders unbound in JUnit");
        ItemStack stack = projectionStack();

        assertTrue(stack.has(ModularShootDataComponents.GUN_DATA.get()));
        assertTrue(stack.has(ModularShootDataComponents.PLUGIN_DATA.get()));

        MemorialConverter.toMemorial(stack);

        assertFalse(stack.has(ModularShootDataComponents.GUN_DATA.get()));
        assertFalse(stack.has(ModularShootDataComponents.PLUGIN_DATA.get()));
        assertEquals(ItemAttributeModifiers.EMPTY, stack.get(DataComponents.ATTRIBUTE_MODIFIERS));
        assertEquals(
                Component.translatable(MemorialConverter.MEMORIAL_NAME_KEY),
                stack.get(DataComponents.CUSTOM_NAME));
    }

    @Test
    void memorialIsNoLongerRecognisedAsProjectionOrGun() {
        assumeTrue(COMPONENT_HOLDERS_BOUND, "ModularShoot component holders unbound in JUnit");
        ItemStack stack = projectionStack();
        assertTrue(ContainerGuard.isProjectionGun(stack, null));

        MemorialConverter.toMemorial(stack);

        assertFalse(ContainerGuard.isProjectionGun(stack, null));
        assertTrue(ModularShootAPI.getGunId(stack).isEmpty());
        assertFalse(ModularShootAPI.isGun(stack));
    }
}