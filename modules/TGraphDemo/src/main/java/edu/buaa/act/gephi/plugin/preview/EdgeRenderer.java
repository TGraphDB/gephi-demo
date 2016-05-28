package edu.buaa.act.gephi.plugin.preview;

import org.gephi.preview.api.*;
import org.gephi.preview.plugin.items.EdgeItem;
import org.gephi.preview.plugin.items.NodeItem;
import org.gephi.preview.types.EdgeColor;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Here we use lots of 'final' keyword to speed up the calculation.
 * Created by song on 16-5-21.
 */
public class EdgeRenderer {
    private static final String SOURCE="source";
    private static final String TARGET="target";
    private static final String TARGET_RADIUS = "edge.target.radius";
    private static final String SOURCE_RADIUS = "edge.source.radius";

    public void render(
            final Item item,
            final RenderTarget target,
            final PreviewProperties properties) {
        final Helper h = new Helper(item);
        final Color color = getColor(item, properties);

        final Graphics2D graphics = ((G2DTarget) target).getGraphics();
        graphics.setStroke(new BasicStroke(
                getThickness(item),
                BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_MITER));
        graphics.setColor(color);
        final Line2D.Float line
                = new Line2D.Float(h.x1, h.y1, h.x2, h.y2);
        graphics.draw(line);

        // custom paint.
        final int heatValue = item.getData("traffic.heat");// range: [ 0, 100 ]
        final double length = Math.sqrt(Math.pow(h.y2-h.y1,2)+Math.pow(h.x2-h.x1,2));
        final double centralX = (h.x1 + h.x2) / 2;
        final double centralY = (h.y1 + h.y2) / 2;
        final Graphics2D g = (Graphics2D) graphics.create();
        g.rotate(Math.atan((h.y2 - h.y1) / (h.x2 - h.x1)), centralX, centralY);
        g.setPaint(new GradientPaint(h.x1, (float) centralY, new Color(0,0,0,20), h.x1, h.y2, new Color(0,0,0,0),true));
        g.fill(new RoundRectangle2D.Double(centralX-length/2, centralY-heatValue, length, 2*heatValue, 10, heatValue));
        g.dispose();
    }

    public void render(
            final Item item,
            final Graphics2D graphics,
            final PreviewProperties properties) {
        final Helper h = new Helper(item);
        final Color color = getColor(item, properties);

        graphics.setStroke(new BasicStroke(
                getThickness(item),
                BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_MITER));
        graphics.setColor(color);
        final Line2D.Float line = new Line2D.Float(h.x1, h.y1, h.x2, h.y2);
        graphics.draw(line);

//        RadialGradientPaint p = new RadialGradientPaint(new Point2D.Double(x, y), radius,
//                new float[]{
//                        0.0f, 1.0f},
//                new Color[]{
//                        startColor,
//                        endColor});
//        graphics.fillOval((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2));

    }

    public CanvasSize getCanvasSize(
            final Item item,
            final PreviewProperties properties) {
        final Item sourceItem = item.getData(SOURCE);
        final Item targetItem = item.getData(TARGET);
        final Float x1 = sourceItem.getData(NodeItem.X);
        final Float x2 = targetItem.getData(NodeItem.X);
        final Float y1 = sourceItem.getData(NodeItem.Y);
        final Float y2 = targetItem.getData(NodeItem.Y);
        final float minX = Math.min(x1, x2);
        final float minY = Math.min(y1, y2);
        final float maxX = Math.max(x1, x2);
        final float maxY = Math.max(y1, y2);
        return new CanvasSize(minX, minY, maxX - minX, maxY - minY);
    }

    private static float getThickness(final Item item) {
        return ((Double) item.getData(EdgeItem.WEIGHT)).floatValue();
    }

    public static Color getColor(
            final Item item,
            final PreviewProperties properties) {
        final Item sourceItem = item.getData(SOURCE);
        final Item targetItem = item.getData(TARGET);
        final EdgeColor edgeColor
                = (EdgeColor) properties.getValue(PreviewProperty.EDGE_COLOR);
        final Color color = edgeColor.getColor(
                (Color) item.getData(EdgeItem.COLOR),
                (Color) sourceItem.getData(NodeItem.COLOR),
                (Color) targetItem.getData(NodeItem.COLOR));
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int) (getAlpha(properties) * 255));
    }

    private static float getAlpha(final PreviewProperties properties) {
        float opacity = properties.getIntValue(PreviewProperty.EDGE_OPACITY) / 100F;
        if (opacity < 0) {
            opacity = 0;
        }
        if (opacity > 1) {
            opacity = 1;
        }
        return opacity;
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
}
