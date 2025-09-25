package pw.wunderlich.lightbeat.gui.swing;

import pw.wunderlich.lightbeat.hue.bridge.color.Color;
import pw.wunderlich.lightbeat.hue.bridge.color.ColorSet;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * A panel that draws a color palette for hue and sat selection in its background.
 * x-axis represents hue value, y-axis saturation (0 is highest sat value)
 */
public class JColorPanel extends JPanel {

    /**
     * The step by which the hue is shifted when colorSet is null.
     */
    private static final float HUE_SHIFT_STEP = 0.05f;

    private BufferedImage canvas;
    private List<Color> colorSet;

    private boolean drawSaturationGradient = true;

    /**
     * The current hue shift offset.
     */
    private float hueShift = 0.0f;


    public JColorPanel() {
        super();

        setBorder(new LineBorder(java.awt.Color.BLACK));
        repaintCanvas();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repaintCanvas();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                setBorder(new LineBorder(java.awt.Color.LIGHT_GRAY));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                setBorder(new LineBorder(java.awt.Color.BLACK));
            }
        });
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.drawImage(canvas, null, null);
    }

    public void setColorSet(ColorSet colorSet) {
        this.colorSet = colorSet.getColors();
        this.hueShift = 0f;
        drawSaturationGradient = false;
        repaintCanvas();
    }

    /**
     * Repaints the canvas, shifting the drawn colors.
     * If a ColorSet is active, it rotates the colors (e.g., [c1, c2, c3] becomes [c2, c3, c1]).
     * If no ColorSet is active (hue gradient), it shifts the hue value by a constant step.
     */
    public void repaintShifted() {
        if (colorSet == null) {
            hueShift = (hueShift + HUE_SHIFT_STEP) % 1f;
        } else {
            if (!colorSet.isEmpty()) {
                Collections.rotate(colorSet, -1);
            }
        }
        repaintCanvas();
    }

    private void repaintCanvas() {

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        if (colorSet == null) {

            for (int x = 0; x < width; x++) {
                float hue = ((float) x / width + hueShift) % 1f;
                for (int y = 0; y < height; y++) {
                    int rgb = java.awt.Color.HSBtoRGB(hue, drawSaturationGradient ? (float) (height - y) / height : 1f, 1f);
                    canvas.setRGB(x, y, rgb);
                }
            }

        } else {
            for (int x = 0; x < width; x++) {
                int chunkSize = width / colorSet.size();
                int index = Math.min(x / chunkSize, colorSet.size() - 1);
                int rgb = colorSet.get(index).getRGB();
                for (int y = 0; y < height; y++) {
                    canvas.setRGB(x, y, rgb);
                }
            }
        }

        repaint();
    }
}
