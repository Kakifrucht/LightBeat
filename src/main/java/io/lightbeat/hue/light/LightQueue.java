package io.lightbeat.hue.light;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.lightbeat.hue.bridge.HueManager;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Sends light updates in a synchronized queue, while waiting for callbacks from the bridge
 * and logging received errors.
 */
public class LightQueue {

    private static final Logger logger = LoggerFactory.getLogger(LightQueue.class);

    private final HueManager hueManager;
    private final Queue<QueueEntry> queue;

    private final PHLightListener lightListener = new PHLightListener() {

        @Override
        public void onSuccess() {
            next();
        }

        @Override
        public void onError(int code, String message) {
            logger.warn("Error ocurred during update of light {}, code {} - {}", currentWork.light.getName(), code, message);
            next();
        }

        @Override
        public void onReceivingLightDetails(PHLight phLight) {}
        @Override
        public void onReceivingLights(List<PHBridgeResource> list) {}
        @Override
        public void onSearchComplete() {}
        @Override
        public void onStateUpdate(Map<String, String> map, List<PHHueError> list) {}
    };

    private transient QueueEntry currentWork;
    private transient boolean shutdownWasMarked = false;


    public LightQueue(HueManager hueManager) {
        this.hueManager = hueManager;
        queue = new ConcurrentLinkedQueue<>();
    }

    public void addUpdate(PHLight light, PHLightState state) {
        queue.add(new QueueEntry(light, state));
        workQueue();
    }

    /**
     * Will shutdown SDK if or once the queue is empty.
     */
    public void markShutdown() {
        shutdownWasMarked = true;
        if (currentWork == null) {
            PHHueSDK.getStoredSDKObject().destroySDK();
        }
    }

    private void workQueue() {
        if (currentWork == null) {
            next();
        }
    }

    private void next() {
        if (queue.isEmpty()) {
            currentWork = null;
            if (shutdownWasMarked) {
                PHHueSDK.getStoredSDKObject().destroySDK();
            }
        } else {
            currentWork = queue.poll();
            currentWork.doUpdate();
        }
    }

    private class QueueEntry {
        private final PHLight light;
        private final PHLightState state;

        QueueEntry(PHLight light, PHLightState state) {
            this.light = light;
            this.state = state;
        }

        void doUpdate() {
            if (hueManager.isConnected()) {
                hueManager.getBridge().updateLightState(light, state, lightListener);
            } else {
                logger.warn("Purged {} light updates", queue.size() + 1);
                queue.clear();
            }
        }
    }
}
