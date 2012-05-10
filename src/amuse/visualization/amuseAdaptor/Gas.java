package amuse.visualization.amuseAdaptor;

import openglCommon.math.VecF3;

public class Gas {
    public final VecF3 location;
    public final VecF3 velocity;
    public final float energy;

    public Gas(VecF3 location, VecF3 velocity, double energy) {
        this.location = location;
        this.velocity = velocity;
        this.energy = (float) energy;
    }
}