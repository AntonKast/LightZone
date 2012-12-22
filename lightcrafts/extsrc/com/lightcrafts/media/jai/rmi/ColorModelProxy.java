/*
 * $RCSfile: ColorModelProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:49 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.lightcrafts.mediax.jai.FloatDoubleColorModel;

/**
 * This class is a serializable proxy for a ColorModel from which the
 * ColorModel may be reconstituted.
 *
 *
 * @since EA3
 */
public class ColorModelProxy implements Serializable {
    /** Flag indicating that the ColorSpace is unknown. */
    private static final int COLORSPACE_UNKNOWN = 0;

    /**
     * Flag indicating that the ColorSpace is one of those of which an
     * instance may be obtained using ColorSpace.getInstance() with one
     * of the pre-defined constants ColorSpace.CS_*.
     */
    private static final int COLORSPACE_PREDEFINED = 1;

    /** Flag indicating that the ColorSpace is an ICC_ColorSpace. */
    private static final int COLORSPACE_ICC = 2;

    /** Flag indicating that the ColorModel is null. */
    private static final int COLORMODEL_NULL = 0;

    /** Flag indicating that the ColorModel is a FloatDoubleColorModel. */
    private static final int COLORMODEL_FLOAT_DOUBLE_COMPONENT = 1;

    /** Flag indicating that the ColorModel is a ComponentColorModel. */
    private static final int COLORMODEL_COMPONENT = 2;

    /** Flag indicating that the ColorModel is a IndexColorModel. */
    private static final int COLORMODEL_INDEX = 3;

    /** Flag indicating that the ColorModel is a DirectColorModel. */
    private static final int COLORMODEL_DIRECT = 4;

    /** The ColorModel. */
    private transient ColorModel colorModel = null;

    /**
     * Returns an array of length one containing the pre-defined
     * ColorSpace.CS_* colorspace which equals the parameter ColorSpace
     * or null if it does not equal any of the pre-defined ColorSpaces.
     */
    private static int[] getPredefinedColorSpace(ColorSpace cs) {
        // Initialize an array of the pre-defined ColorSpaces.
        int[] colorSpaces =
            new int[] {ColorSpace.CS_CIEXYZ, ColorSpace.CS_GRAY,
                           ColorSpace.CS_LINEAR_RGB, ColorSpace.CS_PYCC,
                           ColorSpace.CS_sRGB};

        // Return the pre-defined index if the parameter is one of these.
        for(int i = 0; i < colorSpaces.length; i++) {
	    try {
		if(cs.equals(ColorSpace.getInstance(colorSpaces[i]))) {
		    return new int[] {colorSpaces[i]};
		}
	    } catch (Throwable e) {
		// Profile not found ; resilent.
	    }
        }

        // Try to find a similar ColorSpace.
        int numComponents = cs.getNumComponents();
        int type = cs.getType();
        if(numComponents == 1 && type == ColorSpace.TYPE_GRAY) {
            return new int[] {ColorSpace.CS_GRAY};
        } else if(numComponents == 3) {
            if(type == ColorSpace.TYPE_RGB) {
                return new int[] {ColorSpace.CS_sRGB};
            } else if(type == ColorSpace.TYPE_XYZ) {
                return new int[] {ColorSpace.CS_CIEXYZ};
            }
        }

        // Unknown type - too bad!
        return null;
    }

    /**
     * Serialize the parameter ColorSpace object.
     */
    private static boolean serializeColorSpace(ColorSpace cs,
                                               ObjectOutputStream out)
        throws IOException {
            int[] colorSpaceType = null;
            if(!(cs instanceof ICC_ColorSpace) &&
               (colorSpaceType = getPredefinedColorSpace(cs)) == null) {
                out.writeInt(COLORSPACE_UNKNOWN);
                out.writeInt(cs.getNumComponents());
                return false;
            }

            if(cs instanceof ICC_ColorSpace) {
                out.writeInt(COLORSPACE_ICC);
                ((ICC_ColorSpace)cs).getProfile().write(out);
            } else {
                out.writeInt(COLORSPACE_PREDEFINED);
                out.writeInt(colorSpaceType[0]);
            }

            return true;
    }

    /**
     * Derialize the parameter ColorSpace object.
     */
    private static ColorSpace deserializeColorSpace(ObjectInputStream in)
        throws IOException {
            ColorSpace cs = null;
            int colorSpaceType = in.readInt();
            if(colorSpaceType == COLORSPACE_ICC) {
                cs = new ICC_ColorSpace(ICC_Profile.getInstance(in));
            } else if(colorSpaceType == COLORSPACE_PREDEFINED) {
                cs = ColorSpace.getInstance(in.readInt());
            } else if(colorSpaceType == COLORSPACE_UNKNOWN) {
                // Create probable ColorSpaces for 1- and 3-band cases.
                switch(in.readInt()) {
                case 1: // gray scale
                    cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                    break;
                case 3: // sRGB
                    cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                    break;
                default: // Unknown
                    cs = null; // to be explicit ...
                }
            }

            return cs;
    }

    /**
      * Constructs a <code>ColorModelProxy</code> from a
      * <code>ColorModel</code>.
      *
      * @param source The <code>ColorModel</code> to be serialized.
      */
    public ColorModelProxy(ColorModel source) {
        colorModel = source;
    }

