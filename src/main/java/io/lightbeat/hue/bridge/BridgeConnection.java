package io.lightbeat.hue.bridge;

import io.github.zeroone3010.yahueapi.Hue;
import io.github.zeroone3010.yahueapi.Light;
import io.github.zeroone3010.yahueapi.LightType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Class handling a connection to a bridge.
 * The constructor checks if given accesspoint is a bridge and will either initialize pushlinking
 * or start a recurring task to check if the connection is still alive.
 * Bridge API calls are handled asynchronously, callbacks are given through a listener
 * implementing the {@link ConnectionListener} interface.
 *
 * Bridge state is cached, which allows for state non-blocking state retrieval on {@link Light} objects.
 */
public class BridgeConnection {

    private static final Logger logger = LoggerFactory.getLogger(BridgeConnection.class);
    private static final String APP_NAME = "LightBeat";
    private static final int CONNECTION_CHECK_SECONDS = 10;

    private final ScheduledExecutorService executorService;
    private final ConnectionListener connectionListener;

    private Hue hue;

    private final ScheduledFuture<?> preconnectPushlinkTask;
    private ScheduledFuture<?> heartbeatTask;
    private boolean isConnected = false;


    public BridgeConnection(AccessPoint accessPoint, ScheduledExecutorService executorService, ConnectionListener listener) {

        this.executorService = executorService;
        this.connectionListener = listener;

        // check if is bridge
        Future<Boolean> isBridgeFuture = Hue.hueBridgeConnectionBuilder(accessPoint.getIp()).isHueBridgeEndpoint();
        preconnectPushlinkTask = executorService.schedule(() -> {
            try {
                boolean isBridge = isBridgeFuture.get(5, TimeUnit.SECONDS);
                if (isBridge) {

                    if (accessPoint.hasKey()) {
                        scheduleHeartbeat(accessPoint);
                        return;
                    }

                } else {
                    logger.info("Endpoint at {} is not a hue bridge", accessPoint.getIp());
                    connectionListener.connectionError(ConnectionListener.Error.NOT_A_BRIDGE);
                    return;
                }
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                logger.warn("Exception during check if endpoint is hue bridge", e);
                connectionListener.connectionError(ConnectionListener.Error.EXCEPTION);
                return;
            }

            // start pushlink
            connectionListener.pushlinkRequired();
            Future<String> pushlinkFuture = Hue.hueBridgeConnectionBuilder(accessPoint.getIp())
                    .initializeApiConnection(APP_NAME);

            try {
                String key = pushlinkFuture.get();
                if (pushlinkFuture.isCancelled()) {
                    logger.info("Pushlinking failed");
                }
                scheduleHeartbeat(new AccessPoint(accessPoint.getIp(), key));
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                if (e.getMessage().contains("link button not pressed")) {
                    logger.warn("Pushlinking failed");
                } else {
                    logger.warn("Pushlinking failed", e);
                }
                connectionListener.pushlinkFailed();
            }

        }, 0, TimeUnit.SECONDS);
    }

    private void scheduleHeartbeat(AccessPoint accessPoint) {

        this.hue = new Hue(accessPoint.getIp(), accessPoint.getKey());
        hue.setCaching(true);

        heartbeatTask = executorService.scheduleAtFixedRate(() -> {
            try {
                hue.refresh();
            } catch (Exception e) {
                if (isConnected) {
                    connectionListener.connectionError(ConnectionListener.Error.CONNECTION_LOST);
                } else {
                    connectionListener.connectionError(ConnectionListener.Error.EXCEPTION);
                }

                heartbeatTask.cancel(false);
                return;
            }

            if (!isConnected) {
                isConnected = true;
                if (getLights().isEmpty()) {
                    connectionListener.connectionError(ConnectionListener.Error.NO_LIGHTS);
                } else {
                    connectionListener.connectionSuccess(accessPoint.getKey());
                }
            }
        }, 0, CONNECTION_CHECK_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * @return list containing all valid lights with brightness controls.
     */
    public List<Light> getLights() {
        if (!isConnected) {
            throw new IllegalStateException("Not connected to bridge");
        }
        return hue.getAllLights().getLights()
                .stream()
                .filter(light -> !light.getType().equals(LightType.ON_OFF_PLUGIN_UNIT) && !light.getType().equals(LightType.ON_OFF_LIGHT))
                .sorted((light1, light2) -> light2.getId().compareTo(light1.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Refreshes bridge light state in cache.
     */
    void refresh() {
        hue.refresh();
    }

    /**
     * Disconnect from bridge by stopping any active threads. This will not trigger a call
     * through the {@link ConnectionListener} interface given via the constructor.
     */
    void disconnect() {
        if (preconnectPushlinkTask != null && !preconnectPushlinkTask.isDone()) {
            preconnectPushlinkTask.cancel(true);
        }
        if (isConnected) {
            heartbeatTask.cancel(true);
        }
    }

    /**
     * Implementing class listens for connection state changes.
     */
    public interface ConnectionListener {

        /**
         * @param key key/username to connect to given bridge
         */
        void connectionSuccess(String key);

        void connectionError(Error error);

        void pushlinkRequired();

        void pushlinkFailed();

        enum Error {
            CONNECTION_LOST,
            EXCEPTION,
            NOT_A_BRIDGE,
            NO_LIGHTS
        }
    }
}
