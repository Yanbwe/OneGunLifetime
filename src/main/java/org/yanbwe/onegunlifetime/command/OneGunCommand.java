package org.yanbwe.onegunlifetime.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.yanbwe.onegunlifetime.OneGunLifetime;

/**
 * Registers the {@code /onegun} command tree on the NeoForge game event bus.
 *
 * <p>The root command has no permission gate. Only the destructive
 * {@code unbind} subcommand requires op permission level {@code 2}; the rest
 * are player-facing soul-gun management commands.</p>
 */
@EventBusSubscriber(modid = OneGunLifetime.MODID)
public final class OneGunCommand {

    private OneGunCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("onegun")
                .then(BindCommand.create())
                .then(StatCommand.create())
                .then(TraitCommand.create())
                .then(InfoCommand.create())
                .then(AdminUnbindCommand.create()));
    }
}