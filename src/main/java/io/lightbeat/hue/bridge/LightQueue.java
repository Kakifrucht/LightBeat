package io.lightbeat.hue.bridge;

import io.github.zeroone3010.yahueapi.AlertType;
import io.github.zeroone3010.yahueapi.State;
import io.lightbeat.hue.bridge.light.Light;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends light updates in a synchronized queue, while waiting for callbacks from the bridge
 * and logging received errors. Every light has its own internal queue.
 */
public class LightQueue {

    private static final Logger logger = LoggerFactory.getLogger(LightQueue.class);

    private final HueManager hueManager;
    private final ScheduledExecutorService executorService;
    private final Map<Light, Queue<QueueEntry>> queue;

    private volatile boolean shutdownWasMarked = false;


    LightQueue(HueManager hueManager, ScheduledExecutorService executorService) {
        this.hueManager = hueManager;
        this.executorService = executorService;
        queue = new ConcurrentHashMap<>();
    }

    public void addUpdate(Light light, State state) {

        if (state == null) {
            return;
        }

        if (light == null) {
            throw new IllegalArgumentException("Light cannot be null");
        }

        synchronized (queue) {

            if (shutdownWasMarked) {
                throw new IllegalStateException("Tried to add update for light "
                        + light.getBase().getName() + " when shutdown was already marked");
            }

            QueueEntry entry = new QueueEntry(light, state);
            if (queue.containsKey(light)) {
                queue.get(light).add(entry);
            } else {
                Queue<QueueEntry> lightQueue = new LinkedList<>();
                lightQueue.add(entry);
                queue.put(light, lightQueue);
                next(light);
            }
        }
    }

    /**
     * Will shut down SDK threads if or once the queue is empty.
     */
    void markShutdown() {
        synchronized (queue) {
            shutdownWasMarked = true;
            if (queue.isEmpty()) {
                hueManager.getBridge().disconnect();
            }
        }
    }

    private void next(Light light) {
        synchronized (queue) {
            Queue<QueueEntry> lightQueue = queue.get(light);
            if (!lightQueue.isEmpty()) {
                lightQueue.poll().doUpdate();
            } else {
                queue.remove(light);
            }

            if (shutdownWasMarked && queue.isEmpty()) {
                hueManager.getBridge().disconnect();
            }
        }
    }

    private class QueueEntry {
        private final Light light;
        private final State state;

        QueueEntry(Light light, State state) {
            this.light = light;
            this.state = state;
        }

        void doUpdate() {
            if (hueManager.isConnected()) {
                executorService.schedule(() -> {
                    light.getBase().setState(state);
                    logger.info("Updated light {}", getLightInfo());
                    next(light);
                }, 0, TimeUnit.SECONDS);

            } else {
                logger.warn("Purged {} light updates", queue.size() + 1);
                queue.clear();
            }
        }

        private String getLightInfo() {

            State newState = state;

            String mode = "null";
            if (newState.getAlert() != null && !newState.getAlert().equals(AlertType.NONE)) {
                mode = newState.getAlert().toString();
            }

            return light.getBase().getName()
                    + " (time " + newState.getTransitiontime()
                    + " | bri " + newState.getBri()
                    + " | color " + newState.getHue() + "/" + newState.getSat()
                    + " | mode " + mode
                    + " | on " + newState.getOn() + ")";
        }
    }
}
