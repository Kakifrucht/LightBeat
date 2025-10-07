package pw.wunderlich.lightbeat.audio.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.MediaLocator;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import java.util.concurrent.Executor;

/**
 * An AudioDevice implementation that uses the JMF "push" model.
 * It receives data passively via a BufferTransferHandler callback.
 */
public class PushModelAudioDevice extends BaseJmfAudioDevice {

    private static final Logger logger = LoggerFactory.getLogger(PushModelAudioDevice.class);

    private final Executor executor;
    private PushBufferStream stream;


    public PushModelAudioDevice(Executor executor, MediaLocator mediaLocator, String deviceName) {
        super(mediaLocator, deviceName);
        this.executor = executor;
    }

    @Override
    protected void startReading() {
        if (!(dataSource instanceof PushBufferDataSource pushDataSource)) {
            throw new IllegalStateException("DataSource is not a PushBufferDataSource");
        }
        this.stream = pushDataSource.getStreams()[0];

        BufferTransferHandler transferHandler = (pushBufferStream) -> executor.execute(() -> {
            if (listener == null || !isOpen()) return;
            try {
                pushBufferStream.read(jmfBuffer);
                notifyListener();
            } catch (Exception e) {
                logger.warn("Error reading from JMF capture stream for device '{}'", getName(), e);
            }
        });
        stream.setTransferHandler(transferHandler);
    }

    @Override
    protected void stopReading() {
        if (stream != null) {
            stream.setTransferHandler(null);
        }
    }

    @Override
    public boolean isOpen() {
        return super.isOpen() && stream != null;
    }
}
