package edu.buaa.act.gephi.plugin.utils;

import java.awt.Color;

/**
 * @deprecated use LinearGradientInt instead.
 * A copy from org.gephi.appearance.plugin.RankingElementColorTransformer
 * but return int of RGB value rather than a Color object.
 * Created by song on 16-6-4.
 */
public class LinearGradient {
    private int[] colors;
    private int[] colorsRed;
    private int[] colorsGreen;
    private int[] colorsBlue;
    private int[] colorsAlpha;
    private float[] positions;

    public LinearGradient(Color[] colors, float[] positions) {
        if (colors != null && positions != null) {
            if (colors.length != positions.length) {
                throw new IllegalArgumentException();
            } else {
                this.colors = new int[colors.length];
                this.colorsRed = new int[colors.length];
                this.colorsGreen = new int[colors.length];
                this.colorsBlue = new int[colors.length];
                this.colorsAlpha = new int[colors.length];
                for (int i = 0; i < colors.length; i++) {
                    this.colors[i] = colors[i].getRGB();
                    this.colorsRed[i] = colors[i].getRed();
                    this.colorsGreen[i] = colors[i].getGreen();
                    this.colorsBlue[i] = colors[i].getBlue();
                    this.colorsAlpha[i] = colors[i].getAlpha();
                }
                this.positions = positions;
            }
        } else {
            throw new NullPointerException();
        }
    }

    public int getValue(float pos) {
        for (int a = 0; a < this.positions.length - 1; ++a) {
            if (this.positions[a] == pos) {
                return this.colors[a];
            }

            if (this.positions[a] < pos && pos < this.positions[a + 1]) {
                float v = (pos - this.positions[a]) / (this.positions[a + 1] - this.positions[a]);
                return this.tween(a, a + 1, v);
            }
        }

        if (pos <= this.positions[0]) {
            return this.colors[0];
        } else if (pos >= this.positions[this.positions.length - 1]) {
            return this.colors[this.colors.length - 1];
        } else {
            throw new RuntimeException("this should not happen!");
        }
    }

    private int tween(int c1index, int c2index, float p) {
        final int r = (int) ((float) colorsRed[c1index] * (1.0F - p) + (float) colorsRed[c2index] * p);
        final int g = (int) ((float) colorsGreen[c1index] * (1.0F - p) + (float) colorsGreen[c2index] * p);
        final int b = (int) ((float) colorsBlue[c1index] * (1.0F - p) + (float) colorsBlue[c2index] * p);
        final int a = (int) ((float) colorsAlpha[c1index] * (1.0F - p) + (float) colorsAlpha[c2index] * p);
        return  ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                ((b & 0xFF));
    }
}
