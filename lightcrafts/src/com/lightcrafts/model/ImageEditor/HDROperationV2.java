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
public class HDROperationV2 extends BlendedOperation {
    private double depth = 32;
    private double highlights = 1.0;
    private double detail = 0.5;
    private double shadows = 1.0;
    private double fuzz = 0.2;

    private final static String HIGHLIGHTS = "Highlights";
    private final static String DETAIL = "Detail";
    private final static String SHADOWS = "Shadows";
    private final static String DEPTH = "Depth";
    private final static String FUZZ = "Fuzz";

    public HDROperationV2(Rendering rendering, OperationType type) {
        super(rendering, type);

        DecimalFormat format = new DecimalFormat("0.00");

        addSliderKey(SHADOWS);
        setSliderConfig(SHADOWS, new SliderConfig(0, 10, shadows, .05, false, format));

        addSliderKey(HIGHLIGHTS);
        setSliderConfig(HIGHLIGHTS, new SliderConfig(0.1, 10, highlights, .05, true, format));

        addSliderKey(DETAIL);
        setSliderConfig(DETAIL, new SliderConfig(0, 4, detail, .05, false, format));

        if (type == typeV3 || type == typeV4) {
            addSliderKey(DEPTH);
            setSliderConfig(DEPTH, new SliderConfig(8, 64, depth, .05, false, format));
        }

        if (type == typeV4) {
            addSliderKey(FUZZ);
            setSliderConfig(FUZZ, new SliderConfig(0.1, 1, fuzz, .05, false, format));
        }
    }

    public boolean neutralDefault() {
        return false;
    }

    static final OperationType typeV2 = new OperationTypeImpl("Tone Mapper V2");
    static final OperationType typeV3 = new OperationTypeImpl("Tone Mapper V3");
    static final OperationType typeV4 = new OperationTypeImpl("Tone Mapper V4");

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

                ParameterBlock pb = new ParameterBlock();
                pb.addSource(maskImage);
                pb.add(new int[]{0});
                maskImage = JAI.create("bandselect", pb, null);

                if (fuzz > 0.1) {
                    KernelJAI kernel = Functions.getGaussKernel(10 * (fuzz - 0.1) * scale);
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

            return new HDROpImage(back, mask.get(), shadows, highlights,2 * detail, null);
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
