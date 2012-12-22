/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.jai.JAIContext;

import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.awt.color.ICC_ProfileRGB;
import java.awt.color.ICC_Profile;
import java.awt.color.ColorSpace;
import java.util.Map;

import sun.awt.image.ShortInterleavedRaster;
import Jama.Matrix;

/**
 * Copyright (C) Light Crafts, Inc.
 * User: fabio
 * Date: Mar 20, 2007
 * Time: 4:32:46 PM
 */
public class VibranceOpImage extends PointOpImage {
    private final float transform[][];
    private final boolean saturationIncrease;
    private final float[][] toLinearsRGB;

    public VibranceOpImage(RenderedImage source, float transform[][], Map config) {
        super(source, new ImageLayout(source), config, true);
        permitInPlaceOperation();
        this.transform = transform;

        saturationIncrease = transform[0][0] > 1;

        ICC_ProfileRGB linRGB = (ICC_ProfileRGB) ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB);
        toLinearsRGB = new Matrix(linRGB.getMatrix()).inverse().times(new Matrix(((ICC_ProfileRGB) JAIContext.linearProfile).getMatrix())).getArrayFloat();
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        ushortLoop((ShortInterleavedRaster) sources[0], (ShortInterleavedRaster) dest);
    }

    /*
     * faster float arctan2 implementation.
     * see: http://www.dspguru.com/comp.dsp/tricks/alg/fxdatan2.htm
     */

    static float arctan2(float y, float x) {
        final float coeff_1 = (float) Math.PI / 4;
        final float coeff_2 = 3 * coeff_1;
        final float abs_y = Math.abs(y) + 1e-10f;      // kludge to prevent 0/0 condition
        float angle;

        if (x >= 0) {
            float r = (x - abs_y) / (x + abs_y);
            angle = coeff_1 - coeff_1 * r;
        } else {
            float r = (x + abs_y) / (abs_y - x);
            angle = coeff_2 - coeff_1 * r;
        }

        return y < 0 ? -angle : angle;
    }

    protected void ushortLoop(ShortInterleavedRaster src, ShortInterleavedRaster dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        short dstData[] = dst.getDataStorage();
        int dstBandOffsets[] = dst.getDataOffsets();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();

        short srcData[] = src.getDataStorage();
        int srcBandOffsets[] = src.getDataOffsets();
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();

        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int dstROffset = dstBandOffsets[0];
        int dstGOffset = dstBandOffsets[1];
        int dstBOffset = dstBandOffsets[2];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int srcPixOffset = srcPixelStride * col + row * srcLineStride;
                int r = 0xffff & srcData[srcPixOffset + srcROffset];
                int g = 0xffff & srcData[srcPixOffset + srcGOffset];
                int b = 0xffff & srcData[srcPixOffset + srcBOffset];

                float lr = toLinearsRGB[0][0] * r + toLinearsRGB[0][1] * g + toLinearsRGB[0][2] * b;
                float lg = toLinearsRGB[1][0] * r + toLinearsRGB[1][1] * g + toLinearsRGB[1][2] * b;
                float lb = toLinearsRGB[2][0] * r + toLinearsRGB[2][1] * g + toLinearsRGB[2][2] * b;

                float x = lr - 0.5f*(lg+lb);
                float y = 0.866f*(lg-lb);

                float hue = (float) (arctan2(x, y) + Math.PI);

                if (hue < 0)
                    hue += 2 * Math.PI;

                if (hue > 4 * Math.PI / 3)
                    hue -= 4 * Math.PI / 3;
                else if (hue > 2 * Math.PI / 3)
                    hue -= 2 * Math.PI / 3;

                float mask = (float) (0.5 + 0.5 * (1 - Math.abs(Math.PI / 6 - hue) / (Math.PI / 3)));

                if (saturationIncrease) {
                    int min = Math.min(r, Math.min(g, b));
                    int max = Math.max(r, Math.max(g, b));

                    float saturation = max != 0 ? 1 - min / (float) max : 0;

                    mask *= (1 - saturation * saturation);
                }

                float rr = transform[0][0] * r + transform[0][1] * g + transform[0][2] * b;
                float gg = transform[1][0] * r + transform[1][1] * g + transform[1][2] * b;
                float bb = transform[2][0] * r + transform[2][1] * g + transform[2][2] * b;

                rr = (1 - mask) * r + rr * mask;
                gg = (1 - mask) * g + gg * mask;
                bb = (1 - mask) * b + bb * mask;

                int dstPixOffset = dstPixelStride * col + row * dstLineStride;
                dstData[dstPixOffset + dstROffset] = (short) (rr < 0 ? 0 : rr > 0xffff ? 0xffff : rr);
                dstData[dstPixOffset + dstGOffset] = (short) (gg < 0 ? 0 : gg > 0xffff ? 0xffff : gg);
                dstData[dstPixOffset + dstBOffset] = (short) (bb < 0 ? 0 : bb > 0xffff ? 0xffff : bb);
            }
        }
    }
}
