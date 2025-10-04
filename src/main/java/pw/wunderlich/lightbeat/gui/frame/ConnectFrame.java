package pw.wunderlich.lightbeat.gui.frame;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;
import pw.wunderlich.lightbeat.hue.bridge.AccessPoint;
import pw.wunderlich.lightbeat.hue.bridge.BridgeConnection;
import pw.wunderlich.lightbeat.hue.bridge.HueManager;
import pw.wunderlich.lightbeat.hue.bridge.HueStateObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Connection frame. Displays found bridges, allows manual enter and gives instructions to connect.
 */
public class ConnectFrame extends AbstractFrame implements HueStateObserver {

    private static final int MANUAL_FIELD_MIN_LENGTH = 6;

    private final HueManager hueManager;

    private JPanel mainPanel;

    private LoadingIndicator statusLabelIndicator;
    private JComboBox<String> selectBridgeBox;
    private JTextField manualField;
    private JButton scanButton;

    private JLabel pushlinkImageLabel;
    private JProgressBar pushlinkProgressBar;
    private ScheduledFuture<?> pushlinkProgressTask;

    private JButton connectButton;

    private List<AccessPoint> currentAccessPoints = null;


    public ConnectFrame(AppTaskOrchestrator taskOrchestrator, HueManager hueManager, int x, int y) {
        super(taskOrchestrator, "Connect", x, y);
        this.hueManager = hueManager;

        selectBridgeBox.addActionListener(e -> {

            boolean setManualVisible = selectBridgeBox.getSelectedIndex() + 1 == selectBridgeBox.getItemCount();
            if (!setManualVisible) {
                connectButton.setEnabled(true);
            } else if (!manualField.isVisible()) {
                connectButton.setEnabled(isManualFieldFilled());
            }
            manualField.setVisible(setManualVisible);
            getJFrame().pack();
        });

        manualField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                taskOrchestrator.schedule(() -> runOnSwingThread(() -> {
                    boolean isEnabled = isManualFieldFilled();
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

        scanButton.addActionListener(e -> {
            toggleButtonAndDropdown(false, "Refreshing ...");
            hueManager.doBridgesScan();
        });

        connectButton.addActionListener(e -> {
            int selectedIndex = selectBridgeBox.getSelectedIndex();
            if (currentAccessPoints != null && selectedIndex < currentAccessPoints.size()) {
                AccessPoint accessPoint = currentAccessPoints.get(selectedIndex);
                hueManager.setAttemptConnection(accessPoint);
            } else if (isManualFieldFilled()) {
                hueManager.setAttemptConnection(new AccessPoint(manualField.getText()));
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
    public void displayFoundBridges(List<AccessPoint> foundBridges) {
        List<AccessPoint> allBridges = new ArrayList<>(hueManager.getPreviousBridges());
        allBridges.addAll(foundBridges);
        setBridgeList(allBridges);
        toggleButtonAndDropdown(true, foundBridges.isEmpty() ? "No bridges found" : "Bridges found, please select your bridge");
    }

    @Override
    public void isAttemptingConnection() {
        toggleButtonAndDropdown(false, "Requesting connection");
    }

    @Override
    public void requestPushlink() {

        pushlinkProgressTask = taskOrchestrator.schedulePeriodicTask(() -> {
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
    public void connectionWasLost(AccessPoint accessPoint, BridgeConnection.ConnectionListener.Error error) {
        String message = switch (error) {
            case CONNECTION_LOST -> "Connection was lost";
            case EXCEPTION -> "Connection could not be established";
            case NOT_A_BRIDGE -> "Endpoint is not a Hue bridge";
            case WRONG_CERTIFICATE -> "The security certificate could not be verified.";
            case NO_LIGHTS -> "This bridge has no valid lights exposed";
        };

        if (error.equals(BridgeConnection.ConnectionListener.Error.WRONG_CERTIFICATE)) {
            String dialogTitle = "Security Warning";
            String dialogMessage = String.format(
                    "<html>The security certificate for the bridge at %s has changed.<br><br>" +
                            "This could be a security risk, or the device may have been reset.<br>" +
                            "Do you want to trust this new certificate and connect anyway?</html>",
                    accessPoint.ip()
            );
            int result = JOptionPane.showConfirmDialog(
                    frame,
                    dialogMessage,
                    dialogTitle,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (result == JOptionPane.YES_OPTION) {
                AccessPoint apNoCertificate = new AccessPoint(accessPoint.ip(), accessPoint.key(), accessPoint.name());
                runOnSwingThread(() -> hueManager.setAttemptConnection(apNoCertificate));
                return;
            }
        }

        List<AccessPoint> previousBridges = hueManager.getPreviousBridges();
        if (previousBridges.isEmpty()) {
            hueManager.doBridgesScan();
            toggleButtonAndDropdown(false, message + ", scanning for bridges");
        } else {
            setBridgeList(previousBridges);
            toggleButtonAndDropdown(true, message);
        }
    }

    @Override
    public void disconnected() {
        setBridgeList(hueManager.getPreviousBridges());
        toggleButtonAndDropdown(true, "Bridge disconnected");
    }

    private void setBridgeList(List<AccessPoint> bridges) {
        currentAccessPoints = bridges;
        runOnSwingThread(() -> {
            selectBridgeBox.removeAllItems();

            bridges.stream()
                    .map(bridge -> (bridge.hasKey() ? "Reconnect to " : "Connect to ") + Objects.requireNonNullElse(bridge.name(), "Unknown") + " at " + bridge.ip())
                    .forEach(selectBridgeBox::addItem);

            selectBridgeBox.addItem("Enter IP manually");
            selectBridgeBox.setSelectedIndex(0);
        });
    }

    private boolean isManualFieldFilled() {
        return manualField.getText().length() > MANUAL_FIELD_MIN_LENGTH && !manualField.getForeground().equals(Color.GRAY);
    }

    private void toggleButtonAndDropdown(boolean setEnabled, String labelText) {
        if (statusLabelIndicator.getText().endsWith("for bridges") && labelText.endsWith("for bridges")) {
            return;
        }

        runOnSwingThread(() -> {
            selectBridgeBox.setEnabled(setEnabled);
            scanButton.setEnabled(setEnabled);
            connectButton.setEnabled(setEnabled && (selectBridgeBox.getItemCount() > 1 || isManualFieldFilled()));
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
