package amuse.visualization.amuseAdaptor;

import openglCommon.math.VecF3;
import openglCommon.math.VectorFMath;
import amuse.visualization.AmuseSettings;

public class Gas {
    private final static AmuseSettings settings = AmuseSettings.getInstance();

    public VecF3 rawLocation, processedLocation;
    public final VecF3 velocity;
    public final float energy;

    public VecF3[] bezierPoints;
    public final float[] interpolatedEnergy = new float[settings.getBezierInterpolationSteps()];

    private boolean initialized;

    public Gas(VecF3 location, VecF3 velocity, double energy) {
        this.rawLocation = location;
        this.velocity = velocity;
        this.energy = (float) energy;
    }

    public void init() {
        if (!initialized) {
            this.processedLocation = Astrophysics.locationToScreenCoord(rawLocation);

            initialized = true;
        }
    }

    public void init(Gas otherGas) {
        if (!initialized) {
            int steps = settings.getBezierInterpolationSteps();

            bezierPoints = VectorFMath.bezierCurve(steps, rawLocation, velocity, otherGas.velocity,
                    otherGas.rawLocation);

            float estep = (otherGas.energy - energy) / steps;
            for (int i = 0; i < steps; i++) {
                bezierPoints[i] = Astrophysics.locationToScreenCoord(bezierPoints[i]);
                interpolatedEnergy[i] = energy + (estep * i);
            }

            initialized = true;
        }
    }
}