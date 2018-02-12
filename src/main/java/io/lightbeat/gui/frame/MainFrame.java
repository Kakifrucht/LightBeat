package io.lightbeat.gui.frame;

import com.bulenkov.darcula.DarculaLaf;
import com.philips.lighting.model.PHLight;
import io.lightbeat.ComponentHolder;
import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEvent;
import io.lightbeat.audio.BeatObserver;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.gui.swing.JColorPanel;
import io.lightbeat.gui.swing.JConfigCheckBox;
import io.lightbeat.gui.swing.JConfigSlider;
import io.lightbeat.gui.swing.JIconLabel;
import io.lightbeat.util.UpdateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseAdapter;
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

    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);

    private final AudioReader audioReader;

    private JPanel mainPanel;

    private JIconLabel bannerLabel;

    private JComboBox<String> deviceSelectComboBox;

    private JButton addCustomColorsButton;
    private JButton deleteCustomColorsButton;
    private JPanel colorSelectPanel;
    private ButtonGroup colorButtonGroup;

    private JButton restoreBrightnessButton;
    private JConfigSlider minBrightnessSlider;
    private JConfigSlider maxBrightnessSlider;
    private JConfigSlider transitionTimeSlider;

    private JPanel lightsPanel;
    private JConfigSlider beatTimeBetweenSlider;

    private JPanel advancedPanel;
    private JButton readdColorSetPresetsButton;
    private JButton restoreAdvancedButton;
    private JConfigSlider beatSensitivitySlider;
    private JConfigSlider colorRandomizationSlider;
    private JConfigCheckBox strobeCheckBox;
    private JConfigCheckBox glowCheckBox;

    private JButton startButton;
    private JConfigCheckBox showAdvancedCheckbox;
    private JConfigCheckBox autoStartCheckBox;
    private JConfigCheckBox darculaThemeCheckBox;

    private JLabel urlLabel;
    private JLabel infoLabel;
    private JButton editSelectedButton;
    private JColorPanel colorsPreviewPanel;
    private JButton deviceHelpButton;

    private HueFrame selectionFrame = null;


    public MainFrame(ComponentHolder componentHolder, int x, int y) {
        super(componentHolder, x, y);

        audioReader = componentHolder.getAudioReader();

        // audio source panel
        refreshDeviceSelectComboBox();
        deviceHelpButton.addActionListener(e -> openLinkInBrowser("https://lightbeat.io/audioguide.php"));

        // colors panel
        colorsPreviewPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                openColorSelectionFrame(getSelectedSetButton().getText());
            }
        });

        addCustomColorsButton.addActionListener(e -> openColorSelectionFrame(null));
        editSelectedButton.addActionListener(e -> openColorSelectionFrame(getSelectedSetButton().getText()));

        deleteCustomColorsButton.addActionListener(e -> {

            String selected = getSelectedSetButton().getText();
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

        // lights panel
        List<PHLight> allLights = hueManager.getLights();
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

                config.putList(ConfigNode.LIGHTS_DISABLED, disabledLightsList);
            });
        }

        // brightness panel
        restoreBrightnessButton.addActionListener(e -> {
            minBrightnessSlider.restoreDefault();
            maxBrightnessSlider.restoreDefault();
            transitionTimeSlider.restoreDefault();
        });
        minBrightnessSlider.setBoundedSlider(maxBrightnessSlider, true, 10);
        maxBrightnessSlider.setBoundedSlider(minBrightnessSlider, false, 10);

        // advanced panel
        readdColorSetPresetsButton.addActionListener(e -> addColorPresets());
        restoreAdvancedButton.addActionListener(e -> {
            beatSensitivitySlider.restoreDefault();
            colorRandomizationSlider.restoreDefault();
            strobeCheckBox.restoreDefault();
            glowCheckBox.restoreDefault();
        });

        startButton.addActionListener(e -> {
            if (audioReader.isOpen()) {
                stopBeatDetection();
            } else {
                startBeatDetection();
            }
        });
        startButton.requestFocus();

        String version = componentHolder.getVersion();
        urlLabel.setText("v" + version + " | " + urlLabel.getText());
        urlLabel.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                openLinkInBrowser("https://lightbeat.io");
            }
        });

        drawFrame(mainPanel, true);

        showAdvancedCheckbox.setToRunOnChange(() -> {
            advancedPanel.setVisible(showAdvancedCheckbox.isSelected());
            frame.pack();
        });

        darculaThemeCheckBox.setToRunOnChange(() -> {
            try {
                if (darculaThemeCheckBox.isSelected()) {
                    UIManager.setLookAndFeel(new DarculaLaf());
                } else {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }

                SwingUtilities.updateComponentTreeUI(frame);
                frame.pack();
            } catch (Exception e) {
                logger.warn("Couldn't set look and feel", e);
            }
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

        // schedule updater task
        componentHolder.getExecutorService().schedule(() -> {

            long updateDisableNotificationTime = config.getLong(ConfigNode.UPDATE_DISABLE_NOTIFICATION);

            // only show notification every 4 days, disable if on snapshot version
            long TIME_UNTIL_UPDATE_NOTIFICATION_SECONDS = 345600;
            if (updateDisableNotificationTime + TIME_UNTIL_UPDATE_NOTIFICATION_SECONDS > (System.currentTimeMillis() / 1000)
                    || version.endsWith("SNAPSHOT")) {
                return;
            }

            UpdateChecker updateChecker = new UpdateChecker(version);
            try {

                if (updateChecker.isUpdateAvailable()) {
                    int answerCode = JOptionPane.showConfirmDialog(
                            frame,
                            "A new update is available (version " + updateChecker.getVersionString() + ").\n\nDownload now?",
                            "Update Found",
                            JOptionPane.YES_NO_OPTION);
                    if (answerCode == 0) {
                        openLinkInBrowser("https://lightbeat.io/?downloads");
                    } else if (answerCode == 1) {
                        config.putLong(ConfigNode.UPDATE_DISABLE_NOTIFICATION, (int) (System.currentTimeMillis() / 1000));
                    }
                }

            } catch (Exception ignored) {
                // fail silently
            }

        }, 5, TimeUnit.SECONDS);

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

        bannerLabel = new JIconLabel("/png/banner.png", "/png/bannerflash.png", 482, 100);

        minBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MIN);
        maxBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MAX);
        transitionTimeSlider = new JConfigSlider(config, ConfigNode.LIGHTS_TRANSITION_TIME);

        beatTimeBetweenSlider = new JConfigSlider(config, ConfigNode.BEAT_MIN_TIME_BETWEEN);
        beatSensitivitySlider = new JConfigSlider(config, ConfigNode.BEAT_SENSITIVITY);
        colorRandomizationSlider = new JConfigSlider(config, ConfigNode.COLOR_RANDOMIZATION_RANGE);
        strobeCheckBox = new JConfigCheckBox(config, ConfigNode.BRIGHTNESS_STROBE);
        glowCheckBox = new JConfigCheckBox(config, ConfigNode.BRIGHTNESS_GLOW);

        showAdvancedCheckbox = new JConfigCheckBox(config, ConfigNode.SHOW_ADVANCED_SETTINGS);
        autoStartCheckBox = new JConfigCheckBox(config, ConfigNode.AUTOSTART);
        darculaThemeCheckBox = new JConfigCheckBox(config, ConfigNode.WINDOW_LOOK_AND_FEEL);
    }

    void refreshColorSets() {

        colorSelectPanel.removeAll();
        colorButtonGroup.clearSelection();

        addRadioButton("Random");
        for (String setName : config.getStringList(ConfigNode.COLOR_SET_LIST)) {
            addRadioButton(setName);
        }

        JRadioButton selectedSetButton = getSelectedSetButton();
        if (!selectedSetButton.getText().equals(config.get(ConfigNode.COLOR_SET_SELECTED))) {
            config.put(ConfigNode.COLOR_SET_SELECTED, selectedSetButton.getText());
        }

        colorsPreviewPanel.setColorSet(hueManager.getColorSet());
        colorSelectPanel.repaint();
        frame.pack();
    }

    private void refreshDeviceSelectComboBox() {

        deviceSelectComboBox.removeAllItems();

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
    }

    private void addRadioButton(String setName) {
        JRadioButton radioButton = new JRadioButton(setName);
        radioButton.addActionListener(e -> {
            config.put(ConfigNode.COLOR_SET_SELECTED, setName);
            colorsPreviewPanel.setColorSet(hueManager.getColorSet());
        });

        colorSelectPanel.add(radioButton);
        colorButtonGroup.add(radioButton);
    }

    private JRadioButton getSelectedSetButton() {

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

    @Override
    public void readerStopped(StopStatus status) {
        runOnSwingThread(() -> {

            startButton.setText("Start");
            startButton.setEnabled(false);

            refreshDeviceSelectComboBox();

            // re-enable startbutton with small delay
            executorService.schedule(() -> runOnSwingThread(() -> {
                startButton.setEnabled(true);
                startButton.requestFocus();
            }), 1, TimeUnit.SECONDS);

            if (status.equals(StopStatus.ERROR)) {
                showErrorMessage("Selected audio source could not be read");
                infoLabel.setText("Idle | Selected audio source could not be read");
            } else {
                infoLabel.setText("Idle | Hover over a setting to get a description");
            }
        });
    }

    private void startBeatDetection() {

        if (audioReader.isOpen()) {
            return;
        }

        String selectedMixerName = deviceSelectComboBox.getItemAt(deviceSelectComboBox.getSelectedIndex());
        List<Mixer> supportedMixers = audioReader.getSupportedMixers();
        for (Mixer supportedMixer : supportedMixers) {
            String mixerName = supportedMixer.getMixerInfo().getName();
            if (mixerName.equals(selectedMixerName)) {
                config.put(ConfigNode.LAST_AUDIO_SOURCE, mixerName);

                boolean lightsInitialized = hueManager.initializeLights();
                if (lightsInitialized) {
                    boolean audioReaderStarted = audioReader.start(supportedMixer);
                    if (audioReaderStarted) {
                        startButton.setText("Stop");
                        infoLabel.setText("Running | Stop to reload any changes made to the settings");
                        componentHolder.getAudioEventManager().registerBeatObserver(this);
                        return;
                    }
                } else {
                    showErrorMessage("No lights were selected");
                    return;
                }
            }
        }

        showErrorMessage("Selected audio source is no longer available");
    }

    private void stopBeatDetection() {
        if (audioReader.isOpen()) {
            audioReader.stop();
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

    private void openColorSelectionFrame(String setName) {

        if ("Random".equals(setName)) {

            JOptionPane.showMessageDialog(frame,
                    "You cannot edit this set.",
                    "Cannot Edit",
                    JOptionPane.ERROR_MESSAGE);

        } else if (isSelectionFrameActive()) {
            JOptionPane.showMessageDialog(frame,
                    "Color set editor is already open.",
                    "Editor Open",
                    JOptionPane.INFORMATION_MESSAGE);
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

    private void showErrorMessage(String message) {
        infoLabel.setText(message);
        JOptionPane.showMessageDialog(frame, message + ".", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
