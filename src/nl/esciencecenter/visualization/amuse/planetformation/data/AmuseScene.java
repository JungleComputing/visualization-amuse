package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.openglCommon.datastructures.GLSLAttrib;
import nl.esciencecenter.visualization.openglCommon.datastructures.VBO;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.shaders.Program;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseScene {
    private final static AmuseSettings  settings    = AmuseSettings
                                                            .getInstance();
    private final static Logger         logger      = LoggerFactory
                                                            .getLogger(AmuseScene.class);

    private final AmuseSceneDescription description;
    private final ArrayList<Star>       stars;
    private final AmuseGasOctreeNode    gasOctree;

    private float[][]                   gasParticles;

    private boolean                     initialized = false;
    private VBO                         vbo;

    public AmuseScene(AmuseSceneDescription description, ArrayList<Star> stars,
            AmuseGasOctreeNode gasOctree) {
        this.description = description;
        this.stars = stars;
        this.gasOctree = gasOctree;

        logger.debug("Scene constructed with " + stars.size()
                + " particles and " + gasOctree.childCounter + " gasses.");
    }

    public AmuseScene(AmuseSceneDescription description, ArrayList<Star> stars,
            float[][] gasParticles) {
        this.description = description;
        this.stars = stars;
        gasOctree = null;

        this.gasParticles = gasParticles;
    }

    public AmuseScene(AmuseSceneDescription description, ArrayList<Star> stars,
            AmuseGasOctreeNode gasOctree, float[][] gasParticles) {
        this.description = description;
        this.stars = stars;
        this.gasOctree = gasOctree;

        this.gasParticles = gasParticles;
    }

    public synchronized void drawStars(GL3 gl, Program starProgram,
            MatF4 MVMatrix) {
        for (Star s : stars) {
            s.draw(gl, starProgram, MVMatrix);
        }
    }

    public synchronized void drawGas(GL3 gl, Program gasProgram, MatF4 MVMatrix) {
        gasProgram.setUniformMatrix("global_MVMatrix", MVMatrix);
        gasProgram.setUniform("gas_opacity_factor",
                settings.getGasOpacityFactor());

        // gasOctree.draw(gl, gasProgram);

        if (!initialized) {
            logger.debug("initializing VBO");

            // float X = 0.525731112119133606f;
            // float Z = 0.850650808352039932f;
            //
            // FloatBuffer vdata = FloatBuffer.allocate(12 * 3);
            // vdata.put(-X);
            // vdata.put(0f);
            // vdata.put(Z);
            // vdata.put(X);
            // vdata.put(0f);
            // vdata.put(Z);
            // vdata.put(-X);
            // vdata.put(0f);
            // vdata.put(-Z);
            // vdata.put(X);
            // vdata.put(0f);
            // vdata.put(-Z);
            // vdata.put(0f);
            // vdata.put(Z);
            // vdata.put(X);
            // vdata.put(0f);
            // vdata.put(Z);
            // vdata.put(-X);
            // vdata.put(0f);
            // vdata.put(-Z);
            // vdata.put(X);
            // vdata.put(0f);
            // vdata.put(-Z);
            // vdata.put(-X);
            // vdata.put(Z);
            // vdata.put(X);
            // vdata.put(0f);
            // vdata.put(-Z);
            // vdata.put(X);
            // vdata.put(0f);
            // vdata.put(Z);
            // vdata.put(-X);
            // vdata.put(0f);
            // vdata.put(-Z);
            // vdata.put(-X);
            // vdata.put(0f);
            //
            // IntBuffer tindices = IntBuffer.allocate(20 * 3);
            // tindices.put(1);
            // tindices.put(4);
            // tindices.put(0);
            // tindices.put(4);
            // tindices.put(9);
            // tindices.put(0);
            // tindices.put(4);
            // tindices.put(5);
            // tindices.put(9);
            // tindices.put(8);
            // tindices.put(5);
            // tindices.put(4);
            // tindices.put(1);
            // tindices.put(8);
            // tindices.put(4);
            // tindices.put(1);
            // tindices.put(10);
            // tindices.put(8);
            // tindices.put(10);
            // tindices.put(3);
            // tindices.put(8);
            // tindices.put(8);
            // tindices.put(3);
            // tindices.put(5);
            // tindices.put(3);
            // tindices.put(2);
            // tindices.put(5);
            // tindices.put(3);
            // tindices.put(7);
            // tindices.put(2);
            // tindices.put(3);
            // tindices.put(10);
            // tindices.put(7);
            // tindices.put(10);
            // tindices.put(6);
            // tindices.put(7);
            // tindices.put(6);
            // tindices.put(11);
            // tindices.put(7);
            // tindices.put(6);
            // tindices.put(0);
            // tindices.put(11);
            // tindices.put(6);
            // tindices.put(1);
            // tindices.put(0);
            // tindices.put(10);
            // tindices.put(1);
            // tindices.put(6);
            // tindices.put(11);
            // tindices.put(0);
            // tindices.put(9);
            // tindices.put(2);
            // tindices.put(11);
            // tindices.put(9);
            // tindices.put(5);
            // tindices.put(2);
            // tindices.put(9);
            // tindices.put(11);
            // tindices.put(2);
            // tindices.put(7);
            //
            // gasProgram.passUniformVecArray(gl, "vdata", vdata, 1, 12 *
            // 3);
            // gasProgram.passUniformVecArray(gl, "tindices", tindices, 1,20
            // * 3);
            // gasProgram.passUniform(gl, "radius", 20f);
            // gasProgram.passUniform(gl, "ndiv", 1);

            logger.debug("Particles: " + gasParticles.length);

            GLSLAttrib vAttrib = new GLSLAttrib(toBuffer(gasParticles),
                    "MCvertex", GLSLAttrib.SIZE_FLOAT, 4);

            GLSLAttrib cAttrib = new GLSLAttrib(gasColors(gasParticles,
                    gasOctree), "MCcolor", GLSLAttrib.SIZE_FLOAT, 4);

            vbo = new VBO(gl, vAttrib, cAttrib);
            initialized = true;
        }

        // logger.debug("Drawing gas.");

        try {
            gasProgram.use(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }

        vbo.bind(gl);

        gasProgram.linkAttribs(gl, vbo.getAttribs());

        gl.glDrawArrays(GL3.GL_POINTS, 0, gasParticles.length);
    }

    FloatBuffer gasColors(float[][] particles, AmuseGasOctreeNode root) {
        FloatBuffer result = FloatBuffer.allocate(particles.length * 4);

        for (int i = 0; i < particles.length; i++) {
            result.put(root.getColor(particles[i]));
        }

        result.rewind();

        return result;
    }

    FloatBuffer toBuffer(float[][] particles) {
        FloatBuffer result = FloatBuffer.allocate(particles.length * 4);

        for (int i = 0; i < particles.length; i++) {
            for (int j = 0; j < 4; j++) {
                result.put(particles[i][j]);
            }
        }

        result.rewind();

        return result;
    }

    public AmuseSceneDescription getDescription() {
        return description;
    }
}
