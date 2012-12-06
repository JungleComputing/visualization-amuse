package nl.esciencecenter.visualization.amuse.planetformation.data;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;

public class Gas {
    private final static AmuseSettings settings = AmuseSettings.getInstance();

    private final VecF3                rawLocation;

    private VecF3                      processedLocation;
    private final float                energy;

    private boolean                    initialized, interpolated;

    public Gas(VecF3 location, VecF3 velocity, double energy) {
        this.rawLocation = location;
        this.energy = (float) energy;
    }

    public void init() {
        if (!initialized) {
            this.processedLocation = Astrophysics
                    .auLocationToScreenCoord(rawLocation);

            initialized = true;
            interpolated = false;
        }
    }

    public VecF3 getLocation() throws UninitializedException {
        if (!initialized || interpolated) {
            throw new UninitializedException();
        }
        return processedLocation;
    }

    public float getEnergy() throws UninitializedException {
        if (!initialized || interpolated) {
            throw new UninitializedException();
        }
        return energy;
    }
}