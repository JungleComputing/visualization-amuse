package amuse.visualization.amuseAdaptor;

import openglCommon.math.VecF3;
import openglCommon.scenegraph.OctreeElement;

public class AmuseGasOctreeElement extends OctreeElement {
    private final double u;

    public AmuseGasOctreeElement(VecF3 center, double u) {
        super(center);
        this.u = u;
    }

    public double getEnergy() {
        return u;
    }
}
