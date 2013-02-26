/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.ColorScience;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.mediax.jai.operator.MedianFilterDescriptor;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;
import java.text.DecimalFormat;

public class NoiseReductionOperation extends BlendedOperation {
    static final String DENOISE = "Amount";
    static final OperationType type = new OperationTypeImpl("Noise Reduction");
    private int denoiseLevel = 3;

    public NoiseReductionOperation(Rendering rendering) {
        super(rendering, type);
        colorInputOnly = true;

        this.addSliderKey(DENOISE);
        DecimalFormat format = new DecimalFormat("0");
        this.setSliderConfig(DENOISE, new SliderConfig(1, 10, denoiseLevel, 1, false, format));
    }

    public boolean neutralDefault() {
        return false;
    }

    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key == DENOISE && denoiseLevel != value) {
            denoiseLevel = (int) value;
        } else
            return;

        super.setSliderValue(key, value);
    }

    private class NoiseReduction extends BlendedTransform {
        NoiseReduction(PlanarImage source) {
            super(source);
        }

        RenderedOp denoiser;

        public PlanarImage setFront1() {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource( back );
            return JAI.create("BilateralFilter", pb, null);
        }

        public PlanarImage setFront() {
            ColorScience.YST yst = new ColorScience.YST();

            double[][] rgb2yst = yst.fromRGB(back.getSampleModel().getDataType());
            double[][] yst2rgb = yst.toRGB(back.getSampleModel().getDataType());

            ParameterBlock pb = new ParameterBlock();
            pb.addSource( back );
            pb.add( rgb2yst );
            RenderedOp ystImage = JAI.create("BandCombine", pb, JAIContext.noCacheHint);

            pb = new ParameterBlock();
            pb.addSource(ystImage);
            pb.add(new int[]{0});
            RenderedOp y = JAI.create("bandselect", pb, JAIContext.noCacheHint);

            pb = new ParameterBlock();
            pb.addSource(ystImage);
            pb.add(new int[]{1, 2});
            // NOTE: we cache this because the median filter is an area op that gets its input multiple times
            RenderedOp cc = JAI.create("bandselect", pb, null);

            RenderingHints mfHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            mfHints.add(JAIContext.noCacheHint);
            pb = new ParameterBlock();
            pb.addSource(cc);
            pb.add(MedianFilterDescriptor.MEDIAN_MASK_SQUARE); // X Shape seems to give the least artifacts
            pb.add(new Integer(Math.max(2 * (int) (denoiseLevel * scale) + 1, 3)));
            denoiser = JAI.create("MedianFilter", pb, mfHints);

            RenderingHints layoutHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, Functions.getImageLayout(ystImage));
            pb = new ParameterBlock();
            pb.addSource(y);
            pb.addSource(denoiser);
            layoutHints.add(JAIContext.noCacheHint);
            RenderedOp denoisedyst = JAI.create("BandMerge", pb, layoutHints);

            pb = new ParameterBlock();
            pb.addSource( denoisedyst );
            pb.add( yst2rgb );
            return JAI.create("BandCombine", pb, JAIContext.noCacheHint);
        }
    }

    protected void updateOp(Transform op) {
        op.update();
    }

    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new NoiseReduction(source);
    }

    public OperationType getType() {
        return type;
    }
}
