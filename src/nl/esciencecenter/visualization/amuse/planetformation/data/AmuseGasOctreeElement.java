package nl.esciencecenter.visualization.amuse.planetformation.data;

import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.scenegraph.OctreeElement;

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
