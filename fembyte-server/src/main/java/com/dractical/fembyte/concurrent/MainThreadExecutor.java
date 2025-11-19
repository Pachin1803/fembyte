package com.dractical.fembyte.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface MainThreadExecutor {

    boolean isOnMainThread();

    void execute(Runnable task);

    default <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        execute(() -> {
            try {
                T value = supplier.get();
                future.complete(value);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
