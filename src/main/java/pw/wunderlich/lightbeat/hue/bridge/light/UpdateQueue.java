package pw.wunderlich.lightbeat.hue.bridge.light;

import io.github.zeroone3010.yahueapi.AlertType;
import io.github.zeroone3010.yahueapi.Light;
import io.github.zeroone3010.yahueapi.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;
import pw.wunderlich.lightbeat.util.TimeThreshold;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Sends light updates in a synchronized queue, while waiting for callbacks from the bridge
 * and logging received errors. Every light has its own UpdateQueue instance.
 * <br>
 * Will discard updates that are older than {@link #STALE_THRESHOLD_MS}. The bridge itself
 * does not reply when an update has successfully propagated through the ZigBee network and
 * instead only confirms the acceptance of the update. Calling {@link #addUpdate(State, boolean)}
 * with {@code isEssential = true} will ensure the update will be sent.
 */
public class UpdateQueue {

    private static final Logger logger = LoggerFactory.getLogger(UpdateQueue.class);

    private static final long STALE_THRESHOLD_MS = 250;

    private final Light apiLight;
    private final AppTaskOrchestrator taskOrchestrator;

    private final Queue<QueueEntry> queue;


    public UpdateQueue(Light apiLight, AppTaskOrchestrator taskOrchestrator) {
        this.apiLight = apiLight;
        this.taskOrchestrator = taskOrchestrator;
        this.queue = new LinkedList<>();
    }

    public void addUpdate(State state, boolean isEssential) {
        if (state == null) {
            return;
        }
        synchronized (queue) {
            boolean wasEmpty = queue.isEmpty();
            queue.add(new QueueEntry(state, isEssential));
            if (wasEmpty) {
                next();
            }
        }
    }

    private void next() {
        synchronized (queue) {
            if (queue.isEmpty()) {
                return;
            }

            QueueEntry next = queue.peek();
            if (taskOrchestrator.isShutdown()) {
                next.doUpdate();
            } else {
                taskOrchestrator.dispatchBridgeCommand(next::doUpdate);
            }
        }
    }

    private class QueueEntry {
        private final State state;
        private final TimeThreshold creationTimestamp;
        private final boolean isEssential;

        QueueEntry(State state, boolean isEssential) {
            this.state = state;
            this.creationTimestamp = new TimeThreshold(STALE_THRESHOLD_MS);
            this.isEssential = isEssential;
        }

        void doUpdate() {
            if (!this.isEssential && creationTimestamp.isMet()) {
                long age = creationTimestamp.getCurrentThreshold() - STALE_THRESHOLD_MS;
                logger.warn("Discarding stale light update for {} (age: {}ms).", getLightInfo(), age);
            } else {
                apiLight.setState(state);
                logger.info("Updated light {}", getLightInfo());
            }

            synchronized (queue) {
                queue.poll();
                next();
            }
        }

        private String getLightInfo() {
            String mode = "null";
            if (state.getAlert() != null && !state.getAlert().equals(AlertType.NONE)) {
                mode = state.getAlert().toString();
            }

            String color = "null";
            if (state.getHue() != null) {
                color = "hue/sat " + state.getHue() + "/" + state.getSat();
            } else if (state.getXy() != null) {
                color = "x/y " + state.getXy().get(0) + "/" + state.getXy().get(1);
            } else if (state.getCt() != null) {
                color = "ct " + state.getCt();
            }

            return apiLight.getName()
                    + " (time " + state.getTransitiontime()
                    + " | bri " + state.getBri()
                    + " | color " + color
                    + " | mode " + mode
                    + " | on " + state.getOn() + ")";
        }
    }
}
