package io.lightbeat.gui.frame;

import com.philips.lighting.model.PHLight;
import io.lightbeat.LightBeat;
import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEvent;
import io.lightbeat.audio.BeatObserver;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.gui.swing.JConfigSlider;
import io.lightbeat.gui.swing.JIconLabel;
import io.lightbeat.util.URLConnectionReader;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Main application frame. Interface to set the applications settings and start the magic.
 */
public class MainFrame extends AbstractFrame implements BeatObserver {

    private final AudioReader audioReader;

    private JPanel mainPanel;

    private JIconLabel bannerLabel;

    private JComboBox<String> deviceSelectComboBox;

    private JButton restoreBrightnessButton;
    private JConfigSlider minBrightnessSlider;
    private JConfigSlider maxBrightnessSlider;
    private JConfigSlider sensitivitySlider;

    private JPanel lightsPanel;

    private JPanel advancedPanel;
    private JButton restoreAdvancedButton;

    private JButton startButton;
    private JCheckBox showAdvancedCheckbox;

    private JLabel urlLabel;
    private JLabel infoLabel;
    private JRadioButton randomRadioButton;
    private JConfigSlider beatSensitivitySlider;
    private JConfigSlider beatTimeBetweenSlider;
    private JRadioButton comingSoonRadioButton;
    private JConfigSlider transitionTimeSlider;

    private boolean isRunning = false;


