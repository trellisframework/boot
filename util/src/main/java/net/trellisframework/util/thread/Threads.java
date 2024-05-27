package net.trellisframework.util.thread;

import lombok.SneakyThrows;
import net.trellisframework.core.log.Logger;
import net.trellisframework.util.function.ISupplier;

import java.util.Random;

public class Threads {

    public static void sleepAndRun(int origin, int bound, Runnable runnable) {
        sleep(origin, bound);
        runnable.run();
    }

    public static void sleepAndRun(int origin, Runnable runnable) {
        sleep(origin);
        runnable.run();
    }

    public static void sleep(int origin, int bound) {
        if (origin <= bound && bound > 0)
            sleep((origin == bound) ? origin : new Random().nextInt(origin, bound));
    }

    public static void sleep(int millisecond) {
        try {
            Thread.sleep(millisecond);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.error("Threads", "Thread interrupted", e);
        }
    }

    @SneakyThrows
    public static void sleepUntil(int millisecond, ISupplier<Boolean> condition) {
        while (condition.get()) {
            Threads.sleep(millisecond);
        }
    }

    @SneakyThrows
    public static void sleepUntil(int origin, int bound, ISupplier<Boolean> condition) {
        while (condition.get()) {
            Threads.sleep(origin, bound);
        }
    }
}
