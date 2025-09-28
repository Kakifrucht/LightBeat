package pw.wunderlich.lightbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Orchestrates application-wide task execution using a combination of a scheduled executor
 * for timed tasks and a virtual thread executor for lightweight, concurrent task handling.
 * <p>
 * Designed to efficiently manage I/O-bound operations, particularly those involving communication
 * with external devices like Hue bridges, while preventing resource exhaustion through controlled
 * concurrency limits.
 * <p>
 * Provides methods to dispatch immediate tasks, schedule delayed or periodic tasks, and ensures
 * graceful shutdown of executors.
 */
public class AppTaskOrchestrator implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AppTaskOrchestrator.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService workerExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Limit was chosen as a trade-off to be able to handle high amounts of light to not block
     * during bridge communication i/o and to not overwhelm the bridge with too many commands.
     */
    private static final int BRIDGE_CONCURRENCY_LIMIT = 8;
    private final Semaphore bridgeAccessLimiter = new Semaphore(BRIDGE_CONCURRENCY_LIMIT);


    /**
     * Submits a task to be executed on a virtual thread.
     * Thread count will be limited by a semaphore and the set {@link #BRIDGE_CONCURRENCY_LIMIT}.
     * To be used by threads that access the bridge.
     */
    public void dispatchBridgeCommand(Runnable bridgeTask) {
        workerExecutor.submit(() -> {
            try {
                bridgeAccessLimiter.acquire();
                bridgeTask.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                bridgeAccessLimiter.release();
            }
        });
    }

    /**
     * Submits a task to be executed on a virtual thread.
     * This bypasses the bridge-specific semaphore and is suitable for general-purpose
     * background tasks that don't need throttling.
     *
     * @param task the task to execute.
     * @return a Future representing pending completion of the task.
     */
    public Future<?> dispatch(Runnable task) {
        return workerExecutor.submit(task);
    }

    /**
     * Schedules a one-shot task to execute on a virtual thread after a given delay.
     *
     * @param task  the task to execute.
     * @param delay the time from now to delay execution.
     * @param unit  the time unit of the delay parameter.
     * @return a ScheduledFuture representing pending completion of the task, which can be used to cancel it.
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduler.schedule(() -> workerExecutor.submit(task), delay, unit);
    }

    /**
     * Schedules a periodic task to execute on a virtual thread after a given initial delay.
     *
     * @param task         the task to execute.
     * @param initialDelay the time to delay first execution.
     * @param period       the period between successive executions.
     * @param unit         the time unit of the initialDelay and period parameters.
     * @return a ScheduledFuture representing pending completion, which can be used to cancel the periodic execution.
     */
    public ScheduledFuture<?> schedulePeriodicTask(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(
                () -> workerExecutor.submit(task),
                initialDelay,
                period,
                unit
        );
    }

    /**
     * Gracefully terminates both executors.
     */
    public void shutdown() {
        logger.info("Attempting graceful shutdown of executors...");

        workerExecutor.shutdown();
        scheduler.shutdown();

        try {
            final var TIMEOUT = 5;
            boolean workerTerminated = workerExecutor.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
            boolean schedulerTerminated = scheduler.awaitTermination(TIMEOUT, TimeUnit.SECONDS);

            if (workerTerminated && schedulerTerminated) {
                logger.info("All tasks completed gracefully.");
            } else {
                logger.warn("Graceful shutdown timed out. Forcing shutdown of remaining tasks...");
                forceShutdown();
            }
        } catch (InterruptedException e) {
            logger.warn("Shutdown await was interrupted. Forcing shutdown...");
            forceShutdown();
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShutdown() {
        return workerExecutor.isShutdown() || scheduler.isShutdown();
    }

    private void forceShutdown() {
        workerExecutor.shutdownNow();
        scheduler.shutdownNow();
    }

    @Override
    public void close() {
        shutdown();
    }
}
