package org.java2s;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
/**
 * Created by song on 16-6-4.
 */
public class GraphicsScaleExample extends JPanel {

    public void paint(Graphics g) {

        g.fillRect(0, 0, 20, 20);

        Graphics2D g2 = (Graphics2D) g;

        g2.translate(50, 50);
        g2.rotate(30.0 * Math.PI / 180.0);

        g2.scale(2.0, 2.0);

        g.setColor(Color.red);

        g.fillRect(0, 0, 20, 20);

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new GraphicsScaleExample());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 200);
        frame.setVisible(true);
    }
}