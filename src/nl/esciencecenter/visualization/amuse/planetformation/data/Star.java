package nl.esciencecenter.visualization.amuse.planetformation.data;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import openglCommon.datastructures.Material;
import openglCommon.math.MatF4;
import openglCommon.math.MatrixFMath;
import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.models.Model;
import openglCommon.shaders.Program;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Star {
    private final static Logger        logger   = LoggerFactory
                                                        .getLogger(Star.class);
    private final static AmuseSettings settings = AmuseSettings.getInstance();

    private final Model                baseModel;
    private final VecF4                color;
    private final float                radius;

    private final Material             material;
    private final VecF4                haloColor;

    private final VecF3                location;

    public Star(Model baseModel, VecF3 location, double luminosity,
            double radius) {
        this.baseModel = baseModel;
        this.location = location;
        this.color = Astrophysics.starColor(luminosity, radius);
        this.radius = Astrophysics.starToScreenRadius(radius);

        this.material = new Material(color, color, color);

        this.haloColor = new VecF4(color);
        final float haloAlpha = (float) (0.5f - (this.radius / Astrophysics.STAR_RADIUS_AT_1000_SOLAR_RADII));
        this.haloColor.set(3, haloAlpha);
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix) {
        baseModel.setMaterial(material);
        baseModel.setScale(radius);

        MatF4 MyMVMatrix = MVMatrix.mul(MatrixFMath.translate(location));

        program.setUniformVector("HaloColor", haloColor);

        baseModel.draw(gl, program, MyMVMatrix);
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix, int step) {
        draw(gl, program, MVMatrix);
    }

    public VecF4 getColor() {
        return color;
    }

    public VecF3 getLocation() {
        return location;
    }

    public float getRadius() {
        return radius;
    }

    public VecF4 getColor(int step) {
        return color;
    }

    public VecF3 getLocation(int step) {
        return location;
    }

    public float getRadius(int step) {
        return radius;
    }
}
