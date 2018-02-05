package io.lightbeat.gui.frame;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import io.lightbeat.ComponentHolder;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.hue.bridge.HueStateObserver;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Connection frame. Displays found bridges, allows manual enter and gives instructions to connect.
 */
public class ConnectFrame extends AbstractFrame implements HueStateObserver {

    private JPanel mainPanel;

    private JLabel statusLabel;
    private JComboBox<String> selectBridgeBox;
    private JTextField manualField;
    private JButton refreshButton;

    private JLabel pushlinkImageLabel;
    private JProgressBar pushlinkProgressBar;
    private ScheduledFuture pushlinkProgressTask;

    private JButton connectButton;

    private List<PHAccessPoint> currentAccessPoints = null;


    public ConnectFrame(ComponentHolder componentHolder, int x, int y) {
        super(componentHolder, "Connect", x, y);

        selectBridgeBox.addActionListener(e -> {

            boolean setVisible = false;
            if (selectBridgeBox.getSelectedIndex() + 1 == selectBridgeBox.getItemCount()) {
                setVisible = true;
            }

            if (setVisible != manualField.isVisible()) {
                manualField.setVisible(setVisible);
                getJFrame().pack();
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
                    if (address.length() > 0) {
                        PHAccessPoint accessPoint = new PHAccessPoint();
                        accessPoint.setIpAddress(address);
                        hueManager.setAttemptConnection(accessPoint);
                    }
                } else {
                    if (hueManager.attemptStoredConnection()) {
                        isAttemptingConnection();
                    }
                }

            } else {
                PHAccessPoint accessPoint = currentAccessPoints.get(selectBridgeBox.getSelectedIndex());
                currentAccessPoints = null;
                hueManager.setAttemptConnection(accessPoint);
            }
        });

        drawFrame(mainPanel, true);
    }

    @Override
    public void onWindowClose() {}

    @Override
    public void isScanningForBridges(boolean connectFailed) {
        toggleButtonAndDropdown(false, connectFailed ? "Connection failed, scanning for bridges..." : "Scanning for bridges...");
    }

    @Override
    public void displayFoundBridges(List<PHAccessPoint> list) {
        runOnSwingThread(() -> {
            selectBridgeBox.removeAllItems();
            currentAccessPoints = list;

            if (currentAccessPoints != null) {
                currentAccessPoints.forEach(phAccessPoint -> selectBridgeBox.addItem("Bridge at " + phAccessPoint.getIpAddress()));
            }

            String previousAddress = config.get(ConfigNode.BRIDGE_IPADDRESS);
            if (previousAddress != null) {
                selectBridgeBox.addItem("Previous bridge at " + previousAddress);
            }

            selectBridgeBox.addItem("Enter IP manually");
            selectBridgeBox.setSelectedIndex(0);
            toggleButtonAndDropdown(true, currentAccessPoints != null ? "Bridges found, please select your bridge." : "No bridges found.");
        });
    }

    @Override
    public void isAttemptingConnection() {
        toggleButtonAndDropdown(false, "Requesting connection...");
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
            toggleButtonAndDropdown(false, "Please press the pushlink button on your bridge.");
        });
    }

    @Override
    public void pushlinkHasFailed() {
        runOnSwingThread(() -> {
            pushlinkProgressBar.setVisible(false);
            pushlinkImageLabel.setVisible(false);
            toggleButtonAndDropdown(true, "Pushlinking timed out, please try again.");
        });
    }

    @Override
    public void hasConnected() {}

    private void toggleButtonAndDropdown(boolean setEnabled, String labelText) {
        runOnSwingThread(() -> {
            selectBridgeBox.setEnabled(setEnabled);
            refreshButton.setEnabled(setEnabled);
            connectButton.setEnabled(setEnabled && selectBridgeBox.getItemCount() > 0);
            manualField.setEnabled(setEnabled);
            statusLabel.setText(labelText);
            frame.pack();
        });
    }
}
