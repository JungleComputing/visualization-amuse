package nl.esciencecenter.visualization.amuse.planetformation.data;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.openglCommon.datastructures.Material;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.math.MatrixFMath;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.math.VecF4;
import nl.esciencecenter.visualization.openglCommon.math.VectorFMath;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.shaders.Program;

public class Star {
    private final static AmuseSettings settings              = AmuseSettings
                                                                     .getInstance();

    private final Model                baseModel;
    private final VecF3                rawLocation;
    private final VecF3                velocity;
    private final VecF4                color;
    private final float                radius;

    private final VecF4                haloColor;

    private boolean                    initialized, interpolated;

    private VecF3[]                    bezierPoints;
    private VecF4[]                    interpolatedColors;
    private final Material[]           interpolatedMaterials = new Material[settings
                                                                     .getBezierInterpolationSteps()];
    private final float[]              interpolatedRadii     = new float[settings
                                                                     .getBezierInterpolationSteps()];

    private VecF3                      processedLocation;

    private Star                       interpolationStar;

    public Star(Model baseModel, VecF3 location, VecF3 velocity,
            double luminosity, double radius) {
        this.baseModel = baseModel;
        this.rawLocation = location;
        this.velocity = velocity;
        this.color = Astrophysics.starColor(luminosity, radius);
        this.radius = Astrophysics.starToScreenRadius(radius);

        this.haloColor = new VecF4(color);
        final float haloAlpha = (float) (0.5f - (this.radius / Astrophysics.STAR_RADIUS_AT_1000_SOLAR_RADII));
        this.haloColor.set(3, haloAlpha);

        initialized = false;
        interpolated = false;
    }

    public void init() {
        if (!initialized) {
            this.processedLocation = Astrophysics
                    .auLocationToScreenCoord(rawLocation);

            if (interpolationStar == null) {
                interpolated = false;
            } else {
                if (!initialized) {
                    int steps = settings.getBezierInterpolationSteps();

                    bezierPoints = VectorFMath.bezierCurve(steps, rawLocation,
                            velocity, interpolationStar.velocity,
                            interpolationStar.rawLocation);
                    interpolatedColors = VectorFMath.interpolateColors(steps,
                            color, interpolationStar.color);

                    float rstep = (interpolationStar.radius - radius) / steps;
                    for (int i = 0; i < steps; i++) {
                        bezierPoints[i] = Astrophysics
                                .auLocationToScreenCoord(bezierPoints[i]);
                        interpolatedMaterials[i] = new Material(
                                interpolatedColors[i], interpolatedColors[i],
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
        program.setUniformMatrix("ScaleMatrix", MatrixFMath.scale(radius));

        MatF4 newM = MVMatrix.mul(MatrixFMath.translate(processedLocation));
        program.setUniformMatrix("MVMatrix", newM);

        program.setUniformVector("Color", color);
        program.setUniformVector("HaloColor", haloColor);

        try {
            program.use(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
        baseModel.draw(gl, program);
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix, int step) {
        if (!initialized) {
            init();
        }

        if (interpolated) {
            program.setUniformMatrix("ScaleMatrix",
                    MatrixFMath.scale(interpolatedRadii[step]));

            MVMatrix = MVMatrix.mul(MatrixFMath.translate(bezierPoints[step]));
            program.setUniformMatrix("MVMatrix", MVMatrix);

            program.setUniformVector("HaloColor", interpolatedColors[step]);

            try {
                program.use(gl);
            } catch (UninitializedException e) {
                e.printStackTrace();
            }
            baseModel.draw(gl, program);
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
