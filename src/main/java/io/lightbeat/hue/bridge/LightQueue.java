package io.lightbeat.hue.bridge;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.light.Light;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends light updates in a synchronized queue, while waiting for callbacks from the bridge
 * and logging received errors. Every light has it's own internal queue.
 */
public class LightQueue {

    private static final Logger logger = LoggerFactory.getLogger(LightQueue.class);

    private final HueManager hueManager;
    private final Map<Light, Queue<QueueEntry>> queue;

    private volatile boolean shutdownWasMarked = false;


    LightQueue(HueManager hueManager) {
        this.hueManager = hueManager;
        queue = new ConcurrentHashMap<>();
    }

    public void addUpdate(Light light, PHLightState state) {

        if (light == null || state == null) {
            throw new IllegalArgumentException("Light and state cannot be null");
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
     * Will shutdown SDK threads if or once the queue is empty.
     */
    void markShutdown() {
        synchronized (queue) {
            shutdownWasMarked = true;
            if (queue.isEmpty()) {
                PHHueSDK.getStoredSDKObject().destroySDK();
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
                PHHueSDK.getStoredSDKObject().destroySDK();
            }
        }
    }

    private class QueueEntry {
        private final Light light;
        private final PHLightState state;
        private final LBLightListener lightListener;

        QueueEntry(Light light, PHLightState state) {
            this.light = light;
            this.state = state;
            this.lightListener = new LBLightListener(this);
        }

        void doUpdate() {
            if (hueManager.isConnected()) {
                hueManager.getBridge().updateLightState(light.getBase(), state, lightListener);
            } else {
                logger.warn("Purged {} light updates", queue.size() + 1);
                queue.clear();
            }
        }
    }

    private class LBLightListener implements PHLightListener {

        private final QueueEntry work;


        LBLightListener(QueueEntry work) {
            this.work = work;
        }

        @Override
        public void onSuccess() {
            logger.info("Updated light {}", getLightInfo());
            next(work.light);
        }

        @Override
        public void onError(int code, String message) {
            logger.warn("Error ocurred during update of light {}, code {} - {}", getLightInfo(), code, message);
            work.light.recoverFromError(code);
            next(work.light);
        }

        @Override
        public void onReceivingLightDetails(PHLight phLight) {}
        @Override
        public void onReceivingLights(List<PHBridgeResource> list) {}
        @Override
        public void onSearchComplete() {}
        @Override
        public void onStateUpdate(Map<String, String> map, List<PHHueError> list) {}

        private String getLightInfo() {

            PHLightState newState = work.state;

            String mode = "null";
            if (newState.getAlertMode() != null && !newState.getAlertMode().equals(PHLight.PHLightAlertMode.ALERT_UNKNOWN)) {
                mode = newState.getAlertMode().toString();
            }

            return work.light.getBase().getName()
                    + " (time " + newState.getTransitionTime()
                    + " | bri " + newState.getBrightness()
                    + " | color " + newState.getHue() + "/" + newState.getSaturation()
                    + " | mode " + mode
                    + " | on " + newState.isOn() + ")";
        }
    }
}
