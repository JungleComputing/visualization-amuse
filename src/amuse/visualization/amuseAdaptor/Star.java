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
    private final VecF4 color;
    private final VecF3 location;
    private final float radius;
    private final Model model;

    public Star(Model baseModel, VecF3 location, double radius, double luminosity) {
        super(new Material(), vertex_format.TRIANGLES);
        model = baseModel;
        this.location = location;

        color = Astrophysics.starColor(luminosity, radius);
        this.radius = Astrophysics.starToScreenRadius(radius);
    }

    @Override
    public void draw(GL3 gl, Program program, MatF4 MVMatrix) {
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
