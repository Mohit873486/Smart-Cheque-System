package com.chequeprint.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AppExecutors — Centralized thread pool manager for asynchronous background tasks.
 * Reuses pooled daemon threads to prevent unmanaged thread creation overhead.
 */
public final class AppExecutors {

    private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "app-bg-worker-" + count.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    private AppExecutors() {
    }

    /**
     * Executes a task asynchronously on a managed background worker thread.
     *
     * @param task Background task to execute
     */
    public static void runAsync(Runnable task) {
        if (task != null) {
            BACKGROUND_EXECUTOR.submit(task);
        }
    }

    /**
     * Gets the shared background ExecutorService instance.
     *
     * @return Managed ExecutorService
     */
    public static ExecutorService getExecutor() {
        return BACKGROUND_EXECUTOR;
    }
}
