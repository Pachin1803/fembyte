package com.dractical.fembyte.config.modules.performance;

import com.dractical.fembyte.config.ConfigCategory;
import com.dractical.fembyte.config.ConfigModule;

public final class RandomTickModule extends ConfigModule {

    public static boolean ENABLED = false;
    public static Mode MODE = Mode.VANILLA;
    public static DensityDistribution DENSITY_DISTRIBUTION = DensityDistribution.SMOOTHED_ACCUMULATOR;
    public static double DENSITY_MAX_LAMBDA = 16.0D;
    public static int DENSITY_MAX_TICKS_PER_SECTION = 64;
    public static double DENSITY_POISSON_SLOW_PATH = 5.0D;

    private static String path() {
        return ConfigCategory.PERFORMANCE.getBaseKeyName() + ".random-tick.";
    }

    @Override
    public void onLoaded() {
        ENABLED = config.getBoolean(
                path() + "enabled",
                false,
                ""
        );

        MODE = config.getEnum(
                path() + "mode",
                Mode.VANILLA,
                """
                        What mode to use for random ticking.
                        - VANILLA = default behavior
                        - DENSITY = keeps average tick-rate per block close to vanilla,
                          but uses a probabilistic approximation that reduces CPU usage.
                          Per-tick distributions may be slightly different from vanilla.
                        """
        );

        DENSITY_DISTRIBUTION = config.getEnum(
                path() + "density.distribution",
                DensityDistribution.SMOOTHED_ACCUMULATOR,
                """
                        Selects the strategy used by the density-based ticker.
                        - POISSON samples a Poisson distribution for the number of ticks rolled.
                        - SMOOTHED_ACCUMULATOR keeps a running accumulator per-section to smooth rounding errors.
                        """
        );

        DENSITY_MAX_LAMBDA = config.getDouble(
                path() + "density.max-lambda",
                16.0D,
                """
                        Optional safety cap for the effective lambda (expected ticks per section per game tick).
                        Set to <= 0 to disable the cap.
                        """
        );

        DENSITY_MAX_TICKS_PER_SECTION = config.getInt(
                path() + "density.max-ticks-per-section",
                64,
                """
                        Hard limit on how many random ticks can run for a single section in one game tick.
                        Helps prevent extremely dense sections from consuming too much time at once.
                        """
        );

        DENSITY_POISSON_SLOW_PATH = config.getDouble(
                path() + "density.poisson-slow-path-threshold",
                5.0D,
                """
                        Threshold that decides when the Poisson sampler switches from the Knuth algorithm
                        to a Gaussian approximation. Lower values bias toward the approximation more often.
                        """
        );
    }

    public enum Mode {
        VANILLA,
        DENSITY,
    }

    public enum DensityDistribution {
        POISSON,
        SMOOTHED_ACCUMULATOR,
    }
}
