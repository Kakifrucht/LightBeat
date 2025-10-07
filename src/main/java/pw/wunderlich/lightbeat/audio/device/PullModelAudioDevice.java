package pw.wunderlich.lightbeat.audio.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.MediaLocator;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import java.util.concurrent.Executor;

/**
 * An AudioDevice implementation that uses the JMF "pull" model.
 * It extends the BaseJmfAudioDevice to inherit the common JMF lifecycle management.
 */
public class PullModelAudioDevice extends BaseJmfAudioDevice {

    private static final Logger logger = LoggerFactory.getLogger(PullModelAudioDevice.class);

    private final Executor executor;
    private PullBufferStream stream;
    private volatile boolean isRunning = false;


    public PullModelAudioDevice(Executor executor, MediaLocator mediaLocator, String deviceName) {
        super(mediaLocator, deviceName);
        this.executor = executor;
    }

    @Override
    protected void startReading() {
        if (!(dataSource instanceof PullBufferDataSource pullDataSource)) {
            throw new IllegalStateException("DataSource is not a PullBufferDataSource");
        }
        this.stream = pullDataSource.getStreams()[0];
        this.isRunning = true;
        scheduleRead();
    }

    @Override
    protected void stopReading() {
        this.isRunning = false;
    }

    @Override
    public boolean isOpen() {
        return super.isOpen() && stream != null;
    }

    private void scheduleRead() {
        if (isRunning) {
            executor.execute(this::performRead);
        }
    }

    private void performRead() {
        if (!isRunning) {
            return;
        }

        try {
            stream.read(jmfBuffer);
            notifyListener();
            scheduleRead();
        } catch (Exception e) {
            if (isRunning) {
                logger.warn("Error reading from PulseAudio stream for device '{}'. Stopping device.", getName(), e);
                stopReading();
            }
        }
    }
}
