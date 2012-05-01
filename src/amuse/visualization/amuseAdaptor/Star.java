package amuse.visualization.amuseAdaptor;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import openglCommon.datastructures.Material;
import openglCommon.exceptions.UninitializedException;
import openglCommon.math.MatF4;
import openglCommon.math.MatrixFMath;
import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.models.Model;
import openglCommon.shaders.Program;

public class Star extends Model {
    public final static int STEPS = 10;

    private final VecF4 color;
    private VecF3 location;
    private final float radius;
    private final Model model;

    private boolean prediction_enabled = false;
    private VecF3 locationLast, locationNext;
    private VecF3 velocityLast, velocityNext;

    private VecF3[] bezierPoints;

    public Star(Model baseModel, VecF3 location, double radius, double luminosity) {
        super(new Material(), vertex_format.TRIANGLES);
        this.model = baseModel;
        this.location = location;

        this.color = Astrophysics.starColor(luminosity, radius);
        this.radius = Astrophysics.starToScreenRadius(radius);

        this.prediction_enabled = false;
    }

    public Star(Long key, Model baseModel, VecF3 locationLast, VecF3 locationNext, VecF3 velocityLast,
            VecF3 velocityNext, double radius, double luminosity) {
        super(new Material(), vertex_format.TRIANGLES);
        this.model = baseModel;
        this.locationLast = locationLast;
        this.locationNext = locationNext;

        this.velocityLast = velocityLast;
        this.velocityNext = velocityNext;

        this.color = Astrophysics.starColor(luminosity, radius);
        this.radius = Astrophysics.starToScreenRadius(radius);

        this.prediction_enabled = true;
        VecF3[] newBezierPoints = new VecF3[STEPS];
        for (int i = 0; i < STEPS; i++) {
            newBezierPoints[i] = new VecF3();
        }

        float t = 1f / STEPS;
        float temp = t * t;

        for (int coord = 0; coord < 3; coord++) {
            float p[] = new float[4];
            p[0] = locationLast.get(coord);
            p[1] = locationLast.add(velocityLast).get(coord);
            p[2] = locationNext.add(velocityNext.neg()).get(coord);
            p[3] = locationNext.get(coord);
            // p[2] = locationNext.get(coord);
            // p[3] = velocityNext.get(coord);

            // The algorithm itself begins here ==
            float f, fd, fdd, fddd, fdd_per_2, fddd_per_2, fddd_per_6;

            // I've tried to optimize the amount of
            // multiplications here, but these are exactly
            // the same formulas that were derived earlier
            // for f(0), f'(0)*t etc.
            f = p[0];
            fd = 3 * (p[1] - p[0]) * t;
            fdd_per_2 = 3 * (p[0] - 2 * p[1] + p[2]) * temp;
            fddd_per_2 = 3 * (3 * (p[1] - p[2]) + p[3] - p[0]) * temp * t;

            fddd = fddd_per_2 + fddd_per_2;
            fdd = fdd_per_2 + fdd_per_2;
            fddd_per_6 = fddd_per_2 * (1f / 3);

            for (int loop = 0; loop < STEPS; loop++) {
                newBezierPoints[loop].set(coord, f);

                f = f + fd + fdd_per_2 + fddd_per_6;
                fd = fd + fdd + fddd_per_2;
                fdd = fdd + fddd;
                fdd_per_2 = fdd_per_2 + fddd_per_2;
            }

            // drawBezierpoint(x[3]);
        }

        bezierPoints = new VecF3[STEPS];
        for (int i = 0; i < STEPS; i++) {
            bezierPoints[i] = Astrophysics.locationToScreenCoord(newBezierPoints[i].get(0), newBezierPoints[i].get(1),
                    newBezierPoints[i].get(2));
        }

        this.prediction_enabled = true;
    }

    @Override
    public void draw(GL3 gl, Program program, MatF4 MVMatrix) {
        drawStar(gl, program, MVMatrix);
    }

    public void draw2(GL3 gl, Program program, MatF4 MVMatrix, int predictionStep) {
        if (prediction_enabled) {
            drawStar2(gl, program, MVMatrix, predictionStep);
        } else {
            drawStar(gl, program, MVMatrix);
        }
    }

    private void drawStar(GL3 gl, Program program, MatF4 MVMatrix) {
        program.setUniformVector("DiffuseMaterial", color);
        program.setUniformVector("AmbientMaterial", color);
        program.setUniformVector("SpecularMaterial", color);

        final VecF4 haloColor = new VecF4(color);
        final float haloAlpha = (float) (0.5f - (radius / Astrophysics.STAR_RADIUS_AT_1000_SOLAR_RADII));
        haloColor.set(3, haloAlpha);

        program.setUniformVector("HaloColor", haloColor);

        MVMatrix = MVMatrix.mul(MatrixFMath.translate(location));
        MVMatrix = MVMatrix.mul(MatrixFMath.scale(radius));
        program.setUniformMatrix("MVMatrix", MVMatrix);

        try {
            program.use(gl);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }

        model.getVBO().bind(gl);

        program.linkAttribs(gl, model.getVBO().getAttribs());

        gl.glDrawArrays(GL.GL_TRIANGLES, 0, model.getNumVertices());
    }

    private void drawStar2(GL3 gl, Program program, MatF4 MVMatrix, int predictionStep) {
        program.setUniformVector("DiffuseMaterial", color);
        program.setUniformVector("AmbientMaterial", color);
        program.setUniformVector("SpecularMaterial", color);

        final VecF4 haloColor = new VecF4(color);
        final float haloAlpha = (float) (0.5f - (radius / Astrophysics.STAR_RADIUS_AT_1000_SOLAR_RADII));
        haloColor.set(3, haloAlpha);

        program.setUniformVector("HaloColor", haloColor);

        MVMatrix = MVMatrix.mul(MatrixFMath.translate(bezierPoints[predictionStep]));
        MVMatrix = MVMatrix.mul(MatrixFMath.scale(radius));
        program.setUniformMatrix("MVMatrix", MVMatrix);

        try {
            program.use(gl);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }

        model.getVBO().bind(gl);

        program.linkAttribs(gl, model.getVBO().getAttribs());

        gl.glDrawArrays(GL.GL_TRIANGLES, 0, model.getNumVertices());
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

    @Override
    public void init(GL3 gl) {
        model.init(gl);
    }
}
