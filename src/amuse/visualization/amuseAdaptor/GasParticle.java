package amuse.visualization.amuseAdaptor;

import java.util.HashMap;

import openglCommon.math.VecF3;
import openglCommon.math.VectorFMath;
import amuse.visualization.AmuseSettings;
import amuse.visualization.amuseAdaptor.exceptions.KeyframeUnavailableException;

public class GasParticle {
    private static final AmuseSettings settings               = AmuseSettings.getInstance();

    private HashMap<Integer, VecF3>    locations;
    private HashMap<Integer, VecF3>    velocities;
    private HashMap<Integer, Float>    energies;

    private int                        currentKeyFrame        = -1;
    private VecF3[]                    bezierPoints;
    private float[]                    energyInterpolationPoints;

    private boolean                    interpolationAvailable = false;
    private static boolean             shouldInterpolate      = settings.getBezierInterpolation();

    public GasParticle() {
        locations = new HashMap<Integer, VecF3>();
        velocities = new HashMap<Integer, VecF3>();
        energies = new HashMap<Integer, Float>();
    }

    public void addKeyframe(int currentFrame, VecF3 location, VecF3 velocity, float energy) {
        this.locations.put(currentFrame, location);
        this.velocities.put(currentFrame, velocity);
        this.energies.put(currentFrame, energy);
    }

    public void setCurrentKeyFrame(int currentFrame) throws KeyframeUnavailableException {
        if (currentFrame != currentKeyFrame) {
            if (locations.containsKey(currentFrame) && locations.containsKey(currentFrame + 1) && shouldInterpolate) {
                this.currentKeyFrame = currentFrame;

                VecF3[] newBezierPoints = VectorFMath.bezierCurve(settings.getBezierInterpolationSteps(),
                        locations.get(currentFrame), velocities.get(currentFrame), velocities.get(currentFrame + 1),
                        locations.get(currentFrame + 1));

                bezierPoints = new VecF3[settings.getBezierInterpolationSteps()];
                for (int i = 0; i < settings.getBezierInterpolationSteps(); i++) {
                    bezierPoints[i] = Astrophysics.locationToScreenCoord(newBezierPoints[i].get(0),
                            newBezierPoints[i].get(1), newBezierPoints[i].get(2));
                }

                energyInterpolationPoints = new float[settings.getBezierInterpolationSteps()];
                for (int i = 0; i < settings.getBezierInterpolationSteps(); i++) {
                    energyInterpolationPoints[i] = energies.get(currentFrame)
                            + (energies.get(currentFrame + 1) - energies.get(currentFrame))
                            * (i / settings.getBezierInterpolationSteps());
                }

                interpolationAvailable = true;
            } else if (locations.containsKey(currentFrame)) {
                this.currentKeyFrame = currentFrame;

                interpolationAvailable = false;
            } else {
                throw new KeyframeUnavailableException("Gas at " + currentFrame + "not available");
            }
        }
    }

    public VecF3 getLocation(int interpolationStep) {
        if (interpolationAvailable) {
            return bezierPoints[interpolationStep];
        } else {
            return locations.get(currentKeyFrame);
        }
    }

    public float getEnergy(int interpolationStep) {
        if (interpolationAvailable) {
            return energyInterpolationPoints[interpolationStep];
        } else {
            return energies.get(currentKeyFrame);
        }
    }
}
