package edu.buaa.act.gephi.plugin.utils;

import java.awt.*;

/**
 * A Fast LinearGradient if you have finite number of colors finally.
 * Copy from org.gephi.appearance.plugin.RankingElementColorTransformer
 * Created by song on 16-6-6.
 */
public class LinearGradientInt {
    private Color[] colors;
    private int[] colorArray;
    private float[] positions;
    private int totalColorCount;

    /**
     * Calculate all colors in constructor.
     * @param totalColorCount count of your colors finally.
     */
    public LinearGradientInt(Color[] colors, float[] positions, int totalColorCount) {
        if (colors != null && positions != null) {
            if (colors.length != positions.length) {
                throw new IllegalArgumentException();
            } else {
                this.colors = colors;
                this.positions = positions;
                this.totalColorCount = totalColorCount;
                this.colorArray = new int[totalColorCount];
                for (int i = 0; i < totalColorCount; i++) {
                    this.colorArray[i] = getValue(((float)i)/totalColorCount);
                }
            }
        } else {
            throw new NullPointerException();
        }
    }

    private int getValue(float pos) {
        for (int a = 0; a < this.positions.length - 1; ++a) {
            if (this.positions[a] == pos) {
                return this.colors[a].getRGB();
            }

            if (this.positions[a] < pos && pos < this.positions[a + 1]) {
                float v = (pos - this.positions[a]) / (this.positions[a + 1] - this.positions[a]);
                return this.tween(this.colors[a], this.colors[a + 1], v);
            }
        }

        if (pos <= this.positions[0]) {
            return this.colors[0].getRGB();
        } else if (pos >= this.positions[this.positions.length - 1]) {
            return this.colors[this.colors.length - 1].getRGB();
        } else {
            throw new RuntimeException("this should not happen!");
        }
    }

    private int tween(Color c1, Color c2, float p) {
        return new Color(
                (int)((float)c1.getRed() * (1.0F - p) + (float)c2.getRed() * p),
                (int)((float)c1.getGreen() * (1.0F - p) + (float)c2.getGreen() * p),
                (int)((float)c1.getBlue() * (1.0F - p) + (float)c2.getBlue() * p),
                (int)((float)c1.getAlpha() * (1.0F - p) + (float)c2.getAlpha() * p))
                .getRGB();
    }

    /**
     * Get color value directly from the pre-computed colorArray.
     * @param pos an int, range[0, totalColorCount-1]
     * @return RGBA value of the color.
     */
    public int getValue(int pos) {
        try{
            return colorArray[pos];
        }catch(IndexOutOfBoundsException e){
            throw new IllegalArgumentException("this position is not included in the color array!",e);
        }
    }
}
