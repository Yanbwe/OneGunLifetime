package org.yanbwe.onegunlifetime.command;

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
 * Implements {@code /onegun bind <gunId>}.
 *
 * <p>Binds the executor's soul to a registered template gun, gives them a new
 * projection gun, and refreshes their mounted soul-gun attributes.
 * Implementation lives in
 * {@link org.yanbwe.onegunlifetime.OneGunLifetimeAPI#bindAndGive}.</p>
 */
public final class BindCommand {

    private BindCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("bind")
                .then(Commands.argument("gunId", ResourceLocationArgument.id())
                        .executes(BindCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation gunId = ResourceLocationArgument.getId(context, "gunId");

        return switch (OneGunLifetimeAPI.bindAndGive(player, gunId)) {
            case SUCCESS -> {
                source.sendSuccess(() -> Component.translatable(
                        "onegunlifetime.command.bind.success", gunId), false);
                yield 1;
            }
            case ALREADY_BOUND -> {
                source.sendFailure(Component.translatable(
                        "onegunlifetime.command.bind.already_bound"));
                yield 0;
            }
            case NOT_REGISTERED -> {
                source.sendFailure(Component.translatable(
                        "onegunlifetime.command.bind.not_registered"));
                yield 0;
            }
        };
    }
}
