package edu.buaa.act.gephi.plugin.preview;


import edu.buaa.act.gephi.plugin.utils.LinearGradient;
import edu.buaa.act.gephi.plugin.utils.LinearGradientInt;
import org.gephi.preview.api.CanvasSize;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.Vector;
import org.gephi.preview.plugin.items.NodeItem;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Render HeatMap Effect. inspired by:
 * http://www.cnblogs.com/bdqlaccp/archive/2012/09/12/2681518.html
 * https://github.com/pa7/heatmap.js/blob/master/src/renderer/canvas2d.js
 * How to use this Class ?
 * 0. call resetSize() to clear current size info.
 * 1. call preProcess() on each item to update final image size
 * 2. call getMin/MaxX/Y() to get image size.
 * 3. call firstPhaseRender() on each item to draw a alpha image
 * 4. call calculateHeatColor() to get the final heat map image
 * 5. call secondPhaseRender() one each item to draw the final heat map image.
 * Here we use lots of 'final' keyword to speed up the calculation.
 * Created by song on 16-5-21.
 */
public class HeatMapRenderer {
    public static final String HEAT = "traffic.heat";
    public static final String HEATMAP_IMG = "traffic.heat.img";
    public static final String HEATMAP_IMG_SCALE = "traffic.heat.img.scale";
    private static final String SOURCE = "source";
    private static final String TARGET = "target";
    private static final String TARGET_RADIUS = "edge.target.radius";
    private static final String SOURCE_RADIUS = "edge.source.radius";

    private float minX;
    private float minY;
    private float maxX;
    private float maxY;

    volatile public boolean stopRender = false;

    private final Color startColor = new Color(0, 0, 0, 20);
    private final Color endColor = new Color(0, 0, 0, 0);

    public void resetSize() {
        minX = Float.POSITIVE_INFINITY;
        minY = Float.POSITIVE_INFINITY;
        maxX = Float.NEGATIVE_INFINITY;
        maxY = Float.NEGATIVE_INFINITY;
    }

    public void preProcess(final Item item) {
        final Helper h = new Helper(item);
        updateMinMax(new float[]{h.x1, h.x2}, new float[]{h.y1, h.y2});
    }

