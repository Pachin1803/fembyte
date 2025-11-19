package com.dractical.fembyte.concurrent;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Async {

    private Async() {
    }

    public static CompletableFuture<Void> runCpu(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return CompletableFuture.runAsync(runnable, AsyncExecutors.cpuExecutor());
    }

    public static <T> CompletableFuture<T> supplyCpu(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return CompletableFuture.supplyAsync(supplier, AsyncExecutors.cpuExecutor());
    }

    public static CompletableFuture<Void> runVirtual(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return CompletableFuture.runAsync(runnable, AsyncExecutors.virtualExecutor());
    }

    public static <T> CompletableFuture<T> supplyVirtual(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return CompletableFuture.supplyAsync(supplier, AsyncExecutors.virtualExecutor());
    }

    public static ScheduledFuture<?> schedule(Runnable runnable, Duration delay) {
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(delay, "delay");
        long millis = delay.toMillis();
        return AsyncExecutors.scheduler().schedule(
                runnable,
                millis,
                TimeUnit.MILLISECONDS
        );
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(
            Runnable runnable,
            Duration initialDelay,
            Duration period
    ) {
        Objects.requireNonNull(runnable, "runnable");
        Objects.requireNonNull(initialDelay, "initialDelay");
        Objects.requireNonNull(period, "period");
        long initialMillis = initialDelay.toMillis();
        long periodMillis = period.toMillis();
        return AsyncExecutors.scheduler().scheduleAtFixedRate(
                runnable,
                initialMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public static void onMain(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        MainThreadExecutor main = AsyncExecutors.mainThreadExecutor();
        if (main.isOnMainThread()) {
            runnable.run();
        } else {
            main.execute(runnable);
        }
    }

    public static <T> CompletableFuture<T> supplyOnMain(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        MainThreadExecutor main = AsyncExecutors.mainThreadExecutor();
        if (main.isOnMainThread()) {
            try {
                return CompletableFuture.completedFuture(supplier.get());
            } catch (Throwable t) {
                CompletableFuture<T> failed = new CompletableFuture<>();
                failed.completeExceptionally(t);
                return failed;
            }
        } else {
            return main.supply(supplier);
        }
    }

    public static <T> CompletableFuture<Void> thenAcceptOnMain(
            CompletableFuture<T> future,
            Consumer<T> consumer
    ) {
        Objects.requireNonNull(future, "future");
        Objects.requireNonNull(consumer, "consumer");

        return future.thenCompose(result ->
                supplyOnMain(() -> {
                    consumer.accept(result);
                    return null;
                })
        );
    }

    public static <T, R> CompletableFuture<R> thenApplyOnMain(
            CompletableFuture<T> future,
            java.util.function.Function<T, R> fn
    ) {
        Objects.requireNonNull(future, "future");
        Objects.requireNonNull(fn, "fn");

        return future.thenCompose(result ->
                supplyOnMain(() -> fn.apply(result))
        );
    }

    public static <T> CompletableFuture<T> withTimeout(
            CompletableFuture<T> future,
            Duration timeout
    ) {
        Objects.requireNonNull(future, "future");
        Objects.requireNonNull(timeout, "timeout");

        CompletableFuture<T> timeoutFuture = new CompletableFuture<>();
        ScheduledFuture<?> scheduled = AsyncExecutors.scheduler().schedule(
                () -> timeoutFuture.completeExceptionally(
                        new TimeoutException("Operation timed out after " + timeout)
                ),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );

        future.whenComplete((value, throwable) -> {
            if (!timeoutFuture.isDone()) {
                if (throwable != null) {
                    timeoutFuture.completeExceptionally(throwable);
                } else {
                    timeoutFuture.complete(value);
                }
                scheduled.cancel(false);
            }
        });

        return timeoutFuture;
    }
}
