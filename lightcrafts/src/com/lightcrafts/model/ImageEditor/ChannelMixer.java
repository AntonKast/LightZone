/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.ColorScience;

import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;

public class ChannelMixer extends BlendedOperation {
    static final String Red = "Red";
    static final String Green = "Green";
    static final String Blue = "Blue";

    public ChannelMixer(Rendering rendering) {
        super(rendering, type);
        colorInputOnly = true;

        addSliderKey(Red);
        addSliderKey(Green);
        addSliderKey(Blue);

        // Fabio: pick the right number format
        DecimalFormat format = new DecimalFormat("0.00");

        setSliderConfig(Red, new SliderConfig(-2, 2, red, .01, false, format));
        setSliderConfig(Green, new SliderConfig(-2, 2, green, .01, false, format));
        setSliderConfig(Blue, new SliderConfig(-2, 2, blue, .01, false, format));
    }

    public boolean neutralDefault() {
        return false;
    }

    static final OperationType type = new OperationTypeImpl("Channel Mixer");

    private double red = ColorScience.Wr;
    private double green = ColorScience.Wg;
    private double blue = ColorScience.Wb;

    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key == Red && red != value) {
            red = value;
        } else if (key == Green && green != value) {
            green = value;
        } else if (key == Blue && blue != value) {
            blue = value;
        } else
            return;
        
        super.setSliderValue(key, value);
    }

    private class ChannelMixerTransform extends BlendedTransform {
        ChannelMixerTransform(PlanarImage source) {
            super(source);
        }

        public PlanarImage setFront() {
            double[][] transform = {
                { red, green, blue, 0 },
                { red, green, blue, 0 },
                { red, green, blue, 0 }
            };

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(back);
            pb.add(transform);
            return JAI.create("BandCombine", pb, JAIContext.noCacheHint);  // Desaturate, single banded
        }
    }

    protected void updateOp(Transform op) {
        op.update();
    }

    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new ChannelMixerTransform(source);
    }

    public OperationType getType() {
        return type;
    }
}
