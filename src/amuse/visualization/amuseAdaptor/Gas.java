package amuse.visualization.amuseAdaptor;

import openglCommon.exceptions.UninitializedException;
import openglCommon.math.VecF3;
import openglCommon.math.VectorFMath;
import amuse.visualization.AmuseSettings;

public class Gas {
    private final static AmuseSettings settings = AmuseSettings.getInstance();

    private final VecF3 rawLocation;

    private VecF3 processedLocation;
    private final VecF3 velocity;
    private final float energy;

    private VecF3[] bezierPoints;
    private final float[] interpolatedEnergy = new float[settings.getBezierInterpolationSteps()];

    private boolean initialized, interpolated;

    public Gas(VecF3 location, VecF3 velocity, double energy) {
        this.rawLocation = location;
        this.velocity = velocity;
        this.energy = (float) energy;
    }

    public void init() {
        if (!initialized) {
            this.processedLocation = Astrophysics.locationToScreenCoord(rawLocation);

            initialized = true;
            interpolated = false;
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
            interpolated = true;
        }
    }

    public VecF3 getLocation() throws UninitializedException {
        if (!initialized || interpolated) {
            throw new UninitializedException();
        }
        return processedLocation;
    }

    public VecF3 getLocation(int step) throws UninitializedException {
        if (!initialized || !interpolated) {
            throw new UninitializedException();
        }
        return bezierPoints[step];
    }

    public float getEnergy() throws UninitializedException {
        if (!initialized || interpolated) {
            throw new UninitializedException();
        }
        return energy;
    }

    public float getEnergy(int step) throws UninitializedException {
        if (!initialized || !interpolated) {
            throw new UninitializedException();
        }
        return interpolatedEnergy[step];
    }
}