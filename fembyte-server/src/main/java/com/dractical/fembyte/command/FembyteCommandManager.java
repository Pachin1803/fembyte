package com.dractical.fembyte.command;

import net.minecraft.server.MinecraftServer;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.util.permissions.CraftDefaultPermissions;

import java.util.Map;

public final class FembyteCommandManager {

    public static final String COMMAND_BASE_PERM = CraftDefaultPermissions.FEMBYTE_ROOT + ".command";

    private FembyteCommandManager() {
    }

    private static final Map<String, Command> COMMANDS;

    static {
        COMMANDS = Map.of(FembyteCommand.COMMAND_LABEL, new FembyteCommand());
    }

    public static void registerCommands(final MinecraftServer server) {
        COMMANDS.forEach((label, command) ->
                server.server.getCommandMap().register(label, "Fembyte", command));
    }
}
