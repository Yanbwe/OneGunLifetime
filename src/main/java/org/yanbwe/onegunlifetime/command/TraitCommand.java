package org.yanbwe.onegunlifetime.command;

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
import org.yanbwe.modularshoot.registry.Trait;
import org.yanbwe.onegunlifetime.attribute.PlayerGunAttributeModifierService;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Implements {@code /onegun trait add/remove}.
 */
public final class TraitCommand {

    private TraitCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("trait")
                .then(Commands.literal("add")
                        .then(Commands.argument("traitId", ResourceLocationArgument.id())
                                .executes(TraitCommand::add)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("traitId", ResourceLocationArgument.id())
                                .executes(TraitCommand::remove)));
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation traitId = ResourceLocationArgument.getId(context, "traitId");

        if (!ensureBound(source, player)) {
            return 0;
        }
        if (!isValidTrait(player.registryAccess(), traitId)) {
            source.sendFailure(Component.translatable("onegunlifetime.command.trait.invalid"));
            return 0;
        }

        SoulDataManager.addTrait(player, traitId);
        PlayerGunAttributeModifierService.refresh(player);

        source.sendSuccess(() -> Component.translatable(
                "onegunlifetime.command.trait.add_success", traitId), false);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation traitId = ResourceLocationArgument.getId(context, "traitId");

        if (!ensureBound(source, player)) {
            return 0;
        }
        if (!isValidTrait(player.registryAccess(), traitId)) {
            source.sendFailure(Component.translatable("onegunlifetime.command.trait.invalid"));
            return 0;
        }

        SoulDataManager.removeTrait(player, traitId);
        PlayerGunAttributeModifierService.refresh(player);

        source.sendSuccess(() -> Component.translatable(
                "onegunlifetime.command.trait.remove_success", traitId), false);
        return 1;
    }

    private static boolean ensureBound(CommandSourceStack source, ServerPlayer player) {
        if (SoulDataManager.isBound(player)) {
            return true;
        }
        source.sendFailure(Component.translatable("onegunlifetime.command.not_bound"));
        return false;
    }

    private static boolean isValidTrait(RegistryAccess registryAccess, ResourceLocation traitId) {
        Registry<Trait> traitRegistry =
                registryAccess.registry(ModularShootRegistries.TRAITS_KEY).orElse(null);
        return traitRegistry != null && traitRegistry.get(traitId) != null;
    }
}