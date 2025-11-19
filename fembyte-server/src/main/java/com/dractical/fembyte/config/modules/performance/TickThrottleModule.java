package com.dractical.fembyte.config.modules.performance;

import com.dractical.fembyte.config.ConfigCategory;
import com.dractical.fembyte.config.ConfigModule;

public class TickThrottleModule extends ConfigModule {

    public static boolean ENABLED = false;
    public static double RANDOM_ACTIVATION_TPS = 19.8D;
    public static double RANDOM_MIN_TPS = 17.0D;
    public static double RANDOM_MIN_SCALE = 0.25D;
    public static double PASSIVE_ACTIVATION_TPS = 19.5D;
    public static double PASSIVE_MIN_TPS = 16.5D;
    public static int PASSIVE_MAX_INTERVAL = 4;
    public static double HOSTILE_ACTIVATION_TPS = 19.0D;
    public static double HOSTILE_MIN_TPS = 15.5D;
    public static int HOSTILE_MAX_INTERVAL = 3;

    private static String path() {
        return ConfigCategory.PERFORMANCE.getBaseKeyName() + ".tick-throttle.";
    }

    @Override
    public void onLoaded() {
        ENABLED = config.getBoolean(
                path() + "enabled",
                false,
                ""
        );

        RANDOM_ACTIVATION_TPS = config.getDouble(
                path() + "random.activation-tps",
                19.8D,
                """
                        TPS threshold where random ticks begin scaling down.
                        """
        );

        RANDOM_MIN_TPS = config.getDouble(
                path() + "random.min-tps",
                17.0D,
                """
                        TPS where random ticks reach their minimum configured scale.
                        """
        );

        RANDOM_MIN_SCALE = config.getDouble(
                path() + "random.min-scale",
                0.25D,
                """
                        Fraction of the configured random tick speed that should still run
                        once TPS has fallen to the minimum threshold.
                        """
        );

        PASSIVE_ACTIVATION_TPS = config.getDouble(
                path() + "passive.activation-tps",
                19.5D,
                """
                        TPS threshold where peaceful mob AI begins skipping ticks.
                        """
        );

        PASSIVE_MIN_TPS = config.getDouble(
                path() + "passive.min-tps",
                16.5D,
                """
                        TPS where peaceful mob throttling reaches the maximum skip interval.
                        """
        );

        PASSIVE_MAX_INTERVAL = config.getInt(
                path() + "passive.max-interval",
                4,
                """
                        Maximum interval (in ticks) between full peaceful mob updates when throttling.
                        """
        );

        HOSTILE_ACTIVATION_TPS = config.getDouble(
                path() + "hostile.activation-tps",
                19.0D,
                """
                        TPS threshold where hostile mob AI begins skipping ticks.
                        """
        );

        HOSTILE_MIN_TPS = config.getDouble(
                path() + "hostile.min-tps",
                15.5D,
                """
                        TPS where hostile mob throttling reaches the maximum skip interval.
                        """
        );

        HOSTILE_MAX_INTERVAL = config.getInt(
                path() + "hostile.max-interval",
                3,
                """
                        Maximum interval (in ticks) between hostile mob updates when throttling.
                        """
        );
    }
}