    public MainFrame(int x, int y) {
        super(x, y);

        audioReader = componentHolder.getAudioReader();

        // add mixer names to dropdown
        List<String> mixerNames = audioReader.getSupportedMixers().stream()
                .map(mixer -> mixer.getMixerInfo().getName())
                .collect(Collectors.toList());

        String lastSource = config.get(ConfigNode.LAST_AUDIO_SOURCE);
        if (lastSource != null) {
            for (String mixerName : mixerNames) {
                if (mixerName.equals(lastSource)) {
                    deviceSelectComboBox.addItem(mixerName);
                    break;
                }
            }
        }

        for (String mixerName : mixerNames) {
            if (!mixerName.equals(lastSource)) {
                deviceSelectComboBox.addItem(mixerName);
            }
        }

        restoreBrightnessButton.addActionListener(e -> {
            minBrightnessSlider.restoreDefault();
            maxBrightnessSlider.restoreDefault();
            sensitivitySlider.restoreDefault();
        });
        minBrightnessSlider.setBoundedSlider(maxBrightnessSlider, true, 10);
        maxBrightnessSlider.setBoundedSlider(minBrightnessSlider, false, 10);

        // setup lights disable panel
        List<PHLight> allLights = getHueManager().getAllLights();
        List<String> disabledLights = config.getStringList(ConfigNode.LIGHTS_DISABLED);
        for (PHLight light : allLights) {

            JCheckBox checkBox = new JCheckBox();
            checkBox.setText(light.getName());
            if (!disabledLights.contains(light.getUniqueId())) {
                checkBox.setSelected(true);
            }

            lightsPanel.add(checkBox);

            checkBox.addActionListener(e -> {

                List<String> disabledLightsList = config.getStringList(ConfigNode.LIGHTS_DISABLED);

                if (((JCheckBox) e.getSource()).isSelected()) {
                    disabledLightsList.remove(light.getUniqueId());
                } else {
                    disabledLightsList.add(light.getUniqueId());
                }

                config.putStringList(ConfigNode.LIGHTS_DISABLED, disabledLightsList);
            });
        }

        restoreAdvancedButton.addActionListener(e -> {
            beatSensitivitySlider.restoreDefault();
            beatTimeBetweenSlider.restoreDefault();
            transitionTimeSlider.restoreDefault();
        });

        startButton.addActionListener(e -> {

            if (!isRunning) { // start
                String selectedMixerName = deviceSelectComboBox.getItemAt(deviceSelectComboBox.getSelectedIndex());
                List<Mixer> supportedMixers = audioReader.getSupportedMixers();
                for (Mixer supportedMixer : supportedMixers) {
                    String mixerName = supportedMixer.getMixerInfo().getName();
                    if (mixerName.equals(selectedMixerName)) {
                        config.put(ConfigNode.LAST_AUDIO_SOURCE, mixerName);

                        isRunning = getHueManager().initializeLights();
                        if (isRunning) {
                            audioReader.start(supportedMixer);
                            startButton.setText("Stop");
                            infoLabel.setText("Running");
                            componentHolder.getAudioEventManager().registerBeatObserver(this);
                        } else {
                            infoLabel.setText("No lights were selected");
                        }

                        break;
                    }
                }

            } else { // stop
                startButton.setText("Start");
                infoLabel.setText("Idle");
                onWindowClose();
                isRunning = false;
            }
        });

        showAdvancedCheckbox.setSelected(config.getBoolean(ConfigNode.SHOW_ADVANCED_SETTINGS));
        showAdvancedCheckbox.addActionListener((e) -> {
            boolean isSelected = showAdvancedCheckbox.isSelected();
            config.putBoolean(ConfigNode.SHOW_ADVANCED_SETTINGS, isSelected);
            advancedPanel.setVisible(isSelected);
        });
        advancedPanel.setVisible(showAdvancedCheckbox.isSelected());

        urlLabel.setText("v" + LightBeat.getVersion() + " | " + urlLabel.getText());
        urlLabel.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                openInBrowser("https://lightbeat.io");
            }
        });

        String version = LightBeat.getVersion();
        componentHolder.getExecutorService().schedule(() -> {

            long updateDisableNotificationTime = config.getLong(ConfigNode.UPDATE_DISABLE_NOTIFICATION);
            if (updateDisableNotificationTime + 172800 > (System.currentTimeMillis() / 1000)) {
                // only show update notification every two days
                return;
            }

            URLConnectionReader reader = new URLConnectionReader("https://lightbeat.io/latest.php");
            try {
                String currentVersion = reader.getFirstLine();
                boolean isCurrentVersion = version.equals(currentVersion);

                if (!isCurrentVersion) {
                    int answerCode = JOptionPane.showConfirmDialog(
                            frame,
                            "A new update is available (version " + currentVersion + ").\n\nUpdate now?",
                            "Update found",
                            JOptionPane.YES_NO_OPTION);
                    if (answerCode == 0) {
                        openInBrowser("https://lightbeat.io/?downloads");
                    } else {
                        config.putLong(ConfigNode.UPDATE_DISABLE_NOTIFICATION, (int) (System.currentTimeMillis() / 1000));
                    }
                }

            } catch (Exception ignored) {
                // fail silently
            }

        }, 5, TimeUnit.SECONDS);

        drawFrame(mainPanel);

        long locationStore = config.getLong(ConfigNode.WINDOW_LOCATION);
        if (locationStore > 0) {
            ByteBuffer locationBuffer = ByteBuffer.allocate(8).putLong(locationStore);
            int storedX = locationBuffer.getInt(0);
            int storedY = locationBuffer.getInt(4);

            // check if in bounds
            Rectangle newBounds = new Rectangle(storedX, storedY, frame.getMinimumSize().width, frame.getMinimumSize().height);
            Rectangle screenBounds = new Rectangle(0, 0, 0, 0);

            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screenDevices = graphicsEnvironment.getScreenDevices();
            for (GraphicsDevice device : screenDevices) {
                screenBounds.add(device.getDefaultConfiguration().getBounds());
            }

            if (screenBounds.contains(newBounds)) {
                runOnSwingThread(() -> frame.setBounds(newBounds));
            }
        }
    }

    @Override
    protected void onWindowClose() {
        audioReader.stop();
        componentHolder.getAudioEventManager().unregisterBeatObserver(this);
        getHueManager().recoverOriginalState();

        // store last location of window in long if moved
        long locationStore = ByteBuffer.allocate(8)
                .putInt(frame.getX())
                .putInt(frame.getY())
                .getLong(0);

        config.putLong(ConfigNode.WINDOW_LOCATION, locationStore);
    }

    public void createUIComponents() {

        bannerLabel = new JIconLabel("banner.png", "bannerflash.png");

        minBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MIN);
        maxBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MAX);
        sensitivitySlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_SENSITIVITY);

        beatSensitivitySlider = new JConfigSlider(config, ConfigNode.BEAT_SENSITIVITY);
        beatTimeBetweenSlider = new JConfigSlider(config, ConfigNode.BEAT_MIN_TIME_BETWEEN);
        transitionTimeSlider = new JConfigSlider(config, ConfigNode.LIGHTS_TRANSITION_TIME);
    }

    @Override
    public void beatReceived(BeatEvent event) {
        runOnSwingThread(() -> bannerLabel.flipIcon());
        executorService.schedule(() -> runOnSwingThread(() -> bannerLabel.flipIcon()), 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void noBeatReceived() {}

    @Override
    public void silenceDetected() {}

    private void openInBrowser(String url) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(url));
            } catch (Exception ignored) {}
        }
    }
}
