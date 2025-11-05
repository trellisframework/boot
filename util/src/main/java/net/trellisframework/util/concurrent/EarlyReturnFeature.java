package net.trellisframework.util.concurrent;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EarlyReturnFeature {
    private static final ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    private EarlyReturnFeature() {
    }

    public static CompletableFuture<Void> runAsync(Runnable task, Duration timeout, Consumer<Void> onCompleted) {
        return supplyAsync(() -> {
            task.run();
            return null;
        }, timeout, () -> null, onCompleted);
    }

    public static CompletableFuture<Void> runAsync(Runnable task, Duration timeout, Consumer<Void> onCompleted, int threads) {
        return supplyAsync(() -> {
            task.run();
            return null;
        }, timeout, () -> null, onCompleted, threads);
    }

    public static CompletableFuture<Void> runAsync(Runnable task, Duration timeout, Consumer<Void> onCompleted, Executor executor) {
        return supplyAsync(() -> {
            task.run();
            return null;
        }, timeout, () -> null, onCompleted, executor);
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> task, Duration timeout, Supplier<T> onTimeout, Consumer<T> onCompleted) {
        return supplyAsync(task, timeout, onTimeout, onCompleted, Executors.newVirtualThreadPerTaskExecutor());
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> task, Duration timeout, Supplier<T> onTimeout, Consumer<T> onCompleted, int threads) {
        return supplyAsync(task, timeout, onTimeout, onCompleted, Executors.newFixedThreadPool(threads));
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> task, Duration timeout, Supplier<T> onTimeout, Consumer<T> onCompleted, Executor executor) {
        return core(task, timeout, onTimeout, onCompleted, executor);
    }

    private static <T> CompletableFuture<T> core(Supplier<T> task, Duration timeout, Supplier<T> onTimeout, Consumer<T> onCompleted, Executor executor) {
        CompletableFuture<T> taskFuture = CompletableFuture.supplyAsync(task, executor);
        CompletableFuture<T> early = new CompletableFuture<>();
        ScheduledFuture<?> timer = scheduled.schedule(() -> {
            try {
                early.complete(onTimeout.get());
            } catch (Throwable t) {
                early.completeExceptionally(t instanceof RuntimeException ? t : new RuntimeException(t));
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        taskFuture.whenCompleteAsync((res, err) -> {
            try {
                if (err != null) {
                    if (!early.isDone()) early.completeExceptionally(err instanceof RuntimeException ? err : new RuntimeException(err));
                } else if (!early.isDone()) {
                    early.complete(res);
                }
            } finally {
                if (err == null && onCompleted != null) {
                    onCompleted.accept(res);
                }
            }
        }, executor);
        early.whenComplete((r, e) -> timer.cancel(false));
        return early;
    }
}
