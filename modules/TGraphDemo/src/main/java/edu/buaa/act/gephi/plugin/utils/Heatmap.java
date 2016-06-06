package edu.buaa.act.gephi.plugin.utils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Created by song on 16-5-22.
 */
public class Heatmap
{
    private BufferedImage image=null ;
    private final Graphics2D graphics = new BufferedImage(5000,5000,BufferedImage.TYPE_INT_ARGB).createGraphics();

    public Heatmap() {

    }

    public BufferedImage getImage() {
        return image;
    }

    public Graphics2D getGraphics() {
        return graphics;
    }

    public void setImageSize(float width, float height)
    {
        if(image==null) {
            image = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
        }
    }
}
