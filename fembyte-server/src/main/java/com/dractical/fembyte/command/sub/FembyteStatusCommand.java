package com.dractical.fembyte.command.sub;

import com.dractical.fembyte.command.FembyteCommand;
import com.dractical.fembyte.command.FembyteSubcommand;
import com.dractical.fembyte.perf.TickThrottleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Collections;
import java.util.Locale;

@DefaultQualifier(NonNull.class)
public class FembyteStatusCommand extends FembyteSubcommand {

    public static final String NAME = "status";
    public static final String PERM = FembyteCommand.BASE_PERM + "." + NAME;

    public FembyteStatusCommand() {
        super(NAME, Collections.emptyList(), PERM, PermissionDefault.OP);
    }

    @Override
    public boolean execute(final CommandSender sender, final String label, final String[] args) {
        final TickThrottleManager.TickThrottleMetrics metrics = TickThrottleManager.get().snapshotMetrics();
        sender.sendMessage(Component.text("Tick Throttle", NamedTextColor.AQUA));
        if (!metrics.enabled()) {
            sender.sendMessage(Component.text("  Module disabled.", NamedTextColor.GRAY));
            return true;
        }

        sender.sendMessage(Component.text("  TPS: ", NamedTextColor.GRAY)
                .append(Component.text(formatDouble(metrics.currentTps()) + " / 20.0", NamedTextColor.GREEN)));

        final NamedTextColor randomColor = metrics.randomTickScale() < 0.999D ? NamedTextColor.GOLD : NamedTextColor.GREEN;
        sender.sendMessage(Component.text("  Random ticks: ", NamedTextColor.GRAY)
                .append(Component.text(formatPercent(metrics.randomTickScale()), randomColor))
                .append(Component.text(" (" + metrics.randomTicksSuppressed() + " suppressed)", NamedTextColor.DARK_GRAY)));

        sender.sendMessage(Component.text("  Passive mobs: ", NamedTextColor.GRAY)
                .append(Component.text(formatInterval(metrics.passiveInterval(), metrics.passiveTickingThisRound()), NamedTextColor.YELLOW))
                .append(Component.text(" (" + metrics.passiveTicksSuppressed() + " skipped)", NamedTextColor.DARK_GRAY)));

        sender.sendMessage(Component.text("  Hostile mobs: ", NamedTextColor.GRAY)
                .append(Component.text(formatInterval(metrics.hostileInterval(), metrics.hostileTickingThisRound()), NamedTextColor.YELLOW))
                .append(Component.text(" (" + metrics.hostileTicksSuppressed() + " skipped)", NamedTextColor.DARK_GRAY)));

        return true;
    }

    private static String formatDouble(final double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String formatPercent(final double value) {
        return String.format(Locale.US, "%.0f%%", value * 100.0D);
    }

    private static String formatInterval(final int interval, final boolean tickingThisRound) {
        if (interval <= 1) {
            return "every tick";
        }
        final String cadence = "1 in " + interval + " ticks";
        return cadence + (tickingThisRound ? " (processing)" : " (deferred)");
    }
}