    private void updateMinMax(float[] xList, float[] yList) {
        for (float x : xList) {
            if (x > maxX) maxX = x;
            if (x < minX) minX = x;
        }
        for (float y : yList) {
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
    }

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMaxY() {
        return maxY;
    }

    public void firstPhaseRender(final Item item, final Graphics2D graphics2D, float maxHeatValue, float maxHeatArea) {
        final int heat = item.getData(HEAT);
        if(heat==0) return;
        final Helper r = new Helper(item);
        final float value = maxHeatArea * heat / maxHeatValue; // range: [ 0, maxHeatAreaSize ]
        final double length = Math.sqrt(Math.pow(r.y2 - r.y1, 2) + Math.pow(r.x2 - r.x1, 2));
        final double centralX = (r.x1 + r.x2) / 2;
        final double centralY = (r.y1 + r.y2) / 2;
        if (value * 2 < length) { // for long road with small heat. treat as rect.
            final Graphics2D g = (Graphics2D) graphics2D.create();
            g.rotate(Math.atan((r.y2 - r.y1) / (r.x2 - r.x1)), centralX, centralY);
            g.setPaint(new GradientPaint(
                    r.x1, (float) centralY, startColor,
                    r.x1, r.y2 + value, endColor, true));
            g.fill(new RoundRectangle2D.Double(
                    centralX - length / 2, centralY - value, // start point x y of rect
                    length, 2 * value, // width and height of rect
                    10, value)); //
            g.dispose();
        } else { // for short road with hight heat value, just treat as a point.
            graphics2D.setPaint(new RadialGradientPaint(
                            new Point2D.Double(centralX, centralY), value,
                            new float[]{0.0f, 1.0f},
                            new Color[]{startColor, endColor})
            );
            graphics2D.fillOval((int) (centralX - value), (int) (centralY - value), (int) (value * 2), (int) (value * 2));
        }
    }


    /**
     * convert an alpha image to heat map image.
     * @deprecated use calculateHeatColor instead.
     * because when using LinearGradientInt, render 50000000 pixels only needs 50ms,
     * so we do not need to break the operation. we do not need to show a progress bar.
     */
    public void calculateHeatColorForEachPixel(final BufferedImage image, final int x, final int y, LinearGradient gradient){
//        final int alpha = new Color(image.getRGB(x, y),true).getAlpha();
        final int alpha = (image.getRGB(x, y)>> 24) & 0xff; // get alpha value in color int, faster way of pre line.
        image.setRGB(x, y, gradient.getValue(alpha/255f));
//        image.setRGB(x, y, new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 128).getRGB());
    }

    public void calculateHeatColor(final BufferedImage image, LinearGradientInt gradient){
        final int[] imagePixelData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        final int len = imagePixelData.length;
        for(int i=0;i<len;i++) {
            final int alpha = (imagePixelData[i] >> 24) & 0xff;
            imagePixelData[i] = gradient.getValue(alpha);
        }
    }

    public LinearGradientInt buildColorGradient(){
        final Color colorBlue = colorAddAlpha(Color.BLUE, 128);
        final Color colorCyan = colorAddAlpha(Color.CYAN, 128);
        final Color colorGreen = colorAddAlpha(Color.GREEN, 128);
        final Color colorYellow = colorAddAlpha(Color.YELLOW, 128);
        final Color colorRed = colorAddAlpha(Color.RED, 128);
        return new LinearGradientInt(
                new Color[]{colorBlue , colorCyan, colorGreen, colorYellow, colorRed},
                new float[]{0f, 0.25f, 0.5f, 0.75f, 1f},
                256);
    }

    // speed up. calculate alpha color forehead.
    private Color colorAddAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public void secondPhaseRender(final Item item, final Graphics2D graphics2D){
        final BufferedImage image = item.getData(HEATMAP_IMG);
        final double scale = item.getData(HEATMAP_IMG_SCALE);
        if(image!=null){
            Graphics2D g = (Graphics2D) graphics2D.create();
            g.scale(scale,scale);
            g.drawImage(
                    image,
                    (int) (((int)getMinX())*scale),
                    (int) (((int)getMinY())*scale),
                    image.getWidth(),
                    image.getHeight(),null);
            g.dispose();
        }
    }

    public CanvasSize getCanvasSize(
            final Item item,
            final PreviewProperties properties) {
        return new CanvasSize(minX, minY, maxX - minX, maxY - minY);
    }

    private class Helper {

        public final Item sourceItem;
        public final Item targetItem;
        public final Float x1;
        public final Float x2;
        public final Float y1;
        public final Float y2;

        public Helper(final Item item) {
            sourceItem = item.getData(SOURCE);
            targetItem = item.getData(TARGET);

            Float _x1 = sourceItem.getData(NodeItem.X);
            Float _x2 = targetItem.getData(NodeItem.X);
            Float _y1 = sourceItem.getData(NodeItem.Y);
            Float _y2 = targetItem.getData(NodeItem.Y);

            //Target radius - to start at the base of the arrow
            final Float targetRadius = item.getData(TARGET_RADIUS);
            //Avoid edge from passing the node's center:
            if (targetRadius != null && targetRadius < 0) {
                Vector direction = new Vector(_x2, _y2);
                direction.sub(new Vector(_x1, _y1));
                direction.normalize();
                direction.mult(targetRadius);
                direction.add(new Vector(_x2, _y2));
                _x2 = direction.x;
                _y2 = direction.y;
            }

            //Source radius
            final Float sourceRadius = item.getData(SOURCE_RADIUS);
            //Avoid edge from passing the node's center:
            if (sourceRadius != null && sourceRadius < 0) {
                Vector direction = new Vector(_x1, _y1);
                direction.sub(new Vector(_x2, _y2));
                direction.normalize();
                direction.mult(sourceRadius);
                direction.add(new Vector(_x1, _y1));
                _x1 = direction.x;
                _y1 = direction.y;
            }

            x1 = _x1;
            y1 = _y1;
            x2 = _x2;
            y2 = _y2;
        }
    }

//
//
//    /**
//     * Converts an HSL color value to RGB. Conversion formula
//     * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
//     * Assumes h, s, and l are contained in the set [0, 1] and
//     * returns r, g, and b in the set [0, 255].
//     *
//     * @param h       The hue
//     * @param s       The saturation
//     * @param l       The lightness
//     * @return int array, the RGB representation
//     */
//    public static int[] hslToRgb(float h, float s, float l){
//        float r, g, b;
//
//        if (s == 0f) {
//            r = g = b = l; // achromatic
//        } else {
//            float q = l < 0.5 ? l * (1 + s) : l + s - l * s;
//            float p = 2 * l - q;
//            r = hueToRgb(p, q, h + 1f/3f);
//            g = hueToRgb(p, q, h);
//            b = hueToRgb(p, q, h - 1f/3f);
//        }
//        int[] rgb = {(int) (r * 255), (int) (g * 255), (int) (b * 255)};
//        return rgb;
//    }
//
//    /** Helper method that converts hue to rgb */
//    public static float hueToRgb(float p, float q, float t) {
//        if (t < 0f)
//            t += 1f;
//        if (t > 1f)
//            t -= 1f;
//        if (t < 1f/6f)
//            return p + (q - p) * 6f * t;
//        if (t < 1f/2f)
//            return q;
//        if (t < 2f/3f)
//            return p + (q - p) * (2f/3f - t) * 6f;
//        return p;
//    }
}
