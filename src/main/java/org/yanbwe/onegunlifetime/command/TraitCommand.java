package org.yanbwe.onegunlifetime.command;

import java.util.function.Supplier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.yanbwe.onegunlifetime.OneGunLifetimeAPI;

/**
 * Implements {@code /onegun trait add/remove}.
 *
 * <p>Validation and persistence live in
 * {@link org.yanbwe.onegunlifetime.OneGunLifetimeAPI}.</p>
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

        var result = OneGunLifetimeAPI.addTrait(player, traitId);
        return handleResult(source, result, () -> Component.translatable(
                "onegunlifetime.command.trait.add_success", traitId));
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation traitId = ResourceLocationArgument.getId(context, "traitId");

        var result = OneGunLifetimeAPI.removeTrait(player, traitId);
        return handleResult(source, result, () -> Component.translatable(
                "onegunlifetime.command.trait.remove_success", traitId));
    }

    private static int handleResult(
            CommandSourceStack source,
            OneGunLifetimeAPI.MutationResult result,
            Supplier<Component> successMessage) {
        return switch (result) {
            case SUCCESS -> {
                source.sendSuccess(successMessage, false);
                yield 1;
            }
            case NOT_BOUND -> {
                source.sendFailure(Component.translatable(
                        "onegunlifetime.command.not_bound"));
                yield 0;
            }
            default -> {
                // INVALID_TRAIT is the only reachable failure here
                source.sendFailure(Component.translatable(
                        "onegunlifetime.command.trait.invalid"));
                yield 0;
            }
        };
    }
}
