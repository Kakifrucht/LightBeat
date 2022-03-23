package io.lightbeat.gui.swing;

import io.lightbeat.hue.bridge.color.Color;
import io.lightbeat.hue.bridge.color.ColorSet;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A panel that draws a color palette for hue and sat selection in its background.
 * x-axis represents hue value, y-axis saturation (0 is highest sat value)
 */
public class JColorPanel extends JPanel {

    private BufferedImage canvas;
    private List<Color> colorSet;

    private boolean drawSaturationGradient = true;


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
        drawSaturationGradient = false;
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
                float hue = (float) x / width;
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
