package io.lightbeat.gui.frame;

import com.philips.lighting.hue.sdk.PHAccessPoint;
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


    public ConnectFrame(int x, int y) {
        super("Connect", x, y);

        selectBridgeBox.addActionListener(e -> {
            boolean setVisible = false;
            if (currentAccessPoints == null || selectBridgeBox.getSelectedIndex() >= currentAccessPoints.size()) {
                setVisible = true;
            }

            if (setVisible != manualField.isVisible()) {
                manualField.setVisible(setVisible);
                getJFrame().setVisible(true);
            }
        });

        refreshButton.addActionListener(e -> {
            currentAccessPoints = null;
            getHueManager().doBridgesScan();
        });

        connectButton.addActionListener(e -> {
            if (currentAccessPoints == null || selectBridgeBox.getSelectedIndex() >= currentAccessPoints.size()) {
                // manual connect
                String address = manualField.getText();
                if (address.length() > 0) {
                    PHAccessPoint accessPoint = new PHAccessPoint();
                    accessPoint.setIpAddress(address);
                    getHueManager().setAttemptConnection(accessPoint);
                }
            } else {
                PHAccessPoint accessPoint = currentAccessPoints.get(selectBridgeBox.getSelectedIndex());
                currentAccessPoints = null;
                getHueManager().setAttemptConnection(accessPoint);
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

            selectBridgeBox.addItem("Enter IP manually");
            toggleButtonAndDropdown(true, currentAccessPoints != null ? "Bridges found, please select your bridge." : "No bridges found, please type IP manually.");
        });
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
    public void isAttemptingConnection() {
        toggleButtonAndDropdown(false, "Requesting connection...");
    }

    @Override
    public void pushlinkHasFailed() {
        runOnSwingThread(() -> {
            pushlinkProgressBar.setVisible(false);
            pushlinkImageLabel.setVisible(false);
            toggleButtonAndDropdown(true, "Pushlinking timed out, please try again.");
        });
    }

    private void toggleButtonAndDropdown(boolean setEnabled, String labelText) {
        runOnSwingThread(() -> {
            selectBridgeBox.setEnabled(setEnabled);
            refreshButton.setEnabled(setEnabled);
            connectButton.setEnabled(setEnabled && selectBridgeBox.getItemCount() > 0);
            statusLabel.setText(labelText);
        });
    }
}
