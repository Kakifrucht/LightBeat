package io.lightbeat.hue.light;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.bridge.HueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
            logger.info("Updated light {}", getLightInfo());
            next();
        }

        @Override
        public void onError(int code, String message) {
            logger.warn("Error ocurred during update of light {}, code {} - {}", getLightInfo(), code, message);
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

        private String getLightInfo() {

            PHLightState newState = currentWork.state;

            String mode = "null";
            if (newState.getAlertMode() != null && !newState.getAlertMode().equals(PHLight.PHLightAlertMode.ALERT_UNKNOWN)) {
                mode = newState.getAlertMode().toString();
            }

            return currentWork.light.getName() + " (bri " + newState.getBrightness() + " | color "
                    + newState.getHue() + "/" + newState.getSaturation()
                    + " | mode " + mode + " | on " + newState.isOn() + ")";
        }
    };

    private volatile QueueEntry currentWork;
    private volatile boolean shutdownWasMarked = false;


    public LightQueue(HueManager hueManager) {
        this.hueManager = hueManager;
        queue = new LinkedList<>();
    }

    public void addUpdate(PHLight light, PHLightState state) {
        synchronized (queue) {
            queue.add(new QueueEntry(light, state));
            if (currentWork == null) {
                next();
            }
        }
    }

    /**
     * Will shutdown SDK if or once the queue is empty.
     */
    public void markShutdown() {
        synchronized (queue) {
            shutdownWasMarked = true;
            if (currentWork == null) {
                PHHueSDK.getStoredSDKObject().destroySDK();
            }
        }
    }

    private void next() {
        synchronized (queue) {
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
