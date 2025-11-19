package com.dractical.fembyte.config.modules.performance;

import com.dractical.fembyte.config.ConfigCategory;
import com.dractical.fembyte.config.ConfigModule;

public class TickIntervalsModule extends ConfigModule {

    public static boolean ENABLED = false;
    public static int COLLISION_INTERVAL = 3;

    private static String path() {
        return ConfigCategory.PERFORMANCE.getBaseKeyName() + ".tick-interval.";
    }

    @Override
    public void onLoaded() {
        ENABLED = config.getBoolean(
                path() + "enabled",
                false,
                ""
        );

        COLLISION_INTERVAL = config.getInt(
                path() + "collision",
                5,
                """
                        Ticks between checking collisions.
                        """
        );
    }
}
