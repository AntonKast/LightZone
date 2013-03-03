/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.jai.opimage.FastBilateralFilterOpImage;
import com.lightcrafts.jai.opimage.HDROpImage;
import com.lightcrafts.jai.opimage.HDROpImage2;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.ColorScience;

import java.text.DecimalFormat;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;
import java.lang.ref.SoftReference;

/**
 * Copyryght (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: Apr 18, 2007
 * Time: 12:02:23 PM
 */
public class HDROperationV3 extends BlendedOperation {
    private double depth = 64;
    private double highlights = 0.2;
    private double detail = 1.5;
    private double shadows = 3.0;
    private double fuzz = 0.1;

    private final static String HIGHLIGHTS = "Highlights";
    private final static String DETAIL = "Detail";
    private final static String SHADOWS = "Shadows";
    private final static String DEPTH = "Depth";
    private final static String FUZZ = "Fuzz";

    public HDROperationV3(Rendering rendering, OperationType type) {
        super(rendering, type);

        DecimalFormat format = new DecimalFormat("0.00");

        addSliderKey(SHADOWS);
        setSliderConfig(SHADOWS, new SliderConfig(0, 10, shadows, .05, false, format));

        addSliderKey(HIGHLIGHTS);
        setSliderConfig(HIGHLIGHTS, new SliderConfig(0, 1, highlights, .05, false, format));

        addSliderKey(DETAIL);
        setSliderConfig(DETAIL, new SliderConfig(0, 10, detail, .05, false, format));

        addSliderKey(DEPTH);
        setSliderConfig(DEPTH, new SliderConfig(8, 64, depth, .05, false, format));
        
        addSliderKey(FUZZ);
        setSliderConfig(FUZZ, new SliderConfig(0.1, 1, fuzz, .05, false, format));
    }

    public boolean neutralDefault() {
        return false;
    }

    static final OperationType typeV5 = new OperationTypeImpl("Tone Mapper V5");

    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key == FUZZ && fuzz != value) {
            fuzz = value;
        } else if (key == DEPTH && depth != value) {
            depth = value;
        } else if (key == HIGHLIGHTS && highlights != value) {
            highlights = value;
        } else if (key == DETAIL && detail != value) {
            detail = value;
        } else if (key == SHADOWS && shadows != value) {
            shadows = value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private class ToneMaperTransform extends BlendedTransform {
        ToneMaperTransform(PlanarImage source) {
            super(source);
        }

        SoftReference<PlanarImage> lastBack = new SoftReference<PlanarImage>(null);
        SoftReference<PlanarImage> mask = new SoftReference<PlanarImage>(null);

        int mask_count = 0;

        private double last_radius = 0;
        private double last_fuzz = 0;

        public PlanarImage setFront() {
            if (lastBack.get() != back || mask.get() == null || depth != last_radius || fuzz != last_fuzz) {
                RenderedImage singleChannel;
                if (back.getColorModel().getNumComponents() == 3) {
                    double[][] yChannel = new double[][]{{ColorScience.Wr, ColorScience.Wg, ColorScience.Wb, 0}};

                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource( back );
                    pb.add( yChannel );
                    singleChannel = JAI.create("BandCombine", pb, null);
                } else
                    singleChannel = back;

                BorderExtender copyExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
                RenderingHints extenderHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, copyExtender);

                PlanarImage maskImage = new FastBilateralFilterOpImage(singleChannel,
                                                                       JAIContext.fileCacheHint,
                                                                       (float) (depth * scale), 0.1f);

//                ParameterBlock pb = new ParameterBlock();
//                pb.addSource(maskImage);
//                pb.add(new int[]{1});
//                maskImage = JAI.create("bandselect", pb, null);

                if (true) {
                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource(maskImage);
                    pb.add(new int[]{0});
                    RenderedOp bfMask = JAI.create("bandselect", pb, null);

                    KernelJAI kernel = Functions.getGaussKernel(10 * fuzz * scale);
                    pb = new ParameterBlock();
                    pb.addSource(bfMask);
                    pb.add(kernel);
                    RenderedOp blurredMask = JAI.create("LCSeparableConvolve", pb, extenderHints);

                    pb = new ParameterBlock();
                    pb.addSource( maskImage );
                    pb.addSource( blurredMask );
                    maskImage = JAI.create("BandMerge", pb, null);
                } else {
                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource(maskImage);
                    pb.add(new int[]{0});
                    maskImage = JAI.create("bandselect", pb, null);

                    KernelJAI kernel = Functions.getGaussKernel(10 * fuzz * scale);
                    pb = new ParameterBlock();
                    pb.addSource(maskImage);
                    pb.add(kernel);
                    maskImage = JAI.create("LCSeparableConvolve", pb, extenderHints);
                }

                last_radius = fuzz;
                last_fuzz = detail;

                mask = new SoftReference<PlanarImage>(maskImage);
                lastBack = new SoftReference<PlanarImage>(back);
            }

            return new HDROpImage2(back, mask.get(), shadows, highlights, detail, null);
        }
    }

    protected void updateOp(Transform op) {
        op.update();
    }

    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new ToneMaperTransform(source);
    }

    public OperationType getType() {
        return type;
    }
}
