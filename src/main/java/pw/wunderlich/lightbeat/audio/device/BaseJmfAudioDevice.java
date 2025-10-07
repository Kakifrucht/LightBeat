package pw.wunderlich.lightbeat.audio.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.Buffer;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.format.AudioFormat;
import javax.media.protocol.*;

/**
 * An abstract base class for JMF-based audio devices. This class handles the common
 * device lifecycle (creation, connection, start, stop, disconnect) and leaves the
 * specific data acquisition strategy (push vs. pull) to subclasses.
 */
public abstract class BaseJmfAudioDevice implements AudioDevice {

    private static final Logger logger = LoggerFactory.getLogger(BaseJmfAudioDevice.class);

    protected final MediaLocator mediaLocator;
    protected final String deviceName;
    protected AudioDataListener listener;
    protected LBAudioFormat format;
    protected DataSource dataSource;
    protected final Buffer jmfBuffer = new Buffer();

    public BaseJmfAudioDevice(MediaLocator mediaLocator, String deviceName) {
        this.mediaLocator = mediaLocator;
        this.deviceName = deviceName;
    }

    @Override
    public String getName() {
        return this.deviceName;
    }

    @Override
    public LBAudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public void setAudioListener(AudioDataListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isOpen() {
        return dataSource != null;
    }

    @Override
    public final boolean start() {
        if (isOpen()) {
            return false;
        }
        try {
            dataSource = Manager.createDataSource(mediaLocator);
            if (dataSource instanceof PushBufferDataSource pushDataSource) {
                PushBufferStream stream = pushDataSource.getStreams()[0];
                this.format = new LBAudioFormat((AudioFormat) stream.getFormat());
            } else if (dataSource instanceof PullBufferDataSource pullDataSource) {
                PullBufferStream stream = pullDataSource.getStreams()[0];
                this.format = new LBAudioFormat((AudioFormat) stream.getFormat());
            } else {
                throw new IllegalStateException("DataSource is neither Push nor Pull BufferDataSource: " + dataSource.getClass().getName());
            }

            dataSource.connect();
            dataSource.start();

            startReading();

            logger.info("Started JMF device: {}", getName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to start JMF device: {}", getName(), e);
            if (dataSource != null) {
                try { dataSource.stop(); } catch (Exception ignored) {}
                try { dataSource.disconnect(); } catch (Exception ignored) {}
            }
            dataSource = null;
            return false;
        }
    }

    protected abstract void startReading();
    protected abstract void stopReading();

    protected void notifyListener() {
        if (listener != null) {
            byte[] data = (byte[]) jmfBuffer.getData();
            int offset = jmfBuffer.getOffset();
            int length = jmfBuffer.getLength();

            if (length > 0) {
                byte[] dataCopy = new byte[length];
                System.arraycopy(data, offset, dataCopy, 0, length);
                listener.onDataAvailable(dataCopy, length);
            }
        }
    }

    @Override
    public final boolean stop() {
        if (!isOpen()) {
            return false;
        }
        try {
            stopReading();
            dataSource.stop();
            dataSource.disconnect();
            logger.info("Stopped JMF device: {}", getName());
        } catch (Exception e) {
            logger.warn("Error while stopping JMF device '{}'", getName(), e);
        } finally {
            dataSource = null;
        }
        return true;
    }
}
