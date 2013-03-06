package nl.esciencecenter.visualization.amuse.planetformation.glue.visual;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.glue.Planet;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.math.MatrixFMath;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.math.VecF4;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.shaders.ShaderProgram;

public class PlanetModel {
    private final Model  baseModel;
    private final Planet gluePlanet;

    private VecF3        coords;
    private VecF4        color;
    private float        radius;

    private boolean      initialized;

    public PlanetModel(Model baseModel, Planet gluePlanet) {
        this.baseModel = baseModel;
        this.gluePlanet = gluePlanet;

        initialized = false;
    }

    public void init() {
        if (!initialized) {
            float[] rawCoords = gluePlanet.getCoordinates();
            float[] rawColor = gluePlanet.getColor();

            coords = new VecF3(rawCoords[0], rawCoords[1], rawCoords[2]);
            color = new VecF4(rawColor[0], rawColor[1], rawColor[2],
                    rawColor[3]);
            radius = gluePlanet.getRadius();

            initialized = true;
        }
    }

    public void draw(GL3 gl, ShaderProgram program, MatF4 MVMatrix) {
        if (!initialized) {
            init();
        }

        program.setUniformMatrix("ScaleMatrix", MatrixFMath.scale(radius));

        MatF4 newM = MVMatrix.mul(MatrixFMath.translate(coords));
        program.setUniformMatrix("MVMatrix", newM);

        program.setUniformVector("Color", color);

        try {
            program.use(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
        baseModel.draw(gl, program);
    }

    public VecF4 getColor() {
        if (!initialized) {
            init();
        }
        return color;
    }

    public float getRadius() {
        if (!initialized) {
            init();
        }

        return radius;
    }

    public VecF3 getLocation() {
        if (!initialized) {
            init();
        }
        return coords;
    }
}
