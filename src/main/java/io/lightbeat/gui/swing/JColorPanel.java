package io.lightbeat.gui.swing;

import io.lightbeat.hue.light.color.ColorSet;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A panel that draws a color palette for hue and sat selection in it's background.
 * x-axis represents hue value, y-axis saturation (0 is highest sat value)
 */
public class JColorPanel extends JPanel {

    private BufferedImage canvas;
    private List<Color> colorSet;

    private boolean drawSaturationGradient = true;


    public JColorPanel() {
        super();
        setBorder(new LineBorder(Color.BLACK));
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

    public void setColorSet(ColorSet colorSet) {
        this.colorSet = colorSet.getColors();
        drawSaturationGradient = false;
        createCanvas();
        updateUI();
    }

    private void createCanvas() {

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
                    int rgb = Color.HSBtoRGB(hue, drawSaturationGradient ? (float) (height - y) / height : 1f, 1f);
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
    }
}
