package amuse.visualization.amuseAdaptor;

import javax.media.opengl.GL3;

import openglCommon.datastructures.Material;
import openglCommon.math.MatF4;
import openglCommon.math.MatrixFMath;
import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.models.Model;
import openglCommon.shaders.Program;

public class Star {
    public final Model baseModel;
    public final VecF3 location;
    public final VecF3 velocity;
    public final VecF4 color;
    public final float radius;

    private final Material material;
    private final VecF4 haloColor;

    public Star(Model baseModel, VecF3 location, VecF3 velocity, double luminosity, double radius) {
        this.baseModel = baseModel;
        this.location = Astrophysics.locationToScreenCoord(location);
        this.velocity = velocity;
        this.color = Astrophysics.starColor(luminosity, radius);
        this.radius = Astrophysics.starToScreenRadius(radius);

        this.material = new Material(color, color, color);

        this.haloColor = new VecF4(color);
        final float haloAlpha = (float) (0.5f - (radius / Astrophysics.STAR_RADIUS_AT_1000_SOLAR_RADII));
        this.haloColor.set(3, haloAlpha);
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix) {
        baseModel.setMaterial(material);
        baseModel.setScale(radius);

        MVMatrix = MVMatrix.mul(MatrixFMath.translate(location));

        program.setUniformVector("HaloColor", haloColor);

        baseModel.draw(gl, program, MVMatrix);
    }
}
