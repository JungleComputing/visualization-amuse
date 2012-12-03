package nl.esciencecenter.visualization.amuse.planetformation.data;

import openglCommon.math.VecF3;
import openglCommon.scenegraph.OctreeElement;

public class AmuseGasOctreeElement extends OctreeElement {
    private final double u;
    private final double mass;

    public AmuseGasOctreeElement(VecF3 center, double u, double mass) {
        super(center);
        this.u = u;
        this.mass = mass;
    }

    public double getEnergy() {
        return u;
    }

    public double getMass() {
        return mass;
    }
}
