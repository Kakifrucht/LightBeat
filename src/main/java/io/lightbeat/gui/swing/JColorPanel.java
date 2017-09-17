package io.lightbeat.gui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

/**
 * A panel that draws a color palette for hue and sat selection in it's background.
 * x-axis represents hue value, y-axis saturation (0 is highest sat value)
 */
public class JColorPanel extends JPanel {

    private BufferedImage canvas;


    public JColorPanel() {
        createCanvas();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                createCanvas();
            }
        });
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.drawImage(canvas, null, null);
    }

    private void createCanvas() {

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            float hue = (float) x / width;
            for (int y = 0; y < height; y++) {
                int rgb = Color.HSBtoRGB(hue, (float) (height - y) / height, 1.0f);
                canvas.setRGB(x, y, rgb);
            }
        }
    }
}
