package amuse.visualization.amuseAdaptor;

import java.util.HashMap;

import javax.media.opengl.GL;
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
import amuse.visualization.amuseAdaptor.exceptions.KeyframeUnavailableException;

public class Star2 extends Model {
    private static final AmuseSettings settings                 = AmuseSettings.getInstance();

    private HashMap<Integer, VecF3>    locations;
    private HashMap<Integer, VecF3>    velocities;
    private HashMap<Integer, Float>    radii;
    private HashMap<Integer, VecF4>    colors;

    private int                        currentKeyFrame          = -1;
    private int                        currentInterpolatedFrame = -1;

    private VecF3[]                    bezierPoints;

    private final Model                model;

    private boolean                    interpolationAvailable   = false;
    private static boolean             shouldInterpolate        = settings.getBezierInterpolation();

    public Star2(Model baseModel) {
        super(new Material(), vertex_format.TRIANGLES);
        this.model = baseModel;

        locations = new HashMap<Integer, VecF3>();
        velocities = new HashMap<Integer, VecF3>();
        radii = new HashMap<Integer, Float>();
        colors = new HashMap<Integer, VecF4>();
    }

    public void addKeyframe(int currentFrame, float luminosity, float radius, VecF3 location, VecF3 velocity) {
        this.colors.put(currentFrame, Astrophysics.starColor(luminosity, radius));
        this.radii.put(currentFrame, Astrophysics.starToScreenRadius(radius));
        this.locations.put(currentFrame, location);
        this.velocities.put(currentFrame, velocity);
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

                interpolationAvailable = true;
            } else if (locations.containsKey(currentFrame)) {
                this.currentKeyFrame = currentFrame;

                interpolationAvailable = false;
            } else {
                throw new KeyframeUnavailableException("Stars at " + currentFrame + "not available");
            }
        }
    }

    public void setCurrentInterpolatedFrame(int currentFrame) {
        this.currentInterpolatedFrame = currentFrame;
    }

    @Override
    public void draw(GL3 gl, Program program, MatF4 MVMatrix) {
        program.setUniformVector("DiffuseMaterial", colors.get(currentKeyFrame));
        program.setUniformVector("AmbientMaterial", colors.get(currentKeyFrame));
        program.setUniformVector("SpecularMaterial", colors.get(currentKeyFrame));

        final VecF4 haloColor = new VecF4(colors.get(currentKeyFrame));
        final float haloAlpha = (float) (0.5f - (radii.get(currentKeyFrame) / Astrophysics.STAR_RADIUS_AT_1000_SOLAR_RADII));
        haloColor.set(3, haloAlpha);

        program.setUniformVector("HaloColor", haloColor);

        if (interpolationAvailable) {
            MVMatrix = MVMatrix.mul(MatrixFMath.translate(bezierPoints[currentInterpolatedFrame]));
        } else {
            MVMatrix = MVMatrix.mul(MatrixFMath.translate(locations.get(currentKeyFrame)));
        }
        MVMatrix = MVMatrix.mul(MatrixFMath.scale(radii.get(currentKeyFrame)));
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

    @Override
    public void init(GL3 gl) {
        model.init(gl);
    }
}
