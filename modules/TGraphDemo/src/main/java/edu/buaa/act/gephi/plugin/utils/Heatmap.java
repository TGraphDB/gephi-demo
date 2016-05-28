package edu.buaa.act.gephi.plugin.utils;

import org.apache.batik.ext.awt.g2d.DefaultGraphics2D;
import org.gephi.preview.api.G2DTarget;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by song on 16-5-22.
 */
public class Heatmap
{
    public final BufferedImage image ;
    public final Graphics2D graphics = new DefaultGraphics2D(false);

    public Heatmap(float width, float height)
    {
        image = new BufferedImage((int)width,(int)height,BufferedImage.TYPE_INT_ARGB);

    }

}
