package io.lightbeat.gui.frame;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;
import io.lightbeat.ComponentHolder;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.hue.bridge.AccessPoint;
import io.lightbeat.hue.bridge.BridgeConnection;
import io.lightbeat.hue.bridge.HueStateObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Connection frame. Displays found bridges, allows manual enter and gives instructions to connect.
 */
public class ConnectFrame extends AbstractFrame implements HueStateObserver {

    private static final int MANUAL_FIELD_MIN_LENGTH = 6;

    private JPanel mainPanel;

    private LoadingIndicator statusLabelIndicator;
    private JComboBox<String> selectBridgeBox;
    private JTextField manualField;
    private JButton refreshButton;

    private JLabel pushlinkImageLabel;
    private JProgressBar pushlinkProgressBar;
    private ScheduledFuture<?> pushlinkProgressTask;

    private JButton connectButton;

    private List<AccessPoint> currentAccessPoints = null;


    public ConnectFrame(ComponentHolder componentHolder, int x, int y) {
        super(componentHolder, "Connect", x, y);

        selectBridgeBox.addActionListener(e -> {

            boolean setVisible = selectBridgeBox.getSelectedIndex() + 1 == selectBridgeBox.getItemCount();

            if (setVisible != manualField.isVisible()) {
                manualField.setVisible(setVisible);
                connectButton.setEnabled(manualField.getText().length() > MANUAL_FIELD_MIN_LENGTH && !manualField.getForeground().equals(Color.GRAY));
                getJFrame().pack();
            }
        });

        manualField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                executorService.schedule(() -> runOnSwingThread(() -> {
                    boolean isEnabled = manualField.getText().length() > MANUAL_FIELD_MIN_LENGTH;
                    connectButton.setEnabled(isEnabled);

                    if (isEnabled && e.getKeyCode() == KeyEvent.VK_ENTER) {
                        connectButton.doClick();
                    }
                }), 1 , TimeUnit.MILLISECONDS);
            }
        });

        manualField.addFocusListener(new FocusListener() {
            final String text = manualField.getText();
            @Override
            public void focusGained(FocusEvent e) {
                if (manualField.getText().equals(text)) {
                    manualField.setText("");
                    manualField.setForeground(statusLabelIndicator.getForeground());
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (manualField.getText().isEmpty()) {
                    manualField.setForeground(Color.GRAY);
                    manualField.setText(text);
                }
            }
        });

        refreshButton.addActionListener(e -> {
            currentAccessPoints = null;
            toggleButtonAndDropdown(false, "Refreshing ...");
            hueManager.doBridgesScan();
        });

        connectButton.addActionListener(e -> {

            int selectedIndex = selectBridgeBox.getSelectedIndex();
            if (currentAccessPoints == null || selectedIndex >= currentAccessPoints.size()) {

                // manual connect
                if (selectBridgeBox.getItemCount() == selectedIndex + 1) {
                    String address = manualField.getText();
                    if (manualField.getForeground().equals(statusLabelIndicator.getForeground())) {
                        hueManager.setAttemptConnection(new AccessPoint(address));
                    }
                } else {
                    hueManager.attemptStoredConnection();
                }

            } else {
                AccessPoint accessPoint = currentAccessPoints.get(selectBridgeBox.getSelectedIndex());
                hueManager.setAttemptConnection(accessPoint);
            }
        });

        frame.getRootPane().setDefaultButton(connectButton);
        drawFrame(mainPanel, true);
    }

    @Override
    public void onWindowClose() {}

    @Override
    public void isScanningForBridges() {
        toggleButtonAndDropdown(false, "Scanning for bridges");
    }

    @Override
    public void displayFoundBridges(List<AccessPoint> list) {
        runOnSwingThread(() -> {
            selectBridgeBox.removeAllItems();
            currentAccessPoints = list.isEmpty() ? null : list;

            list.forEach(accessPoint -> selectBridgeBox.addItem("Bridge at " + accessPoint.getIp()));

            String previousAddress = config.get(ConfigNode.BRIDGE_IPADDRESS);
            if (previousAddress != null) {
                selectBridgeBox.addItem("Previous bridge at " + previousAddress);
            }

            selectBridgeBox.addItem("Enter IP manually");
            selectBridgeBox.setSelectedIndex(0);
            toggleButtonAndDropdown(true, list.isEmpty() ? "No bridges found" : "Bridges found, please select your bridge");
        });
    }

    @Override
    public void isAttemptingConnection() {
        toggleButtonAndDropdown(false, "Requesting connection");
    }

    @Override
    public void requestPushlink() {

        pushlinkProgressTask = executorService.scheduleAtFixedRate(() -> {

            int currentValue = pushlinkProgressBar.getValue();
            if (currentValue > 0) {
                runOnSwingThread(() -> pushlinkProgressBar.setValue(currentValue - 1));
            } else {
                pushlinkProgressTask.cancel(false);
            }

        }, 1, 1, TimeUnit.SECONDS);

        runOnSwingThread(() -> {
            pushlinkProgressBar.setValue(30);
            pushlinkProgressBar.setVisible(true);
            pushlinkImageLabel.setVisible(true);
            toggleButtonAndDropdown(false, "Please press the pushlink button on your bridge");
        });
    }

    @Override
    public void pushlinkHasFailed() {
        runOnSwingThread(() -> {
            pushlinkProgressTask.cancel(true);
            pushlinkProgressBar.setVisible(false);
            pushlinkImageLabel.setVisible(false);
            toggleButtonAndDropdown(true, "Pushlinking timed out, please try again");
        });
    }

    @Override
    public void hasConnected() {}

    @Override
    public void connectionWasLost(BridgeConnection.ConnectionListener.Error error) {
        String message = "";
        switch (error) {
            case CONNECTION_LOST:
                message = "Connection was lost";
                break;
            case EXCEPTION:
            case NOT_A_BRIDGE:
                message = "Connection could not be established";
                break;
            case NO_LIGHTS:
                message = "This bridge has no valid lights exposed";
        }

        toggleButtonAndDropdown(false, message + ", scanning for bridges");
        hueManager.doBridgesScan();
    }

    private void toggleButtonAndDropdown(boolean setEnabled, String labelText) {
        if (statusLabelIndicator.getText().endsWith("for bridges") && labelText.endsWith("for bridges")) {
            return;
        }

        runOnSwingThread(() -> {
            selectBridgeBox.setEnabled(setEnabled);
            refreshButton.setEnabled(setEnabled);
            connectButton.setEnabled(setEnabled && selectBridgeBox.getItemCount() > 0);
            manualField.setEnabled(setEnabled);
            statusLabelIndicator.setText(labelText);
            statusLabelIndicator.setRunning(!setEnabled);
            if (connectButton.isEnabled()) {
                connectButton.requestFocus();
            }
            frame.pack();
        });
    }
}
