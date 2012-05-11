package amuse.visualization.amuseAdaptor;

import javax.media.opengl.GL3;

import openglCommon.datastructures.Material;
import openglCommon.exceptions.UninitializedException;
import openglCommon.math.MatF4;
import openglCommon.math.MatrixFMath;
import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.math.VectorFMath;
import openglCommon.models.Model;
import openglCommon.shaders.Program;
import amuse.visualization.AmuseSettings;

public class Star {
    private final static AmuseSettings settings = AmuseSettings.getInstance();

    public final Model baseModel;
    public final VecF3 rawLocation;
    public final VecF3 velocity;
    public final VecF4 color;
    public final float radius;

    private final Material material;
    private final VecF4 haloColor;

    private boolean initialized;

    private VecF3[] bezierPoints;
    private VecF4[] interpolatedColors;
    private final Material[] interpolatedMaterials = new Material[settings.getBezierInterpolationSteps()];
    private final float[] interpolatedRadii = new float[settings.getBezierInterpolationSteps()];

    private VecF3 processedLocation;

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
    }

    public void init() {
        if (!initialized) {
            this.processedLocation = Astrophysics.locationToScreenCoord(rawLocation);

            initialized = true;
        }
    }

    public void init(Star otherStar) {
        if (!initialized) {
            int steps = settings.getBezierInterpolationSteps();

            bezierPoints = VectorFMath.bezierCurve(steps, rawLocation, velocity, otherStar.velocity,
                    otherStar.rawLocation);
            interpolatedColors = VectorFMath.interpolateColors(steps, color, otherStar.color);

            float rstep = (otherStar.radius - radius) / steps;
            for (int i = 0; i < steps; i++) {
                bezierPoints[i] = Astrophysics.locationToScreenCoord(bezierPoints[i]);
                interpolatedMaterials[i] = new Material(interpolatedColors[i], interpolatedColors[i],
                        interpolatedColors[i]);
                interpolatedRadii[i] = radius + (rstep * i);
            }

            initialized = true;
        }
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix) throws UninitializedException {
        if (!initialized) {
            throw new UninitializedException();
        }

        baseModel.setMaterial(material);
        baseModel.setScale(radius);

        MVMatrix = MVMatrix.mul(MatrixFMath.translate(processedLocation));

        program.setUniformVector("HaloColor", haloColor);

        baseModel.draw(gl, program, MVMatrix);
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix, int interpolationStep) throws UninitializedException {
        if (!initialized) {
            throw new UninitializedException();
        }

        baseModel.setMaterial(interpolatedMaterials[interpolationStep]);
        baseModel.setScale(interpolatedRadii[interpolationStep]);

        MVMatrix = MVMatrix.mul(MatrixFMath.translate(bezierPoints[interpolationStep]));

        program.setUniformVector("HaloColor", interpolatedColors[interpolationStep]);

        baseModel.draw(gl, program, MVMatrix);
    }
}
