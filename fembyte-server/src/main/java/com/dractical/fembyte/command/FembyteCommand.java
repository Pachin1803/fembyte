package com.dractical.fembyte.command;

import com.dractical.fembyte.command.sub.FembyteReloadCommand;
import com.dractical.fembyte.command.sub.FembyteStatusCommand;
import io.papermc.paper.command.CommandUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class FembyteCommand extends Command {

    public static final String COMMAND_LABEL = "fembyte";
    public static final String BASE_PERM =
            FembyteCommandManager.COMMAND_BASE_PERM + "." + COMMAND_LABEL;

    private static final Permission BASE_PERMISSION =
            new Permission(BASE_PERM, PermissionDefault.OP);

    private final Map<String, FembyteSubcommand> subcommands = new LinkedHashMap<>();
    private final Map<String, String> aliasToPrimary = new HashMap<>();

    public FembyteCommand() {
        super(COMMAND_LABEL);
        this.description = "Root command for Fembyte functionality";
        this.setPermission(BASE_PERM);
        registerSubcommand(new FembyteReloadCommand());
        registerSubcommand(new FembyteStatusCommand());
        this.usageMessage = createUsageMessage(this.subcommands.keySet());
        final PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        if (pluginManager.getPermission(BASE_PERM) == null) {
            pluginManager.addPermission(BASE_PERMISSION);
        }
        for (final FembyteSubcommand sub : this.subcommands.values()) {
            final @Nullable Permission permission = sub.getPermission();
            if (permission != null && pluginManager.getPermission(permission.getName()) == null) {
                pluginManager.addPermission(permission);
            }
        }
    }

    private void registerSubcommand(final FembyteSubcommand subcommand) {
        final String name = subcommand.getName();
        if (this.subcommands.putIfAbsent(name, subcommand) != null) {
            throw new IllegalStateException("Duplicate Fembyte subcommand name: " + name);
        }

        for (final String alias : subcommand.getAliases()) {
            this.aliasToPrimary.put(alias, name);
        }
    }

    private String createUsageMessage(final Collection<String> arguments) {
        if (arguments.isEmpty()) {
            return "/" + COMMAND_LABEL;
        }
        return "/" + COMMAND_LABEL + " [" + String.join(" | ", arguments) + "]";
    }

    private List<String> accessiblePrimaryLabels(final CommandSender sender) {
        return this.subcommands.values().stream()
                .filter(sub -> sub.hasPermission(sender))
                .map(FembyteSubcommand::getName)
                .toList();
    }

    private boolean hasAnySubcommandPermission(final CommandSender sender) {
        return this.subcommands.values().stream().anyMatch(sub -> sub.hasPermission(sender));
    }

    private record ResolvedSubcommand(String label, FembyteSubcommand handler) {
    }

    private @Nullable ResolvedSubcommand resolveSubcommand(final String raw) {
        String key = raw.toLowerCase(Locale.ENGLISH);

        FembyteSubcommand handler = this.subcommands.get(key);
        String resolvedLabel = key;

        if (handler == null) {
            final String primary = this.aliasToPrimary.get(key);
            if (primary == null) {
                return null;
            }
            resolvedLabel = primary;
            handler = this.subcommands.get(primary);
        }

        if (handler == null) {
            return null;
        }

        return new ResolvedSubcommand(resolvedLabel, handler);
    }

    @Override
    public boolean execute(
            final CommandSender sender,
            final @NotNull String commandLabel,
            final @NotNull String[] args
    ) {
        if (!sender.hasPermission(BASE_PERMISSION) || !this.hasAnySubcommandPermission(sender)) {
            sender.sendMessage(Bukkit.permissionMessage());
            return true;
        }

        final List<String> availableLabels = accessiblePrimaryLabels(sender);
        final String specificUsage = createUsageMessage(availableLabels);

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text(specificUsage, NamedTextColor.GRAY)));
            return false;
        }

        final @Nullable ResolvedSubcommand resolved = resolveSubcommand(args[0]);
        if (resolved == null || !resolved.handler().hasPermission(sender)) {
            sender.sendMessage(Component.text("Usage: ", NamedTextColor.RED)
                    .append(Component.text(specificUsage, NamedTextColor.GRAY)));
            return false;
        }

        final String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
        return resolved.handler().execute(sender, resolved.label(), remainingArgs);
    }

    @Override
    public @NotNull List<String> tabComplete(
            final @NotNull CommandSender sender,
            final @NotNull String alias,
            final String[] args,
            final @Nullable Location location
    ) throws IllegalArgumentException {
        if (args.length <= 1) {
            final List<String> subCommandArguments = this.subcommands.values().stream()
                    .filter(sub -> sub.hasPermission(sender))
                    .map(FembyteSubcommand::getName)
                    .collect(Collectors.toList());

            return CommandUtil.getListMatchingLast(sender, args, subCommandArguments);
        }

        final @Nullable ResolvedSubcommand resolved = resolveSubcommand(args[0]);
        if (resolved != null && resolved.handler().hasPermission(sender)) {
            final String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
            return resolved.handler().tabComplete(sender, resolved.label(), remainingArgs);
        }

        return Collections.emptyList();
    }
}
