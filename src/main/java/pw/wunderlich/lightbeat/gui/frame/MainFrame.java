package pw.wunderlich.lightbeat.gui.frame;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.components.help.HelpButton;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.github.weisj.darklaf.theme.OneDarkTheme;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;
import pw.wunderlich.lightbeat.LightBeat;
import pw.wunderlich.lightbeat.audio.AudioReader;
import pw.wunderlich.lightbeat.audio.BeatEvent;
import pw.wunderlich.lightbeat.audio.BeatEventManager;
import pw.wunderlich.lightbeat.audio.BeatObserver;
import pw.wunderlich.lightbeat.audio.device.AudioDevice;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.gui.swing.*;
import pw.wunderlich.lightbeat.hue.bridge.HueManager;
import pw.wunderlich.lightbeat.hue.bridge.color.ColorSet;
import pw.wunderlich.lightbeat.hue.bridge.color.CustomColorSet;
import pw.wunderlich.lightbeat.hue.bridge.color.RandomColorSet;
import pw.wunderlich.lightbeat.hue.visualizer.HueBeatObserver;
import pw.wunderlich.lightbeat.util.UpdateChecker;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main application frame. UI to set the application settings and start the magic.
 */
public class MainFrame extends AbstractFrame implements BeatObserver {

    private static final int MINIMUM_BRIGHTNESS_DIFFERENCE = 36;

    private final Config config;
    private final AudioReader audioReader;
    private final BeatEventManager beatEventManager;
    private final HueManager hueManager;

    private JPanel mainPanel;

    private JIconLabel bannerLabel;

    private JLabel audioSourceLabel;
    private JComboBox<String> deviceSelectComboBox;
    private HelpButton deviceHelpButton;

    private JButton addCustomColorsButton;
    private JButton deleteCustomColorsButton;
    private JPanel colorSelectPanel;
    private ButtonGroup colorButtonGroup;

    private JPanel lightsPanel;
    private JPanel lightSelectPanel;
    private JButton restoreLightsButton;
    private JConfigSlider beatTimeBetweenSlider;
    private JConfigSlider lightAmountProbabilitySlider;

    private JPanel brightnessPanel;
    private JButton restoreBrightnessButton;
    private JConfigSlider minBrightnessSlider;
    private JConfigSlider maxBrightnessSlider;

    private JPanel advancedPanel;
    private JButton readdColorSetPresetsButton;
    private JButton restoreAdvancedButton;
    private JButton disconnectBridgeButton;
    private JConfigCheckBox strobeCheckBox;
    private JConfigCheckBox colorStrobeCheckbox;
    private JConfigCheckBox glowCheckBox;
    private JConfigCheckBox bassOnlyModeCheckBox;
    private JConfigSlider beatSensitivitySlider;
    private JConfigSlider colorRandomizationSlider;
    private JConfigSlider fadeBrightnessSlider;
    private JConfigSlider maxTransitionTimeSlider;

    private JButton startButton;
    private JConfigCheckBox showAdvancedCheckbox;
    private JConfigCheckBox autoStartCheckBox;
    private JConfigCheckBox lightThemeCheckbox;

    private JLabel urlLabel;
    private JLabel infoLabel;
    private JColorPanel colorsPreviewPanel;

    private HueFrame selectionFrame = null;