    /**
      * Retrieves the associated <code>ColorModel</code>.
      * @return The (perhaps reconstructed) <code>ColorModel</code>.
      */
    public ColorModel getColorModel() {
        return colorModel;
    }

    /**
      * Serialize the <code>ColorModelProxy</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Write serialized form to the stream.
        if(colorModel == null) {
            out.writeInt(COLORMODEL_NULL);
        } else if(colorModel instanceof ComponentColorModel) {
            ComponentColorModel cm = (ComponentColorModel)colorModel;
            int type = COLORMODEL_COMPONENT;
            if(colorModel instanceof FloatDoubleColorModel) {
                type = COLORMODEL_FLOAT_DOUBLE_COMPONENT;
            }
            out.writeInt(type);
            serializeColorSpace(cm.getColorSpace(), out); // ignore return
            if(type == COLORMODEL_COMPONENT) {
                out.writeObject(cm.getComponentSize());
            }
            out.writeBoolean(cm.hasAlpha());
            out.writeBoolean(cm.isAlphaPremultiplied());
            out.writeInt(cm.getTransparency());
            // Create a SampleModel to get the transferType. This is
            // absurd but is the only apparent way to retrieve this value.
            SampleModel sm = cm.createCompatibleSampleModel(1, 1);
            out.writeInt(sm.getTransferType());
        } else if(colorModel instanceof IndexColorModel) {
            IndexColorModel cm = (IndexColorModel)colorModel;
            out.writeInt(COLORMODEL_INDEX);
            int size = cm.getMapSize();
            int[] cmap = new int[size];
            cm.getRGBs(cmap);
            out.writeInt(cm.getPixelSize());
            out.writeInt(size);
            out.writeObject(cmap);
            out.writeBoolean(cm.hasAlpha());
            out.writeInt(cm.getTransparentPixel());
            // Create a SampleModel to get the transferType. This is
            // absurd but is the only apparent way to retrieve this value.
            SampleModel sm = cm.createCompatibleSampleModel(1, 1);
            out.writeInt(sm.getTransferType());
        } else if(colorModel instanceof DirectColorModel) {
            DirectColorModel cm = (DirectColorModel)colorModel;
            out.writeInt(COLORMODEL_DIRECT);
            boolean csSerialized =
                serializeColorSpace(cm.getColorSpace(), out);
            if(!csSerialized) {
                out.writeBoolean(cm.hasAlpha());
            }
            out.writeInt(cm.getPixelSize());
            out.writeInt(cm.getRedMask());
            out.writeInt(cm.getGreenMask());
            out.writeInt(cm.getBlueMask());
            if(csSerialized || cm.hasAlpha()) {
                out.writeInt(cm.getAlphaMask());
            }
            if(csSerialized) {
                out.writeBoolean(cm.isAlphaPremultiplied());
                // Create a SampleModel to get the transferType. This is
                // absurd but is the only apparent way to retrieve this
                // value.
                SampleModel sm = cm.createCompatibleSampleModel(1, 1);
                out.writeInt(sm.getTransferType());
            }
        } else {
            throw new RuntimeException(JaiI18N.getString("ColorModelProxy0"));
        }
    }

    /**
      * Deserialize the <code>ColorModelProxy</code>.
      *
      * @param out The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // Read serialized form from the stream.
        ColorSpace cs = null;

        // Switch on first int which is a flag indicating the class.
        switch((int)in.readInt()) {
        case COLORMODEL_NULL:
            colorModel = null;
            break;
        case COLORMODEL_FLOAT_DOUBLE_COMPONENT:
            if((cs = deserializeColorSpace(in)) == null) {
                colorModel = null;
                return;
            }
            colorModel =
                new FloatDoubleColorModel(cs,
                                          in.readBoolean(),
                                          in.readBoolean(),
                                          in.readInt(), in.readInt());
            break;
        case COLORMODEL_COMPONENT:
            if((cs = deserializeColorSpace(in)) == null) {
                colorModel = null;
                return;
            }
            colorModel =
                new ComponentColorModel(cs, (int[])in.readObject(),
                                        in.readBoolean(), in.readBoolean(),
                                        in.readInt(), in.readInt());
            break;
        case COLORMODEL_INDEX:
            colorModel =
                new IndexColorModel(in.readInt(), in.readInt(),
                                    (int[])in.readObject(), 0,
                                    in.readBoolean(), in.readInt(),
                                    in.readInt());
            break;
        case COLORMODEL_DIRECT:
            if((cs = deserializeColorSpace(in)) != null) {
                colorModel =
                    new DirectColorModel(cs, in.readInt(), in.readInt(),
                                         in.readInt(), in.readInt(),
                                         in.readInt(), in.readBoolean(),
                                         in.readInt());
            } else if(in.readBoolean()) {
                colorModel =
                    new DirectColorModel(in.readInt(), in.readInt(),
                                         in.readInt(), in.readInt(),
                                         in.readInt());
            } else {
                colorModel =
                    new DirectColorModel(in.readInt(), in.readInt(),
                                         in.readInt(), in.readInt());
            }
            break;
        default:
            // NB: Should never get here.
            throw new RuntimeException(JaiI18N.getString("ColorModelProxy1"));
        }
    }
}
