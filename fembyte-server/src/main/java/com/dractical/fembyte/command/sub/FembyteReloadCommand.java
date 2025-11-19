package com.dractical.fembyte.command.sub;

import com.dractical.fembyte.command.FembyteCommand;
import com.dractical.fembyte.command.FembyteSubcommand;
import com.dractical.fembyte.config.FembyteConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.List;

@DefaultQualifier(NonNull.class)
public class FembyteReloadCommand extends FembyteSubcommand {

    public static final String NAME = "reload";
    public static final String PERM = FembyteCommand.BASE_PERM + "." + NAME;

    public FembyteReloadCommand() {
        super(NAME, List.of("r"), PERM, PermissionDefault.OP);
    }

    @Override
    public boolean execute(final CommandSender sender, final String label, final String[] args) {
        Command.broadcastCommandMessage(sender,
                Component.text("Reloading fembyte config...", NamedTextColor.GREEN));
        FembyteConfig.reloadAsync(sender);
        return true;
    }
}