    public MainFrame(Config config, AppTaskOrchestrator taskOrchestrator,
                     AudioReader audioReader, BeatEventManager beatEventManager,
                     HueManager hueManager, int x, int y) {
        super(taskOrchestrator, x, y);
        this.config = config;

        this.audioReader = audioReader;
        this.beatEventManager = beatEventManager;
        this.hueManager = hueManager;

        // audio source panel
        refreshDeviceSelector();
        deviceHelpButton.addActionListener(e -> openLinkInBrowser("https://lightbeat.wunderlich.pw/audioguide"));

        // colors panel
        colorsPreviewPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                openColorSelectionFrame(getSelectedColorSetButton().getText());
            }
        });

        addCustomColorsButton.addActionListener(e -> openColorSelectionFrame(null));

        deleteCustomColorsButton.addActionListener(e -> {

            String selected = getSelectedColorSetButton().getText();
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

        // lights panel
        updateLightsPanel();
        restoreLightsButton.addActionListener(e -> {

            // restore lights
            config.remove(ConfigNode.LIGHTS_DISABLED);
            updateLightsPanel();

            restoreDefaults(lightsPanel);
        });

        // brightness panel
        restoreBrightnessButton.addActionListener(e -> restoreDefaults(brightnessPanel));
        minBrightnessSlider.setBoundedSlider(maxBrightnessSlider, true, MINIMUM_BRIGHTNESS_DIFFERENCE);
        maxBrightnessSlider.setBoundedSlider(minBrightnessSlider, false, MINIMUM_BRIGHTNESS_DIFFERENCE);

        // advanced panel
        readdColorSetPresetsButton.addActionListener(e -> addColorSetPresets());
        restoreAdvancedButton.addActionListener(e -> restoreDefaults(advancedPanel));
        disconnectBridgeButton.addActionListener(e -> hueManager.disconnect());

        startButton.addActionListener(e -> {
            if (audioReader.isOpen()) {
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
                openLinkInBrowser("https://lightbeat.wunderlich.pw");
            }
        });

        showAdvancedCheckbox.setToRunOnChange(() -> {
            boolean isWindowMinimumBefore = frame.getMinimumSize().equals(frame.getSize());
            advancedPanel.setVisible(showAdvancedCheckbox.isSelected());
            Dimension minimumSize = frame.getMinimumSize();
            if (isWindowMinimumBefore || frame.getSize().width < minimumSize.width || frame.getSize().height < minimumSize.height) {
                frame.pack();
            }
        });

        lightThemeCheckbox.setToRunOnChange(() -> {
            boolean isDarkCurrently = LafManager.getInstalledTheme().getThemeClass().equals(OneDarkTheme.class);
            boolean setToDark = !lightThemeCheckbox.isSelected();
            if (isDarkCurrently == setToDark) {
                return;
            }

            Font startButtonFont = startButton.getFont();
            LafManager.install(setToDark ? new OneDarkTheme() : new IntelliJTheme());
            startButton.setFont(startButtonFont);
            refreshColorSets();
        });

        if (config.getBoolean(ConfigNode.AUTOSTART)) {
            runOnSwingThread(this::startBeatDetection);
        }

        drawFrame(mainPanel, true);
        restoreLastWindowLocation();

        refreshColorSets();
        if (colorSelectPanel.getComponentCount() < 2) {
            addColorSetPresets();
        }

        setIdleInfoLabelText();
        runOnSwingThread(startButton::requestFocus);
        scheduleUpdateCheck(version);
    }

    public void createUIComponents() {
        bannerLabel = new JIconLabel("/png/banner.png", "/png/bannerflash.png", 482, 100);

        colorSelectPanel = new JPanel(new WrapLayout(FlowLayout.CENTER));
        lightSelectPanel = new JPanel(new WrapLayout(FlowLayout.CENTER));

        beatTimeBetweenSlider = new JConfigSlider(config, ConfigNode.BEAT_MIN_TIME_BETWEEN, value -> value + " millis");
        lightAmountProbabilitySlider = new JConfigSlider(config, ConfigNode.LIGHT_AMOUNT_PROBABILITY, value -> value * 10 + "%");

        minBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MIN, value -> (int) (value / 254f * 100) + "%");
        maxBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MAX, value -> (int) (value / 254f * 100) + "%");

        strobeCheckBox = new JConfigCheckBox(config, ConfigNode.EFFECT_STROBE);
        colorStrobeCheckbox = new JConfigCheckBox(config, ConfigNode.EFFECT_COLOR_STROBE);
        glowCheckBox = new JConfigCheckBox(config, ConfigNode.EFFECT_ALERT);
        bassOnlyModeCheckBox = new JConfigCheckBox(config, ConfigNode.BEAT_BASS_ONLY_MODE);
        beatSensitivitySlider = new JConfigSlider(config, ConfigNode.BEAT_SENSITIVITY, value -> value * 10 + "%");
        colorRandomizationSlider = new JConfigSlider(config, ConfigNode.COLOR_RANDOMIZATION_RANGE, value -> value * 2 + "%");
        fadeBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_FADE_DIFFERENCE, value -> value * 8 + "%");
        maxTransitionTimeSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_FADE_MAX_TIME, value -> value * 100 + " millis");

        showAdvancedCheckbox = new JConfigCheckBox(config, ConfigNode.SHOW_ADVANCED_SETTINGS);
        autoStartCheckBox = new JConfigCheckBox(config, ConfigNode.AUTOSTART);
        lightThemeCheckbox = new JConfigCheckBox(config, ConfigNode.WINDOW_LIGHT_THEME);
    }

    @Override
    protected void onWindowClose() {

        if (isSelectionFrameActive()) {
            selectionFrame.dispose();
        }

        stopBeatDetection();

        // store last location of window
        long locationStore = ByteBuffer.allocate(8)
                .putShort((short) frame.getX())
                .putShort((short) frame.getY())
                .putShort((short) frame.getWidth())
                .putShort((short) frame.getHeight())
                .getLong(0);

        config.putLong(ConfigNode.WINDOW_LOCATION, locationStore);
    }

    @Override
    public void beatReceived(BeatEvent event) {
        runOnSwingThread(() -> {
            bannerLabel.flipIcon();
            colorsPreviewPanel.repaintShifted();
        });
        taskOrchestrator.schedule(() -> runOnSwingThread(() -> bannerLabel.flipIcon()), 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void noBeatReceived() {}

    @Override
    public void silenceDetected() {}

    @Override
    public void audioReaderStopped(StopStatus status) {
        runOnSwingThread(() -> {

            startButton.setText("Start");
            startButton.setEnabled(false);

            setElementsEnabled(true);
            refreshDeviceSelector();

            // re-enable start button with small delay
            taskOrchestrator.schedule(() -> runOnSwingThread(() -> {
                startButton.setEnabled(true);
                startButton.requestFocus();
            }), 1, TimeUnit.SECONDS);

            if (status.equals(StopStatus.ERROR)) {
                showErrorMessage("Selected audio source could not be read");
            } else {
                setIdleInfoLabelText();
            }
        });
    }

    void refreshColorSets() {

        colorSelectPanel.removeAll();
        colorButtonGroup.clearSelection();

        addColorSetButton("Random");
        for (String setName : config.getStringList(ConfigNode.COLOR_SET_LIST)) {
            addColorSetButton(setName);
        }

        JRadioButton selectedSetButton = getSelectedColorSetButton();
        if (!selectedSetButton.getText().equals(config.get(ConfigNode.COLOR_SET_SELECTED))) {
            config.put(ConfigNode.COLOR_SET_SELECTED, selectedSetButton.getText());
        }

        updateColorPreview();
    }

    private void scheduleUpdateCheck(String version) {
        taskOrchestrator.schedule(() -> {

            long updateDisableNotificationTime = config.getLong(ConfigNode.UPDATE_DISABLE_NOTIFICATION);

            // only show notification every 4 days
            long TIME_UNTIL_UPDATE_NOTIFICATION_SECONDS = 345600;
            if (updateDisableNotificationTime + TIME_UNTIL_UPDATE_NOTIFICATION_SECONDS > (System.currentTimeMillis() / 1000)) {
                return;
            }

            var updateChecker = new UpdateChecker(version);
            try {

                if (updateChecker.isUpdateAvailable()) {
                    int answerCode = JOptionPane.showConfirmDialog(
                            frame,
                            "A new update is available (version " + updateChecker.getVersionString() + ").\n\nDownload now?",
                            "Update Found",
                            JOptionPane.YES_NO_OPTION);
                    if (answerCode == 0) {
                        openLinkInBrowser("https://lightbeat.wunderlich.pw/?downloads");
                    } else if (answerCode == 1) {
                        config.putLong(ConfigNode.UPDATE_DISABLE_NOTIFICATION, (int) (System.currentTimeMillis() / 1000));
                    }
                }

            } catch (Exception ignored) {
                // fail silently
            }

        }, 5, TimeUnit.SECONDS);
    }

    private void restoreLastWindowLocation() {

        long locationStore = config.getLong(ConfigNode.WINDOW_LOCATION);
        if (locationStore > 0) {
            var locationBuffer = ByteBuffer.allocate(8).putLong(locationStore);
            short storedX = locationBuffer.getShort(0);
            short storedY = locationBuffer.getShort(2);
            short width = locationBuffer.getShort(4);
            short height = locationBuffer.getShort(6);

            var newBounds = new Rectangle(storedX, storedY, width, height);

            // check if in bounds
            var screenBounds = new Rectangle(0, 0, 0, 0);
            Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
                    .forEach(sd -> screenBounds.add(sd.getDefaultConfiguration().getBounds()));

            if (screenBounds.contains(newBounds)) {
                this.x = newBounds.x;
                this.y = newBounds.y;
                runOnSwingThread(() -> frame.setBounds(newBounds));
            }
        }
    }

    private void startBeatDetection() {
        if (audioReader.isOpen()) {
            return;
        }

        runOnSwingThread(() -> {
            var selectedDeviceName = deviceSelectComboBox.getItemAt(deviceSelectComboBox.getSelectedIndex());
            var audioDevice = audioReader.getDeviceByName(selectedDeviceName);
            if (audioDevice != null) {
                config.put(ConfigNode.LAST_AUDIO_SOURCE, selectedDeviceName);

                var lights = hueManager.getLights(false);
                if (!lights.isEmpty()) {
                    lights.stream().filter(l -> !l.isOn()).forEach(light -> light.setOn(true));
                    var beatObserver = new HueBeatObserver(config, taskOrchestrator, lights);
                    this.beatEventManager.registerBeatObserver(beatObserver);
                    this.beatEventManager.registerBeatObserver(this);

                    boolean audioReaderStarted = audioReader.start(audioDevice);
                    if (audioReaderStarted) {
                        startButton.setText("Stop");
                        startButton.requestFocus();

                        setInfoLabelText("Some settings cannot be changed during visualisation", true);
                        setElementsEnabled(false);
                        return;
                    }
                } else {
                    showErrorMessage("Please select at least one light");
                    return;
                }
            }

            showErrorMessage("Selected audio source is no longer available");
            refreshDeviceSelector();
        });
    }

    private void stopBeatDetection() {
        if (audioReader.isOpen()) {
            audioReader.stop();
            setElementsEnabled(true);
        }
    }

    private void restoreDefaults(JPanel panel) {
        for (Component component : panel.getComponents()) {
            // recurse into child panels
            if (component instanceof JPanel) {
                restoreDefaults((JPanel) component);
            }

            if (component instanceof ConfigComponent) {
                ((ConfigComponent) component).restoreDefault();
            }
        }
    }

    /**
     * Toggles interface elements that cannot be changed during visualisation.
     *
     * @param enabled true to set enabled, false to disable
     */
    private void setElementsEnabled(boolean enabled) {

        deviceSelectComboBox.setEnabled(enabled);

        for (Component component : lightSelectPanel.getComponents()) {
            component.setEnabled(enabled);
        }

        disconnectBridgeButton.setEnabled(enabled);
        strobeCheckBox.setEnabled(enabled);
        colorStrobeCheckbox.setEnabled(enabled);
        glowCheckBox.setEnabled(enabled);
    }

    private boolean refreshDeviceSelector() {
        List<String> deviceNames = audioReader.getSupportedDevices().stream()
                .map(AudioDevice::getName)
                .toList();

        String lastSource;
        if (deviceSelectComboBox.getItemCount() == 0) {
            lastSource = config.get(ConfigNode.LAST_AUDIO_SOURCE);
        } else {
            lastSource = (String) deviceSelectComboBox.getSelectedItem();
        }
        deviceSelectComboBox.removeAllItems();

        if (deviceNames.isEmpty()) {
            deviceSelectComboBox.addItem("Error: No devices found.");
            startButton.setEnabled(false);
            Runnable deviceChecker = new Runnable() {
                @Override
                public void run() {
                    runOnSwingThread(() -> {
                        boolean devicesFound = refreshDeviceSelector();
                        if (devicesFound) {
                            startButton.setEnabled(true);
                        } else {
                            taskOrchestrator.schedule(this, 5, TimeUnit.SECONDS);
                        }
                    });
                }
            };
            taskOrchestrator.schedule(deviceChecker, 5, TimeUnit.SECONDS);
            return false;
        }

        if (lastSource != null) {
            deviceNames.stream()
                    .filter(name -> name.equals(lastSource))
                    .findFirst()
                    .ifPresent(name -> deviceSelectComboBox.addItem(name));
        }

        deviceNames.stream()
                .filter(name -> !name.equals(lastSource))
                .forEach(deviceSelectComboBox::addItem);

        if (deviceNames.stream().anyMatch(name -> name.startsWith("Loopback: "))) {
            audioSourceLabel.setText("Select your main audio devices Loopback, \"Stereo Mix\" or a virtual audio cable for best results.");
        }
        return true;
    }

    private void updateLightsPanel() {

        lightSelectPanel.removeAll();

        var disabledLights = config.getStringList(ConfigNode.LIGHTS_DISABLED);
        hueManager.getBridge()
                .getLights()
                .forEach(light -> {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setText(light.getName());
            if (!disabledLights.contains(light.getId())) {
                checkBox.setSelected(true);
            }

            lightSelectPanel.add(checkBox);

            checkBox.addActionListener(e -> {

                List<String> disabledLightsList = config.getStringList(ConfigNode.LIGHTS_DISABLED);

                if (((JCheckBox) e.getSource()).isSelected()) {
                    disabledLightsList.remove(light.getId());
                } else {
                    disabledLightsList.add(light.getId());
                }

                config.putList(ConfigNode.LIGHTS_DISABLED, disabledLightsList);
            });
        });

        runOnSwingThread(() -> {
            lightSelectPanel.updateUI();
            setElementsEnabled(!audioReader.isOpen());
        });
    }

    private void addColorSetButton(String setName) {
        JRadioButton radioButton = new JRadioButton(setName);
        radioButton.addActionListener(e -> {
            config.put(ConfigNode.COLOR_SET_SELECTED, setName);
            updateColorPreview();
        });

        colorSelectPanel.add(radioButton);
        colorButtonGroup.add(radioButton);
    }

    private void updateColorPreview() {
        String colorSetName = config.get(ConfigNode.COLOR_SET_SELECTED);
        ColorSet colorSet = colorSetName.equals("Random") ? new RandomColorSet() : new CustomColorSet(config, colorSetName);
        colorsPreviewPanel.setColorSet(colorSet);
        // ugly hack to refresh
        colorSelectPanel.setVisible(false);
        colorSelectPanel.setVisible(true);
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
            selectionFrame = showEditPanel ? new ColorSelectionFrame(config, this, setName) : new ColorSelectionFrame(config, this);
            selectionFrame.getJFrame().requestFocus();
        }
    }

    private void addColorSetPresets() {

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

    private JRadioButton getSelectedColorSetButton() {

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

    private void setIdleInfoLabelText() {
        setInfoLabelText("Hover over a setting to get a description", false);
    }

    private void setInfoLabelText(String message, boolean running) {
        final String spacer = " | ";
        final String bridgeName = hueManager.getBridge().getName();
        String status = (running ? "Running" : "Idle") + spacer;
        status += "Connected to " + bridgeName + spacer;
        status += message;

        infoLabel.setText(status);
    }

    private void showErrorMessage(String message) {
        setInfoLabelText(message, false);
        JOptionPane.showMessageDialog(frame, message + ".", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
