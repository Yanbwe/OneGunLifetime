package org.yanbwe.onegunlifetime.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.yanbwe.modularshoot.registry.ModularShootRegistries;
import org.yanbwe.modularshoot.registry.attribute.AttributeMeta;
import org.yanbwe.onegunlifetime.attribute.PlayerGunAttributeModifierService;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Implements {@code /onegun stat set/reset}.
 */
public final class StatCommand {

    private StatCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("stat")
                .then(Commands.literal("set")
                        .then(Commands.argument("attribute", ResourceLocationArgument.id())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(StatCommand::set))))
                .then(Commands.literal("reset")
                        .executes(StatCommand::resetAll)
                        .then(Commands.argument("attribute", ResourceLocationArgument.id())
                                .executes(StatCommand::resetOne)));
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation attributeId = ResourceLocationArgument.getId(context, "attribute");
        double value = DoubleArgumentType.getDouble(context, "value");

        if (!ensureBound(source, player)) {
            return 0;
        }
        if (!Double.isFinite(value)) {
            source.sendFailure(Component.translatable("onegunlifetime.command.stat.invalid"));
            return 0;
        }
        if (!isValidAttribute(player.registryAccess(), attributeId)) {
            source.sendFailure(Component.translatable("onegunlifetime.command.stat.invalid"));
            return 0;
        }

        SoulDataManager.setStatOverride(player, attributeId, value);
        PlayerGunAttributeModifierService.refresh(player);

        source.sendSuccess(() -> Component.translatable(
                "onegunlifetime.command.stat.set_success", attributeId, value), false);
        return 1;
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        if (!ensureBound(source, player)) {
            return 0;
        }

        SoulDataManager.clearStatOverrides(player);
        PlayerGunAttributeModifierService.refresh(player);

        source.sendSuccess(() -> Component.translatable(
                "onegunlifetime.command.stat.reset_success"), false);
        return 1;
    }

    private static int resetOne(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation attributeId = ResourceLocationArgument.getId(context, "attribute");

        if (!ensureBound(source, player)) {
            return 0;
        }
        if (!isValidAttribute(player.registryAccess(), attributeId)) {
            source.sendFailure(Component.translatable("onegunlifetime.command.stat.invalid"));
            return 0;
        }

        SoulDataManager.removeStatOverride(player, attributeId);
        PlayerGunAttributeModifierService.refresh(player);

        source.sendSuccess(() -> Component.translatable(
                "onegunlifetime.command.stat.reset_success"), false);
        return 1;
    }

    private static boolean ensureBound(CommandSourceStack source, ServerPlayer player) {
        if (SoulDataManager.isBound(player)) {
            return true;
        }
        source.sendFailure(Component.translatable("onegunlifetime.command.not_bound"));
        return false;
    }

    private static boolean isValidAttribute(RegistryAccess registryAccess, ResourceLocation attributeId) {
        Registry<AttributeMeta> metaRegistry =
                registryAccess.registry(ModularShootRegistries.ATTRIBUTE_META_KEY).orElse(null);
        return metaRegistry != null && metaRegistry.get(attributeId) != null;
    }
}