package com.dractical.fembyte.concurrent;

import net.minecraft.server.dedicated.DedicatedServer;

public final class FembyteMainThreadBootstrap {

    private FembyteMainThreadBootstrap() {
    }

    public static void installForDedicatedServer(DedicatedServer server) {
        AsyncExecutors.setMainThreadExecutor(new MainThreadExecutor() {
            @Override
            public boolean isOnMainThread() {
                return server.isSameThread();
            }

            @Override
            public void execute(Runnable task) {
                server.scheduleOnMain(task);
            }
        });
    }
}
