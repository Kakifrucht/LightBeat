package pw.wunderlich.lightbeat.hue.bridge.light;

import io.github.zeroone3010.yahueapi.AlertType;
import io.github.zeroone3010.yahueapi.State;
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
public class UpdateQueue {

    private static final Logger logger = LoggerFactory.getLogger(UpdateQueue.class);

    private final ScheduledExecutorService executorService;
    private final Map<Light, Queue<QueueEntry>> queue;


    public UpdateQueue(ScheduledExecutorService executorService) {
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

    private void next(Light light) {
        synchronized (queue) {
            Queue<QueueEntry> lightQueue = queue.get(light);
            if (!lightQueue.isEmpty()) {
                QueueEntry next = lightQueue.poll();
                if (executorService.isShutdown()) {
                    next.doUpdate();
                } else {
                    executorService.schedule(next::doUpdate, 0, TimeUnit.SECONDS);
                }
            } else {
                queue.remove(light);
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
            light.getBase().setState(state);
            logger.info("Updated light {}", getLightInfo());
            next(light);
        }

        private String getLightInfo() {

            State newState = state;

            String mode = "null";
            if (newState.getAlert() != null && !newState.getAlert().equals(AlertType.NONE)) {
                mode = newState.getAlert().toString();
            }

            String color = "null";
            if (newState.getHue() != null) {
                color = "hue/sat " + newState.getHue() + "/" + newState.getSat();
            } else if (newState.getXy() != null) {
                color = "x/y " + newState.getXy().get(0) + "/" + newState.getXy().get(1);
            } else if (newState.getCt() != null) {
                color = "ct " + newState.getCt();
            }

            return light.getBase().getName()
                    + " (time " + newState.getTransitiontime()
                    + " | bri " + newState.getBri()
                    + " | color " + color
                    + " | mode " + mode
                    + " | on " + newState.getOn() + ")";
        }
    }
}
