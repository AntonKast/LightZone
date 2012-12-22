/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.*;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.ui.editor.EditorMode;

public class SpotOperationImpl extends BlendedOperation implements SpotOperation {

    public SpotOperationImpl(Rendering rendering) {
        super(rendering, type);
    }

    public boolean neutralDefault() {
        return true;
    }

    public void setRegionInverted(boolean inverted) {
        // Inverted regions have no meaning for the Spot Tool
        // super.setRegionInverted(inverted);
    }

    static final OperationType type = new OperationTypeImpl("Spot");

    class Cloner extends BlendedTransform {
        Cloner(PlanarImage source) {
            super(source);
        }

        public PlanarImage setFront() {
            if (getRegion() != null)
                return CloneOperationImpl.buildCloner(getRegion(), rendering, back);
            else
                return back;
        }
    }

    public EditorMode getPreferredMode() {
        return EditorMode.REGION;
    }

    protected void updateOp(Transform op) {
        op.update();
    }

    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new Cloner(source);
    }

    public OperationType getType() {
        return type;
    }
}
