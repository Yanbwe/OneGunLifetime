package org.yanbwe.onegunlifetime.command;

import java.util.function.Supplier;

import com.mojang.brigadier.arguments.DoubleArgumentType;
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
 * Implements {@code /onegun stat set/reset}.
 *
 * <p>Validation and persistence live in
 * {@link org.yanbwe.onegunlifetime.OneGunLifetimeAPI}.</p>
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

        var result = OneGunLifetimeAPI.setStatOverride(player, attributeId, value);
        return handleResult(source, result, () -> Component.translatable(
                "onegunlifetime.command.stat.set_success", attributeId, value));
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        var result = OneGunLifetimeAPI.clearStatOverrides(player);
        return handleResult(source, result,
                () -> Component.translatable("onegunlifetime.command.stat.reset_success"));
    }

    private static int resetOne(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation attributeId = ResourceLocationArgument.getId(context, "attribute");

        var result = OneGunLifetimeAPI.removeStatOverride(player, attributeId);
        return handleResult(source, result,
                () -> Component.translatable("onegunlifetime.command.stat.reset_success"));
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
            case INVALID_VALUE -> {
                source.sendFailure(Component.translatable(
                        "onegunlifetime.command.stat.invalid_value"));
                yield 0;
            }
            case INVALID_ATTRIBUTE, INVALID_TRAIT -> {
                // INVALID_TRAIT is unreachable in the stat path.
                source.sendFailure(Component.translatable(
                        "onegunlifetime.command.stat.invalid_attribute"));
                yield 0;
            }
        };
    }
}
