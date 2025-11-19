package com.dractical.fembyte.command;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@DefaultQualifier(NonNull.class)
public abstract class FembyteSubcommand {

    private final String name;
    private final List<String> aliases;
    private final @Nullable Permission permission;

    protected FembyteSubcommand(
            final String name,
            final List<String> aliases,
            final @Nullable Permission permission
    ) {
        this.name = name.toLowerCase(Locale.ENGLISH);
        this.aliases = aliases.stream()
                .map(a -> a.toLowerCase(Locale.ENGLISH))
                .toList();
        this.permission = permission;
    }

    protected FembyteSubcommand(
            final String name,
            final List<String> aliases,
            final @Nullable String permissionNode,
            final PermissionDefault permissionDefault
    ) {
        this(
                name,
                aliases,
                permissionNode == null ? null : new Permission(permissionNode, permissionDefault)
        );
    }

    public final String getName() {
        return this.name;
    }

    public final List<String> getAliases() {
        return this.aliases;
    }

    public final @Nullable Permission getPermission() {
        return this.permission;
    }

    public final boolean hasPermission(final CommandSender sender) {
        return this.permission == null || sender.hasPermission(this.permission);
    }

    public abstract boolean execute(CommandSender sender, String label, String[] args);

    public List<String> tabComplete(final CommandSender sender, final String label, final String[] args) {
        return Collections.emptyList();
    }
}
