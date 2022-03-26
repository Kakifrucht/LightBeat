package io.lightbeat.gui.frame;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.components.border.DarkBorders;
import com.github.weisj.darklaf.components.help.HelpButton;
import com.github.weisj.darklaf.theme.DarculaTheme;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import io.github.zeroone3010.yahueapi.Light;
import io.lightbeat.ComponentHolder;
import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEvent;
import io.lightbeat.audio.BeatObserver;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.gui.swing.*;
import io.lightbeat.util.UpdateChecker;

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

    private static final int MINIMUM_BRIGHTNESS_DIFFERENCE = 36;

    private final AudioReader audioReader;

    private JPanel mainPanel;

    private JIconLabel bannerLabel;

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


    public MainFrame(ComponentHolder componentHolder, int x, int y) {
        super(componentHolder, x, y);

        audioReader = componentHolder.getAudioReader();

        // audio source panel
        refreshDeviceSelectComboBox();
        deviceHelpButton.addActionListener(e -> openLinkInBrowser("https://lightbeat.io/audioguide"));

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

        refreshColorSets();
        if (colorSelectPanel.getComponentCount() < 2) {
            addColorSetPresets();
        }

        // lights panel
        updateLightsPanel();
        lightSelectPanel.setLayout(new WrapLayout(0));
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

        String version = componentHolder.getVersion();
        urlLabel.setText("v" + version + " | " + urlLabel.getText());
        urlLabel.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                openLinkInBrowser("https://lightbeat.io");
            }
        });

        showAdvancedCheckbox.setToRunOnChange(() -> {
            advancedPanel.setVisible(showAdvancedCheckbox.isSelected());
            frame.pack();
        });

        lightThemeCheckbox.setToRunOnChange(() -> {

            boolean isDarkCurrently = LafManager.getInstalledTheme().getThemeClass().equals(DarculaTheme.class);
            boolean setToDark = !lightThemeCheckbox.isSelected();
            if (isDarkCurrently == setToDark) {
                return;
            }

            Font startButtonFont = startButton.getFont();
            LafManager.install(setToDark ? new DarculaTheme() : new IntelliJTheme());
            startButton.setFont(startButtonFont);
            frame.pack();
        });

        if (config.getBoolean(ConfigNode.AUTOSTART)) {
            runOnSwingThread(this::startBeatDetection);
        }

        drawFrame(mainPanel, true);
        runOnSwingThread(startButton::requestFocus);

        restoreLastWindowLocation();
        scheduleUpdateCheck(version);
    }

    public void createUIComponents() {

        bannerLabel = new JIconLabel("/png/banner.png", "/png/bannerflash.png", 482, 100);

        beatTimeBetweenSlider = new JConfigSlider(config, ConfigNode.BEAT_MIN_TIME_BETWEEN, value -> value + " millis");
        lightAmountProbabilitySlider = new JConfigSlider(config, ConfigNode.LIGHT_AMOUNT_PROBABILITY, value -> value * 10 + "%");

        minBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MIN, value -> (int) (value / 254f * 100) + "%");
        maxBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_MAX, value -> (int) (value / 254f * 100) + "%");

        strobeCheckBox = new JConfigCheckBox(config, ConfigNode.EFFECT_STROBE);
        colorStrobeCheckbox = new JConfigCheckBox(config, ConfigNode.EFFECT_COLOR_STROBE);
        glowCheckBox = new JConfigCheckBox(config, ConfigNode.EFFECT_ALERT);
        bassOnlyModeCheckBox = new JConfigCheckBox(config, ConfigNode.BEAT_BASS_ONLY_MODE);
        beatSensitivitySlider = new JConfigSlider(config, ConfigNode.BEAT_SENSITIVITY);
        colorRandomizationSlider = new JConfigSlider(config, ConfigNode.COLOR_RANDOMIZATION_RANGE);
        fadeBrightnessSlider = new JConfigSlider(config, ConfigNode.BRIGHTNESS_FADE_DIFFERENCE);
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
                .putInt(frame.getX())
                .putInt(frame.getY())
                .getLong(0);

        config.putLong(ConfigNode.WINDOW_LOCATION, locationStore);
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
    public void audioReaderStopped(StopStatus status) {
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
            } else {
                infoLabel.setText("Idle | Hover over a setting to get a description");
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

        colorsPreviewPanel.setColorSet(hueManager.getColorSet());
        colorSelectPanel.setBorder(DarkBorders.createWidgetLineBorder(1, 1, 1, 1));
        colorSelectPanel.repaint();
        frame.pack();
    }

    private void scheduleUpdateCheck(String version) {
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
    }

    private void restoreLastWindowLocation() {

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
                        startButton.requestFocus();

                        infoLabel.setText("Running | Some settings cannot be changed during visualisation");
                        componentHolder.getAudioEventManager().registerBeatObserver(this);
                        setElementsEnabled(false);
                    }
                } else {
                    showErrorMessage("Please select at least one light");
                }

                return;
            }
        }

        showErrorMessage("Selected audio source is no longer available");
        refreshDeviceSelectComboBox();
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

    private void refreshDeviceSelectComboBox() {

        List<String> mixerNames = audioReader.getSupportedMixers().stream()
                .map(mixer -> mixer.getMixerInfo().getName())
                .collect(Collectors.toList());

        String lastSource;
        if (deviceSelectComboBox.getItemCount() == 0) {
            lastSource = config.get(ConfigNode.LAST_AUDIO_SOURCE);
        } else {
            lastSource = (String) deviceSelectComboBox.getSelectedItem();
        }

        // add mixer names to combobox
        deviceSelectComboBox.removeAllItems();
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

    private void updateLightsPanel() {

        lightSelectPanel.removeAll();

        List<String> disabledLights = config.getStringList(ConfigNode.LIGHTS_DISABLED);
        for (Light light : hueManager.getBridge().getLights()) {

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
        }

        runOnSwingThread(() -> {
            lightSelectPanel.updateUI();
            setElementsEnabled(!audioReader.isOpen());
        });
    }

    private void addColorSetButton(String setName) {
        JRadioButton radioButton = new JRadioButton(setName);
        radioButton.addActionListener(e -> {
            config.put(ConfigNode.COLOR_SET_SELECTED, setName);
            colorsPreviewPanel.setColorSet(hueManager.getColorSet());
        });

        colorSelectPanel.add(radioButton);
        colorButtonGroup.add(radioButton);
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

    private void showErrorMessage(String message) {
        infoLabel.setText("Idle | " + message);
        JOptionPane.showMessageDialog(frame, message + ".", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
