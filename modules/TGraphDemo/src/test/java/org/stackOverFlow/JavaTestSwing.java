package org.stackOverFlow;

/**
 * Created by song on 16-5-22.
 */

import org.junit.Ignore;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 *
 * @web http://java-buddy.blogspot.com/
 */
@Ignore
public class JavaTestSwing {

    static JFrameWin jFrameWindow;

    public static class MyComponent extends JComponent{



        @Override
        protected void paintComponent(Graphics g) {

            //g.drawImage(bufferedImage, 0, 0, null);
            Graphics2D graphics = (Graphics2D)g;
            int width = getWidth();
            int height = getHeight();



            float x1 = width/4;
            float y1 = height/4;
            float x2 = width/4 + width/2;
            float y2 = height/4 + height/2;
            Color color1 = Color.RED;
            Color color2 = Color.WHITE;




//            GradientPaint gradientPaint
//                    = new GradientPaint(x1, y1, color1, x2, y2, color2);
//
//            graphics.setPaint(gradientPaint);
//            Rectangle2D.Double rectangle
//                    = new Rectangle2D.Double(width/4, height/4, width/2, height/2);
//            graphics.fill(rectangle);
//            graphics.fill3DRect((int)x1,(int)y1,width/8,height/8,true);

            float hx1=x1+60;
            float hy1=y1+40;
            float hx2=x2-30;
            float hy2=y2;


            double theta = Math.atan((hy2 - hy1) / (hx2 - hx1));
            double length = Math.sqrt(Math.pow(hy2-hy1,2)+Math.pow(hx2-hx1,2));
            Graphics2D g0 = (Graphics2D) graphics.create();
            g0.rotate(theta, (hx1 + hx2) / 2, (hy1 + hy2) / 2);
//            drawPoint(g0, hx1,hy1, "h1");
//            drawPoint(g0, hx2,hy2, "h2");

            float value=20;
//            g0.drawString("h1",hx1,hy1);
            GradientPaint gPaint = new GradientPaint(hx1, (hy1+hy2)/2, new Color(0,0,0,20), hx1, (hy1+hy2)/2+value, new Color(0,0,0,0),true);
            g0.setPaint(gPaint);
            double w = width/2;
            double h = height/2;
            double x = width/4;
            double y = height/4;
            double arcWidth= height/8;
            double arcHeight = height/4;
            float minX = Math.min(hx1,hx2);
            float maxX = Math.max(hx1, hx2);
            float minY = Math.min(hy1, hy2);
            float maxY = Math.max(hy1,hy2);

            drawPoint(g0, minX,minY, "min"+minX);
            drawPoint(g0, maxX,maxY, "max");
            drawPoint(g0, minX, (maxY+minY)/2, "try");
            RoundRectangle2D.Double rect = new RoundRectangle2D.Double(
                    (minX+maxX-length)/2,(maxY+minY)/2-value,
                    length,2*value,
                    0,value);
            g0.fill(rect);
            g0.dispose();
//            g0.drawRenderableImage();


            graphics.setStroke(new BasicStroke(
                    1f,
                    BasicStroke.CAP_SQUARE,
                    BasicStroke.JOIN_MITER));
            graphics.setColor(Color.BLACK);
            final Line2D.Float line = new Line2D.Float(hx1, hy1, hx2, hy2);
            graphics.draw(line);
        }

        void drawPoint(Graphics2D g0, double x, double y, String name){
            Graphics2D g = (Graphics2D) g0.create();
            Rectangle2D.Double point = new Rectangle2D.Double(x - 0.5, y - 0.5, 1, 1);
            g.setColor(Color.RED);
            g.draw(point);
            g.drawString(name, (float) x, (float) y);
            g.dispose();
        }
    }


    public static class JFrameWin extends JFrame{
        public JFrameWin(){
            this.setTitle("java-buddy.blogspot.com");
            this.setSize(300, 300);
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            MyComponent myComponent = new MyComponent();
            this.add(myComponent);
        }
    }

    public static void main(String[] args){
        Runnable doSwingLater = new Runnable(){

            @Override
            public void run() {
                jFrameWindow = new JFrameWin();
                jFrameWindow.setVisible(true);
            }
        };

        SwingUtilities.invokeLater(doSwingLater);

    }

}