package io.lightbeat.gui.frame;

import com.philips.lighting.model.PHLight;
import com.sun.istack.internal.Nullable;
import io.lightbeat.LightBeat;
import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEvent;
import io.lightbeat.audio.BeatObserver;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.gui.swing.JColorPanel;
import io.lightbeat.gui.swing.JConfigCheckBox;
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

    private JButton addCustomColorsButton;
    private JButton deleteCustomColorsButton;
    private JPanel colorSelectPanel;
    private ButtonGroup colorButtonGroup;

    private JPanel advancedPanel;
    private JButton readdColorSetPresetsButton;
    private JButton restoreAdvancedButton;
    private JConfigSlider beatSensitivitySlider;
    private JConfigSlider beatTimeBetweenSlider;
    private JConfigSlider transitionTimeSlider;
    private JConfigCheckBox strobeCheckBox;
    private JConfigCheckBox glowCheckBox;

    private JButton startButton;
    private JConfigCheckBox showAdvancedCheckbox;
    private JConfigCheckBox autoStartCheckBox;

    private JLabel urlLabel;
    private JLabel infoLabel;
    private JButton editSelectedButton;
    private JColorPanel colorsPreviewPanel;
    private JButton deviceHelpButton;

    private boolean audioReaderIsRunning = false;
    private HueFrame selectionFrame = null;


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

        deviceHelpButton.addActionListener(e -> openLinkInBrowser("https://lightbeat.io/audioguide.php"));

        addCustomColorsButton.addActionListener(e -> openColorSelectionFrame(null));
        editSelectedButton.addActionListener(e -> {

            String selectedSetName = setAndGetSelectedButton().getText();
            if (selectedSetName.equals("Random")) {
                JOptionPane.showMessageDialog(frame,
                        "You cannot edit this set.",
                        "Cannot Edit",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            openColorSelectionFrame(selectedSetName);
        });

        deleteCustomColorsButton.addActionListener(e -> {

            String selected = setAndGetSelectedButton().getText();
            if (selected.equals("Random")) {
                JOptionPane.showMessageDialog(frame,
                        "You cannot delete this set.",
                        "Cannot Delete",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int answerCode = JOptionPane.showConfirmDialog(
                    frame,
                    "Are you sure you want to delete color set " + selected + "?",
                    "Confirm Set Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (answerCode == 0) {
                List<String> sets = config.getStringList(ConfigNode.COLOR_SET_LIST);
                sets.remove(selected);
                config.putList(ConfigNode.COLOR_SET_LIST, sets);
                config.remove(ConfigNode.getCustomNode("color.sets." + selected));
                refreshColorSets();
            }
        });

        refreshColorSets();
        if (colorSelectPanel.getComponentCount() < 2) {
            addColorPresets();
        }

        // setup lights toggle panel
        List<PHLight> allLights = getHueManager().getAllLights();
        List<String> disabledLights = config.getStringList(ConfigNode.LIGHTS_DISABLED);
        for (PHLight light : allLights) {

            JCheckBox checkBox = new JCheckBox();
            checkBox.setText(light.getName());
            checkBox.setBackground(Color.WHITE);
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

                config.putList(ConfigNode.LIGHTS_DISABLED, disabledLightsList);
            });
        }

        restoreBrightnessButton.addActionListener(e -> {
            minBrightnessSlider.restoreDefault();
            maxBrightnessSlider.restoreDefault();
            sensitivitySlider.restoreDefault();
        });
        minBrightnessSlider.setBoundedSlider(maxBrightnessSlider, true, 10);
        maxBrightnessSlider.setBoundedSlider(minBrightnessSlider, false, 10);

        readdColorSetPresetsButton.addActionListener(e -> addColorPresets());
        restoreAdvancedButton.addActionListener(e -> {
            beatSensitivitySlider.restoreDefault();
            beatTimeBetweenSlider.restoreDefault();
            transitionTimeSlider.restoreDefault();
            strobeCheckBox.restoreDefault();
            glowCheckBox.restoreDefault();
        });

        startButton.addActionListener(e -> {
            if (audioReaderIsRunning) {
                stopBeatDetection();
            } else {
                startBeatDetection();
            }
        });

        String version = LightBeat.getVersion();
        urlLabel.setText("v" + version + " | " + urlLabel.getText());
        urlLabel.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                openLinkInBrowser("https://lightbeat.io");
            }
        });

        // schedule updater task
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
                        openLinkInBrowser("https://lightbeat.io/?downloads");
                    } else {
                        config.putLong(ConfigNode.UPDATE_DISABLE_NOTIFICATION, (int) (System.currentTimeMillis() / 1000));
                    }
                }

            } catch (Exception ignored) {
                // fail silently
            }

        }, 5, TimeUnit.SECONDS);

        drawFrame(mainPanel, true);

        showAdvancedCheckbox.setToRunOnChange(() -> {
            advancedPanel.setVisible(showAdvancedCheckbox.isSelected());
            frame.pack();
        });

        // restore last windows location
        long locationStore = config.getLong(ConfigNode.WINDOW_LOCATION);
        if (locationStore > 0) {
            ByteBuffer locationBuffer = ByteBuffer.allocate(8).putLong(locationStore);
            int storedX = locationBuffer.getInt(0);
            int storedY = locationBuffer.getInt(4);

            // check if in bounds
            Rectangle newBounds = new Rectangle(storedX, storedY, 100, 100);
            Rectangle screenBounds = new Rectangle(0, 0, 0, 0);

            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screenDevices = graphicsEnvironment.getScreenDevices();
            for (GraphicsDevice device : screenDevices) {
                screenBounds.add(device.getDefaultConfiguration().getBounds());
            }

            if (screenBounds.contains(newBounds)) {
                runOnSwingThread(() -> {
                    newBounds.setSize(frame.getSize());
                    frame.setBounds(newBounds);
                });
            }
        }

        if (config.getBoolean(ConfigNode.AUTOSTART)) {
            runOnSwingThread(this::startBeatDetection);
        }
    }

    @Override
    protected void onWindowClose() {

        if (isSelectionFrameActive()) {
            selectionFrame.dispose();
        }

        stopBeatDetection();

        // store last location of window
        long locationStore = ByteBuffer.allocate(8)
                .putInt(frame.getX())
                .putInt(frame.getY())
                .getLong(0);

        config.putLong(ConfigNode.WINDOW_LOCATION, locationStore);
    }

    public void createUIComponents() {

        bannerLabel = new JIconLabel("/png/banner.png", "/png/bannerflash.png");

        minBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MIN);
        maxBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MAX);
        sensitivitySlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_SENSITIVITY);

        beatSensitivitySlider = new JConfigSlider(config, ConfigNode.BEAT_SENSITIVITY);
        beatTimeBetweenSlider = new JConfigSlider(config, ConfigNode.BEAT_MIN_TIME_BETWEEN);
        transitionTimeSlider = new JConfigSlider(config, ConfigNode.LIGHTS_TRANSITION_TIME);
        strobeCheckBox = new JConfigCheckBox(config, ConfigNode.BRIGHTNESS_STROBE);
        glowCheckBox = new JConfigCheckBox(config, ConfigNode.BRIGHTNESS_GLOW);

        showAdvancedCheckbox = new JConfigCheckBox(config, ConfigNode.SHOW_ADVANCED_SETTINGS);
        autoStartCheckBox = new JConfigCheckBox(config, ConfigNode.AUTOSTART);
    }

    void refreshColorSets() {

        colorSelectPanel.removeAll();
        colorButtonGroup.clearSelection();

        addRadioButton("Random");
        config.getStringList(ConfigNode.COLOR_SET_LIST).forEach(this::addRadioButton);

        JRadioButton selectedSetButton = setAndGetSelectedButton();
        if (!selectedSetButton.getText().equals(config.get(ConfigNode.COLOR_SET_SELECTED))) {
            config.put(ConfigNode.COLOR_SET_SELECTED, selectedSetButton.getText());
        }

        colorsPreviewPanel.setColorSet(getHueManager().getColorSet());
        colorSelectPanel.updateUI();
        frame.pack();
    }

    private void addRadioButton(String setName) {
        JRadioButton radioButton = new JRadioButton(setName);
        radioButton.setBackground(Color.WHITE);
        radioButton.addActionListener(e -> {
            config.put(ConfigNode.COLOR_SET_SELECTED, setName);
            colorsPreviewPanel.setColorSet(getHueManager().getColorSet());
        });

        colorSelectPanel.add(radioButton);
        colorButtonGroup.add(radioButton);
    }

    private JRadioButton setAndGetSelectedButton() {

        JRadioButton toReturn = null;
        String selectedButton = config.get(ConfigNode.COLOR_SET_SELECTED);
        for (Component radioButton : colorSelectPanel.getComponents()) {
            JRadioButton button = (JRadioButton) radioButton;
            if (button.getText().equals(selectedButton)) {
                toReturn = button;
                break;
            }
        }

        if (toReturn == null) {
            toReturn = (JRadioButton) colorSelectPanel.getComponents()[0];
        }

        toReturn.setSelected(true);
        return toReturn;
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

    private void startBeatDetection() {

        if (audioReaderIsRunning) {
            return;
        }

        String selectedMixerName = deviceSelectComboBox.getItemAt(deviceSelectComboBox.getSelectedIndex());
        List<Mixer> supportedMixers = audioReader.getSupportedMixers();
        for (Mixer supportedMixer : supportedMixers) {
            String mixerName = supportedMixer.getMixerInfo().getName();
            if (mixerName.equals(selectedMixerName)) {
                config.put(ConfigNode.LAST_AUDIO_SOURCE, mixerName);

                audioReaderIsRunning = getHueManager().initializeLights();
                if (audioReaderIsRunning) {
                    boolean startedSuccessfully = audioReader.start(supportedMixer);
                    if (startedSuccessfully) {
                        startButton.setText("Stop");
                        infoLabel.setText("Running, stop to reload any changes made");
                        componentHolder.getAudioEventManager().registerBeatObserver(this);
                        return;
                    }

                } else {
                    infoLabel.setText("No lights were selected");
                    return;
                }
            }
        }

        infoLabel.setText("Selected audio source is no longer available");
    }

    private void stopBeatDetection() {
        if (audioReaderIsRunning) {
            startButton.setText("Start");
            startButton.setEnabled(false);
            infoLabel.setText("Idle");
            audioReader.stop();
            componentHolder.getAudioEventManager().unregisterBeatObserver(this);
            getHueManager().recoverOriginalState();
            audioReaderIsRunning = false;

            // re-enable with small delay
            executorService.schedule(() -> runOnSwingThread(() -> startButton.setEnabled(true)), 1, TimeUnit.SECONDS);
        }
    }

    private void openLinkInBrowser(String url) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(url));
            } catch (Exception ignored) {}
        }
    }

    private boolean isSelectionFrameActive() {
        return selectionFrame != null && selectionFrame.getJFrame().isDisplayable();
    }

    private void openColorSelectionFrame(@Nullable String setName) {

        if (isSelectionFrameActive()) {
            JOptionPane.showMessageDialog(frame,
                    "Color set editor is already open.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            selectionFrame.getJFrame().requestFocus();
        } else {
            boolean showEditPanel = setName != null;
            selectionFrame = showEditPanel ? new ColorSelectionFrame(this, setName) : new ColorSelectionFrame(this);
        }
    }

    private void addColorPresets() {

        boolean hasAdded = false;
        List<String> currentSetNames = config.getStringList(ConfigNode.COLOR_SET_LIST);

        for (String presetName : config.getStringList(ConfigNode.COLOR_SET_PRESET_LIST)) {
            if (!currentSetNames.contains(presetName)) {
                hasAdded = true;
                currentSetNames.add(presetName);
            }
        }

        if (hasAdded) {
            config.putList(ConfigNode.COLOR_SET_LIST, currentSetNames);
            refreshColorSets();
        }
    }
}
