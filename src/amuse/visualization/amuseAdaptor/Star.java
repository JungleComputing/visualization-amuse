package amuse.visualization.amuseAdaptor;

import javax.media.opengl.GL3;

import openglCommon.datastructures.Material;
import openglCommon.math.MatF4;
import openglCommon.math.MatrixFMath;
import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.math.VectorFMath;
import openglCommon.models.Model;
import openglCommon.shaders.Program;
import amuse.visualization.AmuseSettings;

public class Star {
    private final static AmuseSettings settings              = AmuseSettings.getInstance();

    private final Model                baseModel;
    private final VecF3                rawLocation;
    private final VecF3                velocity;
    private final VecF4                color;
    private final float                radius;

    private final Material             material;
    private final VecF4                haloColor;

    private boolean                    initialized, interpolated;

    private VecF3[]                    bezierPoints;
    private VecF4[]                    interpolatedColors;
    private final Material[]           interpolatedMaterials = new Material[settings.getBezierInterpolationSteps()];
    private final float[]              interpolatedRadii     = new float[settings.getBezierInterpolationSteps()];

    private VecF3                      processedLocation;

    private Star                       interpolationStar;

    public Star(Model baseModel, VecF3 location, VecF3 velocity, double luminosity, double radius) {
        this.baseModel = baseModel;
        this.rawLocation = location;
        this.velocity = velocity;
        this.color = Astrophysics.starColor(luminosity, radius);
        this.radius = Astrophysics.starToScreenRadius(radius);

        this.material = new Material(color, color, color);

        this.haloColor = new VecF4(color);
        final float haloAlpha = (float) (0.5f - (this.radius / Astrophysics.STAR_RADIUS_AT_1000_SOLAR_RADII));
        this.haloColor.set(3, haloAlpha);

        initialized = false;
        interpolated = false;
    }

    public void init() {
        if (!initialized) {
            this.processedLocation = Astrophysics.locationToScreenCoord(rawLocation);

            if (interpolationStar == null) {
                interpolated = false;
            } else {
                if (!initialized) {
                    int steps = settings.getBezierInterpolationSteps();

                    bezierPoints = VectorFMath.bezierCurve(steps, rawLocation, velocity, interpolationStar.velocity,
                            interpolationStar.rawLocation);
                    interpolatedColors = VectorFMath.interpolateColors(steps, color, interpolationStar.color);

                    float rstep = (interpolationStar.radius - radius) / steps;
                    for (int i = 0; i < steps; i++) {
                        bezierPoints[i] = Astrophysics.locationToScreenCoord(bezierPoints[i]);
                        interpolatedMaterials[i] = new Material(interpolatedColors[i], interpolatedColors[i],
                                interpolatedColors[i]);
                        interpolatedRadii[i] = radius + (rstep * i);
                    }

                    interpolated = true;
                }
            }

            initialized = true;
        }
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix) {
        if (!initialized) {
            init();
        }

        baseModel.setMaterial(material);
        baseModel.setScale(radius);

        MVMatrix = MVMatrix.mul(MatrixFMath.translate(processedLocation));

        program.setUniformVector("HaloColor", haloColor);

        baseModel.draw(gl, program, MVMatrix);
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix, int step) {
        if (!initialized) {
            init();
        }

        if (interpolated) {
            baseModel.setMaterial(interpolatedMaterials[step]);
            baseModel.setScale(interpolatedRadii[step]);

            MVMatrix = MVMatrix.mul(MatrixFMath.translate(bezierPoints[step]));

            program.setUniformVector("HaloColor", interpolatedColors[step]);

            baseModel.draw(gl, program, MVMatrix);
        } else {
            draw(gl, program, MVMatrix);
        }
    }

    public VecF4 getColor() {
        if (!initialized) {
            init();
        }
        return color;
    }

    public VecF3 getLocation() {
        if (!initialized) {
            init();
        }
        return processedLocation;
    }

    public float getRadius() {
        if (!initialized) {
            init();
        }
        return radius;
    }

    public VecF4 getColor(int step) {
        if (!initialized) {
            init();
        }

        if (interpolated) {
            return interpolatedColors[step];
        } else {
            return color;
        }
    }

    public VecF3 getLocation(int step) {
        if (!initialized) {
            init();
        }

        if (interpolated) {
            return bezierPoints[step];
        } else {
            return processedLocation;
        }
    }

    public float getRadius(int step) {
        if (!initialized) {
            init();
        }

        if (interpolated) {
            return interpolatedRadii[step];
        } else {
            return radius;
        }
    }

    public void setInterpolationStar(Star otherStar) {
        this.interpolationStar = otherStar;
        this.initialized = false;
    }
}
