package com.dractical.fembyte.perf;

import com.dractical.fembyte.config.modules.performance.TickThrottleModule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

import javax.annotation.Nullable;

public final class TickThrottleManager {

    private static final TickThrottleManager INSTANCE = new TickThrottleManager();

    public static TickThrottleManager get() {
        return INSTANCE;
    }

    private long lastServerTick = Long.MIN_VALUE;
    private double currentTps = 20.0D;

    private double randomTickScale = 1.0D;
    private int passiveInterval = 1;
    private int hostileInterval = 1;

    private long passiveCursor = 0L;
    private long hostileCursor = 0L;
    private boolean passiveAllowedThisTick = true;
    private boolean hostileAllowedThisTick = true;

    private long suppressedRandomTicks = 0L;
    private long suppressedPassiveTicks = 0L;
    private long suppressedHostileTicks = 0L;

    private TickThrottleManager() {
    }

    public void beginWorldTick(final ServerLevel level, final boolean runsNormally) {
        final MinecraftServer server = level.getServer();
        final int currentTick = server.getTickCount();
        if (this.lastServerTick == currentTick) {
            return;
        }

        this.lastServerTick = currentTick;
        if (!TickThrottleModule.ENABLED || !runsNormally) {
            this.resetState();
            return;
        }

        this.refreshState(server);
    }

    private void resetState() {
        this.currentTps = 20.0D;
        this.randomTickScale = 1.0D;
        this.passiveInterval = 1;
        this.hostileInterval = 1;
        this.passiveCursor = 0L;
        this.hostileCursor = 0L;
        this.passiveAllowedThisTick = true;
        this.hostileAllowedThisTick = true;
    }

    private void refreshState(final MinecraftServer server) {
        final double mspt = server.getAverageTickTimeNanos() / 1_000_000.0D;
        double calculatedTps = mspt <= 0.0D ? 20.0D : Math.min(20.0D, 1000.0D / Math.max(mspt, 0.0001D));
        this.currentTps = Math.max(0.0D, Math.min(20.0D, calculatedTps));

        this.randomTickScale = this.computeScale(
                this.currentTps,
                TickThrottleModule.RANDOM_ACTIVATION_TPS,
                TickThrottleModule.RANDOM_MIN_TPS,
                TickThrottleModule.RANDOM_MIN_SCALE
        );

        this.passiveInterval = this.computeInterval(
                this.currentTps,
                TickThrottleModule.PASSIVE_ACTIVATION_TPS,
                TickThrottleModule.PASSIVE_MIN_TPS,
                TickThrottleModule.PASSIVE_MAX_INTERVAL
        );
        this.hostileInterval = this.computeInterval(
                this.currentTps,
                TickThrottleModule.HOSTILE_ACTIVATION_TPS,
                TickThrottleModule.HOSTILE_MIN_TPS,
                TickThrottleModule.HOSTILE_MAX_INTERVAL
        );

        if (this.passiveInterval <= 1) {
            this.passiveAllowedThisTick = true;
            this.passiveCursor = 0L;
        } else {
            this.passiveAllowedThisTick = (this.passiveCursor++ % this.passiveInterval) == 0;
        }

        if (this.hostileInterval <= 1) {
            this.hostileAllowedThisTick = true;
            this.hostileCursor = 0L;
        } else {
            this.hostileAllowedThisTick = (this.hostileCursor++ % this.hostileInterval) == 0;
        }
    }

    private double computeScale(final double tps, final double activationTps, final double minTps, final double minScale) {
        final double severity = this.computeSeverity(tps, activationTps, minTps);
        if (severity <= 0.0D) {
            return 1.0D;
        }

        final double clampedMinScale = Math.max(0.0D, Math.min(1.0D, minScale));
        final double delta = 1.0D - clampedMinScale;
        return Math.max(clampedMinScale, 1.0D - severity * delta);
    }

    private int computeInterval(final double tps, final double activationTps, final double minTps, final int maxInterval) {
        if (maxInterval <= 1) {
            return 1;
        }

        final double severity = this.computeSeverity(tps, activationTps, minTps);
        if (severity <= 0.0D) {
            return 1;
        }

        final double intervalDelta = (maxInterval - 1) * severity;
        final int additional = Math.max(0, (int)Math.round(intervalDelta));
        return Math.min(maxInterval, 1 + additional);
    }

    private double computeSeverity(final double tps, final double activationTps, final double minTps) {
        final double clampedActivation = Math.max(0.0D, Math.min(20.0D, activationTps));
        final double clampedMin = Math.max(0.0D, Math.min(clampedActivation, minTps));

        if (tps >= clampedActivation || clampedActivation <= clampedMin) {
            return 0.0D;
        }

        if (tps <= clampedMin) {
            return 1.0D;
        }

        return (clampedActivation - tps) / (clampedActivation - clampedMin);
    }

    public int scaleRandomTickSpeed(final int baseTickSpeed) {
        if (!TickThrottleModule.ENABLED || baseTickSpeed <= 0) {
            return baseTickSpeed;
        }

        if (this.randomTickScale >= 0.999D) {
            return baseTickSpeed;
        }

        int scaled = (int)Math.round(baseTickSpeed * this.randomTickScale);
        if (scaled <= 0 && this.randomTickScale > 0.0D) {
            scaled = 1;
        }

        final int suppressed = Math.max(0, baseTickSpeed - scaled);
        if (suppressed > 0) {
            this.suppressedRandomTicks += suppressed;
        }
        return scaled;
    }

    public @Nullable ThrottleCategory evaluateMob(final Entity entity) {
        if (!TickThrottleModule.ENABLED || !(entity instanceof Mob mob)) {
            return null;
        }

        final MobCategory category = mob.getType().getCategory();
        if (category == MobCategory.MONSTER) {
            if (!this.hostileAllowedThisTick && this.hostileInterval > 1) {
                ++this.suppressedHostileTicks;
                return ThrottleCategory.HOSTILE;
            }
        } else {
            if (!this.passiveAllowedThisTick && this.passiveInterval > 1) {
                ++this.suppressedPassiveTicks;
                return ThrottleCategory.PASSIVE;
            }
        }
        return null;
    }

    public TickThrottleMetrics snapshotMetrics() {
        return new TickThrottleMetrics(
                TickThrottleModule.ENABLED,
                this.currentTps,
                this.randomTickScale,
                this.passiveInterval,
                this.passiveAllowedThisTick,
                this.hostileInterval,
                this.hostileAllowedThisTick,
                this.suppressedRandomTicks,
                this.suppressedPassiveTicks,
                this.suppressedHostileTicks
        );
    }

    public enum ThrottleCategory {
        PASSIVE,
        HOSTILE
    }

    public record TickThrottleMetrics(boolean enabled, double currentTps, double randomTickScale, int passiveInterval,
                                      boolean passiveTickingThisRound, int hostileInterval,
                                      boolean hostileTickingThisRound, long randomTicksSuppressed,
                                      long passiveTicksSuppressed, long hostileTicksSuppressed) {
    }
}
